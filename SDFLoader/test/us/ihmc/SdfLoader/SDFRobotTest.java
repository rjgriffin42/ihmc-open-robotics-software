package us.ihmc.SdfLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import javax.xml.bind.JAXBException;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DPrimitiveInstruction;
import us.ihmc.robotics.robotDescription.RobotDescription;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.GroundContactPointGroup;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.RobotConstructorFromRobotDescription;
import us.ihmc.tools.testing.JUnitTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class SDFRobotTest
{
	@DeployableTestMethod(estimatedDuration = 0.6)
   @Test(timeout = 30000)
   public void testSDFRobotVersusRobotDescription() throws FileNotFoundException, JAXBException
   {
	   List<String> resourceDirectories = (List<String>) null;
	   SDFDescriptionMutator mutator = null;
	   
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sdfRobotTest.sdf");
      JaxbSDFLoader loader = new JaxbSDFLoader(inputStream, resourceDirectories, mutator);
      
      String modelName = "atlas";
      GeneralizedSDFRobotModel generalizedSDFRobotModel = loader.getGeneralizedSDFRobotModel(modelName);
      
      SDFHumanoidJointNameMap sdfJointNameMap = null;
      boolean useCollisionMeshes = true;
      SDFHumanoidRobot sdfHumanoidRobot = new SDFHumanoidRobot(generalizedSDFRobotModel, mutator, sdfJointNameMap, useCollisionMeshes);
      

      inputStream = getClass().getClassLoader().getResourceAsStream("sdfRobotTest.sdf");
      RobotDescriptionFromSDFLoader robotDescriptionFromSDFLoader = new RobotDescriptionFromSDFLoader();
      boolean enableTorqueVelocityLimits = true;
      boolean enableDamping = true;
      robotDescriptionFromSDFLoader.loadRobotDescriptionFromSDF(modelName, inputStream, resourceDirectories, mutator, sdfJointNameMap, useCollisionMeshes, enableTorqueVelocityLimits, enableDamping);
   
      RobotDescription robotDescription = robotDescriptionFromSDFLoader.getRobotDescription();
      
      RobotConstructorFromRobotDescription robotConstructor = new RobotConstructorFromRobotDescription(robotDescription);
      Robot robot = robotConstructor.getRobot();
      
      checkRobotsMatch(sdfHumanoidRobot, robot);
   }

   private void checkRobotsMatch(Robot robotOne, Robot robotTwo)
   {
      assertEquals(robotOne.getName(), robotTwo.getName());
      
      ArrayList<Joint> rootJointsOne = robotOne.getRootJoints();
      ArrayList<Joint> rootJointsTwo = robotTwo.getRootJoints();
      
      assertEquals(rootJointsOne.size(), rootJointsTwo.size());
      
      for (int i = 0; i<rootJointsOne.size(); i++)
      {
         Joint rootJointOne = rootJointsOne.get(i);
         Joint rootJointTwo = rootJointsTwo.get(i);
         
         checkJointsMatchRecursively(rootJointOne, rootJointTwo);
      }
   }

   private void checkJointsMatchRecursively(Joint jointOne, Joint jointTwo)
   {
      assertEquals(jointOne.getName(), jointTwo.getName());
      
      Vector3d offsetOne = new Vector3d();
      jointOne.getOffset(offsetOne);
      
      Vector3d offsetTwo = new Vector3d();
      jointTwo.getOffset(offsetTwo);
      
      JUnitTools.assertTuple3dEquals(offsetOne, offsetTwo, 1e-7);
      
      checkGroundContactPointsMatch(jointOne, jointTwo);
      
      Link linkOne = jointOne.getLink();
      Link linkTwo = jointTwo.getLink();
      
      checkLinksMatch(linkOne, linkTwo);
      
      // Check the children

      ArrayList<Joint> childrenJointsOne = jointOne.getChildrenJoints();
      ArrayList<Joint> childrenJointsTwo = jointTwo.getChildrenJoints();
      
      int numberOfChildren = childrenJointsOne.size();
      assertEquals(numberOfChildren, childrenJointsTwo.size());
      
      for (int i=0; i<numberOfChildren; i++)
      {
         Joint childJointOne = childrenJointsOne.get(i);
         Joint childJointTwo = childrenJointsTwo.get(i);
         
         checkJointsMatchRecursively(childJointOne, childJointTwo);
      }
   }

   private void checkGroundContactPointsMatch(Joint jointOne, Joint jointTwo)
   {
      GroundContactPointGroup groundContactPointGroupOne = jointOne.getGroundContactPointGroup();
      GroundContactPointGroup groundContactPointGroupTwo = jointTwo.getGroundContactPointGroup();
      
      if (groundContactPointGroupOne == null)
      {
         assertNull(groundContactPointGroupTwo);
         return;
      }
      ArrayList<GroundContactPoint> groundContactPointsOne = groundContactPointGroupOne.getGroundContactPoints();
      ArrayList<GroundContactPoint> groundContactPointsTwo = groundContactPointGroupTwo.getGroundContactPoints();
      
      assertEquals(groundContactPointsOne.size(), groundContactPointsTwo.size());
      
      for (int i=0; i<groundContactPointsOne.size(); i++)
      {
         GroundContactPoint pointOne = groundContactPointsOne.get(i);
         GroundContactPoint pointTwo = groundContactPointsTwo.get(i);
         
         JUnitTools.assertTuple3dEquals(pointOne.getOffsetCopy(), pointTwo.getOffsetCopy(), 1e-7);
      }
   }

   private void checkLinksMatch(Link linkOne, Link linkTwo)
   {
      assertEquals(linkOne.getName(), linkTwo.getName());

      assertEquals(linkOne.getMass(), linkTwo.getMass(), 1e-7);
      JUnitTools.assertTuple3dEquals(linkOne.getComOffset(), linkTwo.getComOffset(), 1e-7);
      
      Matrix3d momentOfInertiaOne = new Matrix3d();
      linkOne.getMomentOfInertia(momentOfInertiaOne);
      
      Matrix3d momentOfInertiaTwo = new Matrix3d();
      linkTwo.getMomentOfInertia(momentOfInertiaTwo);
      JUnitTools.assertMatrix3dEquals("momentOfInertiaOne = " + momentOfInertiaOne + ", momentOfInertiaTwo = " + momentOfInertiaTwo, momentOfInertiaOne, momentOfInertiaTwo, 1e-7);

      Graphics3DObject linkGraphicsOne = linkOne.getLinkGraphics();
      Graphics3DObject linkGraphicsTwo = linkTwo.getLinkGraphics();
      
      ArrayList<Graphics3DPrimitiveInstruction> graphics3dInstructionsOne = linkGraphicsOne.getGraphics3DInstructions();
      ArrayList<Graphics3DPrimitiveInstruction> graphics3dInstructionsTwo = linkGraphicsTwo.getGraphics3DInstructions();
      
      assertEquals(graphics3dInstructionsOne.size(), graphics3dInstructionsTwo.size());
      
      for (int i=0; i<graphics3dInstructionsOne.size(); i++)
      {
         Graphics3DPrimitiveInstruction instructionOne = graphics3dInstructionsOne.get(i);
         Graphics3DPrimitiveInstruction instructionTwo = graphics3dInstructionsTwo.get(i);
         
         //TODO: Deep check of the instructions...
         assertTrue(instructionOne.getClass() == instructionTwo.getClass());
      }
      
   }

}
