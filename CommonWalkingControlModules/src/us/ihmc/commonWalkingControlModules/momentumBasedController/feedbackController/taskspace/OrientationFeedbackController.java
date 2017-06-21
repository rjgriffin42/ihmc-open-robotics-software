package us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.taskspace;

import static us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerDataReadOnly.Space.*;
import static us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerDataReadOnly.Type.*;

import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.SpatialVelocityCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.FeedbackControllerInterface;
import us.ihmc.euclid.matrix.interfaces.Matrix3DReadOnly;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.BooleanYoVariable;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.filters.RateLimitedYoFrameVector;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.robotics.screwTheory.SpatialAccelerationVector;
import us.ihmc.robotics.screwTheory.Twist;

public class OrientationFeedbackController implements FeedbackControllerInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry;

   private final BooleanYoVariable isEnabled;

   private final YoFrameQuaternion yoDesiredOrientation;
   private final YoFrameQuaternion yoCurrentOrientation;
   private final YoFrameQuaternion yoErrorOrientation;

   private final YoFrameQuaternion yoErrorOrientationCumulated;

   private final YoFrameVector yoDesiredRotationVector;
   private final YoFrameVector yoCurrentRotationVector;
   private final YoFrameVector yoErrorRotationVector;

   private final YoFrameVector yoErrorRotationVectorIntegrated;

   private final YoFrameVector yoDesiredAngularVelocity;
   private final YoFrameVector yoCurrentAngularVelocity;
   private final YoFrameVector yoErrorAngularVelocity;
   private final YoFrameVector yoFeedForwardAngularVelocity;
   private final YoFrameVector yoFeedbackAngularVelocity;
   private final RateLimitedYoFrameVector rateLimitedFeedbackAngularVelocity;

   private final YoFrameVector yoDesiredAngularAcceleration;
   private final YoFrameVector yoFeedForwardAngularAcceleration;
   private final YoFrameVector yoFeedbackAngularAcceleration;
   private final RateLimitedYoFrameVector rateLimitedFeedbackAngularAcceleration;
   private final YoFrameVector yoAchievedAngularAcceleration;

   private final FrameOrientation desiredOrientation = new FrameOrientation();
   private final FrameOrientation currentOrientation = new FrameOrientation();
   private final FrameOrientation errorOrientationCumulated = new FrameOrientation();

   private final FrameVector desiredAngularVelocity = new FrameVector();
   private final FrameVector currentAngularVelocity = new FrameVector();
   private final FrameVector feedForwardAngularVelocity = new FrameVector();

   private final FrameVector desiredAngularAcceleration = new FrameVector();
   private final FrameVector feedForwardAngularAcceleration = new FrameVector();
   private final FrameVector achievedAngularAcceleration = new FrameVector();

   private final Twist currentTwist = new Twist();
   private final SpatialAccelerationVector endEffectorAchievedAcceleration = new SpatialAccelerationVector();

   private final SpatialAccelerationCommand inverseDynamicsOutput = new SpatialAccelerationCommand();
   private final SpatialVelocityCommand inverseKinematicsOutput = new SpatialVelocityCommand();

   private final YoOrientationPIDGainsInterface gains;
   private final Matrix3DReadOnly kp, kd, ki;

   private final SpatialAccelerationCalculator spatialAccelerationCalculator;

   private RigidBody base;
   private ReferenceFrame controlBaseFrame;
   private ReferenceFrame angularGainsFrame;

   private final RigidBody endEffector;
   private final MovingReferenceFrame endEffectorFrame;

   private final double dt;

   public OrientationFeedbackController(RigidBody endEffector, WholeBodyControlCoreToolbox toolbox, FeedbackControllerToolbox feedbackControllerToolbox,
                                        YoVariableRegistry parentRegistry)
   {
      this.endEffector = endEffector;

      spatialAccelerationCalculator = toolbox.getSpatialAccelerationCalculator();

      String endEffectorName = endEffector.getName();
      registry = new YoVariableRegistry(endEffectorName + "OrientationFBController");
      dt = toolbox.getControlDT();
      gains = feedbackControllerToolbox.getOrientationGains(endEffector);
      kp = gains.createProportionalGainMatrix();
      kd = gains.createDerivativeGainMatrix();
      ki = gains.createIntegralGainMatrix();
      DoubleYoVariable maximumRate = gains.getYoMaximumFeedbackRate();

      endEffectorFrame = endEffector.getBodyFixedFrame();

      isEnabled = new BooleanYoVariable(endEffectorName + "IsOrientationFBControllerEnabled", registry);
      isEnabled.set(false);

      yoDesiredOrientation = feedbackControllerToolbox.getOrientation(endEffector, DESIRED, isEnabled);
      yoCurrentOrientation = feedbackControllerToolbox.getOrientation(endEffector, CURRENT, isEnabled);
      yoErrorOrientation = feedbackControllerToolbox.getOrientation(endEffector, ERROR, isEnabled);

      yoErrorOrientationCumulated = feedbackControllerToolbox.getOrientation(endEffector, ERROR_CUMULATED, isEnabled);

      yoDesiredRotationVector = feedbackControllerToolbox.getDataVector(endEffector, DESIRED, ROTATION_VECTOR, isEnabled);
      yoCurrentRotationVector = feedbackControllerToolbox.getDataVector(endEffector, CURRENT, ROTATION_VECTOR, isEnabled);
      yoErrorRotationVector = feedbackControllerToolbox.getDataVector(endEffector, ERROR, ROTATION_VECTOR, isEnabled);

      yoErrorRotationVectorIntegrated = feedbackControllerToolbox.getDataVector(endEffector, ERROR_INTEGRATED, ROTATION_VECTOR, isEnabled);

      yoDesiredAngularVelocity = feedbackControllerToolbox.getDataVector(endEffector, DESIRED, ANGULAR_VELOCITY, isEnabled);

      if (toolbox.isEnableInverseDynamicsModule() || toolbox.isEnableVirtualModelControlModule())
      {
         yoCurrentAngularVelocity = feedbackControllerToolbox.getDataVector(endEffector, CURRENT, ANGULAR_VELOCITY, isEnabled);
         yoErrorAngularVelocity = feedbackControllerToolbox.getDataVector(endEffector, ERROR, ANGULAR_VELOCITY, isEnabled);

         yoDesiredAngularAcceleration = feedbackControllerToolbox.getDataVector(endEffector, DESIRED, ANGULAR_ACCELERATION, isEnabled);
         yoFeedForwardAngularAcceleration = feedbackControllerToolbox.getDataVector(endEffector, FEEDFORWARD, ANGULAR_ACCELERATION, isEnabled);
         yoFeedbackAngularAcceleration = feedbackControllerToolbox.getDataVector(endEffector, FEEDBACK, ANGULAR_ACCELERATION, isEnabled);
         rateLimitedFeedbackAngularAcceleration = feedbackControllerToolbox.getRateLimitedDataVector(endEffector, FEEDBACK, ANGULAR_ACCELERATION, dt,
                                                                                                     maximumRate, isEnabled);
         yoAchievedAngularAcceleration = feedbackControllerToolbox.getDataVector(endEffector, ACHIEVED, ANGULAR_ACCELERATION, isEnabled);
      }
      else
      {
         yoCurrentAngularVelocity = null;
         yoErrorAngularVelocity = null;

         yoDesiredAngularAcceleration = null;
         yoFeedForwardAngularAcceleration = null;
         yoFeedbackAngularAcceleration = null;
         rateLimitedFeedbackAngularAcceleration = null;
         yoAchievedAngularAcceleration = null;
      }

      if (toolbox.isEnableInverseKinematicsModule())
      {
         yoFeedbackAngularVelocity = feedbackControllerToolbox.getDataVector(endEffector, FEEDBACK, ANGULAR_VELOCITY, isEnabled);
         yoFeedForwardAngularVelocity = feedbackControllerToolbox.getDataVector(endEffector, FEEDFORWARD, ANGULAR_ACCELERATION, isEnabled);
         rateLimitedFeedbackAngularVelocity = feedbackControllerToolbox.getRateLimitedDataVector(endEffector, FEEDBACK, ANGULAR_VELOCITY, dt, maximumRate,
                                                                                                 isEnabled);
      }
      else
      {
         yoFeedbackAngularVelocity = null;
         yoFeedForwardAngularVelocity = null;
         rateLimitedFeedbackAngularVelocity = null;
      }

      parentRegistry.addChild(registry);
   }

   public void submitFeedbackControlCommand(OrientationFeedbackControlCommand command)
   {
      if (command.getEndEffector() != endEffector)
         throw new RuntimeException("Wrong end effector - received: " + command.getEndEffector() + ", expected: " + endEffector);

      base = command.getBase();
      controlBaseFrame = command.getControlBaseFrame();

      inverseDynamicsOutput.set(command.getSpatialAccelerationCommand());
      inverseKinematicsOutput.setProperties(command.getSpatialAccelerationCommand());

      gains.set(command.getGains());
      angularGainsFrame = command.getAngularGainsFrame();
      command.getIncludingFrame(desiredOrientation, desiredAngularVelocity, feedForwardAngularAcceleration);

      yoDesiredOrientation.setAndMatchFrame(desiredOrientation);
      yoDesiredRotationVector.setAsRotationVector(desiredOrientation);

      yoDesiredAngularVelocity.setAndMatchFrame(desiredAngularVelocity);

      if (yoFeedForwardAngularVelocity != null)
         yoFeedForwardAngularVelocity.setAndMatchFrame(desiredAngularVelocity);

      if (yoFeedForwardAngularAcceleration != null)
         yoFeedForwardAngularAcceleration.setAndMatchFrame(feedForwardAngularAcceleration);
   }

   @Override
   public void setEnabled(boolean isEnabled)
   {
      this.isEnabled.set(isEnabled);
   }

   @Override
   public void initialize()
   {
      if (rateLimitedFeedbackAngularAcceleration != null)
         rateLimitedFeedbackAngularAcceleration.reset();
      if (rateLimitedFeedbackAngularVelocity != null)
         rateLimitedFeedbackAngularVelocity.reset();
   }

   private final FrameVector proportionalFeedback = new FrameVector();
   private final FrameVector derivativeFeedback = new FrameVector();
   private final FrameVector integralFeedback = new FrameVector();

   @Override
   public void computeInverseDynamics()
   {
      if (!isEnabled())
         return;

      computeProportionalTerm(proportionalFeedback);
      computeDerivativeTerm(derivativeFeedback);
      computeIntegralTerm(integralFeedback);
      yoFeedForwardAngularAcceleration.getFrameTupleIncludingFrame(feedForwardAngularAcceleration);
      feedForwardAngularAcceleration.changeFrame(endEffectorFrame);

      desiredAngularAcceleration.setIncludingFrame(proportionalFeedback);
      desiredAngularAcceleration.add(derivativeFeedback);
      desiredAngularAcceleration.add(integralFeedback);
      desiredAngularAcceleration.limitLength(gains.getMaximumFeedback());
      yoFeedbackAngularAcceleration.setAndMatchFrame(desiredAngularAcceleration);
      rateLimitedFeedbackAngularAcceleration.update();
      rateLimitedFeedbackAngularAcceleration.getFrameTupleIncludingFrame(desiredAngularAcceleration);

      desiredAngularAcceleration.changeFrame(endEffectorFrame);
      desiredAngularAcceleration.add(feedForwardAngularAcceleration);

      yoDesiredAngularAcceleration.setAndMatchFrame(desiredAngularAcceleration);

      inverseDynamicsOutput.setAngularAcceleration(endEffectorFrame, desiredAngularAcceleration);
   }

   @Override
   public void computeInverseKinematics()
   {
      if (!isEnabled())
         return;

      inverseKinematicsOutput.setProperties(inverseDynamicsOutput);

      yoFeedForwardAngularVelocity.getFrameTupleIncludingFrame(feedForwardAngularVelocity);
      computeProportionalTerm(proportionalFeedback);
      computeIntegralTerm(integralFeedback);

      desiredAngularVelocity.setIncludingFrame(proportionalFeedback);
      desiredAngularVelocity.add(integralFeedback);
      desiredAngularVelocity.limitLength(gains.getMaximumFeedback());
      yoFeedbackAngularVelocity.setAndMatchFrame(desiredAngularVelocity);
      rateLimitedFeedbackAngularVelocity.update();
      rateLimitedFeedbackAngularVelocity.getFrameTupleIncludingFrame(desiredAngularVelocity);

      desiredAngularVelocity.add(feedForwardAngularVelocity);

      yoDesiredAngularVelocity.setAndMatchFrame(desiredAngularVelocity);

      desiredAngularVelocity.changeFrame(endEffectorFrame);
      inverseKinematicsOutput.setAngularVelocity(endEffectorFrame, desiredAngularVelocity);
   }

   @Override
   public void computeVirtualModelControl()
   {
      computeInverseDynamics();
   }

   @Override
   public void computeAchievedAcceleration()
   {
      spatialAccelerationCalculator.getRelativeAcceleration(base, endEffector, endEffectorAchievedAcceleration);
      endEffectorAchievedAcceleration.getAngularPart(achievedAngularAcceleration);

      yoAchievedAngularAcceleration.setAndMatchFrame(achievedAngularAcceleration);
   }

   /**
    * Computes the feedback term resulting from the error in orientation:<br>
    * x<sub>FB</sub> = kp * &theta;<sub>error</sub><br>
    * where &theta;<sub>error</sub> is a rotation vector representing the current error in
    * orientation.
    * <p>
    * The desired orientation of the {@code endEffectorFrame} is obtained from
    * {@link #yoDesiredOrientation}.
    * </p>
    * <p>
    * This method also updates {@link #yoCurrentOrientation}, {@link #yoCurrentRotationVector}, and
    * {@link #yoErrorPosition}.
    * </p>
    * 
    * @param feedbackTermToPack the value of the feedback term x<sub>FB</sub>. Modified.
    */
   private void computeProportionalTerm(FrameVector feedbackTermToPack)
   {
      currentOrientation.setToZero(endEffectorFrame);
      currentOrientation.changeFrame(worldFrame);
      yoCurrentOrientation.set(currentOrientation);
      yoCurrentRotationVector.setAsRotationVector(yoCurrentOrientation);

      yoDesiredOrientation.getFrameOrientationIncludingFrame(desiredOrientation);
      desiredOrientation.changeFrame(endEffectorFrame);

      desiredOrientation.normalizeAndLimitToPiMinusPi();
      desiredOrientation.getRotationVectorIncludingFrame(feedbackTermToPack);
      feedbackTermToPack.limitLength(gains.getMaximumProportionalError());
      yoErrorRotationVector.setAndMatchFrame(feedbackTermToPack);
      yoErrorOrientation.setRotationVector(yoErrorRotationVector);

      if (angularGainsFrame != null)
         feedbackTermToPack.changeFrame(angularGainsFrame);
      else
         feedbackTermToPack.changeFrame(endEffectorFrame);

      kp.transform(feedbackTermToPack.getVector());

      feedbackTermToPack.changeFrame(endEffectorFrame);
   }

   /**
    * Computes the feedback term resulting from the error in angular velocity:<br>
    * x<sub>FB</sub> = kd * (&omega;<sub>desired</sub> - &omega;<sub>current</sub>)
    * <p>
    * The desired angular velocity of the {@code endEffectorFrame} relative to the {@code base} is
    * obtained from {@link #yoDesiredAngularVelocity}.
    * </p>
    * <p>
    * This method also updates {@link #yoCurrentAngularVelocity} and
    * {@link #yoErrorAngularVelocity}.
    * </p>
    * 
    * @param feedbackTermToPack the value of the feedback term x<sub>FB</sub>. Modified.
    */
   public void computeDerivativeTerm(FrameVector feedbackTermToPack)
   {
      endEffectorFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
      currentTwist.getAngularPart(currentAngularVelocity);
      currentAngularVelocity.changeFrame(worldFrame);
      yoCurrentAngularVelocity.set(currentAngularVelocity);

      yoDesiredAngularVelocity.getFrameTupleIncludingFrame(desiredAngularVelocity);

      feedbackTermToPack.setToZero(worldFrame);
      feedbackTermToPack.sub(desiredAngularVelocity, currentAngularVelocity);
      feedbackTermToPack.limitLength(gains.getMaximumDerivativeError());
      yoErrorAngularVelocity.set(feedbackTermToPack);

      if (angularGainsFrame != null)
         feedbackTermToPack.changeFrame(angularGainsFrame);
      else
         feedbackTermToPack.changeFrame(endEffectorFrame);

      kd.transform(feedbackTermToPack.getVector());

      feedbackTermToPack.changeFrame(endEffectorFrame);
   }

   /**
    * Computes the feedback term resulting from the integrated error in orientation:<br>
    * x<sub>FB</sub> = ki * &int;<sup>t</sup> &theta;<sub>error</sub>
    * <p>
    * The current error in orientation of the {@code endEffectorFrame} is obtained from
    * {@link #yoErrorOrientation}.
    * </p>
    * <p>
    * This method also updates {@link #yoErrorOrientationCumulated} and
    * {@link #yoErrorRotationVectorIntegrated}.
    * </p>
    * 
    * @param feedbackTermToPack the value of the feedback term x<sub>FB</sub>. Modified.
    */
   public void computeIntegralTerm(FrameVector feedbackTermToPack)
   {
      double maximumIntegralError = gains.getMaximumIntegralError();

      if (maximumIntegralError < 1.0e-5)
      {
         feedbackTermToPack.setToZero(endEffectorFrame);
         yoErrorOrientationCumulated.setToZero();
         yoErrorRotationVectorIntegrated.setToZero();
         return;
      }

      yoErrorOrientationCumulated.getFrameOrientationIncludingFrame(errorOrientationCumulated);
      errorOrientationCumulated.multiply(yoErrorOrientation.getFrameOrientation());
      yoErrorOrientationCumulated.set(errorOrientationCumulated);
      errorOrientationCumulated.normalizeAndLimitToPiMinusPi();

      errorOrientationCumulated.getRotationVectorIncludingFrame(feedbackTermToPack);
      feedbackTermToPack.scale(dt);
      feedbackTermToPack.limitLength(maximumIntegralError);
      yoErrorRotationVectorIntegrated.set(feedbackTermToPack);

      if (angularGainsFrame != null)
         feedbackTermToPack.changeFrame(angularGainsFrame);
      else
         feedbackTermToPack.changeFrame(endEffectorFrame);

      ki.transform(feedbackTermToPack.getVector());

      feedbackTermToPack.changeFrame(endEffectorFrame);
   }

   @Override
   public boolean isEnabled()
   {
      return isEnabled.getBooleanValue();
   }

   @Override
   public SpatialAccelerationCommand getInverseDynamicsOutput()
   {
      if (!isEnabled())
         throw new RuntimeException("This controller is disabled.");
      return inverseDynamicsOutput;
   }

   @Override
   public SpatialVelocityCommand getInverseKinematicsOutput()
   {
      if (!isEnabled())
         throw new RuntimeException("This controller is disabled.");
      return inverseKinematicsOutput;
   }

   @Override
   public SpatialAccelerationCommand getVirtualModelControlOutput()
   {
      return getInverseDynamicsOutput();
   }
}
