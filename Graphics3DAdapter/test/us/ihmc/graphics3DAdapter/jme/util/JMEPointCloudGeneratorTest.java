 package us.ihmc.graphics3DAdapter.jme.util;

import java.util.Random;

import us.ihmc.graphics3DAdapter.Graphics3DWorld;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.jme.JMEGraphics3DAdapter;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.lidar.LidarScan;
import us.ihmc.robotics.lidar.LidarScanParameters;
import us.ihmc.robotics.geometry.RigidBodyTransform;

import com.jme3.scene.Node;

public class JMEPointCloudGeneratorTest
{
   public static void main(String[] args)
   {
      new JMEPointCloudGeneratorTest().testJmePointCloudGenerator();
   }
   
   public void testJmePointCloudGenerator()
   {
      JMEGraphics3DAdapter jmeGraphics3DAdapter = new JMEGraphics3DAdapter(false);
      
      Graphics3DWorld world = new Graphics3DWorld(jmeGraphics3DAdapter);
      
      Graphics3DObject coordinateFrameObject = new Graphics3DObject();
      coordinateFrameObject.addCoordinateSystem(1.0);
      
      Graphics3DNode coordinateFrameNode = new Graphics3DNode(getClass().getSimpleName() + "CoordinateFrame", coordinateFrameObject);
      
      world.addChild(coordinateFrameNode);
      
      Random random = new Random();
      
      int numPoints = 1000;
      
      float[] ranges = RandomTools.generateRandomFloatArray(random, numPoints, 5.0f);
      
      LidarScanParameters lidarScanParameters = new LidarScanParameters(numPoints, 2* Math.PI, 0, Double.POSITIVE_INFINITY);
      
      RigidBodyTransform startTransform = new RigidBodyTransform();
      RigidBodyTransform endTransform = new RigidBodyTransform();
      
      endTransform.rotX(Math.PI / 4);
      LidarScan lidarScan = new LidarScan(lidarScanParameters, startTransform, endTransform, ranges, 1);
      
      JMEPointCloudGenerator jmePointCloudGenerator = new JMEPointCloudGenerator(jmeGraphics3DAdapter.getRenderer().getAssetManager());
      
      Node pointCloudNode = jmePointCloudGenerator.generatePointCloudGraph(lidarScan.getAllPoints3f());
      
      jmeGraphics3DAdapter.getRenderer().getZUpNode().attachChild(pointCloudNode);
      
      world.startWithGui();
   }
}
