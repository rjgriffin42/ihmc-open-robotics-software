package us.ihmc.simulationconstructionset.simulatedSensors;

import us.ihmc.graphics3DAdapter.GPULidar;
import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.SimulatedSensor;
import us.ihmc.utilities.lidar.polarLidar.geometry.LidarScanParameters;
import us.ihmc.robotics.geometry.RigidBodyTransform;

public class LidarMount implements SimulatedSensor
{
   
   private final LidarScanParameters lidarScanParameters;
   protected RigidBodyTransform transformToHere = new RigidBodyTransform();
   private final RigidBodyTransform transformFromJoint;
   private GPULidar lidar;
   private final String lidarName;

   private Joint parentJoint;
   
   public LidarMount(RigidBodyTransform transform3d, LidarScanParameters lidarScanParameters, String sensorName)
   {
      this.transformFromJoint = new RigidBodyTransform(transform3d);
      this.lidarScanParameters = lidarScanParameters;
      this.lidarName = sensorName;
   }

   public void updateTransform(RigidBodyTransform transformToHere, double time)
   {
      this.transformToHere.set(transformToHere);
      this.transformToHere.multiply(transformFromJoint);

      if(lidar != null)
      {
         lidar.setTransformFromWorld(this.transformToHere, time);
      }
   }
   
   public String getName()
   {
      return lidarName;
   }
   
   public void setLidar(GPULidar lidar)
   {
      this.lidar = lidar;
   }

   public LidarScanParameters getLidarScanParameters()
   {
      return lidarScanParameters;
   }
   
   public void setWorld(Graphics3DAdapter graphics3dAdapter)
   {
      // TODO Auto-generated method stub
      
   }
   
   public void setParentJoint(Joint parent)
   {
      this.parentJoint = parent;
   }

   public Joint getParentJoint()
   {
      return parentJoint;
   }

}
