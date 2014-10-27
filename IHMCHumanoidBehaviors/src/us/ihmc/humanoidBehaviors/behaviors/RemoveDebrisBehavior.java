package us.ihmc.humanoidBehaviors.behaviors;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.communication.packets.behaviors.DebrisData;
import us.ihmc.communication.packets.behaviors.HumanoidBehaviorDebrisPacket;
import us.ihmc.humanoidBehaviors.behaviors.midLevel.RemovePieceOfDebrisBehavior;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class RemoveDebrisBehavior extends BehaviorInterface
{
   private final RemovePieceOfDebrisBehavior removePieceOfDebrisBehavior;

   private final ConcurrentListeningQueue<HumanoidBehaviorDebrisPacket> inputListeningQueue = new ConcurrentListeningQueue<HumanoidBehaviorDebrisPacket>();
   private final BooleanYoVariable isDone;
   private final BooleanYoVariable haveInputsBeenSet;
//   private final FullRobotModel fullRobotModel;

   private final ArrayList<DebrisData> debrisDataList = new ArrayList<>();
   private final ArrayList<DebrisData> sortedDebrisDataList = new ArrayList<>();
   private final LinkedHashMap<DebrisData, Double> debrisDistanceMap = new LinkedHashMap<>();

   private double currentDistanceToObject;
   private final FramePoint currentObjectPosition = new FramePoint();

   public RemoveDebrisBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, FullRobotModel fullRobotModel, ReferenceFrames referenceFrame,
         DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridge);
      removePieceOfDebrisBehavior = new RemovePieceOfDebrisBehavior(outgoingCommunicationBridge, fullRobotModel, referenceFrame, yoTime);
      isDone = new BooleanYoVariable("isDone", registry);
      haveInputsBeenSet = new BooleanYoVariable("hasInputsBeenSet", registry);

//      this.fullRobotModel = fullRobotModel;
      this.attachNetworkProcessorListeningQueue(inputListeningQueue, HumanoidBehaviorDebrisPacket.class);

   }

   @Override
   public void doControl()
   {
      if (!isDone.getBooleanValue())
         checkForNewInputs();
      if (removePieceOfDebrisBehavior.isDone())
      {
         removePieceOfDebrisBehavior.finalize();
         debrisDataList.remove(0);
         if (debrisDataList.isEmpty())
         {
            isDone.set(true);
            return;
         }
         removePieceOfDebrisBehavior.initialize();
         removePieceOfDebrisBehavior.setInputs(debrisDataList.get(0).getTransform(), debrisDataList.get(0).getPosition(), debrisDataList.get(0).getVector());
      }
      removePieceOfDebrisBehavior.doControl();
   }

   private void checkForNewInputs()
   {
      HumanoidBehaviorDebrisPacket newestPacket = inputListeningQueue.getNewestPacket();
      if (newestPacket != null)
      {
         debrisDataList.addAll(newestPacket.getDebrisDataList());
//         sortDebrisFromCloserToFarther();
         removePieceOfDebrisBehavior.initialize();
         removePieceOfDebrisBehavior.setInputs(debrisDataList.get(0).getTransform(), debrisDataList.get(0).getPosition(), debrisDataList.get(0).getVector());
         haveInputsBeenSet.set(true);
      }
   }

//   private void sortDebrisFromCloserToFarther()
//   {
//      for (int i = 0; i < debrisDataList.size(); i++)
//      {
//
//         DebrisData currentDebrisData = debrisDataList.get(i);
//         currentObjectPosition.changeFrame(ReferenceFrame.getWorldFrame());
//         currentObjectPosition.set(currentDebrisData.getPosition());
//         currentObjectPosition.changeFrame(fullRobotModel.getChest().getBodyFixedFrame());
//         currentDistanceToObject = currentObjectPosition.getX();
//
//         debrisDistanceMap.put(currentDebrisData, currentDistanceToObject);
//
//      }
//
//      sortedDebrisDataList.clear();
//      sortedDebrisDataList.add(debrisDataList.get(0));
//      for (int i = 1; i < debrisDataList.size(); i++)
//      {
//         currentDistanceToObject = debrisDistanceMap.get(debrisDataList.get(i));
//         int j = 0;
//         while (j < sortedDebrisDataList.size() && currentDistanceToObject > debrisDistanceMap.get(sortedDebrisDataList.get(j)))
//         {
//            j++;
//         }
//         if (j == sortedDebrisDataList.size())
//            sortedDebrisDataList.add(debrisDataList.get(i));
//         else
//            sortedDebrisDataList.add(j, debrisDataList.get(i));
//      }
//   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
      if (removePieceOfDebrisBehavior != null)
         removePieceOfDebrisBehavior.consumeObjectFromNetworkProcessor(object);
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
      if (removePieceOfDebrisBehavior != null)
         removePieceOfDebrisBehavior.consumeObjectFromController(object);
   }

   @Override
   public void stop()
   {
      removePieceOfDebrisBehavior.stop();
   }

   @Override
   public void enableActions()
   {
      removePieceOfDebrisBehavior.enableActions();
   }

   @Override
   public void pause()
   {
      removePieceOfDebrisBehavior.pause();
   }

   @Override
   public void resume()
   {
      removePieceOfDebrisBehavior.resume();
   }

   @Override
   public boolean isDone()
   {
      return isDone.getBooleanValue();
   }

   @Override
   public void finalize()
   {
      removePieceOfDebrisBehavior.finalize();
      debrisDataList.clear();
      sortedDebrisDataList.clear();
      isDone.set(false);
      haveInputsBeenSet.set(false);
   }

   @Override
   public void initialize()
   {
      debrisDataList.clear();
      sortedDebrisDataList.clear();
      isDone.set(false);
      haveInputsBeenSet.set(false);

   }

   @Override
   public boolean hasInputBeenSet()
   {
      return haveInputsBeenSet.getBooleanValue();
   }

}
