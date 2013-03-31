package us.ihmc.sensorProcessing.simulatedSensors;

import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;

public class SimulatedLinearAccelerationSensor extends SimulatedSensor<Vector3d>
{
   private final RigidBody rigidBody;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final FramePoint imuFramePoint = new FramePoint(worldFrame);

   private final FrameVector linearAccelerationFrameVector = new FrameVector(worldFrame);
   private final Vector3d linearAcceleration = new Vector3d();

   private final ReferenceFrame measurementFrame;
   private final SpatialAccelerationCalculator spatialAccelerationCalculator;

   private final ControlFlowOutputPort<Vector3d> linearAccelerationOutputPort = createOutputPort();
   private final FrameVector gravitationalAcceleration;

   public SimulatedLinearAccelerationSensor(String name, RigidBody rigidBody, ReferenceFrame measurementFrame,
           SpatialAccelerationCalculator spatialAccelerationCalculator, Vector3d gravitationalAcceleration)
   {
      this.rigidBody = rigidBody;
      this.measurementFrame = measurementFrame;
      this.spatialAccelerationCalculator = spatialAccelerationCalculator;
      this.gravitationalAcceleration = new FrameVector(ReferenceFrame.getWorldFrame(), gravitationalAcceleration);
   }

   public void startComputation()
   {
      imuFramePoint.setToZero(measurementFrame);
      spatialAccelerationCalculator.packLinearAccelerationOfBodyFixedPoint(linearAccelerationFrameVector, rigidBody, imuFramePoint);
      linearAccelerationFrameVector.changeFrame(gravitationalAcceleration.getReferenceFrame());
      linearAccelerationFrameVector.add(gravitationalAcceleration);
      linearAccelerationFrameVector.changeFrame(measurementFrame);
      linearAccelerationFrameVector.getVector(linearAcceleration);
      linearAccelerationOutputPort.setData(linearAcceleration);
   }

   public void waitUntilComputationIsDone()
   {
      // empty
   }

   public ControlFlowOutputPort<Vector3d> getLinearAccelerationOutputPort()
   {
      return linearAccelerationOutputPort;
   }
}
