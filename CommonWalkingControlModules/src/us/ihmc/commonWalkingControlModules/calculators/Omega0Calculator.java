package us.ihmc.commonWalkingControlModules.calculators;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.OriginAndPointFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

public class Omega0Calculator implements Omega0CalculatorInterface
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();
   private final OriginAndPointFrame copToCoPFrame = new OriginAndPointFrame("copToCoP", worldFrame);
   private final ReferenceFrame centerOfMassFrame;
   private final double totalMass;

   public Omega0Calculator(ReferenceFrame centerOfMassFrame, double totalMass)
   {
      this.centerOfMassFrame = centerOfMassFrame;
      this.totalMass = totalMass;
   }

   public double computeOmega0(List<FramePoint2d> cop2ds, SpatialForceVector totalGroundReactionWrench)
   {
      totalGroundReactionWrench.changeFrame(centerOfMassFrame);
      double fz = totalGroundReactionWrench.getLinearPartCopy().getZ();

      double deltaZ;
      if (cop2ds.size() == 1)
      {
         FramePoint cop = cop2ds.get(0).toFramePoint();
         cop.changeFrame(centerOfMassFrame);
         deltaZ = -cop.getZ();
      }
      else    // assume 2 CoPs
      {
         List<FramePoint> cops = new ArrayList<FramePoint>(cop2ds.size());
         for (FramePoint2d cop2d : cop2ds)
         {
            FramePoint cop = cop2d.toFramePoint();
            cop.changeFrame(copToCoPFrame.getParent());
            cops.add(cop);
         }

         copToCoPFrame.setOriginAndPositionToPointAt(cops.get(0), cops.get(1));
         copToCoPFrame.update();
         FramePoint2d pseudoCoP2d = new FramePoint2d(copToCoPFrame);
         centerOfPressureResolver.resolveCenterOfPressureAndNormalTorque(pseudoCoP2d, totalGroundReactionWrench, copToCoPFrame);
         FramePoint pseudoCoP = pseudoCoP2d.toFramePoint();
         pseudoCoP.changeFrame(centerOfMassFrame);
         deltaZ = -pseudoCoP.getZ();
      }

      double omega0 = Math.sqrt(fz / (totalMass * deltaZ));
      if (Double.isNaN(omega0))
         throw new RuntimeException("omega0 is NaN");
      return omega0;
   }

}
