package us.ihmc.humanoidBehaviors.behaviors.roughTerrain;

import us.ihmc.footstepPlanning.graphSearch.PlanarRegionBipedalFootstepPlannerVisualizer;
import us.ihmc.graphics3DDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotDataLogger.YoVariableServer;
import us.ihmc.robotDataLogger.logger.LogSettings;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.util.PeriodicNonRealtimeThreadScheduler;
import us.ihmc.util.PeriodicThreadScheduler;

public class YoVariableServerPlanarRegionBipedalFootstepPlannerVisualizer
{   
   public static PlanarRegionBipedalFootstepPlannerVisualizer createWithYoVariableServer(double dtForViz, FullRobotModel fullRobotModel,
                                                                                         LogModelProvider logModelProvider,
                                                                                         SideDependentList<ConvexPolygon2d> footPolygonsInSoleFrame)
   {
      YoVariableRegistry registry = new YoVariableRegistry(YoVariableServerPlanarRegionBipedalFootstepPlannerVisualizer.class.getSimpleName());
      YoGraphicsListRegistry graphicsListRegistry = new YoGraphicsListRegistry();

      PlanarRegionBipedalFootstepPlannerVisualizer footstepPlannerVisualizer = new PlanarRegionBipedalFootstepPlannerVisualizer(footPolygonsInSoleFrame, registry, graphicsListRegistry);
      
      
      PeriodicThreadScheduler scheduler = new PeriodicNonRealtimeThreadScheduler("PlannerScheduler");
      YoVariableServer yoVariableServer = new YoVariableServer(YoVariableServerPlanarRegionBipedalFootstepPlannerVisualizer.class, scheduler, logModelProvider, LogSettings.FOOTSTEP_PLANNER,
                                                               dtForViz);
      footstepPlannerVisualizer.setTickAndUpdatable(yoVariableServer);

      yoVariableServer.setMainRegistry(registry, fullRobotModel, graphicsListRegistry);
      yoVariableServer.start();
            
      return footstepPlannerVisualizer;
   }

   public static PlanarRegionBipedalFootstepPlannerVisualizer createWithSimulationConstructionSet(double dtForViz, SideDependentList<ConvexPolygon2d> footPolygonsInSoleFrame)
   {
      YoVariableRegistry registry = new YoVariableRegistry(YoVariableServerPlanarRegionBipedalFootstepPlannerVisualizer.class.getSimpleName());
      YoGraphicsListRegistry graphicsListRegistry = new YoGraphicsListRegistry();

      PlanarRegionBipedalFootstepPlannerVisualizer footstepPlannerVisualizer = new PlanarRegionBipedalFootstepPlannerVisualizer(footPolygonsInSoleFrame, registry, graphicsListRegistry);
      SimulationConstructionSet scs = new SimulationConstructionSet(new Robot("Test"));
      
      footstepPlannerVisualizer.setTickAndUpdatable(scs);

      scs.changeBufferSize(32000);

      scs.addYoVariableRegistry(registry);
      scs.addYoGraphicsListRegistry(graphicsListRegistry);

      scs.setDT(dtForViz, 1);

      scs.setCameraFix(-6.0, 0.0, 0.0);
      scs.setCameraPosition(-11.0, 0.0, 8.0);
      scs.setGroundVisible(false);
      scs.startOnAThread();

      return footstepPlannerVisualizer;
   }

}
