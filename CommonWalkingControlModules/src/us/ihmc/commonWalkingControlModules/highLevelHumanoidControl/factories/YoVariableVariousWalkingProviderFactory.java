package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.UserDesiredFootstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootStateProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredThighLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.FootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredPelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetProducers.CapturabilityBasedStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProducers.HandPoseStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.ControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.DesiredHighLevelStateProvider;
import us.ihmc.commonWalkingControlModules.packetProviders.SystemErrControlStatusProducer;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantTransferTimeCalculator;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.humanoidRobot.footstep.Footstep;

public class YoVariableVariousWalkingProviderFactory implements VariousWalkingProviderFactory
{
   public VariousWalkingProviders createVariousWalkingProviders(DoubleYoVariable yoTime, FullRobotModel fullRobotModel,
         WalkingControllerParameters walkingControllerParameters, CommonHumanoidReferenceFrames referenceFrames, SideDependentList<ContactablePlaneBody> feet,
         ConstantTransferTimeCalculator transferTimeCalculator, ConstantSwingTimeCalculator swingTimeCalculator, ArrayList<Updatable> updatables,
         YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      HandstepProvider handstepProvider = new UserDesiredHandstepProvider(fullRobotModel, registry, yoGraphicsListRegistry);

      HandPoseProvider handPoseProvider = new UserDesiredHandPoseProvider(fullRobotModel,
            walkingControllerParameters.getDesiredHandPosesWithRespectToChestFrame(), registry);

      LinkedHashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters = new LinkedHashMap<Footstep, TrajectoryParameters>();

      FootstepProvider footstepPovider = new UserDesiredFootstepProvider(feet, referenceFrames.getAnkleZUpReferenceFrames(), registry);

      DesiredHighLevelStateProvider highLevelStateProvider = null;
      HeadOrientationProvider headOrientationProvider = new UserDesiredHeadOrientationProvider(referenceFrames.getPelvisZUpFrame(), registry);
      DesiredComHeightProvider desiredComHeightProvider = null;
      PelvisPoseProvider pelvisPoseProvider = new UserDesiredPelvisPoseProvider(registry);
      ChestOrientationProvider chestOrientationProvider = new UserDesiredChestOrientationProvider(referenceFrames.getPelvisZUpFrame(), registry);

      FootPoseProvider footPoseProvider = new UserDesiredFootPoseProvider(fullRobotModel, registry);

      HandLoadBearingProvider handLoadBearingProvider = new UserDesiredHandLoadBearingProvider(registry);
      DesiredFootStateProvider footLoadBearingProvider = null;
      DesiredThighLoadBearingProvider thighLoadBearingProvider = null;
      DesiredPelvisLoadBearingProvider pelvisLoadBearingProvider = null;

      ControlStatusProducer controlStatusProducer = new SystemErrControlStatusProducer();

      CapturabilityBasedStatusProducer capturabilityBasedStatusProducer = null;

      HandPoseStatusProducer handPoseStatusProducer = null;

      VariousWalkingProviders variousProviders = new VariousWalkingProviders(footstepPovider, handstepProvider, mapFromFootstepsToTrajectoryParameters,
            headOrientationProvider, desiredComHeightProvider, pelvisPoseProvider, handPoseProvider, handLoadBearingProvider, chestOrientationProvider,
            footPoseProvider, footLoadBearingProvider, highLevelStateProvider, thighLoadBearingProvider, pelvisLoadBearingProvider, controlStatusProducer,
            capturabilityBasedStatusProducer, handPoseStatusProducer);

      return variousProviders;
   }

}
