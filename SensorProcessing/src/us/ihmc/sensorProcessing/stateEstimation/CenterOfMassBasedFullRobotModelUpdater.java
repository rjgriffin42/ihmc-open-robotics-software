package us.ihmc.sensorProcessing.stateEstimation;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.CenterOfMassAccelerationCalculator;
import us.ihmc.utilities.screwTheory.CenterOfMassCalculator;
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

//assumes that twist calculator and spatial acceleration calculator have already been updated with joint positions and velocities
//TODO: update accelerations
public class CenterOfMassBasedFullRobotModelUpdater implements Runnable
{
   private final ControlFlowInputPort<TwistCalculator> twistCalculatorInputPort;
   private final ControlFlowInputPort<SpatialAccelerationCalculator> spatialAccelerationCalculatorInputPort;

   private final ControlFlowOutputPort<FramePoint> centerOfMassPositionPort;
   private final ControlFlowOutputPort<FrameVector> centerOfMassVelocityPort;
   private final ControlFlowOutputPort<FrameVector> centerOfMassAccelerationPort;

   private final ControlFlowOutputPort<FrameOrientation> orientationPort;
   private final ControlFlowOutputPort<FrameVector> angularVelocityPort;
   private final ControlFlowOutputPort<FrameVector> angularAccelerationPort;

   private final CenterOfMassCalculator centerOfMassCalculator;
   private final CenterOfMassJacobian centerOfMassJacobianBody;
   private final CenterOfMassAccelerationCalculator centerOfMassAccelerationCalculator;

   private final ReferenceFrame estimationFrame;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final SixDoFJoint rootJoint;
   private final RigidBody estimationLink;

   public CenterOfMassBasedFullRobotModelUpdater(ControlFlowInputPort<TwistCalculator> twistCalculatorInputPort, 
         ControlFlowInputPort<SpatialAccelerationCalculator> spatialAccelerationCalculatorInputPort,
           ControlFlowOutputPort<FramePoint> centerOfMassPositionPort, ControlFlowOutputPort<FrameVector> centerOfMassVelocityPort,
           ControlFlowOutputPort<FrameVector> centerOfMassAccelerationPort, ControlFlowOutputPort<FrameOrientation> orientationPort,
           ControlFlowOutputPort<FrameVector> angularVelocityPort, ControlFlowOutputPort<FrameVector> angularAccelerationPort, RigidBody estimationLink,
           ReferenceFrame estimationFrame, SixDoFJoint rootJoint)
   {
      this.twistCalculatorInputPort = twistCalculatorInputPort;
      this.spatialAccelerationCalculatorInputPort = spatialAccelerationCalculatorInputPort;

      this.centerOfMassPositionPort = centerOfMassPositionPort;
      this.centerOfMassVelocityPort = centerOfMassVelocityPort;
      this.centerOfMassAccelerationPort = centerOfMassAccelerationPort;

      this.orientationPort = orientationPort;
      this.angularVelocityPort = angularVelocityPort;
      this.angularAccelerationPort = angularAccelerationPort;

      this.estimationLink = estimationLink;
      this.estimationFrame = estimationFrame;

      RigidBody elevator = rootJoint.getPredecessor();
      this.centerOfMassCalculator = new CenterOfMassCalculator(elevator, rootJoint.getFrameAfterJoint());
      this.centerOfMassJacobianBody = new CenterOfMassJacobian(ScrewTools.computeRigidBodiesInOrder(elevator),
              ScrewTools.computeJointsInOrder(rootJoint.getSuccessor()), rootJoint.getFrameAfterJoint());
      
      //TODO: Should pass the input port for the spatial acceleration calculator here too...
      this.centerOfMassAccelerationCalculator = new CenterOfMassAccelerationCalculator(rootJoint.getSuccessor(), ScrewTools.computeRigidBodiesInOrder(elevator), spatialAccelerationCalculatorInputPort.getData());
      this.rootJoint = rootJoint;
   }

   public void run()
   {
      centerOfMassCalculator.compute();

      updateRootJointConfiguration();
      rootJoint.getFrameAfterJoint().update();

      TwistCalculator twistCalculator = twistCalculatorInputPort.getData();
      SpatialAccelerationCalculator spatialAccelerationCalculator = spatialAccelerationCalculatorInputPort.getData();
      
      updateRootJointTwistAndSpatialAcceleration(twistCalculator, spatialAccelerationCalculator);
      twistCalculator.compute();
      spatialAccelerationCalculator.compute();
   }

   private final Twist tempRootJointTwist = new Twist();
   private final FrameVector tempRootJointAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempRootJointAngularAcceleration = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempRootJointLinearVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempRootJointLinearAcceleration = new FrameVector(ReferenceFrame.getWorldFrame());
   private final SpatialAccelerationVector tempRootJointAcceleration = new SpatialAccelerationVector();

   private void updateRootJointTwistAndSpatialAcceleration(TwistCalculator twistCalculator, 
         SpatialAccelerationCalculator spatialAccelerationCalculator)
   {
      computeRootJointAngularVelocityAndAcceleration(twistCalculator, spatialAccelerationCalculator, tempRootJointAngularVelocity, tempRootJointAngularAcceleration);
      computeRootJointLinearVelocityAndAcceleration(tempRootJointLinearVelocity, tempRootJointLinearAcceleration, tempRootJointAngularVelocity, tempRootJointAngularAcceleration);

      computeRootJointTwist(tempRootJointTwist, tempRootJointAngularVelocity, tempRootJointLinearVelocity);
      rootJoint.setJointTwist(tempRootJointTwist);

      computeRootJointAcceleration(tempRootJointAcceleration, tempRootJointAngularAcceleration, tempRootJointLinearAcceleration);
      rootJoint.setAcceleration(tempRootJointAcceleration);
   }

   private final Twist tempRootToEstimationTwist = new Twist();
   private final SpatialAccelerationVector tempRootToEstimationAcceleration = new SpatialAccelerationVector();
   private final FrameVector tempRootToEstimationAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempRootToEstimationAngularAcceleration = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempCrossTerm = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempEstimationLinkAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());

   private void computeRootJointAngularVelocityAndAcceleration(TwistCalculator twistCalculator, 
         SpatialAccelerationCalculator spatialAccelerationCalculator, FrameVector rootJointAngularVelocityToPack, FrameVector rootJointAngularAccelerationToPack)
   {
      tempEstimationLinkAngularVelocity.setAndChangeFrame(angularVelocityPort.getData());
      
      // T_{root}^{root, estimation}
      twistCalculator.packRelativeTwist(tempRootToEstimationTwist, estimationLink, rootJoint.getSuccessor());
      tempRootToEstimationTwist.changeFrame(rootJoint.getFrameAfterJoint());

      // omega_{root}^{root, estimation}
      tempRootToEstimationAngularVelocity.setToZero(rootJoint.getFrameAfterJoint());
      tempRootToEstimationTwist.packAngularPart(tempRootToEstimationAngularVelocity);

      // omega_{estimation}^{root, world}
      tempEstimationLinkAngularVelocity.changeFrame(rootJoint.getFrameAfterJoint());

      // omega_{root}^{root, world} = omega_{estimation}^{root, world} + omega_{root}^{root, estimation}
      rootJointAngularVelocityToPack.setToZero(rootJoint.getFrameAfterJoint());
      rootJointAngularVelocityToPack.add(tempEstimationLinkAngularVelocity, tempRootToEstimationAngularVelocity);

      // R_{estimation}^{root} ( \omega_{estimation}^{estimation, root} \times \omega_{estimation}^{estimation, world} )
      tempRootToEstimationAngularVelocity.negate();
      tempRootToEstimationAngularVelocity.changeFrame(estimationFrame);
      tempEstimationLinkAngularVelocity.changeFrame(estimationFrame);
      tempCrossTerm.setToZero(estimationFrame);
      tempCrossTerm.cross(tempRootToEstimationAngularVelocity, tempEstimationLinkAngularVelocity);

      // \omega_{root}^{root, estimation}
      spatialAccelerationCalculator.packRelativeAcceleration(tempRootToEstimationAcceleration, estimationLink, rootJoint.getSuccessor());
      tempRootToEstimationAcceleration.changeFrameNoRelativeMotion(rootJoint.getFrameAfterJoint());
      tempRootToEstimationAcceleration.packAngularPart(tempRootToEstimationAngularAcceleration);
      tempRootToEstimationAngularAcceleration.changeFrame(estimationFrame);

      rootJointAngularAccelerationToPack.setAndChangeFrame(angularAccelerationPort.getData());
      rootJointAngularAccelerationToPack.add(tempCrossTerm);
      rootJointAngularAccelerationToPack.add(tempRootToEstimationAngularAcceleration);
      rootJointAngularAccelerationToPack.changeFrame(rootJoint.getFrameAfterJoint());
   }

   private final FramePoint tempComBody = new FramePoint();
   private final FrameVector tempComVelocityBody = new FrameVector();
   private final FrameVector tempComAccelerationBody = new FrameVector();
   private final FrameVector tempCenterOfMassVelocityOffset = new FrameVector();
   private final FrameVector tempCrossPart = new FrameVector();
   private final FrameVector tempAngularAcceleration = new FrameVector();
   private final FrameVector tempCenterOfMassVelocityWorld = new FrameVector(ReferenceFrame.getWorldFrame());

   private void computeRootJointLinearVelocityAndAcceleration(FrameVector rootJointVelocityToPack, FrameVector rootJointAccelerationToPack,
           FrameVector rootJointAngularVelocity, FrameVector rootJointAngularAcceleration)
   {
      tempCenterOfMassVelocityWorld.setAndChangeFrame(centerOfMassVelocityPort.getData());

      ReferenceFrame rootJointFrame = rootJoint.getFrameAfterJoint();

      // \dot{r}^{root}
      centerOfMassJacobianBody.compute();
      tempComVelocityBody.setToZero(rootJointFrame);
      centerOfMassJacobianBody.packCenterOfMassVelocity(tempComVelocityBody);
      tempComVelocityBody.changeFrame(rootJointFrame);

      // \tilde{\omega} r^{root}
      tempComBody.setToZero(rootJointFrame);
      centerOfMassCalculator.packCenterOfMass(tempComBody);
      tempComBody.changeFrame(rootJointFrame);
      tempCrossPart.setToZero(rootJointFrame);
      tempCrossPart.cross(rootJointAngularVelocity, tempComBody);

      // v_{r/p}= \tilde{\omega} r^{root} + \dot{r}^{root}
      tempCenterOfMassVelocityOffset.setToZero(rootJointFrame);
      tempCenterOfMassVelocityOffset.add(tempCrossPart, tempComVelocityBody);

      // v_{root}^{p,w} = R_{w}^{root} \dot{r} - v_{r/p}
      tempCenterOfMassVelocityWorld.changeFrame(rootJointFrame);
      rootJointVelocityToPack.setAndChangeFrame(tempCenterOfMassVelocityWorld);
      rootJointVelocityToPack.sub(tempCenterOfMassVelocityOffset);

      // R_{w}^{p} \ddot{r}
      rootJointAccelerationToPack.setAndChangeFrame(centerOfMassAccelerationPort.getData());
      rootJointAccelerationToPack.changeFrame(rootJointFrame);

      // -\tilde{\omega} R_{w}^{p} \dot{r}
      tempCrossPart.setToZero(rootJointFrame);
      tempCrossPart.cross(rootJointAngularVelocity, tempCenterOfMassVelocityWorld);
      rootJointAccelerationToPack.sub(tempCrossPart);
      
      // -\tilde{\dot{\omega}}r^{p}
      tempAngularAcceleration.setAndChangeFrame(rootJointAngularAcceleration);
      tempAngularAcceleration.changeFrame(rootJointFrame);
      tempCrossPart.cross(tempAngularAcceleration, tempComBody);
      rootJointAccelerationToPack.sub(tempCrossPart);
      
      // -\tilde{\omega} \dot{r}^{p}
      tempCrossPart.cross(rootJointAngularVelocity, tempComVelocityBody);
      rootJointAccelerationToPack.sub(tempCrossPart);
      
      // -\ddot{r}^{p}
      centerOfMassAccelerationCalculator.packCoMAcceleration(tempComAccelerationBody);
      tempComAccelerationBody.changeFrame(rootJointFrame);
      rootJointAccelerationToPack.sub(tempComAccelerationBody);
   }

   private void computeRootJointTwist(Twist rootJointTwistToPack, FrameVector rootJointAngularVelocity, FrameVector rootJointLinearVelocity)
   {
      rootJointAngularVelocity.checkReferenceFrameMatch(rootJoint.getFrameAfterJoint());
      rootJointLinearVelocity.checkReferenceFrameMatch(rootJoint.getFrameAfterJoint());
      rootJointTwistToPack.set(rootJoint.getFrameAfterJoint(), rootJoint.getFrameBeforeJoint(), rootJoint.getFrameAfterJoint(),
                               rootJointLinearVelocity.getVector(), rootJointAngularVelocity.getVector());
   }

   private void computeRootJointAcceleration(SpatialAccelerationVector rootJointAcceleration, FrameVector rootJointAngularAcceleration,
           FrameVector rootJointLinearAcceleration)
   {
      rootJointAngularAcceleration.checkReferenceFrameMatch(rootJoint.getFrameAfterJoint());
      rootJointLinearAcceleration.checkReferenceFrameMatch(rootJoint.getFrameAfterJoint());
      rootJointAcceleration.set(rootJoint.getFrameAfterJoint(), rootJoint.getFrameBeforeJoint(), rootJoint.getFrameAfterJoint(),
                                rootJointLinearAcceleration.getVector(), rootJointAngularAcceleration.getVector());
   }

   private final FramePoint tempCenterOfMassPositionState = new FramePoint(ReferenceFrame.getWorldFrame());
   private final FrameOrientation tempOrientationState = new FrameOrientation(ReferenceFrame.getWorldFrame());
   private final Transform3D tempEstimationLinkToWorld = new Transform3D();
   private final Transform3D tempRootJointToWorld = new Transform3D();

   private void updateRootJointConfiguration()
   {
      tempCenterOfMassPositionState.setAndChangeFrame(centerOfMassPositionPort.getData());
      tempOrientationState.setAndChangeFrame(orientationPort.getData());

      computeEstimationLinkTransform(tempEstimationLinkToWorld, tempCenterOfMassPositionState, tempOrientationState);
      computeRootJointTransform(tempRootJointToWorld, tempEstimationLinkToWorld);
      rootJoint.setPositionAndRotation(tempRootJointToWorld);
   }

   private final FramePoint tempCenterOfMassBody = new FramePoint(ReferenceFrame.getWorldFrame());
   private final Vector3d tempCenterOfMassBodyVector3d = new Vector3d();
   private final Point3d tempEstimationLinkPosition = new Point3d();
   private final Vector3d tempEstimationLinkPositionVector3d = new Vector3d();

   private void computeEstimationLinkTransform(Transform3D estimationLinkToWorldToPack, FramePoint centerOfMassWorld,
           FrameOrientation estimationLinkOrientation)
   {
      // r^{estimation}
      tempCenterOfMassBody.setToZero(estimationFrame);
      centerOfMassCalculator.packCenterOfMass(tempCenterOfMassBody);
      tempCenterOfMassBody.changeFrame(estimationFrame);

      // R_{estimation}^{w}
      estimationLinkOrientation.changeFrame(worldFrame);
      estimationLinkOrientation.getTransform3D(estimationLinkToWorldToPack);

      // R_{estimation}^{w} * r^{estimation}
      tempCenterOfMassBody.getVector(tempCenterOfMassBodyVector3d);
      estimationLinkToWorldToPack.transform(tempCenterOfMassBodyVector3d);

      // p_{estimation}^{w} = r^{w} - R_{estimation}^{w} r^{estimation}
      centerOfMassWorld.getPoint(tempEstimationLinkPosition);
      tempEstimationLinkPosition.sub(tempCenterOfMassBodyVector3d);

      // H_{estimation}^{w}
      tempEstimationLinkPositionVector3d.set(tempEstimationLinkPosition);
      estimationLinkToWorldToPack.setTranslation(tempEstimationLinkPositionVector3d);
   }

   private final Transform3D tempRootJointFrameToEstimationFrame = new Transform3D();

   private void computeRootJointTransform(Transform3D rootJointToWorldToPack, Transform3D estimationLinkTransform)
   {
      // H_{root}^{estimation}
      rootJoint.getFrameAfterJoint().getTransformToDesiredFrame(tempRootJointFrameToEstimationFrame, estimationFrame);

      // H_{root}^{w} = H_{estimation}^{w} * H_{root}^{estimation}
      rootJointToWorldToPack.set(estimationLinkTransform);
      rootJointToWorldToPack.mul(tempRootJointFrameToEstimationFrame);
   }
}
