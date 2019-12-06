package us.ihmc.pathPlanning.visibilityGraphs.dataStructure;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;

/**
 * Visibility graph node data structure associated with a navigable region,
 * holding cost information, and connected graph edges
 */
public class VisibilityGraphNode implements EpsilonComparable<VisibilityGraphNode>
{
   private final VisibilityGraphNavigableRegion visibilityGraphNavigableRegion;
   private final ConnectionPoint3D pointInWorld;
   private final Point2D point2DInLocal;

   private boolean edgesHaveBeenDetermined = false;

   private double costFromStart = Double.NaN;
   private double estimatedCostToGoal = Double.NaN;

   private boolean hasBeenExpanded = false;
   private VisibilityGraphNode bestParentNode = null;

   private final boolean isPreferredNode;

   private final ArrayList<VisibilityGraphEdge> edges = new ArrayList<>();

   public VisibilityGraphNode(Point3DReadOnly pointInWorld, Point2DReadOnly pointInLocal, VisibilityGraphNavigableRegion visibilityGraphNavigableRegion,
                              boolean isPreferredNode)
   {
      this(pointInWorld, pointInLocal, visibilityGraphNavigableRegion, visibilityGraphNavigableRegion.getMapId(), isPreferredNode);
   }

   public VisibilityGraphNode(Point3DReadOnly pointInWorld, Point2DReadOnly pointInLocal, VisibilityGraphNavigableRegion visibilityGraphNavigableRegion,
                              int mapId, boolean isPreferredNode)
   {
      this.visibilityGraphNavigableRegion = visibilityGraphNavigableRegion;
      this.pointInWorld = new ConnectionPoint3D(pointInWorld, mapId);
      this.point2DInLocal = new Point2D(pointInLocal);
      this.isPreferredNode = isPreferredNode;
   }

   public int getRegionId()
   {
      return pointInWorld.getRegionId();
   }

   public VisibilityGraphNavigableRegion getVisibilityGraphNavigableRegion()
   {
      return visibilityGraphNavigableRegion;
   }

   public ConnectionPoint3D getPointInWorld()
   {
      return pointInWorld;
   }

   public Point2DReadOnly getPoint2DInLocal()
   {
      return point2DInLocal;
   }

   public synchronized void addEdge(VisibilityGraphEdge edge)
   {
      if (edge != null)
      {
         edges.add(edge);
      }
   }

   public List<VisibilityGraphEdge> getEdges()
   {
      return edges;
   }

   public double distance(VisibilityGraphNode target)
   {
      return pointInWorld.distance(target.pointInWorld);
   }

   public double distanceXY(VisibilityGraphNode target)
   {
      return pointInWorld.distanceXY(target.pointInWorld);
   }

   public double getCostFromStart()
   {
      return costFromStart;
   }

   public void setCostFromStart(double costFromStart, VisibilityGraphNode bestParentNode)
   {
      this.costFromStart = costFromStart;
      this.bestParentNode = bestParentNode;
   }

   public VisibilityGraphNode getBestParentNode()
   {
      return bestParentNode;
   }

   public double getEstimatedCostToGoal()
   {
      return estimatedCostToGoal;
   }

   public void setEstimatedCostToGoal(double estimatedCostToGoal)
   {
      this.estimatedCostToGoal = estimatedCostToGoal;
   }

   public boolean isPreferredNode()
   {
      return isPreferredNode;
   }

   public boolean getHasBeenExpanded()
   {
      return hasBeenExpanded;
   }

   public void setHasBeenExpanded(boolean hasBeenExpanded)
   {
      this.hasBeenExpanded = hasBeenExpanded;
   }

   public boolean getEdgesHaveBeenDetermined()
   {
      return edgesHaveBeenDetermined;
   }

   public void setEdgesHaveBeenDetermined(boolean edgesHaveBeenDetermined)
   {
      this.edgesHaveBeenDetermined = edgesHaveBeenDetermined;
   }

   @Override
   public boolean epsilonEquals(VisibilityGraphNode other, double epsilon)
   {
      return pointInWorld.epsilonEquals(other.pointInWorld, epsilon);
   }

   @Override
   public String toString()
   {
      return pointInWorld.toString();
   }

}
