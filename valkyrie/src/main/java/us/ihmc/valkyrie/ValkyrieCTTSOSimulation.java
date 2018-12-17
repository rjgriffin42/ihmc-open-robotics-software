package us.ihmc.valkyrie;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.initialSetup.OffsetAndYawRobotInitialSetup;
import us.ihmc.avatar.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.avatar.simulationStarter.DRCSimulationStarter;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.robotEnvironmentAwareness.tools.ConstantPlanarRegionsPublisher;
import us.ihmc.robotEnvironmentAwareness.ui.io.PlanarRegionDataImporter;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.simulationConstructionSetTools.util.environments.PlanarRegionsListDefinedEnvironment;
import us.ihmc.simulationConstructionSetTools.util.environments.planarRegionEnvironments.TwoBollardEnvironment;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ValkyrieCTTSOSimulation
{
   private static final Environment environment = Environment.CINDERS;

   private enum Environment
   {
      JERSEY_BARRIERS("20181210_JerseyBarrierData", 0.71, 0.23, -0.14),
      CINDERS("20181211_CinderBlocksData", 0.4, 0.0, -0.4);

      final String fileName;
      final double startX;
      final double startY;
      final double startYaw;

      Environment(String fileName, double startX, double startY, double startYaw)
      {
         this.fileName = fileName;
         this.startX = startX;
         this.startY = startY;
         this.startYaw = startYaw;
      }
   }

   public static void main(String[] args)
   {
      DRCRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.SCS, false);
      Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("environmentData/" + environment.fileName).getPath());
      PlanarRegionsList planarRegionsList = PlanarRegionDataImporter.importPlanarRegionData(path.toFile());
      PlanarRegionsListDefinedEnvironment simEnvironment = new PlanarRegionsListDefinedEnvironment(planarRegionsList, 0.001, false);

      DRCSimulationStarter simulationStarter = new DRCSimulationStarter(robotModel, simEnvironment);
      simulationStarter.setRunMultiThreaded(true);
      simulationStarter.setInitializeEstimatorToActual(true);
      simulationStarter.setStartingLocation(() -> new OffsetAndYawRobotInitialSetup(environment.startX, environment.startY, 0.001, environment.startYaw));

      DRCNetworkModuleParameters networkProcessorParameters = new DRCNetworkModuleParameters();

      // talk to controller and footstep planner
      networkProcessorParameters.enableControllerCommunicator(true);
      networkProcessorParameters.enableFootstepPlanningToolbox(true);

      // disable everything else
      networkProcessorParameters.enableUiModule(false);
      networkProcessorParameters.enableBehaviorModule(false);
      networkProcessorParameters.enableBehaviorVisualizer(false);
      networkProcessorParameters.enableSensorModule(true);
      networkProcessorParameters.enableZeroPoseRobotConfigurationPublisherModule(false);
      networkProcessorParameters.enablePerceptionModule(true);
      networkProcessorParameters.setEnableJoystickBasedStepping(false);
      networkProcessorParameters.enableRosModule(false);
      networkProcessorParameters.enableLocalControllerCommunicator(false);
      networkProcessorParameters.enableKinematicsToolbox(false);
      networkProcessorParameters.enableWholeBodyTrajectoryToolbox(false);
      networkProcessorParameters.enableKinematicsPlanningToolbox(false);
      networkProcessorParameters.enableRobotEnvironmentAwerenessModule(false);
      networkProcessorParameters.enableMocapModule(false);

      // start sim
      simulationStarter.startSimulation(networkProcessorParameters, false);

      // spoof and publish planar regions
      ConstantPlanarRegionsPublisher constantPlanarRegionsPublisher = new ConstantPlanarRegionsPublisher(planarRegionsList);
      constantPlanarRegionsPublisher.start(100);
   }
}
