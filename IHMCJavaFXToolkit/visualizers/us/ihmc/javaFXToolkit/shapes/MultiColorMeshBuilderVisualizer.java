package us.ihmc.javaFXToolkit.shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import javafx.application.Application;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.stage.Stage;
import us.ihmc.javaFXToolkit.cameraControllers.FocusBasedCameraMouseEventHandler;
import us.ihmc.robotics.random.RandomTools;

public class MultiColorMeshBuilderVisualizer extends Application
{
   private enum MeshToDisplay {BOX, LINE, MULTI_LINE}
   private static final MeshToDisplay MESH_TO_DISPLAY = MeshToDisplay.MULTI_LINE;

   public MultiColorMeshBuilderVisualizer()
   {
      // TODO Auto-generated constructor stub
   }

   @Override
   public void start(Stage primaryStage) throws Exception
   {
      primaryStage.setTitle(getClass().getSimpleName());

      Group rootNode = new Group();
      Scene scene = new Scene(rootNode, 600, 400, true);
      scene.setFill(Color.GRAY);
      setupCamera(rootNode, scene);

      JavaFXCoordinateSystem worldCoordinateSystem = new JavaFXCoordinateSystem(0.3);
      rootNode.getChildren().add(worldCoordinateSystem);

      Color[] colors = {Color.YELLOW, Color.BEIGE, Color.CHOCOLATE, Color.ANTIQUEWHITE};

      MultiColorMeshBuilder meshBuilder = new MultiColorMeshBuilder();

      switch (MESH_TO_DISPLAY)
      {
      case BOX:
         addRandomBoxes(colors, meshBuilder);
         break;
      case LINE:
         addLine(meshBuilder);
      case MULTI_LINE:
         addMultiLine(meshBuilder);
      default:
         break;
      }

      MeshView meshView = new MeshView(meshBuilder.generateMesh());
      meshView.setMaterial(meshBuilder.generateMaterial());
      rootNode.getChildren().add(meshView);

      primaryStage.setScene(scene);
      primaryStage.show();
   }

   private void addMultiLine(MultiColorMeshBuilder meshBuilder)
   {
      List<Point3d> points = new ArrayList<>();
      double radius = 0.4;
      Random random = new Random();

      for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 2.0 * Math.PI / 50.0)
      {
         double x = radius * random.nextDouble() * Math.cos(angle);
         double y = radius * Math.sin(angle);
         double z = 0.1 * random.nextDouble();
         points.add(new Point3d(x, y, z));
      }
      meshBuilder.addMultiLineMesh(points, 0.01, Color.YELLOWGREEN, true);
   }

   private void addLine(MultiColorMeshBuilder meshBuilder)
   {
      Point3d start = new Point3d(0.3, 0.0, -0.);
      Point3d end = new Point3d(0.0, 0.3, 0.0);
      double lineWidth = 0.01;
      Color color = Color.RED;
      meshBuilder.addLineMesh(start, end, lineWidth, color);
   }

   public void addRandomBoxes(Color[] colors, MultiColorMeshBuilder meshBuilder)
   {
      int count = 0;
      for (float x = -1.0f; x <= 1.0f; x += 0.055f)
      {
         for (float y = -1.0f; y <= 1.0f; y += 0.055f)
         {
            for (float z = -0.0f; z <= 0.01f; z += 0.055f)
            {

               Color color = colors[count%colors.length];
               meshBuilder.addCubeMesh(0.05f, new Vector3f(x, y, 0 * RandomTools.generateRandomFloatInRange(new Random(), -5.0f, 5.0f)), color);
               count++;
            }
         }
      }

      System.out.println("Number of boxes: " + count);
   }

   private void setupCamera(Group root, Scene scene)
   {
      PerspectiveCamera camera = new PerspectiveCamera(true);
      camera.setNearClip(0.05);
      camera.setFarClip(50.0);
      scene.setCamera(camera);

      Vector3d up = new Vector3d(0.0, 0.0, 1.0);
      FocusBasedCameraMouseEventHandler cameraController = new FocusBasedCameraMouseEventHandler(scene.widthProperty(), scene.heightProperty(), camera, up);
      scene.addEventHandler(Event.ANY, cameraController);
      root.getChildren().add(cameraController.getFocusPointViz());
   }

   public static void main(String[] args)
   {
      Application.launch(args);
   }
}
