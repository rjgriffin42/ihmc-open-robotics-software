package us.ihmc.darpaRoboticsChallenge;

import java.awt.Component;

import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.controllers.ControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.FlatGroundWalkingHighLevelHumanoidControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.HighLevelHumanoidControllerFactory;
import us.ihmc.commonWalkingControlModules.terrain.TerrainType;
import us.ihmc.darpaRoboticsChallenge.controllers.DRCRobotMomentumBasedControllerFactory;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.initialSetup.SquaredUpDRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicCheckBoxMenuItem;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.inputdevices.MidiSliderBoard;

public class DRCFlatGroundWalkingTrack
{
   private final DRCSimulation drcSimulation;
   private final DRCDemo01Environment environment;

   public DRCFlatGroundWalkingTrack(DRCGuiInitialSetup guiInitialSetup, AutomaticSimulationRunner automaticSimulationRunner, double timePerRecordTick,
                   int simulationDataBufferSize, boolean doChestOrientationControl, boolean showInstructions)
   {
      DRCSCSInitialSetup scsInitialSetup;
      DRCRobotInitialSetup drcRobotInitialSetup;

      drcRobotInitialSetup = new SquaredUpDRCRobotInitialSetup();
      
      
      
      scsInitialSetup = new DRCSCSInitialSetup(TerrainType.FLAT);
      scsInitialSetup.setSimulationDataBufferSize(simulationDataBufferSize);

      double dt = scsInitialSetup.getDT();
      int recordFrequency = (int) Math.round(timePerRecordTick / dt);
      if (recordFrequency < 1)
         recordFrequency = 1;
      scsInitialSetup.setRecordFrequency(recordFrequency);



      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
      YoVariableRegistry registry = new YoVariableRegistry("adjustableParabolicTrajectoryDemoSimRegistry");

      double desiredCoMHeight = 0.9;
      HighLevelHumanoidControllerFactory highLevelHumanoidControllerFactory = new FlatGroundWalkingHighLevelHumanoidControllerFactory(desiredCoMHeight);
      ControllerFactory controllerFactory = new DRCRobotMomentumBasedControllerFactory(highLevelHumanoidControllerFactory);

      environment = new DRCDemo01Environment();

//      r2Simulation = new R2Simulation(environment, r2InitialSetup, sensorNoiseInitialSetup, controllerFactory, scsInitialSetup, guiInitialSetup);
      drcSimulation = new DRCSimulation(drcRobotInitialSetup, controllerFactory, scsInitialSetup, guiInitialSetup);

      SimulationConstructionSet simulationConstructionSet = drcSimulation.getSimulationConstructionSet();
      MidiSliderBoard sliderBoard = new MidiSliderBoard(simulationConstructionSet);
      int i = 1;

      // TODO: get these from CommonAvatarUserInterface once it exists:
      sliderBoard.setSlider(i++, "desiredICPParameterX", getSimulationConstructionSet(), 0.0, 1.0);
      sliderBoard.setSlider(i++, "desiredICPParameterY", getSimulationConstructionSet(), 0.0, 1.0);
      sliderBoard.setSlider(i++, "desiredHeadingFinal", getSimulationConstructionSet(), Math.toRadians(-30.0), Math.toRadians(30.0));
      sliderBoard.setSlider(i++, "desiredPelvisPitch", getSimulationConstructionSet(), Math.toRadians(-20.0), Math.toRadians(20.0));
      sliderBoard.setSlider(i++, "desiredPelvisRoll", getSimulationConstructionSet(), Math.toRadians(-20.0), Math.toRadians(20.0));
      sliderBoard.setSlider(i++, "desiredCenterOfMassHeightFinal", getSimulationConstructionSet(), 0.42, 1.5);

      setUpJoyStick(getSimulationConstructionSet());

      // add other registries
      drcSimulation.addAdditionalDynamicGraphicObjectsListRegistries(dynamicGraphicObjectsListRegistry);
      drcSimulation.addAdditionalYoVariableRegistriesToSCS(registry);

      simulationConstructionSet.setCameraPosition(6.0, -2.0, 4.5);
      simulationConstructionSet.setCameraFix(-0.44, -0.17, 0.75);
      hideDynamicGraphicsForActualDemo(showInstructions);

      setUpInstructionsBoard(simulationConstructionSet, showInstructions);

      if (automaticSimulationRunner != null)
      {
         drcSimulation.start(automaticSimulationRunner);
      }
      else
      {
         drcSimulation.start(null);
      }
   }

   private void hideDynamicGraphicsForActualDemo(boolean showInstructions)
   {
      if (showInstructions)
      {
         Component[] components = drcSimulation.getSimulationConstructionSet().getDynamicGraphicMenuManager().getjMenu().getMenuComponents();
         for (int j = 0; j < components.length; j++)
         {
            Component component = components[j];
            if (component instanceof DynamicGraphicCheckBoxMenuItem)
            {
               DynamicGraphicCheckBoxMenuItem jCheckBox = (DynamicGraphicCheckBoxMenuItem) component;
               if (jCheckBox.getText().equals("trajectoryEndPoint"))
               {
                  jCheckBox.setSelected(true);
               }
               else
               {
                  jCheckBox.setSelected(false);
               }
            }
         }
      }
   }

   private void setUpJoyStick(SimulationConstructionSet simulationConstructionSet)
   {
      try
      {
         new DRCJoystickController(simulationConstructionSet);
      }
      catch (RuntimeException e)
      {
         System.out.println("Could not connect to joystick");
      }
   }

   public static void main(String[] args) throws JSAPException
   {
      DRCDemo0ArgumentParser drcDemo0ArgumentParser = new DRCDemo0ArgumentParser(args);

      boolean showInstructions = drcDemo0ArgumentParser.getShowInstructions();

      AutomaticSimulationRunner automaticSimulationRunner = null;

      

      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup();

      new DRCFlatGroundWalkingTrack(guiInitialSetup, automaticSimulationRunner, 0.005, 16000, true, showInstructions);
   }


   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return drcSimulation.getSimulationConstructionSet();
   }

   public DRCDemo01Environment getEnvironment()
   {
      return environment;
   }

   private void setUpInstructionsBoard(SimulationConstructionSet scs, boolean showInstructions)
   {
      if (showInstructions)
      {
         if (showInstructions)
         {
            Graphics3DObject linkGraphics = new Graphics3DObject();
            linkGraphics.translate(new Vector3d(-1.0, -1.0, 0.0));
            linkGraphics.rotate(Math.toRadians(90), Graphics3DObject.Z);
            linkGraphics.scale(0.2);
            linkGraphics.addModelFile(getClass().getResource("3ds/billboard.3DS"));
            scs.addStaticLinkGraphics(linkGraphics);
         }
      }
   }
}
