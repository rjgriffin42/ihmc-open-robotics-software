package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import static us.ihmc.commonWalkingControlModules.dynamicReachability.CoMIntegrationTools.integrateCoMPositionUsingConstantCMP;
import static us.ihmc.commonWalkingControlModules.dynamicReachability.CoMIntegrationTools.integrateCoMPositionUsingCubicICP;
import static us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools.*;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.robotics.MathTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.trajectories.PositionTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.VelocityConstrainedPositionTrajectoryGenerator;

public class ICPPlannerSegmentedTrajectoryGenerator implements PositionTrajectoryGenerator
{
   private final YoVariableRegistry registry;

   private final YoDouble omega0;

   private final YoDouble maximumSplineDuration;
   private final YoDouble minimumSplineDuration;
   private final YoDouble minimumTimeToSpendOnFinalCMP;
   private final YoDouble totalTrajectoryTime;
   private final YoDouble currentTime;
   private final YoDouble timeSpentOnInitialCMP;
   private final YoDouble timeSpentOnFinalCMP;
   private final YoDouble progressionInPercent;
   private final YoDouble startOfSplineTime;
   private final YoDouble endOfSplineTime;

   private final YoFramePoint yoStartOfSplineICP;
   private final YoFramePoint yoEndOfSplineICP;

   private final YoFramePoint2d yoStartOfSplineCoM;
   private final YoFramePoint2d yoEndOfSplineCoM;

   private final YoInteger currentSegment;

   private ReferenceFrame trajectoryFrame;
   private ReferenceFrame initialFrame;
   private ReferenceFrame finalFrame;

   private final FramePoint initialCornerPointInitialFrame = new FramePoint();
   private final FramePoint finalCornerPointInitialFrame = new FramePoint();
   private final FramePoint initialCMPInitialFrame = new FramePoint();
   private final FramePoint finalCMPInitialFrame = new FramePoint();
   private final FramePoint initialICPInitialFrame = new FramePoint();
   private final FramePoint finalICPInitialFrame = new FramePoint();

   private final FramePoint startOfSplineICPInitialFrame = new FramePoint();
   private final FrameVector3D startOfSplineICPVelocityInitialFrame = new FrameVector3D();
   private final FramePoint endOfSplineICPInitialFrame = new FramePoint();
   private final FrameVector3D endOfSplineICPVelocityInitialFrame = new FrameVector3D();

   private final FramePoint2d startOfSingleSupportCoM = new FramePoint2d();
   private final FramePoint2d startOfSplineCoM = new FramePoint2d();
   private final FramePoint2d endOfSplineCoM = new FramePoint2d();

   private final FramePoint initialCornerPointFinalFrame = new FramePoint();
   private final FramePoint finalCornerPointFinalFrame = new FramePoint();
   private final FramePoint initialCMPFinalFrame = new FramePoint();
   private final FramePoint finalCMPFinalFrame = new FramePoint();
   private final FramePoint initialICPFinalFrame = new FramePoint();
   private final FramePoint finalICPFinalFrame = new FramePoint();

   private final FramePoint startOfSplineICPFinalFrame = new FramePoint();
   private final FrameVector3D startOfSplineICPVelocityFinalFrame = new FrameVector3D();
   private final FramePoint endOfSplineICPFinalFrame = new FramePoint();
   private final FrameVector3D endOfSplineICPVelocityFinalFrame = new FrameVector3D();

   private final FramePoint startOfSplineICP = new FramePoint();
   private final FrameVector3D startOfSplineICPVelocity = new FrameVector3D();
   private final FramePoint endOfSplineICP = new FramePoint();
   private final FrameVector3D endOfSplineICPVelocity = new FrameVector3D();

   private final FramePoint desiredICPInitialFrame = new FramePoint();
   private final FrameVector3D desiredICPVelocityInitialFrame = new FrameVector3D();

   private final FramePoint desiredICPFinalFrame = new FramePoint();
   private final FrameVector3D desiredICPVelocityFinalFrame = new FrameVector3D();

   private final FramePoint desiredICPOutput = new FramePoint();
   private final FrameVector3D desiredICPVelocityOutput = new FrameVector3D();

   private final FramePoint2d desiredCoMPosition = new FramePoint2d();

   private final VelocityConstrainedPositionTrajectoryGenerator spline;

   public ICPPlannerSegmentedTrajectoryGenerator(String namePrefix, ReferenceFrame trajectoryFrame, YoDouble omega0, YoVariableRegistry parentRegistry)
   {
      this.trajectoryFrame = trajectoryFrame;
      initialFrame = trajectoryFrame;
      finalFrame = trajectoryFrame;

      this.omega0 = omega0;

      registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      maximumSplineDuration = new YoDouble(namePrefix + "MaximumSplineDuration", registry);
      minimumSplineDuration = new YoDouble(namePrefix + "MinimumSplineDuration", registry);
      minimumSplineDuration.set(0.1);
      minimumTimeToSpendOnFinalCMP = new YoDouble(namePrefix + "MinimumTimeToSpendOnFinalCMP", registry);
      totalTrajectoryTime = new YoDouble(namePrefix + "TotalTrajectoryTime", registry);
      currentTime = new YoDouble(namePrefix + "CurrentTime", registry);
      timeSpentOnInitialCMP = new YoDouble(namePrefix + "TimeSpentOnInitialCMP", registry);
      timeSpentOnFinalCMP = new YoDouble(namePrefix + "TimeSpentOnFinalCMP", registry);
      progressionInPercent = new YoDouble(namePrefix + "ProgressionInPercent", registry);
      startOfSplineTime = new YoDouble(namePrefix + "StartOfSplineTime", registry);
      endOfSplineTime = new YoDouble(namePrefix + "EndOfSplineTime", registry);

      currentSegment = new YoInteger(namePrefix + "CurrentSegment", registry);

      spline = new VelocityConstrainedPositionTrajectoryGenerator(namePrefix, trajectoryFrame, registry);

      yoStartOfSplineICP = new YoFramePoint(namePrefix + "InitialICPSpline", trajectoryFrame, registry);
      yoEndOfSplineICP = new YoFramePoint(namePrefix + "FinalICPSpline", trajectoryFrame, registry);

      yoStartOfSplineCoM = new YoFramePoint2d(namePrefix + "InitialCoMSpline", trajectoryFrame, registry);
      yoEndOfSplineCoM = new YoFramePoint2d(namePrefix + "FinalCoMSpline", trajectoryFrame, registry);

      parentRegistry.addChild(registry);
   }

   public void createVisualizers(YoGraphicsList yoGraphicsList, ArtifactList artifactList)
   {
      YoGraphicPosition startOfSplineICPViz = new YoGraphicPosition("singleSupportInitialSplineICP", yoStartOfSplineICP, 0.004, YoAppearance.Black(), GraphicType.SOLID_BALL);
      yoGraphicsList.add(startOfSplineICPViz);
      artifactList.add(startOfSplineICPViz.createArtifact());

      YoGraphicPosition endOfSplineICPViz = new YoGraphicPosition("singleSupportFinalSplineICP", yoEndOfSplineICP, 0.004, YoAppearance.Black(), GraphicType.BALL);
      yoGraphicsList.add(endOfSplineICPViz);
      artifactList.add(endOfSplineICPViz.createArtifact());

      YoGraphicPosition startOfSplineCoMViz = new YoGraphicPosition("singleSupportInitialSplineCoM", yoStartOfSplineCoM, 0.004, YoAppearance.Teal(), GraphicType.SOLID_BALL);
      yoGraphicsList.add(startOfSplineCoMViz);
      artifactList.add(startOfSplineCoMViz.createArtifact());

      YoGraphicPosition endOfSplineCoMViz = new YoGraphicPosition("singleSupportFinalSplineCoM", yoEndOfSplineCoM, 0.004, YoAppearance.Teal(), GraphicType.BALL);
      yoGraphicsList.add(endOfSplineCoMViz);
      artifactList.add(endOfSplineCoMViz.createArtifact());
   }

   public void setMaximumSplineDuration(double maximumSplineDuration)
   {
      this.maximumSplineDuration.set(maximumSplineDuration);
   }

   public void setMinimumTimeToSpendOnFinalCMP(double duration)
   {
      minimumTimeToSpendOnFinalCMP.set(duration);
   }

   public void setReferenceFrames(ReferenceFrame initialFrame, ReferenceFrame finalFrame)
   {
      this.initialFrame = initialFrame;
      this.finalFrame = finalFrame;
   }

   public void setTrajectoryTime(double timeSpentOnInitialCMP, double timeSpentOnFinalCMP)
   {
      this.timeSpentOnInitialCMP.set(timeSpentOnInitialCMP);
      this.timeSpentOnFinalCMP.set(timeSpentOnFinalCMP);
      totalTrajectoryTime.set(timeSpentOnInitialCMP + timeSpentOnFinalCMP);
   }

   public void setCornerPoints(YoFramePoint initialCornerPoint, YoFramePoint finalCornerPoint)
   {
      initialCornerPoint.getFrameTupleIncludingFrame(initialCornerPointInitialFrame);
      initialCornerPoint.getFrameTupleIncludingFrame(initialCornerPointFinalFrame);

      finalCornerPoint.getFrameTupleIncludingFrame(finalCornerPointInitialFrame);
      finalCornerPoint.getFrameTupleIncludingFrame(finalCornerPointFinalFrame);
   }

   public void setReferenceCMPs(YoFramePoint initialCMP, YoFramePoint finalCMP)
   {
      initialCMP.getFrameTupleIncludingFrame(initialCMPInitialFrame);
      initialCMP.getFrameTupleIncludingFrame(initialCMPFinalFrame);

      finalCMP.getFrameTupleIncludingFrame(finalCMPInitialFrame);
      finalCMP.getFrameTupleIncludingFrame(finalCMPFinalFrame);
   }

   public void setBoundaryICP(YoFramePoint initialICP, YoFramePoint finalICP)
   {
      initialICP.getFrameTupleIncludingFrame(initialICPInitialFrame);
      initialICP.getFrameTupleIncludingFrame(initialICPFinalFrame);

      finalICP.getFrameTupleIncludingFrame(finalICPInitialFrame);
      finalICP.getFrameTupleIncludingFrame(finalICPFinalFrame);
   }

   public void setInitialCoMPosition(FramePoint2d startOfSingleSupportCoM, ReferenceFrame attachedFrame)
   {
      this.startOfSingleSupportCoM.setIncludingFrame(startOfSingleSupportCoM);
      this.startOfSingleSupportCoM.changeFrame(attachedFrame);
   }

   @Override
   public void initialize()
   {
      initialCornerPointInitialFrame.changeFrame(initialFrame);
      finalCornerPointInitialFrame.changeFrame(initialFrame);
      initialCMPInitialFrame.changeFrame(initialFrame);
      finalCMPInitialFrame.changeFrame(initialFrame);
      initialICPInitialFrame.changeFrame(initialFrame);
      finalICPInitialFrame.changeFrame(initialFrame);

      initialCornerPointFinalFrame.changeFrame(finalFrame);
      finalCornerPointFinalFrame.changeFrame(finalFrame);
      initialCMPFinalFrame.changeFrame(finalFrame);
      finalCMPFinalFrame.changeFrame(finalFrame);
      initialICPFinalFrame.changeFrame(finalFrame);
      finalICPFinalFrame.changeFrame(finalFrame);

      double timeSpentOnInitialCMP = this.timeSpentOnInitialCMP.getDoubleValue();
      double timeSpentOnFinalCMP = this.timeSpentOnFinalCMP.getDoubleValue();

      double alpha = 0.50;
      double startOfSplineTime = timeSpentOnInitialCMP - alpha * maximumSplineDuration.getDoubleValue();
      startOfSplineTime = Math.max(startOfSplineTime, 0.0);

      double endOfSplineTime = timeSpentOnInitialCMP + (1.0 - alpha) * maximumSplineDuration.getDoubleValue();

      if (minimumTimeToSpendOnFinalCMP.getDoubleValue() <= 1.0e-5)
      {
         endOfSplineTime = Math.min(endOfSplineTime, totalTrajectoryTime.getDoubleValue());
      }
      else
      {
         double minTimeOnFinalCMP = minimumTimeToSpendOnFinalCMP.getDoubleValue();
         minTimeOnFinalCMP = Math.min(minTimeOnFinalCMP, timeSpentOnFinalCMP - alpha * minimumSplineDuration.getDoubleValue());

         endOfSplineTime = Math.min(endOfSplineTime, totalTrajectoryTime.getDoubleValue() - minTimeOnFinalCMP);
         
         if (endOfSplineTime > totalTrajectoryTime.getDoubleValue() - minTimeOnFinalCMP)
         {
            endOfSplineTime = totalTrajectoryTime.getDoubleValue() - minTimeOnFinalCMP;
            startOfSplineTime = Math.min(timeSpentOnInitialCMP - (endOfSplineTime - timeSpentOnInitialCMP), 0.0);
         }
      }

      this.startOfSplineTime.set(startOfSplineTime);
      this.endOfSplineTime.set(endOfSplineTime);

      double splineDuration = endOfSplineTime - startOfSplineTime;
      spline.setTrajectoryTime(endOfSplineTime - startOfSplineTime);

      double omega0 = this.omega0.getDoubleValue();
      double dtInitial = startOfSplineTime;
      double dtFinal = endOfSplineTime - timeSpentOnInitialCMP;
      computeDesiredCapturePointPosition(omega0, dtInitial, initialICPInitialFrame, initialCMPInitialFrame, startOfSplineICPInitialFrame);
      computeDesiredCapturePointVelocity(omega0, dtInitial, initialICPInitialFrame, initialCMPInitialFrame, startOfSplineICPVelocityInitialFrame);
      computeDesiredCapturePointPosition(omega0, dtFinal, finalCornerPointInitialFrame, finalCMPInitialFrame, endOfSplineICPInitialFrame);
      computeDesiredCapturePointVelocity(omega0, dtFinal, finalCornerPointInitialFrame, finalCMPInitialFrame, endOfSplineICPVelocityInitialFrame);

      computeDesiredCapturePointPosition(omega0, dtInitial, initialICPFinalFrame, initialCMPFinalFrame, startOfSplineICPFinalFrame);
      computeDesiredCapturePointVelocity(omega0, dtInitial, initialICPFinalFrame, initialCMPFinalFrame, startOfSplineICPVelocityFinalFrame);
      computeDesiredCapturePointPosition(omega0, dtFinal, finalCornerPointFinalFrame, finalCMPFinalFrame, endOfSplineICPFinalFrame);
      computeDesiredCapturePointVelocity(omega0, dtFinal, finalCornerPointFinalFrame, finalCMPFinalFrame, endOfSplineICPVelocityFinalFrame);

      // compute CoM waypoints
      if (Double.isFinite(startOfSplineTime))
         computeCenterOfMassFirstSegment(startOfSplineTime, startOfSplineCoM);
      else
         startOfSplineCoM.set(startOfSingleSupportCoM);

      updateSplineBoundaries();
      initializeSpline();

      if (Double.isFinite(splineDuration))
         computeCenterOfMassSecondSegment(splineDuration, endOfSplineCoM);
      else
         endOfSplineCoM.set(startOfSplineCoM);
   }

   public void computeFinalCoMPosition(FramePoint2d finalCoMToPack)
   {
      computeCenterOfMassFirstSegment(startOfSplineTime.getDoubleValue(), startOfSplineCoM);
      yoStartOfSplineCoM.set(startOfSplineCoM);

      updateSplineBoundaries();
      initializeSpline();
      computeCenterOfMassSecondSegment(spline.getTrajectoryTime(), endOfSplineCoM);
      yoEndOfSplineCoM.set(endOfSplineCoM);

      computeCenterOfMassThirdSegment(totalTrajectoryTime.getDoubleValue() - endOfSplineTime.getDoubleValue(), finalCoMToPack);
   }

   @Override
   public void compute(double time)
   {
      time = MathTools.clamp(time, 0.0, totalTrajectoryTime.getDoubleValue());
      progressionInPercent.set(time / totalTrajectoryTime.getDoubleValue());

      updateSplineBoundaries();

      if (time <= startOfSplineTime.getDoubleValue())
      {
         double timeInSegment = time;
         currentSegment.set(1);
         computeFirstSegment(timeInSegment);
         computeCenterOfMassFirstSegment(timeInSegment, desiredCoMPosition);
      }
      else if (time >= endOfSplineTime.getDoubleValue())
      {
         double timeInSegment = time - endOfSplineTime.getDoubleValue();
         currentSegment.set(3);
         computeThirdSegment(timeInSegment);
         computeCenterOfMassThirdSegment(timeInSegment, desiredCoMPosition);
      }
      else
      {
         double timeInSegment = time - startOfSplineTime.getDoubleValue();
         currentSegment.set(2);
         computeSecondSegment(timeInSegment);
         computeCenterOfMassSecondSegment(timeInSegment, desiredCoMPosition);
      }
   }

   private void computeFirstSegment(double timeInFirstSegment)
   {
      double omega0 = this.omega0.getDoubleValue();
      computeDesiredCapturePointPosition(omega0, timeInFirstSegment, initialICPInitialFrame, initialCMPInitialFrame, desiredICPInitialFrame);
      computeDesiredCapturePointPosition(omega0, timeInFirstSegment, initialICPFinalFrame, initialCMPFinalFrame, desiredICPFinalFrame);

      computeDesiredCapturePointVelocity(omega0, timeInFirstSegment, initialICPInitialFrame, initialCMPInitialFrame, desiredICPVelocityInitialFrame);
      computeDesiredCapturePointVelocity(omega0, timeInFirstSegment, initialICPFinalFrame, initialCMPFinalFrame, desiredICPVelocityFinalFrame);

      interpolatePointFromInitialToFinalFrame(desiredICPOutput, desiredICPInitialFrame, desiredICPFinalFrame, progressionInPercent.getDoubleValue());
      interpolateVectorFromInitialToFinalFrame(desiredICPVelocityOutput, desiredICPVelocityInitialFrame, desiredICPVelocityFinalFrame,
                                               progressionInPercent.getDoubleValue());
   }

   public void computeCenterOfMassFirstSegment(double timeInFirstSegment, FramePoint2d comToPack)
   {
      integrateCoMPositionUsingConstantCMP(timeInFirstSegment, omega0.getDoubleValue(), initialCMPFinalFrame, initialICPFinalFrame,
            startOfSingleSupportCoM, comToPack);
   }

   private void computeSecondSegment(double timeInSecondSegment)
   {
      initializeSpline();
      spline.compute(timeInSecondSegment);

      spline.getPosition(desiredICPOutput);
      spline.getVelocity(desiredICPVelocityOutput);
   }

   public void computeCenterOfMassSecondSegment(double timeInSecondSegment, FramePoint2d comToPack)
   {
      double segmentDuration = spline.getTrajectoryTime();

      yoStartOfSplineCoM.getFrameTuple2d(startOfSplineCoM);
      integrateCoMPositionUsingCubicICP(timeInSecondSegment, segmentDuration, omega0.getDoubleValue(), spline.getCurrentTrajectoryFrame(),
            spline.getXPolynomial(), spline.getYPolynomial(), startOfSplineCoM, comToPack);
   }

   private void initializeSpline()
   {
      spline.setInitialConditions(startOfSplineICP, startOfSplineICPVelocity);
      spline.setFinalConditions(endOfSplineICP, endOfSplineICPVelocity);
      spline.initialize();
   }

   private void computeThirdSegment(double timeInThirdSegment)
   {
      double omega0 = this.omega0.getDoubleValue();
      computeDesiredCapturePointPosition(omega0, timeInThirdSegment, endOfSplineICPInitialFrame, finalCMPInitialFrame, desiredICPInitialFrame);
      computeDesiredCapturePointPosition(omega0, timeInThirdSegment, endOfSplineICPFinalFrame, finalCMPFinalFrame, desiredICPFinalFrame);

      computeDesiredCapturePointVelocity(omega0, timeInThirdSegment, endOfSplineICPInitialFrame, finalCMPInitialFrame, desiredICPVelocityInitialFrame);
      computeDesiredCapturePointVelocity(omega0, timeInThirdSegment, endOfSplineICPFinalFrame, finalCMPFinalFrame, desiredICPVelocityFinalFrame);

      interpolatePointFromInitialToFinalFrame(desiredICPOutput, desiredICPInitialFrame, desiredICPFinalFrame, progressionInPercent.getDoubleValue());
      interpolateVectorFromInitialToFinalFrame(desiredICPVelocityOutput, desiredICPVelocityInitialFrame, desiredICPVelocityFinalFrame,
                                               progressionInPercent.getDoubleValue());
   }
   
   public void computeCenterOfMassThirdSegment(double timeInThirdSegment, FramePoint2d comToPack)
   {
      yoEndOfSplineCoM.getFrameTuple2d(endOfSplineCoM);
      integrateCoMPositionUsingConstantCMP(timeInThirdSegment, omega0.getDoubleValue(), finalCMPFinalFrame, endOfSplineICPFinalFrame, endOfSplineCoM,
            comToPack);
   }

   private void updateSplineBoundaries()
   {
      double progressionInPercent = startOfSplineTime.getDoubleValue() / totalTrajectoryTime.getDoubleValue();
      interpolatePointFromInitialToFinalFrame(startOfSplineICP, startOfSplineICPInitialFrame, startOfSplineICPFinalFrame, progressionInPercent);
      interpolateVectorFromInitialToFinalFrame(startOfSplineICPVelocity, startOfSplineICPVelocityInitialFrame, startOfSplineICPVelocityFinalFrame,
                                               progressionInPercent);

      progressionInPercent = endOfSplineTime.getDoubleValue() / totalTrajectoryTime.getDoubleValue();
      interpolatePointFromInitialToFinalFrame(endOfSplineICP, endOfSplineICPInitialFrame, endOfSplineICPFinalFrame, progressionInPercent);
      interpolateVectorFromInitialToFinalFrame(endOfSplineICPVelocity, endOfSplineICPVelocityInitialFrame, endOfSplineICPVelocityFinalFrame,
                                               progressionInPercent);

      yoStartOfSplineICP.set(startOfSplineICP);
      yoEndOfSplineICP.set(endOfSplineICP);
   }

   private final FramePoint pointA = new FramePoint();
   private final FramePoint pointB = new FramePoint();

   private void interpolatePointFromInitialToFinalFrame(FramePoint pointTrajectoryFrameToPack, FramePoint pointInitialFrame, FramePoint pointFinalFrame,
                                                        double percentOfFinal)
   {
      pointA.setIncludingFrame(pointInitialFrame);
      pointB.setIncludingFrame(pointFinalFrame);
      pointA.changeFrame(trajectoryFrame);
      pointB.changeFrame(trajectoryFrame);
      pointTrajectoryFrameToPack.setToZero(trajectoryFrame);
      pointTrajectoryFrameToPack.interpolate(pointA, pointB, percentOfFinal);
   }

   private final FrameVector3D vectorA = new FrameVector3D();
   private final FrameVector3D vectorB = new FrameVector3D();

   private void interpolateVectorFromInitialToFinalFrame(FrameVector3D vectorTrajectoryFrameToPack, FrameVector3D vectorInitialFrame, FrameVector3D vectorFinalFrame,
                                                         double percentOfFinal)
   {
      vectorA.setIncludingFrame(vectorInitialFrame);
      vectorB.setIncludingFrame(vectorFinalFrame);
      vectorA.changeFrame(trajectoryFrame);
      vectorB.changeFrame(trajectoryFrame);
      vectorTrajectoryFrameToPack.setToZero(trajectoryFrame);
      vectorTrajectoryFrameToPack.interpolate(vectorA, vectorB, percentOfFinal);
   }

   @Override
   public boolean isDone()
   {
      return currentTime.getDoubleValue() >= totalTrajectoryTime.getDoubleValue();
   }

   @Override
   public void getPosition(FramePoint positionToPack)
   {
      positionToPack.setIncludingFrame(desiredICPOutput);
   }

   public boolean isOnExitCMP()
   {
      return progressionInPercent.getDoubleValue() * totalTrajectoryTime.getDoubleValue() > endOfSplineTime.getDoubleValue();
   }

   public void getPosition(YoFramePoint positionToPack)
   {
      positionToPack.set(desiredICPOutput);
   }

   @Override
   public void getVelocity(FrameVector3D velocityToPack)
   {
      velocityToPack.setIncludingFrame(desiredICPVelocityOutput);
   }

   public void getVelocity(YoFrameVector velocityToPack)
   {
      velocityToPack.set(desiredICPVelocityOutput);
   }

   @Override
   public void getAcceleration(FrameVector3D accelerationToPack)
   {
      accelerationToPack.setToZero(trajectoryFrame);
   }

   public void getAcceleration(YoFrameVector accelerationToPack)
   {
      accelerationToPack.setToZero();
   }

   public void getCoMPosition(YoFramePoint2d positionToPack)
   {
      positionToPack.set(desiredCoMPosition);
   }

   @Override
   public void getLinearData(FramePoint positionToPack, FrameVector3D velocityToPack, FrameVector3D accelerationToPack)
   {
      getPosition(positionToPack);
      getVelocity(velocityToPack);
      getAcceleration(accelerationToPack);
   }

   public void getLinearData(YoFramePoint positionToPack, YoFrameVector velocityToPack, YoFrameVector accelerationToPack)
   {
      getPosition(positionToPack);
      getVelocity(velocityToPack);
      getAcceleration(accelerationToPack);
   }

   @Override
   public void showVisualization()
   {
   }

   @Override
   public void hideVisualization()
   {
      yoStartOfSplineICP.setToNaN();
      yoEndOfSplineICP.setToNaN();
   }
}
