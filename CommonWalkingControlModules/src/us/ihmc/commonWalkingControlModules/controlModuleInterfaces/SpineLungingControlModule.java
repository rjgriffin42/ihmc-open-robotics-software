package us.ihmc.commonWalkingControlModules.controlModuleInterfaces;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.SpineTorques;
import us.ihmc.utilities.screwTheory.Wrench;

public interface SpineLungingControlModule extends SpineControlModule
{
   public abstract void doMaintainDesiredChestOrientation();
   
   public abstract void getSpineTorques(SpineTorques spineTorquesToPack);
   
   public abstract void setWrenchBetweenChestAndPelvis(Wrench wrench, SpineTorques spineTorquesToPack);

}

