package us.ihmc.perception.mapping;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Vector4D;
import us.ihmc.log.LogTools;
import us.ihmc.perception.BytedecoTools;
import us.ihmc.perception.slamWrapper.SlamWrapper;
import us.ihmc.perception.tools.PerceptionEuclidTools;
import us.ihmc.perception.tools.PerceptionPrintTools;
import us.ihmc.robotEnvironmentAwareness.planarRegion.slam.PlanarRegionSLAMTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.geometry.PlanarRegionsListWithPose;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PlanarRegionMap
{
   public enum MergingMode
   {
      FILTERING, SMOOTHING
   }

   public enum MatchingMode
   {
      ITERATIVE, GRAPHICAL
   }

   int sensorPoseIndex = 0;

   float planeNoise = 0.01f;
   float odomNoise = 0.00001f;

   private MergingMode merger;
   private MatchingMode matcher;

   private PlanarRegionMappingParameters parameters;
   private SlamWrapper.FactorGraphExternal factorGraph;

   private final RigidBodyTransform currentToPreviousSensorTransform = new RigidBodyTransform();

   private final RigidBodyTransform previousSensorToWorldFrameTransform = new RigidBodyTransform();
   private final RigidBodyTransform worldToPreviousSensorFrameTransform = new RigidBodyTransform();

   RigidBodyTransform sensorToWorldFrameTransformPrior = new RigidBodyTransform();
   RigidBodyTransform sensorToWorldTransformPosterior = new RigidBodyTransform();


   private boolean initialized = false;
   private boolean modified = false;
   private int uniqueRegionsFound = 0;
   private int uniqueIDtracker = 1;

   private final TIntArrayList mapIDs = new TIntArrayList();
   private final TIntArrayList incomingIDs = new TIntArrayList();

   private final HashMap<Integer, TIntArrayList> incomingToMapMatches = new HashMap<>();
   private final HashMap<Integer, TIntArrayList> mapToIncomingMatches = new HashMap<>();

   private final HashSet<Integer> mapRegionIDSet = new HashSet<>();

   private PlanarRegionsList finalMap;

   private int currentTimeIndex = 0;

   public PlanarRegionMap(boolean useSmoothingMerger)
   {
      this(useSmoothingMerger, "");
   }

   public PlanarRegionMap(boolean useSmoothingMerger, String version)
   {
      if (useSmoothingMerger)
      {
         this.merger = MergingMode.SMOOTHING;

         BytedecoTools.loadSlamWrapper();
         factorGraph = new SlamWrapper.FactorGraphExternal();
      }
      else
      {
         this.merger = MergingMode.FILTERING;
      }

      parameters = new PlanarRegionMappingParameters(version);
      finalMap = new PlanarRegionsList();
   }

   public void reset()
   {
      initialized = false;
      finalMap.clear();
   }

   public void submitRegionsUsingIterativeReduction(PlanarRegionsListWithPose regionsWithPose)
   {
      PlanarRegionsList regionsInSensorFrame = new PlanarRegionsList();

      // Filters out regions that have area less than threshold
      for (PlanarRegion region : regionsWithPose.getPlanarRegionsList().getPlanarRegionsAsList())
      {
         if (region.getArea() > parameters.getMinimumPlanarRegionArea())
         {
            regionsInSensorFrame.addPlanarRegion(region);
         }
      }

      PlanarRegionsList priorRegionsInWorld = new PlanarRegionsList();
      PlanarRegionsList posteriorRegionsInWorld = new PlanarRegionsList();

      LogTools.info("--------------------------------------------- New Iteration [{}] ------------------------------------------", sensorPoseIndex);
      LogTools.info("Incoming: {}", regionsInSensorFrame.getPlanarRegionsAsList().size());

      if (regionsInSensorFrame.getNumberOfPlanarRegions() == 0)
      {
         LogTools.warn("Number Of Regions is 0!");
         return;
      }

      // Compute relative (t) to (t-1) transform
      currentToPreviousSensorTransform.set(regionsWithPose.getSensorToWorldFrameTransform());
      currentToPreviousSensorTransform.multiply(worldToPreviousSensorFrameTransform);

      // Compute the new prior (t_prior) transform
      sensorToWorldFrameTransformPrior.set(currentToPreviousSensorTransform);
      sensorToWorldFrameTransformPrior.multiply(sensorToWorldTransformPosterior);

      LogTools.info("Current to Previous: \n{}", currentToPreviousSensorTransform);
      LogTools.info("Sensor To World [Prior]: \n{}", sensorToWorldFrameTransformPrior);
      LogTools.info("Sensor To World [Posterior]: \n{}", sensorToWorldTransformPosterior);

      // Generate regions in world frame with prior transform
      priorRegionsInWorld.addPlanarRegionsList(regionsInSensorFrame.copy());
      priorRegionsInWorld.applyTransform(sensorToWorldFrameTransformPrior);

      modified = true;

      // Assign unique IDs to all incoming regions
      for (PlanarRegion region : priorRegionsInWorld.getPlanarRegionsAsList())
      {
         if (!initialized)
            region.setRegionId(uniqueIDtracker++);
         else
            region.setRegionId(-uniqueIDtracker++);
      }

      PerceptionPrintTools.printRegionIDs("Incoming", priorRegionsInWorld);

      if (!initialized)
      {
         finalMap.addPlanarRegionsList(priorRegionsInWorld);
         if (merger == MergingMode.SMOOTHING)
         {
            initializeFactorGraphForSmoothing(finalMap, sensorToWorldFrameTransformPrior);
         }
         sensorToWorldTransformPosterior.set(regionsWithPose.getSensorToWorldFrameTransform());

         initialized = true;
      }
      else
      {
         PerceptionPrintTools.printRegionIDs("Map", finalMap);

         PlanarRegionSLAMTools.findPlanarRegionMatches(finalMap, priorRegionsInWorld, incomingToMapMatches,
                                                       (float) parameters.getMinimumOverlapThreshold(),
                                                       (float) parameters.getSimilarityThresholdBetweenNormals(),
                                                       (float) parameters.getOrthogonalDistanceThreshold(),
                                                       (float) parameters.getMinimumBoundingBoxSize());

         PerceptionPrintTools.printMatches("Cross Matches", finalMap, priorRegionsInWorld, incomingToMapMatches);

         if (merger == MergingMode.SMOOTHING)
         {
            // Find the optimal transform for incoming regions before merge
            applyFactorGraphBasedSmoothing(finalMap, regionsInSensorFrame.copy(), sensorToWorldFrameTransformPrior,
                                           currentToPreviousSensorTransform, incomingToMapMatches, sensorPoseIndex);

            // Extract the optimal transform from the factor graph
            double[] transformArray = new double[16];
            factorGraph.getPoseById(sensorPoseIndex, transformArray);

            sensorToWorldTransformPosterior.set(transformArray);

            // Transform the incoming regions to the map frame with the optimal transform
            posteriorRegionsInWorld.clear();
            regionsInSensorFrame.copy().getPlanarRegionsAsList().forEach(region -> {
               region.applyTransform(sensorToWorldTransformPosterior);
               posteriorRegionsInWorld.addPlanarRegion(region);
            });

            LogTools.info("Estimated Transform: \n{}", regionsWithPose.getSensorToWorldFrameTransform());
            LogTools.info("Sensor To World [Prior]: \n{}", sensorToWorldFrameTransformPrior);
            LogTools.info("Sensor To World [Posterior (Optimized)]: \n{}", sensorToWorldTransformPosterior);
         }

         // merge all the new regions in
         finalMap = crossReduceRegionsIteratively(finalMap, posteriorRegionsInWorld);
         processUniqueRegions(finalMap);

         // Clear ISAM2 for the next update
         factorGraph.clearISAM2();
      }

      previousSensorToWorldFrameTransform.set(regionsWithPose.getSensorToWorldFrameTransform());
      worldToPreviousSensorFrameTransform.setAndInvert(previousSensorToWorldFrameTransform);

      sensorPoseIndex++;

      finalMap.getPlanarRegionsAsList().forEach(region -> mapIDs.add(region.getRegionId()));
      priorRegionsInWorld.getPlanarRegionsAsList().forEach(region -> incomingIDs.add(region.getRegionId()));

      LogTools.info("Map IDs: {}", Arrays.toString(mapIDs.toArray()));

      mapIDs.clear();
      incomingIDs.clear();

      currentTimeIndex++;
      LogTools.debug("-------------------------------------------------------- Done --------------------------------------------------------------\n");
   }

   public void submitRegionsUsingGraphicalReduction(PlanarRegionsListWithPose regionsWithPose)
   {
      PlanarRegionsList regions = regionsWithPose.getPlanarRegionsList();
      modified = true;
      if (!initialized)
      {
         regions.getPlanarRegionsAsList().forEach(region ->
                                                  {
                                                     region.setRegionId(uniqueIDtracker++);
                                                     mapRegionIDSet.add(region.getRegionId());
                                                  });
         finalMap.addPlanarRegionsList(regions);

         initialized = true;
      }
      else
      {
         // initialize the planar region graph
         PlanarRegionGraph planarRegionGraph = new PlanarRegionGraph();
         finalMap.getPlanarRegionsAsList().forEach(planarRegionGraph::addRootOfBranch);

         // assign unique ids to the incoming regions
         regions.getPlanarRegionsAsList().forEach(region -> region.setRegionId(uniqueIDtracker++));

         addIncomingRegionsToGraph(finalMap,
                                   regions,
                                   planarRegionGraph,
                                   (float) parameters.getSimilarityThresholdBetweenNormals(),
                                   (float) parameters.getOrthogonalDistanceThreshold(),
                                   (float) parameters.getMaxInterRegionDistance());

         // merge all the new regions in
         planarRegionGraph.collapseGraphByMerging(parameters.getUpdateAlphaTowardsMatch());

         // go back through the existing regions and add them to the graph to check for overlap
         checkMapRegionsForOverlap(planarRegionGraph,
                                   (float) parameters.getSimilarityThresholdBetweenNormals(),
                                   (float) parameters.getOrthogonalDistanceThreshold(),
                                   (float) parameters.getMaxInterRegionDistance(),
                                   (float) parameters.getMinimumBoundingBoxSize());

         planarRegionGraph.collapseGraphByMerging(parameters.getUpdateAlphaTowardsMatch());

         finalMap = planarRegionGraph.getAsPlanarRegionsList();

         LogTools.debug("Regions: {}, Unique: {}, Map: {}",
                        regions.getPlanarRegionsAsList().size(),
                        uniqueRegionsFound,
                        finalMap.getPlanarRegionsAsList().size());

         LogTools.debug("Unique Set: [{}]", mapRegionIDSet);
      }
   }

   public static void addIncomingRegionsToGraph(PlanarRegionsList map,
                                                PlanarRegionsList incoming,
                                                PlanarRegionGraph graphToUpdate,
                                                float normalThreshold,
                                                float normalDistanceThreshold,
                                                float distanceThreshold)
   {
      PlanarRegionTools planarRegionTools = new PlanarRegionTools();

      List<PlanarRegion> incomingRegions = incoming.getPlanarRegionsAsList();
      List<PlanarRegion> mapRegions = map.getPlanarRegionsAsList();

      for (int incomingRegionId = 0; incomingRegionId < incomingRegions.size(); incomingRegionId++)
      {
         PlanarRegion newRegion = incomingRegions.get(incomingRegionId);
         boolean foundMatch = false;

         for (int mapRegionIndex = 0; mapRegionIndex < mapRegions.size(); mapRegionIndex++)
         {
            PlanarRegion mapRegion = mapRegions.get(mapRegionIndex);

            //if (boxesIn3DIntersect(mapRegion, newRegion, mapRegion.getBoundingBox3dInWorld().getMaxZ() - mapRegion.getBoundingBox3dInWorld().getMinZ()))
            {
               Point3D newOrigin = new Point3D();
               newRegion.getOrigin(newOrigin);

               Point3D mapOrigin = new Point3D();
               mapRegion.getOrigin(mapOrigin);

               Vector3D originVec = new Vector3D();
               originVec.sub(newOrigin, mapOrigin);

               double normalDistance = Math.abs(originVec.dot(mapRegion.getNormal()));
               double normalSimilarity = newRegion.getNormal().dot(mapRegion.getNormal());

               // check to make sure the angles are similar enough
               boolean wasMatched = normalSimilarity > normalThreshold;
               // check that the regions aren't too far out of plane with one another. TODO should check this normal distance measure. That's likely a problem
               wasMatched &= normalDistance < normalDistanceThreshold;
               // check that the regions aren't too far from one another
               if (wasMatched)
                  wasMatched = planarRegionTools.getDistanceBetweenPlanarRegions(mapRegion, newRegion) <= distanceThreshold;

               //LogTools.debug(String.format("(%d): (%d -> %d) Metrics: (%.3f > %.3f), (%.3f < %.3f), (%.3f < %.3f)",
               //                            mapRegionIndex,
               //                            mapRegion.getRegionId(),
               //                            newRegion.getRegionId(),
               //                            normalSimilarity,
               //                            normalThreshold,
               //                            normalDistance,
               //                            normalDistanceThreshold,
               //                            originDistance,
               //                            distanceThreshold) + ": [{}]", wasMatched);

               if (wasMatched)
               {
                  graphToUpdate.addEdge(mapRegion, newRegion);
                  foundMatch = true;
               }
            }
         }

         if (!foundMatch)
            graphToUpdate.addRootOfBranch(newRegion);
      }
   }

   public static void checkMapRegionsForOverlap(PlanarRegionGraph graphToUpdate, float normalThreshold, float normalDistanceThreshold, float distanceThreshold, float minBoxSize)
   {
      List<PlanarRegion> mapRegions = graphToUpdate.getAsPlanarRegionsList().getPlanarRegionsAsList();

      for (int idA = 0; idA < mapRegions.size(); idA++)
      {
         PlanarRegion regionA = mapRegions.get(idA);

         for (int idB = idA + 1; idB < mapRegions.size(); idB++)
         {
            PlanarRegion regionB = mapRegions.get(idB);

            boolean wasMatched = PlanarRegionSLAMTools.checkRegionsForOverlap(regionA, regionB, normalThreshold, normalDistanceThreshold, distanceThreshold, minBoxSize);

            if (wasMatched)
            {
               graphToUpdate.addEdge(regionB, regionA);
            }
         }
      }
   }

   public PlanarRegionsList selfReduceRegionsIteratively(PlanarRegionsList map)
   {
      boolean changed = false;
      do
      {
         changed = false;

         int parentIndex = 0;
         int childIndex = 0;

         //LogTools.info("Change Iteration: MapTotal({}) - Parent({}) - Child({})", map.getNumberOfPlanarRegions(), parentIndex, childIndex);

         while (parentIndex < map.getNumberOfPlanarRegions())
         {
            //LogTools.info("Parent Iteration: MapTotal({}) - Parent({}) - Child({})", map.getNumberOfPlanarRegions(), parentIndex, childIndex);
            childIndex = 0;
            PlanarRegion parentRegion = map.getPlanarRegion(parentIndex);
            while (childIndex < map.getNumberOfPlanarRegions())
            {
               if (parentIndex == childIndex)
               {
                  childIndex++;
               }
               else
               {
                  int parentId = map.getPlanarRegion(parentIndex).getRegionId();
                  int childId = map.getPlanarRegion(childIndex).getRegionId();

                  PlanarRegion childRegion = map.getPlanarRegion(childIndex);

                  //LogTools.info("Checking Match: Parent({}) - Child({})", parentId, childId);

                  if (PlanarRegionSLAMTools.checkRegionsForOverlap(parentRegion,
                                                                   childRegion,
                                                                   (float) parameters.getSimilarityThresholdBetweenNormals(),
                                                                   (float) parameters.getOrthogonalDistanceThreshold(),
                                                                   (float) parameters.getMinimumOverlapThreshold(),
                                                                   (float) parameters.getMinimumBoundingBoxSize()))
                  {
                     // Request a merge for parent and child regions. If they don't merge, pick the parent if it belongs to the map (based on sign of ID)
                     if (PlanarRegionSLAMTools.mergeRegionIntoParent(parentRegion, childRegion, parameters.getUpdateAlphaTowardsMatch()))
                     {
                        //LogTools.info("Merged({},{}) -> Removing({})", parentIndex, childIndex, childIndex);

                        // Generate ID for the merged region
                        int finalId = generatePostMergeId(parentRegion.getRegionId(), childRegion.getRegionId());
                        parentRegion.setRegionId(finalId);
                        changed = true;

                        map.getPlanarRegionsAsList().remove(childRegion);
                     }
                     else
                     {
                        childIndex++;
                     }
                  }
                  else
                  {
                     childIndex++;
                  }

                  if (changed)
                     break;
               }
               if (changed)
                  break;
            }
            parentIndex++;
         }
      }
      while (changed);

      return map;
   }

   public PlanarRegionsList crossReduceRegionsIteratively(PlanarRegionsList map, PlanarRegionsList regions)
   {
      LogTools.info("Performing Cross Reduction");
      map.addPlanarRegionsList(regions);
      map = selfReduceRegionsIteratively(map);
      return map;
   }

   public void applyFactorGraphBasedSmoothing(PlanarRegionsList mapRegionList,
                                              PlanarRegionsList incomingRegionList,
                                              RigidBodyTransform sensorToWorldTransformPrior,
                                              RigidBodyTransform currentToPreviousTransform,
                                              HashMap<Integer, TIntArrayList> matches,
                                              int poseIndex)
   {
      LogTools.info("------------------------------- Performing Factor Graph Based Smoothing ---------------------------------");

      LogTools.info("Adding odometry factor: x{} -> x{}", poseIndex - 1, poseIndex);
      factorGraph.addOdometryFactorExtended(poseIndex, PerceptionEuclidTools.toArray(currentToPreviousTransform));

      LogTools.info("Adding Pose Initial Value: {}", poseIndex);
      factorGraph.setPoseInitialValueExtended(poseIndex, PerceptionEuclidTools.toArray(sensorToWorldTransformPrior));

      for (Integer incomingIndex : matches.keySet())
      {
         int incomingId = incomingRegionList.getPlanarRegion(incomingIndex).getRegionId();
         PlanarRegion incomingRegion = incomingRegionList.getPlanarRegion(incomingIndex);

         int landmarkId = findBestMatchIndex(incomingId, mapRegionList, matches.get(incomingIndex));

         if(landmarkId > 0)
         {
            LogTools.info("Best Match: Incoming({}) -> Map({})", incomingId, landmarkId);
            insertLandmarkFactorAndValue(incomingRegion, sensorToWorldTransformPrior, poseIndex, landmarkId);
         }
      }

      LogTools.info("Solving factor graph");
      factorGraph.optimizeISAM2((byte) 4);

      factorGraph.printResults();

      LogTools.info("+++++++++ --------- +++++++++ Done (Smoothing) +++++++++ --------- +++++++++");
   }

   public void insertLandmarkFactorAndValue(PlanarRegion childRegion,
                                            RigidBodyTransform sensorToWorldTransformPrior,
                                            int poseId,
                                            int landmarkId)
   {

      // Get origin and normal in sensor frame.
      Point3D childOrigin = new Point3D();
      Vector3D childNormal = new Vector3D();
      childRegion.getOrigin(childOrigin);
      childRegion.getNormal(childNormal);

      // Get origin and normal in world frame
      Point3D childOriginInWorld = new Point3D(childOrigin);
      Vector3D childNormalInWorld = new Vector3D(childNormal);
      childOriginInWorld.applyTransform(sensorToWorldTransformPrior);
      childNormalInWorld.applyTransform(sensorToWorldTransformPrior);

      // Compute plane parameters in sensor frame
      double localDot = childOrigin.dot(childNormal);
      float[] planeInSensorFrame = new float[] {childNormal.getX32(), childNormal.getY32(), childNormal.getZ32(), (float) -localDot};

      // Insert plane factor based on planar region normal and origin in sensor frame
      LogTools.info("Adding plane factor: x{} -> l{} ({})", poseId, landmarkId, Arrays.toString(planeInSensorFrame));
      factorGraph.addOrientedPlaneFactor(landmarkId, poseId, planeInSensorFrame);

      // Compute plane parameters in world frame
      double worldDot = childOriginInWorld.dot(childNormalInWorld);
      float[] planeInWorldFrame = new float[] {childNormalInWorld.getX32(), childNormalInWorld.getY32(), childNormalInWorld.getZ32(), (float) -worldDot};

      // Set initial value for plane based on planar region normal and origin in world frame
      LogTools.info("Setting plane value: {} {}", landmarkId, Arrays.toString(planeInWorldFrame));
      factorGraph.setOrientedPlaneInitialValue(landmarkId, planeInWorldFrame);
   }

   public void initializeFactorGraphForSmoothing(PlanarRegionsList map, RigidBodyTransform transform)
   {
      LogTools.info("------------------------------- Initializing Factor Graph ---------------------------------");

      // Set up the factor graph noise models
      odomNoise = (float) parameters.getOdometryNoiseVariance();
      planeNoise = (float) parameters.getPlaneNoiseVariance();

      factorGraph.createOdometryNoiseModel(new float[] {odomNoise, odomNoise, odomNoise, odomNoise, odomNoise, odomNoise});
      factorGraph.createOrientedPlaneNoiseModel(new float[] {planeNoise, planeNoise, planeNoise});

      factorGraph.addPriorPoseFactorExtended(0, PerceptionEuclidTools.toArray(transform));
      factorGraph.setPoseInitialValueExtended(0, PerceptionEuclidTools.toArray(transform));

      for (PlanarRegion region : map.getPlanarRegionsAsList())
      {
         // Get origin and normal in sensor frame.
         Point3D childOrigin = new Point3D();
         Vector3D childNormal = new Vector3D();
         region.getOrigin(childOrigin);
         region.getNormal(childNormal);

         // Insert plane factor based on planar region normal and origin in sensor frame
         double localDot = childOrigin.dot(childNormal);
         float[] plane = new float[] {childNormal.getX32(), childNormal.getY32(), childNormal.getZ32(), (float) -localDot};
         factorGraph.addOrientedPlaneFactor(region.getRegionId(), 0, plane);
         factorGraph.setOrientedPlaneInitialValue(region.getRegionId(), plane);
      }

      LogTools.info("+++++++++ --------- +++++++++ Done (Smoothing Initialization) +++++++++ --------- +++++++++");
   }

   private int findBestMatchIndex(int incomingId, PlanarRegionsList mapRegions, TIntArrayList matchedMapIndices)
   {
      int landmarkId = 0;

      for (Integer mapIndex : matchedMapIndices.toArray())
      {
         int mapId = mapRegions.getPlanarRegion(mapIndex).getRegionId();
         int bestLandmarkId = Math.abs(generatePostMergeId(incomingId, mapId));

         if (bestLandmarkId > 0)
            landmarkId = generatePostMergeId(bestLandmarkId, landmarkId);
      }

      return landmarkId;
   }

   private int generatePostMergeId(int parentIndex, int childIndex)
   {
      int idToReturn = 0;
      if(parentIndex == 0)
      {
         idToReturn = childIndex;
      }
      else if(childIndex == 0)
      {
         idToReturn = parentIndex;
      }
      else if (parentIndex > 0 && childIndex > 0) // Both belong to the map
      {
         idToReturn = Math.min(parentIndex, childIndex);
         //LogTools.info("Action: Both belong to the map: ({}) ({}) -> ({})", parentIndex, childIndex, idToReturn);
      }
      else if (parentIndex > 0 && childIndex < 0) // Parent belongs to the map, child belongs to the incoming regions
      {
         idToReturn = parentIndex;
         //LogTools.info("Action: Parent in map, child incoming: ({}) ({}) -> ({})", parentIndex, childIndex, idToReturn);
      }
      else if (parentIndex < 0 && childIndex > 0) // Parent belongs to the incoming regions, child belongs to the map
      {
         idToReturn = childIndex;
         //LogTools.info("Action: Parent incoming, child in map: ({}) ({}) -> ({})", parentIndex, childIndex, idToReturn);
      }
      else if (parentIndex < 0 && childIndex < 0) // Both belong to the incoming regions
      {
         idToReturn = Math.max(parentIndex, childIndex);
         //LogTools.info("Action: Both incoming: ({}) ({}) -> ({})", parentIndex, childIndex, idToReturn);
      }
      return idToReturn;
   }

   private void processUniqueRegions(PlanarRegionsList map)
   {
      LogTools.info("------------------------------- Processing Unique Region IDs ---------------------------------");
      for (PlanarRegion region : map.getPlanarRegionsAsList())
      {
         region.setRegionId(Math.abs(region.getRegionId()));
      }
      LogTools.info("+++++++++ --------- +++++++++ Done (Processing Unique Region IDs) +++++++++ --------- +++++++++");
   }

   public PlanarRegionsList getMapRegions()
   {
      return finalMap;
   }

   public boolean isModified()
   {
      return modified;
   }

   public void setModified(boolean modified)
   {
      this.modified = modified;
   }

   public PlanarRegionMappingParameters getParameters()
   {
      return parameters;
   }

   public void destroy()
   {
      factorGraph.clearISAM2();
   }

   public RigidBodyTransform getOptimalSensorToWorldTransform()
   {
      return sensorToWorldTransformPosterior;
   }

   public Vector4D getOptimalLandmarkById(int landmarkId)
   {
      double[] landmark = new double[4];
      factorGraph.getPlanarLandmarkById(landmarkId, landmark);
      return new Vector4D(landmark);
   }
}

