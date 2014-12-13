package us.ihmc.darpaRoboticsChallenge.controllers;

import java.util.List;

import us.ihmc.communication.packets.sensing.RawIMUPacket;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.simulationconstructionset.robotController.MultiThreadedRobotControlElement;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

public class DRCSimulatedIMUPublisher implements MultiThreadedRobotControlElement
{

   GlobalDataProducer globalDataProducer;
   RawIMUPacket packet = new RawIMUPacket();
   IMUSensorReadOnly imuSensorReader = null;

   public DRCSimulatedIMUPublisher(GlobalDataProducer globalDataProducer, List<? extends IMUSensorReadOnly> simulatedIMUOutput, String headLinkName)
   {

      for (int i = 0; i < simulatedIMUOutput.size(); i++)
      {
         if (simulatedIMUOutput.get(i).getSensorName().contains(headLinkName))
         {
            imuSensorReader = simulatedIMUOutput.get(i);
            break;
         }
      }
      this.globalDataProducer = globalDataProducer;
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void read(long currentClockTime)
   {
      if (imuSensorReader != null)
      {
         imuSensorReader.getLinearAccelerationMeasurement(packet.linearAcceleration);
         packet.timestampInNanoSecond = currentClockTime;
         if (globalDataProducer != null)
            globalDataProducer.getObjectCommunicator().consumeObject(packet);
      }
   }

   @Override
   public void run()
   {

   }

   @Override
   public void write(long timestamp)
   {
      //not writing to robot
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }

   @Override
   public String getName()
   {
      return "SlowPublisher";
   }

   @Override
   public YoGraphicsListRegistry getDynamicGraphicObjectsListRegistry()
   {
      return null;
   }

   @Override
   public long nextWakeupTime()
   {
      return 0;
   }

}
