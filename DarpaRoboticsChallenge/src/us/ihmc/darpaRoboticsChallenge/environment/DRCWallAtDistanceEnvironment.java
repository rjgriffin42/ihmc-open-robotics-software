package us.ihmc.darpaRoboticsChallenge.environment;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;

import com.yobotics.simulationconstructionset.ExternalForcePoint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.util.environments.SelectableObjectListener;
import com.yobotics.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import com.yobotics.simulationconstructionset.util.ground.TerrainObject3D;

public class DRCWallAtDistanceEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D combinedTerrainObject;

   private final double wallDistance;

   /**
    * This world is for lidar tests.
    *
    * @param wallDistance
    */
   public DRCWallAtDistanceEnvironment(double wallDistance)
   {
      combinedTerrainObject = new CombinedTerrainObject3D(getClass().getSimpleName());

      this.wallDistance = wallDistance;
      
      combinedTerrainObject.addTerrainObject(setupWall("WallyTheWall"));
      combinedTerrainObject.addTerrainObject(setupGround());
   }
   
   private CombinedTerrainObject3D setupWall(String name)
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D(name);

      double xStart = wallDistance;
      double wallMinY = -1000;
      double wallMaxY = 1000;
      double wallZStart = -1000;
      double wallZEnd = 1000;
      
      AppearanceDefinition appearance = YoAppearance.Green();
      appearance.setTransparency(0.25);
      
      combinedTerrainObject.addBox(xStart, wallMinY, xStart + 0.1, wallMaxY, wallZStart, wallZEnd, appearance);
      return combinedTerrainObject;
   }
   
   private CombinedTerrainObject3D setupGround()
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D("Ground");
      combinedTerrainObject.addBox(-0.2, -0.5, 0.2, 0.5, -0.05, 0.0, YoAppearance.Gold());      
      return combinedTerrainObject;
   }

   @Override
   public TerrainObject3D getTerrainObject3D()
   {
      return combinedTerrainObject;
   }

   @Override
   public List<Robot> getEnvironmentRobots()
   {
      return new ArrayList<Robot>();
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
