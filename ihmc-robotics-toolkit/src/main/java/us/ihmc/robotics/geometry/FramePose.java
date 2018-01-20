package us.ihmc.robotics.geometry;

import java.util.Random;

import us.ihmc.euclid.axisAngle.interfaces.AxisAngleBasics;
import us.ihmc.euclid.axisAngle.interfaces.AxisAngleReadOnly;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.matrix.interfaces.RotationMatrixReadOnly;
import us.ihmc.euclid.referenceFrame.FrameGeometryObject;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.exceptions.ReferenceFrameMismatchException;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFrameQuaternionBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameQuaternionBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FrameQuaternionReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DBasics;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionBasics;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.robotics.random.RandomGeometry;

public class FramePose extends FrameGeometryObject<FramePose, Pose3D> implements FramePose3DReadOnly
{
   private final Pose3D pose;

   private final FixedFramePoint3DBasics positionPart = new FixedFramePoint3DBasics()
   {
      @Override
      public void setX(double x)
      {
         pose.setX(x);
      }

      @Override
      public void setY(double y)
      {
         pose.setY(y);
      }

      @Override
      public void setZ(double z)
      {
         pose.setZ(z);
      }

      @Override
      public ReferenceFrame getReferenceFrame()
      {
         return referenceFrame;
      }

      @Override
      public double getX()
      {
         return pose.getX();
      }

      @Override
      public double getY()
      {
         return pose.getY();
      }

      @Override
      public double getZ()
      {
         return pose.getZ();
      }
   };

   private final FixedFrameQuaternionBasics orientationPart = new FixedFrameQuaternionBasics()
   {

      @Override
      public void setUnsafe(double qx, double qy, double qz, double qs)
      {
         pose.getOrientation().setUnsafe(qx, qy, qz, qs);
      }

      @Override
      public ReferenceFrame getReferenceFrame()
      {
         return referenceFrame;
      }

      @Override
      public double getX()
      {
         return pose.getOrientation().getX();
      }

      @Override
      public double getY()
      {
         return pose.getOrientation().getY();
      }

      @Override
      public double getZ()
      {
         return pose.getOrientation().getZ();
      }

      @Override
      public double getS()
      {
         return pose.getOrientation().getS();
      }
   };

   public FramePose()
   {
      this(new FramePoint3D(), new FrameQuaternion());
   }

   public FramePose(ReferenceFrame referenceFrame)
   {
      this(referenceFrame, new Pose3D());
   }

   public FramePose(ReferenceFrame referenceFrame, Pose3DReadOnly pose)
   {
      super(referenceFrame, new Pose3D(pose));
      this.pose = getGeometryObject();
   }

   public FramePose(FramePoint3DReadOnly position, FrameQuaternion orientation)
   {
      this(position.getReferenceFrame(), new Pose3D(position, orientation));

      if (position.getReferenceFrame() != orientation.getReferenceFrame())
      {
         throw new ReferenceFrameMismatchException("FramePose: The position frame (" + position.getReferenceFrame() + ") does not match the orientation frame ("
               + orientation.getReferenceFrame() + ")");
      }
   }

   public FramePose(ReferenceFrame referenceFrame, Tuple3DReadOnly position, QuaternionReadOnly orientation)
   {
      this(referenceFrame, new Pose3D());
      set(position, orientation);
   }

   public FramePose(FramePose framePose)
   {
      this(framePose.getReferenceFrame(), new Pose3D(framePose.getGeometryObject()));
   }

   public FramePose(ReferenceFrame referenceFrame, RigidBodyTransform transform)
   {
      this(referenceFrame);
      set(transform);
   }

   public FramePose(ReferenceFrame referenceFrame, Tuple3DReadOnly point3d, AxisAngleReadOnly axisAngle4d)
   {
      this(referenceFrame);
      set(point3d, axisAngle4d);
   }

   public static FramePose generateRandomFramePose(Random random, ReferenceFrame referenceFrame, double maxAbsoluteX, double maxAbsoluteY, double maxAbsoluteZ)
   {
      return new FramePose(referenceFrame, RandomGeometry.nextPoint3D(random, maxAbsoluteX, maxAbsoluteY, maxAbsoluteZ), RandomGeometry.nextQuaternion(random));
   }

   public void setX(double x)
   {
      pose.setX(x);
   }

   public void setY(double y)
   {
      pose.setY(y);
   }

   public void setZ(double z)
   {
      pose.setZ(z);
   }

   public void setPose(FramePose pose)
   {
      set(pose);
   }

   public void set(FramePoint3DReadOnly position, FrameQuaternionReadOnly orientation)
   {
      setPosition(position);
      setOrientation(orientation);
   }

   public void set(Tuple3DReadOnly position, QuaternionReadOnly orientation)
   {
      pose.set(position, orientation);
   }

   private void set(Tuple3DReadOnly position, AxisAngleReadOnly orientation)
   {
      pose.set(position, orientation);
   }

   public void set(RigidBodyTransform transform)
   {
      pose.set(transform);
   }

   public void setIncludingFrame(FramePoint3DReadOnly position, FrameQuaternionReadOnly orientation)
   {
      position.checkReferenceFrameMatch(orientation);
      referenceFrame = position.getReferenceFrame();
      pose.set(position, orientation);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, Point3DReadOnly position, QuaternionReadOnly orientation)
   {
      set(position, orientation);
      this.referenceFrame = referenceFrame;
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, RigidBodyTransform transform)
   {
      pose.set(transform);
      this.referenceFrame = referenceFrame;
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, Pose3DReadOnly pose)
   {
      this.pose.set(pose);
      this.referenceFrame = referenceFrame;
   }

   public void setPosition(FramePoint3DReadOnly framePoint)
   {
      checkReferenceFrameMatch(framePoint);
      pose.setPosition(framePoint);
   }

   public void setPosition(Tuple3DReadOnly position)
   {
      pose.setPosition(position);
   }

   public void setPosition(double x, double y, double z)
   {
      pose.setPosition(x, y, z);
   }

   public void setOrientation(double qx, double qy, double qz, double qs)
   {
      pose.setOrientation(qx, qy, qz, qs);
   }

   public void setOrientation(QuaternionReadOnly orientation)
   {
      pose.setOrientation(orientation);
   }

   public void setOrientation(RotationMatrixReadOnly orientation)
   {
      pose.setOrientation(orientation);
   }

   public void setOrientation(AxisAngleReadOnly orientation)
   {
      pose.setOrientation(orientation);
   }

   public void setOrientationYawPitchRoll(double[] yawPitchRoll)
   {
      pose.setOrientationYawPitchRoll(yawPitchRoll);
   }

   public void setOrientationYawPitchRoll(double yaw, double pitch, double roll)
   {
      pose.setOrientationYawPitchRoll(yaw, pitch, roll);
   }

   public void setOrientation(FrameQuaternionReadOnly frameOrientation)
   {
      checkReferenceFrameMatch(frameOrientation);
      pose.setOrientation((QuaternionReadOnly) frameOrientation);
   }

   public void setPosition(FramePoint2DReadOnly position2d)
   {
      pose.setPosition(position2d);
   }

   public void get(Tuple3DBasics tupleToPack, QuaternionBasics quaternionToPack)
   {
      tupleToPack.set(getPosition());
      quaternionToPack.set(getOrientation());
   }

   public void get(Tuple3DBasics tupleToPack, AxisAngleBasics axisAngleToPack)
   {
      tupleToPack.set(getPosition());
      axisAngleToPack.set(getOrientation());
   }

   public void get(RigidBodyTransform transformToPack)
   {
      pose.get(transformToPack);
   }

   public void get(FramePoint3DBasics framePointToPack, FrameQuaternionBasics orientationToPack)
   {
      framePointToPack.setIncludingFrame(positionPart);
      orientationToPack.setIncludingFrame(orientationPart);
   }

   public FixedFramePoint3DBasics getPosition()
   {
      return positionPart;
   }

   public FixedFrameQuaternionBasics getOrientation()
   {
      return orientationPart;
   }

   public void getOrientation(double[] yawPitchRoll)
   {
      pose.getOrientationYawPitchRoll(yawPitchRoll);
   }

   /**
    * Computes and packs the orientation described by the quaternion part of this pose as a rotation
    * vector.
    * <p>
    * WARNING: a rotation vector is different from a yaw-pitch-roll or Euler angles representation.
    * A rotation vector is equivalent to the axis of an axis-angle that is multiplied by the angle
    * of the same axis-angle.
    * </p>
    *
    * @param rotationVectorToPack the vector in which the rotation vector is stored. Modified.
    */
   public void getRotationVector(Vector3DBasics rotationVectorToPack)
   {
      pose.getRotationVector(rotationVectorToPack);
   }

   /**
    * Computes and packs the orientation described by the quaternion part of this pose as a rotation
    * vector. The reference frame of the argument is changed to {@code this.referenceFrame}.
    * <p>
    * WARNING: a rotation vector is different from a yaw-pitch-roll or Euler angles representation.
    * A rotation vector is equivalent to the axis of an axis-angle that is multiplied by the angle
    * of the same axis-angle.
    * </p>
    *
    * @param frameRotationVectorToPack the vector in which the rotation vector and the reference
    *           frame of this pose are stored. Modified.
    */
   public void getRotationVector(FrameVector3DBasics frameRotationVectorToPack)
   {
      frameRotationVectorToPack.setToZero(getReferenceFrame());
      pose.getRotationVector(frameRotationVectorToPack);
   }

   /**
    * Normalizes the quaternion part of this pose to ensure it is a unit-quaternion describing a
    * proper orientation.
    * <p>
    * Edge cases:
    * <ul>
    * <li>if the quaternion contains {@link Double#NaN}, this method is ineffective.
    * </ul>
    * </p>
    */
   public void normalizeQuaternion()
   {
      pose.normalizeQuaternion();
   }

   /**
    * Normalizes the quaternion part of this pose and then limits the angle of the rotation it
    * represents to be &in; [-<i>pi</i>;<i>pi</i>].
    * <p>
    * Edge cases:
    * <ul>
    * <li>if the quaternion contains {@link Double#NaN}, this method is ineffective.
    * </ul>
    * </p>
    */
   public void normalizeQuaternionAndLimitToPi()
   {
      pose.normalizeQuaternionAndLimitToPi();
   }

   public void prependTranslation(double x, double y, double z)
   {
      pose.prependTranslation(x, y, z);
   }

   public void prependTranslation(Tuple3DReadOnly translation)
   {
      pose.prependTranslation(translation);
   }

   public void prependRotation(QuaternionReadOnly rotation)
   {
      pose.prependRotation(rotation);
   }

   public void prependRotation(RotationMatrixReadOnly rotation)
   {
      pose.prependRotation(rotation);
   }

   public void prependRotation(AxisAngleReadOnly rotation)
   {
      pose.prependRotation(rotation);
   }

   public void prependYawRotation(double yaw)
   {
      pose.prependYawRotation(yaw);
   }

   public void prependPitchRotation(double pitch)
   {
      pose.prependPitchRotation(pitch);
   }

   public void prependRollRotation(double roll)
   {
      pose.prependRollRotation(roll);
   }

   public void appendTranslation(double x, double y, double z)
   {
      pose.appendTranslation(x, y, z);
   }

   public void appendTranslation(Tuple3DReadOnly translation)
   {
      pose.appendTranslation(translation);
   }

   public void appendRotation(QuaternionReadOnly rotation)
   {
      pose.appendRotation(rotation);
   }

   public void appendRotation(RotationMatrixReadOnly rotation)
   {
      pose.appendRotation(rotation);
   }

   public void appendYawRotation(double yaw)
   {
      pose.appendYawRotation(yaw);
   }

   public void appendPitchRotation(double pitch)
   {
      pose.appendPitchRotation(pitch);
   }

   public void appendRollRotation(double roll)
   {
      pose.appendRollRotation(roll);
   }

   public double getX()
   {
      return pose.getX();
   }

   public double getY()
   {
      return pose.getY();
   }

   public double getZ()
   {
      return pose.getZ();
   }

   public double getYaw()
   {
      return pose.getYaw();
   }

   public double getPitch()
   {
      return pose.getPitch();
   }

   public double getRoll()
   {
      return pose.getRoll();
   }

   public void interpolate(FramePose framePose1, FramePose framePose2, double alpha)
   {
      checkReferenceFrameMatch(framePose1);
      framePose1.checkReferenceFrameMatch(framePose2);
      pose.interpolate(framePose1.pose, framePose2.pose, alpha);
   }

   public double getPositionDistance(FramePose framePose)
   {
      checkReferenceFrameMatch(framePose);

      return pose.getPosition().distance(framePose.pose.getPosition());
   }

   public double getOrientationDistance(FramePose framePose)
   {
      checkReferenceFrameMatch(framePose);
      return pose.getOrientationDistance(framePose.getOrientation());
   }
}
