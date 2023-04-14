package us.ihmc.perception.arUco;

import us.ihmc.tools.property.*;

/**
 * The JSON file for this property set is located here:
 * ihmc-perception/src/main/resources/us/ihmc/perception/arUco/ArUcoMarkerInfo.json
 *
 * This class was auto generated. Property attributes must be edited in the JSON file,
 * after which this class should be regenerated by running the main. This class uses
 * the generator to assist in the addition, removal, and modification of property keys.
 * It is permissible to forgo these benefits and abandon the generator, in which case
 * you should also move it from the generated-java folder to the java folder.
 *
 * If the constant paths have changed, change them in this file and run the main to regenerate.
 */
public class ArUcoMarkerInfo extends StoredPropertySet implements ArUcoMarkerInfoBasics
{
   public static final StoredPropertyKeyList keys = new StoredPropertyKeyList();

   /**
    * ArUco marker ID in the ArUco dictionary
    */
   public static final IntegerStoredPropertyKey markerID = keys.addIntegerKey("Marker ID");
   /**
    * ArUco marker side length size of the black outer part
    */
   public static final DoubleStoredPropertyKey markerSize = keys.addDoubleKey("Marker size");
   /**
    * ArUco marker origin X translation to parent
    */
   public static final DoubleStoredPropertyKey markerXTranslationToParent = keys.addDoubleKey("Marker X translation to parent");
   /**
    * ArUco marker origin Y translation to parent
    */
   public static final DoubleStoredPropertyKey markerYTranslationToParent = keys.addDoubleKey("Marker Y translation to parent");
   /**
    * ArUco marker origin Z translation to parent
    */
   public static final DoubleStoredPropertyKey markerZTranslationToParent = keys.addDoubleKey("Marker Z translation to parent");
   /**
    * ArUco marker origin yaw rotation to parent (in degrees)
    */
   public static final DoubleStoredPropertyKey markerYawRotationToParentDegrees = keys.addDoubleKey("Marker yaw rotation to parent degrees");
   /**
    * ArUco marker origin pitch rotation to parent (in degrees)
    */
   public static final DoubleStoredPropertyKey markerPitchRotationToParentDegrees = keys.addDoubleKey("Marker pitch rotation to parent degrees");
   /**
    * ArUco marker origin roll rotation to parent (in degrees)
    */
   public static final DoubleStoredPropertyKey markerRollRotationToParentDegrees = keys.addDoubleKey("Marker roll rotation to parent degrees");

   /**
    * Loads this property set.
    */
   public ArUcoMarkerInfo()
   {
      this("");
   }

   /**
    * Loads an alternate version of this property set in the same folder.
    */
   public ArUcoMarkerInfo(String versionSuffix)
   {
      this(ArUcoMarkerInfo.class, versionSuffix);
   }

   /**
    * Loads an alternate version of this property set in other folders.
    */
   public ArUcoMarkerInfo(Class<?> classForLoading, String versionSuffix)
   {
      super(keys, classForLoading, ArUcoMarkerInfo.class, versionSuffix);
      load();
   }

   public ArUcoMarkerInfo(StoredPropertySetReadOnly other)
   {
      super(keys, ArUcoMarkerInfo.class, other.getCurrentVersionSuffix());
      set(other);
   }

   public static void main(String[] args)
   {
      StoredPropertySet parameters = new StoredPropertySet(keys, ArUcoMarkerInfo.class);
      parameters.generateJavaFiles();
   }
}
