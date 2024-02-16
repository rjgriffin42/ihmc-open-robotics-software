package us.ihmc.avatar.sakeGripper;

import controller_msgs.msg.dds.SakeHandDesiredCommandMessage;

public class SakeHandParameters
{
   /**
    * When hand is fully open, fingertips form 210 degrees angle.
    * This corresponds to the normalized value of 1.0.
    */
   public static final double MAX_DESIRED_HAND_OPEN_ANGLE_DEGREES = 210.0;
   /** Joint angle of a finger is 102 degrees when fully open */
   public static final double OPEN_KNUCKLE_JOINT_ANGLE_DEGREES = 105.0;
   /** Sake hand can produce 29 N of grip force between the fingertips */
   public static final double FINGERTIP_GRIP_FORCE_HARDWARE_LIMIT = 29.0;
   /** This is a safe amount of force. */
   public static final double FINGERTIP_GRIP_FORCE_SAFE = 8.7;
   /** This is a moderate amount of force. */
   public static final double FINGERTIP_GRIP_FORCE_MODERATE_THRESHOLD = 14.5;
   /** This is a high amount of force. */
   public static final double FINGERTIP_GRIP_FORCE_HIGH_THRESHOLD = 20.3;
   /** The stall torque of the dynamixel that drives the gripper. */
   public static final double DYNAMIXEL_MX_64AR_STALL_TORQUE = 6.0;
   /** Rough guess how much knuckle torque corresponding to our limit above. */
   public static final double KNUCKLE_TORQUE_AT_LIMIT = 2.0;
   /** Normal hand temperature. */
   public static final double NORMAL_TEMPERATURE_CELCIUS = 30.0;
   /** Warning hand temperature. */
   public static final double WARNING_TEMPERATURE_CELCIUS = 40.0;
   /** Red colored hand temperature. */
   public static final double ERROR_TEMPERATURE_CELCIUS = 50.0;
   /** The max temperature before the hand resets. */
   public static final double TEMPERATURE_LIMIT_CELCIUS = 60.0;
   /** The temperature at which the Dynamixel will break. */
   public static final double DYNAMIXEL_FAILURE_TEMPERATURE_CELCIUS = 80.0;

   /**
    * @param normalizedHandOpenAngle 0.0 (closed) to 1.0 (open)
    * @return actual angle between fingers in radians
    */
   public static double denormalizeHandOpenAngle(double normalizedHandOpenAngle)
   {
      return normalizedHandOpenAngle * Math.toRadians(MAX_DESIRED_HAND_OPEN_ANGLE_DEGREES);
   }

   /**
    * @param handOpenAngle actual angle between fingers in radians
    * @return 0.0 (closed) to 1.0 (open)
    */
   public static double normalizeHandOpenAngle(double handOpenAngle)
   {
      return handOpenAngle / Math.toRadians(MAX_DESIRED_HAND_OPEN_ANGLE_DEGREES);
   }

   public static double denormalizeFingertipGripForceLimit(double normalizedFingertipGripForceLimit)
   {
      return normalizedFingertipGripForceLimit * FINGERTIP_GRIP_FORCE_HARDWARE_LIMIT;
   }

   public static double normalizeFingertipGripForceLimit(double fingertipGripForceLimit)
   {
      return fingertipGripForceLimit / FINGERTIP_GRIP_FORCE_HARDWARE_LIMIT;
   }

   public static double denormalizeKnuckleTorque(double knuckleTorque)
   {
      return knuckleTorque * KNUCKLE_TORQUE_AT_LIMIT;
   }

   public static double normalizeKnuckleTorque(double knuckleTorque)
   {
      return knuckleTorque / KNUCKLE_TORQUE_AT_LIMIT;
   }

   public static double knuckleJointAngleToHandOpenAngle(double knuckleJointAngle)
   {
      return denormalizeHandOpenAngle(knuckleJointAngle / Math.toRadians(OPEN_KNUCKLE_JOINT_ANGLE_DEGREES));
   }

   public static double handOpenAngleToKnuckleJointAngle(double handOpenAngle)
   {
      return normalizeHandOpenAngle(handOpenAngle) * Math.toRadians(OPEN_KNUCKLE_JOINT_ANGLE_DEGREES);
   }

   public static void resetDesiredCommandMessage(SakeHandDesiredCommandMessage sakeHandDesiredCommandMessage)
   {
      sakeHandDesiredCommandMessage.setNormalizedGripperDesiredPosition(-1.0);
      sakeHandDesiredCommandMessage.setNormalizedGripperTorqueLimit(-1.0);
      sakeHandDesiredCommandMessage.setRequestCalibration(false);
      sakeHandDesiredCommandMessage.setRequestCalibration(false);
   }
}
