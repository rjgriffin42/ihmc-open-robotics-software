package us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP;

import us.ihmc.commonWalkingControlModules.calculators.EquivalentConstantCoPCalculator;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCoPControlModule;
import us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait.SingleSupportCondition;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObject;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoFramePoint;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;

public class EquivalentConstantCoPDesiredCoPControlModule implements DesiredCoPControlModule
{
   private final YoVariableRegistry registry = new YoVariableRegistry("EquivalentConstantCoPVelocityViaCoPControlModule");

   private final CommonWalkingReferenceFrames referenceFrames;
   private final ProcessedSensorsInterface processedSensors;
   private final CouplingRegistry couplingRegistry;

   private final DoubleYoVariable doubleSupportFinalTime = new DoubleYoVariable("doubleSupportFinalTime", registry);

   private final YoFramePoint desiredFinalCapturePoint = new YoFramePoint("desiredFinalCapturePoint", "", ReferenceFrame.getWorldFrame(), registry);

   private final DoubleYoVariable alphaDesiredCoP = new DoubleYoVariable("alphaDesiredCoP", registry);
   private final AlphaFilteredYoFramePoint desiredCenterOfPressure = AlphaFilteredYoFramePoint.createAlphaFilteredYoFramePoint("desiredCenterOfPressure", "",
                                                                        registry, alphaDesiredCoP, ReferenceFrame.getWorldFrame());
   private final BooleanYoVariable lastTickSingleSupport = new BooleanYoVariable("lastTickSingleSupport", registry);

   private final double controlDT;


   public EquivalentConstantCoPDesiredCoPControlModule(CommonWalkingReferenceFrames referenceFrames, ProcessedSensorsInterface processedSensors,
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
         DynamicGraphicObject desiredCapturePointGraphic = desiredFinalCapturePoint.createDynamicGraphicPosition("Desired Final Capture Point", 0.01,
                                                              YoAppearance.Yellow(), GraphicType.ROTATED_CROSS);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("EquivalentConstantCoPVelocityViaCoPControlModule", desiredCapturePointGraphic);
         dynamicGraphicObjectsListRegistry.registerArtifact("EquivalentConstantCoPVelocityViaCoPControlModule", desiredCapturePointGraphic.createArtifact());

         DynamicGraphicPosition centerOfPressureDesiredGraphic = new DynamicGraphicPosition("Desired Center of Pressure", desiredCenterOfPressure, 0.012,
                                                                    YoAppearance.Gray(), DynamicGraphicPosition.GraphicType.CROSS);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("EquivalentConstantCoPVelocityViaCoPControlModule", centerOfPressureDesiredGraphic);
         dynamicGraphicObjectsListRegistry.registerArtifact("EquivalentConstantCoPVelocityViaCoPControlModule",
                 centerOfPressureDesiredGraphic.createArtifact());
      }
   }

   public FramePoint2d computeDesiredCoPSingleSupport(RobotSide supportLeg, FrameVector2d desiredVelocity, SingleSupportCondition singleSupportCondition, double timeInState)
   {
      lastTickSingleSupport.set(true);

      FrameConvexPolygon2d supportPolygon = couplingRegistry.getBipedSupportPolygons().getFootPolygonInAnkleZUp(supportLeg);
      FramePoint desiredFinalCapturePoint = new FramePoint();
      couplingRegistry.getDesiredFootstep().getPositionIncludingFrame(desiredFinalCapturePoint);
      FramePoint2d desiredFinalCapturePoint2d = desiredFinalCapturePoint.toFramePoint2d();
//      double finalTime = couplingRegistry.getEstimatedSwingTimeRemaining() + 0.2;    // FIXME: hack
      double finalTime = Math.max(couplingRegistry.getEstimatedSwingTimeRemaining(), 50e-3);    // FIXME: hack
      double comHeight = computeCoMHeightUsingOneFoot(supportLeg);

      computeDesiredCoP(supportPolygon, desiredFinalCapturePoint2d, finalTime, comHeight);

      return desiredCenterOfPressure.getFramePoint2dCopy();
   }

   public FramePoint2d computeDesiredCoPDoubleSupport(RobotSide loadingLeg, FrameVector2d desiredVelocity)
   {
      if (lastTickSingleSupport.getBooleanValue())
      {
         resetCoPFilter();
         lastTickSingleSupport.set(false);
      }

      FrameConvexPolygon2d supportPolygon = couplingRegistry.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp();

      boolean stayInDoubleSupport = loadingLeg == null;
      FramePoint2d desiredFinalCapturePoint;
      if (stayInDoubleSupport)
         desiredFinalCapturePoint = computeAverageOfSweetSpots();
      else
      {
         desiredFinalCapturePoint = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(loadingLeg);
      }

      double finalTime = doubleSupportFinalTime.getDoubleValue();
      double comHeight = computeCoMHeightUsingBothFeet();

      computeDesiredCoP(supportPolygon, desiredFinalCapturePoint, finalTime, comHeight);

      return desiredCenterOfPressure.getFramePoint2dCopy();
   }

   private void computeDesiredCoP(FrameConvexPolygon2d supportPolygon, FramePoint2d desiredFinalCapturePoint, double finalTime, double comHeight)
   {
      ReferenceFrame supportPolygonFrame = supportPolygon.getReferenceFrame();

      FramePoint currentCapturePoint = new FramePoint(couplingRegistry.getCapturePointInFrame(supportPolygonFrame));
      FramePoint2d currentCapturePoint2d = currentCapturePoint.toFramePoint2d();

      desiredFinalCapturePoint.changeFrame(supportPolygonFrame);

      double gravity = -processedSensors.getGravityInWorldFrame().getZ();
      FramePoint2d ret;
      if (finalTime > 0.0)
         ret = EquivalentConstantCoPCalculator.computeEquivalentConstantCoP(currentCapturePoint2d, desiredFinalCapturePoint, finalTime, comHeight, gravity);
      else
      {
         ret = desiredFinalCapturePoint;    // will stay like this only if line from current to desired ICP does not intersect support polygon
         FrameLine2d currentToDesired = new FrameLine2d(currentCapturePoint2d, desiredFinalCapturePoint);
         FramePoint2d[] intersections = supportPolygon.intersectionWith(currentToDesired);

         if (intersections != null)
         {
            double maxDistanceSquaredToDesired = Double.NEGATIVE_INFINITY;
            for (FramePoint2d intersection : intersections)
            {
               double distanceSquared = intersection.distanceSquared(desiredFinalCapturePoint);
               if (distanceSquared > maxDistanceSquaredToDesired)
               {
                  ret = intersection;
                  maxDistanceSquaredToDesired = distanceSquared;
               }
            }
         }
      }

      supportPolygon.orthogonalProjection(ret);

      updateYoFramePoints(desiredFinalCapturePoint, ret);
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
      for (RobotSide robotSide : RobotSide.values)
      {
         sum += computeCoMHeightUsingOneFoot(robotSide);
      }

      return sum / RobotSide.values.length;
   }

   private FramePoint2d computeAverageOfSweetSpots()
   {
      FramePoint2d ret = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(RobotSide.LEFT);    // left sweet spot at this point
      FramePoint2d rightSweetSpot = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(RobotSide.RIGHT);
      rightSweetSpot.changeFrame(ret.getReferenceFrame());
      ret.add(rightSweetSpot);
      ret.scale(0.5);

      return ret;
   }

   private void updateYoFramePoints(FramePoint2d desiredFinalCapturePoint, FramePoint2d desiredCenterOfPressure)
   {
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

      FramePoint2d desiredCenterOfPressureWorld = new FramePoint2d(desiredCenterOfPressure);
      desiredCenterOfPressureWorld.changeFrame(worldFrame);
      this.desiredCenterOfPressure.update(desiredCenterOfPressureWorld.getX(), desiredCenterOfPressureWorld.getY(), 0.0);

      FramePoint2d desiredFinalCapturePointWorld = new FramePoint2d();
      desiredFinalCapturePointWorld.changeFrame(worldFrame);
      this.desiredFinalCapturePoint.set(desiredFinalCapturePointWorld.getX(), desiredFinalCapturePointWorld.getY(), 0.0);
   }

   private void resetCoPFilter()
   {
      desiredCenterOfPressure.reset();
   }

   public void setParametersForM2V2()
   {
      doubleSupportFinalTime.set(0.1);
      alphaDesiredCoP.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(20.0, controlDT));
   }
}
