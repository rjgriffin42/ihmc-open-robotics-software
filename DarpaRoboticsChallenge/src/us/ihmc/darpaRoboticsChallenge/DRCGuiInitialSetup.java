package us.ihmc.darpaRoboticsChallenge;

import javax.swing.JButton;

import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;
import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.atlas.visualization.SliderBoardFactory;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.NullGraphics3DAdapter;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.jme.JMEGraphics3dAdapter;
import us.ihmc.projectM.R2Sim02.initialSetup.GuiInitialSetup;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.dataExporter.DataExporter;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;

public class DRCGuiInitialSetup implements GuiInitialSetup
{
   private static final boolean SHOW_ONLY_WRENCH_VISUALIZER = false;
   private static final boolean SHOW_EXPORT_TORQUE_AND_SPEED = true;
   private boolean isGuiShown = true;
   private boolean is3dGraphicsShown = true;
   private final boolean groundProfileVisible;
   private final boolean drawPlaneAtZ0;
   private final SliderBoardFactory sliderBoardFactory;
   
   public DRCGuiInitialSetup(boolean groundProfileVisible, boolean drawPlaneAtZeroHeight)
   {
      this(groundProfileVisible, drawPlaneAtZeroHeight, null);
   }
   
   public DRCGuiInitialSetup(boolean groundProfileVisible, boolean drawPlaneAtZeroHeight, SliderBoardFactory sliderBoardFactory)
   {
      this.groundProfileVisible = groundProfileVisible;
      this.drawPlaneAtZ0 = drawPlaneAtZeroHeight;
      this.sliderBoardFactory = sliderBoardFactory;
   }
   
   public void initializeGUI(SimulationConstructionSet scs, Robot robot)
   {
      CameraConfiguration behindPelvis = new CameraConfiguration("BehindPelvis");
      behindPelvis.setCameraTracking(false, true, true, false);
      behindPelvis.setCameraDolly(false, true, true, false);
      behindPelvis.setCameraFix(0.0, 0.0, 1.0);
      behindPelvis.setCameraPosition(-2.5, 0.0, 1.0);
      behindPelvis.setCameraTrackingVars("q_x", "q_y", "q_z");
      scs.setupCamera(behindPelvis);

      CameraConfiguration camera5 = new CameraConfiguration("stereo_camera_left");
      camera5.setCameraMount("stereo_camera_right");
      scs.setupCamera(camera5);
      
      CameraConfiguration camera6 = new CameraConfiguration("stereo_camera_right");
      camera6.setCameraMount("stereo_camera_right");
      scs.setupCamera(camera6);
      
      scs.setGroundVisible(groundProfileVisible);
      
      if (drawPlaneAtZ0)
      {
         Graphics3DObject planeAtZ0 = new Graphics3DObject();
         planeAtZ0.addHeightMap(new FlatGroundProfile(), 100, 100, null);
         scs.addStaticLinkGraphics(planeAtZ0);
      }
      
      if(!is3dGraphicsShown)
      {
         scs.hideViewport();
      }

      if (SHOW_ONLY_WRENCH_VISUALIZER)
      {
         scs.hideAllDynamicGraphicObjects();
         scs.setDynamicGraphicObjectsListVisible("wrenchVisualizer", true);
      }
      
      if (SHOW_EXPORT_TORQUE_AND_SPEED)
      {
         JButton exportTorqueAndSpeedButton = new JButton("Export Torque And Speed");
         DataExporter dataExporter = new DataExporter(scs, robot);
         exportTorqueAndSpeedButton.addActionListener(dataExporter);
         scs.addButton(exportTorqueAndSpeedButton);
      }
      
      //TODO: Clean this up!
      DRCRobotJointMap jointMap = new DRCRobotJointMap(DRCRobotModel.ATLAS_NO_HANDS, false);
      JaxbSDFLoader robotLoader = DRCRobotSDFLoader.loadDRCRobot(jointMap);
      GeneralizedSDFRobotModel generalizedSDFRobotModel = robotLoader.getGeneralizedSDFRobotModel(jointMap.getModelName());
      
      if (DRCLocalConfigParameters.MAKE_SLIDER_BOARD && sliderBoardFactory != null)
         sliderBoardFactory.makeSliderBoard(scs, scs.getRootRegistry(), generalizedSDFRobotModel);
   }

   public boolean isGuiShown()
   {
      return isGuiShown;
   }

   public void setIsGuiShown(boolean isGuiShown)
   {
      this.isGuiShown = isGuiShown;
   }

   public Graphics3DAdapter getGraphics3DAdapter()
   {
      if(isGuiShown && is3dGraphicsShown)
      {
         return new JMEGraphics3dAdapter();
      }
      else if(isGuiShown)
      {
         return new NullGraphics3DAdapter();
      }
      else
      {
         return null;
      }
   }

   public void setIs3dGraphicsShown(boolean is3dGraphicsShown)
   {
      this.is3dGraphicsShown = is3dGraphicsShown;
   }
}
