package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.util.Arrays;

import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePoint2D;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FramePose2d;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;

public class Handstep
{
   private static int counter = 0;
   private final String id;
   private final RigidBody endEffector;
   private final RobotSide robotSide;
   private final PoseReferenceFrame poseReferenceFrame;
   private final FrameVector3D surfaceNormal;
   private double swingTrajectoryTime;

   public Handstep(RobotSide robotSide, RigidBody endEffector, FramePose framePose, FrameVector3D surfaceNormal, double swingTrajectoryTime)
   {
      this(robotSide, endEffector, new PoseReferenceFrame("Handstep" + counter, framePose), surfaceNormal, swingTrajectoryTime);
   }

   public Handstep(RobotSide robotSide, RigidBody endEffector, PoseReferenceFrame poseReferenceFrame, FrameVector3D surfaceNormal, double swingTrajectoryTime)
   {
      this(createAutomaticID(endEffector), robotSide, endEffector, poseReferenceFrame, surfaceNormal, swingTrajectoryTime);
   }

   public Handstep(String id, RobotSide robotSide, RigidBody endEffector, PoseReferenceFrame poseReferenceFrame, FrameVector3D surfaceNormal,
         double swingTrajectoryTime)
   {
      poseReferenceFrame.getParent().checkIsWorldFrame();

      this.id = id;
      this.robotSide = robotSide;
      this.endEffector = endEffector;
      this.poseReferenceFrame = poseReferenceFrame;
      this.surfaceNormal = surfaceNormal;
      this.swingTrajectoryTime = swingTrajectoryTime;
   }

   public Handstep(Handstep handstep)
   {
      this(handstep.robotSide, handstep.endEffector, handstep.poseReferenceFrame, handstep.surfaceNormal, handstep.swingTrajectoryTime);
   }

   private static String createAutomaticID(RigidBody endEffector)
   {
      return endEffector.getName() + "_" + counter++;
   }

   public ReferenceFrame getPoseReferenceFrame()
   {
      return poseReferenceFrame;
   }

   public ReferenceFrame getParentFrame()
   {
      return poseReferenceFrame.getParent();
   }

   public void setX(double x)
   {
      poseReferenceFrame.setX(x);
   }

   public void setY(double y)
   {
      poseReferenceFrame.setY(y);
   }

   public void setZ(double z)
   {
      poseReferenceFrame.setZ(z);
   }

   public void setPose(Handstep newHandstep)
   {
      poseReferenceFrame.setPoseAndUpdate(newHandstep.poseReferenceFrame);
   }

   public void setPose(FramePose newHandstepPose)
   {
      poseReferenceFrame.setPoseAndUpdate(newHandstepPose);
   }

   public void setPose(FramePoint3D newPosition, FrameOrientation newOrientation)
   {
      poseReferenceFrame.setPoseAndUpdate(newPosition, newOrientation);
   }

   public void setPositionChangeOnlyXY(FramePoint2D position2d)
   {
      poseReferenceFrame.setXYFromPosition2dAndUpdate(position2d);
   }

   public void setSurfaceNormal(FrameVector3D surfaceNormal)
   {
      this.surfaceNormal.set(surfaceNormal);
   }

   public String getId()
   {
      return id;
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public double getX()
   {
      return poseReferenceFrame.getX();
   }

   public double getY()
   {
      return poseReferenceFrame.getY();
   }

   public double getZ()
   {
      return poseReferenceFrame.getZ();
   }

   public double getYaw()
   {
      return poseReferenceFrame.getYaw();
   }

   public double getPitch()
   {
      return poseReferenceFrame.getPitch();
   }

   public double getRoll()
   {
      return poseReferenceFrame.getRoll();
   }

   public void getPose(Point3D pointToPack, Quaternion quaternionToPack)
   {
      poseReferenceFrame.getPose(pointToPack, quaternionToPack);
   }

   public void getPose(RigidBodyTransform transformToPack)
   {
      poseReferenceFrame.getPose(transformToPack);
   }

   public void getPose(FramePoint3D positionToPack, FrameOrientation orientationToPack)
   {
      poseReferenceFrame.getPoseIncludingFrame(positionToPack, orientationToPack);
   }

   public void getPose(FramePose poseToPack)
   {
      poseReferenceFrame.getPoseIncludingFrame(poseToPack);
   }

   public void getPoseReferenceFrameAndUpdate(PoseReferenceFrame poseReferenceFrameToPackAndUpdate)
   {
      poseReferenceFrameToPackAndUpdate.setPoseAndUpdate(poseReferenceFrame);
   }

   public void getSurfaceNormal(FrameVector3D surfaceNormalToPack)
   {
      surfaceNormalToPack.setIncludingFrame(surfaceNormal);
   }

   public void getSurfaceNormal(Vector3D surfaceNormalToPack)
   {
      this.surfaceNormal.get(surfaceNormalToPack);
   }

   public RigidBody getBody()
   {
      return endEffector;
   }

   public void getPosition(Point3D pointToPack)
   {
      poseReferenceFrame.getPosition(pointToPack);
   }

   public void getPositionIncludingFrame(FramePoint3D framePointToPack)
   {
      poseReferenceFrame.getPositionIncludingFrame(framePointToPack);
   }

   public void getOrientation(Quaternion quaternionToPack)
   {
      poseReferenceFrame.getOrientation(quaternionToPack);
   }

   public void getOrientation(RotationMatrix matrixToPack)
   {
      poseReferenceFrame.getOrientation(matrixToPack);
   }

   public void getOrientationIncludingFrame(FrameOrientation frameOrientationToPack)
   {
      poseReferenceFrame.getOrientationIncludingFrame(frameOrientationToPack);
   }

   public void getPose2d(FramePose2d framePose2dToPack)
   {
      poseReferenceFrame.getPose2dIncludingFrame(framePose2dToPack);
   }

   public void getPosition2d(FramePoint2D framePoint2dToPack)
   {
      poseReferenceFrame.getPosition2dIncludingFrame(framePoint2dToPack);
   }

   public void setSwingTrajectoryTime(double swingTime)
   {
      this.swingTrajectoryTime = swingTime;
   }

   public double getSwingTrajectoryTime()
   {
      return swingTrajectoryTime;
   }

   public boolean epsilonEquals(Handstep otherHandstep, double epsilon)
   {
      boolean arePosesEqual = poseReferenceFrame.epsilonEquals(otherHandstep.poseReferenceFrame, epsilon);
      boolean bodiesHaveTheSameName = endEffector.getName().equals(otherHandstep.endEffector.getName());

      return arePosesEqual && bodiesHaveTheSameName;
   }

   public String toString()
   {
      FrameOrientation frameOrientation = new FrameOrientation(poseReferenceFrame);
      frameOrientation.changeFrame(ReferenceFrame.getWorldFrame());

      double[] ypr = frameOrientation.getYawPitchRoll();
      String yawPitchRoll = "YawPitchRoll = " + Arrays.toString(ypr);

      return "id: " + id + " - pose: " + poseReferenceFrame + "\n\tYawPitchRoll= {" + yawPitchRoll + "}";
   }

}
