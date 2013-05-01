package us.ihmc.commonWalkingControlModules.desiredFootStep;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryWaypointGenerationMethod;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBodyTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.PauseCommand;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.io.streamingData.QueueBasedStreamingDataProducer;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.net.KryoObjectClient;
import us.ihmc.utilities.net.KryoObjectServer;
import us.ihmc.utilities.net.NetClassList;
import us.ihmc.utilities.net.NetProtocol;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.ObjectConsumer;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.test.JUnitTools;

/**
 * User: Matt
 * Date: 1/10/13
 */
public class FootstepDataTest
{
   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }
   
   @After
   public void showMemoryUsageAfterTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   /**
    * This test verifies that FootstepData can be sent and received using our current message passing utilities
    * @throws IOException 
    */
   @Test(timeout = 6000)
   public void testPassingFootstepData() throws IOException
   {
      // setup comms
      int port = 8833;
      QueueBasedStreamingDataProducer<FootstepData> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepData>();
      ObjectCommunicator tcpServer = createAndStartStreamingDataTCPServer(queueBasedStreamingDataProducer, port);
      FootstepDataConsumer footstepDataConsumer = new FootstepDataConsumer();
      ObjectCommunicator tcpClient = createStreamingDataConsumer(FootstepData.class, footstepDataConsumer, port);
      ThreadTools.sleep(100);
      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      for (Footstep footstep : sentFootsteps)
      {
         FootstepData footstepData = new FootstepData(footstep);
         queueBasedStreamingDataProducer.queueDataToSend(footstepData);
      }

      ThreadTools.sleep(100);

      tcpClient.close();
      tcpServer.close();

      // verify received correctly
      ArrayList<Footstep> receivedFootsteps = footstepDataConsumer.getReconstructedFootsteps();
      
      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);
   }

   @Test(timeout = 6000)
   public void testPassingFootstepPath() throws IOException
   {
      // setup comms
      int port = 8833;
      QueueBasedStreamingDataProducer<FootstepDataList> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepDataList>();
      ObjectCommunicator tcpServer = createAndStartStreamingDataTCPServer(queueBasedStreamingDataProducer, port);

      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer();
      ObjectCommunicator tcpClient = createStreamingDataConsumer(FootstepDataList.class, footstepPathConsumer, port);
      ThreadTools.sleep(100);
      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      FootstepDataList footstepsData = convertFootstepsToFootstepData(sentFootsteps);
 
      queueBasedStreamingDataProducer.queueDataToSend(footstepsData);
      ThreadTools.sleep(100);

      tcpClient.close();
      tcpServer.close();

      // verify received correctly
      ArrayList<Footstep> receivedFootsteps = footstepPathConsumer.getReconstructedFootsteps();
      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);
   }

   @Test(timeout = 6000)
   public void testPassingPauseCommand() throws IOException
   {
      // setup comms
      int port = 8833;
      QueueBasedStreamingDataProducer<PauseCommand> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<PauseCommand>();
      ObjectCommunicator tcpServer = createAndStartStreamingDataTCPServer(queueBasedStreamingDataProducer, port);

      PauseConsumer pauseConsumer = new PauseConsumer();
      ObjectCommunicator tcpClient = createStreamingDataConsumer(PauseCommand.class, pauseConsumer, port);
      ThreadTools.sleep(100);
      queueBasedStreamingDataProducer.startProducingData();

      // create test commands
      ArrayList<Boolean> commands = new ArrayList<Boolean>();
      Random random = new Random(77);
      int numberToTest = 100;
      for (int i = 0; i < numberToTest; i++)
      {
         boolean isPaused = random.nextBoolean();
         commands.add(isPaused);
         queueBasedStreamingDataProducer.queueDataToSend(new PauseCommand(isPaused));
      }

      ThreadTools.sleep(100);

      tcpServer.close();
      tcpClient.close();

      // verify received correctly
      ArrayList<Boolean> reconstructedCommands = pauseConsumer.getReconstructedCommands();
      for (int i = 0; i < commands.size(); i++)
      {
         Boolean isPaused = commands.get(i);
         Boolean reconstructedCommand = reconstructedCommands.get(i);
         assertTrue(isPaused.booleanValue() == reconstructedCommand.booleanValue());
      }
   }

   @Test(timeout = 6000)
   public void testPassingFootstepPathAndPauseCommands() throws IOException
   {
      // Create one server for two types of data
      int pathPort = 8833;

      QueueBasedStreamingDataProducer<FootstepDataList> pathQueueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepDataList>();

      QueueBasedStreamingDataProducer<PauseCommand> pauseQueueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<PauseCommand>();

      ObjectCommunicator streamingDataTCPServer = createAndStartStreamingDataTCPServer(pathQueueBasedStreamingDataProducer, pathPort);
      pauseQueueBasedStreamingDataProducer.addConsumer(streamingDataTCPServer);

      // create one client for two types of data
      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer();
      PauseConsumer pauseConsumer = new PauseConsumer();
      
      
      ObjectCommunicator streamingDataTCPClient = createStreamingDataConsumer(FootstepDataList.class, footstepPathConsumer, pathPort);
      streamingDataTCPClient.attachListener(PauseCommand.class, pauseConsumer);
      
      ThreadTools.sleep(100);
      pathQueueBasedStreamingDataProducer.startProducingData();
      pauseQueueBasedStreamingDataProducer.startProducingData();

      // send test footstep path
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      FootstepDataList footstepsData = convertFootstepsToFootstepData(sentFootsteps);

      pathQueueBasedStreamingDataProducer.queueDataToSend(footstepsData);
      ThreadTools.sleep(100);

      // send some commands
      ArrayList<Boolean> commands = new ArrayList<Boolean>();
      int numberToTest = 3;
      Random random = new Random(777);
      for (int i = 0; i < numberToTest; i++)
      {
         boolean isPaused = random.nextBoolean();
         commands.add(isPaused);
         pauseQueueBasedStreamingDataProducer.queueDataToSend(new PauseCommand(isPaused));
      }

      ThreadTools.sleep(100);

      // send another footstep path
      ArrayList<Footstep> sentFootsteps2 = createRandomFootsteps(50);
      footstepsData = convertFootstepsToFootstepData(sentFootsteps2);

      pathQueueBasedStreamingDataProducer.queueDataToSend(footstepsData);
      sentFootsteps.addAll(sentFootsteps2);
      ThreadTools.sleep(100);

      streamingDataTCPClient.close();
      streamingDataTCPServer.close();

      // verify footsteps received correctly
      ArrayList<Footstep> receivedFootsteps = footstepPathConsumer.getReconstructedFootsteps();
      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);

      // verify commands received correctly
      ArrayList<Boolean> reconstructedCommands = pauseConsumer.getReconstructedCommands();
      for (int i = 0; i < commands.size(); i++)
      {
         Boolean isPaused = commands.get(i);
         Boolean reconstructedCommand = reconstructedCommands.get(i);
         assertTrue(isPaused.booleanValue() == reconstructedCommand.booleanValue());
      }
   }

   @Test(timeout = 6000)
   public void testPassingFootstepStatus() throws IOException
   {
      // setup comms
      int port = 8833;
      QueueBasedStreamingDataProducer<FootstepData> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepData>();
      ObjectCommunicator tcpServer = createAndStartStreamingDataTCPServer(queueBasedStreamingDataProducer, port);

      FootstepStatusConsumer footstepStatusConsumer = new FootstepStatusConsumer();
      ObjectCommunicator tcpClient = createStreamingDataConsumer(FootstepStatus.class, footstepStatusConsumer, port);
      ThreadTools.sleep(100);
      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      Random random = new Random(777);
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      ArrayList<FootstepStatus> sentFootstepStatus = new ArrayList<FootstepStatus>();
      for (Footstep footstep : sentFootsteps)
      {
         FootstepStatus.Status status = FootstepStatus.Status.STARTED;
         boolean isComplete = random.nextBoolean();
         if (isComplete)
         {
            status = FootstepStatus.Status.COMPLETED;
         }

         FootstepStatus footstepStatus = new FootstepStatus(footstep, status);
         sentFootstepStatus.add(footstepStatus);
         queueBasedStreamingDataProducer.queueDataToSend(footstepStatus);
      }

      ThreadTools.sleep(100);

      tcpServer.close();
      tcpClient.close();

      // verify received correctly
      ArrayList<FootstepStatus> receivedFootsteps = footstepStatusConsumer.getReconstructedFootsteps();
      compareStatusSentWithReceived(sentFootstepStatus, receivedFootsteps);
   }
   
   private NetClassList getNetClassList()
   {
      NetClassList netClassList = new NetClassList();
      netClassList.addClass(NetProtocol.TCP, FootstepData.class);
      netClassList.addClass(NetProtocol.TCP, FootstepDataList.class);
      netClassList.addClass(NetProtocol.TCP, PauseCommand.class);
      netClassList.addClass(NetProtocol.TCP, FootstepStatus.class);
      
      netClassList.addType(ArrayList.class);
      netClassList.addType(Point3d.class);
      netClassList.addType(Quat4d.class);
      netClassList.addType(FootstepStatus.Status.class);
      netClassList.addType(TrajectoryWaypointGenerationMethod.class);
      
      return netClassList;
   }

   private ObjectCommunicator createAndStartStreamingDataTCPServer(QueueBasedStreamingDataProducer<?> queueBasedStreamingDataProducer, int port) throws IOException
   {
      
      KryoObjectServer server = new KryoObjectServer(port, port+1, getNetClassList());
      server.connect();
      queueBasedStreamingDataProducer.addConsumer(server);
      return server;
   }

   private <T> ObjectCommunicator createStreamingDataConsumer(Class<T> clazz, ObjectConsumer<T> consumer, int port) throws IOException
   {
      KryoObjectClient client = new KryoObjectClient("localhost", port, port+1, getNetClassList());
      client.connect();
      client.attachListener(clazz, consumer);
      return client;
   }

   private ArrayList<Footstep> createRandomFootsteps(int number)
   {
      Random random = new Random(77);
      ArrayList<Footstep> footsteps = new ArrayList<Footstep>();


      for (int footstepNumber = 0; footstepNumber < number; footstepNumber++)
      {
         RigidBody endEffector = createRigidBody("rigid_" + footstepNumber);
         ContactablePlaneBody contactablePlaneBody = ContactablePlaneBodyTools.createRandomContactablePlaneBodyForTests(random, endEffector);

         FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), new Point3d(footstepNumber, 0.0, 0.0),
                                        new Quat4d(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble()));
         
         
         PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("test", pose);

         boolean trustHeight = true;
         ReferenceFrame soleFrame = FootstepUtils.createSoleFrame(poseReferenceFrame, contactablePlaneBody); 
//         ArrayList<FramePoint> expectedContactPoints = new ArrayList<FramePoint>();
//         for (int i = 0; i < 3; i++)
//         {
//            FramePoint framePoint = new FramePoint(ReferenceFrame.getWorldFrame(), footstepNumber, i, 0.0);
//            expectedContactPoints.add(framePoint);
//         }
         
         List<FramePoint> expectedContactPoints = FootstepUtils.getContactPointsInFrame(contactablePlaneBody, soleFrame);
         
         Footstep footstep = new Footstep(contactablePlaneBody, poseReferenceFrame, soleFrame, expectedContactPoints, trustHeight);
         footsteps.add(footstep);
      }

      return footsteps;
   }

   private void compareFootstepsSentWithReceived(ArrayList<Footstep> sentFootsteps, ArrayList<Footstep> receivedFootsteps)
   {
      for (int i = 0; i < sentFootsteps.size(); i++)
      {
         Footstep sentFootstep = sentFootsteps.get(i);
         Footstep receivedFootstep = receivedFootsteps.get(i);

         assertTrue(sentFootstep.getBody().getName().equals(receivedFootstep.getBody().getName()));
         
         
         FramePose sentPose = sentFootstep.getPoseCopy();
         FramePose receivedPose = receivedFootstep.getPoseCopy();
         
         System.out.println("sentPose = " + sentPose);
         System.out.println("receivedPose = " + receivedPose);
         assertTrue(sentPose.epsilonEquals(receivedPose, 0.0001));

         for (int j = 0; j < sentFootstep.getExpectedContactPoints().size(); j++)
         {
            FramePoint sentFramePoint = sentFootstep.getExpectedContactPoints().get(j);
            FramePoint receivedFramePoint = receivedFootstep.getExpectedContactPoints().get(j);
            
            JUnitTools.assertTuple3dEquals(sentFramePoint.getPointCopy(), receivedFramePoint.getPointCopy(), 1e-4);
//            assertTrue(sentFramePoint.epsilonEquals(receivedFramePoint, 0.0001));
         }
      }
   }

   private void compareStatusSentWithReceived(ArrayList<FootstepStatus> sentFootstepStatus, ArrayList<FootstepStatus> receivedFootsteps)
   {
      for (int i = 0; i < sentFootstepStatus.size(); i++)
      {
         FootstepStatus footstepStatus = sentFootstepStatus.get(i);
         FootstepStatus reconstructedFootstepStatus = receivedFootsteps.get(i);
         assertTrue(footstepStatus.getStatus() == reconstructedFootstepStatus.getStatus());
         assertTrue(footstepStatus.getLocation().epsilonEquals(reconstructedFootstepStatus.getLocation(), 0.0001));
      }
   }

   private static FootstepDataList convertFootstepsToFootstepData(ArrayList<Footstep> footsteps)
   {
      FootstepDataList footstepsData = new FootstepDataList();

      for (Footstep footstep : footsteps)
      {
         FootstepData footstepData = new FootstepData(footstep);
         footstepsData.add(footstepData);
      }

      return footstepsData;
   }

   private class FootstepDataConsumer implements ObjectConsumer<FootstepData>
   {
      ArrayList<Footstep> reconstructedFootsteps = new ArrayList<Footstep>();


      public void consumeObject(FootstepData packet)
      {
         RigidBody endEffector = createRigidBody(packet.getRigidBodyName());
         ContactablePlaneBody contactablePlaneBody = ContactablePlaneBodyTools.createTypicalContactablePlaneBodyForTests(endEffector, ReferenceFrame
               .getWorldFrame());

         
         FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), packet.getLocation(), packet.getOrientation());
         PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("test", pose);

         boolean trustHeight = true;
         ReferenceFrame soleFrame = FootstepUtils.createSoleFrame(poseReferenceFrame, contactablePlaneBody); 
         
         ArrayList<FramePoint> expectedContactPoints = new ArrayList<FramePoint>();
         for (Point3d point : packet.getExpectedContactPoints())
         {
            FramePoint framePoint = new FramePoint(soleFrame, point);
            expectedContactPoints.add(framePoint);
         }
         
         Footstep footstep = new Footstep(contactablePlaneBody, poseReferenceFrame, soleFrame, expectedContactPoints, trustHeight);
         reconstructedFootsteps.add(footstep);
      }

      public ArrayList<Footstep> getReconstructedFootsteps()
      {
         return reconstructedFootsteps;
      }
   }


   private class FootstepPathConsumer implements ObjectConsumer<FootstepDataList>
   {
      ArrayList<Footstep> reconstructedFootstepPath = new ArrayList<Footstep>();


      public void consumeObject(FootstepDataList packet)
      {
         for (FootstepData footstepData : packet)
         {
            String name = footstepData.getRigidBodyName();
            RigidBody endEffector = createRigidBody(name);

            ContactablePlaneBody contactablePlaneBody = ContactablePlaneBodyTools.createTypicalContactablePlaneBodyForTests(endEffector, ReferenceFrame.getWorldFrame());

            FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
            PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("test", pose);

            boolean trustHeight = true;
            ReferenceFrame soleFrame = FootstepUtils.createSoleFrame(poseReferenceFrame, contactablePlaneBody);

            ArrayList<FramePoint> expectedContactPoints = new ArrayList<FramePoint>();
            for (Point3d point : footstepData.getExpectedContactPoints())
            {
               FramePoint framePoint = new FramePoint(soleFrame, point);
               expectedContactPoints.add(framePoint);
            }
            
            Footstep footstep = new Footstep(contactablePlaneBody, poseReferenceFrame, soleFrame, expectedContactPoints, trustHeight);
            reconstructedFootstepPath.add(footstep);
         }
      }

      public ArrayList<Footstep> getReconstructedFootsteps()
      {
         return reconstructedFootstepPath;
      }
   }

   private RigidBody createRigidBody(String name)
   {
      RigidBody elevator = new RigidBody("elevator", ReferenceFrame.getWorldFrame());
      SixDoFJoint joint = new SixDoFJoint("joint", elevator, elevator.getBodyFixedFrame());
      return ScrewTools.addRigidBody(name, joint, new Matrix3d(), 0.0, new Vector3d());
   }


   private class PauseConsumer implements ObjectConsumer<PauseCommand>
   {
      ArrayList<Boolean> reconstructedCommands = new ArrayList<Boolean>();


      public void consumeObject(PauseCommand packet)
      {
         reconstructedCommands.add(packet.isPaused());
      }

      public ArrayList<Boolean> getReconstructedCommands()
      {
         return reconstructedCommands;
      }
   }


   private class FootstepStatusConsumer implements ObjectConsumer<FootstepStatus>
   {
      private ArrayList<FootstepStatus> reconstructedFootstepStatuses = new ArrayList<FootstepStatus>();


      public void consumeObject(FootstepStatus footstepStatus)
      {
         reconstructedFootstepStatuses.add(footstepStatus);
      }

      public ArrayList<FootstepStatus> getReconstructedFootsteps()
      {
         return reconstructedFootstepStatuses;
      }
   }
}
