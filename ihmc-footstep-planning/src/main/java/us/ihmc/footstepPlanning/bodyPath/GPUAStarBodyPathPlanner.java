package us.ihmc.footstepPlanning.bodyPath;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.bytedeco.opencl._cl_kernel;
import org.bytedeco.opencl._cl_program;
import org.bytedeco.opencl.global.OpenCL;
import org.bytedeco.opencv.global.opencv_core;
import us.ihmc.commons.MathTools;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.UnitVector3D;
import us.ihmc.euclid.tuple3D.interfaces.UnitVector3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.*;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.log.AStarBodyPathEdgeData;
import us.ihmc.footstepPlanning.log.AStarBodyPathIterationData;
import us.ihmc.log.LogTools;
import us.ihmc.pathPlanning.graph.structure.DirectedGraph;
import us.ihmc.pathPlanning.graph.structure.GraphEdge;
import us.ihmc.pathPlanning.graph.structure.NodeComparator;
import us.ihmc.perception.BytedecoImage;
import us.ihmc.perception.OpenCLManager;
import us.ihmc.sensorProcessing.heightMap.HeightMapData;
import us.ihmc.sensorProcessing.heightMap.HeightMapTools;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoVariable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GPUAStarBodyPathPlanner
{
   private static final int defaultMapWidth = (int) (5.0 / 0.03);

   private static final boolean debug = false;
   private static final boolean useRANSACTraversibility = true;

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final FootstepPlannerParametersReadOnly parameters;
   private final AStarBodyPathPlannerParametersReadOnly plannerParameters;
   private final AStarBodyPathEdgeData edgeData;
   private HeightMapData heightMapData;
   private final HashSet<BodyPathLatticePoint> expandedNodeSet = new HashSet<>();
   private final DirectedGraph<BodyPathLatticePoint> graph = new DirectedGraph<>();
   private final List<BodyPathLatticePoint> neighbors = new ArrayList<>();

   private final YoBoolean containsCollision = new YoBoolean("containsCollision", registry);
   private final YoDouble edgeCost = new YoDouble("edgeCost", registry);
   private final YoDouble deltaHeight = new YoDouble("deltaHeight", registry);
   private final YoDouble snapHeight = new YoDouble("snapHeight", registry);
   private final YoDouble incline = new YoDouble("incline", registry);
   private final YoDouble inclineCost = new YoDouble("inclineCost", registry);
   private final YoDouble traversibilityCost = new YoDouble("traversibilityCost", registry);
   private final YoDouble roll = new YoDouble("roll", registry);
   private final YoDouble rollCost = new YoDouble("rollCost", registry);
   private final YoDouble nominalIncline = new YoDouble("nominalIncline", registry);
   private final YoFrameVector3D leastSqNormal = new YoFrameVector3D("leastSqNormal", ReferenceFrame.getWorldFrame(), registry);
   private final YoDouble heuristicCost = new YoDouble("heuristicCost", registry);
   private final YoDouble totalCost = new YoDouble("totalCost", registry);

   private final HashMap<BodyPathLatticePoint, Double> rollMap = new HashMap<>();

   private final PriorityQueue<BodyPathLatticePoint> stack;
   private BodyPathLatticePoint startNode, goalNode;
   private final HashMap<BodyPathLatticePoint, Double> gridHeightMap = new HashMap<>();
   private BodyPathLatticePoint leastCostNode = null;
   private final YoEnum<RejectionReason> rejectionReason = new YoEnum<>("rejectionReason", registry, RejectionReason.class, true);

   /* Indicator of how flat and planar and available footholds are, using least squares */
   private final BodyPathLSTraversibilityCalculator leastSqTraversibilityCalculator;
   /* Indicator of how flat and planar and available footholds are */
   private final BodyPathRANSACTraversibilityCalculator ransacTraversibilityCalculator;
   /* Performs box collision check */
   private final BodyPathCollisionDetector collisionDetector = new BodyPathCollisionDetector();
   /* Computes surface normals with RANSAC, used for traversibility */
   private final HeightMapRANSACNormalCalculator ransacNormalCalculator = new HeightMapRANSACNormalCalculator();

   private final TIntArrayList xSnapOffsets = new TIntArrayList();
   private final TIntArrayList ySnapOffsets = new TIntArrayList();

   private final List<AStarBodyPathIterationData> iterationData = new ArrayList<>();
   private final HashMap<GraphEdge<BodyPathLatticePoint>, AStarBodyPathEdgeData> edgeDataMap = new HashMap<>();

   private final List<Consumer<FootstepPlannerOutput>> statusCallbacks;
   private final Stopwatch stopwatch;
   private double planningStartTime;
   private int iterations = 0;
   private BodyPathPlanningResult result = null;
   private boolean reachedGoal = false;
   private final AtomicBoolean haltRequested = new AtomicBoolean();
   private static final int maxIterations = 3000;

   private final OpenCLManager openCLManager;
   private _cl_program pathPlannerProgram;
   private _cl_kernel computeNormalsWithLeastSquaresKernel;

   private ByteBuffer heightMapDataBuffer;
   private BytedecoImage heightMapImage;
   private BytedecoImage normalXImage;
   private BytedecoImage normalYImage;
   private BytedecoImage normalZImage;
   private BytedecoImage sampledHeightImage;

   private boolean firstTick = true;

   /* Parameters to extract */
   static final double groundClearance = 0.3;
   static final double maxIncline = Math.toRadians(55.0);
   static final double snapRadius = 0.15;
   static final double boxSizeY = 1.2;
   static final double boxSizeX = 0.35;

   public GPUAStarBodyPathPlanner(FootstepPlannerParametersReadOnly parameters,
                                  AStarBodyPathPlannerParametersReadOnly plannerParameters,
                                  SideDependentList<ConvexPolygon2D> footPolygons)
   {
      this(parameters, plannerParameters, footPolygons, new Stopwatch());
   }

   public GPUAStarBodyPathPlanner(FootstepPlannerParametersReadOnly parameters,
                                  AStarBodyPathPlannerParametersReadOnly plannerParameters,
                                  SideDependentList<ConvexPolygon2D> footPolygons,
                                  Stopwatch stopwatch)
   {
      this(parameters, plannerParameters, footPolygons, new ArrayList<>(), stopwatch);
   }

   public GPUAStarBodyPathPlanner(FootstepPlannerParametersReadOnly parameters,
                                  AStarBodyPathPlannerParametersReadOnly plannerParameters,
                                  SideDependentList<ConvexPolygon2D> footPolygons,
                                  List<Consumer<FootstepPlannerOutput>> statusCallbacks,
                                  Stopwatch stopwatch)
   {
      this.parameters = parameters;
      this.plannerParameters = plannerParameters;
      this.statusCallbacks = statusCallbacks;
      this.stopwatch = stopwatch;
      stack = new PriorityQueue<>(new NodeComparator<>(graph, this::heuristics));

      openCLManager = new OpenCLManager();
      create(defaultMapWidth);

      if (useRANSACTraversibility)
      {
         ransacTraversibilityCalculator = new BodyPathRANSACTraversibilityCalculator(gridHeightMap::get, ransacNormalCalculator, registry);
         leastSqTraversibilityCalculator = null;
      }
      else
      {
         leastSqTraversibilityCalculator = new BodyPathLSTraversibilityCalculator(parameters, footPolygons, gridHeightMap, registry);
         ransacTraversibilityCalculator = null;
      }

      List<YoVariable> allVariables = registry.collectSubtreeVariables();
      this.edgeData = new AStarBodyPathEdgeData(allVariables.size());
      graph.setGraphExpansionCallback(edge ->
                                      {
                                         for (int i = 0; i < allVariables.size(); i++)
                                         {
                                            edgeData.setData(i, allVariables.get(i).getValueAsLongBits());
                                         }

                                         edgeData.setParentNode(edge.getStartNode());
                                         edgeData.setChildNode(edge.getEndNode());
                                         edgeData.setChildSnapHeight(gridHeightMap.get(edge.getEndNode()));

                                         edgeDataMap.put(edge, edgeData.getCopyAndClear());

                                         containsCollision.set(false);
                                         deltaHeight.set(Double.NaN);
                                         edgeCost.set(Double.NaN);
                                         deltaHeight.set(Double.NaN);
                                         rejectionReason.set(null);
                                         leastSqNormal.setToZero();
                                         roll.set(0.0);
                                         incline.set(0.0);
                                         heuristicCost.setToNaN();
                                         totalCost.setToNaN();
                                         ransacTraversibilityCalculator.clearVariables();
                                      });
   }

   public void create(int numberOfCells)
   {
      pathPlannerProgram = openCLManager.loadProgram("BodyPathPlanner");
      computeNormalsWithLeastSquaresKernel = openCLManager.createKernel(pathPlannerProgram, "computeSurfaceNormalsWithLeastSquares");

      heightMapDataBuffer = ByteBuffer.allocate(Float.BYTES * numberOfCells * numberOfCells);
      this.heightMapImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1, heightMapDataBuffer);

      // TODO switch these al lfrom images to buffers
      this.normalXImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1);
      this.normalYImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1);
      this.normalZImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1);
      this.sampledHeightImage = new BytedecoImage(numberOfCells, numberOfCells, opencv_core.CV_32FC1);

      normalXImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);
      normalYImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);
      normalZImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);
      sampledHeightImage.createOpenCLImage(openCLManager, OpenCL.CL_MEM_READ_WRITE);
   }

   public void destroy()
   {
      pathPlannerProgram.close();
      computeNormalsWithLeastSquaresKernel.close();

      heightMapImage.destroy(openCLManager);
      normalXImage.destroy(openCLManager);
      normalYImage.destroy(openCLManager);
      normalZImage.destroy(openCLManager);
      sampledHeightImage.destroy(openCLManager);

      openCLManager.destroy();
   }

   public void setHeightMapData(HeightMapData heightMapData)
   {
      if (this.heightMapData == null || !EuclidCoreTools.epsilonEquals(this.heightMapData.getGridResolutionXY(), heightMapData.getGridResolutionXY(), 1e-3))
      {
         collisionDetector.initialize(heightMapData.getGridResolutionXY(), boxSizeX, boxSizeY);
      }

      this.heightMapData = heightMapData;
      ransacNormalCalculator.initialize(heightMapData);
      rollMap.clear();

      if (useRANSACTraversibility)
      {
         ransacTraversibilityCalculator.setHeightMap(heightMapData);
      }
      else
      {
         leastSqTraversibilityCalculator.setHeightMap(heightMapData);
      }
   }

   static void packRadialOffsets(HeightMapData heightMapData, double radius, TIntArrayList xOffsets, TIntArrayList yOffsets)
   {
      int minMaxOffsetXY = (int) Math.round(radius / heightMapData.getGridResolutionXY());

      xOffsets.clear();
      yOffsets.clear();

      for (int i = -minMaxOffsetXY; i <= minMaxOffsetXY; i++)
      {
         for (int j = -minMaxOffsetXY; j <= minMaxOffsetXY; j++)
         {
            double x = i * heightMapData.getGridResolutionXY();
            double y = j * heightMapData.getGridResolutionXY();
            if (EuclidCoreTools.norm(x, y) < radius && !(i == 0 && j == 0))
            {
               xOffsets.add(i);
               yOffsets.add(j);
            }
         }
      }
   }

   private enum RejectionReason
   {
      INVALID_SNAP,
      TOO_STEEP,
      STEP_TOO_HIGH,
      COLLISION,
      NON_TRAVERSIBLE
   }

   public void handleRequest(FootstepPlannerRequest request, FootstepPlannerOutput outputToPack)
   {
      haltRequested.set(false);
      iterations = 0;
      reachedGoal = false;
      stopwatch.start();
      result = BodyPathPlanningResult.PLANNING;
      planningStartTime = stopwatch.totalElapsed();
      stopwatch.lap();

      iterationData.clear();
      edgeDataMap.clear();
      gridHeightMap.clear();

      packRadialOffsets(heightMapData, snapRadius, xSnapOffsets, ySnapOffsets);

      Pose3D startPose = new Pose3D();
      Pose3D goalPose = new Pose3D();

      startPose.interpolate(request.getStartFootPoses().get(RobotSide.LEFT), request.getStartFootPoses().get(RobotSide.RIGHT), 0.5);
      goalPose.interpolate(request.getGoalFootPoses().get(RobotSide.LEFT), request.getGoalFootPoses().get(RobotSide.RIGHT), 0.5);

      startNode = new BodyPathLatticePoint(startPose.getX(), startPose.getY());
      goalNode = new BodyPathLatticePoint(goalPose.getX(), goalPose.getY());
      stack.clear();
      stack.add(startNode);
      graph.initialize(startNode);
      expandedNodeSet.clear();
      gridHeightMap.put(startNode, startPose.getZ());
      leastCostNode = startNode;
      nominalIncline.set(Math.atan2(goalPose.getZ() - startPose.getZ(), goalPose.getPosition().distanceXY(startPose.getPosition())));

      if (plannerParameters.getComputeSurfaceNormalCost())
      {
         double patchWidth = 0.3;
         computeSurfaceNormals(patchWidth);
      }

      if (useRANSACTraversibility)
      {
         ransacNormalCalculator.initialize(heightMapData);
         ransacTraversibilityCalculator.initialize(startNode);
      }
      else
      {
         leastSqTraversibilityCalculator.initialize(startNode);
      }

      planningLoop:
      while (true)
      {
         iterations++;
         outputToPack.getPlannerTimings().setPathPlanningIterations(iterations);

         if (stopwatch.totalElapsed() >= request.getTimeout())
         {
            result = BodyPathPlanningResult.TIMED_OUT_BEFORE_SOLUTION;
            break;
         }
         if (haltRequested.get())
         {
            result = BodyPathPlanningResult.HALTED;
            break;
         }
         if (iterations > maxIterations)
         {
            result = BodyPathPlanningResult.MAXIMUM_ITERATIONS_REACHED;
            break;
         }

         BodyPathLatticePoint node = getNextNode();
         if (node == null)
         {
            result = BodyPathPlanningResult.NO_PATH_EXISTS;
            if (debug)
            {
               LogTools.info("Stack is empty, no path exists...");
            }
            break;
         }

         populateNeighbors(node);

         double parentSnapHeight = gridHeightMap.get(node);
         for (int neighborIndex = 0; neighborIndex < neighbors.size(); neighborIndex++)
         {
            BodyPathLatticePoint neighbor = neighbors.get(neighborIndex);

            heuristicCost.set(xyDistance(neighbor, goalNode));

            if (!checkEdge(node, parentSnapHeight, neighbor, neighborIndex))
               continue;

            computeEdgeCost(node, neighbor);

            totalCost.set(heuristicCost.getValue() + edgeCost.getValue());
            graph.checkAndSetEdge(node, neighbor, edgeCost.getValue());
            stack.add(neighbor);

            if (node.equals(goalNode))
            {
               reachedGoal = true;
               result = BodyPathPlanningResult.FOUND_SOLUTION;
               break planningLoop;
            }
            else if (heuristics(node) < heuristics(leastCostNode))
            {
               leastCostNode = node;
            }
         }

         expandedNodeSet.add(node);

         AStarBodyPathIterationData iterationData = new AStarBodyPathIterationData();
         iterationData.setParentNode(node);
         iterationData.getChildNodes().addAll(neighbors);
         iterationData.setParentNodeHeight(parentSnapHeight);
         this.iterationData.add(iterationData);

         if (publishStatus(request))
         {
            reportStatus(request, outputToPack);
            stopwatch.lap();
         }
      }

      reportStatus(request, outputToPack);
   }

   private void computeSurfaceNormals(double patchWidth)
   {
      int numberOfCells = heightMapData.getCellsPerAxis();
      // set the kernel arguments
      openCLManager.setKernelArgument(computeNormalsWithLeastSquaresKernel, 0, normalXImage.getOpenCLImageObject());

      openCLManager.execute2D(computeNormalsWithLeastSquaresKernel, numberOfCells, numberOfCells);

      // get the data from the kernel
      openCLManager.enqueueReadImage(normalXImage.getOpenCLImageObject(), numberOfCells, numberOfCells, normalXImage.getBytedecoByteBufferPointer());
      openCLManager.enqueueReadImage(normalYImage.getOpenCLImageObject(), numberOfCells, numberOfCells, normalYImage.getBytedecoByteBufferPointer());
      openCLManager.enqueueReadImage(normalZImage.getOpenCLImageObject(), numberOfCells, numberOfCells, normalZImage.getBytedecoByteBufferPointer());
   }

   private UnitVector3DReadOnly getSurfaceNormal(int key)
   {
      return new UnitVector3D(normalXImage.getBackingDirectByteBuffer().getFloat(Float.BYTES * key),
                              normalYImage.getBackingDirectByteBuffer().getFloat(Float.BYTES * key),
                              normalZImage.getBackingDirectByteBuffer().getFloat(Float.BYTES * key));
   }

   private boolean checkEdge(BodyPathLatticePoint node, double nodeSnapHeight, BodyPathLatticePoint neighbor, int neighborIndex)
   {
      this.snapHeight.set(snap(neighbor));
      if (Double.isNaN(snapHeight.getDoubleValue()))
      {
         rejectionReason.set(RejectionReason.INVALID_SNAP);
         graph.checkAndSetEdge(node, neighbor, Double.POSITIVE_INFINITY);
         return false;
      }

      double xyDistance = xyDistance(node, neighbor);
      deltaHeight.set(Math.abs(snapHeight.getDoubleValue() - nodeSnapHeight));
      incline.set(Math.atan2(deltaHeight.getValue(), xyDistance));

      if (Math.abs(incline.getValue()) > plannerParameters.getMaxIncline())
      {
         rejectionReason.set(RejectionReason.TOO_STEEP);
         graph.checkAndSetEdge(node, neighbor, Double.POSITIVE_INFINITY);
         return false;
      }

      if (plannerParameters.getCheckForCollisions())
      {
         this.containsCollision.set(collisionDetector.collisionDetected(heightMapData, neighbor, neighborIndex, snapHeight.getDoubleValue(), groundClearance));
         if (containsCollision.getValue())
         {
            rejectionReason.set(RejectionReason.COLLISION);
            graph.checkAndSetEdge(node, neighbor, Double.POSITIVE_INFINITY);
            return false;
         }
      }

      if (useRANSACTraversibility)
      {
         ransacTraversibilityCalculator.computeTraversibility(neighbor, node, neighborIndex);

         if (!ransacTraversibilityCalculator.isTraversible())
         {
            rejectionReason.set(RejectionReason.NON_TRAVERSIBLE);
            graph.checkAndSetEdge(node, neighbor, Double.POSITIVE_INFINITY);
            return false;
         }
      }
      else
      {
         leastSqTraversibilityCalculator.computeTraversibilityIndicator(neighbor, node);

         if (!leastSqTraversibilityCalculator.isTraversible())
         {
            rejectionReason.set(RejectionReason.NON_TRAVERSIBLE);
            graph.checkAndSetEdge(node, neighbor, Double.POSITIVE_INFINITY);
            return false;
         }
      }

      return true;
   }

   private void computeEdgeCost(BodyPathLatticePoint node, BodyPathLatticePoint neighbor)
   {
      edgeCost.set(xyDistance(node, neighbor));

      if (useRANSACTraversibility)
      {
         traversibilityCost.set(ransacTraversibilityCalculator.getTraversability());
         if (ransacTraversibilityCalculator.isTraversible())
            edgeCost.add(traversibilityCost);
      }
      else
      {
         traversibilityCost.set(leastSqTraversibilityCalculator.getTraversibility());
         if (leastSqTraversibilityCalculator.isTraversible())
            edgeCost.add(traversibilityCost);
      }

      if (plannerParameters.getComputeSurfaceNormalCost())
      {
         double yaw = Math.atan2(neighbor.getY() - node.getY(), neighbor.getX() - node.getX());
         Pose2D bodyPose = new Pose2D();

         bodyPose.set(neighbor.getX(), neighbor.getY(), yaw);
         bodyPose.interpolate(new Pose2D(node.getX(), node.getY(), yaw), 0.5);
         computeRollCost(node, neighbor, bodyPose);
      }

      if (incline.getValue() < nominalIncline.getValue())
      {
         inclineCost.set(0.0);
      }
      else
      {
         double inclineDelta = Math.abs(incline.getValue() - nominalIncline.getValue());
         inclineCost.set(plannerParameters.getInclineCostWeight() * Math.max(0.0, inclineDelta - plannerParameters.getInclineCostDeadband()));
         this.edgeCost.add(inclineCost);
      }

      if (edgeCost.getValue() < 0.0)
      {
         throw new RuntimeException("Negative edge cost!");
      }
   }


   private void computeRollCost(BodyPathLatticePoint node, BodyPathLatticePoint neighbor, Pose2D bodyPose)
   {
      UnitVector3DReadOnly surfaceNormal = getSurfaceNormal(HeightMapTools.coordinateToKey(bodyPose.getX(),
                                                                                                                 bodyPose.getY(),
                                                                                                                 heightMapData.getGridCenter().getX(),
                                                                                                                 heightMapData.getGridCenter().getY(),
                                                                                                                 heightMapData.getGridResolutionXY(),
                                                                                                                 heightMapData.getCenterIndex()));

         Vector2D edge = new Vector2D(neighbor.getX() - node.getX(), neighbor.getY() - node.getY());
         edge.normalize();

         /* Roll is the amount of incline orthogonal to the direction of motion */
         leastSqNormal.set(surfaceNormal);
         roll.set(Math.asin(Math.abs(edge.getY() * surfaceNormal.getX() - edge.getX() * surfaceNormal.getY())));
         double effectiveRoll = roll.getDoubleValue();

         if (rollMap.containsKey(node))
         {
            effectiveRoll += rollMap.get(node);
         }

         rollMap.put(neighbor, roll.getValue());
         double inclineScale = EuclidCoreTools.clamp(Math.abs(incline.getValue()) / Math.toRadians(7.0), 0.0, 1.0);
         double rollDeadband = Math.toRadians(1.5);
         double rollAngleDeadbanded = Math.max(0.0, Math.abs(effectiveRoll) - rollDeadband);
         rollCost.set(plannerParameters.getRollCostWeight() * inclineScale * rollAngleDeadbanded);
         edgeCost.add(rollCost);
   }

   private void reportStatus(FootstepPlannerRequest request, FootstepPlannerOutput outputToPack)
   {
      if (debug)
      {
         LogTools.info("Reporting status");
      }

      outputToPack.setBodyPathPlanningResult(result);
      outputToPack.getBodyPath().clear();
      outputToPack.getBodyPathUnsmoothed().clear();

      BodyPathLatticePoint terminalNode = reachedGoal ? goalNode : leastCostNode;
      List<BodyPathLatticePoint> path = graph.getPathFromStart(terminalNode);
      List<Point3D> bodyPath = new ArrayList<>();

      for (int i = 0; i < path.size(); i++)
      {
         Point3D waypoint = new Point3D(path.get(i).getX(), path.get(i).getY(), gridHeightMap.get(path.get(i)));
         bodyPath.add(waypoint);
         outputToPack.getBodyPathUnsmoothed().add(waypoint);

            outputToPack.getBodyPath().add(new Pose3D(waypoint, new Quaternion()));
      }


      outputToPack.getPlannerTimings().setTimePlanningBodyPathSeconds(stopwatch.totalElapsed() - planningStartTime);
      outputToPack.getPlannerTimings().setTotalElapsedSeconds(stopwatch.totalElapsed());

      if (reachedGoal)
      {
         outputToPack.setFootstepPlanningResult(FootstepPlanningResult.PLANNING);
      }

      markSolutionEdges(terminalNode);
      statusCallbacks.forEach(callback -> callback.accept(outputToPack));
   }

   public void clearLoggedData()
   {
      edgeDataMap.clear();
      iterationData.clear();
   }

   private boolean publishStatus(FootstepPlannerRequest request)
   {
      double statusPublishPeriod = request.getStatusPublishPeriod();
      if (statusPublishPeriod <= 0.0)
      {
         return false;
      }

      return stopwatch.lapElapsed() > statusPublishPeriod && !MathTools.epsilonEquals(stopwatch.totalElapsed(), request.getTimeout(), 0.8 * request.getStatusPublishPeriod());
   }

   private void markSolutionEdges(BodyPathLatticePoint terminalNode)
   {
      edgeDataMap.values().forEach(data -> data.setSolutionEdge(false));
      List<BodyPathLatticePoint> path = graph.getPathFromStart(terminalNode);
      for (int i = 1; i < path.size(); i++)
      {
         edgeDataMap.get(new GraphEdge<>(path.get(i - 1), path.get(i))).setSolutionEdge(true);
      }
   }

   public BodyPathLatticePoint getNextNode()
   {
      while (!stack.isEmpty())
      {
//         BodyPathLatticePoint nextNode = stack.pollFirst();
         BodyPathLatticePoint nextNode = stack.poll();
         if (!expandedNodeSet.contains(nextNode))
         {
            return nextNode;
         }
      }

      return null;
   }

   /**
    * Populates a 16-connected grid starting along +x and moving clockwise
    */
   private void populateNeighbors(BodyPathLatticePoint latticePoint)
   {
      neighbors.clear();

      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 1, latticePoint.getYIndex()));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 2, latticePoint.getYIndex() + 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 1, latticePoint.getYIndex() + 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 1, latticePoint.getYIndex() + 2));

      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex(), latticePoint.getYIndex() + 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 1, latticePoint.getYIndex() + 2));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 1, latticePoint.getYIndex() + 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 2, latticePoint.getYIndex() + 1));

      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 1, latticePoint.getYIndex()));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 2, latticePoint.getYIndex() - 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 1, latticePoint.getYIndex() - 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() - 1, latticePoint.getYIndex() - 2));

      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex(), latticePoint.getYIndex() - 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 1, latticePoint.getYIndex() - 2));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 1, latticePoint.getYIndex() - 1));
      neighbors.add(new BodyPathLatticePoint(latticePoint.getXIndex() + 2, latticePoint.getYIndex() - 1));
   }

   private double snap(BodyPathLatticePoint latticePoint)
   {
      if (gridHeightMap.containsKey(latticePoint))
      {
         return gridHeightMap.get(latticePoint);
      }

      int centerIndex = heightMapData.getCenterIndex();
      int xIndex = HeightMapTools.coordinateToIndex(latticePoint.getX(), heightMapData.getGridCenter().getX(), heightMapData.getGridResolutionXY(), centerIndex);
      int yIndex = HeightMapTools.coordinateToIndex(latticePoint.getY(), heightMapData.getGridCenter().getY(), heightMapData.getGridResolutionXY(), centerIndex);

      TDoubleArrayList heights = new TDoubleArrayList();
      for (int i = 0; i < xSnapOffsets.size(); i++)
      {
         int xQuery = xIndex + xSnapOffsets.get(i);
         int yQuery = yIndex + ySnapOffsets.get(i);
         double heightQuery = heightMapData.getHeightAt(xQuery, yQuery);
         if (!Double.isNaN(heightQuery))
         {
            heights.add(heightQuery);
         }
      }

      if (heights.isEmpty())
      {
         gridHeightMap.put(latticePoint, Double.NaN);
         return Double.NaN;
      }

      double maxHeight = heights.max();
      double heightSampleDelta = 0.08;
      double minHeight = maxHeight - heightSampleDelta;

      double runningSum = 0.0;
      int numberOfSamples = 0;

      for (int i = 0; i < xSnapOffsets.size(); i++)
      {
         int xQuery = xIndex + xSnapOffsets.get(i);
         int yQuery = yIndex + ySnapOffsets.get(i);
         double heightQuery = heightMapData.getHeightAt(xQuery, yQuery);
         if (!Double.isNaN(heightQuery) && heightQuery > minHeight)
         {
            runningSum += heightQuery;
            numberOfSamples++;
         }
      }

      gridHeightMap.put(latticePoint, runningSum / numberOfSamples);
      return maxHeight;
   }

   static double xyDistance(BodyPathLatticePoint startNode, BodyPathLatticePoint endNode)
   {
      return EuclidCoreTools.norm(startNode.getX() - endNode.getX(), startNode.getY() - endNode.getY());
   }

   private double heuristics(BodyPathLatticePoint node)
   {
      return xyDistance(node, goalNode);
   }

   public void halt()
   {
      haltRequested.set(true);
   }

   public List<AStarBodyPathIterationData> getIterationData()
   {
      return iterationData;
   }

   public HashMap<GraphEdge<BodyPathLatticePoint>, AStarBodyPathEdgeData> getEdgeDataMap()
   {
      return edgeDataMap;
   }

   public YoRegistry getRegistry()
   {
      return registry;
   }
}
