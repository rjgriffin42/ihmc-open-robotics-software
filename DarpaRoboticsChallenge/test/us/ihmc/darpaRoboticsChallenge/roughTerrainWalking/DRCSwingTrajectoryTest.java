package us.ihmc.darpaRoboticsChallenge.roughTerrainWalking;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.robotics.geometry.BoundingBox3d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

/**
 * Created by agrabertilton on 2/25/15.
 */
public abstract class DRCSwingTrajectoryTest implements MultiRobotTestInterface
{
   private SimulationTestingParameters simulationTestingParameters;
   private DRCSimulationTestHelper drcSimulationTestHelper;

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

      simulationTestingParameters = null;
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   private class TestController implements RobotController
   {
      YoVariableRegistry registry = new YoVariableRegistry("SwingHeightTestController");
      Random random = new Random();
      FullHumanoidRobotModel estimatorModel;
      DoubleYoVariable maxFootHeight = new DoubleYoVariable("maxFootHeight", registry);
      DoubleYoVariable leftFootHeight = new DoubleYoVariable("leftFootHeight", registry);
      DoubleYoVariable rightFootHeight = new DoubleYoVariable("rightFootHeight", registry);
      DoubleYoVariable randomNum = new DoubleYoVariable("randomNumberInTestController", registry);
      FramePoint leftFootOrigin;
      FramePoint rightFootOrigin;

      public TestController(FullHumanoidRobotModel estimatorModel)
      {
         this.estimatorModel = estimatorModel;
      }

      @Override
      public void doControl()
      {
         ReferenceFrame leftFootFrame = estimatorModel.getFoot(RobotSide.LEFT).getBodyFixedFrame();
         leftFootOrigin = new FramePoint(leftFootFrame);
         leftFootOrigin.changeFrame(ReferenceFrame.getWorldFrame());

         ReferenceFrame rightFootFrame = estimatorModel.getFoot(RobotSide.RIGHT).getBodyFixedFrame();
         rightFootOrigin = new FramePoint(rightFootFrame);
         rightFootOrigin.changeFrame(ReferenceFrame.getWorldFrame());

         randomNum.set(random.nextDouble());
         leftFootHeight.set(leftFootOrigin.getZ());
         rightFootHeight.set(rightFootOrigin.getZ());
         maxFootHeight.set(Math.max(maxFootHeight.getDoubleValue(), leftFootHeight.getDoubleValue()));
         maxFootHeight.set(Math.max(maxFootHeight.getDoubleValue(), rightFootHeight.getDoubleValue()));
      }

      @Override
      public void initialize()
      {
      }

      @Override
      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      @Override
      public String getName()
      {
         return null;
      }

      @Override
      public String getDescription()
      {
         return null;
      }

      public double getMaxFootHeight()
      {
         return maxFootHeight.getDoubleValue();
      }
   }


	@EstimatedDuration(duration = 82.0)
   @Test(timeout = 409848)
   public void testMultipleHeightFootsteps() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double[] heights = {0.1, 0.2, 0.3};
      double[] maxHeights = new double[heights.length];
      boolean success;
      for (int i = 0; i < heights.length; i++)
      {
         double currentHeight = heights[i];
         DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
         simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
         simulationTestingParameters.setRunMultiThreaded(false);
         drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOverSmallPlatformTest", "", selectedLocation, simulationTestingParameters,
                 getRobotModel());
         FullHumanoidRobotModel estimatorRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
         TestController testController = new TestController(estimatorRobotModel);
         drcSimulationTestHelper.getRobot().setController(testController, 1);

         SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();


         ThreadTools.sleep(1000);
         success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);    // 2.0);

         FootstepDataList footstepDataList = createBasicFootstepFromDefaultForSwingHeightTest(currentHeight);
         drcSimulationTestHelper.send(footstepDataList);
         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0);
         maxHeights[i] = testController.getMaxFootHeight();
         assertTrue(success);

         if (i != heights.length - 1)
            drcSimulationTestHelper.destroySimulation();
      }

      for (int i = 0; i < heights.length - 1; i++)
      {
         if (heights[i] > heights[i + 1])
         {
            assertTrue(maxHeights[i] > maxHeights[i + 1]);
         }
         else
         {
            assertTrue(maxHeights[i] < maxHeights[i + 1]);
         }
      }

      BambooTools.reportTestFinishedMessage();
   }

	@EstimatedDuration(duration = 38.3)
   @Test(timeout = 191531)
   public void testReallyHighFootstep() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      boolean success;
      double currentHeight = 0.6;
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
      simulationTestingParameters.setRunMultiThreaded(false);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOverSmallPlatformTest", "", selectedLocation, simulationTestingParameters,
              getRobotModel());

      ThreadTools.sleep(1000);
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);    // 2.0);

      FootstepDataList footstepDataList = createFootstepsForSwingHeightTest(currentHeight);
      drcSimulationTestHelper.send(footstepDataList);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(8.0);
      assertTrue(success);

      Point3d center = new Point3d(1.2, 0.0, .75);
      Vector3d plusMinusVector = new Vector3d(0.2, 0.2, 0.5);
      BoundingBox3d boundingBox = BoundingBox3d.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
      drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

      BambooTools.reportTestFinishedMessage();
   }

	@EstimatedDuration(duration = 32.5)
   @Test(timeout = 162688)
   public void testNegativeSwingHeight() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      boolean success;
      double currentHeight = -0.1;
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
      simulationTestingParameters.setRunMultiThreaded(false);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOverSmallPlatformTest", "", selectedLocation, simulationTestingParameters,
            getRobotModel());

      ThreadTools.sleep(1000);
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);    // 2.0);

      FootstepDataList footstepDataList = createFootstepsForSwingHeightTest(currentHeight);
      drcSimulationTestHelper.send(footstepDataList);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(6.0);
      assertTrue(success);

      Point3d center = new Point3d(1.2, 0.0, .75);
      Vector3d plusMinusVector = new Vector3d(0.2, 0.2, 0.5);
      BoundingBox3d boundingBox = BoundingBox3d.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
      drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

      BambooTools.reportTestFinishedMessage();
   }

   private FootstepDataList createBasicFootstepFromDefaultForSwingHeightTest(double swingHeight)
   {
      FootstepDataList desiredFootsteps = new FootstepDataList(0.0, 0.0);
      FootstepData footstep = new FootstepData(RobotSide.RIGHT, new Point3d(0.4, -0.125, 0.085), new Quat4d(0, 0, 0, 1));
      footstep.setTrajectoryType(TrajectoryType.OBSTACLE_CLEARANCE);
      footstep.setSwingHeight(swingHeight);
      desiredFootsteps.footstepDataList.add(footstep);

      return desiredFootsteps;
   }

   private FootstepDataList createFootstepsForSwingHeightTest(double swingHeight)
   {
      FootstepDataList desiredFootsteps = new FootstepDataList(0.0, 0.0);
      FootstepData footstep = new FootstepData(RobotSide.RIGHT, new Point3d(0.6, -0.125, 0.085), new Quat4d(0, 0, 0, 1));
      footstep.setTrajectoryType(TrajectoryType.OBSTACLE_CLEARANCE);
      footstep.setSwingHeight(swingHeight);
      desiredFootsteps.footstepDataList.add(footstep);

      footstep = new FootstepData(RobotSide.LEFT, new Point3d(1.2, 0.125, 0.085), new Quat4d(0, 0, 0, 1));
      footstep.setTrajectoryType(TrajectoryType.OBSTACLE_CLEARANCE);
      footstep.setSwingHeight(swingHeight);
      desiredFootsteps.footstepDataList.add(footstep);

      footstep = new FootstepData(RobotSide.RIGHT, new Point3d(1.2, -0.125, 0.085), new Quat4d(0, 0, 0, 1));
      footstep.setTrajectoryType(TrajectoryType.OBSTACLE_CLEARANCE);
      footstep.setSwingHeight(swingHeight);
      desiredFootsteps.footstepDataList.add(footstep);
      return desiredFootsteps;
   }



}
