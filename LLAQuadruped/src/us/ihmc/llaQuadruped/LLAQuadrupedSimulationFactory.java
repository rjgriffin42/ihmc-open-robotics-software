package us.ihmc.llaQuadruped;

import java.io.IOException;

import us.ihmc.SdfLoader.SDFFullQuadrupedRobotModel;
import us.ihmc.SdfLoader.SDFPerfectSimulatedOutputWriter;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.llaQuadruped.simulation.LLAQuadrupedGroundContactParameters;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerManager;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.estimator.sensorProcessing.simulatedSensors.SDFQuadrupedPerfectSimulatedSensor;
import us.ihmc.quadrupedRobotics.factories.QuadrupedControllerManagerFactory;
import us.ihmc.quadrupedRobotics.factories.QuadrupedSimulationFactory;
import us.ihmc.quadrupedRobotics.params.ParameterRegistry;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation.DRCKinematicsBasedStateEstimator;

public class LLAQuadrupedSimulationFactory
{
   private static final QuadrupedControlMode CONTROL_MODE = QuadrupedControlMode.FORCE;
   private final QuadrupedGroundContactModelType groundContactModelType = QuadrupedGroundContactModelType.FLAT;
   private static final double SIMULATION_DT = 0.00006;
   private static final double SIMULATION_GRAVITY = -9.81;
   private static final int RECORD_FREQUENCY = (int) (0.01 / SIMULATION_DT);
   
   // Parameters
   private LLAQuadrupedModelFactory modelFactory;
   private LLAQuadrupedPhysicalProperties physicalProperties;
   private LLAQuadrupedStandPrepParameters standPrepParameters;
   private LLAQuadrupedGroundContactParameters groundContactParameters;
   private LLAQuadrupedNetClassList netClassList;
   private RobotController headController;
   private SimulationConstructionSetParameters scsParameters;
   private SDFRobot sdfRobot;
   private SDFFullQuadrupedRobotModel fullRobotModel;
   private YoGraphicsListRegistry yoGraphicsListRegistry;
   private YoGraphicsListRegistry yoGraphicsListRegistryForDetachedOverhead;
   private QuadrupedReferenceFrames referenceFrames;
   private DRCKinematicsBasedStateEstimator stateEstimator;
   private SDFPerfectSimulatedOutputWriter sdfPerfectSimulatedOutputWriter;
   private SDFQuadrupedPerfectSimulatedSensor sdfQuadrupedPerfectSimulatedSensor;
   
   // Factories
   QuadrupedControllerManagerFactory controllerManagerFactory = new QuadrupedControllerManagerFactory();
   QuadrupedSimulationFactory simulationFactory = new QuadrupedSimulationFactory();
   
   // Products
   private QuadrupedControllerManager controllerManager;
   private SimulationConstructionSet scs;

   private void createSimulationParameters() throws IOException
   {
      modelFactory = new LLAQuadrupedModelFactory();
      sdfRobot = modelFactory.createSdfRobot();
      fullRobotModel = modelFactory.createFullRobotModel();
      yoGraphicsListRegistry = new YoGraphicsListRegistry();
      yoGraphicsListRegistry.setYoGraphicsUpdatedRemotely(true);
      yoGraphicsListRegistryForDetachedOverhead = new YoGraphicsListRegistry();
      netClassList = new LLAQuadrupedNetClassList();
      headController = null;
      stateEstimator = null;
      physicalProperties = new LLAQuadrupedPhysicalProperties();
      standPrepParameters = new LLAQuadrupedStandPrepParameters();
      groundContactParameters = new LLAQuadrupedGroundContactParameters();
      scsParameters = new SimulationConstructionSetParameters();
      ParameterRegistry.getInstance().loadFromResources("parameters/simulation.param");
      
      referenceFrames = new QuadrupedReferenceFrames(fullRobotModel, physicalProperties);
      sdfPerfectSimulatedOutputWriter = new SDFPerfectSimulatedOutputWriter(sdfRobot, fullRobotModel);
      
      sdfQuadrupedPerfectSimulatedSensor = new SDFQuadrupedPerfectSimulatedSensor(sdfRobot, fullRobotModel, referenceFrames);
   }
   
   private void createSCS() throws IOException
   {
      simulationFactory.setControlDT(SIMULATION_DT);
      simulationFactory.setGravity(SIMULATION_GRAVITY);
      simulationFactory.setRecordFrequency(RECORD_FREQUENCY);
      simulationFactory.setControllerManager(controllerManager);
      simulationFactory.setGroundContactModelType(groundContactModelType);
      simulationFactory.setGroundContactParameters(groundContactParameters);
      simulationFactory.setHeadController(headController);
      simulationFactory.setModelFactory(modelFactory);
      simulationFactory.setSCSParameters(scsParameters);
      simulationFactory.setSDFRobot(sdfRobot);
      simulationFactory.setStateEstimator(stateEstimator);
      simulationFactory.setSensorReader(sdfQuadrupedPerfectSimulatedSensor);
      simulationFactory.setOutputWriter(sdfPerfectSimulatedOutputWriter);
      simulationFactory.setShowPlotter(true);
      simulationFactory.setUseTrackAndDolly(false);
      simulationFactory.setYoGraphicsListRegistry(yoGraphicsListRegistry);
      simulationFactory.setYoGraphicsListRegistryForDetachedOverhead(yoGraphicsListRegistryForDetachedOverhead);
      simulationFactory.setStandPrepParameters(standPrepParameters);
      simulationFactory.setFullRobotModel(fullRobotModel);
      simulationFactory.setKryoNetClassList(netClassList);
      simulationFactory.setPhysicalProperties(physicalProperties);
      simulationFactory.setReferenceFrames(referenceFrames);
      simulationFactory.setControlMode(CONTROL_MODE);
      
      scs = simulationFactory.createSimulation();
   }
   
   public SimulationConstructionSet createSimulation() throws IOException
   {
      createSimulationParameters();
      createSCS();
      
      return scs;
   }

   public static void main(String[] commandLineArguments)
   {
      try
      {
         LLAQuadrupedSimulationFactory simulationFactory = new LLAQuadrupedSimulationFactory();
         SimulationConstructionSet scs = simulationFactory.createSimulation();
         scs.startOnAThread();
         scs.simulate();
      }
      catch (IOException ioException)
      {
         ioException.printStackTrace();
      }
   }
}
