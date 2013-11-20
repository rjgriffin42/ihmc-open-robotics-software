package us.ihmc.atlas.visualization;

import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;
import us.ihmc.atlas.api.AtlasJointId;
import us.ihmc.atlas.parameters.AtlasLimits;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.CommonNames;

import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.inputdevices.SliderBoardConfigurationManager;

public class WalkControllerSliderBoard

{
   public WalkControllerSliderBoard(SimulationConstructionSet scs, YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel)
   {
      final EnumYoVariable<SliderBoardMode> sliderBoardMode = new EnumYoVariable<SliderBoardMode>("sliderBoardMode", registry, SliderBoardMode.class);
      final SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);

      sliderBoardConfigurationManager.setSlider(1, "captureKpParallel", registry, 0.0, 2.0);
      sliderBoardConfigurationManager.setKnob(1, "captureKpOrthogonal", registry, 0.0, 2.0);

      sliderBoardConfigurationManager.setSlider(2, "kp_comHeight", registry, 0.0, 40.0);
      sliderBoardConfigurationManager.setKnob(2, "kd_comHeight", registry, 0.0, 13.0);

      sliderBoardConfigurationManager.setSlider(3, "kpPelvisOrientation", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob(3, "zetaPelvisOrientation", registry, 0.0, 1.0);

//    sliderBoardConfigurationManager.setSlider(4, "walkingHeadOrientationKp", registry, 0.0, 40.0);
//    sliderBoardConfigurationManager.setKnob  (4, "walkingHeadOrientationZeta", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(4, "kpUpperBody", registry, 0.0, 200.0);
      sliderBoardConfigurationManager.setKnob(4, "zetaUpperBody", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(5, "kpAllArmJointsL", registry, 0.0, 120.0);
      sliderBoardConfigurationManager.setKnob(5, "zetaAllArmJointsL", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(6, "kpAllArmJointsR", registry, 0.0, 120.0);
      sliderBoardConfigurationManager.setKnob(6, "zetaAllArmJointsR", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(7, CommonNames.doIHMCControlRatio.toString(), registry, 0.0, 1.0);

//    sliderBoardConfigurationManager.setSlider(7, CommonNames.doIHMCControlRatio.toString(), registry, 0.0, 1.0, 3.5, 0.0);
      sliderBoardConfigurationManager.setSlider(8, "offsetHeightAboveGround", registry, 0.0, 0.20);

//    sliderBoardConfigurationManager.setKnob  (8, "sliderBoardMode", registry, 0.0, SliderBoardMode.values().length);
//      sliderBoardConfigurationManager.setKnob(8, "gainScaleFactor", registry, 0.0, 1.0, 3.5, 0.0);

      sliderBoardConfigurationManager.saveConfiguration(SliderBoardMode.WalkingGains.toString());
      sliderBoardConfigurationManager.clearControls();

      sliderBoardConfigurationManager.setSlider(1, "captureKpParallel", registry, 0.0, 2.0);
      sliderBoardConfigurationManager.setKnob(1, "captureKpOrthogonal", registry, 0.0, 2.0);

      sliderBoardConfigurationManager.setSlider(2, "desiredICPX", registry, -0.3, 0.3);
      sliderBoardConfigurationManager.setKnob(2, "desiredICPY", registry, -0.3, 0.3);
      sliderBoardConfigurationManager.setKnob(9, "desiredICPEccentricity", registry, 0, .9);
      sliderBoardConfigurationManager.setKnob(10, "desiredICPAngle", registry, -Math.PI, Math.PI);

      sliderBoardConfigurationManager.setSlider(3, "userDesiredPelvisYaw", registry, -Math.PI, Math.PI);
      sliderBoardConfigurationManager.setKnob(3, "userSetDesiredPelvis", registry, 0.0, 1.0);
      sliderBoardConfigurationManager.setSlider(4, "userDesiredPelvisPitch", registry, -0.4, 0.4);
      sliderBoardConfigurationManager.setSlider(5, "userDesiredPelvisRoll", registry, -0.3, 0.3);

      sliderBoardConfigurationManager.setSlider(6, "userDesiredHeadPitch", registry, -0.5, 0.5);
      sliderBoardConfigurationManager.setKnob(6, "userDesiredNeckPitch", registry, -0.8, 0.8);
      
      sliderBoardConfigurationManager.setSlider(7, "userDesiredHeadYaw", registry, -0.8, 0.8);
      sliderBoardConfigurationManager.setSlider(8, "offsetHeightAboveGround", registry, 0.0, 0.20);

//    sliderBoardConfigurationManager.setKnob  (8, "sliderBoardMode", registry, 0.0, SliderBoardMode.values().length);
      sliderBoardConfigurationManager.setKnob(8, "gainScaleFactor", registry, 0.0, 1.0, 3.5, 0.0);

      sliderBoardConfigurationManager.saveConfiguration(SliderBoardMode.WalkingDesireds.toString());
      sliderBoardConfigurationManager.clearControls();
      
      
 
      final AtlasLimits atlasLimits = AtlasLimits.getHighLevelLimits(generalizedSDFRobotModel);
      
      final EnumYoVariable<AtlasJointId> selectedJoint = new EnumYoVariable<AtlasJointId>("testedJoint", registry, AtlasJointId.class);
      ForceControllerTunerListener.setupSliderBoard(sliderBoardConfigurationManager, selectedJoint, atlasLimits, registry);


      sliderBoardMode.set(SliderBoardMode.WalkingGains);

      VariableChangedListener listener = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable v)
         {
            if (sliderBoardMode.getEnumValue() == SliderBoardMode.ForceGains)
            {
               sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString()); 
            }
            
            else
            {
               sliderBoardConfigurationManager.loadConfiguration(sliderBoardMode.getEnumValue().toString());
            }
         }
      };

      sliderBoardMode.addVariableChangedListener(listener);
      listener.variableChanged(null);

   }

   private enum SliderBoardMode {WalkingGains, WalkingDesireds, ForceGains;}

   private static final SliderBoardFactory factory = new SliderBoardFactory()
   {
      @Override
      public void makeSliderBoard(SimulationConstructionSet scs, YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel)
      {
         new WalkControllerSliderBoard(scs, registry, generalizedSDFRobotModel);
      }
   };

   public static SliderBoardFactory getFactory()
   {
      return factory;
   }
}
