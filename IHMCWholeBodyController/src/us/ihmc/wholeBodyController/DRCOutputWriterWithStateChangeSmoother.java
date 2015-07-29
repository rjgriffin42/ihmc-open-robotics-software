package us.ihmc.wholeBodyController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerStateChangedListener;
import us.ihmc.robotics.humanoidRobot.model.ForceSensorDataHolderReadOnly;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.time.TimeTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoVariable;

public class DRCOutputWriterWithStateChangeSmoother implements DRCOutputWriter
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final DoubleYoVariable alphaForJointTorqueForStateChanges = new DoubleYoVariable("alphaJointTorqueForStateChanges", registry);

   private final ArrayList<OneDoFJoint> allJoints = new ArrayList<>();
   private final LinkedHashMap<OneDoFJoint, AlphaFilteredYoVariable> jointTorquesSmoothedAtStateChange = new LinkedHashMap<>();

   private final AtomicBoolean hasHighLevelControllerStateChanged = new AtomicBoolean(false);
   private final DoubleYoVariable timeAtHighLevelControllerStateChange = new DoubleYoVariable("timeAtControllerStateChange", registry);
   private final DoubleYoVariable slopTime = new DoubleYoVariable("slopTimeForSmoothedJointTorques", registry);

   private final DRCOutputWriter drcOutputWriter;

   public DRCOutputWriterWithStateChangeSmoother(DRCOutputWriter drcOutputWriter)
   {
      this.drcOutputWriter = drcOutputWriter;
      registry.addChild(drcOutputWriter.getControllerYoVariableRegistry());

      alphaForJointTorqueForStateChanges.set(0.0);
      slopTime.set(0.16);
      timeAtHighLevelControllerStateChange.set(Double.NEGATIVE_INFINITY);
   }

   @Override
   public void initialize()
   {
      drcOutputWriter.initialize();      
   }

   @Override
   public void writeAfterController(long timestamp)
   {
      if (hasHighLevelControllerStateChanged.get())
      {
         hasHighLevelControllerStateChanged.set(false);
         timeAtHighLevelControllerStateChange.set(TimeTools.nanoSecondstoSeconds(timestamp));
      }

      double currentTime = TimeTools.nanoSecondstoSeconds(timestamp);
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

      drcOutputWriter.writeAfterController(timestamp);
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
   public void setFullRobotModel(SDFFullRobotModel controllerModel, RawJointSensorDataHolderMap rawJointSensorDataHolderMap)
   {
      drcOutputWriter.setFullRobotModel(controllerModel, rawJointSensorDataHolderMap);

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
      drcOutputWriter.setForceSensorDataHolderForController(forceSensorDataHolderForController);
   }

   @Override
   public YoVariableRegistry getControllerYoVariableRegistry()
   {
      return registry;
   }

}
