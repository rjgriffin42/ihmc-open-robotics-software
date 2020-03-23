package us.ihmc.robotics.physics;

import java.util.List;

import us.ihmc.mecano.multiBodySystem.interfaces.MultiBodySystemBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class PhysicsEngineRobotData implements CollidableHolder
{
   private final RigidBodyBasics rootBody;
   private final MultiBodySystemStateWriter robotInitialStateWriter;

   private final YoVariableRegistry robotRegistry;
   private final MultiBodySystemBasics multiBodySystem;
   private final List<Collidable> collidables;

   // Specific to the type of engine used:
   private final SingleRobotForwardDynamicsPlugin forwardDynamicsPlugin;

   public PhysicsEngineRobotData(String robotName, RigidBodyBasics rootBody, MultiBodySystemStateWriter robotInitialStateWriter,
                                 MultiBodySystemStateWriter controllerOutputWriter, RobotCollisionModel robotCollisionModel)
   {
      this.rootBody = rootBody;
      this.robotInitialStateWriter = robotInitialStateWriter;

      robotRegistry = new YoVariableRegistry(robotName);
      multiBodySystem = MultiBodySystemBasics.toMultiBodySystemBasics(rootBody);
      if (robotInitialStateWriter != null)
         robotInitialStateWriter.setMultiBodySystem(multiBodySystem);

      collidables = robotCollisionModel.getRobotCollidables(multiBodySystem);

      forwardDynamicsPlugin = new SingleRobotForwardDynamicsPlugin(multiBodySystem, controllerOutputWriter);
   }

   public void initialize()
   {
      robotInitialStateWriter.write();
      updateFrames();
   }

   public void updateCollidableBoundingBoxes()
   {
      collidables.forEach(Collidable::updateBoundingBox);
   }

   public void updateFrames()
   {
      rootBody.updateFramesRecursively();
   }

   @Override
   public List<Collidable> getCollidables()
   {
      return collidables;
   }

   public MultiBodySystemBasics getMultiBodySystem()
   {
      return multiBodySystem;
   }

   public SingleRobotForwardDynamicsPlugin getForwardDynamicsPlugin()
   {
      return forwardDynamicsPlugin;
   }

   public RigidBodyBasics getRootBody()
   {
      return rootBody;
   }

   public YoVariableRegistry getRobotRegistry()
   {
      return robotRegistry;
   }
}