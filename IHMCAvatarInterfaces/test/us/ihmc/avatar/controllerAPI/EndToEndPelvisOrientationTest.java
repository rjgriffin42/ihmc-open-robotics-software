package us.ihmc.avatar.controllerAPI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;

import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.avatar.testTools.EndToEndTestTools;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationControlMode;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyTaskspaceControlState;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.tuple4D.Vector4D;
import us.ihmc.humanoidRobotics.communication.packets.SO3TrajectoryPointMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisOrientationTrajectoryMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.math.QuaternionCalculus;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.simulationConstructionSetTools.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;

public abstract class EndToEndPelvisOrientationTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final Vector3D zeroVector = new Vector3D();

   private DRCSimulationTestHelper drcSimulationTestHelper;
   private FullHumanoidRobotModel fullRobotModel;
   private HumanoidReferenceFrames humanoidReferenceFrames;
   private SimulationConstructionSet scs;

   public void testSingleTrajectoryPoint() throws SimulationExceededMaximumTimeException
   {
      double epsilon = 1.0e-10;
      double yaw = Math.toRadians(5.0);
      double pitch = Math.toRadians(-10.0);
      double roll = Math.toRadians(-5.0);
      double trajectoryTime = 0.5;

      Quaternion orientation = new Quaternion();
      orientation.appendYawRotation(yaw);
      orientation.appendPitchRotation(pitch);
      orientation.appendRollRotation(roll);

      ReferenceFrame midFootZUpGroundFrame = humanoidReferenceFrames.getMidFootZUpGroundFrame();
      FrameOrientation pelvisOrientation = new FrameOrientation(midFootZUpGroundFrame, orientation);
      pelvisOrientation.changeFrame(worldFrame);

      PelvisOrientationTrajectoryMessage message = new PelvisOrientationTrajectoryMessage(trajectoryTime, pelvisOrientation.getQuaternion());
      SO3TrajectoryPointMessage waypoint = message.taskspaceTrajectoryPoints[0];
      drcSimulationTestHelper.send(message);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 * getRobotModel().getControllerDT());

      String pelvisName = fullRobotModel.getPelvis().getName();
      EndToEndTestTools.assertNumberOfPoints(pelvisName, 2, scs);
      EndToEndTestTools.assertWaypointInGeneratorMatches(pelvisName, 1, waypoint, scs, epsilon);

      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(trajectoryTime);
      EndToEndTestTools.assertCurrentDesiredsMatchWaypoint(pelvisName, waypoint, scs, epsilon);
   }

   public void testWalking() throws SimulationExceededMaximumTimeException
   {
      double epsilon = 1.0e-4;
      int steps = 4;

      FootstepDataListMessage footsteps = new FootstepDataListMessage();
      double walkingTime = createWalkingMessage(steps, footsteps, true);
      drcSimulationTestHelper.send(footsteps);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(walkingTime + 0.5);

      assertEquals("Control Mode", PelvisOrientationControlMode.WALKING_CONTROLLER, findCurrentControlMode());
      humanoidReferenceFrames.updateFrames();
      ReferenceFrame midFeetZUpFrame = humanoidReferenceFrames.getMidFootZUpGroundFrame();
      FrameOrientation midFeetOrientation = new FrameOrientation(midFeetZUpFrame);
      midFeetOrientation.changeFrame(worldFrame);
      String pelvisName = fullRobotModel.getPelvis().getName();
      EndToEndTestTools.assertCurrentDesiredsMatch(pelvisName, midFeetOrientation.getQuaternion(), zeroVector, scs, epsilon);
   }

   public void testWalkingAfterTrajectory() throws SimulationExceededMaximumTimeException
   {
      double epsilon = 1.0e-4;

      WalkingControllerParameters walkingControllerParameters = getRobotModel().getWalkingControllerParameters();
      if (!walkingControllerParameters.doPreparePelvisForLocomotion())
      {
         fail("This test requires that doPreparePelvisForLocomotion() is true.");
      }

      assertEquals("Control Mode", PelvisOrientationControlMode.WALKING_CONTROLLER, findCurrentControlMode());
      testSingleTrajectoryPoint();
      assertEquals("Control Mode", PelvisOrientationControlMode.USER, findCurrentControlMode());

      humanoidReferenceFrames.updateFrames();
      ReferenceFrame midFeetZUpGroundFrame = humanoidReferenceFrames.getMidFootZUpGroundFrame();

      String pelvisName = fullRobotModel.getPelvis().getName();
      Quaternion currentDesired = EndToEndTestTools.findControllerDesiredOrientation(pelvisName, scs);
      FrameOrientation desiredAfterTrajectory = new FrameOrientation(worldFrame, currentDesired);
      desiredAfterTrajectory.changeFrame(midFeetZUpGroundFrame);

      int steps = 2;
      FootstepDataListMessage footsteps = new FootstepDataListMessage();
      double walkingTime = createWalkingMessage(steps, footsteps, false);
      drcSimulationTestHelper.send(footsteps);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(walkingTime + 0.5);
      assertEquals("Control Mode", PelvisOrientationControlMode.WALKING_CONTROLLER, findCurrentControlMode());

      humanoidReferenceFrames.updateFrames();
      desiredAfterTrajectory.changeFrame(worldFrame);
      EndToEndTestTools.assertCurrentDesiredsMatch(pelvisName, desiredAfterTrajectory.getQuaternion(), zeroVector, scs, epsilon);
   }

   public void testMultipleTrajectoryPoints() throws SimulationExceededMaximumTimeException
   {
      double epsilon = 1.0e-10;
      int numberOfPoints = 23;
      double yawMagnitude = Math.toRadians(5.0);
      double pitchMagnitude = Math.toRadians(-10.0);
      double rollMagnitude = Math.toRadians(-5.0);
      double timePerPoint = 0.1;
      double frequency = 1.0;

      humanoidReferenceFrames.updateFrames();
      ReferenceFrame pelvisFrame = humanoidReferenceFrames.getPelvisFrame();
      FrameOrientation initialOrientation = new FrameOrientation(pelvisFrame);
      initialOrientation.changeFrame(worldFrame);

      PelvisOrientationTrajectoryMessage message = new PelvisOrientationTrajectoryMessage(numberOfPoints);
      message.getFrameInformation().setTrajectoryReferenceFrame(worldFrame);
      Quaternion previousOrientation = initialOrientation.getQuaternionCopy();
      QuaternionCalculus quaternionCalculus = new QuaternionCalculus();

      for (int point = 0; point < numberOfPoints; point++)
      {
         double time = timePerPoint * (point + 1);
         double factor = Math.sin(2.0 * Math.PI * time * frequency);

         Quaternion orientation = new Quaternion();
         orientation.appendYawRotation(yawMagnitude * factor);
         orientation.appendPitchRotation(pitchMagnitude * factor);
         orientation.appendRollRotation(rollMagnitude * factor);
         FrameOrientation frameOrientation = new FrameOrientation(pelvisFrame, orientation);
         frameOrientation.changeFrame(worldFrame);
         frameOrientation.get(orientation);

         Vector4D derivative = new Vector4D();
         quaternionCalculus.computeQDotByFiniteDifferenceCentral(previousOrientation, orientation, timePerPoint, derivative);
         Vector3D angularVelocity = new Vector3D();
         quaternionCalculus.computeAngularVelocityInWorldFrame(orientation, derivative, angularVelocity);

         if (point == numberOfPoints - 1)
            angularVelocity.setToZero();

         message.setTrajectoryPoint(point, time, orientation, angularVelocity, worldFrame);

         previousOrientation = orientation;
      }

      drcSimulationTestHelper.send(message);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 * getRobotModel().getControllerDT());

      String pelvisName = fullRobotModel.getPelvis().getName();
      EndToEndTestTools.assertNumberOfPoints(pelvisName, numberOfPoints + 1, scs);
      for (int point = 1; point < RigidBodyTaskspaceControlState.maxPointsInGenerator; point++)
      {
         SO3TrajectoryPointMessage waypoint = message.getTrajectoryPoint(point - 1);
         EndToEndTestTools.assertWaypointInGeneratorMatches(pelvisName, point, waypoint, scs, epsilon);
      }

      double simulationTime = timePerPoint * numberOfPoints + 0.5;
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      SO3TrajectoryPointMessage waypoint = message.getTrajectoryPoint(numberOfPoints - 1);
      EndToEndTestTools.assertCurrentDesiredsMatchWaypoint(pelvisName, waypoint, scs, epsilon);
   }

   public void testWalkingWithUserControl() throws SimulationExceededMaximumTimeException
   {
      double trajectoryTime = 0.5;
      Quaternion desiredOrientation = new Quaternion();
      ReferenceFrame midFootZUpGroundFrame = humanoidReferenceFrames.getMidFootZUpGroundFrame();

      PelvisOrientationTrajectoryMessage message = new PelvisOrientationTrajectoryMessage(trajectoryTime, desiredOrientation, midFootZUpGroundFrame);
      message.setEnableUserPelvisControlDuringWalking(true);

      assertEquals("Control Mode", PelvisOrientationControlMode.WALKING_CONTROLLER, findCurrentControlMode());
      drcSimulationTestHelper.send(message);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(trajectoryTime);
      assertEquals("Control Mode", PelvisOrientationControlMode.USER, findCurrentControlMode());

      FootstepDataListMessage footsteps = new FootstepDataListMessage();
      double walkingTime = createWalkingMessage(4, footsteps, true);
      drcSimulationTestHelper.send(footsteps);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(walkingTime / 2.0);
      assertEquals("Control Mode", PelvisOrientationControlMode.USER, findCurrentControlMode());
   }

   public void testCustomControlFrame() throws SimulationExceededMaximumTimeException
   {
      double pitch = Math.toRadians(20.0);
      double chestTrajectoryTime = 1.0;
      double epsilon = 1.0E-3;
      Quaternion desiredOrientation = new Quaternion();
      humanoidReferenceFrames.updateFrames();
      ReferenceFrame chestFrame = humanoidReferenceFrames.getChestFrame();

      // first hold the chest in world to avoid feedback effects
      FrameOrientation chestOrientation = new FrameOrientation(chestFrame);
      chestOrientation.changeFrame(worldFrame);
      ChestTrajectoryMessage holdChestInWorldMessage = new ChestTrajectoryMessage(0.0, chestOrientation.getQuaternion(), worldFrame, worldFrame);
      drcSimulationTestHelper.send(holdChestInWorldMessage);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 * getRobotModel().getControllerDT());

      // now hold the pelvis in chest frame
      PelvisOrientationTrajectoryMessage holdPelvisInChestMessage = new PelvisOrientationTrajectoryMessage(0.0, desiredOrientation, chestFrame);
      drcSimulationTestHelper.send(holdPelvisInChestMessage);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 * getRobotModel().getControllerDT());

      // finally pitch the chest forward and assert that the pelvis follows
      humanoidReferenceFrames.updateFrames();
      Quaternion desiredChestOrientation = new Quaternion();
      desiredChestOrientation.appendPitchRotation(pitch);
      FrameOrientation frameChestOrientation = new FrameOrientation(chestFrame, desiredChestOrientation);
      frameChestOrientation.changeFrame(worldFrame);
      frameChestOrientation.get(desiredChestOrientation);
      ChestTrajectoryMessage chestMessage = new ChestTrajectoryMessage(chestTrajectoryTime, desiredChestOrientation, worldFrame, worldFrame);
      drcSimulationTestHelper.send(chestMessage);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(chestTrajectoryTime + 0.5);

      String pelvisName = fullRobotModel.getPelvis().getName();
      EndToEndTestTools.assertCurrentDesiredsMatch(pelvisName, desiredChestOrientation, zeroVector, scs, epsilon);
   }

   @SuppressWarnings("unchecked")
   private PelvisOrientationControlMode findCurrentControlMode()
   {
      String managerName = PelvisOrientationManager.class.getSimpleName();
      YoVariable<?> variable = scs.getVariable(managerName, managerName + "State");
      return ((EnumYoVariable<PelvisOrientationControlMode>) variable).getEnumValue();
   }

   private double createWalkingMessage(int steps, FootstepDataListMessage messageToPack, boolean squareUp)
   {
      WalkingControllerParameters walkingControllerParameters = getRobotModel().getWalkingControllerParameters();
      double swingDuration = walkingControllerParameters.getDefaultSwingTime();
      double transferDuration = walkingControllerParameters.getDefaultTransferTime();
      double stepLength = 0.6 * walkingControllerParameters.getDefaultStepLength();
      double stepWidth = stepLength / 2.0;
      RobotSide robotSide = RobotSide.LEFT;
      ReferenceFrame midFootZUpGroundFrame = humanoidReferenceFrames.getMidFootZUpGroundFrame();
      double time = walkingControllerParameters.getDefaultInitialTransferTime();
      messageToPack.clear();
      messageToPack.setDefaultSwingDuration(swingDuration);
      messageToPack.setDefaultTransferDuration(transferDuration);
      for (int step = 0; step < steps; step++)
      {
         FramePoint location = new FramePoint(midFootZUpGroundFrame);
         if (squareUp && step == steps - 1)
            location.setX(stepLength * step);
         else
            location.setX(stepLength * (step + 1));
         location.setY(robotSide.negateIfRightSide(stepWidth / 2.0));
         location.changeFrame(worldFrame);
         FrameOrientation orientation = new FrameOrientation(midFootZUpGroundFrame);
         orientation.changeFrame(worldFrame);
         FootstepDataMessage footstep = new FootstepDataMessage(robotSide, location.getPoint(), orientation.getQuaternion());
         messageToPack.add(footstep);
         robotSide = robotSide.getOppositeSide();
         time += swingDuration + transferDuration;
      }
      return time;
   }

   @Before
   public void showMemoryUsageBeforeTest() throws SimulationExceededMaximumTimeException
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT_BUT_ALMOST_PI;
      FlatGroundEnvironment environment = new FlatGroundEnvironment();
      drcSimulationTestHelper = new DRCSimulationTestHelper(environment, getClass().getSimpleName(), selectedLocation, simulationTestingParameters, getRobotModel());
      drcSimulationTestHelper.setupCameraForUnitTest(new Point3D(0.0, 0.0, 0.8), new Point3D(-7.0, -9.0, 4.0));
      ThreadTools.sleep(1000);
      assertTrue(drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5));

      fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
      humanoidReferenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      scs = drcSimulationTestHelper.getSimulationConstructionSet();

      humanoidReferenceFrames.updateFrames();
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
}
