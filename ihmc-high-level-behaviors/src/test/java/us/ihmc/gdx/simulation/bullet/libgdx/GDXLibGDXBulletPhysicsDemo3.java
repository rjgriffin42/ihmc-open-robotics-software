package us.ihmc.gdx.simulation.bullet.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import imgui.internal.ImGui;
import imgui.type.ImBoolean;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.yawPitchRoll.YawPitchRoll;
import us.ihmc.gdx.Lwjgl3ApplicationAdapter;
import us.ihmc.gdx.simulation.environment.GDXBulletPhysicsManager;
import us.ihmc.gdx.tools.GDXModelPrimitives;
import us.ihmc.gdx.tools.GDXTools;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;

import java.util.Random;

public class GDXLibGDXBulletPhysicsDemo3
{
   private final GDXImGuiBasedUI baseUI = new GDXImGuiBasedUI(getClass(),
                                                              "ihmc-open-robotics-software",
                                                              "ihmc-high-level-behaviors/src/test/resources");
   private final GDXBulletPhysicsManager bulletPhysicsManager = new GDXBulletPhysicsManager();
   private final ImBoolean simulate = new ImBoolean(false);
   private final Vector3 tempVector = new Vector3();

   public GDXLibGDXBulletPhysicsDemo3()
   {
      baseUI.launchGDXApplication(new Lwjgl3ApplicationAdapter()
      {
         @Override
         public void create()
         {
            baseUI.create();

            bulletPhysicsManager.create();

            float groundSizeX = 1000.0f;
            float groundSizeY = 1000.0f;
            float groundSizeZ = 0.5f;
            ModelInstance groundModelInstance = GDXModelPrimitives.createBox(groundSizeX, groundSizeY, groundSizeZ, Color.DARK_GRAY);
            RigidBodyTransform groundTransform = new RigidBodyTransform(new YawPitchRoll(0.0,
                                                                                         Math.toRadians(15.0),
                                                                                         0.0),
                                                                        new Point3D(0.0, 0.0, -0.0));
            groundTransform.appendTranslation(0.0, 0.0, -0.25);
            GDXTools.toGDX(groundTransform, groundModelInstance.transform);
            tempVector.set(groundSizeX / 2.0f, groundSizeY / 2.0f, groundSizeZ / 2.0f);
            btBoxShape groundBoxShape = new btBoxShape(tempVector);
            bulletPhysicsManager.addStaticObject(groundBoxShape, groundModelInstance.transform);
            baseUI.get3DSceneManager().addModelInstance(groundModelInstance);

            int numberOfBlocks = 6;
            Random random = new Random(1886L);
            double x = 0.0;
            double y = 0.0;
            for (int i = 0; i < numberOfBlocks; i++)
            {
               float boxSizeX = 0.1f;
               float boxSizeY = 0.08f;
               float boxSizeZ = 0.1f;
               x += 0.02;
               y = 0.0;
               double z = boxSizeZ * 1.05 * (i + 1.0);
               ModelInstance boxModelInstance = GDXModelPrimitives.createBox(boxSizeX, boxSizeY, boxSizeZ, Color.RED);
               double yaw = 0.0;
               double pitch = RandomNumbers.nextDouble(random, -Math.PI / 90.0, Math.PI / 90.0);
               double roll = RandomNumbers.nextDouble(random, -Math.PI / 90.0, Math.PI / 90.0);
               RigidBodyTransform boxTransform = new RigidBodyTransform(new YawPitchRoll(yaw, pitch, roll), new Point3D(x, y, z));
               GDXTools.toGDX(boxTransform, boxModelInstance.transform);

               tempVector.set(boxSizeX / 2.0f, boxSizeY / 2.0f, boxSizeZ / 2.0f);
               btBoxShape boxShape = new btBoxShape(tempVector);
               float mass = 0.2f;
               bulletPhysicsManager.addRigidBody(boxShape, mass, boxModelInstance.transform);
               baseUI.get3DSceneManager().addModelInstance(boxModelInstance);
            }

            baseUI.getImGuiPanelManager().addPanel("libGDX Bullet Physics", this::renderImGuiWidgets);
         }

         @Override
         public void render()
         {
            if (simulate.get())
            {
               bulletPhysicsManager.simulate(Gdx.graphics.getDeltaTime());
            }

            baseUI.renderBeforeOnScreenUI();
            baseUI.renderEnd();
         }

         private void renderImGuiWidgets()
         {
            ImGui.checkbox("Simulate", simulate);
         }

         @Override
         public void dispose()
         {
            bulletPhysicsManager.destroy();
            baseUI.dispose();
         }
      });
   }

   public static void main(String[] args)
   {
      new GDXLibGDXBulletPhysicsDemo3();
   }
}
