package us.ihmc.robotEnvironmentAwareness.slam.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Material;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.util.Pair;
import us.ihmc.robotEnvironmentAwareness.communication.REAModuleAPI;
import us.ihmc.robotEnvironmentAwareness.communication.REAUIMessager;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools.ExceptionHandling;
import us.ihmc.robotEnvironmentAwareness.ui.graphicsBuilders.PlanarRegionsMeshBuilder;
import us.ihmc.robotEnvironmentAwareness.ui.graphicsBuilders.SLAMOcTreeMeshBuilder;

public class IhmcSLAMMeshViewer
{
   private static final int SLOW_PACE_UPDATE_PERIOD = 2000;
   private static final int MEDIUM_PACE_UPDATE_PERIOD = 100;
   private static final int HIGH_PACE_UPDATE_PERIOD = 10;

   private final Group root = new Group();

   private final List<ScheduledFuture<?>> meshBuilderScheduledFutures = new ArrayList<>();
   private final MeshView planarRegionMeshView = new MeshView();

   private ScheduledExecutorService executorService = ExecutorServiceTools.newScheduledThreadPool(3, getClass(), ExceptionHandling.CANCEL_AND_REPORT);
   private final AnimationTimer renderMeshAnimation;

   private final PlanarRegionsMeshBuilder planarRegionsMeshBuilder;
   private final SLAMOcTreeMeshBuilder ocTreeViewer;

   private final List<AtomicReference<Boolean>> enableTopicList = new ArrayList<>();
   private final Map<AtomicReference<Boolean>, Node> enableTopicToNode = new HashMap<>();

   public IhmcSLAMMeshViewer(REAUIMessager uiMessager)
   {
      planarRegionsMeshBuilder = new PlanarRegionsMeshBuilder(uiMessager, REAModuleAPI.SLAMPlanarRegionsState, REAModuleAPI.ShowPlanarRegionsMap,
                                                              REAModuleAPI.SLAMVizClear, REAModuleAPI.SLAMClear);

      ocTreeViewer = new SLAMOcTreeMeshBuilder(uiMessager, REAModuleAPI.ShowSLAMOctreeMap, REAModuleAPI.SLAMClear, REAModuleAPI.SLAMOctreeMapState,
                                               REAModuleAPI.SLAMOcTreeDisplayType);

      ocTreeViewer.getRoot().setMouseTransparent(true);
      root.getChildren().addAll(planarRegionMeshView, ocTreeViewer.getRoot());

      AtomicReference<Boolean> planarRegionEnable = uiMessager.createInput(REAModuleAPI.ShowPlanarRegionsMap, false);
      AtomicReference<Boolean> ocTreeEnable = uiMessager.createInput(REAModuleAPI.ShowSLAMOctreeMap, false);
      enableTopicToNode.put(planarRegionEnable, planarRegionMeshView);
      enableTopicToNode.put(ocTreeEnable, ocTreeViewer.getRoot());
      enableTopicList.add(planarRegionEnable);
      enableTopicList.add(ocTreeEnable);

      renderMeshAnimation = new AnimationTimer()
      {
         @Override
         public void handle(long now)
         {
            ocTreeViewer.render();

            if (planarRegionsMeshBuilder.hasNewMeshAndMaterial())
               updateMeshView(planarRegionMeshView, planarRegionsMeshBuilder.pollMeshAndMaterial());
         }
      };

      uiMessager.registerModuleMessagerStateListener(isMessagerOpen -> {
         if (isMessagerOpen)
            start();
         else
            stop();
      });
   }

   private Runnable createViewersController()
   {
      return new Runnable()
      {
         @Override
         public void run()
         {
            Platform.runLater(new Runnable()
            {
               @Override
               public void run()
               {
                  for (int i = 0; i < enableTopicList.size(); i++)
                  {
                     AtomicReference<Boolean> enable = enableTopicList.get(i);
                     Node node = enableTopicToNode.get(enable);
                     if (enable.get())
                     {
                        if (!root.getChildren().contains(node))
                           root.getChildren().addAll(node);
                     }
                     else
                     {
                        if (root.getChildren().contains(node))
                           root.getChildren().removeAll(node);
                     }
                  }
               }

            });

         }

      };
   }

   public void start()
   {
      if (!meshBuilderScheduledFutures.isEmpty())
         return;
      renderMeshAnimation.start();
      meshBuilderScheduledFutures.add(executorService.scheduleAtFixedRate(planarRegionsMeshBuilder, 0, MEDIUM_PACE_UPDATE_PERIOD, TimeUnit.MILLISECONDS));
      meshBuilderScheduledFutures.add(executorService.scheduleAtFixedRate(ocTreeViewer, 0, MEDIUM_PACE_UPDATE_PERIOD, TimeUnit.MILLISECONDS));
      meshBuilderScheduledFutures.add(executorService.scheduleAtFixedRate(createViewersController(), 0, MEDIUM_PACE_UPDATE_PERIOD, TimeUnit.MILLISECONDS));
   }

   public void sleep()
   {
      if (meshBuilderScheduledFutures.isEmpty())
         return;
      renderMeshAnimation.stop();
      for (ScheduledFuture<?> scheduledFuture : meshBuilderScheduledFutures)
         scheduledFuture.cancel(true);
      meshBuilderScheduledFutures.clear();
   }

   public void stop()
   {
      sleep();

      if (executorService != null)
      {
         executorService.shutdownNow();
         executorService = null;
      }
   }

   private void updateMeshView(MeshView meshViewToUpdate, Pair<Mesh, Material> meshMaterial)
   {
      meshViewToUpdate.setMesh(meshMaterial.getKey());
      meshViewToUpdate.setMaterial(meshMaterial.getValue());
   }

   public Node getRoot()
   {
      return root;
   }
}
