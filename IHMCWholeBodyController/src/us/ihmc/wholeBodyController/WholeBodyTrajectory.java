package us.ihmc.wholeBodyController;

import java.util.HashMap;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.wholebody.WholeBodyTrajectoryPacket;
import us.ihmc.communication.packets.wholebody.WholeBodyTrajectoryPacket.WholeBodyPose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.trajectory.TrajectoryND;
import us.ihmc.utilities.trajectory.TrajectoryND.WaypointND;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.ControlledDoF;


public class WholeBodyTrajectory
{
   static public TrajectoryND createJointSpaceTrajectory(
         final WholeBodyIkSolver wbSolver,  
         final OneDoFJoint[] initialState, 
         final OneDoFJoint[] finalState ) throws Exception
   {
      int N = initialState.length;

      double[] deltaQ = new double[N];
      double[] wpQ = new double[N];

      double max = -1e10;
 
      for (int j=0; j<N; j++)
      {
         OneDoFJoint jointI = initialState[j];
         OneDoFJoint jointF = finalState[j];
         deltaQ[j] = jointF.getQ() - jointI.getQ();

         if(max < Math.abs(deltaQ[j])) {
            max = Math.abs(deltaQ[j]);
         }
      }

      double maxDeltaQ = 0.2;

      int segments = (int) Math.round(max/maxDeltaQ);

      TrajectoryND wb_trajectory = new TrajectoryND(N, 0.5, 10 );

      for (int s=0; s <= segments; s++ )
      {
         for (int j=0; j<N; j++)
         {
            OneDoFJoint jointI = initialState[j];
            wpQ[j] = jointI.getQ() + s*( deltaQ[j] / segments);
         }
         wb_trajectory.addWaypoint(wpQ);
      }

      wb_trajectory.buildTrajectory();
      return wb_trajectory;
   }

   
   static public TrajectoryND createTaskSpaceTrajectory(
         final WholeBodyIkSolver wbSolver,  
         final SDFFullRobotModel actualRobotModel,
         final SideDependentList<ReferenceFrame> initialTarget,
         final SideDependentList<ReferenceFrame> finalTarget ) throws Exception
   {
      int N = actualRobotModel.getOneDoFJoints().length;

      double[] wpQ = new double[N];

      HashMap<String, Double> anglesToUseAsInitialState = new HashMap<String, Double> ();
      HashMap<String, Double> outputAngles = new HashMap<String, Double> ();
    
      for (int j=0; j<N; j++)
      {
         OneDoFJoint joint = actualRobotModel.getOneDoFJoints()[j];    
         anglesToUseAsInitialState.put(joint.getName() , joint.getQ() );
      }

      int segmentsPos = 1;
      int segmentsRot = 1;
      
      double maxDeltaPos = 0.2;
      double maxDeltaRot = 0.3;
      
      for (RobotSide side: RobotSide.values)
      {
         ControlledDoF numberOfDoF = wbSolver.getNumberOfControlledDoF(side);
         
         if( numberOfDoF != ControlledDoF.DOF_NONE )
         {
            double distance = RigidBodyTransform.getRotationDifference(
                  initialTarget.get(side).getTransformToWorldFrame(), 
                  finalTarget.get(side).getTransformToWorldFrame() ).length();
                  
            segmentsPos = (int) Math.max(segmentsPos, Math.round( distance / maxDeltaPos) );
         }
         if( numberOfDoF == ControlledDoF.DOF_3P2R || numberOfDoF ==  ControlledDoF.DOF_3P2R )
         {
            double distance = RigidBodyTransform.getTranslationDifference(
                  initialTarget.get(side).getTransformToWorldFrame(), 
                  finalTarget.get(side).getTransformToWorldFrame() ).length();
            
            segmentsRot = (int)  Math.max(segmentsPos, Math.round( distance/ maxDeltaRot) );
         }
      }

      int numSegments = Math.max(segmentsRot, segmentsPos);

      TrajectoryND wb_trajectory = new TrajectoryND(N, 0.5, 10 );
      
      for (int s=0; s <= numSegments; s++ )
      {
         for (RobotSide side: RobotSide.values)
         {
            RigidBodyTransform initialTransform =  initialTarget.get(side).getTransformToWorldFrame();
            RigidBodyTransform finalTransform   =  finalTarget.get(side).getTransformToWorldFrame();
            
            RigidBodyTransform interpolatedTransform = new RigidBodyTransform();
            
            interpolatedTransform.interpolate(initialTransform, finalTransform, ((double)s)/numSegments);
                  
            ReferenceFrame target =  ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("interpolatedTarget",
                  ReferenceFrame.getWorldFrame(), interpolatedTransform );
            
            wbSolver.setHandTarget(actualRobotModel,  side,target );
         }
         
         wbSolver.compute(actualRobotModel,  anglesToUseAsInitialState, outputAngles);
         
         for (int j=0; j<N; j++)
         {
            OneDoFJoint joint = actualRobotModel.getOneDoFJoints()[j];
            wpQ[j] = outputAngles.get( joint.getName() );
            // this solution is used in the next loop
            anglesToUseAsInitialState.put(joint.getName(), wpQ[j]);
         }

         wb_trajectory.addWaypoint(wpQ);
      }

      wb_trajectory.buildTrajectory();

      return wb_trajectory;
   }



   static public WholeBodyTrajectoryPacket convertTrajectoryToPacket(
         SDFFullRobotModel model,
         TrajectoryND wbTrajectory)
   {
      // build the trajectory packet from the new waypoints

      int numJointsPerArm = model.armJointIDsList.get(RobotSide.LEFT).size();
      int numWaypoints = wbTrajectory.getNumWasypoints();
      int numJoints = wbTrajectory.getNumDimensions();


      ReferenceFrame fixedFoot = model.getSoleFrame(RobotSide.RIGHT);

      WholeBodyTrajectoryPacket packet = new WholeBodyTrajectoryPacket(numWaypoints,numJointsPerArm);

      OneDoFJoint[] oneDoFJoints = model.getOneDoFJoints();

      for (int w=0; w<numWaypoints; w++ )
      {
         WaypointND    jointsWaypoint = wbTrajectory.getWaypoint(w);
         WholeBodyPose packetWaypoint = packet.waypoints[w];

         //just take the first one, they are suppose to be all the same.
         packetWaypoint.absTime = jointsWaypoint.absTime;

         int J = 0;
         // TODO: check the order.

         for (RobotSide side: RobotSide.values)
         {
            for(OneDoFJoint joint: model.armJointIDsList.get(side) )
            {
               String jointName = joint.getName();
               int index = -1;
               for(index=0; index< numJoints; index++ )
               {
                  if( oneDoFJoints[index].getName().equals(jointName) ) break;
               } 
               if( side.equals( RobotSide.LEFT))
               {
                  packetWaypoint.leftArmJointAngle[J] = jointsWaypoint.position[index];
                  packetWaypoint.leftArmJointAngle[J] = jointsWaypoint.velocity[index];
               }
               else{
                  packetWaypoint.rightArmJointAngle[J]    = jointsWaypoint.position[index];
                  packetWaypoint.rightArmJointVelocity[J] = jointsWaypoint.velocity[index];
               }
               J++;
            }
         }      

         RigidBodyTransform[] worldToPelvis = new RigidBodyTransform[2];
         RigidBodyTransform[] worldToChest = new RigidBodyTransform[2];

         final double dT = 0.0001;
         
         for (int i=0; i< 2; i++)
         {
            worldToPelvis[i] = new RigidBodyTransform();
            worldToChest[i]  = new RigidBodyTransform();

            for (int j=0; j<numJoints; j++  )
            {
               double jointAngle =  jointsWaypoint.position[j];
               
               if( i==1 )
               {
                  jointAngle += dT*jointsWaypoint.velocity[j];
               }
               model.getOneDoFJoints()[j].setQ( jointAngle );
            }
            model.updateFrames();

            ReferenceFrame pelvis    = model.getRootJoint().getFrameAfterJoint();
            ReferenceFrame movedFoot = model.getSoleFrame(RobotSide.RIGHT);
            ReferenceFrame chest     = model.getChest().getBodyFixedFrame();

            RigidBodyTransform worldToFixed = fixedFoot.getTransformToWorldFrame();
            RigidBodyTransform movedToPelvis = pelvis.getTransformToDesiredFrame(movedFoot);

            worldToPelvis[i].multiply( worldToFixed, movedToPelvis);

            RigidBodyTransform pelvisToChest = chest.getTransformToDesiredFrame( pelvis );
            worldToChest[i].multiply( worldToPelvis[i], pelvisToChest);
         }
         
         Vector3d positionA = new Vector3d();
         Vector3d positionB = new Vector3d();
         
         worldToPelvis[0].getTranslation(positionA);
         worldToPelvis[1].getTranslation(positionB);
         
         packetWaypoint.pelvisVelocity.x = (positionB.x - positionA.x)/dT;
         packetWaypoint.pelvisVelocity.y = (positionB.y - positionA.y)/dT;
         packetWaypoint.pelvisVelocity.z = (positionB.z - positionA.z)/dT;

         worldToPelvis[0].get( packetWaypoint.pelvisOrientation,  packetWaypoint.pelvisPosition);
         worldToChest[0].get( packetWaypoint.chestOrientation );

      }
      return null;
   }
}
