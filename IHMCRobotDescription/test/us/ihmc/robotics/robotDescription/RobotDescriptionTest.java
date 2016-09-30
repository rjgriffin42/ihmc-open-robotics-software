package us.ihmc.robotics.robotDescription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.robotics.Axis;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.tools.testing.JUnitTools;
import us.ihmc.tools.testing.MutationTestingTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class RobotDescriptionTest
{

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testRobotDescriptionOne()
   {
      RobotDescription robotDescription = new RobotDescription("Test");
      assertEquals("Test", robotDescription.getName());

      robotDescription.setName("TestTwo");
      assertEquals("TestTwo", robotDescription.getName());

      FloatingJointDescription rootJointOne = new FloatingJointDescription("rootJointOne");
      assertEquals("rootJointOne", rootJointOne.getName());
      LinkDescription rootLinkOne = new LinkDescription("rootLinkOne");
      assertEquals("rootLinkOne", rootLinkOne.getName());

      rootLinkOne.setMass(1.2);
      rootLinkOne.setCenterOfMassOffset(new Vector3d(1.0, 2.0, 3.0));
      rootLinkOne.setMomentOfInertia(0.1, 0.2, 0.3);

      assertEquals(1.2, rootLinkOne.getMass(), 1e-7);

      Vector3d comOffsetCheck = new Vector3d();
      rootLinkOne.getCenterOfMassOffset(comOffsetCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(1.0, 2.0, 3.0), comOffsetCheck, 1e-7);
      JUnitTools.assertVector3dEquals("", new Vector3d(1.0, 2.0, 3.0), rootLinkOne.getCenterOfMassOffset(), 1e-7);

      Matrix3d momentOfInertiaCopy = rootLinkOne.getMomentOfInertiaCopy();
      assertEquals(0.1, momentOfInertiaCopy.getM00(), 1e-7);
      assertEquals(0.2, momentOfInertiaCopy.getM11(), 1e-7);
      assertEquals(0.3, momentOfInertiaCopy.getM22(), 1e-7);

      DenseMatrix64F momentOfInertiaCheck = rootLinkOne.getMomentOfInertia();
      assertEquals(0.1, momentOfInertiaCheck.get(0, 0), 1e-7);
      assertEquals(0.2, momentOfInertiaCheck.get(1, 1), 1e-7);
      assertEquals(0.3, momentOfInertiaCheck.get(2, 2), 1e-7);

      rootJointOne.setLink(rootLinkOne);

      assertTrue(rootJointOne.getLink() == rootLinkOne);

      robotDescription.addRootJoint(rootJointOne);
      ArrayList<JointDescription> rootJoints = robotDescription.getRootJoints();

      assertEquals(1, rootJoints.size());
      assertTrue(rootJointOne == rootJoints.get(0));

      PinJointDescription rootJointTwo = new PinJointDescription("rootJointTwo", new Vector3d(-0.1, -0.2, -0.3), Axis.Y);
      Vector3d jointAxisCheck = new Vector3d();
      rootJointTwo.getJointAxis(jointAxisCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(0.0, 1.0, 0.0), jointAxisCheck, 1e-7);

      LinkDescription rootLinkTwo = new LinkDescription("rootLinkTwo");
      assertEquals("rootLinkTwo", rootLinkTwo.getName());

      rootLinkTwo.setMass(1.2);
      rootLinkTwo.setCenterOfMassOffset(new Vector3d(1.0, 2.0, 3.0));
      rootLinkTwo.setMomentOfInertia(0.1, 0.2, 0.3);

      rootJointTwo.setLink(rootLinkTwo);

      robotDescription.addRootJoint(rootJointTwo);

      assertEquals(2, robotDescription.getChildrenJoints().size());
      assertTrue(rootJointOne == robotDescription.getChildrenJoints().get(0));
      assertTrue(rootJointTwo == robotDescription.getChildrenJoints().get(1));

      PinJointDescription childJointOne = new PinJointDescription("childJointOne", new Vector3d(1.2, 1.3, 7.7), Axis.Z);

      Vector3d jointOffsetCheck = new Vector3d();
      childJointOne.getOffsetFromParentJoint(jointOffsetCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(1.2, 1.3, 7.7), jointOffsetCheck, 1e-7);

      LinkDescription childLinkOne = new LinkDescription("childLinkOne");
      childLinkOne.setMass(3.3);
      DenseMatrix64F childMomentOfInertiaOne = new DenseMatrix64F(new double[][] { { 1.0, 0.012, 0.013 }, { 0.021, 2.0, 0.023 }, { 0.031, 0.032, 3.0 } });
      childLinkOne.setMomentOfInertia(childMomentOfInertiaOne);
      DenseMatrix64F childMomentOfInertiaOneCheck = new DenseMatrix64F(3, 3);
      childLinkOne.getMomentOfInertia(childMomentOfInertiaOneCheck);

      JUnitTools.assertMatrixEquals("", new DenseMatrix64F(new double[][] { { 1.0, 0.012, 0.013 }, { 0.021, 2.0, 0.023 }, { 0.031, 0.032, 3.0 } }), childMomentOfInertiaOneCheck, 1e-7);

      rootJointOne.addJoint(childJointOne);

      RigidBodyTransform cameraOneTransformToJoint = new RigidBodyTransform();
      CameraSensorDescription cameraOneDescription = new CameraSensorDescription("cameraOne", cameraOneTransformToJoint);
      childJointOne.addCameraSensor(cameraOneDescription);

      ArrayList<CameraSensorDescription> cameraSensors = childJointOne.getCameraSensors();
      assertEquals(1, cameraSensors.size());
      assertTrue(cameraOneDescription == cameraSensors.get(0));

      assertEquals(rootJointOne, childJointOne.getParentJoint());
      assertNull(rootJointOne.getParentJoint());

      childJointOne.setOffsetFromParentJoint(new Vector3d(-0.4, -0.5, -0.6));
      childJointOne.getOffsetFromParentJoint(jointOffsetCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(-0.4, -0.5, -0.6), jointOffsetCheck, 1e-7);

      assertFalse(childJointOne.containsLimitStops());

      double qMin = -0.2;
      double qMax = 0.4;
      double kLimit = 1000.0;
      double bLimit = 100.0;
      childJointOne.setLimitStops(qMin, qMax, kLimit, bLimit);
      double[] limitStopParameters = childJointOne.getLimitStopParameters();
      assertEquals(4, limitStopParameters.length);

      assertEquals(qMin, limitStopParameters[0], 1e-7);
      assertEquals(qMax, limitStopParameters[1], 1e-7);
      assertEquals(kLimit, limitStopParameters[2], 1e-7);
      assertEquals(bLimit, limitStopParameters[3], 1e-7);

      assertEquals(qMin, childJointOne.getLowerLimit(), 1e-7);
      assertEquals(qMax, childJointOne.getUpperLimit(), 1e-7);

      childJointOne.setDamping(4.4);
      assertEquals(4.4, childJointOne.getDamping(), 1e-7);

      childJointOne.setStiction(7.7);
      assertEquals(7.7, childJointOne.getStiction(), 1e-7);

      childJointOne.setEffortLimit(400.3);
      assertEquals(400.3, childJointOne.getEffortLimit(), 1e-7);

      childJointOne.setVelocityLimits(10.0, 30.0);
      assertEquals(10.0, childJointOne.getVelocityLimit(), 1e-7);
      assertEquals(30.0, childJointOne.getVelocityDamping(), 1e-7);

      assertTrue(childJointOne.containsLimitStops());

      childJointOne.getJointAxis(jointAxisCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(0.0, 0.0, 1.0), jointAxisCheck, 1e-7);

      //TODO: Do Axis vectors need to be normalized???
      SliderJointDescription childJointTwo = new SliderJointDescription("childJointTwo", new Vector3d(0.5, 0.7, 0.9), new Vector3d(1.1, 2.2, 3.3));
      assertTrue(Double.POSITIVE_INFINITY == childJointTwo.getEffortLimit());
      assertTrue(Double.POSITIVE_INFINITY == childJointTwo.getVelocityLimit());
      assertFalse(childJointTwo.containsLimitStops());
      assertTrue(Double.NEGATIVE_INFINITY == childJointTwo.getLowerLimit());
      assertTrue(Double.POSITIVE_INFINITY == childJointTwo.getUpperLimit());

      limitStopParameters = childJointTwo.getLimitStopParameters();
      assertTrue(Double.NEGATIVE_INFINITY == limitStopParameters[0]);
      assertTrue(Double.POSITIVE_INFINITY == limitStopParameters[1]);
      assertEquals(0.0, limitStopParameters[2], 1e-7);
      assertEquals(0.0, limitStopParameters[3], 1e-7);

      assertEquals(0.0, childJointTwo.getVelocityDamping(), 1e-7);
      assertEquals(0.0, childJointTwo.getDamping(), 1e-7);
      assertEquals(0.0, childJointTwo.getStiction(), 1e-7);

      childJointTwo.getJointAxis(jointAxisCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(1.1, 2.2, 3.3), jointAxisCheck, 1e-7);

      LinkDescription childLinkTwo = new LinkDescription("childLinkTwo");
      childLinkTwo.setMass(9.9);
      childLinkTwo.setMomentOfInertia(1.9, 2.2, 0.4);

      JUnitTools.assertVector3dEquals("", new Vector3d(), childLinkTwo.getCenterOfMassOffset(), 1e-7);
      childJointTwo.setLink(childLinkTwo);

      rootJointOne.addJoint(childJointTwo);
      ArrayList<JointDescription> childrenJoints = rootJointOne.getChildrenJoints();
      assertEquals(2, childrenJoints.size());

      assertTrue(childJointOne == childrenJoints.get(0));
      assertTrue(childJointTwo == childrenJoints.get(1));

      PinJointDescription childJointThree = new PinJointDescription("childJointThree", new Vector3d(9.9, 0.0, -0.5), Axis.X);
      childJointThree.getOffsetFromParentJoint(jointOffsetCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(9.9, 0.0, -0.5), jointOffsetCheck, 1e-7);
      childJointThree.getJointAxis(jointAxisCheck);
      JUnitTools.assertVector3dEquals("", new Vector3d(1.0, 0.0, 0.0), jointAxisCheck, 1e-7);

      LinkDescription childLinkThree = new LinkDescription("childLinkThree");
      childLinkThree.setMass(1.9);
      childLinkThree.setMomentOfInertia(0.2, 0.3, 0.4);

      LinkGraphicsDescription childGraphicsThree = new LinkGraphicsDescription();
      childLinkThree.setLinkGraphics(childGraphicsThree);

      CollisionMeshDescription childMeshThree = new CollisionMeshDescription();
      childLinkThree.setCollisionMesh(childMeshThree);

      childJointThree.setLink(childLinkThree);

      childJointTwo.addJoint(childJointThree);
      childrenJoints = childJointTwo.getChildrenJoints();
      assertEquals(1, childrenJoints.size());
      assertTrue(childJointThree == childrenJoints.get(0));
      assertTrue(childJointTwo == childJointThree.getParentJoint());

      JointDescription jointDescriptionCheck = robotDescription.getJointDescription("rootJointOne");
      assertTrue(rootJointOne == jointDescriptionCheck);

      jointDescriptionCheck = robotDescription.getJointDescription("rootJointTwo");
      assertTrue(rootJointTwo == jointDescriptionCheck);

      jointDescriptionCheck = robotDescription.getJointDescription("childJointOne");
      assertTrue(childJointOne == jointDescriptionCheck);

      jointDescriptionCheck = robotDescription.getJointDescription("childJointTwo");
      assertTrue(childJointTwo == jointDescriptionCheck);

      jointDescriptionCheck = robotDescription.getJointDescription("childJointThree");
      assertTrue(childJointThree == jointDescriptionCheck);

      assertNull(robotDescription.getJointDescription("noSuchJoint"));
      assertNull(robotDescription.getGraphicsObject("noSuchJoint"));
      assertNull(robotDescription.getCollisionObject("noSuchJoint"));

      Graphics3DObject linkGraphicsCheck = robotDescription.getGraphicsObject("childJointThree");
      assertTrue(linkGraphicsCheck == childGraphicsThree);

      Graphics3DObject collisionMeshCheck = robotDescription.getCollisionObject("childJointThree");
      assertTrue(collisionMeshCheck == childMeshThree);

   }

   public static void main(String[] args)
   {
      String targetTests = RobotDescriptionTest.class.getName();
      String targetClassesInSamePackage = MutationTestingTools.createClassSelectorStringFromTargetString(targetTests);
      MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClassesInSamePackage);
   }

}
