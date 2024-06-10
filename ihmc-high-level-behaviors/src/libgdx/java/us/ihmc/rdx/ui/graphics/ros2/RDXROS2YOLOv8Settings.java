package us.ihmc.rdx.ui.graphics.ros2;

import imgui.ImGui;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import perception_msgs.msg.dds.YOLOv8ParametersMessage;
import us.ihmc.commons.thread.Notification;
import us.ihmc.communication.PerceptionAPI;
import us.ihmc.communication.ros2.ROS2Heartbeat;
import us.ihmc.communication.ros2.ROS2PublishSubscribeAPI;
import us.ihmc.perception.YOLOv8.YOLOv8DetectionClass;
import us.ihmc.rdx.imgui.ImGuiTools;
import us.ihmc.rdx.ui.graphics.RDXVisualizer;
import us.ihmc.tools.thread.Throttler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RDXROS2YOLOv8Settings extends RDXVisualizer
{
   private static final String[] AVAILABLE_SENSORS = {"ZED", "D455"};
   private static final double MESSAGE_PUBLISH_PERIOD = 2; // publish messages every 2 seconds

   private final ROS2PublishSubscribeAPI ros2;

   /**
    * The mask erosion kernel radius determines how much the YOLO mask is eroded,
    * i.e. how much it is shrunk.
    * A larger radius will result is more shrinkage, and a radius of 0 will not shrink the mask.
    * Erosion is useful when the YOLO mask is slightly larger than the object it detects.
    */
   private final ImInt maskErosionKernelRadius = new ImInt(2);
   private final ImFloat pointCloudOutlierRejectionThreshold = new ImFloat(0.2f);
   private final ImFloat confidenceThreshold = new ImFloat(0.3f);
   private final ImFloat nmsThreshold = new ImFloat(0.1f);
   private final ImFloat maskThreshold = new ImFloat(0.0f);
   private final ImFloat candidateAcceptanceThreshold = new ImFloat(0.6f);
   private final ImFloat matchDistanceThreshold = new ImFloat(0.2f);
   private final ImInt selectedSensor = new ImInt(0); // 0 = ZED, 1 = Realsense

   private final Set<YOLOv8DetectionClass> targetDetections = new HashSet<>();

   private final ROS2Heartbeat demandYOLOv8ICPZed;
   private final ROS2Heartbeat demandYOLOv8ICPRealsense;

   private final Throttler messagePublishThrottler = new Throttler().setPeriod(MESSAGE_PUBLISH_PERIOD);
   private final Notification parametersChanged = new Notification();

   public RDXROS2YOLOv8Settings(String title, ROS2PublishSubscribeAPI ros2)
   {
      super(title);

      this.ros2 = ros2;

      demandYOLOv8ICPZed = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_YOLO_ZED);
      demandYOLOv8ICPRealsense = new ROS2Heartbeat(ros2, PerceptionAPI.REQUEST_YOLO_REALSENSE);

      // Select all target detections at beginning
      targetDetections.addAll(Arrays.asList(YOLOv8DetectionClass.values()));
   }

   @Override
   public void renderImGuiWidgets()
   {
      super.renderImGuiWidgets();

      ImGui.combo("Sensor Selection", selectedSensor, AVAILABLE_SENSORS);

      demandYOLOv8ICPZed.setAlive(isActive() && selectedSensor.get() == 0);
      demandYOLOv8ICPRealsense.setAlive(isActive() && selectedSensor.get() == 1);

      if (ImGui.sliderInt("maskErosionKernelRadius", maskErosionKernelRadius.getData(), 0, 10))
         parametersChanged.set();
      if (ImGui.sliderFloat("outlierPointRejectionThreshold", pointCloudOutlierRejectionThreshold.getData(), 0.0f, 5.0f))
         parametersChanged.set();
      if (ImGui.sliderFloat("confidenceThreshold", confidenceThreshold.getData(), 0.0f, 1.0f))
         parametersChanged.set();
      if (ImGui.sliderFloat("nmsThreshold", nmsThreshold.getData(), 0.0f, 1.0f))
         parametersChanged.set();
      if (ImGui.sliderFloat("maskThreshold", maskThreshold.getData(), -1.0f, 1.0f))
         parametersChanged.set();
      if (ImGui.sliderFloat("candidateAcceptanceThreshold", candidateAcceptanceThreshold.getData(), 0.0f, 1.0f))
         parametersChanged.set();
      if (ImGui.sliderFloat("matchDistanceThreshold", matchDistanceThreshold.getData(), 0.0f, 5.0f))
         parametersChanged.set();

      if (ImGui.collapsingHeader("Target Detection Classes"))
      {
         if (ImGui.button("Select All"))
         {
            targetDetections.addAll(Arrays.asList(YOLOv8DetectionClass.values()));
            parametersChanged.set();
         }
         ImGui.sameLine();
         if (ImGui.button("Unselect All"))
         {
            targetDetections.clear();
            parametersChanged.set();
         }

         for (YOLOv8DetectionClass detectionClass : YOLOv8DetectionClass.values())
         {
            if (ImGuiTools.smallCheckbox(detectionClass.toString(), targetDetections.contains(detectionClass)))
            {
               if (targetDetections.contains(detectionClass))
                  targetDetections.remove(detectionClass);
               else
                  targetDetections.add(detectionClass);

               parametersChanged.set();
            }
         }
      }
   }

   @Override
   public void update()
   {
      super.update();

      // Publish a settings message if user changed a setting or enough time has passed since last publication
      if (parametersChanged.poll() || messagePublishThrottler.run())
      {
         YOLOv8ParametersMessage message = new YOLOv8ParametersMessage();
         message.setErosionKernelRadius(maskErosionKernelRadius.get());
         message.setOutlierRejectionThreshold(pointCloudOutlierRejectionThreshold.get());
         message.setConfidenceThreshold(confidenceThreshold.get());
         message.setNonMaximumSuppressionThreshold(nmsThreshold.get());
         message.setSegmentationThreshold(maskThreshold.get());
         message.setCandidateAcceptanceThreshold(candidateAcceptanceThreshold.get());
         message.setMatchDistanceThreshold(matchDistanceThreshold.get());

         message.getTargetDetectionClasses().clear();
         for (YOLOv8DetectionClass targetDetection : targetDetections)
            message.getTargetDetectionClasses().add(targetDetection.toByte());

         ros2.publish(PerceptionAPI.YOLO_PARAMETERS, message);
      }
   }

   @Override
   public void destroy()
   {
      super.destroy();

      demandYOLOv8ICPZed.destroy();
      demandYOLOv8ICPRealsense.destroy();
   }
}