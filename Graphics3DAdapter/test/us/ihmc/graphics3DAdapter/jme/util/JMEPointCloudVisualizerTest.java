package us.ihmc.graphics3DAdapter.jme.util;

import java.util.Arrays;
import java.util.Random;

import javax.vecmath.Point3f;

import org.junit.Test;

import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.code.agileTesting.BambooPlanType;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.BambooPlan;

@BambooPlan(planType={BambooPlanType.UI})
public class JMEPointCloudVisualizerTest
{
   public static void main(String[] args)
   {
      new JMEPointCloudVisualizerTest().testJMEPointCloudVisualizer();
   }

	@AverageDuration
	@Test(timeout=300000)
   public void testJMEPointCloudVisualizer()
   {
      JMEPointCloudVisualizer jmePointCloudVisualizer = new JMEPointCloudVisualizer();
      
      Random random = new Random();
      
      Point3f[] randomPoint3fCloudArray = RandomTools.generateRandomPoint3fCloud(random, 10000, new Point3f(), new Point3f(5.0f, 5.0f, 5.0f));
      
      jmePointCloudVisualizer.addPointCloud(Arrays.asList(randomPoint3fCloudArray));
   }
}
