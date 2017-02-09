package us.ihmc.exampleSimulations.planarWalker;

import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.controllers.PIDController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;
import us.ihmc.robotics.math.filters.AlphaFilteredYoVariable;
import us.ihmc.robotics.math.trajectories.YoMinimumJerkTrajectory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachine;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachineBuilder;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachineState;

public class PeterPlanarWalkerStateMachine
{
   private final double MAX_HIP_ANGLE = 0.8;
   private double HIP_DEFUALT_P_GAIN = 100.0;
   private double HIP_DEFUALT_D_GAIN = 10.0;

   private double KNEE_DEFUALT_P_GAIN = 10000.0;
   private double KNEE_DEFUALT_D_GAIN = 1000.0;

   private double deltaT;

   private PeterPlanarWalkerRobot robot;
   private YoVariableRegistry registry;
   private SideDependentList<PIDController> kneeControllers = new SideDependentList<PIDController>();
   private SideDependentList<PIDController> hipControllers = new SideDependentList<PIDController>();

   private DoubleYoVariable desiredKneeExtension;
   private DoubleYoVariable desiredPitch;
   private DoubleYoVariable desiredHeight;
   private DoubleYoVariable swingTime;
   private DoubleYoVariable desiredSwingLegHipAngle;
   private DoubleYoVariable scaleForVelToAngle;
   private DoubleYoVariable desiredKneeStance;
   private DoubleYoVariable angleForCapture;
   private DoubleYoVariable feedForwardAngle;
   private DoubleYoVariable velocityErrorAngle;
   private DoubleYoVariable feedForwardGain;
   private DoubleYoVariable lastStepHipAngle;
   private DoubleYoVariable stepToStepHipAngleDelta;

   private DoubleYoVariable swingTimeForThisStep;
   private BooleanYoVariable initalizedKneeExtension;
   private BooleanYoVariable initalizedKneeDoubleExtension;

   private DoubleYoVariable kneeMoveStartTime;
   private DoubleYoVariable startingHipAngle;
   private DoubleYoVariable maxVelocityErrorAngle;

   private DoubleYoVariable desiredBodyVelocity;
   private DoubleYoVariable alphaFilterVariable;
   private AlphaFilteredYoVariable filteredDesiredVelocity;

   private YoMinimumJerkTrajectory trajectorySwingHip;
   private YoMinimumJerkTrajectory trajectorySwingKnee;

   private FiniteStateMachineBuilder<ControllerState, ControllerEvent> stateMachineBuilder;
   private FiniteStateMachine<ControllerState, ControllerEvent> stateMachine;
   private final YoVariable<?> timestamp;
   private final YoPlanarWalkerTimedStep stepCommand;

   public PeterPlanarWalkerStateMachine(PeterPlanarWalkerRobot robot, double deltaT, RobotSide robotSide, YoVariable<?> timestamp, YoVariableRegistry parentRegistry)
   {
      String prefix = robotSide.getLowerCaseName();
      this.robot = robot;
      this.deltaT = deltaT;
      this.timestamp = timestamp;
      this.registry = new YoVariableRegistry(prefix + getClass().getSimpleName());
      this.desiredKneeExtension = new DoubleYoVariable("desiredKneeExtension", registry);
      this.desiredPitch = new DoubleYoVariable("desiredPitch", registry);
      this.desiredHeight = new DoubleYoVariable("desiredHeight", registry);
      this.swingTime = new DoubleYoVariable("swingTime", registry);
      this.desiredSwingLegHipAngle = new DoubleYoVariable("desiredSwingLegHipAngle", registry);
      this.scaleForVelToAngle = new DoubleYoVariable("scaleForVelToAngle", registry);
      this.desiredKneeStance = new DoubleYoVariable("desiredKneeStance", registry);
      this.angleForCapture = new DoubleYoVariable("angleForCapture", registry);
      this.feedForwardAngle = new DoubleYoVariable("feedForwardAngle", registry);
      this.velocityErrorAngle = new DoubleYoVariable("velocityErrorAngle", registry);
      this.feedForwardGain = new DoubleYoVariable("feedForwardGain", registry);
      this.lastStepHipAngle = new DoubleYoVariable("lastStepHipAngle", registry);
      this.stepToStepHipAngleDelta = new DoubleYoVariable("stepToStepHipAngleDelta", registry);

      this.swingTimeForThisStep = new DoubleYoVariable("swingTimeForThisStep", registry);
      this.initalizedKneeExtension = new BooleanYoVariable("initalizedKneeExtension", registry);
      this.initalizedKneeDoubleExtension = new BooleanYoVariable("initalizedKneeDoubleExtension", registry);

      this.kneeMoveStartTime = new DoubleYoVariable("kneeMoveStartTime", registry);
      this.startingHipAngle = new DoubleYoVariable("startingHipAngle", registry);
      this.maxVelocityErrorAngle = new DoubleYoVariable("maxVelocityErrorAngle", registry);

      this.desiredBodyVelocity = new DoubleYoVariable("desiredBodyVelocity", registry);
      this.alphaFilterVariable = new DoubleYoVariable("alphaFilterVariable", registry);
      this.filteredDesiredVelocity = new AlphaFilteredYoVariable("filteredDesiredVelocity", registry, alphaFilterVariable,
                                                                   desiredBodyVelocity);
      this.stepCommand = new YoPlanarWalkerTimedStep(prefix + "StepCommand", registry);

      PIDController pidController = new PIDController(robotSide.getSideNameFirstLetter() + "_Knee", registry);
      pidController.setProportionalGain(KNEE_DEFUALT_P_GAIN);
      pidController.setDerivativeGain(KNEE_DEFUALT_D_GAIN);
      kneeControllers.put(robotSide, pidController);

      pidController = new PIDController(robotSide.getSideNameFirstLetter() + "_Hip", registry);
      pidController.setProportionalGain(HIP_DEFUALT_P_GAIN);
      pidController.setDerivativeGain(HIP_DEFUALT_D_GAIN);
      hipControllers.put(robotSide, pidController);

      trajectorySwingHip = new YoMinimumJerkTrajectory("trajectorySwingHip", registry);
      trajectorySwingKnee = new YoMinimumJerkTrajectory("trajectorySwingKnee", registry);
      desiredHeight.set(robot.nominalHeight);
      desiredKneeStance.set(robot.lowerLinkLength / 2.0);
      swingTime.set(0.3);
      scaleForVelToAngle.set(0.9);
      feedForwardGain.set(0.05);
      stepToStepHipAngleDelta.set(0.3);
      maxVelocityErrorAngle.set(0.3);
      alphaFilterVariable.set(0.9999);

      initializeStateMachine(robotSide);
      parentRegistry.addChild(registry);
   }

   private void initializeStateMachine(RobotSide supportLegSide)
   {
      stateMachineBuilder = new FiniteStateMachineBuilder(ControllerState.class, ControllerEvent.class, "stateMachineBuilder", registry);

      stateMachineBuilder.addState(ControllerState.SUPPORT, new SupportState(supportLegSide));
      stateMachineBuilder.addState(ControllerState.SWING, new SwingState(supportLegSide));
      stateMachineBuilder.addTransition(ControllerEvent.TIMEOUT, ControllerState.SUPPORT, ControllerState.SWING);
      stateMachineBuilder.addTransition(ControllerEvent.TIMEOUT, ControllerState.SWING, ControllerState.SUPPORT);

      stateMachine = stateMachineBuilder.build(ControllerState.SUPPORT);
   }

   private class SupportState implements FiniteStateMachineState<ControllerEvent>
   {
      private RobotSide supportLeg;

      public SupportState(RobotSide robotSide)
      {
         supportLeg = robotSide;
      }

      @Override
      public void onEntry()
      {
      }

      @Override
      public ControllerEvent process()
      {
         double currentTime = timestamp.getValueAsDouble();
         double liftOffTime = stepCommand.getTimeInterval().getStartTime();
         double touchDownTime = stepCommand.getTimeInterval().getEndTime();

         controlHipToMaintainPitch(supportLeg);

         //add swing leg torque to stand leg
         addOppositeLegHipTorque(supportLeg);

         //controlKneeToMaintainBodyHeight(supportLeg);
         controlKneeToPosition(supportLeg, desiredKneeStance.getDoubleValue(), 0.0);

         if (currentTime >= liftOffTime && currentTime < touchDownTime)
         {
            return ControllerEvent.TIMEOUT;
         }
         else
         {
            return null;
         }
      }

      @Override
      public void onExit()
      {
      }
   }

   private class SwingState implements FiniteStateMachineState<ControllerEvent>
   {
      private RobotSide swingLeg;

      public SwingState(RobotSide robotSide)
      {
         swingLeg = robotSide;
      }

      @Override
      public void onEntry()
      {
      }

      @Override
      public ControllerEvent process()
      {
         double currentTime = timestamp.getValueAsDouble();
         double liftOffTime = stepCommand.getTimeInterval().getStartTime();
         double touchDownTime = stepCommand.getTimeInterval().getEndTime();
         double timeInCurrentState = currentTime - liftOffTime;

         if ((timeInCurrentState > swingTimeForThisStep.getDoubleValue() / 2.0) && !initalizedKneeExtension.getBooleanValue())
         {
            double currentKneePosition = robot.getKneePosition(swingLeg);
            trajectorySwingKnee.setParams(currentKneePosition, 0.0, 0.0, desiredKneeStance.getDoubleValue(), 0.0, 0.0, 0.0,
                                          swingTimeForThisStep.getDoubleValue() / 2.0);
            initalizedKneeExtension.set(true);
            kneeMoveStartTime.set(timeInCurrentState);
         }
         else if ((timeInCurrentState > swingTimeForThisStep.getDoubleValue() && !initalizedKneeDoubleExtension.getBooleanValue()))
         {
            double currentKneePosition = robot.getKneePosition(swingLeg);
            trajectorySwingKnee.setParams(currentKneePosition, 0.0, 0.0, desiredKneeStance.getDoubleValue() + 0.5, 0.0, 0.0, 0.0, 0.125);
            initalizedKneeDoubleExtension.set(true);
            kneeMoveStartTime.set(timeInCurrentState);
         }

         trajectorySwingKnee.computeTrajectory(timeInCurrentState - kneeMoveStartTime.getDoubleValue());
         double desiredKneePositon = trajectorySwingKnee.getPosition();
         double desiredKneeVelocity = trajectorySwingKnee.getVelocity();
         controlKneeToPosition(swingLeg, desiredKneePositon, desiredKneeVelocity);

         desiredSwingLegHipAngle.set(getDesireHipAngle());
         trajectorySwingHip.setParams(startingHipAngle.getDoubleValue(), 0.0, 0.0, desiredSwingLegHipAngle.getDoubleValue(), 0.0, 0.0, 0.0,
                                      swingTimeForThisStep.getDoubleValue());

         trajectorySwingHip.computeTrajectory(timeInCurrentState);
         double desiredHipAngle = trajectorySwingHip.getPosition();
         double currentHipAngle = robot.getHipPosition(swingLeg);
         double currentHipAngleRate = robot.getHipVelocity(swingLeg);

         PIDController pidController = hipControllers.get(swingLeg);
         double controlEffort = pidController.compute(currentHipAngle, desiredHipAngle, currentHipAngleRate, 0.0, deltaT);
         robot.setHipTorque(swingLeg, controlEffort);

         if (currentTime >= touchDownTime)
         {
            return ControllerEvent.TIMEOUT;
         }
         else
            return null;
      }

      @Override
      public void onExit()
      {
      }
   }

   private double getDesireHipAngle()
   {
      double legLength = robot.upperLinkLength + desiredKneeStance.getDoubleValue();
      angleForCapture.set(HipAngleCapturePointCalculator.getHipAngle(robot.getBodyVelocity(), legLength));
      angleForCapture.set(-angleForCapture.getDoubleValue() * Math.signum(robot.getBodyVelocity()));

      //only use a fraction of it
      //angleForCapture.set(0.8 *angleForCapture.getDoubleValue());

      //limit this angle
      angleForCapture.set(MathTools.clipToMinMax(angleForCapture.getDoubleValue(), MAX_HIP_ANGLE));

      //angle is opposite sign of desired velocity
      double velocityError = (filteredDesiredVelocity.getDoubleValue() - robot.getBodyVelocity());
      velocityErrorAngle.set(velocityError * scaleForVelToAngle.getDoubleValue());
      velocityErrorAngle.set(MathTools.clipToMinMax(velocityErrorAngle.getDoubleValue(), maxVelocityErrorAngle.getDoubleValue()));

      feedForwardAngle.set(filteredDesiredVelocity.getDoubleValue() * feedForwardGain.getDoubleValue());
      double angle = angleForCapture.getDoubleValue() + feedForwardAngle.getDoubleValue() + velocityErrorAngle.getDoubleValue();

      angle = MathTools.clipToMinMax(angle, lastStepHipAngle.getDoubleValue() - stepToStepHipAngleDelta.getDoubleValue(),
                                     lastStepHipAngle.getDoubleValue() + stepToStepHipAngleDelta.getDoubleValue());
      return angle;
   }

   private void controlHipToMaintainPitch(RobotSide robotSide)
   {
      double currentPitch = robot.getBodyPitch();
      double currentPitchRate = robot.getBodyPitchVelocity();

      double controlEffort = -hipControllers.get(robotSide).compute(currentPitch, desiredPitch.getDoubleValue(), currentPitchRate, 0.0, deltaT);
      robot.setHipTorque(robotSide, controlEffort);
   }

   private void addOppositeLegHipTorque(RobotSide legToAddTorque)
   {
      double oppositeLegTorque = robot.getHipTorque(legToAddTorque.getOppositeSide());
      double currentTorque = robot.getHipTorque(legToAddTorque);
      robot.setHipTorque(legToAddTorque, currentTorque - oppositeLegTorque);
   }

   private void controlKneeToMaintainBodyHeight(RobotSide robotSide)
   {
      double currentHeight = robot.getBodyHeight();
      double currentHeightRate = robot.getBodyHeightVelocity();

      double controlEffort = kneeControllers.get(robotSide).compute(currentHeight, desiredHeight.getDoubleValue(), currentHeightRate, 0.0, deltaT);
      robot.setKneeTorque(robotSide, controlEffort);
   }

   private void controlKneeToPosition(RobotSide robotSide, double desiredPosition, double desiredVelocity)
   {
      double kneePosition = robot.getKneePosition(robotSide);
      double kneePositionRate = robot.getKneeVelocity(robotSide);

      double controlEffort = kneeControllers.get(robotSide).compute(kneePosition, desiredPosition, kneePositionRate, desiredVelocity, deltaT);
      robot.setKneeTorque(robotSide, controlEffort);
   }
   
   public FiniteStateMachine<ControllerState, ControllerEvent> getStateMachine()
   {
      return stateMachine;
   }

   public enum ControllerState
   {
      SUPPORT, SWING;
   }

   public enum ControllerEvent
   {
      TIMEOUT;
   }
}
