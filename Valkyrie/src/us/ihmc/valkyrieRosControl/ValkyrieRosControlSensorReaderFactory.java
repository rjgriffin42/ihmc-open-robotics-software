package us.ihmc.valkyrieRosControl;

import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.minlog.Log;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.SixDoFJoint;
import us.ihmc.robotics.sensors.ContactSensorHolder;
import us.ihmc.robotics.sensors.ForceSensorDefinition;
import us.ihmc.robotics.sensors.IMUDefinition;
import us.ihmc.rosControl.EffortJointHandle;
import us.ihmc.rosControl.wholeRobot.PositionJointHandle;
import us.ihmc.rosControl.wholeRobot.ForceTorqueSensorHandle;
import us.ihmc.rosControl.wholeRobot.IMUHandle;
import us.ihmc.sensorProcessing.model.DesiredJointDataHolder;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReaderFactory;
import us.ihmc.sensorProcessing.simulatedSensors.StateEstimatorSensorDefinitions;
import us.ihmc.sensorProcessing.stateEstimation.SensorProcessingConfiguration;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerFailureListener;
import us.ihmc.tools.TimestampProvider;
import us.ihmc.valkyrie.parameters.ValkyrieSensorInformation;
import us.ihmc.valkyrieRosControl.dataHolders.*;

public class ValkyrieRosControlSensorReaderFactory implements SensorReaderFactory
{
   private StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions;
   private ValkyrieRosControlSensorReader sensorReader;

   private final SensorProcessingConfiguration sensorProcessingConfiguration;

   private final HashMap<String, EffortJointHandle> effortJointHandles;
   private final HashMap<String, PositionJointHandle> positionJointHandles;
   private final HashMap<String, IMUHandle> imuHandles;
   private final HashMap<String, ForceTorqueSensorHandle> forceTorqueSensorHandles;

   private final TimestampProvider timestampProvider;
   private final ValkyrieSensorInformation sensorInformation;

   public ValkyrieRosControlSensorReaderFactory(TimestampProvider timestampProvider, SensorProcessingConfiguration sensorProcessingConfiguration,
         HashMap<String, EffortJointHandle> effortJointHandles, HashMap<String, PositionJointHandle> positionJointHandles, HashMap<String, IMUHandle> imuHandles, HashMap<String, ForceTorqueSensorHandle> forceTorqueSensorHandles,
         ValkyrieSensorInformation sensorInformation)
   {
      this.timestampProvider = timestampProvider;
      this.sensorProcessingConfiguration = sensorProcessingConfiguration;

      this.effortJointHandles = effortJointHandles;
      this.positionJointHandles = positionJointHandles;
      this.imuHandles = imuHandles;
      this.forceTorqueSensorHandles = forceTorqueSensorHandles;

      this.sensorInformation = sensorInformation;
   }

   @Override
   public void build(SixDoFJoint rootJoint, IMUDefinition[] imuDefinitions, ForceSensorDefinition[] forceSensorDefinitions,
         ContactSensorHolder contactSensorHolder, RawJointSensorDataHolderMap rawJointSensorDataHolderMap,
         DesiredJointDataHolder estimatorDesiredJointDataHolder, YoVariableRegistry parentRegistry)
   {
      YoVariableRegistry sensorReaderRegistry = new YoVariableRegistry("ValkyrieRosControlSensorReader");

      ArrayList<YoEffortJointHandleHolder> yoEffortJointHandleHolders = new ArrayList<>();
      ArrayList<YoPositionJointHandleHolder> yoPositionJointHandleHolders = new ArrayList<>();
      ArrayList<YoIMUHandleHolder> yoIMUHandleHolders = new ArrayList<>();
      ArrayList<YoForceTorqueSensorHandle> yoForceTorqueSensorHandles = new ArrayList<>();

      stateEstimatorSensorDefinitions = new StateEstimatorSensorDefinitions();

      for (InverseDynamicsJoint joint : ScrewTools.computeSubtreeJoints(rootJoint.getSuccessor()))
      {
         if (joint instanceof OneDoFJoint)
         {
            OneDoFJoint oneDoFJoint = (OneDoFJoint) joint;
            stateEstimatorSensorDefinitions.addJointSensorDefinition(oneDoFJoint);
            if (effortJointHandles.containsKey(joint.getName()))
            {
               YoEffortJointHandleHolder holder = new YoEffortJointHandleHolder(effortJointHandles.get(joint.getName()), oneDoFJoint,
                     estimatorDesiredJointDataHolder.get(oneDoFJoint), sensorReaderRegistry);
               yoEffortJointHandleHolders.add(holder);
            }
            else if(positionJointHandles.containsKey(joint.getName()))
            {
               YoPositionJointHandleHolder holder = new YoPositionJointHandleHolder(positionJointHandles.get(joint.getName()), oneDoFJoint,
                     estimatorDesiredJointDataHolder.get(oneDoFJoint), sensorReaderRegistry);
               yoPositionJointHandleHolders.add(holder);
            }
         }
      }

      for (IMUDefinition imuDefinition : imuDefinitions)
      {
         if (ValkyrieRosControlController.USE_USB_MICROSTRAIN_IMUS)
         {
            HashMap<String, Integer> imuUSBSerialIds = sensorInformation.getImuUSBSerialIds();
            if (imuUSBSerialIds.containsKey(imuDefinition.getName()))
            {
               Log.info("Starting listener for IMU " + imuDefinition.getName());
               YoMicroStrainIMUHandleHolder holder = YoMicroStrainIMUHandleHolder.create(imuUSBSerialIds.get(imuDefinition.getName()), imuDefinition,
                     parentRegistry);
               yoIMUHandleHolders.add(holder);
               stateEstimatorSensorDefinitions.addIMUSensorDefinition(imuDefinition);
            }
            else
            {
               stateEstimatorSensorDefinitions.addIMUSensorDefinition(imuDefinition);
               System.err.println("Cannot create listener for IMU " + imuDefinition.getName() + ", cannot find corresponding serial in ValkyrieSensorNames");
            }
         }
         else if(ValkyrieRosControlController.USE_SWITCHABLE_FILTER_HOLDER_FOR_NON_USB_IMUS)
         {
            String name = imuDefinition.getName();
            name = name.replace(imuDefinition.getRigidBody().getName() + "_", "");

            if(imuHandles.containsKey("CF" + name) && imuHandles.containsKey("EF" + name))
            {
               IMUHandle complimentaryFilterHandle =  imuHandles.get("CF" + name);
               IMUHandle kalmanFilterHandle = imuHandles.get("EF" + name);

               YoSwitchableFilterModeIMUHandleHolder holder = YoSwitchableFilterModeIMUHandleHolder.create(complimentaryFilterHandle, kalmanFilterHandle, imuDefinition, sensorReaderRegistry);
               yoIMUHandleHolders.add(holder);
               stateEstimatorSensorDefinitions.addIMUSensorDefinition(imuDefinition);
            }
            else
            {
               throw new RuntimeException("ValkyrieRosControlController.USE_SWITCHABLE_FILTER_HOLDER_FOR_NON_USB_IMUS set to true, but cannot find required CF and EF handle names required for this functionality.");
            }
         }
         else
         {
            String name = imuDefinition.getName();
            name = name.replace(imuDefinition.getRigidBody().getName() + "_", "");
            if (imuHandles.containsKey(name))
            {
               stateEstimatorSensorDefinitions.addIMUSensorDefinition(imuDefinition);
               YoIMUHandleHolder holder = new YoIMUHandleHolder(imuHandles.get(name), imuDefinition, sensorReaderRegistry);
               yoIMUHandleHolders.add(holder);
            }
         }
      }

      for (ForceSensorDefinition forceSensorDefinition : forceSensorDefinitions)
      {
         System.out.println("Looking for forceSensorDefinition " + forceSensorDefinition.getSensorName());
         if (forceTorqueSensorHandles.containsKey(forceSensorDefinition.getSensorName()))
         {
            stateEstimatorSensorDefinitions.addForceSensorDefinition(forceSensorDefinition);
            YoForceTorqueSensorHandle holder = new YoForceTorqueSensorHandle(forceTorqueSensorHandles.get(forceSensorDefinition.getSensorName()),
                  forceSensorDefinition, sensorReaderRegistry);
            yoForceTorqueSensorHandles.add(holder);
         }
      }

      sensorReader = new ValkyrieRosControlSensorReader(stateEstimatorSensorDefinitions, sensorProcessingConfiguration, timestampProvider,
            yoEffortJointHandleHolders,
            yoPositionJointHandleHolders,
            yoIMUHandleHolders, yoForceTorqueSensorHandles, sensorReaderRegistry);

      parentRegistry.addChild(sensorReaderRegistry);

   }

   @Override
   public ValkyrieRosControlSensorReader getSensorReader()
   {
      return sensorReader;
   }

   @Override
   public StateEstimatorSensorDefinitions getStateEstimatorSensorDefinitions()
   {
      return stateEstimatorSensorDefinitions;
   }

   @Override
   public boolean useStateEstimator()
   {
      return true;
   }

   public ControllerFailureListener createControllerFailureListener()
   {
      ControllerFailureListener listener = new ControllerFailureListener()
      {
         @Override
         public void controllerFailed(FrameVector2d fallingDirection)
         {
            sensorReader.handleControllerFailure();
         }
      };
      return listener;
   }
}
