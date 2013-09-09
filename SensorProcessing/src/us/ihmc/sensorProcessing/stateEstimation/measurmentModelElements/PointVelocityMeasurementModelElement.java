package us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.RigidBodyToIndexMap;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocityDataObject;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.AfterJointReferenceFrameNameMap;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class PointVelocityMeasurementModelElement extends AbstractMeasurementModelElement
{
   public static final int SIZE = 3;

   private final ControlFlowOutputPort<FramePoint> centerOfMassPositionPort;
   private final ControlFlowOutputPort<FrameVector> centerOfMassVelocityPort;
   
   private final boolean assumePerfectIMU;
   private final ControlFlowOutputPort<FrameOrientation> orientationPort;
   private final ControlFlowOutputPort<FrameVector> angularVelocityPort;

   private final ControlFlowInputPort<PointVelocityDataObject> pointVelocityMeasurementInputPort;

   private final ReferenceFrame estimationFrame;
   private final ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort;

   private final DenseMatrix64F residual = new DenseMatrix64F(SIZE, 1);

   private final Matrix3d rotationFromEstimationToWorld = new Matrix3d();
   private final Transform3D tempTransform = new Transform3D();
   private final Matrix3d tempMatrix = new Matrix3d();
   private final FramePoint tempFramePoint = new FramePoint(ReferenceFrame.getWorldFrame());
   private final Twist tempTwist = new Twist();
   private final FrameVector tempFrameVector = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempFrameVector2 = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector residualVector = new FrameVector(ReferenceFrame.getWorldFrame());

   
   private final AfterJointReferenceFrameNameMap referenceFrameNameMap;
   private final RigidBodyToIndexMap estimatorRigidBodyToIndexMap;
   

   public PointVelocityMeasurementModelElement(String name, ControlFlowInputPort<PointVelocityDataObject> pointVelocityMeasurementInputPort,
         ControlFlowOutputPort<FramePoint> centerOfMassPositionPort, ControlFlowOutputPort<FrameVector> centerOfMassVelocityPort,
         ControlFlowOutputPort<FrameOrientation> orientationPort, ControlFlowOutputPort<FrameVector> angularVelocityPort, ReferenceFrame estimationFrame,
         ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort, AfterJointReferenceFrameNameMap referenceFrameNameMap,
         RigidBodyToIndexMap estimatorRigidBodyToIndexMap, boolean assumePerfectIMU, YoVariableRegistry registry)
   {
      super(SIZE, name, registry);

      this.assumePerfectIMU = assumePerfectIMU;
      
      this.centerOfMassPositionPort = centerOfMassPositionPort;
      this.centerOfMassVelocityPort = centerOfMassVelocityPort;
      this.orientationPort = orientationPort;
      this.angularVelocityPort = angularVelocityPort;

      this.pointVelocityMeasurementInputPort = pointVelocityMeasurementInputPort;
      this.pointVelocityMeasurementInputPort.setData(new PointVelocityDataObject());

      this.estimationFrame = estimationFrame;
      this.inverseDynamicsStructureInputPort = inverseDynamicsStructureInputPort;
      
      this.referenceFrameNameMap = referenceFrameNameMap;
      this.estimatorRigidBodyToIndexMap = estimatorRigidBodyToIndexMap;

      outputMatrixBlocks.put(centerOfMassPositionPort, new DenseMatrix64F(SIZE, SIZE));
      outputMatrixBlocks.put(centerOfMassVelocityPort, new DenseMatrix64F(SIZE, SIZE));
      
      if (!assumePerfectIMU)
      {
         outputMatrixBlocks.put(orientationPort, new DenseMatrix64F(SIZE, SIZE));
         outputMatrixBlocks.put(angularVelocityPort, new DenseMatrix64F(SIZE, SIZE));
      }
      
      computeCenterOfMassVelocityStateOutputBlock();
   }

   public ControlFlowInputPort<PointVelocityDataObject> getPointVelocityMeasurementInputPort()
   {
      return pointVelocityMeasurementInputPort;
   }

   public void computeMatrixBlocks()
   {
      estimationFrame.getTransformToDesiredFrame(tempTransform, ReferenceFrame.getWorldFrame());
      tempTransform.get(rotationFromEstimationToWorld);
      
      if (!assumePerfectIMU)
      {
         computeOrientationStateOutputBlock(rotationFromEstimationToWorld);
         computeAngularVelocityStateOutputBlock(rotationFromEstimationToWorld);
      }
   }

   private void computeCenterOfMassVelocityStateOutputBlock()
   {
      CommonOps.setIdentity(outputMatrixBlocks.get(centerOfMassVelocityPort));
   }

   private final FramePoint tempCenterOfMassPosition = new FramePoint();
   private void computeAngularVelocityStateOutputBlock(Matrix3d rotationFromPelvisToWorld)
   {
      tempCenterOfMassPosition.setAndChangeFrame(centerOfMassPositionPort.getData());
      tempCenterOfMassPosition.changeFrame(estimationFrame);

      ReferenceFrame referenceFrame = referenceFrameNameMap.getFrameByName(pointVelocityMeasurementInputPort.getData().getBodyFixedReferenceFrameName());
      tempFramePoint.setAndChangeFrame(referenceFrame, pointVelocityMeasurementInputPort.getData().getMeasurementPointInBodyFrame());
      
      tempFramePoint.changeFrame(estimationFrame);
      tempFramePoint.sub(tempCenterOfMassPosition);
      tempFramePoint.scale(-1.0);

      MatrixTools.toTildeForm(tempMatrix, tempFramePoint.getPoint());
      tempMatrix.mul(rotationFromPelvisToWorld, tempMatrix);
      MatrixTools.setDenseMatrixFromMatrix3d(0, 0, tempMatrix, outputMatrixBlocks.get(angularVelocityPort));
   }

   private final FrameVector tempCenterOfMassVelocity = new FrameVector();
   private void computeOrientationStateOutputBlock(Matrix3d rotationFromPelvisToWorld)
   {
      computeVelocityOfStationaryPoint(tempFrameVector);
      tempFrameVector.changeFrame(estimationFrame);

      tempCenterOfMassVelocity.setAndChangeFrame(centerOfMassVelocityPort.getData());
      tempCenterOfMassVelocity.changeFrame(estimationFrame);
      tempFrameVector.sub(tempCenterOfMassVelocity);
      tempFrameVector.scale(-1.0);

      MatrixTools.toTildeForm(tempMatrix, tempFrameVector.getVector());
      tempMatrix.mul(rotationFromPelvisToWorld, tempMatrix);
      MatrixTools.setDenseMatrixFromMatrix3d(0, 0, tempMatrix, outputMatrixBlocks.get(orientationPort));
   }

   public DenseMatrix64F computeResidual()
   {
      computeVelocityOfStationaryPoint(tempFrameVector);
      tempFrameVector.changeFrame(ReferenceFrame.getWorldFrame());
      tempFrameVector2.setAndChangeFrame(ReferenceFrame.getWorldFrame(), pointVelocityMeasurementInputPort.getData().getVelocityOfMeasurementPointInWorldFrame());

      residualVector.setAndChangeFrame(tempFrameVector2);
      residualVector.sub(tempFrameVector);

      MatrixTools.insertTuple3dIntoEJMLVector(residualVector.getVector(), residual, 0);

      return residual;
   }

   private void computeVelocityOfStationaryPoint(FrameVector stationaryPointVelocityToPack)
   {
      FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
      RigidBody stationaryPointLink = estimatorRigidBodyToIndexMap.getRigidBodyByName(pointVelocityMeasurementInputPort.getData().getRigidBodyName());

      TwistCalculator twistCalculator = inverseDynamicsStructure.getTwistCalculator();

      try
      {
         twistCalculator.packTwistOfBody(tempTwist, stationaryPointLink);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
      tempTwist.changeFrame(tempTwist.getBaseFrame());
      ReferenceFrame referenceFrame = referenceFrameNameMap.getFrameByName(pointVelocityMeasurementInputPort.getData().getBodyFixedReferenceFrameName());
      tempFramePoint.setAndChangeFrame(referenceFrame, pointVelocityMeasurementInputPort.getData().getMeasurementPointInBodyFrame());
      tempFramePoint.changeFrame(tempTwist.getBaseFrame());
      tempTwist.packVelocityOfPointFixedInBodyFrame(stationaryPointVelocityToPack, tempFramePoint);
   }

}
