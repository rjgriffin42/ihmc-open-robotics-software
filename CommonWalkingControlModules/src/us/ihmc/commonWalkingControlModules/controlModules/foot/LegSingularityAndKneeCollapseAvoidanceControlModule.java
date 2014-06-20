package us.ihmc.commonWalkingControlModules.controlModules.foot;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesData;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.GeometryTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicReferenceFrame;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class LegSingularityAndKneeCollapseAvoidanceControlModule
{
   private boolean visualize = true;
   private boolean moreVisualizers = true;
   
   private static final boolean USE_SINGULARITY_AVOIDANCE_SWING = true; // Limit the swing foot motion according to the leg motion range.
   public static final boolean USE_SINGULARITY_AVOIDANCE_SUPPORT = true; // Progressively limit the CoM height as the support leg(s) are getting more straight
   private static final boolean USE_UNREACHABLE_FOOTSTEP_CORRECTION = true; // Lower the CoM if a footstep is unreachable
   private static final boolean USE_COLLAPSE_AVOIDANCE = false; // Try to avoid the knee from collapsing by limiting how low the CoM can be

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame endEffectorFrame;
   private final ReferenceFrame virtualLegTangentialFrameHipCentered, virtualLegTangentialFrameAnkleCentered;

   private final YoVariableRegistry registry;

   private final BooleanYoVariable checkVelocityForSwingSingularityAvoidance;

   private final DoubleYoVariable alphaSwingSingularityAvoidance;
   private final DoubleYoVariable alphaSupportSingularityAvoidance;
   private final DoubleYoVariable alphaCollapseAvoidance;
   private final DoubleYoVariable alphaUnreachableFootstep;
   
   private final DoubleYoVariable maximumLegLength;

   private final DoubleYoVariable percentOfLegLengthThresholdToEnableSingularityAvoidance;
   private final DoubleYoVariable percentOfLegLengthThresholdToDisableSingularityAvoidance;
   private final DoubleYoVariable percentOfLegLengthThresholdForCollapseAvoidance;
   private final DoubleYoVariable maxPercentOfLegLengthForSingularityAvoidanceInSwing;
   private final DoubleYoVariable maxPercentOfLegLengthForSingularityAvoidanceInSupport;
   private final DoubleYoVariable minPercentOfLegLengthForCollapseAvoidance;

   private final DoubleYoVariable footLoadThresholdToEnableCollapseAvoidance;
   private final DoubleYoVariable footLoadThresholdToDisableCollapseAvoidance;
   private final DoubleYoVariable timeDelayToDisableCollapseAvoidance;
   private final DoubleYoVariable timeSwitchCollapseAvoidance;
   private final DoubleYoVariable timeRemainingToDisableCollapseAvoidance;
   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable timeSwitchSingularityAvoidance;

   private final DoubleYoVariable desiredPercentOfLegLength;
   private final DoubleYoVariable currentPercentOfLegLength;
   private final DoubleYoVariable correctedDesiredPercentOfLegLength;

   private final DoubleYoVariable desiredLegLength;
   private final DoubleYoVariable currentLegLength;
   private final DoubleYoVariable correctedDesiredLegLength;

   private final RigidBody pelvis;
   
   private final FrameVector unachievedSwingTranslation = new FrameVector();   
   private final FrameVector unachievedSwingVelocity = new FrameVector(); 
   private final FrameVector unachievedSwingAcceleration = new FrameVector();

   private final FramePoint desiredCenterOfMassHeightPoint = new FramePoint(worldFrame);
   private final FramePoint anklePosition = new FramePoint(worldFrame);
   private final FrameVector equivalentDesiredHipPitchHeightTranslation = new FrameVector();
   private final FrameVector equivalentDesiredHipVelocity = new FrameVector();
   private final FrameVector equivalentDesiredHipPitchAcceleration = new FrameVector();
   
   private final ReferenceFrame frameBeforeHipPitchJoint;

   private final Twist pelvisTwist = new Twist();
   private final FrameVector pelvisLinearVelocity = new FrameVector();
   private final FramePoint desiredFootPosition = new FramePoint();
   private final FrameVector desiredFootLinearVelocity = new FrameVector();
   private final FrameVector desiredFootLinearAcceleration = new FrameVector();
   private final TwistCalculator twistCalculator;

   private final YoFramePoint yoCurrentFootPosition;
   private final YoFramePoint yoDesiredFootPosition;
   private final YoFramePoint yoCorrectedDesiredFootPosition;

   private final YoFrameVector yoDesiredFootLinearVelocity;
   private final YoFrameVector yoCorrectedDesiredFootLinearVelocity;
   
   private final DynamicGraphicReferenceFrame virtualLegTangentialFrameHipCenteredGraphics, virtualLegTangentialFrameAnkleCenteredGraphics;
   private final DynamicGraphicPosition yoDesiredFootPositionGraphic, yoCorrectedDesiredFootPositionGraphic;
   private final DynamicGraphicVector yoDesiredFootLinearVelocityGraphic, yoCorrectedDesiredFootLinearVelocityGraphic;

   private final BooleanYoVariable isSwingSingularityAvoidanceUsed;
   private final BooleanYoVariable isSupportSingularityAvoidanceUsed;
   private final BooleanYoVariable isSupportCollapseAvoidanceUsed;
   private final BooleanYoVariable isUnreachableFootstepCompensated;
   private final BooleanYoVariable doSmoothTransitionOutOfCollapseAvoidance;
   private final BooleanYoVariable doSmoothTransitionOutOfSingularityAvoidance;
   
   private final DoubleYoVariable correctionAlphaFilter;
   private final AlphaFilteredYoVariable heightCorrectedFilteredForCollapseAvoidance;
   private final AlphaFilteredYoVariable heightVelocityCorrectedFilteredForCollapseAvoidance;
   private final AlphaFilteredYoVariable heightAcceleretionCorrectedFilteredForCollapseAvoidance;

   private final AlphaFilteredYoVariable heightCorrectedFilteredForSingularityAvoidance;
   private final AlphaFilteredYoVariable heightVelocityCorrectedFilteredForSingularityAvoidance;
   private final AlphaFilteredYoVariable heightAcceleretionCorrectedFilteredForSingularityAvoidance;

   private final AlphaFilteredYoVariable unachievedSwingTranslationFiltered;   
   private final AlphaFilteredYoVariable unachievedSwingVelocityFiltered; 
   private final AlphaFilteredYoVariable unachievedSwingAccelerationFiltered;

   private final double controlDT;
   
   public LegSingularityAndKneeCollapseAvoidanceControlModule(String namePrefix, ContactablePlaneBody contactablePlaneBody, final RobotSide robotSide,
         WalkingControllerParameters walkingControllerParameters, final MomentumBasedController momentumBasedController,
         YoVariableRegistry parentRegistry)
   {
      registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      parentRegistry.addChild(registry);
      
      maximumLegLength = new DoubleYoVariable(namePrefix + "MaxLegLength", registry);
      maximumLegLength.set(walkingControllerParameters.getLegLength());

      twistCalculator = momentumBasedController.getTwistCalculator();
      controlDT = momentumBasedController.getControlDT();
      yoTime = momentumBasedController.getYoTime();

      pelvis = momentumBasedController.getFullRobotModel().getPelvis();
      frameBeforeHipPitchJoint = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.HIP_PITCH).getFrameBeforeJoint();
      endEffectorFrame = contactablePlaneBody.getBodyFrame();
      
      checkVelocityForSwingSingularityAvoidance = new BooleanYoVariable(namePrefix + "CheckVelocityForSwingSingularityAvoidance", registry);

      alphaSwingSingularityAvoidance = new DoubleYoVariable(namePrefix + "AlphaSwingSingularityAvoidance", registry);
      alphaSupportSingularityAvoidance = new DoubleYoVariable(namePrefix + "AlphaSupportSingularityAvoidance", registry);
      alphaCollapseAvoidance = new DoubleYoVariable(namePrefix + "AlphaCollapseAvoidance", registry);
      alphaUnreachableFootstep = new DoubleYoVariable(namePrefix + "AlphaUnreachableFootstep", registry);
      alphaUnreachableFootstep.set(0.25);
      
      unachievedSwingTranslationFiltered = new AlphaFilteredYoVariable(namePrefix + "UnachievedSwingTranslationFiltered", registry, alphaUnreachableFootstep);
      unachievedSwingVelocityFiltered = new AlphaFilteredYoVariable(namePrefix + "UnachievedSwingVelocityFiltered", registry, alphaUnreachableFootstep);
      unachievedSwingAccelerationFiltered = new AlphaFilteredYoVariable(namePrefix + "UnachievedSwingAccelerationFiltered", registry, alphaUnreachableFootstep);
      
      percentOfLegLengthThresholdToEnableSingularityAvoidance = new DoubleYoVariable(namePrefix + "PercThresSingularityAvoidance", registry);
      percentOfLegLengthThresholdToDisableSingularityAvoidance = new DoubleYoVariable(namePrefix + "PercThresToDisableSingularityAvoidance", registry);
      maxPercentOfLegLengthForSingularityAvoidanceInSwing = new DoubleYoVariable(namePrefix + "MaxPercOfLegLengthForSingularityAvoidanceInSwing", registry);
      maxPercentOfLegLengthForSingularityAvoidanceInSupport = new DoubleYoVariable(namePrefix + "MaxPercOfLegLengthForSingularityAvoidanceInSupport", registry);
      percentOfLegLengthThresholdForCollapseAvoidance = new DoubleYoVariable(namePrefix + "PercThresCollapseAvoidance", registry);
      minPercentOfLegLengthForCollapseAvoidance = new DoubleYoVariable(namePrefix + "MinPercOfLegLengthForCollapseAvoidance", registry);
      footLoadThresholdToEnableCollapseAvoidance = new DoubleYoVariable(namePrefix + "LoadThresholdToEnableCollapseAvoidance", registry);
      footLoadThresholdToDisableCollapseAvoidance = new DoubleYoVariable(namePrefix + "LoadThresholdToDisableCollapseAvoidance", registry);
      timeDelayToDisableCollapseAvoidance = new DoubleYoVariable(namePrefix + "TimeDelayToDisableCollapseAvoidance", registry);
      timeSwitchCollapseAvoidance = new DoubleYoVariable(namePrefix + "TimeSwitchCollapseAvoidance", registry);
      timeRemainingToDisableCollapseAvoidance = new DoubleYoVariable(namePrefix + "TimeRemainingToDisableCollapseAvoidance", registry);
      timeSwitchSingularityAvoidance = new DoubleYoVariable(namePrefix + "TimeSwitchSingularityAvoidance", registry);

      percentOfLegLengthThresholdToEnableSingularityAvoidance.set(0.87);
      percentOfLegLengthThresholdToDisableSingularityAvoidance.set(0.85);
      maxPercentOfLegLengthForSingularityAvoidanceInSwing.set(0.97);
      maxPercentOfLegLengthForSingularityAvoidanceInSupport.set(0.98);
      percentOfLegLengthThresholdForCollapseAvoidance.set(0.83);
      minPercentOfLegLengthForCollapseAvoidance.set(0.76);//walkingControllerParameters.getMinLegLengthBeforeCollapsingSingleSupport() / maximumLegLength.getDoubleValue());
      footLoadThresholdToEnableCollapseAvoidance.set(0.62); // 0.65
      footLoadThresholdToDisableCollapseAvoidance.set(0.59); // 0.62
      timeDelayToDisableCollapseAvoidance.set(0.5);
      timeRemainingToDisableCollapseAvoidance.set(timeDelayToDisableCollapseAvoidance.getDoubleValue());
      timeSwitchCollapseAvoidance.set(yoTime.getDoubleValue());

      correctionAlphaFilter = new DoubleYoVariable(namePrefix + "CorrectionAlphaFilter", registry);
      heightCorrectedFilteredForCollapseAvoidance = new AlphaFilteredYoVariable(namePrefix + "HeightCorrectedFilteredForCollapseAvoidance", registry, correctionAlphaFilter);
      heightVelocityCorrectedFilteredForCollapseAvoidance = new AlphaFilteredYoVariable(namePrefix + "HeightVelocityCorrectedFilteredForCollapseAvoidance", registry, correctionAlphaFilter);
      heightAcceleretionCorrectedFilteredForCollapseAvoidance = new AlphaFilteredYoVariable(namePrefix + "HeightAcceleretionCorrectedFilteredForCollapseAvoidance", registry, correctionAlphaFilter);
      
      heightCorrectedFilteredForSingularityAvoidance = new AlphaFilteredYoVariable(namePrefix + "HeightCorrectedFilteredForSingularityAvoidance", registry, correctionAlphaFilter);
      heightVelocityCorrectedFilteredForSingularityAvoidance = new AlphaFilteredYoVariable(namePrefix + "HeightVelocityCorrectedFilteredForSingularityAvoidance", registry, correctionAlphaFilter);
      heightAcceleretionCorrectedFilteredForSingularityAvoidance = new AlphaFilteredYoVariable(namePrefix + "HeightAcceleretionCorrectedFilteredForSingularityAvoidance", registry, correctionAlphaFilter);
      
      correctionAlphaFilter.set(0.98);

      desiredPercentOfLegLength = new DoubleYoVariable(namePrefix + "DesiredPercentOfLegLength", registry);
      correctedDesiredPercentOfLegLength = new DoubleYoVariable(namePrefix + "CorrectedDesiredPercentOfLegLength", registry);
      currentPercentOfLegLength = new DoubleYoVariable(namePrefix + "CurrentPercentOfLegLength", registry);

      desiredLegLength = new DoubleYoVariable(namePrefix + "DesiredLegLength", registry);
      correctedDesiredLegLength = new DoubleYoVariable(namePrefix + "CorrectedDesiredLegLength", registry);
      currentLegLength = new DoubleYoVariable(namePrefix + "CurrentLegLength", registry);

      isSwingSingularityAvoidanceUsed = new BooleanYoVariable(namePrefix + "IsSwingSingularityAvoidanceUsed", registry);
      isSupportSingularityAvoidanceUsed = new BooleanYoVariable(namePrefix + "IsSupportSingularityAvoidanceUsed", registry);
      isSupportCollapseAvoidanceUsed = new BooleanYoVariable(namePrefix + "IsSupportCollapseAvoidanceUsed", registry);
      isUnreachableFootstepCompensated = new BooleanYoVariable(namePrefix + "IsUnreachableFootstepCompensated", registry);
      doSmoothTransitionOutOfCollapseAvoidance = new BooleanYoVariable(namePrefix + "DoSmoothTransitionCollapseAvoidance", registry);
      doSmoothTransitionOutOfSingularityAvoidance = new BooleanYoVariable(namePrefix + "DoSmoothTransitionSingularityAvoidance", registry);

      final ReferenceFrame pelvisFrame = pelvis.getParentJoint().getFrameAfterJoint();
      virtualLegTangentialFrameHipCentered = new ReferenceFrame(namePrefix + "VirtualLegTangentialFrameHipCentered", pelvisFrame)
      {
         private static final long serialVersionUID = 8992154939350877111L;
         private final AxisAngle4d hipPitchRotationToParentFrame = new AxisAngle4d();
         private final Vector3d hipPitchToParentFrame = new Vector3d();
         private final FramePoint tempPoint = new FramePoint();
         private final FrameVector footToHipAxis = new FrameVector();
         private final FramePoint hipPitchPosition = new FramePoint();

         @Override
         public void updateTransformToParent(Transform3D transformToParent)
         {
            tempPoint.setToZero(frameBeforeHipPitchJoint);
            tempPoint.changeFrame(endEffectorFrame);
            footToHipAxis.setIncludingFrame(tempPoint);
            footToHipAxis.changeFrame(getParent());
            GeometryTools.getRotationBasedOnNormal(hipPitchRotationToParentFrame, footToHipAxis.getVector());
            hipPitchPosition.setToZero(frameBeforeHipPitchJoint);
            hipPitchPosition.changeFrame(getParent());
            hipPitchPosition.get(hipPitchToParentFrame);

            transformToParent.set(hipPitchRotationToParentFrame);
            transformToParent.setTranslation(hipPitchToParentFrame);
         }
      };
      
      virtualLegTangentialFrameAnkleCentered = new ReferenceFrame(namePrefix + "VirtualLegTangentialFrameAnkleCentered", pelvisFrame)
      {
         private static final long serialVersionUID = 2338083143740929570L;
         private final AxisAngle4d anklePitchRotationToParentFrame = new AxisAngle4d();
         private final Vector3d anklePitchToParentFrame = new Vector3d();
         private final FramePoint tempPoint = new FramePoint();
         private final FrameVector footToHipAxis = new FrameVector();
         private final FramePoint anklePitchPosition = new FramePoint();

         @Override
         public void updateTransformToParent(Transform3D transformToParent)
         {
            tempPoint.setToZero(frameBeforeHipPitchJoint);
            tempPoint.changeFrame(endEffectorFrame);
            footToHipAxis.setIncludingFrame(tempPoint);
            footToHipAxis.changeFrame(getParent());
            GeometryTools.getRotationBasedOnNormal(anklePitchRotationToParentFrame, footToHipAxis.getVector());
            anklePitchPosition.setToZero(endEffectorFrame);
            anklePitchPosition.changeFrame(getParent());
            anklePitchPosition.get(anklePitchToParentFrame);

            transformToParent.set(anklePitchRotationToParentFrame);
            transformToParent.setTranslation(anklePitchToParentFrame);
         }
      };
      
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = momentumBasedController.getDynamicGraphicObjectsListRegistry();
      visualize = visualize && dynamicGraphicObjectsListRegistry != null;
      moreVisualizers = visualize && moreVisualizers;

      yoCurrentFootPosition = new YoFramePoint(namePrefix + "CurrentFootPosition", worldFrame, registry);
      yoDesiredFootPosition = new YoFramePoint(namePrefix + "DesiredFootPosition", worldFrame, registry);
      yoCorrectedDesiredFootPosition = new YoFramePoint(namePrefix + "CorrectedDesiredFootPosition", worldFrame, registry);
      yoDesiredFootLinearVelocity = new YoFrameVector(namePrefix + "DesiredFootLinearVelocity", worldFrame, registry);
      yoCorrectedDesiredFootLinearVelocity = new YoFrameVector(namePrefix + "CorrectedDesiredFootLinearVelocity", worldFrame, registry);
      yoDesiredFootPosition.setToNaN();
      yoCorrectedDesiredFootPosition.setToNaN();
      yoDesiredFootLinearVelocity.setToNaN();
      yoCorrectedDesiredFootLinearVelocity.setToNaN();

      if (visualize)
      {
         yoDesiredFootPositionGraphic = yoDesiredFootPosition.createDynamicGraphicPosition(namePrefix + "DesiredFootPosition", 0.025, YoAppearance.Red(), GraphicType.BALL);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("SingularityCollapseAvoidance", yoDesiredFootPositionGraphic);
         yoCorrectedDesiredFootPositionGraphic = yoCorrectedDesiredFootPosition.createDynamicGraphicPosition(namePrefix + "CorrectedDesiredFootPosition", 0.025, YoAppearance.Green(), GraphicType.BALL);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("SingularityCollapseAvoidance", yoCorrectedDesiredFootPositionGraphic);
      }
      else
      {
         yoDesiredFootPositionGraphic = null;
         yoCorrectedDesiredFootPositionGraphic = null;
      }
      
      if (moreVisualizers)
      {
         virtualLegTangentialFrameHipCenteredGraphics = new DynamicGraphicReferenceFrame(virtualLegTangentialFrameHipCentered, registry, 0.1);
         virtualLegTangentialFrameAnkleCenteredGraphics = new DynamicGraphicReferenceFrame(virtualLegTangentialFrameAnkleCentered, registry, 0.1);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("SingularityCollapseAvoidance", virtualLegTangentialFrameHipCenteredGraphics);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("SingularityCollapseAvoidance", virtualLegTangentialFrameAnkleCenteredGraphics);

         yoDesiredFootLinearVelocityGraphic = new DynamicGraphicVector(namePrefix + "DesiredFootLinearVelocity", yoCurrentFootPosition, yoDesiredFootLinearVelocity, 0.2, YoAppearance.Red());
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("SingularityCollapseAvoidance", yoDesiredFootLinearVelocityGraphic);
         yoCorrectedDesiredFootLinearVelocityGraphic = new DynamicGraphicVector(namePrefix + "CorrectedDesiredFootLinearVelocity", yoCurrentFootPosition, yoCorrectedDesiredFootLinearVelocity, 0.2, YoAppearance.Green());
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("SingularityCollapseAvoidance", yoCorrectedDesiredFootLinearVelocityGraphic);
      }
      else
      {
         virtualLegTangentialFrameHipCenteredGraphics = null;
         virtualLegTangentialFrameAnkleCenteredGraphics = null;
         yoDesiredFootLinearVelocityGraphic = null;
         yoCorrectedDesiredFootLinearVelocityGraphic = null;
      }
   }
   
   public void update()
   {
      virtualLegTangentialFrameHipCentered.update();
      virtualLegTangentialFrameAnkleCentered.update();
      
      yoDesiredFootPosition.setToNaN();
      yoCorrectedDesiredFootPosition.setToNaN();
      yoDesiredFootLinearVelocity.setToNaN();
      yoCorrectedDesiredFootLinearVelocity.setToNaN();

      alphaSwingSingularityAvoidance.set(0.0);

      unachievedSwingTranslation.setToZero(unachievedSwingTranslation.getReferenceFrame());
      unachievedSwingVelocity.setToZero(unachievedSwingVelocity.getReferenceFrame());
      unachievedSwingAcceleration.setToZero(unachievedSwingAcceleration.getReferenceFrame());
      
      if (visualize)
      {
         yoDesiredFootPositionGraphic.hideGraphicObject();
         yoCorrectedDesiredFootPositionGraphic.hideGraphicObject();
         if (moreVisualizers)
         {
            virtualLegTangentialFrameHipCenteredGraphics.update();
            virtualLegTangentialFrameAnkleCenteredGraphics.update();
            yoDesiredFootLinearVelocityGraphic.hideGraphicObject();
            yoCorrectedDesiredFootLinearVelocityGraphic.hideGraphicObject();
         }
      }
   }
   
   public void setCheckVelocityForSwingSingularityAvoidance(boolean value)
   {
      checkVelocityForSwingSingularityAvoidance.set(value);
   }

   public double updateAndGetLegLength()
   {
      anklePosition.setToZero(endEffectorFrame);
      anklePosition.changeFrame(worldFrame);
      yoCurrentFootPosition.set(anklePosition);
      anklePosition.changeFrame(virtualLegTangentialFrameHipCentered);
      currentLegLength.set(-anklePosition.getZ());
      
      return currentLegLength.getDoubleValue();
   }

   public void correctSwingFootTrajectoryForSingularityAvoidance(FramePoint desiredFootPositionToCorrect, FrameVector desiredFootLinearVelocityToCorrect, FrameVector desiredFootLinearAccelerationToCorrect)
   {
      isSwingSingularityAvoidanceUsed.set(false);
      alphaSwingSingularityAvoidance.set(0.0);
      
      yoDesiredFootPosition.set(desiredFootPositionToCorrect);
      yoDesiredFootLinearVelocity.set(desiredFootLinearVelocityToCorrect);
      
      if (!USE_SINGULARITY_AVOIDANCE_SWING)
         return;

      anklePosition.setToZero(endEffectorFrame);
      anklePosition.changeFrame(worldFrame);
      yoCurrentFootPosition.set(anklePosition);
      anklePosition.changeFrame(virtualLegTangentialFrameHipCentered);
      currentLegLength.set(-anklePosition.getZ());
      currentPercentOfLegLength.set(currentLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      
      // Check if the leg is extended such as the trajectory needs to be corrected
      desiredFootPosition.setIncludingFrame(desiredFootPositionToCorrect);
      desiredFootLinearAcceleration.setIncludingFrame(desiredFootLinearAccelerationToCorrect);
      desiredFootLinearVelocity.setIncludingFrame(desiredFootLinearVelocityToCorrect);
      
      desiredFootPosition.changeFrame(virtualLegTangentialFrameHipCentered);
      desiredFootLinearAcceleration.changeFrame(virtualLegTangentialFrameHipCentered);
      desiredFootLinearVelocity.changeFrame(virtualLegTangentialFrameHipCentered);
      
      desiredLegLength.set(-desiredFootPosition.getZ());
      desiredPercentOfLegLength.set(desiredLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      correctedDesiredLegLength.set(desiredLegLength.getDoubleValue());
      correctedDesiredPercentOfLegLength.set(desiredPercentOfLegLength.getDoubleValue());
      
      if (desiredPercentOfLegLength.getDoubleValue() < percentOfLegLengthThresholdToEnableSingularityAvoidance.getDoubleValue())
         return;
      
      desiredFootLinearVelocity.changeFrame(virtualLegTangentialFrameAnkleCentered);
      twistCalculator.packTwistOfBody(pelvisTwist, pelvis);
      pelvisTwist.changeFrame(virtualLegTangentialFrameAnkleCentered);
      pelvisTwist.packLinearPart(pelvisLinearVelocity);
      if (checkVelocityForSwingSingularityAvoidance.getBooleanValue() && (desiredFootLinearVelocity.getZ() > 0.0))
         return;
         
      checkVelocityForSwingSingularityAvoidance.set(false);
      isSwingSingularityAvoidanceUsed.set(true);
      
      alphaSwingSingularityAvoidance.set((desiredPercentOfLegLength.getDoubleValue() - percentOfLegLengthThresholdToEnableSingularityAvoidance.getDoubleValue())
            / (maxPercentOfLegLengthForSingularityAvoidanceInSwing.getDoubleValue() - percentOfLegLengthThresholdToEnableSingularityAvoidance.getDoubleValue()));
      alphaSwingSingularityAvoidance.set(MathTools.clipToMinMax(alphaSwingSingularityAvoidance.getDoubleValue(), 0.0, 1.0));

      double desiredOrMaxLegLength = - Math.min(desiredLegLength.getDoubleValue(), maxPercentOfLegLengthForSingularityAvoidanceInSwing.getDoubleValue() * maximumLegLength.getDoubleValue());
      double correctedDesiredPositionZ = desiredOrMaxLegLength; //(1.0 - alphaSingularityAvoidance.getDoubleValue()) * desiredFootPosition.getZ() + alphaSingularityAvoidance.getDoubleValue() * desiredOrMaxLegLength;
      unachievedSwingTranslation.setIncludingFrame(desiredFootPosition.getReferenceFrame(), 0.0, 0.0, desiredFootPosition.getZ() - correctedDesiredPositionZ);
      desiredFootPosition.setZ(correctedDesiredPositionZ);
      
      correctedDesiredLegLength.set(Math.abs(correctedDesiredPositionZ));
      correctedDesiredPercentOfLegLength.set(correctedDesiredLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      
//      if (desiredFootLinearVelocity.getZ() - pelvisLinearVelocity.getZ() < 0.0) // Check if desired velocity results in leg extension
      {
         double desiredLinearVelocityX = desiredFootLinearVelocity.getX();
         double desiredLinearVelocityY = desiredFootLinearVelocity.getY();
         // Mix the desired leg extension velocity to progressively follow the pelvis velocity as the the leg is more straight
         double desiredLinearVelocityZ = (1.0 - alphaSwingSingularityAvoidance.getDoubleValue()) * desiredFootLinearVelocity.getZ() + alphaSwingSingularityAvoidance.getDoubleValue() * pelvisLinearVelocity.getZ();

         unachievedSwingVelocity.setIncludingFrame(desiredFootLinearVelocity.getReferenceFrame(), 0.0, 0.0, desiredFootLinearVelocity.getZ() - desiredLinearVelocityZ);
         desiredFootLinearVelocity.setIncludingFrame(virtualLegTangentialFrameAnkleCentered, desiredLinearVelocityX, desiredLinearVelocityY, desiredLinearVelocityZ);
      }
      
//      if (desiredFootLinearAcceleration.getZ() < 0.0) // Check if desired acceleration results in leg extension
      {
         unachievedSwingAcceleration.setIncludingFrame(desiredFootLinearVelocity.getReferenceFrame(), 0.0, 0.0, alphaSwingSingularityAvoidance.getDoubleValue() * desiredFootLinearAcceleration.getZ());
         desiredFootLinearAcceleration.setZ((1.0 - alphaSwingSingularityAvoidance.getDoubleValue()) * desiredFootLinearAcceleration.getZ());
      }
      
      desiredFootPosition.changeFrame(desiredFootPositionToCorrect.getReferenceFrame());
      desiredFootLinearVelocity.changeFrame(desiredFootLinearVelocityToCorrect.getReferenceFrame());
      desiredFootLinearAcceleration.changeFrame(desiredFootLinearAccelerationToCorrect.getReferenceFrame());

      desiredFootPositionToCorrect.set(desiredFootPosition);
      desiredFootLinearVelocityToCorrect.set(desiredFootLinearVelocity);
      desiredFootLinearAccelerationToCorrect.set(desiredFootLinearAcceleration);

      yoCorrectedDesiredFootPosition.set(desiredFootPositionToCorrect);
      yoCorrectedDesiredFootLinearVelocity.set(desiredFootLinearVelocityToCorrect);

      if (visualize)
      {
         yoDesiredFootPositionGraphic.showGraphicObject();
         yoCorrectedDesiredFootPositionGraphic.showGraphicObject();
         if (moreVisualizers)
         {
         yoDesiredFootLinearVelocityGraphic.showGraphicObject();
         yoCorrectedDesiredFootLinearVelocityGraphic.showGraphicObject();
         }
      }
   }

   public void correctCoMHeightTrajectoryForSupportLeg(FrameVector2d comXYVelocity, CoMHeightTimeDerivativesData comHeightDataToCorrect,
         double zCurrent, ReferenceFrame pelvisZUpFrame, double footLoadPercentage, ConstraintType constraintType)
   {
      correctCoMHeightTrajectoryForSingularityAvoidance(comXYVelocity, comHeightDataToCorrect, zCurrent, pelvisZUpFrame, constraintType);
      
      if (!isSupportSingularityAvoidanceUsed.getBooleanValue())
         correctCoMHeightTrajectoryForCollapseAvoidance(comXYVelocity, comHeightDataToCorrect, zCurrent, pelvisZUpFrame, footLoadPercentage, constraintType);
   }
   
   public void correctCoMHeightTrajectoryForSingularityAvoidance(FrameVector2d comXYVelocity, CoMHeightTimeDerivativesData comHeightDataToCorrect,
         double zCurrent, ReferenceFrame pelvisZUpFrame, ConstraintType constraintType)
   {
      if (!USE_SINGULARITY_AVOIDANCE_SUPPORT)
      {
         alphaSupportSingularityAvoidance.set(0.0);
         isSupportSingularityAvoidanceUsed.set(false);
         doSmoothTransitionOutOfSingularityAvoidance.set(false);
         return;
      }
      
      comHeightDataToCorrect.getComHeight(desiredCenterOfMassHeightPoint);
      desiredCenterOfMassHeightPoint.changeFrame(worldFrame);
      equivalentDesiredHipPitchHeightTranslation.setIncludingFrame(worldFrame, 0.0, 0.0, desiredCenterOfMassHeightPoint.getZ() - zCurrent);
      equivalentDesiredHipPitchHeightTranslation.changeFrame(virtualLegTangentialFrameAnkleCentered);

      equivalentDesiredHipVelocity.setIncludingFrame(worldFrame, 0.0, 0.0, comHeightDataToCorrect.getComHeightVelocity());
      equivalentDesiredHipVelocity.changeFrame(pelvisZUpFrame);
      comXYVelocity.changeFrame(pelvisZUpFrame);
      equivalentDesiredHipVelocity.setX(comXYVelocity.getX());
      equivalentDesiredHipVelocity.changeFrame(virtualLegTangentialFrameAnkleCentered);

      equivalentDesiredHipPitchAcceleration.setIncludingFrame(worldFrame, 0.0, 0.0, comHeightDataToCorrect.getComHeightAcceleration());
      equivalentDesiredHipPitchAcceleration.changeFrame(virtualLegTangentialFrameAnkleCentered);

      desiredLegLength.set(equivalentDesiredHipPitchHeightTranslation.getZ() + currentLegLength.getDoubleValue());
      desiredPercentOfLegLength.set(desiredLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      correctedDesiredLegLength.set(desiredLegLength.getDoubleValue());
      correctedDesiredPercentOfLegLength.set(desiredPercentOfLegLength.getDoubleValue());

      if (constraintType != ConstraintType.FULL && constraintType != ConstraintType.HOLD_POSITION)
      {
         alphaSupportSingularityAvoidance.set(0.0);
         doSmoothTransitionOutOfSingularityAvoidance.set(isSupportSingularityAvoidanceUsed.getBooleanValue());
         if (!isSupportSingularityAvoidanceUsed.getBooleanValue())
            return;
      }
      
      if (isSupportSingularityAvoidanceUsed.getBooleanValue() || doSmoothTransitionOutOfSingularityAvoidance.getBooleanValue())
      {
         if (desiredPercentOfLegLength.getDoubleValue() < percentOfLegLengthThresholdToDisableSingularityAvoidance.getDoubleValue())
         {
            if (!doSmoothTransitionOutOfSingularityAvoidance.getBooleanValue())
            {
               alphaSupportSingularityAvoidance.set(0.0);
               doSmoothTransitionOutOfSingularityAvoidance.set(true);
            }
         }
      }
      
      // Check if the leg is extended such as the trajectory needs to be corrected
      if (desiredPercentOfLegLength.getDoubleValue() < percentOfLegLengthThresholdToEnableSingularityAvoidance.getDoubleValue())
      {
         if (!isSupportSingularityAvoidanceUsed.getBooleanValue() && !doSmoothTransitionOutOfSingularityAvoidance.getBooleanValue())
            return;
      }
      else if (!isSupportSingularityAvoidanceUsed.getBooleanValue())
      {
         isSupportSingularityAvoidanceUsed.set(true);
         doSmoothTransitionOutOfSingularityAvoidance.set(false);
         timeSwitchSingularityAvoidance.set(yoTime.getDoubleValue());
         
         heightCorrectedFilteredForSingularityAvoidance.reset();
         heightVelocityCorrectedFilteredForSingularityAvoidance.reset();
         heightAcceleretionCorrectedFilteredForSingularityAvoidance.reset();
         heightCorrectedFilteredForSingularityAvoidance.update(desiredCenterOfMassHeightPoint.getZ());
         heightVelocityCorrectedFilteredForSingularityAvoidance.update(comHeightDataToCorrect.getComHeightVelocity());
         heightAcceleretionCorrectedFilteredForSingularityAvoidance.update(comHeightDataToCorrect.getComHeightAcceleration());
      }

      if (doSmoothTransitionOutOfSingularityAvoidance.getBooleanValue())
      {
         heightCorrectedFilteredForSingularityAvoidance.update(desiredCenterOfMassHeightPoint.getZ());
         heightVelocityCorrectedFilteredForSingularityAvoidance.set(comHeightDataToCorrect.getComHeightVelocity());
         heightAcceleretionCorrectedFilteredForSingularityAvoidance.set(comHeightDataToCorrect.getComHeightAcceleration());

         comHeightDataToCorrect.setComHeight(desiredCenterOfMassHeightPoint.getReferenceFrame(), heightCorrectedFilteredForSingularityAvoidance.getDoubleValue());
         comHeightDataToCorrect.setComHeightVelocity(heightVelocityCorrectedFilteredForSingularityAvoidance.getDoubleValue());
         comHeightDataToCorrect.setComHeightAcceleration(heightAcceleretionCorrectedFilteredForSingularityAvoidance.getDoubleValue());
         
         if (Math.abs(desiredCenterOfMassHeightPoint.getZ() - heightCorrectedFilteredForSingularityAvoidance.getDoubleValue()) <= 5e-3)
         {
            alphaSupportSingularityAvoidance.set(0.0);
            isSupportSingularityAvoidanceUsed.set(false);
            doSmoothTransitionOutOfSingularityAvoidance.set(false);
         }
         return;
      }

      anklePosition.setToZero(endEffectorFrame);
      anklePosition.changeFrame(worldFrame);
      yoCurrentFootPosition.set(anklePosition);
      anklePosition.changeFrame(virtualLegTangentialFrameHipCentered);
      currentLegLength.set(-anklePosition.getZ());
      currentPercentOfLegLength.set(currentLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      
      alphaSupportSingularityAvoidance.set((desiredPercentOfLegLength.getDoubleValue() - percentOfLegLengthThresholdToEnableSingularityAvoidance.getDoubleValue())
            / (maxPercentOfLegLengthForSingularityAvoidanceInSupport.getDoubleValue() - percentOfLegLengthThresholdToEnableSingularityAvoidance.getDoubleValue()));
      alphaSupportSingularityAvoidance.set(MathTools.clipToMinMax(alphaSupportSingularityAvoidance.getDoubleValue(), 0.0, 1.0));

      double desiredOrMaxLegLength = Math.min(desiredLegLength.getDoubleValue(), maxPercentOfLegLengthForSingularityAvoidanceInSupport.getDoubleValue() * maximumLegLength.getDoubleValue());
      double correctedDesiredTranslationZ = desiredOrMaxLegLength - currentLegLength.getDoubleValue();
//      double correctedDesiredTranslationZ = (1.0 - alphaSingularityAvoidance.getDoubleValue()) * equivalentDesiredHipPitchHeightTranslation.getZ() + alphaSingularityAvoidance.getDoubleValue() * (desiredOrMaxLegLength - currentLegLength.getDoubleValue());
      equivalentDesiredHipPitchHeightTranslation.setZ(correctedDesiredTranslationZ);
      equivalentDesiredHipPitchHeightTranslation.changeFrame(worldFrame);

      correctedDesiredLegLength.set(correctedDesiredTranslationZ + currentLegLength.getDoubleValue());
      correctedDesiredPercentOfLegLength.set(correctedDesiredLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());

      desiredCenterOfMassHeightPoint.setZ(zCurrent + equivalentDesiredHipPitchHeightTranslation.getZ());
      heightCorrectedFilteredForSingularityAvoidance.update(desiredCenterOfMassHeightPoint.getZ());
      comHeightDataToCorrect.setComHeight(desiredCenterOfMassHeightPoint.getReferenceFrame(), heightCorrectedFilteredForSingularityAvoidance.getDoubleValue());

      if (equivalentDesiredHipVelocity.getZ() > 0.0) // Check if desired velocity results in leg extension
      {
         equivalentDesiredHipVelocity.setZ((1.0 - alphaSupportSingularityAvoidance.getDoubleValue()) * equivalentDesiredHipVelocity.getZ());
         equivalentDesiredHipVelocity.changeFrame(pelvisZUpFrame);
         if (Math.abs(comXYVelocity.getX()) > 1e-3 && Math.abs(equivalentDesiredHipVelocity.getX()) > 1e-3)
            equivalentDesiredHipVelocity.scale(comXYVelocity.getX() / equivalentDesiredHipVelocity.getX());
         equivalentDesiredHipVelocity.changeFrame(worldFrame);
         heightVelocityCorrectedFilteredForSingularityAvoidance.update(equivalentDesiredHipVelocity.getZ());
         comHeightDataToCorrect.setComHeightVelocity(heightVelocityCorrectedFilteredForSingularityAvoidance.getDoubleValue());
      }

      if (equivalentDesiredHipPitchAcceleration.getZ() > 0.0) // Check if desired acceleration results in leg extension
      {
         equivalentDesiredHipPitchAcceleration.setZ((1.0 - alphaSupportSingularityAvoidance.getDoubleValue()) * equivalentDesiredHipPitchAcceleration.getZ());
         equivalentDesiredHipPitchAcceleration.changeFrame(worldFrame);
         heightAcceleretionCorrectedFilteredForSingularityAvoidance.update(equivalentDesiredHipPitchAcceleration.getZ());
         comHeightDataToCorrect.setComHeightAcceleration(heightAcceleretionCorrectedFilteredForSingularityAvoidance.getDoubleValue());
      }
   }
   
   public void correctCoMHeightTrajectoryForCollapseAvoidance(FrameVector2d comXYVelocity, CoMHeightTimeDerivativesData comHeightDataToCorrect,
         double zCurrent, ReferenceFrame pelvisZUpFrame, double footLoadPercentage, ConstraintType constraintType)
   {
      if (!USE_COLLAPSE_AVOIDANCE)
      {
         alphaCollapseAvoidance.set(0.0);
         isSupportCollapseAvoidanceUsed.set(false);
         doSmoothTransitionOutOfCollapseAvoidance.set(false);
         return;
      }
      
      comHeightDataToCorrect.getComHeight(desiredCenterOfMassHeightPoint);
      desiredCenterOfMassHeightPoint.changeFrame(worldFrame);
      equivalentDesiredHipPitchHeightTranslation.setIncludingFrame(worldFrame, 0.0, 0.0, desiredCenterOfMassHeightPoint.getZ() - zCurrent);
      equivalentDesiredHipPitchHeightTranslation.changeFrame(virtualLegTangentialFrameAnkleCentered);

      equivalentDesiredHipVelocity.setIncludingFrame(worldFrame, 0.0, 0.0, comHeightDataToCorrect.getComHeightVelocity());
      equivalentDesiredHipVelocity.changeFrame(pelvisZUpFrame);
      comXYVelocity.changeFrame(pelvisZUpFrame);
      equivalentDesiredHipVelocity.setX(comXYVelocity.getX());
      equivalentDesiredHipVelocity.changeFrame(worldFrame);
      equivalentDesiredHipVelocity.changeFrame(virtualLegTangentialFrameAnkleCentered);

      equivalentDesiredHipPitchAcceleration.setIncludingFrame(worldFrame, 0.0, 0.0, comHeightDataToCorrect.getComHeightAcceleration());
      equivalentDesiredHipPitchAcceleration.changeFrame(virtualLegTangentialFrameAnkleCentered);

      desiredLegLength.set(equivalentDesiredHipPitchHeightTranslation.getZ() + currentLegLength.getDoubleValue());
      desiredPercentOfLegLength.set(desiredLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      correctedDesiredLegLength.set(desiredLegLength.getDoubleValue());
      correctedDesiredPercentOfLegLength.set(desiredPercentOfLegLength.getDoubleValue());

      if (constraintType != ConstraintType.FULL && constraintType != ConstraintType.HOLD_POSITION)
      {
         alphaCollapseAvoidance.set(0.0);
         doSmoothTransitionOutOfCollapseAvoidance.set(isSupportCollapseAvoidanceUsed.getBooleanValue());
         timeRemainingToDisableCollapseAvoidance.set(0.0);	
         if (!isSupportCollapseAvoidanceUsed.getBooleanValue())
            return;
      }
      
      if (!isSupportCollapseAvoidanceUsed.getBooleanValue() && !doSmoothTransitionOutOfCollapseAvoidance.getBooleanValue())
      {
         if (footLoadPercentage < footLoadThresholdToEnableCollapseAvoidance.getDoubleValue())
            return;
      }
      else if (footLoadPercentage < footLoadThresholdToDisableCollapseAvoidance.getDoubleValue())
      {
         timeRemainingToDisableCollapseAvoidance.sub(controlDT);
         timeRemainingToDisableCollapseAvoidance.set(Math.max(0.0, timeRemainingToDisableCollapseAvoidance.getDoubleValue()));
         if (timeRemainingToDisableCollapseAvoidance.getDoubleValue() <= 0.0 && !doSmoothTransitionOutOfCollapseAvoidance.getBooleanValue())
         {
            alphaCollapseAvoidance.set(0.0);
            doSmoothTransitionOutOfCollapseAvoidance.set(true);
         }
      }
      else
      {
         timeRemainingToDisableCollapseAvoidance.set(timeDelayToDisableCollapseAvoidance.getDoubleValue());
         doSmoothTransitionOutOfCollapseAvoidance.set(false);
      }
      
      // Check if there is a risk that the support knee collapse
      if (desiredPercentOfLegLength.getDoubleValue() > percentOfLegLengthThresholdForCollapseAvoidance.getDoubleValue())
      {
         alphaCollapseAvoidance.set(0.0);
         doSmoothTransitionOutOfCollapseAvoidance.set(isSupportCollapseAvoidanceUsed.getBooleanValue());
         timeRemainingToDisableCollapseAvoidance.set(0.0);
         if (!isSupportCollapseAvoidanceUsed.getBooleanValue())
            return;
      }
      else if (!isSupportCollapseAvoidanceUsed.getBooleanValue())
      {
         isSupportCollapseAvoidanceUsed.set(true);
         doSmoothTransitionOutOfCollapseAvoidance.set(false);
         timeRemainingToDisableCollapseAvoidance.set(timeDelayToDisableCollapseAvoidance.getDoubleValue());
         timeSwitchCollapseAvoidance.set(yoTime.getDoubleValue());
         
         heightCorrectedFilteredForCollapseAvoidance.reset();
         heightVelocityCorrectedFilteredForCollapseAvoidance.reset();
         heightAcceleretionCorrectedFilteredForCollapseAvoidance.reset();
         heightCorrectedFilteredForCollapseAvoidance.update(desiredCenterOfMassHeightPoint.getZ());
         heightVelocityCorrectedFilteredForCollapseAvoidance.update(comHeightDataToCorrect.getComHeightVelocity());
         heightAcceleretionCorrectedFilteredForCollapseAvoidance.update(comHeightDataToCorrect.getComHeightAcceleration());
      }
      
      if (doSmoothTransitionOutOfCollapseAvoidance.getBooleanValue())
      {
         heightCorrectedFilteredForCollapseAvoidance.update(desiredCenterOfMassHeightPoint.getZ());
         heightVelocityCorrectedFilteredForCollapseAvoidance.set(comHeightDataToCorrect.getComHeightVelocity());
         heightAcceleretionCorrectedFilteredForCollapseAvoidance.set(comHeightDataToCorrect.getComHeightAcceleration());

         comHeightDataToCorrect.setComHeight(desiredCenterOfMassHeightPoint.getReferenceFrame(), heightCorrectedFilteredForCollapseAvoidance.getDoubleValue());
         comHeightDataToCorrect.setComHeightVelocity(heightVelocityCorrectedFilteredForCollapseAvoidance.getDoubleValue());
         comHeightDataToCorrect.setComHeightAcceleration(heightAcceleretionCorrectedFilteredForCollapseAvoidance.getDoubleValue());
         
         if (Math.abs(desiredCenterOfMassHeightPoint.getZ() - heightCorrectedFilteredForCollapseAvoidance.getDoubleValue()) <= 5e-3)
         {
            alphaCollapseAvoidance.set(0.0);
            isSupportCollapseAvoidanceUsed.set(false);
            doSmoothTransitionOutOfCollapseAvoidance.set(false);
         }
         return;
      }

      anklePosition.setToZero(endEffectorFrame);
      anklePosition.changeFrame(worldFrame);
      yoCurrentFootPosition.set(anklePosition);
      anklePosition.changeFrame(virtualLegTangentialFrameHipCentered);
      currentLegLength.set(-anklePosition.getZ());
      currentPercentOfLegLength.set(currentLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());
      
      alphaCollapseAvoidance.set((desiredPercentOfLegLength.getDoubleValue() - percentOfLegLengthThresholdForCollapseAvoidance.getDoubleValue())
            / (minPercentOfLegLengthForCollapseAvoidance.getDoubleValue() - percentOfLegLengthThresholdForCollapseAvoidance.getDoubleValue()));
      alphaCollapseAvoidance.set(MathTools.clipToMinMax(alphaCollapseAvoidance.getDoubleValue(), 0.0, 1.0));

      double desiredOrMinLegLength = Math.max(desiredLegLength.getDoubleValue(), minPercentOfLegLengthForCollapseAvoidance.getDoubleValue() * maximumLegLength.getDoubleValue());
      double correctedDesiredTranslationZ = desiredOrMinLegLength - currentLegLength.getDoubleValue(); //(1.0 - alphaCollapseAvoidance.getDoubleValue()) * equivalentDesiredHipPitchHeightTranslation.getZ() + alphaCollapseAvoidance.getDoubleValue() * (desiredOrMinLegLength - currentLegLength.getDoubleValue());
      equivalentDesiredHipPitchHeightTranslation.setZ(correctedDesiredTranslationZ);
      equivalentDesiredHipPitchHeightTranslation.changeFrame(worldFrame);
      
      correctedDesiredLegLength.set(correctedDesiredTranslationZ + currentLegLength.getDoubleValue());
      correctedDesiredPercentOfLegLength.set(correctedDesiredLegLength.getDoubleValue() / maximumLegLength.getDoubleValue());

      desiredCenterOfMassHeightPoint.setZ(zCurrent + equivalentDesiredHipPitchHeightTranslation.getZ());
      heightCorrectedFilteredForCollapseAvoidance.update(desiredCenterOfMassHeightPoint.getZ());
      comHeightDataToCorrect.setComHeight(desiredCenterOfMassHeightPoint.getReferenceFrame(), heightCorrectedFilteredForCollapseAvoidance.getDoubleValue());

      if (equivalentDesiredHipVelocity.getZ() < 0.0) // Check if desired velocity results in knee flexion
      {
         equivalentDesiredHipVelocity.setZ((1.0 - alphaCollapseAvoidance.getDoubleValue()) * equivalentDesiredHipVelocity.getZ());
         equivalentDesiredHipVelocity.changeFrame(pelvisZUpFrame);
         if (Math.abs(comXYVelocity.getX()) > 1e-3 && Math.abs(equivalentDesiredHipVelocity.getX()) > 1e-3)
            equivalentDesiredHipVelocity.scale(comXYVelocity.getX() / equivalentDesiredHipVelocity.getX());
         equivalentDesiredHipVelocity.changeFrame(worldFrame);
         heightVelocityCorrectedFilteredForCollapseAvoidance.update(equivalentDesiredHipVelocity.getZ());
         comHeightDataToCorrect.setComHeightVelocity(heightVelocityCorrectedFilteredForCollapseAvoidance.getDoubleValue());
      }
      else
      {
         heightVelocityCorrectedFilteredForCollapseAvoidance.reset();
         heightVelocityCorrectedFilteredForCollapseAvoidance.update(comHeightDataToCorrect.getComHeightVelocity());
      }

      if (equivalentDesiredHipPitchAcceleration.getZ() < 0.0) // Check if desired acceleration results in knee flexion
      {
         equivalentDesiredHipPitchAcceleration.setZ((1.0 - alphaCollapseAvoidance.getDoubleValue()) * equivalentDesiredHipPitchAcceleration.getZ());
         equivalentDesiredHipPitchAcceleration.changeFrame(worldFrame);
         heightAcceleretionCorrectedFilteredForCollapseAvoidance.update(equivalentDesiredHipPitchAcceleration.getZ());
         comHeightDataToCorrect.setComHeightAcceleration(heightAcceleretionCorrectedFilteredForCollapseAvoidance.getDoubleValue());
      }
      else
      {
         heightAcceleretionCorrectedFilteredForCollapseAvoidance.reset();
         heightAcceleretionCorrectedFilteredForCollapseAvoidance.update(comHeightDataToCorrect.getComHeightAcceleration());
      }
   }
   
   public void correctCoMHeightTrajectoryForUnreachableFootStep(CoMHeightTimeDerivativesData comHeightDataToCorrect, ConstraintType constraintType)
   {
      isUnreachableFootstepCompensated.set(false);
      
      if (!USE_UNREACHABLE_FOOTSTEP_CORRECTION)
         return;

      if (constraintType != ConstraintType.SWING)
         return;
      
      comHeightDataToCorrect.getComHeight(desiredCenterOfMassHeightPoint);
      desiredCenterOfMassHeightPoint.changeFrame(worldFrame);

      unachievedSwingTranslation.changeFrame(worldFrame);
      if (unachievedSwingTranslation.getZ() < 0.0)
      {
         isUnreachableFootstepCompensated.set(true);
         unachievedSwingTranslationFiltered.update(unachievedSwingTranslation.getZ());
         desiredCenterOfMassHeightPoint.setZ(desiredCenterOfMassHeightPoint.getZ() + unachievedSwingTranslationFiltered.getDoubleValue());
      }
      else
      {
         unachievedSwingTranslationFiltered.set(0.0);
      }
      
      unachievedSwingVelocity.changeFrame(worldFrame);
      if (unachievedSwingVelocity.getZ() < 0.0)
      {
         unachievedSwingVelocityFiltered.update(unachievedSwingVelocity.getZ());
         comHeightDataToCorrect.setComHeightVelocity(comHeightDataToCorrect.getComHeightVelocity() + unachievedSwingVelocityFiltered.getDoubleValue());
      }
      else
      {
         unachievedSwingVelocityFiltered.set(0.0);
      }
      
      if (unachievedSwingAcceleration.getZ() < 0.0)
      {
         unachievedSwingAccelerationFiltered.update(unachievedSwingAcceleration.getZ());
         comHeightDataToCorrect.setComHeightAcceleration(comHeightDataToCorrect.getComHeightAcceleration() + unachievedSwingAccelerationFiltered.getDoubleValue());
      }
      else
      {
         unachievedSwingAccelerationFiltered.set(0.0);
      }
   }
}
