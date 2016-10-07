
package us.ihmc.exampleSimulations.skippy;

import java.io.*;

import java.awt.Container;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import cern.colt.matrix.doublealgo.Formatter;
import us.ihmc.exampleSimulations.skippy.SkippyRobot.RobotType;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.controllers.PIDController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.math.filters.FilteredVelocityYoFrameVector;
import us.ihmc.robotics.math.filters.FilteredVelocityYoVariable;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.stateMachines.State;
import us.ihmc.robotics.stateMachines.StateMachine;
import us.ihmc.robotics.stateMachines.StateMachinesJPanel;
import us.ihmc.robotics.stateMachines.StateTransition;
import us.ihmc.robotics.stateMachines.StateTransitionCondition;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.gui.EventDispatchThreadHelper;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class SkippyController implements RobotController
{

   /**
    *
    * Outline of SkippyToDo: JUMP_FORWARD: If Skippy model is selected, robot
    * JUMP_SIDEWAYS: If Skippy model is selected, robot will jump/balance in x
    * direction (torso's rotation axis) BALANCE: If Skippy/Tippy model is
    * selected, robot will balance POSITION: If Tippy model is selected, robot
    * will balance with the help of LEG joint (not tested)
    *
    * Note: First three SkippyStatuses will allow model to balance according
    * to: q_d_hip: desired angle of TORSO q_d_shoulder: desired angle of
    * SHOULDER
    *
    */

   private enum SkippyToDo
   {
      JUMP_FORWARD, // change initialBodySidewaysLean in SkippyRobot.java to
      // 0.0
      BALANCE,
      POSITION
   }

   private enum States
   {
      BALANCE, PREPARE, LEAN, LIFTOFF, REPOSITION, RECOVER
   }

   private enum SkippyPlaneControlMode
   {
      BALANCE, POSITION
   }

   private StateMachine<States> stateMachine;

   private final YoVariableRegistry registry = new YoVariableRegistry("SkippyController");

   // tau_* is torque, q_* is position, qd_* is velocity for joint *
   // private DoubleYoVariable q_foot_X, q_hip, qHipIncludingOffset, qd_foot_X,
   // qd_hip, qd_shoulder;
   private final DoubleYoVariable k1, k2, k3, k4, k5, k6, k7, k8, angleToCoMInYZPlane, angleToCoMInXZPlane, angularVelocityToCoMYZPlane,
         angularVelocityToCoMXZPlane; // controller
   // gain
   // parameters
   private final DoubleYoVariable planarDistanceYZPlane, planarDistanceXZPlane;

   private final DoubleYoVariable alphaAngularVelocity;
   private final FilteredVelocityYoVariable angularVelocityToCoMYZPlane2, angularVelocityToCoMXZPlane2;

   private final YoFramePoint bodyLocation = new YoFramePoint("body", ReferenceFrame.getWorldFrame(), registry);

   private final ExternalForcePoint forceToCOM;
   private final YoFramePoint centerOfMass = new YoFramePoint("centerOfMass", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector centerOfMassVelocity = new YoFrameVector("centerOfMassVelocity", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector desiredReactionForce = new YoFrameVector("desiredReactionForce", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector actualReactionForce = new YoFrameVector("actualReactionForce", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector balancedReactionForce = new YoFrameVector("balancedReactionForce", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector angularMomentum = new YoFrameVector("angularMomentum", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector lastAngularMomentum = new YoFrameVector("lastAngularMomentum", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector linearMomentum = new YoFrameVector("linearMomentum", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector lastLinearMomentum = new YoFrameVector("lastLinearMomentum", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector rateOfChangeOfLinearMomentum = new YoFrameVector("rateOfChangeOfLinearMomentum", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector rateOfChangeOfAngularMomentum = new YoFrameVector("rateOfChangeOfAngularMomentum", ReferenceFrame.getWorldFrame(), registry);
   
   private final YoFrameVector strongForceUnitVector = new YoFrameVector("strongForceUnitVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector strongForceVector = new YoFrameVector("strongForceVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector weakForceVector = new YoFrameVector("weakForceVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector outOfPlaneReaction = new YoFrameVector("outOfPlaneReaction", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector desiredReactionInPlane = new YoFrameVector("inPlaneReaction", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector desiredReactionInPlaneUnitVector = new YoFrameVector("inPlaneReactionUnitVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector actualIcpToFootError = new YoFrameVector("actualIcpToFootError", ReferenceFrame.getWorldFrame(), registry);
   private final DoubleYoVariable outOfPlane = new DoubleYoVariable("outOfPlane", registry);
   private final DoubleYoVariable alphaBalance = new DoubleYoVariable("alphaBalance", registry);

   private final DoubleYoVariable reactionsAngleCosine = new DoubleYoVariable("reactionsAngleCosine", registry);

   private final YoFrameVector tauShoulderJoint = new YoFrameVector("tauShoulderJoint", ReferenceFrame.getWorldFrame(), registry);
   private final DoubleYoVariable tauOnShoulderJointAxis = new DoubleYoVariable("tauOnShoulderJointAxis", registry);

   private final YoFrameVector tauHipJoint = new YoFrameVector("tauHipJoint", ReferenceFrame.getWorldFrame(), registry);
   private final DoubleYoVariable tauOnHipJointAxis = new DoubleYoVariable("tauOnHipJointAxis", registry);

   private final YoFramePoint hipJointPosition = new YoFramePoint("hipJointPosition", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector hipJointUnitVector = new YoFrameVector("hipJointUnitVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector footToHipPositionVector = new YoFrameVector("hipToFootPositionVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint shoulderJointPosition = new YoFramePoint("shoulderJointPosition", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector shoulderJointUnitVector = new YoFrameVector("shoulderJointUnitVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector footToShoulderPositionVector = new YoFrameVector("shoulderToFootPositionVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector cmpToComPositionVector = new YoFrameVector("cmpToComPositionVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector cmpToComUnitVector = new YoFrameVector("cmpToComUnitVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector footToComPositionVector = new YoFrameVector("footToComPositionVector", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector footToCoMInBodyFrame;

   private final YoFramePoint actualICP = new YoFramePoint("actualICP", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint actualCMPFromDefinition = new YoFramePoint("actualCMPFromDefinition", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint desiredCMPFromICP = new YoFramePoint("desiredCMPFromICP", ReferenceFrame.getWorldFrame(), registry);
   private PIDController controllerCmpX;
   private PIDController controllerCmpY;
   private final YoFramePoint footLocation = new YoFramePoint("foot", ReferenceFrame.getWorldFrame(), registry);

   private final DoubleYoVariable robotMass = new DoubleYoVariable("robotMass", registry);
   private final DoubleYoVariable robotWeight = new DoubleYoVariable("robotWeight", registry);
   private final DoubleYoVariable qHipIncludingOffset = new DoubleYoVariable("qHipIncludingOffset", registry);
   private final DoubleYoVariable qDHipIncludingOffset = new DoubleYoVariable("qDHipIncludingOffset", registry);
   private final DoubleYoVariable qDShoulderIncludingOffset = new DoubleYoVariable("qDShoulderIncludingOffset", registry);
   private final DoubleYoVariable q_d_hip = new DoubleYoVariable("q_d_hip", registry);
   private final DoubleYoVariable qShoulderIncludingOffset = new DoubleYoVariable("qShoulderIncludingOffset", registry);
   private final DoubleYoVariable q_d_shoulder = new DoubleYoVariable("q_d_shoulder", registry);

   private final FramePoint tempFootLocation = new FramePoint(ReferenceFrame.getWorldFrame());
   private final FramePoint tempCoMLocation = new FramePoint(ReferenceFrame.getWorldFrame());
   private final FrameVector tempFootToCoM = new FrameVector(ReferenceFrame.getWorldFrame());

   // private final FramePoint lastCoMLocation = new
   // FramePoint(ReferenceFrame.getWorldFrame());
   // private final FrameVector footToLastCoMLocation = new
   // FrameVector(ReferenceFrame.getWorldFrame());
   private final DoubleYoVariable z0 = new DoubleYoVariable("z0", registry);
   private final DoubleYoVariable averageZ0 = new DoubleYoVariable("averageZ0", registry);
   private final BooleanYoVariable useICPController = new BooleanYoVariable("useICPController", registry);
   private final DoubleYoVariable kCapture = new DoubleYoVariable("kCapture", registry);
   private final DoubleYoVariable virtualMassOnFoot = new DoubleYoVariable("virtualMassOnFoot", registry);

   private final EnumYoVariable<SkippyToDo> skippyToDo = new EnumYoVariable<SkippyToDo>("SkippyToDo", registry, SkippyToDo.class);
   private final EnumYoVariable<SkippyPlaneControlMode> hipPlaneControlMode = new EnumYoVariable<SkippyPlaneControlMode>("hipPlaneControlMode", registry,
                                                                                                                         SkippyPlaneControlMode.class);
   private final EnumYoVariable<SkippyPlaneControlMode> shoulderPlaneControlMode = new EnumYoVariable<SkippyPlaneControlMode>("shoulderPlaneControlMode",
                                                                                                                              registry,
                                                                                                                              SkippyPlaneControlMode.class);

   private String name;
   private SkippyRobot robot;
   private RobotType robotType;

   private double legIntegralTermX = 0.0;
   private double legIntegralTermY = 0.0;
   private double hipIntegralTerm = 0.0;
   private double shoulderIntegralTerm = 0.0;

   double angularMomentumIntegralError = 0.0;
   double lastReactionForce = 0.0;
   int counterForZ0Average = 1;
   boolean printOnce = true;
   PrintWriter writer = null;
   boolean firstStick;

   public SkippyController(SkippyRobot robot, RobotType robotType, String name, double controlDT, YoGraphicsListRegistry yoGraphicsListRegistries)
   {
      this.name = name;
      this.robot = robot;
      this.robotType = robotType;

      useICPController.set(false);
      /*
       * z0 and KCapture
       */
      z0.set(1.216); // got from averaged CoM_Z during simulation
      averageZ0.set(0.0);
      kCapture.set(2.0);//0.9);
      robotMass.set(robot.getMass());
      robotWeight.set(robotMass.getDoubleValue() * Math.abs(robot.getGravityZ()));
      initializeControls();
      firstStick = true;
      /*
       * Set up a file for output method 1
       */
      try
      {
         writer = new PrintWriter("Output.txt", "UTF-8");
      }
      catch (FileNotFoundException | UnsupportedEncodingException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      writer.println("Output file created...");
      //
      footToCoMInBodyFrame = new YoFrameVector("footToCoMInBody", robot.updateAndGetBodyFrame(), registry);
      forceToCOM = new ExternalForcePoint("FORCETOCOM", robot);

      k1 = new DoubleYoVariable("k1", registry);
      k2 = new DoubleYoVariable("k2", registry);
      k3 = new DoubleYoVariable("k3", registry);
      k4 = new DoubleYoVariable("k4", registry);
      k5 = new DoubleYoVariable("k5", registry);
      k6 = new DoubleYoVariable("k6", registry);
      k7 = new DoubleYoVariable("k7", registry);
      k8 = new DoubleYoVariable("k8", registry);

      skippyToDo.set(SkippyToDo.JUMP_FORWARD);
      hipPlaneControlMode.set(SkippyPlaneControlMode.BALANCE);
      shoulderPlaneControlMode.set(SkippyPlaneControlMode.BALANCE);

      if (skippyToDo.getEnumValue() == SkippyToDo.BALANCE)
      {
         q_d_hip.set(0.6);
         q_d_shoulder.set(0.0);
      }
      else if (skippyToDo.getEnumValue() == SkippyToDo.JUMP_FORWARD)
      {
         q_d_hip.set(0.6);
         q_d_shoulder.set(0.0);
      }

      planarDistanceYZPlane = new DoubleYoVariable("planarDistanceYZPlane", registry);
      planarDistanceXZPlane = new DoubleYoVariable("planarDistanceXZPlane", registry);
      angleToCoMInYZPlane = new DoubleYoVariable("angleToCoMYZPlane", registry);
      angleToCoMInXZPlane = new DoubleYoVariable("angleToCoMXZPlane", registry);
      angularVelocityToCoMYZPlane = new DoubleYoVariable("angularVelocityToCoMYZPlane", registry);
      angularVelocityToCoMXZPlane = new DoubleYoVariable("angularVelocityToCoMXZPlane", registry);

      alphaAngularVelocity = new DoubleYoVariable("alphaAngularVelocity", registry);
      alphaAngularVelocity.set(0.8);
      angularVelocityToCoMYZPlane2 = new FilteredVelocityYoVariable("angularVelocityToCoMYZPlane2", "", alphaAngularVelocity, angleToCoMInYZPlane, controlDT,
                                                                    registry);
      angularVelocityToCoMXZPlane2 = new FilteredVelocityYoVariable("angularVelocityToCoMXZPlane2", "", alphaAngularVelocity, angleToCoMInXZPlane, controlDT,
                                                                    registry);

      if (skippyToDo.getEnumValue() != SkippyToDo.BALANCE && skippyToDo.getEnumValue() != SkippyToDo.POSITION || true)
      {
         stateMachine = new StateMachine<States>("stateMachine", "stateMachineTime", States.class, robot.t, registry);
         setUpStateMachines();
         createStateMachineWindow();
      }
      /*
       * CoM
       */
      YoGraphicPosition comPositionYoGraphic = new YoGraphicPosition("CoM", centerOfMass, 0.01, YoAppearance.Black(), GraphicType.BALL_WITH_CROSS);
      yoGraphicsListRegistries.registerYoGraphic("allGraphics", comPositionYoGraphic);
      yoGraphicsListRegistries.registerArtifact("allGraphics", comPositionYoGraphic.createArtifact());
      /*
       * ICP
       */
      YoGraphicPosition icpPositionYoGraphic = new YoGraphicPosition("ICP", actualICP, 0.01, YoAppearance.Blue(), GraphicType.BALL_WITH_ROTATED_CROSS);
      yoGraphicsListRegistries.registerYoGraphic("allGraphics", icpPositionYoGraphic);
      yoGraphicsListRegistries.registerArtifact("allGraphics", icpPositionYoGraphic.createArtifact());
      /*
       * Foot
       */
      YoGraphicPosition footPositionYoGraphic = new YoGraphicPosition("Foot", footLocation, 0.01, YoAppearance.DarkBlue(), GraphicType.BALL);
      yoGraphicsListRegistries.registerYoGraphic("allGraphics", footPositionYoGraphic);
      yoGraphicsListRegistries.registerArtifact("allGraphics", footPositionYoGraphic.createArtifact());
      /*
       * CMP from definition
       */
      YoGraphicPosition cmpPositionYoGraphic = new YoGraphicPosition("CMP from definition", actualCMPFromDefinition, 0.01, YoAppearance.Red(),
                                                                     GraphicType.BALL_WITH_ROTATED_CROSS);
      yoGraphicsListRegistries.registerYoGraphic("allGraphics", cmpPositionYoGraphic);
      yoGraphicsListRegistries.registerArtifact("allGraphics", cmpPositionYoGraphic.createArtifact());
      /*
       * CMP from ICP
       */
      YoGraphicPosition cmpFromIcpPositionYoGraphic = new YoGraphicPosition("CMP from ICP", desiredCMPFromICP, 0.0125, YoAppearance.DarkMagenta(),
                                                                            GraphicType.BALL_WITH_ROTATED_CROSS);
      yoGraphicsListRegistries.registerYoGraphic("actuallGraphicsalICP", cmpFromIcpPositionYoGraphic);
      yoGraphicsListRegistries.registerArtifact("allGraphics", cmpFromIcpPositionYoGraphic.createArtifact());
      /*
       * Desired reaction force
       */
      YoGraphicVector desiredGRFYoGraphic = new YoGraphicVector("desiredGRFYoGraphic", footLocation, desiredReactionForce, 0.05, YoAppearance.Orange(),
                                                                true);
      yoGraphicsListRegistries.registerYoGraphic("desiredReactionForce", desiredGRFYoGraphic);
      /*
       * Strong force
       */
      YoGraphicVector strongForceYoGraphic = new YoGraphicVector("strongForceYoGraphic", footLocation, strongForceVector, 0.05, YoAppearance.Glass(0.75),
                                                                true);
      yoGraphicsListRegistries.registerYoGraphic("strongForceVector", strongForceYoGraphic);
      /*
       * Balanced force
       */
      YoGraphicVector balancedForceYoGraphic = new YoGraphicVector("balancedForceYoGraphic", footLocation, balancedReactionForce, 0.05, YoAppearance.LightBlue(),
                                                                 true);
      yoGraphicsListRegistries.registerYoGraphic("balancedReactionForce", balancedForceYoGraphic);
      /*
       * Acquired reaction force
       */
      YoGraphicVector actualGRFYoGraphic = new YoGraphicVector("actualGRFYoGraphic", footLocation, actualReactionForce, 0.05, YoAppearance.DarkGreen(),
                                                               true);
      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), actualGRFYoGraphic);
      /*
       * CMP to CoM position vector
       */
//      YoGraphicVector cmpToComPositionVectorYoGraphic = new YoGraphicVector("cmpToComPositionVectorYoGraphic", desiredCMPFromICP, cmpToComPositionVector, 1.0,
//                                                                            YoAppearance.LightBlue(), true);
//      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), cmpToComPositionVectorYoGraphic);
      /*
       * Foot to hip joint position vector
       */
//      YoGraphicVector footToHipPositionVectorYoGraphic = new YoGraphicVector("footToHipPositionVectorYoGraphic", footLocation, footToHipPositionVector, 1.0,
//                                                                            YoAppearance.Red(), true);
//      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), footToHipPositionVectorYoGraphic);
      /*
       * Hip joint axis
       */
      YoGraphicVector hipJointAxisYoGraphic = new YoGraphicVector("hipJointAxisYoGraphic", hipJointPosition, hipJointUnitVector, 0.5, YoAppearance.AliceBlue(),
                                                                  true);
      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), hipJointAxisYoGraphic);
      /*
       * Hip joint torque
       */
      YoGraphicVector tauHipJointAxisYoGraphic = new YoGraphicVector("tauHipJointAxisYoGraphic", hipJointPosition, tauHipJoint, 0.05, YoAppearance.MediumSlateBlue(),
                                                                  true);
      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), tauHipJointAxisYoGraphic);
      /*
       * Foot to shoulder joint position vector
       */
//      YoGraphicVector footToShoulderPositionVectorYoGraphic = new YoGraphicVector("footToShoulderPositionVectorYoGraphic", footLocation, footToShoulderPositionVector, 1.0,
//                                                                             YoAppearance.Red(), true);
//      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), footToShoulderPositionVectorYoGraphic);
      /*
       * Shoulder joint axis
       */
      YoGraphicVector shoulderJointAxisYoGraphic = new YoGraphicVector("shoulderJointAxisYoGraphic", shoulderJointPosition, shoulderJointUnitVector, 0.5,
                                                                       YoAppearance.AliceBlue(), true);
      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), shoulderJointAxisYoGraphic);
      /*
       * Shoulder joint torque
       */
      YoGraphicVector tauShoulderJointYoGraphic = new YoGraphicVector("tauShoulderJointYoGraphic", shoulderJointPosition, tauShoulderJoint, 0.05,
                                                                       YoAppearance.MediumSlateBlue(), true);
      yoGraphicsListRegistries.registerYoGraphic(getClass().getSimpleName(), tauShoulderJointYoGraphic);

      initialize();

   }

   public void initializeControls()
   {
      /*
       * ICP_X
       */
      double somethingToDo=0.0;
      controllerCmpX = new PIDController("icpX", registry);
      controllerCmpX.setProportionalGain(1.5);//0.6);// 3);
      controllerCmpX.setDerivativeGain(0.0);// 05);
      controllerCmpX.setIntegralGain(0.0);// 01);
      // desiredBodyZ = new DoubleYoVariable("desiredBodyZ", registry);
      // desiredBodyZ.set(1.5);
      /*
       * ICP_Y
       */
      controllerCmpY = new PIDController("icpY", registry);
      controllerCmpY.setProportionalGain(1.5);// 3);
      controllerCmpY.setDerivativeGain(0.0);// 05);
      controllerCmpY.setIntegralGain(0.0);// 01);
      // desiredBodyZ = new DoubleYoVariable("desiredBodyZ", registry);
      // desiredBodyZ.set(1.5);
   }

   public void doControl()
   {
      //		writer.println(stateMachine.getStateYoVariableName()+skippyToDo+"  "+robot.getTime());
      actualCoMAndCoMVelocity();
      computeICP();
      actualGroundReactionForce();
      virtualMassOnFoot.set(actualReactionForce.getZ() / robot.getQdd_z().getDoubleValue());
      linearAndAngularMomentumRateOfChange();
      cmpFromDefinition();
//      cmpFromIcpDynamics();
      setParametersForControlModes();
      computeFootToCenterOfMassLocation();
      if (skippyToDo.getEnumValue() == SkippyToDo.BALANCE)
      {
         //			writer.println(stateMachine.getCurrentStateEnum()+"+-+-"+skippyToDo+"  "+robot.getTime());
         balanceControl();
      }
      else if (skippyToDo.getEnumValue() == SkippyToDo.POSITION)
      {
         //			writer.println(stateMachine.getStateYoVariableName()+"  "+skippyToDo+"  "+robot.getTime());
         positionControl();
      }
      else
      {
         //			writer.println(stateMachine.getStateYoVariableName()+"  "+skippyToDo+"  "+robot.getTime());
         jumpControl();
      }
   }

   private void setParametersForControlModes()
   {
      switch (shoulderPlaneControlMode.getEnumValue())
      {
      case BALANCE:
      {
         setShoulderPlaneParametersForBalancing();
         break;
      }

      case POSITION:
      {
         setShoulderPlaneParametersForPositionControl();
         break;
      }
      }

      switch (hipPlaneControlMode.getEnumValue())
      {
      case BALANCE:
      {
         setHipPlaneParametersForBalancing();
         break;
      }

      case POSITION:
      {
         setHipPlaneParametersForPositionControl();
         break;
      }
      }
   }

   /**
    * Torque on shoulder joint from reaction on foot
    * @param footReaction TODO
    * @param tauOnShoulderToPack TODO
    * @param tauOnShoulderAxisToPack TODO
    */
   public void tauOnShoulderJoint(YoFrameVector footReaction, YoFrameVector tauOnShoulderToPack, DoubleYoVariable tauOnShoulderAxisToPack)
   {
      tauOnShoulderToPack.cross(footToShoulderPositionVector, desiredReactionForce);
      /*
       * Shoulder joint torque projection modulus
       */
      double tempTauShoulderJointProjectionModulus = tauOnShoulderToPack.dot(shoulderJointUnitVector);
      tauOnShoulderAxisToPack.set(tempTauShoulderJointProjectionModulus);
   }

   /**
    * Torque on hip joint from reaction on foot
    * @param footReaction TODO
    * @param tauOnHipkToPack TODO
    * @param tauOnHipAxisToPack TODO
    */
   public void tauOnHipJoint(YoFrameVector footReaction, YoFrameVector tauOnHipkToPack, DoubleYoVariable tauOnHipAxisToPack)
   {
      tauOnHipkToPack.cross(footToHipPositionVector, footReaction);
      /*
       * Hip joint torque projection modulus
       */
      double tempTauHipJointProjectionModulus = tauOnHipkToPack.dot(hipJointUnitVector);
      tauOnHipAxisToPack.set(tempTauHipJointProjectionModulus);
   }

   /**
    * Hip and Shoulder to Foot joints position vectors
    */
   public void footToJointsPositionVectors()
   {
      Vector3d footToHipInWorld = new Vector3d();
      Vector3d footToShoulderInWorld = new Vector3d();
      Point3d footLocationInWorld = new Point3d();
      footLocationInWorld.set(robot.computeFootLocation());
      /*
       * Foot to hip position vector
       */
      robot.getHipJoint().getTranslationToWorld(footToHipInWorld);
      hipJointPosition.set(footToHipInWorld);
      footToHipPositionVector.set(hipJointPosition);
      footToHipPositionVector.sub(footLocationInWorld);
      /*
       * Shoulder to Foot position vector
       */
      robot.getShoulderJoint().getTranslationToWorld(footToShoulderInWorld);
      shoulderJointPosition.set(footToShoulderInWorld);
      footToShoulderPositionVector.set(footToShoulderInWorld);
      footToShoulderPositionVector.sub(footLocationInWorld);
   }

   /**
    * Hip and Shoulder joint unit vectors
    */
   private void hipAndShoulderUnitVectors()
   {
      Vector3d tempJointAxis = new Vector3d();
      RigidBodyTransform transformToWorld = new RigidBodyTransform();

      // hip:
      robot.getHipJoint().getJointAxis(tempJointAxis);
      robot.getHipJoint().getTransformToWorld(transformToWorld);
      transformToWorld.transform(tempJointAxis);
      hipJointUnitVector.set(tempJointAxis);
      hipJointUnitVector.normalize();

      // shoulder:
      robot.getShoulderJoint().getJointAxis(tempJointAxis);
      robot.getShoulderJoint().getTransformToWorld(transformToWorld);
      transformToWorld.transform(tempJointAxis);
      shoulderJointUnitVector.set(tempJointAxis);
      shoulderJointUnitVector.normalize();
   }

   /**
    * Ground reaction force from foot ground contact point
    */
   public void actualGroundReactionForce()
   {
      /*
       * Ground reaction force
       */
      Vector3d tempReactionForce = new Vector3d();
      /*
       * tempReactionForce is packed from a YoFrameVector in WorldFrame
       */
      robot.computeFootContactForce(tempReactionForce);
      actualReactionForce.set(tempReactionForce);
   }

   /**
    * CoM and CoM velocity
    */
   public void actualCoMAndCoMVelocity()
   {

      Point3d tempCOMPosition = new Point3d();
      Vector3d tempComVelocity = new Vector3d();
      Vector3d tempAngularMomentum = new Vector3d();
      /*
       * CoM and CoM velocity in WorldFrame
       */
      double totalMass = robot.computeCOMMomentum(tempCOMPosition, tempComVelocity, tempAngularMomentum);
      centerOfMass.set(tempCOMPosition);
      if (robot.getFootFS())
         averageZ0.set(((counterForZ0Average - 1) * averageZ0.getDoubleValue() + centerOfMass.getZ()) / counterForZ0Average);
      linearMomentum.set(tempComVelocity);
      angularMomentum.set(tempAngularMomentum);
      tempComVelocity.scale(1.0 / totalMass);
      centerOfMassVelocity.set(tempComVelocity);
   }

   /**
    * CMP to CoM position vector
    */
   public void positionVectorFomCmpToCom()
   {
      cmpToComPositionVector.set(centerOfMass);
      cmpToComPositionVector.sub(desiredCMPFromICP);
   }

   /**
    * Foot to CoM position vector
    */
   public void positionVectorFomFootToCom(YoFramePoint actualFootPosition)
   {
      Vector3d tempFootToComPositionVector = new Vector3d();
      Point3d footLocationInWorld = new Point3d();
      footLocationInWorld.set(robot.computeFootLocation());
      centerOfMass.get(tempFootToComPositionVector);
      footToComPositionVector.setVector(tempFootToComPositionVector);
      footToComPositionVector.sub(footLocationInWorld);
   }

   /**
    * CMP computed from ICP and CMP coupled dynamics from [2] (Eq. 4)
    */
   public void cmpFromIcpDynamics()
   {
      Point3d tempCMP = new Point3d();
      Point3d tempFootLocation = new Point3d();
      /*
       * CPM from ICP dynamics, Equation 3
       */
      actualICP.get(tempCMP); //                         Get actual ICP on tempCMP
      tempFootLocation = robot.computeFootLocation(); // Get foot location on tempFootLocation
      tempCMP.sub(tempFootLocation);   //                Get ICP-foot error on tempCMP
      tempCMP.scale(kCapture.getDoubleValue()); //       Multiply ICP-foot error by kCapture 
      tempCMP.add(actualICP.getPoint3dCopy());  //       Add actual ICP to complete Equation 3
      tempCMP.setZ(0.0);
      desiredCMPFromICP.set(tempCMP);
   }

   /**
    * CMP computed from its definition from [1] (Eq. 2 and 3) when foot is on
    * the ground
    */
   public void cmpFromDefinition()
   {
      if (robot.getFootFS())
      {
         actualCMPFromDefinition.setX((-rateOfChangeOfAngularMomentum.getY() + centerOfMass.getX() * actualReactionForce.getZ()
               - centerOfMass.getZ() * actualReactionForce.getX()) / actualReactionForce.getZ());
         actualCMPFromDefinition.setY((-rateOfChangeOfAngularMomentum.getX() + centerOfMass.getY() * actualReactionForce.getZ()
               - centerOfMass.getZ() * actualReactionForce.getY()) / actualReactionForce.getZ());
         actualCMPFromDefinition.setZ(0.0);
      }

   }

   /**
   * Compute rate of change (ROC) of CoM linear and angular momentum
   */
   public void linearAndAngularMomentumRateOfChange()
   {
      double deltaT = (double) SkippySimulation.DT;
      rateOfChangeOfLinearMomentum.set(linearMomentum);
      rateOfChangeOfLinearMomentum.sub(lastLinearMomentum);
      rateOfChangeOfLinearMomentum.scale(1 / deltaT);
      /*
       * Compute rate of change of CoM angular momentum
       */
      rateOfChangeOfAngularMomentum.set(angularMomentum);
      rateOfChangeOfAngularMomentum.sub(lastAngularMomentum);
      rateOfChangeOfAngularMomentum.scale(1 / deltaT);
      /*
       * Atualize last angular and linear momentum
       */
      lastLinearMomentum.set(linearMomentum);
      lastAngularMomentum.set(angularMomentum);
   }

   /*
    * Compute ICP
    */
   private void computeICP()
   {
      double w0 = Math.sqrt(z0.getDoubleValue() / Math.abs(robot.getGravityt()));
      actualICP.scaleAdd(w0, centerOfMassVelocity, centerOfMass);
      actualICP.setZ(0.0);
   }

   private void computeFootToCenterOfMassLocation()
   {
      ReferenceFrame bodyFrame = robot.updateAndGetBodyFrame();

      FramePoint bodyPoint = new FramePoint(bodyFrame);
      bodyPoint.changeFrame(ReferenceFrame.getWorldFrame());

      bodyLocation.set(bodyPoint);

      footLocation.set(robot.computeFootLocation());

      footLocation.getFrameTupleIncludingFrame(tempFootLocation);
      centerOfMass.getFrameTupleIncludingFrame(tempCoMLocation);

      // footToLastCoMLocation.set(tempFootToCoM.getVectorCopy());
      // lastCoMLocation.set(tempCoMLocation);

      tempFootLocation.changeFrame(bodyFrame);
      tempCoMLocation.changeFrame(bodyFrame);

      tempFootToCoM.setIncludingFrame(tempCoMLocation);
      tempFootToCoM.sub(tempFootLocation);

      footToCoMInBodyFrame.set(tempFootToCoM);
   }

   /**
    * jumpControl: Allows Skippy model to jump sideways or forward
    */
   private void jumpControl()
   {

      stateMachine.doAction();
      stateMachine.checkTransitionConditions();
      balanceControl();
   }

   /**
    * balanceControl: Balances Tippy/Skippy based on q_d_hip and q_d_shoulder
    */
   private void balanceControl()
   {
      /*
       * Allow switch between CMP or actual controllers
       */
      // if (useICPController.getBooleanValue()) {
      /*
       * Apply CMP controller to Skippy
       */
      double deltaT = SkippySimulation.DT;
      /*
       * ICP to Foot error
       */
//      double icpToFootErrorX = controllerCmpX.compute(actualICP.getX(), footLocation.getX(), 0.0, 0.0, deltaT);
//      double icpToFootErrorY = controllerCmpY.compute(actualICP.getY(), footLocation.getY(), 0.0, 0.0, deltaT);
//      actualIcpToFootError.set(icpToFootErrorX, icpToFootErrorY, 0.0);
      actualIcpToFootError.sub(footLocation, actualICP);
      /*
       * Compute CMP
       */
      desiredCMPFromICP.set(actualIcpToFootError);
      desiredCMPFromICP.scale(-kCapture.getDoubleValue());
      desiredCMPFromICP.add(actualICP);
      /*
       * Compute position vector from CMP to COM
       */
      positionVectorFomCmpToCom();
      /*
       * Unit vector from CMP to COM
       */
      cmpToComUnitVector.set(cmpToComPositionVector);
      cmpToComUnitVector.normalize();
      /*
       * This is weakForce: Compute reaction force. 
       */
      double reactionModulus = robot.getMass() * Math.abs(robot.getGravityZ()) / cmpToComUnitVector.getZ();
      desiredReactionForce.set(cmpToComUnitVector);
      desiredReactionForce.scale(reactionModulus);
      /*
       * Compute foot to joints position vectors
       */
      footToJointsPositionVectors();
      hipAndShoulderUnitVectors();
      /*
       * Compute tau on hip axis from weak force
       */
      tauOnHipJoint(desiredReactionForce, tauHipJoint, tauOnHipJointAxis);
      /*
       * Strong force direction unit vector
       */
      strongForceUnitVector.cross(footToHipPositionVector, hipJointUnitVector);
      strongForceUnitVector.normalize();
      /*
       * Weak force on strong force direction
       */
      weakForceVector.set(strongForceUnitVector);
      weakForceVector.scale(tauOnHipJointAxis.getDoubleValue()/footToHipPositionVector.length());
      /*
       * This is strongForce computation: Out off plane desired reaction force vector
       */
      outOfPlane.set(desiredReactionForce.dot(hipJointUnitVector));
      outOfPlaneReaction.set(hipJointUnitVector);
      outOfPlaneReaction.scale(outOfPlane.getDoubleValue());
      /*
       * In plane desired reaction force unit vector
       */
      desiredReactionInPlane.set(desiredReactionForce);
      desiredReactionInPlane.sub(outOfPlaneReaction);
      /*
       * To confirm the desiredReactionInPlane is in the plane
       */
//      double out = desiredReactionInPlane.dot(hipJointUnitVector);
//      System.out.println(out);
      desiredReactionInPlaneUnitVector.set(desiredReactionInPlane);
      desiredReactionInPlaneUnitVector.normalize();
      /*
       * Strong force unit vector projection in plane desired reaction force unit vector
       */
      double rho = strongForceUnitVector.dot(desiredReactionInPlaneUnitVector);
      /*
       * Strong force modulus
       */
      double strongForceModulus = desiredReactionInPlane.length()/rho;
      strongForceVector.set(strongForceUnitVector);
      strongForceVector.scale(strongForceModulus);
      /*
       * Balance the desired reaction 
       *    balancedReactionForce = (1-alpha)*strongForceVector + alpha*weakForceVector
       */
      weakForceVector.negate();
      alphaBalance.set(1.0); 
      balancedReactionForce.interpolate(strongForceVector, weakForceVector, alphaBalance.getDoubleValue());
      /*
       * Torque on hip and shoulder joints 
       */
      tauOnHipJoint(balancedReactionForce, tauHipJoint, tauOnHipJointAxis);
      tauOnShoulderJoint(desiredReactionForce, tauShoulderJoint, tauOnShoulderJointAxis);
      /*
       * Apply torques
       */
      if (robot.getFootFS())
      {
         robot.getHipJointTippy().setTau(tauOnHipJointAxis.getDoubleValue());
         robot.getShoulderJoint().setTau(-tauOnShoulderJointAxis.getDoubleValue());
      }
      // } else {
      /*
       * Apply torques from actual controller on Skippy
       */
//            applyTorqueToHip(q_d_hip.getDoubleValue());
//            applyTorqueToShoulder(q_d_shoulder.getDoubleValue());
      // }
   }

   private void applyTorqueToHip(double hipDesired)
   {
      /*
       * angular pos : angle created w/ com to groundpoint against vertical
       */

      // double footToComZ = centerOfMass.getZ()-footLocation.getZ();
      // double footToComY = centerOfMass.getY()-footLocation.getY();

      double footToComZ = footToCoMInBodyFrame.getZ();
      double footToComY = footToCoMInBodyFrame.getY();

      planarDistanceYZPlane.set(Math.sqrt(Math.pow(centerOfMass.getY() - footLocation.getY(), 2) + Math.pow(footToComZ, 2)));
      double angle = (Math.atan2(footToComY, footToComZ));
      angleToCoMInYZPlane.set(angle);

      /*
       * angular vel : angle created w/ com to groundpoint against vertical
       */
      Vector3d linearMomentum = new Vector3d();
      robot.computeLinearMomentum(linearMomentum);

      // 1: projection vector
      Vector3d componentPerpendicular = new Vector3d(0, 1, -centerOfMass.getY() / centerOfMass.getZ());
      componentPerpendicular.normalize();
      double angleVel = componentPerpendicular.dot(linearMomentum) / componentPerpendicular.length();
      angleVel = angleVel / robotMass.getDoubleValue();
      angularVelocityToCoMYZPlane.set(angleVel);
      angularVelocityToCoMYZPlane2.update();
      double angularVelocityForControl = angularVelocityToCoMYZPlane2.getDoubleValue();

      /*
       * angular pos/vel of hipjoint
       */
      double hipAngle = 0;
      double hipAngleVel = 0;
      double[] hipAngleValues = new double[2];

      hipAngleValues = calculateAnglePosAndDerOfHipJointTippy(robot.getHipJointTippy());

      hipAngle = hipAngleValues[0];
      hipAngleVel = hipAngleValues[1];
      qHipIncludingOffset.set((hipAngle));
      qDHipIncludingOffset.set(hipAngleVel);
      robot.getHipJointTippy().setTau(k1.getDoubleValue() * (0.0 - angle) + k2.getDoubleValue() * (0.0 - angularVelocityForControl)
            + k3.getDoubleValue() * (hipDesired - hipAngle) + k4.getDoubleValue() * (0.0 - hipAngleVel));
   }

   private Vector3d createVectorInDirectionOfHipJointAlongHip()
   {
      Vector3d rootJointCoordinates = new Vector3d();
      robot.getHipJointSkippy().getTranslationToWorld(rootJointCoordinates);
      Vector3d hipEndPointCoordinates = new Vector3d();
      robot.getGroundContactPoints().get(1).getPosition(hipEndPointCoordinates);
      rootJointCoordinates.sub(hipEndPointCoordinates);
      return rootJointCoordinates;
   }

   private void applyTorqueToShoulder(double shoulderDesired)
   {
      /*
       * angular pos : angle created w/ com to groundpoint against vertical
       */

      double footToComZ = footToCoMInBodyFrame.getZ();
      double footToComX = footToCoMInBodyFrame.getX();

      planarDistanceXZPlane.set(Math.sqrt(Math.pow(footToComX, 2) + Math.pow(footToComZ, 2)));
      double angle = (Math.atan2(footToComX, footToComZ));
      angleToCoMInXZPlane.set(angle);

      /*
       * angular vel : angle created w/ com to groundpoint against vertical
       */
      Vector3d linearMomentum = new Vector3d();
      robot.computeLinearMomentum(linearMomentum);

      // 1: projection vector
      Vector3d componentPerpendicular = new Vector3d(1, 0, -centerOfMass.getX() / centerOfMass.getZ());
      componentPerpendicular.normalize();
      double angleVel = componentPerpendicular.dot(linearMomentum) / componentPerpendicular.length();
      angleVel = angleVel / robotMass.getDoubleValue();

      // 2: not used
      // double angleVel = Math.pow(Math.pow(linearMomentum.getY(), 2) +
      // Math.pow(linearMomentum.getZ(), 2), 0.5)/robotMass;
      // angleVel = angleVel / planarDistanceYZPlane;

      // 3: average rate of change (buggy)
      // double angleVel = (angle - prevAngleHip) / SkippySimulation.DT;

      angularVelocityToCoMXZPlane.set(angleVel);
      angularVelocityToCoMXZPlane2.update();
      double angularVelocityForControl = angularVelocityToCoMXZPlane2.getDoubleValue();

      /*
       * angular pos/vel of hipjoint
       */
      double shoulderAngle = 0;
      double shoulderAngleVel = 0;
      double[] shoulderAngleValues = new double[2];

      shoulderAngleValues = calculateAnglePosAndDerOfShoulderJointTippy(robot.getShoulderJoint());

      shoulderAngle = shoulderAngleValues[0];
      shoulderAngleVel = shoulderAngleValues[1];
      qShoulderIncludingOffset.set((shoulderAngle));
      qDShoulderIncludingOffset.set(shoulderAngleVel);

      double shoulderAngleError = AngleTools.computeAngleDifferenceMinusPiToPi(shoulderDesired, shoulderAngle);
      robot.getShoulderJoint().setTau(k5.getDoubleValue() * Math.sin(0.0 - angle) + k6.getDoubleValue() * (0.0 - angularVelocityForControl)
            + k7.getDoubleValue() * (shoulderAngleError) + k8.getDoubleValue() * (0.0 - shoulderAngleVel));

   }

   private Vector3d createVectorInDirectionOfShoulderJointAlongShoulder()
   {
      Vector3d shoulderJointCoordinates = new Vector3d();
      robot.getShoulderJoint().getTranslationToWorld(shoulderJointCoordinates);
      Vector3d shoulderEndPointCoordinates = new Vector3d();
      robot.getGroundContactPoints().get(2).getPosition(shoulderEndPointCoordinates);
      shoulderEndPointCoordinates.sub(shoulderJointCoordinates);
      return shoulderEndPointCoordinates;
   }

   private double[] calculateAnglePosAndDerOfHipJointTippy(PinJoint joint)
   {
      double[] finale = new double[2];

      // for different definition of hipJointAngle (angle b/w hipJoint and
      // vertical (z axis) )
      // double firstAngle =
      // robot.getLegJoint().getQ().getDoubleValue()%(Math.PI*2);
      // if(firstAngle>Math.PI)
      // firstAngle = (Math.PI*2-firstAngle)*-1;
      // double angle =
      // (joint.getQ().getDoubleValue())%(Math.PI*2)+firstAngle;
      // if(angle > Math.PI)
      // angle = angle - Math.PI*2;

      double angle = joint.getQYoVariable().getDoubleValue();
      double angleVel = joint.getQDYoVariable().getDoubleValue();
      finale[0] = angle;
      finale[1] = (angleVel);
      return finale;
   }

   private double[] calculateAnglePosAndDerOfHipJointSkippy(FloatingJoint joint)
   {
      /*
       * Using groundcontact points to create vectors
       */
      double[] finale = new double[2];

      Vector3d verticalVector = new Vector3d(0.0, 0.0, 1.0);
      Vector3d floatVector = createVectorInDirectionOfHipJointAlongHip();
      verticalVector.setX(0.0);
      floatVector.setX(0.0); // angle wrt yz plane only

      double cosineTheta = (floatVector.dot(verticalVector) / (floatVector.length() * verticalVector.length()));
      double angle = Math.acos(cosineTheta);
      if (floatVector.getY() < 0)
         angle = angle * -1;
      /*
       * // increases same speed wrt angle diff. between root and leg
       */
      double angleVel = robot.getLegJoint().getQDYoVariable().getDoubleValue();
      finale[0] = angle;
      finale[1] = (angleVel);
      return finale;
   }

   private double[] calculateAnglePosAndDerOfShoulderJointTippy(PinJoint joint)
   {
      double[] finale = new double[2];

      // for different definition of shoulderJointAngle (angle b/w
      // shoulderJoint and vertical (z-axis) )
      // double firstAngle = 0;
      //
      // firstAngle =
      // (robot.getLegJoint().getSecondJoint().getQ().getDoubleValue())%(Math.PI*2);
      // if(firstAngle>Math.PI)
      // firstAngle = (Math.PI*2-firstAngle)*-1;
      // double angle =
      // (joint.getQ().getDoubleValue())%(Math.PI*2)+firstAngle;
      // if(angle > Math.PI)
      // angle = angle - Math.PI*2;

      double angle = joint.getQYoVariable().getDoubleValue();
      double angleVel = joint.getQDYoVariable().getDoubleValue();

      finale[0] = angle;
      finale[1] = angleVel;
      return finale;
   }

   private double[] calculateAnglePosAndDerOfShoulderJointSkippy(PinJoint joint)
   {
      double[] finale = new double[2];

      Vector3d horizontalVector = new Vector3d(1.0, 0.0, 0.0);
      Vector3d shoulderVector = createVectorInDirectionOfShoulderJointAlongShoulder();
      horizontalVector.setY(0);
      shoulderVector.setY(0);

      double cosineTheta = (horizontalVector.dot(shoulderVector) / (horizontalVector.length() * shoulderVector.length()));
      double angle = Math.abs(Math.acos(cosineTheta));

      Vector3d shoulderJointPosition = new Vector3d();
      joint.getTranslationToWorld(shoulderJointPosition);

      if (robot.getGroundContactPoints().get(2).getZ() < shoulderJointPosition.getZ())
         angle = angle * -1;

      double angleVel = robot.getShoulderJoint().getQDYoVariable().getDoubleValue();
      finale[0] = angle;
      finale[1] = angleVel;
      return finale;
   }

   private double fromRadiansToDegrees(double radians)
   {
      return radians * 180 / Math.PI;
   }

   /**
    * positionControl: positions Tippy model in whatever position desired
    * (specified within method)
    */
   private void positionControl()
   {
      double desiredX = 0.0;
      double desiredY = Math.PI / 6;
      double desiredHip = -2 * Math.PI / 6;
      double desiredShoulder = 0.0;

      positionJointsBasedOnError(robot.getLegJoint(), desiredX, legIntegralTermX, 20000, 150, 2000, true);
      positionJointsBasedOnError(robot.getLegJoint().getSecondJoint(), desiredY, legIntegralTermY, 20000, 150, 2000, false);
      positionJointsBasedOnError(robot.getHipJointTippy(), desiredHip, hipIntegralTerm, 20000, 150, 2000, false);
      positionJointsBasedOnError(robot.getShoulderJoint(), desiredShoulder, shoulderIntegralTerm, 20000, 150, 2000, false);
   }

   public void positionJointsBasedOnError(PinJoint joint, double desiredValue, double integralTerm, double positionErrorGain, double integralErrorGain,
                                          double derivativeErrorGain, boolean isBasedOnWorldCoordinates)
   {
      // try to change position based on angular position wrt xyz coordinate
      // system
      Matrix3d rotationMatrixForWorld = new Matrix3d();
      joint.getRotationToWorld(rotationMatrixForWorld);
      double rotationToWorld = Math.asin((rotationMatrixForWorld.getM21()));
      // if(rotationMatrixForWorld.getM11()<0)
      // rotationToWorld = rotationToWorld * -1;
      if (isBasedOnWorldCoordinates)
      {
         // System.out.println(joint.getName() + " " +
         // (joint.getQ().getDoubleValue()) + " " + rotationToWorld);
      }
      else
         rotationToWorld = joint.getQYoVariable().getDoubleValue();

      double positionError = (positionErrorGain) * ((desiredValue - rotationToWorld));
      integralTerm += (integralErrorGain) * positionError * SkippySimulation.DT;
      double derivativeError = (derivativeErrorGain) * (0 - joint.getQDYoVariable().getDoubleValue());
      joint.setTau(positionError + integralTerm + derivativeError);
      // System.out.print(joint.getName() + ": " +
      // (joint.getQ().getDoubleValue() - desiredValue));
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public void initialize()
   {
   }

   public String getDescription()
   {
      return getName();
   }

   /*
    * STATE MACHINES
    */

   private void setUpStateMachines()
   {
      // states
      State<States> balanceState = new BalanceState(skippyToDo.getEnumValue());
      State<States> prepareState = new PrepareState(skippyToDo.getEnumValue());
      State<States> leanState = new LeanState(skippyToDo.getEnumValue());
      State<States> liftoffState = new LiftoffState(skippyToDo.getEnumValue());
      State<States> repositionState = new RepositionState(skippyToDo.getEnumValue());
      State<States> recoverState = new RecoverState(skippyToDo.getEnumValue());

      // transitions
      StateTransitionCondition balanceToPrepareTransitionCondition = new BalanceToPrepareTransitionCondition();
      StateTransitionCondition prepareToLeanTransitionCondition = new PrepareToLeanTransitionCondition(skippyToDo.getEnumValue());
      StateTransitionCondition leanToLiftoffTransitionCondition = new LeanToLiftoffTransitionCondition(skippyToDo.getEnumValue());
      StateTransitionCondition liftoffToRepositionTransitionCondition = new LiftoffToRepositionTransitionCondition(skippyToDo.getEnumValue());
      StateTransitionCondition repositionToRecoverTransitionCondition = new RepositionToRecoverTransitionCondition(skippyToDo.getEnumValue());
      StateTransitionCondition recoverToBalanceTransitionCondition = new RecoverToBalanceTransitionCondition(skippyToDo.getEnumValue());

      StateTransition<States> balanceToPrepare = new StateTransition<States>(States.PREPARE, balanceToPrepareTransitionCondition);
      balanceState.addStateTransition(balanceToPrepare);

      StateTransition<States> prepareToLean = new StateTransition<States>(States.LEAN, prepareToLeanTransitionCondition);
      prepareState.addStateTransition(prepareToLean);

      StateTransition<States> leanToLiftoff = new StateTransition<States>(States.LIFTOFF, leanToLiftoffTransitionCondition);
      leanState.addStateTransition(leanToLiftoff);

      StateTransition<States> liftoffToReposition = new StateTransition<States>(States.REPOSITION, liftoffToRepositionTransitionCondition);
      liftoffState.addStateTransition(liftoffToReposition);

      StateTransition<States> repositionToRecover = new StateTransition<States>(States.RECOVER, repositionToRecoverTransitionCondition);
      repositionState.addStateTransition(repositionToRecover);

      StateTransition<States> recoverToBalance = new StateTransition<States>(States.BALANCE, recoverToBalanceTransitionCondition);
      recoverState.addStateTransition(recoverToBalance);

      stateMachine.addState(balanceState);
      stateMachine.addState(prepareState);
      stateMachine.addState(leanState);
      stateMachine.addState(liftoffState);
      stateMachine.addState(repositionState);
      stateMachine.addState(recoverState);

      stateMachine.setCurrentState(States.BALANCE);
   }

   public void createStateMachineWindow()
   {
      EventDispatchThreadHelper.invokeAndWait(new Runnable()
      {
         public void run()
         {
            createStateMachineWindowLocal();
         }
      });
   }

   public void createStateMachineWindowLocal()
   {
      JFrame frame = new JFrame("Skippy Jump State Machine");
      Container contentPane = frame.getContentPane();
      contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

      StateMachinesJPanel<States> stateMachinePanel = new StateMachinesJPanel<States>(stateMachine, true);

      frame.getContentPane().add(stateMachinePanel);

      frame.pack();
      frame.setSize(450, 300);
      frame.setAlwaysOnTop(false);
      frame.setVisible(true);

      stateMachine.attachStateChangedListener(stateMachinePanel);
   }

   private void setHipPlaneParametersForPositionControl()
   {
      k1.set(0.0);
      k2.set(0.0);
      k3.set(300.0);
      k4.set(30.0);
   }

   private void setShoulderPlaneParametersForPositionControl()
   {
      k5.set(0.0);
      k6.set(0.0);
      k7.set(300.0);
      k8.set(30.0);
   }

   private void setShoulderPlaneParametersForBalancing()
   {
      k5.set(-1900);
      k6.set(-490.0);
      k7.set(-60.0);
      k8.set(-45.0);
   }

   private void setHipPlaneParametersForBalancing()
   {
      k1.set(-3600.0);
      k2.set(-1500.0);
      k3.set(-170.0);
      k4.set(-130.0);
   }

   public class BalanceToPrepareTransitionCondition implements StateTransitionCondition
   {
      public BalanceToPrepareTransitionCondition()
      {
      }

      public boolean checkCondition()
      {
         if (skippyToDo.getEnumValue() == SkippyToDo.JUMP_FORWARD)
         {
            double time = stateMachine.timeInCurrentState();
            return time >= 4.0;
         }
         else
            return false;
      }
   }

   public class PrepareToLeanTransitionCondition implements StateTransitionCondition
   {

      private final SkippyToDo direction;

      public PrepareToLeanTransitionCondition(SkippyToDo direction)
      {
         this.direction = direction;
      }

      public boolean checkCondition()
      {
         if (direction == SkippyToDo.JUMP_FORWARD)
         {
            double time = stateMachine.timeInCurrentState();
            return time < 7.01 && time > 6.99;
         }
         else
            return false;
      }
   }

   public class LeanToLiftoffTransitionCondition implements StateTransitionCondition
   {

      private final SkippyToDo direction;

      public LeanToLiftoffTransitionCondition(SkippyToDo direction)
      {
         this.direction = direction;
      }

      public boolean checkCondition()
      {
         if (direction == SkippyToDo.JUMP_FORWARD)
         {
            double time = stateMachine.timeInCurrentState();
            return true; // time > 0.2;
         }
         else
            return false;
      }
   }

   public class LiftoffToRepositionTransitionCondition implements StateTransitionCondition
   {

      private final SkippyToDo direction;

      public LiftoffToRepositionTransitionCondition(SkippyToDo direction)
      {
         this.direction = direction;
      }

      public boolean checkCondition()
      {
         if (direction == SkippyToDo.JUMP_FORWARD)
         {
            double time = stateMachine.timeInCurrentState();
            return time < 0.36 && time > 0.35;
         }
         else
            return false;

         // Vector3d angMom = new Vector3d();
         // robot.computeAngularMomentum(angMom);
         // return angMom.length() < 0.01;

      }
   }

   public class RepositionToRecoverTransitionCondition implements StateTransitionCondition
   {

      private final SkippyToDo direction;

      public RepositionToRecoverTransitionCondition(SkippyToDo direction)
      {
         this.direction = direction;
      }

      public boolean checkCondition()
      {
         double time = stateMachine.timeInCurrentState();
         return time < 0.60 && time > 0.59;
      }
   }

   public class RecoverToBalanceTransitionCondition implements StateTransitionCondition
   {

      private final SkippyToDo direction;

      public RecoverToBalanceTransitionCondition(SkippyToDo direction)
      {
         this.direction = direction;
      }

      public boolean checkCondition()
      {
         if (direction == SkippyToDo.JUMP_FORWARD)
         {
            double time = stateMachine.timeInCurrentState();
            return time < 4.01 && time > 3.99;
         }
         return false;
      }
   }

   private class BalanceState extends State<States>
   {

      private final SkippyToDo direction;

      public BalanceState(SkippyToDo direction)
      {
         super(States.BALANCE);
         this.direction = direction;
      }

      public void doAction()
      {
         q_d_hip.set(0.6);
      }

      public void doTransitionIntoAction()
      {
         q_d_hip.set(0.6);
      }

      public void doTransitionOutOfAction()
      {

      }
   }

   private class PrepareState extends State<States>
   {

      private final SkippyToDo direction;

      public PrepareState(SkippyToDo direction)
      {
         super(States.PREPARE);
         this.direction = direction;
      }

      public void doAction()
      {
      }

      public void doTransitionIntoAction()
      {
         if (direction == SkippyToDo.JUMP_FORWARD)
         {
            q_d_hip.set(1.6);
         }
      }

      public void doTransitionOutOfAction()
      {

      }
   }

   private class LeanState extends State<States>
   {
      private final SkippyToDo direction;

      public LeanState(SkippyToDo direction)
      {
         super(States.LEAN);
         this.direction = direction;
      }

      public void doAction()
      {
      }

      public void doTransitionIntoAction()
      {
         if (direction == SkippyToDo.JUMP_FORWARD)
         {
            hipPlaneControlMode.set(SkippyPlaneControlMode.POSITION);
            q_d_hip.set(1.4);

         }
      }

      public void doTransitionOutOfAction()
      {

      }
   }

   private class LiftoffState extends State<States>
   {

      private final SkippyToDo direction;

      public LiftoffState(SkippyToDo direction)
      {
         super(States.LIFTOFF);
         this.direction = direction;
      }

      public void doAction()
      {

      }

      public void doTransitionIntoAction()
      {
         q_d_hip.set(0.45);
      }

      public void doTransitionOutOfAction()
      {

      }
   }

   private class RepositionState extends State<States>
   {

      private final SkippyToDo direction;

      public RepositionState(SkippyToDo direction)
      {
         super(States.REPOSITION);
         this.direction = direction;
      }

      public void doAction()
      {
      }

      public void doTransitionIntoAction()
      {
         q_d_hip.set(-1.3);
      }

      public void doTransitionOutOfAction()
      {

      }
   }

   private class RecoverState extends State<States>
   {

      private final SkippyToDo direction;

      public RecoverState(SkippyToDo direction)
      {
         super(States.RECOVER);
         this.direction = direction;
      }

      public void doAction()
      {
      }

      public void doTransitionIntoAction()
      {
         hipPlaneControlMode.set(SkippyPlaneControlMode.BALANCE);
         shoulderPlaneControlMode.set(SkippyPlaneControlMode.BALANCE);

         q_d_hip.set(-0.9);
         q_d_shoulder.set(0.0);
         robot.glueDownToGroundPoint.setForce(0.0, 0.0, -1450.0);
      }

      public void doTransitionOutOfAction()
      {

      }
   }

   public void closeFile()
   {
      System.out.println("Closed the file");
      writer.close();
   }
}
