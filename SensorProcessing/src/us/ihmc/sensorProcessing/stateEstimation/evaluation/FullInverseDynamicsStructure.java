package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.FloatingInverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.robotics.screwTheory.TwistCalculator;

public class FullInverseDynamicsStructure
{
   private final TwistCalculator twistCalculator;
   private final SpatialAccelerationCalculator spatialAccelerationCalculator;
   private final RigidBody estimationLink;
   private final RigidBody elevator;
   private final FloatingInverseDynamicsJoint rootJoint;

   // TODO: What's a good name for this?
   public FullInverseDynamicsStructure(RigidBody elevator, RigidBody estimationLink, FloatingInverseDynamicsJoint rootInverseDynamicsJoint)
   {
      this.elevator = elevator;
      this.rootJoint = rootInverseDynamicsJoint;

      twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), elevator);
      spatialAccelerationCalculator = new SpatialAccelerationCalculator(elevator, 0.0, false);

      this.estimationLink = estimationLink;
   }

   public FloatingInverseDynamicsJoint getRootJoint()
   {
      return rootJoint;
   }

   public TwistCalculator getTwistCalculator()
   {
      return twistCalculator;
   }

   public SpatialAccelerationCalculator getSpatialAccelerationCalculator()
   {
      return spatialAccelerationCalculator;
   }

   public RigidBody getEstimationLink()
   {
      return estimationLink;
   }

   public ReferenceFrame getEstimationFrame()
   {
      return estimationLink.getParentJoint().getFrameAfterJoint();
   }

   public RigidBody getElevator()
   {
      return elevator;
   }

   public void updateInternalState()
   {
      twistCalculator.compute();
      spatialAccelerationCalculator.compute();
   }

   public static FullInverseDynamicsStructure createInverseDynamicStructure(FullRobotModel fullRobotModel)
   {
      RigidBody elevator = fullRobotModel.getElevator();
      FloatingInverseDynamicsJoint rootInverseDynamicsJoint = fullRobotModel.getRootJoint();
      RigidBody estimationLink = fullRobotModel.getPelvis();

      FullInverseDynamicsStructure inverseDynamicsStructure = new FullInverseDynamicsStructure(elevator, estimationLink, rootInverseDynamicsJoint);

      return inverseDynamicsStructure;
   }
}
