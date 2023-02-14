package us.ihmc.rdx.perception;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.internal.ImGui;
import imgui.type.ImBoolean;
import org.bytedeco.javacpp.BytePointer;
import perception_msgs.msg.dds.HeightMapMessage;
import perception_msgs.msg.dds.SteppableRegionDebugImageMessage;
import perception_msgs.msg.dds.SteppableRegionDebugImagesMessage;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.ihmcPerception.steppableRegions.SteppableRegion;
import us.ihmc.ihmcPerception.steppableRegions.SteppableRegionsListCollection;
import us.ihmc.log.LogTools;
import us.ihmc.perception.steppableRegions.SteppableRegionCalculatorParameters;
import us.ihmc.ihmcPerception.steppableRegions.SteppableRegionsCalculationModule;
import us.ihmc.perception.BytedecoImage;
import us.ihmc.perception.OpenCLManager;
import us.ihmc.rdx.imgui.ImGuiPanel;
import us.ihmc.rdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.rdx.ui.ImGuiStoredPropertySetTuner;
import us.ihmc.rdx.visualizers.RDXSteppableRegionGraphic;
import us.ihmc.sensorProcessing.heightMap.HeightMapData;
import us.ihmc.sensorProcessing.heightMap.HeightMapMessageTools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RDXSteppableRegionsCalculatorUI
{
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final ImBoolean enabled = new ImBoolean(false);

   private final SteppableRegionsCalculationModule steppableRegionsCalculationModule = new SteppableRegionsCalculationModule();

   private final AtomicReference<SteppableRegionsListCollection> latestSteppableRegionsToRender = new AtomicReference<>();
   private final AtomicReference<SteppableRegionDebugImagesMessage> latestSteppableRegionDebugImagesToRender = new AtomicReference<>();

   private final AtomicReference<HeightMapMessage> heightMapMessageReference = new AtomicReference<>();

   private final ImBoolean drawPatches = new ImBoolean(true);


   private ImGuiPanel imguiPanel;
   private final List<RDXCVImagePanel> steppabilityPanels = new ArrayList<>();
   private final List<RDXCVImagePanel> steppableRegionsPanels = new ArrayList<>();

   private ImGuiStoredPropertySetTuner parameterTuner;
   private RDXSteppableRegionGraphic steppableRegionGraphic;
   private int cellsPerSide;
   private final SteppableRegionCalculatorParameters parameters = new SteppableRegionCalculatorParameters();

   public void create()
   {
      imguiPanel = new ImGuiPanel("Steppable Region Extraction", this::renderImGuiWidgets);
      parameterTuner = new ImGuiStoredPropertySetTuner("Steppable Region Parameters");
      parameterTuner.create(parameters);

      cellsPerSide = 100;
      for (int i = 0; i < SteppableRegionsCalculationModule.yawDiscretizations; i++)
      {
         RDXCVImagePanel steppabilityPanel = new RDXCVImagePanel("Raw Steppability " + i, cellsPerSide, cellsPerSide);
         RDXCVImagePanel panel = new RDXCVImagePanel("Steppable Regions " + i, cellsPerSide, cellsPerSide);
         steppabilityPanels.add(steppabilityPanel);
         steppableRegionsPanels.add(panel);
         imguiPanel.addChild(steppabilityPanel.getVideoPanel());
         imguiPanel.addChild(panel.getVideoPanel());
      }

      steppableRegionGraphic = new RDXSteppableRegionGraphic();

      steppableRegionsCalculationModule.addSteppableRegionListCollectionOutputConsumer(latestSteppableRegionsToRender::set);
      steppableRegionsCalculationModule.addSteppableRegionDebugConsumer(latestSteppableRegionDebugImagesToRender::set);
   }

   volatile boolean processing = false;
   volatile boolean needToDraw = false;

   public void extractSteppableRegions()
   {
      extractSteppableRegions(null);
   }

   public void extractSteppableRegions(Runnable runWhenFinished)
   {
      if (enabled.get())
      {
         updateSteppableRegions(runWhenFinished);

         generateSteppableRegionsMesh();
         drawDebugRegions();

         if (needToDraw)
         {
            needToDraw = false;

            render2DPanels();
            renderSteppableRegions();

            processing = false;
         }
         steppableRegionGraphic.update();
      }
   }


   private void updateSteppableRegions(Runnable runWhenFinished)
   {
      HeightMapMessage heightMapMessage = heightMapMessageReference.getAndSet(null);
      if (heightMapMessage == null)
         return;

      HeightMapData heightMapData = HeightMapMessageTools.unpackMessage(heightMapMessage);
      resize(heightMapData.getCellsPerAxis());

      steppableRegionsCalculationModule.setSteppableRegionsCalculatorParameters(parameters);
      steppableRegionsCalculationModule.computeASynch(heightMapData);

      if (runWhenFinished != null)
         runWhenFinished.run();
   }

   private void resize(int newSize)
   {
      if (newSize != cellsPerSide)
      {
         cellsPerSide = newSize;
         OpenCLManager openCLManager = steppableRegionsCalculationModule.getOpenCLManager();
         for (int i = 0; i < SteppableRegionsCalculationModule.yawDiscretizations; i++)
         {
            steppableRegionsPanels.get(i).resize(newSize, newSize, openCLManager);
            steppabilityPanels.get(i).resize(newSize, newSize, openCLManager);
         }
      }
   }

   public void generateSteppableRegionsMesh()
   {
      SteppableRegionsListCollection steppableRegionsListCollection = latestSteppableRegionsToRender.getAndSet(null);
      if (steppableRegionsListCollection == null)
         return;

      List<SteppableRegion> regionsToView = steppableRegionsListCollection.getSteppableRegions(0)
                                                   .getSteppableRegionsAsList();
      steppableRegionGraphic.generateMeshesAsync(regionsToView);
      LogTools.info("Found " + regionsToView.size() + " regions");
      needToDraw = true;
   }

   public void drawDebugRegions()
   {
      SteppableRegionDebugImagesMessage debugImagesMessage = latestSteppableRegionDebugImagesToRender.getAndSet(null);
      if (debugImagesMessage == null)
         return;

      for (int yaw = 0; yaw < SteppableRegionsCalculationModule.yawDiscretizations; yaw++)
      {
         RDXCVImagePanel panel = steppableRegionsPanels.get(yaw);
         if (panel.getVideoPanel().getIsShowing().get() && drawPatches.get())
         {
            BytedecoImage image = panel.getBytedecoImage();

            SteppableRegionDebugImageMessage regionImage = debugImagesMessage.getRegionImages().get(yaw);

            if (image.getImageHeight() != regionImage.getImageHeight() || image.getImageWidth() != regionImage.getImageWidth())
               throw new RuntimeException("Sizes don't match");

            for (int row = 0; row < regionImage.getImageHeight(); row++)
            {
               for (int col = 0; col < regionImage.getImageWidth(); col++)
               {
                  BytePointer pointer = image.getBytedecoOpenCVMat().ptr(row, col);
                  for (int j = 0; j < 3; j++)
                  {
                     int index = row * regionImage.getImageWidth() + col;
                     int start = 3 * index;
                     pointer.put(j, regionImage.getData().get(start + j));
                  }
               }
            }
         }

         panel = steppabilityPanels.get(yaw);
         if (panel.getVideoPanel().getIsShowing().get() && drawPatches.get())
         {
            BytedecoImage image = panel.getBytedecoImage();
            SteppableRegionDebugImageMessage steppabilityImage = debugImagesMessage.getSteppabilityImages().get(yaw);

            for (int row = 0; row < steppabilityImage.getImageHeight(); row++)
            {
               for (int col = 0; col < steppabilityImage.getImageWidth(); col++)
               {
                  BytePointer pointer = image.getBytedecoOpenCVMat().ptr(row, col);
                  for (int j = 0; j < 3; j++)
                  {
                     int index = row * steppabilityImage.getImageWidth() + col;
                     int start = 3 * index;
                     pointer.put(j, steppabilityImage.getData().get(start + j));
                  }
               }
            }
         }
      }

      needToDraw = true;
   }

   private void render2DPanels()
   {
      steppableRegionsPanels.forEach(RDXCVImagePanel::draw);
      steppabilityPanels.forEach(RDXCVImagePanel::draw);
   }

   /**
    * FIXME: This method filled with allocations.
    */
   private void renderSteppableRegions()
   {
      //      if (!render3DPlanarRegions.get())
      //         return;

      //      planarRegionsGraphic.generateMeshes(gpuPlanarRegionExtraction.getPlanarRegionsList());
      //      planarRegionsGraphic.update();
   }

   public void renderImGuiWidgets()
   {
      ImGui.checkbox(labels.get("Draw patches"), drawPatches);
      parameterTuner.renderImGuiWidgets();

      ImGui.text("Input height map dimensions: " + cellsPerSide + " x " + cellsPerSide);
      ImGui.checkbox(labels.get("Enabled"), enabled);
   }

   public void getVirtualRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      // TODO
      steppableRegionGraphic.getRenderables(renderables, pool);
      //      if (render3DPlanarRegions.get())
      //         planarRegionsGraphic.getRenderables(renderables, pool);
   }

   public void acceptHeightMapMessage(HeightMapMessage heightMapMessage)
   {
      heightMapMessageReference.set(heightMapMessage);
   }

   public void destroy()
   {
      steppableRegionsCalculationModule.destroy();
   }

   public ImGuiPanel getPanel()
   {
      return imguiPanel;
   }

   public ImBoolean getEnabled()
   {
      return enabled;
   }
}
