package us.ihmc.valkyrie.simulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.DiagnosticsWhenHangingHelper;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.virtualHoist.VirtualHoist;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.valkyrie.DiagnosticsWhenHangingController;
import us.ihmc.valkyrie.HumanoidDiagnosticsWhenHangingSimulation;
import us.ihmc.valkyrie.HumanoidJointPoseList;
import us.ihmc.valkyrie.ValkyrieInitialSetup;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.configuration.ValkyrieConfigurationRoot;
import us.ihmc.valkyrie.kinematics.transmissions.InefficientPushRodTransmission;
import us.ihmc.valkyrie.kinematics.transmissions.PushRodTransmissionJoint;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class ValkyrieDiagnosticsWhenHangingSimulation
{
   private final DiagnosticsWhenHangingController diagnosticsWhenHangingController;
   
   public ValkyrieDiagnosticsWhenHangingSimulation()
   {
      DRCRobotModel robotModel = new ValkyrieRobotModelWithHoist(false, false);
      double groundZ = 0.0;
      double initialYaw = 0.0;
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = new ValkyrieInitialSetup(groundZ, initialYaw);
      
      HumanoidJointPoseList humanoidJointPoseList = new HumanoidJointPoseList();
      boolean robotIsHanging = true;
      HumanoidDiagnosticsWhenHangingSimulation humanoidDiagnosticsWhenHangingSimulation = new HumanoidDiagnosticsWhenHangingSimulation(humanoidJointPoseList, ValkyrieConfigurationRoot.VALKYRIE_WITH_ARMS, robotIsHanging, robotModel, robotInitialSetup);
      humanoidDiagnosticsWhenHangingSimulation.rememberCorruptorVariableValues();

      
      diagnosticsWhenHangingController = humanoidDiagnosticsWhenHangingSimulation.getDiagnosticsWhenHangingController();
      SimulationConstructionSet simulationConstructionSet = humanoidDiagnosticsWhenHangingSimulation.getSimulationConstructionSet();
      
//      loadUpperBodyDataAndDoSomeOptimizationTests(humanoidDiagnosticsWhenHangingSimulation);
      //loadArmDataAndDoSomeOptimizationTests(humanoidDiagnosticsWhenHangingSimulation);
//      loadLegDataAndDoSomeOptimizationTests(humanoidDiagnosticsWhenHangingSimulation);
      
      
      humanoidDiagnosticsWhenHangingSimulation.updateDataAndComputeTorqueOffsetsBasedOnAverages();
      
      PrintTorqueOffsetsButton printTorqueOffsetsButton = new PrintTorqueOffsetsButton();
      simulationConstructionSet.addButton(printTorqueOffsetsButton);
   }
   
   
   private class PrintTorqueOffsetsButton extends JButton implements ActionListener
   {
      private static final long serialVersionUID = 262981153765265286L;
      
      public PrintTorqueOffsetsButton()
      {
         super("PrintTorqueOffsets");
         
         this.addActionListener(this);
      }

      @Override
      public void actionPerformed(ActionEvent e)
      {
         printOffsetsForCoeffsForValkyrie();
      }

   }
   
   
   
   // Don't Delete! Fix this for Valkyrie for printing out the coeffs when the torque offsets are changed. 
   // Make it be more general, just looking at the tau_off variables...
   public void printOffsetsForCoeffsForValkyrie()
   {
      java.text.NumberFormat doubleFormat = new java.text.DecimalFormat(" 0.00;-0.00");

      System.out.println();

      ArrayList<OneDoFJoint> oneDoFJoints = diagnosticsWhenHangingController.getOneDoFJoints();
      
      for (OneDoFJoint oneDoFJoint : oneDoFJoints)
      {
         DiagnosticsWhenHangingHelper diagnosticsWhenHangingHelper = diagnosticsWhenHangingController.getDiagnosticsWhenHangingHelper(oneDoFJoint);

         if (diagnosticsWhenHangingHelper != null)
         {
            double torqueOffset = diagnosticsWhenHangingHelper.getTorqueOffset();
            double torqueOffsetSign = diagnosticsWhenHangingController.getTorqueOffsetSign(oneDoFJoint);

            double signedTorqueOffset = torqueOffset * torqueOffsetSign;

            String offsetString = doubleFormat.format(signedTorqueOffset);
            System.out.println(oneDoFJoint.getName() + " torque offset = " + offsetString);
         }
      }

      System.out.println();

      double reflectTop = 1.0;
      double reflectBottom = -1.0;
      boolean topJointFirst = true;

      InefficientPushRodTransmission leftAnkleTransmission = new InefficientPushRodTransmission(PushRodTransmissionJoint.ANKLE, reflectTop, reflectBottom,
                                                                topJointFirst, null, null);

      reflectBottom = 1.0;
      InefficientPushRodTransmission rightAnkleTransmission = new InefficientPushRodTransmission(PushRodTransmissionJoint.ANKLE, reflectTop, reflectBottom,
                                                                 topJointFirst, null, null);

      double[] leftAnkleForceOffsets = leftAnkleTransmission.jointToActuatorEffortAtZero(diagnosticsWhenHangingController.getAnkleTorqueOffsets(RobotSide.LEFT));
      double[] rightAnkleForceOffsets = rightAnkleTransmission.jointToActuatorEffortAtZero(diagnosticsWhenHangingController.getAnkleTorqueOffsets(RobotSide.RIGHT));

      System.out.println("\nLeft ankle J5 force offset = " + doubleFormat.format(-leftAnkleForceOffsets[0]));
      System.out.println("Left ankle J6 force offset = " + doubleFormat.format(-leftAnkleForceOffsets[1]));

      System.out.println("\nRight ankle J5 force offset = " + doubleFormat.format(-rightAnkleForceOffsets[0]));
      System.out.println("Right ankle J6 force offset = " + doubleFormat.format(-rightAnkleForceOffsets[1]));

      reflectTop = -1.0;
      reflectBottom = 1.0;
      topJointFirst = false;

      InefficientPushRodTransmission waistTransmission = new InefficientPushRodTransmission(PushRodTransmissionJoint.WAIST, reflectTop, reflectBottom,
                                                            topJointFirst, null, null);

      double[] waistForceOffsets = waistTransmission.jointToActuatorEffortAtZero(diagnosticsWhenHangingController.getWaistTorqueOffsets());

      // TODO: Need to figure out why this is not seeming to work correctly...
      System.out.println("\nWaist J2 force offset = " + doubleFormat.format(waistForceOffsets[0]));
      System.out.println("Waist J3 force offset = " + doubleFormat.format(-waistForceOffsets[1]));

   }
   
   private void loadDataAndDoSomeOptimizationTests(HumanoidDiagnosticsWhenHangingSimulation humanoidDiagnosticsWhenHangingSimulation)
   {
      SimulationConstructionSet simulationConstructionSet = humanoidDiagnosticsWhenHangingSimulation.getSimulationConstructionSet();
      simulationConstructionSet.readData("D:/20141204_091350_Valkyrie_VerySlowArmMotionsWithMorePoses_Processed.data.gz");

      humanoidDiagnosticsWhenHangingSimulation.restoreCorruptorVariableValues();

      //      setInitialCorruptorArmMassValues(simulationConstructionSet);
      //      setInitialCorruptorArmCoMOffsetValues(simulationConstructionSet);
      setInitialCorruptorArmTorqueOffsetValues(simulationConstructionSet);

      //      humanoidDiagnosticsWhenHangingSimulation.setCorruptorVariableValuesToOptimizeToZero();


      String side = "Right";

      // Forearm only:
      String[] containsToOptimizeCoM = new String[]{side + "ForearmCoM"};
      String[] containsToOptimizeTorqueScore = new String[]{""};

      //    String[] containsToOptimizeCoM = new String[]{side + "ShoulderRotatorCoM", side + "ShoulderAdductorCoM", side + "ForearmCoM"};
      //    String[] containsToOptimizeTorqueOffset = new String[]{side + "Shoulder", side + "Elbow"};
      humanoidDiagnosticsWhenHangingSimulation.setVariablesToOptimize(containsToOptimizeCoM, containsToOptimizeTorqueScore);

   }
   
   private void loadLegDataAndDoSomeOptimizationTests(HumanoidDiagnosticsWhenHangingSimulation humanoidDiagnosticsWhenHangingSimulation)
   {
      SimulationConstructionSet simulationConstructionSet = humanoidDiagnosticsWhenHangingSimulation.getSimulationConstructionSet();
//      simulationConstructionSet.readData("D:/20141205_115537_Valkyrie_VerySlowLegMotions_Processed.data.gz");
      simulationConstructionSet.readData("D:/RobotLogData/20141212_155658_Valkyrie_HangingDiagnostics_Renamed.data.gz");

      humanoidDiagnosticsWhenHangingSimulation.restoreCorruptorVariableValues();
      
      String side = ""; //"right";

//      String[] containsToOptimizeCoM = new String[]{side + "ThighCoM", side + "ShinCoM"};
      String[] containsToOptimizeCoM = new String[]{side + "ShinCoM"};
      
      side = ""; //"Right";
//      String[] containsToOptimizeTorqueScore = new String[]{"Waist", side + "Hip", side + "Knee", side + "Ankle"};
      String[] containsToOptimizeTorqueScore = new String[]{side + "Knee"};

      humanoidDiagnosticsWhenHangingSimulation.setVariablesToOptimize(containsToOptimizeCoM, containsToOptimizeTorqueScore);
   }
   
   
   private void loadUpperBodyDataAndDoSomeOptimizationTests(HumanoidDiagnosticsWhenHangingSimulation humanoidDiagnosticsWhenHangingSimulation)
   {
      SimulationConstructionSet simulationConstructionSet = humanoidDiagnosticsWhenHangingSimulation.getSimulationConstructionSet();
//      simulationConstructionSet.readData("D:/RobotLogData/20141208_115309_Valkyrie_SlowChestMotionLastSequence/20141208_115309_Valkyrie_SlowChestMotionLastSequence_Processed.data.gz");
      simulationConstructionSet.readData("D:/RobotLogData/20141212_125659_Valkyrie_ChestMotionsDiagnostic_Processed.data.gz");

      
      
      humanoidDiagnosticsWhenHangingSimulation.restoreCorruptorVariableValues();
      
//      setInitialCorruptorLegCoMOffsetValues(simulationConstructionSet);
      
//      String[] containsToOptimizeCoM = new String[]{"chestCoMOffsetX", "chestCoMOffsetZ"}; //, "ShoulderRotatorCoM", "ShoulderAdductorCoM", "ForearmCoM"};
////      String[] containsToOptimizeCoM = new String[]{"chestCoMOffset", "chestMass"}; //, "ShoulderRotatorCoM", "ShoulderAdductorCoM", "ForearmCoM"};
//      String[] containsToOptimizeTorqueScore = new String[]{"Waist"};
      
      String[] containsToOptimizeCoM = new String[]{"chestCoMOffset", "ShoulderRotatorCoM", "ShoulderAdductorCoM", "ForearmCoM"};
    String[] containsToOptimizeTorqueScore = new String[]{"Waist", "Shoulder", "Elbow"};

      humanoidDiagnosticsWhenHangingSimulation.setVariablesToOptimize(containsToOptimizeCoM, containsToOptimizeTorqueScore);
   }

   private void setInitialCorruptorArmMassValues(SimulationConstructionSet simulationConstructionSet)
   {
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderExtensorMass")).set(2.65);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderAdductorMass")).set(2.87);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderRotatorMass")).set(2.575);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightElbowExtensorMass")).set(2.367);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightForearmMass")).set(2.903);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightWristYokeMass")).set(0.1);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightPalmMass")).set(0.928);
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftShoulderExtensorMass")).set(2.65);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftShoulderAdductorMass")).set(2.87);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftShoulderRotatorMass")).set(2.575);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftElbowExtensorMass")).set(2.367);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftForearmMass")).set(2.903);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftWristYokeMass")).set(0.1);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1LeftPalmMass")).set(0.928);
   }
   
   private void setInitialCorruptorArmCoMOffsetValues(SimulationConstructionSet simulationConstructionSet)
   {
      // Start from Sylvain's Numbers:
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderAdductorCoMOffsetX")).set(-0.02);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderAdductorCoMOffsetY")).set(0.005);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderAdductorCoMOffsetZ")).set(-0.04);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderRotatorCoMOffsetX")).set(0.004);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderRotatorCoMOffsetY")).set(0.02);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightShoulderRotatorCoMOffsetZ")).set(-0.273);
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightElbowExtensorCoMOffsetX")).set(-0.027);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightElbowExtensorCoMOffsetY")).set(0.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightElbowExtensorCoMOffsetZ")).set(-0.08);
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightForearmCoMOffsetX")).set(0.015);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightForearmCoMOffsetY")).set(-0.02);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("v1RightForearmCoMOffsetZ")).set(-0.11);
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightShoulderExtensor")).set(-1.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightShoulderAdductor")).set(0.7);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightShoulderSupinator")).set(0.3);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightElbowExtensor")).set(-0.1);
   }
   
   private void setInitialCorruptorArmTorqueOffsetValues(SimulationConstructionSet simulationConstructionSet)
   {
      // Start from Sylvain's Numbers:
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightShoulderExtensor")).set(-1.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightShoulderAdductor")).set(0.7);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightShoulderSupinator")).set(0.3);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("tau_off_RightElbowExtensor")).set(-0.1);
   }
   
   private void setInitialCorruptorLegCoMOffsetValues(SimulationConstructionSet simulationConstructionSet)
   {
      // Tuned by Jerry on December 8, 2014.
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("leftThighCoMOffsetX")).set(-0.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("leftThighCoMOffsetY")).set(0.05);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("leftThighCoMOffsetZ")).set(-0.220);
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("leftShinCoMOffsetX")).set(0.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("leftShinCoMOffsetY")).set(0.045);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("leftShinCoMOffsetZ")).set(-0.185);
      
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("rightThighCoMOffsetX")).set(0.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("rightThighCoMOffsetY")).set(-0.05);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("rightThighCoMOffsetZ")).set(-0.220);
      
      ((DoubleYoVariable) simulationConstructionSet.getVariable("rightShinCoMOffsetX")).set(0.0);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("rightShinCoMOffsetY")).set(-0.045);
      ((DoubleYoVariable) simulationConstructionSet.getVariable("rightShinCoMOffsetZ")).set(-0.185);
      

   }

   public static void main(String[] args)
   {
      new ValkyrieDiagnosticsWhenHangingSimulation();
   }
   
   
   private class ValkyrieRobotModelWithHoist extends ValkyrieRobotModel
   {

      public ValkyrieRobotModelWithHoist(boolean runningOnRealRobot, boolean headless)
      {
         super(runningOnRealRobot, headless);
      }
      
      @Override
      public SDFRobot createSdfRobot(boolean createCollisionMeshes)
      {
         SDFRobot robot = super.createSdfRobot(createCollisionMeshes);
         
         Joint joint = robot.getJoint("WaistLateralExtensor");
         
         ArrayList<Vector3d> attachmentLocations = new ArrayList<Vector3d>();
             
         attachmentLocations.add(new Vector3d(0.0, 0.15, 0.412));
         attachmentLocations.add(new Vector3d(0.0, -0.15, 0.412));
         
         double updateDT = 0.0001;
         VirtualHoist virtualHoist = new VirtualHoist(joint, robot, attachmentLocations, updateDT);
         robot.setController(virtualHoist, 1);
          
         virtualHoist.turnHoistOn();
         virtualHoist.setTeepeeLocation(new Point3d(0.0, 0.0, 2.5));
         virtualHoist.setHoistStiffness(20000.0);
         virtualHoist.setHoistDamping(5000.0);
         
         return robot;
      }

   }
}
