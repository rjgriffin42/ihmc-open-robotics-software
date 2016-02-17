package us.ihmc.sensorProcessing.simulatedSensors;

import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SpatialAccelerationCalculator;


public class SimulatedLinearAccelerationSensor extends SimulatedSensor<Vector3d>
{
   private final RigidBody rigidBody;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final FramePoint imuFramePoint = new FramePoint(worldFrame);

   private final FrameVector linearAccelerationFrameVector = new FrameVector(worldFrame);
   private final Vector3d linearAcceleration = new Vector3d();
   private final YoFrameVector yoFrameVectorPerfect, yoFrameVectorNoisy;

   private final ReferenceFrame measurementFrame;
   private final SpatialAccelerationCalculator spatialAccelerationCalculator;

   private final ControlFlowOutputPort<Vector3d> linearAccelerationOutputPort = createOutputPort("linearAccelerationOutputPort");
   private final FrameVector gravitationalAcceleration;

   public SimulatedLinearAccelerationSensor(String name, RigidBody rigidBody, ReferenceFrame measurementFrame,
           SpatialAccelerationCalculator spatialAccelerationCalculator, Vector3d gravitationalAcceleration, YoVariableRegistry registry)
   {
      this.rigidBody = rigidBody;
      this.measurementFrame = measurementFrame;
      this.spatialAccelerationCalculator = spatialAccelerationCalculator;
      this.gravitationalAcceleration = new FrameVector(ReferenceFrame.getWorldFrame(), gravitationalAcceleration);
      
      this.yoFrameVectorPerfect = new YoFrameVector(name + "Perfect", measurementFrame, registry);
      this.yoFrameVectorNoisy = new YoFrameVector(name + "Noisy", measurementFrame, registry);
   }

   public void startComputation()
   {
      imuFramePoint.setToZero(measurementFrame);
      spatialAccelerationCalculator.getLinearAccelerationOfBodyFixedPoint(linearAccelerationFrameVector, rigidBody, imuFramePoint);
      linearAccelerationFrameVector.changeFrame(gravitationalAcceleration.getReferenceFrame());
      linearAccelerationFrameVector.add(gravitationalAcceleration);
      linearAccelerationFrameVector.changeFrame(measurementFrame);
      linearAccelerationFrameVector.get(linearAcceleration);
      yoFrameVectorPerfect.set(linearAcceleration);

      corrupt(linearAcceleration);
      yoFrameVectorNoisy.set(linearAcceleration);

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

   public void initialize()
   {
//    empty
   }
}
