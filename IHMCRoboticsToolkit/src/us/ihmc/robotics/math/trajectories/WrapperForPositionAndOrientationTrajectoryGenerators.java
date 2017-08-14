package us.ihmc.robotics.math.trajectories;

import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector3D;


public class WrapperForPositionAndOrientationTrajectoryGenerators implements PoseTrajectoryGenerator
{
   private final PositionTrajectoryGenerator positionTrajectoryGenerator;
   private final OrientationTrajectoryGenerator orientationTrajectoryGenerator;
   
   public WrapperForPositionAndOrientationTrajectoryGenerators(PositionTrajectoryGenerator positionTrajectoryGenerator, OrientationTrajectoryGenerator orientationTrajectoryGenerator)
   {
      this.positionTrajectoryGenerator = positionTrajectoryGenerator;
      this.orientationTrajectoryGenerator = orientationTrajectoryGenerator;
   }

   public void initialize()
   {
      positionTrajectoryGenerator.initialize();
      orientationTrajectoryGenerator.initialize();
   }

   public void compute(double time)
   {
      positionTrajectoryGenerator.compute(time);
      orientationTrajectoryGenerator.compute(time);
   }

   public boolean isDone()
   {
      return positionTrajectoryGenerator.isDone() && orientationTrajectoryGenerator.isDone();
   }

   public void getPosition(FramePoint3D positionToPack)
   {
      positionTrajectoryGenerator.getPosition(positionToPack);
   }

   public void getVelocity(FrameVector3D velocityToPack)
   {
      positionTrajectoryGenerator.getVelocity(velocityToPack);
   }

   public void getAcceleration(FrameVector3D accelerationToPack)
   {
      positionTrajectoryGenerator.getAcceleration(accelerationToPack);
   }

   public void getLinearData(FramePoint3D positionToPack, FrameVector3D velocityToPack, FrameVector3D accelerationToPack)
   {
      positionTrajectoryGenerator.getLinearData(positionToPack, velocityToPack, accelerationToPack);
   }

   public void getOrientation(FrameOrientation orientationToPack)
   {
      orientationTrajectoryGenerator.getOrientation(orientationToPack);
   }
   
   public void getAngularVelocity(FrameVector3D angularVelocityToPack)
   {
      orientationTrajectoryGenerator.getAngularVelocity(angularVelocityToPack);
   }

   public void getAngularAcceleration(FrameVector3D angularAccelerationToPack)
   {
      orientationTrajectoryGenerator.getAngularAcceleration(angularAccelerationToPack);
   }

   public void getAngularData(FrameOrientation orientationToPack, FrameVector3D angularVelocityToPack, FrameVector3D angularAccelerationToPack)
   {
      orientationTrajectoryGenerator.getAngularData(orientationToPack, angularVelocityToPack, angularAccelerationToPack);
   }
   
   private final FramePoint3D tempPosition = new FramePoint3D();
   private final FrameOrientation tempOrientation = new FrameOrientation();
   
   public void getPose(FramePose framePoseToPack)
   {
      positionTrajectoryGenerator.getPosition(tempPosition);
      framePoseToPack.changeFrame(tempPosition.getReferenceFrame());
      framePoseToPack.setPosition(tempPosition);

      orientationTrajectoryGenerator.getOrientation(tempOrientation);
      framePoseToPack.setOrientation(tempOrientation);
   }

   public void showVisualization()
   {
      positionTrajectoryGenerator.showVisualization();
   }

   public void hideVisualization()
   {
      positionTrajectoryGenerator.hideVisualization();
   }
}
