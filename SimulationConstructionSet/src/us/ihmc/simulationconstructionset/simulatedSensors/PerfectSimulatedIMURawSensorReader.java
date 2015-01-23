package us.ihmc.simulationconstructionset.simulatedSensors;

import us.ihmc.simulationconstructionset.rawSensors.RawIMUSensorsInterface;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.yoUtilities.math.corruptors.NoisyDoubleYoVariable;

public class PerfectSimulatedIMURawSensorReader extends SimulatedIMURawSensorReader
{
   public PerfectSimulatedIMURawSensorReader(RawIMUSensorsInterface rawSensors, int imuIndex, RigidBody rigidBody, ReferenceFrame imuFrame, RigidBody rootBody, SpatialAccelerationVector rootAcceleration)
   {
      super(rawSensors, imuIndex, rigidBody, imuFrame, rootBody, rootAcceleration);
   }

   @Override
   protected void initializeNoise()
   {
      rotationMatrix.setIsNoisy(false);
      setIsNoisyToFalse(accelList);
      setIsNoisyToFalse(gyroList);
      setIsNoisyToFalse(compassList);
   }
   
   @Override
   protected void simulateIMU()
   {
      rotationMatrix.update(perfM00.getDoubleValue(), perfM01.getDoubleValue(), perfM02.getDoubleValue(), perfM10.getDoubleValue(), perfM11.getDoubleValue(), perfM12.getDoubleValue(), perfM20.getDoubleValue(), perfM21.getDoubleValue(), perfM22.getDoubleValue());
      
      accelX.update();
      accelY.update();
      accelZ.update();
      
      gyroX.update();
      gyroY.update();
      gyroZ.update();
      
      compassX.update();
      compassY.update();
      compassZ.update();
   }

   private void setIsNoisyToFalse(NoisyDoubleYoVariable[] list)
   {
      for (NoisyDoubleYoVariable i : list)
      {
         i.setIsNoisy(false);
      }
   }
}