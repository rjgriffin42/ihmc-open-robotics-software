package us.ihmc.quadrupedPlanning.networkProcessing.heightTeleop;

import controller_msgs.msg.dds.HighLevelStateChangeStatusMessage;
import us.ihmc.quadrupedPlanning.networkProcessing.OutputManager;
import us.ihmc.quadrupedPlanning.networkProcessing.QuadrupedRobotModelProviderNode;
import us.ihmc.quadrupedPlanning.networkProcessing.QuadrupedToolboxController;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.concurrent.atomic.AtomicReference;

public class QuadrupedBodyHeightTeleopController extends QuadrupedToolboxController
{
   private final QuadrupedBodyHeightTeleopManager teleopManager;

   private final AtomicReference<HighLevelStateChangeStatusMessage> controllerStateChangeMessage = new AtomicReference<>();

   public QuadrupedBodyHeightTeleopController(double initialBodyHeight, OutputManager statusOutputManager,
                                              QuadrupedRobotModelProviderNode robotModelProvider, YoVariableRegistry parentRegistry)
   {
      super(statusOutputManager, parentRegistry);

      teleopManager = new QuadrupedBodyHeightTeleopManager(initialBodyHeight, robotModelProvider.getReferenceFrames());
   }

   public void setPaused(boolean pause)
   {
      teleopManager.setPaused(pause);
   }

   public void processHighLevelStateChangeMessage(HighLevelStateChangeStatusMessage message)
   {
      controllerStateChangeMessage.set(message);
   }

   public void setDesiredBodyHeight(double desiredBodyHeight)
   {
      teleopManager.setDesiredBodyHeight(desiredBodyHeight);
   }

   @Override
   public boolean initialize()
   {
      teleopManager.initialize();

      return true;
   }

   @Override
   public void updateInternal()
   {
      teleopManager.update();
      reportMessage(teleopManager.getBodyHeightMessage());
   }

   @Override
   public boolean isDone()
   {
      return controllerStateChangeMessage.get().getEndHighLevelControllerName() != HighLevelStateChangeStatusMessage.WALKING;
   }
}
