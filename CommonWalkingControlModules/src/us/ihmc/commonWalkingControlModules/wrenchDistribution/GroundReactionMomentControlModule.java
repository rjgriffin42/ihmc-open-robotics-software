package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import javax.vecmath.Matrix3d;

import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.utilities.screwTheory.Momentum;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;


public class GroundReactionMomentControlModule
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame pelvisFrame;
   private final DoubleYoVariable kAngularMomentumZ = new DoubleYoVariable("kAngularMomentumZ", registry);
   private final DoubleYoVariable kPelvisYaw = new DoubleYoVariable("kPelvisYaw", registry);

   public GroundReactionMomentControlModule(ReferenceFrame pelvisFrame, YoVariableRegistry parentRegistry)
   {
      this.pelvisFrame = pelvisFrame;
      parentRegistry.addChild(registry);
   }

   public FrameVector determineGroundReactionMoment(Momentum momentum, double desiredPelvisYaw)
   {
      FrameVector ret = new FrameVector(worldFrame);
      FrameVector angularMomentum = new FrameVector(momentum.getExpressedInFrame(), momentum.getAngularPartCopy());
      angularMomentum.changeFrame(worldFrame);

      Matrix3d pelvisToWorld = new Matrix3d();
      pelvisFrame.getTransformToDesiredFrame(worldFrame).get(pelvisToWorld);
      double pelvisYaw = RotationFunctions.getYaw(pelvisToWorld);

      double error = AngleTools.computeAngleDifferenceMinusPiToPi(desiredPelvisYaw, pelvisYaw);
      ret.setZ(-kAngularMomentumZ.getDoubleValue() * angularMomentum.getZ() + kPelvisYaw.getDoubleValue() * error);

      return ret;
   }

   public void setGains(double kAngularMomentumZ, double kPelvisYaw)
   {
      this.kAngularMomentumZ.set(kAngularMomentumZ);
      this.kPelvisYaw.set(kPelvisYaw);
   }
}
