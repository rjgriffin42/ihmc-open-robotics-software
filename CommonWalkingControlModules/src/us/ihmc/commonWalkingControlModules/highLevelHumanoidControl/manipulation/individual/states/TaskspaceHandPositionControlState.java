package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import java.util.ArrayList;
import java.util.Collection;

import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlState;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphicReferenceFrame;

import com.yobotics.simulationconstructionset.util.trajectory.PoseTrajectoryGenerator;

/**
 * @author twan
 *         Date: 5/9/13
 */
public class TaskspaceHandPositionControlState extends TaskspaceHandControlState
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   protected final SpatialAccelerationVector handAcceleration = new SpatialAccelerationVector();

   // viz stuff:
   private final Collection<YoGraphicReferenceFrame> dynamicGraphicReferenceFrames = new ArrayList<YoGraphicReferenceFrame>();
   protected final PoseReferenceFrame desiredPositionFrame;

   // temp stuff:
   protected final FramePoint desiredPosition = new FramePoint(worldFrame);
   protected final FrameVector desiredVelocity = new FrameVector(worldFrame);
   protected final FrameVector desiredAcceleration = new FrameVector(worldFrame);

   protected final FrameOrientation desiredOrientation = new FrameOrientation(worldFrame);
   protected final FrameVector desiredAngularVelocity = new FrameVector(worldFrame);
   protected final FrameVector desiredAngularAcceleration = new FrameVector(worldFrame);

   protected PoseTrajectoryGenerator poseTrajectoryGenerator;
   protected RigidBodySpatialAccelerationControlModule handSpatialAccelerationControlModule;

   private final DoubleYoVariable doneTrajectoryTime;
   private final DoubleYoVariable holdPositionDuration;

   public TaskspaceHandPositionControlState(String namePrefix, HandControlState stateEnum, MomentumBasedController momentumBasedController, int jacobianId,
         RigidBody base, RigidBody endEffector, YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      super(namePrefix, stateEnum, momentumBasedController, jacobianId, base, endEffector, parentRegistry);

      desiredPositionFrame = new PoseReferenceFrame(name + "DesiredFrame", worldFrame);

      if (yoGraphicsListRegistry != null)
      {
         YoGraphicsList list = new YoGraphicsList(name);

         YoGraphicReferenceFrame dynamicGraphicReferenceFrame = new YoGraphicReferenceFrame(desiredPositionFrame, registry, 0.3);
         dynamicGraphicReferenceFrames.add(dynamicGraphicReferenceFrame);
         list.add(dynamicGraphicReferenceFrame);

         yoGraphicsListRegistry.registerDynamicGraphicObjectsList(list);
         list.hideYoGraphics();
      }

      doneTrajectoryTime = new DoubleYoVariable(namePrefix + "DoneTrajectoryTime", registry);
      holdPositionDuration = new DoubleYoVariable(namePrefix + "HoldPositionDuration", registry);
   }

   protected SpatialAccelerationVector computeDesiredSpatialAcceleration()
   {
      poseTrajectoryGenerator.compute(getTimeInCurrentState());

      poseTrajectoryGenerator.packLinearData(desiredPosition, desiredVelocity, desiredAcceleration);
      poseTrajectoryGenerator.packAngularData(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);

      handSpatialAccelerationControlModule.doPositionControl(desiredPosition, desiredOrientation, desiredVelocity, desiredAngularVelocity, desiredAcceleration,
            desiredAngularAcceleration, getBase());

      handSpatialAccelerationControlModule.packAcceleration(handAcceleration);

      ReferenceFrame handFrame = handSpatialAccelerationControlModule.getEndEffector().getBodyFixedFrame();
      handAcceleration.changeBodyFrameNoRelativeAcceleration(handFrame);
      handAcceleration.changeFrameNoRelativeMotion(handFrame);

      updateVisualizers();

      return handAcceleration;
   }

   @Override
   public void doTransitionIntoAction()
   {
      poseTrajectoryGenerator.showVisualization();
      poseTrajectoryGenerator.initialize();
      doneTrajectoryTime.set(Double.NaN);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      holdPositionDuration.set(0.0);
   }

   @Override
   public boolean isDone()
   {
      if (Double.isNaN(doneTrajectoryTime.getDoubleValue()))
         return false;
      return getTimeInCurrentState() > doneTrajectoryTime.getDoubleValue() + holdPositionDuration.getDoubleValue();
   }

   @Override
   public void doAction()
   {
      if (Double.isNaN(doneTrajectoryTime.getDoubleValue()) && poseTrajectoryGenerator.isDone())
         doneTrajectoryTime.set(getTimeInCurrentState());

      super.doAction();
   }

   private void updateVisualizers()
   {
      desiredPosition.changeFrame(worldFrame);
      desiredOrientation.changeFrame(worldFrame);
      desiredPositionFrame.setPoseAndUpdate(desiredPosition, desiredOrientation);
      desiredPositionFrame.update();

      for (YoGraphicReferenceFrame dynamicGraphicReferenceFrame : dynamicGraphicReferenceFrames)
      {
         dynamicGraphicReferenceFrame.update();
      }

      if (poseTrajectoryGenerator.isDone())
         poseTrajectoryGenerator.hideVisualization();
   }

   public void setHoldPositionDuration(double time)
   {
      holdPositionDuration.set(time);
   }

   public void setTrajectory(PoseTrajectoryGenerator poseTrajectoryGenerator,
         RigidBodySpatialAccelerationControlModule rigidBodySpatialAccelerationControlModule)
   {
      this.poseTrajectoryGenerator = poseTrajectoryGenerator;
      this.taskspaceConstraintData.set(getBase(), getEndEffector());
      this.handSpatialAccelerationControlModule = rigidBodySpatialAccelerationControlModule;
   }

   public ReferenceFrame getReferenceFrame()
   {
      // FIXME: hack

      FramePoint point = new FramePoint();
      poseTrajectoryGenerator.get(point);
      return point.getReferenceFrame();
   }

   public FramePose getDesiredPose()
   {
      poseTrajectoryGenerator.get(desiredPosition);
      desiredPosition.changeFrame(getFrameToControlPoseOf());

      poseTrajectoryGenerator.get(desiredOrientation);
      desiredOrientation.changeFrame(getFrameToControlPoseOf());

      return new FramePose(desiredPosition, desiredOrientation);
   }

   public ReferenceFrame getFrameToControlPoseOf()
   {
      return handSpatialAccelerationControlModule.getTrackingFrame();
   }
}
