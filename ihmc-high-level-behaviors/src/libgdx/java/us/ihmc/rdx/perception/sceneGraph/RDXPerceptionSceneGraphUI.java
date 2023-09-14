package us.ihmc.rdx.perception.sceneGraph;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.ImGui;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImBoolean;
import us.ihmc.communication.ros2.ROS2IOTopicQualifier;
import us.ihmc.communication.ros2.ROS2PublishSubscribeAPI;
import us.ihmc.perception.sceneGraph.SceneGraph;
import us.ihmc.perception.sceneGraph.SceneGraphNodeMove;
import us.ihmc.perception.sceneGraph.SceneNode;
import us.ihmc.perception.sceneGraph.ros2.ROS2SceneGraphPublisher;
import us.ihmc.perception.sceneGraph.ros2.ROS2SceneGraphSubscription;
import us.ihmc.rdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.rdx.imgui.RDXPanel;
import us.ihmc.rdx.sceneManager.RDXSceneLevel;
import us.ihmc.rdx.ui.RDX3DPanel;
import us.ihmc.tools.thread.Throttler;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Manages perception scene graph nodes.
 * - It keeps them up to date with a subscription to the on robot perception process.
 * - It renders them in the 3D scene as semi-transparent models.
 * - TODO: It allows the operator to override the poses of nodes.
 */
public class RDXPerceptionSceneGraphUI
{
   private final SceneGraph sceneGraph;
   private final ROS2PublishSubscribeAPI ros2PublishSubscribeAPI;
   private final RDXSceneNode uiRootNode;
   private final ROS2SceneGraphSubscription sceneGraphSubscription;
   private final ROS2SceneGraphPublisher sceneGraphPublisher = new ROS2SceneGraphPublisher();
   private final RDXPanel panel = new RDXPanel("Perception Scene Graph UI", this::renderImGuiWidgets);
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final ImBoolean showGraphics = new ImBoolean(true);
   private final ImBoolean viewAsTree = new ImBoolean(false);
   private final Throttler publishThrottler = new Throttler().setFrequency(30.0);
   private final SortedSet<SceneNode> sceneNodesByID = new TreeSet<>(Comparator.comparingLong(SceneNode::getID));

   public RDXPerceptionSceneGraphUI(SceneGraph sceneGraph,
                                    ROS2PublishSubscribeAPI ros2PublishSubscribeAPI,
                                    RDX3DPanel panel3D)
   {
      this.sceneGraph = sceneGraph;
      this.ros2PublishSubscribeAPI = ros2PublishSubscribeAPI;

      if (sceneGraph.getRootNode() instanceof RDXSceneNode rootNode)
      {
         uiRootNode = rootNode;
      }
      else
      {
         throw new RuntimeException("Scene graph must be created with RDXSceneGraph::new");
      }

      sceneGraphSubscription = new ROS2SceneGraphSubscription
      (
         sceneGraph,
         ros2PublishSubscribeAPI,
         ROS2IOTopicQualifier.STATUS,
         ros2SceneGraphSubscriptionNode -> RDXSceneGraphTools.createNodeFromMessage(ros2SceneGraphSubscriptionNode, panel3D, sceneGraph)
      );
   }

   public void update()
   {
      sceneGraphSubscription.update();

      // Nodes can be moved by the user clicking stuff
      sceneGraph.getSceneGraphNodeMoves().clear();
      sceneNodesByID.clear();

      update(uiRootNode);

      for (SceneGraphNodeMove sceneGraphNodeMove : sceneGraph.getSceneGraphNodeMoves())
      {
         sceneGraphNodeMove.performMove();
      }

      if (publishThrottler.run())
         sceneGraphPublisher.publish(sceneGraph, ros2PublishSubscribeAPI, ROS2IOTopicQualifier.COMMAND);
   }

   private void update(RDXSceneNodeInterface uiSceneNode)
   {
      uiSceneNode.update(sceneGraph.getSceneGraphNodeMoves());

      if (uiSceneNode instanceof SceneNode sceneNode)
      {
         sceneNodesByID.add(sceneNode);
      }

      if (uiSceneNode instanceof SceneNode sceneNode)
      {
         for (SceneNode child : sceneNode.getChildren())
         {
            if (child instanceof RDXSceneNodeInterface uiChildNode)
            {
               update(uiChildNode);
            }
         }
      }
   }

   private void renderSceneNodeTree(SceneNode sceneNode, int level)
   {
      if (sceneNode instanceof RDXSceneNodeInterface uiSceneNode)
      {
         ImGui.setCursorPos(ImGui.getCursorPosX() + 20f + (level * 2), ImGui.getCursorPosY());
         {
            ImGui.pushStyleVar(ImGuiStyleVar.ChildBorderSize, 1.0f);
            ImGui.beginGroup();
            uiSceneNode.renderImGuiWidgets();
            for (SceneNode child : sceneNode.getChildren())
            {
               renderSceneNodeTree(child, ++level);
            }
            ImGui.endGroup();
            ImGui.popStyleVar();
         }
      }
   }

   public void renderImGuiWidgets()
   {
      ImGui.text("Scene graph updates received: " + sceneGraphSubscription.getNumberOfMessagesReceived());
      ImGui.checkbox(labels.get("Show graphics"), showGraphics);
      ImGui.checkbox(labels.get("View as tree"), viewAsTree);
      ImGui.separator();

      if (viewAsTree.get())
      {
         renderSceneNodeTree(sceneGraph.getRootNode(), 0);
      }
      else
      {
         for (SceneNode sceneNode : sceneNodesByID)
         {
            if (sceneNode instanceof RDXSceneNodeInterface uiSceneNode)
            {
               uiSceneNode.renderImGuiWidgets();
               ImGui.separator();
            }
         }
      }
   }

   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool, Set<RDXSceneLevel> sceneLevels)
   {
      if (showGraphics.get())
      {
         getRenderables(uiRootNode, renderables, pool, sceneLevels);
      }
   }

   private void getRenderables(RDXSceneNodeInterface uiSceneNode, Array<Renderable> renderables, Pool<Renderable> pool, Set<RDXSceneLevel> sceneLevels)
   {
      uiSceneNode.getRenderables(renderables, pool, sceneLevels);

      if (uiSceneNode instanceof SceneNode sceneNode)
      {
         for (SceneNode child : sceneNode.getChildren())
         {
            if (child instanceof RDXSceneNodeInterface uiChildNode)
            {
               getRenderables(uiChildNode, renderables, pool, sceneLevels);
            }
         }
      }
   }

   public RDXPanel getPanel()
   {
      return panel;
   }
}