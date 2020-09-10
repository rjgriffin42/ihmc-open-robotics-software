package us.ihmc.humanoidBehaviors.lookAndStep;

import us.ihmc.tools.property.*;

public class LookAndStepBehaviorParameters extends StoredPropertySet implements LookAndStepBehaviorParametersReadOnly
{
   public static final String PROJECT_NAME = "ihmc-open-robotics-software";
   public static final String TO_RESOURCE_FOLDER = "ihmc-avatar-interfaces/src/behavior-clean-room/resources";

   public static final StoredPropertyKeyList keys = new StoredPropertyKeyList();

   public static final BooleanStoredPropertyKey enableBipedalSupportRegions = keys.addBooleanKey("Enable bipedal support regions");
   public static final BooleanStoredPropertyKey flatGroundBodyPathPlan = keys.addBooleanKey("Flat ground body path plan");
   public static final IntegerStoredPropertyKey swingPlannerType = keys.addIntegerKey("Swing planner type");
   public static final DoubleStoredPropertyKey neckPitchForBodyPath = keys.addDoubleKey("Neck pitch for body path");
   public static final DoubleStoredPropertyKey neckPitchTolerance = keys.addDoubleKey("Neck pitch tolerance");
   public static final DoubleStoredPropertyKey percentSwingToWait = keys.addDoubleKey("Percent swing to wait");
   public static final DoubleStoredPropertyKey swingTime = keys.addDoubleKey("Swing time");
   public static final DoubleStoredPropertyKey transferTime = keys.addDoubleKey("Transfer time");
   public static final DoubleStoredPropertyKey resetDuration = keys.addDoubleKey("Reset duration");
   public static final DoubleStoredPropertyKey goalSatisfactionRadius = keys.addDoubleKey("Goal satisfaction radius");
   public static final DoubleStoredPropertyKey goalSatisfactionOrientationDelta = keys.addDoubleKey("Goal satisfaction orientation delta");
   public static final DoubleStoredPropertyKey planHorizon = keys.addDoubleKey("Plan horizon");
   public static final DoubleStoredPropertyKey footstepPlannerTimeout = keys.addDoubleKey("Footstep planner timeout");
   public static final DoubleStoredPropertyKey planarRegionsExpiration = keys.addDoubleKey("Planar regions expiration");
   public static final DoubleStoredPropertyKey waitTimeAfterPlanFailed = keys.addDoubleKey("Wait time after plan failed");
   public static final IntegerStoredPropertyKey minimumNumberOfPlannedSteps = keys.addIntegerKey("Minimum number of planned steps");
   public static final DoubleStoredPropertyKey robotConfigurationDataExpiration = keys.addDoubleKey("Robot configuration data expiration");
   public static final IntegerStoredPropertyKey acceptableIncompleteFootsteps = keys.addIntegerKey("Acceptable incomplete footsteps");

   public LookAndStepBehaviorParameters()
   {
      super(keys, LookAndStepBehaviorParameters.class, PROJECT_NAME, TO_RESOURCE_FOLDER);
      load();
   }

   public static void main(String[] args)
   {
      StoredPropertySet parameters = new StoredPropertySet(keys, LookAndStepBehaviorParameters.class, PROJECT_NAME, TO_RESOURCE_FOLDER);
      parameters.loadUnsafe();
      parameters.save();
   }
}
