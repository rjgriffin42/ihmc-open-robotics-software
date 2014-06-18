package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.WalkOnTheEdgesManager;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingProviders;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesData;
import us.ihmc.commonWalkingControlModules.trajectories.WalkOnTheEdgesProviders;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParameters;

public class FeetManager
{
   private static final boolean USE_WORLDFRAME_SURFACE_NORMAL_WHEN_FULLY_CONSTRAINED = true;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final SideDependentList<FootControlModule> footControlModules = new SideDependentList<>();

   private final SideDependentList<BooleanYoVariable> requestSupportFootToHoldPosition = new SideDependentList<BooleanYoVariable>();

   private final WalkOnTheEdgesManager walkOnTheEdgesManager;
   private final WalkOnTheEdgesProviders walkOnTheEdgesProviders;

   private final SideDependentList<? extends ContactablePlaneBody> feet;

   private final ReferenceFrame pelvisZUpFrame;

   private final SideDependentList<FootSwitchInterface> footSwitches;

   private final DoubleYoVariable swingKpXY = new DoubleYoVariable("swingKpXY", registry);
   private final DoubleYoVariable swingKpZ = new DoubleYoVariable("swingKpZ", registry);
   private final DoubleYoVariable swingKpOrientation = new DoubleYoVariable("swingKpOrientation", registry);
   private final DoubleYoVariable swingZetaXYZ = new DoubleYoVariable("swingZetaXYZ", registry);
   private final DoubleYoVariable swingZetaOrientation = new DoubleYoVariable("swingZetaOrientation", registry);

   private final DoubleYoVariable holdKpXY = new DoubleYoVariable("holdKpXY", registry);
   private final DoubleYoVariable holdKpOrientation = new DoubleYoVariable("holdKpOrientation", registry);
   private final DoubleYoVariable holdZeta = new DoubleYoVariable("holdZeta", registry);

   private final DoubleYoVariable toeOffKpXY = new DoubleYoVariable("toeOffKpXY", registry);
   private final DoubleYoVariable toeOffKpOrientation = new DoubleYoVariable("toeOffKpOrientation", registry);
   private final DoubleYoVariable toeOffZeta = new DoubleYoVariable("toeOffZeta", registry);

   private final DoubleYoVariable swingMaxPositionAcceleration = new DoubleYoVariable("swingMaxPositionAcceleration", registry);
   private final DoubleYoVariable swingMaxPositionJerk = new DoubleYoVariable("swingMaxPositionJerk", registry);
   private final DoubleYoVariable swingMaxOrientationAcceleration = new DoubleYoVariable("swingMaxOrientationAcceleration", registry);
   private final DoubleYoVariable swingMaxOrientationJerk = new DoubleYoVariable("swingMaxOrientationJerk", registry);

   private final DoubleYoVariable singularityEscapeNullspaceMultiplierSwingLeg = new DoubleYoVariable("singularityEscapeNullspaceMultiplierSwingLeg", registry);
   private final DoubleYoVariable singularityEscapeNullspaceMultiplierSupportLeg = new DoubleYoVariable("singularityEscapeNullspaceMultiplierSupportLeg",
         registry);
   private final DoubleYoVariable singularityEscapeNullspaceMultiplierSupportLegLocking = new DoubleYoVariable(
         "singularityEscapeNullspaceMultiplierSupportLegLocking", registry);

   public FeetManager(MomentumBasedController momentumBasedController, WalkingControllerParameters walkingControllerParameters,
         DoubleProvider swingTimeProvider, VariousWalkingProviders variousWalkingProviders, SideDependentList<FootSwitchInterface> footSwitches,
         YoVariableRegistry parentRegistry)
   {

      double singularityEscapeMultiplierForSwing = walkingControllerParameters.getSwingSingularityEscapeMultiplier();
      singularityEscapeNullspaceMultiplierSwingLeg.set(singularityEscapeMultiplierForSwing);
      singularityEscapeNullspaceMultiplierSupportLeg.set(walkingControllerParameters.getSupportSingularityEscapeMultiplier());
      singularityEscapeNullspaceMultiplierSupportLegLocking.set(0.0); // -0.5);

      swingKpXY.set(walkingControllerParameters.getSwingKpXY());
      swingKpZ.set(walkingControllerParameters.getSwingKpZ());
      swingKpOrientation.set(walkingControllerParameters.getSwingKpOrientation());
      swingZetaXYZ.set(walkingControllerParameters.getSwingZetaXYZ());
      swingZetaOrientation.set(walkingControllerParameters.getSwingZetaOrientation());

      holdKpXY.set(walkingControllerParameters.getHoldKpXY());
      holdKpOrientation.set(walkingControllerParameters.getHoldKpOrientation());
      holdZeta.set(walkingControllerParameters.getHoldZeta());

      toeOffKpXY.set(walkingControllerParameters.getToeOffKpXY());
      toeOffKpOrientation.set(walkingControllerParameters.getToeOffKpOrientation());
      toeOffZeta.set(walkingControllerParameters.getToeOffZeta());

      swingMaxPositionAcceleration.set(walkingControllerParameters.getSwingMaxPositionAcceleration());
      swingMaxPositionJerk.set(walkingControllerParameters.getSwingMaxPositionJerk());
      swingMaxOrientationAcceleration.set(walkingControllerParameters.getSwingMaxOrientationAcceleration());
      swingMaxOrientationJerk.set(walkingControllerParameters.getSwingMaxOrientationJerk());

      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = momentumBasedController.getDynamicGraphicObjectsListRegistry();

      this.walkOnTheEdgesProviders = new WalkOnTheEdgesProviders(walkingControllerParameters, registry);
      feet = momentumBasedController.getContactableFeet();
      walkOnTheEdgesManager = new WalkOnTheEdgesManager(walkingControllerParameters, walkOnTheEdgesProviders, feet, footControlModules, registry);

      this.footSwitches = footSwitches;
      pelvisZUpFrame = momentumBasedController.getReferenceFrames().getPelvisZUpFrame();

      for (RobotSide robotSide : RobotSide.values)
      {
         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();

         BooleanYoVariable requestHoldPosition = new BooleanYoVariable(sidePrefix + "RequestSupportFootToHoldPosition", registry);
         requestSupportFootToHoldPosition.put(robotSide, requestHoldPosition);

         DoubleTrajectoryGenerator pitchTouchdownTrajectoryGenerator = walkOnTheEdgesProviders.getFootTouchdownPitchTrajectoryGenerator(robotSide);
         DoubleProvider maximumTakeoffAngle = walkOnTheEdgesProviders.getMaximumToeOffAngleProvider();
         FootControlModule footControlModule = new FootControlModule(robotSide, pitchTouchdownTrajectoryGenerator, maximumTakeoffAngle, requestHoldPosition,
               walkingControllerParameters, swingTimeProvider, dynamicGraphicObjectsListRegistry, momentumBasedController, registry);

         VariableChangedListener swingGainsChangedListener = createEndEffectorGainsChangedListener(footControlModule);
         swingGainsChangedListener.variableChanged(null);

         footControlModules.put(robotSide, footControlModule);
      }
   }

   private VariableChangedListener createEndEffectorGainsChangedListener(final FootControlModule endEffectorControlModule)
   {
      VariableChangedListener ret = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            endEffectorControlModule.setHoldGains(holdKpXY.getDoubleValue(), holdKpOrientation.getDoubleValue(), holdZeta.getDoubleValue());
            endEffectorControlModule.setSwingGains(swingKpXY.getDoubleValue(), swingKpZ.getDoubleValue(), swingKpOrientation.getDoubleValue(),
                  swingZetaXYZ.getDoubleValue(), swingZetaOrientation.getDoubleValue());
            endEffectorControlModule.setToeOffGains(toeOffKpXY.getDoubleValue(), toeOffKpOrientation.getDoubleValue(), toeOffZeta.getDoubleValue());
            endEffectorControlModule.setMaxAccelerationAndJerk(swingMaxPositionAcceleration.getDoubleValue(), swingMaxPositionJerk.getDoubleValue(),
                  swingMaxOrientationAcceleration.getDoubleValue(), swingMaxOrientationJerk.getDoubleValue());
         }
      };

      swingKpXY.addVariableChangedListener(ret);
      swingKpZ.addVariableChangedListener(ret);
      swingKpOrientation.addVariableChangedListener(ret);
      swingZetaXYZ.addVariableChangedListener(ret);
      swingZetaOrientation.addVariableChangedListener(ret);

      swingMaxPositionAcceleration.addVariableChangedListener(ret);
      swingMaxPositionJerk.addVariableChangedListener(ret);
      swingMaxOrientationAcceleration.addVariableChangedListener(ret);
      swingMaxOrientationJerk.addVariableChangedListener(ret);

      holdKpXY.addVariableChangedListener(ret);
      holdKpOrientation.addVariableChangedListener(ret);
      holdZeta.addVariableChangedListener(ret);

      toeOffKpXY.addVariableChangedListener(ret);
      toeOffKpOrientation.addVariableChangedListener(ret);
      toeOffZeta.addVariableChangedListener(ret);

      return ret;
   }

   public void compute()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         footControlModules.get(robotSide).doControl();
      }
   }

   public void setFootstep(RobotSide robotSide, Footstep footstep, TrajectoryParameters trajectoryParameters)
   {
      footControlModules.get(robotSide).setFootStep(footstep, trajectoryParameters);
   }
   
   public void setFootPose(RobotSide robotSide, FramePose footPose)
   {
      footControlModules.get(robotSide).setFootPose(footPose);
   }

   public boolean isInEdgeTouchdownState(RobotSide robotSide)
   {
      return footControlModules.get(robotSide).isInEdgeTouchdownState();
   }

   public ConstraintType getCurrentConstraintType(RobotSide robotSide)
   {
      return footControlModules.get(robotSide).getCurrentConstraintType();
   }

   public void replanTrajectory(RobotSide swingSide, double swingTimeRemaining)
   {
      footControlModules.get(swingSide).replanTrajectory(swingTimeRemaining);
   }

   public boolean isInSingularityNeighborhood(RobotSide robotSide)
   {
      return footControlModules.get(robotSide).isInSingularityNeighborhood();
   }

   public void doSingularityEscape(RobotSide robotSide)
   {
      footControlModules.get(robotSide).doSingularityEscape(true);
   }

   public void doSingularityEscape(RobotSide robotSide, double temporarySingularityEscapeNullspaceMultiplier)
   {
      footControlModules.get(robotSide).doSingularityEscape(temporarySingularityEscapeNullspaceMultiplier);
   }

   public boolean isInFlatSupportState(RobotSide robotSide)
   {
      return footControlModules.get(robotSide).isInFlatSupportState();
   }

   public void correctCoMHeight(RobotSide trailingLeg, FrameVector2d desiredICPVelocity, double zCurrent, CoMHeightTimeDerivativesData comHeightData,
         boolean checkForKneeCollapsing, boolean checkForStraightKnee)
   {
      RobotSide[] leadingLegFirst;
      if (trailingLeg != null)
         leadingLegFirst = new RobotSide[] { trailingLeg.getOppositeSide(), trailingLeg };
      else
         leadingLegFirst = RobotSide.values;

      // Correct, if necessary, the CoM height trajectory to avoid the knee to collapse
      if (checkForKneeCollapsing)
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            footControlModules.get(robotSide).correctCoMHeightTrajectoryForCollapseAvoidance(desiredICPVelocity, comHeightData, zCurrent, pelvisZUpFrame,
                  footSwitches.get(robotSide).computeFootLoadPercentage());
         }
      }

      // Correct, if necessary, the CoM height trajectory to avoid straight knee
      if (checkForStraightKnee)
      {
         for (RobotSide robotSide : leadingLegFirst)
         {
            FootControlModule footControlModule = footControlModules.get(robotSide);
            footControlModule.correctCoMHeightTrajectoryForSingularityAvoidance(desiredICPVelocity, comHeightData, zCurrent, pelvisZUpFrame);
         }
      }

      // Do that after to make sure the swing foot will land
      for (RobotSide robotSide : RobotSide.values)
      {
         footControlModules.get(robotSide).correctCoMHeightTrajectoryForUnreachableFootStep(comHeightData);
      }
   }

   private final FrameVector footNormalContactVector = new FrameVector(worldFrame, 0.0, 0.0, 1.0);

   public void setOnToesContactState(RobotSide robotSide)
   {
      FootControlModule footControlModule = footControlModules.get(robotSide);
      if (footControlModule.isInFlatSupportState())
      {
         footNormalContactVector.setIncludingFrame(feet.get(robotSide).getPlaneFrame(), 0.0, 0.0, 1.0);
         footNormalContactVector.changeFrame(worldFrame);
      }
      else
      {
         footNormalContactVector.setIncludingFrame(worldFrame, 0.0, 0.0, 1.0);
      }

      footControlModule.setContactState(ConstraintType.TOES, footNormalContactVector);
   }

   public void setTouchdownOnHeelContactState(RobotSide robotSide)
   {
      footNormalContactVector.setIncludingFrame(worldFrame, 0.0, 0.0, 1.0);
      footControlModules.get(robotSide).setContactState(ConstraintType.HEEL_TOUCHDOWN, footNormalContactVector);
   }

   public void setTouchdownOnToesContactState(RobotSide robotSide)
   {
      footNormalContactVector.setIncludingFrame(worldFrame, 0.0, 0.0, 1.0);
      footControlModules.get(robotSide).setContactState(ConstraintType.TOES_TOUCHDOWN, footNormalContactVector);
   }

   public void setFlatFootContactState(RobotSide robotSide)
   {
      if (USE_WORLDFRAME_SURFACE_NORMAL_WHEN_FULLY_CONSTRAINED)
         footNormalContactVector.setIncludingFrame(worldFrame, 0.0, 0.0, 1.0);
      else
         footNormalContactVector.setIncludingFrame(feet.get(robotSide).getPlaneFrame(), 0.0, 0.0, 1.0);
      footControlModules.get(robotSide).setContactState(ConstraintType.FULL, footNormalContactVector);
   }

   public void setContactStateForSwing(RobotSide robotSide)
   {
      FootControlModule endEffectorControlModule = footControlModules.get(robotSide);
      endEffectorControlModule.doSingularityEscape(true);
      endEffectorControlModule.setContactState(ConstraintType.SWING);
   }

   public WalkOnTheEdgesManager getWalkOnTheEdgesManager()
   {
      return walkOnTheEdgesManager;
   }
}
