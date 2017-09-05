package us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator;

import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP.WalkingTrajectoryType;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.math.trajectories.FrameTrajectory3D;
import us.ihmc.robotics.math.trajectories.SegmentedFrameTrajectory3D;
import us.ihmc.robotics.math.trajectories.YoFrameTrajectory3D;

public class AngularMomentumTrajectory extends SegmentedFrameTrajectory3D implements AngularMomentumTrajectoryInterface
{
   private FrameVector3D momentum;
   private FrameVector3D torque;
   private FrameVector3D rotatum;
   private FrameTrajectory3D torqueTrajectory;

   public AngularMomentumTrajectory(int stepNumber, WalkingTrajectoryType type, ReferenceFrame referenceFrame,
                                    int maxNumberOfSegments, int maxNumberOfCoefficients)
   {
      super(maxNumberOfSegments, maxNumberOfCoefficients);
      momentum = new FrameVector3D(referenceFrame);
      torque = new FrameVector3D(referenceFrame);
      rotatum = new FrameVector3D(referenceFrame);
      torqueTrajectory = new FrameTrajectory3D(maxNumberOfCoefficients - 1, referenceFrame);
   }

   @Override
   public void reset()
   {
      super.reset();
      momentum.setToNaN();
      torque.setToNaN();
      rotatum.setToNaN();
   }

   @Override
   public void update(double timeInState, FrameVector3D desiredAngularMomentumToPack)
   {
      update(timeInState);
      desiredAngularMomentumToPack.setIncludingFrame(currentSegment.getFramePosition());
   }

   @Override
   public void update(double timeInState, FrameVector3D desiredAngularMomentumToPack, FrameVector3D desiredTorqueToPack)
   {
      update(timeInState, desiredAngularMomentumToPack);
      desiredTorqueToPack.setIncludingFrame(currentSegment.getFrameVelocity());
   }

   @Override
   public void update(double timeInState, FrameVector3D desiredAngularMomentumToPack, FrameVector3D desiredTorqueToPack, FrameVector3D desiredRotatumToPack)
   {
      update(timeInState, desiredAngularMomentumToPack, desiredTorqueToPack);
      desiredRotatumToPack.setIncludingFrame(currentSegment.getFrameAcceleration());
   }

   @Override
   public void set(FrameTrajectory3D computedAngularMomentumTrajectory)
   {
      segments.get(getNumberOfSegments()).set(computedAngularMomentumTrajectory);
      numberOfSegments++;
   }

   public void set(double t0, double tFinal, FramePoint3D z0, FramePoint3D zf)
   {
      segments.get(getNumberOfSegments()).setLinear(t0, tFinal, z0, zf);
      numberOfSegments++;
   }
}
