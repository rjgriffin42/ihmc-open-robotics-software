package us.ihmc.gdx.logging;

import boofcv.struct.calib.CameraPinholeBrown;
import imgui.ImGui;
import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.opencv.global.opencv_core;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.gdx.Lwjgl3ApplicationAdapter;
import us.ihmc.gdx.imgui.ImGuiPanel;
import us.ihmc.gdx.sceneManager.GDXSceneLevel;
import us.ihmc.gdx.simulation.environment.GDXEnvironmentBuilder;
import us.ihmc.gdx.simulation.sensors.GDXHighLevelDepthSensorSimulator;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.gdx.ui.affordances.GDXInteractableReferenceFrame;
import us.ihmc.gdx.ui.gizmo.GDXPose3DGizmo;
import us.ihmc.gdx.ui.graphics.ImGuiOpenCVSwapVideoPanel;
import us.ihmc.perception.BytedecoImage;
import us.ihmc.perception.BytedecoOpenCVTools;
import us.ihmc.perception.BytedecoTools;
import us.ihmc.tools.thread.Activator;

import java.nio.ByteOrder;

public class GDXFFMPEGL515DepthLoggingDemo
{
   private final Activator nativesLoadedActivator = BytedecoTools.loadNativesOnAThread(opencv_core.class, ffmpeg.class);
   private final GDXImGuiBasedUI baseUI = new GDXImGuiBasedUI(getClass(), "ihmc-open-robotics-software", "ihmc-high-level-behaviors/src/main/resources");
   private final boolean lossless = false;
   private final int framerate = 15;
   private final int bitrate = 1450000;
   private final FFMPEGLoggerDemoHelper ffmpegLoggerDemoHelper = new FFMPEGLoggerDemoHelper("FFMPEGL515DepthLoggingDemo.webm",
                                                                                            avutil.AV_PIX_FMT_RGBA,
                                                                                            avutil.AV_PIX_FMT_YUV420P,
                                                                                            lossless,
                                                                                            framerate,
                                                                                            bitrate);
   private GDXHighLevelDepthSensorSimulator l515;
   private GDXInteractableReferenceFrame robotInteractableReferenceFrame;
   private GDXPose3DGizmo l515PoseGizmo = new GDXPose3DGizmo();
   private GDXEnvironmentBuilder environmentBuilder;
   private ImGuiOpenCVSwapVideoPanel swapCVPanel;
   private int imageWidth;
   private int imageHeight;
   private BytedecoImage normalizedDepthImage;
   private BytedecoImage rgbaDepthImage;

   public GDXFFMPEGL515DepthLoggingDemo()
   {
      baseUI.launchGDXApplication(new Lwjgl3ApplicationAdapter()
      {
         @Override
         public void create()
         {
            baseUI.create();

            ImGuiPanel panel = new ImGuiPanel("Diagnostics", this::renderImGuiWidgets);
            baseUI.getImGuiPanelManager().addPanel(panel);

            environmentBuilder = new GDXEnvironmentBuilder(baseUI.get3DSceneManager());
            environmentBuilder.create(baseUI);
            baseUI.getImGuiPanelManager().addPanel(environmentBuilder.getPanelName(), environmentBuilder::renderImGuiWidgets);
            baseUI.get3DSceneManager().addRenderableProvider(environmentBuilder::getRealRenderables, GDXSceneLevel.REAL_ENVIRONMENT);
            baseUI.get3DSceneManager().addRenderableProvider(environmentBuilder::getVirtualRenderables, GDXSceneLevel.VIRTUAL);
            environmentBuilder.loadEnvironment("DemoPullDoor.json");

            robotInteractableReferenceFrame = new GDXInteractableReferenceFrame();
            robotInteractableReferenceFrame.create(ReferenceFrame.getWorldFrame(), 0.15, baseUI.get3DSceneManager().getCamera3D());
            robotInteractableReferenceFrame.getTransformToParent().getTranslation().add(2.2, 0.0, 1.0);
            baseUI.addImGui3DViewInputProcessor(robotInteractableReferenceFrame::process3DViewInput);
            baseUI.get3DSceneManager().addRenderableProvider(robotInteractableReferenceFrame::getVirtualRenderables, GDXSceneLevel.VIRTUAL);
            l515PoseGizmo = new GDXPose3DGizmo(robotInteractableReferenceFrame.getRepresentativeReferenceFrame());
            l515PoseGizmo.create(baseUI.get3DSceneManager().getCamera3D());
            l515PoseGizmo.setResizeAutomatically(false);
            baseUI.addImGui3DViewPickCalculator(l515PoseGizmo::calculate3DViewPick);
            baseUI.addImGui3DViewInputProcessor(l515PoseGizmo::process3DViewInput);
            baseUI.get3DSceneManager().addRenderableProvider(l515PoseGizmo, GDXSceneLevel.VIRTUAL);
            l515PoseGizmo.getTransformToParent().appendPitchRotation(Math.toRadians(60.0));
         }

         @Override
         public void render()
         {
            if (nativesLoadedActivator.poll())
            {
               if (nativesLoadedActivator.isNewlyActivated())
               {
                  double publishRateHz = 5.0;
                  double verticalFOV = 55.0;
                  imageWidth = 1024;
                  imageHeight = 768;
                  double minRange = 0.105;
                  double maxRange = 5.0;
                  l515 = new GDXHighLevelDepthSensorSimulator("Stepping L515",
                                                              l515PoseGizmo.getGizmoFrame(),
                                                              () -> 0L,
                                                              verticalFOV,
                                                              imageWidth,
                                                              imageHeight,
                                                              minRange,
                                                              maxRange,
                                                              0.005,
                                                              0.005,
                                                              true,
                                                              publishRateHz);
                  baseUI.getImGuiPanelManager().addPanel(l515);
                  l515.setSensorEnabled(true);
                  l515.setPublishPointCloudROS2(false);
                  l515.setRenderPointCloudDirectly(false);
                  l515.setPublishDepthImageROS1(false);
                  l515.setDebugCoordinateFrame(false);
                  l515.setRenderColorVideoDirectly(true);
                  l515.setRenderDepthVideoDirectly(true);
                  l515.setPublishColorImageROS1(false);
                  l515.setPublishColorImageROS2(false);
                  CameraPinholeBrown cameraIntrinsics = l515.getDepthCameraIntrinsics();
                  baseUI.get3DSceneManager().addRenderableProvider(l515, GDXSceneLevel.VIRTUAL);

                  swapCVPanel = new ImGuiOpenCVSwapVideoPanel("Video", false);
                  baseUI.getImGuiPanelManager().addPanel(swapCVPanel.getVideoPanel());

                  normalizedDepthImage = new BytedecoImage(imageWidth, imageHeight, opencv_core.CV_8UC1);
                  rgbaDepthImage = new BytedecoImage(imageWidth, imageHeight, opencv_core.CV_8UC4);

                  ffmpegLoggerDemoHelper.create(imageWidth, imageHeight, () ->
                  {
                     BytedecoOpenCVTools.clampTo8BitUnsignedChar(l515.getLowLevelSimulator().getMetersDepthOpenCVMat(), normalizedDepthImage.getBytedecoOpenCVMat(), 0.0, 255.0);
                     BytedecoOpenCVTools.convert8BitGrayTo8BitRGBA(normalizedDepthImage.getBytedecoOpenCVMat(), rgbaDepthImage.getBytedecoOpenCVMat());

                     ffmpegLoggerDemoHelper.getLogger().put(rgbaDepthImage);
                  });

                  baseUI.getPerspectiveManager().reloadPerspective();
               }

               l515.render(baseUI.get3DSceneManager());
            }

            baseUI.renderBeforeOnScreenUI();
            baseUI.renderEnd();
         }

         private void renderImGuiWidgets()
         {
            ImGui.text("System native byte order: " + ByteOrder.nativeOrder().toString());
            ffmpegLoggerDemoHelper.renderImGuiBasicInfo();

            if (nativesLoadedActivator.peek())
            {
               ImGui.text("Image dimensions: " + imageWidth + " x " + imageHeight);

               ffmpegLoggerDemoHelper.renderImGuiNativesLoaded();
            }
         }

         @Override
         public void dispose()
         {
            baseUI.dispose();
            environmentBuilder.destroy();
            l515.dispose();
         }
      });
   }

   public static void main(String[] args)
   {
      new GDXFFMPEGL515DepthLoggingDemo();
   }
}