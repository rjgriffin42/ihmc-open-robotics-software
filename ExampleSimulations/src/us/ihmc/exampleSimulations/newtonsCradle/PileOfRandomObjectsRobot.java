package us.ihmc.exampleSimulations.newtonsCradle;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Vector3d;

import us.ihmc.graphics3DDescription.Graphics3DObject;
import us.ihmc.graphics3DDescription.appearance.AppearanceDefinition;
import us.ihmc.graphics3DDescription.appearance.YoAppearance;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.robotDescription.CollisionMeshDescription;
import us.ihmc.robotics.robotDescription.FloatingJointDescription;
import us.ihmc.robotics.robotDescription.LinkDescription;
import us.ihmc.robotics.robotDescription.LinkGraphicsDescription;
import us.ihmc.robotics.robotDescription.RobotDescription;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.RobotFromDescription;

public class PileOfRandomObjectsRobot
{
   private final ArrayList<Robot> robots = new ArrayList<Robot>();

   public PileOfRandomObjectsRobot()
   {
      int numberOfObjects = 200;
      Random random = new Random(1886L);

      createFallingObjects(numberOfObjects, random);
      //      createBoardFrame(collisionShapeFactory, random);
   }

   private void createFallingObjects(int numberOfObjects, Random random)
   {
      for (int i = 0; i < numberOfObjects; i++)
      {
         RobotDescription robotDescription = new RobotDescription("RandomRobot" + i);
//         Robot robot = new Robot("RandomRobot" + i);

         Vector3d offset = new Vector3d(0.0, 0.0, 0.0);
         FloatingJointDescription floatingJointDescription = new FloatingJointDescription("object" + i);
         floatingJointDescription.setOffsetFromParentJoint(offset);

         LinkDescription link;

         int shape = random.nextInt(4);
         if (shape == 0)
         {
            link = createRandomBox(random, i);
         }
         else if (shape == 1)
         {
            link = createRandomSphere(random, i);
         }
         else if (shape == 2)
         {
            link = createRandomCapsule(random, i);
         }
         else
         {
            link = createRandomCylinder(random, i);
         }

         floatingJointDescription.setLink(link);
         robotDescription.addRootJoint(floatingJointDescription);

         double xyExtents = 1.5; //0.25;
         double x = RandomTools.generateRandomDouble(random, -xyExtents, xyExtents);
         double y = RandomTools.generateRandomDouble(random, -xyExtents, xyExtents);
         double z = RandomTools.generateRandomDouble(random, 0.2, 6.0);

         double angleExtents = Math.PI / 2.0;
         double yaw = RandomTools.generateRandomDouble(random, -angleExtents, angleExtents);
         double pitch = RandomTools.generateRandomDouble(random, -angleExtents, angleExtents);
         double roll = RandomTools.generateRandomDouble(random, -angleExtents, angleExtents);

         Robot robot = new RobotFromDescription(robotDescription);

         FloatingJoint floatingJoint = (FloatingJoint) robot.getRootJoints().get(0);

         floatingJoint.setPosition(x, y, z);
         floatingJoint.setYawPitchRoll(yaw, pitch, roll);

         this.robots.add(robot);
      }
   }

   private void createBoardFrame(Random random)
   {
      double boardZ = 0.14;

      FloatingJoint board0 = createContainerBoard("board0", random);
      board0.setPosition(0.51, 0.0, boardZ);
      board0.setYawPitchRoll(Math.PI / 2.0, 0.0, 0.0);

      FloatingJoint board1 = createContainerBoard("board1", random);
      board1.setPosition(0.0, 0.35, boardZ);
      board1.setYawPitchRoll(0.0, 0.0, 0.0);

      FloatingJoint board2 = createContainerBoard("board2", random);
      board2.setPosition(0.0, -0.35, boardZ);
      board2.setYawPitchRoll(0.0, 0.0, 0.0);

      FloatingJoint board3 = createContainerBoard("board3", random);
      board3.setPosition(-0.51, 0.0, boardZ);
      board3.setYawPitchRoll(Math.PI / 2.0, 0.0, 0.0);
   }

   private FloatingJoint createContainerBoard(String name, Random random)
   {
      Robot robot = new Robot(name);

      Vector3d offset = new Vector3d(0.0, 0.0, 0.0);
      FloatingJoint floatingJoint = new FloatingJoint(name, offset, robot);

      Link link = createContainerBoardLink(name, random, robot);
      floatingJoint.setLink(link);
      robot.addRootJoint(floatingJoint);
      this.robots.add(robot);

      return floatingJoint;
   }

   private Link createContainerBoardLink(String name, Random random, Robot robot)
   {
      double objectWidth = 0.2;
      double objectLength = 0.8;
      double objectHeight = 0.2;
      double objectMass = 10.0;

      Link link = new Link(name);
      link.setMassAndRadiiOfGyration(objectMass, objectLength / 2.0, objectWidth / 2.0, objectHeight / 2.0);
      link.setComOffset(0.0, 0.0, 0.0);

      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.translate(0.0, 0.0, -objectHeight / 2.0);
      AppearanceDefinition randomColor = YoAppearance.Aquamarine();

      linkGraphics.addCube(objectLength, objectWidth, objectHeight, randomColor);
      link.setLinkGraphics(linkGraphics);

      CollisionMeshDescription collisionMeshDescription = new CollisionMeshDescription();
      collisionMeshDescription.addCubeReferencedAtCenter(objectLength, objectWidth, objectHeight);
      link.setCollisionMesh(collisionMeshDescription);

      return link;
   }

   private LinkDescription createRandomBox(Random random, int i)
   {
      double objectLength = RandomTools.generateRandomDouble(random, 0.04, 0.1);
      double objectWidth = RandomTools.generateRandomDouble(random, 0.04, 0.2);
      double objectHeight = RandomTools.generateRandomDouble(random, 0.04, 0.1);
      double objectMass = RandomTools.generateRandomDouble(random, 0.2, 1.0);

      LinkDescription link = new LinkDescription("object" + i);
      link.setMassAndRadiiOfGyration(objectMass, objectLength / 2.0, objectWidth / 2.0, objectHeight / 2.0);
      link.setCenterOfMassOffset(0.0, 0.0, 0.0);

      LinkGraphicsDescription linkGraphics = new LinkGraphicsDescription();
      linkGraphics.translate(0.0, 0.0, -objectHeight / 2.0);
      AppearanceDefinition randomColor = YoAppearance.randomColor(random);
      linkGraphics.addCube(objectLength, objectWidth, objectHeight, randomColor);
      link.setLinkGraphics(linkGraphics);

      CollisionMeshDescription collisionMesh = new CollisionMeshDescription();
      collisionMesh.addCubeReferencedAtCenter(objectLength, objectWidth, objectHeight);
      link.setCollisionMesh(collisionMesh);

      return link;
   }

   private LinkDescription createRandomSphere(Random random, int i)
   {
      double objectRadius = RandomTools.generateRandomDouble(random, 0.01, 0.05);
      double objectMass = RandomTools.generateRandomDouble(random, 0.2, 1.0);

      LinkDescription link = new LinkDescription("object" + i);
      link.setMassAndRadiiOfGyration(objectMass, objectRadius / 2.0, objectRadius / 2.0, objectRadius / 2.0);
      link.setCenterOfMassOffset(0.0, 0.0, 0.0);

      LinkGraphicsDescription linkGraphics = new LinkGraphicsDescription();
      AppearanceDefinition randomColor = YoAppearance.randomColor(random);
      linkGraphics.addSphere(objectRadius, randomColor);
      link.setLinkGraphics(linkGraphics);

      CollisionMeshDescription collisionMesh = new CollisionMeshDescription();
      collisionMesh.addSphere(objectRadius);
      link.setCollisionMesh(collisionMesh);

      return link;
   }

   private LinkDescription createRandomCapsule(Random random, int i)
   {
      double objectRadius = RandomTools.generateRandomDouble(random, 0.01, 0.05);
      double objectHeight = 2.0 * objectRadius + RandomTools.generateRandomDouble(random, 0.02, 0.05);
      double objectMass = RandomTools.generateRandomDouble(random, 0.2, 1.0);

      LinkDescription link = new LinkDescription("object" + i);
      link.setMassAndRadiiOfGyration(objectMass, objectRadius / 2.0, objectRadius / 2.0, objectHeight / 2.0);
      link.setCenterOfMassOffset(0.0, 0.0, 0.0);

      LinkGraphicsDescription linkGraphics = new LinkGraphicsDescription();
      AppearanceDefinition randomColor = YoAppearance.randomColor(random);
      linkGraphics.addCapsule(objectRadius, objectHeight, randomColor);
      link.setLinkGraphics(linkGraphics);

      CollisionMeshDescription collisionMesh = new CollisionMeshDescription();
      collisionMesh.addCapsule(objectRadius, objectHeight);
      link.setCollisionMesh(collisionMesh);

      return link;
   }

   private LinkDescription createRandomCylinder(Random random, int i)
   {
      double objectHeight = RandomTools.generateRandomDouble(random, 0.05, 0.15);
      double objectRadius = RandomTools.generateRandomDouble(random, 0.01, 0.10);
      double objectMass = RandomTools.generateRandomDouble(random, 0.2, 1.0);

      LinkDescription link = new LinkDescription("object" + i);
      link.setMassAndRadiiOfGyration(objectMass, objectRadius / 2.0, objectRadius / 2.0, objectHeight / 2.0);
      link.setCenterOfMassOffset(0.0, 0.0, 0.0);

      LinkGraphicsDescription linkGraphics = new LinkGraphicsDescription();
      linkGraphics.translate(0.0, 0.0, -objectHeight / 2.0);

      AppearanceDefinition randomColor = YoAppearance.randomColor(random);
      linkGraphics.addCylinder(objectHeight, objectRadius, randomColor);
      link.setLinkGraphics(linkGraphics);

      CollisionMeshDescription collisionMesh = new CollisionMeshDescription();
      collisionMesh.addCylinderReferencedAtCenter(objectRadius, objectHeight);
      link.setCollisionMesh(collisionMesh);

      return link;
   }

   public ArrayList<Robot> getRobots()
   {
      return robots;
   }

}
