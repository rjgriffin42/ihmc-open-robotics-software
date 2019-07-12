package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.stepCost;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.Line2D;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.quadrupedBasics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.quadrupedPlanning.stepStream.QuadrupedXGaitTools;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;


public class XGaitCost implements FootstepCost
{
   private static final double desiredVelocityTrackingWeight = 5.0;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final FootstepPlannerParameters plannerParameters;
   private final FootstepNodeSnapper snapper;
   private final QuadrupedXGaitSettingsReadOnly xGaitSettings;
   private final QuadrantDependentList<Point3D> startFootPositions = new QuadrantDependentList<>();

   private final NominalVelocityProvider velocityProvider;


   public XGaitCost(FootstepPlannerParameters plannerParameters, QuadrupedXGaitSettingsReadOnly xGaitSettings, FootstepNodeSnapper snapper, NominalVelocityProvider velocityProvider)
   {
      this.plannerParameters = plannerParameters;
      this.xGaitSettings = xGaitSettings;
      this.snapper = snapper;
      this.velocityProvider = velocityProvider;

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         startFootPositions.put(robotQuadrant, new Point3D());
   }


   private final FramePose3D startXGaitPose = new FramePose3D();
   private final FramePose3D endXGaitPose = new FramePose3D();
   private final PoseReferenceFrame startXGaitPoseFrame = new PoseReferenceFrame("startXGaitPose", ReferenceFrame.getWorldFrame());
   private final PoseReferenceFrame endXGaitPoseFrame = new PoseReferenceFrame("endXGaitPose", ReferenceFrame.getWorldFrame());

   // FIXME make this more efficient
   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      RobotQuadrant movingQuadrant = endNode.getMovingQuadrant();
      RobotQuadrant previousQuadrant = startNode.getMovingQuadrant();
      if (movingQuadrant.getNextReversedRegularGaitSwingQuadrant() != previousQuadrant)
      { // fixme this should account for different phases.
         throw new RuntimeException("For some reason the feet movement is out of order.");
      }

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         FootstepNodeSnapData snapData = snapper.snapFootstepNode(startNode.getXIndex(robotQuadrant), startNode.getYIndex(robotQuadrant));
         startFootPositions.get(robotQuadrant).set(startNode.getX(robotQuadrant), startNode.getY(robotQuadrant), 0.0);
         snapData.getSnapTransform().transform(startFootPositions.get(robotQuadrant));
      }

      Point2DReadOnly startXGaitPosition = startNode.getOrComputeXGaitCenterPoint();
      Point2DReadOnly endXGaitPosition = endNode.getOrComputeXGaitCenterPoint();

      double nominalPitch = QuadrupedSupportPolygon.getNominalPitch(startFootPositions, 4);
      startXGaitPose.setPosition(startXGaitPosition);
      startXGaitPose.setOrientationYawPitchRoll(startNode.getStepYaw(), nominalPitch, 0.0);
      startXGaitPoseFrame.setPoseAndUpdate(startXGaitPose);


      FrameVector2D nominalVelocityHeading = new FrameVector2D(worldFrame, velocityProvider.computeNominalNormalizedVelocityHeadingInWorld(startNode));
      nominalVelocityHeading.changeFrameAndProjectToXYPlane(startXGaitPoseFrame);

      double durationBetweenSteps = QuadrupedXGaitTools.computeTimeDeltaBetweenSteps(previousQuadrant, xGaitSettings);
      double desiredMaxForwardSpeed = plannerParameters.getMaxWalkingSpeedMultiplier() * xGaitSettings.getMaxSpeed();
      double desiredMaxHorizontalSpeed = xGaitSettings.getMaxHorizontalSpeedFraction() * desiredMaxForwardSpeed;

      double desiredVelocityAtHeading = computeMagnitudeOnEllipseInDirection(desiredMaxForwardSpeed, desiredMaxHorizontalSpeed, nominalVelocityHeading.getX(), nominalVelocityHeading.getY());

      FrameVector2D desiredTranslation = new FrameVector2D(nominalVelocityHeading);
      desiredTranslation.scale(desiredVelocityAtHeading * durationBetweenSteps);
      desiredTranslation.changeFrameAndProjectToXYPlane(worldFrame);

      FramePoint2D nominalEndXGaitPosition = new FramePoint2D();
      nominalEndXGaitPosition.add(startXGaitPosition, desiredTranslation);

      double nominalYawOfEnd = velocityProvider.computeNominalYaw(nominalEndXGaitPosition);
      if (Double.isNaN(nominalYawOfEnd))
         nominalYawOfEnd = startNode.getStepYaw();

      double angleDifference = AngleTools.computeAngleDifferenceMinusPiToPi(startNode.getStepYaw(), nominalYawOfEnd);
      if (Math.abs(angleDifference) > Math.PI / 2.0) // greater than 90 degrees, so go backwards
         nominalYawOfEnd = AngleTools.trimAngleMinusPiToPi(nominalYawOfEnd + Math.PI);


      endXGaitPose.setPosition(nominalEndXGaitPosition);
      endXGaitPose.setOrientationYawPitchRoll(nominalYawOfEnd, 0.0, 0.0);
      endXGaitPoseFrame.setPoseAndUpdate(endXGaitPose);

      FramePoint2D nominalEndFootPosition = new FramePoint2D(endXGaitPoseFrame);
      nominalEndFootPosition.setX(0.5 * movingQuadrant.getEnd().negateIfHindEnd(xGaitSettings.getStanceLength()));
      nominalEndFootPosition.setY(0.5 * movingQuadrant.getSide().negateIfRightSide(xGaitSettings.getStanceWidth()));
      nominalEndFootPosition.changeFrameAndProjectToXYPlane(worldFrame);
      nominalEndFootPosition.setX(FootstepNode.gridSizeXY * FootstepNode.snapToGrid(nominalEndFootPosition.getX()));
      nominalEndFootPosition.setY(FootstepNode.gridSizeXY * FootstepNode.snapToGrid(nominalEndFootPosition.getY()));

      Point2D endFootPosition = new Point2D(endNode.getX(movingQuadrant), endNode.getY(movingQuadrant));

      double rotationError = AngleTools.computeAngleDifferenceMinusPiToPi(endNode.getStepYaw(), nominalYawOfEnd);
      double xGaitArmLength = Math.sqrt(MathTools.square(0.5 * xGaitSettings.getStanceLength() + MathTools.square(0.5 * xGaitSettings.getStanceWidth())));
      double footDisplacementFromRotationError = 2.0 * xGaitArmLength * Math.sin(0.5 * rotationError);
      double distanceFromNominalEndFootPosition = endNode.getOrComputeXGaitCenterPoint().distance(nominalEndXGaitPosition) + footDisplacementFromRotationError;


      double costOfNominalVelocity = desiredVelocityTrackingWeight * nominalEndXGaitPosition.distance(endXGaitPosition);

      double distanceFromNominalXGaitCenter;
      if (nominalEndXGaitPosition.distance(startXGaitPosition) < 1e-2)
      {
         distanceFromNominalXGaitCenter = endXGaitPosition.distance(startXGaitPosition);
      }
      else
      {
         Line2D acceptableTranslation = new Line2D(startXGaitPosition, nominalEndXGaitPosition);
         distanceFromNominalXGaitCenter = acceptableTranslation.distance(endXGaitPosition);
      }
      double costOfXGait = plannerParameters.getXGaitWeight() * distanceFromNominalXGaitCenter;

      return costOfNominalVelocity + costOfNominalVelocity;

//      return plannerParameters.getXGaitWeight() * distanceFromNominalEndFootPosition;
   }

   static double computeMagnitudeOnEllipseInDirection(double maxX, double maxY, double xDirection, double yDirection)
   {
      double magnitude = EuclidCoreTools.norm(xDirection, yDirection);
      magnitude *= maxX * maxY;
      magnitude /= Math.sqrt(MathTools.square(maxX * yDirection) + MathTools.square(maxY * xDirection));
      return magnitude;
   }
}
