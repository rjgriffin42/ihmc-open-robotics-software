package us.ihmc.robotics.geometry;

import us.ihmc.robotics.Axis;

import javax.vecmath.Matrix3d;

public class RotationalInertiaCalculator
{
   // TODO: should this be made private?
   public RotationalInertiaCalculator()
   {
   }

   public static Matrix3d getRotationalInertiaFromRadiiOfGyration(double mass, double radiusOfGyrationX, double radiusOfGyrationY, double radiusOfGyrationZ)
   {
      checkMassAndDimensions(mass, radiusOfGyrationX, radiusOfGyrationY, radiusOfGyrationZ);
      double ixx = mass * (radiusOfGyrationY * radiusOfGyrationY + radiusOfGyrationZ * radiusOfGyrationZ);
      double iyy = mass * (radiusOfGyrationX * radiusOfGyrationX + radiusOfGyrationZ * radiusOfGyrationZ);
      double izz = mass * (radiusOfGyrationX * radiusOfGyrationX + radiusOfGyrationY * radiusOfGyrationY);

      Matrix3d ret = getRotationalInertiaFromDiagonal(ixx, iyy, izz);

      return ret;
   }

   public static void checkMassAndDimensions(double mass, double dimensionX, double dimensionY, double dimensionZ)
   {
      if (mass < 0.0)
         throw new RuntimeException("can not pass in negative mass values");
      if ((dimensionX < 0.0) || (dimensionY < 0.0) || (dimensionZ < 0.0))
         throw new RuntimeException("can not pass in negative dimensions");
   }

   public static Matrix3d getRotationalInertiaMatrixOfTorus(double mass, double radiusOfDonut, double radiusOfTube)
   {
      double ixx = ((5.0 / 8.0 * radiusOfTube * radiusOfTube) + (1.0 / 2.0 * radiusOfDonut * radiusOfDonut)) * mass;
      double iyy = ixx;
      double izz = ((3.0 / 4.0 * radiusOfTube * radiusOfTube) + (radiusOfDonut * radiusOfDonut)) * mass;

      Matrix3d ret = getRotationalInertiaFromDiagonal(ixx, iyy, izz);

      return ret;
   }

   public static Matrix3d getRotationalInertiaMatrixOfSolidCylinder(double mass, double radius, double height, Axis axisOfCylinder)
   {
      double[] rotationalInertias = getIxxIyyIzzOfSolidCylinder(mass, radius, height, axisOfCylinder);

      Matrix3d rotationalMatrix = getRotationalInertiaFromDiagonal(rotationalInertias[0], rotationalInertias[1], rotationalInertias[2]);

      return rotationalMatrix;

   }

   public static Matrix3d getRotationalInertiaMatrixOfSolidEllipsoid(double mass, double xRadius, double yRadius, double zRadius)
   {
      checkMassAndDimensions(mass, xRadius, yRadius, zRadius);
      double ixx = 1.0 / 5.0 * mass * (yRadius * yRadius + zRadius * zRadius);
      double iyy = 1.0 / 5.0 * mass * (zRadius * zRadius + xRadius * xRadius);
      double izz = 1.0 / 5.0 * mass * (xRadius * xRadius + yRadius * yRadius);

      Matrix3d ret = getRotationalInertiaFromDiagonal(ixx, iyy, izz);

      return ret;
   }

   public static Matrix3d getRotationalInertiaMatrixOfSolidBox(double xLength, double yWidth, double zHeight, double mass)
   {
      checkMassAndDimensions(mass, xLength, yWidth, zHeight);
      double ixx = 1 / 12.0 * mass * (yWidth * yWidth + zHeight * zHeight);
      double iyy = 1 / 12.0 * mass * (xLength * xLength + zHeight * zHeight);
      double izz = 1 / 12.0 * mass * (xLength * xLength + yWidth * yWidth);
      Matrix3d ret = getRotationalInertiaFromDiagonal(ixx, iyy, izz);

      return ret;
   }

   public static double[] getIxxIyyIzzOfSolidCylinder(double mass, double radius, double height, Axis axisOfCylinder)
   {
      checkMassAndDimensions(mass, radius, radius, height);

      double IalongAxis = 0.5 * mass * radius * radius;
      double IcrossAxis = mass * (3.0 * radius * radius + height * height) / 12.0;

      double Ixx, Iyy, Izz;

      switch (axisOfCylinder)
      {
         case X :
         {
            Ixx = IalongAxis;
            Iyy = IcrossAxis;
            Izz = IcrossAxis;

            break;
         }

         case Y :
         {
            Ixx = IcrossAxis;
            Iyy = IalongAxis;
            Izz = IcrossAxis;

            break;
         }

         case Z :
         {
            Ixx = IcrossAxis;
            Iyy = IcrossAxis;
            Izz = IalongAxis;

            break;
         }

         default :
         {
            throw new RuntimeException("invalid axis. Axis=" + axisOfCylinder);
         }
      }

      return new double[] {Ixx, Iyy, Izz};

   }

   public static Matrix3d getRotationalInertiaFromDiagonal(double ixx, double iyy, double izz)
   {
      Matrix3d ret = new Matrix3d();
      ret.setM00(ixx);
      ret.setM11(iyy);
      ret.setM22(izz);

      return ret;
   }
}
