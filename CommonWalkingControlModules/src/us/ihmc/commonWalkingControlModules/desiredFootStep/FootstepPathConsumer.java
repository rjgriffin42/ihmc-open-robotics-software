package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.trajectories.SimpleTwoWaypointTrajectoryParameters;
import us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointTrajectoryUtils;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.net.ObjectConsumer;

import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParameters;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryWaypointGenerationMethod;

/**
 * User: Matt
 * Date: 1/18/13
 */
public class FootstepPathConsumer implements ObjectConsumer<FootstepDataList>
{
   private boolean DEBUG = false;
   private FootstepPathCoordinator footstepPathCoordinator;
   private final SideDependentList<? extends ContactablePlaneBody> bipedFeet;
   private final HashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters;

   public FootstepPathConsumer(SideDependentList<? extends ContactablePlaneBody> bipedFeet, FootstepPathCoordinator footstepPathCoordinator,
                               HashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters)
   {
      this.footstepPathCoordinator = footstepPathCoordinator;
      this.bipedFeet = bipedFeet;
      this.mapFromFootstepsToTrajectoryParameters = mapFromFootstepsToTrajectoryParameters;
   }

   public void consumeObject(FootstepDataList footstepList)
   {
      ArrayList<Footstep> footsteps = new ArrayList<Footstep>();
      
      TrajectoryParameters trajectoryParameters = null;
      if(footstepList.getTrajectoryWaypointGenerationMethod() != null)
      {
         if(footstepList.getTrajectoryWaypointGenerationMethod() == TrajectoryWaypointGenerationMethod.BY_BOX)
         {
            trajectoryParameters = new SimpleTwoWaypointTrajectoryParameters(TwoWaypointTrajectoryUtils.getFlatBoxFromSquare(footstepList.getTrajectoryBoxData()));
         }
         else
         {
            trajectoryParameters = new SimpleTwoWaypointTrajectoryParameters(footstepList.getTrajectoryWaypointGenerationMethod());
         }
      }
      else
         System.out.println("trajectory parameters are null");
      
      for (int i = 0; i < footstepList.size(); i++)
      {
         FootstepData footstepData = footstepList.get(i);
         String id = "footstep_" + i;
         
         ContactablePlaneBody contactableBody = bipedFeet.get(footstepData.getRobotSide());

         FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
         PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
         ReferenceFrame soleReferenceFrame = FootstepUtils.createSoleFrame(footstepPoseFrame, contactableBody);

         
         FootstepUtils.getContactPointsInFrame(contactableBody, soleReferenceFrame);
         List<FramePoint> expectedContactPoints = FootstepUtils.getContactPointsInFrame(contactableBody, soleReferenceFrame);
         

         Footstep footstep = new Footstep(id, contactableBody, footstepPoseFrame, soleReferenceFrame, expectedContactPoints, true);
         
         footsteps.add(footstep);
         mapFromFootstepsToTrajectoryParameters.put(footstep, trajectoryParameters);

         if (DEBUG)
         {
            System.out.println("FootstepPathConsumer received " + footstep);
         }
      }
      
      footstepPathCoordinator.setSwingTime(footstepList.swingTime);
      footstepPathCoordinator.setTransferTime(footstepList.transferTime);
      footstepPathCoordinator.updatePath(footsteps);
   }

}