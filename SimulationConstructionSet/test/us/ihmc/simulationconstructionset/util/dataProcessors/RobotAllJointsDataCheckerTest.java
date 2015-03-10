package us.ihmc.simulationconstructionset.util.dataProcessors;

import org.junit.After;
import org.junit.Test;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.BambooPlan;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.code.agileTesting.BambooPlanType;

@BambooPlan(planType = {BambooPlanType.Fast})
public class RobotAllJointsDataCheckerTest
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();


   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         //ThreadTools.sleepForever();
      }
   }

   @EstimatedDuration(duration = 2.0)
   @Test(timeout = 300000)
   public void test()
   {
      TwoLinkRobotForTesting twoLinkRobotForTesting = new TwoLinkRobotForTesting();

      SimulationConstructionSet scs = new SimulationConstructionSet(twoLinkRobotForTesting);
      scs.setDT(0.00001, 100);
      scs.startOnAThread();

      twoLinkRobotForTesting.setElbowPosition(0.0);
      twoLinkRobotForTesting.setUpperPosition(3.0);
      
      twoLinkRobotForTesting.setElbowVelocity(-2.0);
      twoLinkRobotForTesting.setUpperVelocity(-3.0);
      
      scs.simulate(6.0);
      
      while(scs.isSimulating())
      {
         Thread.yield();
      }
      
      RobotAllJointsDataChecker robotAllJointsDataChecker = new RobotAllJointsDataChecker(scs, twoLinkRobotForTesting);
      robotAllJointsDataChecker.cropFirstPoint();
      
      scs.applyDataProcessingFunction(robotAllJointsDataChecker);
   }

}
