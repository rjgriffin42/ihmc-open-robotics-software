package us.ihmc.utilities.ros.subscriber;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import sensor_msgs.PointCloud2;
import us.ihmc.utilities.ros.types.PointType;

public abstract class RosPointCloudSubscriber extends AbstractRosTopicSubscriber<PointCloud2>
{
   private boolean DEBUG = false;

   public RosPointCloudSubscriber()
   {
      super(sensor_msgs.PointCloud2._TYPE);
   }

   /*
    *  rostopic echo /ibeo/points
    * /ibeo_link
    * header:
    * seq: 38993
    * stamp:
    *   secs: 1401924668
    *   nsecs: 468290060
    * frame_id: /ibeo_link
    * height: 1
    * width: 2025
    * fields:
    * -
    *   name: x
    *   offset: 0
    *   datatype: 7
    *   count: 1
    * -
    *   name: y
    *   offset: 4
    *   datatype: 7
    *   count: 1
    * -
    *   name: z
    *   offset: 8
    *   datatype: 7
    *   count: 1
    * is_bigendian: False
    * point_step: 16
    * row_step: 32400
    * data: [172, 111, 184, 64, 115, 205, 219, 64, 127, 44, 124, 190, 0, 0, 128, 63, 122, 230, 184, 64, 9, 91, 220, 64, 108, 116, 248, 189, 0, 0, 128, 63, 251, 94, 131, 64, 5, 32, 151, 64, 182, 14, 50, 190, 0, 0, 128, 63, 104, 76, 133, 64, 164, 87, 153, 64, 190, 177, 179, 189, 0, 0, 128, 63, 77, 37, 182, 64, 22, 75, 202, 64, 78, 220, 116, 190, 0, 0, 128, 63, 26, 69, 183, 64, 185, 138, 203, 64, 181, 224, 247, 189, 0, 0, 128, 63, 46, 240, 181, 64, 216, 26, 195, 64, 106, 181, 114, 190, 0, 0, 128, 63, 183, 165, 182, 64, 132, 221, 195, 64, 80, 224
    */

   /*
    *
    * tingfan@unknownid-All-Series:~/gworkspace/IHMCPerception/catkin_ws/src/lidar_to_point_cloud_transformer$ rostopic echo /multisense/image_points2_color |more
    * header:
    * seq: 1417800
    * stamp:
    * secs: 1423988842
    * nsecs: 953257000
    * frame_id: /multisense/left_camera_optical_frame
    * height: 1
    * width: 442515
    * fields:
    * -
    * name: x
    * offset: 0
    * datatype: 7
    * count: 1
    * -
    * name: y
    * offset: 4
    * datatype: 7
    * count: 1
    * -
    * name: z
    * offset: 8
    * datatype: 7
    * count: 1
    * -
    * name: rgb
    * offset: 12
    * datatype: 7
    * count: 1
    * is_bigendian: False
    * point_step: 16
    * row_step: 7080240
    * data: [33, 215, 239, 63, 230, 81, 175, 191, 130, 209, 59, 64, 60, 80, 69, 0, 22, 132, 240, 63, 230, 81, 175, 191, 130, 209, 59, 64, 66, 91, 73, 0, 11, 49, 241, 63, 230, 81, 175, 191, 130, 209
    *
    */

   protected Point3d[] points = null;
   protected float[] intensities = null;
   protected Color3f[] pointColors = null;
   protected PointType pointType = null;

   protected void unpackPointsAndIntensities(PointCloud2 pointCloud)
   {
      int numberOfPoints = pointCloud.getWidth() * pointCloud.getHeight();
      points = new Point3d[numberOfPoints];
      pointType = PointType.fromFromFieldNames(pointCloud.getFields());

      switch (pointType)
      {
         case XYZI :
            intensities = new float[numberOfPoints];

            break;

         case XYZRGB :
            pointColors = new Color3f[numberOfPoints];

            break;
      }

      int offset = pointCloud.getData().arrayOffset();
      int pointStep = pointCloud.getPointStep();

      ByteBuffer byteBuffer = ByteBuffer.wrap(pointCloud.getData().array(), offset, numberOfPoints * pointStep);

      if (pointCloud.getIsBigendian())
         byteBuffer.order(ByteOrder.BIG_ENDIAN);
      else
         byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

      for (int i = 0; i < numberOfPoints; i++)
      {
         float x = byteBuffer.getFloat();
         float y = byteBuffer.getFloat();
         float z = byteBuffer.getFloat();
         points[i] = new Point3d(x, y, z);

         switch (pointType)
         {
            case XYZI :
               intensities[i] = byteBuffer.getFloat();;

               break;

            case XYZRGB :
               int b = (int) byteBuffer.get();
               int g = (int) byteBuffer.get();
               int r = (int) byteBuffer.get();
               byte dummy = byteBuffer.get();
               pointColors[i] = new Color3f(r, g, b);
         }
      }
   }

   @Override
   public abstract void onNewMessage(PointCloud2 pointCloud);

}
