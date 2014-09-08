package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.ArrayList;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.RobotControllerUpdatablesAdapter;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.HighLevelHumanoidControllerManager;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.DoNothingBehavior;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.HighLevelBehavior;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.ICPAndMomentumBasedController;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.WalkingHighLevelHumanoidController;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ICPBasedLinearMomentumRateOfChangeControlModule;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.InstantaneousCapturePointPlanner;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.InstantaneousCapturePointPlannerWithTimeFreezer;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.SmoothICPComputer2D;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.OldMomentumControlModule;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantTransferTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.LookAheadCoMHeightTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.SwingTimeCalculationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.TransferTimeCalculationProvider;
import us.ihmc.communication.packets.dataobjects.HighLevelState;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.sensors.ForceSensorData;
import us.ihmc.sensorProcessing.sensors.ForceSensorDataHolder;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.io.streamingData.GlobalDataProducer;
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.TotalMassCalculator;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class MomentumBasedControllerFactory
{   
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final WalkingControllerParameters walkingControllerParameters;
   private final ArmControllerParameters armControllerParameters;

   private final double swingTime;
   private final double transferTime;

   private final ConstantSwingTimeCalculator swingTimeCalculator;
   private final ConstantTransferTimeCalculator transferTimeCalculator;
   private final HighLevelState initialBehavior;

   private MomentumBasedController momentumBasedController = null;
   private ICPAndMomentumBasedController icpAndMomentumBasedController = null;
   
   private HighLevelHumanoidControllerManager highLevelHumanoidControllerManager = null;
   private final ArrayList<HighLevelBehavior> highLevelBehaviors = new ArrayList<>();
   
   private VariousWalkingProviderFactory variousWalkingProviderFactory;
   private VariousWalkingProviders variousWalkingProviders;
   private VariousWalkingManagers variousWalkingManagers;

   private ArrayList<HighLevelBehaviorFactory> highLevelBehaviorFactories = new ArrayList<>();

   private final SideDependentList<String> footSensorNames;
   private final ContactableBodiesFactory contactableBodiesFactory;

   private final ArrayList<Updatable> updatables = new ArrayList<Updatable>();

   public MomentumBasedControllerFactory(ContactableBodiesFactory contactableBodiesFactory, SideDependentList<String> footSensorNames,
         WalkingControllerParameters walkingControllerParameters, ArmControllerParameters armControllerParameters, HighLevelState initialBehavior)
   {
      this.footSensorNames = footSensorNames;
      this.contactableBodiesFactory = contactableBodiesFactory;
      this.initialBehavior = initialBehavior;

      this.walkingControllerParameters = walkingControllerParameters;
      this.armControllerParameters = armControllerParameters;
      
      this.transferTime = walkingControllerParameters.getDefaultTransferTime();
      this.swingTime = walkingControllerParameters.getDefaultSwingTime();

      this.swingTimeCalculator = new ConstantSwingTimeCalculator(swingTime, registry);    // new PiecewiseLinearStepTimeCalculator(stepTime, 0.7, 0.6);
      this.transferTimeCalculator = new ConstantTransferTimeCalculator(transferTime, registry);
   }

   public void setVariousWalkingProviderFactory(VariousWalkingProviderFactory variousWalkingProviderFactory)
   {
      this.variousWalkingProviderFactory = variousWalkingProviderFactory;
   }
   
   public void addUpdatable(Updatable updatable)
   {
      this.updatables.add(updatable);
   }

   public RobotController getController(FullRobotModel fullRobotModel, CommonWalkingReferenceFrames referenceFrames, double controlDT, double gravity,
         DoubleYoVariable yoTime, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, TwistCalculator twistCalculator,
         CenterOfMassJacobian centerOfMassJacobian, ForceSensorDataHolder forceSensorDataHolder, GlobalDataProducer dataProducer,
         InverseDynamicsJoint... jointsToIgnore)
   {      
      SideDependentList<ContactablePlaneBody> feet = contactableBodiesFactory.createFootContactableBodies(fullRobotModel, referenceFrames);

      double gravityZ = Math.abs(gravity);
      double totalMass = TotalMassCalculator.computeSubTreeMass(fullRobotModel.getElevator());
      double totalRobotWeight = totalMass * gravityZ;

      SideDependentList<FootSwitchInterface> footSwitches = createFootSwitches(feet, forceSensorDataHolder, totalRobotWeight,
            dynamicGraphicObjectsListRegistry, registry);


      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the different ContactablePlaneBodies ///////////////////////////////////////////////
      
      RigidBody rootBody = fullRobotModel.getRootJoint().getSuccessor();
      SideDependentList<ContactablePlaneBody> thighs = contactableBodiesFactory.createThighContactableBodies(rootBody);
      ContactablePlaneBody pelvisContactablePlaneBody = contactableBodiesFactory.createPelvisContactableBody(fullRobotModel.getPelvis());
      ContactablePlaneBody pelvisBackContactablePlaneBody = contactableBodiesFactory.createPelvisBackContactableBody(fullRobotModel.getPelvis());
      SideDependentList<ContactablePlaneBody> handContactableBodies = contactableBodiesFactory.createHandContactableBodies(rootBody);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the ICPBasedLinearMomentumRateOfChangeControlModule ////////////////////////////////
      ICPBasedLinearMomentumRateOfChangeControlModule iCPBasedLinearMomentumRateOfChangeControlModule =
         new ICPBasedLinearMomentumRateOfChangeControlModule(referenceFrames.getCenterOfMassFrame(), controlDT, totalMass, gravityZ, registry,
            dynamicGraphicObjectsListRegistry);

      iCPBasedLinearMomentumRateOfChangeControlModule.setGains(walkingControllerParameters.getCaptureKpParallelToMotion(),
              walkingControllerParameters.getCaptureKpOrthogonalToMotion(), walkingControllerParameters.getCaptureKi(),
              walkingControllerParameters.getCaptureKiBleedoff(),
              walkingControllerParameters.getCaptureFilterBreakFrequencyInHz(), walkingControllerParameters.getCMPRateLimit(),
              walkingControllerParameters.getCMPAccelerationLimit());

      // No longer need old one. Don't create it.
      // TODO: Remove OldMomentumControlModule completely once QP stuff is solidified.
      OldMomentumControlModule oldMomentumControlModule = null;

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup different things relative to GUI communication and WalkingProviders ////////////////
      
      if (variousWalkingProviderFactory == null)
      {
         variousWalkingProviderFactory = new DoNothingVariousWalkingProviderFactory(controlDT);
      }
      
      variousWalkingProviders = variousWalkingProviderFactory.createVariousWalkingProviders(yoTime, fullRobotModel, walkingControllerParameters,
            referenceFrames, feet, transferTimeCalculator, swingTimeCalculator, updatables, registry, dynamicGraphicObjectsListRegistry);
      if (variousWalkingProviders == null)
         throw new RuntimeException("Couldn't create various walking providers!");

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup different things for walking ///////////////////////////////////////////////////////
      double doubleSupportPercentageIn = 0.3;    // NOTE: used to be 0.35, jojo
      double minimumHeightAboveGround = walkingControllerParameters.minimumHeightAboveAnkle();
      double nominalHeightAboveGround = walkingControllerParameters.nominalHeightAboveAnkle();
      double maximumHeightAboveGround = walkingControllerParameters.maximumHeightAboveAnkle();

      DesiredComHeightProvider desiredComHeightProvider = variousWalkingProviders.getDesiredComHeightProvider();

      LookAheadCoMHeightTrajectoryGenerator centerOfMassHeightTrajectoryGenerator = new LookAheadCoMHeightTrajectoryGenerator(desiredComHeightProvider,
                                                                                       minimumHeightAboveGround, nominalHeightAboveGround,
                                                                                       maximumHeightAboveGround, doubleSupportPercentageIn, yoTime,
                                                                                       dynamicGraphicObjectsListRegistry, registry);
      
      double icpInFromCenter = 0.006; //0.01;
      double doubleSupportFirstStepFraction = 0.5;
      int maxNumberOfConsideredFootsteps = 4;

      SmoothICPComputer2D smoothICPComputer2D = new SmoothICPComputer2D(referenceFrames, controlDT, doubleSupportFirstStepFraction,
                                                   maxNumberOfConsideredFootsteps, registry, dynamicGraphicObjectsListRegistry);
      smoothICPComputer2D.setICPInFromCenter(icpInFromCenter);


      InstantaneousCapturePointPlanner instantaneousCapturePointPlanner = new InstantaneousCapturePointPlannerWithTimeFreezer(smoothICPComputer2D, registry);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the MomentumBasedController ////////////////////////////////////////////////////////
      momentumBasedController = new MomentumBasedController(fullRobotModel, centerOfMassJacobian, referenceFrames, footSwitches,
                                   yoTime, gravityZ, twistCalculator, feet, handContactableBodies, thighs, pelvisContactablePlaneBody,
                                   pelvisBackContactablePlaneBody, controlDT, oldMomentumControlModule,
                                   updatables, dynamicGraphicObjectsListRegistry, jointsToIgnore);

      TransferTimeCalculationProvider transferTimeCalculationProvider = new TransferTimeCalculationProvider("providedTransferTime", registry, transferTimeCalculator, transferTime);
      SwingTimeCalculationProvider swingTimeCalculationProvider = new SwingTimeCalculationProvider("providedSwingTime", registry, swingTimeCalculator, swingTime);
      
      variousWalkingManagers = VariousWalkingManagers.create(momentumBasedController, variousWalkingProviders, walkingControllerParameters, armControllerParameters,
            registry, swingTimeCalculationProvider);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the ICPAndMomentumBasedController //////////////////////////////////////////////////
      BipedSupportPolygons bipedSupportPolygons = new BipedSupportPolygons(referenceFrames.getAnkleZUpReferenceFrames(), referenceFrames.getMidFeetZUpFrame(),
                                                     registry, dynamicGraphicObjectsListRegistry, false);


      icpAndMomentumBasedController = new ICPAndMomentumBasedController(momentumBasedController, fullRobotModel, feet, bipedSupportPolygons, registry);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the WalkingHighLevelHumanoidController /////////////////////////////////////////////

      WalkingHighLevelHumanoidController walkingBehavior = new WalkingHighLevelHumanoidController(variousWalkingProviders, variousWalkingManagers,
            centerOfMassHeightTrajectoryGenerator, transferTimeCalculationProvider, swingTimeCalculationProvider, walkingControllerParameters, iCPBasedLinearMomentumRateOfChangeControlModule,
            instantaneousCapturePointPlanner, icpAndMomentumBasedController, momentumBasedController);
      highLevelBehaviors.add(walkingBehavior);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the DoNothingController ////////////////////////////////////////////////////////////
      // Useful as a transition state on the real robot
      DoNothingBehavior doNothingBehavior = new DoNothingBehavior(momentumBasedController, bipedSupportPolygons);
      highLevelBehaviors.add(doNothingBehavior);
      
      /////////////////////////////////////////////////////////////////////////////////////////////
      // Setup the HighLevelHumanoidControllerManager /////////////////////////////////////////////
      // This is the "highest level" controller that enables switching between the different controllers (walking, multi-contact, driving, etc.)
      highLevelHumanoidControllerManager = new HighLevelHumanoidControllerManager(initialBehavior, highLevelBehaviors, momentumBasedController, variousWalkingProviders);
      highLevelHumanoidControllerManager.addYoVariableRegistry(this.registry);

      createRegisteredControllers();

      RobotController ret = highLevelHumanoidControllerManager;
      
      ret.getYoVariableRegistry().addChild(registry);

      if (dynamicGraphicObjectsListRegistry != null)
      {
         RobotControllerUpdatablesAdapter highLevelHumanoidControllerUpdatables = new RobotControllerUpdatablesAdapter(ret);

         ret = highLevelHumanoidControllerUpdatables;
      }
      return ret;
   }

   private SideDependentList<FootSwitchInterface> createFootSwitches(SideDependentList<ContactablePlaneBody> bipedFeet,
         ForceSensorDataHolder forceSensorDataHolder, double totalRobotWeight, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
         YoVariableRegistry registry)
   {
      SideDependentList<FootSwitchInterface> footSwitches = new SideDependentList<FootSwitchInterface>();

      for (RobotSide robotSide : RobotSide.values)
      {
         ForceSensorData footForceSensor = forceSensorDataHolder.getByName(footSensorNames.get(robotSide));
         double contactThresholdForce = walkingControllerParameters.getContactThresholdForce();
         WrenchBasedFootSwitch wrenchBasedFootSwitch = new WrenchBasedFootSwitch(bipedFeet.get(robotSide).getName(), footForceSensor, 0.02, totalRobotWeight,
               bipedFeet.get(robotSide), dynamicGraphicObjectsListRegistry, contactThresholdForce, registry);
         footSwitches.put(robotSide, wrenchBasedFootSwitch);
      }

      return footSwitches;
   }

   private void createRegisteredControllers()
   {
      for (int i = 0; i < highLevelBehaviorFactories.size(); i++)
      {
         HighLevelBehaviorFactory highLevelBehaviorFactory = highLevelBehaviorFactories.get(i);
         HighLevelBehavior highLevelBehavior = highLevelBehaviorFactory.createHighLevelBehavior(variousWalkingProviders, variousWalkingManagers, momentumBasedController, icpAndMomentumBasedController);
         boolean transitionRequested = highLevelBehaviorFactory.isTransitionToBehaviorRequested();
         highLevelHumanoidControllerManager.addHighLevelBehavior(highLevelBehavior, transitionRequested);
      }
   }

   public void reinitializeWalking()
   {
      highLevelHumanoidControllerManager.requestHighLevelState(HighLevelState.WALKING);
   }

   public void addHighLevelBehaviorFactory(HighLevelBehaviorFactory highLevelBehaviorFactory)
   {
      if (momentumBasedController == null)
      {
         highLevelBehaviorFactories.add(highLevelBehaviorFactory);
      }
      else
      {
         HighLevelBehavior highLevelBehavior = highLevelBehaviorFactory.createHighLevelBehavior(variousWalkingProviders, variousWalkingManagers, momentumBasedController, icpAndMomentumBasedController);
         boolean transitionToBehaviorRequested = highLevelBehaviorFactory.isTransitionToBehaviorRequested();
         highLevelHumanoidControllerManager.addHighLevelBehavior(highLevelBehavior, transitionToBehaviorRequested);
      }
   }
}
