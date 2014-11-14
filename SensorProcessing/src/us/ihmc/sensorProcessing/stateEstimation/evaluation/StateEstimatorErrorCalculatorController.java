package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import javax.vecmath.Vector3d;

import us.ihmc.sensorProcessing.stateEstimation.StateEstimator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.robotController.RobotController;

public class StateEstimatorErrorCalculatorController implements RobotController
{
   private final Vector3d gravitationalAcceleration;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());   
   private final StateEstimatorErrorCalculator composableStateEstimatorEvaluatorErrorCalculator;

   public StateEstimatorErrorCalculatorController(StateEstimator orientationEstimator,
           Robot robot, Joint estimationJoint, boolean assumePerfectIMU, boolean useSimplePelvisPositionEstimator)
   {      
      this.gravitationalAcceleration = new Vector3d();
      robot.getGravity(gravitationalAcceleration);

      this.composableStateEstimatorEvaluatorErrorCalculator = new StateEstimatorErrorCalculator(robot, estimationJoint, orientationEstimator, assumePerfectIMU, useSimplePelvisPositionEstimator, registry);
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
      return registry.getName();
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {      
      composableStateEstimatorEvaluatorErrorCalculator.computeErrors();
   }
}
