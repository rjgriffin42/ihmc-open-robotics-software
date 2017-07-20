package us.ihmc.commonWalkingControlModules.controlModules.leapOfFaith;

import us.ihmc.commonWalkingControlModules.configurations.LeapOfFaithParameters;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.ExternalWrenchCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.frames.YoWrench;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class FootLeapOfFaithModule
{
   private static final String yoNamePrefix = "leapOfFaith";

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());


   private final YoDouble swingDuration;

   private final YoDouble fractionOfSwing = new YoDouble(yoNamePrefix + "FractionOfSwingToScaleFootWeight", registry);
   private final YoBoolean scaleFootWeight = new YoBoolean(yoNamePrefix + "ScaleFootWeight", registry);

   private final YoDouble verticalFootWeightScaleFactor = new YoDouble(yoNamePrefix + "VerticalFootWeightScaleFactor", registry);
   private final YoDouble horizontalFootWeightScaleFactor = new YoDouble(yoNamePrefix + "HorizontalFootWeightScaleFactor", registry);

   private final YoDouble verticalFootWeightScaleFraction = new YoDouble(yoNamePrefix + "VerticalFootWeightScaleFraction", registry);
   private final YoDouble horizontalFootWeightScaleFraction = new YoDouble(yoNamePrefix + "HorizontalFootWeightScaleFraction", registry);
   private final YoDouble minimumHorizontalWeight = new YoDouble(yoNamePrefix + "MinimumHorizontalFootWeight", registry);

   public FootLeapOfFaithModule(YoDouble swingDuration, LeapOfFaithParameters parameters, YoVariableRegistry parentRegistry)
   {
      this.swingDuration = swingDuration;

      scaleFootWeight.set(true);
      fractionOfSwing.set(0.95);

      horizontalFootWeightScaleFactor.set(10.0);
      verticalFootWeightScaleFactor.set(20.0);
      minimumHorizontalWeight.set(2.5);

      parentRegistry.addChild(registry);
   }

   public void compute(double currentTime)
   {
      horizontalFootWeightScaleFraction.set(1.0);
      verticalFootWeightScaleFraction.set(1.0);

      double exceededTime = Math.max(currentTime - fractionOfSwing.getDoubleValue() * swingDuration.getDoubleValue(), 0.0);

      if (exceededTime == 0.0)
         return;

      if (scaleFootWeight.getBooleanValue())
      {
         double horizontalFootWeightScaleFraction = MathTools.clamp(1.0 - horizontalFootWeightScaleFactor.getDoubleValue() * exceededTime, 0.0, 1.0);
         double verticalFootWeightScaleFraction = Math.max(1.0, 1.0 + exceededTime * verticalFootWeightScaleFactor.getDoubleValue());

         this.horizontalFootWeightScaleFraction.set(horizontalFootWeightScaleFraction);
         this.verticalFootWeightScaleFraction.set(verticalFootWeightScaleFraction);
      }
   }

   public void scaleFootWeight(YoFrameVector unscaledLinearWeight, YoFrameVector scaledLinearWeight)
   {
      if (!scaleFootWeight.getBooleanValue())
         return;

      scaledLinearWeight.set(unscaledLinearWeight);
      scaledLinearWeight.scale(horizontalFootWeightScaleFraction.getDoubleValue());

      scaledLinearWeight.setX(Math.max(minimumHorizontalWeight.getDoubleValue(), scaledLinearWeight.getX()));
      scaledLinearWeight.setY(Math.max(minimumHorizontalWeight.getDoubleValue(), scaledLinearWeight.getY()));

      double verticalWeight = unscaledLinearWeight.getZ() * verticalFootWeightScaleFraction.getDoubleValue();
      scaledLinearWeight.setZ(verticalWeight);
   }
}
