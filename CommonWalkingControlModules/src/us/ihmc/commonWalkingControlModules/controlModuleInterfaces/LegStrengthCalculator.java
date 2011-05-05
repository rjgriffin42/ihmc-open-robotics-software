package us.ihmc.commonWalkingControlModules.controlModuleInterfaces;

import us.ihmc.commonWalkingControlModules.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint2d;

public interface LegStrengthCalculator
{
   public abstract void packLegStrengths(SideDependentList<Double> legStrengths, SideDependentList<FramePoint2d> virtualToePoints, FramePoint2d coPDesired);
}
