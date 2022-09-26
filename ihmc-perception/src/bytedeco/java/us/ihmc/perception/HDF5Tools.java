package us.ihmc.perception;

import org.apache.commons.lang3.ArrayUtils;
import org.bytedeco.javacpp.*;
import org.bytedeco.hdf5.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.log.LogTools;

import java.util.ArrayList;

import static org.bytedeco.hdf5.global.hdf5.*;

public class HDF5Tools
{
   static final String FILE_NAME = "/home/bmishra/Workspace/Data/Atlas_Logs/ROSBags/atlas_perception_run_1.h5";
   static final String DATASET_NAME = "/os_cloud_node/points/0";

   static final int PCD_POINT_SIZE = 3;
   static final int DIM0 = 2048;
   static final int DIM1 = 64;

   static final int IMG_WIDTH = 1024;
   static final int IMG_HEIGHT = 768;

   static final String OUSTER_POINT_CLOUD = "/os_cloud_node/points";
   static final String L515_DEPTH = "/chest_l515/depth/image_rect_raw";
   static final String L515_COLOR = "/chest_l515/color/image_rect_raw";

   private static int extractShape(DataSet dataSet, int dim) {
      DataSpace space = dataSet.getSpace();
      int nbDims = space.getSimpleExtentNdims();
      long[] shape = new long[nbDims];
      space.getSimpleExtentDims(shape);
      if(dim < nbDims)
      {
         return (int) shape[dim];
      }
      else return 0;
   }

   public static void loadPointCloud(Group group, int index, RecyclingArrayList<Point3D32> points)
   {
      DataSet dataset = group.openDataSet(String.valueOf(index));
      float[] pointsBuffer = new float[DIM0 * DIM1 * 3];
      FloatPointer p = new FloatPointer(pointsBuffer);
      dataset.read(p, PredType.NATIVE_FLOAT());
      p.get(pointsBuffer);

      points.clear();
      for(int i = 0; i<pointsBuffer.length; i+=3)
      {
         Point3D32 point = points.add();
         point.set(pointsBuffer[i], pointsBuffer[i+1], pointsBuffer[i+2]);
      }
   }

   public static void loadImage(Group group, int index, Mat mat)
   {
      DataSet dataset = group.openDataSet(String.valueOf(index));
      byte[] pointsBuffer = new byte[mat.rows() * mat.cols() * mat.channels()];
      BytePointer p = new BytePointer(pointsBuffer);
      dataset.read(p, PredType.NATIVE_UINT8());
      p.get(pointsBuffer);
      mat.data(p);
   }

   public static void storePointCloud(Group group, int index, RecyclingArrayList<Point3D32> points)
   {
      DataSet dataset = group.openDataSet(String.valueOf(index));
      float[] buf = new float[points.size() * PCD_POINT_SIZE];
      for (int i = 0; i <  points.size(); i++)
         for (int j = 0; j < PCD_POINT_SIZE; j++)
            buf[i * PCD_POINT_SIZE + j] = i + j;

      dataset.write(new FloatPointer(buf), new DataType(PredType.NATIVE_FLOAT()));
   }

   public static void storeDepthMap(Group group, int index, Mat depthMap)
   {
      DataSet dataset = group.openDataSet(String.valueOf(index));
      dataset.write(depthMap.data(), PredType.NATIVE_UINT16());
   }

   public static void storeCompressedImage(Group group, int index, BytePointer data)
   {
      DataSet dataset = group.openDataSet(String.valueOf(index));
      dataset.write(data, PredType.NATIVE_UINT8());
   }

   public static ArrayList<Float> getTimestamps(Group group)
   {
      ArrayList<Float> timestamps = new ArrayList<>();
      return timestamps;
   }

   public static ArrayList<String> getTopicNames(H5File file)
   {
      ArrayList<String> names = new ArrayList<>();

      for(int i = 0; i<file.getNumObjs(); i++)
      {
         String obj = file.getObjnameByIdx(i).getString();
         Group group = file.openGroup(obj);
         exploreH5(group, names, obj);
      }

      return names;
   }



   public static void exploreH5(Group group, ArrayList<String> names, String prefix)
   {
      int count = 0;
      for (int i = 0; i < group.getNumObjs(); i++) {
         BytePointer objPtr = group.getObjnameByIdx(i);
         if (group.childObjType(objPtr) == H5O_TYPE_GROUP) {
            count++;
            String grpName = group.getObjnameByIdx(i).getString();
            Group grp = group.openGroup(grpName);
            exploreH5(grp, names, prefix + "/" + grpName);
         }
         //         if (group.childObjType(objPtr) == H5O_TYPE_DATASET) {
         //            String dsName = group.getObjnameByIdx(i).getString();
         //            System.out.println("Dataset: " + dsName + "\t" + "Prefix: " + prefix);
         //         }

         //         System.out.println("ExploreH5: " + group.getObjnameByIdx(i).getString());
      }

      if(count == 0)
      {
         System.out.println("Prefix: " + prefix);
         names.add(prefix);
      }
   }

   public static void storeFloatArray2D(Group group, long index, ArrayList<Float> data, int cols)
   {
      long[] dims = { HDF5Manager.MAX_BUFFER_SIZE, cols };

      DataSet dataset = group.createDataSet(String.valueOf(index), new DataType(PredType.NATIVE_FLOAT()), new DataSpace(2, dims));
      float[] dataObject = ArrayUtils.toPrimitive(data.toArray(new Float[0]), 0.0F);
      dataset.write(new FloatPointer(dataObject), new DataType(PredType.NATIVE_FLOAT()));
   }

   public static void storeMatrix(Group group, double[] data)
   {
      long[] dims = { 5, 5 };
      if(group.nameExists(String.valueOf(0)))
      {
         DataSet dataset = group.openDataSet(String.valueOf(0));
         dataset.write(new DoublePointer(data), new DataType(PredType.NATIVE_DOUBLE()));
      }
      else
      {
         DataSet dataset = group.createDataSet(String.valueOf(0), new DataType(PredType.NATIVE_DOUBLE()), new DataSpace(2, dims));
      }
   }

   public static void main(String[] args) {
      String HDF5_FILENAME = "/home/bmishra/Workspace/Data/Atlas_Logs/ROSBags/atlas_perception_run_1.h5";
      H5File file = new H5File(HDF5_FILENAME, H5F_ACC_RDONLY);

      ArrayList<String> topicNames = HDF5Tools.getTopicNames(file);
   }
}
