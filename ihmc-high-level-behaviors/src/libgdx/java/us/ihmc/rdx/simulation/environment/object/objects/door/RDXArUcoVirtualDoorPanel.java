package us.ihmc.rdx.simulation.environment.object.objects.door;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.yawPitchRoll.YawPitchRoll;
import us.ihmc.perception.OpenCVArUcoMarker;
import us.ihmc.robotics.referenceFrames.ReferenceFrameMissingTools;

public class RDXArUcoVirtualDoorPanel extends RDXVirtualGhostObject
{

   private final OpenCVArUcoMarker arUcoMarker;
   private final RigidBodyTransform markerToWorld = new RigidBodyTransform();
   private final ReferenceFrame markerFrame = ReferenceFrameMissingTools.constructFrameWithChangingTransformToParent(ReferenceFrame.getWorldFrame(),
                                                                                                                     markerToWorld);
   private final RigidBodyTransform transformToMarker = new RigidBodyTransform();
   private final ReferenceFrame virtualFrame;

   public RDXArUcoVirtualDoorPanel(int id, String name)
   {
      super("environmentObjects/door/doorPanel/DoorPanel.g3dj");

      arUcoMarker = new OpenCVArUcoMarker(id, 0.2032);

      if (id == 0) // pull door
      {
         double x = 0.0;
         double y = -0.678702;
         double z = 1.14141;
         transformToMarker.set(new YawPitchRoll(Math.toRadians(180.0), 0.0, Math.toRadians(180.0)), new Point3D(x, y, z));
      }
      else if (id == 1) // push door
      {
         double x = 0.0;
         double y = 0.678702;
         double z = 1.14141;
         transformToMarker.set(new YawPitchRoll(0.0, 0.0, Math.toRadians(180.0)), new Point3D(x, y, z));
      }

      virtualFrame = ReferenceFrameTools.constructFrameWithChangingTransformToParent(String.format("%s%dPanel", name, id), markerFrame, transformToMarker);
   }

   @Override
   public void update()
   {
      markerFrame.update();

      virtualFrame.getTransformToDesiredFrame(getTransformToParent(), ReferenceFrame.getWorldFrame());

      super.update();
   }

   public RigidBodyTransform getMarkerToWorld()
   {
      return markerToWorld;
   }

   public OpenCVArUcoMarker getArUcoMarker()
   {
      return arUcoMarker;
   }

   public ReferenceFrame getVirtualFrame()
   {
      return virtualFrame;
   }
}
