package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import java.io.IOException;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingProviderFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingProviders;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredArmJointAngleProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootStateProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredThighLoadBearingProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ReinitializeWalkingControllerProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.UserDesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetProviders.ControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.SystemErrControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packets.DesiredHighLevelStateProvider;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantTransferTimeCalculator;
import us.ihmc.robotSide.SideDependentList;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParameters;

public class VariousWalkingProviderFromScriptFactory implements VariousWalkingProviderFactory
{
   private final String filename;
   private final ScriptFileLoader scriptFileLoader;
   
   public VariousWalkingProviderFromScriptFactory(String filename)
   {
      this.filename = filename;
      try
      {
         this.scriptFileLoader = new ScriptFileLoader(filename);
      } 
      catch (IOException e)
      {
         e.printStackTrace();
         throw new RuntimeException("Could not load script file " + filename);
      }
   }
   
   public VariousWalkingProviders createVariousWalkingProviders(final DoubleYoVariable time, FullRobotModel fullRobotModel, WalkingControllerParameters walkingControllerParameters,
         CommonWalkingReferenceFrames referenceFrames, SideDependentList<ContactablePlaneBody> feet, ConstantTransferTimeCalculator transferTimeCalculator,
         ConstantSwingTimeCalculator swingTimeCalculator, YoVariableRegistry registry)
   {
      ScriptBasedFootstepProvider footstepProvider;
     footstepProvider = new ScriptBasedFootstepProvider(scriptFileLoader, time, feet, fullRobotModel, walkingControllerParameters, registry);
      
      DesiredHandPoseProvider handPoseProvider = footstepProvider.getDesiredHandPoseProvider();

      LinkedHashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters = new LinkedHashMap<Footstep, TrajectoryParameters>();
      
      DesiredHighLevelStateProvider highLevelStateProvider = null; 
      DesiredHeadOrientationProvider headOrientationProvider = new UserDesiredHeadOrientationProvider(referenceFrames.getPelvisZUpFrame(), registry); 
      DesiredComHeightProvider desiredComHeightProvider = footstepProvider.getDesiredComHeightProvider();
      DesiredPelvisPoseProvider pelvisPoseProvider = null;
      DesiredChestOrientationProvider chestOrientationProvider = null; 
      DesiredFootPoseProvider footPoseProvider = null; 
      ReinitializeWalkingControllerProvider reinitializeWalkingControllerProvider = null; 

      DesiredHandLoadBearingProvider handLoadBearingProvider = null; 
      DesiredFootStateProvider footLoadBearingProvider = null; 
      DesiredThighLoadBearingProvider thighLoadBearingProvider = null; 
      DesiredPelvisLoadBearingProvider pelvisLoadBearingProvider = null; 
      
      DesiredArmJointAngleProvider armJointAngleProvider = null;
      
      ControlStatusProducer controlStatusProducer = new SystemErrControlStatusProducer();
      
      VariousWalkingProviders variousProvidersFactory = new VariousWalkingProviders(footstepProvider, mapFromFootstepsToTrajectoryParameters,
            headOrientationProvider, desiredComHeightProvider, pelvisPoseProvider, handPoseProvider, handLoadBearingProvider,
            chestOrientationProvider, footPoseProvider, footLoadBearingProvider, highLevelStateProvider, thighLoadBearingProvider,
            pelvisLoadBearingProvider, armJointAngleProvider, reinitializeWalkingControllerProvider, controlStatusProducer);

      return variousProvidersFactory;
   }

}
