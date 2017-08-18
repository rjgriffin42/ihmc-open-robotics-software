package us.ihmc.valkyrie.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.commonWalkingControlModules.configurations.ICPAngularMomentumModifierParameters;
import us.ihmc.commonWalkingControlModules.configurations.LegConfigurationParameters;
import us.ihmc.commonWalkingControlModules.configurations.SteppingParameters;
import us.ihmc.commonWalkingControlModules.configurations.SwingTrajectoryParameters;
import us.ihmc.commonWalkingControlModules.configurations.ToeOffParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.JointAccelerationIntegrationSettings;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ICPControlGains;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.robotics.controllers.PDGains;
import us.ihmc.robotics.controllers.PIDGains;
import us.ihmc.robotics.controllers.pidGains.GainCoupling;
import us.ihmc.robotics.controllers.pidGains.PID3DGains;
import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
import us.ihmc.robotics.controllers.pidGains.YoPIDSE3Gains;
import us.ihmc.robotics.controllers.pidGains.implementations.DefaultPID3DGains;
import us.ihmc.robotics.controllers.pidGains.implementations.DefaultPIDSE3Gains;
import us.ihmc.robotics.controllers.pidGains.implementations.DefaultYoPIDSE3Gains;
import us.ihmc.robotics.controllers.pidGains.implementations.SymmetricYoPIDSE3Gains;
import us.ihmc.robotics.partNames.ArmJointName;
import us.ihmc.robotics.partNames.NeckJointName;
import us.ihmc.robotics.partNames.SpineJointName;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.stateEstimation.FootSwitchType;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class ValkyrieWalkingControllerParameters extends WalkingControllerParameters
{
   private final RobotTarget target;

   private final SideDependentList<RigidBodyTransform> handPosesWithRespectToChestFrame = new SideDependentList<RigidBodyTransform>();

   private final ValkyrieJointMap jointMap;

   private Map<String, YoPID3DGains> taskspaceLinearGains = null;
   private TObjectDoubleHashMap<String> jointHomeConfiguration = null;
   private Map<String, Pose3D> bodyHomeConfiguration = null;
   private ArrayList<String> positionControlledJoints = null;
   private Map<String, JointAccelerationIntegrationSettings> integrationSettings = null;

   private final LegConfigurationParameters legConfigurationParameters;
   private final ToeOffParameters toeOffParameters;
   private final SwingTrajectoryParameters swingTrajectoryParameters;
   private final ValkyrieSteppingParameters steppingParameters;

   public ValkyrieWalkingControllerParameters(ValkyrieJointMap jointMap)
   {
      this(jointMap, RobotTarget.SCS);
   }

   public ValkyrieWalkingControllerParameters(ValkyrieJointMap jointMap, RobotTarget target)
   {
      this.jointMap = jointMap;
      this.target = target;

      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;
      legConfigurationParameters = new ValkyrieLegConfigurationParameters(runningOnRealRobot);
      toeOffParameters = new ValkyrieToeOffParameters();
      swingTrajectoryParameters = new ValkyrieSwingTrajectoryParameters(target);
      steppingParameters = new ValkyrieSteppingParameters(target);

      // Generated using ValkyrieFullRobotModelVisualizer
      RigidBodyTransform leftHandLocation = new RigidBodyTransform(new double[] { 0.8772111323383822, -0.47056204413925823, 0.09524700476706424,
            0.11738015536007923, 1.5892231999088989E-4, 0.1986725292086453, 0.980065916600275, 0.3166524835978034, -0.48010478444326166, -0.8597095955922112,
            0.1743525371234003, -0.13686311108389013, 0.0, 0.0, 0.0, 1.0 });

      RigidBodyTransform rightHandLocation = new RigidBodyTransform(new double[] { 0.8772107606751612, -0.47056267784177724, -0.09524729695945025,
            0.11738015535642271, -1.5509783447718197E-4, -0.19866600827375044, 0.9800672390715021, -0.3166524835989298, -0.48010546476828164,
            -0.8597107556492186, -0.17434494349043353, -0.13686311108617974, 0.0, 0.0, 0.0, 1.0 });

      handPosesWithRespectToChestFrame.put(RobotSide.LEFT, leftHandLocation);
      handPosesWithRespectToChestFrame.put(RobotSide.RIGHT, rightHandLocation);
   }

   @Override
   public double getOmega0()
   {
      // TODO probably need to be tuned.
      return target == RobotTarget.REAL_ROBOT ? 3.0 : 3.3; // 3.3 seems more appropriate.
   }

   @Override
   public boolean allowDisturbanceRecoveryBySpeedingUpSwing()
   {
      return target == RobotTarget.REAL_ROBOT;
   }

   @Override
   public boolean allowAutomaticManipulationAbort()
   {
      return false;
   }

   @Override
   public double getICPErrorThresholdToSpeedUpSwing()
   {
      return 0.05;
   }

   @Override
   public double getMinimumSwingTimeForDisturbanceRecovery()
   {
      return 0.70;
   }

   // USE THESE FOR Real Robot and sims when controlling pelvis height instead of CoM.
   private final double minimumHeightAboveGround = 0.595 + 0.23;
   private double nominalHeightAboveGround = 0.675 + 0.23 - 0.01;
   private final double maximumHeightAboveGround = 0.735 + 0.23;

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
      return 0.0;
   }

   @Override
   public double getMaximumLegLengthForSingularityAvoidance()
   {
      return ValkyriePhysicalProperties.thighLength + ValkyriePhysicalProperties.shinLength;
   }

   @Override
   public boolean controlToeDuringSwing()
   {
      return true;
   }

   @Override
   public ICPControlGains createICPControlGains()
   {
      ICPControlGains gains = new ICPControlGains();

      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpOrthogonal = runningOnRealRobot ? 1.9 : 1.5;
      double kpParallel = runningOnRealRobot ? 2.0 : 2.5;
      double ki = runningOnRealRobot ? 0.0 : 0.0;
      double kiBleedOff = 0.9;

      gains.setKpParallelToMotion(kpParallel);
      gains.setKpOrthogonalToMotion(kpOrthogonal);
      gains.setKi(ki);
      gains.setKiBleedOff(kiBleedOff);

      if (target == RobotTarget.REAL_ROBOT)
         gains.setFeedbackPartMaxRate(1.0);

      return gains;
   }

   @Override
   public PDGains getCoMHeightControlGains()
   {
      PDGains gains = new PDGains("_CoMHeight");

      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kp = runningOnRealRobot ? 40.0 : 50.0;
      double zeta = runningOnRealRobot ? 0.4 : 1.0;
      double maxAcceleration = 0.5 * 9.81;
      double maxJerk = maxAcceleration / 0.05;

      gains.setKp(kp);
      gains.setZeta(zeta);
      gains.setMaximumFeedback(maxAcceleration);
      gains.setMaximumFeedbackRate(maxJerk);

      return gains;
   }

   private YoPID3DGains createHandPositionControlGains(YoVariableRegistry registry)
   {
      SymmetricYoPIDSE3Gains positionGains = new SymmetricYoPIDSE3Gains("HandPosition", registry);
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kp = 100.0;
      double zeta = runningOnRealRobot ? 0.6 : 1.0;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      positionGains.setProportionalGains(kp);
      positionGains.setDampingRatios(zeta);
      positionGains.setIntegralGains(ki, maxIntegralError);
      positionGains.setMaxFeedbackAndFeedbackRate(maxAccel, maxJerk);

      return positionGains;
   }

   /** {@inheritDoc} */
   @Override
   public List<ImmutablePair<PIDGains, List<String>>> getJointSpaceControlGains()
   {
      List<String> spineNames = new ArrayList<>();
      List<String> neckNames = new ArrayList<>();
      List<String> armNames = new ArrayList<>();

      Arrays.stream(jointMap.getSpineJointNames()).forEach(n -> spineNames.add(jointMap.getSpineJointName(n)));
      Arrays.stream(jointMap.getNeckJointNames()).forEach(n -> neckNames.add(jointMap.getNeckJointName(n)));
      for (RobotSide side : RobotSide.values)
      {
         Arrays.stream(jointMap.getArmJointNames()).forEach(n -> armNames.add(jointMap.getArmJointName(side, n)));
      }

      PIDGains spineGains = createSpineControlGains();
      PIDGains neckGains = createNeckControlGains();
      PIDGains armGains = createArmControlGains();

      List<ImmutablePair<PIDGains, List<String>>> jointspaceGains = new ArrayList<>();
      jointspaceGains.add(new ImmutablePair<PIDGains, List<String>>(spineGains, spineNames));
      jointspaceGains.add(new ImmutablePair<PIDGains, List<String>>(neckGains, neckNames));
      jointspaceGains.add(new ImmutablePair<PIDGains, List<String>>(armGains, armNames));

      return jointspaceGains;
   }

   private PIDGains createSpineControlGains()
   {
      PIDGains spineGains = new PIDGains("_SpineJointGains");
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kp = 50.0;
      double zeta = runningOnRealRobot ? 0.3 : 0.8;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 20.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      spineGains.setKp(kp);
      spineGains.setZeta(zeta);
      spineGains.setKi(ki);
      spineGains.setMaxIntegralError(maxIntegralError);
      spineGains.setMaximumFeedback(maxAccel);
      spineGains.setMaximumFeedbackRate(maxJerk);

      return spineGains;
   }

   private PIDGains createNeckControlGains()
   {
      PIDGains gains = new PIDGains("_NeckJointGains");
      boolean realRobot = target == RobotTarget.REAL_ROBOT;

      double kp = 40.0;
      double zeta = realRobot ? 0.4 : 0.8;
      double maxAccel = 18.0;
      double maxJerk = 270.0;

      gains.setKp(kp);
      gains.setZeta(zeta);
      gains.setMaximumFeedback(maxAccel);
      gains.setMaximumFeedbackRate(maxJerk);

      return gains;
   }

   private PIDGains createArmControlGains()
   {
      PIDGains armGains = new PIDGains("_ArmJointGains");
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kp = runningOnRealRobot ? 200.0 : 120.0; // 200.0
      double zeta = runningOnRealRobot ? 1.0 : 0.7;
      double ki = runningOnRealRobot ? 0.0 : 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 50.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 750.0 : Double.POSITIVE_INFINITY;

      armGains.setKp(kp);
      armGains.setZeta(zeta);
      armGains.setKi(ki);
      armGains.setMaxIntegralError(maxIntegralError);
      armGains.setMaximumFeedback(maxAccel);
      armGains.setMaximumFeedbackRate(maxJerk);

      return armGains;
   }

   /** {@inheritDoc} */
   @Override
   public List<ImmutablePair<String, PID3DGains>> getTaskspaceOrientationControlGains()
   {
      List<ImmutablePair<String, PID3DGains>> taskspaceAngularGains = new ArrayList<>();

      PID3DGains chestAngularGains = createChestOrientationControlGains();
      taskspaceAngularGains.add(new ImmutablePair<>(jointMap.getChestName(), chestAngularGains));

      PID3DGains headAngularGains = createHeadOrientationControlGains();
      taskspaceAngularGains.add(new ImmutablePair<>(jointMap.getHeadName(), headAngularGains));

      PID3DGains handAngularGains = createHandOrientationControlGains();
      for (RobotSide robotSide : RobotSide.values)
         taskspaceAngularGains.add(new ImmutablePair<>(jointMap.getHandName(robotSide), handAngularGains));

      PID3DGains pelvisAngularGains = createPelvisOrientationControlGains();
      taskspaceAngularGains.add(new ImmutablePair<>(jointMap.getPelvisName(), pelvisAngularGains));

      return taskspaceAngularGains;
   }

   private PID3DGains createPelvisOrientationControlGains()
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpXY = runningOnRealRobot ? 100.0 : 100.0; // 160.0
      double kpZ = runningOnRealRobot ? 80.0 : 100.0; // 120.0
      double zetaXY = runningOnRealRobot ? 0.9 : 0.8; // 0.7
      double zetaZ = runningOnRealRobot ? 1.00 : 0.8; // 0.7
      double maxAccel = runningOnRealRobot ? 18.0 : 18.0;
      double maxJerk = runningOnRealRobot ? 270.0 : 270.0;

      DefaultPID3DGains gains = new DefaultPID3DGains(GainCoupling.XY, false);
      gains.setProportionalGains(kpXY, kpXY, kpZ);
      gains.setDampingRatios(zetaXY, zetaXY, zetaZ);
      gains.setMaxFeedbackAndFeedbackRate(maxAccel, maxJerk);

      return gains;
   }

   private PID3DGains createHeadOrientationControlGains()
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpX = 5.0;
      double kpYZ = 20.0;//40.0;
      double zeta = runningOnRealRobot ? 0.4 : 0.8;
      double maxAccel = 18.0;
      double maxJerk = 270.0;

      DefaultPID3DGains gains = new DefaultPID3DGains(GainCoupling.YZ, false);
      gains.setProportionalGains(kpX, kpYZ, kpYZ);
      gains.setDampingRatios(zeta, zeta, zeta);
      gains.setMaxFeedbackAndFeedbackRate(maxAccel, maxJerk);

      return gains;
   }

   private PID3DGains createChestOrientationControlGains()
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpXY = runningOnRealRobot ? 80.0 : 100.0;
      double kpZ = runningOnRealRobot ? 60.0 : 100.0;
      double zetaXY = runningOnRealRobot ? 0.8 : 0.8;
      double zetaZ = runningOnRealRobot ? 0.8 : 0.8;
      double maxAccel = runningOnRealRobot ? 12.0 : 18.0;
      double maxJerk = runningOnRealRobot ? 180.0 : 270.0;

      DefaultPID3DGains gains = new DefaultPID3DGains(GainCoupling.XY, false);
      gains.setProportionalGains(kpXY, kpXY, kpZ);
      gains.setDampingRatios(zetaXY, zetaXY, zetaZ);
      gains.setMaxFeedbackAndFeedbackRate(maxAccel, maxJerk);

      return gains;
   }

   private PID3DGains createHandOrientationControlGains()
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kp = 100.0;
      double zeta = runningOnRealRobot ? 0.6 : 1.0;
      double maxAccel = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxJerk = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;

      DefaultPID3DGains gains = new DefaultPID3DGains(GainCoupling.XYZ, false);
      gains.setProportionalGains(kp);
      gains.setDampingRatios(zeta);
      gains.setMaxFeedbackAndFeedbackRate(maxAccel, maxJerk);

      return gains;
   }

   /** {@inheritDoc} */
   @Override
   public Map<String, YoPID3DGains> getOrCreateTaskspacePositionControlGains(YoVariableRegistry registry)
   {
      if (taskspaceLinearGains != null)
         return taskspaceLinearGains;

      taskspaceLinearGains = new HashMap<>();

      YoPID3DGains handLinearGains = createHandPositionControlGains(registry);
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
      jointHomeConfiguration.put(jointMap.getNeckJointName(NeckJointName.DISTAL_NECK_YAW), 0.0);
      jointHomeConfiguration.put(jointMap.getNeckJointName(NeckJointName.DISTAL_NECK_PITCH), 0.0);

      for (RobotSide robotSide : RobotSide.values)
      {
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_ROLL), robotSide.negateIfRightSide(-1.2));
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_PITCH), -0.2);
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.SHOULDER_YAW), 0.7);
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_PITCH), robotSide.negateIfRightSide(-1.5));
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_ROLL), 1.3);
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.FIRST_WRIST_PITCH), 0.0);
         jointHomeConfiguration.put(jointMap.getArmJointName(robotSide, ArmJointName.WRIST_ROLL), 0.0);
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

      if (target == RobotTarget.REAL_ROBOT || target == RobotTarget.GAZEBO)
      {
         for (NeckJointName name : jointMap.getNeckJointNames())
            positionControlledJoints.add(jointMap.getNeckJointName(name));

         for (RobotSide robotSide : RobotSide.values)
         {
            positionControlledJoints.add(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_ROLL));
            positionControlledJoints.add(jointMap.getArmJointName(robotSide, ArmJointName.FIRST_WRIST_PITCH));
            positionControlledJoints.add(jointMap.getArmJointName(robotSide, ArmJointName.WRIST_ROLL));
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

      JointAccelerationIntegrationSettings elbowJointSettings = new JointAccelerationIntegrationSettings();
      elbowJointSettings.setAlphaPosition(0.999);
      elbowJointSettings.setAlphaVelocity(0.83);
      elbowJointSettings.setMaxPositionError(0.2);
      elbowJointSettings.setMaxVelocity(2.0);

      JointAccelerationIntegrationSettings wristJointSettings = new JointAccelerationIntegrationSettings();
      wristJointSettings.setAlphaPosition(0.999);
      wristJointSettings.setAlphaVelocity(0.83);
      wristJointSettings.setMaxPositionError(0.2);
      wristJointSettings.setMaxVelocity(2.0);

      for (RobotSide robotSide : RobotSide.values)
      {
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.ELBOW_ROLL), elbowJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.FIRST_WRIST_PITCH), wristJointSettings);
         integrationSettings.put(jointMap.getArmJointName(robotSide, ArmJointName.WRIST_ROLL), wristJointSettings);
      }

      return integrationSettings;
   }

   @Override
   public YoPIDSE3Gains createSwingFootControlGains(YoVariableRegistry registry)
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpX = 150.0; // Might cause some shakies.
      double kpY = 100.0; // 150.0
      double kpZ = runningOnRealRobot ? 200.0 : 200.0;
      double zetaXZ = runningOnRealRobot ? 0.8 : 0.7; // Might cause some shakies.
      double zetaY = runningOnRealRobot ? 0.5 : 0.7;
      double kpXYOrientation = runningOnRealRobot ? 200.0 : 300.0;
      double kpZOrientation = runningOnRealRobot ? 150.0 : 200.0; // 160
      double zetaOrientationXY = runningOnRealRobot ? 0.7 : 0.7;
      double zetaOrientationZ = runningOnRealRobot ? 0.5 : 0.7;
      double maxLinearAcceleration = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxLinearJerk = runningOnRealRobot ? 150.0 : Double.POSITIVE_INFINITY;
      double maxAngularAcceleration = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;
      double maxAngularJerk = runningOnRealRobot ? 1500.0 : Double.POSITIVE_INFINITY;

      DefaultPIDSE3Gains gains = new DefaultPIDSE3Gains(GainCoupling.NONE, false);
      gains.setPositionProportionalGains(kpX, kpY, kpZ);
      gains.setPositionDampingRatios(zetaXZ, zetaY, zetaXZ);
      gains.setPositionMaxFeedbackAndFeedbackRate(maxLinearAcceleration, maxLinearJerk);
      gains.setOrientationProportionalGains(kpXYOrientation, kpXYOrientation, kpZOrientation);
      gains.setOrientationDampingRatios(zetaOrientationXY, zetaOrientationXY, zetaOrientationZ);
      gains.setOrientationMaxFeedbackAndFeedbackRate(maxAngularAcceleration, maxAngularJerk);

      return new DefaultYoPIDSE3Gains("SwingFoot", gains, registry);
   }

   @Override
   public YoPIDSE3Gains createHoldPositionFootControlGains(YoVariableRegistry registry)
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpXY = 0.0; //40.0;
      double kpZ = 0.0;
      double zetaXYZ = runningOnRealRobot ? 0.7 : 1.0;
      double kpXYOrientation = runningOnRealRobot ? 0.0 : 200.0; // 40.0
      double kpZOrientation = runningOnRealRobot ? 100.0 : 200.0; // 120.0
      double zetaOrientation = runningOnRealRobot ? 0.7 : 1.0;
      double maxLinearAcceleration = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxLinearJerk = runningOnRealRobot ? 150.0 : Double.POSITIVE_INFINITY;
      double maxAngularAcceleration = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;
      double maxAngularJerk = runningOnRealRobot ? 1500.0 : Double.POSITIVE_INFINITY;

      DefaultPIDSE3Gains gains = new DefaultPIDSE3Gains(GainCoupling.XY, false);
      gains.setPositionProportionalGains(kpXY, kpXY, kpZ);
      gains.setPositionDampingRatios(zetaXYZ);
      gains.setPositionMaxFeedbackAndFeedbackRate(maxLinearAcceleration, maxLinearJerk);
      gains.setOrientationProportionalGains(kpXYOrientation, kpXYOrientation, kpZOrientation);
      gains.setOrientationDampingRatios(zetaOrientation);
      gains.setOrientationMaxFeedbackAndFeedbackRate(maxAngularAcceleration, maxAngularJerk);

      return new DefaultYoPIDSE3Gains("HoldFoot", gains, registry);
   }

   @Override
   public YoPIDSE3Gains createToeOffFootControlGains(YoVariableRegistry registry)
   {
      boolean runningOnRealRobot = target == RobotTarget.REAL_ROBOT;

      double kpXY = 40.0;
      double kpZ = 0.0;
      double zetaXYZ = runningOnRealRobot ? 0.7 : 0.4;
      double kpXYOrientation = runningOnRealRobot ? 40.0 : 200.0;
      double kpZOrientation = runningOnRealRobot ? 40.0 : 200.0;
      double zetaOrientation = runningOnRealRobot ? 0.4 : 0.4;
      double maxLinearAcceleration = runningOnRealRobot ? 10.0 : Double.POSITIVE_INFINITY;
      double maxLinearJerk = runningOnRealRobot ? 150.0 : Double.POSITIVE_INFINITY;
      double maxAngularAcceleration = runningOnRealRobot ? 100.0 : Double.POSITIVE_INFINITY;
      double maxAngularJerk = runningOnRealRobot ? 1500.0 : Double.POSITIVE_INFINITY;

      DefaultPIDSE3Gains gains = new DefaultPIDSE3Gains(GainCoupling.XY, false);
      gains.setPositionProportionalGains(kpXY, kpXY, kpZ);
      gains.setPositionDampingRatios(zetaXYZ);
      gains.setPositionMaxFeedbackAndFeedbackRate(maxLinearAcceleration, maxLinearJerk);
      gains.setOrientationProportionalGains(kpXYOrientation, kpXYOrientation, kpZOrientation);
      gains.setOrientationDampingRatios(zetaOrientation);
      gains.setOrientationMaxFeedbackAndFeedbackRate(maxAngularAcceleration, maxAngularJerk);

      return new DefaultYoPIDSE3Gains("ToeOffFoot", gains, registry);
   }

   @Override
   public boolean doPrepareManipulationForLocomotion()
   {
      return true;
   }

   @Override
   public double getDefaultTransferTime()
   {
      return target == RobotTarget.REAL_ROBOT ? 1.00 : 0.25;
   }

   @Override
   public double getDefaultSwingTime()
   {
      return target == RobotTarget.REAL_ROBOT ? 1.20 : 0.60;
   }

   /** @inheritDoc */
   @Override
   public double getDefaultInitialTransferTime()
   {
      return (target == RobotTarget.REAL_ROBOT) ? 2.0 : 1.0;
   }

   @Override
   public double getContactThresholdForce()
   {
      switch(target)
      {
      case REAL_ROBOT:
      case GAZEBO:
         return 50.0;
      default:
         return 5.0;
      }
   }

   @Override
   public double getSecondContactThresholdForceIgnoringCoP()
   {
      return 75.0;
   }

   @Override
   public double getCoPThresholdFraction()
   {
      return 0.01;
   }

   @Override
   public String[] getJointsToIgnoreInController()
   {
      List<String> jointToIgnoreList = new ArrayList<>();

      for (RobotSide robotSide : RobotSide.values)
      {
         String[] forcedSideJointNames = ValkyrieOrderedJointMap.forcedSideDependentJointNames.get(robotSide);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftIndexFingerPitch1]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftIndexFingerPitch2]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftIndexFingerPitch3]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftMiddleFingerPitch1]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftMiddleFingerPitch2]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftMiddleFingerPitch3]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftPinkyPitch1]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftPinkyPitch2]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftPinkyPitch3]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftThumbRoll]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftThumbPitch1]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftThumbPitch2]);
         jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftThumbPitch3]);

         if (target == RobotTarget.REAL_ROBOT)
         {
//            jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.UpperNeckPitch]);
//            jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LowerNeckPitch]);
//            jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.NeckYaw]);
//            jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftForearmYaw]);
//            jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftWristRoll]);
//            jointToIgnoreList.add(forcedSideJointNames[ValkyrieOrderedJointMap.LeftWristPitch]);
         }
      }

      return jointToIgnoreList.toArray(new String[0]);
   }

   @Override
   public MomentumOptimizationSettings getMomentumOptimizationSettings()
   {
      return new ValkyrieMomentumOptimizationSettings(jointMap);
   }

   @Override
   public ICPAngularMomentumModifierParameters getICPAngularMomentumModifierParameters()
   {
      return new ICPAngularMomentumModifierParameters();
   }

   @Override
   public double getContactThresholdHeight()
   {
      return 0.05;
   }

   @Override
   public FootSwitchType getFootSwitchType()
   {
      return FootSwitchType.WrenchBased;
      //      return runningOnRealRobot ? FootSwitchType.WrenchAndContactSensorFused : FootSwitchType.WrenchBased;
   }

   @Override
   public double getMaxICPErrorBeforeSingleSupportX()
   {
      return 0.02;
   }

   @Override
   public double getMaxICPErrorBeforeSingleSupportY()
   {
      return 0.02;
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
      return -1.0;
   }

   /** {@inheritDoc} */
   @Override
   public double getCoPErrorThresholdForHighCoPDamping()
   {
      return Double.POSITIVE_INFINITY;
   }

   @Override
   public boolean useOptimizationBasedICPController()
   {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public LegConfigurationParameters getLegConfigurationParameters()
   {
      return legConfigurationParameters;
   }

   /** {@inheritDoc} */
   @Override
   public boolean minimizeAngularMomentumRateZDuringSwing()
   {
      // For some reason it causes the test ValkyrieEndToEndCinderBlockFieldTest to fail by making the state estimator drift more than usual.
      // As there is no real need for it in sim, I'm leaving it on only for the real robot. (Sylvain)
      return target == RobotTarget.REAL_ROBOT;
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
   public SteppingParameters getSteppingParameters()
   {
      return steppingParameters;
   }
}
