package us.ihmc.quadrupedRobotics.factories;

import us.ihmc.SdfLoader.models.FullQuadrupedRobotModel;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.simulationconstructionset.robotController.RobotController;

public interface QuadrupedRobotControllerFactory
{
   public RobotController createRobotController();

   public void setControlDt(double controlDt);

   public void setFullRobotModel(FullQuadrupedRobotModel fullRobotModel);

   public void setGlobalDataProducer(GlobalDataProducer globalDataProducer);
}
