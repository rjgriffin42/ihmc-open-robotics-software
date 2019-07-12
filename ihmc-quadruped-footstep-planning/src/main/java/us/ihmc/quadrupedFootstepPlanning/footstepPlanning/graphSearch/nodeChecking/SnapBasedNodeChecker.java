package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.commons.MathTools;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.quadrupedBasics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.QuadrupedFootstepPlannerNodeRejectionReason;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

import java.util.List;

import static us.ihmc.humanoidRobotics.footstep.FootstepUtils.worldFrame;

public class SnapBasedNodeChecker extends FootstepNodeChecker
{
   private static final boolean DEBUG = false;
   private static final boolean checkAllXGaits = false;

   private final FootstepPlannerParameters parameters;
   private final FootstepNodeSnapper snapper;

   public SnapBasedNodeChecker(FootstepPlannerParameters parameters, FootstepNodeSnapper snapper)
   {
      this.parameters = parameters;
      this.snapper = snapper;
   }

   @Override
   public void setPlanarRegions(PlanarRegionsList planarRegions)
   {
      super.setPlanarRegions(planarRegions);
      snapper.setPlanarRegions(planarRegions);
   }

   @Override
   public boolean isNodeValidInternal(FootstepNode nodeToCheck, FootstepNode previousNode)
   {
      RobotQuadrant movingQuadrant = nodeToCheck.getMovingQuadrant();

      FootstepNodeSnapData snapData = snapper.snapFootstepNode(nodeToCheck);
      RigidBodyTransform snapTransform = snapData.getSnapTransform();
      if (snapTransform.containsNaN())
      {
         if (DEBUG)
         {
            PrintTools.debug("Was not able to snap node:\n" + nodeToCheck);
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.COULD_NOT_SNAP);
         return false;
      }

      if (snapTransform.getM22() < Math.cos(parameters.getMinimumSurfaceInclineRadians()))
      {
         if (DEBUG)
         {
            PrintTools.debug("Surface incline was too steep at radians = " + Math.acos(snapTransform.getM22()) + "\n" + nodeToCheck);
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.SURFACE_NORMAL_TOO_STEEP_TO_SNAP);

         return false;
      }

      if (previousNode == null)
      {
         return true;
      }

      double previousYaw = previousNode.getStepYaw();
      double currentYaw = nodeToCheck.getStepYaw();

      Vector2D offsetVector = new Vector2D(nodeToCheck.getX(movingQuadrant), nodeToCheck.getY(movingQuadrant));
      offsetVector.sub(previousNode.getX(movingQuadrant), previousNode.getY(movingQuadrant));

      Orientation3DReadOnly previousYawOrientation = previousNode.getStepOrientation();
      previousYawOrientation.transform(offsetVector);

      if (Math.abs(offsetVector.getX()) < parameters.getMinXClearanceFromFoot() && Math.abs(offsetVector.getY()) < parameters.getMinYClearanceFromFoot())
      {
         if (DEBUG)
         {
            PrintTools.info("The node " + nodeToCheck + " is trying to step in place.");
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_IN_PLACE);
         return false;
      }

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (robotQuadrant == movingQuadrant)
            continue;

         offsetVector.set(nodeToCheck.getX(movingQuadrant), nodeToCheck.getY(movingQuadrant));
         offsetVector.sub(previousNode.getX(robotQuadrant), previousNode.getY(robotQuadrant));
         previousYawOrientation.transform(offsetVector);

         if (Math.abs(offsetVector.getX()) < parameters.getMinXClearanceFromFoot() && Math.abs(offsetVector.getY()) < parameters.getMinYClearanceFromFoot())
         {
            if (DEBUG)
            {
               PrintTools.info("The node " + nodeToCheck + " is stepping on another foot.");
            }
            rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_ON_OTHER_FOOT);
            return false;
         }
      }

      double yawChange = AngleTools.computeAngleDifferenceMinusPiToPi(currentYaw, previousYaw);
      if (!MathTools.intervalContains(yawChange, parameters.getMinimumStepYaw(), parameters.getMaximumStepYaw()))
      {
         if (DEBUG)
         {
            PrintTools.info("The node " + nodeToCheck + " results in too much yaw.");
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_YAWING_TOO_MUCH);
         return false;
      }

      FramePoint3D newStepPosition = new FramePoint3D(worldFrame, nodeToCheck.getX(movingQuadrant), nodeToCheck.getY(movingQuadrant), 0.0);
      snapTransform.transform(newStepPosition);

      QuadrantDependentList<Point3D> previousSnappedStepPositions = getSnappedStepPositions(previousNode);
      if (!previousSnappedStepPositions.containsKey(movingQuadrant))
      {
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.COULD_NOT_SNAP);
         return false;
      }

      double stepHeight = newStepPosition.getZ() - previousSnappedStepPositions.get(movingQuadrant).getZ();

      if (Math.abs(stepHeight) > parameters.getMaximumStepChangeZ())
      {
         if (DEBUG)
         {
            PrintTools.debug("Too much height difference (" + Math
                  .round(100.0 * Math.abs(newStepPosition.getZ() - previousSnappedStepPositions.get(movingQuadrant).getZ())) + "cm) to previous node:\n"
                                   + nodeToCheck);
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_HIGH_OR_LOW);
         return false;
      }

      boolean isSteppingUp = stepHeight > parameters.getStepZForSteppingUp();
      boolean isSteppingDown = stepHeight < parameters.getStepZForSteppingDown();

      // check total distance
      if (!checkPositionFromNominalXGait(movingQuadrant, newStepPosition, nodeToCheck, previousNode, previousSnappedStepPositions, isSteppingUp, isSteppingDown))
      {
         return false;
      }

      if (checkAllXGaits && !checkFromAllXGaits(movingQuadrant, newStepPosition, nodeToCheck, previousNode, previousSnappedStepPositions, isSteppingUp,
                                                isSteppingDown))
      {
         return false;
      }

      newStepPosition.changeFrame(worldFrame);

      if (hasPlanarRegions() && isObstacleBetweenSteps(newStepPosition, previousSnappedStepPositions.get(movingQuadrant),
                                                       planarRegionsList.getPlanarRegionsAsList(), parameters.getBodyGroundClearance()))
      {
         if (DEBUG)
         {
            PrintTools.debug("Found an obstacle between the nodes " + nodeToCheck + " and " + previousNode);
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.OBSTACLE_BLOCKING_STEP);
         return false;
      }

      if (hasPlanarRegions() && isObstacleBetweenFeet(newStepPosition, movingQuadrant, previousSnappedStepPositions, planarRegionsList.getPlanarRegionsAsList(),
                                                      parameters.getBodyGroundClearance()))
      {
         if (DEBUG)
         {
            PrintTools.debug("Found an obstacle between the nodes " + nodeToCheck + " and " + previousNode);
         }
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.OBSTACLE_BLOCKING_BODY);
         return false;
      }

      return true;
   }

   private QuadrantDependentList<Point3D> getSnappedStepPositions(FootstepNode node)
   {
      QuadrantDependentList<Point3D> snappedStepPositions = new QuadrantDependentList<>();

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         FootstepNodeSnapData snapData = snapper.snapFootstepNode(node.getXIndex(robotQuadrant), node.getYIndex(robotQuadrant));
         RigidBodyTransform footSnapTransform = snapData.getSnapTransform();
         if (footSnapTransform.containsNaN())
            continue;

         Point3D stepPosition = new Point3D(node.getX(robotQuadrant), node.getY(robotQuadrant), 0.0);
         footSnapTransform.transform(stepPosition);
         snappedStepPositions.put(robotQuadrant, stepPosition);
      }

      return snappedStepPositions;
   }

   private boolean checkPositionFromNominalXGait(RobotQuadrant movingQuadrant, FramePoint3D newStepPosition, FootstepNode nodeToCheck,
                                                 FootstepNode previousNode, QuadrantDependentList<Point3D> previousSnappedStepPositions, boolean isSteppingUp,
                                                 boolean isSteppingDown)
   {
      boolean isMovingFront = movingQuadrant.isQuadrantInFront();

      // check total distance
      double maxReach = isMovingFront ? parameters.getMaximumFrontStepReach() : parameters.getMaximumHindStepReach();
      double maxLength, minLength;
      if (isSteppingUp)
      {
         maxLength = isMovingFront ? parameters.getMaximumFrontStepLengthWhenSteppingUp() : parameters.getMaximumHindStepLengthWhenSteppingUp();
         minLength = isMovingFront ? parameters.getMinimumFrontStepLengthWhenSteppingUp() : parameters.getMinimumHindStepLengthWhenSteppingUp();
      }
      else if (isSteppingDown)
      {
         maxLength = isMovingFront ? parameters.getMaximumFrontStepLengthWhenSteppingDown() : parameters.getMaximumHindStepLengthWhenSteppingDown();
         minLength = isMovingFront ? parameters.getMinimumFrontStepLengthWhenSteppingDown() : parameters.getMinimumHindStepLengthWhenSteppingDown();
      }
      else
      {
         maxLength = isMovingFront ? parameters.getMaximumFrontStepLength() : parameters.getMaximumHindStepLength();
         minLength = isMovingFront ? parameters.getMinimumFrontStepLength() : parameters.getMinimumHindStepLength();
      }

      double maxWidth, minWidth;
      if (movingQuadrant.isQuadrantOnLeftSide())
      {
         minWidth = parameters.getMinimumStepWidth();
         maxWidth = parameters.getMaximumStepWidth();
      }
      else
      {
         minWidth = -parameters.getMaximumStepWidth();
         maxWidth = -parameters.getMinimumStepWidth();
      }

      if (DEBUG)
      {
         if (minLength > 0.0)
            throw new RuntimeException("Robot can't step backwards.");
         if (maxLength < 0.0)
            throw new RuntimeException("Robot can't step forwards.");
         if (maxLength < minLength)
            throw new RuntimeException("Length bounds don't work.");
         if (maxWidth < 0.0)
            throw new RuntimeException("Robot can't step left.");
         if (minWidth > 0.0)
            throw new RuntimeException("Robot can't step right.");
         if (maxWidth < minWidth)
            throw new RuntimeException("Width bounds don't work.");
      }

      ReferenceFrame previousXGaitFrame = getPreviousXGaitFrame(movingQuadrant, previousNode, previousSnappedStepPositions);

      FramePoint3D nominalPreviousStepFromXGait = new FramePoint3D(previousXGaitFrame);
      nominalPreviousStepFromXGait.addX(isMovingFront ? 0.5 * previousNode.getNominalStanceLength() : -0.5 * previousNode.getNominalStanceLength());
      nominalPreviousStepFromXGait.addY(0.5 * movingQuadrant.getSide().negateIfRightSide(previousNode.getNominalStanceWidth()));

      newStepPosition.changeFrame(previousXGaitFrame);

      if ((newStepPosition.distanceXY(nominalPreviousStepFromXGait) > maxReach))
      {
         if (DEBUG)
            PrintTools.debug("The node " + nodeToCheck + " is stepping too far.");
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR);
         return false;
      }

      // check forward/backward
      if ((newStepPosition.getX() - nominalPreviousStepFromXGait.getX()) > maxLength)
      {
         if (DEBUG)
            PrintTools.debug("The node " + nodeToCheck + " is stepping too far forward.");
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_FORWARD);
         return false;
      }
      else if (newStepPosition.getX() - nominalPreviousStepFromXGait.getX() < minLength)
      {
         if (DEBUG)
            PrintTools.debug("The node " + nodeToCheck + " is stepping too far backward.");
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_BACKWARD);
         return false;
      }

      // check left/right
      if (newStepPosition.getY() - nominalPreviousStepFromXGait.getY() > maxWidth)
      {
         if (DEBUG)
            PrintTools.debug("The node " + nodeToCheck + " is stepping too far outward.");
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_OUTWARD);
         return false;
      }
      if (newStepPosition.getY() - nominalPreviousStepFromXGait.getY() < minWidth)
      {
         if (DEBUG)
            PrintTools.debug("The node " + nodeToCheck + " is stepping too far inward.");
         rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_INWARD);
         return false;
      }

      return true;
   }

   private boolean checkFromAllXGaits(RobotQuadrant movingQuadrant, FramePoint3D newStepPosition, FootstepNode nodeToCheck, FootstepNode previousNode,
                                      QuadrantDependentList<Point3D> previousSnappedStepPositions, boolean isSteppingUp, boolean isSteppingDown)
   {
      QuadrantDependentList<PoseReferenceFrame> footFrames = getFootFrames(previousSnappedStepPositions, previousNode.getStepYaw());

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (!footFrames.containsKey(robotQuadrant))
            continue;

         ReferenceFrame footFrame = footFrames.get(robotQuadrant);
         FramePoint3D expectedXGaitPoint = new FramePoint3D(footFrame);

         double forwardOffset = movingQuadrant.getEnd() == robotQuadrant.getEnd() ?
               0.0 :
               movingQuadrant.isQuadrantInFront() ? previousNode.getNominalStanceLength() : -previousNode.getNominalStanceLength();
         double sideOffset = movingQuadrant.getSide() == robotQuadrant.getSide() ?
               0.0 :
               movingQuadrant.isQuadrantOnLeftSide() ? previousNode.getNominalStanceWidth() : -previousNode.getNominalStanceWidth();
         expectedXGaitPoint.add(forwardOffset, sideOffset, 0.0);

         newStepPosition.changeFrame(footFrame);

         // check total distance
         double maxReach = robotQuadrant.isQuadrantInFront() ? parameters.getMaximumFrontStepReach() : parameters.getMaximumHindStepReach();
         double maxLength, minLength;
         if (isSteppingUp)
         {
            maxLength = robotQuadrant.isQuadrantInFront() ?
                  parameters.getMaximumFrontStepLengthWhenSteppingUp() :
                  parameters.getMaximumHindStepLengthWhenSteppingUp();
            minLength = robotQuadrant.isQuadrantInFront() ?
                  parameters.getMinimumFrontStepLengthWhenSteppingUp() :
                  parameters.getMinimumHindStepLengthWhenSteppingUp();
         }
         else if (isSteppingDown)
         {
            maxLength = robotQuadrant.isQuadrantInFront() ?
                  parameters.getMaximumFrontStepLengthWhenSteppingDown() :
                  parameters.getMaximumHindStepLengthWhenSteppingDown();
            minLength = robotQuadrant.isQuadrantInFront() ?
                  parameters.getMinimumFrontStepLengthWhenSteppingDown() :
                  parameters.getMinimumHindStepLengthWhenSteppingDown();
         }
         else
         {
            maxLength = robotQuadrant.isQuadrantInFront() ? parameters.getMaximumFrontStepLength() : parameters.getMaximumHindStepLength();
            minLength = robotQuadrant.isQuadrantInFront() ? parameters.getMinimumFrontStepLength() : parameters.getMinimumHindStepLength();
         }

         double maxWidth, minWidth;
         if (movingQuadrant.isQuadrantOnLeftSide())
         {
            minWidth = parameters.getMinimumStepWidth();
            maxWidth = parameters.getMaximumStepWidth();
         }
         else
         {
            minWidth = -parameters.getMaximumStepWidth();
            maxWidth = -parameters.getMinimumStepWidth();
         }

         if (DEBUG)
         {
            if (minLength > 0.0)
               throw new RuntimeException("Robot can't step backwards.");
            if (maxLength < 0.0)
               throw new RuntimeException("Robot can't step forwards.");
            if (maxLength < minLength)
               throw new RuntimeException("Length bounds don't work.");
            if (maxWidth < 0.0)
               throw new RuntimeException("Robot can't step left.");
            if (minWidth > 0.0)
               throw new RuntimeException("Robot can't step right.");
            if (maxWidth < minWidth)
               throw new RuntimeException("Width bounds don't work.");
         }

         if ((newStepPosition.distance(expectedXGaitPoint) > maxReach))
         {
            if (DEBUG)
               PrintTools.debug("The node " + nodeToCheck + " is stepping too far.");
            rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR);
            return false;
         }

         // check forward/backward
         if ((newStepPosition.getX() - expectedXGaitPoint.getX()) > maxLength)
         {
            rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_FORWARD);
            return false;
         }
         else if (newStepPosition.getX() - expectedXGaitPoint.getX() < minLength)
         {
            if (DEBUG)
               PrintTools.debug("The node " + nodeToCheck + " is stepping too far backward.");
            rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_BACKWARD);
            return false;
         }

         // check left/right
         if (newStepPosition.getY() - expectedXGaitPoint.getY() > maxWidth)
         {
            if (DEBUG)
               PrintTools.debug("The node " + nodeToCheck + " is stepping too far outward.");
            rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_OUTWARD);
            return false;
         }
         if (newStepPosition.getY() - expectedXGaitPoint.getY() < minWidth)
         {
            if (DEBUG)
               PrintTools.debug("The node " + nodeToCheck + " is stepping too far inward.");
            rejectNode(nodeToCheck, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_INWARD);
            return false;
         }
      }

      return true;
   }

   private static ReferenceFrame getPreviousXGaitFrame(RobotQuadrant movingQuadrant, FootstepNode previousNode, QuadrantDependentList<Point3D> snappedPositions)
   {
      int numberOfVertices = 0;
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (!snappedPositions.containsKey(robotQuadrant))
            continue;

         numberOfVertices++;
      }

      PoseReferenceFrame xGaitFrame = new PoseReferenceFrame(movingQuadrant.getCamelCaseName() + "XGaitFrame", ReferenceFrame.getWorldFrame());
      double nominalPitch = QuadrupedSupportPolygon.getNominalPitch(snappedPositions, numberOfVertices);
      Orientation3DReadOnly orientation = new Quaternion(previousNode.getStepYaw(), nominalPitch, 0.0);
      xGaitFrame.setPoseAndUpdate(new Point3D(previousNode.getOrComputeXGaitCenterPoint()), orientation);

      return xGaitFrame;
   }

   private static QuadrantDependentList<PoseReferenceFrame> getFootFrames(QuadrantDependentList<Point3D> stepPositions, double yaw)
   {
      int numberOfVertices = 0;
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (!stepPositions.containsKey(robotQuadrant))
            continue;

         numberOfVertices++;
      }

      double nominalPitch = QuadrupedSupportPolygon.getNominalPitch(stepPositions, numberOfVertices);
      Orientation3DReadOnly orientation = new Quaternion(yaw, nominalPitch, 0.0);

      QuadrantDependentList<PoseReferenceFrame> footFrames = new QuadrantDependentList<>();

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (!stepPositions.containsKey(robotQuadrant))
            continue;

         PoseReferenceFrame footFrame = new PoseReferenceFrame(robotQuadrant.getCamelCaseName() + "FootFrame", ReferenceFrame.getWorldFrame());
         footFrame.setPoseAndUpdate(stepPositions.get(robotQuadrant), orientation);

         footFrames.put(robotQuadrant, footFrame);
      }

      return footFrames;
   }

   /**
    * This is meant to test if there is a wall that the body of the robot would run into when shifting
    * from one step to the next. It is not meant to eliminate swing overs.
    */
   private static boolean isObstacleBetweenSteps(Point3DReadOnly footPosition, Point3DReadOnly previousFootPosition, List<PlanarRegion> planarRegions,
                                                 double groundClearance)
   {
      PlanarRegion bodyPath = createBodyCollisionRegionFromTwoFeet(footPosition, previousFootPosition, groundClearance, 2.0);

      for (PlanarRegion region : planarRegions)
      {
         if (!region.intersect(bodyPath).isEmpty())
            return true;
      }

      return false;
   }

   /**
    * This is meant to test if there is a wall that the body of the robot would run into when shifting
    * from one step to the next. It is not meant to eliminate swing overs.
    */
   private static boolean isObstacleBetweenFeet(Point3DReadOnly newFootPosition, RobotQuadrant newFootQuadrant,
                                                QuadrantDependentList<Point3D> previousStepPositions, List<PlanarRegion> planarRegions, double groundClearance)
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (robotQuadrant == newFootQuadrant || !previousStepPositions.containsKey(robotQuadrant))
            continue;

         PlanarRegion bodyPath = createBodyCollisionRegionFromTwoFeet(newFootPosition, previousStepPositions.get(robotQuadrant), groundClearance, 2.0);

         for (PlanarRegion region : planarRegions)
         {
            if (!region.intersect(bodyPath).isEmpty())
               return true;
         }
      }

      return false;
   }

   /**
    * Given two footstep positions this will create a vertical planar region above the points. The region
    * will be aligned with the vector connecting the nodes. It's lower edge will be the specified
    * distance above the higher of the two nodes and the plane will have the specified height.
    */
   public static PlanarRegion createBodyCollisionRegionFromTwoFeet(Point3DReadOnly footA, Point3DReadOnly footB, double clearance, double height)
   {
      double lowerZ = Math.max(footA.getZ(), footB.getZ()) + clearance;
      Point3D point0 = new Point3D(footA.getX(), footA.getY(), lowerZ);
      Point3D point1 = new Point3D(footA.getX(), footA.getY(), lowerZ + height);
      Point3D point2 = new Point3D(footB.getX(), footB.getY(), lowerZ);
      Point3D point3 = new Point3D(footB.getX(), footB.getY(), lowerZ + height);

      Vector3D xAxisInPlane = new Vector3D();
      xAxisInPlane.sub(point2, point0);
      xAxisInPlane.normalize();
      Vector3D yAxisInPlane = new Vector3D(0.0, 0.0, 1.0);
      Vector3D zAxis = new Vector3D();
      zAxis.cross(xAxisInPlane, yAxisInPlane);

      RigidBodyTransform transform = new RigidBodyTransform();
      transform.setRotation(xAxisInPlane.getX(), xAxisInPlane.getY(), xAxisInPlane.getZ(), yAxisInPlane.getX(), yAxisInPlane.getY(), yAxisInPlane.getZ(),
                            zAxis.getX(), zAxis.getY(), zAxis.getZ());
      transform.setTranslation(point0);
      transform.invertRotation();

      point0.applyInverseTransform(transform);
      point1.applyInverseTransform(transform);
      point2.applyInverseTransform(transform);
      point3.applyInverseTransform(transform);

      ConvexPolygon2D polygon = new ConvexPolygon2D();
      polygon.addVertex(point0.getX(), point0.getY());
      polygon.addVertex(point1.getX(), point1.getY());
      polygon.addVertex(point2.getX(), point2.getY());
      polygon.addVertex(point3.getX(), point3.getY());
      polygon.update();

      return new PlanarRegion(transform, polygon);
   }

   @Override
   public void addStartNode(FootstepNode startNode, QuadrantDependentList<RigidBodyTransform> startNodeTransforms)
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         snapper.addSnapData(startNode.getXIndex(robotQuadrant), startNode.getYIndex(robotQuadrant),
                             new FootstepNodeSnapData(startNodeTransforms.get(robotQuadrant)));
      }
   }
}
