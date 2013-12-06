package us.ihmc.darpaRoboticsChallenge.calib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ddogleg.optimization.functions.FunctionNtoM;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LimbName;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class KinematicCalibrationWristLoopResidual implements FunctionNtoM
{
   //robot model and data
   private final SDFFullRobotModel fullRobotModel;
   private final ArrayList<Map<String, Double>> qdata;
   private final ArrayList<String> calJointNames;
   
   //local data buffer
   Map<String, Double> qoffset = new HashMap<>(), qbuffer = new HashMap<>();
   Vector3d constantOffset = new Vector3d();

   
   public KinematicCalibrationWristLoopResidual(SDFFullRobotModel fullRobotModel, final ArrayList<String> calJointNames, ArrayList<Map<String, Double>> qdata)
   {
      this.fullRobotModel = fullRobotModel;
      this.calJointNames = calJointNames;         
      this.qdata = qdata;
   }
   
   public Map<String, Double> prmArrayToJointMap(double[] prm)
   {
      Map<String, Double> qret = new HashMap<>();
      assert(prm.length == calJointNames.size());
      for(int i=0;i<calJointNames.size();i++)
      {
         qret.put(calJointNames.get(i), prm[i]);
      }
      return qret; 
   }
   
   public double[] calcResiduals(double[] input)
   {
      double[] output = new double[getM()];
      process(input, output);
      return output;
   }

   
   @Override 
   public void process(double[] input, double[] output)
   {
      //convert input into map
      int inputCounter=0;
      for(int i=0;i<calJointNames.size();i++)
         qoffset.put(calJointNames.get(i), input[inputCounter++]);

      //remember to change getN()
//    constantOffset.x=input[inputCounter++];
      constantOffset.y=input[inputCounter++];
//    constantOffset.z=input[inputCounter++];
      
      //compute error            
      int outputCounter=0;
      for(int i=0;i<qdata.size();i++)
      {
         CalibUtil.addQ(qdata.get(i),qoffset, qbuffer);
         CalibUtil.setRobotModelFromData(fullRobotModel,qbuffer);
         
//         FramePoint 
//            leftEE=new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.LEFT, LimbName.ARM)  ,+0.01, 0.13,0),
//            rightEE=new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.RIGHT, LimbName.ARM),+0.01,-0.13,0);
         FramePose leftEE  = new FramePose(fullRobotModel.getEndEffectorFrame(RobotSide.LEFT,  LimbName.ARM),new Point3d(+0.01,+0.13,0), CalibUtil.quat0);
         FramePose rightEE = new FramePose(fullRobotModel.getEndEffectorFrame(RobotSide.RIGHT, LimbName.ARM),new Point3d(+0.01,-0.13,0), CalibUtil.quat0);
         leftEE.addPositionInFrame(constantOffset);

         leftEE.changeFrame(ReferenceFrame.getWorldFrame());
         rightEE.changeFrame(ReferenceFrame.getWorldFrame());
         output[outputCounter++] = leftEE.getX()-rightEE.getX();
         output[outputCounter++] = leftEE.getY()-rightEE.getY();
         output[outputCounter++] = leftEE.getZ()-rightEE.getZ();

         if (AtlasKinematicCalibrator.RESIDUAL_DOF==6)
         {

            boolean QUAT_DIFF=false;
            double scaleRadToCM = 1;//0.01/(Math.PI/2); //30deg -> 1cm
            if(QUAT_DIFF)
            {
              Quat4d qErr = leftEE.getOrientationCopy().getQuaternion();
              qErr.inverse();
              qErr.mul(rightEE.getOrientationCopy().getQuaternion());
              //qErr.normalize();
              AxisAngle4d axErr = new AxisAngle4d();
              axErr.set(qErr);
              output[outputCounter++] = scaleRadToCM*axErr.getX()*axErr.getAngle();
              output[outputCounter++] = scaleRadToCM*axErr.getY()*axErr.getAngle();
              output[outputCounter++] = scaleRadToCM*axErr.getZ()*axErr.getAngle();
            }
            else
            {
               assert(leftEE.getReferenceFrame()==rightEE.getReferenceFrame());
               AxisAngle4d axLeft = new AxisAngle4d(), axRight = new AxisAngle4d();
               axLeft.set(leftEE.getOrientationCopy().getQuaternion());
               axRight.set(rightEE.getOrientationCopy().getQuaternion());
               
               if( axLeft.angle < 0 ) {
                  axLeft.angle *= -1;
                  axLeft.x *= -1;
                  axLeft.y *= -1;
                  axLeft.z *= -1;
               }
               if( axRight.angle < 0 ) {
                  axRight.angle *= -1;
                  axRight.x *= -1;
                  axRight.y *= -1;
                  axRight.z *= -1;
               }
               
               output[outputCounter++] = scaleRadToCM* (axLeft.x*axLeft.angle - axRight.x*axRight.angle); 
               output[outputCounter++] = scaleRadToCM* (axLeft.y*axLeft.angle - axRight.y*axRight.angle);
               output[outputCounter++] = scaleRadToCM* (axLeft.z*axLeft.angle - axRight.z*axRight.angle);
            }
          }
      }
   }
   
   
   @Override
   public int getN()
   {
      //dim parameter
      return calJointNames.size() + 1; //+3 for constant offset
   }
   
   @Override
   public int getM()
   {
      //dim error
       return qdata.size()*AtlasKinematicCalibrator.RESIDUAL_DOF;
   }
}