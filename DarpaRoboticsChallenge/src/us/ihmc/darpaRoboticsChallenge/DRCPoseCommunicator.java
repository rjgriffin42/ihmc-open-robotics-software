package us.ihmc.darpaRoboticsChallenge;

import javax.media.j3d.Transform3D;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.concurrent.Builder;
import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.messages.controller.RobotPoseData;
import us.ihmc.darpaRoboticsChallenge.networking.dataProducers.DRCJointConfigurationData;
import us.ihmc.darpaRoboticsChallenge.networking.dataProducers.JointConfigurationGatherer;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.TimestampProvider;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RawOutputWriter;

// fills a ring buffer with pose and joint data and in a worker thread passes it to the appropriate consumer 
public class DRCPoseCommunicator implements RawOutputWriter
{
   private final int WORKER_SLEEP_TIME_MILLIS = 1;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ReferenceFrame lidarFrame;
   private final ReferenceFrame cameraFrame;
   private final ReferenceFrame rootFrame;

   private final Transform3D rootTransform = new Transform3D();
   private final Transform3D cameraTransform = new Transform3D();
   private final Transform3D lidarTransform = new Transform3D();

   private final ObjectCommunicator networkProcessorCommunicator;
   private final JointConfigurationGatherer jointConfigurationGathererAndProducer;
   private final TimestampProvider timeProvider;

   private final ConcurrentRingBuffer<State> stateRingBuffer;

   public DRCPoseCommunicator(SDFFullRobotModel estimatorModel, JointConfigurationGatherer jointConfigurationGathererAndProducer,
         DRCRobotJointMap jointMap, ObjectCommunicator networkProcessorCommunicator, TimestampProvider timestampProvider)
   {
      this.networkProcessorCommunicator = networkProcessorCommunicator;
      this.jointConfigurationGathererAndProducer = jointConfigurationGathererAndProducer;
      this.timeProvider = timestampProvider;

      lidarFrame = estimatorModel.getLidarBaseFrame(jointMap.getLidarSensorName());
      cameraFrame = estimatorModel.getCameraFrame(jointMap.getLeftCameraName());
      rootFrame = estimatorModel.getRootJoint().getFrameAfterJoint();

      stateRingBuffer = new ConcurrentRingBuffer<State>(State.builder, 8);

      startWriterThread();
   }

   // this thread reads from the stateRingBuffer and pushes the data out to the objectConsumer
   private void startWriterThread()
   {
      new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            while (true)
            {
               if (stateRingBuffer.poll())
               {
                  State state;
                  while ((state = stateRingBuffer.read()) != null)
                  {
                     if(networkProcessorCommunicator == null)
                     {
                        System.out.println("Net Proc Comm");
                     }
                     if(state.poseData == null)
                     {
                        System.out.println("Pose Data");
                     }
                     networkProcessorCommunicator.consumeObject(state.poseData);
                     networkProcessorCommunicator.consumeObject(state.jointData);
                  }
                  stateRingBuffer.flush();
               }

               ThreadTools.sleep(WORKER_SLEEP_TIME_MILLIS);
            }
         }
      }).start();
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

   // puts the state data into the ring buffer for the output thread
   @Override
   public void write()
   {
      rootFrame.getTransformToDesiredFrame(rootTransform, ReferenceFrame.getWorldFrame());
      cameraFrame.getTransformToDesiredFrame(cameraTransform, ReferenceFrame.getWorldFrame());
      lidarFrame.getTransformToDesiredFrame(lidarTransform, ReferenceFrame.getWorldFrame());

      State state = stateRingBuffer.next();
      if (state == null)
      {
         return;
      }
      
      long timestamp = timeProvider.getTimestamp();
      jointConfigurationGathererAndProducer.packEstimatorJoints(timestamp, state.jointData);
      state.poseData.setAll(timestamp, rootTransform, cameraTransform, lidarTransform);

      stateRingBuffer.commit();
   }

   //this object is just a glorified tuple
   private static class State
   {
      public final RobotPoseData poseData;
      public final DRCJointConfigurationData jointData;

      public static final Builder<State> builder = new Builder<State>()
      {
         @Override
         public State newInstance()
         {
            return new State();
         }
      };

      public State()
      {
         poseData = new RobotPoseData();
         jointData = new DRCJointConfigurationData();
      }
   }
}