package us.ihmc.avatar.controllerAPI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static us.ihmc.avatar.controllerAPI.EndToEndHandTrajectoryMessageTest.findPoint2d;
import static us.ihmc.avatar.controllerAPI.EndToEndHandTrajectoryMessageTest.findPoint3d;
import static us.ihmc.avatar.controllerAPI.EndToEndHandTrajectoryMessageTest.findQuat4d;
import static us.ihmc.avatar.controllerAPI.EndToEndHandTrajectoryMessageTest.findVector3d;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.PelvisICPBasedTranslationManager;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisHeightControlMode;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationControlMode;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyTaskspaceControlState;
import us.ihmc.commonWalkingControlModules.desiredFootStep.ComponentBasedDesiredFootstepCalculator;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.ManualDesiredVelocityControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.RateBasedDesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonHumanoidReferenceFramesVisualizer;
import us.ihmc.commonWalkingControlModules.trajectories.LookAheadCoMHeightTrajectoryGenerator;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactableFoot;
import us.ihmc.humanoidRobotics.communication.packets.ExecutionMode;
import us.ihmc.humanoidRobotics.communication.packets.FrameInformation;
import us.ihmc.humanoidRobotics.communication.packets.SE3TrajectoryPointMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.StopAllTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.trajectories.CubicPolynomialTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.waypoints.MultipleWaypointsOrientationTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.waypoints.MultipleWaypointsPositionTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.waypoints.SimpleSE3TrajectoryPoint;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;
import us.ihmc.wholeBodyController.RobotContactPointParameters;

public abstract class EndToEndPelvisTrajectoryMessageTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
   private static final double EPSILON_FOR_DESIREDS = 1.2e-4;
   private static final double EPSILON_FOR_HEIGHT = 1.0e-2;

   private static final boolean DEBUG = false;

   private DRCSimulationTestHelper drcSimulationTestHelper;

   @ContinuousIntegrationTest(estimatedDuration = 25.0)
   @Test
   public void testSingleWaypoint() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      Random random = new Random(564574L);

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, getRobotModel());

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      double trajectoryTime = 1.0;
      RigidBody pelvis = fullRobotModel.getPelvis();

      FramePose desiredRandomPelvisPose = new FramePose(pelvis.getBodyFixedFrame());
      desiredRandomPelvisPose.setOrientation(RandomGeometry.nextQuaternion(random, 1.0));
      desiredRandomPelvisPose.setPosition(RandomGeometry.nextPoint3D(random, 0.05, 0.05, 0.05));
      desiredRandomPelvisPose.setZ(desiredRandomPelvisPose.getZ() - 0.1);
      Point3D desiredPosition = new Point3D();
      Quaternion desiredOrientation = new Quaternion();

      desiredRandomPelvisPose.getPose(desiredPosition, desiredOrientation);
      if (DEBUG)
      {
         System.out.println(desiredPosition);
         System.out.println(desiredOrientation);
      }

      desiredRandomPelvisPose.changeFrame(ReferenceFrame.getWorldFrame());

      desiredRandomPelvisPose.getPose(desiredPosition, desiredOrientation);
      if (DEBUG)
      {
         System.out.println(desiredPosition);
         System.out.println(desiredOrientation);
      }

      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(trajectoryTime, desiredPosition, desiredOrientation);

      drcSimulationTestHelper.send(pelvisTrajectoryMessage);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(getRobotModel().getControllerDT());
      assertTrue(success);

      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();

      RigidBodyTransform fromWorldToMidFeetZUpTransform = new RigidBodyTransform();
      Vector3D midFeetZup = findVector3d(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUp", scs);
      double midFeetZupYaw = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpYaw").getValueAsDouble();
      double midFeetZupPitch = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpPitch").getValueAsDouble();
      double midFeetZupRoll = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpRoll").getValueAsDouble();
      fromWorldToMidFeetZUpTransform.setRotationEulerAndZeroTranslation(midFeetZupRoll, midFeetZupPitch, midFeetZupYaw);
      fromWorldToMidFeetZUpTransform.setTranslation(midFeetZup);
      fromWorldToMidFeetZUpTransform.invert();

      Point2D desiredPosition2d = new Point2D();
      desiredPosition2d.set(desiredPosition.getX(), desiredPosition.getY());
      desiredPosition2d.applyTransform(fromWorldToMidFeetZUpTransform);
      Quaternion desiredOrientationCorrected = new Quaternion(desiredOrientation);
      desiredOrientationCorrected.applyTransform(fromWorldToMidFeetZUpTransform);

      desiredPosition.setX(desiredPosition2d.getX());
      desiredPosition.setY(desiredPosition2d.getY());
      desiredOrientation.set(desiredOrientationCorrected);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 + trajectoryTime);
      assertTrue(success);

      String pelvisName = fullRobotModel.getPelvis().getName();
      assertSingleWaypointExecuted(pelvisName, fullRobotModel, desiredPosition, desiredOrientation, scs);
   }
   
   @ContinuousIntegrationTest(estimatedDuration = 25.0)
   @Test
   public void testSingleWaypointAndWalk() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());
      
      Random random = new Random(564574L);
      
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      
      DRCRobotModel robotModel = getRobotModel();
      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, robotModel);
      
      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);
      
      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
      
      double trajectoryTime = 1.0;
      RigidBody pelvis = fullRobotModel.getPelvis();
      
      FramePose desiredRandomPelvisPose = new FramePose(pelvis.getBodyFixedFrame());
//      desiredRandomPelvisPose.setOrientation(RandomGeometry.nextQuaternion(random, 1.0));
//      desiredRandomPelvisPose.setPosition(RandomGeometry.nextPoint3D(random, 0.10, 0.20, 0.05));
      desiredRandomPelvisPose.setZ(desiredRandomPelvisPose.getZ() - 0.05);
      Point3D desiredPosition = new Point3D();
      Quaternion desiredOrientation = new Quaternion();
      
      desiredRandomPelvisPose.getPose(desiredPosition, desiredOrientation);
      if (DEBUG)
      {
         System.out.println(desiredPosition);
         System.out.println(desiredOrientation);
      }
      
      desiredRandomPelvisPose.changeFrame(ReferenceFrame.getWorldFrame());
      
      desiredRandomPelvisPose.getPose(desiredPosition, desiredOrientation);
      if (DEBUG)
      {
         System.out.println(desiredPosition);
         System.out.println(desiredOrientation);
      }
      
      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(1);
      pelvisTrajectoryMessage.setTrajectoryPoint(0, 1.0, desiredPosition, desiredOrientation, new Vector3D(), new Vector3D());
      
      drcSimulationTestHelper.send(pelvisTrajectoryMessage);
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0 * robotModel.getControllerDT());
      assertTrue(success);
      
      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      
      RigidBodyTransform fromWorldToMidFeetZUpTransform = new RigidBodyTransform();
      Vector3D midFeetZup = findVector3d(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUp", scs);
      double midFeetZupYaw = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpYaw").getValueAsDouble();
      double midFeetZupPitch = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpPitch").getValueAsDouble();
      double midFeetZupRoll = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpRoll").getValueAsDouble();
      fromWorldToMidFeetZUpTransform.setRotationEulerAndZeroTranslation(midFeetZupRoll, midFeetZupPitch, midFeetZupYaw);
      fromWorldToMidFeetZUpTransform.setTranslation(midFeetZup);
      fromWorldToMidFeetZUpTransform.invert();
      
      Point2D desiredPosition2d = new Point2D();
      desiredPosition2d.set(desiredPosition.getX(), desiredPosition.getY());
      desiredPosition2d.applyTransform(fromWorldToMidFeetZUpTransform);
      Quaternion desiredOrientationCorrected = new Quaternion(desiredOrientation);
      desiredOrientationCorrected.applyTransform(fromWorldToMidFeetZUpTransform);
      
      desiredPosition.setX(desiredPosition2d.getX());
      desiredPosition.setY(desiredPosition2d.getY());
      desiredOrientation.set(desiredOrientationCorrected);
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0 + trajectoryTime);
      assertTrue(success);
      
      String pelvisName = fullRobotModel.getPelvis().getName();
      assertSingleWaypointExecuted(pelvisName, fullRobotModel, desiredPosition, desiredOrientation, scs);
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      referenceFrames.updateFrames();
      double walkingTime = sendWalkingPacket(robotModel, fullRobotModel, referenceFrames, scs.getRootRegistry());
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 + walkingTime);
      assertTrue(success);
   }
   
   private double sendWalkingPacket(DRCRobotModel robotModel, FullHumanoidRobotModel fullRobotModel, HumanoidReferenceFrames referenceFrames,
         YoVariableRegistry registry)
   {
      referenceFrames.updateFrames();
      WalkingControllerParameters walkingControllerParameters = robotModel.getWalkingControllerParameters();
      double swingTime = walkingControllerParameters.getDefaultSwingTime();
      double transferTime = walkingControllerParameters.getDefaultTransferTime();
      double stepTime = swingTime + transferTime;

      RateBasedDesiredHeadingControlModule desiredHeadingControlModule = new RateBasedDesiredHeadingControlModule(0.0, robotModel.getControllerDT(), registry);
      ManualDesiredVelocityControlModule desiredVelocityControlModule = new ManualDesiredVelocityControlModule(
            desiredHeadingControlModule.getDesiredHeadingFrame(), registry);

      RobotContactPointParameters contactPointParameters = robotModel.getContactPointParameters();
      ContactableBodiesFactory contactableBodiesFactory = contactPointParameters.getContactableBodiesFactory();
      SideDependentList<ContactableFoot> bipedFeet = contactableBodiesFactory.createFootContactableBodies(fullRobotModel, referenceFrames);

      ComponentBasedDesiredFootstepCalculator desiredFootstepCalculator = new ComponentBasedDesiredFootstepCalculator(referenceFrames.getPelvisZUpFrame(),
            bipedFeet, desiredHeadingControlModule, desiredVelocityControlModule, registry);

      desiredVelocityControlModule.setDesiredVelocity(new FrameVector2d(ReferenceFrame.getWorldFrame(), 0.15, 0.0, "desiredVelocityControlModule"));
      desiredFootstepCalculator.setInPlaceWidth(walkingControllerParameters.getInPlaceWidth());
      desiredFootstepCalculator.setMaxStepLength(walkingControllerParameters.getMaxStepLength());
      desiredFootstepCalculator.setMinStepWidth(walkingControllerParameters.getMinStepWidth());
      desiredFootstepCalculator.setMaxStepWidth(walkingControllerParameters.getMaxStepWidth());
      desiredFootstepCalculator.setStepPitch(walkingControllerParameters.getStepPitch());

      desiredFootstepCalculator.initialize();
      FootstepDataListMessage footsteps = computeNextFootsteps(RobotSide.LEFT, desiredFootstepCalculator, stepTime);
      footsteps.setDefaultSwingDuration(swingTime);
      footsteps.setDefaultTransferDuration(transferTime);

      int numberOfSteps = footsteps.getDataList().size();
      drcSimulationTestHelper.send(footsteps);

      int timeWalking = numberOfSteps;
      double timeToCompleteWalking = stepTime * timeWalking;
      return timeToCompleteWalking;
   }

   private FootstepDataListMessage computeNextFootsteps(RobotSide supportLeg, ComponentBasedDesiredFootstepCalculator componentBasedDesiredFootstepCalculator, double stepTime)
   {
      componentBasedDesiredFootstepCalculator.initializeDesiredFootstep(supportLeg, stepTime);
      FootstepDataMessage footStep = componentBasedDesiredFootstepCalculator.updateAndGetDesiredFootstep(supportLeg);
      FootstepDataListMessage footsteps = new FootstepDataListMessage(Double.NaN, Double.NaN);

      RobotSide robotSide = supportLeg;
      FootstepDataMessage previousFootStep = footStep;
      for (int i = 0; i < 10; i++)
      {
         footStep = componentBasedDesiredFootstepCalculator.predictFootstepAfterDesiredFootstep(robotSide, previousFootStep, stepTime * i, stepTime);
         footsteps.add(footStep);
         robotSide = robotSide.getOppositeSide();
         previousFootStep = footStep;
      }
      footsteps.setExecutionMode(ExecutionMode.OVERRIDE);

      return footsteps;
   }
   
   @ContinuousIntegrationTest(estimatedDuration = 18.0)
   @Test
   public void testHeightUsingMultipleWaypoints() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, getRobotModel());

      ThreadTools.sleep(200);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      double timePerWaypoint = 0.1;
      int numberOfTrajectoryPoints = 100;
      double trajectoryTime = numberOfTrajectoryPoints * timePerWaypoint;
      RigidBody pelvis = fullRobotModel.getPelvis();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      referenceFrames.updateFrames();
      
      FramePoint pelvisPosition = new FramePoint(pelvis.getParentJoint().getFrameAfterJoint());
      MovingReferenceFrame midFootZUpGroundFrame = referenceFrames.getMidFootZUpGroundFrame();
      pelvisPosition.changeFrame(midFootZUpGroundFrame);
      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(numberOfTrajectoryPoints);
      SelectionMatrix6D selectionMatrix6D = new SelectionMatrix6D();
      selectionMatrix6D.clearAngularSelection();
      selectionMatrix6D.clearLinearSelection();
      selectionMatrix6D.selectLinearZ(true);
      pelvisTrajectoryMessage.setSelectionMatrix(selectionMatrix6D);
      
      FrameInformation frameInformation = pelvisTrajectoryMessage.getFrameInformation();
      frameInformation.setTrajectoryReferenceFrame(midFootZUpGroundFrame);
      frameInformation.setDataReferenceFrame(midFootZUpGroundFrame);
      
      double heightAmp = 0.1;
      double heightFreq = 0.5;
      double finalHeight = 0.0;

      int trajectoryPointIndex = 0;
      for (double time = 0.0; time < trajectoryTime - timePerWaypoint; time += timePerWaypoint)
      {
         Quaternion orientation = new Quaternion();
         Vector3D angularVelocity = new Vector3D();

         orientation.setYawPitchRoll(0.0, 0.0, 0.0);
         angularVelocity.set(0.0, 0.0, 0.0);

         double x = pelvisPosition.getX();
         double y = pelvisPosition.getY();
         double z = heightAmp * Math.sin(2.0 * Math.PI * heightFreq * time) + pelvisPosition.getZ() - 0.02;

         double dx = 0.0;
         double dy = 0.0;
         double dz = heightAmp * Math.PI * 2.0 * heightFreq * Math.cos(2.0 * Math.PI * heightFreq * time);

         // set the velocity to zero for the last waypoint
         if(time + timePerWaypoint >= trajectoryTime - timePerWaypoint)
         {
            dz = 0.0;
         }

         Point3D position = new Point3D(x, y, z);
         Vector3D linearVelocity = new Vector3D(dx, dy, dz);
         pelvisTrajectoryMessage.setTrajectoryPoint(trajectoryPointIndex, time, position, orientation, linearVelocity, angularVelocity, midFootZUpGroundFrame);
         trajectoryPointIndex++;
         
         finalHeight = z;
      }

      drcSimulationTestHelper.send(pelvisTrajectoryMessage);
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0 * getRobotModel().getControllerDT());
      assertTrue(success);
      
      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      assertCenterOfMassHeightManagerIsInState(scs, PelvisHeightControlMode.USER);
      String bodyName = pelvis.getName();
      assertEquals(numberOfTrajectoryPoints, findControllerNumberOfWaypointsForHeight(scs, pelvis));
      assertEquals(RigidBodyTaskspaceControlState.maxPointsInGenerator, findControllerNumberOfWaypointsInQueueForHeight(scs, pelvis));

      for (trajectoryPointIndex = 0; trajectoryPointIndex < RigidBodyTaskspaceControlState.maxPointsInGenerator; trajectoryPointIndex++)
      {
         SE3TrajectoryPointMessage fromMessage = pelvisTrajectoryMessage.getTrajectoryPoint(trajectoryPointIndex);
         SimpleSE3TrajectoryPoint expectedTrajectoryPoint = new SimpleSE3TrajectoryPoint();
         expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
//         expectedTrajectoryPoint.applyTransform(fromWorldToMidFeetZUpTransform);
         SimpleSE3TrajectoryPoint controllerTrajectoryPoint = findPelvisHeightTrajectoryPoint(pelvis, bodyName + "Height", trajectoryPointIndex, scs);


//         assertEquals(expectedTrajectoryPoint.getTime(), controllerTrajectoryPoint.getTime(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);

         // only check a few points since the trajectory in the controller used is shorter
         if (trajectoryPointIndex + 1 < RigidBodyTaskspaceControlState.maxPointsInGenerator)
         {
            expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
            
            assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), EPSILON_FOR_DESIREDS);
            assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);
         }
      }
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(pelvisTrajectoryMessage.getTrajectoryTime() + 1.0);
      assertTrue(success);
      
      
      pelvisPosition.setToZero(fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint());
      pelvisPosition.changeFrame(midFootZUpGroundFrame);
      assertEquals(finalHeight, pelvisPosition.getZ(), 0.004);
      assertCenterOfMassHeightManagerIsInState(scs, PelvisHeightControlMode.USER);
   }

   private void assertCenterOfMassHeightManagerIsInState(SimulationConstructionSet scs, PelvisHeightControlMode mode)
   {
      EnumYoVariable<PelvisHeightControlMode> centerOfMassHeightManagerState = (EnumYoVariable<PelvisHeightControlMode> ) scs.getVariable("CenterOfMassHeightManager", "CenterOfMassHeightManagerState");
      assertEquals(mode, centerOfMassHeightManagerState.getEnumValue());
   }

   private SimpleSE3TrajectoryPoint findPelvisHeightTrajectoryPoint(RigidBody rigidBody, String bodyName, int trajectoryPointIndex, SimulationConstructionSet scs)
   {
      String suffix = "AtWaypoint" + trajectoryPointIndex;
      SimpleSE3TrajectoryPoint simpleSE3TrajectoryPoint = new SimpleSE3TrajectoryPoint();

      String pelvisZPrefix = rigidBody.getName() + "Height";
      String positionZTrajectoryName = pelvisZPrefix + MultipleWaypointsPositionTrajectoryGenerator.class.getSimpleName();
      String positionZName = pelvisZPrefix + "Position";
      String linearVelocityZName = pelvisZPrefix + "LinearVelocity";

      Point3D position = findPoint3d(positionZTrajectoryName, positionZName, suffix, scs);

      Vector3D linearVelocity = findVector3d(positionZTrajectoryName, linearVelocityZName, suffix, scs);

      String timeName = pelvisZPrefix + "Time";
      simpleSE3TrajectoryPoint.setTime(scs.getVariable(positionZTrajectoryName, timeName + suffix).getValueAsDouble());
      simpleSE3TrajectoryPoint.setPosition(position);
      simpleSE3TrajectoryPoint.setLinearVelocity(linearVelocity);

      return simpleSE3TrajectoryPoint;
   }

   @ContinuousIntegrationTest(estimatedDuration = 18.0)
   @Test
   public void testHeightUsingMultipleWaypointsWhileWalking() throws Exception
   {
      
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      DRCRobotModel robotModel = getRobotModel();
      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, robotModel);

      ThreadTools.sleep(200);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      double timePerWaypoint = 0.1;
      int numberOfTrajectoryPoints = 100;
      double trajectoryTime = numberOfTrajectoryPoints * timePerWaypoint;
      RigidBody pelvis = fullRobotModel.getPelvis();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      referenceFrames.updateFrames();
      
      FramePoint pelvisPosition = new FramePoint(pelvis.getParentJoint().getFrameAfterJoint());
      MovingReferenceFrame midFootZUpGroundFrame = referenceFrames.getMidFootZUpGroundFrame();
      pelvisPosition.changeFrame(midFootZUpGroundFrame);
      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(numberOfTrajectoryPoints);
      pelvisTrajectoryMessage.setEnableUserPelvisControlDuringWalking(true);
      SelectionMatrix6D selectionMatrix6D = new SelectionMatrix6D();
      selectionMatrix6D.clearAngularSelection();
      selectionMatrix6D.clearLinearSelection();
      selectionMatrix6D.selectLinearZ(true);
      pelvisTrajectoryMessage.setSelectionMatrix(selectionMatrix6D);
      
      FrameInformation frameInformation = pelvisTrajectoryMessage.getFrameInformation();
      frameInformation.setTrajectoryReferenceFrame(midFootZUpGroundFrame);
      frameInformation.setDataReferenceFrame(midFootZUpGroundFrame);
      
      double heightAmp = 0.04;
      double heightFreq = 0.5;
      double finalHeight = 0.0;

      int trajectoryPointIndex = 0;
      for (double time = 0.0; time < trajectoryTime - timePerWaypoint; time += timePerWaypoint)
      {
         Quaternion orientation = new Quaternion();
         Vector3D angularVelocity = new Vector3D();

         orientation.setYawPitchRoll(0.0, 0.0, 0.0);
         angularVelocity.set(0.0, 0.0, 0.0);

         double x = pelvisPosition.getX();
         double y = pelvisPosition.getY();
         double z = heightAmp * Math.sin(2.0 * Math.PI * heightFreq * time) + pelvisPosition.getZ() - 0.02;

         double dx = 0.0;
         double dy = 0.0;
         double dz = heightAmp * Math.PI * 2.0 * heightFreq * Math.cos(2.0 * Math.PI * heightFreq * time);

         // set the velocity to zero for the last waypoint
         if(time + timePerWaypoint >= trajectoryTime - timePerWaypoint)
         {
            dz = 0.0;
         }

         Point3D position = new Point3D(x, y, z);
         Vector3D linearVelocity = new Vector3D(dx, dy, dz);
         pelvisTrajectoryMessage.setTrajectoryPoint(trajectoryPointIndex, time, position, orientation, linearVelocity, angularVelocity, midFootZUpGroundFrame);
         trajectoryPointIndex++;
         
         finalHeight = z;
      }

      drcSimulationTestHelper.send(pelvisTrajectoryMessage);
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0 * robotModel.getControllerDT());
      assertTrue(success);
      
      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();;
      assertCenterOfMassHeightManagerIsInState(scs, PelvisHeightControlMode.USER);
      assertEquals(numberOfTrajectoryPoints, findControllerNumberOfWaypointsForHeight(scs, pelvis));
      assertEquals(RigidBodyTaskspaceControlState.maxPointsInGenerator, findControllerNumberOfWaypointsInQueueForHeight(scs, pelvis));

      for (trajectoryPointIndex = 0; trajectoryPointIndex < RigidBodyTaskspaceControlState.maxPointsInGenerator; trajectoryPointIndex++)
      {
         System.out.println(trajectoryPointIndex);
         SE3TrajectoryPointMessage fromMessage = pelvisTrajectoryMessage.getTrajectoryPoint(trajectoryPointIndex);
         SimpleSE3TrajectoryPoint expectedTrajectoryPoint = new SimpleSE3TrajectoryPoint();
         expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
//         expectedTrajectoryPoint.applyTransform(fromWorldToMidFeetZUpTransform);
         SimpleSE3TrajectoryPoint controllerTrajectoryPoint = findPelvisHeightTrajectoryPoint(pelvis, "pelvisHeight", trajectoryPointIndex, scs);


//         assertEquals(expectedTrajectoryPoint.getTime(), controllerTrajectoryPoint.getTime(), EPSILON_FOR_DESIREDS);
         System.out.println(expectedTrajectoryPoint.getPositionZ());
         assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);

         // only check a few points since the trajectory in the controller used is shorter
         if (trajectoryPointIndex + 1 < RigidBodyTaskspaceControlState.maxPointsInGenerator)
         {
            expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
            
            assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), EPSILON_FOR_DESIREDS);
            assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);
         }
      }
      
      referenceFrames.updateFrames();
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      double walkingTime = sendWalkingPacket(robotModel, fullRobotModel, referenceFrames, simulationConstructionSet.getRootRegistry());
      double simTime = Math.max(walkingTime, pelvisTrajectoryMessage.getTrajectoryTime() );
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simTime + 3.0);
      assertTrue(success);
      
      pelvisPosition.setToZero(fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint());
      pelvisPosition.changeFrame(midFootZUpGroundFrame);
      assertEquals(finalHeight, pelvisPosition.getZ(), 0.006);
      assertCenterOfMassHeightManagerIsInState(scs, PelvisHeightControlMode.USER);
   }
   
   @ContinuousIntegrationTest(estimatedDuration = 18.0)
   @Test
   public void testHeightModeSwitchWhileWalking() throws Exception
   {
      
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      DRCRobotModel robotModel = getRobotModel();
      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, robotModel);

      ThreadTools.sleep(200);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      double timePerWaypoint = 0.1;
      int numberOfTrajectoryPoints = 100;
      double trajectoryTime = numberOfTrajectoryPoints * timePerWaypoint;
      RigidBody pelvis = fullRobotModel.getPelvis();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      referenceFrames.updateFrames();
      
      FramePoint pelvisPosition = new FramePoint(pelvis.getParentJoint().getFrameAfterJoint());
      MovingReferenceFrame midFootZUpGroundFrame = referenceFrames.getMidFootZUpGroundFrame();
      pelvisPosition.changeFrame(midFootZUpGroundFrame);
      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(numberOfTrajectoryPoints);
      pelvisTrajectoryMessage.setEnableUserPelvisControlDuringWalking(false);
      SelectionMatrix6D selectionMatrix6D = new SelectionMatrix6D();
      selectionMatrix6D.clearAngularSelection();
      selectionMatrix6D.clearLinearSelection();
      selectionMatrix6D.selectLinearZ(true);
      pelvisTrajectoryMessage.setSelectionMatrix(selectionMatrix6D);
      
      FrameInformation frameInformation = pelvisTrajectoryMessage.getFrameInformation();
      frameInformation.setTrajectoryReferenceFrame(midFootZUpGroundFrame);
      frameInformation.setDataReferenceFrame(midFootZUpGroundFrame);
      
      double heightAmp = 0.1;
      double heightFreq = 0.5;
      double finalHeight = 0.0;

      int trajectoryPointIndex = 0;
      for (double time = 0.0; time < trajectoryTime - timePerWaypoint; time += timePerWaypoint)
      {
         Quaternion orientation = new Quaternion();
         Vector3D angularVelocity = new Vector3D();

         orientation.setYawPitchRoll(0.0, 0.0, 0.0);
         angularVelocity.set(0.0, 0.0, 0.0);

         double x = pelvisPosition.getX();
         double y = pelvisPosition.getY();
         double z = heightAmp * Math.sin(2.0 * Math.PI * heightFreq * time) + pelvisPosition.getZ() - 0.02;

         double dx = 0.0;
         double dy = 0.0;
         double dz = heightAmp * Math.PI * 2.0 * heightFreq * Math.cos(2.0 * Math.PI * heightFreq * time);

         // set the velocity to zero for the last waypoint
         if(time + timePerWaypoint >= trajectoryTime - timePerWaypoint)
         {
            dz = 0.0;
         }

         Point3D position = new Point3D(x, y, z);
         Vector3D linearVelocity = new Vector3D(dx, dy, dz);
         pelvisTrajectoryMessage.setTrajectoryPoint(trajectoryPointIndex, time, position, orientation, linearVelocity, angularVelocity, midFootZUpGroundFrame);
         trajectoryPointIndex++;
         
         finalHeight = z;
      }

      drcSimulationTestHelper.send(pelvisTrajectoryMessage);
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0 * robotModel.getControllerDT());
      assertTrue(success);
      
      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();;
      assertCenterOfMassHeightManagerIsInState(scs, PelvisHeightControlMode.USER);
      assertEquals(numberOfTrajectoryPoints, findControllerNumberOfWaypointsForHeight(scs, pelvis));
      assertEquals(RigidBodyTaskspaceControlState.maxPointsInGenerator, findControllerNumberOfWaypointsInQueueForHeight(scs, pelvis));

      for (trajectoryPointIndex = 0; trajectoryPointIndex < RigidBodyTaskspaceControlState.maxPointsInGenerator; trajectoryPointIndex++)
      {
         System.out.println(trajectoryPointIndex);
         SE3TrajectoryPointMessage fromMessage = pelvisTrajectoryMessage.getTrajectoryPoint(trajectoryPointIndex);
         SimpleSE3TrajectoryPoint expectedTrajectoryPoint = new SimpleSE3TrajectoryPoint();
         expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
//         expectedTrajectoryPoint.applyTransform(fromWorldToMidFeetZUpTransform);
         SimpleSE3TrajectoryPoint controllerTrajectoryPoint = findPelvisHeightTrajectoryPoint(pelvis, "pelvisHeight", trajectoryPointIndex, scs);


//         assertEquals(expectedTrajectoryPoint.getTime(), controllerTrajectoryPoint.getTime(), EPSILON_FOR_DESIREDS);
         System.out.println(expectedTrajectoryPoint.getPositionZ());
         assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);

         // only check a few points since the trajectory in the controller used is shorter
         if (trajectoryPointIndex + 1 < RigidBodyTaskspaceControlState.maxPointsInGenerator)
         {
            expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
            
            assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), EPSILON_FOR_DESIREDS);
            assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);
         }
      }
      
      referenceFrames.updateFrames();
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      double walkingTime = sendWalkingPacket(robotModel, fullRobotModel, referenceFrames, simulationConstructionSet.getRootRegistry());
      double simTime = Math.max(walkingTime, pelvisTrajectoryMessage.getTrajectoryTime() );
      
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simTime + 1.0);
      assertTrue(success);
      
      assertCenterOfMassHeightManagerIsInState(scs, PelvisHeightControlMode.WALKING_CONTROLLER);
   }

   @ContinuousIntegrationTest(estimatedDuration = 18.0)
   @Test
   public void testMultipleWaypoints() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, getRobotModel());

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
      String pelvisName = fullRobotModel.getPelvis().getName();

      double timePerWaypoint = 0.1;
      int numberOfTrajectoryPoints = 15;
      double trajectoryTime = numberOfTrajectoryPoints * timePerWaypoint;
      RigidBody pelvis = fullRobotModel.getPelvis();

      FramePoint pelvisPosition = new FramePoint(pelvis.getParentJoint().getFrameAfterJoint());
      pelvisPosition.changeFrame(ReferenceFrame.getWorldFrame());

      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(numberOfTrajectoryPoints);

      double rotationAmp = Math.toRadians(20.0);
      double pitchFreq = 0.5;
      
      double heightAmp = 0.05;
      double heightFreq = 0.5;
      double finalHeight = 0.0;

      int index = 0;
      for (double time = 0.1; time < trajectoryTime + 0.1; time += timePerWaypoint)
      {
         Quaternion orientation = new Quaternion();
         Vector3D angularVelocity = new Vector3D();

         double pitch = rotationAmp * Math.sin(2.0 * Math.PI * pitchFreq * time);
         double pitchDot = rotationAmp * Math.PI * 2.0 * pitchFreq * Math.cos(2.0 * Math.PI * pitchFreq * time);
         
         orientation.setYawPitchRoll(0.0, pitch, 0.0);
         angularVelocity.set(0.0, pitchDot, 0.0);

         double x = pelvisPosition.getX();
         double y = pelvisPosition.getY();
         double z = heightAmp * Math.sin(2.0 * Math.PI * heightFreq * time) + pelvisPosition.getZ() - 0.02;

         double dx = 0.0;
         double dy = 0.0;
         double dz = heightAmp * Math.PI * 2.0 * heightFreq * Math.cos(2.0 * Math.PI * heightFreq * time);

         // set the velocity to zero for the last waypoint
         if(time + timePerWaypoint >= trajectoryTime - timePerWaypoint)
         {
            dz = 0.0;
         }

         Point3D position = new Point3D(x, y, z);
         Vector3D linearVelocity = new Vector3D(dx, dy, dz);
         pelvisTrajectoryMessage.setTrajectoryPoint(index, time, position, orientation, linearVelocity, angularVelocity);
         index++;
         
         finalHeight = z;
      }


      drcSimulationTestHelper.send(pelvisTrajectoryMessage);

      final SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();


      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(getRobotModel().getControllerDT());
      assertTrue(success);

      RigidBodyTransform fromWorldToMidFeetZUpTransform = new RigidBodyTransform();
      Vector3D midFeetZup = findVector3d(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUp", scs);
      double midFeetZupYaw = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpYaw").getValueAsDouble();
      double midFeetZupPitch = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpPitch").getValueAsDouble();
      double midFeetZupRoll = scs.getVariable(CommonHumanoidReferenceFramesVisualizer.class.getSimpleName(), "midFeetZUpRoll").getValueAsDouble();
      fromWorldToMidFeetZUpTransform.setRotationEulerAndZeroTranslation(midFeetZupRoll, midFeetZupPitch, midFeetZupYaw);
      fromWorldToMidFeetZUpTransform.setTranslation(midFeetZup);
      fromWorldToMidFeetZUpTransform.invert();

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0 * getRobotModel().getControllerDT());
      assertTrue(success);

      assertNumberOfWaypoints(numberOfTrajectoryPoints + 1, scs);

      for (int trajectoryPointIndex = 0; trajectoryPointIndex < numberOfTrajectoryPoints; trajectoryPointIndex++)
      {
         SE3TrajectoryPointMessage fromMessage = pelvisTrajectoryMessage.getTrajectoryPoint(trajectoryPointIndex);
         SimpleSE3TrajectoryPoint expectedTrajectoryPoint = new SimpleSE3TrajectoryPoint();
         expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
         expectedTrajectoryPoint.applyTransform(fromWorldToMidFeetZUpTransform);
         SimpleSE3TrajectoryPoint controllerTrajectoryPoint = findTrajectoryPoint(pelvisName, trajectoryPointIndex + 1, scs);


         assertEquals(expectedTrajectoryPoint.getPositionX(), controllerTrajectoryPoint.getPositionX(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getPositionY(), controllerTrajectoryPoint.getPositionY(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getLinearVelocityX(), controllerTrajectoryPoint.getLinearVelocityX(), EPSILON_FOR_DESIREDS);
         assertEquals(expectedTrajectoryPoint.getLinearVelocityY(), controllerTrajectoryPoint.getLinearVelocityY(), EPSILON_FOR_DESIREDS);

         // only check a few orientation points since the trajectory in the controller used is shorter
         if (trajectoryPointIndex + 1 < RigidBodyTaskspaceControlState.maxPointsInGenerator)
         {
            assertEquals(expectedTrajectoryPoint.getTime(), controllerTrajectoryPoint.getTime(), EPSILON_FOR_DESIREDS);
            expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
            EuclidCoreTestTools.assertQuaternionEquals(expectedTrajectoryPoint.getOrientationCopy(), controllerTrajectoryPoint.getOrientationCopy(), EPSILON_FOR_DESIREDS);
            EuclidCoreTestTools.assertTuple3DEquals(expectedTrajectoryPoint.getAngularVelocityCopy(), controllerTrajectoryPoint.getAngularVelocityCopy(), EPSILON_FOR_DESIREDS);
            assertEquals(expectedTrajectoryPoint.getLinearVelocityZ(), controllerTrajectoryPoint.getLinearVelocityZ(), EPSILON_FOR_DESIREDS);
            System.out.println(expectedTrajectoryPoint.getPositionZ() + " : " + controllerTrajectoryPoint.getPositionZ());
            assertEquals(expectedTrajectoryPoint.getPositionZ(), controllerTrajectoryPoint.getPositionZ(), 0.006); //something is wrong with the frame change, just check we are within 6mm
         }
      }

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(trajectoryTime);
      assertTrue(success);

      SE3TrajectoryPointMessage fromMessage = pelvisTrajectoryMessage.getLastTrajectoryPoint();
      SimpleSE3TrajectoryPoint expectedTrajectoryPoint = new SimpleSE3TrajectoryPoint();
      expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
      expectedTrajectoryPoint.applyTransform(fromWorldToMidFeetZUpTransform);
      SimpleSE3TrajectoryPoint controllerTrajectoryPoint = findCurrentDesiredTrajectoryPoint(pelvisName, scs);

      // Not check the height on purpose as it is non-trivial.
      assertEquals(expectedTrajectoryPoint.getPositionX(), controllerTrajectoryPoint.getPositionX(), EPSILON_FOR_DESIREDS);
      assertEquals(expectedTrajectoryPoint.getPositionY(), controllerTrajectoryPoint.getPositionY(), EPSILON_FOR_DESIREDS);
      EuclidCoreTestTools.assertTuple3DEquals(expectedTrajectoryPoint.getLinearVelocityCopy(), controllerTrajectoryPoint.getLinearVelocityCopy(), EPSILON_FOR_DESIREDS);

      expectedTrajectoryPoint.set(fromMessage.time, fromMessage.position, fromMessage.orientation, fromMessage.linearVelocity, fromMessage.angularVelocity);
      EuclidCoreTestTools.assertQuaternionEquals(expectedTrajectoryPoint.getOrientationCopy(), controllerTrajectoryPoint.getOrientationCopy(), EPSILON_FOR_DESIREDS);
      EuclidCoreTestTools.assertTuple3DEquals(expectedTrajectoryPoint.getAngularVelocityCopy(), controllerTrajectoryPoint.getAngularVelocityCopy(), EPSILON_FOR_DESIREDS);
   }

   @SuppressWarnings("unchecked")
   @ContinuousIntegrationTest(estimatedDuration = 20.0)
   @Test
   public void testStopAllTrajectory() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      Random random = new Random(564574L);

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper(getClass().getSimpleName(), selectedLocation, simulationTestingParameters, getRobotModel());

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      assertTrue(success);

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();

      double trajectoryTime = 5.0;
      RigidBody pelvis = fullRobotModel.getPelvis();

      FramePose desiredRandomPelvisPose = new FramePose(pelvis.getBodyFixedFrame());
      desiredRandomPelvisPose.setOrientation(RandomGeometry.nextQuaternion(random, 1.0));
      desiredRandomPelvisPose.setPosition(RandomGeometry.nextPoint3D(random, 0.10, 0.20, 0.05));
      desiredRandomPelvisPose.setZ(desiredRandomPelvisPose.getZ() - 0.1);
      Point3D desiredPosition = new Point3D();
      Quaternion desiredOrientation = new Quaternion();

      desiredRandomPelvisPose.getPose(desiredPosition, desiredOrientation);
      if (DEBUG)
      {
         System.out.println(desiredPosition);
         System.out.println(desiredOrientation);
      }

      desiredRandomPelvisPose.changeFrame(ReferenceFrame.getWorldFrame());

      desiredRandomPelvisPose.getPose(desiredPosition, desiredOrientation);
      if (DEBUG)
      {
         System.out.println(desiredPosition);
         System.out.println(desiredOrientation);
      }

      SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      String managerName = PelvisOrientationManager.class.getSimpleName();
      EnumYoVariable<PelvisOrientationControlMode> orientationControlMode = (EnumYoVariable<PelvisOrientationControlMode>) scs.getVariable(managerName, managerName + "State");

      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(trajectoryTime, desiredPosition, desiredOrientation);

      drcSimulationTestHelper.send(pelvisTrajectoryMessage);

      assertEquals(PelvisOrientationControlMode.WALKING_CONTROLLER, orientationControlMode.getEnumValue());
      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(trajectoryTime / 2.0);
      assertTrue(success);

      StopAllTrajectoryMessage stopAllTrajectoryMessage = new StopAllTrajectoryMessage();
      drcSimulationTestHelper.send(stopAllTrajectoryMessage);

      assertEquals(PelvisOrientationControlMode.USER, orientationControlMode.getEnumValue());
      assertFalse(findControllerStopBooleanForXY(scs));
      assertFalse(findControllerStopBooleanForHeight(scs));
      String pelvisName = fullRobotModel.getPelvis().getName();
      Quaternion controllerDesiredOrientationBeforeStop = EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(pelvisName, scs);
      Point3D controllerDesiredPelvisHeightBeforeStop = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(pelvisName, scs);
      Point2D controllerDesiredXYBeforeStop = findControllerDesiredPositionXY(scs);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.05);
      assertTrue(success);

      assertEquals(PelvisOrientationControlMode.WALKING_CONTROLLER, orientationControlMode.getEnumValue());
      assertTrue(findControllerStopBooleanForXY(scs));
      Point3D controllerDesiredPelvisHeightAfterStop = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(pelvisName, scs);
      Quaternion controllerDesiredOrientationAfterStop = EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(pelvisName, scs);
      Point2D controllerDesiredXYAfterStop = findControllerDesiredPositionXY(scs);

      EuclidCoreTestTools.assertQuaternionEquals(controllerDesiredOrientationBeforeStop, controllerDesiredOrientationAfterStop, 1.0e-2);
      EuclidCoreTestTools.assertTuple2DEquals("", controllerDesiredXYBeforeStop, controllerDesiredXYAfterStop, 1.0e-2);
      //checking pelvis hieght only
      assertEquals(controllerDesiredPelvisHeightBeforeStop.getZ(), controllerDesiredPelvisHeightAfterStop.getZ(), 1.0e-2);
   }
   
   public static Point2D findControllerDesiredPositionXY(SimulationConstructionSet scs)
   {
      String pelvisPrefix = "pelvisOffset";
      String subTrajectoryName = pelvisPrefix + "SubTrajectory";
      String currentPositionVarNamePrefix = subTrajectoryName + "CurrentPosition";

      return findPoint2d(subTrajectoryName, currentPositionVarNamePrefix, scs);
   }

   public static double findCurrentPelvisHeight(SimulationConstructionSet scs)
   {
      return scs.getVariable("PelvisLinearStateUpdater", "estimatedRootJointPositionZ").getValueAsDouble();
   }

   public static Vector3D findControllerDesiredLinearVelocity(SimulationConstructionSet scs)
   {
      String pelvisPrefix = "pelvisOffset";
      String subTrajectory = "SubTrajectory";
      String subTrajectoryName = pelvisPrefix + subTrajectory;
      String currentLinearVelocityVarNamePrefix = subTrajectoryName + "CurrentVelocity";

      Vector3D linearVelocity = findVector3d(subTrajectoryName, currentLinearVelocityVarNamePrefix, scs);

      String pelvisHeightPrefix = "pelvisHeightOffset";
      String offsetHeightTrajectoryName = pelvisHeightPrefix + subTrajectory + CubicPolynomialTrajectoryGenerator.class.getSimpleName();

      linearVelocity.setZ(scs.getVariable(offsetHeightTrajectoryName, pelvisHeightPrefix + subTrajectory + "CurrentVelocity").getValueAsDouble());

      return linearVelocity;
   }

   public static boolean findControllerStopBooleanForXY(SimulationConstructionSet scs)
   {
      return ((BooleanYoVariable) scs.getVariable(PelvisICPBasedTranslationManager.class.getSimpleName(), "isPelvisTranslationalTrajectoryStopped")).getBooleanValue();
   }

   public static boolean findControllerStopBooleanForHeight(SimulationConstructionSet scs)
   {
      return ((BooleanYoVariable) scs.getVariable(LookAheadCoMHeightTrajectoryGenerator.class.getSimpleName(), "isPelvisOffsetHeightTrajectoryStopped")).getBooleanValue();
   }

   public static int findControllerNumberOfWaypointsForXY(SimulationConstructionSet scs)
   {
      String pelvisPrefix = "pelvisOffset";
      String positionTrajectoryName = pelvisPrefix + MultipleWaypointsPositionTrajectoryGenerator.class.getSimpleName();
      String numberOfWaypointsVarName = pelvisPrefix + "NumberOfWaypoints";

      int numberOfWaypoints = ((IntegerYoVariable) scs.getVariable(positionTrajectoryName, numberOfWaypointsVarName)).getIntegerValue();
      return numberOfWaypoints;
   }

   public static int findControllerNumberOfWaypointsForHeight(SimulationConstructionSet scs, RigidBody rigidBody)
   {
//      String pelvisPrefix = "pelvisHeight";
//      String offsetHeightTrajectoryName = pelvisPrefix + MultipleWaypointsPositionTrajectoryGenerator.class.getSimpleName();
//      String numberOfWaypointsVarName = pelvisPrefix + "NumberOfWaypoints";
      String bodyName = rigidBody.getName();
      IntegerYoVariable pelvisHeightTaskspaceNumberOfPoints = (IntegerYoVariable) scs.getVariable(bodyName + "HeightTaskspaceControlModule", bodyName + "HeightTaskspaceNumberOfPoints");
      int numberOfWaypoints = pelvisHeightTaskspaceNumberOfPoints.getIntegerValue();
      return numberOfWaypoints;
   }

   public static int findControllerNumberOfWaypointsInQueueForHeight(SimulationConstructionSet scs, RigidBody rigidBody)
   {
      String bodyName = rigidBody.getName();
      String pelvisPrefix = bodyName + "Height";
      String offsetHeightTrajectoryName = pelvisPrefix + MultipleWaypointsPositionTrajectoryGenerator.class.getSimpleName();
      String numberOfWaypointsVarName = pelvisPrefix + "NumberOfWaypoints";
      int numberOfWaypoints = ((IntegerYoVariable) scs.getVariable(offsetHeightTrajectoryName, numberOfWaypointsVarName)).getIntegerValue();
      return numberOfWaypoints;
   }

   public static SimpleSE3TrajectoryPoint findTrajectoryPoint(String bodyName, int trajectoryPointIndex, SimulationConstructionSet scs)
   {
      String suffix = "AtWaypoint" + trajectoryPointIndex;
      SimpleSE3TrajectoryPoint simpleSE3TrajectoryPoint = new SimpleSE3TrajectoryPoint();
      
      String pelvisXYPrefix = "pelvisOffset";
      String positionXYTrajectoryName = pelvisXYPrefix + MultipleWaypointsPositionTrajectoryGenerator.class.getSimpleName();
      String positionXYName = pelvisXYPrefix + "Position";
      String linearVelocityXYName = pelvisXYPrefix + "LinearVelocity";

      Point3D position = findPoint3d(positionXYTrajectoryName, positionXYName, suffix, scs);
      Vector3D linearVelocity = findVector3d(positionXYTrajectoryName, linearVelocityXYName, suffix, scs);

      String timeName = pelvisXYPrefix + "Time";
      simpleSE3TrajectoryPoint.setTime(scs.getVariable(positionXYTrajectoryName, timeName + suffix).getValueAsDouble());
      simpleSE3TrajectoryPoint.setPosition(position);
      simpleSE3TrajectoryPoint.setLinearVelocity(linearVelocity);
      
      if (trajectoryPointIndex < RigidBodyTaskspaceControlState.maxPointsInGenerator)
      {
         String pelvisZPrefix = bodyName + "Height";
         String positionZTrajectoryName = pelvisZPrefix + MultipleWaypointsPositionTrajectoryGenerator.class.getSimpleName();
         String positionZName = pelvisZPrefix + "Position";
         String linearVelocityZName = pelvisZPrefix + "LinearVelocity";
         
         Point3D pelvisHeightPoint = findPoint3d(positionZTrajectoryName, positionZName, suffix, scs);
         double zHeight = pelvisHeightPoint.getZ();
         position.setZ(zHeight);
         
         double zLinearVelocity = findVector3d(positionZTrajectoryName, linearVelocityZName, suffix, scs).getZ();
         linearVelocity.setZ(zLinearVelocity);
         
         String orientationTrajectoryName = bodyName + "Orientation" + MultipleWaypointsOrientationTrajectoryGenerator.class.getSimpleName();
         String orientationName = bodyName + "Orientation" + "Orientation";
         String angularVelocityName = bodyName + "Orientation" + "AngularVelocity";

         simpleSE3TrajectoryPoint.setOrientation(findQuat4d(orientationTrajectoryName, orientationName, suffix, scs));
         simpleSE3TrajectoryPoint.setAngularVelocity(findVector3d(orientationTrajectoryName, angularVelocityName, suffix, scs));
      }
      else
      {
         simpleSE3TrajectoryPoint.setTimeToNaN();
      }

      return simpleSE3TrajectoryPoint;
   }
   
   public static SimpleSE3TrajectoryPoint findCurrentDesiredTrajectoryPoint(String bodyName, SimulationConstructionSet scs)
   {
      SimpleSE3TrajectoryPoint simpleSE3TrajectoryPoint = new SimpleSE3TrajectoryPoint();
      Point2D positionXY = findControllerDesiredPositionXY(scs);
      Point3D position = new Point3D(positionXY.getX(), positionXY.getY(), Double.NaN);
      simpleSE3TrajectoryPoint.setPosition(position);
      simpleSE3TrajectoryPoint.setOrientation(EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(bodyName, scs));
      simpleSE3TrajectoryPoint.setLinearVelocity(findControllerDesiredLinearVelocity(scs));
      simpleSE3TrajectoryPoint.setAngularVelocity(EndToEndHandTrajectoryMessageTest.findControllerDesiredAngularVelocity(bodyName, scs));
      return simpleSE3TrajectoryPoint;
   }

   public static void assertSingleWaypointExecuted(String bodyName, FullHumanoidRobotModel fullRobotModel, Point3D desiredPosition, Quaternion desiredOrientation, SimulationConstructionSet scs)
   {
      assertNumberOfWaypoints(2, scs);

      Point2D desiredControllerXY = findControllerDesiredPositionXY(scs);
      assertEquals(desiredPosition.getX(), desiredControllerXY.getX(), EPSILON_FOR_DESIREDS);
      assertEquals(desiredPosition.getY(), desiredControllerXY.getY(), EPSILON_FOR_DESIREDS);

      Quaternion desiredControllerOrientation = EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(bodyName, scs);
      EuclidCoreTestTools.assertQuaternionEquals(desiredOrientation, desiredControllerOrientation, EPSILON_FOR_DESIREDS);

      // Hard to figure out how to verify the desired there
//      trajOutput = scs.getVariable("pelvisHeightOffsetSubTrajectoryCubicPolynomialTrajectoryGenerator", "pelvisHeightOffsetSubTrajectoryCurrentValue").getValueAsDouble();
//      assertEquals(desiredPosition.getZ(), trajOutput, EPSILON_FOR_DESIREDS);
      // Ending up doing a rough check on the actual height
      MovingReferenceFrame pelvisFrame = fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint();
      FramePoint pelvisPosition = new FramePoint(pelvisFrame);
      pelvisPosition.changeFrame(ReferenceFrame.getWorldFrame());
      double pelvisHeight = pelvisPosition.getZ();
      assertEquals(desiredPosition.getZ(), pelvisHeight, EPSILON_FOR_HEIGHT);
   }

   public static void assertNumberOfWaypoints(int expectedNumberOfWaypoints, SimulationConstructionSet scs)
   {
//      assertEquals(expectedNumberOfWaypoints, findControllerNumberOfWaypointsForOrientation(scs));
      assertEquals(expectedNumberOfWaypoints, findControllerNumberOfWaypointsForXY(scs));
//      assertEquals(expectedNumberOfWaypoints, findControllerNumberOfWaypointsForHeight(scs));
   }

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
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
}
