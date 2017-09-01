package us.ihmc.wholeBodyController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import us.ihmc.commons.Conversions;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.controllers.ControllerStateChangedListener;
import us.ihmc.robotics.math.filters.AlphaFilteredYoVariable;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.sensors.ForceSensorDataHolderReadOnly;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class DRCOutputProcessorWithStateChangeSmoother implements DRCOutputProcessor
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final YoDouble alphaForJointTorqueForStateChanges = new YoDouble("alphaJointTorqueForStateChanges", registry);

   private final ArrayList<OneDoFJoint> allJoints = new ArrayList<>();
   private final LinkedHashMap<OneDoFJoint, AlphaFilteredYoVariable> jointTorquesSmoothedAtStateChange = new LinkedHashMap<>();

   private final AtomicBoolean hasHighLevelControllerStateChanged = new AtomicBoolean(false);
   private final YoDouble timeAtHighLevelControllerStateChange = new YoDouble("timeAtControllerStateChange", registry);
   private final YoDouble slopTime = new YoDouble("slopTimeForSmoothedJointTorques", registry);

   private final DRCOutputProcessor drcOutputProcessor;

   public DRCOutputProcessorWithStateChangeSmoother(DRCOutputProcessor drcOutputWriter)
   {
      this.drcOutputProcessor = drcOutputWriter;
      if(drcOutputWriter != null)
      {
         registry.addChild(drcOutputWriter.getControllerYoVariableRegistry());
      }

      alphaForJointTorqueForStateChanges.set(0.0);
      slopTime.set(0.16);
      timeAtHighLevelControllerStateChange.set(Double.NEGATIVE_INFINITY);
   }

   @Override
   public void initialize()
   {
      if(drcOutputProcessor != null)
      {
         drcOutputProcessor.initialize();               
      }
   }

   @Override
   public void processAfterController(long timestamp)
   {
      if (hasHighLevelControllerStateChanged.get())
      {
         hasHighLevelControllerStateChanged.set(false);
         timeAtHighLevelControllerStateChange.set(Conversions.nanosecondsToSeconds(timestamp));
      }

      double currentTime = Conversions.nanosecondsToSeconds(timestamp);
      double deltaTime = Math.max(currentTime - timeAtHighLevelControllerStateChange.getDoubleValue(), 0.0);
      
      if (deltaTime < slopTime.getDoubleValue())
      {
         alphaForJointTorqueForStateChanges.set(1.0 - deltaTime / slopTime.getDoubleValue());
      }
      else
      {
         alphaForJointTorqueForStateChanges.set(0.0);
      }

      for (int i = 0; i < allJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = allJoints.get(i);
         double tau = oneDoFJoint.getTau();
         AlphaFilteredYoVariable smoothedJointTorque = jointTorquesSmoothedAtStateChange.get(oneDoFJoint);
         smoothedJointTorque.update(tau);
         oneDoFJoint.setTau(smoothedJointTorque.getDoubleValue());
      }

      if(drcOutputProcessor != null)
      {
         drcOutputProcessor.processAfterController(timestamp);
      }
   }

   public ControllerStateChangedListener createControllerStateChangedListener()
   {
      ControllerStateChangedListener controllerStateChangedListener = new ControllerStateChangedListener()
      {
         @Override
         public void controllerStateHasChanged(Enum<?> oldState, Enum<?> newState)
         {
            hasHighLevelControllerStateChanged.set(true);            
         }
      };

      return controllerStateChangedListener;
   }

   @Override
   public void setFullRobotModel(FullHumanoidRobotModel controllerModel, RawJointSensorDataHolderMap rawJointSensorDataHolderMap)
   {
      if(drcOutputProcessor != null)
      {
         drcOutputProcessor.setFullRobotModel(controllerModel, rawJointSensorDataHolderMap);
      }

      OneDoFJoint[] joints = controllerModel.getOneDoFJoints();
      for (int i = 0; i < joints.length; i++)
      {
         OneDoFJoint oneDoFJoint = joints[i];
         String jointName = oneDoFJoint.getName();
         allJoints.add(oneDoFJoint);

         AlphaFilteredYoVariable jointTorqueSmoothedAtStateChange = new AlphaFilteredYoVariable("smoothed_tau_" + jointName, registry, alphaForJointTorqueForStateChanges);
         jointTorquesSmoothedAtStateChange.put(oneDoFJoint, jointTorqueSmoothedAtStateChange);
      }
   }

   @Override
   public void setForceSensorDataHolderForController(ForceSensorDataHolderReadOnly forceSensorDataHolderForController)
   {
      if(drcOutputProcessor != null)
      {
         drcOutputProcessor.setForceSensorDataHolderForController(forceSensorDataHolderForController);
      }
   }

   @Override
   public YoVariableRegistry getControllerYoVariableRegistry()
   {
      return registry;
   }

}
