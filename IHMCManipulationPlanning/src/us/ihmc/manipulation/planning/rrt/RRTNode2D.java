package us.ihmc.manipulation.planning.rrt;

public class RRTNode2D extends RRTNode
{
   static double boxCenterX = 2.0;
   static double boxCenterY = 1.0;
   static double boxSizeX = 1.0;
   static double boxSizeY = 1.0;
   
   public RRTNode2D()
   {
      super(2);
   }
   
   public RRTNode2D(double px, double py)
   {
      super(2);
      super.setNodeData(0, px);
      super.setNodeData(1, py);
   }
   
   @Override
   public boolean isValidNode()
   {
      double px = this.getNodeData(0);
      double py = this.getNodeData(1);
      if ((px < (boxCenterX + 0.5*boxSizeX) && px > (boxCenterX - 0.5*boxSizeX)) && (py < (boxCenterY + 0.5*boxSizeY)) && (py > (boxCenterY - 0.5*boxSizeY)))
      {
         return false;
      }
      
      return true;
   }

   @Override
   public RRTNode createNode()
   {
      return new RRTNode2D();
   }

}
