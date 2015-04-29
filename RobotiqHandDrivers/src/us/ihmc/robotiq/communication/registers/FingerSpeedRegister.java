package us.ihmc.robotiq.communication.registers;

import us.ihmc.robotiq.communication.Finger;

public class FingerSpeedRegister implements RobotiqRegister
{
   private final byte MAX_SPEED = (byte) 0xFF;
   private final byte MIN_SPEED = (byte) 0x00;
   
   private final int index;
   private byte speed;
   
   public FingerSpeedRegister(Finger finger)
   {
      switch(finger)
      {
         case FINGER_A:
            index = 4;
            break;
         case FINGER_B:
            index = 7;
            break;
         case FINGER_C:
            index = 10;
            break;
         case SCISSOR:
            index = 13;
            break;
         default:
            throw new RuntimeException(getClass().getSimpleName() + ": " + finger.name() + " is not recognized as a Robotiq finger");
      }
      
      speed = (byte)0x00;
   }
   
   public void setSpeed(byte speed)
   {
      this.speed = speed;
   }
   
   @Override
   public byte getRegisterValue()
   {
      return speed;
   }
   @Override
   public int getRegisterIndex()
   {
      return index;
   }

}
