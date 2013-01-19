package us.ihmc.commonWalkingControlModules.controlModules;

import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;


/**
 * This class resolves where the Center of Pressure is for a given wrench and plane.
 * If the z component of the force is close to zero, then the returned CoP will be
 * just at the origin of the passed in referenceFrame, and there still may be components of 
 * moment in the plane.
 * 
 * The Center of Pressure is the point at which components of moment in the plane become zero.
 * It can be viewed as the point in which the wrench could be resisted by a universal joint
 * (freely spinning bearing on the x axis and y axis of the given plane).
 * Since the Center of Pressure requires a plane, it makes sense for flat ground, or situations in which
 * the plane is obvious. However, for multi-contact, it may not make as much sense to pick a plane.
 * The plane may be arbitrary, except perhaps for a flat plane, in which the surface normal is aligned with
 * the gravity vector.
 */
public class CenterOfPressureResolver
{
   private final SpatialForceVector wrenchResolvedOnPlane = new SpatialForceVector();
   private final Vector3d torqueAtZeroInPlaneFrame = new Vector3d();
   private final Vector3d forceInPlaneFrame = new Vector3d();
   
   public double resolveCenterOfPressureAndNormalTorque(FramePoint2d centerOfPressureToPack, SpatialForceVector spatialForceVector, ReferenceFrame centerOfPressurePlaneFrame)
   {
      // First resolve the wrench at the plane origin:
      wrenchResolvedOnPlane.set(spatialForceVector);
      wrenchResolvedOnPlane.changeFrame(centerOfPressurePlaneFrame);
      
      wrenchResolvedOnPlane.packAngularPart(torqueAtZeroInPlaneFrame);
      wrenchResolvedOnPlane.packLinearPart(forceInPlaneFrame);
            
      double fz = forceInPlaneFrame.getZ();
      
      double vector12x = 0.0;
      double vector12y = 0.0;
      
      if (Math.abs(fz) > 1e-7)
      {
         vector12x = -1.0/fz * torqueAtZeroInPlaneFrame.getY();
         vector12y = 1.0/fz * torqueAtZeroInPlaneFrame.getX(); 
      }
  
      double normalTorqueAtCenterOfPressure = torqueAtZeroInPlaneFrame.getZ() - vector12x * forceInPlaneFrame.getY() + vector12y * forceInPlaneFrame.getX();
      
      centerOfPressureToPack.set(centerOfPressurePlaneFrame, vector12x, vector12y);
      return normalTorqueAtCenterOfPressure;
   }
  
}
