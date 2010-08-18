package us.ihmc.commonWalkingControlModules.captureRegion;

import java.awt.Color;
import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.SideDependentList;
import us.ihmc.commonWalkingControlModules.timing.GlobalTimer;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoAppearance;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.DynamicGraphicYoPolygonArtifact;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameConvexPolygon2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;


//import us.ihmc.plotting.shapes.PointArtifact;

/**
 * <p>Title: YoboticsBipedCaptureRegionCalculator</p>
 *
 * <p>Description: Computes the Capture Region, using the Capture Point, the line of sight vertices on the support foot, and the swing time remaining.</p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: </p>
 *
 * @author Yobotics-IHMC biped team (with an assist by Michigan)
 * @version 1.0
 */
public class CaptureRegionCalculator
{
   // Warning! The following may not be rewindable if not made a Yo...
// private FrameConvexPolygon2d captureRegion;

   private final SideDependentList<FrameConvexPolygon2d> reachableRegions;

   private final YoFrameConvexPolygon2d captureRegionGraphic;
   private final YoFramePoint[] captureRegionBestCaseVertices;
   private final YoFramePoint[] captureRegionKinematicLimitVertices;
   private final YoFramePoint[] estimatedCOPExtremes;

   private final YoFramePoint[] additionalKinematicLimitPoints;

   private ReferenceFrame worldFrame, bodyZUpFrame;
   private final SideDependentList<ReferenceFrame> ankleZUpFrames;
      private CapturePointCalculatorInterface capturePointCalculator;

   public static boolean DRAW_CAPTURE_REGION = true;    //
   public static final double DRAWN_POINT_BASE_SIZE = 0.004;

   public static final double KINEMATIC_RANGE_FROM_COP = 0.7;    // 0.6; //0.7;
   public static final int NUMBER_OF_POINTS_TO_APPROXIMATE_KINEMATIC_LIMITS = 5;    // 3; //1; //10; //
   public static final int MAX_CAPTURE_REGION_POLYGON_POINTS = 20;    // 4 + NUMBER_OF_POINTS_TO_APPROXIMATE_KINEMATIC_LIMITS + 8;

   public static final double SWING_TIME_TO_ADD_FOR_CAPTURING_SAFETY_FACTOR = 0.05;    // 0.1; //

// public static final double FINAL_CAPTURE_REGION_SAFETY_MARGIN = 0.5; //0.1; //

//   private RobotSide currentSide = null;
//   private int index = 0;
//   private boolean DRAW_SCORE_ON_GROUND = false;    // true;

//   private final StepLocationScorer weightedDistanceScorer;

   private GlobalTimer globalTimer;

   public CaptureRegionCalculator(SideDependentList<ReferenceFrame> ankleZUpFrames, ReferenceFrame bodyZUpFrame, double midFootAnkleXOffset, double footWidth,
                                      CapturePointCalculatorInterface capturePointCalculator, YoVariableRegistry yoVariableRegistry,
                                      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.worldFrame = ReferenceFrame.getWorldFrame();
      this.bodyZUpFrame = bodyZUpFrame;
      this.ankleZUpFrames = ankleZUpFrames;
      
      this.capturePointCalculator = capturePointCalculator;

      int numPoints = MAX_CAPTURE_REGION_POLYGON_POINTS - 1;
      double[][] reachableRegionPoints = new double[numPoints + 1][2];
      double radius = KINEMATIC_RANGE_FROM_COP;

      for (int i = 0; i < numPoints; i++)
      {
         double angle = -0.03 * Math.PI - 0.70 * Math.PI * ((double) i) / ((double) (numPoints - 1));

         double x = radius * Math.cos(angle);
         double y = radius * Math.sin(angle);

         reachableRegionPoints[i][0] = x + midFootAnkleXOffset; 
         reachableRegionPoints[i][1] = y;
      }

      reachableRegionPoints[numPoints][0] = 0.0;

//    reachableRegionPoints[numPoints][1] = 0.0;
      reachableRegionPoints[numPoints][1] = -footWidth/2.0; 

      FrameConvexPolygon2d leftReachableRegionInSupportFootFrame = new FrameConvexPolygon2d(ankleZUpFrames.get(RobotSide.LEFT), reachableRegionPoints);

      reachableRegionPoints = new double[numPoints + 1][2];

      for (int i = 0; i < numPoints; i++)
      {
         double angle = 0.03 * Math.PI + 0.70 * Math.PI * ((double) i) / ((double) (numPoints - 1));

         double x = radius * Math.cos(angle);
         double y = radius * Math.sin(angle);

         reachableRegionPoints[i][0] = x;
         reachableRegionPoints[i][1] = y;
      }

      reachableRegionPoints[numPoints][0] = 0.0;

//    reachableRegionPoints[numPoints][1] = 0.0;
      reachableRegionPoints[numPoints][1] = footWidth/2.0;

      FrameConvexPolygon2d rightReachableRegionInSupportFootFrame = new FrameConvexPolygon2d(ankleZUpFrames.get(RobotSide.RIGHT), reachableRegionPoints);

      this.reachableRegions = new SideDependentList<FrameConvexPolygon2d>(leftReachableRegionInSupportFootFrame, rightReachableRegionInSupportFootFrame);


      captureRegionBestCaseVertices = new YoFramePoint[3];
      captureRegionKinematicLimitVertices = new YoFramePoint[3];
      estimatedCOPExtremes = new YoFramePoint[3];
      additionalKinematicLimitPoints = new YoFramePoint[NUMBER_OF_POINTS_TO_APPROXIMATE_KINEMATIC_LIMITS];

      YoVariableRegistry registry = new YoVariableRegistry("captureRegion");
      globalTimer = new GlobalTimer("captureRegionCalculator", registry);



      // Set up the scoring function:
      SideDependentList<ReferenceFrame> footZUpFrames = new SideDependentList<ReferenceFrame>(ankleZUpFrames.get(RobotSide.LEFT),
                                                           ankleZUpFrames.get(RobotSide.RIGHT));

//      // set up scorer
//      DoubleYoVariable stanceWidthForScore = new DoubleYoVariable("stanceWidthForScore", "Stance width for the scoring function.", registry);
//      DoubleYoVariable stanceLengthForScore = new DoubleYoVariable("stanceLengthForScore", "Stance length for the scoring function.", registry);
//      DoubleYoVariable stepAngleForScore = new DoubleYoVariable("stepAngleForScore", "Step angle for the scoring function.", registry);
//      DoubleYoVariable stepDistanceForScore = new DoubleYoVariable("stepDistanceForScore", "Step distance for the scoring function.", registry);
//
//      stanceWidthForScore.set(0.22);
//      stanceLengthForScore.set(M2V2CaptureRegionCalculator.KINEMATIC_RANGE_FROM_COP * 0.7);    // 0.40
//      stepAngleForScore.set(Math.atan(stanceWidthForScore.getDoubleValue() / stanceLengthForScore.getDoubleValue()));
//      stepDistanceForScore.set(Math.sqrt((stanceWidthForScore.getDoubleValue() * stanceWidthForScore.getDoubleValue())
//                                         + (stanceLengthForScore.getDoubleValue() * stanceLengthForScore.getDoubleValue())));

//      weightedDistanceScorer = new WeightedDistanceScorer(this, footZUpFrames, stanceWidthForScore, stanceLengthForScore, stepAngleForScore,
//              stepDistanceForScore);



      // Done setting up the scoring function

      DynamicGraphicObjectsList dynamicGraphicObjectsList = null;
      ArtifactList artifactList = null;
      captureRegionGraphic = new YoFrameConvexPolygon2d("Capture Region", "", worldFrame, MAX_CAPTURE_REGION_POLYGON_POINTS, registry);

      if (dynamicGraphicObjectsListRegistry == null)
      {
         DRAW_CAPTURE_REGION = false;
      }

      if (DRAW_CAPTURE_REGION && (dynamicGraphicObjectsListRegistry != null))
      {
         dynamicGraphicObjectsList = new DynamicGraphicObjectsList("CaptureRegionCalculator");
         artifactList = new ArtifactList("CaptureRegionCalculator");
                  
         DynamicGraphicYoPolygonArtifact dynamicGraphicYoPolygonArtifact = new DynamicGraphicYoPolygonArtifact("Capture Region", captureRegionGraphic, Color.LIGHT_GRAY, false);
         
         artifactList.add(dynamicGraphicYoPolygonArtifact);
//         YoboticsBipedPlotter.registerArtifact(dynamicGraphicYoPolygonArtifact);

//         YoboticsBipedPlotter.registerDynamicGraphicPolygon("Capture Region", Color.LIGHT_GRAY, captureRegionGraphic, false);
      }



      for (int i = 0; i < captureRegionBestCaseVertices.length; i++)
      {
         String pointName = "captureRegionBestCaseVertex" + i;
         captureRegionBestCaseVertices[i] = new YoFramePoint(pointName, "", worldFrame, registry);

         if (DRAW_CAPTURE_REGION && (dynamicGraphicObjectsListRegistry != null))
         {
            DynamicGraphicPosition position = new DynamicGraphicPosition("Position", captureRegionBestCaseVertices[i], DRAWN_POINT_BASE_SIZE * ((4 + i) / 4),
                                                 YoAppearance.Green(), DynamicGraphicPosition.GraphicType.BALL);
            dynamicGraphicObjectsList.add(position);
//            YoboticsBipedPlotter.registerDynamicGraphicPosition(pointName, position);
            
            artifactList.add(position.createArtifact());
//            YoboticsBipedPlotter.registerDynamicGraphicObject(position);
         }
      }

      for (int i = 0; i < captureRegionKinematicLimitVertices.length; i++)
      {
         String pointName = "captureRegionKinematicLimitVertex" + i;
         captureRegionKinematicLimitVertices[i] = new YoFramePoint(pointName, "", worldFrame, registry);

         if (DRAW_CAPTURE_REGION && (dynamicGraphicObjectsListRegistry != null))
         {
            DynamicGraphicPosition position = new DynamicGraphicPosition(pointName, captureRegionKinematicLimitVertices[i], DRAWN_POINT_BASE_SIZE * ((4 + i) / 4),
                                                 YoAppearance.Blue(), DynamicGraphicPosition.GraphicType.BALL);
            dynamicGraphicObjectsList.add(position);
//            YoboticsBipedPlotter.registerDynamicGraphicPosition(pointName, position);
            
            artifactList.add(position.createArtifact());
//            YoboticsBipedPlotter.registerDynamicGraphicObject(position);
         }
      }

      for (int i = 0; i < estimatedCOPExtremes.length; i++)
      {
         String pointName = "estimatedCOPExtreme" + i;
         estimatedCOPExtremes[i] = new YoFramePoint(pointName, "", worldFrame, registry);

         if (DRAW_CAPTURE_REGION && (dynamicGraphicObjectsListRegistry != null))
         {
            DynamicGraphicPosition position = new DynamicGraphicPosition(pointName, estimatedCOPExtremes[i], DRAWN_POINT_BASE_SIZE * ((4 + i) / 4), YoAppearance.Black(),
                                                 DynamicGraphicPosition.GraphicType.BALL);
            dynamicGraphicObjectsList.add(position);
//            YoboticsBipedPlotter.registerDynamicGraphicPosition(pointName, position);
            
            artifactList.add(position.createArtifact());
//            YoboticsBipedPlotter.registerDynamicGraphicObject(position);
         }
      }


      for (int i = 0; i < additionalKinematicLimitPoints.length; i++)
      {
         String pointName = "additionalKinematicLimitPoint" + i;
         additionalKinematicLimitPoints[i] = new YoFramePoint(pointName, "", worldFrame, registry);

         if (DRAW_CAPTURE_REGION && (dynamicGraphicObjectsListRegistry != null))
         {
            DynamicGraphicPosition position = new DynamicGraphicPosition(pointName, additionalKinematicLimitPoints[i], DRAWN_POINT_BASE_SIZE * 0.5, YoAppearance.Aqua(),
                                                 DynamicGraphicPosition.GraphicType.BALL);

            dynamicGraphicObjectsList.add(position);
//            YoboticsBipedPlotter.registerDynamicGraphicPosition(pointName, position);
            
            artifactList.add(position.createArtifact());
//            YoboticsBipedPlotter.registerDynamicGraphicObject(position);
         }
      }

      if (DRAW_CAPTURE_REGION && (dynamicGraphicObjectsListRegistry != null))
      {
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }

      if (yoVariableRegistry != null)
      {
         yoVariableRegistry.addChild(registry);
      }
   }


   public FrameConvexPolygon2d calculateCaptureRegion(RobotSide supportLeg, FrameConvexPolygon2d supportFoot, double swingTimeRemaining)
   {
      // The general idea is to predict where we think we can drive the capture point
      // to by the end of the swing (as determined just by swing time), given our current COM state and support foot. We first
      // find the extremes on the foot where we can put the COP, then predict where each extreme would drive us.
      // We will also consider the points between the line of sight points to get the inner points of the polygon.
      // Since the foot is convex, these lines should be convex too.
      // If we step
      // in front of these points, we would fall to the outside of the swing, so we have to step beyond these points. The
      // limits on the extent we can reach to is dominated by kinematics, so we will just choose a conservative distance to
      // specify the distal constraints of the capture region. See Jerry Pratt's or John Rebula's notes from 2009.1.4.
      // We also will have a reachable region that we intersect with. If the Capture Point is inside the foot, then we'll just
      // return the reachable region.

      globalTimer.startTimer();
      ReferenceFrame supportAnkleZUpFrame = ankleZUpFrames.get(supportLeg);

      // first get all of the objects we will need to calculate the capture region
      FramePoint2d capturePoint = capturePointCalculator.getCapturePoint2dInFrame(supportAnkleZUpFrame);
      FramePoint2d footCentroid = supportFoot.getCentroidCopy();

      // 0. hmm, wierd things are happening when we predict the capture point given the cop extremes as calculated by
      // the line of sightlines from the capture point when the capture point is close to the foot polygon. Let's try returning
      // no capture region when the center of mass is still over the support polygon

//    FramePoint com = processedSensors.centerOfMassBodyZUp.getFramePointCopy().changeFrameCopy(supportAnkleZUpFrame);
//    FramePoint2d com2d = new FramePoint2d(com.getReferenceFrame(), com.getX(), com.getY());
//    FramePoint2d[] extremesOfFeasibleCOPFromCOM = supportFoot.getLineOfSightVertices(com2d);
//    if(extremesOfFeasibleCOPFromCOM == null)
//    {
//       return null;
//    }

      // 1. find visible points on polygon...
//    FramePoint2d[] extremesOfFeasibleCOP = supportFoot.getLineOfSightVertices(capturePoint);

      ArrayList<FramePoint2d> extremesOfFeasibleCOP = supportFoot.getAllVisibleVerticesFromOutsideLeftToRight(capturePoint);

      if (extremesOfFeasibleCOP == null)    // Inside the polygon, Capture Region is everywhere reachable. Make it reachable region...
      {
         FrameConvexPolygon2d captureRegion = reachableRegions.get(supportLeg);

         if (DRAW_CAPTURE_REGION)
            captureRegionGraphic.setFrameConvexPolygon2d(captureRegion.changeFrameCopy(worldFrame));
         globalTimer.stopTimer();

         return captureRegion;
      }


      // 2. predict extreme capture points
      FrameVector2d[] directionLimits = new FrameVector2d[extremesOfFeasibleCOP.size()];
      ArrayList<FramePoint2d> captureRegionVertices = new ArrayList<FramePoint2d>();
      for (int i = 0; i < extremesOfFeasibleCOP.size(); i++)
      {
         FramePoint2d copExtremeInWorld = extremesOfFeasibleCOP.get(i).changeFrameCopy(worldFrame);
         if (i < estimatedCOPExtremes.length)
            estimatedCOPExtremes[i].set(copExtremeInWorld.getX(), copExtremeInWorld.getY(), 0.0);

         FramePoint2d copExtremeInBodyZUp = extremesOfFeasibleCOP.get(i).changeFrameCopy(bodyZUpFrame);
         FramePoint2d copExtremeInSupportAnkleZUp = copExtremeInBodyZUp.changeFrameCopy(supportAnkleZUpFrame);
         FramePoint copExtremeInBodyZUp3d = new FramePoint(bodyZUpFrame, copExtremeInBodyZUp.getX(),
                                               copExtremeInBodyZUp.getY(), 0.0);

         capturePointCalculator.computePredictedCapturePoint(supportLeg, swingTimeRemaining + SWING_TIME_TO_ADD_FOR_CAPTURING_SAFETY_FACTOR,
                 copExtremeInBodyZUp3d, null);

         FramePoint predictedExtremeCapturePoint = capturePointCalculator.getPredictedCapturePointInFrame(supportAnkleZUpFrame);
         FramePoint2d predictedExtremeCapturePoint2d = new FramePoint2d(predictedExtremeCapturePoint.getReferenceFrame(), predictedExtremeCapturePoint.getX(),
                                                          predictedExtremeCapturePoint.getY());
         captureRegionVertices.add(predictedExtremeCapturePoint2d);

         // update position in plotter
         if (DRAW_CAPTURE_REGION)
         {
            FramePoint predictedCapturePointInWorld = predictedExtremeCapturePoint.changeFrameCopy(worldFrame);
            if (i < captureRegionBestCaseVertices.length)
               captureRegionBestCaseVertices[i].set(predictedCapturePointInWorld);
         }

         // 3. project from cop extremes to capture point extremes, find point at a given distance to determine kinematic limits of the capture region.
         FrameVector2d projectedLine = new FrameVector2d(predictedExtremeCapturePoint2d);
         projectedLine.sub(copExtremeInSupportAnkleZUp);

         // Look at JPratt Notes February 18, 2009 for details on the following:
         FramePoint2d kinematicExtreme = solveIntersectionOfRayAndCircle(footCentroid, predictedExtremeCapturePoint2d, projectedLine, KINEMATIC_RANGE_FROM_COP);
         if (kinematicExtreme == null)
         {
            FrameConvexPolygon2d captureRegion = null;

            if (DRAW_CAPTURE_REGION)
               captureRegionGraphic.setFrameConvexPolygon2d(null);
            globalTimer.stopTimer();

            return captureRegion;
         }


//       // Don't invert the Capture Region. IF the projected line isn't kinematically reachable, then the Capture Region is null.
//       if (projectedLine.lengthSquared() > KINEMATIC_RANGE_FROM_COP * KINEMATIC_RANGE_FROM_COP)
//       {
//          FrameConvexPolygon2d captureRegion = null;
//
//          if (DRAW_CAPTURE_REGION) captureRegionGraphic.setFrameConvexPolygon2d(null);
//          globalTimer.stopTimer();
//          return captureRegion;
//       }
//
//       projectedLine.normalize();
//       projectedLine.scale(KINEMATIC_RANGE_FROM_COP);
//       projectedLine.add(copExtremeInSupportAnkleZUp);
//
//       FramePoint2d kinematicExtreme = new FramePoint2d(projectedLine.getReferenceFrame(), projectedLine.getX(), projectedLine.getY());
         captureRegionVertices.add(kinematicExtreme);

         FrameVector2d footToKinematicLimitDirection = new FrameVector2d(kinematicExtreme);
         footToKinematicLimitDirection.sub(footCentroid);
         directionLimits[i] = footToKinematicLimitDirection;

         // update position in plotter
         if (DRAW_CAPTURE_REGION)
         {
            FramePoint2d kinematicExtremeInWorld = kinematicExtreme.changeFrameCopy(worldFrame);
            if (i < captureRegionKinematicLimitVertices.length)
               captureRegionKinematicLimitVertices[i].set(kinematicExtremeInWorld.getX(), kinematicExtremeInWorld.getY(), 0.0);
         }
      }

      // we want to add a certain number of points to form the kinematic perimeter. We will compute these by approximating a sector centered
      // at the foot center with a given radius.
      for (int i = 0; i < NUMBER_OF_POINTS_TO_APPROXIMATE_KINEMATIC_LIMITS; i++)
      {
         double alphaFromAToB = ((double) (i + 1)) / ((double) (NUMBER_OF_POINTS_TO_APPROXIMATE_KINEMATIC_LIMITS + 1));
         FramePoint2d additionalKinematicPoint = getPointBetweenVectorsAtDistanceFromOriginCircular(directionLimits[0], directionLimits[1], alphaFromAToB,
                                                    KINEMATIC_RANGE_FROM_COP, footCentroid);
         captureRegionVertices.add(additionalKinematicPoint);
         additionalKinematicPoint = additionalKinematicPoint.changeFrameCopy(worldFrame);
         additionalKinematicLimitPoints[i].set(additionalKinematicPoint.getX(), additionalKinematicPoint.getY(), 0.0);
      }

      // connect the 4 dots (2 extreme predicted proximal capture points, 2 distal kinematic limit points) + the extra kinematic dots
      FrameConvexPolygon2d captureRegion = new FrameConvexPolygon2d(captureRegionVertices);

      // Intersect this with the reachableRegion:
      FrameConvexPolygon2d reachableRegion = reachableRegions.get(supportLeg);

      captureRegion = captureRegion.intersectionWith(reachableRegion);

//      if (DRAW_SCORE_ON_GROUND && (captureRegion != null))
//      {
//         if (currentSide != supportLeg)
//         {
//            // clear the old
//            for (double i = 0; i < index; i++)
//            {
//               YoboticsBipedPlotter.deregisterArtifactNoRepaint("Point" + index);
//            }
//
//            YoboticsBipedPlotter.repaint();
//            index = 0;
//
//            currentSide = supportLeg;
//            Point2d minPoint = new Point2d();
//            Point2d maxPoint = new Point2d();
//            BoundingBox2d boundingBox = captureRegion.getBoundingBox();
//
//            boundingBox.getMinPoint(minPoint);
//            boundingBox.getMaxPoint(maxPoint);
//
//            // add colored points for score
//            for (double i = minPoint.getX(); i <= maxPoint.getX(); i = i + 0.01)
//            {
//               for (double j = minPoint.getY(); j <= maxPoint.getY(); j = j + 0.01)
//               {
//                  Point2d desiredPoint = new Point2d(i, j);
//                  FramePoint desiredFootLocation = new FramePoint(ankleZUpFrames.get(supportLeg), desiredPoint.getX(),
//                                                      desiredPoint.getY(), 0.0);
//                  Footstep desiredFootstep = new Footstep(supportLeg, desiredFootLocation, 0.0);
//                  double stepLocationScore = weightedDistanceScorer.getStepLocationScore(supportLeg, desiredFootstep);
//
//                  if (stepLocationScore != 0.0)
//                  {
//                     FramePoint desiredFootLocationInWorld = desiredFootLocation.changeFrameCopy(ReferenceFrame.getWorldFrame());
//                     us.ihmc.plotting.shapes.PointArtifact point1 = new us.ihmc.plotting.shapes.PointArtifact("Point" + index,
//                                                                       new Point2d(desiredFootLocationInWorld.getX(), desiredFootLocationInWorld.getY()));
//                     double colorIndex = (1.0 - (stepLocationScore * 0.6));    // 0.6 to lighten the color a bit
//                     point1.setColor(new Color((float) 0.0, (float) colorIndex, (float) 0.0, (float) 0.4));
//                     point1.setLevel(86);
//                     YoboticsBipedPlotter.registerArtifactNoRepaint(point1);
//                     index++;
//                  }
//               }
//            }
//
//            YoboticsBipedPlotter.repaint();
//         }
//      }

//    // now shrink that polygon
//    ArrayList<FramePoint2d> shrunkPoints = new ArrayList<FramePoint2d>();
//    for (int i = 0; i < captureRegionVertices.size(); i++)
//    {
//       FramePoint2d shrunkPoint = new FramePoint2d(captureRegionVertices.get(i));
//       captureRegionFromPoints.pullPointTowardsCentroid(shrunkPoint,
//                                                        captureRegionVertices.get(i).distance(captureRegionFromPoints.getCentroidCopy()) *
//                                                        FINAL_CAPTURE_REGION_SAFETY_MARGIN);
//       shrunkPoints.add(shrunkPoint);
//    }
//    captureRegionFromPoints = new FrameConvexPolygon2d(shrunkPoints);

      if (DRAW_CAPTURE_REGION)
      {
         if (captureRegion == null)
            captureRegionGraphic.setFrameConvexPolygon2d(null);
         else
            captureRegionGraphic.setFrameConvexPolygon2d(captureRegion.changeFrameCopy(worldFrame));
      }

      globalTimer.stopTimer();

      return captureRegion;
   }


   public FrameConvexPolygon2d getReachableRegion(RobotSide supportSide)
   {
      return reachableRegions.get(supportSide);
   }



   private static FramePoint2d solveIntersectionOfRayAndCircle(FramePoint2d pointA, FramePoint2d pointB, FrameVector2d vector, double R)
   {
      // Look at JPratt Notes February 18, 2009 for details on the following:

      pointA.checkReferenceFrameMatch(pointB);
      pointA.checkReferenceFrameMatch(vector);

      double Ax = pointA.getX();
      double Ay = pointA.getY();

      double Bx = pointB.getX();
      double By = pointB.getY();

      double vx = vector.getX();
      double vy = vector.getY();

      double A = (vx * vx + vy * vy);
      double B = (2.0 * vx * (Bx - Ax) + 2.0 * vy * (By - Ay));
      double C = (Bx - Ax) * (Bx - Ax) + (By - Ay) * (By - Ay) - R * R;

      double insideSqrt = B * B - 4 * A * C;

      if (insideSqrt < 0.0)
         return null;

      double l2 = (-B + Math.sqrt(insideSqrt)) / (2.0 * A);


      FramePoint2d ret = new FramePoint2d(pointA.getReferenceFrame(), pointB.getX() + l2 * vector.getX(), pointB.getY() + l2 * vector.getY());

      return ret;
   }

   /**
    * Finds a point that lies at a given distance along a direction vector from a given origin. The direction vector is specified
    * by two extreme direction vectors, a and b, along with an alpha. If the alpha is zero, direction a is used, if alpha is 1,
    * direction b is used. An intermediate alpha will yield a direction vector morphed from a to b by the direction value.
    * @param directionA FrameVector2d
    * @param directionB FrameVector2d
    * @param alphaFromAToB double
    * @param distance double
    * @param origin FramePoint2d
    * @return FramePoint2d
    */
   public static FramePoint2d getPointBetweenVectorsAtDistanceFromOrigin(FrameVector2d directionA, FrameVector2d directionB, double alphaFromAToB,
           double distance, FramePoint2d origin)
   {
      directionA.checkReferenceFrameMatch(directionB.getReferenceFrame());
      directionA.checkReferenceFrameMatch(origin.getReferenceFrame());
      alphaFromAToB = MathTools.clipToMinMax(alphaFromAToB, 0.0, 1.0);

      FrameVector2d aToB = new FrameVector2d(directionB);
      aToB.sub(directionA);

//    aToB.normalize();
      aToB.scale(alphaFromAToB);

      FrameVector2d directionVector = new FrameVector2d(directionA);
      directionVector.add(aToB);
      directionVector.normalize();

      directionVector.scale(distance);

      FramePoint2d ret = new FramePoint2d(directionVector);
      ret.add(origin);

      return ret;
   }


   public static FramePoint2d getPointBetweenVectorsAtDistanceFromOriginCircular(FrameVector2d directionA, FrameVector2d directionB, double alphaFromAToB,
           double distance, FramePoint2d origin)
   {
      directionA.checkReferenceFrameMatch(directionB.getReferenceFrame());
      directionA.checkReferenceFrameMatch(origin.getReferenceFrame());
      alphaFromAToB = MathTools.clipToMinMax(alphaFromAToB, 0.0, 1.0);

      double angleBetweenDirections = directionA.angle(directionB);
      double angleBetweenDirectionsToSetLine = angleBetweenDirections * alphaFromAToB;

      FrameVector rotatedFromA = new FrameVector(directionA.getReferenceFrame(), directionA.getX(), directionA.getY(), 0.0);
      Transform3D rotation = new Transform3D();
      rotation.setRotation(new AxisAngle4d(new Vector3d(0.0, 0.0, -1.0), angleBetweenDirectionsToSetLine));
      rotatedFromA.applyTransform(rotation);

      rotatedFromA.normalize();
      rotatedFromA.scale(distance);

      FramePoint2d ret = new FramePoint2d(rotatedFromA.getReferenceFrame(), rotatedFromA.getX(), rotatedFromA.getY());
      ret.add(origin);

      return ret;
   }

   /**
    * hideCaptureRegion
    */
   public void hideCaptureRegion()
   {
      capturePointCalculator.hidePredictedCapturePoint();

      if (captureRegionGraphic != null)
      {
         this.captureRegionGraphic.setFrameConvexPolygon2d(null);

         for (YoFramePoint yoFramePoint : captureRegionBestCaseVertices)
         {
            yoFramePoint.set(Double.NaN, Double.NaN, Double.NaN);
         }

         for (YoFramePoint yoFramePoint : captureRegionKinematicLimitVertices)
         {
            yoFramePoint.set(Double.NaN, Double.NaN, Double.NaN);
         }

         for (YoFramePoint yoFramePoint : estimatedCOPExtremes)
         {
            yoFramePoint.set(Double.NaN, Double.NaN, Double.NaN);
         }

         for (YoFramePoint yoFramePoint : additionalKinematicLimitPoints)
         {
            yoFramePoint.set(Double.NaN, Double.NaN, Double.NaN);
         }
      }
   }

}
