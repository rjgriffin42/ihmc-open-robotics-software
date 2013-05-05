package us.ihmc.darpaRoboticsChallenge.handControl;

public class FingerJoint
{
   private final String name;
   private double q;
   private double qd;
   
   
   private double tau;
   private double qDesired;
   private double qdDesired;
   
   private double kp;
   private double kd;
   
   public FingerJoint(String name)
   {
      super();
      this.name = name;
   }

   public String getName()
   {
      return name;
   }

   public double getQ()
   {
      return q;
   }

   public double getQd()
   {
      return qd;
   }

   public double getTau()
   {
      return tau;
   }

   public double getqDesired()
   {
      return qDesired;
   }

   public double getQdDesired()
   {
      return qdDesired;
   }

   public double getKp()
   {
      return kp;
   }

   public double getKd()
   {
      return kd;
   }

   public void setQ(double q)
   {
      this.q = q;
   }

   public void setQd(double qd)
   {
      this.qd = qd;
   }

   public void setTau(double tau)
   {
      this.tau = tau;
   }

   public void setqDesired(double qDesired)
   {
      this.qDesired = qDesired;
   }

   public void setQdDesired(double qdDesired)
   {
      this.qdDesired = qdDesired;
   }

   public void setKp(double kp)
   {
      this.kp = kp;
   }

   public void setKd(double kd)
   {
      this.kd = kd;
   }

}
