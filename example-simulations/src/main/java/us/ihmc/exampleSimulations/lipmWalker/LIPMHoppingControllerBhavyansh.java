package us.ihmc.exampleSimulations.lipmWalker;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.math.trajectories.interfaces.PolynomialBasics;
import us.ihmc.robotics.math.trajectories.yoVariables.YoPolynomial;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.stateMachine.core.State;
import us.ihmc.robotics.stateMachine.core.StateMachine;
import us.ihmc.robotics.stateMachine.core.StateTransitionCondition;
import us.ihmc.robotics.stateMachine.factories.StateMachineFactory;
import us.ihmc.simulationconstructionset.util.RobotController;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;

import java.util.ArrayList;

public class LIPMHoppingControllerBhavyansh implements RobotController
{
   private final LIPMWalkerRobot robot;
   private YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private YoDouble t;

   private final YoDouble kdFoot = new YoDouble("kdFoot", registry);

   private final YoDouble kpBody = new YoDouble("kpBody", registry);
   private final YoDouble kdBody = new YoDouble("kdBody", registry);

   private final YoDouble kpKnee = new YoDouble("kpKnee", registry);
   private final YoDouble kdKnee = new YoDouble("kdKnee", registry);
   private final YoDouble kpHip = new YoDouble("kpHip", registry);
   private final YoDouble kdHip = new YoDouble("kdHip", registry);

   private final YoDouble q_d_leftKnee = new YoDouble("q_d_leftKnee", registry);
   private final YoDouble q_d_rightKnee = new YoDouble("q_d_rightKnee", registry);
   private final YoDouble q_d_leftHip = new YoDouble("q_d_leftHip", registry);
   private final YoDouble q_d_rightHip = new YoDouble("q_d_rightHip", registry);

   private final YoDouble q_rightHipWorld = new YoDouble("q_rightHipWorld", registry);
   private final YoDouble q_leftHipWorld = new YoDouble("q_leftHipWorld", registry);

   private final YoDouble footXTarget = new YoDouble("footXTarget", registry);

   private final YoDouble comHeight = new YoDouble("comHeight", registry);

   private final YoDouble comXVelocity = new YoDouble("comXVelocity", registry);
   private final YoDouble comXPositionFromFoot = new YoDouble("comXPositionFromFoot", registry);

   private final SideDependentList<YoDouble> desiredKneeLengths = new SideDependentList<YoDouble>(q_d_leftKnee, q_d_rightKnee);
   private final SideDependentList<YoDouble> desiredHipAngles = new SideDependentList<YoDouble>(q_d_leftHip, q_d_rightHip);
   private final SideDependentList<YoDouble> worldHipAngles = new SideDependentList<YoDouble>(q_leftHipWorld, q_rightHipWorld);
   private final SideDependentList<StateMachine<LIPMHoppingControllerBhavyansh.States, State>> stateMachines;

   private final YoDouble desiredHeight = new YoDouble("desiredHeight", registry);
   private final YoDouble orbitalEnergy = new YoDouble("orbitalEnergy", registry);
   private final YoDouble strideLength = new YoDouble("strideLength", registry);
   private final YoDouble hipDiffAngle = new YoDouble("hipDiffAngle", registry);
   private final YoDouble swingTime = new YoDouble("swingTime", registry);
   private final YoDouble lastStepLength = new YoDouble("lastStepLength", registry);

   private final YoDouble leftFootZForce = new YoDouble("leftFootZForce", registry);
   private final YoDouble rightFootZForce = new YoDouble("rightFootZForce", registry);

   private ArrayList<Double> groundContactPositions = new ArrayList<Double>();
   private final SideDependentList<YoDouble> footZForces = new SideDependentList<YoDouble>(leftFootZForce, rightFootZForce);

   private PolynomialBasics trajectorySwingHipPitch;
   private PolynomialBasics trajectorySwingKneeLength;
   private final YoFrameVector3D nextStepCoMLocation;

   private final YoInteger numberOfStepsTaken = new YoInteger("numberOfStepsTaken", registry);

   private enum States
   {
      SUPPORT, SWING, FLIGHT;
   }

   private YoDouble lastStanceTime = new YoDouble("lastStanceTime", registry);
   private YoDouble timeOfLastHeelOff = new YoDouble("timeOfLastHeelOff", registry);
   private YoDouble timeOfLastHeelOn = new YoDouble("timeOfLastHeelOn", registry);
   private YoBoolean swingTrajectoriesCalculated = new YoBoolean("footLocationCalculated", registry);
   private YoBoolean swingFootGroundContact = new YoBoolean("swingFootGroundContact", registry);
   private YoDouble desiredTopVelocity = new YoDouble("desiredTopVelocity", registry);
   private YoDouble desiredEnergy = new YoDouble("desiredEnergy", registry);

   private final double g = 9.81;

   public LIPMHoppingControllerBhavyansh(LIPMWalkerRobot robot)
   {
      this.robot = robot;

      t = (YoDouble) robot.getRobot().findVariable("t");
      trajectorySwingHipPitch = new YoPolynomial("trajectorySwingHipAngle", 6, registry);
      trajectorySwingKneeLength = new YoPolynomial("trajectorySwingKneeLength", 6, registry);

      nextStepCoMLocation = new YoFrameVector3D("nextStepCoMLocation", ReferenceFrame.getWorldFrame(), registry);

      initialize();

      stateMachines = setupStateMachines();
   }

   @Override
   public void initialize()
   {
      numberOfStepsTaken.set(0);

      kdFoot.set(10.0);

      kpBody.set(200.0);
      kdBody.set(30.0);

      kpKnee.set(1000.0);
      kdKnee.set(100.0);

      kpHip.set(1000.0);
      kdHip.set(10.0);

      q_d_leftKnee.set(0.8);
      q_d_rightKnee.set(0.7);

      q_d_leftHip.set(0.0);
      q_d_rightHip.set(0.0);

      desiredHeight.set(0.8);

      strideLength.set(0.5);
      swingTime.set(0.4);

      lastStanceTime.set(0.3);
      timeOfLastHeelOff.set(0.0);
      timeOfLastHeelOn.set(0.0);
      swingTrajectoriesCalculated.set(false);
      swingFootGroundContact.set(false);
      desiredTopVelocity.set(1.0f);
      desiredEnergy.set(0.5 * desiredTopVelocity.getValue() * desiredTopVelocity.getValue());

      orbitalEnergy.set(0.5 * 0.7 * 0.7);
      calculateStepCoMPosition();
      footXTarget.set(nextStepCoMLocation.getX());

      calculateSwingTrajectories(RobotSide.RIGHT, nextStepCoMLocation, 0.0, true);

      groundContactPositions.add(0.0);

      leftFootZForce.set(robot.getFootZForce(RobotSide.LEFT));
      rightFootZForce.set(robot.getFootZForce(RobotSide.RIGHT));
   }

   @Override
   public YoRegistry getYoRegistry()
   {
      return registry;
   }

   @Override
   public void doControl()
   {
      filterFootForces();

      for (RobotSide robotSide : RobotSide.values())
      {
         stateMachines.get(robotSide).doAction();
         stateMachines.get(robotSide).doTransitions();
      }

      LogTools.info("State -> Left: {}, Right: {}",
                    stateMachines.get(RobotSide.LEFT).getCurrentStateKey(),
                    stateMachines.get(RobotSide.RIGHT).getCurrentStateKey());
   }

   public void filterFootForces()
   {
      leftFootZForce.set(leftFootZForce.getValue() * 0.9 + robot.getFootZForce(RobotSide.LEFT) * 0.1);
      rightFootZForce.set(rightFootZForce.getValue() * 0.9 + robot.getFootZForce(RobotSide.RIGHT) * 0.1);
   }

   private void computeOrbitalEnergy(RobotSide supportSide)
   {

      /* Compute and set orbital energy YoDouble. Based on virtual spring-mass system. */
      comXVelocity.set(robot.getCenterOfMassVelocity().getX());
      comXPositionFromFoot.set(robot.getCenterOfMassXDistanceFromSupportFoot());
      double orbitalEnergyValue = 0.5 * comXVelocity.getValue() * comXVelocity.getValue()
                                  - 0.5 * g / desiredHeight.getDoubleValue() * comXPositionFromFoot.getValue() * comXPositionFromFoot.getValue();
      orbitalEnergy.set(orbitalEnergyValue);
   }

   private void calculateStepCoMPosition()
   {
      double energy = orbitalEnergy.getValue();
      double x_final = (strideLength.getValue() / 2) + (desiredHeight.getValue() / (g * strideLength.getValue()) * (desiredEnergy.getValue() - energy));

      LogTools.info("Xf: {} \tEnergy: {} \tDesiredEnergy: {}", x_final, energy, desiredEnergy);

      Vector3DReadOnly stepCoMPosition = new Vector3D(x_final, 0.0, 0.0);
      nextStepCoMLocation.set(stepCoMPosition);
   }

   private void calculateSwingTrajectories(RobotSide swingSide, Vector3DReadOnly stepCoMPosition, double comPositionFromSupportFoot, boolean initial)
   {
      double distanceToNextStep = strideLength.getValue() - stepCoMPosition.getX();
      double desiredHipAngle = -EuclidCoreTools.atan2(distanceToNextStep, desiredHeight.getValue());
      double desiredKneeLength = EuclidCoreTools.squareRoot(distanceToNextStep * distanceToNextStep + desiredHeight.getValue() * desiredHeight.getValue());

      LogTools.info("DistanceToNextStep: {} \tCoMX: {} \tDesiredKneeLength: {}", distanceToNextStep, comPositionFromSupportFoot, desiredKneeLength);

      double hipAngle = worldHipAngles.get(swingSide).getValue();
      double kneeLength = robot.getKneeLength(swingSide);
      trajectorySwingHipPitch.setQuintic(comPositionFromSupportFoot, nextStepCoMLocation.getX(), hipAngle, 0.0, 0.0, desiredHipAngle, 0.0, 0.0);
      trajectorySwingKneeLength.setQuintic(comPositionFromSupportFoot, nextStepCoMLocation.getX(), kneeLength, 0.0, 0.0, desiredKneeLength, 0.0, 0.0);
   }

   private void controlSupportLeg(RobotSide side)
   {
      double kneeLength = robot.getKneeLength(side);
      double kneeVelocity = robot.getKneeVelocity(side);

      double desiredKneeLength = 1.0f;

      Point3DReadOnly centerOfMassPosition = robot.getCenterOfMassPosition();
      Vector3DReadOnly centerOfMassVelocity = robot.getCenterOfMassVelocity();
      double mass = robot.getMass();

      computeOrbitalEnergy(side);
      comHeight.set(centerOfMassPosition.getZ());

//      if (centerOfMassVelocity.getZ() < -1.0 && footZForces.get(side).getValue() < 1.0f)
//      {
//         desiredKneeLength = 1.0f;
//      }

      //      double feedForwardSupportKneeForce = g * mass * kneeLength / centerOfMassPosition.getZ();
      //      double feedBackKneeForce =
      //            kpKnee.getValue() * (desiredHeight.getValue() - comHeight.getValue()) + kdKnee.getValue() * (0.0 - centerOfMassVelocity.getZ());

      double feedBackKneeForce = 4000 * (desiredKneeLength - kneeLength) + 20 * (0.0 - kneeVelocity);
      robot.setKneeForce(side, feedBackKneeForce);

      robot.setHipTorque(side, -kpBody.getValue() * (0.0 - robot.getBodyPitchAngle()) + kdBody.getValue() * (robot.getBodyPitchAngularVelocity()));

      worldHipAngles.get(side).set(robot.getHipAngle(side) + robot.getBodyPitchAngle());
   }

   private void controlSwingLeg(RobotSide side, double timeInState)
   {
      double feedBackKneeForce;

      double hipAngle = robot.getHipAngle(side) + robot.getBodyPitchAngle();
      worldHipAngles.get(side).set(hipAngle);

      double supportHipAngle = robot.getHipAngle(side.getOppositeSide()) + robot.getBodyPitchAngle();
      double hipVelocity = robot.getHipVelocity(side);
      double kneeLength = robot.getKneeLength(side);
      double kneeVelocity = robot.getKneeVelocity(side);

      //      if (StrictMath.abs(comXPositionFromFoot.getValue()) < 0.01 && swingTrajectoriesCalculated.getValue() == false)
      //      {
      //         calculateStepCoMPosition();
      //         footXTarget.set(nextStepCoMLocation.getX());
      //         calculateSwingTrajectories(side, nextStepCoMLocation, robot.getCenterOfMassXDistanceFromSupportFoot(), false);
      //         swingTrajectoriesCalculated.set(true);
      //      }

      Point3DReadOnly centerOfMassPosition = robot.getCenterOfMassPosition();
      Vector3DReadOnly centerOfMassVelocity = robot.getCenterOfMassVelocity();


      double footXDesiredFromCoM = centerOfMassVelocity.getX() * lastStanceTime.getValue() / 2 + kdFoot.getValue() * (desiredTopVelocity.getValue() - centerOfMassVelocity.getX());
//      double desiredHipAngle = - EuclidCoreTools.acos(footXDesiredFromCoM / 0.9f);
      double desiredHipAngle =  (0.02 * kdFoot.getValue() * (desiredTopVelocity.getValue() / 2.0f - centerOfMassVelocity.getX()));


      double desiredKneeLength = desiredKneeLengths.get(side).getValue();
      double desiredKneeVelocity = 0.0;
      double desiredHipVelocity = 0.0;

//      if (comXPositionFromFoot.getValue() > 0.0)
//      {
//         //         trajectorySwingKneeLength.compute(comXPositionFromFoot.getValue());
//         //         trajectorySwingHipPitch.compute(comXPositionFromFoot.getValue());
//         //         desiredKneeLength = trajectorySwingKneeLength.getValue();
//         //         desiredKneeVelocity = trajectorySwingKneeLength.getVelocity();
//         //         desiredHipAngle = trajectorySwingHipPitch.getValue();
//         //         desiredHipVelocity = trajectorySwingHipPitch.getVelocity();
//
//         desiredHipAngle = ;
//      }

      /* ----------------------------------- Compute and set knee force. ----------------------------------------------*/
      desiredKneeLengths.get(side).set(desiredKneeLength);
      feedBackKneeForce = 1300 * (desiredKneeLengths.get(side).getValue() - kneeLength) + 70 * (desiredKneeVelocity - kneeVelocity);
      robot.setKneeForce(side, feedBackKneeForce);

      /* ----------------------------------- Compute and set hip torque. --------------------------------------------*/
      double feedBackHipTorque = kpHip.getValue() * (desiredHipAngle - hipAngle) + kdHip.getValue() * (desiredHipVelocity - hipVelocity);
      desiredHipAngles.get(side).set(desiredHipAngle);
      robot.setHipTorque(side, feedBackHipTorque);

      //      LogTools.info("CoMX: {}, KneeLength: {}, HipAngle: {}", comXPositionFromFoot.getValue(), desiredKneeLength, desiredHipAngle);
   }

   private void controlFlightLeg(RobotSide side, double timeInState)
   {
      double hipAngle = robot.getHipAngle(side) + robot.getBodyPitchAngle();
      worldHipAngles.get(side).set(hipAngle);

      double supportHipAngle = robot.getHipAngle(side.getOppositeSide()) + robot.getBodyPitchAngle();
      double hipVelocity = robot.getHipVelocity(side);
      double kneeLength = robot.getKneeLength(side);
      double kneeVelocity = robot.getKneeVelocity(side);

      double desiredKneeLength = desiredKneeLengths.get(side).getValue();
      double desiredKneeVelocity = 0.0;
      double desiredHipAngle = 0.0;
      double desiredHipVelocity = 0.0;

      /* ----------------------------------- Compute and set knee force. ----------------------------------------------*/
      double feedBackKneeForce = 1300 * (desiredKneeLengths.get(side).getValue() - kneeLength) + 70 * (desiredKneeVelocity - kneeVelocity);
      robot.setKneeForce(side, feedBackKneeForce);

      double feedBackHipTorque = kdHip.getValue() * (0 - hipVelocity);
      desiredHipAngles.get(side).set(desiredHipAngle);
      robot.setHipTorque(side, feedBackHipTorque);
   }

   private SideDependentList<StateMachine<LIPMHoppingControllerBhavyansh.States, State>> setupStateMachines()
   {
      // States and Actions:
      StateMachineFactory<LIPMHoppingControllerBhavyansh.States, State> leftFactory = new StateMachineFactory<>(LIPMHoppingControllerBhavyansh.States.class);
      StateMachineFactory<LIPMHoppingControllerBhavyansh.States, State> rightFactory = new StateMachineFactory<>(LIPMHoppingControllerBhavyansh.States.class);

      leftFactory.setNamePrefix("leftState");
      rightFactory.setNamePrefix("rightState");

      leftFactory.setRegistry(registry);
      rightFactory.setRegistry(registry);

      leftFactory.buildClock(robot.getRobot().getYoTime());
      rightFactory.buildClock(robot.getRobot().getYoTime());

      // Left State Transitions:
      leftFactory.addTransition(LIPMHoppingControllerBhavyansh.States.SUPPORT,
                                LIPMHoppingControllerBhavyansh.States.FLIGHT,
                                new HeelOffGroundCondition(RobotSide.LEFT));
      leftFactory.addTransition(LIPMHoppingControllerBhavyansh.States.FLIGHT,
                                LIPMHoppingControllerBhavyansh.States.SWING,
                                new OppositeContactCondition(RobotSide.LEFT));
      leftFactory.addTransition(LIPMHoppingControllerBhavyansh.States.SWING,
                                LIPMHoppingControllerBhavyansh.States.SUPPORT,
                                new HeelOnGroundCondition(RobotSide.LEFT));

      // Right State Transitions:
      rightFactory.addTransition(LIPMHoppingControllerBhavyansh.States.SUPPORT,
                                 LIPMHoppingControllerBhavyansh.States.FLIGHT,
                                 new HeelOffGroundCondition(RobotSide.RIGHT));
      rightFactory.addTransition(LIPMHoppingControllerBhavyansh.States.FLIGHT,
                                 LIPMHoppingControllerBhavyansh.States.SWING,
                                 new OppositeContactCondition(RobotSide.RIGHT));
      rightFactory.addTransition(LIPMHoppingControllerBhavyansh.States.SWING,
                                 LIPMHoppingControllerBhavyansh.States.SUPPORT,
                                 new HeelOnGroundCondition(RobotSide.RIGHT));

      // Assemble the Left State Machine:
      leftFactory.addState(LIPMHoppingControllerBhavyansh.States.SUPPORT, new SupportState(RobotSide.LEFT));
      leftFactory.addState(LIPMHoppingControllerBhavyansh.States.SWING, new SwingState(RobotSide.LEFT));
      leftFactory.addState(LIPMHoppingControllerBhavyansh.States.FLIGHT, new FlightState(RobotSide.LEFT));

      // Assemble the Right State Machine:
      rightFactory.addState(LIPMHoppingControllerBhavyansh.States.SUPPORT, new SupportState(RobotSide.RIGHT));
      rightFactory.addState(LIPMHoppingControllerBhavyansh.States.SWING, new SwingState(RobotSide.RIGHT));
      rightFactory.addState(LIPMHoppingControllerBhavyansh.States.FLIGHT, new FlightState(RobotSide.RIGHT));

      return new SideDependentList<>(leftFactory.build(LIPMHoppingControllerBhavyansh.States.SUPPORT),
                                     rightFactory.build(LIPMHoppingControllerBhavyansh.States.SWING));
   }

   private class SupportState implements State
   {
      private final RobotSide robotSide;

      public SupportState(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      @Override
      public void onEntry()
      {

      }

      @Override
      public void doAction(double timeInState)
      {
         // Support Side:
         controlSupportLeg(robotSide);
      }

      @Override
      public void onExit(double timeInState)
      {

      }
   }

   private class SwingState implements State
   {
      private final RobotSide robotSide;

      public SwingState(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      @Override
      public void onEntry()
      {

      }

      @Override
      public void doAction(double timeInState)
      {
         controlSwingLeg(robotSide, timeInState);
      }

      @Override
      public void onExit(double timeInState)
      {

      }
   }

   private class FlightState implements State
   {
      private final RobotSide robotSide;

      public FlightState(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      @Override
      public void onEntry()
      {

      }

      @Override
      public void doAction(double timeInState)
      {
         controlFlightLeg(robotSide, timeInState);
      }

      @Override
      public void onExit(double timeInState)
      {

      }
   }

   private class HeelOffGroundCondition implements StateTransitionCondition
   {
      private final RobotSide robotSide;

      public HeelOffGroundCondition(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      @Override
      public boolean testCondition(double timeInCurrentState)
      {
         double timeDiff = t.getValue() - timeOfLastHeelOff.getValue();
         boolean fs =
               footZForces.get(robotSide).getValue() < 5.0 && footZForces.get(robotSide).getValue() > 2.0 && timeDiff > 0.1; // Eliminates switch bouncing.
         //         LogTools.info("Switch: {} {} {}", fs, footZForces.get(robotSide).getValue(), timeDiff);
         if (fs)
         {
            //            LogTools.info("Step: {} -> ComX: {}", robot.getCenterOfMassXDistanceFromSupportFoot(), numberOfStepsTaken.getValue());

            groundContactPositions.add(robot.getFootPosition(robotSide).getX());

            if (groundContactPositions.size() >= 2)
               lastStepLength.set(
                     groundContactPositions.get(groundContactPositions.size() - 1) - groundContactPositions.get(groundContactPositions.size() - 2));

            timeOfLastHeelOff.set(t.getValue());
            lastStanceTime.set(timeOfLastHeelOn.getValue() - timeOfLastHeelOff.getValue());

            desiredKneeLengths.get(robotSide.getOppositeSide()).set(0.9);
            desiredKneeLengths.get(robotSide).set(0.6);
         }
         return fs;
      }
   }

   private class HeelOnGroundCondition implements StateTransitionCondition
   {
      private final RobotSide robotSide;

      public HeelOnGroundCondition(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      @Override
      public boolean testCondition(double timeInCurrentState)
      {
         double timeDiff = t.getValue() - timeOfLastHeelOn.getValue();
         boolean fs = footZForces.get(robotSide).getValue() > 5.0 && timeDiff > 0.1; // Eliminates switch bouncing.
         //         LogTools.info("Switch: {} {} {}", fs, footZForces.get(robotSide).getValue(), timeDiff);
         if (fs)
         {
            //            LogTools.info("Step: {} -> ComX: {}", robot.getCenterOfMassXDistanceFromSupportFoot(), numberOfStepsTaken.getValue());

            groundContactPositions.add(robot.getFootPosition(robotSide).getX());

            if (groundContactPositions.size() >= 2)
               lastStepLength.set(
                     groundContactPositions.get(groundContactPositions.size() - 1) - groundContactPositions.get(groundContactPositions.size() - 2));

            timeOfLastHeelOn.set(t.getValue());
         }
         return fs;
      }
   }

   private class OppositeContactCondition implements StateTransitionCondition
   {
      private final RobotSide robotSide;

      public OppositeContactCondition(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      @Override
      public boolean testCondition(double timeInCurrentState)
      {
         double timeDiff = t.getValue() - timeOfLastHeelOn.getValue();
         boolean fs = footZForces.get(robotSide.getOppositeSide()).getValue() > 5.0 && timeDiff > 0.1; // Eliminates switch bouncing.
//         if (fs)
//         {
//            timeOfLastHeelOn.set(t.getValue());
//         }
         return fs;
      }
   }
}