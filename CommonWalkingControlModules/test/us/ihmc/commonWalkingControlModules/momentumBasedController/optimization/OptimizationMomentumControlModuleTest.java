package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.EjmlUnitTests;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.RectangularContactableBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization.MomentumOptimizerNative;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.ContactPointWrenchMatrixCalculator;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.CenterOfMassReferenceFrame;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.*;
import us.ihmc.utilities.test.JUnitTools;

import javax.vecmath.Vector3d;
import java.util.*;

import static junit.framework.Assert.assertTrue;

/**
 * @author twan
 *         Date: 5/4/13
 */
public class OptimizationMomentumControlModuleTest
{
   private static final Vector3d X = new Vector3d(1.0, 0.0, 0.0);
   private static final Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
   private static final Vector3d Z = new Vector3d(0.0, 0.0, 1.0);

   private final double controlDT = 5e-3;
   private final double gravityZ = 9.81;

   @Test
   public void testMomentumAndJointSpaceConstraints()
   {
      Random random = new Random(1223525L);
      Vector3d[] jointAxes = new Vector3d[]
      {
         X, Y, Z, Z, X, Y, X, Y
      };
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      randomFloatingChain.setRandomPositionsAndVelocities(random);

      InverseDynamicsJoint rootJoint = randomFloatingChain.getRootJoint();
      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", ReferenceFrame.getWorldFrame(), rootJoint.getSuccessor());
      centerOfMassFrame.update();

      MomentumOptimizationSettings momentumOptimizationSettings = createStandardOptimizationSettings();
      OptimizationMomentumControlModule momentumControlModule = createMomentumControlModule(rootJoint, centerOfMassFrame, momentumOptimizationSettings);

      LinkedHashMap<ContactablePlaneBody, PlaneContactState> contactStates = new LinkedHashMap<ContactablePlaneBody, PlaneContactState>();
      double coefficientOfFriction = 1.0;
      addContactState(coefficientOfFriction, randomFloatingChain.getLeafBody(), contactStates);

      MomentumRateOfChangeData momentumRateOfChangeData = new MomentumRateOfChangeData(centerOfMassFrame);
      double totalMass = TotalMassCalculator.computeSubTreeMass(randomFloatingChain.getElevator());
      SpatialForceVector momentumRateOfChangeIn = generateRandomFeasibleMomentumRateOfChange(centerOfMassFrame, contactStates, totalMass, gravityZ, random,
                                                     momentumOptimizationSettings.getRhoMinScalar());
      momentumRateOfChangeData.set(momentumRateOfChangeIn);
      momentumControlModule.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);

      List<RevoluteJoint> revoluteJoints = randomFloatingChain.getRevoluteJoints();
      InverseDynamicsJoint[] revoluteJointsArray = revoluteJoints.toArray(new InverseDynamicsJoint[revoluteJoints.size()]);
      DenseMatrix64F desiredJointAccelerations = setRandomJointAccelerations(random, momentumControlModule, revoluteJoints);

      momentumControlModule.compute(contactStates, null);

      SpatialForceVector momentumRateOfChangeOut = momentumControlModule.getDesiredCentroidalMomentumRate();
      DenseMatrix64F jointAccelerationsBack = new DenseMatrix64F(desiredJointAccelerations.getNumRows(), 1);
      ScrewTools.packDesiredJointAccelerationsMatrix(revoluteJointsArray, jointAccelerationsBack);

      assertWrenchesSumUpToMomentumDot(momentumControlModule.getExternalWrenches(), momentumRateOfChangeOut, gravityZ, totalMass, centerOfMassFrame);
      assertWrenchesInFrictionCones(momentumControlModule.getExternalWrenches(), contactStates, coefficientOfFriction);
      JUnitTools.assertSpatialForceVectorEquals(momentumRateOfChangeIn, momentumRateOfChangeOut, 1e-3);
      EjmlUnitTests.assertEquals(desiredJointAccelerations, jointAccelerationsBack, 1e-9);
   }

   @Test
   public void testMomentumAndTaskSpaceConstraints()
   {
      Random random = new Random(1223525L);
      Vector3d[] jointAxes = new Vector3d[]
      {
         X, Y, Z, Y, Y, X, Z, Y, X, X, Y, Z
      };
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      randomFloatingChain.setRandomPositionsAndVelocities(random);

      InverseDynamicsJoint rootJoint = randomFloatingChain.getRootJoint();
      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", ReferenceFrame.getWorldFrame(), rootJoint.getSuccessor());
      centerOfMassFrame.update();

      MomentumOptimizationSettings momentumOptimizationSettings = createStandardOptimizationSettings();
      momentumOptimizationSettings.setRhoMin(-10000.0);
      OptimizationMomentumControlModule momentumControlModule = createMomentumControlModule(rootJoint, centerOfMassFrame, momentumOptimizationSettings);

      LinkedHashMap<ContactablePlaneBody, PlaneContactState> contactStates = new LinkedHashMap<ContactablePlaneBody, PlaneContactState>();
      RigidBody endEffector = randomFloatingChain.getLeafBody();
      double coefficientOfFriction = 1000.0;
      addContactState(coefficientOfFriction, endEffector, contactStates);

      MomentumRateOfChangeData momentumRateOfChangeData = new MomentumRateOfChangeData(centerOfMassFrame);
      RigidBody elevator = randomFloatingChain.getElevator();
      double totalMass = TotalMassCalculator.computeSubTreeMass(elevator);
      SpatialForceVector momentumRateOfChangeIn = generateRandomFeasibleMomentumRateOfChange(centerOfMassFrame, contactStates, totalMass, gravityZ, random,
                                                     0.0);
      momentumRateOfChangeData.set(momentumRateOfChangeIn);
      momentumControlModule.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);

      RigidBody base = elevator; // rootJoint.getSuccessor();
      GeometricJacobian jacobian = new GeometricJacobian(base, endEffector, endEffector.getBodyFixedFrame());
      jacobian.compute();
      TaskspaceConstraintData taskSpaceConstraintData = new TaskspaceConstraintData();
      SpatialAccelerationVector endEffectorSpatialAcceleration = new SpatialAccelerationVector(endEffector.getBodyFixedFrame(), base.getBodyFixedFrame(),
                                                                    endEffector.getBodyFixedFrame());
      endEffectorSpatialAcceleration.setAngularPart(RandomTools.generateRandomVector(random));
      endEffectorSpatialAcceleration.setLinearPart(RandomTools.generateRandomVector(random));
      taskSpaceConstraintData.set(endEffectorSpatialAcceleration);
      momentumControlModule.setDesiredSpatialAcceleration(jacobian, taskSpaceConstraintData); // , 10.0);

      momentumControlModule.compute(contactStates, null);
      SpatialForceVector momentumRateOfChangeOut = momentumControlModule.getDesiredCentroidalMomentumRate();

      TwistCalculator twistCalculator = new TwistCalculator(elevator.getBodyFixedFrame(), elevator);
      SpatialAccelerationCalculator spatialAccelerationCalculator = createSpatialAccelerationCalculator(twistCalculator, elevator);
      twistCalculator.compute();
      spatialAccelerationCalculator.compute();
      SpatialAccelerationVector endEffectorAccelerationBack = new SpatialAccelerationVector();
      spatialAccelerationCalculator.packRelativeAcceleration(endEffectorAccelerationBack, base, endEffector);
      JUnitTools.assertSpatialMotionVectorEquals(endEffectorSpatialAcceleration, endEffectorAccelerationBack, 1e-3);

      assertWrenchesSumUpToMomentumDot(momentumControlModule.getExternalWrenches(), momentumRateOfChangeOut, gravityZ, totalMass, centerOfMassFrame);
      JUnitTools.assertSpatialForceVectorEquals(momentumRateOfChangeIn, momentumRateOfChangeOut, 1e-1);
      assertWrenchesInFrictionCones(momentumControlModule.getExternalWrenches(), contactStates, coefficientOfFriction);
   }

   private OptimizationMomentumControlModule createMomentumControlModule(InverseDynamicsJoint rootJoint, ReferenceFrame centerOfMassFrame,
           MomentumOptimizationSettings momentumOptimizationSettings)
   {
      YoVariableRegistry registry = new YoVariableRegistry("test");
      InverseDynamicsJoint[] jointsToOptimizeFor = ScrewTools.computeSupportAndSubtreeJoints(rootJoint.getSuccessor());

      return new OptimizationMomentumControlModule(rootJoint, centerOfMassFrame, controlDT, registry, jointsToOptimizeFor, momentumOptimizationSettings,
              gravityZ);
   }

   private static SpatialAccelerationCalculator createSpatialAccelerationCalculator(TwistCalculator twistCalculator, RigidBody elevator)
   {
      ReferenceFrame rootFrame = elevator.getBodyFixedFrame();
      SpatialAccelerationVector rootAcceleration = new SpatialAccelerationVector(rootFrame, rootFrame, rootFrame);
      SpatialAccelerationCalculator spatialAccelerationCalculator = new SpatialAccelerationCalculator(elevator, rootFrame, rootAcceleration, twistCalculator,
                                                                       true, true);

      return spatialAccelerationCalculator;
   }

   private static DenseMatrix64F setRandomJointAccelerations(Random random, OptimizationMomentumControlModule momentumControlModule,
           List<? extends InverseDynamicsJoint> joints)
   {
      DenseMatrix64F desiredJointAccelerations = new DenseMatrix64F(ScrewTools.computeDegreesOfFreedom(joints), 1);
      int index = 0;
      for (InverseDynamicsJoint joint : joints)
      {
         DenseMatrix64F jointAcceleration = new DenseMatrix64F(joint.getDegreesOfFreedom(), 1);
         RandomMatrices.setRandom(jointAcceleration, random);
         momentumControlModule.setDesiredJointAcceleration(joint, jointAcceleration);
         CommonOps.insert(jointAcceleration, desiredJointAccelerations, index, 0);
         index += joint.getDegreesOfFreedom();
      }

      return desiredJointAccelerations;
   }

   private void addContactState(double coefficientOfFriction, RigidBody endEffector, LinkedHashMap<ContactablePlaneBody, PlaneContactState> contactStates)
   {
      ReferenceFrame soleFrame = endEffector.getBodyFixedFrame();
      ContactablePlaneBody contactablePlaneBody = new RectangularContactableBody(endEffector, soleFrame, 1.0, -2.0, 3.0, -4.0);
      YoPlaneContactState contactState = new YoPlaneContactState("testContactState", endEffector.getParentJoint().getFrameAfterJoint(), soleFrame,
                                            new YoVariableRegistry("bla"));
      contactState.set(contactablePlaneBody.getContactPoints2d(), coefficientOfFriction);
      contactStates.put(contactablePlaneBody, contactState);
   }

   private SpatialForceVector generateRandomFeasibleMomentumRateOfChange(ReferenceFrame centerOfMassFrame,
           LinkedHashMap<ContactablePlaneBody, PlaneContactState> contactStates, double totalMass, double gravityZ, Random random, double rhoMin)

   {
      ContactPointWrenchMatrixCalculator contactPointWrenchMatrixCalculator = new ContactPointWrenchMatrixCalculator(centerOfMassFrame,
                                                                                 MomentumOptimizerNative.nSupportVectors, MomentumOptimizerNative.rhoSize);
      contactPointWrenchMatrixCalculator.computeMatrix(contactStates.values());
      DenseMatrix64F rho = new DenseMatrix64F(MomentumOptimizerNative.rhoSize, 1);
      RandomMatrices.setRandom(rho, random);
      CommonOps.add(rho, rhoMin);
      contactPointWrenchMatrixCalculator.computeWrenches(contactStates.values(), rho);
      Wrench ret = TotalWrenchCalculator.computeTotalWrench(contactPointWrenchMatrixCalculator.getWrenches().values(), centerOfMassFrame);
      ret.setLinearPartZ(ret.getLinearPartZ() - totalMass * gravityZ);

      return ret;
   }

   private static MomentumOptimizationSettings createStandardOptimizationSettings()
   {
      MomentumOptimizationSettings momentumOptimizationSettings = new MomentumOptimizationSettings(new YoVariableRegistry("test1"));
      momentumOptimizationSettings.setMomentumWeight(1.0, 1.0, 1.0, 1.0);
//      momentumOptimizationSettings.setDampedLeastSquaresFactor(1e-11);
//      momentumOptimizationSettings.setGroundReactionForceRegularization(1e-5);
      momentumOptimizationSettings.setRhoMin(0.0);

      return momentumOptimizationSettings;
   }

   private void assertWrenchesSumUpToMomentumDot(Map<ContactablePlaneBody, Wrench> externalWrenches, SpatialForceVector desiredCentroidalMomentumRate,
           double gravityZ, double mass, ReferenceFrame centerOfMassFrame)
   {
      SpatialForceVector totalWrench = new Wrench(centerOfMassFrame, centerOfMassFrame);
      for (Wrench wrench : externalWrenches.values())
      {
         wrench.changeBodyFrameAttachedToSameBody(centerOfMassFrame);
         wrench.changeFrame(centerOfMassFrame);
         totalWrench.add(wrench);
      }

      Wrench gravitationalWrench = new Wrench(centerOfMassFrame, centerOfMassFrame);
      gravitationalWrench.setLinearPartZ(-mass * gravityZ);
      totalWrench.add(gravitationalWrench);

      JUnitTools.assertSpatialForceVectorEquals(desiredCentroidalMomentumRate, totalWrench, 1e-3);
   }

   private void assertWrenchesInFrictionCones(Map<ContactablePlaneBody, Wrench> externalWrenches,
           LinkedHashMap<ContactablePlaneBody, PlaneContactState> contactStates, double coefficientOfFriction)
   {
      CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();

      for (ContactablePlaneBody contactablePlaneBody : externalWrenches.keySet())
      {
         Wrench wrench = externalWrenches.get(contactablePlaneBody);
         PlaneContactState contactState = contactStates.get(contactablePlaneBody);
         ReferenceFrame planeFrame = contactState.getPlaneFrame();

         wrench.changeFrame(planeFrame);

         double fZ = wrench.getLinearPartZ();
         assertTrue(fZ > 0.0);

         double fT = Math.hypot(wrench.getLinearPartX(), wrench.getLinearPartY());
         assertTrue(fT / fZ < coefficientOfFriction);

         FramePoint2d cop = new FramePoint2d(planeFrame);
         centerOfPressureResolver.resolveCenterOfPressureAndNormalTorque(cop, wrench, planeFrame);

         FrameConvexPolygon2d supportPolygon = new FrameConvexPolygon2d(contactState.getContactPoints2d());
         assertTrue(supportPolygon.isPointInside(cop));

      }
   }
}
