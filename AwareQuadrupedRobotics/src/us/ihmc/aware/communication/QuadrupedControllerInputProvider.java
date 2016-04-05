package us.ihmc.aware.communication;

import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.aware.params.DoubleArrayParameter;
import us.ihmc.aware.params.DoubleParameter;
import us.ihmc.aware.params.ParameterFactory;
import us.ihmc.aware.packets.*;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.quadrupedRobotics.dataProviders.QuadrupedControllerInputProviderInterface;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.RotationTools;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

public class QuadrupedControllerInputProvider implements QuadrupedControllerInputProviderInterface
{
   private final ParameterFactory parameterFactory = new ParameterFactory(QuadrupedControllerInputProvider.class.getName());
   private final DoubleParameter comHeightNominalParameter = parameterFactory.createDouble("comHeightNominal", 0.55);
   private final DoubleArrayParameter comPositionLowerLimitsParameter = parameterFactory.createDoubleArray("comPositionLowerLimits", -Double.MAX_VALUE, -Double.MAX_VALUE, 0.0);
   private final DoubleArrayParameter comPositionUpperLimitsParameter = parameterFactory.createDoubleArray("comPositionUpperLimits", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
   private final DoubleArrayParameter comVelocityLowerLimitsParameter = parameterFactory.createDoubleArray("comVelocityLowerLimits", -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
   private final DoubleArrayParameter comVelocityUpperLimitsParameter = parameterFactory.createDoubleArray("comVelocityUpperLimits", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
   private final DoubleArrayParameter bodyOrientationLowerLimitsParameter = parameterFactory.createDoubleArray("bodyOrientationLowerLimits", -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
   private final DoubleArrayParameter bodyOrientationUpperLimitsParameter = parameterFactory.createDoubleArray("bodyOrientationUpperLimits", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
   private final DoubleArrayParameter bodyAngularRateLowerLimitsParameter = parameterFactory.createDoubleArray("bodyAngularRateLowerLimits", -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
   private final DoubleArrayParameter bodyAngularRateUpperLimitsParameter = parameterFactory.createDoubleArray("bodyAngularRateUpperLimits", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
   private final DoubleArrayParameter planarVelocityLowerLimitsParameter = parameterFactory.createDoubleArray("planarVelocityLowerLimits", -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
   private final DoubleArrayParameter planarVelocityUpperLimitsParameter = parameterFactory.createDoubleArray("planarVelocityUpperLimits", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

   private final AtomicReference<ComPositionPacket> comPositionPacket;
   private final AtomicReference<ComVelocityPacket> comVelocityPacket;
   private final AtomicReference<BodyOrientationPacket> bodyOrientationPacket;
   private final AtomicReference<BodyAngularRatePacket> bodyAngularRatePacket;
   private final AtomicReference<PlanarVelocityPacket> planarVelocityPacket;
   private final DoubleYoVariable yoComPositionInputX;
   private final DoubleYoVariable yoComPositionInputY;
   private final DoubleYoVariable yoComPositionInputZ;
   private final DoubleYoVariable yoComVelocityInputX;
   private final DoubleYoVariable yoComVelocityInputY;
   private final DoubleYoVariable yoComVelocityInputZ;
   private final DoubleYoVariable yoBodyOrientationInputYaw;
   private final DoubleYoVariable yoBodyOrientationInputPitch;
   private final DoubleYoVariable yoBodyOrientationInputRoll;
   private final DoubleYoVariable yoBodyAngularRateInputX;
   private final DoubleYoVariable yoBodyAngularRateInputY;
   private final DoubleYoVariable yoBodyAngularRateInputZ;
   private final DoubleYoVariable yoPlanarVelocityInputX;
   private final DoubleYoVariable yoPlanarVelocityInputY;
   private final DoubleYoVariable yoPlanarVelocityInputZ;
   private final Point3d comPositionInput;
   private final Vector3d comVelocityInput;
   private final Quat4d bodyOrientationInput;
   private final Vector3d bodyAngularRateInput;
   private final Vector3d planarVelocityInput;

   public QuadrupedControllerInputProvider(GlobalDataProducer globalDataProducer, YoVariableRegistry registry)
   {
      comPositionPacket = new AtomicReference<>(new ComPositionPacket());
      comVelocityPacket = new AtomicReference<>(new ComVelocityPacket());
      bodyOrientationPacket = new AtomicReference<>(new BodyOrientationPacket());
      bodyAngularRatePacket = new AtomicReference<>(new BodyAngularRatePacket());
      planarVelocityPacket = new AtomicReference<>(new PlanarVelocityPacket());
      yoComPositionInputX = new DoubleYoVariable("comPositionInputX", registry);
      yoComPositionInputY = new DoubleYoVariable("comPositionInputY", registry);
      yoComPositionInputZ = new DoubleYoVariable("comPositionInputZ", registry);
      yoComVelocityInputX = new DoubleYoVariable("comVelocityInputX", registry);
      yoComVelocityInputY = new DoubleYoVariable("comVelocityInputY", registry);
      yoComVelocityInputZ = new DoubleYoVariable("comVelocityInputZ", registry);
      yoBodyOrientationInputYaw = new DoubleYoVariable("bodyOrientationInputYaw", registry);
      yoBodyOrientationInputPitch = new DoubleYoVariable("bodyOrientationInputPitch", registry);
      yoBodyOrientationInputRoll = new DoubleYoVariable("bodyOrientationInputRoll", registry);
      yoBodyAngularRateInputX = new DoubleYoVariable("bodyAngularRateInputX", registry);
      yoBodyAngularRateInputY = new DoubleYoVariable("bodyAngularRateInputY", registry);
      yoBodyAngularRateInputZ = new DoubleYoVariable("bodyAngularRateInputZ", registry);
      yoPlanarVelocityInputX = new DoubleYoVariable("planarVelocityInputX", registry);
      yoPlanarVelocityInputY = new DoubleYoVariable("planarVelocityInputY", registry);
      yoPlanarVelocityInputZ = new DoubleYoVariable("planarVelocityInputZ", registry);
      comPositionInput = new Point3d();
      comVelocityInput = new Vector3d();
      bodyOrientationInput = new Quat4d();
      bodyAngularRateInput = new Vector3d();
      planarVelocityInput = new Vector3d();

      // initialize com height
      yoComPositionInputZ.set(comHeightNominalParameter.get());

      globalDataProducer.attachListener(ComPositionPacket.class, new PacketConsumer<ComPositionPacket>()
      {
         @Override
         public void receivedPacket(ComPositionPacket packet)
         {
            comPositionPacket.set(packet);
            yoComPositionInputX.set(MathTools.clipToMinMax(comPositionPacket.get().getX(), comPositionLowerLimitsParameter.get(0), comPositionUpperLimitsParameter.get(0)));
            yoComPositionInputY.set(MathTools.clipToMinMax(comPositionPacket.get().getY(), comPositionLowerLimitsParameter.get(1), comPositionUpperLimitsParameter.get(1)));
            yoComPositionInputZ.set(MathTools.clipToMinMax(comPositionPacket.get().getZ(), comPositionLowerLimitsParameter.get(2), comPositionUpperLimitsParameter.get(2)));
         }
      });

      globalDataProducer.attachListener(ComVelocityPacket.class, new PacketConsumer<ComVelocityPacket>()
      {
         @Override
         public void receivedPacket(ComVelocityPacket packet)
         {
            comVelocityPacket.set(packet);
            yoComVelocityInputX.set(MathTools.clipToMinMax(comVelocityPacket.get().getX(), comVelocityLowerLimitsParameter.get(0), comVelocityUpperLimitsParameter.get(0)));
            yoComVelocityInputY.set(MathTools.clipToMinMax(comVelocityPacket.get().getY(), comVelocityLowerLimitsParameter.get(1), comVelocityUpperLimitsParameter.get(1)));
            yoComVelocityInputZ.set(MathTools.clipToMinMax(comVelocityPacket.get().getZ(), comVelocityLowerLimitsParameter.get(2), comVelocityUpperLimitsParameter.get(2)));
         }
      });

      globalDataProducer.attachListener(BodyOrientationPacket.class, new PacketConsumer<BodyOrientationPacket>()
      {
         @Override
         public void receivedPacket(BodyOrientationPacket packet)
         {
            yoBodyOrientationInputYaw.set(MathTools.clipToMinMax(bodyOrientationPacket.get().getYaw(), bodyOrientationLowerLimitsParameter.get(0), bodyOrientationUpperLimitsParameter.get(0)));
            yoBodyOrientationInputPitch.set(MathTools.clipToMinMax(bodyOrientationPacket.get().getPitch(), bodyOrientationLowerLimitsParameter.get(1), bodyOrientationUpperLimitsParameter.get(1)));
            yoBodyOrientationInputRoll.set(MathTools.clipToMinMax(bodyOrientationPacket.get().getRoll(), bodyOrientationLowerLimitsParameter.get(2), bodyOrientationUpperLimitsParameter.get(2)));
            bodyOrientationPacket.set(packet);
         }
      });

      globalDataProducer.attachListener(BodyAngularRatePacket.class, new PacketConsumer<BodyAngularRatePacket>()
      {
         @Override
         public void receivedPacket(BodyAngularRatePacket packet)
         {
            yoBodyAngularRateInputX.set(MathTools.clipToMinMax(bodyAngularRatePacket.get().getX(), bodyAngularRateLowerLimitsParameter.get(0), bodyAngularRateUpperLimitsParameter.get(0)));
            yoBodyAngularRateInputY.set(MathTools.clipToMinMax(bodyAngularRatePacket.get().getY(), bodyAngularRateLowerLimitsParameter.get(1), bodyAngularRateUpperLimitsParameter.get(1)));
            yoBodyAngularRateInputZ.set(MathTools.clipToMinMax(bodyAngularRatePacket.get().getZ(), bodyAngularRateLowerLimitsParameter.get(2), bodyAngularRateUpperLimitsParameter.get(2)));
            bodyAngularRatePacket.set(packet);
         }
      });

      globalDataProducer.attachListener(PlanarVelocityPacket.class, new PacketConsumer<PlanarVelocityPacket>()
      {
         @Override
         public void receivedPacket(PlanarVelocityPacket packet)
         {
            yoPlanarVelocityInputX.set(MathTools.clipToMinMax(planarVelocityPacket.get().getX(), planarVelocityLowerLimitsParameter.get(0), planarVelocityUpperLimitsParameter.get(0)));
            yoPlanarVelocityInputY.set(MathTools.clipToMinMax(planarVelocityPacket.get().getY(), planarVelocityLowerLimitsParameter.get(1), planarVelocityUpperLimitsParameter.get(1)));
            yoPlanarVelocityInputZ.set(MathTools.clipToMinMax(planarVelocityPacket.get().getZ(), planarVelocityLowerLimitsParameter.get(2), planarVelocityUpperLimitsParameter.get(2)));
            planarVelocityPacket.set(packet);
         }
      });
   }

   @Override
   public Point3d getComPositionInput()
   {
      comPositionInput.set(yoComPositionInputX.getDoubleValue(), yoComPositionInputY.getDoubleValue(), yoComPositionInputZ.getDoubleValue());
      return comPositionInput;
   }

   @Override
   public Vector3d getComVelocityInput()
   {
      comVelocityInput.set(yoComVelocityInputX.getDoubleValue(), yoComVelocityInputY.getDoubleValue(), yoComVelocityInputZ.getDoubleValue());
      return comVelocityInput;
   }

   @Override
   public Quat4d getBodyOrientationInput()
   {
      RotationTools.convertYawPitchRollToQuaternion(yoBodyOrientationInputYaw.getDoubleValue(), yoBodyOrientationInputPitch.getDoubleValue(), yoBodyOrientationInputRoll.getDoubleValue(), bodyOrientationInput);
      return bodyOrientationInput;
   }

   @Override
   public Vector3d getBodyAngularRateInput()
   {
      bodyAngularRateInput.set(yoBodyAngularRateInputX.getDoubleValue(), yoBodyAngularRateInputY.getDoubleValue(), yoBodyAngularRateInputZ.getDoubleValue());
      return bodyAngularRateInput;
   }

   @Override
   public Vector3d getPlanarVelocityInput()
   {
      planarVelocityInput.set(yoPlanarVelocityInputX.getDoubleValue(), yoPlanarVelocityInputY.getDoubleValue(), yoPlanarVelocityInputZ.getDoubleValue());
      return planarVelocityInput;
   }
}
