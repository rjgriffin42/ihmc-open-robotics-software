package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.utilities.humanoidRobot.footstep.Footstep;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.plotting.ArtifactList;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.yoUtilities.math.frames.YoFrameVector2d;

public class ReferenceCentroidalMomentumPivotLocationsCalculator
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final double CMP_POINT_SIZE = 0.005;

   private YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   /**
    * <li> When using only one CMP per support:
    * Desired constant CMP locations for the entire single support phases. </li>
    * <li> When using two CMPs per support:
    * Desired CMP locations on the feet in the early single support phase. </li>
    * */
   private final ArrayList<YoFramePointInMultipleFrames> entryCMPs = new ArrayList<YoFramePointInMultipleFrames>();
   private final ArrayList<YoFramePoint> entryCMPsInWorldFrameReadOnly = new ArrayList<YoFramePoint>();

   /**
    * Only used when computing two CMPs per support.
    * Desired CMP locations on the feet in the late single support phase.
    */
   private final ArrayList<YoFramePointInMultipleFrames> exitCMPs = new ArrayList<YoFramePointInMultipleFrames>();
   private final ArrayList<YoFramePoint> exitCMPsInWorldFrameReadOnly = new ArrayList<YoFramePoint>();
   
   private final BooleanYoVariable isDoneWalking = new BooleanYoVariable("isDoneWalking", registry);
   private final DoubleYoVariable maxForwardCMPOffset = new DoubleYoVariable("maxForwardConstantCMPOffset", registry);
   private final DoubleYoVariable minForwardCMPOffset = new DoubleYoVariable("minForwardConstantCMPOffset", registry);

   private final SideDependentList<ReferenceFrame> soleFrames = new SideDependentList<>();
   private final SideDependentList<FrameConvexPolygon2d> supportFootPolygons = new SideDependentList<>();

   private final SideDependentList<YoFrameVector2d> cmpUserOffsets = new SideDependentList<>();

   private final ArrayList<Footstep> upcomingFootsteps = new ArrayList<>();

   private final FramePoint cmp = new FramePoint();
   private final FramePoint firstCMP = new FramePoint();
   private final FramePoint secondCMP = new FramePoint();

   private final FramePoint2d cmp2d = new FramePoint2d();
   private final FramePoint2d previousExitCMP2d = new FramePoint2d();
   private final FramePoint2d firstEntryCMPForSingleSupport = new FramePoint2d();
   private final SideDependentList<ConvexPolygon2d> defaultFootPolygons = new SideDependentList<>();
   private final FrameConvexPolygon2d tempSupportPolygon = new FrameConvexPolygon2d();

   private final DoubleYoVariable safeMarginDistanceForFootPolygon = new DoubleYoVariable("safeMarginDistanceForFootPolygon", registry);

   private boolean useTwoCMPsPerSupport = false;

   public ReferenceCentroidalMomentumPivotLocationsCalculator(String namePrefix, BipedSupportPolygons bipedSupportPolygons, SideDependentList<? extends ContactablePlaneBody> contactableFeet,
         int numberFootstepsToConsider, YoVariableRegistry parentRegistry)
   {
      firstEntryCMPForSingleSupport.setToNaN();

      for (RobotSide robotSide : RobotSide.values)
      {
         soleFrames.put(robotSide, contactableFeet.get(robotSide).getSoleFrame());
         FrameConvexPolygon2d defaultFootPolygon = new FrameConvexPolygon2d(contactableFeet.get(robotSide).getContactPoints2d());
         defaultFootPolygons.put(robotSide, defaultFootPolygon.getConvexPolygon2d());

         String sidePrefix = robotSide.getCamelCaseNameForMiddleOfExpression();
         YoFrameVector2d cmpUserOffset = new YoFrameVector2d(namePrefix + sidePrefix + "CMPConstantOffsets", null, registry);
         cmpUserOffsets.put(robotSide, cmpUserOffset);
         supportFootPolygons.put(robotSide, bipedSupportPolygons.getFootPolygonInSoleFrame(robotSide));
         tempSupportPolygon.setIncludingFrameAndUpdate(supportFootPolygons.get(robotSide)); // Just to allocate memory
      }

      for (int i = 0; i < numberFootstepsToConsider; i++)
      {
         YoFramePointInMultipleFrames entryConstantCMP = new YoFramePointInMultipleFrames(namePrefix + "EntryConstantCMP" + i, parentRegistry, worldFrame, soleFrames.get(RobotSide.LEFT), soleFrames.get(RobotSide.RIGHT));
         entryConstantCMP.setToNaN();
         entryCMPs.add(entryConstantCMP);
         entryCMPsInWorldFrameReadOnly.add(entryConstantCMP.buildUpdatedYoFramePointForVisualizationOnly());

         YoFramePointInMultipleFrames exitConstantCMP = new YoFramePointInMultipleFrames(namePrefix + "ExitConstantCMP" + i, parentRegistry, worldFrame, soleFrames.get(RobotSide.LEFT), soleFrames.get(RobotSide.RIGHT));
         exitConstantCMP.setToNaN();
         exitCMPs.add(exitConstantCMP);
         exitCMPsInWorldFrameReadOnly.add(exitConstantCMP.buildUpdatedYoFramePointForVisualizationOnly());
      }

      parentRegistry.addChild(registry);
   }

   public void update()
   {
      for (int i = 0; i < entryCMPs.size(); i++)
         entryCMPs.get(i).notifyVariableChangedListeners();
      for (int i = 0; i < exitCMPs.size(); i++)
         exitCMPs.get(i).notifyVariableChangedListeners();
   }

   public void setUseTwoCMPsPerSupport(boolean useTwoCMPsPerSupport)
   {
      this.useTwoCMPsPerSupport = useTwoCMPsPerSupport;
   }

   public void setSafeDistanceFromSupportEdges(double distance)
   {
      safeMarginDistanceForFootPolygon.set(distance);
   }

   public void setMinMaxForwardCMPLocationFromFootCenter(double minX, double maxX)
   {
      maxForwardCMPOffset.set(maxX);
      minForwardCMPOffset.set(minX);
   }

   public void createVisualizerForConstantCMPs(YoGraphicsList yoGraphicsList, ArtifactList artifactList)
   {
      for (int i = 0; i < entryCMPs.size(); i++)
      {
         YoGraphicPosition entryCMPViz = new YoGraphicPosition("Entry CMP" + i, entryCMPsInWorldFrameReadOnly.get(i), CMP_POINT_SIZE, YoAppearance.Green(), GraphicType.SOLID_BALL);
         yoGraphicsList.add(entryCMPViz);
         artifactList.add(entryCMPViz.createArtifact());
      }

      for (int i = 0; i < exitCMPs.size(); i++)
      {
         YoGraphicPosition exitCMPViz = new YoGraphicPosition("Exit CMP" + i, exitCMPsInWorldFrameReadOnly.get(i), CMP_POINT_SIZE, YoAppearance.Green(), GraphicType.BALL);
         yoGraphicsList.add(exitCMPViz);
         artifactList.add(exitCMPViz.createArtifact());
      }
   }

   public void setSymmetricCMPConstantOffsets(double cmpForwardOffset, double cmpInsideOffset)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         YoFrameVector2d cmpUserOffset = cmpUserOffsets.get(robotSide);
         cmpUserOffset.setX(cmpForwardOffset);
         cmpUserOffset.setY(robotSide.negateIfLeftSide(cmpInsideOffset));
      }
   }

   public void clear()
   {
      upcomingFootsteps.clear();
   }

   public void addUpcomingFootstep(Footstep footstep)
   {
      if (footstep != null)
         upcomingFootsteps.add(footstep);
   }

   public void computeReferenceCMPsStartingFromDoubleSupport(boolean atAStop, RobotSide transferToSide)
   {
      RobotSide transferFromSide = transferToSide.getOppositeSide();
      int numberOfUpcomingFootsteps = upcomingFootsteps.size();
      int cmpIndex = 0;
      boolean noUpcomingFootsteps = numberOfUpcomingFootsteps == 0;
      isDoneWalking.set(noUpcomingFootsteps);
      ReferenceFrame transferToSoleFrame = soleFrames.get(transferToSide);
      ReferenceFrame transferFromSoleFrame = soleFrames.get(transferFromSide);

      if (atAStop || noUpcomingFootsteps)
      {
         computeEntryCMPForSupportFoot(firstCMP, RobotSide.LEFT, null, null);
         computeEntryCMPForSupportFoot(secondCMP, RobotSide.RIGHT, null, null);
         firstCMP.changeFrame(transferFromSoleFrame);
         secondCMP.changeFrame(transferFromSoleFrame);
         entryCMPs.get(cmpIndex).switchCurrentReferenceFrame(transferFromSoleFrame);
         exitCMPs.get(cmpIndex).switchCurrentReferenceFrame(transferFromSoleFrame);
         entryCMPs.get(cmpIndex).interpolate(firstCMP, secondCMP, 0.5);
         exitCMPs.get(cmpIndex).interpolate(firstCMP, secondCMP, 0.5);
         cmpIndex++;

         if (noUpcomingFootsteps)
         {
            setRemainingCMPsToDuplicateLastComputedCMP(0);
            return;
         }
      }
      else
      {
         computeEntryCMPForSupportFoot(cmp, transferFromSide, null, null);
         cmp.changeFrame(transferFromSoleFrame);
         entryCMPs.get(cmpIndex).setIncludingFrame(cmp);
         computeExitCMPForSupportFoot(cmp, transferFromSide, transferToSoleFrame);
         cmp.changeFrame(transferFromSoleFrame);
         exitCMPs.get(cmpIndex).setIncludingFrame(cmp);
         cmpIndex++;
      }

      computeEntryCMPForSupportFoot(cmp, transferToSide, transferFromSoleFrame, exitCMPs.get(cmpIndex - 1));
      cmp.changeFrame(transferToSoleFrame);
      entryCMPs.get(cmpIndex).setIncludingFrame(cmp);
      firstEntryCMPForSingleSupport.setByProjectionOntoXYPlaneIncludingFrame(cmp);
      computeExitCMPForSupportFoot(cmp, transferToSide, upcomingFootsteps.get(0).getSoleReferenceFrame());
      cmp.changeFrame(transferToSoleFrame);
      exitCMPs.get(cmpIndex).setIncludingFrame(cmp);
      cmpIndex++;

      computeReferenceCMPsWithUpcomingFootsteps(transferToSide, numberOfUpcomingFootsteps, cmpIndex);
   }

   public void computeReferenceCMPsStartingFromSingleSupport(RobotSide supportSide)
   {
      int numberOfUpcomingFootsteps = upcomingFootsteps.size();
      int constantCMPIndex = 0;
      boolean onlyOneUpcomingFootstep = numberOfUpcomingFootsteps == 1;
      isDoneWalking.set(onlyOneUpcomingFootstep);

      // Can happen if this method is the first to be called, so it should pretty much never happen.
      if (firstEntryCMPForSingleSupport.containsNaN())
         computeEntryCMPForSupportFoot(cmp, supportSide, null, null);
      else
         cmp.setXYIncludingFrame(firstEntryCMPForSingleSupport);

      ReferenceFrame supportSoleFrame = soleFrames.get(supportSide);
      cmp.changeFrame(supportSoleFrame);
      entryCMPs.get(constantCMPIndex).setIncludingFrame(cmp);
      computeExitCMPForSupportFoot(cmp, supportSide, upcomingFootsteps.get(0).getSoleReferenceFrame());
      cmp.changeFrame(supportSoleFrame);
      exitCMPs.get(constantCMPIndex).setIncludingFrame(cmp);
      constantCMPIndex++;

      if (onlyOneUpcomingFootstep)
      {
         computeEntryCMPForSupportFoot(firstCMP, supportSide, null, null);
         computeEntryCMPForFootstep(secondCMP, upcomingFootsteps.get(0), null, null);
         firstCMP.changeFrame(supportSoleFrame);
         secondCMP.changeFrame(supportSoleFrame);
         entryCMPs.get(constantCMPIndex).switchCurrentReferenceFrame(supportSoleFrame);
         exitCMPs.get(constantCMPIndex).switchCurrentReferenceFrame(supportSoleFrame);
         entryCMPs.get(constantCMPIndex).interpolate(firstCMP, secondCMP, 0.5);
         exitCMPs.get(constantCMPIndex).interpolate(firstCMP, secondCMP, 0.5);
         setRemainingCMPsToDuplicateLastComputedCMP(constantCMPIndex);
         return;
      }

      computeReferenceCMPsWithUpcomingFootsteps(supportSide, numberOfUpcomingFootsteps, constantCMPIndex);
   }

   private void computeReferenceCMPsWithUpcomingFootsteps(RobotSide firstSupportSide, int numberOfUpcomingFootsteps, int cmpIndex)
   {
      ReferenceFrame soleFrameOfPreviousFootstep = soleFrames.get(firstSupportSide);

      for (int i = 0; i < numberOfUpcomingFootsteps; i++)
      {
         Footstep currentFootstep = upcomingFootsteps.get(i);
         ReferenceFrame soleOfNextFootstep = null;
         if (i < upcomingFootsteps.size() - 1) soleOfNextFootstep = upcomingFootsteps.get(i + 1).getSoleReferenceFrame();
         
         computeExitCMPForFootstep(cmp, currentFootstep, soleOfNextFootstep);
         cmp.changeFrame(soleFrames.get(firstSupportSide));
         exitCMPs.get(cmpIndex).setIncludingFrame(cmp);

         YoFramePoint previousExitCMP = exitCMPs.get(cmpIndex - 1);
         computeEntryCMPForFootstep(cmp, currentFootstep, soleFrameOfPreviousFootstep, previousExitCMP);
         cmp.changeFrame(soleFrames.get(firstSupportSide));
         entryCMPs.get(cmpIndex).setIncludingFrame(cmp);
         
         cmpIndex++;
         soleFrameOfPreviousFootstep = currentFootstep.getSoleReferenceFrame();

         if (cmpIndex >= entryCMPs.size())
            break;
      }
      setRemainingCMPsToDuplicateLastComputedCMP(cmpIndex - 1);
   }

   private void setRemainingCMPsToDuplicateLastComputedCMP(int lastComputedCMPIndex)
   {
      for (int i = lastComputedCMPIndex + 1; i < entryCMPs.size(); i++)
         entryCMPs.get(i).setIncludingFrame(entryCMPs.get(lastComputedCMPIndex));
      for (int i = lastComputedCMPIndex + 1; i < exitCMPs.size(); i++)
         exitCMPs.get(i).setIncludingFrame(exitCMPs.get(lastComputedCMPIndex));
   }

   private void computeEntryCMPForSupportFoot(FramePoint entryCMPToPack, RobotSide robotSide, ReferenceFrame soleFrameOfPreviousSupportFoot, YoFramePoint previousLateCMP)
   {
      ReferenceFrame soleFrame = soleFrames.get(robotSide);
      tempSupportPolygon.setIncludingFrameAndUpdate(supportFootPolygons.get(robotSide));
      tempSupportPolygon.changeFrame(soleFrame);

      computeEntryCMP(entryCMPToPack, robotSide, soleFrame, soleFrameOfPreviousSupportFoot, previousLateCMP);
   }

   private void computeEntryCMPForFootstep(FramePoint entryCMPToPack, Footstep footstep, ReferenceFrame soleFrameOfPreviousSupportFoot, YoFramePoint previousLateCMP)
   {
      ReferenceFrame soleFrame = footstep.getSoleReferenceFrame();
      List<Point2d> predictedContactPoints = footstep.getPredictedContactPoints();
      RobotSide robotSide = footstep.getRobotSide();

      if (predictedContactPoints != null)
         tempSupportPolygon.setIncludingFrameAndUpdate(soleFrame, predictedContactPoints);
      else
         tempSupportPolygon.setIncludingFrameAndUpdate(soleFrame, defaultFootPolygons.get(robotSide));

      computeEntryCMP(entryCMPToPack, robotSide, soleFrame, soleFrameOfPreviousSupportFoot, previousLateCMP);
   }

   private void computeEntryCMP(FramePoint entryCMPToPack, RobotSide robotSide, ReferenceFrame soleFrame, ReferenceFrame soleFrameOfPreviousSupportFoot, YoFramePoint previousLateCMP)
   {
      if (useTwoCMPsPerSupport)
      {
         if (soleFrameOfPreviousSupportFoot != null)
            cmp2d.setIncludingFrame(soleFrameOfPreviousSupportFoot, 0.0, 0.0);
         else
            cmp2d.setToZero(soleFrame);
         cmp2d.changeFrameAndProjectToXYPlane(soleFrame);

         if (previousLateCMP != null)
         {
            previousLateCMP.getFrameTuple2dIncludingFrame(previousExitCMP2d);
            previousExitCMP2d.changeFrameAndProjectToXYPlane(soleFrame);
            // Choose the laziest option
            if (Math.abs(previousExitCMP2d.getX()) < Math.abs(cmp2d.getX()))
               cmp2d.set(previousExitCMP2d);
         }

         constrainCMPAccordingToSupportPolygonAndUserOffsets(robotSide);
      }
      else
      {
         cmp2d.setIncludingFrame(tempSupportPolygon.getCentroid());
         YoFrameVector2d offset = cmpUserOffsets.get(robotSide);
         cmp2d.add(offset.getX(), offset.getY());
      }

      entryCMPToPack.setXYIncludingFrame(cmp2d);
      entryCMPToPack.changeFrame(worldFrame);
   }

   private void computeExitCMPForSupportFoot(FramePoint exitCMPToPack, RobotSide robotSide, ReferenceFrame soleFrameOfUpcomingSupportFoot)
   {
      if (useTwoCMPsPerSupport)
      {
         ReferenceFrame soleFrame = soleFrames.get(robotSide);
         tempSupportPolygon.setIncludingFrameAndUpdate(supportFootPolygons.get(robotSide));
         tempSupportPolygon.changeFrame(soleFrame);
         
         computeExitCMP(exitCMPToPack, robotSide, soleFrame, soleFrameOfUpcomingSupportFoot);
      }
      else
      {
         exitCMPToPack.setToNaN(worldFrame);
      }
   }

   private void computeExitCMPForFootstep(FramePoint exitCMPToPack, Footstep footstep, ReferenceFrame soleFrameOfUpcomingSupportFoot)
   {
      if (useTwoCMPsPerSupport)
      {
         ReferenceFrame soleFrame = footstep.getSoleReferenceFrame();
         List<Point2d> predictedContactPoints = footstep.getPredictedContactPoints();
         RobotSide robotSide = footstep.getRobotSide();

         if (predictedContactPoints != null)
            tempSupportPolygon.setIncludingFrameAndUpdate(soleFrame, predictedContactPoints);
         else
            tempSupportPolygon.setIncludingFrameAndUpdate(soleFrame, defaultFootPolygons.get(robotSide));

         computeExitCMP(exitCMPToPack, robotSide, soleFrame, soleFrameOfUpcomingSupportFoot);
      }
      else
      {
         exitCMPToPack.setToNaN(worldFrame);
      }
   }

   private void computeExitCMP(FramePoint exitCMPToPack, RobotSide robotSide, ReferenceFrame soleFrame, ReferenceFrame soleFrameOfUpcomingSupportFoot)
   {
      if (soleFrameOfUpcomingSupportFoot != null)
         cmp2d.setIncludingFrame(soleFrameOfUpcomingSupportFoot, 0.0, 0.0);
      else
         cmp2d.setToZero(soleFrame);
      cmp2d.changeFrameAndProjectToXYPlane(soleFrame);

      constrainCMPAccordingToSupportPolygonAndUserOffsets(robotSide);

      exitCMPToPack.setXYIncludingFrame(cmp2d);
      exitCMPToPack.changeFrame(worldFrame);
   }

   private void constrainCMPAccordingToSupportPolygonAndUserOffsets(RobotSide robotSide)
   {
      // First constrain the computed CMP to the given min/max along the x-axis.
      cmp2d.setX(MathTools.clipToMinMax(cmp2d.getX(), minForwardCMPOffset.getDoubleValue(), maxForwardCMPOffset.getDoubleValue()));
      cmp2d.setY(tempSupportPolygon.getCentroid().getY() + cmpUserOffsets.get(robotSide).getY());
      
      // Then constrain the computed CMP to be inside a safe support region
      tempSupportPolygon.shrink(safeMarginDistanceForFootPolygon.getDoubleValue());
      tempSupportPolygon.orthogonalProjection(cmp2d);
   }

   public ArrayList<YoFramePoint> getEntryCMPs()
   {
      return entryCMPsInWorldFrameReadOnly;
   }

   public ArrayList<YoFramePoint> getExitCMPs()
   {
      return exitCMPsInWorldFrameReadOnly;
   }

   public YoFramePoint getNextEntryCMP()
   {
      return entryCMPsInWorldFrameReadOnly.get(0);
   }

   public void getNextEntryCMP(FramePoint entryCMPToPack)
   {
      entryCMPsInWorldFrameReadOnly.get(0).getFrameTupleIncludingFrame(entryCMPToPack);
   }

   public boolean isDoneWalking()
   {
      return isDoneWalking.getBooleanValue();
   }
}
