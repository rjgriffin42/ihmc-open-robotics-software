package us.ihmc.commonWalkingControlModules.configurations;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import us.ihmc.euclid.geometry.BoundingBox2D;
import us.ihmc.euclid.tuple2D.Vector2D;

public class SmoothCMPPlannerParameters extends ICPWithTimeFreezingPlannerParameters
{
   private final double modelScale;
   /**
    * Vector offsets relative to centroid of support polygon defined copOffsetFrames
    */
   protected final EnumMap<CoPPointName, Vector2D> copOffsetsInFootFrame = new EnumMap<>(CoPPointName.class);
   /**
    * Bounds of the CoP offsets in the foot frame
    */
   protected final EnumMap<CoPPointName, Vector2D> copOffsetBoundsInFootFrame = new EnumMap<>(CoPPointName.class);
   protected final EnumMap<CoPPointName, CoPSupportPolygonNames> copOffsetFrameNames = new EnumMap<>(CoPPointName.class);

   /**
    * Order list of flags indicating whether specified bounding boxes should be used to constrain the CoP point
    */
   protected final EnumMap<CoPPointName, Boolean> constrainToMinMax = new EnumMap<>(CoPPointName.class);
   /**
    * Order list of flags indicating whether CoP should reside within the support polygon specified in copOffsetFrames
    */
   protected final EnumMap<CoPPointName, Boolean> constrainToSupportPolygon = new EnumMap<>(CoPPointName.class);
   protected final EnumMap<CoPPointName, CoPSupportPolygonNames> stepLengthOffsetPolygon = new EnumMap<>(CoPPointName.class);
   /**
    * Ordered list of fractions indicating whether CoP offset changes with step length
    */
   protected final EnumMap<CoPPointName, Double> stepLengthToCoPOffsetFactor = new EnumMap<>(CoPPointName.class);

   /**
    * Last transfer is planned till the endCoP in the transferCoPPointsToPlan
    */
   protected CoPPointName[] swingCopPointsToPlan;
   protected CoPPointName[] transferCoPPointsToPlan;

   /** Final CoP name (chicken support will be used only for this point). In case */
   protected CoPPointName endCoPName;
   /** Indicate the first CoP for the swing phase */
   protected CoPPointName entryCoPName;
   /** Indicate the last CoP for the swing phase. Typically everything for this point should be determined from the final values otherwise computation is not possible */
   protected CoPPointName exitCoPName;

   public SmoothCMPPlannerParameters()
   {
      this(1.0);
   }

   public SmoothCMPPlannerParameters(double modelScale)
   {
      this.modelScale = modelScale;

      copOffsetFrameNames.put(CoPPointName.HEEL_COP, CoPSupportPolygonNames.SUPPORT_FOOT_POLYGON);
      copOffsetFrameNames.put(CoPPointName.BALL_COP, CoPSupportPolygonNames.SUPPORT_FOOT_POLYGON);
      copOffsetFrameNames.put(CoPPointName.TOE_COP, CoPSupportPolygonNames.SUPPORT_FOOT_POLYGON);
      copOffsetFrameNames.put(CoPPointName.MIDFEET_COP, CoPSupportPolygonNames.INITIAL_DOUBLE_SUPPORT_POLYGON);

      stepLengthOffsetPolygon.put(CoPPointName.MIDFEET_COP, CoPSupportPolygonNames.NULL);
      stepLengthOffsetPolygon.put(CoPPointName.HEEL_COP, CoPSupportPolygonNames.INITIAL_SWING_POLYGON);
      stepLengthOffsetPolygon.put(CoPPointName.BALL_COP, CoPSupportPolygonNames.FINAL_SWING_POLYGON);
      stepLengthOffsetPolygon.put(CoPPointName.TOE_COP, CoPSupportPolygonNames.FINAL_SWING_POLYGON);

      constrainToMinMax.put(CoPPointName.MIDFEET_COP, false);
      constrainToMinMax.put(CoPPointName.HEEL_COP, true);
      constrainToMinMax.put(CoPPointName.BALL_COP, true);
      constrainToMinMax.put(CoPPointName.TOE_COP, true);

      constrainToSupportPolygon.put(CoPPointName.MIDFEET_COP, false);
      constrainToSupportPolygon.put(CoPPointName.HEEL_COP, true);
      constrainToSupportPolygon.put(CoPPointName.BALL_COP, true);
      constrainToSupportPolygon.put(CoPPointName.TOE_COP, true);

      stepLengthToCoPOffsetFactor.put(CoPPointName.MIDFEET_COP, 0.0);
      stepLengthToCoPOffsetFactor.put(CoPPointName.HEEL_COP, 1.0 / 3.0);
      stepLengthToCoPOffsetFactor.put(CoPPointName.BALL_COP, 1.0 / 8.0);
      stepLengthToCoPOffsetFactor.put(CoPPointName.TOE_COP, 1.0 / 3.0);

      copOffsetsInFootFrame.put(CoPPointName.MIDFEET_COP, new Vector2D(0.0, 0.0));
      copOffsetsInFootFrame.put(CoPPointName.HEEL_COP, new Vector2D(0.0, -0.005));
      copOffsetsInFootFrame.put(CoPPointName.BALL_COP, new Vector2D(0.0, 0.01));
      copOffsetsInFootFrame.put(CoPPointName.TOE_COP, new Vector2D(0.0, 0.025));

      copOffsetBoundsInFootFrame.put(CoPPointName.MIDFEET_COP, new Vector2D(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
      copOffsetBoundsInFootFrame.put(CoPPointName.HEEL_COP, new Vector2D(-0.04, 0.03));
      copOffsetBoundsInFootFrame.put(CoPPointName.BALL_COP, new Vector2D(0.0, 0.055));
      copOffsetBoundsInFootFrame.put(CoPPointName.TOE_COP, new Vector2D(0.0, 0.08));
   }

   @Override
   /** {@inheritDoc} */
   public boolean useSmoothCMPPlanner()
   {
      return true;
   }
   @Override
   /** {@inheritDoc} */
   public int getNumberOfFootstepsToConsider()
   {
      return 3;
   }

   @Override
   /** {@inheritDoc} */
   public int getNumberOfCoPWayPointsPerFoot()
   {
      return swingCopPointsToPlan.length + 1;
   }

   /** {@inheritDoc} */
   @Override
   public double getFreezeTimeFactor()
   {
      return 0.9;
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxInstantaneousCapturePointErrorForStartingSwing()
   {
      return modelScale * 0.25;
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxAllowedErrorWithoutPartialTimeFreeze()
   {
      return modelScale * 0.03;
   }

   /** {@inheritDoc} */
   @Override
   public boolean getDoTimeFreezing()
   {
      return false;
   }

   @Override
   /**
    * The transfer phase is split into two halves. This introduces a midpoint between
    * the initial and final CoP, which represents how far to shift the CoP during the first phase.
    */
   public double getTransferSplitFraction()
   {
      return 0.5;
   }

   @Override
   /**
    * <p>
    * Only used when using an ICP planner with two or more CoP waypoints.
    * </p>
    * <p>
    * If there are two CoP waypoints, introduces a midpoint between the initial and final CoP,
    * which represents how far to shift the CoP during the first phase.
    * </p>
    * <p>
    * If there are three CoP waypoints, represents the fraction of the swing duration spent
    * shifting to the first waypoint.
    * </p>
    */
   public double getSwingSplitFraction()
   {
      return 0.5;
   }

   /**
    * Fraction of the swing duration which the CoP is spent shifting the CoP from the heel CoP to the exit CoP.
    * The remaining fraction of swing is spent on the exit CoP.
    * The value calculated using this must be greater than that returned by {@link #getMinTimeToSpendOnExitCoPInSingleSupport()},
    * or it is overwritten.
    */
   public double getSwingDurationShiftFraction()
   {
      return 0.8;
   }

   @Override
   /** {@inheritDoc} */
   public double getMinTimeToSpendOnExitCoPInSingleSupport()
   {
      return 0.0;
   }

   @Override
   /** {@inheritDoc} */
   public double getVelocityDecayDurationWhenDone()
   {
      return Double.NaN;
   }

   @Override
   /**
    * This is not used in this planner.
    */
   public double getMaxDurationForSmoothingEntryToExitCoPSwitch()
   {
      return Double.POSITIVE_INFINITY;
   }

   @Override
   /**
    * <p>
    *  {@inheritDoc}
    * </p>
    * <p>
    * The data is organized such that the heel CoP bounds are the first entry in the list,
    * the ball CoP bounds are the second entry in the list, and the toe CoP bounds are the
    * third entry in the list. The minimum value is the X field in the vector, while the
    * maximum value is the Y field in the vector.
    * </p>
    */
   public List<Vector2D> getCoPForwardOffsetBounds()
   {
      Vector2D heelBounds = new Vector2D(-0.04, 0.03);
      Vector2D ballBounds = new Vector2D(0.0, 0.08);

      heelBounds.scale(modelScale);
      ballBounds.scale(modelScale);

      List<Vector2D> copBounds = new ArrayList<>();
      copBounds.add(heelBounds);
      copBounds.add(ballBounds);
      copBounds.add(ballBounds);

      return copBounds;
   }

   public EnumMap<CoPPointName, Vector2D> getCoPForwardOffsetBoundsInFoot()
   {
      return copOffsetBoundsInFootFrame;
   }

   @Override @Deprecated
   /**
    * <p>
    * {@inheritDoc}
    * </p>
    * <p>
    * The first entry contains the offsets for the heel CoP, the second entry contains the offsets
    * for the ball CoP, and the third entry contains the offsets for the toe second.
    * </p>
    */
   public List<Vector2D> getCoPOffsets()
   {
      throw new RuntimeException("Usage of deprecated function");
   }

   public EnumMap<CoPPointName, Vector2D> getCopOffsetsInFootFrame()
   {
      return copOffsetsInFootFrame;
   }

   public EnumMap<CoPPointName, CoPSupportPolygonNames> getSupportPolygonNames()
   {
      return copOffsetFrameNames;
   }
   
   public CoPPointName getEntryCoPName()
   {
      return entryCoPName;
   }

   public CoPPointName getExitCoPName()
   {
      return exitCoPName;
   }

   public CoPPointName getEndCoPName()
   {
      return endCoPName;
   }

   @Override
   /** {@inheritDoc} */
   public double getCoPSafeDistanceAwayFromSupportEdges()
   {
      return modelScale * 0.01;
   }

   public CoPPointName[] getSwingCoPPointsToPlan()
   {
      return swingCopPointsToPlan;
   }

   public CoPPointName[] getTransferCoPPointsToPlan()
   {
      return transferCoPPointsToPlan;
   }

   @Override
   /** {@inheritDoc} */
   public double getStepLengthToBallCoPOffsetFactor()
   {
      return 1.0 / 3.0;
   }

   @Override
   /** {@inheritDoc} */
   public boolean putExitCoPOnToes()
   {
      return false;
   }

   @Override
   /** {@inheritDoc} */
   public boolean useExitCoPOnToesForSteppingDown()
   {
      return false;
   }

   @Override
   /** {@inheritDoc} */
   public double getStepLengthThresholdForExitCoPOnToesWhenSteppingDown()
   {
      return modelScale * 0.15;
   }

   @Override
   public double getStepHeightThresholdForExitCoPOnToesWhenSteppingDown()
   {
      return modelScale * 0.05;
   }

   @Override
   /** {@inheritDoc} */
   public double getCoPSafeDistanceAwayFromToesWhenSteppingDown()
   {
      return modelScale * 0.0;
   }

   @Override
   /** {@inheritDoc} */
   public double getExitCoPForwardSafetyMarginOnToes()
   {
      return modelScale * 1.6e-2;
   }

   @Override
   /** {@inheritDoc} */
   public double getStepLengthThresholdForExitCoPOnToes()
   {
      return modelScale * 0.15;
   }

   public EnumMap<CoPPointName, Boolean> getIsConstrainedToMinMaxFlags()
   {
      return constrainToMinMax;
   }

   public EnumMap<CoPPointName, Boolean> getIsConstrainedToSupportPolygonFlags()
   {
      return constrainToSupportPolygon;
   }

   public EnumMap<CoPPointName, CoPSupportPolygonNames> getStepLengthToCoPOffsetFlags()
   {
      return stepLengthOffsetPolygon;
   }

   public EnumMap<CoPPointName, Double> getStepLengthToCoPOffsetFactors()
   {
      return stepLengthToCoPOffsetFactor;
   }

   public CoPSplineType getOrderOfCoPInterpolation()
   {
      return CoPSplineType.LINEAR;
   }
}
