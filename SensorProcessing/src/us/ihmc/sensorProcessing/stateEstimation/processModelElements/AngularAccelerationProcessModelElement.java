package us.ihmc.sensorProcessing.stateEstimation.processModelElements;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.TimeDomain;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class AngularAccelerationProcessModelElement extends AbstractProcessModelElement
{
   private static final int SIZE = 3;
   private final ControlFlowOutputPort<FrameVector> angularAccelerationStatePort;
   private final ControlFlowInputPort<FrameVector> angularAccelerationInputPort;
   private final FrameVector angularAcceleration;
   private final FrameVector angularAccelerationDelta;
   private final ReferenceFrame estimationFrame;
   private final Vector3d angularAccelerationVector3d = new Vector3d();

   public AngularAccelerationProcessModelElement(String name, ReferenceFrame estimationFrame, YoVariableRegistry registry,
           ControlFlowOutputPort<FrameVector> angularAccelerationStatePort, ControlFlowInputPort<FrameVector> angularAccelerationInputPort)
   {
      super(angularAccelerationStatePort, TimeDomain.DISCRETE, false, SIZE, name, registry);
      this.angularAccelerationStatePort = angularAccelerationStatePort;
      this.angularAccelerationInputPort = angularAccelerationInputPort;
      this.angularAcceleration = new FrameVector(estimationFrame);
      this.angularAccelerationDelta = new FrameVector(estimationFrame);

      this.estimationFrame = estimationFrame;
      computeAngularAccelerationInputMatrixBlock();
   }

   private void computeAngularAccelerationInputMatrixBlock()
   {
      DenseMatrix64F angularAccelerationInputBlock = new DenseMatrix64F(SIZE, SIZE);
      CommonOps.setIdentity(angularAccelerationInputBlock);
      inputMatrixBlocks.put(angularAccelerationInputPort, angularAccelerationInputBlock);
   }

   public void computeMatrixBlocks()
   {
      // empty
   }

   public void propagateState(double dt)
   {
      FrameVector angularAccelerationInputData = angularAccelerationInputPort.getData();

      // TODO: Figure out how to deal best with ReferenceFrames here.
      // Upon generation, the generator might not know what the estimation frame will
      // be. So here we're just making sure that the frame is null if it's not
      // the estimation frame.

      if (angularAccelerationInputData.getReferenceFrame() != null)
      {
         angularAccelerationInputData.checkReferenceFrameMatch(estimationFrame);
      }

      angularAccelerationInputData.get(angularAccelerationVector3d);
      angularAcceleration.setIncludingFrame(estimationFrame, angularAccelerationVector3d);
      angularAccelerationStatePort.setData(angularAcceleration);
   }

   public void correctState(DenseMatrix64F correction)
   {
      MatrixTools.extractTuple3dFromEJMLVector(angularAccelerationDelta.getVector(), correction, 0);
      angularAcceleration.set(angularAccelerationStatePort.getData());
      angularAcceleration.add(angularAccelerationDelta);
      angularAccelerationStatePort.setData(angularAcceleration);
   }
}
