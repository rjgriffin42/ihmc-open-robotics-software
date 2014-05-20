package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.statemachines.State;

public abstract class HighLevelBehavior extends State<HighLevelState>
{

   public HighLevelBehavior(HighLevelState stateEnum)
   {
      super(stateEnum);
   }

   @Override
   public abstract void doAction();

   @Override
   public abstract void doTransitionIntoAction();

   @Override
   public abstract void doTransitionOutOfAction();

   public abstract YoVariableRegistry getYoVariableRegistry();
}
