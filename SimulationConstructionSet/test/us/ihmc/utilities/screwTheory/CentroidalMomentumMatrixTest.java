package us.ihmc.utilities.screwTheory;

import java.util.LinkedHashMap;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.CenterOfMassReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.utilities.test.JUnitTools;

import com.yobotics.simulationconstructionset.FloatingJoint;
import com.yobotics.simulationconstructionset.Link;
import com.yobotics.simulationconstructionset.PinJoint;
import com.yobotics.simulationconstructionset.Robot;

public class CentroidalMomentumMatrixTest
{
   @Test
   public void testTree()
   {
      Random random = new Random(167L);

      Robot robot = new Robot("robot");
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      LinkedHashMap<RevoluteJoint, PinJoint> jointMap = new LinkedHashMap<RevoluteJoint, PinJoint>();
      ReferenceFrame elevatorFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());
      RigidBody elevator = new RigidBody("elevator", elevatorFrame);
      double gravity = 0.0;

      int numberOfJoints = 3;
      InverseDynamicsCalculatorTest.createRandomTreeRobotAndSetJointPositionsAndVelocities(robot, jointMap, worldFrame, elevator, numberOfJoints, gravity,
            true, true, random);
      robot.updateVelocities();

      CenterOfMassReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", worldFrame, elevator);
      centerOfMassFrame.update();
      CentroidalMomentumMatrix centroidalMomentumMatrix = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);
      centroidalMomentumMatrix.compute();

      Momentum comMomentum = computeCoMMomentum(elevator, centerOfMassFrame, centroidalMomentumMatrix);

      Point3d comPoint = new Point3d();
      Vector3d linearMomentum = new Vector3d();
      Vector3d angularMomentum = new Vector3d();
      robot.computeCOMMomentum(comPoint, linearMomentum, angularMomentum);

      JUnitTools.assertTuple3dEquals(linearMomentum, comMomentum.getLinearPart(), 1e-12);
      JUnitTools.assertTuple3dEquals(angularMomentum, comMomentum.getAngularPart(), 1e-12);
   }

   @Test
   public void testFloatingBody()
   {
      Random random = new Random(167L);

      double mass = random.nextDouble();
      Matrix3d momentOfInertia = RandomTools.generateRandomDiagonalMatrix3d(random);
      Vector3d comOffset = RandomTools.generateRandomVector(random);

      Robot robot = new Robot("robot");
      FloatingJoint rootJoint = new FloatingJoint("rootJoint", new Vector3d(), robot);
      Link link = new Link("link");
      link.setMass(mass);
      link.setMomentOfInertia(momentOfInertia);
      link.setComOffset(comOffset);
      rootJoint.setLink(link);
      robot.addRootJoint(rootJoint);

      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      ReferenceFrame elevatorFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());
      RigidBody elevator = new RigidBody("elevator", elevatorFrame);
      SixDoFJoint sixDoFJoint = new SixDoFJoint("sixDoFJoint", elevator, elevatorFrame);
      ScrewTools.addRigidBody("rigidBody", sixDoFJoint, momentOfInertia, mass, comOffset);

      CenterOfMassReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", worldFrame, elevator);
      CentroidalMomentumMatrix centroidalMomentumMatrix = new CentroidalMomentumMatrix(elevator, centerOfMassFrame);

      int nTests = 10;
      for (int i = 0; i < nTests; i++)
      {
         Vector3d position = RandomTools.generateRandomVector(random);
         Matrix3d rotation = new Matrix3d();
         RotationFunctions.setYawPitchRoll(rotation, random.nextDouble(), random.nextDouble(), random.nextDouble());
         Vector3d linearVelocityInBody = RandomTools.generateRandomVector(random);
         Vector3d linearVelocityInWorld = new Vector3d(linearVelocityInBody);
         rotation.transform(linearVelocityInWorld);
         Vector3d angularVelocity = RandomTools.generateRandomVector(random);

         rootJoint.setPosition(position);
         rootJoint.setRotation(rotation);
         rootJoint.setVelocity(linearVelocityInWorld);
         rootJoint.setAngularVelocityInBody(angularVelocity);
         robot.updateVelocities();
         Point3d comPoint = new Point3d();
         Vector3d linearMomentum = new Vector3d();
         Vector3d angularMomentum = new Vector3d();
         robot.computeCOMMomentum(comPoint, linearMomentum, angularMomentum);

         sixDoFJoint.setPosition(position);
         sixDoFJoint.setRotation(rotation);
         Twist jointTwist = new Twist();
         sixDoFJoint.packJointTwist(jointTwist);
         jointTwist.setAngularPart(angularVelocity);
         jointTwist.setLinearPart(linearVelocityInBody);
         sixDoFJoint.setJointTwist(jointTwist);
         elevator.updateFramesRecursively();

         centerOfMassFrame.update();

         centroidalMomentumMatrix.compute();
         Momentum comMomentum = computeCoMMomentum(elevator, centerOfMassFrame, centroidalMomentumMatrix);

         JUnitTools.assertTuple3dEquals(linearMomentum, comMomentum.getLinearPart(), 1e-12);
         JUnitTools.assertTuple3dEquals(angularMomentum, comMomentum.getAngularPart(), 1e-12);
      }
   }

   public static Momentum computeCoMMomentum(RigidBody elevator, ReferenceFrame centerOfMassFrame, CentroidalMomentumMatrix centroidalMomentumMatrix)
   {
      DenseMatrix64F mat = centroidalMomentumMatrix.getMatrix();
      InverseDynamicsJoint[] jointList = ScrewTools.computeJointsInOrder(elevator);
      DenseMatrix64F jointVelocities = new DenseMatrix64F(ScrewTools.computeDegreesOfFreedom(jointList), 1);
      ScrewTools.packJointVelocitiesMatrix(jointList, jointVelocities);

      DenseMatrix64F comMomentumMatrix = MatrixTools.mult(mat, jointVelocities);

      Momentum comMomentum = new Momentum(centerOfMassFrame, comMomentumMatrix);

      return comMomentum;
   }
}
