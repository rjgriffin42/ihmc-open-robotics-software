package us.ihmc.perception.objects;

import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DBasics;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.ReferenceFrameMissingTools;

public class ArUcoMarkerObject
{
   private RigidBodyTransform markerToWorld = new RigidBodyTransform();
   private final ReferenceFrame markerFrame = ReferenceFrameMissingTools.constructFrameWithChangingTransformToParent(ReferenceFrame.getWorldFrame(),
                                                                                                                     markerToWorld);
   private final RigidBodyTransform objectToMarker = new RigidBodyTransform();
   // object frame might be different from marker location, for example the door handle is not exactly where the marker is on the door
   private ReferenceFrame objectFrame;
   private FramePose3D objectPose = new FramePose3D();
   private final RigidBodyTransform appendixToObject = new RigidBodyTransform();
   private FramePose3D appendixPose = new FramePose3D();
   private boolean hasAppendix =  false;

   public ArUcoMarkerObject(int id, ObjectInfo arucoInfo)
   {
      objectToMarker.set(arucoInfo.getMarkerYawPitchRoll(id), arucoInfo.getMarkerTranslation(id));
      objectFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(markerFrame, objectToMarker);
      if (arucoInfo.hasAppendix(id))
      {
         hasAppendix = true;
         appendixToObject.set(arucoInfo.getAppendixYawPitchRoll(id), arucoInfo.getAppendixTranslation(id));
      }
   }

   public void updateFrame()
   {
      markerFrame.update();
      objectFrame.update();
   }

   public void computeObjectPose(FramePose3DBasics markerPose)
   {
      objectPose.set(markerPose);
      objectPose.appendTransform(objectToMarker);
      if (hasAppendix)
      {
         appendixPose.set(objectPose);
         appendixPose.appendTransform(appendixToObject);
      }
   }

   public RigidBodyTransform getMarkerToWorld()
   {
      return markerToWorld;
   }

   public ReferenceFrame getMarkerFrame()
   {
      return markerFrame;
   }

   public ReferenceFrame getObjectFrame()
   {
      return objectFrame;
   }

   public FramePose3D getObjectPose()
   {
      return objectPose;
   }

   public FramePose3D getAppendixPose()
   {
      return appendixPose;
   }
}
