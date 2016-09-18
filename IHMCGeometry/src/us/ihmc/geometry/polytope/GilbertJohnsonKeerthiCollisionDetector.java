package us.ihmc.geometry.polytope;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * GilbertJohnsonKeerthi (GJK) algorithm for doing collision detection
 *
 * For more information see book, papers, and presentations at http://realtimecollisiondetection.net/pubs/
 */
public class GilbertJohnsonKeerthiCollisionDetector
{
   private static final double LAMBDA_STOPPING_DELTA = 1e-9;

   private final Vector3d negativeSupportDirection = new Vector3d();

   private final SimplexPolytope simplex = new SimplexPolytope();
   private final Vector3d tempVertex = new Vector3d();

   private GilbertJohnsonKeerthiCollisionDetectorListener listener;
   
   public void computeSupportPointOnMinkowskiDifference(ConvexPolytope cubeOne, ConvexPolytope cubeTwo, Vector3d supportDirection, Point3d supportPoint)
   {
      // Because everything is linear and convex, the support point on the Minkowski difference is s_{a minkowskidiff b}(d) = s_a(d) - s_b(-d)

      PolytopeVertex supportingVertexOne = cubeOne.getSupportingVertex(supportDirection);

      negativeSupportDirection.set(supportDirection);
      negativeSupportDirection.scale(-1.0);

      PolytopeVertex supportingVertexTwo = cubeTwo.getSupportingVertex(negativeSupportDirection);

      supportPoint.set(supportingVertexOne.getPosition());
      supportPoint.sub(supportingVertexTwo.getPosition());
   }

   public void setGilbertJohnsonKeerthiCollisionDetectorListener(GilbertJohnsonKeerthiCollisionDetectorListener listener)
   {
      this.listener = listener;
   }
   
   public boolean arePolytopesColliding(ConvexPolytope polytopeA, ConvexPolytope polytopeB, Point3d pointOnAToPack, Point3d pointOnBToPack)
   {
      simplex.clearPoints();

      // Step 1) Initialize Simplex Q to a single point in A minkowskiDifference B. Here we'll just use A.vertex0 and B.vertex0
      PolytopeVertex vertexOne = polytopeA.getVertex(0);
      PolytopeVertex vertexTwo = polytopeB.getVertex(0);

      Point3d minkowskiDifferenceVertex = new Point3d();
      minkowskiDifferenceVertex.sub(vertexOne.getPosition(), vertexTwo.getPosition());

      simplex.addVertex(minkowskiDifferenceVertex, vertexOne.getPosition(), vertexTwo.getPosition());
      
      if (listener != null)
      {
         listener.addedVertexToSimplex(simplex, minkowskiDifferenceVertex, vertexOne.getPosition(), vertexTwo.getPosition());
      }
      
      int iterations = 0;
      int metStoppingConditionsCount = 0;
      while (true)
      {
         // Step 2) Compute closest point to origin in the simplex. 4) Reduce points of Q not used in determining P.
         Point3d closestPointToOrigin = new Point3d();
         simplex.getClosestPointToOriginOnConvexHullAndRemoveUnusedVertices(closestPointToOrigin);

         if (listener != null)
         {
            listener.foundClosestPointOnSimplex(simplex, closestPointToOrigin);
         }
         
         // Step 3) If P is origin, done.
         double distanceSquared = closestPointToOrigin.distanceSquared(new Point3d());
         //TODO: Magic epsilon here...
         if (distanceSquared < 1e-8)
         {
            if (listener != null)
            {
               listener.foundCollision(simplex, pointOnAToPack, pointOnBToPack);
            }
            
            simplex.getClosestPointsOnAAndB(pointOnAToPack, pointOnBToPack);
            return true;
         }

         // Step 5) v = support vector in negative P direction on A minkowskiDifference B.
         // In other words, it is suppport vector on A in P direction minus support vector on B in negative P direction.

         Vector3d supportDirection = new Vector3d(closestPointToOrigin);
         supportDirection.negate();
         PolytopeVertex supportingVertexA = polytopeA.getSupportingVertex(supportDirection);

         supportDirection.negate();
         PolytopeVertex supportingVertexB = polytopeB.getSupportingVertex(supportDirection);

         Vector3d supportingVertexOnSimplex = new Vector3d();
         
         Point3d supportingVertexOnA = supportingVertexA.getPosition();
         Point3d supportingVertexOnB = supportingVertexB.getPosition();
         if (simplex.wereMostRecentlyDiscared(supportingVertexOnA, supportingVertexOnB))
         {
            simplex.getClosestPointsOnAAndB(pointOnAToPack, pointOnBToPack);
            
            if (listener != null)
            {
               listener.metStoppingConditionForNoIntersection(pointOnAToPack, pointOnBToPack);
            }
            
            return false;
         }
         supportingVertexOnSimplex.sub(supportingVertexOnA, supportingVertexOnB);
         
         if (listener != null)
         {
            listener.foundSupportPoints(simplex, supportingVertexOnA, supportingVertexOnB, supportingVertexOnSimplex);
         }

         // Step 6) If v is no more extremal in direction -P than P itself, then not intersecting. magnitude(v) is distance. Use v to determine closest points.

         Vector3d P = new Vector3d(closestPointToOrigin);

         double vDotP = supportingVertexOnSimplex.dot(P);
         double percentCloser = vDotP/P.dot(P);

         if (listener != null)
         {
            listener.computeVDotPAndPercentCloser(vDotP, percentCloser);
         }
         
//         System.out.println("vDotP - PSquared = " + foo);

         // TODO: Do we need this epsilon here. Seems when to abort is tricky. Maybe look that the extremal points in the
         // simplex stop changing?
//         if (percentCloser >= 1.0 - LAMBDA_STOPPING_DELTA)
//         {
//            metStoppingConditionsCount++;
//         }
         
         // TODO: Get rid of this. Hack right now to make sure really hard cases get a full shot...
         if (metStoppingConditionsCount == 4)
         {
            simplex.getClosestPointsOnAAndB(pointOnAToPack, pointOnBToPack);
            
            if (listener != null)
            {
               listener.metStoppingConditionForNoIntersection(pointOnAToPack, pointOnBToPack);
            }
            
            return false;
         }

         // Step 7) Add v to Q and got to step 2.
         Point3d pointToAddToSimplex = new Point3d(supportingVertexOnSimplex);
         boolean successfullyAddedVertex = simplex.addVertex(pointToAddToSimplex, supportingVertexA.getPosition(), supportingVertexB.getPosition());

         
         if (!successfullyAddedVertex)
         {
            simplex.getClosestPointsOnAAndB(pointOnAToPack, pointOnBToPack);
            
            if (listener != null)
            {
               listener.metStoppingConditionForNoIntersection(pointOnAToPack, pointOnBToPack);
            }
            
            return false;
         }
         
         if (listener != null)
         {
            listener.addedVertexToSimplex(simplex, pointToAddToSimplex, supportingVertexA.getPosition(), supportingVertexB.getPosition());
         }
         
         iterations++;
         if (iterations > 100)
         {
            System.out.println("Seems to be looping... lambda = " + percentCloser);
            System.out.println("Seems to be looping... closestPointToOrigin = " + closestPointToOrigin);
         }
         
         if (iterations > 106)
         {
            simplex.getClosestPointsOnAAndB(pointOnAToPack, pointOnBToPack);
            
            if (listener != null)
            {
               listener.tooManyIterationsStopping(simplex, pointOnAToPack, pointOnBToPack);
            }
            
            return false;
         }
      }

   }

}
