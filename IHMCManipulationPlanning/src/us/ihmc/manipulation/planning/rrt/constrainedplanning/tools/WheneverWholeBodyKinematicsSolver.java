package us.ihmc.manipulation.planning.rrt.constrainedplanning.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.commonWalkingControlModules.configurations.JointPrivilegedConfigurationParameters;
import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerDataReadOnly;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCore;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.CenterOfMassFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.JointLimitReductionCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand.PrivilegedConfigurationOption;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.packets.KinematicsToolboxOutputStatus;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicCoordinateSystem;
import us.ihmc.humanoidRobotics.communication.packets.KinematicsToolboxOutputConverter;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotModels.FullRobotModelUtils;
import us.ihmc.robotics.controllers.SE3PIDGainsInterface;
import us.ihmc.robotics.controllers.YoSymmetricSE3PIDGains;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoseUsingQuaternions;
import us.ihmc.robotics.partNames.ArmJointName;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.FloatingInverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.robotics.sensors.ForceSensorDefinition;
import us.ihmc.robotics.sensors.IMUDefinition;
import us.ihmc.robotics.weightMatrices.WeightMatrix6D;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;

/*
 * Make sure this class is for testing whole-body inverse kinematics only.
 * Whenever we want to get whole-body solution without sending and receiving message on @code KinematicsToolboxModule, use this one.
 * Use 'setDesired<rigidBody>Message()', 'putTrajectoryMessages()' method and 'isSolved()' method to verify the user put messages.
 * This class has originality in @code KinematicsToolboxController and @author Sylvain Bertrand.
 * 2017.06.19. Inho Lee. 
 */

public class WheneverWholeBodyKinematicsSolver
{
   private boolean DEBUG = false;
   protected final String name = getClass().getSimpleName();
   protected final YoVariableRegistry registry = new YoVariableRegistry(name);

   private WheneverWholeBodyFunctions wholeBodyFunctions = new WheneverWholeBodyFunctions();

   private FullHumanoidRobotModelFactory fullRobotModelFactory;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final double updateDT = 1.0e-3;
   private static final String centerOfMassName = "CenterOfMass";

   private final FullHumanoidRobotModel desiredFullRobotModel;
   /** Reference to the desired robot's root body. */
   private final RigidBody rootBody;
   /** Reference to the desired robot's floating joint. */
   private final FloatingInverseDynamicsJoint rootJoint;
   private final OneDoFJoint[] oneDoFJoints;
   private final Map<Long, OneDoFJoint> jointNameBasedHashCodeMap = new HashMap<>();
   private final CommonHumanoidReferenceFrames referenceFrames;

   /** The same set of gains is used for controlling any part of the desired robot body. */
   private final YoSymmetricSE3PIDGains gains = new YoSymmetricSE3PIDGains("genericGains", registry);
   private final ControllerCoreCommand controllerCoreCommand = new ControllerCoreCommand(WholeBodyControllerCoreMode.INVERSE_KINEMATICS);
   private final WholeBodyControllerCore controllerCore;
   private final FeedbackControllerDataReadOnly feedbackControllerDataHolder;

   private final KinematicsToolboxOutputStatus inverseKinematicsSolution;
   private final YoDouble solutionQualityOld = new YoDouble("solutionQualityOld", registry);
   private final YoDouble solutionQuality = new YoDouble("solutionQuality", registry);
   private final SideDependentList<YoBoolean> isFootInSupport = new SideDependentList<>();
   private final SideDependentList<YoFramePoseUsingQuaternions> initialFootPoses = new SideDependentList<>();
   private final YoFramePoint initialCenterOfMassPosition = new YoFramePoint("initialCenterOfMass", worldFrame, registry);
   private final YoBoolean holdSupportFootPose = new YoBoolean("holdSupportFootPose", registry);
   private final YoBoolean holdCenterOfMassXYPosition = new YoBoolean("holdCenterOfMassXYPosition", registry);
   private final YoDouble privilegedWeight = new YoDouble("privilegedWeight", registry);
   private final YoDouble privilegedConfigurationGain = new YoDouble("privilegedConfigurationGain", registry);
   private final YoDouble privilegedMaxVelocity = new YoDouble("privilegedMaxVelocity", registry);
   private final AtomicReference<PrivilegedConfigurationCommand> privilegedConfigurationCommandReference = new AtomicReference<>(null);

   private final YoDouble footWeight = new YoDouble("footWeight", registry);
   private final YoDouble momentumWeight = new YoDouble("momentumWeight", registry);
   private int tickCount = 0;

   private final EnumMap<LegJointName, YoDouble> legJointLimitReductionFactors = new EnumMap<>(LegJointName.class);
   private final List<RigidBody> listOfControllableRigidBodies = new ArrayList<>();
   private final Map<RigidBody, YoGraphicCoordinateSystem> desiredCoodinateSystems = new HashMap<>();
   private final Map<RigidBody, YoGraphicCoordinateSystem> currentCoodinateSystems = new HashMap<>();
   private final AtomicReference<RobotConfigurationData> latestRobotConfigurationDataReference = new AtomicReference<>(null);
   private final Map<String, FeedbackControlCommand<?>> userFeedbackCommands = new HashMap<>();
   private final YoInteger numberOfActiveCommands = new YoInteger("numberOfActiveCommands", registry);

   private KinematicsToolboxOutputConverter outputConverter;

   private SideDependentList<WeightMatrix6D> handWeightMatrices = new SideDependentList<>(new WeightMatrix6D(), new WeightMatrix6D());
   private SideDependentList<SelectionMatrix6D> handSelectionMatrices = new SideDependentList<>(new SelectionMatrix6D(), new SelectionMatrix6D());
   private SideDependentList<FramePose> handFramePoses = new SideDependentList<>(new FramePose(), new FramePose());

   private WeightMatrix6D pelvisWeightMatrix = new WeightMatrix6D();
   private SelectionMatrix6D pelvisSelectionMatrix = new SelectionMatrix6D();
   private FramePose pelvisFramePose = new FramePose();

   private WeightMatrix6D chestWeightMatrix = new WeightMatrix6D();
   private SelectionMatrix6D chestSelectionMatrix = new SelectionMatrix6D();
   private FrameOrientation chestFrameOrientation = new FrameOrientation();

   private static int maximumCntForUpdateInternal = 200;
   private static int cntForUpdateInternal = 0;

   private static int numberOfTest = 0;

   private boolean isSolved = false;

   public WheneverWholeBodyKinematicsSolver(FullHumanoidRobotModelFactory fullRobotModelFactory)
   {
      this.fullRobotModelFactory = fullRobotModelFactory;
      outputConverter = new KinematicsToolboxOutputConverter(fullRobotModelFactory);

      this.desiredFullRobotModel = fullRobotModelFactory.createFullRobotModel();

      referenceFrames = new HumanoidReferenceFrames(this.desiredFullRobotModel);
      rootBody = this.desiredFullRobotModel.getElevator();
      rootJoint = this.desiredFullRobotModel.getRootJoint();

      populateJointLimitReductionFactors();
      populateListOfControllableRigidBodies();

      controllerCore = createControllerCore();
      feedbackControllerDataHolder = controllerCore.getWholeBodyFeedbackControllerDataHolder();

      oneDoFJoints = FullRobotModelUtils.getAllJointsExcludingHands(this.desiredFullRobotModel);
      Arrays.stream(oneDoFJoints).forEach(joint -> jointNameBasedHashCodeMap.put(joint.getNameBasedHashCode(), joint));

      inverseKinematicsSolution = new KinematicsToolboxOutputStatus(oneDoFJoints);
      inverseKinematicsSolution.setDestination(-1);

      gains.setProportionalGain(800.0); // Gains used for everything. It is as high as possible to reduce the convergence time.

      footWeight.set(200.0);
      momentumWeight.set(1.0);
      privilegedWeight.set(1.0);
      privilegedConfigurationGain.set(50.0);
      privilegedMaxVelocity.set(Double.POSITIVE_INFINITY);

      for (RobotSide robotSide : RobotSide.values)
      {
         String side = robotSide.getCamelCaseNameForMiddleOfExpression();
         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
         isFootInSupport.put(robotSide, new YoBoolean("is" + side + "FootInSupport", registry));
         initialFootPoses.put(robotSide, new YoFramePoseUsingQuaternions(sidePrefix + "FootInitial", worldFrame, registry));
      }
   }

   public WheneverWholeBodyKinematicsSolver(FullHumanoidRobotModelFactory fullRobotModelFactory, FullHumanoidRobotModel sdfFullRobotModel)
   {
      this.fullRobotModelFactory = fullRobotModelFactory;
      outputConverter = new KinematicsToolboxOutputConverter(fullRobotModelFactory);

      this.desiredFullRobotModel = fullRobotModelFactory.createFullRobotModel();

      referenceFrames = new HumanoidReferenceFrames(this.desiredFullRobotModel);
      rootBody = this.desiredFullRobotModel.getElevator();
      rootJoint = this.desiredFullRobotModel.getRootJoint();

      populateJointLimitReductionFactors();
      populateListOfControllableRigidBodies();

      controllerCore = createControllerCore();
      feedbackControllerDataHolder = controllerCore.getWholeBodyFeedbackControllerDataHolder();

      oneDoFJoints = FullRobotModelUtils.getAllJointsExcludingHands(this.desiredFullRobotModel);
      Arrays.stream(oneDoFJoints).forEach(joint -> jointNameBasedHashCodeMap.put(joint.getNameBasedHashCode(), joint));

      inverseKinematicsSolution = new KinematicsToolboxOutputStatus(oneDoFJoints);
      inverseKinematicsSolution.setDestination(-1);

      gains.setProportionalGain(800.0); // Gains used for everything. It is as high as possible to reduce the convergence time.

      footWeight.set(200.0);
      momentumWeight.set(1.0);
      privilegedWeight.set(1.0);
      privilegedConfigurationGain.set(50.0);
      privilegedMaxVelocity.set(Double.POSITIVE_INFINITY);

      for (RobotSide robotSide : RobotSide.values)
      {
         String side = robotSide.getCamelCaseNameForMiddleOfExpression();
         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
         isFootInSupport.put(robotSide, new YoBoolean("is" + side + "FootInSupport", registry));
         initialFootPoses.put(robotSide, new YoFramePoseUsingQuaternions(sidePrefix + "FootInitial", worldFrame, registry));
      }

      updateRobotConfigurationData(FullRobotModelUtils.getAllJointsExcludingHands(sdfFullRobotModel), sdfFullRobotModel.getRootJoint());
   }

   public WholeBodyControllerCore createControllerCore()
   {
      InverseDynamicsJoint[] controlledJoints = HighLevelHumanoidControllerToolbox.computeJointsToOptimizeFor(desiredFullRobotModel);
      ReferenceFrame centerOfMassFrame = referenceFrames.getCenterOfMassFrame();
      WholeBodyControlCoreToolbox toolbox = new WholeBodyControlCoreToolbox(updateDT, 0.0, rootJoint, controlledJoints, centerOfMassFrame, null, null,
                                                                            registry);
      toolbox.setJointPrivilegedConfigurationParameters(new JointPrivilegedConfigurationParameters());
      toolbox.setupForInverseKinematicsSolver();
      FeedbackControlCommandList controllerCoreTemplate = createControllerCoreTemplate();
      controllerCoreTemplate.addCommand(new CenterOfMassFeedbackControlCommand());
      return new WholeBodyControllerCore(toolbox, controllerCoreTemplate, registry);
   }

   public void populateJointLimitReductionFactors()
   {
      YoDouble hipReductionFactor = new YoDouble("hipLimitReductionFactor", registry);
      YoDouble kneeReductionFactor = new YoDouble("kneeLimitReductionFactor", registry);
      YoDouble ankleReductionFactor = new YoDouble("ankleLimitReductionFactor", registry);
      hipReductionFactor.set(0.05);

      legJointLimitReductionFactors.put(LegJointName.HIP_PITCH, hipReductionFactor);
      legJointLimitReductionFactors.put(LegJointName.HIP_ROLL, hipReductionFactor);
      legJointLimitReductionFactors.put(LegJointName.HIP_YAW, hipReductionFactor);
      legJointLimitReductionFactors.put(LegJointName.KNEE_PITCH, kneeReductionFactor);
      legJointLimitReductionFactors.put(LegJointName.ANKLE_PITCH, ankleReductionFactor);
      legJointLimitReductionFactors.put(LegJointName.ANKLE_ROLL, ankleReductionFactor);
   }

   private void populateListOfControllableRigidBodies()
   {
      listOfControllableRigidBodies.add(desiredFullRobotModel.getChest());
      listOfControllableRigidBodies.add(desiredFullRobotModel.getPelvis());

      for (RobotSide robotSide : RobotSide.values)
      {
         listOfControllableRigidBodies.add(desiredFullRobotModel.getHand(robotSide));
         listOfControllableRigidBodies.add(desiredFullRobotModel.getFoot(robotSide));
      }
   }

   private FeedbackControlCommandList createControllerCoreTemplate()
   {
      FeedbackControlCommandList template = new FeedbackControlCommandList();
      listOfControllableRigidBodies.stream().map(this::createFeedbackControlCommand).forEach(template::addCommand);
      return template;
   }

   private SpatialFeedbackControlCommand createFeedbackControlCommand(RigidBody endEffector)
   {
      SpatialFeedbackControlCommand command = new SpatialFeedbackControlCommand();
      command.set(rootBody, endEffector);
      return command;
   }

   public boolean initialize2()
   {
      controllerCore.initialize();
      userFeedbackCommands.clear();
      isSolved = false;
      cntForUpdateInternal = 0;
      solutionQualityOld.setToNaN();
      //---------------------------------------------      
      RobotConfigurationData robotConfigurationData = latestRobotConfigurationDataReference.get();
      wholeBodyFunctions.setRobotStateFromRobotConfigurationData(robotConfigurationData, rootJoint, oneDoFJoints);
      //---------------------------------------------                        
      desiredFullRobotModel.updateFrames();

      for (RobotSide robotSide : RobotSide.values)
         isFootInSupport.get(robotSide).set(true);

      // Initialize the initialCenterOfMassPosition and initialFootPoses to match the current state of the robot.
      updateCoMPositionAndFootPoses();

      // By default, always hold the support foot/feet and center of mass in place. This can be changed on the fly by sending a KinematicsToolboxConfigurationMessage.
      holdSupportFootPose.set(true);
      holdCenterOfMassXYPosition.set(true);

      // Sets the privileged configuration to match the current robot configuration such that the solution will be as close as possible to the current robot configuration.
      snapPrivilegedConfigurationToCurrent();

      return true;
   }

   public boolean initialize()
   {
      userFeedbackCommands.clear();
      isSolved = false;
      cntForUpdateInternal = 0;
      solutionQualityOld.setToNaN();

      RobotConfigurationData robotConfigurationData = latestRobotConfigurationDataReference.get();

      if (robotConfigurationData == null)
         return false;

      // Initializes this desired robot to the most recent robot configuration data received from the walking controller.
      wholeBodyFunctions.setRobotStateFromRobotConfigurationData(robotConfigurationData, rootJoint, oneDoFJoints);

      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         double jointPosition = oneDoFJoints[i].getQ();
         if (DEBUG)
            System.out.println(oneDoFJoints[i].getName() + " " + jointPosition);
      }

      for (RobotSide robotSide : RobotSide.values)
         isFootInSupport.get(robotSide).set(true);

      // Initialize the initialCenterOfMassPosition and initialFootPoses to match the current state of the robot.
      updateCoMPositionAndFootPoses();

      // By default, always hold the support foot/feet and center of mass in place. This can be changed on the fly by sending a KinematicsToolboxConfigurationMessage.
      holdSupportFootPose.set(true);
      holdCenterOfMassXYPosition.set(true);

      // Sets the privileged configuration to match the current robot configuration such that the solution will be as close as possible to the current robot configuration.
      snapPrivilegedConfigurationToCurrent();
      //      if (DEBUG)
      //         PrintTools.info("Initial posture ");
      //      HumanoidReferenceFrames desiredReferenceFrames = new HumanoidReferenceFrames(desiredFullRobotModel);
      //      desiredReferenceFrames.updateFrames();
      //      if (DEBUG)
      //         printOutRobotModel(desiredFullRobotModel, desiredReferenceFrames.getMidFootZUpGroundFrame());

      return true;
   }

   private void updateInternal()
   {
      // Updating the reference frames and twist calculator.
      updateTools();

      // Compiling all the commands to be submitted to the controller core.
      controllerCoreCommand.clear();
      controllerCoreCommand.addFeedbackControlCommand(createHoldCenterOfMassXYCommand());
      controllerCoreCommand.addFeedbackControlCommand(createHoldSupportFootCommands());

      FeedbackControlCommandList userCommands = new FeedbackControlCommandList();
      userFeedbackCommands.values().forEach(userCommands::addCommand);

      numberOfActiveCommands.set(userCommands.getNumberOfCommands());
      controllerCoreCommand.addFeedbackControlCommand(userCommands);

      controllerCoreCommand.addInverseKinematicsCommand(createJointLimitReductionCommand());
      controllerCoreCommand.addInverseKinematicsCommand(privilegedConfigurationCommandReference.getAndSet(null));

      // Save all commands used for this control tick for computing the solution quality.
      FeedbackControlCommandList allFeedbackControlCommands = new FeedbackControlCommandList(controllerCoreCommand.getFeedbackControlCommandList());

      controllerCore.reset();
      controllerCore.submitControllerCoreCommand(controllerCoreCommand);
      controllerCore.compute();
      // Calculating the solution quality based on sum of all the commands' tracking error.
      solutionQuality.set(wholeBodyFunctions.calculateSolutionQuality(allFeedbackControlCommands, feedbackControllerDataHolder));

      // Updating the the robot state from the current solution, initializing the next control tick.
      wholeBodyFunctions.setRobotStateFromControllerCoreOutput(controllerCore.getControllerCoreOutput(), rootJoint, oneDoFJoints);

      inverseKinematicsSolution.setDesiredJointState(rootJoint, oneDoFJoints);
      inverseKinematicsSolution.setSolutionQuality(solutionQuality.getDoubleValue());

      double solutionStableThreshold = 0.01;
      double solutionQualityThreshold = 0.005;

      double deltaSolutionQuality = solutionQuality.getDoubleValue() - solutionQualityOld.getDoubleValue();
      boolean isSolutionStable = (Math.abs(deltaSolutionQuality) < solutionStableThreshold);
      boolean isSolutionGoodEnough = solutionQuality.getDoubleValue() < solutionQualityThreshold;
      boolean isGoodSolutionCur = isSolutionStable && isSolutionGoodEnough;

      if (DEBUG)
         PrintTools.info("" + cntForUpdateInternal + " cur SQ " + solutionQuality.getDoubleValue() + " old " + solutionQualityOld.getDoubleValue() + " "
               + isSolutionStable + " " + isSolutionGoodEnough + " " + isGoodSolutionCur);

      if (isGoodSolutionCur)
      {
         isSolved = true;

      }

      solutionQualityOld.set(solutionQuality.getDoubleValue());
      cntForUpdateInternal++;
   }

   public boolean getIsSolved()
   {
      return isSolved;
   }

   public boolean isSolved()
   {
      numberOfTest++;

      for (int i = 0; i < maximumCntForUpdateInternal; i++)
      {
         updateInternal();
         if (isSolved)
         {
            if (DEBUG)
               printOutRobotModel(desiredFullRobotModel, referenceFrames.getMidFootZUpGroundFrame());
            PrintTools.info("cntForUpdateInternal " + cntForUpdateInternal);

            return isSolved;
         }
      }
      PrintTools.info("cntForUpdateInternal " + cntForUpdateInternal);
      return false;
   }

   public int getCntForUpdateInternal()
   {
      return cntForUpdateInternal;
   }

   public void updateTools()
   {
      desiredFullRobotModel.updateFrames();
      referenceFrames.updateFrames();
   }

   private FeedbackControlCommand<?> createHoldSupportFootCommands()
   {
      if (!holdSupportFootPose.getBooleanValue())
         return null;

      FeedbackControlCommandList inputs = new FeedbackControlCommandList();

      for (RobotSide robotSide : RobotSide.values)
      {
         if (!isFootInSupport.get(robotSide).getBooleanValue())
            continue;

         RigidBody foot = desiredFullRobotModel.getFoot(robotSide);

         // Do not hold the foot position if the user is already controlling it.
         if (userFeedbackCommands.containsKey(foot.getName()))
            continue;

         FramePose poseToHold = new FramePose();
         initialFootPoses.get(robotSide).getFramePoseIncludingFrame(poseToHold);

         SpatialFeedbackControlCommand feedbackControlCommand = new SpatialFeedbackControlCommand();
         feedbackControlCommand.set(rootBody, foot);
         feedbackControlCommand.setGains((SE3PIDGainsInterface) gains);
         feedbackControlCommand.setWeightForSolver(footWeight.getDoubleValue());
         feedbackControlCommand.set(poseToHold);
         inputs.addCommand(feedbackControlCommand);
      }
      return inputs;
   }

   private FeedbackControlCommand<?> createHoldCenterOfMassXYCommand()
   {
      if (!holdCenterOfMassXYPosition.getBooleanValue())
         return null;

      // Do not hold the CoM position if the user is already controlling it.
      if (userFeedbackCommands.containsKey(centerOfMassName))
      {
         holdCenterOfMassXYPosition.set(false);
         return null;
      }

      FramePoint positionToHold = new FramePoint();
      initialCenterOfMassPosition.getFrameTupleIncludingFrame(positionToHold);

      CenterOfMassFeedbackControlCommand feedbackControlCommand = new CenterOfMassFeedbackControlCommand();
      feedbackControlCommand.setGains(gains);
      feedbackControlCommand.setWeightForSolver(momentumWeight.getDoubleValue());
      feedbackControlCommand.setSelectionMatrixForLinearXYControl();
      feedbackControlCommand.set(positionToHold);
      return feedbackControlCommand;
   }

   private JointLimitReductionCommand createJointLimitReductionCommand()
   {
      JointLimitReductionCommand jointLimitReductionCommand = new JointLimitReductionCommand();
      for (RobotSide robotSide : RobotSide.values)
      {
         for (LegJointName legJointName : desiredFullRobotModel.getRobotSpecificJointNames().getLegJointNames())
         {
            OneDoFJoint joint = desiredFullRobotModel.getLegJoint(robotSide, legJointName);
            double reductionFactor = legJointLimitReductionFactors.get(legJointName).getDoubleValue();
            jointLimitReductionCommand.addReductionFactor(joint, reductionFactor);
         }
      }
      return jointLimitReductionCommand;
   }

   private void updateCoMPositionAndFootPoses()
   {
      updateTools();

      initialCenterOfMassPosition.setFromReferenceFrame(referenceFrames.getCenterOfMassFrame());

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody foot = desiredFullRobotModel.getFoot(robotSide);
         initialFootPoses.get(robotSide).setFromReferenceFrame(foot.getBodyFixedFrame());
      }
   }

   private void snapPrivilegedConfigurationToCurrent()
   {
      PrivilegedConfigurationCommand privilegedConfigurationCommand = new PrivilegedConfigurationCommand();
      privilegedConfigurationCommand.setPrivilegedConfigurationOption(PrivilegedConfigurationOption.AT_CURRENT);
      privilegedConfigurationCommand.setDefaultWeight(privilegedWeight.getDoubleValue());
      privilegedConfigurationCommand.setDefaultConfigurationGain(privilegedConfigurationGain.getDoubleValue());
      privilegedConfigurationCommand.setDefaultMaxVelocity(privilegedMaxVelocity.getDoubleValue());
      privilegedConfigurationCommandReference.set(privilegedConfigurationCommand);
   }

   public void updateRobotConfigurationData(OneDoFJoint[] joints, Vector3D translation, Quaternion rotation)
   {
      ForceSensorDefinition[] forceSensorDefinitions;
      IMUDefinition[] imuDefinitions;

      imuDefinitions = desiredFullRobotModel.getIMUDefinitions();
      forceSensorDefinitions = desiredFullRobotModel.getForceSensorDefinitions();

      RobotConfigurationData currentRobotConfigurationData = new RobotConfigurationData(joints, forceSensorDefinitions, null, imuDefinitions);

      currentRobotConfigurationData.setRootOrientation(new Quaternion(rotation));
      currentRobotConfigurationData.setRootTranslation(new Vector3D(translation));

      currentRobotConfigurationData.setJointState(joints);

      updateRobotConfigurationData(currentRobotConfigurationData);
   }

   public void updateRobotConfigurationData(OneDoFJoint[] joints, FloatingInverseDynamicsJoint rootJoint)
   {
      ForceSensorDefinition[] forceSensorDefinitions;
      IMUDefinition[] imuDefinitions;

      imuDefinitions = desiredFullRobotModel.getIMUDefinitions();
      forceSensorDefinitions = desiredFullRobotModel.getForceSensorDefinitions();

      RobotConfigurationData currentRobotConfigurationData = new RobotConfigurationData(joints, forceSensorDefinitions, null, imuDefinitions);

      currentRobotConfigurationData.setRootOrientation(new Quaternion(rootJoint.getRotationForReading()));
      currentRobotConfigurationData.setRootTranslation(new Vector3D(rootJoint.getTranslationForReading()));

      currentRobotConfigurationData.setJointState(joints);

      updateRobotConfigurationData(currentRobotConfigurationData);
   }

   public void updateRobotConfigurationDataJointsOnly(OneDoFJoint[] joints)
   {
      latestRobotConfigurationDataReference.get().setJointState(joints);
   }

   private void updateRobotConfigurationData(RobotConfigurationData newConfigurationData)
   {
      System.out.println(newConfigurationData.rootTranslation);
      System.out.println(newConfigurationData.rootOrientation);
      latestRobotConfigurationDataReference.set(newConfigurationData);
   }

   public FullHumanoidRobotModel getDesiredFullRobotModel()
   {
      return desiredFullRobotModel;
   }

   public FullHumanoidRobotModel getFullRobotModelCopy()
   {
      KinematicsToolboxOutputStatus currentOutputStatus = new KinematicsToolboxOutputStatus(oneDoFJoints);

      for (int i = 0; i < oneDoFJoints.length; i++)
         oneDoFJoints[i].setqDesired(oneDoFJoints[i].getQ());

      currentOutputStatus.setDesiredJointState(rootJoint, oneDoFJoints);

      KinematicsToolboxOutputConverter currentOutputConverter;
      currentOutputConverter = new KinematicsToolboxOutputConverter(fullRobotModelFactory);

      currentOutputConverter.updateFullRobotModel(currentOutputStatus);
      FullHumanoidRobotModel fullRobotModelCopy = currentOutputConverter.getFullRobotModel();

      return fullRobotModelCopy;
   }

   protected boolean isDone()
   {
      /*
       * blank.
       */
      return false;
   }

   public KinematicsToolboxOutputStatus getSolution()
   {
      return inverseKinematicsSolution;
   }

   public CommonHumanoidReferenceFrames getReferenceFrames()
   {
      return referenceFrames;
   }

   public void holdCurrentTrajectoryMessages()
   {
      KinematicsToolboxOutputStatus currentOutputStatus = new KinematicsToolboxOutputStatus(oneDoFJoints);
      for (int i = 0; i < oneDoFJoints.length; i++)
         oneDoFJoints[i].setqDesired(oneDoFJoints[i].getQ());

      currentOutputStatus.setDesiredJointState(rootJoint, oneDoFJoints);

      KinematicsToolboxOutputConverter currentOutputConverter;
      currentOutputConverter = new KinematicsToolboxOutputConverter(fullRobotModelFactory);

      currentOutputConverter.updateFullRobotModel(currentOutputStatus);
      FullHumanoidRobotModel currentFullRobotModel = currentOutputConverter.getFullRobotModel();

      HumanoidReferenceFrames currentReferenceFrames = new HumanoidReferenceFrames(currentFullRobotModel);
      currentReferenceFrames.updateFrames();

      if (DEBUG)
         PrintTools.info("Posture to be hold is ");
      if (DEBUG)
         printOutRobotModel(currentFullRobotModel, currentReferenceFrames.getMidFootZUpGroundFrame());

      for (RobotSide robotSide : RobotSide.values)
      {
         handSelectionMatrices.get(robotSide).clearSelection();
         handSelectionMatrices.get(robotSide).setLinearAxisSelection(true, true, true);
         handSelectionMatrices.get(robotSide).setAngularAxisSelection(true, true, true);

         double handWeight = 20.0;
         handWeightMatrices.get(robotSide).setLinearWeights(handWeight, handWeight, handWeight);
         handWeightMatrices.get(robotSide).setAngularWeights(handWeight, handWeight, handWeight);

         ReferenceFrame desiredHandReferenceFrame = currentFullRobotModel.getHand(robotSide).getBodyFixedFrame();
         FramePose desiredHandFramePose = new FramePose(desiredHandReferenceFrame);

         desiredHandFramePose.changeFrame(worldFrame);

         handFramePoses.get(robotSide).set(desiredHandFramePose);
      }

      ReferenceFrame desiredPelvisReferenceFrame = currentFullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint();
      FramePose desiredPelvisFramePose = new FramePose(desiredPelvisReferenceFrame);
      desiredPelvisFramePose.changeFrame(worldFrame);

      pelvisSelectionMatrix.clearSelection();
      pelvisSelectionMatrix.setLinearAxisSelection(false, false, true);
      pelvisSelectionMatrix.setAngularAxisSelection(true, true, true);

      double pelvisWeight = 10.0;
      pelvisWeightMatrix.setLinearWeights(pelvisWeight, pelvisWeight, pelvisWeight);
      pelvisWeightMatrix.setAngularWeights(pelvisWeight, pelvisWeight, pelvisWeight);

      pelvisFramePose.set(desiredPelvisFramePose);

      ReferenceFrame desiredChestReferenceFrame = currentFullRobotModel.getChest().getBodyFixedFrame();
      FrameOrientation desiredChestFrameOrientation = new FrameOrientation(desiredChestReferenceFrame);
      desiredChestFrameOrientation.changeFrame(worldFrame);

      chestSelectionMatrix.clearSelection();
      chestSelectionMatrix.setLinearAxisSelection(false, false, false);
      chestSelectionMatrix.setAngularAxisSelection(true, true, true);

      double chestWeight = 10.0;
      chestWeightMatrix.setLinearWeights(chestWeight, chestWeight, chestWeight);
      chestWeightMatrix.setAngularWeights(chestWeight, chestWeight, chestWeight);

      chestFrameOrientation.set(desiredChestFrameOrientation);
   }

   public void setDesiredHandPose(RobotSide robotSide, Pose3D desiredPoseToMidZUp)
   {
      handSelectionMatrices.get(robotSide).clearSelection();
      handSelectionMatrices.get(robotSide).setLinearAxisSelection(true, true, true);
      handSelectionMatrices.get(robotSide).setAngularAxisSelection(true, true, true);

      handWeightMatrices.get(robotSide).setLinearWeights(20.0, 20.0, 20.0);
      handWeightMatrices.get(robotSide).setAngularWeights(20.0, 20.0, 20.0);

      /*
       * The Z coordinate is upward like as robot coordinate and matched when
       * human thumb up ahead. The X coordinate is forward like as robot
       * coordinate and matched when human punch out ahead.
       */
      Quaternion preMultipliedOrientation = new Quaternion();
      if (robotSide == RobotSide.RIGHT)
      {
         preMultipliedOrientation.appendRollRotation(-Math.PI * 0.5);
         preMultipliedOrientation.appendYawRotation(Math.PI * 0.5);
      }
      else
      {
         preMultipliedOrientation.appendRollRotation(Math.PI * 0.5);
         preMultipliedOrientation.appendYawRotation(-Math.PI * 0.5);
      }

      FramePoint desiredPointToWorld = new FramePoint(referenceFrames.getMidFootZUpGroundFrame(), desiredPoseToMidZUp.getPosition());
      FrameOrientation desiredOrientationToWorld = new FrameOrientation(referenceFrames.getMidFootZUpGroundFrame(), desiredPoseToMidZUp.getOrientation());

      desiredOrientationToWorld.multiply(preMultipliedOrientation);

      FramePose desiredPoseToWorld = new FramePose(desiredPointToWorld, desiredOrientationToWorld);

      desiredPoseToWorld.changeFrame(worldFrame);

      handFramePoses.get(robotSide).set(desiredPoseToWorld);
   }

   public void setDesiredPelvisHeight(double desiredHeightToMidZUp)
   {
      pelvisSelectionMatrix.clearLinearSelection();
      pelvisSelectionMatrix.selectLinearZ(true);
      pelvisSelectionMatrix.setSelectionFrame(worldFrame);

      FramePoint desiredPointToWorld = new FramePoint(referenceFrames.getMidFootZUpGroundFrame(), new Point3D(0, 0, desiredHeightToMidZUp));
      FrameOrientation desiredOrientationToWorld = new FrameOrientation(referenceFrames.getMidFootZUpGroundFrame(), new Quaternion());

      FramePose desiredPoseToWorld = new FramePose(desiredPointToWorld, desiredOrientationToWorld);
      desiredPoseToWorld.changeFrame(worldFrame);

      pelvisFramePose.set(desiredPoseToWorld);
   }

   public void setDesiredChestOrientation(Quaternion desiredOrientationToMidZUp)
   {
      FrameOrientation desiredOrientationToWorld = new FrameOrientation(referenceFrames.getMidFootZUpGroundFrame(), desiredOrientationToMidZUp);
      desiredOrientationToWorld.changeFrame(worldFrame);

      chestSelectionMatrix.clearSelection();
      chestSelectionMatrix.setLinearAxisSelection(false, false, false);
      chestSelectionMatrix.setAngularAxisSelection(true, true, true);

      double chestWeight = 10.0;
      chestWeightMatrix.setLinearWeights(chestWeight, chestWeight, chestWeight);
      chestWeightMatrix.setAngularWeights(chestWeight, chestWeight, chestWeight);

      chestFrameOrientation.set(desiredOrientationToWorld);
   }

   public void setHandSelectionMatrix(RobotSide robotSide, SelectionMatrix6D selectionMatrix)
   {
      handSelectionMatrices.get(robotSide).set(selectionMatrix);
   }

   public void setPelvisSelectionMatrix(SelectionMatrix6D selectionMatrix)
   {
      pelvisSelectionMatrix.set(selectionMatrix);
   }

   public void setChestSelectionMatrix(SelectionMatrix6D selectionMatrix)
   {
      chestSelectionMatrix.set(selectionMatrix);
   }

   public void setHandSelectionMatrixFree(RobotSide robotSide)
   {
      SelectionMatrix6D selectionMatrix = new SelectionMatrix6D();
      selectionMatrix.clearSelection();
      setHandSelectionMatrix(robotSide, selectionMatrix);
   }

   public void putTrajectoryMessages()
   {
      putHandTrajectoryMessages();
      putPelvisTrajectoryMessages();
      putChestTrajectoryMessages();
   }

   private void putHandTrajectoryMessages()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         if (handFramePoses.get(robotSide).containsNaN())
         {
            PrintTools.warn("The desired " + robotSide + " has Nan Value");
         }
         else
         {
            SpatialFeedbackControlCommand feedbackControlCommand = new SpatialFeedbackControlCommand();

            feedbackControlCommand.set(rootBody, desiredFullRobotModel.getHand(robotSide));
            feedbackControlCommand.setGains((SE3PIDGainsInterface) gains);

            feedbackControlCommand.setWeightMatrixForSolver(handWeightMatrices.get(robotSide));
            feedbackControlCommand.setSelectionMatrix(handSelectionMatrices.get(robotSide));
            feedbackControlCommand.set(handFramePoses.get(robotSide));
            userFeedbackCommands.put(desiredFullRobotModel.getHand(robotSide).getName(), feedbackControlCommand);
         }
      }
   }

   private void putPelvisTrajectoryMessages()
   {
      SpatialFeedbackControlCommand feedbackControlCommand = new SpatialFeedbackControlCommand();

      feedbackControlCommand.set(rootBody, desiredFullRobotModel.getPelvis());
      feedbackControlCommand.setGains((SE3PIDGainsInterface) gains);

      feedbackControlCommand.setWeightMatrixForSolver(pelvisWeightMatrix);
      feedbackControlCommand.setSelectionMatrix(pelvisSelectionMatrix);
      feedbackControlCommand.set(pelvisFramePose);

      userFeedbackCommands.put(desiredFullRobotModel.getPelvis().getName(), feedbackControlCommand);
   }

   private void putChestTrajectoryMessages()
   {
      SpatialFeedbackControlCommand feedbackControlCommand = new SpatialFeedbackControlCommand();
      feedbackControlCommand.set(rootBody, desiredFullRobotModel.getChest());
      feedbackControlCommand.setGains((SE3PIDGainsInterface) gains);

      feedbackControlCommand.setWeightMatrixForSolver(chestWeightMatrix);
      feedbackControlCommand.setSelectionMatrix(chestSelectionMatrix);
      feedbackControlCommand.set(chestFrameOrientation);

      userFeedbackCommands.put(desiredFullRobotModel.getChest().getName(), feedbackControlCommand);
   }

   public double getArmJointLimitScore(RobotSide robotSide)
   {
      double jointLimitScore = 0;

      jointLimitScore = jointLimitScore + getJointLimitScore("back_bkz");
      jointLimitScore = jointLimitScore + getJointLimitScore("back_bky");
      jointLimitScore = jointLimitScore + getJointLimitScore("back_bkx");
      jointLimitScore = jointLimitScore + getArmJointLimitScore(robotSide, ArmJointName.SHOULDER_YAW);
      jointLimitScore = jointLimitScore + getArmJointLimitScore(robotSide, ArmJointName.SHOULDER_ROLL);
      jointLimitScore = jointLimitScore + getArmJointLimitScore(robotSide, ArmJointName.ELBOW_PITCH);
      jointLimitScore = jointLimitScore + getArmJointLimitScore(robotSide, ArmJointName.ELBOW_ROLL);
      jointLimitScore = jointLimitScore + getArmJointLimitScore(robotSide, ArmJointName.FIRST_WRIST_PITCH);
      jointLimitScore = jointLimitScore + getArmJointLimitScore(robotSide, ArmJointName.WRIST_ROLL);

      return jointLimitScore;
   }

   private double getArmJointLimitScore(RobotSide robotSide, ArmJointName armJointName)
   {
      String jointName = desiredFullRobotModel.getArmJoint(robotSide, armJointName).getName();

      return getJointLimitScore(jointName);
   }

   private double getJointLimitScore(String jointName)
   {
      OneDoFJoint aJoint = desiredFullRobotModel.getOneDoFJointByName(jointName);

      double jointLimitScore = 0;      
      double aJointValue = aJoint.getQ();
      double upperValue = aJoint.getJointLimitUpper();
      double lowerValue = aJoint.getJointLimitLower();
      
      double limitSize = upperValue - lowerValue;
      aJointValue = aJointValue/limitSize;
      upperValue = upperValue/limitSize;
      lowerValue = lowerValue/limitSize;

      double diffUpper = Math.abs((upperValue - aJointValue) * (upperValue - aJointValue));
      double diffLower = Math.abs((aJointValue - lowerValue) * (aJointValue - lowerValue));

      if (diffUpper > diffLower)
         jointLimitScore = diffLower;
      else
         jointLimitScore = diffUpper;

      if (DEBUG)
         PrintTools.info(""+jointName+" " + jointLimitScore + " " + aJointValue + " " + upperValue + " " + lowerValue);

      return jointLimitScore;
   }

   public void printOutRobotModel(FullHumanoidRobotModel printFullRobotModel, ReferenceFrame frame)
   {
      HumanoidReferenceFrames currentReferenceFrames = new HumanoidReferenceFrames(printFullRobotModel);
      currentReferenceFrames.updateFrames();

      PrintTools.info("root Joint");
      System.out.println(rootJoint.getTranslationForReading());
      System.out.println(rootJoint.getRotationForReading());

      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         double jointPosition = printFullRobotModel.getOneDoFJoints()[i].getQ();
         System.out.println(printFullRobotModel.getOneDoFJoints()[i].getName() + " " + jointPosition);
      }

      for (RobotSide robotSide : RobotSide.values)
      {
         ReferenceFrame desiredHandReferenceFrame = printFullRobotModel.getHand(robotSide).getBodyFixedFrame();
         FramePose desiredHandFramePose = new FramePose(desiredHandReferenceFrame);

         desiredHandFramePose.changeFrame(frame);
         PrintTools.info("" + robotSide + " Hand");
         System.out.println(desiredHandFramePose);
      }

      for (RobotSide robotSide : RobotSide.values)
      {
         ReferenceFrame desiredFootReferenceFrame = printFullRobotModel.getFoot(robotSide).getBodyFixedFrame();
         FramePose desiredFootFramePose = new FramePose(desiredFootReferenceFrame);

         desiredFootFramePose.changeFrame(frame);
         PrintTools.info("" + robotSide + " Foot");
         System.out.println(desiredFootFramePose);
      }

      ReferenceFrame desiredPelvisReferenceFrame = printFullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint();
      FramePose desiredPelvisFramePose = new FramePose(desiredPelvisReferenceFrame);
      desiredPelvisFramePose.changeFrame(frame);
      PrintTools.info("Pelvis");
      System.out.println(desiredPelvisFramePose);

      ReferenceFrame desiredChestReferenceFrame = printFullRobotModel.getChest().getBodyFixedFrame();
      FrameOrientation desiredChestFrameOrientation = new FrameOrientation(desiredChestReferenceFrame);
      desiredChestFrameOrientation.changeFrame(frame);
      PrintTools.info("Chest");
      System.out.println(desiredChestFrameOrientation);
   }

}
