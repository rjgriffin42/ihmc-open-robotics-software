package us.ihmc.commonWalkingControlModules.sensors;

import us.ihmc.simulationconstructionset.robotController.SensorProcessor;
import us.ihmc.robotics.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

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
