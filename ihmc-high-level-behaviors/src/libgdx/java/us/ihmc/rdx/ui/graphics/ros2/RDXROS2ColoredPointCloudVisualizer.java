package us.ihmc.rdx.ui.graphics.ros2;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import imgui.internal.ImGui;
import imgui.type.ImBoolean;
import org.bytedeco.opencl._cl_kernel;
import org.bytedeco.opencl._cl_program;
import perception_msgs.msg.dds.ImageMessage;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.log.LogTools;
import us.ihmc.perception.OpenCLFloatBuffer;
import us.ihmc.perception.OpenCLManager;
import us.ihmc.perception.opencl.OpenCLBooleanParameter;
import us.ihmc.perception.opencl.OpenCLFloatParameters;
import us.ihmc.perception.opencl.OpenCLRigidBodyTransformParameter;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.rdx.RDXPointCloudRenderer;
import us.ihmc.rdx.imgui.ImGuiTools;
import us.ihmc.rdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.rdx.ui.visualizers.RDXVisualizer;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.ros2.RealtimeROS2Node;
import us.ihmc.tools.string.StringTools;

public class RDXROS2ColoredPointCloudVisualizer extends RDXVisualizer implements RenderableProvider
{
   private final float FOCAL_LENGTH_COLOR = 0.00254f;
   private final float CMOS_WIDTH_COLOR = 0.0036894f;
   private final float CMOS_HEIGHT_COLOR = 0.0020753f;
   private static final float depthToMetersScalar = 2.500000118743628E-4f;
   private final static boolean sinusoidalPatternEnabled = false;

   private final RDXROS2ColoredPointCloudVisualizerPinholeDepthChannel depthChannel;
   private final RDXROS2ColoredPointCloudVisualizerPinholeColorChannel colorChannel;

   private final RDXPointCloudRenderer pointCloudRenderer = new RDXPointCloudRenderer();

   private final Object imageMessagesSyncObject = new Object();

   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final ImBoolean subscribed = new ImBoolean(false);
   private final String titleBeforeAdditions;
   private final PubSubImplementation pubSubImplementation;
   private RealtimeROS2Node realtimeROS2Node;

   private OpenCLManager openCLManager;
   private OpenCLFloatBuffer finalColoredDepthBuffer;
   private _cl_program openCLProgram;
   private _cl_kernel createPointCloudKernel;

   private final OpenCLFloatParameters parametersBuffer = new OpenCLFloatParameters();
   private final OpenCLBooleanParameter sinusoidalPatternEnabledParameter = new OpenCLBooleanParameter();
   private final OpenCLRigidBodyTransformParameter sensorTransformToWorldParameter = new OpenCLRigidBodyTransformParameter();

   public RDXROS2ColoredPointCloudVisualizer(String title,
                                             PubSubImplementation pubSubImplementation,
                                             ROS2Topic<ImageMessage> depthTopic,
                                             ROS2Topic<ImageMessage> colorTopic)
   {
      super(title + " (ROS 2)");
      titleBeforeAdditions = title;
      this.pubSubImplementation = pubSubImplementation;
      depthChannel = new RDXROS2ColoredPointCloudVisualizerPinholeDepthChannel(depthTopic);
      colorChannel = new RDXROS2ColoredPointCloudVisualizerPinholeColorChannel(colorTopic);
   }

   private void subscribe()
   {
      subscribed.set(true);
      this.realtimeROS2Node = ROS2Tools.createRealtimeROS2Node(pubSubImplementation, StringTools.titleToSnakeCase(titleBeforeAdditions));
      depthChannel.subscribe(realtimeROS2Node, imageMessagesSyncObject);
      colorChannel.subscribe(realtimeROS2Node, imageMessagesSyncObject);
      realtimeROS2Node.spin();
   }

   @Override
   public void create()
   {
      super.create();

      openCLManager = new OpenCLManager();
      openCLProgram = openCLManager.loadProgram("PinholePinholeColoredPointCloudVisualizer", "PerceptionCommon.cl");
      createPointCloudKernel = openCLManager.createKernel(openCLProgram, "createPointCloud");
   }

   @Override
   public void update()
   {
      super.update();

      if (subscribed.get() && isActive() && depthChannel.getImageAvailable() && colorChannel.getImageAvailable())
      {
         synchronized (imageMessagesSyncObject)
         {
            depthChannel.update(openCLManager);
            colorChannel.update(openCLManager);

            if (finalColoredDepthBuffer == null)
            {
               int totalNumberOfPoints = depthChannel.getTotalNumberOfPixels();
               // Allocates memory for vertex buffer for the point cloud
               LogTools.info("Allocated new buffers. {} total points", totalNumberOfPoints);
               pointCloudRenderer.create(totalNumberOfPoints);

               finalColoredDepthBuffer = new OpenCLFloatBuffer(totalNumberOfPoints * RDXPointCloudRenderer.FLOATS_PER_VERTEX,
                                                               pointCloudRenderer.getVertexBuffer());
               finalColoredDepthBuffer.createOpenCLBufferObject(openCLManager);
            }

            sensorTransformToWorldParameter.setTranslation(depthChannel.getTranslationToWorld());
            sensorTransformToWorldParameter.setRotationMatrix(depthChannel.getRotationMatrixToWorld());
         }

         // If both depth and color images are available, configure the OpenCL kernel and run it, to generate the point cloud float buffer.
         parametersBuffer.setParameter(FOCAL_LENGTH_COLOR);
         parametersBuffer.setParameter(CMOS_WIDTH_COLOR);
         parametersBuffer.setParameter(CMOS_HEIGHT_COLOR);
         parametersBuffer.setParameter(depthChannel.getCx());
         parametersBuffer.setParameter(depthChannel.getCy());
         parametersBuffer.setParameter(depthChannel.getFx());
         parametersBuffer.setParameter(depthChannel.getFy());
         parametersBuffer.setParameter((float) depthChannel.getImageWidth());
         parametersBuffer.setParameter((float) depthChannel.getImageHeight());
         parametersBuffer.setParameter((float) colorChannel.getImageWidth());
         parametersBuffer.setParameter((float) colorChannel.getImageHeight());
         parametersBuffer.setParameter(depthToMetersScalar);
         sinusoidalPatternEnabledParameter.setParameter(sinusoidalPatternEnabled);

         // Upload the buffers to the OpenCL device (GPU)
         depthChannel.getDepth16UC1Image().writeOpenCLImage(openCLManager);
         colorChannel.getColor8UC4Image().writeOpenCLImage(openCLManager);
         parametersBuffer.writeOpenCLBufferObject(openCLManager);
         sensorTransformToWorldParameter.writeOpenCLBufferObject(openCLManager);

         // Set the OpenCL kernel arguments
         openCLManager.setKernelArgument(createPointCloudKernel, 0, depthChannel.getDepth16UC1Image().getOpenCLImageObject());
         openCLManager.setKernelArgument(createPointCloudKernel, 1, colorChannel.getColor8UC4Image().getOpenCLImageObject());
         openCLManager.setKernelArgument(createPointCloudKernel, 2, finalColoredDepthBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(createPointCloudKernel, 3, parametersBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(createPointCloudKernel, 4, sinusoidalPatternEnabledParameter);
         openCLManager.setKernelArgument(createPointCloudKernel, 5, sensorTransformToWorldParameter.getOpenCLBufferObject());

         // Run the OpenCL kernel
         openCLManager.execute2D(createPointCloudKernel, depthChannel.getImageWidth(), depthChannel.getImageHeight());

         // Read the OpenCL buffer back to the CPU
         finalColoredDepthBuffer.readOpenCLBufferObject(openCLManager);

         // Request the PointCloudRenderer to render the point cloud from OpenCL-mapped buffers
         pointCloudRenderer.updateMeshFastest(depthChannel.getTotalNumberOfPixels());
      }
   }

   @Override
   public void renderImGuiWidgets()
   {
      if (ImGui.checkbox(labels.getHidden(getTitle() + "Subscribed"), subscribed))
      {
         setSubscribed(subscribed.get());
      }
      ImGuiTools.previousWidgetTooltip("Subscribed");
      ImGui.sameLine();
      super.renderImGuiWidgets();
      ImGui.text(colorChannel.getTopic().getName());
      if (colorChannel.getReceivedOne())
      {
         colorChannel.getMessageSizeReadout().renderImGuiWidgets();
      }
      ImGui.text(depthChannel.getTopic().getName());
      if (depthChannel.getReceivedOne())
      {
         depthChannel.getMessageSizeReadout().renderImGuiWidgets();
      }
      if (colorChannel.getReceivedOne())
         colorChannel.getFrequencyPlot().renderImGuiWidgets();
      if (depthChannel.getReceivedOne())
         depthChannel.getFrequencyPlot().renderImGuiWidgets();

      if (colorChannel.getReceivedOne())
         colorChannel.getDelayPlot().renderImGuiWidgets();
      if (depthChannel.getReceivedOne())
         depthChannel.getDelayPlot().renderImGuiWidgets();

      if (colorChannel.getReceivedOne())
         colorChannel.getSequenceDiscontinuityPlot().renderImGuiWidgets();
      if (depthChannel.getReceivedOne())
         depthChannel.getSequenceDiscontinuityPlot().renderImGuiWidgets();
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      if (isActive())
         pointCloudRenderer.getRenderables(renderables, pool);
   }

   @Override
   public void destroy()
   {
      unsubscribe();
      super.destroy();
   }

   public void setSubscribed(boolean subscribed)
   {
      if (subscribed && realtimeROS2Node == null)
      {
         subscribe();
      }
      else if (!subscribed && realtimeROS2Node == null)
      {
         unsubscribe();
      }
   }

   private void unsubscribe()
   {
      subscribed.set(false);
      if (realtimeROS2Node != null)
      {
         realtimeROS2Node.destroy();
         realtimeROS2Node = null;
      }
   }

   public boolean isSubscribed()
   {
      return subscribed.get();
   }
}