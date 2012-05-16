package us.ihmc.commonWalkingControlModules.captureRegion;

import java.util.ArrayList;

import javax.vecmath.Vector2d;

import us.ihmc.CapturePointCalculator.LinearInvertedPendulumCapturePointCalculator;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.plotting.Artifact;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoAppearance;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePointInMultipleFrames;


public class CommonCapturePointCalculator implements CapturePointCalculatorInterface
{
   private final ArrayList<Artifact> artifactsToRecordHistory = new ArrayList<Artifact>();

   private final boolean CHECK_COLLINEAR = false;

   private final YoFramePointInMultipleFrames capturePointInMultipleFrames;

   private final YoVariableRegistry registry = new YoVariableRegistry("CapturePoint");
   private final ProcessedSensorsInterface processedSensors;

   private final ReferenceFrame pelvisZUpFrame, midFeetZUpFrame;

   private final SideDependentList<ReferenceFrame> ankleZUpFrames;

   private final DoubleYoVariable pointsCollinearAngle = new DoubleYoVariable("pointsCollinearAngle", registry);

   public CommonCapturePointCalculator(ProcessedSensorsInterface processedSensors, CommonWalkingReferenceFrames referenceFrames, YoVariableRegistry yoVariableRegistry,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.processedSensors = processedSensors;

      this.pelvisZUpFrame = referenceFrames.getPelvisZUpFrame();
      this.midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();

      ankleZUpFrames = referenceFrames.getAnkleZUpReferenceFrames();

      ReferenceFrame[] referenceFramesForCapturePoint = {ReferenceFrame.getWorldFrame(), referenceFrames.getPelvisZUpFrame(),
            referenceFrames.getMidFeetZUpFrame(), referenceFrames.getAnkleZUpFrame(RobotSide.LEFT), referenceFrames.getAnkleZUpFrame(RobotSide.RIGHT)};
      capturePointInMultipleFrames = new YoFramePointInMultipleFrames("capture", referenceFramesForCapturePoint, registry);

      if (yoVariableRegistry != null)
      {
         yoVariableRegistry.addChild(registry);
      }

      if (dynamicGraphicObjectsListRegistry != null)
      {
         DynamicGraphicPosition capturePointWorldGraphicPosition = new DynamicGraphicPosition("Capture Point",
                 capturePointInMultipleFrames.getPointInFrame(ReferenceFrame.getWorldFrame()), 0.01, YoAppearance.Blue(),
                 DynamicGraphicPosition.GraphicType.ROTATED_CROSS);
         DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("CapturePoint");
         dynamicGraphicObjectsList.add(capturePointWorldGraphicPosition);

         ArtifactList artifactList = new ArtifactList("CapturePointCalculator");

         Artifact capturePointArtifact = capturePointWorldGraphicPosition.createArtifact();
         capturePointArtifact.setRecordHistory(true);
         capturePointArtifact.setDrawHistory(true);
         artifactsToRecordHistory.add(capturePointArtifact);
         artifactList.add(capturePointArtifact);

         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }
   }

   public void computeCapturePoint(RobotSide supportLeg)
   {
      ReferenceFrame capturePointFrame = getFrameToComputeCapturePointIn(supportLeg);

      FrameVector comVelocity = processedSensors.getCenterOfMassVelocityInFrame(capturePointFrame);
      FramePoint centerOfMassPosition = processedSensors.getCenterOfMassPositionInFrame(capturePointFrame);
      double gravity = -processedSensors.getGravityInWorldFrame().getZ();
      double comHeight = centerOfMassPosition.getZ();

      double captureX = LinearInvertedPendulumCapturePointCalculator.calculateCapturePoint(comVelocity.getX(), gravity, comHeight);
      double captureY = LinearInvertedPendulumCapturePointCalculator.calculateCapturePoint(comVelocity.getY(), gravity, comHeight);

      captureX += centerOfMassPosition.getX();
      captureY += centerOfMassPosition.getY();

      FramePoint capturePoint = new FramePoint(capturePointFrame, captureX, captureY, 0.0);

      capturePointInMultipleFrames.setFramePoint(capturePoint);
      
      takeArtifactHistorySnapshots();
   }
  

   public FramePoint computePredictedCapturePoint(RobotSide supportLeg, double captureTime, FramePoint centerOfPressure)
   {      
      ReferenceFrame capturePointFrame = getFrameToComputeCapturePointIn(supportLeg);
      
      centerOfPressure = centerOfPressure.changeFrameCopy(capturePointFrame);

      FrameVector comVelocity = processedSensors.getCenterOfMassVelocityInFrame(capturePointFrame);
      FramePoint centerOfMassPosition = processedSensors.getCenterOfMassPositionInFrame(capturePointFrame);
      double gravity = -processedSensors.getGravityInWorldFrame().getZ();
      double comHeight = centerOfMassPosition.getZ();

      double xCoM = centerOfMassPosition.getX();
      double yCoM = centerOfMassPosition.getY();

      double predictedCaptureBodyZUpX = LinearInvertedPendulumCapturePointCalculator.calculatePredictedCapturePoint(xCoM, centerOfPressure.getX(),
                                           comVelocity.getX(), gravity, comHeight, 0.0, captureTime);
      double predictedCaptureBodyZUpY = LinearInvertedPendulumCapturePointCalculator.calculatePredictedCapturePoint(yCoM, centerOfPressure.getY(),
                                           comVelocity.getY(), gravity, comHeight, 0.0, captureTime);

      FramePoint2d predictedCapturePoint2d = new FramePoint2d(capturePointFrame, predictedCaptureBodyZUpX, predictedCaptureBodyZUpY);

      FramePoint predictedCapturePoint = new FramePoint(predictedCapturePoint2d.getReferenceFrame(),
            predictedCapturePoint2d.getX(), predictedCapturePoint2d.getY(), 0.0);    // laInBody.getZ());

      if (CHECK_COLLINEAR)
      {
         YoFramePoint capturePointBodyZUp = capturePointInMultipleFrames.getPointInFrame(pelvisZUpFrame);
         checkCoPCapturePredictedColinear(centerOfPressure, capturePointBodyZUp.getFramePointCopy(), predictedCapturePoint);
      }
      
      return predictedCapturePoint;
   }

   public FramePoint getCapturePointInFrame(ReferenceFrame referenceFrame)
   {
      return capturePointInMultipleFrames.getPointInFrame(referenceFrame).getFramePointCopy();
   }

   public FramePoint2d getCapturePoint2dInFrame(ReferenceFrame referenceFrame)
   {
      if (!referenceFrame.isZupFrame())
      {
         throw new RuntimeException("!referenceFrame.isZupFrame()");
      }

      FramePoint capturePoint = getCapturePointInFrame(referenceFrame);

      return new FramePoint2d(capturePoint.getReferenceFrame(), capturePoint.getX(), capturePoint.getY());
   }
   
   public FrameVector computeCapturePointVelocityInFrame(ReferenceFrame referenceFrame)
   {
      throw new RuntimeException("Not yet implemented");
   }
   
   private ReferenceFrame getFrameToComputeCapturePointIn(RobotSide supportLeg)
   {
      boolean doubleSupport = (supportLeg == null);

      ReferenceFrame capturePointFrame;
      if (doubleSupport)
      {
         capturePointFrame = midFeetZUpFrame;
      }
      else
      {
         capturePointFrame = ankleZUpFrames.get(supportLeg);
      }

      return capturePointFrame;
   }

   private void checkCoPCapturePredictedColinear(FramePoint CoPInBodyZUp, FramePoint captureInBodyZUp, FramePoint predictedCaptureInBodyZUp)
   {
      CoPInBodyZUp.checkReferenceFrameMatch(pelvisZUpFrame);
      captureInBodyZUp.checkReferenceFrameMatch(pelvisZUpFrame);
      predictedCaptureInBodyZUp.checkReferenceFrameMatch(pelvisZUpFrame);

      FrameVector CoPToCapture = new FrameVector(captureInBodyZUp);
      CoPToCapture.sub(CoPInBodyZUp);

      FrameVector captureToPredicted = new FrameVector(predictedCaptureInBodyZUp);
      captureToPredicted.sub(captureInBodyZUp);

      Vector2d CoPToCapture2d = new Vector2d(CoPToCapture.getX(), CoPToCapture.getY());
      Vector2d captureToPredicted2d = new Vector2d(captureToPredicted.getX(), captureToPredicted.getY());

      pointsCollinearAngle.set(CoPToCapture2d.angle(captureToPredicted2d));
   }

   
   private double timeOfLastSnapshot = Double.MIN_VALUE;
   private static final double snapshotTimeSpacing = 0.1;
   
   private void takeArtifactHistorySnapshots()
   {
      if (processedSensors.getTime() > timeOfLastSnapshot + snapshotTimeSpacing)
      {
         timeOfLastSnapshot = processedSensors.getTime();
         
         for (Artifact artifact : artifactsToRecordHistory)
         {
            artifact.takeHistorySnapshot();
         }
      }
   }
}
