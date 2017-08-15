package us.ihmc.humanoidBehaviors.behaviors.debug;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.TextToSpeechPacket;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridgeInterface;
import us.ihmc.humanoidRobotics.communication.packets.ExecutionMode;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.time.YoStopwatch;

public class TestICPOptimizationBehavior extends AbstractBehavior
{
   private final HumanoidReferenceFrames referenceFrames;
   private final YoDouble swingTime = new YoDouble("BehaviorSwingTime", registry);
   private final YoDouble sleepTime = new YoDouble("BehaviorSleepTime", registry);
   private final YoDouble transferTime = new YoDouble("BehaviorTransferTime", registry);
   private final YoDouble stepLength = new YoDouble("BehaviorStepLength", registry);
   private final YoBoolean stepInPlace = new YoBoolean("StepInPlace", registry);
   private final YoBoolean abortBehavior = new YoBoolean("AbortBehavior", registry);

   private final YoStopwatch timer;

   public TestICPOptimizationBehavior(CommunicationBridgeInterface communicationBridge, HumanoidReferenceFrames referenceFrames, YoDouble yoTime)
   {
      super(communicationBridge);
      this.referenceFrames = referenceFrames;

      swingTime.set(1.2);
      transferTime.set(0.6);
      sleepTime.set(10.0);
      stepLength.set(0.3);

      timer = new YoStopwatch(yoTime);
   }

   @Override
   public void doControl()
   {
      if (!(timer.totalElapsed() > sleepTime.getDoubleValue()))
         return;

      FootstepDataListMessage footsteps = new FootstepDataListMessage(swingTime.getDoubleValue(), transferTime.getDoubleValue());
      footsteps.setExecutionMode(ExecutionMode.OVERRIDE);
      footsteps.setDestination(PacketDestination.BROADCAST);

      ReferenceFrame leftSoleFrame = referenceFrames.getSoleFrame(RobotSide.LEFT);
      ReferenceFrame rightSoleFrame = referenceFrames.getSoleFrame(RobotSide.RIGHT);
      FramePoint3D rightFoot = new FramePoint3D(rightSoleFrame);
      rightFoot.changeFrame(leftSoleFrame);
      FramePose stepPose = new FramePose(leftSoleFrame);
      stepPose.setY(-0.25);

      if (Math.abs(rightFoot.getX()) > 0.1)
      {
         sendPacket(new TextToSpeechPacket("Squaring up."));
      }
      else if (!stepInPlace.getBooleanValue())
      {
         sendPacket(new TextToSpeechPacket("Step forward."));
         stepPose.setX(stepLength.getDoubleValue());
      }
      else
      {
         sendPacket(new TextToSpeechPacket("Step in place."));
      }

      stepPose.changeFrame(ReferenceFrame.getWorldFrame());

      Point3D location = new Point3D();
      Quaternion orientation = new Quaternion();
      stepPose.getPose(location, orientation);

      FootstepDataMessage footstepData = new FootstepDataMessage(RobotSide.RIGHT, location, orientation);
      footsteps.add(footstepData);

      sendPacket(footsteps);
      timer.reset();
   }

   @Override
   public void onBehaviorEntered()
   {
      abortBehavior.set(false);
      stepInPlace.set(true);
      sendPacket(new TextToSpeechPacket("Starting to step forward and backward with the right foot."));
   }

   @Override
   public void onBehaviorAborted()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void onBehaviorPaused()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void onBehaviorResumed()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void onBehaviorExited()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean isDone()
   {
      return abortBehavior.getBooleanValue();
   }
}
