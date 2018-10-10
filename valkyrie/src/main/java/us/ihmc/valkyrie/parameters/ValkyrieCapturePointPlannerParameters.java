package us.ihmc.valkyrie.parameters;

import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.commonWalkingControlModules.configurations.ContinuousCMPICPPlannerParameters;
import us.ihmc.euclid.tuple2D.Vector2D;

import java.util.EnumMap;

/** {@inheritDoc} */
public class ValkyrieCapturePointPlannerParameters extends ContinuousCMPICPPlannerParameters
{
   private final boolean runningOnRealRobot;
   private final boolean useTwoCMPsPerSupport;

   private final CoPPointName exitCoPName = CoPPointName.EXIT_COP;

   private EnumMap<CoPPointName, Vector2D> copOffsets;
   private EnumMap<CoPPointName, Vector2D> copForwardOffsetBounds;

   public ValkyrieCapturePointPlannerParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;
      useTwoCMPsPerSupport = true;
   }

   /** {@inheritDoc} */
   @Override
   public int getNumberOfCoPWayPointsPerFoot()
   {
      if (useTwoCMPsPerSupport)
         return 2;
      else
         return 1;
   }

   /**{@inheritDoc} */
   @Override
   public CoPPointName getExitCoPName()
   {
      return exitCoPName;
   }


   /** {@inheritDoc} */
   @Override
   public EnumMap<CoPPointName, Vector2D> getCoPOffsetsInFootFrame()
   {
      if (copOffsets != null)
         return copOffsets;

      Vector2D entryOffset, exitOffset;
      if (runningOnRealRobot)
         entryOffset = new Vector2D(0.01, 0.01);
      else
         entryOffset = new Vector2D(0.0, 0.006);

      exitOffset = new Vector2D(0.0, 0.025);

      copOffsets = new EnumMap<>(CoPPointName.class);
      copOffsets.put(CoPPointName.ENTRY_COP, entryOffset);
      copOffsets.put(exitCoPName, exitOffset);

      return copOffsets;
   }


   /** {@inheritDoc} */
   @Override
   public EnumMap<CoPPointName, Vector2D> getCoPForwardOffsetBoundsInFoot()
   {
      if (copForwardOffsetBounds != null)
         return copForwardOffsetBounds;

      Vector2D entryBounds = new Vector2D(0.0, 0.03);
      Vector2D exitBounds = new Vector2D(-0.04, 0.06);

      copForwardOffsetBounds = new EnumMap<>(CoPPointName.class);
      copForwardOffsetBounds.put(CoPPointName.ENTRY_COP, entryBounds);
      copForwardOffsetBounds.put(exitCoPName, exitBounds);

      return copForwardOffsetBounds;
   }

   /** {@inheritDoc} */
   @Override
   public double getVelocityDecayDurationWhenDone()
   {
      return 0.5;
   }
}
