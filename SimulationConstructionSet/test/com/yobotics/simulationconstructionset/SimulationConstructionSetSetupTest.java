package com.yobotics.simulationconstructionset;

import javax.swing.JWindow;

import org.junit.Test;

import com.yobotics.simulationconstructionset.gui.SplashPanel;

public class SimulationConstructionSetSetupTest
{
   private static final int pauseTimeForGUIs = 5000;
   


   @Test
   public void testSplashScreen()
   {
      SplashPanel splashPanel = new SplashPanel();
      JWindow window = splashPanel.showSplashScreen();

      sleep(pauseTimeForGUIs);
      window.dispose();
   }

   @Test
   public void testSimulationConstructionSetWithoutARobot()
   {
      SimulationConstructionSet scs = new SimulationConstructionSet();
      Thread thread = new Thread(scs);
      thread.start();

      sleep(pauseTimeForGUIs);
      scs.closeAndDispose();
   }
   
   @Test
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
      } catch (InterruptedException e)
      {
      }
   }



}
