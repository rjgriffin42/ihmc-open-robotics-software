package us.ihmc.valkyrie.visualizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelHumanoidControllerFactory;
import us.ihmc.commons.FormattingTools;
import us.ihmc.communication.configuration.NetworkParameterKeys;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelControllerName;
import us.ihmc.robotDataLogger.Announcement;
import us.ihmc.robotDataLogger.YoVariableClient;
import us.ihmc.robotDataLogger.rtps.LogProducerDisplay;
import us.ihmc.robotDataVisualizer.visualizer.SCSVisualizer;
import us.ihmc.robotDataVisualizer.visualizer.SCSVisualizerStateListener;
import us.ihmc.simulationConstructionSetTools.util.inputdevices.SliderBoardConfigurationManager;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoVariable;

public class RemoteValkyrieVisualizer implements SCSVisualizerStateListener
{
   public static final int BUFFER_SIZE = 16384;

   public RemoteValkyrieVisualizer()
   {
      String host = NetworkParameters.getHost(NetworkParameterKeys.logger);
      System.out.println("Connecting to host " + host);

      SCSVisualizer scsVisualizer = new SCSVisualizer(BUFFER_SIZE);
      scsVisualizer.setDisplayOneInNPackets(5);
      scsVisualizer.addSCSVisualizerStateListener(this);
      scsVisualizer.setShowOverheadView(true);

      YoVariableClient client = new YoVariableClient(scsVisualizer, new RemoteValkyrieVisualizerLogFilter());
      client.start();
   }

   @Override
   public void starting(final SimulationConstructionSet scs, Robot robot, YoVariableRegistry registry)
   {
      // TODO The sliderboard throws an NPE when scrubbing, at least in Sim. If this is okay on the real robot then feel free to uncomment. -- Doug
      createSliderBoard(scs, registry);

      final YoEnum<?> requestHighLevelControlMode = (YoEnum<?>) scs.getVariable(HighLevelHumanoidControllerFactory.class.getSimpleName(),
                                                                                "requestedHighLevelControllerState");

      HighLevelControllerName[] valuesToDisplay = {HighLevelControllerName.CALIBRATION, HighLevelControllerName.STAND_TRANSITION_STATE, HighLevelControllerName.EXIT_WALKING};
      Map<HighLevelControllerName, String> displayNames = new HashMap<>();
      displayNames.put(HighLevelControllerName.STAND_TRANSITION_STATE, "Go Walking Val!");

      final String[] comboBoxValues = new String[valuesToDisplay.length + 1];
      comboBoxValues[0] = "High-Level Control Mode";

      Map<Integer, Integer> fromComboBoxIndexToEnumOrdinalMap = new HashMap<>();
      fromComboBoxIndexToEnumOrdinalMap.put(0, -1);
      Map<Integer, Integer> fromEnumOrdinalToComboBoxIndexMap = new HashMap<>();
      fromEnumOrdinalToComboBoxIndexMap.put(-1, 0);
      int comboBoxIndex = 1; // Leave the first for a default item
      for (HighLevelControllerName valueToDisplay : valuesToDisplay)
      {
         if (displayNames.containsKey(valueToDisplay))
         {
            comboBoxValues[comboBoxIndex] = displayNames.get(valueToDisplay);
         }
         else
         {
            comboBoxValues[comboBoxIndex] = valueToDisplay.name();
            comboBoxValues[comboBoxIndex] = comboBoxValues[comboBoxIndex].replaceAll("_", " _");
            comboBoxValues[comboBoxIndex] = FormattingTools.underscoredToCamelCase(comboBoxValues[comboBoxIndex], true);
         }

         fromComboBoxIndexToEnumOrdinalMap.put(comboBoxIndex, valueToDisplay.ordinal());
         fromEnumOrdinalToComboBoxIndexMap.put(valueToDisplay.ordinal(), comboBoxIndex);
         comboBoxIndex++;
      }

      final JComboBox<String> requestControlModeComboBox = new JComboBox<>(comboBoxValues);
      requestControlModeComboBox.setSelectedIndex(0);
      requestControlModeComboBox.addActionListener(new ActionListener()
      {
         @Override
         public void actionPerformed(ActionEvent e)
         {
            int newOrdinal = fromComboBoxIndexToEnumOrdinalMap.get(requestControlModeComboBox.getSelectedIndex());
            int currentOrdinal = requestHighLevelControlMode.getOrdinal();
            if (newOrdinal != currentOrdinal)
            {
               requestHighLevelControlMode.set(newOrdinal);
            }
         }
      });

      requestHighLevelControlMode.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void notifyOfVariableChange(YoVariable<?> v)
         {
            int currentIndex = requestControlModeComboBox.getSelectedIndex();
            int newIndex = fromEnumOrdinalToComboBoxIndexMap.get(requestHighLevelControlMode.getOrdinal());
            if (currentIndex != newIndex)
               requestControlModeComboBox.setSelectedIndex(newIndex);
         }
      });

      scs.addComboBox(requestControlModeComboBox);
   }

   private void createSliderBoard(final SimulationConstructionSet scs, YoVariableRegistry registry)
   {
      SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);
      sliderBoardConfigurationManager.setButton(1, registry.getVariable("PelvisICPBasedTranslationManager", "manualModeICPOffset"));
      sliderBoardConfigurationManager.setSlider(1, "desiredICPOffsetX", registry, -0.3, 0.3);
      sliderBoardConfigurationManager.setSlider(2, "desiredICPOffsetY", registry, -0.3, 0.3);
      sliderBoardConfigurationManager.setSlider(8, "offsetHeightAboveGround", registry, 0.0, 0.20);
   }

   public static void main(String[] args)
   {
      new RemoteValkyrieVisualizer();
   }

   private class RemoteValkyrieVisualizerLogFilter implements LogProducerDisplay.LogSessionFilter
   {
      @Override
      public boolean shouldAddToDisplay(Announcement description)
      {
         return description.getHostNameAsString().startsWith("link");
      }
   }
}
