package us.ihmc.atlas.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import us.ihmc.atlas.AtlasJointMap;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.commonWalkingControlModules.configurations.ICPAngularMomentumModifierParameters;
import us.ihmc.commonWalkingControlModules.configurations.JointPrivilegedConfigurationParameters;
import us.ihmc.commonWalkingControlModules.configurations.LegConfigurationParameters;
import us.ihmc.commonWalkingControlModules.configurations.SteppingParameters;
import us.ihmc.commonWalkingControlModules.configurations.SwingTrajectoryParameters;
import us.ihmc.commonWalkingControlModules.configurations.ToeOffParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.ExplorationParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.ToeSlippingDetector;
import us.ihmc.commonWalkingControlModules.controlModules.foot.YoFootOrientationGains;
import us.ihmc.commonWalkingControlModules.controlModules.foot.YoFootSE3Gains;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.JointAccelerationIntegrationSettings;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ICPControlGains;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.JointLimitParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.YoPDGains;
import us.ihmc.robotics.controllers.YoPIDGains;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.controllers.YoSymmetricSE3PIDGains;
import us.ihmc.robotics.partNames.ArmJointName;
import us.ihmc.robotics.partNames.NeckJointName;
import us.ihmc.robotics.partNames.SpineJointName;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.stateEstimation.FootSwitchType;
import us.ihmc.yoVariables.registry.YoVariableRegistry;



public class AtlasWalkingControllerParameters extends WalkingControllerParameters
{
   private final RobotTarget target;
   private final boolean runningOnRealRobot;
   private final SideDependentList<RigidBodyTransform> handPosesWithRespectToChestFrame = new SideDependentList<RigidBodyTransform>();

   private static final boolean USE_SIMPLE_ICP_OPTIMIZATION = true;

// USE THESE FOR Real Atlas Robot and sims when controlling pelvis height instead of CoM.
   private final double minimumHeightAboveGround;// = 0.625;
   private double       nominalHeightAboveGround;// = 0.705;
   private final double maximumHeightAboveGround;// = 0.765 + 0.08;

   private final AtlasJointMap jointMap;
   private final AtlasMomentumOptimizationSettings momentumOptimizationSettings;
   private final ICPAngularMomentumModifierParameters angularMomentumModifierParameters;
   private final double massScale;

   private Map<String, YoPIDGains> jointspaceGains = null;
   private Map<String, YoOrientationPIDGainsInterface> taskspaceAngularGains = null;
   private Map<String, YoPositionPIDGainsInterface> taskspaceLinearGains = null;
   private TObjectDoubleHashMap<String> jointHomeConfiguration = null;
   private Map<String, Pose3D> bodyHomeConfiguration = null;
   private ArrayList<String> positionControlledJoints = null;
   private Map<String, JointAccelerationIntegrationSettings> integrationSettings = null;

   private final JointPrivilegedConfigurationParameters jointPrivilegedConfigurationParameters;
   private final LegConfigurationParameters legConfigurationParameters;
   private final ToeOffParameters toeOffParameters;
   private final SwingTrajectoryParameters swingTrajectoryParameters;
   private final ICPOptimizationParameters icpOptimizationParameters;
   private final AtlasSteppingParameters steppingParameters;

   public AtlasWalkingControllerParameters(RobotTarget target, AtlasJointMap jointMap, AtlasContactPointParameters contactPointParameters)
   {
      this.target = target;
      this.jointMap = jointMap;
      this.massScale = Math.pow(jointMap.getModelScale(), jointMap.getMassScalePower());

      momentumOptimizationSettings = new AtlasMomentumOptimizationSettings(jointMap, contactPointParameters.getNumberOfContactableBodies());
      angularMomentumModifierParameters = new ICPAngularMomentumModifierParameters();

      minimumHeightAboveGround = jointMap.getModelScale() * ( 0.625 );
      nominalHeightAboveGround = jointMap.getModelScale() * ( 0.705 );
      maximumHeightAboveGround = jointMap.getModelScale() * ( 0.765 + 0.08 );

      runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      jointPrivilegedConfigurationParameters = new AtlasJointPrivilegedConfigurationParameters(runningOnRealRobot);
      legConfigurationParameters = new AtlasLegConfigurationParameters(runningOnRealRobot);
      toeOffParameters = new AtlasToeOffParameters(jointMap);
      swingTrajectoryParameters = new AtlasSwingTrajectoryParameters(target, jointMap.getModelScale());
      steppingParameters = new AtlasSteppingParameters(jointMap);

      if (USE_SIMPLE_ICP_OPTIMIZATION)
         icpOptimizationParameters = new AtlasSimpleICPOptimizationParameters(runningOnRealRobot);
      else
         icpOptimizationParameters = new AtlasICPOptimizationParameters(runningOnRealRobot);

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBodyTransform transform = new RigidBodyTransform();

         double x = 0.20;
         double y = robotSide.negateIfRightSide(0.35);    // 0.30);
         double z = -0.40;
         Vector3D translation = new Vector3D(x, y, z);
         translation.scale(jointMap.getModelScale());
         transform.setTranslation(translation);

         RotationMatrix rotation = new RotationMatrix();
         double yaw = 0.0;    // robotSide.negateIfRightSide(-1.7);
         double pitch = 0.7;
         double roll = 0.0;    // robotSide.negateIfRightSide(-0.8);
         rotation.setYawPitchRoll(yaw, pitch, roll);
         transform.setRotation(rotation);

         handPosesWithRespectToChestFrame.put(robotSide, transform);
      }
   }

   @Override
   public double getOmega0()
   {
      // TODO probably need to be tuned.
      return (runningOnRealRobot ? 3.4 : 3.0) / Math.sqrt(jointMap.getModelScale()); // 3.0 seems more appropriate.
//      return 3.0;
   }

   /** {@inheritDoc} */
   @Override
   public boolean enableToeOffSlippingDetection()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public void configureToeSlippingDetector(ToeSlippingDetector toeSlippingDetectorToConfigure)
   {
      double forceMagnitudeThreshold = 25.0;
      double velocityThreshold = 0.4;
      double slippageDistanceThreshold = 0.04;
      double filterBreakFrequency = 10.0;
      toeSlippingDetectorToConfigure.configure(forceMagnitudeThreshold, velocityThreshold, slippageDistanceThreshold, filterBreakFrequency);
   }

   /** @inheritDoc */
   @Override
   public boolean allowDisturbanceRecoveryBySpeedingUpSwing()
   {
      return true; // TODO Seems to work well but still need to be heavily tested on the robot.
   }

   @Override
   public boolean allowAutomaticManipulationAbort()
   {
      return true;
   }

   @Override
   public double getICPErrorThresholdToSpeedUpSwing()
   {
      return jointMap.getModelScale() * 0.05;
   }

   @Override
   public double getMinimumSwingTimeForDisturbanceRecovery()
   {
      if (runningOnRealRobot)
         return 0.6;
      else
         return 0.3;
   }

// USE THESE FOR DRC Atlas Model TASK 2 UNTIL WALKING WORKS BETTER WITH OTHERS.
//   private final double minimumHeightAboveGround = 0.785;
//   private double nominalHeightAboveGround = 0.865;
//   private final double maximumHeightAboveGround = 0.925;

//   // USE THESE FOR VRC Atlas Model TASK 2 UNTIL WALKING WORKS BETTER WITH OTHERS.
//   private double minimumHeightAboveGround = 0.68;
//   private double nominalHeightAboveGround = 0.76;
//   private double maximumHeightAboveGround = 0.82;

//   // USE THESE FOR IMPROVING WALKING, BUT DONT CHECK THEM IN UNTIL IT IMPROVED WALKING THROUGH MUD.
//   private double minimumHeightAboveGround = 0.68;
//   private double nominalHeightAboveGround = 0.80;  // NOTE: used to be 0.76, jojo
//   private double maximumHeightAboveGround = 0.84;  // NOTE: used to be 0.82, jojo

   @Override
   public double minimumHeightAboveAnkle()
   {
      return minimumHeightAboveGround;
   }

   @Override
   public double nominalHeightAboveAnkle()
   {
      return nominalHeightAboveGround;
   }

   @Override
   public double maximumHeightAboveAnkle()
   {
      return maximumHeightAboveGround;
   }

   @Override
   public double defaultOffsetHeightAboveAnkle()
   {
      double defaultOffset = runningOnRealRobot ? 0.035 : 0.0;
      return defaultOffset * jointMap.getModelScale();
   }

   public void setNominalHeightAboveAnkle(double nominalHeightAboveAnkle)
   {
      this.nominalHeightAboveGround = nominalHeightAboveAnkle;
   }

   @Override
   public double getMaximumLegLengthForSingularityAvoidance()
   {
      return jointMap.getPhysicalProperties().getShinLength()  + jointMap.getPhysicalProperties().getThighLength();
   }

   @Override
   public ICPControlGains createICPControlGains()
   {
      ICPControlGains gains = new ICPControlGains();

      double kpParallel = 2.5;
      double kpOrthogonal = 1.5;
      double ki = 0.0;
      double kiBleedOff = 0.0;

      gains.setKpParallelToMotion(kpParallel);
      gains.setKpOrthogonalToMotion(kpOrthogonal);
      gains.setKi(ki);
      gains.setKiBleedOff(kiBleedOff);

//      if (runningOnRealRobot) gains.setFeedbackPartMaxRate(1.0);
      return gains;
   }

   @Override
   public YoPDGains createCoMHeightControlGains(YoVariableRegistry registry)
   {
      YoPDGains gains = new YoPDGains("CoMHeight", registry);

      double kp = 40.0;
      double zeta = runningOnRealRobot ? 0.4 : 0.8;
      double maxAcceleration = 0.5 * 9.81;
      double maxJerk = maxAcceleration / 0.05;

      gains.setKp(kp);
      gains.setZeta(zeta);
      gains.setMaximumFeedback(maxAcceleration);
      gains.setMaximumFeedbackRate(maxJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   private YoOrientationPIDGainsInterface createPelvisOrientationControlGains(YoVariableRegistry registry)
   {
      YoFootOrientationGains gains = new YoFootOrientationGains("PelvisOrientation", registry);

      double kpXY = 80.0;
      double kpZ = 40.0;
      double zeta = runningOnRealRobot ? 0.5 : 0.8;
      double maxAccel = runningOnRealRobot ? 12.0 : 36.0;
      double maxJerk = runningOnRealRobot ? 180.0 : 540.0;

      gains.setProportionalGains(kpXY, kpZ);
      gains.setDampingRatio(zeta);
      gains.setMaximumFeedback(maxAccel);
      gains.setMaximumFeedbackRate(maxJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   private YoOrientationPIDGainsInterface createHeadOrientationControlGains(YoVariableRegistry registry)
   {
      YoSymmetricSE3PIDGains gains = new YoSymmetricSE3PIDGains("HeadOrientation", registry);

      double kp = 40.0;
      double zeta = runningOnRealRobot ? 0.4 : 0.8;
      double maxAccel = runningOnRealRobot ? 6.0 : 36.0;
      double maxJerk = runningOnRealRobot ? 60.0 : 540.0;

      gains.setProportionalGain(kp);
      gains.setDampingRatio(zeta);
      gains.setMaximumFeedback(maxAccel);
      gains.setMaximumFeedbackRate(maxJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   private YoPIDGains createHeadJointspaceControlGains(YoVariableRegistry registry)
   {
      YoPIDGains gains = new YoPIDGains("HeadJointspace", registry);

      double kp = 40.0;
      double zeta = runningOnRealRobot ? 0.4 : 0.8;
      double maxAccel = runningOnRealRobot ? 6.0 : 36.0;
      double maxJerk = runningOnRealRobot ? 60.0 : 540.0;

      gains.setKp(kp);
      gains.setZeta(zeta);
      gains.setMaximumFeedback(maxAccel);
      gains.setMaximumFeedbackRate(maxJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   private YoOrientationPIDGainsInterface createChestControlGains(YoVariableRegistry registry)
   {
      YoFootOrientationGains gains = new YoFootOrientationGains("ChestOrientation", registry);

      double kpXY = 40.0;
      double kpZ = 40.0;
      double zetaXY = runningOnRealRobot ? 0.5 : 0.8;
      double zetaZ = runningOnRealRobot ? 0.22 : 0.8;
      double maxAccel = runningOnRealRobot ? 6.0 : 36.0;
      double maxJerk = runningOnRealRobot ? 60.0 : 540.0;
      double maxProportionalError = 10.0 * Math.PI/180.0;

      gains.setProportionalGains(kpXY, kpZ);
      gains.setDampingRatios(zetaXY, zetaZ);
      gains.setMaximumFeedback(maxAccel);
      gains.setMaximumFeedbackRate(maxJerk);
      gains.setMaxProportionalError(maxProportionalError);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   private YoPIDGains createSpineControlGains(YoVariableRegistry registry)
   {
      double kp = 50.0;
      double zeta = runningOnRealRobot ? 0.3 : 0.8;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      YoPIDGains spineGains = new YoPIDGains("SpineJointspace", registry);
      spineGains.setKp(kp);
      spineGains.setZeta(zeta);
      spineGains.setKi(ki);
      spineGains.setMaximumIntegralError(maxIntegralError);
      spineGains.setMaximumFeedback(maxAccel);
      spineGains.setMaximumFeedbackRate(maxJerk);
      spineGains.createDerivativeGainUpdater(true);

      return spineGains;
   }

   private YoPIDGains createArmControlGains(YoVariableRegistry registry)
   {
      YoPIDGains armGains = new YoPIDGains("ArmJointspace", registry);

      double kp = runningOnRealRobot ? 40.0 : 80.0;
      double zeta = runningOnRealRobot ? 0.3 : 0.6;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 20.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      armGains.setKp(kp);
      armGains.setZeta(zeta);
      armGains.setKi(ki);
      armGains.setMaximumIntegralError(maxIntegralError);
      armGains.setMaximumFeedback(maxAccel);
      armGains.setMaximumFeedbackRate(maxJerk);
      armGains.createDerivativeGainUpdater(true);

      return armGains;
   }

   private YoOrientationPIDGainsInterface createHandOrientationControlGains(YoVariableRegistry registry)
   {
      YoSymmetricSE3PIDGains orientationGains = new YoSymmetricSE3PIDGains("HandOrientation", registry);

      double kp = runningOnRealRobot ? 40.0 :100.0;
      // When doing position control, the damping here seems to result into some kind of spring.
      double zeta = runningOnRealRobot ? 0.0 : 1.0;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      orientationGains.setProportionalGain(kp);
      orientationGains.setDampingRatio(zeta);
      orientationGains.setIntegralGain(ki);
      orientationGains.setMaximumIntegralError(maxIntegralError);
      orientationGains.setMaximumFeedback(maxAccel);
      orientationGains.setMaximumFeedbackRate(maxJerk);
      orientationGains.createDerivativeGainUpdater(true);

      return orientationGains;
   }

   private YoPositionPIDGainsInterface createHandPositionControlGains(YoVariableRegistry registry)
   {
      YoSymmetricSE3PIDGains positionGains = new YoSymmetricSE3PIDGains("HandPosition", registry);

      double kp = runningOnRealRobot ? 40.0 :100.0;
      // When doing position control, the damping here seems to result into some kind of spring.
      double zeta = runningOnRealRobot ? 0.0 : 1.0;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      positionGains.setProportionalGain(kp);
      positionGains.setDampingRatio(zeta);
      positionGains.setIntegralGain(ki);
      positionGains.setMaximumIntegralError(maxIntegralError);
      positionGains.setMaximumFeedback(maxAccel);
      positionGains.setMaximumFeedbackRate(maxJerk);
      positionGains.createDerivativeGainUpdater(true);

      return positionGains;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, YoPIDGains> getOrCreateJointSpaceControlGains(YoVariableRegistry registry)
   {
      if (jointspaceGains != null)
         return jointspaceGains;

      jointspaceGains = new HashMap<>();

      YoPIDGains spineGains = createSpineControlGains(registry);
      for (SpineJointName name : jointMap.getSpineJointNames())
         jointspaceGains.put(jointMap.getSpineJointName(name), spineGains);

      YoPIDGains headGains = createHeadJointspaceControlGains(registry);
      for (NeckJointName name : jointMap.getNeckJointNames())
         jointspaceGains.put(jointMap.getNeckJointName(name), headGains);

      YoPIDGains armGains = createArmControlGains(registry);
      for (RobotSide robotSide : RobotSide.values)
      {
         for (ArmJointName name : jointMap.getArmJointNames())
            jointspaceGains.put(jointMap.getArmJointName(robotSide, name), armGains);
      }

      return jointspaceGains;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, YoOrientationPIDGainsInterface> getOrCreateTaskspaceOrientationControlGains(YoVariableRegistry registry)
   {
      if (taskspaceAngularGains != null)
         return taskspaceAngularGains;

      taskspaceAngularGains = new HashMap<>();

      YoOrientationPIDGainsInterface chestAngularGains = createChestControlGains(registry);
      taskspaceAngularGains.put(jointMap.getChestName(), chestAngularGains);

      YoOrientationPIDGainsInterface headAngularGains = createHeadOrientationControlGains(registry);
      taskspaceAngularGains.put(jointMap.getHeadName(), headAngularGains);

      YoOrientationPIDGainsInterface handAngularGains = createHandOrientationControlGains(registry);
      for (RobotSide robotSide : RobotSide.values)
         taskspaceAngularGains.put(jointMap.getHandName(robotSide), handAngularGains);

      YoOrientationPIDGainsInterface pelvisAngularGains = createPelvisOrientationControlGains(registry);
      taskspaceAngularGains.put(jointMap.getPelvisName(), pelvisAngularGains);

      return taskspaceAngularGains;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, YoPositionPIDGainsInterface> getOrCreateTaskspacePositionControlGains(YoVariableRegistry registry)
   {
      if (taskspaceLinearGains != null)
         return taskspaceLinearGains;

      taskspaceLinearGains = new HashMap<>();

      YoPositionPIDGainsInterface handLinearGains = createHandPositionControlGains(registry);
      for (RobotSide robotSide : RobotSide.values)
         taskspaceLinearGains.put(jointMap.getHandName(robotSide), handLinearGains);

      return taskspaceLinearGains;
   }

   /** {@inheritDoc} */
   @Override
   public RigidBodyControlMode getDefaultControlModeForRigidBody(String bodyName)
   {
      if (bodyName.equals(jointMap.getChestName()))
      {
         return RigidBodyControlMode.TASKSPACE;
      }
      else
      {
         return RigidBodyControlMode.JOINTSPACE;
      }
   }

   /** {@inheritDoc} */
   @Override
   public TObjectDoubleHashMap<String> getOrCreateJointHomeConfiguration()
   {
      if (jointHomeConfiguration != null)
         return jointHomeConfiguration;

      jointHomeConfiguration = new TObjectDoubleHashMap<String>();

      jointHomeConfiguration.put(jointMap.getSpineJointName(SpineJointName.SPINE_PITCH), 0.0);
      jointHomeConfiguration.put(jointMap.getSpineJointName(SpineJointName.SPINE_ROLL), 0.0);
      jointHomeConfiguration.put(jointMap.getSpineJointName(SpineJointName.SPINE_YAW), 0.0);

      jointHomeConfiguration.put(jointMap.getNeckJointName(NeckJointName.PROXIMAL_NECK_PITCH), 0.0);

      for (RobotSide robotSide : RobotSide.values)
      {
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_YAW), robotSide.negateIfRightSide(0.785398));
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_ROLL), robotSide.negateIfRightSide(-0.1));
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_PITCH), 3.00);
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_ROLL), robotSide.negateIfRightSide(1.8));
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.FIRST_WRIST_PITCH), -0.30);
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.WRIST_ROLL), robotSide.negateIfRightSide(0.70));
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.SECOND_WRIST_PITCH), 0.15);
      }

      return jointHomeConfiguration;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, Pose3D> getOrCreateBodyHomeConfiguration()
   {
      if (bodyHomeConfiguration != null)
         return bodyHomeConfiguration;

      bodyHomeConfiguration = new HashMap<String, Pose3D>();

      Pose3D homeChestPoseInPelvisZUpFrame = new Pose3D();
      bodyHomeConfiguration.put(jointMap.getChestName(), homeChestPoseInPelvisZUpFrame);

      return bodyHomeConfiguration;
   }

   /** {@inheritDoc} */
   @Override
   public List<String> getOrCreatePositionControlledJoints()
   {
      if (positionControlledJoints != null)
         return positionControlledJoints;

      positionControlledJoints = new ArrayList<String>();

      if (runningOnRealRobot)
      {
         for (NeckJointName name : jointMap.getNeckJointNames())
            positionControlledJoints.add(jointMap.getNeckJointName(name));

         for (RobotSide robotSide : RobotSide.values)
         {
            for (ArmJointName name : jointMap.getArmJointNames())
               positionControlledJoints.add(jointMap.getArmJointName(robotSide, name));
         }
      }

      return positionControlledJoints;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, JointAccelerationIntegrationSettings> getOrCreateIntegrationSettings()
   {
      if (integrationSettings != null)
         return integrationSettings;

      integrationSettings = new HashMap<String, JointAccelerationIntegrationSettings>();

      JointAccelerationIntegrationSettings neckJointSettings = new JointAccelerationIntegrationSettings();
      neckJointSettings.setAlphaPosition(0.9996);
      neckJointSettings.setAlphaVelocity(0.95);
      neckJointSettings.setMaxPositionError(0.2);
      neckJointSettings.setMaxVelocity(2.0);

      for (NeckJointName name : jointMap.getNeckJointNames())
         integrationSettings.put(jointMap.getNeckJointName(name), neckJointSettings);

      JointAccelerationIntegrationSettings shoulderJointSettings = new JointAccelerationIntegrationSettings();
      shoulderJointSettings.setAlphaPosition(0.9998);
      shoulderJointSettings.setAlphaVelocity(0.95);
      shoulderJointSettings.setMaxPositionError(0.2);
      shoulderJointSettings.setMaxVelocity(2.0);

      JointAccelerationIntegrationSettings elbowJointSettings = new JointAccelerationIntegrationSettings();
      elbowJointSettings.setAlphaPosition(0.9996);
      elbowJointSettings.setAlphaVelocity(0.95);
      elbowJointSettings.setMaxPositionError(0.2);
      elbowJointSettings.setMaxVelocity(2.0);

      JointAccelerationIntegrationSettings wristJointSettings = new JointAccelerationIntegrationSettings();
      wristJointSettings.setAlphaPosition(0.9999);
      wristJointSettings.setAlphaVelocity(0.95);
      wristJointSettings.setMaxPositionError(0.2);
      wristJointSettings.setMaxVelocity(2.0);

      for (RobotSide robotSide : RobotSide.values)
      {
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_YAW), shoulderJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_ROLL), shoulderJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_PITCH), elbowJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_ROLL), elbowJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.FIRST_WRIST_PITCH), wristJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.WRIST_ROLL), wristJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.SECOND_WRIST_PITCH), wristJointSettings);
      }

      return integrationSettings;
   }

   @Override
   public YoSE3PIDGainsInterface createSwingFootControlGains(YoVariableRegistry registry)
   {
      YoFootSE3Gains gains = new YoFootSE3Gains("SwingFoot", registry);

      double kpXY = 150.0;
      double kpZ = 200.0;
      double zetaXYZ = runningOnRealRobot ? 0.7 : 0.7;

      double kpXYOrientation = 200.0;
      double kpZOrientation = 200.0;
      double zetaOrientation = 0.7;

      // Reduce maxPositionAcceleration from 30 to 6 to prevent too high acceleration when hitting joint limits.
      double maxPositionAcceleration = runningOnRealRobot ? 20.0 : Double.POSITIVE_INFINITY;
//      double maxPositionAcceleration = runningOnRealRobot ? 30.0 : Double.POSITIVE_INFINITY;
      double maxPositionJerk = runningOnRealRobot ? 300.0 : Double.POSITIVE_INFINITY;
      double maxOrientationAcceleration = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;
      double maxOrientationJerk = runningOnRealRobot ? 1500.0 : Double.POSITIVE_INFINITY;

      gains.setPositionProportionalGains(kpXY, kpZ);
      gains.setPositionDampingRatio(zetaXYZ);
      gains.setPositionMaxFeedbackAndFeedbackRate(maxPositionAcceleration, maxPositionJerk);
      gains.setOrientationProportionalGains(kpXYOrientation, kpZOrientation);
      gains.setOrientationDampingRatio(zetaOrientation);
      gains.setOrientationMaxFeedbackAndFeedbackRate(maxOrientationAcceleration, maxOrientationJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   @Override
   public YoSE3PIDGainsInterface createHoldPositionFootControlGains(YoVariableRegistry registry)
   {
      YoFootSE3Gains gains = new YoFootSE3Gains("HoldFoot", registry);

      double kpXY = 100.0;
      double kpZ = 0.0;
      double zetaXYZ = runningOnRealRobot ? 0.2 : 1.0;
      double kpXYOrientation = runningOnRealRobot ? 100.0 : 175.0;
      double kpZOrientation = runningOnRealRobot ? 100.0 : 200.0;
      double zetaOrientation = runningOnRealRobot ? 0.2 : 1.0;
      // Reduce maxPositionAcceleration from 10 to 6 to prevent too high acceleration when hitting joint limits.
      double maxLinearAcceleration = runningOnRealRobot ? 6.0 : Double.POSITIVE_INFINITY;
//      double maxLinearAcceleration = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxLinearJerk = runningOnRealRobot ? 150.0 : Double.POSITIVE_INFINITY;
      double maxAngularAcceleration = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;
      double maxAngularJerk = runningOnRealRobot ? 1500.0 : Double.POSITIVE_INFINITY;

      gains.setPositionProportionalGains(kpXY, kpZ);
      gains.setPositionDampingRatio(zetaXYZ);
      gains.setPositionMaxFeedbackAndFeedbackRate(maxLinearAcceleration, maxLinearJerk);
      gains.setOrientationProportionalGains(kpXYOrientation, kpZOrientation);
      gains.setOrientationDampingRatio(zetaOrientation);
      gains.setOrientationMaxFeedbackAndFeedbackRate(maxAngularAcceleration, maxAngularJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   @Override
   public YoSE3PIDGainsInterface createToeOffFootControlGains(YoVariableRegistry registry)
   {
      YoFootSE3Gains gains = new YoFootSE3Gains("ToeOffFoot", registry);

      double kpXY = 100.0;
      double kpZ = 100.0;
      double zetaXYZ = runningOnRealRobot ? 0.4 : 0.4;
      double kpXYOrientation = runningOnRealRobot ? 200.0 : 200.0;
      double kpZOrientation = runningOnRealRobot ? 200.0 : 200.0;
      double zetaOrientation = runningOnRealRobot ? 0.4 : 0.4;
      // Reduce maxPositionAcceleration from 10 to 6 to prevent too high acceleration when hitting joint limits.
      double maxLinearAcceleration = runningOnRealRobot ? 6.0 : Double.POSITIVE_INFINITY;
//      double maxLinearAcceleration = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxLinearJerk = runningOnRealRobot ? 150.0 : Double.POSITIVE_INFINITY;
      double maxAngularAcceleration = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;
      double maxAngularJerk = runningOnRealRobot ? 1500.0 : Double.POSITIVE_INFINITY;

      gains.setPositionProportionalGains(kpXY, kpZ);
      gains.setPositionDampingRatio(zetaXYZ);
      gains.setPositionMaxFeedbackAndFeedbackRate(maxLinearAcceleration, maxLinearJerk);
      gains.setOrientationProportionalGains(kpXYOrientation, kpZOrientation);
      gains.setOrientationDampingRatio(zetaOrientation);
      gains.setOrientationMaxFeedbackAndFeedbackRate(maxAngularAcceleration, maxAngularJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   public double getSwingMaxHeightForPushRecoveryTrajectory()
   {
      return 0.15 * jointMap.getModelScale();
   }

   @Override
   public boolean doPrepareManipulationForLocomotion()
   {
      //this allows me to walk with hand poses locked in a reference frame
      return false;
   }

   @Override
   public double getDefaultTransferTime()
   {
      return (runningOnRealRobot ? 0.8 : 0.25); //Math.sqrt(jointMap.getModelScale()) *
   }

   @Override
   public double getDefaultSwingTime()
   {
      return (runningOnRealRobot ? 1.2 : 0.60); //Math.sqrt(jointMap.getModelScale()) *
   }

   @Override
   public double getContactThresholdForce()
   {
      switch (target)
      {
         case REAL_ROBOT :
            return massScale * 80.0;

         case GAZEBO :
            return massScale * 50.0;

         case SCS:
            return massScale * 5.0;

         default :
            throw new RuntimeException();
      }
   }

   @Override
   public double getSecondContactThresholdForceIgnoringCoP()
   {
      return massScale * 220.0;
   }

   @Override
   public double getCoPThresholdFraction()
   {
      return 0.02;
   }

   @Override
   public String[] getJointsToIgnoreInController()
   {
      return new String[0];
   }

   @Override
   public MomentumOptimizationSettings getMomentumOptimizationSettings()
   {
      return momentumOptimizationSettings;
   }

   @Override
   public ICPAngularMomentumModifierParameters getICPAngularMomentumModifierParameters()
   {
      return angularMomentumModifierParameters;
   }

   @Override
   public double getContactThresholdHeight()
   {
      return jointMap.getModelScale() * 0.05;
   }

   @Override
   public FootSwitchType getFootSwitchType()
   {
      return FootSwitchType.WrenchBased;
   }

   @Override
   public double getMaxICPErrorBeforeSingleSupportX()
   {
      return 0.035 * jointMap.getModelScale();
   }

   @Override
   public double getMaxICPErrorBeforeSingleSupportY()
   {
      return 0.015 * jointMap.getModelScale();
   }

   @Override
   public boolean finishSingleSupportWhenICPPlannerIsDone()
   {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public double getHighCoPDampingDurationToPreventFootShakies()
   {
      return -1.0; // 0.5;
   }

   /** {@inheritDoc} */
   @Override
   public double getCoPErrorThresholdForHighCoPDamping()
   {
      return Double.POSITIVE_INFINITY; //0.075;
   }

//   private ExplorationParameters explorationParameters = null;
   /** {@inheritDoc} */
   @Override
   public ExplorationParameters getOrCreateExplorationParameters(YoVariableRegistry registry)
   {
      return null;

      // GW: Disabled this for now since it creates lots of debugging yoVariables.
//      if (explorationParameters == null)
//         explorationParameters = new ExplorationParameters(registry);
//      return explorationParameters;
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxAllowedDistanceCMPSupport()
   {
      return 0.06 * jointMap.getModelScale();
   }

   /** {@inheritDoc} */
   @Override
   public boolean useCenterOfMassVelocityFromEstimator()
   {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public String[] getJointsWithRestrictiveLimits(JointLimitParameters jointLimitParametersToPack)
   {
      jointLimitParametersToPack.setMaxAbsJointVelocity(9.0);
      jointLimitParametersToPack.setJointLimitDistanceForMaxVelocity(30.0 * Math.PI/180.0);
      jointLimitParametersToPack.setJointLimitFilterBreakFrequency(15.0);
      jointLimitParametersToPack.setVelocityControlGain(30.0);

      String bkxName = jointMap.getSpineJointName(SpineJointName.SPINE_ROLL);
      String bkyName = jointMap.getSpineJointName(SpineJointName.SPINE_PITCH);
      String[] joints = {bkxName, bkyName};
      return joints;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useOptimizationBasedICPController()
   {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean controlToeDuringSwing()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public JointPrivilegedConfigurationParameters getJointPrivilegedConfigurationParameters()
   {
      return jointPrivilegedConfigurationParameters;
   }

   /** {@inheritDoc} */
   @Override
   public LegConfigurationParameters getLegConfigurationParameters()
   {
      return legConfigurationParameters;
   }

   /** {@inheritDoc} */
   @Override
   public boolean controlHeightWithMomentum()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public ToeOffParameters getToeOffParameters()
   {
      return toeOffParameters;
   }

   /** {@inheritDoc} */
   @Override
   public SwingTrajectoryParameters getSwingTrajectoryParameters()
   {
      return swingTrajectoryParameters;
   }

   /** {@inheritDoc} */
   @Override
   public ICPOptimizationParameters getICPOptimizationParameters()
   {
      return icpOptimizationParameters;
   }

   /** {@inheritDoc} */
   @Override
   public SteppingParameters getSteppingParameters()
   {
      return steppingParameters;
   }
}
