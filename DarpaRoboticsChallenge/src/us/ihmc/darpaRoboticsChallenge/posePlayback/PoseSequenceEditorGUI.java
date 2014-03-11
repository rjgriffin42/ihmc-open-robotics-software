package us.ihmc.darpaRoboticsChallenge.posePlayback;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class PoseSequenceEditorGUI extends JFrame
{
   private final PoseSequenceSelectorPanel poseSequenceSelectorPanel;
   private final ButtonPanel buttonPanel;

   public PoseSequenceEditorGUI(DRCRobotModel robotModel)
   {
      super("Pose sequence editor");
      setSize(1400, 600);
      poseSequenceSelectorPanel = new PoseSequenceSelectorPanel(robotModel);
      buttonPanel = new ButtonPanel();
      
      buttonPanelInit();
   }

   public PoseSequenceEditorGUI(YoVariableRegistry registry,PosePlaybackAllJointsController posePlaybackController,SDFRobot sdfRobot,DRCRobotMidiSliderBoardPositionManipulation sliderBoard)
   {
      super("Pose sequence editor");
      setSize(1400, 600);
      poseSequenceSelectorPanel = new PoseSequenceSelectorPanel(registry,posePlaybackController,sdfRobot,sliderBoard);
      buttonPanel = new ButtonPanel();
      
      buttonPanelInit();       
   }
   
   private void buttonPanelInit()
   {
      getContentPane().add(new JointNameKey(), BorderLayout.NORTH);
      getContentPane().add(poseSequenceSelectorPanel, BorderLayout.CENTER);
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);
   }
   
   private class ButtonPanel extends JPanel implements ActionListener 
   { 
      private JButton selectNewPoseSequence, selectPoseSequence, deleteRow, updateSCS, setRowWithSlider, save, switchSideDependentValues, copyAndInsertRow,
            insertInterpolation;
      
      public ButtonPanel()
      {
         JPanel buttonPanel = new JPanel();
         
         selectNewPoseSequence = new JButton("Select new pose sequence");
         selectPoseSequence = new JButton("Select pose sequence to append");
         deleteRow = new JButton("Delete"); 
         updateSCS = new JButton("Update SCS");
         setRowWithSlider = new JButton("Set row with slider");
         save = new JButton("Save");
         copyAndInsertRow = new JButton("Copy/insert row");
         switchSideDependentValues = new JButton("Switch side dependent values");
         insertInterpolation = new JButton("Insert interpolated row");
         
         buttonPanel.add(selectNewPoseSequence);
         buttonPanel.add(selectPoseSequence);
         buttonPanel.add(copyAndInsertRow);
         buttonPanel.add(insertInterpolation);
         buttonPanel.add(deleteRow);
         buttonPanel.add(updateSCS);
         buttonPanel.add(setRowWithSlider);
         buttonPanel.add(switchSideDependentValues);
         buttonPanel.add(save);
         
         selectNewPoseSequence.addActionListener(this);
         copyAndInsertRow.addActionListener(this);
         insertInterpolation.addActionListener(this);
         selectPoseSequence.addActionListener(this);
         deleteRow.addActionListener(this);
         updateSCS.addActionListener(this);
         setRowWithSlider.addActionListener(this);
         save.addActionListener(this);
         switchSideDependentValues.addActionListener(this);
                  
         setLayout(new BorderLayout());
         add(buttonPanel, BorderLayout.SOUTH);

      }

      public void actionPerformed(ActionEvent e)
      {
         if(e.getSource().equals(selectPoseSequence))
            poseSequenceSelectorPanel.addSequenceFromFile();
         
         else if(e.getSource().equals(deleteRow))
            poseSequenceSelectorPanel.deleteSelectedRows();
         
         else if(e.getSource().equals(updateSCS))
            poseSequenceSelectorPanel.updateSCS();
         
         else if(e.getSource().equals(setRowWithSlider))
            poseSequenceSelectorPanel.setRowWithSlider();
         
         else if(e.getSource().equals(save))
            poseSequenceSelectorPanel.save();
         
         else if(e.getSource().equals(selectNewPoseSequence))
            poseSequenceSelectorPanel.newSequenceFromFile();
         
         else if(e.getSource().equals(copyAndInsertRow))
            poseSequenceSelectorPanel.copyAndInsertRow();
         
         else if(e.getSource().equals(insertInterpolation))
            poseSequenceSelectorPanel.insertInterpolation();
         
         else if(e.getSource().equals(switchSideDependentValues))
            poseSequenceSelectorPanel.switchSideDependentValues();
      }
   }
   
   public void setSequence(PosePlaybackRobotPoseSequence seq)
   {
      poseSequenceSelectorPanel.setSequence(seq);
   }
   
   public void addSequence(PosePlaybackRobotPoseSequence seq)
   {
      poseSequenceSelectorPanel.addSequence(seq);
   }
   
   private class JointNameKey extends JPanel
   {
      public JointNameKey()
      {
         String key = "Joint name    =   (left, right)   +   (ankle, elbow, hip, knee, neck, shoulder, spine, wrist)   +   (yaw, pitch, roll)";
         JLabel jointNameKey = new JLabel(key);
         add("key", jointNameKey);
      }
   }

   
   public static void main(String[] args)
   {
      // Flag to set robot model
      JSAP jsap = new JSAP();
      FlaggedOption robotModel = new FlaggedOption("robotModel").setLongFlag("model").setShortFlag('m').setRequired(true).setStringParser(JSAP.STRING_PARSER);
      robotModel.setHelp("Robot models: " + Arrays.toString(DRCRobotModel.values()));
      
      DRCRobotModel model;
      try
      {
         jsap.registerParameter(robotModel);

         JSAPResult config = jsap.parse(args);

         if (config.success())
         {
            model = DRCRobotModel.valueOf(config.getString("robotModel"));
         }
         else
         {
            System.out.println("Enter a robot model.");
            return;
         }
      }
      catch (Exception e)
      {
         System.out.println("Robot model not found");
         e.printStackTrace();
         return;
      }
      
      PoseSequenceEditorGUI scriptedEditorGUI = new PoseSequenceEditorGUI(model);
      scriptedEditorGUI.setDefaultCloseOperation(EXIT_ON_CLOSE);
      scriptedEditorGUI.setVisible(true);
   }
}

