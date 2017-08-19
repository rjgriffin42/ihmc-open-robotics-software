package us.ihmc.robotics.math.trajectories;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.ihmc.commons.Epsilons;
import us.ihmc.commons.PrintTools;
import us.ihmc.robotics.dataStructures.ComplexNumber;
import us.ihmc.robotics.dataStructures.Polynomial;
import us.ihmc.robotics.math.FastFourierTransform;

public class TrajectoryMathTools
{
   private final int maxNumberOfCoefficients;
   private final Trajectory tempTraj1;
   private final Trajectory tempTraj2;
   private final Trajectory3D tempTraj3;
   private final List<Double> tempTimeList;
   private final FastFourierTransform fft;
   private final ComplexNumber[] tempComplex1;
   private final ComplexNumber[] tempComplex2;
   
   private ComplexNumber[] tempComplexReference;
   private FrameTrajectory3D segmentTraj1, segmentTraj2;
   private int tempTimeArrayLength;
   
   public TrajectoryMathTools(int maxNumberOfCoefficients)
   {
      this.maxNumberOfCoefficients = (int) Math.pow(2, Math.ceil( Math.log(maxNumberOfCoefficients) / Math.log(2.0)) ); // Rounding up the nearest power of two
      this.tempTraj1 = new Trajectory(this.maxNumberOfCoefficients);
      this.tempTraj2 = new Trajectory(this.maxNumberOfCoefficients);
      this.tempTraj3 = new Trajectory3D(this.maxNumberOfCoefficients);
      this.tempTimeList = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0, 0.0));
      this.tempTimeArrayLength = 4;
      this.fft = new FastFourierTransform(this.maxNumberOfCoefficients);
      this.tempComplex1 = ComplexNumber.getComplexArray(this.maxNumberOfCoefficients);
      this.tempComplex2 = ComplexNumber.getComplexArray(this.maxNumberOfCoefficients);
   }

   public void scale(Trajectory scaledTrajectoryToPack, Trajectory trajectoryToScale, double scalar)
   {
      for (int i = 0; i < trajectoryToScale.getNumberOfCoefficients(); i++)
         scaledTrajectoryToPack.setDirectlyFast(i, trajectoryToScale.getCoefficient(i) * scalar);
   }

   /**
    * Add two trajectories that have the same start and end times. 
    * Throws runtime exception in case the start and end time values do not match
    * @param trajToPack
    * @param traj1
    * @param traj2
    */
   public void add(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      validatePackingTrajectoryForLinearCombination(trajToPack, traj1, traj2);
      validateTrajectoryTimes(traj1, traj2);
      trajToPack.setTime(traj1.getInitialTime(), traj2.getFinalTime());
      setCoeffsByAddition(trajToPack, traj1, traj2);
   }

   /**
    * Adds two trajectories by taking the intersection of the time intervals over which they are defined.
    * Throws runtime exception in case null intersection is found between the two trajectories
    * @param trajToPack
    * @param traj1
    * @param traj2
    */
   public void addByTrimming(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      validatePackingTrajectoryForLinearCombination(trajToPack, traj1, traj2);
      setTimeIntervalByTrimming(trajToPack, traj1, traj2);
      setCoeffsByAddition(trajToPack, traj1, traj2);
   }

   private void setCoeffsByAddition(Trajectory trajectoryToPack, Trajectory trajectory1, Trajectory trajectory2)
   {
      int numberOfCoeffsToSet = Math.max(trajectory1.getNumberOfCoefficients(), trajectory2.getNumberOfCoefficients());

      for (int i = 0; i < numberOfCoeffsToSet; i++)
      {
         double coefficient = 0.0;
         if (i < trajectory1.getNumberOfCoefficients())
            coefficient += trajectory1.getCoefficient(i);
         if (i < trajectory2.getNumberOfCoefficients())
            coefficient += trajectory2.getCoefficient(i);
         trajectoryToPack.setDirectlyFast(i, coefficient);
      }
      trajectoryToPack.reshape(numberOfCoeffsToSet);
   }

   /**
    * Subtracts {@code traj2} from {@code traj1} in case the two have the same start and end times
    * Throws runtime exception in case the start and end time values do not match or if {@code trajToPack} is not large enough to store the result
    * @param trajToPack
    * @param traj1
    * @param traj2
    */
   public void subtract(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      validatePackingTrajectoryForLinearCombination(trajToPack, traj1, traj2);
      validateTrajectoryTimes(traj1, traj2);
      trajToPack.setTime(traj1.getInitialTime(), traj2.getFinalTime());
      setCoeffsBySubtraction(trajToPack, traj1, traj2);
   }

   /**
    * Subtracts the two trajectories by taking the intersection of the time intervals over which the two are defined
    * @param trajToPack
    * @param traj1
    * @param traj2
    */
   public void subtractByTrimming(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      validatePackingTrajectoryForLinearCombination(trajToPack, traj1, traj2);
      setTimeIntervalByTrimming(trajToPack, traj1, traj2);
      setCoeffsBySubtraction(trajToPack, traj1, traj2);
   }

   private void setCoeffsBySubtraction(Trajectory trajectoryToPack, Trajectory trajectory1, Trajectory trajectory2)
   {
      int numberOfCoeffsToSet = Math.max(trajectory1.getNumberOfCoefficients(), trajectory2.getNumberOfCoefficients());
      for (int i = 0; i < numberOfCoeffsToSet; i++)
      {
         double coefficient = 0.0;
         if (i < trajectory1.getNumberOfCoefficients())
            coefficient += trajectory1.getCoefficient(i);
         if (i < trajectory2.getNumberOfCoefficients())
            coefficient -= trajectory2.getCoefficient(i);
         trajectoryToPack.setDirectlyFast(i, coefficient);
      }
      trajectoryToPack.reshape(numberOfCoeffsToSet);
   }

   public void multiply(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      validatePackingTrajectoryForMultiplication(trajToPack, traj1, traj2);
      validateTrajectoryTimes(traj1, traj2);
      trajToPack.setTime(traj1.getInitialTime(), traj2.getFinalTime());
      setCoeffsByMultiplication(trajToPack, traj1, traj2);
   }

   public void multiplyByTrimming(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      validatePackingTrajectoryForMultiplication(trajToPack, traj1, traj2);
      setTimeIntervalByTrimming(trajToPack, traj1, traj2);
      setCoeffsByMultiplication(trajToPack, traj1, traj2);
   }

   private void setCoeffsByMultiplication(Trajectory trajectoryToPack, Trajectory trajectory1, Trajectory trajectory2)
   {
      int numberOfCoeffsToSet = trajectory1.getNumberOfCoefficients() + trajectory2.getNumberOfCoefficients() - 1;

      fft.setCoefficients(trajectory1.getCoefficients());
      tempComplexReference = fft.getForwardTransform();
      ComplexNumber.copyComplexArray(tempComplex1, tempComplexReference);

      fft.setCoefficients(trajectory2.getCoefficients());
      tempComplexReference = fft.getForwardTransform();
      ComplexNumber.copyComplexArray(tempComplex2, tempComplexReference);

      for (int i = 0; i < tempComplex1.length; i++)
         tempComplex1[i].timesAndStore(tempComplex2[i]);

      fft.setCoefficients(tempComplex1);
      tempComplexReference = fft.getInverseTransform();

      for (int i = 0; i < numberOfCoeffsToSet; i++)
         trajectoryToPack.setDirectlyFast(i, tempComplexReference[i].real());
      trajectoryToPack.reshape(numberOfCoeffsToSet);
   }

   public void scale(Trajectory3D trajToPack, Trajectory3D traj, double scalarX, double scalarY, double scalarZ)
   {
      scale(trajToPack.getTrajectoryX(), traj.getTrajectoryX(), scalarX);
      scale(trajToPack.getTrajectoryY(), traj.getTrajectoryY(), scalarY);
      scale(trajToPack.getTrajectoryZ(), traj.getTrajectoryZ(), scalarZ);
   }

   public void scale(Trajectory3D trajToPack, Trajectory3D traj, double scalar)
   {
      scale(trajToPack, traj, scalar, scalar, scalar);
   }

   public void add(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      for (int direction = 0; direction < 3; direction++)
         add(trajToPack.getTrajectory(direction), traj1.getTrajectory(direction), traj2.getTrajectory(direction));
   }

   public void addByTrimming(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      for (int direction = 0; direction < 3; direction++)
         addByTrimming(trajToPack.getTrajectory(direction), traj1.getTrajectory(direction), traj2.getTrajectory(direction));
   }

   public void subtract(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      for (int direction = 0; direction < 3; direction++)
         subtract(trajToPack.getTrajectory(direction), traj1.getTrajectory(direction), traj2.getTrajectory(direction));
   }

   public void subtractByTrimming(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      for (int direction = 0; direction < 3; direction++)
         subtractByTrimming(trajToPack.getTrajectory(direction), traj1.getTrajectory(direction), traj2.getTrajectory(direction));
   }

   public void dotProduct(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      for (int direction = 0; direction < 3; direction++)
         multiply(trajToPack.getTrajectory(direction), traj1.getTrajectory(direction), traj2.getTrajectory(direction));
   }

   public void dotProductByTrimming(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      for (int direction = 0; direction < 3; direction++)
         multiplyByTrimming(trajToPack.getTrajectory(direction), traj1.getTrajectory(direction), traj2.getTrajectory(direction));
   }

   public void dotProduct(Trajectory trajToPackX, Trajectory trajToPackY, Trajectory trajToPackZ, Trajectory traj1X, Trajectory traj1Y,
                                 Trajectory traj1Z, Trajectory traj2X, Trajectory traj2Y, Trajectory traj2Z)
   {
      multiply(trajToPackX, traj1X, traj2X);
      multiply(trajToPackY, traj1Y, traj2Y);
      multiply(trajToPackZ, traj1Z, traj2Z);
   }

   public void dotProductByTrimming(Trajectory trajToPackX, Trajectory trajToPackY, Trajectory trajToPackZ, Trajectory traj1X,
                                           Trajectory traj1Y, Trajectory traj1Z, Trajectory traj2X, Trajectory traj2Y, Trajectory traj2Z)
   {
      multiplyByTrimming(trajToPackX, traj1X, traj2X);
      multiplyByTrimming(trajToPackY, traj1Y, traj2Y);
      multiplyByTrimming(trajToPackZ, traj1Z, traj2Z);
   }

   public void crossProduct(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      crossProduct(trajToPack.getTrajectoryX(), trajToPack.getTrajectoryY(), trajToPack.getTrajectoryZ(), traj1.getTrajectoryX(),
                   traj1.getTrajectoryY(), traj1.getTrajectoryZ(), traj2.getTrajectoryX(), traj2.getTrajectoryY(), traj2.getTrajectoryZ());
   }

   public void crossProductByTrimming(Trajectory3D trajToPack, Trajectory3D traj1, Trajectory3D traj2)
   {
      crossProductByTrimming(trajToPack.getTrajectoryX(), trajToPack.getTrajectoryY(), trajToPack.getTrajectoryZ(), traj1.getTrajectoryX(),
                             traj1.getTrajectoryY(), traj1.getTrajectoryZ(), traj2.getTrajectoryX(), traj2.getTrajectoryY(), traj2.getTrajectoryZ());
   }

   public void crossProduct(Trajectory xTrajectoryToPack, Trajectory yTrajectoryToPack, Trajectory zTrajectoryToPack,
                                   Trajectory traj1X, Trajectory traj1Y, Trajectory traj1Z, Trajectory traj2X, Trajectory traj2Y, Trajectory traj2Z)
   {
      multiply(tempTraj1, traj1Y, traj2Z);
      multiply(tempTraj2, traj1Z, traj2Y);
      subtract(tempTraj3.xTrajectory, tempTraj1, tempTraj2);
      
      multiply(tempTraj1, traj1X, traj2Z);
      multiply(tempTraj2, traj1Z, traj2X);
      subtract(tempTraj3.yTrajectory, tempTraj2, tempTraj1);

      multiply(tempTraj1, traj1X, traj2Y);
      multiply(tempTraj2, traj1Y, traj2X);
      subtract(tempTraj3.zTrajectory, tempTraj1, tempTraj2);

      xTrajectoryToPack.set(tempTraj3.xTrajectory);
      yTrajectoryToPack.set(tempTraj3.yTrajectory);
      zTrajectoryToPack.set(tempTraj3.zTrajectory);
   }

   public void crossProductByTrimming(Trajectory trajToPackX, Trajectory trajToPackY, Trajectory trajToPackZ, Trajectory traj1X,
                                             Trajectory traj1Y, Trajectory traj1Z, Trajectory traj2X, Trajectory traj2Y, Trajectory traj2Z)
   {
      multiplyByTrimming(tempTraj1, traj1Y, traj2Z);
      multiplyByTrimming(tempTraj2, traj1Z, traj2Y);
      subtractByTrimming(tempTraj3.xTrajectory, tempTraj1, tempTraj2);

      multiplyByTrimming(tempTraj1, traj1X, traj2Z);
      multiplyByTrimming(tempTraj2, traj1Z, traj2X);
      subtractByTrimming(tempTraj3.yTrajectory, tempTraj2, tempTraj1);

      multiplyByTrimming(tempTraj1, traj1X, traj2Y);
      multiplyByTrimming(tempTraj2, traj1Y, traj2X);
      subtractByTrimming(tempTraj3.zTrajectory, tempTraj1, tempTraj2);
      trajToPackX.set(tempTraj3.xTrajectory);
      trajToPackY.set(tempTraj3.yTrajectory);
      trajToPackZ.set(tempTraj3.zTrajectory);
   }

   public void validateTrajectoryTimes(Trajectory traj1, Trajectory traj2)
   {
      if (Math.abs(traj1.getInitialTime() - traj2.getInitialTime()) > Epsilons.ONE_THOUSANDTH
            || Math.abs(traj1.getFinalTime() - traj2.getFinalTime()) > Epsilons.ONE_THOUSANDTH)
      {
         PrintTools.warn("Time mismatch in trajectories being added");
         throw new InvalidParameterException();
      }
   }

   private void setTimeIntervalByTrimming(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      double latestStartingTime = Math.max(traj1.getInitialTime(), traj2.getInitialTime());
      double earliestEndingTime = Math.min(traj1.getFinalTime(), traj2.getFinalTime());
      if (earliestEndingTime <= latestStartingTime)
      {
         PrintTools.debug(traj1.toString());
         PrintTools.debug(traj2.toString());
         throw new RuntimeException("Got null intersection for time intervals during trajectory operation");
      }
      trajToPack.setInitialTime(latestStartingTime);
      trajToPack.setFinalTime(earliestEndingTime);
   }

   public void validatePackingTrajectoryForLinearCombination(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      if (trajToPack.getMaximumNumberOfCoefficients() < Math.max(traj1.getNumberOfCoefficients(), traj2.getNumberOfCoefficients()))
      {
         PrintTools.warn("Not enough coefficients to store result of trajectory operation. Needed: "
               + Math.max(traj1.getNumberOfCoefficients(), traj2.getNumberOfCoefficients()) + " Available: " + trajToPack.getMaximumNumberOfCoefficients());
         throw new InvalidParameterException();
      }
   }

   public void validatePackingTrajectoryForMultiplication(Trajectory trajToPack, Trajectory traj1, Trajectory traj2)
   {
      if (trajToPack.getMaximumNumberOfCoefficients() < traj1.getNumberOfCoefficients() + traj2.getNumberOfCoefficients() - 1)
      {
         PrintTools.warn("Not enough coefficients to store result of trajectory multplication");
         throw new InvalidParameterException();
      }
   }

   /**
    * Add trajectories that do not have the same initial and final time. Trajectories added are assumed to be zero where they are not defined 
    * @param trajListToPack
    * @param traj1
    * @param traj2
    * @param TIME_EPSILON
    */
   public int add(List<Trajectory> trajListToPack, Trajectory traj1, Trajectory traj2, double TIME_EPSILON)
   {
      checkZeroTimeTrajectory(traj1, TIME_EPSILON);
      checkZeroTimeTrajectory(traj2, TIME_EPSILON);
      int numberOfSegments = getSegmentTimeList(tempTimeList, traj1, traj2, TIME_EPSILON);
      for (int i = 0; i < numberOfSegments; i++)
      {
         Trajectory segmentTrajToPack = trajListToPack.get(i);
         setCurrentSegmentPolynomial(tempTraj1, traj1, tempTimeList.get(i), tempTimeList.get(i + 1), TIME_EPSILON);
         setCurrentSegmentPolynomial(tempTraj2, traj2, tempTimeList.get(i), tempTimeList.get(i + 1), TIME_EPSILON);
         add(segmentTrajToPack, tempTraj1, tempTraj2);
      }
      return numberOfSegments;
   }

   /**
    * Subtract trajectories that do not have the same initial and final time. Trajectories added are assumed to be zero where they are not defined 
    * @param trajListToPack
    * @param traj1
    * @param traj2
    * @param TIME_EPSILON
    */
   public int subtract(List<Trajectory> trajListToPack, Trajectory traj1, Trajectory traj2, double TIME_EPSILON)
   {
      checkZeroTimeTrajectory(traj1, TIME_EPSILON);
      checkZeroTimeTrajectory(traj2, TIME_EPSILON);
      int numberOfSegments = getSegmentTimeList(tempTimeList, traj1, traj2, TIME_EPSILON);
      for (int i = 0; i < numberOfSegments; i++)
      {
         Trajectory segmentTrajToPack = trajListToPack.get(i);
         setCurrentSegmentPolynomial(tempTraj1, traj1, tempTimeList.get(i), tempTimeList.get(i + 1), TIME_EPSILON);
         setCurrentSegmentPolynomial(tempTraj2, traj2, tempTimeList.get(i), tempTimeList.get(i + 1), TIME_EPSILON);
         subtract(segmentTrajToPack, tempTraj1, tempTraj2);
      }
      return numberOfSegments;
   }

   /**
    * Multiply trajectories that do not have the same initial and final time. Trajectories added are assumed to be zero where they are not defined 
    * @param trajListToPack
    * @param traj1
    * @param traj2
    * @param TIME_EPSILON
    */
   public int multiply(List<Trajectory> trajListToPack, Trajectory traj1, Trajectory traj2, double TIME_EPSILON)
   {
      checkZeroTimeTrajectory(traj1, TIME_EPSILON);
      checkZeroTimeTrajectory(traj2, TIME_EPSILON);
      int numberOfSegments = getSegmentTimeList(tempTimeList, traj1, traj2, TIME_EPSILON);
      for (int i = 0; i < numberOfSegments; i++)
      {
         Trajectory segmentTrajToPack = trajListToPack.get(i);
         setCurrentSegmentPolynomial(tempTraj1, traj1, tempTimeList.get(i), tempTimeList.get(i + 1), TIME_EPSILON);
         setCurrentSegmentPolynomial(tempTraj2, traj2, tempTimeList.get(i), tempTimeList.get(i + 1), TIME_EPSILON);
         multiply(segmentTrajToPack, tempTraj1, tempTraj2);
      }
      return numberOfSegments;
   }

   private void setCurrentSegmentPolynomial(Trajectory trajToPack, Trajectory traj, double segmentStartTime, double segmentFinalTime,
                                                   double TIME_EPSILON)
   {
      trajToPack.set(traj);
      trajToPack.setInitialTime(segmentStartTime);
      trajToPack.setFinalTime(segmentFinalTime);
      if (traj.getInitialTime() > segmentStartTime + TIME_EPSILON || traj.getFinalTime() < segmentStartTime + TIME_EPSILON)
         trajToPack.setZero();
   }

   private void setCurrentSegmentPolynomial(Trajectory3D trajToPack, Trajectory3D traj, double segmentStartTime, double segmentFinalTime,
                                                   double TIME_EPSILON)
   {
      for (int i = 0; i < 3; i++)
         setCurrentSegmentPolynomial(trajToPack.getTrajectory(i), traj.getTrajectory(i), segmentStartTime, segmentFinalTime, TIME_EPSILON);
   }

   public int getSegmentTimeList(List<Double> trajTimeListToPack, Trajectory traj1, Trajectory traj2, double TIME_EPSILON)
   {
      trajTimeListToPack.set(0, traj1.getInitialTime());
      trajTimeListToPack.set(1, traj1.getFinalTime());
      trajTimeListToPack.set(2, traj2.getInitialTime());
      trajTimeListToPack.set(3, traj2.getFinalTime());

      tempTimeArrayLength = trajTimeListToPack.size();
      for (int i = 0, j = 0, k = 2; k < tempTimeArrayLength; i++)
      {
         if (Math.abs(trajTimeListToPack.get(j) - trajTimeListToPack.get(k)) < TIME_EPSILON)
         {
            tempTimeArrayLength--;
            trajTimeListToPack.set(k, trajTimeListToPack.get(tempTimeArrayLength));
            trajTimeListToPack.set(tempTimeArrayLength, Double.POSITIVE_INFINITY);
         }
         if (trajTimeListToPack.get(j) > trajTimeListToPack.get(k))
         {
            trajTimeListToPack.set(j, trajTimeListToPack.get(j) + trajTimeListToPack.get(k));
            trajTimeListToPack.set(k, trajTimeListToPack.get(j) - trajTimeListToPack.get(k));
            trajTimeListToPack.set(j, trajTimeListToPack.get(j) - trajTimeListToPack.get(k));
         }
         j++;
         k += i;
      }
      if (trajTimeListToPack.get(1) > trajTimeListToPack.get(2))
      {
         trajTimeListToPack.set(1, trajTimeListToPack.get(1) + trajTimeListToPack.get(2));
         trajTimeListToPack.set(2, trajTimeListToPack.get(1) - trajTimeListToPack.get(2));
         trajTimeListToPack.set(1, trajTimeListToPack.get(1) - trajTimeListToPack.get(2));
      }
      return tempTimeArrayLength - 1;
   }

   public void checkZeroTimeTrajectory(Trajectory trajectory, double TIME_EPSILON)
   {
      if (Math.abs(trajectory.getFinalTime() - trajectory.getInitialTime()) < TIME_EPSILON)
         throw new RuntimeException("Cannot operate with null trajectory, start time: " + trajectory.getInitialTime() + " end time: "
               + trajectory.getFinalTime() + " epsilon: " + TIME_EPSILON);
   }

   public void addTimeOffset(Trajectory trajectory, double timeOffset)
   {
      int index = 1;
      int n = trajectory.getNumberOfCoefficients();
      double factorial = 1;
      double power = 1;
      for (index = 0; index < maxNumberOfCoefficients; index++)
      {
         tempComplex1[index].setToZero();
         tempComplex2[index].setToZero();
      }

      for (index = 1; index <= n; index++)
      {
         tempComplex1[index - 1].setToPurelyReal(power / factorial);
         tempComplex2[n - index].setToPurelyReal(factorial * trajectory.getCoefficient(index - 1));
         power *= -timeOffset;
         factorial *= (index);
      }

      fft.setCoefficients(tempComplex1);
      tempComplexReference = fft.getForwardTransform();
      ComplexNumber.copyComplexArray(tempComplex1, tempComplexReference);

      fft.setCoefficients(tempComplex2);
      tempComplexReference = fft.getForwardTransform();
      ComplexNumber.copyComplexArray(tempComplex2, tempComplexReference);

      for (int i = 0; i < tempComplex1.length; i++)
         tempComplex1[i].timesAndStore(tempComplex2[i]);

      fft.setCoefficients(tempComplex1);
      tempComplexReference = fft.getInverseTransform();

      factorial = 1;
      for (index = 1; index <= n; index++)
      {
         trajectory.setDirectlyFast(index - 1, tempComplexReference[n - index].real() / factorial);
         factorial *= index;
      }
      trajectory.setTime(trajectory.getInitialTime() + timeOffset, trajectory.getFinalTime() + timeOffset);
   }

   public void getIntergal(Trajectory trajectoryToPack, Trajectory trajectoryToIntegrate)
   {
      if (trajectoryToPack.getMaximumNumberOfCoefficients() < trajectoryToIntegrate.getNumberOfCoefficients() + 1)
         throw new InvalidParameterException("Not enough coefficients to store result of trajectory integration");

      for (int i = trajectoryToIntegrate.getNumberOfCoefficients(); i > 0; i--)
         trajectoryToPack.setDirectlyFast(i, trajectoryToIntegrate.getCoefficient(i - 1) / (i));
      trajectoryToIntegrate.compute(trajectoryToIntegrate.getInitialTime());
      double position = trajectoryToIntegrate.getPosition();
      trajectoryToPack.setDirectly(0, -position);
      trajectoryToPack.reshape(trajectoryToIntegrate.getNumberOfCoefficients() + 1);
      trajectoryToPack.setTime(trajectoryToIntegrate.getInitialTime(), trajectoryToIntegrate.getFinalTime());
   }

   public void getDerivative(Trajectory derivativeToPack, Trajectory trajectoryToDifferentiate)
   {
      if (derivativeToPack.getMaximumNumberOfCoefficients() < trajectoryToDifferentiate.getNumberOfCoefficients() - 1)
         throw new InvalidParameterException("Not enough coefficients to store the result of differentiation");

      derivativeToPack.reshape(Math.max(trajectoryToDifferentiate.getNumberOfCoefficients() - 1, 1));
      if (trajectoryToDifferentiate.getNumberOfCoefficients() == 1)
         derivativeToPack.setConstant(trajectoryToDifferentiate.getInitialTime(), trajectoryToDifferentiate.getFinalTime(), 0);
      for (int i = trajectoryToDifferentiate.getNumberOfCoefficients() - 1; i > 0; i--)
         derivativeToPack.setDirectlyFast(i - 1, i * trajectoryToDifferentiate.getCoefficient(i));
      derivativeToPack.setTime(trajectoryToDifferentiate.getInitialTime(), trajectoryToDifferentiate.getFinalTime());
   }

   public void addSegmentedTrajectories(SegmentedFrameTrajectory3D trajToPack, SegmentedFrameTrajectory3D traj1, SegmentedFrameTrajectory3D traj2,
                                               double TIME_EPSILON)
   {
      double currentTime = Math.min(traj1.getSegment(0).getInitialTime(), traj2.getSegment(0).getInitialTime());
      int k = 0;
      for (int i = 0, j = 0; i < traj1.getNumberOfSegments() || j < traj2.getNumberOfSegments(); k++)
      {
         // Select the one that is ahead or set if no intersection
         if (i >= traj1.getNumberOfSegments()
               || (j < traj2.getNumberOfSegments() && traj2.getSegment(j).getFinalTime() < traj1.getSegment(i).getInitialTime() - TIME_EPSILON))
         {
            setCurrentSegmentPolynomial(trajToPack.getSegment(k), traj2.getSegment(j), currentTime, traj2.getSegment(j).getFinalTime(), TIME_EPSILON);
            currentTime = traj2.getSegment(j++).getFinalTime();
            continue;
         }
         else if (j >= traj2.getNumberOfSegments()
               || (i < traj1.getNumberOfSegments() && traj1.getSegment(i).getFinalTime() < traj2.getSegment(j).getInitialTime() - TIME_EPSILON))
         {
            setCurrentSegmentPolynomial(trajToPack.getSegment(k), traj1.getSegment(i), currentTime, traj1.getSegment(i).getFinalTime(), TIME_EPSILON);
            currentTime = traj1.getSegment(i++).getFinalTime();
            continue;
         }
         else if (traj1.getSegment(i).getInitialTime() < traj2.getSegment(j).getInitialTime())
         {
            segmentTraj1 = traj1.getSegment(i);
            segmentTraj2 = traj2.getSegment(j);
         }
         else
         {
            segmentTraj2 = traj1.getSegment(i);
            segmentTraj1 = traj2.getSegment(j);
         }
         
         if (segmentTraj1.getInitialTime() < segmentTraj2.getInitialTime() - TIME_EPSILON && currentTime - TIME_EPSILON < segmentTraj1.getInitialTime())
         {
            setCurrentSegmentPolynomial(trajToPack.getSegment(k++), segmentTraj1, currentTime, segmentTraj2.getInitialTime(), TIME_EPSILON);
         }

         addByTrimming(trajToPack.getSegment(k), segmentTraj1, segmentTraj2);
         currentTime = Math.min(segmentTraj1.getFinalTime(), segmentTraj2.getFinalTime());
         if (currentTime < traj2.getSegment(j).getFinalTime() - TIME_EPSILON)
            i++;
         else if (currentTime < traj1.getSegment(j).getFinalTime() - TIME_EPSILON)
            j++;
         else
         {
            i++;
            j++;
         }
      }
      trajToPack.setNumberOfSegments(k);
   }

   public void getDerivative(FrameTrajectory3D trajectoryToPack, FrameTrajectory3D trajectoryToDifferentiate)
   {
      for(int i = 0; i < 3; i++)
         getDerivative(trajectoryToPack.getTrajectory(i), trajectoryToDifferentiate.getTrajectory(i));
   }

}
