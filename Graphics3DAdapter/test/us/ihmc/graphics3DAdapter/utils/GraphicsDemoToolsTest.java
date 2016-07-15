package us.ihmc.graphics3DAdapter.utils;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.tools.testing.TestPlanTarget;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

@DeployableTestClass(targets={TestPlanTarget.UI})
public class GraphicsDemoToolsTest
{

	@DeployableTestMethod(estimatedDuration = 0.6)
	@Test(timeout = 30000)
   public void testCreatePointCloud()
   {
      List<Point3d> worldPoints = new ArrayList<Point3d>();
      
      for (int i = 0; i < 1000; i++)
      {
         worldPoints.add(new Point3d(1.0, 1.0, 1.0));
      }
      
      Graphics3DNode pointCloudNode = GraphicsDemoTools.createPointCloud("PointCloud", worldPoints, 0.001, YoAppearance.Green());
      
      assertNotNull("Point cloud node is null. ", pointCloudNode);
   }
}
