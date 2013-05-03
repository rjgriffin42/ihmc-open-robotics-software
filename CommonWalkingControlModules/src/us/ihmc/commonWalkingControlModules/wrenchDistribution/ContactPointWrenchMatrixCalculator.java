package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.WrenchDistributorTools;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization.ContactPointWrenchOptimizerNative;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.Wrench;

import java.util.*;

/**
 * @author twan
 *         Date: 5/1/13
 */
public class ContactPointWrenchMatrixCalculator
{
   private final ReferenceFrame centerOfMassFrame;

   private final DenseMatrix64F q;
   private final Map<PlaneContactState, Wrench> wrenches = new LinkedHashMap<PlaneContactState, Wrench>();

   // intermediate result storage:
   private final ArrayList<FrameVector> normalizedSupportVectors = new ArrayList<FrameVector>(ContactPointWrenchOptimizerNative.NUMBER_OF_SUPPORT_VECTORS);
   private final FramePoint tempContactPoint = new FramePoint(ReferenceFrame.getWorldFrame());
   private final FrameVector tempVector = new FrameVector(ReferenceFrame.getWorldFrame());
   private final DenseMatrix64F qBlock = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F rhoBlock = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F wrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
   private DenseMatrix64F rhoMin;

   public ContactPointWrenchMatrixCalculator(ReferenceFrame centerOfMassFrame, int nSupportVectorsPerContactPoint, int nColumns)
   {
      this.centerOfMassFrame = centerOfMassFrame;

      for (int i = 0; i < nSupportVectorsPerContactPoint; i++)
      {
         normalizedSupportVectors.add(new FrameVector(ReferenceFrame.getWorldFrame()));
      }
      q = new DenseMatrix64F(Wrench.SIZE, nColumns);
   }

   public DenseMatrix64F getRhoMin(Collection<? extends PlaneContactState> contactStates, double rhoMinScalar)
   {
      rhoMin.zero();
      int index = 0;
      for (PlaneContactState contactState : contactStates)
      {
         for (int i = 0; i < contactState.getNumberOfContactPoints(); i++)
         {
            for (int j = 0; j < normalizedSupportVectors.size(); j++)
            {
               rhoMin.set(index++, 0, rhoMinScalar);
            }
         }
      }

      return rhoMin;
   }

   public void computeMatrix(Collection<? extends PlaneContactState> contactStates)
   {
      q.zero();

      int column = 0;
      for (PlaneContactState contactState : contactStates)
      {
         List<FramePoint2d> contactPoints2d = contactState.getContactPoints2d();
         WrenchDistributorTools.getSupportVectors(normalizedSupportVectors, contactState.getCoefficientOfFriction(), contactState.getPlaneFrame());

         for (FramePoint2d contactPoint2d : contactPoints2d)
         {
            // torque part of A
            tempContactPoint.set(contactPoint2d.getReferenceFrame(), contactPoint2d.getX(), contactPoint2d.getY(), 0.0);
            tempContactPoint.changeFrame(centerOfMassFrame);

            for (FrameVector supportVector : normalizedSupportVectors)
            {
               supportVector.changeFrame(centerOfMassFrame);
               int startRow = Wrench.SIZE / 2;
               MatrixTools.setDenseMatrixFromTuple3d(q, supportVector.getVector(), startRow, column);

               tempVector.setToZero(centerOfMassFrame);
               tempVector.cross(tempContactPoint, supportVector);
               int startRow1 = 0;
               MatrixTools.setDenseMatrixFromTuple3d(q, tempVector.getVector(), startRow1, column);
               column++;
            }
         }
      }
   }

   public DenseMatrix64F getMatrix()
   {
      return q;
   }

   public void computeWrenches(Collection<? extends PlaneContactState> contactStates, DenseMatrix64F rho)
   {
      int columnNumber = 0;
      for (PlaneContactState contactState : contactStates)
      {
         int nColumns = contactState.getNumberOfContactPoints() * normalizedSupportVectors.size();
         qBlock.reshape(Wrench.SIZE, nColumns);
         CommonOps.extract(q, 0, Wrench.SIZE, columnNumber, columnNumber + nColumns, qBlock, 0, 0);

         rhoBlock.reshape(nColumns, 1);
         CommonOps.extract(rho, columnNumber, columnNumber + nColumns, 0, 1, rhoBlock, 0, 0);

         CommonOps.mult(qBlock, rhoBlock, wrenchMatrix);

         Wrench wrench = getOrCreateWrench(contactState);
         wrench.set(centerOfMassFrame, wrenchMatrix);

         columnNumber += nColumns;
      }
   }

   public Wrench getWrench(PlaneContactState contactState)
   {
      return wrenches.get(contactState);
   }

   private Wrench getOrCreateWrench(PlaneContactState contactState)
   {
      Wrench wrench = wrenches.get(contactState);
      if (wrench == null)
      {
         wrench = new Wrench(contactState.getBodyFrame(), centerOfMassFrame);
         wrenches.put(contactState, wrench);
      }
      return wrench;
   }
}
