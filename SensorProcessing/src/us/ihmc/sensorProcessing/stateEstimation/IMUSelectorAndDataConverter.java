package us.ihmc.sensorProcessing.stateEstimation;

import java.util.Collection;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.AbstractControlFlowElement;
import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFrameQuaternionControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFrameVectorControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.AngularVelocitySensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.OrientationSensorConfiguration;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class IMUSelectorAndDataConverter extends AbstractControlFlowElement
{
   private final ControlFlowInputPort<Matrix3d> orientationInputPort;
   private final ControlFlowInputPort<Vector3d> angularVelocityInputPort;
   private final ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort;

   private final ControlFlowOutputPort<FrameOrientation> orientationOutputPort;
   private final ControlFlowOutputPort<FrameVector> angularVelocityOutputPort;
   private final ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort;

   private final RigidBody estimationLink;
   private final RigidBody angularVelocityMeasurementLink;

   private final ReferenceFrame estimationFrame;
   private final ReferenceFrame orientationMeasurementFrame;
   private final ReferenceFrame angularVelocityMeasurementFrame;
   
   private final YoVariableRegistry registry;
   private final DoubleYoVariable imuSimulatedDriftYawAcceleration;
   private final DoubleYoVariable imuSimulatedDriftYawVelocity;
   private final DoubleYoVariable imuSimulatedDriftYawAngle;
   
   private final double estimatorDT;

   public IMUSelectorAndDataConverter(ControlFlowGraph controlFlowGraph, Collection<OrientationSensorConfiguration> orientationSensorConfigurations,
                                      Collection<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations,
                                      ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort, double estimatorDT, YoVariableRegistry registry)
   {
      OrientationSensorConfiguration selectedOrientationSensorConfiguration = null;
      if ((orientationSensorConfigurations.size() != 1) || (angularVelocitySensorConfigurations.size() != 1))
         throw new RuntimeException("We are assuming there is only 1 IMU for right now..");

      imuSimulatedDriftYawAcceleration = new DoubleYoVariable("imuSimulatedDriftYawAcceleration", registry);
      imuSimulatedDriftYawVelocity = new DoubleYoVariable("imuSimulatedDriftYawVelocity", registry);
      imuSimulatedDriftYawAngle = new DoubleYoVariable("imuSimulatedDriftYawAngle", registry);
      this.estimatorDT = estimatorDT;
      
      for (OrientationSensorConfiguration orientationSensorConfiguration : orientationSensorConfigurations)
      {
         selectedOrientationSensorConfiguration = orientationSensorConfiguration;
      }

      AngularVelocitySensorConfiguration selectedAngularVelocitySensorConfiguration = null;
      if ((orientationSensorConfigurations.size() != 1) || (angularVelocitySensorConfigurations.size() != 1))
         throw new RuntimeException("We are assuming there is only 1 IMU for right now..");

      for (AngularVelocitySensorConfiguration angularVelocitySensorConfiguration : angularVelocitySensorConfigurations)
      {
         selectedAngularVelocitySensorConfiguration = angularVelocitySensorConfiguration;
      }
      
      this.registry = registry;

      ControlFlowOutputPort<Matrix3d> orientationSensorOutputPort = selectedOrientationSensorConfiguration.getOutputPort();
      ControlFlowOutputPort<Vector3d> angularVelocitySensorOutputPort = selectedAngularVelocitySensorConfiguration.getOutputPort();

      this.orientationInputPort = createInputPort("orientationInputPort");
      this.angularVelocityInputPort = createInputPort("angularVelocityInputPort");
      this.inverseDynamicsStructureInputPort = createInputPort("inverseDynamicsStructureInputPort");

      controlFlowGraph.connectElements(orientationSensorOutputPort, orientationInputPort);
      controlFlowGraph.connectElements(angularVelocitySensorOutputPort, angularVelocityInputPort);
      controlFlowGraph.connectElements(inverseDynamicsStructureOutputPort, inverseDynamicsStructureInputPort);

      this.estimationLink = inverseDynamicsStructureOutputPort.getData().getEstimationLink();
      this.estimationFrame = inverseDynamicsStructureOutputPort.getData().getEstimationFrame();
      
      this.orientationMeasurementFrame = selectedOrientationSensorConfiguration.getMeasurementFrame();
      this.angularVelocityMeasurementLink = selectedAngularVelocitySensorConfiguration.getAngularVelocityMeasurementLink();
      this.angularVelocityMeasurementFrame = selectedAngularVelocitySensorConfiguration.getMeasurementFrame();
      
      this.inverseDynamicsStructureOutputPort = createOutputPort("inverseDynamicsStructureOutputPort");
      this.inverseDynamicsStructureInputPort.setData(inverseDynamicsStructureOutputPort.getData());
      this.inverseDynamicsStructureOutputPort.setData(inverseDynamicsStructureInputPort.getData());
      
      this.orientationOutputPort = new YoFrameQuaternionControlFlowOutputPort(this, "orientationOutput", ReferenceFrame.getWorldFrame(), registry);
      registerOutputPort(orientationOutputPort);
      this.angularVelocityOutputPort = new YoFrameVectorControlFlowOutputPort(this, "angularVelocityOutput", estimationFrame, registry);
      registerOutputPort(angularVelocityOutputPort);

   }

   public void startComputation()
   {
      imuSimulatedDriftYawVelocity.add(imuSimulatedDriftYawAcceleration.getDoubleValue() * estimatorDT);
      imuSimulatedDriftYawAngle.add(imuSimulatedDriftYawVelocity.getDoubleValue() * estimatorDT);
      imuSimulatedDriftYawAngle.set(AngleTools.trimAngleMinusPiToPi(imuSimulatedDriftYawAngle.getDoubleValue()));
      
      convertOrientationAndSetOnOutputPort();      
      convertAngularVelocityAndSetOnOutputPort();   
   }

   private final FrameOrientation tempOrientationEstimationFrame = new FrameOrientation(ReferenceFrame.getWorldFrame());

   private final FrameVector tempAngularVelocityMeasurementLink = new FrameVector();
   private final FrameVector tempAngularVelocityEstimationLink = new FrameVector();
   private final Twist tempRelativeTwistOrientationMeasFrameToEstFrame = new Twist();

   private final Transform3D transformFromEstimationFrameToIMUFrame = new Transform3D();
   private final FrameVector relativeAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());

   private final Transform3D transformFromIMUToWorld = new Transform3D();
   private final Transform3D transformFromEstimationToWorld = new Transform3D();
   private final Matrix3d rotationFromEstimationToWorld = new Matrix3d();
   
   private final double[] estimationFrameYawPitchRoll = new double[3];
   
   private void convertOrientationAndSetOnOutputPort()
   {
      transformFromIMUToWorld.set(orientationInputPort.getData());

      estimationFrame.getTransformToDesiredFrame(transformFromEstimationFrameToIMUFrame, orientationMeasurementFrame);
      
      transformFromEstimationToWorld.mul(transformFromIMUToWorld, transformFromEstimationFrameToIMUFrame);
      transformFromEstimationToWorld.get(rotationFromEstimationToWorld);
      tempOrientationEstimationFrame.set(ReferenceFrame.getWorldFrame(), rotationFromEstimationToWorld);
      
      // Introduce simulated IMU drift
      tempOrientationEstimationFrame.getYawPitchRoll(estimationFrameYawPitchRoll);
      estimationFrameYawPitchRoll[0] += imuSimulatedDriftYawAngle.getDoubleValue();
      tempOrientationEstimationFrame.setYawPitchRoll(estimationFrameYawPitchRoll);
      
      orientationOutputPort.setData(tempOrientationEstimationFrame);
   }
   
   private void convertAngularVelocityAndSetOnOutputPort()
   {
      Vector3d measuredAngularVelocityVector3d = angularVelocityInputPort.getData();
      TwistCalculator twistCalculator = inverseDynamicsStructureInputPort.getData().getTwistCalculator();

      twistCalculator.packRelativeTwist(tempRelativeTwistOrientationMeasFrameToEstFrame, angularVelocityMeasurementLink, estimationLink);
      tempRelativeTwistOrientationMeasFrameToEstFrame.packAngularPart(relativeAngularVelocity);
      relativeAngularVelocity.changeFrame(estimationFrame);

      tempAngularVelocityMeasurementLink.set(angularVelocityMeasurementFrame, measuredAngularVelocityVector3d); 
      tempAngularVelocityMeasurementLink.changeFrame(estimationFrame);
      relativeAngularVelocity.add(tempAngularVelocityMeasurementLink);

      tempAngularVelocityEstimationLink.setAndChangeFrame(relativeAngularVelocity); // just a copy for clarity
      
      // Introduce simulated IMU drift
      tempAngularVelocityEstimationLink.setZ(tempAngularVelocityEstimationLink.getZ() + imuSimulatedDriftYawVelocity.getDoubleValue());

      angularVelocityOutputPort.setData(tempAngularVelocityEstimationLink);
   }


   public void waitUntilComputationIsDone()
   {
   }

   public ControlFlowOutputPort<FrameOrientation> getOrientationOutputPort()
   {
      return orientationOutputPort;
   }

   public ControlFlowOutputPort<FrameVector> getAngularVelocityOutputPort()
   {
      return angularVelocityOutputPort;
   }

   public ControlFlowOutputPort<FullInverseDynamicsStructure> getInverseDynamicsStructureOutputPort()
   {
      return inverseDynamicsStructureOutputPort;
   }
   
   public YoVariableRegistry getRegistry()
   {
      return registry;
   }

   public void initialize()
   {
      startComputation();
      waitUntilComputationIsDone();
   }

   public void initializeOrientionToActual(FrameOrientation actualOrientation)
   {
      orientationOutputPort.setData(actualOrientation);
   }
}
