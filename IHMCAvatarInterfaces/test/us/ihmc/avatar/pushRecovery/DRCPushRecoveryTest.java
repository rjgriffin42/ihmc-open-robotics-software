package us.ihmc.avatar.pushRecovery;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.partNames.LimbName;
import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.WalkingHighLevelHumanoidController;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states.WalkingStateEnum;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.robotics.controllers.ControllerFailureException;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.stateMachines.StateTransitionCondition;
import us.ihmc.simulationToolkit.controllers.PushRobotController;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.thread.ThreadTools;

public abstract class DRCPushRecoveryTest
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private DRCSimulationTestHelper drcSimulationTestHelper;

   private static String script = "scripts/ExerciseAndJUnitScripts/walkingPushTestScript.xml";

   private static double simulationTime = 6.0;

   private PushRobotController pushRobotController;

   private double swingTime, transferTime;

   private SideDependentList<StateTransitionCondition> singleSupportStartConditions = new SideDependentList<>();

   private SideDependentList<StateTransitionCondition> doubleSupportStartConditions = new SideDependentList<>();

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      singleSupportStartConditions = null;
      doubleSupportStartConditions = null;
      pushRobotController = null;
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   protected abstract DRCRobotModel getRobotModel();

   @ContinuousIntegrationTest(estimatedDuration =  20.0)
   @Test(timeout = 120000)
   public void testPushICPOptimiWhileInSwing() throws SimulationExceededMaximumTimeException
   {
      setupTest(script, true, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = singleSupportStartConditions.get(RobotSide.RIGHT);
      double delay = 0.5 * swingTime;

      // push parameters:
      Vector3d forceDirection = new Vector3d(0.0, -1.0, 0.0);
      double magnitude = 600.0;
      double duration = 0.1;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
   }


   @ContinuousIntegrationTest(estimatedDuration = 23.7)
   @Test(timeout = 120000)
   public void testPushWhileInSwing() throws SimulationExceededMaximumTimeException
   {
      setupTest(script, true, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = singleSupportStartConditions.get(RobotSide.RIGHT);
      double delay = 0.5 * swingTime;

      // push parameters:
      Vector3d forceDirection = new Vector3d(0.0, -1.0, 0.0);
      double magnitude = 550.0;
      double duration = 0.1;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 22.3)
   @Test(timeout = 110000)
   public void testRecoveringWithSwingSpeedUpWhileInSwing() throws SimulationExceededMaximumTimeException
   {
      setupTest(script, false, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = singleSupportStartConditions.get(RobotSide.RIGHT);
      double delay = 0.25 * swingTime;

      // push parameters:
      Vector3d forceDirection = new Vector3d(0.0, -1.0, 0.0);
      double magnitude = 450.0;
      double duration = 0.1;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 21.1)
   @Test(timeout = 110000)
   public void testPushWhileInTransfer() throws SimulationExceededMaximumTimeException
   {
      setupTest(script, true, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = doubleSupportStartConditions.get(RobotSide.RIGHT);
      double delay = 0.5 * transferTime;

      // push parameters:
      Vector3d forceDirection = new Vector3d(0.0, 1.0, 0.0);
      double magnitude = 450.0;
      double duration = 0.1;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 21.5)
   @Test(timeout = 110000)
   public void testPushWhileStanding() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, true, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 1.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(1.0, 0.0, 0.0);
      double magnitude = 350.0;
      double duration = 0.15;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 21.4)
   @Test(timeout = 110000)
   public void testPushWhileStandingRecoveringAfterControllerFailureKickedIn() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, false, true);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 1.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(1.0, 0.0, 0.0);
      double magnitude = 350.0;
      double duration = 0.15;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 18.7)
   @Test(timeout = 93000)
   public void testLongForwardPushWhileStanding() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, true, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 0.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(1.0, 0.0, 0.0);
      forceDirection.normalize();
      double magnitude = 100.0;
      double duration = 1.0;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(duration + 2.0);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 13.8)
   @Test(timeout = 69000)
   public void testControllerFailureKicksIn() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, false, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 0.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(-1.0, 0.0, 0.0);
      double magnitude = 80.0;
      double duration = 2.0;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);

      try
      {
         drcSimulationTestHelper.simulateAndBlock(duration + 2.0);
         fail("Robot fall has not been detected");
      }
      catch (ControllerFailureException e)
      {
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 27.2)
   @Test(timeout = 140000)
   public void testLongBackwardPushWhileStanding() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, true, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 0.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(-1.0, 0.0, 0.0);
      double magnitude = 100.0;
      double duration = 1.0;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(duration + 2.0);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 20.0)
   @Test(timeout = 100000)
   public void testLongForwardPushWhileStandingAfterControllerFailureKickedIn() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, false, true);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 0.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(1.0, 0.0, 0.0);
      forceDirection.normalize();
      double magnitude = 100.0;
      double duration = 1.0;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(duration + 2.0);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 18.6)
   @Test(timeout = 93000)
   public void testLongBackwardPushWhileStandingAfterControllerFailureKickedIn() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, false, true);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 0.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(-1.0, 0.0, 0.0);
      double magnitude = 100.0;
      double duration = 1.0;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(duration + 2.0);
      assertTrue(success);
   }

   @ContinuousIntegrationTest(estimatedDuration = 16.9)
   @Test(timeout = 84000)
   public void testRecoveryWhileInFlamingoStance() throws SimulationExceededMaximumTimeException
   {
      setupTest(null, false, false);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      RobotSide footSide = RobotSide.LEFT;
      FramePose footPose = new FramePose(
            drcSimulationTestHelper.getDRCSimulationFactory().getControllerFullRobotModel().getEndEffectorFrame(footSide, LimbName.LEG));
      footPose.changeFrame(ReferenceFrame.getWorldFrame());
      footPose.translate(0.0, 0.0, 0.2);
      Point3d desiredFootPosition = new Point3d();
      Quat4d desiredFootOrientation = new Quat4d();
      footPose.getPose(desiredFootPosition, desiredFootOrientation);
      FootTrajectoryMessage footPosePacket = new FootTrajectoryMessage(footSide, 0.6, desiredFootPosition, desiredFootOrientation);
      drcSimulationTestHelper.send(footPosePacket);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 0.0;

      // push parameters:
      Vector3d forceDirection = new Vector3d(0.0, 1.0, 0.0);
      double magnitude = 180.0;
      double duration = 0.2;
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.5);
      assertTrue(success);
   }

   private void setupTest(String scriptName, boolean enablePushRecoveryControlModule, boolean enablePushRecoveryOnFailure) throws SimulationExceededMaximumTimeException
   {
      FlatGroundEnvironment flatGround = new FlatGroundEnvironment();
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      drcSimulationTestHelper = new DRCSimulationTestHelper(flatGround, "DRCSimpleFlatGroundScriptTest", selectedLocation, simulationTestingParameters, getRobotModel());
      FullHumanoidRobotModel fullRobotModel = getRobotModel().createFullRobotModel();
      pushRobotController = new PushRobotController(drcSimulationTestHelper.getRobot(), fullRobotModel);
      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      scs.addYoGraphic(pushRobotController.getForceVisualizer());

      if (scriptName != null && !scriptName.isEmpty())
      {
         drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.001);
         InputStream scriptInputStream = getClass().getClassLoader().getResourceAsStream(scriptName);
         drcSimulationTestHelper.loadScriptFile(scriptInputStream, ReferenceFrame.getWorldFrame());
      }

      // get rid of this once push recovery is enabled by default
      BooleanYoVariable enable = (BooleanYoVariable) scs.getVariable("PushRecoveryControlModule", "enablePushRecovery");
      enable.set(enablePushRecoveryControlModule);
      BooleanYoVariable enableOnFailure = (BooleanYoVariable) scs.getVariable(WalkingHighLevelHumanoidController.class.getSimpleName(),
            "enablePushRecoveryOnFailure");
      enableOnFailure.set(enablePushRecoveryOnFailure);

      for (RobotSide robotSide : RobotSide.values)
      {
         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
         String footPrefix = sidePrefix + "Foot";
         @SuppressWarnings("unchecked")
         final EnumYoVariable<ConstraintType> footConstraintType = (EnumYoVariable<ConstraintType>) scs.getVariable(sidePrefix + "FootControlModule",
               footPrefix + "State");
         @SuppressWarnings("unchecked")
         final EnumYoVariable<WalkingStateEnum> walkingState = (EnumYoVariable<WalkingStateEnum>) scs.getVariable("WalkingHighLevelHumanoidController",
               "walkingState");
         singleSupportStartConditions.put(robotSide, new SingleSupportStartCondition(footConstraintType));
         doubleSupportStartConditions.put(robotSide, new DoubleSupportStartCondition(walkingState, robotSide));
      }

      setupCamera(scs);
      swingTime = getRobotModel().getWalkingControllerParameters().getDefaultSwingTime();
      transferTime = getRobotModel().getWalkingControllerParameters().getDefaultTransferTime();
      ThreadTools.sleep(1000);
   }

   private void setupCamera(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(0.0, 0.0, 0.89);
      Point3d cameraPosition = new Point3d(10.0, 2.0, 1.37);
      drcSimulationTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
   }

   private class SingleSupportStartCondition implements StateTransitionCondition
   {
      private final EnumYoVariable<ConstraintType> footConstraintType;

      public SingleSupportStartCondition(EnumYoVariable<ConstraintType> footConstraintType)
      {
         this.footConstraintType = footConstraintType;
      }

      @Override
      public boolean checkCondition()
      {
         return footConstraintType.getEnumValue() == ConstraintType.SWING;
      }
   }

   private class DoubleSupportStartCondition implements StateTransitionCondition
   {
      private final EnumYoVariable<WalkingStateEnum> walkingState;

      private final RobotSide side;

      public DoubleSupportStartCondition(EnumYoVariable<WalkingStateEnum> walkingState, RobotSide side)
      {
         this.walkingState = walkingState;
         this.side = side;
      }

      @Override
      public boolean checkCondition()
      {
         if (side == RobotSide.LEFT)
         {
            return (walkingState.getEnumValue() == WalkingStateEnum.TO_STANDING) || (walkingState.getEnumValue() == WalkingStateEnum.TO_WALKING_LEFT_SUPPORT);
         }
         else
         {
            return (walkingState.getEnumValue() == WalkingStateEnum.TO_STANDING) || (walkingState.getEnumValue() == WalkingStateEnum.TO_WALKING_RIGHT_SUPPORT);
         }
      }
   }
}
