package us.ihmc.humanoidBehaviors.fancyPoses;

import java.util.concurrent.atomic.AtomicInteger;

import controller_msgs.msg.dds.FootstepDataListMessage;
import controller_msgs.msg.dds.FootstepDataMessage;
import controller_msgs.msg.dds.FootstepStatusMessage;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.commons.thread.Notification;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.BehaviorDefinition;
import us.ihmc.humanoidBehaviors.tools.BehaviorHelper;
import us.ihmc.humanoidBehaviors.tools.RemoteHumanoidRobotInterface;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.LoadBearingRequest;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.Category;
import us.ihmc.messager.MessagerAPIFactory.CategoryTheme;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.thread.ActivationReference;
import us.ihmc.tools.thread.PausablePeriodicThread;

public class FancyPosesBehavior implements BehaviorInterface
{
   public static final BehaviorDefinition DEFINITION = new BehaviorDefinition("Fancy Poses", FancyPosesBehavior::new, API.create());

   private final BehaviorHelper helper;

   private final ActivationReference<Boolean> stepping;
   private final Notification goToSingleSupportNotification = new Notification();
   private final Notification goToDoubleSupportNotification = new Notification();
   private final Notification goToRunningManNotification = new Notification();
   private final Notification goToKarateKid1Notification = new Notification();
   private final Notification goToKarateKid2Notification = new Notification();
   private final Notification goToKarateKid3Notification = new Notification();
   private final Notification goToPresentNotification = new Notification();
   private final Notification goToShutdownPoseNotification = new Notification();

   private final AtomicInteger footstepsTaken = new AtomicInteger(2);

   private final RobotSide supportSide = RobotSide.RIGHT;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final double trajectoryTime = 3.0;
   private final PausablePeriodicThread mainThread;
   private final RemoteHumanoidRobotInterface robot;

   public FancyPosesBehavior(BehaviorHelper helper)
   {
      LogTools.debug("Initializing FancyPosesBehavior");

      this.helper = helper;
      robot = helper.getOrCreateRobotInterface();

      robot.createFootstepStatusCallback(this::acceptFootstepStatus);
      stepping = helper.createBooleanActivationReference(API.Stepping);

      helper.createUICallback(API.GoToSingleSupport, object -> goToSingleSupportNotification.set());
      helper.createUICallback(API.GoToDoubleSupport, object -> goToDoubleSupportNotification.set());
      helper.createUICallback(API.GoToRunningMan, object -> goToRunningManNotification.set());
      helper.createUICallback(API.GoToKarateKid1, object -> goToKarateKid1Notification.set());
      helper.createUICallback(API.GoToKarateKid2, object -> goToKarateKid2Notification.set());
      helper.createUICallback(API.GoToKarateKid3, object -> goToKarateKid3Notification.set());
      helper.createUICallback(API.GoToPresent, object -> goToPresentNotification.set());
      helper.createUICallback(API.GoToShutdownPose, object -> goToShutdownPoseNotification.set());

      helper.createUICallback(API.Abort, this::doOnAbort);

      mainThread = helper.createPausablePeriodicThread(getClass(), 1.0, this::doBehavior);
   }

   @Override
   public void setEnabled(boolean enabled)
   {
      LogTools.info("Fancy poses behavior selected = {}", enabled);

      mainThread.setRunning(enabled);
      helper.setCommunicationCallbacksEnabled(enabled);
   }

   private void doOnAbort(boolean abort)
   {
      if (abort)
      {
         LogTools.info("Abort received. Shutting down threadScheduler.");
         mainThread.stop();
      }
   }

   private void acceptFootstepStatus(FootstepStatusMessage footstepStatusMessage)
   {
      LogTools.info("acceptFootstepStatus: " + footstepStatusMessage);

      if (footstepStatusMessage.getFootstepStatus() == FootstepStatus.COMPLETED.toByte())
      {
         int footstepsTakenSoFar = footstepsTaken.incrementAndGet();
         LogTools.info("Have taken " + footstepsTakenSoFar + " footsteps.");
      }
   }

   private void goToSingleSupport()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame ankleZUpFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      FramePose3D anklePose = new FramePose3D(ankleZUpFrame);
      anklePose.prependTranslation(0.0, supportSide.negateIfLeftSide(0.25), 0.15);
      anklePose.changeFrame(worldFrame);
      Point3D position = new Point3D();
      Quaternion orientation = new Quaternion();
      anklePose.get(position, orientation);

      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, anklePose);
      robot.requestChestGoHome(trajectoryTime);
      robot.requestPelvisGoHome(trajectoryTime);

      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, leftHandWiderHomeJointAngles);
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, rightHandWiderHomeJointAngles);

      robot.requestFootLoadBearing(supportSide, LoadBearingRequest.LOAD);
      robot.requestFootLoadBearing(supportSide.getOppositeSide(), LoadBearingRequest.LOAD);
   }

   private void goToDoubleSupport()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame ankleZUpFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      FramePose3D anklePose = new FramePose3D(ankleZUpFrame);
      anklePose.prependTranslation(0.0, supportSide.negateIfLeftSide(0.25), 0.0);
      anklePose.changeFrame(worldFrame);
      Point3D position = new Point3D();
      Quaternion orientation = new Quaternion();
      anklePose.get(position, orientation);

      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, anklePose);
      robot.requestChestGoHome(trajectoryTime);
      robot.requestPelvisGoHome(trajectoryTime);

      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, leftHandWiderHomeJointAngles);
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, rightHandWiderHomeJointAngles);
   }

   public void goToRunningManPose()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame supportAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      FramePose3D footPose = new FramePose3D(supportAnkleZUpFrame);
      footPose.setPosition(-0.40, supportSide.negateIfLeftSide(0.25), 0.40);
      footPose.setOrientationYawPitchRoll(0.0, 0.8 * Math.PI / 2.0, 0.0);
      footPose.changeFrame(worldFrame);
      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, footPose);

      FrameQuaternion chestOrientation = new FrameQuaternion(supportAnkleZUpFrame, 0.0, Math.toRadians(20.0), 0.0);
      chestOrientation.changeFrame(worldFrame);
      robot.requestChestOrientationTrajectory(trajectoryTime, chestOrientation, worldFrame, referenceFrames.getPelvisZUpFrame());

      FrameQuaternion pelvisOrientation = new FrameQuaternion(supportAnkleZUpFrame, 0.0, Math.toRadians(10.0), 0.0);
      pelvisOrientation.changeFrame(worldFrame);
      robot.requestPelvisOrientationTrajectory(trajectoryTime, pelvisOrientation);

      double[] jointAngles = new double[] {-1.2584547081002637, 0.016489729127989374, 0.0, 1.5707963267948966, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, jointAngles);

      jointAngles = new double[] {-0.77, 0.0, 3.0, -1.57, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, jointAngles);
   }

   public void goToKarateKid1Pose()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame supportAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      FramePose3D footPose = new FramePose3D(supportAnkleZUpFrame);
      footPose.setPosition(0.10, supportSide.negateIfLeftSide(0.25), 0.20);
      footPose.changeFrame(worldFrame);
      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, footPose);

      robot.requestChestGoHome(trajectoryTime);
      robot.requestPelvisGoHome(trajectoryTime);

      double[] jointAngles = new double[] {0.0, 0.0, 2.35, 0.76, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, jointAngles);

      jointAngles = new double[] {0.0, 0.0, 2.35, -0.76, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, jointAngles);
   }

   public void goToKarateKid2Pose()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame supportAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      FramePose3D footPose = new FramePose3D(supportAnkleZUpFrame);
      footPose.setPosition(0.6, supportSide.negateIfLeftSide(0.25), 0.25);
      footPose.setOrientationYawPitchRoll(0.0, -Math.PI / 4.0, 0.0);
      footPose.changeFrame(worldFrame);
      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, footPose);

      FrameQuaternion chestOrientation = new FrameQuaternion(supportAnkleZUpFrame, 0.0, Math.toRadians(-5.0), 0.0);
      chestOrientation.changeFrame(worldFrame);
      robot.requestChestOrientationTrajectory(trajectoryTime, chestOrientation, worldFrame, referenceFrames.getPelvisZUpFrame());

      FrameQuaternion pelvisOrientation = new FrameQuaternion(supportAnkleZUpFrame, 0.0, Math.toRadians(-15), 0.0);
      pelvisOrientation.changeFrame(worldFrame);
      robot.requestPelvisOrientationTrajectory(trajectoryTime, pelvisOrientation);

      double[] jointAngles = new double[] {-0.63, -0.815, 1.78, 1.40, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, jointAngles);

      jointAngles = new double[] {0.63, 0.815, 1.78, -1.40, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, jointAngles);
   }

   public void goToKarateKid3Pose()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame supportAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      FramePose3D footPose = new FramePose3D(supportAnkleZUpFrame);
      footPose.setPosition(0.00, supportSide.negateIfLeftSide(0.65), 0.2);
      footPose.setOrientationYawPitchRoll(0.0, 0.0, supportSide.negateIfLeftSide(Math.toRadians(40.0)));
      footPose.changeFrame(worldFrame);
      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, footPose);

      FrameQuaternion chestOrientation = new FrameQuaternion(supportAnkleZUpFrame, 0.0, 0.0, supportSide.negateIfLeftSide(Math.toRadians(30.0)));
      chestOrientation.changeFrame(worldFrame);
      robot.requestChestOrientationTrajectory(trajectoryTime, chestOrientation, worldFrame, referenceFrames.getPelvisZUpFrame());

      FrameQuaternion pelvisOrientation = new FrameQuaternion(supportAnkleZUpFrame, 0.0, 0.0, supportSide.negateIfLeftSide(Math.toRadians(20.0)));
      pelvisOrientation.changeFrame(worldFrame);
      robot.requestPelvisOrientationTrajectory(trajectoryTime, pelvisOrientation);

      double[] jointAngles = new double[] {-0.02, -0.017, 1.93, 0.10, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, jointAngles);

      jointAngles = new double[] {1.28, 0.0, 1.56, -1.57, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, jointAngles);
   }

   public void goToPresentPose()
   {
      HumanoidReferenceFrames referenceFrames = robot.pollHumanoidRobotState();

      ReferenceFrame soleFrame = referenceFrames.getSoleFrame(RobotSide.RIGHT);
      double trajectoryTime = 5.0;

      FramePose3D footPose = new FramePose3D(soleFrame);
      footPose.setPosition(-0.511, 0.160, 0.277);
      footPose.setOrientation(0.008, 0.263, 0.216, 0.940);
      footPose.changeFrame(worldFrame);
      Point3D position = new Point3D();
      Quaternion orientation = new Quaternion();
      footPose.get(position, orientation);
      robot.requestFootTrajectory(supportSide.getOppositeSide(), trajectoryTime, position, orientation);

      FrameQuaternion chestOrientation = new FrameQuaternion(soleFrame, 0.039, 0.147, 0.398, 0.905);
      chestOrientation.changeFrame(worldFrame);
      robot.requestChestOrientationTrajectory(trajectoryTime, chestOrientation, worldFrame, referenceFrames.getPelvisZUpFrame());

      FrameQuaternion pelvisOrientation = new FrameQuaternion(soleFrame, 0.014, 0.156, 0.175, 0.972);
      FramePoint3D pelvisPosition = new FramePoint3D(soleFrame, -0.158, -0.048, 0.739);
      pelvisOrientation.changeFrame(worldFrame);
      pelvisPosition.changeFrame(worldFrame);
      robot.requestPelvisTrajectory(trajectoryTime, pelvisPosition, pelvisOrientation);

      double[] jointAngles = new double[] {0.765356719493866, 0.024195531383156776, 2.9822821617126465, 1.6808037757873535, -0.3247416913509369,
            0.67205411195755, 0.15090779960155487};
      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, jointAngles);

      jointAngles = new double[] {0.10722935199737549, -0.23587453365325928, 2.419130802154541, -0.9118338823318481, -2.2621233463287354, -0.5176281929016113,
            0.005108347628265619};
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, jointAngles);
   }

   public void goToShutdownPose()
   {
      robot.requestChestGoHome(trajectoryTime);
      robot.requestPelvisGoHome(trajectoryTime);

      double[] jointAngles = new double[] {0.0, -1.4, 0.0, 0.0, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.LEFT, trajectoryTime, jointAngles);

      jointAngles = new double[] {0.0, 1.4, 0.0, 0.0, 0.0, 0.0, 0.0};
      robot.requestArmTrajectory(RobotSide.RIGHT, trajectoryTime, jointAngles);
   }

   private void doBehavior()
   {
      if (stepping.poll())
      {
         if (stepping.hasChanged())
         {
            LogTools.info("Starting to step");
         }

         if (footstepsTaken.compareAndSet(2, 0))
         {
            LogTools.info("Sending steps");

            FullHumanoidRobotModel fullRobotModel = robot.pollFullRobotModel();
            FootstepDataListMessage footstepList = createTwoStepInPlaceSteps(fullRobotModel);
            robot.requestWalk(footstepList);
         }
      }
      else if (stepping.hasChanged())
      {
         LogTools.info("Stopped stepping");
      }

      if (goToSingleSupportNotification.poll())
      {
         LogTools.info("Going to Single Support!");
         goToSingleSupport();
      }

      if (goToDoubleSupportNotification.poll())
      {
         LogTools.info("Going to Double Support!");
         goToDoubleSupport();
      }

      if (goToRunningManNotification.poll())
      {
         LogTools.info("Going to Running Man!");
         goToRunningManPose();
      }

      if (goToKarateKid1Notification.poll())
      {
         LogTools.info("Going to KarateKid1!");
         goToKarateKid1Pose();
      }

      if (goToKarateKid2Notification.poll())
      {
         LogTools.info("Going to KarateKid2!");
         goToKarateKid2Pose();
      }

      if (goToKarateKid3Notification.poll())
      {
         LogTools.info("Going to KarateKid3!");
         goToKarateKid3Pose();
      }

      if (goToPresentNotification.poll())
      {
         LogTools.info("Going to Present!");
         goToPresentPose();
      }

      if (goToShutdownPoseNotification.poll())
      {
         LogTools.info("Going to Shutdown Pose!");
         goToShutdownPose();
      }
   }

   private FootstepDataListMessage createTwoStepInPlaceSteps(FullHumanoidRobotModel fullRobotModel)
   {
      FootstepDataListMessage footstepList = new FootstepDataListMessage();
      RecyclingArrayList<FootstepDataMessage> footstepDataMessages = footstepList.getFootstepDataList();

      for (RobotSide side : RobotSide.values)
      {
         MovingReferenceFrame stepFrame = fullRobotModel.getSoleFrame(side);
         FramePoint3D footLocation = new FramePoint3D(stepFrame);
         FrameQuaternion footOrientation = new FrameQuaternion(stepFrame);
         footLocation.changeFrame(ReferenceFrame.getWorldFrame());
         footOrientation.changeFrame(ReferenceFrame.getWorldFrame());

         FootstepDataMessage footstepDataMessage = HumanoidMessageTools.createFootstepDataMessage(side, footLocation, footOrientation);
         footstepDataMessages.add().set(footstepDataMessage);
      }
      footstepList.setAreFootstepsAdjustable(true);
      return footstepList;
   }

   public static final double[] leftHandWiderHomeJointAngles = new double[] {0.785398, -0.5237988813979918, 2.377081269248866, 2.35619, -0.33780669067363706,
         0.20773039981059624, -0.026599098661993822};
   public static final double[] rightHandWiderHomeJointAngles = new double[] {-0.785398, 0.5143374964757462, 2.2503094898479272, -2.132492022530739,
         -0.22447272781774874, -0.4780687104960028, -0.24919417978503655};

   public static class API
   {
      private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
      private static final Category RootCategory = apiFactory.createRootCategory("FancyPosesBehavior");
      private static final CategoryTheme FancyPoses = apiFactory.createCategoryTheme("FancyPoses");
      private static final Category FancyPosesCategory = RootCategory.child(FancyPoses);

      public static final Topic<Boolean> Stepping = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("Stepping"));
      public static final Topic<Boolean> Abort = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("Abort"));
      public static final Topic<Boolean> Enable = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("Enable"));
      public static final Topic<Boolean> GoToSingleSupport = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToSingleSupport"));
      public static final Topic<Boolean> GoToDoubleSupport = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToDoubleSupport"));
      public static final Topic<Boolean> GoToRunningMan = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToRunningMan"));
      public static final Topic<Boolean> GoToKarateKid1 = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToKarateKid1"));
      public static final Topic<Boolean> GoToKarateKid2 = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToKarateKid2"));
      public static final Topic<Boolean> GoToKarateKid3 = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToKarateKid3"));
      public static final Topic<Boolean> GoToPresent = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToPresent"));
      public static final Topic<Boolean> GoToShutdownPose = FancyPosesCategory.topic(apiFactory.createTypedTopicTheme("GoToShutdownPose"));

      public static final MessagerAPI create()
      {
         return apiFactory.getAPIAndCloseFactory();
      }
   }
}
