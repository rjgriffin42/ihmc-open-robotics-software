package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import java.util.List;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.GroundContactPoint;
import com.yobotics.simulationconstructionset.robotController.ModularRobotController;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import com.yobotics.simulationconstructionset.util.perturbance.GroundContactPointsSlipper;

public class SlipOnNextStepPerturber extends ModularRobotController
{
   private final GroundContactPointsSlipper groundContactPointsSlipper;
   private final List<GroundContactPoint> groundContactPoints;

   private final SDFRobot robot;

   private final EnumYoVariable<SlipState> slipState;
   private final BooleanYoVariable slipNextStep;
   private final DoubleYoVariable slipAfterTimeDelta, touchdownTimeForSlip;
   private final YoFrameVector amountToSlipNextStep;
   private final YoFrameOrientation rotationToSlipNextStep;

   public SlipOnNextStepPerturber(SDFRobot robot, RobotSide robotSide)
   {
      super(robotSide.getCamelCaseNameForStartOfExpression() + "SlipOnEachStepPerturber");

      String sideString = robotSide.getCamelCaseNameForStartOfExpression();
      this.robot = robot;
      this.touchdownTimeForSlip = new DoubleYoVariable(sideString + "TouchdownTimeForSlip", registry);
      this.slipAfterTimeDelta = new DoubleYoVariable(sideString + "SlipAfterTimeDelta", registry);
      this.slipNextStep = new BooleanYoVariable(sideString + "SlipNextStep", registry);

      amountToSlipNextStep = new YoFrameVector(sideString + "AmountToSlipNextStep", ReferenceFrame.getWorldFrame(), registry);
      rotationToSlipNextStep = new YoFrameOrientation(sideString + "RotationToSlipNextStep", ReferenceFrame.getWorldFrame(), registry);
      slipState = new EnumYoVariable<SlipState>(sideString + "SlipState", registry, SlipState.class);
      slipState.set(SlipState.NOT_SLIPPING);

      groundContactPoints = robot.getFootGroundContactPoints(robotSide);
      
      groundContactPointsSlipper = new GroundContactPointsSlipper();
      groundContactPointsSlipper.addGroundContactPoints(groundContactPoints);
      groundContactPointsSlipper.setPercentToSlipPerTick(0.05);

      this.addRobotController(groundContactPointsSlipper);
   }

   public void setPercentToSlipPerTick(double percentToSlipPerTick)
   {
      groundContactPointsSlipper.setPercentToSlipPerTick(percentToSlipPerTick);
   }
   
   public void setSlipAfterStepTimeDelta(double slipAfterTimeDelta)
   {
      this.slipAfterTimeDelta.set(slipAfterTimeDelta);
   }
   
   public void setSlipNextStep(boolean slipNextStep)
   {
      this.slipNextStep.set(slipNextStep);
   }

   public void setAmountToSlipNextStep(Vector3d amountToSlipNextStep)
   {
      this.amountToSlipNextStep.set(amountToSlipNextStep);
   }
   
   public void setRotationToSlipNextStep(double yaw, double pitch, double roll) 
   {
      rotationToSlipNextStep.setYawPitchRoll(yaw, pitch, roll);
   }

   public void doControl()
   {
      super.doControl();

      switch (slipState.getEnumValue())
      {
         case NOT_SLIPPING :
         {
            if (footTouchedDown())
            {
               if (slipNextStep.getBooleanValue())
               {
                  slipState.set(SlipState.TOUCHED_DOWN);
                  touchdownTimeForSlip.set(robot.getTime());
               }
               else // Wait till foot lift back up before allowing a slip.
               {
                  slipState.set(SlipState.DONE_SLIPPING);
               }
            }

            break;
         }

         case TOUCHED_DOWN :
         {
            if (robot.getTime() > touchdownTimeForSlip.getDoubleValue() + slipAfterTimeDelta.getDoubleValue())
            {
               slipState.set(SlipState.SLIPPING);
               startSlipping(amountToSlipNextStep.getVector3dCopy(), rotationToSlipNextStep.getYawPitchRoll());
            }

            break;
         }

         case SLIPPING :
         {
            if (groundContactPointsSlipper.isDoneSlipping())
            {
               slipState.set(SlipState.DONE_SLIPPING);
            }

            break;
         }

         case DONE_SLIPPING :
         {
            if (footLiftedUp())
            {
               slipState.set(SlipState.NOT_SLIPPING);
            }

            break;
         }
      }
   }
   
   private void startSlipping(Vector3d slipAmount, double[] yawPitchRoll)
   {
      groundContactPointsSlipper.setDoSlip(true);
      groundContactPointsSlipper.setPercentToSlipPerTick(0.01);
      groundContactPointsSlipper.setSlipTranslation(slipAmount);
      groundContactPointsSlipper.setSlipRotation(yawPitchRoll);
   }

   private boolean footTouchedDown()
   {
      for (GroundContactPoint groundContactPoint : groundContactPoints)
      {
         if (groundContactPoint.isInContact())
            return true;
      }

      return false;
   }

   private boolean footLiftedUp()
   {
      for (GroundContactPoint groundContactPoint : groundContactPoints)
      {
         if (groundContactPoint.isInContact())
            return false;
      }

      return true;
   }

   private enum SlipState {NOT_SLIPPING, TOUCHED_DOWN, SLIPPING, DONE_SLIPPING}
}
