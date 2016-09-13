package us.ihmc.simulationconstructionset.physics;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A list of points between the two shapes which are in contact with each other.
 */
public interface Contacts
{
   public abstract CollisionShape getShapeA();
   public abstract CollisionShape getShapeB();

   /**
    * Total number of contacts found between the two shapes.
    */
   public int getNumberOfContacts();

   /**
    * Location on shapeA that the contact occurred.  World coordinates.
    *
    * @param which Contact index.
    * @param location Storage for location.  If null a new instance will be declared.
    */
   public void getWorldA(int which, Point3d locationAToPack);

   /**
    * Location on shapeB that the contact occurred.  World coordinates.
    *
    * @param which Contact index.
    * @param location Storage for location.  If null a new instance will be declared.
    */
   public void getWorldB(int which, Point3d locationBToPack);

   /**
    * Distance between the two points.
    */
   public double getDistance(int which);

   /**
    * The normal between the collision in world coordinates.  The normal can be in reference to object A or B.  Call {@link #isNormalOnA()}  to
    * determin which one it is.
    *
    * @param which Contact index.
    * @return Normal in world coordinates.
    */
   public void getWorldNormal(int which, Vector3d normalToPack);

   /**
    * Is the normal for the surface of A or B.
    *
    * @return true for normal being on A and false for B.
    */
   public boolean isNormalOnA();
}

