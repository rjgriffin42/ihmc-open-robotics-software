package us.ihmc.robotics.geometry;

import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import us.ihmc.robotics.geometry.interfaces.PointInterface;
import us.ihmc.robotics.geometry.transformables.TransformablePoint3d;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

/**
 * One of the main goals of this class is to check, at runtime, that operations on points occur within the same Frame.
 * This method checks for one Vector argument.
 *
 * @author Learning Locomotion Team
 * @version 2.0
 */
public class FramePoint extends FrameTuple<FramePoint, TransformablePoint3d> implements PointInterface
{
   private static final long serialVersionUID = -4831948077397801540L;

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, Tuple3d position)
   {
      this(referenceFrame, position, null);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, Tuple3d position, String name)
   {
      super(referenceFrame, new TransformablePoint3d(position), name);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, double[] position)
   {
      this(referenceFrame, position, null);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, double[] position, String name)
   {
      super(referenceFrame, new TransformablePoint3d(position), name);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint()
   {
      this(ReferenceFrame.getWorldFrame());
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame)
   {
      super(referenceFrame, new TransformablePoint3d(), null);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, String name)
   {
      super(referenceFrame, new TransformablePoint3d(), name);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(FrameTuple<?, ?> frameTuple)
   {
      super(frameTuple.referenceFrame, new TransformablePoint3d(frameTuple.tuple), frameTuple.name);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(FrameTuple2d<?, ?> frameTuple2d)
   {
      super(frameTuple2d.referenceFrame, new TransformablePoint3d(), frameTuple2d.name);
      setXY(frameTuple2d);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, double x, double y, double z)
   {
      this(referenceFrame, x, y, z, null);
   }

   /** FramePoint <p/> A normal point associated with a specific reference frame. */
   public FramePoint(ReferenceFrame referenceFrame, double x, double y, double z, String name)
   {
      super(referenceFrame, new TransformablePoint3d(x, y, z), name);
   }

   public static FramePoint average(List<? extends FramePoint> framePoints)
   {
      ReferenceFrame frame = framePoints.get(0).getReferenceFrame();
      FramePoint nextPoint = new FramePoint(frame);

      FramePoint average = new FramePoint(framePoints.get(0));
      for (int i = 1; i < framePoints.size(); i++)
      {
         nextPoint.set(framePoints.get(i));
         nextPoint.changeFrame(frame);
         average.add(nextPoint);
      }

      average.scale(1.0 / ((double) framePoints.size()));
      return average;
   }

   public static FramePoint generateRandomFramePoint(Random random, ReferenceFrame frame, double xMaxAbsoluteX, double yMaxAbsoluteY, double zMaxAbsoluteZ)
   {
      FramePoint randomPoint = new FramePoint(frame, RandomTools.generateRandomDouble(random, xMaxAbsoluteX),
            RandomTools.generateRandomDouble(random, yMaxAbsoluteY), RandomTools.generateRandomDouble(random, zMaxAbsoluteZ));
      return randomPoint;
   }

   public static FramePoint generateRandomFramePoint(Random random, ReferenceFrame frame, double xMin, double xMax, double yMin, double yMax, double zMin,
         double zMax)
   {
      FramePoint randomPoint = new FramePoint(frame, RandomTools.generateRandomDouble(random, xMin, xMax), RandomTools.generateRandomDouble(random, yMin, yMax),
            RandomTools.generateRandomDouble(random, zMin, zMax));
      return randomPoint;
   }

   public double getXYPlaneDistance(FramePoint framePoint)
   {
      checkReferenceFrameMatch(framePoint);

      double dx, dy;

      dx = this.getX() - framePoint.getX();
      dy = this.getY() - framePoint.getY();
      return Math.sqrt(dx * dx + dy * dy);
   }

   public double getXYPlaneDistance(FramePoint2d framePoint2d)
   {
      checkReferenceFrameMatch(framePoint2d);

      double dx, dy;

      dx = this.getX() - framePoint2d.getX();
      dy = this.getY() - framePoint2d.getY();
      return Math.sqrt(dx * dx + dy * dy);
   }

   public double distance(Point3d point)
   {
      return this.tuple.distance(point);
   }

   public double distance(FramePoint framePoint)
   {
      checkReferenceFrameMatch(framePoint);

      return this.tuple.distance(framePoint.tuple);
   }

   public double distanceSquared(FramePoint framePoint)
   {
      checkReferenceFrameMatch(framePoint);

      return this.tuple.distanceSquared(framePoint.tuple);
   }

   /**
    * Creates a new FramePoint2d based on the x and y components of this FramePoint
    */
   public FramePoint2d toFramePoint2d()
   {
      return new FramePoint2d(this.getReferenceFrame(), this.getX(), this.getY());
   }

   /**
    * Makes the pointToPack a FramePoint2d version of this FramePoint
    * 
    * @param pointToPack
    */
   public void getFramePoint2d(FramePoint2d pointToPack)
   {
      pointToPack.setIncludingFrame(this.getReferenceFrame(), this.getX(), this.getY());
   }

   public void getPoint2d(Point2d pointToPack)
   {
      pointToPack.setX(this.getX());
      pointToPack.setY(this.getY());
   }

   /**
    * Makes the tupleToPack a FrameTuple2d version of this FramePoint
    * 
    * @param tupleToPack
    */
   public void getFrameTuple2d(FrameTuple2d<?, ?> tupleToPack)
   {
      tupleToPack.setIncludingFrame(this.getReferenceFrame(), this.getX(), this.getY());
   }

   /**
    * Returns the Point3d used in this FramePoint
    *
    * @return Point3d
    */
   public Point3d getPoint()
   {
      return this.tuple;
   }

   public static FramePoint getMidPoint(FramePoint point1, FramePoint point2)
   {
      point1.checkReferenceFrameMatch(point2);
      FramePoint midPoint = new FramePoint(point1.referenceFrame);
      midPoint.interpolate(point1, point2, 0.5);
      return midPoint;
   }

   /**
    * yawAboutPoint
    *
    * @param pointToYawAbout FramePoint
    * @param yaw             double
    * @return CartesianPositionFootstep
    */
   public void yawAboutPoint(FramePoint pointToYawAbout, FramePoint resultToPack, double yaw)
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

   public void pitchAboutPoint(FramePoint pointToPitchAbout, FramePoint resultToPack, double pitch)
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
   public void getPoint(Point3d pointToPack)
   {
      this.get(pointToPack);
   }

   @Override
   public void setPoint(PointInterface pointInterface)
   {
      pointInterface.getPoint(this.getPoint());
   }

   @Override
   public void setPoint(Point3d point)
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

   public double getDistanceFromOrigin()
   {
      return this.tuple.getLength();
   }
}
