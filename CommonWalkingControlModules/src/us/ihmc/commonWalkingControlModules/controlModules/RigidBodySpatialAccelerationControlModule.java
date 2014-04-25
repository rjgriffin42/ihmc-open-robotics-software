package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class RigidBodySpatialAccelerationControlModule
{
   private static final boolean VISUALIZE = true;

   private final YoVariableRegistry registry;
   private final TwistCalculator twistCalculator;
   private final SE3PDController se3pdController;
   private final SpatialAccelerationVector acceleration;
   private final RigidBody endEffector;
   private final ReferenceFrame endEffectorFrame;

   private final YoFrameVector desiredAccelerationLinearViz, desiredAccelerationAngularViz;

   private final DoubleYoVariable maximumLinearAccelerationMagnitude, maximumAngularAccelerationMagnitude;
   private final BooleanYoVariable limitAccelerations;

   public RigidBodySpatialAccelerationControlModule(String namePrefix, TwistCalculator twistCalculator, RigidBody endEffector, ReferenceFrame endEffectorFrame,
         double dt, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      this.twistCalculator = twistCalculator;
      this.endEffector = endEffector;
      this.endEffectorFrame = endEffectorFrame;
      this.se3pdController = new SE3PDController(namePrefix, endEffectorFrame, VISUALIZE, dt, registry);
      this.acceleration = new SpatialAccelerationVector();

      desiredAccelerationLinearViz = new YoFrameVector(namePrefix + "LinearAccelViz", endEffectorFrame, registry);
      desiredAccelerationAngularViz = new YoFrameVector(namePrefix + "AngularAccelViz", endEffectorFrame, registry);

      maximumLinearAccelerationMagnitude = new DoubleYoVariable(namePrefix + "MaxLinearAccelMagnitude", registry);
      maximumAngularAccelerationMagnitude = new DoubleYoVariable(namePrefix + "MaxAngularAccelMagnitude", registry);
      limitAccelerations = new BooleanYoVariable(namePrefix + "LimitAccelerations", registry);
      limitAccelerations.set(false);

      parentRegistry.addChild(registry);
   }

   public void reset()
   {
      se3pdController.reset();
   }

   public void setMaximumLinearAccelerationMagnitude(double maximumLinearAccelerationMagnitude)
   {
      this.maximumLinearAccelerationMagnitude.set(maximumLinearAccelerationMagnitude);
   }

   public void setMaximumAngularAccelerationMagnitude(double maximumAngularAccelerationMagnitude)
   {
      this.maximumAngularAccelerationMagnitude.set(maximumAngularAccelerationMagnitude);
   }

   public void setLimitAccelerations(boolean limitAccelerations)
   {
      this.limitAccelerations.set(limitAccelerations);
   }

   public RigidBody getEndEffector()
   {
      return endEffector;
   }

   public void packAcceleration(SpatialAccelerationVector accelerationToPack)
   {
      accelerationToPack.set(acceleration);
   }

   public FrameVector getPositionErrorInWorld()
   {
      FrameVector ret = new FrameVector(endEffectorFrame);
      se3pdController.getPositionError(ret);
      ret.changeFrame(ReferenceFrame.getWorldFrame());

      return ret;
   }

   Twist currentTwist = new Twist();
   public void doPositionControl(FramePose desiredEndEffectorPose, Twist desiredEndEffectorTwist,
         SpatialAccelerationVector feedForwardEndEffectorSpatialAcceleration, RigidBody base)
   {
      twistCalculator.packRelativeTwist(currentTwist, base, endEffector);
      currentTwist.changeBodyFrameNoRelativeTwist(endEffectorFrame);
      currentTwist.changeFrame(endEffectorFrame);

      se3pdController.compute(acceleration, desiredEndEffectorPose, desiredEndEffectorTwist, feedForwardEndEffectorSpatialAcceleration, currentTwist);

      if (limitAccelerations.getBooleanValue())
      {
         acceleration.limitLinearPartMagnitude(maximumLinearAccelerationMagnitude.getDoubleValue());
         acceleration.limitAngularPartMagnitude(maximumAngularAccelerationMagnitude.getDoubleValue());
      }

      acceleration.getExpressedInFrame().checkReferenceFrameMatch(desiredAccelerationLinearViz.getReferenceFrame());

      desiredAccelerationLinearViz.set(acceleration.getLinearPartCopy());
      desiredAccelerationAngularViz.set(acceleration.getAngularPartCopy());
   }

   FramePose desiredEndEffectorPose = new FramePose();
   Twist desiredEndEffectorTwist = new Twist();
   SpatialAccelerationVector feedForwardEndEffectorSpatialAcceleration = new SpatialAccelerationVector();

   public void doPositionControl(FramePoint desiredPosition, FrameOrientation desiredOrientation, FrameVector desiredLinearVelocityOfOrigin,
         FrameVector desiredAngularVelocity, FrameVector desiredLinearAccelerationOfOrigin, FrameVector desiredAngularAcceleration, RigidBody base)
   {
      packDesiredEndEffectorPoseFromDesiredPositions(desiredEndEffectorPose, desiredPosition, desiredOrientation);
      packDesiredEndEffectorTwist(desiredEndEffectorTwist, desiredLinearVelocityOfOrigin, desiredAngularVelocity, base);
      calculateDesiredEndEffectorSpatialAcceleration(feedForwardEndEffectorSpatialAcceleration, desiredLinearAccelerationOfOrigin, desiredAngularAcceleration, base);
      doPositionControl(desiredEndEffectorPose, desiredEndEffectorTwist, feedForwardEndEffectorSpatialAcceleration, base);
   }

   FramePoint endEffectorPosition = new FramePoint();
   FrameOrientation endEffectorOrientation = new FrameOrientation();

   public void packDesiredEndEffectorPoseFromDesiredPositions(FramePose poseToPack, FramePoint endEffectorPositionIn, FrameOrientation endEffectorOrientationIn)
   {
      endEffectorPosition.setIncludingFrame(endEffectorPositionIn);
      endEffectorPosition.changeFrame(endEffectorFrame);

      endEffectorOrientation.setAndChangeFrame(endEffectorOrientationIn);
      endEffectorOrientation.changeFrame(endEffectorFrame);

      poseToPack.setIncludingFrame(endEffectorPosition, endEffectorOrientation);
   }

   public void packDesiredEndEffectorTwist(Twist twistToPack, FrameVector linearVelocityOfOrigin, FrameVector angularVelocity, RigidBody base)
   {
      angularVelocity.changeFrame(endEffectorFrame);
      linearVelocityOfOrigin.changeFrame(endEffectorFrame);

      twistToPack.set(endEffectorFrame, base.getBodyFixedFrame(), endEffectorFrame, linearVelocityOfOrigin.getVector(), angularVelocity.getVector());
   }

   Twist twistOfEndEffectorWithRespectToElevator = new Twist();

   public void calculateDesiredEndEffectorSpatialAcceleration(SpatialAccelerationVector toPack, FrameVector linearAccelerationOfOrigin,
         FrameVector angularAcceleration, RigidBody base)
   {
      angularAcceleration.changeFrame(endEffectorFrame);

      linearAccelerationOfOrigin.changeFrame(endEffectorFrame);
      twistCalculator.packRelativeTwist(twistOfEndEffectorWithRespectToElevator, base, endEffector);
      twistOfEndEffectorWithRespectToElevator.changeBodyFrameNoRelativeTwist(endEffectorFrame);

      toPack.setBasedOnOriginAcceleration(endEffectorFrame, base.getBodyFixedFrame(), endEffectorFrame, angularAcceleration, linearAccelerationOfOrigin,
            twistOfEndEffectorWithRespectToElevator);
   }

   public void setPositionProportionalGains(double kpx, double kpy, double kpz)
   {
      se3pdController.setPositionProportionalGains(kpx, kpy, kpz);
   }

   public void setPositionDerivativeGains(double kdx, double kdy, double kdz)
   {
      se3pdController.setPositionDerivativeGains(kdx, kdy, kdz);
   }

   public void setPositionIntegralGains(double kix, double kiy, double kiz, double maxIntegralError)
   {
      se3pdController.setPositionIntegralGains(kix, kiy, kiz, maxIntegralError);
   }

   public void setOrientationProportionalGains(double kpx, double kpy, double kpz)
   {
      se3pdController.setOrientationProportionalGains(kpx, kpy, kpz);
   }

   public void setOrientationDerivativeGains(double kdx, double kdy, double kdz)
   {
      se3pdController.setOrientationDerivativeGains(kdx, kdy, kdz);
   }

   public void setOrientationIntegralGains(double kix, double kiy, double kiz, double maxIntegralError)
   {
      se3pdController.setOrientationIntegralGains(kix, kiy, kiz, maxIntegralError);
   }

   public ReferenceFrame getTrackingFrame()
   {
      return endEffectorFrame;
   }

   public void setGains(SE3PDGains gains)
   {
      se3pdController.setGains(gains);
   }

   public void setPositionMaxAccelerationAndJerk(double maxAcceleration, double maxJerk)
   {
      se3pdController.setPositionMaxAccelerationAndJerk(maxAcceleration, maxJerk);
   }

   public void setOrientationMaxAccelerationAndJerk(double maxAcceleration, double maxJerk)
   {
      se3pdController.setOrientationMaxAccelerationAndJerk(maxAcceleration, maxJerk);
   }
   
   public void packEndeffectorVelocity(FrameVector vectorToPack)
   {
      // check that...
      vectorToPack.setIncludingFrame(currentTwist.getBodyOriginLinearVelocityInBaseFrame());
   }
   
   public void packEndeffectorPosition(FramePoint pointToPack)
   {
      // check that...
      pointToPack.setIncludingFrame(endEffectorPosition);
   }
}
