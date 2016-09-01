package us.ihmc.commonWalkingControlModules.sensors.footSwitch;

import us.ihmc.commonWalkingControlModules.touchdownDetector.TouchdownDetector;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.sensors.FootSwitchInterface;

import java.util.ArrayList;

public abstract class TouchdownDetectorBasedFootswitch implements FootSwitchInterface
{
   protected final YoVariableRegistry registry;
   protected final ArrayList<TouchdownDetector> touchdownDetectors = new ArrayList<>();

   protected final BooleanYoVariable controllerThinksHasTouchedDown;

   protected boolean touchdownDetectorsUpdated = false;

   public TouchdownDetectorBasedFootswitch(String name, YoVariableRegistry parentRegistry)
   {
      this.registry = parentRegistry;
      controllerThinksHasTouchedDown = new BooleanYoVariable(name + "_controllerThinksHasTouchedDown", parentRegistry);
   }

   @Override
   public void reset()
   {
      controllerThinksHasTouchedDown.set(false);
   }

   @Override
   public boolean getForceMagnitudePastThreshhold()
   {
      return false;
   }

   @Override
   public void setFootContactState(boolean hasFootHitGround)
   {
      controllerThinksHasTouchedDown.set(hasFootHitGround);
   }
}
