package us.ihmc.robotics.screwTheory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.matrix.interfaces.RotationMatrixReadOnly;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionBasics;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.robotics.geometry.RotationTools;

public class SixDoFJoint extends AbstractInverseDynamicsJoint implements FloatingInverseDynamicsJoint
{
   private final Quaternion jointRotation = new Quaternion(0.0, 0.0, 0.0, 1.0);
   private final Vector3D jointTranslation = new Vector3D();
   private final Twist jointTwist;
   private final SpatialAccelerationVector jointAcceleration;
   private final SpatialAccelerationVector jointAccelerationDesired;

   private List<Twist> unitTwists;

   private Wrench successorWrench;

   public SixDoFJoint(String name, RigidBody predecessor)
   {
      this(name, predecessor, null);
   }

   public SixDoFJoint(String name, RigidBody predecessor, RigidBodyTransform transformToParent)
   {
      super(name, predecessor, transformToParent);
      jointTwist = new Twist(afterJointFrame, beforeJointFrame, afterJointFrame);
      jointAcceleration = new SpatialAccelerationVector(afterJointFrame, beforeJointFrame, afterJointFrame);
      jointAccelerationDesired = new SpatialAccelerationVector(afterJointFrame, beforeJointFrame, afterJointFrame);
   }

   @Override
   protected void updateJointTransform(RigidBodyTransform jointTransform)
   {
      jointTransform.set(jointRotation, jointTranslation);
   }

   @Override
   public void getJointTwist(Twist twistToPack)
   {
      twistToPack.set(jointTwist);
   }

   @Override
   public void getJointAcceleration(SpatialAccelerationVector accelerationToPack)
   {
      accelerationToPack.set(jointAcceleration);
   }

   @Override
   public void getDesiredJointAcceleration(SpatialAccelerationVector accelerationToPack)
   {
      accelerationToPack.set(jointAccelerationDesired);
   }

   @Override
   public void getTauMatrix(DenseMatrix64F matrix)
   {
      successorWrench.getMatrix(matrix);
   }

   @Override
   public void getVelocityMatrix(DenseMatrix64F matrix, int rowStart)
   {
      jointTwist.getMatrix(matrix, rowStart);
   }

   @Override
   public void getAngularVelocity(Vector3DBasics angularVelocityToPack)
   {
      jointTwist.getAngularPart(angularVelocityToPack);
   }

   @Override
   public void getLinearVelocity(Vector3DBasics linearVelocityToPack)
   {
      jointTwist.getLinearPart(linearVelocityToPack);
   }

   @Override
   public Vector3DReadOnly getLinearVelocityForReading()
   {
      return jointTwist.getLinearPart();
   }

   @Override
   public Vector3DReadOnly getAngularVelocityForReading()
   {
      return jointTwist.getAngularPart();
   }

   @Override
   public void getDesiredAccelerationMatrix(DenseMatrix64F matrix, int rowStart)
   {
      jointAccelerationDesired.getMatrix(matrix, rowStart);
   }

   @Override
   public void setDesiredAccelerationToZero()
   {
      jointAccelerationDesired.setToZero();
   }

   @Override
   public void setSuccessor(RigidBody successor)
   {
      this.successor = successor;
      setMotionSubspace();

      ReferenceFrame successorFrame = successor.getBodyFixedFrame();

      successorWrench = new Wrench(successorFrame, successorFrame);
   }

   @Override
   public void setTorqueFromWrench(Wrench jointWrench)
   {
      successorWrench.checkAndSet(jointWrench);
   }

   @Override
   public int getDegreesOfFreedom()
   {
      return 6;
   }

   @Override
   public void setDesiredAcceleration(DenseMatrix64F matrix, int rowStart)
   {
      jointAccelerationDesired.set(jointAccelerationDesired.getBodyFrame(), jointAccelerationDesired.getBaseFrame(),
                                   jointAccelerationDesired.getExpressedInFrame(), matrix, rowStart);
   }

   @Override
   public void setJointTorque(DenseMatrix64F matrix, int rowStart)
   {
      successorWrench.set(successorWrench.getBodyFrame(), successorWrench.getExpressedInFrame(), matrix, rowStart);
   }

   public void setPositionAndRotation(RigidBodyTransform transform)
   {
      transform.getRotation(jointRotation);
      jointRotation.checkIfUnitary();

      transform.getTranslation(jointTranslation);
   }

   @Override
   public void setRotation(double yaw, double pitch, double roll)
   {
      jointRotation.setYawPitchRoll(yaw, pitch, roll);
   }

   @Override
   public void setRotation(QuaternionReadOnly jointRotation)
   {
      this.jointRotation.set(jointRotation);
   }

   @Override
   public void setRotation(double x, double y, double z, double w)
   {
      jointRotation.set(x, y, z, w);
   }

   @Override
   public void setRotation(RotationMatrixReadOnly jointRotation)
   {
      this.jointRotation.set(jointRotation);
   }

   @Override
   public void setPosition(Tuple3DReadOnly jointTranslation)
   {
      this.jointTranslation.set(jointTranslation);
   }

   @Override
   public void setPosition(double x, double y, double z)
   {
      jointTranslation.set(x, y, z);
   }

   @Override
   public void setJointTwist(Twist jointTwist)
   {
      this.jointTwist.checkAndSet(jointTwist);
   }

   @Override
   public void setAcceleration(SpatialAccelerationVector jointAcceleration)
   {
      this.jointAcceleration.checkAndSet(jointAcceleration);
   }

   @Override
   public void setDesiredAcceleration(SpatialAccelerationVector jointAcceleration)
   {
      jointAccelerationDesired.checkAndSet(jointAcceleration);
   }

   @Override
   public void setWrench(Wrench jointWrench)
   {
      successorWrench.checkAndSet(jointWrench);
   }

   @Override
   public void getRotation(QuaternionBasics rotationToPack)
   {
      rotationToPack.set(jointRotation);
   }

   @Override
   public void getRotation(RotationMatrix rotationToPack)
   {
      rotationToPack.set(jointRotation);
   }

   @Override
   public void getRotation(double[] yawPitchRollToPack)
   {
      jointRotation.getYawPitchRoll(yawPitchRollToPack);
   }

   @Override
   public void getTranslation(Tuple3DBasics translationToPack)
   {
      translationToPack.set(jointTranslation);
   }

   @Override
   public Tuple3DReadOnly getTranslationForReading()
   {
      return jointTranslation;
   }

   @Override
   public QuaternionReadOnly getRotationForReading()
   {
      return jointRotation;
   }

   @Override
   public void getWrench(Wrench wrenchToPack)
   {
      wrenchToPack.set(successorWrench);
   }

   private void setMotionSubspace()
   {
      int nDegreesOfFreedom = getDegreesOfFreedom();

      ArrayList<Twist> unitTwistsInBodyFrame = new ArrayList<>();
      RigidBodyTransform identity = new RigidBodyTransform();

      ReferenceFrame[] intermediateFrames = new ReferenceFrame[nDegreesOfFreedom - 1];
      ReferenceFrame previousFrame = afterJointFrame;
      for (int i = 0; i < nDegreesOfFreedom - 1; i++) // from afterJointFrame to beforeJointFrame
      {
         int index = intermediateFrames.length - i - 1;
         ReferenceFrame frame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("intermediateFrame" + index, previousFrame, identity);
         intermediateFrames[index] = frame;
         previousFrame = frame;
      }

      previousFrame = beforeJointFrame;

      for (int i = 0; i < nDegreesOfFreedom; i++) // from beforeJointFrame to afterJointFrame
      {
         ReferenceFrame frame;
         if (i < nDegreesOfFreedom - 1)
         {
            frame = intermediateFrames[i];
         }
         else
         {
            frame = afterJointFrame;
         }

         DenseMatrix64F twistMatrix = new DenseMatrix64F(nDegreesOfFreedom, 1);
         twistMatrix.set(i, 0, 1.0);
         Twist twist = new Twist(frame, previousFrame, frame, twistMatrix);
         unitTwistsInBodyFrame.add(twist);

         previousFrame = frame;
      }

      unitTwists = Collections.unmodifiableList(unitTwistsInBodyFrame);

      motionSubspace = new GeometricJacobian(this, afterJointFrame);
      motionSubspace.compute();
   }

   @Override
   public void getUnitTwist(int dofIndex, Twist unitTwistToPack)
   {
      if (dofIndex < 0 || dofIndex >= getDegreesOfFreedom())
         throw new ArrayIndexOutOfBoundsException("Illegal index: " + dofIndex + ", was expecting dofIndex in [0, " + getDegreesOfFreedom() + "[.");
      unitTwistToPack.set(unitTwists.get(dofIndex));
   }

   @Override
   public void updateMotionSubspace()
   {
      // empty
   }

   @Override
   public void getConfigurationMatrix(DenseMatrix64F matrix, int rowStart)
   {
      jointRotation.get(rowStart, matrix);
      jointTranslation.get(rowStart + 4, matrix);
   }

   @Override
   public void setConfiguration(DenseMatrix64F matrix, int rowStart)
   {
      jointRotation.set(rowStart, matrix);
      jointTranslation.set(rowStart + 4, matrix);
   }

   @Override
   public void setVelocity(DenseMatrix64F matrix, int rowStart)
   {
      jointTwist.set(jointTwist.getBodyFrame(), jointTwist.getBaseFrame(), jointTwist.getExpressedInFrame(), matrix, rowStart);
   }

   //FIXME: FIX THIS!!!!
   /**
    * The implementation for this method generates garbage and is wrong. Do not use it or fix it.
    *
    * The implementation should be something like this:
    * <p>
    * {@code RigidBodyTransform inverseTransformToRoot = afterJointFrame.getInverseTransformToRoot();}
    * <p>
    * {@code inverseTransformToRoot.transform(linearVelocityInWorld);}
    * <p>
    * {@code jointTwist.setLinearPart(linearVelocityInWorld);}
    *
    * Sylvain
    *
    * @deprecated
    * @param linearVelocityInWorld
    */
   @Deprecated
   public void setLinearVelocityInWorld(Vector3D linearVelocityInWorld)
   {
      Twist newTwist = new Twist(jointTwist.getBodyFrame(), jointTwist.getBaseFrame(), ReferenceFrame.getWorldFrame());
      newTwist.setLinearPart(linearVelocityInWorld);
      newTwist.changeFrame(jointTwist.getExpressedInFrame());
      newTwist.setAngularPart(jointTwist.getAngularPart());

      jointTwist.set(newTwist);
   }

   @Override
   public int getConfigurationMatrixSize()
   {
      int positionSize = 3;

      return RotationTools.QUATERNION_SIZE + positionSize;
   }

   private SixDoFJoint checkAndGetAsSiXDoFJoint(InverseDynamicsJoint originalJoint)
   {
      if (originalJoint instanceof SixDoFJoint)
      {
         return (SixDoFJoint) originalJoint;
      }
      else
      {
         throw new RuntimeException("Cannot set " + getClass().getSimpleName() + " to " + originalJoint.getClass().getSimpleName());
      }
   }

   @Override
   public void setJointPositionVelocityAndAcceleration(InverseDynamicsJoint originalJoint)
   {
      SixDoFJoint sixDoFOriginalJoint = checkAndGetAsSiXDoFJoint(originalJoint);
      setPosition(sixDoFOriginalJoint.jointTranslation);
      setRotation(sixDoFOriginalJoint.jointRotation);

      jointTwist.setAngularPart(sixDoFOriginalJoint.jointTwist.getAngularPart());
      jointTwist.setLinearPart(sixDoFOriginalJoint.jointTwist.getLinearPart());

      jointAcceleration.setAngularPart(sixDoFOriginalJoint.jointAcceleration.getAngularPart());
      jointAcceleration.setLinearPart(sixDoFOriginalJoint.jointAcceleration.getLinearPart());
   }

   @Override
   public void setQddDesired(InverseDynamicsJoint originalJoint)
   {
      SixDoFJoint sixDoFOriginalJoint = checkAndGetAsSiXDoFJoint(originalJoint);
      jointAccelerationDesired.setAngularPart(sixDoFOriginalJoint.jointAccelerationDesired.getAngularPart());
      jointAccelerationDesired.setLinearPart(sixDoFOriginalJoint.jointAccelerationDesired.getLinearPart());
   }

   @Override
   public void calculateJointStateChecksum(GenericCRC32 checksum)
   {
      checksum.update(jointTranslation);
      checksum.update(jointRotation);
      checksum.update(jointTwist.getAngularPart());
      checksum.update(jointTwist.getLinearPart());
      checksum.update(jointAcceleration.getAngularPart());
      checksum.update(jointAcceleration.getLinearPart());
   }

   @Override
   public void calculateJointDesiredChecksum(GenericCRC32 checksum)
   {
      checksum.update(jointAccelerationDesired.getAngularPart());
      checksum.update(jointAccelerationDesired.getLinearPart());
   }

   @Override
   public void getLinearAcceleration(Vector3DBasics linearAccelerationToPack)
   {
      linearAccelerationToPack.setX(jointAcceleration.getLinearPartX());
      linearAccelerationToPack.setY(jointAcceleration.getLinearPartY());
      linearAccelerationToPack.setZ(jointAcceleration.getLinearPartZ());
   }
}
