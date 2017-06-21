package us.ihmc.simulationConstructionSetTools.joystick;

import net.java.games.input.Component;
import net.java.games.input.Event;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.tools.inputDevices.joystick.JoystickEventListener;

public class BooleanYoVariableJoystickEventListener implements JoystickEventListener
{
   private final YoBoolean variable;
   private final Component component;
   private final boolean flip;
   private final boolean toggle;
   
   public BooleanYoVariableJoystickEventListener(YoBoolean variable, Component component, boolean toggle)
   {
      this(variable, component, toggle, false);
   }

   public BooleanYoVariableJoystickEventListener(YoBoolean variable, Component component, boolean toggle,  boolean flip)
   {
      if (component.isAnalog())
         throw new RuntimeException("component is analog; should be digital (i.e. an on/off button)");
      this.variable = variable;
      this.component = component;
      this.flip = flip;
      this.toggle=toggle;
   }

   @Override
   public void processEvent(Event event)
   {
      if (event.getComponent() == component)
      {
         boolean value = event.getValue() == 1.0f;
         if(toggle)
         {
            if(value)
               variable.set(!variable.getBooleanValue());
         }
         else
         {
            variable.set(value ^ flip);
         }
      }
   }
}
