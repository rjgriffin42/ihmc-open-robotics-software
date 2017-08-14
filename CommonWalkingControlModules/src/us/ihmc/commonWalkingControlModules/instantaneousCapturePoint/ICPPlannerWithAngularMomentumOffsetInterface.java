package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import us.ihmc.commonWalkingControlModules.configurations.ICPAngularMomentumModifierParameters;
import us.ihmc.commonWalkingControlModules.configurations.ICPPlannerParameters;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.robotSide.RobotSide;

public interface ICPPlannerWithAngularMomentumOffsetInterface extends ICPPlannerWithTimeFreezerInterface
{
   void modifyDesiredICPForAngularMomentum(FramePoint3D copEstimate, RobotSide supportSide);

   void initializeParameters(ICPPlannerParameters plannerParameters, ICPAngularMomentumModifierParameters angularMomentumModifierParameters);
}
