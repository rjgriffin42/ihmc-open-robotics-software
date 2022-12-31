package us.ihmc.footstepPlanning;

import us.ihmc.tools.property.*;

/**
 * The JSON file for this property set is located here:
 * ihmc-footstep-planning/src/main/resources/us/ihmc/footstepPlanning/AStarBodyPathPlannerParameters.json
 *
 * This class was auto generated. Property attributes must be edited in the JSON file,
 * after which this class should be regenerated by running the main. This class uses
 * the generator to assist in the addition, removal, and modification of property keys.
 * It is permissible to forgo these benefits and abandon the generator, in which case
 * you should also move it from the generated-java folder to the java folder.
 *
 * If the constant paths have changed, change them in this file and run the main to regenerate.
 */
public class AStarBodyPathPlannerParameters extends StoredPropertySet implements AStarBodyPathPlannerParametersBasics
{
   public static final String DIRECTORY_NAME_TO_ASSUME_PRESENT = "ihmc-open-robotics-software";
   public static final String SUBSEQUENT_PATH_TO_RESOURCE_FOLDER = "ihmc-footstep-planning/src/main/resources";
   public static final String SUBSEQUENT_PATH_TO_JAVA_FOLDER = "ihmc-footstep-planning/src/main/generated-java";

   public static final StoredPropertyKeyList keys = new StoredPropertyKeyList();

   public static final DoubleStoredPropertyKey rollCostWeight = keys.addDoubleKey("Roll cost weight");
   public static final DoubleStoredPropertyKey inclineCostWeight = keys.addDoubleKey("Incline cost weight");
   public static final DoubleStoredPropertyKey inclineCostDeadband = keys.addDoubleKey("Incline cost deadband");
   public static final DoubleStoredPropertyKey maxIncline = keys.addDoubleKey("Max incline");
   public static final BooleanStoredPropertyKey checkForCollisions = keys.addBooleanKey("Check for collisions");
   public static final BooleanStoredPropertyKey computeSurfaceNormalCost = keys.addBooleanKey("Compute surface normal cost");
   public static final BooleanStoredPropertyKey performSmoothing = keys.addBooleanKey("Perform smoothing");

   /**
    * Loads this property set.
    */
   public AStarBodyPathPlannerParameters()
   {
      this("");
   }

   /**
    * Loads an alternate version of this property set in the same folder.
    */
   public AStarBodyPathPlannerParameters(String versionSpecifier)
   {
      this(AStarBodyPathPlannerParameters.class, DIRECTORY_NAME_TO_ASSUME_PRESENT, SUBSEQUENT_PATH_TO_RESOURCE_FOLDER, versionSpecifier);
   }

   /**
    * Loads an alternate version of this property set in other folders.
    */
   public AStarBodyPathPlannerParameters(Class<?> classForLoading, String directoryNameToAssumePresent, String subsequentPathToResourceFolder, String versionSuffix)
   {
      super(keys, classForLoading, AStarBodyPathPlannerParameters.class, directoryNameToAssumePresent, subsequentPathToResourceFolder, versionSuffix);
      load();
   }

   public AStarBodyPathPlannerParameters(StoredPropertySetReadOnly other)
   {
      super(keys, AStarBodyPathPlannerParameters.class, DIRECTORY_NAME_TO_ASSUME_PRESENT, SUBSEQUENT_PATH_TO_RESOURCE_FOLDER, other.getCurrentVersionSuffix());
      set(other);
   }

   public static void main(String[] args)
   {
      StoredPropertySet parameters = new StoredPropertySet(keys,
                                                           AStarBodyPathPlannerParameters.class,
                                                           DIRECTORY_NAME_TO_ASSUME_PRESENT,
                                                           SUBSEQUENT_PATH_TO_RESOURCE_FOLDER);
      parameters.generateJavaFiles(SUBSEQUENT_PATH_TO_JAVA_FOLDER);
   }
}
