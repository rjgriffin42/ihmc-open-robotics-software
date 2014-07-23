package us.ihmc.atlas.parameters;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotCameraParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotLidarParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPointCloudParamaters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotSensorInformation;
import us.ihmc.graphics3DAdapter.camera.VideoSettingsFactory;
import us.ihmc.graphics3DAdapter.camera.VideoSettingsH264LowLatency;
import us.ihmc.robotSide.SideDependentList;

public class AtlasSensorInformation implements DRCRobotSensorInformation
{
   private static final String multisense_namespace = "/multisense";
   private static final String baseTfName = multisense_namespace + "/head";
   private static final String multisenseHandoffFrame = "head";

   /**
    * Force Sensor Parameters
    */
   public static final String[] forceSensorNames = { "l_leg_akx", "r_leg_akx", "l_arm_wrx", "r_arm_wrx" };
   public static final SideDependentList<String> feetForceSensorNames = new SideDependentList<String>("l_leg_akx", "r_leg_akx");
   public static final SideDependentList<String> handForceSensorNames = new SideDependentList<String>("l_arm_wrx", "r_arm_wrx");
   
   /**
    * PPS Parameters
    */
   private static final int PPS_PROVIDER_PORT = 5050;
   private static final String MULTISENSE_SL_PPS_TOPIC = "/multisense/pps";
   
   /**
    * Camera Parameters
    */
   private final DRCRobotCameraParameters[] cameraParamaters = new DRCRobotCameraParameters[4];
   public static final int MULTISENSE_SL_LEFT_CAMERA_ID = 0;
   public static final int MULTISENSE_SL_RIGHT_CAMERA_ID = 1;
   public static final int BLACKFLY_LEFT_CAMERA_ID = 2;
   public static final int BLACKFLY_RIGHT_CAMERA_ID = 3;
   
   private static final String left_camera_name = "stereo_camera_left";
   private static final String left_camera_topic = multisense_namespace + "/left/image_rect_color/compressed";
   private static final String left_info_camera_topic = multisense_namespace +"/left/camera_info";
   private static final String left_frame_name = multisense_namespace + "/left_camera_frame";
   
   private static final String right_camera_name = "stereo_camera_right";
   private static final String right_camera_topic = multisense_namespace + "/right/image_rect/compressed";
   private static final String right_info_camera_topic = multisense_namespace +"/right/camera_info";
   private static final String right_frame_name = multisense_namespace + "/right_camera_frame";
   
   
   private static final String fisheye_pose_source = "utorso";
   private static final String fisheye_left_camera_topic = "/blackfly/camera/left/compressed";
   private static final String leftFisheyeCameraName = "l_situational_awareness_camera";
                        
   private static final String fisheye_right_camera_topic = "/blackfly/camera/right/compressed";
   private static final String right_fisheye_camera_name = "r_situational_awareness_camera";
   
   /**
    * Lidar Parameters
    */
   private static final double lidar_spindle_velocity = 5.1;
   
   private final DRCRobotLidarParameters[] lidarParamaters = new DRCRobotLidarParameters[1];
   public final int MULTISENSE_LIDAR_ID = 0;
   private final String lidarJointName; //this has to match LidarDataReceiver::LIDAR_HEAD_FRAME; gazebo should use: "hokuyo_joint"; 
   private final String lidarBaseFrame = multisense_namespace + "/head";
   private final String lidarEndFrame = multisense_namespace + "/head_hokuyo_frame";
   
   private static final String lidarSensorName = "head_hokuyo_sensor";
   private static final String lidarJointTopic = multisense_namespace + "joint_states";
   private static final String multisense_laser_topic_string = multisense_namespace+"/lidar_scan";
   private static final String bodyIMUSensor = "pelvis_imu_sensor";
   private static final String[] imuSensorsToUse = { bodyIMUSensor };
   
   /**
    * Stereo Parameters
    */
   private final DRCRobotPointCloudParamaters[] pointCloudParamaters = new DRCRobotPointCloudParamaters[1];
   private final int multiSenseStereoId = 0;
   private static final String stereoSensorName = "stereo_camera";
   private static final String stereoColorTopic = multisense_namespace + "image_points2_color";
   private final String stereoBaseFrame = multisense_namespace + "/head";
   private final String stereoEndFrame = multisense_namespace + "/left_camera_frame";
   
   public AtlasSensorInformation(boolean runningOnRealRobot)
   {
      VideoSettingsH264LowLatency videoSetting = VideoSettingsFactory.get32kBitSettingsWide();
      cameraParamaters[MULTISENSE_SL_LEFT_CAMERA_ID] = new DRCRobotCameraParameters(left_camera_name, left_camera_topic, left_info_camera_topic, multisenseHandoffFrame, left_frame_name, baseTfName, videoSetting, MULTISENSE_SL_LEFT_CAMERA_ID);
      cameraParamaters[MULTISENSE_SL_RIGHT_CAMERA_ID] = new DRCRobotCameraParameters(right_camera_name, right_camera_topic, right_info_camera_topic, multisenseHandoffFrame, right_frame_name, baseTfName, videoSetting, MULTISENSE_SL_RIGHT_CAMERA_ID);
      cameraParamaters[BLACKFLY_LEFT_CAMERA_ID] = new DRCRobotCameraParameters(leftFisheyeCameraName, fisheye_left_camera_topic, fisheye_pose_source, videoSetting, BLACKFLY_LEFT_CAMERA_ID);
      cameraParamaters[BLACKFLY_RIGHT_CAMERA_ID] = new DRCRobotCameraParameters(right_fisheye_camera_name, fisheye_right_camera_topic, fisheye_pose_source, videoSetting, BLACKFLY_RIGHT_CAMERA_ID);
      lidarJointName = runningOnRealRobot ? "head" : "hokuyo_joint";
      lidarParamaters[MULTISENSE_LIDAR_ID] = new DRCRobotLidarParameters(lidarSensorName, multisense_laser_topic_string, lidarJointName, lidarJointTopic, multisenseHandoffFrame, lidarBaseFrame, lidarEndFrame, lidar_spindle_velocity, MULTISENSE_LIDAR_ID);
      
      pointCloudParamaters[multiSenseStereoId] = new DRCRobotPointCloudParamaters(stereoSensorName, stereoColorTopic, stereoBaseFrame, stereoEndFrame, multiSenseStereoId);
   }
   
   @Override
   public DRCRobotLidarParameters[] getLidarParameters()
   {
      return lidarParamaters;
   }
   
   @Override
   public DRCRobotLidarParameters getPrimaryLidarParameters()
   {
      return lidarParamaters[MULTISENSE_LIDAR_ID];
   }

   @Override
   public String[] getIMUSensorsToUse()
   {
      return imuSensorsToUse;
   }
   
   @Override
   public String getPrimaryBodyImu()
   {
      return bodyIMUSensor;
   }
   
   @Override
   public String[] getForceSensorNames()
   {
      return forceSensorNames;
   }

   @Override
   public SideDependentList<String> getFeetForceSensorNames()
   {
      return feetForceSensorNames;
   }

   @Override
   public SideDependentList<String> getWristForceSensorNames()
   {
      return handForceSensorNames;
   }

   @Override
   public DRCRobotCameraParameters[] getCameraParameters()
   {
      return cameraParamaters;
   }

   @Override
   public DRCRobotCameraParameters getPrimaryCameraParameters()
   {
      return cameraParamaters[0];
   }

   public String getCameraStringBase()
   {
      return multisense_namespace;
   }

   public int getPPSProviderPort()
   {
      return PPS_PROVIDER_PORT;
   }

   public String getPPSRosTopic()
   {
      return MULTISENSE_SL_PPS_TOPIC;
   }

   @Override
   public DRCRobotPointCloudParamaters[] getPointCloudParameters()
   {
      return pointCloudParamaters;
   }

   @Override
   public DRCRobotPointCloudParamaters getPrimaryPointCloudParameters()
   {
      return pointCloudParamaters[multiSenseStereoId];
   }
}
