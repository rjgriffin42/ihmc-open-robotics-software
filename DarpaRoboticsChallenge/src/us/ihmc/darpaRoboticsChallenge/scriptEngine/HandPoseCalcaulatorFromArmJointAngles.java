package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFFullRobotModelFactory;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.packets.ArmJointAnglePacket;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.darpaRoboticsChallenge.DRCLocalConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.DRCRobotWalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.graphics3DAdapter.jme.util.JMEDataTypeUtils;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

import javax.media.j3d.Transform3D;
import java.util.EnumMap;

/**
 * Created by Peter on 12/5/13.
 */
public class HandPoseCalcaulatorFromArmJointAngles
{
   private FullRobotModel fullRobotModel;
   private SideDependentList<EnumMap<ArmJointName, OneDoFJoint>> oneDoFJoints = SideDependentList.createListOfEnumMaps(ArmJointName.class);
   private DRCRobotWalkingControllerParameters drcRobotWalkingControllerParameters = new DRCRobotWalkingControllerParameters();


   public HandPoseCalcaulatorFromArmJointAngles()
   {
      DRCRobotJointMap jointMap = new DRCRobotJointMap(DRCLocalConfigParameters.robotModelToUse, false);
      JaxbSDFLoader jaxbSDFLoader = DRCRobotSDFLoader.loadDRCRobot(jointMap, true);
      SDFFullRobotModelFactory fullRobotModelFactory = new SDFFullRobotModelFactory(jaxbSDFLoader.getGeneralizedSDFRobotModel(jointMap.getModelName()),
            jointMap);

      fullRobotModel = fullRobotModelFactory.create();

      for(RobotSide robotSide : RobotSide.values())
      {
         oneDoFJoints.get(robotSide).put(ArmJointName.SHOULDER_PITCH, fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_PITCH));
         oneDoFJoints.get(robotSide).put(ArmJointName.SHOULDER_ROLL, fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_ROLL));
         oneDoFJoints.get(robotSide).put(ArmJointName.ELBOW_PITCH, fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_PITCH));
         oneDoFJoints.get(robotSide).put(ArmJointName.ELBOW_ROLL, fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_ROLL));
         oneDoFJoints.get(robotSide).put(ArmJointName.WRIST_PITCH, fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_PITCH));
         oneDoFJoints.get(robotSide).put(ArmJointName.WRIST_ROLL, fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_ROLL));
      }
   }

   public FramePose getHandPoseInChestFrame(ArmJointAnglePacket armJointAnglePacket, Transform3D wristToHandTansform)
   {
      RobotSide robotSide = armJointAnglePacket.getRobotSide();

      oneDoFJoints.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setQ(armJointAnglePacket.getShy());
      oneDoFJoints.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setQ(armJointAnglePacket.getShx());
      oneDoFJoints.get(robotSide).get(ArmJointName.ELBOW_PITCH).setQ(armJointAnglePacket.getEly());
      oneDoFJoints.get(robotSide).get(ArmJointName.ELBOW_ROLL).setQ(armJointAnglePacket.getElx());
      oneDoFJoints.get(robotSide).get(ArmJointName.WRIST_PITCH).setQ(armJointAnglePacket.getWry());
      oneDoFJoints.get(robotSide).get(ArmJointName.WRIST_ROLL).setQ(armJointAnglePacket.getWrx());

      ReferenceFrame targetBody = fullRobotModel.getHand(robotSide).getParentJoint().getFrameAfterJoint();
      ReferenceFrame handPositionControlFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("targetBody_" + robotSide, targetBody,
            drcRobotWalkingControllerParameters.getHandControlFramesWithRespectToFrameAfterWrist().get(robotSide));

      FramePose handPose = new FramePose(handPositionControlFrame, wristToHandTansform);
      handPose.changeFrame(fullRobotModel.getChest().getParentJoint().getFrameAfterJoint());

      return handPose;
   }
}
