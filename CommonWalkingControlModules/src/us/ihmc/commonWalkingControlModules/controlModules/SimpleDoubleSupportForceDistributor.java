package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DoubleSupportForceDistributor;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class SimpleDoubleSupportForceDistributor implements DoubleSupportForceDistributor
{
   private final ReferenceFrame pelvisFrame;

   public SimpleDoubleSupportForceDistributor(CommonWalkingReferenceFrames referenceFrames)
   {
      this.pelvisFrame = referenceFrames.getPelvisFrame();
   }

   public void packForcesAndTorques(SideDependentList<Double> zForcesInPelvisFrameToPack, SideDependentList<FrameVector> torquesOnPelvis,
                                    double zForceInPelvisFrameTotal, FrameVector torqueOnPelvisTotal,
                                    SideDependentList<Double> legStrengths, SideDependentList<FramePoint2d> virtualToePoints)
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         double legStrength = legStrengths.get(robotSide);

         zForcesInPelvisFrameToPack.put(robotSide, zForceInPelvisFrameTotal * legStrength);

         FrameVector torque = torqueOnPelvisTotal.changeFrameCopy(pelvisFrame);
         torque.scale(legStrength);

         torquesOnPelvis.set(robotSide, torque);
      }
   }

}
