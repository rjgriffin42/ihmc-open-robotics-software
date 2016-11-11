package us.ihmc.graphics3DDescription.instructions;

import us.ihmc.graphics3DDescription.appearance.AppearanceDefinition;
import us.ihmc.graphics3DDescription.instructions.listeners.AppearanceChangedListener;

public abstract class Graphics3DInstruction implements Graphics3DPrimitiveInstruction
{
   private AppearanceDefinition appearance = null;
   
   private AppearanceChangedListener appearanceChangedListener = null;

   public AppearanceDefinition getAppearance()
   {
      return appearance;
   }

   public void setAppearance(AppearanceDefinition appearance)
   {
      this.appearance = appearance;
      if(appearanceChangedListener != null)
      {
         appearanceChangedListener.appearanceChanged(appearance);
      }
   }
   
   public void setAppearanceChangedListener(AppearanceChangedListener appearanceChangedListener)
   {
      this.appearanceChangedListener = appearanceChangedListener;
   }

   

}
