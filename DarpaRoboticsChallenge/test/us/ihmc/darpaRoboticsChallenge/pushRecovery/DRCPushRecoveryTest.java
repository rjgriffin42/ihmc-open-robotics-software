package us.ihmc.darpaRoboticsChallenge.pushRecovery;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.WalkingState;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.controllers.DRCPushRobotController;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.ground.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.ground.FlatGroundTerrainObject;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionCondition;

public abstract class DRCPushRecoveryTest
{
   private static final boolean showGUI = true;
   private static final boolean KEEP_SCS_UP = false;
   
   private static String script = "scripts/ExerciseAndJUnitScripts/walkingPushTestScript.xml";
   private static double simulationTime = 6.0;
   
   private DRCSimulationTestHelper drcSimulationTestHelper;
   private DRCPushRobotController pushRobotController;
   
   private double swingTime, transferTime;
   private SideDependentList<StateTransitionCondition> singleSupportStartConditions = new SideDependentList<>();
   private SideDependentList<StateTransitionCondition> doubleSupportStartConditions = new SideDependentList<>();
   
   protected abstract DRCRobotModel getRobotModel();
   
   @Test
   public void testPushWhileInSwing() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      setupTest(script);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      
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
      
      BambooTools.reportTestFinishedMessage();
   }
   
   @Test
   public void testPushWhileInTransfer() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      setupTest(script);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      
      // push timing:
      StateTransitionCondition pushCondition = doubleSupportStartConditions.get(RobotSide.LEFT);
      double delay = 0.5 * transferTime;
      
      // push parameters:
      Vector3d forceDirection = new Vector3d(0.0, -1.0, 0.0);
      double magnitude = 600.0;
      double duration = 0.1;
      
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration); 
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
      
      BambooTools.reportTestFinishedMessage();
   }
   
   @Test
   public void testPushWhileStanding() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      setupTest(null);
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      
      // push timing:
      StateTransitionCondition pushCondition = null;
      double delay = 1.0;
      
      // push parameters:
      Vector3d forceDirection = new Vector3d(1.0, 0.0, 0.0);
      double magnitude = 500.0;
      double duration = 0.1;
      
      pushRobotController.applyForceDelayed(pushCondition, delay, forceDirection, magnitude, duration); 
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);
      assertTrue(success);
      
      BambooTools.reportTestFinishedMessage();
   }
   
   private void setupTest(String scriptName)
   {
      String fileName;
      if(scriptName == null)
      {
         fileName = null;
      }
      else
      {
         fileName = BambooTools.getFullFilenameUsingClassRelativeURL(DRCPushRecoveryTest.class, scriptName);
      }
      FlatGroundTerrainObject flatGround = new FlatGroundTerrainObject();
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper(flatGround, "DRCSimpleFlatGroundScriptTest", fileName, selectedLocation, false, showGUI, false, getRobotModel());
      SDFFullRobotModel fullRobotModel = getRobotModel().createFullRobotModel();
      pushRobotController = new DRCPushRobotController(drcSimulationTestHelper.getRobot(), fullRobotModel);
      
      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      scs.addDynamicGraphicObject(pushRobotController.getForceVisualizer());
      
      // get rid of this once push recovery is enabled by default
      BooleanYoVariable enable = (BooleanYoVariable) scs.getVariable("PushRecoveryControlModule", "enablePushRecovery");
      BooleanYoVariable enableDS = (BooleanYoVariable) scs.getVariable("PushRecoveryControlModule","enablePushRecoveryFromDoubleSupport");
      enable.set(true);
      enableDS.set(true);
      
      for (RobotSide robotSide : RobotSide.values)
      {
         String prefix = fullRobotModel.getFoot(robotSide).getName();
         final EnumYoVariable<ConstraintType> footConstraintType = (EnumYoVariable<ConstraintType>) scs.getVariable(prefix + "FootControlModule", prefix + "State");
         final EnumYoVariable<WalkingState> walkingState = (EnumYoVariable<WalkingState>) scs.getVariable("WalkingHighLevelHumanoidController", "walkingState");
         
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
   
   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (KEEP_SCS_UP)
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
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
      private final EnumYoVariable<WalkingState> walkingState;
      private final RobotSide side;

      public DoubleSupportStartCondition(EnumYoVariable<WalkingState> walkingState, RobotSide side)
      {
         this.walkingState = walkingState;
         this.side = side;
      }

      @Override
      public boolean checkCondition()
      {
         if (side == RobotSide.LEFT)
         {
            return walkingState.getEnumValue() == WalkingState.DOUBLE_SUPPORT || walkingState.getEnumValue() == WalkingState.TRANSFER_TO_LEFT_SUPPORT;
         }
         else
         {
            return walkingState.getEnumValue() == WalkingState.DOUBLE_SUPPORT || walkingState.getEnumValue() == WalkingState.TRANSFER_TO_RIGHT_SUPPORT;
         }
      }
   }
}
