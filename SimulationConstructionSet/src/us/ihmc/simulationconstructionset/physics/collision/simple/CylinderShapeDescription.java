package us.ihmc.simulationconstructionset.physics.collision.simple;

import javax.vecmath.Point3d;

import us.ihmc.geometry.polytope.CylinderSupportingVertexHolder;
import us.ihmc.geometry.polytope.SupportingVertexHolder;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.shapes.Cylinder3d;
import us.ihmc.simulationconstructionset.physics.CollisionShapeDescription;

public class CylinderShapeDescription<T extends CylinderShapeDescription<T>> implements CollisionShapeDescription<T>
{
   private final CylinderSupportingVertexHolder supportingVertexHolder;
   private double radius;
   private double height;
   private double smoothingRadius = 0.0;

   private final RigidBodyTransform transform = new RigidBodyTransform();

   private final RigidBodyTransform cylinderConsistencyTransform = new RigidBodyTransform();

   //TODO: Get rid of this redundancy. Make cylinder definitions consistent...
   private final Cylinder3d cylinder3d;

   public CylinderShapeDescription(double radius, double height)
   {
      supportingVertexHolder = new CylinderSupportingVertexHolder(radius, height);

      this.radius = radius;
      this.height = height;

      cylinder3d = new Cylinder3d(height, radius);

      cylinderConsistencyTransform.setTranslation(0.0, 0.0, -height / 2.0);
      cylinder3d.setTransform(cylinderConsistencyTransform);
   }

   public double getRadius()
   {
      return radius;
   }

   public double getHeight()
   {
      return height;
   }

   public void getTransform(RigidBodyTransform transformToPack)
   {
      transformToPack.set(transform);
   }

   @Override
   public CylinderShapeDescription<T> copy()
   {
      CylinderShapeDescription<T> copy = new CylinderShapeDescription<T>(radius, height);
      copy.smoothingRadius = this.smoothingRadius;
      copy.setTransform(this.transform);
      return copy;
   }

   public void setTransform(RigidBodyTransform transform2)
   {
      this.transform.set(this.transform);
      this.supportingVertexHolder.setTransform(transform);

      this.cylinder3d.setTransform(cylinderConsistencyTransform);
      this.cylinder3d.applyTransform(transform);
   }

   public double getSmoothingRadius()
   {
      return smoothingRadius;
   }

   @Override
   public void applyTransform(RigidBodyTransform transformToWorld)
   {
      transform.multiply(transformToWorld, transform);
      supportingVertexHolder.setTransform(transform);

      this.cylinder3d.setTransform(cylinderConsistencyTransform);
      this.cylinder3d.applyTransform(transform);
   }

   @Override
   public void setFrom(T cylinder)
   {
      this.radius = cylinder.getRadius();
      this.height = cylinder.getHeight();

      cylinder.getTransform(this.transform);
      supportingVertexHolder.setTransform(transform);

      this.cylinder3d.setTransform(cylinderConsistencyTransform);
      this.cylinder3d.applyTransform(transform);
   }

   public SupportingVertexHolder getSupportingVertexHolder()
   {
      return supportingVertexHolder;
   }

   public void getProjection(Point3d centerOfSphere, Point3d closestPointOnCylinderToPack)
   {
      closestPointOnCylinderToPack.set(centerOfSphere);
      cylinder3d.orthogonalProjection(closestPointOnCylinderToPack);
   }
}
