package us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration;

import javax.vecmath.Point3d;

import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class PointPositionDataObject
{
   protected String bodyFixedReferenceFrameName;
   protected boolean isPointPositionValid = true;
   protected final Point3d measurementPointInBodyFrame = new Point3d();
   protected final Point3d positionOfMeasurementPointInWorldFrame = new Point3d();

   public void set(FramePoint measurementPointInBodyFrame, FramePoint positionOfMeasurementPointInWorldFrame, boolean isPointPositionValid)
   {
      bodyFixedReferenceFrameName = measurementPointInBodyFrame.getReferenceFrame().getName();
      this.isPointPositionValid = isPointPositionValid;
      positionOfMeasurementPointInWorldFrame.checkReferenceFrameMatch(ReferenceFrame.getWorldFrame());

      measurementPointInBodyFrame.getPoint(this.measurementPointInBodyFrame);
      positionOfMeasurementPointInWorldFrame.getPoint(this.positionOfMeasurementPointInWorldFrame);
   }

   public Point3d getMeasurementPointInWorldFrame()
   {
      return positionOfMeasurementPointInWorldFrame;
   }

   public Point3d getMeasurementPointInBodyFrame()
   {
      return measurementPointInBodyFrame;
   }

   public void set(PointPositionDataObject other)
   {  
      bodyFixedReferenceFrameName = other.bodyFixedReferenceFrameName;
      isPointPositionValid = other.isPointPositionValid;
      measurementPointInBodyFrame.set(other.measurementPointInBodyFrame);
      positionOfMeasurementPointInWorldFrame.set(other.positionOfMeasurementPointInWorldFrame);
   }

   public boolean epsilonEquals(PointPositionDataObject other, double epsilon)
   {
      if (bodyFixedReferenceFrameName != other.bodyFixedReferenceFrameName)
         return false;

      boolean validStateEqual = isPointPositionValid == other.isPointPositionValid;
      boolean bodyPointsEqual = getMeasurementPointInBodyFrame().epsilonEquals(other.getMeasurementPointInBodyFrame(), epsilon);
      boolean worldPointsEqual = getMeasurementPointInWorldFrame().epsilonEquals(other.getMeasurementPointInWorldFrame(), epsilon);
      return validStateEqual && bodyPointsEqual && worldPointsEqual;
   }

   public String getBodyFixedReferenceFrameName()
   {
      return bodyFixedReferenceFrameName;
   }

   public boolean isPointPositionValid()
   {
      return isPointPositionValid;
   }

   public void invalidatePointPosition()
   {
      isPointPositionValid = false;
   }
}
