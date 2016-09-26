package us.ihmc.geometry.polytope;

import java.util.PriorityQueue;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import gnu.trove.map.hash.THashMap;
import us.ihmc.robotics.lists.RecyclingArrayList;

public class ExpandingPolytopeAlgorithm
{
   private final PriorityQueue<ExpandingPolytopeEntry> triangleEntryQueue = new PriorityQueue<ExpandingPolytopeEntry>();
   private final ExpandingPolytopeEdgeList edgeList = new ExpandingPolytopeEdgeList();

   private final THashMap<Point3d, Point3d> correspondingPointsOnA = new THashMap<>();
   private final THashMap<Point3d, Point3d> correspondingPointsOnB = new THashMap<>();

   private ConvexPolytope polytopeA;
   private ConvexPolytope polytopeB;

   private final double epsilonRelative;

   private ExpandingPolytopeAlgorithmListener listener;

   private final RecyclingArrayList<ExpandingPolytopeEntry> polytopeEntryPool = new RecyclingArrayList<>(ExpandingPolytopeEntry.class);

   public ExpandingPolytopeAlgorithm(double epsilonRelative)
   {
      this.epsilonRelative = epsilonRelative;
   }

   public void setExpandingPolytopeAlgorithmListener(ExpandingPolytopeAlgorithmListener listener)
   {
      this.listener = listener;
   }

   public void setPolytopes(SimplexPolytope simplex, ConvexPolytope polytopeOne, ConvexPolytope polytopeTwo)
   {
      polytopeEntryPool.clear();

      correspondingPointsOnA.clear();
      correspondingPointsOnB.clear();
      edgeList.clear();
      triangleEntryQueue.clear();

      this.polytopeA = polytopeOne;
      this.polytopeB = polytopeTwo;

      int numberOfPoints = simplex.getNumberOfPoints();
      if (numberOfPoints != 4)
         throw new RuntimeException("Implement for non tetrahedral simplex");

      Point3d pointOne = simplex.getPoint(0);
      Point3d pointTwo = simplex.getPoint(1);
      Point3d pointThree = simplex.getPoint(2);
      Point3d pointFour = simplex.getPoint(3);

      correspondingPointsOnA.put(pointOne, simplex.getCorrespondingPointOnPolytopeA(pointOne));
      correspondingPointsOnA.put(pointTwo, simplex.getCorrespondingPointOnPolytopeA(pointTwo));
      correspondingPointsOnA.put(pointThree, simplex.getCorrespondingPointOnPolytopeA(pointThree));
      correspondingPointsOnA.put(pointFour, simplex.getCorrespondingPointOnPolytopeA(pointFour));

      correspondingPointsOnB.put(pointOne, simplex.getCorrespondingPointOnPolytopeB(pointOne));
      correspondingPointsOnB.put(pointTwo, simplex.getCorrespondingPointOnPolytopeB(pointTwo));
      correspondingPointsOnB.put(pointThree, simplex.getCorrespondingPointOnPolytopeB(pointThree));
      correspondingPointsOnB.put(pointFour, simplex.getCorrespondingPointOnPolytopeB(pointFour));

      ExpandingPolytopeEntry entry123 = polytopeEntryPool.add();
      ExpandingPolytopeEntry entry324 = polytopeEntryPool.add();
      ExpandingPolytopeEntry entry421 = polytopeEntryPool.add();
      ExpandingPolytopeEntry entry134 = polytopeEntryPool.add();

      entry123.reset(pointOne, pointTwo, pointThree);
      entry324.reset(pointThree, pointTwo, pointFour);
      entry421.reset(pointFour, pointTwo, pointOne);
      entry134.reset(pointOne, pointThree, pointFour);

      entry123.setAdjacentTriangle(1, entry324, 0);
      entry324.setAdjacentTriangle(0, entry123, 1);

      entry123.setAdjacentTriangle(0, entry421, 1);
      entry421.setAdjacentTriangle(1, entry123, 0);

      entry123.setAdjacentTriangle(2, entry134, 0);
      entry134.setAdjacentTriangle(0, entry123, 2);

      entry324.setAdjacentTriangle(1, entry421, 0);
      entry421.setAdjacentTriangle(0, entry324, 1);

      entry324.setAdjacentTriangle(2, entry134, 1);
      entry134.setAdjacentTriangle(1, entry324, 2);

      entry421.setAdjacentTriangle(2, entry134, 2);
      entry134.setAdjacentTriangle(2, entry421, 2);

      if (entry123.closestIsInternal())
         triangleEntryQueue.add(entry123);
      if (entry324.closestIsInternal())
         triangleEntryQueue.add(entry324);
      if (entry421.closestIsInternal())
         triangleEntryQueue.add(entry421);
      if (entry134.closestIsInternal())
         triangleEntryQueue.add(entry134);

      if (listener != null)
      {
         listener.setPolytopes(simplex, polytopeOne, polytopeTwo, entry123);
      }
   }

   private final Vector3d supportDirection = new Vector3d();

   public void computeExpandedPolytope(Vector3d separatingVectorToPack, Point3d closestPointOnA, Point3d closestPointOnB)
   {
      double mu = Double.POSITIVE_INFINITY; // Upper bound for the square penetration depth.
      Vector3d closestPointToOrigin = null;
      ExpandingPolytopeEntry closestTriangleToOrigin = null;

      int numberOfIterations = 0;
      while (true)
      {
         //TODO: Stop the looping!
         numberOfIterations++;

         ExpandingPolytopeEntry triangleEntryToExpand = triangleEntryQueue.poll();
         if (listener != null)
            listener.polledEntryToExpand(triangleEntryToExpand);

         boolean closeEnough = false;

         if (!triangleEntryToExpand.isObsolete())
         {
            closestPointToOrigin = triangleEntryToExpand.getClosestPointToOrigin();
            closestTriangleToOrigin = triangleEntryToExpand;

            supportDirection.set(closestPointToOrigin);

            PolytopeVertex supportingVertexA = polytopeA.getSupportingVertex(supportDirection);
            supportDirection.negate();
            PolytopeVertex supportingVertexB = polytopeB.getSupportingVertex(supportDirection);

            Vector3d w = new Vector3d();
            w.sub(supportingVertexA.getPosition(), supportingVertexB.getPosition());

            if (listener != null)
            {
               listener.computedSupportingVertices(supportingVertexA, supportingVertexB, w);
            }

            double vDotW = closestPointToOrigin.dot(w);
            double lengthSquared = closestPointToOrigin.lengthSquared();
            mu = Math.min(mu, vDotW * vDotW / lengthSquared);
            closeEnough = (mu <= (1.0 + epsilonRelative) * (1.0 + epsilonRelative) * lengthSquared);

            if (listener != null)
            {
               listener.computedCloseEnough(vDotW, lengthSquared, mu, closeEnough);
            }

            if (!closeEnough)
            {
               // Blow up the current polytope by adding vertex w.
               ExpandingPolytopeSilhouetteConstructor.computeSilhouetteFromW(triangleEntryToExpand, w, edgeList);

               if (listener != null)
               {
                  listener.computedSilhouetteFromW(edgeList);
               }

               // edgeList now is the entire silhouette of the current polytope as seen from w.

               ExpandingPolytopeEntry firstNewEntry = null;
               Point3d wPoint = new Point3d(w);
               correspondingPointsOnA.put(wPoint, supportingVertexA.getPosition());
               correspondingPointsOnB.put(wPoint, supportingVertexB.getPosition());

               int numberOfEdges = edgeList.getNumberOfEdges();

               //TODO: Recycle the trash here...
               THashMap<Point3d, ExpandingPolytopeEntry[]> mapFromStitchVertexToTriangles = new THashMap<>();

               for (int edgeIndex = 0; edgeIndex < numberOfEdges; edgeIndex++)
               {
                  ExpandingPolytopeEdge edge = edgeList.getEdge(edgeIndex);

                  ExpandingPolytopeEntry sentry = edge.getEntry();
                  int sentryEdgeIndex = edge.getEdgeIndex();
                  int nextIndex = (sentryEdgeIndex + 1) % 3;

                  Point3d sentryVertexOne = sentry.getVertex(sentryEdgeIndex);
                  Point3d sentryVertexTwo = sentry.getVertex(nextIndex);

//                  ExpandingPolytopeEntry newEntry = polytopeEntryPool.add();
//                  newEntry.reset(sentryVertexTwo, sentryVertexOne, wPoint);
                  
                  ExpandingPolytopeEntry newEntry = polytopeEntryPool.add();
                  newEntry.reset(sentryVertexTwo, sentryVertexOne, wPoint);
//                  ExpandingPolytopeEntry newEntry = new ExpandingPolytopeEntry(sentryVertexTwo, sentryVertexOne, wPoint);

                  if (newEntry.isAffinelyDependent())
                  {
                     computeClosestPointsOnAAndB(closestTriangleToOrigin, closestPointOnA, closestPointOnB);
                     if (listener != null)
                     {
                        listener.foundMinimumPenetrationVector(closestPointToOrigin, closestPointOnA, closestPointOnB);
                     }
                     separatingVectorToPack.set(closestPointToOrigin);
                     return;
                  }

                  ExpandingPolytopeEntry[] twoTriangles = getOrCreateTwoTriangleArray(mapFromStitchVertexToTriangles, sentryVertexOne);
                  storeNewEntry(newEntry, twoTriangles);
                  twoTriangles = getOrCreateTwoTriangleArray(mapFromStitchVertexToTriangles, sentryVertexTwo);
                  storeNewEntry(newEntry, twoTriangles);

                  newEntry.setAdjacentTriangle(0, sentry, sentryEdgeIndex);
                  sentry.setAdjacentTriangle(sentryEdgeIndex, newEntry, 0);

                  if (edgeIndex == 0)
                     firstNewEntry = newEntry;

                  if (listener != null)
                     listener.createdNewEntry(newEntry);

                  double newEntryClosestDistanceSquared = newEntry.getClosestPointToOrigin().lengthSquared();
                  if ((newEntry.closestIsInternal()) && (closestPointToOrigin.lengthSquared() <= newEntryClosestDistanceSquared)
                        && (newEntryClosestDistanceSquared <= mu))
                  {
                     triangleEntryQueue.add(newEntry);

                     if (listener != null)
                        listener.addedNewEntryToQueue(newEntry);
                  }
               }

               // Stich em up:
               Set<Point3d> keySet = mapFromStitchVertexToTriangles.keySet();

               for (Point3d stitchVertex : keySet)
               {
                  ExpandingPolytopeEntry[] trianglesToStitch = mapFromStitchVertexToTriangles.get(stitchVertex);
                  if ((trianglesToStitch[0] == null) || (trianglesToStitch[1] == null))
                  {
                     throw new RuntimeException("Stitch triangle is null");
                  }

                  ExpandingPolytopeEntry triangleOne = trianglesToStitch[0];
                  ExpandingPolytopeEntry triangleTwo = trianglesToStitch[1];

                  boolean stitchedTriangles = triangleOne.setAdjacentTriangleIfPossible(triangleTwo);
                  if (!stitchedTriangles)
                  {
                     throw new RuntimeException("Failed to stitch triangles!!");
                  }
               }

               if (listener != null)
               {
                  listener.expandedPolytope(firstNewEntry);
               }
            }
         }

         if ((numberOfIterations > 1000) || (closeEnough) || (triangleEntryQueue.isEmpty())
               || (triangleEntryQueue.peek().getClosestPointToOrigin().lengthSquared() > mu))
         {
            computeClosestPointsOnAAndB(closestTriangleToOrigin, closestPointOnA, closestPointOnB);

            if (listener != null)
            {
               listener.foundMinimumPenetrationVector(closestPointToOrigin, closestPointOnA, closestPointOnB);
            }
            separatingVectorToPack.set(closestPointToOrigin);
            return;
         }
      }
   }

   private void storeNewEntry(ExpandingPolytopeEntry newEntry, ExpandingPolytopeEntry[] twoTriangles)
   {
      if (twoTriangles[0] == null)
         twoTriangles[0] = newEntry;
      else if (twoTriangles[1] != null)
      {
         throw new RuntimeException("twoTriangles[1] != null");
      }
      else
      {
         twoTriangles[1] = newEntry;
      }
   }

   private ExpandingPolytopeEntry[] getOrCreateTwoTriangleArray(THashMap<Point3d, ExpandingPolytopeEntry[]> mapFromStitchVertexToTriangles,
         Point3d sentryVertexOne)
   {
      ExpandingPolytopeEntry[] twoTriangleArray = mapFromStitchVertexToTriangles.get(sentryVertexOne);
      if (twoTriangleArray == null)
      {
         twoTriangleArray = new ExpandingPolytopeEntry[2];
         mapFromStitchVertexToTriangles.put(sentryVertexOne, twoTriangleArray);
      }

      return twoTriangleArray;
   }

   private final Point3d tempPoint = new Point3d();

   private void computeClosestPointsOnAAndB(ExpandingPolytopeEntry closestTriangleToOrigin, Point3d closestPointOnA, Point3d closestPointOnB)
   {
      closestPointOnA.set(0.0, 0.0, 0.0);
      closestPointOnB.set(0.0, 0.0, 0.0);

      for (int i = 0; i < 3; i++)
      {
         Point3d vertex = closestTriangleToOrigin.getVertex(i);
         double lambda = closestTriangleToOrigin.getLambda(i);

         Point3d pointOnA = correspondingPointsOnA.get(vertex);
         Point3d pointOnB = correspondingPointsOnB.get(vertex);

         tempPoint.set(pointOnA);
         tempPoint.scale(lambda);
         closestPointOnA.add(tempPoint);

         tempPoint.set(pointOnB);
         tempPoint.scale(lambda);
         closestPointOnB.add(tempPoint);
      }

   }

}
