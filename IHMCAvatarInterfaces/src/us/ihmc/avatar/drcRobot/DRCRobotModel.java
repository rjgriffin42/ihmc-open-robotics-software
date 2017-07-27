package us.ihmc.avatar.drcRobot;

import com.jme3.math.Transform;

import us.ihmc.avatar.handControl.HandCommandManager;
import us.ihmc.avatar.handControl.packetsAndConsumers.HandModel;
import us.ihmc.avatar.initialSetup.DRCRobotInitialSetup;
import us.ihmc.avatar.ros.DRCROSPPSTimestampOffsetProvider;
import us.ihmc.avatar.sensors.DRCSensorSuiteManager;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.humanoidRobotics.communication.streamingData.HumanoidGlobalDataProducer;
import us.ihmc.humanoidRobotics.footstep.footstepGenerator.FootstepPlanningParameterization;
import us.ihmc.ihmcPerception.depthData.CollisionBoxProvider;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotDataLogger.logger.LogSettings;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.simulationConstructionSetTools.robotController.MultiThreadedRobotControlElement;
import us.ihmc.simulationconstructionset.FloatingRootJointRobot;
import us.ihmc.simulationconstructionset.HumanoidFloatingRootJointRobot;
import us.ihmc.tools.thread.CloseableAndDisposableRegistry;
import us.ihmc.wholeBodyController.DRCHandType;
import us.ihmc.wholeBodyController.DRCOutputWriter;
import us.ihmc.wholeBodyController.DRCRobotJointMap;
import us.ihmc.wholeBodyController.SimulatedFullHumanoidRobotModelFactory;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;
import us.ihmc.wholeBodyController.concurrent.ThreadDataSynchronizerInterface;

public interface DRCRobotModel extends SimulatedFullHumanoidRobotModelFactory, WholeBodyControllerParameters
{
   public abstract FootstepPlanningParameterization getFootstepParameters();

   public abstract StateEstimatorParameters getStateEstimatorParameters();

   public abstract DRCRobotPhysicalProperties getPhysicalProperties();

   public abstract DRCRobotJointMap getJointMap();

   public abstract DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> getDefaultRobotInitialSetup(double groundHeight, double initialYaw);

   public abstract void setEnableJointDamping(boolean enableJointDamping);

   public abstract boolean getEnableJointDamping();

   public abstract HandModel getHandModel();

   public abstract Transform getJmeTransformWristToHand(RobotSide side);

   public abstract RigidBodyTransform getTransform3dWristToHand(RobotSide side);

   public abstract double getSimulateDT();

   public abstract double getEstimatorDT();

   public abstract double getStandPrepAngle(String jointName);

   public abstract DRCROSPPSTimestampOffsetProvider getPPSTimestampOffsetProvider();

   public abstract DRCSensorSuiteManager getSensorSuiteManager();

   public abstract SideDependentList<HandCommandManager> createHandCommandManager();

   public abstract MultiThreadedRobotControlElement createSimulatedHandController(FloatingRootJointRobot simulatedRobot,
         ThreadDataSynchronizerInterface threadDataSynchronizer, HumanoidGlobalDataProducer globalDataProducer,
         CloseableAndDisposableRegistry closeableAndDisposableRegistry);

   public abstract DRCHandType getDRCHandType();

   public abstract LogSettings getLogSettings();

   public abstract LogModelProvider getLogModelProvider();

   public abstract String getSimpleRobotName();

   public abstract CollisionBoxProvider getCollisionBoxProvider();

   /**
    * Override this method to create a custom output writer to be used with this robot.
    * <p>
    * <b> This output writer is meant to be used in simulation only.
    * </p>
    *
    * @param parentOutputWriter the default output writer that should be wrapped in the custom output writer.
    * @return the custom output writer.
    */
   public default DRCOutputWriter getCustomSimulationOutputWriter(DRCOutputWriter parentOutputWriter)
   {
      return null;
   }
}
