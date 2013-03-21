package us.ihmc.commonWalkingControlModules.stateEstimation.processModelElements;


import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.RotationFunctions;

public class OrientationProcessModelElement extends AbstractProcessModelElement
{
   static final int SIZE = 3;
   private final ControlFlowOutputPort<FrameVector> angularVelocityPort;
   private final ControlFlowOutputPort<FrameOrientation> orientationPort;

   private final Vector3d angularVelocity = new Vector3d();
   private final Vector3d tempRotationVector = new Vector3d();
   private final AxisAngle4d tempAxisAngle = new AxisAngle4d();
   private final Matrix3d tempMatrix3d = new Matrix3d();

   private final Quat4d quaternionDelta = new Quat4d();
   private final Quat4d quaternion = new Quat4d();

   public OrientationProcessModelElement(ControlFlowOutputPort<FrameVector> angularVelocityPort, ControlFlowOutputPort<FrameOrientation> orientationPort)
   {
      super(SIZE, 2, 1);
      this.angularVelocityPort = angularVelocityPort;
      this.orientationPort = orientationPort;

      stateMatrixBlocks.put(angularVelocityPort, new DenseMatrix64F(SIZE, SIZE));
      stateMatrixBlocks.put(orientationPort, new DenseMatrix64F(SIZE, SIZE));

      computeAngularVelocityStateMatrixBlock();
   }

   public void computeMatrixBlocks()
   {
      computeOrientationStateMatrixBlock();
   }

   private void computeOrientationStateMatrixBlock()
   {
      angularVelocityPort.getData().getVector(angularVelocity);
      angularVelocity.scale(-0.5);
      MatrixTools.toTildeForm(tempMatrix3d, angularVelocity);
      MatrixTools.setDenseMatrixFromMatrix3d(0, 0, tempMatrix3d, stateMatrixBlocks.get(orientationPort));
   }

   private void computeAngularVelocityStateMatrixBlock()
   {
      CommonOps.setIdentity(stateMatrixBlocks.get(angularVelocityPort));
   }

   public void propagateState(double dt)
   {
      angularVelocityPort.getData().getVector(angularVelocity);
      orientationPort.getData().getQuaternion(quaternion);

      tempRotationVector.set(angularVelocity);
      tempRotationVector.scale(dt);

      RotationFunctions.setAxisAngleBasedOnRotationVector(tempAxisAngle, tempRotationVector);

      quaternionDelta.set(tempAxisAngle);
      quaternion.mul(quaternionDelta);
      quaternion.normalize();    // the previous operation should preserve norm, so this might not be necessary every step
      orientationPort.getData().set(quaternion);
   }

   public void correctState(DenseMatrix64F correction)
   {
      orientationPort.getData().getQuaternion(quaternion);
      MatrixTools.extractTuple3dFromEJMLVector(tempRotationVector, correction, 0);
      RotationFunctions.setAxisAngleBasedOnRotationVector(tempAxisAngle, tempRotationVector);
      quaternionDelta.set(tempAxisAngle);
      quaternion.mul(quaternionDelta);
      orientationPort.getData().set(quaternion);
   }
}
