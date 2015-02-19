package us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator;

import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.utilities.humanoidRobot.footstep.Footstep;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import javax.vecmath.Point2d;
import java.util.List;

/**
 * Created by agrabertilton on 2/18/15.
 */
public class FootstepTools
{
   public static Footstep generateFootstepFromFootstepData(FootstepData footstepData, ContactablePlaneBody contactableBody, int index)
   {
      String id = "footstep_" + index;
      FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
      PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
      List<Point2d> contactPoints = footstepData.getPredictedContactPoints();
      if ((contactPoints != null) && (contactPoints.size() == 0))
         throw new RuntimeException(
             "Cannot have an empty list of contact points in FootstepData. Should be null to use the default controller contact points.");
      Footstep footstep = new Footstep(id, contactableBody.getRigidBody(), footstepData.getRobotSide(), contactableBody.getSoleFrame(), footstepPoseFrame,
                                       true, contactPoints);
      footstep.trajectoryType = footstepData.getTrajectoryType();
      footstep.swingHeight = footstepData.swingHeight;

      return footstep;
   }

   public static Footstep generateFootstepFromFootstepData(FootstepData footstepData, ContactablePlaneBody contactableBody)
   {
      FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
      PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
      List<Point2d> contactPoints = footstepData.getPredictedContactPoints();
      if ((contactPoints != null) && (contactPoints.size() == 0))
         throw new RuntimeException(
             "Cannot have an empty list of contact points in FootstepData. Should be null to use the default controller contact points.");
      Footstep footstep = new Footstep(contactableBody.getRigidBody(), footstepData.getRobotSide(), contactableBody.getSoleFrame(), footstepPoseFrame, true,
                                       contactPoints);
      footstep.trajectoryType = footstepData.getTrajectoryType();
      footstep.swingHeight = footstepData.swingHeight;

      return footstep;
   }
}
