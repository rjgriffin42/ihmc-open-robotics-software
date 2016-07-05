package us.ihmc.quadrupedRobotics;

import us.ihmc.quadrupedRobotics.controller.position.QuadrupedPositionControllerRequestedEvent;
import us.ihmc.quadrupedRobotics.controller.position.QuadrupedPositionControllerState;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public class QuadrupedPositionTestYoVariables extends QuadrupedTestYoVariables
{
   private final EnumYoVariable<QuadrupedPositionControllerRequestedEvent> userTrigger;
   private final EnumYoVariable<QuadrupedPositionControllerState> positionControllerState;

   @SuppressWarnings("unchecked")
   public QuadrupedPositionTestYoVariables(SimulationConstructionSet scs)
   {
      super(scs);
      
      userTrigger = (EnumYoVariable<QuadrupedPositionControllerRequestedEvent>) scs.getVariable("usertrigger");
      positionControllerState = (EnumYoVariable<QuadrupedPositionControllerState>) scs.getVariable("positionControllerState");
   }

   public EnumYoVariable<QuadrupedPositionControllerRequestedEvent> getUserTrigger()
   {
      return userTrigger;
   }

   public EnumYoVariable<QuadrupedPositionControllerState> getPositionControllerState()
   {
      return positionControllerState;
   }
}
