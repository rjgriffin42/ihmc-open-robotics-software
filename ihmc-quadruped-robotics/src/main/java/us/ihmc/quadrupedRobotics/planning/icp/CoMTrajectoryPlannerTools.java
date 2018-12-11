package us.ihmc.quadrupedRobotics.planning.icp;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFrameVector3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.quadrupedRobotics.planning.ContactState;

public class CoMTrajectoryPlannerTools
{
   static void constructDesiredCoMPosition(FixedFramePoint3DBasics desiredCoMPositionToPack, FramePoint3DReadOnly firstCoefficient,
                                           FramePoint3DReadOnly secondCoefficient, FramePoint3DReadOnly desiredCoPPosition, ContactState contactState,
                                           double timeInPhase, double omega, double gravityZ, double nominalCoMHeight)
   {
      double firstCoefficientPositionMultiplier = CoMTrajectoryPlannerTools.getFirstCoefficientPositionMultiplier(contactState, timeInPhase, omega);
      double secondCoefficientPositionMultiplier = CoMTrajectoryPlannerTools.getSecondCoefficientPositionMultiplier(contactState, timeInPhase, omega);
      double gravityPositionEffect = CoMTrajectoryPlannerTools.getGravityPositionEffect(contactState, timeInPhase, gravityZ);

      if (contactState == ContactState.IN_CONTACT)
      {
         desiredCoMPositionToPack.set(desiredCoPPosition);
         desiredCoMPositionToPack.addZ(nominalCoMHeight);
      }
      else
      {
         desiredCoMPositionToPack.setToZero();
      }
      desiredCoMPositionToPack.scaleAdd(firstCoefficientPositionMultiplier, firstCoefficient, desiredCoMPositionToPack);
      desiredCoMPositionToPack.scaleAdd(secondCoefficientPositionMultiplier, secondCoefficient, desiredCoMPositionToPack);
      desiredCoMPositionToPack.addZ(gravityPositionEffect);
   }

   static void constructDesiredCoMVelocity(FixedFrameVector3DBasics desiredCoMVelocityToPack, FramePoint3DReadOnly firstCoefficient,
                                           FramePoint3DReadOnly secondCoefficient, ContactState contactState, double timeInPhase, double omega, double gravityZ)
   {
      double firstCoefficientVelocityMultiplier = CoMTrajectoryPlannerTools.getFirstCoefficientVelocityMultiplier(contactState, timeInPhase, omega);
      double secondCoefficientVelocityMultiplier = CoMTrajectoryPlannerTools.getSecondCoefficientVelocityMultiplier(contactState, timeInPhase, omega);
      double gravityVelocityEffect = CoMTrajectoryPlannerTools.getGravityVelocityEffect(contactState, timeInPhase, gravityZ);

      desiredCoMVelocityToPack.setToZero();
      desiredCoMVelocityToPack.scaleAdd(firstCoefficientVelocityMultiplier, firstCoefficient, desiredCoMVelocityToPack);
      desiredCoMVelocityToPack.scaleAdd(secondCoefficientVelocityMultiplier, secondCoefficient, desiredCoMVelocityToPack);
      desiredCoMVelocityToPack.addZ(gravityVelocityEffect);
   }

   static void constructDesiredCoMAcceleration(FixedFrameVector3DBasics desiredCoMAccelerationToPack, FramePoint3DReadOnly firstCoefficient,
                                               FramePoint3DReadOnly secondCoefficient, ContactState contactState, double timeInPhase, double omega,
                                               double gravityZ)
   {
      double firstCoefficientAccelerationMultiplier = CoMTrajectoryPlannerTools.getFirstCoefficientAccelerationMultiplier(contactState, timeInPhase, omega);
      double secondCoefficientAccelerationMultiplier = CoMTrajectoryPlannerTools.getSecondCoefficientAccelerationMultiplier(contactState, timeInPhase, omega);
      double gravityAccelerationEffect = CoMTrajectoryPlannerTools.getGravityAccelerationEffect(contactState, gravityZ);

      desiredCoMAccelerationToPack.setToZero();
      desiredCoMAccelerationToPack.scaleAdd(firstCoefficientAccelerationMultiplier, firstCoefficient, desiredCoMAccelerationToPack);
      desiredCoMAccelerationToPack.scaleAdd(secondCoefficientAccelerationMultiplier, secondCoefficient, desiredCoMAccelerationToPack);
      desiredCoMAccelerationToPack.addZ(gravityAccelerationEffect);
   }

   static double getFirstCoefficientPositionMultiplier(ContactState contactState, double timeInPhase, double omega)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return Math.min(Double.MAX_VALUE, Math.exp(omega * timeInPhase));
      }
      else
      {
         return timeInPhase;
      }
   }

   static double getSecondCoefficientPositionMultiplier(ContactState contactState, double timeInPhase, double omega)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return Math.exp(-omega * timeInPhase);
      }
      else
      {
         return 1.0;
      }
   }

   static double getFirstCoefficientVelocityMultiplier(ContactState contactState, double timeInPhase, double omega)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return Math.min(Double.MAX_VALUE, omega * Math.exp(omega * timeInPhase));
      }
      else
      {
         return 1.0;
      }
   }

   static double getSecondCoefficientVelocityMultiplier(ContactState contactState, double timeInPhase, double omega)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return -omega * getSecondCoefficientPositionMultiplier(contactState, timeInPhase, omega);
      }
      else
      {
         return 0.0;
      }
   }

   static double getFirstCoefficientAccelerationMultiplier(ContactState contactState, double timeInPhase, double omega)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return Math.min(Double.MAX_VALUE, MathTools.square(omega) * Math.exp(omega * timeInPhase));
      }
      else
      {
         return 1.0;
      }
   }

   static double getSecondCoefficientAccelerationMultiplier(ContactState contactState, double timeInPhase, double omega)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return MathTools.square(omega) * getSecondCoefficientPositionMultiplier(contactState, timeInPhase, omega);
      }
      else
      {
         return 0.0;
      }
   }

   static double getGravityPositionEffect(ContactState contactState, double timeInPhase, double gravityZ)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return 0.0;
      }
      else
      {
         return -0.5 * gravityZ * MathTools.square(timeInPhase);
      }
   }

   static double getGravityVelocityEffect(ContactState contactState, double timeInPhase, double gravityZ)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return 0.0;
      }
      else
      {
         return -gravityZ * timeInPhase;
      }
   }

   static double getGravityAccelerationEffect(ContactState contactState, double gravityZ)
   {
      if (contactState == ContactState.IN_CONTACT)
      {
         return 0.0;
      }
      else
      {
         return -gravityZ;
      }
   }

   static int getFirstCoefficientIndex(int sequenceId)
   {
      return 2 * sequenceId;
   }

   static int getSecondCoefficientIndex(int sequenceId)
   {
      return 2 * sequenceId + 1;
   }
}
