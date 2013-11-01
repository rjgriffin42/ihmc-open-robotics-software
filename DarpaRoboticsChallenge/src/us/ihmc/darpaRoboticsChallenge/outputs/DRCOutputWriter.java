package us.ihmc.darpaRoboticsChallenge.outputs;

import us.ihmc.SdfLoader.SDFFullRobotModel;

import com.yobotics.simulationconstructionset.robotController.RawOutputWriter;
import com.yobotics.simulationconstructionset.robotController.RobotControlElement;

public interface DRCOutputWriter extends RobotControlElement
{
   public abstract void writeAfterController(long timestamp);
   public abstract void writeAfterEstimator(long timestamp);
   
   public abstract void writeAfterSimulationTick();
   public abstract void setFullRobotModel(SDFFullRobotModel controllerModel);
   public abstract void setEstimatorModel(SDFFullRobotModel estimatorModel);
}
