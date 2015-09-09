package us.ihmc.robotics.geometry;

import org.junit.Test;

import us.ihmc.tools.random.RandomTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class PointToLineUnProjectorTest
{
   private static final double eps = 1e-7;

	@DeployableTestMethod(duration = 0.0)
	@Test(timeout = 30000)
   public void testsimpleCase()
   {
      double x0 = 1.0;
      double y0 = 0.0;
      double z0 = 0.0;
      double x1 = 0.0;
      double y1 = 0.0;
      double z1 = 0.0;
      double x2 = 0.0;
      double y2 = 0.0;
      double z2 = 0.0;
      runTest(x0, y0, z0, x1, y1, z1, x2, y2, z2);
   }

	@DeployableTestMethod(duration = 0.0)
	@Test(timeout = 30000)
   public void testGeneralCase()
   {
      Random gen = new Random(124L);
      for (int i=0;i<100000;i++)
      {
         Point3d point1 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         Point3d point2 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         double s = RandomTools.generateRandomDouble(gen, 0, 1);
         Point3d temp = new Point3d(point1);
         temp.scale(1-s);
         Point3d point3 = new Point3d(temp);
         temp.set(point2);
         temp.scale(s);
         point3.add(temp);
         runTest(point1.x, point1.y, point1.z, point2.x, point2.y, point2.z, point3.x, point3.y, point3.z);
      }
   }

	@DeployableTestMethod(duration = 0.0)
	@Test(timeout = 30000)
   public void testDegenerateCase()
   {
      Random gen = new Random(124L);
      for (int i=0;i<100000;i++)
      {
         Point3d point1 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         Point3d point2 = new Point3d(point1);
         double s = RandomTools.generateRandomDouble(gen, 0, 1);
         Point3d temp = new Point3d(point1);
         temp.scale(1-s);
         Point3d point3 = new Point3d(temp);
         temp.set(point2);
         temp.scale(s);
         point3.add(temp);
         runTest(point1.x, point1.y, point1.z, point2.x, point2.y, point2.z, point3.x, point3.y, point3.z);
      }
   }

	@DeployableTestMethod(duration = 0.0)
	@Test(timeout = 30000)
   public void testYCase()
   {
      Random gen = new Random(124L);
      for (int i=0;i<100000;i++)
      {
         Point3d point1 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         Point3d point2 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         point2.setY(point1.getY());
         double s = RandomTools.generateRandomDouble(gen, 0, 1);
         Point3d temp = new Point3d(point1);
         temp.scale(1-s);
         Point3d point3 = new Point3d(temp);
         temp.set(point2);
         temp.scale(s);
         point3.add(temp);
         runTest(point1.x, point1.y, point1.z, point2.x, point2.y, point2.z, point3.x, point3.y, point3.z);
      }
   }   @Test(timeout=300000)
   public void testXCase()
   {
      Random gen = new Random(124L);
      for (int i=0;i<100000;i++)
      {
         Point3d point1 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         Point3d point2 = RandomTools.generateRandomPoint(gen, -1000, -1000, -1000, 1000, 1000, 1000);
         point2.setX(point1.getX());
         double s = RandomTools.generateRandomDouble(gen, 0, 1);
         Point3d temp = new Point3d(point1);
         temp.scale(1-s);
         Point3d point3 = new Point3d(temp);
         temp.set(point2);
         temp.scale(s);
         point3.add(temp);
         runTest(point1.x, point1.y, point1.z, point2.x, point2.y, point2.z, point3.x, point3.y, point3.z);
      }
   }
   private void runTest(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2)
   {
      PointToLineUnProjector unProjector = new PointToLineUnProjector();
      
      unProjector.setLine(new Point2d(x0,y0), new Point2d(x1,y1), z0, z1);
      assertEquals(z2,unProjector.unProject(x2, y2),eps);
   }
   

}
