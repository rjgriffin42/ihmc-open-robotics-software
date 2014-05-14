package us.ihmc.valkyrie.simulation;

import java.io.IOException;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.posePlayback.PosePlaybackSCSBridge;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.valkyrie.ValkyrieRobotModel;

public class ValkyriePosePlaybackSCSBridge
{

   public static void main(String[] args) throws IOException
   {
      DRCRobotModel robotModel = new ValkyrieRobotModel(false, false);
      
      DRCRobotJointMap jointMap = robotModel.getJointMap();
      SDFRobot sdfRobot = robotModel.createSdfRobot(false);
      FullRobotModel fullRobotModel = robotModel.createFullRobotModel();
      SDFFullRobotModel fullRobotModelForSlider = robotModel.createFullRobotModel();
   
      new PosePlaybackSCSBridge(sdfRobot, fullRobotModel, fullRobotModelForSlider, robotModel.getControllerDT());
   
   }
}
