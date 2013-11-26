package us.ihmc.darpaRoboticsChallenge.handControl.packetsAndConsumers;

import us.ihmc.concurrent.Builder;
import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.iRobotHandControl.IRobotHandSensorData;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.TimestampProvider;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RawOutputWriter;

// fills a ring buffer with pose and joint data and in a worker thread passes it to the appropriate consumer 
public class HandJointAngleCommunicator implements RawOutputWriter
{
   private final int WORKER_SLEEP_TIME_MILLIS = 250;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final ObjectCommunicator networkProcessorCommunicator;
   private final ConcurrentRingBuffer<HandJointAnglePacket> packetRingBuffer;
   private final double[][] fingers = new double[3][];
   private final RobotSide side;
   private HandJointAnglePacket currentPacket;

   public HandJointAngleCommunicator(RobotSide side, ObjectCommunicator networkProcessorCommunicator)
   {
      this.side = side;
      this.networkProcessorCommunicator = networkProcessorCommunicator;
      packetRingBuffer = new ConcurrentRingBuffer<HandJointAnglePacket>(HandJointAngleCommunicator.builder, 8);
      startWriterThread();
   }

   // this thread reads from the stateRingBuffer and pushes the data out to the objectConsumer
   private void startWriterThread()
   {
      AsyncContinuousExecutor.executeContinuously(new Runnable()
      {
         @Override
         public void run()
         {
            if (packetRingBuffer.poll())
            {
               while ((currentPacket = packetRingBuffer.read()) != null)
               {
                  if (networkProcessorCommunicator == null)
                  {
                     System.out.println("Net Proc Comm");
                  }
                  networkProcessorCommunicator.consumeObject(currentPacket);
               }
               packetRingBuffer.flush();
            }
         }
      }, WORKER_SLEEP_TIME_MILLIS, "Hand Joint Angle Communicator");
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return getClass().getSimpleName();
   }

   @Override
   public String getDescription()
   {
      return getName();
   }

   public void updateHandAngles(IRobotHandSensorData sensorDataFromHand)
   {
      fingers[0] = sensorDataFromHand.getIndexJointAngles();
      fingers[1] = sensorDataFromHand.getMiddleJointAngles();
      fingers[2] = sensorDataFromHand.getThumbJointAngles();
   }

   // puts the state data into the ring buffer for the output thread
   @Override
   public void write()
   {
      HandJointAnglePacket packet = packetRingBuffer.next();
      if (packet == null)
      {
         return;
      }
      packet.setAll(side, fingers[0], fingers[1], fingers[2]);
      packetRingBuffer.commit();
   }

   public static final Builder<HandJointAnglePacket> builder = new Builder<HandJointAnglePacket>()
   {
      @Override
      public HandJointAnglePacket newInstance()
      {
         return new HandJointAnglePacket();
      }
   };
}