package us.ihmc.valkyrie;

import us.ihmc.commonWalkingControlModules.configurations.HighLevelControllerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.StandPrepParameters;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;

public class ValkyrieHighLevelControllerParameters implements HighLevelControllerParameters
{
   private final ValkyrieStandPrepParameters standPrepParameters;
   private final ValkyriePositionControlParameters positionControlParameters;

   private final boolean runningOnRealRobot;

   public ValkyrieHighLevelControllerParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;

      standPrepParameters = new ValkyrieStandPrepParameters();
      positionControlParameters = new ValkyriePositionControlParameters();
   }

   @Override
   public StandPrepParameters getStandPrepParameters()
   {
      return standPrepParameters;
   }

   @Override
   public JointDesiredControlMode getJointDesiredControlMode(String jointName, HighLevelController state)
   {
      switch(state)
      {
      case DO_NOTHING_BEHAVIOR:
         return JointDesiredControlMode.DISABLED;
      default:
         return JointDesiredControlMode.EFFORT;
      }
   }

   @Override
   public double getDesiredJointStiffness(String jointName)
   {
      return positionControlParameters.getProportionalGain(jointName);
   }

   @Override
   public double getDesiredJointDamping(String jointName)
   {
      return positionControlParameters.getDerivativeGain(jointName);
   }

   @Override
   public HighLevelController getDefaultInitialControllerState()
   {
      return HighLevelController.WALKING;
   }

   @Override
   public HighLevelController getFallbackControllerState()
   {
      return HighLevelController.DO_NOTHING_BEHAVIOR;
   }

   @Override
   public boolean automaticallyTransitionToWalkingWhenReady()
   {
      return runningOnRealRobot;
   }

   @Override
   public double getTimeToMoveInStandPrep()
   {
      return 5.0;
   }

   @Override
   public double getMinimumTimeInStandReady()
   {
      return 3.0;
   }

   @Override
   public double getTimeInStandTransition()
   {
      return 3.0;
   }
}
