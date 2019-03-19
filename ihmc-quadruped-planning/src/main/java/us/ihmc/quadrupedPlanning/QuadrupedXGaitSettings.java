package us.ihmc.quadrupedPlanning;

public class QuadrupedXGaitSettings implements QuadrupedXGaitSettingsBasics
{
   private double stanceLength;
   private double stanceWidth;
   private double stepGroundClearance;
   private double endPhaseShift;
   private QuadrupedSpeed quadrupedSpeed;

   private final QuadrupedGaitTimingsBasics paceSlowTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics paceMediumTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics paceFastTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics ambleSlowTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics ambleMediumTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics ambleFastTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics trotSlowTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics trotMediumTimings = new QuadrupedGaitTimings();
   private final QuadrupedGaitTimingsBasics trotFastTimings = new QuadrupedGaitTimings();

   public QuadrupedXGaitSettings()
   {
   }

   public QuadrupedXGaitSettings(QuadrupedXGaitSettingsReadOnly defaultSettings)
   {
      set(defaultSettings);
   }

   @Override
   public QuadrupedSpeed getQuadrupedSpeed()
   {
      return quadrupedSpeed;
   }

   @Override
   public double getEndPhaseShift()
   {
      return endPhaseShift;
   }

   @Override
   public double getStanceLength()
   {
      return stanceLength;
   }

   @Override
   public double getStanceWidth()
   {
      return stanceWidth;
   }

   @Override
   public double getStepGroundClearance()
   {
      return stepGroundClearance;
   }

   @Override
   public void setEndPhaseShift(double endPhaseShift)
   {
      this.endPhaseShift = endPhaseShift;
   }

   @Override
   public void setQuadrupedSpeed(QuadrupedSpeed quadrupedSpeed)
   {
      this.quadrupedSpeed = quadrupedSpeed;
   }

   @Override
   public void setStanceLength(double stanceLength)
   {
      this.stanceLength = stanceLength;
   }

   @Override
   public void setStanceWidth(double stanceWidth)
   {
      this.stanceWidth = stanceWidth;
   }

   @Override
   public void setStepGroundClearance(double stepGroundClearance)
   {
      this.stepGroundClearance = stepGroundClearance;
   }

   @Override
   public QuadrupedGaitTimingsBasics getPaceSlowTimings()
   {
      return paceSlowTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getPaceMediumTimings()
   {
      return paceMediumTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getPaceFastTimings()
   {
      return paceFastTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getAmbleSlowTimings()
   {
      return ambleSlowTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getAmbleMediumTimings()
   {
      return ambleMediumTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getAmbleFastTimings()
   {
      return ambleFastTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getTrotSlowTimings()
   {
      return trotSlowTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getTrotMediumTimings()
   {
      return trotMediumTimings;
   }

   @Override
   public QuadrupedGaitTimingsBasics getTrotFastTimings()
   {
      return trotFastTimings;
   }

}
