package us.ihmc.commonWalkingControlModules.momentumBasedController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait.Updatable;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.outputs.ProcessedOutputsInterface;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.stateEstimation.DesiredCoMAndAngularAccelerationGrabber;
import us.ihmc.commonWalkingControlModules.stateEstimation.PointPositionGrabber;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimationDataFromControllerSink;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
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

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class MomentumBasedController implements RobotController
{
   private final String name = getClass().getSimpleName();
   protected final YoVariableRegistry registry = new YoVariableRegistry(name);

   protected final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   protected final ReferenceFrame elevatorFrame;
   protected final ReferenceFrame centerOfMassFrame;

   protected final FullRobotModel fullRobotModel;
   protected final CenterOfMassJacobian centerOfMassJacobian;
   protected final CommonWalkingReferenceFrames referenceFrames;
   protected final TwistCalculator twistCalculator;
   protected final List<ContactablePlaneBody> contactablePlaneBodies;

   private final LinkedHashMap<ContactablePlaneBody, DoubleYoVariable> normalTorques = new LinkedHashMap<ContactablePlaneBody, DoubleYoVariable>();
   private final LinkedHashMap<ContactablePlaneBody, DoubleYoVariable> groundReactionForceMagnitudes = new LinkedHashMap<ContactablePlaneBody, DoubleYoVariable>();
   protected final LinkedHashMap<ContactablePlaneBody, YoPlaneContactState> contactStates = new LinkedHashMap<ContactablePlaneBody, YoPlaneContactState>();
   protected final ArrayList<Updatable> updatables = new ArrayList<Updatable>();
   protected final DoubleYoVariable yoTime;
   protected final double controlDT;
   protected final double gravity;

   protected final YoFrameVector finalDesiredPelvisLinearAcceleration;
   protected final YoFrameVector finalDesiredPelvisAngularAcceleration;
   protected final YoFrameVector desiredPelvisForce;
   protected final YoFrameVector desiredPelvisTorque;

   protected final YoFrameVector admissibleDesiredGroundReactionTorque;
   protected final YoFrameVector admissibleDesiredGroundReactionForce;
   protected final YoFrameVector groundReactionTorqueCheck;
   protected final YoFrameVector groundReactionForceCheck;

   private final LinkedHashMap<ContactablePlaneBody, YoFramePoint> centersOfPressureWorld = new LinkedHashMap<ContactablePlaneBody, YoFramePoint>();
   private final LinkedHashMap<ContactablePlaneBody, YoFramePoint2d> centersOfPressure2d = new LinkedHashMap<ContactablePlaneBody, YoFramePoint2d>();

   protected final LinkedHashMap<OneDoFJoint, DoubleYoVariable> desiredAccelerationYoVariables = new LinkedHashMap<OneDoFJoint, DoubleYoVariable>();

   protected final ProcessedOutputsInterface processedOutputs;
   protected final MomentumRateOfChangeControlModule momentumRateOfChangeControlModule;
   protected final RootJointAccelerationControlModule rootJointAccelerationControlModule;
   protected final InverseDynamicsCalculator inverseDynamicsCalculator;

   private final DesiredCoMAndAngularAccelerationGrabber desiredCoMAndAngularAccelerationGrabber;
   protected final PointPositionGrabber pointPositionGrabber;

   protected final MomentumControlModule momentumControlModule;

   protected final SpatialForceVector gravitationalWrench;
   protected final EnumYoVariable<RobotSide> upcomingSupportLeg = EnumYoVariable.create("upcomingSupportLeg", "", RobotSide.class, registry, true);    // FIXME: not general enough; this should not be here

   private final CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();
   private final Map<ContactablePlaneBody, FramePoint2d> cops = new LinkedHashMap<ContactablePlaneBody, FramePoint2d>();
   private final TaskspaceConstraintData rootJointTaskSpaceConstraintData = new TaskspaceConstraintData();
   private final SpatialAccelerationVector rootJointAcceleration;
   private final DenseMatrix64F rootJointAccelerationMatrix = new DenseMatrix64F(SpatialAccelerationVector.SIZE, 1);
   private final DenseMatrix64F rootJointNullspaceMultipliers = new DenseMatrix64F(0, 1);
   private final DenseMatrix64F rootJointSelectionMatrix = new DenseMatrix64F(1, 1);

   public MomentumBasedController(RigidBody estimationLink, ReferenceFrame estimationFrame, FullRobotModel fullRobotModel,
                                  CenterOfMassJacobian centerOfMassJacobian, CommonWalkingReferenceFrames referenceFrames, DoubleYoVariable yoTime,
                                  double gravityZ, TwistCalculator twistCalculator, Collection<? extends ContactablePlaneBody> contactablePlaneBodies,
                                  double controlDT, ProcessedOutputsInterface processedOutputs, MomentumControlModule momentumControlModule,
                                  ArrayList<Updatable> updatables, MomentumRateOfChangeControlModule momentumRateOfChangeControlModule,
                                  RootJointAccelerationControlModule rootJointAccelerationControlModule,
                                  StateEstimationDataFromControllerSink stateEstimationDataFromControllerSink,
                                  DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      centerOfMassFrame = referenceFrames.getCenterOfMassFrame();

      this.momentumControlModule = momentumControlModule;

      MathTools.checkIfInRange(gravityZ, 0.0, Double.POSITIVE_INFINITY);

      this.fullRobotModel = fullRobotModel;
      this.centerOfMassJacobian = centerOfMassJacobian;
      this.referenceFrames = referenceFrames;
      this.twistCalculator = twistCalculator;
      this.contactablePlaneBodies = new ArrayList<ContactablePlaneBody>(contactablePlaneBodies);
      this.controlDT = controlDT;
      this.gravity = gravityZ;
      this.yoTime = yoTime;

      RigidBody elevator = fullRobotModel.getElevator();

      this.processedOutputs = processedOutputs;
      this.inverseDynamicsCalculator = new InverseDynamicsCalculator(twistCalculator, gravityZ);

      double totalMass = TotalMassCalculator.computeSubTreeMass(elevator);

      if (stateEstimationDataFromControllerSink != null)
      {
         this.desiredCoMAndAngularAccelerationGrabber = new DesiredCoMAndAngularAccelerationGrabber(stateEstimationDataFromControllerSink, estimationLink,
                 estimationFrame, totalMass);
         
         this.pointPositionGrabber = new PointPositionGrabber(stateEstimationDataFromControllerSink, registry, controlDT, 0.0, 0.01);
      }
      else
      {
         this.desiredCoMAndAngularAccelerationGrabber = null;
         this.pointPositionGrabber = null;
      }

      gravitationalWrench = new SpatialForceVector(centerOfMassFrame, new Vector3d(0.0, 0.0, totalMass * gravityZ), new Vector3d());


      ReferenceFrame pelvisFrame = referenceFrames.getPelvisFrame();
      this.finalDesiredPelvisLinearAcceleration = new YoFrameVector("finalDesiredPelvisLinearAcceleration", "", pelvisFrame, registry);
      this.finalDesiredPelvisAngularAcceleration = new YoFrameVector("finalDesiredPelvisAngularAcceleration", "", pelvisFrame, registry);
      this.desiredPelvisForce = new YoFrameVector("desiredPelvisForce", "", centerOfMassFrame, registry);
      this.desiredPelvisTorque = new YoFrameVector("desiredPelvisTorque", "", centerOfMassFrame, registry);


      this.admissibleDesiredGroundReactionTorque = new YoFrameVector("admissibleDesiredGroundReactionTorque", centerOfMassFrame, registry);
      this.admissibleDesiredGroundReactionForce = new YoFrameVector("admissibleDesiredGroundReactionForce", centerOfMassFrame, registry);

      this.groundReactionTorqueCheck = new YoFrameVector("groundReactionTorqueCheck", centerOfMassFrame, registry);
      this.groundReactionForceCheck = new YoFrameVector("groundReactionForceCheck", centerOfMassFrame, registry);


      for (ContactablePlaneBody contactableBody : contactablePlaneBodies)
      {
         DoubleYoVariable forceMagnitude = new DoubleYoVariable(contactableBody.getRigidBody().getName() + "ForceMagnitude", registry);
         groundReactionForceMagnitudes.put(contactableBody, forceMagnitude);

         DoubleYoVariable normalTorque = new DoubleYoVariable(contactableBody.getRigidBody().getName() + "NormalTorque", registry);
         normalTorques.put(contactableBody, normalTorque);

         String copName = contactableBody.getRigidBody().getName() + "CoP";
         String listName = "cops";

         YoFramePoint2d cop2d = new YoFramePoint2d(copName + "2d", "", contactableBody.getPlaneFrame(), registry);
         centersOfPressure2d.put(contactableBody, cop2d);

         YoFramePoint cop = new YoFramePoint(copName, ReferenceFrame.getWorldFrame(), registry);
         centersOfPressureWorld.put(contactableBody, cop);

         if (dynamicGraphicObjectsListRegistry != null)
         {
            DynamicGraphicPosition copViz = cop.createDynamicGraphicPosition(copName, 0.005, YoAppearance.Navy(), DynamicGraphicPosition.GraphicType.BALL);
            dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject(listName, copViz);
            dynamicGraphicObjectsListRegistry.registerArtifact(listName, copViz.createArtifact());
         }
      }

      if (updatables != null)
      {
         this.updatables.addAll(updatables);
      }


      elevatorFrame = fullRobotModel.getElevatorFrame();

      for (ContactablePlaneBody contactablePlaneBody : contactablePlaneBodies)
      {
         RigidBody rigidBody = contactablePlaneBody.getRigidBody();
         YoPlaneContactState contactState = new YoPlaneContactState(rigidBody.getName(), contactablePlaneBody.getBodyFrame(),
                                               contactablePlaneBody.getPlaneFrame(), registry);
         double coefficientOfFriction = 1.0;    // TODO: magic number...
         contactState.set(contactablePlaneBody.getContactPoints2d(), coefficientOfFriction);    // initialize with flat 'feet'
         contactStates.put(contactablePlaneBody, contactState);
      }

      InverseDynamicsJoint[] joints = ScrewTools.computeSupportAndSubtreeJoints(fullRobotModel.getRootJoint().getSuccessor());
      for (InverseDynamicsJoint joint : joints)
      {
         if (joint instanceof OneDoFJoint)
         {
            desiredAccelerationYoVariables.put((OneDoFJoint) joint, new DoubleYoVariable(joint.getName() + "qdd_d", registry));
         }
      }

      this.momentumRateOfChangeControlModule = momentumRateOfChangeControlModule;
      this.rootJointAccelerationControlModule = rootJointAccelerationControlModule;

      this.rootJointAcceleration = new SpatialAccelerationVector();
   }

   protected static double computeDesiredAcceleration(double k, double d, double qDesired, double qdDesired, OneDoFJoint joint)
   {
      return k * (qDesired - joint.getQ()) + d * (qdDesired - joint.getQd());
   }

   // TODO: visibility changed for "public"
   public void setExternalHandWrench(RobotSide robotSide, Wrench handWrench)
   {
      inverseDynamicsCalculator.setExternalWrench(fullRobotModel.getHand(robotSide), handWrench);
   }

   public void doMotionControl()
   {
   }
   
   // TODO: Temporary method for a big refactor allowing switching between high level behaviors
   public void doPrioritaryControl()
   {
      callUpdatables();

      inverseDynamicsCalculator.reset();
      momentumControlModule.reset();
   }
   
   // TODO: Temporary method for a big refactor allowing switching between high level behaviors   
   public void doSecondaryControl()
   {
      rootJointAccelerationControlModule.startComputation();
      rootJointAccelerationControlModule.waitUntilComputationIsDone();
      RootJointAccelerationData rootJointAccelerationData = rootJointAccelerationControlModule.getRootJointAccelerationOutputPort().getData();

      momentumRateOfChangeControlModule.startComputation();
      momentumRateOfChangeControlModule.waitUntilComputationIsDone();
      MomentumRateOfChangeData momentumRateOfChangeData = momentumRateOfChangeControlModule.getMomentumRateOfChangeOutputPort().getData();


      CommonOps.mult(rootJointAccelerationData.getAccelerationSubspace(), rootJointAccelerationData.getAccelerationMultipliers(), rootJointAccelerationMatrix);
      rootJointAcceleration.set(rootJointAccelerationData.getBodyFrame(), rootJointAccelerationData.getBaseFrame(), rootJointAccelerationData.getExpressedInFrame(), rootJointAccelerationMatrix, 0);
      rootJointAcceleration.changeFrameNoRelativeMotion(rootJointAccelerationData.getBodyFrame());

      DenseMatrix64F accelerationSubspace = rootJointAccelerationData.getAccelerationSubspace();
      rootJointSelectionMatrix.reshape(accelerationSubspace.getNumCols(), accelerationSubspace.getNumRows());
      CommonOps.transpose(accelerationSubspace, rootJointSelectionMatrix);
      rootJointTaskSpaceConstraintData.set(rootJointAcceleration, rootJointNullspaceMultipliers, rootJointSelectionMatrix);
      momentumControlModule.setDesiredSpatialAcceleration(fullRobotModel.getRootJoint().getMotionSubspace(), rootJointTaskSpaceConstraintData);
      momentumControlModule.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);
      momentumControlModule.compute(this.contactStates, upcomingSupportLeg.getEnumValue());

      SpatialForceVector desiredCentroidalMomentumRate = momentumControlModule.getDesiredCentroidalMomentumRate();

      Map<? extends ContactablePlaneBody, Wrench> externalWrenches = momentumControlModule.getExternalWrenches();
      cops.clear();
      for (ContactablePlaneBody contactablePlaneBody : contactablePlaneBodies)
      {
         PlaneContactState contactState = this.contactStates.get(contactablePlaneBody);
         List<FramePoint> footContactPoints = contactState.getContactPoints();

         if (footContactPoints.size() > 0)
         {
            Wrench wrench = externalWrenches.get(contactablePlaneBody);
            inverseDynamicsCalculator.setExternalWrench(contactablePlaneBody.getRigidBody(), wrench);

            FrameVector force = wrench.getLinearPartAsFrameVectorCopy();

            FramePoint2d cop = new FramePoint2d(ReferenceFrame.getWorldFrame());
            double normalTorque = centerOfPressureResolver.resolveCenterOfPressureAndNormalTorque(cop, wrench, contactablePlaneBody.getPlaneFrame());
            cops.put(contactablePlaneBody, cop);

            centersOfPressure2d.get(contactablePlaneBody).set(cop);

            FramePoint cop3d = cop.toFramePoint();
            cop3d.changeFrame(ReferenceFrame.getWorldFrame());

            centersOfPressureWorld.get(contactablePlaneBody).set(cop3d);
            groundReactionForceMagnitudes.get(contactablePlaneBody).set(force.length());
            normalTorques.get(contactablePlaneBody).set(normalTorque);
         }
         else
         {
            groundReactionForceMagnitudes.get(contactablePlaneBody).set(0.0);

//          centersOfPressure2d.get(contactablePlaneBody).set(Double.NaN, Double.NaN);
            centersOfPressureWorld.get(contactablePlaneBody).setToNaN();
         }
      }

      SpatialForceVector totalGroundReactionWrench = new SpatialForceVector(centerOfMassFrame);
      Wrench admissibleGroundReactionWrench = TotalWrenchCalculator.computeTotalWrench(externalWrenches.values(),
                                                 totalGroundReactionWrench.getExpressedInFrame());
      admissibleDesiredGroundReactionTorque.set(admissibleGroundReactionWrench.getAngularPartCopy());
      admissibleDesiredGroundReactionForce.set(admissibleGroundReactionWrench.getLinearPartCopy());

      SpatialForceVector groundReactionWrenchCheck = inverseDynamicsCalculator.computeTotalExternalWrench(centerOfMassFrame);
      groundReactionTorqueCheck.set(groundReactionWrenchCheck.getAngularPartCopy());
      groundReactionForceCheck.set(groundReactionWrenchCheck.getLinearPartCopy());

      if (desiredCoMAndAngularAccelerationGrabber != null)
         this.desiredCoMAndAngularAccelerationGrabber.set(inverseDynamicsCalculator.getSpatialAccelerationCalculator(), desiredCentroidalMomentumRate);

      if (pointPositionGrabber != null)
         pointPositionGrabber.set(contactStates, cops);

      inverseDynamicsCalculator.compute();

      if (processedOutputs != null)
         fullRobotModel.setTorques(processedOutputs);
      updateYoVariables();
   }
   
   public final void doControl()
   {
      doPrioritaryControl();
      doMotionControl();
      doSecondaryControl();
   }

   protected void resetGroundReactionWrenchFilter()
   {
      momentumControlModule.resetGroundReactionWrenchFilter();
   }

   private void callUpdatables()
   {
      double time = yoTime.getDoubleValue();
      for (Updatable updatable : updatables)
      {
         updatable.update(time);
      }
   }

   // TODO: visibility changed for "public"
   public ReferenceFrame getHandFrame(RobotSide robotSide)
   {
      return fullRobotModel.getHand(robotSide).getBodyFixedFrame();
   }

   public void addUpdatable(Updatable updatable)
   {
      updatables.add(updatable);
   }

   // TODO: visibility changed for "public"
   public void doPDControl(OneDoFJoint[] joints, double k, double d)
   {
      for (OneDoFJoint joint : joints)
      {
         doPDControl(joint, k, d, 0.0, 0.0);
      }
   }

   // TODO: visibility changed for "public"
   public void doPDControl(OneDoFJoint joint, double k, double d, double desiredPosition, double desiredVelocity)
   {
      double desiredAcceleration = computeDesiredAcceleration(k, d, desiredPosition, desiredVelocity, joint);
      setOneDoFJointAcceleration(joint, desiredAcceleration);
   }

   protected void setOneDoFJointAcceleration(OneDoFJoint joint, double desiredAcceleration)
   {
      DenseMatrix64F jointAcceleration = new DenseMatrix64F(joint.getDegreesOfFreedom(), 1);
      jointAcceleration.set(0, 0, desiredAcceleration);
      momentumControlModule.setDesiredJointAcceleration(joint, jointAcceleration);
   }

   private void updateYoVariables()
   {
      SpatialAccelerationVector pelvisAcceleration = new SpatialAccelerationVector();
      fullRobotModel.getRootJoint().packDesiredJointAcceleration(pelvisAcceleration);

      finalDesiredPelvisAngularAcceleration.checkReferenceFrameMatch(pelvisAcceleration.getExpressedInFrame());
      finalDesiredPelvisAngularAcceleration.set(pelvisAcceleration.getAngularPartCopy());

      finalDesiredPelvisLinearAcceleration.checkReferenceFrameMatch(pelvisAcceleration.getExpressedInFrame());
      finalDesiredPelvisLinearAcceleration.set(pelvisAcceleration.getLinearPartCopy());

      Wrench pelvisJointWrench = new Wrench();
      fullRobotModel.getRootJoint().packWrench(pelvisJointWrench);
      pelvisJointWrench.changeFrame(referenceFrames.getCenterOfMassFrame());
      desiredPelvisForce.set(pelvisJointWrench.getLinearPartCopy());
      desiredPelvisTorque.set(pelvisJointWrench.getAngularPartCopy());

      for (OneDoFJoint joint : desiredAccelerationYoVariables.keySet())
      {
         desiredAccelerationYoVariables.get(joint).set(joint.getQddDesired());
      }
   }

   public void initialize()
   {
      inverseDynamicsCalculator.compute();
      momentumControlModule.initialize();
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

   // TODO: visibility changed for "public"
   public FramePoint2d getCoP(ContactablePlaneBody contactablePlaneBody)
   {
      return centersOfPressure2d.get(contactablePlaneBody).getFramePoint2dCopy();
   }

   
   // TODO: Following has been added for big refactor. Need to be checked.
   
   public LinkedHashMap<ContactablePlaneBody, YoPlaneContactState> getContactStates()
   {
      return contactStates;
   }

   public List<ContactablePlaneBody> getContactablePlaneBodies()
   {
      return contactablePlaneBodies;
   }

   public ReferenceFrame getCenterOfMassFrame()
   {
      return centerOfMassFrame;
   }

   public void setDesiredSpatialAcceleration(GeometricJacobian jacobian, TaskspaceConstraintData taskspaceConstraintData)
   {
      momentumControlModule.setDesiredSpatialAcceleration(jacobian, taskspaceConstraintData);
   }

   public void setDesiredJointAcceleration(OneDoFJoint joint, DenseMatrix64F jointAcceleration)
   {
      momentumControlModule.setDesiredJointAcceleration(joint, jointAcceleration);
   }

   public ReferenceFrame getPelvisZUpFrame()
   {
      return referenceFrames.getPelvisZUpFrame();
   }
}
