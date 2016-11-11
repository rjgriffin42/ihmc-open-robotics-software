package us.ihmc.footstepPlanning.polygonWiggling;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.convexOptimization.quadraticProgram.QuadProgSolver;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.tools.io.printing.PrintTools;

public class PolygonWiggler
{
   private static final boolean DEBUG = false;

   /**
    * Constant weight factor given to rotation in comparison to translation when attempting to move a polygon into a region.
    */
   private static final double gamma = 0.2;

   /**
    * Returns a transform that will move the given polygon into a planar region. Problematic if the planar region consists of
    * multiple sub convex polygons. The polygon to wiggle must have the same transform to world as the planar region.
    *
    * @param polygonToWiggleInRegionFrame
    * @param regionToWiggleInto
    * @param maxYaw
    * @param minYaw
    * @return
    */
   public static RigidBodyTransform wigglePolygonIntoRegion(ConvexPolygon2d polygonToWiggleInRegionFrame, PlanarRegion regionToWiggleInto, double maxYaw, double minYaw)
   {
      // find the part of the region that has the biggest intersection with the polygon
      ConvexPolygon2d bestMatch = null;
      double overlap = 0.0;
      for (int i = 0; i < regionToWiggleInto.getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2d intersection = new ConvexPolygon2d();
         regionToWiggleInto.getConvexPolygon(i).intersectionWith(polygonToWiggleInRegionFrame, intersection);
         if (intersection.getArea() > overlap)
         {
            overlap = intersection.getArea();
            bestMatch = regionToWiggleInto.getConvexPolygon(i);
         }
      }

      if (bestMatch == null)
         return null;

      RigidBodyTransform wiggleTransform = findWiggleTransform(polygonToWiggleInRegionFrame, bestMatch, maxYaw, minYaw);
      if (wiggleTransform == null || wiggleTransform.containsNaN())
         return null;

      return wiggleTransform;
   }

   /**
    * This method moves a convex polygon into a given convex region. It will return the new polygon without modifying the given one.
    * The algorithm assumes a small rotation angle (it will linearize sin and cos around 0.0). For that reason it is possible to
    * specify a maximum and a minimum rotation.
    *
    * @param polygonToWiggle
    * @param planeToWiggleInto
    * @param maxYaw
    * @param minYaw
    * @return
    */
   public static ConvexPolygon2d wigglePolygon(ConvexPolygon2d polygonToWiggle, ConvexPolygon2d planeToWiggleInto, double maxYaw, double minYaw)
   {
      ConvexPolygon2d wiggledPolygon = new ConvexPolygon2d(polygonToWiggle);
      RigidBodyTransform wiggleTransform = findWiggleTransform(polygonToWiggle, planeToWiggleInto, maxYaw, minYaw);
      if (wiggleTransform == null || wiggleTransform.containsNaN())
         return null;
      wiggledPolygon.applyTransformAndProjectToXYPlane(wiggleTransform);
      return wiggledPolygon;
   }

   /**
    * This method will find a transform that moves a convex polygon into a given convex region. The algorithm assumes a small rotation
    * angle (it will linearize sin and cos around 0.0). For that reason it is possible to specify a maximum and a minimum rotation.
    *
    * @param polygonToWiggle
    * @param planeToWiggleInto
    * @param maxYaw
    * @param minYaw
    * @return
    */
   public static RigidBodyTransform findWiggleTransform(ConvexPolygon2d polygonToWiggle, ConvexPolygon2d planeToWiggleInto, double maxYaw, double minYaw)
   {
      int constraintsPerPoint = planeToWiggleInto.getNumberOfVertices();
      int numberOfPoints = polygonToWiggle.getNumberOfVertices();
      Point2d pointToRotateAbout = polygonToWiggle.getCentroid();

      DenseMatrix64F A = new DenseMatrix64F(0);
      DenseMatrix64F b = new DenseMatrix64F(0);
      convertToInequalityConstraints(planeToWiggleInto, A, b);

      DenseMatrix64F A_full = new DenseMatrix64F(constraintsPerPoint * numberOfPoints + 2, 3);
      DenseMatrix64F b_full = new DenseMatrix64F(constraintsPerPoint * numberOfPoints + 2, 1);
      // add limits on allowed rotation
      // theta <= 10 degree
      A_full.set(constraintsPerPoint * numberOfPoints, 2, 1.0);
      b_full.set(constraintsPerPoint * numberOfPoints, maxYaw);
      // theta >= -10 degree --> -theta <= 10 degree
      A_full.set(constraintsPerPoint * numberOfPoints + 1, 2, -1.0);
      b_full.set(constraintsPerPoint * numberOfPoints + 1, minYaw);

      for (int i = 0; i < numberOfPoints; i++)
      {
         DenseMatrix64F p = new DenseMatrix64F(2, 1);
         p.set(0, polygonToWiggle.getVertex(i).x);
         p.set(1, polygonToWiggle.getVertex(i).y);

         // inequality constraint becomes A*V * x <= b - A*p
         Point2d point = new Point2d(polygonToWiggle.getVertex(i));
         point.sub(pointToRotateAbout);
         DenseMatrix64F V = new DenseMatrix64F(2, 3);
         V.set(0, 0, 1.0);
         V.set(0, 1, 0.0);
         V.set(0, 2, -point.y);
         V.set(1, 0, 0.0);
         V.set(1, 1, 1.0);
         V.set(1, 2, point.x);

         DenseMatrix64F A_new = new DenseMatrix64F(constraintsPerPoint, 3);
         DenseMatrix64F b_new = new DenseMatrix64F(constraintsPerPoint, 1);
         CommonOps.mult(A, V, A_new);
         CommonOps.mult(A, p, b_new);
         CommonOps.changeSign(b_new);
         CommonOps.add(b, b_new, b_new);

         CommonOps.insert(A_new, A_full, constraintsPerPoint * i, 0);
         CommonOps.insert(b_new, b_full, constraintsPerPoint * i, 0);
      }

      DenseMatrix64F costMatrix = new DenseMatrix64F(3, 3);
      CommonOps.setIdentity(costMatrix);
      costMatrix.set(2, 2, gamma);
      DenseMatrix64F costVector = new DenseMatrix64F(3, 1);
      CommonOps.fill(costVector, 0.0);

      QuadProgSolver solver = new QuadProgSolver();
      DenseMatrix64F Aeq = new DenseMatrix64F(0, 3);
      DenseMatrix64F beq = new DenseMatrix64F(0, 3);
      DenseMatrix64F result = new DenseMatrix64F(3, 1);
      try
      {
         int iterations = solver.solve(costMatrix, costVector, Aeq, beq, A_full, b_full, result, false);
         if (DEBUG)
         {
            PrintTools.info("Iterations: " + iterations);
            PrintTools.info("Result: " + result);
         }
      }
      catch (Exception e)
      {
         return null;
      }

      // assemble the transform
      double theta = result.get(2);
      Vector3d translation = new Vector3d(result.get(0), result.get(1), 0.0);
      Vector3d offset = new Vector3d(pointToRotateAbout.x, pointToRotateAbout.y, 0.0);

      RigidBodyTransform toOriginTransform = new RigidBodyTransform();
      toOriginTransform.setTranslationAndIdentityRotation(offset);

      RigidBodyTransform rotationTransform = new RigidBodyTransform();
      rotationTransform.applyRotationZ(theta);

      RigidBodyTransform fullTransform = new RigidBodyTransform(toOriginTransform);
      fullTransform.multiply(rotationTransform);
      toOriginTransform.invert();
      fullTransform.multiply(toOriginTransform);

      Matrix3d rotationMatrix = new Matrix3d();
      rotationTransform.getRotation(rotationMatrix);
      rotationMatrix.transpose();
      rotationMatrix.transform(translation);
      RigidBodyTransform translationTransform = new RigidBodyTransform();
      translationTransform.setTranslationAndIdentityRotation(translation);
      fullTransform.multiply(translationTransform);

      return fullTransform;
   }

   /**
    * Packs the matrices A and b such that any point x is inside the polygon if it satisfies the equation A*x <= b.
    *
    * @param polygon
    * @param A
    * @param b
    */
   public static void convertToInequalityConstraints(ConvexPolygon2d polygon, DenseMatrix64F A, DenseMatrix64F b)
   {
      int constraints = polygon.getNumberOfVertices();
      A.reshape(constraints, 2);
      b.reshape(constraints, 1);

      for (int i = 0; i < constraints; i++)
      {
         Point2d firstPoint = polygon.getVertex(i);
         Point2d secondPoint = polygon.getNextVertex(i);

         A.set(i, 0, firstPoint.y - secondPoint.y);
         A.set(i, 1, -firstPoint.x + secondPoint.x);
         b.set(i, firstPoint.y * (secondPoint.x - firstPoint.x) - firstPoint.x * (secondPoint.y - firstPoint.y));
      }
   }

}
