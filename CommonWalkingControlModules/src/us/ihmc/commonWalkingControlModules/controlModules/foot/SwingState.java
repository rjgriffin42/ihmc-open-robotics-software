package us.ihmc.commonWalkingControlModules.controlModules.foot;

import java.util.List;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.trajectories.SoftTouchdownPoseTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointSwingGenerator;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactableFoot;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.trajectories.PoseTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSE3TrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.MultipleWaypointsPoseTrajectoryGenerator;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SpatialAccelerationVector;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.robotics.trajectories.providers.CurrentStateProvider;

public class SwingState extends AbstractUnconstrainedState
{
   private final BooleanYoVariable replanTrajectory;
   private final BooleanYoVariable doContinuousReplanning;

   private static final double maxScalingFactor = 1.2;
   private static final double minScalingFactor = 0.1;
   private static final double exponentialScalingRate = 5.0;

   private TrajectoryType activeTrajectoryType = TrajectoryType.DEFAULT;
   
   private final TwoWaypointSwingGenerator swingTrajectoryOptimizer;
   private final MultipleWaypointsPoseTrajectoryGenerator swingTrajectory;
   private final SoftTouchdownPoseTrajectoryGenerator touchdownTrajectory;
   
   private final CurrentStateProvider currentStateProvider;
   
   private final YoFrameVector yoTouchdownAcceleration;
   private final YoFrameVector yoTouchdownVelocity;
   
   private final ReferenceFrame oppositeSoleFrame;
   
   private final FramePoint initialPosition = new FramePoint();
   private final FrameVector initialLinearVelocity = new FrameVector();
   private final FrameOrientation initialOrientation = new FrameOrientation();
   private final FrameVector initialAngularVelocity = new FrameVector();
   private final FramePoint finalPosition = new FramePoint();
   private final FrameVector finalLinearVelocity = new FrameVector();
   private final FrameOrientation finalOrientation = new FrameOrientation();
   private final FrameVector finalAngularVelocity = new FrameVector();
   private final FramePoint stanceFootPosition = new FramePoint();
   
   private final RecyclingArrayList<FramePoint> positionWaypointsForSole = new RecyclingArrayList<>(FramePoint.class);
   private final RecyclingArrayList<FrameSE3TrajectoryPoint> swingWaypoints = new RecyclingArrayList<>(FrameSE3TrajectoryPoint.class);

   private final DoubleYoVariable swingDuration;
   private final DoubleYoVariable swingHeight;

   private final DoubleYoVariable swingTimeSpeedUpFactor;
   private final DoubleYoVariable maxSwingTimeSpeedUpFactor;
   private final DoubleYoVariable minSwingTimeForDisturbanceRecovery;
   private final BooleanYoVariable isSwingSpeedUpEnabled;
   private final DoubleYoVariable currentTime;
   private final DoubleYoVariable currentTimeWithSwingSpeedUp;

   private final DoubleYoVariable finalSwingHeightOffset;
   private final double controlDT;

   private final DoubleYoVariable minHeightDifferenceForObstacleClearance;

   private final ReferenceFrame soleFrame;
   private final ReferenceFrame controlFrame;

   private final PoseReferenceFrame desiredSoleFrame = new PoseReferenceFrame("desiredSoleFrame", worldFrame);
   private final PoseReferenceFrame desiredControlFrame = new PoseReferenceFrame("desiredControlFrame", desiredSoleFrame);
   private final RigidBodyTransform soleToControlFrameTransform = new RigidBodyTransform();
   private final FramePose desiredPose = new FramePose();
   private final Twist desiredTwist = new Twist();
   private final SpatialAccelerationVector desiredSpatialAcceleration = new SpatialAccelerationVector();

   private final RigidBodyTransform transformFromToeToAnkle = new RigidBodyTransform();

   private final DoubleYoVariable velocityAdjustmentDamping;
   private final YoFrameVector adjustmentVelocityCorrection;
   private final FramePoint unadjustedPosition = new FramePoint(worldFrame);
   
   private final FramePose footstepPose = new FramePose();
   private final FramePose lastFootstepPose = new FramePose();
   
   private final FrameEuclideanTrajectoryPoint tempPositionTrajectoryPoint = new FrameEuclideanTrajectoryPoint();

   public SwingState(FootControlHelper footControlHelper, YoFrameVector yoTouchdownVelocity, YoFrameVector yoTouchdownAcceleration,
         YoSE3PIDGainsInterface gains, YoVariableRegistry registry)
   {
      super(ConstraintType.SWING, footControlHelper, gains, registry);
      
      this.yoTouchdownAcceleration = yoTouchdownAcceleration;
      this.yoTouchdownVelocity = yoTouchdownVelocity;

      controlDT = footControlHelper.getHighLevelHumanoidControllerToolbox().getControlDT();

      String namePrefix = robotSide.getCamelCaseNameForStartOfExpression() + "Foot";
      WalkingControllerParameters walkingControllerParameters = footControlHelper.getWalkingControllerParameters();

      finalSwingHeightOffset = new DoubleYoVariable(namePrefix + "SwingFinalHeightOffset", registry);
      finalSwingHeightOffset.set(footControlHelper.getWalkingControllerParameters().getDesiredTouchdownHeightOffset());
      replanTrajectory = new BooleanYoVariable(namePrefix + "SwingReplanTrajectory", registry);

      minHeightDifferenceForObstacleClearance = new DoubleYoVariable(namePrefix + "MinHeightDifferenceForObstacleClearance", registry);
      minHeightDifferenceForObstacleClearance.set(walkingControllerParameters.getMinHeightDifferenceForStepUpOrDown());

      velocityAdjustmentDamping = new DoubleYoVariable(namePrefix + "VelocityAdjustmentDamping", registry);
      velocityAdjustmentDamping.set(footControlHelper.getWalkingControllerParameters().getSwingFootVelocityAdjustmentDamping());
      adjustmentVelocityCorrection = new YoFrameVector(namePrefix + "AdjustmentVelocityCorrection", worldFrame, registry);

      // todo make a smarter distinction on this as a way to work with the push recovery module
      doContinuousReplanning = new BooleanYoVariable(namePrefix + "DoContinuousReplanning", registry);

      soleFrame = footControlHelper.getHighLevelHumanoidControllerToolbox().getReferenceFrames().getSoleFrame(robotSide);
      ReferenceFrame footFrame = contactableFoot.getFrameAfterParentJoint();
      ReferenceFrame toeFrame = createToeFrame(robotSide);
      controlFrame = walkingControllerParameters.controlToeDuringSwing() ? toeFrame : footFrame;
      controlFrame.getTransformToDesiredFrame(soleToControlFrameTransform, soleFrame);
      desiredControlFrame.setPoseAndUpdate(soleToControlFrameTransform);

      TwistCalculator twistCalculator = controllerToolbox.getTwistCalculator();
      RigidBody foot = contactableFoot.getRigidBody();

      oppositeSoleFrame = controllerToolbox.getReferenceFrames().getSoleFrame(robotSide.getOppositeSide());

      double maxSwingHeightFromStanceFoot = 0.0;
      double minSwingHeightFromStanceFoot = 0.0;
      if (walkingControllerParameters != null)
      {
         maxSwingHeightFromStanceFoot = walkingControllerParameters.getMaxSwingHeightFromStanceFoot();
         minSwingHeightFromStanceFoot = walkingControllerParameters.getMinSwingHeightFromStanceFoot();
      }

      YoGraphicsListRegistry yoGraphicsListRegistry = controllerToolbox.getYoGraphicsListRegistry();

      swingTrajectoryOptimizer = new TwoWaypointSwingGenerator(namePrefix + "Swing", minSwingHeightFromStanceFoot, maxSwingHeightFromStanceFoot, registry, yoGraphicsListRegistry);
      swingTrajectory = new MultipleWaypointsPoseTrajectoryGenerator(namePrefix + "Swing", 10, registry);
      touchdownTrajectory = new SoftTouchdownPoseTrajectoryGenerator(namePrefix + "Touchdown", registry);
      currentStateProvider = new CurrentStateProvider(soleFrame, foot, twistCalculator);
      
      swingDuration = new DoubleYoVariable(namePrefix + "SwingDuration", registry);
      swingHeight = new DoubleYoVariable(namePrefix + "SwingHeight", registry);
      
      swingTimeSpeedUpFactor = new DoubleYoVariable(namePrefix + "SwingTimeSpeedUpFactor", registry);
      minSwingTimeForDisturbanceRecovery = new DoubleYoVariable(namePrefix + "MinSwingTimeForDisturbanceRecovery", registry);
      minSwingTimeForDisturbanceRecovery.set(walkingControllerParameters.getMinimumSwingTimeForDisturbanceRecovery());
      maxSwingTimeSpeedUpFactor = new DoubleYoVariable(namePrefix + "MaxSwingTimeSpeedUpFactor", registry);
      currentTime = new DoubleYoVariable(namePrefix + "CurrentTime", registry);
      currentTimeWithSwingSpeedUp = new DoubleYoVariable(namePrefix + "CurrentTimeWithSwingSpeedUp", registry);
      isSwingSpeedUpEnabled = new BooleanYoVariable(namePrefix + "IsSwingSpeedUpEnabled", registry);
      isSwingSpeedUpEnabled.set(walkingControllerParameters.allowDisturbanceRecoveryBySpeedingUpSwing());

      scaleSecondaryJointWeights.set(walkingControllerParameters.applySecondaryJointScaleDuringSwing());

      FramePose controlFramePose = new FramePose(controlFrame);
      controlFramePose.changeFrame(contactableFoot.getRigidBody().getBodyFixedFrame());
      changeControlFrame(controlFramePose);
      
      lastFootstepPose.setToNaN();
      footstepPose.setToNaN();
   }

   private ReferenceFrame createToeFrame(RobotSide robotSide)
   {
      ContactableFoot contactableFoot = controllerToolbox.getContactableFeet().get(robotSide);
      ReferenceFrame footFrame = controllerToolbox.getReferenceFrames().getFootFrame(robotSide);
      FramePoint2d toeContactPoint2d = new FramePoint2d();
      contactableFoot.getToeOffContactPoint(toeContactPoint2d);
      FramePoint toeContactPoint = new FramePoint();
      toeContactPoint.setXYIncludingFrame(toeContactPoint2d);
      toeContactPoint.changeFrame(footFrame);

      transformFromToeToAnkle.setTranslation(toeContactPoint.getVectorCopy());
      return ReferenceFrame.constructFrameWithUnchangingTransformToParent(robotSide.getCamelCaseNameForStartOfExpression() + "ToeFrame", footFrame, transformFromToeToAnkle);
   }

   @Override
   protected void initializeTrajectory()
   {
      currentStateProvider.getPosition(initialPosition);
      currentStateProvider.getLinearVelocity(initialLinearVelocity);
      currentStateProvider.getOrientation(initialOrientation);
      currentStateProvider.getAngularVelocity(initialAngularVelocity);
      stanceFootPosition.setToZero(oppositeSoleFrame);
      
      fillAndInitializeTrajectories(true);
   }
   
   private void fillAndInitializeTrajectories(boolean initializeOptimizer)
   {
      footstepPose.getPoseIncludingFrame(finalPosition, finalOrientation);
      finalLinearVelocity.setIncludingFrame(yoTouchdownVelocity.getFrameTuple());
      finalAngularVelocity.setToZero(worldFrame);
      
      // append current pose as initial trajectory point
      swingTrajectory.clear(worldFrame);
      swingTrajectory.appendPositionWaypoint(0.0, initialPosition, initialLinearVelocity);
      swingTrajectory.appendOrientationWaypoint(0.0, initialOrientation, initialAngularVelocity);

      if (activeTrajectoryType == TrajectoryType.WAYPOINTS)
      {
         for (int i = 0; i < swingWaypoints.size(); i++)
         {
            swingTrajectory.appendPoseWaypoint(swingWaypoints.get(i));
         }
      }
      else
      {
         if (initializeOptimizer)
         {
            initializeOptimizer();
         }
         
         for (int i = 0; i < swingTrajectoryOptimizer.getNumberOfWaypoints(); i++)
         {
            swingTrajectoryOptimizer.getWaypointData(i, tempPositionTrajectoryPoint);
            swingTrajectory.appendPositionWaypoint(tempPositionTrajectoryPoint);
         }
         
      }
      
      // append footstep pose
      double swingDuration = this.swingDuration.getDoubleValue();
      swingTrajectory.appendPositionWaypoint(swingDuration, finalPosition, finalLinearVelocity);
      swingTrajectory.appendOrientationWaypoint(swingDuration, finalOrientation, finalAngularVelocity);
      
      // setup touchdown trajectory
      // TODO: revisit the touchdown velocity and accelerations
      FrameVector touchdownAcceleration = yoTouchdownAcceleration.getFrameTuple();
      touchdownTrajectory.setLinearTrajectory(swingDuration, finalPosition, finalLinearVelocity, touchdownAcceleration);
      
      swingTrajectory.initialize();
      touchdownTrajectory.initialize();
   }

   private void initializeOptimizer()
   {
      swingTrajectoryOptimizer.setInitialConditions(initialPosition, initialLinearVelocity);
      swingTrajectoryOptimizer.setFinalConditions(finalPosition, finalLinearVelocity);
      swingTrajectoryOptimizer.setStepTime(swingDuration.getDoubleValue());
      swingTrajectoryOptimizer.setTrajectoryType(activeTrajectoryType, positionWaypointsForSole);
      swingTrajectoryOptimizer.setSwingHeight(swingHeight.getDoubleValue());
      swingTrajectoryOptimizer.setStanceFootPosition(stanceFootPosition);
      swingTrajectoryOptimizer.initialize();
   }

   protected void reinitializeTrajectory(boolean initializeOptimizer)
   {
      // Can not yet replan if trajectory type is WAYPOINTS
      if (activeTrajectoryType == TrajectoryType.WAYPOINTS)
      {
         return;
      }
      
      fillAndInitializeTrajectories(initializeOptimizer);
   }

   @Override
   protected void computeAndPackTrajectory()
   {
      if (this.replanTrajectory.getBooleanValue()) // This seems like a bad place for this?
      {
         if (!doContinuousReplanning.getBooleanValue())
         {
            reinitializeTrajectory(true);
            replanTrajectory.set(false);
         }
      }

      currentTime.set(getTimeInCurrentState());

      double time;
      if (!isSwingSpeedUpEnabled.getBooleanValue() || currentTimeWithSwingSpeedUp.isNaN())
      {
         time = currentTime.getDoubleValue();
      }
      else
      {
         currentTimeWithSwingSpeedUp.add(swingTimeSpeedUpFactor.getDoubleValue() * controlDT);
         time = currentTimeWithSwingSpeedUp.getDoubleValue();
      }
      
      PoseTrajectoryGenerator activeTrajectory;
      if (time > swingDuration.getDoubleValue())
      {
         activeTrajectory = touchdownTrajectory;
      }
      else
      {
         activeTrajectory = swingTrajectory;
      }
      
      boolean footstepWasAdjusted = false;
      if (replanTrajectory.getBooleanValue())
      {
         activeTrajectory.compute(time);
         activeTrajectory.getPosition(unadjustedPosition);
         reinitializeTrajectory(true);

         footstepWasAdjusted = true;
         replanTrajectory.set(false);
      }
      else if (activeTrajectoryType != TrajectoryType.WAYPOINTS)
      {
         if (swingTrajectoryOptimizer.doOptimizationUpdate())
         {
            reinitializeTrajectory(false);
         }
      }

      activeTrajectory.compute(time);
      activeTrajectory.getLinearData(desiredPosition, desiredLinearVelocity, desiredLinearAcceleration);
      activeTrajectory.getAngularData(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);

      if (footstepWasAdjusted)
      {
         adjustmentVelocityCorrection.set(desiredPosition);
         adjustmentVelocityCorrection.sub(unadjustedPosition);
         adjustmentVelocityCorrection.scale(1.0 / controlDT);
         adjustmentVelocityCorrection.setZ(0.0);
         adjustmentVelocityCorrection.scale(velocityAdjustmentDamping.getDoubleValue());

         desiredLinearVelocity.add(adjustmentVelocityCorrection.getFrameTuple());
      }
      else
      {
         adjustmentVelocityCorrection.setToZero();
      }

      if (isSwingSpeedUpEnabled.getBooleanValue() && !currentTimeWithSwingSpeedUp.isNaN())
      {
         desiredLinearVelocity.scale(swingTimeSpeedUpFactor.getDoubleValue());
         desiredAngularVelocity.scale(swingTimeSpeedUpFactor.getDoubleValue());

         double speedUpFactorSquared = swingTimeSpeedUpFactor.getDoubleValue() * swingTimeSpeedUpFactor.getDoubleValue();
         desiredLinearAcceleration.scale(speedUpFactorSquared);
         desiredAngularAcceleration.scale(speedUpFactorSquared);
      }

      transformDesiredsFromSoleFrameToControlFrame();

      secondaryJointWeightScale.set(computeScaleFactor(time));
   }

   private void transformDesiredsFromSoleFrameToControlFrame()
   {
      desiredSoleFrame.setPoseAndUpdate(desiredPosition, desiredOrientation);

      // change pose
      desiredPose.setToZero(desiredControlFrame);
      desiredPose.changeFrame(worldFrame);
      desiredPose.getPosition(desiredPosition.getPoint());
      desiredPose.getOrientation(desiredOrientation.getQuaternion());

      // change twist
      desiredLinearVelocity.changeFrame(desiredSoleFrame);
      desiredAngularVelocity.changeFrame(desiredSoleFrame);
      desiredTwist.set(desiredSoleFrame, worldFrame, desiredSoleFrame, desiredLinearVelocity, desiredAngularVelocity);
      desiredTwist.changeFrame(desiredControlFrame);
      desiredTwist.getLinearPart(desiredLinearVelocity);
      desiredTwist.getAngularPart(desiredAngularVelocity);
      desiredLinearVelocity.changeFrame(worldFrame);
      desiredAngularVelocity.changeFrame(worldFrame);

      // change spatial acceleration
      desiredLinearAcceleration.changeFrame(desiredSoleFrame);
      desiredAngularAcceleration.changeFrame(desiredSoleFrame);
      desiredSpatialAcceleration.set(desiredSoleFrame, worldFrame, desiredSoleFrame, desiredLinearAcceleration, desiredAngularAcceleration);
      desiredSpatialAcceleration.changeFrameNoRelativeMotion(desiredControlFrame);
      desiredSpatialAcceleration.getLinearPart(desiredLinearAcceleration);
      desiredSpatialAcceleration.getAngularPart(desiredAngularAcceleration);
      desiredLinearAcceleration.changeFrame(worldFrame);
      desiredAngularAcceleration.changeFrame(worldFrame);
   }

   private double computeScaleFactor(double time)
   {
      double phaseInSwingState = time / swingDuration.getDoubleValue();

      return  (maxScalingFactor - minScalingFactor) * (1.0 - Math.exp(-exponentialScalingRate * phaseInSwingState)) + minScalingFactor;
   }

   public void setFootstep(Footstep footstep, double swingTime)
   {
      swingDuration.set(swingTime);
      maxSwingTimeSpeedUpFactor.set(Math.max(swingTime / minSwingTimeForDisturbanceRecovery.getDoubleValue(), 1.0));

      lastFootstepPose.setIncludingFrame(footstepPose);
      if (lastFootstepPose.containsNaN())
         lastFootstepPose.setToZero(soleFrame);

      footstep.getPose(footstepPose);
      footstepPose.changeFrame(worldFrame);
      footstepPose.setZ(footstepPose.getZ() + finalSwingHeightOffset.getDoubleValue());

      // if replanning do not change the original trajectory type or waypoints
      if (replanTrajectory.getBooleanValue())
         return;

      activeTrajectoryType = footstep.getTrajectoryType();
      this.positionWaypointsForSole.clear();
      this.swingWaypoints.clear();
      lastFootstepPose.changeFrame(worldFrame);

      if (activeTrajectoryType == TrajectoryType.CUSTOM)
      {
         List<FramePoint> positionWaypointsForSole = footstep.getCustomPositionWaypoints();
         for (int i = 0; i < positionWaypointsForSole.size(); i++)
            this.positionWaypointsForSole.add().setIncludingFrame(positionWaypointsForSole.get(i));
      }
      else if (activeTrajectoryType == TrajectoryType.WAYPOINTS)
      {
         List<FrameSE3TrajectoryPoint> swingWaypoints = footstep.getSwingTrajectory();
         for (int i = 0; i < swingWaypoints.size(); i++)
            this.swingWaypoints.add().set(swingWaypoints.get(i));
      }
      else
      {
         swingHeight.set(footstep.getSwingHeight());

         double zDifference = Math.abs(footstepPose.getZ() - lastFootstepPose.getZ());
         boolean stepUpOrDown = zDifference > minHeightDifferenceForObstacleClearance.getDoubleValue();

         if (stepUpOrDown)
         {
            activeTrajectoryType = TrajectoryType.OBSTACLE_CLEARANCE;
         }
      }
   }

   public void replanTrajectory(Footstep newFootstep, double swingTime, boolean continuousReplan)
   {
      replanTrajectory.set(true);
      setFootstep(newFootstep, swingTime);
      doContinuousReplanning.set(continuousReplan);
   }

   private double computeSwingTimeRemaining()
   {
      double swingDuration = this.swingDuration.getDoubleValue();
      if (!currentTimeWithSwingSpeedUp.isNaN())
      {
         double swingTimeRemaining = (swingDuration - currentTimeWithSwingSpeedUp.getDoubleValue()) / swingTimeSpeedUpFactor.getDoubleValue();
         return swingTimeRemaining;
      }
      else
      {
         return swingDuration - getTimeInCurrentState();
      }
   }

   /**
    * Request the swing trajectory to speed up using the given speed up factor.
    * It is clamped w.r.t. to {@link WalkingControllerParameters#getMinimumSwingTimeForDisturbanceRecovery()}.
    * @param speedUpFactor
    * @return the current swing time remaining for the swing foot trajectory
    */
   public double requestSwingSpeedUp(double speedUpFactor)
   {
      if (isSwingSpeedUpEnabled.getBooleanValue() && (speedUpFactor > 1.1 && speedUpFactor > swingTimeSpeedUpFactor.getDoubleValue()))
      {
         speedUpFactor = MathTools.clamp(speedUpFactor, swingTimeSpeedUpFactor.getDoubleValue(), maxSwingTimeSpeedUpFactor.getDoubleValue());

         //         speedUpFactor = MathTools.clipToMinMax(speedUpFactor, 0.7, maxSwingTimeSpeedUpFactor.getDoubleValue());
         //         if (speedUpFactor < 1.0) speedUpFactor = 1.0 - 0.5 * (1.0 - speedUpFactor);

         swingTimeSpeedUpFactor.set(speedUpFactor);
         if (currentTimeWithSwingSpeedUp.isNaN())
            currentTimeWithSwingSpeedUp.set(currentTime.getDoubleValue());
      }

      return computeSwingTimeRemaining();
   }

   @Override
   public void doTransitionIntoAction()
   {
      super.doTransitionIntoAction();
      swingTimeSpeedUpFactor.set(1.0);
      currentTimeWithSwingSpeedUp.set(Double.NaN);
      replanTrajectory.set(false);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      super.doTransitionOutOfAction();

      swingTimeSpeedUpFactor.set(Double.NaN);
      currentTimeWithSwingSpeedUp.set(Double.NaN);

      swingTrajectoryOptimizer.informDone();

      adjustmentVelocityCorrection.setToZero();
   }
}
