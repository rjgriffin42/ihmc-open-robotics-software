package us.ihmc.darpaRoboticsChallenge.calib;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LimbName;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.*;

/**
 *
 */
public class KinematicCalibrationHeadLoopResidual implements FunctionNtoM
{
   // which side of the robot is being optimized
   boolean isLeft;

   //robot model and data
   private final SDFFullRobotModel fullRobotModel;
   private final ArrayList<Map<String, Object>> qdata;
   private final ArrayList<Map<String, Double>> q;
   private final List<String> calJointNames;

   //local data buffer
   Map<String, Double> qoffset = new HashMap<>(), qbuffer = new HashMap<>();

   IntrinsicParameters intrinsic;
   PlanarCalibrationTarget calibGrid;

   // normial orientation of the target.  only rotation around y is optimized
   public static final Matrix3d TARGET_LEFT_ROT = new Matrix3d(
         -1.0, 0.0, 0.0,
         0.0, 0.0, 1.0,
         0.0, 1.0, 0.0);

   public static final Matrix3d TARGET_RIGHT_ROT = new Matrix3d(
          0, 1, 0,
          0, 0,-1,
         -1, 0, 0);


   private Matrix3d targetRotation;

   public final RobotSide activeSide;

   public KinematicCalibrationHeadLoopResidual(SDFFullRobotModel fullRobotModel,
                                               boolean isLeft,
                                               IntrinsicParameters intrinsic,
                                               PlanarCalibrationTarget calibGrid,
                                               ArrayList<Map<String, Object>> qdata,
                                               ArrayList<Map<String, Double>> q)
   {
      this.fullRobotModel = fullRobotModel;
      this.qdata = qdata;
      this.q = q;
      this.intrinsic = intrinsic;
      this.calibGrid = calibGrid;
      this.isLeft = isLeft;
      this.activeSide = isLeft ? RobotSide.LEFT : RobotSide.RIGHT;

      if( isLeft ) {
         targetRotation = TARGET_LEFT_ROT;
      } else {
         targetRotation = TARGET_RIGHT_ROT;
      }

      this.calJointNames = getOrderedArmJointsNames(fullRobotModel, isLeft);

   }

   public static List<String> getOrderedArmJointsNames(SDFFullRobotModel fullRobotModel, boolean isLeft)
   {
      final OneDoFJoint[] joints = fullRobotModel.getOneDoFJoints();

      String filter = isLeft ? "l_arm" : "r_arm";

      ArrayList<OneDoFJoint> armJoints = new ArrayList<OneDoFJoint>();
      for (int i = 0; i < joints.length; i++)
      {
         if (joints[i].getName().contains(filter))
         {
            armJoints.add(joints[i]);
         } else if (joints[i].getName().contains("neck"))
         {
            armJoints.add(joints[i]);
         }

      }
      List<String> ret = CalibUtil.toStringArrayList(armJoints);
      Collections.sort(ret);
      return ret;
   }

   @Override
   public void process(double[] input, double[] output)
   {
      //convert input into map
      int inputCounter = 0;
      for (int i = 0; i < calJointNames.size(); i++)
         qoffset.put(calJointNames.get(i), input[inputCounter++]);

      Transform3D targetToEE = computeTargetToEE(input, inputCounter,isLeft);

      ReferenceFrame cameraFrame = fullRobotModel.getCameraFrame("stereo_camera_left");
      Transform3D imageToCamera = new Transform3D(new double[]{0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 1});
      ReferenceFrame cameraImageFrame = ReferenceFrame.
            constructBodyFrameWithUnchangingTransformToParent("cameraImage", cameraFrame, imageToCamera);

      //compute error
      int offset = 0;
      for (int i = 0; i < qdata.size(); i++)
      {
         CalibUtil.addQ(q.get(i), qoffset, qbuffer);
         CalibUtil.setRobotModelFromData(fullRobotModel, qbuffer);

         List<Point2D_F64> observations = (List) qdata.get(i).get(AtlasHeadLoopKinematicCalibrator.CHESSBOARD_DETECTIONS_KEY);

         Transform3D targetToCamera = computeKinematicsTargetToCamera(cameraImageFrame, targetToEE);

         computeError(observations, targetToCamera, output, offset);

         offset += observations.size() * 2;
      }
   }

   public static Transform3D computeTargetToEE(double param[], int offset, boolean isLeft)
   {
      // Apply rotation around Y to the nominal rotation
      Vector3d tran = new Vector3d();

      tran.set(param[offset], param[offset + 1], param[offset + 2]);
      AxisAngle4d axisY = new AxisAngle4d(new Vector3d(0, 1, 0), param[offset + 3]);
      Matrix3d matAxisY = new Matrix3d();
      matAxisY.set(axisY);

      Matrix3d rotFull = new Matrix3d();
      Matrix3d targetRotation = isLeft ? TARGET_LEFT_ROT : TARGET_RIGHT_ROT;
      rotFull.mul(matAxisY, targetRotation);

      Transform3D targetToEE = new Transform3D();
      targetToEE.setTranslation(tran);
      targetToEE.setRotation(rotFull);

      return targetToEE;
   }

   private Transform3D computeKinematicsTargetToCamera(ReferenceFrame cameraImageFrame, Transform3D targetToEE)
   {

      ReferenceFrame activeArmEEFrame = fullRobotModel.getEndEffectorFrame(activeSide, LimbName.ARM);
      ReferenceFrame boardFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("boardFrame", activeArmEEFrame, targetToEE);
      return boardFrame.getTransformToDesiredFrame(cameraImageFrame);
   }

   private void computeError(List<Point2D_F64> observations, Transform3D targetToCamera, double[] output, int offset)
   {

      // Points in chessboard frame
      Point2D_F64 norm = new Point2D_F64();
      Point2D_F64 expectedPixel = new Point2D_F64();

      for (int i = 0; i < calibGrid.points.size(); i++)
      {
         Point2D_F64 p = calibGrid.points.get(i);

         // convert to camera frame
         Point3d p3 = new Point3d(p.x, p.y, 0);
         targetToCamera.transform(p3);

         // convert to pixels
         norm.set(p3.x / p3.z, p3.y / p3.z);
         PerspectiveOps.convertNormToPixel(intrinsic, norm, expectedPixel);

         Point2D_F64 observedPixel = observations.get(i);

         // Errors
         output[i * 2 + offset] = observedPixel.x - expectedPixel.x;
         output[i * 2 + offset + 1] = observedPixel.y - expectedPixel.y;
      }
   }

   public List<String> getCalJointNames()
   {
      return calJointNames;
   }

   @Override
   public int getNumOfInputsN()
   {
      //dim parameter
      return calJointNames.size() + 4;
   }

   @Override
   public int getNumOfOutputsM()
   {
      return qdata.size() * calibGrid.points.size() * 2;
   }
}