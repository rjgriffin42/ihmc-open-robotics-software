package us.ihmc.darpaRoboticsChallenge.sensors;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packets.IMUPacket;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.simulationconstructionset.IMUMount;
import us.ihmc.simulationconstructionset.robotController.RobotController;

/**
 * This is to simulate the Multisense IMU on Atlas. In reality the data comes from ROS and
 * arrives at the sensor manager. So here we send the IMU sensor data from simulation to
 * the sensor manager.
 *
 * @author Georg
 *
 */
public class SimulatedIMUSensor implements RobotController
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final IMUMount imuMount;
   private final DoubleYoVariable robotTime;
   private final GlobalDataProducer dataProducer;

   private final Vector3d linearAcceleration = new Vector3d();
   private final Quat4d orientation = new Quat4d();
   private final Vector3d angularVelocity = new Vector3d();

   private final IMUPacket imuPacket = new IMUPacket();

   public SimulatedIMUSensor(IMUMount imuMount, GlobalDataProducer dataProducer, DoubleYoVariable time)
   {
      this.imuMount = imuMount;
      this.dataProducer = dataProducer;
      this.robotTime = time;
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getDescription()
   {
      return getName();
   }

   @Override
   public void doControl()
   {
      imuMount.getLinearAccelerationInBody(linearAcceleration);
      imuMount.getOrientation(orientation);
      imuMount.getAngularVelocityInBody(angularVelocity);

      imuPacket.linearAcceleration.set(linearAcceleration);
      imuPacket.orientation.set(orientation);
      imuPacket.angularVelocity.set(angularVelocity);
      imuPacket.time = robotTime.getDoubleValue();
      imuPacket.setDestination(PacketDestination.SENSOR_MANAGER);
      dataProducer.queueDataToSend(imuPacket);
   }

}
