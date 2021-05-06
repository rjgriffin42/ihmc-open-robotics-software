package us.ihmc.gdx.simulation.environment.object.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import us.ihmc.euclid.shape.primitives.Box3D;
import us.ihmc.euclid.shape.primitives.Sphere3D;
import us.ihmc.gdx.simulation.environment.object.GDXEnvironmentObject;
import us.ihmc.gdx.tools.GDXModelLoader;
import us.ihmc.gdx.tools.GDXModelPrimitives;
import us.ihmc.gdx.tools.GDXTools;
import us.ihmc.graphicsDescription.appearance.YoAppearance;

import java.util.concurrent.atomic.AtomicInteger;

public class GDXLabFloorObject extends GDXEnvironmentObject
{
   private static final AtomicInteger INDEX = new AtomicInteger();

   public GDXLabFloorObject()
   {
      Model realisticModel = GDXModelLoader.loadG3DModel("labFloor/LabFloor.g3dj");

      double sizeX = 0.178334;
      double sizeY = 0.178334;
      double sizeZ = 0.178334;
      Sphere3D boundingSphere = new Sphere3D(100.0);
      Box3D collisionBox = new Box3D(sizeX, sizeY, sizeZ);
      Model collisionGraphic = GDXModelPrimitives.buildModel(meshBuilder ->
      {
         Color color = GDXTools.toGDX(YoAppearance.LightSkyBlue());
         meshBuilder.addBox((float) sizeX, (float) sizeY, (float) sizeZ, color);
         meshBuilder.addMultiLineBox(collisionBox.getVertices(), 0.01, color); // some can see it better
      }, "collisionModel" + INDEX.getAndIncrement());
      collisionGraphic.materials.get(0).set(new BlendingAttribute(true, 0.4f));
      create(realisticModel, boundingSphere, collisionBox, collisionBox::isPointInside, collisionGraphic);
   }

   @Override
   public GDXLabFloorObject duplicate()
   {
      return new GDXLabFloorObject();
   }
}
