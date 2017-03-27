package us.ihmc.atlas.environments;

import java.util.List;

import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationConstructionSetTools.util.environments.DefaultCommonAvatarEnvironment;
import us.ihmc.simulationConstructionSetTools.util.environments.SelectableObjectListener;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;

public class DarpaRoboticsChallengeTrialsWalkingEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D combinedTerrainObject3D;


   public DarpaRoboticsChallengeTrialsWalkingEnvironment()
   {
      combinedTerrainObject3D = new CombinedTerrainObject3D(getClass().getSimpleName());
      combinedTerrainObject3D.addTerrainObject(DefaultCommonAvatarEnvironment.setUpPath4DRCTrialsTrainingWalkingCourse("Path 4 Walking Course"));
      combinedTerrainObject3D.addTerrainObject(DefaultCommonAvatarEnvironment.setUpGround("Ground"));
   }

   @Override
   public TerrainObject3D getTerrainObject3D()
   {
      return combinedTerrainObject3D;
   }

   @Override
   public List<? extends Robot> getEnvironmentRobots()
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
