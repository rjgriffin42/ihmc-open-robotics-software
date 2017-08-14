package us.ihmc.robotics.geometry;

import java.util.List;
import java.util.Random;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.robotics.geometry.interfaces.PointInterface;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

/**
 * One of the main goals of this class is to check, at runtime, that operations on points occur
 * within the same Frame. This method checks for one Vector argument.
 *
 * @author Learning Locomotion Team
 * @version 2.0
 */
public class FramePoint3D extends FrameTuple3D<FramePoint3D, Point3D> implements PointInterface
{
   private static final long serialVersionUID = -4831948077397801540L;

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, Tuple3DReadOnly position)
   {
      this(referenceFrame, position, null);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, Tuple3DReadOnly position, String name)
   {
      super(referenceFrame, new Point3D(position), name);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, double[] position)
   {
      this(referenceFrame, position, null);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, double[] position, String name)
   {
      super(referenceFrame, new Point3D(position), name);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D()
   {
      this(ReferenceFrame.getWorldFrame());
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame)
   {
      super(referenceFrame, new Point3D(), null);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, String name)
   {
      super(referenceFrame, new Point3D(), name);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(FrameTuple3D<?, ?> frameTuple)
   {
      super(frameTuple.referenceFrame, new Point3D(frameTuple.tuple), frameTuple.name);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(FrameTuple2D<?, ?> frameTuple2d)
   {
      super(frameTuple2d.referenceFrame, new Point3D(), frameTuple2d.name);
      setXY(frameTuple2d);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, double x, double y, double z)
   {
      this(referenceFrame, x, y, z, null);
   }

   /**
    * FramePoint
    * <p/>
    * A normal point associated with a specific reference frame.
    */
   public FramePoint3D(ReferenceFrame referenceFrame, double x, double y, double z, String name)
   {
      super(referenceFrame, new Point3D(x, y, z), name);
   }

   public static FramePoint3D average(List<? extends FramePoint3D> framePoints)
   {
      ReferenceFrame frame = framePoints.get(0).getReferenceFrame();
      FramePoint3D nextPoint = new FramePoint3D(frame);

      FramePoint3D average = new FramePoint3D(framePoints.get(0));
      for (int i = 1; i < framePoints.size(); i++)
      {
         nextPoint.set(framePoints.get(i));
         nextPoint.changeFrame(frame);
         average.add(nextPoint);
      }

      average.scale(1.0 / ((double) framePoints.size()));
      return average;
   }

   public static FramePoint3D generateRandomFramePoint(Random random, ReferenceFrame frame, double xMaxAbsoluteX, double yMaxAbsoluteY, double zMaxAbsoluteZ)
   {
      FramePoint3D randomPoint = new FramePoint3D(frame, RandomNumbers.nextDouble(random, xMaxAbsoluteX),
                                              RandomNumbers.nextDouble(random, yMaxAbsoluteY), RandomNumbers.nextDouble(random, zMaxAbsoluteZ));
      return randomPoint;
   }

   public static FramePoint3D generateRandomFramePoint(Random random, ReferenceFrame frame, double xMin, double xMax, double yMin, double yMax, double zMin,
                                                     double zMax)
   {
      FramePoint3D randomPoint = new FramePoint3D(frame, RandomNumbers.nextDouble(random, xMin, xMax), RandomNumbers.nextDouble(random, yMin, yMax),
                                              RandomNumbers.nextDouble(random, zMin, zMax));
      return randomPoint;
   }

   public double getXYPlaneDistance(FramePoint3D framePoint)
   {
      checkReferenceFrameMatch(framePoint);

      double dx, dy;

      dx = this.getX() - framePoint.getX();
      dy = this.getY() - framePoint.getY();
      return Math.sqrt(dx * dx + dy * dy);
   }

   public double getXYPlaneDistance(FramePoint2D framePoint2d)
   {
      checkReferenceFrameMatch(framePoint2d);

      double dx, dy;

      dx = this.getX() - framePoint2d.getX();
      dy = this.getY() - framePoint2d.getY();
      return Math.sqrt(dx * dx + dy * dy);
   }

   public double distance(Point3DReadOnly point)
   {
      return this.tuple.distance(point);
   }

   public double distance(FramePoint3D framePoint)
   {
      checkReferenceFrameMatch(framePoint);

      return this.tuple.distance(framePoint.tuple);
   }

   public double distanceSquared(FramePoint3D framePoint)
   {
      checkReferenceFrameMatch(framePoint);

      return this.tuple.distanceSquared(framePoint.tuple);
   }

   public double distanceFromOrigin()
   {
      return tuple.distanceFromOrigin();
   }

   public double distanceFromOriginSquared()
   {
      return tuple.distanceFromOriginSquared();
   }

   /**
    * Creates a new FramePoint2d based on the x and y components of this FramePoint
    */
   public FramePoint2D toFramePoint2d()
   {
      return new FramePoint2D(this.getReferenceFrame(), this.getX(), this.getY());
   }

   /**
    * Makes the pointToPack a FramePoint2d version of this FramePoint
    * 
    * @param pointToPack
    */
   public void getFramePoint2d(FramePoint2D pointToPack)
   {
      pointToPack.setIncludingFrame(this.getReferenceFrame(), this.getX(), this.getY());
   }

   public void getPoint2d(Point2DBasics pointToPack)
   {
      pointToPack.setX(this.getX());
      pointToPack.setY(this.getY());
   }

   /**
    * Makes the tupleToPack a FrameTuple2d version of this FramePoint
    * 
    * @param tupleToPack
    */
   public void getFrameTuple2d(FrameTuple2D<?, ?> tupleToPack)
   {
      tupleToPack.setIncludingFrame(this.getReferenceFrame(), this.getX(), this.getY());
   }

   /**
    * Returns the Point3D used in this FramePoint
    *
    * @return Point3D
    */
   public Point3D getPoint()
   {
      return this.tuple;
   }

   public static FramePoint3D getMidPoint(FramePoint3D point1, FramePoint3D point2)
   {
      point1.checkReferenceFrameMatch(point2);
      FramePoint3D midPoint = new FramePoint3D(point1.referenceFrame);
      midPoint.interpolate(point1, point2, 0.5);
      return midPoint;
   }

   /**
    * yawAboutPoint
    *
    * @param pointToYawAbout FramePoint
    * @param yaw double
    * @return CartesianPositionFootstep
    */
   public void yawAboutPoint(FramePoint3D pointToYawAbout, FramePoint3D resultToPack, double yaw)
   {
      checkReferenceFrameMatch(pointToYawAbout);
      double tempX = getX() - pointToYawAbout.getX();
      double tempY = getY() - pointToYawAbout.getY();
      double tempZ = getZ() - pointToYawAbout.getZ();

      double cosAngle = Math.cos(yaw);
      double sinAngle = Math.sin(yaw);

      double x = cosAngle * tempX + -sinAngle * tempY;
      tempY = sinAngle * tempX + cosAngle * tempY;
      tempX = x;

      resultToPack.setIncludingFrame(pointToYawAbout);
      resultToPack.add(tempX, tempY, tempZ);
   }

   public void pitchAboutPoint(FramePoint3D pointToPitchAbout, FramePoint3D resultToPack, double pitch)
   {
      checkReferenceFrameMatch(pointToPitchAbout);
      double tempX = getX() - pointToPitchAbout.getX();
      double tempY = getY() - pointToPitchAbout.getY();
      double tempZ = getZ() - pointToPitchAbout.getZ();

      double cosAngle = Math.cos(pitch);
      double sinAngle = Math.sin(pitch);

      double x = cosAngle * tempX + sinAngle * tempZ;
      tempZ = -sinAngle * tempX + cosAngle * tempZ;
      tempX = x;

      resultToPack.setIncludingFrame(pointToPitchAbout);
      resultToPack.add(tempX, tempY, tempZ);
   }

   @Override
   public void getPoint(Point3D pointToPack)
   {
      this.get(pointToPack);
   }

   @Override
   public void setPoint(PointInterface pointInterface)
   {
      pointInterface.getPoint(this.getPoint());
   }

   @Override
   public void setPoint(Point3D point)
   {
      this.set(point);
   }

   /**
    * Sets this point to the location of the origin of passed in referenceFrame.
    */
   @Override
   public void setFromReferenceFrame(ReferenceFrame referenceFrame)
   {
      super.setFromReferenceFrame(referenceFrame);
   }
}
