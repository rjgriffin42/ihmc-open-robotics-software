package us.ihmc.robotics.geometry.shapes;

import us.ihmc.euclid.geometry.Plane3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.ReferenceFrameHolder;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.robotics.geometry.FrameLine3D;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector3D;

public class FramePlane3d implements ReferenceFrameHolder
{
   private ReferenceFrame referenceFrame;
   private Plane3D plane3d;

   private final RigidBodyTransform temporaryTransformToDesiredFrame = new RigidBodyTransform();
   private final Vector3D temporaryVector = new Vector3D();
   private final Point3D temporaryPoint = new Point3D();
   
   private final Point3D temporaryPointA = new Point3D();
   private final Point3D temporaryPointB = new Point3D();
   private final Point3D temporaryPointC = new Point3D();

   public FramePlane3d()
   {
      this(ReferenceFrame.getWorldFrame());
   }
   
   public FramePlane3d(ReferenceFrame referenceFrame, Plane3D plane3d)
   {
      this.referenceFrame = referenceFrame;
      this.plane3d = plane3d;
   }

   public FramePlane3d(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      this.plane3d = new Plane3D();
   }

   public FramePlane3d(FramePlane3d framePlane3d)
   {
      this.referenceFrame = framePlane3d.referenceFrame;
      this.plane3d = new Plane3D(framePlane3d.plane3d);
   }

   public FramePlane3d(FrameVector3D normal, FramePoint3D point)
   {
      normal.checkReferenceFrameMatch(point);
      this.referenceFrame = normal.getReferenceFrame();
      this.plane3d = new Plane3D(point.getPoint(), normal.getVector());
   }

   public FramePlane3d(ReferenceFrame referenceFrame, Point3DReadOnly point, Vector3DReadOnly normal)
   {
      this.referenceFrame = referenceFrame;
      this.plane3d = new Plane3D(point, normal);
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   public void getNormal(FrameVector3D normalToPack)
   {
      checkReferenceFrameMatch(normalToPack.getReferenceFrame());
      this.plane3d.getNormal(temporaryVector);
      normalToPack.set(temporaryVector);
   }

   public FrameVector3D getNormalCopy()
   {
      FrameVector3D returnVector = new FrameVector3D(this.referenceFrame);
      getNormal(returnVector);
      return returnVector;
   }
   
   public Vector3DReadOnly getNormal()
   {
      return plane3d.getNormal();
   }

   public void setNormal(double x, double y, double z)
   {
      plane3d.setNormal(x, y, z);
   }

   public void setNormal(Vector3DReadOnly normal)
   {
      plane3d.setNormal(normal);
   }

   public void getPoint(FramePoint3D pointToPack)
   {
      checkReferenceFrameMatch(pointToPack.getReferenceFrame());
      this.plane3d.getPoint(temporaryPoint);
      pointToPack.set(temporaryPoint);
   }

   public FramePoint3D getPointCopy()
   {
      FramePoint3D pointToReturn = new FramePoint3D(this.getReferenceFrame());
      this.getPoint(pointToReturn);
      return pointToReturn;
   }
   
   public Point3DReadOnly getPoint()
   {
      return plane3d.getPoint();
   }

   public void setPoint(double x, double y, double z)
   {
      plane3d.setPoint(x, y, z);
   }

   public void setPoint(Point3DReadOnly point)
   {
      plane3d.setPoint(point);
   }

   public void setPoints(FramePoint3D pointA, FramePoint3D pointB, FramePoint3D pointC)
   {
      pointA.checkReferenceFrameMatch(referenceFrame);
      pointB.checkReferenceFrameMatch(referenceFrame);
      pointC.checkReferenceFrameMatch(referenceFrame);

      pointA.get(temporaryPointA);
      pointB.get(temporaryPointB);
      pointC.get(temporaryPointC);
      
      plane3d.set(temporaryPointA, temporaryPointB, temporaryPointC);
   }
   
   public void changeFrame(ReferenceFrame desiredFrame)
   {
      if (desiredFrame != referenceFrame)
      {
         referenceFrame.getTransformToDesiredFrame(temporaryTransformToDesiredFrame, desiredFrame);
         plane3d.applyTransform(temporaryTransformToDesiredFrame);
         referenceFrame = desiredFrame;
      }

      // otherwise: in the right frame already, so do nothing
   }

   public boolean isOnOrAbove(FramePoint3D pointToTest)
   {
      checkReferenceFrameMatch(pointToTest);

      return plane3d.isOnOrAbove(pointToTest.getPoint());
   }

   public boolean isOnOrBelow(FramePoint3D pointToTest)
   {
      checkReferenceFrameMatch(pointToTest);

      return plane3d.isOnOrBelow(pointToTest.getPoint());
   }

   /**
    * Tests if the two planes are parallel by testing if their normals are collinear.
    * The latter is done given a tolerance on the angle between the two normal axes in the range ]0; <i>pi</i>/2[.
    * 
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either normal is below {@code 1.0E-7}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param otherPlane the other plane to do the test with. Not modified.
    * @param angleEpsilon tolerance on the angle in radians.
    * @return {@code true} if the two planes are parallel, {@code false} otherwise.
    */
   public boolean isParallel(FramePlane3d otherPlane, double angleEpsilon)
   {
      checkReferenceFrameMatch(otherPlane);
      return plane3d.isParallel(otherPlane.plane3d, angleEpsilon);
   }

   /**
    * Tests if this plane and the given plane are coincident:
    * <ul>
    *    <li> {@code this.normal} and {@code otherPlane.normal} are collinear given the tolerance {@code angleEpsilon}.
    *    <li> the distance of {@code otherPlane.point} from the this plane is less than {@code distanceEpsilon}.
    * </ul>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either normal is below {@code 1.0E-7}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param otherPlane the other plane to do the test with. Not modified.
    * @param angleEpsilon tolerance on the angle in radians to determine if the plane normals are collinear. 
    * @param distanceEpsilon tolerance on the distance to determine if {@code otherPlane.point} belongs to this plane.
    * @return {@code true} if the two planes are coincident, {@code false} otherwise.
    */
   public boolean isCoincident(FramePlane3d otherPlane, double angleEpsilon, double distanceEpsilon)
   {
      checkReferenceFrameMatch(otherPlane);
      return plane3d.isCoincident(otherPlane.plane3d, angleEpsilon, distanceEpsilon);
   }

   public FramePoint3D orthogonalProjectionCopy(FramePoint3D point)
   {
      checkReferenceFrameMatch(point);
      FramePoint3D returnPoint = new FramePoint3D(point);
      orthogonalProjection(returnPoint);

      return returnPoint;
   }

   public void orthogonalProjection(FramePoint3D point)
   {
      checkReferenceFrameMatch(point);
      plane3d.orthogonalProjection(point.getPoint());
   }

   public double getZOnPlane(FramePoint2d xyPoint)
   {
      checkReferenceFrameMatch(xyPoint.getReferenceFrame());
      return plane3d.getZOnPlane(xyPoint.getX(), xyPoint.getY());
   }
   
   public double distance(FramePoint3D point)
   {
      checkReferenceFrameMatch(point);
      return plane3d.distance(point.getPoint());
   }

   public boolean epsilonEquals(FramePlane3d plane, double epsilon)
   {
      checkReferenceFrameMatch(plane.getReferenceFrame());

      return ((referenceFrame == plane.getReferenceFrame()) && (plane.plane3d.epsilonEquals(this.plane3d, epsilon)));
   }

   public FramePlane3d applyTransformCopy(RigidBodyTransform transformation)
   {
      FramePlane3d returnPlane = new FramePlane3d(this);
      returnPlane.applyTransform(transformation);

      return returnPlane;
   }

   public void applyTransform(RigidBodyTransform transformation)
   {
      plane3d.applyTransform(transformation);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, double pointX, double pointY, double pointZ, double normalX, double normalY, double normalZ)
   {
      this.referenceFrame = referenceFrame;
      plane3d.setPoint(pointX, pointY, pointZ);
      plane3d.setNormal(normalX, normalY, normalZ);
   }
   
   public void getIntersectionWithLine(FramePoint3D pointToPack, FrameLine3D line)
   {
	   checkReferenceFrameMatch(line.getReferenceFrame());
	   checkReferenceFrameMatch(pointToPack.getReferenceFrame());
	   
	   Point3D intersectionToPack = new Point3D();
	   plane3d.intersectionWith(intersectionToPack, line.getPoint(), line.getDirection());
	   pointToPack.set(intersectionToPack);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append("ReferenceFrame = " + referenceFrame + ", " + plane3d.toString());

      return builder.toString();
   }
}
