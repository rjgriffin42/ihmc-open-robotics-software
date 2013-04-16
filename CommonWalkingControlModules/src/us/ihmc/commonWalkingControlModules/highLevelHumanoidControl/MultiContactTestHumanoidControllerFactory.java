package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl;

import java.util.HashMap;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ListOfPointsContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.calculators.GainCalculator;
import us.ihmc.commonWalkingControlModules.controlModules.ContactPointGroundReactionWrenchDistributor;
import us.ihmc.commonWalkingControlModules.controllers.HandControllerInterface;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CoMBasedMomentumRateOfChangeControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.RootJointAngularAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.outputs.ProcessedOutputsInterface;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.FingerForceSensors;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.TotalMassCalculator;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.gui.GUISetterUpperRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class MultiContactTestHumanoidControllerFactory implements HighLevelHumanoidControllerFactory
{
   private final SideDependentList<String> namesOfJointsBeforeHands;
   private final SideDependentList<Transform3D> handContactPointTransforms;
   private final SideDependentList<List<Point2d>> handContactPoints;
   private final RobotSide[] footContactSides;
   private final RobotSide[] handContactSides;

   public MultiContactTestHumanoidControllerFactory(SideDependentList<String> namesOfJointsBeforeHands,
           SideDependentList<Transform3D> handContactPointTransforms, SideDependentList<List<Point2d>> handContactPoints, RobotSide[] footContactSides,
           RobotSide[] handContactSides)
   {
      this.namesOfJointsBeforeHands = namesOfJointsBeforeHands;
      this.handContactPointTransforms = handContactPointTransforms;
      this.handContactPoints = handContactPoints;
      this.footContactSides = footContactSides;
      this.handContactSides = handContactSides;
   }

   public MomentumBasedController create(RigidBody estimationLink, ReferenceFrame estimationFrame, FullRobotModel fullRobotModel, CommonWalkingReferenceFrames referenceFrames, FingerForceSensors fingerForceSensors,
                                 DoubleYoVariable yoTime, double gravityZ, TwistCalculator twistCalculator, CenterOfMassJacobian centerOfMassJacobian,
                                 SideDependentList<ContactablePlaneBody> feet, double controlDT, SideDependentList<FootSwitchInterface> footSwitches,
                                 SideDependentList<HandControllerInterface> handControllers, OneDoFJoint lidarJoint, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
                                 YoVariableRegistry registry, GUISetterUpperRegistry guiSetterUpperRegistry, ProcessedOutputsInterface processedOutputs)
   {
      HashMap<ContactablePlaneBody, RigidBody> contactablePlaneBodiesAndBases = new HashMap<ContactablePlaneBody, RigidBody>();

      // TODO: code duplication from driving controller
      InverseDynamicsJoint[] allJoints = ScrewTools.computeJointsInOrder(fullRobotModel.getElevator());
      SideDependentList<ContactablePlaneBody> hands = new SideDependentList<ContactablePlaneBody>();
      for (RobotSide robotSide : RobotSide.values())
      {
         InverseDynamicsJoint[] jointBeforeHandArray = ScrewTools.findJointsWithNames(allJoints, namesOfJointsBeforeHands.get(robotSide));
         if (jointBeforeHandArray.length != 1)
            throw new RuntimeException("Incorrect number of joints before hand found: " + jointBeforeHandArray.length);

         RigidBody handBody = jointBeforeHandArray[0].getSuccessor();

         ReferenceFrame afterHipFrame = handBody.getParentJoint().getFrameAfterJoint();
         ReferenceFrame handContactsFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(robotSide.getCamelCaseNameForStartOfExpression()
                                               + "HandContact", afterHipFrame, handContactPointTransforms.get(robotSide));

         ContactablePlaneBody hand = new ListOfPointsContactablePlaneBody(handBody, handContactsFrame, handContactPoints.get(robotSide));
         hands.put(robotSide, hand);
      }

      for (ContactablePlaneBody contactablePlaneBody : feet.values())
      {
         contactablePlaneBodiesAndBases.put(contactablePlaneBody, fullRobotModel.getPelvis());
      }

      for (ContactablePlaneBody contactablePlaneBody : hands.values())
      {
         contactablePlaneBodiesAndBases.put(contactablePlaneBody, fullRobotModel.getChest());
      }

      ContactPointGroundReactionWrenchDistributor groundReactionWrenchDistributor =
         new ContactPointGroundReactionWrenchDistributor(referenceFrames.getCenterOfMassFrame(), registry);
      double[] diagonalCWeights = new double[]
      {
         1.0, 1.0, 1.0, 1.0, 1.0, 1.0
      };
      groundReactionWrenchDistributor.setWeights(diagonalCWeights, 1.0, 0.001);

      CoMBasedMomentumRateOfChangeControlModule momentumRateOfChangeControlModule =
         new CoMBasedMomentumRateOfChangeControlModule(referenceFrames.getCenterOfMassFrame(), centerOfMassJacobian, registry);

      double comProportionalGain = 100.0;
      double dampingRatio = 1.0;
      double totalMass = TotalMassCalculator.computeSubTreeMass(fullRobotModel.getElevator());
      double comDerivativeGain = GainCalculator.computeDampingForSecondOrderSystem(totalMass, comProportionalGain, dampingRatio);
      momentumRateOfChangeControlModule.setProportionalGains(comProportionalGain, comProportionalGain, comProportionalGain);
      momentumRateOfChangeControlModule.setDerivativeGains(comDerivativeGain, comDerivativeGain, comDerivativeGain);

      RootJointAngularAccelerationControlModule rootJointAccelerationControlModule =
         new RootJointAngularAccelerationControlModule(fullRobotModel.getRootJoint(), twistCalculator, registry);
      rootJointAccelerationControlModule.setProportionalGains(100.0, 100.0, 100.0);
      rootJointAccelerationControlModule.setDerivativeGains(20.0, 20.0, 20.0);

      double groundReactionWrenchBreakFrequencyHertz = 7.0;
      MultiContactTestHumanoidController ret = new MultiContactTestHumanoidController(estimationLink, estimationFrame, fullRobotModel, centerOfMassJacobian, referenceFrames, yoTime, gravityZ,
                                                  twistCalculator, contactablePlaneBodiesAndBases, controlDT, processedOutputs,
                                                  groundReactionWrenchDistributor, null, momentumRateOfChangeControlModule, rootJointAccelerationControlModule,
                                                  groundReactionWrenchBreakFrequencyHertz, momentumRateOfChangeControlModule.getDesiredCoMPositionInputPort(),
                                                  rootJointAccelerationControlModule.getDesiredPelvisOrientationTrajectoryInputPort(),
                                                  dynamicGraphicObjectsListRegistry);



      double coefficientOfFriction = 1.0;

      for (ContactablePlaneBody contactablePlaneBody : contactablePlaneBodiesAndBases.keySet())
      {
         ret.setContactablePlaneBodiesInContact(contactablePlaneBody, false, coefficientOfFriction);
      }

      for (RobotSide robotSide : footContactSides)
      {
         ret.setContactablePlaneBodiesInContact(feet.get(robotSide), true, coefficientOfFriction);
      }

      for (RobotSide robotSide : handContactSides)
      {
         ret.setContactablePlaneBodiesInContact(hands.get(robotSide), true, coefficientOfFriction);
      }

      return ret;
   }

}
