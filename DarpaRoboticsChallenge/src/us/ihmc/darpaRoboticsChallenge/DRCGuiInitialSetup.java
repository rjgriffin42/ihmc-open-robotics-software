package us.ihmc.darpaRoboticsChallenge;

import javax.swing.JButton;

import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotCameraParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotSensorInformation;
import us.ihmc.darpaRoboticsChallenge.initialSetup.GuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.visualization.WalkControllerSliderBoard;
import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.graphics3DAdapter.NullGraphics3DAdapter;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.jme.JMEGraphics3DAdapter;

import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.dataExporter.DataExporter;
import us.ihmc.simulationconstructionset.util.ground.FlatGroundProfile;

public class DRCGuiInitialSetup implements GuiInitialSetup
{
   private static final boolean SHOW_ONLY_WRENCH_VISUALIZER = false;
   private static final boolean SHOW_EXPORT_TORQUE_AND_SPEED = true;
   private static final boolean SHOW_OVERHEAD_VIEW = true;
   
   private boolean isGuiShown = true;
   private boolean is3dGraphicsShown = true;
   private final boolean groundProfileVisible;
   private final boolean drawPlaneAtZ0;
   private boolean showOverheadView = SHOW_OVERHEAD_VIEW;

   public DRCGuiInitialSetup(boolean groundProfileVisible, boolean drawPlaneAtZeroHeight)
   {
      this(groundProfileVisible, drawPlaneAtZeroHeight, true);
   }

   public DRCGuiInitialSetup(boolean groundProfileVisible, boolean drawPlaneAtZeroHeight, boolean showGUI)
   {
      this.groundProfileVisible = groundProfileVisible;
      this.drawPlaneAtZ0 = drawPlaneAtZeroHeight;
      this.isGuiShown = showGUI;
   }

   public void initializeGUI(SimulationConstructionSet scs, Robot robot)
   {
      // use
      // public void initializeGUI(SimulationConstructionSet scs, Robot robot, DRCRobotModel robotModel)
      throw new RuntimeException("Should not be here. This function is a relict of the GuiInitialSetup interface.");
   }

   public void initializeGUI(SimulationConstructionSet scs, Robot robot, DRCRobotModel robotModel)
   {
      CameraConfiguration behindPelvis = new CameraConfiguration("BehindPelvis");
      behindPelvis.setCameraTracking(false, true, true, false);
      behindPelvis.setCameraDolly(false, true, true, false);
      behindPelvis.setCameraFix(0.0, 0.0, 1.0);
      behindPelvis.setCameraPosition(-2.5, 0.0, 1.0);
      behindPelvis.setCameraTrackingVars("q_x", "q_y", "q_z");
      scs.setupCamera(behindPelvis);

      DRCRobotSensorInformation sensorInformation = robotModel.getSensorInformation();
      DRCRobotCameraParameters[] cameraInfo = sensorInformation.getCameraParameters();
      for (int i = 0; i < cameraInfo.length; i++)
      {
         CameraConfiguration camera = new CameraConfiguration(cameraInfo[i].getSensorNameInSdf());
         camera.setCameraMount(cameraInfo[i].getSensorNameInSdf());
         scs.setupCamera(camera);
      }

      scs.setGroundVisible(groundProfileVisible);

      if (drawPlaneAtZ0)
      {
         Graphics3DObject planeAtZ0 = new Graphics3DObject();
         
         HeightMap heightMap = new FlatGroundProfile();
         planeAtZ0.addHeightMap(heightMap, 100, 100, null);
         scs.addStaticLinkGraphics(planeAtZ0);
      }

      if (!is3dGraphicsShown)
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
      GeneralizedSDFRobotModel generalizedSDFRobotModel = robotModel.getGeneralizedRobotModel();

      if (DRCConfigParameters.MAKE_SLIDER_BOARD)
      {
         new WalkControllerSliderBoard(scs, scs.getRootRegistry(), generalizedSDFRobotModel);
      }
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
      if (isGuiShown && is3dGraphicsShown)
      {
         return new JMEGraphics3DAdapter();
      }
      else if (isGuiShown)
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

   public boolean isShowOverheadView()
   {
      return showOverheadView;
   }

   public void setShowOverheadView(boolean showOverheadView)
   {
      this.showOverheadView = showOverheadView;
   }
}
