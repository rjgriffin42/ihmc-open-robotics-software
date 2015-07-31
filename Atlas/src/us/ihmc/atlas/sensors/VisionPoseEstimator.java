package us.ihmc.atlas.sensors;

import georegression.struct.plane.PlaneGeneral3D_F64;
import georegression.struct.point.Point3D_F64;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.vecmath.Point3f;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.opencv.core.Rect;

import us.ihmc.SdfLoader.SDFFullRobotModelFactory;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.DetectedObjectPacket;
import us.ihmc.communication.producers.RobotConfigurationDataBuffer;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData.PointCloudDataReceiver;
import us.ihmc.humanoidOperatorInterface.sensors.DetectedObjectManager.DetectedObjectId;
import us.ihmc.ihmcPerception.OpenCVFaceDetector;
import us.ihmc.ihmcPerception.chessboardDetection.OpenCVChessboardPoseEstimator;
import us.ihmc.sensorProcessing.sensorData.CameraData;
import us.ihmc.sensorProcessing.sensorData.DRCStereoListener;
import org.apache.commons.lang3.tuple.ImmutablePair;
import us.ihmc.robotics.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import boofcv.struct.calib.IntrinsicParameters;
import bubo.clouds.FactoryPointCloudShape;
import bubo.clouds.detect.CloudShapeTypes;
import bubo.clouds.detect.PointCloudShapeFinder;
import bubo.clouds.detect.PointCloudShapeFinder.Shape;
import bubo.clouds.detect.wrapper.ConfigMultiShapeRansac;
import bubo.clouds.detect.wrapper.ConfigSurfaceNormals;

public class VisionPoseEstimator implements DRCStereoListener
{
   final OpenCVFaceDetector faceDetector = new OpenCVFaceDetector(0.5);
   private final static boolean DEBUG = false;
   private final RobotConfigurationDataBuffer robotConfigurationDataBuffer;

   ExecutorService executorService = Executors.newFixedThreadPool(2);

   PacketCommunicator communicator;
   LinkedBlockingQueue<ImmutablePair<CameraData, RigidBodyTransform>> imagesToProcess = new LinkedBlockingQueue<>(2);
   SDFFullRobotModelFactory modelFactory;
   PointCloudDataReceiver pointCloudDataReceiver;

   public VisionPoseEstimator(PacketCommunicator packetCommunicator, PointCloudDataReceiver pointCloudDataReceiver, SDFFullRobotModelFactory modelFactory,
         RobotConfigurationDataBuffer robotConfigurationDataBuffer, boolean runningOnRealRobot)
   {
      if (runningOnRealRobot)
      {
         //         startChessBoardDetector(new OpenCVChessboardPoseEstimator(4, 5, 0.01), DetectedObjectManager.DetectedObjectId.RIGHT_HAND.ordinal());
         startChessBoardDetector(new OpenCVChessboardPoseEstimator(4, 7, 0.01), DetectedObjectId.RIGHT_HAND.ordinal());
      }
      else
      {
         startChessBoardDetector(new OpenCVChessboardPoseEstimator(5, 6, 0.00935), DetectedObjectId.RIGHT_HAND.ordinal());
      }

      this.communicator = packetCommunicator;
      this.robotConfigurationDataBuffer = robotConfigurationDataBuffer;
      this.modelFactory = modelFactory;
      this.pointCloudDataReceiver = pointCloudDataReceiver;

      //      startFaceDetector();
      //      startPlaneDetector();
   }

   private void startFaceDetector()
   {
      executorService.submit(new Runnable()
      {

         @Override
         public void run()
         {

            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            while (true)
            {
               try
               {
                  ImmutablePair<CameraData, RigidBodyTransform> data = imagesToProcess.take();
                  long timeStart = System.currentTimeMillis();
                  //robotConfigurationDataBuffer.updateFullRobotModelWithNewestData(fullRobotModel, null);
                  //IntrinsicParameters intrinsicParameters = data.first().intrinsicParameters;
                  BufferedImage image = data.getLeft().image;
                  Rect[] faces = faceDetector.detect(image);

                  for (Rect face : faces)
                  {

                     RigidBodyTransform transform = new RigidBodyTransform(new Quat4d(), new Vector3d(face.x, face.y, face.width));
                     communicator.send(new DetectedObjectPacket(transform, DetectedObjectId.FACE.ordinal()));
                     PrintTools.debug(DEBUG, this, "face found " + face);
                  }

                  long detectionTimeInNanos = System.currentTimeMillis() - timeStart;
                  PrintTools.debug(VisionPoseEstimator.DEBUG, this, "detection time " + detectionTimeInNanos);

               }
               catch (InterruptedException e)
               {
                  e.printStackTrace();
               }
            }
         }
      });

   }

   private void startPlaneDetector()
   {
      final FullRobotModel fullRobotModel = modelFactory.createFullRobotModel();
      final int pointDropFactor = 4;
      final float searchRadius = 2.0f;
      executorService.submit(new Runnable()
      {

         @Override
         public void run()
         {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            while (true)
            {

               Point3f[] fullPoints = pointCloudDataReceiver.getDecayingPointCloudPoints();

               //get head
               robotConfigurationDataBuffer.updateFullRobotModelWithNewestData(fullRobotModel, null);
               RigidBodyTransform headToWorld = fullRobotModel.getHead().getBodyFixedFrame().getTransformToWorldFrame();
               Point3f head = new Point3f();
               headToWorld.transform(head);

               //filter points
               ArrayList<Point3D_F64> pointsNearBy = new ArrayList<Point3D_F64>();
               int counter = 0;
               for (Point3f tmpPoint : fullPoints)
               {
                  if (!Double.isNaN(tmpPoint.z) & counter % pointDropFactor == 0 && tmpPoint.distance(head) < searchRadius)
                     pointsNearBy.add(new Point3D_F64(tmpPoint.x, tmpPoint.y, tmpPoint.z));
                  counter++;
               }
               PrintTools.debug(DEBUG, this, "Points around center " + pointsNearBy.size());

               //find plane
               ConfigMultiShapeRansac configRansac = ConfigMultiShapeRansac.createDefault(30, 1.2, 0.010, CloudShapeTypes.PLANE);
               configRansac.minimumPoints = 100;
               PointCloudShapeFinder findPlanes = FactoryPointCloudShape.ransacSingleAll(new ConfigSurfaceNormals(20, 0.10), configRansac);

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
                  findPlanes.process(pointsNearBy, null);
               } finally
               {
                  System.setOut(out);
               }

               //sort large to small
               List<Shape> planes = findPlanes.getFound();
               Collections.sort(planes, new Comparator<Shape>()
               {

                  @Override
                  public int compare(Shape o1, Shape o2)
                  {
                     return -Integer.compare(o1.points.size(), o2.points.size());
                  };
               });

               for (Shape plane : planes)
               {
                  PlaneGeneral3D_F64 planeNormal = (PlaneGeneral3D_F64) plane.getParameters();
                  PrintTools.debug(DEBUG, this, "plane #point " + plane.points.size() + " normal " + planeNormal);

               }

            }
         }

      });

   }

   @Override
   public void newImageAvailable(CameraData data, RigidBodyTransform transformToWorld)
   {
      imagesToProcess.offer(new ImmutablePair<CameraData, RigidBodyTransform>(data, transformToWorld));
   }

   private void startChessBoardDetector(final OpenCVChessboardPoseEstimator chessboardDetector, final int targerId)
   {
      //      final FullRobotModel fullRobotModel=modelFactory.createFullRobotModel();
      executorService.submit(new Runnable()
      {

         @Override
         public void run()
         {

            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            while (true)
            {
               try
               {
                  ImmutablePair<CameraData, RigidBodyTransform> data = imagesToProcess.take();

                  //
                  long timeStart = System.currentTimeMillis();

                  //                  robotConfigurationDataBuffer.updateFullRobotModelWithNewestData(fullRobotModel, null);
                  IntrinsicParameters intrinsicParameters = data.getLeft().intrinsicParameters;
                  chessboardDetector.setCameraMatrix(intrinsicParameters.fx, intrinsicParameters.fy, intrinsicParameters.cx, intrinsicParameters.cy);

                  RigidBodyTransform targetToCameraOpticalFrame = chessboardDetector.detect(data.getLeft().image);
                  if (targetToCameraOpticalFrame != null)
                  {
                     RigidBodyTransform cameraToWorld = data.getRight();
                     RigidBodyTransform opticalFrameToCameraFrame = new RigidBodyTransform();
                     opticalFrameToCameraFrame.setEuler(-Math.PI / 2.0, 0.0, -Math.PI / 2);
                     RigidBodyTransform targetToWorld = new RigidBodyTransform();

                     targetToWorld.multiply(cameraToWorld, opticalFrameToCameraFrame);
                     targetToWorld.multiply(targetToCameraOpticalFrame);

                     communicator.send(new DetectedObjectPacket(targetToWorld, targerId));
                  }

                  long detectionTimeInNanos = System.currentTimeMillis() - timeStart;
                  PrintTools.debug(VisionPoseEstimator.DEBUG, this, "detection time " + detectionTimeInNanos);

               }
               catch (InterruptedException e)
               {
                  e.printStackTrace();
               }
            }
         }
      });

   }

}
