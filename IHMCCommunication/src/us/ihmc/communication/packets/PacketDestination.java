package us.ihmc.communication.packets;

public enum PacketDestination
{
   BROADCAST, CONTROLLER, NETWORK_PROCESSOR, UI, BEHAVIOR_MODULE, SCS_SENSORS, LEFT_HAND_MANAGER, RIGHT_HAND_MANAGER, SENSOR_MANAGER, PERCEPTION_MODULE, ROS_MODULE, SCRIPTED_FOOTSTEP_DATA_LIST, TRAFFIC_SHAPER, GFE;
   
   public static final PacketDestination[] values = values();
}
