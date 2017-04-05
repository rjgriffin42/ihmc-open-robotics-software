package us.ihmc.exampleSimulations.controllerCore.robotArmWithFixedBase;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.simulationConstructionSetTools.util.inputdevices.MidiSliderBoard;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;

public class RobotArmSimulation
{
   public static void main(String[] args)
   {
      double controlDT = 5.0e-5;
      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

      RobotArm robotArm = new RobotArm();
      WholeBodyControllerCoreMode controlMode = WholeBodyControllerCoreMode.INVERSE_DYNAMICS;

      if (controlMode == WholeBodyControllerCoreMode.INVERSE_KINEMATICS)
         robotArm.setDynamic(false);

      RobotArmController robotArmController = new RobotArmController(robotArm, controlDT, controlMode, yoGraphicsListRegistry);
      robotArm.setController(robotArmController);

      SimulationConstructionSetParameters parameters = new SimulationConstructionSetParameters();
      parameters.setDataBufferSize((int) Math.pow(2, 16)); // => 65536
      SimulationConstructionSet scs = new SimulationConstructionSet(robotArm, parameters);
      setupSliderBoard(scs);
      scs.setFastSimulate(true, 15);
      scs.addYoGraphicsListRegistry(yoGraphicsListRegistry, true);
      scs.setDT(controlDT, 10);
      scs.startOnAThread();
   }

   private static void setupSliderBoard(SimulationConstructionSet scs)
   {
      MidiSliderBoard midiSliderBoard = new MidiSliderBoard(scs);
      int sliderIndex = 1;
      midiSliderBoard.setSlider(sliderIndex++, "handTargetX", scs, -1.0, 1.0);
      midiSliderBoard.setSlider(sliderIndex++, "handTargetY", scs, -1.0, 1.0);
      midiSliderBoard.setSlider(sliderIndex++, "handTargetZ", scs, -1.0, 1.0);
      midiSliderBoard.setSlider(sliderIndex++, "handTargetYaw", scs, -Math.PI, Math.PI);
      midiSliderBoard.setSlider(sliderIndex++, "handTargetPitch", scs, -Math.PI, Math.PI);
      midiSliderBoard.setSlider(sliderIndex++, "handTargetRoll", scs, -Math.PI, Math.PI);

      int buttonIndex = 1;
      midiSliderBoard.setButton(buttonIndex++, "goToTarget", scs);

      buttonIndex = 9; // Second row
      midiSliderBoard.setButton(buttonIndex++, "controlLinearX", scs);
      midiSliderBoard.setButton(buttonIndex++, "controlLinearY", scs);
      midiSliderBoard.setButton(buttonIndex++, "controlLinearZ", scs);
      midiSliderBoard.setButton(buttonIndex++, "controlAngularX", scs);
      midiSliderBoard.setButton(buttonIndex++, "controlAngularY", scs);
      midiSliderBoard.setButton(buttonIndex++, "controlAngularZ", scs);
   }
}
