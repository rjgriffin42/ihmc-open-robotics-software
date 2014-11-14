package us.ihmc.simulationconstructionset.util.ground;

import static org.junit.Assert.assertEquals;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.utilities.test.JUnitTools;

public class RotatableRampTerrainObjectTest
{
   private RotatableRampTerrainObject simpleRamp, simpleRampDown, ramp90;
   private RotatableRampTerrainObject simpleRampTranslated, ramp90Translated;
   private double epsilon = 1e-12;
   
   private Point3d pointsOnSimpleRamp[] =
   {
      new Point3d(0, 0, 0), new Point3d(1, 0, 1), new Point3d(0.5, 0, 0.5), new Point3d(0.5, -1, 0.5), new Point3d(0.5, 1, 0.5), new Point3d(1, 1, 1),
      new Point3d(1,-1,1)
   };
   
   private Point3d strictlyInternalPointsOnSimpleRampDown[] =
   {
      new Point3d(0.001, 0.0, 0.999), new Point3d(0.999, 0, 0.001), new Point3d(0.5, 0, 0.5), 
      new Point3d(0.5, -0.999, 0.5),  new Point3d(0.5, 0.999, 0.5), 
      new Point3d(0.999, 0.999, 0.001)
   };
   
   private Point3d pointsOnOtherRampFaces[] = {new Point3d(1, 0, 0.5), new Point3d(0.5, 1, 0.25), new Point3d(0.5, -1, 0.25)};
   private Vector3d expectedSimpleSurfaceNormal = new Vector3d(-1, 0, 1);
   private Vector3d expectedSimpleSurfaceNormalOnOtherFaces[] = {new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, -1, 0)};
   private Point3d pointsOnOtherRampFacesSlopeDown[] = {new Point3d(0, 0, 0.5), new Point3d(0.5, 1, 0.25), new Point3d(0.5, -1, 0.25)};
   private Vector3d expectedSimpleSurfaceNormalSlopeDown = new Vector3d(1, 0, 1);
   private Vector3d expectedSimpleSurfaceNormalOnOtherFacesSlopeDown[] = {new Vector3d(-1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, -1, 0)};

   private Point3d pointsOnRamp90[] =
   {
      new Point3d(0, 0, 0.5), new Point3d(1, 0, 0.5), new Point3d(-1, 0, 0.5),
      new Point3d(0, -0.49, 0.01),  new Point3d(1, -0.5, 0), new Point3d(-0.99, -0.499, 0.001),
      new Point3d(0.5, 0.25, 0.75), new Point3d(0.9, 0.4, 0.9), new Point3d(1.0, 0.4, 0.9), new Point3d(1.0, 0.45, 0.95), new Point3d(1.0, 0.499, 0.999),
      new Point3d(0, 0.5, 1), new Point3d(-1, 0.5, 1), new Point3d(0.9, 0.5,1)//, new Point3d(0.909, 0.5,1),//, new Point3d(1, 0.5, 1)
      
   };
   
   private Point3d pointsOnRamp90Translated[] =
   {
      new Point3d(0, 0, 0.5), new Point3d(-0.99, -0.499, 0.001),
      new Point3d(0.9, 0.4, 0.9)      
   };
   
   private Point3d pointsOnRamp90PassingHeightCornerCases[] =
   {
      new Point3d(-1, -0.5, 0), new Point3d(0, -0.5, 0)
      
   };
   private Point3d pointsOnRamp90withNumericalRotationError[] =
   {
         new Point3d(0.909, 0.5,1), new Point3d(1, 0.5, 1)
      
   };
   private Vector3d expectedSurfaceNormalRamp90 = new Vector3d(0, -1, 1);
   private Point3d pointsOnOtherFacesRamp90[] = {new Point3d(0, 0.5, 0.5), new Point3d(1, 0, 0.25), new Point3d(-1, 0, 0.25)};
   private Vector3d expectedSurfaceNormalOnOtherFacesRamp90[] = {new Vector3d(0, 1, 0), new Vector3d(1, 0, 0), new Vector3d(-1, 0, 0)};
   
   private static double transX=3.0;
   private static double transY=2.0;
   
   @Before
   public void setUp() throws Exception
   {
      simpleRamp = new RotatableRampTerrainObject(0.5, 0, 1, 2, 1, 0);
      simpleRampDown = new RotatableRampTerrainObject(0.5, 0, -1, 2, 1, 0);
      ramp90 = new RotatableRampTerrainObject(0, 0, 1, 2, 1, 90);
      simpleRampTranslated = new RotatableRampTerrainObject(transX+0.5, transY, 1, 2, 1, 0);
      ramp90Translated = new RotatableRampTerrainObject(transX, transY, 1, 2, 1, 90);
   }

   @Test
   public void testHeightAt()
   {
      testHeightAtRampForAnyRamp(pointsOnSimpleRamp, simpleRamp);
   }

   @Test
   public void testHeightAtForRampDown()
   {
      testHeightAtRampForAnyRamp(strictlyInternalPointsOnSimpleRampDown, simpleRampDown);
   }

   @Test
   public void testSurfaceNormalAt()
   {
      testSurfaceNormalsForAnyRampFace(simpleRamp, expectedSimpleSurfaceNormal, pointsOnSimpleRamp);
   }

   @Test
   public void testOtherSurfaceNormalAt()
   {
      testSurfaceNormalsForAnyOtherRampSides(simpleRamp, 
            expectedSimpleSurfaceNormalOnOtherFaces, pointsOnOtherRampFaces);
   }

   @Test
   public void testSurfaceNormalAtForSlopedDown()
   {
      testSurfaceNormalsForAnyRampFace(simpleRampDown, 
            expectedSimpleSurfaceNormalSlopeDown, strictlyInternalPointsOnSimpleRampDown);
   }

   @Test
   public void testOtherSurfaceNormalAtForSlopedDown()
   {
      testSurfaceNormalsForAnyOtherRampSides(simpleRampDown,
            expectedSimpleSurfaceNormalOnOtherFacesSlopeDown, pointsOnOtherRampFacesSlopeDown);
   }

   @Test
   public void testHeightAtRamp90()
   {
      testHeightAtRampForAnyRamp(pointsOnRamp90, ramp90);
      testHeightAtRampForAnyRamp(pointsOnRamp90PassingHeightCornerCases, ramp90);      
   }
   
   @Test @Ignore
   public void HeightAtRamp90EdgeCasesFailDueToNumericalErrorTest()
   {
      testHeightAtRampForAnyRamp(pointsOnRamp90withNumericalRotationError, ramp90);
   }   

   @Test   
   public void testSurfaceNormalForRamp90()
   {
      testSurfaceNormalsForAnyRampFace(ramp90, 
            expectedSurfaceNormalRamp90, pointsOnRamp90);
   }

   @Test   
   public void testOtherSurfaceNormalForRamp90()
   {
      testSurfaceNormalsForAnyOtherRampSides(ramp90, 
            expectedSurfaceNormalOnOtherFacesRamp90, pointsOnOtherFacesRamp90);
   }

   private void testHeightAtRampForAnyRamp(Point3d[] pointsOnRamp, RotatableRampTerrainObject ramp)
   {
      for (int i = 0; i < pointsOnRamp.length; i++)
      {
         String message = "Expected Height For point " + pointsOnRamp[i].getX() + " " + pointsOnRamp[i].getY() + " " + pointsOnRamp[i].getZ();
         assertEquals(message, pointsOnRamp[i].getZ(), ramp.heightAt(pointsOnRamp[i].getX(), pointsOnRamp[i].getY(), pointsOnRamp[i].getZ()), epsilon);
      }
   }

   private void testHeightAtRampForAnyRampWithTranslation(Point3d[] pointsOnRamp, RotatableRampTerrainObject ramp, Vector3d translation)
   {
      for (int i = 0; i < pointsOnRamp.length; i++)
      {
         String message = "Expected Height For point " + (pointsOnRamp[i].getX()+translation.x) + " " + 
               (pointsOnRamp[i].getY()+translation.y) + " " + pointsOnRamp[i].getZ();
         assertEquals(message, pointsOnRamp[i].getZ(), ramp.heightAt(pointsOnRamp[i].getX()+translation.x, pointsOnRamp[i].getY()+translation.y, pointsOnRamp[i].getZ()), epsilon);
      }
   }
   
   @Test
   public void testHeightAtTranslation()
   {
      testHeightAtRampForAnyRampWithTranslation(pointsOnSimpleRamp, simpleRampTranslated, new Vector3d(transX,transY,0));
   }

   @Test
   public void testHeightAt90Translation()
   {
      testHeightAtRampForAnyRampWithTranslation(pointsOnRamp90Translated, ramp90Translated, new Vector3d(transX,transY,0));
   }


   private void testSurfaceNormalsForAnyRampFace(RotatableRampTerrainObject ramp, 
         Vector3d expectedRampSurfaceNormal, Point3d[] pointsOnRamp)
 {
    expectedRampSurfaceNormal.normalize();

    for (int i = 0; i < pointsOnRamp.length; i++)
    {
       Vector3d normal = new Vector3d();
       ramp.surfaceNormalAt(pointsOnRamp[i].getX(), pointsOnRamp[i].getY(), pointsOnRamp[i].getZ(), normal);
       String message = "Normal for point " + pointsOnRamp[i].getX() + " " + pointsOnRamp[i].getY() + " " + pointsOnRamp[i].getZ();
       JUnitTools.assertTuple3dEquals(message, expectedRampSurfaceNormal, normal, epsilon);
    }
 }
 
   private void testSurfaceNormalsForAnyOtherRampSides(RotatableRampTerrainObject ramp,
         Vector3d[] expectedSurfaceNormalOnOtherFaces, Point3d[] pointsOnOtherRampFaces)
 {
    for (int i = 0; i < pointsOnOtherRampFaces.length; i++)
    {
       expectedSurfaceNormalOnOtherFaces[i].normalize();
       Vector3d normal = new Vector3d();
       ramp.surfaceNormalAt(pointsOnOtherRampFaces[i].getX(), pointsOnOtherRampFaces[i].getY(), pointsOnOtherRampFaces[i].getZ(), normal);
       String message = "Normal for point " + pointsOnOtherRampFaces[i].getX() + " " + pointsOnOtherRampFaces[i].getY() + " "
                        + pointsOnOtherRampFaces[i].getZ();
       JUnitTools.assertTuple3dEquals(message, expectedSurfaceNormalOnOtherFaces[i], normal, epsilon);
    }
 }
 
   


}
