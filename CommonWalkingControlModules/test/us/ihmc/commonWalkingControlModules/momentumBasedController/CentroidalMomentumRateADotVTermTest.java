package us.ihmc.commonWalkingControlModules.momentumBasedController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.CenterOfMassReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.CenterOfMassCalculator;
import us.ihmc.utilities.screwTheory.CentroidalMomentumMatrix;
import us.ihmc.utilities.screwTheory.CentroidalMomentumRateADotVTerm;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RevoluteJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTestTools;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.TotalMassCalculator;
import us.ihmc.utilities.test.JUnitTools;

import com.yobotics.simulationconstructionset.Link;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.PinJoint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.RobotTools;
import com.yobotics.simulationconstructionset.RobotTools.SCSRobotFromInverseDynamicsRobotModel;
import com.yobotics.simulationconstructionset.UnreasonableAccelerationException;

public class CentroidalMomentumRateADotVTermTest
{

   private static final double EPSILON = 1.0e-5;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final int NUMBER_OF_ITERATIONS = 5;

   private final Vector3d X = new Vector3d(1.0, 0.0, 0.0);
   private final Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
   private final Vector3d Z = new Vector3d(0.0, 0.0, 1.0);
   private final double controlDT = 0.00000005;

   private final DenseMatrix64F a = new DenseMatrix64F(0, 0);
   private final DenseMatrix64F aPrevVal = new DenseMatrix64F(0, 0);
   private final DenseMatrix64F aDot = new DenseMatrix64F(0, 0);

   private final DenseMatrix64F aDotVNumerical = new DenseMatrix64F(6, 1);
   private final DenseMatrix64F aDotVAnalytical = new DenseMatrix64F(6, 1);

//   @Ignore
   @Test
   public void chainTest() throws UnreasonableAccelerationException
   {
      Random random = new Random(12651L);

      ArrayList<RevoluteJoint> joints = new ArrayList<>();
      ReferenceFrame elevatorFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());
      RigidBody elevator = new RigidBody("elevator", elevatorFrame);
      int numberOfJoints = 10;
      Vector3d[] jointAxes = new Vector3d[numberOfJoints];
      for (int i = 0; i < numberOfJoints; i++)
         jointAxes[i] = RandomTools.generateRandomVector(random, 1.0);

      ScrewTestTools.createRandomChainRobot("blop", joints, elevator, jointAxes, random);
      SCSRobotFromInverseDynamicsRobotModel robot = new SCSRobotFromInverseDynamicsRobotModel("robot", elevator.getChildrenJoints().get(0));

      assertADotV(random, joints, elevator, robot);
   }

//   @Ignore
   @Test
   public void treeTest() throws UnreasonableAccelerationException
   {
      Random random = new Random(12651L);

      ArrayList<RevoluteJoint> joints = new ArrayList<>();
      ReferenceFrame elevatorFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());
      RigidBody elevator = new RigidBody("elevator", elevatorFrame);
      RevoluteJoint rootJoint = ScrewTestTools.addRandomRevoluteJoint("rootJoint", random, elevator); // Just to make sure there is only one root joint for the SCS robot
      RigidBody rootBody = ScrewTestTools.addRandomRigidBody("rootBody", random, rootJoint);

      int numberOfJoints = 10; // FIXME More than 8 joints and the test fails.... check if there is a real bug there.
      ScrewTestTools.createRandomTreeRobot(joints, rootBody, numberOfJoints - 1, random);
      joints.add(0, rootJoint);
      SCSRobotFromInverseDynamicsRobotModel robot = new SCSRobotFromInverseDynamicsRobotModel("robot", rootJoint);

      assertADotV(random, joints, elevator, robot);
   }

   @Ignore
   @Test
   public void floatingChainTest() throws UnreasonableAccelerationException
   {
      Random random = new Random(12651L);

      ArrayList<RevoluteJoint> joints = new ArrayList<>();
      int numberOfJoints = 1; // NOTE: The final error in aDotV seems to be growing as we increase the number of joints
      Vector3d[] jointAxes = new Vector3d[numberOfJoints];
      for (int i = 0; i < numberOfJoints; i++)
         jointAxes[i] = RandomTools.generateRandomVector(random, 1.0);

      ScrewTestTools.RandomFloatingChain idRobot = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      RigidBody elevator = idRobot.getElevator();
      joints.addAll(idRobot.getRevoluteJoints());

      SCSRobotFromInverseDynamicsRobotModel robot = new SCSRobotFromInverseDynamicsRobotModel("robot", idRobot.getRootJoint());

      assertADotV(random, joints, elevator, robot);
   }

   private void assertADotV(Random random, ArrayList<RevoluteJoint> joints, RigidBody elevator, SCSRobotFromInverseDynamicsRobotModel robot)
         throws UnreasonableAccelerationException
   {
      int numberOfDoFs = ScrewTools.computeDegreesOfFreedom(ScrewTools.computeSubtreeJoints(elevator));
      DenseMatrix64F v = new DenseMatrix64F(numberOfDoFs, 1);

      CenterOfMassReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", worldFrame, elevator);

      CentroidalMomentumMatrix centroidalMomentumMatrixCalculator = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);

      a.reshape(6, numberOfDoFs);
      aPrevVal.reshape(6, numberOfDoFs);
      aDot.reshape(6, numberOfDoFs);

      double totalMass = TotalMassCalculator.computeSubTreeMass(elevator);
      CentroidalMomentumRateADotVTerm aDotVAnalyticalCalculator = new CentroidalMomentumRateADotVTerm(elevator, centerOfMassFrame,
            centroidalMomentumMatrixCalculator, totalMass, v);

      for (int i = 0; i < NUMBER_OF_ITERATIONS; i++)
      {
         ScrewTestTools.setRandomVelocities(joints, random);
         ScrewTestTools.setRandomPositions(joints, random);
         ScrewTestTools.setRandomTorques(joints, random);

         robot.updateJointPositions_ID_to_SCS();
         robot.updateJointVelocities_ID_to_SCS();
         robot.updateJointTorques_ID_to_SCS();

         centerOfMassFrame.update();

         centroidalMomentumMatrixCalculator.compute();
         aPrevVal.set(centroidalMomentumMatrixCalculator.getMatrix());

         robot.doDynamicsAndIntegrate(controlDT);
         robot.updateVelocities();
         robot.updateJointPositions_SCS_to_ID();
         robot.updateJointVelocities_SCS_to_ID();
         elevator.updateFramesRecursively();
         centerOfMassFrame.update();
         ScrewTools.packJointVelocitiesMatrix(joints, v);

         // Compute aDotV analytically
         aDotVAnalyticalCalculator.compute();
         aDotVAnalytical.set(aDotVAnalyticalCalculator.getMatrix());

         // Compute aDotV numerically
         centroidalMomentumMatrixCalculator.compute();
         a.set(centroidalMomentumMatrixCalculator.getMatrix());
         MatrixTools.numericallyDifferentiate(aDot, aPrevVal, a, controlDT);
         CommonOps.mult(aDot, v, aDotVNumerical);

         smartPrintOutADotV(EPSILON);

         JUnitTools.assertMatrixEquals(aDotVNumerical, aDotVAnalytical, EPSILON);
      }
   }

   private void smartPrintOutADotV(double epsilon)
   {
      DenseMatrix64F difference = new DenseMatrix64F(aDotVNumerical.numRows, aDotVNumerical.numCols);
      CommonOps.sub(aDotVNumerical, aDotVAnalytical, difference);

      for (int i = 0; i < difference.numRows; i++)
         if (Math.abs(difference.get(i, 0)) > epsilon)
            printOutADotV();
   }

   private void printOutADotV()
   {
      int numChar = 6;
      int precision = 3;
      String format = "%" + numChar + "." + precision + "f ";

      System.out.println("----------- ADotV -----------");
      System.out.println("Numerical: ||\tAnalytical:");
      for (int i = 0; i < aDotVNumerical.numRows; i++)
         System.out.printf(format + "\t   ||\t" + format + "\n", aDotVNumerical.get(i, 0), aDotVAnalytical.get(i, 0));
      System.out.println();
   }

   @Ignore
   @Test
   public void TestTree() throws UnreasonableAccelerationException
   {

      Random random = new Random(167L);

      Robot robot = new Robot("robot");
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      LinkedHashMap<RevoluteJoint, PinJoint> jointMap = new LinkedHashMap<RevoluteJoint, PinJoint>();
      ReferenceFrame elevatorFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());
      RigidBody elevator = new RigidBody("elevator", elevatorFrame);
      double gravity = 0.0;

      //Very simple, single link robot
      int numberOfJoints = 4;
      createRandomTreeRobotAndSetJointPositionsAndVelocities(robot, jointMap, worldFrame, elevator, numberOfJoints, gravity, true, true, random);
      robot.updateVelocities();

      updateJointMap(jointMap);

      elevator.updateFramesRecursively();

      double totalMass = TotalMassCalculator.computeSubTreeMass(elevator);

      CenterOfMassReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", worldFrame, elevator);
      centerOfMassFrame.update();

      CenterOfMassCalculator comCalculator = new CenterOfMassCalculator(elevator, worldFrame);
      comCalculator.compute();

      //Create two CentroidalMomentumMatrix objects for before and after state integration
      CentroidalMomentumMatrix centroidalMomentumMatrix0 = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);
      CentroidalMomentumMatrix centroidalMomentumMatrix1 = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);

      InverseDynamicsJoint[] jointsInOrder = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      DenseMatrix64F v = new DenseMatrix64F(numberOfJoints, 1);
      ScrewTools.packJointVelocitiesMatrix(jointsInOrder, v);

      CentroidalMomentumRateADotVTerm aDotV1Analytical = new CentroidalMomentumRateADotVTerm(elevator, centerOfMassFrame, centroidalMomentumMatrix0, totalMass,
            v);

      // Compute initial A matrix at initial condition
      centroidalMomentumMatrix0.compute();

      // Integrate robot state
      robot.doDynamicsAndIntegrate(controlDT);

      robot.updateVelocities();

      updateJointMap(jointMap);

      elevator.updateFramesRecursively();
      centerOfMassFrame.update();

      //Compute A matrix after initial condition is integrated. Should be different than initial condition
      centroidalMomentumMatrix1.compute();

      // Compute \dot{A}*v analytically.
      aDotV1Analytical.compute();

      DenseMatrix64F centroidalMomentumMatrixDerivative = new DenseMatrix64F(centroidalMomentumMatrix0.getMatrix().numRows,
            centroidalMomentumMatrix0.getMatrix().numCols);
      MatrixTools.numericallyDifferentiate(centroidalMomentumMatrixDerivative, centroidalMomentumMatrix0.getMatrix(), centroidalMomentumMatrix1.getMatrix(),
            controlDT);

      DenseMatrix64F adotV = new DenseMatrix64F(6, 1);
      ScrewTools.packJointVelocitiesMatrix(jointsInOrder, v);
      CommonOps.mult(centroidalMomentumMatrixDerivative, v, adotV);

      System.out.print(adotV + "\n");
      System.out.print(aDotV1Analytical.getMatrix());

      JUnitTools.assertMatrixEquals(adotV, aDotV1Analytical.getMatrix(), 1e-2);
   }

   @Ignore
   public void TestFloatingChain() throws UnreasonableAccelerationException
   {
      Random random = new Random();

      Vector3d[] jointAxes = { Y, Y, Y, Y };
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);

      InverseDynamicsJoint[] jointsInOrder = ScrewTools.computeSupportAndSubtreeJoints(randomFloatingChain.getElevator());

      randomFloatingChain.setRandomPositionsAndVelocities(random);

      double totalMass = TotalMassCalculator.computeSubTreeMass(randomFloatingChain.getElevator());

      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", ReferenceFrame.getWorldFrame(), randomFloatingChain.getElevator());
      centerOfMassFrame.update();

      CentroidalMomentumMatrix previousAMatrix = new CentroidalMomentumMatrix(randomFloatingChain.getElevator(), centerOfMassFrame);

      int nDoFs = ScrewTools.computeDegreesOfFreedom(jointsInOrder);
      DenseMatrix64F v = new DenseMatrix64F(nDoFs, 1);
      ScrewTools.packJointVelocitiesMatrix(jointsInOrder, v);

      previousAMatrix.compute(); // Compute A matrix at initial condition

      RobotTools.SCSRobotFromInverseDynamicsRobotModel scsRobot = new RobotTools.SCSRobotFromInverseDynamicsRobotModel("RobbyRobot",
            randomFloatingChain.getRootJoint());

      ArrayList<OneDegreeOfFreedomJoint> scsRobotJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      scsRobot.getAllOneDegreeOfFreedomJoints(scsRobotJoints);

      //    Set joint position values for scsRobot joints equal to that of randomFloatingChain
      for (int i = 0; i < randomFloatingChain.getRevoluteJoints().size(); i++)
      {
         scsRobotJoints.get(i).setQ(randomFloatingChain.getRevoluteJoints().get(i).getQ());
         scsRobotJoints.get(i).setQd(randomFloatingChain.getRevoluteJoints().get(i).getQd());
         scsRobotJoints.get(i).setQdd(randomFloatingChain.getRevoluteJoints().get(i).getQdd());

         //      scsRobotJoints.get(i).setTau(0.0);
      }
      scsRobot.updateVelocities();

      scsRobot.doDynamicsAndIntegrate(controlDT);
      scsRobot.updateVelocities();

      CentroidalMomentumMatrix aMatrix = new CentroidalMomentumMatrix(randomFloatingChain.getElevator(), centerOfMassFrame);
      CentroidalMomentumRateADotVTerm aDotVAnalytical = new CentroidalMomentumRateADotVTerm(randomFloatingChain.getElevator(), centerOfMassFrame, aMatrix,
            totalMass, v);

      for (int i = 0; i < randomFloatingChain.getRevoluteJoints().size(); i++)
      {
         // After integrating the scsRobot, set the random floating chain positions and velocities to integrated values
         randomFloatingChain.getRevoluteJoints().get(i).setQ(scsRobotJoints.get(i).getQ().getDoubleValue());
         randomFloatingChain.getRevoluteJoints().get(i).setQd(scsRobotJoints.get(i).getQD().getDoubleValue());
         randomFloatingChain.getRevoluteJoints().get(i).setQdd(scsRobotJoints.get(i).getQDD().getDoubleValue());
      }
      randomFloatingChain.getElevator().updateFramesRecursively();
      centerOfMassFrame.update();

      aMatrix.compute();
      aDotVAnalytical.compute(); // compute \dot{A}*v term

      DenseMatrix64F centroidalMomentumMatrixDerivative = new DenseMatrix64F(previousAMatrix.getMatrix().numRows, previousAMatrix.getMatrix().numCols);

      DenseMatrix64F previousMatrix = new DenseMatrix64F(previousAMatrix.getMatrix().numRows, previousAMatrix.getMatrix().numCols);
      DenseMatrix64F newMatrix = new DenseMatrix64F(previousAMatrix.getMatrix().numRows, previousAMatrix.getMatrix().numCols);

      previousMatrix.set(previousAMatrix.getMatrix());
      newMatrix.set(aMatrix.getMatrix());
      MatrixTools.numericallyDifferentiate(centroidalMomentumMatrixDerivative, previousMatrix, newMatrix, controlDT);

      DenseMatrix64F adotVNumerical = new DenseMatrix64F(6, 1);
      CommonOps.mult(centroidalMomentumMatrixDerivative, v, adotVNumerical);

      DenseMatrix64F tmpMatrix = new DenseMatrix64F(previousAMatrix.getMatrix().numRows, previousAMatrix.getMatrix().numCols);
      CommonOps.sub(previousAMatrix.getMatrix(), aMatrix.getMatrix(), tmpMatrix);

      System.out.print(adotVNumerical + "\n");

      System.out.print(aDotVAnalytical.getMatrix() + "\n");

      JUnitTools.assertMatrixEquals(adotVNumerical, aDotVAnalytical.getMatrix(), 1e-0);

   }

   public static void createRandomTreeRobotAndSetJointPositionsAndVelocities(Robot robot, HashMap<RevoluteJoint, PinJoint> jointMap, ReferenceFrame worldFrame,
         RigidBody elevator, int numberOfJoints, double gravity, boolean useRandomVelocity, boolean useRandomAcceleration, Random random)
   {
      robot.setGravity(gravity);

      ArrayList<PinJoint> potentialParentJoints = new ArrayList<PinJoint>();
      ArrayList<RevoluteJoint> potentialInverseDynamicsParentJoints = new ArrayList<RevoluteJoint>(); // synchronized with potentialParentJoints

      for (int i = 0; i < numberOfJoints; i++)
      {
         Vector3d jointOffset = RandomTools.generateRandomVector(random);
         Vector3d jointAxis = new Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
         jointAxis.normalize();
         Matrix3d momentOfInertia = RandomTools.generateRandomDiagonalMatrix3d(random);
         double mass = random.nextDouble();
         Vector3d comOffset = RandomTools.generateRandomVector(random);
         double jointPosition = random.nextDouble();
         double jointVelocity = useRandomVelocity ? random.nextDouble() : 0.0;
         double jointAcceleration = useRandomAcceleration ? random.nextDouble() : 0.0;

         PinJoint currentJoint = new PinJoint("joint" + i, jointOffset, robot, jointAxis);
         currentJoint.setInitialState(jointPosition, jointVelocity);
         RigidBody inverseDynamicsParentBody;
         if (potentialParentJoints.isEmpty())
         {
            robot.addRootJoint(currentJoint);
            inverseDynamicsParentBody = elevator;
         }
         else
         {
            int parentIndex = random.nextInt(potentialParentJoints.size());
            potentialParentJoints.get(parentIndex).addJoint(currentJoint);
            RevoluteJoint inverseDynamicsParentJoint = potentialInverseDynamicsParentJoints.get(parentIndex);
            inverseDynamicsParentBody = inverseDynamicsParentJoint.getSuccessor();
         }

         RevoluteJoint currentIDJoint = ScrewTools.addRevoluteJoint("jointID" + i, inverseDynamicsParentBody, jointOffset, jointAxis);
         currentIDJoint.setQ(jointPosition);
         currentIDJoint.setQd(jointVelocity);
         currentIDJoint.setQddDesired(jointAcceleration);
         ScrewTools.addRigidBody("bodyID" + i, currentIDJoint, momentOfInertia, mass, comOffset);

         Link currentBody = new Link("body" + i);
         currentBody.setComOffset(comOffset);
         currentBody.setMass(mass);
         currentBody.setMomentOfInertia(momentOfInertia);
         currentJoint.setLink(currentBody);

         jointMap.put(currentIDJoint, currentJoint);

         potentialParentJoints.add(currentJoint);
         potentialInverseDynamicsParentJoints.add(currentIDJoint);
      }
   }

   private void updateJointMap(LinkedHashMap<RevoluteJoint, PinJoint> jointMap)
   {
      for (OneDoFJoint screwJoint : jointMap.keySet())
      {
         OneDegreeOfFreedomJoint robotJoint = jointMap.get(screwJoint);
         screwJoint.setQ(robotJoint.getQ().getDoubleValue());
         screwJoint.setQd(robotJoint.getQD().getDoubleValue());
         screwJoint.setQdd(robotJoint.getQDD().getDoubleValue());
      }
   }

}
