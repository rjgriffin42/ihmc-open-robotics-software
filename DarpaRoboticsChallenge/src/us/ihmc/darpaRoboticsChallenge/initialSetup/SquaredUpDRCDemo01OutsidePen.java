package us.ihmc.darpaRoboticsChallenge.initialSetup;

import javax.vecmath.Vector3d;

public class SquaredUpDRCDemo01OutsidePen extends AdditionalOffsetSquaredUpDRCDemo01InitialSetup
{
   private static final Vector3d additionalOffset = new Vector3d(3.0, 12.0, 0.0);
   private static final double yaw = Math.PI/2.0;
   
   public SquaredUpDRCDemo01OutsidePen(double groundZ)
   {
      super(groundZ, additionalOffset, yaw);
   }
}

