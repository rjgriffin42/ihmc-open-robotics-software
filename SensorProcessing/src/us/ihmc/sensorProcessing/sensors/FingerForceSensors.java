package us.ihmc.sensorProcessing.sensors;

import us.ihmc.robotics.humanoidRobot.partNames.FingerName;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.utilities.robotSide.RobotSide;

public interface FingerForceSensors
{
   public abstract FrameVector getFingerForce(RobotSide robotSide, FingerName fingerName);
}
