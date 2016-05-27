package us.ihmc.humanoidBehaviors.behaviors;

import us.ihmc.SdfLoader.SDFFullHumanoidRobotModel;
import us.ihmc.humanoidBehaviors.behaviors.coactiveElements.PickUpBallBehaviorCoactiveElement.BehaviorState;
import us.ihmc.humanoidBehaviors.behaviors.coactiveElements.PickUpBallBehaviorCoactiveElementBehaviorSide;
import us.ihmc.humanoidBehaviors.behaviors.primitives.*;
import us.ihmc.humanoidBehaviors.coactiveDesignFramework.CoactiveElement;
import us.ihmc.humanoidBehaviors.communication.BehaviorCommunicationBridge;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.taskExecutor.*;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HandConfiguration;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.sensing.DepthDataFilterParameters;
import us.ihmc.humanoidRobotics.communication.packets.walking.GoHomeMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.GoHomeMessage.BodyPart;
import us.ihmc.humanoidRobotics.communication.packets.walking.HeadTrajectoryMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.ihmcPerception.vision.shapes.HSVRange;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.*;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.taskExecutor.PipeLine;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;

public class PickUpBallBehavior extends BehaviorInterface
{
   private final PickUpBallBehaviorCoactiveElementBehaviorSide coactiveElement;

   private final ArrayList<BehaviorInterface> behaviors = new ArrayList<BehaviorInterface>();
   private final EnableLidarBehavior enableLidarBehavior;
   private final SetLidarParametersBehavior setLidarParametersBehavior;
   private final ClearLidarBehavior clearLidarBehavior;
   private final SphereDetectionBehavior initialSphereDetectionBehavior;
   private final WaitForUserValidationBehavior waitForUserValidationBehavior;
   private final WalkToLocationBehavior walkToLocationBehavior;
   private final ChestTrajectoryBehavior chestTrajectoryBehavior;
   private final HandDesiredConfigurationBehavior handDesiredConfigurationBehavior;
   private final WholeBodyInverseKinematicsBehavior wholeBodyBehavior;
   private final GoHomeBehavior chestGoHomeBehavior;
   private final GoHomeBehavior pelvisGoHomeBehavior;
   private final GoHomeBehavior armGoHomeLeftBehavior;
   private final GoHomeBehavior armGoHomeRightBehavior;
   private final ArmTrajectoryBehavior armTrajectoryBehavior;

   private final PipeLine<BehaviorInterface> pipeLine = new PipeLine<>();

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final DoubleYoVariable yoTime;
   private final ReferenceFrame midZupFrame;
   private final double standingDistance = 0.5;

   private HumanoidReferenceFrames referenceFrames;

   public PickUpBallBehavior(BehaviorCommunicationBridge outgoingCommunicationBridge, DoubleYoVariable yoTime, BooleanYoVariable yoDoubleSupport,
         SDFFullHumanoidRobotModel fullRobotModel, HumanoidReferenceFrames referenceFrames, WholeBodyControllerParameters wholeBodyControllerParameters)
   {
      super(outgoingCommunicationBridge);
      this.yoTime = yoTime;
      midZupFrame = referenceFrames.getMidFeetZUpFrame();
      this.referenceFrames = referenceFrames;

      // create sub-behaviors:
      setLidarParametersBehavior = new SetLidarParametersBehavior(outgoingCommunicationBridge);
      behaviors.add(setLidarParametersBehavior);

      enableLidarBehavior = new EnableLidarBehavior(outgoingCommunicationBridge);
      behaviors.add(enableLidarBehavior);

      clearLidarBehavior = new ClearLidarBehavior(outgoingCommunicationBridge);
      behaviors.add(clearLidarBehavior);

      initialSphereDetectionBehavior = new SphereDetectionBehavior(outgoingCommunicationBridge, referenceFrames); //  new BlobFilteredSphereDetectionBehavior(outgoingCommunicationBridge, referenceFrames, fullRobotModel);
      behaviors.add(initialSphereDetectionBehavior);
      //      finalSphereDetectionBehavior = new SphereDetectionBehavior(outgoingCommunicationBridge, referenceFrames);
      //      behaviors.add(finalSphereDetectionBehavior);

      walkToLocationBehavior = new WalkToLocationBehavior(outgoingCommunicationBridge, fullRobotModel, referenceFrames,
            wholeBodyControllerParameters.getWalkingControllerParameters());
      behaviors.add(walkToLocationBehavior);
      wholeBodyBehavior = new WholeBodyInverseKinematicsBehavior(wholeBodyControllerParameters, yoTime, outgoingCommunicationBridge);

      behaviors.add(wholeBodyBehavior);

      chestTrajectoryBehavior = new ChestTrajectoryBehavior(outgoingCommunicationBridge, yoTime);
      behaviors.add(chestTrajectoryBehavior);

      chestGoHomeBehavior = new GoHomeBehavior("chest", outgoingCommunicationBridge, yoTime);
      behaviors.add(chestGoHomeBehavior);

      pelvisGoHomeBehavior = new GoHomeBehavior("pelvis", outgoingCommunicationBridge, yoTime);
      behaviors.add(pelvisGoHomeBehavior);

      armGoHomeLeftBehavior = new GoHomeBehavior("leftArm", outgoingCommunicationBridge, yoTime);
      behaviors.add(armGoHomeLeftBehavior);
      armGoHomeRightBehavior = new GoHomeBehavior("rightArm", outgoingCommunicationBridge, yoTime);
      behaviors.add(armGoHomeRightBehavior);
      armTrajectoryBehavior = new ArmTrajectoryBehavior("handTrajectory", outgoingCommunicationBridge, yoTime);
      behaviors.add(armTrajectoryBehavior);

      handDesiredConfigurationBehavior = new HandDesiredConfigurationBehavior(outgoingCommunicationBridge, yoTime);
      behaviors.add(handDesiredConfigurationBehavior);

      coactiveElement = new PickUpBallBehaviorCoactiveElementBehaviorSide();
      coactiveElement.setPickUpBallBehavior(this);
      registry.addChild(coactiveElement.getUserInterfaceWritableYoVariableRegistry());
      registry.addChild(coactiveElement.getMachineWritableYoVariableRegistry());

      waitForUserValidationBehavior = new WaitForUserValidationBehavior(outgoingCommunicationBridge, coactiveElement.validClicked,
            coactiveElement.validAcknowledged);
      behaviors.add(waitForUserValidationBehavior);
      for (BehaviorInterface behavior : behaviors)
      {
         registry.addChild(behavior.getYoVariableRegistry());
      }

   }

   @Override
   public CoactiveElement getCoactiveElement()
   {
      return coactiveElement;
   }

   boolean locationSet = false;

   @Override
   public void doControl()
   {
      pipeLine.doControl();
   }

   private void setupPipelineForKick()
   {

      HandDesiredConfigurationTask closeHand = new HandDesiredConfigurationTask(RobotSide.LEFT, HandConfiguration.CLOSE, handDesiredConfigurationBehavior,
            yoTime);
      HandDesiredConfigurationTask openHand = new HandDesiredConfigurationTask(RobotSide.LEFT, HandConfiguration.OPEN, handDesiredConfigurationBehavior,
            yoTime);

      //ENABLE LIDAR
      BehaviorTask enableLidarTask = new BehaviorTask(enableLidarBehavior, yoTime, 1)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.ENABLING_LIDAR);
         }
      };

      //REDUCE LIDAR RANGE *******************************************

      BehaviorTask setLidarMediumRangeTask = new BehaviorTask(setLidarParametersBehavior, yoTime, 1)
      {
         @Override
         protected void setBehaviorInput()
         {

            coactiveElement.currentState.set(BehaviorState.SETTING_LIDAR_PARAMS);
            DepthDataFilterParameters param = new DepthDataFilterParameters();
            param.nearScanRadius = 1.75f;
            setLidarParametersBehavior.setInput(param);
         }
      };

      //CLEAR LIDAR POINTS FOR CLEAN SCAN *******************************************
      BehaviorTask clearLidarTask = new BehaviorTask(clearLidarBehavior, yoTime, 1)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.CLEARING_LIDAR);
         }
      };

      //SEARCH FOR BALL *******************************************

      BehaviorTask findBallTask = new BehaviorTask(initialSphereDetectionBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.SEARCHING_FOR_BALL);
            coactiveElement.searchingForBall.set(true);
            coactiveElement.foundBall.set(false);
            coactiveElement.ballX.set(0);
            coactiveElement.ballY.set(0);
            coactiveElement.ballZ.set(0);

         }
      };

      //

      // Confirm from the user that this is the correct ball *******************************************

      BehaviorTask validateBallTask = new BehaviorTask(waitForUserValidationBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.WAITING_FOR_USER_CONFIRMATION);
            coactiveElement.searchingForBall.set(false);
            coactiveElement.waitingForValidation.set(true);
            coactiveElement.validAcknowledged.set(false);
            coactiveElement.foundBall.set(false);
            coactiveElement.ballX.set(initialSphereDetectionBehavior.getBallLocation().x);
            coactiveElement.ballY.set(initialSphereDetectionBehavior.getBallLocation().y);
            coactiveElement.ballZ.set(initialSphereDetectionBehavior.getBallLocation().z);
            coactiveElement.ballRadius.set(initialSphereDetectionBehavior.getSpehereRadius());

         }
      };

      // WALK TO THE BALL *******************************************

      BehaviorTask walkToBallTask = new BehaviorTask(walkToLocationBehavior, yoTime, 0)
      {

         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.WALKING_TO_BALL);
            coactiveElement.searchingForBall.set(false);
            coactiveElement.waitingForValidation.set(false);
            coactiveElement.foundBall.set(true);
            walkToLocationBehavior.setTarget(getoffsetPoint());
         }
      };

      //LOOK DOWN *******************************************

      Vector3d axis = new Vector3d(0, 1, 0);
      double rotationDownAngle = 1.4;

      AxisAngle4d desiredAxisAngle = new AxisAngle4d();
      desiredAxisAngle.set(axis, rotationDownAngle);
      Quat4d desiredHeadQuat = new Quat4d();
      desiredHeadQuat.set(desiredAxisAngle);

      HeadTrajectoryMessage message = new HeadTrajectoryMessage(2, desiredHeadQuat);

      HeadTrajectoryBehavior headTrajectoryBehavior = new HeadTrajectoryBehavior(outgoingCommunicationBridge, yoTime);

      headTrajectoryBehavior.initialize();
      headTrajectoryBehavior.setInput(message);

      BehaviorTask lookDown = new BehaviorTask(headTrajectoryBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.BENDING_OVER);
         }
      };

      double rotationUpAngle = 0;

      AxisAngle4d desiredAxisUpAngle = new AxisAngle4d();
      desiredAxisUpAngle.set(axis, rotationUpAngle);
      Quat4d desiredHeadUpQuat = new Quat4d();
      desiredHeadUpQuat.set(desiredAxisUpAngle);

      HeadTrajectoryMessage messageHeadUp = new HeadTrajectoryMessage(2, desiredHeadUpQuat);

      HeadTrajectoryBehavior headTrajectoryUpBehavior = new HeadTrajectoryBehavior(outgoingCommunicationBridge, yoTime);

      headTrajectoryUpBehavior.initialize();
      headTrajectoryUpBehavior.setInput(messageHeadUp);

      BehaviorTask lookUp = new BehaviorTask(headTrajectoryUpBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.STOPPED);
         }
      };

      // BEND OVER *******************************************
      FrameOrientation desiredChestOrientation = new FrameOrientation(referenceFrames.getPelvisZUpFrame(), Math.toRadians(30), Math.toRadians(20), 0);
      desiredChestOrientation.changeFrame(worldFrame);
      ChestOrientationTask chestOrientationTask = new ChestOrientationTask(desiredChestOrientation, yoTime, chestTrajectoryBehavior, 4, 0);

      //REDUCE LIDAR RANGE *******************************************

      BehaviorTask setLidarShortRangeTask = new BehaviorTask(setLidarParametersBehavior, yoTime, 1)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.SETTING_LIDAR_PARAMS);
            DepthDataFilterParameters param = new DepthDataFilterParameters();
            param.nearScanRadius = 0.6f;
            setLidarParametersBehavior.setInput(param);
         }
      };

      //CLEAR LIDAR POINTS FOR CLEAN SCAN *******************************************
      BehaviorTask clearLidarTask2 = new BehaviorTask(clearLidarBehavior, yoTime, 1)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.CLEARING_LIDAR);
         }
      };

      //SEARCH FOR BALL AGAIN FOR FINAL LOCATION *******************************************
      BehaviorTask finalFindBallTask = new BehaviorTask(initialSphereDetectionBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.SEARCHING_FOR_BALL);
            coactiveElement.searchingForBall.set(true);
            coactiveElement.foundBall.set(false);
            coactiveElement.ballX.set(0);
            coactiveElement.ballY.set(0);
            coactiveElement.ballZ.set(0);

         }
      };

      // re-validat ball with user *******************************************
      BehaviorTask validateBallTask2 = new BehaviorTask(waitForUserValidationBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.WAITING_FOR_USER_CONFIRMATION);
            coactiveElement.searchingForBall.set(false);
            coactiveElement.waitingForValidation.set(true);
            coactiveElement.validAcknowledged.set(false);
            coactiveElement.foundBall.set(false);
            coactiveElement.ballX.set(initialSphereDetectionBehavior.getBallLocation().x);
            coactiveElement.ballY.set(initialSphereDetectionBehavior.getBallLocation().y);
            coactiveElement.ballZ.set(initialSphereDetectionBehavior.getBallLocation().z);
            coactiveElement.ballRadius.set(initialSphereDetectionBehavior.getSpehereRadius());

         }
      };

      // GO TO INITIAL POICKUP LOCATION *******************************************
      BehaviorTask goToPickUpBallInitialLocationTask = new BehaviorTask(wholeBodyBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.REACHING_FOR_BALL);
            FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), initialSphereDetectionBehavior.getBallLocation().x,
                  initialSphereDetectionBehavior.getBallLocation().y,
                  initialSphereDetectionBehavior.getBallLocation().z + initialSphereDetectionBehavior.getSpehereRadius() + 0.25);
            wholeBodyBehavior.setSolutionQualityThreshold(2.01);
            wholeBodyBehavior.setTrajectoryTime(6);
            FrameOrientation tmpOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(90), Math.toRadians(90), 0);
            wholeBodyBehavior.setDesiredHandPose(RobotSide.LEFT, point, tmpOr);

//            FramePoint rhPoint = new FramePoint(referenceFrames.getChestFrame(), 0.5, -0.25, 0);
//            FrameOrientation rhOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(90), 0, 0);

//            wholeBodyBehavior.setDesiredHandPose(RobotSide.RIGHT, rhPoint, rhOr);

         }
      };

      //REACH FOR THE BALL *******************************************
      BehaviorTask pickUpBallTask = new BehaviorTask(wholeBodyBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), initialSphereDetectionBehavior.getBallLocation().x,
                  initialSphereDetectionBehavior.getBallLocation().y,
                  initialSphereDetectionBehavior.getBallLocation().z + initialSphereDetectionBehavior.getSpehereRadius());
            wholeBodyBehavior.setSolutionQualityThreshold(2.01);
            wholeBodyBehavior.setTrajectoryTime(6);
            FrameOrientation tmpOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(90), Math.toRadians(90), 0);
            wholeBodyBehavior.setDesiredHandPose(RobotSide.LEFT, point, tmpOr);

         }
      };

      //PICK UP THE BALL *******************************************

      BehaviorTask goToFinalPickUpBallLocationTask = new BehaviorTask(wholeBodyBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.PICKING_UP_BALL);

            FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), initialSphereDetectionBehavior.getBallLocation().x,
                  initialSphereDetectionBehavior.getBallLocation().y,
                  initialSphereDetectionBehavior.getBallLocation().z + initialSphereDetectionBehavior.getSpehereRadius() + 0.25);
            wholeBodyBehavior.setSolutionQualityThreshold(2.01);
            wholeBodyBehavior.setTrajectoryTime(6);
            FrameOrientation tmpOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(90), Math.toRadians(90), 0);
            wholeBodyBehavior.setDesiredHandPose(RobotSide.LEFT, point, tmpOr);

         }
      };

      //RESET BODY POSITIONS *******************************************
      GoHomeMessage goHomeChestMessage = new GoHomeMessage(BodyPart.CHEST, 3);
      chestGoHomeBehavior.setInput(goHomeChestMessage);
      GoHomeTask goHomeChestTask = new GoHomeTask(goHomeChestMessage, chestGoHomeBehavior, yoTime, 0);

      GoHomeMessage goHomepelvisMessage = new GoHomeMessage(BodyPart.PELVIS, 3);
      pelvisGoHomeBehavior.setInput(goHomepelvisMessage);
      GoHomeTask goHomePelvisTask = new GoHomeTask(goHomepelvisMessage, pelvisGoHomeBehavior, yoTime, 0);

      BehaviorTask goToDropBallInitialLocationTask = new BehaviorTask(wholeBodyBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.PUTTING_BALL_IN_BASKET);
            FramePoint point = new FramePoint(referenceFrames.getChestFrame(), 0.5, 0.1, 0);

            wholeBodyBehavior.setSolutionQualityThreshold(2.01);
            wholeBodyBehavior.setTrajectoryTime(6);
            FrameOrientation tmpOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(-90), Math.toRadians(45), Math.toRadians(-90));
            wholeBodyBehavior.setDesiredHandPose(RobotSide.LEFT, point, tmpOr);

            FramePoint rhPoint = new FramePoint(referenceFrames.getChestFrame(), 0.5, -0.25, 0);
            FrameOrientation rhOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(90), 0, 0);

            wholeBodyBehavior.setDesiredHandPose(RobotSide.RIGHT, rhPoint, rhOr);

         }
      };
      BehaviorTask goToDropBallFinalLocationTask = new BehaviorTask(wholeBodyBehavior, yoTime, 0)
      {
         @Override
         protected void setBehaviorInput()
         {
            coactiveElement.currentState.set(BehaviorState.PUTTING_BALL_IN_BASKET);
            FramePoint point = new FramePoint(referenceFrames.getChestFrame(), 0.5, -0.1, 0);

            wholeBodyBehavior.setSolutionQualityThreshold(2.01);
            wholeBodyBehavior.setTrajectoryTime(6);
            FrameOrientation tmpOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(-90), Math.toRadians(20), Math.toRadians(-90));
            wholeBodyBehavior.setDesiredHandPose(RobotSide.LEFT, point, tmpOr);

            FramePoint rhPoint = new FramePoint(referenceFrames.getChestFrame(), 0.5, -0.25, 0);
            FrameOrientation rhOr = new FrameOrientation(point.getReferenceFrame(), Math.toRadians(90), 0, 0);

            wholeBodyBehavior.setDesiredHandPose(RobotSide.RIGHT, rhPoint, rhOr);

         }
      };

      GoHomeMessage goHomeLeftArmMessage = new GoHomeMessage(BodyPart.ARM, RobotSide.LEFT, 3);
      armGoHomeLeftBehavior.setInput(goHomeLeftArmMessage);
      GoHomeTask goHomeLeftArmTask = new GoHomeTask(goHomeLeftArmMessage, armGoHomeLeftBehavior, yoTime, 0);

      //      GoHomeMessage goHomeRightArmMessage = new GoHomeMessage(BodyPart.ARM, RobotSide.RIGHT, 3);
      //      armGoHomeRightBehavior.setInput(goHomeRightArmMessage);
      //      GoHomeTask goHomeRightArmTask = new GoHomeTask(goHomeRightArmMessage, armGoHomeRightBehavior, yoTime, 0);

      double[] rightHandWiderHomeJointAngles = new double[] {-0.785398, 0.5143374964757462, 2.2503094898479272, -2.132492022530739, -0.22447272781774874,
            -0.4780687104960028, -0.24919417978503655};

      ArmTrajectoryMessage widerHome = new ArmTrajectoryMessage(RobotSide.RIGHT, 3, rightHandWiderHomeJointAngles);

      ArmTrajectoryTask rightArmHomeTask = new ArmTrajectoryTask(widerHome, armTrajectoryBehavior, yoTime);

      double[] rightHandBucketLocation1 = new double[] {0.5489321822438367, 0.2899665391571677, 2.096340823983413, -1.2225333451166707, 0.1256161514011733,
            -1.3433026185064938, -1.1994258903111514};

      ArmTrajectoryMessage rightHandBucketLocation1Message = new ArmTrajectoryMessage(RobotSide.RIGHT, 3, rightHandBucketLocation1);

      ArmTrajectoryTask rightHandBucketLocation1Task = new ArmTrajectoryTask(rightHandBucketLocation1Message, armTrajectoryBehavior, yoTime);

      double[] leftHandBucketLocation1 = new double[] {-0.5609186812662719, -0.39273790125704305, 1.89931104400202, 1.8345084796174007, -1.9173410679363112,
            -0.7657081703756509, -0.7098631227127279};

      ArmTrajectoryMessage leftHandBucketLocation1Message = new ArmTrajectoryMessage(RobotSide.LEFT, 3, leftHandBucketLocation1);

      ArmTrajectoryTask leftHandBucketLocation1Task = new ArmTrajectoryTask(leftHandBucketLocation1Message, armTrajectoryBehavior, yoTime);

      double[] rightHandBucketLocation2 = new double[] {0.4765048070153984, 0.305694742754363, 2.173812006625049, -1.4970540590789951, 0.10321456673940527,
            -1.2120648871681976, -1.1591439074587626};

      ArmTrajectoryMessage rightHandBucketLocation2Message = new ArmTrajectoryMessage(RobotSide.RIGHT, 3, rightHandBucketLocation2);

      ArmTrajectoryTask rightHandBucketLocation2Task = new ArmTrajectoryTask(rightHandBucketLocation2Message, armTrajectoryBehavior, yoTime);

      double[] leftHandBucketLocation2 = new double[] {-0.6312858675745908, -0.6560594198655715, 2.026449179186367, 2.0325182474649997, -1.4129369066719957,
            -0.33189990885720594, -1.1435699210219243};

      ArmTrajectoryMessage leftHandBucketLocation2Message = new ArmTrajectoryMessage(RobotSide.LEFT, 3, leftHandBucketLocation2);

      ArmTrajectoryTask leftHandBucketLocation2Task = new ArmTrajectoryTask(leftHandBucketLocation2Message, armTrajectoryBehavior, yoTime);

      //PUT BALL IN BUCKET *******************************************

      pipeLine.submitSingleTaskStage(rightArmHomeTask);
      //      
      pipeLine.submitSingleTaskStage(closeHand);
      pipeLine.submitSingleTaskStage(enableLidarTask);
      pipeLine.submitSingleTaskStage(setLidarMediumRangeTask);
      pipeLine.submitSingleTaskStage(clearLidarTask);
      pipeLine.submitSingleTaskStage(findBallTask);
      pipeLine.submitSingleTaskStage(validateBallTask);
      pipeLine.submitSingleTaskStage(walkToBallTask);
      pipeLine.submitSingleTaskStage(lookDown);
      pipeLine.submitSingleTaskStage(chestOrientationTask);
      pipeLine.submitSingleTaskStage(setLidarShortRangeTask);
      pipeLine.submitSingleTaskStage(clearLidarTask2);
      pipeLine.submitSingleTaskStage(finalFindBallTask);
      pipeLine.submitSingleTaskStage(validateBallTask2);
      pipeLine.submitSingleTaskStage(goToPickUpBallInitialLocationTask);
      pipeLine.submitSingleTaskStage(openHand);
      pipeLine.submitSingleTaskStage(pickUpBallTask);
      pipeLine.submitSingleTaskStage(closeHand);
                  pipeLine.submitSingleTaskStage(goToFinalPickUpBallLocationTask);
      //
      pipeLine.submitSingleTaskStage(goHomeChestTask);
      pipeLine.submitSingleTaskStage(goHomePelvisTask);

      //PUT BALL IN BUCKET
      pipeLine.submitSingleTaskStage(rightArmHomeTask);

      pipeLine.submitSingleTaskStage(rightHandBucketLocation1Task);
      pipeLine.submitSingleTaskStage(leftHandBucketLocation1Task);

      pipeLine.submitSingleTaskStage(rightHandBucketLocation2Task);
      pipeLine.submitSingleTaskStage(leftHandBucketLocation2Task);

      pipeLine.submitSingleTaskStage(openHand);
      pipeLine.submitSingleTaskStage(closeHand);
      pipeLine.submitSingleTaskStage(openHand);

      pipeLine.submitSingleTaskStage(leftHandBucketLocation1Task);

      //      pipeLine.submitSingleTaskStage(goToDropBallInitialLocationTask);
      //      pipeLine.submitSingleTaskStage(goToDropBallFinalLocationTask);

      //
      //
      //      pipeLine.submitSingleTaskStage(goToDropBallInitialLocationTask);
      //      pipeLine.submitSingleTaskStage(closeHand);
      pipeLine.submitSingleTaskStage(rightArmHomeTask);
      //
      pipeLine.submitSingleTaskStage(goHomeLeftArmTask);
      //      
      pipeLine.submitSingleTaskStage(goHomeChestTask);
      pipeLine.submitSingleTaskStage(goHomePelvisTask);
      //      
      //      
      //      pipeLine.submitSingleTaskStage(rightArmHomeTask);
      //      pipeLine.submitSingleTaskStage(lookUp);
   }

   private FramePose2d getoffsetPoint()
   {

      FramePoint2d ballPosition2d = new FramePoint2d(ReferenceFrame.getWorldFrame(), initialSphereDetectionBehavior.getBallLocation().x,
            initialSphereDetectionBehavior.getBallLocation().y);
      FramePoint2d robotPosition = new FramePoint2d(midZupFrame, 0.0, 0.0);
      robotPosition.changeFrame(worldFrame);
      FrameVector2d walkingDirection = new FrameVector2d(worldFrame);
      walkingDirection.set(ballPosition2d);
      walkingDirection.sub(robotPosition);
      walkingDirection.normalize();
      double walkingYaw = Math.atan2(walkingDirection.getY(), walkingDirection.getX());

      //get a point offset from the ball
      double x = ballPosition2d.getX() - walkingDirection.getX() * standingDistance;
      double y = ballPosition2d.getY() - walkingDirection.getY() * standingDistance;
      double rotationAngle = Math.toRadians(45);
      //rotate that point around the ball so that the robot stands to the side.

      double newX = ballPosition2d.getX() + (x - ballPosition2d.getX()) * Math.cos(rotationAngle) - (y - ballPosition2d.getY()) * Math.sin(rotationAngle);
      double newY = ballPosition2d.getY() + (x - ballPosition2d.getX()) * Math.sin(rotationAngle) + (y - ballPosition2d.getY()) * Math.cos(rotationAngle);

      FramePose2d poseToWalkTo = new FramePose2d(worldFrame, new Point2d(newX, newY), walkingYaw);
      return poseToWalkTo;
   }

   @Override
   public void initialize()
   {
      defaultInitialize();
      setupPipelineForKick();

   }

   @Override
   public void doPostBehaviorCleanup()
   {

      defaultPostBehaviorCleanup();
      coactiveElement.currentState.set(BehaviorState.STOPPED);

      coactiveElement.searchingForBall.set(false);
      coactiveElement.waitingForValidation.set(false);
      coactiveElement.foundBall.set(false);
      coactiveElement.ballX.set(0);
      coactiveElement.ballY.set(0);
      coactiveElement.ballZ.set(0);
      for (BehaviorInterface behavior : behaviors)
      {
         behavior.doPostBehaviorCleanup();
      }
   }

   @Override
   public void stop()
   {
      defaultStop();
      for (BehaviorInterface behavior : behaviors)
      {
         behavior.stop();
      }
   }

   @Override
   public void pause()
   {
      defaultPause();
      for (BehaviorInterface behavior : behaviors)
      {
         behavior.pause();
      }
   }

   @Override
   public boolean isDone()
   {
      return pipeLine.isDone();
   }

   @Override
   public void enableActions()
   {
   }

   @Override
   public void resume()
   {
      defaultResume();
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
      for (BehaviorInterface behavior : behaviors)
      {
         behavior.consumeObjectFromNetworkProcessor(object);
      }
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
      for (BehaviorInterface behavior : behaviors)
      {
         behavior.consumeObjectFromController(object);
      }
   }

   @Override
   public boolean hasInputBeenSet()
   {
      return true;
   }

   public void abort()
   {
      doPostBehaviorCleanup();
      this.stop();
      this.pipeLine.clearAll();
   }

   public void setHSVRange(HSVRange hsvRange)
   {
      if(initialSphereDetectionBehavior instanceof BlobFilteredSphereDetectionBehavior)
      {
         BlobFilteredSphereDetectionBehavior blobFilteredSphereDetectionBehavior = (BlobFilteredSphereDetectionBehavior) initialSphereDetectionBehavior;
         blobFilteredSphereDetectionBehavior.resetHSVRanges();
         blobFilteredSphereDetectionBehavior.addHSVRange(hsvRange);
      }
   }
}
