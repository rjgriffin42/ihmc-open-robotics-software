package us.ihmc.robotiq.simulatedHand;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.communication.packets.dataobjects.FingerState;
import us.ihmc.robotiq.RobotiqGraspMode;
import us.ihmc.robotiq.model.RobotiqHandModel.RobotiqHandJointNameMinimal;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.utilities.humanoidRobot.partNames.FingerName;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.math.trajectories.YoPolynomial;
import us.ihmc.yoUtilities.stateMachines.State;
import us.ihmc.yoUtilities.stateMachines.StateMachine;
import us.ihmc.yoUtilities.stateMachines.StateTransition;
import us.ihmc.yoUtilities.stateMachines.StateTransitionCondition;

public class IndividualRobotiqHandController implements RobotController
{
   enum GraspState
   {
      BASIC_OPEN, BASIC_CLOSED,
      PINCH_OPEN, PINCH_CLOSED,
      HOOK
   }
   
   private final boolean DEBUG = false;
   
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry;

   private final RobotSide robotSide;

   private final List<RobotiqHandJointNameMinimal> indexJointEnumValues = new ArrayList<RobotiqHandJointNameMinimal>();
   private final List<RobotiqHandJointNameMinimal> middleJointEnumValues = new ArrayList<RobotiqHandJointNameMinimal>();
   private final List<RobotiqHandJointNameMinimal> thumbJointEnumValues = new ArrayList<RobotiqHandJointNameMinimal>();

   private final EnumMap<RobotiqHandJointNameMinimal, OneDegreeOfFreedomJoint> indexJoints = new EnumMap<>(RobotiqHandJointNameMinimal.class);
   private final EnumMap<RobotiqHandJointNameMinimal, OneDegreeOfFreedomJoint> middleJoints = new EnumMap<>(RobotiqHandJointNameMinimal.class);
   private final EnumMap<RobotiqHandJointNameMinimal, OneDegreeOfFreedomJoint> thumbJoints = new EnumMap<>(RobotiqHandJointNameMinimal.class);

   private final List<OneDegreeOfFreedomJoint> allFingerJoints = new ArrayList<>();

   private final YoPolynomial yoPolynomial;
   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable startTrajectoryTime, currentTrajectoryTime, endTrajectoryTime, trajectoryTime;
   private final BooleanYoVariable hasTrajectoryTimeChanged, isStopped;
   private final LinkedHashMap<OneDegreeOfFreedomJoint, DoubleYoVariable> initialDesiredAngles = new LinkedHashMap<>();
   private final LinkedHashMap<OneDegreeOfFreedomJoint, DoubleYoVariable> finalDesiredAngles = new LinkedHashMap<>();
   private final LinkedHashMap<OneDegreeOfFreedomJoint, DoubleYoVariable> desiredAngles = new LinkedHashMap<>();
   
   private final EnumYoVariable<RobotiqGraspMode> graspMode;
   private final EnumYoVariable<RobotiqGraspMode> desiredGraspMode;
   private final EnumYoVariable<FingerState> fingerState;
   private final EnumYoVariable<FingerState> desiredFingerState;
   
   private StateMachine<GraspState> stateMachine;

   public IndividualRobotiqHandController(RobotSide robotSide, DoubleYoVariable yoTime, DoubleYoVariable trajectoryTime, SDFRobot simulatedRobot,
         YoVariableRegistry parentRegistry)
   {
      String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
      registry = new YoVariableRegistry(sidePrefix + name);
      parentRegistry.addChild(registry);
      this.robotSide = robotSide;
      this.yoTime = yoTime;

      for (RobotiqHandJointNameMinimal jointEnum : RobotiqHandJointNameMinimal.values)
      {
         String jointName = jointEnum.getJointName(robotSide);
         OneDegreeOfFreedomJoint fingerJoint = simulatedRobot.getOneDegreeOfFreedomJoint(jointName);

         DoubleYoVariable initialDesiredAngle = new DoubleYoVariable("q_d_initial_" + jointName, registry);
         initialDesiredAngles.put(fingerJoint, initialDesiredAngle);

         DoubleYoVariable finalDesiredAngle = new DoubleYoVariable("q_d_final_" + jointName, registry);
         finalDesiredAngles.put(fingerJoint, finalDesiredAngle);

         DoubleYoVariable desiredAngle = new DoubleYoVariable("q_d_" + jointName, registry);
         desiredAngles.put(fingerJoint, desiredAngle);

         allFingerJoints.add(fingerJoint);

         switch (jointEnum.getFinger(robotSide))
         {
         case INDEX:
            indexJoints.put(jointEnum, fingerJoint);
            indexJointEnumValues.add(jointEnum);
            break;

         case MIDDLE:
            middleJoints.put(jointEnum, fingerJoint);
            middleJointEnumValues.add(jointEnum);
            break;

         case THUMB:
            thumbJoints.put(jointEnum, fingerJoint);
            thumbJointEnumValues.add(jointEnum);
            break;

         default:
            break;
         }
      }

      startTrajectoryTime = new DoubleYoVariable(sidePrefix + "StartTrajectoryTime", registry);
      currentTrajectoryTime = new DoubleYoVariable(sidePrefix + "CurrentTrajectoryTime", registry);
      endTrajectoryTime = new DoubleYoVariable(sidePrefix + "EndTrajectoryTime", registry);
      this.trajectoryTime = trajectoryTime;
      hasTrajectoryTimeChanged = new BooleanYoVariable(sidePrefix + "HasTrajectoryTimeChanged", registry);
      isStopped = new BooleanYoVariable(sidePrefix + "IsStopped", registry);
      isStopped.set(false);
      trajectoryTime.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            hasTrajectoryTimeChanged.set(true);
         }
      });
      yoPolynomial = new YoPolynomial(sidePrefix + name, 4, registry);
      yoPolynomial.setCubic(0.0, trajectoryTime.getDoubleValue(), 0.0, 0.0, 1.0, 0.0);
      
      graspMode = new EnumYoVariable<>(sidePrefix + "RobotiqGraspMode", registry, RobotiqGraspMode.class);
      graspMode.set(RobotiqGraspMode.BASIC_MODE);
      desiredGraspMode = new EnumYoVariable<>(sidePrefix + "RobotiqDesiredGraspMode", registry, RobotiqGraspMode.class);
      fingerState = new EnumYoVariable<>(sidePrefix + "RobotiqFingerState", registry, FingerState.class);
      fingerState.set(FingerState.OPEN);
      desiredFingerState = new EnumYoVariable<>(sidePrefix + "RobotiqDesiredFingerState", registry, FingerState.class);
      
//      setupStateMachine();
   }
   
   private void setupStateMachine()
   {
      stateMachine = new StateMachine<>("RobotiqGraspStateMachine", "FingerTrajectoryTime", GraspState.class, yoTime, registry);

      State<GraspState> stateOpenBasicGrip = new OpenBasicGrip();
      State<GraspState> stateClosedBasicGrip = new ClosedBasicGrip();
      State<GraspState> stateOpenPinchGrip = new OpenPinchGrip();
      State<GraspState> stateClosedPinchGrip = new ClosedPinchGrip();
      State<GraspState> stateHookGrip = new HookGrip();
      
      stateMachine.addState(stateOpenBasicGrip);
      stateMachine.addState(stateClosedBasicGrip);
      stateMachine.addState(stateOpenPinchGrip);
      stateMachine.addState(stateClosedPinchGrip);
      stateMachine.addState(stateHookGrip);
      stateMachine.setCurrentState(GraspState.BASIC_OPEN);
      
      StateTransitionCondition openBasicGripCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {
            return desiredGraspMode.getEnumValue().equals(RobotiqGraspMode.BASIC_MODE) && desiredFingerState.getEnumValue().equals(FingerState.OPEN);
         }
      };
      
      StateTransitionCondition closedBasicGripCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {
            return desiredGraspMode.getEnumValue().equals(RobotiqGraspMode.BASIC_MODE) && desiredFingerState.getEnumValue().equals(FingerState.CLOSE);
         }
      };
      
      StateTransitionCondition openPinchGripCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {
            return desiredGraspMode.getEnumValue().equals(RobotiqGraspMode.PINCH_MODE) && desiredFingerState.getEnumValue().equals(FingerState.OPEN);
         }
      };
      
      StateTransitionCondition closedPinchGripCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {
            return desiredGraspMode.getEnumValue().equals(RobotiqGraspMode.PINCH_MODE) && desiredFingerState.getEnumValue().equals(FingerState.OPEN);
         }
      };
      
      StateTransitionCondition hookGripCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {
            return desiredFingerState.getEnumValue().equals(FingerState.HOOK);
         }
      };
      
      //BASIC_OPEN
      stateOpenBasicGrip.addStateTransition(new StateTransition<GraspState>(GraspState.BASIC_CLOSED, closedBasicGripCondition));
      stateOpenBasicGrip.addStateTransition(new StateTransition<GraspState>(GraspState.PINCH_OPEN, openPinchGripCondition));
      stateOpenBasicGrip.addStateTransition(new StateTransition<GraspState>(GraspState.HOOK, hookGripCondition));
      
      //BASIC_CLOSED
      stateClosedBasicGrip.addStateTransition(new StateTransition<GraspState>(GraspState.BASIC_OPEN, openBasicGripCondition));
      
      //PINCH_OPEN
      stateOpenPinchGrip.addStateTransition(new StateTransition<GraspState>(GraspState.PINCH_CLOSED, closedPinchGripCondition));
      stateOpenPinchGrip.addStateTransition(new StateTransition<GraspState>(GraspState.BASIC_OPEN, openBasicGripCondition));
      stateOpenPinchGrip.addStateTransition(new StateTransition<GraspState>(GraspState.HOOK, hookGripCondition));
      
      //PINCH_CLOSED
      stateClosedPinchGrip.addStateTransition(new StateTransition<GraspState>(GraspState.PINCH_OPEN, openPinchGripCondition));
      
      //HOOK
      stateHookGrip.addStateTransition(new StateTransition<GraspState>(GraspState.BASIC_OPEN, openBasicGripCondition));
      stateHookGrip.addStateTransition(new StateTransition<GraspState>(GraspState.PINCH_OPEN, openPinchGripCondition));
   }
   
   private class OpenBasicGrip extends State<GraspState>
   {
      public OpenBasicGrip()
      {
         super(GraspState.BASIC_OPEN);
      }

      @Override
      public void doAction()
      {
      }

      @Override
      public void doTransitionIntoAction()
      {
         graspMode.set(RobotiqGraspMode.BASIC_MODE);
         fingerState.set(FingerState.OPEN);
         computeAllFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getOpenBasicGripDesiredConfiguration(robotSide));
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }
   
   private class ClosedBasicGrip extends State<GraspState>
   {
      public ClosedBasicGrip()
      {
         super(GraspState.BASIC_CLOSED);
      }

      @Override
      public void doAction()
      {
      }

      @Override
      public void doTransitionIntoAction()
      {
         graspMode.set(RobotiqGraspMode.BASIC_MODE);
         fingerState.set(FingerState.CLOSE);
         computeAllFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getClosedBasicGripDesiredConfiguration(robotSide));
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }
   
   private class OpenPinchGrip extends State<GraspState>
   {
      public OpenPinchGrip()
      {
         super(GraspState.PINCH_OPEN);
      }

      @Override
      public void doAction()
      {
      }

      @Override
      public void doTransitionIntoAction()
      {
         graspMode.set(RobotiqGraspMode.PINCH_MODE);
         fingerState.set(FingerState.OPEN);
         computeAllFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getOpenPinchGripDesiredConfiguration(robotSide));
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }
   
   private class ClosedPinchGrip extends State<GraspState>
   {
      public ClosedPinchGrip()
      {
         super(GraspState.PINCH_CLOSED);
      }

      @Override
      public void doAction()
      {
      }

      @Override
      public void doTransitionIntoAction()
      {
         graspMode.set(RobotiqGraspMode.PINCH_MODE);
         fingerState.set(FingerState.CLOSE);
         computeAllFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getClosedPinchGripDesiredConfiguration(robotSide));
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }
   
   private class HookGrip extends State<GraspState>
   {
      public HookGrip()
      {
         super(GraspState.HOOK);
      }

      @Override
      public void doAction()
      {
         
      }

      @Override
      public void doTransitionIntoAction()
      {
         graspMode.set(RobotiqGraspMode.BASIC_MODE);
         fingerState.set(FingerState.HOOK);
         computeIndexFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getOpenBasicGripDesiredConfiguration(robotSide));
         computeMiddleFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getClosedBasicGripDesiredConfiguration(robotSide));
         computeThumbFinalDesiredAngles(1.0, RobotiqHandsDesiredConfigurations.getClosedBasicGripDesiredConfiguration(robotSide));
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }
   
   @Override
   public void doControl()
   {
//      stateMachine.checkTransitionConditions();
//      stateMachine.doAction();
      computeDesiredJointAngles();
   }
   
   //TODO start
   public void open()
   {
      open(1.0);
   }
   
   public void open(double percent)
   {
      isStopped.set(false);
      
      switch(graspMode.getEnumValue())
      {
      case BASIC_MODE:
         computeAllFinalDesiredAngles(percent, RobotiqHandsDesiredConfigurations.getOpenBasicGripDesiredConfiguration(robotSide));
         break;
      case PINCH_MODE:
         computeAllFinalDesiredAngles(percent, RobotiqHandsDesiredConfigurations.getOpenPinchGripDesiredConfiguration(robotSide));
         break;
      case SCISSOR_MODE:
         break;
      case WIDE_MODE:
         break;
      default:
         break;
      }
   }

   public void open(FingerName fingerName)
   {
      open(1.0, fingerName);
   }

   public void open(double percent, FingerName fingerName)
   {
      isStopped.set(false);
      EnumMap<RobotiqHandJointNameMinimal, Double> openFingerDesiredConfiguration = RobotiqHandsDesiredConfigurations
            .getOpenBasicGripDesiredConfiguration(robotSide);
      computeOneFingerDesiredAngles(percent, openFingerDesiredConfiguration, fingerName);
   }

   public void close()
   {
      close(1.0);
   }
   
   public void close(double percent)
   {
      isStopped.set(false);

      switch(graspMode.getEnumValue())
      {
      case BASIC_MODE:
         computeAllFinalDesiredAngles(percent, RobotiqHandsDesiredConfigurations.getClosedBasicGripDesiredConfiguration(robotSide));
         break;
      case PINCH_MODE:
         computeAllFinalDesiredAngles(percent, RobotiqHandsDesiredConfigurations.getClosedPinchGripDesiredConfiguration(robotSide));
         break;
      case SCISSOR_MODE:
         break;
      case WIDE_MODE:
         break;
      default:
         break;
      }
   }

   public void close(FingerName fingerName)
   {
      close(1.0, fingerName);
   }

   public void close(double percent, FingerName fingerName)
   {
      isStopped.set(false);
      EnumMap<RobotiqHandJointNameMinimal, Double> closedFingerDesiredConfiguration = RobotiqHandsDesiredConfigurations
            .getClosedBasicGripDesiredConfiguration(robotSide);
      computeOneFingerDesiredAngles(percent, closedFingerDesiredConfiguration, fingerName);
   }

   public void hook()
   {
      isStopped.set(false);
      EnumMap<RobotiqHandJointNameMinimal, Double> closedHandDesiredConfiguration = RobotiqHandsDesiredConfigurations
            .getClosedBasicGripDesiredConfiguration(robotSide);
      EnumMap<RobotiqHandJointNameMinimal, Double> openHandDesiredConfiguration = RobotiqHandsDesiredConfigurations.getOpenBasicGripDesiredConfiguration(robotSide);

      computeIndexFinalDesiredAngles(1.0, openHandDesiredConfiguration);
      computeMiddleFinalDesiredAngles(1.0, closedHandDesiredConfiguration);
      computeThumbFinalDesiredAngles(1.0, closedHandDesiredConfiguration);
   }

   public void crush()
   {
      close();
   }

   public void crush(FingerName fingerName)
   {
      close(fingerName);
   }
   //TODO end
   
   public void basicGrip()
   {
//      desiredGraspMode.set(RobotiqGraspMode.BASIC_MODE);
      graspMode.set(RobotiqGraspMode.BASIC_MODE);
   }
   
   public void pinch()
   {
//      desiredGraspMode.set(RobotiqGraspMode.PINCH_MODE);
      graspMode.set(RobotiqGraspMode.PINCH_MODE);
   }

   public void stop()
   {
      if (!isStopped.getBooleanValue())
      {
         isStopped.set(true);
      }
   }

   public void reset()
   {
      isStopped.set(false);
      for (int i = 0; i < allFingerJoints.size(); i++)
      {
         OneDegreeOfFreedomJoint fingerJoint = allFingerJoints.get(i);
         finalDesiredAngles.get(fingerJoint).set(0.0);
      }
   }

   private void computeAllFinalDesiredAngles(double percent, EnumMap<RobotiqHandJointNameMinimal, Double> handDesiredConfiguration)
   {
      computeIndexFinalDesiredAngles(percent, handDesiredConfiguration);

      computeMiddleFinalDesiredAngles(percent, handDesiredConfiguration);

      computeThumbFinalDesiredAngles(percent, handDesiredConfiguration);
   }

   private void computeOneFingerDesiredAngles(double percent, EnumMap<RobotiqHandJointNameMinimal, Double> fingerDesiredConfiguration, FingerName fingerName)
   {
      switch (fingerName)
      {
      case INDEX:
         computeIndexFinalDesiredAngles(percent, fingerDesiredConfiguration);
         break;

      case MIDDLE:
         computeMiddleFinalDesiredAngles(percent, fingerDesiredConfiguration);
         break;

      case THUMB:
         computeThumbFinalDesiredAngles(percent, fingerDesiredConfiguration);
      default:
         break;
      }
   }

   private void computeThumbFinalDesiredAngles(double percent, EnumMap<RobotiqHandJointNameMinimal, Double> fingerDesiredConfiguration)
   {
      for (int i = 0; i < thumbJointEnumValues.size(); i++)
      {
         RobotiqHandJointNameMinimal fingerJointEnum = thumbJointEnumValues.get(i);
         OneDegreeOfFreedomJoint fingerJoint = thumbJoints.get(fingerJointEnum);
         double qDesired = percent * fingerDesiredConfiguration.get(fingerJointEnum);
         finalDesiredAngles.get(fingerJoint).set(qDesired);
      }
      initializeTrajectory();
   }

   private void computeMiddleFinalDesiredAngles(double percent, EnumMap<RobotiqHandJointNameMinimal, Double> fingerDesiredConfiguration)
   {
      for (int i = 0; i < middleJointEnumValues.size(); i++)
      {
         RobotiqHandJointNameMinimal fingerJointEnum = middleJointEnumValues.get(i);
         OneDegreeOfFreedomJoint fingerJoint = middleJoints.get(fingerJointEnum);
         double qDesired = percent * fingerDesiredConfiguration.get(fingerJointEnum);
         finalDesiredAngles.get(fingerJoint).set(qDesired);
      }
      initializeTrajectory();
   }

   private void computeIndexFinalDesiredAngles(double percent, EnumMap<RobotiqHandJointNameMinimal, Double> fingerDesiredConfiguration)
   {
      for (int i = 0; i < indexJointEnumValues.size(); i++)
      {
         RobotiqHandJointNameMinimal fingerJointEnum = indexJointEnumValues.get(i);
         OneDegreeOfFreedomJoint fingerJoint = indexJoints.get(fingerJointEnum);
         double qDesired = percent * fingerDesiredConfiguration.get(fingerJointEnum);
         finalDesiredAngles.get(fingerJoint).set(qDesired);
      }
      initializeTrajectory();
   }

   /**
    * Only place where the SCS robot should be modified
    */
   public void writeDesiredJointAngles()
   {
      for (int i = 0; i < allFingerJoints.size(); i++)
      {
         OneDegreeOfFreedomJoint fingerJoint = allFingerJoints.get(i);
         fingerJoint.setqDesired(desiredAngles.get(fingerJoint).getDoubleValue());
      }
   }

   private void initializeTrajectory()
   {
      for (int i = 0; i < allFingerJoints.size(); i++)
      {
         OneDegreeOfFreedomJoint fingerJoint = allFingerJoints.get(i);
         initialDesiredAngles.get(fingerJoint).set(desiredAngles.get(fingerJoint).getDoubleValue());
      }

      startTrajectoryTime.set(yoTime.getDoubleValue());
      endTrajectoryTime.set(startTrajectoryTime.getDoubleValue() + trajectoryTime.getDoubleValue());

      if (hasTrajectoryTimeChanged.getBooleanValue())
      {
         yoPolynomial.setCubic(0.0, trajectoryTime.getDoubleValue(), 0.0, 0.0, 1.0, 0.0);
         hasTrajectoryTimeChanged.set(false);
      }
   }

   private void computeDesiredJointAngles()
   {
      if (!isStopped.getBooleanValue())
      {
         currentTrajectoryTime.set(yoTime.getDoubleValue() - startTrajectoryTime.getDoubleValue());
         currentTrajectoryTime.set(MathTools.clipToMinMax(currentTrajectoryTime.getDoubleValue(), 0.0, trajectoryTime.getDoubleValue()));
      }
      yoPolynomial.compute(currentTrajectoryTime.getDoubleValue());
      double alpha = MathTools.clipToMinMax(yoPolynomial.getPosition(), 0.0, 1.0);

      for (int i = 0; i < allFingerJoints.size(); i++)
      {
         OneDegreeOfFreedomJoint fingerJoint = allFingerJoints.get(i);

         double q_d_initial = initialDesiredAngles.get(fingerJoint).getDoubleValue();
         double q_d_final = finalDesiredAngles.get(fingerJoint).getDoubleValue();
         double q_d = (1.0 - alpha) * q_d_initial + alpha * q_d_final;
         desiredAngles.get(fingerJoint).set(q_d);
         
         if (DEBUG && alpha > 0.0 && alpha < 1.0)
            PrintTools.debug(this, fingerJoint.getName() + "Desired q : " + q_d);
      }
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public String getName()
   {
      return robotSide.getCamelCaseNameForStartOfExpression() + getClass().getSimpleName();
   }

   @Override
   public String getDescription()
   {
      return "Simulated controller for the " + robotSide.getLowerCaseName() + " Robotiq hands.";
   }
}
