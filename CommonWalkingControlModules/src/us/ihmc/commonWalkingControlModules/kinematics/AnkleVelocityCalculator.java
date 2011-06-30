package us.ihmc.commonWalkingControlModules.kinematics;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointVelocities;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.RobotSpecificJointNames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.Twist;

public class AnkleVelocityCalculator
{
   private final ProcessedSensorsInterface processedSensors;
   private final RobotSide robotSide;
   private final SwingFullLegJacobian swingLegJacobian;
   private final LegJointName[] legJointNames;

   public AnkleVelocityCalculator(RobotSpecificJointNames robotJointNames, ProcessedSensorsInterface processedSensors, RobotSide robotSide,
                                  SwingFullLegJacobian swingLegJacobian)
   {
      this.legJointNames = robotJointNames.getLegJointNames();

      this.processedSensors = processedSensors;
      this.robotSide = robotSide;
      this.swingLegJacobian = swingLegJacobian;
   }

   public FrameVector getAnkleVelocityInWorldFrame()
   {
      swingLegJacobian.computeJacobian();    // TODO: doing this twice, but ok.
      LegJointVelocities legJointVelocities = getLegJointVelocities();
      Twist twistOfFootWithRespectToBody = swingLegJacobian.getTwistOfFootWithRespectToPelvisInFootFrame(legJointVelocities);

      Twist twistOfFootWithRespectToWorld = processedSensors.getTwistOfPelvisWithRespectToWorld();    // twist of pelvis w.r.t. world at this point
      final ReferenceFrame footFrame = twistOfFootWithRespectToBody.getExpressedInFrame();
      twistOfFootWithRespectToWorld.changeFrame(footFrame);
      twistOfFootWithRespectToWorld.add(twistOfFootWithRespectToBody);    // twist of foot w.r.t. world at this point

      Vector3d linearVelocity = twistOfFootWithRespectToWorld.getLinearPartCopy();    // linear velocity of ankle origin w.r.t. world, in foot frame
      final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      Transform3D transform_foot_world = footFrame.getTransformToDesiredFrame(worldFrame);
      transform_foot_world.transform(linearVelocity);    // linear velocity of ankle origin w.r.t. world, in world frame

      return new FrameVector(worldFrame, linearVelocity);
   }

   private LegJointVelocities getLegJointVelocities()
   {
      LegJointVelocities ret = new LegJointVelocities(legJointNames, robotSide);
      for (LegJointName legJointName : legJointNames)
      {
         ret.setJointVelocity(legJointName, processedSensors.getLegJointVelocity(robotSide, legJointName));
      }
      return ret;
   }
}
