package us.ihmc.simulationconstructionset.physics.collision.gdx;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShapeZ;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifold;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.physics.CollisionShape;
import us.ihmc.simulationconstructionset.physics.CollisionShapeDescription;
import us.ihmc.simulationconstructionset.physics.CollisionShapeFactory;
import us.ihmc.simulationconstructionset.physics.CollisionShapeWithLink;
import us.ihmc.simulationconstructionset.physics.ScsCollisionDetector;
import us.ihmc.simulationconstructionset.physics.collision.CollisionDetectionResult;
import us.ihmc.simulationconstructionset.physics.collision.simple.SimpleContactWrapper;

public class GdxCollisionDetector implements ScsCollisionDetector
{
   private List<BulletCollisionShapeWithLink> allShapes = new ArrayList<BulletCollisionShapeWithLink>();
   private final btCollisionWorld collisionWorld;

   private final GdxCollisionFactory factory = new GdxCollisionFactory();

   private YoVariableRegistry registry;

   private RigidBodyTransform transformScs = new RigidBodyTransform();
   private Matrix4 transformGdx = new Matrix4();

   static
   {
      Bullet.init();
   }

   /**
    *
    * @param worldRadius Sets the size of the world for broadphase collision detection.  Should be large enough to contain all the objects
    */
   public GdxCollisionDetector(YoVariableRegistry registryParent, double worldRadius)
   {
      registry = new YoVariableRegistry("GDX");
      registryParent.addChild(registry);

      btDefaultCollisionConfiguration collisionConfiguration = new btDefaultCollisionConfiguration();
      btCollisionDispatcher dispatcher = new btCollisionDispatcher(collisionConfiguration);

      float r = (float) worldRadius;
      Vector3 worldAabbMin = new Vector3(-r, -r, -r);
      Vector3 worldAabbMax = new Vector3(r, r, r);

      btAxisSweep3 broadphase = new btAxisSweep3(worldAabbMin, worldAabbMax);

      collisionWorld = new btCollisionWorld(dispatcher, broadphase, collisionConfiguration);
   }

   @Override
   public void initialize()
   {
   }

   public CollisionShapeFactory getShapeFactory()
   {
      return factory;
   }

   public void removeShape(Link link)
   {
      BulletCollisionShapeWithLink info = (BulletCollisionShapeWithLink) link.getCollisionShape();
      collisionWorld.removeCollisionObject(info);
      allShapes.remove(info);
   }

   public CollisionShape lookupCollisionShape(Link link)
   {
      for (int i = 0; i < allShapes.size(); i++)
      {
         BulletCollisionShapeWithLink info = allShapes.get(i);
         if (info.link == link)
            return info;
      }

      throw new RuntimeException("Can't find matching shape");
   }

   @Override
   public void performCollisionDetection(CollisionDetectionResult result)
   {
      Vector3d world = new Vector3d();

      for (int i = 0; i < allShapes.size(); i++)
      {
         BulletCollisionShapeWithLink info = allShapes.get(i);
         info.getTransformToWorld(transformScs);

         transformScs.getTranslation(world);

         GdxUtil.convert(transformScs, transformGdx);
//         info.setTransformToWorld(transformScs);
         info.setWorldTransform(transformGdx);
      }

      collisionWorld.performDiscreteCollisionDetection();

      int numManifolds = collisionWorld.getDispatcher().getNumManifolds();
      for (int i = 0; i < numManifolds; i++)
      {
         btPersistentManifold contactManifold = collisionWorld.getDispatcher().getManifoldByIndexInternal(i);
         BulletCollisionShapeWithLink obA = (BulletCollisionShapeWithLink) contactManifold.getBody0();
         BulletCollisionShapeWithLink obB = (BulletCollisionShapeWithLink) contactManifold.getBody1();

         SimpleContactWrapper simpleContact = new SimpleContactWrapper(obA, obB);

         int numContacts = contactManifold.getNumContacts();
         for (int j = 0; j < numContacts; j++)
         {
            btManifoldPoint contactPoint = contactManifold.getContactPoint(j);

            Point3d pointOnA = new Point3d();
            Point3d pointOnB = new Point3d();

            Vector3 a = new Vector3();
            contactPoint.getPositionWorldOnA(a);

            Vector3 b = new Vector3();
            contactPoint.getPositionWorldOnB(b);

            GdxUtil.convert(a, pointOnA);
            GdxUtil.convert(b, pointOnB);

            float distance = contactPoint.getDistance();

            Vector3 v = new Vector3();

            contactPoint.getNormalWorldOnB(v);

            Vector3d normalA = new Vector3d();
            normalA.set(-v.x, -v.y, -v.z);

            simpleContact.addContact(pointOnA, pointOnB, normalA, distance);
         }

         result.addContact(simpleContact);

         // you can un-comment out this line, and then all points are removed
//         contactManifold.clearManifold();
      }
   }

   public class GdxCollisionFactory implements CollisionShapeFactory
   {
      float margin = (float) CollisionShapeFactory.DEFAULT_MARGIN;

      public void setMargin(double margin)
      {
         this.margin = (float) margin;
      }

      public CollisionShapeDescription createBox(double radiusX, double radiusY, double radiusZ)
      {
         btBoxShape box = new btBoxShape(new Vector3((float) radiusX, (float) radiusY, (float) radiusZ));
         box.setMargin(margin);

         return new ShapeDescription(box);
      }

      public CollisionShapeDescription createCylinder(double radius, double height)
      {
         btCylinderShape shape = new btCylinderShapeZ(new Vector3((float) radius, (float) radius, (float) height / 2.0f));
         shape.setMargin((float) margin);

         return new ShapeDescription(shape);
      }

      public CollisionShapeDescription createSphere(double radius)
      {
         btSphereShape shape = new btSphereShape((float) radius);
         shape.setMargin(margin);

         return new ShapeDescription(shape);
      }

      public CollisionShape addShape(Link link, RigidBodyTransform shapeToLink, CollisionShapeDescription description, boolean isGround, int collisionGroup, int collisionMask)
      {
         if (shapeToLink == null)
         {
            shapeToLink = new RigidBodyTransform();
         }

         BulletCollisionShapeWithLink shape = new BulletCollisionShapeWithLink("shape" + allShapes.size(), (ShapeDescription) description, link, isGround, shapeToLink);
         collisionWorld.addCollisionObject(shape, (short) collisionGroup, (short) collisionMask);

         allShapes.add(shape);

         return shape;
      }
   }

   /**
    * Just a wrapper around {@link com.bulletphysics.collision.shapes.CollisionShape}.
    */
   public static class ShapeDescription implements CollisionShapeDescription
   {
      public btCollisionShape shape;

      public ShapeDescription(btCollisionShape shape)
      {
         this.shape = shape;
      }
   }

   /**
    * Reference to the shape's description and its link.
    */
   private static class BulletCollisionShapeWithLink extends btCollisionObject implements CollisionShapeWithLink
   {
      private final ShapeDescription description;
      private final Link link;
      private final boolean isGround;

      // transform from shapeToLink coordinate system
      private final RigidBodyTransform shapeToLink = new RigidBodyTransform();

      public BulletCollisionShapeWithLink(String name, ShapeDescription description, Link link, boolean isGround, RigidBodyTransform shapeToLink)
      {
         this.description = description;
         this.link = link;
         this.isGround = isGround;

         this.shapeToLink.set(shapeToLink);
         setCollisionFlags(CollisionFlags.CF_KINEMATIC_OBJECT);
         setCollisionShape(description.shape);
      }

      private final RigidBodyTransform tempTransform = new RigidBodyTransform();

      @Override
      public CollisionShapeDescription getDescription()
      {
         return description;
      }

      @Override
      public Link getLink()
      {
         return link;
      }

      @Override
      public boolean isGround()
      {
         return isGround;
      }

      @Override
      public void getShapeToLink(RigidBodyTransform shapeToLinkToPack)
      {
         shapeToLinkToPack.set(shapeToLink);
      }

      @Override
      public int getGroupMask()
      {
         return getBroadphaseHandle().getCollisionFilterGroup() & 0xFFFF;
      }

      @Override
      public int getCollisionMask()
      {
         return getBroadphaseHandle().getCollisionFilterMask() & 0xFFFF;
      }

      @Override
      public void getTransformToWorld(RigidBodyTransform transformToWorldToPack)
      {
         link.getParentJoint().getTransformToWorld(tempTransform);
         transformToWorldToPack.multiply(tempTransform, shapeToLink);
      }

      @Override
      public void setTransformToWorld(RigidBodyTransform transformToWorld)
      {
         throw new RuntimeException("Shouldn't call this!");
      }
   }

}
