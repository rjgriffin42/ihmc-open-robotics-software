package us.ihmc.commonWalkingControlModules.packetConsumers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.j3d.Transform3D;

import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;

public class UserDesiredHandPoseProvider implements HandPoseProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final EnumYoVariable<HandPosePacket.DataType> userHandPoseDataType = new EnumYoVariable<HandPosePacket.DataType>("userHandPoseDataType", registry,
                                                                                   HandPosePacket.DataType.class);
   private final DoubleYoVariable userHandPoseTrajectoryTime = new DoubleYoVariable("userHandPoseTrajectoryTime", registry);
   private final BooleanYoVariable userHandPoseTakeEm = new BooleanYoVariable("userHandPoseTakeEm", registry);

   private final EnumYoVariable<RobotSide> userHandPoseSide = new EnumYoVariable<RobotSide>("userHandPoseSide", registry, RobotSide.class);


   private final DoubleYoVariable userHandPoseX = new DoubleYoVariable("userHandPoseX", registry);
   private final DoubleYoVariable userHandPoseY = new DoubleYoVariable("userHandPoseY", registry);
   private final DoubleYoVariable userHandPoseZ = new DoubleYoVariable("userHandPoseZ", registry);

// private final DoubleYoVariable userStepWidth = new DoubleYoVariable("userStepWidth", registry);
// private final DoubleYoVariable userStepHeight = new DoubleYoVariable("userStepHeight", registry);
// private final DoubleYoVariable userStepYaw = new DoubleYoVariable("userStepYaw", registry);
// private final BooleanYoVariable userStepsTakeEm = new BooleanYoVariable("userStepsTakeEm", registry);
// private final IntegerYoVariable userStepsNotifyCompleteCount = new IntegerYoVariable("userStepsNotifyCompleteCount", registry);
//
// private final ArrayList<Footstep> footstepList = new ArrayList<Footstep>();



   private final SideDependentList<FramePose> homePositions = new SideDependentList<FramePose>();
   private final SideDependentList<FramePose> desiredHandPoses = new SideDependentList<FramePose>();
   private final SideDependentList<Map<OneDoFJoint, Double>> finalDesiredJointAngleMaps = new SideDependentList<Map<OneDoFJoint, Double>>();

   private final ReferenceFrame chestFrame;

   private final SideDependentList<ReferenceFrame> packetReferenceFrames;
   private final FullRobotModel fullRobotModel;

   public UserDesiredHandPoseProvider(FullRobotModel fullRobotModel, SideDependentList<Transform3D> desiredHandPosesWithRespectToChestFrame, YoVariableRegistry parentRegistry)
   {
      this.fullRobotModel = fullRobotModel;
      chestFrame = fullRobotModel.getChest().getBodyFixedFrame();
      packetReferenceFrames = new SideDependentList<ReferenceFrame>(chestFrame, chestFrame);

      for (RobotSide robotSide : RobotSide.values)
      {
         FramePose homePose = new FramePose(chestFrame, desiredHandPosesWithRespectToChestFrame.get(robotSide));
         homePositions.put(robotSide, homePose);
         
         System.out.println("homePose = " + homePose);

         desiredHandPoses.put(robotSide, homePose);

         finalDesiredJointAngleMaps.put(robotSide, new LinkedHashMap<OneDoFJoint, Double>());
      }

      userHandPoseTrajectoryTime.set(1.0);
      
      parentRegistry.addChild(registry);
   }

   private void updateFromNewestPacket(RobotSide robotSide)
   {
      FramePose userDesiredHandPose = new FramePose(homePositions.get(robotSide));
      FramePoint position = new FramePoint();
      userDesiredHandPose.getPositionIncludingFrame(position);

      position.setX(position.getX() + userHandPoseX.getDoubleValue());
      position.setY(position.getY() + userHandPoseY.getDoubleValue());
      position.setZ(position.getZ() + userHandPoseZ.getDoubleValue());

      userDesiredHandPose.setPosition(position);
      desiredHandPoses.put(robotSide, userDesiredHandPose);
      packetReferenceFrames.put(robotSide, userDesiredHandPose.getReferenceFrame());

      System.out.println("userDesiredHandPose = " + userDesiredHandPose);

      Map<OneDoFJoint, Double> finalDesiredJointAngleMap = finalDesiredJointAngleMaps.get(robotSide);

      for (ArmJointName armJoint : fullRobotModel.getRobotSpecificJointNames().getArmJointNames())
      {
         finalDesiredJointAngleMap.put(fullRobotModel.getArmJoint(robotSide, armJoint), 0.0);
      }

   }

   public boolean checkForNewPose(RobotSide robotSide)
   {
      if (userHandPoseSide.getEnumValue() != robotSide)
         return false;

      return userHandPoseTakeEm.getBooleanValue();
   }

   public HandPosePacket.DataType checkPacketDataType(RobotSide robotSide)
   {
      return userHandPoseDataType.getEnumValue();
   }

   public boolean checkForHomePosition(RobotSide robotSide)
   {
      return false; 
      
//      if (!checkForNewPose(robotSide))
//         return false;
//
////    if (!packets.get(robotSide).get().isToHomePosition())
////       return false;
//
//      desiredHandPoses.put(robotSide, homePositions.get(robotSide));
//
//      return true;
   }

   public FramePose getDesiredHandPose(RobotSide robotSide)
   {
      updateFromNewestPacket(robotSide);

      userHandPoseTakeEm.set(false);
      return desiredHandPoses.get(robotSide);
   }

   public Map<OneDoFJoint, Double> getFinalDesiredJointAngleMaps(RobotSide robotSide)
   {
      updateFromNewestPacket(robotSide);

      return finalDesiredJointAngleMaps.get(robotSide);
   }

   public ReferenceFrame getDesiredReferenceFrame(RobotSide robotSide)
   {
      return packetReferenceFrames.get(robotSide);
   }

   public double getTrajectoryTime()
   {
      return userHandPoseTrajectoryTime.getDoubleValue();
   }

}
