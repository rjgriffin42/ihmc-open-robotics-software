package us.ihmc.commonWalkingControlModules.kinematics;

import javax.vecmath.Tuple3d;

import us.ihmc.sensorProcessing.stateEstimation.BodyVelocityEstimator;
import us.ihmc.sensorProcessing.stateEstimation.LegToTrustForVelocityReadOnly;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.Twist;


public class BodyVelocityEstimatorScrewTheory implements BodyVelocityEstimator
{
   private final String name;
   private final YoVariableRegistry registry;
   private final LegToTrustForVelocityReadOnly legToTrustForVelocity;
   private final ReferenceFrame footZUpFrame;
   private final RigidBody foot;
   private final InverseDynamicsJoint imuJoint;
   private final Twist bodyTwist = new Twist();
   private final FrameVector bodyLinearVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempLinearPart = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempAngularPart = new FrameVector(ReferenceFrame.getWorldFrame());
   private final RobotSide robotSide;
   private final DoubleYoVariable defaultCovariance;

   public BodyVelocityEstimatorScrewTheory(RigidBody foot, ReferenceFrame footZUpFrame, InverseDynamicsJoint imuJoint, RobotSide robotSide,
           LegToTrustForVelocityReadOnly legToTrustForVelocity, double defaultCovariance)
   {
      this.name = robotSide.getSideNameFirstLetter() + getClass().getSimpleName();
      this.registry = new YoVariableRegistry(name);
      this.legToTrustForVelocity = legToTrustForVelocity;
      this.robotSide = robotSide;
      this.foot = foot;
      this.footZUpFrame = footZUpFrame;
      this.imuJoint = imuJoint;
      this.defaultCovariance = new DoubleYoVariable(robotSide.getCamelCaseNameForStartOfExpression() + "VelocityScrewTheoryCovariance", registry);
      this.defaultCovariance.set(defaultCovariance);
   }

   public void estimateBodyVelocity()
   {
      getTwistOfAnkleWithRespectToIMU(bodyTwist, robotSide);
      tempLinearPart.setToZero(bodyTwist.getExpressedInFrame());
      bodyTwist.getLinearPart(tempLinearPart.getVector());

      getAngularVelocityOfAnkleZUpWithRespectToIMU(tempAngularPart, robotSide);
      bodyTwist.set(footZUpFrame, bodyTwist.getBaseFrame(), bodyTwist.getExpressedInFrame(), tempLinearPart.getVector(), tempAngularPart.getVector());

      bodyTwist.invert();
      bodyTwist.changeFrame(imuJoint.getFrameAfterJoint());
      bodyLinearVelocity.setToZero(bodyTwist.getExpressedInFrame());
      bodyTwist.getLinearPart(bodyLinearVelocity.getVector());
      bodyLinearVelocity.changeFrame(ReferenceFrame.getWorldFrame());
   }

   public void getBodyVelocity(FrameVector bodyVelocityToPack)
   {
      bodyVelocityToPack.setIncludingFrame(this.bodyLinearVelocity);
   }

   private final Twist jointTwist = new Twist();

   private void getTwistOfAnkleWithRespectToIMU(Twist twistToPack, RobotSide robotSide)
   {
      RigidBody currentBody = foot;
      ReferenceFrame footFrame = foot.getParentJoint().getFrameAfterJoint();
      twistToPack.setToZero(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), footFrame);

      while (currentBody != imuJoint.getSuccessor())
      {
         currentBody.getParentJoint().getPredecessorTwist(jointTwist);
         jointTwist.changeFrame(twistToPack.getExpressedInFrame());
         twistToPack.add(jointTwist);
         currentBody = currentBody.getParentJoint().getPredecessor();
      }

      twistToPack.invert();
      twistToPack.changeBodyFrameNoRelativeTwist(footFrame);
      twistToPack.changeBaseFrameNoRelativeTwist(imuJoint.getFrameAfterJoint());
   }

   private void getAngularVelocityOfAnkleZUpWithRespectToIMU(FrameVector angularVelocityToPack, RobotSide robotSide)
   {
      ReferenceFrame footFrame = foot.getParentJoint().getFrameAfterJoint();

      imuJoint.getJointTwist(jointTwist);
      angularVelocityToPack.setToZero(jointTwist.getExpressedInFrame());
      jointTwist.getAngularPart(angularVelocityToPack.getVector());    // angular velocity of IMU w.r.t world == IMU w.r.t ankle ZUp by assumption, in IMU frame
      angularVelocityToPack.changeFrame(footFrame);    // angular velocity of IMU w.r.t. ankle ZUp, in foot frame
      angularVelocityToPack.scale(-1.0);    // angular velocity of ankle ZUp w.r.t. IMU, in foot frame
   }

   public void getCovariance(Tuple3d covarianceToPack)
   {
      // TODO: also use angular velocity of foot with respect to ground. Create FootAngularVelocityCalculator; do only once
      double covariance = this.legToTrustForVelocity.isLegTrustedForVelocity(robotSide) ? defaultCovariance.getDoubleValue() : Double.POSITIVE_INFINITY;
      covarianceToPack.set(covariance, covariance, covariance);
   }
}
