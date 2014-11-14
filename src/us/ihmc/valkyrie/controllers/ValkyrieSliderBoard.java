package us.ihmc.valkyrie.controllers;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.CommonNames;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.InverseDynamicsJointController;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.visualization.WalkControllerSliderBoard;
import us.ihmc.valkyrie.configuration.ValkyrieConfigurationRoot;
import us.ihmc.valkyrie.kinematics.urdf.Interface;
import us.ihmc.valkyrie.kinematics.urdf.Transmission;
import us.ihmc.valkyrie.kinematics.urdf.URDFRobotRoot;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.inputdevices.SliderBoardConfigurationManager;
import us.ihmc.simulationconstructionset.util.math.functionGenerator.YoFunctionGeneratorMode;

/**
 * Created by dstephen on 2/28/14.
 */
public class ValkyrieSliderBoard
{
   public enum ValkyrieSliderBoardType {ON_BOARD_POSITION, TORQUE_PD_CONTROL, WALKING, TUNING, GRAVITY_COMPENSATION}

   private final EnumYoVariable<ValkyrieSliderBoardSelectableJoints> selectedJoint, remoteSelectedJoint;

   private final LinkedHashMap<String, IntegerYoVariable> storedTurboIndex = new LinkedHashMap<>();

   private final IntegerYoVariable remoteTurboIndex;

   @SuppressWarnings("unchecked")
   public ValkyrieSliderBoard(SimulationConstructionSet scs, YoVariableRegistry registry, DRCRobotModel drcRobotModel,
                              ValkyrieSliderBoardType sliderBoardType)
   {
      selectedJoint = new EnumYoVariable<>("selectedJoint", registry, ValkyrieSliderBoardSelectableJoints.class);
      selectedJoint.set(ValkyrieSliderBoardSelectableJoints.RightKneeExtensor);
      remoteSelectedJoint = (EnumYoVariable<ValkyrieSliderBoardSelectableJoints>) registry.getVariable("ValkyrieSliderBoardController", "selectedJoint");

      remoteTurboIndex = (IntegerYoVariable) registry.getVariable("ValkyrieSliderBoardController", "turboIndex");

      final SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);

      switch (sliderBoardType)
      {
         case ON_BOARD_POSITION :
            setupSliderBoardForOnBoardPositionControl(registry, drcRobotModel.getGeneralizedRobotModel(), sliderBoardConfigurationManager);

            break;

         case TORQUE_PD_CONTROL :
            setupSliderBoardForForceControl(registry, drcRobotModel.getGeneralizedRobotModel(), sliderBoardConfigurationManager);

            break;

         case WALKING :
            new WalkControllerSliderBoard(scs, registry, drcRobotModel.getGeneralizedRobotModel());

            break;

         case TUNING :
            try
            {
               setupSliderBoardForForceControlTuning(registry, drcRobotModel.getGeneralizedRobotModel(), sliderBoardConfigurationManager);
            }
            catch (JAXBException e)
            {
               e.printStackTrace();
            }
            
            break;
         case GRAVITY_COMPENSATION :
            
            new InverseDynamicsJointController.GravityCompensationSliderBoard(scs, drcRobotModel.createFullRobotModel(), registry, CommonNames.doIHMCControlRatio.toString(), 0.0, 1.0);
            
            break;
      }

      sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());
   }

   private void setupSliderBoardForForceControl(YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel,
           final SliderBoardConfigurationManager sliderBoardConfigurationManager)
   {
      for (ValkyrieSliderBoardSelectableJoints jointId :
              ValkyrieSliderBoardSelectableJoints.values())
      {
         String jointName = jointId.toString();
         if (!jointName.contains("Ibeo"))
         {
            String pdControllerBaseName = jointName + "ValkyrieJointPDController";

            // knobs

            sliderBoardConfigurationManager.setKnob(1, selectedJoint, 0, ValkyrieSliderBoardSelectableJoints.values().length - 1);

            // sliders
            sliderBoardConfigurationManager.setSlider(1, pdControllerBaseName + "_q_d", registry,
                    generalizedSDFRobotModel.getJointHolder(jointName).getLowerLimit(), generalizedSDFRobotModel.getJointHolder(jointName).getUpperLimit());
            sliderBoardConfigurationManager.setSlider(2, "kp_" + pdControllerBaseName, registry, 0.0, 2000.0);
            sliderBoardConfigurationManager.setSlider(3, "kd_" + pdControllerBaseName, registry, 0.0, 600.0);

            sliderBoardConfigurationManager.saveConfiguration(jointId.toString());
         }

      }


      selectedJoint.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            System.out.println("loading configuration " + selectedJoint.getEnumValue());
            sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());

            if (remoteSelectedJoint != null)
            {
               remoteSelectedJoint.set(selectedJoint.getEnumValue());
            }
         }
      });
   }

   private void setupSliderBoardForForceControlTuning(YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel,
           final SliderBoardConfigurationManager sliderBoardConfigurationManager)
           throws JAXBException
   {
      JAXBContext context = JAXBContext.newInstance(URDFRobotRoot.class);
      Unmarshaller um = context.createUnmarshaller();
      URDFRobotRoot urdfRoot = (URDFRobotRoot) um.unmarshal(ValkyrieConfigurationRoot.class.getResourceAsStream(ValkyrieConfigurationRoot.URDF_FILE));

      for (Transmission t : urdfRoot.getTransmissions())
      {
         for (Interface i : t.getInterfaces())
         {
            if (i.getType().equals("JointToActuatorStateInterface"))
            {
               ArrayList<String> turbos = new ArrayList<>();
               boolean isForearm = false;
               for (Interface.Actuator a : i.getActuators())
               {
                  if(a.getName().toLowerCase().contains("forearm"))
                  {
                     isForearm = true;
                  }
                  else
                  {
                     turbos.add(a.getName().replace("/", "_"));
                  }
               }

               for (Interface.Joint j : i.getJoints())
               {
                  if (!isForearm)
                  {
                     String jointName = j.getName();
                     String pdControllerBaseName = jointName + "ValkyrieJointTorqueControlTuner";

                     if (!jointName.contains("Ibeo"))
                     {
                        if (isTunableRotaryJoint(jointName))
                        {
                           assert turbos.size() == 1;

                           String turboName = turbos.get(0);
                           // knobs
                           sliderBoardConfigurationManager.setKnob(1, selectedJoint, 0,
                                   ValkyrieSliderBoardSelectableJoints.values().length - 1);
                           sliderBoardConfigurationManager.setKnob(3, turboName + "_lowLevelKp", registry, 0.0, 20.0);
                           sliderBoardConfigurationManager.setKnob(4, turboName + "_lowLevelKd", registry, 0.0, 0.5);
                           sliderBoardConfigurationManager.setKnob(5, turboName + "_forceAlpha", registry, 0.0, 1.0);
                           sliderBoardConfigurationManager.setKnob(6, turboName + "_forceDotAlpha", registry, 0.0, 1.0);
                           sliderBoardConfigurationManager.setKnob(7, turboName + "_parallelDamping", registry, -10.0, 0.0);
                           sliderBoardConfigurationManager.setKnob(8, "requestedFunctionGeneratorMode", registry, 0, YoFunctionGeneratorMode.values().length - 1);
                           sliderBoardConfigurationManager.setKnob(9, turboName + "_effortFF", registry, -0.1, 0.1);
                           // sliders
                           sliderBoardConfigurationManager.setSlider(1, pdControllerBaseName + "_q_d", registry,
                                 generalizedSDFRobotModel.getJointHolder(jointName).getLowerLimit(),
                                 generalizedSDFRobotModel.getJointHolder(jointName).getUpperLimit());
                           sliderBoardConfigurationManager.setSlider(2, "kp_" + pdControllerBaseName, registry, 0.0, 2000.0);
                           sliderBoardConfigurationManager.setSlider(3, "kd_" + pdControllerBaseName, registry, 0.0, 600.0);
                           sliderBoardConfigurationManager.setSlider(4, "ki_" + pdControllerBaseName, registry, 0.0, 600.0);
                           sliderBoardConfigurationManager.setSlider(5, pdControllerBaseName + "_transitionFactor", registry, 0.0, 1.0);

                           sliderBoardConfigurationManager.setSlider(6, pdControllerBaseName + "_functionGeneratorAmplitude", registry, 0, 200);
                           sliderBoardConfigurationManager.setSlider(7, pdControllerBaseName + "_functionGeneratorFrequency", registry, 0, 50);
                           sliderBoardConfigurationManager.setSlider(8, pdControllerBaseName + "_functionGeneratorOffset", registry, -100, 100);

                           sliderBoardConfigurationManager.saveConfiguration(jointName);
                           sliderBoardConfigurationManager.clearControls();
                        }
                        else if (isTunableLinearActuatorJoint(jointName))
                        {
                           assert turbos.size() == 2;
                           IntegerYoVariable turboIndexMonitor = new IntegerYoVariable(jointName + "_turboIndexMonitor", registry);

                           for (int count = 0; count < turbos.size(); count++)
                           {
                              String turboName = turbos.get(count);

                              // knobs
                              sliderBoardConfigurationManager.setKnob(1, selectedJoint, 0,
                                      ValkyrieSliderBoardSelectableJoints.values().length - 1);
                              sliderBoardConfigurationManager.setKnob(2, turboIndexMonitor, 0, 1);
                              sliderBoardConfigurationManager.setKnob(3, turboName + "_lowLevelKp", registry, 0.0, 0.1);
                              sliderBoardConfigurationManager.setKnob(4, turboName + "_lowLevelKd", registry, 0.0, 0.001);
                              sliderBoardConfigurationManager.setKnob(5, turboName + "_forceAlpha", registry, 0.0, 1.0);
                              sliderBoardConfigurationManager.setKnob(6, turboName + "_forceDotAlpha", registry, 0.0, 1.0);
                              sliderBoardConfigurationManager.setKnob(7, turboName + "_parallelDamping", registry, -10.0, 0.0);
                              sliderBoardConfigurationManager.setKnob(8, "requestedFunctionGeneratorMode", registry, 0, YoFunctionGeneratorMode.values().length - 1);
                              sliderBoardConfigurationManager.setKnob(9, turboName + "_effortFF", registry, -0.01, 0.01);
                              // sliders
                              sliderBoardConfigurationManager.setSlider(1, pdControllerBaseName + "_q_d", registry,
                                      generalizedSDFRobotModel.getJointHolder(jointName).getLowerLimit(),
                                      generalizedSDFRobotModel.getJointHolder(jointName).getUpperLimit());
                              sliderBoardConfigurationManager.setSlider(2, "kp_" + pdControllerBaseName, registry, 0.0, 2000.0);
                              sliderBoardConfigurationManager.setSlider(3, "kd_" + pdControllerBaseName, registry, 0.0, 600.0);
                              sliderBoardConfigurationManager.setSlider(4, "ki_" + pdControllerBaseName, registry, 0.0, 600.0);                              
                              sliderBoardConfigurationManager.setSlider(5, pdControllerBaseName + "_transitionFactor", registry, 0.0, 1.0);
//                              sliderBoardConfigurationManager.setSlider(5, pdControllerBaseName + "_tauDesired", registry, -100.0, 100.0);

//                              sliderBoardConfigurationManager.setButton(1, pdControllerBaseName + "_useFunctionGenerator", registry);
                              sliderBoardConfigurationManager.setSlider(6, pdControllerBaseName + "_functionGeneratorAmplitude", registry, 0, 200);
                              sliderBoardConfigurationManager.setSlider(7, pdControllerBaseName + "_functionGeneratorFrequency", registry, 0, 50);
                              sliderBoardConfigurationManager.setSlider(8, pdControllerBaseName + "_functionGeneratorOffset", registry, -100, 100);

                              sliderBoardConfigurationManager.saveConfiguration(jointName + count);
                              sliderBoardConfigurationManager.clearControls();
                           }

                           storedTurboIndex.put(jointName, turboIndexMonitor);

                           turboIndexMonitor.addVariableChangedListener(new VariableChangedListener()
                           {
                              @Override
                              public void variableChanged(YoVariable<?> v)
                              {
                                 if (isTunableRotaryJoint(selectedJoint.getEnumValue().toString()))
                                 {
                                    remoteTurboIndex.set(0);
                                    System.out.println("loading configuration " + selectedJoint.getEnumValue());
                                    sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());
                                 }

                                 if (isTunableLinearActuatorJoint(selectedJoint.getEnumValue().toString()))
                                 {
                                    int storedIndex = storedTurboIndex.get(selectedJoint.getEnumValue().toString()).getIntegerValue();
                                    remoteTurboIndex.set(storedIndex);
                                    System.out.println("loading configuration " + selectedJoint.getEnumValue() + " " + storedIndex);
                                    sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString() + storedIndex);
                                 }

                                 if (remoteSelectedJoint != null)
                                 {
                                    remoteSelectedJoint.set(selectedJoint.getEnumValue());
                                 }
                              }
                           });
                        }
                     }
                  }
               }
            }
         }
      }

      selectedJoint.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            if (isTunableRotaryJoint(selectedJoint.getEnumValue().toString()))
            {
               remoteTurboIndex.set(0);
               System.out.println("loading configuration " + selectedJoint.getEnumValue());
               sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());
            }

            if (isTunableLinearActuatorJoint(selectedJoint.getEnumValue().toString()))
            {               
               int storedIndex = storedTurboIndex.get(selectedJoint.getEnumValue().toString()).getIntegerValue();
               remoteTurboIndex.set(storedIndex);
               System.out.println("loading configuration " + selectedJoint.getEnumValue() + " " + storedIndex);
               sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString() + storedIndex);
            }

            if (remoteSelectedJoint != null)
            {
               remoteSelectedJoint.set(selectedJoint.getEnumValue());
            }
         }
      });
   }

   static final String[] untunableOrNonRotaryJoints = new String[]
   {
      "WaistExtensor", "WaistLateral", "Ankle", "Neck", "Forearm", "Wrist"
   };

   private boolean isTunableRotaryJoint(String jointName)
   {
      boolean ret = true;

      for (String s : untunableOrNonRotaryJoints)
      {
         if (jointName.contains(s))
         {
            ret = false;
         }
      }

      return ret;
   }

   static final String[] tunableLinearActuatorJoint = new String[] {"WaistExtensor", "WaistLateral", "Ankle"};

   private boolean isTunableLinearActuatorJoint(String jointName)
   {
      for (String s : tunableLinearActuatorJoint)
      {
         if (jointName.contains(s))
         {
            return true;
         }
      }

      return false;
   }

   private void setupSliderBoardForOnBoardPositionControl(YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel,
           final SliderBoardConfigurationManager sliderBoardConfigurationManager)
   {
      for (ValkyrieSliderBoardSelectableJoints jointId :
              ValkyrieSliderBoardSelectableJoints.values())
      {
         String jointName = jointId.toString();
         System.out.println(jointName);

         // knobs

         sliderBoardConfigurationManager.setKnob(1, selectedJoint, 0, ValkyrieSliderBoardSelectableJoints.values().length - 1);

         // sliders
         sliderBoardConfigurationManager.setSlider(1, jointName + CommonNames.q_d, registry,
                 generalizedSDFRobotModel.getJointHolder(jointName).getLowerLimit(), generalizedSDFRobotModel.getJointHolder(jointName).getUpperLimit());
         sliderBoardConfigurationManager.setSlider(2, jointName + CommonNames.qd_d, registry, -9, 9);

         sliderBoardConfigurationManager.saveConfiguration(jointId.toString());
      }


      selectedJoint.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            System.out.println("loading configuration " + selectedJoint.getEnumValue());
            sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());

            if (remoteSelectedJoint != null)
            {
               remoteSelectedJoint.set(selectedJoint.getEnumValue());
            }
         }
      });
   }
}
