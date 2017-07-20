package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import us.ihmc.commonWalkingControlModules.configurations.ICPAngularMomentumModifierParameters;
import us.ihmc.commonWalkingControlModules.configurations.ICPPlannerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.filters.AlphaFilteredYoFrameVector;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class ICPPlannerWithAngularMomentumOffsetWrapper extends ICPPlannerWithTimeFreezerWrapper implements ICPPlannerWithAngularMomentumOffsetInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final String namePrefix = "icpPlanner";

   private final YoBoolean modifyICPPlanByAngularMomentum;

   private final YoFrameVector modifiedICPVelocity;
   private final YoFrameVector modifiedICPAcceleration;

   private final YoFramePoint modifiedCMPPosition;
   private final YoFrameVector modifiedCMPVelocity;

   private final FrameVector cmpOffsetFromCoP = new FrameVector();
   private final YoFrameVector cmpOffset;
   private final AlphaFilteredYoFrameVector filteredCMPOffset;

   private final YoDouble modifiedTimeInCurrentState;
   private final YoDouble modifiedTimeInCurrentStateRemaining;

   private final YoDouble angularMomentumRateForwardGain;
   private final YoDouble angularMomentumRateLateralGain;

   private final YoDouble cmpOffsetAlphaFilter;

   private final SideDependentList<ReferenceFrame> soleZUpFrames;

   public ICPPlannerWithAngularMomentumOffsetWrapper(ICPPlannerInterface icpPlanner, SideDependentList<ReferenceFrame> soleZUpFrames,
                                                     YoVariableRegistry parentRegistry)
   {
      super(icpPlanner, parentRegistry);

      this.soleZUpFrames = soleZUpFrames;

      modifyICPPlanByAngularMomentum = new YoBoolean(namePrefix + "ModifyICPPlanByAngularMomentum", registry);

      modifiedICPVelocity = new YoFrameVector(namePrefix + "ModifiedCapturePointVelocity", worldFrame, registry);
      modifiedICPAcceleration = new YoFrameVector(namePrefix + "ModifiedCapturePointAcceleration", worldFrame, registry);

      modifiedCMPPosition = new YoFramePoint(namePrefix + "ModifiedCMPPosition", worldFrame, registry);
      modifiedCMPVelocity = new YoFrameVector(namePrefix + "ModifiedCMPVelocity", worldFrame, registry);

      cmpOffsetAlphaFilter = new YoDouble(namePrefix + "CMPOffsetAlphaFilter", registry);

      cmpOffset = new YoFrameVector(namePrefix + "CMPOffset", worldFrame, registry);
      filteredCMPOffset = AlphaFilteredYoFrameVector.createAlphaFilteredYoFrameVector(namePrefix + "FilteredCMPOffset", "", registry,
                                                                                      cmpOffsetAlphaFilter, cmpOffset);

      modifiedTimeInCurrentState = new YoDouble(namePrefix + "ModifiedTimeInCurrentState", registry);
      modifiedTimeInCurrentStateRemaining = new YoDouble(namePrefix + "ModifiedRemainingTime", registry);

      angularMomentumRateForwardGain = new YoDouble(namePrefix + "AngularMomentumRateForwardGain", registry);
      angularMomentumRateLateralGain = new YoDouble(namePrefix + "AngularMomentumRateLateralGain", registry);

      parentRegistry.addChild(registry);
   }

   @Override
   public void initializeParameters(ICPPlannerParameters icpPlannerParameters, ICPAngularMomentumModifierParameters angularMomentumModifierParameters)
   {
      super.initializeParameters(icpPlannerParameters);

      if (angularMomentumModifierParameters != null)
      {
         modifyICPPlanByAngularMomentum.set(angularMomentumModifierParameters.getModifyICPPlanByAngularMomentumRate());
         cmpOffsetAlphaFilter.set(angularMomentumModifierParameters.getCMPOffsetAlphaFilter());
         angularMomentumRateForwardGain.set(angularMomentumModifierParameters.getAngularMomentumRateForwardGain());
         angularMomentumRateLateralGain.set(angularMomentumModifierParameters.getAngularMomentumRateLateralGain());
      }
   }

   private final FramePoint desiredCMPPosition = new FramePoint();
   private final FrameVector desiredCMPVelocity = new FrameVector();

   private final FramePoint desiredICPPosition = new FramePoint();
   private final FrameVector desiredICPVelocity = new FrameVector();
   private final FrameVector desiredICPAcceleration = new FrameVector();

   public void modifyDesiredICPForAngularMomentum(FramePoint copEstimate, RobotSide supportSide)
   {
      super.getDesiredCentroidalMomentumPivotPosition(desiredCMPPosition);
      super.getDesiredCentroidalMomentumPivotVelocity(desiredCMPVelocity);

      super.getDesiredCapturePointPosition(desiredICPPosition);
      super.getDesiredCapturePointVelocity(desiredICPVelocity);
      super.getDesiredCapturePointAcceleration(desiredICPAcceleration);

      if (!modifyICPPlanByAngularMomentum.getBooleanValue() || copEstimate.containsNaN())
      {
         modifiedCMPPosition.set(desiredCMPPosition);
         modifiedCMPVelocity.set(desiredCMPVelocity);

         modifiedICPVelocity.set(desiredICPVelocity);
         modifiedICPAcceleration.set(desiredICPAcceleration);

         modifiedTimeInCurrentState.set(super.getTimeInCurrentState());
         modifiedTimeInCurrentStateRemaining.set(super.getTimeInCurrentStateRemaining());
      }
      else
      {
         cmpOffsetFromCoP.set(desiredCMPPosition);
         cmpOffsetFromCoP.sub(copEstimate);

         RobotSide transferToSide = super.getTransferToSide();

         if (supportSide == null && transferToSide == null)
         {
            cmpOffsetFromCoP.scale(angularMomentumRateForwardGain.getDoubleValue());
         }
         else
         {
            ReferenceFrame soleFrame;
            if (supportSide != null)
               soleFrame = soleZUpFrames.get(supportSide);
            else
               soleFrame = soleZUpFrames.get(transferToSide);

            cmpOffsetFromCoP.changeFrame(soleFrame);
            cmpOffsetFromCoP.setX(angularMomentumRateForwardGain.getDoubleValue() * cmpOffsetFromCoP.getX());
            cmpOffsetFromCoP.setY(angularMomentumRateLateralGain.getDoubleValue() * cmpOffsetFromCoP.getY());
            cmpOffsetFromCoP.changeFrame(worldFrame);
         }

         cmpOffset.set(cmpOffsetFromCoP);
         filteredCMPOffset.update();

         modifiedCMPPosition.set(desiredCMPPosition);
         modifiedCMPPosition.add(filteredCMPOffset);

         estimateCurrentTimeWithModifiedCMP(modifiedCMPPosition.getFrameTuple());

         double omega0 = super.getOmega0();
         modifiedICPVelocity.set(desiredICPPosition);
         modifiedICPVelocity.sub(modifiedCMPPosition);
         modifiedICPVelocity.scale(omega0);

         CapturePointTools.computeDesiredCapturePointAcceleration(omega0, modifiedICPVelocity, modifiedICPAcceleration);
         CapturePointTools.computeDesiredCentroidalMomentumPivotVelocity(modifiedICPVelocity, modifiedICPAcceleration, omega0, modifiedCMPVelocity);
      }
   }

   private void estimateCurrentTimeWithModifiedCMP(FramePoint desiredCoPFromAngularMomentum)
   {
      double copCMPDistance = desiredCMPPosition.getXYPlaneDistance(desiredCoPFromAngularMomentum);
      double distanceFromCMP = desiredICPPosition.getXYPlaneDistance(modifiedCMPPosition.getFrameTuple());

      double modifiedTimeInState = 1.0 / super.getOmega0() * Math.log(distanceFromCMP / copCMPDistance);
      modifiedTimeInCurrentState.set(modifiedTimeInState);
      modifiedTimeInCurrentStateRemaining.set(getCurrentStateDuration() - modifiedTimeInCurrentState.getDoubleValue());
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCapturePointVelocity(FrameVector desiredCapturePointVelocityToPack)
   {
      modifiedICPVelocity.getFrameTupleIncludingFrame(desiredCapturePointVelocityToPack);
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCapturePointVelocity(FrameVector2d desiredCapturePointVelocityToPack)
   {
      modifiedICPVelocity.getFrameTuple2dIncludingFrame(desiredCapturePointVelocityToPack);
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCapturePointVelocity(YoFrameVector desiredCapturePointVelocityToPack)
   {
      desiredCapturePointVelocityToPack.set(modifiedICPVelocity);
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCentroidalMomentumPivotPosition(FramePoint desiredCentroidalMomentumPivotPositionToPack)
   {
      modifiedCMPPosition.getFrameTupleIncludingFrame(desiredCentroidalMomentumPivotPositionToPack);
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCentroidalMomentumPivotPosition(FramePoint2d desiredCentroidalMomentumPivotPositionToPack)
   {
      modifiedCMPPosition.getFrameTuple2dIncludingFrame(desiredCentroidalMomentumPivotPositionToPack);
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCentroidalMomentumPivotVelocity(FrameVector desiredCentroidalMomentumPivotVelocityToPack)
   {
      modifiedCMPVelocity.getFrameTupleIncludingFrame(desiredCentroidalMomentumPivotVelocityToPack);
   }

   /** {@inheritDoc} */
   @Override
   public void getDesiredCentroidalMomentumPivotVelocity(FrameVector2d desiredCentroidalMomentumPivotVelocityToPack)
   {
      modifiedCMPVelocity.getFrameTuple2dIncludingFrame(desiredCentroidalMomentumPivotVelocityToPack);
   }

   /** {@inheritDoc} */
   @Override
   public double getTimeInCurrentState()
   {
      return modifiedTimeInCurrentState.getDoubleValue();
   }

   /** {@inheritDoc} */
   @Override
   public double getTimeInCurrentStateRemaining()
   {
      return modifiedTimeInCurrentStateRemaining.getDoubleValue();
   }
}
