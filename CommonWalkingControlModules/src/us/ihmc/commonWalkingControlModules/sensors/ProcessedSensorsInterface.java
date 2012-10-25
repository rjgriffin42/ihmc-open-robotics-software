package us.ihmc.commonWalkingControlModules.sensors;

import java.util.EnumMap;
import java.util.HashMap;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CenterOfMassControlType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.FingerName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.NeckJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.SpineJointName;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.ExternalForcePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public interface ProcessedSensorsInterface
{
   public abstract double getTime();
   public abstract DoubleYoVariable getYoTime();
   
   public abstract double getKneeAngle(RobotSide robotSide);

   public abstract FramePoint getCenterOfMassPositionInFrame(ReferenceFrame referenceFrame);
   public abstract FramePoint getCenterOfMassPositionInFrame(ReferenceFrame referenceFrame, CenterOfMassControlType centerOfMassControlType);

   public abstract FrameVector getGravityInWorldFrame();

   public abstract Twist getTwistOfPelvisWithRespectToWorld();
   public abstract SpatialAccelerationVector getAccelerationOfPelvisWithRespectToWorld();

   public abstract FrameOrientation getPelvisOrientationInFrame(ReferenceFrame referenceFrame);

   public abstract FramePoint getCenterOfMassGroundProjectionInFrame(ReferenceFrame referenceFrame);

   public abstract double getLegJointPosition(RobotSide robotSide, LegJointName legJointName);
   public abstract double getLegJointVelocity(RobotSide robotSide, LegJointName legJointName);

   public abstract double getArmJointPosition(RobotSide robotSide, ArmJointName legJointName);
   public abstract double getArmJointVelocity(RobotSide robotSide, ArmJointName legJointName);


   public abstract double getTotalMass();
   public abstract double getTotalMass(CenterOfMassControlType centerOfMassControlType);
   
   public abstract String getLegJointPositionName(RobotSide robotSide, LegJointName jointName);

   public abstract String getLegJointVelocityName(RobotSide robotSide, LegJointName jointName);

   public abstract FrameOrientation getChestOrientationInFrame(ReferenceFrame desiredHeadingFrame);
   public abstract FrameVector getChestAngularVelocityInChestFrame();

   public abstract double getSpineJointPosition(SpineJointName spineJointName);
   public abstract double getSpineJointVelocity(SpineJointName spineJointName);

   public abstract double getNeckJointPosition(NeckJointName neckJointName);
   public abstract double getNeckJointVelocity(NeckJointName neckJointName);
   public abstract FullRobotModel getFullRobotModel();
   
   public abstract FrameVector getBodyVelocity();
   public abstract FrameVector getCenterOfMassVelocityInFrame(ReferenceFrame referenceFrame);
   public abstract FrameVector getCenterOfMassVelocityInFrame(ReferenceFrame referenceFrame, CenterOfMassControlType centerOfMassControlType);
   
   public abstract FramePoint getCentroidalMomentPivotInFrame(ReferenceFrame referenceFrame);
   public abstract FrameVector getAngularMomentumInFrame(ReferenceFrame midFeetZUp);
   
   public abstract HashMap<FramePoint2d, Boolean> getContactMap(RobotSide robotSide);
   
   public abstract FrameVector getFingerForce(RobotSide robotSide, FingerName fingerName);
}
