package us.ihmc.perception;

import org.bytedeco.opencv.opencv_core.Mat;
import perception_msgs.msg.dds.ArUcoMarkerPoses;
import us.ihmc.communication.ros2.ROS2PublishSubscribeAPI;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.perception.scene.ArUcoDetectableObject;
import us.ihmc.perception.scene.PredefinedSceneObjectLibrary;

import java.util.HashMap;

/**
 * Used to publish detected ArUco marker poses over ROS 2.
 */
public class OpenCVArUcoMarkerROS2Publisher
{
   private final OpenCVArUcoMarkerDetection arUcoMarkerDetection;
   private final HashMap<Integer, ArUcoDetectableObject> arUcoDetectableObjects = new HashMap<>();
   private final FramePose3D framePoseOfMarker = new FramePose3D();
   private final ArUcoMarkerPoses arUcoMarkerPoses = new ArUcoMarkerPoses();
   private final ReferenceFrame cameraFrame;
   private final ROS2PublishSubscribeAPI ros2;

   public OpenCVArUcoMarkerROS2Publisher(OpenCVArUcoMarkerDetection arUcoMarkerDetection,
                                         PredefinedSceneObjectLibrary predefinedSceneObjectLibrary,
                                         ReferenceFrame cameraFrame,
                                         ROS2PublishSubscribeAPI ros2)
   {
      this.arUcoMarkerDetection = arUcoMarkerDetection;
      this.cameraFrame = cameraFrame;
      this.ros2 = ros2;

      for (ArUcoDetectableObject arUcoDetectableObject : predefinedSceneObjectLibrary.getArUcoDetectableObjects())
      {
         this.arUcoDetectableObjects.put(arUcoDetectableObject.getMarkerID(), arUcoDetectableObject);
      }
   }

   public void update()
   {
      if(arUcoMarkerDetection.isEnabled())
      {
         synchronized (arUcoMarkerDetection.getSyncObject())
         {
            Mat ids = arUcoMarkerDetection.getIds();
            arUcoMarkerPoses.getMarkerId().clear();
            arUcoMarkerPoses.getOrientation().clear();
            arUcoMarkerPoses.getPosition().clear();
            // Iterat
            for (int i = 0; i < ids.rows(); i++)
            {
               int markerID = ids.ptr(i, 0).getInt();
               ArUcoDetectableObject detectedObject = arUcoDetectableObjects.get(markerID);

               if (detectedObject != null)
               {
                  arUcoMarkerDetection.getPose(detectedObject);

                  framePoseOfMarker.setIncludingFrame(cameraFrame, arUcoMarkerDetection.getPose(detectedObject));
                  framePoseOfMarker.changeFrame(ReferenceFrame.getWorldFrame());

                  arUcoMarkerPoses.getMarkerId().add(markerID);
                  arUcoMarkerPoses.getOrientation().add().set(framePoseOfMarker.getOrientation());
                  arUcoMarkerPoses.getPosition().add().set(framePoseOfMarker.getX(), framePoseOfMarker.getY(), framePoseOfMarker.getZ());
               }
            }
            ros2.publish(ROS2Tools.ARUCO_MARKER_POSES, arUcoMarkerPoses);
         }
      }
   }
}
