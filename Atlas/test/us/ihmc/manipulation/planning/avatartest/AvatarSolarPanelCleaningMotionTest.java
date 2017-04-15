package us.ihmc.manipulation.planning.avatartest;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.kinematicsToolboxModule.KinematicsToolboxController;
import us.ihmc.avatar.networkProcessor.kinematicsToolboxModule.KinematicsToolboxModule;
import us.ihmc.avatar.testTools.DRCBehaviorTestHelper;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.rotationConversion.AxisAngleConversion;
import us.ihmc.euclid.rotationConversion.QuaternionConversion;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.manipulation.planning.forwaypoint.SO3TrajectoryPointCalculator;
import us.ihmc.manipulation.planning.manipulation.solarpanelmotion.SolarPanelCleaningPose;
import us.ihmc.manipulation.planning.manipulation.solarpanelmotion.SolarPanelLinearPath;
import us.ihmc.manipulation.planning.manipulation.solarpanelmotion.SolarPanelMotionPlanner;
import us.ihmc.manipulation.planning.manipulation.solarpanelmotion.SolarPanelMotionPlanner.CleaningMotion;
import us.ihmc.manipulation.planning.manipulation.solarpanelmotion.SolarPanelPath;
import us.ihmc.manipulation.planning.rrt.RRTNode;
import us.ihmc.manipulation.planning.rrttimedomain.RRTNode1DTimeDomain;
import us.ihmc.manipulation.planning.rrttimedomain.RRTPlannerSolarPanelCleaning;
import us.ihmc.manipulation.planning.rrttimedomain.RRTTreeTimeDomain;
import us.ihmc.manipulation.planning.solarpanelmotion.SolarPanel;
import us.ihmc.manipulation.planning.tobeDeleted.SolarPanelPoseValidityTesterOld;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.transformables.Pose;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.math.trajectories.waypoints.EuclideanTrajectoryPointCalculator;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSO3TrajectoryPoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationConstructionSetTools.util.environments.DefaultCommonAvatarEnvironment;
import us.ihmc.simulationConstructionSetTools.util.environments.SelectableObjectListener;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;

public abstract class AvatarSolarPanelCleaningMotionTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
   private boolean isKinematicsToolboxVisualizerEnabled = false;
   private DRCBehaviorTestHelper drcBehaviorTestHelper;
   private KinematicsToolboxModule kinematicsToolboxModule;
   private PacketCommunicator toolboxCommunicator;
      
   private FullHumanoidRobotModel fullRobotModel;
   SolarPanel solarPanel;
   
   public class SolarPanelCleaningEnvironment implements CommonAvatarEnvironmentInterface
   {
      private final CombinedTerrainObject3D EnvSet;

      public SolarPanelCleaningEnvironment()
      {
         setUpSolarPanel();
         
         EnvSet = DefaultCommonAvatarEnvironment.setUpGround("Ground");
                  
         EnvSet.addRotatableBox(solarPanel.getRigidBodyTransform(), solarPanel.getSizeX(), solarPanel.getSizeY(), solarPanel.getSizeZ(), YoAppearance.Aqua());
      }
      
      @Override
      public TerrainObject3D getTerrainObject3D()
      {
         // TODO Auto-generated method stub
         return EnvSet;
      }

      @Override
      public List<? extends Robot> getEnvironmentRobots()
      {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public void createAndSetContactControllerToARobot()
      {
         // TODO Auto-generated method stub

      }

      @Override
      public void addContactPoints(List<? extends ExternalForcePoint> externalForcePoints)
      {
         // TODO Auto-generated method stub

      }

      @Override
      public void addSelectableListenerToSelectables(SelectableObjectListener selectedListener)
      {
         // TODO Auto-generated method stub
      }
   }


   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (!ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer()) // set
      //if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      if (kinematicsToolboxModule != null)
      {
         kinematicsToolboxModule.destroy();
         kinematicsToolboxModule = null;
      }

      if (toolboxCommunicator != null)
      {
         toolboxCommunicator.close();
         toolboxCommunicator.closeConnection();
         toolboxCommunicator = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @Before
   public void setUp() throws IOException
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      CommonAvatarEnvironmentInterface envrionment = new SolarPanelCleaningEnvironment();

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(envrionment, getSimpleRobotName(), null, simulationTestingParameters, getRobotModel());
      fullRobotModel = drcBehaviorTestHelper.getControllerFullRobotModel();
      setupKinematicsToolboxModule();
   }
      
   //@Test(timeout = 160000)
   public void testHalfCircleMotion() throws SimulationExceededMaximumTimeException, IOException
   {
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();      
      setupCamera(scs);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      
      // ********** Planning *** //
      
      

      
      // ********** Behavior *** //
      
      
      
      drcBehaviorTestHelper.updateRobotModel();
            
      RigidBody chest = fullRobotModel.getChest();
      RigidBody hand = fullRobotModel.getHand(RobotSide.RIGHT);
      ReferenceFrame chestFrame = chest.getBodyFixedFrame();
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      
      double motionTime = 3.0;
      
      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();
      //HandTrajectoryMessage handTrajectoryMessageOne = new HandTrajectoryMessage(RobotSide.RIGHT, motionTime, new Point3D(0.70,  -0.5,  1.2), new Quaternion());
      HandTrajectoryMessage handTrajectoryMessageOne = new HandTrajectoryMessage(RobotSide.RIGHT, motionTime, new Point3D(0.70,  -0.5,  1.2), new Quaternion(), worldFrame, worldFrame);
      int numberOfWayPointForCircle = 19;
      HandTrajectoryMessage handTrajectoryMessageCircle = new HandTrajectoryMessage(RobotSide.RIGHT, numberOfWayPointForCircle);
      handTrajectoryMessageCircle.setTrajectoryReferenceFrameId(worldFrame);
      
      wholeBodyTrajectoryMessage.clear();
      wholeBodyTrajectoryMessage.setHandTrajectoryMessage(handTrajectoryMessageOne);
      drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      
      wholeBodyTrajectoryMessage.clear();
      
      
      
            
      drcBehaviorTestHelper.updateRobotModel();
      ReferenceFrame handControlFrameR = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(RobotSide.RIGHT);      
      FramePose curHandPoseR = new FramePose(handControlFrameR);
      curHandPoseR.changeFrame(ReferenceFrame.getWorldFrame());      
      Point3D curHandPosR = new Point3D();
      Quaternion curHandOriR = new Quaternion();
      curHandPoseR.getPosition(curHandPosR);
      curHandPoseR.getOrientation(curHandOriR);
      PrintTools.info("Cur Pos "+curHandPosR.getX()+" "+curHandPosR.getY()+" "+curHandPosR.getZ()+" ");
      PrintTools.info("Cur Ori "+curHandOriR.getS()+" "+curHandOriR.getX()+" "+curHandOriR.getY()+" "+curHandOriR.getZ());      
      
      double radius = 0.15;
      Pose centerPoseOfCircle = new Pose(new Point3D(0.65,  -0.35,  1.2), new Quaternion());
      double timePerWaypoint = 0.2;
      double trajectoryTime = numberOfWayPointForCircle * timePerWaypoint;

      EuclideanTrajectoryPointCalculator euclideanCalculator = new EuclideanTrajectoryPointCalculator();
      SO3TrajectoryPointCalculator so3Calculator = new SO3TrajectoryPointCalculator();
      for(int i=0;i<numberOfWayPointForCircle;i++)
      {         
         Pose poseOfWayPoint = new Pose(centerPoseOfCircle.getPosition(), centerPoseOfCircle.getOrientation());
         
         poseOfWayPoint.translate(0, -radius*Math.cos(Math.PI*i/(numberOfWayPointForCircle-1)), radius*Math.sin(Math.PI*i/(numberOfWayPointForCircle-1)));
         
         Quaternion appendingOrientation = new Quaternion();
         appendingOrientation.appendYawRotation(Math.PI*0.0*i/(numberOfWayPointForCircle-1));
         poseOfWayPoint.setOrientation(appendingOrientation);
         
         scs.addStaticLinkGraphics(createXYZAxis(poseOfWayPoint));
         // ----------------------------------
         Point3D desiredPosition = new Point3D(poseOfWayPoint.getPosition());
         Quaternion desiredOrientation = new Quaternion(poseOfWayPoint.getOrientation());
         
         euclideanCalculator.appendTrajectoryPoint(desiredPosition);
         so3Calculator.appendTrajectoryPoint(desiredOrientation);
         
      }

      euclideanCalculator.computeTrajectoryPointTimes(0.1, trajectoryTime);
      euclideanCalculator.computeTrajectoryPointVelocities(true);

      so3Calculator.setTrajectoryPointTimes(euclideanCalculator.getTrajectoryPoints());
      so3Calculator.computeTrajectoryPoints();            
      
      RecyclingArrayList<FrameEuclideanTrajectoryPoint> trajectoryPoints = euclideanCalculator.getTrajectoryPoints();
      RecyclingArrayList<FrameSO3TrajectoryPoint> trajectoryQuaternions = so3Calculator.getTrajectoryPoints();
      
      for (int i=0;i<numberOfWayPointForCircle;i++)
      {
         Point3D desiredPosition = new Point3D();
         Vector3D desiredLinearVelocity = new Vector3D();
         Quaternion desiredOrientation = new Quaternion();
         Vector3D desiredAngularVelocity = new Vector3D();
         
         double time = trajectoryPoints.get(i).get(desiredPosition, desiredLinearVelocity);
         
         trajectoryQuaternions.get(i).getOrientation(desiredOrientation);;
         trajectoryQuaternions.get(i).getAngularVelocity(desiredAngularVelocity);
                  
         handTrajectoryMessageCircle.setTrajectoryPoint(i, time, desiredPosition, desiredOrientation, desiredLinearVelocity, desiredAngularVelocity, worldFrame);
      }
      
      wholeBodyTrajectoryMessage.setHandTrajectoryMessage(handTrajectoryMessageCircle);
      drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(trajectoryTime);
   }
   
   //@Test(timeout = 160000)
   public void testCombinedMotionTest() throws SimulationExceededMaximumTimeException, IOException
   {
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();      
      setupCamera(scs);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      
      // ********** Planning *** //
      
      
      // ********** Behavior *** //
      drcBehaviorTestHelper.updateRobotModel();
      
      double motionTime = 3.0;
      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();
            
      SolarPanelCleaningPose aCleaningPoseOne = new SolarPanelCleaningPose(solarPanel, 0.5, 0.1, -0.1);
      aCleaningPoseOne.setZRotation(-Math.PI*0.2);
      scs.addStaticLinkGraphics(createXYZAxis(aCleaningPoseOne.getPose()));
                  
      wholeBodyTrajectoryMessage.clear();
      wholeBodyTrajectoryMessage.setHandTrajectoryMessage(aCleaningPoseOne.getHandTrajectoryMessage(motionTime));
      drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      
      FramePose desiredPelvisPose = new FramePose(drcBehaviorTestHelper.getReferenceFrames().getPelvisFrame());
      Quaternion desiredPelvisOrientation = new Quaternion();
      desiredPelvisOrientation.appendYawRotation(Math.PI*0.2);
      desiredPelvisPose.setPosition(new Point3D());
      desiredPelvisPose.setOrientation(desiredPelvisOrientation);      
      desiredPelvisPose.changeFrame(ReferenceFrame.getWorldFrame());
      Point3D desiredPosition = new Point3D(desiredPelvisPose.getPosition());
      Quaternion desiredOrientation = new Quaternion(desiredPelvisPose.getOrientation());
      PrintTools.info("desiredPosition "+desiredPosition.getX()+" "+desiredPosition.getY()+" "+desiredPosition.getZ()+" ");
      PelvisTrajectoryMessage pelvisTrajectoryMessage = new PelvisTrajectoryMessage(motionTime, desiredPosition, desiredOrientation);      
      
      SolarPanelCleaningPose aCleaningPoseTwo = new SolarPanelCleaningPose(solarPanel, 0.1, 0.1, -0.1);
      aCleaningPoseTwo.setZRotation(-Math.PI*0.4);
      scs.addStaticLinkGraphics(createXYZAxis(aCleaningPoseTwo.getPose()));
      wholeBodyTrajectoryMessage.clear();
      wholeBodyTrajectoryMessage.setHandTrajectoryMessage(aCleaningPoseTwo.getHandTrajectoryMessage(motionTime));
      wholeBodyTrajectoryMessage.setPelvisTrajectoryMessage(pelvisTrajectoryMessage);
      
      drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      
      
      
      
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
   }
   
   
   //@Test(timeout = 160000)
   public void testSolarPanelMotion() throws SimulationExceededMaximumTimeException, IOException
   {
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();      
      setupCamera(scs);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();

      double motionTime = 0;
      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();
      // ********** Planning *** //
      SolarPanelMotionPlanner solarPanelPlanner = new SolarPanelMotionPlanner(solarPanel);

      if(solarPanelPlanner.setWholeBodyTrajectoryMessage(CleaningMotion.ReadyPose) == true)
      {
         wholeBodyTrajectoryMessage = solarPanelPlanner.getWholeBodyTrajectoryMessage();
         motionTime = solarPanelPlanner.getMotionTime();
         drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);   
      }
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      if(solarPanelPlanner.setWholeBodyTrajectoryMessage(CleaningMotion.LinearCleaningMotion))
      {
         wholeBodyTrajectoryMessage = solarPanelPlanner.getWholeBodyTrajectoryMessage();
         motionTime = solarPanelPlanner.getMotionTime();
         drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      }
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      
      
      
      
      for(int i=0;i<solarPanelPlanner.debugPose.size();i++)
      {
         scs.addStaticLinkGraphics(createXYZAxis(solarPanelPlanner.debugPose.get(i)));
      }
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      
      

      
   }
   
   //@Test
   public void testWholeBodyValidityTest() throws SimulationExceededMaximumTimeException, IOException
   {
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();      
      setupCamera(scs);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();
      //ThreadTools.sleep(20000);
      
      
      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();

      KinematicsToolboxController kinematicsToolBoxController = (KinematicsToolboxController) kinematicsToolboxModule.getToolboxController();      
                  
      SolarPanelPoseValidityTesterOld solarPanelValidityTester = new SolarPanelPoseValidityTesterOld(solarPanel, toolboxCommunicator, kinematicsToolBoxController);
      
      HandTrajectoryMessage handTrajectoryMessage = new HandTrajectoryMessage(RobotSide.RIGHT, 0.2, new Point3D(0.5, -0.35, 1.0), new Quaternion(), ReferenceFrame.getWorldFrame(), ReferenceFrame.getWorldFrame());
      wholeBodyTrajectoryMessage.setHandTrajectoryMessage(handTrajectoryMessage);
      PrintTools.info("send first");
      PrintTools.info("send first");
      PrintTools.info("send first");
      PrintTools.info("send first");
      PrintTools.info("r shz"+kinematicsToolBoxController.getSolution().getJointAngles()[12]);
      solarPanelValidityTester.sendWholebodyTrajectoryMessage(wholeBodyTrajectoryMessage);
      
      ThreadTools.sleep(2000);
      //drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(getRobotModel().getControllerDT());
      PrintTools.info("Wake up CMD");
      //kinematicsToolboxModule.wakeUp(PacketDestination.KINEMATICS_TOOLBOX_MODULE);      
      PrintTools.info("r shz"+kinematicsToolBoxController.getSolution().getJointAngles()[12]);
      
      
      
      ThreadTools.sleep(2000);
      
      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage2 = new WholeBodyTrajectoryMessage();
      handTrajectoryMessage = new HandTrajectoryMessage(RobotSide.RIGHT, 0.2, new Point3D(0.7, -0.45, 1.1), new Quaternion(), ReferenceFrame.getWorldFrame(), ReferenceFrame.getWorldFrame());
      wholeBodyTrajectoryMessage2.setHandTrajectoryMessage(handTrajectoryMessage);
      PrintTools.info("send second");
      PrintTools.info("send second");
      PrintTools.info("send second");
      PrintTools.info("send second"+kinematicsToolBoxController.getSolution().getJointAngles()[12]);
      solarPanelValidityTester.sendWholebodyTrajectoryMessage(wholeBodyTrajectoryMessage2);
      
      ThreadTools.sleep(2000);
      PrintTools.info("r shz"+kinematicsToolBoxController.getSolution().getJointAngles()[12]);
      ThreadTools.sleep(2000);
      PrintTools.info("r shz"+kinematicsToolBoxController.getSolution().getJointAngles()[12]);
      //drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      
      //success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(3.0);
      
      // ***************************************************** //
      
      
      scs.addStaticLinkGraphics(solarPanelValidityTester.getRobotCollisionModel().getCollisionGraphics());
         
//      for (int i=0;i<kinematicsToolBoxController.getDesiredFullRobotModel().getOneDoFJoints().length;i++)
//      {
//         PrintTools.info("");
//         OneDoFJoint aJoint = kinematicsToolBoxController.getDesiredFullRobotModel().getOneDoFJoints()[i];
//         PrintTools.info(""+aJoint.getName()+" "+aJoint.getQ());
//         OneDoFJoint bJoint = drcBehaviorTestHelper.getControllerFullRobotModel().getOneDoFJoints()[i];
//         PrintTools.info(""+bJoint.getName()+" "+bJoint.getQ());         
//      }
      
      
//      while (true)
//      {
//         ThreadTools.sleep(100);
//         toolboxCommunicator.send(wholeBodyTrajectoryMessage);
//      }
      
      
      
      


   }
   
   //@Test
   public void testRRTTimeDomainTest() throws SimulationExceededMaximumTimeException, IOException
   {
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();      
      setupCamera(scs);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();
      ThreadTools.sleep(20000);
      
      
      KinematicsToolboxController kinematicsToolBoxController = (KinematicsToolboxController) kinematicsToolboxModule.getToolboxController(); 
      
      RRTNode1DTimeDomain.nodeValidityTester = new SolarPanelPoseValidityTesterOld(solarPanel, toolboxCommunicator, kinematicsToolBoxController);
      RRTNode1DTimeDomain nodeOne = new RRTNode1DTimeDomain(0.1, Math.PI*0.2);
      
      PrintTools.info("Node One "+nodeOne.getNodeData(0)+" "+nodeOne.getNodeData(1));
      PrintTools.info("Node One "+nodeOne.isValidNode()); 
      
      
      double motionTime = 5.0;
      double maximumDisplacement = 0.1;
      double maximumTimeGap = 0.1;
      
      RRTNode1DTimeDomain nodeRoot = new RRTNode1DTimeDomain(0.0, 0.0);
      RRTNode1DTimeDomain nodeLowerBound = new RRTNode1DTimeDomain(0.0, -Math.PI*0.4);
      RRTNode1DTimeDomain nodeUpperBound = new RRTNode1DTimeDomain(motionTime*1.5, Math.PI*0.4);
      
//      RRTPlannerSolarPanelCleaningOLDOLDOLD plannerTimeDomain = new RRTPlannerSolarPanelCleaningOLDOLDOLD(nodeRoot);
//      
//      plannerTimeDomain.getTree().setMaximumMotionTime(motionTime);
//      plannerTimeDomain.getTree().setUpperBound(nodeUpperBound);
//      plannerTimeDomain.getTree().setLowerBound(nodeLowerBound);
//      plannerTimeDomain.getTree().setMaximumDisplacementOfStep(maximumDisplacement);      
//      plannerTimeDomain.getTree().setMaximumTimeGapOfStep(maximumTimeGap);
//      
//      plannerTimeDomain.expandTreeGoal(500);
//      plannerTimeDomain.updateOptimalPath(100);
//      
//      PrintTools.info(""+plannerTimeDomain.getOptimalPath().size());
      
   // ************************************* //
      // show
   // ************************************* //
//      JFrame frame;
//      DrawPanel drawPanel;
//      Dimension dim;
//      
//
//      frame = new JFrame("RRTTest");
//      drawPanel = new DrawPanel(plannerTimeDomain);
//      dim = new Dimension(1600, 800);
//      frame.setPreferredSize(dim);
//      frame.setLocation(200, 100);
//
//      frame.add(drawPanel);
//      frame.pack();
//      frame.setVisible(true);      
   }
   
   
   @Test
   public void testRRTAndMotion() throws SimulationExceededMaximumTimeException, IOException
   {
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();      
      setupCamera(scs);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel(); 
      ThreadTools.sleep(10000);
      
      
      KinematicsToolboxController kinematicsToolBoxController = (KinematicsToolboxController) kinematicsToolboxModule.getToolboxController(); 
      
      RRTNode1DTimeDomain.nodeValidityTester = new SolarPanelPoseValidityTesterOld(solarPanel, toolboxCommunicator, kinematicsToolBoxController);
      
      // ********** Planning *** //
      
      double motionTime = 0;
      WholeBodyTrajectoryMessage wholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();
     
      SolarPanelMotionPlanner solarPanelPlanner = new SolarPanelMotionPlanner(solarPanel);

      if(solarPanelPlanner.setWholeBodyTrajectoryMessage(CleaningMotion.ReadyPose) == true)
      {
         wholeBodyTrajectoryMessage = solarPanelPlanner.getWholeBodyTrajectoryMessage();
         motionTime = solarPanelPlanner.getMotionTime();
         drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);         
      }      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      if(solarPanelPlanner.setWholeBodyTrajectoryMessage(CleaningMotion.LinearCleaningMotion) == true)
      {
         wholeBodyTrajectoryMessage = solarPanelPlanner.getWholeBodyTrajectoryMessage();
         motionTime = solarPanelPlanner.getMotionTime();
         drcBehaviorTestHelper.send(wholeBodyTrajectoryMessage);
      }      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(motionTime);
      
      
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      
      
      
      scs.addStaticLinkGraphics(getPrintCleaningPath(RRTNode1DTimeDomain.cleaningPath));
      
      // ************************************* //
      // show
      // ************************************* //
      JFrame frame;
      DrawPanel drawPanel;
      Dimension dim;
      

      frame = new JFrame("RRTTest");
      drawPanel = new DrawPanel(solarPanelPlanner.rrtPlanner);
      dim = new Dimension(1600, 800);
      frame.setPreferredSize(dim);
      frame.setLocation(200, 100);

      frame.add(drawPanel);
      frame.pack();
      frame.setVisible(true);
      
      // ************************************* //
      // show
      // ************************************* //
      PrintTools.info("END");
   }
   
   // ************************************* //
   class DrawPanel extends JPanel
   {
      int timeScale = 100;
      int pelvisYawScale = 300;
      RRTPlannerSolarPanelCleaning planner;
      
      DrawPanel(RRTPlannerSolarPanelCleaning plannerTimeDomain)
      {
         this.planner = plannerTimeDomain;
      }

      public void paint(Graphics g)
      {
         super.paint(g);
//       g.setColor(Color.red);
//       branch(g, 1.5, -Math.PI*0.1, 1.5, Math.PI*0.1, 4);
//       branch(g, 1.5, Math.PI*0.1, 2.5, Math.PI*0.1, 4);
//       branch(g, 2.5, Math.PI*0.1, 2.5, -Math.PI*0.1, 4);
//       branch(g, 2.5, -Math.PI*0.1, 1.5, -Math.PI*0.1, 4);

         
         
         
         for(int j =0;j<planner.getNumberOfPlanners();j++)
         {
            RRTTreeTimeDomain tree = planner.getPlanner(j).getTree();
            ArrayList<RRTNode> wholeNode = tree.getWholeNode();
            g.setColor(Color.BLACK);
            for(int i =1;i<wholeNode.size();i++)
            {
               RRTNode rrtNode1 = wholeNode.get(i);
               RRTNode rrtNode2 = rrtNode1.getParentNode();
               branch(g, rrtNode1.getNodeData(0), rrtNode1.getNodeData(1), rrtNode2.getNodeData(0), rrtNode2.getNodeData(1), 4);
            }
            
            
            g.setColor(Color.BLUE);
            ArrayList<RRTNode> nodePath = tree.getPathNode();
            for(int i =1;i<nodePath.size();i++)
            {
               RRTNode rrtNode1 = nodePath.get(i);
               RRTNode rrtNode2 = rrtNode1.getParentNode();
               branch(g, rrtNode1.getNodeData(0), rrtNode1.getNodeData(1), rrtNode2.getNodeData(0), rrtNode2.getNodeData(1), 4);
            }
            
             g.setColor(Color.CYAN);
             ArrayList<RRTNode> nodeShort = planner.getPlanner(j).getOptimalPath();
             for(int i =1;i<nodeShort.size();i++)
             {
                RRTNode rrtNode1 = nodeShort.get(i);
                RRTNode rrtNode2 = nodeShort.get(i-1);
                branch(g, rrtNode1.getNodeData(0), rrtNode1.getNodeData(1), rrtNode2.getNodeData(0), rrtNode2.getNodeData(1), 4);
             }
             
             g.setColor(Color.RED);
             ArrayList<RRTNode> nodeFail = tree.failNodes;
             PrintTools.info("whole "+wholeNode.size() + " path " + nodePath.size() + " nodeShort " + nodeShort.size() + " fail " + nodeFail.size());
             for(int i =0;i<nodeFail.size();i++)
             {
                RRTNode rrtNode1 = nodeFail.get(i);
                point(g, rrtNode1.getNodeData(0), rrtNode1.getNodeData(1), 4);
             }
         }

         

         
         g.setColor(Color.yellow);
         branch(g, RRTNode1DTimeDomain.cleaningPath.getArrivalTime().get(1), -Math.PI*0.4, RRTNode1DTimeDomain.cleaningPath.getArrivalTime().get(1), Math.PI*0.4, 4);
         branch(g, RRTNode1DTimeDomain.cleaningPath.getArrivalTime().get(2), -Math.PI*0.4, RRTNode1DTimeDomain.cleaningPath.getArrivalTime().get(2), Math.PI*0.4, 4);
         branch(g, RRTNode1DTimeDomain.cleaningPath.getArrivalTime().get(3), -Math.PI*0.4, RRTNode1DTimeDomain.cleaningPath.getArrivalTime().get(3), Math.PI*0.4, 4);
      }
      
      public int t2u(double time)
      {
         return (int) Math.round((time * timeScale) + 50);
      }

      public int y2v(double yaw)
      {
         return (int) Math.round(((-yaw)) * pelvisYawScale + 400);
      }
      
      public void point(Graphics g, double time, double yaw, int size)
      {
         int diameter = size;
         g.drawOval(t2u(time) - diameter / 2, y2v(yaw) - diameter / 2, diameter, diameter);
      }
      
      public void branch(Graphics g, double time1, double yaw1, double time2, double yaw2, int size)
      {
         point(g, time1, yaw1, size);
         point(g, time2, yaw2, size);
         
         g.drawLine(t2u(time1), y2v(yaw1), t2u(time2), y2v(yaw2));
      }
   }
   
   // ************************************* //
   
   
   
   private void setUpSolarPanel()
   {
      Pose poseSolarPanel = new Pose();
      Quaternion quaternionSolarPanel = new Quaternion();
      poseSolarPanel.setPosition(0.7, -0.05, 1.0);
      quaternionSolarPanel.appendRollRotation(0.0);
      quaternionSolarPanel.appendPitchRotation(-Math.PI*0.25);
      poseSolarPanel.setOrientation(quaternionSolarPanel);
      
      solarPanel = new SolarPanel(poseSolarPanel, 0.6, 0.6);
   }

   public void setupCamera(SimulationConstructionSet scs)
   {
      Point3D cameraFix = new Point3D(0.5, 0.0, 1.0);
      Point3D cameraPosition = new Point3D(-1.0, -2.5, 3.0);

      drcBehaviorTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
   }
   
  
   private void setupKinematicsToolboxModule() throws IOException
   {
      DRCRobotModel robotModel = getRobotModel();      
      kinematicsToolboxModule = new KinematicsToolboxModule(robotModel, true);
      toolboxCommunicator = drcBehaviorTestHelper.createAndStartPacketCommunicator(NetworkPorts.KINEMATICS_TOOLBOX_MODULE_PORT, PacketDestination.KINEMATICS_TOOLBOX_MODULE);
   }
   
   
   private ArrayList<Graphics3DObject> createXYZAxis(Pose pose)
   {
      return createXYZAxis(pose, 0.05, 0.005);
   }
   
   private ArrayList<Graphics3DObject> createXYZAxis(Pose pose, double height, double radius)
   {
      double axisHeight = height;
      double axisRadius = radius;
      ArrayList<Graphics3DObject> ret = new ArrayList<Graphics3DObject>();

      Graphics3DObject retCenter = new Graphics3DObject();
      Graphics3DObject retX = new Graphics3DObject();
      Graphics3DObject retY = new Graphics3DObject();
      Graphics3DObject retZ = new Graphics3DObject();

      Point3D centerPoint = new Point3D(pose.getPoint());

      retX.translate(centerPoint);
      retY.translate(centerPoint);
      retZ.translate(centerPoint);

      retCenter.translate(centerPoint);
      retCenter.addSphere(0.01, YoAppearance.Black());
      
      ret.add(retCenter);
      
      Quaternion qtAxis = new Quaternion(pose.getOrientation());
      Quaternion qtAlpha = new Quaternion();
      AxisAngle rvAlpha = new AxisAngle();
      AxisAngle rvAxis = new AxisAngle();      
      
      QuaternionConversion.convertAxisAngleToQuaternion(rvAlpha, qtAlpha);
      
      qtAxis.multiply(qtAlpha);
      AxisAngleConversion.convertQuaternionToAxisAngle(qtAxis, rvAxis);
            
      retZ.rotate(rvAxis);
      retZ.addCylinder(axisHeight, axisRadius, YoAppearance.Blue());      
      
      rvAlpha = new AxisAngle(0,1,0,Math.PI/2);
      QuaternionConversion.convertAxisAngleToQuaternion(rvAlpha, qtAlpha);
      
      qtAxis.multiply(qtAlpha);
      AxisAngleConversion.convertQuaternionToAxisAngle(qtAxis, rvAxis);
            
      retX.rotate(rvAxis);
      retX.addCylinder(axisHeight, axisRadius, YoAppearance.Red());
      
      
      rvAlpha = new AxisAngle(1,0,0,-Math.PI/2);
      QuaternionConversion.convertAxisAngleToQuaternion(rvAlpha, qtAlpha);
      
      qtAxis.multiply(qtAlpha);
      AxisAngleConversion.convertQuaternionToAxisAngle(qtAxis, rvAxis);
            
      retY.rotate(rvAxis);
      retY.addCylinder(axisHeight, axisRadius, YoAppearance.Green());
      
      ret.add(retX);
      ret.add(retY);
      ret.add(retZ);

      return ret;
   }
   
   private ArrayList<Graphics3DObject> createXYZAxis(RigidBodyTransform rigidBodyTransform, double height, double radius)
   {
      Point3D centerPoint = new Point3D();
      rigidBodyTransform.getTranslation(centerPoint);
      
      Quaternion qtAxis = new Quaternion();
      rigidBodyTransform.getRotation(qtAxis);

      return createXYZAxis(centerPoint, qtAxis, height, radius);
   }

   private ArrayList<Graphics3DObject> createXYZAxis(Point3D center, Quaternion orientation, double height, double radius)
   {
      double axisHeight = height;
      double axisRadius = radius;
      ArrayList<Graphics3DObject> ret = new ArrayList<Graphics3DObject>();

      Graphics3DObject retCenter = new Graphics3DObject();
      Graphics3DObject retX = new Graphics3DObject();
      Graphics3DObject retY = new Graphics3DObject();
      Graphics3DObject retZ = new Graphics3DObject();

      Point3D centerPoint = center;

      retX.translate(centerPoint);
      retY.translate(centerPoint);
      retZ.translate(centerPoint);

      retCenter.translate(centerPoint);
      retCenter.addSphere(0.01, YoAppearance.Black());
      
      ret.add(retCenter);
      
      Quaternion qtAxis = orientation;
      Quaternion qtAlpha = new Quaternion();
      AxisAngle rvAlpha = new AxisAngle();
      AxisAngle rvAxis = new AxisAngle();      
      
      QuaternionConversion.convertAxisAngleToQuaternion(rvAlpha, qtAlpha);
      
      qtAxis.multiply(qtAlpha);
      AxisAngleConversion.convertQuaternionToAxisAngle(qtAxis, rvAxis);
            
      retZ.rotate(rvAxis);
      retZ.addCylinder(axisHeight, axisRadius, YoAppearance.Blue());      
      
      rvAlpha = new AxisAngle(0,1,0,Math.PI/2);
      QuaternionConversion.convertAxisAngleToQuaternion(rvAlpha, qtAlpha);
      
      qtAxis.multiply(qtAlpha);
      AxisAngleConversion.convertQuaternionToAxisAngle(qtAxis, rvAxis);
            
      retX.rotate(rvAxis);
      retX.addCylinder(axisHeight, axisRadius, YoAppearance.Red());
            
      rvAlpha = new AxisAngle(1,0,0,-Math.PI/2);
      QuaternionConversion.convertAxisAngleToQuaternion(rvAlpha, qtAlpha);
      
      qtAxis.multiply(qtAlpha);
      AxisAngleConversion.convertQuaternionToAxisAngle(qtAxis, rvAxis);
            
      retY.rotate(rvAxis);
      retY.addCylinder(axisHeight, axisRadius, YoAppearance.Green());
      
      ret.add(retX);
      ret.add(retY);
      ret.add(retZ);

      return ret;
   }
   
   private ArrayList<Graphics3DObject> getPrintCleaningPath(SolarPanelPath cleaningPath)
   {
      ArrayList<Graphics3DObject> ret = new ArrayList<Graphics3DObject>();
      
      for(int i=0;i<cleaningPath.getLinearPath().size();i++)
      {
         ret.addAll(getPrintLinearPath(cleaningPath.getLinearPath().get(i)));
      }
      
      return ret;
   }
   
   private ArrayList<Graphics3DObject> getPrintLinearPath(SolarPanelLinearPath linearPath)
   {
      ArrayList<Graphics3DObject> ret = new ArrayList<Graphics3DObject>();

      
      Graphics3DObject nodeOneSphere = new Graphics3DObject();
      Graphics3DObject nodeTwoSphere = new Graphics3DObject();
      
      Graphics3DObject lineCapsule = new Graphics3DObject();
      
      Point3D translationNodeOne = linearPath.getStartPose().getDesiredHandPosition();
      nodeOneSphere.translate(translationNodeOne);
      nodeOneSphere.addSphere(0.02, YoAppearance.DarkGray());
      
      Point3D translationNodeTwo = linearPath.getEndPose().getDesiredHandPosition();
      nodeTwoSphere.translate(translationNodeTwo);
      nodeTwoSphere.addSphere(0.02, YoAppearance.DarkGray());
      
      Point3D translationLine = new Point3D((translationNodeOne.getX()+translationNodeTwo.getX())/2, (translationNodeOne.getY()+translationNodeTwo.getY())/2, (translationNodeOne.getZ()+translationNodeTwo.getZ())/2);
      AxisAngle rotationLine = new AxisAngle(-(translationNodeOne.getY()-translationNodeTwo.getY()), (translationNodeOne.getX()-translationNodeTwo.getX()), 0, Math.PI/2);
      lineCapsule.translate(translationLine);      
      lineCapsule.rotate(rotationLine);
      lineCapsule.addCapsule(0.02, translationNodeOne.distance(translationNodeTwo), YoAppearance.Gray());
            
      ret.add(nodeOneSphere);
      ret.add(nodeTwoSphere);

      return ret;
   }
   
}