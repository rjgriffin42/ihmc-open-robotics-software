package us.ihmc.quadrupedRobotics.planning.bodyPath;

import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.euclid.referenceFrame.FramePose2D;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class QuadrupedBodyPathMultiplexer implements QuadrupedPlanarBodyPathProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final QuadrupedWaypointBasedBodyPathProvider waypointBasedPath;
   private final QuadrupedConstantVelocityBodyPathProvider joystickBasedPath;

   private YoBoolean usingJoystickBasedPath = new YoBoolean("usingJoystickBasedPath", registry);

   public QuadrupedBodyPathMultiplexer(QuadrupedReferenceFrames referenceFrames, YoDouble timestamp, PacketCommunicator packetCommunicator, YoVariableRegistry parentRegistry)
   {
      waypointBasedPath = new QuadrupedWaypointBasedBodyPathProvider(referenceFrames, packetCommunicator, timestamp, registry);
      joystickBasedPath = new QuadrupedConstantVelocityBodyPathProvider(referenceFrames, Double.NaN, timestamp, registry);

      parentRegistry.addChild(registry);
   }

   @Override
   public void initialize()
   {
      usingJoystickBasedPath.set(false);
      joystickBasedPath.initialize();
   }

   @Override
   public void getPlanarPose(double time, FramePose2D poseToPack)
   {
      if(usingJoystickBasedPath.getBooleanValue())
      {
         if(waypointBasedPath.bodyPathIsAvailable())
         {
            waypointBasedPath.initialize();
            waypointBasedPath.getPlanarPose(time, poseToPack);
            usingJoystickBasedPath.set(false);
         }
         else
         {
            joystickBasedPath.getPlanarPose(time, poseToPack);
         }
      }
      else
      {
         if(waypointBasedPath.isDone())
         {
            joystickBasedPath.initialize();
            joystickBasedPath.getPlanarPose(time, poseToPack);
            usingJoystickBasedPath.set(true);
         }
         else
         {
            waypointBasedPath.getPlanarPose(time, poseToPack);
         }
      }
   }

   public void setPlanarVelocity(double desiredVelocityX, double desiredVelocityY, double desiredVelocityYaw)
   {
      joystickBasedPath.setPlanarVelocity(desiredVelocityX, desiredVelocityY, desiredVelocityYaw);
   }
}
