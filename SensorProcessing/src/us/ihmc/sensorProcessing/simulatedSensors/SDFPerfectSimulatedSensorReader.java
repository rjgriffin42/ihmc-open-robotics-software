package us.ihmc.sensorProcessing.simulatedSensors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.communication.packets.dataobjects.AuxiliaryRobotData;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorRawOutputMapReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.robotController.RawSensorReader;
import us.ihmc.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDataHolder;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDataHolderReadOnly;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDefinition;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.TimeTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;

public class SDFPerfectSimulatedSensorReader implements RawSensorReader, SensorOutputMapReadOnly, SensorRawOutputMapReadOnly
{
   private final String name;
   private final SDFRobot robot;
   private final SixDoFJoint rootJoint;
   private final CommonHumanoidReferenceFrames referenceFrames;

   private final ArrayList<Pair<OneDegreeOfFreedomJoint, OneDoFJoint>> revoluteJoints = new ArrayList<Pair<OneDegreeOfFreedomJoint, OneDoFJoint>>();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final LongYoVariable timestamp = new LongYoVariable("timestamp", registry);
   private final LongYoVariable visionSensorTimestamp = new LongYoVariable("visionSensorTimestamp", registry);
   private final LongYoVariable sensorHeadPPSTimetamp = new LongYoVariable("sensorHeadPPSTimetamp", registry);

   private final LinkedHashMap<ForceSensorDefinition, WrenchCalculatorInterface> forceTorqueSensors = new LinkedHashMap<ForceSensorDefinition, WrenchCalculatorInterface>();

   private final ForceSensorDataHolder forceSensorDataHolderToUpdate;

   public SDFPerfectSimulatedSensorReader(SDFRobot robot, FullRobotModel fullRobotModel, CommonHumanoidReferenceFrames referenceFrames)
   {
      this(robot, fullRobotModel, null, referenceFrames);
   }

   public SDFPerfectSimulatedSensorReader(SDFRobot robot, FullRobotModel fullRobotModel, ForceSensorDataHolder forceSensorDataHolderToUpdate,
         CommonHumanoidReferenceFrames referenceFrames)
   {
      this(robot, fullRobotModel.getRootJoint(), forceSensorDataHolderToUpdate, referenceFrames);
   }

   public SDFPerfectSimulatedSensorReader(SDFRobot robot, SixDoFJoint rootJoint, ForceSensorDataHolder forceSensorDataHolderToUpdate,
         CommonHumanoidReferenceFrames referenceFrames)
   {
      name = robot.getName() + "SimulatedSensorReader";
      this.robot = robot;
      this.referenceFrames = referenceFrames;
      this.forceSensorDataHolderToUpdate = forceSensorDataHolderToUpdate;

      this.rootJoint = rootJoint;

      InverseDynamicsJoint[] jointsArray = ScrewTools.computeSubtreeJoints(rootJoint.getSuccessor());

      for (InverseDynamicsJoint joint : jointsArray)
      {
         if (joint instanceof OneDoFJoint)
         {
            OneDoFJoint oneDoFJoint = (OneDoFJoint) joint;
            String name = oneDoFJoint.getName();
            OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint = robot.getOneDegreeOfFreedomJoint(name);

            Pair<OneDegreeOfFreedomJoint, OneDoFJoint> jointPair = new Pair<OneDegreeOfFreedomJoint, OneDoFJoint>(oneDegreeOfFreedomJoint, oneDoFJoint);
            revoluteJoints.add(jointPair);
         }
      }
   }

   public void addForceTorqueSensorPort(ForceSensorDefinition forceSensorDefinition, WrenchCalculatorInterface groundContactPointBasedWrenchCalculator)
   {
      forceTorqueSensors.put(forceSensorDefinition, groundContactPointBasedWrenchCalculator);
   }

   @Override
   public void initialize()
   {
      read();
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getDescription()
   {
      return getName();
   }

   private final RigidBodyTransform temporaryRootToWorldTransform = new RigidBodyTransform();

   @Override
   public void read()
   {
      // Think about adding root body acceleration to the fullrobotmodel
      readAndUpdateOneDoFJointPositionsVelocitiesAndAccelerations();
      readAndUpdateRootJointPositionAndOrientation();
      updateReferenceFrames();
      readAndUpdateRootJointAngularAndLinearVelocity();

      long timestamp = TimeTools.secondsToNanoSeconds(robot.getTime());
      this.timestamp.set(timestamp);
      this.visionSensorTimestamp.set(timestamp);
      this.sensorHeadPPSTimetamp.set(timestamp);

      if (forceSensorDataHolderToUpdate != null)
      {
         for (Entry<ForceSensorDefinition, WrenchCalculatorInterface> forceTorqueSensorEntry : forceTorqueSensors.entrySet())
         {
            final WrenchCalculatorInterface forceTorqueSensor = forceTorqueSensorEntry.getValue();
            forceTorqueSensor.calculate();
            forceSensorDataHolderToUpdate.setForceSensorValue(forceTorqueSensorEntry.getKey(), forceTorqueSensor.getWrench());
         }
      }
   }

   private void readAndUpdateRootJointAngularAndLinearVelocity()
   {
      ReferenceFrame elevatorFrame = rootJoint.getFrameBeforeJoint();
      ReferenceFrame pelvisFrame = rootJoint.getFrameAfterJoint();

      FrameVector linearVelocity = robot.getRootJointVelocity();
      linearVelocity.changeFrame(pelvisFrame);

      FrameVector angularVelocity = robot.getPelvisAngularVelocityInPelvisFrame(pelvisFrame);
      angularVelocity.changeFrame(pelvisFrame);

      Twist bodyTwist = new Twist(pelvisFrame, elevatorFrame, pelvisFrame, linearVelocity.getVector(), angularVelocity.getVector());
      rootJoint.setJointTwist(bodyTwist);
   }

   private void updateReferenceFrames()
   {
      if (referenceFrames != null)
      {
         referenceFrames.updateFrames();
      }
      else
      {
         rootJoint.getPredecessor().updateFramesRecursively();
      }
   }

   private void readAndUpdateRootJointPositionAndOrientation()
   {
      packRootTransform(robot, temporaryRootToWorldTransform);
      temporaryRootToWorldTransform.normalize();
      rootJoint.setPositionAndRotation(temporaryRootToWorldTransform);
   }

   private void readAndUpdateOneDoFJointPositionsVelocitiesAndAccelerations()
   {
      for (Pair<OneDegreeOfFreedomJoint, OneDoFJoint> jointPair : revoluteJoints)
      {
         OneDegreeOfFreedomJoint pinJoint = jointPair.first();
         OneDoFJoint revoluteJoint = jointPair.second();

         revoluteJoint.setQ(pinJoint.getQ().getDoubleValue());
         revoluteJoint.setQd(pinJoint.getQD().getDoubleValue());
         revoluteJoint.setQdd(pinJoint.getQDD().getDoubleValue());
      }
   }

   protected void packRootTransform(SDFRobot robot, RigidBodyTransform transformToPack)
   {
      robot.getRootJointToWorldTransform(transformToPack);
   }

   @Override
   public long getTimestamp()
   {
      return timestamp.getLongValue();
   }

   @Override
   public long getVisionSensorTimestamp()
   {
      return visionSensorTimestamp.getLongValue();
   }

   @Override
   public long getSensorHeadPPSTimestamp()
   {
      return sensorHeadPPSTimetamp.getLongValue();
   }

   @Override
   public double getJointPositionProcessedOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getQ();
   }

   @Override
   public double getJointVelocityProcessedOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getQd();
   }

   @Override
   public double getJointAccelerationProcessedOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getQdd();
   }

   @Override
   public double getJointTauProcessedOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getTau();
   }

   @Override
   public List<? extends IMUSensorReadOnly> getIMUProcessedOutputs()
   {
      return new ArrayList<>();
   }

   @Override
   public ForceSensorDataHolderReadOnly getForceSensorProcessedOutputs()
   {
      return forceSensorDataHolderToUpdate;
   }

   @Override
   public double getJointPositionRawOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getQ();
   }

   @Override
   public double getJointVelocityRawOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getQd();
   }

   @Override
   public double getJointAccelerationRawOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getQdd();
   }

   @Override
   public double getJointTauRawOutput(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.getTau();
   }

   @Override
   public boolean isJointEnabled(OneDoFJoint oneDoFJoint)
   {
      return oneDoFJoint.isEnabled();
   }

   @Override
   public List<? extends IMUSensorReadOnly> getIMURawOutputs()
   {
      return new ArrayList<>();
   }

   @Override
   public ForceSensorDataHolderReadOnly getForceSensorRawOutputs()
   {
      return forceSensorDataHolderToUpdate;
   }

   @Override
   public AuxiliaryRobotData getAuxiliaryRobotData()
   {
      return null;
   }
}
