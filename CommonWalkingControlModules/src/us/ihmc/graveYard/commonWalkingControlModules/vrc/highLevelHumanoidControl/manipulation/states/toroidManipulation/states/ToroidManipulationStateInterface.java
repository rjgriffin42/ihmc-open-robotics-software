package us.ihmc.graveYard.commonWalkingControlModules.vrc.highLevelHumanoidControl.manipulation.states.toroidManipulation.states;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.stateMachines.State;


public abstract class ToroidManipulationStateInterface<T extends Enum<T>> extends State<T>
{
   public ToroidManipulationStateInterface(T stateEnum)
   {
      super(stateEnum);
   }

   public abstract SpatialAccelerationVector getDesiredHandAcceleration(RobotSide robotSide);

   public abstract Wrench getHandExternalWrench(RobotSide robotSide);

   public abstract boolean isDone();
}
  