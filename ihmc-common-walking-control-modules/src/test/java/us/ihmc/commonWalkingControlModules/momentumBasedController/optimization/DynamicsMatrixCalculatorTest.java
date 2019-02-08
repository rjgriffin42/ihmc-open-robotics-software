package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.jupiter.api.AfterEach;
import us.ihmc.robotics.Assert;
import org.junit.jupiter.api.Test;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBodyTools;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.WrenchMatrixCalculator;
import us.ihmc.commons.PrintTools;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.mecano.algorithms.CentroidalMomentumRateCalculator;
import us.ihmc.mecano.algorithms.InverseDynamicsCalculator;
import us.ihmc.mecano.multiBodySystem.interfaces.FloatingJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.multiBodySystem.iterators.SubtreeStreams;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.tools.JointStateType;
import us.ihmc.mecano.tools.MultiBodySystemRandomTools;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModelTestTools;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.robotics.testing.JUnitTools;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class DynamicsMatrixCalculatorTest
{
   private final static double tolerance = 0.0001;
   private final static int iters = 1;
   private final static double maxRho = 1.0e2;
   private final static double maxQddot = 1.0e2;

   private final Random random = new Random(5641654L);

   private FullHumanoidRobotModel fullHumanoidRobotModel;
   private WholeBodyControlCoreToolbox toolbox;

   private WrenchMatrixCalculator wrenchMatrixCalculator;
   private JointIndexHandler jointIndexHandler;

   private InverseDynamicsCalculator inverseDynamicsCalculator;
   private DynamicsMatrixCalculator dynamicsMatrixCalculator;
   private CentroidalMomentumRateCalculator centroidalMomentumRateCalculator;

   private double gravityZ;

   int degreesOfFreedom;
   int floatingBaseDoFs;
   int bodyDoFs;

   @AfterEach
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

   @Test
   public void testEquivalence() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();

         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, joints);

         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, wrenchMatrixCalculator.getRhoSize(), 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         update();
         dynamicsMatrixCalculator.computeRequiredRhoAndAchievableQddotGivenRho(dynamicsMatrixCalculator, qddotSolution, rhoSolution);

         solveAndCompare(qddotSolution, rhoSolution, true);
      }
   }

   @Test
   public void testMassMatrixOnly() throws Exception
   {
      setupTest();

      gravityZ = 0.0;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();

         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);

         int rhoSize = wrenchMatrixCalculator.getRhoSize();
         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, rhoSize, 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         update();
         dynamicsMatrixCalculator.computeQddotGivenRho(dynamicsMatrixCalculator, qddotSolution, rhoSolution);

         solveAndCompare(qddotSolution, rhoSolution, true);
      }
   }

   @Test
   public void testNoLoad() throws Exception
   {
      setupTest();

      gravityZ = 0.0;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);

         DenseMatrix64F rhoSolution = new DenseMatrix64F(wrenchMatrixCalculator.getRhoSize(), 1);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         solveAndCompare(qddotSolution, rhoSolution, false);
      }
   }

   @Test
   public void testGravityOnly() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);

         DenseMatrix64F rhoSolution = new DenseMatrix64F(wrenchMatrixCalculator.getRhoSize(), 1);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         solveAndCompare(qddotSolution, rhoSolution, false);
      }
   }

   @Test
   public void testGravityAndCoriolisOnly() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, joints);

         DenseMatrix64F rhoSolution = new DenseMatrix64F(wrenchMatrixCalculator.getRhoSize(), 1);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         solveAndCompare(qddotSolution, rhoSolution, false);
      }
   }

   @Test
   public void testMassMatrixAndGravityOnly() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);

         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, wrenchMatrixCalculator.getRhoSize(), 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         update();
         dynamicsMatrixCalculator.computeRequiredRhoAndAchievableQddotGivenRho(dynamicsMatrixCalculator, qddotSolution, rhoSolution);

         solveAndCompare(qddotSolution, rhoSolution, true);
      }
   }

   @Test
   public void testMassMatrixAndCoriolisOnly() throws Exception
   {
      setupTest();

      gravityZ = 0.0;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, joints);

         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, wrenchMatrixCalculator.getRhoSize(), 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         update();
         dynamicsMatrixCalculator.computeRequiredRhoAndAchievableQddotGivenRho(dynamicsMatrixCalculator, qddotSolution, rhoSolution);

         solveAndCompare(qddotSolution, rhoSolution, true);
      }
   }

   @Test
   public void testMassMatrixGravityAndCoriolisOnly() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, joints);

         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, wrenchMatrixCalculator.getRhoSize(), 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         update();
         dynamicsMatrixCalculator.computeRequiredRhoAndAchievableQddotGivenRho(dynamicsMatrixCalculator, qddotSolution, rhoSolution);

         solveAndCompare(qddotSolution, rhoSolution, true);
      }
   }

   @Test
   public void testForceAndGravityOnly() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);

         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, wrenchMatrixCalculator.getRhoSize(), 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         solveAndCompare(qddotSolution, rhoSolution, false);
      }
   }

   @Test
   public void testForceGravityAndCoriolisOnly() throws Exception
   {
      setupTest();

      gravityZ = 9.81;

      ArrayList<OneDoFJointBasics> joints = new ArrayList<>();
      fullHumanoidRobotModel.getOneDoFJoints(joints);

      for (int i = 0; i < iters; i++)
      {
         inverseDynamicsCalculator.setExternalWrenchesToZero();
         dynamicsMatrixCalculator.reset();
         MultiBodySystemRandomTools.nextState(random, JointStateType.CONFIGURATION, -Math.PI / 2.0, Math.PI / 2.0, joints);
         MultiBodySystemRandomTools.nextState(random, JointStateType.VELOCITY, joints);

         DenseMatrix64F rhoSolution = RandomGeometry.nextDenseMatrix64F(random, wrenchMatrixCalculator.getRhoSize(), 1, 0.0, maxRho);
         DenseMatrix64F qddotSolution = new DenseMatrix64F(degreesOfFreedom, 1);

         solveAndCompare(qddotSolution, rhoSolution, false);
      }
   }

   @Test
   public void testOther() throws Exception
   {
   }


   private void setupTest()
   {
      YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

      double controlDT = 0.005;

      fullHumanoidRobotModel = new FullRobotModelTestTools.RandomFullHumanoidRobotModel(random);
      fullHumanoidRobotModel.updateFrames();
      CommonHumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullHumanoidRobotModel);

      TwistCalculator twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), fullHumanoidRobotModel.getElevator());

      ControllerCoreOptimizationSettings momentumOptimizationSettings = new GeneralMomentumOptimizationSettings();
      ArrayList<ContactablePlaneBody> contactablePlaneBodies = new ArrayList<>();
      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBodyBasics footBody = fullHumanoidRobotModel.getFoot(robotSide);
         ReferenceFrame soleFrame = fullHumanoidRobotModel.getSoleFrame(robotSide);
         contactablePlaneBodies.add(ContactablePlaneBodyTools.createTypicalContactablePlaneBodyForTests(footBody, soleFrame));
      }

      JointBasics[] jointsToOptimizeFor = HighLevelHumanoidControllerToolbox.computeJointsToOptimizeFor(fullHumanoidRobotModel, new JointBasics[0]);
      
      FloatingJointBasics rootJoint = fullHumanoidRobotModel.getRootJoint();
      ReferenceFrame centerOfMassFrame = referenceFrames.getCenterOfMassFrame();
      toolbox = new WholeBodyControlCoreToolbox(controlDT, gravityZ, rootJoint, jointsToOptimizeFor, centerOfMassFrame, momentumOptimizationSettings,
                                                yoGraphicsListRegistry, registry);
      toolbox.setupForInverseDynamicsSolver(contactablePlaneBodies);

      wrenchMatrixCalculator = new WrenchMatrixCalculator(toolbox, registry);
      jointIndexHandler = toolbox.getJointIndexHandler();

      inverseDynamicsCalculator = new InverseDynamicsCalculator(toolbox.getRootBody());
      inverseDynamicsCalculator.setGravitionalAcceleration(-gravityZ); // Watch out for the sign here, it changed with the switch to Mecano.
      dynamicsMatrixCalculator = new DynamicsMatrixCalculator(toolbox, wrenchMatrixCalculator);

      centroidalMomentumRateCalculator = new CentroidalMomentumRateCalculator(twistCalculator.getRootBody(), toolbox.getCenterOfMassFrame());

      degreesOfFreedom = jointIndexHandler.getNumberOfDoFs();
      floatingBaseDoFs = fullHumanoidRobotModel.getRootJoint().getDegreesOfFreedom();
      bodyDoFs = degreesOfFreedom - floatingBaseDoFs;
   }

   private void update()
   {
      fullHumanoidRobotModel.updateFrames();

      wrenchMatrixCalculator.computeMatrices();
      dynamicsMatrixCalculator.compute();
      centroidalMomentumRateCalculator.reset();
   }

   private void solveAndCompare(DenseMatrix64F qddotSolution, DenseMatrix64F rhoSolution, boolean checkRigidBodyDynamics)
   {
      fullHumanoidRobotModel.updateFrames();

      wrenchMatrixCalculator.computeMatrices();
      Map<RigidBodyBasics, Wrench> contactWrenches = wrenchMatrixCalculator.computeWrenchesFromRho(rhoSolution);
      for (int i = 0; i < toolbox.getContactablePlaneBodies().size(); i++)
      {
         RigidBodyBasics rigidBody = toolbox.getContactablePlaneBodies().get(i).getRigidBody();
         inverseDynamicsCalculator.setExternalWrench(rigidBody, contactWrenches.get(rigidBody));
      }

      DenseMatrix64F inverseDynamicsTauSolution = new DenseMatrix64F(bodyDoFs, 1);
      DenseMatrix64F dynamicsMatrixTauSolution = new DenseMatrix64F(bodyDoFs, 1);

      // compute torques using dynamics matrix calculator
      dynamicsMatrixCalculator.compute();
      dynamicsMatrixCalculator.computeJointTorques(dynamicsMatrixTauSolution, qddotSolution, rhoSolution);

      // compute torques using inverse dynamics calculator
      MultiBodySystemTools.insertJointsState(jointIndexHandler.getIndexedJoints(), JointStateType.ACCELERATION, qddotSolution);
      inverseDynamicsCalculator.compute();
      inverseDynamicsCalculator.writeComputedJointWrenches(SubtreeStreams.fromChildren(toolbox.getRootBody()).toArray(JointBasics[]::new));

      dynamicsMatrixCalculator.extractTorqueMatrix(jointIndexHandler.getIndexedJoints(), inverseDynamicsTauSolution);

      if (checkRigidBodyDynamics)
         Assert.assertTrue(dynamicsMatrixCalculator.checkFloatingBaseRigidBodyDynamicsSatisfied(dynamicsMatrixCalculator, qddotSolution, dynamicsMatrixTauSolution, rhoSolution));

      for(int i = 0; i < inverseDynamicsTauSolution.getNumRows(); i++)
      {
         if (Math.abs(inverseDynamicsTauSolution.get(i, 0) - dynamicsMatrixTauSolution.get(i, 0)) > tolerance)
         {
            PrintTools.warn("Joint " + jointIndexHandler.getIndexedOneDoFJoints()[i].getName() + " did not result in an equivalent torque");
         }
      }

      checkTestQuality(qddotSolution, rhoSolution, inverseDynamicsTauSolution, dynamicsMatrixTauSolution);
      JUnitTools.assertMatrixEquals(inverseDynamicsTauSolution, dynamicsMatrixTauSolution, tolerance);
   }

   private void checkTestQuality(DenseMatrix64F qddotSolution, DenseMatrix64F rhoSolution, DenseMatrix64F inverseDynamicsSolution, DenseMatrix64F matrixSolution)
   {
      Assert.assertTrue(CommonOps.elementMin(rhoSolution) > -0.000001);

      Assert.assertTrue(CommonOps.elementMax(qddotSolution) < maxQddot);
      Assert.assertTrue(CommonOps.elementMin(qddotSolution) > -maxQddot);

      Assert.assertTrue(!MatrixTools.isEmptyMatrix(inverseDynamicsSolution));
      Assert.assertTrue(!MatrixTools.isEmptyMatrix(matrixSolution));
   }

   private class GeneralMomentumOptimizationSettings implements ControllerCoreOptimizationSettings
   {

      // defaults for unscaled model:
      private static final double defaultRhoWeight = 0.00001;
      private static final double defaultRhoMin = 4.0;
      private static final double defaultRhoRateDefaultWeight = 0.002;
      private static final double defaultRhoRateHighWeight = 0.05;

      private final int nBasisVectorsPerContactPoint = 4;
      private final int nContactPointsPerContactableBody = 4;
      private final int nContactableBodies = 2;

      private final double jointAccelerationWeight = 0.005;
      private final double jointJerkWeight = 0.1;
      private final Vector2D copWeight = new Vector2D(100.0, 200.0);
      private final Vector2D copRateDefaultWeight = new Vector2D(20000.0, 20000.0);
      private final Vector2D copRateHighWeight = new Vector2D(2500000.0, 10000000.0);

      private final double rhoWeight;
      private final double rhoMin;
      private final double rhoRateDefaultWeight;
      private final double rhoRateHighWeight;

      public GeneralMomentumOptimizationSettings()
      {
         double scale = 1.0;
         rhoWeight = defaultRhoWeight / scale;
         rhoMin = defaultRhoMin * scale;
         rhoRateDefaultWeight = defaultRhoRateDefaultWeight / (scale * scale);
         rhoRateHighWeight = defaultRhoRateHighWeight / (scale * scale);
      }

      /** @inheritDoc */
      @Override
      public double getJointAccelerationWeight()
      {
         return jointAccelerationWeight;
      }

      /** @inheritDoc */
      @Override
      public double getJointJerkWeight()
      {
         return jointJerkWeight;
      }

      /** @inheritDoc */
      @Override
      public double getRhoWeight()
      {
         return rhoWeight;
      }

      /** @inheritDoc */
      @Override
      public double getRhoMin()
      {
         return rhoMin;
      }

      /** @inheritDoc */
      @Override
      public double getRhoRateDefaultWeight()
      {
         return rhoRateDefaultWeight;
      }

      /** @inheritDoc */
      @Override
      public double getRhoRateHighWeight()
      {
         return rhoRateHighWeight;
      }

      /** @inheritDoc */
      @Override
      public Vector2D getCoPWeight()
      {
         return copWeight;
      }

      /** @inheritDoc */
      @Override
      public Vector2D getCoPRateDefaultWeight()
      {
         return copRateDefaultWeight;
      }

      /** @inheritDoc */
      @Override
      public Vector2D getCoPRateHighWeight()
      {
         return copRateHighWeight;
      }

      /** @inheritDoc */
      @Override
      public int getNumberOfBasisVectorsPerContactPoint()
      {
         return nBasisVectorsPerContactPoint;
      }

      /** @inheritDoc */
      @Override
      public int getNumberOfContactPointsPerContactableBody()
      {
         return nContactPointsPerContactableBody;
      }

      /** @inheritDoc */
      @Override
      public int getNumberOfContactableBodies()
      {
         return nContactableBodies;
      }

      @Override
      public boolean getDeactivateRhoWhenNotInContact()
      {
         return false;
      }
   }
}
