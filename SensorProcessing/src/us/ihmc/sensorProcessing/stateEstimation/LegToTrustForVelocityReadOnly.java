package us.ihmc.sensorProcessing.stateEstimation;

import us.ihmc.robotSide.RobotSide;

public interface LegToTrustForVelocityReadOnly
{
   public abstract boolean isLegTrustedForVelocity(RobotSide robotSide);
   public abstract RobotSide getSupportLeg();
   public abstract RobotSide getLegToUseForCOMOffset();
}