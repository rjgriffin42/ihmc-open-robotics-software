package us.ihmc.robotics.trajectories.providers;

import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.robotics.screwTheory.TwistCalculator;

public class CurrentAngularVelocityProvider implements VectorProvider
{
   private final ReferenceFrame referenceFrame;
   private final RigidBody rigidBody;
   private final TwistCalculator twistCalculator;
   private final Twist twist = new Twist();

   public CurrentAngularVelocityProvider(ReferenceFrame referenceFrame, RigidBody rigidBody, TwistCalculator twistCalculator)
   {
      this.referenceFrame = referenceFrame;
      this.rigidBody = rigidBody;
      this.twistCalculator = twistCalculator;
   }

   public void get(FrameVector frameVectorToPack)
   {
      twistCalculator.packTwistOfBody(twist, rigidBody);
      twist.changeFrame(referenceFrame);
      twist.packAngularPart(frameVectorToPack);
   }
}
