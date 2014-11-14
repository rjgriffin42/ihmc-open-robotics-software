package us.ihmc.simulationconstructionset.util.gui;

import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;

import us.ihmc.simulationconstructionset.NewDataListener;

public interface YoVariableToggleContainer
{
   public abstract void processingStateChange(boolean endStateValue);

   public abstract void handleStateChange();

   public abstract void registerWithVariableChangedListener(VariableChangedListener variableChangedListener);

   public abstract NewDataListener getDataListener();
}
