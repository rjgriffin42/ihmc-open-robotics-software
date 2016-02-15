package us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator;

import java.util.List;

import javax.vecmath.Point2d;

import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.io.printing.PrintTools;

public class FootstepTools
{
   public static Footstep generateFootstepFromFootstepData(FootstepDataMessage footstepData, ContactablePlaneBody contactableBody, int index)
   {
      String id = "footstep_" + index;
      FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
      PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
      List<Point2d> contactPoints = footstepData.getPredictedContactPoints();
      if ((contactPoints != null) && (contactPoints.size() == 0))
      {
         contactPoints = null;
         emptyContactPointListErrorMessage();
      }
      Footstep footstep = new Footstep(id, contactableBody.getRigidBody(), footstepData.getRobotSide(), contactableBody.getSoleFrame(), footstepPoseFrame, true,
            contactPoints);
      footstep.trajectoryType = footstepData.getTrajectoryType();
      footstep.swingHeight = footstepData.swingHeight;

      return footstep;
   }

   public static Footstep generateFootstepFromFootstepData(FootstepDataMessage footstepData, ContactablePlaneBody contactableBody)
   {
      FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
      PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
      List<Point2d> contactPoints = footstepData.getPredictedContactPoints();
      if ((contactPoints != null) && (contactPoints.size() == 0))
      {
         contactPoints = null;
         emptyContactPointListErrorMessage();
      }
      Footstep footstep = new Footstep(contactableBody.getRigidBody(), footstepData.getRobotSide(), contactableBody.getSoleFrame(), footstepPoseFrame, true,
            contactPoints);
      footstep.trajectoryType = footstepData.getTrajectoryType();
      footstep.swingHeight = footstepData.swingHeight;

      return footstep;
   }

   public static Footstep generateFootstepFromFootstepDataSole(FootstepDataMessage footstepData, ContactablePlaneBody contactableBody)
   {
      FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
      PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
      List<Point2d> contactPoints = footstepData.getPredictedContactPoints();
      if ((contactPoints != null) && (contactPoints.size() == 0))
      {
         contactPoints = null;
         emptyContactPointListErrorMessage();
      }
      Footstep footstep = new Footstep(contactableBody.getRigidBody(), footstepData.getRobotSide(), contactableBody.getSoleFrame(), footstepPoseFrame, true,
            contactPoints);
      footstep.trajectoryType = footstepData.getTrajectoryType();
      footstep.swingHeight = footstepData.swingHeight;
      footstep.setSolePose(footstepPose);
      return footstep;
   }

   private static void emptyContactPointListErrorMessage()
   {
      PrintTools.error(FootstepTools.class, "Should not have an empty list of contact points in FootstepData."
            + "Should be null to use the default controller contact points. Setting it to null");
   }
}
