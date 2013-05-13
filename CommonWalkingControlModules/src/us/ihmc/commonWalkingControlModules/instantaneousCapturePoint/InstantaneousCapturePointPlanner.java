package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;

public interface InstantaneousCapturePointPlanner
{
   public abstract void initializeSingleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double initialTime);

   public abstract void reInitializeSingleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double currentTime);

   public abstract void initializeDoubleSupportInitialTransfer(TransferToAndNextFootstepsData transferToAndNextFootstepsData, Point2d initialICPPosition,
           double initialTime);

   public abstract void initializeDoubleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double initialTime);

   public abstract void getICPPositionAndVelocity(FramePoint2d icpPostionToPack, FrameVector2d icpVelocityToPack, FramePoint2d ecmpToPack, double time);

   public abstract void reset(double time);

   public abstract boolean isDone(double time);

   public abstract FramePoint2d getFinalDesiredICP();


}
