package us.ihmc.quadrupedRobotics.factories;

import java.io.IOException;

import javax.vecmath.Point3d;

import us.ihmc.SdfLoader.OutputWriter;
import us.ihmc.SdfLoader.SDFFullQuadrupedRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.SdfLoader.partNames.QuadrupedJointName;
import us.ihmc.commonWalkingControlModules.sensors.footSwitch.FootSwitchInterface;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.humanoidRobotics.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.quadrupedRobotics.communication.QuadrupedGlobalDataProducer;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerManager;
import us.ihmc.quadrupedRobotics.controller.QuadrupedSimulationController;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerManager;
import us.ihmc.quadrupedRobotics.controller.forceDevelopment.QuadrupedForceDevelopmentControllerManager;
import us.ihmc.quadrupedRobotics.model.QuadrupedModelFactory;
import us.ihmc.quadrupedRobotics.model.QuadrupedPhysicalProperties;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.model.QuadrupedStandPrepParameters;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactParameters;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.sensorProcessing.communication.producers.DRCPoseCommunicator;
import us.ihmc.sensorProcessing.sensorData.JointConfigurationGatherer;
import us.ihmc.sensorProcessing.sensorProcessors.SensorTimestampHolder;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;
import us.ihmc.simulationconstructionset.UnreasonableAccelerationException;
import us.ihmc.simulationconstructionset.gui.tools.VisualizerUtils;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.util.LinearGroundContactModel;
import us.ihmc.simulationconstructionset.util.ground.AlternatingSlopesGroundProfile;
import us.ihmc.simulationconstructionset.util.ground.FlatGroundProfile;
import us.ihmc.simulationconstructionset.util.ground.RollingGroundProfile;
import us.ihmc.simulationconstructionset.util.ground.RotatablePlaneTerrainProfile;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation.DRCKinematicsBasedStateEstimator;
import us.ihmc.util.PeriodicNonRealtimeThreadScheduler;
import us.ihmc.util.PeriodicThreadScheduler;

public class QuadrupedSimulationFactory
{
   // Controller
   private SDFFullQuadrupedRobotModel fullRobotModel;
   private QuadrupedPhysicalProperties physicalProperties;
   private QuadrupedControlMode controlMode;
   private GlobalDataProducer globalDataProducer;
   private QuadrantDependentList<FootSwitchInterface> footSwitches;
   private SDFRobot sdfRobot;
   
   // Simulation
   private double controlDT;
   private double gravity;
   private int recordFrequency;
   private boolean useTrackAndDolly;
   private boolean showPlotter;
   private YoGraphicsListRegistry yoGraphicsListRegistry;
   private YoGraphicsListRegistry yoGraphicsListRegistryForDetachedOverhead;
   private QuadrupedModelFactory modelFactory;
   private SimulationConstructionSetParameters scsParameters;
   private QuadrupedGroundContactModelType groundContactModelType;
   private QuadrupedGroundContactParameters groundContactParameters;
   private QuadrupedStandPrepParameters standPrepParameters;
   private RobotController headController;
   private DRCKinematicsBasedStateEstimator stateEstimator;
   private DRCPoseCommunicator poseCommunicator;
   private QuadrupedControllerManager controllerManager;
   private SensorReader sensorReader;
   private OutputWriter outputWriter;
   private boolean useNetworking;
   private IHMCCommunicationKryoNetClassList netClassList;
   private SensorTimestampHolder timestampProvider;
   private PacketCommunicator packetCommunicator;
   
   // TO CONSTRUCT
   private GroundProfile3D groundProfile3D;
   private LinearGroundContactModel groundContactModel;
   private QuadrupedSimulationController simulationController;
   
   // CREATION
   
   private void createPacketCommunicator() throws IOException
   {
      packetCommunicator = PacketCommunicator.createTCPPacketCommunicatorServer(NetworkPorts.CONTROLLER_PORT, netClassList);
      packetCommunicator.connect();
   }
   
   private void createGlobalDataProducer()
   {
      if (useNetworking)
      {
         globalDataProducer = new QuadrupedGlobalDataProducer(packetCommunicator);
      }
   }
   
   private void setupYoRegistries()
   {
      yoGraphicsListRegistry.setYoGraphicsUpdatedRemotely(true);
      yoGraphicsListRegistryForDetachedOverhead = new YoGraphicsListRegistry();
   }

   public void createControllerManager() throws IOException
   {
      
      QuadrupedRuntimeEnvironment runtimeEnvironment = new QuadrupedRuntimeEnvironment(controlDT, sdfRobot.getYoTime(), fullRobotModel, sdfRobot.getRobotsYoVariableRegistry(),
                                                           yoGraphicsListRegistry, yoGraphicsListRegistryForDetachedOverhead, globalDataProducer, footSwitches);
      switch (controlMode)
      {
      case FORCE:
         controllerManager = new QuadrupedForceControllerManager(runtimeEnvironment, physicalProperties);
         break;
      case FORCE_DEV:
         controllerManager = new QuadrupedForceDevelopmentControllerManager(runtimeEnvironment, physicalProperties);
         break;
      case POSITION:
         controllerManager = null;
         break;
      case POSITION_DEV:
         controllerManager = null;
         break;
      default:
         controllerManager = null;
         break;
      }
   }
   
   private void createPoseCommunicator()
   {
      JointConfigurationGatherer jointConfigurationGathererAndProducer = new JointConfigurationGatherer(fullRobotModel);
      PeriodicThreadScheduler scheduler = new PeriodicNonRealtimeThreadScheduler("PoseCommunicator");
      
      if (useNetworking)
      {
         poseCommunicator = new DRCPoseCommunicator(fullRobotModel, jointConfigurationGathererAndProducer, null, globalDataProducer,
                                                    timestampProvider, sensorReader.getSensorRawOutputMapReadOnly(),
                                                    controllerManager.getMotionStatusHolder(), null, scheduler, netClassList);
      }
      else
      {
         poseCommunicator = null;
      }
   }
   
   private void createGroundContactModel()
   {
      switch (groundContactModelType)
      {
      case FLAT:
         groundProfile3D = new FlatGroundProfile(0.0);
         break;
      case ROLLING_HILLS:
      groundProfile3D =  new RollingGroundProfile(0.025, 1.0, 0.0, -20.0, 20.0, -20.0, 20.0);
         break;
      case ROTATABLE:
         groundProfile3D = new RotatablePlaneTerrainProfile(new Point3d(), sdfRobot, yoGraphicsListRegistry, controlDT);
         break;
      case SLOPES:
         double xMin = -5.0, xMax = 40.0;
         double yMin = -5.0, yMax =  5.0;
         double[][] xSlopePairs = new double[][]
         {
            {1.0, 0.0}, {3.0, 0.1}
         };
         groundProfile3D = new AlternatingSlopesGroundProfile(xSlopePairs, xMin, xMax, yMin, yMax);
         break;
      default:
         groundProfile3D = null;
         break;
      }
      
      groundContactModel = new LinearGroundContactModel(sdfRobot, sdfRobot.getRobotsYoVariableRegistry());
      groundContactModel.setZStiffness(groundContactParameters.getZStiffness());
      groundContactModel.setZDamping(groundContactParameters.getZDamping());
      groundContactModel.setXYStiffness(groundContactParameters.getXYStiffness());
      groundContactModel.setXYDamping(groundContactParameters.getXYDamping());
      groundContactModel.setGroundProfile3D(groundProfile3D);
   }
   
   private void createSimulationController()
   {
      simulationController = new QuadrupedSimulationController(sdfRobot, sensorReader, outputWriter, controllerManager,
                                                               stateEstimator, poseCommunicator, headController);
   }
   
   private void setupSDFRobot()
   {
      sdfRobot.setController(simulationController);
      sdfRobot.setPositionInWorld(standPrepParameters.getInitialBodyPosition());
      for (QuadrupedJointName quadrupedJointName : modelFactory.getQuadrupedJointNames())
      {
         OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint = sdfRobot.getOneDegreeOfFreedomJoint(modelFactory.getSDFNameForJointName(quadrupedJointName));
         oneDegreeOfFreedomJoint.setQ(standPrepParameters.getInitialJointPosition(quadrupedJointName));
      }
      try
      {
         sdfRobot.update();
         sdfRobot.doDynamicsButDoNotIntegrate();
         sdfRobot.update();
      }
      catch (UnreasonableAccelerationException unreasonableAccelerationException)
      {
         throw new RuntimeException("UnreasonableAccelerationException");
      }
      double totalMass = sdfRobot.computeCenterOfMass(standPrepParameters.getInitialCOMPosition());
      sdfRobot.setGravity(gravity);
      sdfRobot.setGroundContactModel(groundContactModel);
      System.out.println("Total mass: " + totalMass);
   }
   
   public SimulationConstructionSet createSimulation() throws IOException
   {
//      createGlobalDataProducer();
      setupYoRegistries();
      createControllerManager();
      createPoseCommunicator();
      createGroundContactModel();
      createSimulationController();
      setupSDFRobot();
      
      SimulationConstructionSet scs = new SimulationConstructionSet(sdfRobot, scsParameters);
      if (groundContactModelType == QuadrupedGroundContactModelType.ROTATABLE)
      {
         scs.setGroundVisible(false);
      }
      scs.addYoGraphicsListRegistry(yoGraphicsListRegistry);
      VisualizerUtils.createOverheadPlotter(scs, false, "centerOfMass", yoGraphicsListRegistry);
      VisualizerUtils.createOverheadPlotterInSeparateWindow(scs, false, "centerOfMass", yoGraphicsListRegistryForDetachedOverhead);
      scs.setDT(controlDT, recordFrequency);
      if (scs.getSimulationConstructionSetParameters().getCreateGUI())
      {
         scs.setCameraTrackingVars("q_x", "q_y", "q_z");
         scs.setCameraDollyVars("q_x", "q_y", "q_z");
         scs.setCameraTracking(useTrackAndDolly, useTrackAndDolly, useTrackAndDolly, useTrackAndDolly);
         scs.setCameraDolly(useTrackAndDolly, useTrackAndDolly, useTrackAndDolly, false);
         scs.setCameraDollyOffsets(4.0, 4.0, 1.0);
         if (showPlotter)
         {
            scs.getStandardSimulationGUI().selectPanel("Plotter");
         }
      }
      return scs;
   }
   
   // OPTIONS
   
   public void setControlDT(double controlDT)
   {
      this.controlDT = controlDT;
   }
   
   public void setGravity(double gravity)
   {
      this.gravity = gravity;
   }
   
   public void setRecordFrequency(int recordFrequency)
   {
      this.recordFrequency = recordFrequency;
   }
   
   public void setUseTrackAndDolly(boolean useTrackAndDolly)
   {
      this.useTrackAndDolly = useTrackAndDolly;
   }
   
   public void setShowPlotter(boolean showPlotter)
   {
      this.showPlotter = showPlotter;
   }
   
   public void setModelFactory(QuadrupedModelFactory modelFactory)
   {
      this.modelFactory = modelFactory;
   }
   
   public void setSCSParameters(SimulationConstructionSetParameters scsParameters)
   {
      this.scsParameters = scsParameters;
   }
   
   public void setGroundContactModelType(QuadrupedGroundContactModelType groundContactModelType)
   {
      this.groundContactModelType = groundContactModelType;
   }
   
   public void setGroundContactParameters(QuadrupedGroundContactParameters groundContactParameters)
   {
      this.groundContactParameters = groundContactParameters;
   }
   
   public void setHeadController(RobotController headController)
   {
      this.headController = headController;
   }
   
   public void setOutputWriter(OutputWriter outputWriter)
   {
      this.outputWriter = outputWriter;
   }
   
   public void setStandPrepParameters(QuadrupedStandPrepParameters standPrepParameters)
   {
      this.standPrepParameters = standPrepParameters;
   }
   
   public void setPhysicalProperties(QuadrupedPhysicalProperties physicalProperties)
   {
      this.physicalProperties = physicalProperties;
   }
   
   public void setControlMode(QuadrupedControlMode controlMode)
   {
      this.controlMode = controlMode;
   }
   
   public void setFullRobotModel(SDFFullQuadrupedRobotModel fullRobotModel)
   {
      this.fullRobotModel = fullRobotModel;
   }
   
   public void setGlobalDataProducer(GlobalDataProducer globalDataProducer)
   {
      this.globalDataProducer = globalDataProducer;
   }

   public void setFootSwitches(QuadrantDependentList<FootSwitchInterface> footSwitches)
   {
      this.footSwitches = footSwitches;
   }
   
   public void setYoGraphicsListRegistry(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.yoGraphicsListRegistry = yoGraphicsListRegistry;
   }
   
   public void setStateEstimator(DRCKinematicsBasedStateEstimator stateEstimator)
   {
      this.stateEstimator = stateEstimator;
   }
   
   public void setUseNetworking(boolean useNetworking)
   {
      this.useNetworking = useNetworking;
   }
   
   public void setNetClassList(IHMCCommunicationKryoNetClassList netClassList)
   {
      this.netClassList = netClassList;
   }
   
   public void setTimestampHolder(SensorTimestampHolder timestampProvider)
   {
      this.timestampProvider = timestampProvider;
   }

   public void setSDFRobot(SDFRobot sdfRobot)
   {
      this.sdfRobot = sdfRobot;
   }
   
   public void setSensorReader(SensorReader sensorReader)
   {
      this.sensorReader = sensorReader;
   }
}
