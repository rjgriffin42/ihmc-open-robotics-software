package com.yobotics.simulationconstructionset.joystick;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import net.java.games.input.Component;
import net.java.games.input.Event;

import org.junit.Test;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.DoubleYoVariable;

public class DoubleYoVariableJoystickEventListenerTest
{
   @Test
   public void testMinMaxAverage()
   {
      YoVariableRegistry registry = new YoVariableRegistry("test");
      DoubleYoVariable variable = new DoubleYoVariable("test", registry);
      TestComponent component = new TestComponent();
      component.setAnalog(true);
      component.setDeadZone(0.0f);

      int nTests = 100;
      for (int i = 0; i < nTests; i++)
      {
         Random random = new Random(12342L);
         double min = random.nextDouble() - 0.5;
         double max = min + random.nextDouble();
         double deadZone = 0.0;
         boolean signFlip = false;
         DoubleYoVariableJoystickEventListener listener = new DoubleYoVariableJoystickEventListener(variable, component, min, max, deadZone, signFlip);

         Event event = new Event();

         event.set(component, -1.0f, 0);
         listener.processEvent(event);
         assertEquals(min, variable.getDoubleValue(), 0.0);

         event.set(component, 1.0f, 0);
         listener.processEvent(event);
         assertEquals(max, variable.getDoubleValue(), 0.0);
         
         event.set(component, 0.0f, 0);
         listener.processEvent(event);
         assertEquals((min + max) / 2.0, variable.getDoubleValue(), 0.0);         
      }
   }

   private final class TestComponent implements Component
   {
      private boolean isRelative;
      private boolean isAnalog;
      private float pollData;
      private String name;
      private Identifier identifier;
      private float deadZone;

      public boolean isRelative()
      {
         return isRelative;
      }

      public boolean isAnalog()
      {
         return isAnalog;
      }

      public float getPollData()
      {
         return pollData;
      }

      public String getName()
      {
         return name;
      }

      public Identifier getIdentifier()
      {
         return identifier;
      }

      public float getDeadZone()
      {
         return deadZone;
      }

      public void setRelative(boolean isRelative)
      {
         this.isRelative = isRelative;
      }

      public void setAnalog(boolean isAnalog)
      {
         this.isAnalog = isAnalog;
      }

      public void setPollData(float pollData)
      {
         this.pollData = pollData;
      }

      public void setName(String name)
      {
         this.name = name;
      }

      public void setIdentifier(Identifier identifier)
      {
         this.identifier = identifier;
      }

      public void setDeadZone(float deadZone)
      {
         this.deadZone = deadZone;
      }
   }
}
