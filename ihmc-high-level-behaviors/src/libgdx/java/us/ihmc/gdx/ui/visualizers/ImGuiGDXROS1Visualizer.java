package us.ihmc.gdx.ui.visualizers;

import us.ihmc.utilities.ros.RosNodeInterface;

public abstract class ImGuiGDXROS1Visualizer extends ImGuiGDXVisualizer
{
   public ImGuiGDXROS1Visualizer(String title)
   {
      super(title + " (ROS 1)");
   }

   public abstract void subscribe(RosNodeInterface ros1Node);

   public abstract void unsubscribe(RosNodeInterface ros1Node);
}
