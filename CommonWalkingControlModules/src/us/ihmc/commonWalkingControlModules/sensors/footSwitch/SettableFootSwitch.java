package us.ihmc.commonWalkingControlModules.sensors.footSwitch;

import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.Wrench;

public class SettableFootSwitch implements FootSwitchInterface
{
   BooleanYoVariable hasFootHitGround;
   private final ContactablePlaneBody foot;
   private final double totalRobotWeight; 
   
   public SettableFootSwitch(ContactablePlaneBody foot, RobotQuadrant quadrant, double totalRobotWeight, YoVariableRegistry registry)
   {
      this.hasFootHitGround = new BooleanYoVariable(quadrant.getCamelCaseName() + "_SettableFootSwitch", registry);
      this.totalRobotWeight = totalRobotWeight;
      this.foot = foot;
      hasFootHitGround.set(true);
   }
   
   @Override
   public boolean hasFootHitGround()
   {
      return hasFootHitGround.getBooleanValue();
   }

   @Override
   public double computeFootLoadPercentage()
   {
      return Double.NaN;
   }

   @Override
   public void computeAndPackCoP(FramePoint2d copToPack)
   {
      copToPack.setToNaN(getMeasurementFrame());
   }

   @Override
   public void computeAndPackFootWrench(Wrench footWrenchToPack)
   {
      footWrenchToPack.setToZero();
      if (hasFootHitGround())
         footWrenchToPack.setLinearPartZ(totalRobotWeight / 4.0);
   }

   @Override
   public ReferenceFrame getMeasurementFrame()
   {
      return foot.getSoleFrame();
   }

   @Override
   public void reset()
   {
      hasFootHitGround.set(false);
   }

   @Override
   public boolean getForceMagnitudePastThreshhold()
   {
      return false;
   }
   
   @Override
   public void setFootContactState(boolean hasFootHitGround)
   {
      this.hasFootHitGround.set(hasFootHitGround);
   }

}
