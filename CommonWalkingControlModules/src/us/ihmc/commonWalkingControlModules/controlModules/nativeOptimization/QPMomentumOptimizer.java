package us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.utilities.exeptions.NoConvergenceException;

public abstract class QPMomentumOptimizer implements MomentumOptimizerInterface
{
   static final int nPointsPerPlane = 4;
   static final int nSupportVectorsPerPoint=4;
   static final int nPlanes=4;
   static final int nWrench=6;
   static final int nRho = nPlanes*nPointsPerPlane*nSupportVectorsPerPoint;
   final int nDoF;

   protected DenseMatrix64F
      A, b, C,    //quad(vd)
      Js, ps, Ws,  //quad(vd)
      Jp, pp,      //quad(vd)

      WRho, Lambda, //reg

      WRhoSmoother, //quad(rho)

      rhoPrevMean, WRhoCoPPenalty, //quad(rho)

      QRho, c, rhoMin;  //constraints
     
   public DenseMatrix64F vd, rho;
   public DenseMatrix64F prevVd, prevRho;
   double optVal;
   int iter;

   public QPMomentumOptimizer(int _nDoF)
   {
      nDoF = _nDoF;
      vd = new DenseMatrix64F(nDoF,1);
      rho = new DenseMatrix64F(getRhoSize(),1);
      prevVd = new DenseMatrix64F(nDoF,1);
      prevRho = new DenseMatrix64F(getRhoSize(),1);

      //Size of constraints may change on the fly
      Js = new DenseMatrix64F(nDoF,nDoF);
      ps = new DenseMatrix64F(nDoF,1);
      Jp = new DenseMatrix64F(nDoF, nDoF);
      pp = new DenseMatrix64F(nDoF, 1);
      Ws = new DenseMatrix64F(nDoF, nDoF);
   }


   @Override
   public void setInputs(DenseMatrix64F _a, DenseMatrix64F _b, DenseMatrix64F _momentumDotWeight, DenseMatrix64F _jSecondary, DenseMatrix64F _pSecondary,
         DenseMatrix64F _weightMatrixSecondary, DenseMatrix64F _WRho, DenseMatrix64F _Lambda, DenseMatrix64F _WRhoSmoother, DenseMatrix64F _rhoPrevAvg,
         DenseMatrix64F _WRhoCop, DenseMatrix64F _QRho, DenseMatrix64F _c, DenseMatrix64F _rhoMin)
   {
      A=_a;
      b=_b;
      C=_momentumDotWeight;
      if (!(A.numRows == nWrench && A.numCols == nDoF && b.numRows == nWrench && b.numCols == 1 && C.numRows == nWrench && C.numCols == nWrench))
         throw new RuntimeException("Incorrect input size A/b/c");

      //Constraint size may change, so copy
      CommonOps.insert(_jSecondary, Js, 0, 0);
      CommonOps.insert(_pSecondary, ps, 0, 0);
      CommonOps.insert(_weightMatrixSecondary, Ws, 0, 0);
      
      Jp.set(0, 0, Double.NaN); //when calling as a NULL solver, marke primary constraint as not used
      pp.set(0, 0, Double.NaN);

      WRho=_WRho;
      Lambda=_Lambda;
      
      WRhoSmoother =_WRhoSmoother;

      rhoPrevMean = _rhoPrevAvg;
      WRhoCoPPenalty = _WRhoCop;

      QRho = _QRho; 
      c = _c;
      rhoMin = _rhoMin;

   }

   @Override
   public void setInputs(DenseMatrix64F a, DenseMatrix64F b, DenseMatrix64F momentumDotWeight, DenseMatrix64F jPrimary, DenseMatrix64F pPrimary,
         DenseMatrix64F jSecondary, DenseMatrix64F pSecondary, DenseMatrix64F weightMatrixSecondary, DenseMatrix64F WRho, DenseMatrix64F Lambda,
         DenseMatrix64F WRhoSmoother, DenseMatrix64F rhoPrevAvg, DenseMatrix64F WRhoCop, DenseMatrix64F QRho, DenseMatrix64F c, DenseMatrix64F rhoMin)
   {
      setInputs(a, b, momentumDotWeight, jSecondary, pSecondary,
         weightMatrixSecondary, WRho, Lambda, WRhoSmoother, rhoPrevAvg,
         WRhoCop, QRho, c, rhoMin);
      
      //note: the nrow(Jp vd=pp) may change
      CommonOps.insert(jPrimary, Jp, 0, 0);
      CommonOps.insert(pPrimary, pp, 0, 0);
   }

   @Override
   public void reset()
   {
      Jp.zero();
      pp.zero();
      Js.zero();
      ps.zero();
   }


   @Override
   public int getRhoSize()
   {
      return nPlanes*nPointsPerPlane*nSupportVectorsPerPoint;
   }

   @Override
   public int getNSupportVectors()
   {
      return nSupportVectorsPerPoint;
   }

   @Override
   public int getNPointsPerPlane()
   {
      return nPointsPerPlane;
   }

   @Override
   public void setRateOfChangeOfGroundReactionForceRegularization(DenseMatrix64F _wRhoSmoother)
   {
      WRhoSmoother = _wRhoSmoother;
   }


   @Override
   public int getNPlanes()
   {
      return nPlanes;
   } 
   

   @Override
   public DenseMatrix64F getOutputRho()
   {
      return rho;
   }
   
   
   @Override
   public DenseMatrix64F getOutputJointAccelerations()
   {
      return vd;
   }

   @Override
   public double getOutputOptVal()
   {
      return optVal;
   }

   @Override
   public abstract int solve() throws NoConvergenceException;

}
