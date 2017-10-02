package us.ihmc.commonWalkingControlModules.controllerAPI.input.userDesired;

import us.ihmc.communication.controllerAPI.CommandInputManager;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.ChestTrajectoryCommand;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.math.frames.YoFrameOrientation;

public class UserDesiredChestOrientationControllerCommandGenerator
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final YoBoolean userDesiredChestGoToHomeOrientation = new YoBoolean("userDesiredChestGoToHomeOrientation", registry);
   private final YoDouble userDesiredChestTrajectoryTime = new YoDouble("userDesiredChestTrajectoryTime", registry);
   private final YoBoolean userDoChestOrientation = new YoBoolean("userDoChestOrientation", registry);
   private final YoFrameOrientation userDesiredChestOrientation;

   private final FrameOrientation frameOrientation = new FrameOrientation();

   public UserDesiredChestOrientationControllerCommandGenerator(final CommandInputManager controllerCommandInputManager, double defaultTrajectoryTime, YoVariableRegistry parentRegistry)
   {
      userDesiredChestOrientation = new YoFrameOrientation("userDesiredChest", ReferenceFrame.getWorldFrame(), registry);

      userDoChestOrientation.addVariableChangedListener(new VariableChangedListener()
      {
         public void notifyOfVariableChanged(YoVariable<?> v)
         {
            if (userDoChestOrientation.getBooleanValue())
            {
               userDesiredChestOrientation.getFrameOrientationIncludingFrame(frameOrientation);

               ChestTrajectoryCommand chestTrajectoryControllerCommand = new ChestTrajectoryCommand();
               chestTrajectoryControllerCommand.addTrajectoryPoint(userDesiredChestTrajectoryTime.getDoubleValue(), frameOrientation.getQuaternionCopy(), new Vector3D());
               controllerCommandInputManager.submitCommand(chestTrajectoryControllerCommand);

               userDoChestOrientation.set(false);
            }
         }
      });

      userDesiredChestGoToHomeOrientation.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void notifyOfVariableChanged(YoVariable<?> v)
         {
            if (userDesiredChestGoToHomeOrientation.getBooleanValue())
            {
               userDesiredChestOrientation.setYawPitchRoll(0.0, 0.0, 0.0);
               userDoChestOrientation.set(true, true);
               userDesiredChestGoToHomeOrientation.set(false);
            }
         }
      });

      userDesiredChestTrajectoryTime.set(defaultTrajectoryTime);

      parentRegistry.addChild(registry);
   }
}
