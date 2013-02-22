package us.ihmc.commonWalkingControlModules.desiredFootStep;

import us.ihmc.robotSide.RobotSide;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class DesiredFootstepCalculatorFootstepProviderWrapper implements FootstepProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final DesiredFootstepCalculator desiredFootstepCalculator;
   private final EnumYoVariable<RobotSide> nextSwingLeg = EnumYoVariable.create("nextSwingLeg", RobotSide.class, registry);
   private final BooleanYoVariable walk = new BooleanYoVariable("walk", registry);
   private Footstep lastPolledFootstep;

   public DesiredFootstepCalculatorFootstepProviderWrapper(DesiredFootstepCalculator desiredFootstepCalculator, YoVariableRegistry parentRegistry)
   {
      this.desiredFootstepCalculator = desiredFootstepCalculator;
      parentRegistry.addChild(registry);
   }

   public void setNextSwingLeg(RobotSide nextSwingLeg)
   {
      this.nextSwingLeg.set(nextSwingLeg);
   }

   public Footstep poll()
   {
      Footstep ret = null;
      if (!isEmpty())
      {
         RobotSide supportLeg = nextSwingLeg.getEnumValue().getOppositeSide();
         desiredFootstepCalculator.initializeDesiredFootstep(supportLeg);
         ret = desiredFootstepCalculator.updateAndGetDesiredFootstep(supportLeg);
         nextSwingLeg.set(supportLeg);
      }

      lastPolledFootstep = ret;

      return ret;
   }

   public Footstep peek()
   {
      return desiredFootstepCalculator.predictFootstepAfterDesiredFootstep(nextSwingLeg.getEnumValue(), lastPolledFootstep);
   }

   public boolean isEmpty()
   {
      return !walk.getBooleanValue();
   }

   public void notifyComplete()
   {
//    System.out.println("DesiredFootstepCalculatorFootstepProviderWrapper: notifyComplete not implemented in DesiredFootstepCalculatorFootstepProviderWrapper");
   }

   public void setWalk(boolean walk)
   {
      this.walk.set(walk);
   }
}
