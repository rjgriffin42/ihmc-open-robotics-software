package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;


import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;


public class DoNothingBipedFeetUpdater implements BipedFeetUpdater
{
   private final ReferenceFrame midFeetZUpFrame;

   private final YoVariableRegistry registry = new YoVariableRegistry("BipedFeetUpdater");

   public DoNothingBipedFeetUpdater(CommonWalkingReferenceFrames referenceFrames, double footForward, double footBack,
           YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();


      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }
   }

   public void updateBipedFeet(BipedFootInterface leftFoot, BipedFootInterface rightFoot, RobotSide supportLeg, FramePoint capturePointInMidFeetZUp,
                               boolean forceHindOnToes)
   {
      capturePointInMidFeetZUp.checkReferenceFrameMatch(midFeetZUpFrame);

      if (supportLeg == null)    // If in double support, then set both feet to supporting, and compute the toes/heels lines, and decide which polygon to use.
      {
         leftFoot.setIsSupportingFoot(true);
         rightFoot.setIsSupportingFoot(true);
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
      }
   }

   public void setResizePolygonInDoubleSupport(boolean doResize)
   {
   }
}
