package us.ihmc.darpaRoboticsChallenge;

import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.projectM.R2Sim02.initialSetup.GuiInitialSetup;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.SupportedGraphics3DAdapter;

public class DRCGuiInitialSetup implements GuiInitialSetup<Robot>
{
   private SupportedGraphics3DAdapter graphics3dAdapter = SupportedGraphics3DAdapter.JAVA_MONKEY_ENGINE;

   public void initializeGUI(SimulationConstructionSet scs, Robot robot)
   {
      CameraConfiguration behindPelvis = new CameraConfiguration("BehindPelvis");
      behindPelvis.setCameraTracking(false, true, true, false);
      behindPelvis.setCameraDolly(false, true, true, false);
      behindPelvis.setCameraFix(0.0, 0.0, 1.0);
      behindPelvis.setCameraPosition(-2.5, 0.0, 1.0);
      behindPelvis.setCameraTrackingVars("q_x", "q_y", "q_z");
      scs.setupCamera(behindPelvis);

      CameraConfiguration camera5 = new CameraConfiguration("left_camera_sensor");
      camera5.setCameraMount("left_camera_sensor");
      scs.setupCamera(camera5);
      CameraConfiguration camera6 = new CameraConfiguration("right_camera_sensor");
      camera6.setCameraMount("right_camera_sensor");
      scs.setupCamera(camera6);
   }

   public SupportedGraphics3DAdapter getGraphics3dAdapter()
   {
      return graphics3dAdapter;
   }

   public void setGraphics3dAdapter(SupportedGraphics3DAdapter graphics3dAdapter)
   {
      this.graphics3dAdapter = graphics3dAdapter;
   }

   public boolean isGuiShown()
   {
      return true;
   }
}
