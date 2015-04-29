package us.ihmc.wanderer.hardware;

import us.ihmc.acsell.hardware.AcsellActuator;


public enum WandererActuator implements AcsellActuator
{
   LEFT_ANKLE_RIGHT("leftAnkleRightActuator", 1.335e-3, 1.152, 0.587, 26.0, 0, 1, 1),
   LEFT_ANKLE_LEFT("leftAnkleLeftActuator", 1.335e-3, 1.152, 0.587, 26.0, 0, 2, 1),
   LEFT_KNEE("leftKneeActuator", 2.142e-3, 0.612, 0.991, 50.0, 0, 3, 1),
   LEFT_HIP_Y("leftHipYActuator", 2.142e-3, 0.612, 0.991, 50.0, 0, 4, 1),
   LEFT_HIP_Z("leftHipZActuator", 3.55e-4, 0.702, 0.299, 18.1, 0, 5, 1),
   LEFT_HIP_X("leftHipXActuator", 1.551e-3, 2.286, 0.749, 11.0, 0, 6, 1),

   RIGHT_ANKLE_RIGHT("rightAnkleRightActuator", 1.335e-3, 1.152, .587, 26.0, 1, 1, 1),
   RIGHT_ANKLE_LEFT("rightAnkleLeftActuator", 1.335e-3, 1.152, .587, 26.0, 1, 2, 1),
   RIGHT_KNEE("rightKneeActuator", 2.142e-3, 0.612, 0.991, 50.0, 1, 3, 1),
   RIGHT_HIP_Y("rightHipYActuator", 2.142e-3, 0.612, 0.991, 50.0, 1, 4, 1),
   RIGHT_HIP_Z("rightHipZActuator", 3.55e-4, 0.702, 0.299, 18.1, 1, 5, 1),
   RIGHT_HIP_X("rightHipXActuator", 1.551e-3, 2.286, 0.749, 11.0, 1, 6, 1),

   TORSO_X("torsoXActuator", 0.069e-3, 0.398, .104,  6.8, 2, 1, 1),
   TORSO_Y("torsoYActuator", 0.197e-3, 0.450, .192, 18.3, 2, 2, 1),
   TORSO_Z("torsoZActuator", 0.069e-3, 0.316, .104, 10.8, 2, 3, 1);

   public static final WandererActuator[] values = values();

   private final String name;
   private final double motorInertia;
   private final double ktSinesoidal;
   private final double Km;
   private final double currentLimit;
   private final int bus;
   private final int index;
   private final int SensedCurrentToTorqueDirection;
   public static final double motorScalingConstantFromDiagnostics = 1.0;

   private WandererActuator(String name, double motorInertial, double ktPeak, double km, double currentLimit, int bus, int index, int SensedCurrentToTorqueDirection)
   {
      this.name = name;
      this.motorInertia = motorInertial;
      this.ktSinesoidal = (ktPeak * Math.sqrt(3.0) / 2.0) / motorScalingConstantFromDiagnostics;
      this.Km = km;
      this.currentLimit = currentLimit;
      this.bus = bus;
      this.index = index;
      this.SensedCurrentToTorqueDirection = SensedCurrentToTorqueDirection;
   }

   public String getName()
   {
      return name;
   }

   public double getKt()
   {
      return ktSinesoidal;
   }

   public double getKm()
   {
      return Km;
   }

   public int getBus()
   {
      return bus;
   }

   public int getIndex()
   {
      return index;
   }

   public int getSensedCurrentToTorqueDirection()
   {
      return SensedCurrentToTorqueDirection;
   }

   public double getMotorInertia()
   {
      return motorInertia;
   }
   
   public double getCurrentLimit()
   {
      return currentLimit;
   }

}
