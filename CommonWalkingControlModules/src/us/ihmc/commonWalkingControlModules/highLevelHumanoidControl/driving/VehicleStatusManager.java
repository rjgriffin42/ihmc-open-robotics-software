package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.driving;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import us.ihmc.utilities.CheckTools;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RevoluteJoint;
import us.ihmc.utilities.screwTheory.RigidBody;

/**
 * @author twan
 *         Date: 6/7/13
 */
public class VehicleStatusManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final RigidBody vehicle;

   private final EnumYoVariable<DrivingInterface.GearName> gear = new EnumYoVariable<DrivingInterface.GearName>("gear", registry,
                                                                            DrivingInterface.GearName.class);
   private final DoubleYoVariable steeringWheelAngle = new DoubleYoVariable("steeringWheelAngle", registry);

   private final BooleanYoVariable handBrakeEngaged = new BooleanYoVariable("handBrakeEngaged", registry);
   private final DoubleYoVariable gasPedalPosition = new DoubleYoVariable("gasPedalPosition", registry);
   private final DoubleYoVariable brakePedalPosition = new DoubleYoVariable("brakePedalPosition", registry);

   private final BooleanYoVariable isRobotHoldingSteeringWheel = new BooleanYoVariable("isRobotHoldingSteeringWheel", registry);

   private final double handBrakeEngagedAngle;
   private final double handBrakeDisengagedAngle;
   private final RevoluteJoint handBrakeJoint;


   public VehicleStatusManager(YoVariableRegistry parentRegistry, DrivingReferenceFrames drivingReferenceFrames, VehicleModelObjects vehicleModelObjects)
   {
      vehicle = new RigidBody("vehicle", drivingReferenceFrames.getVehicleFrame());
      ReferenceFrame handBrakeFrame = drivingReferenceFrames.getObjectFrame(VehicleObject.HAND_BRAKE);
      FrameVector axis = new FrameVector(handBrakeFrame, vehicleModelObjects.getHandBrakeAxis());
      handBrakeJoint = new RevoluteJoint("handBrakeJoint", vehicle, handBrakeFrame, axis);

      handBrakeEngagedAngle = vehicleModelObjects.getHandBrakeEngagedAngle();
      handBrakeDisengagedAngle = vehicleModelObjects.getHandBrakeDisengagedAngle();

      handBrakeJoint.setJointLimitLower(Math.min(handBrakeEngagedAngle, handBrakeDisengagedAngle));
      handBrakeJoint.setJointLimitLower(Math.max(handBrakeEngagedAngle, handBrakeDisengagedAngle));

      parentRegistry.addChild(registry);
   }

   public DrivingInterface.GearName getGear()
   {
      return gear.getEnumValue();
   }

   public double getSteeringWheelAngle()
   {
      return steeringWheelAngle.getDoubleValue();
   }

   public boolean isHandBrakeEngaged()
   {
      return handBrakeEngaged.getBooleanValue();
   }

   public void setGear(DrivingInterface.GearName gearName)
   {
      this.gear.set(gearName);
   }

   /**
    * negative means gas pedal is pressed, zero means gas pedal is at its default, unpressed position
    */
   public void setGasPedalPosition(double position)
   {
      MathTools.checkIfInRange(position, -Double.POSITIVE_INFINITY, 0.0);
      this.gasPedalPosition.set(position);
   }

   /**
    * negative means gas pedal is pressed, zero means gas pedal is at its default, unpressed position
    */
   public void setBrakePedalPosition(double position)
   {
      MathTools.checkIfInRange(position, -Double.POSITIVE_INFINITY, 0.0);
      this.brakePedalPosition.set(position);
   }

   public void setHandBrakeEngaged(boolean handBrakeEngaged)
   {
      this.handBrakeEngaged.set(handBrakeEngaged);
      if (handBrakeEngaged)
         handBrakeJoint.setQ(handBrakeEngagedAngle);
      else
         handBrakeJoint.setQ(handBrakeDisengagedAngle);
      handBrakeJoint.getFrameAfterJoint().update();
   }

   public void setSteeringWheelAngle(double steeringWheelAngle)
   {
      this.steeringWheelAngle.set(steeringWheelAngle);
   }

   public void setIsRobotHoldingSteeringWheel(boolean holdingSteeringWheel)
   {
      this.isRobotHoldingSteeringWheel.set(holdingSteeringWheel);
   }

   public RevoluteJoint getHandBrakeJoint()
   {
      return handBrakeJoint;
   }

   public double getHandBrakeDisengagedAngle()
   {
      return handBrakeDisengagedAngle;
   }

   public double getHandBrakeEngagedAngle()
   {
      return handBrakeEngagedAngle;
   }

   public boolean isBrakePedalPressed()
   {
      return brakePedalPosition.getDoubleValue() < 0.0;
   }

   public boolean isGasPedalPressed()
   {
      return gasPedalPosition.getDoubleValue() < 0.0;
   }

   public boolean isRobotHoldingSteeringWheel()
   {
      return isRobotHoldingSteeringWheel.getBooleanValue();
   }
}
