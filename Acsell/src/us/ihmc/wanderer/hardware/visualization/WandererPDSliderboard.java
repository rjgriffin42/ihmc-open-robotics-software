package us.ihmc.wanderer.hardware.visualization;

import java.util.EnumMap;

import us.ihmc.robotDataLogger.YoVariableClient;
import us.ihmc.robotDataVisualizer.visualizer.SCSVisualizer;
import us.ihmc.yoVariables.IndexChangedListener;
import us.ihmc.yoVariables.YoVariableHolder;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.EnumYoVariable;
import us.ihmc.yoVariables.variable.YoVariable;
import us.ihmc.simulationconstructionset.FloatingRootJointRobot;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationConstructionSetTools.util.inputdevices.SliderBoardConfigurationManager;
import us.ihmc.wanderer.hardware.WandererActuator;
import us.ihmc.wanderer.hardware.WandererDashboard;
import us.ihmc.wanderer.hardware.WandererJoint;

public class WandererPDSliderboard extends SCSVisualizer implements IndexChangedListener
{

   private final YoVariableRegistry sliderBoardRegistry = new YoVariableRegistry("WandererPDSliderBoard");
   private final EnumYoVariable<WandererJoint> selectedJoint = new EnumYoVariable<>("selectedJoint", sliderBoardRegistry, WandererJoint.class);

   private final YoBoolean selectedJoint_enabled = new YoBoolean("selectedJoint_enabled", sliderBoardRegistry);
   private final YoDouble selectedJoint_q = new YoDouble("selectedJoint_q", sliderBoardRegistry);
   private final YoDouble selectedJoint_qd = new YoDouble("selectedJoint_qd", sliderBoardRegistry);
   private final YoDouble selectedJoint_tau = new YoDouble("selectedJoint_tau", sliderBoardRegistry);
   private final YoDouble selectedJoint_tau_d = new YoDouble("selectedJoint_tau_d", sliderBoardRegistry);

   private final YoDouble selectedJoint_q_d = new YoDouble("selectedJoint_q_d", sliderBoardRegistry);
   private final YoDouble selectedJoint_qd_d = new YoDouble("selectedJoint_qd_d", sliderBoardRegistry);
   private final YoDouble selectedJoint_kp = new YoDouble("selectedJoint_kp", sliderBoardRegistry);
   private final YoDouble selectedJoint_kd = new YoDouble("selectedJoint_kd", sliderBoardRegistry);
   private final YoDouble selectedJoint_tauFF = new YoDouble("selectedJoint_tauFF", sliderBoardRegistry);
   private final YoDouble selectedJoint_damping = new YoDouble("selectedJoint_damping", sliderBoardRegistry);

   private volatile boolean started = false;

   private final EnumMap<WandererJoint, JointVariables> allJointVariables = new EnumMap<>(WandererJoint.class);

   public WandererPDSliderboard(int bufferSize)
   {
      super(bufferSize);

   }

   @Override
   public void starting(SimulationConstructionSet scs, Robot robot, YoVariableRegistry registry)
   {
      scs.getDataBuffer().attachIndexChangedListener(this);
      registry.addChild(sliderBoardRegistry);
      final SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);

      for (WandererJoint joint : WandererJoint.values)
      {
         JointVariables variables = new JointVariables(joint, registry);

         OneDegreeOfFreedomJoint oneDoFJoint = ((FloatingRootJointRobot) robot).getOneDegreeOfFreedomJoint(joint.getSdfName());
         sliderBoardConfigurationManager.setKnob(1, selectedJoint, 0, WandererJoint.values.length);
         sliderBoardConfigurationManager.setSlider(1, variables.q_d, oneDoFJoint.getJointLowerLimit(), oneDoFJoint.getJointUpperLimit());
         sliderBoardConfigurationManager.setSlider(2, variables.qd_d, -1, 1);
         sliderBoardConfigurationManager.setSlider(3, variables.kp, 0, 10 * joint.getRatio() * joint.getRatio());
         sliderBoardConfigurationManager.setSlider(4, variables.kd, 0, 5 * joint.getRatio() * joint.getRatio());

         if (Double.isNaN(oneDoFJoint.getTorqueLimit()) || Double.isInfinite(oneDoFJoint.getTorqueLimit()))
         {
            sliderBoardConfigurationManager.setSlider(5, variables.tauFF, -50, 50);
         }
         else
         {
            sliderBoardConfigurationManager.setSlider(5, variables.tauFF, -oneDoFJoint.getTorqueLimit(), oneDoFJoint.getTorqueLimit());
         }
         sliderBoardConfigurationManager.setSlider(6, variables.damping, 0, .5 * joint.getRatio() * joint.getRatio());

         sliderBoardConfigurationManager.setButton(1, variables.enabled);
         sliderBoardConfigurationManager.saveConfiguration(joint.toString());
         allJointVariables.put(joint, variables);
      }

      selectedJoint.addVariableChangedListener(new VariableChangedListener()
      {

         @Override
         public void variableChanged(YoVariable<?> v)
         {
            sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());
         }
      });

      selectedJoint.set(WandererJoint.RIGHT_KNEE_Y);

      WandererDashboard.createDashboard(scs, registry);

      started = true;
   }

   private class JointVariables
   {
      private final YoBoolean enabled;

      private final YoDouble q;
      private final YoDouble qd;
      private final YoDouble tau;
      private final YoDouble tau_d;

      private final YoDouble q_d;
      private final YoDouble qd_d;

      private final YoDouble kp;
      private final YoDouble kd;

      private final YoDouble tauFF;
      private final YoDouble damping;

      public JointVariables(final WandererJoint joint, final YoVariableHolder variableHolder)
      {
         final String prefix = joint.getSdfName();
         final String namespace = "WandererCommand." + prefix;
         enabled = new YoBoolean(joint.getSdfName() + "_enabled", sliderBoardRegistry);
         enabled.addVariableChangedListener(new VariableChangedListener()
         {

            @Override
            public void variableChanged(YoVariable<?> v)
            {
               for (WandererActuator actuator : joint.getActuators())
               {
                  String namespace;
                  String variable;
                  switch (actuator)
                  {
                  case LEFT_ANKLE_RIGHT:
                     namespace = "WandererCommand.leftAnkleCommand.leftAnkleCommandRightActuator";
                     variable = "leftAnkleCommandRightActuatorEnabled";
                     break;
                  case LEFT_ANKLE_LEFT:
                     namespace = "WandererCommand.leftAnkleCommand.leftAnkleCommandLeftActuator";
                     variable = "leftAnkleCommandLeftActuatorEnabled";
                     break;
                  case RIGHT_ANKLE_RIGHT:
                     namespace = "WandererCommand.rightAnkleCommand.rightAnkleCommandRightActuator";
                     variable = "rightAnkleCommandRightActuatorEnabled";
                     break;
                  case RIGHT_ANKLE_LEFT:
                     namespace = "WandererCommand.rightAnkleCommand.rightAnkleCommandLeftActuator";
                     variable = "rightAnkleCommandLeftActuatorEnabled";
                     break;
                  default:
                     namespace = "WandererCommand." + actuator.getName();
                     variable = actuator.getName() + "Enabled";
                     break;
                  }

                  YoBoolean actEnabled = (YoBoolean) variableHolder.getVariable(namespace, variable);
                  actEnabled.set(enabled.getBooleanValue());
               }
            }
         });

         String stateNameSpace;
         String qStateVariable;
         String qdStateVariable;
         String tauStateVariable;
         switch (joint)
         {
         case LEFT_ANKLE_X:
            stateNameSpace = "Wanderer.leftAnkle";
            qStateVariable = "leftAnkle_q_x";
            qdStateVariable = "leftAnkle_qd_x";
            tauStateVariable = "leftAnkle_tau_xPredictedCurrent";
            break;
         case LEFT_ANKLE_Y:
            stateNameSpace = "Wanderer.leftAnkle";
            qStateVariable = "leftAnkle_q_y";
            qdStateVariable = "leftAnkle_qd_y";
            tauStateVariable = "leftAnkle_tau_yPredictedCurrent";
            break;
         case RIGHT_ANKLE_X:
            stateNameSpace = "Wanderer.rightAnkle";
            qStateVariable = "rightAnkle_q_x";
            qdStateVariable = "rightAnkle_qd_x";
            tauStateVariable = "rightAnkle_tau_xPredictedCurrent";
            break;
         case RIGHT_ANKLE_Y:
            stateNameSpace = "Wanderer.rightAnkle";
            qStateVariable = "rightAnkle_q_y";
            qdStateVariable = "rightAnkle_qd_y";
            tauStateVariable = "rightAnkle_tau_yPredictedCurrent";
            break;

         default:
            stateNameSpace = "Wanderer." + prefix;
            qStateVariable = prefix + "_q";
            qdStateVariable = prefix + "_qd";
            tauStateVariable = prefix + "_tauPredictedCurrent";
         }
         q = (YoDouble) variableHolder.getVariable(stateNameSpace, qStateVariable);
         qd = (YoDouble) variableHolder.getVariable(stateNameSpace, qdStateVariable);
         tau = (YoDouble) variableHolder.getVariable(stateNameSpace, tauStateVariable);

         q_d = (YoDouble) variableHolder.getVariable("WandererPDJointController", prefix + "_q_d");
         qd_d = (YoDouble) variableHolder.getVariable("WandererPDJointController", prefix + "_qd_d");
         tauFF = (YoDouble) variableHolder.getVariable("WandererPDJointController", prefix + "_tau_ff");
         damping = (YoDouble) variableHolder.getVariable("WandererPDJointController", prefix + "_damping");
         kp = (YoDouble) variableHolder.getVariable("WandererPDJointController", "kp_" + prefix);
         kd = (YoDouble) variableHolder.getVariable("WandererPDJointController", "kd_" + prefix);

         tau_d = (YoDouble) variableHolder.getVariable(namespace, prefix + "TauDesired");

      }

      public void update()
      {
         selectedJoint_enabled.set(enabled.getBooleanValue());
         selectedJoint_q.set(q.getDoubleValue());
         selectedJoint_qd.set(qd.getDoubleValue());
         selectedJoint_tau.set(tau.getDoubleValue());
         selectedJoint_tau_d.set(tau_d.getDoubleValue());
         selectedJoint_q_d.set(q_d.getDoubleValue());
         selectedJoint_qd_d.set(qd_d.getDoubleValue());
         selectedJoint_kp.set(kp.getDoubleValue());
         selectedJoint_kd.set(kd.getDoubleValue());
         selectedJoint_tauFF.set(tauFF.getDoubleValue());
         selectedJoint_damping.set(damping.getDoubleValue());
      }

      public void initialize()
      {
         if(!enabled.getBooleanValue())
         {
            q_d.set(q.getDoubleValue());
            qd_d.set(0.0);
         }
      }
   }

   @Override
   public void indexChanged(int newIndex, double newTime)
   {
      if (started)
      {
         WandererJoint joint = selectedJoint.getEnumValue();
         
         for(WandererJoint wandererJoint:WandererJoint.values)
         {
            allJointVariables.get(wandererJoint).initialize();
         }
         
         allJointVariables.get(joint).update();
      }
   }

   public static void main(String[] args)
   {
      SCSVisualizer scsYoVariablesUpdatedListener = new WandererPDSliderboard(16384);
      scsYoVariablesUpdatedListener.setShowOverheadView(false);

      YoVariableClient client = new YoVariableClient(scsYoVariablesUpdatedListener, "remote");
      client.start();

   }

}
