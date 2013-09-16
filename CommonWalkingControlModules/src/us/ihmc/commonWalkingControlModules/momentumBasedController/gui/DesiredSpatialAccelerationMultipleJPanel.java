package us.ihmc.commonWalkingControlModules.momentumBasedController.gui;


import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.DesiredSpatialAccelerationCommandAndMotionConstraint;

public class DesiredSpatialAccelerationMultipleJPanel extends AbstractMultipleReusableJPanel<DesiredSpatialAccelerationJPanel>
{
   private static final long serialVersionUID = -4892971095902174475L;
   private final JTable jTable;
   private final TableTools tableTools;
   private DefaultTableModel model;
   
   public DesiredSpatialAccelerationMultipleJPanel()
   {
//    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//      this.setLayout(new GridLayout(39, 1));
//      this.setLayout(new GridLayout(39, 1));
      this.tableTools = new TableTools();
      ArrayList<String> columnHeader = new ArrayList<String>();
      columnHeader.add("BodyName");columnHeader.add("BaseName");columnHeader.add("DesiredSpatialAcceleration");
      columnHeader.add("desiredSpatialAcceleration2");columnHeader.add("AchievedSpatialAcceleration");columnHeader.add("errorSpatialAcceleration");
      
      jTable = tableTools.createJTableModelForDesiredJointAcceleration(columnHeader, 4);
      model = tableTools.getModel();
      tableTools.setPreferredWidth(0, 80);
      tableTools.setPreferredWidth(1, 80);
      tableTools.setPreferredWidth(2, 297);
      tableTools.setPreferredWidth(3, 297); 
      tableTools.setPreferredWidth(4, 297); 
      tableTools.setPreferredWidth(5, 297); 
      this.repaint();
   }

   public synchronized void setDesiredSpatialAccelerations(ArrayList<DesiredSpatialAccelerationCommandAndMotionConstraint> desiredSpatialAccelerationCommandAndMotionConstraints)
   {
//      rearrangePanelsIfNecessary(desiredSpatialAccelerationCommandAndMotionConstraints.size() );

      for (int i = 0; i < desiredSpatialAccelerationCommandAndMotionConstraints.size(); i++)
      {
         DesiredSpatialAccelerationCommandAndMotionConstraint desiredSpatialAccelerationCommandAndMotionConstraint = desiredSpatialAccelerationCommandAndMotionConstraints.get(i);
         
         DesiredSpatialAccelerationJPanel desiredJointAccelerationJPanel = new DesiredSpatialAccelerationJPanel();

         desiredJointAccelerationJPanel.setDesiredSpatialAccelerationCommand(desiredSpatialAccelerationCommandAndMotionConstraint);
         writeDataToJointTable(i ,desiredJointAccelerationJPanel);
      }

      this.repaint();
   }

   private void writeDataToJointTable(int rowCount, DesiredSpatialAccelerationJPanel desiredJointAccelerationJPanel)
   {
      model.setValueAt(" " + desiredJointAccelerationJPanel.getBodyName(), rowCount + 1, 0);
      model.setValueAt(" " + desiredJointAccelerationJPanel.getBaseName(), rowCount + 1, 1);
      model.setValueAt(" " + desiredJointAccelerationJPanel.getDesiredSpatialAcceleration(), rowCount + 1, 2);
      model.setValueAt(" " + desiredJointAccelerationJPanel.getDesiredSpatialAcceleration2(), rowCount + 1, 3);
      model.setValueAt(" " + desiredJointAccelerationJPanel.getAchievedSpatialAcceleration(), rowCount + 1, 4);
      model.setValueAt(" " + desiredJointAccelerationJPanel.getErrorSpatialAcceleration(), rowCount + 1, 5);
   }
   
   public DesiredSpatialAccelerationJPanel constructNewJPanel()
   {
      return new DesiredSpatialAccelerationJPanel();
   }

   public synchronized void paintComponent(Graphics g)
   {
      super.paintComponent(g);
      add(jTable);
   }

}

