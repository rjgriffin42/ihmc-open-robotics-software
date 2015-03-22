package us.ihmc.wholeBodyController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.wholebody.WholeBodyTrajectoryPacket;
import us.ihmc.utilities.math.MathTools;
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
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   
   private double maxJointVelocity;
   private double maxJointAcceleration;
   private double maxDistanceInTaskSpaceBetweenWaypoints;
   private final SDFFullRobotModel currentRobotModel;
   private final HashMap<String,Integer> jointNameToTrajectoryIndex = new HashMap<String,Integer>();
   private final HashMap<String, Double> desiredJointAngles = new HashMap<String, Double> ();
   private RigidBodyTransform worldToFoot; 
   private final SDFFullRobotModel fullRobotModel;

   public WholeBodyTrajectory(SDFFullRobotModel fullRobotModel, double maxJointVelocity, double maxJointAcceleration, double maxDistanceInTaskSpaceBetweenWaypoints)
   {
      currentRobotModel = new SDFFullRobotModel( fullRobotModel );
      this.fullRobotModel = fullRobotModel;

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
      wbSolver.setVerbosityLevel(1);
      int N = wbSolver.getNumberOfJoints();

      worldToFoot = initialRobotState.getSoleFrame(RobotSide.RIGHT).getTransformToWorldFrame();

      if( ! worldToFoot.epsilonEquals( finalRoboState.getSoleFrame(RobotSide.RIGHT).getTransformToWorldFrame() , 0.001) )
      {
         System.out.println("WholeBodyTrajectory: potential error. Check the root foot (RIGHT) pose of initial and final models");
      }


      InverseDynamicsJointStateCopier copier = new InverseDynamicsJointStateCopier(
            initialRobotState.getElevator(), currentRobotModel.getElevator() );

      copier.copy();
      currentRobotModel.updateFrames();
      

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

         ReferenceFrame attachment = wbSolver.getDesiredGripperAttachmentFrame(side, worldFrame );
         ReferenceFrame palm       = wbSolver.getDesiredGripperPalmFrame(side, worldFrame );
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
            wbSolver.setNumberOfControlledDoF(side, ControlledDoF.DOF_3P2R ); 
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

    /*  int segmentsPos = 1;
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
*/

      int numSegments = 1;

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

               if(  wbSolver.getNumberOfControlledDoF(side) != ControlledDoF.DOF_NONE )
                     System.out.println(">>>>>>>>>>> step:  " + s + "\n" +interpolatedTransform );
               
               FramePose target =  new FramePose( worldFrame, interpolatedTransform );

               wbSolver.setGripperPalmTarget( side, target );
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
                  System.out.println("OOOOPS");
                   /*  for ( Map.Entry<String, Integer> entry: nameToIndex.entrySet() )
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
         {
            wb_trajectory.addWaypoint(thisWaypointAngles.data);
         }
      }

      for (RobotSide side: RobotSide.values)
      {
         wbSolver.setNumberOfControlledDoF(side, previousOption.get(side) ); 
      }

      wb_trajectory.buildTrajectory();

      wbSolver.maxNumberOfAutomaticReseeds = oldMaxReseed;

      wbSolver.setLockLevel( previousLockLimit );

      // cleanup currentRobotModel
      copier.copy();
      currentRobotModel.updateFrames();

      return wb_trajectory;
   }


   public WholeBodyTrajectoryPacket convertTrajectoryToPacket(
         TrajectoryND wbTrajectory)
   {
      int numJointsPerArm = currentRobotModel.armJointIDsList.get(RobotSide.LEFT).size();
      int numWaypoints    = wbTrajectory.getNumWaypoints();

      WholeBodyTrajectoryPacket packet = new WholeBodyTrajectoryPacket(numWaypoints ,numJointsPerArm);

      Vector3d temp = new Vector3d();

      for (int w=0; w < numWaypoints; w++ )
      {
         WaypointND    jointsWaypoint = wbTrajectory.getWaypoint(w);
                
         packet.timeAtWaypoint[w] = jointsWaypoint.absTime + 0.2;

         //-----  check if this is part of the arms ---------

         int J = 0 ;
         for(OneDoFJoint armJoint: currentRobotModel.armJointIDsList.get( RobotSide.LEFT) )
         {   
            int index = jointNameToTrajectoryIndex.get( armJoint.getName() );
            packet.leftArmJointAngle[J][w]    = jointsWaypoint.position[index];
            packet.leftArmJointVelocity[J][w] = jointsWaypoint.velocity[index];
            J++;
         }

         J = 0 ;
         for(OneDoFJoint armJoint: currentRobotModel.armJointIDsList.get( RobotSide.RIGHT) )
         {   
            int index = jointNameToTrajectoryIndex.get( armJoint.getName() );
            packet.rightArmJointAngle[J][w]    = jointsWaypoint.position[index];
            packet.rightArmJointVelocity[J][w] = jointsWaypoint.velocity[index];
            J++;
         }

         ///---------- calculation in task space ------------

         // store this before updating the model.

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
         ReferenceFrame pelvis    = currentRobotModel.getRootJoint().getFrameAfterJoint();
         ReferenceFrame movedFoot = currentRobotModel.getSoleFrame(RobotSide.RIGHT);

         RigidBodyTransform footToPelvis  = pelvis.getTransformToDesiredFrame( movedFoot );
         RigidBodyTransform worldToPelvis = new RigidBodyTransform();
         worldToPelvis.multiply( worldToFoot, footToPelvis );

         currentRobotModel.getRootJoint().setPositionAndRotation(worldToPelvis);

         currentRobotModel.updateFrames();

         TwistCalculator twistCalculator = new TwistCalculator(worldFrame,  currentRobotModel.getElevator() );
         twistCalculator.compute();

         //-----  store pelvis data --------
         worldToPelvis.get( packet.pelvisWorldOrientation[w], temp );
         packet.pelvisWorldPosition[w].set( temp );

         Twist twistToPack = new Twist();
         twistCalculator.packTwistOfBody(twistToPack, currentRobotModel.getElevator() );

         packet.pelvisLinearVelocity[w].set(  twistToPack.getLinearPartCopy()  );
         packet.pelvisAngularVelocity[w].set( twistToPack.getAngularPartCopy() );

         //-----  store chest data --------
         RigidBodyTransform worldToChest =  currentRobotModel.getChest().getParentJoint().getFrameAfterJoint().getTransformToWorldFrame();
         worldToChest.get( packet.chestWorldOrientation[w] );

         twistCalculator.packTwistOfBody(twistToPack, currentRobotModel.getChest() );
         packet.chestAngularVelocity[w].set(  twistToPack.getAngularPartCopy() );
      }
      
      // check if parts of the trajectory packet can be set to null to decrease packet size:
      double epsilon = 1e-5;
      Vector3d zeroVector = new Vector3d();
      
      RigidBodyTransform pelvisToWorld = fullRobotModel.getRootJoint().getFrameAfterJoint().getTransformToWorldFrame();
      Point3d pelvisPosition = new Point3d();
      Quat4d pelvisOrientation = new Quat4d();
      pelvisToWorld.get(pelvisOrientation, pelvisPosition);
      
      RigidBodyTransform chestToWorld =  fullRobotModel.getChest().getParentJoint().getFrameAfterJoint().getTransformToWorldFrame();
      Point3d chestPosition = new Point3d();
      Quat4d chestOrientation = new Quat4d();
      chestToWorld.get(chestOrientation, chestPosition);
      
      boolean rightArmVelocityContent = false;
      boolean rightArmPositionContent = false;
      boolean leftArmVelocityContent = false;
      boolean leftArmPositionContent = false;
      
      boolean pelvisLinearVelocityContent = false;
      boolean pelvisWorldPositionContent = false;
      boolean pelvisAngularVelocityContent = false;
      boolean pelvisWorldOrientationContent = false;
      
      boolean chestWorldOrientationContent = false;
      boolean chestAngulatVelocityContent = false;
      
      for (int n = 0; n < numWaypoints; n++)
      {
         int J = 0;
         for(OneDoFJoint armJoint: fullRobotModel.armJointIDsList.get(RobotSide.RIGHT))
         {   
            if (!MathTools.epsilonEquals(packet.rightArmJointAngle[J][n], armJoint.getQ(), epsilon))
            {
               rightArmPositionContent = true;
            }
            if (!MathTools.epsilonEquals(packet.rightArmJointVelocity[J][n], 0.0, epsilon))
            {
               rightArmVelocityContent = true;
            }
            J++;
         }
         
         J = 0;
         for(OneDoFJoint armJoint: fullRobotModel.armJointIDsList.get(RobotSide.LEFT))
         {   
            if (!MathTools.epsilonEquals(packet.leftArmJointAngle[J][n], armJoint.getQ(), epsilon))
            {
               leftArmPositionContent = true;
            }
            if (!MathTools.epsilonEquals(packet.leftArmJointVelocity[J][n], 0.0, epsilon))
            {
               leftArmVelocityContent = true;
            }
            J++;
         }
         
         if(!packet.pelvisLinearVelocity[n].epsilonEquals(zeroVector, epsilon));
         {
            pelvisLinearVelocityContent = true;
         }
         if(!packet.pelvisWorldPosition[n].epsilonEquals(pelvisPosition, epsilon))
         {
            pelvisWorldPositionContent = true;
         }
         if(!packet.pelvisWorldOrientation[n].epsilonEquals(pelvisOrientation, epsilon))
         {
            pelvisWorldOrientationContent = true;
         }
         if(!packet.pelvisAngularVelocity[n].epsilonEquals(zeroVector, epsilon))
         {
            pelvisLinearVelocityContent = true;
         }
         
         if(!packet.chestWorldOrientation[n].epsilonEquals(chestOrientation, epsilon))
         {
            chestWorldOrientationContent = true;
         }
         if(!packet.chestAngularVelocity[n].epsilonEquals(zeroVector, epsilon))
         {
            chestAngulatVelocityContent = true;
         }
      }
      
      if (!rightArmPositionContent)
      {
//         System.out.println("no right arm position content");
         packet.rightArmJointAngle = null;
      }
      if (!rightArmVelocityContent)
      {
//         System.out.println("no right arm velocity content");
         packet.rightArmJointVelocity = null;
      }
      if (!leftArmPositionContent)
      {
//         System.out.println("no left arm position content");
         packet.leftArmJointAngle = null;
      }
      if (!leftArmVelocityContent)
      {
//         System.out.println("no left arm velocity content");
         packet.leftArmJointVelocity = null;
      }
      if (!pelvisLinearVelocityContent)
      {
//         System.out.println("no pelvis linear velocity");
         packet.pelvisLinearVelocity = null;
      }
      if(!pelvisWorldPositionContent)
      {
//         System.out.println("no pelvis position content");
         packet.pelvisWorldPosition = null;
      }
      if(!pelvisWorldOrientationContent)
      {
//         System.out.println("no pelvis orientation content");
         packet.pelvisWorldOrientation = null;
      }
      if(!pelvisAngularVelocityContent)
      {
//         System.out.println("no pelvis angular velocity content");
         packet.pelvisAngularVelocity = null;
      }
      if(!chestWorldOrientationContent)
      {
//         System.out.println("no chest orientation content");
         packet.chestWorldOrientation = null;
      }
      if(!chestAngulatVelocityContent)
      {
//         System.out.println("no chest angular velocity content");
         packet.chestAngularVelocity = null;
      }
      
      return packet;
   }
}
