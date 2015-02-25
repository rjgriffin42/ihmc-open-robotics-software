package us.ihmc.darpaRoboticsChallenge.roughTerrainWalking;

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
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.TrajectoryType;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created by agrabertilton on 2/25/15.
 */
public abstract class DRCSwingTrajectoryTest implements MultiRobotTestInterface
{

   private SimulationTestingParameters simulationTestingParameters;
   private DRCSimulationTestHelper drcSimulationTestHelper;

   private class TestController implements RobotController
   {
      YoVariableRegistry registry = new YoVariableRegistry("SwingHeightTestController");
      Random random = new Random();
      FullRobotModel estimatorModel;
      DoubleYoVariable maxFootHeight = new DoubleYoVariable("maxFootHeight", registry);
      DoubleYoVariable leftFootHeight = new DoubleYoVariable("leftFootHeight", registry);
      DoubleYoVariable rightFootHeight = new DoubleYoVariable("rightFootHeight", registry);
      DoubleYoVariable randomNum = new DoubleYoVariable("randomNumberInTestController", registry);
      FramePoint leftFootOrigin;
      FramePoint rightFootOrigin;

      public TestController(FullRobotModel estimatorModel){
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

      public double getMaxFootHeight(){ return maxFootHeight.getDoubleValue();}
   }

   @BambooAnnotations.AverageDuration
   @Test(timeout = 300000)
   public void testMultipleHeightFootsteps() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double[] heights = {0.1, 0.2, 0.3};
      double[] maxHeights = new double[heights.length];
      boolean success;
      for (int i = 0; i < heights.length; i++){
         double currentHeight = heights[i];
         DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
         simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
         simulationTestingParameters.setRunMultiThreaded(false);
         drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOverSmallPlatformTest", "", selectedLocation,  simulationTestingParameters, getRobotModel());
         FullRobotModel estimatorRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
         TestController testController = new TestController(estimatorRobotModel);
         drcSimulationTestHelper.getRobot().setController(testController, 1);

         SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();


         ThreadTools.sleep(1000);
         success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0); //2.0);

         FootstepDataList footstepDataList = createBasicFootstepFromDefaultForSwingHeightTest(currentHeight);
         drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);
         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0);
         maxHeights[i] = testController.getMaxFootHeight();
         assertTrue(success);

         if (i != heights.length - 1)
            drcSimulationTestHelper.destroySimulation();
      }

      for (int i = 0; i < heights.length -1; i++){
         if (heights[i] > heights[i+1]){
            assertTrue(maxHeights[i] > maxHeights[i+1]);
         }else{
            assertTrue(maxHeights[i] < maxHeights[i+1]);
         }
      }

      BambooTools.reportTestFinishedMessage();
   }


   private FootstepDataList createBasicFootstepFromDefaultForSwingHeightTest(double swingHeight)
   {
      FootstepDataList desiredFootsteps =  new FootstepDataList(0.0, 0.0);
      FootstepData footstep = new FootstepData(RobotSide.RIGHT, new Point3d(0.4, -0.125, 0.085), new Quat4d(0, 0, 0, 1));
      footstep.setTrajectoryType(TrajectoryType.OBSTACLE_CLEARANCE);
      footstep.setSwingHeight(swingHeight);
      desiredFootsteps.footstepDataList.add(footstep);
      return desiredFootsteps;
   }


}
