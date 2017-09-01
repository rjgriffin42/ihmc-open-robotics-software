package us.ihmc.wholeBodyController;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.esotericsoftware.kryo.io.Output;

import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.sensors.ForceSensorDataHolderReadOnly;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;

public class DRCOutputProcessorWithTorqueOffsets implements DRCOutputProcessor, JointTorqueOffsetProcessor
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final DRCOutputProcessor drcOutputWriter;

   private final YoDouble alphaTorqueOffset = new YoDouble("alphaTorqueOffset",
         "Filter for integrating acceleration to get a torque offset at each joint", registry);

   private final YoBoolean resetTorqueOffsets = new YoBoolean("resetTorqueOffsets", registry);

   private ArrayList<OneDoFJoint> oneDoFJoints;
   private LinkedHashMap<OneDoFJoint, YoDouble> torqueOffsetMap;

   private final double updateDT;

   public DRCOutputProcessorWithTorqueOffsets(DRCOutputProcessor drcOutputWriter, double updateDT)
   {
      this.updateDT = updateDT;
      this.drcOutputWriter = drcOutputWriter;
      if(drcOutputWriter != null)
      {
         registry.addChild(drcOutputWriter.getControllerYoVariableRegistry());
      }
   }

   @Override
   public void initialize()
   {
      if(drcOutputWriter != null)
      {
         drcOutputWriter.initialize();
      }
   }

   @Override
   public void processAfterController(long timestamp)
   {
      for (int i = 0; i < oneDoFJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = oneDoFJoints.get(i);
         double desiredAcceleration = oneDoFJoint.getQddDesired();

         YoDouble torqueOffsetVariable = torqueOffsetMap.get(oneDoFJoint);
         if (resetTorqueOffsets.getBooleanValue())
            torqueOffsetVariable.set(0.0);

         double offsetTorque = torqueOffsetVariable.getDoubleValue();
         double ditherTorque = 0.0;

         double alpha = alphaTorqueOffset.getDoubleValue();
         offsetTorque = alpha * (offsetTorque + desiredAcceleration * updateDT) + (1.0 - alpha) * offsetTorque;
         torqueOffsetVariable.set(offsetTorque);
         oneDoFJoint.setTau(oneDoFJoint.getTau() + offsetTorque + ditherTorque);
      }

      if(drcOutputWriter != null)
      {
         drcOutputWriter.processAfterController(timestamp);
      }
   }

   @Override
   public void setFullRobotModel(FullHumanoidRobotModel controllerModel, RawJointSensorDataHolderMap rawJointSensorDataHolderMap)
   {
      if(drcOutputWriter != null)
      {
         drcOutputWriter.setFullRobotModel(controllerModel, rawJointSensorDataHolderMap);
      }

      oneDoFJoints = new ArrayList<OneDoFJoint>();
      controllerModel.getOneDoFJoints(oneDoFJoints);

      torqueOffsetMap = new LinkedHashMap<OneDoFJoint, YoDouble>();

      for (int i = 0; i < oneDoFJoints.size(); i++)
      {
         final OneDoFJoint oneDoFJoint = oneDoFJoints.get(i);
         final YoDouble torqueOffset = new YoDouble("tauOffset_" + oneDoFJoint.getName(), registry);

         torqueOffsetMap.put(oneDoFJoint, torqueOffset);
      }

   }

   @Override
   public void setForceSensorDataHolderForController(ForceSensorDataHolderReadOnly forceSensorDataHolderForController)
   {
      if(drcOutputWriter != null)
      {
         drcOutputWriter.setForceSensorDataHolderForController(forceSensorDataHolderForController);
      }
   }

   @Override
   public YoVariableRegistry getControllerYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public void subtractTorqueOffset(OneDoFJoint oneDoFJoint, double torqueOffset)
   {
      YoDouble torqueOffsetVariable = torqueOffsetMap.get(oneDoFJoint);
      torqueOffsetVariable.sub(torqueOffset);
   }
}
