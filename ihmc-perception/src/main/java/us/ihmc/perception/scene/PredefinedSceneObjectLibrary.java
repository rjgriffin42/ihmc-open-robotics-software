package us.ihmc.perception.scene;

import gnu.trove.map.hash.TIntDoubleHashMap;
import us.ihmc.perception.objects.BasicSceneObjects;
import us.ihmc.perception.objects.DoorSceneObjects;
import us.ihmc.perception.objects.StaticArUcoRelativeDetectableSceneObject;

import java.util.*;

/**
 * Use to specify which scene objects a robot is looking for.
 *
 * We can put more specific stuff in here for now, like door specific
 * and even dynamic objects.
 *
 * This is a super high level class, it includes ROS 2 topics,
 * heuristic stuff for certain objects, stuff like that.
 *
 * This class exists so we can support multiple robots detecting the same
 * objects, but also to serve the need we have to define things
 * by hand and construct custom heuristics.
 */
public class PredefinedSceneObjectLibrary
{
   private final ArUcoDetectableObject pushDoorPanel;
   private final ArUcoDetectableObject pullDoorPanel;
   private final StaticArUcoRelativeDetectableSceneObject pushDoorFrame;
   private final StaticArUcoRelativeDetectableSceneObject pullDoorFrame;
   private final ArUcoDetectableObject box;
   private final ArUcoDetectableObject canOfSoup;

   private final HashSet<ArUcoDetectableObject> arUcoDetectableObjects = new HashSet<>();
   private final HashMap<Integer, StaticArUcoRelativeDetectableSceneObject> staticArUcoRelativeDetectableObjects = new HashMap<>();
   private final TIntDoubleHashMap arUcoMarkerIDsToSizes = new TIntDoubleHashMap();
   private final StaticArUcoRelativeDetectableSceneObject pushDoorLeverHandle;
   private final StaticArUcoRelativeDetectableSceneObject pullDoorLeverHandle;

   public static PredefinedSceneObjectLibrary defaultObjects()
   {
      return new PredefinedSceneObjectLibrary();
   }

   private PredefinedSceneObjectLibrary()
   {
      // Add door stuff
      pushDoorPanel = DoorSceneObjects.createPushDoorPanel();
      pullDoorPanel = DoorSceneObjects.createPullDoorPanel();
      arUcoDetectableObjects.add(pushDoorPanel);
      arUcoDetectableObjects.add(pullDoorPanel);
      arUcoMarkerIDsToSizes.put(pushDoorPanel.getMarkerID(), pushDoorPanel.getMarkerSize());
      arUcoMarkerIDsToSizes.put(pullDoorPanel.getMarkerID(), pullDoorPanel.getMarkerSize());

      // The frames stay in place after being seen
      pushDoorFrame = DoorSceneObjects.createPushDoorFrame();
      pullDoorFrame = DoorSceneObjects.createPullDoorFrame();
      pushDoorLeverHandle = DoorSceneObjects.createPushDoorLeverHandle();
      pullDoorLeverHandle = DoorSceneObjects.createPullDoorLeverHandle();
      staticArUcoRelativeDetectableObjects.put(pushDoorFrame.getMarkerID(), pushDoorFrame);
      staticArUcoRelativeDetectableObjects.put(pullDoorFrame.getMarkerID(), pullDoorFrame);
      staticArUcoRelativeDetectableObjects.put(pushDoorLeverHandle.getMarkerID(), pushDoorLeverHandle);
      staticArUcoRelativeDetectableObjects.put(pullDoorLeverHandle.getMarkerID(), pullDoorLeverHandle);

      box = BasicSceneObjects.createBox();
      canOfSoup = BasicSceneObjects.createCanOfSoup();
      arUcoDetectableObjects.add(box);
      arUcoDetectableObjects.add(canOfSoup);
      arUcoMarkerIDsToSizes.put(box.getMarkerID(), box.getMarkerSize());
      arUcoMarkerIDsToSizes.put(canOfSoup.getMarkerID(), canOfSoup.getMarkerSize());

      // TODO: Add non-ArUco cup -- detected by neural net

   }

   public Set<ArUcoDetectableObject> getArUcoDetectableObjects()
   {
      return arUcoDetectableObjects;
   }

   public HashMap<Integer, StaticArUcoRelativeDetectableSceneObject> getStaticArUcoRelativeDetectableObjects()
   {
      return staticArUcoRelativeDetectableObjects;
   }

   public TIntDoubleHashMap getArUcoMarkerIDsToSizes()
   {
      return arUcoMarkerIDsToSizes;
   }
}
