package us.ihmc.simpleWholeBodyWalking.simpleSphere;

import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.robotics.controllers.PIDController;
import us.ihmc.robotics.controllers.pidGains.implementations.YoPIDGains;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class SimpleBasicHeightController
{
   private static final double kp = 100.0;
   private static final double ki = 0.0;
   private static final double kd = 10.0;

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final YoDouble yoDesiredHeight = new YoDouble("desiredHeight", registry);
   private final YoDouble weight = new YoDouble("weight", registry);

   private final PIDController heightController;

   private final YoDouble verticalForce = new YoDouble("verticalForce", registry);
   private final double controlDT;

   private final SimpleSphereRobot sphereRobot;
   private final FramePoint3DReadOnly centerOfMass;
   private final FrameVector3DReadOnly centerOfMassVelocity;

   public SimpleBasicHeightController(SimpleSphereRobot sphereRobot, YoRegistry parentRegistry)
   {
      this.controlDT = sphereRobot.getControlDT();
      this.sphereRobot = sphereRobot;

      yoDesiredHeight.set(sphereRobot.getDesiredHeight());

      centerOfMass = sphereRobot.getCenterOfMass();
      centerOfMassVelocity = sphereRobot.getCenterOfMassVelocity();

      YoPIDGains pidGains = new YoPIDGains("height", registry);
      pidGains.setKp(kp);
      pidGains.setKd(kd);
      pidGains.setKi(ki);

      heightController = new PIDController(pidGains, "heightController", registry);

      parentRegistry.addChild(registry);
   }

   public void doControl()
   {
      double z = centerOfMass.getZ();

      weight.set(sphereRobot.getScsRobot().getGravityZ() * sphereRobot.getTotalMass());
      verticalForce.set(heightController.compute(z, yoDesiredHeight.getDoubleValue(), centerOfMassVelocity.getZ(), 0.0, controlDT));
      verticalForce.add(Math.abs(weight.getDoubleValue()));
   }

   public double getVerticalForce()
   {
      return verticalForce.getDoubleValue();
   }
}
