package us.ihmc.gdx.ui.vr;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.internal.ImGui;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.ros2.ROS2ControllerHelper;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.gdx.imgui.GDX3DSituatedImGuiPanel;
import us.ihmc.gdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.gdx.vr.GDXVRContext;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.Map;

/**
 * TODO: Figure out how to incorporate this class with things better.
 */
public class GDXVRModeManager
{
   private GDXVRHandPlacedFootstepMode handPlacedFootstepMode;
   private GDXVRKinematicsStreamingMode kinematicsStreamingMode;
   private GDX3DSituatedImGuiPanel leftHandPanel;
   private final FramePose3D leftHandPanelPose = new FramePose3D();
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private GDXVRMode mode = GDXVRMode.INPUTS_DISABLED;
   private boolean headsetIsConnected;

   public void create(GDXImGuiBasedUI baseUI,
                      DRCRobotModel robotModel,
                      Map<String, Double> initialConfiguration,
                      ROS2ControllerHelper controllerHelper)
   {
      handPlacedFootstepMode = new GDXVRHandPlacedFootstepMode();
      handPlacedFootstepMode.create(robotModel, controllerHelper);

      kinematicsStreamingMode = new GDXVRKinematicsStreamingMode(robotModel, initialConfiguration, controllerHelper);
      kinematicsStreamingMode.create(baseUI.getVRManager().getContext());

      baseUI.getImGuiPanelManager().addPanel("VR Mode Manager", this::renderImGuiWidgets);

      leftHandPanel = new GDX3DSituatedImGuiPanel("VR Mode Manager", this::renderImGuiWidgets);
      leftHandPanel.create(baseUI.getImGuiWindowAndDockSystem().getImGuiGl3(), 0.3, 0.5, 10);
      leftHandPanel.setBackgroundTransparency(new Color(0.3f, 0.3f, 0.3f, 0.75f));
      baseUI.getVRManager().getContext().addVRPickCalculator(leftHandPanel::calculateVRPick);
      baseUI.getVRManager().getContext().addVRInputProcessor(leftHandPanel::processVRInput);
   }

   public void processVRInput(GDXVRContext vrContext)
   {
      headsetIsConnected = vrContext.getHeadset().isConnected();

      vrContext.getController(RobotSide.LEFT).runIfConnected(controller ->
      {
         leftHandPanelPose.setToZero(controller.getXForwardZUpControllerFrame());
         leftHandPanelPose.getOrientation().setYawPitchRoll(Math.PI / 2.0, 0.0, Math.PI / 4.0);
         leftHandPanelPose.getPosition().addY(-0.05);
         leftHandPanelPose.changeFrame(ReferenceFrame.getWorldFrame());
         leftHandPanel.updateDesiredPose(leftHandPanelPose::get);
      });

      switch (mode)
      {
         case FOOTSTEP_PLACEMENT -> handPlacedFootstepMode.processVRInput(vrContext);
         case WHOLE_BODY_IK_STREAMING -> kinematicsStreamingMode.processVRInput(vrContext);
      }
   }

   public void update()
   {
      kinematicsStreamingMode.update();
      leftHandPanel.update();
   }

   private void renderImGuiWidgets()
   {
      if (ImGui.radioButton(labels.get("Inputs disabled"), mode == GDXVRMode.INPUTS_DISABLED))
      {
         mode = GDXVRMode.INPUTS_DISABLED;
      }
      if (ImGui.radioButton(labels.get("Footstep placement"), mode == GDXVRMode.FOOTSTEP_PLACEMENT))
      {
         mode = GDXVRMode.FOOTSTEP_PLACEMENT;
      }
      if (ImGui.radioButton(labels.get("Whole body IK streaming"), mode == GDXVRMode.WHOLE_BODY_IK_STREAMING))
      {
         mode = GDXVRMode.WHOLE_BODY_IK_STREAMING;
      }
//      if (ImGui.radioButton(labels.get("Joystick"), mode == 2))
//      {
//         mode = 2;
//      }

      kinematicsStreamingMode.renderImGuiWidgets();
      imgui.ImGui.text("VR Controller Input Mappings:");
      imgui.ImGui.text("Teleport - Right controller B button");
      imgui.ImGui.text("Adjust VR Z height - Slide up and down on the right controller touchpad");
      imgui.ImGui.text("Left/Right Triggers: Hold and release to place a footstep");
      imgui.ImGui.text("Clear footsteps - Left controller B button");
      imgui.ImGui.text("Walk - Right controller A button");
      imgui.ImGui.text("Move ImGui panel - Grip right controller and drag the panel around");
      imgui.ImGui.text("Click buttons on the ImGui panel - Right controller trigger click");
   }

   public void getVirtualRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      handPlacedFootstepMode.getRenderables(renderables, pool);
      kinematicsStreamingMode.getVirtualRenderables(renderables, pool);

      if (headsetIsConnected)
         leftHandPanel.getRenderables(renderables, pool);
   }

   public void destroy()
   {
      leftHandPanel.dispose();
      kinematicsStreamingMode.destroy();
   }

   public GDXVRKinematicsStreamingMode getKinematicsStreamingMode()
   {
      return kinematicsStreamingMode;
   }
}
