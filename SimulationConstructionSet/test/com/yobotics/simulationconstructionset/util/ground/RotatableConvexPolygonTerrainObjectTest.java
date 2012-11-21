package com.yobotics.simulationconstructionset.util.ground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Before;
import org.junit.Test;

import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.Plane3d;
import us.ihmc.utilities.test.JUnitTools;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;

public class RotatableConvexPolygonTerrainObjectTest
{
   private RotatableConvexPolygonTerrainObject flatTopFaceOctagon3d, inclinedTopFaceOctagon3d, inclinedTopFaceOctagon3dSecond;
   private Vector3d normalZVector, normalYZVector;
   private ConvexPolygon2d convexPolygon;
   private double[][] pointList;
   private double centroidHeight;
   private double epsilon = 1e-8;

   @Before
   public void setUp() throws Exception
   {
      normalZVector = new Vector3d(0.0, 0.0, 1.0);
      double[][] pointList =
      {
         {2.0, 1.0}, {1.0, 2.0}, {-1.0, 2.0}, {-2.0, 1.0}, {-2.0, -1.0}, {-1.0, -2.0}, {1.0, -2.0}, {2.0, -1.0}
      };
      this.pointList = pointList;
      convexPolygon = new ConvexPolygon2d(pointList);

      centroidHeight = 1.0;

      flatTopFaceOctagon3d = new RotatableConvexPolygonTerrainObject(normalZVector, convexPolygon, centroidHeight);

      normalYZVector = new Vector3d(0.0, 1.0, 1.0);

      inclinedTopFaceOctagon3d = new RotatableConvexPolygonTerrainObject(normalYZVector, convexPolygon, centroidHeight);

      inclinedTopFaceOctagon3dSecond = new RotatableConvexPolygonTerrainObject(normalYZVector, convexPolygon, 3.0);
   }

   @Test
   public void testHeightAt()
   {
      Point2d centroid = convexPolygon.getCentroidCopy();
      assertEquals(centroidHeight, flatTopFaceOctagon3d.heightAt(centroid.getX(), centroid.getY(), centroidHeight), epsilon);
      double expectedY;
      for (double[] point : pointList)
      {
         expectedY = centroidHeight - point[1];
         assertEquals(expectedY, inclinedTopFaceOctagon3d.heightAt(point[0], point[1], centroidHeight), epsilon);
      }

      assertEquals(0.0, flatTopFaceOctagon3d.heightAt(5.0, 5.0, 5.0), epsilon);
   }

   @Test
   public void testIsClose()
   {
      assertTrue(flatTopFaceOctagon3d.isClose(0.0, 0.0, 0.5));    // Point Inside
      assertFalse(flatTopFaceOctagon3d.isClose(0.0, 0.0, 1.5));    // Point On the top Outside
      assertFalse(flatTopFaceOctagon3d.isClose(2.0, 2.0, 1.5));    // Point Outside

      assertTrue(inclinedTopFaceOctagon3d.isClose(0.0, 1.0, centroidHeight));    // Point Outside
      assertTrue(inclinedTopFaceOctagon3d.isClose(0.0, -1.0, centroidHeight));    // Point Inside
      assertTrue(inclinedTopFaceOctagon3d.isClose(0.0, 0.0, centroidHeight));    // Point is on the center of the top surface

   }

   @Test
   public void testClosestIntersectionTo()
   {
      Point3d pointToPack = new Point3d();

      Point3d expectedPoint = new Point3d(2.0, 0.0, 0.5);
      flatTopFaceOctagon3d.closestIntersectionTo(3.0, 0.0, 0.5, pointToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint.set(-1.5, -1.5, 0.0);
      flatTopFaceOctagon3d.closestIntersectionTo(-4.0, -4.0, 0.0, pointToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint.set(0.0, 2.0, 0.9);
      flatTopFaceOctagon3d.closestIntersectionTo(0.0, 2.3, 0.9, pointToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint.set(1.0, -1.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionTo(1.0, -1.0, 1.1, pointToPack);    // Point on top surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint.set(1.5, -1.5, 1.0);
      flatTopFaceOctagon3d.closestIntersectionTo(1.5, -1.5, 1.0, pointToPack);    // Point on top surface edge
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint.set(0.0, 1.5, 1.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionTo(0.0, 2.0, 2.0, pointToPack);    // Point on top (inclined) surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint.set(0.0, 2.0, 0.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionTo(0.0, 3.0, 0.5, pointToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(1.0, 2.0, 0.5);
      flatTopFaceOctagon3d.closestIntersectionTo(1.1, 5.0, 0.5, pointToPack);    // Point on lateral edge
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(1.0, 2.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionTo(1.1, 5.0, 2.0, pointToPack);    // Point on the top corner
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(-1.0, -2.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionTo(-2.0, -4.0, 1.5, pointToPack);    // Point on the top corner
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(-2.0, 0.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionTo(-3.0, 0.0, 1.5, pointToPack);    // Point on the top edge
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      flatTopFaceOctagon3d.closestIntersectionTo(1.99, 0.0, 0.5, pointToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionTo(1.99, 0.0, 0.5, pointToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      flatTopFaceOctagon3d.closestIntersectionTo(1.5, 0.0, 0.5, pointToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionTo(1.5, 0.0, 0.5, pointToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(1.49, 0.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionTo(1.49, 0.0, 0.5, pointToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionTo(1.49, 0.0, 0.5, pointToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);

   }

   @Test
   public void testIsInsideTheFace()
   {
      Point3d faceCenter = new Point3d(1.0, 0.0, 0.0);
      Vector3d faceNormal = new Vector3d(1.0, 0.0, 0.0);
      Plane3d facePlane = new Plane3d(faceCenter, faceNormal);
      ArrayList<Point3d> faceVertices3d = new ArrayList<Point3d>();
      faceVertices3d.add(new Point3d(1.0, -2.0, 0.0));
      faceVertices3d.add(new Point3d(1.0, 0.0, -2.0));
      faceVertices3d.add(new Point3d(1.0, 2.0, 0.0));
      faceVertices3d.add(new Point3d(1.0, 0.0, 2.0));

      // Expected conversions v1=(2.0, 0.0) v2=(0.0, 2.0) v3=(-2.0, 0.0) v4=(0.0, -2.0)
      Point3d pointToCheck = new Point3d(1.0, -1.0, 0.0);    // Point inside (1.0, 0.0)
      assertTrue(flatTopFaceOctagon3d.isInsideTheFace(facePlane, faceVertices3d, pointToCheck));

      pointToCheck.set(1.0, -1.0, 1.0);    // Point on the edge (1.0, 1.0)
      assertTrue(flatTopFaceOctagon3d.isInsideTheFace(facePlane, faceVertices3d, pointToCheck));

      pointToCheck.set(1.0, 1.0, 2.0);    // Point outside (-1.0, -2.0)
      assertFalse(flatTopFaceOctagon3d.isInsideTheFace(facePlane, faceVertices3d, pointToCheck));
   }

   @Test
   public void testSurfaceNormalAt()
   {
      Vector3d normalToPack = new Vector3d();
      flatTopFaceOctagon3d.surfaceNormalAt(0.0, 0.0, 1.01, normalToPack);
      JUnitTools.assertTuple3dEquals(new Vector3d(0.0, 0.0, 1.0), normalToPack, 1e-4);

      flatTopFaceOctagon3d.surfaceNormalAt(0.0, 0.0, 0.99, normalToPack);
      JUnitTools.assertTuple3dEquals(new Vector3d(0.0, 0.0, 1.0), normalToPack, 1e-4);

      Vector3d expected = new Vector3d(1.0, 0.0, 0.5);
      expected.normalize();
      flatTopFaceOctagon3d.surfaceNormalAt(3.0, 0.0, 1.5, normalToPack);    // Point on top surface edge
      JUnitTools.assertTuple3dEquals(expected, normalToPack, epsilon);
   }

   @Test
   public void testClosestIntersectionAndNormalAt()
   {
      Point3d pointToPack = new Point3d();
      Vector3d normalToPack = new Vector3d();
      Vector3d expectedVector = new Vector3d();

      Point3d expectedPoint = new Point3d(2.0, 0.0, 0.5);
      expectedVector.set(1.0, 0.0, 0.0);
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(3.0, 0.0, 0.5, pointToPack, normalToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint.set(-1.5, -1.5, 0.0);
      expectedVector.set(-1.0, -1.0, 0.0);
      expectedVector.normalize();
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(-4.0, -4.0, 0.0, pointToPack, normalToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint.set(0.0, 2.0, 0.9);
      expectedVector.set(0.0, 1.0, 0.0);
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(0.0, 2.3, 0.9, pointToPack, normalToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint.set(1.0, -1.0, 1.0);
      expectedVector.set(0.0, 0.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.0, -1.0, 1.1, pointToPack, normalToPack);    // Point on top surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint.set(1.5, -1.5, 1.0);
      expectedVector.set(1.0, -1.0, 0.0);
      expectedVector.normalize();
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.5, -1.5, 1.0, pointToPack, normalToPack);    // Point on top surface edge
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint.set(0.0, 1.5, 1.5);
      expectedVector.set(0.0, 1.0, 1.0);
      expectedVector.normalize();
      inclinedTopFaceOctagon3dSecond.closestIntersectionAndNormalAt(0.0, 2.0, 2.0, pointToPack, normalToPack);    // Point on top (inclined) surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint.set(0.0, 2.0, 0.5);
      expectedVector.set(.0, 1.0, 0.0);
      inclinedTopFaceOctagon3dSecond.closestIntersectionAndNormalAt(0.0, 3.0, 0.5, pointToPack, normalToPack);    // Point on lateral surface
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(1.0, 2.0, 0.5);
      expectedVector.set(1.1, 5.0, 0.5);
      expectedVector.sub(expectedPoint);
      expectedVector.normalize();
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.1, 5.0, 0.5, pointToPack, normalToPack);    // Point on lateral edge
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(1.0, 2.0, 1.0);
      expectedVector.set(1.1, 5.0, 2.0);
      expectedVector.sub(expectedPoint);
      expectedVector.normalize();
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.1, 5.0, 2.0, pointToPack, normalToPack);    // Point on the top corner
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(-1.0, -2.0, 1.0);
      expectedVector.set(-2.0, -4.0, 1.5);
      expectedVector.sub(expectedPoint);
      expectedVector.normalize();
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(-2.0, -4.0, 1.5, pointToPack, normalToPack);    // Point on the top corner
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(-2.0, 0.0, 1.0);
      expectedVector.set(-3.0, 0.0, 1.5);
      expectedVector.sub(expectedPoint);
      expectedVector.normalize();
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(-3.0, 0.0, 1.5, pointToPack, normalToPack);    // Point on the top edge
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      expectedVector.set(1.0, 0.0, 0.0);
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.99, 0.0, 0.5, pointToPack, normalToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionAndNormalAt(1.99, 0.0, 0.5, pointToPack, normalToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.5, 0.0, 0.5, pointToPack, normalToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      inclinedTopFaceOctagon3dSecond.closestIntersectionAndNormalAt(1.5, 0.0, 0.5, pointToPack, normalToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(1.49, 0.0, 1.0);
      expectedVector.set(0.0, 0.0, 1.0);
      flatTopFaceOctagon3d.closestIntersectionAndNormalAt(1.49, 0.0, 0.5, pointToPack, normalToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);

      expectedPoint = new Point3d(2.0, 0.0, 0.5);
      expectedVector.set(1.0, 0.0, 0.0);
      inclinedTopFaceOctagon3dSecond.closestIntersectionAndNormalAt(1.49, 0.0, 0.5, pointToPack, normalToPack);    // Point just inside the rightmost vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);
      
      expectedPoint = new Point3d(0.0, -1.85, 4.85);
      expectedVector.set(0.0, 1.0, 1.0);
      expectedVector.normalize();
      inclinedTopFaceOctagon3dSecond.closestIntersectionAndNormalAt(0.0, -1.8, 4.9, pointToPack, normalToPack);    // Point just inside the 'back' (lowest y) vertical plane
      JUnitTools.assertTuple3dEquals(expectedPoint, pointToPack, epsilon);
      JUnitTools.assertTuple3dEquals(expectedVector, normalToPack, epsilon);
   }

   @Test
   public void testGetXMin()
   {
      assertEquals(-2.0, flatTopFaceOctagon3d.getXMin(), epsilon);
   }

   @Test
   public void testGetXMax()
   {
      assertEquals(2.0, flatTopFaceOctagon3d.getXMax(), epsilon);
   }

   @Test
   public void testGetYMin()
   {
      assertEquals(-2.0, flatTopFaceOctagon3d.getYMin(), epsilon);
   }

   @Test
   public void testGetYMax()
   {
      assertEquals(2.0, flatTopFaceOctagon3d.getYMax(), epsilon);
   }

   public void testSetupInEnvironment()
   {
      // Not an actual test, could be given @Test for visual confirmation though
      SimulationConstructionSet scs = new SimulationConstructionSet();
      scs.addStaticLinkGraphics(inclinedTopFaceOctagon3d.getLinkGraphics());

      scs.setGroundVisible(false);

      scs.startOnAThread();

      while (true)
      {
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e)
         {
         }

      }
   }

}
