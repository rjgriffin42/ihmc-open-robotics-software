package us.ihmc.robotics.geometry;

import java.util.Random;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.referenceFrame.FrameTuple3DReadOnly;
import us.ihmc.euclid.referenceFrame.FrameVector3DReadOnly;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.robotics.random.RandomGeometry;

/**
 * One of the main goals of this class is to check, at runtime, that operations on vectors occur
 * within the same Frame. This method checks for one Vector argument.
 *
 * @author Learning Locomotion Team
 * @version 2.0
 */
public class FrameVector extends FrameTuple3D<FrameVector, Vector3D> implements FrameVector3DReadOnly, Vector3DBasics
{
   private static final long serialVersionUID = -4475317718392284548L;

   /**
    * FrameVector
    * <p/>
    * A normal vector associated with a specific reference frame.
    */
   public FrameVector()
   {
      this(ReferenceFrame.getWorldFrame());
   }

   /**
    * FrameVector
    * <p/>
    * A normal vector associated with a specific reference frame.
    */
   public FrameVector(ReferenceFrame referenceFrame)
   {
      super(referenceFrame, new Vector3D());
   }

   /**
    * FrameVector
    * <p/>
    * A normal vector associated with a specific reference frame.
    */
   public FrameVector(ReferenceFrame referenceFrame, double x, double y, double z)
   {
      super(referenceFrame, new Vector3D(x, y, z));
   }

   /**
    * FrameVector
    * <p/>
    * A normal vector associated with a specific reference frame.
    */
   public FrameVector(ReferenceFrame referenceFrame, double[] vector)
   {
      super(referenceFrame, new Vector3D(vector));
   }

   /**
    * FrameVector
    * <p/>
    * A normal vector associated with a specific reference frame.
    */
   public FrameVector(ReferenceFrame referenceFrame, Tuple3DReadOnly tuple)
   {
      super(referenceFrame, new Vector3D(tuple));
   }

   /**
    * FrameVector
    * <p/>
    * A normal vector associated with a specific reference frame.
    */
   public FrameVector(FrameTuple3DReadOnly other)
   {
      super(other.getReferenceFrame(), new Vector3D(other));
   }

   public static FrameVector generateRandomFrameVector(Random random, ReferenceFrame frame)
   {
      FrameVector randomVector = new FrameVector(frame, RandomGeometry.nextVector3D(random));
      return randomVector;
   }

   public static FrameVector generateRandomFrameVector(Random random, ReferenceFrame frame, double xMin, double xMax, double yMin, double yMax, double zMin,
                                                       double zMax)
   {
      FrameVector randomVector = new FrameVector(frame, RandomNumbers.nextDouble(random, xMin, xMax), RandomNumbers.nextDouble(random, yMin, yMax),
                                                 RandomNumbers.nextDouble(random, zMin, zMax));
      return randomVector;
   }

   public void setAndNormalize(FrameTuple3DReadOnly other)
   {
      checkReferenceFrameMatch(other);
      tuple.setAndNormalize(other);
   }

   /**
    * Retrieves the vector inside this FrameVector
    *
    * @return Vector3d
    */
   public Vector3D getVector()
   {
      return this.tuple;
   }

   /**
    * Creates a new FrameVector2d based on the x and y components of this FrameVector
    */
   public FrameVector2d toFrameVector2d()
   {
      return new FrameVector2d(this.getReferenceFrame(), this.getX(), this.getY());
   }

   public boolean isEpsilonParallel(FrameVector frameVector, double epsilonAngle)
   {
      checkReferenceFrameMatch(frameVector);

      double angleMinusZeroToPi = Math.abs(AngleTools.trimAngleMinusPiToPi(this.angle(frameVector)));

      double errorFromParallel = Math.min(angleMinusZeroToPi, Math.PI - angleMinusZeroToPi);
      return errorFromParallel < epsilonAngle;
   }

   public boolean isEpsilonParallel(FrameVector frameVector)
   {
      return isEpsilonParallel(frameVector, 1e-7);
   }

   public void cross(FrameVector3DReadOnly frameTuple1)
   {
      checkReferenceFrameMatch(frameTuple1);
      tuple.cross(tuple, frameTuple1);
   }

   public void cross(FrameTuple3DReadOnly frameTuple1)
   {
      checkReferenceFrameMatch(frameTuple1);
      tuple.cross(tuple, frameTuple1);
   }

   public void cross(FrameVector3DReadOnly frameTuple1, FrameVector3DReadOnly frameTuple2)
   {
      cross((FrameTuple3DReadOnly) frameTuple1, (FrameTuple3DReadOnly) frameTuple2);
   }

   public void cross(FrameTuple3DReadOnly frameTuple1, FrameTuple3DReadOnly frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);
      tuple.cross(frameTuple1, frameTuple2);
   }

   public void cross(FrameTuple3DReadOnly frameTuple1, Tuple3DReadOnly frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      tuple.cross(frameTuple1, frameTuple2);
   }

   public void cross(Tuple3DReadOnly frameTuple1, FrameTuple3DReadOnly frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple2);
      tuple.cross(frameTuple1, frameTuple2);
   }
}
