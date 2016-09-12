package us.ihmc.simulationconstructionset.physics.collision;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.physics.CollisionShapeDescription;
import us.ihmc.simulationconstructionset.physics.CollisionShapeFactory;
import us.ihmc.simulationconstructionset.physics.ScsCollisionDetector;
import us.ihmc.simulationconstructionset.physics.collision.gdx.GdxCollisionDetector;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class SCSCollisionDetectorEvaluator
{

   public SCSCollisionDetectorEvaluator()
   {
      double lengthX = 1.0;
      double widthY = 1.0;
      double heightZ = 1.0;

      double halfX = lengthX/2.0;
      double halfY = widthY/2.0;
      double halfZ = heightZ/2.0;

      Robot robot = new Robot("robot");
      IntegerYoVariable numberOfCollisions = new IntegerYoVariable("numberOfCollisions", robot.getRobotsYoVariableRegistry());

      ArrayList<YoGraphicPosition> pointsOnA = new ArrayList<>();
      ArrayList<YoGraphicPosition> pointsOnB = new ArrayList<>();

      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

      for (int i=0; i<4; i++)
      {
         YoGraphicPosition pointOnAViz = new YoGraphicPosition("pointOnA", "_" + i, robot.getRobotsYoVariableRegistry(), 0.03, YoAppearance.Purple());
         YoGraphicPosition pointOnBViz = new YoGraphicPosition("pointOnB", "_" + i, robot.getRobotsYoVariableRegistry(), 0.03, YoAppearance.Gold());

         yoGraphicsListRegistry.registerYoGraphic("Collision", pointOnAViz);
         yoGraphicsListRegistry.registerYoGraphic("Collision", pointOnBViz);

         pointsOnA.add(pointOnAViz);
         pointsOnB.add(pointOnBViz);
      }

      robot.addDynamicGraphicObjectsListRegistry(yoGraphicsListRegistry );
      // Create the joints:
      FloatingJoint jointOne = new FloatingJoint("one", "cubeOne", new Vector3d(), robot);
      Link linkOne = new Link("CubeOne");

      FloatingJoint jointTwo = new FloatingJoint("two", "cubeTwo", new Vector3d(), robot);
      Link linkTwo = new Link("CubeTwo");

      // Set mass parameters
      double mass = 1.0;

      linkOne.setMass(mass);
      linkOne.setMomentOfInertia(0.1 * mass, 0.1 * mass, 0.1 * mass);
      linkOne.enableCollisions(10.0, robot.getRobotsYoVariableRegistry());

      linkTwo.setMass(mass);
      linkTwo.setMomentOfInertia(0.1 * mass, 0.1 * mass, 0.1 * mass);
      linkTwo.enableCollisions(10.0, robot.getRobotsYoVariableRegistry());

      // Graphics
      Graphics3DObject linkOneGraphics = new Graphics3DObject();
      linkOneGraphics.translate(0.0, 0.0, -halfZ);
      linkOneGraphics.addCube(lengthX, widthY, heightZ, YoAppearance.Red());
      linkOne.setLinkGraphics(linkOneGraphics);

      Graphics3DObject linkTwoGraphics = new Graphics3DObject();
      linkTwoGraphics.translate(0.0, 0.0, -halfZ);
      linkTwoGraphics.addCube(lengthX, widthY, heightZ, YoAppearance.Green());
      linkTwo.setLinkGraphics(linkTwoGraphics);

      // Collison Detector
      double worldRadius = 100.0;
      ScsCollisionDetector collisionDetector = new GdxCollisionDetector(robot.getRobotsYoVariableRegistry(), worldRadius);

      CollisionShapeFactory factory = collisionDetector.getShapeFactory();
      factory.setMargin(0.002);
      CollisionShapeDescription shapeDescriptionOne = factory.createBox(halfX, halfY, halfZ);

      int collisionGroup = 0xFFFFFFFF;
      int collisionMask = 0xFFFFFFFF;
      RigidBodyTransform shapeToLinkOne = new RigidBodyTransform();

      factory.addShape(linkOne, shapeToLinkOne, shapeDescriptionOne, false, collisionGroup, collisionMask);

      CollisionShapeDescription shapeDescriptionTwo = factory.createBox(halfX, halfY, halfZ);
//      CollisionShapeDescription shapeDescriptionTwo = factory.createSphere(halfX);
      RigidBodyTransform shapeToLinkTwo = new RigidBodyTransform();
      factory.addShape(linkTwo, shapeToLinkTwo, shapeDescriptionTwo, false, collisionGroup, collisionMask);

      // Assemble
      jointOne.setLink(linkOne);
      jointTwo.setLink(linkTwo);

      robot.addRootJoint(jointOne);
      robot.addRootJoint(jointTwo);

      robot.setGravity(0.0);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      scs.setGroundVisible(false);
      scs.startOnAThread();


      double x = -1.2;
      double y = 0.0;
      double z = 0.0;

      for (int i=0; i<4000; i++)
      {
         jointOne.setPosition(x, y, z);
         robot.update();

         x = x + 0.001;
         robot.setTime(robot.getTime() + 0.001);

         CollisionDetectionResult result = new CollisionDetectionResult();
         collisionDetector.performCollisionDetection(result);

         numberOfCollisions.set(result.getNumberOfCollisions());

         for (int j=0; j<pointsOnA.size(); j++)
         {
            pointsOnA.get(j).setPosition(Double.NaN, Double.NaN, Double.NaN);
            pointsOnB.get(j).setPosition(Double.NaN, Double.NaN, Double.NaN);
         }

         for (int j=0; j<numberOfCollisions.getIntegerValue(); j++)
         {
            DetectedCollision collision = result.getCollision(j);

            Point3d pointOnA = new Point3d();
            collision.getPointOnA(pointOnA);
            pointsOnA.get(j).setPosition(pointOnA);

            Point3d pointOnB = new Point3d();
            collision.getPointOnB(pointOnB);
            pointsOnB.get(j).setPosition(pointOnB);
         }

         scs.tickAndUpdate();
      }

   }

   public static void main(String[] args)
   {
      new SCSCollisionDetectorEvaluator();
   }
}
