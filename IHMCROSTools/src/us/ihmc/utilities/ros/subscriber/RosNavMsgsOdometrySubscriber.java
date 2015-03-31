package us.ihmc.utilities.ros.subscriber;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.kinematics.TimeStampedTransform3D;

public abstract class RosNavMsgsOdometrySubscriber extends AbstractRosTopicSubscriber<nav_msgs.Odometry>
{
   private long timeStamp;
   private String frameID;
   private Vector3d pos;
   private Quat4d rot;
      
   public RosNavMsgsOdometrySubscriber()
   {
      super(nav_msgs.Odometry._TYPE);    
   }

   public synchronized void onNewMessage(nav_msgs.Odometry msg)
   {
      // get timestamp
      timeStamp = msg.getHeader().getStamp().totalNsecs();
      
      // get Point
      Pose pose = msg.getPose().getPose();
      Point position = pose.getPosition();
      Double posx = position.getX();
      Double posy = position.getY();
      Double posz = position.getZ();
      
      pos = new Vector3d(posx, posy, posz);
      
      // get Rotation
      Quaternion orientation = pose.getOrientation();
      Double rotx = orientation.getX();
      Double roty = orientation.getY();
      Double rotz = orientation.getZ();
      Double rotw = orientation.getW();
      
      rot = new Quat4d(rotx, roty, rotz, rotw);
      
      // get frameID
      frameID = msg.getHeader().getFrameId();

      TimeStampedTransform3D transform = new TimeStampedTransform3D();
      transform.getTransform3D().setTranslation(pos);
      transform.getTransform3D().setRotation(rot);
      transform.setTimeStamp(timeStamp);
      
      newPose(frameID, transform);
   }
   
   public synchronized Vector3d getPoint()
   {
      return pos;     
   }
   
   public synchronized Quat4d getRotation()
   {
      return rot;
   }
   
   public synchronized long getTimestamp()
   {
      return timeStamp;
   }
   
   public synchronized String getFrameID()
   {
      return frameID;
   }
   
   protected abstract void newPose(String frameID, TimeStampedTransform3D transform);
   
}
