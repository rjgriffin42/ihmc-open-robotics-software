package us.ihmc.quadrupedRobotics.controller.states;

import java.util.ArrayList;

import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.quadrupedRobotics.controlModules.foot.QuadrupedFeetManager;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.robotics.stateMachine.core.State;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputBasics;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.ArrayList;

/**
 * A controller that does nothing, but signifies that the robot is ready to transition to stand prep
 */
public class QuadrupedDoNothingController implements State
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final JointDesiredOutputList jointDesiredOutputList;
   private final ArrayList<YoDouble> desiredDoNothingTorques = new ArrayList<>();
   private final ArrayList<OneDoFJointBasics> legJoints = new ArrayList<>();
   private final QuadrupedFeetManager feetManager;

   private final YoBoolean forceFeedbackControlEnabled;

   public QuadrupedDoNothingController(QuadrupedFeetManager feetManager, QuadrupedRuntimeEnvironment environment, QuadrupedControlMode controlMode,
                                       YoVariableRegistry parentRegistry)
   {
      FullQuadrupedRobotModel fullRobotModel = environment.getFullRobotModel();
      this.jointDesiredOutputList = environment.getJointDesiredOutputList();

      for (OneDoFJointBasics joint : fullRobotModel.getOneDoFJoints())
      {
            legJoints.add(joint);
            desiredDoNothingTorques.add(new YoDouble(joint.getName() + "DoNothingTorque", registry));
      }

      forceFeedbackControlEnabled = new YoBoolean("forceFeedbackControlEnabled", registry);
      forceFeedbackControlEnabled.set(controlMode == QuadrupedControlMode.FORCE);

      this.feetManager = feetManager;
      parentRegistry.addChild(registry);
   }

   @Override
   public void onEntry()
   {
      for (int i = 0; i < legJoints.size(); i++)
      {
         OneDoFJointBasics joint = legJoints.get(i);
         JointDesiredOutputBasics jointDesiredOutput = jointDesiredOutputList.getJointDesiredOutput(joint);
         if (forceFeedbackControlEnabled.getBooleanValue())
            jointDesiredOutput.setControlMode(JointDesiredControlMode.EFFORT);
         else
            jointDesiredOutput.setControlMode(JointDesiredControlMode.POSITION);
         jointDesiredOutput.setStiffness(0.0);
         jointDesiredOutput.setDamping(0.0);
         jointDesiredOutput.setDesiredTorque(0.0);
      }

   }

   @Override
   public void doAction(double timeInState)
   {
      for (int i = 0; i < legJoints.size(); i++)
      {
         OneDoFJointBasics joint = legJoints.get(i);
         JointDesiredOutputBasics jointDesiredOutput = jointDesiredOutputList.getJointDesiredOutput(joint);
         jointDesiredOutput.setDesiredTorque(desiredDoNothingTorques.get(i).getDoubleValue());
      }
   }

   @Override
   public void onExit()
   {
   }
}

