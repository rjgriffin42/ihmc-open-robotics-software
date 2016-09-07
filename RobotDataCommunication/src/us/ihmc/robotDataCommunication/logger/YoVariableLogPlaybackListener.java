package us.ihmc.robotDataCommunication.logger;

import us.ihmc.SdfLoader.HumanoidFloatingRootJointRobot;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;

public interface YoVariableLogPlaybackListener
{
   public void setRobot(HumanoidFloatingRootJointRobot robot);
   public void setYoVariableRegistry(YoVariableRegistry registry);
   
   public void updated(long timestamp);
}
