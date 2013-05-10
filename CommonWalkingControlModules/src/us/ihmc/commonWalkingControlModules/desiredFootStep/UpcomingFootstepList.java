package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.stateEstimation.PointPositionGrabber;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.IntegerYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePose;

public class UpcomingFootstepList
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final FootstepProvider footstepProvider;

   private final IntegerYoVariable nextFootstepIndex = new IntegerYoVariable("nextFootstepIndex", registry);
   private final IntegerYoVariable nextNextFootstepIndex = new IntegerYoVariable("nextNextFootstepIndex", registry);
   private final IntegerYoVariable nextNextNextFootstepIndex = new IntegerYoVariable("nextNextNextFootstepIndex", registry);

   private final List<Footstep> nextFootstepList = new ArrayList<Footstep>();
   private final ArrayList<Footstep> nextNextFootstepList = new ArrayList<Footstep>();
   private final ArrayList<Footstep> nextNextNextFootstepList = new ArrayList<Footstep>();

   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoFramePose nextFootstepPose = new YoFramePose("nextFootstep", "", worldFrame, registry);

   public UpcomingFootstepList(FootstepProvider footstepProvider)
   {
      this.footstepProvider = footstepProvider;
   }

   public void checkForFootsteps(PointPositionGrabber pointPositionGrabber, BooleanYoVariable readyToGrabNextFootstep,
                                 EnumYoVariable<RobotSide> upcomingSupportLeg, SideDependentList<? extends ContactablePlaneBody> bipedFeet)
   {
      if (readyToGrabNextFootstep.getBooleanValue())
      {
         for (int i = nextFootstepList.size() - 1; i > nextFootstepIndex.getIntegerValue(); i--)
         {
            nextFootstepList.remove(i);
         }

         for (int i = nextNextFootstepList.size() - 1; i > nextNextFootstepIndex.getIntegerValue(); i--)
         {
            nextNextFootstepList.remove(i);
         }
         
         for (int i = nextNextNextFootstepList.size() - 1; i > nextNextNextFootstepIndex.getIntegerValue(); i--)
         {
            nextNextNextFootstepList.remove(i);
         }

         Footstep nextFootstep = footstepProvider.poll();

         if (nextFootstep != null)
         {
            if (pointPositionGrabber != null)
            {
               pointPositionGrabber.setExpectedFootstep(nextFootstep);
            }

            nextFootstepList.add(nextFootstep);
            nextFootstepIndex.set(nextFootstepList.size() - 1);

            upcomingSupportLeg.set(getRobotSide(nextFootstep.getBody(), bipedFeet).getOppositeSide());
            nextFootstepPose.set(nextFootstep.getPoseCopy());

            readyToGrabNextFootstep.set(false);

            Footstep nextNextFootstep = footstepProvider.peek();
            if (nextNextFootstep != null)
            {
               nextNextFootstepList.add(nextNextFootstep);
               nextNextFootstepIndex.set(nextNextFootstepList.size() - 1);
            }
            else
            {
               nextNextFootstepIndex.increment();
            }
            
            Footstep nextNextNextFootstep = footstepProvider.peekPeek();
            if (nextNextNextFootstep != null)
            {
               nextNextNextFootstepList.add(nextNextNextFootstep);
               nextNextNextFootstepIndex.set(nextNextNextFootstepList.size() - 1);
            }
            else
            {
               nextNextNextFootstepIndex.increment();
            }

         }

         else
         {
            nextFootstepList.clear();
            nextFootstepIndex.set(0);
            nextNextFootstepList.clear();
            nextNextFootstepIndex.set(0);
            nextNextNextFootstepList.clear();
            nextNextNextFootstepIndex.set(0);
         }
      }
   }

   public Footstep getNextFootstep()
   {
      if (nextFootstepIndex.getIntegerValue() >= nextFootstepList.size())
         return null;
      Footstep nextFootstep = nextFootstepList.get(nextFootstepIndex.getIntegerValue());

      return nextFootstep;
   }

   public Footstep getNextNextFootstep()
   {
      if (nextNextFootstepIndex.getIntegerValue() >= nextNextFootstepList.size())
         return null;
      Footstep nextNextFootstep = nextNextFootstepList.get(nextNextFootstepIndex.getIntegerValue());

      return nextNextFootstep;
   }
   
   public Footstep getNextNextNextFootstep()
   {
      if (nextNextNextFootstepIndex.getIntegerValue() >= nextNextNextFootstepList.size())
         return null;
      Footstep nextNextNextFootstep = nextNextNextFootstepList.get(nextNextNextFootstepIndex.getIntegerValue());

      return nextNextNextFootstep;
   }

   public void notifyComplete()
   {
      footstepProvider.notifyComplete();
   }

   public boolean isFootstepProviderEmpty()
   {
      return footstepProvider.isEmpty();
   }
   
   public int getNumberOfFootstepsToProvide()
   {
      return footstepProvider.getNumberOfFootstepsToProvide();
   }

   public boolean doesNextFootstepListHaveFewerThanTwoElements()
   {
      return (nextFootstepIndex.getIntegerValue() < 2);
   }

   public Footstep getFootstepTwoBackFromNextFootstepList()
   {
      return nextFootstepList.get(nextFootstepIndex.getIntegerValue() - 2);
   }

   private static RobotSide getRobotSide(ContactablePlaneBody body, SideDependentList<? extends ContactablePlaneBody> bipedFeet)
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         if (body == bipedFeet.get(robotSide))
            return robotSide;
      }

      throw new RuntimeException("ContactablePlaneBody: " + body + " not found.");
   }

   public boolean hasNextFootsteps()
   {
      return (nextFootstepList.size() > 0);
   }
}
