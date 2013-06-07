package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.driving;

import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

/**
 * @author twan
 *         Date: 5/29/13
 */
public interface VehicleModelObjects
{
   FramePose getFramePose(ReferenceFrame vehicleReferenceFrame, VehicleObject vehicleObject);

   Transform3D getTransform(VehicleObject vehicleObject);

   double getHandBrakeEngagedAngle();

   double getHandBrakeDisengagedAngle();

   Vector3d getHandBrakeAxis();

   double getSteeringWheelOuterRadius();

   double getSteeringWheelInnerRadius();
}
