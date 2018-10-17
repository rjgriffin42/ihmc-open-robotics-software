package us.ihmc.pathPlanning.bodyPathPlanner;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;

public class WaypointDefinedBodyPathPlanner implements BodyPathPlanner
{
   private List<Point3DReadOnly> waypoints;
   private double[] maxAlphas;
   private double[] segmentLengths;
   private double[] segmentHeadings;
   private final BodyPathPlan bodyPathPlan = new BodyPathPlan();

   @Override
   public void setWaypoints(List<? extends Point3DReadOnly> waypoints)
   {
      if (waypoints.size() < 2)
      {
         throw new RuntimeException("Must have at least two waypoints!");
      }
      this.waypoints = new ArrayList<>();
      this.waypoints.addAll(waypoints);
      this.maxAlphas = new double[waypoints.size() - 1];
      this.segmentLengths = new double[waypoints.size() - 1];
      this.segmentHeadings = new double[waypoints.size() - 1];
   }

   @Override
   public BodyPathPlan compute()
   {
      double totalPathLength = 0.0;

      for (int i = 0; i < segmentLengths.length; i++)
      {
         Point3DReadOnly segmentStart = waypoints.get(i);
         Point3DReadOnly segmentEnd = waypoints.get(i + 1);
         segmentLengths[i] = segmentEnd.distanceXY(segmentStart);
         totalPathLength = totalPathLength + segmentLengths[i];


         segmentHeadings[i] = calculateHeading(segmentStart, segmentEnd);
      }

      for (int i = 0; i < segmentLengths.length; i++)
      {
         double previousMaxAlpha = (i == 0) ? 0.0 : maxAlphas[i - 1];
         maxAlphas[i] = previousMaxAlpha + segmentLengths[i] / totalPathLength;
      }

      int startIndex = 0;
      int endIndex = waypoints.size() - 1;
      bodyPathPlan.clear();
      bodyPathPlan.setStartPose(waypoints.get(startIndex), segmentHeadings[startIndex]);
      bodyPathPlan.setGoalPose(waypoints.get(endIndex), segmentHeadings[endIndex - 1]);
      bodyPathPlan.addWaypoints(waypoints);

      return bodyPathPlan;
   }

   @Override
   public BodyPathPlan getPlan()
   {
      return bodyPathPlan;
   }

   private static double calculateHeading(Point3DReadOnly startPose, Point3DReadOnly endPoint)
   {
      double deltaX = endPoint.getX() - startPose.getX();
      double deltaY = endPoint.getY() - startPose.getY();
      double heading;

      double pathHeading = Math.atan2(deltaY, deltaX);
      heading = AngleTools.trimAngleMinusPiToPi(pathHeading);

      return heading;

   }

   @Override
   public void getPointAlongPath(double alpha, Pose2D poseToPack)
   {
      int segmentIndex = getRegionIndexFromAlpha(alpha);
      Point3DReadOnly firstPoint = waypoints.get(segmentIndex);
      Point3DReadOnly secondPoint = waypoints.get(segmentIndex + 1);

      double alphaInSegment = getPercentInSegment(segmentIndex, alpha);

      Point3D pointToPack = new Point3D();
      pointToPack.interpolate(firstPoint, secondPoint, alphaInSegment);
      poseToPack.setPosition(pointToPack);
      poseToPack.setYaw(segmentHeadings[segmentIndex]);
   }

   @Override
   public double getClosestPoint(Point2D point, Pose2D poseToPack)
   {
      double closestPointDistance = Double.POSITIVE_INFINITY;
      double alpha = Double.NaN;
      Point3D tempClosestPoint = new Point3D();
      Point3D point3D = new Point3D(point);

      for (int i = 0; i < segmentLengths.length; i++)
      {
         Point3DReadOnly segmentStart = waypoints.get(i);
         Point3DReadOnly segmentEnd = waypoints.get(i + 1);
         EuclidGeometryTools.orthogonalProjectionOnLineSegment3D(point3D, segmentStart, segmentEnd, tempClosestPoint);

         double distance = tempClosestPoint.distanceXY(point3D);
         if (distance < closestPointDistance)
         {
            double distanceToSegmentStart = tempClosestPoint.distanceXY(segmentStart);
            double alphaInSegment = distanceToSegmentStart / segmentLengths[i];

            boolean firstSegment = i == 0;
            double alphaSegmentStart = firstSegment ? 0.0 : maxAlphas[i - 1];
            double alphaSegmentEnd = maxAlphas[i];
            alpha = alphaSegmentStart + alphaInSegment * (alphaSegmentEnd - alphaSegmentStart);

            closestPointDistance = distance;
         }
      }

      getPointAlongPath(alpha, poseToPack);
      return alpha;
   }

   @Override
   public double computePathLength(double alpha)
   {
      int segmentIndex = getRegionIndexFromAlpha(alpha);
      double alphaInSegment = getPercentInSegment(segmentIndex, alpha);

      double segmentLength = (1.0 - alphaInSegment) * segmentLengths[segmentIndex];
      for (int i = segmentIndex + 1; i < segmentLengths.length; i++)
      {
         segmentLength = segmentLength + segmentLengths[i];
      }

      return segmentLength;
   }

   private double getPercentInSegment(int segment, double alpha)
   {
      boolean firstSegment = segment == 0;
      double alphaSegmentStart = firstSegment ? 0.0 : maxAlphas[segment - 1];
      double alphaSegmentEnd = maxAlphas[segment];
      return (alpha - alphaSegmentStart) / (alphaSegmentEnd - alphaSegmentStart);
   }

   private int getRegionIndexFromAlpha(double alpha)
   {
      if (alpha > maxAlphas[maxAlphas.length - 1])
      {
         return maxAlphas.length - 1;
      }

      for (int i = 0; i < maxAlphas.length; i++)
      {
         if (maxAlphas[i] >= alpha)
         {
            return i;
         }
      }

      throw new RuntimeException("Alpha = " + alpha + "\nalpha must be between [0,1] and maxAlphas highest value must be 1.0.");
   }
}
