package us.ihmc.gdx.perception;

import imgui.ImGui;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.bytedeco.librealsense2.global.realsense2;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import us.ihmc.gdx.Lwjgl3ApplicationAdapter;
import us.ihmc.gdx.imgui.ImGuiPanel;
import us.ihmc.gdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.gdx.ui.gizmo.GDXPose3DGizmo;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.perception.BytedecoImage;
import us.ihmc.perception.BytedecoTools;
import us.ihmc.perception.MutableBytePointer;
import us.ihmc.perception.spinnaker.BytedecoBlackfly;
import us.ihmc.perception.spinnaker.SpinnakerHardwareManager;
import us.ihmc.tools.thread.Activator;
import us.ihmc.tools.time.FrequencyCalculator;
import us.ihmc.yoVariables.registry.YoRegistry;

import java.nio.ByteOrder;

public class GDXBlackflyUI
{
   private final GDXImGuiBasedUI baseUI = new GDXImGuiBasedUI(getClass(),
                                                              "ihmc-open-robotics-software",
                                                              "ihmc-high-level-behaviors/src/main/resources");
   private final Activator nativesLoadedActivator;
   private final GDXPose3DGizmo sensorPoseGizmo = new GDXPose3DGizmo();
   private YoRegistry yoRegistry = new YoRegistry(getClass().getSimpleName());
   private YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
   private BytedecoBlackfly blackfly;
   private SpinnakerHardwareManager spinnakerHardwareManager;
   private GDXCVImagePanel imagePanel;
   private Mat depthU16C1Image;
   private FrequencyCalculator frameReadFrequency = new FrequencyCalculator();
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final ImFloat laserPower = new ImFloat(100.0f);
   private final ImFloat receiverSensitivity = new ImFloat(0.5f);
   private final ImInt digitalGain = new ImInt(realsense2.RS2_DIGITAL_GAIN_LOW);
   private final String[] digitalGains = new String[] { "AUTO", "LOW", "HIGH" };

   public GDXBlackflyUI()
   {
      nativesLoadedActivator = BytedecoTools.loadNativesOnAThread();

      baseUI.launchGDXApplication(new Lwjgl3ApplicationAdapter()
      {
         @Override
         public void create()
         {
            baseUI.create();

            ImGuiPanel panel = new ImGuiPanel("Blackfly", this::renderImGuiWidgets);
            baseUI.getImGuiPanelManager().addPanel(panel);
         }

         @Override
         public void render()
         {
            if (nativesLoadedActivator.poll())
            {
               if (nativesLoadedActivator.isNewlyActivated())
               {
                  spinnakerHardwareManager = SpinnakerHardwareManager.getInstance();
                  blackfly = spinnakerHardwareManager.buildBlackfly("Serial number goes here"); //TODO serial number
                  blackfly.initialize();
               }

               if (blackfly.readFrameData())
               {
                  blackfly.updateDataBytePointers();

                  if (imagePanel == null)
                  {
                     MutableBytePointer frameData = blackfly.getFrameData();
                     depthU16C1Image = new Mat(blackfly.getHeight(), blackfly.getWidth(), opencv_core.CV_16UC1, frameData);

                     imagePanel = new GDXCVImagePanel("Blackfly Image", blackfly.getWidth(), blackfly.getHeight());
                     baseUI.getImGuiPanelManager().addPanel(imagePanel.getVideoPanel());

                     baseUI.getPerspectiveManager().reloadPerspective();
                  }

                  frameReadFrequency.ping();
               }
            }

            baseUI.renderBeforeOnScreenUI();
            baseUI.renderEnd();
         }

         private void renderImGuiWidgets()
         {
            ImGui.text("System native byte order: " + ByteOrder.nativeOrder().toString());

            if (imagePanel != null)
            {
               ImGui.text("Frame read frequency: " + frameReadFrequency.getFrequency());

               ImGui.text("Unsigned 16 Depth:");

               for (int i = 0; i < 5; i++)
               {
                  ImGui.text(depthU16C1Image.ptr(0, i).getShort() + " ");
               }

               ImGui.text("Float 32 Meters:");

               ImGui.text("R G B A:");

               imagePanel.getBytedecoImage().rewind();
               for (int i = 0; i < 5; i++)
               {
                  printBytes(imagePanel.getBytedecoImage().getBytedecoOpenCVMat().ptr(0, i).get(0),
                             imagePanel.getBytedecoImage().getBytedecoOpenCVMat().ptr(0, i).get(1),
                             imagePanel.getBytedecoImage().getBytedecoOpenCVMat().ptr(0, i).get(2),
                             imagePanel.getBytedecoImage().getBytedecoOpenCVMat().ptr(0, i).get(3));
               }
            }
         }

         private void printBytes(byte byte0, byte byte1, byte byte2, byte byte3)
         {
            printInts(Byte.toUnsignedInt(byte0),
                      Byte.toUnsignedInt(byte1),
                      Byte.toUnsignedInt(byte2),
                      Byte.toUnsignedInt(byte3));
         }

         private void printInts(int int0, int int1, int int2, int int3)
         {
            ImGui.text(int0 + " " + int1 + " " + int2 + " " + int3);
         }

         @Override
         public void dispose()
         {
            baseUI.dispose();
            blackfly.destroy();
         }
      });
   }

   public static void main(String[] args)
   {
      new GDXRealsenseL515UI();
   }
}
