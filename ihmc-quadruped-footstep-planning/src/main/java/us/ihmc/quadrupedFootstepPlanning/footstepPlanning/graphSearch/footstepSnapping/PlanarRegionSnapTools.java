package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping;

import boofcv.struct.image.Planar;
import gnu.trove.list.array.TIntArrayList;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.robotics.geometry.ConvexPolygonScaler;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.yoVariables.providers.BooleanProvider;
import us.ihmc.yoVariables.providers.DoubleProvider;

import java.util.List;

public class PlanarRegionSnapTools
{
   private final ConvexPolygonScaler polygonScaler = new ConvexPolygonScaler();
   private final ConvexPolygon2D scaledRegionPolygon = new ConvexPolygon2D();
   private final ConvexPolygon2D tempPolygon = new ConvexPolygon2D();

   private final DoubleProvider projectionInsideDelta;
   private final BooleanProvider projectInsideUsingConvexHull;
   private final boolean enforceTranslationLessThanGridCell;

   public PlanarRegionSnapTools(DoubleProvider projectionInsideDelta, BooleanProvider projectInsideUsingConvexHull, boolean enforceTranslationLessThanGridCell)
   {
      this.projectionInsideDelta = projectionInsideDelta;
      this.projectInsideUsingConvexHull = projectInsideUsingConvexHull;
      this.enforceTranslationLessThanGridCell = enforceTranslationLessThanGridCell;
   }

   public PlanarRegion findHighestRegion(Point2DReadOnly point, Vector2D projectionTranslationToPack, List<PlanarRegion> planarRegions)
   {
      return findHighestRegion(point.getX(), point.getY(), projectionTranslationToPack, planarRegions);
   }

   public PlanarRegion findHighestRegion(double x, double y, Vector2D projectionTranslationToPack, List<PlanarRegion> planarRegions)
   {
      tempPolygon.clearAndUpdate();
      tempPolygon.addVertex(0.5 * FootstepNode.gridSizeXY, 0.5 * FootstepNode.gridSizeXY);
      tempPolygon.addVertex(0.5 * FootstepNode.gridSizeXY, - 0.5 * FootstepNode.gridSizeXY);
      tempPolygon.addVertex(- 0.5 * FootstepNode.gridSizeXY, 0.5 * FootstepNode.gridSizeXY);
      tempPolygon.addVertex(- 0.5 * FootstepNode.gridSizeXY, - 0.5 * FootstepNode.gridSizeXY);
      tempPolygon.update();
      tempPolygon.translate(x, y);

      List<PlanarRegion> intersectingRegions = PlanarRegionTools.findPlanarRegionsIntersectingPolygon(tempPolygon, planarRegions);
      if (intersectingRegions == null || intersectingRegions.isEmpty())
      {
         return null;
      }

      double highestPoint = Double.NEGATIVE_INFINITY;
      PlanarRegion highestPlanarRegion = null;

      for (int i = 0; i < intersectingRegions.size(); i++)
      {
         PlanarRegion planarRegion = intersectingRegions.get(i);
         Vector3D projectionTranslation = projectPointIntoRegion(planarRegion, x, y);
         double height;

         if(projectionTranslation.containsNaN())
         {
            // even if projection fails, remember highest region. this will be considered an obstacle
            height = planarRegion.getPlaneZGivenXY(x, y);
         }
         else
         {
            height = planarRegion.getPlaneZGivenXY(x + projectionTranslation.getX(), y + projectionTranslation.getY());
         }

         if (height > highestPoint)
         {
            highestPoint = height;
            highestPlanarRegion = planarRegion;
            projectionTranslationToPack.set(projectionTranslation);
         }
      }

      return highestPlanarRegion;
   }

   private Vector3D projectPointIntoRegion(PlanarRegion region, double x, double y)
   {
      Vector3D projectionTranslation = new Vector3D();
      Point3D pointToSnap = new Point3D();

      pointToSnap.set(x, y, region.getPlaneZGivenXY(x, y));
      region.transformFromWorldToLocal(pointToSnap);
      Point2D projectedPoint = new Point2D(pointToSnap);

      boolean successfulScale;
      if (projectInsideUsingConvexHull.getValue())
         successfulScale = polygonScaler.scaleConvexPolygon(region.getConvexHull(), projectionInsideDelta.getValue(), scaledRegionPolygon);
      else
         successfulScale = computeScaledPolygonRegionForConstraint(projectedPoint, region, projectionInsideDelta.getValue());

      // region is too small to wiggle inside
      if(!successfulScale)
      {
         projectionTranslation.setToNaN();
         return projectionTranslation;
      }



      double signedDistanceToPolygon = scaledRegionPolygon.signedDistance(projectedPoint);
      if(signedDistanceToPolygon <= 0.0)
      {
         // return, no need to project
         projectionTranslation.setToZero();
         return projectionTranslation;
      }

      if (enforceTranslationLessThanGridCell && signedDistanceToPolygon >= 0.5 * FootstepNode.gridSizeXY)
      {
         // Projection distance is too big. Must be smaller than half of the grid size
         projectionTranslation.setToNaN();
         return projectionTranslation;
      }

      boolean successfulProjection = scaledRegionPolygon.orthogonalProjection(projectedPoint);
      if(!successfulProjection)
      {
         projectionTranslation.setToNaN();
         return projectionTranslation;
      }

      projectionTranslation.set(projectedPoint.getX(), projectedPoint.getY(), 0.0);
      projectionTranslation.sub(pointToSnap.getX(), pointToSnap.getY(), 0.0);
      region.transformFromLocalToWorld(projectionTranslation);
      projectionTranslation.setZ(0.0);

      return projectionTranslation;
   }

   private boolean computeScaledPolygonRegionForConstraint(Point2DReadOnly pointToCheck, PlanarRegion region, double projectionDistance)
   {
      List<ConvexPolygon2D> allRegions = region.getConvexPolygons();
      ConvexPolygon2DReadOnly containingRegion = getContainingRegion(pointToCheck, allRegions);
      TIntArrayList indicesToIgnore = new TIntArrayList();

      for (int index = 0; index < containingRegion.getNumberOfVertices(); index++)
      {
         Point2DReadOnly vertex = containingRegion.getVertex(index);
         Point2DReadOnly nextVertex = containingRegion.getNextVertex(index);
         if (isPointInOtherRegion(vertex, containingRegion, allRegions) && isPointInOtherRegion(nextVertex, containingRegion, allRegions))
            indicesToIgnore.add(index);
      }

      return polygonScaler.scaleConvexPolygon(region.getConvexHull(), projectionDistance, scaledRegionPolygon, indicesToIgnore.toArray());
   }

   private ConvexPolygon2DReadOnly getContainingRegion(Point2DReadOnly pointToCheck, List<ConvexPolygon2D> convexPolygons)
   {
      for (ConvexPolygon2D convexPolygon : convexPolygons)
      {
         if (convexPolygon.isPointInside(pointToCheck))
            return convexPolygon;
      }

      return null;
   }

   private boolean isPointInOtherRegion(Point2DReadOnly point, ConvexPolygon2DReadOnly regionToIgnore, List<ConvexPolygon2D> allRegions)
   {
      for (ConvexPolygon2D convexPolygon : allRegions)
      {
         if (regionToIgnore.equals(convexPolygon))
            continue;

         if (convexPolygon.isPointInside(point))
            return true;

         for (Point2DReadOnly vertex : convexPolygon.getVertexBufferView())
         {
            if (vertex.epsilonEquals(point, 1e-8))
               return true;
         }
      }

      return false;
   }

   public static RigidBodyTransform getSnapTransformToRegion(Point2DReadOnly pointToSnap, PlanarRegion planarRegionToSnapTo)
   {
      Point3D point = new Point3D(pointToSnap);
      point.setZ(planarRegionToSnapTo.getPlaneZGivenXY(pointToSnap.getX(), pointToSnap.getY()));

      Vector3D surfaceNormal = new Vector3D();
      planarRegionToSnapTo.getNormal(surfaceNormal);

      RigidBodyTransform snapTransform = PlanarRegionSnapTools.createTransformToMatchSurfaceNormalPreserveX(surfaceNormal);
      PlanarRegionSnapTools.setTranslationSettingZAndPreservingXAndY(point, snapTransform);

      return snapTransform;
   }

   public static RigidBodyTransform createTransformToMatchSurfaceNormalPreserveX(Vector3D surfaceNormal)
   {
      Vector3D xAxis = new Vector3D();
      Vector3D yAxis = new Vector3D(0.0, 1.0, 0.0);

      xAxis.cross(yAxis, surfaceNormal);
      xAxis.normalize();
      yAxis.cross(surfaceNormal, xAxis);

      RotationMatrix rotationMatrix = new RotationMatrix();
      rotationMatrix.setColumns(xAxis, yAxis, surfaceNormal);
      RigidBodyTransform transformToReturn = new RigidBodyTransform();
      transformToReturn.setRotation(rotationMatrix);
      return transformToReturn;
   }

   public static void setTranslationSettingZAndPreservingXAndY(Point3DReadOnly point, RigidBodyTransform transformToReturn)
   {
      setTranslationSettingZAndPreservingXAndY(point.getX(), point.getY(), point.getX(), point.getY(), point.getZ(), transformToReturn);
   }

   public static void setTranslationSettingZAndPreservingXAndY(double x, double y, double xTranslated, double yTranslated, double z, RigidBodyTransform transformToReturn)
   {
      Vector3D newTranslation = new Vector3D(x, y, 0.0);
      transformToReturn.transform(newTranslation);
      newTranslation.scale(-1.0);
      newTranslation.add(xTranslated, yTranslated, z);

      transformToReturn.setTranslation(newTranslation);
   }
}
