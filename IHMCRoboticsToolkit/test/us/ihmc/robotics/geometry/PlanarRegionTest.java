package us.ihmc.robotics.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.testing.JUnitTools;
import us.ihmc.tools.testing.MutationTestingTools;

public class PlanarRegionTest
{
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testWithLShapedPlanarRegionWithIdentityTransform()
   {
      // polygons forming a L-shaped region.
      List<ConvexPolygon2d> regionConvexPolygons = new ArrayList<>();
      ConvexPolygon2d polygon1 = new ConvexPolygon2d();
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.addVertex(-1.0, 1.0);
      ConvexPolygon2d polygon2 = new ConvexPolygon2d();
      polygon2.addVertex(3.0, 1.0);
      polygon2.addVertex(3.0, -1.0);
      polygon2.addVertex(1.0, -1.0);
      polygon2.addVertex(1.0, 1.0);
      ConvexPolygon2d polygon3 = new ConvexPolygon2d();
      polygon3.addVertex(1.0, 3.0);
      polygon3.addVertex(1.0, 1.0);
      polygon3.addVertex(-1.0, 1.0);
      polygon3.addVertex(-1.0, 3.0);

      regionConvexPolygons.add(polygon1);
      regionConvexPolygons.add(polygon2);
      regionConvexPolygons.add(polygon3);
      for (ConvexPolygon2d convexPolygon : regionConvexPolygons)
         convexPolygon.update();

      RigidBodyTransform regionTransform = new RigidBodyTransform();
      PlanarRegion planarRegion = new PlanarRegion(regionTransform, regionConvexPolygons);

      assertEquals("Wrong number of convex polygons in the region.", 3, planarRegion.getNumberOfConvexPolygons());
      for (int i = 0; i < 3; i++)
         assertTrue("Unexpected region polygon.", regionConvexPolygons.get(i).epsilonEquals(planarRegion.getConvexPolygon(i), 1.0e-10));

      Vector3d actualNormal = new Vector3d();
      planarRegion.getNormal(actualNormal);
      JUnitTools.assertVector3dEquals("Wrong region normal.", new Vector3d(0.0, 0.0, 1.0), actualNormal, 1.0e-10);
      Point3d actualOrigin = new Point3d();
      planarRegion.getPointInRegion(actualOrigin);
      JUnitTools.assertPoint3dEquals("Wrong region origin.", new Point3d(), actualOrigin, 1.0e-10);
      RigidBodyTransform actualTransform = new RigidBodyTransform();
      planarRegion.getTransformToWorld(actualTransform);
      assertTrue("Wrong region transform to world.", regionTransform.epsilonEquals(actualTransform, 1.0e-10));

      Point2d point2d = new Point2d();

      // Do a bunch of trivial queries with isPointInside(Point2d) method.
      point2d.set(0.0, 0.0);
      assertTrue(planarRegion.isPointInside(point2d));
      point2d.set(2.0, 0.0);
      assertTrue(planarRegion.isPointInside(point2d));
      point2d.set(0.0, 2.0);
      assertTrue(planarRegion.isPointInside(point2d));
      point2d.set(2.0, 2.0);
      assertFalse(planarRegion.isPointInside(point2d));

      Point3d point3d = new Point3d();
      double maximumOrthogonalDistance = 1.0e-3;
      // Do a bunch of trivial queries with isPointInside(Point3d, double) method. Point in plane
      point3d.set(0.0, 0.0, 0.0);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(2.0, 0.0, 0.0);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 2.0, 0.0);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(2.0, 2.0, 0.0);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      // Do a bunch of trivial queries with isPointInside(Point3d, double) method. Point below plane
      point3d.set(0.0, 0.0, -0.5 * maximumOrthogonalDistance);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(2.0, 0.0, -0.5 * maximumOrthogonalDistance);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 2.0, -0.5 * maximumOrthogonalDistance);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 0.0, -1.5 * maximumOrthogonalDistance);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(2.0, 0.0, -1.5 * maximumOrthogonalDistance);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 2.0, -1.5 * maximumOrthogonalDistance);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      // Do a bunch of trivial queries with isPointInside(Point3d, double) method. Point above plane
      point3d.set(0.0, 0.0, 0.5 * maximumOrthogonalDistance);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(2.0, 0.0, 0.5 * maximumOrthogonalDistance);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 2.0, 0.5 * maximumOrthogonalDistance);
      assertTrue(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 0.0, 1.5 * maximumOrthogonalDistance);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(2.0, 0.0, 1.5 * maximumOrthogonalDistance);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));
      point3d.set(0.0, 2.0, 1.5 * maximumOrthogonalDistance);
      assertFalse(planarRegion.isPointInside(point3d, maximumOrthogonalDistance));

      // Do a bunch of trivial queries with isPointInsideByProjectionOntoXYPlane(double, double) method.
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(0.0, 0.0));
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(2.0, 0.0));
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(0.0, 2.0));
      assertFalse(planarRegion.isPointInsideByProjectionOntoXYPlane(2.0, 2.0));

      // Do a bunch of trivial queries with isPointInsideByProjectionOntoXYPlane(Point2d) method.
      point2d.set(0.0, 0.0);
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d));
      point2d.set(2.0, 0.0);
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d));
      point2d.set(0.0, 2.0);
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d));
      point2d.set(2.0, 2.0);
      assertFalse(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d));

      // Do a bunch of trivial queries with isPointInsideByProjectionOntoXYPlane(Point3d) method.
      point3d.set(0.0, 0.0, Double.POSITIVE_INFINITY);
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d));
      point3d.set(2.0, 0.0, Double.POSITIVE_INFINITY);
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d));
      point3d.set(0.0, 2.0, Double.POSITIVE_INFINITY);
      assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d));
      point3d.set(2.0, 2.0, Double.POSITIVE_INFINITY);
      assertFalse(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d));

      ConvexPolygon2d convexPolygon = new ConvexPolygon2d();
      convexPolygon.addVertex(0.2, 0.2);
      convexPolygon.addVertex(0.2, -0.2);
      convexPolygon.addVertex(-0.2, -0.2);
      convexPolygon.addVertex(-0.2, 0.2);
      convexPolygon.update();

      // Do a bunch of trivial queries with isPolygonIntersecting(ConvexPolygon2d) method.
      assertTrue(planarRegion.isPolygonIntersecting(convexPolygon));
      assertTrue(planarRegion.isPolygonIntersecting(translateConvexPolygon(2.0, 0.0, convexPolygon)));
      assertTrue(planarRegion.isPolygonIntersecting(translateConvexPolygon(0.0, 2.0, convexPolygon)));
      assertFalse(planarRegion.isPolygonIntersecting(translateConvexPolygon(2.0, 2.0, convexPolygon)));
      assertFalse(planarRegion.isPolygonIntersecting(translateConvexPolygon(1.21, 1.21, convexPolygon)));
      assertTrue(planarRegion.isPolygonIntersecting(translateConvexPolygon(1.09, 1.09, convexPolygon)));
      assertTrue(planarRegion.isPolygonIntersecting(translateConvexPolygon(1.21, 1.09, convexPolygon)));
      assertTrue(planarRegion.isPolygonIntersecting(translateConvexPolygon(1.09, 1.21, convexPolygon)));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testWithLShapedPlanarRegionWithRandomTransform()
   {
      Random random = new Random(42L);

      // polygons forming a L-shaped region.
      List<ConvexPolygon2d> regionConvexPolygons = new ArrayList<>();
      ConvexPolygon2d polygon1 = new ConvexPolygon2d();
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.addVertex(-1.0, 1.0);
      ConvexPolygon2d polygon2 = new ConvexPolygon2d();
      polygon2.addVertex(3.0, 1.0);
      polygon2.addVertex(3.0, -1.0);
      polygon2.addVertex(1.0, -1.0);
      polygon2.addVertex(1.0, 1.0);
      ConvexPolygon2d polygon3 = new ConvexPolygon2d();
      polygon3.addVertex(1.0, 3.0);
      polygon3.addVertex(1.0, 1.0);
      polygon3.addVertex(-1.0, 1.0);
      polygon3.addVertex(-1.0, 3.0);

      regionConvexPolygons.add(polygon1);
      regionConvexPolygons.add(polygon2);
      regionConvexPolygons.add(polygon3);
      for (ConvexPolygon2d convexPolygon : regionConvexPolygons)
         convexPolygon.update();

      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

      for (int iteration = 0; iteration < 10; iteration++)
      {
         Quat4d orientation = RandomTools.generateRandomQuaternion(random, Math.toRadians(45.0));
         Vector3d translation = RandomTools.generateRandomVector(random, 10.0);
         RigidBodyTransform regionTransform = new RigidBodyTransform(orientation, translation);
         ReferenceFrame localFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("local", worldFrame, regionTransform);
         PlanarRegion planarRegion = new PlanarRegion(regionTransform, regionConvexPolygons);

         assertEquals("Wrong number of convex polygons in the region.", 3, planarRegion.getNumberOfConvexPolygons());
         for (int i = 0; i < 3; i++)
            assertTrue("Unexpected region polygon.", regionConvexPolygons.get(i).epsilonEquals(planarRegion.getConvexPolygon(i), 1.0e-10));

         Vector3d expectedNormal = new Vector3d(0.0, 0.0, 1.0);
         regionTransform.transform(expectedNormal);
         Vector3d actualNormal = new Vector3d();
         planarRegion.getNormal(actualNormal);
         JUnitTools.assertVector3dEquals("Wrong region normal.", expectedNormal, actualNormal, 1.0e-10);
         Point3d expectedOrigin = new Point3d();
         regionTransform.transform(expectedOrigin);
         Point3d actualOrigin = new Point3d();
         planarRegion.getPointInRegion(actualOrigin);
         JUnitTools.assertPoint3dEquals("Wrong region origin.", expectedOrigin, actualOrigin, 1.0e-10);
         RigidBodyTransform actualTransform = new RigidBodyTransform();
         planarRegion.getTransformToWorld(actualTransform);
         assertTrue("Wrong region transform to world.", regionTransform.epsilonEquals(actualTransform, 1.0e-10));

         FramePoint2d point2d = new FramePoint2d();

         // Do a bunch of trivial queries with isPointInside(Point2d) method.
         point2d.setIncludingFrame(localFrame, 0.0, 0.0);
         assertTrue(planarRegion.isPointInside(point2d.getPoint()));
         point2d.setIncludingFrame(localFrame, 2.0, 0.0);
         assertTrue(planarRegion.isPointInside(point2d.getPoint()));
         point2d.setIncludingFrame(localFrame, 0.0, 2.0);
         assertTrue(planarRegion.isPointInside(point2d.getPoint()));
         point2d.setIncludingFrame(localFrame, 2.0, 2.0);
         assertFalse(planarRegion.isPointInside(point2d.getPoint()));

         FramePoint point3d = new FramePoint();
         double maximumOrthogonalDistance = 1.0e-3;
         // Do a bunch of trivial queries with isPointInside(Point3d, double) method. Point in plane
         point3d.setIncludingFrame(localFrame, 0.0, 0.0, 0.0);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 2.0, 0.0, 0.0);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 2.0, 0.0);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 2.0, 2.0, 0.0);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         // Do a bunch of trivial queries with isPointInside(Point3d, double) method. Point below plane
         point3d.setIncludingFrame(localFrame, 0.0, 0.0, -0.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 2.0, 0.0, -0.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 2.0, -0.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 0.0, -1.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 2.0, 0.0, -1.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 2.0, -1.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         // Do a bunch of trivial queries with isPointInside(Point3d, double) method. Point above plane
         point3d.setIncludingFrame(localFrame, 0.0, 0.0, 0.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 2.0, 0.0, 0.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 2.0, 0.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertTrue(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 0.0, 1.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 2.0, 0.0, 1.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));
         point3d.setIncludingFrame(localFrame, 0.0, 2.0, 1.5 * maximumOrthogonalDistance);
         point3d.changeFrame(worldFrame);
         assertFalse(planarRegion.isPointInside(point3d.getPoint(), maximumOrthogonalDistance));

         // Do a bunch of trivial queries with isPointInsideByProjectionOntoXYPlane(double, double) method.
         point2d.setIncludingFrame(localFrame, 0.0, 0.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getX(), point2d.getY()));
         point2d.setIncludingFrame(localFrame, 2.0, 0.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getX(), point2d.getY()));
         point2d.setIncludingFrame(localFrame, 0.0, 2.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getX(), point2d.getY()));
         point2d.setIncludingFrame(localFrame, 2.0, 2.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertFalse(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getX(), point2d.getY()));

         // Do a bunch of trivial queries with isPointInsideByProjectionOntoXYPlane(Point2d) method.
         point2d.setIncludingFrame(localFrame, 0.0, 0.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getPoint()));
         point2d.setIncludingFrame(localFrame, 2.0, 0.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getPoint()));
         point2d.setIncludingFrame(localFrame, 0.0, 2.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getPoint()));
         point2d.setIncludingFrame(localFrame, 2.0, 2.0);
         point2d.changeFrameAndProjectToXYPlane(worldFrame);
         assertFalse(planarRegion.isPointInsideByProjectionOntoXYPlane(point2d.getPoint()));

         // Do a bunch of trivial queries with isPointInsideByProjectionOntoXYPlane(Point3d) method.
         point3d.setIncludingFrame(localFrame, 0.0, 0.0, 0.0);
         point3d.changeFrame(worldFrame);
         point3d.setZ(Double.POSITIVE_INFINITY);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d.getPoint()));
         point3d.setIncludingFrame(localFrame, 2.0, 0.0, 0.0);
         point3d.changeFrame(worldFrame);
         point3d.setZ(Double.POSITIVE_INFINITY);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d.getPoint()));
         point3d.setIncludingFrame(localFrame, 0.0, 2.0, 0.0);
         point3d.changeFrame(worldFrame);
         point3d.setZ(Double.POSITIVE_INFINITY);
         assertTrue(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d.getPoint()));
         point3d.setIncludingFrame(localFrame, 2.0, 2.0, 0.0);
         point3d.changeFrame(worldFrame);
         point3d.setZ(Double.POSITIVE_INFINITY);
         assertFalse(planarRegion.isPointInsideByProjectionOntoXYPlane(point3d.getPoint()));

         ConvexPolygon2d convexPolygon = new ConvexPolygon2d();
         convexPolygon.addVertex(0.2, 0.2);
         convexPolygon.addVertex(0.2, -0.2);
         convexPolygon.addVertex(-0.2, -0.2);
         convexPolygon.addVertex(-0.2, 0.2);
         convexPolygon.update();

         // Do a bunch of trivial queries with isPolygonIntersecting(ConvexPolygon2d) method.
         assertTrue(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, convexPolygon)));
         assertTrue(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(2.0, 0.0, convexPolygon))));
         assertTrue(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(0.0, 2.0, convexPolygon))));
         assertFalse(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(2.0, 2.0, convexPolygon))));
         assertFalse(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(1.21, 1.21, convexPolygon))));
         assertTrue(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(1.09, 1.09, convexPolygon))));
         assertTrue(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(1.21, 1.09, convexPolygon))));
         assertTrue(planarRegion.isPolygonIntersecting(transformConvexPolygon(regionTransform, translateConvexPolygon(1.09, 1.21, convexPolygon))));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPlaneZGivenXY()
   {
      ConvexPolygon2d convexPolygon2d = new ConvexPolygon2d();
      convexPolygon2d.addVertex(1.0, 1.0);
      convexPolygon2d.addVertex(-1.0, 1.0);
      convexPolygon2d.addVertex(-1.0, -1.0);
      convexPolygon2d.addVertex(1.0, -1.0);
      convexPolygon2d.update();
      ArrayList<ConvexPolygon2d> polygonList = new ArrayList<>();
      polygonList.add(convexPolygon2d);
      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      PlanarRegion planarRegion = new PlanarRegion(transformToWorld, polygonList);

      double xWorld = 0.0;
      double yWorld = 0.0;
      double planeZGivenXY = planarRegion.getPlaneZGivenXY(xWorld, yWorld);

      assertEquals(0.0, planeZGivenXY, 1e-7);

      transformToWorld.setTranslation(1.0, 2.0, 3.0);
      planarRegion = new PlanarRegion(transformToWorld, polygonList);
      planeZGivenXY = planarRegion.getPlaneZGivenXY(xWorld, yWorld);

      assertEquals(3.0, planeZGivenXY, 1e-7);

      double angle = Math.PI/4.0;
      transformToWorld.setRotationEulerAndZeroTranslation(0.0, angle, 0.0);
      planarRegion = new PlanarRegion(transformToWorld, polygonList);
      xWorld = 1.3;
      planeZGivenXY = planarRegion.getPlaneZGivenXY(xWorld, yWorld);

      assertEquals(-xWorld * Math.tan(angle), planeZGivenXY, 1e-7);
      assertTrue(planarRegion.isPointInside(new Point3d(0.0, 0.0, 0.0), 1e-7));

      // Try really close to 90 degrees
      angle = Math.PI/2.0 - 0.001;
      transformToWorld.setRotationEulerAndZeroTranslation(0.0, angle, 0.0);
      planarRegion = new PlanarRegion(transformToWorld, polygonList);
      xWorld = 1.3;
      planeZGivenXY = planarRegion.getPlaneZGivenXY(xWorld, yWorld);

      assertEquals(-xWorld * Math.tan(angle), planeZGivenXY, 1e-7);
      assertTrue(planarRegion.isPointInside(new Point3d(0.0, 0.0, 0.0), 1e-7));

      // Exactly 90 degrees.
      angle = Math.PI/2.0 - 0.1;
      transformToWorld.setRotationEulerAndZeroTranslation(0.0, angle, 0.0);
      planarRegion = new PlanarRegion(transformToWorld, polygonList);
      xWorld = 1.3;
      planeZGivenXY = planarRegion.getPlaneZGivenXY(xWorld, yWorld);

      // With numerical roundoff, Math.tan(Math.PI/2.0) is not NaN:
      double tangent = Math.tan(angle);
      boolean planeZGivenXYIsNaN = Double.isNaN(planeZGivenXY);
      boolean valueMatchesComputed = (Math.abs(planeZGivenXY - -tangent * xWorld) < 1e-7);
      assertFalse(planeZGivenXYIsNaN);
      assertTrue(valueMatchesComputed);
      assertTrue(planarRegion.isPointInside(new Point3d(0.0, 0.0, 0.0), 1e-7));

      // If we set the transform to exactly z axis having no zWorld component (exactly 90 degree rotation about y in this case), then we do get NaN.
      // However (0, 0, 0) should still be inside. As should (0, 0, 0.5)
      transformToWorld.set(new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0});
      planarRegion = new PlanarRegion(transformToWorld, polygonList);
      xWorld = 1.3;
      planeZGivenXY = planarRegion.getPlaneZGivenXY(xWorld, yWorld);

      planeZGivenXYIsNaN = Double.isNaN(planeZGivenXY);
      valueMatchesComputed = (Math.abs(planeZGivenXY - -tangent * xWorld) < 1e-7);
      assertTrue(planeZGivenXYIsNaN);
      assertFalse(valueMatchesComputed);
      assertTrue(planarRegion.isPointInside(new Point3d(0.0, 0.0, 0.0), 1e-7));
      assertTrue(planarRegion.isPointInside(new Point3d(0.0, 0.0, 0.5), 1e-7));
   }

   static ConvexPolygon2d translateConvexPolygon(double xTranslation, double yTranslation, ConvexPolygon2d convexPolygon)
   {
      Vector2d translation = new Vector2d(xTranslation, yTranslation);
      return ConvexPolygon2dCalculator.translatePolygonCopy(translation, convexPolygon);
   }

   private static ConvexPolygon2d transformConvexPolygon(RigidBodyTransform transform, ConvexPolygon2d convexPolygon)
   {
      ConvexPolygon2d transformedConvexPolygon = new ConvexPolygon2d(convexPolygon);
      transformedConvexPolygon.applyTransformAndProjectToXYPlane(transform);
      return transformedConvexPolygon;
   }

   public static void main(String[] args)
   {
      String targetTests = PlanarRegionTest.class.getName();
      String targetClassesInSamePackage = PlanarRegion.class.getName();
      MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClassesInSamePackage);
   }
}
