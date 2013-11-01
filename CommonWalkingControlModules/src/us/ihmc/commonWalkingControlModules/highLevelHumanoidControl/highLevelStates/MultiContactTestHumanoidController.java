package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controllers.LidarControllerInterface;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingManagers;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingProviders;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CoMBasedMomentumRateOfChangeControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.commonWalkingControlModules.trajectories.ChangeableConfigurationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantConfigurationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationInterpolationTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.PoseTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.StraightLinePositionTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.WrapperForPositionAndOrientationTrajectoryGenerators;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;

import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.trajectory.ConstantDoubleProvider;

public class MultiContactTestHumanoidController extends AbstractHighLevelHumanoidControlPattern
{
   public final static HighLevelState controllerState = HighLevelState.MULTI_CONTACT;

   private final YoFramePoint desiredCoMPosition = new YoFramePoint("desiredCoM", worldFrame, registry);

   private final DesiredFootPoseProvider footPoseProvider;

   private final LinkedHashMap<ContactablePlaneBody, ChangeableConfigurationProvider> desiredConfigurationProviders = new LinkedHashMap<ContactablePlaneBody,
                                                                                                                         ChangeableConfigurationProvider>();
   private final LinkedHashMap<ContactablePlaneBody, StraightLinePositionTrajectoryGenerator> swingPositionTrajectoryGenerators =
      new LinkedHashMap<ContactablePlaneBody, StraightLinePositionTrajectoryGenerator>();
   private final LinkedHashMap<ContactablePlaneBody, OrientationInterpolationTrajectoryGenerator> swingOrientationTrajectoryGenerators =
      new LinkedHashMap<ContactablePlaneBody, OrientationInterpolationTrajectoryGenerator>();


   private final ConstantDoubleProvider trajectoryTimeProvider = new ConstantDoubleProvider(1.0);
   private final CoMBasedMomentumRateOfChangeControlModule momentumRateOfChangeControlModule;
   private final OneDoFJoint[] neckJoints;

   public MultiContactTestHumanoidController(VariousWalkingProviders variousWalkingProviders, VariousWalkingManagers variousWalkingManagers,
           CoMBasedMomentumRateOfChangeControlModule momentumRateOfChangeControlModule,
           MomentumBasedController momentumBasedController,
           WalkingControllerParameters walkingControllerParameters, 
           LidarControllerInterface lidarControllerInterface,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      super(variousWalkingProviders, variousWalkingManagers, momentumBasedController, walkingControllerParameters,
            lidarControllerInterface, dynamicGraphicObjectsListRegistry, controllerState);

      this.footPoseProvider = variousWalkingProviders.getDesiredFootPoseProvider();
      this.momentumRateOfChangeControlModule = momentumRateOfChangeControlModule;

      RigidBody pelvis = fullRobotModel.getPelvis();
      RigidBody chest = fullRobotModel.getChest();

      GeometricJacobian spineJacobian = new GeometricJacobian(pelvis, chest, chest.getBodyFixedFrame());
      variousWalkingManagers.getChestOrientationManager().setUp(pelvis, spineJacobian, 100.0, 100.0, 100.0, 20.0, 20.0, 20.0);

      this.neckJoints = ScrewTools.filterJoints(ScrewTools.createJointPath(chest, fullRobotModel.getHead()), OneDoFJoint.class);

      setupFootControlModules();
   }

   protected void setupFootControlModules()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody foot = feet.get(robotSide);
         GeometricJacobian jacobian = legJacobians.get(robotSide);

         String bodyName = foot.getRigidBody().getName();


         final ConstantConfigurationProvider currentConfigurationProvider = new ConstantConfigurationProvider(new FramePose(foot.getBodyFrame()));
         final ChangeableConfigurationProvider desiredConfigurationProvider =
            new ChangeableConfigurationProvider(footPoseProvider.getDesiredFootPose(robotSide));

         StraightLinePositionTrajectoryGenerator positionTrajectoryGenerator = new StraightLinePositionTrajectoryGenerator(bodyName, worldFrame,
                                                                                  trajectoryTimeProvider, currentConfigurationProvider,
                                                                                  desiredConfigurationProvider, registry);

         OrientationInterpolationTrajectoryGenerator orientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator(bodyName, worldFrame,
                                                                                         trajectoryTimeProvider, currentConfigurationProvider,
                                                                                         desiredConfigurationProvider, registry);

         PoseTrajectoryGenerator poseTrajectoryGenerator = new WrapperForPositionAndOrientationTrajectoryGenerators(positionTrajectoryGenerator, orientationTrajectoryGenerator);

         desiredConfigurationProviders.put(foot, desiredConfigurationProvider);
         swingPositionTrajectoryGenerators.put(foot, positionTrajectoryGenerator);
         swingOrientationTrajectoryGenerators.put(foot, orientationTrajectoryGenerator);

         OneDoFJoint kneeJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.KNEE);
         EndEffectorControlModule endEffectorControlModule = new EndEffectorControlModule(controlDT, foot, jacobian, kneeJoint, poseTrajectoryGenerator, null,
                                                                null, momentumBasedController, registry);
         endEffectorControlModule.setSwingGains(100.0, 200.0, 200.0, 1.0);

         footEndEffectorControlModules.put(foot, endEffectorControlModule);
      }
   }

   public void initialize()
   {
      super.initialize();

      FramePoint currentCoM = new FramePoint(momentumBasedController.getCenterOfMassFrame());
      currentCoM.changeFrame(desiredCoMPosition.getReferenceFrame());
      desiredCoMPosition.set(currentCoM);

      FrameOrientation currentPelvisOrientaton = new FrameOrientation(referenceFrames.getPelvisFrame());
      currentPelvisOrientaton.changeFrame(desiredPelvisOrientation.getReferenceFrame());
      desiredPelvisOrientation.set(currentPelvisOrientaton);

      // keep desired pelvis orientation as it is
      desiredPelvisAngularVelocity.set(0.0, 0.0, 0.0);
      desiredPelvisAngularAcceleration.set(0.0, 0.0, 0.0);
   }

   protected void doCoMControl()
   {
      momentumRateOfChangeControlModule.getDesiredCoMPositionInputPort().setData(desiredCoMPosition.getFramePointCopy());

      momentumRateOfChangeControlModule.startComputation();
      momentumRateOfChangeControlModule.waitUntilComputationIsDone();
      MomentumRateOfChangeData momentumRateOfChangeData = momentumRateOfChangeControlModule.getMomentumRateOfChangeOutputPort().getData();
      momentumBasedController.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);
   }

   protected void doFootControl()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody foot = feet.get(robotSide);

         if (footPoseProvider.checkForNewPose(robotSide))
         {
            FramePose newFootPose = footPoseProvider.getDesiredFootPose(robotSide);
            desiredConfigurationProviders.get(foot).set(newFootPose);
            footEndEffectorControlModules.get(foot).resetCurrentState();
         }
      }

      super.doFootControl();
   }

   protected void doHeadControl()
   {
      for (OneDoFJoint neckJoint : neckJoints)
      {
         momentumBasedController.doPDControl(neckJoint, 100.0, 20.0, 0.0, 0.0, 10.0, 1000.0);
      }
   }

   public void setFootInContact(RobotSide robotSide, boolean inContact)
   {
      if (feet == null)
         return;

      if (inContact)
      {
         setFlatFootContactState(feet.get(robotSide));
      }
      else
      {
         setContactStateForSwing(feet.get(robotSide));
      }
   }

   public void setHandInContact(RobotSide robotSide, boolean inContact)
   {
      ContactablePlaneBody handPalm = handPalms.get(robotSide);

      if (inContact)
      {
         // TODO: If we know the surface normal here, use it.
         FrameVector normalContactVector = null;
         momentumBasedController.setPlaneContactStateFullyConstrained(handPalm, coefficientOfFriction.getDoubleValue(), normalContactVector);
      }
      else
      {
         momentumBasedController.setPlaneContactStateFree(handPalm);
      }
   }

   private void setFlatFootContactState(ContactablePlaneBody contactableBody)
   {
      footEndEffectorControlModules.get(contactableBody).setContactState(ConstraintType.FULL);
   }

   private void setContactStateForSwing(ContactablePlaneBody contactableBody)
   {
      // Initialize desired foot pose to the actual, so no surprising behavior
      ReferenceFrame footFrame = footEndEffectorControlModules.get(contactableBody).getEndEffectorFrame();
      desiredConfigurationProviders.get(contactableBody).set(new FramePose(footFrame));

      footEndEffectorControlModules.get(contactableBody).doSingularityEscapeBeforeTransitionToNextState();
      footEndEffectorControlModules.get(contactableBody).setContactState(ConstraintType.UNCONSTRAINED);
   }
}
