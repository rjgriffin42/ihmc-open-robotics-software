package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;


import java.awt.Color;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.SideDependentList;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedFeetUpdater;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedFootInterface;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.FootPolygonEnum;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.timing.GlobalTimer;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameGeometry2dPlotter;
import us.ihmc.utilities.math.geometry.FrameGeometryTestFrame;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.Line2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.YoFrameLine2dArtifact;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLine2d;


public class GoOnToesDuringDoubleSupportBipedFeetUpdater implements BipedFeetUpdater
{
   private static final boolean VISUALIZE = false;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame midFeetZUpFrame, bodyZUpFrame, leftFootZUpFrame, rightFootZUpFrame;

   private final double footForward, footBack;

   private final SideDependentList<FrameLine2d> onToesLines = new SideDependentList<FrameLine2d>();
   private final SideDependentList<FrameLine2d> onHeelLines = new SideDependentList<FrameLine2d>();

   private final YoFrameLine2d leftOnToesLineViz, rightOnToesLineViz;


   private final YoVariableRegistry registry = new YoVariableRegistry("BipedFeetUpdater");

   // Foot polygon decision parameters:
   private DoubleYoVariable onToeDecisionThreshold = new DoubleYoVariable("onToeDecisionThresh", registry);
   private DoubleYoVariable onHeelDecisionThreshold = new DoubleYoVariable("onHeelDecisionThresh", registry);

   private DoubleYoVariable onToeSaturationPercent = new DoubleYoVariable("onToeSaturationPercent", registry);

   private final GlobalTimer updateBipedFeeetTimer = new GlobalTimer("updateBipedFeet", registry);

   public GoOnToesDuringDoubleSupportBipedFeetUpdater(CommonWalkingReferenceFrames referenceFrames, double footForward, double footBack,
           YoVariableRegistry yoVariableRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.footForward = footForward;
      this.footBack = footBack;

      onToeDecisionThreshold.set(0.0);
      onHeelDecisionThreshold.set(0.0);

      onToeSaturationPercent.set(0.4);    // 0.8);

      bodyZUpFrame = referenceFrames.getABodyAttachedZUpFrame();
      midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();

      leftFootZUpFrame = referenceFrames.getAnkleZUpReferenceFrames().get(RobotSide.LEFT);
      rightFootZUpFrame = referenceFrames.getAnkleZUpReferenceFrames().get(RobotSide.RIGHT);

      if (VISUALIZE)
      {
         leftOnToesLineViz = new YoFrameLine2d("leftOnToesLine", "", ReferenceFrame.getWorldFrame(), registry);
         rightOnToesLineViz = new YoFrameLine2d("rightOnToesLineViz", "", ReferenceFrame.getWorldFrame(), registry);

         YoFrameLine2dArtifact leftOnToesArtifact = new YoFrameLine2dArtifact("Left On Toes Line", leftOnToesLineViz, Color.pink);
         YoFrameLine2dArtifact rightOnToesArtifact = new YoFrameLine2dArtifact("Right On Toes Line", rightOnToesLineViz, Color.pink);

         ArtifactList artifactList = new ArtifactList("GoOnToes");
         artifactList.add(leftOnToesArtifact);
         artifactList.add(rightOnToesArtifact);

         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);

//       YoboticsBipedPlotter.registerYoFrameLine2d("Left On Toes Line", Color.pink, leftOnToesLineViz);
//       YoboticsBipedPlotter.registerYoFrameLine2d("Right On Toes Line", Color.pink, rightOnToesLineViz);

//       DynamicGraphicObjectsList dynamicGraphicObjectList = new DynamicGraphicObjectsList("BipedFeetUpdater");
//
//       DynamicGraphicLineSegment leftOnToesLine = new DynamicGraphicLineSegment()
//       leftOnToesLine = new
//       capturePointDesiredWorldGraphicPosition = new DynamicGraphicPosition(capturePointDesiredWorld, 0.01,
//                                                                          YoAppearance.Yellow(),
//                                                                          DynamicGraphicPosition.GraphicType.
//                                                                          ROTATED_CROSS);
//
//       YoboticsBipedPlotter.registerDynamicGraphicPosition("Desired Capture Point", capturePointDesiredWorldGraphicPosition);
//
//       dynamicGraphicObjectList.add(capturePointDesiredWorldGraphicPosition);
//
//       dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectList);
      }
      else
      {
         leftOnToesLineViz = null;
         rightOnToesLineViz = null;

//       leftVTPGraphic = rightVTPGraphic = null;
//       capturePointDesiredWorldGraphicPosition = null;
      }

      if (yoVariableRegistry != null)
      {
         yoVariableRegistry.addChild(registry);
      }

//    if (VarListsToRegister.REGISTER_BIPED_FEET_UPDATER)
//    {
//       yoVariableRegistry.addChild(registry);
//    }
   }

   public void updateBipedFeet(BipedFootInterface leftFoot, BipedFootInterface rightFoot, RobotSide supportLeg, FramePoint capturePointInMidFeetZUp,
                               boolean forceHindOnToes)
   {
      updateBipedFeeetTimer.startTimer();

      capturePointInMidFeetZUp.checkReferenceFrameMatch(midFeetZUpFrame);

      if (supportLeg == null)    // If in double support, then set both feet to supporting, and compute the toes/heels lines, and decide which polygon to use.
      {
         leftFoot.setIsSupportingFoot(true);
         rightFoot.setIsSupportingFoot(true);

         computeOnToesAndOnHeelsLines(leftFoot, rightFoot);

         FramePoint2d capturePointInMidFeetZUp2d = new FramePoint2d(capturePointInMidFeetZUp.getReferenceFrame(), capturePointInMidFeetZUp.getX(),
                                                      capturePointInMidFeetZUp.getY());
         decideWhichFootPolygonsToUse(leftFoot, rightFoot, capturePointInMidFeetZUp2d, forceHindOnToes);    // , supportPolygons);
      }

      else    // If in single support, then select the right supporting foot, set both Polygons to flat, and there are no heel/toe lines.
      {
         if (supportLeg == RobotSide.LEFT)
         {
            leftFoot.setIsSupportingFoot(true);
            rightFoot.setIsSupportingFoot(false);
         }

         else if (supportLeg == RobotSide.RIGHT)
         {
            leftFoot.setIsSupportingFoot(false);
            rightFoot.setIsSupportingFoot(true);
         }

         leftFoot.setFootPolygonInUse(FootPolygonEnum.FLAT);
         rightFoot.setFootPolygonInUse(FootPolygonEnum.FLAT);

         this.onToesLines.set(RobotSide.LEFT, null);
         this.onToesLines.set(RobotSide.RIGHT, null);

         this.onHeelLines.set(RobotSide.LEFT, null);
         this.onHeelLines.set(RobotSide.RIGHT, null);
      }

      updateBipedFeeetTimer.stopTimer();
   }


   protected boolean DEBUG_DECIDE = false;
   protected FrameGeometryTestFrame decisionFrame = null;
   protected FrameGeometry2dPlotter decisionPlotter = null;
   private int decisionDebugCount = 0;

   protected void decideWhichFootPolygonsToUse(BipedFootInterface leftFoot, BipedFootInterface rightFoot, FramePoint2d capturePointInMidFeetZUp,
           boolean forceHindOnToes)    // , BipedSupportPolygons supportPolygons)
   {
      SideDependentList<BipedFootInterface> feet = new SideDependentList<BipedFootInterface>(leftFoot, rightFoot);

      FramePoint2d leftToRightFoot = new FramePoint2d(rightFootZUpFrame);
      leftToRightFoot = leftToRightFoot.changeFrameCopy(bodyZUpFrame);
      leftToRightFoot = leftToRightFoot.changeFrameCopy(leftFootZUpFrame);

      RobotSide footInRear = null;    // RobotSide.LEFT;
      if (leftToRightFoot.getX() > 0.01)
         footInRear = RobotSide.LEFT;
      else if (leftToRightFoot.getX() < -0.01)
         footInRear = RobotSide.RIGHT;

      // 1. If desiredCapturePointInZUp is null, use flat feet
      if (capturePointInMidFeetZUp == null)
      {
         for (BipedFootInterface foot : feet)
         {
            foot.setFootPolygonInUse(FootPolygonEnum.FLAT);
         }

         return;
      }

      // 2. Calculate the scores for going up on toes or heel:
      SideDependentList<Double> toeScores = new SideDependentList<Double>();
      SideDependentList<Double> heelScores = new SideDependentList<Double>();

      double onToeSaturationDistance = onToeSaturationPercent.getDoubleValue() * footForward;
      double onHeelSaturationDistance = footBack;
      for (RobotSide side : RobotSide.values())
      {
//       FrameLine2d onToeLine = supportPolygons.getOnToeLineCopy(side);
         FrameLine2d onToeLine = new FrameLine2d(onToesLines.get(side));
         double toeScore;
         if ((onToeLine != null) && (onToeLine.isPointInFrontOfLine(capturePointInMidFeetZUp)))
         {
            toeScore = Math.min(1.0, onToeLine.distance(capturePointInMidFeetZUp) / onToeSaturationDistance);
         }
         else
         {
            toeScore = 0.0;
         }

//       FrameLine2d onHeelLine = supportPolygons.getOnHeelLineCopy(side);
         FrameLine2d onHeelLine = new FrameLine2d(onHeelLines.get(side));

         double heelScore;
         if ((onHeelLine != null) && (onHeelLine.isPointBehindLine(capturePointInMidFeetZUp)))
         {
            heelScore = Math.min(1.0, onHeelLine.distance(capturePointInMidFeetZUp) / onHeelSaturationDistance);
         }
         else
         {
            heelScore = 0.0;
         }

         toeScores.set(side, new Double(toeScore));
         heelScores.set(side, new Double(heelScore));
      }

      // Make a decision:
      for (RobotSide side : RobotSide.values())
      {
         if (forceHindOnToes && (footInRear == side))
         {
            feet.get(side).setFootPolygonInUse(FootPolygonEnum.ONTOES);
            feet.get(side).setShift(0.9);
         }

         else if ((toeScores.get(side) > onToeDecisionThreshold.getDoubleValue()) && (footInRear == side))    // For now only let the trailing leg go on toes...
         {
            feet.get(side).setFootPolygonInUse(FootPolygonEnum.ONTOES);
            feet.get(side).setShift(toeScores.get(side));
         }

         // Going on heels disabled due to chatter (November 10, 2008)
         // else if (heelScores.get(side) > onHeelDecisionThreshold.val)
         // {
         // feet.get(side).setFootPolygonInUse(FootPolygonEnum.ONHEEL);
         // feet.get(side).setShift(heelScores.get(side));
         // }
         else
         {
            feet.get(side).setFootPolygonInUse(FootPolygonEnum.FLAT);
         }
      }

      // System.out.println("heelScores: " + heelScores);

      // Visualizer stuff:
      if (DEBUG_DECIDE)
      {
         if (decisionDebugCount == 0)
         {
            SideDependentList<Color> colors = new SideDependentList<Color>(Color.red, Color.blue);

            for (RobotSide side : RobotSide.values())
            {
               decisionPlotter.addPolygon(feet.get(side).getFlatFootPolygon().changeFrameCopy(worldFrame), colors.get(side));
               decisionPlotter.addFrameLine2d(onToesLines.get(side).changeFrameCopy(worldFrame), colors.get(side));
               decisionPlotter.addFrameLine2d(onHeelLines.get(side).changeFrameCopy(worldFrame), colors.get(side));
            }

            // Line pointing forward:
            decisionPlotter.addFrameLine2d(new FrameLine2d(worldFrame, new Line2d(new Point2d(0.0, 0.0), new Vector2d(1.0, 0.0))), Color.orange);
         }


         // Points. Right side only:
         if ((toeScores.get(RobotSide.RIGHT) > onToeDecisionThreshold.getDoubleValue())
                 && (heelScores.get(RobotSide.RIGHT) > onHeelDecisionThreshold.getDoubleValue()))
         {
            decisionPlotter.addFramePoint2d(capturePointInMidFeetZUp.changeFrameCopy(worldFrame), Color.black);
         }
         else if (toeScores.get(RobotSide.RIGHT) > onToeDecisionThreshold.getDoubleValue())
         {
            decisionPlotter.addFramePoint2d(capturePointInMidFeetZUp.changeFrameCopy(worldFrame), Color.green);
         }
         else if (heelScores.get(RobotSide.RIGHT) > onHeelDecisionThreshold.getDoubleValue())
         {
            decisionPlotter.addFramePoint2d(capturePointInMidFeetZUp.changeFrameCopy(worldFrame), Color.red);
         }
         else
         {
            decisionPlotter.addFramePoint2d(capturePointInMidFeetZUp.changeFrameCopy(worldFrame), Color.gray);
         }

         decisionDebugCount++;
      }
   }


   private void computeOnToesAndOnHeelsLines(BipedFootInterface leftFoot, BipedFootInterface rightFoot)
   {
      // Only compute onToesLines, onHeelLines if in double support:
      SideDependentList<FrameLine2d>
         onToesLines = null, onHeelLines = null;



      // +++JEP090106: This is already done in BipedSupportPolygons!!!
      // Need to simplify this class. All we're trying to compute is the BipedFoot shift amount based on where the feet are and the Capture Point location during double support.
      // Should be much simpler than this...


      // Extract the foot polygons in use from the leftFoot and the rightFoot
//    SideDependentList<FrameConvexPolygon2d> footPolygonsInAnkleZUp = new SideDependentList<FrameConvexPolygon2d> (leftFoot.getFootPolygonInUse(), rightFoot.getFootPolygonInUse());
      SideDependentList<FrameConvexPolygon2d> footPolygonsInAnkleZUp = new SideDependentList<FrameConvexPolygon2d>(leftFoot.getFlatFootPolygon(),
                                                                          rightFoot.getFlatFootPolygon());

      // Change the foot polygons to midFeetZUp frame
      FrameConvexPolygon2d leftPolygon = footPolygonsInAnkleZUp.get(RobotSide.LEFT).changeFrameAndProjectToXYPlaneCopy(midFeetZUpFrame);
      FrameConvexPolygon2d rightPolygon = footPolygonsInAnkleZUp.get(RobotSide.RIGHT).changeFrameAndProjectToXYPlaneCopy(midFeetZUpFrame);

      SideDependentList<FrameConvexPolygon2d> footPolygonsInMidFeetZUp = new SideDependentList<FrameConvexPolygon2d>(leftPolygon, rightPolygon);

      // onToesLines:
      SideDependentList<FramePoint[]> onToesPointsLists = new SideDependentList<FramePoint[]>();
      onToesPointsLists.set(RobotSide.LEFT, FramePoint.changeFrameCopyBatch(leftFoot.getToePointsCopy(), midFeetZUpFrame));    // ugly, but for now
      onToesPointsLists.set(RobotSide.RIGHT, FramePoint.changeFrameCopyBatch(rightFoot.getToePointsCopy(), midFeetZUpFrame));    // ugly, but for now

      onToesLines = getOnToesLines(onToesPointsLists, footPolygonsInMidFeetZUp, bodyZUpFrame);
      this.onToesLines.set(onToesLines);

      // onHeelsLines:
      SideDependentList<FramePoint[]> onHeelPointsLists = new SideDependentList<FramePoint[]>();
      onHeelPointsLists.set(RobotSide.LEFT, FramePoint.changeFrameCopyBatch(leftFoot.getHeelPointsCopy(), midFeetZUpFrame));    // ugly, but for now
      onHeelPointsLists.set(RobotSide.RIGHT, FramePoint.changeFrameCopyBatch(rightFoot.getHeelPointsCopy(), midFeetZUpFrame));    // ugly, but for now

      onHeelLines = getOnHeelLines(onHeelPointsLists, footPolygonsInMidFeetZUp, bodyZUpFrame);
      this.onHeelLines.set(onHeelLines);

      if (VISUALIZE)
      {
         leftOnToesLineViz.setFrameLine2d(onToesLines.get(RobotSide.LEFT).changeFrameCopy(worldFrame));
         rightOnToesLineViz.setFrameLine2d(onToesLines.get(RobotSide.RIGHT).changeFrameCopy(worldFrame));
      }

   }



   protected static SideDependentList<FrameLine2d> getOnToesLines(SideDependentList<FramePoint[]> onToesPointsLists,
           SideDependentList<FrameConvexPolygon2d> footPolygonsInMidFeetZUp, ReferenceFrame bodyZUp)
   {
      boolean legsCrossed = footPolygonsInMidFeetZUp.get(RobotSide.RIGHT).getCentroidCopy().changeFrameCopy(bodyZUp).getY()
                            > footPolygonsInMidFeetZUp.get(RobotSide.LEFT).getCentroidCopy().changeFrameCopy(bodyZUp).getY();

      // TODO: case feet crossed
      SideDependentList<FrameLine2d> onToesLines = new SideDependentList<FrameLine2d>();
      for (RobotSide side : RobotSide.values())
      {
         // First point on onToesLine is the (oppositeSide)most line of sight vertex, seen from any toe point:
         FramePoint2d firstToePoint = onToesPointsLists.get(side)[0].toFramePoint2d();
         FramePoint2d[] lineOfSightVertices = footPolygonsInMidFeetZUp.get(side.getOppositeSide()).getLineOfSightVertices(firstToePoint);

         if ((lineOfSightVertices == null) || (lineOfSightVertices.length < 2))
            return null;    // If feet are on top of each other, just return null for now...

         FrameVector2d[] lineOfSightVectors = new FrameVector2d[] {new FrameVector2d(firstToePoint, lineOfSightVertices[0]),
                 new FrameVector2d(firstToePoint, lineOfSightVertices[1])};

         double crossProduct1 = lineOfSightVectors[0].cross(lineOfSightVectors[1]);

         FramePoint2d firstPointOnOnToesLine;
         if (!legsCrossed)
         {
            if (((side == RobotSide.LEFT) && (crossProduct1 > 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct1 < 0.0)))
            {
               firstPointOnOnToesLine = lineOfSightVertices[0];
            }
            else
            {
               firstPointOnOnToesLine = lineOfSightVertices[1];
            }
         }
         else
         {
            if (((side == RobotSide.LEFT) && (crossProduct1 < 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct1 > 0.0)))
            {
               firstPointOnOnToesLine = lineOfSightVertices[0];
            }
            else
            {
               firstPointOnOnToesLine = lineOfSightVertices[1];
            }
         }

         // Second point on onToesLine is the (side)most toePoint seen from the first point on the onToesLine
         FrameVector2d[] fromFirstPointToToePoints = new FrameVector2d[] {
                                                        new FrameVector2d(firstPointOnOnToesLine, onToesPointsLists.get(side)[0].toFramePoint2d()),
                 new FrameVector2d(firstPointOnOnToesLine, onToesPointsLists.get(side)[1].toFramePoint2d())};

         double crossProduct2 = fromFirstPointToToePoints[0].cross(fromFirstPointToToePoints[1]);

         FramePoint2d secondPointOnOnToesLine;
         if (!legsCrossed)
         {
            if (((side == RobotSide.LEFT) && (crossProduct2 < 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct2 > 0.0)))
            {
               secondPointOnOnToesLine = onToesPointsLists.get(side)[0].toFramePoint2d();
            }
            else
            {
               secondPointOnOnToesLine = onToesPointsLists.get(side)[1].toFramePoint2d();
            }
         }
         else
         {
            if (((side == RobotSide.LEFT) && (crossProduct2 > 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct2 < 0.0)))
            {
               secondPointOnOnToesLine = onToesPointsLists.get(side)[0].toFramePoint2d();
            }
            else
            {
               secondPointOnOnToesLine = onToesPointsLists.get(side)[1].toFramePoint2d();
            }
         }

         onToesLines.set(side, new FrameLine2d(firstPointOnOnToesLine, secondPointOnOnToesLine));
      }

      return onToesLines;
   }

   protected static SideDependentList<FrameLine2d> getOnHeelLines(SideDependentList<FramePoint[]> onHeelPointsLists,
           SideDependentList<FrameConvexPolygon2d> footPolygonsInMidFeetZUp, ReferenceFrame bodyZUp)
   {
      boolean legsCrossed = footPolygonsInMidFeetZUp.get(RobotSide.RIGHT).getCentroidCopy().changeFrameCopy(bodyZUp).getY()
                            > footPolygonsInMidFeetZUp.get(RobotSide.LEFT).getCentroidCopy().changeFrameCopy(bodyZUp).getY();

      // TODO: case feet crossed
      SideDependentList<FrameLine2d> onHeelLines = new SideDependentList<FrameLine2d>();
      for (RobotSide side : RobotSide.values())
      {
         // First point on onToesLine is the (oppositeSide)most line of sight vertex, seen from any toe point:
         FramePoint2d firstHeelPoint = onHeelPointsLists.get(side)[0].toFramePoint2d();
         FramePoint2d[] lineOfSightVertices = footPolygonsInMidFeetZUp.get(side.getOppositeSide()).getLineOfSightVertices(firstHeelPoint);

         if ((lineOfSightVertices == null) || (lineOfSightVertices.length < 2))
            return null;    // If feet are on top of each other, just return null for now...

         FrameVector2d[] lineOfSightVectors = new FrameVector2d[] {new FrameVector2d(firstHeelPoint, lineOfSightVertices[0]),
                 new FrameVector2d(firstHeelPoint, lineOfSightVertices[1])};

         double crossProduct1 = lineOfSightVectors[0].cross(lineOfSightVectors[1]);

         FramePoint2d firstPointOnOnHeelLine;
         if (!legsCrossed)
         {
            if (((side == RobotSide.LEFT) && (crossProduct1 < 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct1 > 0.0)))
            {
               firstPointOnOnHeelLine = lineOfSightVertices[0];
            }
            else
            {
               firstPointOnOnHeelLine = lineOfSightVertices[1];
            }
         }
         else
         {
            if (((side == RobotSide.LEFT) && (crossProduct1 > 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct1 < 0.0)))
            {
               firstPointOnOnHeelLine = lineOfSightVertices[0];
            }
            else
            {
               firstPointOnOnHeelLine = lineOfSightVertices[1];
            }

         }

         // Second point on onToesLine is the (side)most toePoint seen from the first point on the onToesLine
         FrameVector2d[] fromFirstPointToHeelPoints = new FrameVector2d[] {
                                                         new FrameVector2d(firstPointOnOnHeelLine, onHeelPointsLists.get(side)[0].toFramePoint2d()),
                 new FrameVector2d(firstPointOnOnHeelLine, onHeelPointsLists.get(side)[1].toFramePoint2d())};

         double crossProduct2 = fromFirstPointToHeelPoints[0].cross(fromFirstPointToHeelPoints[1]);

         FramePoint2d secondPointOnOnHeelLine;
         if (!legsCrossed)
         {
            if (((side == RobotSide.LEFT) && (crossProduct2 > 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct2 < 0.0)))
            {
               secondPointOnOnHeelLine = onHeelPointsLists.get(side)[0].toFramePoint2d();
            }
            else
            {
               secondPointOnOnHeelLine = onHeelPointsLists.get(side)[1].toFramePoint2d();
            }
         }
         else
         {
            if (((side == RobotSide.LEFT) && (crossProduct2 < 0.0)) || ((side == RobotSide.RIGHT) && (crossProduct2 > 0.0)))
            {
               secondPointOnOnHeelLine = onHeelPointsLists.get(side)[0].toFramePoint2d();
            }
            else
            {
               secondPointOnOnHeelLine = onHeelPointsLists.get(side)[1].toFramePoint2d();
            }
         }

         onHeelLines.set(side, new FrameLine2d(firstPointOnOnHeelLine, secondPointOnOnHeelLine));
      }

      return onHeelLines;
   }




}
