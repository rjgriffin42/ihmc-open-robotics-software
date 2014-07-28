package com.yobotics.simulationconstructionset.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.test.JUnitTools;

import com.yobotics.simulationconstructionset.GroundContactPoint;
import com.yobotics.simulationconstructionset.GroundContactPointsHolder;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.ground.SlopedPlaneGroundProfile;

public class LinearStickSlipGroundContactModelTest
{
   @Test
   public void testOnFlatGroundNoSlipCompareWithAndWithoutNormals()
   {
      YoVariableRegistry registry = new YoVariableRegistry("TestRegistry");

      GroundContactPoint groundContactPoint = new GroundContactPoint("testPoint", registry);
      GroundContactPointsHolder pointsHolder = createGroundContactPointsHolder(groundContactPoint);

      LinearStickSlipGroundContactModel groundContactModel = new LinearStickSlipGroundContactModel(pointsHolder, registry);
      groundContactModel.disableSlipping();

      Point3d position = new Point3d(0.0, 0.0, -0.002);
      Vector3d velocity = new Vector3d(0.0, 0.0, -1.0);

      groundContactPoint.setPosition(position);
      groundContactPoint.setVelocity(velocity);

      groundContactModel.enableSurfaceNormal();
      groundContactModel.doGroundContact();

      Vector3d force = new Vector3d();
      groundContactPoint.getForce(force);

      assertEquals(0.0, force.getX(), 1e-7);
      assertEquals(0.0, force.getY(), 1e-7);
      assertTrue(force.getZ() > 0.0);

      Point3d touchdownPosition = new Point3d();
      groundContactPoint.getTouchdownLocation(touchdownPosition);

      JUnitTools.assertTuple3dEquals(touchdownPosition, position, 1e-7);

      groundContactModel.disableSurfaceNormal();
      groundContactModel.doGroundContact();

      Vector3d forceWithNormalsDisabled = new Vector3d();
      groundContactPoint.getForce(forceWithNormalsDisabled);

      JUnitTools.assertTuple3dEquals(force, forceWithNormalsDisabled, 1e-7);

      int numberOfTests = 1000;

      Random random = new Random(1977L);

      for (int i = 0; i < numberOfTests; i++)
      {
         double maxAbsoluteX = 0.01;
         double maxAbsoluteY = 0.01;
         double maxAbsoluteZ = 0.01;
         double maxSpeed = 0.1;

         position = RandomTools.generateRandomPoint(random, maxAbsoluteX, maxAbsoluteY, maxAbsoluteZ);

         // Keep it under ground for now to make sure touchdown doesn't change.
         if (position.getZ() > -1e-7)
            position.setZ(-1e-7);

         velocity = RandomTools.generateRandomVector(random, maxSpeed);

         groundContactPoint.setPosition(position);
         groundContactPoint.setVelocity(velocity);

         groundContactModel.enableSurfaceNormal();
         groundContactModel.doGroundContact();
         assertTrue(groundContactPoint.isInContact());
         groundContactPoint.getForce(force);

         groundContactModel.disableSurfaceNormal();
         groundContactModel.doGroundContact();
         assertTrue(groundContactPoint.isInContact());
         groundContactPoint.getForce(forceWithNormalsDisabled);

         JUnitTools.assertTuple3dEquals(force, forceWithNormalsDisabled, 1e-7);

         Point3d touchdownTest = new Point3d();
         groundContactPoint.getTouchdownLocation(touchdownTest);

         JUnitTools.assertTuple3dEquals(touchdownPosition, touchdownTest, 1e-7);
      }

      // Test one above ground:
      position.set(0.2, 0.3, 1e-7);
      velocity.set(0.0, 0.0, 0.0);

      groundContactPoint.setPosition(position);
      groundContactPoint.setVelocity(velocity);

      groundContactModel.enableSurfaceNormal();
      groundContactModel.doGroundContact();
      assertFalse(groundContactPoint.isInContact());
      groundContactPoint.getForce(force);

      JUnitTools.assertTuple3dEquals(new Vector3d(0.0, 0.0, 0.0), force, 1e-7);
   }
   
   @Test
   public void testOnSlantedGroundCompareWithAndWithoutNormals()
   {
      YoVariableRegistry registryOnFlat = new YoVariableRegistry("TestRegistryOnFlat");
      YoVariableRegistry registryOnSlope = new YoVariableRegistry("TestRegistryOnFlat");

      GroundContactPoint groundContactPointOnFlat = new GroundContactPoint("testPointOnFlat", registryOnFlat);
      GroundContactPointsHolder pointsHolderOnFlat = createGroundContactPointsHolder(groundContactPointOnFlat);
      
      GroundContactPoint groundContactPointOnSlope = new GroundContactPoint("testPointOnSlope", registryOnSlope);
      GroundContactPointsHolder pointsHolderOnSlope = createGroundContactPointsHolder(groundContactPointOnSlope);
      
      LinearStickSlipGroundContactModel groundContactModelOnFlat = new LinearStickSlipGroundContactModel(pointsHolderOnFlat, registryOnFlat);
      groundContactModelOnFlat.enableSlipping();
      
      LinearStickSlipGroundContactModel groundContactModelOnSlope = new LinearStickSlipGroundContactModel(pointsHolderOnSlope, registryOnSlope);
      groundContactModelOnSlope.enableSlipping();
      
      FlatGroundProfile flatGroundProfile = new FlatGroundProfile();
      groundContactModelOnFlat.setGroundProfile3D(flatGroundProfile);
      
      Transform3D transform3D = new Transform3D();
      transform3D.rotX(0.3);
      transform3D.rotY(-0.7);
      transform3D.setTranslation(new Vector3d(0.1, 0.2, 0.3));
      
      Transform3D inverseTransform3D = new Transform3D(transform3D);
      inverseTransform3D.invert();
      
      Vector3d surfaceNormal = new Vector3d(0.0, 0.0, 1.0);
      transform3D.transform(surfaceNormal);
      surfaceNormal.normalize();
      Point3d intersectionPoint = new Point3d();
      transform3D.transform(intersectionPoint);
      
      SlopedPlaneGroundProfile slopedGroundProfile = new SlopedPlaneGroundProfile(surfaceNormal, intersectionPoint, 100.0);
      groundContactModelOnSlope.setGroundProfile3D(slopedGroundProfile);
      
      Random random = new Random(1833L);

      int numberOfTests = 10000;

      for (int i=0; i<numberOfTests; i++)
      {
         double maxAbsoluteXYZ = 0.1;
         double maxAbsoluteVelocity = 1.0;
         Point3d queryPointOnFlat = RandomTools.generateRandomPoint(random, maxAbsoluteXYZ , maxAbsoluteXYZ, maxAbsoluteXYZ);
         Vector3d queryVelocityOnFlat = RandomTools.generateRandomVector(random, maxAbsoluteVelocity);

         groundContactPointOnFlat.setPosition(queryPointOnFlat);
         groundContactPointOnFlat.setVelocity(queryVelocityOnFlat);
         groundContactModelOnFlat.doGroundContact();
         Vector3d forceOnFlat = new Vector3d();
         groundContactPointOnFlat.getForce(forceOnFlat);

         Point3d queryPointOnSlope = new Point3d(queryPointOnFlat);
         Vector3d queryVelocityOnSlope = new Vector3d(queryVelocityOnFlat);

         transform3D.transform(queryPointOnSlope);
         transform3D.transform(queryVelocityOnSlope);

         groundContactPointOnSlope.setPosition(queryPointOnSlope);
         groundContactPointOnSlope.setVelocity(queryVelocityOnSlope);
         groundContactModelOnSlope.doGroundContact();

         Vector3d forceOnSlope = new Vector3d();
         groundContactPointOnSlope.getForce(forceOnSlope);

         inverseTransform3D.transform(forceOnSlope);

         JUnitTools.assertTuple3dEquals(forceOnFlat, forceOnSlope, 1e-7);
         
         assertTrue(groundContactPointOnFlat.isInContact() == groundContactPointOnSlope.isInContact());
         assertTrue(groundContactPointOnFlat.isSlipping() == groundContactPointOnSlope.isSlipping());
         
      }
   }


   private GroundContactPointsHolder createGroundContactPointsHolder(GroundContactPoint groundContactPoint)
   {
      final ArrayList<GroundContactPoint> groundContactPoints = new ArrayList<GroundContactPoint>();
      groundContactPoints.add(groundContactPoint);

      GroundContactPointsHolder pointsHolder = new GroundContactPointsHolder()
      {
         public ArrayList<GroundContactPoint> getGroundContactPoints(int groundContactGroupIdentifier)
         {
            return groundContactPoints;
         }
      };
      return pointsHolder;
   }

}
