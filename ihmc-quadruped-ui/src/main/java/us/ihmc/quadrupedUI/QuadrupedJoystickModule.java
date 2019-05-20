package us.ihmc.quadrupedUI;

import controller_msgs.msg.dds.QuadrupedTeleopDesiredPose;
import controller_msgs.msg.dds.QuadrupedTeleopDesiredVelocity;
import javafx.animation.AnimationTimer;
import net.java.games.input.Event;
import org.apache.commons.lang3.mutable.MutableDouble;
import us.ihmc.commons.MathTools;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelControllerName;
import us.ihmc.messager.Messager;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettings;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsBasics;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.tools.inputDevices.joystick.Joystick;
import us.ihmc.tools.inputDevices.joystick.JoystickCustomizationFilter;
import us.ihmc.tools.inputDevices.joystick.JoystickEventListener;
import us.ihmc.tools.inputDevices.joystick.mapping.XBoxOneMapping;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static us.ihmc.quadrupedUI.QuadrupedXBoxBindings.*;

public class QuadrupedJoystickModule extends AnimationTimer implements JoystickEventListener
{
   private static final int pollRateMillis = 50;
   private static final double maximumBodyHeightOffset = 0.1;
   private static final double bodyHeightDeltaPerClick = 0.01;
   private static final double maxBodyYaw = 0.15;
   private static final double maxBodyRoll = 0.15;
   private static final double maxBodyPitch = 0.15;
   private static final double bodyOrientationShiftTime = 0.1;
   private static final double maxTranslationX = 0.25;
   private static final double maxTranslationY = 0.15;
   private static final double maxYSpeedFraction = 0.5;
   private static final double maxYawSpeedFraction = 0.75;

   private final MutableDouble maxVelocityY = new MutableDouble();
   private final MutableDouble maxVelocityYaw = new MutableDouble();
   private final MutableDouble bodyHeightOffset = new MutableDouble();

   private final Messager messager;
   private final double nominalBodyHeight;
   private final Map<XBoxOneMapping, Double> channels = Collections.synchronizedMap(new EnumMap<>(XBoxOneMapping.class));
   private final QuadrupedXGaitSettingsBasics xGaitSettings;
   private final AtomicBoolean joystickPollFlag = new AtomicBoolean();

   private final AtomicReference<Boolean> enabled;
   private final AtomicBoolean resetBodyPose = new AtomicBoolean(false);

   public QuadrupedJoystickModule(Messager messager, QuadrupedXGaitSettingsReadOnly defaultXGaitSettings, double nominalBodyHeight, Joystick joystick)
   {
      this.messager = messager;
      this.xGaitSettings = new QuadrupedXGaitSettings(defaultXGaitSettings);
      this.nominalBodyHeight = nominalBodyHeight;

      joystick.addJoystickEventListener(this);
      joystick.setPollInterval(pollRateMillis);
      configureJoystickFilters(joystick);

      for (XBoxOneMapping channel : XBoxOneMapping.values)
      {
         channels.put(channel, 0.0);
      }

      enabled = messager.createInput(QuadrupedUIMessagerAPI.EnableJoystickTopic, false);
      messager.registerTopicListener(QuadrupedUIMessagerAPI.XGaitSettingsTopic, xGaitSettings::set);
      messager.registerTopicListener(QuadrupedUIMessagerAPI.CurrentControllerNameTopic, state ->
      {
         if (state != HighLevelControllerName.WALKING)
         {
            enabled.set(false);
         }
      });
   }

   private static void configureJoystickFilters(Joystick device)
   {
      device.setCustomizationFilter(new JoystickCustomizationFilter(xVelocityMapping, xVelocityInvert, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(yVelocityMapping, yVelocityInvert, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(negativeYawRateMapping, negativeYawRateInvert, 0.05, 1, 1.0));
      device.setCustomizationFilter(new JoystickCustomizationFilter(positiveYawRateMapping, positiveYawRateInvert, 0.05, 1, 1.0));

      device.setCustomizationFilter(new JoystickCustomizationFilter(xTranslationMapping, xTranslationInvert, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(yTranslationMapping, yTranslationInvert, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(negativeYawMapping, negativeYawInvert, 0.05, 1, 1.0));
      device.setCustomizationFilter(new JoystickCustomizationFilter(positiveYawMapping, positiveYawInvert, 0.05, 1, 1.0));

      device.setCustomizationFilter(new JoystickCustomizationFilter(rollMapping, rollInvert, 0.1, 1));
      device.setCustomizationFilter(new JoystickCustomizationFilter(pitchMapping, pitchInvert, 0.1, 1));
   }

   @Override
   public void handle(long now)
   {
      if (!joystickPollFlag.getAndSet(false))
      {
         return;
      }

      if (resetBodyPose.getAndSet(false))
      {
         sendResetCommands();
      }

      if (!enabled.get())
      {
         return;
      }

      messager.submitMessage(QuadrupedUIMessagerAPI.XGaitSettingsTopic, xGaitSettings);

      processJoystickStepCommands();
      processJoystickBodyCommands();
      processJoystickHeightCommands();
   }

   private void sendResetCommands()
   {
      bodyHeightOffset.setValue(0.0);

      QuadrupedTeleopDesiredPose desiredPoseMessage = new QuadrupedTeleopDesiredPose();
      desiredPoseMessage.getPose().getPosition().set(0.0, 0.0, nominalBodyHeight);
      desiredPoseMessage.getPose().getOrientation().setYawPitchRoll(0.0, 0.0, 0.0);
      desiredPoseMessage.setPoseShiftTime(bodyOrientationShiftTime);

      messager.submitMessage(QuadrupedUIMessagerAPI.DesiredTeleopBodyPoseTopic, desiredPoseMessage);
      messager.submitMessage(QuadrupedUIMessagerAPI.DesiredBodyHeightTopic, nominalBodyHeight);
   }

   private void processJoystickStepCommands()
   {
      maxVelocityY.setValue(maxYSpeedFraction * xGaitSettings.getMaxSpeed());
      maxVelocityYaw.setValue(maxYawSpeedFraction * xGaitSettings.getMaxSpeed());

      double xVelocity = channels.get(xVelocityMapping) * xGaitSettings.getMaxSpeed();
      double yVelocity = channels.get(yVelocityMapping) * maxVelocityY.getValue();

      double bodyYawLeft = channels.get(negativeYawRateMapping);
      double bodyYawRight = channels.get(positiveYawRateMapping);
      double yawRate = (bodyYawLeft - bodyYawRight) * maxVelocityYaw.getValue();

      QuadrupedTeleopDesiredVelocity desiredVelocity = new QuadrupedTeleopDesiredVelocity();
      desiredVelocity.setDesiredXVelocity(xVelocity);
      desiredVelocity.setDesiredYVelocity(yVelocity);
      desiredVelocity.setDesiredYawVelocity(yawRate);
      messager.submitMessage(QuadrupedUIMessagerAPI.DesiredTeleopVelocity, desiredVelocity);
   }

   private void processJoystickBodyCommands()
   {
      double bodyRoll = channels.get(rollMapping) * maxBodyRoll;
      double bodyPitch = channels.get(pitchMapping) * maxBodyPitch;

      double bodyXTranslation = channels.get(xTranslationMapping) * maxTranslationX;
      double bodyYTranslation = channels.get(yTranslationMapping) * maxTranslationY;

      double bodyYawLeft = channels.get(negativeYawMapping);
      double bodyYawRight = channels.get(positiveYawMapping);
      double bodyYaw = (bodyYawLeft - bodyYawRight) * maxBodyYaw;

      QuadrupedTeleopDesiredPose desiredPoseMessage = new QuadrupedTeleopDesiredPose();
      desiredPoseMessage.getPose().getPosition().set(bodyXTranslation, bodyYTranslation, nominalBodyHeight + bodyHeightOffset.getValue());
      desiredPoseMessage.getPose().getOrientation().setYawPitchRoll(bodyYaw, bodyPitch, bodyRoll);
      desiredPoseMessage.setPoseShiftTime(bodyOrientationShiftTime);
      messager.submitMessage(QuadrupedUIMessagerAPI.DesiredTeleopBodyPoseTopic, desiredPoseMessage);
   }

   private void processJoystickHeightCommands()
   {
      if (channels.get(XBoxOneMapping.DPAD) == 0.25)
      {
         bodyHeightOffset.add(bodyHeightDeltaPerClick);
      }
      else if (channels.get(XBoxOneMapping.DPAD) == 0.75)
      {
         bodyHeightOffset.add(- bodyHeightDeltaPerClick);
      }

      bodyHeightOffset.setValue(MathTools.clamp(bodyHeightOffset.getValue(), maximumBodyHeightOffset));
      messager.submitMessage(QuadrupedUIMessagerAPI.DesiredBodyHeightTopic, nominalBodyHeight + bodyHeightOffset.getValue());
   }

   private void processStateChangeRequests(Event event)
   {
      if (event.getValue() < 0.5 || xGaitSettings == null)
         return;

      XBoxOneMapping mapping = XBoxOneMapping.getMapping(event);

      if (mapping == endPhaseShiftDown && channels.get(mapping) < 0.5) // the bumpers were firing twice for one click
      {
         xGaitSettings.setEndPhaseShift(xGaitSettings.getEndPhaseShift() - 90.0);
      }
      else if (mapping == endPhaseShiftUp && channels.get(mapping) < 0.5)
      {
         xGaitSettings.setEndPhaseShift(xGaitSettings.getEndPhaseShift() + 90.0);
      }

      if (mapping == XBoxOneMapping.START)
      {
         messager.submitMessage(QuadrupedUIMessagerAPI.EnableJoystickTopic, !enabled.get());
         messager.submitMessage(QuadrupedUIMessagerAPI.EnableStepTeleopTopic, false);
      }

      if (mapping == XBoxOneMapping.B)
      {
         resetBodyPose.set(true);
      }
   }

   @Override
   public void processEvent(Event event)
   {
      joystickPollFlag.set(true);
      processStateChangeRequests(event);
      channels.put(XBoxOneMapping.getMapping(event), (double) event.getValue());
   }
}
