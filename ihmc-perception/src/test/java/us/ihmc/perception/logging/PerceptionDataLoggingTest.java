package us.ihmc.perception.logging;

import org.bytedeco.hdf5.Group;
import org.bytedeco.hdf5.global.hdf5;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.junit.jupiter.api.Test;
import us.ihmc.log.LogTools;
import us.ihmc.perception.BytedecoOpenCVTools;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static us.ihmc.robotics.Assert.assertEquals;

public class PerceptionDataLoggingTest
{
   private HDF5Manager hdf5ManagerReader;
   private HDF5Manager hdf5ManagerWriter;

   @Test
   public void testLoggingByteArray()
   {
      hdf5ManagerWriter = new HDF5Manager("/home/quantum/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_TRUNC());
      Group writeGroup = hdf5ManagerWriter.getGroup("/test/bytes/");

      byte[] dataArray = {(byte) 0, (byte) 255, (byte) 1, (byte) 3, (byte) 4, (byte) 42, (byte) 153, (byte) 0};

      ByteBuffer buffer = ByteBuffer.wrap(dataArray);
      IntBuffer intBuffer = buffer.asIntBuffer();
      int[] array = new int[dataArray.length / Integer.BYTES];
      intBuffer.get(array);

      HDF5Tools.storeIntArray(writeGroup, 0, array, array.length);

      writeGroup.close();
      hdf5ManagerWriter.getFile().close();

      hdf5ManagerReader = new HDF5Manager("/home/quantum/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_RDONLY());
      Group readGroup = hdf5ManagerReader.openGroup("/test/bytes/");
      int[] outputIntArray = HDF5Tools.loadIntArray(readGroup, 0);

      byte[] outputArray = new byte[outputIntArray.length * Integer.BYTES];
      ByteBuffer byteBuffer = ByteBuffer.wrap(outputArray);
      IntBuffer outputIntBuffer = byteBuffer.asIntBuffer();
      outputIntBuffer.put(outputIntArray);

      for (int i = 0; i < dataArray.length; i++)
      {
         assertEquals(dataArray[i], outputArray[i]);
      }
      hdf5ManagerReader.getFile().close();
   }

   @Test
   public void testLoggingLargeByteArray()
   {
      hdf5ManagerWriter = new HDF5Manager("/home/quantum/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_TRUNC());
      Group writeGroup = hdf5ManagerWriter.getGroup("/test/bytes/");

      byte[] dataArray = new byte[40];
      for (int i = 0; i < dataArray.length; i++)
      {
         dataArray[i] = (byte) i;
      }

      long begin = System.currentTimeMillis();

      System.out.println(Arrays.toString(dataArray));

      HDF5Tools.storeByteArray(writeGroup, 0, dataArray, dataArray.length);

      long intermediate = System.currentTimeMillis();

      writeGroup.close();
      hdf5ManagerWriter.getFile().close();

      hdf5ManagerReader = new HDF5Manager("/home/quantum/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_RDONLY());
      Group readGroup = hdf5ManagerReader.openGroup("/test/bytes/");

      byte[] outputArray = HDF5Tools.loadByteArray(readGroup, 0);

      long end = System.currentTimeMillis();
      LogTools.info("Logging Took: {} ms", intermediate - begin);
      LogTools.info("Loading Took: {} ms", end - intermediate);

      System.out.println(Arrays.toString(outputArray));

      for (int i = 0; i < dataArray.length; i++)
      {
         assertEquals(dataArray[i], outputArray[i]);
      }
      hdf5ManagerReader.getFile().close();
   }

   @Test
   public void testLoggingIntArray()
   {
      hdf5ManagerWriter = new HDF5Manager("/home/quantum/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_TRUNC());
      Group writeGroup = hdf5ManagerWriter.getGroup("/test/ints/");

      int[] dataArray = {0, 255, 1, 3, 4, 42, 153};

      HDF5Tools.storeIntArray(writeGroup, 0, dataArray, dataArray.length);

      writeGroup.close();
      hdf5ManagerWriter.getFile().close();

      hdf5ManagerReader = new HDF5Manager("/home/quantum/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_RDONLY());
      Group readGroup = hdf5ManagerReader.getGroup("/test/ints/");
      int[] outputArray = HDF5Tools.loadIntArray(readGroup, 0);

      assertEquals(dataArray.length, 7);
      assertEquals(outputArray.length, 7);

      for (int i = 0; i < dataArray.length; i++)
      {
         assertEquals(dataArray[i], outputArray[i]);
      }
      hdf5ManagerReader.getFile().close();
   }

   @Test
   public void testCompressedFloatDepthLogging()
   {

      hdf5ManagerWriter = new HDF5Manager("/home/bmishra/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_TRUNC());

      Mat depth = new Mat(128, 128, opencv_core.CV_32FC1);
      depth.put(new Scalar(1.234));


      BytePointer compressedDepthPointer = new BytePointer();
      BytedecoOpenCVTools.compressFloatDepthPNG(depth, compressedDepthPointer);

      byte[] dataArray = new byte[compressedDepthPointer.asBuffer().remaining()];
      Group writeGroup = hdf5ManagerWriter.getGroup("/test/bytes/");
      HDF5Tools.storeByteArray(writeGroup, 0, dataArray, dataArray.length);

      writeGroup.close();
      hdf5ManagerWriter.getFile().close();

//      hdf5ManagerReader = new HDF5Manager("/home/bmishra/Workspace/Data/Sensor_Logs/hdf5_test.hdf5", hdf5.H5F_ACC_RDONLY());
//      Group readGroup = hdf5ManagerReader.openGroup("/test/bytes/");
//
//      byte[] outputArray = HDF5Tools.loadByteArray(readGroup, 0);
//
//      BytePointer messageEncodedBytePointer = new BytePointer(outputArray);
//      messageEncodedBytePointer.limit(outputArray.length);
//
//      Mat inputJPEGMat = new Mat(1, outputArray.length, opencv_core.CV_8UC1, messageEncodedBytePointer);
//      Mat inputYUVI420Mat = new Mat(1, 1, opencv_core.CV_8UC1);
//      opencv_imgcodecs.imdecode(inputJPEGMat, opencv_imgcodecs.IMREAD_UNCHANGED, inputYUVI420Mat);
//
//      Mat finalMat = new Mat((int) (inputYUVI420Mat.rows() / 1.5f), inputYUVI420Mat.cols(), opencv_core.CV_8UC4);
//
//      LogTools.info("Height: {}, Width: {}", inputYUVI420Mat.rows(), inputYUVI420Mat.cols());
//      opencv_imgproc.cvtColor(inputYUVI420Mat, finalMat, opencv_imgproc.COLOR_YUV2RGBA_I420);
//
//      Mat finalDepth = new Mat(128, 128, opencv_core.CV_32FC1, finalMat.data());
//
//      FloatBufferIndexer indexer = new FloatBufferIndexer(finalDepth.getFloatBuffer());
//      FloatBufferIndexer indexerActual = new FloatBufferIndexer(depth.getFloatBuffer());
//
//      for(int i = 0; i<128; i++)
//      {
//         for(int j = 0; j<128; j++)
//         {
//            LogTools.info("Depth ({} {}): {}", i, j, indexer.get(i*128 + j));
//            LogTools.info("Actual ({} {}): {}", i, j, indexerActual.get(i*128 + j));
//         }
//      }
   }
}
