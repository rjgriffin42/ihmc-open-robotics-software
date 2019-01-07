package us.ihmc.quadrupedPlanning.pathPlanning;

import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.robotics.math.trajectories.YoPolynomial;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class SplinePathPlanner extends AbstractWaypointsForFootstepsPlanner
{
   private static final int numberOfPoints = 5;

   private final YoPolynomial xPoly;
   private final YoPolynomial yPoly;
   private final YoPolynomial zPoly;

   public SplinePathPlanner(YoVariableRegistry parentRegistry)
   {
      xPoly = new YoPolynomial("xPoly", 4, parentRegistry);
      yPoly = new YoPolynomial("yPoly", 4, parentRegistry);
      zPoly = new YoPolynomial("zPoly", 4, parentRegistry);
   }

   public void planWaypoints()
   {
      waypoints.clear();
      double yaw = bodyStartPose.getYaw();
      xPoly.setQuadratic(0.0, 1.0, bodyStartPose.getX(), Math.cos(yaw) * 0.2, bodyGoalPose.getX());
      yPoly.setQuadratic(0.0, 1.0, bodyStartPose.getY(), Math.sin(yaw) * 0.2, bodyGoalPose.getY());
      zPoly.setQuadratic(0.0, 1.0, bodyStartPose.getZ(), Math.sin(yaw) * 0.2, bodyGoalPose.getZ());
      for (int i = 0; i < numberOfPoints; i++)
      {
         double percent = i / (double) (numberOfPoints - 1);
         xPoly.compute(percent);
         yPoly.compute(percent);
         zPoly.compute(percent);
         Point3D point = new Point3D(xPoly.getPosition(), yPoly.getPosition(), zPoly.getPosition());
         waypoints.add(point);
      }

   }
}
