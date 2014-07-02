package us.ihmc.sensorProcessing.stateEstimation;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;

import us.ihmc.controlFlow.AbstractControlFlowElement;
import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.controlFlow.ControlFlowPort;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

public class OrientationStateRobotModelUpdater extends AbstractControlFlowElement implements Runnable
{
   private final ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort;
   private final ControlFlowPort<FrameOrientation> orientationPort;
   private final ControlFlowPort<FrameVector> angularVelocityPort;

   private final ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   // Constructor in case of use as a ControlFlowElement
   public OrientationStateRobotModelUpdater(ControlFlowGraph controlFlowGraph,
           ControlFlowOutputPort<FullInverseDynamicsStructure> inverseDynamicsStructureOutputPort,
           ControlFlowOutputPort<FrameOrientation> orientationOutputPort, ControlFlowOutputPort<FrameVector> angularVelocityOutputPort)
   {
      this.orientationPort = createInputPort("orientationInputPort");
      this.angularVelocityPort = createInputPort("angularVelocityInputPort");
      this.inverseDynamicsStructureInputPort = createInputPort("inverseDynamicsStructureInputPort");
      this.inverseDynamicsStructureOutputPort = createOutputPort("inverseDynamicsStructureOutputPort");

      controlFlowGraph.connectElements(inverseDynamicsStructureOutputPort, inverseDynamicsStructureInputPort);
      controlFlowGraph.connectElements(orientationOutputPort, (ControlFlowInputPort<FrameOrientation>) orientationPort);
      controlFlowGraph.connectElements(angularVelocityOutputPort, (ControlFlowInputPort<FrameVector>) angularVelocityPort);

      this.inverseDynamicsStructureInputPort.setData(inverseDynamicsStructureOutputPort.getData());
      this.inverseDynamicsStructureOutputPort.setData(inverseDynamicsStructureInputPort.getData());
   }

   // Constructor in case of use as a Runnable
   public OrientationStateRobotModelUpdater(ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort,
           ControlFlowOutputPort<FrameOrientation> orientationPort, ControlFlowOutputPort<FrameVector> angularVelocityPort)
   {
      this.inverseDynamicsStructureInputPort = inverseDynamicsStructureInputPort;
      this.inverseDynamicsStructureOutputPort = null;
      this.orientationPort = orientationPort;
      this.angularVelocityPort = angularVelocityPort;
   }

   public void run()
   {
      FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
      SixDoFJoint rootJoint = inverseDynamicsStructure.getRootJoint();

      ReferenceFrame estimationFrame = inverseDynamicsStructure.getEstimationFrame();
      updateRootJointRotation(inverseDynamicsStructure.getRootJoint(), orientationPort.getData(), estimationFrame);

      rootJoint.getFrameAfterJoint().update();

      TwistCalculator twistCalculator = inverseDynamicsStructure.getTwistCalculator();
      updateRootJointTwistAngularPart(twistCalculator, rootJoint, angularVelocityPort.getData());
      rootJoint.getFrameAfterJoint().update();
      twistCalculator.compute();
   }

   public void startComputation()
   {
      run();
      inverseDynamicsStructureOutputPort.setData(inverseDynamicsStructureInputPort.getData());
   }

   public void waitUntilComputationIsDone()
   {
   }

   private final FrameVector tempRootJointAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final Twist tempRootJointTwist = new Twist();

   private void updateRootJointTwistAngularPart(TwistCalculator twistCalculator, SixDoFJoint rootJoint, FrameVector estimationLinkAngularVelocity)
   {
      rootJoint.packJointTwist(tempRootJointTwist);
      computeRootJointAngularVelocity(twistCalculator, tempRootJointAngularVelocity, estimationLinkAngularVelocity);

      tempRootJointTwist.setAngularPart(tempRootJointAngularVelocity.getVector());
      rootJoint.setJointTwist(tempRootJointTwist);
   }

   private final Twist tempRootToEstimationTwist = new Twist();
   private final FrameVector tempRootToEstimationAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
   private final FrameVector tempEstimationLinkAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());

   private void computeRootJointAngularVelocity(TwistCalculator twistCalculator, FrameVector rootJointAngularVelocityToPack,
           FrameVector angularVelocityEstimationLink)
   {
      FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
      SixDoFJoint rootJoint = inverseDynamicsStructure.getRootJoint();
      RigidBody estimationLink = inverseDynamicsStructure.getEstimationLink();

      tempEstimationLinkAngularVelocity.setIncludingFrame(angularVelocityEstimationLink);

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
   }

   private final FrameOrientation tempOrientationEstimatinLink = new FrameOrientation(ReferenceFrame.getWorldFrame());    // worldframe just for initializing
   private final Transform3D tempEstimationLinkToWorld = new Transform3D();
   private final Transform3D tempRootJointToWorld = new Transform3D();

   private void updateRootJointRotation(SixDoFJoint rootJoint, FrameOrientation estimationLinkOrientation, ReferenceFrame estimationFrame)
   {
      tempOrientationEstimatinLink.setIncludingFrame(estimationLinkOrientation);

      computeEstimationLinkToWorldTransform(tempEstimationLinkToWorld, tempOrientationEstimatinLink);
      computeRootJointToWorldTransform(rootJoint, estimationFrame, tempRootJointToWorld, tempEstimationLinkToWorld);
      Matrix3d rootJointRotation = new Matrix3d();
      tempRootJointToWorld.get(rootJointRotation);

      rootJoint.setRotation(rootJointRotation);
   }

   private void computeEstimationLinkToWorldTransform(Transform3D estimationLinkToWorldToPack, FrameOrientation estimationLinkOrientation)
   {
      // R_{estimation}^{w}
      estimationLinkOrientation.changeFrame(worldFrame);
      estimationLinkOrientation.getTransform3D(estimationLinkToWorldToPack);
   }

   private final Transform3D tempRootJointFrameToEstimationFrame = new Transform3D();

   private void computeRootJointToWorldTransform(SixDoFJoint rootJoint, ReferenceFrame estimationFrame, Transform3D rootJointToWorldToPack,
           Transform3D estimationLinkTransform)
   {
      // H_{root}^{estimation}
      rootJoint.getFrameAfterJoint().getTransformToDesiredFrame(tempRootJointFrameToEstimationFrame, estimationFrame);

      // H_{root}^{w} = H_{estimation}^{w} * H_{root}^{estimation}
      rootJointToWorldToPack.set(estimationLinkTransform);
      rootJointToWorldToPack.mul(tempRootJointFrameToEstimationFrame);
   }

// private final Twist tempRootJointTwistExisting = new Twist();
// private final FrameVector tempRootJointTwistExistingLinearPart = new FrameVector();

// private void computeRootJointTwistAngularPart(SixDoFJoint rootJoint, Twist rootJointTwistToPack, FrameVector rootJointAngularVelocity)
// {
//    rootJointAngularVelocity.checkReferenceFrameMatch(rootJoint.getFrameAfterJoint());
//
//    rootJoint.packJointTwist(tempRootJointTwistExisting);
//    tempRootJointTwistExisting.checkReferenceFramesMatch(rootJoint.getFrameAfterJoint(), rootJoint.getFrameBeforeJoint(), rootJoint.getFrameAfterJoint());
//    tempRootJointTwistExisting.packLinearPart(tempRootJointTwistExistingLinearPart);
//
//    rootJointTwistToPack.set(rootJoint.getFrameAfterJoint(), rootJoint.getFrameBeforeJoint(), rootJoint.getFrameAfterJoint(),
//                             tempRootJointTwistExistingLinearPart.getVector(), rootJointAngularVelocity.getVector());
// }

   public ControlFlowOutputPort<FullInverseDynamicsStructure> getInverseDynamicsStructureOutputPort()
   {
      return inverseDynamicsStructureOutputPort;
   }

   public void initialize()
   {
      startComputation();
      waitUntilComputationIsDone();
   }

   public void initializeOrientionToActual(FrameOrientation actualOrientation)
   {
      FullInverseDynamicsStructure inverseDynamicsStructure = inverseDynamicsStructureInputPort.getData();
      updateRootJointRotation(inverseDynamicsStructure.getRootJoint(), actualOrientation, inverseDynamicsStructure.getEstimationFrame());
   }
}
