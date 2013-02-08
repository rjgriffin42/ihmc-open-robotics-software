package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.ArrayList;

import us.ihmc.utilities.linearDynamicSystems.ComplexConjugateMode;
import us.ihmc.utilities.linearDynamicSystems.EigenvalueDecomposer;
import us.ihmc.utilities.linearDynamicSystems.SingleRealMode;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.dataStructures.ComplexNumber;

import com.mathworks.jama.Matrix;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class CoMHeightTimeDerivativesSmoother
{
   private final double dt;
   
//   private final double maximumAcceleration = 10.0;
   
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private final DoubleYoVariable inputComHeight = new DoubleYoVariable("inputComHeight", registry);
   private final DoubleYoVariable inputComHeightVelocity = new DoubleYoVariable("inputComHeightVelocity", registry);
   private final DoubleYoVariable inputComHeightAcceleration = new DoubleYoVariable("inputComHeightAcceleration", registry);
   
   private final DoubleYoVariable smoothComHeight = new DoubleYoVariable("smoothComHeight", registry);
   private final DoubleYoVariable smoothComHeightVelocity = new DoubleYoVariable("smoothComHeightVelocity", registry);
   private final DoubleYoVariable smoothComHeightAcceleration = new DoubleYoVariable("smoothComHeightAcceleration", registry);
   private final DoubleYoVariable smoothComHeightJerk = new DoubleYoVariable("smoothComHeightJerk", registry);
   
   private final DoubleYoVariable comHeightGain = new DoubleYoVariable("comHeightGain", registry);
   private final DoubleYoVariable comHeightVelocityGain = new DoubleYoVariable("comHeightVelocityGain", registry);
   private final DoubleYoVariable comHeightAccelerationGain = new DoubleYoVariable("comHeightAccelerationGain", registry);

   private final DoubleYoVariable eigenValueOneReal = new DoubleYoVariable("eigenValueOneReal", registry);
   private final DoubleYoVariable eigenValueOneImag = new DoubleYoVariable("eigenValueOneImag", registry);
   private final DoubleYoVariable eigenValueTwoReal = new DoubleYoVariable("eigenValueTwoReal", registry);
   private final DoubleYoVariable eigenValueTwoImag = new DoubleYoVariable("eigenValueTwoImag", registry);
   private final DoubleYoVariable eigenValueThreeReal = new DoubleYoVariable("eigenValueThreeReal", registry);
   private final DoubleYoVariable eigenValueThreeImag = new DoubleYoVariable("eigenValueThreeImag", registry);

   private double maximumAcceleration = 3.0 * 9.81;
   private double minimumAcceleration = 9.81;
   private double maximumJerk = maximumAcceleration/0.1;

   
   public CoMHeightTimeDerivativesSmoother(double dt, YoVariableRegistry parentRegistry)
   {
      this.dt = dt;

//      comHeightGain.set(200.0); // * 0.001/dt); //200.0;
//      comHeightVelocityGain.set(80.0); // * 0.001/dt); // 80.0;
//      comHeightAccelerationGain.set(10.0); // * 0.001/dt); // 10.0

      double w0 = 15.0;
      double w1 = 15.0;
      double zeta1 = 0.7;
      
      computeGainsByPolePlacement(w0, w1, zeta1);
      parentRegistry.addChild(registry);
      computeEigenvalues();
   }
   
   public void setMaximumAcceleration(double maximumAcceleration)
   {
      this.maximumAcceleration = maximumAcceleration;
   }
   
   public void setMinimumAcceleration(double minimumAcceleration)
   {
      this.minimumAcceleration = minimumAcceleration;
   }
   
   public void computeGainsByPolePlacement(double w0, double w1, double zeta1)
   {
      comHeightGain.set(w0*w1*w1);
      comHeightVelocityGain.set(w1*w1 + 2.0 * zeta1 * w1 * w0);
      comHeightAccelerationGain.set(w0 + 2.0 * zeta1 * w1);
   }
   
   public void computeEigenvalues()
   {
      double[][] matrixAValues = new double[][]{{0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}, 
            {-comHeightGain.getDoubleValue(), -comHeightVelocityGain.getDoubleValue(), -comHeightAccelerationGain.getDoubleValue()}};
      
      Matrix matrixA = new Matrix(matrixAValues);
      EigenvalueDecomposer eigenvalueDecomposer = new EigenvalueDecomposer(matrixA);
      
      ComplexNumber[] eigenvalues = eigenvalueDecomposer.getEigenvalues();
      eigenValueOneReal.set(eigenvalues[0].real());
      eigenValueOneImag.set(eigenvalues[0].imag());
      eigenValueTwoReal.set(eigenvalues[1].real());
      eigenValueTwoImag.set(eigenvalues[1].imag());
      eigenValueThreeReal.set(eigenvalues[2].real());
      eigenValueThreeImag.set(eigenvalues[2].imag());
      
      
      ArrayList<SingleRealMode> realModes = eigenvalueDecomposer.getRealModes();
      ArrayList<ComplexConjugateMode> complexConjugateModes = eigenvalueDecomposer.getComplexConjugateModes();
     
      for (SingleRealMode realMode : realModes)
      {
         System.out.println(realMode);
      }
      
      for (ComplexConjugateMode complexConjugateMode : complexConjugateModes)
      {
         System.out.println(complexConjugateMode);
      }
   }
   

   public void smooth(CoMHeightTimeDerivativesData heightZDataOutputToPack, CoMHeightTimeDerivativesData heightZDataInput)
   {
      double heightIn = heightZDataInput.getComHeight();
      double heightVelocityIn = heightZDataInput.getComHeightVelocity();
      double heightAccelerationIn = heightZDataInput.getComHeightAcceleration();
      
      inputComHeight.set(heightIn);
      inputComHeightVelocity.set(heightVelocityIn);
      inputComHeightAcceleration.set(heightAccelerationIn);
      
      
      double heightError = heightIn - smoothComHeight.getDoubleValue();
      double velocityError = heightVelocityIn - smoothComHeightVelocity.getDoubleValue();
      double accelerationError = heightAccelerationIn - smoothComHeightAcceleration.getDoubleValue();
      
      double jerk = comHeightAccelerationGain.getDoubleValue() * accelerationError + comHeightVelocityGain.getDoubleValue() * velocityError + comHeightGain.getDoubleValue() * heightError;
      jerk = MathTools.clipToMinMax(jerk, -maximumJerk, maximumJerk);
      
      smoothComHeightJerk.set(jerk);
      
      smoothComHeightAcceleration.add(jerk * dt);
      
      if (smoothComHeightAcceleration.getDoubleValue() > maximumAcceleration)
      {
         smoothComHeightAcceleration.set(10.0);
      }
      if (smoothComHeightAcceleration.getDoubleValue() < -minimumAcceleration)
      {
         smoothComHeightAcceleration.set(-10.0);
      }
      
      smoothComHeightVelocity.add(smoothComHeightAcceleration.getDoubleValue() * dt);
      smoothComHeight.add(smoothComHeightVelocity.getDoubleValue() * dt);
      
      
      heightZDataOutputToPack.setComHeight(smoothComHeight.getDoubleValue());
      heightZDataOutputToPack.setComHeightVelocity(smoothComHeightVelocity.getDoubleValue());
      heightZDataOutputToPack.setComHeightAcceleration(smoothComHeightAcceleration.getDoubleValue());
      
   }

   public void initialize(CoMHeightTimeDerivativesData comHeightDataIn)
   {
      smoothComHeight.set(comHeightDataIn.getComHeight());
      smoothComHeightVelocity.set(comHeightDataIn.getComHeightVelocity());
      smoothComHeightAcceleration.set(comHeightDataIn.getComHeightAcceleration());
      
      
   }
}
