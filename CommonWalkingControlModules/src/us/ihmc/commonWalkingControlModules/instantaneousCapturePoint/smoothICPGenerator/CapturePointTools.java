package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator;

import java.util.ArrayList;

import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;


public class CapturePointTools
{
   /**
    * Compute constant centers of pressure, placing the initial COP between the feet 
    * and all of the rest on the footsteps.
    * 
    * @param arrayToPack ArrayList that will be packed with the constant center of pressure locations
    * @param footstepList ArrayList containing the footsteps
    * @param numberFootstepsToConsider Integer describing the number of footsteps to consider when laying out the COP's
    */
   public static void computeConstantCentersOfPressureWithStartBetweenFeetAndRestOnFeet(ArrayList<YoFramePoint> arrayToPack,
         ArrayList<YoFramePoint> footstepList, int numberFootstepsToConsider)
   {
      arrayToPack.get(0).set(footstepList.get(0));
      arrayToPack.get(0).add(footstepList.get(1));
      arrayToPack.get(0).scale(0.5);

      for (int i = 1; i < numberFootstepsToConsider; i++)
      {
         arrayToPack.get(i).set(footstepList.get(i));
      }
   }

   /**
    * Put the constant COP'S except the last one on the footsteps. 
    * Put the last COP between the feet.
    * 
    * @param arrayToPack ArrayList that will be packed with the constant center of pressure locations
    * @param footstepList ArrayList containing the footsteps
    * @param numberFootstepsToConsider Integer describing the number of footsteps to consider when laying out the COP's
    */
   public static void computeConstantCentersOfPressuresOnFeetWithEndBetweenFeet(ArrayList<YoFramePoint> arrayToPack, ArrayList<YoFramePoint> footstepList,
         int numberFootstepsToConsider)
   {
      for (int i = 0; i < numberFootstepsToConsider - 1; i++)
      {
         arrayToPack.get(i).set(footstepList.get(i));
      }

      arrayToPack.get(numberFootstepsToConsider - 1).set(footstepList.get(numberFootstepsToConsider - 2));
      arrayToPack.get(numberFootstepsToConsider - 1).add(footstepList.get(numberFootstepsToConsider - 1));
      arrayToPack.get(numberFootstepsToConsider - 1).scale(0.5);
   }
   
   /**
    * Put the first and last COP between the footsteps, all the
    * rest go on the footsteps. 
    * 
    * @param arrayToPack ArrayList that will be packed with the constant center of pressure locations
    * @param footstepList ArrayList containing the footsteps
    * @param numberFootstepsToConsider Integer describing the number of footsteps to consider when laying out the COP's
    */
   public static void computeConstantCentersOfPressuresWithBeginningAndEndBetweenFeetRestOnFeet(ArrayList<YoFramePoint> arrayToPack, ArrayList<YoFramePoint> footstepList,
         int numberFootstepsToConsider)
   {
      arrayToPack.get(0).set(footstepList.get(0));
      arrayToPack.get(0).add(footstepList.get(1));
      arrayToPack.get(0).scale(0.5);
      
      for (int i = 1; i < numberFootstepsToConsider - 1; i++)
      {
         arrayToPack.get(i).set(footstepList.get(i));
      }

      arrayToPack.get(numberFootstepsToConsider - 1).set(footstepList.get(numberFootstepsToConsider - 2));
      arrayToPack.get(numberFootstepsToConsider - 1).add(footstepList.get(numberFootstepsToConsider - 1));
      arrayToPack.get(numberFootstepsToConsider - 1).scale(0.5);
   }

   /**
    * Put the constant COP'S except the last one on the footsteps. 
    * Put the last COP between the feet.
    * 
    * @param arrayToPack ArrayList that will be packed with the constant center of pressure locations
    * @param footstepList ArrayList containing the footsteps
    * @param numberFootstepsToConsider Integer describing the number of footsteps to consider when laying out the COP's
    */
   public static void computeConstantCentersOfPressuresOnFeet(ArrayList<YoFramePoint> arrayToPack, ArrayList<YoFramePoint> footstepList,
         int numberFootstepsToConsider)
   {
      for (int i = 0; i < numberFootstepsToConsider - 1; i++)
      {
         arrayToPack.get(i).set(footstepList.get(i));
      }
   }

   /**
    * Backward calculation of desired end of step capture point locations.
    * 
    * @param constantCentersOfPressure
    * @param capturePointsToPack
    * @param stepTime
    * @param omega0
    */
   public static void computeDesiredEndOfStepCapturePointLocations(ArrayList<YoFramePoint> constantCentersOfPressure,
         ArrayList<YoFramePoint> capturePointsToPack, double stepTime, double omega0)
   {
      // Probably something broken with this logic.
      capturePointsToPack.set(capturePointsToPack.size()-1, constantCentersOfPressure.get(constantCentersOfPressure.size()-1));

      for (int i = constantCentersOfPressure.size() - 2; i >= 0; i--)
      {
         double tmpX = (capturePointsToPack.get(i+1).getX()-constantCentersOfPressure.get(i).getX())*(1 / Math.exp(omega0 * stepTime)) + constantCentersOfPressure.get(i).getX();
         double tmpY = (capturePointsToPack.get(i+1).getY()-constantCentersOfPressure.get(i).getY())*(1 / Math.exp(omega0 * stepTime)) + constantCentersOfPressure.get(i).getY();
         
         capturePointsToPack.get(i).setX(tmpX);
         capturePointsToPack.get(i).setY(tmpY);
      }
   }

   /**
    * Compute the desired capture point position at a given time. 
    * ICP_d = e^{w0*t} + (1-e^{w0*t})*p0
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCenterOfPressure
    * @param positionToPack
    */
   public static void computeDesiredCapturePointPosition(double omega0, double time, YoFramePoint initialCapturePoint, YoFramePoint initialCenterOfPressure,
         YoFramePoint positionToPack)
   {
      initialCapturePoint.getReferenceFrame().checkReferenceFrameMatch(initialCenterOfPressure.getReferenceFrame());
      
      double exponentialTerm = Math.exp(omega0*time);
      double copX0 = initialCenterOfPressure.getX(); // This is done using doubles to prevent the generation of garbage 
      double copY0 = initialCenterOfPressure.getY(); // by allocating new memory on the heap.
      
      copX0 = (1-exponentialTerm)*copX0;
      copY0 = (1-exponentialTerm)*copY0;
      
      positionToPack.set(initialCapturePoint);
      positionToPack.scale(exponentialTerm);
      
      positionToPack.setX(positionToPack.getX() + copX0);
      positionToPack.setY(positionToPack.getY() + copY0);
      
   }
   
   /**
    * Compute the desired capture point velocity at a given time.
    * ICPv_d = w * e^{w*t} * ICP0 - p0 * w * e^{w*t}
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCenterOfPressure
    * @param velocityToPack
    */
   public static void computeDesiredCapturePointVelocity(double omega0, double time, YoFramePoint initialCapturePoint, YoFramePoint initialCenterOfPressure,
         YoFrameVector velocityToPack)
   {
      initialCapturePoint.getReferenceFrame().checkReferenceFrameMatch(initialCenterOfPressure.getReferenceFrame());
      
      double exponentialTerm = Math.exp(omega0*time);
      
      double x = omega0*exponentialTerm*initialCapturePoint.getX() - initialCenterOfPressure.getX()*omega0*exponentialTerm;
      double y = omega0*exponentialTerm*initialCapturePoint.getY() - initialCenterOfPressure.getY()*omega0*exponentialTerm;
      
      velocityToPack.setX(x);
      velocityToPack.setY(y);
   }
   
   /**
    * Compute the desired capture point acceleration given the desired 
    * capture point velocity
    * 
    * @param omega0
    * @param desiredCapturePointVelocity
    * @param accelerationToPack
    */
   public static void computeDesiredCapturePointAcceleration(double omega0, YoFrameVector desiredCapturePointVelocity, YoFrameVector accelerationToPack)
   {
      accelerationToPack.set(desiredCapturePointVelocity);
      accelerationToPack.scale(omega0);
   }
   
   /**
    * Compute the desired capture point velocity at a given time.
    * ICPv_d = w^2 * e^{w*t} * ICP0 - p0 * w^2 * e^{w*t}
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCenterOfPressure
    * @param accelerationToPack
    */
   public static void computeDesiredCapturePointAcceleration(double omega0, double time, YoFramePoint initialCapturePoint, YoFramePoint initialCenterOfPressure,
         YoFrameVector accelerationToPack)
   {
      initialCapturePoint.getReferenceFrame().checkReferenceFrameMatch(initialCenterOfPressure.getReferenceFrame());
      
      double exponentialTerm = Math.exp(omega0*time);
      
      double x = omega0*(omega0*exponentialTerm*initialCapturePoint.getX() - initialCenterOfPressure.getX()*omega0*exponentialTerm);
      double y = omega0*(omega0*exponentialTerm*initialCapturePoint.getY() - initialCenterOfPressure.getY()*omega0*exponentialTerm);
      
      accelerationToPack.setX(x);
      accelerationToPack.setY(y);
   }
}
