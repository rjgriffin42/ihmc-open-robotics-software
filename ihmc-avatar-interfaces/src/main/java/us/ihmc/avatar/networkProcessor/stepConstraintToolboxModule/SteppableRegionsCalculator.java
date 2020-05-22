package us.ihmc.avatar.networkProcessor.stepConstraintToolboxModule;

import us.ihmc.commons.InterpolationTools;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.StepConstraintRegion;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster.ClusterType;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.ExtrusionHull;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.ObstacleExtrusionDistanceCalculator;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.ObstacleRegionFilter;
import us.ihmc.pathPlanning.visibilityGraphs.tools.ClusterTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionTools;
import us.ihmc.robotics.geometry.concavePolygon2D.ConcavePolygon2D;
import us.ihmc.robotics.geometry.concavePolygon2D.ConcavePolygon2DBasics;
import us.ihmc.robotics.geometry.concavePolygon2D.ConcavePolygon2DReadOnly;
import us.ihmc.robotics.geometry.concavePolygon2D.GeometryPolygonTools;
import us.ihmc.robotics.geometry.concavePolygon2D.weilerAtherton.PolygonClippingAndMerging;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SteppableRegionsCalculator
{
   private static final double POPPING_MULTILINE_POINTS_THRESHOLD = MathTools.square(0.10);

   private static final double maxNormalAngleFromVertical = 0.3;
   private static final double minimumAreaToConsider = 0.01;
   private static final double defaultCanDuckUnderHeight = 2.0;
   private static final double defaultCanEasilyStepOverHeight = 0.1;
   private static final double defaultOrthogonalAngle = Math.toRadians(75.0);
   private static final double defaultMinimumDistanceFromCliffBottoms = 0.1;

   private static final Vector3D verticalAxis = new Vector3D(0.0, 0.0, 1.0);

   private final YoDouble maxAngleForSteppable;
   private final YoDouble minimumAreaForSteppable;
   private final YoDouble maximumStepReach;

   private final YoDouble canDuckUnderHeight;
   private final YoDouble canEasilyStepOverHeight;

   private final YoDouble orthogonalAngle;
   private final YoDouble minimumDistanceFromCliffBottoms;

   private List<StepConstraintRegion> steppableRegions = new ArrayList<>();
   private List<PlanarRegion> allPlanarRegions = new ArrayList<>();

   private final FramePoint2D stanceFootPosition = new FramePoint2D();

   /** See notes in {@link VisibilityGraphsparametersReadOnly} */
   private final ObstacleRegionFilter obstacleRegionFilter = new ObstacleRegionFilter()
   {
      @Override
      public boolean isRegionValidObstacle(PlanarRegion potentialObstacleRegion, PlanarRegion navigableRegion)
      {
         if (!PlanarRegionTools.isRegionAOverlappingWithRegionB(potentialObstacleRegion, navigableRegion, minimumDistanceFromCliffBottoms.getDoubleValue()))
            return false;

         if (potentialObstacleRegion.getBoundingBox3dInWorld().getMinZ()
             > navigableRegion.getBoundingBox3dInWorld().getMaxZ() + canDuckUnderHeight.getDoubleValue())
            return false;

         return PlanarRegionTools.isPlanarRegionAAbovePlanarRegionB(potentialObstacleRegion, navigableRegion, canEasilyStepOverHeight.getDoubleValue());
      }
   };

   private final ObstacleExtrusionDistanceCalculator obstacleExtrusionDistanceCalculator = new ObstacleExtrusionDistanceCalculator()
   {
      @Override
      public double computeExtrusionDistance(Point2DReadOnly pointToExtrude, double obstacleHeight)
      {
         if (obstacleHeight < 0.0)
         {
            return 0.0;
         }
         else if (obstacleHeight < canEasilyStepOverHeight.getDoubleValue())
         {
            double alpha = obstacleHeight / canEasilyStepOverHeight.getDoubleValue();
            return InterpolationTools.linearInterpolate(0.0, minimumDistanceFromCliffBottoms.getDoubleValue(), alpha);
         }
         else
         {
            return minimumDistanceFromCliffBottoms.getDoubleValue();
         }
      }
   };

   public SteppableRegionsCalculator(double maximumReach, YoVariableRegistry registry)
   {
      maxAngleForSteppable = new YoDouble("maxAngleForSteppable", registry);
      minimumAreaForSteppable = new YoDouble("minimumAreaForSteppable", registry);
      maximumStepReach = new YoDouble("maximumStepReach", registry);
      canDuckUnderHeight = new YoDouble("canDuckUnderHeight", registry);
      canEasilyStepOverHeight = new YoDouble("canEasyStepOverHeight", registry);
      orthogonalAngle = new YoDouble("orthogonalAngle", registry);
      minimumDistanceFromCliffBottoms = new YoDouble("tooHighToStepDistance", registry);

      maxAngleForSteppable.set(maxNormalAngleFromVertical);
      minimumAreaForSteppable.set(minimumAreaToConsider);
      maximumStepReach.set(maximumReach);
      canDuckUnderHeight.set(defaultCanDuckUnderHeight);
      canEasilyStepOverHeight.set(defaultCanEasilyStepOverHeight);
      orthogonalAngle.set(defaultOrthogonalAngle);
      minimumDistanceFromCliffBottoms.set(defaultMinimumDistanceFromCliffBottoms);
   }

   public void setPlanarRegions(List<PlanarRegion> planarRegions)
   {
      allPlanarRegions = planarRegions;
   }

   public void setStanceFootPosition(FramePoint3DReadOnly stanceFootPosition)
   {
      this.stanceFootPosition.set(stanceFootPosition);
   }

   public void setCanEasilyStepOverHeight(double canEasilyStepOverHeight)
   {
      this.canEasilyStepOverHeight.set(canEasilyStepOverHeight);
   }

   public void setMinimumDistanceFromCliffBottoms(double minimumDistanceFromCliffBottoms)
   {
      this.minimumDistanceFromCliffBottoms.set(minimumDistanceFromCliffBottoms);
   }

   public void setOrthogonalAngle(double orthogonalAngle)
   {
      this.orthogonalAngle.set(orthogonalAngle);
   }

   public List<StepConstraintRegion> computeSteppableRegions()
   {
      List<PlanarRegion> candidateRegions = allPlanarRegions.stream().filter(this::isRegionValidForStepping).collect(Collectors.toList());

      steppableRegions = candidateRegions.stream()
                                         .map(region -> createSteppableRegionFromPlanarRegion(region, allPlanarRegions))
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toList());
      return steppableRegions;
   }

   private boolean isRegionValidForStepping(PlanarRegion planarRegion)
   {
      double angle = planarRegion.getNormal().angle(verticalAxis);

      if (angle > maxAngleForSteppable.getValue())
         return false;

      if (PlanarRegionTools.computePlanarRegionArea(planarRegion) < minimumAreaForSteppable.getValue())
         return false;

      return isRegionWithinReach(stanceFootPosition, maximumStepReach.getDoubleValue(), planarRegion);
   }

   private static boolean isRegionWithinReach(Point2DReadOnly point, double reach, PlanarRegion planarRegion)
   {
      // TODO do a check on the bounding box distance first

      if (planarRegion.getConvexHull().distance(point) > reach)
         return false;

      boolean closeEnough = false;
      for (ConvexPolygon2DReadOnly convexPolygon : planarRegion.getConvexPolygons())
      {
         if (convexPolygon.distance(point) < reach)
         {
            closeEnough = true;
            break;
         }
      }
      return closeEnough;
   }

   private StepConstraintRegion createSteppableRegionFromPlanarRegion(PlanarRegion candidateRegion, List<PlanarRegion> allOtherRegions)
   {
      ConcavePolygon2D candidateConstraintRegion = new ConcavePolygon2D();
      candidateConstraintRegion.addVertices(Vertex2DSupplier.asVertex2DSupplier(candidateRegion.getConcaveHull()));
      candidateConstraintRegion.update();

      List<PlanarRegion> obstacleRegions = allOtherRegions.stream()
                                                          .filter(candidate -> obstacleRegionFilter.isRegionValidObstacle(candidate, candidateRegion))
                                                          .collect(Collectors.toList());

      double zThresholdBeforeOrthogonal = Math.cos(orthogonalAngle.getDoubleValue());
      List<ConcavePolygon2D> obstacleExtrusions = obstacleRegions.stream()
                                                                 .map(region -> createObstacleExtrusion(candidateRegion,
                                                                                                        region,
                                                                                                        obstacleExtrusionDistanceCalculator,
                                                                                                        zThresholdBeforeOrthogonal))
                                                                 .collect(Collectors.toList());
      mergeAllExtrusions(obstacleExtrusions);

      if (obstacleExtrusions.stream().anyMatch(region -> isRegionMasked(candidateConstraintRegion, region)))
         return null;

      List<ConcavePolygon2D> listOfHoles = obstacleExtrusions.stream()
                                                             .filter(region -> isObstacleAHole(candidateConstraintRegion, region))
                                                             .collect(Collectors.toList());
      obstacleExtrusions.removeAll(listOfHoles);

      obstacleExtrusions.forEach(region -> removeObstacleFromSteppableArea(candidateConstraintRegion, region));

      return new StepConstraintRegion(candidateRegion.getTransformToWorld(), candidateConstraintRegion, listOfHoles);
   }

   private boolean isObstacleAHole(ConcavePolygon2DBasics constraintArea, ConcavePolygon2DReadOnly obstacleConcaveHull)
   {
      return GeometryPolygonTools.isPolygonInsideOtherPolygon(obstacleConcaveHull, constraintArea);
   }

   private boolean isRegionMasked(ConcavePolygon2DBasics region, ConcavePolygon2DReadOnly candidateMask)
   {
      return GeometryPolygonTools.isPolygonInsideOtherPolygon(region, candidateMask);
   }

   private boolean removeObstacleFromSteppableArea(ConcavePolygon2DBasics constraintArea, ConcavePolygon2DReadOnly obstacleConcaveHull)
   {

      ConcavePolygon2D clippedArea = new ConcavePolygon2D();
      PolygonClippingAndMerging.removeAreaInsideClip(obstacleConcaveHull, constraintArea, clippedArea);

      constraintArea.set(clippedArea);

      return false;
   }

   private static void mergeAllExtrusions(List<ConcavePolygon2D> extrusions)
   {
      int i = 0;
      while (i < extrusions.size())
      {
         ConcavePolygon2D polygonA = extrusions.get(i);
         int j = i + 1;
         boolean shouldRemoveA = false;
         while (j < extrusions.size())
         {
            ConcavePolygon2D polygonB = extrusions.get(j);
            if (GeometryPolygonTools.doPolygonsIntersect(polygonA, polygonB))
            {
               ConcavePolygon2D newPolygon = new ConcavePolygon2D();
               PolygonClippingAndMerging.merge(polygonA, polygonB, newPolygon);

               extrusions.set(i, newPolygon);
               extrusions.remove(j);
            }
            else if (GeometryPolygonTools.isPolygonInsideOtherPolygon(polygonB, polygonA))
            {
               extrusions.remove(j);
            }
            else if (GeometryPolygonTools.isPolygonInsideOtherPolygon(polygonA, polygonB))
            {
               shouldRemoveA = true;
               break;
            }
            else
            {
               j++;
            }
         }

         if (shouldRemoveA)
            extrusions.remove(i);
         else
            i++;
      }
   }

   static ConcavePolygon2D createObstacleExtrusion(PlanarRegion homeRegion,
                                                   PlanarRegion obstacleRegion,
                                                   ObstacleExtrusionDistanceCalculator extrusionDistanceCalculator,
                                                   double zThresholdBeforeOrthogonal)
   {
      List<Point2D> concaveHull = obstacleRegion.getConcaveHull();

      RigidBodyTransformReadOnly transformFromObstacleToWorld = obstacleRegion.getTransformToWorld();

      // Transform the obstacle to world and also Project the obstacle to z = 0:
      List<Point3DReadOnly> obstacleClustersInWorld = new ArrayList<>();
      ClusterTools.calculatePointsInWorldAtRegionHeight(concaveHull, transformFromObstacleToWorld, homeRegion, null, obstacleClustersInWorld);

      Vector3DReadOnly obstacleNormal = obstacleRegion.getNormal();
      boolean isObstacleWall = Math.abs(obstacleNormal.getZ()) < zThresholdBeforeOrthogonal;

      ClusterType obstacleClusterType = isObstacleWall ? ClusterType.MULTI_LINE : ClusterType.POLYGON;
      if (isObstacleWall)
         obstacleClustersInWorld = ClusterTools.filterVerticalPolygonForMultiLineExtrusion(obstacleClustersInWorld, POPPING_MULTILINE_POINTS_THRESHOLD);

      // actually extrude the points
      List<? extends Point2DReadOnly> extrusionInFlatWorld = ClusterTools.computeObstacleNavigableExtrusionsInLocal(obstacleClusterType,
                                                                                                                       obstacleClustersInWorld,
                                                                                                                       extrusionDistanceCalculator);


      // Project the points back up to the home region.
      RigidBodyTransformReadOnly transformFromWorldToHome = homeRegion.getTransformToLocal();
      ExtrusionHull nonNavigableExtrusionsInHomeRegionLocal = ClusterTools.projectPointsVerticallyToPlanarRegionLocal(homeRegion,
                                                                                                                      extrusionInFlatWorld,
                                                                                                                      transformFromWorldToHome);

      return new ConcavePolygon2D(Vertex2DSupplier.asVertex2DSupplier(nonNavigableExtrusionsInHomeRegionLocal.getPoints()));
   }
}
