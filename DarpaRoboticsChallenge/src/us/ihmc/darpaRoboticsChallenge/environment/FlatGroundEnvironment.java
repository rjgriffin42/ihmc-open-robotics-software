package us.ihmc.darpaRoboticsChallenge.environment;

import java.util.List;

import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationconstructionset.util.environments.SelectableObjectListener;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;

public class FlatGroundEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D flatGround;

   public FlatGroundEnvironment()
   {
      flatGround = DRCDemo01NavigationEnvironment.setUpGround("Ground");
   }
   
   @Override
   public TerrainObject3D getTerrainObject3D()
   {
      return flatGround;
   }

   @Override
   public List<Robot> getEnvironmentRobots()
   {
      return null;
   }

   @Override
   public void createAndSetContactControllerToARobot()
   {
   }

   @Override
   public void addContactPoints(List<? extends ExternalForcePoint> externalForcePoints)
   {
   }

   @Override
   public void addSelectableListenerToSelectables(SelectableObjectListener selectedListener)
   {
   }
}
