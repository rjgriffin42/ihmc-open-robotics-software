package us.ihmc.commonWalkingControlModules.stateEstimation;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;

public interface OrientationEstimator
{
   public abstract FrameOrientation getEstimatedOrientation();
   
   public abstract void setEstimatedOrientation(FrameOrientation estimatedOrientation);

   public abstract FrameVector getEstimatedAngularVelocity();

   public abstract void setEstimatedAngularVelocity(FrameVector estimatedAngularVelocity);
   
   public abstract FramePoint getEstimatedCoMPosition();

   public abstract void setEstimatedCoMPosition(FramePoint estimatedCoMPosition);

   public abstract FrameVector getEstimatedCoMVelocity();

   public abstract void setEstimatedCoMVelocity(FrameVector estimatedCoMVelocity);

   public abstract DenseMatrix64F getCovariance();

   public abstract DenseMatrix64F getState();

   public abstract void setState(DenseMatrix64F x, DenseMatrix64F covariance);
}
