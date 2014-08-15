package com.yobotics.simulationconstructionset.util.ground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.test.JUnitTools;

public class CylinderTerrainObjectTest
{
   private static final double errEpsilon = 1e-14;
   private static final double testDelta = .0001;

   @Ignore
   @Test
   public void testSimpleCylinder()
   {
      double height = 1.3;
      double radius = 0.2;
      Transform3D location = new Transform3D();
      location.setTranslation(new Vector3d(0.0, 0.0, height/2.0));

      CylinderTerrainObject cylinderTerrainObject = new CylinderTerrainObject(location, height, radius, YoAppearance.Red());
      
      Vector3d surfaceNormal = new Vector3d();
      double heightAt = cylinderTerrainObject.heightAndNormalAt(0.0, 0.0, 0.0, surfaceNormal);
      assertEquals(height, heightAt, 1e-7);
      JUnitTools.assertTuple3dEquals(new Vector3d(0.0, 0.0, 1.0), surfaceNormal, 1e-7);
      
      heightAt = cylinderTerrainObject.heightAndNormalAt(0.0, radius - 1e-7, 0.0, surfaceNormal);
      assertEquals(height, heightAt, 1e-7);
      
      heightAt = cylinderTerrainObject.heightAndNormalAt(0.0, radius + 1e-7, 0.0, surfaceNormal);
      assertEquals(0.0, heightAt, 1e-7);
      
      heightAt = cylinderTerrainObject.heightAndNormalAt(radius + 1e-7, 0.0, 0.0, surfaceNormal);
      assertEquals(0.0, heightAt, 1e-7);
      
      Point3d intersection = new Point3d();
      boolean isInside = cylinderTerrainObject.checkIfInside(0.0, 0.0, height-0.02, intersection, surfaceNormal);
      
      assertTrue(isInside);
      
      //TODO: FIXME!!!
      JUnitTools.assertTuple3dEquals(new Vector3d(0.0, 0.0, 1.0), surfaceNormal, 1e-7);
      JUnitTools.assertTuple3dEquals(new Vector3d(0.0, 0.0, height), intersection, 1e-7);

   }
   
   
   @Test
   public void testHeightAtTranslatedRot90TallHorizontalCylinderJustInsideAndOutside()
   {
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      Vector3d translatedCenter = new Vector3d(5, 3, 1.5);

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 90.0;
      yawDegrees = 90.0;
      double tallHeight = 2 * height;
      CylinderTerrainObject translatedRot90TallHorizontalCylinder = new CylinderTerrainObject(translatedCenter, slopeDegrees, yawDegrees, tallHeight, radius,
                                                                       app);

      double expectedHeight = radius + translatedCenter.z;
      double expectedMiss = 0.0;
      double[] signY = {0, -1, 1};
      boolean[] isEdge = {false, true, true};

      for (int i = 0; i < signY.length; i++)
      {
         double testX = 0.0 + +translatedCenter.x;
         double testY = signY[i] * (tallHeight / 2 - testDelta) + +translatedCenter.y;
         double testZ = expectedHeight + 1.0;

         assertEquals(expectedHeight, translatedRot90TallHorizontalCylinder.heightAt(testX, testY, testZ), errEpsilon);

         if (isEdge[i])
         {
            testY = signY[i] * (tallHeight / 2 + testDelta) + translatedCenter.y;
            assertEquals(expectedMiss, translatedRot90TallHorizontalCylinder.heightAt(testX, testY, testZ), errEpsilon);
         }
      }
   }

   @Test
   public void testHeightAtRot90TallHorizontalCylinderJustInsideAndOutside()
   {
      Vector3d center = new Vector3d(0, 0, 0);
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 90.0;
      yawDegrees = 90.0;
      double tallHeight = 2 * height;
      CylinderTerrainObject rot90TallHorizontalCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, tallHeight, radius, app);

      double expectedHeight = radius;
      double expectedMiss = 0.0;
      double[] signY = {0, -1, 1};
      boolean[] isEdge = {false, true, true};

      for (int i = 0; i < signY.length; i++)
      {
         double testX = 0.0;
         double testY = signY[i] * (tallHeight / 2 - testDelta);
         double testZ = expectedHeight + 1.0;

         assertEquals(expectedHeight, rot90TallHorizontalCylinder.heightAt(testX, testY, testZ), errEpsilon);

         if (isEdge[i])
         {
            testY = signY[i] * (tallHeight / 2 + testDelta);
            assertEquals(expectedMiss, rot90TallHorizontalCylinder.heightAt(testX, testY, testZ), errEpsilon);
         }
      }
   }

   @Test
   public void testHeightAtTranslatedVerticalCylinderJustInside()
   {
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      Vector3d translatedCenter = new Vector3d(5, 3, 1.5);

      AppearanceDefinition app = YoAppearance.Red();

      CylinderTerrainObject translatedVerticalCylinder = new CylinderTerrainObject(translatedCenter, slopeDegrees, yawDegrees, height, radius, app);

      double expectedHeight = translatedCenter.z + height / 2;
      double[] signX = {-1, 0, 1, 0};
      double[] signY = {0, -1, 0, 1};

      for (int i = 0; i < signX.length; i++)
      {
         double testX = signX[i] * (radius - testDelta) + translatedCenter.x;
         double testY = signY[i] * (radius - testDelta) + translatedCenter.y;
         double testZ = expectedHeight + 1.0;

         assertEquals(expectedHeight, translatedVerticalCylinder.heightAt(testX, testY, testZ), errEpsilon);
      }
   }

   @Test
   public void testHeightAtTranslatedHorizontalCylinderJustInside()
   {
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      Vector3d translatedCenter = new Vector3d(5, 3, 1.5);

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 90.0;
      CylinderTerrainObject translatedHorizontalCylinder = new CylinderTerrainObject(translatedCenter, slopeDegrees, yawDegrees, height, radius, app);


      double expectedHeight = translatedCenter.z + radius;
      double[] signX = {-1, 0, 1};

      for (int i = 0; i < signX.length; i++)
      {
         double testX = signX[i] * (height / 2 - testDelta) + translatedCenter.x;
         double testY = translatedCenter.y;
         double testZ = expectedHeight + 1.0;

         assertEquals(expectedHeight, translatedHorizontalCylinder.heightAt(testX, testY, testZ), errEpsilon);
      }
   }

   @Test
   public void testHeightAtTranslatedVerticalCylinderJustOutside()
   {
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      Vector3d translatedCenter = new Vector3d(5, 3, 1.5);
      AppearanceDefinition app = YoAppearance.Red();
      CylinderTerrainObject translatedVerticalCylinder = new CylinderTerrainObject(translatedCenter, slopeDegrees, yawDegrees, height, radius, app);

      double expectedHeight = 0;
      double[] signX = {-1, 0, 1, 0};
      double[] signY = {0, -1, 0, 1};

      for (int i = 0; i < signX.length; i++)
      {
         double testX = signX[i] * (radius + testDelta) + translatedCenter.x;
         double testY = signY[i] * (radius + testDelta) + translatedCenter.y;
         double testZ = expectedHeight + 1.0;

         assertEquals(expectedHeight, translatedVerticalCylinder.heightAt(testX, testY, testZ), errEpsilon);
      }
   }

   @Test
   public void testHeightAtTranslatedHorizontalCylinderJustOutside()
   {
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      Vector3d translatedCenter = new Vector3d(5, 3, 1.5);

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 90.0;
      CylinderTerrainObject translatedHorizontalCylinder = new CylinderTerrainObject(translatedCenter, slopeDegrees, yawDegrees, height, radius, app);

      double expectedHeight = 0;
      double[] signX = {-1, 1};

      for (int i = 0; i < signX.length; i++)
      {
         double testX = signX[i] * (height / 2 + testDelta) + translatedCenter.x;
         double testY = translatedCenter.y;
         double testZ = expectedHeight + 1.0;

         assertEquals(expectedHeight, translatedHorizontalCylinder.heightAt(testX, testY, testZ), errEpsilon);
      }
   }

   @Test
   public void testHeightAtVerticalCylinderOutside()
   {
      Vector3d center = new Vector3d(0, 0, 0);
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      AppearanceDefinition app = YoAppearance.Red();

      CylinderTerrainObject verticalCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, height, radius, app);
      slopeDegrees = 90.0;
      CylinderTerrainObject horizontalCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, height, radius, app);

      assertEquals(0, verticalCylinder.heightAt(height / 2 * 1.5, 0, 0), errEpsilon);
      assertEquals(0, verticalCylinder.heightAt(-height / 2 * 1.5, 0, 0), errEpsilon);
      assertEquals(0, verticalCylinder.heightAt(0, -height / 2 * 1.5, 0), errEpsilon);
      assertEquals(0, verticalCylinder.heightAt(0, height / 2 * 1.5, 0), errEpsilon);
      assertEquals(0, horizontalCylinder.heightAt(height / 2 * 1.5, 0, 0), errEpsilon);    // fails with only test outside to side
      assertEquals(0, horizontalCylinder.heightAt(-height / 2 * 1.5, 0, 0), errEpsilon);    // fails with only test outside to side
      assertEquals(0, horizontalCylinder.heightAt(0, radius * 1.5, 0), errEpsilon);
      assertEquals(0, horizontalCylinder.heightAt(0, -radius * 1.5, 0), errEpsilon);
      assertEquals(height / 2.0, verticalCylinder.heightAt(0, 0, height / 2.0), errEpsilon);
      assertEquals(-height / 2.0, verticalCylinder.heightAt(0, 0, -height / 2.0), errEpsilon);
      assertEquals(height / 2.0, verticalCylinder.heightAt(radius / 2, 0, height / 2.0), errEpsilon);
      assertEquals(height / 2.0, verticalCylinder.heightAt(-radius / 2, 0, height / 2.0), errEpsilon);
      assertEquals(height / 2.0, verticalCylinder.heightAt(0, radius / 2, height / 2.0), errEpsilon);
      assertEquals(height / 2.0, verticalCylinder.heightAt(0, -radius / 2, height / 2.0), errEpsilon);
      assertEquals(radius, verticalCylinder.heightAt(0, 0, radius), errEpsilon);
      assertEquals(-radius, verticalCylinder.heightAt(0, 0, -radius), errEpsilon);
      assertEquals(radius, verticalCylinder.heightAt(height / 4, 0, radius), errEpsilon);
      assertEquals(radius, verticalCylinder.heightAt(-height / 4, 0, radius), errEpsilon);

      double expectedHeightOnCircle = Math.sqrt(radius * radius - (radius / 2) * (radius / 2));
      assertEquals(expectedHeightOnCircle, horizontalCylinder.heightAt(height / 4, radius / 2, radius), errEpsilon);
   }

   @Test
   public void testHeightAtSlopedRotatedTwoSidesTop()
   {
      Vector3d center = new Vector3d(0, 0, 0);
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 45.0;
      yawDegrees = 30.0;
      double angleAxisDownRadians = Math.toRadians(slopeDegrees);
      double angleAxisFromXRadians = Math.toRadians(yawDegrees);
      CylinderTerrainObject slopedRotatedCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, height, radius, app);

      double distanceAlongAxis = height / 2;
      double dCenterToContact = Math.sqrt(distanceAlongAxis * distanceAlongAxis + radius * radius);
      double angleFromAxisToD = Math.atan(radius / distanceAlongAxis);
      double dHorizontal = dCenterToContact * Math.cos(angleAxisDownRadians + angleFromAxisToD);
      double dVertical = dCenterToContact * Math.sin(angleAxisDownRadians + angleFromAxisToD);
      double x = dHorizontal * Math.cos(angleAxisFromXRadians);
      double y = dHorizontal * Math.sin(angleAxisFromXRadians);
      double z = dVertical;

      assertEquals(z, slopedRotatedCylinder.heightAt(x, y, z + 1), errEpsilon);
   }

   @Test
   public void testHeightAtSlopedRotatedTwoSidesBottom()
   {
      Vector3d center = new Vector3d(0, 0, 0);
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 45.0;
      yawDegrees = 30.0;
      double angleAxisDownRadians = Math.toRadians(slopeDegrees);
      double angleAxisFromXRadians = Math.toRadians(yawDegrees);
      CylinderTerrainObject slopedRotatedCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, height, radius, app);


      double distanceAlongAxis = height / 2;
      double dCenterToContact = Math.sqrt(distanceAlongAxis * distanceAlongAxis + radius * radius);
      double angleFromAxisToD = Math.atan(radius / distanceAlongAxis);    // Should Be 45 for expectedHeight below to work
      double dHorizontal = dCenterToContact * Math.cos(angleAxisDownRadians + angleFromAxisToD);
      double dVertical = dCenterToContact * Math.sin(angleAxisDownRadians + angleFromAxisToD);
      double x = dHorizontal * Math.cos(angleAxisFromXRadians);
      double y = dHorizontal * Math.sin(angleAxisFromXRadians);
      double z = dVertical;

      double expectedHeight = z - Math.sqrt(2 * (2 * radius) * (2 * radius));
      assertEquals(expectedHeight, slopedRotatedCylinder.heightAt(x, y, -1), errEpsilon);
   }

   @Test
   public void testHeightAtSlopedRotatedEndAndSideTop()
   {
      Vector3d center = new Vector3d(0, 0, 0);
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 45.0;
      yawDegrees = 30.0;
      double angleAxisDownRadians = Math.toRadians(slopeDegrees);
      double angleAxisFromXRadians = Math.toRadians(yawDegrees);
      CylinderTerrainObject slopedRotatedCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, height, radius, app);

      double distanceAlongAxis = height / 2;
      double dHorizontal = distanceAlongAxis * Math.cos(angleAxisDownRadians);
      double dVertical = distanceAlongAxis * Math.sin(angleAxisDownRadians);
      double x = dHorizontal * Math.cos(angleAxisFromXRadians);
      double y = dHorizontal * Math.sin(angleAxisFromXRadians);
      double z = dVertical;

      double expectedHeight = z;
      assertEquals(expectedHeight, slopedRotatedCylinder.heightAt(x, y, 2), errEpsilon);
   }

   @Test
   public void testHeightAtSlopedRotatedEndAndSideBottom()
   {
      Vector3d center = new Vector3d(0, 0, 0);
      double slopeDegrees = 0.0;
      double yawDegrees = 0.0;
      double height = 2.0;
      double radius = 1.0;

      AppearanceDefinition app = YoAppearance.Red();

      slopeDegrees = 45.0;
      yawDegrees = 30.0;
      double angleAxisDownRadians = Math.toRadians(slopeDegrees);
      double angleAxisFromXRadians = Math.toRadians(yawDegrees);
      CylinderTerrainObject slopedRotatedCylinder = new CylinderTerrainObject(center, slopeDegrees, yawDegrees, height, radius, app);

      double distanceAlongAxis = height / 2;
      double dHorizontal = distanceAlongAxis * Math.cos(angleAxisDownRadians);
      double dVertical = distanceAlongAxis * Math.sin(angleAxisDownRadians);
      double x = dHorizontal * Math.cos(angleAxisFromXRadians);
      double y = dHorizontal * Math.sin(angleAxisFromXRadians);
      double z = dVertical;

      double expectedHeight = z - Math.sqrt(radius * radius * 2);
      assertEquals(expectedHeight, slopedRotatedCylinder.heightAt(x, y, -2), errEpsilon);
   }

}
