package us.ihmc.commonWalkingControlModules.stateEstimation.processModelElements;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.stateEstimation.TimeDomain;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class BiasProcessModelElement extends AbstractProcessModelElement
{
   private static final int SIZE = 3;
   private final ControlFlowOutputPort<FrameVector> biasPort;
   private final FrameVector bias;
   private final FrameVector biasDelta;

   public BiasProcessModelElement(ControlFlowOutputPort<FrameVector> statePort, ReferenceFrame frame, String name, YoVariableRegistry registry)
   {
      super(statePort, TimeDomain.CONTINUOUS, false, SIZE, name, registry);
      this.biasPort = statePort;
      this.bias = new FrameVector(frame);
      this.biasDelta = new FrameVector(frame);
   }

   public void computeMatrixBlocks()
   {
      // empty
   }

   public void propagateState(double dt)
   {
      // empty
   }

   public void correctState(DenseMatrix64F correction)
   {
      MatrixTools.extractTuple3dFromEJMLVector(biasDelta.getVector(), correction, 0);
      updateBias(biasDelta);
   }

   private void updateBias(FrameVector biasDelta)
   {
      bias.set(biasPort.getData());
      bias.add(biasDelta);
      biasPort.setData(bias);
   }
}
