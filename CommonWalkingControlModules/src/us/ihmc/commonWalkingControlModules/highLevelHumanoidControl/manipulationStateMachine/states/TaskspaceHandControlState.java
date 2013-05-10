package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.states;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.statemachines.State;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.utilities.FormattingTools;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;

public abstract class TaskspaceHandControlState extends State<IndividualHandControlState>
{
   protected final String name;
   protected final YoVariableRegistry registry;
   protected final GeometricJacobian jacobian;

   private final TaskspaceConstraintData taskspaceConstraintData = new TaskspaceConstraintData();
   private final MomentumBasedController momentumBasedController;


   public TaskspaceHandControlState(MomentumBasedController momentumBasedController, GeometricJacobian jacobian,
                                    YoVariableRegistry parentRegistry)
   {
      super(IndividualHandControlState.MOVE_HAND_TO_POSITION_IN_WORLDFRAME);

      RigidBody endEffector = jacobian.getEndEffector();
      name = endEffector.getName() + FormattingTools.underscoredToCamelCase(stateEnum.toString(), true) + "State";
      registry = new YoVariableRegistry(name);

      this.momentumBasedController = momentumBasedController;
      this.jacobian = jacobian;

      parentRegistry.addChild(registry);
   }

   @Override
   public final void doAction()
   {
      SpatialAccelerationVector handAcceleration = computeDesiredSpatialAcceleration();
      taskspaceConstraintData.set(handAcceleration);
      momentumBasedController.setDesiredSpatialAcceleration(jacobian, taskspaceConstraintData);
   }

   protected abstract SpatialAccelerationVector computeDesiredSpatialAcceleration();

   @Override
   public void doTransitionIntoAction()
   {
   }

   @Override
   public void doTransitionOutOfAction()
   {
   }
}
