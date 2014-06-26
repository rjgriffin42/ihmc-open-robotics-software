package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.BlindWalkingToDestinationDesiredFootstepCalculator;
import us.ihmc.commonWalkingControlModules.desiredFootStep.ComponentBasedDesiredFootstepCalculator;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingUpdater;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.HeadingAndVelocityEvaluationScript;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.ManualDesiredVelocityControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.RateBasedDesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.SimpleDesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class HighLevelHumanoidControllerFactoryHelper
{
   public static BlindWalkingToDestinationDesiredFootstepCalculator getBlindWalkingToDestinationDesiredFootstepCalculator(
         WalkingControllerParameters walkingControllerParameters, CommonWalkingReferenceFrames referenceFrames,
         SideDependentList<ContactablePlaneBody> bipedFeet, YoVariableRegistry registry)
   {
      BlindWalkingToDestinationDesiredFootstepCalculator desiredFootstepCalculator = new BlindWalkingToDestinationDesiredFootstepCalculator(
            referenceFrames.getAnkleZUpReferenceFrames(), referenceFrames.getFootReferenceFrames(), bipedFeet, registry);

      desiredFootstepCalculator.setDesiredStepWidth(walkingControllerParameters.getInPlaceWidth());
      desiredFootstepCalculator.setDesiredStepForward(walkingControllerParameters.getDesiredStepForward());
      desiredFootstepCalculator.setMaxStepLength(walkingControllerParameters.getMaxStepLength());
      desiredFootstepCalculator.setMinStepWidth(walkingControllerParameters.getMinStepWidth());
      desiredFootstepCalculator.setMaxStepWidth(walkingControllerParameters.getMaxStepWidth());
      desiredFootstepCalculator.setStepPitch(walkingControllerParameters.getStepPitch());

      return desiredFootstepCalculator;
   }

   public static ComponentBasedDesiredFootstepCalculator getDesiredFootstepCalculator(WalkingControllerParameters walkingControllerParameters,
         CommonWalkingReferenceFrames referenceFrames, SideDependentList<ContactablePlaneBody> bipedFeet, double controlDT, YoVariableRegistry registry,
         ArrayList<Updatable> updatables, boolean useHeadingAndVelocityScript)
   {
      ComponentBasedDesiredFootstepCalculator desiredFootstepCalculator;
      ManualDesiredVelocityControlModule desiredVelocityControlModule;

      DesiredHeadingControlModule desiredHeadingControlModule;
      if (useHeadingAndVelocityScript)
      {
         desiredVelocityControlModule = new ManualDesiredVelocityControlModule(ReferenceFrame.getWorldFrame(), registry);
         desiredVelocityControlModule.setDesiredVelocity(new FrameVector2d(ReferenceFrame.getWorldFrame(), 1.0, 0.0));

         SimpleDesiredHeadingControlModule simpleDesiredHeadingControlModule = new SimpleDesiredHeadingControlModule(0.0, controlDT, registry);
         simpleDesiredHeadingControlModule.setMaxHeadingDot(0.4);
         simpleDesiredHeadingControlModule.updateDesiredHeadingFrame();
         boolean cycleThroughAllEvents = true;
         HeadingAndVelocityEvaluationScript headingAndVelocityEvaluationScript = new HeadingAndVelocityEvaluationScript(cycleThroughAllEvents, controlDT,
               simpleDesiredHeadingControlModule, desiredVelocityControlModule, registry);
         updatables.add(headingAndVelocityEvaluationScript);
         desiredHeadingControlModule = simpleDesiredHeadingControlModule;
      }
      else
      {
         desiredHeadingControlModule = new RateBasedDesiredHeadingControlModule(0.0, controlDT, registry);
         desiredVelocityControlModule = new ManualDesiredVelocityControlModule(desiredHeadingControlModule.getDesiredHeadingFrame(), registry);
      }

      updatables.add(new DesiredHeadingUpdater(desiredHeadingControlModule));

      desiredFootstepCalculator = new ComponentBasedDesiredFootstepCalculator(referenceFrames.getAnkleZUpReferenceFrames(),
            referenceFrames.getFootReferenceFrames(), bipedFeet, desiredHeadingControlModule, desiredVelocityControlModule, registry);

      desiredFootstepCalculator.setInPlaceWidth(walkingControllerParameters.getInPlaceWidth());
      desiredFootstepCalculator.setMaxStepLength(walkingControllerParameters.getMaxStepLength());
      desiredFootstepCalculator.setMinStepWidth(walkingControllerParameters.getMinStepWidth());
      desiredFootstepCalculator.setMaxStepWidth(walkingControllerParameters.getMaxStepWidth());
      desiredFootstepCalculator.setStepPitch(walkingControllerParameters.getStepPitch());

      return desiredFootstepCalculator;
   }

   public static InverseDynamicsJoint[] computeJointsToOptimizeFor(FullRobotModel fullRobotModel, InverseDynamicsJoint... jointsToRemove)
   {
      List<InverseDynamicsJoint> joints = new ArrayList<InverseDynamicsJoint>();
      InverseDynamicsJoint[] allJoints = ScrewTools.computeSupportAndSubtreeJoints(fullRobotModel.getRootJoint().getSuccessor());
      joints.addAll(Arrays.asList(allJoints));

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody hand = fullRobotModel.getHand(robotSide);
         if (hand != null)
         {
            List<InverseDynamicsJoint> fingerJoints = Arrays.asList(ScrewTools.computeSubtreeJoints(hand));
            joints.removeAll(fingerJoints);
         }
      }

      if (jointsToRemove != null)
      {
         for (InverseDynamicsJoint joint : jointsToRemove)
         {
            joints.remove(joint);
         }
      }

      return joints.toArray(new InverseDynamicsJoint[joints.size()]);
   }

   public static MomentumOptimizationSettings createMomentumOptimizationSettings(FullRobotModel fullRobotModel, YoVariableRegistry registry,
         InverseDynamicsJoint... jointsToIgnore)
   {
      InverseDynamicsJoint[] jointsToOptimizeFor = HighLevelHumanoidControllerFactoryHelper.computeJointsToOptimizeFor(fullRobotModel, jointsToIgnore);

      MomentumOptimizationSettings momentumOptimizationSettings = new MomentumOptimizationSettings(jointsToOptimizeFor, registry);
      momentumOptimizationSettings.setDampedLeastSquaresFactor(0.05);
      momentumOptimizationSettings.setRhoPlaneContactRegularization(0.001);
      momentumOptimizationSettings.setMomentumWeight(1.0, 1.0, 10.0, 10.0);
      momentumOptimizationSettings.setRhoMin(4.0);
      momentumOptimizationSettings.setRateOfChangeOfRhoPlaneContactRegularization(0.01);
      momentumOptimizationSettings.setRhoPenalizerPlaneContactRegularization(0.01);

      return momentumOptimizationSettings;
   }

}
