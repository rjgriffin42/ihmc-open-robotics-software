package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.PositionController;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.statemachines.State;
import com.yobotics.simulationconstructionset.util.trajectory.PositionTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.IndividualHandControlState;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.FormattingTools;
import us.ihmc.utilities.math.geometry.*;
import us.ihmc.utilities.screwTheory.*;

/**
 * @author twan
 *         Date: 5/9/13
 */
public class PointPositionHandControlState extends State<IndividualHandControlState>
{
   private final YoVariableRegistry registry;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final MomentumBasedController momentumBasedController;
   private final TwistCalculator twistCalculator;
   private final YoFramePoint yoDesiredPosition;


   private PositionTrajectoryGenerator positionTrajectoryGenerator;
   private PositionController positionController;

   private FramePoint pointInBody;
   private GeometricJacobian jacobian;

   // temp stuff:
   private final FramePoint desiredPosition = new FramePoint(worldFrame);
   private final FrameVector desiredVelocity = new FrameVector(worldFrame);
   private final FrameVector desiredAcceleration = new FrameVector(worldFrame);
   private final FrameVector currentVelocity = new FrameVector(worldFrame);

   private final FrameVector pointAcceleration = new FrameVector(worldFrame);
   private Twist currentTwist = new Twist();


   private final FramePoint point = new FramePoint(worldFrame);

   public PointPositionHandControlState(MomentumBasedController momentumBasedController, RobotSide robotSide,
                                        DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry parentRegistry)
   {
      super(IndividualHandControlState.POINT_POSITION);
      this.momentumBasedController = momentumBasedController;
      this.twistCalculator = momentumBasedController.getTwistCalculator();

      String stateName = FormattingTools.underscoredToCamelCase(this.stateEnum.toString(), true) + "State";
      String name = robotSide.getCamelCaseNameForStartOfExpression() + stateName;
      registry = new YoVariableRegistry(name);
      String desiredHandPositionName = robotSide.getCamelCaseNameForStartOfExpression() + "HandDesPointPosition";
      yoDesiredPosition = new YoFramePoint(desiredHandPositionName, worldFrame, registry);

      if (dynamicGraphicObjectsListRegistry != null)
      {
         DynamicGraphicPosition desiredPositionViz = yoDesiredPosition.createDynamicGraphicPosition(desiredHandPositionName, 0.01, YoAppearance.FireBrick());
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject(stateName, desiredPositionViz);
      }

      parentRegistry.addChild(registry);
   }

   @Override
   public void doAction()
   {
      point.setAndChangeFrame(pointInBody);
      point.changeFrame(jacobian.getBaseFrame());

      updateCurrentVelocity(point);

      positionTrajectoryGenerator.compute(getTimeInCurrentState());

      positionTrajectoryGenerator.packLinearData(desiredPosition, desiredVelocity, desiredAcceleration);
      pointAcceleration.setToZero(positionController.getBodyFrame());

      positionController.compute(pointAcceleration, desiredPosition, desiredVelocity, currentVelocity, desiredAcceleration);

      desiredPosition.changeFrame(worldFrame);
      yoDesiredPosition.set(desiredPosition);

      pointAcceleration.changeFrame(jacobian.getBaseFrame());

      jacobian.compute();
      momentumBasedController.setDesiredPointAcceleration(jacobian, point, pointAcceleration);
   }


   private void updateCurrentVelocity(FramePoint point)
   {
      twistCalculator.packRelativeTwist(currentTwist, jacobian.getBase(), jacobian.getEndEffector());
      currentTwist.changeFrame(jacobian.getBaseFrame());
      currentTwist.packVelocityOfPointFixedInBodyFrame(currentVelocity, point);
   }

   @Override
   public void doTransitionIntoAction()
   {
      positionTrajectoryGenerator.initialize();
   }

   @Override
   public void doTransitionOutOfAction()
   {
   }

   @Override
   public boolean isDone()
   {
      return positionTrajectoryGenerator.isDone();
   }

   public void setTrajectory(PositionTrajectoryGenerator positionTrajectoryGenerator,
                             PositionController positionController, FramePoint pointInBody, GeometricJacobian jacobian)
   {
      this.positionTrajectoryGenerator = positionTrajectoryGenerator;
      this.positionController = positionController;
      this.pointInBody = new FramePoint(pointInBody);
      this.jacobian = jacobian;
   }

   public FramePoint getDesiredPosition()
   {
      positionTrajectoryGenerator.compute(getTimeInCurrentState());
      positionTrajectoryGenerator.get(desiredPosition);
      return desiredPosition;
   }

   public ReferenceFrame getFrameToControlPoseOf()
   {
      return positionController.getBodyFrame();
   }
}
