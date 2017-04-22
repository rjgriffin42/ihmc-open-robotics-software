package us.ihmc.commonWalkingControlModules.controlModules.foot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.toeOffCalculator.ToeOffCalculator;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesData;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootTrajectoryCommand;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.trajectories.providers.YoVelocityProvider;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.sensors.FootSwitchInterface;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.GenericStateMachine;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateMachineTools;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateTransition;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateTransitionCondition;

public class FootControlModule
{
   private final YoVariableRegistry registry;
   private final ContactablePlaneBody contactableFoot;

   private final boolean useNewSupportState;

   public enum ConstraintType
   {
      FULL, HOLD_POSITION, TOES, SWING, MOVE_VIA_WAYPOINTS, EXPLORE_POLYGON
   }

   private static final double coefficientOfFriction = 0.8;

   private final GenericStateMachine<ConstraintType, AbstractFootControlState> stateMachine;
   private final EnumYoVariable<ConstraintType> requestedState;
   private final EnumMap<ConstraintType, boolean[]> contactStatesMap = new EnumMap<ConstraintType, boolean[]>(ConstraintType.class);

   private final HighLevelHumanoidControllerToolbox controllerToolbox;
   private final RobotSide robotSide;

   private final LegSingularityAndKneeCollapseAvoidanceControlModule legSingularityAndKneeCollapseAvoidanceControlModule;

   private final BooleanYoVariable holdPositionIfCopOnEdge;
   /** For testing purpose only. */
   private final BooleanYoVariable alwaysHoldPosition;
   private final BooleanYoVariable neverHoldPosition;

   private final HoldPositionState holdPositionState;
   private final SwingState swingState;
   private final MoveViaWaypointsState moveViaWaypointsState;
   private final OnToesState onToesState;
   private final FullyConstrainedState supportState;
   private final ExploreFootPolygonState exploreFootPolygonState;
   private final SupportState supportStateNew;

   private final FootSwitchInterface footSwitch;
   private final DoubleYoVariable footLoadThresholdToHoldPosition;

   private final FootControlHelper footControlHelper;
   private final ToeOffCalculator toeOffCalculator;

   private final BooleanYoVariable requestExploration;
   private final BooleanYoVariable resetFootPolygon;

   public FootControlModule(RobotSide robotSide, ToeOffCalculator toeOffCalculator, WalkingControllerParameters walkingControllerParameters,
         YoSE3PIDGainsInterface swingFootControlGains, YoSE3PIDGainsInterface holdPositionFootControlGains, YoSE3PIDGainsInterface toeOffFootControlGains,
         YoSE3PIDGainsInterface edgeTouchdownFootControlGains, HighLevelHumanoidControllerToolbox controllerToolbox, YoVariableRegistry parentRegistry)
   {
      this.toeOffCalculator = toeOffCalculator;

      contactableFoot = controllerToolbox.getContactableFeet().get(robotSide);
      controllerToolbox.setFootContactCoefficientOfFriction(robotSide, coefficientOfFriction);
      controllerToolbox.setFootContactStateFullyConstrained(robotSide);

      String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
      String namePrefix = sidePrefix + "Foot";
      registry = new YoVariableRegistry(sidePrefix + getClass().getSimpleName());
      parentRegistry.addChild(registry);
      footControlHelper = new FootControlHelper(robotSide, walkingControllerParameters, controllerToolbox, registry);

      this.controllerToolbox = controllerToolbox;
      this.robotSide = robotSide;

      footSwitch = controllerToolbox.getFootSwitches().get(robotSide);
      footLoadThresholdToHoldPosition = new DoubleYoVariable("footLoadThresholdToHoldPosition", registry);
      footLoadThresholdToHoldPosition.set(0.2);

      holdPositionIfCopOnEdge = new BooleanYoVariable(namePrefix + "HoldPositionIfCopOnEdge", registry);
      holdPositionIfCopOnEdge.set(walkingControllerParameters.doFancyOnToesControl());
      alwaysHoldPosition = new BooleanYoVariable(namePrefix + "AlwaysHoldPosition", registry);
      neverHoldPosition = new BooleanYoVariable(namePrefix + "NeverHoldPosition", registry);

      legSingularityAndKneeCollapseAvoidanceControlModule = footControlHelper.getLegSingularityAndKneeCollapseAvoidanceControlModule();

      // set up states and state machine
      DoubleYoVariable time = controllerToolbox.getYoTime();
      stateMachine = new GenericStateMachine<>(namePrefix + "State", namePrefix + "SwitchTime", ConstraintType.class, time, registry);
      requestedState = EnumYoVariable.create(namePrefix + "RequestedState", "", ConstraintType.class, registry, true);
      requestedState.set(null);

      setupContactStatesMap();

      YoVelocityProvider touchdownVelocityProvider = new YoVelocityProvider(namePrefix + "TouchdownVelocity", ReferenceFrame.getWorldFrame(), registry);
      touchdownVelocityProvider.set(new Vector3D(0.0, 0.0, walkingControllerParameters.getDesiredTouchdownVelocity()));

      YoVelocityProvider touchdownAccelerationProvider = new YoVelocityProvider(namePrefix + "TouchdownAcceleration", ReferenceFrame.getWorldFrame(), registry);
      touchdownAccelerationProvider.set(new Vector3D(0.0, 0.0, walkingControllerParameters.getDesiredTouchdownAcceleration()));

      List<AbstractFootControlState> states = new ArrayList<AbstractFootControlState>();

      onToesState = new OnToesState(footControlHelper, toeOffCalculator, toeOffFootControlGains, registry);
      states.add(onToesState);

      supportStateNew = new SupportState(footControlHelper, holdPositionFootControlGains, registry);
      useNewSupportState = walkingControllerParameters.useSupportState();

      if (useNewSupportState)
      {
         states.add(supportStateNew);

         supportState = null;
         holdPositionState = null;
         exploreFootPolygonState = null;
      }
      else
      {
         supportState = new FullyConstrainedState(footControlHelper, registry);
         holdPositionState = new HoldPositionState(footControlHelper, holdPositionFootControlGains, registry);
         states.add(supportState);
         states.add(holdPositionState);

         if (walkingControllerParameters.getOrCreateExplorationParameters(registry) != null)
         {
            exploreFootPolygonState = new ExploreFootPolygonState(footControlHelper, holdPositionFootControlGains, registry);
            states.add(exploreFootPolygonState);
         }
         else
            exploreFootPolygonState = null;
      }

      swingState = new SwingState(footControlHelper, touchdownVelocityProvider, touchdownAccelerationProvider, swingFootControlGains, registry);
      states.add(swingState);

      moveViaWaypointsState = new MoveViaWaypointsState(footControlHelper, touchdownVelocityProvider, touchdownAccelerationProvider, swingFootControlGains, registry);
      states.add(moveViaWaypointsState);

      setupStateMachine(states);

      requestExploration = new BooleanYoVariable(namePrefix + "RequestExploration", registry);
      resetFootPolygon = new BooleanYoVariable(namePrefix + "ResetFootPolygon", registry);
   }

   private void setupContactStatesMap()
   {
      boolean[] falses = new boolean[contactableFoot.getTotalNumberOfContactPoints()];
      Arrays.fill(falses, false);
      boolean[] trues = new boolean[contactableFoot.getTotalNumberOfContactPoints()];
      Arrays.fill(trues, true);

      contactStatesMap.put(ConstraintType.SWING, falses);
      contactStatesMap.put(ConstraintType.MOVE_VIA_WAYPOINTS, falses);
      contactStatesMap.put(ConstraintType.FULL, trues);
      contactStatesMap.put(ConstraintType.EXPLORE_POLYGON, trues);
      contactStatesMap.put(ConstraintType.HOLD_POSITION, trues);
      contactStatesMap.put(ConstraintType.TOES, getOnEdgeContactPointStates(contactableFoot, ConstraintType.TOES));
   }

   private void setupStateMachine(List<AbstractFootControlState> states)
   {
      // TODO Clean that up (Sylvain)
      for (AbstractFootControlState fromState : states)
      {
         for (AbstractFootControlState toState : states)
         {
            StateMachineTools.addRequestedStateTransition(requestedState, false, fromState, toState);
         }
      }

      if (!useNewSupportState)
      {
         supportState.addStateTransition(new StateTransition<FootControlModule.ConstraintType>(ConstraintType.HOLD_POSITION, new StateTransitionCondition()
         {
            @Override
            public boolean checkCondition()
            {
               if (alwaysHoldPosition.getBooleanValue())
                  return true;
               if (neverHoldPosition.getBooleanValue())
                  return false;

               if (isFootBarelyLoaded())
                  return true;
               if (holdPositionIfCopOnEdge.getBooleanValue())
                  return footControlHelper.isCoPOnEdge();
               return false;
            }
         }));

         holdPositionState.addStateTransition(new StateTransition<FootControlModule.ConstraintType>(ConstraintType.FULL, new StateTransitionCondition()
         {
            @Override
            public boolean checkCondition()
            {
               if (useNewSupportState)
                  return true;

               if (alwaysHoldPosition.getBooleanValue())
                  return false;
               if (neverHoldPosition.getBooleanValue())
                  return true;

               if (isFootBarelyLoaded())
                  return false;
               return !footControlHelper.isCoPOnEdge();
            }
         }));

         if (exploreFootPolygonState != null)
         {
            exploreFootPolygonState.addStateTransition(new StateTransition<FootControlModule.ConstraintType>(ConstraintType.FULL, new StateTransitionCondition()
            {
               @Override
               public boolean checkCondition()
               {
                  return exploreFootPolygonState.isDoneExploring();
               }
            }));
         }
      }

      for (AbstractFootControlState state : states)
      {
         stateMachine.addState(state);
      }
      stateMachine.setCurrentState(ConstraintType.FULL);
   }

   public void setWeights(double highFootWeight, double defaultFootWeight)
   {
      swingState.setWeight(defaultFootWeight);
      moveViaWaypointsState.setWeight(defaultFootWeight);
      onToesState.setWeight(highFootWeight);
      if (supportState != null)
         supportState.setWeight(highFootWeight);
      if (supportStateNew != null)
         supportStateNew.setWeight(highFootWeight);
      if (holdPositionState != null)
         holdPositionState.setWeight(defaultFootWeight);
      if (exploreFootPolygonState != null)
         exploreFootPolygonState.setWeight(defaultFootWeight);
   }

   public void setWeights(Vector3D highAngularFootWeight, Vector3D highLinearFootWeight, Vector3D defaultAngularFootWeight, Vector3D defaultLinearFootWeight)
   {
      swingState.setWeights(defaultAngularFootWeight, defaultLinearFootWeight);
      moveViaWaypointsState.setWeights(defaultAngularFootWeight, defaultLinearFootWeight);
      onToesState.setWeights(highAngularFootWeight, highLinearFootWeight);
      if (supportState != null)
         supportState.setWeights(highAngularFootWeight, highLinearFootWeight);
      if (supportStateNew != null)
         supportStateNew.setWeights(highAngularFootWeight, highLinearFootWeight);
      if (holdPositionState != null)
         holdPositionState.setWeights(highAngularFootWeight, highLinearFootWeight);
      if (exploreFootPolygonState != null)
         exploreFootPolygonState.setWeights(defaultAngularFootWeight, defaultLinearFootWeight);
   }

   public void replanTrajectory(Footstep footstep, double swingTime, boolean continuousReplan)
   {
      swingState.replanTrajectory(footstep, swingTime, continuousReplan);
   }

   public void requestTouchdownForDisturbanceRecovery()
   {
      if (stateMachine.getCurrentState() == moveViaWaypointsState)
         moveViaWaypointsState.requestTouchdownForDisturbanceRecovery();
   }

   public void requestStopTrajectoryIfPossible()
   {
      if (stateMachine.getCurrentState() == moveViaWaypointsState)
         moveViaWaypointsState.requestStopTrajectory();
   }

   public void setContactState(ConstraintType constraintType)
   {
      setContactState(constraintType, null);
   }

   public void setContactState(ConstraintType constraintType, FrameVector normalContactVector)
   {
      if (constraintType == ConstraintType.HOLD_POSITION || constraintType == ConstraintType.FULL)
      {
         if (constraintType == ConstraintType.HOLD_POSITION)
            System.out.println("Warning: HOLD_POSITION state is handled internally.");

         if (isFootBarelyLoaded() && !useNewSupportState)
            constraintType = ConstraintType.HOLD_POSITION;
         else
            constraintType = ConstraintType.FULL;

         footControlHelper.setFullyConstrainedNormalContactVector(normalContactVector);
      }

      controllerToolbox.setFootContactState(robotSide, contactStatesMap.get(constraintType), normalContactVector);

      if (getCurrentConstraintType() == constraintType) // Use resetCurrentState() for such case
         return;

      requestedState.set(constraintType);
   }

   private boolean isFootBarelyLoaded()
   {
      return footSwitch.computeFootLoadPercentage() < footLoadThresholdToHoldPosition.getDoubleValue();
   }

   public ConstraintType getCurrentConstraintType()
   {
      return stateMachine.getCurrentStateEnum();
   }

   public void initialize()
   {
      stateMachine.setCurrentState(ConstraintType.FULL);
   }

   public void doControl()
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.resetSwingParameters();
      footControlHelper.update();

      if (resetFootPolygon.getBooleanValue())
      {
         resetFootPolygon();
      }

      if (requestExploration.getBooleanValue())
      {
         requestExploration();
      }

      stateMachine.checkTransitionConditions();

      if (!isInFlatSupportState() && footControlHelper.getPartialFootholdControlModule() != null)
         footControlHelper.getPartialFootholdControlModule().reset();

      stateMachine.doAction();
   }

   // Used to restart the current state reseting the current state time
   public void resetCurrentState()
   {
      stateMachine.setCurrentState(getCurrentConstraintType());
   }

   public boolean isInFlatSupportState()
   {
      ConstraintType currentConstraintType = getCurrentConstraintType();
      return currentConstraintType == ConstraintType.FULL || currentConstraintType == ConstraintType.EXPLORE_POLYGON || currentConstraintType == ConstraintType.HOLD_POSITION;
   }

   public boolean isInToeOff()
   {
      return getCurrentConstraintType() == ConstraintType.TOES;
   }

   public void setUsePointContactInToeOff(boolean usePointContact)
   {
      onToesState.setUsePointContact(usePointContact);
   }

   private boolean[] getOnEdgeContactPointStates(ContactablePlaneBody contactableBody, ConstraintType constraintType)
   {
      FrameVector direction = new FrameVector(contactableBody.getFrameAfterParentJoint(), 1.0, 0.0, 0.0);

      int[] indexOfPointsInContact = DesiredFootstepCalculatorTools.findMaximumPointIndexesInDirection(contactableBody.getContactPointsCopy(), direction, 2);

      boolean[] contactPointStates = new boolean[contactableBody.getTotalNumberOfContactPoints()];

      for (int i = 0; i < indexOfPointsInContact.length; i++)
      {
         contactPointStates[indexOfPointsInContact[i]] = true;
      }

      return contactPointStates;
   }

   public void updateLegSingularityModule()
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.update();
   }

   public void correctCoMHeightTrajectoryForSingularityAvoidance(FrameVector2d comXYVelocity, CoMHeightTimeDerivativesData comHeightDataToCorrect,
         double zCurrent, ReferenceFrame pelvisZUpFrame)
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.correctCoMHeightTrajectoryForSingularityAvoidance(comXYVelocity, comHeightDataToCorrect, zCurrent,
            pelvisZUpFrame, getCurrentConstraintType());
   }

   public void correctCoMHeightTrajectoryForCollapseAvoidance(FrameVector2d comXYVelocity, CoMHeightTimeDerivativesData comHeightDataToCorrect, double zCurrent,
         ReferenceFrame pelvisZUpFrame, double footLoadPercentage)
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.correctCoMHeightTrajectoryForCollapseAvoidance(comXYVelocity, comHeightDataToCorrect, zCurrent,
            pelvisZUpFrame, footLoadPercentage, getCurrentConstraintType());
   }

   public void correctCoMHeightTrajectoryForUnreachableFootStep(CoMHeightTimeDerivativesData comHeightDataToCorrect)
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.correctCoMHeightTrajectoryForUnreachableFootStep(comHeightDataToCorrect, getCurrentConstraintType());
   }

   public void setFootstep(Footstep footstep, double swingTime)
   {
      // TODO Used to pass the desireds from the toe off state to swing state. Clean that up.
      if (stateMachine.getCurrentStateEnum() == ConstraintType.TOES)
      {
         FrameOrientation initialOrientation = new FrameOrientation();
         FrameVector initialAngularVelocity = new FrameVector();
         onToesState.getDesireds(initialOrientation, initialAngularVelocity);
         swingState.setInitialDesireds(initialOrientation, initialAngularVelocity);
      }
      swingState.setFootstep(footstep, swingTime);
   }

   public void handleFootTrajectoryCommand(FootTrajectoryCommand command)
   {
      moveViaWaypointsState.handleFootTrajectoryCommand(command);
   }

   public void resetHeightCorrectionParametersForSingularityAvoidance()
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.resetHeightCorrectionParameters();
   }

   /**
    * Request the swing trajectory to speed up using the given speed up factor.
    * It is clamped w.r.t. to {@link WalkingControllerParameters#getMinimumSwingTimeForDisturbanceRecovery()}.
    * @param speedUpFactor
    * @return the current swing time remaining for the swing foot trajectory
    */
   public double requestSwingSpeedUp(double speedUpFactor)
   {
      return swingState.requestSwingSpeedUp(speedUpFactor);
   }

   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return stateMachine.getCurrentState().getInverseDynamicsCommand();
   }

   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return stateMachine.getCurrentState().getFeedbackControlCommand();
   }

   public FeedbackControlCommandList createFeedbackControlTemplate()
   {
      FeedbackControlCommandList ret = new FeedbackControlCommandList();
      for (ConstraintType constraintType : ConstraintType.values())
      {
         AbstractFootControlState state = stateMachine.getState(constraintType);
         if (state != null && state.getFeedbackControlCommand() != null)
            ret.addCommand(state.getFeedbackControlCommand());
      }
      return ret;
   }

   public void initializeFootExploration()
   {
      if (useNewSupportState)
         supportStateNew.requestFootholdExploration();
      else
         setContactState(ConstraintType.EXPLORE_POLYGON);
   }

   public void setAllowFootholdAdjustments(boolean allow)
   {
      if (holdPositionState != null)
         holdPositionState.doFootholdAdjustments(allow);
   }

   private void requestExploration()
   {
      if (!isInFlatSupportState()) return;
      requestExploration.set(false);
      initializeFootExploration();
   }

   private void resetFootPolygon()
   {
      if (!isInFlatSupportState()) return;
      resetFootPolygon.set(false);
      if (footControlHelper.getPartialFootholdControlModule() != null)
      {
         footControlHelper.getPartialFootholdControlModule().reset();
      }
      controllerToolbox.resetFootSupportPolygon(robotSide);
   }
}