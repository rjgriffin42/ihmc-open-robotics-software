package us.ihmc.sensorProcessing.simulatedSensors;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsCalculator;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.utilities.test.JUnitTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameQuaternion;

import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.RandomRobotGenerator;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.UnreasonableAccelerationException;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public class InverseDynamicsJointsFromSCSRobotGeneratorTest
{
   private static final boolean SHOW_SCS = false;
   private static final boolean KEEP_SCS_UP = false;
   private static final boolean DO_ASSERTS = true;
   private static final boolean DO_TWIST_ASSERTS = true;

   private BlockingSimulationRunner blockingSimulationRunner;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }
   
   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (KEEP_SCS_UP)
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (blockingSimulationRunner != null)
      {
         blockingSimulationRunner.destroySimulation();
         blockingSimulationRunner = null;
      }
      
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   @Test(timeout=300000)
   public void testSinglePinJoint() throws SimulationExceededMaximumTimeException
   {
      Robot robot = new Robot("TestSinglePinJoint");

      final PinJoint joint1 = new PinJoint("joint1", new Vector3d(), robot, Axis.Z);
      Link link1 = new Link("link1");
      link1.setMassAndRadiiOfGyration(1.0, 0.1, 0.1, 0.1);
      link1.setComOffset(new Vector3d(0.0, 0.2, 0.11));
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCylinder(0.1, 1.0, YoAppearance.Red());
      linkGraphics.translate(0.5, 0.0, 0.1);
      linkGraphics.addCylinder(0.1, 0.05, YoAppearance.Black());
      link1.setLinkGraphics(linkGraphics);
      joint1.setLink(link1);
      joint1.setQd(1.0);

      robot.addRootJoint(joint1);

      final InverseDynamicsJointsFromSCSRobotGenerator generator = new InverseDynamicsJointsFromSCSRobotGenerator(robot);

      VerifyGeneratorController controller = new VerifyGeneratorController(robot, generator);
      robot.setController(controller);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot, SHOW_SCS);
      scs.setDT(0.001, 1);
      scs.setGroundVisible(false);
      scs.startOnAThread();

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);
      blockingSimulationRunner.simulateAndBlock(2.0);
   }
   
   
   @Test(timeout=300000)
   public void testTwoPinJoints() throws SimulationExceededMaximumTimeException
   {
      Robot robot = new Robot("TestTwoPinJoints");

      PinJoint joint1 = new PinJoint("joint1", new Vector3d(), robot, Axis.Z);
      Link link1 = new Link("link1");
      link1.setMassAndRadiiOfGyration(1.0, 0.1, 0.1, 0.1);
      link1.setComOffset(new Vector3d(0.25, 0.0, 0.0));
     
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.rotate(Math.PI/2.0, Axis.Y);
      linkGraphics.addCylinder(0.5, 0.05, YoAppearance.Gold());
      link1.setLinkGraphics(linkGraphics);
      joint1.setLink(link1);
      
      joint1.setQ(0.5);
      joint1.setQd(1.0);

      robot.addRootJoint(joint1);

      PinJoint joint2 = new PinJoint("joint2", new Vector3d(0.5, 0.0, 0.0), robot, Axis.Z);
      Link link2 = new Link("link2");
      link2.setMassAndRadiiOfGyration(1.0, 0.1, 0.1, 0.1);
      link2.setComOffset(new Vector3d(0.25, 0.0, 0.0));
      
      Graphics3DObject linkGraphics2 = new Graphics3DObject();
      linkGraphics2.rotate(Math.PI/2.0, Axis.Y);
      linkGraphics2.addCylinder(0.5, 0.05, YoAppearance.Red());
      
      link2.setLinkGraphics(linkGraphics2);
      joint2.setLink(link2);
      
      joint2.setQ(0.6);
      joint2.setQd(1.2);
      
      joint1.addJoint(joint2);
      
      final InverseDynamicsJointsFromSCSRobotGenerator generator = new InverseDynamicsJointsFromSCSRobotGenerator(robot);

      VerifyGeneratorController controller = new VerifyGeneratorController(robot, generator);
      robot.setController(controller);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot, SHOW_SCS);
      scs.setDT(0.001, 1);
      scs.setGroundVisible(false);
      scs.startOnAThread();

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);
      blockingSimulationRunner.simulateAndBlock(2.0);
   }
   
   
   @Test(timeout=300000)
   public void testSingleFloatingJoint() throws SimulationExceededMaximumTimeException
   {
      Robot robot = new Robot("TestSingleFloatingJoint");

      final FloatingJoint joint1 = new FloatingJoint("joint1", new Vector3d(), robot);
      Link link1 = new Link("link1");
      link1.setMassAndRadiiOfGyration(1.0, 0.1, 0.1, 0.1);
      link1.setComOffset(new Vector3d());
      link1.setComOffset(new Vector3d(0.0, 0.2, 0.11));
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addSphere(1.0, YoAppearance.Red());
      linkGraphics.translate(0.0, 0.0, 1.0);
      linkGraphics.addCylinder(0.1, 0.05, YoAppearance.Black());
      link1.setLinkGraphics(linkGraphics);
      joint1.setLink(link1);

      joint1.setAngularVelocityInBody(new Vector3d(0.7, 2.0, 1.0));
      joint1.setVelocity(new Vector3d(0.0, 0.0, 1.0));
      
      robot.addRootJoint(joint1);
      robot.setGravity(0.05);
      
      final InverseDynamicsJointsFromSCSRobotGenerator generator = new InverseDynamicsJointsFromSCSRobotGenerator(robot);

      VerifyGeneratorController controller = new VerifyGeneratorController(robot, generator);
      robot.setController(controller);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot, SHOW_SCS);
      scs.setDT(0.001, 1);
      scs.setGroundVisible(false);
      scs.startOnAThread();

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);
      blockingSimulationRunner.simulateAndBlock(2.0);
   }



   @Test(timeout=300000)
   public void testRandomLinearChainRobot() throws SimulationExceededMaximumTimeException
   {
      Random random = new Random(1984L);
      int numberOfJoints = 10;

      boolean startWithFloatingJoint = true;
      Robot robot = RandomRobotGenerator.generateRandomLinearChainRobot("TestLinearChainRobot", startWithFloatingJoint , numberOfJoints, random);
      RandomRobotGenerator.setRandomJointPositions(robot, random);
      RandomRobotGenerator.setRandomJointVelocities(robot, random);
      robot.setGravity(0.05);

      final InverseDynamicsJointsFromSCSRobotGenerator generator = new InverseDynamicsJointsFromSCSRobotGenerator(robot);

      VerifyGeneratorController controller = new VerifyGeneratorController(robot, generator);
      robot.setController(controller);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot, SHOW_SCS);
      scs.setDT(0.0001, 1);
      scs.setGroundVisible(false);
      scs.startOnAThread();

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);
      blockingSimulationRunner.simulateAndBlock(2.0);
   }

   private class VerifyGeneratorController implements RobotController
   {
      private final InverseDynamicsJointsFromSCSRobotGenerator generator;
      private final SCSToInverseDynamicsJointMap scsToInverseDynamicsJointMap;
      
      private final Robot robot;
      
      private final Random random = new Random(1984L);
      
      private final ReferenceFrame inertialFrame = ReferenceFrame.getWorldFrame();
      private final RigidBody elevator;

      private final ArrayList<FloatingJoint> floatingJoints = new ArrayList<FloatingJoint>();
      private final ArrayList<OneDegreeOfFreedomJoint> pinJoints = new ArrayList<OneDegreeOfFreedomJoint>();

      private final Joint lastJoint;
      private final InverseDynamicsJoint lastInverseDynamicsJoint;

      private final YoVariableRegistry registry = new YoVariableRegistry("Controller");
      
      private final YoFrameQuaternion lastFrameOrientationID = new YoFrameQuaternion("lastFrameID", ReferenceFrame.getWorldFrame(), registry);
      private final YoFrameQuaternion lastFrameOrientation = new YoFrameQuaternion("lastFrame", ReferenceFrame.getWorldFrame(), registry);

      private final ArrayList<DoubleYoVariable> tauErrors = new ArrayList<DoubleYoVariable>();
      private final ArrayList<DoubleYoVariable> inverseDynamicsTaus = new ArrayList<DoubleYoVariable>();

      private final ArrayList<DoubleYoVariable> wrenchLinearPartErrors = new ArrayList<DoubleYoVariable>();
      private final ArrayList<DoubleYoVariable> wrenchAngularPartErrors = new ArrayList<DoubleYoVariable>();
      
      private final TwistCalculator twistCalculator;
      private final InverseDynamicsCalculator inverseDynamicsCalculator;


      public VerifyGeneratorController(Robot robot, InverseDynamicsJointsFromSCSRobotGenerator generator)
      {
         this.robot = robot;
         this.generator = generator;

         this.elevator = generator.getElevator();
         
         scsToInverseDynamicsJointMap = generator.getSCSToInverseDynamicsJointMap();
         
         this.floatingJoints.addAll(scsToInverseDynamicsJointMap.getFloatingJoints());
         this.pinJoints.addAll(scsToInverseDynamicsJointMap.getSCSOneDegreeOfFreedomJoints());
         
         this.twistCalculator = new TwistCalculator(inertialFrame, elevator);
         this.inverseDynamicsCalculator = new InverseDynamicsCalculator(twistCalculator, -robot.getGravityZ());

         for (FloatingJoint floatingJoint : floatingJoints)
         {
            DoubleYoVariable wrenchLinearPartError = new DoubleYoVariable(floatingJoint.getName() + "LinearPartError", registry);
            wrenchLinearPartErrors.add(wrenchLinearPartError);
            
            DoubleYoVariable wrenchAngularPartError = new DoubleYoVariable(floatingJoint.getName() + "AngularPartError", registry);
            wrenchAngularPartErrors.add(wrenchAngularPartError);
         }

         for (OneDegreeOfFreedomJoint pinJoint : pinJoints)
         {
            DoubleYoVariable tauError = new DoubleYoVariable(pinJoint.getName() + "TauError", registry);
            tauErrors.add(tauError);

            DoubleYoVariable inverseDynamicsTau = new DoubleYoVariable(pinJoint.getName() + "InverseDynamicsTau", registry);
            inverseDynamicsTaus.add(inverseDynamicsTau);
         }

         if (!pinJoints.isEmpty())
         {
            lastJoint = pinJoints.get(pinJoints.size() - 1);
            lastInverseDynamicsJoint = scsToInverseDynamicsJointMap.getInverseDynamicsOneDoFJoint((PinJoint) lastJoint);
         }
         else
         {
            lastJoint = floatingJoints.get(0);
            lastInverseDynamicsJoint = scsToInverseDynamicsJointMap.getInverseDynamicsSixDoFJoint((FloatingJoint) lastJoint);
         }
      }

      public void initialize()
      {
      }

      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      public String getName()
      {
         return "Controller";
      }

      public String getDescription()
      {
         return getName();
      }

      public void doControl()
      {
         // First randomly generate torques:
         for (int i = 0; i < pinJoints.size(); i++)
         {
            OneDegreeOfFreedomJoint pinJoint = pinJoints.get(i);
            double tau = random.nextGaussian() * 10.0;
            pinJoint.setTau(tau);
         }
         
         // Next, we tell the robot to do the dynamics but do not integrate in order to find what the resultant accelerations are:
         try
         {
            robot.doDynamicsButDoNotIntegrate();
         } 
         catch (UnreasonableAccelerationException e)
         {
            throw new RuntimeException();
         }
         
         // Then update the generators state based on the robot's state. This will also set the desired accelerations to the actual accelerations:
         generator.updateInverseDynamicsRobotModelFromRobot(true, true);
         
         // Compute the inverse dynamics:
         twistCalculator.compute();
         inverseDynamicsCalculator.compute();
         
         // Next, extract the inverse dynamics torques and compare to the applied torques:
         for (int i = 0; i < pinJoints.size(); i++)
         {
            OneDegreeOfFreedomJoint pinJoint = pinJoints.get(i);
            double appliedTau = pinJoint.getTau().getDoubleValue();

            OneDoFJoint revoluteJoint = scsToInverseDynamicsJointMap.getInverseDynamicsOneDoFJoint(pinJoint);
            DoubleYoVariable inverseDynamicsTau = inverseDynamicsTaus.get(i);
            inverseDynamicsTau.set(revoluteJoint.getTau());

            DoubleYoVariable tauError = tauErrors.get(i);
            tauError.set(Math.abs(appliedTau - inverseDynamicsTau.getDoubleValue()));

            if (DO_ASSERTS)
            {
               if (tauError.getDoubleValue() > 1e-7)
                  throw new RuntimeException("InverseDynamicsTau doesn't match!");

               if (DO_TWIST_ASSERTS)
               {
                  Vector3d pinJointAngularVelocityInBody = new Vector3d();
                  Vector3d pinJointCoMLinearVelocityInBody = new Vector3d();

                  pinJoint.physics.getAngularVelocityInBody(pinJointAngularVelocityInBody);
                  Vector3d comOffset = new Vector3d();
                  pinJoint.getLink().getComOffset(comOffset);
                  pinJoint.physics.getLinearVelocityInBody(pinJointCoMLinearVelocityInBody, comOffset);

                  FramePoint comOffsetCheck = new FramePoint();
                  revoluteJoint.getSuccessor().packCoMOffset(comOffsetCheck);
                  JUnitTools.assertTuple3dEquals(comOffset, comOffsetCheck.getVectorCopy(), 1e-7);

                  Twist revoluteJointTwist = new Twist();
                  twistCalculator.packTwistOfBody(revoluteJointTwist, revoluteJoint.getSuccessor());
                  revoluteJointTwist.changeFrame(revoluteJoint.getSuccessor().getBodyFixedFrame());
                  
                  Vector3d revoluteJointAngularVelocityInBody = revoluteJointTwist.getAngularPartCopy();
                  Vector3d revoluteJointCoMLinearVelocityInBody = revoluteJointTwist.getLinearPartCopy();

                  JUnitTools.assertTuple3dEquals(pinJointAngularVelocityInBody, revoluteJointAngularVelocityInBody, 1e-7);
                  JUnitTools.assertTuple3dEquals(pinJointCoMLinearVelocityInBody, revoluteJointCoMLinearVelocityInBody, 1e-7);
               }
            }
         }
         
         // And for the FloatingJoints, make sure the necessary applied wrench equals zero:
         for (int i=0; i < floatingJoints.size(); i++)
         {
            FloatingJoint floatingJoint = floatingJoints.get(i);
            
            SixDoFJoint sixDoFJoint = scsToInverseDynamicsJointMap.getInverseDynamicsSixDoFJoint(floatingJoint);
            
            Wrench wrench = new Wrench();
            sixDoFJoint.packWrench(wrench);
            
            Vector3d angularPartCopy = wrench.getAngularPartCopy();
            Vector3d linearPartCopy = wrench.getLinearPartCopy();
            
            DoubleYoVariable wrenchAngularPartError = wrenchAngularPartErrors.get(i);
            DoubleYoVariable wrenchLinearPartError = wrenchLinearPartErrors.get(i);
            
            wrenchAngularPartError.set(angularPartCopy.length());
            wrenchLinearPartError.set(linearPartCopy.length());  
            
            if (DO_ASSERTS)
            {
               assertEquals(0.0, wrenchAngularPartError.getDoubleValue(), 1e-7);
               assertEquals(0.0, wrenchLinearPartError.getDoubleValue(), 1e-7);
            }
         }

         // Now, check the position and velocity kinematics of the last joint:
         ReferenceFrame spinningFrame = lastInverseDynamicsJoint.getFrameAfterJoint();
         FrameOrientation orientation = new FrameOrientation(spinningFrame);
         orientation.changeFrame(ReferenceFrame.getWorldFrame());
         lastFrameOrientationID.set(orientation);

         RigidBodyTransform transformToWorld = new RigidBodyTransform();
         lastJoint.getTransformToWorld(transformToWorld);
         Matrix3d rotationMatrix = new Matrix3d();
         transformToWorld.get(rotationMatrix);
         lastFrameOrientation.set(rotationMatrix);
         
         transformToWorld.invert();
         orientation.applyTransform(transformToWorld);
         double[] yawPitchRoll = orientation.getYawPitchRoll();
         assertEquals(0.0, yawPitchRoll[0], 1e-7);
         assertEquals(0.0, yawPitchRoll[1], 1e-7);
         assertEquals(0.0, yawPitchRoll[2], 1e-7);
         
      }
   }

}
