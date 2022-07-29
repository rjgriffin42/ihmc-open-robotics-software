package us.ihmc.gdx.ui.graphics.live;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import controller_msgs.msg.dds.FusedSensorHeadPointCloudMessage;
import controller_msgs.msg.dds.LidarScanMessage;
import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import imgui.internal.ImGui;
import imgui.type.ImFloat;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import us.ihmc.communication.IHMCROS2Callback;
import us.ihmc.communication.packets.LidarPointCloudCompression;
import us.ihmc.communication.packets.StereoPointCloudCompression;
import us.ihmc.gdx.GDXPointCloudRenderer;
import us.ihmc.gdx.imgui.ImGuiPlot;
import us.ihmc.gdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.gdx.ui.visualizers.ImGuiFrequencyPlot;
import us.ihmc.gdx.ui.visualizers.ImGuiGDXVisualizer;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.ros2.ROS2QosProfile;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.tools.thread.MissingThreadTools;
import us.ihmc.tools.thread.ResettableExceptionHandlingExecutorService;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;

public class GDXROS2PointCloudVisualizer extends ImGuiGDXVisualizer implements RenderableProvider
{
   private final ROS2Node ros2Node;
   private final ROS2Topic<?> topic;
   private final ImGuiFrequencyPlot frequencyPlot = new ImGuiFrequencyPlot();
   private final ImGuiPlot segmentIndexPlot = new ImGuiPlot("Segment", 1000, 230, 20);
   private final ImFloat pointSize = new ImFloat(0.01f);
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final GDXPointCloudRenderer pointCloudRenderer = new GDXPointCloudRenderer();
   private final int pointsPerSegment;
   private final int numberOfSegments;
   private final ResettableExceptionHandlingExecutorService threadQueue;
   private final LZ4FastDecompressor lz4Decompressor = LZ4Factory.nativeInstance().fastDecompressor();
   private final ByteBuffer decompressionInputDirectBuffer;
   private final ByteBuffer decompressionOutputDirectBuffer;
   private final int inputBytesPerPoint = 4 * Integer.BYTES;
   private final AtomicReference<FusedSensorHeadPointCloudMessage> latestFusedSensorHeadPointCloudMessageReference = new AtomicReference<>(null);
   private final AtomicReference<LidarScanMessage> latestLidarScanMessageReference = new AtomicReference<>(null);
   private final AtomicReference<StereoVisionPointCloudMessage> latestStereoVisionMessageReference = new AtomicReference<>(null);
   private final Color color = new Color();
   private int latestSegmentIndex = -1;

   public GDXROS2PointCloudVisualizer(String title, ROS2Node ros2Node, ROS2Topic<?> topic)
   {
      this(title, ros2Node, topic, 500000, 1);
   }

   public GDXROS2PointCloudVisualizer(String title, ROS2Node ros2Node, ROS2Topic<?> topic, int pointsPerSegment, int numberOfSegments)
   {
      super(title);
      this.ros2Node = ros2Node;
      this.topic = topic;
      this.pointsPerSegment = pointsPerSegment;
      this.numberOfSegments = numberOfSegments;
      threadQueue = MissingThreadTools.newSingleThreadExecutor(getClass().getSimpleName(), true, 1);
      decompressionInputDirectBuffer = ByteBuffer.allocateDirect(pointsPerSegment * inputBytesPerPoint);
      decompressionInputDirectBuffer.order(ByteOrder.nativeOrder());
      decompressionOutputDirectBuffer = ByteBuffer.allocateDirect(pointsPerSegment * inputBytesPerPoint);
      decompressionOutputDirectBuffer.order(ByteOrder.nativeOrder());

      if (topic.getType().equals(LidarScanMessage.class))
      {
         new IHMCROS2Callback<>(ros2Node, topic.withType(LidarScanMessage.class), this::queueRenderLidarScan);
      }
      else if (topic.getType().equals(StereoVisionPointCloudMessage.class))
      {
         new IHMCROS2Callback<>(ros2Node, topic.withType(StereoVisionPointCloudMessage.class), this::queueRenderStereoVisionPointCloud);
      }
      else if (topic.getType().equals(FusedSensorHeadPointCloudMessage.class))
      {
         new IHMCROS2Callback<>(ros2Node,
                                topic.withType(FusedSensorHeadPointCloudMessage.class),
                                ROS2QosProfile.BEST_EFFORT(),
                                this::queueRenderFusedSensorHeadPointCloud);
      }
   }

   private void queueRenderStereoVisionPointCloud(StereoVisionPointCloudMessage message)
   {
      frequencyPlot.recordEvent();
      // TODO: Possibly decompress on a thread here
      // TODO: threadQueue.clearQueueAndExecute(() ->
      latestStereoVisionMessageReference.set(message);
   }

   private void queueRenderLidarScan(LidarScanMessage message)
   {
      frequencyPlot.recordEvent();
      latestLidarScanMessageReference.set(message);
   }

   private void queueRenderFusedSensorHeadPointCloud(FusedSensorHeadPointCloudMessage message)
   {
      frequencyPlot.recordEvent();
      latestFusedSensorHeadPointCloudMessageReference.set(message);
   }

   @Override
   public void create()
   {
      super.create();
      pointCloudRenderer.create(pointsPerSegment, numberOfSegments);
   }

   @Override
   public void update()
   {
      super.update();
      if (isActive())
      {
         FusedSensorHeadPointCloudMessage fusedMessage = latestFusedSensorHeadPointCloudMessageReference.getAndSet(null);
         if (fusedMessage != null)
         {
            decompressionInputDirectBuffer.rewind();
            int numberOfBytes = fusedMessage.getScan().size();
            decompressionInputDirectBuffer.limit(numberOfBytes);
            for (int i = 0; i < numberOfBytes; i++)
            {
               decompressionInputDirectBuffer.put(fusedMessage.getScan().get(i));
            }
            decompressionInputDirectBuffer.flip();
            decompressionOutputDirectBuffer.clear();
            lz4Decompressor.decompress(decompressionInputDirectBuffer, decompressionOutputDirectBuffer);
            decompressionOutputDirectBuffer.rewind();

            latestSegmentIndex = (int) fusedMessage.getSegmentIndex();
            // TODO: Move to OpenCL
            pointCloudRenderer.updateMeshFastest(xyzRGBASizeFloatBuffer ->
            {
               float size = pointSize.get();
               for (int i = 0; i < pointsPerSegment; i++)
               {
                  float x = decompressionOutputDirectBuffer.getInt() * 0.003f;
                  float y = decompressionOutputDirectBuffer.getInt() * 0.003f;
                  float z = decompressionOutputDirectBuffer.getInt() * 0.003f;
                  color.set(decompressionOutputDirectBuffer.getInt());
                  // float r = 1.0f;
                  // float g = 1.0f;
                  // float b = 1.0f;
                  // float a = 1.0f;
                  xyzRGBASizeFloatBuffer.put(x);
                  xyzRGBASizeFloatBuffer.put(y);
                  xyzRGBASizeFloatBuffer.put(z);
                  xyzRGBASizeFloatBuffer.put(color.r);
                  xyzRGBASizeFloatBuffer.put(color.g);
                  xyzRGBASizeFloatBuffer.put(color.b);
                  xyzRGBASizeFloatBuffer.put(color.a);
                  xyzRGBASizeFloatBuffer.put(size);
               }
               return pointsPerSegment;
            }, latestSegmentIndex);
         }

         LidarScanMessage latestLidarScanMessage = latestLidarScanMessageReference.getAndSet(null);
         if (latestLidarScanMessage != null)
         {
            int numberOfScanPoints = latestLidarScanMessage.getNumberOfPoints();
            pointCloudRenderer.updateMeshFastest(xyzRGBASizeFloatBuffer ->
            {
               float size = pointSize.get();
               LidarPointCloudCompression.decompressPointCloud(latestLidarScanMessage.getScan(), numberOfScanPoints, (i, x, y, z) ->
               {
                  xyzRGBASizeFloatBuffer.put((float) x);
                  xyzRGBASizeFloatBuffer.put((float) y);
                  xyzRGBASizeFloatBuffer.put((float) z);
                  xyzRGBASizeFloatBuffer.put(color.r);
                  xyzRGBASizeFloatBuffer.put(color.g);
                  xyzRGBASizeFloatBuffer.put(color.b);
                  xyzRGBASizeFloatBuffer.put(color.a);
                  xyzRGBASizeFloatBuffer.put(size);
               });

               return numberOfScanPoints;
            });
         }

         StereoVisionPointCloudMessage latestStereoVisionMessage = latestStereoVisionMessageReference.getAndSet(null);
         if (latestStereoVisionMessage != null)
         {
            float size = pointSize.get();
            pointCloudRenderer.updateMeshFastest(xyzRGBASizeFloatBuffer ->
            {
               StereoPointCloudCompression.decompressPointCloud(latestStereoVisionMessage, (x, y, z) ->
               {
                  try
                  {
                     xyzRGBASizeFloatBuffer.put((float) x);
                     xyzRGBASizeFloatBuffer.put((float) y);
                     xyzRGBASizeFloatBuffer.put((float) z);
                     xyzRGBASizeFloatBuffer.put(color.r);
                     xyzRGBASizeFloatBuffer.put(color.g);
                     xyzRGBASizeFloatBuffer.put(color.b);
                     xyzRGBASizeFloatBuffer.put(color.a);
                     xyzRGBASizeFloatBuffer.put(size);
                  }
                  catch (BufferOverflowException e)
                  {
                     e.printStackTrace();
                  }
               });

               return latestStereoVisionMessage.getNumberOfPoints();
            });
         }
      }
   }

   @Override
   public void renderImGuiWidgets()
   {
      super.renderImGuiWidgets();
      ImGui.text(topic.getName());
      ImGui.sameLine();
      ImGui.pushItemWidth(30.0f);
      ImGui.dragFloat(labels.get("Size"), pointSize.getData(), 0.001f, 0.0005f, 0.1f);
      ImGui.popItemWidth();
      frequencyPlot.renderImGuiWidgets();
      segmentIndexPlot.render(latestSegmentIndex);
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      if (isActive())
         pointCloudRenderer.getRenderables(renderables, pool);
   }
}
