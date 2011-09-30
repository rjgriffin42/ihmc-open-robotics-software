package us.ihmc.commonWalkingControlModules.kinematics;

import javax.vecmath.Tuple3d;

import us.ihmc.commonWalkingControlModules.sensors.LegToTrustForVelocityReadOnly;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class BodyVelocityEstimatorScrewTheory implements BodyVelocityEstimator
{
   private final String name;
   private final YoVariableRegistry registry;
   private final LegToTrustForVelocityReadOnly legToTrustForVelocity;
   private final ReferenceFrame footZUpFrame;
   private final RigidBody foot;
   private final SixDoFJoint imuJoint;
   private final Twist bodyTwist = new Twist();
   private final FrameVector bodyLinearVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempLinearPart = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempAngularPart = new FrameVector(ReferenceFrame.getWorldFrame());
   private final RobotSide robotSide;
   private final DoubleYoVariable defaultCovariance;

   public BodyVelocityEstimatorScrewTheory(RigidBody foot, ReferenceFrame footZUpFrame, SixDoFJoint imuJoint, RobotSide robotSide,
           LegToTrustForVelocityReadOnly legToTrustForVelocity, double defaultCovariance)
   {
      this.name = robotSide + getClass().getSimpleName();
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
      packTwistOfAnkleWithRespectToIMU(bodyTwist, robotSide);
      tempLinearPart.setToZero(bodyTwist.getExpressedInFrame());
      bodyTwist.packLinearPart(tempLinearPart.getVector());

      packAngularVelocityOfAnkleZUpWithRespectToIMU(tempAngularPart, robotSide);
      bodyTwist.set(footZUpFrame, bodyTwist.getBaseFrame(), bodyTwist.getExpressedInFrame(), tempLinearPart.getVector(), tempAngularPart.getVector());

      bodyTwist.invert();
      bodyTwist.changeFrame(imuJoint.getFrameAfterJoint());
      bodyLinearVelocity.setToZero(bodyTwist.getExpressedInFrame());
      bodyTwist.packLinearPart(bodyLinearVelocity.getVector());
      bodyLinearVelocity.changeFrame(ReferenceFrame.getWorldFrame());
   }

   public void packBodyVelocity(FrameVector bodyVelocityToPack)
   {
      bodyVelocityToPack.setAndChangeFrame(this.bodyLinearVelocity);
   }

   private final Twist jointTwist = new Twist();

   private void packTwistOfAnkleWithRespectToIMU(Twist twistToPack, RobotSide robotSide)
   {
      RigidBody currentBody = foot;
      ReferenceFrame footFrame = foot.getParentJoint().getFrameAfterJoint();
      twistToPack.setToZero(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), footFrame);

      while (currentBody != imuJoint.getSuccessor())
      {
         currentBody.getParentJoint().packPredecessorTwist(jointTwist);
         jointTwist.changeFrame(twistToPack.getExpressedInFrame());
         twistToPack.add(jointTwist);
         currentBody = currentBody.getParentJoint().getPredecessor();
      }

      twistToPack.invert();
      twistToPack.changeBodyFrameNoRelativeTwist(footFrame);
      twistToPack.changeBaseFrameNoRelativeTwist(imuJoint.getFrameAfterJoint());
   }

   private void packAngularVelocityOfAnkleZUpWithRespectToIMU(FrameVector angularVelocityToPack, RobotSide robotSide)
   {
      ReferenceFrame footFrame = foot.getParentJoint().getFrameAfterJoint();

      imuJoint.packJointTwist(jointTwist);
      angularVelocityToPack.setToZero(jointTwist.getExpressedInFrame());
      jointTwist.packAngularPart(angularVelocityToPack.getVector());    // angular velocity of IMU w.r.t world == IMU w.r.t ankle ZUp by assumption, in IMU frame
      angularVelocityToPack.changeFrame(footFrame);    // angular velocity of IMU w.r.t. ankle ZUp, in foot frame
      angularVelocityToPack.scale(-1.0);    // angular velocity of ankle ZUp w.r.t. IMU, in foot frame
   }

   public void packCovariance(Tuple3d covarianceToPack)
   {
      // TODO: also use angular velocity of foot with respect to ground. Create FootAngularVelocityCalculator; do only once
      double covariance;
      if (legToTrustForVelocity == null)
      {
         covariance = defaultCovariance.getDoubleValue();
      }
      else
      {
         RobotSide legToTrustForVelocity = this.legToTrustForVelocity.getLegToTrustForVelocity();
         if (legToTrustForVelocity == null)
         {
            covariance = defaultCovariance.getDoubleValue();
         }
         else
         {
            boolean trustingLegForVelocity = legToTrustForVelocity == robotSide;
            covariance = trustingLegForVelocity ? defaultCovariance.getDoubleValue() : Double.POSITIVE_INFINITY;
         }
      }

      covarianceToPack.set(covariance, covariance, covariance);
   }

   public void configureAfterEstimation()
   {      
   }
}
