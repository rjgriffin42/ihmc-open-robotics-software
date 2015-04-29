package us.ihmc.robotiq.communication.registers;

public class ActionRequestRegister implements RobotiqRegister
{
   private rACT ract;
   private rMOD rmod;
   private rGTO rgto;
   private rATR ratr;
   
   public ActionRequestRegister(rACT ract, rMOD rmod, rGTO rgto, rATR ratr)
   {
      this.ract = ract;
      this.rmod = rmod;
      this.rgto = rgto;
      this.ratr = ratr;
   }
   
   public rACT getRact()
   {
      return ract;
   }

   public void setRact(rACT ract)
   {
      this.ract = ract;
   }

   public rMOD getRmod()
   {
      return rmod;
   }

   public void setRmod(rMOD rmod)
   {
      this.rmod = rmod;
   }

   public rGTO getRgto()
   {
      return rgto;
   }

   public void setRgto(rGTO rgto)
   {
      this.rgto = rgto;
   }

   public rATR getRatr()
   {
      return ratr;
   }

   public void setRatr(rATR ratr)
   {
      this.ratr = ratr;
   }

   @Override
   public byte getRegisterValue()
   {
      byte ret = (byte)0x00;
      
      ret |= ratr.getValue() << 4;
      ret |= rgto.getValue() << 3;
      ret |= rmod.getValue() << 2;
      ret |= ract.getValue();
      
      return ret;
   }
   
   @Override
   public int getRegisterIndex()
   {
      return 0;
   }
   
   public enum rACT implements RobotiqRegisterComponent
   {
      DEACTIVATE_GRIPPER((byte)0x0), ACTIVATE_GRIPPER((byte)0x1);
      
      private byte value;
      
      private rACT(byte value)
      {
         this.value = value;
      }
      
      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum rMOD implements RobotiqRegisterComponent
   {
      BASIC_MODE((byte)0x0), PINCH_MODE((byte)0x1), WIDE_MODE((byte)0x2), SCISSOR_MODE((byte)0x3);
      
      private byte value;
      
      private rMOD(byte value)
      {
         this.value = value;
      }

      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum rGTO implements RobotiqRegisterComponent
   {
      STOP((byte)0x0), GO_TO((byte)0x1);
      
      private byte value;
      
      private rGTO(byte value)
      {
         this.value = value;
      }
      
      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum rATR implements RobotiqRegisterComponent
   {
      NORMAL((byte)0x0), EMERGENCY_ATORELEASE((byte)0x1);
      
      private byte value;
      
      private rATR(byte value)
      {
         this.value = value;
      }
      
      @Override
      public byte getValue()
      {
         return value;
      }
   }
}
