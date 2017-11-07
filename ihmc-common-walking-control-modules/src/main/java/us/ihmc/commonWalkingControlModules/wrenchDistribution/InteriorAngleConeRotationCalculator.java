package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;

public class InteriorAngleConeRotationCalculator implements FrictionConeRotationCalculator
{

   @Override
   public double computeConeRotation(YoPlaneContactState yoPlaneContactState, int contactPointIndex)
   {
      double angleOfEdge = yoPlaneContactState.getAngleOfEdgeAfterPoint(contactPointIndex);
      double interiorAngle = yoPlaneContactState.getInteriorAngle(contactPointIndex);
      return angleOfEdge - 0.5 * interiorAngle;
   }

}
