package us.ihmc.darpaRoboticsChallenge.controllerAPI;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFFullHumanoidRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage.BaseForControl;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTestTools;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.thread.ThreadTools;
import us.ihmc.wholeBodyController.parameters.DefaultArmConfigurations.ArmConfigurations;

public abstract class EndToEndWholeBodyTrajectoryMessageTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private DRCSimulationTestHelper drcSimulationTestHelper;

   @DeployableTestMethod(estimatedDuration = 50.0)
   @Test //(timeout = 300000)
   public void testSingleWaypoint() throws Exception
   {
      BambooTools.reportTestStartedMessage();

      Random random = new Random(564574L);
      double epsilon = 1.0e-10;

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), "", selectedLocation, simulationTestingParameters, getRobotModel());

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();

      SDFFullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      RobotSide footSide = RobotSide.LEFT;
      // First need to pick up the foot:
      RigidBody foot = fullRobotModel.getFoot(footSide);
      FramePose footPoseCloseToActual = new FramePose(foot.getBodyFixedFrame());
      footPoseCloseToActual.setPosition(0.0, 0.0, 0.10);
      footPoseCloseToActual.changeFrame(ReferenceFrame.getWorldFrame());
      Point3d desiredPosition = new Point3d();
      Quat4d desiredOrientation = new Quat4d();
      footPoseCloseToActual.getPose(desiredPosition, desiredOrientation);

      FootTrajectoryMessage footTrajectoryMessage = new FootTrajectoryMessage(footSide, 0.0, desiredPosition, desiredOrientation);
      drcSimulationTestHelper.send(footTrajectoryMessage);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0 + getRobotModel().getCapturePointPlannerParameters().getDoubleSupportInitialTransferDuration());
      assertTrue(success);

      // Now we can do the usual test.
      double trajectoryTime = 1.0;
      FramePose desiredFootPose = new FramePose(foot.getBodyFixedFrame());
      desiredFootPose.setOrientation(RandomTools.generateRandomQuaternion(random, 1.0));
      desiredFootPose.setPosition(RandomTools.generateRandomPoint(random, -0.1, -0.1, 0.05, 0.1, 0.2, 0.3));
      desiredFootPose.changeFrame(ReferenceFrame.getWorldFrame());
      desiredFootPose.getPose(desiredPosition, desiredOrientation);
      wholeBodyTrajectoryMessage.setFootTrajectoryMessage(new FootTrajectoryMessage(footSide, trajectoryTime, desiredPosition, desiredOrientation));

      SideDependentList<FramePose> desiredHandPoses = new SideDependentList<>();

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody chest = fullRobotModel.getChest();
         RigidBody hand = fullRobotModel.getHand(robotSide);
         OneDoFJoint[] arm = ScrewTools.createOneDoFJointPath(chest, hand);
         OneDoFJoint[] armClone = ScrewTools.cloneOneDoFJointPath(chest, hand);
         for (int i = 0; i < armClone.length; i++)
         {
            OneDoFJoint joint = armClone[i];
            joint.setQ(arm[i].getQ() + RandomTools.generateRandomDouble(random, -0.2, 0.2));
         }
         RigidBody handClone = armClone[armClone.length - 1].getSuccessor();
         FramePose desiredRandomHandPose = new FramePose(handClone.getBodyFixedFrame());
         desiredRandomHandPose.changeFrame(ReferenceFrame.getWorldFrame());
         desiredHandPoses.put(robotSide, desiredRandomHandPose);
         desiredPosition = new Point3d();
         desiredOrientation = new Quat4d();
         desiredRandomHandPose.getPose(desiredPosition, desiredOrientation);
         wholeBodyTrajectoryMessage.setHandTrajectoryMessage(new HandTrajectoryMessage(robotSide, BaseForControl.WORLD, trajectoryTime, desiredPosition, desiredOrientation));
      }


      RigidBody pelvis = fullRobotModel.getPelvis();
      FramePose desiredPelvisPose = new FramePose(pelvis.getBodyFixedFrame());
      desiredPelvisPose.setOrientation(RandomTools.generateRandomQuaternion(random, 1.0));
      desiredPelvisPose.setPosition(RandomTools.generateRandomPoint(random, 0.05, 0.03, 0.05));
      desiredPelvisPose.setZ(desiredPelvisPose.getZ() - 0.1);
      desiredPosition = new Point3d();
      desiredOrientation = new Quat4d();
      desiredPelvisPose.changeFrame(ReferenceFrame.getWorldFrame());
      desiredPelvisPose.getPose(desiredPosition, desiredOrientation);
      wholeBodyTrajectoryMessage.setPelvisTrajectoryMessage(new PelvisTrajectoryMessage(trajectoryTime, desiredPosition, desiredOrientation));

      FrameOrientation desiredChestOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame(), RandomTools.generateRandomQuaternion(random, 0.5));
      desiredChestOrientation.changeFrame(ReferenceFrame.getWorldFrame());
      desiredOrientation = new Quat4d();
      desiredChestOrientation.getQuaternion(desiredOrientation);
      wholeBodyTrajectoryMessage.setChestTrajectoryMessage(new ChestTrajectoryMessage(trajectoryTime, desiredOrientation));

      drcSimulationTestHelper.send(wholeBodyTrajectoryMessage);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0 + trajectoryTime);
      assertTrue(success);

      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();

      EndToEndChestTrajectoryMessageTest.assertSingleWaypointExecuted(desiredChestOrientation, scs);
//      EndToEndPelvisTrajectoryMessageTest.assertSingleWaypointExecuted(desiredPosition, desiredOrientation, scs);
      EndToEndFootTrajectoryMessageTest.assertSingleWaypointExecuted(footSide, desiredFootPose.getFramePointCopy().getPoint(), desiredFootPose.getFrameOrientationCopy().getQuaternion(), scs);
      for (RobotSide robotSide : RobotSide.values)
         EndToEndHandTrajectoryMessageTest.assertSingleWaypointExecuted(robotSide, desiredHandPoses.get(robotSide).getFramePointCopy().getPoint(), desiredHandPoses.get(robotSide).getFrameOrientationCopy().getQuaternion(), scs);
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
