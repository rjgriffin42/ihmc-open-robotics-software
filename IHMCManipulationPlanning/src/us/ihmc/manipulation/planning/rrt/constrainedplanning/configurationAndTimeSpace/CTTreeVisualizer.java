package us.ihmc.manipulation.planning.rrt.constrainedplanning.configurationAndTimeSpace;

import java.util.ArrayList;

public class CTTreeVisualizer
{
   private int configurationDimension;
   
   private ArrayList<CTNodeVisualizer> nodeVisualizers;
   
   public CTTreeVisualizer(CTTaskNodeTree tree)
   {
      this.configurationDimension = tree.getDimensionOfTask();
      
      this.nodeVisualizers = new ArrayList<CTNodeVisualizer>();
      for(int i=0;i<configurationDimension;i++)
      {
         CTNodeVisualizer nodeVisualizer = new CTNodeVisualizer(tree.getTaskName(i+1), i+1, tree.getTaskNodeRegion().isEnable(i+1));
         this.nodeVisualizers.add(nodeVisualizer);
      }
   }
   
   public void initialize()
   {
      for(int i=0;i<configurationDimension;i++)
      {
         nodeVisualizers.get(i).initialize();
      }
   }
   
   public void update(CTTaskNode newNode)
   {
      for(int i=0;i<configurationDimension;i++)
      {
         nodeVisualizers.get(i).updateVisualizer(newNode);
      }
   }
}
