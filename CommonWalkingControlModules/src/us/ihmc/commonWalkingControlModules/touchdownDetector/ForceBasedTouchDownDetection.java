package us.ihmc.commonWalkingControlModules.touchdownDetector;

import java.util.List;

import org.ejml.alg.dense.misc.UnrolledInverseFromMinor;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.BooleanYoVariable;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.GeometricJacobian;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;

public class ForceBasedTouchDownDetection implements TouchdownDetector
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry ;
   private final GeometricJacobian footJacobian;
   private final DenseMatrix64F linearPartOfJacobian = new DenseMatrix64F(3, 3);   
   private final DenseMatrix64F linearJacobianInverse = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F selectionMatrix = CommonOps.identity(6);   
   private final DenseMatrix64F jointTorques = new DenseMatrix64F(3, 1);
   private final DenseMatrix64F footLinearForce = new DenseMatrix64F(3, 1);
   private final List<OneDoFJoint> legOneDoFJoints;
   
   private final BooleanYoVariable isInContact;
   private final DoubleYoVariable zForceThreshold;
   private final DoubleYoVariable measuredZForce;
   private final FrameVector footForce = new FrameVector();
   
   public ForceBasedTouchDownDetection(FullQuadrupedRobotModel robotModel, RobotQuadrant robotQuadrant, ReferenceFrame soleFrame, YoVariableRegistry parentRegistry)
   {
      String prefix = robotQuadrant.getCamelCaseName() + name;
      registry = new YoVariableRegistry(prefix);
      
      isInContact = new BooleanYoVariable(prefix + "isInContact", registry);
      zForceThreshold = new DoubleYoVariable(prefix + "zForceThreshold", registry);
      measuredZForce = new DoubleYoVariable(prefix + "measuredZForce", registry);
      
      zForceThreshold.set(80.0);
      
      RigidBody body = robotModel.getPelvis();
      RigidBody foot = robotModel.getFoot(robotQuadrant);
      footJacobian = new GeometricJacobian(body, foot, soleFrame);
      
      legOneDoFJoints = robotModel.getLegOneDoFJoints(robotQuadrant);
      
      //remove angular part
      MatrixTools.removeRow(selectionMatrix, 0);
      MatrixTools.removeRow(selectionMatrix, 0);
      MatrixTools.removeRow(selectionMatrix, 0);
      
      parentRegistry.addChild(registry);
   }

   @Override
   public boolean hasTouchedDown()
   {
      return isInContact.getBooleanValue();
   }

   @Override
   public void update()
   {
      for(int i = 0; i < legOneDoFJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = legOneDoFJoints.get(i);
         jointTorques.set(i, 0, oneDoFJoint.getTauMeasured());
      }
      
      footJacobian.compute();
      DenseMatrix64F jacobianMatrix = footJacobian.getJacobianMatrix();
      CommonOps.mult(selectionMatrix, jacobianMatrix, linearPartOfJacobian);
      UnrolledInverseFromMinor.inv3(linearPartOfJacobian, linearJacobianInverse, 1.0);
      
      CommonOps.multTransA(linearJacobianInverse, jointTorques, footLinearForce);
      
      footForce.setToZero(footJacobian.getJacobianFrame());
      footForce.set(footLinearForce.get(0), footLinearForce.get(1), footLinearForce.get(2));
      footForce.changeFrame(ReferenceFrame.getWorldFrame());
      measuredZForce.set(footForce.getZ() * -1.0);
      isInContact.set(measuredZForce.getDoubleValue() > zForceThreshold.getDoubleValue());
   }
}
