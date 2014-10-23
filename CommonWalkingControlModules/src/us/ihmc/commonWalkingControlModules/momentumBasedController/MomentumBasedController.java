package us.ihmc.commonWalkingControlModules.momentumBasedController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactPointVisualizer;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelHumanoidControllerFactoryHelper;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumControlModuleBridge.MomentumControlModuleType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.DesiredJointAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.DesiredPointAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.DesiredRateOfChangeOfMomentumCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.DesiredSpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.MomentumModuleSolution;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumControlModuleException;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.OptimizationMomentumControlModule;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.commonWalkingControlModules.visualizer.WrenchVisualizer;
import us.ihmc.utilities.frictionModels.FrictionModel;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsCalculator;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.SpatialForceVector;
import us.ihmc.utilities.screwTheory.TotalMassCalculator;
import us.ihmc.utilities.screwTheory.TotalWrenchCalculator;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoFrameVector2d;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoVariable;
import us.ihmc.yoUtilities.math.filters.RateLimitedYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;
import us.ihmc.yoUtilities.math.frames.YoFrameVector2d;


public class MomentumBasedController
{
   public static final boolean DO_NECK_PD_CONTROL = true;
   private static final boolean DO_PASSIVE_KNEE_CONTROL = true;
   
   public static final boolean SPY_ON_MOMENTUM_BASED_CONTROLLER = false;

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final ReferenceFrame centerOfMassFrame;
   private final FullRobotModel fullRobotModel;
   private final CenterOfMassJacobian centerOfMassJacobian;
   private final CommonHumanoidReferenceFrames referenceFrames;
   private final TwistCalculator twistCalculator;

   private final SideDependentList<ContactablePlaneBody> feet, hands, thighs;
   private final ContactablePlaneBody pelvis, pelvisBack;

   private final List<ContactablePlaneBody> contactablePlaneBodyList;
   private final List<YoPlaneContactState> yoPlaneContactStateList = new ArrayList<YoPlaneContactState>();
   private final LinkedHashMap<ContactablePlaneBody, YoPlaneContactState> yoPlaneContactStates = new LinkedHashMap<ContactablePlaneBody, YoPlaneContactState>();

   private final DoubleYoVariable leftPassiveKneeTorque = new DoubleYoVariable("leftPassiveKneeTorque", registry);
   private final DoubleYoVariable rightPassiveKneeTorque = new DoubleYoVariable("rightPassiveKneeTorque", registry);
   private final SideDependentList<DoubleYoVariable> passiveKneeTorque = new SideDependentList<DoubleYoVariable>(leftPassiveKneeTorque, rightPassiveKneeTorque);

   private final DoubleYoVariable passiveQKneeThreshold = new DoubleYoVariable("passiveQKneeThreshold", registry);
   private final DoubleYoVariable passiveKneeMaxTorque = new DoubleYoVariable("passiveKneeMaxTorque", registry);
   private final DoubleYoVariable passiveKneeKv = new DoubleYoVariable("passiveKneeKv", registry);

   private final ArrayList<Updatable> updatables = new ArrayList<Updatable>();
   private final DoubleYoVariable yoTime;
   private final double controlDT;
   private final double gravity;

   private final YoFrameVector finalDesiredPelvisLinearAcceleration;
   private final YoFrameVector finalDesiredPelvisAngularAcceleration;
   private final YoFrameVector desiredPelvisForce;
   private final YoFrameVector desiredPelvisTorque;
   
   private final FrameVector centroidalMomentumRateSolutionLinearPart;
   private final YoFrameVector qpOutputCoMAcceleration;


   private final YoFrameVector admissibleDesiredGroundReactionTorque;
   private final YoFrameVector admissibleDesiredGroundReactionForce;
   //   private final YoFrameVector groundReactionTorqueCheck;
   //   private final YoFrameVector groundReactionForceCheck;

   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> preRateLimitedDesiredAccelerations = new LinkedHashMap<OneDoFJoint, DoubleYoVariable>();
   private final LinkedHashMap<OneDoFJoint, RateLimitedYoVariable> rateLimitedDesiredAccelerations = new LinkedHashMap<OneDoFJoint, RateLimitedYoVariable>();

   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> desiredAccelerationYoVariables = new LinkedHashMap<OneDoFJoint, DoubleYoVariable>();

   private final InverseDynamicsCalculator inverseDynamicsCalculator;

   private final MomentumControlModuleBridge momentumControlModuleBridge;

   private final SpatialForceVector gravitationalWrench;
   private final EnumYoVariable<RobotSide> upcomingSupportLeg = EnumYoVariable.create("upcomingSupportLeg", "", RobotSide.class, registry, true); // FIXME: not general enough; this should not be here

   private final PlaneContactWrenchProcessor planeContactWrenchProcessor;
   private final MomentumBasedControllerSpy momentumBasedControllerSpy;
   private final ContactPointVisualizer contactPointVisualizer;
   private final WrenchVisualizer wrenchVisualizer;

   private final GeometricJacobianHolder robotJacobianHolder = new GeometricJacobianHolder();

   private final SideDependentList<FootSwitchInterface> footSwitches;
   private final DoubleYoVariable alphaCoPControl = new DoubleYoVariable("alphaCoPControl", registry);
   private final DoubleYoVariable maxAnkleTorqueCoPControl = new DoubleYoVariable("maxAnkleTorqueCoPControl", registry);
   private final SideDependentList<AlphaFilteredYoFrameVector2d> desiredTorquesForCoPControl;
   
   
   private final SideDependentList<Double> xSignsForCoPControl, ySignsForCoPControl;
   private final double minZForceForCoPControlScaling, maxZForceForCoPControlScaling;
   
   private final SideDependentList<YoFrameVector2d> yoCoPError;
   private final SideDependentList<DoubleYoVariable> yoCoPErrorMagnitude = new SideDependentList<DoubleYoVariable>(new DoubleYoVariable(
         "leftFootCoPErrorMagnitude", registry), new DoubleYoVariable("rightFootCoPErrorMagnitude", registry));
   private final DoubleYoVariable gainCoPX = new DoubleYoVariable("gainCoPX", registry);
   private final DoubleYoVariable gainCoPY = new DoubleYoVariable("gainCoPY", registry);
   private final SideDependentList<DoubleYoVariable> copControlScales;


   // once we receive the data from the UI through a provider this variables souldn't be necessary
   private final DoubleYoVariable frictionCompensationEffectiveness;
   private final EnumYoVariable<FrictionModel> frictionModelForAllJoints;
   private final BooleanYoVariable useBeforeTransmissionVelocityForFriction;

   private final YoGraphicsListRegistry yoGraphicsListRegistry;

   private final InverseDynamicsJoint[] controlledJoints;
   
   private final BooleanYoVariable feetCoPControlIsActive;
   private final DoubleYoVariable footCoPOffsetX, footCoPOffsetY;
   private final EnumYoVariable<RobotSide> footUnderCoPControl;

   private final BooleanYoVariable userActivateFeetForce = new BooleanYoVariable("userActivateFeetForce", registry);
   private final DoubleYoVariable userLateralFeetForce = new DoubleYoVariable("userLateralFeetForce", registry);
   private final DoubleYoVariable userForwardFeetForce = new DoubleYoVariable("userForwardFeetForce", registry);
   private final DoubleYoVariable userYawFeetTorque = new DoubleYoVariable("userYawFeetTorque", registry);
   private final Wrench userAdditionalFeetWrench = new Wrench();

   private final double totalMass;
   
   public MomentumBasedController(FullRobotModel fullRobotModel, CenterOfMassJacobian centerOfMassJacobian, CommonHumanoidReferenceFrames referenceFrames,
         SideDependentList<FootSwitchInterface> footSwitches, DoubleYoVariable yoTime, double gravityZ, TwistCalculator twistCalculator,
         SideDependentList<ContactablePlaneBody> feet, SideDependentList<ContactablePlaneBody> handsWithFingersBentBack,
         SideDependentList<ContactablePlaneBody> thighs, ContactablePlaneBody pelvis, ContactablePlaneBody pelvisBack, double controlDT,
         OldMomentumControlModule oldMomentumControlModule, ArrayList<Updatable> updatables, WalkingControllerParameters walkingControllerParameters,
         YoGraphicsListRegistry yoGraphicsListRegistry, InverseDynamicsJoint... jointsToIgnore)
   {
      this.yoGraphicsListRegistry = yoGraphicsListRegistry;

      centerOfMassFrame = referenceFrames.getCenterOfMassFrame();
      totalMass = TotalMassCalculator.computeSubTreeMass(fullRobotModel.getElevator());
      
      this.footSwitches = footSwitches;

      if (SPY_ON_MOMENTUM_BASED_CONTROLLER)
         momentumBasedControllerSpy = new MomentumBasedControllerSpy(registry);
      else
         momentumBasedControllerSpy = null;

      MathTools.checkIfInRange(gravityZ, 0.0, Double.POSITIVE_INFINITY);

      this.fullRobotModel = fullRobotModel;
      this.centerOfMassJacobian = centerOfMassJacobian;
      this.referenceFrames = referenceFrames;
      this.twistCalculator = twistCalculator;
      this.controlDT = controlDT;
      this.gravity = gravityZ;
      this.yoTime = yoTime;

      // Initialize the contactable bodies
      this.feet = feet;
      this.hands = handsWithFingersBentBack;
      this.thighs = thighs;
      this.pelvis = pelvis;
      this.pelvisBack = pelvisBack;

      RigidBody elevator = fullRobotModel.getElevator();

      if (jointsToIgnore != null && jointsToIgnore.length > 0)
         inverseDynamicsCalculator = new InverseDynamicsCalculator(twistCalculator, gravityZ, Arrays.asList(jointsToIgnore));
      else
         inverseDynamicsCalculator = new InverseDynamicsCalculator(twistCalculator, gravityZ);

      double totalMass = TotalMassCalculator.computeSubTreeMass(elevator);

      gravitationalWrench = new SpatialForceVector(centerOfMassFrame, new Vector3d(0.0, 0.0, totalMass * gravityZ), new Vector3d());

      ReferenceFrame pelvisFrame = referenceFrames.getPelvisFrame();
      this.finalDesiredPelvisLinearAcceleration = new YoFrameVector("finalDesiredPelvisLinearAcceleration", "", pelvisFrame, registry);
      this.finalDesiredPelvisAngularAcceleration = new YoFrameVector("finalDesiredPelvisAngularAcceleration", "", pelvisFrame, registry);
      this.desiredPelvisForce = new YoFrameVector("desiredPelvisForce", "", centerOfMassFrame, registry);
      this.desiredPelvisTorque = new YoFrameVector("desiredPelvisTorque", "", centerOfMassFrame, registry);

      centroidalMomentumRateSolutionLinearPart = new FrameVector(centerOfMassFrame);
      this.qpOutputCoMAcceleration = new YoFrameVector("qpOutputCoMAcceleration", "", centerOfMassFrame, registry);

      this.admissibleDesiredGroundReactionTorque = new YoFrameVector("admissibleDesiredGroundReactionTorque", centerOfMassFrame, registry);
      this.admissibleDesiredGroundReactionForce = new YoFrameVector("admissibleDesiredGroundReactionForce", centerOfMassFrame, registry);

      //      this.groundReactionTorqueCheck = new YoFrameVector("groundReactionTorqueCheck", centerOfMassFrame, registry);
      //      this.groundReactionForceCheck = new YoFrameVector("groundReactionForceCheck", centerOfMassFrame, registry);

      if (updatables != null)
      {
         this.updatables.addAll(updatables);
      }

      double coefficientOfFriction = 1.0; // TODO: magic number...

      // TODO: get rid of the null checks
      this.contactablePlaneBodyList = new ArrayList<ContactablePlaneBody>();

      if (feet != null)
      {
         this.contactablePlaneBodyList.addAll(feet.values()); //leftSole and rightSole
      }

      if (handsWithFingersBentBack != null)
      {
         this.contactablePlaneBodyList.addAll(handsWithFingersBentBack.values());
      }

      if (thighs != null)
      {
         this.contactablePlaneBodyList.addAll(thighs.values());
      }

      if (pelvis != null)
         this.contactablePlaneBodyList.add(pelvis);

      if (pelvisBack != null)
         this.contactablePlaneBodyList.add(pelvisBack);

      for (ContactablePlaneBody contactablePlaneBody : this.contactablePlaneBodyList)
      {
         RigidBody rigidBody = contactablePlaneBody.getRigidBody();
         YoPlaneContactState contactState = new YoPlaneContactState(contactablePlaneBody.getSoleFrame().getName(), rigidBody,
               contactablePlaneBody.getSoleFrame(), contactablePlaneBody.getContactPoints2d(), coefficientOfFriction, registry);
         yoPlaneContactStates.put(contactablePlaneBody, contactState);
         yoPlaneContactStateList.add(contactState);
      }

      InverseDynamicsJoint[] jointsToOptimizeFor = HighLevelHumanoidControllerFactoryHelper.computeJointsToOptimizeFor(fullRobotModel, jointsToIgnore);
      MomentumOptimizationSettings momentumOptimizationSettings = new MomentumOptimizationSettings(jointsToOptimizeFor, registry);
      walkingControllerParameters.setupMomentumOptimizationSettings(momentumOptimizationSettings);

      controlledJoints = momentumOptimizationSettings.getJointsToOptimizeFor();
      for (InverseDynamicsJoint joint : controlledJoints)
      {
         if (joint instanceof OneDoFJoint)
         {
            desiredAccelerationYoVariables.put((OneDoFJoint) joint, new DoubleYoVariable(joint.getName() + "qdd_d", registry));
            rateLimitedDesiredAccelerations.put((OneDoFJoint) joint, new RateLimitedYoVariable(joint.getName() + "_rl_qdd_d", registry, 10000.0, controlDT));
            preRateLimitedDesiredAccelerations.put((OneDoFJoint) joint, new DoubleYoVariable(joint.getName() + "_prl_qdd_d", registry));
         }
      }

      this.planeContactWrenchProcessor = new PlaneContactWrenchProcessor(this.contactablePlaneBodyList, yoGraphicsListRegistry, registry);

      if (yoGraphicsListRegistry != null)
      {
         contactPointVisualizer = new ContactPointVisualizer(new ArrayList<YoPlaneContactState>(yoPlaneContactStateList), yoGraphicsListRegistry,
               registry);
         List<RigidBody> rigidBodies = Arrays.asList(ScrewTools.computeSupportAndSubtreeSuccessors(fullRobotModel.getRootJoint().getSuccessor()));
         wrenchVisualizer = new WrenchVisualizer("DesiredExternalWrench", rigidBodies, yoGraphicsListRegistry, registry);
      }
      else
      {
         contactPointVisualizer = null;
         wrenchVisualizer = null;
      }

      OptimizationMomentumControlModule optimizationMomentumControlModule = null;
      if (momentumOptimizationSettings != null)
      {
         optimizationMomentumControlModule = new OptimizationMomentumControlModule(fullRobotModel.getRootJoint(), referenceFrames.getCenterOfMassFrame(),
               controlDT, gravityZ, momentumOptimizationSettings, twistCalculator, robotJacobianHolder, yoPlaneContactStateList,
               yoGraphicsListRegistry, registry);
      }

      momentumControlModuleBridge = new MomentumControlModuleBridge(optimizationMomentumControlModule, oldMomentumControlModule, centerOfMassFrame, registry);

      if (DO_PASSIVE_KNEE_CONTROL)
      {
         passiveQKneeThreshold.set(0.55);
         passiveKneeMaxTorque.set(60.0);
         passiveKneeKv.set(5.0);
      }

      desiredTorquesForCoPControl = new SideDependentList<AlphaFilteredYoFrameVector2d>();
      yoCoPError = new SideDependentList<YoFrameVector2d>();
      xSignsForCoPControl = new SideDependentList<Double>();
      ySignsForCoPControl = new SideDependentList<Double>();
      copControlScales = new SideDependentList<DoubleYoVariable>();

      for (RobotSide robotSide : RobotSide.values())
      {
         OneDoFJoint anklePitchJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_PITCH);
         OneDoFJoint ankleRollJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_ROLL);

         FrameVector pitchJointAxis = anklePitchJoint.getJointAxis();
         FrameVector rollJointAxis = ankleRollJoint.getJointAxis();

         xSignsForCoPControl.put(robotSide, pitchJointAxis.getY());
         ySignsForCoPControl.put(robotSide, rollJointAxis.getY());

         copControlScales.put(robotSide, new DoubleYoVariable(robotSide.getCamelCaseNameForStartOfExpression() + "CoPControlScale", registry));
      }
      
      minZForceForCoPControlScaling = 0.20 * totalMass * 9.81;
      maxZForceForCoPControlScaling = 0.45 * totalMass * 9.81;
      
      alphaCoPControl.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(16.0, controlDT));
      maxAnkleTorqueCoPControl.set(10.0);

      for (RobotSide robotSide : RobotSide.values)
      {
         desiredTorquesForCoPControl.put(
               robotSide,
               AlphaFilteredYoFrameVector2d.createAlphaFilteredYoFrameVector2d("desired" + robotSide.getCamelCaseNameForMiddleOfExpression()
                     + "AnkleTorqueForCoPControl", "", registry, alphaCoPControl, feet.get(robotSide).getSoleFrame()));
         yoCoPError.put(robotSide, new YoFrameVector2d(robotSide.getCamelCaseNameForStartOfExpression() + "FootCoPError", feet.get(robotSide).getSoleFrame(),
               registry));
      }

      // friction variables for all robot
      frictionModelForAllJoints = new EnumYoVariable<>("frictionModelForAllJoints", registry, FrictionModel.class);
      frictionCompensationEffectiveness = new DoubleYoVariable("frictionCompensationEffectiveness", registry);
      useBeforeTransmissionVelocityForFriction = new BooleanYoVariable("usePreTransmissionVelocityForFriction", registry);
      addVariableChangedListenerToFrictionCompensationVariables(frictionCompensationEffectiveness, frictionModelForAllJoints,
            useBeforeTransmissionVelocityForFriction);
      initializeFrictionCompensationToZero();

      feetCoPControlIsActive = new BooleanYoVariable("feetCoPControlIsActive", registry);
      footCoPOffsetX = new DoubleYoVariable("footCoPOffsetX", registry);
      footCoPOffsetY = new DoubleYoVariable("footCoPOffsetY", registry);
      footUnderCoPControl = new EnumYoVariable<RobotSide>("footUnderCoPControl", registry, RobotSide.class);
   }

   public void getFeetContactStates(ArrayList<PlaneContactState> feetContactStatesToPack)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         feetContactStatesToPack.add(yoPlaneContactStates.get(feet.get(robotSide)));
      }
   }

   public SpatialForceVector getGravitationalWrench()
   {
      return gravitationalWrench;
   }

   private static double computeDesiredAcceleration(double k, double d, double qDesired, double qdDesired, OneDoFJoint joint)
   {
      return k * (qDesired - joint.getQ()) + d * (qdDesired - joint.getQd());
   }

   public void setExternalWrenchToCompensateFor(RigidBody rigidBody, Wrench wrench)
   {
      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.setExternalWrenchToCompensateFor(rigidBody, wrench);
      }

      //      momentumControlModuleBridge.getActiveMomentumControlModule().setExternalWrenchToCompensateFor(rigidBody, wrench);
      momentumControlModuleBridge.setExternalWrenchToCompensateFor(rigidBody, wrench);
   }

   // TODO: Temporary method for a big refactor allowing switching between high level behaviors
   public void doPrioritaryControl()
   {
      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.doPrioritaryControl();
      }

      robotJacobianHolder.compute();

      callUpdatables();

      inverseDynamicsCalculator.reset();
      momentumControlModuleBridge.reset();
   }

   // TODO: Temporary method for a big refactor allowing switching between high level behaviors
   public void doSecondaryControl()
   {
      if (contactPointVisualizer != null)
         contactPointVisualizer.update();

      updateMomentumBasedControllerSpy();

      MomentumModuleSolution momentumModuleSolution;
      
      if (feetCoPControlIsActive.getBooleanValue())
      {
         if(footCoPOffsetX.isNaN() || footCoPOffsetY.isNaN())
         {
            footCoPOffsetX.set(0.0);
            footCoPOffsetY.set(0.0);
         }
         
         ReferenceFrame selectedFootSoleFrame = feet.get(footUnderCoPControl.getEnumValue()).getSoleFrame();
         Vector3d copOffset = new Vector3d(footCoPOffsetX.getDoubleValue(), footCoPOffsetY.getDoubleValue(), 0.0);
         String sideName = footUnderCoPControl.getEnumValue().name(); 
         ReferenceFrame desiredCoPFrame = ReferenceFrame.constructBodyFrameWithUnchangingTranslationFromParent(sideName +"_desiredCoPFrame", selectedFootSoleFrame, copOffset); 
         
         momentumControlModuleBridge.setFootCoPControlData(footUnderCoPControl.getEnumValue(), desiredCoPFrame);
      }
      else
      {
         footCoPOffsetX.set(Double.NaN);
         footCoPOffsetY.set(Double.NaN);
         momentumControlModuleBridge.setFootCoPControlData(footUnderCoPControl.getEnumValue(), null);
      }
      
      try
      {
         momentumModuleSolution = momentumControlModuleBridge.compute(this.yoPlaneContactStates, upcomingSupportLeg.getEnumValue());
      }
      catch (MomentumControlModuleException momentumControlModuleException)
      {
         if (momentumBasedControllerSpy != null)
            momentumBasedControllerSpy.printMomentumCommands(System.err);

         // Don't crash and burn. Instead do the best you can with what you have.
         // Or maybe just use the previous ticks solution.
         // Need to test these.
         momentumModuleSolution = momentumControlModuleException.getMomentumModuleSolution();
         //throw new RuntimeException(momentumControlModuleException);
      }

      SpatialForceVector centroidalMomentumRateSolution = momentumModuleSolution.getCentroidalMomentumRateSolution();
      centroidalMomentumRateSolution.packLinearPart(centroidalMomentumRateSolutionLinearPart);
      centroidalMomentumRateSolutionLinearPart.scale(1.0 / totalMass);
      qpOutputCoMAcceleration.set(centroidalMomentumRateSolutionLinearPart);
      
      Map<RigidBody, Wrench> externalWrenches = momentumModuleSolution.getExternalWrenchSolution();

      if (userActivateFeetForce.getBooleanValue())
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            RigidBody foot = fullRobotModel.getFoot(robotSide);
            userAdditionalFeetWrench.setToZero(foot.getBodyFixedFrame(), referenceFrames.getMidFeetZUpFrame());
            userAdditionalFeetWrench.setLinearPartX(robotSide.negateIfRightSide(userForwardFeetForce.getDoubleValue()));
            userAdditionalFeetWrench.setLinearPartY(robotSide.negateIfRightSide(userLateralFeetForce.getDoubleValue()));
            userAdditionalFeetWrench.setAngularPartZ(robotSide.negateIfRightSide(userYawFeetTorque.getDoubleValue()));
            
            Wrench externalWrench = externalWrenches.get(foot);
            userAdditionalFeetWrench.changeFrame(externalWrench.getExpressedInFrame());
            externalWrench.add(userAdditionalFeetWrench);
         }
      }

      for (RigidBody rigidBody : externalWrenches.keySet())
      {
         inverseDynamicsCalculator.setExternalWrench(rigidBody, externalWrenches.get(rigidBody));
      }

      planeContactWrenchProcessor.compute(externalWrenches);
      if (wrenchVisualizer != null)
         wrenchVisualizer.visualize(externalWrenches);

      SpatialForceVector totalGroundReactionWrench = new SpatialForceVector(centerOfMassFrame);
      Wrench admissibleGroundReactionWrench = TotalWrenchCalculator.computeTotalWrench(externalWrenches.values(),
            totalGroundReactionWrench.getExpressedInFrame());
      admissibleDesiredGroundReactionTorque.set(admissibleGroundReactionWrench.getAngularPartX(), admissibleGroundReactionWrench.getAngularPartY(),
            admissibleGroundReactionWrench.getAngularPartZ());
      admissibleDesiredGroundReactionForce.set(admissibleGroundReactionWrench.getLinearPartX(), admissibleGroundReactionWrench.getLinearPartY(),
            admissibleGroundReactionWrench.getLinearPartZ());

      //      SpatialForceVector groundReactionWrenchCheck = inverseDynamicsCalculator.computeTotalExternalWrench(centerOfMassFrame);
      //      groundReactionTorqueCheck.set(groundReactionWrenchCheck.getAngularPartCopy());
      //      groundReactionForceCheck.set(groundReactionWrenchCheck.getLinearPartCopy());

      inverseDynamicsCalculator.compute();

      updateYoVariables();
   }

   // FIXME GET RID OF THAT HACK!!!
   /**
    * Call this method after doSecondaryControl() to generate a small torque of flexion at the knees when almost straight.
    * This helps a lot when working near singularities but it is kinda hackish.
    */
   public void doPassiveKneeControl()
   {
      double maxPassiveTorque = passiveKneeMaxTorque.getDoubleValue();
      double kneeLimit = passiveQKneeThreshold.getDoubleValue();
      double kdKnee = passiveKneeKv.getDoubleValue();

      for (RobotSide robotSide : RobotSide.values)
      {
         OneDoFJoint kneeJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.KNEE);
         double sign = Math.signum(kneeJoint.getJointAxis().getY());
         double tauKnee = kneeJoint.getTau();
         double qKnee = kneeJoint.getQ() * sign;
         double qdKnee = kneeJoint.getQd() * sign;
         if (qKnee < kneeLimit)
         {
            double percent = 1.0 - qKnee / kneeLimit;
            percent = MathTools.clipToMinMax(percent, 0.0, 1.0);
            passiveKneeTorque.get(robotSide).set(sign * maxPassiveTorque * MathTools.square(percent) - kdKnee * qdKnee);
            tauKnee += passiveKneeTorque.get(robotSide).getDoubleValue();
            kneeJoint.setTau(tauKnee);
         }
         else
         {
            passiveKneeTorque.get(robotSide).set(0.0);
         }
      }
   }

   private final FramePoint2d copDesired = new FramePoint2d();
   private final FramePoint2d copActual = new FramePoint2d();
   private final FrameVector2d copError = new FrameVector2d();
   private final Wrench footWrench = new Wrench();
   private final FrameVector footForceVector = new FrameVector();

   public final void doProportionalControlOnCoP()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody contactablePlaneBody = feet.get(robotSide);
         ReferenceFrame planeFrame = contactablePlaneBody.getSoleFrame();
         AlphaFilteredYoFrameVector2d desiredTorqueForCoPControl = desiredTorquesForCoPControl.get(robotSide);

         FramePoint2d cop = planeContactWrenchProcessor.getCops().get(contactablePlaneBody);

         if (cop == null || cop.containsNaN())
         {
            desiredTorqueForCoPControl.setToZero();
            return;
         }

         copDesired.setIncludingFrame(cop);
         FootSwitchInterface footSwitch = footSwitches.get(robotSide);
         footSwitch.computeAndPackCoP(copActual);

         if (copActual.containsNaN())
         {
            desiredTorqueForCoPControl.setToZero();
            return;
         }
         
         copError.setToZero(planeFrame);
         copError.sub(copDesired, copActual);
         yoCoPError.get(robotSide).set(copError);
         yoCoPErrorMagnitude.get(robotSide).set(copError.length());

         double xSignForCoPControl = xSignsForCoPControl.get(robotSide);
         double ySignForCoPControl = ySignsForCoPControl.get(robotSide);
         
         footSwitch.computeAndPackFootWrench(footWrench);
         footForceVector.setToZero(footWrench.getExpressedInFrame());
         footWrench.packLinearPart(footForceVector);
         footForceVector.changeFrame(ReferenceFrame.getWorldFrame());
         
         double zForce = footForceVector.getZ();
         double scale = (zForce - minZForceForCoPControlScaling) / (maxZForceForCoPControlScaling - minZForceForCoPControlScaling);
         scale = MathTools.clipToMinMax(scale, 0.0, 1.0);
         
         copControlScales.get(robotSide).set(scale);
         
         copError.scale(scale * xSignForCoPControl * gainCoPX.getDoubleValue(), scale * ySignForCoPControl * gainCoPY.getDoubleValue());
         copError.clipMaxLength(maxAnkleTorqueCoPControl.getDoubleValue());

         desiredTorqueForCoPControl.update(copError);

         OneDoFJoint anklePitchJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_PITCH);
         anklePitchJoint.setTau(anklePitchJoint.getTau() + desiredTorqueForCoPControl.getX());

         OneDoFJoint ankleRollJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_ROLL);
         ankleRollJoint.setTau(ankleRollJoint.getTau() + desiredTorqueForCoPControl.getY());
      }
   }

   private void updateMomentumBasedControllerSpy()
   {
      if (momentumBasedControllerSpy != null)
      {
         for (int i = 0; i < contactablePlaneBodyList.size(); i++)
         {
            ContactablePlaneBody contactablePlaneBody = contactablePlaneBodyList.get(i);
            YoPlaneContactState contactState = yoPlaneContactStates.get(contactablePlaneBody);
            if (contactState.inContact())
            {
               momentumBasedControllerSpy.setPlaneContactState(contactablePlaneBody, contactState.getContactFramePoints2dInContactCopy(),
                     contactState.getCoefficientOfFriction(), contactState.getContactNormalFrameVectorCopy());
            }
         }
         momentumBasedControllerSpy.doSecondaryControl();
      }
   }

   public void callUpdatables()
   {
      double time = yoTime.getDoubleValue();
      for (int i = 0; i < updatables.size(); i++)
      {
         updatables.get(i).update(time);
      }
   }

   public void addUpdatable(Updatable updatable)
   {
      updatables.add(updatable);
   }

   public void doPDControl(OneDoFJoint[] joints, double kp, double kd, double maxAcceleration, double maxJerk)
   {
      for (OneDoFJoint joint : joints)
      {
         doPDControl(joint, kp, kd, 0.0, 0.0, maxAcceleration, maxJerk);
      }
   }

   public void doPDControl(OneDoFJoint joint, double kp, double kd, double desiredPosition, double desiredVelocity, double maxAcceleration, double maxJerk)
   {
      double desiredAcceleration = computeDesiredAcceleration(kp, kd, desiredPosition, desiredVelocity, joint);
      
      if (!DO_NECK_PD_CONTROL)
      {
         if (joint.getName().contains("Neck"))
         {
            desiredAcceleration = 0.0;
         }
      }
      
      desiredAcceleration = MathTools.clipToMinMax(desiredAcceleration, maxAcceleration);
      preRateLimitedDesiredAccelerations.get(joint).set(desiredAcceleration);

      RateLimitedYoVariable rateLimitedDesiredAcceleration = this.rateLimitedDesiredAccelerations.get(joint);
      rateLimitedDesiredAcceleration.setMaxRate(maxJerk);
      rateLimitedDesiredAcceleration.update(desiredAcceleration);

      setOneDoFJointAcceleration(joint, rateLimitedDesiredAcceleration.getDoubleValue());
   }

   private final Map<OneDoFJoint, DenseMatrix64F> tempJointAcceleration = new LinkedHashMap<OneDoFJoint, DenseMatrix64F>();

   public void setOneDoFJointAcceleration(OneDoFJoint joint, double desiredAcceleration)
   {
      joint.setMomentumBasedQddDesired(desiredAcceleration);

      if (tempJointAcceleration.get(joint) == null)
         tempJointAcceleration.put(joint, new DenseMatrix64F(joint.getDegreesOfFreedom(), 1));

      DenseMatrix64F jointAcceleration = tempJointAcceleration.get(joint);
      jointAcceleration.set(0, 0, desiredAcceleration);

      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.setDesiredJointAcceleration(joint, jointAcceleration);
      }

      DesiredJointAccelerationCommand desiredJointAccelerationCommand = new DesiredJointAccelerationCommand(joint, jointAcceleration);
      momentumControlModuleBridge.setDesiredJointAcceleration(desiredJointAccelerationCommand);
   }

   private final SpatialAccelerationVector pelvisAcceleration = new SpatialAccelerationVector();
   private final Wrench pelvisJointWrench = new Wrench();

   private void updateYoVariables()
   {
      fullRobotModel.getRootJoint().packDesiredJointAcceleration(pelvisAcceleration);

      finalDesiredPelvisAngularAcceleration.checkReferenceFrameMatch(pelvisAcceleration.getExpressedInFrame());
      finalDesiredPelvisAngularAcceleration.set(pelvisAcceleration.getAngularPartX(), pelvisAcceleration.getAngularPartY(),
            pelvisAcceleration.getAngularPartZ());

      finalDesiredPelvisLinearAcceleration.checkReferenceFrameMatch(pelvisAcceleration.getExpressedInFrame());
      finalDesiredPelvisLinearAcceleration.set(pelvisAcceleration.getLinearPartX(), pelvisAcceleration.getLinearPartY(), pelvisAcceleration.getLinearPartZ());

      fullRobotModel.getRootJoint().packWrench(pelvisJointWrench);
      pelvisJointWrench.changeFrame(referenceFrames.getCenterOfMassFrame());
      desiredPelvisForce.set(pelvisJointWrench.getLinearPartX(), pelvisJointWrench.getLinearPartY(), pelvisJointWrench.getLinearPartZ());
      desiredPelvisTorque.set(pelvisJointWrench.getAngularPartX(), pelvisJointWrench.getAngularPartY(), pelvisJointWrench.getAngularPartZ());

      for (OneDoFJoint joint : desiredAccelerationYoVariables.keySet())
      {
         desiredAccelerationYoVariables.get(joint).set(joint.getQddDesired());
      }
   }

   private void updateFrictionModel(FrictionModel model, FullRobotModel fullRobotModel)
   {
      OneDoFJoint[] joints = fullRobotModel.getOneDoFJoints();
      int numberOfJoint = joints.length;
      for (int i = 0; i < numberOfJoint; i++)
      {
         joints[i].setFrictionModel(model);
      }
   }

   private void updateFrictionCompensationEffectiveness(double effectiveness, FullRobotModel fullRobotModel)
   {
      OneDoFJoint[] joints = fullRobotModel.getOneDoFJoints();
      int numberOfJoint = joints.length;
      for (int i = 0; i < numberOfJoint; i++)
      {
         joints[i].setFrictionCompensationEffectiveness(effectiveness);
      }
   }

   private void updateUseBeforeTransmissionVelocityForFriction(boolean value, FullRobotModel fullRobotModel)
   {
      OneDoFJoint[] joints = fullRobotModel.getOneDoFJoints();
      int numberOfJoint = joints.length;
      for (int i = 0; i < numberOfJoint; i++)
      {
         joints[i].setUseBeforeTransmissionVelocityForFriction(value);
      }
   }

   private void addVariableChangedListenerToFrictionCompensationVariables(DoubleYoVariable effectiveness, EnumYoVariable<FrictionModel> model,
         BooleanYoVariable selectedVelocity)
   {
      VariableChangedListener changedModel = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            updateFrictionModel(frictionModelForAllJoints.getEnumValue(), fullRobotModel);
         }
      };

      VariableChangedListener changedEffectiveness = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            updateFrictionCompensationEffectiveness(frictionCompensationEffectiveness.getDoubleValue(), fullRobotModel);
         }
      };

      VariableChangedListener changedVelocity = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            updateUseBeforeTransmissionVelocityForFriction(useBeforeTransmissionVelocityForFriction.getBooleanValue(), fullRobotModel);
         }
      };

      effectiveness.addVariableChangedListener(changedEffectiveness);
      model.addVariableChangedListener(changedModel);
      selectedVelocity.addVariableChangedListener(changedVelocity);
   }

   private void initializeFrictionCompensationToZero()
   {
      frictionCompensationEffectiveness.set(0.0);
      frictionModelForAllJoints.set(FrictionModel.OFF);
      useBeforeTransmissionVelocityForFriction.set(false);
   }

   public void initialize()
   {
      // When you initialize into this controller, reset the estimator positions to current. Otherwise it might be in a bad state 
      // where the feet are all jacked up. For example, after falling and getting back up.
      inverseDynamicsCalculator.compute();
      momentumControlModuleBridge.initialize();
      planeContactWrenchProcessor.initialize();
      callUpdatables();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   public FramePoint2d getCoP(ContactablePlaneBody contactablePlaneBody)
   {
      return planeContactWrenchProcessor.getCops().get(contactablePlaneBody);
   }

   public void setPlaneContactCoefficientOfFriction(ContactablePlaneBody contactableBody, double coefficientOfFriction)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      yoPlaneContactState.setCoefficientOfFriction(coefficientOfFriction);
   }

   public void setPlaneContactStateNormalContactVector(ContactablePlaneBody contactableBody, FrameVector normalContactVector)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      yoPlaneContactState.setContactNormalVector(normalContactVector);
   }

   public void setPlaneContactState(ContactablePlaneBody contactableBody, boolean[] newContactPointStates)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      yoPlaneContactState.setContactPointsInContact(newContactPointStates);
   }

   public void setPlaneContactState(ContactablePlaneBody contactableBody, boolean[] newContactPointStates, FrameVector normalContactVector)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      yoPlaneContactState.setContactPointsInContact(newContactPointStates);
      yoPlaneContactState.setContactNormalVector(normalContactVector);
   }

   public void setPlaneContactStateFullyConstrained(ContactablePlaneBody contactableBody)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      yoPlaneContactState.setFullyConstrained();
   }

   public void setPlaneContactStateFullyConstrained(ContactablePlaneBody contactableBody, double coefficientOfFriction, FrameVector normalContactVector)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      yoPlaneContactState.setFullyConstrained();
      yoPlaneContactState.setCoefficientOfFriction(coefficientOfFriction);
      yoPlaneContactState.setContactNormalVector(normalContactVector);
   }

   public void setPlaneContactStateFree(ContactablePlaneBody contactableBody)
   {
      YoPlaneContactState yoPlaneContactState = yoPlaneContactStates.get(contactableBody);
      if (yoPlaneContactState != null)
         yoPlaneContactState.clear();
   }

   public ReferenceFrame getCenterOfMassFrame()
   {
      return centerOfMassFrame;
   }

   public void setDesiredSpatialAcceleration(int jacobianId, TaskspaceConstraintData taskspaceConstraintData)
   {
      GeometricJacobian jacobian = getJacobian(jacobianId);

      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.setDesiredSpatialAcceleration(jacobian, taskspaceConstraintData);
      }

      DesiredSpatialAccelerationCommand desiredSpatialAccelerationCommand = new DesiredSpatialAccelerationCommand(jacobian, taskspaceConstraintData);
      momentumControlModuleBridge.setDesiredSpatialAcceleration(desiredSpatialAccelerationCommand);
   }

   public void setDesiredPointAcceleration(int rootToEndEffectorJacobianId, FramePoint contactPoint, FrameVector desiredAcceleration)
   {
      GeometricJacobian rootToEndEffectorJacobian = getJacobian(rootToEndEffectorJacobianId);

      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.setDesiredPointAcceleration(rootToEndEffectorJacobian, contactPoint, desiredAcceleration);
      }

      DesiredPointAccelerationCommand desiredPointAccelerationCommand = new DesiredPointAccelerationCommand(rootToEndEffectorJacobian, contactPoint,
            desiredAcceleration);
      momentumControlModuleBridge.setDesiredPointAcceleration(desiredPointAccelerationCommand);
   }

   public void setDesiredPointAcceleration(int rootToEndEffectorJacobianId, FramePoint contactPoint, FrameVector desiredAcceleration,
         DenseMatrix64F selectionMatrix)
   {
      GeometricJacobian rootToEndEffectorJacobian = getJacobian(rootToEndEffectorJacobianId);

      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.setDesiredPointAcceleration(rootToEndEffectorJacobian, contactPoint, desiredAcceleration);
      }

      DesiredPointAccelerationCommand desiredPointAccelerationCommand = new DesiredPointAccelerationCommand(rootToEndEffectorJacobian, contactPoint,
            desiredAcceleration, selectionMatrix);
      momentumControlModuleBridge.setDesiredPointAcceleration(desiredPointAccelerationCommand);
   }

   public void setDesiredRateOfChangeOfMomentum(MomentumRateOfChangeData momentumRateOfChangeData)
   {
      if (momentumBasedControllerSpy != null)
      {
         momentumBasedControllerSpy.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);
      }

      DesiredRateOfChangeOfMomentumCommand desiredRateOfChangeOfMomentumCommand = new DesiredRateOfChangeOfMomentumCommand(momentumRateOfChangeData);
      momentumControlModuleBridge.setDesiredRateOfChangeOfMomentum(desiredRateOfChangeOfMomentumCommand);
   }

   public ReferenceFrame getPelvisZUpFrame()
   {
      return referenceFrames.getPelvisZUpFrame();
   }

   public EnumYoVariable<RobotSide> getUpcomingSupportLeg()
   {
      return upcomingSupportLeg;
   }

   public CommonHumanoidReferenceFrames getReferenceFrames()
   {
      return referenceFrames;
   }

   public DoubleYoVariable getYoTime()
   {
      return yoTime;
   }

   public double getGravityZ()
   {
      return gravity;
   }

   public double getControlDT()
   {
      return controlDT;
   }

   public FullRobotModel getFullRobotModel()
   {
      return fullRobotModel;
   }

   public TwistCalculator getTwistCalculator()
   {
      return twistCalculator;
   }

   public InverseDynamicsCalculator getInverseDynamicsCalculator()
   {
      return inverseDynamicsCalculator;
   }

   public CenterOfMassJacobian getCenterOfMassJacobian()
   {
      return centerOfMassJacobian;
   }

   public FrameVector getAdmissibleDesiredGroundReactionForceCopy()
   {
      return admissibleDesiredGroundReactionForce.getFrameVectorCopy();
   }

   public FrameVector getAdmissibleDesiredGroundReactionTorqueCopy()
   {
      return admissibleDesiredGroundReactionTorque.getFrameVectorCopy();
   }

   public SideDependentList<ContactablePlaneBody> getContactableFeet()
   {
      return feet;
   }

   public SideDependentList<ContactablePlaneBody> getContactableHands()
   {
      return hands;
   }

   public SideDependentList<ContactablePlaneBody> getContactablePlaneThighs()
   {
      return thighs;
   }

   public ContactablePlaneBody getContactablePelvis()
   {
      return pelvis;
   }

   public ContactablePlaneBody getContactablePelvisBack()
   {
      return pelvisBack;
   }

   public void getContactPoints(ContactablePlaneBody contactablePlaneBody, List<FramePoint> contactPointListToPack)
   {
      yoPlaneContactStates.get(contactablePlaneBody).getContactFramePointsInContact(contactPointListToPack);
   }

   public YoPlaneContactState getContactState(ContactablePlaneBody contactablePlaneBody)
   {
      return yoPlaneContactStates.get(contactablePlaneBody);
   }

   public List<? extends PlaneContactState> getPlaneContactStates()
   {
      return yoPlaneContactStateList;
   }
   
   public List<ContactablePlaneBody> getContactablePlaneBodyList()
   {
      return contactablePlaneBodyList;
   }

   public void clearContacts()
   {
      for (int i = 0; i < yoPlaneContactStateList.size(); i++)
      {
         yoPlaneContactStateList.get(i).clear();
      }
   }
   
   public void setSideOfFootUnderCoPControl(RobotSide side)
   {
      footUnderCoPControl.set(side);
   }
   
   public void setFeetCoPControlIsActive(boolean isActive)
   {
      feetCoPControlIsActive.set(isActive);
   }
   
   public void setFootCoPOffsetX(double offseX)
   {
      footCoPOffsetX.set(offseX);
   }
   
   public void setFootCoPOffsetY(double offseY)
   {
      footCoPOffsetY.set(offseY);
   }

   public void setMomentumControlModuleToUse(MomentumControlModuleType momentumControlModuleToUse)
   {
      momentumControlModuleBridge.setMomentumControlModuleToUse(momentumControlModuleToUse);
   }

   public int getOrCreateGeometricJacobian(RigidBody ancestor, RigidBody descendant, ReferenceFrame jacobianFrame)
   {
      return robotJacobianHolder.getOrCreateGeometricJacobian(ancestor, descendant, jacobianFrame);
   }

   public int getOrCreateGeometricJacobian(InverseDynamicsJoint[] joints, ReferenceFrame jacobianFrame)
   {
      return robotJacobianHolder.getOrCreateGeometricJacobian(joints, jacobianFrame);
   }

   /**
    * Return a jacobian previously created with the getOrCreate method using a jacobianId.
    * @param jacobianId
    * @return
    */
   public GeometricJacobian getJacobian(int jacobianId)
   {
      return robotJacobianHolder.getJacobian(jacobianId);
   }

   public SideDependentList<FootSwitchInterface> getFootSwitches()
   {
      return footSwitches;
   }

   public YoGraphicsListRegistry getDynamicGraphicObjectsListRegistry()
   {
      return yoGraphicsListRegistry;
   }

   public InverseDynamicsJoint[] getControlledJoints()
   {
      return controlledJoints;
   }
}
