package us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.CapturePointCenterOfPressureControlModule;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCoPControlModule;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.GuideLineCalculator;
import us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait.SingleSupportCondition;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
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
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObject;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoFramePoint2d;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint2d;

public class GuideLineDesiredCoPControlModule implements DesiredCoPControlModule
{
   private final YoVariableRegistry registry = new YoVariableRegistry("GuideLineVelocityViaCoPControlModule");

   private final double controlDT;
   private final CommonWalkingReferenceFrames referenceFrames;
   private final CapturePointCenterOfPressureControlModule capturePointCenterOfPressureControlModule;
   private final GuideLineCalculator guideLineCalculator;
   private final ProcessedSensorsInterface processedSensors;
   private final CouplingRegistry couplingRegistry;

   private static final ReferenceFrame world = ReferenceFrame.getWorldFrame();

   private final YoFramePoint desiredCapturePointInWorld = new YoFramePoint("desiredCapturePoint", "", world, registry);
   private final FramePoint desiredCenterOfPressure = new FramePoint(world);
   private final DoubleYoVariable desiredCaptureForwardDoubleSupport = new DoubleYoVariable("desiredCaptureForwardDoubleSupport", registry);
   private final DoubleYoVariable desiredCaptureInwardDoubleSupport = new DoubleYoVariable("desiredCaptureInwardDoubleSupport", registry);
   private final DoubleYoVariable desiredCaptureForwardStayInDoubleSupport = new DoubleYoVariable("desiredCaptureForwardNotLoading", registry);

   private final DoubleYoVariable alphaDesiredCoP = new DoubleYoVariable("alphaDesiredCoP", registry);

   /*
    * Need to have three different filtered points because it matters in which frame you filter.
    */
   private final AlphaFilteredYoFramePoint2d filteredDesiredCoPDoubleSupport;
   private final SideDependentList<AlphaFilteredYoFramePoint2d> filteredDesiredCoPsSingleSupport = new SideDependentList<AlphaFilteredYoFramePoint2d>();

   private final YoFramePoint2d finalDesiredCoPInWorld = new YoFramePoint2d("desiredCoPInWorld", "", world, registry);
   private final BooleanYoVariable lastTickDoubleSupport = new BooleanYoVariable("lastTickDoubleSupport", registry);

   public GuideLineDesiredCoPControlModule(double controlDT, CouplingRegistry couplingRegistry, ProcessedSensorsInterface processedSensors,
           CommonWalkingReferenceFrames referenceFrames, YoVariableRegistry parentRegistry,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, GuideLineCalculator guideLineCalculator,
           CapturePointCenterOfPressureControlModule capturePointCenterOfPressureControlModule)
   {
      this.controlDT = controlDT;
      this.referenceFrames = referenceFrames;
      this.couplingRegistry = couplingRegistry;
      this.processedSensors = processedSensors;
      this.capturePointCenterOfPressureControlModule = capturePointCenterOfPressureControlModule;
      this.guideLineCalculator = guideLineCalculator;
      this.filteredDesiredCoPDoubleSupport = AlphaFilteredYoFramePoint2d.createAlphaFilteredYoFramePoint2d("filteredDesCoPDoubleSupport", "", registry, alphaDesiredCoP,
              referenceFrames.getMidFeetZUpFrame());
      for (RobotSide robotSide : RobotSide.values())
      {
         String namePrefix = "filteredDesiredCoP" + robotSide.getCamelCaseNameForMiddleOfExpression();
         ReferenceFrame ankleZUpFrame = referenceFrames.getAnkleZUpFrame(robotSide);
         filteredDesiredCoPsSingleSupport.put(robotSide, AlphaFilteredYoFramePoint2d.createAlphaFilteredYoFramePoint2d(namePrefix, "", registry, alphaDesiredCoP,
               ankleZUpFrame));
      }

      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }

      if (dynamicGraphicObjectsListRegistry != null)
      {
         DynamicGraphicObjectsList dynamicGraphicObjectList = new DynamicGraphicObjectsList("VelocityViaCoPControlModule");
         ArtifactList artifactList = new ArtifactList("VelocityViaCoPControlModule");

         DynamicGraphicPosition centerOfPressureDesiredWorldGraphicPosition = new DynamicGraphicPosition("Desired Center of Pressure",
                                                                                 finalDesiredCoPInWorld, 0.012, YoAppearance.Gray(),
                                                                                 DynamicGraphicPosition.GraphicType.CROSS);
         dynamicGraphicObjectList.add(centerOfPressureDesiredWorldGraphicPosition);
         artifactList.add(centerOfPressureDesiredWorldGraphicPosition.createArtifact());

         DynamicGraphicObject desiredCapturePointGraphic = desiredCapturePointInWorld.createDynamicGraphicPosition("Desired Capture Point", 0.01,
                                                              YoAppearance.Yellow(), GraphicType.ROTATED_CROSS);
         dynamicGraphicObjectList.add(desiredCapturePointGraphic);
         artifactList.add(desiredCapturePointGraphic.createArtifact());

         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectList);
         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }
   }

   public FramePoint2d computeDesiredCoPSingleSupport(RobotSide supportLeg, FrameVector2d desiredVelocity, SingleSupportCondition singleSupportCondition, double timeInState)
   {
      if (lastTickDoubleSupport.getBooleanValue())
      {
         resetCoPFilter();
         lastTickDoubleSupport.set(false);
      }


      ReferenceFrame supportFootAnkleZUpFrame = referenceFrames.getAnkleZUpFrame(supportLeg);

      BipedSupportPolygons bipedSupportPolygons = couplingRegistry.getBipedSupportPolygons();

      FramePoint capturePointInAnkleZUp = couplingRegistry.getCapturePointInFrame(supportFootAnkleZUpFrame);
      FramePoint2d capturePoint2d = capturePointInAnkleZUp.toFramePoint2d();

      FrameVector2d actualCenterOfMassVelocityInSupportFootFrame = processedSensors.getCenterOfMassVelocityInFrame(supportFootAnkleZUpFrame).toFrameVector2d();

      FramePoint desiredCapturePoint = null;
      FrameLineSegment2d guideLine = null;
      if (desiredVelocity.lengthSquared() > 0.0)
      {
         Footstep footstep = couplingRegistry.getDesiredFootstep();
         FramePoint finalDesiredSwingTarget = footstep.getFootstepPose().getPosition();
         FrameVector2d desiredVelocityInSupportFootFrame = desiredVelocity.changeFrameCopy(supportFootAnkleZUpFrame);
         guideLineCalculator.update(supportLeg, bipedSupportPolygons, capturePoint2d, finalDesiredSwingTarget, desiredVelocityInSupportFootFrame);
         guideLine = guideLineCalculator.getGuideLine(supportLeg);
         guideLine.changeFrame(supportFootAnkleZUpFrame);
         desiredCapturePointInWorld.setToNaN();
      }
      else
      {
         desiredCapturePoint = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(supportLeg).toFramePoint();
         desiredCapturePointInWorld.set(desiredCapturePoint.changeFrameCopy(world));
      }


      FramePoint centerOfMassPosition = processedSensors.getCenterOfMassPositionInFrame(supportFootAnkleZUpFrame);

      capturePointCenterOfPressureControlModule.controlSingleSupport(supportLeg, bipedSupportPolygons, capturePointInAnkleZUp, desiredVelocity,
              guideLine, desiredCapturePoint, centerOfMassPosition, actualCenterOfMassVelocityInSupportFootFrame);    // , percentToFarEdgeOfFoot); // calculates capture points

      capturePointCenterOfPressureControlModule.packDesiredCenterOfPressure(desiredCenterOfPressure);
      FramePoint2d desiredCoP2d = desiredCenterOfPressure.toFramePoint2d();

      AlphaFilteredYoFramePoint2d filteredDesiredCoPSingleSupport = filteredDesiredCoPsSingleSupport.get(supportLeg);
      desiredCoP2d.changeFrame(filteredDesiredCoPSingleSupport.getReferenceFrame());
      filteredDesiredCoPSingleSupport.update(desiredCoP2d);
      filteredDesiredCoPSingleSupport.getFramePoint2d(desiredCoP2d);
      desiredCoP2d.changeFrame(world);
      finalDesiredCoPInWorld.set(desiredCoP2d);

      return desiredCoP2d;
   }

   public FramePoint2d computeDesiredCoPDoubleSupport(RobotSide loadingLeg, FrameVector2d desiredVelocity)
   {
      if (!lastTickDoubleSupport.getBooleanValue())
      {
         resetCoPFilter();
         lastTickDoubleSupport.set(true);
      }

      BipedSupportPolygons bipedSupportPolygons = couplingRegistry.getBipedSupportPolygons();

      FramePoint desiredCapturePoint = computeDesiredCapturePointDoubleSupport(loadingLeg, desiredVelocity, bipedSupportPolygons);

      ReferenceFrame midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();

//    desiredCapturePoint = desiredCapturePoint.changeFrameCopy(currentCapturePoint.getReferenceFrame());
      desiredCapturePoint.changeFrame(midFeetZUpFrame);
      FramePoint currentCapturePoint = couplingRegistry.getCapturePointInFrame(midFeetZUpFrame);
      FramePoint centerOfMassPosition = processedSensors.getCenterOfMassPositionInFrame(midFeetZUpFrame);
      FrameVector2d currentBodyVelocity = processedSensors.getCenterOfMassVelocityInFrame(midFeetZUpFrame).toFrameVector2d();

//    desiredVelocity.setX(0.0);
//    desiredVelocity.setY(0.0);
      capturePointCenterOfPressureControlModule.controlDoubleSupport(bipedSupportPolygons, currentCapturePoint, desiredCapturePoint, centerOfMassPosition,
              desiredVelocity, currentBodyVelocity);
      capturePointCenterOfPressureControlModule.packDesiredCenterOfPressure(desiredCenterOfPressure);

      FramePoint2d desiredCoP2d = new FramePoint2d(desiredCenterOfPressure.getReferenceFrame(), desiredCenterOfPressure.getX(), desiredCenterOfPressure.getY());

      desiredCoP2d.changeFrame(filteredDesiredCoPDoubleSupport.getReferenceFrame());
      this.filteredDesiredCoPDoubleSupport.update(desiredCoP2d);
      filteredDesiredCoPDoubleSupport.getFramePoint2d(desiredCoP2d);
      
      FrameConvexPolygon2d supportPolygon = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();
      supportPolygon.orthogonalProjection(desiredCoP2d);

      desiredCoP2d.changeFrame(world);
      finalDesiredCoPInWorld.set(desiredCoP2d);

      return desiredCoP2d;
   }

   private FramePoint computeDesiredCapturePointDoubleSupport(RobotSide loadingLeg, FrameVector2d desiredVelocity, BipedSupportPolygons bipedSupportPolygons)
   {
      FramePoint desiredCapturePoint;

      boolean stayInDoubleSupport = loadingLeg == null;
      if (stayInDoubleSupport)
      {
         desiredCapturePoint = new FramePoint(referenceFrames.getMidFeetZUpFrame());

         FrameVector leftForward = new FrameVector(referenceFrames.getAnkleZUpFrame(RobotSide.LEFT), 1.0, 0.0, 0.0);
         FrameVector rightForward = new FrameVector(referenceFrames.getAnkleZUpFrame(RobotSide.RIGHT), 1.0, 0.0, 0.0);

         leftForward.changeFrame(desiredCapturePoint.getReferenceFrame());
         rightForward.changeFrame(desiredCapturePoint.getReferenceFrame());

         FrameVector offset = leftForward;
         offset.add(rightForward);
         offset.normalize();
         offset.scale(desiredCaptureForwardStayInDoubleSupport.getDoubleValue());
         desiredCapturePoint.add(offset);
      }
      else if (desiredVelocity.lengthSquared() > 0.0)
      {
         double desiredCaptureY = loadingLeg.negateIfLeftSide(desiredCaptureInwardDoubleSupport.getDoubleValue());
         desiredCapturePoint = new FramePoint(referenceFrames.getAnkleZUpReferenceFrames().get(loadingLeg),
                 desiredCaptureForwardDoubleSupport.getDoubleValue(), desiredCaptureY, 0.0);
      }
      else
      {
         desiredCapturePoint = bipedSupportPolygons.getSweetSpotCopy(loadingLeg).toFramePoint();
      }

      this.desiredCapturePointInWorld.set(desiredCapturePoint.changeFrameCopy(ReferenceFrame.getWorldFrame()));

      return desiredCapturePoint;
   }

   private void resetCoPFilter()
   {
      filteredDesiredCoPDoubleSupport.reset();
      for (RobotSide robotSide : RobotSide.values())
      {
         filteredDesiredCoPsSingleSupport.get(robotSide).reset();
      }
   }

   public void setUpParametersForR2()
   {
      desiredCaptureForwardDoubleSupport.set(0.12); // 0.18);    // 0.2);    // 0.15;
      desiredCaptureInwardDoubleSupport.set(0.01);    // 0.02);
      desiredCaptureForwardStayInDoubleSupport.set(0.05);
      alphaDesiredCoP.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(8.84, controlDT));
   }

   public void setUpParametersForM2V2()
   {
      desiredCaptureForwardDoubleSupport.set(0.03);    // 0.02); // 0.04 (equal to where the guide line ends) // 0.08);
      desiredCaptureInwardDoubleSupport.set(0.0);
      desiredCaptureForwardStayInDoubleSupport.set(0.02);
      alphaDesiredCoP.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(8.84, controlDT));
   }
}
