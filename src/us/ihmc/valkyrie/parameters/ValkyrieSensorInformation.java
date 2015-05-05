package us.ihmc.valkyrie.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import us.ihmc.sensorProcessing.parameters.DRCRobotCameraParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotLidarParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotPointCloudParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorParameters;
import us.ihmc.utilities.Triplet;
import us.ihmc.utilities.humanoidRobot.model.ContactSensorType;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.valkyrie.configuration.ValkyrieConfigurationRoot;

public class ValkyrieSensorInformation implements DRCRobotSensorInformation
{

   public static final String[] forceSensorNames;
   static
   {
      if (ValkyrieConfigurationRoot.VALKYRIE_WITH_ARMS)
         forceSensorNames = new String[]{ "LeftAnkle", "RightAnkle", "LeftForearmSupinator", "RightForearmSupinator" };
      else
         forceSensorNames = new String[]{ "LeftAnkle", "RightAnkle"};
   }
   
   private static final SideDependentList<String> urdfTekscanSensorNames = new SideDependentList<String>("/v1/LeftLegHermes_Offset", "/v1/RightLegHermes_Offset");
   private static final SideDependentList<String> footContactSensorNames = new SideDependentList<String>("LeftFootContactSensor","RightFootContactSensor");
   private static final SideDependentList<String> feetForceSensorNames = new SideDependentList<String>("LeftAnkle", "RightAnkle");
   private static final SideDependentList<String> urdfFeetForceSensorNames = new SideDependentList<>("/v1/LeftLeg6Axis_Offset", "/v1/RightLeg6Axis_Offset");
   public static final SideDependentList<LinkedHashMap<String, LinkedHashMap<String,ContactSensorType>>> contactSensors = new SideDependentList<LinkedHashMap<String,LinkedHashMap<String,ContactSensorType>>>();

   public static final boolean USE_JSC_FOOT_MASS_TARING = false;

   public static final SideDependentList<RigidBodyTransform> transformFromSixAxisMeasurementToAnkleZUpFrames = new SideDependentList<>();
   static
   {     
      RigidBodyTransform translateForwardAndDownOnFoot = new RigidBodyTransform();
      translateForwardAndDownOnFoot.setTranslation(0.02150, 0.0, -0.058547);  //from Will's CAD measurement
      
      RigidBodyTransform rotYBy7dot5 = new RigidBodyTransform();
      rotYBy7dot5.rotY(-Math.PI/24.0);
      
      RigidBodyTransform rotXByPi = new RigidBodyTransform();
      rotXByPi.rotX(Math.PI);
      
      RigidBodyTransform rotateZ60Degrees = new RigidBodyTransform();
      rotateZ60Degrees.rotZ(-Math.PI/3.0);
      
      RigidBodyTransform leftTransform = new RigidBodyTransform();
      leftTransform.multiply(translateForwardAndDownOnFoot);
      leftTransform.multiply(rotYBy7dot5);
      leftTransform.multiply(rotateZ60Degrees);
      leftTransform.multiply(rotXByPi);

      transformFromSixAxisMeasurementToAnkleZUpFrames.put(RobotSide.LEFT, leftTransform);
      transformFromSixAxisMeasurementToAnkleZUpFrames.put(RobotSide.RIGHT, new RigidBodyTransform(leftTransform));
   }
   
   public static final SideDependentList<RigidBodyTransform> transformFromTekscanMeasurementToAnkleZUpFrames = new SideDependentList<>();
   static
   {
      //Fortunately the Tekscan measurement frame is a z-up frame and is aligned with the foot, so only translation required.
      RigidBodyTransform transform = new RigidBodyTransform();
      transform.setTranslation(-0.0289334,0,0.075159108);
      
      transformFromTekscanMeasurementToAnkleZUpFrames.put(RobotSide.LEFT, transform);
      transformFromTekscanMeasurementToAnkleZUpFrames.put(RobotSide.RIGHT, new RigidBodyTransform(transform));
   }
   
   static
   {
      contactSensors.put(RobotSide.LEFT, new LinkedHashMap<String, LinkedHashMap<String,ContactSensorType>>());
      contactSensors.get(RobotSide.LEFT).put("LeftAnkle",new LinkedHashMap<String,ContactSensorType>());
      contactSensors.get(RobotSide.LEFT).get("LeftAnkle").put(footContactSensorNames.get(RobotSide.LEFT), ContactSensorType.SOLE);
      
      //@TODO Need a bit more work before multiple contact sensors can be added to a single rigid body.
//      contactSensors.get(RobotSide.LEFT).get("LeftAnkle").put("LeftToeContactSensor", ContactSensorType.TOE);
//      contactSensors.get(RobotSide.LEFT).get("LeftAnkle").put("LeftHeelContactSensor", ContactSensorType.HEEL);
      
      contactSensors.put(RobotSide.RIGHT, new LinkedHashMap<String, LinkedHashMap<String,ContactSensorType>>());
      contactSensors.get(RobotSide.RIGHT).put("RightAnkle",new LinkedHashMap<String,ContactSensorType>());
      contactSensors.get(RobotSide.RIGHT).get("RightAnkle").put(footContactSensorNames.get(RobotSide.RIGHT), ContactSensorType.SOLE);
      
      //@TODO Need a bit more work before multiple contact sensors can be added to a single rigid body.      
//      contactSensors.get(RobotSide.RIGHT).get("RightAnkle").put("RightToeContactSensor", ContactSensorType.TOE);
//      contactSensors.get(RobotSide.RIGHT).get("RightAnkle").put("RightHeelContactSensor", ContactSensorType.HEEL);
   }

   /**
    * PointCloud Parameters
    */
   //Make pointCloudParameters null to not use point cloud in UI.
   private final DRCRobotPointCloudParameters[] pointCloudParamaters = new DRCRobotPointCloudParameters[1];
   public static final int POINT_CLOUD_SENSOR_ID = 0;
   private static final String pointCloudSensorName = "/v1/Ibeo_sensor";
   private static final String pointCloudTopic = "/v1/Ensenso/Points_in_world";
   
   private static final SideDependentList<String> wristForceSensorNames = new SideDependentList<String>("LeftForearmSupinator", "RightForearmSupinator");
   
   private static int foreheadCameraId = 0;
   private static int leftHazardCameraId = 1;
   private static int rightHazardCameraId = 2;
   private static int primaryCameraId = foreheadCameraId;
   
   private static final String headLinkName = "/v1/Head";
   private final DRCRobotCameraParameters[] cameraParamaters = new DRCRobotCameraParameters[3];
   
   private static final String foreheadCameraName = "/v1/HeadWebcam___default__";
   private static final String foreheadCameraInfo = "/head/camera_info";
   private static final String foreheadCameraTopic = "/head/image_color/compressed";
   
   private static final String leftStereoCameraName = "/v1/LeftHazardCamera___default__";
   private static final String leftCameraTopic = "/v1/LeftHazardCamera/compressed";
   
   private static final String rightStereoCameraName ="/v1/RightHazardCamera___default__";
   private static final String rightCameraTopic = "/v1/rightHazardCamera/compressed";
   
   
   private static final String rightTrunkIMUSensor = "v1Trunk_RightIMU";
   private static final String leftTrunkIMUSensor = "v1Trunk_LeftIMU";
   private static final String leftPelvisIMUSensor = "v1Pelvis_LeftIMU";
   private static final String rightPelvisIMUSensor = "v1Pelvis_RightIMU";
   private static final RigidBodyTransform transformFromHeadToCamera = new RigidBodyTransform();
   static
   {
      transformFromHeadToCamera.setEuler(0.0, 0.0, 0.0);
      transformFromHeadToCamera.setTranslation(0.0,-0.09,0.04);
   }
   
   private static final HashMap<String, Integer> imuUSBSerialIds = new HashMap<>();
   static
   {
      imuUSBSerialIds.put(leftPelvisIMUSensor, 622730571);
      imuUSBSerialIds.put(rightPelvisIMUSensor, 622730566);
      imuUSBSerialIds.put(leftTrunkIMUSensor, 622730569);
      imuUSBSerialIds.put(rightTrunkIMUSensor, 622709817);
   }
   
   // Use this until sim can handle multiple IMUs
//    public static final String[] imuSensorsToUse = {leftPelvisIMUSensor, rightPelvisIMUSensor};
//   public static final String[] imuSensorsToUse = {leftPelvisIMUSensor};
   public static final String[] imuSensorsToUse = {leftPelvisIMUSensor};
   
   public ValkyrieSensorInformation()
   {
      cameraParamaters[0] = new DRCRobotCameraParameters(null, foreheadCameraName,foreheadCameraTopic,headLinkName,foreheadCameraInfo,transformFromHeadToCamera, foreheadCameraId);
      cameraParamaters[1] = new DRCRobotCameraParameters(RobotSide.LEFT, leftStereoCameraName,leftCameraTopic,headLinkName,leftHazardCameraId);
      cameraParamaters[2] = new DRCRobotCameraParameters(RobotSide.RIGHT, rightStereoCameraName,rightCameraTopic,headLinkName,rightHazardCameraId);
      if(pointCloudParamaters.length > 0)
      {
         pointCloudParamaters[POINT_CLOUD_SENSOR_ID] = new DRCRobotPointCloudParameters(pointCloudSensorName,pointCloudTopic,headLinkName,POINT_CLOUD_SENSOR_ID);
      }
   }
   
   public static String getUrdfFeetForceSensorName(RobotSide side)
   {
      return urdfFeetForceSensorNames.get(side);
   }
   
   public static String getUrdfTekscanFeetForceSensorName(RobotSide side)
   {
      return urdfTekscanSensorNames.get(side);
   }
   
   public HashMap<String, Integer> getImuUSBSerialIds()
   {
      return imuUSBSerialIds;
   }

   @Override
   public String[] getIMUSensorsToUseInStateEstimator()
   {
      return imuSensorsToUse;
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
      return wristForceSensorNames;
   }

   @Override
   public String getPrimaryBodyImu()
   {
      return leftPelvisIMUSensor;
   }
   
   public RigidBodyTransform getTransformFromAnkleURDFFrameToZUpFrame(RobotSide robotSide)
   {
      return transformFromSixAxisMeasurementToAnkleZUpFrames.get(robotSide);
   }

   @Override
   public DRCRobotCameraParameters[] getCameraParameters()
   {
      return cameraParamaters;
   }

   @Override
   public DRCRobotCameraParameters getCameraParameters(int sensorId)
   {
      return cameraParamaters[sensorId];
   }

   @Override
   public DRCRobotLidarParameters[] getLidarParameters()
   {
      return new DRCRobotLidarParameters[0];
   }
   
   @Override
   public DRCRobotLidarParameters getLidarParameters(int sensorId)
   {
      return null;
   }

   @Override
   public DRCRobotPointCloudParameters[] getPointCloudParameters()
   {
      return pointCloudParamaters;
   }

   @Override
   public DRCRobotPointCloudParameters getPointCloudParameters(int sensorId)
   {
      return pointCloudParamaters[sensorId];
   }
   
   private void sensorFramesToTrack(DRCRobotSensorParameters[] sensorParams, ArrayList<String> holder)
   {
      for(int i = 0; i < sensorParams.length; i++)
      {
         if(sensorParams[i].getPoseFrameForSdf() != null)
         {
            holder.add(sensorParams[i].getPoseFrameForSdf());
         }
      }
   }
   
   @Override
   public String[] getSensorFramesToTrack()
   {
      ArrayList<String> sensorFramesToTrack = new ArrayList<String>();
      sensorFramesToTrack(cameraParamaters,sensorFramesToTrack);
      String[] sensorFramesToTrackAsPrimitive = new String[sensorFramesToTrack.size()];
      sensorFramesToTrack.toArray(sensorFramesToTrackAsPrimitive);
      return sensorFramesToTrackAsPrimitive;
   }

   @Override
   public boolean setupROSLocationService()
   {
      return false;
   }

   @Override
   public boolean setupROSParameterSetters()
   {
      return false;
   }

   @Override
   public boolean isMultisenseHead()
   {
      return false;
   }

   @Override
   public ReferenceFrame getHeadIMUFrameWhenLevel()
   {
      return null;
   }

   public String getRightTrunkIMUSensor()
   {
      return rightTrunkIMUSensor;
   }

   public String getLeftTrunkIMUSensor()
   {
      return leftTrunkIMUSensor;
   }

   public String getLeftPelvisIMUSensor()
   {
      return leftPelvisIMUSensor;
   }

   public String getRightPelvisIMUSensor()
   {
      return rightPelvisIMUSensor;
   }

   @Override
   public SideDependentList<String> getFeetContactSensorNames()
   {
      return footContactSensorNames;
   }

   @Override
   public ArrayList<Triplet<String, String, RigidBodyTransform>> getStaticTransformsForRos()
   {
      return null;
   }
}
