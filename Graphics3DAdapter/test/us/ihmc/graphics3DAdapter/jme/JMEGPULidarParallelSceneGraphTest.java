package us.ihmc.graphics3DAdapter.jme;

import java.util.concurrent.Callable;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.GPULidarListener;
import us.ihmc.graphics3DAdapter.Graphics3DFrameListener;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.jme.lidar.JMEGPULidar;
import us.ihmc.graphics3DAdapter.jme.util.JMELidarScanVisualizer;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.graphics3DAdapter.utils.FlatHeightMap;
import us.ihmc.robotics.Axis;
import us.ihmc.tools.agileTesting.BambooAnnotations.BambooPlan;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.tools.agileTesting.BambooPlanType;
import us.ihmc.robotics.lidar.LidarScan;
import us.ihmc.robotics.lidar.LidarScanParameters;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.shapes.Sphere3d;
import us.ihmc.robotics.geometry.TransformTools;

@BambooPlan(planType={BambooPlanType.UI})
public class JMEGPULidarParallelSceneGraphTest
{
   /**
    * The blue sphere should not show up in the lidar data.
    */

	@EstimatedDuration(duration = 10.2)
	@Test(timeout = 51000)
   public void testGPULidarParallelSceneGraph()
   {
      JMEGraphics3DWorld world = new JMEGraphics3DWorld(getClass().getSimpleName(), new JMEGraphics3DAdapter(true));
      final JMERenderer renderer = world.getGraphics3DAdapter().getRenderer();

      final LidarScanParameters scanParameters = new LidarScanParameters(720, Math.PI / 2, 0.2, 1e3);

      final JMELidarScanVisualizer visualizer = new JMELidarScanVisualizer();

      AppearanceDefinition invisibleSphereAppearance = YoAppearance.Blue();
      invisibleSphereAppearance.setTransparency(0.5);
      Graphics3DObject invisibleSphereObject = new Graphics3DObject(new Sphere3d(0.5), invisibleSphereAppearance);
      final Graphics3DNode invisibleSphereNode = new Graphics3DNode("InvisibleSphereNode", Graphics3DNodeType.VISUALIZATION, invisibleSphereObject);
      invisibleSphereNode.translate(1.5, 1.0, 1.0);

      world.addChild(invisibleSphereNode);
      
      AppearanceDefinition visibleSphereAppearance = YoAppearance.Red();
      invisibleSphereAppearance.setTransparency(0.5);
      Graphics3DObject visibleSphereObject = new Graphics3DObject(new Sphere3d(0.5), visibleSphereAppearance);
      final Graphics3DNode visibleSphereNode = new Graphics3DNode("VisibleSphereNode", Graphics3DNodeType.JOINT, visibleSphereObject);
      visibleSphereNode.translate(1.5, -1.0, 1.0);

      world.addChild(visibleSphereNode);

      AppearanceDefinition wallAppearance = YoAppearance.Green();
      wallAppearance.setTransparency(0.5);
      Graphics3DObject wallObject = new Graphics3DObject();
      wallObject.addCube(0.1, 100, 100, wallAppearance);
      Graphics3DNode wallNode = new Graphics3DNode("WallyTheWall", Graphics3DNodeType.GROUND, wallObject);
      wallNode.translate(2.0, 0.0, -50);

      world.addChild(wallNode);

      world.getGraphics3DAdapter().getRenderer().setHeightMap(new FlatHeightMap());
      world.getGraphics3DAdapter().getRenderer().setGroundVisible(true);

      final Graphics3DNode lidarNode = new Graphics3DNode("lidar", Graphics3DNodeType.ROOTJOINT, new Graphics3DObject());
      lidarNode.getGraphics3DObject().addModelFile("models/hokuyo.dae", YoAppearance.Black());
      lidarNode.translate(0.5, Axis.Z);

      world.addChild(lidarNode);

      final JMEGPULidar gpuLidar = renderer.createGPULidar(scanParameters);
      gpuLidar.setTransformFromWorld(lidarNode.getTransform(), 0.0);
      gpuLidar.addGPULidarListener(new GPULidarListener()
      {
         @Override
         public void scan(float[] scan, RigidBodyTransform currentTransform, double time)
         {
            LidarScan lidarScan = new LidarScan(scanParameters, currentTransform, currentTransform, scan, 1);

            visualizer.updateLidarNodeTransform(lidarNode.getTransform());
            visualizer.addPointCloud(lidarScan.getAllPoints3f());

            RigidBodyTransform newTransform = new RigidBodyTransform(currentTransform);

            TransformTools.rotate(newTransform, Math.PI / 1e2, Axis.X);

            renderer.enqueue(new Callable<Object>()
            {
               @Override
               public Object call() throws Exception
               {
                  TransformTools.rotate(lidarNode.getTransform(), Math.PI / 1e2, Axis.X);

                  return null;
               }
            });

            gpuLidar.setTransformFromWorld(newTransform, 0.0);
         }
      });

      world.startWithGui();
      
      world.addFrameListener(new Graphics3DFrameListener()
      {
         double time = 0.0;
         double speed = 0.2;
         
         @Override
         public void postFrame(double timePerFrame)
         {
            // Spin spheres around
            
            time += timePerFrame;
            
            double invisibleY = Math.cos(time * speed); // Starts at 1.0
            double invisibleZ = -Math.sin(time * speed) + 1.0;
            
            double visibleY = Math.cos(time * speed + Math.PI); // Starts at -1.0
            double visibleZ = Math.sin(time * speed) + 1.0;
            
            invisibleSphereNode.translateTo(1.5, invisibleY, invisibleZ);
            visibleSphereNode.translateTo(1.5, visibleY, visibleZ);
         }
      });
      
      double keepAlive = 5;

      world.keepAlive(keepAlive);
      visualizer.keepAlive(keepAlive);

      world.stop();
      visualizer.stop();
   }
}
