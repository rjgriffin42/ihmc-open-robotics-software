package us.ihmc.quadrupedRobotics.estimator.footSwitch;

import org.ejml.alg.dense.misc.UnrolledInverseFromMinor;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.touchdownDetector.WrenchCalculator;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.spatial.interfaces.WrenchReadOnly;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.GeometricJacobian;

import java.util.List;

public class JointTorqueBasedWrenchCalculator implements WrenchCalculator
{
   private final Wrench wrench = new Wrench();

   private final DenseMatrix64F linearPartOfJacobian = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F angularPartOfJacobian = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F linearJacobianInverse = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F angularJacobianInverse = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F linearSelectionMatrix = CommonOps.identity(6);
   private final DenseMatrix64F angularSelectionMatrix = CommonOps.identity(6);

   private final DenseMatrix64F footLinearForce = new DenseMatrix64F(3, 1);
   private final DenseMatrix64F footAngularForce = new DenseMatrix64F(3, 1);

   private final DenseMatrix64F jointTorques = new DenseMatrix64F(3, 1);

   private final GeometricJacobian footJacobian;
   private final List<JointTorqueProvider> jointTorqueProviders;

   private final String prefix;

   public JointTorqueBasedWrenchCalculator(String prefix, FullQuadrupedRobotModel robotModel, RobotQuadrant robotQuadrant, ReferenceFrame soleFrame,
                                           List<JointTorqueProvider> jointTorqueProviders)
   {
      this.prefix = prefix;
      this.jointTorqueProviders = jointTorqueProviders;
      RigidBodyBasics body = robotModel.getRootBody();
      RigidBodyBasics foot = robotModel.getFoot(robotQuadrant);
      footJacobian = new GeometricJacobian(body, foot, soleFrame);

      wrench.setToZero(ReferenceFrame.getWorldFrame());

      // remove linear part
      MatrixTools.removeRow(angularSelectionMatrix, 3);
      MatrixTools.removeRow(angularSelectionMatrix, 3);
      MatrixTools.removeRow(angularSelectionMatrix, 3);

      // remove angular part
      MatrixTools.removeRow(linearSelectionMatrix, 0);
      MatrixTools.removeRow(linearSelectionMatrix, 0);
      MatrixTools.removeRow(linearSelectionMatrix, 0);
   }

   @Override
   public void calculate()
   {
      for(int i = 0; i < jointTorqueProviders.size(); i++)
         jointTorques.set(i, 0, jointTorqueProviders.get(i).getTorque());

      footJacobian.compute();
      DenseMatrix64F jacobianMatrix = footJacobian.getJacobianMatrix();
      CommonOps.mult(linearSelectionMatrix, jacobianMatrix, linearPartOfJacobian);
      CommonOps.mult(angularSelectionMatrix, jacobianMatrix, angularPartOfJacobian);
      UnrolledInverseFromMinor.inv3(linearPartOfJacobian, linearJacobianInverse, 1.0);
      UnrolledInverseFromMinor.inv3(angularPartOfJacobian, angularJacobianInverse, 1.0);

      CommonOps.multTransA(-1.0, linearJacobianInverse, jointTorques, footLinearForce);
      CommonOps.multTransA(-1.0, angularJacobianInverse, jointTorques, footAngularForce);


      wrench.setToZero(footJacobian.getJacobianFrame());
      wrench.setLinearPartX(footLinearForce.get(0));
      wrench.setLinearPartY(footLinearForce.get(1));
      wrench.setLinearPartZ(footLinearForce.get(2));
      wrench.setAngularPartX(footAngularForce.get(0));
      wrench.setAngularPartY(footAngularForce.get(1));
      wrench.setAngularPartZ(footAngularForce.get(2));
   }

   @Override
   public WrenchReadOnly getWrench()
   {
      return wrench;
   }

   @Override
   public String getName()
   {
      return prefix + "JointTorqueWrenchCalculator";
   }

   public interface JointTorqueProvider
   {
      double getTorque();
   }


}
