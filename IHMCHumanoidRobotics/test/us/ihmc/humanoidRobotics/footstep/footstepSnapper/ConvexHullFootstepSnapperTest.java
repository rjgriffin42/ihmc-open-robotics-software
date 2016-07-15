package us.ihmc.humanoidRobotics.footstep.footstepSnapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;

import org.junit.Test;

import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.humanoidRobotics.footstep.footstepSnapper.ConvexHullFootstepSnapper;
import us.ihmc.humanoidRobotics.footstep.footstepSnapper.FootstepSnappingParameters;
import us.ihmc.humanoidRobotics.footstep.footstepSnapper.GenericFootstepSnappingParameters;
import us.ihmc.humanoidRobotics.footstep.footstepSnapper.SimpleFootstepValueFunction;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.random.RandomTools;


/**
 * Created by agrabertilton on 1/20/15.
 */
public class ConvexHullFootstepSnapperTest
{

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout = 150000)
	public void testBasicCropping()
	{
      FootstepSnappingParameters snappingParameters = new GenericFootstepSnappingParameters();
      ConvexHullFootstepSnapper footstepSnapper = new ConvexHullFootstepSnapper(new SimpleFootstepValueFunction(snappingParameters), snappingParameters);
      List<Point2d> pointsToCrop = new ArrayList<Point2d>();
      pointsToCrop.add(new Point2d(1,1));
      pointsToCrop.add(new Point2d(-1,1));
      pointsToCrop.add(new Point2d(-1,-1));
      pointsToCrop.add(new Point2d(1,-1));
      pointsToCrop.add(new Point2d(1.1,0));

      List<Point2d> finalPoints = footstepSnapper.reduceListOfPointsByArea(pointsToCrop, 4);
      assertTrue(finalPoints.size() == 4.0);
      ConvexPolygon2d endPolygon = new ConvexPolygon2d(finalPoints);
      assertEquals(4.0, endPolygon.getArea(), 1e-15);
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout = 150000)
	public void testRandomCropping()
	{
      FootstepSnappingParameters snappingParameters = new GenericFootstepSnappingParameters();
      ConvexHullFootstepSnapper footstepSnapper = new ConvexHullFootstepSnapper(new SimpleFootstepValueFunction(snappingParameters), snappingParameters);
      List<Point2d> pointsToCrop = new ArrayList<Point2d>();
      Random random = new Random(82368L);
      double maxX = 10;
      double maxY = 10;
      int numPoints = 100;
      for (int i = 0; i < numPoints; i++){
         pointsToCrop.add(RandomTools.generateRandomPoint2d(random, maxX, maxY));
      }

      ConvexPolygon2d startPolygon = new ConvexPolygon2d(pointsToCrop);
      startPolygon.update();
      double startArea = startPolygon.getArea();

      ConvexPolygon2d intermediateStepPolygon = new ConvexPolygon2d(footstepSnapper.reduceListOfPointsByArea(pointsToCrop, Math.max(4, startPolygon.getNumberOfVertices() / 2)));
      intermediateStepPolygon.update();
      double intermediateStepArea = intermediateStepPolygon.getArea();
      assertTrue(intermediateStepArea <= startArea);
      assertTrue(intermediateStepPolygon.getNumberOfVertices() <= Math.max(4, startPolygon.getNumberOfVertices()));

      ConvexPolygon2d endPolygon = new ConvexPolygon2d(footstepSnapper.reduceListOfPointsByArea(pointsToCrop, 4));
      endPolygon.update();
      double endArea = endPolygon.getArea();
      assertTrue(endArea <= startArea);
      assertTrue(endArea <= intermediateStepArea);
      assertTrue(endPolygon.getNumberOfVertices() <= 4);
   }
}
