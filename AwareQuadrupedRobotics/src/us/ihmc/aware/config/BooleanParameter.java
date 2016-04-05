package us.ihmc.aware.config;

public class BooleanParameter extends Parameter
{
   private boolean value;

   BooleanParameter(String path, boolean defaultValue)
   {
      super(path);
      this.value = defaultValue;
   }

   public boolean get()
   {
      return value;
   }

   public void set(boolean value)
   {
      this.value = value;
      notifyChangedListeners();
   }

   @Override
   public boolean tryLoadValue(String value)
   {
      this.value = Boolean.parseBoolean(value);
      return true;
   }

   @Override
   protected String dumpValue()
   {
      return Boolean.toString(value);
   }
}

