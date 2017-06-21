package us.ihmc.exampleSimulations.beetle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.java.games.input.Event;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.EnumYoVariable;
import us.ihmc.tools.inputDevices.joystick.Joystick;
import us.ihmc.tools.inputDevices.joystick.JoystickCustomizationFilter;
import us.ihmc.tools.inputDevices.joystick.JoystickEventListener;
import us.ihmc.tools.inputDevices.joystick.mapping.XBoxOneMapping;

public class HexapodGamePadManager implements JoystickEventListener
{
   private final Map<XBoxOneMapping, Double> channels = Collections.synchronizedMap(new EnumMap<XBoxOneMapping, Double>(XBoxOneMapping.class));
   
   private final EnumYoVariable<WholeBodyControllerCoreMode> controllerCoreMode;
   private final YoDouble desiredLinearVelocityX;
   private final YoDouble desiredLinearVelocityY;
   private final YoDouble desiredLinearVelocityZ;
   private final YoDouble currentOrientationPitch;
   private final YoDouble currentOrientationRoll;
   private final YoDouble desiredAngularVelocityZ;
   private final YoDouble desiredBodyHeight;
   
   public HexapodGamePadManager(Joystick device, YoVariableRegistry registry)
   {
      device.setCustomizationFilter(new JoystickCustomizationFilter(XBoxOneMapping.LEFT_TRIGGER, false, 0.05, 1, 1.0));
      device.setCustomizationFilter(new JoystickCustomizationFilter(XBoxOneMapping.RIGHT_TRIGGER, false, 0.05, 1, 1.0));
      device.setCustomizationFilter(new JoystickCustomizationFilter(XBoxOneMapping.LEFT_STICK_X, true, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(XBoxOneMapping.LEFT_STICK_Y, true, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(XBoxOneMapping.RIGHT_STICK_X, true, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(XBoxOneMapping.RIGHT_STICK_Y, true, 0.1, 1));
      
      device.setPollInterval(10);
      
     desiredLinearVelocityX = (YoDouble) registry.getVariable("BODYdesiredLinearVelocityX");
     desiredLinearVelocityY = (YoDouble) registry.getVariable("BODYdesiredLinearVelocityY");
     desiredLinearVelocityZ = (YoDouble) registry.getVariable("BODYdesiredLinearVelocityZ");
     desiredBodyHeight = (YoDouble) registry.getVariable("BODYdesiredBodyHeight");
     
     currentOrientationPitch = (YoDouble) registry.getVariable("BODYdesiredOrientationPitch");
     currentOrientationRoll = (YoDouble) registry.getVariable("BODYdesiredOrientationRoll");
     desiredAngularVelocityZ = (YoDouble) registry.getVariable("BODYdesiredAngularVelocityZ");
     controllerCoreMode = (EnumYoVariable<WholeBodyControllerCoreMode>) registry.getVariable("controllerCoreMode");
     
     
      
     device.addJoystickEventListener(this);
   }
   
   @Override
   public void processEvent(Event event)
   {
      // Store updated value in a cache so historical values for all channels can be used.
      channels.put(XBoxOneMapping.getMapping(event), (double) event.getValue());

      // Handle events that should trigger once immediately after the event is triggered.
      switch (XBoxOneMapping.getMapping(event))
      {
      case LEFT_BUMPER:
         break;
      case RIGHT_BUMPER:
         break;
      case LEFT_TRIGGER:
         double value = channels.get(XBoxOneMapping.LEFT_TRIGGER) - 1.0;
         currentOrientationRoll.set(value * -0.4);
         break;
      case RIGHT_TRIGGER:
         double value2 = channels.get(XBoxOneMapping.RIGHT_TRIGGER) - 1.0;
         currentOrientationRoll.set(value2 * -0.4);
         break;
      case LEFT_STICK_X:
         desiredLinearVelocityY.set(channels.get(XBoxOneMapping.LEFT_STICK_X) * 0.25);
         break;
      case LEFT_STICK_Y:
         desiredLinearVelocityX.set(channels.get(XBoxOneMapping.LEFT_STICK_Y) * 0.2);
         break;
      case RIGHT_STICK_X:
         desiredAngularVelocityZ.set(channels.get(XBoxOneMapping.RIGHT_STICK_X) * 0.5);
         break;
      case RIGHT_STICK_Y:
         currentOrientationPitch.set(channels.get(XBoxOneMapping.RIGHT_STICK_Y) * 0.2);
         break;
      case SELECT:
         controllerCoreMode.set(WholeBodyControllerCoreMode.INVERSE_DYNAMICS);
         break;
      case START:
         controllerCoreMode.set(WholeBodyControllerCoreMode.VIRTUAL_MODEL);
         break;
      case DPAD_DOWN:
         desiredBodyHeight.add(-0.01);
         break;
      case DPAD_UP:
         desiredBodyHeight.add(0.01);
         break;
      case DPAD:
         if(event.getValue() == 0.25)
         {
            desiredBodyHeight.add(0.01);
         }
         else if(event.getValue() == 0.75)
         {
            desiredBodyHeight.add(-0.01);
         }
      default:
         break;
      }
   }

}
