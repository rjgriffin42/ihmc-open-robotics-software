package com.yobotics.simulationconstructionset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.AWTException;

import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.utilities.Axis;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;

import com.yobotics.simulationconstructionset.examples.FallingBrickRobot;
import com.yobotics.simulationconstructionset.gui.SimulationGUITestFixture;

public class SimulationConstructionSetTest
{
   @Test
   public void testSimulationConstructionSetUsingGUITestFixture() throws AWTException
   {
      FallingBrickRobot robot = new FallingBrickRobot();

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      YoVariableRegistry registryOne = new YoVariableRegistry("RegistryOne");
      EnumYoVariable<Axis> enumForTests = new EnumYoVariable<Axis>("enumForTests", registryOne, Axis.class);
      YoVariableRegistry registryTwo = new YoVariableRegistry("RegistryTwo");
      BooleanYoVariable booleanForTests = new BooleanYoVariable("booleanForTests", registryTwo);
      registryOne.addChild(registryTwo);
      scs.addYoVariableRegistry(registryOne);

      scs.setFrameMaximized();
      scs.startOnAThread();
      scs.setSimulateDuration(2.0);


      SimulationGUITestFixture testFixture = new SimulationGUITestFixture(scs);

      testFixture.removeAllGraphs();
      testFixture.removeAllEntryBoxes();

      testFixture.selectNameSpaceTab();
      testFixture.selectNameSpace("root/RegistryOne");
      testFixture.selectVariableInOpenTab("enumForTests");
      ThreadTools.sleep(500);

      testFixture.selectNameSpaceTab();
      testFixture.selectNameSpace("root/RegistryOne/RegistryTwo");
      testFixture.selectVariableInOpenTab("booleanForTests");
      ThreadTools.sleep(500);

      testFixture.clickOnUnusedEntryBox();

      assertTrue(booleanForTests.getBooleanValue() == false);
      testFixture.findEntryBoxAndEnterValue("booleanForTests", 1.0);
      assertTrue(booleanForTests.getBooleanValue() == true);


      testFixture.selectSearchTab();
      testFixture.enterSearchText("q_");

      testFixture.selectVariableInSearchTab("q_y");
      testFixture.clickNewGraphButton();
      testFixture.middleClickInEmptyGraph();

      testFixture.selectVariableInSearchTab("q_z");
      testFixture.clickNewGraphButton();
      testFixture.middleClickInEmptyGraph();

      testFixture.removeAllGraphs();


      // Setup a few entry boxes:
      enumForTests.set(Axis.X);

      testFixture.selectSearchTab();
      testFixture.deleteSearchText();
      testFixture.enterSearchText("enumForTests");
      testFixture.selectVariableInSearchTab("enumForTests");

      testFixture.clickOnUnusedEntryBox();

      assertTrue(enumForTests.getEnumValue() == Axis.X);
      testFixture.findEnumEntryBoxAndSelectValue("enumForTests", "Z");
      assertTrue(enumForTests.getEnumValue() == Axis.Z);

      // Search for variables, change their values, and plot them:
//    testFixture.selectNameSpaceTab();
//    ThreadTools.sleep(1000);

      testFixture.selectSearchTab();

      testFixture.deleteSearchText();
      testFixture.enterSearchText("q_");

      testFixture.selectVariableInSearchTab("q_x");
      testFixture.clickRemoveEmptyGraphButton();
      testFixture.clickNewGraphButton();
      testFixture.middleClickInEmptyGraph();

      testFixture.clickNewGraphButton();
      testFixture.clickNewGraphButton();
      testFixture.selectVariableInSearchTab("q_y");
      testFixture.middleClickInNthGraph(2);
      ThreadTools.sleep(500);
      testFixture.selectVariableInSearchTab("q_z");
      testFixture.middleClickInNthGraph(2);

      testFixture.selectVariableAndSetValueInSearchTab("q_z", 1.31);
      DoubleYoVariable q_z = (DoubleYoVariable) scs.getVariable("q_z");
      assertEquals(1.31, q_z.getDoubleValue(), 1e-9);

      // Simulate and replay
      ThreadTools.sleep(500);
      testFixture.clickSimulateButton();
      ThreadTools.sleep(500);
      testFixture.clickStopButton();
      ThreadTools.sleep(500);
      testFixture.clickPlayButton();
      ThreadTools.sleep(500);
      testFixture.clickStopButton();
      ThreadTools.sleep(500);

      // Remove variables from graphs:
      testFixture.removeVariableFromNthGraph("q_y", 2);
      testFixture.clickRemoveEmptyGraphButton();


      // Go to In/out points, step through data. Add KeyPoints, Verify at the expected indices
      testFixture.clickGotoInPointButton();

      ThreadTools.sleep(100);

      int index = scs.getIndex();
      int inPoint = scs.getInPoint();
      assertEquals(index, inPoint);

      // Do some stepping forwards and putting in key points:
      int stepsForward = 4;
      for (int i = 0; i < stepsForward; i++)
      {
         testFixture.clickStepForwardButton();
      }

      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(stepsForward, index);
      testFixture.clickAddKeyPointButton();

      for (int i = 0; i < stepsForward; i++)
      {
         testFixture.clickStepForwardButton();
      }

      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(2 * stepsForward, index);
      testFixture.clickAddKeyPointButton();

      for (int i = 0; i < stepsForward; i++)
      {
         testFixture.clickStepForwardButton();
      }

      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(3 * stepsForward, index);
      testFixture.clickAddKeyPointButton();

      // Zoom in and out
      testFixture.clickZoomInButton();
      testFixture.clickZoomInButton();
      testFixture.clickZoomInButton();
      testFixture.clickZoomInButton();
      testFixture.clickZoomOutButton();

      testFixture.clickGotoInPointButton();
      testFixture.clickToggleKeyModeButton();

      testFixture.clickStepForwardButton();
      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(stepsForward, index);

      testFixture.clickStepForwardButton();
      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(2 * stepsForward, index);

      // Toggle a keypoint off:
      testFixture.clickAddKeyPointButton();

      testFixture.clickStepBackwardButton();
      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(stepsForward, index);

      testFixture.clickSetInPointButton();
      testFixture.clickStepForwardButton();
      testFixture.clickSetOutPointButton();

      testFixture.clickGotoInPointButton();
      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(stepsForward, index);

      testFixture.clickGotoOutPointButton();
      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(3 * stepsForward, index);
      testFixture.clickGotoInPointButton();

      testFixture.clickToggleKeyModeButton();
      testFixture.clickStepForwardButton();
      ThreadTools.sleep(100);
      index = scs.getIndex();
      assertEquals(stepsForward + 1, index);

      testFixture.closeAndDispose();
      scs.closeAndDispose();
      scs = null;
      testFixture = null;
   }

   @Test
   public void testSimulationConstructionSetNewGraphWindowUsingGUITestFixture() throws AWTException
   {
      FallingBrickRobot robot = new FallingBrickRobot();

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      scs.setDT(0.0001, 100);
      scs.setFrameMaximized();
      scs.startOnAThread();
      scs.setSimulateDuration(2.0);

      ThreadTools.sleep(2000);
      SimulationGUITestFixture testFixture = new SimulationGUITestFixture(scs);

      testFixture.closeAllGraphArrayWindows();

      testFixture.selectNewGraphWindowMenu();
      testFixture.selectNewGraphWindowMenu();

      testFixture.focusNthGraphArrayWindow(0);
      testFixture.clickNewGraphButton();

      testFixture.focusMainSCSWindow();
      testFixture.selectSearchTab();
      testFixture.deleteSearchText();
      testFixture.enterSearchText("q_");
      testFixture.selectVariableInSearchTab("q_z");

      testFixture.focusNthGraphArrayWindow(0);
      testFixture.middleClickInEmptyGraph();

      testFixture.focusMainSCSWindow();
      testFixture.selectSearchTab();
      testFixture.deleteSearchText();
      testFixture.enterSearchText("q_");
      testFixture.selectVariableInSearchTab("q_y");

      testFixture.focusNthGraphArrayWindow(1);
      testFixture.clickNewGraphButton();
      testFixture.middleClickInEmptyGraph();

      testFixture.closeAndDispose();
      scs.closeAndDispose();
      scs = null;
      testFixture = null;
   }
   
   @Ignore
   @Test
   public void testSimulationConstructionSetNewViewportWindowUsingGUITestFixture() throws AWTException
   {
      FallingBrickRobot robot = new FallingBrickRobot();

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      scs.setDT(0.0001, 100);
      scs.setFrameMaximized();
      scs.startOnAThread();
      scs.setSimulateDuration(2.0);

      ThreadTools.sleep(2000);
      SimulationGUITestFixture testFixture = new SimulationGUITestFixture(scs);
      
      testFixture.closeAllViewportWindows();
      testFixture.selectNewViewportWindowMenu();
      
      testFixture.focusNthViewportWindow(0);

      ThreadTools.sleepForever();
      
      testFixture.closeAndDispose();
      scs.closeAndDispose();
      scs = null;
      testFixture = null;

   }
   

   @Ignore
   @Test
   public void testSimulationConstructionSetMovieGenerationUsingGUITestFixture() throws AWTException
   {
      FallingBrickRobot robot = new FallingBrickRobot();

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      scs.setDT(0.0001, 100);
      scs.setFrameMaximized();
      scs.startOnAThread();
      scs.setSimulateDuration(2.0);

      SimulationGUITestFixture testFixture = new SimulationGUITestFixture(scs);
      testFixture.clickSimulateButton();
      ThreadTools.sleep(1000);
      
      testFixture.clickMediaCaptureButton();

      testFixture.focusDialog("Export Movie");
      testFixture.clickPlayButton();
      
      ThreadTools.sleepForever();

      testFixture.closeAndDispose();
      scs.closeAndDispose();
      scs = null;
      testFixture = null;
   }
}
