package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlManager;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.ManipulationControlModule;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.BalanceManager;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.CenterOfMassHeightManager;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage.BaseForControl;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.YoPIDGains;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.tools.io.printing.PrintTools;

public class HighLevelControlManagerFactory
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final StatusMessageOutputManager statusOutputManager;

   private BalanceManager balanceManager;
   private CenterOfMassHeightManager centerOfMassHeightManager;
   private HeadOrientationManager headOrientationManager;
   private ManipulationControlModule manipulationControlModule;
   private FeetManager feetManager;
   private PelvisOrientationManager pelvisOrientationManager;

   private final Map<String, RigidBodyControlManager> rigidBodyManagerMapByBodyName = new HashMap<>();

   private HighLevelHumanoidControllerToolbox momentumBasedController;
   private WalkingControllerParameters walkingControllerParameters;
   private CapturePointPlannerParameters capturePointPlannerParameters;
   private ICPOptimizationParameters icpOptimizationParameters;
   private ArmControllerParameters armControllerParameters;
   private MomentumOptimizationSettings momentumOptimizationSettings;

   public HighLevelControlManagerFactory(StatusMessageOutputManager statusOutputManager, YoVariableRegistry parentRegistry)
   {
      this.statusOutputManager = statusOutputManager;
      parentRegistry.addChild(registry);
   }

   public void setMomentumBasedController(HighLevelHumanoidControllerToolbox momentumBasedController)
   {
      this.momentumBasedController = momentumBasedController;
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

   public void setArmControlParameters(ArmControllerParameters armControllerParameters)
   {
      this.armControllerParameters = armControllerParameters;
   }

   public BalanceManager getOrCreateBalanceManager()
   {
      if (balanceManager != null)
         return balanceManager;

      if (!hasMomentumBasedController(BalanceManager.class))
         return null;
      if (!hasWalkingControllerParameters(BalanceManager.class))
         return null;
      if (!hasCapturePointPlannerParameters(BalanceManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(BalanceManager.class))
         return null;

      balanceManager = new BalanceManager(momentumBasedController, walkingControllerParameters, capturePointPlannerParameters, icpOptimizationParameters, registry);
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

      if (!hasMomentumBasedController(CenterOfMassHeightManager.class))
         return null;
      if (!hasWalkingControllerParameters(CenterOfMassHeightManager.class))
         return null;

      centerOfMassHeightManager = new CenterOfMassHeightManager(momentumBasedController, walkingControllerParameters, registry);
      return centerOfMassHeightManager;
   }

   public HeadOrientationManager getOrCreatedHeadOrientationManager()
   {
      if (headOrientationManager != null)
         return headOrientationManager;

      FullRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();

      if (fullRobotModel.getHead() == null)
      {
         robotMissingBodyWarning("head", HeadOrientationManager.class);
         return null;
      }

      if (!hasWalkingControllerParameters(HeadOrientationManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(HeadOrientationManager.class))
         return null;

      headOrientationManager = new HeadOrientationManager(momentumBasedController, walkingControllerParameters, registry);
      double headJointspaceWeight = momentumOptimizationSettings.getHeadJointspaceWeight();
      double headTaskspaceWeight = momentumOptimizationSettings.getHeadTaskspaceWeight();
      double headUserModeWeight = momentumOptimizationSettings.getHeadUserModeWeight();
      headOrientationManager.setWeights(headJointspaceWeight, headTaskspaceWeight, headUserModeWeight);
      return headOrientationManager;
   }

   public RigidBodyControlManager getOrCreateRigidBodyManager(RigidBody bodyToControl, RigidBody rootBody, ReferenceFrame rootFrame)
   {
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

      CommonHumanoidReferenceFrames referenceFrames = momentumBasedController.getReferenceFrames();
      FullHumanoidRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();
      Map<BaseForControl, ReferenceFrame> controlFrameMap = new EnumMap<>(BaseForControl.class);
      controlFrameMap.put(BaseForControl.CHEST, fullRobotModel.getChest().getBodyFixedFrame());
      controlFrameMap.put(BaseForControl.WALKING_PATH, referenceFrames.getMidFeetUnderPelvisFrame());
      controlFrameMap.put(BaseForControl.WORLD, ReferenceFrame.getWorldFrame());

      RigidBodyControlManager manager = new RigidBodyControlManager(bodyToControl, rootBody, momentumBasedController, walkingControllerParameters, controlFrameMap, rootFrame, registry);

      Vector3D chestAngularWeight = momentumOptimizationSettings.getChestAngularWeight();
      Vector3D chestLinearWeight = null;

      YoOrientationPIDGainsInterface taskspaceOrientationGains = walkingControllerParameters.createChestControlGains(registry);
      YoPositionPIDGainsInterface taskspacePositionGains = null;

      TObjectDoubleHashMap<String> jointspaceWeights = momentumOptimizationSettings.getJointspaceWeights();
      Map<String, YoPIDGains> jointspaceGains = walkingControllerParameters.getOrCreateJointSpaceControlGains(registry);
      TObjectDoubleHashMap<String> userModeWeights = momentumOptimizationSettings.getUserModeWeights();

      manager.setWeights(jointspaceWeights, chestAngularWeight, chestLinearWeight, userModeWeights);
      manager.setGains(jointspaceGains, taskspaceOrientationGains, taskspacePositionGains);

      rigidBodyManagerMapByBodyName.put(bodyName, manager);
      return manager;
   }

   public ManipulationControlModule getOrCreateManipulationControlModule()
   {
      if (manipulationControlModule != null)
         return manipulationControlModule;

      FullHumanoidRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();

      if (fullRobotModel.getChest() == null)
      {
         robotMissingBodyWarning("chest", ManipulationControlModule.class);
         return null;
      }

      if (fullRobotModel.getHand(RobotSide.LEFT) == null)
      {
         robotMissingBodyWarning("left hand", ManipulationControlModule.class);
         return null;
      }

      if (fullRobotModel.getHand(RobotSide.RIGHT) == null)
      {
         robotMissingBodyWarning("right hand", ManipulationControlModule.class);
         return null;
      }

      if (!hasArmControllerParameters(ManipulationControlModule.class))
         return null;
      if (!hasMomentumBasedController(ManipulationControlModule.class))
         return null;
      if (!hasMomentumOptimizationSettings(ManipulationControlModule.class))
         return null;

      manipulationControlModule = new ManipulationControlModule(armControllerParameters, momentumBasedController, registry);
      double handJointspaceWeight = momentumOptimizationSettings.getHandJointspaceWeight();
      Vector3D handAngularTaskspaceWeight = momentumOptimizationSettings.getHandAngularTaskspaceWeight();
      Vector3D handLinearTaskspaceWeight = momentumOptimizationSettings.getHandLinearTaskspaceWeight();
      double handUserModeWeight = momentumOptimizationSettings.getHandUserModeWeight();
      manipulationControlModule.setWeights(handJointspaceWeight, handAngularTaskspaceWeight, handLinearTaskspaceWeight, handUserModeWeight);
      return manipulationControlModule;
   }

   public FeetManager getOrCreateFeetManager()
   {
      if (feetManager != null)
         return feetManager;

      if (!hasMomentumBasedController(FeetManager.class))
         return null;
      if (!hasWalkingControllerParameters(FeetManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(FeetManager.class))
         return null;

      feetManager = new FeetManager(momentumBasedController, walkingControllerParameters, registry);
      Vector3D highLinearFootWeight = momentumOptimizationSettings.getHighLinearFootWeight();
      Vector3D highAngularFootWeight = momentumOptimizationSettings.getHighAngularFootWeight();
      Vector3D defaultLinearFootWeight = momentumOptimizationSettings.getDefaultLinearFootWeight();
      Vector3D defaultAngularFootWeight = momentumOptimizationSettings.getDefaultAngularFootWeight();
      feetManager.setWeights(highAngularFootWeight, highLinearFootWeight, defaultAngularFootWeight, defaultLinearFootWeight);
      return feetManager;
   }

   public PelvisOrientationManager getOrCreatePelvisOrientationManager()
   {
      if (pelvisOrientationManager != null)
         return pelvisOrientationManager;

      if (!hasMomentumBasedController(PelvisOrientationManager.class))
         return null;
      if (!hasWalkingControllerParameters(PelvisOrientationManager.class))
         return null;
      if (!hasMomentumOptimizationSettings(PelvisOrientationManager.class))
         return null;

      pelvisOrientationManager = new PelvisOrientationManager(walkingControllerParameters, momentumBasedController, registry);
      pelvisOrientationManager.setWeights(momentumOptimizationSettings.getPelvisAngularWeight());
      return pelvisOrientationManager;
   }

   private boolean hasMomentumBasedController(Class<?> managerClass)
   {
      if (momentumBasedController != null)
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

   private boolean hasArmControllerParameters(Class<?> managerClass)
   {
      if (armControllerParameters != null)
         return true;
      missingObjectWarning(ArmControllerParameters.class, managerClass);
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

   private void robotMissingBodyWarning(String missingBodyName, Class<?> managerClass)
   {
      PrintTools.warn(this, "The robot is missing the body: " + missingBodyName + ", cannot create: " + managerClass.getSimpleName());
   }

   public void initializeManagers()
   {
      if (balanceManager != null)
         balanceManager.initialize();
      if (centerOfMassHeightManager != null)
         centerOfMassHeightManager.initialize();
      if (manipulationControlModule != null)
         manipulationControlModule.initialize();
      if (headOrientationManager != null)
         headOrientationManager.initialize();

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

      if (manipulationControlModule != null)
      {
         FeedbackControlCommandList template = manipulationControlModule.createFeedbackControlTemplate();
         for (int i = 0; i < template.getNumberOfCommands(); i++)
            ret.addCommand(template.getCommand(i));
      }

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

      if (pelvisOrientationManager != null)
      {
         ret.addCommand(pelvisOrientationManager.getFeedbackControlCommand());
      }

      return ret;
   }
}
