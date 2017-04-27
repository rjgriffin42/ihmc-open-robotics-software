package us.ihmc.commonWalkingControlModules.controlModules.foot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.toeOffCalculator.ToeOffCalculator;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.*;
import us.ihmc.robotics.math.filters.GlitchFilteredBooleanYoVariable;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class ToeOffManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final boolean DO_TOEOFF_FOR_SIDE_STEPS = false;
   private static final boolean ENABLE_TOE_OFF_FOR_STEP_DOWN = true;

   private static final double forwardSteppingThreshold = -0.05;
   private static final double stepDownTooFarForToeOff = -0.10;
   private static final double minimumAngleForSideStepping = 45.0;
   private static final double extraCoMHeightWithToes = 0.08;

   private static final int largeGlitchWindowSize = 10;
   private static final int smallGlitchWindowSize = 2;

   private final BooleanYoVariable doToeOffIfPossibleInDoubleSupport = new BooleanYoVariable("doToeOffIfPossibleInDoubleSupport", registry);
   private final BooleanYoVariable doToeOffIfPossibleInSingleSupport = new BooleanYoVariable("doToeOffIfPossibleInSingleSupport", registry);
   private final BooleanYoVariable doToeOffWhenHittingAnkleLimit = new BooleanYoVariable("doToeOffWhenHittingAnkleLimit", registry);
   private final BooleanYoVariable doPointToeOff = new BooleanYoVariable("doPointToeOff", registry);
   private final BooleanYoVariable doLineToeOff = new BooleanYoVariable("doLineToeOff", registry);

   private final BooleanYoVariable useToeLineContactInSwing = new BooleanYoVariable("useToeLineContactInSwing", registry);
   private final BooleanYoVariable useToeLineContactInTransfer = new BooleanYoVariable("useToeLineContactInTransfer", registry);
   private final BooleanYoVariable computeToeLineContact = new BooleanYoVariable("computeToeLineContact", registry);
   private final BooleanYoVariable computeToePointContact = new BooleanYoVariable("computeToePointContact", registry);
   private final BooleanYoVariable updateLineContactDuringToeOff = new BooleanYoVariable("updateLineContactDuringToeOff", registry);
   private final BooleanYoVariable updatePointContactDuringToeOff = new BooleanYoVariable("updatePointContactDuringToeOff", registry);

   private final DoubleYoVariable ankleLowerLimitToTriggerToeOff = new DoubleYoVariable("ankleLowerLimitToTriggerToeOff", registry);
   private final DoubleYoVariable icpPercentOfStanceForDSToeOff = new DoubleYoVariable("icpPercentOfStanceForDSToeOff", registry);
   private final DoubleYoVariable icpPercentOfStanceForSSToeOff = new DoubleYoVariable("icpPercentOfStanceForSSToeOff", registry);
   private final DoubleYoVariable ecmpProximityForToeOff = new DoubleYoVariable("ecmpProximityForToeOff", registry);

   private final BooleanYoVariable isDesiredICPOKForToeOff = new BooleanYoVariable("isDesiredICPOKForToeOff", registry);
   private final BooleanYoVariable isCurrentICPOKForToeOff = new BooleanYoVariable("isCurrentICPOKForToeOff", registry);
   private final BooleanYoVariable isDesiredECMPOKForToeOff = new BooleanYoVariable("isDesiredECMPOKForToeOff", registry);
   private final BooleanYoVariable needToSwitchToToeOffForAnkleLimit = new BooleanYoVariable("needToSwitchToToeOffForAnkleLimit", registry);
   private final BooleanYoVariable isRearAnklePitchHittingLimit = new BooleanYoVariable("isRearAnklePitchHittingLimit", registry);

   private final GlitchFilteredBooleanYoVariable isDesiredICPOKForToeOffFilt = new GlitchFilteredBooleanYoVariable("isDesiredICPOKForToeOffFilt",
         registry, isDesiredICPOKForToeOff, smallGlitchWindowSize);
   private final GlitchFilteredBooleanYoVariable isCurrentICPOKForToeOffFilt = new GlitchFilteredBooleanYoVariable("isCurrentICPOKForToeOffFilt",
         registry, isCurrentICPOKForToeOff, smallGlitchWindowSize);
   private final GlitchFilteredBooleanYoVariable isDesiredECMPOKForToeOffFilt = new GlitchFilteredBooleanYoVariable("isDesiredECMPOKForToeOffFilt",
         registry, isDesiredECMPOKForToeOff, smallGlitchWindowSize);

   private final GlitchFilteredBooleanYoVariable isRearAnklePitchHittingLimitFilt = new GlitchFilteredBooleanYoVariable("isRearAnklePitchHittingLimitFilt",
         registry, isRearAnklePitchHittingLimit, largeGlitchWindowSize);

   private final DoubleYoVariable minStepLengthForToeOff = new DoubleYoVariable("minStepLengthForToeOff", registry);
   private final DoubleYoVariable minStepHeightForToeOff = new DoubleYoVariable("minStepHeightForToeOff", registry);
   private final DoubleYoVariable extraCoMMaxHeightWithToes = new DoubleYoVariable("extraCoMMaxHeightWithToes", registry);

   private final SideDependentList<YoPlaneContactState> footContactStates;
   private final List<FramePoint> contactStatePoints = new ArrayList<>();

   private final SideDependentList<? extends ContactablePlaneBody> feet;
   private final SideDependentList<FrameConvexPolygon2d> footDefaultPolygons;
   private final FrameConvexPolygon2d leadingFootSupportPolygon = new FrameConvexPolygon2d();
   private final FrameConvexPolygon2d onToesSupportPolygon = new FrameConvexPolygon2d();

   private final FramePoint tempLeadingFootPosition = new FramePoint();
   private final FramePoint tempTrailingFootPosition = new FramePoint();
   private final FramePoint tempLeadingFootPositionInWorld = new FramePoint();
   private final FramePoint tempTrailingFootPositionInWorld = new FramePoint();
   private final FrameVector toLeadingFoot = new FrameVector();

   private final HashMap<ToeContact, AbstractToeContact> toeContacts = new HashMap<>();

   private Footstep nextFootstep;

   private final WalkingControllerParameters walkingControllerParameters;

   private final FullHumanoidRobotModel fullRobotModel;
   private final ToeOffCalculator toeOffCalculator;

   private final double inPlaceWidth;
   private final double footLength;

   public ToeOffManager(HighLevelHumanoidControllerToolbox controllerToolbox, ToeOffCalculator toeOffCalculator,
         WalkingControllerParameters walkingControllerParameters, SideDependentList<? extends ContactablePlaneBody> feet, YoVariableRegistry parentRegistry)
   {
      this(controllerToolbox.getFullRobotModel(), toeOffCalculator, walkingControllerParameters, feet, createFootContactStates(controllerToolbox), parentRegistry);
   }

   public ToeOffManager(FullHumanoidRobotModel fullRobotModel, ToeOffCalculator toeOffCalculator,
         WalkingControllerParameters walkingControllerParameters, SideDependentList<? extends ContactablePlaneBody> feet,
         SideDependentList<YoPlaneContactState> footContactStates, YoVariableRegistry parentRegistry)
   {
      this.doToeOffIfPossibleInDoubleSupport.set(walkingControllerParameters.doToeOffIfPossible());
      this.doToeOffIfPossibleInSingleSupport.set(walkingControllerParameters.doToeOffIfPossibleInSingleSupport());
      this.doToeOffWhenHittingAnkleLimit.set(walkingControllerParameters.doToeOffWhenHittingAnkleLimit());

      this.ankleLowerLimitToTriggerToeOff.set(walkingControllerParameters.getAnkleLowerLimitToTriggerToeOff());
      this.icpPercentOfStanceForDSToeOff.set(walkingControllerParameters.getICPPercentOfStanceForDSToeOff());
      this.icpPercentOfStanceForSSToeOff.set(walkingControllerParameters.getICPPercentOfStanceForSSToeOff());

      this.ecmpProximityForToeOff.set(walkingControllerParameters.getECMPProximityForToeOff());

      this.toeOffCalculator = toeOffCalculator;
      this.walkingControllerParameters = walkingControllerParameters;

      this.fullRobotModel = fullRobotModel;
      this.feet = feet;

      this.inPlaceWidth = walkingControllerParameters.getInPlaceWidth();
      this.footLength = walkingControllerParameters.getFootBackwardOffset() + walkingControllerParameters.getFootForwardOffset();

      extraCoMMaxHeightWithToes.set(extraCoMHeightWithToes);

      minStepLengthForToeOff.set(walkingControllerParameters.getMinStepLengthForToeOff());
      minStepHeightForToeOff.set(walkingControllerParameters.getMinStepHeightForToeOff());

      useToeLineContactInSwing.set(walkingControllerParameters.useToeOffLineContactInSwing());
      useToeLineContactInTransfer.set(walkingControllerParameters.useToeOffLineContactInTransfer());

      updateLineContactDuringToeOff.set(walkingControllerParameters.updateLineContactDuringToeOff());
      updatePointContactDuringToeOff.set(walkingControllerParameters.updatePointContactDuringToeOff());

      footDefaultPolygons = new SideDependentList<>();
      for (RobotSide robotSide : RobotSide.values)
      {
         footDefaultPolygons.put(robotSide, new FrameConvexPolygon2d(feet.get(robotSide).getContactPoints2d()));
      }

      this.footContactStates = footContactStates;

      toeContacts.put(ToeContact.LINE, new ToeLineContact());
      toeContacts.put(ToeContact.POINT, new ToePointContact());

      parentRegistry.addChild(registry);
   }

   private static SideDependentList<YoPlaneContactState> createFootContactStates(HighLevelHumanoidControllerToolbox controllerToolbox)
   {
      SideDependentList<YoPlaneContactState> footContactStates = new SideDependentList<>();
      for (RobotSide robotSide : RobotSide.values)
      {
         footContactStates.put(robotSide, controllerToolbox.getFootContactState(robotSide));
      }
      return footContactStates;
   }

   public void reset()
   {
      isDesiredECMPOKForToeOff.set(false);
      isDesiredECMPOKForToeOffFilt.set(false);

      isRearAnklePitchHittingLimit.set(false);
      isRearAnklePitchHittingLimitFilt.set(false);

      isDesiredICPOKForToeOff.set(false);
      isDesiredICPOKForToeOffFilt.set(false);

      isCurrentICPOKForToeOff.set(false);
      isCurrentICPOKForToeOffFilt.set(false);

      computeToeLineContact.set(true);
      computeToePointContact.set(true);

      doLineToeOff.set(false);
      doPointToeOff.set(false);
   }

   /**
    * Sets the upcoming footstep, which is used to predict the support polygon in single support.
    * @param nextFootstep
    */
   public void submitNextFootstep(Footstep nextFootstep)
   {
      this.nextFootstep = nextFootstep;
   }

   /**
    * <p>
    * Checks whether or not the robot state is proper for toe-off when in double support, and sets the {@link ToeOffManager#doLineToeOff} variable accordingly.
    * </p>
    * <p>
    * These checks include:
    * </p>
    * <ol>
    *   <li>doToeOffIfPossibleInDoubleSupport</li>
    *   <li>desiredECMP location being within the support polygon account for toe-off, if {@link WalkingControllerParameters#checkECMPLocationToTriggerToeOff()} is true.</li>
    *   <li>desiredICP location being within the leading foot base of support.</li>
    *   <li>currentICP location being within the leading foot base of support.</li>
    *   <li>needToSwitchToToeOffForAnkleLimit</li>
    * </ol>
    * <p>
    * If able and the ankles are at the joint limits, transitions to toe-off. Then checks the current state being with the base of support. Then checks the
    * positioning of the leading leg to determine if it is acceptable.
    * </p>
    *
    * @param desiredECMP current desired ECMP from ICP feedback.
    * @param desiredICP current desired ICP from the reference trajectory.
    * @param currentICP current ICP based on the robot state.
    */
   public void updateToeOffStatusSingleSupport(FramePoint exitCMP, FramePoint2d desiredECMP, FramePoint2d desiredICP, FramePoint2d currentICP)
   {
      if (!doToeOffIfPossibleInSingleSupport.getBooleanValue())
      {
         doLineToeOff.set(false);
         doPointToeOff.set(false);

         isDesiredICPOKForToeOff.set(false);
         isDesiredICPOKForToeOffFilt.set(false);

         isCurrentICPOKForToeOff.set(false);
         isCurrentICPOKForToeOffFilt.set(false);

         isDesiredECMPOKForToeOff.set(false);
         isDesiredECMPOKForToeOffFilt.set(false);

         computeToePointContact.set(false);
         computeToeLineContact.set(false);
         return;
      }

      RobotSide trailingLeg = nextFootstep.getRobotSide().getOppositeSide();
      double percentProximity = icpPercentOfStanceForSSToeOff.getDoubleValue();
      setLeadingFootSupportPolygonFromNextFootstep();

      AbstractToeContact toeContact;
      if (useToeLineContactInSwing.getBooleanValue())
      {
         computeToePointContact.set(false);
         toeContact = toeContacts.get(ToeContact.LINE);
      }
      else
      {
         computeToeLineContact.set(false);
         toeContact = toeContacts.get(ToeContact.POINT);
      }

      toeContact.updateToeSupportPolygon(exitCMP, desiredECMP, trailingLeg, leadingFootSupportPolygon);

      ReferenceFrame soleFrame = nextFootstep.getSoleReferenceFrame();
      checkICPLocations(trailingLeg, desiredICP, currentICP, leadingFootSupportPolygon, soleFrame, percentProximity);

      checkECMPLocation(desiredECMP);

      if (!toeContact.evaluateToeOffConditions(trailingLeg))
         return;

      toeContact.isReadyToSwitchToToeOff(trailingLeg, soleFrame);
   }

   /**
    * <p>
    * Checks whether or not the robot state is proper for toe-off when in double support, and sets the {@link ToeOffManager#doLineToeOff} variable accordingly.
    * </p>
    * <p>
    * These checks include:
    * </p>
    * <ol>
    *   <li>doToeOffIfPossibleInDoubleSupport</li>
    *   <li>desiredECMP location being within the support polygon account for toe-off, if {@link WalkingControllerParameters#checkECMPLocationToTriggerToeOff()} is true.</li>
    *   <li>desiredICP location being within the leading foot base of support.</li>
    *   <li>currentICP location being within the leading foot base of support.</li>
    *   <li>needToSwitchToToeOffForAnkleLimit</li>
    * </ol>
    * <p>
    * If able and the ankles are at the joint limits, transitions to toe-off. Then checks the current state being with the base of support. Then checks the
    * positioning of the leading leg to determine if it is acceptable.
    * </p>
    *
    * @param trailingLeg robot side for the trailing leg
    * @param desiredECMP current desired ECMP from ICP feedback.
    * @param desiredICP current desired ICP from the reference trajectory.
    * @param currentICP current ICP based on the robot state.
    */
   public void updateToeOffStatusDoubleSupport(RobotSide trailingLeg, FramePoint exitCMP, FramePoint2d desiredECMP, FramePoint2d desiredICP, FramePoint2d currentICP)
   {
      nextFootstep = null;
      if (!doToeOffIfPossibleInDoubleSupport.getBooleanValue())
      {
         doLineToeOff.set(false);
         doPointToeOff.set(false);

         isDesiredICPOKForToeOff.set(false);
         isDesiredICPOKForToeOffFilt.set(false);

         isCurrentICPOKForToeOff.set(false);
         isCurrentICPOKForToeOffFilt.set(false);

         isDesiredECMPOKForToeOff.set(false);
         isDesiredECMPOKForToeOffFilt.set(false);

         needToSwitchToToeOffForAnkleLimit.set(false);
         computeToePointContact.set(false);
         computeToeLineContact.set(false);
         return;
      }

      setLeadingFootSupportPolygon(trailingLeg);

      double percentProximity = icpPercentOfStanceForDSToeOff.getDoubleValue();

      AbstractToeContact toeContact;
      if (useToeLineContactInTransfer.getBooleanValue())
      {
         computeToePointContact.set(false);
         toeContact = toeContacts.get(ToeContact.LINE);
      }
      else
      {
         computeToeLineContact.set(false);
         toeContact = toeContacts.get(ToeContact.POINT);
      }

      toeContact.updateToeSupportPolygon(exitCMP, desiredECMP, trailingLeg, leadingFootSupportPolygon);

      ReferenceFrame soleFrame = feet.get(trailingLeg.getOppositeSide()).getFrameAfterParentJoint();
      checkICPLocations(trailingLeg, desiredICP, currentICP, leadingFootSupportPolygon, soleFrame, percentProximity);

      checkECMPLocation(desiredECMP);

      if (!toeContact.evaluateToeOffConditions(trailingLeg))
         return;

      toeContact.isReadyToSwitchToToeOff(trailingLeg, soleFrame);
   }

   /**
    * Sets the support polygon of the location of the leading support foot. Only works during transfer
    * @param trailingLeg trailing leg
    */
   private void setLeadingFootSupportPolygon(RobotSide trailingLeg)
   {
      RobotSide leadingLeg = trailingLeg.getOppositeSide();
      if (footContactStates != null && footContactStates.get(leadingLeg).getTotalNumberOfContactPoints() > 0)
      {
         footContactStates.get(leadingLeg).getContactFramePointsInContact(contactStatePoints);
         leadingFootSupportPolygon.setIncludingFrameByProjectionOntoXYPlaneAndUpdate(worldFrame, contactStatePoints);
      }
      else
      {
         leadingFootSupportPolygon.setIncludingFrameAndUpdate(footDefaultPolygons.get(leadingLeg));
         leadingFootSupportPolygon.changeFrameAndProjectToXYPlane(worldFrame);
      }
   }

   /**
    * Sets the predicted support polygon of the leading support foot from the next footstep.
    */
   private void setLeadingFootSupportPolygonFromNextFootstep()
   {
      if (nextFootstep == null)
         throw new RuntimeException("The next footstep has not been set.");

      ReferenceFrame footstepSoleFrame = nextFootstep.getSoleReferenceFrame();
      List<Point2D> predictedContactPoints = nextFootstep.getPredictedContactPoints();
      if (predictedContactPoints != null && !predictedContactPoints.isEmpty())
      {
         leadingFootSupportPolygon.setIncludingFrameAndUpdate(footstepSoleFrame, predictedContactPoints);
      }
      else
      {
         ConvexPolygon2d footPolygon = footDefaultPolygons.get(nextFootstep.getRobotSide()).getConvexPolygon2d();
         leadingFootSupportPolygon.setIncludingFrameAndUpdate(footstepSoleFrame, footPolygon);
      }
      leadingFootSupportPolygon.changeFrameAndProjectToXYPlane(worldFrame);
   }

   private void checkECMPLocation(FramePoint2d desiredECMP)
   {
      if (walkingControllerParameters.checkECMPLocationToTriggerToeOff())
      {
         desiredECMP.changeFrameAndProjectToXYPlane(onToesSupportPolygon.getReferenceFrame());
         isDesiredECMPOKForToeOff.set(onToesSupportPolygon.distance(desiredECMP) <= ecmpProximityForToeOff.getDoubleValue());
         isDesiredECMPOKForToeOffFilt.update();
      }
      else
      {
         isDesiredECMPOKForToeOff.set(true);
         isDesiredECMPOKForToeOffFilt.set(true);
      }
   }

   private void checkICPLocations(RobotSide trailingLeg, FramePoint2d desiredICP, FramePoint2d currentICP, FrameConvexPolygon2d leadingFootSupportPolygon,
         ReferenceFrame nextSoleFrame, double percentProximity)
   {
      boolean isDesiredICPOKForToeOff, isCurrentICPOKForToeOff;
      if (percentProximity > 0.0)
      {
         // compute stance length
         double requiredProximity = computeRequiredICPProximity(trailingLeg, nextSoleFrame, percentProximity);

         isDesiredICPOKForToeOff =
               onToesSupportPolygon.isPointInside(desiredICP) && leadingFootSupportPolygon.distance(desiredICP) < requiredProximity;
         isCurrentICPOKForToeOff =
               onToesSupportPolygon.isPointInside(currentICP) && leadingFootSupportPolygon.distance(currentICP) < requiredProximity;
      }
      else
      {
         isDesiredICPOKForToeOff = leadingFootSupportPolygon.isPointInside(desiredICP);
         isCurrentICPOKForToeOff = leadingFootSupportPolygon.isPointInside(currentICP);
      }

      this.isCurrentICPOKForToeOff.set(isCurrentICPOKForToeOff);
      this.isDesiredICPOKForToeOff.set(isDesiredICPOKForToeOff);
      this.isCurrentICPOKForToeOffFilt.update();
      this.isDesiredICPOKForToeOffFilt.update();
   }

   private double computeRequiredICPProximity(RobotSide trailingLeg, ReferenceFrame nextSoleFrame, double percentOfStanceForToeOff)
   {
      ReferenceFrame trailingFootFrame = feet.get(trailingLeg).getSoleFrame();
      tempLeadingFootPosition.setToZero(nextSoleFrame);
      tempTrailingFootPosition.setToZero(trailingFootFrame);
      tempLeadingFootPosition.changeFrame(trailingFootFrame);

      toLeadingFoot.setToZero(trailingFootFrame);
      toLeadingFoot.set(tempLeadingFootPosition);
      toLeadingFoot.sub(tempTrailingFootPosition);

      return percentOfStanceForToeOff * toLeadingFoot.length();
   }

   private boolean checkAnkleLimitForToeOff(RobotSide trailingLeg)
   {
      OneDoFJoint anklePitch = fullRobotModel.getLegJoint(trailingLeg, LegJointName.ANKLE_PITCH);
      double lowerLimit = Math.max(anklePitch.getJointLimitLower() + 0.02, ankleLowerLimitToTriggerToeOff.getDoubleValue()); // todo extract variable
      isRearAnklePitchHittingLimit.set(anklePitch.getQ() < lowerLimit);
      isRearAnklePitchHittingLimitFilt.update();

      if (!doToeOffWhenHittingAnkleLimit.getBooleanValue() || !isDesiredICPOKForToeOffFilt.getBooleanValue() || !isCurrentICPOKForToeOffFilt.getBooleanValue())
         return false;

      return isRearAnklePitchHittingLimitFilt.getBooleanValue();
   }

   private boolean isFrontFootWellPositionedForToeOff(RobotSide trailingLeg, ReferenceFrame frontFootFrame)
   {
      ReferenceFrame trailingFootFrame = feet.get(trailingLeg).getSoleFrame();
      tempLeadingFootPosition.setToZero(frontFootFrame);
      tempTrailingFootPosition.setToZero(trailingFootFrame);
      tempLeadingFootPosition.changeFrame(trailingFootFrame);

      if (Math.abs(tempLeadingFootPosition.getY()) > inPlaceWidth)
         tempLeadingFootPosition.setY(tempLeadingFootPosition.getY() + trailingLeg.negateIfRightSide(inPlaceWidth));
      else
         tempLeadingFootPosition.setY(0.0);

      tempLeadingFootPositionInWorld.setToZero(frontFootFrame);
      tempTrailingFootPositionInWorld.setToZero(trailingFootFrame);
      tempLeadingFootPositionInWorld.changeFrame(worldFrame);
      tempTrailingFootPositionInWorld.changeFrame(worldFrame);

      double stepHeight = tempLeadingFootPositionInWorld.getZ() - tempTrailingFootPositionInWorld.getZ();

      boolean isNextStepHighEnough = stepHeight > minStepHeightForToeOff.getDoubleValue();
      if (isNextStepHighEnough)
         return true;

      boolean isForwardOrSideStepping = tempLeadingFootPosition.getX() > forwardSteppingThreshold;
      if (!isForwardOrSideStepping)
         return false;

      if (ENABLE_TOE_OFF_FOR_STEP_DOWN)
      {
         boolean isNextStepLowEnough = stepHeight < -minStepHeightForToeOff.getDoubleValue();
         if (isNextStepLowEnough)
            return true;
      }
      else
      {
         boolean isNextStepTooLow = stepHeight < stepDownTooFarForToeOff;
         if (isNextStepTooLow)
            return false;
      }

      boolean isSideStepping = Math.abs(Math.atan2(tempLeadingFootPosition.getY(), tempLeadingFootPosition.getX())) > Math.toRadians(minimumAngleForSideStepping);
      if (isSideStepping && !DO_TOEOFF_FOR_SIDE_STEPS)
         return false;

      boolean isStepLongEnough = tempLeadingFootPosition.distance(tempTrailingFootPosition) > minStepLengthForToeOff.getDoubleValue();
      boolean isStepLongEnoughAlongX = tempLeadingFootPosition.getX() > footLength;
      return isStepLongEnough && isStepLongEnoughAlongX;
   }

   /**
    * Checks whether or not the next footstep in {@param nextFootstep} is in correct location to achieve toe off.
    * @param nextFootstep footstep to consider.
    * @param transferToSide upcoming support side.
    * @return whether or not the footstep location is ok.
    */
   public boolean canDoSingleSupportToeOff(Footstep nextFootstep, RobotSide transferToSide)
   {
      if (!doToeOffIfPossibleInSingleSupport.getBooleanValue())
         return false;

      return canDoToeOff(nextFootstep, transferToSide);
   }

   /**
    * Checks whether or not the next footstep in {@param nextFootstep} is in correct location to achieve toe off.
    * @param nextFootstep footstep to consider.
    * @param transferToSide upcoming support side.
    * @return whether or not the footstep location is ok.
    */
   public boolean canDoDoubleSupportToeOff(Footstep nextFootstep, RobotSide transferToSide)
   {
      if (!doToeOffIfPossibleInDoubleSupport.getBooleanValue())
         return false;

      return canDoToeOff(nextFootstep, transferToSide);
   }

   public boolean canDoToeOff(Footstep nextFootstep, RobotSide transferToSide)
   {
      RobotSide nextTrailingLeg = transferToSide.getOppositeSide();
      ReferenceFrame nextFrontFootFrame;
      if (nextFootstep != null)
         nextFrontFootFrame = nextFootstep.getSoleReferenceFrame();
      else
         nextFrontFootFrame = feet.get(nextTrailingLeg.getOppositeSide()).getSoleFrame();

      return isFrontFootWellPositionedForToeOff(nextTrailingLeg, nextFrontFootFrame);
   }

   public boolean doLineToeOff()
   {
      return doLineToeOff.getBooleanValue();
   }

   public boolean doPointToeOff()
   {
      return doPointToeOff.getBooleanValue();
   }

   public boolean shouldComputeToeLineContact()
   {
      return computeToeLineContact.getBooleanValue();
   }

   public boolean shouldComputeToePointContact()
   {
      return computeToePointContact.getBooleanValue();
   }

   public double getExtraCoMMaxHeightWithToes()
   {
      return extraCoMMaxHeightWithToes.getDoubleValue();
   }


   private enum ToeContact
   {
      POINT, LINE
   }

   private abstract class AbstractToeContact
   {
      protected final FrameLineSegment2d toeOffLine = new FrameLineSegment2d();
      protected final FramePoint2d toeOffPoint = new FramePoint2d();
      protected final FramePoint2d tmpPoint2d = new FramePoint2d();

      public abstract void updateToeSupportPolygon(FramePoint exitCMP, FramePoint2d desiredECMP, RobotSide trailingSide, FrameConvexPolygon2d leadingFootSupportPolygon);

      public abstract void isReadyToSwitchToToeOff(RobotSide trailingLeg, ReferenceFrame frontFootFrame);

      public abstract boolean evaluateToeOffConditions(RobotSide trailingLeg);

      protected void computeToeContacts(RobotSide supportSide)
      {
         FrameConvexPolygon2d footDefaultPolygon = footDefaultPolygons.get(supportSide);
         ReferenceFrame referenceFrame = footDefaultPolygon.getReferenceFrame();
         toeOffLine.setFirstEndpoint(referenceFrame, Double.NEGATIVE_INFINITY, 0.0);
         toeOffLine.setSecondEndpoint(referenceFrame, Double.NEGATIVE_INFINITY, 0.0);

         // gets the leading two toe points
         for (int i = 0; i < footDefaultPolygon.getNumberOfVertices(); i++)
         {
            footDefaultPolygon.getFrameVertex(i, tmpPoint2d);
            if (tmpPoint2d.getX() > toeOffLine.getFirstEndpoint().getX())
            { // further ahead than leading point
               toeOffLine.setSecondEndpoint(referenceFrame, toeOffLine.getFirstEndpoint());
               toeOffLine.setFirstEndpoint(tmpPoint2d);
            }
            else if (tmpPoint2d.getX() > toeOffLine.getSecondEndpoint().getX())
            { // further ahead than second leading point
               toeOffLine.setSecondEndpoint(tmpPoint2d);
            }
         }

         toeOffPoint.setToZero(footDefaultPolygon.getReferenceFrame());
         toeOffPoint.set(toeOffLine.midpoint());
      }
   }

   private class ToeLineContact extends AbstractToeContact
   {
      public void updateToeSupportPolygon(FramePoint exitCMP, FramePoint2d desiredECMP, RobotSide trailingSide, FrameConvexPolygon2d leadingFootSupportPolygon)
      {
         if (exitCMP == null)
            computeToeContacts(trailingSide);
         else
            computeToeContacts(exitCMP, desiredECMP, trailingSide);

         onToesSupportPolygon.setIncludingFrameAndUpdate(leadingFootSupportPolygon);
         onToesSupportPolygon.changeFrameAndProjectToXYPlane(worldFrame);

         toeOffLine.getFirstEndpoint(tmpPoint2d);
         onToesSupportPolygon.addVertexChangeFrameAndProjectToXYPlane(tmpPoint2d);
         toeOffLine.getSecondEndpoint(tmpPoint2d);
         onToesSupportPolygon.addVertexChangeFrameAndProjectToXYPlane(tmpPoint2d);

         onToesSupportPolygon.update();
      }

      public void isReadyToSwitchToToeOff(RobotSide trailingLeg, ReferenceFrame frontFootFrame)
      {
         if (!isFrontFootWellPositionedForToeOff(trailingLeg, frontFootFrame))
         {
            doLineToeOff.set(false);
            computeToeLineContact.set(true);
            return;
         }

         computeToeLineContact.set(updateLineContactDuringToeOff.getBooleanValue());
         doLineToeOff.set(true);
      }

      private void computeToeContacts(FramePoint exitCMP, FramePoint2d desiredECMP, RobotSide supportSide)
      {
         toeOffCalculator.setExitCMP(exitCMP, supportSide);
         toeOffCalculator.computeToeOffContactLine(desiredECMP, supportSide);

         toeOffLine.setToZero(feet.get(supportSide).getSoleFrame());
         toeOffCalculator.getToeOffContactLine(toeOffLine, supportSide);
      }

      public boolean evaluateToeOffConditions(RobotSide trailingLeg)
      {
         if (!isDesiredICPOKForToeOffFilt.getBooleanValue() || !isCurrentICPOKForToeOffFilt.getBooleanValue())
         {
            doLineToeOff.set(false);
            computeToeLineContact.set(true);
            return false;
         }

         needToSwitchToToeOffForAnkleLimit.set(checkAnkleLimitForToeOff(trailingLeg));
         if (needToSwitchToToeOffForAnkleLimit.getBooleanValue())
         {
            doLineToeOff.set(true);
            computeToeLineContact.set(updateLineContactDuringToeOff.getBooleanValue());
            return false;
         }

         if (!isDesiredECMPOKForToeOffFilt.getBooleanValue())
         {
            doLineToeOff.set(false);
            computeToeLineContact.set(true);
            return false;
         }

         return true;
      }
   }

   private class ToePointContact extends AbstractToeContact
   {
      public void updateToeSupportPolygon(FramePoint exitCMP, FramePoint2d desiredECMP, RobotSide trailingSide, FrameConvexPolygon2d leadingFootSupportPolygon)
      {
         if (exitCMP == null)
            computeToeContacts(trailingSide);
         else
            computeToeContacts(exitCMP, desiredECMP, trailingSide);

         onToesSupportPolygon.setIncludingFrameAndUpdate(leadingFootSupportPolygon);
         onToesSupportPolygon.changeFrameAndProjectToXYPlane(worldFrame);

         onToesSupportPolygon.addVertexChangeFrameAndProjectToXYPlane(toeOffPoint);
         onToesSupportPolygon.update();
      }

      public void isReadyToSwitchToToeOff(RobotSide trailingLeg, ReferenceFrame frontFootFrame)
      {
         if (!isFrontFootWellPositionedForToeOff(trailingLeg, frontFootFrame))
         {
            doPointToeOff.set(false);
            computeToePointContact.set(true);
            return;
         }

         computeToePointContact.set(updatePointContactDuringToeOff.getBooleanValue());
         doPointToeOff.set(true);
      }

      private void computeToeContacts(FramePoint exitCMP, FramePoint2d desiredECMP, RobotSide supportSide)
      {
         toeOffCalculator.setExitCMP(exitCMP, supportSide);
         toeOffCalculator.computeToeOffContactPoint(desiredECMP, supportSide);

         toeOffPoint.setToZero(feet.get(supportSide).getSoleFrame());
         toeOffCalculator.getToeOffContactPoint(toeOffPoint, supportSide);
      }

      public boolean evaluateToeOffConditions(RobotSide trailingLeg)
      {
         if (!isDesiredICPOKForToeOffFilt.getBooleanValue() || !isCurrentICPOKForToeOffFilt.getBooleanValue())
         {
            doPointToeOff.set(false);
            computeToePointContact.set(true);
            return false;
         }

         needToSwitchToToeOffForAnkleLimit.set(checkAnkleLimitForToeOff(trailingLeg));
         if (needToSwitchToToeOffForAnkleLimit.getBooleanValue())
         {
            doPointToeOff.set(true);
            computeToePointContact.set(updatePointContactDuringToeOff.getBooleanValue());
            return false;
         }

         if (!isDesiredECMPOKForToeOffFilt.getBooleanValue())
         {
            doPointToeOff.set(false);
            computeToePointContact.set(true);
            return false;
         }

         return true;
      }
   }
}