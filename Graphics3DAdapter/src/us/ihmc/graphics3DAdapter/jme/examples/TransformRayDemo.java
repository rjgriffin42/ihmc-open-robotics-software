package us.ihmc.graphics3DAdapter.jme.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.util.concurrent.Callable;

import javax.swing.JFrame;
import javax.swing.JPanel;

import us.ihmc.graphics3DAdapter.camera.ClassicCameraController;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.jme.JMERenderer;
import us.ihmc.graphics3DAdapter.jme.JMERenderer.RenderType;
import us.ihmc.graphics3DAdapter.jme.JMEViewportAdapter;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.graphics3DAdapter.utils.GraphicsDemoTools.PanBackAndForthTrackingAndDollyPositionHolder;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;

public class TransformRayDemo
{
   RigidBodyTransform tInit = new RigidBodyTransform();
   RigidBodyTransform tAct = new RigidBodyTransform();

   public static void main(String[] args)
   {
      new TransformRayDemo();
   }

   public TransformRayDemo()
   {
      final Graphics3DObject graphic = new Graphics3DObject();
      incrementalRotation.rotX(-Math.PI / 16);
      graphic.addCylinder(5.0, 0.2, YoAppearance.Red());

      final Graphics3DNode rayNode = new Graphics3DNode("laserRay", Graphics3DNodeType.VISUALIZATION);
      tInit.rotY(Math.PI / 4.0);
      // tInit.rotX(Math.PI / 4.0);
      rayNode.setTransform(tInit);
      rayNode.setGraphicsObject(graphic);

      final JMERenderer renderer = new JMERenderer(RenderType.AWTPANELS);
      renderer.setupSky();

      renderer.addRootNode(rayNode);

      PanBackAndForthTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder = new PanBackAndForthTrackingAndDollyPositionHolder(20.0, 20.0, 20.0);
      JMEViewportAdapter viewportAdapter = (JMEViewportAdapter) renderer.createNewViewport(null, false, false);
      ClassicCameraController classicCameraController = ClassicCameraController.createClassicCameraControllerAndAddListeners(viewportAdapter,
                                                           cameraTrackAndDollyVariablesHolder, renderer);
      viewportAdapter.setCameraController(classicCameraController);

      Canvas canvas = viewportAdapter.getCanvas();
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", canvas);
      panel.setPreferredSize(new Dimension(1800, 1080));

      JFrame jFrame = new JFrame("Example One");
      jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add("Center", panel);

      jFrame.pack();
      jFrame.setLocationRelativeTo(null);
      jFrame.setVisible(true);

      new Thread(new Runnable()
      {
         public void run()
         {
            while (true)
            {
               renderer.enqueue(new Callable<Object>()
               {
                  public Object call() throws Exception
                  {
                     rayNode.setTransform(generateTransform(rayNode.getTransform()));

//
//
//
//                   Transform3D rotatorZ = new Transform3D();
////                 rotator.setRotation(new AxisAngle4d(0, 0, 1, Math.PI / 100));
//                   rotatorZ.rotZ(Math.PI / 16);
//
////                 Transform3D rotatorX = new Transform3D(rayNode.getTransform());
////                 rotatorX.rotX(Math.PI / 16);
//
//                   // rotatorZ.mul(rotatorX);
//                   tAct.mul(rotatorZ);
//                   rotatorZ.mul(tInit, tAct);
//                   rayNode.setTransform(rotatorZ);
//                   // rayNode.getTransform().mul(rotatorX);

                     return null;
                  }
               });

               try
               {
                  Thread.sleep(50);
               }
               catch (InterruptedException e)
               {
                  e.printStackTrace();
               }
            }

         }
      }).start();
   }

   RigidBodyTransform incrementalRotation = new RigidBodyTransform();

   public RigidBodyTransform generateTransform(RigidBodyTransform init)
   {
      RigidBodyTransform rotated = new RigidBodyTransform();
      rotated.multiply(init, incrementalRotation);

      return rotated;
   }
}
