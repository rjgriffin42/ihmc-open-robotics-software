package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.BlindWalkingPacketConsumer;
import us.ihmc.commonWalkingControlModules.desiredFootStep.BlindWalkingToDestinationDesiredFootstepCalculator;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepPathConsumer;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepPathCoordinator;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepTimingParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.PauseCommandConsumer;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootStateProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredJointsPositionProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredThighLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ObjectWeightProvider;
import us.ihmc.commonWalkingControlModules.packetProducers.CapturabilityBasedStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProducers.HandPoseStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.ControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.DesiredHighLevelStateProvider;
import us.ihmc.commonWalkingControlModules.packetProviders.NetworkControlStatusProducer;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantTransferTimeCalculator;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packets.BumStatePacket;
import us.ihmc.communication.packets.HighLevelStatePacket;
import us.ihmc.communication.packets.manipulation.HandLoadBearingPacket;
import us.ihmc.communication.packets.manipulation.HandPoseListPacket;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.manipulation.HandRotateAboutAxisPacket;
import us.ihmc.communication.packets.manipulation.HandstepPacket;
import us.ihmc.communication.packets.manipulation.ObjectWeightPacket;
import us.ihmc.communication.packets.manipulation.StopArmMotionPacket;
import us.ihmc.communication.packets.sensing.LookAtPacket;
import us.ihmc.communication.packets.walking.BlindWalkingPacket;
import us.ihmc.communication.packets.walking.ChestOrientationPacket;
import us.ihmc.communication.packets.walking.ComHeightPacket;
import us.ihmc.communication.packets.walking.FootPosePacket;
import us.ihmc.communication.packets.walking.FootStatePacket;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.communication.packets.walking.HeadOrientationPacket;
import us.ihmc.communication.packets.walking.PauseCommand;
import us.ihmc.communication.packets.walking.PelvisPosePacket;
import us.ihmc.communication.packets.walking.ThighStatePacket;
import us.ihmc.communication.packets.wholebody.JointAnglesPacket;
import us.ihmc.communication.packets.wholebody.WholeBodyTrajectoryPacket;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.utilities.humanoidRobot.footstep.Footstep;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;


public class DataProducerVariousWalkingProviderFactory implements VariousWalkingProviderFactory
{
   private final GlobalDataProducer objectCommunicator;
   private final FootstepTimingParameters footstepTimingParameters;

   public DataProducerVariousWalkingProviderFactory(GlobalDataProducer objectCommunicator, FootstepTimingParameters footstepTimingParameters)
   {
      this.objectCommunicator = objectCommunicator;
      this.footstepTimingParameters = footstepTimingParameters;
   }

   public VariousWalkingProviders createVariousWalkingProviders(DoubleYoVariable yoTime, FullRobotModel fullRobotModel,
           WalkingControllerParameters walkingControllerParameters, CommonHumanoidReferenceFrames referenceFrames, SideDependentList<ContactablePlaneBody> feet,
           ConstantTransferTimeCalculator transferTimeCalculator, ConstantSwingTimeCalculator swingTimeCalculator, ArrayList<Updatable> updatables,
           YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      DesiredHandstepProvider handstepProvider = new DesiredHandstepProvider(fullRobotModel);

      DesiredHandPoseProvider handPoseProvider = new DesiredHandPoseProvider(fullRobotModel,
                                                    walkingControllerParameters.getDesiredHandPosesWithRespectToChestFrame(), objectCommunicator);
      PacketConsumer<StopArmMotionPacket> handPauseCommandConsumer = handPoseProvider.getHandPauseCommandConsumer();
      HandPoseStatusProducer handPoseStatusProducer = new HandPoseStatusProducer(objectCommunicator);
      
      LinkedHashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters = new LinkedHashMap<Footstep, TrajectoryParameters>();

      BlindWalkingToDestinationDesiredFootstepCalculator desiredFootstepCalculator =
         HighLevelHumanoidControllerFactoryHelper.getBlindWalkingToDestinationDesiredFootstepCalculator(walkingControllerParameters, referenceFrames, feet,
            registry);

      CapturabilityBasedStatusProducer capturabilityBasedStatusProducer = new CapturabilityBasedStatusProducer(objectCommunicator);

      FootstepPathCoordinator footstepPathCoordinator = new FootstepPathCoordinator(footstepTimingParameters, objectCommunicator, desiredFootstepCalculator,
                                                           swingTimeCalculator, transferTimeCalculator, registry);

      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer(feet, footstepPathCoordinator, mapFromFootstepsToTrajectoryParameters);
      BlindWalkingPacketConsumer blindWalkingPacketConsumer = new BlindWalkingPacketConsumer(footstepPathCoordinator);
      PauseCommandConsumer pauseCommandConsumer = new PauseCommandConsumer(footstepPathCoordinator);
      DesiredHighLevelStateProvider highLevelStateProvider = new DesiredHighLevelStateProvider();
      DesiredHeadOrientationProvider headOrientationProvider = new DesiredHeadOrientationProvider(fullRobotModel.getChest().getBodyFixedFrame());
      DesiredComHeightProvider desiredComHeightProvider = new DesiredComHeightProvider(objectCommunicator);
      DesiredPelvisPoseProvider pelvisPoseProvider = new DesiredPelvisPoseProvider();
      DesiredChestOrientationProvider chestOrientationProvider = new DesiredChestOrientationProvider(ReferenceFrame.getWorldFrame(), walkingControllerParameters.getTrajectoryTimeHeadOrientation());
      DesiredFootPoseProvider footPoseProvider = new DesiredFootPoseProvider(walkingControllerParameters.getDefaultSwingTime());
   
      DesiredJointsPositionProvider desiredJointsPositionProvider = new DesiredJointsPositionProvider();
      

      DesiredHandLoadBearingProvider handLoadBearingProvider = new DesiredHandLoadBearingProvider();
      DesiredFootStateProvider footLoadBearingProvider = new DesiredFootStateProvider();
      DesiredThighLoadBearingProvider thighLoadBearingProvider = new DesiredThighLoadBearingProvider();
      DesiredPelvisLoadBearingProvider pelvisLoadBearingProvider = new DesiredPelvisLoadBearingProvider();
      
      ObjectWeightProvider objectWeightProvider = new ObjectWeightProvider();

      objectCommunicator.attachListener(FootstepDataList.class, footstepPathConsumer);
      objectCommunicator.attachListener(HandstepPacket.class, handstepProvider);
      objectCommunicator.attachListener(BlindWalkingPacket.class, blindWalkingPacketConsumer);
      objectCommunicator.attachListener(PauseCommand.class, pauseCommandConsumer);
      objectCommunicator.attachListener(HighLevelStatePacket.class, highLevelStateProvider);
      objectCommunicator.attachListener(HeadOrientationPacket.class, headOrientationProvider.getHeadOrientationPacketConsumer());
      
      objectCommunicator.attachListener(ComHeightPacket.class,           desiredComHeightProvider.getComHeightPacketConsumer());
      objectCommunicator.attachListener(WholeBodyTrajectoryPacket.class, desiredComHeightProvider.getWholeBodyPacketConsumer());
      
      objectCommunicator.attachListener(LookAtPacket.class, headOrientationProvider.getLookAtPacketConsumer());
      objectCommunicator.attachListener(StopArmMotionPacket.class, handPauseCommandConsumer);
      objectCommunicator.attachListener(FootPosePacket.class, footPoseProvider);

      objectCommunicator.attachListener(ChestOrientationPacket.class, chestOrientationProvider);
      objectCommunicator.attachListener(WholeBodyTrajectoryPacket.class, chestOrientationProvider.getWholeBodyTrajectoryPacketConsumer());
      
      objectCommunicator.attachListener(PelvisPosePacket.class, pelvisPoseProvider);
      objectCommunicator.attachListener(WholeBodyTrajectoryPacket.class, pelvisPoseProvider.getWholeBodyTrajectoryPacketConsumer());
    
      objectCommunicator.attachListener(HandPosePacket.class, handPoseProvider);
      objectCommunicator.attachListener(HandPoseListPacket.class, handPoseProvider.getHandPoseListConsumer());
      objectCommunicator.attachListener(HandRotateAboutAxisPacket.class, handPoseProvider.getHandRotateAboutAxisConsumer());
      objectCommunicator.attachListener(HandLoadBearingPacket.class, handLoadBearingProvider);
      objectCommunicator.attachListener(WholeBodyTrajectoryPacket.class, handPoseProvider.getWholeBodyTrajectoryPacketConsumer());

      objectCommunicator.attachListener(FootStatePacket.class, footLoadBearingProvider);
      objectCommunicator.attachListener(ThighStatePacket.class, thighLoadBearingProvider);
      objectCommunicator.attachListener(BumStatePacket.class, pelvisLoadBearingProvider);
      
      objectCommunicator.attachListener(ObjectWeightPacket.class, objectWeightProvider);
     
      objectCommunicator.attachListener( JointAnglesPacket.class , desiredJointsPositionProvider.getPacketConsumer() );
      
      
      ControlStatusProducer controlStatusProducer = new NetworkControlStatusProducer(objectCommunicator);

      VariousWalkingProviders variousWalkingProviders = new VariousWalkingProviders(footstepPathCoordinator, handstepProvider,
                                                           mapFromFootstepsToTrajectoryParameters, headOrientationProvider, desiredComHeightProvider,
                                                           pelvisPoseProvider, handPoseProvider, handLoadBearingProvider, chestOrientationProvider,
                                                           footPoseProvider, footLoadBearingProvider, highLevelStateProvider, thighLoadBearingProvider,
                                                           pelvisLoadBearingProvider, controlStatusProducer, capturabilityBasedStatusProducer, handPoseStatusProducer,
                                                           objectWeightProvider,desiredJointsPositionProvider );

      return variousWalkingProviders;
   }

}
