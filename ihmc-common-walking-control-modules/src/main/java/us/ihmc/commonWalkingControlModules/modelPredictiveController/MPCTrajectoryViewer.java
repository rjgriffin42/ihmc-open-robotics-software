package us.ihmc.commonWalkingControlModules.modelPredictiveController;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.yoVariables.registry.YoRegistry;

public class MPCTrajectoryViewer
{
   private static final int numberOfVectors = 100;
   private static final double dt = 0.025;
   private static final double ballSize = 0.005;

   private final BagOfVectors comTrajectoryVectors;
   private final BagOfVectors dcmTrajectoryVectors;
   private final BagOfVectors vrpTrajectoryVectors;

   private final FramePoint3D comPositionToThrowAway = new FramePoint3D();
   private final FrameVector3D comVelocityToThrowAway = new FrameVector3D();
   private final FrameVector3D comAccelerationToThrowAway = new FrameVector3D();
   private final FramePoint3D dcmPositionToThrowAway = new FramePoint3D();
   private final FrameVector3D dcmVelocityToThrowAway = new FrameVector3D();
   private final FramePoint3D vrpPositionToThrowAway = new FramePoint3D();
   private final FrameVector3D vrpVelocityToThrowAway = new FrameVector3D();
   private final FramePoint3D ecmpPositionToThrowAway = new FramePoint3D();

   public MPCTrajectoryViewer(YoRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      comTrajectoryVectors = new BagOfVectors(numberOfVectors, dt, ballSize, "CoM", YoAppearance.Black(), registry, yoGraphicsListRegistry);
      dcmTrajectoryVectors = new BagOfVectors(numberOfVectors, dt, ballSize, "DCM", YoAppearance.Yellow(), registry, yoGraphicsListRegistry);
      vrpTrajectoryVectors = new BagOfVectors(numberOfVectors, dt, ballSize, "VRP", YoAppearance.Green(), registry, yoGraphicsListRegistry);
   }

   public void compute(CoMTrajectoryModelPredictiveController mpc, double currentTimeInState)
   {
      comTrajectoryVectors.reset();
      dcmTrajectoryVectors.reset();
      vrpTrajectoryVectors.reset();

      for (int i = 0; i < numberOfVectors; i++)
      {
         double time = dt * i + currentTimeInState;

         mpc.compute(time,
                     comPositionToThrowAway,
                     comVelocityToThrowAway,
                     comAccelerationToThrowAway,
                     dcmPositionToThrowAway,
                     dcmVelocityToThrowAway,
                     vrpPositionToThrowAway,
                     vrpVelocityToThrowAway,
                     ecmpPositionToThrowAway);

         dcmTrajectoryVectors.setVector(dcmPositionToThrowAway, dcmVelocityToThrowAway);
         vrpTrajectoryVectors.setVector(vrpPositionToThrowAway, vrpVelocityToThrowAway);
         comTrajectoryVectors.setVector(comPositionToThrowAway, comVelocityToThrowAway);
      }
   }
}
