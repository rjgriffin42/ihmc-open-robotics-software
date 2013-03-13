package com.yobotics.simulationconstructionset.ground;

import javax.vecmath.Vector3d;
import org.junit.Test;

import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.utilities.test.JUnitTools;

public abstract class GroundProfileTest
{
   protected GroundProfile groundProfile;
   private final double epsilon;
   private final boolean debug = true;

   public GroundProfileTest(double epsilon)
   {
      this.epsilon = epsilon;
   }

   @Test
   public void testSurfaceNormalAlongXAxis()
   {
      int nSteps = 1000;
      double xStep = (groundProfile.getXMax() - groundProfile.getXMin()) / nSteps;
      double dx = 1e-8;
      double y = (groundProfile.getYMax() - groundProfile.getYMin()) / 2.0;
      double z = 0.0;

      for (int i = 0; i < nSteps; i++)
      {
         double x = i * xStep;
         double dzdx = (groundProfile.heightAt(x + dx, y, z) - groundProfile.heightAt(x, y, z)) / dx;
         Vector3d numericalSurfaceNormal = new Vector3d(-dzdx, 0.0, 1.0);
         numericalSurfaceNormal.normalize();

         Vector3d surfaceNormalFromGroundProfile = new Vector3d();
         groundProfile.surfaceNormalAt(x, y, z, surfaceNormalFromGroundProfile);
         JUnitTools.assertTuple3dEquals(numericalSurfaceNormal, surfaceNormalFromGroundProfile, epsilon);
      }
   }
}
