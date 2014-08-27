package us.ihmc.darpaRoboticsChallenge.controllers;

import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.environments.ContactableToroidRobot;
import com.yobotics.simulationconstructionset.util.math.functionGenerator.YoFunctionGenerator;
import com.yobotics.simulationconstructionset.util.math.functionGenerator.YoFunctionGeneratorMode;

public class SteeringWheelDisturbanceController implements RobotController
{  
   private final String NAME = "SteeringWheelDisturbanceController";
   private final ContactableToroidRobot robot;
   
   private YoVariableRegistry registry = new YoVariableRegistry(NAME);
   private YoFunctionGenerator fnGenerator;

   public SteeringWheelDisturbanceController(ContactableToroidRobot robot, YoFunctionGeneratorMode mode)
   {
      fnGenerator = new YoFunctionGenerator(NAME + "FunctionGenerator", robot.getYoTime(), registry);
      this.robot = robot;
      fnGenerator.setMode(mode);
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
      return NAME;
   }

   public String getDescription()
   {      
      return "Perturb steering wheel based on a YoFunctionGenerator";
   }

   public void doControl()
   {
      robot.setTau(fnGenerator.getValue());
   }

}
