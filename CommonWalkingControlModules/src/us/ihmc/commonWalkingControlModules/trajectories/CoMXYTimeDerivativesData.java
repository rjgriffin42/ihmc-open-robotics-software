package us.ihmc.commonWalkingControlModules.trajectories;

import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class CoMXYTimeDerivativesData
{
   private FramePoint2d comXYPosition = new FramePoint2d(ReferenceFrame.getWorldFrame());
   private FrameVector2d comXYVelocity = new FrameVector2d(ReferenceFrame.getWorldFrame());
   private FrameVector2d comXYAcceleration = new FrameVector2d(ReferenceFrame.getWorldFrame());
   
   public void set(CoMXYTimeDerivativesData comXYData)
   {
      this.comXYPosition.set(comXYData.comXYPosition);
      this.comXYVelocity.set(comXYData.comXYVelocity);
      this.comXYAcceleration.set(comXYData.comXYAcceleration);
   }
   
   public void getCoMXYPosition(FramePoint2d comXYPositionToPack)
   {
      comXYPositionToPack.set(this.comXYPosition);
   }
   
   public void getCoMXYVelocity(FrameVector2d comXYVelocityToPack)
   {
      comXYVelocityToPack.set(this.comXYVelocity);
   }
   
   public void getCoMXYAcceleration(FrameVector2d comXYAccelerationToPack)
   {
      comXYAccelerationToPack.set(this.comXYAcceleration);
   }
   
   public void setCoMXYPosition(FramePoint2d comXYPosition)
   {
      this.comXYPosition.set(comXYPosition);
   }
   
   public void setCoMXYVelocity(FrameVector2d comXYVelocity)
   {
      this.comXYVelocity.set(comXYVelocity);
   }
   
   public void setCoMXYAcceleration(FrameVector2d comXYAcceleration)
   {
      this.comXYAcceleration.set(comXYAcceleration);
   }
}
