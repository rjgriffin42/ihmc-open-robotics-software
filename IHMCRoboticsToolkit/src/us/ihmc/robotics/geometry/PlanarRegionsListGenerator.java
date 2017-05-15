package us.ihmc.robotics.geometry;

import java.util.ArrayList;

import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.robotics.Axis;

public class PlanarRegionsListGenerator
{
   private PlanarRegionsList planarRegionsList = new PlanarRegionsList(new ArrayList<PlanarRegion>());

   private final RigidBodyTransformGenerator transformGenerator = new RigidBodyTransformGenerator();

   private int id = 0;
   
   public void addCubeReferencedAtCenter(double lengthX, double widthY, double heightZ)
   {
      RigidBodyTransformGenerator transformGeneratorTwo = new RigidBodyTransformGenerator(transformGenerator);

      transformGeneratorTwo.set(transformGenerator);
      transformGeneratorTwo.translate(0.0, 0.0, heightZ / 2.0);
      addRectangle(transformGeneratorTwo, lengthX, widthY);
      transformGeneratorTwo.translate(0.0, 0.0, -heightZ);
      transformGeneratorTwo.rotate(Math.PI, Axis.Y);
      addRectangle(transformGeneratorTwo, lengthX, widthY);
      transformGeneratorTwo.rotate(Math.PI, Axis.Y);

      transformGeneratorTwo.set(transformGenerator);
      transformGeneratorTwo.rotate(Math.PI / 2.0, Axis.Y);
      transformGeneratorTwo.translate(0.0, 0.0, lengthX / 2.0);
      addRectangle(transformGeneratorTwo, heightZ, widthY);
      transformGeneratorTwo.translate(0.0, 0.0, -lengthX);
      transformGeneratorTwo.rotate(Math.PI, Axis.Y);
      addRectangle(transformGeneratorTwo, heightZ, widthY);
      transformGeneratorTwo.rotate(Math.PI, Axis.Y);

      transformGeneratorTwo.set(transformGenerator);
      transformGeneratorTwo.rotate(Math.PI / 2.0, Axis.X);
      transformGeneratorTwo.translate(0.0, 0.0, widthY / 2.0);
      addRectangle(transformGeneratorTwo, lengthX, heightZ);
      transformGeneratorTwo.translate(0.0, 0.0, -widthY);
      transformGeneratorTwo.rotate(Math.PI, Axis.X);
      addRectangle(transformGeneratorTwo, lengthX, heightZ);
      transformGeneratorTwo.rotate(Math.PI, Axis.X);
   }

   public void addCubeReferencedAtBottomMiddle(double lengthX, double widthY, double heightZ)
   {
      translate(0.0, 0.0, heightZ / 2.0);
      addCubeReferencedAtCenter(lengthX, widthY, heightZ);
      translate(0.0, 0.0, -heightZ / 2.0);
   }
   
   public void addCubeReferencedAtBottomNegativeXEdgeCenter(double lengthX, double widthY, double heightZ)
   {
      translate(-lengthX / 2.0, 0.0, heightZ / 2.0);
      addCubeReferencedAtCenter(lengthX, widthY, heightZ);
      translate(lengthX / 2.0, 0.0, -heightZ / 2.0);
   }

   public void addRectangle(double lengthX, double widthY)
   {
      ConvexPolygon2D rectangle = createRectanglePolygon(lengthX, widthY);
      addPolygon(rectangle);
   }

   public void addPolygon(ConvexPolygon2D polygon)
   {
      PlanarRegion planarRegion = new PlanarRegion(transformGenerator.getRigidBodyTransformCopy(), polygon);
      planarRegion.setRegionId(id++);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   public void addPolygons(ArrayList<ConvexPolygon2D> polygons)
   {
      PlanarRegion planarRegion = new PlanarRegion(transformGenerator.getRigidBodyTransformCopy(), polygons);
      planarRegion.setRegionId(id++);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   private void addRectangle(RigidBodyTransformGenerator transformGenerator, double lengthX, double widthY)
   {
      ConvexPolygon2D rectangle = createRectanglePolygon(lengthX, widthY);
      PlanarRegion planarRegion = new PlanarRegion(transformGenerator.getRigidBodyTransformCopy(), rectangle);
      planarRegion.setRegionId(id++);
      planarRegionsList.addPlanarRegion(planarRegion);
   }

   private static ConvexPolygon2D createRectanglePolygon(double lengthX, double widthY)
   {
      ConvexPolygon2D convexPolygon = new ConvexPolygon2D();
      convexPolygon.addVertex(lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, -widthY / 2.0);
      convexPolygon.addVertex(lengthX / 2.0, -widthY / 2.0);
      convexPolygon.update();
      return convexPolygon;
   }

   public void translate(double x, double y, double z)
   {
      transformGenerator.translate(x, y, z);
   }

   public void translate(Vector3D translationVector)
   {
      transformGenerator.translate(translationVector);
   }

   public void identity()
   {
      transformGenerator.identity();
   }

   public void rotateEuler(Vector3D eulerAngles)
   {
      transformGenerator.rotateEuler(eulerAngles);
   }

   public void rotate(RotationMatrix rotation)
   {
      transformGenerator.rotate(rotation);
   }

   public void rotate(Quaternion rotation)
   {
      transformGenerator.rotate(rotation);
   }

   public void rotate(double rotationAngle, Axis axis)
   {
      transformGenerator.rotate(rotationAngle, axis);
   }

   public void setTransform(RigidBodyTransform transform)
   {
      transformGenerator.setTransform(transform);
   }

   public void translateThenRotate(RigidBodyTransform transform)
   {
      transformGenerator.translateThenRotate(transform);
   }

   public PlanarRegionsList getPlanarRegionsList()
   {
      return planarRegionsList;
   }

}
