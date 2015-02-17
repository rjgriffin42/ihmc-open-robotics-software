package us.ihmc.darpaRoboticsChallenge;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.utilities.FormattingTools;

public abstract class DRCSimulationTools
{
   public static DRCSimulationStarter createObstacleCourseSimulationStarter(DRCRobotModel robotModel)
   {
      return new DRCSimulationStarter(robotModel, new DRCDemo01NavigationEnvironment());
   }

   @SuppressWarnings({ "hiding", "unchecked" })
   public static <T extends DRCStartingLocation, Enum> void startSimulationWithGraphicSelector(DRCSimulationStarter simulationStarter, T... possibleStartingLocations)
   {
      List<Modules> modulesToStart = new ArrayList<Modules>();
      DRCStartingLocation startingLocation = null;

      if (simulationStarter.getEnvironment() instanceof DRCDemo01NavigationEnvironment)
      {
         startingLocation = showSelectorWithStartingLocation(modulesToStart, DRCObstacleCourseStartingLocation.values());
      }
      else
      {
         startingLocation = showSelectorWithStartingLocation(modulesToStart, possibleStartingLocations);
      }

      if (startingLocation != null)
         simulationStarter.setStartingLocation(startingLocation);

      if (modulesToStart.isEmpty())
         return;
      else if (modulesToStart.size() == 1)
         simulationStarter.setSpawnOperatorInterfaceInDifferentProcess(false);

      
      boolean automaticallyStartSimulation = true;
      DRCNetworkModuleParameters networkProcessorParameters = new DRCNetworkModuleParameters();
      networkProcessorParameters.setUseUiModule(modulesToStart.contains(Modules.NETWORK_PROCESSOR));
      networkProcessorParameters.setUseBehaviorModule(modulesToStart.contains(Modules.BEHAVIOR_MODULE));
      networkProcessorParameters.setUseBehaviorVisualizer(modulesToStart.contains(Modules.BEHAVIOR_MODULE));
      networkProcessorParameters.setUseSensorModule(modulesToStart.contains(Modules.SENSOR_MODULE));
      networkProcessorParameters.setUsePerceptionModule(true);
      
      if (modulesToStart.contains(Modules.SIMULATION))
         simulationStarter.startSimulation(networkProcessorParameters, automaticallyStartSimulation);

      if (modulesToStart.contains(Modules.OPERATOR_INTERFACE))
         simulationStarter.startOpertorInterface();

      if (modulesToStart.contains(Modules.BEHAVIOR_VISUALIZER))
         simulationStarter.startBehaviorVisualizer();
   }

   @SuppressWarnings({ "hiding", "unchecked", "rawtypes" })
   private static <T extends DRCStartingLocation, Enum> DRCStartingLocation showSelectorWithStartingLocation(List<Modules> modulesToStartListToPack, T... possibleStartingLocations)
   {
      JPanel userPromptPanel = new JPanel(new BorderLayout());
      JPanel checkBoxesPanel = new JPanel(new GridLayout(3, 2));

      String configFile = System.getProperty("user.home") + "/.ihmc/drcSimulationDefaultOptions.config";
      Properties properties = new Properties();
      try
      {
         FileInputStream lastConfigInputStream = new FileInputStream(configFile);
         properties.load(lastConfigInputStream);
         lastConfigInputStream.close();
      }
      catch (IOException e)
      {
         // No config file, whatever.
      }

      JLabel userMessageLabel = new JLabel("Select which modules to start:");
      final EnumMap<Modules, JCheckBox> moduleCheckBoxes = new EnumMap<>(Modules.class);

      for (Modules module : Modules.values())
      {
         boolean selected = Boolean.parseBoolean(properties.getProperty(module.getPropertyName(), Boolean.toString(module.getDefaultValue())));
         JCheckBox checkBox = new JCheckBox(module.getName(), selected);
         checkBoxesPanel.add(checkBox);
         moduleCheckBoxes.put(module, checkBox);
      }

      moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).addChangeListener(new ChangeListener()
      {
         @Override
         public void stateChanged(ChangeEvent e)
         {
            boolean isNetworkProcessorSelected = moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).isSelected();
            boolean isNetworkProcessorEnabled = moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).isEnabled();
            moduleCheckBoxes.get(Modules.BEHAVIOR_MODULE).setEnabled(isNetworkProcessorSelected && isNetworkProcessorEnabled);
         }
      });

      moduleCheckBoxes.get(Modules.SIMULATION).addChangeListener(new ChangeListener()
      {
         @Override
         public void stateChanged(ChangeEvent e)
         {
            boolean isSimulationSelected = moduleCheckBoxes.get(Modules.SIMULATION).isSelected();
            moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).setEnabled(isSimulationSelected);
         }
      });

      JComboBox obstacleCourseStartingLocationComboBox = null;

      if (possibleStartingLocations != null && possibleStartingLocations.length > 0)
      {
         JPanel comboBoxPanel = new JPanel(new BorderLayout());
         HashMap<JPanel, JComboBox> comboBoxPanelsMap = new HashMap<JPanel, JComboBox>();
         JLabel selectObstacleCourseLocationLabel = new JLabel("Select a Starting Location: ");
         JPanel locationPanel = new JPanel();
         locationPanel.setLayout(new BoxLayout(locationPanel, BoxLayout.PAGE_AXIS));

         final JPanel obstacleCourseLocationPanel = new JPanel(new BorderLayout());
         obstacleCourseLocationPanel.setVisible(true);
         obstacleCourseStartingLocationComboBox = new JComboBox(possibleStartingLocations);

         obstacleCourseStartingLocationComboBox.setSelectedItem(possibleStartingLocations[0]);
         comboBoxPanelsMap.put(obstacleCourseLocationPanel, obstacleCourseStartingLocationComboBox);

         obstacleCourseLocationPanel.add(selectObstacleCourseLocationLabel, BorderLayout.WEST);
         obstacleCourseLocationPanel.add(obstacleCourseStartingLocationComboBox, BorderLayout.EAST);
         obstacleCourseLocationPanel.setVisible(true);
         locationPanel.add(obstacleCourseLocationPanel);
         comboBoxPanel.add(locationPanel, BorderLayout.CENTER);
         userPromptPanel.add(comboBoxPanel, BorderLayout.NORTH);
      }

      userPromptPanel.add(userMessageLabel, BorderLayout.CENTER);
      userPromptPanel.add(checkBoxesPanel, BorderLayout.SOUTH);

      int selectedOption = JOptionPane.showOptionDialog(null, userPromptPanel, "Select", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
      if (selectedOption != JOptionPane.OK_OPTION)
      {
         System.exit(-1);
      }

      properties = new Properties();
      for (Modules module : Modules.values())
      {
         boolean selected = moduleCheckBoxes.get(module).isSelected();
         boolean enabled = moduleCheckBoxes.get(module).isEnabled();
         if (selected && enabled)
            modulesToStartListToPack.add(module);
         
         properties.setProperty(module.getPropertyName(), String.valueOf(selected && enabled));
      }

      FileOutputStream newConfigOutputStream;
      try
      {
         newConfigOutputStream = new FileOutputStream(configFile);
         properties.store(newConfigOutputStream, "Default configuration for the graphic selector of the DRCSimulationTools");
         newConfigOutputStream.close();
      }
      catch (IOException e)
      {
      }

      if (obstacleCourseStartingLocationComboBox == null)
         return null;
      else
         return (DRCStartingLocation) obstacleCourseStartingLocationComboBox.getSelectedItem();
   }

   public enum Modules
   {
      SIMULATION, OPERATOR_INTERFACE, BEHAVIOR_MODULE, BEHAVIOR_VISUALIZER, NETWORK_PROCESSOR, SENSOR_MODULE;

      public String getPropertyName()
      {
         return "start" + FormattingTools.underscoredToCamelCase(toString(), true);
      }

      public boolean getDefaultValue()
      {
         if (this == SIMULATION || this == OPERATOR_INTERFACE || this == NETWORK_PROCESSOR || this == SENSOR_MODULE)
            return true;
         else
            return false;
      }

      public String getName()
      {
         return FormattingTools.underscoredToCamelCase(toString(), true);
      }
   };
}
