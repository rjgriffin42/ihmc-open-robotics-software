package us.ihmc.aware.planning.chooser.footstepChooser;

public interface SwingTargetGeneratorParameters
{

   double getMinimumVelocityForFullSkew();

   double getMinimumDistanceFromSameSideFoot();

   double getStanceLength();

   double getStanceWidth();

   double getMaxForwardSkew();
   
   double getMaxLateralSkew();

   double getMaxYawPerStep();
   
   double getXOffsetFromCenterOfHips();

}