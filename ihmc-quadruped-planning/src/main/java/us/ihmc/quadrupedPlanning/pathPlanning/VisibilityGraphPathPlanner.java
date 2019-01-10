package us.ihmc.quadrupedPlanning.pathPlanning;

import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.statistics.VisibilityGraphStatistics;
import us.ihmc.pathPlanning.visibilityGraphs.NavigableRegionsManager;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.quadrupedPlanning.footstepPlanning.FootstepPlanningResult;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.ArrayList;
import java.util.List;

public class VisibilityGraphPathPlanner extends AbstractWaypointsForFootstepsPlanner
{
   private final NavigableRegionsManager navigableRegionsManager;

   private final VisibilityGraphStatistics visibilityGraphStatistics = new VisibilityGraphStatistics();

   public VisibilityGraphPathPlanner(VisibilityGraphsParameters visibilityGraphsParameters, YoVariableRegistry registry)
   {
      this("", visibilityGraphsParameters, registry);
   }

   public VisibilityGraphPathPlanner(String prefix, VisibilityGraphsParameters visibilityGraphsParameters, YoVariableRegistry registry)
   {
      super(prefix, registry);
      this.navigableRegionsManager = new NavigableRegionsManager(visibilityGraphsParameters);
   }

   public FootstepPlanningResult planWaypoints()
   {
      waypoints.clear();

      if (planarRegionsList == null)
      {
         waypoints.add(new Point3D(bodyStartPose.getPosition()));
         waypoints.add(new Point3D(bodyGoalPose.getPosition()));
      }
      else
      {
         Point3DReadOnly startPos = PlanarRegionTools.projectPointToPlanesVertically(bodyStartPose.getPosition(), planarRegionsList);
         Point3DReadOnly goalPos = PlanarRegionTools.projectPointToPlanesVertically(bodyGoalPose.getPosition(), planarRegionsList);
         navigableRegionsManager.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());

         if (startPos == null)
         {
            LogTools.info("adding plane at start foot");
            startPos = new Point3D(bodyStartPose.getX(), bodyStartPose.getY(), 0.0);
            addPlanarRegionAtZeroHeight(bodyStartPose.getX(), bodyStartPose.getY());
         }
         if (goalPos == null)
         {
            LogTools.info("adding plane at goal pose");
            goalPos = new Point3D(bodyGoalPose.getX(), bodyGoalPose.getY(), 0.0);
            addPlanarRegionAtZeroHeight(bodyGoalPose.getX(), bodyGoalPose.getY());
         }

         if (debug)
         {
            LogTools.info("Starting to plan using " + getClass().getSimpleName());
            LogTools.info("Body start pose: " + startPos);
            LogTools.info("Body goal pose:  " + goalPos);
         }

         try
         {
            List<Point3DReadOnly> path = new ArrayList<>(navigableRegionsManager.calculateBodyPath(startPos, goalPos));

            for (Point3DReadOnly waypoint3d : path)
            {
               waypoints.add(new Point3D(waypoint3d));
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
            yoResult.set(FootstepPlanningResult.PLANNER_FAILED);
            return yoResult.getEnumValue();
         }
      }

      yoResult.set(FootstepPlanningResult.SUB_OPTIMAL_SOLUTION);
      return yoResult.getEnumValue();
   }

   public VisibilityGraphStatistics getPlannerStatistics()
   {
      packVisibilityGraphStatistics(visibilityGraphStatistics);
      return visibilityGraphStatistics;
   }

   public void cancelPlanning()
   {
   }

   public void setTimeout(double timeout)
   {
   }

   // TODO hack to add start and goal planar regions
   private void addPlanarRegionAtZeroHeight(double xLocation, double yLocation)
   {
      ConvexPolygon2D polygon = new ConvexPolygon2D();
      polygon.addVertex(0.3, 0.3);
      polygon.addVertex(-0.3, 0.3);
      polygon.addVertex(0.3, -0.3);
      polygon.addVertex(-0.3, -0.25);
      polygon.update();

      PlanarRegion planarRegion = new PlanarRegion(new RigidBodyTransform(new AxisAngle(), new Vector3D(xLocation, yLocation, 0.0)), polygon);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   private void packVisibilityGraphStatistics(VisibilityGraphStatistics statistics)
   {
      VisibilityMapHolder startMap = navigableRegionsManager.getStartMap();
      VisibilityMapHolder goalMap = navigableRegionsManager.getGoalMap();
      VisibilityMapHolder interRegionsMap = navigableRegionsManager.getInterRegionConnections();
      List<VisibilityMapWithNavigableRegion> visibilityMapWithNavigableRegions = navigableRegionsManager.getNavigableRegionsList();

      statistics.setStartVisibilityMapInWorld(startMap.getMapId(), startMap.getVisibilityMapInWorld());
      statistics.setGoalVisibilityMapInWorld(goalMap.getMapId(), goalMap.getVisibilityMapInWorld());
      statistics.setInterRegionsVisibilityMapInWorld(interRegionsMap.getMapId(), interRegionsMap.getVisibilityMapInWorld());
      statistics.addNavigableRegions(visibilityMapWithNavigableRegions);
   }
}
