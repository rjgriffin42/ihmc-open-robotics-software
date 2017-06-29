package us.ihmc.robotics.geometry.shapes;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class FrameEllipsoid3d extends FrameShape3d<FrameEllipsoid3d, Ellipsoid3D>
{
   private final Ellipsoid3D ellipsoid;

   public FrameEllipsoid3d(FrameEllipsoid3d other)
   {
      this(other.referenceFrame, other.ellipsoid);
   }

   public FrameEllipsoid3d(ReferenceFrame referenceFrame, Ellipsoid3D ellipsoid)
   {
      super(referenceFrame, new Ellipsoid3D(ellipsoid));
      this.ellipsoid = getGeometryObject();
   }

   public FrameEllipsoid3d(ReferenceFrame referenceFrame, double xRadius, double yRadius, double zRadius)
   {
      super(referenceFrame, new Ellipsoid3D(xRadius, yRadius, zRadius));
      ellipsoid = getGeometryObject();
   }
   
   public FrameEllipsoid3d(ReferenceFrame referenceFrame, double xRadius, double yRadius, double zRadius, RigidBodyTransform transform)
   {
      super(referenceFrame, new Ellipsoid3D(xRadius, yRadius, zRadius, transform));
      ellipsoid = getGeometryObject();
   }

   public Ellipsoid3D getEllipsoid3d()
   {
      return ellipsoid;
   }
}
