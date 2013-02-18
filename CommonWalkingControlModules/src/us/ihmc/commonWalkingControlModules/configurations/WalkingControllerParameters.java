package us.ihmc.commonWalkingControlModules.configurations;

import javax.media.j3d.Transform3D;

import us.ihmc.robotSide.SideDependentList;

public interface WalkingControllerParameters extends HeadOrientationControllerParameters
{
   public abstract SideDependentList<Transform3D> getDesiredHandPosesWithRespectToChestFrame();

   public abstract double getDesiredCoMHeight();

   public abstract boolean doStrictPelvisControl();
      
   public abstract String[] getChestOrientationControlJointNames();

   public abstract boolean checkOrbitalCondition();

   public abstract double nominalHeightAboveGround();

   public abstract double initialHeightAboveGround();

   public abstract double getGroundReactionWrenchBreakFrequencyHertz();

   public abstract boolean resetDesiredICPToCurrentAtStartOfSwing();

   
}