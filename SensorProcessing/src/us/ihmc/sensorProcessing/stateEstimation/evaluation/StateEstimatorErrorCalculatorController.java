package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import javax.vecmath.Vector3d;

import us.ihmc.sensorProcessing.stateEstimation.StateEstimator;

import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;

public class StateEstimatorErrorCalculatorController implements RobotController
{
   private final Vector3d gravitationalAcceleration;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());   
   private final StateEstimatorErrorCalculator composableStateEstimatorEvaluatorErrorCalculator;

   public StateEstimatorErrorCalculatorController(StateEstimator orientationEstimator,
           Robot robot, Joint estimationJoint, double controlDT)
   {      
      this.gravitationalAcceleration = new Vector3d();
      robot.getGravity(gravitationalAcceleration);

      this.composableStateEstimatorEvaluatorErrorCalculator = new StateEstimatorErrorCalculator(robot, estimationJoint, orientationEstimator, registry);
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
