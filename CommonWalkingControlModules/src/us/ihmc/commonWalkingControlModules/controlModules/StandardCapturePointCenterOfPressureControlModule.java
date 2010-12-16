package us.ihmc.commonWalkingControlModules.controlModules;


import java.awt.Color;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.SideDependentList;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.captureRegion.CapturePointCalculatorInterface;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.CapturePointCenterOfPressureControlModule;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoAppearance;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.YoFrameLine2dArtifact;
import com.yobotics.simulationconstructionset.plotting.YoFrameLineSegment2dArtifact;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLine2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLineSegment2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;


public class StandardCapturePointCenterOfPressureControlModule implements CapturePointCenterOfPressureControlModule
{
   private final CommonWalkingReferenceFrames yoboticsBipedReferenceFrames;
   private final double controlDT;

   private final YoVariableRegistry registry = new YoVariableRegistry("CapturePointCenterOfPressureController");

   private final BooleanYoVariable turnOffCaptureControl = new BooleanYoVariable("turnOffCaptureControl", registry);

   {
      turnOffCaptureControl.set(false);
   }

   private final BooleanYoVariable singleSupportWasPreviousTick = new BooleanYoVariable("singleSupportWasPreviousTick", registry);

   private final BooleanYoVariable keepInsideFootSingleSupport = new BooleanYoVariable("keepInsideFootSingleSupport",
                                                                    "If true, just control the Capture Point to stay inside the foot the best you can.",
                                                                    registry);


   private final DoubleYoVariable kCaptureX = new DoubleYoVariable("K_capture_x", registry);
   private final DoubleYoVariable kCaptureY = new DoubleYoVariable("K_capture_y", registry);

   private final DoubleYoVariable kCaptureGuide = new DoubleYoVariable("K_capture_guide", registry);

   private final BooleanYoVariable capturePointInsideFootPolygon = new BooleanYoVariable("capturePointInsideFootPolygon", registry);
   private final BooleanYoVariable foundIntersections = new BooleanYoVariable("foundIntersections", registry);
   private final DoubleYoVariable percentToFarEdgeOfFoot = new DoubleYoVariable("percentToFarEdgeOfFoot", registry);

// private final YoVariable B_capture_x = new YoVariable("B_capture_x", registry);
// private final YoVariable B_capture_y = new YoVariable("B_capture_y", registry);

   private final DoubleYoVariable unfilteredXControl = new DoubleYoVariable("unfilteredXControl", registry);
   private final DoubleYoVariable unfilteredYControl = new DoubleYoVariable("unfilteredYControl", registry);

// private final InfiniteImpulseResponseFilteredYoVariable highPassYControl = new InfiniteImpulseResponseFilteredYoVariable("highPassYControl", 2, 2, registry);
// private final InfiniteImpulseResponseFilteredYoVariable yCaptureControl = new InfiniteImpulseResponseFilteredYoVariable("yCaptureControl", 2, 2, registry);
// private final YoVariable saturationYControl = new YoVariable("saturationYControl", registry);

   private final DoubleYoVariable alphaCaptureControl = new DoubleYoVariable("alphaCaptureControl", registry);
   private final AlphaFilteredYoVariable xCaptureControl = new AlphaFilteredYoVariable("xCaptureControl", registry, alphaCaptureControl);
   private final AlphaFilteredYoVariable yCaptureControl = new AlphaFilteredYoVariable("yCaptureControl", registry, alphaCaptureControl);


// private final YoVariable captureDeadZone = new YoVariable("captureDeadZone", registry);
// private final YoVariable maxCenterOfPressureRate = new YoVariable("maxCenterOfPressureRate", registry);

// private final RateLimitedYoVariable xCaptureControl = new RateLimitedYoVariable("xCaptureControl", registry, maxCenterOfPressureRate, YoboticsBipedEmbeddedMain.CONTROLLER_DT);

// private final InfiniteImpulseResponseFilteredYoVariable lowPassXYDot = new InfiniteImpulseResponseFilteredYoVariable("lowPassXYDot", 4, 4, registry);

   private final YoFramePoint centerOfPressureDesiredWorld, centerOfPressureDesiredMidFeet, centerOfPressureDesiredLeftAnkleZUp,
                              centerOfPressureDesiredRightAnkleZUp;

   private final SideDependentList<YoFramePoint> centerOfPressureDesiredAnkleZUp;

   private final DynamicGraphicPosition centerOfPressureDesiredWorldGraphicPosition;

   private final YoFrameLineSegment2d guideLineWorld;
   private final YoFrameLine2d parallelLineWorld;

// private final YoFramePoint[] footCapturePointDesired = new YoFramePoint[]{capturePointDesiredLeftAnkleRoll, capturePointDesiredRightAnkleRoll};
//   private final YoFramePoint[] footCenterOfPressureDesired;

   // Reference frames:
   ReferenceFrame bodyZUp, midFeetZUp, world;

   public StandardCapturePointCenterOfPressureControlModule(double controlDT, CommonWalkingReferenceFrames referenceFrames, YoVariableRegistry yoVariableRegistry,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.controlDT = controlDT;
      this.yoboticsBipedReferenceFrames = referenceFrames;

      keepInsideFootSingleSupport.set(false);

      bodyZUp = yoboticsBipedReferenceFrames.getABodyAttachedZUpFrame();
      midFeetZUp = yoboticsBipedReferenceFrames.getMidFeetZUpFrame();
      world = ReferenceFrame.getWorldFrame();

      guideLineWorld = new YoFrameLineSegment2d("guideLine", "", world, registry);
      parallelLineWorld = new YoFrameLine2d("parallelLine", "", world, registry);

      centerOfPressureDesiredWorld = new YoFramePoint("copDesWorld", "", world, registry);
      centerOfPressureDesiredMidFeet = new YoFramePoint("copDesMidfeet", "", midFeetZUp, registry);
      centerOfPressureDesiredLeftAnkleZUp = new YoFramePoint("copDesLaZUp", "", yoboticsBipedReferenceFrames.getAnkleZUpReferenceFrames().get(RobotSide.LEFT),
              registry);
      centerOfPressureDesiredRightAnkleZUp = new YoFramePoint("copDesRaZUp", "",
              yoboticsBipedReferenceFrames.getAnkleZUpReferenceFrames().get(RobotSide.RIGHT), registry);

      centerOfPressureDesiredAnkleZUp = new SideDependentList<YoFramePoint>(centerOfPressureDesiredLeftAnkleZUp, centerOfPressureDesiredRightAnkleZUp);

      if (dynamicGraphicObjectsListRegistry != null)
      {
         DynamicGraphicObjectsList dynamicGraphicObjectList = new DynamicGraphicObjectsList("CapturePointController");

         centerOfPressureDesiredWorldGraphicPosition = new DynamicGraphicPosition("Desired Center of Pressure", centerOfPressureDesiredWorld, 0.012,
                 YoAppearance.Gray(), DynamicGraphicPosition.GraphicType.CROSS);

         dynamicGraphicObjectList.add(centerOfPressureDesiredWorldGraphicPosition);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectList);

         ArtifactList artifactList = new ArtifactList("Capture Point CoP Control Module");

         artifactList.add(centerOfPressureDesiredWorldGraphicPosition.createArtifact());

         YoFrameLineSegment2dArtifact dynamicGraphicYoFrameLineSegment2dArtifact = new YoFrameLineSegment2dArtifact("Guide Line", guideLineWorld, Color.RED);
         artifactList.add(dynamicGraphicYoFrameLineSegment2dArtifact);

         YoFrameLine2dArtifact dynamicGraphicYoFrameLine2dArtifact = new YoFrameLine2dArtifact("Parallel Line", parallelLineWorld, Color.GREEN);
         artifactList.add(dynamicGraphicYoFrameLine2dArtifact);

         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }
      else
      {
         centerOfPressureDesiredWorldGraphicPosition = null;
      }

      if (yoVariableRegistry != null)    // && (VarListsToRegister.REGISTER_CAPTURE_POINT_CENTER_OF_PRESSURE_CONTROLLER))
      {
         yoVariableRegistry.addChild(registry);
      }

   }

   public void controlDoubleSupport(BipedSupportPolygons bipedSupportPolygons, CapturePointCalculatorInterface yoboticsBipedCapturePointCalculator,
         FramePoint desiredCapturePoint)
   {
      FramePoint currentCapturePoint = yoboticsBipedCapturePointCalculator.getCapturePointInFrame(desiredCapturePoint.getReferenceFrame());
      
      controlDoubleSupport(bipedSupportPolygons, currentCapturePoint, desiredCapturePoint, null, null, null);
   }

   public void controlDoubleSupport(BipedSupportPolygons bipedSupportPolygons, FramePoint currentCapturePoint, FramePoint desiredCapturePoint, FramePoint centerOfMassPositionInZUpFrame, FrameVector2d desiredVelocity, FrameVector2d currentVelocity)
   {
      // Hide the guideline and parallel line since not used in double support:
      guideLineWorld.setFrameLineSegment2d(null);
      parallelLineWorld.setFrameLine2d(null);

      // DesiredBalancePoint must be in a ZUp frame in order to ignore z. We really should use 2d points...
      if (!desiredCapturePoint.getReferenceFrame().isZupFrame())
      {
         throw new RuntimeException("desiredBalancePoint Not a ZUpFrame!!");
      }

//    FramePoint current = yoboticsBipedCapturePointCalculator.getCapturePointInFrame(desiredCapturePoint.getReferenceFrame());

//    lowPassXYDot.update(processedSensors.pd_y_body_zup.val);

//    FramePoint hijackedDesired = new FramePoint(desiredCapturePoint);
//    hijackedDesired.setX(0.05);
//    hijackedDesired.setY(0.0);

//    FrameVector control = new FrameVector(hijackedDesired);
      FrameVector control = new FrameVector(desiredCapturePoint);
      control.sub(currentCapturePoint);
      control.setX(control.getX() * kCaptureX.getDoubleValue());
      control.setY(control.getY() * kCaptureY.getDoubleValue());
      control.setZ(0.0);

      // Control from here on is in MidFeetZUpFrame:
      if (control.getReferenceFrame() != midFeetZUp)
      {
//       control = control.changeFrameCopy(bodyZUp);
         control = control.changeFrameCopy(midFeetZUp);
      }

//    unfilteredXControl.val = K_capture_x.val * (desiredBalancePoint.getX() - current.getX());
//    unfilteredYControl.val = K_capture_y.val * (desiredBalancePoint.getY() - current.getY());
//
//    highPassYControl.update(unfilteredYControl.val);
//    double saturatedHighPassYControl = MathTools.clipToMinMax(highPassYControl.val, -saturationYControl.val, saturationYControl.val);
//
//    xCaptureControl.update(unfilteredXControl.val);

      unfilteredXControl.set(control.getX());
      unfilteredYControl.set(control.getY());


      if (singleSupportWasPreviousTick.getBooleanValue())
      {
         xCaptureControl.reset();
         yCaptureControl.reset();

         singleSupportWasPreviousTick.set(false);
      }

      xCaptureControl.update(unfilteredXControl.getDoubleValue());
      yCaptureControl.update(unfilteredYControl.getDoubleValue());    // saturatedHighPassYControl);


      control.setX(xCaptureControl.getDoubleValue());
      control.setY(yCaptureControl.getDoubleValue());

      FramePoint centerOfPressureDesired = currentCapturePoint.changeFrameCopy(midFeetZUp);
      centerOfPressureDesired.sub(control);


      // Make sure inside the polygon, if not, project along the line to the desired capture point, (not orthogonal!)
      if (bipedSupportPolygons != null)
      {
         FramePoint2d centerOfPressureDesired2d = new FramePoint2d(centerOfPressureDesired.getReferenceFrame(), centerOfPressureDesired.getX(),
                                                     centerOfPressureDesired.getY());

         FrameConvexPolygon2d supportPolygon = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();

         // If feasible CoP is not inside the convex hull of the feet, project it into it.
         if (!supportPolygon.isPointInside(centerOfPressureDesired2d))
         {
            // supportPolygon.orthogonalProjection(centerOfPressureDesired2d);

            if (desiredCapturePoint.getReferenceFrame() != midFeetZUp)
            {
               desiredCapturePoint = desiredCapturePoint.changeFrameCopy(midFeetZUp);
            }

            FramePoint2d desiredCapturePoint2d = new FramePoint2d(desiredCapturePoint.getReferenceFrame(), desiredCapturePoint.getX(),
                                                    desiredCapturePoint.getY());
            FrameLineSegment2d desiredCaptureToDesiredCop = new FrameLineSegment2d(desiredCapturePoint2d, centerOfPressureDesired2d);

            // John Carff fixed this on 090625. Make sure his fix didn't break this line!!!
            // JEP 100826: Indeed it may have. When the desired capture point is outside of the support polygon, seems to get the wrong intersection!
            FramePoint2d[] intersections = supportPolygon.intersectionWith(desiredCaptureToDesiredCop);
            if (intersections != null)
            {
               FramePoint2d intersectionToUse;

               if (intersections.length == 1)
               {
                  intersectionToUse = intersections[0];
               }
               else
               {
                  double distanceSquaredToIntersection0 = centerOfPressureDesired2d.distanceSquared(intersections[0]);
                  double distanceSquaredToIntersection1 = centerOfPressureDesired2d.distanceSquared(intersections[1]);

                  if (distanceSquaredToIntersection0 <= distanceSquaredToIntersection1)
                     intersectionToUse = intersections[0];
                  else
                     intersectionToUse = intersections[1];
               }

               centerOfPressureDesired.setX(intersectionToUse.getX());
               centerOfPressureDesired.setY(intersectionToUse.getY());

               // Move in a little along the line:
               FrameVector2d frameVector2d = desiredCaptureToDesiredCop.getVectorCopy();
               frameVector2d.normalize();
               frameVector2d.scale(-0.002);    // Move toward desired capture by 2 mm to prevent some jerky behavior with VTPs..

               centerOfPressureDesired.setX(centerOfPressureDesired.getX() + frameVector2d.getX());
               centerOfPressureDesired.setY(centerOfPressureDesired.getY() + frameVector2d.getY());
            }
            else
            {
//               throw new RuntimeException("Shouldn't get here"); // TODO: turn on again
            }
         }
      }

      if (turnOffCaptureControl.getBooleanValue())
      {
         centerOfPressureDesiredMidFeet.setX(0.0);
         centerOfPressureDesiredMidFeet.setY(0.0);
         centerOfPressureDesiredMidFeet.setZ(0.0);
      }

      else
      {
         centerOfPressureDesiredMidFeet.set(centerOfPressureDesired);

//       centerOfPressureDesiredMidFeet.setX(yoboticsBipedCapturePointCalculator.capturePointMidfeet.getX() - unfilteredXControl.val); //xCaptureControl.val);
//       centerOfPressureDesiredMidFeet.setY(yoboticsBipedCapturePointCalculator.capturePointMidfeet.getY() - unfilteredYControl.val); //yCaptureControl.val);
//       centerOfPressureDesiredMidFeet.setZ(0.0);
      }

      centerOfPressureDesiredWorld.set(centerOfPressureDesiredMidFeet.getFramePointCopy().changeFrameCopy(world));
   }

   public void controlSingleSupport(FramePoint currentCapturePoint, FrameLineSegment2d guideLine, FramePoint desiredCapturePoint, RobotSide supportLeg,
           ReferenceFrame referenceFrame, BipedSupportPolygons supportPolygons)    // , double percentToFarEdgeOfFoot)
   {
      singleSupportWasPreviousTick.set(true);

      // Compute and get capture point:
//    FramePoint currentCapturePoint = yoboticsBipedCapturePointCalculator.getCapturePointInFrame(referenceFrame);

      // Transformation to 2d points: (should just use 2d throughout the program eventually)
      FramePoint2d currentCapturePoint2d = new FramePoint2d(currentCapturePoint.getReferenceFrame(), currentCapturePoint.getX(), currentCapturePoint.getY());

      if (guideLine != null)
         guideLineWorld.setFrameLineSegment2d(guideLine.changeFrameCopy(world));

      // Foot stuff:
      FrameConvexPolygon2d footPolygon = supportPolygons.getFootPolygonInAnkleZUp(supportLeg);
      capturePointInsideFootPolygon.set(supportPolygons.getFootPolygonInAnkleZUp(supportLeg).isPointInside(currentCapturePoint2d));

      // Calculate the desired center of pressure to control the Capture Point to the desiredCapturePoint:
      FramePoint2d desiredCenterOfPressure2d;
      if (((desiredCapturePoint != null) && capturePointInsideFootPolygon.getBooleanValue()) || (guideLine == null)
              || (keepInsideFootSingleSupport.getBooleanValue()))
      {
         if (desiredCapturePoint == null)
         {
            FramePoint2d guideLineStart = guideLine.getFirstEndPointCopy();
            desiredCapturePoint = new FramePoint(guideLineStart.getReferenceFrame(), guideLineStart.getX(), guideLineStart.getY(), 0.0);
         }

         // Calculate the error vector:
         FrameVector2d error = new FrameVector2d(currentCapturePoint2d);

         FramePoint2d desiredCapturePoint2d = new FramePoint2d(desiredCapturePoint.getReferenceFrame(), desiredCapturePoint.getX(), desiredCapturePoint.getY());
         error.sub(desiredCapturePoint2d);

         // Calculate desired center of pressure:
         desiredCenterOfPressure2d = new FramePoint2d(referenceFrame);
         desiredCenterOfPressure2d.setX(kCaptureX.getDoubleValue() * error.getX());
         desiredCenterOfPressure2d.setY(kCaptureY.getDoubleValue() * error.getY());
         desiredCenterOfPressure2d.add(supportPolygons.getSweetSpotCopy(supportLeg));
      }
      else
      {
         // Transform finalDesiredSwingTarget to foot ZUp:

         FramePoint2d captureProjectedOntoGuideLine = guideLine.orthogonalProjectionCopy(currentCapturePoint2d);

         FrameVector2d projectedToCurrent = new FrameVector2d(captureProjectedOntoGuideLine, currentCapturePoint2d);
         projectedToCurrent.scale(kCaptureGuide.getDoubleValue());

         FramePoint2d shiftedPoint = new FramePoint2d(captureProjectedOntoGuideLine);
         shiftedPoint.add(projectedToCurrent);

         FrameVector2d frameVector2d = guideLine.getVectorCopy();
         FrameLine2d shiftedParallelLine = new FrameLine2d(shiftedPoint, frameVector2d);

//       FrameLine2d shiftedParallelLine = new FrameLine2d(shiftedPoint, guideLine.getNormalizedFrameVector());

         parallelLineWorld.setFrameLine2d(shiftedParallelLine.changeFrameCopy(world));


//       FramePoint finalDesiredSwingTargetInFootZUp = finalDesiredSwingTarget.changeFrameCopy(footPolygon.getReferenceFrame());
//       FramePoint2d finalDesiredSwingTargetInFootZUp2d = new FramePoint2d(finalDesiredSwingTargetInFootZUp.getReferenceFrame(),
//                                                                          finalDesiredSwingTargetInFootZUp.getX(),
//                                                                          finalDesiredSwingTargetInFootZUp.getY());
//
//       FrameLine2d finalDesiredToCurrentCaptureLine = new FrameLine2d(finalDesiredSwingTargetInFootZUp2d, currentCapturePoint2d);
         FramePoint2d[] intersections = footPolygon.intersectionWith(shiftedParallelLine);
         if ((intersections == null) || (intersections.length == 0))
         {
            foundIntersections.set(false);
            desiredCenterOfPressure2d = footPolygon.getClosestVertexCopy(shiftedParallelLine);

            // Now shift it inside a tad bit to make sure foot doesn't rotate on a point:
            double percentToCentroid = 0.2;    // 0.1 +++JEP081126: To prevent slipping!
            FramePoint2d centroid = footPolygon.getCentroidCopy();
            centroid.scale(percentToCentroid);
            desiredCenterOfPressure2d.scale(1.0 - percentToCentroid);
            desiredCenterOfPressure2d.add(centroid);
         }
         else
         {
            foundIntersections.set(true);

            FrameLineSegment2d insideToOusideIntersections = new FrameLineSegment2d(intersections);

            // Make sure this line segment is in the opposite direction of the guide line. If not, flip it.
            if (insideToOusideIntersections.dotProduct(guideLine) > 0.0)
            {
               insideToOusideIntersections.flipDirection();
            }

            if (capturePointInsideFootPolygon.getBooleanValue())
            {
               percentToFarEdgeOfFoot.set(0.8);
            }
            else
            {
               // +++JEP 091103: This still needs some work...
               // Assume for now that the shifted point is not inside the foot...
               FramePoint2d insideIntersection = insideToOusideIntersections.getFirstEndPointCopy();
               double insideToShiftedPointDistance = insideIntersection.distance(shiftedPoint);

               double maxDistanceBeforeWeightAllInside = 0.15;
               double weightAllInsidePercentage = 0.15;    // 0.2;
               double weightAllOutsidePercentage = 0.5;    // 0.8;

               if (insideToShiftedPointDistance >= maxDistanceBeforeWeightAllInside)
                  percentToFarEdgeOfFoot.set(weightAllInsidePercentage);
               else
                  percentToFarEdgeOfFoot.set(weightAllOutsidePercentage
                                             + (weightAllInsidePercentage - weightAllOutsidePercentage)
                                               * (insideToShiftedPointDistance / maxDistanceBeforeWeightAllInside));
            }

            // Use percent to far edge, unless the capture point is to the inside of the foot...

            desiredCenterOfPressure2d = insideToOusideIntersections.pointBetweenEndPointsGivenParameter(percentToFarEdgeOfFoot.getDoubleValue());
         }
      }

      // Prevent the point from getting too far to the corner of the foot to prevent foot twist:
      // +++JEP081126
//    if (desiredCenterOfPressure2d.getX() > 0.08) desiredCenterOfPressure2d.setX(0.08);
//    if (desiredCenterOfPressure2d.getY() > 0.04) desiredCenterOfPressure2d.setY(0.04);
//    if (desiredCenterOfPressure2d.getY() < -0.04) desiredCenterOfPressure2d.setY(-0.04);


      // Set the desired center of pressure:
      if (turnOffCaptureControl.getBooleanValue())
      {
         centerOfPressureDesiredAnkleZUp.get(supportLeg).set(0.0, 0.0, 0.0);

      }
      else
      {
         centerOfPressureDesiredAnkleZUp.get(supportLeg).set(desiredCenterOfPressure2d.getX(), desiredCenterOfPressure2d.getY(), 0.0);
      }

      centerOfPressureDesiredWorld.set(centerOfPressureDesiredAnkleZUp.get(supportLeg).getFramePointCopy().changeFrameCopy(world));
   }

   public void setKCaptureX(double kx)
   {
      kCaptureX.set(kx);
   }

   public void setKCaptureY(double ky)
   {
      kCaptureY.set(ky);
   }

   public YoFramePoint getCenterOfPressureDesiredMidFeet()
   {
      return centerOfPressureDesiredMidFeet;
   }

   public YoFramePoint getCenterOfPressureDesiredAnkleZUp(RobotSide robotSide)
   {
      return centerOfPressureDesiredAnkleZUp.get(robotSide);
   }

 
   public void controlSingleSupport(FramePoint currentCapturePoint, FrameLineSegment2d guideLine, FramePoint desiredCapturePoint, RobotSide supportLeg,
         ReferenceFrame referenceFrame, BipedSupportPolygons supportPolygons, FramePoint centerOfMassPositionInZUpFrame, FrameVector2d desiredVelocity,
         FrameVector2d currentVelocity)
   {
      controlSingleSupport(currentCapturePoint, guideLine, desiredCapturePoint, supportLeg, referenceFrame, supportPolygons);
      
   }
   

   public void setParametersForM2V2()
   {
      alphaCaptureControl.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(8.84, controlDT));
      kCaptureX.set(4.0); // 3.0);    // 2.5;
      kCaptureY.set(4.0); // 3.0);    // 2.5;
      kCaptureGuide.set(2.0);    // 3.0; //2.5;
   }
}
