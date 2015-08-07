package us.ihmc.simulationconstructionset;

import static org.junit.Assert.assertNotNull;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.simulationconstructionset.RobotTools.SCSRobotFromInverseDynamicsRobotModel;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.robotics.screwTheory.ScrewTestTools;
import us.ihmc.robotics.screwTheory.ScrewTestTools.RandomFloatingChain;

public class RobotToolsTest
{
   private RandomFloatingChain getRandomFloatingChain()
   {
      Random random = new Random();

      Vector3d[] jointAxes = {new Vector3d(1.0, 0.0, 0.0)};
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);

      randomFloatingChain.setRandomPositionsAndVelocities(random);

      return randomFloatingChain;
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void testScsRobotFromInverseDynamicsRobotModel()
   {
      SCSRobotFromInverseDynamicsRobotModel scsRobotFromInverseDynamicsRobotModel = new RobotTools.SCSRobotFromInverseDynamicsRobotModel("robot",
                                                                                       getRandomFloatingChain().getRootJoint());

      assertNotNull(scsRobotFromInverseDynamicsRobotModel);
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void testAddScsJointUsingIDJoint()
   {
      Joint scsRootJoint = RobotTools.addSCSJointUsingIDJoint(getRandomFloatingChain().getRootJoint(), new Robot("robot"), true);

      assertNotNull(scsRootJoint);
   }
}
