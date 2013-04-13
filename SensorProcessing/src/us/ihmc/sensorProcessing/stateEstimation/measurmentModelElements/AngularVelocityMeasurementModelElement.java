package us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements;


import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class AngularVelocityMeasurementModelElement extends AbstractMeasurementModelElement
{
   static final int SIZE = 3;
   private final ControlFlowOutputPort<FrameVector> angularVelocityStatePort;
   private final ControlFlowOutputPort<FrameVector> biasStatePort;
   private final ControlFlowInputPort<Vector3d> angularVelocityMeasurementInputPort;

   private final RigidBody orientationEstimationLink;
   private final ReferenceFrame estimationFrame;

   private final RigidBody measurementLink;
   private final ReferenceFrame measurementFrame;

   private final ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort;
   private final DenseMatrix64F residual = new DenseMatrix64F(SIZE, 1);

   // temp stuff
   private final Twist tempTwist = new Twist();
   private final Transform3D tempTransform = new Transform3D();
   private final Matrix3d tempMatrix = new Matrix3d();
   private final FrameVector relativeAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector angularVelocityResidual;

   public AngularVelocityMeasurementModelElement(ControlFlowOutputPort<FrameVector> angularVelocityStatePort, ControlFlowOutputPort<FrameVector> biasStatePort,
           ControlFlowInputPort<Vector3d> angularVelocityMeasurementInputPort, RigidBody orientationEstimationLink, ReferenceFrame estimationFrame,
           RigidBody measurementLink, ReferenceFrame measurementFrame, ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort, String name, YoVariableRegistry registry)
   {
      super(angularVelocityMeasurementInputPort, SIZE, name, registry);
      this.angularVelocityStatePort = angularVelocityStatePort;
      this.biasStatePort = biasStatePort;
      this.angularVelocityMeasurementInputPort = angularVelocityMeasurementInputPort;
      this.orientationEstimationLink = orientationEstimationLink;
      this.estimationFrame = estimationFrame;
      this.measurementLink = measurementLink;
      this.measurementFrame = measurementFrame;
      this.inverseDynamicsStructureInputPort = inverseDynamicsStructureInputPort;
      this.angularVelocityResidual = new FrameVector(measurementFrame);

      outputMatrixBlocks.put(angularVelocityStatePort, new DenseMatrix64F(SIZE, SIZE));
      outputMatrixBlocks.put(biasStatePort, new DenseMatrix64F(SIZE, SIZE));

      computeAngularVelocityStateOutputBlock();
      computeBiasStateOutputBlock();
   }

   private void computeAngularVelocityStateOutputBlock()
   {
      estimationFrame.getTransformToDesiredFrame(tempTransform, measurementFrame);
      tempTransform.get(tempMatrix);
      MatrixTools.setDenseMatrixFromMatrix3d(0, 0, tempMatrix, outputMatrixBlocks.get(angularVelocityStatePort));
   }

   private void computeBiasStateOutputBlock()
   {
      CommonOps.setIdentity(outputMatrixBlocks.get(biasStatePort));
   }

   public void computeMatrixBlocks()
   {
      computeAngularVelocityStateOutputBlock();
   }

   public DenseMatrix64F computeResidual()
   {
      Vector3d measuredAngularVelocityVector3d = angularVelocityMeasurementInputPort.getData();
      TwistCalculator twistCalculator = inverseDynamicsStructureInputPort.getData().getTwistCalculator();
      
      twistCalculator.packRelativeTwist(tempTwist, orientationEstimationLink, measurementLink);
      tempTwist.packAngularPart(relativeAngularVelocity);
      relativeAngularVelocity.changeFrame(measurementFrame);

      // TODO: garbage generation
      FrameVector predictedAngularVelocityMeasurement = new FrameVector(angularVelocityStatePort.getData());
      predictedAngularVelocityMeasurement.changeFrame(measurementFrame);
      predictedAngularVelocityMeasurement.add(relativeAngularVelocity);
      predictedAngularVelocityMeasurement.add(biasStatePort.getData());

      angularVelocityResidual.set(measurementFrame, measuredAngularVelocityVector3d);
      angularVelocityResidual.sub(predictedAngularVelocityMeasurement);
      MatrixTools.insertTuple3dIntoEJMLVector(angularVelocityResidual.getVector(), residual, 0);

      return residual;
   }

}
