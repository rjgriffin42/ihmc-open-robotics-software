package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class InstantaneousCapturePointPlannerWithTimeFreezer implements InstantaneousCapturePointPlanner
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final DoubleYoVariable timeDelay = new DoubleYoVariable("timeDelay", registry);
   private final DoubleYoVariable icpError = new DoubleYoVariable("icpError", registry);
   private final DoubleYoVariable maxICPErrorForStartingSwing = new DoubleYoVariable("maxICPErrorForStartingSwing", registry);
   
   private final DoubleYoVariable icpDistanceToFreezeLine = new DoubleYoVariable("icpDistanceToFreezeLine", registry);
   
   private final DoubleYoVariable previousTime = new DoubleYoVariable("previousTime", registry);
   private final DoubleYoVariable freezeTimeFactor = new DoubleYoVariable("freezeTimeFactor", "Set to 0.0 to turn off, 1.0 to completely freeze time", registry);
   private final double maxFreezeLineICPErrorWithoutTimeFreeze = 0.03; 
   
   
   private final InstantaneousCapturePointPlanner instantaneousCapturePointPlanner;
   
   public InstantaneousCapturePointPlannerWithTimeFreezer(InstantaneousCapturePointPlanner instantaneousCapturePointPlanner, YoVariableRegistry parentRegistry)
   {
      this.instantaneousCapturePointPlanner = instantaneousCapturePointPlanner;
      
      parentRegistry.addChild(registry);
      timeDelay.set(0.0);
      freezeTimeFactor.set(0.9); 
      maxICPErrorForStartingSwing.set(0.02); 
   }
   
   public void initializeSingleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double initialTime)
   {
      instantaneousCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, initialTime);
      resetTiming(initialTime);
   }

   public void reInitializeSingleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double currentTime)
   {
      instantaneousCapturePointPlanner.reInitializeSingleSupport(transferToAndNextFootstepsData, currentTime);
   }

   public void initializeDoubleSupportInitialTransfer(TransferToAndNextFootstepsData transferToAndNextFootstepsData, Point2d initialICPPosition,
         double initialTime)
   {
      instantaneousCapturePointPlanner.initializeDoubleSupportInitialTransfer(transferToAndNextFootstepsData, initialICPPosition, initialTime);
      resetTiming(initialTime);
   }

   public void initializeDoubleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double initialTime)
   {
      instantaneousCapturePointPlanner.initializeDoubleSupport(transferToAndNextFootstepsData, initialTime);
      resetTiming(initialTime);
   }

   
   public void getICPPositionAndVelocity(FramePoint2d icpPostionToPack, FrameVector2d icpVelocityToPack, FramePoint2d ecmpToPack, FramePoint2d actualICP,
         double time)
   {
      instantaneousCapturePointPlanner.getICPPositionAndVelocity(icpPostionToPack, icpVelocityToPack, ecmpToPack, actualICP, getTimeWithDelay(time));    

      icpError.set(icpPostionToPack.distance(actualICP));
      icpDistanceToFreezeLine.set(computeDistanceFromFreezeLine(icpPostionToPack, icpVelocityToPack, actualICP));

      if (this.isDone(time))
      {
         freezeTime(time, 1.0);
      }
      else if ((getEstimatedTimeRemainingForState(time) < 0.1) && 
            (instantaneousCapturePointPlanner.isPerformingICPDoubleSupport()) && 
            //            (icpError.getDoubleValue() > maxICPErrorForStartingSwing.getDoubleValue()))
            (icpDistanceToFreezeLine.getDoubleValue() > maxICPErrorForStartingSwing.getDoubleValue()))
      {
         freezeTime(time, 1.0);
      }

      else if ((icpDistanceToFreezeLine.getDoubleValue() > maxFreezeLineICPErrorWithoutTimeFreeze))
      {
         freezeTime(time, freezeTimeFactor.getDoubleValue());
      }
      
      previousTime.set(time);
   }

   private FrameVector2d normalizedVelocityVector = new FrameVector2d(ReferenceFrame.getWorldFrame());
   private FrameVector2d vectorFromDesiredToActualICP = new FrameVector2d(ReferenceFrame.getWorldFrame());
   private FrameVector2d deltaICP = new FrameVector2d(ReferenceFrame.getWorldFrame());
   
   private double computeDistanceFromFreezeLine(FramePoint2d icpPostionToPack, FrameVector2d icpVelocityToPack, FramePoint2d actualICP)
   {
      normalizedVelocityVector.setAndChangeFrame(icpVelocityToPack);
      normalizedVelocityVector.normalize();
            
      vectorFromDesiredToActualICP.setAndChangeFrame(actualICP);
      vectorFromDesiredToActualICP.sub(icpPostionToPack);
      
      double distance = vectorFromDesiredToActualICP.dot(normalizedVelocityVector);
      
      deltaICP.setAndChangeFrame(normalizedVelocityVector);
      deltaICP.scale(distance);
      return -distance;
   }

   private void freezeTime(double time, double freezeTimeFactor)
   {      
      double timeInState = instantaneousCapturePointPlanner.getTimeInState(getTimeWithDelay(time));
      if (timeInState < 0.0) return;
      
      timeDelay.add(freezeTimeFactor * (time - previousTime.getDoubleValue()));
   }

   public void reset(double time)
   {
      instantaneousCapturePointPlanner.reset(time);  
      resetTiming(time);
   }

   public boolean isDone(double time)
   {      
      if (instantaneousCapturePointPlanner.isPerformingICPDoubleSupport())
      {
         return instantaneousCapturePointPlanner.isDone(getTimeWithDelay(time));
      }
      else
      {
         return instantaneousCapturePointPlanner.isDone(getTimeWithDelay(time));
      }
   }

   public double getEstimatedTimeRemainingForState(double time)
   {
      return instantaneousCapturePointPlanner.getEstimatedTimeRemainingForState(getTimeWithDelay(time));
   }

   public boolean isPerformingICPDoubleSupport()
   {
      return instantaneousCapturePointPlanner.isPerformingICPDoubleSupport();
   }

   public FramePoint2d getFinalDesiredICP()
   {
      return instantaneousCapturePointPlanner.getFinalDesiredICP();
   }
   
   public double getTimeInState(double time)
   {
      return instantaneousCapturePointPlanner.getTimeInState(getTimeWithDelay(time));
   }
   
   private void resetTiming(double initialTime)
   {
      timeDelay.set(0.0);
      previousTime.set(initialTime);
   }
   
   private double getTimeWithDelay(double time)
   {
      return time - timeDelay.getDoubleValue();
   }

   public void setDoHeelToToeTransfer(boolean doHeelToToeTransfer)
   {
      instantaneousCapturePointPlanner.setDoHeelToToeTransfer(doHeelToToeTransfer);
   }
}
