package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import java.util.ArrayList;

import us.ihmc.controlFlow.ControlFlowGraph;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;

public class ControlFlowGraphExecutorController implements RobotController
{
   private final String name = getClass().getSimpleName();

   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final ArrayList<ControlFlowGraph> controlFlowGraphs = new ArrayList<ControlFlowGraph>();

   public ControlFlowGraphExecutorController()
   {
   }

   public ControlFlowGraphExecutorController(ControlFlowGraph controlFlowGraph)
   {
      addControlFlowGraph(controlFlowGraph);
   }

   public void addControlFlowGraph(ControlFlowGraph controlFlowGraph)
   {
      this.controlFlowGraphs.add(controlFlowGraph);
   }

   public void initialize()
   {
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {
      for (ControlFlowGraph controlFlowGraph : controlFlowGraphs)
      {
         controlFlowGraph.startComputation();
      }

      for (ControlFlowGraph controlFlowGraph : controlFlowGraphs)
      {
         controlFlowGraph.waitUntilComputationIsDone();
      }
   }
}
