package us.ihmc.commonWalkingControlModules.controlModules.foot;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameMatrix3D;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.SpatialMotionVector;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.controllers.YoOrientationPIDGains;
import us.ihmc.yoUtilities.controllers.YoSE3PIDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;

public class FootControlHelper
{
   private static final double EPSILON_POINT_ON_EDGE = 5e-3;
   private static final double minJacobianDeterminant = 0.035;

   private final RobotSide robotSide;
   private final RigidBody rootBody;
   private final ContactablePlaneBody contactableFoot;
   private final MomentumBasedController momentumBasedController;
   private final TwistCalculator twistCalculator;
   private final RigidBodySpatialAccelerationControlModule accelerationControlModule;
   private final WalkingControllerParameters walkingControllerParameters;
   private final PartialFootholdControlModule partialFootholdControlModule;

   private final int jacobianId;
   private final GeometricJacobian jacobian;
   private final EnumYoVariable<ConstraintType> requestedState;
   private final FrameVector fullyConstrainedNormalContactVector;
   private final BooleanYoVariable isDesiredCoPOnEdge;

   private final FrameConvexPolygon2d contactPolygon = new FrameConvexPolygon2d();

   private final DenseMatrix64F nullspaceMultipliers = new DenseMatrix64F(0, 1);
   private final BooleanYoVariable doSingularityEscape;
   private final DoubleYoVariable singularityEscapeNullspaceMultiplier;
   private final DoubleYoVariable nullspaceMultiplier;
   private final DoubleYoVariable jacobianDeterminant;
   private final BooleanYoVariable jacobianDeterminantInRange;

   private final LegSingularityAndKneeCollapseAvoidanceControlModule legSingularityAndKneeCollapseAvoidanceControlModule;

   private final DenseMatrix64F selectionMatrix;
   private final TaskspaceConstraintData taskspaceConstraintData = new TaskspaceConstraintData();

   private final BooleanYoVariable isFootRollUncontrollable;

   private final FrameVector hipYawAxis = new FrameVector();
   private final FrameVector ankleRollAxis = new FrameVector();

   private final DoubleYoVariable ankleRollAndHipYawAlignmentFactor;
   private final DoubleYoVariable ankleRollAndHipYawAlignmentTreshold;
   private final FrameMatrix3D angularSelectionMatrix = new FrameMatrix3D();

   public FootControlHelper(RobotSide robotSide, WalkingControllerParameters walkingControllerParameters, MomentumBasedController momentumBasedController,
         YoVariableRegistry registry)
   {
      this.robotSide = robotSide;
      this.momentumBasedController = momentumBasedController;
      this.walkingControllerParameters = walkingControllerParameters;

      contactableFoot = momentumBasedController.getContactableFeet().get(robotSide);
      twistCalculator = momentumBasedController.getTwistCalculator();

      RigidBody foot = contactableFoot.getRigidBody();
      String namePrefix = foot.getName();
      ReferenceFrame frameAfterAnkle = contactableFoot.getFrameAfterParentJoint();
      double controlDT = momentumBasedController.getControlDT();

      accelerationControlModule = new RigidBodySpatialAccelerationControlModule(namePrefix, twistCalculator, foot, frameAfterAnkle, controlDT, registry);

      partialFootholdControlModule = new PartialFootholdControlModule(namePrefix, controlDT, contactableFoot, twistCalculator, walkingControllerParameters,
            registry, momentumBasedController.getDynamicGraphicObjectsListRegistry());


      FullRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();
      RigidBody pelvis = fullRobotModel.getPelvis();
      jacobianId = momentumBasedController.getOrCreateGeometricJacobian(pelvis, foot, foot.getBodyFixedFrame());
      jacobian = momentumBasedController.getJacobian(jacobianId);

      requestedState = EnumYoVariable.create(namePrefix + "RequestedState", "", ConstraintType.class, registry, true);

      isDesiredCoPOnEdge = new BooleanYoVariable(namePrefix + "IsDesiredCoPOnEdge", registry);

      fullyConstrainedNormalContactVector = new FrameVector(contactableFoot.getSoleFrame(), 0.0, 0.0, 1.0);

      contactPolygon.setIncludingFrameAndUpdate(contactableFoot.getContactPoints2d());

      doSingularityEscape = new BooleanYoVariable(namePrefix + "DoSingularityEscape", registry);
      jacobianDeterminant = new DoubleYoVariable(namePrefix + "JacobianDeterminant", registry);
      jacobianDeterminantInRange = new BooleanYoVariable(namePrefix + "JacobianDeterminantInRange", registry);
      nullspaceMultiplier = new DoubleYoVariable(namePrefix + "NullspaceMultiplier", registry);
      singularityEscapeNullspaceMultiplier = new DoubleYoVariable(namePrefix + "SingularityEscapeNullspaceMultiplier", registry);
      isFootRollUncontrollable = new BooleanYoVariable(namePrefix + "IsFootRollUncontrollable", registry);
      ankleRollAndHipYawAlignmentFactor = new DoubleYoVariable(namePrefix + "AkleRollAndHipYawAlignmentFactor", registry);
      ankleRollAndHipYawAlignmentTreshold = new DoubleYoVariable(namePrefix + "AkleRollAndHipYawAlignmentThreshold", registry);
      ankleRollAndHipYawAlignmentTreshold.set(0.9);

      legSingularityAndKneeCollapseAvoidanceControlModule = new LegSingularityAndKneeCollapseAvoidanceControlModule(namePrefix, contactableFoot, robotSide,
            walkingControllerParameters, momentumBasedController, registry);

      selectionMatrix = new DenseMatrix64F(SpatialMotionVector.SIZE, SpatialMotionVector.SIZE);
      CommonOps.setIdentity(selectionMatrix);
      rootBody = twistCalculator.getRootBody();
      taskspaceConstraintData.set(rootBody, contactableFoot.getRigidBody());
   }

   public void update()
   {
      FramePoint2d cop = momentumBasedController.getDesiredCoP(contactableFoot);

      if (cop == null || cop.containsNaN())
         isDesiredCoPOnEdge.set(false);
      else
         isDesiredCoPOnEdge.set(!contactPolygon.isPointInside(cop, -EPSILON_POINT_ON_EDGE)); // Minus means that the check is done with a smaller polygon
   
      computeNullspaceMultipliers();
   }

   public boolean isCoPOnEdge()
   {
      return isDesiredCoPOnEdge.getBooleanValue();
   }

   public void computeNullspaceMultipliers()
   {
      jacobianDeterminant.set(jacobian.det());
      jacobianDeterminantInRange.set(Math.abs(jacobianDeterminant.getDoubleValue()) < minJacobianDeterminant);
      computeJointsAlignmentFactor();

      if (jacobianDeterminantInRange.getBooleanValue() && ankleRollAndHipYawAlignmentFactor.getDoubleValue() < ankleRollAndHipYawAlignmentTreshold.getDoubleValue())
      {
         nullspaceMultipliers.reshape(1, 1);
         if (doSingularityEscape.getBooleanValue())
         {
            nullspaceMultipliers.set(0, nullspaceMultiplier.getDoubleValue());
         }
         else
         {
            nullspaceMultipliers.set(0, 0);
         }
      }
      else
      {
         nullspaceMultiplier.set(Double.NaN);
         nullspaceMultipliers.reshape(0, 1);
         doSingularityEscape.set(false);
      }
   }

   private void computeJointsAlignmentFactor()
   {
      FullRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();
      hipYawAxis.setIncludingFrame(fullRobotModel.getLegJoint(robotSide, LegJointName.HIP_YAW).getJointAxis());
      ankleRollAxis.setIncludingFrame(fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_ROLL).getJointAxis());

      ankleRollAxis.changeFrame(hipYawAxis.getReferenceFrame());
      ankleRollAndHipYawAlignmentFactor.set(Math.abs(ankleRollAxis.dot(hipYawAxis)));
   }

   public void submitTaskspaceConstraint(SpatialAccelerationVector footAcceleration)
   {
      submitTaskspaceConstraint(jacobianId, footAcceleration);
   }

   public void submitTaskspaceConstraint(int jacobianId, SpatialAccelerationVector footAcceleration)
   {
      ReferenceFrame bodyFixedFrame = contactableFoot.getRigidBody().getBodyFixedFrame();
      footAcceleration.changeBodyFrameNoRelativeAcceleration(bodyFixedFrame);
      footAcceleration.changeFrameNoRelativeMotion(bodyFixedFrame);
      taskspaceConstraintData.set(footAcceleration, nullspaceMultipliers, selectionMatrix);
      momentumBasedController.setDesiredSpatialAcceleration(jacobianId, taskspaceConstraintData);
   }

   public void updateSelectionMatrixToHandleAnkleRollAndHipYawAlignment()
   {
      if (ankleRollAndHipYawAlignmentFactor.getDoubleValue() > ankleRollAndHipYawAlignmentTreshold.getDoubleValue())
      {
         isFootRollUncontrollable.set(true);
         ReferenceFrame footFrame = contactableFoot.getFrameAfterParentJoint();
         ReferenceFrame jacobianFrame = jacobian.getJacobianFrame();
         angularSelectionMatrix.setToIdentity(footFrame);
         double s22 = 10.0 * (1.0 - ankleRollAndHipYawAlignmentFactor.getDoubleValue());
         angularSelectionMatrix.setM22(s22);
         angularSelectionMatrix.changeFrame(jacobianFrame);
         angularSelectionMatrix.getDenseMatrix(selectionMatrix, 0, 0);
         OneDoFJoint ankleRollJoint = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.ANKLE_ROLL);
         momentumBasedController.doPDControl(ankleRollJoint, 1.0, 0.0, 0.0, 0.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      }
      else
      {
         isFootRollUncontrollable.set(false);
         selectionMatrix.set(0, 0, 1.0);
         selectionMatrix.set(1, 1, 1.0);
         selectionMatrix.set(2, 2, 1.0);
      }
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public ContactablePlaneBody getContactableFoot()
   {
      return contactableFoot;
   }

   public MomentumBasedController getMomentumBasedController()
   {
      return momentumBasedController;
   }

   public RigidBodySpatialAccelerationControlModule getAccelerationControlModule()
   {
      return accelerationControlModule;
   }

   public void setGains(YoSE3PIDGains gains)
   {
      accelerationControlModule.setGains(gains);
   }

   public void setOrientationGains(YoOrientationPIDGains gains)
   {
      accelerationControlModule.setOrientationGains(gains);
   }

   public void setGainsToZero()
   {
      accelerationControlModule.setPositionProportionalGains(0.0, 0.0, 0.0);
      accelerationControlModule.setPositionDerivativeGains(0.0, 0.0, 0.0);
      accelerationControlModule.setPositionIntegralGains(0.0, 0.0, 0.0, 0.0);
      accelerationControlModule.setPositionMaxAccelerationAndJerk(0.0, 0.0);
      accelerationControlModule.setOrientationProportionalGains(0.0, 0.0, 0.0);
      accelerationControlModule.setOrientationDerivativeGains(0.0, 0.0, 0.0);
      accelerationControlModule.setOrientationIntegralGains(0.0, 0.0, 0.0, 0.0);
      accelerationControlModule.setOrientationMaxAccelerationAndJerk(0.0, 0.0);
   }

   public void resetAccelerationControlModule()
   {
      accelerationControlModule.reset();
   }

   public WalkingControllerParameters getWalkingControllerParameters()
   {
      return walkingControllerParameters;
   }

   public PartialFootholdControlModule getPartialFootholdControlModule()
   {
      return partialFootholdControlModule;
   }

   public int getJacobianId()
   {
      return jacobianId;
   }

   public GeometricJacobian getJacobian()
   {
      return jacobian;
   }

   public void requestState(ConstraintType requestedState)
   {
      this.requestedState.set(requestedState);
   }

   public ConstraintType getRequestedState()
   {
      return requestedState.getEnumValue();
   }

   public void setRequestedStateAsProcessed()
   {
      requestedState.set(null);
   }

   public void setFullyConstrainedNormalContactVector(FrameVector normalContactVector)
   {
      if (normalContactVector != null)
         fullyConstrainedNormalContactVector.setIncludingFrame(normalContactVector);
      else
         fullyConstrainedNormalContactVector.setIncludingFrame(contactableFoot.getSoleFrame(), 0.0, 0.0, 1.0);
   }

   public FrameVector getFullyConstrainedNormalContactVector()
   {
      return fullyConstrainedNormalContactVector;
   }

   public DenseMatrix64F getNullspaceMultipliers()
   {
      return nullspaceMultipliers;
   }

   public void resetNullspaceMultipliers()
   {
      nullspaceMultipliers.reshape(0, 1);
   }

   public boolean isDoingSingularityEscape()
   {
      return doSingularityEscape.getBooleanValue();
   }

   public void resetSingularityEscape()
   {
      doSingularityEscape.set(false);
   }

   public void doSingularityEscape(boolean doSingularityEscape)
   {
      this.doSingularityEscape.set(doSingularityEscape);
      this.nullspaceMultiplier.set(singularityEscapeNullspaceMultiplier.getDoubleValue());
   }

   public void doSingularityEscape(double temporarySingularityEscapeNullspaceMultiplier)
   {
      doSingularityEscape.set(true);
      this.nullspaceMultiplier.set(temporarySingularityEscapeNullspaceMultiplier);
   }

   public void setNullspaceMultiplier(double singularityEscapeNullspaceMultiplier)
   {
      this.singularityEscapeNullspaceMultiplier.set(singularityEscapeNullspaceMultiplier);
   }

   public double getJacobianDeterminant()
   {
      return jacobianDeterminant.getDoubleValue();
   }

   public boolean isJacobianDeterminantInRange()
   {
      return jacobianDeterminantInRange.getBooleanValue();
   }

   public LegSingularityAndKneeCollapseAvoidanceControlModule getLegSingularityAndKneeCollapseAvoidanceControlModule()
   {
      return legSingularityAndKneeCollapseAvoidanceControlModule;
   }

   public DenseMatrix64F getSelectionMatrix()
   {
      return selectionMatrix;
   }

   public void resetSelectionMatrix()
   {
      selectionMatrix.reshape(SpatialMotionVector.SIZE, SpatialMotionVector.SIZE);
      CommonOps.setIdentity(selectionMatrix);
   }
}
