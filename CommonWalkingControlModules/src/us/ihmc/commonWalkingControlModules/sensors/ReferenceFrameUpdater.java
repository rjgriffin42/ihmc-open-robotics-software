package us.ihmc.commonWalkingControlModules.sensors;

import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.robotController.SensorProcessor;

public class ReferenceFrameUpdater implements SensorProcessor
{

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final ReferenceFrames referenceFrames;

   public ReferenceFrameUpdater(ReferenceFrames referenceFrames)
   {
      this.referenceFrames = referenceFrames;
   }

   public void initialize()
   {
      update();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   public void update()
   {
      referenceFrames.updateFrames();
   }
}
