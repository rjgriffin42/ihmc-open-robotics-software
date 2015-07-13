package us.ihmc.simulationconstructionset.joystick;

import net.java.games.input.Component;
import net.java.games.input.Event;
import us.ihmc.utilities.inputDevices.joystick.JoystickEventListener;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;


public class BooleanYoVariableJoystickEventListener implements JoystickEventListener
{
   private final BooleanYoVariable variable;
   private final Component component;
   private final boolean flip;
   private final boolean toggle;
   
   public BooleanYoVariableJoystickEventListener(BooleanYoVariable variable, Component component, boolean toggle)
   {
      this(variable, component, toggle, false);
   }
        


   public BooleanYoVariableJoystickEventListener(BooleanYoVariable variable, Component component, boolean toggle,  boolean flip)
   {
      if (component.isAnalog())
         throw new RuntimeException("component is analog; should be digital (i.e. an on/off button)");
      this.variable = variable;
      this.component = component;
      this.flip = flip;
      this.toggle=toggle;
   }

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
