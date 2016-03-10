package us.ihmc.humanoidBehaviors.behaviors;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import bubo.clouds.FactoryPointCloudShape;
import bubo.clouds.detect.CloudShapeTypes;
import bubo.clouds.detect.PointCloudShapeFinder;
import bubo.clouds.detect.PointCloudShapeFinder.Shape;
import bubo.clouds.detect.wrapper.ConfigMultiShapeRansac;
import bubo.clouds.detect.wrapper.ConfigSurfaceNormals;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Sphere3D_F64;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidRobotics.communication.packets.DetectedObjectPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.PointCloudWorldPacket;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.tools.io.printing.PrintTools;

public class LocalizeBallBehavior extends BehaviorInterface
{

   private BooleanYoVariable ballFound = new BooleanYoVariable("ballFound", registry);
   private DoubleYoVariable ballRadius = new DoubleYoVariable("ballRadius", registry);
   private DoubleYoVariable ballX = new DoubleYoVariable("ballX", registry);
   private DoubleYoVariable ballY = new DoubleYoVariable("ballY", registry);
   private DoubleYoVariable ballZ = new DoubleYoVariable("ballZ", registry);
   private DoubleYoVariable totalBallsFound = new DoubleYoVariable("totalBallsFound", registry);
   private DoubleYoVariable smallestBallFound = new DoubleYoVariable("smallestBallFound", registry);
   

   
   
   
   ExecutorService executorService = Executors.newFixedThreadPool(2);
//   final int pointDropFactor = 4;
   private final static boolean DEBUG = false;



   private final ConcurrentListeningQueue<PointCloudWorldPacket> pointCloudQueue = new ConcurrentListeningQueue<PointCloudWorldPacket>();

   private final HumanoidReferenceFrames humanoidReferenceFrames;

   public LocalizeBallBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, HumanoidReferenceFrames referenceFrames)
   {
      super(outgoingCommunicationBridge);
      this.attachNetworkProcessorListeningQueue(pointCloudQueue, PointCloudWorldPacket.class);
      this.humanoidReferenceFrames = referenceFrames;

    

   }

   public boolean foundBall()
   {
      return ballFound.getBooleanValue();
   }

   public void reset()
   {
      ballFound.set(false);
   }

   public Point3d getBallLocation()
   {
      return new Point3d(ballX.getDoubleValue(), ballY.getDoubleValue(), ballZ.getDoubleValue());
   }

   PointCloudWorldPacket pointCloudPacket;
   PointCloudWorldPacket pointCloudPacketLatest = null;

   @Override
   public void doControl()
   {

      while ((pointCloudPacket = pointCloudQueue.getNewestPacket()) != null)
      {
         pointCloudPacketLatest = pointCloudPacket;
      }

      if (pointCloudPacketLatest != null)
      {
         Point3f[] points = pointCloudPacketLatest.getDecayingWorldScan();
         findBallsAndSaveResult(points);
      }

   }

   private void findBallsAndSaveResult(Point3f[] points)
   {
      ArrayList<Sphere3D_F64> balls = detectBalls(points);
      
     
      totalBallsFound.set(getNumberOfBallsFound());
      smallestBallFound.set(getSmallestRadius());


      
      int id = 4;
      for (Sphere3D_F64 ball : balls)
      {
    	  id++;
         RigidBodyTransform t = new RigidBodyTransform();
         t.setTranslation(ball.getCenter().x, ball.getCenter().y, ball.getCenter().z);
         sendPacketToNetworkProcessor(new DetectedObjectPacket(t, 4));
      }


      if (balls.size() > 0)
      {
         ballFound.set(true);
         ballRadius.set(balls.get(0).radius);
         ballX.set(balls.get(0).getCenter().x);
         ballY.set(balls.get(0).getCenter().y);
         ballZ.set(balls.get(0).getCenter().z);
      }
      else
      {
         ballFound.set(false);
         ballRadius.set(0);
         ballX.set(0);
         ballY.set(0);
         ballZ.set(0);
      }

   }
   
   
   
   
   public ArrayList<Sphere3D_F64> detectBalls(Point3f[] fullPoints)
   {

      ArrayList<Sphere3D_F64> foundBalls = new ArrayList<Sphere3D_F64>();
      // filter points
      ArrayList<Point3D_F64> pointsNearBy = new ArrayList<Point3D_F64>();
      for (Point3f tmpPoint : fullPoints)
      {
         pointsNearBy.add(new Point3D_F64(tmpPoint.x, tmpPoint.y, tmpPoint.z));
      }

//    filters =7; angleTolerance =0.9143273078940257; distanceThreashold = 0.08726045545980951; numNeighbors =41; maxDisance = 0.09815802524093345;

      
      // find plane
      ConfigMultiShapeRansac configRansac = ConfigMultiShapeRansac.createDefault(7, 0.9143273078940257, 0.08726045545980951, CloudShapeTypes.SPHERE);
      configRansac.minimumPoints = 30;
      PointCloudShapeFinder findSpheres = FactoryPointCloudShape.ransacSingleAll( new ConfigSurfaceNormals(41, 0.09815802524093345), configRansac);

      PrintStream out = System.out;
      System.setOut(new PrintStream(new OutputStream()
      {
         @Override
         public void write(int b) throws IOException
         {
         }
      }));
      try
      {
         findSpheres.process(pointsNearBy, null);
      } finally
      {
         System.setOut(out);
      }

      // sort large to small
      List<Shape> spheres = findSpheres.getFound();
      Collections.sort(spheres, new Comparator<Shape>()
      {
         
         @Override
         public int compare(Shape o1, Shape o2)
         {
            return Integer.compare(o1.points.size(), o2.points.size());
         };
      });

      if (spheres.size() > 0)
      {
         PrintTools.debug(DEBUG, this, "spheres.size() " + spheres.size());
         ballsFound = spheres.size();
         smallestRadius = ((Sphere3D_F64) spheres.get(0).getParameters()).getRadius();
      }
      for (Shape sphere : spheres)
      {
         Sphere3D_F64 sphereParams = (Sphere3D_F64) sphere.getParameters();
         PrintTools.debug(DEBUG, this, "sphere radius" + sphereParams.getRadius() + " center " + sphereParams.getCenter());

         if ((sphereParams.getRadius() < 0.152)&&(sphereParams.getRadius() > 0.102))// soccer ball -
         {
            foundBalls.add(sphereParams);
            PrintTools.debug(DEBUG, this, "------Found Soccer Ball radius" + sphereParams.getRadius() + " center " + sphereParams.getCenter());

            RigidBodyTransform t = new RigidBodyTransform();
            t.setTranslation(sphereParams.getCenter().x, sphereParams.getCenter().y, sphereParams.getCenter().z);
         }

      }
      return foundBalls;

   }
   private double ballsFound = 0;
   private double smallestRadius = 0;
   public double getNumberOfBallsFound()
   {
	   return ballsFound;
   }
   public double getSmallestRadius()
   {
	   return smallestRadius;
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
   }

   @Override
   public void stop()
   {
      defaultStop();
   }

   @Override
   public void enableActions()
   {

   }

   @Override
   public void pause()
   {
      defaultPause();
   }

   @Override
   public void resume()
   {
      defaultResume();
   }

   @Override
   public boolean isDone()
   {
      return ballFound.getBooleanValue();
   }

   @Override
   public void doPostBehaviorCleanup()
   {
      defaultPostBehaviorCleanup();
      ballFound.set(false);
   }

   @Override
   public boolean hasInputBeenSet()
   {
      // TODO Auto-generated method stub
      return true;
   }

   @Override
   public void initialize()
   {
      defaultPostBehaviorCleanup();
   }
}
