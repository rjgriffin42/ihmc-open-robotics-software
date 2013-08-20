package us.ihmc.utilities.ros;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceClient;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

public class RosMainNode implements NodeMain
{
   private final LinkedHashMap<String, RosTopicSubscriberInterface<? extends Message>> subscribers = new LinkedHashMap<String, RosTopicSubscriberInterface<? extends Message>>();
   private final LinkedHashMap<String, RosTopicPublisher<? extends Message>> publishers = new LinkedHashMap<String, RosTopicPublisher<? extends Message>>();
   private final LinkedHashMap<String, RosServiceClient<? extends Message, ? extends Message>> clients = new LinkedHashMap<String, RosServiceClient<? extends Message, ? extends Message>>();
   
   private final URI masterURI;
   private boolean isStarted = false;
   
   private final String graphName;
   
   public RosMainNode(URI masterURI, String graphName)
   {
      this.masterURI = masterURI;
      this.graphName = graphName;
   }
   
   public void attachServiceClient(String topicName, RosServiceClient<? extends Message, ? extends Message> client)
   {
      checkNotStarted();
      
      clients.put(topicName, client);
   }
   
   public void attachPublisher(String topicName, RosTopicPublisher<? extends Message> publisher)
   {
      checkNotStarted();
      
      publishers.put(topicName, publisher);
      
   }
   
   public void attachSubscriber(String topicName, RosTopicSubscriberInterface<? extends Message> subscriber)
   {
      checkNotStarted();
      
      subscribers.put(topicName, subscriber);
   }

   private void checkNotStarted()
   {
      if(isStarted)
      {
         throw new RuntimeException("Cannot attach new subscribers or publishers after execution started");
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void onStart(ConnectedNode connectedNode)
   {
      isStarted = true;
      
      for(Entry<String, RosTopicSubscriberInterface<? extends Message>> entry : subscribers.entrySet())
      {
         final RosTopicSubscriberInterface rosTopicSubscriber = entry.getValue();
         Subscriber<? extends Message> subscriber = connectedNode.newSubscriber(entry.getKey(), rosTopicSubscriber.getMessageType());
         subscriber.addMessageListener(rosTopicSubscriber);
         rosTopicSubscriber.connected();
      }
      
      
      for(Entry<String, RosTopicPublisher<? extends Message>> entry : publishers.entrySet())
      {
         final RosTopicPublisher rosTopicPublisher = entry.getValue();
         Publisher<? extends Message> publisher = connectedNode.newPublisher(entry.getKey(), rosTopicPublisher.getMessageType());
         rosTopicPublisher.setPublisher(publisher);
         rosTopicPublisher.connected();
      }
      
      for(Entry<String, RosServiceClient<? extends Message, ? extends Message>> entry : clients.entrySet())
      {
         final RosServiceClient<? extends Message,? extends Message> rosServiceClient = entry.getValue();
         try
         {
            ServiceClient client = connectedNode.newServiceClient(entry.getKey(), rosServiceClient.getRequestType());
            rosServiceClient.setServiceClient(client);
         }
         catch (ServiceNotFoundException e)
         {
            throw new RuntimeException(e);
         }
      }
   }

   public void onShutdown(Node node)
   {
   }

   public void onShutdownComplete(Node node)
   {
   }

   public void onError(Node node, Throwable throwable)
   {
   }

   public final GraphName getDefaultNodeName()
   {
      return GraphName.of(graphName);
   }
   
   public void execute()
   {
      NodeConfiguration nodeConfiguration = RosTools.createNodeConfiguration(masterURI);
      NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
      nodeMainExecutor.execute(this, nodeConfiguration);
   }

}
