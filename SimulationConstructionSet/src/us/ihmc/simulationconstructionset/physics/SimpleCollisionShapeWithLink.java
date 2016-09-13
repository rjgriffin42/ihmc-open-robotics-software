package us.ihmc.simulationconstructionset.physics;

import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.physics.collision.simple.SimpleCollisionShape;

public class SimpleCollisionShapeWithLink extends SimpleCollisionShape implements CollisionShapeWithLink
{
   private final Link link;

   private final RigidBodyTransform shapeToLink = new RigidBodyTransform();
   private final RigidBodyTransform tempTransform = new RigidBodyTransform();

   public SimpleCollisionShapeWithLink(Link link, CollisionShapeDescription collisionShapeDescription, RigidBodyTransform shapeToLink)
   {
      super(collisionShapeDescription);
      this.link = link;
      this.shapeToLink.set(shapeToLink);
   }

   @Override
   public Link getLink()
   {
      return link;
   }

   @Override
   public void getShapeToLink(RigidBodyTransform shapeToLinkToPack)
   {
      shapeToLinkToPack.set(shapeToLink);
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
