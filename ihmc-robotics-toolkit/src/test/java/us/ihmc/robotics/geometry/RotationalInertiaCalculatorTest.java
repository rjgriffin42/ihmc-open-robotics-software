package us.ihmc.robotics.geometry;

import static us.ihmc.robotics.Assert.*;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.euclid.Axis;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.commons.MathTools;

public class RotationalInertiaCalculatorTest
{
   private static final double DELTA = 1e-3;
   private static final int ITERATIONS = 1000;
   private double maxRandomValue;
   private Random random;

   @BeforeEach
   public void setUp()
   {
      maxRandomValue = 1e3;
      random = new Random();
   }

   private double randomPositiveDouble()
   {
      return random.nextDouble() * maxRandomValue;
   }

	@Test
   public void testBasicCylinder()
   {
      for (int i = 0; i < ITERATIONS; i++)
      {
         double mass = randomPositiveDouble();
         double radius = randomPositiveDouble();
         double height = randomPositiveDouble();

         int pick = random.nextInt(Axis.values().length);
         Axis axis = Axis.values()[pick];

         double[] IxxIyyIzz = RotationalInertiaCalculator.getIxxIyyIzzOfSolidCylinder(mass, radius, height, axis);
         double inCircleAxis0, inCircleAxis1, mainAxis;

         switch (axis)
         {
            case X :
               inCircleAxis0 = IxxIyyIzz[1];
               inCircleAxis1 = IxxIyyIzz[2];
               mainAxis = IxxIyyIzz[0];

               break;

            case Y :
               inCircleAxis0 = IxxIyyIzz[0];
               inCircleAxis1 = IxxIyyIzz[2];
               mainAxis = IxxIyyIzz[1];

               break;

            case Z :
               inCircleAxis0 = IxxIyyIzz[0];
               inCircleAxis1 = IxxIyyIzz[1];
               mainAxis = IxxIyyIzz[2];

               break;

            default :
               throw new RuntimeException("A new axis has been added to the Axis enum, but is not covered in the testcase.");
         }

         assertEquals(mass * (3.0 * radius * radius + height * height) / 12.0, inCircleAxis0, DELTA);
         assertEquals(mass * (3.0 * radius * radius + height * height) / 12.0, inCircleAxis1, DELTA);
         assertEquals(0.5 * mass * radius * radius, mainAxis, DELTA);
         
         Matrix3D rotationMatrix = RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidCylinder(mass, radius, height, axis);
         assertEquals(IxxIyyIzz[0], rotationMatrix.getM00(), DELTA);
         assertEquals(IxxIyyIzz[1], rotationMatrix.getM11(), DELTA);
         assertEquals(IxxIyyIzz[2], rotationMatrix.getM22(), DELTA);
         assertEquals(0.0, rotationMatrix.getM01(), DELTA);
         assertEquals(0.0, rotationMatrix.getM02(), DELTA);
         assertEquals(0.0, rotationMatrix.getM10(), DELTA);
         assertEquals(0.0, rotationMatrix.getM12(), DELTA);
         assertEquals(0.0, rotationMatrix.getM20(), DELTA);
         assertEquals(0.0, rotationMatrix.getM21(), DELTA);
      }
   }

	@Test
   public void testBasicCylinderNegativeMass()
   {
      double mass = -1.0;
      double radius = 1.0;
      double height = 1.0;
      Axis axis = Axis.Z;
      RotationalInertiaCalculator.getIxxIyyIzzOfSolidCylinder(mass, radius, height, axis);
   }

	@Test
   public void testBasicCylinderNegativeRadius()
   {
      double mass = 1.0;
      double radius = -1.0;
      double height = 1.0;
      Axis axis = Axis.Z;
      RotationalInertiaCalculator.getIxxIyyIzzOfSolidCylinder(mass, radius, height, axis);
   }

	@Test
   public void testBasicCylinderNegativeHeight()
   {
      double mass = 1.0;
      double radius = 1.0;
      double height = -1.0;
      Axis axis = Axis.Z;
      RotationalInertiaCalculator.getIxxIyyIzzOfSolidCylinder(mass, radius, height, axis);
   }

	@Test
   public void testSolidEllipsoid()
   {
	   for( int i = 0 ; i < ITERATIONS; i++)
	   {
		   double mass = randomPositiveDouble();
		   double xRadius = randomPositiveDouble();
		   double yRadius = randomPositiveDouble();
		   double zRadius = randomPositiveDouble();

		   double Ixx = mass * (MathTools.square(yRadius) + MathTools.square(zRadius)) / 5.0;
		   double Iyy = mass * (MathTools.square(xRadius) + MathTools.square(zRadius)) / 5.0;
		   double Izz = mass * (MathTools.square(yRadius) + MathTools.square(xRadius)) / 5.0;
		   
		   Matrix3D inertiaTensor = RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidEllipsoid(mass, xRadius, yRadius, zRadius);

		   assertEquals(Ixx, inertiaTensor.getM00(), DELTA);
		   assertEquals(Iyy, inertiaTensor.getM11(), DELTA);
		   assertEquals(Izz, inertiaTensor.getM22(), DELTA);
	   }
   }

	@Test
   public void testSolidEllipsoidNegativeMass()
   {
	   double mass = -1.0;
	   double xRadius = 1.0;
	   double yRadius = 1.0;
	   double zRadius = 1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidEllipsoid(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testSolidEllipsoidNegativeXRadius()
   {
	   double mass = 1.0;
	   double xRadius = -1.0;
	   double yRadius = 1.0;
	   double zRadius = 1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidEllipsoid(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testSolidEllipsoidNegativeYRadius()
   {
	   double mass = 1.0;
	   double xRadius = 1.0;
	   double yRadius = -1.0;
	   double zRadius = 1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidEllipsoid(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testSolidEllipsoidNegativeZRadius()
   {
	   double mass = 1.0;
	   double xRadius = 1.0;
	   double yRadius = 1.0;
	   double zRadius = -1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidEllipsoid(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testRadiiOfGyration()
   {
	   for(int i = 0; i< ITERATIONS; i++)
	   {
		   double mass = randomPositiveDouble();
		   double xRadius = randomPositiveDouble();
		   double yRadius = randomPositiveDouble();
		   double zRadius = randomPositiveDouble();
		   
		   Matrix3D rotationalInertia = RotationalInertiaCalculator.getRotationalInertiaFromRadiiOfGyration(mass, xRadius, yRadius, zRadius);

		   assertEquals(mass * (MathTools.square(yRadius) + MathTools.square(zRadius)), rotationalInertia.getM00(), DELTA);
		   assertEquals(mass * (MathTools.square(zRadius) + MathTools.square(xRadius)), rotationalInertia.getM11(), DELTA);
		   assertEquals(mass * (MathTools.square(xRadius) + MathTools.square(yRadius)), rotationalInertia.getM22(), DELTA);
		   assertEquals(0.0, rotationalInertia.getM01(), DELTA);
		   assertEquals(0.0, rotationalInertia.getM02(), DELTA);
		   assertEquals(0.0, rotationalInertia.getM10(), DELTA);
		   assertEquals(0.0, rotationalInertia.getM12(), DELTA);
		   assertEquals(0.0, rotationalInertia.getM20(), DELTA);
		   assertEquals(0.0, rotationalInertia.getM21(), DELTA);
	   }
   }

	@Test
   public void testRadiiOfGyrationNegativeMass()
   {
	   double mass = -1.0;
	   double xRadius = 1.0;
	   double yRadius = 1.0;
	   double zRadius = 1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaFromRadiiOfGyration(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testRadiiOfGyrationNegativeXRadius()
   {
	   double mass = 1.0;
	   double xRadius = -1.0;
	   double yRadius = 1.0;
	   double zRadius = 1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaFromRadiiOfGyration(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testRadiiOfGyrationNegativeYRadius()
   {
	   double mass = 1.0;
	   double xRadius = 1.0;
	   double yRadius = -1.0;
	   double zRadius = 1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaFromRadiiOfGyration(mass, xRadius, yRadius, zRadius);
   }

	@Test
   public void testRadiiOfGyrationNegativeZRadius()
   {
	   double mass = 1.0;
	   double xRadius = 1.0;
	   double yRadius = 1.0;
	   double zRadius = -1.0;
	   
	   RotationalInertiaCalculator.getRotationalInertiaFromRadiiOfGyration(mass, xRadius, yRadius, zRadius);
   }
}
