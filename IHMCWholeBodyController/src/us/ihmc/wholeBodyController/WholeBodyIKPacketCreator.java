package us.ihmc.wholeBodyController;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.walking.ChestOrientationPacket;
import us.ihmc.communication.packets.walking.ComHeightPacket;
import us.ihmc.communication.packets.walking.PelvisPosePacket;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.InverseDynamicsJointStateCopier;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;

public class WholeBodyIKPacketCreator
{
//   private final ArmJointName[] armJointNames;
   private final SDFFullRobotModel outgoingSDFFullRobotModel;
   private final ReferenceFrames desiredReferenceFrames;
   private final ReferenceFrame pelvisReferenceFrame;
   private final ReferenceFrame chestReferenceFrame;
   private final ReferenceFrame midZUpReferenceFrame;
   private final double nominalComHeight;
   private final InverseDynamicsJointStateCopier desiredToPreviewInverseDynamicsJointStateCopier;

   public WholeBodyIKPacketCreator(WholeBodyControllerParameters drcRobotModel)
   {
      this.nominalComHeight = drcRobotModel.getWalkingControllerParameters().nominalHeightAboveAnkle();
//      this.armJointNames = drcRobotModel.getJointMap().getArmJointNames();
      this.outgoingSDFFullRobotModel = drcRobotModel.createFullRobotModel();
      this.desiredReferenceFrames = new ReferenceFrames(outgoingSDFFullRobotModel);
      this.pelvisReferenceFrame = desiredReferenceFrames.getPelvisFrame();
      this.chestReferenceFrame = desiredReferenceFrames.getChestFrame();
      this.midZUpReferenceFrame = desiredReferenceFrames.getMidFeetZUpFrame();
      this.desiredToPreviewInverseDynamicsJointStateCopier = new InverseDynamicsJointStateCopier(outgoingSDFFullRobotModel.getElevator(), outgoingSDFFullRobotModel.getElevator());
   }

   public void createPackets(SDFFullRobotModel desiredSdfFullRobotModel, double trajectoryTime, ArrayList<Packet> packetArrayToPack)
   {
      updateOutgoingSDFFullRobotModelToDesired(desiredSdfFullRobotModel);
      desiredReferenceFrames.updateFrames();

      packetArrayToPack.add(createHandPosePackets(RobotSide.LEFT, trajectoryTime));
      packetArrayToPack.add(createHandPosePackets(RobotSide.RIGHT, trajectoryTime));
      packetArrayToPack.add(createPelvisPosePacket(trajectoryTime));
      packetArrayToPack.add(createCOMHeightPacket(trajectoryTime));
      packetArrayToPack.add(createChestOrientationPacket(trajectoryTime));
   }

   private ChestOrientationPacket createChestOrientationPacket(double trajectoryTime)
   {
      chestReferenceFrame.update();
      Quat4d chestOrientation = new Quat4d();
      FramePose chestPose = new FramePose(chestReferenceFrame);
      chestPose.changeFrame(ReferenceFrame.getWorldFrame());
      chestPose.getOrientation(chestOrientation);
      return new ChestOrientationPacket(chestOrientation, trajectoryTime);
   }

   private ComHeightPacket createCOMHeightPacket(double trajectoryTime)
   {
      double comHeight = calculateComOffsetFromNominal();
      return new ComHeightPacket(comHeight, trajectoryTime);
   }

   private PelvisPosePacket createPelvisPosePacket(double trajectoryTime)
   {
      RigidBodyTransform pelvisPose = pelvisReferenceFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
      Quat4d rotation = new Quat4d();
      Vector3d translation = new Vector3d();
      pelvisPose.get (rotation, translation );

      return new PelvisPosePacket(new Point3d(translation), rotation, trajectoryTime);
   }

   private HandPosePacket createHandPosePackets(RobotSide robotSide, double trajectoryTime)
   {
      double[] jointAngles = new double[outgoingSDFFullRobotModel.getRobotSpecificJointNames().getArmJointNames().length];
      synchronized (outgoingSDFFullRobotModel)
      {
         int i = -1;
         for (ArmJointName jointName : outgoingSDFFullRobotModel.getRobotSpecificJointNames().getArmJointNames())
         {
            jointAngles[++i] = outgoingSDFFullRobotModel.getArmJoint(robotSide, jointName).getQ();
         }
      }
      return new HandPosePacket(robotSide, trajectoryTime, jointAngles);
   }

   private double calculateComOffsetFromNominal()
   {
      RigidBodyTransform transformFromMidFeetToPelvis = pelvisReferenceFrame.getTransformToDesiredFrame(midZUpReferenceFrame);
      Vector3d translationOffset = new Vector3d();
      transformFromMidFeetToPelvis.getTranslation(translationOffset);
      return translationOffset.getZ() - nominalComHeight;
   }

   private void updateOutgoingSDFFullRobotModelToDesired(SDFFullRobotModel desiredSdfRobotModel)
   {
      desiredToPreviewInverseDynamicsJointStateCopier.setRigidBodies(desiredSdfRobotModel.getElevator(), outgoingSDFFullRobotModel.getElevator());
      desiredToPreviewInverseDynamicsJointStateCopier.copy();
   }
}
