package us.ihmc.stateEstimation.humanoid;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.sensorProcessing.simulatedSensors.JointAndIMUSensorMap;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimationDataFromController;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimator;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorWithPorts;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.ControlFlowGraphExecutorController;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.RigidBodyToIndexMap;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.SensorAndEstimatorAssembler;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.AfterJointReferenceFrameNameMap;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;


public class DRCKalmanFilterBasedStateEstimator implements DRCStateEstimatorInterface
{
   //   private final SensorNoiseParameters sensorNoiseParametersForEstimator =
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final SensorAndEstimatorAssembler sensorAndEstimatorAssembler;
   private final StateEstimatorWithPorts stateEstimatorWithPorts;
   private final ControlFlowGraphExecutorController controlFlowGraphExecutorController;
   
   private final boolean assumePerfectIMU;

   public DRCKalmanFilterBasedStateEstimator(StateEstimationDataFromController stateEstimatorDataFromControllerSource,
         FullInverseDynamicsStructure inverseDynamicsStructure, AfterJointReferenceFrameNameMap estimatorReferenceFrameMap,
         RigidBodyToIndexMap estimatorRigidBodyToIndexMap, JointAndIMUSensorMap jointAndIMUSensorMap, double gravitationalAcceleration,
         StateEstimatorParameters stateEstimatorParameters, 
         SideDependentList<ContactablePlaneBody> bipedFeet, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.assumePerfectIMU = true; //stateEstimatorParameters.getAssumePerfectIMU();
      
      // Make the estimator here.
      sensorAndEstimatorAssembler = new SensorAndEstimatorAssembler(stateEstimatorDataFromControllerSource,
            jointAndIMUSensorMap, stateEstimatorParameters, gravitationalAcceleration, inverseDynamicsStructure,
            estimatorReferenceFrameMap, estimatorRigidBodyToIndexMap, registry);

      stateEstimatorWithPorts = sensorAndEstimatorAssembler.getEstimator();

      ControlFlowGraph controlFlowGraph = sensorAndEstimatorAssembler.getControlFlowGraph();

      controlFlowGraphExecutorController = new ControlFlowGraphExecutorController(controlFlowGraph);
      registry.addChild(controlFlowGraphExecutorController.getYoVariableRegistry());
   }

   public StateEstimator getStateEstimator()
   {
      return stateEstimatorWithPorts;
   }

   public void initialize()
   {
      sensorAndEstimatorAssembler.initialize();
      doControl();
   }
   
   public void initializeEstimatorToActual(Point3d initialCoMPosition, Quat4d initialEstimationLinkOrientation)
   {
      // Setting the initial CoM Position here.
      FramePoint estimatedCoMPosition = new FramePoint();
      stateEstimatorWithPorts.getEstimatedCoMPosition(estimatedCoMPosition);
      estimatedCoMPosition.checkReferenceFrameMatch(ReferenceFrame.getWorldFrame());
      estimatedCoMPosition.set(initialCoMPosition);

      FrameOrientation estimatedOrientation = null;
      if(!assumePerfectIMU)
      {
         estimatedOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame());
         stateEstimatorWithPorts.getEstimatedOrientation(estimatedOrientation);

         estimatedOrientation.checkReferenceFrameMatch(ReferenceFrame.getWorldFrame());
         estimatedOrientation.set(initialEstimationLinkOrientation);
      }
      else
      {
         estimatedOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame(), initialEstimationLinkOrientation);
      }

      sensorAndEstimatorAssembler.initializeEstimatorToActual(estimatedCoMPosition, estimatedOrientation);
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {
      controlFlowGraphExecutorController.doControl();
   }
}
