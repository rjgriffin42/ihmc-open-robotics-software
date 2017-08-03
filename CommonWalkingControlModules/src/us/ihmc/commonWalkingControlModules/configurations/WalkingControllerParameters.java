package us.ihmc.commonWalkingControlModules.configurations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import us.ihmc.commonWalkingControlModules.controlModules.foot.ExplorationParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.ToeSlippingDetector;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOffsetTrajectoryWhileWalking;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.JointAccelerationIntegrationSettings;
import us.ihmc.commonWalkingControlModules.controllerCore.parameters.JointAccelerationIntegrationParametersReadOnly;
import us.ihmc.commonWalkingControlModules.dynamicReachability.DynamicReachabilityCalculator;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ICPControlGains;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.JointLimitParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.YoPDGains;
import us.ihmc.robotics.controllers.YoPIDGains;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.partNames.NeckJointName;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.sensorProcessing.stateEstimation.FootSwitchType;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public abstract class WalkingControllerParameters implements SteppingParameters
{
   private final LegConfigurationParameters legConfigurationParameters;
   private final JointPrivilegedConfigurationParameters jointPrivilegedConfigurationParameters;
   private final DynamicReachabilityParameters dynamicReachabilityParameters;
   private final PelvisOffsetWhileWalkingParameters pelvisOffsetWhileWalkingParameters;
   private final LeapOfFaithParameters leapOfFaithParameters;

   public WalkingControllerParameters()
   {
      jointPrivilegedConfigurationParameters = new JointPrivilegedConfigurationParameters();
      dynamicReachabilityParameters = new DynamicReachabilityParameters();
      pelvisOffsetWhileWalkingParameters = new PelvisOffsetWhileWalkingParameters();
      leapOfFaithParameters = new LeapOfFaithParameters();
      legConfigurationParameters = new LegConfigurationParameters();
   }

   /**
    * Specifies if the controller should by default compute for all the robot joints desired
    * position and desired velocity from the desired acceleration.
    * <p>
    * It is {@code false} by default and this method should be overridden to return otherwise.
    * </p>
    *
    * @return {@code true} if the desired acceleration should be integrated into desired velocity
    *         and position for all the joints.
    */
   public boolean enableJointAccelerationIntegrationForAllJoints()
   {
      return false;
   }

   /**
    * Returns a map from joint name to joint acceleration integration parameters.
    * <p>
    * Note that this method is only called if
    * {@link #enableJointAccelerationIntegrationForAllJoints()} returns {@code true}.
    * </p>
    * <p>
    * This method is called by the controller to know the set of joints for which specific
    * parameters are to be used. If a joint is not added to this map, the default parameters will be used.
    * </p>
    * Example a robot for which we want to provide specific parameters for the elbow joints only:</br>
    * {@code Map<String, JointAccelerationIntegrationParametersReadOnly> jointParameters = new HashMap<>();}</br>
    * {@code JointAccelerationIntegrationParametersReadOnly elbowParameters = new YoJointAccelerationIntegrationParameters("elbow", 0.999, 0.95, 0.1, 0.1, registry);}</br>
    * {@code jointParameters.put("leftElbow", elbowParameters);}</br>
    * {@code jointParameters.put("rightElbow", elbowParameters);}</br>
    * {@code return jointParameters;}</br>
    * </p>
    * @param registry the controller registry allowing to create {@code YoVariable}s for the
    *           parameters.
    * @return the map from the names of the joints with their specific parameters to use.
    *
    * TODO: remove registry
    */
   public Map<String, JointAccelerationIntegrationParametersReadOnly> getJointAccelerationIntegrationParameters(YoVariableRegistry registry)
   {
      return null;
   }

   /**
    * Returns the value of sqrt(g / z0) which corrsponds to omega0 in the Linear Inverted Pendulum
    * Model that the ICP is based on. Note, that this value is a tuning parameter for each robot and
    * is not computed from the actual CoM height.
    *
    * @return the value for omega0 that is used in the controller for ICP related computations.
    */
   public abstract double getOmega0();

   /**
    * Specifies if the controller should attempt at detecting foot slipping during toe off when
    * walking. If foot slip is detected the swing is started right away.
    *
    * @return whether the controller will detect foot slipping during the toe off state
    */
   public boolean enableToeOffSlippingDetection()
   {
      return false;
   }

   /**
    * Method will set robot specific parameters in the provided {@link #ToeSlippingDetector}.
    * Note that this method is only called if {@link #enableToeOffSlippingDetection()} returns {@code true}.
    * </p>
    * Override this method to configure the parameters as follows:
    * </p>
    * {@code double forceMagnitudeThreshold = 25.0;}</br>
    * {@code double velocityThreshold = 0.4;}</br>
    * {@code double double slippageDistanceThreshold = 0.04;}</br>
    * {@code double filterBreakFrequency = 10.0;}</br>
    * {@code toeSlippingDetectorToConfigure.configure(forceMagnitudeThreshold, velocityThreshold, slippageDistanceThreshold, filterBreakFrequency);}
    * </p>
    * @param toeSlippingDetectorToConfigure (modified)
    * @see ToeSlippingDetector#configure(double, double, double, double)
    */
   public void configureToeSlippingDetector(ToeSlippingDetector toeSlippingDetectorToConfigure)
   {
      throw new RuntimeException("Override this method if using the " + ToeSlippingDetector.class.getSimpleName());
   }

   /**
    * If the return value is {@code true} the controller will speed up the swing of a foot when walking to match
    * the desired ICP to the current ICP. See {@link #getICPErrorThresholdToSpeedUpSwing()} to specify the threshold
    * on the ICP error which will cause a swing speedup.
    *
    * @return whether swing speedup is enabled
    */
   public abstract boolean allowDisturbanceRecoveryBySpeedingUpSwing();

   /**
    * Parameter determines the minimum swing time in case the controller is speeding up the swing.
    *
    * @return minimum value the controller can reduce the swing time to when recovering
    * @see #allowDisturbanceRecoveryBySpeedingUpSwing()
    */
   public abstract double getMinimumSwingTimeForDisturbanceRecovery();

   /**
    * Determines the threshold on the ICP error that will cause the controller to speed up the swing when in single
    * support. Note that this will only have an effect if {@link #allowDisturbanceRecoveryBySpeedingUpSwing()} returns
    * {@code true}.
    *
    * @return the threshold on the ICP error to trigger swing speedup
    */
   public abstract double getICPErrorThresholdToSpeedUpSwing();

   /**
    * Specifies whether the controller will abort any arm trajectories when close to loosing its balance. This is
    * determined by the ICP tracking error.
    * TODO: extract threshold.
    *
    * @return whether the robot will abort arm trajectories when the ICP error is above a threshold
    */
   public abstract boolean allowAutomaticManipulationAbort();

   /**
    * Determines whether to use the ICP Optimization controller or a standard ICP proportional controller (new feature to be tested with Atlas)
    *
    * @return boolean (true = use ICP Optimization, false = use ICP Proportional Controller)
    */
   public abstract boolean useOptimizationBasedICPController();

   /**
    * The desired position of the CMP is computed based on a feedback control law on the ICP. This method returns
    * the gains used in this controller.
    *
    * TODO: remove registry
    */
   public abstract ICPControlGains createICPControlGains(YoVariableRegistry registry);

   /**
    * This method returns the gains used in the controller to regulate the center of mass height.
    *
    * TODO: remove registry
    */
   public abstract YoPDGains createCoMHeightControlGains(YoVariableRegistry registry);

   /**
    * The map returned contains all controller gains for tracking jointspace trajectories. The key of
    * the map is the joint name as defined in the robot joint map. If a joint is not contained in the
    * map, jointspace control is not supported for that joint.
    *
    * @param registry used to create the gains the first time this function is called during a run
    * @return map containing jointspace PID gains by joint name
    *
    * TODO: remove registry
    */
   public Map<String, YoPIDGains> getOrCreateJointSpaceControlGains(YoVariableRegistry registry)
   {
      return new HashMap<String, YoPIDGains>();
   }

   /**
    * The map returned contains all controller gains for tracking taskspace orientation trajectories
    * (or the orientation part of a pose trajectory) for a rigid body. The key of the map is the rigid
    * body name as defined in the robot joint map. If a joint is not contained in the map, taskspace
    * orientation or pose control is not supported for that rigid body.
    *
    * @param registry used to create the gains the first time this function is called during a run
    * @return map containing taskspace orientation PID gains by rigid body name
    *
    * TODO: remove registry
    */
   public Map<String, YoOrientationPIDGainsInterface> getOrCreateTaskspaceOrientationControlGains(YoVariableRegistry registry)
   {
      return new HashMap<String, YoOrientationPIDGainsInterface>();
   }

   /**
    * The map returned contains all controller gains for tracking taskspace position trajectories
    * (or the position part of a pose trajectory) for a rigid body. The key of the map is the rigid
    * body name as defined in the robot joint map. If a joint is not contained in the map, taskspace
    * position or pose control is not supported for that rigid body.
    *
    * @param registry used to create the gains the first time this function is called during a run
    * @return map containing taskspace position PID gains by rigid body name
    *
    * TODO: remove registry
    */
   public Map<String, YoPositionPIDGainsInterface> getOrCreateTaskspacePositionControlGains(YoVariableRegistry registry)
   {
      return new HashMap<String, YoPositionPIDGainsInterface>();
   }

   /**
    * Returns the default control mode for a rigid body. The modes are defined in {@link RigidBodyControlMode}
    * and by default the mode should be {@link RigidBodyControlMode#JOINTSPACE}. In some cases (e.g. the chest)
    * it makes more sense to use the default mode {@link RigidBodyControlMode#TASKSPACE}.
    *
    * @param bodyName is the name of the {@link RigidBody}
    * @return the default control mode of the body
    */
   public RigidBodyControlMode getDefaultControlModeForRigidBody(String bodyName)
   {
      return RigidBodyControlMode.JOINTSPACE;
   }

   /**
    * The map returned contains the default home joint angles. The key of the map is the joint name
    * as defined in the robot joint map.
    *
    * @return map containing home joint angles by joint name
    */
   public TObjectDoubleHashMap<String> getOrCreateJointHomeConfiguration()
   {
      return new TObjectDoubleHashMap<String>();
   }

   /**
    * The map returned contains the default rigid body poses in their respective base frame. For example, if the base
    * frame of the chest body is the pelvis z-up frame this should contain the home pose of the chest in that frame.
    * If the particular body does not support full pose control but only orientation control the position part of the
    * pose will be disregarded.
    * <p>
    * The key of the map is the name of the rigid body that can be obtained with {@link RigidBody#getName()}. If a
    * body is not contained in this map but a default control mode of {@link RigidBodyControlMode#TASKSPACE} is not
    * supported for that body.
    *
    * @return map containing home pose in base frame by body name
    */
   public Map<String, Pose3D> getOrCreateBodyHomeConfiguration()
   {
      return new HashMap<String, Pose3D>();
   }

   /**
    * The list of strings returned contains all joint names that are position controlled. The names
    * of the joints are defined in the robots joint map.
    *
    * @return list of position controlled joint names
    */
   public List<String> getOrCreatePositionControlledJoints()
   {
      return new ArrayList<String>();
   }

   /**
    * The map returned contains the integration settings for position controlled joints. The settings
    * define how the controller core integrated desired accelerations to find desired joint positions
    * and velocities. The key of the map is the joint name as defined in the robot joint map. If a
    * joint is not contained in the map, position control is not supported for that joint.
    *
    * @return map containing acceleration integration settings by joint name
    */
   public Map<String, JointAccelerationIntegrationSettings> getOrCreateIntegrationSettings()
   {
      return new HashMap<String, JointAccelerationIntegrationSettings>();
   }

   /**
    * Returns the gains used for the foot pose when in swing.
    *
    * TODO: remove registry
    */
   public abstract YoSE3PIDGainsInterface createSwingFootControlGains(YoVariableRegistry registry);

   /**
    * Returns the gains used for the foot when in support. Note that these gains are only used when the foot
    * is not loaded or close to tipping. Of that is not the case the foot pose when in support is not controlled
    * using a feedback controller.
    *
    * TODO: remove registry
    */
   public abstract YoSE3PIDGainsInterface createHoldPositionFootControlGains(YoVariableRegistry registry);

   /**
    * Returns the gains used for the foot when in the toe off state. Note that some parts of the foot orientation
    * will not use these gains. The foot pitch for example is usually not controlled explicitly during tow off.
    *
    * TODO: remove registry
    */
   public abstract YoSE3PIDGainsInterface createToeOffFootControlGains(YoVariableRegistry registry);

   /**
    * Specifies if the arm controller should be switching
    * to chest frame or jointspace only if necessary.
    * This is particularly useful when manipulation was performed
    * with respect to world during standing to prevent "leaving a hand behind"
    * when the robot starts walking.
    *
    * @return whether the manipulation control should get prepared
    *  for walking.
    */
   public abstract boolean doPrepareManipulationForLocomotion();

   /**
    * Specifies if the pelvis orientation controller should
    * be initialized before starting to walk.
    * When the controller is initialized, the pelvis will
    * smoothly cancel out the user orientation offset on
    * the first transfer of a walking sequence.
    *
    * @return whether the pelvis orientation control should get prepared
    *  for walking.
    */
   public boolean doPreparePelvisForLocomotion()
   {
      return true;
   }

   /**
    * Specifies whether upper-body motion is allowed when the robot is walking
    * or during any exchange support.
    *
    * @return whether the upper-body can be moved during walking or not.
    */
   public boolean allowUpperBodyMotionDuringLocomotion()
   {
      return false;
   }

   /**
    * The default transfer time used in the walking controller. This is the time interval spent in double support shifting
    * the weight from one foot to the other while walking.
    */
   public abstract double getDefaultTransferTime();

   /**
    * The default swing time used in the walking controller. This is the time interval spent in single support moving the
    * swing foot to the next foothold.
    */
   public abstract double getDefaultSwingTime();

   /**
    * This is the default transfer time used in the walking controller to shift the weight back to the center of the feet
    * after executing a footstep plan.
    */
   public double getDefaultFinalTransferTime()
   {
      return getDefaultTransferTime();
   }

   /**
    * Ramps up the maximum loading of the normal force of the toe contact points over time, if returns true. If returns false, it simply
    * immediately sets the normal force maximum to infinity.
    *
    * @return whether or not to ramp up.
    */
   public boolean rampUpAllowableToeLoadAfterContact()
   {
      return false;
   }

   /**
    * Defines the duration spent ramping up the allowable normal toe contact force if {@link #rampUpAllowableToeLoadAfterContact()} is true.
    *
    * @return duration (s)
    */
   public double getToeLoadingDuration()
   {
      return 0.2;
   }

   /**
    * The maximum normal force allowed in the toe if {@link #rampUpAllowableToeLoadAfterContact()} is true at the time returned by
    * {@link #getToeLoadingDuration()}. After this time, the maximum normal force goes to infinity.
    * @return
    */
   public double getFullyLoadedToeForce()
   {
      return 1.0e3;
   }

   /**
    * This is the default transfer time used in the walking controller to shift the weight to the initial stance foot
    * when starting to execute a footstep plan.
    */
   public double getDefaultInitialTransferTime()
   {
      return 1.0;
   }

   /**
    * This is the minimum transfer time that the controller will allow when adjusting transfer times to achieve certain step
    * times in footstep plans.
    */
   public double getMinimumTransferTime()
   {
      return 0.1;
   }

   /**
    * Determines the type of footswitch used with the robot. Usually this will be wrench based if the robot can
    * sense the ground reaction forces.
    */
   public FootSwitchType getFootSwitchType()
   {
      return FootSwitchType.WrenchBased;
   }

   /**
    * When determining that a foot has hit the floor after a step the z-force on the foot needs to be past the
    * threshold defined by this method. In addition the center of pressure needs to be inside certain bounds of
    * the foot (see {@link #getCoPThresholdFraction()}).
    * </p>
    * See also {@link #getSecondContactThresholdForceIgnoringCoP()}
    * for another threshold on the contact force that does not require the CoP to be within bounds.
    * </p>
    * This will be used if the foot switch type as defined in {@link #getFootSwitchType()} is set to
    * {@link FootSwitchType#WrenchBased}
    */
   public abstract double getContactThresholdForce();

   /**
    * This threshold is a second boundary for the ground contact force required for the controller to assume
    * foot contact after a step. If the ground contact force in z goes above this threshold the foot touchdown
    * is triggered regardless of the position of the CoP within the foothold. See {@link #getContactThresholdForce}
    * for the first threshold.
    * </p>
    * This will be used if the foot switch type as defined in {@link #getFootSwitchType()} is set to
    * {@link FootSwitchType#WrenchBased}
    */
   public abstract double getSecondContactThresholdForceIgnoringCoP();

   /**
    * When determining whether a foot has touched down after a step the controller will make sure that the CoP
    * of the foot is within bounds before the touchdown is triggered. This fraction of the foot length is used
    * to move these bounds in. In addition the ground reaction force needs to be above the threshold defined in
    * {@link #getContactThresholdForce()}
    * </p>
    * This will be used if the foot switch type as defined in {@link #getFootSwitchType()} is set to
    * {@link FootSwitchType#WrenchBased}
    */
   public abstract double getCoPThresholdFraction();

   /**
    * When determining whether a foot has hit the ground the controller can use the height difference between the
    * swing foot and the lowest of the feet of the robot. If the difference falls below this threshold foot-ground
    * contact is assumed.
    * </p>
    * This will be used if the foot switch type as defined in {@link #getFootSwitchType()} is set to
    * {@link FootSwitchType#KinematicBased}
    */
   public double getContactThresholdHeight()
   {
      return 0.05;
   }

   /**
    * Returns a list of joints that will not be used by the controller.
    */
   public abstract String[] getJointsToIgnoreInController();

   /**
    * Returns the {@link MomentumOptimizationSettings} for this robot. These parameters define the weights
    * given to the objectives of the walking controller in the QP.
    */
   public abstract MomentumOptimizationSettings getMomentumOptimizationSettings();

   /**
    * Returns the {@link ICPAngularMomentumModifierParameters} for this robot. The parameters are used when
    * angular momentum rates are considered in the ICP planner.
    */
   public abstract ICPAngularMomentumModifierParameters getICPAngularMomentumModifierParameters();

   /**
    * This parameter is used when the controller checks if it is safe to transition from transfer to single
    * support state when walking. The transition is considered safe if the ICP tracking error lies within
    * an ellipse with the axes aligned with the z-up ankle frame of the stance foot. This parameter defines
    * the radius of the ellipse along the x-axis of that frame.
    * </p>
    * Note that if the ICP leaves the support area the single support state will be started regardless of the
    * ICP error in the hope to recover by stepping.
    * </p>
    * @see #getMaxICPErrorBeforeSingleSupportY()
    */
   public abstract double getMaxICPErrorBeforeSingleSupportX();

   /**
    * This parameter is used when the controller checks if it is safe to transition from transfer to single
    * support state when walking. The transition is considered safe if the ICP tracking error lies within
    * an ellipse with the axes aligned with the z-up ankle frame of the stance foot. This parameter defines
    * the radius of the ellipse along the y-axis of that frame.
    * </p>
    * Note that if the ICP leaves the support area the single support state will be started regardless of the
    * ICP error in the hope to recover by stepping.
    * </p>
    * @see #getMaxICPErrorBeforeSingleSupportX()
    */
   public abstract double getMaxICPErrorBeforeSingleSupportY();

   /**
    * Determines whether the controller should always leave the single support state after the expected
    * single support time has passed. If set to {@code false} the controller will wait for the foot switch to
    * trigger the transition.
    */
   public boolean finishSingleSupportWhenICPPlannerIsDone()
   {
      return false;
   }

   /**
    * This is the duration for which the desired foot center of pressure will be
    * drastically dampened to calm shakies. This particularly useful when
    * dealing with bad footholds.
    * Set to -1.0 to deactivate this feature.
    */
   public double getHighCoPDampingDurationToPreventFootShakies()
   {
      return -1.0;
   }

   /**
    * This is complimentary information to {@link #getHighCoPDampingDurationToPreventFootShakies()}.
    * The high CoP damping is triggered on large CoP tracking error.
    * Set to {@link Double#POSITIVE_INFINITY} to deactivate this feature.
    */
   public double getCoPErrorThresholdForHighCoPDamping()
   {
      return Double.POSITIVE_INFINITY;
   }

   /**
    * Get the parameters for foothold exploration. The parameters should be created the first time this
    * method is called.
    *
    * TODO: remove registry
    */
   public ExplorationParameters getOrCreateExplorationParameters(YoVariableRegistry registry)
   {
      return null;
   }

   /**
    * During normal execution the control algorithm computes a desired CMP. It is then projected in the
    * support polygon to avoid angular momentum of the upper body. When the robot is falling and recovery is
    * impossible otherwise, the support used for CMP projection can be increased and the robot uses upper body
    * momentum. This value defines the amount the support polygon for CMP projection is increased in that case.
    *
    * @return maxAllowedDistanceCMPSupport
    */
   public double getMaxAllowedDistanceCMPSupport()
   {
      return Double.NaN;
   }

   /**
    * Usually the desired CMP will be projected into the support area to avoid the generation of large amounts of
    * angular momentum. This method determines whether the desired CMP is allowed to be in area that is larger then
    * the support. The size of the area is determined by the value {@link #getMaxAllowedDistanceCMPSupport()}
    *
    * @return alwaysAllowMomentum
    */
   public boolean alwaysAllowMomentum()
   {
      return false;
   }

   /**
    * When true, some of the tracking performance will be degraded to reduce the generated angular momentum rate around
    * the vertical axis during swing only.
    * Useful when the robot has heavy legs and tends to slips during swing.
    * @return
    */
   public boolean minimizeAngularMomentumRateZDuringSwing()
   {
      return false;
   }

   /**
    * Determines whether the robot should use the velocity to be computed in the estimator, or just compute it from the robot state in the
    * controller (new feature to be tested with Atlas)
    */
   public boolean useCenterOfMassVelocityFromEstimator()
   {
      return false;
   }

   /**
    * Returns a list of joint that should use the more restrictive joint limit enforcement in the QP
    */
   public String[] getJointsWithRestrictiveLimits(JointLimitParameters jointLimitParametersToPack)
   {
      return new String[0];
   }

   @Override
   /**
    * Returns the minimum swing height from the stance foot for this robot. It is also the default swing height
    * used in the controller unless a different value is specified.
    */
   public double getMinSwingHeightFromStanceFoot()
   {
      return 0.1;
   }

   /**
    * Determines whether the swing of the robot controls the toe point of the foot for better tracking or not.
    * (new feature to be tested with Atlas)
    */
   public boolean controlToeDuringSwing()
   {
      return false;
   }

   /**
    * Returns the parameters used in the privileged configuration handler.
    */
   public JointPrivilegedConfigurationParameters getJointPrivilegedConfigurationParameters()
   {
      return jointPrivilegedConfigurationParameters;
   }

   /**
    * Returns the parameters used for straight leg walking
    */
   public LegConfigurationParameters getLegConfigurationParameters()
   {
      return legConfigurationParameters;
   }

   /**
    * Returns the parameters in the dynamic reachability calculator.
    */
   public DynamicReachabilityParameters getDynamicReachabilityParameters()
   {
      return dynamicReachabilityParameters;
   }

   /**
    * Determines whether or not to attempt to directly control the height.
    * If true, the height will be controlled by controlling either the pelvis or the center of mass height.
    * If false, the height will be controlled inside the nullspace by trying to achieve the desired
    * privileged configuration in the legs.
    * @return boolean (true = control height with momentum, false = do not control height with momentum)
    */
   public boolean controlHeightWithMomentum()
   {
      return true;
   }

   /**
    * Sets whether or not the {@link DynamicReachabilityCalculator} will simply check whether or not the
    * upcoming step is reachable using the given step timing ({@return} is false), or will edit the step timings
    * to make sure that the step is reachable if ({@return} is true).
    *
    * @return whether or not to edit the timing based on the reachability of the step.
    */
   public boolean editStepTimingForReachability()
   {
      return false;
   }

   /**
    * Whether or not to use a secondary joint scaling factor during swing, where the secondary joint is any joint
    * located in the kinematic chain between the base and the optional primary base of a SpatialAccelerationCommand
    * and a SpatialVelocityCommand.
    */
   public boolean applySecondaryJointScaleDuringSwing()
   {
      return false;
   }

   /**
    * Parameters for the {@link PelvisOffsetTrajectoryWhileWalking}. These parameters can be used to
    * shape the pelvis orientation trajectory while walking to create a more natural motion and
    * improve foot reachability.
    */
   public PelvisOffsetWhileWalkingParameters getPelvisOffsetWhileWalkingParameters()
   {
      return pelvisOffsetWhileWalkingParameters;
   }

   /**
    * Parameters for the 'Leap of Faith' Behavior. This caused the robot to activly fall onto an upcoming
    * foothold when necessary to reach an upcoming foothold. This method returns the robot specific
    * implementation of the {@link LeapOfFaithParameters};
    */
   public LeapOfFaithParameters getLeapOfFaithParameters()
   {
      return leapOfFaithParameters;
   }

   /**
    * Returns {@link ToeOffParameters} that contain all parameters relevant to the toe off state when walking.
    */
   public abstract ToeOffParameters getToeOffParameters();

   /**
    * Returns {@link SwingTrajectoryParameters} that contain all parameters relevant to the swing trajectory.
    */
   public abstract SwingTrajectoryParameters getSwingTrajectoryParameters();

   // remove: unused
   public abstract SideDependentList<RigidBodyTransform> getDesiredHandPosesWithRespectToChestFrame();

   // remove: unused
   public abstract String[] getDefaultChestOrientationControlJointNames();

   // move to UI specific parameters
   public abstract double getAnkleHeight();

   // replace: just add shin and thigh length from the physical parameters in a default method instead of forcing an implementation for each robot
   public abstract double getLegLength();

   // move to CoM height parameters
   public abstract double minimumHeightAboveAnkle();

   // move to CoM height parameters
   public abstract double nominalHeightAboveAnkle();

   // move to CoM height parameters
   public abstract double maximumHeightAboveAnkle();

   // move to CoM height parameters
   public abstract double defaultOffsetHeightAboveAnkle();

   // move to UI specific parameters
   public abstract double pelvisToAnkleThresholdForWalking();

   // remove: unused
   public abstract double getTimeToGetPreparedForLocomotion();

   // remove: unused (was only used in dead code that needs to go away)
   public abstract boolean getCoMHeightDriftCompensation();

   // remove: unused
   public abstract YoPDGains createUnconstrainedJointsControlGains(YoVariableRegistry registry);

   // remove from interface and nuke chest manager
   public abstract YoOrientationPIDGainsInterface createChestControlGains(YoVariableRegistry registry);

   // remove: unused
   public abstract boolean allowShrinkingSingleSupportFootPolygon();

   // move to slider board specific parameters
   public abstract boolean controlHeadAndHandsWithSliders();

   // remove: unused
   public abstract YoPDGains createPelvisICPBasedXYControlGains(YoVariableRegistry registry);

   // remove from interface and use getOrCreateTaskspaceOrientationControlGains instead
   public abstract YoOrientationPIDGainsInterface createPelvisOrientationControlGains(YoVariableRegistry registry);

   // remove: unused
   public abstract YoSE3PIDGainsInterface createEdgeTouchdownFootControlGains(YoVariableRegistry registry);

   // remove: unused
   public abstract double getSwingHeightMaxForPushRecoveryTrajectory();

   // move to UI specific parameters
   public abstract double getSpineYawLimit();

   // move to UI specific parameters
   public abstract double getSpinePitchUpperLimit();

   // move to UI specific parameters
   public abstract double getSpinePitchLowerLimit();

   // move to UI specific parameters
   public abstract double getSpineRollLimit();

   // move to UI specific parameters
   public abstract boolean isSpinePitchReversed();

   // remove: unused
   public abstract double getFoot_start_toetaper_from_back();

   // move to UI specific parameters
   public abstract double getSideLengthOfBoundingBoxForFootstepHeight();

   // move to slider board specific parameters
   public abstract LinkedHashMap<NeckJointName, ImmutablePair<Double, Double>> getSliderBoardControlledNeckJointsWithLimits();

   // move to slider board specific parameters
   public abstract SideDependentList<LinkedHashMap<String, ImmutablePair<Double, Double>>> getSliderBoardControlledFingerJointsWithLimits();

   // remove this once the support state is used in all robots
   public abstract boolean doFancyOnToesControl();

   // remove this once the support state is used in all robots
   public boolean useSupportState()
   {
      return false;
   }

   // move to UI specific parameters
   public double getDefaultTrajectoryTime()
   {
      return 3.0;
   }

   // remove: exo specific
   public abstract void useInverseDynamicsControlCore();

   // remove: exo specific
   public abstract void useVirtualModelControlCore();
}
