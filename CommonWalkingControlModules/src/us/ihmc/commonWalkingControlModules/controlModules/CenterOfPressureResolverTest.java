package us.ihmc.commonWalkingControlModules.controlModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;
import us.ihmc.utilities.test.JUnitTools;

public class CenterOfPressureResolverTest
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   
   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }
   
   @After
   public void showMemoryUsageAfterTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   @Test
   public void testCenterOfPressureResolverSimpleCaseWithNoTorque()
   {      
      Point3d groundPoint = new Point3d();
      Vector3d groundNormal = new Vector3d(0.0, 0.0, 1.0);

      Point3d centerOfMassPoint = new Point3d(0.0, 0.0, 1.0);
      
      Vector3d centerOfMassForce = new Vector3d(1.0, 2.0, 1.0);
      Vector3d centerOfMassTorque = new Vector3d(0.0, 0.0, 0.0);
      
      Vector3d expectedCenterOfPressure = new Vector3d(-1.0, -2.0, 0.0);
      
      double expectedNormalTorque = 0.0;
      
      computeAndVerifyCenterOfPressureAndNormalTorque(groundPoint, groundNormal, centerOfMassPoint, centerOfMassForce, centerOfMassTorque,
            expectedCenterOfPressure, expectedNormalTorque);
   }
   
   @Test
   public void testCenterOfPressureResolverSimpleCaseWithVerticalForce()
   {      
      Point3d groundPoint = new Point3d();
      Vector3d groundNormal = new Vector3d(0.0, 0.0, 1.0);

      Point3d centerOfMassPoint = new Point3d(0.0, 0.0, 1.0);
      
      Vector3d centerOfMassForce = new Vector3d(0.0, 0.0, 1.0);
      Vector3d centerOfMassTorque = new Vector3d(1.0, 2.0, 3.0);
      
      Vector3d expectedCenterOfPressure = new Vector3d(-2.0, 1.0, 0.0);
      
      double expectedNormalTorque = 3.0;
      
      computeAndVerifyCenterOfPressureAndNormalTorque(groundPoint, groundNormal, centerOfMassPoint, centerOfMassForce, centerOfMassTorque,
            expectedCenterOfPressure, expectedNormalTorque);
   }
   
   
   @Test
   public void testCenterOfPressureResolverNoForceInZ()
   {      
      Point3d groundPoint = new Point3d();
      Vector3d groundNormal = new Vector3d(0.0, 0.0, 1.0);

      Point3d centerOfMassPoint = new Point3d(0.0, 0.0, 1.0);
      
      Vector3d centerOfMassForce = new Vector3d(1.0, 0.0, 0.0);
      Vector3d centerOfMassTorque = new Vector3d(0.1, 0.2, 0.3);
      
      Vector3d expectedCenterOfPressure = new Vector3d(0.0, 0.0, 0.0);
      
      double expectedNormalTorque = 0.3;
      
      computeAndVerifyCenterOfPressureAndNormalTorque(groundPoint, groundNormal, centerOfMassPoint, centerOfMassForce, centerOfMassTorque,
            expectedCenterOfPressure, expectedNormalTorque);
   }


   @Test
   public void testRandomExamples()
   {
      Random random = new Random(1776L);
      int numberOfTests = 10000;
      
      for (int i=0; i<numberOfTests; i++)
      {
         Point3d groundPoint = RandomTools.generateRandomPoint(random, 1.0, 1.0, 0.2);
         
         Vector3d groundNormal = RandomTools.generateRandomVector(random);
         groundNormal.setZ(1.0);
         groundNormal.normalize();
         
         Point3d centerOfMassPoint = RandomTools.generateRandomPoint(random, 1.0, 1.0, 0.2);
         centerOfMassPoint.setZ(1.2);
         
         Vector3d centerOfMassForce = RandomTools.generateRandomVector(random, 100.0);
         centerOfMassForce.setZ(127.0);
         
         Vector3d centerOfMassTorque = new Vector3d(0.0, 0.0, 0.0);
                  
         PoseReferenceFrame centerOfMassFrame = createTranslatedZUpFrame("centerOfMassFrame", centerOfMassPoint);
         SpatialForceVector spatialForceVector = new SpatialForceVector(centerOfMassFrame, centerOfMassForce, centerOfMassTorque);

         Pair<FramePoint, Double> centerOfPressureAndNormalTorque = computeCenterOfPressureAndNormalTorque(groundPoint, groundNormal, centerOfMassFrame, spatialForceVector);
         FramePoint centerOfPressure = centerOfPressureAndNormalTorque.first();
         double normalTorqueResolvedAtCenterOfPressure = centerOfPressureAndNormalTorque.second();
         centerOfPressure.changeFrame(worldFrame);
         
         PoseReferenceFrame centerOfPressurePlaneFrame = createPlaneFrame("groundPlaneFrame", centerOfPressure.getPointCopy(), groundNormal);
         spatialForceVector.changeFrame(centerOfPressurePlaneFrame);
         
         FrameVector forceResolvedInCenterOfPressureFrame = spatialForceVector.getLinearPartAsFrameVectorCopy();
         FrameVector torqueResolvedInCenterOfPressureFrame = spatialForceVector.getAngularPartAsFrameVectorCopy();
         
         forceResolvedInCenterOfPressureFrame.changeFrame(worldFrame);
         
         // Forces should be the same, after rotated into the same frame:
         JUnitTools.assertTuple3dEquals(centerOfMassForce, forceResolvedInCenterOfPressureFrame.getVectorCopy(), 1e-7); 
         
         // Torque should have no components in x and y resolved at the Center of Pressure:
         assertEquals(0.0, torqueResolvedInCenterOfPressureFrame.getX(), 1e-7);
         assertEquals(0.0, torqueResolvedInCenterOfPressureFrame.getY(), 1e-7);         
         assertEquals(normalTorqueResolvedAtCenterOfPressure, torqueResolvedInCenterOfPressureFrame.getZ(), 1e-7);         
      }
   }
   
   
   private static void computeAndVerifyCenterOfPressureAndNormalTorque(Point3d groundPoint, Vector3d groundNormal, Point3d centerOfMassPoint,
         Vector3d centerOfMassForce, Vector3d centerOfMassTorque, Vector3d expectedCenterOfPressure, double expectedNormalTorque)
   {
      PoseReferenceFrame centerOfMassFrame = createTranslatedZUpFrame("centerOfMassFrame", centerOfMassPoint);
      SpatialForceVector spatialForceVector = new SpatialForceVector(centerOfMassFrame, centerOfMassForce, centerOfMassTorque);

      Pair<FramePoint, Double> centerOfPressureAndNormalTorque = computeCenterOfPressureAndNormalTorque(groundPoint, groundNormal, centerOfMassFrame, spatialForceVector);
      FramePoint centerOfPressure = centerOfPressureAndNormalTorque.first();
      double normalTorque = centerOfPressureAndNormalTorque.second();
      
      FramePoint expectedCenterOfPressureFramePoint = new FramePoint(worldFrame, expectedCenterOfPressure);
      assertTrue("expectedCenterOfPressureFramePoint = " + expectedCenterOfPressureFramePoint + ", centerOfPressure = " + centerOfPressure, expectedCenterOfPressureFramePoint.epsilonEquals(centerOfPressure, 1e-7));
      
      assertEquals(expectedNormalTorque, normalTorque, 1e-7);
   }


   private static Pair<FramePoint, Double> computeCenterOfPressureAndNormalTorque(Point3d groundPoint, Vector3d groundNormal, ReferenceFrame centerOfMassFrame, SpatialForceVector spatialForceVector)
   {
      CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();
      
      PoseReferenceFrame groundPlaneFrame = createPlaneFrame("groundPlaneFrame", groundPoint, groundNormal);      
      FramePoint2d centerOfPressure2d = new FramePoint2d(worldFrame);
      
      double normalTorqueAtCenterOfPressure = centerOfPressureResolver.resolveCenterOfPressureAndNormalTorque(centerOfPressure2d, spatialForceVector, groundPlaneFrame);
      FramePoint centerOfPressure = centerOfPressure2d.toFramePoint();
      centerOfPressure.changeFrame(worldFrame);
      return new Pair<FramePoint, Double>(centerOfPressure, normalTorqueAtCenterOfPressure);
   }


   private static PoseReferenceFrame createPlaneFrame(String name, Point3d planeReferencePoint, Vector3d planeSurfaceNormal)
   {
      PoseReferenceFrame planeFrame = new PoseReferenceFrame(name, ReferenceFrame.getWorldFrame());
      Matrix3d rotationFromNormal = computeRotationFromNormal(planeSurfaceNormal);
      
      Transform3D transform3D = new Transform3D(rotationFromNormal, new Vector3d(planeReferencePoint), 1.0);
      FramePose framePose = new FramePose(ReferenceFrame.getWorldFrame(), transform3D);
      
      planeFrame.updatePose(framePose);
      planeFrame.update();
      return planeFrame;
   }


   private static PoseReferenceFrame createTranslatedZUpFrame(String name, Point3d frameCenterPoint)
   {
      PoseReferenceFrame translatedZUpFrame = new PoseReferenceFrame(name, ReferenceFrame.getWorldFrame());
      Transform3D transform3D = new Transform3D();
      transform3D.setTranslation(new Vector3d(frameCenterPoint));
      
      FramePose framePose = new FramePose(ReferenceFrame.getWorldFrame(), transform3D);
      translatedZUpFrame.updatePose(framePose);
      translatedZUpFrame.update();
      return translatedZUpFrame;
   }

   
   private static Matrix3d computeRotationFromNormal(Vector3d normal)
   {
      // Won't work if normal is 1,0,0
      
      Vector3d zAxis = new Vector3d(normal);
      zAxis.normalize();
      
      Vector3d xAxis = new Vector3d(1.0, 0.0, 0.0);
      
      Vector3d yAxis = new Vector3d();
      yAxis.cross(normal, xAxis);
      yAxis.normalize();
      
      xAxis.cross(yAxis, zAxis);
      xAxis.normalize();
      
      zAxis.cross(xAxis, yAxis);
      zAxis.normalize();
      
      Matrix3d rotation = new Matrix3d(xAxis.getX(), xAxis.getY(), xAxis.getZ(), yAxis.getX(), yAxis.getY(), yAxis.getZ(), zAxis.getX(), zAxis.getY(), zAxis.getZ());
      return rotation;
   }
}
