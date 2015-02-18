package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

public enum HumanoidArmPose
{   
   STAND_PREP, SMALL_CHICKEN_WINGS, LARGE_CHICKEN_WINGS, STRAIGHTEN_ELBOWS, SUPPINATE_ARMS_IN_A_LITTLE, ARMS_BACK, LARGER_CHICKEN_WINGS,
   ARMS_OUT_EXTENDED, SUPPINATE_ARMS_IN_MORE, SUPPINATE_ARMS_IN_A_LOT, SUPER_CHICKEN_WINGS, FLYING, FLYING_SUPPINATE_IN, FLYING_SUPPINATE_OUT,
   REACH_BACK, REACH_WAY_BACK, ARMS_03, REACH_FORWARD, REACH_WAY_FORWARD, ARM_STRAIGHT_DOWN, ARM_NINETY_ELBOW_DOWN, ARM_NINETY_ELBOW_FORWARD,
   ARM_NINETY_ELBOW_UP, ARM_FORTFIVE_ELBOW_UP, ARM_FORTFIVE_ELBOW_DOWN, ARM_OUT_TRICEP_EXERCISE, ARM_NINETY_ELBOW_DOWN2, ARM_NINETY_ELBOW_FORWARD2, ARM_NINETY_ELBOW_UP2, ARM_FORTFIVE_ELBOW_UP2, ARM_FORTFIVE_ELBOW_UP3, ARM_FORTFIVE_ELBOW_DOWN2, ARM_FORTFIVE_ELBOW_DOWN3, REACH_FAR_FORWARD, REACH_FAR_BACK;
 
   private static final double halfPi = Math.PI / 2.0;

   /**
    * Arm angles are as follows:
    * 
    * 1 - shoulder extensor (negative forward / positive back) min: 0.3 max: 0.6
    * 2 - shoulder adductor (negative out / positive in) min: -1.3 max: -0.4
    * 3 - elbow supinator (negative out / positive in) min: 0.05 max: 0.5
    * 4 - elbow extensor (negative up / positive down) min: -1.7 max: -0.9
    * 
    * @return armAngles
    */
   public double[] getArmJointAngles()
   {
      switch (this)
      {
         case STAND_PREP:
            return new double[]{0.3, -0.4, 0.05, -1.7};
         case SMALL_CHICKEN_WINGS:
            return new double[]{0.3, -0.6, 0.05, -1.7};
         case LARGE_CHICKEN_WINGS:
            return new double[]{0.3, -0.8, 0.05, -1.7};
         case STRAIGHTEN_ELBOWS:
            return new double[]{0.3, -0.6, 0.05, -1.0};
         case SUPPINATE_ARMS_IN_A_LITTLE:
            return new double[]{0.3, -0.6, 0.2, -1.7};
         case ARMS_BACK:
            return new double[]{0.6, -0.4, 0.05, -1.7};
         case LARGER_CHICKEN_WINGS:
            return new double[]{0.3, -1.0, 0.05, -1.7};
         case ARMS_OUT_EXTENDED:
            return new double[]{0.3, -1.0, 0.05, -0.9};
         case FLYING:
            return new double[]{0.3, -1.2, 0.05, -0.4};
         case FLYING_SUPPINATE_IN:
            return new double[]{0.3, -1.2, 1.0, -0.4};
         case FLYING_SUPPINATE_OUT:
            return new double[]{0.3, -1.2, -1.0, -0.4};
         case SUPPINATE_ARMS_IN_MORE:
            return new double[]{0.3, -0.4, 0.3, -1.7};
         case SUPPINATE_ARMS_IN_A_LOT:
            return new double[]{0.3, -0.4, 0.5, -1.7};
         case SUPER_CHICKEN_WINGS:
            return new double[]{0.3, -1.3, 0.05, -1.7};
            
         case REACH_BACK:
            return new double[]{1.0, -0.4, 0.05, -1.7};
         case REACH_WAY_BACK:
            return new double[]{1.0, -0.4, 0.05, -0.4};
         case ARMS_03:
            return new double[]{0.3, -0.4, 0.3, -1.7};
         case REACH_FORWARD:
            return new double[]{-0.6, -0.4, 0.05, -1.7};
         case REACH_WAY_FORWARD:
            return new double[]{-0.6, -0.4, 0.05, -0.4};
         case REACH_FAR_FORWARD:
            return new double[]{-0.8 * halfPi, -0.4, 0.0, 0.0};
         case REACH_FAR_BACK:
            return new double[]{ 0.8 * halfPi, -0.4, 0.0, 0.0};
            
            
         case ARM_STRAIGHT_DOWN:
            return new double[]{0.0, -0.5, 1.45, -0.53};

         case ARM_NINETY_ELBOW_DOWN:
            return new double[]{0.0, -halfPi, halfPi, -halfPi};
         case ARM_NINETY_ELBOW_DOWN2:
            return new double[]{halfPi / 2.0, -halfPi, halfPi / 2.0, -halfPi};
         case ARM_NINETY_ELBOW_FORWARD:
            return new double[]{0.0, -halfPi, 0.0, -halfPi};
         case ARM_NINETY_ELBOW_FORWARD2:
            return new double[]{halfPi / 2.0, -halfPi, -halfPi / 2.0, -halfPi};
         case ARM_NINETY_ELBOW_UP:
            return new double[]{0.0, -halfPi, -halfPi, -halfPi};
         case ARM_NINETY_ELBOW_UP2:
            return new double[]{-halfPi / 2.0, -halfPi, -halfPi / 2.0, -halfPi};
         case ARM_FORTFIVE_ELBOW_UP:
            return new double[]{0.0, -halfPi, -halfPi / 2.0, -halfPi};
         case ARM_FORTFIVE_ELBOW_UP2:
            return new double[]{-halfPi /2.0, -halfPi, 0.0, -halfPi};
         case ARM_FORTFIVE_ELBOW_UP3:
            return new double[]{halfPi /2.0, -halfPi, -halfPi, -halfPi};
         case ARM_FORTFIVE_ELBOW_DOWN:
            return new double[]{0.0, -halfPi, 0.6, -halfPi};
         case ARM_FORTFIVE_ELBOW_DOWN2:
            return new double[]{halfPi/ 2.0, -halfPi, 0.0, -halfPi};
         case ARM_FORTFIVE_ELBOW_DOWN3:
            return new double[]{-halfPi/ 2.0, -halfPi, halfPi, -halfPi};
            
         case ARM_OUT_TRICEP_EXERCISE:
            return new double[]{0.0, -1.4, 1.4, 0.05};
            
                     
         default:
            throw new RuntimeException("Shouldn't get here!");
      }
   }


   public double getDesiredElbowAngle()
   {
      return getArmJointAngles()[3];
   }

   /**
    * Get the orientation of the upper-arm w.r.t. to the chest. By using IK solver as in DiagnosticBehavior this is robot agnostic whereas the getArmJointAngles() is Valkyrie specific. 
    * @return
    */
   public double[] getDesiredUpperArmYawPitchRoll()
   {
      switch (this)
      {
         case STAND_PREP:
            return new double[]{-0.0485, 0.3191, 0.3852};
         case SMALL_CHICKEN_WINGS:
            return new double[]{-0.0435, 0.3279, 0.5865};
         case LARGE_CHICKEN_WINGS:
            return new double[]{-0.0368, 0.3356, 0.7884};
         case STRAIGHTEN_ELBOWS:
            return new double[]{-0.0435, 0.3279, 0.5865};
         case SUPPINATE_ARMS_IN_A_LITTLE:
            return new double[]{-0.1795, 0.4080, 0.5375};
         case ARMS_BACK:
            return new double[]{-0.0565, 0.6187, 0.3676};
         case LARGER_CHICKEN_WINGS:
            return new double[]{-0.0286, 0.3419, 0.9909};
         case ARMS_OUT_EXTENDED:
            return new double[]{-0.0286, 0.3419, 0.9909};
         case FLYING:
            return new double[]{-0.0192, 0.3465, 1.1938};
         case FLYING_SUPPINATE_IN:
            return new double[]{-0.8201, 1.1406, 0.5912};
         case FLYING_SUPPINATE_OUT:
            return new double[]{0.3871, -0.6305, 1.1278};
         case SUPPINATE_ARMS_IN_MORE:
            return new double[]{-0.3004, 0.4030, 0.2957};
         case SUPPINATE_ARMS_IN_A_LOT:
            return new double[]{-0.5133, 0.4530, 0.2070};
         case SUPER_CHICKEN_WINGS:
            return new double[]{-0.0142, 0.3481, 1.2954};
            
         case REACH_BACK:
            return new double[]{-0.0877, 1.0177, 0.3257};
         case REACH_WAY_BACK:
            return new double[]{-0.0877, 1.0177, 0.3257};
         case ARMS_03:
            return new double[]{-0.3004, 0.4030, 0.2957};
         case REACH_FORWARD:
            return new double[]{-0.0550, -0.5798, 0.4306};
         case REACH_WAY_FORWARD:
            return new double[]{-0.0550, -0.5798, 0.4306};
         case REACH_FAR_FORWARD:
            return new double[]{0.0, -1.2566, 0.4};
         case REACH_FAR_BACK:
            return new double[]{ 0.0, 1.2566, 0.4};
            
            
         case ARM_STRAIGHT_DOWN:
            return new double[]{0.0, -0.0, 0.5};

         case ARM_NINETY_ELBOW_DOWN:
            return new double[]{-Math.PI/4.0, Math.PI/2.0, Math.PI/4.0};
         case ARM_NINETY_ELBOW_DOWN2:
            return new double[]{-0.1925, Math.PI/2.0, 1.3782};
         case ARM_NINETY_ELBOW_FORWARD:
            return new double[]{0.0, 0.0, Math.PI/2.0};
         case ARM_NINETY_ELBOW_FORWARD2:
            return new double[]{0.0, 0.0, Math.PI/2.0};
         case ARM_NINETY_ELBOW_UP:
            return new double[]{Math.PI/4.0, -Math.PI/2.0, Math.PI/4.0};
         case ARM_NINETY_ELBOW_UP2:
            return new double[]{0.1925, -Math.PI/2.0, 1.3782};
         case ARM_FORTFIVE_ELBOW_UP:
            return new double[]{0.0, -Math.PI/4.0, Math.PI/2.0};
         case ARM_FORTFIVE_ELBOW_UP2:
            return new double[]{0.0, -Math.PI/4.0, Math.PI/2.0};
         case ARM_FORTFIVE_ELBOW_UP3:
            return new double[]{0.0, -Math.PI/4.0, Math.PI/2.0};
         case ARM_FORTFIVE_ELBOW_DOWN:
            return new double[]{0.0, 0.6, Math.PI/2.0};
         case ARM_FORTFIVE_ELBOW_DOWN2:
            return new double[]{0.0, Math.PI/4.0, Math.PI/2.0};
         case ARM_FORTFIVE_ELBOW_DOWN3:
            return new double[]{0.0, Math.PI/4.0, Math.PI/2.0};
            
         case ARM_OUT_TRICEP_EXERCISE:
            return new double[]{-0.7780, 1.3298, 0.7780};
            
                     
         default:
            throw new RuntimeException("Shouldn't get here!");
      }
   }
}
