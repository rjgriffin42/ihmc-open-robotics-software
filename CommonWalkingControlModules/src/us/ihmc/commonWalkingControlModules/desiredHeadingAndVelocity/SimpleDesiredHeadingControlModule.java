package us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;

import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;


public class SimpleDesiredHeadingControlModule implements DesiredHeadingControlModule
{
   private SimpleDesiredHeadingControlModuleVisualizer simpleDesiredHeadingControlModuleVisualizer;
   
   private final YoVariableRegistry registry = new YoVariableRegistry("DesiredHeadingControlModule");
   private final DoubleYoVariable desiredHeadingFinal = new DoubleYoVariable("desiredHeadingFinal",
                                                           "Yaw of the desired heading frame with respect to the world.", registry);
   private final DoubleYoVariable desiredHeading = new DoubleYoVariable("desiredHeading", registry);
   private final DoubleYoVariable maxHeadingDot = new DoubleYoVariable("maxHeadingDot", "In units of rad/sec", registry);

   private final DesiredHeadingFrame desiredHeadingFrame = new DesiredHeadingFrame();

   private final double controlDT;
   
   public SimpleDesiredHeadingControlModule(double desiredHeadingfinal, double controlDT,
           YoVariableRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
      this.controlDT = controlDT;

      maxHeadingDot.set(0.1);

      this.desiredHeadingFinal.set(desiredHeadingfinal);
      this.desiredHeading.set(this.desiredHeadingFinal.getDoubleValue());    // The final is the first one according to the initial setup of the robot
      
      updateDesiredHeadingFrame();
   }
   
   public void setMaxHeadingDot(double maxHeadingDot)
   {
      this.maxHeadingDot.set(maxHeadingDot);
   }
   
   public double getMaxHeadingDot()
   {
      return maxHeadingDot.getDoubleValue();
   }
   
   public void initializeVisualizer(ProcessedSensorsInterface processedSensors, YoGraphicsListRegistry dynamicGraphicObjectsListRegistry)
   {
      if (simpleDesiredHeadingControlModuleVisualizer != null)
      {
         throw new RuntimeException("Already setupVisualizer");
      }
      
      simpleDesiredHeadingControlModuleVisualizer = new SimpleDesiredHeadingControlModuleVisualizer(processedSensors, registry, dynamicGraphicObjectsListRegistry);
   }

   public void updateDesiredHeadingFrame()
   {
      updateDesiredHeading();
      desiredHeadingFrame.update();

      if (simpleDesiredHeadingControlModuleVisualizer != null)
      {
         simpleDesiredHeadingControlModuleVisualizer.updateDesiredHeading(desiredHeading.getDoubleValue(), desiredHeadingFinal.getDoubleValue());
      }
   }

   public double getFinalHeadingTargetAngle()
   {
      return desiredHeadingFinal.getDoubleValue();
   }
   
   public FrameVector2d getFinalHeadingTarget()
   {
      FrameVector2d finalHeading = new FrameVector2d(ReferenceFrame.getWorldFrame(), Math.cos(desiredHeadingFinal.getDoubleValue()),
                                    Math.sin(desiredHeadingFinal.getDoubleValue()));

      return finalHeading;
   }

   public ReferenceFrame getDesiredHeadingFrame()
   {
      return desiredHeadingFrame;
   }

   public void setFinalHeadingTargetAngle(double finalHeadingTargetAngle)
   {
      this.desiredHeadingFinal.set(finalHeadingTargetAngle);
   }
   
   public void setFinalHeadingTarget(FrameVector2d finalHeadingTarget)
   {
      finalHeadingTarget.checkReferenceFrameMatch(ReferenceFrame.getWorldFrame());
      setFinalHeadingTargetAngle(Math.atan2(finalHeadingTarget.getY(), finalHeadingTarget.getX()));
   }

   public double getDesiredHeadingAngle()
   {
      return desiredHeading.getDoubleValue();
   }
   
   public FrameVector2d getDesiredHeading()
   {
      FrameVector2d desiredHeadingVector = new FrameVector2d(ReferenceFrame.getWorldFrame(), Math.cos(desiredHeading.getDoubleValue()),
                                    Math.sin(desiredHeading.getDoubleValue()));

      return desiredHeadingVector;
   }

   public void resetHeadingAngle(double newHeading)
   {
      this.desiredHeading.set(newHeading);
      this.desiredHeadingFinal.set(newHeading);
   }

   private void updateDesiredHeading()
   {
      double error = desiredHeadingFinal.getDoubleValue() - desiredHeading.getDoubleValue();
      double maximumChangePerTick = maxHeadingDot.getDoubleValue() * controlDT;

      double deltaHeading = MathTools.clipToMinMax(error, -maximumChangePerTick, maximumChangePerTick);

      desiredHeading.set(desiredHeading.getDoubleValue() + deltaHeading);
   }

   private class DesiredHeadingFrame extends ReferenceFrame
   {
      private static final long serialVersionUID = 4657294310129415811L;

      public DesiredHeadingFrame()
      {
         super("DesiredHeadingFrame", ReferenceFrame.getWorldFrame(), false, false, true);
      }

      protected void updateTransformToParent(Transform3D transformToParent)
      {
         Matrix3d rotation = new Matrix3d();
         rotation.rotZ(desiredHeading.getDoubleValue());

         transformToParent.set(rotation);
      }
   }
}
