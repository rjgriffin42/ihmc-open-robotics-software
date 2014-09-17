package us.ihmc.graphics3DAdapter.utils;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import us.ihmc.utilities.math.geometry.Transform3d;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.camera.ClassicCameraController;
import us.ihmc.graphics3DAdapter.camera.SimpleCameraTrackingAndDollyPositionHolder;
import us.ihmc.graphics3DAdapter.camera.ViewportAdapter;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DInstruction;
import us.ihmc.graphics3DAdapter.input.ModifierKeyInterface;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.BlinkingDaemon;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.VectorTuple;
import us.ihmc.utilities.math.geometry.Sphere3d;

public class GraphicsDemoTools
{
   public static void addBlinkingAppearance(ArrayList<Runnable> runnables, Graphics3DObject teapotObject)
   {
      Graphics3DInstruction teapotAppearanceHolder = teapotObject.addTeaPot(YoAppearance.Red());
      BlinkRunnable blinker = new BlinkRunnable(teapotAppearanceHolder);
      runnables.add(blinker);
   }
   public static void addJiggle(Graphics3DNode node,Graphics3DAdapter adapter,ArrayList<Runnable> runnables)
   {
//      adapter.removeRootNode(node);
      Graphics3DNode jiggleNode = new Graphics3DNode("jiggle", Graphics3DNodeType.JOINT);
      jiggleNode.addChild(node);
//      adapter.addRootNode(jiggleNode);
      RotateAndScaleNodeRunnable jiggler = new RotateAndScaleNodeRunnable(node);
      runnables.add(jiggler);    
   }
   public static void addFirstCamera(Graphics3DAdapter graphics3DAdapter, PanBackAndForthTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder)
   {
      ViewportAdapter viewportAdapter = graphics3DAdapter.createNewViewport(null, false, false);
      ClassicCameraController classicCameraController = ClassicCameraController.createClassicCameraControllerAndAddListeners(viewportAdapter,
                                                           cameraTrackAndDollyVariablesHolder, graphics3DAdapter);
      viewportAdapter.setCameraController(classicCameraController);
      classicCameraController.setTracking(true, true, false, false);
      Canvas canvas = viewportAdapter.getCanvas();
      createNewWindow(canvas);
   }

   public static Graphics3DNode addRotatingScalingNode(ArrayList<Runnable> runnables, Graphics3DObject teapotObject)
   {
      Graphics3DNode teapotAndSphereNode = new Graphics3DNode("teaPot", Graphics3DNodeType.JOINT);
      teapotAndSphereNode.setGraphicsObject(teapotObject);
      RotateAndScaleNodeRunnable rotator = new RotateAndScaleNodeRunnable(teapotAndSphereNode);
      runnables.add(rotator);

      return teapotAndSphereNode;
   }

   public static void addSecondCamera(Graphics3DAdapter graphics3DAdapter, PanBackAndForthTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder)
   {
      ViewportAdapter secondCamera = graphics3DAdapter.createNewViewport(null, false, false);
      ClassicCameraController secondController = ClassicCameraController.createClassicCameraControllerAndAddListeners(secondCamera,
                                                    cameraTrackAndDollyVariablesHolder, graphics3DAdapter);
      secondCamera.setCameraController(secondController);
      createNewWindow(secondCamera.getCanvas());
   }

   public static void addSimpleSelectedListener(Graphics3DAdapter graphics3DAdapter, Graphics3DNode box)
   {
      SelectedListener selectedListener = new SelectedListener()
      {
         public void selected(Graphics3DNode graphics3dNode, ModifierKeyInterface modifierKeyHolder, Point3d location, Point3d cameraLocation,
                              Quat4d cameraRotation)
         {
            System.out.println("Selected " + graphics3dNode.getName() + " @ location " + location);

         }
      };


      graphics3DAdapter.addSelectedListener(selectedListener);
      box.addSelectedListener(selectedListener);
   }

   public static void buildBlinkingRotatingTeapot(Graphics3DAdapter graphics3DAdapter, ArrayList<Runnable> runnables)
   {
      Graphics3DObject teapotObject = new Graphics3DObject();
      GraphicsDemoTools.addBlinkingAppearance(runnables, teapotObject);
      Graphics3DNode teapotAndSphereNode = GraphicsDemoTools.addRotatingScalingNode(runnables, teapotObject);
      graphics3DAdapter.addRootNode(teapotAndSphereNode);
   }

   public static void continuouslyRunAllRunnables(ArrayList<Runnable> runnables)
   {
      while (true)
      {
         for (Runnable runnable : runnables)
         {
            runnable.run();
         }

         try
         {
            Thread.sleep(10L);
         }
         catch (InterruptedException e)
         {
            e.printStackTrace();
         }
      }
   }

   public static Graphics3DObject createCubeObject(double lengthWidthHeight)
   {
      Graphics3DObject cube = new Graphics3DObject();
      cube.addCube(lengthWidthHeight, lengthWidthHeight, lengthWidthHeight, YoAppearance.Red());

      return cube;
   }

   public static Graphics3DObject createCylinderObject(double radius)
   {
      Graphics3DObject cylinder = new Graphics3DObject();
      double height = 1.0;
      cylinder.addCylinder(height, radius, YoAppearance.Pink());

      return cylinder;
   }

   public static void createNewWindow(Canvas canvas)
   {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", canvas);

      JFrame jFrame = new JFrame("Example One");
      jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add("Center", panel);

      jFrame.pack();
      jFrame.setVisible(true);
      jFrame.setSize(800, 600);
   }

   public static Graphics3DObject createRandomObject(Random random)
   {
      int selection = random.nextInt(3);

      switch (selection)
      {
         case 0 :
         {
            return createCubeObject(random.nextDouble());
         }

         case 1 :
         {
            return createSphereObject(random.nextDouble() * 0.5);
         }

         case 2 :
         {
            return createCylinderObject(random.nextDouble() * 0.5);
         }

         default :
         {
            throw new RuntimeException("Should not get here");
         }
      }
   }

   public static Graphics3DObject createSphereObject(double radius)
   {
      Graphics3DObject sphere = new Graphics3DObject();
      sphere.addSphere(radius, YoAppearance.Green());

      return sphere;
   }

   public static void createWindow(Canvas canvas1, Canvas canvas2)
   {
      JPanel panel = new JPanel(new FlowLayout());
      panel.add("Center", canvas1);
      panel.add("East", canvas2);
      canvas1.setPreferredSize(new Dimension(390, 600));
      canvas2.setPreferredSize(new Dimension(390, 600));

      JFrame jFrame = new JFrame("Example One");
      jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add(panel);

      jFrame.pack();
      jFrame.setVisible(true);
      jFrame.setSize(800, 600);
   }
   
   public static Graphics3DNode createPointCloud(String name, List<Point3d> worldPoints, double pointRadius, AppearanceDefinition appearance)
   {
      // TODO Change to point sprite mesh
      Graphics3DNode pointCloud = new Graphics3DNode("PointCloud" + name);

      for (int i = 0; i < worldPoints.size(); i++)
      {
         Graphics3DNode worldPointNode = new Graphics3DNode(name + "point" + i, Graphics3DNodeType.VISUALIZATION,
               new Graphics3DObject(new Sphere3d(pointRadius), appearance));
         worldPointNode.translate(worldPoints.get(i).getX(), worldPoints.get(i).getY(), worldPoints.get(i).getZ());
         
         pointCloud.addChild(worldPointNode);
      }
      
      return pointCloud;
   }

   public static void daemonizeAllRunnables(ArrayList<Runnable> runnables)
   {
      for (Runnable runnable : runnables)
      {
         BlinkingDaemon.startAsBlinkingDaemon(runnable, 10L);
      }
   }

   public static void setupCameras(Graphics3DAdapter graphics3DAdapter)
   {
      PanBackAndForthTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder = new PanBackAndForthTrackingAndDollyPositionHolder(0.0, 2.0, 0.2);
      addFirstCamera(graphics3DAdapter, cameraTrackAndDollyVariablesHolder);
      addSecondCamera(graphics3DAdapter, cameraTrackAndDollyVariablesHolder);
   }

   public static Graphics3DNode setupStaticBox(Graphics3DAdapter graphics3DAdapter)
   {
      Graphics3DNode box = new Graphics3DNode("box", Graphics3DNodeType.JOINT);
      Graphics3DObject boxGraphics = new Graphics3DObject();
      boxGraphics.addCube(1.0, 1.0, 1.0, YoAppearance.Green());
      box.setGraphicsObject(boxGraphics);
      graphics3DAdapter.addRootNode(box);

      return box;
   }

   public static class BlinkRunnable implements Runnable
   {
      private final Graphics3DInstruction instruction;
      private double transparency = 0.0;

      public BlinkRunnable(Graphics3DInstruction instruction)
      {
         this.instruction = instruction;
      }

      public void run()
      {
         transparency += 0.01;
         if (transparency > 1.0)
            transparency = 0.0;

         Color3f color = new Color3f((float) Math.random(), (float) Math.random(), (float) Math.random());
         YoAppearanceRGBColor appearance = new YoAppearanceRGBColor(color, 0.0);
         appearance.setTransparency(transparency);
         instruction.setAppearance(appearance);
      }

   }


   public static class PanBackAndForthTrackingAndDollyPositionHolder extends SimpleCameraTrackingAndDollyPositionHolder implements Runnable
   {
      private final double panXOffset, panXAmplitude, panXFrequency;
      private long startTime = System.currentTimeMillis();

      public PanBackAndForthTrackingAndDollyPositionHolder(double panXOffset, double panXAmplitude, double panXFrequency)
      {
         this.panXOffset = panXOffset;
         this.panXAmplitude = panXAmplitude;
         this.panXFrequency = panXFrequency;

         ThreadTools.startAsDaemon(this, "Pan Tracking & Dolly Daemon");
      }

      public void run()
      {
         while (true)
         {
            long currentTime = System.currentTimeMillis();
            double time = (currentTime - startTime) * 0.001;

            double cameraTrackingX = panXOffset + panXAmplitude * Math.sin(2.0 * Math.PI * panXFrequency * time);
            this.setTrackingX(cameraTrackingX);

            ThreadTools.sleep(100L);

         }

      }


   }


   public static class RotateAndScaleNodeRunnable implements Runnable
   {
      private final Graphics3DNode node;
      private final RotateScaleParametersHolder parametersHolder;

      public RotateAndScaleNodeRunnable(Graphics3DNode node)
      {
         this(node,generateDefaultParameters());
      }
     
      
      public RotateAndScaleNodeRunnable(Graphics3DNode node, RotateScaleParametersHolder parametersHolder)
      {
         this.node = node;
         this.parametersHolder=parametersHolder;
      }
      
       private static RotateScaleParametersHolder generateDefaultParameters()
      {
         SimpleBounceTrajectory xTrajectory = new SimpleBounceTrajectory(0.5, 20, 0.01);
         SimpleBounceTrajectory zRotTrajectory = new SimpleBounceTrajectory(0, 100, 0.01);
         SimpleBounceTrajectory scaleTrajectory = new SimpleBounceTrajectory(0.1, 2.0, 0.01);
         SimpleBounceTrajectory zeroTrajectory = new SimpleBounceTrajectory();
         VectorTuple<SimpleBounceTrajectory> translation = new VectorTuple<SimpleBounceTrajectory>(xTrajectory,zeroTrajectory.copy(),zeroTrajectory.copy());
         VectorTuple<SimpleBounceTrajectory> rotation = new VectorTuple<SimpleBounceTrajectory>(zeroTrajectory.copy(),zeroTrajectory.copy(),zRotTrajectory);

         return new RotateScaleParametersHolder(translation,rotation,scaleTrajectory);
      }

      public void run()
      {
         Transform3d transform = new Transform3d();
         transform.setEuler(nextVector3d(parametersHolder.getRotationTrajectory()));
         transform.setTranslation(nextVector3d(parametersHolder.getTranslationTrajectory()));
         transform.setScale(parametersHolder.getScaleTrajectory().getNextValue());
         node.setTransform(transform);
      }
      public static Vector3d nextVector3d(VectorTuple<SimpleBounceTrajectory> trajectoryVector)
      {
         return new Vector3d(trajectoryVector.x().getNextValue(),trajectoryVector.y().getNextValue(),trajectoryVector.z().getNextValue());
      }
   }


   public static class RotateScaleParametersHolder
   {
      private final VectorTuple<SimpleBounceTrajectory> rotationTrajectory;
      private final SimpleBounceTrajectory scaleTrajectory;
      private final VectorTuple<SimpleBounceTrajectory> translationTrajectory;

      public RotateScaleParametersHolder(VectorTuple<SimpleBounceTrajectory> translationTrajectory, VectorTuple<SimpleBounceTrajectory> rotationTrajectory,
                                         SimpleBounceTrajectory scaleTrajectory)
      {
         this.translationTrajectory = translationTrajectory;
         this.rotationTrajectory = rotationTrajectory;
         this.scaleTrajectory = scaleTrajectory;
      }

      public VectorTuple<SimpleBounceTrajectory> getRotationTrajectory()
      {
         return rotationTrajectory;
      }

      public SimpleBounceTrajectory getScaleTrajectory()
      {
         return scaleTrajectory;
      }

      public VectorTuple<SimpleBounceTrajectory> getTranslationTrajectory()
      {
         return translationTrajectory;
      }

   }


   public static class SimpleBounceTrajectory
   {
      private double current;
      private boolean increasing;
      private final double max;
      private final double min;
      private final double rate;

      public SimpleBounceTrajectory()
      {
         this(0.0, 0.0, 0.0);
      }

      public SimpleBounceTrajectory(double min, double max, double rate)
      {
         this.min = min;
         this.max = max;
         this.rate = Math.abs(rate);
         current = min;
         increasing = true;
      }

      public SimpleBounceTrajectory copy()
      {
         SimpleBounceTrajectory ret = new SimpleBounceTrajectory(this.min, this.max, this.rate);
         ret.setCurrent(this.current, this.increasing);

         return ret;
      }

      public double getNextValue()
      {
         if (increasing)
         {
            current += rate;
         }
         else
         {
            current -= rate;
         }

         if (current > max)
         {
            current = max;
            increasing = false;
         }

         if (current < min)
         {
            current = min;
            increasing = true;
         }

         return current;
      }

      public void setCurrent(double current, boolean increasing)
      {
         this.current = current;
         this.increasing = increasing;
      }


   }

}
