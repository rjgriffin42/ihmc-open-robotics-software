package us.ihmc.exampleSimulations.planarWalker;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.controllers.PDController;
import us.ihmc.robotics.math.trajectories.yoVariables.YoPolynomial;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.stateMachine.core.State;
import us.ihmc.robotics.stateMachine.core.StateMachine;
import us.ihmc.robotics.stateMachine.core.StateTransitionCondition;
import us.ihmc.robotics.stateMachine.factories.StateMachineFactory;
import us.ihmc.scs2.definition.controller.interfaces.Controller;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoint3D;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class BWCPlanarWalkingController implements Controller
{
   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final BWCPlanarWalkingRobot controllerRobot;

   private final YoDouble desiredBodyHeight = new YoDouble("desiredBodyHeight", registry);
   private final YoDouble desiredSwingHeight = new YoDouble("desiredSwingHeight", registry);
   private final YoDouble desiredSwingDuration = new YoDouble("desiredSwingDuration", registry);
   private final YoDouble desiredForwardVelocity = new YoDouble("desiredForwardVelocity", registry);

   private final YoDouble supportLegLengthKp = new YoDouble("supportLegLengthKp", registry);
   private final YoDouble supportLegLengthKd = new YoDouble("supportLegLengthKd", registry);

   private final YoDouble swingFootHeightKp = new YoDouble("swingFootHeightKp", registry);
   private final YoDouble swingFootHeightKd = new YoDouble("swingFootHeightKd", registry);
   private final YoDouble swingHipPitchKp = new YoDouble("swingHipPitchKp", registry);
   private final YoDouble swingHipPitchKd = new YoDouble("swingHipPitchKd", registry);

   private final YoDouble capturePointFeedback = new YoDouble("capturePointFeedback", registry);
   private final YoDouble capturePointFeedforward = new YoDouble("capturePointFeedforward", registry);
   private final YoDouble currentForwardVelocity = new YoDouble("currentForwardVelocity", registry);

   private enum LegStateName
   {Swing, Support}

   private final SideDependentList<StateMachine<LegStateName, State>> legStateMachines = new SideDependentList<>();
   private final SideDependentList<YoBoolean> hasFootHitGround = new SideDependentList<>();

   public BWCPlanarWalkingController(BWCPlanarWalkingRobot controllerRobot, RobotSide initialSwingSide)
   {
      this.controllerRobot = controllerRobot;

      // set up our leg length gains for the leg length controller
      supportLegLengthKp.set(15000.0);
      supportLegLengthKd.set(500.0);

      swingFootHeightKp.set(200.0);
      swingFootHeightKd.set(10.0);

      swingHipPitchKp.set(500.0);
      swingHipPitchKd.set(25.0);

      desiredBodyHeight.set((BWCPlanarWalkingRobotDefinition.shinLength + BWCPlanarWalkingRobotDefinition.thighLength) + -0.2);
      desiredSwingHeight.set(0.1);
      desiredSwingDuration.set(0.3);

      for (RobotSide robotSide : RobotSide.values)
      {
         hasFootHitGround.put(robotSide, new YoBoolean(robotSide.getLowerCaseName() + "HasFootHitGround", registry));

         StateMachineFactory<LegStateName, State> stateMachineFactory = new StateMachineFactory<>(LegStateName.class);
         stateMachineFactory.setNamePrefix(robotSide.getLowerCaseName() + "LegState").setRegistry(registry).buildYoClock(controllerRobot.getTime());

         stateMachineFactory.addState(LegStateName.Support, new SupportFootState(robotSide));
         stateMachineFactory.addState(LegStateName.Swing, new SwingFootState(robotSide));

         stateMachineFactory.addDoneTransition(LegStateName.Swing, LegStateName.Support);
         stateMachineFactory.addTransition(LegStateName.Support, LegStateName.Swing, new StartSwingCondition(robotSide));

         LegStateName initialState = robotSide == initialSwingSide ? LegStateName.Swing : LegStateName.Support;
         legStateMachines.put(robotSide, stateMachineFactory.build(initialState));
      }

      registry.addChild(controllerRobot.getYoRegistry());
   }

   @Override
   public YoRegistry getYoRegistry()
   {
      return registry;
   }

   @Override
   public void doControl()
   {
      controllerRobot.update();

      for (RobotSide robotSide : RobotSide.values)
      {
         legStateMachines.get(robotSide).doActionAndTransition();
      }
   }

   private class StartSwingCondition implements StateTransitionCondition
   {
      private final RobotSide candidateSwingState;

      public StartSwingCondition(RobotSide robotSide)
      {
         candidateSwingState = robotSide;
      }

      @Override
      public boolean testCondition(double timeInCurrentState)
      {
         if (hasFootHitGround.get(candidateSwingState.getOppositeSide()).getValue())
            return true;

         return false;
      }
   }

   private class SwingFootState implements State
   {
      private final RobotSide swingSide;
      private final YoPolynomial swingFootHeightTrajectory;
      private final PDController swingFootHeightController;
      private final PDController swingHipPitchController;
      private final YoDouble swingLegForce;
      private final YoDouble swingHipForce;

      public SwingFootState(RobotSide swingSide)
      {
         this.swingSide = swingSide;

         swingFootHeightTrajectory = new YoPolynomial(swingSide.getLowerCaseName() + "SwingFootHeight", 5, registry);
         swingFootHeightController = new PDController(swingFootHeightKp,
                                                      swingFootHeightKd,
                                                      swingSide.getLowerCaseName() + "SwingFootHeightController",
                                                      registry);
         swingHipPitchController = new PDController(swingHipPitchKp, swingHipPitchKd, swingSide.getLowerCaseName() + "SwingHipPitchController", registry);
         swingLegForce = new YoDouble(swingSide.getLowerCaseName() + "DesiredLegForce", registry);
         swingHipForce = new YoDouble(swingSide.getLowerCaseName() + "DesiredHipForce", registry);
      }

      @Override
      public void onEntry()
      {
         FramePoint3D footPosition = new FramePoint3D(controllerRobot.getFootFrame(swingSide));
         footPosition.changeFrame(controllerRobot.getWorldFrame());

         hasFootHitGround.get(swingSide).set(false);

         double initialTime = 0.0;
         double swingDuration = desiredSwingDuration.getDoubleValue();
         double initialHeight = footPosition.getZ();
         double initialVelocity = 0.0;
         double finalHeight = 0.0;
         double finalVelocity = 0.0;
         swingFootHeightTrajectory.setQuarticUsingWayPoint(initialTime,
                                                           0.5 * swingDuration,
                                                           swingDuration,
                                                           initialHeight,
                                                           initialVelocity,
                                                           desiredSwingHeight.getDoubleValue(),
                                                           finalHeight,
                                                           finalVelocity);
      }

      @Override
      public void doAction(double timeInState)
      {
         swingFootHeightTrajectory.compute(timeInState);

         FramePoint3D footPosition = new FramePoint3D(controllerRobot.getFootFrame(swingSide));
         footPosition.changeFrame(controllerRobot.getWorldFrame());

         double currentHeight = footPosition.getZ();
         double desiredHeight = swingFootHeightTrajectory.getValue();
         // This is approximately the right velocity. Good enough for stability, but not great overall.
         double currentVelocity = controllerRobot.getKneeJoint(swingSide).getQd();
         double desiredVelocity = swingFootHeightTrajectory.getVelocity();

         double legForce = swingFootHeightController.compute(currentHeight, desiredHeight, currentVelocity, desiredVelocity);
         swingLegForce.set(legForce);

         // set the desired torque to the knee joint to achieve the desired swing foot height
         controllerRobot.getKneeJoint(swingSide).setTau(legForce);

         double z0 = 1.0;
         double omega = Math.sqrt(9.81 / z0);
//         double currentForwardVelocity = controllerRobot.getCenterOfMassVelocity().getX();
//         currentForwardVelocity.changeFrame(cont);
         capturePointFeedback.set(controllerRobot.getCenterOfMassVelocity() / omega);
         capturePointFeedforward.set(
               -desiredForwardVelocity.getValue() * desiredSwingDuration.getValue() / (Math.exp(omega * desiredSwingDuration.getValue()) - 1));
         double desiredFootPositionX = capturePointFeedback.getDoubleValue() + capturePointFeedforward.getDoubleValue();
         double desiredFootVelocityX = 0.0;

         footPosition.changeFrame(controllerRobot.getCenterOfMassFrame());
         double currentFootPositionX = footPosition.getX();
         double currentFootVelocityX = -controllerRobot.getHipJoint(swingSide).getQd(); // FIXME make this have some actual value
         double hipForce = -swingHipPitchController.compute(currentFootPositionX, desiredFootPositionX, currentFootVelocityX, desiredFootVelocityX);
         swingHipForce.set(hipForce);

         controllerRobot.getHipJoint(swingSide).setTau(hipForce);
      }

      @Override
      public void onExit(double timeInState)
      {

      }

      @Override
      public boolean isDone(double timeInState)
      {
         return timeInState > desiredSwingDuration.getDoubleValue();
      }
   }

   private class SupportFootState implements State
   {
      private final RobotSide supportSide;

      private final PDController supportBodyController;
      private final PDController supportLegLengthController;
      private final YoDouble supportLegDesiredKneeForce;

      public SupportFootState(RobotSide supportSide)
      {
         this.supportSide = supportSide;

         supportLegDesiredKneeForce = new YoDouble(supportSide.getLowerCaseName() + "SUpportLegDesiredKneeForce", registry);
         supportLegLengthController = new PDController(supportLegLengthKp,
                                                       supportLegLengthKd,
                                                       supportSide.getLowerCaseName() + "SupportLegLengthController",
                                                       registry);
         supportBodyController = new PDController(
                                                       supportSide.getLowerCaseName() + "SupportBodyController",
                                                       registry);
         supportBodyController.setProportionalGain(50.0);
         supportBodyController.setDerivativeGain(1.0);
      }

      @Override
      public void onEntry()
      {
         hasFootHitGround.get(supportSide).set(true);
      }

      @Override
      public void doAction(double timeInState)
      {
         FramePoint3D comPosition = new FramePoint3D(controllerRobot.getCenterOfMassFrame());
         comPosition.changeFrame(controllerRobot.getWorldFrame());

         double legLengthVelocity = -controllerRobot.getKneeJoint(supportSide).getQd();

         double desiredHeight = desiredBodyHeight.getDoubleValue();
         double desiredKneeVelocity = 0.0;
         // compute and record the desired torque to hold the leg at the desired length
         double torque = supportLegLengthController.compute(comPosition.getZ(), desiredHeight, legLengthVelocity, desiredKneeVelocity);
         supportLegDesiredKneeForce.set(torque);

         // set the desired torque to the knee joint to hold the leg at the desired length
         controllerRobot.getKneeJoint(supportSide).setTau(-torque);

         FrameQuaternion bodyOrientation = new FrameQuaternion(controllerRobot.getBodyFrame());
         bodyOrientation.changeFrame(controllerRobot.getWorldFrame());
         double hipTorque = supportBodyController.compute(bodyOrientation.getPitch(), 0.0, 0.0, 0.0);
         // set the desired hip pitch torque to zero.
         controllerRobot.getHipJoint(supportSide).setTau(hipTorque);
      }

      @Override
      public void onExit(double timeInState)
      {

      }

      @Override
      public boolean isDone(double timeInState)
      {
         return false;
      }
   }
}
