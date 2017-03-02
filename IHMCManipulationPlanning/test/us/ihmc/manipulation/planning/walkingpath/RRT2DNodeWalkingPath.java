package us.ihmc.manipulation.planning.walkingpath;

import us.ihmc.manipulation.planning.rrt.RRTNode;

public class RRT2DNodeWalkingPath extends RRTNode
{
   public RRT2DNodeWalkingPath()
   {
      super(2);
   }
   
   public RRT2DNodeWalkingPath(double px, double py)
   {
      super(2);
      super.setNodeData(0, px);
      super.setNodeData(1, py);
   }
   
   @Override
   public boolean isValidNode()
   {
      return true;
   }

   @Override
   public RRTNode createNode()
   {
      return  new RRT2DNodeWalkingPath();
   }

}
