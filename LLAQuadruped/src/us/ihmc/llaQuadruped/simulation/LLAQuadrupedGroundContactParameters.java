package us.ihmc.llaQuadruped.simulation;

import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactParameters;

public class LLAQuadrupedGroundContactParameters implements QuadrupedGroundContactParameters
{
   @Override
   public double getZStiffness()
   {
      return 1000.0;
   }
   
   @Override
   public double getZDamping()
   {
      return 400.0;
   }
   
   @Override
   public double getXYStiffness()
   {
      return 4000.0;
   }
   
   @Override
   public double getXYDamping()
   {
      return 500.0;
   }
}
