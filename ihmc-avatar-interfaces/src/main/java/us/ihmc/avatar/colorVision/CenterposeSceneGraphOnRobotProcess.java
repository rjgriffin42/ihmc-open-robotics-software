package us.ihmc.avatar.colorVision;

import perception_msgs.msg.dds.DetectedObjectPacket;
import us.ihmc.communication.IHMCROS2Input;
import us.ihmc.communication.PerceptionAPI;
import us.ihmc.communication.ros2.ROS2Helper;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.perception.filters.TimeBasedDetectionFilter;
import us.ihmc.perception.sceneGraph.SceneNode;
import us.ihmc.perception.sceneGraph.centerpose.CenterposeNode;
import us.ihmc.perception.sceneGraph.modification.SceneGraphNodeAddition;
import us.ihmc.perception.sceneGraph.ros2.ROS2SceneGraph;
import us.ihmc.ros2.ROS2Topic;

import java.util.ArrayList;
import java.util.List;

public class CenterposeSceneGraphOnRobotProcess
{
   private final IHMCROS2Input<DetectedObjectPacket> subscriber;
   private final RigidBodyTransform sensorInZedTransform = new RigidBodyTransform(); // TODO: This is specific to ZED, remove or move somewhere else?

   public CenterposeSceneGraphOnRobotProcess(ROS2Helper ros2Helper)
   {
      ROS2Topic<DetectedObjectPacket> topicName = PerceptionAPI.CENTERPOSE_DETECTED_OBJECT;
      subscriber = ros2Helper.subscribe(topicName);

      sensorInZedTransform.getTranslation().set(0.0, 0.06, 0.0);
      sensorInZedTransform.getRotation().setEuler(0.0, Math.toRadians(90.0), Math.toRadians(180.0));
   }

   public void update(ROS2SceneGraph onRobotSceneGraph, ReferenceFrame sensorFrame)
   {
      onRobotSceneGraph.updateSubscription();
      updateSceneGraph(onRobotSceneGraph, sensorFrame);
      onRobotSceneGraph.updateOnRobotOnly(sensorFrame);
      onRobotSceneGraph.updatePublication();
   }

   public void updateSceneGraph(ROS2SceneGraph sceneGraph, ReferenceFrame cameraFrame)
   {
      // Handle a new DetectedObjectPacket message
      if (subscriber.getMessageNotification().poll())
      {
         ReferenceFrame sensorFrame = ReferenceFrameTools.constructFrameWithChangingTransformToParent("SensorFrame", cameraFrame, sensorInZedTransform);

         DetectedObjectPacket detectedObjectPacket = subscriber.getMessageNotification().read();

         // Update or add the corresponding CenterposeSceneNode
         sceneGraph.modifyTree(modificationQueue ->
         {
          final CenterposeNode centerposeNode;

            Point3D[] vertices = detectedObjectPacket.getBoundingBoxVertices();
            for (Point3D vertex : vertices)
            {
               FramePoint3D frameVertex = new FramePoint3D();
               frameVertex.setIncludingFrame(sensorFrame, vertex);
               frameVertex.changeFrame(ReferenceFrame.getWorldFrame());
               vertex.set(frameVertex);
            }

            // Update
            if (sceneGraph.getCenterposeDetectedMarkerIDToNodeMap().containsKey(detectedObjectPacket.getId()))
            {
               centerposeNode = sceneGraph.getCenterposeDetectedMarkerIDToNodeMap().get(detectedObjectPacket.getId());

               centerposeNode.setVertices3D(vertices);
               centerposeNode.setObjectType(detectedObjectPacket.getObjectTypeAsString());
               centerposeNode.setConfidence(detectedObjectPacket.getConfidence());
            }
            // Add
            else
            {
               centerposeNode = new CenterposeNode(sceneGraph.getNextID().getAndIncrement(),
                                                   "CenterposeDetectedObject%d".formatted(detectedObjectPacket.getId()),
                                                   detectedObjectPacket.getId(),
                                                   vertices,
                                                   detectedObjectPacket.getBoundingBox2dVertices());

               modificationQueue.accept(new SceneGraphNodeAddition(centerposeNode, sceneGraph.getRootNode()));
               sceneGraph.getCenterposeDetectedMarkerIDToNodeMap().put(centerposeNode.getObjectID(), centerposeNode);
               sceneGraph.getCenterposeNodeDetectionFilters().put(centerposeNode.getObjectID(), new TimeBasedDetectionFilter(1.0f, 2));
            }

            sceneGraph.getCenterposeNodeDetectionFilters().get(centerposeNode.getObjectID()).registerDetection();
         });
      }

      List<CenterposeNode> removals = new ArrayList<>();

      for (CenterposeNode centerposeNode : sceneGraph.getCenterposeDetectedMarkerIDToNodeMap().valueCollection())
      {
         boolean found = false;

         // TODO: fix this state issue
         for (SceneNode rootChild : sceneGraph.getRootNode().getChildren())
            if (rootChild instanceof CenterposeNode centerposeRootChild)
               if (centerposeRootChild.getObjectID() == centerposeNode.getObjectID())
                  found = true;

         if (found)
         {
            centerposeNode.update();
            TimeBasedDetectionFilter detectionFilter = sceneGraph.getCenterposeNodeDetectionFilters().get(centerposeNode.getObjectID());
            detectionFilter.update();
            centerposeNode.setCurrentlyDetected(detectionFilter.isDetected());
         }
         else
         {
            removals.add(centerposeNode);
         }
      }

      // TODO: fix this state issue
      for (CenterposeNode centerposeNode : removals)
      {
         sceneGraph.getCenterposeDetectedMarkerIDToNodeMap().remove(centerposeNode.getObjectID());
         sceneGraph.getCenterposeNodeDetectionFilters().remove(centerposeNode.getObjectID());
      }
   }

   public void destroy()
   {
      subscriber.destroy();
   }
}
