package us.ihmc.robotics.math.interpolators;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;

import us.ihmc.commons.MathTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;
import us.ihmc.robotics.linearAlgebra.MatrixTools;

/**
 * Quintic spline interpolator This class calculates a spline through multiple waypoints, minimizing
 * the jerk and a finite 5th derivative of position. 
 *  
 * @author Jesper Smith
 */
public class QuinticSplineInterpolator
{
   private final int maximumNumberOfPoints;
   private final int numberOfSplines;

   private final YoVariableRegistry registry;
   private final YoBoolean initialized;

   // Matrices used internally to calculate constants
   private DenseMatrix64F h;
   private DenseMatrix64F D;
   private DenseMatrix64F C;
   private DenseMatrix64F Cblock;
   private DenseMatrix64F A;
   private final LinearSolver<DenseMatrix64F> solver;

   private DenseMatrix64F sol;
   private DenseMatrix64F s;
   private DenseMatrix64F yd;

   private DenseMatrix64F a;
   private DenseMatrix64F b;
   private DenseMatrix64F c;
   private DenseMatrix64F d;
   private DenseMatrix64F e;
   private DenseMatrix64F f;

   // Constants
   private final YoDouble[] x;
   private final YoInteger numberOfPoints;
   private final QuinticSpline[] splines;

   // Result

   private final YoDouble[] position;
   private final YoDouble[] velocity;
   private final YoDouble[] acceleration;
   private final YoDouble[] jerk;

   /**
    * Create a new QuinticSplineInterpolator
    * 
    * @param name                  Name of the YoVariableRegistry
    * @param maximumNumberOfPoints Maximum number of points the spline can interpolate (>= 2)
    * @param numberOfSplines       Number of splines it creates that have the same x coordinates for
    *                              the points to interpolate. This allows re-use of memory and saves some computational time.
    */
   public QuinticSplineInterpolator(String name, int maximumNumberOfPoints, int numberOfSplines, YoVariableRegistry parentRegistry)
   {
      this.maximumNumberOfPoints = maximumNumberOfPoints;
      this.numberOfSplines = numberOfSplines;

      registry = new YoVariableRegistry(name);
      initialized = new YoBoolean(name + "_initialized", registry);
      initialized.set(false);
      numberOfPoints = new YoInteger(name + "_numberOfPoints", registry);

      h = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);
      D = new DenseMatrix64F(maximumNumberOfPoints - 1, maximumNumberOfPoints + 2);
      C = new DenseMatrix64F(maximumNumberOfPoints, maximumNumberOfPoints + 2);
      Cblock = new DenseMatrix64F(maximumNumberOfPoints - 1, maximumNumberOfPoints + 2);
      A = new DenseMatrix64F(maximumNumberOfPoints + 2, maximumNumberOfPoints + 2);
      solver = LinearSolverFactory.linear(maximumNumberOfPoints + 2);

      s = new DenseMatrix64F(maximumNumberOfPoints + 2, 1);
      sol = new DenseMatrix64F(maximumNumberOfPoints + 2, 1);
      yd = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);

      a = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);
      b = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);
      c = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);
      d = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);
      e = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);
      f = new DenseMatrix64F(maximumNumberOfPoints - 1, 1);

      x = new YoDouble[maximumNumberOfPoints];
      for (int i = 0; i < maximumNumberOfPoints; i++)
      {
         x[i] = new YoDouble("x[" + i + "]", registry);
      }

      position = new YoDouble[numberOfSplines];
      velocity = new YoDouble[numberOfSplines];
      acceleration = new YoDouble[numberOfSplines];
      jerk = new YoDouble[numberOfSplines];

      splines = new QuinticSpline[numberOfSplines];
      for (int i = 0; i < numberOfSplines; i++)
      {
         String eName = name + "[" + i + "]";
         splines[i] = new QuinticSpline(eName, maximumNumberOfPoints, registry);

         position[i] = new YoDouble(eName + "_position", registry);
         velocity[i] = new YoDouble(eName + "_velocity", registry);
         acceleration[i] = new YoDouble(eName + "_acceleration", registry);
         jerk[i] = new YoDouble(eName + "_jerk", registry);
      }

      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }

   }

   /**
    * Initializes the spline.
    * 
    * @param xIn Array (length = pointsToInterpolate) with the x coordinates of the points to
    *            interpolate
    */
   public void initialize(double[] xIn)
   {
      if (xIn.length > maximumNumberOfPoints)
         throw new RuntimeException("xIn exceeds the maximum number of points");

      numberOfPoints.set(xIn.length);

      for (int i = 0; i < numberOfPoints.getValue(); i++)
      {
         x[i].set(xIn[i]);
      }

      h.reshape(numberOfPoints.getValue() - 1, 1);
      D.reshape(numberOfPoints.getValue() - 1, numberOfPoints.getValue() + 2);
      C.reshape(numberOfPoints.getValue(), numberOfPoints.getValue() + 2);
      Cblock.reshape(numberOfPoints.getValue() - 1, numberOfPoints.getValue() + 2);
      A.reshape(numberOfPoints.getValue() + 2, numberOfPoints.getValue() + 2);

      s.reshape(numberOfPoints.getValue() + 2, 1);
      sol.reshape(numberOfPoints.getValue() + 2, 1);
      yd.reshape(numberOfPoints.getValue() - 1, 1);

      a.reshape(numberOfPoints.getValue() - 1, 1);
      b.reshape(numberOfPoints.getValue() - 1, 1);
      c.reshape(numberOfPoints.getValue() - 1, 1);
      d.reshape(numberOfPoints.getValue() - 1, 1);
      e.reshape(numberOfPoints.getValue() - 1, 1);
      f.reshape(numberOfPoints.getValue() - 1, 1);

      MatrixTools.diff(xIn, h);

      // Build D DenseMatrix64F
      MatrixTools.setToZero(D);
      D.unsafe_set(0, 1, 1.0);
      for (int i = 1; i < numberOfPoints.getValue() - 1; i++)
      {
         D.unsafe_set(i, i + 1, 2.0 * h.unsafe_get(i - 1, 0));
         D.unsafe_set(i, i + 2, 2.0 * h.unsafe_get(i - 1, 0));
         MatrixTools.addMatrixBlock(D, i, 0, D, i - 1, 0, 1, numberOfPoints.getValue() + 2, 1.0);
      }

      // Build C DenseMatrix64F
      MatrixTools.setToZero(C);
      C.unsafe_set(0, 0, 1.0);
      for (int i = 1; i < numberOfPoints.getValue(); i++)
      {
         C.unsafe_set(i, i + 1, 4.0 * MathTools.square(h.unsafe_get(i - 1, 0)));
         C.unsafe_set(i, i + 2, 2.0 * MathTools.square(h.unsafe_get(i - 1, 0)));
         MatrixTools.addMatrixBlock(C, i, 0, C, i - 1, 0, 1, numberOfPoints.getValue() + 2, 1.0);
         MatrixTools.addMatrixBlock(C, i, 0, D, i - 1, 0, 1, numberOfPoints.getValue() + 2, 3.0 * h.unsafe_get(i - 1, 0));
      }
      MatrixTools.setMatrixBlock(Cblock, 0, 0, C, 0, 0, numberOfPoints.getValue() - 1, numberOfPoints.getValue() + 2, 1.0);

      // Build A DenseMatrix64F
      MatrixTools.setToZero(A);
      for (int i = 0; i < numberOfPoints.getValue() - 2; i++)
      {
         A.unsafe_set(i + 4, i + 2, 11.0 / 5.0 * MathTools.cube(h.unsafe_get(i, 0)));
         A.unsafe_set(i + 4, i + 3, 4.0 / 5.0 * MathTools.cube(h.unsafe_get(i, 0)) + 4.0 / 5.0 * MathTools.cube(h.unsafe_get(i + 1, 0)));
         A.unsafe_set(i + 4, i + 4, 1.0 / 5.0 * MathTools.cube(h.unsafe_get(i + 1, 0)));
         MatrixTools.addMatrixBlock(A, i + 4, 0, C, i, 0, 1, numberOfPoints.getValue() + 2, h.unsafe_get(i, 0));
         MatrixTools.addMatrixBlock(A, i + 4, 0, C, i + 1, 0, 1, numberOfPoints.getValue() + 2, h.unsafe_get(i + 1, 0));
         MatrixTools.addMatrixBlock(A, i + 4, 0, D, i, 0, 1, numberOfPoints.getValue() + 2, 2.0 * MathTools.square(h.unsafe_get(i, 0)));
         MatrixTools.addMatrixBlock(A, i + 4, 0, D, i + 1, 0, 1, numberOfPoints.getValue() + 2, MathTools.square(h.unsafe_get(i + 1, 0)));
      }

      // Add boundary conditions
      A.unsafe_set(0, 0, h.unsafe_get(0, 0));
      A.unsafe_set(0, 1, MathTools.square(h.unsafe_get(0, 0)));
      A.unsafe_set(0, 2, 4.0 / 5.0 * MathTools.cube(h.unsafe_get(0, 0)));
      A.unsafe_set(0, 3, 1.0 / 5.0 * MathTools.cube(h.unsafe_get(0, 0)));

      A.unsafe_set(1, 0, 2.0);

      A.unsafe_set(2, numberOfPoints.getValue(), 11.0 / 5.0 * MathTools.cube(h.unsafe_get(numberOfPoints.getValue() - 2, 0)));
      A.unsafe_set(2, numberOfPoints.getValue() + 1, 4.0 / 5.0 * MathTools.cube(h.unsafe_get(numberOfPoints.getValue() - 2, 0)));

      MatrixTools.addMatrixBlock(A,
                                 2,
                                 0,
                                 C,
                                 numberOfPoints.getValue() - 2,
                                 0,
                                 1,
                                 numberOfPoints.getValue() + 2,
                                 h.unsafe_get(numberOfPoints.getValue() - 2, 0));
      MatrixTools.addMatrixBlock(A,
                                 2,
                                 0,
                                 D,
                                 numberOfPoints.getValue() - 2,
                                 0,
                                 1,
                                 numberOfPoints.getValue() + 2,
                                 2.0 * MathTools.square(h.unsafe_get(numberOfPoints.getValue() - 2, 0)));

      MatrixTools.addMatrixBlock(A, 3, 0, C, numberOfPoints.getValue() - 1, 0, 1, numberOfPoints.getValue() + 2, 2.0);

      if (!solver.setA(A))
      {
         throw new IllegalArgumentException("Singular matrix");
      }
      initialized.set(true);

      for (int i = 0; i < numberOfSplines; i++)
      {
         splines[i].setCoefficientsSet(false);
      }

   }

   /**
    * Determines the coefficients for one spline
    * 
    * @param splineIndex Index for the spline ( 0 <= splineIndex < pointsToInterpolate)
    * @param yIn         Array (length = pointsToInterpolate) with the y coordinates of the points to
    *                    interpolate
    * @param v0          Initial velocity
    * @param vf          Final velocity
    * @param a0          Initial acceleration
    * @param af          Final acceleration
    */
   public void determineCoefficients(int splineIndex, double[] yIn, double v0, double vf, double a0, double af)
   {
      if (!initialized.getBooleanValue())
         throw new RuntimeException("QuinticSplineInterpolator is not initialized");

      if (splineIndex > numberOfSplines - 1 || splineIndex < 0)
         throw new RuntimeException("SplineIndex is out of bounds");

      if (yIn.length != numberOfPoints.getValue())
         throw new RuntimeException("y should have as many elements as points to interpolate");

      MatrixTools.setMatrixColumnFromArray(a, 0, yIn);

      MatrixTools.diff(yIn, yd);

      if (numberOfPoints.getValue() > 2)
      {
         s.unsafe_set(0, 0, yIn[1] / h.unsafe_get(0, 0) - yIn[0] / h.unsafe_get(1, 0) - v0);
         for (int i = 0; i < numberOfPoints.getValue() - 2; i++)
         {
            s.unsafe_set(i + 4, 0, yd.unsafe_get(i + 1, 0) / h.unsafe_get(i + 1, 0) - yd.unsafe_get(i, 0) / h.unsafe_get(i, 0));
         }
      }
      else
      {
         s.unsafe_set(0, 0, yIn[1] / h.unsafe_get(0, 0) - yIn[0] / h.unsafe_get(0, 0) - v0);
      }

      s.unsafe_set(1, 0, a0);
      s.unsafe_set(2,
                   0,
                   vf - yIn[numberOfPoints.getValue() - 1] / h.unsafe_get(numberOfPoints.getValue() - 2, 0)
                         + yIn[numberOfPoints.getValue() - 2] / h.unsafe_get(numberOfPoints.getValue() - 2, 0));
      s.unsafe_set(3, 0, af);

      /*
       * TODO: Rewrite so no new objects are created
       */

      solver.solve(s, sol);

      CommonOps.mult(Cblock, sol, c);
      CommonOps.mult(D, sol, d);

      MatrixTools.setMatrixBlock(e, 0, 0, sol, 2, 0, numberOfPoints.getValue() - 1, 1, 1.0);

      MatrixTools.diff(sol, 2, numberOfPoints.getValue(), f);
      CommonOps.scale(1.0 / 5.0, f);
      CommonOps.elementDiv(f, h);

      for (int i = 0; i < numberOfPoints.getValue() - 1; i++)
      {
         double hi = h.unsafe_get(i, 0);
         double hi2 = MathTools.square(hi);
         double hi3 = hi2 * hi;
         double hi4 = hi3 * hi;

         b.unsafe_set(i,
                      0,
                      yd.unsafe_get(i, 0) / hi - c.unsafe_get(i, 0) * hi - d.unsafe_get(i, 0) * hi2 - e.unsafe_get(i, 0) * hi3 - f.unsafe_get(i, 0) * hi4);
      }

      splines[splineIndex].seta(a);
      splines[splineIndex].setb(b);
      splines[splineIndex].setc(c);
      splines[splineIndex].setd(d);
      splines[splineIndex].sete(e);
      splines[splineIndex].setf(f);

      splines[splineIndex].setCoefficientsSet(true);

   }

   /**
    * Calculates the y coordinate of the spline corresponding to the x coordinate After calling
    * compute, the resulting position, velocity, acceleration and jerk can be queried using
    * getPosition(spline), getVelocity(spline), getAcceleration(spline) and getJerk(spline)
    * respectively
    * 
    * @param xx x coordinate to calculate
    */
   public void compute(double xx)
   {
      if (xx > x[numberOfPoints.getValue() - 1].getDoubleValue())
         xx = x[numberOfPoints.getValue() - 1].getDoubleValue();
      if (xx < x[0].getDoubleValue())
         xx = x[0].getDoubleValue();

      int index = determineSplineIndex(xx);

      double h = xx - x[index].getDoubleValue();

      double h2 = MathTools.square(h);
      double h3 = h2 * h;
      double h4 = h3 * h;
      double h5 = h4 * h;

      for (int i = 0; i < numberOfSplines; i++)
      {
         splines[i].value(index, h, h2, h3, h4, h5, position[i], velocity[i], acceleration[i], jerk[i], null, null);
      }

   }

   public double getPosition(int spline)
   {
      return position[spline].getValue();
   }

   public double getVelocity(int spline)
   {
      return velocity[spline].getValue();
   }

   public double getAcceleration(int spline)
   {
      return acceleration[spline].getValue();
   }

   public double getJerk(int spline)
   {
      return jerk[spline].getValue();
   }

   private int determineSplineIndex(double xx)
   {
      for (int i = 0; i < numberOfPoints.getValue() - 2; i++)
      {
         if (xx >= x[i].getDoubleValue() && xx <= x[i + 1].getDoubleValue())
            return i;
      }
      return numberOfPoints.getValue() - 2;
   }

   /*
    * Storage for quintic spline coefficients
    */
   private class QuinticSpline
   {
      private final int segments;
      private final YoVariableRegistry registry;

      private final YoDouble[] a;
      private final YoDouble[] b;
      private final YoDouble[] c;
      private final YoDouble[] d;
      private final YoDouble[] e;
      private final YoDouble[] f;

      private final YoBoolean coefficientsSet;

      public QuinticSpline(String name, int pointsToInterpolate, YoVariableRegistry parentRegistry)
      {
         this.segments = pointsToInterpolate - 1;
         this.registry = new YoVariableRegistry(name);
         parentRegistry.addChild(this.registry);
         a = new YoDouble[segments];
         b = new YoDouble[segments];
         c = new YoDouble[segments];
         d = new YoDouble[segments];
         e = new YoDouble[segments];
         f = new YoDouble[segments];

         coefficientsSet = new YoBoolean(name + "_initialized", registry);
         coefficientsSet.set(false);

         for (int i = 0; i < segments; i++)
         {
            a[i] = new YoDouble(name + "_a[" + i + "]", registry);
            b[i] = new YoDouble(name + "_b[" + i + "]", registry);
            c[i] = new YoDouble(name + "_c[" + i + "]", registry);
            d[i] = new YoDouble(name + "_d[" + i + "]", registry);
            e[i] = new YoDouble(name + "_e[" + i + "]", registry);
            f[i] = new YoDouble(name + "_f[" + i + "]", registry);
         }
      }

      private void set(YoDouble[] var, DenseMatrix64F value)
      {
         for (int i = 0; i < segments; i++)
         {
            var[i].set(value.unsafe_get(i, 0));
         }
      }

      protected void seta(DenseMatrix64F value)
      {
         set(a, value);
      }

      protected void setb(DenseMatrix64F value)
      {
         set(b, value);
      }

      protected void setc(DenseMatrix64F value)
      {
         set(c, value);
      }

      protected void setd(DenseMatrix64F value)
      {
         set(d, value);
      }

      protected void sete(DenseMatrix64F value)
      {
         set(e, value);
      }

      protected void setf(DenseMatrix64F value)
      {
         set(f, value);
      }

      protected void setCoefficientsSet(boolean coefficientsSet)
      {
         this.coefficientsSet.set(coefficientsSet);
      }

      protected void value(int index, double h, double h2, double h3, double h4, double h5, YoDouble pos, YoDouble vel, YoDouble acc, YoDouble jerk,
                           YoDouble snap, YoDouble crackle)
      {
         if (!coefficientsSet.getBooleanValue())
            throw new RuntimeException("Spline coefficients not set");

         pos.set(a[index].getDoubleValue() + b[index].getDoubleValue() * h + c[index].getDoubleValue() * h2 + d[index].getDoubleValue() * h3
               + e[index].getDoubleValue() * h4 + f[index].getDoubleValue() * h5);

         if (crackle != null)
         {
            crackle.set(120.0 * f[index].getDoubleValue());
         }
         if (snap != null)
         {
            snap.set(24.0 * e[index].getDoubleValue() + 120.0 * f[index].getDoubleValue() * h);
         }
         jerk.set(6.0 * d[index].getDoubleValue() + 24.0 * e[index].getDoubleValue() * h + 60 * f[index].getDoubleValue() * h2);
         acc.set(2.0 * c[index].getDoubleValue() + 6.0 * d[index].getDoubleValue() * h + 12.0 * e[index].getDoubleValue() * h2
               + 20.0 * f[index].getDoubleValue() * h3);
         vel.set(b[index].getDoubleValue() + 2.0 * c[index].getDoubleValue() * h + 3.0 * d[index].getDoubleValue() * h2 + 4.0 * e[index].getDoubleValue() * h3
               + 5.0 * f[index].getDoubleValue() * h4);
      }

   }

}
