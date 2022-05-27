package us.ihmc.perception.gpuHeightMap;

import org.bytedeco.opencl._cl_kernel;
import org.bytedeco.opencl._cl_program;
import org.bytedeco.opencl.global.OpenCL;
import org.bytedeco.opencv.global.opencv_core;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.matrix.interfaces.RotationMatrixBasics;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.perception.BytedecoImage;
import us.ihmc.perception.OpenCLFloatBuffer;
import us.ihmc.perception.OpenCLManager;

import java.nio.ByteBuffer;

public class SimpleImageGPUHeightMapUpdater
{
   private final SimpleGPUHeightMapParameters parameters;
   private final int numberOfCells;

   private final OpenCLManager openCLManager;

   private final OpenCLFloatBuffer localizationBuffer = new OpenCLFloatBuffer(14);
   private final OpenCLFloatBuffer parametersBuffer = new OpenCLFloatBuffer(11);
   private final OpenCLFloatBuffer intrinsicsBuffer = new OpenCLFloatBuffer(4);

   private final OpenCLFloatBuffer elevationMapData;
   //   private final OpenCLFloatBuffer updatedMapData;

   private final BytedecoImage depthImage;
   private final BytedecoImage heightImage;
   private final BytedecoImage varianceImage;
   private final BytedecoImage countImage;
   private final int imageWidth;
   private final int imageHeight;

   private final _cl_program heightMapProgram;
   private final _cl_kernel zeroValuesKernel;
   private final _cl_kernel addPointsFromImageKernel;
   private final _cl_kernel averageMapKernel;

   private final SimpleGPUHeightMap simpleGPUHeightMap;

   private final Stopwatch readingStopwatch = new Stopwatch();
   private final Stopwatch writingStopwatch = new Stopwatch();
   private final Stopwatch zeroStopwatch = new Stopwatch();
   private final Stopwatch packingStopwatch = new Stopwatch();
   private final Stopwatch averageStopwatch = new Stopwatch();
   private final Stopwatch updateStopwatch = new Stopwatch();

   private float fx;
   private float fy;
   private float cx;
   private float cy;

   public SimpleImageGPUHeightMapUpdater(int imageWidth, int imageHeight, ByteBuffer sourceData, SimpleGPUHeightMapParameters parameters)
   {
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;


      this.openCLManager = new OpenCLManager();
      this.parameters = parameters;

      // the added two are for the borders
      numberOfCells = ((int) Math.round(parameters.mapLength / parameters.resolution)) + 2;

      this.depthImage = new BytedecoImage(imageWidth, imageHeight, opencv_core.CV_16UC1, sourceData);
      this.heightImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1, sourceData);
      this.varianceImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1, sourceData);
      this.countImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_8UC1, sourceData);

      simpleGPUHeightMap = new SimpleGPUHeightMap();

      int floatsPerLayer = numberOfCells * numberOfCells;
      elevationMapData = new OpenCLFloatBuffer(3 * floatsPerLayer);
      //      updatedMapData = new OpenCLFloatBuffer(3 * floatsPerLayer);

      openCLManager.create();
      heightMapProgram = openCLManager.loadProgram("SimpleGPUHeightMap");
      zeroValuesKernel = openCLManager.createKernel(heightMapProgram, "zeroValuesKernel");
      addPointsFromImageKernel = openCLManager.createKernel(heightMapProgram, "addPointsFromImageKernel");
      averageMapKernel = openCLManager.createKernel(heightMapProgram, "averageMapImagesKernel");

      zeroStopwatch.start();
      readingStopwatch.start();
      writingStopwatch.start();
      packingStopwatch.start();
      averageStopwatch.start();
      updateStopwatch.start();
      zeroStopwatch.suspend();
      packingStopwatch.suspend();
      averageStopwatch.suspend();
      updateStopwatch.suspend();
      readingStopwatch.suspend();
      writingStopwatch.suspend();
   }

   public void destroy()
   {
      heightMapProgram.close();
      zeroValuesKernel.close();
      addPointsFromImageKernel.close();
      averageMapKernel.close();
      openCLManager.destroy();
   }

   public void setCameraIntrinsics(double fx, double fy, double cx, double cy)
   {
      this.fx = (float) fx;
      this.fy = (float) fy;
      this.cx = (float) cx;
      this.cy = (float) cy;
   }

   public void computeFromDepthMap(RigidBodyTransformReadOnly transformToWorld)
   {
      populateLocalizaitonBuffer(transformToWorld.getTranslation().getX32(), transformToWorld.getTranslation().getY32(), transformToWorld);
      populateParametersBuffer();
      populateIntrinsicsBuffer();

      updateMapWithKernel();

      updateMapObject(transformToWorld.getTranslation().getX32(), transformToWorld.getTranslation().getY32());
   }

   public SimpleGPUHeightMap getHeightMap()
   {
      return simpleGPUHeightMap;
   }

   private final RotationMatrixBasics rotation = new RotationMatrix();

   private void populateLocalizaitonBuffer(float centerX, float centerY, RigidBodyTransformReadOnly transformToDesiredFrame)
   {
      rotation.set(transformToDesiredFrame.getRotation());
      LogTools.info("rotation " + rotation);

      localizationBuffer.getBytedecoFloatBufferPointer().put(0, centerX);
      localizationBuffer.getBytedecoFloatBufferPointer().put(1, centerY);
      localizationBuffer.getBytedecoFloatBufferPointer().put(2, (float) rotation.getM00());
      localizationBuffer.getBytedecoFloatBufferPointer().put(3, (float) rotation.getM01());
      localizationBuffer.getBytedecoFloatBufferPointer().put(4, (float) rotation.getM02());
      localizationBuffer.getBytedecoFloatBufferPointer().put(5, (float) rotation.getM10());
      localizationBuffer.getBytedecoFloatBufferPointer().put(6, (float) rotation.getM11());
      localizationBuffer.getBytedecoFloatBufferPointer().put(7, (float) rotation.getM12());
      localizationBuffer.getBytedecoFloatBufferPointer().put(8, (float) rotation.getM20());
      localizationBuffer.getBytedecoFloatBufferPointer().put(9, (float) rotation.getM21());
      localizationBuffer.getBytedecoFloatBufferPointer().put(10, (float) rotation.getM22());
      localizationBuffer.getBytedecoFloatBufferPointer().put(11, transformToDesiredFrame.getTranslation().getX32());
      localizationBuffer.getBytedecoFloatBufferPointer().put(12, transformToDesiredFrame.getTranslation().getY32());
      localizationBuffer.getBytedecoFloatBufferPointer().put(13, transformToDesiredFrame.getTranslation().getZ32());
   }

   private void populateIntrinsicsBuffer()
   {
      intrinsicsBuffer.getBytedecoFloatBufferPointer().put(0, cx);
      intrinsicsBuffer.getBytedecoFloatBufferPointer().put(1, cy);
      intrinsicsBuffer.getBytedecoFloatBufferPointer().put(2, fx);
      intrinsicsBuffer.getBytedecoFloatBufferPointer().put(3, fy);
   }

   private void populateParametersBuffer()
   {
      parametersBuffer.getBytedecoFloatBufferPointer().put(0, (float) numberOfCells);
      parametersBuffer.getBytedecoFloatBufferPointer().put(1, (float) numberOfCells);
      parametersBuffer.getBytedecoFloatBufferPointer().put(2, (float) parameters.resolution);
      parametersBuffer.getBytedecoFloatBufferPointer().put(3, (float) parameters.minValidDistance);
      parametersBuffer.getBytedecoFloatBufferPointer().put(4, (float) parameters.maxHeightRange);
      parametersBuffer.getBytedecoFloatBufferPointer().put(5, (float) parameters.rampedHeightRangeA);
      parametersBuffer.getBytedecoFloatBufferPointer().put(6, (float) parameters.rampedHeightRangeB);
      parametersBuffer.getBytedecoFloatBufferPointer().put(7, (float) parameters.rampedHeightRangeC);
      parametersBuffer.getBytedecoFloatBufferPointer().put(8, (float) parameters.sensorNoiseFactor);
      parametersBuffer.getBytedecoFloatBufferPointer().put(9, (float) parameters.initialVariance);
      parametersBuffer.getBytedecoFloatBufferPointer().put(10, (float) parameters.maxVariance);
   }

   boolean firstRun = true;

   private void updateMapWithKernel()
   {
      writingStopwatch.resume();
      // TODO reshape height map
      if (firstRun)
      {
         firstRun = false;
         localizationBuffer.createOpenCLBufferObject(openCLManager);
         parametersBuffer.createOpenCLBufferObject(openCLManager);
         elevationMapData.createOpenCLBufferObject(openCLManager);
         intrinsicsBuffer.createOpenCLBufferObject(openCLManager);
         depthImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_ONLY);
         heightImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);
         varianceImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);
         countImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);

         openCLManager.setKernelArgument(zeroValuesKernel, 0, parametersBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(zeroValuesKernel, 1, elevationMapData.getOpenCLBufferObject());

         openCLManager.setKernelArgument(addPointsFromImageKernel, 0, depthImage.getOpenCLImageObject());
         openCLManager.setKernelArgument(addPointsFromImageKernel, 1, localizationBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(addPointsFromImageKernel, 2, parametersBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(addPointsFromImageKernel, 3, intrinsicsBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(addPointsFromImageKernel, 4, elevationMapData.getOpenCLBufferObject());

         openCLManager.setKernelArgument(averageMapKernel, 0, elevationMapData.getOpenCLBufferObject());
         openCLManager.setKernelArgument(averageMapKernel, 1, parametersBuffer.getOpenCLBufferObject());
         openCLManager.setKernelArgument(averageMapKernel, 2, heightImage.getOpenCLImageObject());
         openCLManager.setKernelArgument(averageMapKernel, 3, varianceImage.getOpenCLImageObject());
         openCLManager.setKernelArgument(averageMapKernel, 4, countImage.getOpenCLImageObject());
      }
      else
      {
         depthImage.writeOpenCLImage(openCLManager);

         localizationBuffer.writeOpenCLBufferObject(openCLManager);
         parametersBuffer.writeOpenCLBufferObject(openCLManager);
         intrinsicsBuffer.writeOpenCLBufferObject(openCLManager);
      }
      writingStopwatch.lap();
      writingStopwatch.suspend();

      zeroStopwatch.resume();
      openCLManager.execute2D(zeroValuesKernel, numberOfCells, numberOfCells);
      zeroStopwatch.lap();
      zeroStopwatch.suspend();

      packingStopwatch.resume();
      openCLManager.execute2D(addPointsFromImageKernel, imageWidth, imageHeight);
      packingStopwatch.lap();
      packingStopwatch.suspend();

      averageStopwatch.resume();
      openCLManager.execute2D(averageMapKernel, numberOfCells, numberOfCells);
      averageStopwatch.lap();
      averageStopwatch.suspend();

      readingStopwatch.resume();
      openCLManager.enqueueReadImage(heightImage.getOpenCLImageObject(), numberOfCells, numberOfCells, heightImage.getBytedecoByteBufferPointer());

//      varianceImage.readOpenCLImage(openCLManager);
//      countImage.readOpenCLImage(openCLManager);

      openCLManager.finish();
      readingStopwatch.lap();
      readingStopwatch.suspend();
   }

   public void printStopwatches()
   {
      LogTools.info("Reading time " + readingStopwatch.averageLap());
      LogTools.info("Zeroing time " + zeroStopwatch.averageLap());
      LogTools.info("Packing time " + packingStopwatch.averageLap());
      LogTools.info("Averaging time " + averageStopwatch.averageLap());
      LogTools.info("Update time " + updateStopwatch.averageLap());
      LogTools.info("Writing time " + writingStopwatch.averageLap());
   }

   private void updateMapObject(double centerX, double centerY)
   {
      updateStopwatch.resume();
      simpleGPUHeightMap.setCenter(centerX, centerY);
      simpleGPUHeightMap.setResolution(parameters.resolution);

      simpleGPUHeightMap.updateFromFloatBufferImage(heightImage.getBackingDirectByteBuffer().asFloatBuffer(),
//                                               varianceImage.getBackingDirectByteBuffer().asFloatBuffer(),
//                                               countImage.getBackingDirectByteBuffer().asFloatBuffer(),
                                               numberOfCells);
      updateStopwatch.lap();
      updateStopwatch.suspend();
   }
}
