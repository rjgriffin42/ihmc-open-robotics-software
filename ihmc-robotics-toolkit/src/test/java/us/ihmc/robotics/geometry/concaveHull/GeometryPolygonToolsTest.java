package us.ihmc.robotics.geometry.concaveHull;

import org.junit.jupiter.api.Test;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryPolygonTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;

import java.util.ArrayList;
import java.util.List;

import static us.ihmc.robotics.Assert.assertEquals;

public class GeometryPolygonToolsTest
{
   @Test
   public void testConcavePolygonAreaAndCentroid()
   {
      ConvexPolygon2D polygon1 = new ConvexPolygon2D();
      ConvexPolygon2D polygon2 = new ConvexPolygon2D();

      polygon1.addVertex(-1.0, 1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.update();

      polygon2.addVertex(1.0, 0.5);
      polygon2.addVertex(1.0, -0.5);
      polygon2.addVertex(2.0, -0.5);
      polygon2.addVertex(2.0, 0.5);
      polygon2.update();

      List<Point2DReadOnly> concaveHull = new ArrayList<>();
      concaveHull.add(new Point2D(-1.0, 1.0));
      concaveHull.add(new Point2D(1.0, 1.0));
      concaveHull.add(new Point2D(1.0, 0.5));
      concaveHull.add(new Point2D(2.0, 0.5));
      concaveHull.add(new Point2D(2.0, -0.5));
      concaveHull.add(new Point2D(1.0, -0.5));
      concaveHull.add(new Point2D(1.0, -1.0));
      concaveHull.add(new Point2D(-1.0, -1.0));

      double totalArea = polygon1.getArea() + polygon2.getArea();
      Point2D totalCentroid = new Point2D();
      Point2D scaledCentroid1 = new Point2D();
      Point2D scaledCentroid2 = new Point2D();
      scaledCentroid1.set(polygon1.getCentroid());
      scaledCentroid1.scale(polygon1.getArea() / totalArea);
      scaledCentroid2.set(polygon2.getCentroid());
      scaledCentroid2.scale(polygon2.getArea() / totalArea);
      totalCentroid.add(scaledCentroid1, scaledCentroid2);

      Point2D centroid = new Point2D();
      double actualArea = EuclidGeometryPolygonTools.computeConvexPolygon2DArea(concaveHull, concaveHull.size(), true, centroid);

      assertEquals(totalArea, actualArea, 1e-7);
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(totalCentroid, centroid, 1e-7);
   }
}
