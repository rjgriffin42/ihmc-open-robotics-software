package us.ihmc.commonWalkingControlModules.momentumBasedController;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.MatrixYoVariableConversionTools;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.ops.CommonOps;
import us.ihmc.utilities.CheckTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author twan
 *         Date: 4/22/13
 */
public class MomentumSolver3 implements MomentumSolverInterface
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final CentroidalMomentumMatrix centroidalMomentumMatrix;
   private final DenseMatrix64F centroidalMomentumMatrixDerivative;
   private final DenseMatrix64F previousCentroidalMomentumMatrix;
   private final DoubleYoVariable[][] yoPreviousCentroidalMomentumMatrix;    // to make numerical differentiation rewindable

   private final InverseDynamicsJoint rootJoint; // TODO: make this not be special
   private final InverseDynamicsJoint[] jointsInOrder;

   private final double controlDT;

   private final DenseMatrix64F b;

   private final DenseMatrix64F vdot;

   private final DenseMatrix64F adotV = new DenseMatrix64F(SpatialMotionVector.SIZE, 1);
   private final DenseMatrix64F v;
   private final DenseMatrix64F hdot = new DenseMatrix64F(Momentum.SIZE, 1);

   private final int nDegreesOfFreedom;

   private final LinearSolver<DenseMatrix64F> solver;
   private final MotionConstraintHandler motionConstraintHandler;

   public MomentumSolver3(SixDoFJoint rootJoint, RigidBody elevator, ReferenceFrame centerOfMassFrame, TwistCalculator twistCalculator,
                          LinearSolver<DenseMatrix64F> jacobianSolver, double controlDT, YoVariableRegistry parentRegistry)
   {
      this.rootJoint = rootJoint;
      this.jointsInOrder = ScrewTools.computeSupportAndSubtreeJoints(rootJoint.getSuccessor());

      this.motionConstraintHandler = new MotionConstraintHandler(jointsInOrder);

      this.centroidalMomentumMatrix = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);
      this.previousCentroidalMomentumMatrix = new DenseMatrix64F(centroidalMomentumMatrix.getMatrix().getNumRows(),

            centroidalMomentumMatrix.getMatrix().getNumCols());
      this.centroidalMomentumMatrixDerivative = new DenseMatrix64F(centroidalMomentumMatrix.getMatrix().getNumRows(),
            centroidalMomentumMatrix.getMatrix().getNumCols());
      yoPreviousCentroidalMomentumMatrix = new DoubleYoVariable[previousCentroidalMomentumMatrix.getNumRows()][previousCentroidalMomentumMatrix.getNumCols()];
      MatrixYoVariableConversionTools.populateYoVariables(yoPreviousCentroidalMomentumMatrix, "previousCMMatrix", registry);

      this.controlDT = controlDT;

      nDegreesOfFreedom = ScrewTools.computeDegreesOfFreedom(jointsInOrder);
      this.b = new DenseMatrix64F(SpatialMotionVector.SIZE, 1);

      this.vdot = new DenseMatrix64F(nDegreesOfFreedom, 1);
      this.v = new DenseMatrix64F(nDegreesOfFreedom, 1);

      solver = LinearSolverFactory.pseudoInverse(true);

      parentRegistry.addChild(registry);
      reset();
   }

   public void initialize()
   {
      centroidalMomentumMatrix.compute();
      previousCentroidalMomentumMatrix.set(centroidalMomentumMatrix.getMatrix());
      MatrixYoVariableConversionTools.storeInYoVariables(previousCentroidalMomentumMatrix, yoPreviousCentroidalMomentumMatrix);
   }

   public void reset()
   {
      motionConstraintHandler.reset();
   }

   public void setDesiredJointAcceleration(InverseDynamicsJoint joint, DenseMatrix64F jointAcceleration)
   {
      motionConstraintHandler.setDesiredJointAcceleration(joint, jointAcceleration);
   }

   public void setDesiredSpatialAcceleration(GeometricJacobian jacobian, TaskspaceConstraintData taskspaceConstraintData)
   {
      setDesiredSpatialAcceleration(jacobian.getJointsInOrder(), jacobian, taskspaceConstraintData);
   }

   public void setDesiredSpatialAcceleration(InverseDynamicsJoint[] constrainedJoints, GeometricJacobian jacobian, TaskspaceConstraintData
         taskspaceConstraintData)
   {
      motionConstraintHandler.setDesiredSpatialAcceleration(constrainedJoints, jacobian, taskspaceConstraintData);
   }

   public void compute()
   {
      centroidalMomentumMatrix.compute();
      MatrixYoVariableConversionTools.getFromYoVariables(previousCentroidalMomentumMatrix, yoPreviousCentroidalMomentumMatrix);

      MatrixTools.numericallyDifferentiate(centroidalMomentumMatrixDerivative, previousCentroidalMomentumMatrix, centroidalMomentumMatrix.getMatrix(),
            controlDT);
      MatrixYoVariableConversionTools.storeInYoVariables(previousCentroidalMomentumMatrix, yoPreviousCentroidalMomentumMatrix);

      ScrewTools.packJointVelocitiesMatrix(jointsInOrder, v);
      CommonOps.mult(centroidalMomentumMatrixDerivative, v, adotV);
   }

   private final DenseMatrix64F T = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F alpha1 = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F N = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F beta1 = new DenseMatrix64F(1, 1);

   public void solve(SpatialForceVector momentumRateOfChange)
   {
      T.reshape(SpatialMotionVector.SIZE, 0);
      alpha1.reshape(T.getNumCols(), 1);
      N.reshape(SpatialForceVector.SIZE, SpatialForceVector.SIZE);
      beta1.reshape(N.getNumCols(), 1);

      CommonOps.setIdentity(N);
      momentumRateOfChange.packMatrix(beta1);

      solve(T, alpha1, N, beta1);
   }

   private final DenseMatrix64F sTranspose = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F sTransposeA = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F JpPlus = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F JpPluspp = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F P = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F AP = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F bMinusAJPlusP = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F APPlusbMinusAJpPluspp = new DenseMatrix64F(1, 1);


   public void solve(DenseMatrix64F accelerationSubspace, DenseMatrix64F accelerationMultipliers, DenseMatrix64F momentumSubspace,
                     DenseMatrix64F momentumMultipliers)
   {
      if (accelerationSubspace.getNumCols() > 0)
      {
         TaskspaceConstraintData rootJointTaskspaceConstraintData = new TaskspaceConstraintData();
         DenseMatrix64F rootJointAccelerationMatrix = new DenseMatrix64F(SpatialMotionVector.SIZE, 1);
         CommonOps.mult(accelerationSubspace, accelerationMultipliers, rootJointAccelerationMatrix);
         SpatialAccelerationVector spatialAcceleration = new SpatialAccelerationVector(rootJoint.getFrameAfterJoint(), rootJoint.getFrameBeforeJoint(), rootJoint.getFrameAfterJoint(), rootJointAccelerationMatrix);
         spatialAcceleration.changeBodyFrameNoRelativeAcceleration(rootJoint.getSuccessor().getBodyFixedFrame());
         spatialAcceleration.changeFrameNoRelativeMotion(rootJoint.getSuccessor().getBodyFixedFrame());
         DenseMatrix64F nullspaceMultipliers = new DenseMatrix64F(0, 1);
         DenseMatrix64F selectionMatrix = new DenseMatrix64F(accelerationSubspace.getNumCols(), accelerationSubspace.getNumRows());
         CommonOps.transpose(accelerationSubspace, selectionMatrix);
         rootJointTaskspaceConstraintData.set(spatialAcceleration, nullspaceMultipliers, selectionMatrix);
         setDesiredSpatialAcceleration(rootJoint.getMotionSubspace(), rootJointTaskspaceConstraintData);
      }

      // sTranspose
      sTranspose.reshape(momentumSubspace.getNumCols(), momentumSubspace.getNumRows());
      CommonOps.transpose(momentumSubspace, sTranspose);

      // b
      b.reshape(sTranspose.getNumRows(), 1);
      b.set(momentumMultipliers);
      CommonOps.multAdd(-1.0, sTranspose, adotV, b);

      // sTransposeA
      sTransposeA.reshape(sTranspose.getNumRows(), centroidalMomentumMatrix.getMatrix().getNumCols());
      CommonOps.mult(sTranspose, centroidalMomentumMatrix.getMatrix(), sTransposeA);

      // assemble Jp, pp
      motionConstraintHandler.compute();;
      DenseMatrix64F Jp = motionConstraintHandler.getJacobian();
      DenseMatrix64F pp = motionConstraintHandler.getRightHandSide();

      // J+
      JpPlus.reshape(Jp.getNumCols(), Jp.getNumRows());
      solver.setA(Jp);
      solver.invert(JpPlus);

      // J+p
      JpPluspp.reshape(JpPlus.getNumRows(), pp.getNumCols());
      CommonOps.mult(JpPlus, pp, JpPluspp);

      // bMinusAJPlusp
      bMinusAJPlusP.reshape(b.getNumRows(), b.getNumCols());
      bMinusAJPlusP.set(b);
      CommonOps.multAdd(-1.0, sTransposeA, JpPluspp, bMinusAJPlusP);

      // P
      P.reshape(JpPlus.getNumRows(), Jp.getNumCols());
      CommonOps.setIdentity(P);
      CommonOps.multAdd(-1.0, JpPlus, Jp, P);

      // AP
      AP.reshape(sTransposeA.getNumRows(), P.getNumCols());
      CommonOps.mult(sTransposeA, P, AP);

      // APPlusbMinusAJpPluspp
      APPlusbMinusAJpPluspp.reshape(AP.getNumCols(), bMinusAJPlusP.getNumCols());
      solver.setA(AP);
      solver.solve(bMinusAJPlusP, APPlusbMinusAJpPluspp);

      // vdot
      vdot.set(JpPluspp);
      CommonOps.multAdd(P, APPlusbMinusAJpPluspp, vdot);

      ScrewTools.setDesiredAccelerations(jointsInOrder, vdot);
   }


   public void solve(RootJointAccelerationData rootJointAccelerationData, MomentumRateOfChangeData momentumRateOfChangeData)
   {
      solve(rootJointAccelerationData.getAccelerationSubspace(), rootJointAccelerationData.getAccelerationMultipliers(),
            momentumRateOfChangeData.getMomentumSubspace(), momentumRateOfChangeData.getMomentumMultipliers());
   }

   public void getRateOfChangeOfMomentum(SpatialForceVector rateOfChangeOfMomentumToPack)
   {
      CommonOps.mult(centroidalMomentumMatrix.getMatrix(), vdot, hdot);
      CommonOps.addEquals(hdot, adotV);
      rateOfChangeOfMomentumToPack.set(centroidalMomentumMatrix.getReferenceFrame(), hdot);
   }
}
