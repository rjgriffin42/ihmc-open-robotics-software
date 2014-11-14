package us.ihmc.simulationconstructionset.simulatedSensors;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;

public interface WrenchCalculatorInterface
{

   public abstract String getName();
   public abstract void calculate();
   public abstract DenseMatrix64F getWrench();
   public abstract OneDegreeOfFreedomJoint getJoint();

}