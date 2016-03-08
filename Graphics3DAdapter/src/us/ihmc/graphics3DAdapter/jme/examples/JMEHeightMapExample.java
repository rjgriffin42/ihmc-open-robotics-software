package us.ihmc.graphics3DAdapter.jme.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.graphics3DAdapter.camera.ClassicCameraController;
import us.ihmc.graphics3DAdapter.camera.SimpleCameraTrackingAndDollyPositionHolder;
import us.ihmc.graphics3DAdapter.camera.ViewportAdapter;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.jme.JMEGraphics3DAdapter;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.robotics.geometry.BoundingBox3d;

public class JMEHeightMapExample
{
   public static void main(String[] args)
   {
      JMEGraphics3DAdapter graphicsAdapter = new JMEGraphics3DAdapter();

      FlatHeightMap flatHeightMap = new FlatHeightMap();

//      graphicsAdapter.setHeightMap(flatHeightMap);
      Vector3d translation = new Vector3d(10.0, 10.0, 0.0);

      Graphics3DObject sphereObject = new Graphics3DObject();
      sphereObject.addCoordinateSystem(1.0);
      sphereObject.translate(translation);
      sphereObject.addSphere(0.5, YoAppearance.Green());

      
      Graphics3DNode sphereNode = new Graphics3DNode("sphere", Graphics3DNodeType.JOINT);
      sphereNode.setGraphicsObject(sphereObject);
      graphicsAdapter.addRootNode(sphereNode);

      Graphics3DObject groundObject = new Graphics3DObject();
      groundObject.translate(translation);
     

      groundObject.addHeightMap(flatHeightMap, 100, 100, YoAppearance.Purple());
      Graphics3DNode groundNode = new Graphics3DNode("ground", Graphics3DNodeType.JOINT);
      
      groundNode.setGraphicsObject(groundObject);
      graphicsAdapter.addRootNode(groundNode);

      createAndShowStandardWindow(graphicsAdapter);

   }


   private static void createAndShowStandardWindow(JMEGraphics3DAdapter graphicsAdapter)
   {
      SimpleCameraTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder = new SimpleCameraTrackingAndDollyPositionHolder();
      ViewportAdapter camera = graphicsAdapter.createNewViewport(null, false, false);
      ClassicCameraController classicCameraController = ClassicCameraController.createClassicCameraControllerAndAddListeners(camera,
                                                           cameraTrackAndDollyVariablesHolder, graphicsAdapter);
      camera.setCameraController(classicCameraController);
      Canvas canvas = camera.getCanvas();
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", canvas);

      JFrame jFrame = new JFrame("JME HeightMap Example");
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add("Center", panel);

      jFrame.pack();
      jFrame.setVisible(true);
      jFrame.setSize(800, 600);
   }


   private static class FlatHeightMap implements HeightMap
   {
      private final BoundingBox3d boundingBox = new BoundingBox3d(-1.0, -2.0, Double.MIN_VALUE, 2.0, 5.0, 0.0);

      public double heightAt(double x, double y, double z)
      {
         return 0.0;
      }

      public BoundingBox3d getBoundingBox()
      {
         return boundingBox;
      }
   }
}
