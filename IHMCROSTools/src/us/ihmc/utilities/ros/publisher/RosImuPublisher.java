package us.ihmc.utilities.ros.publisher;

import geometry_msgs.Quaternion;
import geometry_msgs.Vector3;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.ros.message.Time;

import std_msgs.Header;

public class RosImuPublisher extends RosTopicPublisher<sensor_msgs.Imu>
{
   int counter=0;
   public RosImuPublisher(boolean latched)
   {
      super(sensor_msgs.Imu._TYPE,latched);
   }
   
   public void publish(long timestamp, Vector3f linearAcceleration, Quat4f orientation, Vector3f angularVelocity)
   {
      sensor_msgs.Imu message = getMessage();

      Vector3  localLinearAcceleration = newMessageFromType(Vector3._TYPE);
      localLinearAcceleration.setX(linearAcceleration.x);
      localLinearAcceleration.setY(linearAcceleration.y);
      localLinearAcceleration.setZ(linearAcceleration.z);
      
      Quaternion localOrientation = newMessageFromType(Quaternion._TYPE);
      localOrientation.setW(orientation.w);
      localOrientation.setX(orientation.x);
      localOrientation.setY(orientation.y);
      localOrientation.setZ(orientation.z);
      
      Vector3 localAngularVelocity = newMessageFromType(Vector3._TYPE);
      localAngularVelocity.setX(angularVelocity.x);
      localAngularVelocity.setY(angularVelocity.y);
      localAngularVelocity.setZ(angularVelocity.z);
      
      Header header = newMessageFromType(Header._TYPE);
      header.setFrameId("pelvis_imu");
      header.setStamp(Time.fromNano(timestamp));
      header.setSeq(counter);
            
      message.setHeader(header);
      message.setLinearAcceleration(localLinearAcceleration);
      message.setOrientation(localOrientation);
      message.setAngularVelocity(localAngularVelocity);
      
      publish(message);
   }
}