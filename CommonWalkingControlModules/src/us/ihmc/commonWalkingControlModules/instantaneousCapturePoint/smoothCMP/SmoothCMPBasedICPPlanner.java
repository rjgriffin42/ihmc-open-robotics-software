package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP;

import static us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools.computeDesiredCentroidalMomentumPivot;
import static us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools.computeDesiredCentroidalMomentumPivotVelocity;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator.AngularMomentumEstimationParameters;
import us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator.FootstepAngularMomentumPredictor;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.configurations.ICPPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.ICPTrajectoryPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.SmoothCMPPlannerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.AbstractICPPlanner;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class SmoothCMPBasedICPPlanner extends AbstractICPPlanner
{
   private static final boolean VISUALIZE = true;

   private static final double ZERO_TIME = 0.0;

   private final ReferenceCoPTrajectoryGenerator referenceCoPGenerator;
   private final ReferenceCMPTrajectoryGenerator referenceCMPGenerator;
   private final ReferenceICPTrajectoryGenerator referenceICPGenerator;
   private final FootstepAngularMomentumPredictor angularMomentumGenerator;

   private final List<YoDouble> swingDurationShiftFractions = new ArrayList<>();
   private final YoDouble defaultSwingDurationShiftFraction;
   
   private static final double ICP_CORNER_POINT_SIZE = 0.01;
   private List<YoFramePointInMultipleFrames> icpPhaseEntryCornerPoints = new ArrayList<>();
   private List<YoFramePointInMultipleFrames> icpPhaseExitCornerPoints = new ArrayList<>();

   private final FullRobotModel fullRobotModel;
   private final double gravityZ;

   private final YoFramePoint yoSingleSupportFinalCoM;
   private final FramePoint3D singleSupportFinalCoM = new FramePoint3D();
   
   private final int maxNumberOfICPCornerPointsVisualized = 20;

   public SmoothCMPBasedICPPlanner(FullRobotModel fullRobotModel, BipedSupportPolygons bipedSupportPolygons,
                                   SideDependentList<? extends ContactablePlaneBody> contactableFeet, int maxNumberOfFootstepsToConsider,
                                   int numberOfPointsPerFoot, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry, double gravityZ)
   {
      super(bipedSupportPolygons, maxNumberOfFootstepsToConsider);

      yoSingleSupportFinalCoM = new YoFramePoint(namePrefix + "SingleSupportFinalCoM", worldFrame, registry);

      this.gravityZ = gravityZ;
      defaultSwingDurationShiftFraction = new YoDouble(namePrefix + "DefaultSwingDurationShiftFraction", registry);

      this.fullRobotModel = fullRobotModel;

      ReferenceFrame[] framesToRegister = new ReferenceFrame[] {worldFrame, midFeetZUpFrame, soleZUpFrames.get(RobotSide.LEFT), soleZUpFrames.get(RobotSide.RIGHT)};
      for (int i = 0; i < maxNumberOfFootstepsToConsider; i++)
      {
         YoDouble swingDurationShiftFraction = new YoDouble(namePrefix + "SwingDurationShiftFraction" + i, registry);
         swingDurationShiftFractions.add(swingDurationShiftFraction);
      }
      
      for (int i = 0; i < maxNumberOfICPCornerPointsVisualized - 1; i++)
      {
         YoFramePointInMultipleFrames entryCornerPoint = new YoFramePointInMultipleFrames(namePrefix + "EntryCornerPoints" + i, registry, framesToRegister);
         icpPhaseEntryCornerPoints.add(entryCornerPoint);

         YoFramePointInMultipleFrames exitCornerPoint = new YoFramePointInMultipleFrames(namePrefix + "ExitCornerPoints" + i, registry, framesToRegister);
         icpPhaseExitCornerPoints.add(exitCornerPoint);
      }

      referenceCoPGenerator = new ReferenceCoPTrajectoryGenerator(namePrefix, numberOfPointsPerFoot, maxNumberOfFootstepsToConsider, bipedSupportPolygons,
                                                                  contactableFeet, numberFootstepsToConsider, swingDurations, transferDurations,
                                                                  swingDurationAlphas, swingDurationShiftFractions, transferDurationAlphas, registry);

      referenceCMPGenerator = new ReferenceCMPTrajectoryGenerator(namePrefix, maxNumberOfFootstepsToConsider, numberFootstepsToConsider, swingDurations,
                                                                  transferDurations, swingDurationAlphas, transferDurationAlphas, registry);

      referenceICPGenerator = new ReferenceICPTrajectoryGenerator(namePrefix, omega0, numberFootstepsToConsider, isStanding, isInitialTransfer, isDoubleSupport,
                                                                  worldFrame, registry);

      angularMomentumGenerator = new FootstepAngularMomentumPredictor(namePrefix, omega0, registry);

      parentRegistry.addChild(registry);

      if (yoGraphicsListRegistry != null)
      {
         setupVisualizers(yoGraphicsListRegistry, maxNumberOfFootstepsToConsider);
      }
   }

   public void initializeParameters(ICPPlannerParameters icpPlannerParameters)
   {
      initializeParameters(icpPlannerParameters, false);
   }

   public void initializeParameters(ICPPlannerParameters icpPlannerParameters, boolean computePredictedAngularMomentum)
   {
      super.initializeParameters((ICPTrajectoryPlannerParameters) icpPlannerParameters);

      if (icpPlannerParameters instanceof SmoothCMPPlannerParameters)
      {
         numberFootstepsToConsider.set(icpPlannerParameters.getNumberOfFootstepsToConsider());

         referenceCoPGenerator.initializeParameters((SmoothCMPPlannerParameters) icpPlannerParameters);
         referenceCMPGenerator.setGroundReaction(fullRobotModel.getTotalMass() * gravityZ);
         //FIXME have the angular momentum parameters be passed into or as part of the ICP Planner parameters to the trajectory generator
         angularMomentumGenerator.initializeParameters(new AngularMomentumEstimationParameters(fullRobotModel,
                                                                                               (SmoothCMPPlannerParameters) icpPlannerParameters,
                                                                                               computePredictedAngularMomentum, gravityZ));
         defaultSwingDurationShiftFraction.set(((SmoothCMPPlannerParameters) icpPlannerParameters).getSwingDurationShiftFraction());
      }
      else
      {
         throw new RuntimeException("Tried to submit the wrong type of parameters.");
      }
   }

   private void setupVisualizers(YoGraphicsListRegistry yoGraphicsListRegistry, int maxNumberOfFootstepsToConsider)
   {
      YoGraphicsList yoGraphicsList = new YoGraphicsList(getClass().getSimpleName());
      ArtifactList artifactList = new ArtifactList(getClass().getSimpleName());

      referenceCoPGenerator.createVisualizerForConstantCoPs(yoGraphicsList, artifactList);
      
      
      for (int i = 0; i < maxNumberOfICPCornerPointsVisualized - 1; i++)
      {
         YoFramePoint icpEntryCornerPointInWorld = icpPhaseEntryCornerPoints.get(i).buildUpdatedYoFramePointForVisualizationOnly();
         YoGraphicPosition icpEntryCornerPointsViz = new YoGraphicPosition("ICPEntryCornerPoints" + i, icpEntryCornerPointInWorld, ICP_CORNER_POINT_SIZE,
                                                                           YoAppearance.Green(), GraphicType.SOLID_BALL);

         yoGraphicsList.add(icpEntryCornerPointsViz);
         artifactList.add(icpEntryCornerPointsViz.createArtifact());

         YoFramePoint icpExitCornerPointInWorld = icpPhaseExitCornerPoints.get(i).buildUpdatedYoFramePointForVisualizationOnly();
         YoGraphicPosition icpExitCornerPointsViz = new YoGraphicPosition("ICPExitCornerPoints" + i, icpExitCornerPointInWorld, ICP_CORNER_POINT_SIZE,
                                                                       YoAppearance.Blue(), GraphicType.BALL);

         yoGraphicsList.add(icpExitCornerPointsViz);
         artifactList.add(icpExitCornerPointsViz.createArtifact());
      }

      artifactList.setVisible(VISUALIZE);
      yoGraphicsList.setVisible(VISUALIZE);

      yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
      yoGraphicsListRegistry.registerArtifactList(artifactList);
   }

   @Override
   /** {@inheritDoc} */
   public void clearPlan()
   {
      referenceCoPGenerator.clear();
      referenceCMPGenerator.reset();
      referenceICPGenerator.reset();
      angularMomentumGenerator.clear();

      for (int i = 0; i < swingDurations.size(); i++)
      {
         swingDurations.get(i).setToNaN();
         transferDurations.get(i).setToNaN();
         swingDurationAlphas.get(i).setToNaN();
         transferDurationAlphas.get(i).setToNaN();
         swingDurationShiftFractions.get(i).setToNaN();
      }
   }

   @Override
   /** {@inheritDoc} */
   public void addFootstepToPlan(Footstep footstep, FootstepTiming timing)
   {
      if (footstep == null)
         return;
      referenceCoPGenerator.addFootstepToPlan(footstep, timing);

      int footstepIndex = referenceCoPGenerator.getNumberOfFootstepsRegistered() - 1;
      if(Double.isFinite(timing.getSwingTime())) 
         swingDurations.get(footstepIndex).set(timing.getSwingTime());
      else
         swingDurations.get(footstepIndex).set(1.0);
         
      if(Double.isFinite(timing.getTransferTime()))
         transferDurations.get(footstepIndex).set(timing.getTransferTime());
      else
         transferDurations.get(footstepIndex).set(1.0);
         
      swingDurationAlphas.get(footstepIndex).set(defaultSwingDurationAlpha.getDoubleValue());
      transferDurationAlphas.get(footstepIndex).set(defaultTransferDurationAlpha.getDoubleValue());
      swingDurationShiftFractions.get(footstepIndex).set(defaultSwingDurationShiftFraction.getDoubleValue());

      finalTransferDuration.set(defaultFinalTransferDuration.getDoubleValue());
      finalTransferDurationAlpha.set(defaultTransferDurationAlpha.getDoubleValue());
   }

   @Override
   /** {@inheritDoc} */
   public void initializeForStanding(double initialTime)
   {
      clearPlan();

      this.initialTime.set(initialTime);

      isInitialTransfer.set(isStanding.getBooleanValue());
      isStanding.set(true);
      isDoubleSupport.set(true);
      transferDurations.get(0).set(finalTransferDuration.getDoubleValue());
      transferDurationAlphas.get(0).set(finalTransferDurationAlpha.getDoubleValue());
      updateTransferPlan(true);
   }

   @Override
   /** {@inheritDoc} */
   public void initializeForTransfer(double initialTime)
   {
      this.initialTime.set(initialTime);

      isDoubleSupport.set(true);
      isInitialTransfer.set(isStanding.getBooleanValue());
      isStanding.set(false);
      int numberOfFootstepRegistered = getNumberOfFootstepsRegistered();
      transferDurations.get(numberOfFootstepRegistered).set(finalTransferDuration.getDoubleValue());
      transferDurationAlphas.get(numberOfFootstepRegistered).set(finalTransferDurationAlpha.getDoubleValue());

      updateTransferPlan(true);
   }

   @Override
   /** {@inheritDoc} */
   public void computeFinalCoMPositionInTransfer()
   {
      referenceICPGenerator.getFinalCoMPositionInTransfer(singleSupportFinalCoM);
      yoSingleSupportFinalCoM.set(singleSupportFinalCoM);
   }

   @Override
   /** {@inheritDoc} */
   public void initializeForSingleSupport(double initialTime)
   {
      this.initialTime.set(initialTime);

      isStanding.set(false);
      isDoubleSupport.set(false);

      isInitialTransfer.set(false);
      isHoldingPosition.set(false);

      int numberOfFootstepRegistered = getNumberOfFootstepsRegistered();
      transferDurations.get(numberOfFootstepRegistered).set(finalTransferDuration.getDoubleValue());
      transferDurationAlphas.get(numberOfFootstepRegistered).set(finalTransferDurationAlpha.getDoubleValue());

      updateSingleSupportPlan(true);
   }

   @Override
   /** {@inheritDoc} */
   public void computeFinalCoMPositionInSwing()
   {
      referenceICPGenerator.getFinalCoMPositionInSwing(singleSupportFinalCoM);
      yoSingleSupportFinalCoM.set(singleSupportFinalCoM);
   }

   @Override
   /** {@inheritDoc} */
   protected void updateTransferPlan(boolean computeUpcomingFootstep)
   {
      RobotSide transferToSide = this.transferToSide.getEnumValue();
      if (transferToSide == null)
         transferToSide = RobotSide.LEFT;

      // TODO set up the CoP Generator to be able to only update the current Support Feet CMPs      
      referenceCoPGenerator.computeReferenceCoPsStartingFromDoubleSupport(isInitialTransfer.getBooleanValue(), transferToSide);
      referenceCMPGenerator.setNumberOfRegisteredSteps(referenceCoPGenerator.getNumberOfFootstepsRegistered());
      referenceICPGenerator.setNumberOfRegisteredSteps(referenceCoPGenerator.getNumberOfFootstepsRegistered());

      referenceCoPGenerator.initializeForTransfer(ZERO_TIME);
      referenceICPGenerator.initializeForTransferFromCoPs(referenceCoPGenerator.getTransferCoPTrajectories(), referenceCoPGenerator.getSwingCoPTrajectories());

      referenceICPGenerator.adjustDesiredTrajectoriesForInitialSmoothing();

      angularMomentumGenerator.addFootstepCoPsToPlan(referenceCoPGenerator.getWaypoints(), referenceCoPGenerator.getNumberOfFootstepsRegistered()); //TODO: update CoP waypoints after adjustment
      angularMomentumGenerator.computeReferenceAngularMomentumStartingFromDoubleSupport(isInitialTransfer.getBooleanValue());
      angularMomentumGenerator.initializeForTransfer(ZERO_TIME);

      referenceCMPGenerator.initializeForTransfer(ZERO_TIME, referenceCoPGenerator.getTransferCoPTrajectories(),
                                                  referenceCoPGenerator.getSwingCoPTrajectories(),
                                                  angularMomentumGenerator.getTransferAngularMomentumTrajectories(),
                                                  angularMomentumGenerator.getSwingAngularMomentumTrajectories());
      referenceICPGenerator.initializeForTransfer(ZERO_TIME, referenceCMPGenerator.getTransferCMPTrajectories(),
                                                  referenceCMPGenerator.getSwingCMPTrajectories());

      referenceICPGenerator.initializeCenterOfMass();
      
      referenceICPGenerator.getICPPhaseEntryCornerPoints(icpPhaseEntryCornerPoints);
      referenceICPGenerator.getICPPhaseExitCornerPoints(icpPhaseExitCornerPoints);
      
      // TODO implement requested hold position
      // TODO implement is done walking
   }

   @Override
   /** {@inheritDoc} */
   protected void updateSingleSupportPlan(boolean computeUpcomingFootstep)
   {
      RobotSide supportSide = this.supportSide.getEnumValue();

      // TODO set up the CoP Generator to be able to only update the current Support Feet CMPs
      referenceCoPGenerator.computeReferenceCoPsStartingFromSingleSupport(supportSide);
      referenceCMPGenerator.setNumberOfRegisteredSteps(referenceCoPGenerator.getNumberOfFootstepsRegistered());
      referenceICPGenerator.setNumberOfRegisteredSteps(referenceCoPGenerator.getNumberOfFootstepsRegistered());

      referenceCoPGenerator.initializeForSwing(ZERO_TIME);
      referenceICPGenerator.initializeForSwingFromCoPs(referenceCoPGenerator.getTransferCoPTrajectories(), referenceCoPGenerator.getSwingCoPTrajectories());

      angularMomentumGenerator.addFootstepCoPsToPlan(referenceCoPGenerator.getWaypoints(), referenceCoPGenerator.getNumberOfFootstepsRegistered()); //TODO: update CoP waypoints after adjustment
      angularMomentumGenerator.computeReferenceAngularMomentumStartingFromSingleSupport();
      angularMomentumGenerator.initializeForSwing(ZERO_TIME);

      referenceCMPGenerator.initializeForSwing(ZERO_TIME, referenceCoPGenerator.getTransferCoPTrajectories(), referenceCoPGenerator.getSwingCoPTrajectories(),
                                               angularMomentumGenerator.getTransferAngularMomentumTrajectories(),
                                               angularMomentumGenerator.getSwingAngularMomentumTrajectories());
      referenceICPGenerator.initializeForSwing(ZERO_TIME, referenceCMPGenerator.getTransferCMPTrajectories(), referenceCMPGenerator.getSwingCMPTrajectories());

      referenceICPGenerator.initializeCenterOfMass();
      
      referenceICPGenerator.getICPPhaseEntryCornerPoints(icpPhaseEntryCornerPoints);
      referenceICPGenerator.getICPPhaseExitCornerPoints(icpPhaseExitCornerPoints);
   }

   private void printCoPTrajectories()
   {
      List<? extends CoPTrajectory> transferCoPTrajectory = referenceCoPGenerator.getTransferCoPTrajectories();
      List<? extends CoPTrajectory> swingCoPTrajectory = referenceCoPGenerator.getSwingCoPTrajectories();
      
      for (int i = 0; i < referenceCoPGenerator.getNumberOfFootstepsRegistered(); i++)
      {
         PrintTools.debug(transferCoPTrajectory.get(i).toString());
         PrintTools.debug(swingCoPTrajectory.get(i).toString());
      }
      PrintTools.debug(transferCoPTrajectory.get(referenceCoPGenerator.getNumberOfFootstepsRegistered()).toString());
   }

   @Override
   /** {@inheritDoc} */
   public void compute(double time)
   {
      timeInCurrentState.set(time - initialTime.getDoubleValue());
      timeInCurrentStateRemaining.set(getCurrentStateDuration() - timeInCurrentState.getDoubleValue());
      
      double timeInCurrentState = MathTools.clamp(this.timeInCurrentState.getDoubleValue(), 0.0, referenceCoPGenerator.getCurrentStateFinalTime());

      referenceICPGenerator.compute(timeInCurrentState);
      referenceCoPGenerator.update(timeInCurrentState);
      referenceCMPGenerator.update(timeInCurrentState);
      angularMomentumGenerator.update(timeInCurrentState);

      referenceCoPGenerator.getDesiredCenterOfPressure(desiredCoPPosition, desiredCoPVelocity);
      referenceCMPGenerator.getLinearData(desiredCMPPosition, desiredCMPVelocity);
      referenceICPGenerator.getLinearData(desiredICPPosition, desiredICPVelocity, desiredICPAcceleration);
      angularMomentumGenerator.getDesiredAngularMomentum(desiredCentroidalAngularMomentum, desiredCentroidalTorque);

      referenceICPGenerator.getCoMPosition(desiredCoMPosition);
      referenceICPGenerator.getCoMVelocity(desiredCoMVelocity);
      referenceICPGenerator.getCoMAcceleration(desiredCoMAcceleration);
      /*
       * else { referenceCoPGenerator.update(time);
       * referenceCoPGenerator.getDesiredCenterOfPressure(desiredCoPPosition);
       * desiredCoPVelocity.setToZero();
       * desiredCMPPosition.set(desiredCoPPosition);
       * desiredCMPVelocity.setToZero();
       * desiredICPPosition.set(desiredCoPPosition);
       * desiredICPVelocity.setToZero(); desiredICPAcceleration.setToZero();
       * desiredCoMPosition.set(desiredCoPPosition);
       * desiredCoMVelocity.setToZero(); desiredCoMAcceleration.setToZero(); }
       */
      decayDesiredVelocityIfNeeded();

      // done to account for the delayed velocity
      computeDesiredCentroidalMomentumPivot(desiredICPPosition, desiredICPVelocity, omega0.getDoubleValue(), desiredCMPPosition);
      computeDesiredCentroidalMomentumPivotVelocity(desiredICPVelocity, desiredICPAcceleration, omega0.getDoubleValue(), desiredCMPVelocity);
   }

   /** {@inheritDoc} */
   public void updateListeners()
   {
      referenceCoPGenerator.updateListeners();
   }

   /** {@inheritDoc} */
   public List<CoPPointsInFoot> getCoPWaypoints()
   {
      return referenceCoPGenerator.getWaypoints();
   }

   private final FramePoint3D tempFinalICP = new FramePoint3D();

   @Override
   /** {@inheritDoc} */
   public void getFinalDesiredCapturePointPosition(FramePoint3D finalDesiredCapturePointPositionToPack)
   {
      if (isStanding.getBooleanValue())
      {
         // TODO: replace by CMP generator
         referenceCoPGenerator.getFinalCoPLocation(finalDesiredCapturePointPositionToPack);
      }
      else if (!isInDoubleSupport())
      {
         tempFinalICP.set(getFinalDesiredCapturePointPositions().get(referenceCMPGenerator.getSwingCMPTrajectories().get(0).getNumberOfSegments() - 1));
         tempFinalICP.changeFrame(finalDesiredCapturePointPositionToPack.getReferenceFrame());
         finalDesiredCapturePointPositionToPack.set(tempFinalICP);
      }
      else
      {
         tempFinalICP.set(getFinalDesiredCapturePointPositions().get(referenceCMPGenerator.getTransferCMPTrajectories().get(0).getNumberOfSegments() - 1));
         tempFinalICP.changeFrame(finalDesiredCapturePointPositionToPack.getReferenceFrame());
         finalDesiredCapturePointPositionToPack.set(tempFinalICP);
      }
   }

   @Override
   /** {@inheritDoc} */
   public void getFinalDesiredCapturePointPosition(YoFramePoint2d finalDesiredCapturePointPositionToPack)
   {
      getFinalDesiredCapturePointPosition(tempFinalICP);
      finalDesiredCapturePointPositionToPack.setByProjectionOntoXYPlane(tempFinalICP);
   }

   public List<FramePoint3D> getInitialDesiredCapturePointPositions()
   {
      return referenceICPGenerator.getICPPositionDesiredInitialList();
   }

   public List<FramePoint3D> getFinalDesiredCapturePointPositions()
   {
      return referenceICPGenerator.getICPPositionDesiredFinalList();
   }

   public List<FramePoint3D> getInitialDesiredCenterOfMassPositions()
   {
      return referenceICPGenerator.getCoMPositionDesiredInitialList();
   }

   public List<FramePoint3D> getFinalDesiredCenterOfMassPositions()
   {
      return referenceICPGenerator.getCoMPositionDesiredFinalList();
   }

   private final FramePoint3D tempFinalCoM = new FramePoint3D();

   @Override
   /** {@inheritDoc} */
   public void getFinalDesiredCenterOfMassPosition(FramePoint3D finalDesiredCenterOfMassPositionToPack)
   {
      if (isStanding.getBooleanValue())
      {
         referenceCoPGenerator.getWaypoints().get(1).get(0).getPosition(tempFinalCoM);
      }
      else
      {
         tempFinalCoM.set(singleSupportFinalCoM);
      }

      tempFinalCoM.changeFrame(worldFrame);
      finalDesiredCenterOfMassPositionToPack.setIncludingFrame(tempFinalCoM);

   }

   @Override
   /** {@inheritDoc} */
   public void getNextExitCMP(FramePoint3D exitCMPToPack)
   {
      //CMPTrajectory nextSwingTrajectory = referenceCMPGenerator.getSwingCMPTrajectories().get(0);
      //nextSwingTrajectory.getExitCMPLocation(exitCMPToPack);

      List<CoPPointsInFoot> plannedCoPWaypoints = referenceCoPGenerator.getWaypoints();
      CoPPointsInFoot copPointsInFoot = plannedCoPWaypoints.get(1);
      copPointsInFoot.get(copPointsInFoot.getCoPPointList().size() - 1).getPosition(exitCMPToPack);
   }

   @Override
   /** {@inheritDoc} */
   public boolean isOnExitCMP()
   {
      return referenceCoPGenerator.isOnExitCoP();
   }

   @Override
   /** {@inheritDoc} */
   public int getNumberOfFootstepsToConsider()
   {
      return numberFootstepsToConsider.getIntegerValue();
   }

   @Override
   /** {@inheritDoc} */
   public int getNumberOfFootstepsRegistered()
   {
      return referenceCoPGenerator.getNumberOfFootstepsRegistered();
   }

   public void getPredictedCenterOfMassPosition(YoFramePoint estimatedCenterOfMassPositionToPack, double time)
   {
      angularMomentumGenerator.getPredictedCenterOfMassPosition(estimatedCenterOfMassPositionToPack, time);
   }

   public void getPredictedSwingFootPosition(YoFramePoint predictedSwingFootPositionToPack, double time)
   {
      angularMomentumGenerator.getPredictedSwingFootPosition(predictedSwingFootPositionToPack, time);
   }

   public int getTotalNumberOfSegments()
   {
      return referenceICPGenerator.getTotalNumberOfSegments();
   }

   @Override
   /** {@inheritDoc} */
   public void holdCurrentICP(FramePoint3D icpPositionToHold)
   {
      super.holdCurrentICP(icpPositionToHold);
      // Asking the CoP and ICP to be the same since we assume that holds can be requested in static conditions only
      referenceCoPGenerator.holdPosition(icpPositionToHold);
   }
}
