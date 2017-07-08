package us.ihmc.commonWalkingControlModules.controllerCore.command;

import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolderReadOnly;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.RootJointDesiredConfigurationData;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.RootJointDesiredConfigurationDataReadOnly;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.robotics.geometry.FramePoint2D;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;

public class ControllerCoreOutput implements ControllerCoreOutputReadOnly
{
   private final CenterOfPressureDataHolder centerOfPressureDataHolder;
   private final FrameVector3D linearMomentumRate = new FrameVector3D(ReferenceFrame.getWorldFrame());
   private final RootJointDesiredConfigurationData rootJointDesiredConfigurationData = new RootJointDesiredConfigurationData();
   private final LowLevelOneDoFJointDesiredDataHolder lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();

   public ControllerCoreOutput(CenterOfPressureDataHolder centerOfPressureDataHolder, OneDoFJoint[] controlledOneDoFJoints)
   {
      this.centerOfPressureDataHolder = centerOfPressureDataHolder;
      linearMomentumRate.setToNaN(ReferenceFrame.getWorldFrame());
      lowLevelOneDoFJointDesiredDataHolder.registerJointsWithEmptyData(controlledOneDoFJoints);
   }

   public void setDesiredCenterOfPressure(FramePoint2D cop, RigidBody rigidBody)
   {
      centerOfPressureDataHolder.setCenterOfPressure(cop, rigidBody);
   }

   @Override
   public void getDesiredCenterOfPressure(FramePoint2D copToPack, RigidBody rigidBody)
   {
      centerOfPressureDataHolder.getCenterOfPressure(copToPack, rigidBody);
   }

   public void setLinearMomentumRate(FrameVector3D linearMomentumRate)
   {
      this.linearMomentumRate.set(linearMomentumRate);
   }

   public void setAndMatchFrameLinearMomentumRate(FrameVector3D linearMomentumRate)
   {
      this.linearMomentumRate.setIncludingFrame(linearMomentumRate);
      this.linearMomentumRate.changeFrame(ReferenceFrame.getWorldFrame());
   }

   @Override
   public void getLinearMomentumRate(FrameVector3D linearMomentumRateToPack)
   {
      linearMomentumRateToPack.setIncludingFrame(linearMomentumRate);
   }

   public void setRootJointDesiredConfigurationData(RootJointDesiredConfigurationDataReadOnly rootJointDesiredConfigurationData)
   {
      this.rootJointDesiredConfigurationData.set(rootJointDesiredConfigurationData);
   }

   @Override
   public RootJointDesiredConfigurationDataReadOnly getRootJointDesiredConfigurationData()
   {
      return rootJointDesiredConfigurationData;
   }

   public void setLowLevelOneDoFJointDesiredDataHolder(LowLevelOneDoFJointDesiredDataHolderReadOnly lowLevelOneDoFJointDesiredDataHolder)
   {
      this.lowLevelOneDoFJointDesiredDataHolder.overwriteWith(lowLevelOneDoFJointDesiredDataHolder);
   }

   @Override
   public LowLevelOneDoFJointDesiredDataHolderReadOnly getLowLevelOneDoFJointDesiredDataHolder()
   {
      return lowLevelOneDoFJointDesiredDataHolder;
   }
}
