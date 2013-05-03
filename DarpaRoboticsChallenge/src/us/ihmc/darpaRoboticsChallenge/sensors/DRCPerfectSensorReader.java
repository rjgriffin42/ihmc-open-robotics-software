package us.ihmc.darpaRoboticsChallenge.sensors;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import us.ihmc.sensorProcessing.sensors.ForceSensorDataHolder;
import us.ihmc.sensorProcessing.simulatedSensors.ControllerDispatcher;
import us.ihmc.sensorProcessing.simulatedSensors.ForceSensorDefinition;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.sensorProcessing.simulatedSensors.WrenchCalculatorInterface;
import us.ihmc.sensorProcessing.stateEstimation.JointAndIMUSensorDataSource;
import us.ihmc.utilities.math.TimeTools;

import com.yobotics.simulationconstructionset.IntegerYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RawSensorReader;
import com.yobotics.simulationconstructionset.robotController.RobotController;

public class DRCPerfectSensorReader implements SensorReader, RobotController
{
   private final YoVariableRegistry registry = new YoVariableRegistry("DRCPerfectSensorReader");
   private final IntegerYoVariable step = new IntegerYoVariable("step", registry);
   private final long estimateDTinNs;
   private RawSensorReader rawSensorReader;
   private ControllerDispatcher controllerDispatcher;

   private final LinkedHashMap<ForceSensorDefinition, WrenchCalculatorInterface> forceTorqueSensors = new LinkedHashMap<ForceSensorDefinition, WrenchCalculatorInterface>();

   private ForceSensorDataHolder forceSensorDataHolder;
   
   public DRCPerfectSensorReader(double estimateDT)
   {
      this.estimateDTinNs = TimeTools.toNanoSeconds(estimateDT);
      step.set(9807);
   }

   public void setSensorReader(RawSensorReader rawSensorReader)
   {
      this.rawSensorReader = rawSensorReader;
   }
   
   public void setJointAndIMUSensorDataSource(JointAndIMUSensorDataSource jointAndIMUSensorDataSource)
   {
   }

   public void setControllerDispatcher(ControllerDispatcher controllerDispatcher)
   {
      this.controllerDispatcher = controllerDispatcher;
   }

   public void setForceSensorDataHolder(ForceSensorDataHolder forceSensorDataHolder)
   {
      this.forceSensorDataHolder = forceSensorDataHolder;
   }

   public void initialize()
   {
      read();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return "DRCPerfectSensorReader";
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {
      step.increment();
      if(controllerDispatcher != null)
      {
         controllerDispatcher.waitUntilComputationIsDone();
      }
      
      read();
      
      if(controllerDispatcher != null)
      {
         controllerDispatcher.startEstimator(estimateDTinNs * step.getIntegerValue());
      }
   }

   private void read()
   {
      if(rawSensorReader != null)
      {
         rawSensorReader.read();
      }
      
      if(forceSensorDataHolder != null)
      {
         for(Entry<ForceSensorDefinition, WrenchCalculatorInterface> forceTorqueSensorEntry : forceTorqueSensors.entrySet())
         {
            final WrenchCalculatorInterface forceTorqueSensor = forceTorqueSensorEntry.getValue();
            forceTorqueSensor.startComputation();
            forceTorqueSensor.waitUntilComputationIsDone();  
            forceSensorDataHolder.setForceSensorValue(forceTorqueSensorEntry.getKey(), forceTorqueSensor.getForceSensorOutputPort().getData());
         }
      }
   }
   
   public void addForceTorqueSensorPort(ForceSensorDefinition forceSensorDefinition, WrenchCalculatorInterface groundContactPointBasedWrenchCalculator)
   {
      forceTorqueSensors.put(forceSensorDefinition, groundContactPointBasedWrenchCalculator);
   }

}
