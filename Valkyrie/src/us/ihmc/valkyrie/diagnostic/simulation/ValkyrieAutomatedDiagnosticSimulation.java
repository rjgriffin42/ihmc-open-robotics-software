package us.ihmc.valkyrie.diagnostic.simulation;

import java.io.InputStream;
import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFHumanoidRobot;
import us.ihmc.darpaRoboticsChallenge.diagnostics.AutomatedDiagnosticConfiguration;
import us.ihmc.darpaRoboticsChallenge.diagnostics.AutomatedDiagnosticSimulationFactory;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel.RobotTarget;
import us.ihmc.sensorProcessing.diagnostic.DiagnosticParameters.DiagnosticEnvironment;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.util.virtualHoist.VirtualHoist;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.diagnostic.ValkyrieDiagnosticParameters;

public class ValkyrieAutomatedDiagnosticSimulation
{
   public ValkyrieAutomatedDiagnosticSimulation()
   {
      ValkyrieRobotModelWithHoist robotModel = new ValkyrieRobotModelWithHoist(RobotTarget.SCS, false);
      ValkyrieDiagnosticParameters diagnosticParameters = new ValkyrieDiagnosticParameters(DiagnosticEnvironment.RUNTIME_CONTROLLER, robotModel);

      AutomatedDiagnosticSimulationFactory simulationFactory = new AutomatedDiagnosticSimulationFactory(robotModel);

      InputStream gainStream = getClass().getClassLoader().getResourceAsStream("standPrep/simulationGains.yaml");
      InputStream setpointStream = getClass().getClassLoader().getResourceAsStream("standPrep/setpoints.yaml");

      simulationFactory.setGainStream(gainStream);
      simulationFactory.setSetpointStream(setpointStream);
      simulationFactory.setRobotInitialSetup(0.5, 0.0);
      simulationFactory.setDiagnosticParameters(diagnosticParameters);
      
      AutomatedDiagnosticConfiguration automatedDiagnosticConfiguration = simulationFactory.createDiagnosticController();
      automatedDiagnosticConfiguration.addWait(1.0);
//      automatedDiagnosticConfiguration.addJointCheckUpDiagnostic();
      automatedDiagnosticConfiguration.addPelvisIMUCheckUpDiagnostic();

      simulationFactory.startSimulation();
   }

   private class ValkyrieRobotModelWithHoist extends ValkyrieRobotModel
   {

      public ValkyrieRobotModelWithHoist(DRCRobotModel.RobotTarget target, boolean headless)
      {
         super(target, headless);
      }

      @Override
      public SDFHumanoidRobot createSdfRobot(boolean createCollisionMeshes)
      {
         SDFHumanoidRobot robot = super.createSdfRobot(createCollisionMeshes);

         Joint joint = robot.getJoint("torsoRoll");

         ArrayList<Vector3d> attachmentLocations = new ArrayList<Vector3d>();

         attachmentLocations.add(new Vector3d(0.0, 0.15, 0.412));
         attachmentLocations.add(new Vector3d(0.0, -0.15, 0.412));

         double updateDT = 0.0001;
         VirtualHoist virtualHoist = new VirtualHoist(joint, robot, attachmentLocations, updateDT);
         robot.setController(virtualHoist, 1);

         virtualHoist.turnHoistOn();
         virtualHoist.setTeepeeLocation(new Point3d(0.0, 0.0, 2.5));
         virtualHoist.setHoistStiffness(20000.0);
         virtualHoist.setHoistDamping(15000.0);

         return robot;
      }
   }

   public static void main(String[] args)
   {
      new ValkyrieAutomatedDiagnosticSimulation();
   }
}
