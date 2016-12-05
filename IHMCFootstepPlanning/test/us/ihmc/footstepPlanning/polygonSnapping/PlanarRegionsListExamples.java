package us.ihmc.footstepPlanning.polygonSnapping;

import java.util.Random;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.Axis;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.geometry.PlanarRegionsListGenerator;
import us.ihmc.robotics.random.RandomTools;

public class PlanarRegionsListExamples
{
   public static PlanarRegionsList generateFlatGround(double lengthX, double widthY)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      generator.addCubeReferencedAtBottomMiddle(lengthX, widthY, 0.001);
      PlanarRegionsList flatGround = generator.getPlanarRegionsList();
      return flatGround;
   }

   public static PlanarRegionsList generateStairCase()
   {
      return generateStairCase(new Vector3d());
   }

   public static PlanarRegionsList generateStairCase(Vector3d rotationVector)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      int numberOfSteps = 5;

      double length = 0.4;
      double width = 0.8;
      double height = 0.1;

      generator.translate(length * numberOfSteps / 2.0, 0.0, 0.001);
      generator.addRectangle(1.2 * length * numberOfSteps, 1.2 * width);

      generator.identity();
      generator.translate(length, 0.0, 0.0);
      generator.rotateEuler(rotationVector);
      for (int i = 0; i < numberOfSteps; i++)
      {
         generator.addCubeReferencedAtBottomMiddle(length, width, height);
         generator.translate(length, 0.0, 0.0);
         height = height + 0.1;
      }

      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();
      return planarRegionsList;
   }
   
   public static PlanarRegionsList generateCinderBlockField(double startX, double startY, double cinderBlockSize, int courseWidthXInNumberOfBlocks, int courseLengthYInNumberOfBlocks)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();
      
      double cinderBlockHeight = 0.15;
      double courseWidth = courseLengthYInNumberOfBlocks * cinderBlockSize;
      
      generator.translate(startX, startY, 0.001); // avoid graphical issue
      generator.addRectangle(0.6, courseWidth); // standing platform
      generator.translate(0.5, 0.0, 0.0); // forward to first row
      generator.translate(0.0, -2.5 * cinderBlockSize, 0.0); // over to grid origin
      
      Random random = new Random(1231239L);
      for (int x = 0; x < courseWidthXInNumberOfBlocks; x++)
      {
         for (int y = 0; y < courseLengthYInNumberOfBlocks; y++)
         {
            int angleType = Math.abs(random.nextInt() % 3);
            int axisType = Math.abs(random.nextInt() % 2);
            
            generateSingleCiderBlock(generator, cinderBlockSize, cinderBlockHeight, angleType, axisType);
            
            generator.translate(0.0, cinderBlockSize, 0.0);
         }
         
         if ((x / 2) % 2 == 0)
         {
            generator.translate(0.0, 0.0, 0.1);
         }
         else
         {
            generator.translate(0.0, 0.0, -0.1);
         }
            
         generator.translate(cinderBlockSize, -cinderBlockSize * 6, 0.0);
      }
      
      generator.identity();
      generator.translate(9.0, 0.0, 0.001);
      generator.addRectangle(0.6, courseWidth);
      
      return generator.getPlanarRegionsList();
   }

   public static void generateSingleCiderBlock(PlanarRegionsListGenerator generator, double cinderBlockSize, double cinderBlockHeight, int angleType,
                                                int axisType)
   {
      double angle = 0;
      switch (angleType)
      {
      case 0:
         angle = 0.0;
         break;
      case 1:
         angle = Math.toRadians(15);
         break;
      case 2:
         angle = -Math.toRadians(15);
         break;
      }

      Axis axis = null;
      switch (axisType)
      {
      case 0:
         axis = Axis.X;
         break;
      case 1:
         axis = Axis.Y;
         break;
      }

      generator.rotate(angle, axis);
      generator.addCubeReferencedAtBottomMiddle(cinderBlockSize, cinderBlockSize, cinderBlockHeight);
      generator.rotate(-angle, axis);
   }

   public static PlanarRegionsList generateRandomObjects(Random random, int numberOfRandomObjects, double maxX, double maxY, double maxZ)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      double length = RandomTools.generateRandomDouble(random, 0.2, 1.0);
      double width = RandomTools.generateRandomDouble(random, 0.2, 1.0);
      double height = RandomTools.generateRandomDouble(random, 0.2, 1.0);

      for (int i = 0; i < numberOfRandomObjects; i++)
      {
         generator.identity();

         Vector3d translationVector = RandomTools.generateRandomVector(random, -maxX, -maxY, 0.0, maxX, maxY, maxZ);
         generator.translate(translationVector);

         Quat4d rotation = RandomTools.generateRandomQuaternion(random);
         generator.rotate(rotation);

         generator.addCubeReferencedAtBottomMiddle(length, width, height);
      }

      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();
      return planarRegionsList;
   }

   public static PlanarRegionsList generateBumpyGround(Random random, double maxX, double maxY, double maxZ)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      double length = 0.5;
      double width = 0.5;

      generator.translate(maxX/2.0 + length/2.0, maxY/2.0 - width/2.0, 0.0);
      generator.addCubeReferencedAtBottomMiddle(1.5 * maxX, 1.25 * maxY, 0.01);
      generator.identity();

      int sizeX = (int) (maxX/length);
      int sizeY = (int) (maxY/width);

      for (int i=0; i<sizeY; i++)
      {
         generator.identity();
         generator.translate(0.0, i * width, 0.0);
         for (int j=0; j<sizeX; j++)
         {
            generator.translate(length, 0.0, 0.0);
            double height = RandomTools.generateRandomDouble(random, 0.01, maxZ);
            generator.addCubeReferencedAtBottomMiddle(length, width, height + random.nextDouble() * 0.1);
         }
      }

      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();
      return planarRegionsList;
   }

   public static ConvexPolygon2d createRectanglePolygon(double lengthX, double widthY)
   {
      ConvexPolygon2d convexPolygon = new ConvexPolygon2d();
      convexPolygon.addVertex(lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, -widthY / 2.0);
      convexPolygon.addVertex(lengthX / 2.0, -widthY / 2.0);
      convexPolygon.update();
      return convexPolygon;
   }
}
