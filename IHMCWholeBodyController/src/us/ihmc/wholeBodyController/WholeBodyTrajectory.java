package us.ihmc.wholeBodyController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.wholebody.WholeBodyTrajectoryPacket;
import us.ihmc.utilities.math.Vector64F;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.InverseDynamicsJointStateCopier;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.trajectory.TrajectoryND;
import us.ihmc.utilities.trajectory.TrajectoryND.WaypointND;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.ComputeResult;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.ControlledDoF;
import us.ihmc.wholeBodyController.WholeBodyIkSolver.LockLevel;


public class WholeBodyTrajectory
{
   private double maxJointVelocity;
   private double maxJointAcceleration;
   private double maxDistanceInTaskSpaceBetweenWaypoints;
   private final SDFFullRobotModel currentRobotModel;
   private final HashMap<String,Integer> jointNameToTrajectoryIndex = new HashMap<String,Integer>();
   private final HashMap<String, Double> desiredJointAngles = new HashMap<String, Double> ();

   public WholeBodyTrajectory(SDFFullRobotModel fullRobotModel, double maxJointVelocity, double maxJointAcceleration, double maxDistanceInTaskSpaceBetweenWaypoints)
   {
      currentRobotModel = new SDFFullRobotModel( fullRobotModel );

      this.maxJointVelocity = maxJointVelocity;
      this.maxJointAcceleration = maxJointAcceleration;
      this.maxDistanceInTaskSpaceBetweenWaypoints = maxDistanceInTaskSpaceBetweenWaypoints;
   }

   public TrajectoryND createTaskSpaceTrajectory(
         final WholeBodyIkSolver wbSolver,  
         final SDFFullRobotModel initialRobotState,
         final SDFFullRobotModel finalRoboState
         ) throws Exception
   {
      int N = wbSolver.getNumberOfJoints();

      // TODO this is for debug purpose only
      maxDistanceInTaskSpaceBetweenWaypoints = 0.15;

      InverseDynamicsJointStateCopier copier = new InverseDynamicsJointStateCopier(
            initialRobotState.getElevator(), currentRobotModel.getElevator() );

      copier.copy();

      int oldMaxReseed = wbSolver.maxNumberOfAutomaticReseeds;
      wbSolver.maxNumberOfAutomaticReseeds = 0;

      SideDependentList<ControlledDoF> previousOption = new SideDependentList<ControlledDoF>();
      SideDependentList<RigidBodyTransform> initialTransform  = new SideDependentList<RigidBodyTransform>();
      SideDependentList<RigidBodyTransform> finalTransform    = new SideDependentList<RigidBodyTransform>();
      LockLevel previousLockLimit = wbSolver.getLockLevel();

      wbSolver.setLockLevel( LockLevel.LOCK_LEGS_AND_WAIST );

      for (RobotSide side: RobotSide.values)
      {
         ReferenceFrame initialTargetFrame = initialRobotState.getHandControlFrame( side );
         ReferenceFrame finalTargetFrame   = finalRoboState.getHandControlFrame( side );

         ReferenceFrame attachment = wbSolver.getDesiredGripperAttachmentFrame(side, ReferenceFrame.getWorldFrame() );
         ReferenceFrame palm       = wbSolver.getDesiredGripperPalmFrame(side, ReferenceFrame.getWorldFrame() );
         RigidBodyTransform attachmentToPalm = palm.getTransformToDesiredFrame( attachment );

         previousOption.set( side, wbSolver.getNumberOfControlledDoF(side) );

         RigidBodyTransform transform =  initialTargetFrame.getTransformToWorldFrame();
         transform.multiply( attachmentToPalm );
         initialTransform.set( side, transform );

         transform =  finalTargetFrame.getTransformToWorldFrame();
         transform.multiply( attachmentToPalm );
         finalTransform.set( side, transform );

         if( previousOption.get(side) != ControlledDoF.DOF_NONE)
         {
            wbSolver.setNumberOfControlledDoF(side, ControlledDoF.DOF_3P3R ); 
         }
      }

      ArrayList<String> jointNames = new ArrayList<String>();

      int counter = 0;
      for (OneDoFJoint joint: initialRobotState.getOneDoFJoints() )
      {
         if( wbSolver.hasJoint( joint.getName())  )
         {
            jointNameToTrajectoryIndex.put( joint.getName(),counter);
            counter++;
            
            jointNames.add( joint.getName() );
         }    
      }

      int segmentsPos = 1;
      int segmentsRot = 1;

      double maxDeltaPos = maxDistanceInTaskSpaceBetweenWaypoints;

      for (RobotSide side: RobotSide.values)
      {
         ControlledDoF numberOfDoF = wbSolver.getNumberOfControlledDoF(side);

         if( numberOfDoF != ControlledDoF.DOF_NONE )
         {
            double distance = RigidBodyTransform.getTranslationDifference(
                  initialTransform.get(side), 
                  finalTransform.get(side) ).length();

            segmentsPos = (int) Math.max(segmentsPos, Math.round( distance / maxDeltaPos) );
         }
      }

      int numSegments = Math.max(segmentsRot, segmentsPos);

      //numSegments = 6;

      Vector64F thisWaypointAngles = new Vector64F(N);
      HashMap<String,Double> thisWaypointAnglesByName = new HashMap<String,Double>();

      for ( Map.Entry<String, Integer> entry: jointNameToTrajectoryIndex.entrySet() )
      {
         String jointName = entry.getKey();
         int index = entry.getValue();
         double initialAngle = initialRobotState.getOneDoFJointByName(jointName).getQ();
         thisWaypointAngles.set( index, initialAngle );  
         thisWaypointAnglesByName.put( jointName, initialAngle );   
      }

      TrajectoryND wb_trajectory = new TrajectoryND(N, maxJointVelocity,  maxJointAcceleration );

      wb_trajectory.addNames( jointNames );

      for (int s=0; s <= numSegments; s++ )
      {
         if( s > 0  )
         {
            for ( Map.Entry<String, Integer> entry: jointNameToTrajectoryIndex.entrySet() )
            {
               String jointName = entry.getKey();
               int index = entry.getValue();

               double alpha = ((double)s) / (double) ( numSegments  ); 

               double initialAngle = initialRobotState.getOneDoFJointByName(jointName).getQ();
               double finalAngle   = finalRoboState.getOneDoFJointByName(jointName).getQ(); 
               double currentAngle = currentRobotModel.getOneDoFJointByName(jointName).getQ(); 

               //double interpolatedAngle = currentAngle*(1.0 -alpha) + finalAngle*alpha ;
               double interpolatedAngle = initialAngle*(1.0 -alpha) + finalAngle*alpha ;

               thisWaypointAngles.set(index,interpolatedAngle );
               thisWaypointAnglesByName.put( jointName, interpolatedAngle  );
            }

            currentRobotModel.updateJointsAngleButKeepOneFootFixed(thisWaypointAnglesByName, RobotSide.RIGHT);


            for (RobotSide side: RobotSide.values)
            {
               RigidBodyTransform interpolatedTransform = new RigidBodyTransform();

               double alpha = ((double)s)/(numSegments);
               interpolatedTransform.interpolate(initialTransform.get(side), finalTransform.get(side), alpha);

               FramePose target =  new FramePose( ReferenceFrame.getWorldFrame(), interpolatedTransform );

               wbSolver.setGripperPalmTarget( initialRobotState,  side, target );

               if( side == RobotSide.RIGHT )
               {
                  RigidBodyTransform targetTrans = new RigidBodyTransform();
                  target.getRigidBodyTransform( targetTrans );
               }
            }

            if( s < numSegments )
            {
               //---------------------------------
               ComputeResult ret =  wbSolver.compute(currentRobotModel,  thisWaypointAnglesByName, desiredJointAngles);
               //---------------------------------

               // note: use also the failed one that didn't converge... better than nothing.
               // if( ret != ComputeResult.FAILED_INVALID)
               if( ret == ComputeResult.SUCCEEDED)
               {
                  //  System.out.println(" SUCCEEDED ");
                  for ( Map.Entry<String, Integer> entry: jointNameToTrajectoryIndex.entrySet() )
                  {
                     String jointName = entry.getKey();
                     int index = entry.getValue();
                     Double angle = desiredJointAngles.get( jointName );
                     thisWaypointAngles.set(index, angle );
                     thisWaypointAnglesByName.put( jointName, angle );  
                  }
                  wb_trajectory.addWaypoint(thisWaypointAngles.data);
               }
               else{
                  /*  System.out.println("OOOOPS");
                 for ( Map.Entry<String, Integer> entry: nameToIndex.entrySet() )
                  {
                     String jointName = entry.getKey();
                     int index = entry.getValue();
                     double alpha = 1.0 / (double) ( numSegments + 1 -s ); 

                     double currentAngle = thisWaypointAnglesByName.get(jointName);
                     double finalAngle   = finalRoboState.getOneDoFJointByName(jointName).getQ();  

                     double interpolatedAngle = currentAngle*(1.0 -alpha) + finalAngle*alpha ;
                     thisWaypointAngles.set(index, interpolatedAngle );
                     thisWaypointAnglesByName.put( jointName, interpolatedAngle );  
                  }*/
               }
            }
         }
         if( s==0 || s==numSegments )
            wb_trajectory.addWaypoint(thisWaypointAngles.data);
      }

      for (RobotSide side: RobotSide.values)
      {
         wbSolver.setNumberOfControlledDoF(side, previousOption.get(side) ); 
      }

      wb_trajectory.buildTrajectory();

      wbSolver.maxNumberOfAutomaticReseeds = oldMaxReseed;

      wbSolver.setLockLevel( previousLockLimit );

      return wb_trajectory;
   }


   public WholeBodyTrajectoryPacket convertTrajectoryToPacket(
         TrajectoryND wbTrajectory)
   {
      int numJointsPerArm = currentRobotModel.armJointIDsList.get(RobotSide.LEFT).size();
      int numWaypoints    = wbTrajectory.getNumWaypoints();

      WholeBodyTrajectoryPacket packet = new WholeBodyTrajectoryPacket(numWaypoints,numJointsPerArm);

      double prevAbsoluteTime = 1.0;
      
      Vector3d temp = new Vector3d();
      
      for (int w=0; w < numWaypoints; w++ )
      {
         WaypointND    jointsWaypoint = wbTrajectory.getWaypoint(w);

         packet.timeSincePrevious[w] = jointsWaypoint.absTime - prevAbsoluteTime;
         prevAbsoluteTime = jointsWaypoint.absTime + 1.0;

         // store trajectories in the correct place

         //-----  check if this is part of the arms ---------
         packet.allocateArmTrajectory(RobotSide.LEFT);
         packet.allocateArmTrajectory(RobotSide.RIGHT);
         
         int J = 0 ;
         for(OneDoFJoint armJoint: currentRobotModel.armJointIDsList.get( RobotSide.LEFT) )
         {   
            int index = jointNameToTrajectoryIndex.get( armJoint.getName() );
            packet.leftArmJointAngle[J][w] = jointsWaypoint.position[index];
            packet.leftArmJointAngle[J][w] = jointsWaypoint.velocity[index];
         }

         J = 0 ;
         for(OneDoFJoint armJoint: currentRobotModel.armJointIDsList.get( RobotSide.RIGHT) )
         {   
            int index = jointNameToTrajectoryIndex.get( armJoint.getName() );
            packet.rightArmJointAngle[J][w] = jointsWaypoint.position[index];
            packet.rightArmJointAngle[J][w] = jointsWaypoint.velocity[index];
         }

         ///---------- calculation in task space ------------

         // store this before updating the model.
         ReferenceFrame fixedFoot = currentRobotModel.getSoleFrame(RobotSide.RIGHT);

         // first, we need to copy position and velocities in the SDFFullRobotModel
         for ( Map.Entry<String, Integer> entry: jointNameToTrajectoryIndex.entrySet() )
         {
            OneDoFJoint joint = currentRobotModel.getOneDoFJointByName( entry.getKey() );
            int index = entry.getValue();

            joint.setQ(  jointsWaypoint.position[index] );
            joint.setQd( jointsWaypoint.velocity[index] );
         }
         currentRobotModel.updateFrames();

         // align the foot as usually
         TwistCalculator twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(),  currentRobotModel.getElevator() );
         twistCalculator.compute();

         ReferenceFrame pelvis    = currentRobotModel.getRootJoint().getFrameAfterJoint();
         ReferenceFrame movedFoot = currentRobotModel.getSoleFrame(RobotSide.RIGHT);

         RigidBodyTransform worldToFoot   = fixedFoot.getTransformToWorldFrame();
         RigidBodyTransform footToPelvis  = pelvis.getTransformToDesiredFrame( movedFoot );
         RigidBodyTransform worldToPelvis = new RigidBodyTransform();
         worldToPelvis.multiply( worldToFoot, footToPelvis );

         currentRobotModel.getRootJoint().setPositionAndRotation(worldToPelvis);
         
         currentRobotModel.updateFrames();
         
         //-----  store pelvis data --------
         packet.allocatePelvisTrajectory();
         worldToPelvis.get( packet.pelvisWorldOrientation[w], temp );
         packet.pelvisWorldPosition[w].set( temp );
       
         Twist twistToPack = new Twist();
         twistCalculator.packTwistOfBody(twistToPack, currentRobotModel.getElevator() );
         
         packet.pelvisLinearVelocity[w].set(  twistToPack.getLinearPartCopy()  );
         packet.pelvisAngularVelocity[w].set( twistToPack.getAngularPartCopy() );
         
       //-----  store chest data --------
         packet.allocateChestTrajectory();
         RigidBodyTransform worldToChest =  currentRobotModel.getChest().getParentJoint().getFrameAfterJoint().getTransformToWorldFrame();
         worldToChest.get( packet.chestWorldOrientation[w] );
        
         twistCalculator.packTwistOfBody(twistToPack, currentRobotModel.getChest() );
         packet.chestAngularVelocity[w].set(  twistToPack.getAngularPartCopy() );
      }
     
      
      return packet;
   }
}
