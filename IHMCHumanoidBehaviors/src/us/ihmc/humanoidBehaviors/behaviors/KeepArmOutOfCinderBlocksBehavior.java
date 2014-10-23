package us.ihmc.humanoidBehaviors.behaviors;

import us.ihmc.humanoidBehaviors.behaviors.primitives.HandPoseBehavior;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class KeepArmOutOfCinderBlocksBehavior extends BehaviorInterface
{

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final FullRobotModel fullRobotModel;
   private final ReferenceFrames referenceFrames;

   private final HandPoseBehavior handPoseBehavior;
   
   
   
   public KeepArmOutOfCinderBlocksBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, FullRobotModel fullRobotModel,
         ReferenceFrames referenceFrames, DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridge);
 
      this.fullRobotModel = fullRobotModel;
      this.referenceFrames = referenceFrames;
      
      
      handPoseBehavior = new HandPoseBehavior(outgoingCommunicationBridge, yoTime);
   }

   @Override
   public void initialize()
   {

   }


   @Override
   public void doControl()
   {
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
   }

   @Override
   public void stop()
   {
      isStopped.set(true);
   }

   @Override
   public void enableActions()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void pause()
   {
      isPaused.set(true);
   }

   @Override
   public void resume()
   {
      isPaused.set(false);

   }

   @Override
   public boolean isDone()
   {
      return false;
   }

   @Override
   public void finalize()
   {
      isPaused.set(false);
      isStopped.set(false);
   }

   public boolean hasInputBeenSet()
   {
         return false;
   }

}
