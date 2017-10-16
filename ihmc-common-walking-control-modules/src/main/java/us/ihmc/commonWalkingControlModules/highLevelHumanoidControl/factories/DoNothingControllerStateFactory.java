package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.HighLevelControllerFactoryHelper;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.HighLevelControllerState;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.DoNothingControllerState;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;

public class DoNothingControllerStateFactory implements HighLevelControllerStateFactory
{
   private DoNothingControllerState doNothingControllerState;

   @Override
   public HighLevelControllerState getOrCreateControllerState(HighLevelControllerFactoryHelper controllerFactoryHelper)
   {
      if (doNothingControllerState == null)
         doNothingControllerState = new DoNothingControllerState(controllerFactoryHelper.getHighLevelHumanoidControllerToolbox(),
                                                                 controllerFactoryHelper.getHighLevelControllerParameters());

      return doNothingControllerState;
   }

   @Override
   public HighLevelController getStateEnum()
   {
      return HighLevelController.DO_NOTHING_BEHAVIOR;
   }
}
