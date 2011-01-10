package us.ihmc.commonWalkingControlModules.controlModules;

import java.awt.Color;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.calculators.EquivalentConstantCoPCalculator;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.CapturePointCenterOfPressureControlModule;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoAppearance;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.YoFrameLine2dArtifact;
import com.yobotics.simulationconstructionset.plotting.YoFrameLineSegment2dArtifact;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoFramePoint;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLine2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLineSegment2d;

public class EqConstCoPAndGuideLineCapturePointCoPControlModule implements CapturePointCenterOfPressureControlModule
{
   private final YoVariableRegistry registry = new YoVariableRegistry("EqConstCoPAndGuideLineCapturePointCoPControlModule");

   private final CommonWalkingReferenceFrames referenceFrames;
   private final ProcessedSensorsInterface processedSensors;
   private final CouplingRegistry couplingRegistry;

   private final ReferenceFrame world = ReferenceFrame.getWorldFrame();

   private final DoubleYoVariable minFinalTime = new DoubleYoVariable("minFinalTime", registry);
   private final DoubleYoVariable additionalSingleSupportSwingTime = new DoubleYoVariable("additionalSingleSupportSwingTime", registry);
   
   private final YoFrameLineSegment2d guideLineWorld = new YoFrameLineSegment2d("guideLine", "", world, registry);
   private final YoFrameLine2d parallelLineWorld = new YoFrameLine2d("parallelLine", "", world, registry);

   private final DoubleYoVariable alphaDesiredCoP = new DoubleYoVariable("alphaDesiredCoP", registry);
   private final AlphaFilteredYoFramePoint desiredCenterOfPressure = AlphaFilteredYoFramePoint.createAlphaFilteredYoFramePoint("desiredCenterOfPressure", "",
                                                                        registry, alphaDesiredCoP, ReferenceFrame.getWorldFrame());
   private final BooleanYoVariable lastTickSingleSupport = new BooleanYoVariable("lastTickSingleSupport", registry);
   private final DoubleYoVariable kCaptureGuide = new DoubleYoVariable("kCaptureGuide", "ICP distance to guide line --> position of parallel line", registry);

   private final double controlDT;

   public EqConstCoPAndGuideLineCapturePointCoPControlModule(CommonWalkingReferenceFrames referenceFrames, ProcessedSensorsInterface processedSensors,
           CouplingRegistry couplingRegistry, double controlDT, YoVariableRegistry parentRegistry,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.referenceFrames = referenceFrames;
      this.processedSensors = processedSensors;
      this.couplingRegistry = couplingRegistry;
      this.controlDT = controlDT;

      this.lastTickSingleSupport.set(true);

      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }

      if (dynamicGraphicObjectsListRegistry != null)
      {
         ArtifactList artifactList = new ArtifactList("Capture Point CoP Control Module");

         DynamicGraphicPosition centerOfPressureDesiredGraphic = new DynamicGraphicPosition("Desired Center of Pressure", desiredCenterOfPressure, 0.012,
                                                                    YoAppearance.Gray(), DynamicGraphicPosition.GraphicType.CROSS);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("EquivalentConstantCoPVelocityViaCoPControlModule", centerOfPressureDesiredGraphic);
         artifactList.add(centerOfPressureDesiredGraphic.createArtifact());

         YoFrameLineSegment2dArtifact guideLineArtifact = new YoFrameLineSegment2dArtifact("Guide Line", guideLineWorld, Color.RED);
         artifactList.add(guideLineArtifact);

         YoFrameLine2dArtifact parallellLineArtifact = new YoFrameLine2dArtifact("Parallel Line", parallelLineWorld, Color.GREEN);
         artifactList.add(parallellLineArtifact);

         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }
   }

   public void controlDoubleSupport(BipedSupportPolygons bipedSupportPolygons, FramePoint currentCapturePoint, FramePoint desiredCapturePoint,
                                    FramePoint centerOfMassPositionInWorldFrame, FrameVector2d desiredVelocity, FrameVector2d currentVelocity)
   {
      if (lastTickSingleSupport.getBooleanValue())
      {
         desiredCenterOfPressure.reset();
         lastTickSingleSupport.set(false);
      }

      FrameConvexPolygon2d supportPolygon = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();
      double finalTime = minFinalTime.getDoubleValue();
      double comHeight = computeCoMHeightUsingBothFeet();
      computeDesiredCoP(supportPolygon, desiredCapturePoint.toFramePoint2d(), finalTime, comHeight, null);
   }

   public void controlSingleSupport(FramePoint currentCapturePoint, FrameLineSegment2d guideLine, FramePoint desiredCapturePoint, RobotSide supportLeg,
                                    ReferenceFrame referenceFrame, BipedSupportPolygons supportPolygons, FramePoint centerOfMassPositionInZUpFrame,
                                    FrameVector2d desiredVelocity, FrameVector2d currentVelocity)
   {
      lastTickSingleSupport.set(true);

      FrameConvexPolygon2d supportPolygon = supportPolygons.getFootPolygonInAnkleZUp(supportLeg);
      double finalTime = computeFinalTimeSingleSupport();
      double comHeight = computeCoMHeightUsingOneFoot(supportLeg);
      computeDesiredCoP(supportPolygon, guideLine.getSecondEndPointCopy(), finalTime, comHeight, guideLine);
   }

   public void packDesiredCenterOfPressure(FramePoint desiredCenterOfPressureToPack)
   {
      double x = desiredCenterOfPressure.getX();
      double y = desiredCenterOfPressure.getY();
      double z = 0.0;

      desiredCenterOfPressureToPack.set(desiredCenterOfPressure.getReferenceFrame(), x, y, z);
   }

   private void computeDesiredCoP(FrameConvexPolygon2d supportPolygon, FramePoint2d desiredFinalCapturePoint, double finalTime, double comHeight,
                                  FrameLineSegment2d guideLine)
   {
      ReferenceFrame supportPolygonFrame = supportPolygon.getReferenceFrame();
      desiredFinalCapturePoint.changeFrame(supportPolygonFrame);
      FramePoint2d currentCapturePoint2d = getCapturePoint2dInFrame(supportPolygonFrame);

      double gravity = -processedSensors.getGravityInWorldFrame().getZ();
      FramePoint2d desiredCenterOfPressure = EquivalentConstantCoPCalculator.computeEquivalentConstantCoP(currentCapturePoint2d, desiredFinalCapturePoint,
                                                finalTime, comHeight, gravity);

      if (guideLine != null)
      {
         guideLineWorld.setFrameLineSegment2d(guideLine.changeFrameCopy(world));
         guideLine = guideLine.changeFrameCopy(supportPolygonFrame);
         FrameLine2d shiftedParallelLine = createShiftedParallelLine(guideLine, currentCapturePoint2d);
         shiftedParallelLine.orthogonalProjection(desiredCenterOfPressure);
         movePointInsidePolygon(desiredCenterOfPressure, supportPolygon, shiftedParallelLine);
      }
      else
      {
         FrameLine2d currentToDesired = new FrameLine2d(currentCapturePoint2d, desiredFinalCapturePoint);
         movePointInsidePolygon(desiredCenterOfPressure, supportPolygon, currentToDesired);
         hideLines();
      }

      desiredCenterOfPressure.changeFrame(this.desiredCenterOfPressure.getReferenceFrame());
      this.desiredCenterOfPressure.update(desiredCenterOfPressure.getX(), desiredCenterOfPressure.getY(), 0.0);
   }

   private FramePoint2d getCapturePoint2dInFrame(ReferenceFrame supportPolygonFrame)
   {
      FramePoint currentCapturePoint = new FramePoint(couplingRegistry.getCapturePoint());
      currentCapturePoint.changeFrame(supportPolygonFrame);
      FramePoint2d currentCapturePoint2d = currentCapturePoint.toFramePoint2d();

      return currentCapturePoint2d;
   }

   private FrameLine2d createShiftedParallelLine(FrameLineSegment2d guideLine, FramePoint2d currentCapturePoint2d)
   {
      FramePoint2d captureProjectedOntoGuideLine = guideLine.orthogonalProjectionCopy(currentCapturePoint2d);

      FrameVector2d projectedToCurrent = new FrameVector2d(captureProjectedOntoGuideLine, currentCapturePoint2d);
      projectedToCurrent.scale(kCaptureGuide.getDoubleValue());

      FramePoint2d shiftedPoint = new FramePoint2d(captureProjectedOntoGuideLine);
      shiftedPoint.add(projectedToCurrent);

      FrameVector2d directionVector = guideLine.getVectorCopy();
      FrameLine2d shiftedParallelLine = new FrameLine2d(shiftedPoint, directionVector);

      parallelLineWorld.setFrameLine2d(shiftedParallelLine.changeFrameCopy(world));

      return shiftedParallelLine;
   }

   private void hideLines()
   {
      guideLineWorld.setFrameLineSegment2d(null);
      parallelLineWorld.setFrameLine2d(null);
   }

   private static void movePointInsidePolygon(FramePoint2d point, FrameConvexPolygon2d polygon, FrameLine2d guideLine)
   {
      // If feasible CoP is not inside the convex hull of the feet, project it into it.
      if (!polygon.isPointInside(point))
      {
         // supportPolygon.orthogonalProjection(centerOfPressureDesired2d);

         FramePoint2d[] intersections = polygon.intersectionWith(guideLine);
         if (intersections != null)
         {
            FramePoint2d intersectionToUse;

            if (intersections.length == 2)
            {
               double distanceSquaredToIntersection0 = point.distanceSquared(intersections[0]);
               double distanceSquaredToIntersection1 = point.distanceSquared(intersections[1]);

               if (distanceSquaredToIntersection0 <= distanceSquaredToIntersection1)
                  intersectionToUse = intersections[0];
               else
                  intersectionToUse = intersections[1];


               point.setX(intersectionToUse.getX());
               point.setY(intersectionToUse.getY());

               // Move in a little along the line:
               FrameLineSegment2d guideLineSegment = new FrameLineSegment2d(intersections);
               FrameVector2d frameVector2d = guideLineSegment.getVectorCopy();
               frameVector2d.normalize();
               frameVector2d.scale(-0.002);    // Move toward desired capture by 2 mm to prevent some jerky behavior with VTPs..

               point.setX(point.getX() + frameVector2d.getX());
               point.setY(point.getY() + frameVector2d.getY());
            }
            else
            {
               throw new RuntimeException("This is interesting, shouldn't get here.");
            }
         }
         else
         {
            point.set(polygon.getClosestVertexCopy(guideLine));
         }
      }
   }

   private double computeCoMHeightUsingOneFoot(RobotSide sideToGetCoMHeightFor)
   {
      ReferenceFrame footFrame = referenceFrames.getAnkleZUpReferenceFrames().get(sideToGetCoMHeightFor);
      FramePoint centerOfMass = processedSensors.getCenterOfMassPositionInFrame(footFrame);

      return centerOfMass.getZ();
   }

   private double computeCoMHeightUsingBothFeet()
   {
      double sum = 0.0;
      for (RobotSide robotSide : RobotSide.values())
      {
         sum += computeCoMHeightUsingOneFoot(robotSide);
      }

      return sum / RobotSide.values().length;
   }

   private double computeFinalTimeSingleSupport()
   {
      double estimatedSwingTimeRemaining = couplingRegistry.getEstimatedSwingTimeRemaining();
      double ret = Math.max(estimatedSwingTimeRemaining, minFinalTime.getDoubleValue());
      ret += additionalSingleSupportSwingTime.getDoubleValue();
      return ret;
   }
   
   public void setParametersForR2()
   {
      minFinalTime.set(0.1);
      kCaptureGuide.set(2.0);
      additionalSingleSupportSwingTime.set(0.1);
      alphaDesiredCoP.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(50.0, controlDT));
   }

   public void setParametersForM2V2()
   {
      minFinalTime.set(0.1);
      kCaptureGuide.set(4.0);
      additionalSingleSupportSwingTime.set(0.3);
      alphaDesiredCoP.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(50.0, controlDT));
   }
}
