package us.ihmc.footstepPlanning.aStar;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

import us.ihmc.graphics3DDescription.appearance.YoAppearance;
import us.ihmc.graphics3DDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphics3DDescription.yoGraphics.YoGraphicReferenceFrame;
import us.ihmc.graphics3DDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public class FootstepNodeVisualization implements GraphVisualization
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final SimulationConstructionSet scs = new SimulationConstructionSet(new Robot("Dummy"));
   private final YoGraphicsListRegistry graphicsListRegistry = new YoGraphicsListRegistry();
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final DoubleYoVariable time = new DoubleYoVariable("Time", registry);

   private final HashMap<FootstepNode, YoGraphicPosition> activeNodes = new HashMap<>();
   private final HashMap<FootstepNode, YoGraphicPosition> inactiveNodes = new HashMap<>();

   private final IntegerYoVariable nodeCount = new IntegerYoVariable("NodeCount", registry);

   private final Queue<YoGraphicPosition> activeNodeGraphicsQueue = new ArrayDeque<>();
   private final Queue<YoGraphicPosition> inactiveNodeGraphicsQueue = new ArrayDeque<>();

   public FootstepNodeVisualization()
   {
      this(1000, 0.01);
   }

   public FootstepNodeVisualization(int maxNodes, double playbackRate)
   {
      nodeCount.set(0);
      time.set(0.0);

      String listName = getClass().getSimpleName();
      for (int i = 0; i < maxNodes; i++)
      {
         YoFramePoint yoPoint = new YoFramePoint("ActiveNode" + i, worldFrame, registry);
         yoPoint.setToNaN();
         YoGraphicPosition yoGraphic = new YoGraphicPosition("ActiveNode " + i, yoPoint, 0.025, YoAppearance.Green());
         activeNodeGraphicsQueue.add(yoGraphic);
         graphicsListRegistry.registerYoGraphic("Active" + listName, yoGraphic);
      }

      for (int i = 0; i < maxNodes; i++)
      {
         YoFramePoint yoPoint = new YoFramePoint("InactiveNode" + i, worldFrame, registry);
         yoPoint.setToNaN();
         YoGraphicPosition yoGraphic = new YoGraphicPosition("InactiveNode " + i, yoPoint, 0.025, YoAppearance.Red());
         inactiveNodeGraphicsQueue.add(yoGraphic);
         graphicsListRegistry.registerYoGraphic("Inactive" + listName, yoGraphic);
      }

      YoGraphicReferenceFrame worldViz = new YoGraphicReferenceFrame(worldFrame, registry, 0.5);
      graphicsListRegistry.registerYoGraphic(listName, worldViz);

      scs.addYoVariableRegistry(registry);
      scs.addYoGraphicsListRegistry(graphicsListRegistry);
      scs.setPlaybackRealTimeRate(playbackRate);
      scs.setCameraFix(0.0, 0.0, 0.0);
      scs.setCameraPosition(-0.001, 0.0, 15.0);
      scs.tickAndUpdate();
   }

   @Override
   public void addNode(FootstepNode node, boolean active)
   {
      if (nodeExists(node))
      {
         if (isNodeActive(node) == active)
            return;

         if (active)
            setNodeActive(node);
         else
            setNodeInactive(node);
         return;
      }

      addNodeUnsafe(node, active);
   }

   private void addNodeUnsafe(FootstepNode node, boolean active)
   {
      if (active)
      {
         YoGraphicPosition graphics = activeNodeGraphicsQueue.poll();
         setGraphics(graphics, node);
         activeNodes.put(node, graphics);
      }
      else
      {
         YoGraphicPosition graphics = inactiveNodeGraphicsQueue.poll();
         setGraphics(graphics, node);
         inactiveNodes.put(node, graphics);
      }

      nodeCount.increment();
   }

   @Override
   public void setNodeActive(FootstepNode node)
   {
      if (nodeExists(node))
      {
         if (isNodeActive(node))
            return;

         YoGraphicPosition inactiveDisplay = inactiveNodes.remove(node);
         YoGraphicPosition activeDisplay = activeNodeGraphicsQueue.poll();

         hideGraphics(inactiveDisplay);
         setGraphics(activeDisplay, node);

         activeNodes.put(node, activeDisplay);
         inactiveNodeGraphicsQueue.add(inactiveDisplay);
      }
      else
         addNodeUnsafe(node, true);
   }

   @Override
   public void setNodeInactive(FootstepNode node)
   {
      if (nodeExists(node))
      {
         if (!isNodeActive(node))
            return;

         YoGraphicPosition inactiveDisplay = activeNodes.remove(node);
         YoGraphicPosition activeDisplay = inactiveNodeGraphicsQueue.poll();

         hideGraphics(inactiveDisplay);
         setGraphics(activeDisplay, node);

         inactiveNodes.put(node, activeDisplay);
         activeNodeGraphicsQueue.add(inactiveDisplay);
      }
      else
         addNodeUnsafe(node, false);
   }

   @Override
   public boolean nodeExists(FootstepNode node)
   {
      if (activeNodes.containsKey(node))
         return true;
      if (inactiveNodes.containsKey(node))
         return true;
      return false;
   }

   private boolean isNodeActive(FootstepNode node)
   {
      if (activeNodes.containsKey(node))
         return true;
      if (inactiveNodes.containsKey(node))
         return false;
      throw new RuntimeException("Node does not exist.");
   }

   private void setGraphics(YoGraphicPosition graphics, FootstepNode node)
   {
      graphics.setPosition(node.getX(), node.getY(), 0.0);
      graphics.update();
   }

   private void hideGraphics(YoGraphicPosition graphics)
   {
      graphics.setPositionToNaN();
      graphics.update();
   }

   public void showAndSleep(boolean autoplay)
   {
      if (autoplay)
         scs.play();
      scs.startOnAThread();
   }

   @Override
   public void tickAndUpdate()
   {
      scs.setTime(time.getDoubleValue());
      scs.tickAndUpdate();
      time.add(1.0);
   }

   /**
    * Test method that makes a pattern of active and inactive nodes just for demonstration.
    * @param args
    */
   public static void main(String[] args)
   {
      FootstepNodeVisualization viz = new FootstepNodeVisualization(100, 0.01);

      for (int i = 0; i < 10; i++)
      {
         viz.addNode(new FootstepNode(0.05 * i, 0.0), true);
         viz.tickAndUpdate();
      }

      for (int i = 0; i < 10; i++)
      {
         viz.addNode(new FootstepNode(0.05 * i, 0.1), false);
         viz.tickAndUpdate();
      }

      for (int i = 0; i < 10; i++)
      {
         viz.setNodeInactive(new FootstepNode(0.05 * i, 0.0));
         viz.tickAndUpdate();
      }

      for (int i = 0; i < 10; i++)
      {
         viz.setNodeActive(new FootstepNode(0.05 * i, 0.1));
         viz.tickAndUpdate();
      }

      for (int i = 0; i < 10; i++)
      {
         viz.addNode(new FootstepNode(0.05 * i, 0.0), true);
         viz.tickAndUpdate();
      }

      for (int i = 0; i < 10; i++)
      {
         viz.addNode(new FootstepNode(0.05 * i, 0.1), false);
         viz.tickAndUpdate();
      }

      viz.showAndSleep(true);
   }
}
