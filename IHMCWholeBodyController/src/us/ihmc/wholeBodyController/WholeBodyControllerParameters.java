package us.ihmc.wholeBodyController;

import us.ihmc.commonWalkingControlModules.configurations.ICPWithTimeFreezingPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.humanoidRobotics.footstep.footstepGenerator.FootstepPlanningParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;

public interface WholeBodyControllerParameters
{
   public double getControllerDT();

   /**
    * Returns the parameters used to create Footstep Plans.
    */
   default public FootstepPlanningParameters getFootstepPlanningParameters()
   {
      return null;
   }

   public StateEstimatorParameters getStateEstimatorParameters();

   public ICPWithTimeFreezingPlannerParameters getCapturePointPlannerParameters();

	public WalkingControllerParameters getWalkingControllerParameters();

	public RobotContactPointParameters getContactPointParameters();

   public DRCRobotSensorInformation getSensorInformation();
}
