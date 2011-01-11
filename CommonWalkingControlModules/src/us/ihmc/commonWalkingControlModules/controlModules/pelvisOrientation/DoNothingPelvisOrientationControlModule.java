package us.ihmc.commonWalkingControlModules.controlModules.pelvisOrientation;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.PelvisOrientationControlModule;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.Orientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class DoNothingPelvisOrientationControlModule implements PelvisOrientationControlModule
{
   public DoNothingPelvisOrientationControlModule(ReferenceFrame pelvisFrame)
   {
      this.pelvisFrame = pelvisFrame;
   }

   private final ReferenceFrame pelvisFrame;

   public FrameVector computePelvisTorque(RobotSide supportLeg, Orientation desiredPelvisOrientation)
   {
      return new FrameVector(pelvisFrame);
   }

}
