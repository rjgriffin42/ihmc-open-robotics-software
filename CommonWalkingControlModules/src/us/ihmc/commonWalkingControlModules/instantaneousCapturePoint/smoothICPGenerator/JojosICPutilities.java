package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator;


import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

public class JojosICPutilities
{
   public JojosICPutilities()
   {
   }

   public static void extrapolateDCMpos(DenseMatrix64F constCoPcurrentStep, double time, double dcmConst, DenseMatrix64F ficalICPcurrentFootStep,
           DenseMatrix64F finalDoubleSupportICP)
   {
      double exponentialTerm = Math.exp(time / dcmConst);
      DenseMatrix64F tempVect = new DenseMatrix64F(3, 1);
      CommonOps.sub(ficalICPcurrentFootStep, constCoPcurrentStep, tempVect);
      CommonOps.scale(exponentialTerm, tempVect);
      CommonOps.add(constCoPcurrentStep, tempVect, finalDoubleSupportICP);
   }

   public static void extrapolateDCMposAndVel(DenseMatrix64F constCoPcurrentStep, double time, double dcmConst, DenseMatrix64F ficalICPcurrentFootStep,
           DenseMatrix64F finalDoubleSupportICPpos, DenseMatrix64F finalDoubleSupportICPvel)
   {
      double exponentialTerm = Math.exp(time / dcmConst);
      DenseMatrix64F tempVect = new DenseMatrix64F(3, 1);
      CommonOps.sub(ficalICPcurrentFootStep, constCoPcurrentStep, tempVect);
      CommonOps.scale(exponentialTerm, tempVect);
      CommonOps.add(constCoPcurrentStep, tempVect, finalDoubleSupportICPpos);
      CommonOps.scale(1 / dcmConst, tempVect, finalDoubleSupportICPvel);
   }


   public static void discreteIntegrateCoMAndGetCoMVelocity(double sampleTime, double dcmConst, DenseMatrix64F icp, DenseMatrix64F comPosition,
           DenseMatrix64F comVelocity)
   {
      double exponentialFactor = Math.exp(-sampleTime / dcmConst);
      DenseMatrix64F tempMatrix = new DenseMatrix64F(3, 1);

      CommonOps.sub(comPosition, icp, tempMatrix);
      CommonOps.scale(exponentialFactor, tempMatrix);
      CommonOps.add(icp, tempMatrix, comPosition);

      CommonOps.sub(comPosition, icp, comVelocity);
      CommonOps.scale(-1 / dcmConst, comVelocity);
   }

}
