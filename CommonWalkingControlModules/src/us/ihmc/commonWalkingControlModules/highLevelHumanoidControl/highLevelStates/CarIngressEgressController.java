package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.calculators.GainCalculator;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.desiredChestOrientation.DesiredChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controlModules.head.DesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.controlModules.pelvisOrientation.DesiredPelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.controllers.HandControllerInterface;
import us.ihmc.commonWalkingControlModules.controllers.LidarControllerInterface;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.DesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.TorusPoseProvider;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.OrientationTrajectoryData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.RootJointAngularAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantConfigurationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.CurrentOrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationInterpolationTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.SE3ConfigurationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.SettableOrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.StraightLinePositionTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.ThirdOrderPolynomialTrajectoryGenerator;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.Twist;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.util.EuclideanPositionController;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.trajectory.ConstantDoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleTrajectoryGenerator;

public class CarIngressEgressController extends AbstractHighLevelHumanoidControlPattern implements VariableChangedListener
{
   public final static HighLevelState controllerState = HighLevelState.INGRESS_EGRESS;

   private final DesiredFootPoseProvider footPoseProvider;

   private final DesiredPelvisPoseProvider pelvisPoseProvider;
   private final EuclideanPositionController pelvisPositionController;
   private final ReferenceFrame pelvisPositionControlFrame;
   private final GeometricJacobian pelvisJacobian;
   private final TaskspaceConstraintData pelvisTaskspaceConstraintData = new TaskspaceConstraintData();
   private final ChangeableConfigurationProvider desiredPelvisConfigurationProvider;
   private final StraightLinePositionTrajectoryGenerator pelvisPositionTrajectoryGenerator;
   private final OrientationInterpolationTrajectoryGenerator pelvisOrientationTrajectoryGenerator;
   private double pelvisTrajectoryStartTime = 0.0;

   private final DesiredChestOrientationProvider chestOrientationProvider;
   private final ReferenceFrame chestPositionControlFrame;
   private final SettableOrientationProvider desiredChestOrientation;
   private final OrientationInterpolationTrajectoryGenerator chestOrientationTrajectoryGenerator;
   private double chestTrajectoryStartTime = 0.0;
   
   
   private final BooleanYoVariable l_footDoHeelOff = new BooleanYoVariable("l_footDoHeelOff", registry);
   private final BooleanYoVariable r_footDoHeelOff = new BooleanYoVariable("r_footDoHeelOff", registry);
   private final SideDependentList<BooleanYoVariable> doHeelOff = new SideDependentList<BooleanYoVariable>(l_footDoHeelOff, r_footDoHeelOff);

   private final LinkedHashMap<ContactablePlaneBody, ChangeableConfigurationProvider> desiredFootConfigurationProviders = new LinkedHashMap<ContactablePlaneBody, ChangeableConfigurationProvider>();
   private final LinkedHashMap<ContactablePlaneBody, StraightLinePositionTrajectoryGenerator> swingPositionTrajectoryGenerators = new LinkedHashMap<ContactablePlaneBody, StraightLinePositionTrajectoryGenerator>();
   private final LinkedHashMap<ContactablePlaneBody, OrientationInterpolationTrajectoryGenerator> swingOrientationTrajectoryGenerators = new LinkedHashMap<ContactablePlaneBody, OrientationInterpolationTrajectoryGenerator>();

   private final LinkedHashMap<ContactablePlaneBody, YoPlaneContactState> contactStates;

   private final ConstantDoubleProvider trajectoryTimeProvider = new ConstantDoubleProvider(1.0);


   public CarIngressEgressController(SideDependentList<? extends ContactablePlaneBody> feet, SideDependentList<? extends ContactablePlaneBody> hands,
                                     RootJointAngularAccelerationControlModule rootJointAccelerationControlModule, DesiredHeadOrientationProvider desiredHeadOrientationProvider,
         MomentumBasedController momentumBasedController, WalkingControllerParameters walkingControllerParameters, DesiredHandPoseProvider handPoseProvider,
         TorusPoseProvider torusPoseProvider, DesiredFootPoseProvider footPoseProvider, DesiredPelvisPoseProvider pelvisPoseProvider,
         DesiredChestOrientationProvider chestOrientationProvider, SideDependentList<HandControllerInterface> handControllers,
         LidarControllerInterface lidarControllerInterface, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      super(feet, rootJointAccelerationControlModule, desiredHeadOrientationProvider, momentumBasedController, walkingControllerParameters, handPoseProvider,
            torusPoseProvider, handControllers, lidarControllerInterface, dynamicGraphicObjectsListRegistry, controllerState);

      this.footPoseProvider = footPoseProvider;
      this.contactStates = momentumBasedController.getContactStates();

      
      // Setup the pelvis trajectory generator 
      this.pelvisPoseProvider = pelvisPoseProvider;
      pelvisPositionControlFrame = fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint();
      this.pelvisPositionController = new EuclideanPositionController("pelvis", pelvisPositionControlFrame, registry);
      double kPelvis = 75.0;
      double dPelvis = GainCalculator.computeDerivativeGain(kPelvis, 2.0);
      pelvisPositionController.setProportionalGains(kPelvis, kPelvis, kPelvis);
      pelvisPositionController.setDerivativeGains(dPelvis, dPelvis, dPelvis);
      pelvisJacobian = new GeometricJacobian(fullRobotModel.getElevator(), fullRobotModel.getPelvis(), fullRobotModel.getPelvis().getBodyFixedFrame());

      final ConstantConfigurationProvider currentPelvisConfigurationProvider = new ConstantConfigurationProvider(new FramePose(pelvisPositionControlFrame));
      desiredPelvisConfigurationProvider = new ChangeableConfigurationProvider(new FramePose(pelvisPositionControlFrame));
      
      pelvisPositionTrajectoryGenerator = new StraightLinePositionTrajectoryGenerator("pelvis", worldFrame, trajectoryTimeProvider.getValue(),
            currentPelvisConfigurationProvider, desiredPelvisConfigurationProvider, registry, false);
      pelvisOrientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator("pelvis", worldFrame, trajectoryTimeProvider,
            currentPelvisConfigurationProvider, desiredPelvisConfigurationProvider, registry, false);
      
      // Setup the chest trajectory generator
      this.chestOrientationProvider = chestOrientationProvider;
      chestPositionControlFrame = fullRobotModel.getChest().getParentJoint().getFrameAfterJoint();
      final CurrentOrientationProvider currentChestOrientationProvider = new CurrentOrientationProvider(worldFrame, chestPositionControlFrame); // TODO: not sure about that
      desiredChestOrientation = new SettableOrientationProvider("chest", worldFrame, registry);
      chestOrientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator("chest", worldFrame, trajectoryTimeProvider, currentChestOrientationProvider,
            desiredChestOrientation, registry, false);
      
      setupFootControlModules();

      for (RobotSide robotSide : RobotSide.values)
      {
         doHeelOff.get(robotSide).addVariableChangedListener(this);
      }
   }

   protected void setupFootControlModules()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody foot = bipedFeet.get(robotSide);
         GeometricJacobian jacobian = legJacobians.get(robotSide);

         String bodyName = foot.getRigidBody().getName();
         String sideString = robotSide.getCamelCaseNameForStartOfExpression();

         final ConstantConfigurationProvider currentConfigurationProvider = new ConstantConfigurationProvider(new FramePose(foot.getBodyFrame()));
         final ChangeableConfigurationProvider desiredConfigurationProvider = new ChangeableConfigurationProvider(
               new FramePose(foot.getBodyFrame()));

         StraightLinePositionTrajectoryGenerator positionTrajectoryGenerator = new StraightLinePositionTrajectoryGenerator(bodyName, worldFrame,
               trajectoryTimeProvider.getValue(), currentConfigurationProvider, desiredConfigurationProvider, registry, false);

         OrientationInterpolationTrajectoryGenerator orientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator(bodyName, worldFrame,
               trajectoryTimeProvider, currentConfigurationProvider, desiredConfigurationProvider, registry, false);

         desiredFootConfigurationProviders.put(foot, desiredConfigurationProvider);
         swingPositionTrajectoryGenerators.put(foot, positionTrajectoryGenerator);
         swingOrientationTrajectoryGenerators.put(foot, orientationTrajectoryGenerator);

//         DoubleTrajectoryGenerator onToesTrajectory = createDummyDoubleTrajectoryGenerator();

         DoubleProvider onToesInitialPitchProvider = new ConstantDoubleProvider(0.0);
         DoubleProvider onToesInitialPitchVelocityProvider = new ConstantDoubleProvider(0.0);
         DoubleProvider onToesFinalPitchProvider = new ConstantDoubleProvider(Math.PI / 4.0);
         
         DoubleTrajectoryGenerator onToesTrajectory = new ThirdOrderPolynomialTrajectoryGenerator(sideString + bodyName, onToesInitialPitchProvider , onToesInitialPitchVelocityProvider,
               onToesFinalPitchProvider, trajectoryTimeProvider, registry);
         
         
         EndEffectorControlModule endEffectorControlModule = new EndEffectorControlModule(foot, jacobian, positionTrajectoryGenerator, null,
               orientationTrajectoryGenerator, onToesTrajectory, momentumBasedController, registry);
         footEndEffectorControlModules.put(foot, endEffectorControlModule);

      }
   }

   public void initialize()
   {
      super.initialize();

      FramePose currentPelvisPose = new FramePose(pelvisPositionControlFrame);
      desiredPelvisConfigurationProvider.set(currentPelvisPose);

      pelvisPositionTrajectoryGenerator.initialize();
      pelvisOrientationTrajectoryGenerator.initialize();
      
      FrameOrientation currentChestOrientation = new FrameOrientation(chestPositionControlFrame);
      currentChestOrientation.changeFrame(worldFrame);
      desiredChestOrientation.setOrientation(currentChestOrientation);
      
      chestOrientationTrajectoryGenerator.initialize();
   }

   protected void doPelvisControl()
   {
      if (pelvisPoseProvider.checkForNewPose())
      {
         desiredPelvisConfigurationProvider.set(pelvisPoseProvider.getDesiredPelvisPose());
         pelvisTrajectoryStartTime = yoTime.getDoubleValue();

         pelvisPositionTrajectoryGenerator.initialize();
         pelvisOrientationTrajectoryGenerator.initialize();
      }
      
      pelvisPositionTrajectoryGenerator.compute(yoTime.getDoubleValue() - pelvisTrajectoryStartTime);
      
      FramePoint desiredPosition = new FramePoint(pelvisPositionControlFrame);
      FrameVector desiredVelocity = new FrameVector(pelvisPositionControlFrame);
      FrameVector desiredPelvisAcceleration = new FrameVector(pelvisPositionControlFrame);
      pelvisPositionTrajectoryGenerator.get(desiredPosition);
      pelvisPositionTrajectoryGenerator.packVelocity(desiredVelocity);
      pelvisPositionTrajectoryGenerator.packAcceleration(desiredPelvisAcceleration);
      desiredPosition.changeFrame(pelvisPositionControlFrame);
      desiredVelocity.changeFrame(pelvisPositionControlFrame);
      desiredPelvisAcceleration.changeFrame(pelvisPositionControlFrame);
      
      FrameVector currentVelocity = new FrameVector(pelvisPositionControlFrame);
      Twist pelvisTwist = new Twist();
      fullRobotModel.getRootJoint().packJointTwist(pelvisTwist);
      pelvisTwist.changeFrame(pelvisPositionControlFrame);
      pelvisTwist.packLinearPart(currentVelocity);

      FrameVector feedForward = new FrameVector(pelvisPositionControlFrame);
      pelvisPositionController.compute(desiredPelvisAcceleration, desiredPosition, desiredVelocity, currentVelocity, feedForward);

      pelvisTaskspaceConstraintData.setLinearAcceleration(fullRobotModel.getPelvis().getBodyFixedFrame(), fullRobotModel.getElevatorFrame(),
            desiredPelvisAcceleration);
      momentumBasedController.setDesiredSpatialAcceleration(pelvisJacobian, pelvisTaskspaceConstraintData);

      pelvisOrientationTrajectoryGenerator.compute(yoTime.getDoubleValue() - pelvisTrajectoryStartTime);
      FrameOrientation desiredOrientation = new FrameOrientation(pelvisPositionControlFrame);
      FrameVector desiredAngularVelocity = new FrameVector(pelvisPositionControlFrame);
      FrameVector desiredAngularAcceleration = new FrameVector(pelvisPositionControlFrame);
      pelvisOrientationTrajectoryGenerator.get(desiredOrientation);
      pelvisOrientationTrajectoryGenerator.packAngularVelocity(desiredAngularVelocity);
      pelvisOrientationTrajectoryGenerator.packAngularAcceleration(desiredAngularAcceleration);

      // TODO: clean up
      super.doPelvisControl();
   }

   protected void doChestControl()
   {
      if (chestOrientationProvider.checkForNewPose())
      {
         desiredChestOrientation.setOrientation(chestOrientationProvider.getDesiredChestOrientation());
         chestTrajectoryStartTime = yoTime.getDoubleValue();
         
         chestOrientationTrajectoryGenerator.initialize();
      }
      
      chestOrientationTrajectoryGenerator.compute(yoTime.getDoubleValue() - chestTrajectoryStartTime);
      FrameOrientation desiredOrientation = new FrameOrientation(chestPositionControlFrame);
      FrameVector desiredAngularVelocity = new FrameVector(chestPositionControlFrame);
      FrameVector desiredAngularAcceleration = new FrameVector(chestPositionControlFrame);
      chestOrientationTrajectoryGenerator.get(desiredOrientation);
      chestOrientationTrajectoryGenerator.packAngularVelocity(desiredAngularVelocity);
      chestOrientationTrajectoryGenerator.packAngularAcceleration(desiredAngularAcceleration);
      
      chestOrientationManager.setDesireds(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);
      
      super.doChestControl();
   }
   
   protected void doFootControl()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody foot = bipedFeet.get(robotSide);

         if (footPoseProvider.checkForNewPose(robotSide))
         {
            FramePose newFootPose = footPoseProvider.getDesiredFootPose(robotSide);
            desiredFootConfigurationProviders.get(foot).set(newFootPose);
            footEndEffectorControlModules.get(foot).resetCurrentState();
            
            System.out.println(footPoseProvider.getDesiredFootPose(robotSide));
         }

         EndEffectorControlModule endEffectorControlModule = footEndEffectorControlModules.get(foot);
         FramePoint2d cop = momentumBasedController.getCoP(foot);
         endEffectorControlModule.setCenterOfPressure(cop);
      }

      super.doFootControl();
   }

   public void setContactablePlaneBodiesInContact(ContactablePlaneBody contactablePlaneBody, boolean inContact, double coefficientOfFriction)
   {
      if (!footEndEffectorControlModules.keySet().contains(contactablePlaneBody))
      {
         YoPlaneContactState contactState = momentumBasedController.getContactStates().get(contactablePlaneBody);
         if (inContact)
         {
            contactState.set(contactablePlaneBody.getContactPoints2d(), coefficientOfFriction);
         }
         else
         {
            contactState.set(new ArrayList<FramePoint2d>(), coefficientOfFriction);
         }
         return;
      }

      if (inContact)
      {
         setFlatFootContactState(contactablePlaneBody);
      }
      else
      {
         setContactStateForSwing(contactablePlaneBody);
      }
   }

   private void setOnToesContactState(ContactablePlaneBody contactableBody)
   {
      FrameVector normalContactVector = new FrameVector(worldFrame, 0.0, 0.0, 1.0);
      List<FramePoint> contactPoints = getContactPointsAccordingToFootConstraint(contactableBody, ConstraintType.TOES);
      List<FramePoint2d> contactPoints2d = getContactPoints2d(contactableBody, contactPoints);
      setContactState(contactableBody, contactPoints2d, ConstraintType.TOES, normalContactVector);
   }

   private void setFlatFootContactState(ContactablePlaneBody contactableBody)
   {
      FrameVector normalContactVector = new FrameVector(contactableBody.getPlaneFrame(), 0.0, 0.0, 1.0);
      setContactState(contactableBody, contactableBody.getContactPoints2d(), ConstraintType.FULL, normalContactVector);
   }

   private void setContactStateForSwing(ContactablePlaneBody contactableBody)
   {
      // Initialize desired foot pose to the actual, so no surprising behavior
      ReferenceFrame footFrame = footEndEffectorControlModules.get(contactableBody).getEndEffectorFrame();
      desiredFootConfigurationProviders.get(contactableBody).set(new FramePose(footFrame));

      FrameVector normalContactVector = new FrameVector(contactableBody.getPlaneFrame(), 0.0, 0.0, 1.0);
      setContactState(contactableBody, new ArrayList<FramePoint2d>(), ConstraintType.UNCONSTRAINED, normalContactVector);
   }

   private List<FramePoint> getContactPointsAccordingToFootConstraint(ContactablePlaneBody contactableBody, ConstraintType constraintType)
   {
      FrameVector direction = new FrameVector(contactableBody.getBodyFrame(), 1.0, 0.0, 0.0);
      if (constraintType == ConstraintType.HEEL)
         direction.scale(-1.0);

      return DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(contactableBody.getContactPoints(), direction, 2);
   }

   private List<FramePoint2d> getContactPoints2d(ContactablePlaneBody contactableBody, List<FramePoint> contactPoints)
   {
      List<FramePoint2d> contactPoints2d = new ArrayList<FramePoint2d>(contactPoints.size());
      for (FramePoint contactPoint : contactPoints)
      {
         contactPoint.changeFrame(contactableBody.getPlaneFrame());
         contactPoints2d.add(contactPoint.toFramePoint2d());
      }

      return contactPoints2d;
   }

   private void setContactState(ContactablePlaneBody contactableBody, List<FramePoint2d> contactPoints, ConstraintType constraintType,
         FrameVector normalContactVector)
   {
      if (contactPoints.size() == 0)
      {
         footEndEffectorControlModules.get(contactableBody).doSingularityEscapeBeforeTransitionToNextState();
      }
      YoPlaneContactState contactState = contactStates.get(contactableBody);
      contactState.set(contactPoints, coefficientOfFriction.getDoubleValue(), normalContactVector);
      updateEndEffectorControlModule(contactableBody, contactState, constraintType);
   }

   private void updateEndEffectorControlModule(ContactablePlaneBody contactablePlaneBody, PlaneContactState contactState, ConstraintType constraintType)
   {
      List<FramePoint2d> contactPoints = contactState.getContactPoints2d();
      footEndEffectorControlModules.get(contactablePlaneBody).setContactPoints(contactPoints, constraintType);
   }

   public void variableChanged(YoVariable v)
   {
      if (!(v instanceof BooleanYoVariable))
         return;

      for (RobotSide robotSide : RobotSide.values)
      {
         if (v.equals(doHeelOff.get(robotSide)))
         {
            if (doHeelOff.get(robotSide).getBooleanValue())
            {
               setOnToesContactState(bipedFeet.get(robotSide));
            }
            else
            {
               setFlatFootContactState(bipedFeet.get(robotSide));
            }
         }
      }
   }

   private DoubleTrajectoryGenerator createDummyDoubleTrajectoryGenerator()
   {
      DoubleTrajectoryGenerator onToesFixedTrajectory = new DoubleTrajectoryGenerator()
      {

         public double getValue()
         {
            return 0;
         }

         public boolean isDone()
         {
            return true;
         }

         public void initialize()
         {
         }

         public void compute(double time)
         {
         }

         public double getVelocity()
         {
            return 0;
         }

         public double getAcceleration()
         {
            return 0;
         }
      };
      return onToesFixedTrajectory;
   }

   private static class ChangeableConfigurationProvider implements SE3ConfigurationProvider
   {
      private final FramePose configuration;

      public ChangeableConfigurationProvider(FramePose initialConfiguration)
      {
         configuration = new FramePose(initialConfiguration);
      }

      @SuppressWarnings("unused")
      public void get(FramePose framePose)
      {
         framePose.setIncludingFrame(configuration);
      }

      public void get(FramePoint positionToPack)
      {
         configuration.getPosition(positionToPack);
      }

      public void get(FrameOrientation orientationToPack)
      {
         configuration.getOrientation(orientationToPack);
      }

      public void set(FramePose newPose)
      {
         configuration.setIncludingFrame(newPose);
      }
   }
}
