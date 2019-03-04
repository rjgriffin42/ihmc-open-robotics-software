package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeChecker;

import org.junit.jupiter.api.Test;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.QuadrupedFootstepPlannerNodeRejectionReason;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.SimplePlanarRegionFootstepNodeSnapper;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.listeners.QuadrupedFootstepPlannerListener;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeChecking.FootstepNodeChecker;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeChecking.SnapBasedNodeChecker;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SnapBasedNodeCheckerTest
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @Test
   public void testStepInPlace()
   {
      FootstepPlannerParameters parameters = new DefaultFootstepPlannerParameters()
      {
         @Override
         public double getMinimumStepLength()
         {
            return -0.3;
         }
      };
      SimplePlanarRegionFootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(parameters);
      FootstepNodeChecker nodeChecker = new SnapBasedNodeChecker(parameters, snapper);

      TestListener testListener = new TestListener();
      nodeChecker.addPlannerListener(testListener);

      // set up kind of a random ground plane
      ConvexPolygon2D groundPlanePolygon = new ConvexPolygon2D();
      groundPlanePolygon.addVertex(10.0, 10.0);
      groundPlanePolygon.addVertex(-10.0, 10.0);
      groundPlanePolygon.addVertex(10.0, -10.0);
      groundPlanePolygon.addVertex(-10.0, -10.0);
      groundPlanePolygon.update();
      AxisAngle normal = new AxisAngle(new Vector3D(0.05, 0.05, 0.8), Math.PI / 6);
      Vector3D translation = new Vector3D(0.78, 0.9, 0.12);
      RigidBodyTransform transform = new RigidBodyTransform(normal, translation);
      PlanarRegionsList planarRegionList = new PlanarRegionsList();
      planarRegionList.addPlanarRegion(new PlanarRegion(transform, groundPlanePolygon));

      PoseReferenceFrame footstepFrame = new PoseReferenceFrame("footstepFrame", worldFrame);
      footstepFrame.setPoseAndUpdate(new Point3D(0.6, 0.9, 0.0), new AxisAngle(-Math.PI / 4, 0.0, 0.0));

      // have to set this really small, so that we don't get a ton of rounding issues
      FootstepNode.gridSizeXY = 0.001;
      RobotQuadrant robotQuadrant = RobotQuadrant.FRONT_LEFT;

      FramePoint2D frontLeft = new FramePoint2D(footstepFrame, 1.57, 0.67);
      FramePoint2D frontRight = new FramePoint2D(footstepFrame, 1.63, 0.12);
      FramePoint2D hindLeft = new FramePoint2D(footstepFrame, 0.67, 0.72);
      FramePoint2D otherHindLeft = new FramePoint2D(footstepFrame, 0.69, 0.72);
      FramePoint2D hindRight = new FramePoint2D(footstepFrame, 0.73, 0.05);

      frontLeft.changeFrame(worldFrame);
      frontRight.changeFrame(worldFrame);
      hindLeft.changeFrame(worldFrame);
      otherHindLeft.changeFrame(worldFrame);
      hindRight.changeFrame(worldFrame);


      // check stepping on exactly the same spot
      FootstepNode previousNode = new FootstepNode(robotQuadrant, frontLeft, frontRight, otherHindLeft, hindRight, 1.0, 0.5);
      FootstepNode node = new FootstepNode(robotQuadrant, frontLeft, frontRight, hindLeft, hindRight, 1.0, 0.5);


      PoseReferenceFrame nodeFrame = new PoseReferenceFrame("nodeFrame", worldFrame);
      nodeFrame.setPoseAndUpdate(new Point3D(previousNode.getOrComputeXGaitCenterPoint().getX(), previousNode.getOrComputeXGaitCenterPoint().getY(), 0.0),
                                 new AxisAngle(previousNode.getNominalYaw(), 0.0, 0.0));


      String message = "Stepping from " + previousNode + " to " + node ;

//      assertFalse(nodeChecker.isNodeValid(node, previousNode));
//      testListener.assertCorrectRejection(message, node, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_IN_PLACE);

      FrameVector2D clearanceVector = new FrameVector2D(nodeFrame, parameters.getMinXClearanceFromFoot(), parameters.getMinYClearanceFromFoot());
      clearanceVector.changeFrame(worldFrame);

      // check stepping on not quite the same, but well within the clearance vector
      Random random = new Random(1738L);
      for (int iter = 0; iter < 50; iter++)
      {
         Vector2D offsetVector = new Vector2D(clearanceVector);
         double scaleFactor = RandomNumbers.nextDouble(random, 0.0, 0.8);
         offsetVector.scale(scaleFactor);
         if (RandomNumbers.nextBoolean(random, 0.5))
            offsetVector.setX(-offsetVector.getX());
         if (RandomNumbers.nextBoolean(random, 0.5))
            offsetVector.setY(-offsetVector.getY());

         Point2D shiftedFrontLeft = new Point2D(frontLeft);
         shiftedFrontLeft.add(offsetVector);

         FootstepNode newNode = new FootstepNode(robotQuadrant, shiftedFrontLeft, frontRight, hindLeft, hindRight, 1.0, 0.5);

         if (newNode.getXIndex(robotQuadrant) == previousNode.getXIndex(robotQuadrant) && newNode.getYIndex(robotQuadrant) == previousNode.getYIndex(robotQuadrant))
            continue;

         message = "iter = " + iter + ". Stepping from " + previousNode + "\nTo " + newNode + "\n, clearance amount in the moving foot is only " + offsetVector + "\n clearance required is " + clearanceVector;
         assertFalse(nodeChecker.isNodeValid(newNode, previousNode), message);
         testListener.assertCorrectRejection(message, newNode, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_IN_PLACE);
      }

      // check stepping near the edge of the clearance vector
      for (int iter = 0; iter < 10; iter++)
      {
         FrameVector2D offsetVector = new FrameVector2D(clearanceVector);
         double scaleFactor = RandomNumbers.nextDouble(random, 0.8, 0.95);
         offsetVector.scale(scaleFactor);
         if (RandomNumbers.nextBoolean(random, 0.5))
            offsetVector.setX(-offsetVector.getX());
         if (RandomNumbers.nextBoolean(random, 0.5))
            offsetVector.setY(-offsetVector.getY());

         FramePoint2D shiftedFrontLeft = new FramePoint2D(frontLeft);
         shiftedFrontLeft.add(offsetVector);

         FootstepNode newNode = new FootstepNode(robotQuadrant, shiftedFrontLeft, frontRight, hindLeft, hindRight, 1.0, 0.5);


         offsetVector.changeFrame(nodeFrame);
         message = "Stepping from " + previousNode + "\n To " + newNode + "\n, clearance amount in the moving foot is only " + offsetVector + "\n";

         assertFalse(nodeChecker.isNodeValid(newNode, previousNode));
         testListener.assertCorrectRejection(message, newNode, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_IN_PLACE);
      }

      // check stepping with enough clearance
      for (int iter = 0; iter < 10; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);

         clearanceVector.changeFrame(nodeFrame);
         FrameVector2D offsetVector = new FrameVector2D(clearanceVector);
         double scaleFactor = RandomNumbers.nextDouble(random, 1.1, 1.15);
         offsetVector.scale(scaleFactor);
         if (RandomNumbers.nextBoolean(random, 0.5))
            offsetVector.setX(-offsetVector.getX());

         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedFrontLeft = new FramePoint2D(frontLeft);
         shiftedFrontLeft.add(offsetVector);



         FootstepNode newNode = new FootstepNode(robotQuadrant, shiftedFrontLeft, frontRight, hindLeft, hindRight, 1.0, 0.5);


         frontLeft.changeFrame(nodeFrame);
         shiftedFrontLeft.changeFrame(nodeFrame);
         frontRight.changeFrame(nodeFrame);
         clearanceVector.changeFrame(nodeFrame);
         offsetVector.changeFrame(nodeFrame);

         message = "Stepping from " + previousNode + "\n To " + newNode + "\n";
         message += "Front left = " + frontLeft + "\nShifted front left = " + shiftedFrontLeft + "\nFront right = " + frontRight + "\nOffset = " + offsetVector + "\nRequired clearance = " + clearanceVector + "\n";

         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, null, null, null);
         assertTrue(isValid);
      }
   }

   @Test
   public void testStepTooFarInwardFrontLeft()
   {
      double stanceLength = 1.0;
      double stanceWidth = 0.5;
      FootstepPlannerParameters parameters = new DefaultFootstepPlannerParameters()
      {
         @Override
         public double getMinXClearanceFromFoot()
         {
            return 0.0;
         }

         @Override
         public double getMinYClearanceFromFoot()
         {
            return 0.0;
         }
      };
      SimplePlanarRegionFootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(parameters);
      FootstepNodeChecker nodeChecker = new SnapBasedNodeChecker(parameters, snapper);

      TestListener testListener = new TestListener();
      nodeChecker.addPlannerListener(testListener);

      // set up kind of a random ground plane
      ConvexPolygon2D groundPlanePolygon = new ConvexPolygon2D();
      groundPlanePolygon.addVertex(10.0, 10.0);
      groundPlanePolygon.addVertex(-10.0, 10.0);
      groundPlanePolygon.addVertex(10.0, -10.0);
      groundPlanePolygon.addVertex(-10.0, -10.0);
      groundPlanePolygon.update();
      AxisAngle normal = new AxisAngle(new Vector3D(0.05, 0.05, 0.8), Math.PI / 6);
      Vector3D translation = new Vector3D(0.78, 0.9, 0.12);
      RigidBodyTransform transform = new RigidBodyTransform(normal, translation);
      PlanarRegionsList planarRegionList = new PlanarRegionsList();
      planarRegionList.addPlanarRegion(new PlanarRegion(transform, groundPlanePolygon));

      PoseReferenceFrame footstepFrame = new PoseReferenceFrame("footstepFrame", worldFrame);
      footstepFrame.setPoseAndUpdate(new Point3D(0.6, 0.9, 0.0), new AxisAngle(-Math.PI / 4, 0.0, 0.0));

      // have to set this really small, so that we don't get a ton of rounding issues
      FootstepNode.gridSizeXY = 0.001;
      RobotQuadrant robotQuadrant = RobotQuadrant.FRONT_LEFT;

      FramePoint2D frontLeft = new FramePoint2D(footstepFrame, 1.57, 0.67);
      FramePoint2D frontRight = new FramePoint2D(footstepFrame, 1.63, 0.12);
      FramePoint2D hindLeft = new FramePoint2D(footstepFrame, 0.67, 0.72);
      FramePoint2D hindRight = new FramePoint2D(footstepFrame, 0.73, 0.05); // way further out


      frontLeft.changeFrame(worldFrame);
      frontRight.changeFrame(worldFrame);
      hindLeft.changeFrame(worldFrame);
      hindRight.changeFrame(worldFrame);

      FootstepNode previousNode = new FootstepNode(robotQuadrant, frontLeft, frontRight, hindLeft, hindRight, stanceLength, stanceWidth);



      PoseReferenceFrame nodeFrame = new PoseReferenceFrame("nodeFrame", worldFrame);
      nodeFrame.setPoseAndUpdate(new Point3D(previousNode.getOrComputeXGaitCenterPoint()), new AxisAngle(previousNode.getNominalYaw(), 0.0, 0.0));


      frontLeft.changeFrame(nodeFrame);
      frontRight.changeFrame(nodeFrame);
      hindLeft.changeFrame(nodeFrame);
      hindRight.changeFrame(nodeFrame);

      double minXGaitCenterYFromRightSide = Math.max(FootstepNode.gridSizeXY * FootstepNode.snapToGrid(frontRight.getY()), FootstepNode.gridSizeXY * FootstepNode.snapToGrid(hindRight.getY())) + 0.5 * stanceWidth;
      double minXGaitCenterYFromLeftSide = Math.max(FootstepNode.gridSizeXY * FootstepNode.snapToGrid(frontLeft.getY()), FootstepNode.gridSizeXY * FootstepNode.snapToGrid(hindLeft.getY())) - 0.5 * stanceWidth;


      double minStartingFootY = Math.max(minXGaitCenterYFromLeftSide, minXGaitCenterYFromRightSide) + 0.5 * stanceWidth;
      double effectiveMinStepWidth = minStartingFootY + parameters.getMinimumStepWidth() - frontLeft.getY();



      // check stepping within the reach
      Random random = new Random(1738L);
      for (int iter = 0; iter < 50; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);
         hindRight.changeFrame(worldFrame);
         hindLeft.changeFrame(worldFrame);

         FrameVector2D offsetVector = new FrameVector2D(nodeFrame, 0.0, effectiveMinStepWidth);
         double scaleFactor = RandomNumbers.nextDouble(random, 0.05, 0.95);
         offsetVector.scale(scaleFactor);
         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedFrontLeft = new FramePoint2D(frontLeft);
         shiftedFrontLeft.add(offsetVector);


         FootstepNode newNode = new FootstepNode(robotQuadrant, shiftedFrontLeft, frontRight, hindLeft, hindRight, stanceLength, stanceWidth);

         FrameVector2D actualShift = new FrameVector2D();
         actualShift.set(newNode.getX(robotQuadrant) - previousNode.getX(robotQuadrant), newNode.getY(robotQuadrant) - previousNode.getY(robotQuadrant));
         actualShift.changeFrame(nodeFrame);


         frontLeft.changeFrame(footstepFrame);
         shiftedFrontLeft.changeFrame(footstepFrame);
         offsetVector.changeFrame(footstepFrame);
         String message = "Stepping from " + previousNode + " to " + newNode + "\n";
         message += "Front left = " + frontLeft + "\n";
         message += "Shifted front left = " + shiftedFrontLeft + "\n";
         message += "Shift = " + offsetVector + "\n";
         message += "Actual shift = " + actualShift + "\n";
         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, null, null, null);
         assertTrue(isValid);
      }

      // check stepping around reach max
      for (int iter = 0; iter < 50; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);
         hindRight.changeFrame(worldFrame);
         hindLeft.changeFrame(worldFrame);

         FrameVector2D offsetVector = new FrameVector2D(footstepFrame, 0.0, effectiveMinStepWidth);
         double scaleFactor = RandomNumbers.nextDouble(random, 0.95, 0.99);
         offsetVector.scale(scaleFactor);
         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedFrontLeft = new FramePoint2D(frontLeft);
         shiftedFrontLeft.add(offsetVector);

         FootstepNode newNode = new FootstepNode(robotQuadrant, shiftedFrontLeft, frontRight, hindLeft, hindRight, stanceLength, stanceWidth);

         FrameVector2D actualShift = new FrameVector2D();
         actualShift.set(newNode.getX(robotQuadrant) - previousNode.getX(robotQuadrant), newNode.getY(robotQuadrant) - previousNode.getY(robotQuadrant));
         actualShift.changeFrame(footstepFrame);


         frontLeft.changeFrame(footstepFrame);
         shiftedFrontLeft.changeFrame(footstepFrame);
         offsetVector.changeFrame(footstepFrame);
         String message = "Stepping from " + previousNode + " to " + newNode + "\n";
         message += "Front left = " + frontLeft + "\n";
         message += "Shifted front left = " + shiftedFrontLeft + "\n";
         message += "Shift = " + offsetVector + "\n";
         message += "Actual shift = " + actualShift + "\n";
         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, null, null, null);
         assertTrue(isValid);
      }

      // check stepping past the reach
      for (int iter = 0; iter < 50; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);
         hindRight.changeFrame(worldFrame);
         hindLeft.changeFrame(worldFrame);

         FrameVector2D offsetVector = new FrameVector2D(footstepFrame, 0.0, effectiveMinStepWidth);
         double scaleFactor = RandomNumbers.nextDouble(random, 1.01, 1.2);
         offsetVector.scale(scaleFactor);
         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedFrontLeft = new FramePoint2D(frontLeft);
         shiftedFrontLeft.add(offsetVector);


         FootstepNode newNode = new FootstepNode(robotQuadrant, shiftedFrontLeft, frontRight, hindLeft, hindRight, stanceLength, stanceWidth);

         FrameVector2D actualShift = new FrameVector2D();
         actualShift.set(newNode.getX(robotQuadrant) - previousNode.getX(robotQuadrant), newNode.getY(robotQuadrant) - previousNode.getY(robotQuadrant));
         actualShift.changeFrame(footstepFrame);

         frontLeft.changeFrame(footstepFrame);
         shiftedFrontLeft.changeFrame(footstepFrame);
         offsetVector.changeFrame(footstepFrame);
         String message = "Stepping from " + previousNode + " to " + newNode + "\n";
         message += "Front left = " + frontLeft + "\n";
         message += "Shifted front left = " + shiftedFrontLeft + "\n";
         message += "Shift = " + offsetVector + "\n";
         message += "Actual shift = " + actualShift + "\n";
         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, newNode, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_INWARD);
         assertFalse(isValid);
      }
   }

   @Test
   public void testStepTooFarInwardHindRight()
   {
      double stanceLength = 1.0;
      double stanceWidth = 0.5;
      FootstepPlannerParameters parameters = new DefaultFootstepPlannerParameters()
      {
         @Override
         public double getMinXClearanceFromFoot()
         {
            return 0.0;
         }

         @Override
         public double getMinYClearanceFromFoot()
         {
            return 0.0;
         }
      };
      SimplePlanarRegionFootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(parameters);
      FootstepNodeChecker nodeChecker = new SnapBasedNodeChecker(parameters, snapper);

      TestListener testListener = new TestListener();
      nodeChecker.addPlannerListener(testListener);

      // set up kind of a random ground plane
      ConvexPolygon2D groundPlanePolygon = new ConvexPolygon2D();
      groundPlanePolygon.addVertex(10.0, 10.0);
      groundPlanePolygon.addVertex(-10.0, 10.0);
      groundPlanePolygon.addVertex(10.0, -10.0);
      groundPlanePolygon.addVertex(-10.0, -10.0);
      groundPlanePolygon.update();
      AxisAngle normal = new AxisAngle(new Vector3D(0.05, 0.05, 0.8), Math.PI / 6);
      Vector3D translation = new Vector3D(0.78, 0.9, 0.12);
      RigidBodyTransform transform = new RigidBodyTransform(normal, translation);
      PlanarRegionsList planarRegionList = new PlanarRegionsList();
      planarRegionList.addPlanarRegion(new PlanarRegion(transform, groundPlanePolygon));

      PoseReferenceFrame footstepFrame = new PoseReferenceFrame("footstepFrame", worldFrame);
      footstepFrame.setPoseAndUpdate(new Point3D(0.6, 0.9, 0.0), new AxisAngle(-Math.PI / 4, 0.0, 0.0));

      // have to set this really small, so that we don't get a ton of rounding issues
      FootstepNode.gridSizeXY = 0.001;
      RobotQuadrant robotQuadrant = RobotQuadrant.HIND_RIGHT;

      FramePoint2D frontLeft = new FramePoint2D(footstepFrame, 1.57, 0.67);
      FramePoint2D frontRight = new FramePoint2D(footstepFrame, 1.63, 0.12);
      FramePoint2D hindLeft = new FramePoint2D(footstepFrame, 0.67, 0.72);
      FramePoint2D hindRight = new FramePoint2D(footstepFrame, 0.73, 0.05);


      frontLeft.changeFrame(worldFrame);
      frontRight.changeFrame(worldFrame);
      hindLeft.changeFrame(worldFrame);
      hindRight.changeFrame(worldFrame);

      FootstepNode previousNode = new FootstepNode(robotQuadrant, frontLeft, frontRight, hindLeft, hindRight, stanceLength, stanceWidth);



      PoseReferenceFrame nodeFrame = new PoseReferenceFrame("nodeFrame", worldFrame);
      nodeFrame.setPoseAndUpdate(new Point3D(previousNode.getOrComputeXGaitCenterPoint()), new AxisAngle(previousNode.getNominalYaw(), 0.0, 0.0));


      frontLeft.changeFrame(nodeFrame);
      frontRight.changeFrame(nodeFrame);
      hindLeft.changeFrame(nodeFrame);
      hindRight.changeFrame(nodeFrame);

      double minXGaitCenterYFromRightSide = Math.min(FootstepNode.gridSizeXY * FootstepNode.snapToGrid(frontRight.getY()), FootstepNode.gridSizeXY * FootstepNode.snapToGrid(hindRight.getY())) + 0.5 * stanceWidth;
      double minXGaitCenterYFromLeftSide = Math.min(FootstepNode.gridSizeXY * FootstepNode.snapToGrid(frontLeft.getY()), FootstepNode.gridSizeXY * FootstepNode.snapToGrid(hindLeft.getY())) - 0.5 * stanceWidth;


      double minStartingFootY = Math.max(minXGaitCenterYFromLeftSide, minXGaitCenterYFromRightSide) + 0.5 * stanceWidth;
      double effectiveMinStepWidth = minStartingFootY + parameters.getMinimumStepWidth() - frontLeft.getY();



      // check stepping within the reach
      Random random = new Random(1738L);
      for (int iter = 0; iter < 50; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);
         hindRight.changeFrame(worldFrame);
         hindLeft.changeFrame(worldFrame);

         FrameVector2D offsetVector = new FrameVector2D(nodeFrame, 0.0, -effectiveMinStepWidth);
         double scaleFactor = RandomNumbers.nextDouble(random, 0.05, 0.95);
         offsetVector.scale(scaleFactor);
         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedHindRight = new FramePoint2D(hindRight);
         shiftedHindRight.add(offsetVector);


         FootstepNode newNode = new FootstepNode(robotQuadrant, frontLeft, frontRight, hindLeft, shiftedHindRight, stanceLength, stanceWidth);

         FrameVector2D actualShift = new FrameVector2D();
         actualShift.set(newNode.getX(robotQuadrant) - previousNode.getX(robotQuadrant), newNode.getY(robotQuadrant) - previousNode.getY(robotQuadrant));
         actualShift.changeFrame(nodeFrame);


         hindRight.changeFrame(footstepFrame);
         shiftedHindRight.changeFrame(footstepFrame);
         offsetVector.changeFrame(footstepFrame);
         String message = "Stepping from " + previousNode + " to " + newNode + "\n";
         message += "Hind right = " + hindRight + "\n";
         message += "Shifted hind right = " + shiftedHindRight + "\n";
         message += "Shift = " + offsetVector + "\n";
         message += "Actual shift = " + actualShift + "\n";
         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, null, null, null);
         assertTrue(isValid);
      }

      // check stepping around reach max
      for (int iter = 0; iter < 50; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);
         hindRight.changeFrame(worldFrame);
         hindLeft.changeFrame(worldFrame);

         FrameVector2D offsetVector = new FrameVector2D(nodeFrame, 0.0, -effectiveMinStepWidth);
         double scaleFactor = RandomNumbers.nextDouble(random, 0.95, 0.99);
         offsetVector.scale(scaleFactor);
         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedHindRight = new FramePoint2D(hindRight);
         shiftedHindRight.add(offsetVector);

         FootstepNode newNode = new FootstepNode(robotQuadrant, frontLeft, frontRight, hindLeft, shiftedHindRight, stanceLength, stanceWidth);

         FrameVector2D actualShift = new FrameVector2D();
         actualShift.set(newNode.getX(robotQuadrant) - previousNode.getX(robotQuadrant), newNode.getY(robotQuadrant) - previousNode.getY(robotQuadrant));
         actualShift.changeFrame(footstepFrame);


         hindRight.changeFrame(footstepFrame);
         shiftedHindRight.changeFrame(footstepFrame);
         offsetVector.changeFrame(footstepFrame);
         String message = "Stepping from " + previousNode + " to " + newNode + "\n";
         message += "Hind right = " + hindRight + "\n";
         message += "Shifted hind right = " + shiftedHindRight + "\n";
         message += "Shift = " + offsetVector + "\n";
         message += "Actual shift = " + actualShift + "\n";
         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, null, null, null);
         assertTrue(isValid);
      }

      // check stepping past the reach
      for (int iter = 0; iter < 50; iter++)
      {
         frontLeft.changeFrame(worldFrame);
         frontRight.changeFrame(worldFrame);
         hindRight.changeFrame(worldFrame);
         hindLeft.changeFrame(worldFrame);

         FrameVector2D offsetVector = new FrameVector2D(nodeFrame, 0.0, -effectiveMinStepWidth);
         double scaleFactor = RandomNumbers.nextDouble(random, 1.01, 1.2);
         offsetVector.scale(scaleFactor);
         offsetVector.changeFrame(worldFrame);

         FramePoint2D shiftedHindRight = new FramePoint2D(hindRight);
         shiftedHindRight.add(offsetVector);


         FootstepNode newNode = new FootstepNode(robotQuadrant, frontLeft, frontRight, hindLeft, shiftedHindRight, stanceLength, stanceWidth);

         FrameVector2D actualShift = new FrameVector2D();
         actualShift.set(newNode.getX(robotQuadrant) - previousNode.getX(robotQuadrant), newNode.getY(robotQuadrant) - previousNode.getY(robotQuadrant));
         actualShift.changeFrame(footstepFrame);

         hindRight.changeFrame(footstepFrame);
         shiftedHindRight.changeFrame(footstepFrame);
         offsetVector.changeFrame(footstepFrame);
         String message = "Stepping from " + previousNode + " to " + newNode + "\n";
         message += "Hind right = " + hindRight + "\n";
         message += "Shifted hind right = " + shiftedHindRight + "\n";
         message += "Shift = " + offsetVector + "\n";
         message += "Actual shift = " + actualShift + "\n";
         boolean isValid = nodeChecker.isNodeValid(newNode, previousNode);
         testListener.assertCorrectRejection(message, newNode, previousNode, QuadrupedFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_INWARD);
         assertFalse(isValid);
      }


   }

   private class TestListener implements QuadrupedFootstepPlannerListener
   {
      private final AtomicReference<QuadrupedFootstepPlannerNodeRejectionReason> reason = new AtomicReference<>();
      private final AtomicReference<FootstepNode> rejectedNode = new AtomicReference<>();
      private final AtomicReference<FootstepNode> rejectedParentNode = new AtomicReference<>();

      @Override
      public void addNode(FootstepNode node, FootstepNode previousNode)
      {

      }

      public void assertCorrectRejection(String message, FootstepNode node, FootstepNode parentNode, QuadrupedFootstepPlannerNodeRejectionReason rejectionReason)
      {
         assertEquals(message, rejectionReason, reason.getAndSet(null));
         assertEquals(message, node, rejectedNode.getAndSet(null));
         assertEquals(message, parentNode, rejectedParentNode.getAndSet(null));
      }

      @Override
      public void rejectNode(FootstepNode node, FootstepNode parentNode, QuadrupedFootstepPlannerNodeRejectionReason rejectionReason)
      {
         rejectedNode.set(node);
         rejectedParentNode.set(parentNode);
         reason.set(rejectionReason);
      }

      @Override
      public void plannerFinished(List<FootstepNode> plan)
      {

      }

      @Override
      public void reportLowestCostNodeList(List<FootstepNode> plan)
      {

      }

      @Override
      public void tickAndUpdate()
      {

      }
   }
}
