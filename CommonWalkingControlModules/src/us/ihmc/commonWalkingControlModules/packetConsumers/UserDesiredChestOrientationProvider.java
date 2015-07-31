package us.ihmc.commonWalkingControlModules.packetConsumers;

import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameOrientation;
import us.ihmc.yoUtilities.math.trajectories.WaypointOrientationTrajectoryData;

public class UserDesiredChestOrientationProvider implements ChestOrientationProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final BooleanYoVariable goToHomeOrientation = new BooleanYoVariable("userDesiredChestGoToHomeOrientation", registry);
   private final DoubleYoVariable chestTrajectoryTime = new DoubleYoVariable("userDesiredChestTrajectoryTime", registry);
   private final BooleanYoVariable isNewChestOrientationInformationAvailable = new BooleanYoVariable("isNewChestOrientationInformationAvailable", registry);
   private final YoFrameOrientation userChest;

   private final FrameOrientation frameOrientation = new FrameOrientation();

   private final ReferenceFrame chestOrientationFrame;

   public UserDesiredChestOrientationProvider(ReferenceFrame chestOrientationFrame, YoVariableRegistry parentRegistry)
   {
      this.chestOrientationFrame = chestOrientationFrame;
      userChest = new YoFrameOrientation("userDesiredChest", chestOrientationFrame, registry);

      VariableChangedListener variableChangedListener = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            isNewChestOrientationInformationAvailable.set(true);
            userChest.getFrameOrientationIncludingFrame(frameOrientation);
         }
      };

      userChest.attachVariableChangedListener(variableChangedListener);

      goToHomeOrientation.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            if (goToHomeOrientation.getBooleanValue())
               userChest.setYawPitchRoll(0.0, 0.0, 0.0, false);
         }
      });

      parentRegistry.addChild(registry);
   }

   @Override
   public boolean checkForNewChestOrientation()
   {
      return isNewChestOrientationInformationAvailable.getBooleanValue();
   }

   @Override
   public boolean checkForHomeOrientation()
   {
      if (goToHomeOrientation.getBooleanValue())
      {
         goToHomeOrientation.set(false);
         return true;
      }
      return false;
   }

   @Override
   public FrameOrientation getDesiredChestOrientation()
   {
      if (!isNewChestOrientationInformationAvailable.getBooleanValue())
         return null;

      isNewChestOrientationInformationAvailable.set(false);

      return frameOrientation;
   }

   @Override
   public ReferenceFrame getChestOrientationExpressedInFrame()
   {
      return chestOrientationFrame;
   }

   @Override
   public double getTrajectoryTime()
   {
      return chestTrajectoryTime.getDoubleValue();
   }

   @Override
   public boolean checkForNewChestOrientationWithWaypoints()
   {
      return false;
   }

   @Override
   public WaypointOrientationTrajectoryData getDesiredChestOrientationWithWaypoints()
   {
      return null;
   }
}
