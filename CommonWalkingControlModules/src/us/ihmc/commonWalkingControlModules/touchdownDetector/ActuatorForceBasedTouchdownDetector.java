package us.ihmc.commonWalkingControlModules.touchdownDetector;

import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.robotics.sensors.ForceSensorDataReadOnly;

public class ActuatorForceBasedTouchdownDetector implements TouchdownDetector
{
   private final int GLITCH_FLITER_WINDOW_SIZE = 10;

   private final YoBoolean touchdownDetected;
   private final GlitchFilteredYoBoolean touchdownDetectedFiltered;

   private final ForceSensorDataReadOnly foreSensorData;
   private final DoubleYoVariable touchdownForceThreshold;

   private final Wrench wrenchToPack = new Wrench();
   private final Vector3D vectorToPack = new Vector3D();

   public ActuatorForceBasedTouchdownDetector(String name, ForceSensorDataReadOnly forceSensorData, double touchdownForceThreshold, YoVariableRegistry registry)
   {
      this.foreSensorData = forceSensorData;
      this.touchdownForceThreshold = new DoubleYoVariable(name + "_touchdownForceThreshold", registry);
      this.touchdownForceThreshold.set(touchdownForceThreshold);

      touchdownDetected = new YoBoolean(name + "_touchdownDetected", registry);
      touchdownDetectedFiltered = new GlitchFilteredYoBoolean(touchdownDetected.getName() + "Filtered", registry, touchdownDetected, GLITCH_FLITER_WINDOW_SIZE);
   }

   @Override
   public boolean hasTouchedDown()
   {
      return touchdownDetectedFiltered.getBooleanValue();
   }

   @Override
   public void update()
   {
      foreSensorData.getWrench(wrenchToPack);
      wrenchToPack.getLinearPart(vectorToPack);

      touchdownDetected.set(vectorToPack.length() > touchdownForceThreshold.getDoubleValue());
      touchdownDetectedFiltered.update();
   }
}
