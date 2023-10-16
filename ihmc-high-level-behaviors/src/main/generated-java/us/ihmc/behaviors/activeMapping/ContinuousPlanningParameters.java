package us.ihmc.behaviors.activeMapping;

import us.ihmc.tools.property.*;

/**
 * The JSON file for this property set is located here:
 * ihmc-high-level-behaviors/src/main/resources/us/ihmc/behaviors/activeMapping/ContinuousPlanningParameters.json
 *
 * This class was auto generated. Property attributes must be edited in the JSON file,
 * after which this class should be regenerated by running the main. This class uses
 * the generator to assist in the addition, removal, and modification of property keys.
 * It is permissible to forgo these benefits and abandon the generator, in which case
 * you should also move it from the generated-java folder to the java folder.
 *
 * If the constant paths have changed, change them in this file and run the main to regenerate.
 */
public class ContinuousPlanningParameters extends StoredPropertySet implements ContinuousPlanningParametersBasics
{
   public static final StoredPropertyKeyList keys = new StoredPropertyKeyList();

   public static final BooleanStoredPropertyKey activeMapping = keys.addBooleanKey("Active mapping");
   public static final BooleanStoredPropertyKey pauseContinuousWalking = keys.addBooleanKey("Pause Continuous Walking");

   /**
    * Loads this property set.
    */
   public ContinuousPlanningParameters()
   {
      this("");
   }

   /**
    * Loads an alternate version of this property set in the same folder.
    */
   public ContinuousPlanningParameters(String versionSuffix)
   {
      this(ContinuousPlanningParameters.class, versionSuffix);
   }

   /**
    * Loads an alternate version of this property set in other folders.
    */
   public ContinuousPlanningParameters(Class<?> classForLoading, String versionSuffix)
   {
      super(keys, classForLoading, ContinuousPlanningParameters.class, versionSuffix);
      load();
   }

   public ContinuousPlanningParameters(StoredPropertySetReadOnly other)
   {
      super(keys, ContinuousPlanningParameters.class, other.getCurrentVersionSuffix());
      set(other);
   }

   public static void main(String[] args)
   {
      StoredPropertySet parameters = new StoredPropertySet(keys, ContinuousPlanningParameters.class);
      parameters.generateJavaFiles();
   }
}
