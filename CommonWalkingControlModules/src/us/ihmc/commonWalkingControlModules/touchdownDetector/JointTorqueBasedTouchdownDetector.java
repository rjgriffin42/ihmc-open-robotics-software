package us.ihmc.commonWalkingControlModules.touchdownDetector;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.math.filters.GlitchFilteredBooleanYoVariable;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class JointTorqueBasedTouchdownDetector implements TouchdownDetector
{
   private final OneDoFJoint joint;
   private final DoubleYoVariable jointTorque;
   private final DoubleYoVariable torqueThreshold;
   private final BooleanYoVariable touchdownDetected;
   private final GlitchFilteredBooleanYoVariable touchdownDetectedFiltered;

   private double signum;

   public JointTorqueBasedTouchdownDetector(OneDoFJoint joint, YoVariableRegistry registry)
   {
      this.joint = joint;
      jointTorque = new DoubleYoVariable(joint.getName() + "_torqueUsedForTouchdownDetection", registry);
      torqueThreshold = new DoubleYoVariable(joint.getName() + "_touchdownTorqueThreshold", registry);
      touchdownDetected = new BooleanYoVariable(joint.getName() + "_torqueBasedTouchdownDetected", registry);
      touchdownDetectedFiltered = new GlitchFilteredBooleanYoVariable(joint.getName() + "_torqueBasedTouchdownDetectedFiltered", touchdownDetected, 20);
   }

   /**
    *
    * @param torqueThreshold
    *
    * If torqueThreshold < 0, hasTouchedDown will be true when jointTorque > torqueThreshold. If toqueThreshold > 0,
    * hasTouchedDown will be true when jointTorque < torqueThreshold.
    *
    */
   public void setTorqueThreshold(double torqueThreshold)
   {
      this.torqueThreshold.set(torqueThreshold);
      signum = Math.signum(torqueThreshold);
   }

   @Override
   public boolean hasTouchedDown()
   {
      return touchdownDetectedFiltered.getBooleanValue();
   }

   @Override
   public void update()
   {
      double threshold = torqueThreshold.getDoubleValue() * signum;
      double torque = joint.getTauMeasured() * signum;

      jointTorque.set(joint.getTauMeasured());

      touchdownDetected.set(torque > threshold);
      touchdownDetectedFiltered.update();
   }
}
