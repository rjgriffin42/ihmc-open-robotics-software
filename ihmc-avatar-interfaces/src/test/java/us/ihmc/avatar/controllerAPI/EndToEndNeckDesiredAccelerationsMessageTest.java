package us.ihmc.avatar.controllerAPI;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.After;
import org.junit.Before;

import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlMode;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyUserControlState;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyInverseDynamicsSolver;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.humanoidRobotics.communication.packets.walking.NeckDesiredAccelerationsMessage;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.yoVariables.variable.YoEnum;

public abstract class EndToEndNeckDesiredAccelerationsMessageTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();

   private DRCSimulationTestHelper drcSimulationTestHelper;

   public void testSimpleCommands() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      Random random = new Random(564654L);

      drcSimulationTestHelper = new DRCSimulationTestHelper(simulationTestingParameters, getRobotModel());
      drcSimulationTestHelper.createSimulation(getClass().getSimpleName());

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      RigidBody chest = fullRobotModel.getChest();
      RigidBody head = fullRobotModel.getHead();
      String headName = head.getName();
      OneDoFJoint[] neckJoints = ScrewTools.createOneDoFJointPath(chest, head);
      double[] neckDesiredJointAccelerations = RandomNumbers.nextDoubleArray(random, neckJoints.length, 0.1);
      NeckDesiredAccelerationsMessage neckDesiredAccelerationsMessage = new NeckDesiredAccelerationsMessage(neckDesiredJointAccelerations);

      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      assertEquals(RigidBodyControlMode.JOINTSPACE, findControllerState(headName, scs));

      drcSimulationTestHelper.send(neckDesiredAccelerationsMessage);
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(RigidBodyUserControlState.TIME_WITH_NO_MESSAGE_BEFORE_ABORT - 0.05);
      assertTrue(success);

      assertEquals(RigidBodyControlMode.USER, findControllerState(headName, scs));
      double[] controllerDesiredJointAccelerations = findControllerDesiredJointAccelerations(neckJoints, headName, scs);
      assertArrayEquals(neckDesiredJointAccelerations, controllerDesiredJointAccelerations, 1.0e-10);
      double[] qpOutputJointAccelerations = findQPOutputJointAccelerations(neckJoints, scs);
      assertArrayEquals(neckDesiredJointAccelerations, qpOutputJointAccelerations, 1.0e-3);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.07);
      assertTrue(success);

      assertEquals(RigidBodyControlMode.JOINTSPACE, findControllerState(headName, scs));
   }

   @SuppressWarnings("unchecked")
   public static RigidBodyControlMode findControllerState(String bodyName, SimulationConstructionSet scs)
   {
      String headOrientatManagerName = bodyName + "Manager";
      String headControlStateName = headOrientatManagerName + "State";
      return ((YoEnum<RigidBodyControlMode>) scs.getVariable(headOrientatManagerName, headControlStateName)).getEnumValue();
   }

   public static double[] findQPOutputJointAccelerations(OneDoFJoint[] neckJoints, SimulationConstructionSet scs)
   {
      double[] qdd_ds = new double[neckJoints.length];
      for (int i = 0; i < neckJoints.length; i++)
      {
         qdd_ds[i] = scs.getVariable(WholeBodyInverseDynamicsSolver.class.getSimpleName(), "qdd_qp_" + neckJoints[i].getName()).getValueAsDouble();
      }
      return qdd_ds;
   }

   public static double[] findControllerDesiredJointAccelerations(OneDoFJoint[] neckJoints, String bodyName, SimulationConstructionSet scs)
   {
      double[] qdd_ds = new double[neckJoints.length];
      String nameSpace = bodyName + "UserControlModule";

      for (int i = 0; i < neckJoints.length; i++)
      {
         String variable = bodyName + "UserMode_" + neckJoints[i].getName() + "_qdd_d";
         qdd_ds[i] = scs.getVariable(nameSpace, variable).getValueAsDouble();
      }
      return qdd_ds;
   }

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
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
}
