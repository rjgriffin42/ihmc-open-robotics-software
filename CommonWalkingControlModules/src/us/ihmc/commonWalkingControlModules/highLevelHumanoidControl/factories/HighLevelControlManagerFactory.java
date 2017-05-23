package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.PelvisOffsetWhileWalkingParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.kneeAngle.KneeAngleManager;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlManager;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.JointAccelerationIntegrationSettings;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.BalanceManager;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.CenterOfMassHeightManager;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.YoPIDGains;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.transformables.Pose;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public class HighLevelControlManagerFactory
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final StatusMessageOutputManager statusOutputManager;

   private BalanceManager balanceManager;
   private CenterOfMassHeightManager centerOfMassHeightManager;
   private HeadOrientationManager headOrientationManager;
   private FeetManager feetManager;
   private PelvisOrientationManager pelvisOrientationManager;
   private KneeAngleManager kneeAngleManager;

   private final Map<String, RigidBodyControlManager> rigidBodyManagerMapByBodyName = new HashMap<>();

   private HighLevelHumanoidControllerToolbox controllerToolbox;
   private WalkingControllerParameters walkingControllerParameters;
   private CapturePointPlannerParameters capturePointPlannerParameters;
   private ICPOptimizationParameters icpOptimizationParameters;
   private MomentumOptimizationSettings momentumOptimizationSettings;

   public HighLevelControlManagerFactory(StatusMessageOutputManager statusOutputManager, YoVariableRegistry parentRegistry)
   {
      this.statusOutputManager = statusOutputManager;
      parentRegistry.addChild(registry);
   }

   public void setHighLevelHumanoidControllerToolbox(HighLevelHumanoidControllerToolbox controllerToolbox)
   {
      this.controllerToolbox = controllerToolbox;
   }

   public void setWalkingControllerParameters(WalkingControllerParameters walkingControllerParameters)
   {
      this.walkingControllerParameters = walkingControllerParameters;
      momentumOptimizationSettings = walkingControllerParameters.getMomentumOptimizationSettings();
   }

   public void setCapturePointPlannerParameters(CapturePointPlannerParameters capturePointPlannerParameters)
   {
      this.capturePointPlannerParameters = capturePointPlannerParameters;
   }

   public void setICPOptimizationParameters(ICPOptimizationParameters icpOptimizationParameters)
   {
      this.icpOptimizationParameters = icpOptimizationParameters;
   }

   public BalanceManager getOrCreateBalanceManager()
   {
      if (balanceManager != null)
         return balanceManager;

      if (!hasHighLevelHumanoidControllerToolbox(BalanceManager.class))
         return null;
      if (!hasWalkingControllerParameters(BalanceManager.class))
         return null;
      if (!hasCapturePointPlannerParameters(BalanceManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(BalanceManager.class))
         return null;

      balanceManager = new BalanceManager(controllerToolbox, walkingControllerParameters, capturePointPlannerParameters, icpOptimizationParameters, registry);
      Vector3D linearMomentumWeight = momentumOptimizationSettings.getLinearMomentumWeight();
      Vector3D angularMomentumWeight = momentumOptimizationSettings.getAngularMomentumWeight();
      balanceManager.setMomentumWeight(angularMomentumWeight, linearMomentumWeight);
      balanceManager.setHighMomentumWeightForRecovery(momentumOptimizationSettings.getHighLinearMomentumWeightForRecovery());
      return balanceManager;
   }

   public CenterOfMassHeightManager getOrCreateCenterOfMassHeightManager()
   {
      if (centerOfMassHeightManager != null)
         return centerOfMassHeightManager;

      if (!hasHighLevelHumanoidControllerToolbox(CenterOfMassHeightManager.class))
         return null;
      if (!hasWalkingControllerParameters(CenterOfMassHeightManager.class))
         return null;

      centerOfMassHeightManager = new CenterOfMassHeightManager(controllerToolbox, walkingControllerParameters, registry);
      centerOfMassHeightManager.setWeights(momentumOptimizationSettings.getPelvisLinearWeight());
      return centerOfMassHeightManager;
   }

   public RigidBodyControlManager getOrCreateRigidBodyManager(RigidBody bodyToControl, RigidBody baseBody, ReferenceFrame controlFrame,
         ReferenceFrame baseFrame, Collection<ReferenceFrame> trajectoryFrames)
   {
      if (bodyToControl == null)
         return null;

      String bodyName = bodyToControl.getName();
      if (rigidBodyManagerMapByBodyName.containsKey(bodyName))
      {
         RigidBodyControlManager manager = rigidBodyManagerMapByBodyName.get(bodyName);
         if (manager != null)
            return manager;
      }

      if (!hasWalkingControllerParameters(RigidBodyControlManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(RigidBodyControlManager.class))
         return null;

      // Gains
      Map<String, YoPIDGains> jointspaceGains = walkingControllerParameters.getOrCreateJointSpaceControlGains(registry);
      YoOrientationPIDGainsInterface taskspaceOrientationGains = walkingControllerParameters.getOrCreateTaskspaceOrientationControlGains(registry).get(bodyName);
      YoPositionPIDGainsInterface taskspacePositionGains = walkingControllerParameters.getOrCreateTaskspacePositionControlGains(registry).get(bodyName);

      // Weights
      TObjectDoubleHashMap<String> jointspaceWeights = momentumOptimizationSettings.getJointspaceWeights();
      TObjectDoubleHashMap<String> userModeWeights = momentumOptimizationSettings.getUserModeWeights();
      Vector3D taskspaceAngularWeight = momentumOptimizationSettings.getTaskspaceAngularWeights().get(bodyName);
      Vector3D taskspaceLinearWeight = momentumOptimizationSettings.getTaskspaceLinearWeights().get(bodyName);

      TObjectDoubleHashMap<String> homeConfiguration = walkingControllerParameters.getOrCreateJointHomeConfiguration();
      Pose homePose = walkingControllerParameters.getOrCreateBodyHomeConfiguration().get(bodyName);
      List<String> positionControlledJoints = walkingControllerParameters.getOrCreatePositionControlledJoints();
      Map<String, JointAccelerationIntegrationSettings> integrationSettings = walkingControllerParameters.getOrCreateIntegrationSettings();
      RigidBody elevator = controllerToolbox.getFullRobotModel().getElevator();
      DoubleYoVariable yoTime = controllerToolbox.getYoTime();

      ContactablePlaneBody contactableBody = controllerToolbox.getContactableBody(bodyToControl);
      YoGraphicsListRegistry graphicsListRegistry = controllerToolbox.getYoGraphicsListRegistry();
      RigidBodyControlMode defaultControlModeForRigidBody = walkingControllerParameters.getDefaultControlModeForRigidBody(bodyName);

      RigidBodyControlManager manager = new RigidBodyControlManager(bodyToControl, baseBody, elevator, homeConfiguration, homePose, positionControlledJoints,
            integrationSettings, trajectoryFrames, controlFrame, baseFrame, contactableBody, yoTime, graphicsListRegistry, registry);
      manager.setGains(jointspaceGains, taskspaceOrientationGains, taskspacePositionGains);
      manager.setWeights(jointspaceWeights, taskspaceAngularWeight, taskspaceLinearWeight, userModeWeights);
      manager.setDefaultControlMode(defaultControlModeForRigidBody);

      rigidBodyManagerMapByBodyName.put(bodyName, manager);
      return manager;
   }

   public FeetManager getOrCreateFeetManager()
   {
      if (feetManager != null)
         return feetManager;

      if (!hasHighLevelHumanoidControllerToolbox(FeetManager.class))
         return null;
      if (!hasWalkingControllerParameters(FeetManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(FeetManager.class))
         return null;

      feetManager = new FeetManager(controllerToolbox, walkingControllerParameters, registry);
      Vector3D highLinearFootWeight = momentumOptimizationSettings.getHighLinearFootWeight();
      Vector3D highAngularFootWeight = momentumOptimizationSettings.getHighAngularFootWeight();
      Vector3D defaultLinearFootWeight = momentumOptimizationSettings.getDefaultLinearFootWeight();
      Vector3D defaultAngularFootWeight = momentumOptimizationSettings.getDefaultAngularFootWeight();
      feetManager.setWeights(highAngularFootWeight, highLinearFootWeight, defaultAngularFootWeight, defaultLinearFootWeight);
      return feetManager;
   }

   public KneeAngleManager getOrCreateKneeAngleManager()
   {
      if (kneeAngleManager != null)
         return kneeAngleManager;

      if (!hasHighLevelHumanoidControllerToolbox(KneeAngleManager.class))
         return null;
      if (!hasWalkingControllerParameters(KneeAngleManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(KneeAngleManager.class))
         return null;

      kneeAngleManager = new KneeAngleManager(controllerToolbox, walkingControllerParameters.getStraightLegWalkingParameters(), registry);
      return kneeAngleManager;
   }

   public PelvisOrientationManager getOrCreatePelvisOrientationManager()
   {
      if (pelvisOrientationManager != null)
         return pelvisOrientationManager;

      if (!hasHighLevelHumanoidControllerToolbox(PelvisOrientationManager.class))
         return null;
      if (!hasWalkingControllerParameters(PelvisOrientationManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(PelvisOrientationManager.class))
         return null;

      YoOrientationPIDGainsInterface pelvisGains = walkingControllerParameters.createPelvisOrientationControlGains(registry);
      PelvisOffsetWhileWalkingParameters pelvisOffsetWhileWalkingParameters = walkingControllerParameters.getPelvisOffsetWhileWalkingParameters();
      Vector3D pelvisAngularWeight = momentumOptimizationSettings.getPelvisAngularWeight();

      pelvisOrientationManager = new PelvisOrientationManager(pelvisGains, pelvisOffsetWhileWalkingParameters, controllerToolbox, registry);
      pelvisOrientationManager.setWeights(pelvisAngularWeight);
      return pelvisOrientationManager;
   }

   private boolean hasHighLevelHumanoidControllerToolbox(Class<?> managerClass)
   {
      if (controllerToolbox != null)
         return true;
      missingObjectWarning(HighLevelHumanoidControllerToolbox.class, managerClass);
      return false;
   }

   private boolean hasWalkingControllerParameters(Class<?> managerClass)
   {
      if (walkingControllerParameters != null)
         return true;
      missingObjectWarning(WalkingControllerParameters.class, managerClass);
      return false;
   }

   private boolean hasCapturePointPlannerParameters(Class<?> managerClass)
   {
      if (capturePointPlannerParameters != null)
         return true;
      missingObjectWarning(CapturePointPlannerParameters.class, managerClass);
      return false;
   }

   private boolean hasMomentumOptimizationSettings(Class<?> managerClass)
   {
      if (momentumOptimizationSettings != null)
         return true;
      missingObjectWarning(MomentumOptimizationSettings.class, managerClass);
      return false;
   }

   private void missingObjectWarning(Class<?> missingObjectClass, Class<?> managerClass)
   {
      PrintTools.warn(this, missingObjectClass.getSimpleName() + " has not been set, cannot create: " + managerClass.getSimpleName());
   }

   public void initializeManagers()
   {
      if (balanceManager != null)
         balanceManager.initialize();
      if (centerOfMassHeightManager != null)
         centerOfMassHeightManager.initialize();
      if (headOrientationManager != null)
         headOrientationManager.initialize();
      if (pelvisOrientationManager != null)
         pelvisOrientationManager.initialize();

      Collection<RigidBodyControlManager> bodyManagers = rigidBodyManagerMapByBodyName.values();
      for (RigidBodyControlManager bodyManager : bodyManagers)
      {
         if (bodyManager != null)
            bodyManager.initialize();
      }
   }

   public FeedbackControlCommandList createFeedbackControlTemplate()
   {
      FeedbackControlCommandList ret = new FeedbackControlCommandList();

      if (feetManager != null)
      {
         FeedbackControlCommandList template = feetManager.createFeedbackControlTemplate();
         for (int i = 0; i < template.getNumberOfCommands(); i++)
            ret.addCommand(template.getCommand(i));
      }

      if (headOrientationManager != null)
      {
         ret.addCommand(headOrientationManager.createFeedbackControlTemplate());
      }

      Collection<RigidBodyControlManager> bodyManagers = rigidBodyManagerMapByBodyName.values();
      for (RigidBodyControlManager bodyManager : bodyManagers)
      {
         if (bodyManager != null)
            ret.addCommand(bodyManager.createFeedbackControlTemplate());
      }

      ret.addCommand(centerOfMassHeightManager.createFeedbackControlTemplate());
      
      if (pelvisOrientationManager != null)
      {
         ret.addCommand(pelvisOrientationManager.createFeedbackControlTemplate());
      }

      return ret;
   }

}
