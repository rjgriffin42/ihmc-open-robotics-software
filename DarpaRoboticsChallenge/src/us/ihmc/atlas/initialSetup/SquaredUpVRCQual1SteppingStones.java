package us.ihmc.atlas.initialSetup;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;

public class SquaredUpVRCQual1SteppingStones extends SquaredUpDRCRobotInitialSetup implements DRCRobotInitialSetup<SDFRobot>
{
   private final Vector3d additionalOffset = new Vector3d(17.0,8.0,0.0);
   private final double yaw = 0.0;

   private Vector3d newOffset = null;

   public SquaredUpVRCQual1SteppingStones(Vector3d additionalOffset, double yaw)
   {
      this(0.0);
   }

   public SquaredUpVRCQual1SteppingStones(double groundHeight)
   {
      super(groundHeight);
   }

   public void initializeRobot(SDFRobot robot, DRCRobotJointMap jointMap)
   {
      super.initializeRobot(robot, jointMap);

      if (newOffset == null)
      {
         newOffset = new Vector3d();
         super.getOffset(newOffset);
         newOffset.add(additionalOffset);
      }

      super.setOffset(newOffset);
      robot.setPositionInWorld(newOffset);
      robot.setOrientation(yaw, 0.0, 0.0);
   }
}
