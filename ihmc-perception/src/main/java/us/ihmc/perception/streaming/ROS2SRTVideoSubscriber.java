package us.ihmc.perception.streaming;

import org.bytedeco.opencv.opencv_core.Mat;
import perception_msgs.msg.dds.SRTStreamMessage;
import us.ihmc.commons.Conversions;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.ros2.ROS2IOTopicPair;
import us.ihmc.communication.ros2.ROS2PublishSubscribeAPI;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.perception.camera.CameraIntrinsics;
import us.ihmc.ros2.ROS2Input;
import us.ihmc.tools.thread.MissingThreadTools;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static us.ihmc.perception.streaming.StreamingTools.CONNECTION_TIMEOUT;
import static us.ihmc.perception.streaming.StreamingTools.STATUS_MESSAGE_UUID;

public class ROS2SRTVideoSubscriber
{
   private static final double UPDATE_TIMEOUT = 0.25; // quarter of a second

   private final UUID subscriberId = UUID.randomUUID();

   private final ROS2PublishSubscribeAPI ros2;
   private final ROS2IOTopicPair<SRTStreamMessage> streamMessageTopicPair;
   private final ROS2Input<SRTStreamMessage> statusMessageSubscription;
   private final ROS2Input<SRTStreamMessage> ackMessageSubscription;
   private final Object ackMessageReceivedNotification = new Object();

   private final SRTStreamMessage requestMessage;
   private final SRTVideoReceiver videoReceiver;

   private Mat nextFrame;
   private final RigidBodyTransform sensorTransformToWorld = new RigidBodyTransform();
   private final CameraIntrinsics cameraIntrinsics;
   private float depthDiscretization = 0.0f;

   private final List<Consumer<Mat>> newFrameConsumers = new ArrayList<>();

   private final Thread subscriptionThread;
   private final AtomicBoolean subscriptionDesired = new AtomicBoolean(false);
   private volatile boolean shutDown = false;

   public ROS2SRTVideoSubscriber(ROS2PublishSubscribeAPI ros2,
                                 ROS2IOTopicPair<SRTStreamMessage> streamMessageTopicPair,
                                 InetSocketAddress inputAddress,
                                 int outputAVPixelFormat)
   {
      this.ros2 = ros2;
      this.streamMessageTopicPair = streamMessageTopicPair;

      statusMessageSubscription = ros2.subscribe(streamMessageTopicPair.getStatusTopic(),
                                                 statusMessage -> STATUS_MESSAGE_UUID.equals(MessageTools.toUUID(statusMessage.getId())));
      ackMessageSubscription = ros2.subscribe(streamMessageTopicPair.getStatusTopic(),
                                              ackMessage -> subscriberId.equals(MessageTools.toUUID(ackMessage.getId())));
      ackMessageSubscription.addCallback(this::receiveACKMessage);

      requestMessage = new SRTStreamMessage();
      requestMessage.setReceiverAddress(inputAddress.getHostString());
      requestMessage.setReceiverPort(inputAddress.getPort());
      MessageTools.toMessage(subscriberId, requestMessage.getId());

      videoReceiver = new SRTVideoReceiver(inputAddress, outputAVPixelFormat);
      cameraIntrinsics = new CameraIntrinsics();

      subscriptionThread = ThreadTools.startAThread(this::subscription, "ROS2SRTVideoSubscription");

      // This was added to solve a shutdown bug where FFMPEG would hang after receiving SIGINT, causing the destroy method to never be called.
      // Allowing the JVM to call the destroy method on SIGINT fixes the issue.
      Runtime.getRuntime().addShutdownHook(new Thread(this::destroy, getClass().getSimpleName() + "Destruction"));
   }

   public void addNewFrameConsumer(Consumer<Mat> newFrameConsumer)
   {
      newFrameConsumers.add(newFrameConsumer);
   }

   public void subscribe()
   {
      synchronized (subscriptionDesired)
      {
         subscriptionDesired.set(true);
         subscriptionDesired.notify();
      }
   }

   public void unsubscribe()
   {
      synchronized (subscriptionDesired)
      {
         subscriptionDesired.set(false);
         subscriptionThread.interrupt();
      }
   }

   public void destroy()
   {
      shutDown = true;
      unsubscribe();
      try
      {
         subscriptionThread.join();
      }
      catch (InterruptedException exception)
      {
         LogTools.error("Interrupted while waiting for {} thread to shut down.", subscriptionThread.getName());
      }
   }

   public boolean isConnected()
   {
      return videoReceiver.isConnected();
   }

   public boolean hasCameraIntrinsics()
   {
      return statusMessageSubscription.hasReceivedFirstMessage();
   }

   public RigidBodyTransformReadOnly getSensorTransformToWorld()
   {
      return sensorTransformToWorld;
   }

   public CameraIntrinsics getCameraIntrinsics()
   {
      return cameraIntrinsics;
   }

   public float getDepthDiscretization()
   {
      return depthDiscretization;
   }

   private void subscription()
   {
      // Maintain subscription while not shutting down
      while (!shutDown)
      {
         try
         {
            // If we don't want to be subscribed, but we're connected, then unsubscribe
            if (!subscriptionDesired.get() && videoReceiver.isConnected())
               disconnectFromStreamer();

            // If we don't want to be subscribed, wait until subscription is desired
            while (!subscriptionDesired.get() && !shutDown)
            {
               synchronized (subscriptionDesired)
               {
                  subscriptionDesired.wait();
               }
            }

            // If we want to be subscribed, but we're not connected, then try to connect
            if (subscriptionDesired.get() && !videoReceiver.isConnected() && !connectToStreamer())
               MissingThreadTools.sleep(UPDATE_TIMEOUT); // I believe this is interruptible, unlike ThreadTools.sleep()

            // If connected, get the next frame and give it to consumers
            if (subscriptionDesired.get() && videoReceiver.isConnected())
               update();
         }
         catch (InterruptedException ignored) {}
      }

      if (videoReceiver.isConnected())
         disconnectFromStreamer();

      statusMessageSubscription.destroy();
      ackMessageSubscription.destroy();
      videoReceiver.destroy();
      if (nextFrame != null)
         nextFrame.close();
   }

   private synchronized void disconnectFromStreamer()
   {
      requestMessage.setConnectionWanted(false);
      ros2.publish(streamMessageTopicPair.getCommandTopic(), requestMessage);
      videoReceiver.disconnect();
   }

   private boolean connectToStreamer() throws InterruptedException
   {
      // Send a request message
      requestMessage.setConnectionWanted(true);
      ros2.publish(streamMessageTopicPair.getCommandTopic(), requestMessage);

      // Wait for ACK or timeout
      synchronized (ackMessageReceivedNotification)
      {
         ackMessageReceivedNotification.wait((long) Conversions.secondsToMilliseconds(CONNECTION_TIMEOUT));
      }

      return videoReceiver.isConnected();
   }

   private void receiveACKMessage(SRTStreamMessage ackMessage)
   {
      if (!subscriptionDesired.get() || videoReceiver.isConnected())
         return;

      videoReceiver.waitForConnection(CONNECTION_TIMEOUT);

      // get camera intrinsics from ACK message
      cameraIntrinsics.setWidth(ackMessage.getImageWidth());
      cameraIntrinsics.setHeight(ackMessage.getImageHeight());
      cameraIntrinsics.setFx(ackMessage.getFx());
      cameraIntrinsics.setFy(ackMessage.getFy());
      cameraIntrinsics.setCx(ackMessage.getCx());
      cameraIntrinsics.setCy(ackMessage.getCy());
      depthDiscretization = ackMessage.getDepthDiscretization();

      synchronized (ackMessageReceivedNotification)
      {
         ackMessageReceivedNotification.notify();
      }
   }

   private synchronized void update()
   {
      // Get the next frame
      nextFrame = videoReceiver.getNextFrame(UPDATE_TIMEOUT);

      // Get updated sensor data
      if (statusMessageSubscription.hasReceivedFirstMessage())
      {
         SRTStreamMessage statusMessage = statusMessageSubscription.getLatest();
         sensorTransformToWorld.set(statusMessage.getOrientation(), statusMessage.getPosition());
      }

      // Give frame to consumers
      if (nextFrame != null)
      {
         newFrameConsumers.forEach(consumer -> consumer.accept(nextFrame));
         nextFrame.close();
      }
   }
}
