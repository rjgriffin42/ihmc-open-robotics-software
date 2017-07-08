package us.ihmc.robotics.math.trajectories;

import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FrameTuple3D;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.math.trajectories.NDoFTrapezoidalVelocityTrajectory.AlphaToAlphaType;


public class FramePointTrapezoidalVelocityTrajectory extends FrameNDoFTrapezoidalVelocityTrajectory
{
   public FramePointTrapezoidalVelocityTrajectory(double t0, FramePoint3D x0, FramePoint3D xF, FrameVector3D v0, FrameVector3D vF, FrameVector3D vMax, FrameVector3D aMax,
           AlphaToAlphaType alphaToAlphaType)
   {
      super(x0.getReferenceFrame(), t0, toArray(x0), toArray(xF), toArray(v0), toArray(vF), toArray(vMax), toArray(aMax), alphaToAlphaType);
      doReferenceFrameChecks(x0, xF, v0, vF, vMax, aMax);
   }

   private static double[] toArray(FrameTuple3D<?, ?> tuple)
   {
      return new double[]{tuple.getX(), tuple.getY(), tuple.getZ()};
   }

   public FramePoint3D getPosition(double t)
   {
      return new FramePoint3D(getReferenceFrame(), getPositionArray(t));
   }

   public FrameVector3D getVelocity(double t)
   {
      return new FrameVector3D(getReferenceFrame(), getVelocityArray(t));
   }

   public FrameVector3D getAcceleration(double t)
   {
      return new FrameVector3D(getReferenceFrame(), getAccelerationArray(t));
   }

   public FrameVector3D getMaximumVelocity()
   {
      return new FrameVector3D(getReferenceFrame(), getMaximumVelocityArray());
   }

   public FrameVector3D getMaximumAcceleration()
   {
      return new FrameVector3D(getReferenceFrame(), getMaximumAccelerationArray());
   }

   public FramePoint3D getInitialPosition()
   {
      return new FramePoint3D(getReferenceFrame(), getX0Array());
   }

   public FrameVector3D getInitialVelocity()
   {
      return new FrameVector3D(getReferenceFrame(), getV0Array());
   }
}
