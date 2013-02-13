package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.awt.geom.Ellipse2D;

import javax.media.j3d.Transform3D;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

public class RangeOfStep2d extends Ellipse2D.Double
{
   private static final long serialVersionUID = -2194197143315735789L;
   private static final Transform3D IDENTITY = new Transform3D();
      
   private final RobotSide robotSide;
   private final RigidBody rigidBody;
   private ReferenceFrame referenceFrame;
   
   
   public RangeOfStep2d(RigidBody rigidBody, RobotSide robotSide, double forwardLength, double sideLength, double offset)
   {
      super(-0.5 * sideLength, -0.5 * forwardLength + robotSide.negateIfRightSide(offset), forwardLength, sideLength);
      
      this.robotSide = robotSide;
      this.rigidBody = rigidBody;
      setReferenceFrame(rigidBody.getBodyFixedFrame());
      
      System.out.println("Coords of ellipse center: "+this.getCenterX()+", "+this.getCenterY());
      System.out.println(rigidBody.getBodyFixedFrame());
   }

    public boolean contains(FramePoint point)
    {
       updateReferenceFrame(rigidBody.getBodyFixedFrame());
       
       System.out.println("Before frame change: "+point);
       
       point.changeFrame(referenceFrame);
       
       System.out.println("After frame change: "+point);
      
       if (robotSide == RobotSide.LEFT && point.getY() < getCenterY())
          return false;
       
       if (robotSide == RobotSide.RIGHT && point.getY() > getCenterY())
          return false;
       
       return contains(point.getX(), point.getY());
    }

   private void updateReferenceFrame(ReferenceFrame referenceFrame)
   {
      Transform3D transform = this.referenceFrame.getTransformToDesiredFrame(referenceFrame);
      
      if (!transform.equals(IDENTITY))
         setReferenceFrame(referenceFrame);
   }
   
   private void setReferenceFrame(ReferenceFrame referenceFrame)
   {
      Transform3D transform = referenceFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
      
      this.referenceFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("translation", ReferenceFrame.getWorldFrame(), transform);
   }
}
