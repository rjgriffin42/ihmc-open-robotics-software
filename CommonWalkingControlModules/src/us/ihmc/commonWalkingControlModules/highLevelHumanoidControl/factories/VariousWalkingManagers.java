package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.HeadOrientationControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.ChestOrientationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.ChestOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.PelvisDesiredsHandler;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationManager;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.ManipulationControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.DoubleProvider;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class VariousWalkingManagers
{
   private final HeadOrientationManager headOrientationManager;
   private final ChestOrientationManager chestOrientationManager;
   private final ManipulationControlModule manipulationControlModule;
   private final FeetManager feetManager;
   private final PelvisDesiredsHandler pelvisDesiredsHandler; // mid competition hack

   public VariousWalkingManagers(HeadOrientationManager headOrientationManager, ChestOrientationManager chestOrientationManager,
         ManipulationControlModule manipulationControlModule, FeetManager feetManager, PelvisDesiredsHandler pelvisDesiredsHandler)
   {
      this.headOrientationManager = headOrientationManager;
      this.chestOrientationManager = chestOrientationManager;
      this.manipulationControlModule = manipulationControlModule;
      this.feetManager = feetManager;
      this.pelvisDesiredsHandler = pelvisDesiredsHandler;
   }

   public static VariousWalkingManagers create(MomentumBasedController momentumBasedController, VariousWalkingProviders variousWalkingProviders,
         WalkingControllerParameters walkingControllerParameters, ArmControllerParameters armControlParameters,
         YoVariableRegistry registry, DoubleProvider swingTimeProvider)
   {
      FullRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();
      TwistCalculator twistCalculator = momentumBasedController.getTwistCalculator();
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = momentumBasedController.getDynamicGraphicObjectsListRegistry();
      double controlDT = momentumBasedController.getControlDT();

      DesiredHeadOrientationProvider desiredHeadOrientationProvider = null;
      HeadOrientationControlModule headOrientationControlModule = null;
      HeadOrientationManager headOrientationManager = null;

      if (fullRobotModel.getHead() != null)
      {
         desiredHeadOrientationProvider = variousWalkingProviders.getDesiredHeadOrientationProvider();

         headOrientationControlModule = setupHeadOrientationControlModule(momentumBasedController, desiredHeadOrientationProvider, walkingControllerParameters,
               dynamicGraphicObjectsListRegistry, registry);

         headOrientationManager = new HeadOrientationManager(momentumBasedController, headOrientationControlModule, desiredHeadOrientationProvider,
               walkingControllerParameters.getTrajectoryTimeHeadOrientation(), registry);
      }

      ChestOrientationControlModule chestOrientationControlModule = null;
      ChestOrientationManager chestOrientationManager = null;

      if (fullRobotModel.getChest() != null)
      {
         chestOrientationControlModule = setupChestOrientationControlModule(controlDT, fullRobotModel, twistCalculator, registry);
         chestOrientationManager = new ChestOrientationManager(momentumBasedController, chestOrientationControlModule, registry);
      }

      ManipulationControlModule manipulationControlModule = null;

      if (fullRobotModel.getChest() != null && fullRobotModel.getHand(RobotSide.LEFT) != null && fullRobotModel.getHand(RobotSide.RIGHT) != null)
      {
         // Setup arm+hand manipulation state machines
         manipulationControlModule = new ManipulationControlModule(variousWalkingProviders, armControlParameters, momentumBasedController, registry);
      }

      PelvisDesiredsHandler pelvisDesiredsHandler = new PelvisDesiredsHandler(controlDT, momentumBasedController.getYoTime(), registry);

      FeetManager feetManager = new FeetManager(momentumBasedController, walkingControllerParameters, swingTimeProvider, registry);

      VariousWalkingManagers variousWalkingManagers = new VariousWalkingManagers(headOrientationManager, chestOrientationManager, manipulationControlModule,
            feetManager, pelvisDesiredsHandler);

      return variousWalkingManagers;
   }

   private static ChestOrientationControlModule setupChestOrientationControlModule(double controlDT, FullRobotModel fullRobotModel,
         TwistCalculator twistCalculator, YoVariableRegistry registry)
   {
      RigidBody chest = fullRobotModel.getChest();
      RigidBody pelvis = fullRobotModel.getPelvis();

      ChestOrientationControlModule chestOrientationControlModule = new ChestOrientationControlModule(pelvis, chest, twistCalculator, controlDT, registry);

      return chestOrientationControlModule;
   }

   private static HeadOrientationControlModule setupHeadOrientationControlModule(MomentumBasedController momentumBasedController,
         DesiredHeadOrientationProvider desiredHeadOrientationProvider, HeadOrientationControllerParameters headOrientationControllerParameters,
         DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry registry)
   {
      CommonWalkingReferenceFrames referenceFrames = momentumBasedController.getReferenceFrames();

      ReferenceFrame headOrientationExpressedInFrame;
      if (desiredHeadOrientationProvider != null)
         headOrientationExpressedInFrame = desiredHeadOrientationProvider.getHeadOrientationExpressedInFrame();
      else
         headOrientationExpressedInFrame = referenceFrames.getPelvisZUpFrame(); // ReferenceFrame.getWorldFrame(); //

      HeadOrientationControlModule headOrientationControlModule = new HeadOrientationControlModule(momentumBasedController, headOrientationExpressedInFrame,
            headOrientationControllerParameters, registry, dynamicGraphicObjectsListRegistry);

      // Setting initial head pitch
      // This magic number (0.67) is a good default head pitch for getting good LIDAR point coverage of ground by feet
      // it would be in DRC config parameters, but the would require updating several nested constructors with an additional parameter
      FrameOrientation orientation = new FrameOrientation(headOrientationExpressedInFrame, 0.0, 0.67, 0.0);
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

   public PelvisDesiredsHandler getPelvisDesiredsHandler()
   {
      return pelvisDesiredsHandler;
   }
}
