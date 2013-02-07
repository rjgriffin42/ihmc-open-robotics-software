package us.ihmc.commonWalkingControlModules.kinematics;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RevoluteJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;

public class FootTwistCalculator
{
   private final ProcessedSensorsInterface processedSensors;
   private final RobotSide robotSide;
   private final FullRobotModel fullRobotModel;
   private final Twist tempTwist = new Twist();
   private final RigidBody pelvis;
   private final ReferenceFrame footFrame;

   public FootTwistCalculator(RobotSide robotSide, ProcessedSensorsInterface processedSensors)
   {
      this.robotSide = robotSide;
      this.processedSensors = processedSensors;
      this.fullRobotModel = processedSensors.getFullRobotModel();
      this.pelvis = fullRobotModel.getPelvis();
      this.footFrame = fullRobotModel.getFoot(robotSide).getParentJoint().getFrameAfterJoint();
   }

   public Twist computeFootTwist()
   {
      Twist ret = processedSensors.getTwistOfPelvisWithRespectToWorld();
      ret.changeBodyFrameNoRelativeTwist(pelvis.getBodyFixedFrame());

      for (LegJointName legJointName : fullRobotModel.getRobotSpecificJointNames().getLegJointNames())
      {
         OneDoFJoint joint = fullRobotModel.getLegJoint(robotSide, legJointName);
         joint.packSuccessorTwist(tempTwist);
         ret.changeFrame(tempTwist.getExpressedInFrame());
         ret.add(tempTwist);
      }

      ret.changeBodyFrameNoRelativeTwist(footFrame);
      ret.changeFrame(footFrame);

      return ret;
   }
}
