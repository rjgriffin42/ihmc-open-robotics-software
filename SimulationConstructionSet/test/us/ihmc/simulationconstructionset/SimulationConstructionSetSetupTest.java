package us.ihmc.simulationconstructionset;

import javax.swing.JWindow;

import org.junit.Test;

import us.ihmc.simulationconstructionset.gui.SplashPanel;

public class SimulationConstructionSetSetupTest
{
   private static final int pauseTimeForGUIs = 5000;

   @Test(timeout=300000)
   public void testSplashScreen()
   {
      SplashPanel splashPanel = new SplashPanel();
      JWindow window = splashPanel.showSplashScreen();

      sleep(pauseTimeForGUIs);
      window.dispose();
   }

   @Test(timeout=300000)
   public void testSimulationConstructionSetWithoutARobot()
   {
      SimulationConstructionSet scs = new SimulationConstructionSet();
      Thread thread = new Thread(scs);
      thread.start();

      sleep(pauseTimeForGUIs);
      scs.closeAndDispose();
   }

   @Test(timeout=300000)
   public void testSimulationConstructionSetWithARobot()
   {
      Robot robot = new Robot("NullRobot");
      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      Thread thread = new Thread(scs);
      thread.start();

      sleep(pauseTimeForGUIs);
      scs.closeAndDispose();
   }

   private void sleep(long sleepMillis)
   {
      try
      {
         Thread.sleep(sleepMillis);
      }
      catch (InterruptedException e)
      {
      }
   }
}