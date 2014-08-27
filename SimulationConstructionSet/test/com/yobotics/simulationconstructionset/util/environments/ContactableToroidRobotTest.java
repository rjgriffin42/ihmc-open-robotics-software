package com.yobotics.simulationconstructionset.util.environments;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.utilities.RandomTools;

public class ContactableToroidRobotTest
{
   @Test
   public void testIsPointOnOrInsideAtOrigin()
   {
      double majorRadius = ContactableToroidRobot.DEFAULT_RADIUS;
      double minorRadius = ContactableToroidRobot.DEFAULT_THICKNESS;
      double delta = 5e-4;

      ContactableToroidRobot bot = new ContactableToroidRobot("bot", new Transform3D());

      Point3d testPoint = new Point3d();

      for (double x = majorRadius - minorRadius; x < majorRadius + minorRadius; x += delta)
      {
         for (double y = majorRadius - minorRadius; y < majorRadius + minorRadius; y += delta)
         {
            for (double z = -minorRadius; z < minorRadius; z += delta)
            {
               double x_sq = x * x;
               double y_sq = y * y;
               double z_sq = z * z;
               double rma_sq = (majorRadius + minorRadius) * (majorRadius + minorRadius);
               double rmi_sq = (minorRadius * minorRadius);               

               if ((x_sq + y_sq < rma_sq) &&  ((x_sq + y_sq) + z_sq < rmi_sq))
               {
                  testPoint.set(x, y, z);
                  assertTrue("Nope: " + testPoint, bot.isPointOnOrInside(testPoint));
               }
            }
         }
      }
   }
   
   @Test
   public void testIsPointOnOrInsideNotAtOriginUsingTransform()
   {
      Random random = new Random(1972L);
      
      double majorRadius = ContactableToroidRobot.DEFAULT_RADIUS;
      double minorRadius = ContactableToroidRobot.DEFAULT_THICKNESS;
      double delta = 5e-4;
      
      Vector3d randomVector = RandomTools.generateRandomVector(random);
      Transform3D transform3d = new Transform3D();
      transform3d.setTranslation(randomVector);

      ContactableToroidRobot bot = new ContactableToroidRobot("bot", transform3d);

      Point3d pt = new Point3d();
      
      for (double x = randomVector.x + majorRadius - minorRadius; x < randomVector.x + majorRadius + minorRadius; x += delta)
      {
         for (double y = randomVector.y + majorRadius - minorRadius; y < randomVector.y + majorRadius + minorRadius; y += delta)
         {
            for (double z = randomVector.z - minorRadius; z < randomVector.z + minorRadius; z += delta)
            {
               double x_sq = x * x;
               double y_sq = y * y;
               double z_sq = z * z;
               double rma_sq = (randomVector.x + majorRadius + minorRadius) * (randomVector.x + majorRadius + minorRadius);
               double rmi_sq = (minorRadius * minorRadius);               

               if ((x_sq + y_sq < rma_sq) &&  ((x_sq + y_sq) + z_sq < rmi_sq))
               {
                  pt.set(x, y, z);
                  assertTrue("Nope: " + pt, bot.isPointOnOrInside(pt));
               }
            }
         }
      }
   }
   
   @Test
   public void testPointIsntInsideWhenUsingComOffset()
   {
      Random random = new Random(1972L);
      
      double majorRadius = ContactableToroidRobot.DEFAULT_RADIUS;
      double minorRadius = ContactableToroidRobot.DEFAULT_THICKNESS;
      double delta = 5e-4;
      
      Vector3d vector3d = RandomTools.generateRandomVector(random);
      Transform3D randomTransform = new Transform3D();
      randomTransform.setTranslation(vector3d);

      ContactableToroidRobot bot = new ContactableToroidRobot("bot", randomTransform);

      Point3d pt = new Point3d();
      
      for (double x = vector3d.x + majorRadius - minorRadius; x < vector3d.x + majorRadius + minorRadius; x += delta)
      {
         for (double y = vector3d.y + majorRadius - minorRadius; y < vector3d.y + majorRadius + minorRadius; y += delta)
         {
            for (double z = vector3d.z - minorRadius; z < vector3d.z + minorRadius; z += delta)
            {
               double x_sq = x * x;
               double y_sq = y * y;
               double z_sq = z * z;
               double rma_sq = (vector3d.x + majorRadius + minorRadius) * (vector3d.x + majorRadius + minorRadius);
               double rmi_sq = (minorRadius * minorRadius);               

               if ((x_sq + y_sq < rma_sq) &&  ((x_sq + y_sq) + z_sq < rmi_sq))
               {
                  pt.set(x, y, z);
                  assertFalse("Nope: " + pt, bot.isPointOnOrInside(pt));
               }
            }
         }
      }
   }

}
