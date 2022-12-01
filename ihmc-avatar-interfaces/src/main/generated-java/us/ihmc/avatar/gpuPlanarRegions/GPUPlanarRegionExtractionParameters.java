package us.ihmc.avatar.gpuPlanarRegions;

import us.ihmc.tools.property.*;

/**
 * The JSON file for this property set is located here:
 * ihmc-avatar-interfaces/src/main/resources/us/ihmc/avatar/gpuPlanarRegions/GPUPlanarRegionExtractionParameters.json
 *
 * This class was auto generated. Property attributes must be edited in the JSON file,
 * after which this class should be regenerated by running the main. This class uses
 * the generator to assist in the addition, removal, and modification of property keys.
 * It is permissible to forgo these benefits and abandon the generator, in which case
 * you should also move it from the generated-java folder to the java folder.
 *
 * If the constant paths have changed, change them in this file and run the main to regenerate.
 */
public class GPUPlanarRegionExtractionParameters extends StoredPropertySet implements GPUPlanarRegionExtractionParametersBasics
{
   public static final String DIRECTORY_NAME_TO_ASSUME_PRESENT = "ihmc-open-robotics-software";
   public static final String SUBSEQUENT_PATH_TO_RESOURCE_FOLDER = "ihmc-avatar-interfaces/src/main/resources";
   public static final String SUBSEQUENT_PATH_TO_JAVA_FOLDER = "ihmc-avatar-interfaces/src/main/generated-java";

   public static final StoredPropertyKeyList keys = new StoredPropertyKeyList();

   public static final DoubleStoredPropertyKey mergeDistanceThreshold = keys.addDoubleKey("Merge distance threshold");
   public static final DoubleStoredPropertyKey mergeAngularThreshold = keys.addDoubleKey("Merge angular threshold");
   public static final DoubleStoredPropertyKey filterDisparityThreshold = keys.addDoubleKey("Filter disparity threshold");
   public static final IntegerStoredPropertyKey patchSize = keys.addIntegerKey("Patch size");
   public static final IntegerStoredPropertyKey deadPixelFilterPatchSize = keys.addIntegerKey("Dead pixel filter patch size");
   public static final DoubleStoredPropertyKey focalLengthXPixels = keys.addDoubleKey("Focal length X pixels");
   public static final DoubleStoredPropertyKey focalLengthYPixels = keys.addDoubleKey("Focal length Y pixels");
   public static final DoubleStoredPropertyKey principalOffsetXPixels = keys.addDoubleKey("Principal offset X pixels");
   public static final DoubleStoredPropertyKey principalOffsetYPixels = keys.addDoubleKey("Principal offset Y pixels");
   public static final BooleanStoredPropertyKey earlyGaussianBlur = keys.addBooleanKey("Early gaussian blur");
   public static final BooleanStoredPropertyKey useFilteredImage = keys.addBooleanKey("Use filtered image");
   public static final BooleanStoredPropertyKey useSVDNormals = keys.addBooleanKey("Use SVD normals");
   public static final IntegerStoredPropertyKey svdReductionFactor = keys.addIntegerKey("SVD reduction factor");
   public static final IntegerStoredPropertyKey gaussianSize = keys.addIntegerKey("Gaussian size");
   public static final DoubleStoredPropertyKey gaussianSigma = keys.addDoubleKey("Gaussian sigma");
   public static final IntegerStoredPropertyKey searchDepthLimit = keys.addIntegerKey("Search depth limit");
   public static final IntegerStoredPropertyKey regionMinPatches = keys.addIntegerKey("Region min patches");
   public static final IntegerStoredPropertyKey boundaryMinPatches = keys.addIntegerKey("Boundary min patches");
   public static final DoubleStoredPropertyKey regionGrowthFactor = keys.addDoubleKey("Region growth factor");

   /**
    * Loads this property set.
    */
   public GPUPlanarRegionExtractionParameters()
   {
      this("");
   }

   /**
    * Loads an alternate version of this property set in the same folder.
    */
   public GPUPlanarRegionExtractionParameters(String versionSpecifier)
   {
      this(GPUPlanarRegionExtractionParameters.class, DIRECTORY_NAME_TO_ASSUME_PRESENT, SUBSEQUENT_PATH_TO_RESOURCE_FOLDER, versionSpecifier);
   }

   /**
    * Loads an alternate version of this property set in other folders.
    */
   public GPUPlanarRegionExtractionParameters(Class<?> classForLoading, String directoryNameToAssumePresent, String subsequentPathToResourceFolder, String versionSuffix)
   {
      super(keys, classForLoading, GPUPlanarRegionExtractionParameters.class, directoryNameToAssumePresent, subsequentPathToResourceFolder, versionSuffix);
      load();
   }

   public GPUPlanarRegionExtractionParameters(StoredPropertySetReadOnly other)
   {
      super(keys, GPUPlanarRegionExtractionParameters.class, DIRECTORY_NAME_TO_ASSUME_PRESENT, SUBSEQUENT_PATH_TO_RESOURCE_FOLDER, other.getCurrentVersionSuffix());
      set(other);
   }

   public static void main(String[] args)
   {
      StoredPropertySet parameters = new StoredPropertySet(keys,
                                                           GPUPlanarRegionExtractionParameters.class,
                                                           DIRECTORY_NAME_TO_ASSUME_PRESENT,
                                                           SUBSEQUENT_PATH_TO_RESOURCE_FOLDER);
      parameters.generateJavaFiles(SUBSEQUENT_PATH_TO_JAVA_FOLDER);
   }
}
