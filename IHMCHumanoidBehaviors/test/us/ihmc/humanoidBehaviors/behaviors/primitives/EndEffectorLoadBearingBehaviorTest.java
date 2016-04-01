package us.ihmc.humanoidBehaviors.behaviors.primitives;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidRobotics.communication.packets.walking.EndEffectorLoadBearingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.EndEffectorLoadBearingMessage.EndEffector;
import us.ihmc.humanoidRobotics.communication.packets.walking.EndEffectorLoadBearingMessage.LoadBearingRequest;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class EndEffectorLoadBearingBehaviorTest
{
   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testSetInput()
   {
      OutgoingCommunicationBridgeInterface outgoingCommunicationBridge = null;
      EndEffectorLoadBearingBehavior endEffectorLoadBearingBehavior = new EndEffectorLoadBearingBehavior(outgoingCommunicationBridge);

      EndEffectorLoadBearingMessage message = new EndEffectorLoadBearingMessage(RobotSide.LEFT, EndEffector.FOOT, LoadBearingRequest.LOAD);
      
      PacketDestination destination = PacketDestination.UI;
      message.setDestination(destination);
      
      endEffectorLoadBearingBehavior.setInput(message);
      
      assertTrue("Input was not set correctly.", endEffectorLoadBearingBehavior.hasInputBeenSet());
   }
}
