package us.ihmc.rdx.ui.graphics.live;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import perception_msgs.msg.dds.PlanarRegionsListMessage;
import imgui.internal.ImGui;
import us.ihmc.communication.IHMCROS2Callback;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.rdx.imgui.ImGuiPlot;
import us.ihmc.rdx.sceneManager.RDXSceneLevel;
import us.ihmc.rdx.ui.visualizers.ImGuiFrequencyPlot;
import us.ihmc.rdx.ui.visualizers.RDXVisualizer;
import us.ihmc.rdx.visualizers.RDXPlanarRegionsGraphic;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.ROS2NodeInterface;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.tools.thread.MissingThreadTools;

import us.ihmc.tools.thread.ResettableExceptionHandlingExecutorService;

import java.util.Set;

public class RDXROS2PlanarRegionsVisualizer extends RDXVisualizer
{
   private final RDXPlanarRegionsGraphic planarRegionsGraphic = new RDXPlanarRegionsGraphic();
   private final ResettableExceptionHandlingExecutorService executorService;
   private final ROS2Topic<PlanarRegionsListMessage> topic;

   private final ImGuiFrequencyPlot frequencyPlot = new ImGuiFrequencyPlot();
   private final ImGuiPlot numberOfRegionsPlot = new ImGuiPlot("# Regions", 1000, 230, 20);
   private int numberOfPlanarRegions = 0;

   public RDXROS2PlanarRegionsVisualizer(String title, ROS2NodeInterface ros2Node, ROS2Topic<PlanarRegionsListMessage> topic)
   {
      super(title + " (ROS 2)");
      this.topic = topic;
      new IHMCROS2Callback<>(ros2Node, topic, this::acceptMessage);

      boolean daemon = true;
      int queueSize = 1;
      executorService = MissingThreadTools.newSingleThreadExecutor(getClass().getSimpleName(), daemon, queueSize);
   }

   private void acceptMessage(PlanarRegionsListMessage planarRegionsListMessage)
   {
      frequencyPlot.recordEvent();
      if (isActive())
      {
         executorService.clearQueueAndExecute(() ->
         {
            PlanarRegionsList planarRegionsList = PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListMessage);
            numberOfPlanarRegions = planarRegionsList.getNumberOfPlanarRegions();
            planarRegionsGraphic.generateMeshes(planarRegionsList);
         });
      }
   }

   @Override
   public void setActive(boolean active)
   {
      super.setActive(active);
      if (!isActive())
      {
         executorService.interruptAndReset();
      }
   }

   @Override
   public void renderImGuiWidgets()
   {
      super.renderImGuiWidgets();
      if (!isActive())
      {
         executorService.interruptAndReset();
      }
      ImGui.text(topic.getName());
      frequencyPlot.renderImGuiWidgets();
      numberOfRegionsPlot.render(numberOfPlanarRegions);
   }

   @Override
   public void update()
   {
      super.update();
      if (isActive())
      {
         planarRegionsGraphic.update();
      }
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool, Set<RDXSceneLevel> sceneLevels)
   {
      if (isActive() && sceneLevelCheck(sceneLevels))
      {
         planarRegionsGraphic.getRenderables(renderables, pool);
      }
   }

   public void destroy()
   {
      planarRegionsGraphic.destroy();
   }
}
