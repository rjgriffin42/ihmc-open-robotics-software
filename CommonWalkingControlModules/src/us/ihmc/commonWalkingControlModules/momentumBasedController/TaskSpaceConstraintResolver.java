package us.ihmc.commonWalkingControlModules.momentumBasedController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.ops.CommonOps;

import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.NullspaceCalculator;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.DesiredJointAccelerationCalculator;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.Momentum;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

public class TaskSpaceConstraintResolver
{
   private final ReferenceFrame rootJointFrame;
   private final RigidBody rootJointPredecessor;
   private final RigidBody rootJointSuccessor;

   private final InverseDynamicsJoint[] jointsInOrder;
   private final TwistCalculator twistCalculator;
   private final NullspaceCalculator nullspaceCalculator;
   private final LinearSolver<DenseMatrix64F> jacobianSolver;

   private final DenseMatrix64F aTaskSpace;
   private final DenseMatrix64F vdotTaskSpace;
   private final DenseMatrix64F cTaskSpace;
   private final DenseMatrix64F taskSpaceAccelerationMatrix;
   private final DenseMatrix64F sJInverse;
   private final DenseMatrix64F sJ;

   private final SpatialAccelerationVector convectiveTerm = new SpatialAccelerationVector();
   private final Twist twistOfCurrentWithRespectToNew = new Twist();
   private final Twist twistOfBodyWithRespectToBase = new Twist();

   // LinkedHashMaps so that order of computation is defined, to make rewindability checks using reflection easier
   private final Map<GeometricJacobian, DenseMatrix64F> phiCMap = new LinkedHashMap<GeometricJacobian, DenseMatrix64F>();
   private final Map<GeometricJacobian, Map<InverseDynamicsJoint, DenseMatrix64F>> phiJMap = new LinkedHashMap<GeometricJacobian,
                                                                                                Map<InverseDynamicsJoint, DenseMatrix64F>>();

   private final DenseMatrix64F convectiveTermMatrix = new DenseMatrix64F(SpatialAccelerationVector.SIZE, 1);

   public TaskSpaceConstraintResolver(SixDoFJoint rootJoint, InverseDynamicsJoint[] jointsInOrder, TwistCalculator twistCalculator,
                                      NullspaceCalculator nullspaceCalculator, LinearSolver<DenseMatrix64F> jacobianSolver)
   {
      rootJointFrame = rootJoint.getFrameAfterJoint();
      rootJointPredecessor = rootJoint.getPredecessor();
      rootJointSuccessor = rootJoint.getSuccessor();
      this.jointsInOrder = jointsInOrder;
      this.twistCalculator = twistCalculator;
      this.nullspaceCalculator = nullspaceCalculator;
      this.jacobianSolver = jacobianSolver;

      int size = Momentum.SIZE;
      this.aTaskSpace = new DenseMatrix64F(size, size);    // reshaped
      this.vdotTaskSpace = new DenseMatrix64F(size, 1);    // reshaped
      this.cTaskSpace = new DenseMatrix64F(size, 1);
      this.taskSpaceAccelerationMatrix = new DenseMatrix64F(size, 1);
      this.sJInverse = new DenseMatrix64F(size, size);    // reshaped
      this.sJ = new DenseMatrix64F(size, size);    // reshaped
   }

   public void reset()
   {
      phiCMap.clear();
      phiJMap.clear();
   }

   public void handleTaskSpaceAccelerations(HashMap<InverseDynamicsJoint, DenseMatrix64F> aHats, DenseMatrix64F bHat, DenseMatrix64F centroidalMomentumMatrix,
           GeometricJacobian jacobian, SpatialAccelerationVector taskSpaceAcceleration, DenseMatrix64F nullspaceMultiplier, DenseMatrix64F selectionMatrix)
   {
      assert(selectionMatrix.getNumCols() == SpatialAccelerationVector.SIZE);

      if (taskSpaceAcceleration.getExpressedInFrame() != taskSpaceAcceleration.getBodyFrame())
      {
         throw new RuntimeException("Not supported");
//         twistCalculator.packRelativeTwist(twistOfCurrentWithRespectToNew, rootJointSuccessor, jacobian.getEndEffector());
//         twistCalculator.packRelativeTwist(twistOfBodyWithRespectToBase, rootJointPredecessor, jacobian.getEndEffector());
//         taskSpaceAcceleration.changeFrame(rootJointSuccessor.getBodyFixedFrame(), twistOfCurrentWithRespectToNew, twistOfBodyWithRespectToBase);
//         taskSpaceAcceleration.changeFrameNoRelativeMotion(rootJointFrame);
      }

      // joint bookkeeping
      RigidBody base = getBase(taskSpaceAcceleration);
      RigidBody endEffector = getEndEffector(taskSpaceAcceleration);
      InverseDynamicsJoint[] constrainedJoints = jacobian.getJointsInOrder();
      InverseDynamicsJoint[] unconstrainedJointsOnPath = computeUnconstrainedJointsOnPath(constrainedJoints, base, endEffector);
      LinkedHashMap<InverseDynamicsJoint, DenseMatrix64F> phiJMapForCurrentConstraint = new LinkedHashMap<InverseDynamicsJoint,
                                                                                           DenseMatrix64F>(unconstrainedJointsOnPath.length);
      phiJMap.put(jacobian, phiJMapForCurrentConstraint);

      // taskSpaceAcceleration
      taskSpaceAcceleration.packMatrix(taskSpaceAccelerationMatrix, 0);

      // J
//      jacobian.changeFrame(rootJointFrame);
      jacobian.changeFrame(taskSpaceAcceleration.getExpressedInFrame());
      jacobian.compute();
      sJ.reshape(selectionMatrix.getNumRows(), jacobian.getNumberOfColumns());
      CommonOps.mult(selectionMatrix, jacobian.getJacobianMatrix(), sJ);

      // aTaskSpace
      int[] columnIndices = ScrewTools.computeIndicesForJoint(jointsInOrder, constrainedJoints);
      aTaskSpace.reshape(aTaskSpace.getNumRows(), columnIndices.length);
      MatrixTools.extractColumns(centroidalMomentumMatrix, aTaskSpace, columnIndices);

      // convectiveTerm
      GeometricJacobian baseToEndEffectorJacobian = new GeometricJacobian(base, endEffector, taskSpaceAcceleration.getExpressedInFrame()); // FIXME: garbage, repeated computation
      baseToEndEffectorJacobian.compute();
      DesiredJointAccelerationCalculator desiredJointAccelerationCalculator = new DesiredJointAccelerationCalculator(baseToEndEffectorJacobian, null);    // TODO: garbage
      desiredJointAccelerationCalculator.computeJacobianDerivativeTerm(convectiveTerm);
//      twistCalculator.packRelativeTwist(twistOfCurrentWithRespectToNew, rootJointSuccessor, endEffector);
//      twistCalculator.packRelativeTwist(twistOfBodyWithRespectToBase, base, endEffector);
//      convectiveTerm.changeFrame(rootJointSuccessor.getBodyFixedFrame(), twistOfCurrentWithRespectToNew, twistOfBodyWithRespectToBase);
//      convectiveTerm.changeFrameNoRelativeMotion(rootJointFrame);
      convectiveTerm.getBodyFrame().checkReferenceFrameMatch(taskSpaceAcceleration.getBodyFrame());
      convectiveTerm.getExpressedInFrame().checkReferenceFrameMatch(taskSpaceAcceleration.getExpressedInFrame());
      convectiveTerm.packMatrix(convectiveTermMatrix, 0);

      sJInverse.reshape(sJ.getNumCols(), sJ.getNumCols());
      DenseMatrix64F phiC = new DenseMatrix64F(sJInverse.getNumRows(), 1);    // TODO: garbage
      phiCMap.put(jacobian, phiC);

      // JInverse
      jacobianSolver.setA(sJ);
      jacobianSolver.invert(sJInverse);

      int nullity = nullspaceMultiplier.getNumRows();
      if (nullity > 0)
      {
         nullspaceCalculator.setMatrix(sJ, nullity);    // TODO: check if this works for S != I6
         nullspaceCalculator.removeNullspaceComponent(sJInverse);
      }

      // Phi
      DenseMatrix64F phi = new DenseMatrix64F(sJInverse.getNumRows(), selectionMatrix.getNumCols());    // TODO: garbage
      CommonOps.mult(sJInverse, selectionMatrix, phi);

      // cTaskSpace
      cTaskSpace.zero();
      cTaskSpace.reshape(taskSpaceAccelerationMatrix.getNumRows(), 1);
      CommonOps.sub(taskSpaceAccelerationMatrix, convectiveTermMatrix, cTaskSpace);

      // Phi * c
      CommonOps.mult(phi, cTaskSpace, phiC);

      if (nullity > 0)
      {
         nullspaceCalculator.addNullspaceComponent(phiC, nullspaceMultiplier);
      }

      // update aHats
      for (InverseDynamicsJoint unconstrainedJointOnPath : unconstrainedJointsOnPath)
      {
         // J
         GeometricJacobian jointJacobian = new GeometricJacobian(unconstrainedJointOnPath, taskSpaceAcceleration.getExpressedInFrame());
         jointJacobian.compute();

         // Phi * J
         DenseMatrix64F phiJ = new DenseMatrix64F(phi.getNumRows(), jointJacobian.getNumberOfColumns());
         CommonOps.mult(phi, jointJacobian.getJacobianMatrix(), phiJ);
         phiJMapForCurrentConstraint.put(unconstrainedJointOnPath, phiJ);

         CommonOps.multAdd(-1.0, aTaskSpace, phiJ, aHats.get(unconstrainedJointOnPath));
      }

      // update b
      CommonOps.multAdd(-1.0, aTaskSpace, phiC, bHat);
   }

   public void solveAndSetTaskSpaceAccelerations(LinkedHashMap<InverseDynamicsJoint, Boolean> jointAccelerationValidMap)
   {
      for (GeometricJacobian jacobian : phiCMap.keySet())
      {
         DenseMatrix64F phiC = phiCMap.get(jacobian);
         vdotTaskSpace.setReshape(phiC);
         Map<InverseDynamicsJoint, DenseMatrix64F> phiJMapForJacobian = phiJMap.get(jacobian);
         for (InverseDynamicsJoint unconstrainedJointOnPath : phiJMapForJacobian.keySet())
         {
            if (!jointAccelerationValidMap.get(unconstrainedJointOnPath))
               throw new RuntimeException("Trying to use invalid data to compute task space constrained joint accelerations");

            DenseMatrix64F vdot = new DenseMatrix64F(unconstrainedJointOnPath.getDegreesOfFreedom(), 1);
            DenseMatrix64F phiJ = phiJMapForJacobian.get(unconstrainedJointOnPath);
            unconstrainedJointOnPath.packDesiredAccelerationMatrix(vdot, 0);
            CommonOps.multAdd(-1.0, phiJ, vdot, vdotTaskSpace);
         }

         ScrewTools.setDesiredAccelerations(jacobian.getJointsInOrder(), vdotTaskSpace);
         for (InverseDynamicsJoint joint : jacobian.getJointsInOrder())
         {
            jointAccelerationValidMap.put(joint, true);
         }
      }
   }

   public void convertInternalSpatialAccelerationToJointSpace(Map<InverseDynamicsJoint, DenseMatrix64F> jointSpaceAccelerations, GeometricJacobian jacobian,
           SpatialAccelerationVector spatialAcceleration, DenseMatrix64F nullspaceMultipliers, DenseMatrix64F selectionMatrix)
   {
      jacobian.changeFrame(spatialAcceleration.getExpressedInFrame());
      jacobian.compute();
      DesiredJointAccelerationCalculator desiredJointAccelerationCalculator = new DesiredJointAccelerationCalculator(jacobian, null);

      // convectiveTerm
      desiredJointAccelerationCalculator.computeJacobianDerivativeTerm(convectiveTerm);
      convectiveTerm.packMatrix(convectiveTermMatrix, 0);

      // sJ
      sJ.reshape(selectionMatrix.getNumRows(), jacobian.getNumberOfColumns());
      CommonOps.mult(selectionMatrix, jacobian.getJacobianMatrix(), sJ);

      // taskSpaceAcceleration
      spatialAcceleration.packMatrix(taskSpaceAccelerationMatrix, 0);

      // c
      cTaskSpace.reshape(selectionMatrix.getNumRows(), 1);
      cTaskSpace.zero();
      CommonOps.multAdd(1.0, selectionMatrix, taskSpaceAccelerationMatrix, cTaskSpace);
      CommonOps.multAdd(-1.0, selectionMatrix, convectiveTermMatrix, cTaskSpace);

      jacobianSolver.setA(sJ);
      vdotTaskSpace.reshape(cTaskSpace.getNumRows(), 1);
      jacobianSolver.solve(cTaskSpace, vdotTaskSpace);

      int nullity = nullspaceMultipliers.getNumRows();

      if (nullity > 0)
      {
         nullspaceCalculator.setMatrix(sJ, nullity);    // TODO: check if this works
         nullspaceCalculator.removeNullspaceComponent(vdotTaskSpace);
         nullspaceCalculator.addNullspaceComponent(vdotTaskSpace, nullspaceMultipliers);
      }

      int index = 0;
      for (InverseDynamicsJoint joint : jacobian.getJointsInOrder())
      {
         DenseMatrix64F jointAcceleration = new DenseMatrix64F(joint.getDegreesOfFreedom(), 1);    // TODO: garbage
         int degreesOfFreedom = joint.getDegreesOfFreedom();
         CommonOps.extract(vdotTaskSpace, index, index + degreesOfFreedom, 0, 1, jointAcceleration, 0, 0);
         jointSpaceAccelerations.put(joint, jointAcceleration);
         index += degreesOfFreedom;
      }
   }

   private static InverseDynamicsJoint[] computeUnconstrainedJointsOnPath(InverseDynamicsJoint[] constrainedJoints, RigidBody base, RigidBody endEffector)
   {
      int nConstrainedJoints = constrainedJoints.length;
      int distance = ScrewTools.computeDistanceToAncestor(endEffector, base);
      int nUnconstrainedJointsOnPath = distance - nConstrainedJoints;
      InverseDynamicsJoint[] ret = new InverseDynamicsJoint[nUnconstrainedJointsOnPath];

      int constrainedJointsIndex = nConstrainedJoints - 1;
      int unconstrainedJointsIndex = nUnconstrainedJointsOnPath - 1;
      RigidBody currentBody = endEffector;
      while (currentBody != base)
      {
         InverseDynamicsJoint parentJoint = currentBody.getParentJoint();
         if (constrainedJointsIndex >= 0 && constrainedJoints[constrainedJointsIndex] == parentJoint)
         {
            constrainedJointsIndex--;
         }
         else
         {
            ret[unconstrainedJointsIndex--] = parentJoint;
         }

         currentBody = parentJoint.getPredecessor();
      }

      if (constrainedJointsIndex != -1)
         throw new RuntimeException("Constrained joints are not all on path between base and end effector");

      if (unconstrainedJointsIndex != -1)
         throw new RuntimeException("Something is really wrong");

      return ret;
   }
   

   private RigidBody getBase(SpatialAccelerationVector taskSpaceAcceleration)
   {
      for (InverseDynamicsJoint joint : jointsInOrder)
      {         
         RigidBody predecessor = joint.getPredecessor();
         if (predecessor.getBodyFixedFrame() == taskSpaceAcceleration.getBaseFrame())
            return predecessor;
      }
      throw new RuntimeException("Base for " + taskSpaceAcceleration + " could not be determined");
   }

   private RigidBody getEndEffector(SpatialAccelerationVector taskSpaceAcceleration)
   {
      for (InverseDynamicsJoint joint : jointsInOrder)
      {         
         RigidBody successor = joint.getSuccessor();
         if (successor.getBodyFixedFrame() == taskSpaceAcceleration.getBodyFrame())
            return successor;
      }
      throw new RuntimeException("End effector for " + taskSpaceAcceleration + " could not be determined");
   }
}
