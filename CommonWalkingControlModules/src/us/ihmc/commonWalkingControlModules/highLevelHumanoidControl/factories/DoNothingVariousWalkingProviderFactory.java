package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.AbortWalkingProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorFootstepProviderWrapper;
import us.ihmc.commonWalkingControlModules.packetConsumers.AutomaticManipulationAbortCommunicator;
import us.ihmc.commonWalkingControlModules.packetConsumers.ChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootStateProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredJointsPositionProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredSteeringWheelProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredThighLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandComplianceControlParametersProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.MultiJointPositionProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ObjectWeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.SingleJointPositionProvider;
import us.ihmc.commonWalkingControlModules.packetProducers.CapturabilityBasedStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProducers.HandPoseStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.ControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.DesiredHighLevelStateProvider;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantTransferTimeCalculator;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.utilities.humanoidRobot.footstep.Footstep;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;


public class DoNothingVariousWalkingProviderFactory implements VariousWalkingProviderFactory    // extends ComponentBasedVariousWalkingProviderFactory
{
   public DoNothingVariousWalkingProviderFactory(double controlDT)
   {
   }


   public VariousWalkingProviders createVariousWalkingProviders(DoubleYoVariable yoTime, FullRobotModel fullRobotModel,
           WalkingControllerParameters walkingControllerParameters, CommonHumanoidReferenceFrames referenceFrames,
           SideDependentList<ContactablePlaneBody> feet, ConstantTransferTimeCalculator transferTimeCalculator,
           ConstantSwingTimeCalculator swingTimeCalculator, ArrayList<Updatable> updatables, YoVariableRegistry registry,
           YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      HandstepProvider handstepProvider = null;
      DesiredFootstepCalculatorFootstepProviderWrapper footstepProvider = null;
      LinkedHashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters = new LinkedHashMap<Footstep, TrajectoryParameters>();

      DesiredHighLevelStateProvider highLevelStateProvider = null;
      HeadOrientationProvider headOrientationProvider = null;
      DesiredComHeightProvider comHeightProvider = null;
      PelvisPoseProvider pelvisPoseProvider = null;
      ChestOrientationProvider chestOrientationProvider = null;
      DesiredHandPoseProvider handPoseProvider = null;
      DesiredFootPoseProvider footPoseProvider = null;


      HandLoadBearingProvider handLoadBearingProvider = null;
      DesiredFootStateProvider footLoadBearingProvider = null;
      DesiredPelvisLoadBearingProvider pelvisLoadBearingProvider = null;
      DesiredThighLoadBearingProvider thighLoadBearingProvider = null;

      ControlStatusProducer controlStatusProducer = null;

      CapturabilityBasedStatusProducer capturabilityBasedStatusProducer = null;

      HandPoseStatusProducer handPoseStatusProducer = null;
      ObjectWeightProvider objectWeightProvider = null;
      
      DesiredJointsPositionProvider desiredJointsPositionProvider = null;
      SingleJointPositionProvider singleJointPositionProvider = null;
      MultiJointPositionProvider multiJointPositionProvider = null;
      AbortWalkingProvider abortWalkingProvider = null;

      HandComplianceControlParametersProvider handComplianceControlParametersProvider = null;

      DesiredSteeringWheelProvider desiredSteeringWheelProvider = null;

      AutomaticManipulationAbortCommunicator automaticManipulationAbortCommunicator = null;

      VariousWalkingProviders variousWalkingProviders = new VariousWalkingProviders(footstepProvider, handstepProvider, mapFromFootstepsToTrajectoryParameters,
                                                           headOrientationProvider, comHeightProvider, pelvisPoseProvider, handPoseProvider,
                                                           handComplianceControlParametersProvider, desiredSteeringWheelProvider, handLoadBearingProvider, automaticManipulationAbortCommunicator, chestOrientationProvider, footPoseProvider,
                                                           footLoadBearingProvider, highLevelStateProvider, thighLoadBearingProvider, pelvisLoadBearingProvider, controlStatusProducer,
                                                           capturabilityBasedStatusProducer, handPoseStatusProducer, objectWeightProvider, desiredJointsPositionProvider,
                                                           singleJointPositionProvider, abortWalkingProvider, multiJointPositionProvider);

      return variousWalkingProviders;
   }
}
