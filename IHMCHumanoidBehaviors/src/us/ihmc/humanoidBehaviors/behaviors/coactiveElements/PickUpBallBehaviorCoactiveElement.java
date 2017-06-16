package us.ihmc.humanoidBehaviors.behaviors.coactiveElements;

import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.yoVariables.variable.EnumYoVariable;
import us.ihmc.yoVariables.variable.IntegerYoVariable;

public abstract class PickUpBallBehaviorCoactiveElement extends BehaviorCoactiveElement
{
   public enum PickUpBallBehaviorState
   {
      STOPPED,
      SETUP_ROBOT,
      SEARCHING_FOR_BALL_FAR,
      WALKING_TO_BALL,
      SEARCHING_FOR_BALL_NEAR,
      PICKING_UP_BALL,
      PUTTING_BALL_IN_BASKET,
      RESET_ROBOT,
      WAITING_FOR_USER_CONFIRMATION
   }

   //UI SIDE YOVARS
   public final IntegerYoVariable minHue = new IntegerYoVariable("minHue", userInterfaceWritableRegistry);
   public final IntegerYoVariable minSat = new IntegerYoVariable("minSat", userInterfaceWritableRegistry);
   public final IntegerYoVariable minVal = new IntegerYoVariable("minVal", userInterfaceWritableRegistry);
   public final IntegerYoVariable maxHue = new IntegerYoVariable("maxHue", userInterfaceWritableRegistry);
   public final IntegerYoVariable maxSat = new IntegerYoVariable("maxSat", userInterfaceWritableRegistry);
   public final IntegerYoVariable maxVal = new IntegerYoVariable("maxVal", userInterfaceWritableRegistry);
   
   public final IntegerYoVariable userInterfaceSideCount = new IntegerYoVariable("userInterfaceSideCount", userInterfaceWritableRegistry);
   public final YoBoolean abortClicked = new YoBoolean("abortClicked", userInterfaceWritableRegistry);
   public final YoBoolean validClicked = new YoBoolean("validClicked", userInterfaceWritableRegistry);

   //BEHAVIOR SIDE YOVARS
   public final EnumYoVariable<PickUpBallBehaviorState> currentState = new EnumYoVariable<PickUpBallBehaviorState>("currentPickUpState", machineWritableRegistry,
         PickUpBallBehaviorState.class);
   public final IntegerYoVariable machineSideCount = new IntegerYoVariable("machineSideCount", machineWritableRegistry);
   public final IntegerYoVariable abortCount = new IntegerYoVariable("abortCount", machineWritableRegistry);
   public final YoBoolean abortAcknowledged = new YoBoolean("abortAcknowledged", machineWritableRegistry);
   public final YoBoolean searchingForBall = new YoBoolean("searchingForBall", machineWritableRegistry);
   public final YoBoolean foundBall = new YoBoolean("foundBall", machineWritableRegistry);
   public final DoubleYoVariable ballX = new DoubleYoVariable("ballX", machineWritableRegistry);
   public final DoubleYoVariable ballY = new DoubleYoVariable("ballY", machineWritableRegistry);
   public final DoubleYoVariable ballZ = new DoubleYoVariable("ballZ", machineWritableRegistry);
   public final DoubleYoVariable ballRadius = new DoubleYoVariable("ballRadius", machineWritableRegistry);
   public final YoBoolean validAcknowledged = new YoBoolean("validAcknowledged", machineWritableRegistry);
   public final YoBoolean waitingForValidation = new YoBoolean("waitingForValidation", machineWritableRegistry);

}
