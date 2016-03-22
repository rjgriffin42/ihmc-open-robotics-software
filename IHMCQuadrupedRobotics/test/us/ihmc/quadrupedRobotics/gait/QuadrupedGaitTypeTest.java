package us.ihmc.quadrupedRobotics.gait;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.tools.testing.JUnitTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.thread.RunnableThatThrows;

public class QuadrupedGaitTypeTest
{
   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGaitStateTimes()
   {
      assertEquals("not ALL_FOURS", QuadrupedSupportConfiguration.ALL_FOURS, QuadrupedGaitCycle.SAFE_WALK.getGaitState(0.5));
      assertEquals("not ALL_FOURS", QuadrupedSupportConfiguration.ALL_FOURS, QuadrupedGaitCycle.SAFE_WALK.getGaitState(0.52));
      assertEquals("not WALK_HIND_RIGHT", QuadrupedSupportConfiguration.WALK_HIND_RIGHT, QuadrupedGaitCycle.SAFE_WALK.getGaitState(0.9));
      assertEquals("not ALL_FOURS", QuadrupedSupportConfiguration.ALL_FOURS, QuadrupedGaitCycle.SAFE_WALK.getGaitState(0.0));
      assertEquals("not WALK_FRONT_LEFT", QuadrupedSupportConfiguration.WALK_FRONT_LEFT, QuadrupedGaitCycle.SAFE_WALK.getGaitState(0.05));
      assertEquals("not ALL_FOURS", QuadrupedSupportConfiguration.ALL_FOURS, QuadrupedGaitCycle.SAFE_WALK.getGaitState(1.0 % 1.0));
      assertEquals("not ALL_FOURS", QuadrupedSupportConfiguration.ALL_FOURS, QuadrupedGaitCycle.SAFE_WALK.getGaitState(1.01 % 1.0));
   }
   
   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testSwingDurations()
   {
      assertEquals("not correct", 0.20, QuadrupedGaitCycle.SAFE_WALK.getRemainingSwingDuration(RobotQuadrant.FRONT_LEFT, 0.05), 1e-7);
      assertEquals("not correct", 0.73, QuadrupedGaitCycle.BOUND.getRemainingSwingDuration(RobotQuadrant.HIND_RIGHT, 0.27), 1e-7);
      assertEquals("not correct", 0.72, QuadrupedGaitCycle.BOUND.getRemainingSwingDuration(RobotQuadrant.HIND_RIGHT, 0.28), 1e-7);
      assertEquals("not correct", 0.35, QuadrupedGaitCycle.TRANSVERSE_GALLOP.getRemainingSwingDuration(RobotQuadrant.FRONT_LEFT, 0.0), 1e-7);
      
      JUnitTools.assertExceptionThrown(RuntimeException.class, new RunnableThatThrows()
      {
         @Override
         public void run() throws Throwable
         {
            // Front right is not a swing leg at 0.05
            QuadrupedGaitCycle.SAFE_WALK.getRemainingSwingDuration(RobotQuadrant.FRONT_RIGHT, 0.05);
         }
      });
   }
}
