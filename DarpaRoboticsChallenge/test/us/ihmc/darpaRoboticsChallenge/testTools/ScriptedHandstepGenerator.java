package us.ihmc.darpaRoboticsChallenge.testTools;

import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.desiredFootStep.Handstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.HandstepHelper;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;

public class ScriptedHandstepGenerator
{
   private final HandstepHelper handstepHelper;
   
   public ScriptedHandstepGenerator(FullRobotModel fullRobotModel)
   {
      handstepHelper = new HandstepHelper(fullRobotModel);
   }

   public Handstep createHandstep(RobotSide robotSide, Tuple3d position, Vector3d surfaceNormal, double rotationAngleAboutNormal)
   {
      Handstep desiredHandstep = handstepHelper.getDesiredHandstep(robotSide, position, surfaceNormal, rotationAngleAboutNormal);
      
      return desiredHandstep;
   }

}
