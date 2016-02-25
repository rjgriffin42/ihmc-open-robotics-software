package us.ihmc.robotics.math.trajectories.waypoints;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class FrameSO3Waypoint extends FrameWaypoint<FrameSO3Waypoint> implements SO3WaypointInterface<FrameSO3Waypoint>
{
   private ReferenceFrame referenceFrame = ReferenceFrame.getWorldFrame();

   private double time;
   private final FrameOrientation orientation = new FrameOrientation();
   private final FrameVector angularVelocity = new FrameVector();

   public FrameSO3Waypoint()
   {
      setToZero(ReferenceFrame.getWorldFrame());
   }

   public FrameSO3Waypoint(ReferenceFrame referenceFrame)
   {
      setToZero(referenceFrame);
   }

   public FrameSO3Waypoint(double time, FrameOrientation orientation, FrameVector angularVelocity)
   {
      setIncludingFrame(time, orientation, angularVelocity);
   }

   public FrameSO3Waypoint(ReferenceFrame referenceFrame, SO3WaypointInterface<?> so3WaypointInterface)
   {
      setIncludingFrame(referenceFrame, so3WaypointInterface);
   }

   public FrameSO3Waypoint(FrameSO3Waypoint frameSO3Waypoint)
   {
      setIncludingFrame(frameSO3Waypoint);
   }

   @Override
   public void setTime(double time)
   {
      this.time = time;
   }

   public void setOrientation(Quat4d orientation)
   {
      this.orientation.set(orientation);
   }

   public void setOrientation(FrameOrientation orientation)
   {
      this.orientation.set(orientation);
   }

   public void setAngularVelocity(Vector3d angularVelocity)
   {
      this.angularVelocity.set(angularVelocity);
   }

   public void setAngularVelocity(FrameVector angularVelocity)
   {
      this.angularVelocity.set(angularVelocity);
   }

   public void set(double time, Quat4d orientation, Vector3d angularVelocity)
   {
      // Ensuring frame consistency without crashing
      setIncludingFrame(referenceFrame, time, orientation, angularVelocity);
   }

   public void set(double time, FrameOrientation orientation, FrameVector angularVelocity)
   {
      // Ensuring frame consistency without crashing
      setToZero(referenceFrame);
      this.time = time;
      this.orientation.set(orientation);
      this.angularVelocity.set(angularVelocity);
   }

   public void set(SO3WaypointInterface<?> so3Waypoint)
   {
      // Ensuring frame consistency without crashing
      setIncludingFrame(referenceFrame, so3Waypoint);
   }

   @Override
   public void set(FrameSO3Waypoint frameSO3Waypoint)
   {
      checkReferenceFrameMatch(frameSO3Waypoint);
      frameSO3Waypoint.checkFrameConsistency();

      // Ensuring frame consistency without crashing
      setToZero(referenceFrame);

      time = frameSO3Waypoint.time;
      orientation.set(frameSO3Waypoint.orientation);
      angularVelocity.set(frameSO3Waypoint.angularVelocity);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, double time, Quat4d orientation, Vector3d angularVelocity)
   {
      setToZero(referenceFrame);

      this.time = time;
      this.orientation.set(orientation);
      this.angularVelocity.set(angularVelocity);
   }

   public void setIncludingFrame(double time, FrameOrientation orientation, FrameVector angularVelocity)
   {
      orientation.checkReferenceFrameMatch(angularVelocity);
      referenceFrame = orientation.getReferenceFrame();

      this.time = time;
      this.orientation.setIncludingFrame(orientation);
      this.angularVelocity.setIncludingFrame(angularVelocity);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, SO3WaypointInterface<?> so3Waypoint)
   {
      setToZero(referenceFrame);

      time = so3Waypoint.getTime();
      so3Waypoint.getOrientation(orientation.getQuaternion());
      so3Waypoint.getAngularVelocity(angularVelocity.getVector());
   }

   @Override
   public void setIncludingFrame(FrameSO3Waypoint frameSO3Waypoint)
   {
      frameSO3Waypoint.checkFrameConsistency();

      referenceFrame = frameSO3Waypoint.referenceFrame;
      time = frameSO3Waypoint.time;
      orientation.setIncludingFrame(frameSO3Waypoint.orientation);
      angularVelocity.setIncludingFrame(frameSO3Waypoint.angularVelocity);
   }

   public void setTimeToZero()
   {
      time = 0.0;
   }

   public void setOrientationToZero()
   {
      orientation.setToZero();
   }

   public void setAngularVelocityToZero()
   {
      angularVelocity.setToZero();
   }

   @Override
   public void setToZero()
   {
      time = 0.0;
      orientation.setToZero();
      angularVelocity.setToZero();
   }

   @Override
   public void setToZero(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      time = 0.0;
      orientation.setToZero(referenceFrame);
      angularVelocity.setToZero(referenceFrame);
   }

   public void setTimeToNaN()
   {
      time = Double.NaN;
   }

   public void setOrientationToNaN()
   {
      orientation.setToNaN();
   }

   public void setAngularVelocityToNaN()
   {
      angularVelocity.setToNaN();
   }

   @Override
   public void setToNaN()
   {
      time = Double.NaN;
      orientation.setToNaN();
      angularVelocity.setToNaN();
   }

   @Override
   public void setToNaN(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      time = Double.NaN;
      orientation.setToNaN(referenceFrame);
      angularVelocity.setToNaN(referenceFrame);
   }

   @Override
   public void addTimeOffset(double timeOffsetToAdd)
   {
      time += timeOffsetToAdd;
   }

   @Override
   public void subtractTimeOffset(double timeOffsetToSubtract)
   {
      time -= timeOffsetToSubtract;
   }

   public void checkFrameConsistency()
   {
      if (referenceFrame != orientation.getReferenceFrame())
         throwFrameInconsistencyException();
      if (referenceFrame != angularVelocity.getReferenceFrame())
         throwFrameInconsistencyException();
   }

   @Override
   public boolean containsNaN()
   {
      return Double.isNaN(time) || orientation.containsNaN() || angularVelocity.containsNaN();
   }

   @Override
   public double getTime()
   {
      return time;
   }

   @Override
   public void getOrientation(Quat4d orientationToPack)
   {
      orientation.getQuaternion(orientationToPack);
   }

   @Override
   public void getAngularVelocity(Vector3d angularVelocityToPack)
   {
      angularVelocity.get(angularVelocityToPack);
   }

   public void getOrientation(FrameOrientation orientationToPack)
   {
      orientationToPack.set(orientation);
   }

   public void getAngularVelocity(FrameVector angularVelocityToPack)
   {
      angularVelocityToPack.set(angularVelocity);
   }

   public void getOrientationIncludingFrame(FrameOrientation orientationToPack)
   {
      orientationToPack.setIncludingFrame(orientation);
   }

   public void getAngularVelocityIncludingFrame(FrameVector angularVelocityToPack)
   {
      angularVelocityToPack.setIncludingFrame(angularVelocity);
   }

   /**
    * Return the original orientation held by this waypoint.
    */
   public FrameOrientation getOrientation()
   {
      return orientation;
   }

   /**
    * Return the original angularVelocity held by this waypoint.
    */
   public FrameVector getAngularVelocity()
   {
      return angularVelocity;
   }

   @Override
   public void changeFrame(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      orientation.changeFrame(referenceFrame);
      angularVelocity.changeFrame(referenceFrame);
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   @Override
   public boolean epsilonEquals(FrameSO3Waypoint other, double epsilon)
   {
      if (!MathTools.epsilonEquals(time, other.time, epsilon))
         return false;
      if (!orientation.epsilonEquals(other.orientation, epsilon))
         return false;
      if (!angularVelocity.epsilonEquals(other.angularVelocity, epsilon))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      NumberFormat doubleFormat = new DecimalFormat(" 0.00;-0.00");
      String qxToString = doubleFormat.format(orientation.getQx());
      String qyToString = doubleFormat.format(orientation.getQy());
      String qzToString = doubleFormat.format(orientation.getQz());
      String qsToString = doubleFormat.format(orientation.getQs());
      String wxToString = doubleFormat.format(angularVelocity.getX());
      String wyToString = doubleFormat.format(angularVelocity.getY());
      String wzToString = doubleFormat.format(angularVelocity.getZ());

      String timeToString = "time = " + doubleFormat.format(time);
      String orientationToString = "orientation = (" + qxToString + ", " + qyToString + ", " + qzToString + ", " + qsToString + ")";
      String angularVelocityToString = "angular velocity = (" + wxToString + ", " + wyToString + ", " + wzToString + ")";
      String referenceFrameToString = "reference frame = " + referenceFrame.getName();

      return "SO3 waypoint: (" + timeToString + ", " + orientationToString + ", " + angularVelocityToString + ", " + referenceFrameToString + ")";
   }
}
