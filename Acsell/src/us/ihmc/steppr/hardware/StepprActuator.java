package us.ihmc.steppr.hardware;

public enum StepprActuator
{
   LEFT_ANKLE_RIGHT("leftAnkleRightActuator",1.01e-3, 1.152, 0.587, 0, 1, -1), //measured current is -1*commanded current
   LEFT_ANKLE_LEFT("leftAnkleLeftActuator", 1.01e-3, 1.152, 0.587, 0, 2, 1),
   LEFT_KNEE("leftKneeActuator", 1.31e-03, 2.286, 0.749, 0, 3, -1),
   LEFT_HIP_Y("leftHipYActuator", 1.32e-3, 0.893, 0.744, 0, 4, 1),
   LEFT_HIP_Z("leftHipZActuator", 3.53e-4, 0.702, 0.299, 0, 5, 1),
   LEFT_HIP_X("leftHipXActuator", 1.32e-03, 2.286, 0.749, 0, 6, 1),
   
   RIGHT_ANKLE_RIGHT("rightAnkleRightActuator",1.01e-3, 1.152, .587, 1, 1, -1),
   RIGHT_ANKLE_LEFT("rightAnkleLeftActuator", 1.01e-3, 1.152, .587, 1, 2, 1),
   RIGHT_KNEE("rightKneeActuator", 1.31e-3, 2.286, .749, 1, 3, -1),
   RIGHT_HIP_Y("rightHipYActuator", 1.32e-3, 0.893, .744, 1, 4, 1),
   RIGHT_HIP_Z("rightHipZActuator", 3.53e-4, 0.702, .299, 1, 5, 1),
   RIGHT_HIP_X("rightHipXActuator", 1.32e-3, 2.286, .749, 1, 6, -1),
   
   TORSO_X("torsoXActuator", 0, 0.398, .104, 2, 1, 1),
   TORSO_Y("torsoYActuator", 0, 0.45, .192, 2, 2, 1),
   TORSO_Z("torsoZActuator", 0, 0.316, .104, 2, 3, 1);
   
   
   public static final StepprActuator[] values = values();
   
   private final String name;
   private final double motorInertia;
   private final double ktSinesoidal;
   private final double Km;
   private final int bus;
   private final int index;
   private final int SensedCurrentToTorqueDirection;
   public double motorScalingConstantFromDiagnostics = 1.14;

   private StepprActuator(String name, double motorInertial, double ktPeak, double km, int bus, int index, int SensedCurrentToTorqueDirection)
   {
      this.name = name;
      this.motorInertia=motorInertial;
      this.ktSinesoidal = (ktPeak * Math.sqrt(3.0) / 2.0)/ motorScalingConstantFromDiagnostics;
      this.Km = km;
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
   
   
}
