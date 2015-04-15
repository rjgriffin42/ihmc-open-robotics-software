package us.ihmc.darpaRoboticsChallenge.drcRobot;

import java.util.ArrayList;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.trajectories.HeightCalculatorParameters;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.darpaRoboticsChallenge.handControl.HandCommandManager;
import us.ihmc.darpaRoboticsChallenge.handControl.packetsAndConsumers.HandModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.sensors.DRCSensorSuiteManager;
import us.ihmc.pathGeneration.footstepPlanner.FootstepPlanningParameterization;
import us.ihmc.ihmcPerception.depthData.CollisionBoxProvider;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotDataCommunication.logger.LogSettings;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.simulationconstructionset.physics.ScsCollisionConfigure;
import us.ihmc.simulationconstructionset.robotController.MultiThreadedRobotControlElement;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.ros.PPSTimestampOffsetProvider;
import us.ihmc.wholeBodyController.DRCHandType;
import us.ihmc.wholeBodyController.DRCRobotJointMap;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;
import us.ihmc.wholeBodyController.concurrent.ThreadDataSynchronizerInterface;

import com.jme3.math.Transform;

public interface DRCRobotModel extends WholeBodyControllerParameters
{
   // TODO: RobotBoundingBoxes.java

// public abstract boolean isRunningOnRealRobot();

   public abstract FootstepPlanningParameterization getFootstepParameters();

   public abstract WalkingControllerParameters getDrivingControllerParameters();

   public abstract StateEstimatorParameters getStateEstimatorParameters();

   public abstract DRCRobotPhysicalProperties getPhysicalProperties();

   public abstract DRCRobotJointMap getJointMap();

   public abstract DRCRobotSensorInformation getSensorInformation();

   public abstract DRCRobotInitialSetup<SDFRobot> getDefaultRobotInitialSetup(double groundHeight, double initialYaw);

   public abstract ScsCollisionConfigure getPhysicsConfigure(SDFRobot robotModel);

   public abstract void setEnableJointDamping(boolean enableJointDamping);

   public abstract boolean getEnableJointDamping();

   public abstract void setJointDamping(SDFRobot simulatedRobot);

   public abstract HandModel getHandModel();

   public abstract Transform getJmeTransformWristToHand(RobotSide side);

   public abstract RigidBodyTransform getTransform3dWristToHand(RobotSide side);

   public abstract double getSimulateDT();

   public abstract double getEstimatorDT();

   public abstract PPSTimestampOffsetProvider getPPSTimestampOffsetProvider();

   public abstract DRCSensorSuiteManager getSensorSuiteManager();

   public abstract SideDependentList<HandCommandManager> createHandCommandManager();

   public abstract MultiThreadedRobotControlElement createSimulatedHandController(SDFRobot simulatedRobot,
           ThreadDataSynchronizerInterface threadDataSynchronizer, GlobalDataProducer globalDataProducer);

   public abstract DRCHandType getDRCHandType();
   
   public abstract SideDependentList<ArrayList<String>> getActuatableFingerJointNames();

   public abstract LogSettings getLogSettings();

   public abstract LogModelProvider getLogModelProvider();

   public abstract Pair<Class<?>, String[]> getOperatorInterfaceStarter();

   public abstract Class<?> getSpectatorInterfaceClass();

   public abstract HeightCalculatorParameters getHeightCalculatorParameters();

   public abstract String getSimpleRobotName();
   
   public abstract CollisionBoxProvider getCollisionBoxProvider();
}
