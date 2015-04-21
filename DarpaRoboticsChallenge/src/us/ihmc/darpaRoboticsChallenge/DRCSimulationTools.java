package us.ihmc.darpaRoboticsChallenge;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalExclusionType;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.utilities.FormattingTools;
import us.ihmc.utilities.ThreadTools;

public abstract class DRCSimulationTools
{

   @SuppressWarnings({ "hiding", "unchecked" })
   public static <T extends DRCStartingLocation, Enum> void startSimulationWithGraphicSelector(DRCSimulationStarter simulationStarter,
         T... possibleStartingLocations)
   {
      List<Modules> modulesToStart = new ArrayList<Modules>();
      DRCStartingLocation startingLocation = null;

      startingLocation = showSelectorWithStartingLocation(modulesToStart, possibleStartingLocations);

      if (startingLocation != null)
         simulationStarter.setStartingLocation(startingLocation);

      if (modulesToStart.isEmpty())
         return;
      else if (modulesToStart.size() == 1)
         simulationStarter.setSpawnOperatorInterfaceInDifferentProcess(false);

      boolean automaticallyStartSimulation = true;
      DRCNetworkModuleParameters networkProcessorParameters;
      if (modulesToStart.contains(Modules.NETWORK_PROCESSOR))
      {
         networkProcessorParameters = new DRCNetworkModuleParameters();
         networkProcessorParameters.enableUiModule(true);
         networkProcessorParameters.enableBehaviorModule(modulesToStart.contains(Modules.BEHAVIOR_MODULE));
         networkProcessorParameters.enableBehaviorVisualizer(modulesToStart.contains(Modules.BEHAVIOR_MODULE));
         networkProcessorParameters.enableSensorModule(modulesToStart.contains(Modules.SENSOR_MODULE));
         networkProcessorParameters.enablePerceptionModule(true);
         networkProcessorParameters.enableRosModule(modulesToStart.contains(Modules.ROS_MODULE));
         networkProcessorParameters.enableLocalControllerCommunicator(true);
      }
      else
      {
         networkProcessorParameters = null;
      }

      if (modulesToStart.contains(Modules.SIMULATION))
         simulationStarter.startSimulation(networkProcessorParameters, automaticallyStartSimulation);

      if (modulesToStart.contains(Modules.OPERATOR_INTERFACE))
      {
         simulationStarter.setSpawnOperatorInterfaceInDifferentProcess(modulesToStart.contains(Modules.SIMULATION));

         simulationStarter.startOpertorInterfaceUsingProcessSpawner();
      }

      if (modulesToStart.contains(Modules.BEHAVIOR_VISUALIZER))
         simulationStarter.startBehaviorVisualizer();

      if (modulesToStart.contains(Modules.SPECTATOR_INTERFACE))
         simulationStarter.startSpectatorInterface();
   }

   @SuppressWarnings({ "hiding", "unchecked", "rawtypes", "serial" })
   private static <T extends DRCStartingLocation, Enum> DRCStartingLocation showSelectorWithStartingLocation(List<Modules> modulesToStartListToPack,
         T... possibleStartingLocations)
   {
      JPanel userPromptPanel = new JPanel(new BorderLayout());
      JPanel checkBoxesPanel = new JPanel(new GridLayout(2, 4));

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
         boolean enabled;
         if (module.isAlwaysEnabled()) // So Simulation, operator and spectator interfaces, and behavior visualizer can never get disabled
            enabled = true;
         else
            enabled = Boolean.parseBoolean(properties.getProperty(module.getPropertyNameForEnable(), Boolean.toString(module.getDefaultValueForEnable())));
         boolean selected = Boolean.parseBoolean(properties.getProperty(module.getPropertyNameForSelected(), Boolean.toString(module.getDefaultValueForSelected())));
         JCheckBox checkBox = new JCheckBox(module.getName());
         checkBox.setSelected(selected);
         checkBox.setEnabled(enabled);
         checkBoxesPanel.add(checkBox);
         moduleCheckBoxes.put(module, checkBox);
      }

      ChangeListener networkProcessorCheckBoxChangeListener = new ChangeListener()
      {
         @Override
         public void stateChanged(ChangeEvent e)
         {
            boolean isNetworkProcessorSelected = moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).isSelected();
            boolean isNetworkProcessorEnabled = moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).isEnabled();
            moduleCheckBoxes.get(Modules.BEHAVIOR_MODULE).setEnabled(isNetworkProcessorSelected && isNetworkProcessorEnabled);
            moduleCheckBoxes.get(Modules.SENSOR_MODULE).setEnabled(isNetworkProcessorSelected && isNetworkProcessorEnabled);
            moduleCheckBoxes.get(Modules.ROS_MODULE).setEnabled(isNetworkProcessorSelected && isNetworkProcessorEnabled);
         }
      };

      ChangeListener simulationCheckBoxChangeListener = new ChangeListener()
      {
         @Override
         public void stateChanged(ChangeEvent e)
         {
            boolean isSimulationSelected = moduleCheckBoxes.get(Modules.SIMULATION).isSelected();
            moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).setEnabled(isSimulationSelected);
         }
      };
      moduleCheckBoxes.get(Modules.NETWORK_PROCESSOR).addChangeListener(networkProcessorCheckBoxChangeListener);
      moduleCheckBoxes.get(Modules.SIMULATION).addChangeListener(simulationCheckBoxChangeListener);

      // Call the listeners
      networkProcessorCheckBoxChangeListener.stateChanged(null);
      simulationCheckBoxChangeListener.stateChanged(null);

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

      final JFrame frame = new JFrame("Launch");
      frame.setIconImage(new ImageIcon(DRCSimulationTools.class.getClassLoader().getResource("running-man-32x32-Launch.png")).getImage());
      frame.setLayout(new BorderLayout());
      frame.add(userPromptPanel, BorderLayout.CENTER);
      JPanel optionPanel = new JPanel();
      optionPanel.add(new JButton(new AbstractAction("Okay")
      {
         @Override
         public void actionPerformed(ActionEvent arg0)
         {
            frame.dispose();
            frame.setEnabled(false);
         }
      }));
      optionPanel.add(new JButton(new AbstractAction("Cancel")
      {
         @Override
         public void actionPerformed(ActionEvent e)
         {
            System.exit(-1);
         }
      }));
      frame.add(optionPanel, BorderLayout.SOUTH);
      frame.pack();
      frame.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
      frame.setLocationRelativeTo(null);
      SwingUtilities.invokeLater(new Runnable()
      {
         @Override
         public void run()
         {
            frame.setVisible(true);
         }
      });
      
      while (frame.isEnabled())
         ThreadTools.sleep(50);
      
//      int selectedOption = JOptionPane.showOptionDialog(null, userPromptPanel, "Select", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
//            null, null);
//      if (selectedOption != JOptionPane.OK_OPTION)
//      {
//         System.exit(-1);
//      }

      properties = new Properties();
      for (Modules module : Modules.values())
      {
         boolean selected = moduleCheckBoxes.get(module).isSelected();
         boolean enabled = moduleCheckBoxes.get(module).isEnabled();
         if (selected && enabled)
            modulesToStartListToPack.add(module);

         properties.setProperty(module.getPropertyNameForEnable(), String.valueOf(enabled));
         properties.setProperty(module.getPropertyNameForSelected(), String.valueOf(selected));
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
      SIMULATION, OPERATOR_INTERFACE, SPECTATOR_INTERFACE, BEHAVIOR_VISUALIZER, NETWORK_PROCESSOR, SENSOR_MODULE, ROS_MODULE, BEHAVIOR_MODULE;

      public String getPropertyNameForEnable()
      {
         return "enable" + FormattingTools.underscoredToCamelCase(toString(), true);
      }

      public String getPropertyNameForSelected()
      {
         return "select" + FormattingTools.underscoredToCamelCase(toString(), true);
      }

      public boolean isAlwaysEnabled()
      {
         if (this == SIMULATION || this == OPERATOR_INTERFACE || this == BEHAVIOR_VISUALIZER || this == SPECTATOR_INTERFACE)
            return true;
         else
            return false;
      }

      public boolean getDefaultValueForEnable()
      {
         return true;
      }

      public boolean getDefaultValueForSelected()
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
