package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Random;

import javax.vecmath.AxisAngle4d;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;


import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.manipulation.HandPosePacket.Frame;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.midLevel.RotateHandAboutAxisBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.HandPoseBehavior;
import us.ihmc.humanoidBehaviors.communication.BehaviorCommunicationBridge;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.TransformReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.time.GlobalTimer;

public abstract class DRCRotateHandAboutAxisBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      GlobalTimer.clearTimers();

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCChestOrientationBehaviorTest.class + " after class.");
   }

   private DRCBehaviorTestHelper drcBehaviorTestHelper;

   private ArmJointName[] armJointNames;
   private int numberOfArmJoints;
   private LinkedHashMap<ArmJointName, Integer> armJointIndices = new LinkedHashMap<ArmJointName, Integer>();

   private static final boolean DEBUG = true;

   @Before
   public void setUp()
   {
      if (NetworkPorts.USE_BEHAVIORS_MODULE)
      {
         throw new RuntimeException("Must set NetworkConfigParameters.USE_BEHAVIORS_MODULE = false in order to perform this test!");
      }
      
      drcBehaviorTestHelper = new DRCBehaviorTestHelper(new DRCDemo01NavigationEnvironment(), getSimpleRobotName(), null,
            DRCObstacleCourseStartingLocation.DEFAULT, simulationTestingParameters, getRobotModel());

      this.armJointNames = drcBehaviorTestHelper.getSDFFullRobotModel().getRobotSpecificJointNames().getArmJointNames();
      this.numberOfArmJoints = armJointNames.length;
      for (int i = 0; i < numberOfArmJoints; i++)
      {
         armJointIndices.put(armJointNames[i], i);
      }
   }

   @EstimatedDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testRotateSingleArmJointAndUnrotateInTaskSpace() throws FileNotFoundException,
         SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      PrintTools.debug(this, "Initializing Simulation");
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      RobotSide robotSide = RobotSide.LEFT;
      double trajectoryTime = 1.0;
      int randomArmJointIndex = RandomTools.generateRandomInt(new Random(), 0, numberOfArmJoints - 1);
      ArmJointName armJointToRotateName = armJointNames[randomArmJointIndex];
     
      ReferenceFrame armJointFrame = drcBehaviorTestHelper.getReferenceFrames().getArmFrame(robotSide, armJointToRotateName);

      FramePose initialHandPose = getCurrentHandPose(robotSide);
      double q_armJointInitial = getCurrentArmJointAngle(robotSide, armJointToRotateName);
      double q_armJointDesired = q_armJointInitial + Math.toRadians(90.0);
      q_armJointDesired = clipDesiredToJointLimits(robotSide, armJointToRotateName, q_armJointDesired);
      double desiredRotationAngle = q_armJointDesired - q_armJointInitial;
      
//      rotateSingleArmJoint(robotSide, armJointToRotateName, q_armJointDesired, trajectoryTime);
//      moveHandInTaskSpaceAlongLine(robotSide, initialHandPose, trajectoryTime);

      TransformReferenceFrame armJointFrameInitial = new TransformReferenceFrame("beforeRotationStaticFrame", ReferenceFrame.getWorldFrame(),
            armJointFrame.getTransformToWorldFrame());
      rotateSingleArmJoint(robotSide, armJointToRotateName, q_armJointDesired, trajectoryTime);
      
      assertEquals("Arm Joint Should Have Rotated " + Math.toDegrees(desiredRotationAngle) + " Degrees!", Math.abs(desiredRotationAngle),
            initialHandPose.getOrientationDistance(getCurrentHandPose(robotSide)), Math.toRadians(1.0));
      
      Axis rotationAxis = getAxisOfRotation(armJointFrameInitial, armJointFrame);

      RotateHandAboutAxisBehavior taskSpaceBehavior = moveHandInTaskSpaceAboutAxis(robotSide, rotationAxis, armJointFrame, -desiredRotationAngle, trajectoryTime);

      assertTrue(taskSpaceBehavior.isDone());
      assertTrue(success);
      assertPosesAreWithinThresholds(initialHandPose, getCurrentHandPose(robotSide), 0.05, Math.toRadians(1.0));

      BambooTools.reportTestFinishedMessage();
   }

   private HandPoseBehavior rotateSingleArmJoint(RobotSide robotSide, ArmJointName armJointToRotateName, double q_armJointDesired, double trajectoryTime) throws SimulationExceededMaximumTimeException
   {
      HandPoseBehavior jointSpaceBehavior = createJointSpaceHandPoseBehavior(robotSide, armJointToRotateName, q_armJointDesired, trajectoryTime);
      
      PrintTools.debug(this,"Rotating " + armJointToRotateName + " to desired position: " + q_armJointDesired);
      drcBehaviorTestHelper.executeBehaviorUntilDone(jointSpaceBehavior);
      PrintTools.debug(this,"Done Rotating");
      
      return jointSpaceBehavior;
   }
      
   private RotateHandAboutAxisBehavior moveHandInTaskSpaceAboutAxis(RobotSide robotSide, Axis rotationAxis, ReferenceFrame rotationAxisFrame, double rotationAngle, double trajectoryTime) throws SimulationExceededMaximumTimeException
   {
      final RotateHandAboutAxisBehavior rotateAboutAxisBehavior = createRotateHandBehavior(robotSide, false);
      rotateAboutAxisBehavior.initialize();

      double rotationRate = Math.abs(rotationAngle) / trajectoryTime;
      boolean stopHandIfCollision = false;
      rotateAboutAxisBehavior.setInput(robotSide, getCurrentHandPose(robotSide), rotationAxis, rotationAxisFrame.getTransformToWorldFrame(),
            rotationAngle, rotationRate, stopHandIfCollision);

      PrintTools.debug(this,"Moving Hand Along Circular Trajectory");
      boolean success = drcBehaviorTestHelper.executeBehaviorUntilDone(rotateAboutAxisBehavior);
      assertTrue(success);
      PrintTools.debug(this,"Un-Rotating Should Be Done");
      
      return rotateAboutAxisBehavior;
   }

   private void moveHandInTaskSpaceAlongLine(RobotSide robotSide, FramePose desiredPose, double trajectoryTime) throws SimulationExceededMaximumTimeException
   {
      final HandPoseBehavior handPoseBehavior = new HandPoseBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
            drcBehaviorTestHelper.getYoTime());
      
      handPoseBehavior.initialize();
      RigidBodyTransform worldToDesiredHandPoseTransform = new RigidBodyTransform();
      desiredPose.getPose(worldToDesiredHandPoseTransform);
      handPoseBehavior.setInput(Frame.WORLD, worldToDesiredHandPoseTransform, robotSide, trajectoryTime);

      PrintTools.debug(this,"Un-Rotating ArmJoint Using Straight-Line Trajectory");
      boolean success = drcBehaviorTestHelper.executeBehaviorUntilDone(handPoseBehavior);
      assertTrue(success);
      PrintTools.debug(this,"Un-Rotating Should Be Done");

      assertTrue(handPoseBehavior.isDone());
      FramePose handPoseAfterTaskSpaceUnRotation = getCurrentHandPose(robotSide);
      assertPosesAreWithinThresholds(desiredPose, handPoseAfterTaskSpaceUnRotation, 0.05, Math.toRadians(1.0));
   }
   
   private HandPoseBehavior createJointSpaceHandPoseBehavior(RobotSide robotSide, ArmJointName armJointToRotateName, double q_armJointDesired, double trajectoryTime)
   {
      final HandPoseBehavior jointSpaceBehavior = new HandPoseBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
            drcBehaviorTestHelper.getYoTime());
      
      jointSpaceBehavior.initialize();
      double[] desiredJointAngles = getCurrentArmPose(robotSide);
      setDesiredAngleOfSingleArmJoint(desiredJointAngles, robotSide, armJointToRotateName, q_armJointDesired, false);
      HandPosePacket handPosePacket = new HandPosePacket(robotSide, trajectoryTime, desiredJointAngles);
      jointSpaceBehavior.setInput(handPosePacket);
      
      return jointSpaceBehavior;
   }
   
   private RotateHandAboutAxisBehavior createRotateHandBehavior(RobotSide robotSide, boolean useWholeBodyInverseKinematics)
   {
      BehaviorCommunicationBridge communicationBridge = drcBehaviorTestHelper.getBehaviorCommunicationBridge();
      DoubleYoVariable yoTime = drcBehaviorTestHelper.getYoTime();

      RotateHandAboutAxisBehavior ret = new RotateHandAboutAxisBehavior(robotSide.getLowerCaseName(), communicationBridge,
            drcBehaviorTestHelper.getSDFFullRobotModel(), yoTime);
      return ret;
   }

   private FramePose getCurrentHandPose(RobotSide robotSideToTest)
   {
      FramePose ret = new FramePose();
      drcBehaviorTestHelper.updateRobotModel();
      ret.setToZero(drcBehaviorTestHelper.getSDFFullRobotModel().getHandControlFrame(robotSideToTest));
      ret.changeFrame(ReferenceFrame.getWorldFrame());
      return ret;
   }
   
   private Axis getAxisOfRotation(TransformReferenceFrame frameBeforeRotation, ReferenceFrame frameAfterRotation)
   {
      RigidBodyTransform beforeToAfterRotationTransform = frameBeforeRotation.getTransformToDesiredFrame(frameAfterRotation);
      AxisAngle4d beforeToAfterRotationAxisAngle = new AxisAngle4d();
      beforeToAfterRotationTransform.getRotation(beforeToAfterRotationAxisAngle);

      double rotationAngle = beforeToAfterRotationAxisAngle.angle;
      Axis rotationAxis = null;
      if (Math.abs(Math.round(beforeToAfterRotationAxisAngle.x)) == 1.0)
      {
         rotationAxis = Axis.X;
         rotationAngle *= Math.signum(beforeToAfterRotationAxisAngle.x);
      }
      if (Math.abs(Math.round(beforeToAfterRotationAxisAngle.y)) == 1.0)
      {
         rotationAxis = Axis.Y;
         rotationAngle *= Math.signum(beforeToAfterRotationAxisAngle.y);
      }
      if (Math.abs(Math.round(beforeToAfterRotationAxisAngle.z)) == 1.0)
      {
         rotationAxis = Axis.Z;
         rotationAngle *= Math.signum(beforeToAfterRotationAxisAngle.z);
      }

      if (DEBUG)
      {
         PrintTools.debug(this,"beforeToAfterRotationAxisAngle: " + beforeToAfterRotationAxisAngle);
         PrintTools.debug(this,"Rotation Axis: " + rotationAxis);
         PrintTools.debug(this,"Rotation Angle: " + rotationAngle);
      }
      
      return rotationAxis;
   }

   private void switchDirectionOfRotationIfNecessaryToAvoidJointLimits(RobotSide robotSide, ArmJointName armJointName, double q_initial, double q_desired)
   {
      OneDoFJoint joint = drcBehaviorTestHelper.getSDFFullRobotModel().getArmJoint(robotSide, armJointName);

      double initialProximityToLowerLimit = Math.abs(q_initial - joint.getJointLimitLower());
      double initialProximityToUpperLimit = Math.abs(q_initial - joint.getJointLimitUpper());

      double turnAngleRad = q_desired - q_initial;

      if (initialProximityToLowerLimit < Math.abs(turnAngleRad))
      {
         q_desired = q_initial + Math.abs(turnAngleRad);
      }
      else if (initialProximityToUpperLimit < Math.abs(turnAngleRad))
      {
         q_desired = q_initial - Math.abs(turnAngleRad);
      }
   }

   private void printArmJointAngles(double[] armJointAngles)
   {
      for (int i = 0; i < numberOfArmJoints; i++)
      {
         PrintTools.debug(this,"desired q_" + armJointNames[i] + " : " + armJointAngles[i]);
      }
   }
   
   private double[] createRandomArmPose(RobotSide robotSide)
   {
      double[] armPose = new double[numberOfArmJoints];

      for (int jointNum = 0; jointNum < numberOfArmJoints; jointNum++)
      {
         double qDesired = clipDesiredToJointLimits(robotSide, armJointNames[jointNum], RandomTools.generateRandomDouble(new Random(), 1.5));
         armPose[jointNum] = qDesired;
      }

      return armPose;
   }

   private void setDesiredAngleOfSingleArmJoint(double[] armPoseToPack, RobotSide robotSide, ArmJointName armJointName, double desiredJointAngle,
         boolean clipDesiredQToJointLimits)
   {
      if (clipDesiredQToJointLimits)
         desiredJointAngle = clipDesiredToJointLimits(robotSide, armJointName, desiredJointAngle);

      armPoseToPack[armJointIndices.get(armJointName)] = desiredJointAngle;
   }

   private double clipDesiredToJointLimits(RobotSide robotSide, ArmJointName armJointName, double qDesired)
   {
      FullRobotModel fullRobotModel = drcBehaviorTestHelper.getSDFFullRobotModel();
      double qMin = fullRobotModel.getArmJoint(robotSide, armJointName).getJointLimitLower();
      double qMax = fullRobotModel.getArmJoint(robotSide, armJointName).getJointLimitUpper();

      if (qMin > qMax)
      {
         double temp = qMax;
         qMax = qMin;
         qMin = temp;
      }
      return MathTools.clipToMinMax(qDesired, qMin, qMax);
   }

   private double[] getCurrentArmPose(RobotSide robotSide)
   {
      double[] armPose = new double[numberOfArmJoints];

      for (int jointNum = 0; jointNum < numberOfArmJoints; jointNum++)
      {
         armPose[jointNum] = getCurrentArmJointAngle(robotSide, armJointNames[jointNum]);
      }
      return armPose;
   }

   private double getCurrentArmJointAngle(RobotSide robotSide, ArmJointName armJointName)
   {
      return drcBehaviorTestHelper.getSDFFullRobotModel().getArmJoint(robotSide, armJointName).getQ();
   }

   private void assertPosesAreWithinThresholds(FramePose desiredPose, FramePose actualPose, double positionThreshold, double orientationThreshold)
   {
      double positionDistance = desiredPose.getPositionDistance(actualPose);
      double orientationDistance = desiredPose.getOrientationDistance(actualPose);

      if (DEBUG)
      {
         System.out.println("positionDistance=" + positionDistance);
         System.out.println("orientationDistance=" + orientationDistance);
      }
      assertEquals("Pose position error :" + positionDistance + " exceeds threshold: " + positionThreshold, 0.0, positionDistance, positionThreshold);
      assertEquals("Pose orientation error :" + orientationDistance + " exceeds threshold: " + orientationThreshold, 0.0, orientationDistance,
            orientationThreshold);
   }

}
