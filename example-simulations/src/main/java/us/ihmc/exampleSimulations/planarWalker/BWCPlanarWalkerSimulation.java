package us.ihmc.exampleSimulations.planarWalker;

import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.scs2.SimulationConstructionSet2;
import us.ihmc.scs2.definition.terrain.TerrainObjectDefinition;
import us.ihmc.scs2.simulation.parameters.ContactPointBasedContactParameters;
import us.ihmc.scs2.simulation.physicsEngine.PhysicsEngineFactory;
import us.ihmc.scs2.simulation.robot.Robot;
import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;

public class BWCPlanarWalkerSimulation
{
   public BWCPlanarWalkerSimulation()
   {
      int simTicksPerControlTick = 3;


      ContactPointBasedContactParameters contactParameters = ContactPointBasedContactParameters.defaultParameters();
//      contactParameters.setKxy(5000.0);
//      contactParameters.setBxy(500.0);
      PhysicsEngineFactory physicsEngineFactory = PhysicsEngineFactory.newContactPointBasedPhysicsEngineFactory(contactParameters);
//      PhysicsEngineFactory physicsEngineFactory = PhysicsEngineFactory.newContactPointBasedPhysicsEngineFactory();
      SimulationConstructionSet2 scs = new SimulationConstructionSet2("bloop", physicsEngineFactory);
      scs.setBufferRecordTickPeriod(simTicksPerControlTick);
//      scs.getGravity().setToZero();

      BWCPlanarWalkingRobotDefinition robotDefinition = new BWCPlanarWalkingRobotDefinition();
      Robot robot = new Robot(robotDefinition, scs.getInertialFrame());
      scs.addRobot(robot);
      scs.addTerrainObject(new SlopeGroundDefinition(0.0));

      // set up the controller robot that has convenience methods for us to do control things with.
      BWCPlanarWalkingRobot controllerRobot = new BWCPlanarWalkingRobot(robot, scs.getTime());
      // create the robot controller
      BWCPlanarWalkingController controller = new BWCPlanarWalkingController(controllerRobot, RobotSide.LEFT);
      // set the controller to control the robot.
      robot.addThrottledController(controller, scs.getDT() * simTicksPerControlTick);

      scs.startSimulationThread();
      scs.simulate();
   }

   public static void main(String[] args)
   {
      new BWCPlanarWalkerSimulation();
   }
}
