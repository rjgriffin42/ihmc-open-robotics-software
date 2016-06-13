package us.ihmc.simulationconstructionset.util.ground;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class WavyGroundProfileTest extends GroundProfileTest
{
   @Override
   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout=300000)
   public void testSurfaceNormalGridForSmoothTerrainUsingHeightMap()
   {
      super.testSurfaceNormalGridForSmoothTerrainUsingHeightMap();
   }
   
   @Override
   public GroundProfile3D getGroundProfile()
   {
      return new WavyGroundProfile();
   }

   @Override
   public double getMaxPercentageOfAllowableValleyPoints()
   {
      return 0.0;
   }

   @Override
   public double getMaxPercentageOfAllowablePeakPoints()
   {
      return 0.0;
   }

   @Override
   public double getMaxPercentageOfAllowableDropOffs()
   {
      //TODO: 30 percent is too high. Need to actually compute the surface normal properly...
      return 0.3;
   }
}
