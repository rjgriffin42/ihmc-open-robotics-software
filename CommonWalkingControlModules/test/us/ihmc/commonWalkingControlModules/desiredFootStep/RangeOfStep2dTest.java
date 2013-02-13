package us.ihmc.commonWalkingControlModules.desiredFootStep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.commonWalkingControlModules.desiredFootStep.RangeOfStep2d;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

public class RangeOfStep2dTest
{
   private final double epsilon = 1e-7;
//   private final double verticalLength = 0.0;

   @Test
   public void testExampleUsage()
   {
      double forwardLength = 1.0;
      double sideLength = 1.0;
      double offset = 0.0;
      
      RigidBody rigidBody = new RigidBody("rigidBody", ReferenceFrame.getWorldFrame());
      
      RangeOfStep2d range = new RangeOfStep2d(rigidBody, RobotSide.LEFT, forwardLength, sideLength, offset);
      
      assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 0.0)));
      assertEquals(range.height, forwardLength, 1e-7);
      assertEquals(range.width, sideLength, 1e-7);
   }
   
   @Test
   public void testEllipseAtCenter()
   {
      double forwardLength = 1.0;
      double sideLength = 1.0;
      double offset = 0.1;
      
      RobotSide robotSide = null;
      
      RigidBody rigidBody = new RigidBody("rigidBody", ReferenceFrame.getWorldFrame());
      
      for (int i = 0; i < 2; i++)
      {
         if (i == 0)
            robotSide = RobotSide.LEFT;
         if (i == 1)
            robotSide = RobotSide.RIGHT;
         
         RangeOfStep2d range = new RangeOfStep2d(rigidBody, robotSide, forwardLength, sideLength, offset);
         
         // center of ellipse is shifted by offset so neither hemi-ellipse should contain the origin
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 0.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 1.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, 0.0, -1.0)));
         
         // moving in +/- y (depending on robotSide) by offset should give the midpoint of the straight edge of the hemi-ellipse
         assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, robotSide.negateIfRightSide(offset), 0.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, robotSide.negateIfRightSide(offset-epsilon), 0.0)));
         
         // from midpoint of flat edge, moving in +/- x by half the forwardLength should give the two corners of the hemi-ellipse
         assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), 0.5 * forwardLength - epsilon, robotSide.negateIfRightSide(offset), 0.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), forwardLength, robotSide.negateIfRightSide(offset-epsilon), 0.0)));
         
         assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), -0.5 * (forwardLength - epsilon), robotSide.negateIfRightSide(offset), 0.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), -forwardLength, robotSide.negateIfRightSide(offset - epsilon), 0.0)));
      }
   }
   
   @Test
   public void testTranslatedEllipse()
   {
      double forwardLength = 1.0;
      double sideLength = 1.0;
      double offset = 0.1;
      
      double x = 5.0;
      double y = 5.0;
      double z = 5.0;
      
      RobotSide robotSide = null;
      
      Transform3D transform = new Transform3D();
      transform.setTranslation(new Vector3d(x, y, z));
      ReferenceFrame frame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("translatedFrame", ReferenceFrame.getWorldFrame(), transform);
      
      RigidBody rigidBody = new RigidBody("rigidBody", frame);
      
      for (int i = 0; i < 2; i++)
      {
         if (i == 0)
            robotSide = RobotSide.LEFT;
         if (i == 1)
            robotSide = RobotSide.RIGHT;
         
         RangeOfStep2d range = new RangeOfStep2d(rigidBody, robotSide, forwardLength, sideLength, offset);
         
         System.out.println("center X: "+range.getCenterX()+" center Y: "+range.getCenterY());
         
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x, y, 0.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x, y, 1.0)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x, y, -1.0)));
         
         assertFalse(range.contains(new FramePoint(frame, 0.0, 0.0, 0.0)));
         
         assertTrue(range.contains(new FramePoint(frame, 0.0, robotSide.negateIfRightSide(offset), 0.0)));
         assertFalse(range.contains(new FramePoint(frame, 0.0, robotSide.negateIfRightSide(offset - epsilon), 0.0)));
         
         assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x, y + robotSide.negateIfRightSide(offset + epsilon), z)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x, y + robotSide.negateIfRightSide(offset - epsilon), z)));
         
         assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x + 0.5 * forwardLength - epsilon, y + robotSide.negateIfRightSide(offset + epsilon), z)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x + 0.5 * forwardLength + epsilon, y + robotSide.negateIfRightSide(offset + epsilon), z)));
         
         assertTrue(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x - (0.5 * forwardLength - epsilon), y + robotSide.negateIfRightSide(offset + epsilon), z)));
         assertFalse(range.contains(new FramePoint(ReferenceFrame.getWorldFrame(), x - (0.5 * forwardLength + epsilon), y + robotSide.negateIfRightSide(offset + epsilon), z)));
      }
   }
}
