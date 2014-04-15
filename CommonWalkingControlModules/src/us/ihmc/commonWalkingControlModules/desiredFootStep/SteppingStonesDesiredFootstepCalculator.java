package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.captureRegion.OneStepCaptureRegionCalculator;
import us.ihmc.commonWalkingControlModules.captureRegion.SteppingStonesCaptureRegionIntersectionCalculator;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.ground.steppingStones.SteppingStones;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;



/**
 * <p>Title: SteppingStonesDesiredStepLocationCalculator</p>
 *
 * <p>Description: A DesiredStepLocationCalculator that tries to step in a direction, a given distance (with respect to the support foot)
 * but will adjust to be inside the stepping stones, and if this spot will lead to the robot falling, this will try to calculate a position
 * close to the desired point that will result
 * in the robot maintaining balance.</p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: </p>
 *
 * @author Yobotics-IHMC biped team
 * @version 1.0
 */
public class SteppingStonesDesiredFootstepCalculator implements DesiredFootstepCalculator
{
   private final YoVariableRegistry registry = new YoVariableRegistry("DesiredStepLocationCalculator");

   private final SideDependentList<ReferenceFrame> footZUpFrames;
   private final DoubleYoVariable stanceWidth = new DoubleYoVariable("stanceWidth", registry);
   private final DoubleYoVariable stanceLength = new DoubleYoVariable("stanceLength", registry);
   private final DoubleYoVariable stepAngle = new DoubleYoVariable("stepAngle", registry);
   private final DoubleYoVariable stepDistance = new DoubleYoVariable("stepDistance", registry);
   private final DoubleYoVariable stepUp = new DoubleYoVariable("stepUp", registry);
   private final DoubleYoVariable stepYaw = new DoubleYoVariable("stepYaw", registry);

   private final DoubleYoVariable percentNominalToAdjusted = new DoubleYoVariable("percentNominalToAdjusted", registry);

   private final YoFramePoint adjustedStepPosition = new YoFramePoint("adjustedStepPosition", "", ReferenceFrame.getWorldFrame(), registry);

   private final SteppingStonesCaptureRegionIntersectionCalculator steppingStonesCaptureRegionIntersectionCalculator;

   private StepLocationScorer stepLocationScorer;

   private final CaptureRegionStepLocationSelectionMethod captureRegionStepLocationSelectionMethod = CaptureRegionStepLocationSelectionMethod.NEAREST_POINT;

   private boolean VISUALIZE = true;

   private final CouplingRegistry couplingRegistry;

   private Footstep desiredFootstep;

   private final SideDependentList<? extends ContactablePlaneBody> contactableBodies;


   // Constructors
   public SteppingStonesDesiredFootstepCalculator(SideDependentList<? extends ContactablePlaneBody> contactableBodies, SteppingStones steppingStones,
           CouplingRegistry couplingRegistry, CommonWalkingReferenceFrames commonWalkingReferenceFrames, OneStepCaptureRegionCalculator captureRegionCalculator,
           YoVariableRegistry yoVariableRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      percentNominalToAdjusted.set(1.0);

      this.couplingRegistry = couplingRegistry;
      this.contactableBodies = contactableBodies;


      double defaultStepWidth = 0.22;
      stanceWidth.set(defaultStepWidth);
      double defaultStanceLength = captureRegionCalculator.getKinematicStepRange() * 0.5;    // 0.7;//0.40;
      stanceLength.set(defaultStanceLength);
      stepAngle.set(Math.atan(stanceWidth.getDoubleValue() / stanceLength.getDoubleValue()));    // 0.4
      stepDistance.set(Math.sqrt((stanceWidth.getDoubleValue() * stanceWidth.getDoubleValue())
                                 + (stanceLength.getDoubleValue() * stanceLength.getDoubleValue())));

      ReferenceFrame leftAnkleZUpFrame = commonWalkingReferenceFrames.getAnkleZUpReferenceFrames().get(RobotSide.LEFT);
      ReferenceFrame rightAnkleZUpFrame = commonWalkingReferenceFrames.getAnkleZUpReferenceFrames().get(RobotSide.RIGHT);

      footZUpFrames = new SideDependentList<ReferenceFrame>(leftAnkleZUpFrame, rightAnkleZUpFrame);

      stepLocationScorer = new WeightedDistanceScorer(captureRegionCalculator, footZUpFrames, stanceWidth, stanceLength, stepAngle, stepDistance);

//    stepLocationScorer = new BasicReachableScorer(yoboticsBipedCaptureRegionCalculator);

      if (steppingStones != null)
      {
         steppingStonesCaptureRegionIntersectionCalculator = new SteppingStonesCaptureRegionIntersectionCalculator(steppingStones, yoVariableRegistry,
                 dynamicGraphicObjectsListRegistry);
      }
      else
      {
         steppingStonesCaptureRegionIntersectionCalculator = null;
      }

      yoVariableRegistry.addChild(registry);

      if (dynamicGraphicObjectsListRegistry == null)
         VISUALIZE = false;

      if (VISUALIZE)
      {
         double scale = 0.012;
         DynamicGraphicPosition adjustedStepPositionDynamicGraphicPosition = new DynamicGraphicPosition("Adjusted Step Position", adjustedStepPosition, scale,
                                                                                YoAppearance.Yellow());

         dynamicGraphicObjectsListRegistry.registerArtifact("DesiredStepLocation", adjustedStepPositionDynamicGraphicPosition.createArtifact());

//       int level = 4;
//       YoboticsBipedPlotter.registerDynamicGraphicPosition("Adjusted Step Position", adjustedStepPositionDynamicGraphicPosition, level);
      }
   }

   // Getters
// public Footstep getDesiredFootstep()
// {
//    return desiredFootstep;
// }


   public void initializeDesiredFootstep(RobotSide supportLegSide)
   {
      updateAndGetDesiredFootstep(supportLegSide);

   }


   public Footstep updateAndGetDesiredFootstep(RobotSide supportLegSide)
   {
      FramePoint nominalLocation = getNominalStepLocation(supportLegSide);
      Point2d nominalLocation2d = new Point2d(nominalLocation.getX(), nominalLocation.getY());

      FrameConvexPolygon2d captureRegion = couplingRegistry.getCaptureRegion();

      if (captureRegion == null)
      {
         // Foot Step Orientation
         FrameOrientation footstepOrientation = new FrameOrientation(nominalLocation.getReferenceFrame());
         footstepOrientation.setYawPitchRoll(stepYaw.getDoubleValue(), 0.0, 0.0);

         // Create a foot Step Pose from Position and Orientation
         FramePose footstepPose = new FramePose(nominalLocation, footstepOrientation);
         PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("poseReferenceFrame", footstepPose);

         ContactablePlaneBody foot = contactableBodies.get(supportLegSide.getOppositeSide());

         boolean trustHeight = false;
         ReferenceFrame soleFrame = FootstepUtils.createSoleFrame(poseReferenceFrame, foot);
         List<FramePoint> expectedContactPoints = FootstepUtils.getContactPointsInFrame(foot, soleFrame);

         desiredFootstep = new Footstep(foot, poseReferenceFrame, soleFrame, expectedContactPoints, trustHeight);
      }

      captureRegion = captureRegion.changeFrameCopy(ReferenceFrame.getWorldFrame());

      ArrayList<ConvexPolygon2d> captureRegionSteppingStonesIntersections = null;
      if (steppingStonesCaptureRegionIntersectionCalculator != null)
      {
         captureRegionSteppingStonesIntersections =
            steppingStonesCaptureRegionIntersectionCalculator.findIntersectionsBetweenSteppingStonesAndCaptureRegion(captureRegion);
      }

      switch (captureRegionStepLocationSelectionMethod)
      {
         case NEAREST_CENTROID :
            computeAndSetBestCentroidToStepTo(nominalLocation2d, captureRegionSteppingStonesIntersections);

            break;

         case NEAREST_POINT :
            computeAndSetBestNearestPointToStepTo(nominalLocation2d, captureRegionSteppingStonesIntersections);

            break;

         default :
            throw new RuntimeException("Enum constant not handled");
      }

      FrameVector nominalToAdjusted = new FrameVector(adjustedStepPosition.getFramePointCopy());
      nominalToAdjusted.sub(nominalLocation);
      nominalToAdjusted.scale(percentNominalToAdjusted.getDoubleValue());

      FramePoint locationToReturn = new FramePoint(nominalLocation);
      locationToReturn.add(nominalToAdjusted);

      // Foot Step Orientation
      FrameOrientation footstepOrientation = new FrameOrientation(nominalLocation.getReferenceFrame());
      footstepOrientation.setYawPitchRoll(stepYaw.getDoubleValue(), 0.0, 0.0);

      // Create a foot Step Pose from Position and Orientation
      FramePose footstepPose = new FramePose(locationToReturn, footstepOrientation);
      PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("poseReferenceFrame", footstepPose);

      ContactablePlaneBody foot = contactableBodies.get(supportLegSide.getOppositeSide());

      boolean trustHeight = false;
      ReferenceFrame soleReferenceFrame = FootstepUtils.createSoleFrame(poseReferenceFrame, foot);
      List<FramePoint> expectedContactPoints = FootstepUtils.getContactPointsInFrame(foot, soleReferenceFrame);

      desiredFootstep = new Footstep(foot, poseReferenceFrame, soleReferenceFrame, expectedContactPoints, trustHeight);

      return desiredFootstep;
   }

   public FramePoint getNominalStepLocation(RobotSide supportLeg)
   {
      double sideDependentStepAngle = supportLeg.negateIfLeftSide(stepAngle.getDoubleValue());

      ReferenceFrame supportLegAnkleZUpFrame = footZUpFrames.get(supportLeg);

      FramePoint desiredSwingToPosition = new FramePoint(supportLegAnkleZUpFrame);
      desiredSwingToPosition.setX(desiredSwingToPosition.getX() + stepDistance.getDoubleValue() * Math.cos(sideDependentStepAngle));
      desiredSwingToPosition.setY(desiredSwingToPosition.getY() + stepDistance.getDoubleValue() * Math.sin(sideDependentStepAngle));

      desiredSwingToPosition.setZ(desiredSwingToPosition.getZ() + stepUp.getDoubleValue());
      desiredSwingToPosition = desiredSwingToPosition.changeFrameCopy(ReferenceFrame.getWorldFrame());

      return desiredSwingToPosition;
   }

   public double getStepLocationScore(RobotSide supportLeg, FramePose desiredFootPose)
   {
      return stepLocationScorer.getStepLocationScore(supportLeg, desiredFootPose);
   }

   private void computeAndSetBestNearestPointToStepTo(Point2d nominalLocation2d, ArrayList<ConvexPolygon2d> captureRegionSteppingStonesIntersections)
   {
      Point2d nearestPoint = new Point2d();
      double nearestDistanceSquared = Double.POSITIVE_INFINITY;
      Point2d pointToTest = new Point2d();

      if (captureRegionSteppingStonesIntersections != null)    // If there are no captureRegionSteppingStonesIntersections, just keep stepping where you were before for now...
      {
         for (ConvexPolygon2d possiblePlaceToStep : captureRegionSteppingStonesIntersections)
         {
            pointToTest.set(nominalLocation2d);
            possiblePlaceToStep.orthogonalProjection(pointToTest);

            double possibleDistanceSquared = nominalLocation2d.distanceSquared(pointToTest);

            if (possibleDistanceSquared < nearestDistanceSquared)
            {
               nearestPoint.set(pointToTest);
               nearestDistanceSquared = possibleDistanceSquared;
            }
         }

         if (nearestDistanceSquared != Double.POSITIVE_INFINITY)    // If there are no near centroids, just keep stepping where you were before...
         {
            adjustedStepPosition.set(nearestPoint.x, nearestPoint.y, 0.0);
         }
      }

   }

   private void computeAndSetBestCentroidToStepTo(Point2d nominalLocation2d, ArrayList<ConvexPolygon2d> captureRegionSteppingStonesIntersections)
   {
      // For now, just step to the midpoint of the nearest one:
      Point2d nearestCentroid = null;
      double nearestDistanceSquared = Double.POSITIVE_INFINITY;
      Point2d centroid = new Point2d();

      if (captureRegionSteppingStonesIntersections != null)    // If there are no captureRegionSteppingStonesIntersections, just keep stepping where you were before for now...
      {
         for (ConvexPolygon2d possiblePlaceToStep : captureRegionSteppingStonesIntersections)
         {
            possiblePlaceToStep.getCentroid(centroid);
            double possibleDistanceSquared = nominalLocation2d.distanceSquared(centroid);

            if (possibleDistanceSquared < nearestDistanceSquared)
            {
               nearestCentroid = new Point2d(centroid);
               nearestDistanceSquared = possibleDistanceSquared;
            }
         }

         if (nearestCentroid != null)    // If there are no near centroids, just keep stepping where you were before...
         {
            adjustedStepPosition.set(nearestCentroid.x, nearestCentroid.y, 0.0);
         }
      }
   }

   private enum CaptureRegionStepLocationSelectionMethod {NEAREST_CENTROID, NEAREST_POINT}

   public void setupParametersForM2V2()
   {
   }

   public void setupParametersForR2()
   {
   }

   public Footstep predictFootstepAfterDesiredFootstep(RobotSide supportLegSide, Footstep desiredFootstep)
   {
      return null;
   }

   public boolean isDone()
   {
      return false;
   }

}
