package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

public class ICPControlGains
{
   private double kpParallelToMotion;
   private double kpOrthogonalToMotion;
   private double ki;
   private double kiBleedOff = 1.0;

   public ICPControlGains()
   {
   }

   public double getKpParallelToMotion()
   {
      return kpParallelToMotion;
   }

   public void setKpParallelToMotion(double kpParallelToMotion)
   {
      this.kpParallelToMotion = kpParallelToMotion;
   }

   public double getKpOrthogonalToMotion()
   {
      return kpOrthogonalToMotion;
   }

   public void setKpOrthogonalToMotion(double kpOrthogonalToMotion)
   {
      this.kpOrthogonalToMotion = kpOrthogonalToMotion;
   }

   public double getKi()
   {
      return ki;
   }

   public void setKi(double ki)
   {
      this.ki = ki;
   }

   public double getKiBleedOff()
   {
      return kiBleedOff;
   }

   public void setKiBleedOff(double kiBleedOff)
   {
      this.kiBleedOff = kiBleedOff;
   }
}
