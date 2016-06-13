package us.ihmc.darpaRoboticsChallenge.environment;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceTexture;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.environments.SelectableObjectListener;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;

import java.awt.*;
import java.util.List;

public class ColoredBallEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D combinedTerrainObject;
   public static final double BALL_RADIUS = 0.0762;

   public ColoredBallEnvironment()
   {
      combinedTerrainObject = new CombinedTerrainObject3D("Colored balls");
      YoAppearanceTexture texture = new YoAppearanceTexture("Textures/gridGroundProfile.png");
      combinedTerrainObject.addBox(-10.0, -10.0, 10.0, 10.0, -0.05, 0.0, texture);

      int color = Color.HSBtoRGB(80.0f / 180.0f, 200.0f / 256.0f, 200.0f / 256.0f);
      combinedTerrainObject.addSphere(1.5, 0.0, 1.0, BALL_RADIUS, YoAppearance.RGBColorFromHex(color));
   }

   @Override public TerrainObject3D getTerrainObject3D()
   {
      return combinedTerrainObject;
   }

   @Override public List<? extends Robot> getEnvironmentRobots()
   {
      return null;
   }

   @Override public void createAndSetContactControllerToARobot()
   {

   }

   @Override public void addContactPoints(List<? extends ExternalForcePoint> externalForcePoints)
   {

   }

   @Override public void addSelectableListenerToSelectables(SelectableObjectListener selectedListener)
   {

   }
}
