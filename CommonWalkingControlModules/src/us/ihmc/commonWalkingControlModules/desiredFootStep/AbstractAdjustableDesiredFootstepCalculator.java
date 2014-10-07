package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.util.List;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.humanoidRobot.footstep.Footstep;
import us.ihmc.yoUtilities.math.frames.YoFrameOrientation;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;


public abstract class AbstractAdjustableDesiredFootstepCalculator implements DesiredFootstepCalculator
{
   protected final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   protected final SideDependentList<YoFramePoint> footstepPositions = new SideDependentList<YoFramePoint>();
   protected final SideDependentList<YoFrameOrientation> footstepOrientations = new SideDependentList<YoFrameOrientation>();
   protected final SideDependentList<? extends ContactablePlaneBody> contactableBodies;

   private DesiredFootstepAdjustor desiredFootstepAdjustor;

   public AbstractAdjustableDesiredFootstepCalculator(SideDependentList<? extends ContactablePlaneBody> contactableBodies,
         SideDependentList<ReferenceFrame> framesToSaveFootstepIn, YoVariableRegistry parentRegistry)
   {
      this.contactableBodies = contactableBodies;

      for (RobotSide robotSide : RobotSide.values)
      {
         String namePrefix = robotSide.getCamelCaseNameForMiddleOfExpression() + "Footstep";

         ReferenceFrame frameToSaveFootstepIn = framesToSaveFootstepIn.get(robotSide);
         YoFramePoint footstepPosition = new YoFramePoint(namePrefix + "Position", frameToSaveFootstepIn, registry);
         footstepPositions.put(robotSide, footstepPosition);

         YoFrameOrientation footstepOrientation = new YoFrameOrientation(namePrefix + "Orientation", "", frameToSaveFootstepIn, registry);
         footstepOrientations.put(robotSide, footstepOrientation);
      }

      parentRegistry.addChild(registry);
   }

   public Footstep updateAndGetDesiredFootstep(RobotSide supportLegSide)
   {
      RobotSide swingLegSide = supportLegSide.getOppositeSide();

      FramePose footstepPose = new FramePose(footstepPositions.get(swingLegSide).getFramePointCopy(),
            footstepOrientations.get(swingLegSide).getFrameOrientationCopy());
      footstepPose.changeFrame(ReferenceFrame.getWorldFrame());

      PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("poseReferenceFrame", footstepPose);

      ContactablePlaneBody foot = contactableBodies.get(swingLegSide);

      boolean trustHeight = true;
      Footstep desiredFootstep = new Footstep(foot, poseReferenceFrame, trustHeight);

      if (desiredFootstepAdjustor != null)
      {
         ContactablePlaneBody stanceFoot = contactableBodies.get(supportLegSide);
         RigidBody stanceFootBody = stanceFoot.getRigidBody();
         FramePose stanceFootPose = new FramePose(stanceFootBody.getBodyFixedFrame());
         stanceFootPose.changeFrame(ReferenceFrame.getWorldFrame());

         PoseReferenceFrame stanceFootPoseFrame = new PoseReferenceFrame("desiredFootstep", stanceFootPose);

         Footstep stanceFootstep = new Footstep(stanceFoot, stanceFootPoseFrame, trustHeight);
         desiredFootstep = desiredFootstepAdjustor.adjustDesiredFootstep(stanceFootstep, desiredFootstep);

         desiredFootstep.getPose(footstepPose);
         YoFramePoint yoFramePoint = footstepPositions.get(swingLegSide);
         FramePoint footstepPosition = new FramePoint();
         footstepPose.getPositionIncludingFrame(footstepPosition);
         footstepPosition.changeFrame(yoFramePoint.getReferenceFrame());
         yoFramePoint.set(footstepPosition);
         YoFrameOrientation yoFrameOrientation = footstepOrientations.get(swingLegSide);
         FrameOrientation footstepOrientation = new FrameOrientation();
         footstepPose.getOrientationIncludingFrame(footstepOrientation);
         footstepOrientation.changeFrame(yoFrameOrientation.getReferenceFrame());
         yoFrameOrientation.set(footstepOrientation);
      }

      return desiredFootstep;
   }

   public Footstep predictFootstepAfterDesiredFootstep(RobotSide supportLegSide, Footstep desiredFootstep)
   {
      return null;
   }

   public void setDesiredFootstepAdjustor(DesiredFootstepAdjustor desiredFootstepAdjustor)
   {
      this.desiredFootstepAdjustor = desiredFootstepAdjustor;
   }

   protected abstract List<FramePoint> getContactPoints(RobotSide swingSide);

}
