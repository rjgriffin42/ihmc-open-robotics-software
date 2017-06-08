package us.ihmc.humanoidRobotics.communication.packets;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.communication.packets.TrackablePacket;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.tools.ArrayTools;

public abstract class AbstractDesiredAccelerationsMessage<T extends AbstractDesiredAccelerationsMessage<T>> extends TrackablePacket<T>
{
   @RosExportedField(documentation = "Specifies the desired joint accelerations.")
   public double[] desiredJointAccelerations;
   
   /** the time to delay this command on the controller side before being executed **/
   public double executionDelayTime;

   public AbstractDesiredAccelerationsMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public AbstractDesiredAccelerationsMessage(Random random)
   {
      int randomNumberOfAccels = random.nextInt(16) + 1;
      desiredJointAccelerations = new double[randomNumberOfAccels];

      for(int i = 0; i < randomNumberOfAccels; i++)
      {
         desiredJointAccelerations[i] = RandomNumbers.nextDoubleWithEdgeCases(random, 0.01);
      }
   }

   public AbstractDesiredAccelerationsMessage(double[] desiredJointAccelerations)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      this.desiredJointAccelerations = desiredJointAccelerations;
   }

   public int getNumberOfJoints()
   {
      if (desiredJointAccelerations == null)
         return 0;
      else
         return desiredJointAccelerations.length;
   }

   public double[] getDesiredJointAccelerations()
   {
      return desiredJointAccelerations;
   }

   public double getDesiredJointAcceleration(int jointIndex)
   {
      return desiredJointAccelerations[jointIndex];
   }
   
   /**
    * returns the amount of time this command is delayed on the controller side before executing
    * @return the time to delay this command in seconds
    */
   public double getExecutionDelayTime()
   {
      return executionDelayTime;
   }
   
   /**
    * sets the amount of time this command is delayed on the controller side before executing
    * @param delayTime the time in seconds to delay after receiving the command before executing
    */
   public void setExecutionDelayTime(double delayTime)
   {
      this.executionDelayTime = delayTime;
   }

   @Override
   public boolean epsilonEquals(T other, double epsilon)
   {
      if (!ArrayTools.deltaEquals(getDesiredJointAccelerations(), other.getDesiredJointAccelerations(), epsilon))
         return false;
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validateDesiredAccelerationsMessage(this, true);
   }

   @Override
   public String toString()
   {
         String ret = "desired accelerations = [";
         NumberFormat doubleFormat = new DecimalFormat(" 0.00;-0.00");
         for (int i = 0; i < getNumberOfJoints(); i++)
         {
            double jointDesiredAcceleration = desiredJointAccelerations[i];
            ret += doubleFormat.format(jointDesiredAcceleration);
            if (i < getNumberOfJoints() - 1)
               ret += ", ";
         }
         return ret + "].";
   }
}
