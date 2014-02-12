package us.ihmc.darpaRoboticsChallenge;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.ControllerFactory;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.MultiContactTestHumanoidControllerFactory;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.darpaRoboticsChallenge.controllers.DRCRobotMomentumBasedControllerFactory;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.MultiContactDRCRobotInitialSetup;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.inputdevices.MidiSliderBoard;

public class DRCMultiContact
{
   private final HumanoidRobotSimulation<SDFRobot> drcSimulation;
   private final MultiContactTestEnvironment environment;

   public DRCMultiContact(DRCRobotModel robotModel, DRCGuiInitialSetup guiInitialSetup, AutomaticSimulationRunner automaticSimulationRunner,
                          double timePerRecordTick, int simulationDataBufferSize)
   {
       DRCRobotParameters.ENABLE_JOINT_VELOCITY_TORQUE_LIMITS = false; // doesn't work with joint torque limits

      DRCSCSInitialSetup scsInitialSetup;


      RobotSide[] footContactSides = RobotSide.values;
      RobotSide[] handContactSides = new RobotSide[]{RobotSide.LEFT};
      RobotInitialSetup<SDFRobot> robotInitialSetup = new MultiContactDRCRobotInitialSetup();

//      RobotSide[] footContactSides = RobotSide.values;
//      RobotSide[] handContactSides = RobotSide.values;
//      RobotInitialSetup<SDFRobot> robotInitialSetup = new PushUpDRCRobotInitialSetup();

      DRCRobotJointMap jointMap = new DRCRobotJointMap(robotModel, true);

      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      environment = new MultiContactTestEnvironment(robotInitialSetup, jointMap, dynamicGraphicObjectsListRegistry, footContactSides, handContactSides);
      final PlainDRCRobot robotInterface = new PlainDRCRobot(robotModel, false);
      scsInitialSetup = new DRCSCSInitialSetup(environment, robotInterface.getSimulateDT());
      scsInitialSetup.setSimulationDataBufferSize(simulationDataBufferSize);

      double dt = scsInitialSetup.getDT();
      int recordFrequency = (int) Math.round(timePerRecordTick / dt);
      if (recordFrequency < 1)
         recordFrequency = 1;
      scsInitialSetup.setRecordFrequency(recordFrequency);

      SideDependentList<String> namesOfJointsBeforeHands = new SideDependentList<String>();
      SideDependentList<Transform3D> handContactPointTransforms = new SideDependentList<Transform3D>();
      SideDependentList<List<Point2d>> handContactPoints = new SideDependentList<List<Point2d>>();
      for (RobotSide robotSide : RobotSide.values)
      {
         namesOfJointsBeforeHands.put(robotSide, jointMap.getNameOfJointBeforeHand(robotSide));
         handContactPointTransforms.put(robotSide, DRCRobotParameters.invisibleContactablePlaneHandContactPointTransforms.get(robotSide));
         handContactPoints.put(robotSide, DRCRobotParameters.invisibleContactablePlaneHandContactPoints.get(robotSide));
      }

      WalkingControllerParameters controllerParameters = new DRCRobotMultiContactControllerParameters(){
         public Map<OneDoFJoint, Double> getDefaultArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
         {
            Map<OneDoFJoint, Double> jointPositions = new LinkedHashMap<OneDoFJoint, Double>();
            EnumMap<ArmJointName, Double> defaultArmPosition = MultiContactDRCRobotInitialSetup.getDefaultArmPositionForMultiContactSimulation().get(robotSide);

            for (ArmJointName armJointName : defaultArmPosition.keySet())
            {
               double position = defaultArmPosition.get(armJointName);
               OneDoFJoint joint = fullRobotModel.getArmJoint(robotSide, armJointName);
               jointPositions.put(joint, position);
            }

            return jointPositions;
         }
      };
      
      ArmControllerParameters armControllerParameters = new DRCRobotArmControllerParameters();


      MultiContactTestHumanoidControllerFactory highLevelHumanoidControllerFactory = new MultiContactTestHumanoidControllerFactory(namesOfJointsBeforeHands,
            handContactPointTransforms, handContactPoints, footContactSides, handContactSides, controllerParameters, armControllerParameters);
      ControllerFactory controllerFactory = new DRCRobotMomentumBasedControllerFactory(highLevelHumanoidControllerFactory, DRCConfigParameters.contactTresholdForceForSCS);

      Pair<HumanoidRobotSimulation<SDFRobot>, DRCController> humanoidSimulation = DRCSimulationFactory.createSimulation(controllerFactory, environment, robotInterface, robotInitialSetup, scsInitialSetup, guiInitialSetup, null, null, dynamicGraphicObjectsListRegistry);
      drcSimulation = humanoidSimulation.first();

      SimulationConstructionSet simulationConstructionSet = drcSimulation.getSimulationConstructionSet();

      
      MidiSliderBoard sliderBoard = new MidiSliderBoard(simulationConstructionSet);
      sliderBoard.setSlider(1, "desiredCoMX", simulationConstructionSet, -0.2, 0.2);
      sliderBoard.setSlider(2, "desiredCoMY", simulationConstructionSet, -0.2, 0.2);
      sliderBoard.setSlider(3, "desiredCoMZ", simulationConstructionSet, -0.5, 1.2);
      sliderBoard.setSlider(4, "desiredPelvisYaw", simulationConstructionSet, -Math.PI / 8.0, Math.PI / 8.0);
      sliderBoard.setSlider(5, "desiredPelvisPitch", simulationConstructionSet, -Math.PI, Math.PI);
      sliderBoard.setSlider(6, "desiredPelvisRoll", simulationConstructionSet, -Math.PI / 8.0, Math.PI / 8.0);
      sliderBoard.setKnob(1, "desiredChestOrientationYaw", simulationConstructionSet, -Math.PI / 8.0, Math.PI / 8.0);
      sliderBoard.setKnob(2, "desiredChestOrientationPitch", simulationConstructionSet, -Math.PI / 8.0, Math.PI / 8.0);
      sliderBoard.setKnob(3, "desiredChestOrientationRoll", simulationConstructionSet, -Math.PI / 8.0, Math.PI / 8.0);



      simulationConstructionSet.setCameraPosition(6.0, -2.0, 4.5);
      simulationConstructionSet.setCameraFix(-0.44, -0.17, 0.75);

      if (automaticSimulationRunner != null)
      {
         drcSimulation.start(automaticSimulationRunner);
      }
      else
      {
         drcSimulation.start(null);
      }
   }

   public static void main(String[] args) throws JSAPException
   {
      AutomaticSimulationRunner automaticSimulationRunner = null;

      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(false, true);

      double timePerRecordTick = DRCConfigParameters.CONTROL_DT;
      int simulationDataBufferSize = 16000;
      new DRCMultiContact(DRCRobotModel.ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS, guiInitialSetup, automaticSimulationRunner, timePerRecordTick,
                          simulationDataBufferSize);
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return drcSimulation.getSimulationConstructionSet();
   }
}
