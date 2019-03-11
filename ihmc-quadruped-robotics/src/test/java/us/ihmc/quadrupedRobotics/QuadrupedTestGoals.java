package us.ihmc.quadrupedRobotics;

import us.ihmc.robotics.testing.YoVariableTestGoal;

public class QuadrupedTestGoals
{
   public static YoVariableTestGoal notFallen(QuadrupedForceTestYoVariables variables)
   {
      YoVariableTestGoal minHeightGoal = YoVariableTestGoal.deltaGreaterThan(variables.getRobotBodyZ(), variables.getGroundPlanePointZ(), 0.0);
      YoVariableTestGoal fallenFlag = YoVariableTestGoal.booleanEquals(variables.getIsFallDetected(), false);
      return YoVariableTestGoal.and(minHeightGoal, fallenFlag);
   }

   public static YoVariableTestGoal bodyHeight(QuadrupedForceTestYoVariables variables, double height)
   {
      return YoVariableTestGoal.deltaGreaterThan(variables.getRobotBodyZ(), variables.getGroundPlanePointZ(), height);
   }

   public static YoVariableTestGoal notFallen(QuadrupedPositionTestYoVariables variables)
   {
      return YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), variables.getDesiredCoMPositionZ().getDoubleValue() / 2.0);
   }

   public static YoVariableTestGoal timeInFuture(QuadrupedTestYoVariables variables, double durationFromNow)
   {
      return YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), variables.getYoTime().getDoubleValue() + durationFromNow);
   }
}
