package us.ihmc.atlas;

import us.ihmc.atlas.parameters.AtlasContactPointParameters;
import us.ihmc.commonAvatarInterfaces.CommonAvatarEnvironmentInterface;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseDemoStarter;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.environment.DRCWallWorldEnvironment;
import us.ihmc.darpaRoboticsChallenge.visualization.SliderBoardFactory;
import us.ihmc.darpaRoboticsChallenge.visualization.WalkControllerSliderBoard;
import us.ihmc.utilities.processManagement.JavaProcessSpawner;

import com.martiansoftware.jsap.JSAPException;

public class AtlasWallWorldDemo extends DRCObstacleCourseDemoStarter
{
   private static final AtlasRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS, false, false);

   public static void main(final String[] args) throws JSAPException
   {
      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
      AtlasWallWorldDemo atlasDemo = new AtlasWallWorldDemo();
      boolean automaticallyStartSimulation = true;
      boolean startDRCNetworkProcessor = true;
      boolean initializeEstimatorToActual = false;

      CommonAvatarEnvironmentInterface environment = new DRCWallWorldEnvironment(-10.0, 10.0);
//      double stepHeight = 0.2;
//      CommonAvatarEnvironmentInterface environment = new BigStepUpWithHandPlatformEnvironment(stepHeight);
      
      AtlasContactPointParameters contactPointParameters = robotModel.getContactPointParameters();
      contactPointParameters.createHandKnobContactPoints();
      
      atlasDemo.obstacleCourseStarter(environment.getTerrainObject3D(), robotModel, sliderBoardFactory, initializeEstimatorToActual, automaticallyStartSimulation,
            startDRCNetworkProcessor);
   }

   @Override
   public void SpawnUI(DRCRobotModel robotModel)
   {
      AtlasRobotModel atlasRobotModel = (AtlasRobotModel) robotModel;
      JavaProcessSpawner spawner = new JavaProcessSpawner(true);
      String[] args = {"-m " + atlasRobotModel.getAtlasVersion().name()};
      spawner.spawn(AtlasOperatorUserInterface.class, args);
   }
}
