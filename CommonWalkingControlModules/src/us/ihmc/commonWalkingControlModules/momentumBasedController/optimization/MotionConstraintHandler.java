package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.commonWalkingControlModules.momentumBasedController.GeometricJacobianHolder;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.JointspaceAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.SpatialAccelerationCommand;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.linearAlgebra.NullspaceCalculator;
import us.ihmc.robotics.screwTheory.ConvectiveTermCalculator;
import us.ihmc.robotics.screwTheory.GeometricJacobian;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.PointJacobian;
import us.ihmc.robotics.screwTheory.PointJacobianConvectiveTermCalculator;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.SpatialAccelerationVector;
import us.ihmc.robotics.screwTheory.TwistCalculator;

/**
 * @author twan
 *         Date: 4/30/13
 */
public class MotionConstraintHandler
{
   private final InverseDynamicsJoint[] jointsInOrder;
   private final LinkedHashMap<InverseDynamicsJoint, int[]> columnsForJoints = new LinkedHashMap<InverseDynamicsJoint, int[]>();
   private final int nDegreesOfFreedom;

   private final SpatialAccelerationVector convectiveTerm = new SpatialAccelerationVector();
   private final DenseMatrix64F convectiveTermMatrix = new DenseMatrix64F(SpatialAccelerationVector.SIZE, 1);

   private final DenseMatrix64F taskSpaceAccelerationMatrix = new DenseMatrix64F(SpatialAccelerationVector.SIZE, 1);
   private final List<DenseMatrix64F> jList = new ArrayList<DenseMatrix64F>();
   private final List<DenseMatrix64F> pList = new ArrayList<DenseMatrix64F>();
   private final List<MutableDouble> weightList = new ArrayList<MutableDouble>();

   private final DenseMatrix64F j = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F p = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F w = new DenseMatrix64F(1, 1);

   private final DenseMatrix64F jBlockCompact = new DenseMatrix64F(1, 1);

   private final ConvectiveTermCalculator convectiveTermCalculator = new ConvectiveTermCalculator();
   private final PointJacobian pointJacobian = new PointJacobian();

   private int motionConstraintIndex = 0;
   private final FrameVector pPointVelocity = new FrameVector();
   private final PointJacobianConvectiveTermCalculator pointJacobianConvectiveTermCalculator;

   private final YoVariableRegistry registry;
   private final BooleanYoVariable removeNullspaceFromJ;
   private final GeometricJacobianHolder geometricJacobianHolder;
   private final boolean jacobiansHaveToBeUpdated;
   private final TIntArrayList indicesIntoCompactBlock = new TIntArrayList();

   public MotionConstraintHandler(String name, InverseDynamicsJoint[] jointsInOrder, TwistCalculator twistCalculator,
         GeometricJacobianHolder geometricJacobianHolder, YoVariableRegistry parentRegistry)
   {
      registry = new YoVariableRegistry(name + getClass().getSimpleName());
      removeNullspaceFromJ = new BooleanYoVariable(name + "RemoveNullspaceFromJ", registry);
      removeNullspaceFromJ.set(true);

      this.geometricJacobianHolder = (geometricJacobianHolder != null) ? geometricJacobianHolder : new GeometricJacobianHolder();
      jacobiansHaveToBeUpdated = geometricJacobianHolder == null;

      this.jointsInOrder = jointsInOrder;
      this.nDegreesOfFreedom = ScrewTools.computeDegreesOfFreedom(jointsInOrder);

      for (InverseDynamicsJoint joint : jointsInOrder)
      {
         TIntArrayList listToPackIndices = new TIntArrayList();
         ScrewTools.computeIndexForJoint(jointsInOrder, listToPackIndices, joint);
         int[] indeces = listToPackIndices.toArray();

         columnsForJoints.put(joint, indeces);
      }

      pointJacobianConvectiveTermCalculator = new PointJacobianConvectiveTermCalculator(twistCalculator);

      parentRegistry.addChild(registry);
   }

   public void reset()
   {
      motionConstraintIndex = 0;
   }

   private final DenseMatrix64F tempBaseToEndEffectorJacobianMatrix = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempJacobianMatrix = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempMatrixUTranspose = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempMatrixW = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempMatrixVTranspose = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempMatrixCTranspose = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempMatrixCTransposeJ = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempPVector = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempCTransposeP = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tempNullSpaceMatrixNTranspose = new DenseMatrix64F(1, 1);

   private int svdRows = -1;
   private int svdCols = -1;
   private SingularValueDecomposition<DenseMatrix64F> svd;

   private SingularValueDecomposition<DenseMatrix64F> getCachedSVD(int rows, int cols)
   {
      if (rows != svdRows || cols != svdCols)
      {
         svd = DecompositionFactory.svd(rows, cols, true, true, false);
         svdRows = rows;
         svdCols = cols;
      }

      return svd;
   }

   public void setDesiredSpatialAcceleration(SpatialAccelerationCommand spatialAccelerationCommand)
   {
      // (S * J) * vdot = S * (Tdot - Jdot * v)
      GeometricJacobian jacobian = spatialAccelerationCommand.getJacobian();
      double weight = spatialAccelerationCommand.getWeight();

      SpatialAccelerationVector taskSpaceAcceleration = spatialAccelerationCommand.getSpatialAcceleration();
      DenseMatrix64F selectionMatrix = spatialAccelerationCommand.getSelectionMatrix();
      DenseMatrix64F nullspaceMultipliers = spatialAccelerationCommand.getNullspaceMultipliers();

      if (selectionMatrix.getNumRows() > 0)
      {
         RigidBody base = (spatialAccelerationCommand.getBase() == null) ? getBase(taskSpaceAcceleration) : spatialAccelerationCommand.getBase();
         RigidBody endEffector = (spatialAccelerationCommand.getEndEffector() == null) ? getEndEffector(taskSpaceAcceleration) : spatialAccelerationCommand.getEndEffector();

         long baseToEndEffectorJacobianId = geometricJacobianHolder.getOrCreateGeometricJacobian(base, endEffector, taskSpaceAcceleration.getExpressedInFrame());
         GeometricJacobian baseToEndEffectorJacobian = geometricJacobianHolder.getJacobian(baseToEndEffectorJacobianId);
         if (jacobiansHaveToBeUpdated)
            baseToEndEffectorJacobian.compute();

         // TODO: inefficient
         convectiveTermCalculator.computeJacobianDerivativeTerm(baseToEndEffectorJacobian, convectiveTerm);
         convectiveTerm.getBodyFrame().checkReferenceFrameMatch(taskSpaceAcceleration.getBodyFrame());
         convectiveTerm.getExpressedInFrame().checkReferenceFrameMatch(taskSpaceAcceleration.getExpressedInFrame());
         convectiveTerm.getMatrix(convectiveTermMatrix, 0);

         jBlockCompact.reshape(selectionMatrix.getNumRows(), baseToEndEffectorJacobian.getNumberOfColumns());

         tempBaseToEndEffectorJacobianMatrix.set(baseToEndEffectorJacobian.getJacobianMatrix());
         tempJacobianMatrix.set(jacobian.getJacobianMatrix());

         int nullity = nullspaceMultipliers.getNumRows();
         if (nullity > 0)
         {
            // Take the singular value decomposition of the leg Jacobian to find the approximate nullspace.
            SingularValueDecomposition<DenseMatrix64F> svd = getCachedSVD(tempJacobianMatrix.getNumRows(), tempJacobianMatrix.getNumCols());
            svd.decompose(tempJacobianMatrix);

            tempMatrixUTranspose.reshape(tempJacobianMatrix.getNumRows(), tempJacobianMatrix.getNumRows());
            tempMatrixW.reshape(tempJacobianMatrix.getNumRows(), tempJacobianMatrix.getNumCols());
            tempMatrixVTranspose.reshape(tempJacobianMatrix.getNumCols(), tempJacobianMatrix.getNumCols());
            svd.getU(tempMatrixUTranspose, true);
            svd.getW(tempMatrixW);
            svd.getV(tempMatrixVTranspose, true);

            SingularOps.descendingOrder(tempMatrixUTranspose, true, tempMatrixW, tempMatrixVTranspose, true);

            tempMatrixCTranspose.reshape(tempJacobianMatrix.getNumRows() - nullity, tempJacobianMatrix.getNumCols());
            CommonOps.extract(tempMatrixUTranspose, 0, tempJacobianMatrix.getNumRows() - nullity, 0, tempJacobianMatrix.getNumCols(), tempMatrixCTranspose, 0,
                  0);

            tempMatrixCTransposeJ.reshape(tempMatrixCTranspose.getNumRows(), tempBaseToEndEffectorJacobianMatrix.getNumCols());
            CommonOps.mult(tempMatrixCTranspose, tempBaseToEndEffectorJacobianMatrix, tempMatrixCTransposeJ);

            taskSpaceAcceleration.getMatrix(taskSpaceAccelerationMatrix, 0);

            tempPVector.reshape(selectionMatrix.getNumRows(), 1);
            CommonOps.mult(selectionMatrix, taskSpaceAccelerationMatrix, tempPVector);
            CommonOps.multAdd(-1.0, selectionMatrix, convectiveTermMatrix, tempPVector);

            tempCTransposeP.reshape(tempMatrixCTranspose.getNumRows(), tempPVector.getNumCols());
            CommonOps.mult(tempMatrixCTranspose, tempPVector, tempCTransposeP);

            DenseMatrix64F pBlock;
            if (removeNullspaceFromJ.getBooleanValue())
            {
               pBlock = getMatrixFromList(pList, motionConstraintIndex, selectionMatrix.getNumRows(), 1);

               CommonOps.extract(tempCTransposeP, 0, tempCTransposeP.getNumRows(), 0, tempCTransposeP.getNumCols(), pBlock, 0, 0);
               CommonOps.extract(nullspaceMultipliers, 0, nullspaceMultipliers.getNumRows(), 0, nullspaceMultipliers.getNumCols(), pBlock,
                     tempCTransposeP.getNumRows(), 0);
            }
            else
            {
               jBlockCompact.reshape(selectionMatrix.getNumRows() + 1, baseToEndEffectorJacobian.getNumberOfColumns()); // Add a row instead of substitute a row.
               pBlock = getMatrixFromList(pList, motionConstraintIndex, selectionMatrix.getNumRows() + 1, 1);

               CommonOps.extract(tempPVector, 0, tempPVector.getNumRows(), 0, tempPVector.getNumCols(), pBlock, 0, 0);
               CommonOps.extract(nullspaceMultipliers, 0, nullspaceMultipliers.getNumRows(), 0, nullspaceMultipliers.getNumCols(), pBlock,
                     tempPVector.getNumRows(), 0);
            }

            jBlockCompact.zero();

            tempNullSpaceMatrixNTranspose.reshape(nullity, tempMatrixVTranspose.getNumCols());
            CommonOps.extract(tempMatrixVTranspose, tempMatrixVTranspose.getNumRows() - nullity, tempMatrixVTranspose.getNumRows(), 0,
                  tempMatrixVTranspose.getNumCols(), tempNullSpaceMatrixNTranspose, 0, 0);
            NullspaceCalculator.makeLargestComponentInEachColumnPositive(tempNullSpaceMatrixNTranspose);

            if (removeNullspaceFromJ.getBooleanValue())
            {
               //TODO: Fix the null space stuff. Verify that the three things added are correct. Figure out why qdd is way off from desired.
               CommonOps.extract(tempMatrixCTransposeJ, 0, tempMatrixCTransposeJ.getNumRows(), 0, tempMatrixCTransposeJ.getNumCols(), jBlockCompact, 0, 0);
               CommonOps.extract(tempNullSpaceMatrixNTranspose, 0, tempNullSpaceMatrixNTranspose.getNumRows(), 0, tempNullSpaceMatrixNTranspose.getNumCols(),
                     jBlockCompact, tempMatrixCTransposeJ.getNumRows(), 6); //Hack 6
            }
            else
            {
               try
               {
                  CommonOps.extract(tempBaseToEndEffectorJacobianMatrix, 0, tempBaseToEndEffectorJacobianMatrix.getNumRows(), 0,
                        tempBaseToEndEffectorJacobianMatrix.getNumCols(), jBlockCompact, 0, 0);
                  CommonOps.extract(tempNullSpaceMatrixNTranspose, 0, tempNullSpaceMatrixNTranspose.getNumRows(), 0, tempNullSpaceMatrixNTranspose.getNumCols(),
                        jBlockCompact, tempBaseToEndEffectorJacobianMatrix.getNumRows(), 6); //Hack 6
               }
               catch (IllegalArgumentException e)
               {
                  System.out.println("blop");
               }
            }

            DenseMatrix64F jFullBlock = getMatrixFromList(jList, motionConstraintIndex, jBlockCompact.getNumRows(), nDegreesOfFreedom);
            compactBlockToFullBlock(baseToEndEffectorJacobian.getJointsInOrder(), jBlockCompact, jFullBlock);

            MutableDouble weightBlock = getMutableDoubleFromList(weightList, motionConstraintIndex);
            weightBlock.setValue(weight);

            motionConstraintIndex++;
         }
         else
         {
            CommonOps.mult(selectionMatrix, tempBaseToEndEffectorJacobianMatrix, jBlockCompact);

            DenseMatrix64F pBlock = getMatrixFromList(pList, motionConstraintIndex, selectionMatrix.getNumRows(), 1);
            taskSpaceAcceleration.getMatrix(taskSpaceAccelerationMatrix, 0);
            CommonOps.mult(selectionMatrix, taskSpaceAccelerationMatrix, pBlock);
            CommonOps.multAdd(-1.0, selectionMatrix, convectiveTermMatrix, pBlock);

            DenseMatrix64F jFullBlock = getMatrixFromList(jList, motionConstraintIndex, jBlockCompact.getNumRows(), nDegreesOfFreedom);
            compactBlockToFullBlock(baseToEndEffectorJacobian.getJointsInOrder(), jBlockCompact, jFullBlock);

            MutableDouble weightBlock = getMutableDoubleFromList(weightList, motionConstraintIndex);
            weightBlock.setValue(weight);

            motionConstraintIndex++;
         }
      }
   }

   private void compactBlockToFullBlock(InverseDynamicsJoint[] joints, DenseMatrix64F compactMatrix, DenseMatrix64F fullBlock)
   {
      fullBlock.zero();

      for (int index = 0; index < joints.length; index++)
      {
         InverseDynamicsJoint joint = joints[index];
         indicesIntoCompactBlock.reset();
         ScrewTools.computeIndexForJoint(joints, indicesIntoCompactBlock, joint);
         int[] indicesIntoFullBlock = columnsForJoints.get(joint);

         if (indicesIntoFullBlock == null) // don't do anything for joints that are not in the list
            return;

         for (int i = 0; i < indicesIntoCompactBlock.size(); i++)
         {
            int compactBlockIndex = indicesIntoCompactBlock.get(i);
            int fullBlockIndex = indicesIntoFullBlock[i];
            CommonOps.extract(compactMatrix, 0, compactMatrix.getNumRows(), compactBlockIndex, compactBlockIndex + 1, fullBlock, 0, fullBlockIndex);
         }
      }
   }

   public void setDesiredJointAcceleration(JointspaceAccelerationCommand jointspaceAccelerationCommand)
   {
      for (int jointIndex = 0; jointIndex < jointspaceAccelerationCommand.getNumberOfJoints(); jointIndex++)
      {
         InverseDynamicsJoint joint = jointspaceAccelerationCommand.getJoint(jointIndex);
         DenseMatrix64F jointAcceleration = jointspaceAccelerationCommand.getDesiredAcceleration(jointIndex);
         double weight = jointspaceAccelerationCommand.getWeight();

         MathTools.checkIfEqual(joint.getDegreesOfFreedom(), jointAcceleration.getNumRows());
         int[] columnsForJoint = this.columnsForJoints.get(joint);

         if (columnsForJoint != null) // don't do anything for joints that are not in the list
         {
            DenseMatrix64F jBlock = getMatrixFromList(jList, motionConstraintIndex, jointAcceleration.getNumRows(), nDegreesOfFreedom);
            jBlock.zero();

            for (int i = 0; i < jointAcceleration.getNumRows(); i++)
            {
               jBlock.set(i, columnsForJoint[i], 1.0);
            }

            DenseMatrix64F pBlock = getMatrixFromList(pList, motionConstraintIndex, jointAcceleration.getNumRows(), 1);
            pBlock.set(jointAcceleration);

            MutableDouble weightBlock = getMutableDoubleFromList(weightList, motionConstraintIndex);
            weightBlock.setValue(weight);

            motionConstraintIndex++;
         }
      }
   }

   private final DenseMatrix64F tempPPointMatrixVelocity = new DenseMatrix64F(3, 1);

   public void setDesiredPointAcceleration(GeometricJacobian jacobian, FramePoint bodyFixedPoint, FrameVector desiredAccelerationWithRespectToBase,
         DenseMatrix64F selectionMatrix, double weight)
   {
      pointJacobian.set(jacobian, bodyFixedPoint);
      pointJacobian.compute();
      desiredAccelerationWithRespectToBase.changeFrame(jacobian.getBaseFrame());

      DenseMatrix64F pointJacobianMatrix = pointJacobian.getJacobianMatrix();

      jBlockCompact.reshape(selectionMatrix.getNumRows(), pointJacobianMatrix.getNumCols());
      CommonOps.mult(selectionMatrix, pointJacobianMatrix, jBlockCompact);

      DenseMatrix64F jFullBlock = getMatrixFromList(jList, motionConstraintIndex, jBlockCompact.getNumRows(), nDegreesOfFreedom);
      compactBlockToFullBlock(jacobian.getJointsInOrder(), jBlockCompact, jFullBlock);

      pointJacobianConvectiveTermCalculator.compute(pointJacobian, pPointVelocity);
      pPointVelocity.scale(-1.0);
      pPointVelocity.add(desiredAccelerationWithRespectToBase);
      DenseMatrix64F pBlock = getMatrixFromList(pList, motionConstraintIndex, selectionMatrix.getNumRows(), 1);
      MatrixTools.setDenseMatrixFromTuple3d(tempPPointMatrixVelocity, pPointVelocity.getVector(), 0, 0);
      CommonOps.mult(selectionMatrix, tempPPointMatrixVelocity, pBlock);

      MutableDouble weightBlock = getMutableDoubleFromList(weightList, motionConstraintIndex);
      weightBlock.setValue(weight);

      motionConstraintIndex++;
   }

   public void setDesiredPointAcceleration(GeometricJacobian jacobian, FramePoint bodyFixedPoint, FrameVector desiredAccelerationWithRespectToBase,
         double weight)
   {
      pointJacobian.set(jacobian, bodyFixedPoint);
      pointJacobian.compute();
      desiredAccelerationWithRespectToBase.changeFrame(jacobian.getBaseFrame());

      DenseMatrix64F pointJacobianMatrix = pointJacobian.getJacobianMatrix();
      DenseMatrix64F jFullBlock = getMatrixFromList(jList, motionConstraintIndex, pointJacobianMatrix.getNumRows(), nDegreesOfFreedom);
      compactBlockToFullBlock(jacobian.getJointsInOrder(), pointJacobianMatrix, jFullBlock);

      pointJacobianConvectiveTermCalculator.compute(pointJacobian, pPointVelocity);
      pPointVelocity.scale(-1.0);
      pPointVelocity.add(desiredAccelerationWithRespectToBase);
      DenseMatrix64F pBlock = getMatrixFromList(pList, motionConstraintIndex, pointJacobianMatrix.getNumRows(), 1);
      MatrixTools.setDenseMatrixFromTuple3d(pBlock, pPointVelocity.getVector(), 0, 0);

      MutableDouble weightBlock = getMutableDoubleFromList(weightList, motionConstraintIndex);
      weightBlock.setValue(weight);

      motionConstraintIndex++;
   }

   public void compute()
   {
      assembleEquation(jList, pList, motionConstraintIndex, j, p);
      assembleWeightMatrix(weightList, jList, motionConstraintIndex, w);
   }

   public void getMotionConstraintJMatrixPVectorAndWeight(int motionConstraintIndex, DenseMatrix64F jToPack, DenseMatrix64F pToPack, MutableDouble wToPack)
   {
      jToPack.set(jList.get(motionConstraintIndex));
      pToPack.set(pList.get(motionConstraintIndex));
      wToPack.setValue(weightList.get(motionConstraintIndex));
   }

   private void assembleEquation(List<DenseMatrix64F> matrixList, List<DenseMatrix64F> vectorList, int size, DenseMatrix64F matrix, DenseMatrix64F vector)
   {
      if (matrixList.size() != vectorList.size())
         throw new RuntimeException("sizes not equal");

      int nRows = 0;
      for (int i = 0; i < size; i++)
      {
         nRows += matrixList.get(i).getNumRows();
      }

      matrix.reshape(nRows, nDegreesOfFreedom);
      vector.reshape(nRows, 1);
      matrix.zero();
      vector.zero();

      int rowNumber = 0;
      for (int i = 0; i < size; i++)
      {
         DenseMatrix64F matrixBlock = matrixList.get(i);
         CommonOps.insert(matrixBlock, matrix, rowNumber, 0);

         DenseMatrix64F vectorBlock = vectorList.get(i);
         CommonOps.insert(vectorBlock, vector, rowNumber, 0);

         rowNumber += matrixBlock.getNumRows();
      }
   }

   private void assembleWeightMatrix(List<MutableDouble> weightList, List<DenseMatrix64F> jacobianList, int nConstraints, DenseMatrix64F weightMatrix)
   {
      MathTools.checkIfEqual(weightList.size(), jacobianList.size());

      int size = 0;
      for (int i = 0; i < nConstraints; i++)
      {
         size += jacobianList.get(i).getNumRows();
      }

      weightMatrix.reshape(size, size);
      weightMatrix.zero();
      int matrixIndex = 0;
      for (int i = 0; i < nConstraints; i++)
      {
         double weight = weightList.get(i).doubleValue();
         int blockSize = jacobianList.get(i).getNumRows();
         for (int blockIndex = 0; blockIndex < blockSize; blockIndex++)
         {
            weightMatrix.set(matrixIndex, matrixIndex, weight);
            matrixIndex++;
         }
      }
   }

   public DenseMatrix64F getJacobian()
   {
      return j;
   }

   public DenseMatrix64F getRightHandSide()
   {
      return p;
   }

   public DenseMatrix64F getWeightMatrix()
   {
      return w;
   }

   private static DenseMatrix64F getMatrixFromList(List<DenseMatrix64F> matrixList, int index, int nRows, int nColumns)
   {
      for (int i = matrixList.size(); i <= index; i++)
      {
         matrixList.add(new DenseMatrix64F(1, 1));
      }

      DenseMatrix64F ret = matrixList.get(index);
      ret.reshape(nRows, nColumns);

      return ret;
   }

   private static MutableDouble getMutableDoubleFromList(List<MutableDouble> mutableDoubleList, int index)
   {
      for (int i = mutableDoubleList.size(); i <= index; i++)
      {
         mutableDoubleList.add(new MutableDouble());
      }

      MutableDouble ret = mutableDoubleList.get(index);

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
