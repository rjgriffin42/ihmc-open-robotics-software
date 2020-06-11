package us.ihmc.simulationConstructionSetTools.util.environments;

import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.simulationConstructionSetTools.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationConstructionSetTools.util.ground.PlanarRegionTerrainObject;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;

import java.util.List;

/**
 * @author Doug Stephen <a href="mailto:dstephen@ihmc.us">(dstephen@ihmc.us)</a>
 */
public class PlanarRegionsListDefinedEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D combinedTerrainObject;
   private final String environmentName;
   private final PlanarRegionsList[] planarRegionsLists;
   private final AppearanceDefinition[] appearances;

   public PlanarRegionsListDefinedEnvironment(PlanarRegionsList planarRegionsList, double allowablePenetrationThickness, boolean generateGroundPlane)
   {
      this(PlanarRegionsListDefinedEnvironment.class.getSimpleName(), planarRegionsList, allowablePenetrationThickness, generateGroundPlane);
   }

   public PlanarRegionsListDefinedEnvironment(String name,
                                              PlanarRegionsList planarRegionsList,
                                              double allowablePenetrationThickness,
                                              boolean generateGroundPlane)
   {
      this(name, new PlanarRegionsList[] {planarRegionsList}, null, allowablePenetrationThickness, generateGroundPlane);
   }

   public PlanarRegionsListDefinedEnvironment(String name,
                                              PlanarRegionsList planarRegionsList,
                                              AppearanceDefinition appearance,
                                              double allowablePenetrationThickness,
                                              boolean generateGroundPlane)
   {
      this(name, new PlanarRegionsList[] {planarRegionsList}, new AppearanceDefinition[] {appearance}, allowablePenetrationThickness, generateGroundPlane);
   }

   public PlanarRegionsListDefinedEnvironment(String environmentName,
                                              PlanarRegionsList[] planarRegionsList,
                                              AppearanceDefinition[] appearances,
                                              double allowablePenetrationThickness,
                                              boolean generateGroundPlane)
   {
      this.environmentName = environmentName;
      this.planarRegionsLists = planarRegionsList;

      if(appearances == null || appearances.length != planarRegionsList.length)
      {
         this.appearances = null;
      }
      else
      {
         this.appearances = appearances;
      }

      combinedTerrainObject = createCombinedTerrainObjectFromPlanarRegionsList(this.environmentName, allowablePenetrationThickness);

      if (generateGroundPlane)
      {
         combinedTerrainObject.addTerrainObject(DefaultCommonAvatarEnvironment.setUpGround("Ground"));
      }
   }

   private CombinedTerrainObject3D createCombinedTerrainObjectFromPlanarRegionsList(String environmentName, double allowablePenetrationThickness)
   {
      CombinedTerrainObject3D combinedTerrainObject3D = new CombinedTerrainObject3D(environmentName);

      for (int i = 0; i < planarRegionsLists.length; i++)
      {
         PlanarRegionsList planarRegionsList = planarRegionsLists[i];
         for (int j = 0; j < planarRegionsList.getNumberOfPlanarRegions(); j++)
         {
            PlanarRegion planarRegion = planarRegionsList.getPlanarRegion(j);

            if(appearances == null)
            {
               combinedTerrainObject3D.addTerrainObject(new PlanarRegionTerrainObject(planarRegion, allowablePenetrationThickness));
            }
            else
            {
//               LogTools.info("Applying appearance {} of type {}", i, appearances[i].getClass());
               combinedTerrainObject3D.addTerrainObject(new PlanarRegionTerrainObject(planarRegion, allowablePenetrationThickness, appearances[i]));
            }
         }
      }

      return combinedTerrainObject3D;
   }

   public CombinedTerrainObject3D getCombinedTerrainObject3D()
   {
      return combinedTerrainObject;
   }
   
   @Override
   public TerrainObject3D getTerrainObject3D()
   {
      return combinedTerrainObject;
   }

   @Override
   public List<? extends Robot> getEnvironmentRobots()
   {
      LogTools.warn("Environment robots currently unimplemented for this class");
      return null;
   }

   @Override
   public void createAndSetContactControllerToARobot()
   {
      LogTools.warn("Contact points currently unimplemented for this class");
   }

   @Override
   public void addContactPoints(List<? extends ExternalForcePoint> externalForcePoints)
   {
      LogTools.warn("Contact points currently unimplemented for this class");
   }

   @Override
   public void addSelectableListenerToSelectables(SelectableObjectListener selectedListener)
   {

   }
}
