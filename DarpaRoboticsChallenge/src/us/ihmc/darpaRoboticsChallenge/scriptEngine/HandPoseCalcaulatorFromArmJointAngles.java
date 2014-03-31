package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import java.util.EnumMap;

import javax.media.j3d.Transform3D;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFFullRobotModelFactory;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.packets.ArmJointAnglePacket;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

/**
 * Created by Peter on 12/5/13.
 */
public class HandPoseCalcaulatorFromArmJointAngles
{
   private FullRobotModel fullRobotModel;
   private SideDependentList<EnumMap<ArmJointName, OneDoFJoint>> oneDoFJoints = SideDependentList.createListOfEnumMaps(ArmJointName.class);
   private WalkingControllerParameters drcRobotWalkingControllerParameters;
   private DRCRobotJointMap jointMap;


   public HandPoseCalcaulatorFromArmJointAngles(DRCRobotModel robotModel)
   {
      jointMap = robotModel.getJointMap();
      JaxbSDFLoader jaxbSDFLoader = DRCRobotSDFLoader.loadDRCRobot(jointMap, true);
      SDFFullRobotModelFactory fullRobotModelFactory = new SDFFullRobotModelFactory(jaxbSDFLoader.getGeneralizedSDFRobotModel(jointMap.getModelName()),
                                                          jointMap);

      drcRobotWalkingControllerParameters = robotModel.getWalkingControlParamaters();
      fullRobotModel = fullRobotModelFactory.create();

      for (RobotSide robotSide : RobotSide.values())
      {
    	  for(ArmJointName jointName : jointMap.getArmJointNames())
    	  {
    		  oneDoFJoints.get(robotSide).put(jointName, fullRobotModel.getArmJoint(robotSide, jointName));
    	  }
      }
   }

   public FramePose getHandPoseInChestFrame(ArmJointAnglePacket armJointAnglePacket, Transform3D wristToHandTansform)
   {
      RobotSide robotSide = armJointAnglePacket.getRobotSide();
      int i = -1;
      for(ArmJointName jointName : jointMap.getArmJointNames())
      {
    	  oneDoFJoints.get(robotSide).get(jointName).setQ(armJointAnglePacket.getJointAngles()[++i]);
      }

      ReferenceFrame targetBody = fullRobotModel.getHand(robotSide).getParentJoint().getFrameAfterJoint();
      ReferenceFrame handPositionControlFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("targetBody_" + robotSide, targetBody,
                                                   drcRobotWalkingControllerParameters.getHandControlFramesWithRespectToFrameAfterWrist().get(robotSide));

      FramePose handPose = new FramePose(handPositionControlFrame, wristToHandTansform);
      handPose.changeFrame(fullRobotModel.getChest().getParentJoint().getFrameAfterJoint());

      return handPose;
   }
}
