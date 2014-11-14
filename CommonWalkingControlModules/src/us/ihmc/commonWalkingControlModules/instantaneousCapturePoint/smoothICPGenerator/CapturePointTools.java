package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator;

import java.util.ArrayList;

import us.ihmc.utilities.lists.RecyclingArrayList;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;

/**
 * Note: CMP stands for Centroidal Momentum Pivot
 *
 */
public class CapturePointTools
{
   /**
    * Compute the constant CMP locations and store them in constantCMPsToPack.
    * 
    * @param constantCMPsToPack ArrayList that will be packed with the constant CMP locations
    * @param footstepList ArrayList containing the footsteps
    * @param firstFootstepIndex Integer describing the index of the first footstep to consider when laying out the CMP's
    * @param lastFootstepIndex Integer describing the index of the last footstep to consider when laying out the CMP's
    * @param startStanding If true, the first constant CMP will be between the 2 first footsteps, else it will at the first footstep. 
    * @param endStanding If true, the last constant CMP will be between the 2 last footsteps, else it will at the last footstep. 
    */
   public static void computeConstantCMPs(ArrayList<YoFramePoint> constantCMPsToPack, RecyclingArrayList<FramePoint> footstepList, int firstFootstepIndex, int lastFootstepIndex, boolean startStanding, boolean endStanding)
   {
      if (startStanding)
      {
         // Start with the first constant CMP located between the feet.
         YoFramePoint firstConstantCMPPlanned = constantCMPsToPack.get(firstFootstepIndex);
         FramePoint firstFootstepToConsider = footstepList.get(firstFootstepIndex);
         FramePoint secondFootstepToConsider = footstepList.get(firstFootstepIndex + 1);
         putConstantCMPBetweenFeet(firstConstantCMPPlanned, firstFootstepToConsider, secondFootstepToConsider);
         firstFootstepIndex++;
      }

      if (endStanding)
      {
         // End with the last constant CMP located between the feet.
         YoFramePoint lastConstantCMPPlanned = constantCMPsToPack.get(lastFootstepIndex);
         FramePoint lastFootstepToConsider = footstepList.get(lastFootstepIndex);
         FramePoint beforeLastFootstepToConsider = footstepList.get(lastFootstepIndex - 1);
         putConstantCMPBetweenFeet(lastConstantCMPPlanned, beforeLastFootstepToConsider, lastFootstepToConsider);
         lastFootstepIndex--;
      }

      computeConstantCMPsOnFeet(constantCMPsToPack, footstepList, firstFootstepIndex, lastFootstepIndex);
   }

   /**
    * Put the constant CMP's on the footsteps.
    * 
    * @param constantCMPsToPack ArrayList that will be packed with the constant CMP locations
    * @param footstepList ArrayList containing the footsteps
    * @param firstFootstepIndex Integer describing the index of the first footstep to consider when laying out the CMP's
    * @param lastFootstepIndex Integer describing the index of the last footstep to consider when laying out the CMP's
    */
   public static void computeConstantCMPsOnFeet(ArrayList<YoFramePoint> constantCMPsToPack, RecyclingArrayList<FramePoint> footstepList, int firstFootstepIndex, int lastFootstepIndex)
   {
      for (int i = firstFootstepIndex; i <= lastFootstepIndex; i++)
      {
         YoFramePoint constantCMP = constantCMPsToPack.get(i);
         // Put the constant CMP at the footstep location
         constantCMP.setAndMatchFrame(footstepList.get(i));
      }
   }

   /**
    * Put the constant CMP in the middle of the two given footsteps.
    * @param constantCMPToPack YoFramePoint that will be packed with the constant CMP location
    * @param firstFootstep FramePoint holding the position of the first footstep
    * @param secondFootstep FramePoint holding the position of the second footstep
    */
   public static void putConstantCMPBetweenFeet(YoFramePoint constantCMPToPack, FramePoint firstFootstep, FramePoint secondFootstep)
   {
      constantCMPToPack.setAndMatchFrame(firstFootstep);
      constantCMPToPack.add(secondFootstep);
      constantCMPToPack.scale(0.5);
   }

   /**
    * Backward calculation of desired end of step capture point locations.
    * 
    * @param constantCMPs
    * @param cornerPointsToPack
    * @param stepTime
    * @param omega0
    */
   public static void computeDesiredCornerPoints(ArrayList<YoFramePoint> cornerPointsToPack, ArrayList<YoFramePoint> constantCMPs, boolean skipFirstCornerPoint, double stepTime, double omega0)
   {
      double exponentialTerm = Math.exp(- omega0 * stepTime);
      YoFramePoint nextCornerPoint = constantCMPs.get(cornerPointsToPack.size());
      
      int firstCornerPointIndex = skipFirstCornerPoint ? 1 : 0;
      for (int i = cornerPointsToPack.size() - 1; i >= firstCornerPointIndex; i--)
      {
         YoFramePoint cornerPoint = cornerPointsToPack.get(i);
         YoFramePoint initialCMP = constantCMPs.get(i);

         cornerPoint.interpolate(initialCMP, nextCornerPoint, exponentialTerm);
         
         nextCornerPoint = cornerPoint;
      }
   }

   /**
    * Given a desired capturePoint location and an initial position of the capture point,
    * compute the constant CMP that will drive the capture point from the 
    * initial position to the final position.
    * @param cmpToPack
    * @param finalDesiredCapturePoint
    * @param initialCapturePoint
    * @param omega0
    * @param stepTime
    */
   public static void computeConstantCMPFromInitialAndFinalCapturePointLocations(YoFramePoint cmpToPack,
         YoFramePoint finalDesiredCapturePoint, YoFramePoint initialCapturePoint, double omega0, double stepTime)
   {
      double exponentialTerm = Math.exp(- omega0 * stepTime);
      cmpToPack.scaleSub(exponentialTerm, finalDesiredCapturePoint, initialCapturePoint);
      cmpToPack.scale(1.0 / (exponentialTerm - 1.0));
   }

   /**
    * Compute the desired capture point position at a given time. ICP_d =
    * e^{w0*t}*ICP_0 + (1-e^{w0*t})*p0
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCMP
    * @param desiredCapturePointToPack
    */
   public static void computeDesiredCapturePointPosition(double omega0, double time, YoFramePoint initialCapturePoint, YoFramePoint initialCMP, YoFramePoint desiredCapturePointToPack)
   {
      desiredCapturePointToPack.interpolate(initialCMP, initialCapturePoint, Math.exp(omega0 * time));
   }

   /**
    * Compute the desired capture point position at a given time. ICP_d =
    * e^{w0*t}*ICP_0 + (1-e^{w0*t})*p0
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCMP
    * @param desiredCapturePointToPack
    */
   public static void computeDesiredCapturePointPosition(double omega0, double time, FramePoint initialCapturePoint, FramePoint initialCMP, FramePoint desiredCapturePointToPack)
   {
      desiredCapturePointToPack.interpolate(initialCMP, initialCapturePoint, Math.exp(omega0 * time));
   }

   /**
    * Compute the desired capture point velocity at a given time. 
    * ICPv_d = w * e^{w*t} * ICP0 - p0 * w * e^{w*t}
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCMP
    * @param desiredCapturePointVelocityToPack
    */
   public static void computeDesiredCapturePointVelocity(double omega0, double time, YoFramePoint initialCapturePoint, YoFramePoint initialCMP, YoFrameVector desiredCapturePointVelocityToPack)
   {
      desiredCapturePointVelocityToPack.subAndScale(omega0 * Math.exp(omega0 * time), initialCapturePoint, initialCMP);
   }

   /**
    * Compute the desired capture point velocity at a given time.
    * ICPv_d = w * * e^{w*t} * ICP0 - p0 * w * e^{w*t}
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCMP
    * @param desiredCapturePointVelocityToPack
    */
   public static void computeDesiredCapturePointVelocity(double omega0, double time, FramePoint initialCapturePoint, FramePoint initialCMP, FrameVector desiredCapturePointVelocityToPack)
   {
      desiredCapturePointVelocityToPack.subAndScale(omega0 * Math.exp(omega0 * time), initialCapturePoint, initialCMP);
   }

   /**
    * Compute the desired capture point acceleration given the desired capture
    * point velocity
    * 
    * @param omega0
    * @param desiredCapturePointVelocity
    * @param desiredCapturePointAccelerationToPack
    */
   public static void computeDesiredCapturePointAcceleration(double omega0, YoFrameVector desiredCapturePointVelocity, YoFrameVector desiredCapturePointAccelerationToPack)
   {
      desiredCapturePointAccelerationToPack.setAndMatchFrame(desiredCapturePointVelocity.getFrameTuple());
      desiredCapturePointAccelerationToPack.scale(omega0);
   }

   /**
    * Compute the desired capture point velocity at a given time.
    * ICPv_d = w^2 * e^{w*t} * ICP0 - p0 * w^2 * e^{w*t}
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCMP
    * @param desiredCapturePointAccelerationToPack
    */
   public static void computeDesiredCapturePointAcceleration(double omega0, double time, YoFramePoint initialCapturePoint, YoFramePoint initialCMP, YoFrameVector desiredCapturePointAccelerationToPack)
   {
      desiredCapturePointAccelerationToPack.subAndScale(omega0 * omega0 * Math.exp(omega0 * time), initialCapturePoint, initialCMP);
   }

   /**
    * Compute the desired capture point velocity at a given time.
    * ICPv_d = w^2 * e^{w*t} * ICP0 - p0 * w^2 * e^{w*t}
    * 
    * @param omega0
    * @param time
    * @param initialCapturePoint
    * @param initialCMP
    * @param desiredCapturePointAccelerationToPack
    */
   public static void computeDesiredCapturePointAcceleration(double omega0, double time, FramePoint initialCapturePoint, FramePoint initialCMP, FrameVector desiredCapturePointAccelerationToPack)
   {
      desiredCapturePointAccelerationToPack.subAndScale(omega0 * omega0 * Math.exp(omega0 * time), initialCapturePoint, initialCMP);
   }

   /**
    * Computes the desired centroidal momentum pivot by,
    * CMP_{d} = ICP_{d} - \dot{ICP}_{d}/omega0
    * 
    * @param desiredCapturePointPosition
    * @param desiredCapturePointVelocity
    * @param omega0
    * @param desiredCMPToPack
    */
   public static void computeDesiredCentroidalMomentumPivot(YoFramePoint desiredCapturePointPosition, YoFrameVector desiredCapturePointVelocity, double omega0, YoFramePoint desiredCMPToPack)
   {
      desiredCMPToPack.scaleAdd(- 1.0 / omega0, desiredCapturePointVelocity, desiredCapturePointPosition);
   }

   /**
    * Computes the desired centroidal momentum pivot by,
    * CMP_{d} = ICP_{d} - \dot{ICP}_{d}/omega0
    * 
    * @param desiredCapturePointPosition
    * @param desiredCapturePointVelocity
    * @param omega0
    * @param desiredCMPToPack
    */
   public static void computeDesiredCentroidalMomentumPivot(FramePoint desiredCapturePointPosition, FrameVector desiredCapturePointVelocity, double omega0, YoFramePoint desiredCMPToPack)
   {
      desiredCMPToPack.scaleAdd(- 1.0 / omega0, desiredCapturePointVelocity, desiredCapturePointPosition);
   }

   /**
    * Compute the distance along the capture point guide line from the 
    * current capture point position to the desired capture point position.
    * 
    * @param currentCapturePointPosition
    * @param desiredCapturePointPosition
    * @param desiredCapturePointVelocity
    * @return
    */
   public static double computeDistanceToCapturePointFreezeLine(FramePoint currentCapturePointPosition, FramePoint desiredCapturePointPosition, FrameVector desiredCapturePointVelocity)
   {
      currentCapturePointPosition.checkReferenceFrameMatch(desiredCapturePointPosition);
      desiredCapturePointVelocity.checkReferenceFrameMatch(desiredCapturePointPosition);

      double desiredCapturePointVelocityMagnitude = desiredCapturePointVelocity.length();

      if (desiredCapturePointVelocityMagnitude == 0.0)
      {
         return Double.NaN;
      }
      else
      {
         double normalizedCapturePointVelocityX = desiredCapturePointVelocity.getX() / desiredCapturePointVelocityMagnitude;
         double normalizedCapturePointVelocityY = desiredCapturePointVelocity.getY() / desiredCapturePointVelocityMagnitude;
         double normalizedCapturePointVelocityZ = desiredCapturePointVelocity.getZ() / desiredCapturePointVelocityMagnitude;

         double capturePointErrorX = currentCapturePointPosition.getX() - desiredCapturePointPosition.getX();
         double capturePointErrorY = currentCapturePointPosition.getY() - desiredCapturePointPosition.getY();
         double capturePointErrorZ = currentCapturePointPosition.getZ() - desiredCapturePointPosition.getZ();

         return -(normalizedCapturePointVelocityX * capturePointErrorX + normalizedCapturePointVelocityY * capturePointErrorY + normalizedCapturePointVelocityZ * capturePointErrorZ);
      }
   }

   /**
    * Given a capture point trajectory line and the actual 
    * capture point position, project the current capture point 
    * onto the capture point trajectory line.
    * 
    * @param actualICP
    * @param capturePointTrajectoryLine
    * @param projectedCapturePoint
    */
   public static void computeCapturePointOnTrajectoryAndClosestToActualCapturePoint(FramePoint actualICP, FrameLine2d capturePointTrajectoryLine,
         FramePoint2d projectedCapturePoint)
   {
      projectedCapturePoint.set(actualICP.getX(), actualICP.getY());
      capturePointTrajectoryLine.orthogonalProjection(projectedCapturePoint);
   }
}
