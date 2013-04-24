package us.ihmc.darpaRoboticsChallenge.initialSetup;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;

public class SquaredUpDRCRobotInitialSetup implements RobotInitialSetup<SDFRobot>
{
   private final double groundZ;
   private Transform3D rootToWorld = new Transform3D();
   protected Vector3d offset = new Vector3d();
   
   public SquaredUpDRCRobotInitialSetup()
   {
      this(0.0);
   }
   
   public SquaredUpDRCRobotInitialSetup(double groundZ)
   {
      this.groundZ = groundZ;
   }
   
   public void initializeRobot(SDFRobot robot)
   {
      
      
      // Avoid singularities at startup
      robot.getOneDoFJoint("l_arm_ely").setQ(1.57);
      robot.getOneDoFJoint("l_arm_elx").setQ(1.57);
      
      robot.getOneDoFJoint("r_arm_ely").setQ(1.57);
      robot.getOneDoFJoint("r_arm_elx").setQ(-1.57);
      
      robot.getOneDoFJoint("l_leg_lhy").setQ(-0.4);
      robot.getOneDoFJoint("r_leg_lhy").setQ(-0.4);
      
      robot.getOneDoFJoint("l_leg_kny").setQ(0.8);
      robot.getOneDoFJoint("r_leg_kny").setQ(0.8);
      
      robot.getOneDoFJoint("l_leg_uay").setQ(-0.4);
      robot.getOneDoFJoint("r_leg_uay").setQ(-0.4);
      
      
      robot.update();
      robot.getRootJointToWorldTransform(rootToWorld);
      rootToWorld.get(offset);
      
      
//      GroundContactPoint gc1 = robot.getFootGroundContactPoints(RobotSide.LEFT).get(0);
//      double pelvisToFoot = offset.getZ() - gc1.getPositionPoint().getZ();
//      System.out.println("Footheight: " + pelvisToFoot);
      
      // Hardcoded for gazebo integration
      double pelvisToFoot = 0.887;
      
      
      offset.setZ(groundZ + pelvisToFoot);
//      offset.add(robot.getPositionInWorld());
      robot.setPositionInWorld(offset);      
   }
   
   
   public void getOffset(Vector3d offsetToPack)
   {
      offsetToPack.set(offset);
   }
   
   public void setOffset(Vector3d offset)
   {
      this.offset.set(offset);
   }

}
