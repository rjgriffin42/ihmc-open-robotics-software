package us.ihmc.valkyrie.paramaters;

import java.util.HashMap;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotCameraParamaters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotLidarParamaters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPointCloudParamaters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotSensorInformation;
import us.ihmc.graphics3DAdapter.camera.VideoSettingsFactory;
import us.ihmc.graphics3DAdapter.camera.VideoSettingsH264LowLatency;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

public class ValkyrieSensorInformation implements DRCRobotSensorInformation
{

   public static final String[] forceSensorNames = { "LeftAnkle", "RightAnkle", "LeftForearmSupinator", "RightForearmSupinator" };
   private static final SideDependentList<String> feetForceSensorNames = new SideDependentList<String>("LeftAnkle", "RightAnkle");
   private static final SideDependentList<String> urdfFeetForceSensorNames = new SideDependentList<>("/v1/LeftLeg6Axis_Offset", "/v1/RightLeg6Axis_Offset");

   public static final boolean USE_JSC_FOOT_MASS_TARING = false;
   public static final boolean USE_HOME_MADE_FOOT_SENSOR_TARRING = true;

   private static final SideDependentList<SpatialForceVector> footForceSensorTareOffsets;
   static
   {
//      SpatialForceVector leftFootForceSensorTareOffset_20140406 = new SpatialForceVector(null, new double[] { 7.1, -23.7, 3.8, 186.2, 319.7, 1067.7 });
//      SpatialForceVector rightFootForceSensorTareOffset_20140406 = new SpatialForceVector(null, new double[] { -1.08, -2.79, -9.63, 38.05, 7.35, -109.3 });
//      SpatialForceVector leftFootForceSensorTareOffset_zero = new SpatialForceVector(null, new double[6]);
//      SpatialForceVector rightFootForceSensorTareOffset_zero = new SpatialForceVector(null, new double[6]); 

      SpatialForceVector leftFootForceSensorTareOffset_20140512 = new SpatialForceVector(null, new double[] { 7.02, -23.79, 3.09, 189.6, 322.5, 1081.0 });
      SpatialForceVector rightFootForceSensorTareOffset_20140512 = new SpatialForceVector(null, new double[] {-1.12, -2.46, -8.94, 27.54, 3.70, -101.7});
      
      footForceSensorTareOffsets = new SideDependentList<SpatialForceVector>(leftFootForceSensorTareOffset_20140512, rightFootForceSensorTareOffset_20140512);
   }

   public static final SideDependentList<Transform3D> transformFromMeasurementToAnkleZUpFrames = new SideDependentList<>();
   static
   {     
      Transform3D translateForwardAndDownOnFoot = new Transform3D();
      translateForwardAndDownOnFoot.setTranslation(new Vector3d(0.02150, 0.0, -0.058547));  //from Will's CAD measurement
      
      Transform3D rotYBy7dot5 = new Transform3D();
      rotYBy7dot5.rotY(-Math.PI/24.0);
      
      Transform3D rotXByPi = new Transform3D();
      rotXByPi.rotX(Math.PI);
      
      Transform3D rotateZ60Degrees = new Transform3D();
      rotateZ60Degrees.rotZ(-Math.PI/3.0);
      
      Transform3D leftTransform = new Transform3D();
      leftTransform.mul(translateForwardAndDownOnFoot);
      leftTransform.mul(rotYBy7dot5);
      leftTransform.mul(rotateZ60Degrees);
      leftTransform.mul(rotXByPi);

      transformFromMeasurementToAnkleZUpFrames.put(RobotSide.LEFT, leftTransform);
      transformFromMeasurementToAnkleZUpFrames.put(RobotSide.RIGHT, new Transform3D(leftTransform));
   }

   /**
    * PointCloud Parameters
    */
   private final DRCRobotPointCloudParamaters[] pointCloudParamaters = new DRCRobotPointCloudParamaters[1];
   private final int ibeoId = 0;
   private static final String ibeoSensorName = "/v1/Ibeo_sensor";
   private static final String ibeoTopic = "/ibeo/points";
   
   private static final SideDependentList<String> wristForceSensorNames = new SideDependentList<String>("LeftForearmSupinator", "RightForearmSupinator");
   
   private static int forheadCameraId = 0;
   private static int leftHazardCameraId = 1;
   private static int rightHazardCameraId = 2;
   private static int primaryCameraId = forheadCameraId;
   
   private static final String headLinkName = "/v1/Head";
   private final DRCRobotCameraParamaters[] cameraParamaters = new DRCRobotCameraParamaters[3];
   
   private static final String forheadCameraName = "/v1/HeadWebcam___default__";
   private static final String forheadCameraTopic = "/forhead/image_raw/compressed";
   
   private static final String leftStereoCameraName = "/v1/LeftHazardCamera___default__";
   private static final String leftCameraTopic = "/v1/LeftHazardCamera/compressed";
   
   private static final String rightStereoCameraName ="/v1/RightHazardCamera___default__";
   private static final String rightCameraTopic = "/v1/rightHazardCamera/compressed";
   
   
   private static final String rightTrunkIMUSensor = "v1Trunk_RightIMU";
   private static final String leftTrunkIMUSensor = "v1Trunk_LeftIMU";
   private static final String leftPelvisIMUSensor = "v1Pelvis_LeftIMU";
   private static final String rightPelvisIMUSensor = "v1Pelvis_RightIMU";
   private static final String fakePelvisIMUSensor = "v1Pelvis_SimulatedIMU";
   
   private static final HashMap<String, Integer> imuUSBSerialIds = new HashMap<>();
   static
   {
      imuUSBSerialIds.put("v1Pelvis_LeftIMU", 622730566);
      imuUSBSerialIds.put("v1Pelvis_RightIMU", 622730571);
   }
   
   // Use this until sim can handle multiple IMUs
    public static final String[] imuSensorsToUse = {leftPelvisIMUSensor, rightPelvisIMUSensor};
//   public static final String[] imuSensorsToUse = {leftPelvisIMUSensor};
//   public static final String[] imuSensorsToUse = {rightPelvisIMUSensor};
   
   public ValkyrieSensorInformation()
   {
      VideoSettingsH264LowLatency videoSetting = VideoSettingsFactory.get32kBitSettingsWide();
      cameraParamaters[0] = new DRCRobotCameraParamaters(forheadCameraName,forheadCameraTopic,headLinkName,videoSetting,forheadCameraId);
      cameraParamaters[1] = new DRCRobotCameraParamaters(leftStereoCameraName,leftCameraTopic,headLinkName,videoSetting,leftHazardCameraId);
      cameraParamaters[2] = new DRCRobotCameraParamaters(rightStereoCameraName,rightCameraTopic,headLinkName,videoSetting,rightHazardCameraId);
      pointCloudParamaters[ibeoId] = new DRCRobotPointCloudParamaters(ibeoSensorName,ibeoTopic,null,null, ibeoId);
   }
   
   public static String getUrdfFeetForceSensorName(RobotSide side)
   {
      return urdfFeetForceSensorNames.get(side);
   }
   
   public HashMap<String, Integer> getImuUSBSerialIds()
   {
      return imuUSBSerialIds;
   }
   
   @Override
   public String[] getIMUSensorsToUse()
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

   public static SpatialForceVector getFootForceSensorTareOffset(RobotSide robotSide)
   {
      return footForceSensorTareOffsets.get(robotSide);
   }
   
   public Transform3D getTransformFromAnkleURDFFrameToZUpFrame(RobotSide robotSide)
   {
      return transformFromMeasurementToAnkleZUpFrames.get(robotSide);
   }

   @Override
   public DRCRobotCameraParamaters[] getCameraParameters()
   {
      return cameraParamaters;
   }

   @Override
   public DRCRobotCameraParamaters getPrimaryCameraParameters()
   {
      return cameraParamaters[primaryCameraId];
   }

   @Override
   public DRCRobotLidarParamaters[] getLidarParameters()
   {
      return null;
   }

   @Override
   public DRCRobotLidarParamaters getPrimaryLidarParameters()
   {
      return null;
   }

   @Override
   public DRCRobotPointCloudParamaters[] getPointCloudParameters()
   {
      return pointCloudParamaters;
   }

   @Override
   public DRCRobotPointCloudParamaters getPrimaryPointCloudParameters()
   {
      return pointCloudParamaters[ibeoId];
   }
}
