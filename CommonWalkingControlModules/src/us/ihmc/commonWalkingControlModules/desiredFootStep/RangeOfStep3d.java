package us.ihmc.commonWalkingControlModules.desiredFootStep;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameEllipsoid3d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.jme3.math.Vector3f;

public class RangeOfStep3d extends FrameEllipsoid3d
{
   private final RigidBody rigidBody;
   private final RobotSide robotSide;
   
   private final double offset;

   public RangeOfStep3d(RigidBody rigidBody, RobotSide robotSide, double sideLength, double forwardLength, double verticalLength, double offset)
   {
      super(rigidBody.getBodyFixedFrame(), 0.5 * sideLength, 0.5 * forwardLength, 0.5 * verticalLength);
      
      Transform3D offsetTranslation = new Transform3D();
      offsetTranslation.setTranslation(new Vector3d(0.0, robotSide.negateIfRightSide(offset), 0.0));
      
      this.applyTransform(offsetTranslation);
      
      this.rigidBody = rigidBody;
      this.robotSide = robotSide;
      this.offset = robotSide.negateIfRightSide(offset);
   }
   
   public boolean containsPoint(FramePoint pointToCheck)
   {
      this.changeFrame(rigidBody.getBodyFixedFrame());
      
      pointToCheck.changeFrame(this.getReferenceFrame());
      
      if ((robotSide == RobotSide.LEFT && pointToCheck.getY() < offset) || 
            (robotSide == RobotSide.RIGHT && pointToCheck.getY() > offset))
         return false;
      
      return this.isInsideOrOnSurface(pointToCheck);
   }
   
   public boolean containsPointInWorld(Vector3f mouseLocationInWorld)
   {
      return containsPoint(new FramePoint(ReferenceFrame.getWorldFrame(), mouseLocationInWorld.getX(), mouseLocationInWorld.getY(), mouseLocationInWorld.getZ()));
   }
}
