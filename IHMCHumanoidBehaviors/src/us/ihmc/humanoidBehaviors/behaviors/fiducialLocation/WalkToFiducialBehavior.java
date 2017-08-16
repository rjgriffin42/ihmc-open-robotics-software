package us.ihmc.humanoidBehaviors.behaviors.fiducialLocation;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningToolboxOutputStatus;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.robotSide.RobotSide;

public class WalkToFiducialBehavior extends AbstractBehavior
{
   private final YoBoolean sentPlanningRequest = new YoBoolean("SentPlanningRequest", registry);
   private final YoBoolean recievedPlan = new YoBoolean("RecievedPlan", registry);
   private final YoBoolean planValid = new YoBoolean("PlanValid", registry);

   private final FramePose goalPose = new FramePose();
   private final ConcurrentListeningQueue<FootstepPlanningToolboxOutputStatus> footstepPlanQueue = new ConcurrentListeningQueue<FootstepPlanningToolboxOutputStatus>(40);

   public WalkToFiducialBehavior(CommunicationBridgeInterface communicationBridge)
   {
      super(communicationBridge);
      attachNetworkListeningQueue(footstepPlanQueue, FootstepPlanningToolboxOutputStatus.class);
   }

   @Override
   public void doControl()
   {
      if (!sentPlanningRequest.getBooleanValue())
      {
         FootstepPlanningRequestPacket request = new FootstepPlanningRequestPacket();
         request.set(new FramePose(ReferenceFrame.getWorldFrame()), RobotSide.LEFT, goalPose);
         request.setDestination(PacketDestination.FOOTSTEP_PLANNING_TOOLBOX_MODULE);
         sendPacket(request);
      }

      if (!recievedPlan.getBooleanValue() && footstepPlanQueue.isNewPacketAvailable())
      {
         FootstepPlanningToolboxOutputStatus latestPacket = footstepPlanQueue.getLatestPacket();
         planValid.set(latestPacket.planningResult.validForExecution());

         if (planValid.getBooleanValue())
         {
            FootstepDataListMessage footstepDataList = latestPacket.footstepDataList;
            sendPacketToUI(footstepDataList);
         }

         recievedPlan.set(true);
      }
   }

   @Override
   public boolean isDone()
   {
      return false; //recievedPlan.getBooleanValue();
   }

   @Override
   public void onBehaviorEntered()
   {
      footstepPlanQueue.clear();
      sentPlanningRequest.set(false);
      recievedPlan.set(false);
      planValid.set(false);
   }

   public void setGoalPose(FramePose goalPose)
   {
      goalPose.setIncludingFrame(goalPose);
   }

   @Override
   public void onBehaviorAborted()
   {
   }

   @Override
   public void onBehaviorPaused()
   {
   }

   @Override
   public void onBehaviorResumed()
   {
   }

   @Override
   public void onBehaviorExited()
   {
   }
}
