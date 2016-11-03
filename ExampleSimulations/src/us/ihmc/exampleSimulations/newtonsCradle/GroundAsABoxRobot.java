package us.ihmc.exampleSimulations.newtonsCradle;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.NullJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.physics.CollisionShape;
import us.ihmc.simulationconstructionset.physics.CollisionShapeDescription;
import us.ihmc.simulationconstructionset.physics.CollisionShapeFactory;
import us.ihmc.simulationconstructionset.physics.ScsCollisionDetector;

public class GroundAsABoxRobot extends Robot
{
   private final Link baseLink;
   private final CollisionShapeFactory collisionShapeFactory;

   public GroundAsABoxRobot(ScsCollisionDetector collisionDetector)
   {
      this(collisionDetector, false);
   }

   public GroundAsABoxRobot(ScsCollisionDetector collisionDetector, boolean addWalls)
   {
      super("GroundAsABoxRobot");
      NullJoint baseJoint = new NullJoint("base", new Vector3d(), this);

      //    FloatingJoint baseJoint = new FloatingJoint("base", new Vector3d(), this);
      baseLink = new Link("base");
      baseLink.setMassAndRadiiOfGyration(1000000000.0, 100.0, 100.0, 100.0);
      collisionShapeFactory = collisionDetector.getShapeFactory();

      double floorLength = 4.0;
      double floorWidth = 4.0;
      double floorThickness = 0.05;

      Graphics3DObject baseLinkGraphics = new Graphics3DObject();
      baseLinkGraphics.translate(0.0, 0.0, -floorThickness / 2.0);
      baseLinkGraphics.addCube(floorLength, floorWidth, floorThickness, YoAppearance.Green());

      CollisionShapeDescription<?> groundShapeDescription = collisionShapeFactory.createBox(floorLength / 2.0, floorWidth / 2.0, floorThickness / 2.0);
      RigidBodyTransform shapeToLinkTransform = new RigidBodyTransform();
      shapeToLinkTransform.setTranslation(new Vector3d(-0.0, 0.0, 0.0));
      CollisionShape groundShape = collisionShapeFactory.addShape(baseLink, shapeToLinkTransform, groundShapeDescription, true, 0xFFFFFFFF, 0xFFFFFFFF);
      groundShape.setIsGround(true);


      if (addWalls)
      {
         double offsetX = -0.75;
         double offsetY = 0.0;
         double xRotation = 0.0;
         double yRotation = Math.PI/8.0;
         addWall(floorLength, floorWidth, floorThickness, baseLinkGraphics, offsetX, offsetY, xRotation, yRotation);

         offsetX = 0.75;
         offsetY = 0.0;
         xRotation = 0.0;
         yRotation = -Math.PI/8.0;
         addWall(floorLength, floorWidth, floorThickness, baseLinkGraphics, offsetX, offsetY, xRotation, yRotation);

         offsetX = 0.0;
         offsetY = -0.75;
         xRotation = -Math.PI/8.0;
         yRotation = 0.0;
         addWall(floorLength, floorWidth, floorThickness, baseLinkGraphics, offsetX, offsetY, xRotation, yRotation);

         offsetX = 0.0;
         offsetY = 0.75;
         xRotation = Math.PI/8.0;
         yRotation = 0.0;
         addWall(floorLength, floorWidth, floorThickness, baseLinkGraphics, offsetX, offsetY, xRotation, yRotation);
      }



      //    baseJoint.setVelocity(0.0, 0.0, 1.0);


      baseLink.setLinkGraphics(baseLinkGraphics);
      baseLink.enableCollisions(100.0, this.getRobotsYoVariableRegistry());


      baseJoint.setLink(baseLink);
      this.addRootJoint(baseJoint);
      this.addStaticLink(baseLink);
   }

   private void addWall(double floorLength, double floorWidth, double floorThickness, Graphics3DObject baseLinkGraphics, double offsetX, double offsetY, double xRotation, double yRotation)
   {
      CollisionShapeDescription<?> groundShapeDescription;
      RigidBodyTransform shapeToLinkTransform;
      CollisionShape groundShape;
      baseLinkGraphics.identity();
      baseLinkGraphics.translate(new Vector3d(offsetX, offsetY, -floorThickness / 2.0));
      Matrix3d rotationMatrixX = new Matrix3d();
      rotationMatrixX.rotX(xRotation);
      baseLinkGraphics.rotate(rotationMatrixX);
      Matrix3d rotationMatrixY = new Matrix3d();
      rotationMatrixY.rotY(yRotation);
      baseLinkGraphics.rotate(rotationMatrixY);
      baseLinkGraphics.addCube(floorLength, floorWidth, floorThickness, YoAppearance.Green());

      groundShapeDescription = collisionShapeFactory.createBox(floorLength / 2.0, floorWidth / 2.0, floorThickness / 2.0);
      shapeToLinkTransform = new RigidBodyTransform();
      shapeToLinkTransform.setRotationEulerAndZeroTranslation(new Vector3d(xRotation, yRotation, 0.0));
      shapeToLinkTransform.setTranslation(new Vector3d(offsetX, offsetY, 0.0));

      groundShape = collisionShapeFactory.addShape(baseLink, shapeToLinkTransform, groundShapeDescription, true, 0xFFFFFFFF, 0xFFFFFFFF);
      groundShape.setIsGround(true);
   }
}
