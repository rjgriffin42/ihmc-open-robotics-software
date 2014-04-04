package us.ihmc.commonWalkingControlModules.posePlayback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.HighLevelState;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.IntegerYoVariable;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.PDController;
import com.yobotics.simulationconstructionset.util.statemachines.State;
import com.yobotics.simulationconstructionset.util.trajectory.CubicPolynomialTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoVariableDoubleProvider;

public class PosePlaybackController extends State<HighLevelState>
{
   private static final HighLevelState controllerState = HighLevelState.POSE_PLAYBACK;

   private static final String CONTROLLER_PREFIX = "PosePlayback";

   private final YoVariableRegistry registry = new YoVariableRegistry(CONTROLLER_PREFIX);

   private final ArrayList<OneDoFJoint> allOneDoFJoints;
   private final LinkedHashMap<OneDoFJoint, CubicPolynomialTrajectoryGenerator> jointTrajectories;
   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> jointInitialDesiredAngles;
   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> jointFinalDesiredAngles;
   private final DoubleYoVariable currentPoseTrajectoryTime;
   private final LinkedHashMap<OneDoFJoint, PDController> jointPDControllers;

   private List<Double> trajectoryTimes;
   private List<Map<OneDoFJoint, Double>> listOfPosesToPlayback;

   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable poseSwitchTime = new DoubleYoVariable(CONTROLLER_PREFIX + "_poseSwitchTime", registry);
   private final DoubleYoVariable timeInCurrentPose = new DoubleYoVariable(CONTROLLER_PREFIX + "_timeInCurrentPose", registry);
   private final IntegerYoVariable currentPoseIndex = new IntegerYoVariable(CONTROLLER_PREFIX + "_currentPoseIndex", registry);

   private final BooleanYoVariable hasBeenInitialized = new BooleanYoVariable(CONTROLLER_PREFIX + "_hasBeenInitialized", registry);
   private final BooleanYoVariable startPosePlayback = new BooleanYoVariable(CONTROLLER_PREFIX + "_startPosePlayback", registry);

   private final DoubleYoVariable outputScaling = new DoubleYoVariable(CONTROLLER_PREFIX + "_outputScaling", registry);
   
   private final double controlDT;

   public PosePlaybackController(FullRobotModel fullRobotModel, DoubleYoVariable yoTime, double controlDT)
   {
      super(controllerState);

      this.yoTime = yoTime;
      this.controlDT = controlDT;
      
      outputScaling.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable v)
         {
            outputScaling.set(MathTools.clipToMinMax(outputScaling.getDoubleValue(), 0.0, 1.0));
         }
      });
      
      startPosePlayback.set(false);

      trajectoryTimes = null;
      listOfPosesToPlayback = null;

      allOneDoFJoints = new ArrayList<>(Arrays.asList(fullRobotModel.getOneDoFJoints()));
      jointTrajectories = new LinkedHashMap<>(allOneDoFJoints.size());
      jointInitialDesiredAngles = new LinkedHashMap<>(allOneDoFJoints.size());
      jointFinalDesiredAngles = new LinkedHashMap<>(allOneDoFJoints.size());
      currentPoseTrajectoryTime = new DoubleYoVariable(CONTROLLER_PREFIX + "_currentPoseTrajectoryTime", registry);

      jointPDControllers = new LinkedHashMap<>(allOneDoFJoints.size());

      for (int i = 0; i < allOneDoFJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = allOneDoFJoints.get(i);
         String namePrefix = CONTROLLER_PREFIX + oneDoFJoint.getName();
         DoubleYoVariable initialPosition = new DoubleYoVariable(namePrefix + "_initialQd", registry);
         DoubleYoVariable finalPosition = new DoubleYoVariable(namePrefix + "_finalQd", registry);
         DoubleProvider initialPositionProvider = new YoVariableDoubleProvider(initialPosition);
         DoubleProvider finalPositionProvider = new YoVariableDoubleProvider(finalPosition);
         YoVariableDoubleProvider trajectoryTimeProvider = new YoVariableDoubleProvider(currentPoseTrajectoryTime);

         CubicPolynomialTrajectoryGenerator jointTrajectory = new CubicPolynomialTrajectoryGenerator(namePrefix + "Traj", initialPositionProvider,
               finalPositionProvider, trajectoryTimeProvider, registry);

         jointInitialDesiredAngles.put(oneDoFJoint, initialPosition);
         jointFinalDesiredAngles.put(oneDoFJoint, finalPosition);
         jointTrajectories.put(oneDoFJoint, jointTrajectory);

         PDController pdController = new PDController(namePrefix, registry);
         jointPDControllers.put(oneDoFJoint, pdController);
      }
   }

   private void initialize()
   {
      hasBeenInitialized.set(true);
      
      currentPoseIndex.set(0);
      timeInCurrentPose.set(0.0);

      updateTrajectoryGenerators(true);
   }

   public void setupForPosePlayblack(PosePlaybackPacket posePlaybackPacket)
   {
      trajectoryTimes = posePlaybackPacket.getTrajectoryTimes();
      listOfPosesToPlayback = posePlaybackPacket.getListOfPosesToPlayback();

      if (trajectoryTimes.size() != listOfPosesToPlayback.size())
         throw new RuntimeException("Should be of the length.");

      Map<OneDoFJoint, Double> jointKps = posePlaybackPacket.getJointKps();
      Map<OneDoFJoint, Double> jointKds = posePlaybackPacket.getJointKds();

      for (int i = 0; i < allOneDoFJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = allOneDoFJoints.get(i);
         PDController pdController = jointPDControllers.get(oneDoFJoint);
         Double kp = jointKps.get(oneDoFJoint);
         if (kp == null) kp = 0.0;
         pdController.setProportionalGain(kp);
         Double kd = jointKds.get(oneDoFJoint);
         if (kd == null) kd = 0.0;
         pdController.setDerivativeGain(kd);
      }
      
      initialize();
   }

   @Override
   public void doAction()
   {
      if (!hasBeenInitialized.getBooleanValue())
      {
         for (int i = 0; i < allOneDoFJoints.size(); i++)
         {
            allOneDoFJoints.get(i).setTau(0.0);
         }
         
         return;
      }
      
      if (startPosePlayback.getBooleanValue())
      {
         timeInCurrentPose.add(controlDT);
      }

      if (timeInCurrentPose.getDoubleValue() > currentPoseTrajectoryTime.getDoubleValue() && currentPoseIndex.getIntegerValue() < trajectoryTimes.size() - 1)
      {
         poseSwitchTime.set(yoTime.getDoubleValue());
         timeInCurrentPose.set(0.0);

         currentPoseIndex.increment();
         updateTrajectoryGenerators(false);
      }

      for (int i = 0; i < allOneDoFJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = allOneDoFJoints.get(i);

         CubicPolynomialTrajectoryGenerator jointTrajectory = jointTrajectories.get(oneDoFJoint);
         jointTrajectory.compute(timeInCurrentPose.getDoubleValue());

         double currentPosition = oneDoFJoint.getQ();
         double desiredPosition = jointTrajectory.getValue();
         double currentRate = oneDoFJoint.getQd();
         double desiredRate = jointTrajectory.getVelocity();

         double tauDesired = jointPDControllers.get(oneDoFJoint).compute(currentPosition, desiredPosition, currentRate, desiredRate);
         tauDesired *= outputScaling.getDoubleValue();
         
         oneDoFJoint.setTau(tauDesired);
      }
   }
   
   private void updateTrajectoryGenerators(boolean setInitialDesiredAngleToActualJointAngle)
   {
      currentPoseTrajectoryTime.set(trajectoryTimes.get(currentPoseIndex.getIntegerValue()));

      for (int i = 0; i < allOneDoFJoints.size(); i++)
      {
         OneDoFJoint oneDoFJoint = allOneDoFJoints.get(i);

         DoubleYoVariable jointInitialDesiredAngle = jointInitialDesiredAngles.get(oneDoFJoint);
         if (setInitialDesiredAngleToActualJointAngle)
            jointInitialDesiredAngle.set(oneDoFJoint.getQ());
         else
            jointInitialDesiredAngle.set(jointTrajectories.get(oneDoFJoint).getValue());
         
         Double finalDesiredAngle = listOfPosesToPlayback.get(currentPoseIndex.getIntegerValue()).get(oneDoFJoint);
         if (finalDesiredAngle == null)
            finalDesiredAngle = oneDoFJoint.getQ();

         jointFinalDesiredAngles.get(oneDoFJoint).set(finalDesiredAngle);

         jointTrajectories.get(oneDoFJoint).initialize();
         jointTrajectories.get(oneDoFJoint).compute(0.0);
      }
   }

   @Override
   public void doTransitionIntoAction()
   {
      initialize();
   }

   @Override
   public void doTransitionOutOfAction()
   {
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }
}
