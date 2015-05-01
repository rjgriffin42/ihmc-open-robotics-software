package us.ihmc.robotiq.communication.registers;

public class GripperStatusRegister implements RobotiqRegister
{
   private gACT gact;
   private gMOD gmod;
   private gGTO ggto;
   private gIMC gimc;
   private gSTA gsta;
   
   public GripperStatusRegister(gACT gact, gMOD gmod, gGTO ggto, gIMC gimc, gSTA gsta)
   {
      this.gact = gact;
      this.gmod = gmod;
      this.ggto = ggto;
      this.gimc = gimc;
      this.gsta = gsta;
   }

   public gACT getGact()
   {
      return gact;
   }

   public void setGact(gACT gact)
   {
      this.gact = gact;
   }

   public gMOD getGmod()
   {
      return gmod;
   }

   public void setGmod(gMOD gmod)
   {
      this.gmod = gmod;
   }

   public gGTO getGgto()
   {
      return ggto;
   }

   public void setGgto(gGTO ggto)
   {
      this.ggto = ggto;
   }

   public gIMC getGimc()
   {
      return gimc;
   }

   public void setGimc(gIMC gimc)
   {
      this.gimc = gimc;
   }

   public gSTA getGsta()
   {
      return gsta;
   }

   public void setGsta(gSTA gsta)
   {
      this.gsta = gsta;
   }

   @Override
   public byte getRegisterValue()
   {
      byte ret = (byte)0x0;
      
      ret |= gsta.getValue() << 7;
      ret |= gimc.getValue() << 5;
      ret |= ggto.getValue() << 3;
      ret |= gmod.getValue() << 2;
      ret |= gact.getValue();
      
      return ret;
   }

   @Override
   public int getRegisterIndex()
   {
      return 0;
   }
   
   public enum gACT implements RobotiqRegisterComponent
   {
      GRIPPER_RESET((byte)0x0), GRIPPER_ACTIVATION((byte)0x1);

      private byte value;
      
      private gACT(byte value)
      {
         this.value = value;
      }
      
      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum gMOD implements RobotiqRegisterComponent
   {
      BASIC_MODE((byte)0x0), PINCH_MODE((byte)0x1), WIDE_MODE((byte)0x2), SCISSOR_MODE((byte)0x3);
      
      private byte value;
      
      private gMOD(byte value)
      {
         this.value = value;
      }

      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum gGTO implements RobotiqRegisterComponent
   {
      STOPPED((byte)0x0), GO_TO((byte)0x1);
      
      private byte value;
      
      private gGTO(byte value)
      {
         this.value = value;
      }

      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum gIMC implements RobotiqRegisterComponent
   {
      GRIPPER_IN_RESET((byte)0x0), ACTIVATION_IN_PROGRESS((byte)0x1), MODE_CHANGE_IN_PROGRESS((byte)0x2), ACTIVATION_AND_MODE_CHANGE_COMPLETE((byte)0x3);
      
      private byte value;
      
      private gIMC(byte value)
      {
         this.value = value;
      }

      @Override
      public byte getValue()
      {
         return value;
      }
   }
   
   public enum gSTA implements RobotiqRegisterComponent
   {
      GRIPPER_IN_MOTION((byte)0x0), ONE_OR_TWO_FINGERS_STOPPED((byte)0x1), ALL_FINGERS_STOPPED((byte)0x2), FINGERS_REACHED_REQUESTED_POSITION((byte)0x3);
      
      private byte value;
      
      private gSTA(byte value)
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
