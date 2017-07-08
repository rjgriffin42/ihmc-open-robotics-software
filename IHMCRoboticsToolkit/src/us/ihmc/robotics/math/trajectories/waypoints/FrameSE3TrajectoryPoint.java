package us.ihmc.robotics.math.trajectories.waypoints;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionBasics;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.geometry.frameObjects.FrameSE3Waypoint;
import us.ihmc.robotics.geometry.interfaces.SE3WaypointInterface;
import us.ihmc.robotics.geometry.transformables.EuclideanWaypoint;
import us.ihmc.robotics.geometry.transformables.SE3Waypoint;
import us.ihmc.robotics.geometry.transformables.SO3Waypoint;
import us.ihmc.robotics.math.trajectories.waypoints.interfaces.SE3TrajectoryPointInterface;

public class FrameSE3TrajectoryPoint extends FrameTrajectoryPoint<FrameSE3TrajectoryPoint, SimpleSE3TrajectoryPoint>
      implements SE3TrajectoryPointInterface<FrameSE3TrajectoryPoint>
{
   private final SimpleSE3TrajectoryPoint geometryObject;

   public FrameSE3TrajectoryPoint()
   {
      super(new SimpleSE3TrajectoryPoint());
      geometryObject = getGeometryObject();
      setToZero(ReferenceFrame.getWorldFrame());
   }

   public FrameSE3TrajectoryPoint(ReferenceFrame referenceFrame)
   {
      this();
      setToZero(referenceFrame);
   }

   public FrameSE3TrajectoryPoint(double time, FramePoint3D position, FrameOrientation orientation, FrameVector3D linearVelocity, FrameVector3D angularVelocity)
   {
      this();
      setIncludingFrame(time, position, orientation, linearVelocity, angularVelocity);
   }

   public FrameSE3TrajectoryPoint(ReferenceFrame referenceFrame, SE3TrajectoryPointInterface<?> se3TrajectoryPointInterface)
   {
      this();
      setIncludingFrame(referenceFrame, se3TrajectoryPointInterface);
   }

   public FrameSE3TrajectoryPoint(FrameSE3TrajectoryPoint frameSE3TrajectoryPoint)
   {
      this();
      setIncludingFrame(frameSE3TrajectoryPoint);
   }

   @Override
   public void setPosition(Point3DReadOnly position)
   {
      geometryObject.setPosition(position);
   }

   public void setPosition(FramePoint3D position)
   {
      checkReferenceFrameMatch(position);
      geometryObject.setPosition(position.getPoint());
   }

   @Override
   public void setOrientation(QuaternionReadOnly orientation)
   {
      geometryObject.setOrientation(orientation);
   }

   public void setOrientation(FrameOrientation orientation)
   {
      checkReferenceFrameMatch(orientation);
      geometryObject.setOrientation(orientation.getQuaternion());
   }

   @Override
   public void setLinearVelocity(Vector3DReadOnly linearVelocity)
   {
      geometryObject.setLinearVelocity(linearVelocity);
   }

   public void setLinearVelocity(FrameVector3D linearVelocity)
   {
      checkReferenceFrameMatch(linearVelocity);
      geometryObject.setLinearVelocity(linearVelocity.getVector());
   }

   @Override
   public void setAngularVelocity(Vector3DReadOnly angularVelocity)
   {
      geometryObject.setAngularVelocity(angularVelocity);
   }

   public void setAngularVelocity(FrameVector3D angularVelocity)
   {
      checkReferenceFrameMatch(angularVelocity);
      geometryObject.setAngularVelocity(angularVelocity.getVector());
   }

   public void set(double time, Point3DReadOnly position, QuaternionReadOnly orientation, Vector3DReadOnly linearVelocity, Vector3DReadOnly angularVelocity)
   {
      geometryObject.set(time, position, orientation, linearVelocity, angularVelocity);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, double time, Point3DReadOnly position, QuaternionReadOnly orientation, Vector3DReadOnly linearVelocity,
         Vector3DReadOnly angularVelocity)
   {
      setToZero(referenceFrame);
      geometryObject.set(time, position, orientation, linearVelocity, angularVelocity);
   }

   public void set(double time, FramePoint3D position, FrameOrientation orientation, FrameVector3D linearVelocity, FrameVector3D angularVelocity)
   {
      checkReferenceFrameMatch(position);
      checkReferenceFrameMatch(orientation);
      checkReferenceFrameMatch(linearVelocity);
      checkReferenceFrameMatch(angularVelocity);
      geometryObject.set(time, position.getPoint(), orientation.getQuaternion(), linearVelocity.getVector(), angularVelocity.getVector());
   }

   public void setIncludingFrame(double time, FramePoint3D position, FrameOrientation orientation, FrameVector3D linearVelocity, FrameVector3D angularVelocity)
   {
      position.checkReferenceFrameMatch(orientation);
      position.checkReferenceFrameMatch(linearVelocity);
      position.checkReferenceFrameMatch(angularVelocity);
      setToZero(position.getReferenceFrame());
      geometryObject.set(time, position.getPoint(), orientation.getQuaternion(), linearVelocity.getVector(), angularVelocity.getVector());
   }

   public void set(double time, SE3WaypointInterface<?> se3Waypoint)
   {
      geometryObject.set(time, se3Waypoint);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, double time, SE3WaypointInterface<?> se3Waypoint)
   {
      setToZero(referenceFrame);
      geometryObject.set(time, se3Waypoint);
   }

   public void set(SE3TrajectoryPointInterface<?> se3TrajectoryPoint)
   {
      geometryObject.set(se3TrajectoryPoint);
   }

   public void setIncludingFrame(ReferenceFrame referenceFrame, SE3TrajectoryPointInterface<?> se3TrajectoryPoint)
   {
      setToZero(referenceFrame);
      geometryObject.set(se3TrajectoryPoint);
   }

   public void set(double time, FrameSE3Waypoint frameSE3Waypoint)
   {
      checkReferenceFrameMatch(frameSE3Waypoint);
      setTime(time);
      frameSE3Waypoint.get(geometryObject);
   }

   public void setIncludingFrame(double time, FrameSE3Waypoint frameSE3Waypoint)
   {
      setToZero(frameSE3Waypoint.getReferenceFrame());
      set(time, frameSE3Waypoint);
   }

   @Override
   public void setPositionToZero()
   {
      geometryObject.setPositionToZero();
   }

   @Override
   public void setOrientationToZero()
   {
      geometryObject.setOrientationToZero();
   }

   @Override
   public void setLinearVelocityToZero()
   {
      geometryObject.setLinearVelocityToZero();
   }

   @Override
   public void setAngularVelocityToZero()
   {
      geometryObject.setAngularVelocityToZero();
   }

   @Override
   public void setPositionToNaN()
   {
      geometryObject.setPositionToNaN();
   }

   @Override
   public void setOrientationToNaN()
   {
      geometryObject.setOrientationToNaN();
   }

   @Override
   public void setLinearVelocityToNaN()
   {
      geometryObject.setLinearVelocityToNaN();
   }

   @Override
   public void setAngularVelocityToNaN()
   {
      geometryObject.setAngularVelocityToNaN();
   }

   public void getSE3Waypoint(SE3Waypoint se3WaypointToPack)
   {
      EuclideanWaypoint euclideanWaypoint = geometryObject.getEuclideanWaypoint();
      se3WaypointToPack.setPosition(euclideanWaypoint.getPosition());
      se3WaypointToPack.setLinearVelocity(euclideanWaypoint.getLinearVelocity());

      SO3Waypoint so3Waypoint = geometryObject.getSO3Waypoint();
      se3WaypointToPack.setOrientation(so3Waypoint.getOrientation());
      se3WaypointToPack.setAngularVelocity(so3Waypoint.getAngularVelocity());
   }

   public void getFrameSE3Waypoint(FrameSE3Waypoint frameSE3Waypoint)
   {
      checkReferenceFrameMatch(frameSE3Waypoint);

      EuclideanWaypoint euclideanWaypoint = geometryObject.getEuclideanWaypoint();
      frameSE3Waypoint.setPosition(euclideanWaypoint.getPosition());
      frameSE3Waypoint.setLinearVelocity(euclideanWaypoint.getLinearVelocity());

      SO3Waypoint so3Waypoint = geometryObject.getSO3Waypoint();
      frameSE3Waypoint.setOrientation(so3Waypoint.getOrientation());
      frameSE3Waypoint.setAngularVelocity(so3Waypoint.getAngularVelocity());
   }

   @Override
   public double positionDistance(FrameSE3TrajectoryPoint other)
   {
      checkReferenceFrameMatch(other);
      return geometryObject.positionDistance(other.geometryObject);
   }

   @Override
   public void getPosition(Point3DBasics positionToPack)
   {
      geometryObject.getPosition(positionToPack);
   }

   @Override
   public void getOrientation(QuaternionBasics orientationToPack)
   {
      geometryObject.getOrientation(orientationToPack);
   }

   @Override
   public void getLinearVelocity(Vector3DBasics linearVelocityToPack)
   {
      geometryObject.getLinearVelocity(linearVelocityToPack);
   }

   @Override
   public void getAngularVelocity(Vector3DBasics angularVelocityToPack)
   {
      geometryObject.getAngularVelocity(angularVelocityToPack);
   }

   public void getPosition(FramePoint3D positionToPack)
   {
      checkReferenceFrameMatch(positionToPack);
      geometryObject.getPosition(positionToPack.getPoint());
   }

   public FramePoint3D getPositionCopy()
   {
      FramePoint3D positionCopy = new FramePoint3D(getReferenceFrame());
      getPosition(positionCopy);
      return positionCopy;
   }

   public void getOrientation(FrameOrientation orientationToPack)
   {
      checkReferenceFrameMatch(orientationToPack);
      geometryObject.getOrientation(orientationToPack.getQuaternion());
   }

   public FrameOrientation getOrientationCopy()
   {
      FrameOrientation orientationCopy = new FrameOrientation(getReferenceFrame());
      getOrientation(orientationCopy);
      return orientationCopy;
   }

   public void getPose(FramePose poseToPack)
   {
      checkReferenceFrameMatch(poseToPack);
      SE3Waypoint waypointData = geometryObject.waypointData;
      poseToPack.setPosition(waypointData.getEuclideanWaypoint().getPosition());
      poseToPack.setOrientation(waypointData.getSO3Waypoint().getOrientation());
   }

   public void getLinearVelocity(FrameVector3D linearVelocityToPack)
   {
      checkReferenceFrameMatch(linearVelocityToPack);
      geometryObject.getLinearVelocity(linearVelocityToPack.getVector());
   }

   public FrameVector3D getLinearVelocityCopy()
   {
      FrameVector3D linearVelocityCopy = new FrameVector3D(getReferenceFrame());
      getLinearVelocity(linearVelocityCopy);
      return linearVelocityCopy;
   }

   public void getAngularVelocity(FrameVector3D angularVelocityToPack)
   {
      checkReferenceFrameMatch(angularVelocityToPack);
      geometryObject.getAngularVelocity(angularVelocityToPack.getVector());
   }

   public FrameVector3D getAngularVelocityCopy()
   {
      FrameVector3D angularVelocityCopy = new FrameVector3D(getReferenceFrame());
      getAngularVelocity(angularVelocityCopy);
      return angularVelocityCopy;
   }

   public void getPositionIncludingFrame(FramePoint3D positionToPack)
   {
      positionToPack.setToZero(getReferenceFrame());
      geometryObject.getPosition(positionToPack.getPoint());
   }

   public void getOrientationIncludingFrame(FrameOrientation orientationToPack)
   {
      orientationToPack.setToZero(getReferenceFrame());
      geometryObject.getOrientation(orientationToPack.getQuaternion());
   }

   public void getPoseIncludingFrame(FramePose poseToPack)
   {
      poseToPack.setToZero(getReferenceFrame());
      getPose(poseToPack);
   }

   public void getLinearVelocityIncludingFrame(FrameVector3D linearVelocityToPack)
   {
      linearVelocityToPack.setToZero(getReferenceFrame());
      geometryObject.getLinearVelocity(linearVelocityToPack.getVector());
   }

   public void getAngularVelocityIncludingFrame(FrameVector3D angularVelocityToPack)
   {
      angularVelocityToPack.setToZero(getReferenceFrame());
      geometryObject.getAngularVelocity(angularVelocityToPack.getVector());
   }

   public double getPositionX()
   {
      return geometryObject.getPositionX();
   }

   public double getPositionY()
   {
      return geometryObject.getPositionY();
   }

   public double getPositionZ()
   {
      return geometryObject.getPositionZ();
   }

   public double getOrientationQx()
   {
      return geometryObject.getOrientationQx();
   }

   public double getOrientationQy()
   {
      return geometryObject.getOrientationQy();
   }

   public double getOrientationQz()
   {
      return geometryObject.getOrientationQz();
   }

   public double getOrientationQs()
   {
      return geometryObject.getOrientationQs();
   }

   public double getLinearVelocityX()
   {
      return geometryObject.getLinearVelocityX();
   }

   public double getLinearVelocityY()
   {
      return geometryObject.getLinearVelocityY();
   }

   public double getLinearVelocityZ()
   {
      return geometryObject.getLinearVelocityZ();
   }

   public double getAngularVelocityX()
   {
      return geometryObject.getAngularVelocityX();
   }

   public double getAngularVelocityY()
   {
      return geometryObject.getAngularVelocityY();
   }

   public double getAngularVelocityZ()
   {
      return geometryObject.getAngularVelocityZ();
   }
}
