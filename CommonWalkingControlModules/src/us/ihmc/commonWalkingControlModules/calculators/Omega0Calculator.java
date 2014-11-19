package us.ihmc.commonWalkingControlModules.calculators;

import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.OriginAndPointFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

public class Omega0Calculator implements Omega0CalculatorInterface
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();
   private final OriginAndPointFrame copToCoPFrame = new OriginAndPointFrame("copToCoP", worldFrame);
   private final ReferenceFrame centerOfMassFrame;
   private final double totalMass;
   private final SideDependentList<FramePoint> cops = new SideDependentList<>(); // Max of 2 CoPs assumed here
   private final FramePoint2d pseudoCoP2d = new FramePoint2d();
   private final FramePoint pseudoCoP = new FramePoint();
   private final SpatialForceVector totalGroundReactionWrench = new SpatialForceVector();

   public Omega0Calculator(ReferenceFrame centerOfMassFrame, double totalMass)
   {
      this.centerOfMassFrame = centerOfMassFrame;
      this.totalMass = totalMass;

      for (RobotSide robotSide : RobotSide.values) // Max of 2 CoPs assumed here
         cops.put(robotSide, new FramePoint());
   }

   private final FramePoint tempCoP3d = new FramePoint();

   public double computeOmega0(SideDependentList<FramePoint2d> cop2ds, SpatialForceVector newTotalGroundReactionWrench)
   {
      totalGroundReactionWrench.set(newTotalGroundReactionWrench);
      totalGroundReactionWrench.changeFrame(centerOfMassFrame);
      double fz = totalGroundReactionWrench.getLinearPartZ();

      int numberOfValidCoPs = 0;
      for (RobotSide robotSide : RobotSide.values)
         numberOfValidCoPs += cop2ds.get(robotSide).containsNaN() ? 0 : 1;

      double deltaZ = Double.NaN;
      if (numberOfValidCoPs == 1)
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            FramePoint2d cop2d = cop2ds.get(robotSide);
            if (!cop2d.containsNaN())
            {
               tempCoP3d.setXYIncludingFrame(cop2d);
               tempCoP3d.changeFrame(centerOfMassFrame);
               deltaZ = -tempCoP3d.getZ();
               break;
            }
         }
      }
      else // assume 2 CoPs
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            FramePoint2d cop2d = cop2ds.get(robotSide);
            cops.get(robotSide).setIncludingFrame(cop2d.getReferenceFrame(), cop2d.getX(), cop2d.getY(), 0.0);
            cops.get(robotSide).changeFrame(copToCoPFrame.getParent());
         }

         copToCoPFrame.setOriginAndPositionToPointAt(cops.get(0), cops.get(1));
         copToCoPFrame.update();
         pseudoCoP2d.setToZero(copToCoPFrame);
         centerOfPressureResolver.resolveCenterOfPressureAndNormalTorque(pseudoCoP2d, totalGroundReactionWrench, copToCoPFrame);
         pseudoCoP.setXYIncludingFrame(pseudoCoP2d);
         pseudoCoP.changeFrame(centerOfMassFrame);
         deltaZ = -pseudoCoP.getZ();
      }

      double omega0 = Math.sqrt(fz / (totalMass * deltaZ));
      if (Double.isNaN(omega0))
         throw new RuntimeException("omega0 is NaN");
      return omega0;
   }

}
