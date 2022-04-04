package us.ihmc.commonWalkingControlModules.capturePoint.controller;

import static us.ihmc.graphicsDescription.appearance.YoAppearance.Purple;

import us.ihmc.commonWalkingControlModules.capturePoint.CapturePointTools;
import us.ihmc.commonWalkingControlModules.capturePoint.ICPControlGainsReadOnly;
import us.ihmc.commonWalkingControlModules.capturePoint.ParameterizedICPControlGains;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint2DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFrameVector2DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FrameConvexPolygon2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector2DReadOnly;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactLine2d;
import us.ihmc.robotics.time.ExecutionTimer;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameLine2D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoint2D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector2D;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

/**
 * HeuristicICPController controls the ICP using a few simple heuristics, including: a) Use a simple
 * proportional controller on the ICP error for a feedback term. b) Use the perfect CMP for a
 * feedforward term. c) If there is a large perpendicular error, ignore the feedforward term and
 * only do the feedback term. d) Project the unconstrained CoP answer into the foot along the Vector
 * from the unconstrained CMP to the ICP, but do not project too far into the foot, and also do not
 * project closer to the ICP than a certain threshold.
 **/
public class HeuristicICPController implements ICPControllerInterface
{
   private static final boolean VISUALIZE = true;

   private final String yoNamePrefix = "heuristic";
   private final YoRegistry registry = new YoRegistry("HeuristicICPController");
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   // Control Parameters:
   private final YoDouble pureFeedbackErrorThreshold = new YoDouble(yoNamePrefix + "PureFeedbackErrorThresh",
                                                                    "Amount of ICP error before feedforward terms are ignored.",
                                                                    registry);
   private final YoDouble minICPPushDelta = new YoDouble(yoNamePrefix
         + "MinICPPushDelta", "When projecting the CoP into the foot, make sure to not move the CMP any closer than this amount from the ICP", registry);
   private final YoDouble maxCoPProjectionInside = new YoDouble(yoNamePrefix + "MaxCoPProjectionInside",
                                                                "When projecting the CoP into the foot, move up to this far from the edge if possible",
                                                                registry);

   private final ICPControlGainsReadOnly feedbackGains;
   //   private final BooleanProvider useCMPFeedback;
   //   private final BooleanProvider useAngularMomentum;

   // Algorithm Inputs:
   private final FramePoint2D desiredICP = new FramePoint2D();
   private final FrameVector2D desiredICPVelocity = new FrameVector2D();
   private final FrameVector2D parallelDirection = new FrameVector2D();
   private final FrameVector2D perpDirection = new FrameVector2D();
   private final FrameVector2D perfectCMPOffset = new FrameVector2D();
   private final FramePoint2D currentICP = new FramePoint2D();
   private final FramePoint2D currentCoMPosition = new FramePoint2D();
   private final FrameVector2D currentCoMVelocity = new FrameVector2D();

   final YoFramePoint2D perfectCoP = new YoFramePoint2D(yoNamePrefix + "PerfectCoP", worldFrame, registry);
   final YoFramePoint2D perfectCMP = new YoFramePoint2D(yoNamePrefix + "PerfectCMP", worldFrame, registry);

   // Feedback control computations before projection (unconstrained)
   final YoFrameVector2D icpError = new YoFrameVector2D(yoNamePrefix + "ICPError", "", worldFrame, registry);
   private final YoDouble icpErrorMagnitude = new YoDouble(yoNamePrefix + "ICPErrorMagnitude", registry);
   private final YoDouble icpParallelError = new YoDouble(yoNamePrefix + "ICPParallelError", registry);
   private final YoDouble icpPerpError = new YoDouble(yoNamePrefix + "ICPPerpError", registry);

   private final YoFrameVector2D pureFeedforwardControl = new YoFrameVector2D(yoNamePrefix + "PureFeedforwardControl", "", worldFrame, registry);
   private final YoDouble pureFeedforwardMagnitude = new YoDouble(yoNamePrefix + "PureFeedforwardMagnitude", registry);

   private final YoFrameVector2D pureFeedbackControl = new YoFrameVector2D(yoNamePrefix + "PureFeedbackControl", "", worldFrame, registry);
   private final YoDouble pureFeedbackMagnitude = new YoDouble(yoNamePrefix + "PureFeedbackMagnitude", registry);

   private final YoDouble feedbackFeedforwardAlpha = new YoDouble(yoNamePrefix + "FeedbackFeedforwardAlpha", registry);

   private final YoDouble icpParallelFeedback = new YoDouble(yoNamePrefix + "ICPParallelFeedback", registry);
   private final YoDouble icpPerpFeedback = new YoDouble(yoNamePrefix + "ICPPerpFeedback", registry);

   private final YoFrameVector2D unconstrainedFeedback = new YoFrameVector2D(yoNamePrefix + "UnconstrainedFeedback", worldFrame, registry);
   private final YoFramePoint2D unconstrainedFeedbackCMP = new YoFramePoint2D(yoNamePrefix + "UnconstrainedFeedbackCMP", worldFrame, registry);
   private final YoFramePoint2D unconstrainedFeedbackCoP = new YoFramePoint2D(yoNamePrefix + "UnconstrainedFeedbackCoP", worldFrame, registry);

   // Projection computation variables:
   private final YoDouble yoDotProduct = new YoDouble(yoNamePrefix + "DotProduct", registry);
   private final YoDouble adjustedICP = new YoDouble(yoNamePrefix + "AdjustedICP", registry);
   private final YoDouble firstIntersection = new YoDouble(yoNamePrefix + "FirstIntersection", registry);
   private final YoDouble secondIntersection = new YoDouble(yoNamePrefix + "SecondIntersection", registry);
   private final YoDouble firstPerfect = new YoDouble(yoNamePrefix + "FirstPerfect", registry);
   private final YoDouble secondPerfect = new YoDouble(yoNamePrefix + "SecondPerfect", registry);
   private final YoDouble adjustmentDistance = new YoDouble(yoNamePrefix + "AdjustmentDistance", registry);

   private final YoFrameVector2D projectionVector = new YoFrameVector2D(yoNamePrefix + "ProjectionVector", worldFrame, registry);
   private final YoFrameLine2D projectionLine = new YoFrameLine2D(yoNamePrefix + "ProjectionLine", worldFrame, registry);

   private final YoFramePoint2D firstProjectionIntersection = new YoFramePoint2D(yoNamePrefix + "FirstIntersection", worldFrame, registry);
   private final YoFramePoint2D secondProjectionIntersection = new YoFramePoint2D(yoNamePrefix + "SecondIntersection", worldFrame, registry);
   private final YoFramePoint2D closestPointWithProjectionLine = new YoFramePoint2D(yoNamePrefix + "ClosestPointWithProjectionLine", worldFrame, registry);

   private final YoFramePoint2D icpProjection = new YoFramePoint2D(yoNamePrefix + "icpProjection", worldFrame, registry);
//   private final YoFramePoint2D coPProjection = new YoFramePoint2D(yoNamePrefix + "CoPProjection", worldFrame, registry);

   // Outputs:
   private final YoFramePoint2D feedbackCoP = new YoFramePoint2D(yoNamePrefix + "FeedbackCoPSolution", worldFrame, registry);
   private final YoFramePoint2D feedbackCMP = new YoFramePoint2D(yoNamePrefix + "FeedbackCMPSolution", worldFrame, registry);
   private final YoFrameVector2D expectedControlICPVelocity = new YoFrameVector2D(yoNamePrefix + "ExpectedControlICPVelocity", worldFrame, registry);

   private final YoFrameVector2D residualError = new YoFrameVector2D(yoNamePrefix + "ResidualDynamicsError", worldFrame, registry);

   private final ExecutionTimer controllerTimer = new ExecutionTimer("icpControllerTimer", 0.5, registry);

   //   private final double controlDT;
   //   private final double controlDTSquare;

   private final FrameVector2D tempVector = new FrameVector2D();
   private final FrameVector2D tempVectorTwo = new FrameVector2D();
   
   private final FrameVector2D bestPerpendicularVector = new FrameVector2D();


   public HeuristicICPController(ICPControllerParameters icpControllerParameters, 
                                 double controlDT,
                                 YoRegistry parentRegistry,
                                 YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      
      pureFeedbackErrorThreshold.set(icpControllerParameters.getPureFeedbackErrorThreshold());
      minICPPushDelta.set(icpControllerParameters.getMinICPPushDelta());
      maxCoPProjectionInside.set(icpControllerParameters.getMaxCoPProjectionInside());

      
      pureFeedbackErrorThreshold.set(0.06);
      minICPPushDelta.set(0.05);
      maxCoPProjectionInside.set(0.04);

//      this.controlDT = controlDT;
//      this.controlDTSquare = controlDT * controlDT;

//      useCMPFeedback = new BooleanParameter(yoNamePrefix + "UseCMPFeedback", registry, icpControllerParameters.useCMPFeedback());
//      useAngularMomentum = new BooleanParameter(yoNamePrefix + "UseAngularMomentum", registry, icpControllerParameters.useAngularMomentum());

      feedbackGains = new ParameterizedICPControlGains("", icpControllerParameters.getICPFeedbackGains(), registry);

      if (yoGraphicsListRegistry != null)
         setupVisualizers(yoGraphicsListRegistry);

      parentRegistry.addChild(registry);
   }

   public ICPControlGainsReadOnly getFeedbackGains()
   {
      return feedbackGains;
   }

   private final FrameVector2D desiredCMPOffsetToThrowAway = new FrameVector2D();

   @Override
   public void compute(FrameConvexPolygon2DReadOnly supportPolygonInWorld,
                       FramePoint2DReadOnly desiredICP,
                       FrameVector2DReadOnly desiredICPVelocity,
                       FramePoint2DReadOnly perfectCoP,
                       FramePoint2DReadOnly currentICP,
                       FramePoint2DReadOnly currentCoMPosition,
                       double omega0)
   {
      desiredCMPOffsetToThrowAway.setToZero(worldFrame);
      compute(supportPolygonInWorld, desiredICP, desiredICPVelocity, perfectCoP, desiredCMPOffsetToThrowAway, currentICP, currentCoMPosition, omega0);
   }

   @Override
   public void compute(FrameConvexPolygon2DReadOnly supportPolygonInWorld,
                       FramePoint2DReadOnly desiredICP,
                       FrameVector2DReadOnly desiredICPVelocity,
                       FramePoint2DReadOnly perfectCoP,
                       FrameVector2DReadOnly perfectCMPOffset,
                       FramePoint2DReadOnly currentICP,
                       FramePoint2DReadOnly currentCoMPosition,
                       double omega0)
   {
      //TODO: Try working in velocity and angle space instead of xy space.
      // Have a gain on the velocity based on whether leading or lagging. 
      // Then have a gain on the angle or something to push towards the ICP direction line.
      // This should reduce the dependence on the perfect CoP/CMP, which can throw things off
      // when there is a big error. And also should not cause so much outside to project fixes.
      // Especially if you limit the amount of velocity increase you can have.
      controllerTimer.startMeasurement();

      this.desiredICP.setMatchingFrame(desiredICP);
      this.desiredICPVelocity.setMatchingFrame(desiredICPVelocity);
      this.perfectCMPOffset.setMatchingFrame(perfectCMPOffset);
      this.currentICP.setMatchingFrame(currentICP);
      this.currentCoMPosition.setMatchingFrame(currentCoMPosition);

      CapturePointTools.computeCenterOfMassVelocity(currentCoMPosition, currentICP, omega0, currentCoMVelocity);

      this.perfectCoP.setMatchingFrame(perfectCoP);
      this.perfectCMP.add(this.perfectCoP, this.perfectCMPOffset);

      this.icpError.sub(currentICP, desiredICP);
      this.icpErrorMagnitude.set(icpError.length());

      this.parallelDirection.set(this.desiredICPVelocity);

      if (parallelDirection.lengthSquared() > 1e-7)
      {
         parallelDirection.normalize();
         perpDirection.set(-parallelDirection.getY(), parallelDirection.getX());

         icpParallelError.set(icpError.dot(parallelDirection));
         icpPerpError.set(icpError.dot(perpDirection));
      }
      else
      {
         parallelDirection.setToNaN();

         perpDirection.set(icpError);
         perpDirection.normalize();
         if (perpDirection.containsNaN())
         {
            perpDirection.set(1.0, 0.0);
         }

         icpPerpError.set(icpError.length());
         icpParallelError.set(0.0);
      }

      icpParallelFeedback.set(icpParallelError.getValue());
      icpParallelFeedback.mul(feedbackGains.getKpParallelToMotion());

      icpPerpFeedback.set(icpPerpError.getValue());
      icpPerpFeedback.mul(feedbackGains.getKpOrthogonalToMotion());

      pureFeedbackControl.set(icpError);
      pureFeedbackControl.scale(feedbackGains.getKpOrthogonalToMotion());
      pureFeedbackMagnitude.set(pureFeedbackControl.length());
      //      pureFeedbackControl.clipToMaxLength(maxLength);

      pureFeedforwardControl.sub(perfectCMP, desiredICP);
      pureFeedforwardMagnitude.set(pureFeedforwardControl.length());

      //      if (icpErrorMagnitude.getValue() >= pureFeedbackThreshError.getValue())
      if (Math.abs(icpPerpError.getValue()) >= pureFeedbackErrorThreshold.getValue())
      {
         feedbackFeedforwardAlpha.set(1.0);
      }
      else
      {
         //         feedbackFeedforwardAlpha.set(icpErrorMagnitude.getValue()/pureFeedbackThreshError.getValue());
         double perpErrorAdjusted = Math.abs(icpPerpError.getValue()) - pureFeedbackErrorThreshold.getValue() / 2.0;
         if (perpErrorAdjusted < 0.0)
            perpErrorAdjusted = 0.0;

         feedbackFeedforwardAlpha.set(perpErrorAdjusted / (pureFeedbackErrorThreshold.getValue() / 2.0));
      }

      limitAbsoluteValue(icpParallelFeedback, feedbackGains.getFeedbackPartMaxValueParallelToMotion());
      limitAbsoluteValue(icpPerpFeedback, feedbackGains.getFeedbackPartMaxValueOrthogonalToMotion());

      //      unconstrainedFeedback.interpolate(pureFeedforwardControl, pureFeedbackControl, feedbackFeedforwardAlpha.getValue());

      unconstrainedFeedback.set(pureFeedforwardControl);
      unconstrainedFeedback.scale(1.0 - feedbackFeedforwardAlpha.getValue());
      unconstrainedFeedback.add(pureFeedbackControl);

      unconstrainedFeedbackCMP.set(currentICP);
      unconstrainedFeedbackCMP.add(unconstrainedFeedback);

      //      unconstrainedFeedback.setToZero();
      //      if (!parallelDirection.containsNaN())
      //      {
      //         tempVector.set(parallelDirection);
      //         tempVector.scale(icpParallelFeedback.getValue());
      //         unconstrainedFeedback.add(tempVector);
      //      }
      //
      //      if (!perpDirection.containsNaN())
      //      {
      //         tempVector.set(perpDirection);
      //         tempVector.scale(icpPerpFeedback.getValue());
      //         unconstrainedFeedback.add(tempVector);
      //      }
      //
      //      unconstrainedFeedbackCMP.add(perfectCoP, perfectCMPOffset);
      //      unconstrainedFeedbackCMP.add(icpError);
      //      unconstrainedFeedbackCMP.add(unconstrainedFeedback);

      unconstrainedFeedbackCoP.set(unconstrainedFeedbackCMP);
      unconstrainedFeedbackCoP.sub(perfectCMPOffset);

      projectTowardsMidpoint(supportPolygonInWorld);

      feedbackCMP.set(feedbackCoP);
      feedbackCMP.add(perfectCMPOffset);

      expectedControlICPVelocity.sub(currentICP, feedbackCMP);
      expectedControlICPVelocity.scale(omega0);

      controllerTimer.stopMeasurement();
   }

   private void projectTowardsMidpoint(FrameConvexPolygon2DReadOnly supportPolygonInWorld)
   {
      // Project the CoP onto the support polygon, using a projection vector.
//      coPProjection.setToNaN();
      yoDotProduct.setToNaN();
      icpProjection.setToNaN();
      firstProjectionIntersection.setToNaN();
      secondProjectionIntersection.setToNaN();
      closestPointWithProjectionLine.setToNaN();
      projectionVector.setToNaN();
      projectionLine.setToNaN();

      // Determine the closest point, intersection point, etc.
      //Compute the projection vector and projection line:
      projectionVector.set(currentICP);
      projectionVector.sub(unconstrainedFeedbackCMP);

      adjustedICP.set(projectionVector.length());

      // If the projection vector, and hence the error is small and already inside the foot, then do not project at all.
      // But if it is small and not inside the foot, then orthogonally project the CoP into the foot
      if (adjustedICP.getValue() < 0.002)
      {
         if (supportPolygonInWorld.isPointInside(unconstrainedFeedbackCoP))
         {
            feedbackCoP.set(unconstrainedFeedbackCoP);
            return;
         }
         else
         {
            supportPolygonInWorld.orthogonalProjection(unconstrainedFeedbackCoP);
            feedbackCoP.set(unconstrainedFeedbackCoP);
            return;
         }
      }

      projectionVector.normalize();
      projectionLine.set(unconstrainedFeedbackCoP, projectionVector);

      supportPolygonInWorld.intersectionWith(projectionLine, firstProjectionIntersection, secondProjectionIntersection);

      if (firstProjectionIntersection.containsNaN() || (secondProjectionIntersection.containsNaN()))
      {
         projectWhenProjectionLineDoesNotIntersectFoot(supportPolygonInWorld);
         return;
      }

      tempVector.set(firstProjectionIntersection);
      tempVector.sub(unconstrainedFeedbackCoP);
      firstIntersection.set(tempVector.dot(projectionVector));

      tempVector.set(secondProjectionIntersection);
      tempVector.sub(unconstrainedFeedbackCoP);
      secondIntersection.set(tempVector.dot(projectionVector));

      if (firstIntersection.getValue() > secondIntersection.getValue())
      {
         double temp = firstIntersection.getValue();
         firstIntersection.set(secondIntersection.getValue());
         secondIntersection.set(temp);
         
         tempVector.set(firstProjectionIntersection);
         firstProjectionIntersection.set(secondProjectionIntersection);
         secondProjectionIntersection.set(tempVector);
      }

      //TODO: Decide whether or not to use the perfect points in the projection.
      //TODO: Right now we are just setting them to the intersections.
      firstPerfect.set(firstIntersection.getValue());
      secondPerfect.set(secondIntersection.getValue());

      //      tempVector.set(perfectCoP);
      //      tempVector.sub(unconstrainedFeedbackCMP);
      //      firstPerfect.set(tempVector.dot(projectionVector));
      //      secondPerfect.set(firstPerfect.getValue());
      //
      //      firstPerfect.sub(0.01);
      //      secondPerfect.add(0.01);

      adjustmentDistance.set(HeuristicICPControllerHelper.computeAdjustmentDistance(adjustedICP.getValue(),
                                                                                    firstIntersection.getValue(),
                                                                                    secondIntersection.getValue(),
                                                                                    firstPerfect.getValue(),
                                                                                    secondPerfect.getValue(),
                                                                                    minICPPushDelta.getValue(),
                                                                                    maxCoPProjectionInside.getValue()));

      // If the adjustment distance is greater than the adjustedICP, then that means you are pushing directly backwards on the ICP. 
      // Instead, in that case, do a smart projection.
      
      if (adjustmentDistance.getValue() > adjustedICP.getValue())
      {
         projectWhenProjectionLineDoesNotIntersectFoot(supportPolygonInWorld);
         return;
      }
      
      tempVector.set(projectionVector);
      tempVector.scale(adjustmentDistance.getValue());
      feedbackCoP.set(unconstrainedFeedbackCoP);
      feedbackCoP.add(tempVector);
   }

   private void projectWhenProjectionLineDoesNotIntersectFoot(FrameConvexPolygon2DReadOnly supportPolygonInWorld)
   {
      // Project the ICP onto the foot. Then figure out which direction is best to move perpendicular from there to try to help things out.
      // Then move in that perpendicular direction a little bit more than the distance from the foot to the ICP.
      // (Note this distance if made infinite, would just find the line of sight point. But the line of sight point can be too far if it is
      // not helping much. By going just a little more than 1, you make sure you do not fight the badness that comes after 45 degrees, 
      // where you increase the ICP expected velocity a lot, just to get a little bit more angle.)
      // Then project that point back into the foot.
      
      icpProjection.set(currentICP);
      supportPolygonInWorld.orthogonalProjection(icpProjection);

      tempVector.set(currentICP);
      tempVector.sub(unconstrainedFeedbackCoP);
      tempVector.normalize();
      if (tempVector.containsNaN())
      {
         feedbackCoP.set(icpProjection);
         return;
      }

      bestPerpendicularVector.sub(currentICP, icpProjection);
      double distanceFromProjectionToICP = bestPerpendicularVector.length();

      if (distanceFromProjectionToICP < 0.001)
      {
         feedbackCoP.set(icpProjection);
         return;
      }

      bestPerpendicularVector.scale(1.0/distanceFromProjectionToICP);
      bestPerpendicularVector.set(-bestPerpendicularVector.getY(), bestPerpendicularVector.getX());

      double dotProduct = tempVector.dot(bestPerpendicularVector);

      if (dotProduct > 0.0)
         bestPerpendicularVector.scale(-1.0);
      else
      {
         dotProduct = -1.0 * dotProduct;
      }
      yoDotProduct.set(dotProduct);

      double scaleDistanceFromICP = 1.5;
      double addDistanceToPerpendicular = 0.04;
      
      double amountToMoveInPerpendicularDirection = (scaleDistanceFromICP * distanceFromProjectionToICP) + addDistanceToPerpendicular;
      amountToMoveInPerpendicularDirection = amountToMoveInPerpendicularDirection * dotProduct;

      bestPerpendicularVector.scale(amountToMoveInPerpendicularDirection);
      feedbackCoP.set(icpProjection);
      feedbackCoP.add(bestPerpendicularVector);

      supportPolygonInWorld.orthogonalProjection(feedbackCoP);
   }

   private FramePoint2DReadOnly computeBestLineOfSIghtPoint(FrameConvexPolygon2DReadOnly supportPolygonInWorld)
   {
      int lineOfSightStartIndex = supportPolygonInWorld.lineOfSightStartIndex(currentICP);
      int lineOfSightEndIndex = supportPolygonInWorld.lineOfSightEndIndex(currentICP);

      FramePoint2DReadOnly bestLineOfSightPoint = null;
      if ((lineOfSightStartIndex >= 0) && (lineOfSightEndIndex >=0))
      {
         FramePoint2DReadOnly lineOfSightVertexOne = supportPolygonInWorld.getVertex(lineOfSightStartIndex);
         FramePoint2DReadOnly lineOfSightVertexTwo = supportPolygonInWorld.getVertex(lineOfSightEndIndex);

         tempVector.set(currentICP);
         tempVector.sub(unconstrainedFeedbackCoP);

         tempVectorTwo.set(currentICP);
         tempVectorTwo.sub(lineOfSightVertexOne);
         tempVectorTwo.normalize();

         double dotOne = tempVectorTwo.dot(tempVector);

         tempVectorTwo.set(currentICP);
         tempVectorTwo.sub(lineOfSightVertexTwo);
         tempVectorTwo.normalize();

         double dotTwo = tempVectorTwo.dot(tempVector);

         if (dotOne > dotTwo)
         {
            bestLineOfSightPoint = lineOfSightVertexOne;
         }
         else
         {
            bestLineOfSightPoint = lineOfSightVertexTwo;
         }
      }
      return bestLineOfSightPoint;
   }

   private void limitAbsoluteValue(YoDouble yoDouble, double maxAbsoluteValue)
   {
      if (yoDouble.getValue() > maxAbsoluteValue)
         yoDouble.set(maxAbsoluteValue);
      else if (yoDouble.getValue() < -maxAbsoluteValue)
         yoDouble.set(-maxAbsoluteValue);
   }

   private void setupVisualizers(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      ArtifactList artifactList = new ArtifactList(getClass().getSimpleName());

      YoGraphicPosition feedbackCoPViz = new YoGraphicPosition(yoNamePrefix + "FeedbackCoP", this.feedbackCoP, 0.005, YoAppearance.Darkorange(), YoGraphicPosition.GraphicType.BALL_WITH_CROSS);

      YoGraphicPosition unconstrainedFeedbackCMPViz = new YoGraphicPosition(yoNamePrefix + "UnconstrainedFeedbackCMP", this.unconstrainedFeedbackCMP, 0.008, Purple(), GraphicType.BALL_WITH_CROSS);

      YoGraphicPosition unconstrainedFeedbackCoPViz = new YoGraphicPosition(yoNamePrefix
            + "UnconstrainedFeedbackCoP", this.unconstrainedFeedbackCoP, 0.004, YoAppearance.Green(), GraphicType.BALL_WITH_ROTATED_CROSS);

      //TODO: Figure out a viz that works with the logger.
      YoArtifactLine2d projectionLineViz = new YoArtifactLine2d(yoNamePrefix + "ProjectionLine", this.projectionLine, YoAppearance.Aqua().getAwtColor());

      YoGraphicPosition firstIntersectionViz = new YoGraphicPosition(yoNamePrefix + "FirstIntersection", this.firstProjectionIntersection, 0.004, YoAppearance.Green(), GraphicType.SOLID_BALL);

      YoGraphicPosition secondIntersectionViz = new YoGraphicPosition(yoNamePrefix + "SecondIntersection", this.secondProjectionIntersection, 0.004, YoAppearance.Green(), GraphicType.SOLID_BALL);

      YoGraphicPosition closestPointWithProjectionLineViz = new YoGraphicPosition(yoNamePrefix
            + "ClosestToProjectionLine", this.closestPointWithProjectionLine, 0.003, YoAppearance.Green(), GraphicType.SOLID_BALL);

//      YoGraphicPosition copProjectionViz = new YoGraphicPosition(yoNamePrefix + "CoPProjection", this.coPProjection, 0.002, YoAppearance.Green(), GraphicType.SOLID_BALL);
      YoGraphicPosition icpProjectionViz = new YoGraphicPosition(yoNamePrefix + "ICPProjection", this.icpProjection, 0.003, YoAppearance.Purple(), GraphicType.BALL);

      artifactList.add(feedbackCoPViz.createArtifact());
      artifactList.add(unconstrainedFeedbackCMPViz.createArtifact());
      artifactList.add(unconstrainedFeedbackCoPViz.createArtifact());
      //      artifactList.add(projectionLineViz);

      artifactList.add(firstIntersectionViz.createArtifact());
      artifactList.add(secondIntersectionViz.createArtifact());
      artifactList.add(closestPointWithProjectionLineViz.createArtifact());
//      artifactList.add(copProjectionViz.createArtifact());
      artifactList.add(icpProjectionViz.createArtifact());

      artifactList.setVisible(VISUALIZE);

      yoGraphicsListRegistry.registerArtifactList(artifactList);
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void setKeepCoPInsideSupportPolygon(boolean keepCoPInsideSupportPolygon)
   {
   }

   @Override
   public void getDesiredCMP(FixedFramePoint2DBasics desiredCMPToPack)
   {
      desiredCMPToPack.set(feedbackCMP);
   }

   @Override
   public void getDesiredCoP(FixedFramePoint2DBasics desiredCoPToPack)
   {
      desiredCoPToPack.set(feedbackCoP);
   }

   @Override
   public void getExpectedControlICPVelocity(FixedFrameVector2DBasics expectedControlICPVelocityToPack)
   {
      expectedControlICPVelocityToPack.set(expectedControlICPVelocity);
   }

   @Override
   public boolean useAngularMomentum()
   {
      return false;
   }

   @Override
   public FrameVector2DReadOnly getResidualError()
   {
      return residualError;
   }

}
