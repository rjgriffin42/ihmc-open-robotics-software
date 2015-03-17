package us.ihmc.atlas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.atlas.AtlasRobotModel.AtlasTarget;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.IMUMount;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.simulatedSensors.GroundContactPointBasedWrenchCalculator;
import us.ihmc.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.InverseDynamicsCalculator;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SixDoFJointReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;

public class AtlasInverseDynamicsCalculatorTestHelper
{
   private final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private final YoVariableRegistry registry = new YoVariableRegistry("AtlasInverseDynamicsCalculatorTestHelper");

   private final YoFrameVector computedRootJointForces = new YoFrameVector("tau_computed_root_force_", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector computedRootJointTorques = new YoFrameVector("tau_computed_root_torques_", ReferenceFrame.getWorldFrame(), registry);
   
   private final YoFrameVector leftFootComputedWrenchForce = new YoFrameVector("wrench_computed_leftFootForce", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector rightFootComputedWrenchForce = new YoFrameVector("wrench_computed_rightFootForce", ReferenceFrame.getWorldFrame(), registry);

   private final SDFRobot robot;
   private final SDFFullRobotModel fullRobotModel;
   private final SimulationConstructionSet scs;
   private final TwistCalculator twistCalculator;
   private final InverseDynamicsCalculator inverseDynamicsCalculator;

   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> computedJointTorques = new LinkedHashMap<OneDoFJoint, DoubleYoVariable>();
   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> computedJointAccelerations = new LinkedHashMap<OneDoFJoint, DoubleYoVariable>();

   private final YoFrameVector computedRootJointLinearAcceleration = new YoFrameVector("qdd_computed_root_linear", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector computedRootJointAngularAcceleration;
   
   private final ExternalForcePoint rootJointExternalForcePoint;

   private final SideDependentList<ExternalForcePoint> feetExternalForcePoints;

   public static AtlasInverseDynamicsCalculatorTestHelper createAtlasInverseDynamicsCalculatorTestHelperUsingAtlasUnplugged(boolean visualize, double gravityZ)
   {
      boolean headless = false;
      AtlasRobotModel atlasRobotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, AtlasTarget.SIM, headless);
      SDFFullRobotModel fullRobotModel = atlasRobotModel.createFullRobotModel();
      
      boolean createCollisionMeshes = false;
      atlasRobotModel.setEnableJointDamping(false);
      SDFRobot robot = atlasRobotModel.createSdfRobot(createCollisionMeshes);
      robot.setGravity(gravityZ);
      
      return new AtlasInverseDynamicsCalculatorTestHelper(fullRobotModel, robot, visualize, gravityZ);
   }
   
   
   public AtlasInverseDynamicsCalculatorTestHelper(SDFFullRobotModel fullRobotModel, SDFRobot robot, boolean visualize, double gravityZ)
   {
      this.fullRobotModel = fullRobotModel;
      this.robot = robot;

      FloatingJoint rootJoint = robot.getRootJoint();
      rootJointExternalForcePoint = new ExternalForcePoint("rootJointExternalForce", robot);
      rootJoint.addExternalForcePoint(rootJointExternalForcePoint);
      
      List<GroundContactPoint> footGroundContactPoints = robot.getFootGroundContactPoints(RobotSide.LEFT);
      Joint leftAnkleJoint = footGroundContactPoints.get(0).getParentJoint();
      
      footGroundContactPoints = robot.getFootGroundContactPoints(RobotSide.RIGHT);
      Joint rightAnkleJoint = footGroundContactPoints.get(0).getParentJoint();
      
      IMUMount leftIMUMount = new IMUMount("leftIMU", new RigidBodyTransform(), robot);
      leftAnkleJoint.addIMUMount(leftIMUMount);
      IMUMount rightIMUMount = new IMUMount("rightIMU", new RigidBodyTransform(), robot);
      rightAnkleJoint.addIMUMount(rightIMUMount);
      
      ExternalForcePoint leftFootExternalForcePoint = new ExternalForcePoint("leftFootExternalForcePoint", robot);
      leftAnkleJoint.addExternalForcePoint(leftFootExternalForcePoint);
      
      ExternalForcePoint rightFootExternalForcePoint = new ExternalForcePoint("rightFootExternalForcePoint", robot);
      rightAnkleJoint.addExternalForcePoint(rightFootExternalForcePoint);
      
      feetExternalForcePoints = new SideDependentList<ExternalForcePoint>(leftFootExternalForcePoint, rightFootExternalForcePoint);
            
      computedRootJointAngularAcceleration = new YoFrameVector("qdd_computed_root_angular", fullRobotModel.getRootJoint().getFrameAfterJoint(), registry);

      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());

         DoubleYoVariable computedJointTorque = new DoubleYoVariable("tau_computed_" + oneDegreeOfFreedomJoint.getName(), registry);
         computedJointTorques.put(oneDoFJoint, computedJointTorque);
         
         DoubleYoVariable computedJointAcceleration = new DoubleYoVariable("qdd_computed_" + oneDegreeOfFreedomJoint.getName(), registry);
         computedJointAccelerations.put(oneDoFJoint, computedJointAcceleration);
      }

      twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), fullRobotModel.getElevator());
      double gravity = -robot.getGravityZ();
      inverseDynamicsCalculator = new InverseDynamicsCalculator(twistCalculator, gravity);

      robot.addYoVariableRegistry(registry);

      if (visualize)
      {
         scs = new SimulationConstructionSet(robot, simulationTestingParameters);
         double simulateDT = 0.00001;
         int recordFrequency = 1;
         scs.setDT(simulateDT, recordFrequency);
      }

      else
      {
         scs = null;
      }
   }

   public void startSimulationOnAThread()
   {
      if (scs != null)
         scs.startOnAThread();
   }

   public void setRobotTorquesToMatchFullRobotModel()
   {
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      SixDoFJoint rootJoint = fullRobotModel.getRootJoint();
      ReferenceFrame bodyFixedFrame = fullRobotModel.getPelvis().getBodyFixedFrame();
      
      Wrench rootJointWrench = new Wrench(bodyFixedFrame, bodyFixedFrame);
      rootJoint.packWrench(rootJointWrench);

      FrameVector rootJointForce = rootJointWrench.getLinearPartAsFrameVectorCopy();
      FrameVector rootJointTorque = rootJointWrench.getAngularPartAsFrameVectorCopy();
      
      rootJointForce.changeFrame(ReferenceFrame.getWorldFrame());
      rootJointTorque.changeFrame(ReferenceFrame.getWorldFrame());

      computedRootJointForces.set(rootJointForce);
      computedRootJointTorques.set(rootJointTorque);
      
      rootJointExternalForcePoint.setForce(rootJointForce.getVectorCopy());
      rootJointExternalForcePoint.setMoment(rootJointTorque.getVectorCopy());
      
      FramePoint rootJointPosition = new FramePoint(bodyFixedFrame);
      rootJointPosition.changeFrame(ReferenceFrame.getWorldFrame());
      
      rootJointExternalForcePoint.setOffsetWorld(rootJointPosition.getPointCopy());
      Vector3d offsetInJoint = rootJointExternalForcePoint.getOffsetCopy();
      
      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());

         double inverseDynamicsTorque = oneDoFJoint.getTau();
         oneDegreeOfFreedomJoint.setTau(inverseDynamicsTorque);
         
         DoubleYoVariable computedJointTorque = computedJointTorques.get(oneDoFJoint);
         computedJointTorque.set(inverseDynamicsTorque);
      }      
   }
   
   public boolean checkAccelerationsMatchBetweenFullRobotModelAndSimulatedRobot(double epsilon)
   {
      SixDoFJoint sixDoFJoint = fullRobotModel.getRootJoint();
      FloatingJoint floatingJoint = robot.getRootJoint();
      boolean allAccelerationsMatch = checkFullRobotModelRootJointAccelerationmatchesRobot(floatingJoint, sixDoFJoint, epsilon);

      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);
      
      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());

         double inverseDynamicsAcceleration = oneDoFJoint.getQddDesired();
         double simulatedRobotAcceleration = oneDegreeOfFreedomJoint.getQDD().getDoubleValue();

         DoubleYoVariable computedJointAcceleration = computedJointAccelerations.get(oneDoFJoint);
         computedJointAcceleration.set(inverseDynamicsAcceleration);

         boolean accelerationsMatch = Math.abs(inverseDynamicsAcceleration - simulatedRobotAcceleration) < epsilon;
         if (!accelerationsMatch)
            allAccelerationsMatch = false;
      }

      return allAccelerationsMatch;
   }
   
   
   public boolean checkTorquesMatchBetweenFullRobotModelAndSimulatedRobot(double epsilon)
   {
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      SixDoFJoint rootJoint = fullRobotModel.getRootJoint();
      Wrench rootJointWrench = new Wrench(rootJoint.getFrameAfterJoint(), rootJoint.getFrameAfterJoint());
      rootJoint.packWrench(rootJointWrench);

      FrameVector rootJointForce = rootJointWrench.getLinearPartAsFrameVectorCopy();
      FrameVector rootJointTorque = rootJointWrench.getAngularPartAsFrameVectorCopy();

      rootJointForce.changeFrame(ReferenceFrame.getWorldFrame());
      rootJointTorque.changeFrame(ReferenceFrame.getWorldFrame());
      
      computedRootJointForces.set(rootJointForce);
      computedRootJointTorques.set(rootJointTorque);
      
      Vector3d simulatedRootJointForce = new Vector3d();
      Vector3d simulatedRootJointTorque = new Vector3d();

      rootJointExternalForcePoint.getForce(simulatedRootJointForce);
      rootJointExternalForcePoint.getMoment(simulatedRootJointTorque);
      
      Vector3d forceErrorVector = rootJointForce.getVectorCopy();
      forceErrorVector.sub(simulatedRootJointForce);
      double rootJointForceError = forceErrorVector.length();
      
      Vector3d torqueErrorVector = rootJointTorque.getVectorCopy();
      torqueErrorVector.sub(simulatedRootJointTorque);
      double rootJointTorqueError = forceErrorVector.length();

      boolean allTorquesMatch = ((rootJointForceError < epsilon) && (rootJointTorqueError < epsilon));

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());

         double inverseDynamicsTorque = oneDoFJoint.getTau();
         double simulatedRobotTorque = oneDegreeOfFreedomJoint.getTau().getDoubleValue();

         DoubleYoVariable computedJointTorque = computedJointTorques.get(oneDoFJoint);
         computedJointTorque.set(inverseDynamicsTorque);

         boolean torquesMatch = Math.abs(inverseDynamicsTorque - simulatedRobotTorque) < epsilon;
         if (!torquesMatch)
            allTorquesMatch = false;
      }

      return allTorquesMatch;
   }

   public boolean checkComputedRootJointWrenchIsZero(double epsilon)
   {
      if (computedRootJointForces.length() > epsilon)
         return false;
      if (computedRootJointTorques.length() > epsilon)
         return false;

      return true;
   }

   public void setFullRobotModelStateAndAccelerationToMatchRobot()
   {
      setFullRobotModelStateToMatchRobot();
      setFullRobotModelAccelerationToMatchRobot();
   }

   public void setFullRobotModelStateToMatchRobot()
   {
      robot.update();

      SixDoFJoint sixDoFJoint = fullRobotModel.getRootJoint();
      FloatingJoint floatingJoint = robot.getRootJoint();

      setFullRobotModelRootJointPositionAndOrientationToMatchRobot(sixDoFJoint, floatingJoint);
      fullRobotModel.updateFrames();
      setFullRobotModelRootJointVelocityAndAngularVelocityToMatchRobot(sixDoFJoint, floatingJoint);

      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());

         oneDoFJoint.setQ(oneDegreeOfFreedomJoint.getQ().getDoubleValue());
         oneDoFJoint.setQd(oneDegreeOfFreedomJoint.getQD().getDoubleValue());
      }
   }
   
   public void setRobotStateToMatchFullRobotModel()
   {
      SixDoFJoint sixDoFJoint = fullRobotModel.getRootJoint();
      FloatingJoint floatingJoint = robot.getRootJoint();

      fullRobotModel.updateFrames();

      setRobotRootJointPositionAndOrientationToMatchFullRobotModel(sixDoFJoint, floatingJoint);
      setRobotRootJointVelocityAndAngularVelocityToMatchFullRobotModel(sixDoFJoint, floatingJoint);

      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());

         oneDegreeOfFreedomJoint.setQ(oneDoFJoint.getQ());
         oneDegreeOfFreedomJoint.setQd(oneDoFJoint.getQd());
      }
      
      robot.update();
   }

   public void setFullRobotModelAccelerationRandomly(Random random, double maxPelvisLinearAcceleration, double maxPelvisAngularAcceleration, double maxJointAcceleration)
   {
      SixDoFJoint sixDoFJoint = fullRobotModel.getRootJoint();
      setSixDoFJointAccelerationRandomly(sixDoFJoint, random, maxPelvisLinearAcceleration, maxPelvisAngularAcceleration);
      
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());
         oneDoFJoint.setQddDesired(RandomTools.generateRandomDouble(random, maxJointAcceleration));
      }      
   }
   
   
   public void setFullRobotModelAccelerationToMatchRobot()
   {
      robot.update();

      SixDoFJoint sixDoFJoint = fullRobotModel.getRootJoint();
      FloatingJoint floatingJoint = robot.getRootJoint();

      fullRobotModel.updateFrames();

      copyAccelerationFromForwardToInverse(floatingJoint, sixDoFJoint);

      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(oneDegreeOfFreedomJoint.getName());
         
         double robotJointAcceleration = oneDegreeOfFreedomJoint.getQDD().getDoubleValue();
         oneDoFJoint.setQddDesired(robotJointAcceleration);
         
         DoubleYoVariable computedJointAcceleration = computedJointAccelerations.get(oneDoFJoint);
         computedJointAcceleration.set(robotJointAcceleration);
      }
   }

   public void setFullRobotModelWrenchesToMatchRobot()
   {
      inverseDynamicsCalculator.reset();

      ArrayList<WrenchCalculatorInterface> groundContactPointBasedWrenchCalculators = new ArrayList<WrenchCalculatorInterface>();
      robot.getForceSensors(groundContactPointBasedWrenchCalculators);

      for (WrenchCalculatorInterface groundContactPointBasedWrenchCalculator : groundContactPointBasedWrenchCalculators)
      {
         if (groundContactPointBasedWrenchCalculator instanceof GroundContactPointBasedWrenchCalculator)
         {
            OneDegreeOfFreedomJoint joint = groundContactPointBasedWrenchCalculator.getJoint();
            OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(joint.getName());

            RigidBody rigidBodyToApplyWrenchTo = oneDoFJoint.getSuccessor();
            ReferenceFrame bodyFixedFrame = rigidBodyToApplyWrenchTo.getBodyFixedFrame();

            groundContactPointBasedWrenchCalculator.calculate();
            DenseMatrix64F wrenchFromSimulation = groundContactPointBasedWrenchCalculator.getWrench();
            ReferenceFrame frameAtJoint = rigidBodyToApplyWrenchTo.getParentJoint().getFrameAfterJoint();

            Wrench wrench = new Wrench(frameAtJoint, frameAtJoint, wrenchFromSimulation);
            wrench.changeBodyFrameAttachedToSameBody(bodyFixedFrame);
            wrench.changeFrame(bodyFixedFrame);

            inverseDynamicsCalculator.setExternalWrench(rigidBodyToApplyWrenchTo, wrench);
         }
      }
      
      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody foot = fullRobotModel.getFoot(robotSide);

         Wrench wrench = new Wrench();
         inverseDynamicsCalculator.getExternalWrench(foot, wrench);
         
         ReferenceFrame footFrame = foot.getBodyFixedFrame();

         ExternalForcePoint footExternalForcePoint = feetExternalForcePoints.get(robotSide);
         
         
         Vector3d externalForce = new Vector3d();
         Vector3d externalMoment = new Vector3d();
         
         footExternalForcePoint.getForce(externalForce);
         footExternalForcePoint.getMoment(externalMoment);
//         System.out.println("externalMoment = " + externalMoment);

         FrameVector externalForcePointForce = new FrameVector(ReferenceFrame.getWorldFrame(), externalForce);
         FrameVector externalForcePointMoment = new FrameVector(ReferenceFrame.getWorldFrame(), externalMoment);
         
         externalForcePointForce.changeFrame(footFrame);
         externalForcePointMoment.changeFrame(footFrame);
         
         Point3d position = new Point3d();
         footExternalForcePoint.getPosition(position);
         FramePoint pointOfApplication = new FramePoint(ReferenceFrame.getWorldFrame(), position);
         pointOfApplication.changeFrame(footFrame);
         
         Vector3d torqueFromLeverArm = new Vector3d();
         Vector3d leverArm = pointOfApplication.getVectorCopy();
         torqueFromLeverArm.cross(leverArm, externalForcePointForce.getVectorCopy());
         
         externalForcePointMoment.add(torqueFromLeverArm);
         
         
         
         FrameVector totalTorqueOnFoot = wrench.getAngularPartAsFrameVectorCopy();
         FrameVector totalForceOnFoot = wrench.getLinearPartAsFrameVectorCopy();
         
         totalTorqueOnFoot.add(externalForcePointMoment);
         totalForceOnFoot.add(externalForcePointForce);

         wrench.setAngularPart(totalTorqueOnFoot);
         wrench.setLinearPart(totalForceOnFoot);
         
         inverseDynamicsCalculator.setExternalWrench(foot, wrench);
      }
   }

   public void setFullRobotModelRootJointVelocityAndAngularVelocityToMatchRobot(SixDoFJoint sixDoFJoint, FloatingJoint floatingJoint)
   {
      FrameVector angularVelocityFrameVector = new FrameVector();
      FrameVector linearVelocityFrameVector = new FrameVector();

      ReferenceFrame elevatorFrame = sixDoFJoint.getFrameBeforeJoint();
      ReferenceFrame bodyFrame = sixDoFJoint.getFrameAfterJoint();

      floatingJoint.getVelocity(linearVelocityFrameVector);
      linearVelocityFrameVector.changeFrame(bodyFrame);
      floatingJoint.getAngularVelocity(angularVelocityFrameVector, bodyFrame);

      Twist bodyTwist = new Twist(bodyFrame, elevatorFrame, bodyFrame, linearVelocityFrameVector.getVector(), angularVelocityFrameVector.getVector());
      sixDoFJoint.setJointTwist(bodyTwist);
   }

   
   public void setRobotRootJointVelocityAndAngularVelocityToMatchFullRobotModel(SixDoFJoint sixDoFJoint, FloatingJoint floatingJoint)
   {
      Twist rootJointTwist = new Twist();
      sixDoFJoint.packJointTwist(rootJointTwist);
      
      floatingJoint.setAngularVelocityInBody(rootJointTwist.getAngularPartCopy());
      
      FrameVector linearVelocityInWorld = new FrameVector();
      rootJointTwist.packLinearPart(linearVelocityInWorld);
      
      linearVelocityInWorld.changeFrame(ReferenceFrame.getWorldFrame());
      floatingJoint.setVelocity(linearVelocityInWorld.getVectorCopy());
   }
   

   public void setFullRobotModelRootJointPositionAndOrientationToMatchRobot(SixDoFJoint sixDoFJoint, FloatingJoint floatingJoint)
   {
      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      floatingJoint.getTransformToWorld(transformToWorld);
      sixDoFJoint.setPositionAndRotation(transformToWorld);
   }
   
   public void setRobotRootJointPositionAndOrientationToMatchFullRobotModel(SixDoFJoint sixDoFJoint, FloatingJoint floatingJoint)
   {
      RigidBodyTransform transform = new RigidBodyTransform(sixDoFJoint.getJointTransform3D());
      floatingJoint.setRotationAndTranslation(transform);
   }

   public void setFullRobotModelStateRandomly(Random random, double maxJointVelocity, double maxRootJointLinearAndAngularVelocity)
   {
      SixDoFJoint rootJoint = fullRobotModel.getRootJoint();
      
      ReferenceFrame elevatorFrame = rootJoint.getFrameBeforeJoint();
      ReferenceFrame bodyFrame = rootJoint.getFrameAfterJoint();

      Twist bodyTwist = new Twist(bodyFrame, elevatorFrame, bodyFrame, RandomTools.generateRandomVector(random, maxRootJointLinearAndAngularVelocity), RandomTools.generateRandomVector(random, maxRootJointLinearAndAngularVelocity));
      rootJoint.setJointTwist(bodyTwist);
      
      rootJoint.setPosition(RandomTools.generateRandomVector(random));

      double yaw = RandomTools.generateRandomDouble(random, Math.PI / 20.0);
      double pitch = RandomTools.generateRandomDouble(random, Math.PI / 20.0);
      double roll = RandomTools.generateRandomDouble(random, Math.PI / 20.0);
      rootJoint.setRotation(yaw, pitch, roll);

      
      ArrayList<OneDoFJoint> oneDoFJoints = new ArrayList<OneDoFJoint>();
      fullRobotModel.getOneDoFJoints(oneDoFJoints);

      for (OneDoFJoint oneDoFJoint : oneDoFJoints)
      {
         double lowerLimit = oneDoFJoint.getJointLimitLower();
         double upperLimit = oneDoFJoint.getJointLimitUpper();
         double delta = upperLimit - lowerLimit;
         lowerLimit = lowerLimit + 0.05 * delta;
         upperLimit = upperLimit - 0.05 * delta;

         oneDoFJoint.setQ(RandomTools.generateRandomDouble(random, lowerLimit, upperLimit));
         oneDoFJoint.setQd(RandomTools.generateRandomDouble(random, maxJointVelocity));
      }
   }
   
   
   public void setRobotStateRandomly(Random random, double maxJointVelocity, double maxRootJointLinearAndAngularVelocity)
   {
      FloatingJoint rootJoint = robot.getRootJoint();
      rootJoint.setVelocity(RandomTools.generateRandomVector(random, maxRootJointLinearAndAngularVelocity));
      rootJoint.setAngularVelocityInBody(RandomTools.generateRandomVector(random, maxRootJointLinearAndAngularVelocity));

      rootJoint.setPosition(RandomTools.generateRandomVector(random));
      double yaw = RandomTools.generateRandomDouble(random, Math.PI / 20.0);
      double pitch = RandomTools.generateRandomDouble(random, Math.PI / 20.0);
      double roll = RandomTools.generateRandomDouble(random, Math.PI / 20.0);
      rootJoint.setYawPitchRoll(yaw, pitch, roll);


      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         double lowerLimit = oneDegreeOfFreedomJoint.getJointLowerLimit();
         double upperLimit = oneDegreeOfFreedomJoint.getJointUpperLimit();
         double delta = upperLimit - lowerLimit;
         lowerLimit = lowerLimit + 0.05 * delta;
         upperLimit = upperLimit - 0.05 * delta;

         oneDegreeOfFreedomJoint.setQ(RandomTools.generateRandomDouble(random, lowerLimit, upperLimit));
         oneDegreeOfFreedomJoint.setQd(RandomTools.generateRandomDouble(random, maxJointVelocity));
      }
   }

   public void setRobotExternalForcesRandomly(Random random, double maxGroundContactPointForce, double maxFootExternalForce, double maxFootExternalTorque)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         List<GroundContactPoint> footGroundContactPoints = robot.getFootGroundContactPoints(robotSide);

         for (GroundContactPoint groundContactPoint : footGroundContactPoints)
         {
            groundContactPoint.setForce(RandomTools.generateRandomVector(random, maxGroundContactPointForce));
         }
         
         ExternalForcePoint footExternalForcePoint = feetExternalForcePoints.get(robotSide);
         footExternalForcePoint.setForce(RandomTools.generateRandomVector(random, maxFootExternalForce));
         footExternalForcePoint.setMoment(RandomTools.generateRandomVector(random, maxFootExternalTorque));
      }
   }
   
   public void setRobotsExternalForcesToMatchFullRobotModel()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody foot = fullRobotModel.getFoot(robotSide);

         Wrench wrench = new Wrench();
         inverseDynamicsCalculator.getExternalWrench(foot, wrench);
         
         ReferenceFrame bodyFixedFrame = foot.getBodyFixedFrame();
         FramePoint pointOfWrenchApplication = new FramePoint(bodyFixedFrame);
         pointOfWrenchApplication.changeFrame(ReferenceFrame.getWorldFrame());
         
         ExternalForcePoint footExternalForcePoint = feetExternalForcePoints.get(robotSide);
         footExternalForcePoint.setOffsetWorld(pointOfWrenchApplication.getPointCopy());
         
         FrameVector wrenchForce = wrench.getLinearPartAsFrameVectorCopy();
         wrenchForce.changeFrame(ReferenceFrame.getWorldFrame());
         
         FrameVector wrenchTorque = wrench.getAngularPartAsFrameVectorCopy();
         wrenchTorque.changeFrame(ReferenceFrame.getWorldFrame());
         
         footExternalForcePoint.setForce(wrenchForce.getVectorCopy());
         footExternalForcePoint.setMoment(wrenchTorque.getVectorCopy());
      }
   }

   public void setFullRobotModelExternalForcesRandomly(Random random, double maxFeetExternalForce, double maxFeetExternalTorque)
   {
      inverseDynamicsCalculator.reset();

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody foot = fullRobotModel.getFoot(robotSide);
         ReferenceFrame bodyFixedFrame = foot.getBodyFixedFrame();
         
         Wrench wrench = new Wrench(bodyFixedFrame, bodyFixedFrame, RandomTools.generateRandomVector(random, maxFeetExternalForce), RandomTools.generateRandomVector(random, maxFeetExternalTorque));
         inverseDynamicsCalculator.setExternalWrench(foot, wrench);
      }
   }
   
   public void setRobotRootJointExternalForcesRandomly(Random random, double maxRootJointForceAndTorque)
   {
      rootJointExternalForcePoint.setForce(RandomTools.generateRandomVector(random, maxRootJointForceAndTorque));
      rootJointExternalForcePoint.setMoment(RandomTools.generateRandomVector(random, maxRootJointForceAndTorque));
   }

   public void setRobotTorquesRandomly(Random random, double maxJointTorque)
   {
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         oneDegreeOfFreedomJoint.setTau(RandomTools.generateRandomDouble(random, maxJointTorque));
      }
   }

   public void copyAccelerationFromForwardToInverseBroken(FloatingJoint floatingJoint, SixDoFJoint sixDoFJoint)
   {
      ReferenceFrame elevatorFrame = sixDoFJoint.getFrameBeforeJoint();
      SixDoFJointReferenceFrame bodyFrame = sixDoFJoint.getFrameAfterJoint();

      FrameVector angularAccelerationInBody = new FrameVector();
      floatingJoint.getAngularAcceleration(angularAccelerationInBody, bodyFrame);
      FrameVector linearAccelerationInBody = new FrameVector();
      floatingJoint.getLinearAcceleration(linearAccelerationInBody);
      linearAccelerationInBody.changeFrame(bodyFrame);


      SpatialAccelerationVector jointAcceleration = new SpatialAccelerationVector(bodyFrame, elevatorFrame, bodyFrame);
      jointAcceleration.setLinearPart(linearAccelerationInBody);
      jointAcceleration.setAngularPart(angularAccelerationInBody);

      sixDoFJoint.setDesiredAcceleration(jointAcceleration);
   }

   public void setSixDoFJointAccelerationRandomly(SixDoFJoint sixDoFJoint, Random random, double maxRootJointLinearAcceleration, double maxRootJointAngularAcceleration)
   {
      // Note: To get the acceleration, you can't just changeFrame on the acceleration provided by SCS. Use setBasedOnOriginAcceleration instead.
      ReferenceFrame elevatorFrame = sixDoFJoint.getFrameBeforeJoint();
      SixDoFJointReferenceFrame bodyFrame = sixDoFJoint.getFrameAfterJoint();

      Twist bodyTwist = new Twist();
      sixDoFJoint.packJointTwist(bodyTwist);

      FrameVector originAcceleration = new FrameVector(elevatorFrame, RandomTools.generateRandomVector(random, maxRootJointLinearAcceleration));
      FrameVector angularAcceleration = new FrameVector(bodyFrame, RandomTools.generateRandomVector(random, maxRootJointAngularAcceleration));
//      originAcceleration.changeFrame(elevatorFrame);

      SpatialAccelerationVector spatialAccelerationVector = new SpatialAccelerationVector(bodyFrame, elevatorFrame, bodyFrame);

      spatialAccelerationVector.setBasedOnOriginAcceleration(angularAcceleration, originAcceleration, bodyTwist);
      sixDoFJoint.setDesiredAcceleration(spatialAccelerationVector);
   }
   
   public void copyAccelerationFromForwardToInverse(FloatingJoint floatingJoint, SixDoFJoint sixDoFJoint)
   {
      // Note: To get the acceleration, you can't just changeFrame on the acceleration provided by SCS. Use setBasedOnOriginAcceleration instead.
      ReferenceFrame elevatorFrame = sixDoFJoint.getFrameBeforeJoint();
      SixDoFJointReferenceFrame bodyFrame = sixDoFJoint.getFrameAfterJoint();

      Twist bodyTwist = new Twist();
      sixDoFJoint.packJointTwist(bodyTwist);

      FrameVector originAcceleration = new FrameVector(elevatorFrame);
      FrameVector angularAcceleration = new FrameVector(bodyFrame);

      floatingJoint.getLinearAccelerationInWorld(originAcceleration.getVector());
      floatingJoint.getAngularAccelerationInBody(angularAcceleration.getVector());
      originAcceleration.changeFrame(elevatorFrame);

      SpatialAccelerationVector spatialAccelerationVector = new SpatialAccelerationVector(bodyFrame, elevatorFrame, bodyFrame);

      spatialAccelerationVector.setBasedOnOriginAcceleration(angularAcceleration, originAcceleration, bodyTwist);
      sixDoFJoint.setDesiredAcceleration(spatialAccelerationVector);
   }
   
   public boolean checkFullRobotModelRootJointAccelerationmatchesRobot(FloatingJoint floatingJoint, SixDoFJoint sixDoFJoint, double epsilon)
   {
      // Note: To get the acceleration, you can't just changeFrame on the acceleration provided by SCS. Use setBasedOnOriginAcceleration instead.
      ReferenceFrame elevatorFrame = sixDoFJoint.getFrameBeforeJoint();
      SixDoFJointReferenceFrame bodyFrame = sixDoFJoint.getFrameAfterJoint();

      Twist bodyTwist = new Twist();
      sixDoFJoint.packJointTwist(bodyTwist);

      FrameVector originAcceleration = new FrameVector(elevatorFrame);
      FrameVector angularAcceleration = new FrameVector(bodyFrame);

      floatingJoint.getLinearAccelerationInWorld(originAcceleration.getVector());
      floatingJoint.getAngularAccelerationInBody(angularAcceleration.getVector());
      originAcceleration.changeFrame(elevatorFrame);

      //TODO: These should be from the inverse dynamics to the SCS frames...
      originAcceleration.changeFrame(ReferenceFrame.getWorldFrame());
      computedRootJointLinearAcceleration.set(originAcceleration);
      computedRootJointAngularAcceleration.set(angularAcceleration);
      
      SpatialAccelerationVector spatialAccelerationVectorOfSimulatedRootJoint = new SpatialAccelerationVector(bodyFrame, elevatorFrame, bodyFrame);
      spatialAccelerationVectorOfSimulatedRootJoint.setBasedOnOriginAcceleration(angularAcceleration, originAcceleration, bodyTwist);
      
      SpatialAccelerationVector spatialAccelerationVectorOfInverseDynamicsRootJoint = new SpatialAccelerationVector();
      sixDoFJoint.packDesiredJointAcceleration(spatialAccelerationVectorOfInverseDynamicsRootJoint);
      
      return spatialAccelerationVectorOfInverseDynamicsRootJoint.epsilonEquals(spatialAccelerationVectorOfInverseDynamicsRootJoint, epsilon);
   }

   public void computeTwistCalculatorAndInverseDynamicsCalculator()
   {
      twistCalculator.compute();
      inverseDynamicsCalculator.compute();
   }

   public Robot getRobot()
   {
      return robot;
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return scs;
   }

   public SimulationTestingParameters getSimulationTestingParameters()
   {
      return simulationTestingParameters;
   }

   public SDFFullRobotModel getFullRobotModel()
   {
      return fullRobotModel;
   }

}
