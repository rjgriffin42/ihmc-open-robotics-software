package us.ihmc.simulationconstructionset.physics.collision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.physics.CollisionHandler;
import us.ihmc.simulationconstructionset.physics.CollisionShape;
import us.ihmc.simulationconstructionset.physics.CollisionShapeDescription;
import us.ihmc.simulationconstructionset.physics.CollisionShapeFactory;
import us.ihmc.simulationconstructionset.physics.Contacts;
import us.ihmc.simulationconstructionset.physics.ScsCollisionDetector;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

/**
 * Tests compliance to the {@link us.ihmc.simulationconstructionset.physics.ScsCollisionDetector}
 *
 */
public abstract class SCSCollisionDetectorTest
{
   public abstract ScsCollisionDetector createCollisionDetector();

   /**
    * Make a small object and see if it detects the collision correctly.  Small objects aren't already handled correctly
    */
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testSmallBox()
   {
      double epsilon = 0.001;
      double r = 0.5; // box width = 1cm

      ScsCollisionDetector collisionDetector = createCollisionDetector();
      CollisionDetectionResult result = new CollisionDetectionResult();

      FloatingJoint cubeA = cube(collisionDetector, "A", 10, r, r, r);
      FloatingJoint cubeB = cube(collisionDetector, "B", 10, r, r, r);

      // Barely miss
      setPosition(cubeA, 2.0 * r + epsilon, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertEquals(0, result.getNumberOfCollisions());
      result.clear();

      // Barely intersect
      setPosition(cubeA, 2.0 * r - epsilon, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertTrue(result.getNumberOfCollisions() > 0);
      result.clear();

      // Really Miss
      setPosition(cubeA, 20.0 * r, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertTrue(result.getNumberOfCollisions() == 0);
      result.clear();

      // Barely miss
      setPosition(cubeA, 2.0 * r + 0.015, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertEquals(0, result.getNumberOfCollisions());
      result.clear();

      // Barely intersect
      setPosition(cubeA, 2.0 * r - epsilon, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertTrue(result.getNumberOfCollisions() > 0);
      result.clear();

      // Barely miss. (This sometimes fails after a barely intersect for some reason. Maybe collision thing trying to be smart with max velocity stuff.)
      setPosition(cubeA, 2.0 * r + 0.015, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertEquals(0, result.getNumberOfCollisions());
      result.clear();
   }

   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testUnitBox()
   {
      // add a bit of separation to ensure they don't collide
      double epsilon = 0.001;

      ScsCollisionDetector collisionDetector = createCollisionDetector();
      CollisionDetectionResult result = new CollisionDetectionResult();

      FloatingJoint cubeA = cube(collisionDetector, "A", 10, 0.5, 0.5, 0.5);
      FloatingJoint cubeB = cube(collisionDetector, "B", 10, 0.5, 0.5, 0.5);

      setPosition(cubeA, 0.0, 0.0, 0.0);
      setPosition(cubeB, 1.0 + epsilon, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertEquals(0, result.getNumberOfCollisions());
      result.clear();

      setPosition(cubeA, 0.0, 0.0, 0.0);
      setPosition(cubeB, 1.0 - epsilon, 0.0, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertTrue(result.getNumberOfCollisions() > 0);
      result.clear();

      setPosition(cubeA, 0.0, 0.0, 0.0);
      setPosition(cubeB, 0.0, 1.0 + epsilon, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertEquals(0, result.getNumberOfCollisions());
      result.clear();

      setPosition(cubeA, 0.0, 0.0, 0.0);
      setPosition(cubeB, 0.0, 1.0 - epsilon, 0.0);
      collisionDetector.performCollisionDetection(result);
      assertTrue(result.getNumberOfCollisions() > 0);
      result.clear();

      setPosition(cubeA, 0.0, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 1.0 + epsilon);
      collisionDetector.performCollisionDetection(result);
      assertEquals(0, result.getNumberOfCollisions());
      result.clear();

      setPosition(cubeA, 0.0, 0.0, 0.0);
      setPosition(cubeB, 0.0, 0.0, 1.0 - epsilon);
      collisionDetector.performCollisionDetection(result);
      assertTrue(result.getNumberOfCollisions() > 0);
      result.clear();
   }


   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testBoxCloseButNoCollisions()
   {
      ScsCollisionDetector collisionDetector = createCollisionDetector();

      FloatingJoint cubeA = cube(collisionDetector, "A", 10, 0.5, 1.0, 1.5);
      FloatingJoint cubeB = cube(collisionDetector, "B", 10, 0.75, 1.2, 1.7);

      double a[] = new double[] { 0.5 + 0.75, 1.0 + 1.2, 1.5 + 1.7 };

      // add a bit of separation to ensure they don't collide
      double tau = 0.001;

      // should just barely not intersect
      for (int i = 0; i < 3; i++)
      {
         double Tx, Ty, Tz;
         Tx = Ty = Tz = 0.0;

         if (i == 0)
            Tx = a[i] / 2.0 + tau;
         if (i == 1)
            Ty = a[i] / 2.0 + tau;
         if (i == 2)
            Tz = a[i] / 2.0 + tau;

         cubeA.setPosition(Tx, Ty, Tz);
         cubeB.setPosition(-Tx, -Ty, -Tz);

         cubeA.getRobot().update();
         cubeB.getRobot().update();

         RigidBodyTransform transformToWorld = new RigidBodyTransform();
         cubeA.getTransformToWorld(transformToWorld);

         CollisionDetectionResult result = new CollisionDetectionResult();
         collisionDetector.performCollisionDetection(result);

         assertEquals(0, result.getNumberOfCollisions());
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testBoxBarelyCollisions()
   {
      ScsCollisionDetector collisionDetector = createCollisionDetector();

      FloatingJoint cubeA = cube(collisionDetector, "A", 10, 0.5, 1.0, 1.5);
      FloatingJoint cubeB = cube(collisionDetector, "B", 10, 0.75, 1.2, 1.7);

      double a[] = new double[] { 0.5 + 0.75, 1.0 + 1.2, 1.5 + 1.7 };

      // add a bit of separation to ensure they don't collide
      double tau = -0.001;

      // should just barely not intersect
      for (int i = 0; i < 3; i++)
      {
         double Tx, Ty, Tz;
         Tx = Ty = Tz = 0.0;

         if (i == 0)
            Tx = a[i] / 2.0 + tau;
         if (i == 1)
            Ty = a[i] / 2.0 + tau;
         if (i == 2)
            Tz = a[i] / 2.0 + tau;

         cubeA.setPosition(Tx, Ty, Tz);
         cubeB.setPosition(-Tx, -Ty, -Tz);

         cubeA.getRobot().update();
         cubeB.getRobot().update();

         RigidBodyTransform transformToWorld = new RigidBodyTransform();
         cubeA.getTransformToWorld(transformToWorld);

         CollisionDetectionResult result = new CollisionDetectionResult();
         collisionDetector.performCollisionDetection(result);

         assertTrue(result.getNumberOfCollisions() > 0);

         DetectedCollision collision = result.getCollision(0);

         Point3d pointOnA = new Point3d();
         Point3d pointOnB = new Point3d();
         collision.getPointOnA(pointOnA);
         collision.getPointOnB(pointOnB);

         System.out.println("Contacted A at " + pointOnA);
         System.out.println("Contacted B at " + pointOnB);
      }
   }



   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void collisionMask_hit()
   {
      ScsCollisionDetector collisionDetector = createCollisionDetector();

      // all 3 shapes will overlap.  the mask determines what intersects
      FloatingJoint cubeA = cube(collisionDetector, "A", 10, null, 0.5, 1, 1.5, 0x01, 0x02);
      FloatingJoint cubeB = cube(collisionDetector, "A", 10, null, 0.75, 1.2, 1.7, 0x02, 0x01);
      FloatingJoint cubeC = cube(collisionDetector, "A", 10, null, 10, 10, 10, 0x04, 0x04);

      // just do an offset so that not everything is at the origin
      cubeB.setPosition(0.4, 0, 0);

      cubeA.update();
      cubeB.update();
      cubeC.update();

      CollisionDetectionResult result = new CollisionDetectionResult();
      collisionDetector.performCollisionDetection(result);

      assertEquals(1, result.getNumberOfCollisions());
   }

   /**
    * Makes sure the offset from the link is handled correctly
    */

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void checkCollisionShape_offset()
   {
      ScsCollisionDetector collisionDetector = createCollisionDetector();

      CheckCollisionMasks check = new CheckCollisionMasks();

      collisionDetector.initialize(check);

      RigidBodyTransform offset = new RigidBodyTransform();
      offset.setTranslation(new Vector3d(0, 0, -1.7));

      FloatingJoint cubeA = cube(collisionDetector, "A", 10, null, 1, 1, 1, 2, 2);
      FloatingJoint cubeB = cube(collisionDetector, "B", 10, null, 1, 1, 1, 2, 2);
      FloatingJoint cubeC = cube(collisionDetector, "C", 10, offset, 1, 1, 1, 2, 2);

      cubeA.setPosition(0, 0, 0.5);
      cubeB.setPosition(0, 0, 0.5);

      cubeA.update();
      cubeB.update();
      cubeC.update();

      CollisionDetectionResult result = new CollisionDetectionResult();
      collisionDetector.performCollisionDetection(result);

      // only A and B should collide
      assertEquals(1, check.totalCollisions);
   }

   public FloatingJoint cube(ScsCollisionDetector collisionDetector, String name, double mass, double radiusX, double radiusY, double radiusZ)
   {
      return cube(collisionDetector, name, mass, null, radiusX, radiusY, radiusZ, 0xFFFFFFFF, 0xFFFFFFFF);
   }

   public FloatingJoint cube(ScsCollisionDetector collisionDetector, String name, double mass, RigidBodyTransform shapeToLink, double radiusX, double radiusY, double radiusZ, int collisionGroup, int collisionMask)
   {
      Robot robot = new Robot("null");
      FloatingJoint joint = new FloatingJoint("cube", new Vector3d(), robot);
      Link link = new Link(name);

      //    link.setMass(mass);
      //    link.setMomentOfInertia(0.1 * mass, 0.1 * mass, 0.1 * mass);
      //    link.enableCollisions(10,null);

      CollisionShapeFactory factory = collisionDetector.getShapeFactory();
      factory.setMargin(0.0000002);
      CollisionShapeDescription shapeDesc = factory.createBox(radiusX, radiusY, radiusZ);
      factory.addShape(link, shapeToLink, shapeDesc, false, collisionGroup, collisionMask);

      joint.setLink(link);

      robot.addRootJoint(joint);
      return joint;
   }

   private static class CheckCollisionMasks implements CollisionHandler
   {
      int totalCollisions = 0;

      public void initialize(ScsCollisionDetector collision)
      {
      }

      public void maintenanceBeforeCollisionDetection()
      {
      }

      public void maintenanceAfterCollisionDetection()
      {
      }

      public void addListener(CollisionHandlerListener listener)
      {
      }

      public void handle(CollisionShape shapeA, CollisionShape shapeB, Contacts contacts)
      {
         totalCollisions++;

         assertTrue((shapeA.getCollisionMask() & shapeB.getGroupMask()) != 0 || (shapeB.getCollisionMask() & shapeA.getGroupMask()) != 0);
      }
   }

   private void setPosition(FloatingJoint cubeJoint, double x, double y, double z)
   {
      cubeJoint.setPosition(x, y, z);
      cubeJoint.getRobot().update();
   }



}
