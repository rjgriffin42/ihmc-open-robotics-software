package us.ihmc.humanoidBehaviors.behaviors.coactiveElements;


import us.ihmc.humanoidBehaviors.behaviors.PickUpBallBehavior;

public class PickUpBallBehaviorCoactiveElementBehaviorSide extends PickUpBallBehaviorCoactiveElement
{
  
   private PickUpBallBehavior pickUpBallBehavior;

   public void setPickUpBallBehavior(PickUpBallBehavior pickUpBallBehavior)
   {
      this.pickUpBallBehavior = pickUpBallBehavior;
   }

   @Override public void initializeUserInterfaceSide()
   {
   }

   @Override public void updateUserInterfaceSide()
   {
   }

   @Override
   public void initializeMachineSide()
   {
      machineSideCount.set(100);
   }

   @Override
   public void updateMachineSide()
   {
      if (abortAcknowledged.getBooleanValue() && (!abortClicked.getBooleanValue()))
      {
         abortAcknowledged.set(false);
      }

      if ((abortClicked.getBooleanValue()) && (!abortAcknowledged.getBooleanValue()))
      {
         if (pickUpBallBehavior != null)
         {
            pickUpBallBehavior.abort();
         }
         abortCount.increment();
         abortAcknowledged.set(true);
      }

      machineSideCount.increment();
   }
}
