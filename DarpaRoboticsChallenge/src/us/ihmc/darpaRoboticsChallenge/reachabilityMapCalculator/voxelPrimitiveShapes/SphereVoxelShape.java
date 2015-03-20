package us.ihmc.darpaRoboticsChallenge.reachabilityMapCalculator.voxelPrimitiveShapes;

import java.awt.Color;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.GeometryTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RotationFunctions;

/**
 * SphereVoxelShape creates N points uniformly distributed on the surface of a sphere.
 * For each point, a ray, which goes from the point on the surface of the sphere to its origin, is generated.
 * For each ray M rotations are generated by computing the orientation aligning the x-axis to the ray and transforming this orientation by M rotations around the ray.
 * 
 * This class is meant to help discretizing the 3D space of orientations and also to simulate the different possibilities for grasping a spherical object.
 *
 */
public class SphereVoxelShape
{
   public enum SphereVoxelType {graspOrigin, graspAroundSphere};
   private final Quat4d[][] rotations;
   private final Point3d[] pointsOnSphere;
   /** Origin of the sphere in the current voxel coordinate. Should probably always be set to zero. */
   private final Point3d sphereOrigin = new Point3d();
   private final double voxelSize;

   private final int numberOfRays;
   private final int numberOfRotationsAroundRay;

   private final SphereVoxelType type;
   private final ReferenceFrame parentFrame;
   
   public SphereVoxelShape(ReferenceFrame parentFrame, double voxelSize, int numberOfRays, int numberOfRotationsAroundRay, SphereVoxelType type)
   {
      this.voxelSize = voxelSize;
      this.parentFrame = parentFrame;
      this.type = type;
      this.numberOfRays = numberOfRays;
      this.numberOfRotationsAroundRay = numberOfRotationsAroundRay;

      pointsOnSphere = generatePointsOnSphereUsingSpiralBasedAlgorithm(sphereOrigin, voxelSize, numberOfRays);
      rotations = new Quat4d[numberOfRays][numberOfRotationsAroundRay];
      
      Vector3d rayThroughSphere = new Vector3d();
      
      AxisAngle4d rotationForXAxisAlignedWithRay = new AxisAngle4d();
      AxisAngle4d rotationAroundRay = new AxisAngle4d();
      Matrix3d finalRotationMatrix = new Matrix3d();
      Matrix3d rotationMatrixForXAxisAlignedWithRay = new Matrix3d();
      Matrix3d rotationMatrixAroundRay = new Matrix3d();
      
      Vector3d xAxis = new Vector3d(1.0, 0.0, 0.0);
      double stepSizeAngleArounRay = 2.0 * Math.PI / numberOfRotationsAroundRay;
      
      for (int rayIndex = 0; rayIndex < numberOfRays; rayIndex++)
      {
         // Ray that goes from the surface of the sphere to its origin
         rayThroughSphere.sub(sphereOrigin, pointsOnSphere[rayIndex]);
         rayThroughSphere.normalize();
         
         GeometryTools.getRotationBasedOnNormal(rotationForXAxisAlignedWithRay, rayThroughSphere, xAxis);
         rotationMatrixForXAxisAlignedWithRay.set(rotationForXAxisAlignedWithRay);

         for (int rotationAroundRayIndex = 0; rotationAroundRayIndex < numberOfRotationsAroundRay; rotationAroundRayIndex++)
         {
            double angle = rotationAroundRayIndex * stepSizeAngleArounRay;
            rotationAroundRay.set(rayThroughSphere, angle);
            rotationMatrixAroundRay.set(rotationAroundRay);

            finalRotationMatrix.mul(rotationMatrixAroundRay, rotationMatrixForXAxisAlignedWithRay);

            Quat4d rotation = new Quat4d();
            RotationFunctions.setQuaternionBasedOnMatrix3d(rotation, finalRotationMatrix);
            rotations[rayIndex][rotationAroundRayIndex] = rotation;
         }
      }
   }

   public int getNumberOfRays()
   {
      return numberOfRays;
   }

   public int getNumberOfRotationsAroundRay()
   {
      return numberOfRotationsAroundRay;
   }

   public void getRay(Vector3d rayToPack, int rayIndex)
   {
      MathTools.checkIfInRange(rayIndex, 0, numberOfRays - 1);

      rayToPack.sub(sphereOrigin, pointsOnSphere[rayIndex]);
      rayToPack.normalize();
   }

   public void getOrientation(FrameOrientation orientation, int rayIndex, int rotationAroundRayIndex)
   {
      MathTools.checkIfInRange(rayIndex, 0, numberOfRays - 1);
      MathTools.checkIfInRange(rotationAroundRayIndex, 0, numberOfRotationsAroundRay - 1);

      orientation.setIncludingFrame(parentFrame, rotations[rayIndex][rotationAroundRayIndex]);
   }

   public void getPose(FrameVector translationFromVoxelOrigin, FrameOrientation orientation, int rayIndex, int rotationAroundRayIndex)
   {
      MathTools.checkIfInRange(rayIndex, 0, numberOfRays - 1);
      MathTools.checkIfInRange(rotationAroundRayIndex, 0, numberOfRotationsAroundRay - 1);

      if (type == SphereVoxelType.graspAroundSphere)
         translationFromVoxelOrigin.setIncludingFrame(parentFrame, pointsOnSphere[rayIndex]);
      else
         translationFromVoxelOrigin.setToZero(parentFrame);
      orientation.setIncludingFrame(parentFrame, rotations[rayIndex][rotationAroundRayIndex]);
   }

   public Point3d[] getPointsOnSphere()
   {
      return pointsOnSphere;
   }

   public Graphics3DObject createVisualization(FramePoint voxelLocation, double scale, double reachabilityValue)
   {
      ReferenceFrame originalFrame = voxelLocation.getReferenceFrame();
      voxelLocation.changeFrame(ReferenceFrame.getWorldFrame());

      Graphics3DObject voxelViz = new Graphics3DObject();

      AppearanceDefinition appearance = YoAppearance.RGBColorFromHex(Color.HSBtoRGB((float) (0.7 * reachabilityValue), 1.0f, 1.0f));

      voxelViz.translate(voxelLocation.getX(), voxelLocation.getY(), voxelLocation.getZ());
      voxelViz.addSphere(scale * voxelSize / 2.0, appearance);

      voxelLocation.changeFrame(originalFrame);

      return voxelViz;
   }

   public static Point3d[] generatePointsOnSphereUsingSpiralBasedAlgorithm(double sphereRadius, int numberOfPointsToGenerate)
   {
      return generatePointsOnSphereUsingSpiralBasedAlgorithm(new Point3d(), sphereRadius, numberOfPointsToGenerate);
   }

   /**
    * Generates a number of points uniformly distributed over the surface of a sphere using a spiral-based approach.
    * This algorithm can be found in the paper: "Distributing Many Points on a Sphere" by E.B. Saff and B.J. Kuijlaars. (PDF version was on Google on the 02/13/2015).
    */
   public static Point3d[] generatePointsOnSphereUsingSpiralBasedAlgorithm(Point3d sphereOrigin, double sphereRadius, int numberOfPointsToGenerate)
   {
      Point3d[] pointsOnSphere = new Point3d[numberOfPointsToGenerate];

      // Two solutions are suggested for deltaN:
//      double deltaN = Math.sqrt(8.0 * Math.PI / Math.sqrt(3)) / Math.sqrt(numberOfPoints);
      // From the paper it is said that the following can also be used. It seems to be a magic number at the end.
      double deltaN = 3.6 / Math.sqrt(numberOfPointsToGenerate);
      double phi;
      double previousPhi = 0.0;

      pointsOnSphere[0] = new Point3d();
      pointsOnSphere[0].set(0.0, 0.0, -sphereRadius);
      pointsOnSphere[0].add(sphereOrigin);

      for (int planeIndex = 1; planeIndex < numberOfPointsToGenerate - 1; planeIndex++)
      {
         double unitHeight = -1.0 + 2.0 * planeIndex / (numberOfPointsToGenerate - 1.0);
         double theta = Math.acos(unitHeight);
         phi = previousPhi + deltaN / Math.sqrt(1 - unitHeight * unitHeight);
         AngleTools.trimAngleMinusPiToPi(phi);

         double rSinTheta = sphereRadius * Math.sin(theta);
         pointsOnSphere[planeIndex] = new Point3d();
         pointsOnSphere[planeIndex].x = rSinTheta * Math.cos(phi);
         pointsOnSphere[planeIndex].y = rSinTheta * Math.sin(phi);
         pointsOnSphere[planeIndex].z = sphereRadius * Math.cos(theta);
         pointsOnSphere[planeIndex].add(sphereOrigin);

         previousPhi = phi;
      }

      pointsOnSphere[numberOfPointsToGenerate - 1] = new Point3d();
      pointsOnSphere[numberOfPointsToGenerate - 1].set(0.0, 0.0, sphereRadius);
      pointsOnSphere[numberOfPointsToGenerate - 1].add(sphereOrigin);

      return pointsOnSphere;
   }
}
