package us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait;

import us.ihmc.commonWalkingControlModules.configurations.BalanceOnOneLegConfiguration;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.PreSwingControlModule;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.SwingLegTorqueControlModule;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculator;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.kinematics.AnkleVelocityCalculator;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegTorques;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.commonWalkingControlModules.trajectories.CartesianTrajectoryGenerator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.kinematics.OrientationInterpolationCalculator;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.Orientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoAppearance;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.BagOfBalls;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicCoordinateSystem;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObject;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import com.yobotics.simulationconstructionset.util.trajectory.YoMinimumJerkTrajectory;

public class ChangingEndpointSwingSubController implements SwingSubController
{
   private final ProcessedSensorsInterface processedSensors;
   private final CommonWalkingReferenceFrames referenceFrames;
   private final CouplingRegistry couplingRegistry;

   private final DesiredFootstepCalculator desiredFootstepCalculator;

   private final CartesianTrajectoryGenerator walkingTrajectoryGenerator;
   private final SideDependentList<CartesianTrajectoryGenerator> swingInAirTrajectoryGenerator;

   private final SwingLegTorqueControlModule swingLegTorqueControlModule;

   private final SideDependentList<AnkleVelocityCalculator> ankleVelocityCalculators;
   private final SideDependentList<FootSwitchInterface> footSwitches;

   private final PreSwingControlModule preSwingControlModule;

   private final YoVariableRegistry registry = new YoVariableRegistry("SwingSubConroller");

   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final DoubleYoVariable passiveHipCollapseTime = new DoubleYoVariable("passiveHipCollapseTime", registry);

   private final DoubleYoVariable swingDuration = new DoubleYoVariable("swingDuration", "The duration of the swing movement. [s]", registry);
   private final DoubleYoVariable swingOrientationTime = new DoubleYoVariable("swingOrientationTime",
                                                            "The duration of the foot orientation part of the swing.", registry);

   private final DoubleYoVariable initialSwingVelocity = new DoubleYoVariable("initialSwingVelocity", registry);
   private final DoubleYoVariable initialSwingAcceleration = new DoubleYoVariable("initialSwingAcceleration", registry);
   private final DoubleYoVariable finalSwingVelocity = new DoubleYoVariable("finalSwingVelocity", registry);
   private final DoubleYoVariable finalSwingAcceleration = new DoubleYoVariable("finalSwingAcceleration", registry);

   private final DoubleYoVariable minimumTerminalSwingDuration = new DoubleYoVariable("minimumTerminalSwingDuration",
                                                                    "The minimum duration of terminal swing state. [s]", registry);
   private final DoubleYoVariable maximumTerminalSwingDuration = new DoubleYoVariable("maximumTerminalSwingDuration",
                                                                    "The maximum duration of terminal swing state. [s]", registry);

   private final DoubleYoVariable terminalSwingGainRampTime = new DoubleYoVariable("terminalSwingGainRampTime", "The time to ramp the gains to zero [s]",
                                                                 registry);

   private final DoubleYoVariable estimatedSwingTimeRemaining = new DoubleYoVariable("estimatedSwingTimeRemaining", "The estimated Swing Time Remaining [s]",
                                                                   registry);
   private final DoubleYoVariable antiGravityPercentage = new DoubleYoVariable("antiGravityPercentage", "The percent of antigravity effort (0,1)", registry);

   private final DoubleYoVariable swingToePitchUpOnLanding = new DoubleYoVariable("swingToePitchUpOnLanding",
                                                                "How much to pitch up the swing toe at the end of the swing.", registry);

   private final DoubleYoVariable comXThresholdToFinishInitialSwing =
      new DoubleYoVariable("comXThresholdToFinishInitialSwing",
                           "How far the CoM should be in front of the support foot before transitioning out of initial swing.", registry);

   private final DoubleYoVariable timeSpentInPreSwing = new DoubleYoVariable("timeSpentInPreSwing", "This is the time spent in Pre swing.", registry);
   private final DoubleYoVariable timeSpentInInitialSwing = new DoubleYoVariable("timeSpentInInitialSwing", "This is the time spent in initial swing.",
                                                               registry);
   private final DoubleYoVariable timeSpentInMidSwing = new DoubleYoVariable("timeSpentInMidSwing", "This is the time spend in mid swing.", registry);
   private final DoubleYoVariable timeSpentInTerminalSwing = new DoubleYoVariable("timeSpentInTerminalSwing", "This is the time spent in terminal swing.",
                                                                registry);
   private final DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", "This is the toal time spent in single support.",
                                                             registry);

   private final DoubleYoVariable swingFootPositionError = new DoubleYoVariable("swingFootPositionError", registry);

   private final YoMinimumJerkTrajectory minimumJerkTrajectoryForFootOrientation = new YoMinimumJerkTrajectory("swingFootOrientation", registry);

   private final YoFrameOrientation desiredFootOrientationInWorldFrame = new YoFrameOrientation("desiredFootOrientationInWorld", "", worldFrame, registry);
   private final YoFrameOrientation finalDesiredFootOrientationInWorldFrame = new YoFrameOrientation("finalDesiredFootOrientationInWorld", "", worldFrame, registry);
   private final SideDependentList<YoFrameOrientation> startSwingOrientations = new SideDependentList<YoFrameOrientation>();
   private final SideDependentList<YoFrameOrientation> endSwingOrientations = new SideDependentList<YoFrameOrientation>();
   private final SideDependentList<YoFrameOrientation> desiredFootOrientations = new SideDependentList<YoFrameOrientation>();

   private final YoFramePoint finalDesiredSwingFootPosition = new YoFramePoint("finalDesiredSwing", "", worldFrame, registry);
   private final YoFramePoint desiredSwingFootPositionInWorldFrame = new YoFramePoint("desiredSwing", "", worldFrame, registry);
   private final YoFrameVector desiredSwingFootVelocityInWorldFrame = new YoFrameVector("desiredSwingVelocity", "", worldFrame, registry);
   private final YoFrameVector desiredSwingFootAccelerationInWorldFrame = new YoFrameVector("desiredSwingAcceleration", "", worldFrame, registry);
   private final YoFrameVector desiredSwingFootAngularVelocityInWorldFrame = new YoFrameVector("desiredSwingAngularVelocity", "", worldFrame, registry);
   private final YoFrameVector desiredSwingFootAngularAccelerationInWorldFrame = new YoFrameVector("desiredSwingAngularAcceleration", "", worldFrame, registry);
   private DynamicGraphicCoordinateSystem swingFootOrientationViz = null, finalDesiredSwingOrientationViz = null;

   private BagOfBalls bagOfBalls;
   private final double controlDT;
   private RobotSide swingSide;

   public ChangingEndpointSwingSubController(ProcessedSensorsInterface processedSensors, CommonWalkingReferenceFrames referenceFrames,
           CouplingRegistry couplingRegistry, DesiredFootstepCalculator desiredFootstepCalculator,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry parentRegistry,
           SideDependentList<AnkleVelocityCalculator> ankleVelocityCalculators, SideDependentList<FootSwitchInterface> footSwitches,
           CartesianTrajectoryGenerator walkingCartesianTrajectoryGenerator, SideDependentList<CartesianTrajectoryGenerator> swingInAirCartesianTrajectoryGenerator,
           PreSwingControlModule preSwingControlModule, double controlDT, SwingLegTorqueControlModule swingLegTorqueControlModule)
   {
      this.processedSensors = processedSensors;
      this.referenceFrames = referenceFrames;
      this.couplingRegistry = couplingRegistry;
      this.desiredFootstepCalculator = desiredFootstepCalculator;
      this.swingLegTorqueControlModule = swingLegTorqueControlModule;
      this.ankleVelocityCalculators = new SideDependentList<AnkleVelocityCalculator>(ankleVelocityCalculators);
      this.footSwitches = new SideDependentList<FootSwitchInterface>(footSwitches);
      this.walkingTrajectoryGenerator = walkingCartesianTrajectoryGenerator;
      this.swingInAirTrajectoryGenerator = swingInAirCartesianTrajectoryGenerator;
      this.preSwingControlModule = preSwingControlModule;
      this.controlDT = controlDT;
      
      for(RobotSide side : RobotSide.values())
      {
         ReferenceFrame orientationReferenceFrame = referenceFrames.getAnkleZUpFrame(side.getOppositeSide());
         YoFrameOrientation startSwingOrientation = new YoFrameOrientation(side.getCamelCaseNameForStartOfExpression() + "startSwing", "", orientationReferenceFrame, registry);
         YoFrameOrientation endSwingOrientation = new YoFrameOrientation(side.getCamelCaseNameForStartOfExpression() + "endSwing", "", orientationReferenceFrame, registry);
         YoFrameOrientation desiredFootOrientation = new YoFrameOrientation(side.getCamelCaseNameForStartOfExpression() + "desiredSwing", "", orientationReferenceFrame, registry);
         startSwingOrientations.set(side, startSwingOrientation);
         endSwingOrientations.set(side, endSwingOrientation);
         desiredFootOrientations.set(side, desiredFootOrientation);
      }
      
      createVisualizers(dynamicGraphicObjectsListRegistry, parentRegistry);
      couplingRegistry.setEstimatedSwingTimeRemaining(estimatedSwingTimeRemaining.getDoubleValue());
      couplingRegistry.setSingleSupportDuration(swingDuration.getDoubleValue());
      parentRegistry.addChild(registry);
   }

   private void createVisualizers(DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry parentRegistry)
   {
      if (dynamicGraphicObjectsListRegistry != null)
      {
         ArtifactList artifactList = new ArtifactList("ChangingEndpoint");

         swingFootOrientationViz = new DynamicGraphicCoordinateSystem("Coordinate System", desiredSwingFootPositionInWorldFrame, desiredFootOrientationInWorldFrame, 0.1);
         finalDesiredSwingOrientationViz = new DynamicGraphicCoordinateSystem("Final Desired Orientation", finalDesiredSwingFootPosition, finalDesiredFootOrientationInWorldFrame, 0.1);

         int numberOfBalls = 1;
         double ballSize = (numberOfBalls > 1) ? 0.005 : 0.02;
         bagOfBalls = new BagOfBalls(numberOfBalls, ballSize, "swingTarget", YoAppearance.Aqua(), parentRegistry, dynamicGraphicObjectsListRegistry);


         DynamicGraphicPosition finalDesiredSwingViz = finalDesiredSwingFootPosition.createDynamicGraphicPosition("Final Desired Swing", 0.03,
                                                          YoAppearance.Black(), GraphicType.BALL_WITH_CROSS);

         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjects("R2Sim02SwingSubController", new DynamicGraphicObject[] {swingFootOrientationViz,
                 finalDesiredSwingViz, finalDesiredSwingOrientationViz});

         artifactList.add(finalDesiredSwingViz.createArtifact());
         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }
   }

   public boolean canWeStopNow()
   {
      return true;
   }

   public boolean isReadyForDoubleSupport()
   {
      FramePoint swingAnkle = new FramePoint(referenceFrames.getFootFrame(swingSide));
      swingAnkle.changeFrame(referenceFrames.getAnkleZUpFrame(swingSide.getOppositeSide()));
      double footHeight = swingAnkle.getZ();
      double maxFootHeight = 0.02;

      return swingInAirTrajectoryGenerator.get(swingSide).isDone() && (footHeight < maxFootHeight);
   }

   public void doPreSwing(LegTorques legTorquesToPackForSwingLeg, double timeInState)
   {
      setEstimatedSwingTimeRemaining(swingDuration.getDoubleValue());
      this.swingSide = legTorquesToPackForSwingLeg.getRobotSide();
      preSwingControlModule.doPreSwing(legTorquesToPackForSwingLeg, timeInState);
      swingLegTorqueControlModule.computePreSwing(swingSide);
      timeSpentInPreSwing.set(timeInState);
   }

   public void doInitialSwing(LegTorques legTorquesToPackForSwingLeg, double timeInState)
   {
      doInitialAndMidSwing(legTorquesToPackForSwingLeg, timeInState);
      timeSpentInInitialSwing.set(timeInState);
   }

   public void doMidSwing(LegTorques legTorquesToPackForSwingLeg, double timeInState)
   {
      double timeSpentSwingingUpToNow = timeInState + timeSpentInInitialSwing.getDoubleValue();
      doInitialAndMidSwing(legTorquesToPackForSwingLeg, timeSpentSwingingUpToNow);
      timeSpentInMidSwing.set(timeInState);
   }

   private void doInitialAndMidSwing(LegTorques legTorquesToPackForSwingLeg, double timeSpentSwingingUpToNow)
   {
      this.swingSide = legTorquesToPackForSwingLeg.getRobotSide();
      updateFinalDesiredPosition(walkingTrajectoryGenerator);
      computeDesiredFootPosVelAcc(swingSide, walkingTrajectoryGenerator, timeSpentSwingingUpToNow);
      computeSwingLegTorques(legTorquesToPackForSwingLeg);
      setEstimatedSwingTimeRemaining(swingDuration.getDoubleValue() - timeSpentSwingingUpToNow);
   }

   public void doTerminalSwing(LegTorques legTorquesToPackForSwingLeg, double timeInState)
   {
      setEstimatedSwingTimeRemaining(0.0);

      // Continue swinging to the same place in world coordinates, not the
      // same place in body coordinates...

      desiredSwingFootVelocityInWorldFrame.set(0.0, 0.0, 0.0);
      desiredSwingFootAngularVelocityInWorldFrame.set(0.0, 0.0, 0.0);

      desiredSwingFootAccelerationInWorldFrame.set(0.0, 0.0, 0.0);
      desiredSwingFootAngularAccelerationInWorldFrame.set(0.0, 0.0, 0.0);

      computeSwingLegTorques(legTorquesToPackForSwingLeg);

      timeSpentInTerminalSwing.set(timeInState);
   }

   public void doSwingInAir(LegTorques legTorques, double timeInCurrentState)
   {
      this.swingSide = legTorques.getRobotSide();

      FramePoint swingFootPosition = new FramePoint(referenceFrames.getFootFrame(swingSide));
      swingFootPosition.changeFrame(referenceFrames.getAnkleZUpFrame(swingSide.getOppositeSide()));
      double footZ = swingFootPosition.getZ();

      double minFootZ = 0.02;
      if (footZ < minFootZ)
         swingLegTorqueControlModule.setAnkleGainsSoft(swingSide);
      else
         swingLegTorqueControlModule.setAnkleGainsDefault(swingSide);

      computeDesiredFootPosVelAcc(swingSide, swingInAirTrajectoryGenerator.get(swingSide), timeInCurrentState);
      computeSwingLegTorques(legTorques);
   }

   public void doTransitionIntoPreSwing(RobotSide swingSide)
   {
      desiredFootstepCalculator.initializeDesiredFootstep(swingSide.getOppositeSide());

      // Reset the timers
      timeSpentInPreSwing.set(0.0);
      timeSpentInInitialSwing.set(0.0);
      timeSpentInMidSwing.set(0.0);
      timeSpentInTerminalSwing.set(0.0);
      singleSupportDuration.set(0.0);
   }

   public void doTransitionIntoInitialSwing(RobotSide swingSide)
   {
      ReferenceFrame cartesianTrajectoryGeneratorFrame = walkingTrajectoryGenerator.getReferenceFrame();

      // Get the current position of the swing foot
      FramePoint startPoint = new FramePoint(referenceFrames.getAnkleZUpFrame(swingSide));

      // Get the desired position of the swing foot
      Footstep desiredFootstep = couplingRegistry.getDesiredFootstep();
      FramePoint endPoint = new FramePoint(desiredFootstep.getFootstepPose().getPosition());

      // Get the initial velocity of the swing foot
      FrameVector initialSwingVelocityVector = ankleVelocityCalculators.get(swingSide).getAnkleVelocityInWorldFrame();

      // Express everything in the same frame and initialize the trajectory generator
      startPoint.changeFrame(cartesianTrajectoryGeneratorFrame);
      endPoint.changeFrame(cartesianTrajectoryGeneratorFrame);
      initialSwingVelocityVector.changeFrame(cartesianTrajectoryGeneratorFrame);
      walkingTrajectoryGenerator.initialize(startPoint, initialSwingVelocityVector, endPoint);

      // Setup the orientation trajectory
      setupSwingFootOrientationTrajectory(desiredFootstep);

      // Set the finalDesiredSwingPosition
      endPoint.changeFrame(finalDesiredSwingFootPosition.getReferenceFrame());
      finalDesiredSwingFootPosition.set(endPoint);
      
      this.finalDesiredFootOrientationInWorldFrame.set(desiredFootstep.getFootstepOrientationInFrame(worldFrame));
      
      swingLegTorqueControlModule.setAnkleGainsDefault(swingSide);
   }

   public void doTransitionIntoMidSwing(RobotSide swingSide)
   {
   }

   public void doTransitionIntoTerminalSwing(RobotSide swingSide)
   {
      swingLegTorqueControlModule.setAnkleGainsSoft(swingSide);
   }

   public void doTransitionIntoSwingInAir(RobotSide swingLeg, BalanceOnOneLegConfiguration currentConfiguration)
   {
      
      minimumJerkTrajectoryForFootOrientation.setParams(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, swingOrientationTime.getDoubleValue());
      setEstimatedSwingTimeRemaining(0.0);

      FramePoint currentPosition = new FramePoint(referenceFrames.getFootFrame(swingLeg));

      FrameVector currentVelocity = ankleVelocityCalculators.get(swingLeg).getAnkleVelocityInWorldFrame();

      FramePoint finalDesiredPosition = currentConfiguration.getDesiredSwingFootPosition();

      swingInAirTrajectoryGenerator.get(swingLeg).initialize(currentPosition, currentVelocity, finalDesiredPosition);
   }

   public void doTransitionOutOfInitialSwing(RobotSide swingSide)
   {
      swingLegTorqueControlModule.setAnkleGainsDefault(swingSide);
   }

   public void doTransitionOutOfMidSwing(RobotSide swingSide)
   {
   }

   public void doTransitionOutOfPreSwing(RobotSide swingSide)
   {
   }

   public void doTransitionOutOfTerminalSwing(RobotSide swingSide)
   {
      singleSupportDuration.set(timeSpentInPreSwing.getDoubleValue() + timeSpentInInitialSwing.getDoubleValue() + timeSpentInMidSwing.getDoubleValue()
                                + timeSpentInTerminalSwing.getDoubleValue());
      couplingRegistry.setSingleSupportDuration(singleSupportDuration.getDoubleValue());
   }

   public void doTransitionOutOfSwingInAir(RobotSide swingLeg)
   {
      // TODO Auto-generated method stub

   }

   private void setEstimatedSwingTimeRemaining(double timeRemaining)
   {
      this.estimatedSwingTimeRemaining.set(timeRemaining);
      this.couplingRegistry.setEstimatedSwingTimeRemaining(timeRemaining);
   }

   public double getEstimatedSwingTimeRemaining()
   {
      return estimatedSwingTimeRemaining.getDoubleValue();
   }

   public boolean isDoneWithPreSwingC(RobotSide loadingLeg, double timeInState)
   {
      return (timeInState > passiveHipCollapseTime.getDoubleValue());
   }

   public boolean isDoneWithInitialSwing(RobotSide swingSide, double timeInState)
   {
      RobotSide oppositeSide = swingSide.getOppositeSide();
      ReferenceFrame stanceAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(oppositeSide);
      FramePoint comProjection = processedSensors.getCenterOfMassGroundProjectionInFrame(stanceAnkleZUpFrame);
      FramePoint2d sweetSpot = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(oppositeSide);
      sweetSpot.changeFrame(stanceAnkleZUpFrame);
      boolean inStateLongEnough = timeInState > 0.05;
      boolean isCoMPastSweetSpot = comProjection.getX() > sweetSpot.getX();
      boolean trajectoryIsDone = walkingTrajectoryGenerator.isDone();
      boolean footHitEarly = footSwitches.get(swingSide).hasFootHitGround();

      return inStateLongEnough && (isCoMPastSweetSpot || trajectoryIsDone || footHitEarly);
   }

   public boolean isDoneWithMidSwing(RobotSide swingSide, double timeInState)
   {
      boolean trajectoryIsDone = walkingTrajectoryGenerator.isDone();
//      boolean capturePointInsideFoot = isCapturePointInsideFoot(swingSide);

      return trajectoryIsDone; //  || capturePointInsideFoot;
   }

   public boolean isDoneWithTerminalSwing(RobotSide swingSide, double timeInState)
   {
      boolean footOnGround = footSwitches.get(swingSide).hasFootHitGround();

      boolean minimumTerminalSwingTimePassed = (timeInState > minimumTerminalSwingDuration.getDoubleValue());
      boolean maximumTerminalSwingTimePassed = (timeInState > maximumTerminalSwingDuration.getDoubleValue());

      boolean capturePointInsideFoot = isCapturePointInsideFoot(swingSide);

      return ((footOnGround && minimumTerminalSwingTimePassed) || maximumTerminalSwingTimePassed || (capturePointInsideFoot && minimumTerminalSwingTimePassed));
   }

   public boolean isDoneWithSwingInAir(RobotSide swingSide, double timeInState)
   {
      return swingInAirTrajectoryGenerator.get(swingSide).isDone() && (timeInState > 2.0);
   }

   public void setParametersForR2()
   {
      swingDuration.set(0.4);    // (0.4);
      swingOrientationTime.set(0.2);    // 0.75 * swingDuration.getDoubleValue());

      swingToePitchUpOnLanding.set(0.25);    // 0.4); // (0.5);

      initialSwingVelocity.set(0.2);    // 0.12;
      initialSwingAcceleration.set(0.0);

      finalSwingVelocity.set(0.2);    // 0.12;
      finalSwingAcceleration.set(0.0);

      minimumTerminalSwingDuration.set(0.03);    // 0.1); // 0.25;
      maximumTerminalSwingDuration.set(0.15);    // 0.15);    // 0.1); // 0.25;
      terminalSwingGainRampTime.set(minimumTerminalSwingDuration.getDoubleValue() / 4.0);

      passiveHipCollapseTime.set(0.07);    // 0.06); // 0.1);

      antiGravityPercentage.set(1.0);

      comXThresholdToFinishInitialSwing.set(0.15);
   }

   public void setParametersForM2V2()
   {
      swingDuration.set(0.6);    // 0.7);    // 0.5);    // (0.4);
      swingOrientationTime.set(0.2);    // 0.75 * swingDuration.getDoubleValue());

      swingToePitchUpOnLanding.set(0.25);    // 0.4);    // (0.5);

      initialSwingVelocity.set(0.2);    // 0.12;
      initialSwingAcceleration.set(0.0);

      finalSwingVelocity.set(0.2);    // 0.12;
      finalSwingAcceleration.set(0.0);

      minimumTerminalSwingDuration.set(0.0);    // 0.1);    // 0.25;
      maximumTerminalSwingDuration.set(0.05);    // 0.2);    // 0.1);    // 0.25;
      terminalSwingGainRampTime.set(minimumTerminalSwingDuration.getDoubleValue() / 4.0);

      passiveHipCollapseTime.set(0.07);    // 0.1);    // 07);    // 0.06);    // 0.1);

      antiGravityPercentage.set(1.0);

      comXThresholdToFinishInitialSwing.set(0.10);    // 15);
   }

   private void updateFinalDesiredPosition(CartesianTrajectoryGenerator trajectoryGenerator)
   {
      Footstep desiredFootstep = couplingRegistry.getDesiredFootstep();
      
      FramePose desiredFootstepPose = desiredFootstep.getFootstepPose();
      FramePoint finalDesiredSwingFootPosition =
         desiredFootstepPose.getPosition().changeFrameCopy(this.finalDesiredSwingFootPosition.getReferenceFrame());
      this.finalDesiredSwingFootPosition.set(finalDesiredSwingFootPosition);
      
      this.finalDesiredFootOrientationInWorldFrame.set(desiredFootstep.getFootstepOrientationInFrame(worldFrame));
      
      ReferenceFrame cartesianTrajectoryGeneratorFrame = trajectoryGenerator.getReferenceFrame();
      finalDesiredSwingFootPosition.changeFrame(cartesianTrajectoryGeneratorFrame);
      trajectoryGenerator.updateFinalDesiredPosition(finalDesiredSwingFootPosition);
   }

   private void computeDesiredFootPosVelAcc(RobotSide swingSide, CartesianTrajectoryGenerator trajectoryGenerator, double timeInState)
   {
      ReferenceFrame cartesianTrajectoryGeneratorFrame = trajectoryGenerator.getReferenceFrame();

      // TODO: Don't generate so much junk here.
      FramePoint position = new FramePoint(cartesianTrajectoryGeneratorFrame);
      FrameVector velocity = new FrameVector(cartesianTrajectoryGeneratorFrame);
      FrameVector acceleration = new FrameVector(cartesianTrajectoryGeneratorFrame);

      trajectoryGenerator.computeNextTick(position, velocity, acceleration, controlDT);
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

      position.changeFrame(worldFrame);
      velocity.changeFrame(worldFrame);
      acceleration.changeFrame(worldFrame);

      desiredSwingFootPositionInWorldFrame.set(position);
      desiredSwingFootVelocityInWorldFrame.set(velocity);
      desiredSwingFootAccelerationInWorldFrame.set(acceleration);

      
      
      // Determine foot orientation and angular velocity
      minimumJerkTrajectoryForFootOrientation.computeTrajectory(timeInState);
      double orientationInterpolationAlpha = minimumJerkTrajectoryForFootOrientation.getPosition();
      YoFrameOrientation endSwingOrientation = endSwingOrientations.get(swingSide);
      YoFrameOrientation desiredFootOrientation = desiredFootOrientations.get(swingSide);
      YoFrameOrientation startSwingOrientation = startSwingOrientations.get(swingSide);
      desiredFootOrientation.interpolate(startSwingOrientation, endSwingOrientation, orientationInterpolationAlpha);

      // Visualisation
      Orientation desiredFootOrientationToViz = desiredFootOrientation.getFrameOrientationCopy();
      desiredFootOrientationToViz.changeFrame(worldFrame);
      desiredFootOrientationInWorldFrame.set(desiredFootOrientationToViz);
      
      double alphaDot = minimumJerkTrajectoryForFootOrientation.getVelocity();
      FrameVector desiredSwingFootAngularVelocity = OrientationInterpolationCalculator.computeAngularVelocity(startSwingOrientation.getFrameOrientationCopy(),
                                                       endSwingOrientation.getFrameOrientationCopy(), alphaDot);
      desiredSwingFootAngularVelocity.changeFrame(worldFrame);
      desiredSwingFootAngularVelocityInWorldFrame.set(desiredSwingFootAngularVelocity);

      double alphaDDot = minimumJerkTrajectoryForFootOrientation.getAcceleration();
      FrameVector desiredSwingFootAngularAcceleration =
         OrientationInterpolationCalculator.computeAngularAcceleration(startSwingOrientation.getFrameOrientationCopy(),
            endSwingOrientation.getFrameOrientationCopy(), alphaDDot);
      desiredSwingFootAngularAcceleration.changeFrame(worldFrame);
      desiredSwingFootAngularAccelerationInWorldFrame.set(desiredSwingFootAngularAcceleration);

      updateSwingfootError(position);
   }

   private void updateSwingfootError(FramePoint desiredPosition)
   {
      ReferenceFrame swingFootFrame = referenceFrames.getFootFrame(swingSide);
      desiredPosition.changeFrame(swingFootFrame);
      swingFootPositionError.set(desiredPosition.distance(new FramePoint(swingFootFrame)));
   }

   private void computeSwingLegTorques(LegTorques legTorquesToPackForSwingLeg)
   {
      swingLegTorqueControlModule.compute(legTorquesToPackForSwingLeg, desiredSwingFootPositionInWorldFrame.getFramePointCopy(),
              desiredFootOrientations.get(legTorquesToPackForSwingLeg.getRobotSide()).getFrameOrientationCopy(), desiredSwingFootVelocityInWorldFrame.getFrameVectorCopy(),
              desiredSwingFootAngularVelocityInWorldFrame.getFrameVectorCopy(), desiredSwingFootAccelerationInWorldFrame.getFrameVectorCopy(),
              desiredSwingFootAngularAccelerationInWorldFrame.getFrameVectorCopy());

      leaveTrailOfBalls();
   }

   private void leaveTrailOfBalls()
   {
      if (bagOfBalls != null)
      {
         bagOfBalls.setBallLoop(desiredSwingFootPositionInWorldFrame.getFramePointCopy());
      }
   }

   private void setupSwingFootOrientationTrajectory(Footstep desiredFootStep)
   {
      // Why do we have Xf = 1.0 ?
      minimumJerkTrajectoryForFootOrientation.setParams(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, swingOrientationTime.getDoubleValue());

      initializeStartOrientationToMatchActual(desiredFootStep.getFootstepSide());

//    Orientation endOrientation = desiredFootStep.getFootstepPose().getOrientation().changeFrameCopy(endSwingOrientation.getReferenceFrame());
//    endSwingOrientation.set(endOrientation);

      Orientation endOrientation = desiredFootStep.getFootstepPose().getOrientation();
      RobotSide swingFootSide = desiredFootStep.getFootstepSide();
      ReferenceFrame supportFootAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(swingFootSide.getOppositeSide());
      endSwingOrientations.get(swingFootSide).set(endOrientation.changeFrameCopy(supportFootAnkleZUpFrame));
   }

   private void initializeStartOrientationToMatchActual(RobotSide swingSide)
   {
      ReferenceFrame swingFootFrame = referenceFrames.getFootFrame(swingSide);
      Orientation startOrientation = new Orientation(swingFootFrame);
      startOrientation = startOrientation.changeFrameCopy(referenceFrames.getAnkleZUpFrame(swingSide.getOppositeSide()));
      startSwingOrientations.get(swingSide).set(startOrientation);
   }

   private boolean isCapturePointInsideFoot(RobotSide swingSide)
   {
      FrameConvexPolygon2d footPolygon = couplingRegistry.getBipedSupportPolygons().getFootPolygonInAnkleZUp(swingSide);
      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(footPolygon.getReferenceFrame()).toFramePoint2d();

      boolean capturePointInsideFoot = footPolygon.isPointInside(capturePoint);

      return capturePointInsideFoot;
   }

   public void initialize()
   {      
   }
}
