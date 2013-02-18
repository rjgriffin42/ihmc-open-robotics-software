package us.ihmc.darpaRoboticsChallenge;

import us.ihmc.graphics3DAdapter.camera.VideoSettings;
import us.ihmc.graphics3DAdapter.camera.VideoSettings.Quality;
import us.ihmc.graphics3DAdapter.camera.VideoSettingsH264LowLatency;

public class DRCConfigParameters
{
   // Set whether or not to use GFE Robot Model
   public static final boolean USE_GFE_ROBOT_MODEL = true;

   // Convenience field
   public static final boolean USE_R2_ROBOT_MODEL = !USE_GFE_ROBOT_MODEL;

   public static final boolean STREAM_VIDEO = true;

   // Networking
   public static final String SCS_MACHINE_IP_ADDRESS = "localhost"; //"10.100.0.37";
   public static final String OPERATOR_INTERFACE_IP_ADDRESS = "localhost"; //"10.4.8.1";
   public static final int BG_VIDEO_SERVER_PORT_NUMBER = 2099;

   public static final int ROBOT_DATA_RECEIVER_PORT_NUMBER = 7777;
   public static final long JOINT_DATA_IDENTIFIER = 5L;

   public static final int FOOTSTEP_PATH_PORT_NUMBER = 3333;
   public static final long FOOTSTEP_PATH_DATA_IDENTIFIER = 3333L;

   public static final int FOOTSTEP_STATUS_PORT_NUMBER = 4444;
   public static final long FOOTSTEP_STATUS_DATA_IDENTIFIER = 4444L;

   public static final int PAUSE_COMMAND_PORT_NUMBER = 5555;
   public static final long PAUSE_COMMAND_DATA_IDENTIFIER = 5555L;

   public static final int HEAD_ORIENTATION_PORT_NUMBER = 6666;
   public static final long HEAD_ORIENTATION_DATA_IDENTIFIER = 6666L;

   public static final int PELVIS_ORIENTATION_PORT_NUMBER = 8888;
   public static final long PELVIS_ORIENTATION_DATA_IDENTIFIER = 8888L;

   public static final int LIDAR_DATA_PORT_NUMBER = 4697;
   public static final long LIDAR_DATA_IDENTIFIER = 4697L;
   public static final int LIDAR_X_RESOLUTION_OVERRIDE = 50;

   public static final long ROBOT_JOINT_SERVER_UPDATE_MILLIS = 100;

   public static final boolean STREAM_VANILLA_LIDAR = false;
   public static final boolean STREAM_POLAR_LIDAR = true;

   public static final VideoSettings VIDEOSETTINGS = new VideoSettingsH264LowLatency(800, 600, Quality.MEDIUM);
   //   public static final VideoSettings VIDEOSETTINGS = new VideoSettingsH264LowLatency(200, 150, Quality.LOW);

   static final int LIDAR_UPDATE_RATE_OVERRIDE = 3;

   static final double LIDAR_VERTICAL_SCAN_ANGLE = 0.9;

   static final double LIDAR_HORIZONTAL_SCAN_ANGLE = 0.1;

   static final int LIDAR_SWEEPS_PER_SCAN = 12;//1

   static final int LIDAR_POINTS_PER_SWEEP = 40;//640

   static final boolean OVERRIDE_DRC_LIDAR_CONFIG = true;

   static final double MIN_LIDAR_DISTANCE = 0.2;

   public static final float LIDAR_MIN_DISTANCE = 0.2f;

   public static final float LIDAR_SWEEP_MAX_YAW = 0.6f;

   public static final float LIDAR_SWEEP_MIN_YAW = -0.6f;

   public static final float LIDAR_SCAN_MAX_PITCH = 0.2f; // tilting the lidar down towards the ground

   public static final float LDIAR_SCAN_MIN_PITCH = -0.1f;

   public static final double GRID_RESOLUTION = 0.05;// 5 centimeter resolution

}
