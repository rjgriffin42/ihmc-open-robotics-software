package us.ihmc.quadrupedRobotics.planning.stepStream;

import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.providers.QuadrupedPreplannedStepInputProvider;
import us.ihmc.quadrupedRobotics.util.PreallocatedList;
import us.ihmc.quadrupedRobotics.util.TimeIntervalTools;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;

import java.util.ArrayList;

public class QuadrupedPreplannedStepStream implements QuadrupedStepStream
{
   private static int MAXIMUM_STEP_QUEUE_SIZE = 100;

   private final QuadrupedPreplannedStepInputProvider preplannedStepProvider;
   private final QuadrupedReferenceFrames referenceFrames;
   private final DoubleYoVariable timestamp;
   private final PreallocatedList<QuadrupedTimedStep> stepSequence;
   private final FrameOrientation bodyOrientation;

   public QuadrupedPreplannedStepStream(QuadrupedPreplannedStepInputProvider preplannedStepProvider, QuadrupedReferenceFrames referenceFrames, DoubleYoVariable timestamp)
   {
      this.preplannedStepProvider = preplannedStepProvider;
      this.referenceFrames = referenceFrames;
      this.timestamp = timestamp;
      this.stepSequence = new PreallocatedList<>(MAXIMUM_STEP_QUEUE_SIZE, QuadrupedTimedStep.class);
      this.bodyOrientation = new FrameOrientation();
   }

   @Override
   public void onEntry()
   {
      double currentTime = timestamp.getDoubleValue();
      boolean isExpressedInAbsoluteTime = preplannedStepProvider.isStepPlanExpressedInAbsoluteTime();
      ArrayList<QuadrupedTimedStep> steps = preplannedStepProvider.getAndClearSteps();
      stepSequence.clear();
      for (int i = 0; i < steps.size(); i++)
      {
         double timeShift = isExpressedInAbsoluteTime ? 0.0 : currentTime;
         double touchdownTime = steps.get(i).getTimeInterval().getEndTime();
         if (touchdownTime + timeShift >= currentTime)
         {
            stepSequence.add();
            stepSequence.get(stepSequence.size() - 1).set(steps.get(i));
            stepSequence.get(stepSequence.size() - 1).getTimeInterval().shiftInterval(timeShift);
         }
      }
      TimeIntervalTools.sortByEndTime(stepSequence);

      bodyOrientation.setToZero(referenceFrames.getCenterOfFeetZUpFrameAveragingLowestZHeightsAcrossEnds());
   }

   @Override
   public void process()
   {
      // dequeue completed steps
      double currentTime = timestamp.getDoubleValue();
      TimeIntervalTools.removeEndTimesLessThan(currentTime, stepSequence);

      bodyOrientation.setToZero(referenceFrames.getCenterOfFeetZUpFrameAveragingLowestZHeightsAcrossEnds());
   }

   @Override
   public void onExit()
   {
      stepSequence.clear();
   }

   @Override
   public void getBodyOrientation(FrameOrientation bodyOrientation)
   {
      bodyOrientation.setIncludingFrame(this.bodyOrientation);
   }

   @Override
   public PreallocatedList<QuadrupedTimedStep> getSteps()
   {
      return stepSequence;
   }
}
