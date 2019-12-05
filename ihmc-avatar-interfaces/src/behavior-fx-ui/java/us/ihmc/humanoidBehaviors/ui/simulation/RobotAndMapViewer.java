package us.ihmc.humanoidBehaviors.ui.simulation;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.humanoidBehaviors.ui.graphics.BodyPathPlanGraphic;
import us.ihmc.humanoidBehaviors.ui.graphics.FootstepPlanGraphic;
import us.ihmc.humanoidBehaviors.ui.graphics.live.LivePlanarRegionsGraphic;
import us.ihmc.humanoidBehaviors.ui.tools.JavaFXRemoteRobotVisualizer;
import us.ihmc.javaFXToolkit.cameraControllers.FocusBasedCameraMouseEventHandler;
import us.ihmc.javaFXToolkit.scenes.View3DFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.ros2.Ros2Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RobotAndMapViewer
{
   private FootstepPlanGraphic footstepPlanGraphic;
   private BodyPathPlanGraphic bodyPathPlanGraphic;

   public RobotAndMapViewer(DRCRobotModel robotModel, Ros2Node ros2Node)
   {
      Platform.runLater(() ->
      {
         View3DFactory view3dFactory = new View3DFactory(1200, 800);
         FocusBasedCameraMouseEventHandler camera = view3dFactory.addCameraController(0.05, 2000.0, true);
         double isoZoomOut = 10.0;
         camera.changeCameraPosition(-isoZoomOut, -isoZoomOut, isoZoomOut);
         view3dFactory.addWorldCoordinateSystem(0.3);
         view3dFactory.addDefaultLighting();

         view3dFactory.addNodeToView(new LivePlanarRegionsGraphic(ros2Node));
         view3dFactory.addNodeToView(new JavaFXRemoteRobotVisualizer(robotModel, ros2Node));

         footstepPlanGraphic = new FootstepPlanGraphic(robotModel);
         view3dFactory.addNodeToView(footstepPlanGraphic);

         bodyPathPlanGraphic = new BodyPathPlanGraphic();
         view3dFactory.addNodeToView(bodyPathPlanGraphic);

         Stage primaryStage = new Stage();
         primaryStage.setTitle(getClass().getSimpleName());
         primaryStage.setMaximized(false);
         primaryStage.setScene(view3dFactory.getScene());

         primaryStage.show();
         primaryStage.toFront();
      });
   }

   public void setFootstepsToVisualize(FootstepPlan footstepPlan)
   {
      ArrayList<Pair<RobotSide, Pose3D>> footstepLocations = new ArrayList<>();
      for (int i = 0; i < footstepPlan.getNumberOfSteps(); i++)  // this code makes the message smaller to send over the network, TODO investigate
      {
         FramePose3D soleFramePoseToPack = new FramePose3D();
         footstepPlan.getFootstep(i).getSoleFramePose(soleFramePoseToPack);
         footstepLocations.add(new MutablePair<>(footstepPlan.getFootstep(i).getRobotSide(), new Pose3D(soleFramePoseToPack)));
      }
      footstepPlanGraphic.generateMeshesAsynchronously(footstepLocations);
   }

   public void setBodyPathPlanToVisualize(List<Point3DReadOnly> bodyPath)
   {
      bodyPathPlanGraphic.generateMeshesAsynchronously(bodyPath.stream().map(Point3D::new).collect(Collectors.toList())); // deep copy
   }
}
