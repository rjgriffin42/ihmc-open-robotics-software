package us.ihmc.sensorProcessing.pointClouds.combinationQuadTreeOctTree;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import us.ihmc.utilities.dataStructures.hyperCubeTree.HyperCubeLeaf;
import us.ihmc.utilities.dataStructures.hyperCubeTree.HyperCubeTree;
import us.ihmc.utilities.dataStructures.hyperCubeTree.LineSegmentSearchVolume;
import us.ihmc.utilities.dataStructures.hyperCubeTree.Octree;
import us.ihmc.utilities.dataStructures.hyperCubeTree.OneDimensionalBounds;
import us.ihmc.utilities.dataStructures.hyperCubeTree.RecursableHyperTreeNode;
import us.ihmc.utilities.math.dataStructures.HeightMap;
import us.ihmc.utilities.math.geometry.InclusionFunction;
import us.ihmc.utilities.math.geometry.PointToLineUnProjector;
import us.ihmc.utilities.test.LowPassTimingReporter;

public class GroundOnlyQuadTree extends HyperCubeTree<GroundAirDescriptor, GroundOnlyQuadTreeData> implements HeightMap
{
   private final double constantResolution;
   private final double heightThreshold;
   private int numberOfNodes = 0;
   private int maxNodes=1000;
   private LowPassTimingReporter clearingTimer = new LowPassTimingReporter(7);

   public GroundOnlyQuadTree(double minX, double minY, double maxX, double maxY, double resolution, double heightThreshold, int maxNodes)
   {
      this(toBounds(minX, maxX, minY, maxY), resolution, heightThreshold, maxNodes);

   }

   private GroundOnlyQuadTree(OneDimensionalBounds[] bounds, double resolution, double heightThreshold, int maxNodes)
   {
      super(bounds);
      this.constantResolution = resolution;
      this.heightThreshold = heightThreshold;
      this.getRootNode().setMetaData(new GroundOnlyQuadTreeData(false));
      this.maxNodes = maxNodes;
      numberOfNodes = 1;
//      clearingTimer.setupRecording("GroundOnlyQuadTree", "perform the lidar beam search", 10000L, 20000L);
   }


   public double heightAtPoint(double x, double y)
   {
      HyperCubeLeaf<GroundAirDescriptor> hyperCubeLeaf = this.get(new double[] {x, y});
      if (hyperCubeLeaf == null)
         return Double.NaN;
      if (hyperCubeLeaf.getValue() == null)
         return Double.NaN;
      if (hyperCubeLeaf.getValue().getHeight() == null)
         return Double.NaN;

      return hyperCubeLeaf.getValue().getHeight();
   }

   public boolean addPoint(double x, double y, double z)
   {
      return this.put(new double[] {x, y}, (float)z);
   }
   private PointToLineUnProjector unProjector = new PointToLineUnProjector();
   public void addLidarRay(Point3d origin, Point3d end)
   {
      clearingTimer.startTime();
      Point2d point1 = new Point2d(origin.getX(),origin.getY());
      Point2d point2 = new Point2d(end.getX(),end.getY());
      LineSegmentSearchVolume lineSegment = new LineSegmentSearchVolume(point1, point2);
      List<RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData>> hyperVolumeIntersection = this.getHyperVolumeIntersection(lineSegment);
      unProjector.setLine(point1, point2, origin.getZ(), end.getZ());
      for (int i=0;i<hyperVolumeIntersection.size();i++)
      {
//         if (null==hyperVolumeIntersection.get(i).getLeaf())
//            hyperVolumeIntersection.get(i).setLeaf(new HyperCubeLeaf<GroundAirDescriptor>(new GroundAirDescriptor(height, minClearHeight), location))
         GroundAirDescriptor leafValue = hyperVolumeIntersection.get(i).getLeaf().getValue();
         Float minClearHeight = leafValue.getMinClearHeight();
         double[] intersection = lineSegment.intersectionWithBounds(hyperVolumeIntersection.get(i).getBoundsCopy());
         float newMinClearHeight = (float) unProjector.unProject(intersection[0], intersection[1]);
         if (null==minClearHeight)
            leafValue.setMinClearHeight(newMinClearHeight);
         if (newMinClearHeight<minClearHeight)
            leafValue.setMinClearHeight(newMinClearHeight);
         hyperVolumeIntersection.get(i).getMetaData();
         
      }
      clearingTimer.endTime();
   }

   public boolean containsPoint(double x, double y)
   {
      HyperCubeLeaf<GroundAirDescriptor> hyperCubeLeaf = this.get(new double[] {x, y});
      if (hyperCubeLeaf == null)
         return false;
      if (hyperCubeLeaf.getValue() == null)
         return false;

      return true;
   }

   public void clear()
   {
      this.clearTree();
   }

   public boolean put(double[] location, Float value)
   {
      checkDimensionality(location);
      HyperCubeLeaf<GroundAirDescriptor> leaf = new HyperCubeLeaf<GroundAirDescriptor>(new GroundAirDescriptor(value,null), location);

      return this.putRecursively(this.getRootNode(), leaf);
   }

   private void puntLeaf(HyperCubeLeaf<GroundAirDescriptor> leafToPunt)
   {
      if (null==octree)
         return;
      double[] location = new double[]{leafToPunt.getLocation()[0],leafToPunt.getLocation()[1],leafToPunt.getValue().getHeight()};
      octree.upRezz(location);
      octree.put(location, true);
   }

   public void mergeOneLevel(RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> node)
   {
      if (!node.hasChildren())
         return;
      HyperCubeLeaf<GroundAirDescriptor> firstLeaf = null;
      boolean stuffAboveToMatch = node.getChild(0).getMetaData().getIsStuffAboveMe();
      for (int i = 0; i < node.getChildNumber(); i++)
      {
         RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> lowerLevelNode = node.getChild(i);
         if (lowerLevelNode.hasChildren())
            return;
         boolean childHasIncompatibleMetaData = stuffAboveToMatch != lowerLevelNode.getMetaData().getIsStuffAboveMe();
         if (childHasIncompatibleMetaData)
            return;
         if (null == lowerLevelNode.getLeaf())
            continue;

         if (null == firstLeaf)
         {
            firstLeaf = lowerLevelNode.getLeaf();

            continue;
         }

         if (!canMergeLeaves(firstLeaf, lowerLevelNode.getLeaf()))
            return;
      }

      if (null == firstLeaf)
         return;
      node.clear();
      node.setLeaf(firstLeaf);
      node.setMetaData(new GroundOnlyQuadTreeData(stuffAboveToMatch));
   }

   private boolean putRecursively(RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> node, final HyperCubeLeaf<GroundAirDescriptor> leaf)
   {
      if (node.hasChildren())
      {
         boolean treeChanged = putRecursively(node.getChildAtLocation(leaf.getLocation()), leaf);
         if (treeChanged)
            this.mergeOneLevel(node);

         return treeChanged;
      }

      if (node.getLeaf() == null)
      {
         replaceLeaf(node, leaf);

         return true;
      }

      boolean newLeafIsSignificantyAboveOldLeaf = (leaf.getValue().getHeight() - node.getLeaf().getValue().getHeight()) > heightThreshold;
      boolean newLeafIsSignificantyBelowOldLeaf = (leaf.getValue().getHeight() - node.getLeaf().getValue().getHeight()) < heightThreshold;

      if (node.getMetaData().getIsStuffAboveMe() && newLeafIsSignificantyAboveOldLeaf)
      {
         puntLeaf(leaf);

         return false;
      }

      if (this.canMergeLeaves(node.getLeaf(), leaf))
      {
         return false;
      }

      if (canSplit(node))
      {
         HyperCubeLeaf<GroundAirDescriptor> oldLeaf = node.getLeaf();

         node.setLeaf(null);
         node.split();

         node.getChildAtLocation(oldLeaf.getLocation()).setLeaf(oldLeaf);

//       replaceLeaf(node.getChildAtLocation(leaf.getLocation()), leaf); Only in a slowly increasing resolution quadtree, incompatible with height map.

         setAllChildrenStuffAbove(node, node.getMetaData().getIsStuffAboveMe());
         if (node.getMetaData().getIsStuffAboveMe())
            setDefaultLeafInAllEmptyChildren(node, oldLeaf.getValue());

         putRecursively(node, leaf);

         return true;
      }

      if (newLeafIsSignificantyAboveOldLeaf)
      {
         puntLeaf(leaf);
         node.setMetaData(new GroundOnlyQuadTreeData(true));

         return true;    // true because metadata changed
      }

      if (newLeafIsSignificantyBelowOldLeaf)
      {
         puntLeaf(node.getLeaf());
         node.setLeaf(leaf);
         node.setMetaData(new GroundOnlyQuadTreeData(true));

         return true;
      }

      replaceLeaf(node, leaf);

      return true;
   }

   public void setDefaultLeafInAllEmptyChildren(RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> node, GroundAirDescriptor value)
   {
      for (int i = 0; i < node.getChildNumber(); i++)
      {
         RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> child = node.getChild(i);
         if (child.getLeaf() == null)
         {
            double[] childMiddle = new double[node.getDimensionality()];
            for (int j = 0; j < node.getDimensionality(); j++)
            {
               childMiddle[j] = child.getBounds(j).midpoint();
            }

            child.setLeaf(new HyperCubeLeaf<GroundAirDescriptor>(value, childMiddle));
         }
      }
   }

   public void setAllChildrenStuffAbove(RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> node, boolean hasStuffAbove)
   {
      for (int i = 0; i < node.getChildNumber(); i++)
      {
         node.getChild(i).setMetaData(new GroundOnlyQuadTreeData(hasStuffAbove));
      }
   }

   public List<Point3d> getAllPointsWithinArea(double xCenter, double yCenter, double xExtent, double yExtent)
   {
      return getAllPointsUsingGrid(xCenter, yCenter, xExtent, yExtent, constantResolution);
   }

   public List<Point3d> getAllPointsUsingGrid(double centerX, double centerY, double extentX, double extentY, double resolution)
   {
      ArrayList<Point3d> points = new ArrayList<Point3d>();

      for (double x = centerX - extentX * 0.5; x <= centerX + extentX * 0.5; x += resolution)
      {
         for (double y = centerY - extentY * 0.5; y <= centerY + extentY * 0.5; y += resolution)
         {
            Float f = get(x, y);
            if (f != null)
               points.add(new Point3d(x, y, f));

         }
      }

      return points;
   }


   public List<Point3d> getAllPointsWithinArea(double xCenter, double yCenter, double xExtent, double yExtent,
           InclusionFunction<Point3d> maskFunctionAboutCenter)
   {
      ArrayList<Point3d> points = new ArrayList<Point3d>();

      for (double x = xCenter - xExtent * 0.5; x <= xCenter + xExtent * 0.5; x += constantResolution)
      {
         for (double y = yCenter - yExtent * 0.5; y <= yCenter + yExtent * 0.5; y += constantResolution)
         {
            Float f = get(x, y);
            if (f != null)
            {
               Point3d point3d = new Point3d(x, y, f);
               if (maskFunctionAboutCenter.isIncluded(point3d))
                  points.add(point3d);
            }

         }
      }

      return points;
   }

   protected boolean canSplit(RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> node)
   {
      if (this.numberOfNodes>=this.maxNodes)
         return false;
      for (int i = 0; i < node.getDimensionality(); i++)
      {
         if (node.getBounds(i).size() <= constantResolution)
            return false;
      }

      return true;
   }

   private void replaceLeaf(RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> node, HyperCubeLeaf<GroundAirDescriptor> leaf)
   {
      HyperCubeLeaf<GroundAirDescriptor> oldLeaf = node.getLeaf();
      node.setLeaf(mergeLeaves(oldLeaf, leaf));
   }

   protected HyperCubeLeaf<GroundAirDescriptor> mergeLeaves(HyperCubeLeaf<GroundAirDescriptor> oldLeaf, HyperCubeLeaf<GroundAirDescriptor> newLeaf)
   {
      // PclTODO: this needs work.
      return newLeaf;
   }

   @Override
   protected boolean canMergeLeaves(HyperCubeLeaf<GroundAirDescriptor> firstLeaf, HyperCubeLeaf<GroundAirDescriptor> secondLeaf)
   {
      float diff = (firstLeaf.getValue().getHeight() - secondLeaf.getValue().getHeight());

      return (diff < heightThreshold) && (diff > -heightThreshold);
   }

   public Float get(double xToTest, double yToTest)
   {
      HyperCubeLeaf<GroundAirDescriptor> resultLeaf = this.get(toLocation(xToTest, yToTest));
      if (null == resultLeaf)
         return null;

      return resultLeaf.getValue().getHeight();
   }

   public void put(float x, float y, float value)
   {
      this.put(toLocation(x, y), value);
   }

   public void nodeAdded(String id, OneDimensionalBounds[] bounds, HyperCubeLeaf<GroundAirDescriptor> leaf)
   {
      super.nodeAdded(id, bounds, leaf);
      this.numberOfNodes++;
   }
   public void nodeRemoved(String id)
   {
      super.nodeRemoved(id);
      this.numberOfNodes--;
   }
   public RecursableHyperTreeNode<GroundAirDescriptor, GroundOnlyQuadTreeData> getLeafNodeAtLocation(float xToTest, float yToTest)
   {
      return getLeafNodeAtLocation(toLocation(xToTest, yToTest));
   }

   protected static double[] toLocation(double x, double y)
   {
      return new double[] {x, y};
   }

   protected static OneDimensionalBounds[] toBounds(double xMin, double xMax, double yMin, double yMax)
   {
      return new OneDimensionalBounds[] {new OneDimensionalBounds(xMin, xMax), new OneDimensionalBounds(yMin, yMax)};
   }

   public double getMaxY()
   {
      return this.getRootNode().getBounds(1).max();
   }

   public double getMinY()
   {
      return this.getRootNode().getBounds(1).min();
   }

   public double getMaxX()
   {
      return this.getRootNode().getBounds(0).max();
   }

   public double getMinX()
   {
      return this.getRootNode().getBounds(0).min();
   }

   public void remove(double x, double y)
   {
      this.remove(this.get(toLocation(x, y)));
   }
   private Octree octree;
   public void setOctree(Octree octree)
   {
      this.octree=octree;
   }

   public void treeCleared()
   {
      // TODO Auto-generated method stub
      
   }
   
   

}
