package us.ihmc.robotics.math.frames;

import us.ihmc.euclid.referenceFrame.FrameTuple2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.exceptions.ReferenceFrameMismatchException;
import us.ihmc.euclid.referenceFrame.interfaces.FrameTuple2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameTuple3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.ReferenceFrameHolder;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DReadOnly;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

//Note: You should only make these once at the initialization of a controller. You shouldn't make any on the fly since they contain YoVariables.
public abstract class YoFrameTuple2d<S, T extends FrameTuple2D<?, ?>> implements ReferenceFrameHolder, FrameTuple2DReadOnly
{
   private final YoDouble x, y; // This is where the data is stored. All operations must act on these numbers.
   private final T frameTuple2d; // This is only for assistance. The data is stored in the YoVariables, not in here!
   private final ReferenceFrame referenceFrame; // Redundant but allows to make sure the frame isn't changed


   public YoFrameTuple2d(YoDouble xVariable, YoDouble yVariable, ReferenceFrame referenceFrame)
   {
      this.x = xVariable;
      this.y = yVariable;
      this.referenceFrame = referenceFrame;
      this.frameTuple2d = createEmptyFrameTuple2d();
      putYoValuesIntoFrameTuple2d();
   }

   public YoFrameTuple2d(String namePrefix, String nameSuffix, ReferenceFrame referenceFrame, YoVariableRegistry registry)
   {
      this(namePrefix, nameSuffix, "", referenceFrame, registry);
   }

   public YoFrameTuple2d(String namePrefix, String nameSuffix, String description, ReferenceFrame referenceFrame, YoVariableRegistry registry)
   {
      x = new YoDouble(YoFrameVariableNameTools.createXName(namePrefix, nameSuffix), description, registry);
      y = new YoDouble(YoFrameVariableNameTools.createYName(namePrefix, nameSuffix), description, registry);
      this.referenceFrame = referenceFrame;
      this.frameTuple2d = createEmptyFrameTuple2d();
      putYoValuesIntoFrameTuple2d();
   }

   public final T getFrameTuple2d()
   {
      putYoValuesIntoFrameTuple2d();
      return frameTuple2d;
   }

   public final double getX()
   {
      return x.getDoubleValue();
   }

   public final double getY()
   {
      return y.getDoubleValue();
   }

   public final YoDouble getYoX()
   {
      return x;
   }

   public final YoDouble getYoY()
   {
      return y;
   }

   public final void setX(double newX)
   {
      x.set(newX);
   }

   public final void setY(double newY)
   {
      y.set(newY);
   }

   public final void set(double newX, double newY)
   {
      x.set(newX);
      y.set(newY);
   }
   
   public final void setAndMatchFrame(FrameTuple2DReadOnly frameTuple2d)
   {
      setAndMatchFrame(frameTuple2d, true);
   }

   public final void setAndMatchFrame(FrameTuple2DReadOnly frameTuple2d, boolean notifyListeners)
   {
      this.frameTuple2d.setIncludingFrame(frameTuple2d);
      this.frameTuple2d.changeFrame(getReferenceFrame());
      getYoValuesFromFrameTuple2d(notifyListeners);
   }

   public final void set(ReferenceFrame referenceFrame, double x, double y)
   {
      checkReferenceFrameMatch(referenceFrame);
      set(x, y);
   }

   public final void set(FrameTuple2DReadOnly frameTuple2d)
   {
      set(frameTuple2d, true);
   }

   public final void set(FrameTuple2DReadOnly frameTuple2d, boolean notifyListeners)
   {
      this.frameTuple2d.set(frameTuple2d);
      getYoValuesFromFrameTuple2d(notifyListeners);
   }

   public final void set(Tuple2DReadOnly tuple2d)
   {
      this.frameTuple2d.set(tuple2d);
      getYoValuesFromFrameTuple2d();
   }

   public final void setByProjectionOntoXYPlane(FrameTuple3DReadOnly frameTuple)
   {
      setByProjectionOntoXYPlane(frameTuple, true);
   }

   public final void setByProjectionOntoXYPlane(FrameTuple3DReadOnly frameTuple, boolean notifyListeners)
   {
      this.frameTuple2d.set(frameTuple);
      getYoValuesFromFrameTuple2d(notifyListeners);
   }

   public final void add(double dx, double dy)
   {
      x.set(x.getDoubleValue() + dx);
      y.set(y.getDoubleValue() + dy);
   }

   public final void add(Tuple2DReadOnly tuple2d)
   {
      putYoValuesIntoFrameTuple2d();
      this.frameTuple2d.add(tuple2d);
      getYoValuesFromFrameTuple2d();
   }

   public final void add(FrameTuple2DReadOnly frameTuple2d)
   {
      putYoValuesIntoFrameTuple2d();
      this.frameTuple2d.add(frameTuple2d);
      getYoValuesFromFrameTuple2d();
   }

   public final void sub(Tuple2DReadOnly tuple2d)
   {
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.sub(tuple2d);
      getYoValuesFromFrameTuple2d();
   }

   public final void sub(FrameTuple2DReadOnly frameTuple2d)
   {
      putYoValuesIntoFrameTuple2d();
      this.frameTuple2d.sub(frameTuple2d);
      getYoValuesFromFrameTuple2d();
   }

   public final void sub(FrameTuple2DReadOnly frameTuple1, FrameTuple2DReadOnly frameTuple2)
   {
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.sub(frameTuple1, frameTuple2);
      getYoValuesFromFrameTuple2d();
   }

   public final void scale(double scaleFactor)
   {
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.scale(scaleFactor);
      getYoValuesFromFrameTuple2d();
   }

   /**
    * Sets the value of this tuple to the scalar multiplication of itself and then adds frameTuple1 (this = scaleFactor * this + frameTuple1).
    * Checks if reference frames match.
    *
    * @param scaleFactor double
    * @param frameTuple1 FrameTuple2d<?, ?>
    * @throws ReferenceFrameMismatchException
    */
   public final void scaleAdd(double scaleFactor, FrameTuple2DReadOnly frameTuple2d)
   {
      putYoValuesIntoFrameTuple2d();
      this.frameTuple2d.scaleAdd(scaleFactor, frameTuple2d);
      getYoValuesFromFrameTuple2d();
   }

   public final void scaleAdd(double scaleFactor, FrameTuple2DReadOnly frameTuple1, FrameTuple2DReadOnly frameTuple2)
   {
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.scaleAdd(scaleFactor, frameTuple1, frameTuple2);
      getYoValuesFromFrameTuple2d();
   }

   public final void interpolate(Tuple2DReadOnly tuple1, Tuple2DReadOnly tuple2, double alpha)
   {
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.interpolate(tuple1, tuple2, alpha);
      getYoValuesFromFrameTuple2d();
   }

   /**
    *  Linearly interpolates between tuples frameTuple1 and frameTuple2 and places the result into this tuple:  this = (1-alpha) * frameTuple1 + alpha * frameTuple2.
    *  @param frameTuple1  the first tuple
    *  @param frameTuple2  the second tuple  
    *  @param alpha  the alpha interpolation parameter
    * @throws ReferenceFrameMismatchException
    */
   public final void interpolate(FrameTuple2DReadOnly frameTuple1, FrameTuple2DReadOnly frameTuple2, double alpha)
   {
      checkReferenceFrameMatch(frameTuple1);
      checkReferenceFrameMatch(frameTuple2);
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.interpolate(frameTuple1, frameTuple2, alpha);
      getYoValuesFromFrameTuple2d();
   }

   public final void setToZero()
   {
      frameTuple2d.setToZero(referenceFrame);
      getYoValuesFromFrameTuple2d();
   }

   public final void setToNaN()
   {
      frameTuple2d.setToNaN(referenceFrame);
      getYoValuesFromFrameTuple2d();
   }

   public final void applyTransform(Transform transform)
   {
      putYoValuesIntoFrameTuple2d();
      frameTuple2d.applyTransform(transform);
      getYoValuesFromFrameTuple2d();
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   public final void attachVariableChangedListener(VariableChangedListener variableChangedListener)
   {
      x.addVariableChangedListener(variableChangedListener);
      y.addVariableChangedListener(variableChangedListener);
   }

   protected abstract T createEmptyFrameTuple2d();

   private final void putYoValuesIntoFrameTuple2d()
   {
      frameTuple2d.setIncludingFrame(referenceFrame, x.getDoubleValue(), y.getDoubleValue());
   }

   protected void getYoValuesFromFrameTuple2d()
   {
      getYoValuesFromFrameTuple2d(true);
   }

   private void getYoValuesFromFrameTuple2d(boolean notifyListeners)
   {
      x.set(frameTuple2d.getX(), notifyListeners);
      y.set(frameTuple2d.getY(), notifyListeners);
   }

   /**
    * toString
    *
    * String representation of a FrameVector (x,y)-reference frame name
    *
    * @return String
    */
   @Override
   public String toString()
   {
      putYoValuesIntoFrameTuple2d();
      return frameTuple2d.toString();
   }
}
