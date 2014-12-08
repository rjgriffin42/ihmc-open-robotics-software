package us.ihmc.darpaRoboticsChallenge.initialSetup;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.wholeBodyController.DRCRobotJointMap;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.simulationconstructionset.GroundContactPoint;

public class SquaredUpDRCRobotInitialSetup implements DRCRobotInitialSetup<SDFRobot>
{
   private double groundZ;
   private final RigidBodyTransform rootToWorld = new RigidBodyTransform();
   private final Vector3d offset = new Vector3d();

   public SquaredUpDRCRobotInitialSetup()
   {
      this(0.0);
   }

   public SquaredUpDRCRobotInitialSetup(double groundZ)
   {
      this.groundZ = groundZ;
   }

   public void initializeRobot(SDFRobot robot, DRCRobotJointMap jointMap)
   {
      setArmJointPositions(robot);
      setLegJointPositions(robot);
      setPositionInWorld(robot);
   }
   
   protected void setPositionInWorld(SDFRobot robot)
   {
      robot.update();
      robot.getRootJointToWorldTransform(rootToWorld);
      rootToWorld.get(offset);
      Vector3d positionInWorld = new Vector3d();
      
      rootToWorld.get(positionInWorld);
      
      GroundContactPoint gc1 = robot.getFootGroundContactPoints(RobotSide.LEFT).get(0);
      double pelvisToFoot = positionInWorld.getZ() - gc1.getPositionPoint().getZ();
      
      offset.setZ(groundZ + pelvisToFoot);
      robot.setPositionInWorld(offset);
   }

   protected void setArmJointPositions(SDFRobot robot)
   {
      // Avoid singularities at startup

      //      robot.getOneDoFJoint(jointNames[l_arm_ely]).setQ(1.57);
//      robot.getOneDoFJoint(jointNames[l_arm_elx]).setQ(1.57);
//
//      robot.getOneDoFJoint(jointNames[r_arm_ely]).setQ(1.57);
//      robot.getOneDoFJoint(jointNames[r_arm_elx]).setQ(-1.57);
   }

   protected void setLegJointPositions(SDFRobot robot)
   {
//      try{
//         robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_hpy]).setQ(-0.4);
//         robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_hpy]).setQ(-0.4);
//   
//         robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_kny]).setQ(0.8);
//         robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_kny]).setQ(0.8);
//   
//         robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_aky]).setQ(-0.4);
//         robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_aky]).setQ(-0.4);
//      } catch(Exception e)
//      {
//         System.err.println("Hard Coded joint positions for wrong model! FIXME - SquaredUpDrcRobotInitialSetUp");
//      }
   }

   public void getOffset(Vector3d offsetToPack)
   {
      offsetToPack.set(offset);
   }

   public void setOffset(Vector3d offset)
   {
      this.offset.set(offset);
   }

   public void setInitialYaw(double yaw)
   {
   }

   public void setInitialGroundHeight(double groundHeight)
   {
      groundZ = groundHeight;
   }

   @Override
   public double getInitialYaw()
   {
      return 0;
   }

   @Override
   public double getInitialGroundHeight()
   {
      return groundZ;
   }
}
