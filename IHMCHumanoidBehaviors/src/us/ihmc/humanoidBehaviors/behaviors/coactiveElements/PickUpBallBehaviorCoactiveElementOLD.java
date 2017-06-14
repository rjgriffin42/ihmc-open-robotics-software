package us.ihmc.humanoidBehaviors.behaviors.coactiveElements;

import us.ihmc.yoVariables.variable.BooleanYoVariable;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.yoVariables.variable.EnumYoVariable;
import us.ihmc.yoVariables.variable.IntegerYoVariable;

public abstract class PickUpBallBehaviorCoactiveElementOLD extends BehaviorCoactiveElement
{
   public enum PickUpBallBehaviorState
   {
      STOPPED,
      ENABLING_LIDAR,
      SETTING_LIDAR_PARAMS,
      CLEARING_LIDAR,
      SEARCHING_FOR_BALL,
      WAITING_FOR_USER_CONFIRMATION,
      WALKING_TO_BALL,
      BENDING_OVER,
      REACHING_FOR_BALL,
      CLOSING_HAND,
      PICKING_UP_BALL,
      PUTTING_BALL_IN_BASKET
   }

   //UI SIDE YOVARS
   public final IntegerYoVariable minHue = new IntegerYoVariable("minHue", userInterfaceWritableRegistry);
   public final IntegerYoVariable minSat = new IntegerYoVariable("minSat", userInterfaceWritableRegistry);
   public final IntegerYoVariable minVal = new IntegerYoVariable("minVal", userInterfaceWritableRegistry);
   public final IntegerYoVariable maxHue = new IntegerYoVariable("maxHue", userInterfaceWritableRegistry);
   public final IntegerYoVariable maxSat = new IntegerYoVariable("maxSat", userInterfaceWritableRegistry);
   public final IntegerYoVariable maxVal = new IntegerYoVariable("maxVal", userInterfaceWritableRegistry);
   
   public final IntegerYoVariable userInterfaceSideCount = new IntegerYoVariable("userInterfaceSideCount", userInterfaceWritableRegistry);
   public final BooleanYoVariable abortClicked = new BooleanYoVariable("abortClicked", userInterfaceWritableRegistry);
   public final BooleanYoVariable validClicked = new BooleanYoVariable("validClicked", userInterfaceWritableRegistry);

   //BEHAVIOR SIDE YOVARS
   public final EnumYoVariable<PickUpBallBehaviorState> currentState = new EnumYoVariable<PickUpBallBehaviorState>("currentPickUpState", machineWritableRegistry,
         PickUpBallBehaviorState.class);
   public final IntegerYoVariable machineSideCount = new IntegerYoVariable("machineSideCount", machineWritableRegistry);
   public final IntegerYoVariable abortCount = new IntegerYoVariable("abortCount", machineWritableRegistry);
   public final BooleanYoVariable abortAcknowledged = new BooleanYoVariable("abortAcknowledged", machineWritableRegistry);
   public final BooleanYoVariable searchingForBall = new BooleanYoVariable("searchingForBall", machineWritableRegistry);
   public final BooleanYoVariable foundBall = new BooleanYoVariable("foundBall", machineWritableRegistry);
   public final DoubleYoVariable ballX = new DoubleYoVariable("ballX", machineWritableRegistry);
   public final DoubleYoVariable ballY = new DoubleYoVariable("ballY", machineWritableRegistry);
   public final DoubleYoVariable ballZ = new DoubleYoVariable("ballZ", machineWritableRegistry);
   public final DoubleYoVariable ballRadius = new DoubleYoVariable("ballRadius", machineWritableRegistry);
   public final BooleanYoVariable validAcknowledged = new BooleanYoVariable("validAcknowledged", machineWritableRegistry);
   public final BooleanYoVariable waitingForValidation = new BooleanYoVariable("waitingForValidation", machineWritableRegistry);

}
