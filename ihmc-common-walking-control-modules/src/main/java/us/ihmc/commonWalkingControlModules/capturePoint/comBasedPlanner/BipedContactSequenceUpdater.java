package us.ihmc.commonWalkingControlModules.capturePoint.comBasedPlanner;

import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FrameConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BipedContactSequenceUpdater
{
   private static final int maxCapacity = 5;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final SideDependentList<ConvexPolygon2D> defaultFootPolygons;

   private final RecyclingArrayList<BipedStepTransition> stepTransitionsInAbsoluteTime = new RecyclingArrayList<>(BipedStepTransition::new);

   private final RecyclingArrayList<SimpleBipedContactPhase> contactSequenceInRelativeTime = new RecyclingArrayList<>(maxCapacity, SimpleBipedContactPhase::new);
   private final RecyclingArrayList<SimpleBipedContactPhase> contactSequenceInAbsoluteTime = new RecyclingArrayList<>(maxCapacity, SimpleBipedContactPhase::new);

   private final List<RobotSide> feetInContact = new ArrayList<>();
   private final SideDependentList<FramePose3D> solePoses = new SideDependentList<>();
   private final SideDependentList<MovingReferenceFrame> soleFrames;

   private final FrameConvexPolygon2D tempPolygon = new FrameConvexPolygon2D();
   private final FrameConvexPolygon2D tempFrame = new FrameConvexPolygon2D();

   public BipedContactSequenceUpdater(SideDependentList<ConvexPolygon2D> defaultFootPolygons, SideDependentList<MovingReferenceFrame> soleFrames)
   {
      this.defaultFootPolygons = defaultFootPolygons;
      this.soleFrames = soleFrames;
      contactSequenceInAbsoluteTime.clear();
      contactSequenceInRelativeTime.clear();

      for (RobotSide robotSide : RobotSide.values)
      {
         feetInContact.add(robotSide);
         solePoses.set(robotSide, new FramePose3D());
      }
   }

   public void initialize()
   {
      contactSequenceInAbsoluteTime.clear();
   }

   public List<SimpleBipedContactPhase> getContactSequence()
   {
      return contactSequenceInRelativeTime;
   }

   public void update(List<? extends BipedTimedStep> stepSequence, List<RobotSide> currentFeetInContact, double currentTime)
   {
      // initialize contact state and sole positions
      for (RobotSide robotSide : RobotSide.values)
      {
         solePoses.get(robotSide).setToZero(soleFrames.get(robotSide));
      }
      feetInContact.clear();
      for (int footIndex = 0; footIndex < currentFeetInContact.size(); footIndex++)
         feetInContact.add(currentFeetInContact.get(footIndex));
   }

   public void computeStepTransitionsFromStepSequence(RecyclingArrayList<BipedStepTransition> stepTransitionsToPack, double currentTime,
                                          List<? extends BipedTimedStep> stepSequence)
   {
      stepTransitionsToPack.clear();
      for (int i = 0; i < stepSequence.size(); i++)
      {
         BipedTimedStep step = stepSequence.get(i);

         if (step.getTimeInterval().getStartTime() >= currentTime)
         {
            BipedStepTransition stepTransition = stepTransitionsToPack.add();
            stepTransition.reset();

            stepTransition.setTransitionTime(step.getTimeInterval().getStartTime());

            stepTransition.addTransition(BipedStepTransitionType.LIFT_OFF, step.getRobotSide(), step.getGoalPose());
         }

         if (step.getTimeInterval().getEndTime() >= currentTime)
         {
            BipedStepTransition stepTransition = stepTransitionsToPack.add();
            stepTransition.reset();

            stepTransition.setTransitionTime(step.getTimeInterval().getEndTime());
            stepTransition.addTransition(BipedStepTransitionType.TOUCH_DOWN, step.getRobotSide(), step.getGoalPose());
         }
      }

      // sort step transitions in ascending order as a function of time
      stepTransitionsToPack.sort(Comparator.comparingDouble(BipedStepTransition::getTransitionTime));

      // collapse the transitions that occur at the same time
      BipedContactSequenceTools.collapseTransitionEvents(stepTransitionsToPack);

      // remove any transitions that already happened
      stepTransitionsToPack.removeIf(transition -> transition.getTransitionTime() < currentTime);
   }

   // fixme this function is not correct

   private void computeContactPhasesFromStepTransitions()
   {
      int numberOfTransitions = stepTransitionsInAbsoluteTime.size();
      SimpleBipedContactPhase contactPhase = contactSequenceInAbsoluteTime.getLast();

      // compute transition time and center of pressure for each time interval
      for (int transitionNumber = 0; transitionNumber < numberOfTransitions; transitionNumber++)
      {
         BipedStepTransition stepTransition = stepTransitionsInAbsoluteTime.get(transitionNumber);

         for (int transitioningFootNumber = 0; transitioningFootNumber < stepTransition.getNumberOfFeetInTransition(); transitioningFootNumber++)
         {
            RobotSide transitionSide = stepTransition.getTransitionSide(transitioningFootNumber);
            switch (stepTransition.getTransitionType(transitioningFootNumber))
            {
            case LIFT_OFF:
               feetInContact.remove(transitionSide);
               break;
            case TOUCH_DOWN:
               feetInContact.add(transitionSide);
               solePoses.get(transitionSide).setIncludingFrame(stepTransition.transitionPose(transitionSide));
               break;
            }
         }

         // end the previous phase and add a new one
         contactPhase.getTimeInterval().setEndTime(stepTransition.getTransitionTime());
         contactPhase = contactSequenceInAbsoluteTime.add();

         contactPhase.reset();
         contactPhase.setFeetInContact(feetInContact);
//         contactPhase.set(solePositions);
         contactPhase.getTimeInterval().setStartTime(stepTransition.getTransitionTime());
         contactPhase.update();


         boolean isLastContact = (transitionNumber == numberOfTransitions - 1) || (contactSequenceInAbsoluteTime.size() == maxCapacity);
         if (isLastContact)
            break;
      }

      contactPhase.getTimeInterval().setEndTime(Double.POSITIVE_INFINITY);

   }


}
