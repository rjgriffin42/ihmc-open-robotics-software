package us.ihmc.commonWalkingControlModules.desiredFootStep;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBodyTools;
import us.ihmc.communication.net.NetClassList;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.KryoPacketClientEndPointCommunicator;
import us.ihmc.communication.packetCommunicator.KryoPacketServer;
import us.ihmc.communication.packetCommunicator.interfaces.PacketCommunicator;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.communication.packets.walking.FootstepStatus;
import us.ihmc.communication.packets.walking.PauseCommand;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.unitTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.TrajectoryGenerationMethod;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.utilities.humanoidRobot.footstep.Footstep;

/**
 * User: Matt
 * Date: 1/10/13
 */
public class FootstepDataTest
{
   private static final RobotSide robotSide = RobotSide.LEFT;

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

	@AverageDuration
	@Test(timeout=300000) //(timeout = 6000)
   public void testPassingFootstepData() throws IOException
   {
      // setup comms
      int port = 8833;
//      QueueBasedStreamingDataProducer<FootstepData> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepData>("FootstepData");
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);
      FootstepDataConsumer footstepDataConsumer = new FootstepDataConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(FootstepData.class, footstepDataConsumer, port);
      ThreadTools.sleep(100);
//      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      for (Footstep footstep : sentFootsteps)
      {
    	  Point3d location = new Point3d();
    	  Quat4d orientation = new Quat4d();
    	  footstep.getPose(location, orientation);
    	  
         FootstepData footstepData = new FootstepData(robotSide, location, orientation);
         tcpServer.send(footstepData);
//         queueBasedStreamingDataProducer.queueDataToSend(footstepData);
      }

      ThreadTools.sleep(100);

      tcpClient.close();
      tcpServer.close();

      // verify received correctly
      ArrayList<Footstep> receivedFootsteps = footstepDataConsumer.getReconstructedFootsteps();

      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);
   }

	@AverageDuration
	@Test(timeout = 6000)
   public void testPassingFootstepPath() throws IOException
   {
      Random random = new Random(1582l);
      // setup comms
      int port = 8833;
//      QueueBasedStreamingDataProducer<FootstepDataList> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepDataList>("FootstepDataList");
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);

      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(FootstepDataList.class, footstepPathConsumer, port);
      ThreadTools.sleep(100);
//      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      FootstepDataList footstepsData = convertFootstepsToFootstepData(sentFootsteps,random.nextDouble(), random.nextDouble());

      tcpServer.send(footstepsData);
      ThreadTools.sleep(100);

      tcpClient.close();
      tcpServer.close();

      // verify received correctly
      ArrayList<Footstep> receivedFootsteps = footstepPathConsumer.getReconstructedFootsteps();
      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);
   }

	@AverageDuration
	@Test(timeout = 6000)
   public void testPassingPauseCommand() throws IOException
   {
      // setup comms
      int port = 8833;
//      QueueBasedStreamingDataProducer<PauseCommand> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<PauseCommand>("PauseCommand");
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);

      PauseConsumer pauseConsumer = new PauseConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(PauseCommand.class, pauseConsumer, port);
      ThreadTools.sleep(100);
//      queueBasedStreamingDataProducer.startProducingData();

      // create test commands
      ArrayList<Boolean> commands = new ArrayList<Boolean>();
      Random random = new Random(77);
      int numberToTest = 100;
      for (int i = 0; i < numberToTest; i++)
      {
         boolean isPaused = random.nextBoolean();
         commands.add(isPaused);
         tcpServer.send(new PauseCommand(isPaused));
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

	@AverageDuration
	@Test(timeout = 6000)
   public void testPassingFootstepPathAndPauseCommands() throws IOException
   {
      // Create one server for two types of data
      int pathPort = 8833;

//      QueueBasedStreamingDataProducer<FootstepDataList> pathQueueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepDataList>("FootstepDataList");

//      QueueBasedStreamingDataProducer<PauseCommand> pauseQueueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<PauseCommand>("PauseCommand");

      PacketCommunicator streamingDataTCPServer = createAndStartStreamingDataTCPServer( pathPort);
//      pauseQueueBasedStreamingDataProducer.addConsumer(streamingDataTCPServer);

      // create one client for two types of data
      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer();
      PauseConsumer pauseConsumer = new PauseConsumer();

      PacketCommunicator streamingDataTCPClient = createStreamingDataConsumer(FootstepDataList.class, footstepPathConsumer, pathPort);
      streamingDataTCPClient.attachListener(PauseCommand.class, pauseConsumer);

      ThreadTools.sleep(100);
//      pathQueueBasedStreamingDataProducer.startProducingData();
//      pauseQueueBasedStreamingDataProducer.startProducingData();

      Random random = new Random(777);

      // send test footstep path
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      FootstepDataList footstepsData = convertFootstepsToFootstepData(sentFootsteps, random.nextDouble(), random.nextDouble());

      streamingDataTCPServer.send(footstepsData);
      ThreadTools.sleep(100);

      // send some commands
      ArrayList<Boolean> commands = new ArrayList<Boolean>();
      int numberToTest = 3;
      for (int i = 0; i < numberToTest; i++)
      {
         boolean isPaused = random.nextBoolean();
         commands.add(isPaused);
         streamingDataTCPServer.send(new PauseCommand(isPaused));
      }

      ThreadTools.sleep(100);

      // send another footstep path
      ArrayList<Footstep> sentFootsteps2 = createRandomFootsteps(50);
      footstepsData = convertFootstepsToFootstepData(sentFootsteps2, random.nextDouble(), random.nextDouble());

      streamingDataTCPServer.send(footstepsData);
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

	@AverageDuration
	@Test(timeout = 6000)
   public void testPassingFootstepStatus() throws IOException
   {
      // setup comms
      int port = 8833;
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);

      FootstepStatusConsumer footstepStatusConsumer = new FootstepStatusConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(FootstepStatus.class, footstepStatusConsumer, port);
      ThreadTools.sleep(100);

      // create test footsteps
      Random random = new Random(777);
      ArrayList<FootstepStatus> sentFootstepStatus = new ArrayList<FootstepStatus>();
      for (int i = 0; i < 50; i++)
      {
         FootstepStatus.Status status = FootstepStatus.Status.STARTED;
         boolean isComplete = random.nextBoolean();
         if (isComplete)
         {
            status = FootstepStatus.Status.COMPLETED;
         }

         FootstepStatus footstepStatus = new FootstepStatus(status, i);
         sentFootstepStatus.add(footstepStatus);
         tcpServer.send(footstepStatus);
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
      netClassList.registerPacketClass(FootstepData.class);
      netClassList.registerPacketClass(FootstepDataList.class);
      netClassList.registerPacketClass(PauseCommand.class);
      netClassList.registerPacketClass(FootstepStatus.class);

      netClassList.registerPacketField(ArrayList.class);
      netClassList.registerPacketField(Point3d.class);
      netClassList.registerPacketField(Quat4d.class);
      netClassList.registerPacketField(PacketDestination.class);
      netClassList.registerPacketField(FootstepStatus.Status.class);
      netClassList.registerPacketField(TrajectoryGenerationMethod.class);
      netClassList.registerPacketField(RobotSide.class);

      return netClassList;
   }

   private PacketCommunicator createAndStartStreamingDataTCPServer(int port)
         throws IOException
   {

      
      KryoPacketServer server = new KryoPacketServer(port, getNetClassList(), PacketDestination.CONTROLLER.ordinal(), getClass().getSimpleName() + "server");
      server.connect();
//      queueBasedStreamingDataProducer.addConsumer(server);
      return server;
   }

   private <T extends Packet> PacketCommunicator createStreamingDataConsumer(Class<T> clazz, PacketConsumer<T> consumer, int port) throws IOException
   {
      KryoPacketClientEndPointCommunicator client = new KryoPacketClientEndPointCommunicator("localhost", port, getNetClassList(), PacketDestination.NETWORK_PROCESSOR.ordinal(), getClass().getSimpleName() + "client");
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
         RigidBody endEffector = createRigidBody(robotSide);
         ContactablePlaneBody contactablePlaneBody = ContactablePlaneBodyTools.createRandomContactablePlaneBodyForTests(random, endEffector);

         FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), new Point3d(footstepNumber, 0.0, 0.0), new Quat4d(random.nextDouble(),
               random.nextDouble(), random.nextDouble(), random.nextDouble()));

         PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("test", pose);

         boolean trustHeight = true;
         Footstep footstep = new Footstep(contactablePlaneBody.getRigidBody(), robotSide, contactablePlaneBody.getSoleFrame(), poseReferenceFrame, trustHeight);
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

         assertTrue(sentFootstep.epsilonEquals(receivedFootstep, 1e-4));
      }
   }

   private void compareStatusSentWithReceived(ArrayList<FootstepStatus> sentFootstepStatus, ArrayList<FootstepStatus> receivedFootsteps)
   {
      for (int i = 0; i < sentFootstepStatus.size(); i++)
      {
         FootstepStatus footstepStatus = sentFootstepStatus.get(i);
         FootstepStatus reconstructedFootstepStatus = receivedFootsteps.get(i);
         assertTrue(footstepStatus.getStatus() == reconstructedFootstepStatus.getStatus());
      }
   }

   private static FootstepDataList convertFootstepsToFootstepData(ArrayList<Footstep> footsteps, double swingTime,
         double transferTime)
   {
      FootstepDataList footstepsData = new FootstepDataList(swingTime, transferTime);

      for (Footstep footstep : footsteps)
      {
         footstepsData.add(new FootstepData(footstep));
      }

      return footstepsData;
   }

   private class FootstepDataConsumer implements PacketConsumer<FootstepData>
   {
      ArrayList<Footstep> reconstructedFootsteps = new ArrayList<Footstep>();

      @Override
      public void receivedPacket(FootstepData packet)
      {
         RigidBody endEffector = createRigidBody(packet.getRobotSide());
         ContactablePlaneBody contactablePlaneBody = ContactablePlaneBodyTools.createTypicalContactablePlaneBodyForTests(endEffector,
               ReferenceFrame.getWorldFrame());

         FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), packet.getLocation(), packet.getOrientation());
         PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("test", pose);

         boolean trustHeight = true;
         Footstep footstep = new Footstep(contactablePlaneBody.getRigidBody(), packet.getRobotSide(), contactablePlaneBody.getSoleFrame(), poseReferenceFrame, trustHeight);
         reconstructedFootsteps.add(footstep);
      }

      public ArrayList<Footstep> getReconstructedFootsteps()
      {
         return reconstructedFootsteps;
      }
   }

   private class FootstepPathConsumer implements PacketConsumer<FootstepDataList>
   {
      ArrayList<Footstep> reconstructedFootstepPath = new ArrayList<Footstep>();

      @Override
      public void receivedPacket(FootstepDataList packet)
      {
         for (FootstepData footstepData : packet)
         {
            RigidBody endEffector = createRigidBody(footstepData.getRobotSide());

            ContactablePlaneBody contactablePlaneBody = ContactablePlaneBodyTools.createTypicalContactablePlaneBodyForTests(endEffector,
                  ReferenceFrame.getWorldFrame());

            FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
            PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("test", pose);

            boolean trustHeight = true;
            Footstep footstep = new Footstep(contactablePlaneBody.getRigidBody(), footstepData.getRobotSide(), contactablePlaneBody.getSoleFrame(), poseReferenceFrame, trustHeight);
            reconstructedFootstepPath.add(footstep);
         }
      }

      public ArrayList<Footstep> getReconstructedFootsteps()
      {
         return reconstructedFootstepPath;
      }
   }

   private RigidBody createRigidBody(RobotSide robotSide)
   {
      return createRigidBody(robotSide.getCamelCaseNameForStartOfExpression() + "Foot");
   }

   private RigidBody createRigidBody(String name)
   {
      RigidBody elevator = new RigidBody("elevator", ReferenceFrame.getWorldFrame());
      SixDoFJoint joint = new SixDoFJoint("joint", elevator, elevator.getBodyFixedFrame());
      return ScrewTools.addRigidBody(name, joint, new Matrix3d(), 0.0, new Vector3d());
   }

   private class PauseConsumer implements PacketConsumer<PauseCommand>
   {
      ArrayList<Boolean> reconstructedCommands = new ArrayList<Boolean>();

      @Override
      public void receivedPacket(PauseCommand packet)
      {
         reconstructedCommands.add(packet.isPaused());
      }

      public ArrayList<Boolean> getReconstructedCommands()
      {
         return reconstructedCommands;
      }
   }

   private class FootstepStatusConsumer implements PacketConsumer<FootstepStatus>
   {
      private final ArrayList<FootstepStatus> reconstructedFootstepStatuses = new ArrayList<FootstepStatus>();

      @Override
      public void receivedPacket(FootstepStatus footstepStatus)
      {
         reconstructedFootstepStatuses.add(footstepStatus);
      }

      public ArrayList<FootstepStatus> getReconstructedFootsteps()
      {
         return reconstructedFootstepStatuses;
      }
   }
}
