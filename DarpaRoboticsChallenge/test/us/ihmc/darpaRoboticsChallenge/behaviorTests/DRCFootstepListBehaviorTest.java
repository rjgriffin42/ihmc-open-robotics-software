package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFHumanoidRobot;
import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.primitives.FootstepListBehavior;
import us.ihmc.humanoidBehaviors.utilities.StopThreadUpdatable;
import us.ihmc.humanoidBehaviors.utilities.TrajectoryBasedStopThreadUpdatable;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.HumanoidBehaviorControlModePacket.HumanoidBehaviorControlModeEnum;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepData;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataList;
import us.ihmc.humanoidRobotics.communication.subscribers.RobotDataReceiver;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FramePose2d;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.RotationFunctions;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.time.GlobalTimer;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.io.printing.PrintTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.thread.ThreadTools;

public abstract class DRCFootstepListBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      GlobalTimer.clearTimers();
      
      

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   private static final boolean DEBUG = false;
   private final double POSITION_THRESHOLD = 0.1;
   private final double ORIENTATION_THRESHOLD = 0.05;

   private DRCBehaviorTestHelper drcBehaviorTestHelper;
   private RobotDataReceiver robotDataReceiver;
   private SDFHumanoidRobot robot;
   private FullHumanoidRobotModel fullRobotModel;

   @Before
   public void setUp()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      DRCDemo01NavigationEnvironment testEnvironment = new DRCDemo01NavigationEnvironment();

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(testEnvironment, getSimpleRobotName(), null,
            DRCObstacleCourseStartingLocation.DEFAULT, simulationTestingParameters, getRobotModel());

      fullRobotModel = drcBehaviorTestHelper.getSDFFullRobotModel();

      robot = drcBehaviorTestHelper.getRobot();
      robotDataReceiver = drcBehaviorTestHelper.getRobotDataReceiver();
   }

   @DeployableTestMethod(estimatedDuration = 31.9)
   @Test(timeout = 95822)
   public void testTwoStepsForwards() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      PrintTools.debug(this, "Initializing Sim");
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      PrintTools.debug(this, "Dispatching Behavior");
      FootstepListBehavior footstepListBehavior = new FootstepListBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(),getRobotModel().getWalkingControllerParameters());
      drcBehaviorTestHelper.dispatchBehavior(footstepListBehavior);

      SideDependentList<FramePose2d> desiredFootPoses = new SideDependentList<FramePose2d>();
      ArrayList<Footstep> desiredFootsteps = new ArrayList<Footstep>();

      double xOffset = 0.1;

      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose2d desiredFootPose = createFootPoseOffsetFromCurrent(robotSide, xOffset, 0.0);
         Footstep desiredFootStep = generateFootstepOnFlatGround(robotSide, desiredFootPose);

         desiredFootPoses.set(robotSide, desiredFootPose);
         desiredFootsteps.add(desiredFootStep);
      }
      assertTrue(!areFootstepsTooFarApart(footstepListBehavior, desiredFootsteps));

      PrintTools.debug(this, "Initializing Behavior");
      footstepListBehavior.initialize();
      footstepListBehavior.set(desiredFootsteps);
      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(0.1);
      assertTrue(success);
      assertTrue(footstepListBehavior.hasInputBeenSet());
      assertTrue(footstepListBehavior.isWalking());

      PrintTools.debug(this, "Begin Executing Behavior");
      while (!footstepListBehavior.isDone())
      {
         success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
         assertTrue(success);
      }
      assertTrue(!footstepListBehavior.isWalking());
      assertTrue(footstepListBehavior.isDone());
      PrintTools.debug(this, "Behavior should be done");

      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose2d finalFootPose = getRobotFootPose2d(robot, robotSide);
         assertPosesAreWithinThresholds(desiredFootPoses.get(robotSide), finalFootPose);
      }

      BambooTools.reportTestFinishedMessage();
   }

   @DeployableTestMethod(estimatedDuration = 31.9)
   @Test(timeout = 95822)
   public void testSideStepping() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      PrintTools.debug(this, "Initializing Sim");
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      PrintTools.debug(this, "Dispatching Behavior");
      drcBehaviorTestHelper.updateRobotModel();
      FootstepListBehavior footstepListBehavior = new FootstepListBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(), getRobotModel().getWalkingControllerParameters());
      drcBehaviorTestHelper.dispatchBehavior(footstepListBehavior);

      SideDependentList<FramePose2d> desiredFootPoses = new SideDependentList<FramePose2d>();
      ArrayList<Footstep> desiredFootsteps = new ArrayList<Footstep>();

      double yOffset = 0.1;

      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose2d desiredFootPose = createFootPoseOffsetFromCurrent(robotSide, 0.0, yOffset);
         Footstep desiredFootStep = generateFootstepOnFlatGround(robotSide, desiredFootPose);

         desiredFootPoses.set(robotSide, desiredFootPose);
         desiredFootsteps.add(desiredFootStep);
      }
      assertTrue(!areFootstepsTooFarApart(footstepListBehavior, desiredFootsteps));
      
      PrintTools.debug(this, "Initializing Behavior");
      footstepListBehavior.initialize();
      footstepListBehavior.set(desiredFootsteps);
      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(0.1);
      assertTrue(success);
      assertTrue(footstepListBehavior.hasInputBeenSet());
      assertTrue(footstepListBehavior.isWalking());

      PrintTools.debug(this, "Begin Executing Behavior");
      while (!footstepListBehavior.isDone())
      {
         success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
         assertTrue(success);
      }
      assertTrue(!footstepListBehavior.isWalking());
      assertTrue(footstepListBehavior.isDone());
      PrintTools.debug(this, "Behavior should be done");

      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose2d finalFootPose = getRobotFootPose2d(robot, robotSide);
         assertPosesAreWithinThresholds(desiredFootPoses.get(robotSide), finalFootPose);
      }

      BambooTools.reportTestFinishedMessage();
   }

   @DeployableTestMethod(estimatedDuration = 31.9)
   @Test(timeout = 95822)
   public void testStepLongerThanMaxStepLength() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      PrintTools.debug(this, "Initializing Sim");
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SideDependentList<FramePose2d> initialFootPoses = new SideDependentList<FramePose2d>();
      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose2d initialFootPose = getRobotFootPose2d(robot, robotSide);
         initialFootPoses.put(robotSide, initialFootPose);
      }

      PrintTools.debug(this, "Dispatching Behavior");
      FootstepListBehavior footstepListBehavior = new FootstepListBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(), getRobotModel().getWalkingControllerParameters());
      drcBehaviorTestHelper.dispatchBehavior(footstepListBehavior);

      SideDependentList<FramePose2d> desiredFootPoses = new SideDependentList<FramePose2d>();
      ArrayList<Footstep> desiredFootsteps = new ArrayList<Footstep>();

      double xOffset = 1.5 * getRobotModel().getWalkingControllerParameters().getMaxStepLength();

      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose2d desiredFootPose = createFootPoseOffsetFromCurrent(robotSide, xOffset, 0.0);
         Footstep desiredFootStep = generateFootstepOnFlatGround(robotSide, desiredFootPose);

         desiredFootPoses.set(robotSide, desiredFootPose);
         desiredFootsteps.add(desiredFootStep);
      }
      assertTrue(areFootstepsTooFarApart(footstepListBehavior, desiredFootsteps));
   }

   private FootstepDataList createFootstepDataList(ArrayList<Footstep> desiredFootsteps)
   {
      FootstepDataList ret = new FootstepDataList();

      for (int i = 0; i < desiredFootsteps.size(); i++)
      {
         Footstep footstep = desiredFootsteps.get(i);
         Point3d location = new Point3d(footstep.getX(), footstep.getY(), footstep.getZ());
         Quat4d orientation = new Quat4d();
         footstep.getOrientation(orientation);

         RobotSide footstepSide = footstep.getRobotSide();
         FootstepData footstepData = new FootstepData(footstepSide, location, orientation);
         ret.add(footstepData);
      }

      return ret;
   }

   private boolean areFootstepsTooFarApart(FootstepListBehavior footstepListBehavior, ArrayList<Footstep> desiredFootsteps)
   {
      ArrayList<Double> footStepLengths = footstepListBehavior.getFootstepLengths(createFootstepDataList(desiredFootsteps),
            drcBehaviorTestHelper.getSDFFullRobotModel(), getRobotModel().getWalkingControllerParameters());

      if(DEBUG)
      for (double footStepLength : footStepLengths)
      {
         PrintTools.debug(this, "foot step length : " + footStepLength);
      }
      
      boolean footStepsAreTooFarApart = footstepListBehavior.areFootstepsTooFarApart(createFootstepDataList(desiredFootsteps), fullRobotModel, getRobotModel().getWalkingControllerParameters());
      
      return footStepsAreTooFarApart;
   }

   @DeployableTestMethod(estimatedDuration = 31.9)
   @Test(timeout = 95822)
   public void testStop() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      PrintTools.debug(this, "Initializing Sim");
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      ArrayList<Double> xOffsets = new ArrayList<Double>();
      xOffsets.add(0.1);
      xOffsets.add(0.2);
      xOffsets.add(0.3);

      FootstepListBehavior footstepListBehavior = new FootstepListBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(), getRobotModel().getWalkingControllerParameters());
      SideDependentList<FramePose2d> desiredFinalFootPoses = new SideDependentList<FramePose2d>();

      ArrayList<Footstep> desiredFootsteps = new ArrayList<Footstep>();
      for (double xOffset : xOffsets)
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            FramePose2d desiredFootPose = createFootPoseOffsetFromCurrent(robotSide, xOffset, 0.0);
            desiredFootsteps.add(generateFootstepOnFlatGround(robotSide, desiredFootPose));
            desiredFinalFootPoses.put(robotSide, desiredFootPose);
         }
      }

      areFootstepsTooFarApart(footstepListBehavior, desiredFootsteps);

      PrintTools.debug(this, "Initializing Behavior");
      footstepListBehavior.initialize();
      footstepListBehavior.set(desiredFootsteps);

      PrintTools.debug(this, "Begin Executing Behavior");
      double pausePercent = Double.POSITIVE_INFINITY;
      double pauseDuration = Double.POSITIVE_INFINITY;
      double stepNumberToStopOn = desiredFootsteps.size() - 1.0;
      double stopPercent = 100.0 * stepNumberToStopOn / desiredFootsteps.size();
      ReferenceFrame frameToKeepTrackOf = drcBehaviorTestHelper.getReferenceFrames().getFootFrame(RobotSide.LEFT);
      StopThreadUpdatable stopThreadUpdatable = new TrajectoryBasedStopThreadUpdatable(robotDataReceiver, footstepListBehavior, pausePercent, pauseDuration,
            stopPercent, desiredFinalFootPoses.get(RobotSide.LEFT), frameToKeepTrackOf);
      drcBehaviorTestHelper.executeBehaviorPauseAndResumeOrStop(footstepListBehavior, stopThreadUpdatable);
      PrintTools.debug(this, "Behavior should be done");

      SideDependentList<FramePose2d> footPosesAtStop = new SideDependentList<FramePose2d>();
      for (RobotSide robotSide : RobotSide.values)
      {
         ReferenceFrame footFrame = stopThreadUpdatable.getReferenceFramesAtTransition(HumanoidBehaviorControlModeEnum.STOP).getFootFrame(robotSide);
         footPosesAtStop.put(robotSide, stopThreadUpdatable.getTestFramePose2dCopy(footFrame.getTransformToWorldFrame()));
      }

      SideDependentList<FramePose2d> footPosesFinal = new SideDependentList<FramePose2d>();
      for (RobotSide robotSide : RobotSide.values)
      {
         drcBehaviorTestHelper.updateRobotModel();
         ReferenceFrame footFrame = drcBehaviorTestHelper.getReferenceFrames().getFootFrame(robotSide);
         footPosesFinal.put(robotSide, stopThreadUpdatable.getTestFramePose2dCopy(footFrame.getTransformToWorldFrame()));
      }

      // Foot position and orientation may change after stop command if the robot is currently in single support, 
      // since the robot will complete the current step (to get back into double support) before actually stopping
      double positionThreshold = getRobotModel().getWalkingControllerParameters().getMaxStepLength();
      double orientationThreshold = Math.PI;
      for (RobotSide robotSide : RobotSide.values)
      {
         assertPosesAreWithinThresholds(footPosesAtStop.get(robotSide), footPosesFinal.get(robotSide), positionThreshold, orientationThreshold);
      }
      assertTrue(!footstepListBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

   private FramePose2d createFootPoseOffsetFromCurrent(RobotSide robotSide, double xOffset, double yOffset)
   {
      FramePose2d currentFootPose = getRobotFootPose2d(robot, robotSide);
      return createFootPoseOffsetFromExisting(robotSide, xOffset, yOffset, currentFootPose);
   }

   private FramePose2d createFootPoseOffsetFromExisting(RobotSide robotSide, double xOffset, double yOffset, FramePose2d existingFootStep)
   {
      FramePose2d desiredFootPose = new FramePose2d(existingFootStep);
      desiredFootPose.setX(desiredFootPose.getX() + xOffset);
      desiredFootPose.setY(desiredFootPose.getY() + yOffset);

      return desiredFootPose;
   }

   private Footstep generateFootstepOnFlatGround(RobotSide robotSide, FramePose2d desiredFootPose2d)
   {
      Footstep ret = generateFootstep(desiredFootPose2d, fullRobotModel.getFoot(robotSide), fullRobotModel.getSoleFrame(robotSide), robotSide, 0.0,
            new Vector3d(0.0, 0.0, 1.0));

      return ret;
   }

   private Footstep generateFootstep(FramePose2d footPose2d, RigidBody foot, ReferenceFrame soleFrame, RobotSide robotSide, double height, Vector3d planeNormal)
   {
      double yaw = footPose2d.getYaw();
      Point3d position = new Point3d(footPose2d.getX(), footPose2d.getY(), height);
      Quat4d orientation = new Quat4d();
      RotationFunctions.getQuaternionFromYawAndZNormal(yaw, planeNormal, orientation);

      Footstep footstep = new Footstep(foot, robotSide, soleFrame);
      footstep.setSolePose(new FramePose(ReferenceFrame.getWorldFrame(), position, orientation));

      return footstep;
   }

   private FramePose2d getRobotFootPose2d(SDFHumanoidRobot robot, RobotSide robotSide)
   {
      List<GroundContactPoint> gcPoints = robot.getFootGroundContactPoints(robotSide);
      Joint ankleJoint = gcPoints.get(0).getParentJoint();
      RigidBodyTransform ankleTransformToWorld = new RigidBodyTransform();
      ankleJoint.getTransformToWorld(ankleTransformToWorld);

      FramePose2d ret = new FramePose2d();
      ret.setPose(ankleTransformToWorld);

      return ret;
   }

   private void assertPosesAreWithinThresholds(FramePose2d desiredPose, FramePose2d actualPose)
   {
      assertPosesAreWithinThresholds(desiredPose, actualPose, POSITION_THRESHOLD);
   }

   private void assertPosesAreWithinThresholds(FramePose2d desiredPose, FramePose2d actualPose, double positionThreshold)
   {
      assertPosesAreWithinThresholds(desiredPose, actualPose, positionThreshold, ORIENTATION_THRESHOLD);
   }

   private void assertPosesAreWithinThresholds(FramePose2d desiredPose, FramePose2d actualPose, double positionThreshold, double orientationThreshold)
   {
      double positionDistance = desiredPose.getPositionDistance(actualPose);
      double orientationDistance = desiredPose.getOrientationDistance(actualPose);

      if (DEBUG)
      {
         PrintTools.debug(this, " desired Midfeet Pose :\n" + desiredPose + "\n");
         PrintTools.debug(this, " actual Midfeet Pose :\n" + actualPose + "\n");

         PrintTools.debug(this, " positionDistance = " + positionDistance);
         PrintTools.debug(this, " orientationDistance = " + orientationDistance);
      }

      assertEquals("Pose position error :" + positionDistance + " exceeds threshold: " + positionThreshold, 0.0, positionDistance, positionThreshold);
      assertEquals("Pose orientation error :" + orientationDistance + " exceeds threshold: " + orientationThreshold, 0.0, orientationDistance, orientationThreshold);
   }

}
