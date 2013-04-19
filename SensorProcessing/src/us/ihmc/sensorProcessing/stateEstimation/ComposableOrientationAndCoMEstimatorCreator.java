package us.ihmc.sensorProcessing.stateEstimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFramePointControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFrameQuaternionControlFlowOutputPort;
import us.ihmc.sensorProcessing.controlFlowPorts.YoFrameVectorControlFlowOutputPort;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.AngularVelocityMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.LinearAccelerationMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.OrientationMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.PointVelocityMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.AngularAccelerationProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.AngularVelocityProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.BiasProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.CenterOfMassAccelerationProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.CenterOfMassPositionProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.CenterOfMassVelocityProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.OrientationProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.ProcessModelElement;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.AngularVelocitySensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.LinearAccelerationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.OrientationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocityDataObject;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocitySensorConfiguration;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SixDoFJoint;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class ComposableOrientationAndCoMEstimatorCreator
{
   private static final int VECTOR3D_LENGTH = 3;

   private final RigidBody orientationEstimationLink;
   
   private final ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort;

   private final DenseMatrix64F angularAccelerationNoiseCovariance;
   private final DenseMatrix64F comAccelerationNoiseCovariance;

   private final List<OrientationSensorConfiguration> orientationSensorConfigurations = new ArrayList<OrientationSensorConfiguration>();
   private final List<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations = new ArrayList<AngularVelocitySensorConfiguration>();
   private final List<LinearAccelerationSensorConfiguration> linearAccelerationSensorConfigurations = new ArrayList<LinearAccelerationSensorConfiguration>();
   private final List<PointVelocitySensorConfiguration> pointVelocitySensorConfigurations = new ArrayList<PointVelocitySensorConfiguration>();

   public ComposableOrientationAndCoMEstimatorCreator(DenseMatrix64F angularAccelerationNoiseCovariance, DenseMatrix64F comAccelerationNoiseCovariance,
         RigidBody orientationEstimationLink, ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort)
   {
      this.angularAccelerationNoiseCovariance = angularAccelerationNoiseCovariance;
      this.comAccelerationNoiseCovariance = comAccelerationNoiseCovariance;
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

   public void addOrientationSensorConfiguration(OrientationSensorConfiguration orientationSensorConfiguration)
   {
      orientationSensorConfigurations.add(orientationSensorConfiguration);
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

   public void addLinearAccelerationSensorConfigurations(Collection<LinearAccelerationSensorConfiguration> linearAccelerationSensorConfigurations)
   {
      for (LinearAccelerationSensorConfiguration linearAccelerationSensorConfiguration : linearAccelerationSensorConfigurations)
      {
         this.addLinearAccelerationSensorConfiguration(linearAccelerationSensorConfiguration);
      }
   }

   public void addLinearAccelerationSensorConfiguration(LinearAccelerationSensorConfiguration linearAccelerationSensorConfiguration)
   {
      this.linearAccelerationSensorConfigurations.add(linearAccelerationSensorConfiguration);
   }

   public void addPointVelocitySensorConfigurations(Collection<PointVelocitySensorConfiguration> pointVelocitySensorConfigurations)
   {
      for (PointVelocitySensorConfiguration pointVelocitySensorConfiguration : pointVelocitySensorConfigurations)
      {
         this.addPointVelocitySensorConfiguration(pointVelocitySensorConfiguration);
      }
   }
   
   public void addPointVelocitySensorConfiguration(PointVelocitySensorConfiguration pointVelocitySensorConfiguration)
   {
      this.pointVelocitySensorConfigurations.add(pointVelocitySensorConfiguration);      
   }
   
   public StateEstimatorWithPorts createOrientationEstimator(ControlFlowGraph controlFlowGraph, double controlDT, SixDoFJoint rootJoint, RigidBody estimationLink,
         ReferenceFrame estimationFrame, YoVariableRegistry registry)
   {
      return new ComposableOrientationAndCoMEstimator("orientationEstimator", controlDT, rootJoint, estimationLink, 
            estimationFrame, controlFlowGraph,
            inverseDynamicsStructureOutputPort,
            registry);
   }

   private class ComposableOrientationAndCoMEstimator extends ComposableStateEstimator implements StateEstimatorWithPorts
   {
      private final ControlFlowGraph controlFlowGraph;
      
      private final ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort;
      private final ControlFlowInputPort<FrameVector> desiredCenterOfMassAccelerationInputPort;
      private final ControlFlowInputPort<FrameVector> desiredAngularAccelerationInputPort;
      
      private final ControlFlowOutputPort<FrameOrientation> orientationStatePort;
      private final ControlFlowOutputPort<FrameVector> angularVelocityStatePort;
      private final ControlFlowOutputPort<FramePoint> centerOfMassPositionStatePort;
      private final ControlFlowOutputPort<FrameVector> centerOfMassVelocityStatePort;
      private final ControlFlowOutputPort<FrameVector> centerOfMassAccelerationStatePort;
      private final ControlFlowOutputPort<FrameVector> angularAccelerationStatePort;
      private final ControlFlowOutputPort<FullInverseDynamicsStructure> updatedInverseDynamicsStructureOutputPort;

      private final CenterOfMassBasedFullRobotModelUpdater centerOfMassBasedFullRobotModelUpdater;
      
      public ComposableOrientationAndCoMEstimator(String name, double controlDT, SixDoFJoint rootJoint, RigidBody estimationLink,
            ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph, 
            ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort,
            YoVariableRegistry parentRegistry)
      {
         super(name, controlDT, parentRegistry);
         
         this.controlFlowGraph = controlFlowGraph;
         this.inverseDynamicsStructureInputPort = createInputPort();
         controlFlowGraph.connectElements(inverseDynamicsStructureOutputPort, inverseDynamicsStructureInputPort);
         
         inverseDynamicsStructureInputPort.setData(inverseDynamicsStructureOutputPort.getData());

         orientationStatePort = new YoFrameQuaternionControlFlowOutputPort(this, name, ReferenceFrame.getWorldFrame(), parentRegistry);
         angularVelocityStatePort = new YoFrameVectorControlFlowOutputPort(this, name + "Omega", estimationFrame, registry);
         angularAccelerationStatePort = new YoFrameVectorControlFlowOutputPort(this, name + "AngularAcceleration", estimationFrame, registry);
         centerOfMassPositionStatePort = new YoFramePointControlFlowOutputPort(this, name + "CoMPosition", ReferenceFrame.getWorldFrame(), registry);
         centerOfMassVelocityStatePort = new YoFrameVectorControlFlowOutputPort(this, name + "CoMVelocity", ReferenceFrame.getWorldFrame(), registry);
         centerOfMassAccelerationStatePort = new YoFrameVectorControlFlowOutputPort(this, name + "CoMAcceleration", ReferenceFrame.getWorldFrame(), registry);
         
         this.updatedInverseDynamicsStructureOutputPort = createOutputPort();
         
         desiredAngularAccelerationInputPort = createProcessInputPort(VECTOR3D_LENGTH);
         desiredCenterOfMassAccelerationInputPort = createProcessInputPort(VECTOR3D_LENGTH);

         addOrientationProcessModelElement();
         addAngularVelocityProcessModelElement(estimationFrame, desiredAngularAccelerationInputPort);
         addAngularAccelerationProcessModelElement(estimationFrame, desiredAngularAccelerationInputPort);

         addCoMPositionProcessModelElement();
         addCoMVelocityProcessModelElement(desiredCenterOfMassAccelerationInputPort);
         addCoMAccelerationProcessModelElement(desiredCenterOfMassAccelerationInputPort);

         for (OrientationSensorConfiguration orientationSensorConfiguration : orientationSensorConfigurations)
         {
            addOrientationSensor(estimationFrame, controlFlowGraph, orientationSensorConfiguration);
         }

         for (AngularVelocitySensorConfiguration angularVelocitySensorConfiguration : angularVelocitySensorConfigurations)
         {
            addAngularVelocitySensor(estimationFrame, controlFlowGraph, angularVelocitySensorConfiguration);
         }

         for (LinearAccelerationSensorConfiguration linearAccelerationSensorConfiguration : linearAccelerationSensorConfigurations)
         {
            addLinearAccelerationSensor(estimationFrame, controlFlowGraph, linearAccelerationSensorConfiguration);
         }
         
         for (PointVelocitySensorConfiguration pointVelocitySensorConfiguration : pointVelocitySensorConfigurations)
         {
            addPointVelocitySensor(estimationFrame, controlFlowGraph, pointVelocitySensorConfiguration);
         }

         this.centerOfMassBasedFullRobotModelUpdater = new CenterOfMassBasedFullRobotModelUpdater(
               inverseDynamicsStructureInputPort, 
               centerOfMassPositionStatePort, centerOfMassVelocityStatePort, centerOfMassAccelerationStatePort,
               orientationStatePort, angularVelocityStatePort, angularAccelerationStatePort);

         Runnable runnable = new Runnable(){

            public void run()
            {
               //TODO: Less magic at a distance communication here.
               centerOfMassBasedFullRobotModelUpdater.run();
               updatedInverseDynamicsStructureOutputPort.setData(inverseDynamicsStructureInputPort.getData());
            }};

            addPostStateChangeRunnable(runnable);
         
         initialize();
      }
      
      private void addOrientationProcessModelElement()
      {
         ProcessModelElement processModelElement = new OrientationProcessModelElement(angularVelocityStatePort, orientationStatePort, "orientation", registry);
         addProcessModelElement(orientationStatePort, processModelElement);
      }

      private void addAngularVelocityProcessModelElement(ReferenceFrame estimationFrame, ControlFlowInputPort<FrameVector> angularAccelerationInputPort)
      {
         AngularVelocityProcessModelElement processModelElement = new AngularVelocityProcessModelElement(estimationFrame, angularVelocityStatePort,
               angularAccelerationInputPort, "angularVelocity", registry);

         processModelElement.setProcessNoiseCovarianceBlock(angularAccelerationNoiseCovariance);
         addProcessModelElement(angularVelocityStatePort, processModelElement);
      }

      private void addAngularAccelerationProcessModelElement(ReferenceFrame estimationFrame, ControlFlowInputPort<FrameVector> angularAccelerationInputPort)
      {
         AngularAccelerationProcessModelElement processModelElement = new AngularAccelerationProcessModelElement("angularAcceleration", estimationFrame,
               registry, angularAccelerationStatePort, angularAccelerationInputPort);
         processModelElement.setProcessNoiseCovarianceBlock(angularAccelerationNoiseCovariance);
         addProcessModelElement(angularAccelerationStatePort, processModelElement);
      }

      private void addCoMPositionProcessModelElement()
      {
         CenterOfMassPositionProcessModelElement processModelElement = new CenterOfMassPositionProcessModelElement(centerOfMassPositionStatePort,
               centerOfMassVelocityStatePort, "CoMPosition", registry);
         addProcessModelElement(centerOfMassPositionStatePort, processModelElement);
      }

      private void addCoMVelocityProcessModelElement(ControlFlowInputPort<FrameVector> centerOfMassAccelerationInputPort)
      {
         CenterOfMassVelocityProcessModelElement processModelElement = new CenterOfMassVelocityProcessModelElement(centerOfMassVelocityStatePort,
               centerOfMassAccelerationInputPort, "CoMVelocity", registry);
         processModelElement.setProcessNoiseCovarianceBlock(comAccelerationNoiseCovariance);
         addProcessModelElement(centerOfMassVelocityStatePort, processModelElement);
      }

      private void addCoMAccelerationProcessModelElement(ControlFlowInputPort<FrameVector> centerOfMassAccelerationInputPort)
      {
         CenterOfMassAccelerationProcessModelElement processModelElement = new CenterOfMassAccelerationProcessModelElement("CoMAcceleration", registry,
               centerOfMassAccelerationStatePort, centerOfMassAccelerationInputPort);
         processModelElement.setProcessNoiseCovarianceBlock(comAccelerationNoiseCovariance);
         addProcessModelElement(centerOfMassAccelerationStatePort, processModelElement);
      }

      private void addOrientationSensor(ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph,
            OrientationSensorConfiguration orientationSensorConfiguration)
      {
         ReferenceFrame measurementFrame = orientationSensorConfiguration.getMeasurementFrame();
         ControlFlowInputPort<Matrix3d> orientationMeasurementPort = createMeasurementInputPort(VECTOR3D_LENGTH);
         String name = orientationSensorConfiguration.getName();
         DenseMatrix64F orientationNoiseCovariance = orientationSensorConfiguration.getOrientationNoiseCovariance();

         OrientationMeasurementModelElement orientationMeasurementModel = new OrientationMeasurementModelElement(orientationStatePort,
               orientationMeasurementPort, estimationFrame, measurementFrame, name, registry);
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
         ControlFlowInputPort<Vector3d> angularVelocityMeasurementPort = createMeasurementInputPort(VECTOR3D_LENGTH);

         ControlFlowOutputPort<FrameVector> biasPort = new YoFrameVectorControlFlowOutputPort(this, biasName, measurementFrame, registry);
         BiasProcessModelElement biasProcessModelElement = new BiasProcessModelElement(biasPort, measurementFrame, biasName, registry);
         DenseMatrix64F biasProcessNoiseCovariance = angularVelocitySensorConfiguration.getBiasProcessNoiseCovariance();
         biasProcessModelElement.setProcessNoiseCovarianceBlock(biasProcessNoiseCovariance);
         addProcessModelElement(biasPort, biasProcessModelElement);
         String name = angularVelocitySensorConfiguration.getName();
         DenseMatrix64F angularVelocityNoiseCovariance = angularVelocitySensorConfiguration.getAngularVelocityNoiseCovariance();

         AngularVelocityMeasurementModelElement angularVelocityMeasurementModel = new AngularVelocityMeasurementModelElement(angularVelocityStatePort,
               biasPort, angularVelocityMeasurementPort, orientationEstimationLink, estimationFrame, measurementLink, measurementFrame, inverseDynamicsStructureInputPort, name,
               registry);
         angularVelocityMeasurementModel.setNoiseCovariance(angularVelocityNoiseCovariance);

         addMeasurementModelElement(angularVelocityMeasurementModel);
         controlFlowGraph.connectElements(angularVelocitySensorConfiguration.getOutputPort(), angularVelocityMeasurementPort);
      }

      private void addLinearAccelerationSensor(ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph,
            LinearAccelerationSensorConfiguration linearAccelerationSensorConfiguration)
      {
         String biasName = linearAccelerationSensorConfiguration.getName() + "BiasEstimate";
         ReferenceFrame measurementFrame = linearAccelerationSensorConfiguration.getMeasurementFrame();
         RigidBody measurementLink = linearAccelerationSensorConfiguration.getLinearAccelerationMeasurementLink();
         ControlFlowInputPort<Vector3d> linearAccelerationMeasurementInputPort = createMeasurementInputPort(VECTOR3D_LENGTH);

         ControlFlowOutputPort<FrameVector> biasPort = new YoFrameVectorControlFlowOutputPort(this, biasName, measurementFrame, registry);

         BiasProcessModelElement biasProcessModelElement = new BiasProcessModelElement(biasPort, measurementFrame, biasName, registry);
         DenseMatrix64F biasProcessNoiseCovariance = linearAccelerationSensorConfiguration.getBiasProcessNoiseCovariance();
         biasProcessModelElement.setProcessNoiseCovarianceBlock(biasProcessNoiseCovariance);
         addProcessModelElement(biasPort, biasProcessModelElement);
         String name = linearAccelerationSensorConfiguration.getName();

         DenseMatrix64F linearAccelerationNoiseCovariance = linearAccelerationSensorConfiguration.getLinearAccelerationNoiseCovariance();

         double gZ = linearAccelerationSensorConfiguration.getGravityZ();

         LinearAccelerationMeasurementModelElement linearAccelerationMeasurementModel = new LinearAccelerationMeasurementModelElement(name, registry,
               centerOfMassPositionStatePort, centerOfMassVelocityStatePort, centerOfMassAccelerationStatePort, orientationStatePort, angularVelocityStatePort,
               angularAccelerationStatePort, biasPort, linearAccelerationMeasurementInputPort, 
               inverseDynamicsStructureInputPort, measurementLink,
               measurementFrame, orientationEstimationLink, estimationFrame, gZ);

         linearAccelerationMeasurementModel.setNoiseCovariance(linearAccelerationNoiseCovariance);

         addMeasurementModelElement(linearAccelerationMeasurementModel);
         controlFlowGraph.connectElements(linearAccelerationSensorConfiguration.getOutputPort(), linearAccelerationMeasurementInputPort);
      }
      
      private void addPointVelocitySensor(ReferenceFrame estimationFrame, ControlFlowGraph controlFlowGraph,
            PointVelocitySensorConfiguration pointVelocitySensorConfiguration)
      {
         RigidBody measurementLink = pointVelocitySensorConfiguration.getPointVelocityMeasurementLink();
         FramePoint stationaryPoint = pointVelocitySensorConfiguration.getPointVelocityMeasurementPoint();

         ControlFlowInputPort<PointVelocityDataObject> pointVelocityMeasurementInputPort = createInputPort();

         String name = pointVelocitySensorConfiguration.getName();

         DenseMatrix64F pointVelocityNoiseCovariance = pointVelocitySensorConfiguration.getPointVelocityNoiseCovariance();


         PointVelocityMeasurementModelElement pointVelocityMeasurementModelElement = new PointVelocityMeasurementModelElement(
               name, pointVelocityMeasurementInputPort, 
               centerOfMassPositionStatePort, centerOfMassVelocityStatePort, orientationStatePort, 
               angularVelocityStatePort, estimationFrame, 
               measurementLink, stationaryPoint, inverseDynamicsStructureInputPort, registry);

         pointVelocityMeasurementModelElement.setNoiseCovariance(pointVelocityNoiseCovariance);

         addMeasurementModelElement(pointVelocityMeasurementModelElement);
         controlFlowGraph.connectElements(pointVelocitySensorConfiguration.getOutputPort(), pointVelocityMeasurementInputPort);
      }

      public FrameOrientation getEstimatedOrientation()
      {
         return orientationStatePort.getData();
      }

      public FrameVector getEstimatedAngularVelocity()
      {
         return angularVelocityStatePort.getData();
      }

      public FramePoint getEstimatedCoMPosition()
      {
         return centerOfMassPositionStatePort.getData();
      }
      
      public FrameVector getEstimatedCoMVelocity()
      {
         return centerOfMassVelocityStatePort.getData();
      }

      public void setEstimatedOrientation(FrameOrientation orientation)
      {
         orientationStatePort.setData(orientation);
         centerOfMassBasedFullRobotModelUpdater.run();
         FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
         inverseDynamicsStructure.updateInternalState();
         updatedInverseDynamicsStructureOutputPort.setData(inverseDynamicsStructure);
      }

      public void setEstimatedAngularVelocity(FrameVector angularVelocity)
      {
         angularVelocityStatePort.setData(angularVelocity);
         centerOfMassBasedFullRobotModelUpdater.run();
         FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
         inverseDynamicsStructure.updateInternalState();
         updatedInverseDynamicsStructureOutputPort.setData(inverseDynamicsStructure);
      }
      
      public void setEstimatedCoMPosition(FramePoint estimatedCoMPosition)
      {
         centerOfMassPositionStatePort.setData(estimatedCoMPosition);
         centerOfMassBasedFullRobotModelUpdater.run();
         FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
         inverseDynamicsStructure.updateInternalState();
         updatedInverseDynamicsStructureOutputPort.setData(inverseDynamicsStructure);
      }
      
      public void setEstimatedCoMVelocity(FrameVector estimatedCoMVelocity)
      {
         centerOfMassVelocityStatePort.setData(estimatedCoMVelocity);
         centerOfMassBasedFullRobotModelUpdater.run();
         FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
         inverseDynamicsStructure.updateInternalState();
         updatedInverseDynamicsStructureOutputPort.setData(inverseDynamicsStructure);
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
         return desiredCenterOfMassAccelerationInputPort;
      }

      public ControlFlowGraph getControlFlowGraph()
      {
         return controlFlowGraph;
      }

   }
}
