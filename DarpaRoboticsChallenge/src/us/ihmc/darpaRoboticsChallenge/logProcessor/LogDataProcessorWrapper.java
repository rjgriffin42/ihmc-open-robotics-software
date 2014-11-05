package us.ihmc.darpaRoboticsChallenge.logProcessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import us.ihmc.darpaRoboticsChallenge.DRCControllerThread;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

import com.yobotics.simulationconstructionset.DataProcessingFunction;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.scripts.Script;

public class LogDataProcessorWrapper implements DataProcessingFunction, Script
{
   private final YoVariableRegistry logDataProcessorRegistry = new YoVariableRegistry("LogDataProcessor");

   private final List<LogDataProcessorFunction> logDataProcessorFunctions = new ArrayList<LogDataProcessorFunction>();

   private final List<YoVariable<?>> varsToSave = new ArrayList<>();
   private final LinkedHashMap<YoVariable<?>, Double> yoVarsToDoublesMap = new LinkedHashMap<>();

   private boolean isControllerTick = true;

   public LogDataProcessorWrapper(SimulationConstructionSet scs)
   {
      scs.addYoVariableRegistry(logDataProcessorRegistry);
      scs.addScript(this);
      
      YoVariable<?> controllerTimerCount = scs.getVariable(DRCControllerThread.class.getSimpleName(), "controllerTimerCount");
      controllerTimerCount.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            isControllerTick = true;
         }
      });
   }

   @Override
   public void initializeProcessing()
   {
      saveYoVariablesAsDoubles();
   }

   @Override
   public void processData()
   {
      retrieveYoVariablesFromDoubles();
      if (isControllerTick)
      {
         isControllerTick = false;
         processDataAtControllerRate();
      }
      processDataAtStateEstimatorRate();
      saveYoVariablesAsDoubles();
   }

   @Override
   public void doScript(double t)
   {
      if (isControllerTick)
      {
         isControllerTick = false;
         processDataAtControllerRate();
      }
      processDataAtStateEstimatorRate();
   }

   private void processDataAtControllerRate()
   {
      for (int i = 0; i < logDataProcessorFunctions.size(); i++)
      {
         logDataProcessorFunctions.get(i).processDataAtControllerRate();
      }
   }

   private void processDataAtStateEstimatorRate()
   {
      for (int i = 0; i < logDataProcessorFunctions.size(); i++)
      {
         logDataProcessorFunctions.get(i).processDataAtStateEstimatorRate();
      }
   }

   public void addLogDataProcessor(LogDataProcessorFunction logDataProcessorFunction)
   {
      logDataProcessorFunctions.add(logDataProcessorFunction);
      logDataProcessorRegistry.addChild(logDataProcessorFunction.getYoVariableRegistry());
      varsToSave.addAll(logDataProcessorFunction.getYoVariableRegistry().getAllVariables());
   }

   public void saveYoVariablesAsDoubles()
   {
      for (int i = 0; i < varsToSave.size(); i++)
      {
         YoVariable<?> currentYoVariable = varsToSave.get(i);
         yoVarsToDoublesMap.put(currentYoVariable, currentYoVariable.getValueAsDouble());
      }
   }

   public void retrieveYoVariablesFromDoubles()
   {
      for (int i = 0; i < varsToSave.size(); i++)
      {
         YoVariable<?> currentYoVariable = varsToSave.get(i);
         Double previousValueAsDouble = yoVarsToDoublesMap.get(currentYoVariable);
         currentYoVariable.setValueFromDouble(previousValueAsDouble);
      }
   }
}
