package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import java.util.concurrent.ConcurrentLinkedQueue;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.driving.DrivingInterface;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.driving.DrivingInterface.GearName;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.driving.VehicleModelObjects;
import us.ihmc.packets.LowLevelDrivingAction;
import us.ihmc.packets.LowLevelDrivingCommand;
import us.ihmc.packets.LowLevelDrivingStatus;
import us.ihmc.utilities.net.ObjectConsumer;

public class DrivingCommandProvider implements ObjectConsumer<LowLevelDrivingCommand>
{
   private final ConcurrentLinkedQueue<LowLevelDrivingCommand> drivingCommands = new ConcurrentLinkedQueue<LowLevelDrivingCommand>();
   private DrivingInterface drivingInterface;
   
   private double maximumGasPedalDistance;
   private double maximumBrakePedalDistance;
   
   private boolean doNothing = false;
   private double doNothingEndTime = 0;
   
   
   public void consumeObject(LowLevelDrivingCommand object)
   {
      drivingCommands.add(object);
   }
   
   private GearName getGearName(double value)
   {
      if(value > 0.0)
      {
         return GearName.FORWARD;
      }
      else
      {
         return GearName.REVERSE;
      }
      
   }
   
   private boolean getHandbrakeState(double value)
   {
      if(value > 0.0)
      {
         return true;
      }
      else
      {
         return false;
      }
      
   }

   public void doControl(double currentTime)
   {
      LowLevelDrivingCommand command = drivingCommands.poll();
      while(command != null)
      {
         double value = command.getValue();
         switch(command.getAction())
         {
         case DIRECTION:  
            drivingInterface.setGear(getGearName(value), false);
         break;
         case FOOTBRAKE: 
            drivingInterface.pressBrakePedal(value * maximumBrakePedalDistance);
         break;
         case GASPEDAL:
            drivingInterface.pressGasPedal(value * maximumGasPedalDistance);
            break;
         case GET_IN_CAR:
            System.err.println("Cannot get in the car using magic");
            break;
         case HANDBRAKE:
            drivingInterface.setHandBrake(getHandbrakeState(value), false);
            break;
         case STEERING:
            drivingInterface.turnSteeringWheel(value);
            break;
         case DO_NOTHING:
            doNothing = true;
            doNothingEndTime = currentTime + value;
            break;
         case REINITIALIZE:
            drivingInterface.reinitialize();
            break;
         }
         
         command = drivingCommands.poll();
      }
      
      
      if(doNothing)
      {
         if(currentTime > doNothingEndTime)
         {
            doNothing = false;
            drivingInterface.getStatusProducer().queueDataToSend(new LowLevelDrivingStatus(LowLevelDrivingAction.DO_NOTHING, true));
         }
      }
   }

   public void setDrivingInterfaceAndVehicleModel(DrivingInterface drivingInterface, VehicleModelObjects vehicleModelObjects)
   {
      this.drivingInterface = drivingInterface;
      this.maximumGasPedalDistance = vehicleModelObjects.getMaximumGasPedalDistance();
      this.maximumBrakePedalDistance = vehicleModelObjects.getMaximumBrakePedalDistance();
   }

}
