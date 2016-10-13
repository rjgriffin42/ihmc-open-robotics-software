package us.ihmc.robotiq.communication.registers;

import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rACT;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rATR;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rGTO;
import us.ihmc.robotiq.communication.registers.ActionRequestRegister.rMOD;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;

@ContinuousIntegrationPlan(categories = IntegrationCategory.FAST)
public class ActionRequestRegisterTest extends RobotiqOutputRegisterTest
{
   @Override
   public RobotiqOutputRegister getOutputRegister()
   {
      return new ActionRequestRegister(rACT.ACTIVATE_GRIPPER, rMOD.WIDE_MODE, rGTO.GO_TO, rATR.NORMAL);
   }

   @Override
   protected byte getExpectedByteValue()
   {
      return 13;
   }
}
