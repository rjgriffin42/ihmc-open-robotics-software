package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;


import us.ihmc.utilities.math.geometry.Transform3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

/**
 * Very simple FinalDesiredICPCalculator that just puts the final desired ICP 4cm or so inside the transfer to foot.
 *
 */
public class ShiftInsideFinalDesiredICPCalculator implements FinalDesiredICPCalculator
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final double extraX;
   private final double extraY;

   public ShiftInsideFinalDesiredICPCalculator(YoVariableRegistry parentRegistry, double extraX, double extraY)
   {
      parentRegistry.addChild(registry);
      this.extraX = extraX;
      this.extraY = extraY;
   }

   private FramePoint2d finalDesiredICP;

   public void initialize(TransferToAndNextFootstepsData transferToAndNextFootstepsData)
   {
      finalDesiredICP = getFinalDesiredICPForWalking(transferToAndNextFootstepsData);
   }

   public FramePoint2d getFinalDesiredICP()
   {
      return finalDesiredICP;
   }

   private FramePoint2d getFinalDesiredICPForWalking(TransferToAndNextFootstepsData transferToAndNextFootstepsData)
   {
      Footstep transferToFootstep = transferToAndNextFootstepsData.getTransferToFootstep();

      FramePose transferToFootstepAnklePose = new FramePose();
      transferToFootstep.getPose(transferToFootstepAnklePose);
      ContactablePlaneBody transferToFootContactablePlaneBody = transferToAndNextFootstepsData.getTransferToFootContactablePlaneBody();
      FrameConvexPolygon2d transferToFootPolygonInSoleFrame = transferToAndNextFootstepsData.getTransferToFootPolygonInSoleFrame();
      RobotSide transferToSide = transferToAndNextFootstepsData.getTransferToSide();

      Transform3d footstepAnkleToWorldTransform = new Transform3d();
      getTransformFromPoseToWorld(footstepAnkleToWorldTransform, transferToFootstepAnklePose);

      ReferenceFrame footBodyFrame = transferToFootContactablePlaneBody.getFrameAfterParentJoint();
      ReferenceFrame footPlaneFrame = transferToFootContactablePlaneBody.getSoleFrame();
      Transform3d ankleToSoleTransform = footPlaneFrame.getTransformToDesiredFrame(footBodyFrame);

      FramePoint2d centroid2d = transferToFootPolygonInSoleFrame.getCentroidCopy();
      FramePoint centroid = centroid2d.toFramePoint();
      centroid.changeFrameUsingTransform(null, ankleToSoleTransform);
      centroid.changeFrameUsingTransform(worldFrame, footstepAnkleToWorldTransform);

      FramePoint pointOffsetFromCentroid = new FramePoint(centroid);

      double extraY = transferToSide.negateIfLeftSide(this.extraY);
      FrameVector offset = new FrameVector(null, extraX, extraY, 0.0);
      offset.changeFrameUsingTransform(worldFrame, footstepAnkleToWorldTransform);

      pointOffsetFromCentroid.changeFrame(offset.getReferenceFrame());
      pointOffsetFromCentroid.add(offset);

      FramePoint2d ret = pointOffsetFromCentroid.toFramePoint2d();

      return ret;
   }


   private static void getTransformFromPoseToWorld(Transform3d poseToWorldTransformToPack, FramePose framePose)
   {
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      framePose.changeFrame(worldFrame);
      framePose.getPose(poseToWorldTransformToPack);
   }
}
