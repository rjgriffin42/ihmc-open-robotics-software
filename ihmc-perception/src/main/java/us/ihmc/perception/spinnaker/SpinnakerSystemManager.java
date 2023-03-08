package us.ihmc.perception.spinnaker;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.spinnaker.Spinnaker_C.*;
import org.bytedeco.spinnaker.global.Spinnaker_C;
import us.ihmc.log.LogTools;

import java.util.ArrayList;

import static us.ihmc.perception.spinnaker.SpinnakerTools.assertNoError;

public class SpinnakerSystemManager
{
   private final spinSystem spinSystem = new spinSystem();
   private final spinCameraList spinCameraList = new spinCameraList();
   private final ArrayList<Runnable> stuffToDestroy = new ArrayList<>();

   public SpinnakerSystemManager()
   {
      initializeAndShutdownToFixState();

      assertNoError(Spinnaker_C.spinSystemGetInstance(spinSystem), "Unable to retrieve Spinnaker system instance!");
      assertNoError(Spinnaker_C.spinCameraListCreateEmpty(spinCameraList), "Unable to create camera list");
      assertNoError(Spinnaker_C.spinSystemGetCameras(spinSystem, spinCameraList), "Unable to retrieve camera list from Spinnaker system");
   }

   public SpinnakerBlackfly createBlackfly(String serialNumber)
   {
      LogTools.info("Creating Blackfly with serial number {}", serialNumber);
      spinCamera blackflyCamera = new spinCamera();
      assertNoError(Spinnaker_C.spinCameraListGetBySerial(spinCameraList, new BytePointer(serialNumber), blackflyCamera),
                    "Unable to create spinCamera from serial number!");

      SpinnakerBlackfly spinnakerBlackfly = new SpinnakerBlackfly(blackflyCamera, serialNumber);
      stuffToDestroy.add(spinnakerBlackfly::destroy);
      return spinnakerBlackfly;
   }

   public void destroy()
   {
      for (Runnable destroy : stuffToDestroy)
      {
         destroy.run();
      }

      Spinnaker_C.spinCameraListClear(spinCameraList);
      Spinnaker_C.spinCameraListDestroy(spinCameraList);
      Spinnaker_C.spinSystemReleaseInstance(spinSystem);
   }

   /**
    * Work around to fix camera state issues if the spinnaker cameras were not
    * released properly (if the process crashed mid-way, for example)
    */
   private static void initializeAndShutdownToFixState()
   {
      spinSystem spinSystem = new spinSystem();
      assertNoError(Spinnaker_C.spinSystemGetInstance(spinSystem), "Unable to get spin system instance");

      spinCameraList spinCameraList = new spinCameraList();
      assertNoError(Spinnaker_C.spinCameraListCreateEmpty(spinCameraList), "Unable to create camera list");

      assertNoError(Spinnaker_C.spinSystemGetCameras(spinSystem, spinCameraList), "Unable to get spin cameras");

      SizeTPointer cameraCount = new SizeTPointer(1);
      assertNoError(Spinnaker_C.spinCameraListGetSize(spinCameraList, cameraCount), "Unable to get spin camera list size");

      for (int i = 0; i < cameraCount.get(); i++)
      {
         spinCamera spinCamera = new spinCamera();
         assertNoError(Spinnaker_C.spinCameraListGet(spinCameraList, i, spinCamera), "Unable to get spin camera");

         spinNodeMapHandle tlDeviceNodeMap = new spinNodeMapHandle();
         assertNoError(Spinnaker_C.spinCameraGetTLDeviceNodeMap(spinCamera, tlDeviceNodeMap), "Unable to get spin TL device node map");

         assertNoError(Spinnaker_C.spinCameraInit(spinCamera), "Unable to initialize spin camera");

         spinNodeMapHandle nodeMap = new spinNodeMapHandle();
         assertNoError(Spinnaker_C.spinCameraGetNodeMap(spinCamera, nodeMap), "Unable to get spin camera node map");

         spinNodeHandle acquisitionMode = new spinNodeHandle();
         assertNoError(Spinnaker_C.spinNodeMapGetNode(nodeMap, new BytePointer("AcquisitionMode"), acquisitionMode), "Unable to get AcquisitionMode");

         spinNodeHandle acquisitionModeContinuous = new spinNodeHandle();
         assertNoError(Spinnaker_C.spinEnumerationGetEntryByName(acquisitionMode, new BytePointer("Continuous"), acquisitionModeContinuous),
                       "Unable to get AcquisitionMode Continuous entry");

         LongPointer acquisitionModeContinuousInt = new LongPointer(1);
         assertNoError(Spinnaker_C.spinEnumerationEntryGetIntValue(acquisitionModeContinuous, acquisitionModeContinuousInt),
                       "Unable to get AcquisitionMode Continuous value");

         assertNoError(Spinnaker_C.spinEnumerationSetIntValue(acquisitionMode, acquisitionModeContinuousInt.get()),
                       "Unable to set AcquisitionMode Continuous value");
         assertNoError(Spinnaker_C.spinCameraBeginAcquisition(spinCamera), "Unable to start spin camera acquisition");
         assertNoError(Spinnaker_C.spinCameraEndAcquisition(spinCamera), "Unable to end spin camera acquisition");
         assertNoError(Spinnaker_C.spinCameraRelease(spinCamera), "Unable to release spin camera");
      }

      assertNoError(Spinnaker_C.spinCameraListClear(spinCameraList), "Unable to clear spin camera list");
      assertNoError(Spinnaker_C.spinCameraListDestroy(spinCameraList), "Unable to destroy spin camera list");
      assertNoError(Spinnaker_C.spinSystemReleaseInstance(spinSystem), "Unable to release spin system instance");
   }
}