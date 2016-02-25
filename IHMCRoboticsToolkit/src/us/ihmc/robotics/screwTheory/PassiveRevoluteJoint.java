package us.ihmc.robotics.screwTheory;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class PassiveRevoluteJoint extends RevoluteJoint
{
   private boolean hasBeenInitialized = false;
   private final boolean isPartOfClosedKinematicLoop;

   /**
    * In this type of joint:
    * 
    *    1)  You can NOT set any inputs (Q, Qd, or Tau) --> because they are not actuated 
    *    joints, in other words they cannot be controlled because their motion is determined 
    *    by that of another non-passive joint.
    *    
    *    2) getTau() should always return a zero because, since the joint is NOT actuated,
    *    there is no torque.
    */
   
   public PassiveRevoluteJoint(String name, RigidBody predecessor, ReferenceFrame beforeJointFrame, FrameVector jointAxis, boolean isPartOfClosedKinematicLoop)
   {
      super(name, predecessor, beforeJointFrame, jointAxis);
      this.isPartOfClosedKinematicLoop = isPartOfClosedKinematicLoop;
   }
   
   /**
    * This method should only be called once. Do not use as a setQ(), or setQD()
    */
   public void initializePositionAndVelocity(double q, double qd)
   {
      if(!hasBeenInitialized)
      {
         this.q = q;
         this.qd = qd;         
      }
      else
      {
         throw new RuntimeException(getName() + " has already been initialized!");
      }
      hasBeenInitialized = true;
   }
   
   /**
    * Torque on a passive joint is always zero
    */
   @Override
   public void getTauMatrix(DenseMatrix64F matrix)
   {
      MathTools.checkIfInRange(matrix.getNumRows(), 1, Integer.MAX_VALUE);
      MathTools.checkIfInRange(matrix.getNumCols(), 1, Integer.MAX_VALUE);
      matrix.set(0, 0, 0);
   }

   @Override
   public void setDesiredAccelerationToZero()
   {
      throw new RuntimeException("Cannot set acceleration of a passive joint");
   }

   @Override
   public void setTorqueFromWrench(Wrench jointWrench)
   {
      throw new RuntimeException("Cannot set torque of a passive joint");
   }

   @Override
   public void setDesiredAcceleration(DenseMatrix64F matrix, int rowStart)
   {
      throw new RuntimeException("Cannot set acceleration of a passive joint");
   }

   @Override
   public void setQ(double q)
   {
      throw new RuntimeException("Cannot set position of a passive joint");
   }

   @Override
   public void setQd(double qd)
   {
      throw new RuntimeException("Cannot set velocity of a passive joint");
   }

   @Override
   public void setQdd(double qdd)
   {
      throw new RuntimeException("Cannot set acceleration of a passive joint");
   }

   @Override
   public void setQddDesired(double qddDesired)
   {
      throw new RuntimeException("Cannot set acceleration of a passive joint");
   }

   @Override
   public void setTau(double tau)
   {
      throw new RuntimeException("Cannot set torque of a passive joint");  
   }

   @Override
   public void setConfiguration(DenseMatrix64F matrix, int rowStart)
   {
      throw new RuntimeException("Cannot set position of a passive joint");
   }

   @Override
   public void setVelocity(DenseMatrix64F matrix, int rowStart)
   {
      throw new RuntimeException("Cannot set velocity of a passive joint");
   }

   @Override
   public void setJointPositionVelocityAndAcceleration(InverseDynamicsJoint originalJoint)
   {
      throw new RuntimeException("Cannot set position, velocity, or acceleration of a passive joint");
   }

   @Override
   public void setQddDesired(InverseDynamicsJoint originalJoint)
   {
      throw new RuntimeException("Cannot set acceleration of a passive joint");
   }

   @Override
   public boolean isPassiveJoint()
   {
      return true;
   }
   
   public boolean isPartOfClosedKinematicLoop()
   {
      return isPartOfClosedKinematicLoop;
   }
   
   void updateQ(double q)
   {
      if (Double.isNaN(q))
         throw new RuntimeException("q is NaN! this = " + this);
      this.q = q;
      afterJointFrame.setAndUpdate(q);      
   }
   
   void updateQd(double qd)
   {
      this.qd = qd;
   }
}
