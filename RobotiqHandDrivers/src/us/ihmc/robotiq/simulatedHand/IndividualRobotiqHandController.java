package us.ihmc.robotiq.simulatedHand;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.robotiq.model.RobotiqHandModel.RobotiqHandJointNameMinimal;
import us.ihmc.utilities.humanoidRobot.partNames.FingerName;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.math.trajectories.YoPolynomial;

import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;

public class IndividualRobotiqHandController
{
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
   private final DoubleYoVariable startTrajectoryTime, endTrajectoryTime, trajectoryTime;
   private final BooleanYoVariable hasTrajectoryTimeChanged;
   private final LinkedHashMap<OneDegreeOfFreedomJoint, DoubleYoVariable> initialDesiredAngles = new LinkedHashMap<>();
   private final LinkedHashMap<OneDegreeOfFreedomJoint, DoubleYoVariable> finalDesiredAngles = new LinkedHashMap<>();
   private final LinkedHashMap<OneDegreeOfFreedomJoint, DoubleYoVariable> desiredAngles = new LinkedHashMap<>();

   public IndividualRobotiqHandController(RobotSide robotSide, DoubleYoVariable yoTime, DoubleYoVariable trajectoryTime, SDFRobot simulatedRobot, YoVariableRegistry parentRegistry)
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
      endTrajectoryTime = new DoubleYoVariable(sidePrefix + "EndTrajectoryTime", registry);
      this.trajectoryTime = trajectoryTime;
      hasTrajectoryTimeChanged = new BooleanYoVariable(sidePrefix + "HasTrajectoryTimeChanged", registry);
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
   }

   public void open()
   {
      open(1.0);
   }

   public void open(double percent)
   {
      EnumMap<RobotiqHandJointNameMinimal, Double> openHandDesiredConfiguration = RobotiqHandsDesiredConfigurations.getOpenHandDesiredConfiguration(robotSide);
      computeAllFinalDesiredAngles(percent, openHandDesiredConfiguration);
   }

   public void open(FingerName fingerName)
   {
      open(1.0, fingerName);
   }

   public void open(double percent, FingerName fingerName)
   {
      EnumMap<RobotiqHandJointNameMinimal, Double> openFingerDesiredConfiguration = RobotiqHandsDesiredConfigurations.getOpenHandDesiredConfiguration(robotSide);
      computeOneFingerDesiredAngles(percent, openFingerDesiredConfiguration, fingerName);
   }

   public void close()
   {
      close(1.0);
   }

   public void close(double percent)
   {
      EnumMap<RobotiqHandJointNameMinimal, Double> closedHandDesiredConfiguration = RobotiqHandsDesiredConfigurations.getClosedHandDesiredConfiguration(robotSide);
      computeAllFinalDesiredAngles(percent, closedHandDesiredConfiguration);
   }

   public void close(FingerName fingerName)
   {
      close(1.0, fingerName);
   }

   public void close(double percent, FingerName fingerName)
   {
      EnumMap<RobotiqHandJointNameMinimal, Double> closedFingerDesiredConfiguration = RobotiqHandsDesiredConfigurations.getClosedHandDesiredConfiguration(robotSide);
      computeOneFingerDesiredAngles(percent, closedFingerDesiredConfiguration, fingerName);
   }

   public void hook()
   {
      EnumMap<RobotiqHandJointNameMinimal, Double> closedHandDesiredConfiguration = RobotiqHandsDesiredConfigurations.getClosedHandDesiredConfiguration(robotSide);
      EnumMap<RobotiqHandJointNameMinimal, Double> openHandDesiredConfiguration = RobotiqHandsDesiredConfigurations.getOpenHandDesiredConfiguration(robotSide);

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

   public void reset()
   {
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

   public void computeDesiredJointAngles()
   {
      double currentTrajectoryTime = yoTime.getDoubleValue() - startTrajectoryTime.getDoubleValue();
      currentTrajectoryTime = MathTools.clipToMinMax(currentTrajectoryTime, 0.0, trajectoryTime.getDoubleValue());
      yoPolynomial.compute(currentTrajectoryTime);
      double alpha = MathTools.clipToMinMax(yoPolynomial.getPosition(), 0.0, 1.0);
      

      for (int i = 0; i < allFingerJoints.size(); i++)
      {
         OneDegreeOfFreedomJoint fingerJoint = allFingerJoints.get(i);
         
         double q_d_initial = initialDesiredAngles.get(fingerJoint).getDoubleValue();
         double q_d_final = finalDesiredAngles.get(fingerJoint).getDoubleValue();
         double q_d = (1.0 - alpha) * q_d_initial + alpha * q_d_final;
         desiredAngles.get(fingerJoint).set(q_d);
      }
   }
}
