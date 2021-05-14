package us.ihmc.robotDataVisualizer.visualizer;

import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public interface SCSVisualizerStateListener
{
   public void starting(SimulationConstructionSet scs, Robot robot, YoRegistry registry);
}
