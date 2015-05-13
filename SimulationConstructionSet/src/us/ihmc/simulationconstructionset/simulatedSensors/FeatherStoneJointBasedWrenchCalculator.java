package us.ihmc.simulationconstructionset.simulatedSensors;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.simulationconstructionset.JointWrenchSensor;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.utilities.screwTheory.Wrench;

public class FeatherStoneJointBasedWrenchCalculator implements WrenchCalculatorInterface
{
   private final String forceSensorName;
   private final OneDegreeOfFreedomJoint forceTorqueSensorJoint;
   private boolean doWrenchCorruption = false;
   
   
   private final DenseMatrix64F wrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
   private final DenseMatrix64F corruptionMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
   
   public FeatherStoneJointBasedWrenchCalculator(String forceSensorName,
         OneDegreeOfFreedomJoint forceTorqueSensorJoint)
   {
      this.forceSensorName = forceSensorName;
      this.forceTorqueSensorJoint = forceTorqueSensorJoint;
   }
   
   public String getName()
   {
      return forceSensorName;
   }

   private Vector3d force = new Vector3d();
   private Vector3d tau = new Vector3d();
   
   public void calculate()
   {
      JointWrenchSensor sensor = forceTorqueSensorJoint.getJointWrenchSensor();

      sensor.getJointForce(force);
      sensor.getJointTorque(tau);

      wrenchMatrix.zero();

      // Get the action wrench from the joint sensor.
      wrenchMatrix.set(0, 0, tau.x);
      wrenchMatrix.set(1, 0, tau.y);
      wrenchMatrix.set(2, 0, tau.z);
      
      wrenchMatrix.set(3, 0, force.x);
      wrenchMatrix.set(4, 0, force.y);
      wrenchMatrix.set(5, 0, force.z);

      // Get the opposite to obtain the reaction wrench.
      CommonOps.scale(-1.0, wrenchMatrix);
      
      if(doWrenchCorruption)
      {
         for(int i = 0; i < Wrench.SIZE; i++)
         {
            wrenchMatrix.add(i, 0, corruptionMatrix.get(i,0));
         }
      }
   }


   public OneDegreeOfFreedomJoint getJoint()
   {
      return forceTorqueSensorJoint;
   }


   public DenseMatrix64F getWrench()
   {
      return wrenchMatrix;
   }
   
   public void corruptWrenchElement(int row, double value)
   {
      corruptionMatrix.add(row, 0, value);
   }
   
   public String toString()
   {
      return forceSensorName;
   }

   public void setDoWrenchCorruption(boolean value)
   {
      this.doWrenchCorruption = value;
   }
}
