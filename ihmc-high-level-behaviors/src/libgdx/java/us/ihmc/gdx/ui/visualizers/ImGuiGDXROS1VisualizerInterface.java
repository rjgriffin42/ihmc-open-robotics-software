package us.ihmc.gdx.ui.visualizers;

import us.ihmc.utilities.ros.RosNodeInterface;

public interface ImGuiGDXROS1VisualizerInterface
{
   public abstract void subscribe(RosNodeInterface ros1Node);

   public abstract void unsubscribe(RosNodeInterface ros1Node);

   public void updateSubscribers(RosNodeInterface ros1Node);
}