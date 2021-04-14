package us.ihmc.commonWalkingControlModules.modelPredictiveController.core;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.misc.UnrolledInverseFromMinor_DDRM;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.commands.DiscreteAngularVelocityOrientationCommand;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.ioHandling.MPCContactPlane;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.tools.EfficientMatrixExponentialCalculator;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.matrix.interfaces.CommonMatrix3DBasics;
import us.ihmc.euclid.matrix.interfaces.Matrix3DReadOnly;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FrameOrientation3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.robotics.MatrixMissingTools;

import java.util.ArrayList;
import java.util.List;

// TODO this needs to be cleaned up
public class OrientationDynamicsCalculator
{
   private final DMatrixRMaj gravityVector = new DMatrixRMaj(3, 1);

   private final FrameVector3D desiredNetAngularMomentumRate = new FrameVector3D();
   private final FrameVector3D desiredBodyAngularMomentumRate = new FrameVector3D();

   private final FrameVector3D rotatedBodyAngularMomentumRate = new FrameVector3D();
   private final DMatrixRMaj skewRotatedBodyAngularMomentumRate = new DMatrixRMaj(3, 3);

   private final CommonMatrix3DBasics desiredRotationMatrix = new RotationMatrix();
   private final DMatrixRMaj rotationMatrix = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj inertiaMatrixInBody = new DMatrixRMaj(3, 3);

   private final DMatrixRMaj inverseInertia = new DMatrixRMaj(3, 3);

   private final DMatrixRMaj desiredBodyAngularVelocity = new DMatrixRMaj(3, 1);
   private final DMatrixRMaj skewDesiredBodyAngularVelocity = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj desiredCoMAcceleration = new DMatrixRMaj(3, 1);
   private final DMatrixRMaj desiredContactForce = new DMatrixRMaj(3, 1);
   private final DMatrixRMaj desiredCoMPositionVector = new DMatrixRMaj(3, 1);
   private final DMatrixRMaj skewDesiredContactForce = new DMatrixRMaj(3, 3);

   private final DMatrixRMaj comPositionJacobian = new DMatrixRMaj(3, 0);
   final DMatrixRMaj contactForceJacobian = new DMatrixRMaj(3, 0);
   final DMatrixRMaj contactForceToOriginTorqueJacobian = new DMatrixRMaj(3, 0);

   private final DMatrixRMaj b0 = new DMatrixRMaj(3, 1);
   private final DMatrixRMaj b1 = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj b2 = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj b3 = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj b4 = new DMatrixRMaj(3, 3);

   private final DMatrixRMaj A = new DMatrixRMaj(6, 6);
   private final DMatrixRMaj B = new DMatrixRMaj(6, 0);
   private final DMatrixRMaj C = new DMatrixRMaj(6, 1);

   private DiscretizationCalculator discretizationCalculator = new EfficientFirstOrderHoldDiscretizationCalculator();

   private final DMatrixRMaj Ad = new DMatrixRMaj(6, 6);
   private final DMatrixRMaj Bd = new DMatrixRMaj(6, 0);
   private final DMatrixRMaj Cd = new DMatrixRMaj(6, 1);

   private final double mass;

   private static final DMatrixRMaj identity3 = CommonOps_DDRM.identity(3);

   public OrientationDynamicsCalculator(double mass, double gravity)
   {
      this.mass = mass;

      gravityVector.set(2, 0, -Math.abs(gravity));
   }

   private final List<MPCContactPlane> contactPlanes = new ArrayList<>();

   public void setDiscretizationCalculator(DiscretizationCalculator discretizationCalculator)
   {
      this.discretizationCalculator = discretizationCalculator;
   }

   public boolean compute(DiscreteAngularVelocityOrientationCommand command)
   {
      contactPlanes.clear();
      for (int i = 0; i < command.getNumberOfContacts(); i++)
         contactPlanes.add(command.getContactPlane(i));

      setMomentumOfInertiaInBodyFrame(command.getMomentOfInertiaInBodyFrame());

      return compute(command.getDesiredCoMPosition(),
                     command.getDesiredCoMAcceleration(),
                     command.getDesiredBodyOrientation(),
                     command.getDesiredBodyAngularVelocity(),
                     command.getDesiredNetAngularMomentumRate(),
                     command.getDesiredInternalAngularMomentumRate(),
                     contactPlanes,
                     command.getTimeOfConstraint(),
                     command.getDurationOfHold(),
                     command.getOmega());
   }

   public void setMomentumOfInertiaInBodyFrame(Matrix3DReadOnly inertiaMatrixInBody)
   {
      inertiaMatrixInBody.get(this.inertiaMatrixInBody);
   }

   public boolean compute(FramePoint3DReadOnly desiredComPosition,
                          FrameVector3DReadOnly desiredCoMAcceleration,
                          FrameOrientation3DReadOnly desiredBodyOrientation,
                          Vector3DReadOnly desiredBodyAngularVelocityInBodyFrame,
                          Vector3DReadOnly desiredNetAngularMomentumRate,
                          Vector3DReadOnly desiredInternalAngularMomentumRate,
                          List<MPCContactPlane> contactPlanes,
                          double timeOfConstraint,
                          double durationOfHold,
                          double omega)
   {
      reset(contactPlanes);
      getAllTheTermsFromTheCommandInput(desiredComPosition,
                                        desiredCoMAcceleration,
                                        desiredBodyOrientation,
                                        desiredBodyAngularVelocityInBodyFrame,
                                        desiredNetAngularMomentumRate,
                                        desiredInternalAngularMomentumRate);

      calculateStateJacobians(desiredComPosition, contactPlanes, timeOfConstraint, omega);

      calculateAffineAxisAngleErrorTerms();

      computeAffineTimeInvariantTerms(timeOfConstraint);
      if (!Double.isFinite(durationOfHold))
         throw new IllegalArgumentException("The duration of the hold is not finite.");

      discretizationCalculator.compute(A, B, C, Ad, Bd, Cd, durationOfHold);

      return true;
   }

   DiscretizationCalculator getDiscretizationCalculator()
   {
      return discretizationCalculator;
   }

   DMatrixRMaj getContinuousAMatrix()
   {
      return A;
   }

   DMatrixRMaj getContinuousBMatrix()
   {
      return B;
   }

   DMatrixRMaj getContinuousCMatrix()
   {
      return C;
   }

   DMatrixRMaj getDiscreteAMatrix()
   {
      return Ad;
   }

   DMatrixRMaj getDiscreteBMatrix()
   {
      return Bd;
   }

   DMatrixRMaj getDiscreteCMatrix()
   {
      return Cd;
   }

   DMatrixRMaj getB0()
   {
      return b0;
   }

   DMatrixRMaj getB1()
   {
      return b1;
   }

   DMatrixRMaj getB2()
   {
      return b2;
   }

   DMatrixRMaj getB3()
   {
      return b3;
   }

   DMatrixRMaj getB4()
   {
      return b4;
   }

   private void reset(List<MPCContactPlane> contactPlanes)
   {
      int totalContactPoints = 0;
      int rhoCoefficientsInSegment = 0;
      for (int i = 0; i < contactPlanes.size(); i++)
      {
         totalContactPoints += contactPlanes.get(i).getNumberOfContactPoints();
         rhoCoefficientsInSegment += contactPlanes.get(i).getCoefficientSize();
      }

      int coefficientsInSegment = LinearMPCIndexHandler.comCoefficientsPerSegment + rhoCoefficientsInSegment;

      comPositionJacobian.reshape(3, coefficientsInSegment);
      int contactForceVectorSize = 3 * totalContactPoints;
      contactForceJacobian.reshape(contactForceVectorSize, rhoCoefficientsInSegment);
      contactForceToOriginTorqueJacobian.reshape(3, contactForceVectorSize);

      B.reshape(6, coefficientsInSegment);
      Bd.reshape(6, coefficientsInSegment);

      comPositionJacobian.zero();
      contactForceJacobian.zero();
      contactForceToOriginTorqueJacobian.zero();

      b0.zero();
      b1.zero();
      b2.zero();
      b3.zero();
      b4.zero();

      A.zero();
      B.zero();
      C.zero();

      Ad.zero();
      Bd.zero();
      Cd.zero();
   }

   private void getAllTheTermsFromTheCommandInput(FramePoint3DReadOnly desiredCoMPosition,
                                                  FrameVector3DReadOnly desiredCoMAcceleration,
                                                  FrameOrientation3DReadOnly desiredBodyOrientation,
                                                  Vector3DReadOnly desiredBodyAngularVelocityInBodyFrame,
                                                  Vector3DReadOnly desiredNetAngularMomentumRate,
                                                  Vector3DReadOnly desiredInternalAngularMomentumRate)
   {
      this.desiredNetAngularMomentumRate.set(desiredNetAngularMomentumRate);
      desiredBodyAngularMomentumRate.sub(desiredNetAngularMomentumRate, desiredInternalAngularMomentumRate);

      desiredBodyOrientation.get(desiredRotationMatrix);
      desiredRotationMatrix.get(rotationMatrix);

      desiredCoMAcceleration.get(this.desiredCoMAcceleration);
      desiredBodyAngularVelocityInBodyFrame.get(desiredBodyAngularVelocity);

      desiredContactForce.set(this.desiredCoMAcceleration);
      desiredContactForce.add(2, 0, -gravityVector.get(2, 0));
      CommonOps_DDRM.scale(mass, desiredContactForce);

      desiredCoMPosition.get(this.desiredCoMPositionVector);
      MatrixMissingTools.toSkewSymmetricMatrix(desiredContactForce, skewDesiredContactForce);
      MatrixMissingTools.toSkewSymmetricMatrix(desiredBodyAngularVelocityInBodyFrame, skewDesiredBodyAngularVelocity);

      UnrolledInverseFromMinor_DDRM.inv3(inertiaMatrixInBody, inverseInertia, 1.0);
   }

   private void calculateStateJacobians(FramePoint3DReadOnly desiredCoMPosition, List<MPCContactPlane> contactPlanes, double timeOfConstraint, double omega)
   {
      int rhoStartIndex = 0;

      CoMCoefficientJacobianCalculator.calculateCoMJacobian(0, timeOfConstraint, comPositionJacobian, 0, 1.0);

      int contactRow = 0;
      for (int i = 0; i < contactPlanes.size(); i++)
      {
         MPCContactPlane contactPlane = contactPlanes.get(i);
         ContactPlaneJacobianCalculator.computeLinearJacobian(0, timeOfConstraint, omega, LinearMPCIndexHandler.comCoefficientsPerSegment + rhoStartIndex, contactPlane, comPositionJacobian);
         ContactPlaneJacobianCalculator.computeContactPointAccelerationJacobian(mass, timeOfConstraint, omega, contactRow, rhoStartIndex, contactPlane, contactForceJacobian);
         computeTorqueAboutBodyJacobian(contactRow, desiredCoMPosition, contactPlane, contactForceToOriginTorqueJacobian);

         contactRow += 3 * contactPlane.getNumberOfContactPoints();
         rhoStartIndex += contactPlane.getCoefficientSize();
      }
   }

   private final DMatrixRMaj IR = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj angularMomentum = new DMatrixRMaj(3, 1);
   private final DMatrixRMaj skewAngularMomentum = new DMatrixRMaj(3, 3);
   private final DMatrixRMaj torqueAboutPoint = new DMatrixRMaj(3, 1);

   private void calculateAffineAxisAngleErrorTerms()
   {
      CommonOps_DDRM.multTransB(inverseInertia, rotationMatrix, IR);

      CommonOps_DDRM.mult(IR, skewDesiredContactForce, b1);
      CommonOps_DDRM.mult(IR, contactForceToOriginTorqueJacobian, b2);

      desiredRotationMatrix.inverseTransform(desiredBodyAngularMomentumRate, rotatedBodyAngularMomentumRate);
      MatrixMissingTools.toSkewSymmetricMatrix(rotatedBodyAngularMomentumRate, skewRotatedBodyAngularMomentumRate);

      CommonOps_DDRM.mult(inverseInertia, skewRotatedBodyAngularMomentumRate, b3);

      CommonOps_DDRM.mult(inertiaMatrixInBody, desiredBodyAngularVelocity, angularMomentum);
      MatrixMissingTools.toSkewSymmetricMatrix(angularMomentum, skewAngularMomentum);
      CommonOps_DDRM.multAdd(-1.0, skewDesiredBodyAngularVelocity, inertiaMatrixInBody, skewAngularMomentum);
      CommonOps_DDRM.mult(inverseInertia, skewAngularMomentum, b4);

      desiredNetAngularMomentumRate.get(torqueAboutPoint);
      CommonOps_DDRM.multAdd(skewDesiredContactForce, desiredCoMPositionVector, torqueAboutPoint);
      CommonOps_DDRM.scale(-1.0, torqueAboutPoint);

      CommonOps_DDRM.mult(IR, torqueAboutPoint, b0);
   }

   private final FrameVector3D momentArm = new FrameVector3D();
   private final DMatrixRMaj skewMomentArm = new DMatrixRMaj(3, 3);

   private void computeTorqueAboutBodyJacobian(int colStart, FramePoint3DReadOnly desiredCoMPosition, MPCContactPlane contactPlane, DMatrixRMaj jacobianToPack)
   {
      for (int i = 0; i < contactPlane.getNumberOfContactPoints(); i++)
      {
         momentArm.sub(contactPlane.getContactPointHelper(i).getBasisVectorOrigin(), desiredCoMPosition);
         MatrixMissingTools.toSkewSymmetricMatrix(momentArm, skewMomentArm);

         MatrixMissingTools.setMatrixBlock(jacobianToPack, 0, colStart, skewMomentArm, 0, 0, 3, 3, 1.0);
         colStart += 3;
      }
   }

   private void computeAffineTimeInvariantTerms(double timeOfConstraint)
   {
      MatrixTools.setMatrixBlock(A, 0, 0, skewDesiredBodyAngularVelocity, 0, 0, 3, 3, -1.0);
      MatrixTools.setMatrixBlock(A, 0, 3, identity3, 0, 0, 3, 3, 1.0);
      MatrixTools.setMatrixBlock(A, 3, 0, b3, 0, 0, 3, 3, 1.0);
      MatrixTools.setMatrixBlock(A, 3, 3, b4, 0, 0, 3, 3, 1.0);

      MatrixTools.multAddBlock(b1, comPositionJacobian, B, 3, 0);
      MatrixTools.multAddBlock(b2, contactForceJacobian, B, 3, LinearMPCIndexHandler.comCoefficientsPerSegment);

      MatrixTools.setMatrixBlock(C, 3, 0, b0, 0, 0, 3, 1, 1.0);
      MatrixTools.multAddBlock(0.5 * timeOfConstraint * timeOfConstraint, b1, gravityVector, C, 3, 0);
   }
}
