package us.ihmc.robotics.geometry;

import java.io.Serializable;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.euclid.interfaces.GeometryObject;
import us.ihmc.euclid.referenceFrame.FrameGeometryObject;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.exceptions.ReferenceFrameMismatchException;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;

/**
 * One of the main goals of this class is to check, at runtime, that operations on tuples occur within the same Frame.
 * This method checks for one Vector argument.
 *
 * @author Learning Locomotion Team
 * @version 2.0
 */
public abstract class FrameTuple3D<S extends FrameTuple3D<S, T>, T extends Tuple3DBasics & GeometryObject<T>> extends FrameGeometryObject<S, T> implements Serializable
{
   private static final long serialVersionUID = 3894861900288076730L;

   protected final T tuple;

   public FrameTuple3D(ReferenceFrame referenceFrame, T tuple)
   {
      super(referenceFrame, tuple);
      this.tuple = getGeometryObject();
   }

   public final void set(double x, double y, double z)
   {
      tuple.set(x, y, z);
   }

   public final void setIncludingFrame(ReferenceFrame referenceFrame, double x, double y, double z)
   {
      this.referenceFrame = referenceFrame;
      set(x, y, z);
   }

   /**
    * Sets this tuple's components {@code x}, {@code y}, {@code z} in order from the given array
    * {@code tupleArray} and sets this tuple frame to {@code referenceFrame}.
    *
    * @param referenceFrame the new reference frame for this tuple.
    * @param tupleArray the array containing the new values for this tuple's components. Not
    *           modified.
    */
   public final void setIncludingFrame(ReferenceFrame referenceFrame, double[] tupleArray)
   {
      this.referenceFrame = referenceFrame;
      tuple.set(tupleArray);
   }

   /**
    * Sets this tuple's components {@code x}, {@code y}, {@code z} in order from the given array
    * {@code tupleArray} and sets this tuple frame to {@code referenceFrame}.
    *
    * @param referenceFrame the new reference frame for this tuple.
    * @param startIndex the first index to start reading from in the array.
    * @param tupleArray the array containing the new values for this tuple's components. Not
    *           modified.
    */
   public final void setIncludingFrame(ReferenceFrame referenceFrame, int startIndex, double[] tupleArray)
   {
      this.referenceFrame = referenceFrame;
      tuple.set(startIndex, tupleArray);
   }

   /**
    * Sets this tuple's components {@code x}, {@code y}, {@code z} in order from the given column
    * vector starting to read from its first row index and sets this tuple frame to
    * {@code referenceFrame}.
    *
    * @param referenceFrame the new reference frame for this tuple.
    * @param matrix the column vector containing the new values for this tuple's components. Not
    *           modified.
    */
   public final void setIncludingFrame(ReferenceFrame referenceFrame, DenseMatrix64F tupleDenseMatrix)
   {
      this.referenceFrame = referenceFrame;
      tuple.set(tupleDenseMatrix);
   }

   /**
    * Sets this tuple's components {@code x}, {@code y}, {@code z} in order from the given column
    * vector starting to read from {@code startRow} and sets this tuple frame to
    * {@code referenceFrame}.
    *
    * @param referenceFrame the new reference frame for this tuple.
    * @param startRow the first row index to start reading in the dense-matrix.
    * @param matrix the column vector containing the new values for this tuple's components. Not
    *           modified.
    */
   public final void setIncludingFrame(ReferenceFrame referenceFrame, int startRow, DenseMatrix64F tupleDenseMatrix)
   {
      this.referenceFrame = referenceFrame;
      tuple.set(startRow, tupleDenseMatrix);
   }

   /**
    * Sets this tuple's components {@code x}, {@code y}, {@code z} in order from the given matrix
    * starting to read from {@code startRow} at the column index {@code column} and sets this tuple
    * frame to {@code referenceFrame}.
    *
    * @param referenceFrame the new reference frame for this tuple.
    * @param startRow the first row index to start reading in the dense-matrix.
    * @param column the column index to read in the dense-matrix.
    * @param matrix the column vector containing the new values for this tuple's components. Not
    *           modified.
    */
   public final void setIncludingFrame(ReferenceFrame referenceFrame, int startRow, int column, DenseMatrix64F tupleDenseMatrix)
   {
      this.referenceFrame = referenceFrame;
      tuple.set(startRow, column, tupleDenseMatrix);
   }

   public final void set(Tuple3DReadOnly tuple)
   {
      this.tuple.set(tuple);
   }

   public final void set(FrameTuple3D<?, ?> frameTuple)
   {
      checkReferenceFrameMatch(frameTuple);
      set(frameTuple.tuple);
   }

   public final void setIncludingFrame(ReferenceFrame referenceFrame, Tuple3DReadOnly tuple)
   {
      this.referenceFrame = referenceFrame;
      set(tuple);
   }

   /**
    * Set the x and y components of this frameTuple to tuple2d.x and tuple2d.y respectively, and sets the z component to zero.
    * @param tuple2d
    */
   public final void setXYIncludingFrame(ReferenceFrame referenceFrame, Tuple2DReadOnly tuple)
   {
      this.referenceFrame = referenceFrame;
      setXY(tuple);
   }

   /**
    * Set the x and y components of this frameTuple to frameTuple2d.x and frameTuple2d.y respectively, and sets the z component to zero.
    * Changes the referenceFrame of this frameTuple to frameTuple2d.getReferenceFrame().
    * @param frameTuple2d
    */
   public void setXYIncludingFrame(FrameTuple2d<?, ?> frameTuple2d)
   {
      this.referenceFrame = frameTuple2d.getReferenceFrame();
      setXY(frameTuple2d);
   }

   public final void setIncludingFrame(FrameTuple3D<?, ?> frameTuple)
   {
      setIncludingFrame(frameTuple.referenceFrame, frameTuple.tuple);
   }

   public final void setX(double x)
   {
      tuple.setX(x);
   }

   public final void setY(double y)
   {
      tuple.setY(y);
   }

   public final void setZ(double z)
   {
      tuple.setZ(z);
   }

   /**
    * Selects a component of this tuple based on {@code index} and sets it to {@code value}.
    * <p>
    * For an {@code index} of 0, the corresponding component is {@code x}, 1 it is {@code y}, 2 it
    * is {@code z}.
    * </p>
    *
    * @param index the index of the component to set.
    * @param value the new value of the selected component.
    * @throws IndexOutOfBoundsException if {@code index} &notin; [0, 2].
    */
   public void setElement(int index, double value)
   {
      tuple.setElement(index, value);
   }

   /**
    * Set the x and y components of this frameTuple to tuple2d.x and tuple2d.y respectively, and sets the z component to zero.
    * @param tuple2d
    */
   public void setXY(Tuple2DReadOnly tuple2d)
   {
      this.tuple.setX(tuple2d.getX());
      this.tuple.setY(tuple2d.getY());
      this.tuple.setZ(0.0);
   }

   /**
    * Set the x and y components of this frameTuple to frameTuple2d.x and frameTuple2d.y respectively, and sets the z component to zero.
    * @param frameTuple2d
    * @throws ReferenceFrameMismatchException
    */
   public void setXY(FrameTuple2d<?, ?> frameTuple2d)
   {
      checkReferenceFrameMatch(frameTuple2d);
      setXY(frameTuple2d.tuple);
   }

   public final double get(Direction direction)
   {
      return Direction.get(tuple, direction);
   }

   /**
    * Selects a component of this tuple based on {@code index} and returns its value.
    * <p>
    * For an {@code index} of 0, the corresponding component is {@code x}, 1 it is {@code y}, 2 it
    * is {@code z}.
    * </p>
    *
    * @param index the index of the component to get.
    * @return the value of the component.
    * @throws IndexOutOfBoundsException if {@code index} &notin; [0, 2].
    */
   public double getElement(int index)
   {
      return tuple.getElement(index);
   }

   public final void scale(double scaleFactor)
   {
      tuple.scale(scaleFactor);
   }

   public final void scale(double scaleXFactor, double scaleYFactor, double scaleZFactor)
   {
      tuple.scale(scaleXFactor, scaleYFactor, scaleZFactor);
   }

   public final double getX()
   {
      return tuple.getX();
   }

   public final double getY()
   {
      return tuple.getY();
   }

   public final double getZ()
   {
      return tuple.getZ();
   }

   /**
    * Returns a Point3D copy of the tuple in this FrameTuple.
    *
    * @return Point3D
    */
   public final Point3D getPointCopy()
   {
      return new Point3D(tuple);
   }

   /**
    * Returns a Vector3d copy of the tuple in this FrameTuple.
    *
    * @return Vector3d
    */
   public final Vector3D getVectorCopy()
   {
      return new Vector3D(tuple);
   }

   @Override
   public final void get(Tuple3DBasics tuple3dToPack)
   {
      tuple3dToPack.set(tuple);
   }

   public final void checkForNaN()
   {
      if (containsNaN())
         throw new RuntimeException(getClass().getSimpleName() + " " + this + " has a NaN!");
   }

   public final boolean containsInfinity()
   {
      return Double.isInfinite(tuple.getX()) || Double.isInfinite(tuple.getY()) || Double.isInfinite(tuple.getZ());
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of tuple1 (this = scaleFactor * tuple1).
    *
    * @param scaleFactor double
    * @param tuple1 Tuple3d
    */
   public final void setAndScale(double scaleFactor, Tuple3DReadOnly tuple1)
   {
      tuple.setAndScale(scaleFactor, tuple1);
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of tuple1 and then adds tuple2 (this = scaleFactor * tuple1 + tuple2).
    *
    * @param scaleFactor double
    * @param tuple1 Tuple3d
    * @param tuple2 Tuple3d
    */
   public final void scaleAdd(double scaleFactor, Tuple3DReadOnly tuple1, Tuple3DReadOnly tuple2)
   {
      tuple.scaleAdd(scaleFactor, tuple1, tuple2);
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of itself and then adds tuple1 (this = scaleFactor * this + tuple1).
    * Checks if reference frames match.
    *
    * @param scaleFactor double
    * @param tuple1 Tuple3d
    */
   public final void scaleAdd(double scaleFactor, Tuple3DReadOnly tuple1)
   {
      tuple.scaleAdd(scaleFactor, tuple1);
   }

   /**
    * Sets the value of this frameTuple to the scalar multiplication of frameTuple1 (this = scaleFactor * frameTuple1).
    * Checks if reference frames match.
    *
    * @param scaleFactor double
    * @param frameTuple1 FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void setAndScale(double scaleFactor, FrameTuple3D<?, ?> frameTuple1)
   {
      checkReferenceFrameMatch(frameTuple1);
      setAndScale(scaleFactor, frameTuple1.tuple);
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of itself and then adds frameTuple1 (this = scaleFactor * this + frameTuple1).
    * Checks if reference frames match.
    *
    * @param scaleFactor double
    * @param frameTuple1 FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void scaleAdd(double scaleFactor, FrameTuple3D<?, ?> frameTuple1)
   {
      checkReferenceFrameMatch(frameTuple1);
      scaleAdd(scaleFactor, frameTuple1.tuple);
   }

   /**
    * Sets the value of this frameTuple to the scalar multiplication of frameTuple1 and then adds frameTuple2 (this = scaleFactor * frameTuple1 + frameTuple2).
    * Checks if reference frames match.
    *
    * @param scaleFactor double
    * @param frameTuple1 FrameTuple<?, ?>
    * @param frameTuple2 FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void scaleAdd(double scaleFactor, FrameTuple3D<?, ?> frameTuple1, FrameTuple3D<?, ?> frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);
      scaleAdd(scaleFactor, frameTuple1.tuple, frameTuple2.tuple);
   }

   /**
    * Sets the value of this frameTuple to the scalar multiplication of frameTuple1 and then subs frameTuple2 (this = scaleFactor * frameTuple1 - frameTuple2).
    * Checks if reference frames match.
    *
    * @param scaleFactor double
    * @param frameTuple1 FrameTuple<?, ?>
    * @param frameTuple2 FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void scaleSub(double scaleFactor, FrameTuple3D<?, ?> frameTuple1, FrameTuple3D<?, ?> frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);

      tuple.scaleSub(scaleFactor, frameTuple1.tuple, frameTuple2.tuple);
   }

   /**  
    * Sets the value of this tuple to the sum of itself and tuple1.
    * @param tuple1 the other Tuple3d
    */
   public final void add(Tuple3DReadOnly tuple1)
   {
      tuple.add(tuple1);
   }

   /**  
    * Sets the value of this tuple to the sum of itself and tuple1.
    * @param tuple1 the other Tuple3d
    */
   public final void add(double x, double y, double z)
   {
      tuple.add(x, y, z);
   }

   /**
    * Sets the value of this tuple to the sum of tuple1 and tuple2 (this = tuple1 + tuple2).
    * @param tuple1 the first Tuple3d
    * @param tuple2 the second Tuple3d
    */
   public final void add(Tuple3DReadOnly tuple1, Tuple3DReadOnly tuple2)
   {
      tuple.add(tuple1, tuple2);
   }

   /**  
    * Sets the value of this frameTuple to the sum of itself and frameTuple1 (this += frameTuple1).
    * Checks if reference frames match.
    * @param frameTuple1 the other Tuple3d
    * @throws ReferenceFrameMismatchException
    */
   public final void add(FrameTuple3D<?, ?> frameTuple1)
   {
      checkReferenceFrameMatch(frameTuple1);
      add(frameTuple1.tuple);
   }

   /**
    * Sets the value of this frameTuple to the sum of frameTuple1 and frameTuple2 (this = frameTuple1 + frameTuple2).
    * @param frameTuple1 the first FrameTuple<?, ?>
    * @param frameTuple2 the second FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void add(FrameTuple3D<?, ?> frameTuple1, FrameTuple3D<?, ?> frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);
      add(frameTuple1.tuple, frameTuple2.tuple);
   }

   /**  
    * Sets the value of this tuple to the difference of itself and tuple1 (this -= tuple1).
    * @param tuple1 the other Tuple3d
    */
   public final void sub(double x, double y, double z)
   {
      tuple.sub(x, y, z);
   }

   /**  
    * Sets the value of this tuple to the difference of itself and tuple1 (this -= tuple1).
    * @param tuple1 the other Tuple3d
    */
   public final void sub(Tuple3DReadOnly tuple1)
   {
      tuple.sub(tuple1);
   }

   /**  
    * Sets the value of this tuple to the difference of tuple1 and tuple2 (this = tuple1 - tuple2).
    * @param tuple1 the first Tuple3d
    * @param tuple2 the second Tuple3d
    */
   public final void sub(Tuple3DReadOnly tuple1, Tuple3DReadOnly tuple2)
   {
      tuple.sub(tuple1, tuple2);
   }

   /**  
    * Sets the value of this frameTuple to the difference of itself and frameTuple1 (this -= frameTuple1).
    * @param frameTuple1 the first FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void sub(FrameTuple3D<?, ?> frameTuple1)
   {
      checkReferenceFrameMatch(frameTuple1);
      sub(frameTuple1.tuple);
   }

   /**  
    * Sets the value of this frameTuple to the difference of frameTuple1 and frameTuple2 (this = frameTuple1 - frameTuple2).
    * @param frameTuple1 the first FrameTuple<?, ?>
    * @param frameTuple2 the second FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void sub(FrameTuple3D<?, ?> frameTuple1, FrameTuple3D<?, ?> frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);
      sub(frameTuple1.tuple, frameTuple2.tuple);
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of tuple1 minus tuple2 (this = scaleFactor * ( tuple1 - tuple2 ) ).
    *
    * @param scaleFactor double
    * @param tuple1 Tuple3d
    * @param tuple2 Tuple3d
    */
   public final void subAndScale(double scaleFactor, Tuple3DReadOnly tuple1, Tuple3DReadOnly tuple2)
   {
      sub(tuple1, tuple2);
      scale(scaleFactor);
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of frameTuple1 minus frameTuple2 (this = scaleFactor * ( frameTuple1 - frameTuple2 ) ).
    *
    * @param scaleFactor double
    * @param frameTuple1 the first FrameTuple<?, ?>
    * @param frameTuple2 the second FrameTuple<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void subAndScale(double scaleFactor, FrameTuple3D<?, ?> frameTuple1, FrameTuple3D<?, ?> frameTuple2)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);
      subAndScale(scaleFactor, frameTuple1.tuple, frameTuple2.tuple);
   }

   /**
     *  Linearly interpolates between tuples tuple1 and tuple2 and places the result into this tuple:  this = (1-alpha) * tuple1 + alpha * tuple2.
     *  @param t1  the first tuple
     *  @param t2  the second tuple  
     *  @param alpha  the alpha interpolation parameter  
    */
   public final void interpolate(Tuple3DReadOnly tuple1, Tuple3DReadOnly tuple2, double alpha)
   {
      tuple.interpolate(tuple1, tuple2, alpha);
   }

   /**
     *  Linearly interpolates between tuples tuple1 and tuple2 and places the result into this tuple:  this = (1-alpha) * tuple1 + alpha * tuple2.
     *  @param t1  the first tuple
     *  @param t2  the second tuple  
     *  @param alpha  the alpha interpolation parameter
    * @throws ReferenceFrameMismatchException
    */
   public final void interpolate(FrameTuple3D<?, ?> frameTuple1, FrameTuple3D<?, ?> frameTuple2, double alpha)
   {
      frameTuple1.checkReferenceFrameMatch(frameTuple2);

      interpolate(frameTuple1.tuple, frameTuple2.tuple, alpha);
      referenceFrame = frameTuple1.getReferenceFrame();
   }

   /**
    * Packs the components {@code x}, {@code y}, {@code z} in order in a column vector starting from
    * its first row index.
    *
    * @param tupleMatrixToPack the array in which this tuple is frame stored. Modified.
    */
   public final void get(DenseMatrix64F tupleMatrixToPack)
   {
      tuple.get(tupleMatrixToPack);
   }

   /**
    * Packs the components {@code x}, {@code y}, {@code z} in order in a column vector starting from
    * {@code startRow}.
    *
    * @param startRow the first row index to start writing in the dense-matrix.
    * @param tupleMatrixToPack the column vector in which this frame tuple is stored. Modified.
    */
   public final void get(int startRow, DenseMatrix64F tupleMatrixToPack)
   {
      tuple.get(startRow, tupleMatrixToPack);
   }

   /**
    * Packs the components {@code x}, {@code y}, {@code z} in order in a column vector starting from
    * {@code startRow} at the column index {@code column}.
    *
    * @param startRow the first row index to start writing in the dense-matrix.
    * @param column the column index to write in the dense-matrix.
    * @param tupleMatrixToPack the matrix in which this frame tuple is stored. Modified.
    */
   public final void get(int startRow, int column, DenseMatrix64F tupleMatrixToPack)
   {
      tuple.get(startRow, column, tupleMatrixToPack);
   }

   public final void clipToMinMax(double minValue, double maxValue)
   {
      this.tuple.clipToMinMax(minValue, maxValue);
   }

   public final void negate()
   {
      tuple.negate();
   }

   public final void absolute()
   {
      tuple.absolute();
   }

   /**
     * Returns true if the L-infinite distance between this tuple and tuple1 is less than or equal to the epsilon parameter, otherwise returns false.
     * The L-infinite distance is equal to MAX[abs(x1-x2), abs(y1-y2), abs(z1-z2)].
    * @param tuple1 Tuple3d
    * @param threshold double
    */
   public final boolean epsilonEquals(Tuple3DReadOnly tuple1, double threshold)
   {
      if (tuple1 == null)
      {
         return false;
      }

      return tuple.epsilonEquals(tuple1, threshold);
   }

   /**
     * Returns true if the L-infinite distance between this frameTuple and frameTuple1 is less than or equal to the epsilon parameter, otherwise returns false.
     * The L-infinite distance is equal to MAX[abs(x1-x2), abs(y1-y2), abs(z1-z2)].
    * @param frameTuple1 FrameTuple<?, ?>
    * @param threshold double
    * @throws ReferenceFrameMismatchException
    */
   public final boolean epsilonEquals(FrameTuple3D<?, ?> frameTuple1, double threshold)
   {
      if (frameTuple1 == null)
      {
         return false;
      }

      checkReferenceFrameMatch(frameTuple1);

      return epsilonEquals(frameTuple1.tuple, threshold);
   }
}
