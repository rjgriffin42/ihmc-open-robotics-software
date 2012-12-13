package us.ihmc.graphics3DAdapter.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.ModifierKeyHolder;
import us.ihmc.graphics3DAdapter.SelectedListener;
import us.ihmc.graphics3DAdapter.camera.SimpleCameraTrackingAndDollyPositionHolder;
import us.ihmc.graphics3DAdapter.camera.ViewportAdapter;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.ThreadTools;

public class Graphics3DAdapterExampleTwo
{
   public void doExample(Graphics3DAdapter adapter)
   {
      Random random = new Random(1776L);
      
      Graphics3DNode node1 = new Graphics3DNode("node1", Graphics3DNodeType.JOINT);
      
      Transform3D transform1 = new Transform3D();
      transform1.setTranslation(new Vector3d(2.0, 0.0, 0.0));
      node1.setTransform(transform1);
      
      Graphics3DNode node2 = new Graphics3DNode("node2", Graphics3DNodeType.JOINT);
      Graphics3DNode rootNode = new Graphics3DNode("rootNode", Graphics3DNodeType.JOINT);
     
      Graphics3DObject object2 = Graphics3DAdapterExampleOne.createCubeObject(0.6);
      Graphics3DObject object1 = Graphics3DAdapterExampleOne.createRandomObject(random);
      
      node1.setGraphicsObject(object1);
      node2.setGraphicsObject(object2);
      
      rootNode.addChild(node1);
      rootNode.addChild(node2);
      
      adapter.addRootNode(rootNode);
      
      SelectedListener selectedListener = new SelectedListener()
      {
         public void selected(Graphics3DNode graphics3dNode, ModifierKeyHolder modifierKeyHolder, Point3d location, Point3d cameraLocation,
               Vector3d lookAtDirection)
         {
            System.out.println("Selected " + graphics3dNode.getName() + " @ location " + location);                        
            
         }
      };
      
      adapter.addSelectedListener(selectedListener);
      node2.addSelectedListener(selectedListener);

      SimpleCameraTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder = new SimpleCameraTrackingAndDollyPositionHolder();

      ViewportAdapter camera = adapter.createNewViewport(cameraTrackAndDollyVariablesHolder, null, null);
      Canvas canvas = camera.getCanvas();
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", canvas);
      
      JFrame jFrame = new JFrame("Example Two");
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add("Center", panel);
      
      jFrame.pack();
      jFrame.setVisible(true);
      jFrame.setSize(800, 600);
      
      double rotation = 0.0;
      
      int count = 0;
      
      while (true)
      {
         rotation = rotation + 0.01;
         node2.getTransform().rotZ(rotation);
         
         count++;
         if (count > 200)
         {
            Graphics3DObject randomObject = Graphics3DAdapterExampleOne.createRandomObject(random);
            node1.setGraphicsObject(randomObject);
            count = 0;
         }
         
         ThreadTools.sleep(1L);
      }
   }
}
