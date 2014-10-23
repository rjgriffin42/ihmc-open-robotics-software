package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.HeadOrientationControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.ChestOrientationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.ChestOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationManager;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.ManipulationControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.packetConsumers.ChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisPoseProvider;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.DoubleProvider;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.controllers.YoOrientationPIDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;


public class VariousWalkingManagers
{
   private final HeadOrientationManager headOrientationManager;
   private final ChestOrientationManager chestOrientationManager;
   private final ManipulationControlModule manipulationControlModule;
   private final FeetManager feetManager;
   private final PelvisOrientationManager pelvisOrientationManager; // mid competition hack

   public VariousWalkingManagers(HeadOrientationManager headOrientationManager, ChestOrientationManager chestOrientationManager,
         ManipulationControlModule manipulationControlModule, FeetManager feetManager, PelvisOrientationManager pelvisOrientationManager)
   {
      this.headOrientationManager = headOrientationManager;
      this.chestOrientationManager = chestOrientationManager;
      this.manipulationControlModule = manipulationControlModule;
      this.feetManager = feetManager;
      this.pelvisOrientationManager = pelvisOrientationManager;
   }

   public static VariousWalkingManagers create(MomentumBasedController momentumBasedController, VariousWalkingProviders variousWalkingProviders,
         WalkingControllerParameters walkingControllerParameters, ArmControllerParameters armControlParameters, YoVariableRegistry registry,
         DoubleProvider swingTimeProvider)
   {
      FullRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();
      TwistCalculator twistCalculator = momentumBasedController.getTwistCalculator();
      YoGraphicsListRegistry yoGraphicsListRegistry = momentumBasedController.getDynamicGraphicObjectsListRegistry();
      double controlDT = momentumBasedController.getControlDT();

      HeadOrientationProvider desiredHeadOrientationProvider = null;
      HeadOrientationControlModule headOrientationControlModule = null;
      HeadOrientationManager headOrientationManager = null;

      if (fullRobotModel.getHead() != null)
      {
         desiredHeadOrientationProvider = variousWalkingProviders.getDesiredHeadOrientationProvider();

         headOrientationControlModule = setupHeadOrientationControlModule(momentumBasedController, desiredHeadOrientationProvider, walkingControllerParameters,
               yoGraphicsListRegistry, registry);

         headOrientationManager = new HeadOrientationManager(momentumBasedController, headOrientationControlModule, desiredHeadOrientationProvider,
               walkingControllerParameters.getTrajectoryTimeHeadOrientation(), registry);
      }

      ChestOrientationProvider desiredChestOrientationProvider = null;
      ChestOrientationControlModule chestOrientationControlModule = null;
      ChestOrientationManager chestOrientationManager = null;

      if (fullRobotModel.getChest() != null)
      {
         RigidBody chest = fullRobotModel.getChest();
         desiredChestOrientationProvider = variousWalkingProviders.getDesiredChestOrientationProvider();
         ReferenceFrame chestOrientationExpressedInFrame;
         if (desiredChestOrientationProvider != null)
            chestOrientationExpressedInFrame = desiredChestOrientationProvider.getChestOrientationExpressedInFrame();
         else
            chestOrientationExpressedInFrame = momentumBasedController.getReferenceFrames().getPelvisZUpFrame(); // ReferenceFrame.getWorldFrame(); //
         YoOrientationPIDGains chestControlGains = walkingControllerParameters.createChestControlGains(registry);

         chestOrientationControlModule = new ChestOrientationControlModule(chestOrientationExpressedInFrame, chest, twistCalculator, controlDT, chestControlGains, registry);

         chestOrientationManager = new ChestOrientationManager(momentumBasedController, chestOrientationControlModule,
               variousWalkingProviders.getDesiredChestOrientationProvider(), walkingControllerParameters.getTrajectoryTimeHeadOrientation(), registry);
      }

      ManipulationControlModule manipulationControlModule = null;

      if (fullRobotModel.getChest() != null && fullRobotModel.getHand(RobotSide.LEFT) != null && fullRobotModel.getHand(RobotSide.RIGHT) != null)
      {
         // Setup arm+hand manipulation state machines
         manipulationControlModule = new ManipulationControlModule(variousWalkingProviders, armControlParameters, momentumBasedController, registry);
      }

      FeetManager feetManager = new FeetManager(momentumBasedController, walkingControllerParameters, swingTimeProvider, registry);

      PelvisPoseProvider desiredPelvisPoseProvider = variousWalkingProviders.getDesiredPelvisPoseProvider();
      PelvisOrientationManager pelvisOrientationManager = new PelvisOrientationManager(walkingControllerParameters, swingTimeProvider, momentumBasedController, desiredPelvisPoseProvider, registry);
      

      VariousWalkingManagers variousWalkingManagers = new VariousWalkingManagers(headOrientationManager, chestOrientationManager, manipulationControlModule,
            feetManager, pelvisOrientationManager);

      return variousWalkingManagers;
   }

   private static HeadOrientationControlModule setupHeadOrientationControlModule(MomentumBasedController momentumBasedController,
         HeadOrientationProvider desiredHeadOrientationProvider, HeadOrientationControllerParameters headOrientationControllerParameters,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry registry)
   {
      CommonHumanoidReferenceFrames referenceFrames = momentumBasedController.getReferenceFrames();

      ReferenceFrame headOrientationExpressedInFrame;
      if (desiredHeadOrientationProvider != null)
         headOrientationExpressedInFrame = desiredHeadOrientationProvider.getHeadOrientationExpressedInFrame();
      else
         headOrientationExpressedInFrame = referenceFrames.getPelvisZUpFrame(); // ReferenceFrame.getWorldFrame(); //

      YoOrientationPIDGains gains = headOrientationControllerParameters.createHeadOrientationControlGains(registry);
      HeadOrientationControlModule headOrientationControlModule = new HeadOrientationControlModule(momentumBasedController, headOrientationExpressedInFrame,
            headOrientationControllerParameters, gains, registry, yoGraphicsListRegistry);

      // Setting initial head pitch
      FrameOrientation orientation = new FrameOrientation(headOrientationExpressedInFrame, headOrientationControllerParameters.getInitialHeadYawPitchRoll());
      headOrientationControlModule.setOrientationToTrack(new FrameOrientation(orientation));

      return headOrientationControlModule;
   }

   public void initializeManagers()
   {
      if (manipulationControlModule != null)
         manipulationControlModule.initialize();
   }

   public HeadOrientationManager getHeadOrientationManager()
   {
      return headOrientationManager;
   }

   public ChestOrientationManager getChestOrientationManager()
   {
      return chestOrientationManager;
   }

   public ManipulationControlModule getManipulationControlModule()
   {
      return manipulationControlModule;
   }

   public FeetManager getFeetManager()
   {
      return feetManager;
   }

   public PelvisOrientationManager getPelvisOrientationManager()
   {
      return pelvisOrientationManager;
   }
}
