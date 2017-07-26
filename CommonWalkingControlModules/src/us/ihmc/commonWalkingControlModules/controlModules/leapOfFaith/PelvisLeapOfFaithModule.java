package us.ihmc.commonWalkingControlModules.controlModules.leapOfFaith;

import us.ihmc.commonWalkingControlModules.configurations.LeapOfFaithParameters;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class PelvisLeapOfFaithModule
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final String yoNamePrefix = "leapOfFaith";

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final YoFrameOrientation orientationOffset = new YoFrameOrientation(yoNamePrefix + "PelvisOrientationOffset", worldFrame, registry);

   private final YoBoolean isInSwing = new YoBoolean(yoNamePrefix + "IsInSwing", registry);
   private final YoBoolean usePelvisRotation = new YoBoolean(yoNamePrefix + "UsePelvisRotation", registry);
   private final YoBoolean relaxPelvis = new YoBoolean(yoNamePrefix + "RelaxPelvis", registry);

   private final YoDouble reachingYawGain = new YoDouble(yoNamePrefix + "PelvisReachingYawGain", registry);
   private final YoDouble reachingRollGain = new YoDouble(yoNamePrefix + "PelvisReachingRollGain", registry);
   private final YoDouble reachingMaxYaw = new YoDouble(yoNamePrefix + "PelvisReachingMaxYaw", registry);
   private final YoDouble reachingMaxRoll = new YoDouble(yoNamePrefix + "PelvisReachingMaxRoll", registry);
   private final YoDouble reachingFractionOfSwing = new YoDouble(yoNamePrefix + "PelvisReachingFractionOfSwing", registry);

   private final YoDouble relaxationRate = new YoDouble(yoNamePrefix + "RelaxationRate", registry);
   private final YoDouble relaxationFraction = new YoDouble(yoNamePrefix + "RelaxationFraction", registry);

   private final SideDependentList<? extends ReferenceFrame> soleZUpFrames;

   private RobotSide supportSide;
   private Footstep upcomingFootstep;

   private double stateDuration;

   public PelvisLeapOfFaithModule(SideDependentList<? extends ReferenceFrame> soleZUpFrames, LeapOfFaithParameters parameters,
                                  YoVariableRegistry parentRegistry)
   {
      this.soleZUpFrames = soleZUpFrames;

      usePelvisRotation.set(parameters.usePelvisRotation());
      relaxPelvis.set(parameters.relaxPelvisControl());

      reachingYawGain.set(parameters.getPelvisReachingYawGain());
      reachingRollGain.set(parameters.getPelvisReachingRollGain());
      reachingMaxYaw.set(parameters.getPelvisReachingMaxYaw());
      reachingMaxRoll.set(parameters.getPelvisReachingMaxRoll());
      reachingFractionOfSwing.set(parameters.getPelvisReachingFractionOfSwing());

      relaxationRate.set(parameters.getRelaxationRate());

      parentRegistry.addChild(registry);
   }

   public void setUpcomingFootstep(Footstep upcomingFootstep)
   {
      this.upcomingFootstep = upcomingFootstep;
      supportSide = upcomingFootstep.getRobotSide().getOppositeSide();
   }

   public void initializeStanding()
   {
      isInSwing.set(false);
   }

   public void initializeTransfer(double transferDuration)
   {
      stateDuration = transferDuration;
      isInSwing.set(false);
   }

   public void initializeSwing(double swingDuration)
   {
      stateDuration = swingDuration;

      isInSwing.set(true);
   }

   private final FramePoint tempPoint = new FramePoint();
   public void updateAngularOffsets(double currentTimeInState)
   {
      orientationOffset.setToZero();

      if (isInSwing.getBooleanValue() && usePelvisRotation.getBooleanValue())
      {
         double exceededTime = Math.max(currentTimeInState - reachingFractionOfSwing.getDoubleValue() * stateDuration, 0.0);

         if (exceededTime == 0.0)
            return;

         tempPoint.setToZero(upcomingFootstep.getSoleReferenceFrame());
         tempPoint.changeFrame(soleZUpFrames.get(supportSide));
         double stepLength = tempPoint.getX();

         double yawAngleOffset = reachingYawGain.getDoubleValue() * exceededTime * stepLength;
         double rollAngleOffset = reachingRollGain.getDoubleValue() * exceededTime;

         yawAngleOffset = MathTools.clamp(yawAngleOffset, reachingMaxYaw.getDoubleValue());
         rollAngleOffset = MathTools.clamp(rollAngleOffset, reachingMaxRoll.getDoubleValue());

         yawAngleOffset = supportSide.negateIfRightSide(yawAngleOffset);
         rollAngleOffset = supportSide.negateIfRightSide(rollAngleOffset);

         orientationOffset.setRoll(rollAngleOffset);
         orientationOffset.setYaw(yawAngleOffset);
      }
   }

   public void relaxAngularWeight(double currentTimeInState, Vector3D angularWeightToPack)
   {
      if (isInSwing.getBooleanValue() && relaxPelvis.getBooleanValue())
      {
         double exceededTime = Math.max(currentTimeInState - stateDuration, 0.0);
         double relaxationFraction;
         if (exceededTime > 0.0)
            relaxationFraction = MathTools.clamp(relaxationRate.getDoubleValue() * exceededTime + 0.2, 0.0, 1.0);
         else
            relaxationFraction = 0.0;
         this.relaxationFraction.set(relaxationFraction);

         angularWeightToPack.scale(1.0 - relaxationFraction);
      }
   }

   public void addAngularOffset(FrameOrientation orientationToPack)
   {
      orientationToPack.preMultiply(orientationOffset.getFrameOrientation());
   }
}
