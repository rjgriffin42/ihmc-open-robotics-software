package us.ihmc.atlas;

import org.junit.Assume;
import org.junit.Test;

import org.junit.internal.AssumptionViolatedException;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;

import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public class AtlasFlatGroundWalkingTest extends DRCFlatGroundWalkingTest
{

   private DRCRobotModel robotModel;

   @Test
   public void testAtlasFlatGroundWalking() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String runName = "AtlasFlatGroundWalkingTest";
      robotModel = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, false, false);

      boolean doPelvisYawWarmup = true;
      setupAndTestFlatGroundSimulationTrack(robotModel, runName, doPelvisYawWarmup);
   }

   @Test
   public void testFlatGroundWalkingRunsSameWayTwice() throws SimulationExceededMaximumTimeException
   {
      try
      {
         Assume.assumeTrue(BambooTools.isNightlyBuild());
         BambooTools.reportTestStartedMessage();

         robotModel = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, false, false);

         setupAndTestFlatGroundSimulationTrackTwice(robotModel);
      }
      catch(AssumptionViolatedException e)
      {
         System.out.println("Not Nightly Build, skipping AtlasFlatGroundWalkingTest.testFlatGroundWalkingRunsSameWayTwice");
      }
   }

   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }
}
