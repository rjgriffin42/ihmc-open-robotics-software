package us.ihmc.gdx.perception;

import imgui.ImGui;
import us.ihmc.gdx.Lwjgl3ApplicationAdapter;
import us.ihmc.gdx.imgui.ImGuiPanel;
import us.ihmc.gdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.perception.BytedecoTools;
import us.ihmc.perception.spinnaker.BytedecoBlackfly;
import us.ihmc.perception.spinnaker.SpinnakerHardwareManager;
import us.ihmc.tools.thread.Activator;
import us.ihmc.tools.time.FrequencyCalculator;

import java.nio.ByteOrder;

public class GDXBlackflyUI
{
   private final GDXImGuiBasedUI baseUI = new GDXImGuiBasedUI(getClass(),
                                                              "ihmc-open-robotics-software",
                                                              "ihmc-high-level-behaviors/src/main/resources");
   private final Activator nativesLoadedActivator;
   private BytedecoBlackfly blackfly;
   private SpinnakerHardwareManager spinnakerHardwareManager;
   private GDXCVImagePanel imagePanel;
   private FrequencyCalculator frameReadFrequency = new FrequencyCalculator();
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());

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
                  blackfly = spinnakerHardwareManager.buildBlackfly("17403057");
                  blackfly.initialize();
               }

               if (blackfly.readFrameData())
               {
                  if (imagePanel == null)
                  {
                     imagePanel = new GDXCVImagePanel("Blackfly Image", blackfly.getWidth(), blackfly.getHeight());
                     baseUI.getImGuiPanelManager().addPanel(imagePanel.getVideoPanel());

                     baseUI.getPerspectiveManager().reloadPerspective();
                  }

                  frameReadFrequency.ping();
                  imagePanel.getBytedecoImage().rewind();
                  blackfly.getImageData(imagePanel.getBytedecoImage().getBytedecoByteBufferPointer());
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
            blackfly.destroy();
            baseUI.dispose();
         }
      });
   }

   public static void main(String[] args)
   {
      new GDXBlackflyUI();
   }
}