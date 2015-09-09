package us.ihmc.ihmcPerception.depthData.collisionShapes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Point3d;

import org.junit.Test;

import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;

public class CollisionBoxTest
{
	@DeployableTestMethod(duration = 0.1)
   @Test(timeout = 30000)
   public void testInsideBoxPoints()
   {
      Random random = new Random(0101010101L);

      CollisionBox box = new CollisionBox(new RigidBodyTransform(), 5, 5, 5);
      for (int i = 0; i < 1000000; i++)
      {
         double x = -5.0 + random.nextDouble() * 10;
         double y = -5.0 + random.nextDouble() * 10;
         double z = -5.0 + random.nextDouble() * 10;

         Point3d point = new Point3d(x, y, z);
         assertTrue(box.contains(point));
      }
   }

	@DeployableTestMethod(duration = 0.1)
   @Test(timeout = 30000)
   public void testOutsideBoxPoints()
   {
      Random random = new Random(1098551L);

      CollisionBox box = new CollisionBox(new RigidBodyTransform(), 5, 5, 5);
      for (int i = 0; i < 1000000; i++)
      {
         double x = (random.nextBoolean() ? -1 : 1) * (5.0 + random.nextDouble() * 10);
         double y = (random.nextBoolean() ? -1 : 1) * (5.0 + random.nextDouble() * 10);
         double z = (random.nextBoolean() ? -1 : 1) * (5.0 + random.nextDouble() * 10);

         Point3d point = new Point3d(x, y, z);
         assertFalse(box.contains(point));
      }
   }

	@DeployableTestMethod(duration = 0.2)
   @Test(timeout = 30000)
   public void compareWithJMEBox()
   {
      Random random = new Random(1098551L);

      CollisionBox box = new CollisionBox(new RigidBodyTransform(), 5, 5, 5);
      Mesh mesh = new Box(5, 5, 5);
      mesh.updateBound();
      for (int i = 0; i < 1000000; i++)
      {
         double x = -10.0 + 20.0 * random.nextDouble();
         double y = -10.0 + 20.0 * random.nextDouble();
         double z = -10.0 + 20.0 * random.nextDouble();
         Point3d point = new Point3d(x, y, z);

         assertTrue(box.contains(point) == mesh.getBound().contains(new Vector3f((float) x, (float) y, (float) z)));
      }
   }
}
