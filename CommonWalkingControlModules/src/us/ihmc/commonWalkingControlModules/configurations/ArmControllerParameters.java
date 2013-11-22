package us.ihmc.commonWalkingControlModules.configurations;

public interface ArmControllerParameters
{
   public abstract double getArmJointspaceKp();
   public abstract double getArmJointspaceZeta();
   public abstract double getArmJointspaceKi();
   public abstract double getArmJointspaceMaxIntegralError();
   public abstract double getArmJointspaceMaxAcceleration();
   public abstract double getArmJointspaceMaxJerk();

   public abstract double getArmTaskspaceKp();
   public abstract double getArmTaskspaceZeta();
   public abstract double getArmTaskspaceKi();
   public abstract double getArmTaskspaceMaxIntegralError();
   public abstract double getArmTaskspaceMaxAcceleration();
   public abstract double getArmTaskspaceMaxJerk();
}
