package us.ihmc.commonWalkingControlModules.controlModules.foot.toeOff;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.*;
import us.ihmc.euclid.referenceFrame.interfaces.FrameConvexPolygon2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameLine2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint2DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.robotics.EuclidCoreMissingTools;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ZUpFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoVariables.listener.YoVariableChangedListener;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class DynamicStateInspector
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final DynamicStateInspectorParameters parameters;

   private final YoDouble distanceOfCurrentICPPastHeel = new YoDouble("distanceOfCurrentICPPastHeel", registry);
   private final YoDouble distanceSquaredOfCurrentICPFromToe = new YoDouble("distanceSquaredOfCurrentICPFromToe", registry);
   private final YoDouble distanceSquaredOfDesiredICPFromToe = new YoDouble("distanceSquaredOfDesiredICPFromToe", registry);
   private final YoDouble distanceOfCurrentICPInside = new YoDouble("distanceOfCurrentICPInside", registry);
   private final YoDouble distanceOfDesiredICPInside = new YoDouble("distanceOfDesiredICPInside", registry);

   private final YoDouble currentOrthogonalDistanceToOutsideEdge = new YoDouble("currentOrthogonalDistanceToOutsideEdge", registry);
   private final YoDouble desiredOrthogonalDistanceToOutsideEdge = new YoDouble("desiredOrthogonalDistanceToOutsideEdge", registry);
   private final YoDouble errorDistanceToOutsideEdge = new YoDouble("errorDistanceToOutsideEdge", registry);

   private final YoDouble currentOrthogonalDistanceToInsideEdge = new YoDouble("currentOrthogonalDistanceToInsideEdge", registry);
   private final YoDouble desiredOrthogonalDistanceToInsideEdge = new YoDouble("desiredOrthogonalDistanceToInsideEdge", registry);
   private final YoDouble errorDistanceToInsideEdge = new YoDouble("errorDistanceToInsideEdge", registry);

   private final YoBoolean currentIcpIsPastTheHeel = new YoBoolean("CurrentICPIsPastTheHeel", registry);
   private final YoBoolean currentIcpIsFarEnoughFromTheToe = new YoBoolean("currentIcpIsFarEnoughFromTheToe", registry);
   private final YoBoolean desiredIcpIsFarEnoughFromTheToe = new YoBoolean("desiredIcpIsFarEnoughFromTheToe", registry);
   private final YoBoolean currentIcpIsFarEnoughInside = new YoBoolean("currentIcpIsFarEnoughInside", registry);
   private final YoBoolean desiredIcpIsFarEnoughInside = new YoBoolean("desiredIcpIsFarEnoughInside", registry);

   private final YoBoolean currentIcpIsFarEnoughInsideOutsideEdge = new YoBoolean("currentIcpIsFarEnoughInsideOutsideEdge", registry);
   private final YoBoolean desiredIcpIsFarEnoughInsideOutsideEdge = new YoBoolean("desiredIcpIsFarEnoughInsideOutsideEdge", registry);

   private final YoBoolean currentIcpIsFarEnoughInsideInsideEdge = new YoBoolean("currentIcpIsFarEnoughInsideInsideEdge", registry);
   private final YoBoolean desiredIcpIsFarEnoughInsideInsideEdge = new YoBoolean("desiredIcpIsFarEnoughInsideInsideEdge", registry);

   private final YoBoolean isDesiredICPOKForToeOff = new YoBoolean("isDesiredICPOKForToeOff", registry);
   private final YoBoolean isCurrentICPOKForToeOff = new YoBoolean("isCurrentICPOKForToeOff", registry);

   private final YoBoolean dynamicsAreOkForToeOff = new YoBoolean("dynamicsAreOKForToeOff", registry);

   private final FrameConvexPolygon2D leadingFootPolygon = new FrameConvexPolygon2D();
   private final FrameConvexPolygon2D trailingFootPolygon = new FrameConvexPolygon2D();
   private final FrameConvexPolygon2D onToesPolygon = new FrameConvexPolygon2D();

   private final FramePoint2D desiredICP = new FramePoint2D();
   private final FramePoint2D currentICP = new FramePoint2D();
   private final FramePoint2D toeOffPoint = new FramePoint2D();

   private final PoseReferenceFrame leadingFootFrame = new PoseReferenceFrame("leadingFootFrame", worldFrame);
   private final ZUpFrame leadingFootZUpFrame = new ZUpFrame(leadingFootFrame, "leadingFootZUpFrame");

   private final FrameLine2D insideEdge = new FrameLine2D();
   private final FrameLine2D outsideEdge = new FrameLine2D();
   private final FrameVector2D errorDirection = new FrameVector2D();

   private final Point2DBasics tempPoint = new Point2D();

   public DynamicStateInspector(DynamicStateInspectorParameters parameters, YoRegistry parentRegistry)
   {
      this.parameters = parameters;

      parentRegistry.addChild(registry);
   }

   public void setPolygons(FrameConvexPolygon2DReadOnly leadingFootPolygon,
                           FrameConvexPolygon2DReadOnly trailingFootPolygon,
                           FrameConvexPolygon2DReadOnly onToesPolygon)
   {
      this.leadingFootPolygon.setIncludingFrame(leadingFootPolygon);
      this.trailingFootPolygon.setIncludingFrame(trailingFootPolygon);
      this.onToesPolygon.setIncludingFrame(onToesPolygon);
   }

   public void checkICPLocations(RobotSide trailingFootSide,
                                 FramePose3DReadOnly leadingFootPose,
                                 FramePoint2DReadOnly desiredICP,
                                 FramePoint2DReadOnly currentICP,
                                 FramePoint2DReadOnly toeOffPoint)
   {
      leadingFootFrame.setPoseAndUpdate(leadingFootPose);
      leadingFootZUpFrame.update();

      this.desiredICP.setIncludingFrame(desiredICP);
      this.currentICP.setIncludingFrame(currentICP);
      this.toeOffPoint.setIncludingFrame(toeOffPoint);

      double pseudoStepLength = computeDistanceToLeadingFoot();

      // If the ICP is far enough past the heel, should probably toe off.
      checkIfICPIsSuperFarForward();
      // Check the IcP for not being too far towards the outside of the stance foot
      checkIfICPIsTooFarOutward(trailingFootSide);
      // Check to make sure the ICP is far enough from the toe, so that its control authority doesn't matter too much.
      checkIfICPIsFarEnoughFromTheToe(pseudoStepLength);
      checkICPDistanceFromEdges(trailingFootSide);

      boolean isDesiredICPOKForToeOff = desiredIcpIsFarEnoughFromTheToe.getBooleanValue() && desiredIcpIsFarEnoughInside.getValue()
                                        && desiredIcpIsFarEnoughInsideOutsideEdge.getBooleanValue() && desiredIcpIsFarEnoughInsideInsideEdge.getBooleanValue();
      boolean isCurrentICPOKForToeOff = currentIcpIsFarEnoughFromTheToe.getBooleanValue() && currentIcpIsFarEnoughInside.getValue()
                                        && currentIcpIsFarEnoughInsideOutsideEdge.getBooleanValue() && currentIcpIsFarEnoughInsideInsideEdge.getBooleanValue();

      this.isCurrentICPOKForToeOff.set(isCurrentICPOKForToeOff);
      this.isDesiredICPOKForToeOff.set(isDesiredICPOKForToeOff);

      if (currentIcpIsPastTheHeel.getValue())
         dynamicsAreOkForToeOff.set(true);
      else
         dynamicsAreOkForToeOff.set(isCurrentICPOKForToeOff && isDesiredICPOKForToeOff);
   }

   private void checkIfICPIsSuperFarForward()
   {
      currentICP.changeFrame(leadingFootZUpFrame);
      toeOffPoint.changeFrame(leadingFootZUpFrame);

      leadingFootPolygon.changeFrameAndProjectToXYPlane(leadingFootZUpFrame);

      double minXValue = Math.max(leadingFootPolygon.getMinX(), toeOffPoint.getX());
      distanceOfCurrentICPPastHeel.set(currentICP.getX() - minXValue);

      currentIcpIsPastTheHeel.set(distanceOfCurrentICPPastHeel.getValue() > parameters.getDistanceForwardFromHeel());
   }

   private void checkIfICPIsTooFarOutward(RobotSide trailingFootSide)
   {
      leadingFootPolygon.changeFrameAndProjectToXYPlane(leadingFootFrame);
      currentICP.changeFrame(leadingFootZUpFrame);
      desiredICP.changeFrame(leadingFootZUpFrame);
      toeOffPoint.changeFrame(leadingFootZUpFrame);

      if (trailingFootSide == RobotSide.LEFT)
      {
         double maxY = Math.max(toeOffPoint.getY(), leadingFootPolygon.getMaxY() + parameters.getMinLateralDistance());

         distanceOfCurrentICPInside.set(maxY - currentICP.getY());
         distanceOfDesiredICPInside.set(maxY - desiredICP.getY());
      }
      else
      {
         double minY = Math.min(toeOffPoint.getY(), leadingFootPolygon.getMinY() - parameters.getMinLateralDistance());

         distanceOfCurrentICPInside.set(currentICP.getY() - minY);
         distanceOfDesiredICPInside.set(desiredICP.getY() - minY);
      }

      currentIcpIsFarEnoughInside.set(distanceOfCurrentICPInside.getValue() > parameters.getMinLateralDistance());
      desiredIcpIsFarEnoughInside.set(distanceOfDesiredICPInside.getValue() > parameters.getMinLateralDistance());
   }

   private void checkIfICPIsFarEnoughFromTheToe(double pseudoStepLength)
   {
      currentICP.changeFrame(worldFrame);
      desiredICP.changeFrame(worldFrame);
      toeOffPoint.changeFrame(worldFrame);

      double minDistance = Math.max(parameters.getMinDistanceFromTheToe(), parameters.getMinFractionOfStrideFromTheToe() * pseudoStepLength);
      double minDistanceSquared = MathTools.square(minDistance);

      distanceSquaredOfCurrentICPFromToe.set(currentICP.distanceSquared(toeOffPoint));
      distanceSquaredOfDesiredICPFromToe.set(desiredICP.distanceSquared(toeOffPoint));

      currentIcpIsFarEnoughFromTheToe.set(distanceSquaredOfCurrentICPFromToe.getValue() > minDistanceSquared);
      desiredIcpIsFarEnoughFromTheToe.set(distanceSquaredOfDesiredICPFromToe.getValue() > minDistanceSquared);
   }


   private void checkICPDistanceFromEdges(RobotSide trailingFootSide)
   {
      leadingFootPolygon.changeFrameAndProjectToXYPlane(worldFrame);
      toeOffPoint.changeFrameAndProjectToXYPlane(worldFrame);
      currentICP.changeFrameAndProjectToXYPlane(worldFrame);
      desiredICP.changeFrameAndProjectToXYPlane(worldFrame);

      computeEdgesOfToeOff(trailingFootSide);

      errorDirection.sub(currentICP, desiredICP);
      errorDirection.normalize();

      checkOutsideEdge(trailingFootSide);
      checkInsideEdge(trailingFootSide);
   }

   private void computeEdgesOfToeOff(RobotSide trailingFootSide)
   {
      int startIndex = leadingFootPolygon.lineOfSightStartIndex(toeOffPoint);
      int endIndex = leadingFootPolygon.lineOfSightEndIndex(toeOffPoint);
      boolean isClockwise = leadingFootPolygon.isClockwiseOrdered();

      if (isClockwise == (trailingFootSide == RobotSide.RIGHT))
      {
         outsideEdge.setIncludingFrame(toeOffPoint, leadingFootPolygon.getVertex(endIndex));
         insideEdge.setIncludingFrame(toeOffPoint, leadingFootPolygon.getVertex(startIndex));
      }
      else
      {
         outsideEdge.setIncludingFrame(toeOffPoint, leadingFootPolygon.getVertex(startIndex));
         insideEdge.setIncludingFrame(toeOffPoint, leadingFootPolygon.getVertex(endIndex));
      }
   }

   private void checkOutsideEdge(RobotSide trailingFootSide)
   {
      double currentOrthogonalDistanceToOutsideEdge = outsideEdge.distance(currentICP);
      double desiredOrthogonalDistanceToOutsideEdge = outsideEdge.distance(desiredICP);
      double directionToEdgeInError = rayDistance(outsideEdge);

      if (outsideEdge.isPointOnSideOfLine(currentICP, trailingFootSide == RobotSide.LEFT))
      {  // inside or outside
         currentOrthogonalDistanceToOutsideEdge = -currentOrthogonalDistanceToOutsideEdge;
         directionToEdgeInError = -directionToEdgeInError;
      }

      if (outsideEdge.isPointOnSideOfLine(desiredICP, trailingFootSide == RobotSide.LEFT))
         desiredOrthogonalDistanceToOutsideEdge = -desiredOrthogonalDistanceToOutsideEdge;

      this.desiredOrthogonalDistanceToOutsideEdge.set(desiredOrthogonalDistanceToOutsideEdge);
      this.currentOrthogonalDistanceToOutsideEdge.set(currentOrthogonalDistanceToOutsideEdge);
      this.errorDistanceToOutsideEdge.set(directionToEdgeInError);

      desiredIcpIsFarEnoughInsideOutsideEdge.set(desiredOrthogonalDistanceToOutsideEdge < parameters.getMinOrthogonalDistanceFromOutsideEdge());
      currentIcpIsFarEnoughInsideOutsideEdge.set(currentOrthogonalDistanceToOutsideEdge < parameters.getMinOrthogonalDistanceFromOutsideEdge()
                                                 && directionToEdgeInError < parameters.getMinDistanceFromOutsideEdge());
   }

   private void checkInsideEdge(RobotSide trailingFootSide)
   {
      double currentOrthogonalDistanceToInsideEdge = insideEdge.distance(currentICP);
      double desiredOrthogonalDistanceToInsideEdge = insideEdge.distance(desiredICP);
      double directionToEdgeInError = rayDistance(insideEdge);

      if (insideEdge.isPointOnSideOfLine(currentICP, trailingFootSide == RobotSide.RIGHT))
      {  // inside or outside
         currentOrthogonalDistanceToInsideEdge = -currentOrthogonalDistanceToInsideEdge;
         directionToEdgeInError = -directionToEdgeInError;
      }

      if (insideEdge.isPointOnSideOfLine(desiredICP, trailingFootSide == RobotSide.RIGHT))
         desiredOrthogonalDistanceToInsideEdge = -desiredOrthogonalDistanceToInsideEdge;

      this.desiredOrthogonalDistanceToInsideEdge.set(desiredOrthogonalDistanceToInsideEdge);
      this.currentOrthogonalDistanceToInsideEdge.set(currentOrthogonalDistanceToInsideEdge);
      this.errorDistanceToInsideEdge.set(directionToEdgeInError);

      desiredIcpIsFarEnoughInsideInsideEdge.set(desiredOrthogonalDistanceToInsideEdge < parameters.getMinOrthogonalDistanceFromInsideEdge());
      currentIcpIsFarEnoughInsideInsideEdge.set(currentOrthogonalDistanceToInsideEdge < parameters.getMinOrthogonalDistanceFromInsideEdge()
                                                && directionToEdgeInError < parameters.getMinDistanceFromInsideEdge());
   }

   private double rayDistance(FrameLine2DReadOnly lineToIntersection)
   {
     boolean success = EuclidCoreMissingTools.intersectionBetweenRay2DAndLine2D(desiredICP,
                                                                                errorDirection,
                                                                                lineToIntersection.getPoint(),
                                                                                lineToIntersection.getDirection(),
                                                                                tempPoint);

     if (success)
        return tempPoint.distance(currentICP);

     return Double.POSITIVE_INFINITY;
   }

   private double computeDistanceToLeadingFoot()
   {
      this.toeOffPoint.changeFrameAndProjectToXYPlane(leadingFootZUpFrame);

      return toeOffPoint.distanceFromOrigin();
   }

   public boolean areDynamicsOkForToeOff()
   {
      return dynamicsAreOkForToeOff.getValue();
   }

   boolean isCurrentICPIsPastTheHeel()
   {
      return currentIcpIsPastTheHeel.getBooleanValue();
   }

   boolean isCurrentICPFarEnoughFromTheToe()
   {
      return currentIcpIsFarEnoughFromTheToe.getBooleanValue();
   }

   boolean isDesiredICPFarEnoughFromTheToe()
   {
      return desiredIcpIsFarEnoughFromTheToe.getBooleanValue();
   }

   boolean isCurrentICPFarEnoughInside()
   {
      return currentIcpIsFarEnoughInside.getBooleanValue();
   }

   boolean isDesiredICPFarEnoughInside()
   {
      return desiredIcpIsFarEnoughInside.getBooleanValue();
   }

   boolean isCurrentICPFarEnoughInsideOutsideEdge()
   {
      return currentIcpIsFarEnoughInsideOutsideEdge.getBooleanValue();
   }

   boolean isDesiredICPFarEnoughInsideOutsideEdge()
   {
      return desiredIcpIsFarEnoughInsideOutsideEdge.getBooleanValue();
   }

   boolean isCurrentICPFarEnoughInsideInsideEdge()
   {
      return currentIcpIsFarEnoughInsideInsideEdge.getBooleanValue();
   }

   boolean isDesiredICPFarEnoughInsideInsideEdge()
   {
      return desiredIcpIsFarEnoughInsideInsideEdge.getBooleanValue();
   }

   boolean isDesiredICPOKForToeOff()
   {
      return isDesiredICPOKForToeOff.getBooleanValue();
   }

   boolean isCurrentICPOKForToeOff()
   {
      return isCurrentICPOKForToeOff.getBooleanValue();
   }

   double getDistanceOfCurrentICPPastTheHeel()
   {
      return distanceOfCurrentICPPastHeel.getValue();
   }

   double getDistanceSquaredOfCurrentICPFromToe()
   {
      return distanceSquaredOfCurrentICPFromToe.getValue();
   }

   double getDistanceSquaredOfDesiredICPFromToe()
   {
      return distanceSquaredOfDesiredICPFromToe.getValue();
   }

   double getLateralDistanceOfCurrentICPInside()
   {
      return distanceOfCurrentICPInside.getDoubleValue();
   }

   double getLateralDistanceOfDesiredICPInside()
   {
      return distanceOfDesiredICPInside.getDoubleValue();
   }

   double getCurrentOrthogonalDistanceToOutsideEdge()
   {
      return currentOrthogonalDistanceToOutsideEdge.getDoubleValue();
   }

   double getDesiredOrthogonalDistanceToOutsideEdge()
   {
      return desiredOrthogonalDistanceToOutsideEdge.getDoubleValue();
   }

   double getErrorDistanceToOutsideEdge()
   {
      return errorDistanceToOutsideEdge.getDoubleValue();
   }

   double getCurrentOrthogonalDistanceToInsideEdge()
   {
      return currentOrthogonalDistanceToInsideEdge.getDoubleValue();
   }

   double getDesiredOrthogonalDistanceToInsideEdge()
   {
      return desiredOrthogonalDistanceToInsideEdge.getDoubleValue();
   }

   double getErrorDistanceToInsideEdge()
   {
      return errorDistanceToInsideEdge.getDoubleValue();
   }

   public void attachParameterChangeListener(YoVariableChangedListener changedListener)
   {
      parameters.attachParameterChangeListener(changedListener);
   }
}
