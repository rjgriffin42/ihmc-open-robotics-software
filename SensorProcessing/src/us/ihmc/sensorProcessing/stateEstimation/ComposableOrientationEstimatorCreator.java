package us.ihmc.sensorProcessing.stateEstimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFrameQuaternionControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFrameVectorControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.AngularVelocityMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.OrientationMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.AngularVelocityProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.BiasProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.OrientationProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.ProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.AngularVelocitySensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.OrientationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointPositionDataObject;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocityDataObject;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class ComposableOrientationEstimatorCreator
{
   private static final int VECTOR3D_LENGTH = 3;

   private final RigidBody orientationEstimationLink;
   private final ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort;

   private final DenseMatrix64F angularAccelerationNoiseCovariance;

   private final List<OrientationSensorConfiguration> orientationSensorConfigurations = new ArrayList<OrientationSensorConfiguration>();
   private final List<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations = new ArrayList<AngularVelocitySensorConfiguration>();

   public ComposableOrientationEstimatorCreator(DenseMatrix64F angularAccelerationNoiseCovariance, RigidBody orientationEstimationLink,
           ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort)
   {
      this.angularAccelerationNoiseCovariance = angularAccelerationNoiseCovariance;
      this.orientationEstimationLink = orientationEstimationLink;
      this.inverseDynamicsStructureOutputPort = inverseDynamicsStructureOutputPort;
   }

   public void addOrientationSensorConfigurations(Collection<OrientationSensorConfiguration> orientationSensorConfigurations)
   {
      for (OrientationSensorConfiguration orientationSensorConfiguration : orientationSensorConfigurations)
      {
         this.addOrientationSensorConfiguration(orientationSensorConfiguration);
      }
   }

   public void addAngularVelocitySensorConfigurations(Collection<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations)
   {
      for (AngularVelocitySensorConfiguration angularVelocitySensorConfiguration : angularVelocitySensorConfigurations)
      {
         addAngularVelocitySensorConfiguration(angularVelocitySensorConfiguration);
      }
   }

   public void addAngularVelocitySensorConfiguration(AngularVelocitySensorConfiguration angularVelocitySensorConfiguration)
   {
      this.angularVelocitySensorConfigurations.add(angularVelocitySensorConfiguration);
   }

   public void addOrientationSensorConfiguration(OrientationSensorConfiguration orientationSensorConfiguration)
   {
      orientationSensorConfigurations.add(orientationSensorConfiguration);
   }

   public StateEstimatorWithPorts createOrientationEstimator(ControlFlowGraph controlFlowGraph, double controlDT, ReferenceFrame estimationFrame,
           YoVariableRegistry registry)
   {
      return new ComposableOrientationEstimator("orientationEstimator", controlDT, estimationFrame, controlFlowGraph, registry);
   }

   private class ComposableOrientationEstimator extends ComposableStateEstimator implements StateEstimatorWithPorts
   {
      private final ControlFlowGraph controlFlowGraph;
      
      private final ControlFlowOutputPort<FrameOrientation> orientationStatePort;
      private final ControlFlowOutputPort<FrameVector> angularVelocityStatePort;

      private final ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort;
      private final ControlFlowInputPort<FrameVector> desiredAngularAccelerationInputPort;

      public ComposableOrientationEstimator(String name, double controlDT, ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph,
              YoVariableRegistry parentRegistry)
      {
         super(name, controlDT, parentRegistry);

         this.controlFlowGraph = controlFlowGraph;
         
         orientationStatePort = new YoFrameQuaternionControlFlowOutputPort(this, name, ReferenceFrame.getWorldFrame(), parentRegistry);
         angularVelocityStatePort = new YoFrameVectorControlFlowOutputPort(this, name + "Omega", estimationFrame, registry);

         this.inverseDynamicsStructureInputPort = createInputPort("inverseDynamicsStructureInputPort");
         this.desiredAngularAccelerationInputPort = createInputPort("desiredAngularAccelerationInputPort");

         addOrientationProcessModelElement();
         addAngularVelocityProcessModelElement(estimationFrame, controlFlowGraph);

         for (OrientationSensorConfiguration orientationSensorConfiguration : orientationSensorConfigurations)
         {
            addOrientationSensor(estimationFrame, controlFlowGraph, orientationSensorConfiguration);
         }

         for (AngularVelocitySensorConfiguration angularVelocitySensorConfiguration : angularVelocitySensorConfigurations)
         {
            addAngularVelocitySensor(estimationFrame, controlFlowGraph, angularVelocitySensorConfiguration);
         }

         controlFlowGraph.connectElements(inverseDynamicsStructureOutputPort, inverseDynamicsStructureInputPort);

         initialize();
      }

      private void addOrientationProcessModelElement()
      {
         ProcessModelElement orientationProcessModelElement = new OrientationProcessModelElement(angularVelocityStatePort, orientationStatePort, "orientation", registry);
         addProcessModelElement(orientationStatePort, orientationProcessModelElement);
      }

      private void addAngularVelocityProcessModelElement(ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph)
      {
         AngularVelocityProcessModelElement angularVelocityProcessModelElement = new AngularVelocityProcessModelElement(estimationFrame, angularVelocityStatePort,
               desiredAngularAccelerationInputPort, "angularVelocity", registry);

         angularVelocityProcessModelElement.setProcessNoiseCovarianceBlock(angularAccelerationNoiseCovariance);
         addProcessModelElement(angularVelocityStatePort, angularVelocityProcessModelElement);
      }

      private void addOrientationSensor(ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph,
                                        OrientationSensorConfiguration orientationSensorConfiguration)
      {
         ReferenceFrame measurementFrame = orientationSensorConfiguration.getMeasurementFrame();
         ControlFlowInputPort<Matrix3d> measurementInputPort = createInputPort("orientationMeasurementInputPort");

         ControlFlowInputPort<Matrix3d> orientationMeasurementPort = measurementInputPort;
         String name = orientationSensorConfiguration.getName();
         DenseMatrix64F orientationNoiseCovariance = orientationSensorConfiguration.getOrientationNoiseCovariance();

         OrientationMeasurementModelElement orientationMeasurementModel = new OrientationMeasurementModelElement(orientationStatePort, orientationMeasurementPort,
                                                                             estimationFrame, measurementFrame, name, registry);
         orientationMeasurementModel.setNoiseCovariance(orientationNoiseCovariance);

         addMeasurementModelElement(orientationMeasurementModel);
         controlFlowGraph.connectElements(orientationSensorConfiguration.getOutputPort(), orientationMeasurementPort);
      }

      private void addAngularVelocitySensor(ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph,
              AngularVelocitySensorConfiguration angularVelocitySensorConfiguration)
      {
         String biasName = angularVelocitySensorConfiguration.getName() + "BiasEstimate";
         ReferenceFrame measurementFrame = angularVelocitySensorConfiguration.getMeasurementFrame();
         RigidBody measurementLink = angularVelocitySensorConfiguration.getAngularVelocityMeasurementLink();
         ControlFlowInputPort<Vector3d> measurementInputPort = createInputPort("angularVelocityMeasurementInputPort");

         ControlFlowInputPort<Vector3d> angularVelocityMeasurementPort = measurementInputPort;

         ControlFlowOutputPort<FrameVector> biasPort = new YoFrameVectorControlFlowOutputPort(this, biasName, measurementFrame, registry);
         BiasProcessModelElement biasProcessModelElement = new BiasProcessModelElement(biasPort, measurementFrame, biasName, registry);
         DenseMatrix64F biasProcessNoiseCovariance = angularVelocitySensorConfiguration.getBiasProcessNoiseCovariance();
         biasProcessModelElement.setProcessNoiseCovarianceBlock(biasProcessNoiseCovariance);
         addProcessModelElement(biasPort, biasProcessModelElement);
         String name = angularVelocitySensorConfiguration.getName();
         DenseMatrix64F angularVelocityNoiseCovariance = angularVelocitySensorConfiguration.getAngularVelocityNoiseCovariance();

         AngularVelocityMeasurementModelElement angularVelocityMeasurementModel = new AngularVelocityMeasurementModelElement(angularVelocityStatePort, biasPort,
                                                                                     angularVelocityMeasurementPort, orientationEstimationLink,
                                                                                     estimationFrame, measurementLink, measurementFrame,
                                                                                     inverseDynamicsStructureInputPort, name, registry);
         angularVelocityMeasurementModel.setNoiseCovariance(angularVelocityNoiseCovariance);

         addMeasurementModelElement(angularVelocityMeasurementModel);
         controlFlowGraph.connectElements(angularVelocitySensorConfiguration.getOutputPort(), angularVelocityMeasurementPort);
      }

      public void getEstimatedOrientation(FrameOrientation estimatedOrientationToPack)
      {
         estimatedOrientationToPack.setAndChangeFrame(orientationStatePort.getData());
      }

      public void getEstimatedAngularVelocity(FrameVector estimatedAngularVelocityToPack)
      {
         estimatedAngularVelocityToPack.setAndChangeFrame(angularVelocityStatePort.getData());
      }

      public void getEstimatedCoMPosition(FramePoint estimatedCoMPositionToPack)
      {
         // Do nothing.
      }
      
      public void getEstimatedCoMVelocity(FrameVector estimatedCoMVelocityToPack)
      {
         // Do nothing.
      }

      public void setEstimatedOrientation(FrameOrientation orientation)
      {
         orientationStatePort.setData(orientation);
      }

      public void setEstimatedAngularVelocity(FrameVector angularVelocity)
      {
         angularVelocityStatePort.setData(angularVelocity);
      }

      public void setEstimatedCoMPosition(FramePoint estimatedCoMPosition)
      {
         // do nothing
      }

      public void setEstimatedCoMVelocity(FrameVector estimatedCoMVelocity)
      {
         // do nothing
      }

      public DenseMatrix64F getCovariance()
      {
         return kalmanFilter.getCovariance();
      }

      public DenseMatrix64F getState()
      {
         return kalmanFilter.getState();
      }

      public void setState(DenseMatrix64F x, DenseMatrix64F covariance)
      {
         kalmanFilter.setState(x, covariance);
      }

      public ControlFlowInputPort<FrameVector> getDesiredAngularAccelerationInputPort()
      {
         return desiredAngularAccelerationInputPort;
      }

      public ControlFlowInputPort<FrameVector> getDesiredCenterOfMassAccelerationInputPort()
      {
         return null;
      }

      public ControlFlowInputPort<Set<PointPositionDataObject>> getPointPositionInputPort()
      {
         return null;
      }
      
      public ControlFlowInputPort<Set<PointVelocityDataObject>> getPointVelocityInputPort()
      {
         return null;
      }

      public ControlFlowGraph getControlFlowGraph()
      {
         return controlFlowGraph;
      }

   }
}
