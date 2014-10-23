package us.ihmc.graveYard.commonWalkingControlModules.vrc.highLevelHumanoidControl.manipulation.states.toroidManipulation;

import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.graveYard.commonWalkingControlModules.vrc.TorusPoseProvider;
import us.ihmc.graveYard.commonWalkingControlModules.vrc.highLevelHumanoidControl.manipulation.ManipulableToroid;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.taskExecutor.Task;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;


/**
 * @author twan
 *         Date: 5/14/13
 */
public class HighLevelToroidManipulationState implements Task
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final TorusPoseProvider torusPoseProvider;
   private final ManipulableToroid manipulableToroid;
   private final ToroidManipulationStateMachine toroidManipulationStateMachine;

   public HighLevelToroidManipulationState(double controlDT, DoubleYoVariable yoTime, FullRobotModel fullRobotModel, TwistCalculator twistCalculator,
           SideDependentList<ReferenceFrame> handPositionControlFrames, SideDependentList<Integer> jacobianIds,
           TorusPoseProvider torusPoseProvider, MomentumBasedController momentumBasedController, YoGraphicsListRegistry yoGraphicsListRegistry,
           YoVariableRegistry parentRegistry)
   {
      RigidBody toroidBase = fullRobotModel.getElevator();
      double gravityZ = momentumBasedController.getGravityZ();
      this.torusPoseProvider = torusPoseProvider;
      this.manipulableToroid = new ManipulableToroid("twoHandGrip", toroidBase, yoGraphicsListRegistry, registry);
      this.toroidManipulationStateMachine = new ToroidManipulationStateMachine(yoTime, fullRobotModel, twistCalculator, manipulableToroid,
              handPositionControlFrames, jacobianIds, gravityZ, momentumBasedController, controlDT, registry, yoGraphicsListRegistry);

      toroidManipulationStateMachine.setIndividualHandPositionControlGains(100.0, 20.0, 100.0, 20.0);

      parentRegistry.addChild(registry);
   }

   public void doAction()
   {
      toroidManipulationStateMachine.doControl();
   }

   private void setToroidLocationAndRadius()
   {
      if (torusPoseProvider.checkForNewPose())
      {
         manipulableToroid.setToroidLocation(torusPoseProvider.getFramePose());
         manipulableToroid.setToroidRadius(torusPoseProvider.getFingerHoleRadius());
      }
   }

   public void doTransitionIntoAction()
   {
      setToroidLocationAndRadius();
      toroidManipulationStateMachine.initialize();
   }

   public void doTransitionOutOfAction()
   {
      // TODO: automatically generated code
   }

   public boolean isDone()
   {
      return false;  // TODO: automatically generated code
   }
}
