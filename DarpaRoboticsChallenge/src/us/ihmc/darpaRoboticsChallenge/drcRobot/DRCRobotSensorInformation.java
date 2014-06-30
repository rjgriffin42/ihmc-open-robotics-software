package us.ihmc.darpaRoboticsChallenge.drcRobot;

import us.ihmc.robotSide.SideDependentList;

public interface DRCRobotSensorInformation
{
   public abstract String[] getIMUSensorsToUse();
   
   public abstract DRCRobotCameraParamaters[] getCameraParamaters();

   public abstract DRCRobotCameraParamaters getPrimaryCameraParamaters();
   
   public abstract String getLidarSensorName();

   public abstract String getLidarJointName();

   public abstract String getLidarScanTopic();

   public abstract String getLidarJointTopic();

   public abstract String[] getForceSensorNames();

   public abstract SideDependentList<String> getFeetForceSensorNames();
   
   public abstract SideDependentList<String> getWristForceSensorNames();

   public abstract String getPrimaryBodyImu();

   public abstract String getLidarBaseFrame();

   public abstract String getLidarEndFrame();
   
   public abstract double getLidarCRC();
}
