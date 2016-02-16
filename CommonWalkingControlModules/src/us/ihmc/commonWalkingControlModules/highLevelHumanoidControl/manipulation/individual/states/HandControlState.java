package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlMode;
import us.ihmc.robotics.stateMachines.State;

public abstract class HandControlState extends State<HandControlMode>
{

   public HandControlState(HandControlMode stateEnum)
   {
      super(stateEnum);
   }

}
