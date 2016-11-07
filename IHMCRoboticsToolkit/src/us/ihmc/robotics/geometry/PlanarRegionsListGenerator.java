package us.ihmc.robotics.geometry;

import java.util.ArrayList;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.Axis;

public class PlanarRegionsListGenerator
{
   private PlanarRegionsList planarRegionsList = new PlanarRegionsList(new ArrayList<PlanarRegion>());

   private final RigidBodyTransformGenerator transformGenerator = new RigidBodyTransformGenerator();

   public void addCubeReferencedAtCenter(double lengthX, double widthY, double heightZ)
   {
      RigidBodyTransformGenerator transformGeneratorTwo = new RigidBodyTransformGenerator(transformGenerator);

      transformGeneratorTwo.set(transformGenerator);
      transformGeneratorTwo.translate(0.0, 0.0, heightZ / 2.0);
      addRectangle(transformGeneratorTwo, lengthX, widthY);
      transformGeneratorTwo.translate(0.0, 0.0, -heightZ);
      addRectangle(transformGeneratorTwo, lengthX, widthY);

      transformGeneratorTwo.set(transformGenerator);
      transformGeneratorTwo.rotate(Math.PI / 2.0, Axis.Y);
      transformGeneratorTwo.translate(0.0, 0.0, lengthX / 2.0);
      addRectangle(transformGeneratorTwo, heightZ, widthY);
      transformGeneratorTwo.translate(0.0, 0.0, -lengthX);
      addRectangle(transformGeneratorTwo, heightZ, widthY);

      transformGeneratorTwo.set(transformGenerator);
      transformGeneratorTwo.rotate(Math.PI / 2.0, Axis.X);
      transformGeneratorTwo.translate(0.0, 0.0, widthY / 2.0);
      addRectangle(transformGeneratorTwo, lengthX, heightZ);
      transformGeneratorTwo.translate(0.0, 0.0, -widthY);
      addRectangle(transformGeneratorTwo, lengthX, heightZ);
   }

   public void addCubeReferencedAtBottomMiddle(double lengthX, double widthY, double heightZ)
   {
      translate(0.0, 0.0, heightZ / 2.0);
      addCubeReferencedAtCenter(lengthX, widthY, heightZ);
      translate(0.0, 0.0, -heightZ / 2.0);
   }

   public void addRectangle(double lengthX, double widthY)
   {
      ConvexPolygon2d rectangle = createRectanglePolygon(lengthX, widthY);
      addPolygon(rectangle);
   }

   public void addPolygon(ConvexPolygon2d polygon)
   {
      PlanarRegion planarRegion = new PlanarRegion(transformGenerator.getRigidBodyTransformCopy(), polygon);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   public void addPolygons(ArrayList<ConvexPolygon2d> polygons)
   {
      PlanarRegion planarRegion = new PlanarRegion(transformGenerator.getRigidBodyTransformCopy(), polygons);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   private void addRectangle(RigidBodyTransformGenerator transformGenerator, double lengthX, double widthY)
   {
      ConvexPolygon2d rectangle = createRectanglePolygon(lengthX, widthY);
      PlanarRegion planarRegion = new PlanarRegion(transformGenerator.getRigidBodyTransformCopy(), rectangle);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   private static ConvexPolygon2d createRectanglePolygon(double lengthX, double widthY)
   {
      ConvexPolygon2d convexPolygon = new ConvexPolygon2d();
      convexPolygon.addVertex(lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, -widthY / 2.0);
      convexPolygon.addVertex(lengthX / 2.0, -widthY / 2.0);
      return convexPolygon;
   }

   public void translate(double x, double y, double z)
   {
      transformGenerator.translate(x, y, z);
   }

   public void translate(Vector3d translationVector)
   {
      transformGenerator.translate(translationVector);
   }

   public void identity()
   {
      transformGenerator.identity();
   }

   public void rotateEuler(Vector3d eulerAngles)
   {
      transformGenerator.rotateEuler(eulerAngles);
   }

   public void rotate(Matrix3d rotation)
   {
      transformGenerator.rotate(rotation);
   }
}
