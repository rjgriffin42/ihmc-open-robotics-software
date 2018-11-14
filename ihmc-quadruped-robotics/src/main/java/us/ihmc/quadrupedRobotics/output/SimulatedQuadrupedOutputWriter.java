package us.ihmc.quadrupedRobotics.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotModels.OutputWriter;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputList;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputListReadOnly;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputReadOnly;
import us.ihmc.sensorProcessing.outputData.LowLevelActuatorMode;
import us.ihmc.sensorProcessing.outputData.LowLevelState;
import us.ihmc.sensorProcessing.outputData.LowLevelStateList;
import us.ihmc.simulationToolkit.controllers.LowLevelActuatorSimulator;
import us.ihmc.simulationconstructionset.FloatingRootJointRobot;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class SimulatedQuadrupedOutputWriter implements OutputWriter
{
   private final OneDoFJointBasics[] controllerJoints;

   protected final JointDesiredOutputListReadOnly jointDesiredOutputList;
   protected final LowLevelStateList lowLevelStateList;
   private final HashMap<OneDoFJointBasics, LowLevelActuatorSimulator> quadrupedActuators = new HashMap<>();

   private final List<QuadrupedJointController> quadrupedJoints = new ArrayList<>();


   public SimulatedQuadrupedOutputWriter(FloatingRootJointRobot robot, FullRobotModel fullRobotModel, JointDesiredOutputList jointDesiredOutputList,
                                         double controlDT)
   {
      this.jointDesiredOutputList = jointDesiredOutputList;
      controllerJoints = fullRobotModel.getOneDoFJoints();
      lowLevelStateList = new LowLevelStateList(controllerJoints);

      YoVariableRegistry registry = new YoVariableRegistry("quadrupedOutputWriter");
      for (OneDoFJointBasics controllerJoint : controllerJoints)
      {
         String name = controllerJoint.getName();
         OneDegreeOfFreedomJoint simulatedJoint = robot.getOneDegreeOfFreedomJoint(name);

         LowLevelActuatorSimulator actuator = new LowLevelActuatorSimulator(simulatedJoint, lowLevelStateList.getLowLevelState(controllerJoint), controlDT);
         quadrupedActuators.put(controllerJoint, actuator);
         robot.setController(actuator);

         quadrupedJoints.add(new QuadrupedJointController(controllerJoint, jointDesiredOutputList.getJointDesiredOutput(controllerJoint), registry));
      }

      robot.getRobotsYoVariableRegistry().addChild(registry);
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void setFullRobotModel(FullRobotModel fullRobotModel)
   {
      throw new RuntimeException("This should have been done earlier.");
   }

   @Override
   public void write()
   {
      for (int i = 0; i < controllerJoints.length; i++)
      {
         quadrupedJoints.get(i).computeDesiredStateFromJointController();
      }

      for (OneDoFJointBasics controllerJoint : controllerJoints)
      {
         LowLevelState desiredState = lowLevelStateList.getLowLevelState(controllerJoint);
         JointDesiredOutputReadOnly jointSetpoints = jointDesiredOutputList.getJointDesiredOutput(controllerJoint);
         desiredState.clear();

         // Pass through setpoints
         if (jointSetpoints.hasDesiredAcceleration())
            desiredState.setAcceleration(jointSetpoints.getDesiredAcceleration());
         if (jointSetpoints.hasDesiredVelocity())
            desiredState.setVelocity(jointSetpoints.getDesiredVelocity());
         if (jointSetpoints.hasDesiredPosition())
            desiredState.setPosition(jointSetpoints.getDesiredPosition());
         if (jointSetpoints.hasDesiredTorque())
            desiredState.setEffort(jointSetpoints.getDesiredTorque());

         // Apply velocity scaling
         if (desiredState.isVelocityValid() && jointSetpoints.hasVelocityScaling())
            desiredState.setVelocity(jointSetpoints.getVelocityScaling() * desiredState.getVelocity());

         quadrupedActuators.get(controllerJoint).setActuatorMode(getDesiredActuatorMode(jointSetpoints));
      }
   }

   private LowLevelActuatorMode getDesiredActuatorMode(JointDesiredOutputReadOnly jointDesiredSetpoints)
   {
      if (jointDesiredSetpoints.hasControlMode())
      {
         switch (jointDesiredSetpoints.getControlMode())
         {
         case POSITION:
            return LowLevelActuatorMode.POSITION;
         case VELOCITY:
            return LowLevelActuatorMode.VELOCITY;
         case EFFORT:
            return LowLevelActuatorMode.EFFORT;
         case DISABLED:
            return LowLevelActuatorMode.DISABLED;
         default:
            throw new RuntimeException("Control mode " + jointDesiredSetpoints.getControlMode() + " not implemented.");
         }
      }
      return LowLevelActuatorMode.DISABLED;
   }
}
