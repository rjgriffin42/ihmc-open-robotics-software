package us.ihmc.darpaRoboticsChallenge;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.visualization.SliderBoardFactory;
import us.ihmc.atlas.visualization.WalkControllerSliderBoard;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.ControllerFactory;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepTimingParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.PolyvalentHighLevelHumanoidControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.HighLevelState;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.InverseDynamicsJointController;
import us.ihmc.darpaRoboticsChallenge.controllers.DRCRobotMomentumBasedControllerFactory;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.ExternalForcePoint;
import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.GainCalculator;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;

public class DRCInverseDynamicsControllerDemo
{
   private static final DRCRobotModel defaultModelForGraphicSelector = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT);

   private static final double ROBOT_FLOATING_HEIGHT = 0.3;

   private static final HighLevelState INVERSE_DYNAMICS_JOINT_CONTROL = HighLevelState.INVERSE_DYNAMICS_JOINT_CONTROL;
   
   private final HumanoidRobotSimulation<SDFRobot> drcSimulation;
   private final DRCController drcController;

   public DRCInverseDynamicsControllerDemo(DRCRobotInterface robotInterface, DRCRobotInitialSetup<SDFRobot> robotInitialSetup, DRCGuiInitialSetup guiInitialSetup,
                                    DRCSCSInitialSetup scsInitialSetup, AutomaticSimulationRunner automaticSimulationRunner,
                                    double timePerRecordTick, int simulationDataBufferSize, DRCRobotModel model)
   {
      scsInitialSetup.setSimulationDataBufferSize(simulationDataBufferSize);

      double dt = scsInitialSetup.getDT();
      int recordFrequency = (int) Math.round(timePerRecordTick / dt);
      if (recordFrequency < 1)
         recordFrequency = 1;
      scsInitialSetup.setRecordFrequency(recordFrequency);

      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry;
      if (guiInitialSetup.isGuiShown())
         dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry(false);
      else
         dynamicGraphicObjectsListRegistry = null;
      YoVariableRegistry registry = new YoVariableRegistry("adjustableParabolicTrajectoryDemoSimRegistry");
      
      WalkingControllerParameters walkingControlParameters = model.getWalkingControlParamaters();
      ArmControllerParameters armControlParameters = model.getArmControllerParameters();

      FootstepTimingParameters footstepTimingParameters = FootstepTimingParameters.createForFastWalkingInSimulation(walkingControlParameters);
      
      PolyvalentHighLevelHumanoidControllerFactory highLevelHumanoidControllerFactory = new PolyvalentHighLevelHumanoidControllerFactory(null,
            footstepTimingParameters, walkingControlParameters, walkingControlParameters, armControlParameters, false, false, false, INVERSE_DYNAMICS_JOINT_CONTROL);

      SideDependentList<String> footForceSensorNames = new SideDependentList<>();
      for(RobotSide robotSide : RobotSide.values)
      {
         footForceSensorNames.put(robotSide, robotInterface.getJointMap().getJointBeforeFootName(robotSide));
      }
      
      ControllerFactory controllerFactory = new DRCRobotMomentumBasedControllerFactory(highLevelHumanoidControllerFactory, DRCConfigParameters.contactTresholdForceForSCS, footForceSensorNames);
      Pair<HumanoidRobotSimulation<SDFRobot>, DRCController> humanoidSimulation = DRCSimulationFactory.createSimulation(controllerFactory, null,
            robotInterface, robotInitialSetup, scsInitialSetup, guiInitialSetup, null, null, dynamicGraphicObjectsListRegistry, model);
      drcSimulation = humanoidSimulation.first();
      drcController = humanoidSimulation.second();

      // add other registries
      drcSimulation.addAdditionalYoVariableRegistriesToSCS(registry);
      
      
      HoldRobotInTheAir controller = new HoldRobotInTheAir(drcSimulation.getRobot(), drcSimulation.getSimulationConstructionSet(), drcController.getControllerModel());
      drcSimulation.getRobot().setController(controller);
      controller.initialize();
      
      new InverseDynamicsJointController.GravityCompensationSliderBoard(getSimulationConstructionSet(), drcController.getControllerModel(), getSimulationConstructionSet().getRootRegistry());
      
      if (automaticSimulationRunner != null)
      {
         drcSimulation.start(automaticSimulationRunner);
      }
      else
      {
         drcSimulation.start(null);
      }
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return drcSimulation.getSimulationConstructionSet();
   }

   public DRCController getDrcController()
   {
      return drcController;
   }

   public static void main(String[] args) throws JSAPException
   {
      DRCRobotModel model = null;
      
      model = DRCRobotModelFactory.selectModelFromFlag(args);
      
      if (model == null)
         model = DRCRobotModelFactory.selectModelFromGraphicSelector(defaultModelForGraphicSelector);
      
      if (model == null)
         throw new RuntimeException("No robot model selected");
      
      AutomaticSimulationRunner automaticSimulationRunner = null;

      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false, sliderBoardFactory);

      DRCRobotInterface robotInterface = new PlainDRCRobot(model);
      
      final double groundHeight = 0.0;
      GroundProfile groundProfile = new FlatGroundProfile(groundHeight);

      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT());
      scsInitialSetup.setDrawGroundProfile(true);
      scsInitialSetup.setInitializeEstimatorToActual(true);
      
      double initialYaw = 0.0;
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = model.getDefaultRobotInitialSetup(groundHeight + ROBOT_FLOATING_HEIGHT, initialYaw);

      new DRCInverseDynamicsControllerDemo(robotInterface, robotInitialSetup, guiInitialSetup, scsInitialSetup, automaticSimulationRunner,
            DRCConfigParameters.CONTROL_DT, 16000, model);
   }

   private class HoldRobotInTheAir implements RobotController
   {
      private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
      
      private final ArrayList<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      private final ArrayList<Vector3d> efp_offsetFromRootJoint = new ArrayList<>();
      private final double dx = 0.05, dy = 0.12, dz = 0.4;
      
      private final ArrayList<Vector3d> initialPositions = new ArrayList<>();

      private final DoubleYoVariable holdPelvisKp = new DoubleYoVariable("holdPelvisKp", registry);
      private final DoubleYoVariable holdPelvisKv = new DoubleYoVariable("holdPelvisKv", registry);
      private final double robotMass, robotWeight;
      
      private final SDFRobot robot;
      
      private final DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry(true);
      private final ArrayList<DynamicGraphicPosition> efp_positionViz = new ArrayList<>();
      
      public HoldRobotInTheAir(SDFRobot robot, SimulationConstructionSet scs, SDFFullRobotModel sdfFullRobotModel)
      {
         this.robot = robot;
         robotMass = robot.computeCenterOfMass(new Point3d());
         robotWeight = robotMass * Math.abs(robot.getGravityZ());
         
         Joint jointToAddExternalForcePoints;
         try
         {
            String lastSpineJointName = sdfFullRobotModel.getChest().getParentJoint().getName();
            jointToAddExternalForcePoints = robot.getJoint(lastSpineJointName);
         }
         catch(NullPointerException e)
         {
            System.err.println("No chest or spine found. Stack trace:");
            e.printStackTrace();
            
            jointToAddExternalForcePoints = robot.getPelvisJoint();
         }
         
         holdPelvisKp.set(5000.0);
         holdPelvisKv.set(GainCalculator.computeDampingForSecondOrderSystem(robotMass, holdPelvisKp.getDoubleValue(), 1.0));

         efp_offsetFromRootJoint.add(new Vector3d(dx, dy, dz));
         efp_offsetFromRootJoint.add(new Vector3d(dx, -dy, dz));
         efp_offsetFromRootJoint.add(new Vector3d(-dx, dy, dz));
         efp_offsetFromRootJoint.add(new Vector3d(-dx, -dy, dz));
         
         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
         {
            initialPositions.add(new Vector3d());
            
            String linkName = jointToAddExternalForcePoints.getLink().getName();
            ExternalForcePoint efp = new ExternalForcePoint("efp_" + linkName + "_" + String.valueOf(i) + "_", efp_offsetFromRootJoint.get(i), robot);
            externalForcePoints.add(efp);
            jointToAddExternalForcePoints.addExternalForcePoint(efp);
            
            efp_positionViz.add(efp.getYoPosition().createDynamicGraphicPosition(efp.getName(), 0.05, YoAppearance.Red()));
         }
         
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjects("EFP", efp_positionViz);
         dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      }

      @Override
      public void initialize()
      {
         robot.update();
         
         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
         {
            externalForcePoints.get(i).getYoPosition().getPoint3d(initialPositions.get(i));
            efp_positionViz.get(i).update();
         }
         
         doControl();
      }

      private final Vector3d proportionalTerm = new Vector3d();
      private final Vector3d derivativeTerm = new Vector3d();
      private final Vector3d pdControlOutput = new Vector3d();
      
      @Override
      public void doControl()
      {
         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
         {
            ExternalForcePoint efp = externalForcePoints.get(i);
            efp.getYoPosition().getPoint3d(proportionalTerm);
            proportionalTerm.sub(initialPositions.get(i));
            proportionalTerm.scale(-holdPelvisKp.getDoubleValue());
            
            efp.getYoVelocity().get(derivativeTerm);
            derivativeTerm.scale(- holdPelvisKv.getDoubleValue());
            
            pdControlOutput.add(proportionalTerm, derivativeTerm);
            
            efp.setForce(pdControlOutput);
            efp.fz.add(robotWeight / efp_offsetFromRootJoint.size());

            efp_positionViz.get(i).update();
         }
      }

      @Override
      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      @Override
      public String getName()
      {
         return registry.getName();
      }

      @Override
      public String getDescription()
      {
         return getName();
      }
   }
}
