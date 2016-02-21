package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.EjmlUnitTests;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ListOfPointsContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.solver.PlaneContactStateCommandPool;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTestTools;
import us.ihmc.robotics.screwTheory.SpatialForceVector;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class PlaneContactWrenchMatrixCalculatorTest
{
   private static final Vector3d X = new Vector3d(1.0, 0.0, 0.0);
   private static final Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
   private static final Vector3d Z = new Vector3d(0.0, 0.0, 1.0);

   @DeployableTestMethod(estimatedDuration = 5.0)
   @Test(timeout = 30000)
   public void testComputePlaneContactWrenchMatrix() throws Exception
   {
      Random random = new Random(12341253L);

      int nTests = 1000;
      for (int testNumber = 0; testNumber < nTests; testNumber++)
      {
         Vector3d[] jointAxes = new Vector3d[] {X, Y, Z, X, Y, Z};
         ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
         randomFloatingChain.setRandomPositionsAndVelocities(random);

         CenterOfMassReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("com", ReferenceFrame.getWorldFrame(),
               randomFloatingChain.getElevator());
         centerOfMassFrame.update();

         List<RigidBody> bodies = new ArrayList<RigidBody>();
         List<ContactablePlaneBody> contactablePlaneBodies = new ArrayList<>();
         bodies.add(randomFloatingChain.getRevoluteJoints().get(2).getSuccessor());
         bodies.add(randomFloatingChain.getRevoluteJoints().get(4).getSuccessor());

         LinkedHashMap<RigidBody, PlaneContactState> contactStates = new LinkedHashMap<RigidBody, PlaneContactState>();
         YoVariableRegistry registry = new YoVariableRegistry("test");

         double coefficientOfFriction = random.nextDouble();

         int nContactPoints = 4;
         int contactNumber = 0;
         
         PlaneContactStateCommandPool pool = new PlaneContactStateCommandPool();

         for (RigidBody body : bodies)
         {
            ReferenceFrame planeFrame = body.getBodyFixedFrame();

            List<FramePoint2d> contactPoints = new ArrayList<FramePoint2d>();
            List<Point2d> contactPoint2ds = new ArrayList<Point2d>();
            
            for (int i = 0; i < nContactPoints; i++)
            {
               FramePoint2d contactPoint = new FramePoint2d(planeFrame, random.nextDouble(), random.nextDouble());
               contactPoints.add(contactPoint);
               Point2d cp2d = new Point2d();
               contactPoint.get(cp2d);
               contactPoint2ds.add(cp2d);
            }

            YoPlaneContactState contactState = new YoPlaneContactState("contactState" + contactNumber++, body, planeFrame, contactPoints, coefficientOfFriction,
                  registry);
            contactStates.put(body, contactState);
            contactState.getPlaneContactStateCommand(pool.createCommand());
            
            contactablePlaneBodies.add(new ListOfPointsContactablePlaneBody(body, planeFrame, contactPoint2ds));
         }

         int nSupportVectorsPerContactPoint = 4;
         int rhoSize = nContactPoints * contactStates.size() * nSupportVectorsPerContactPoint + 5;
         double wRho = 0.0;
         double wRhoSmoother = 0.0;
         double wRhoPenalizer = 0.0;
         PlaneContactWrenchMatrixCalculator calculator = new PlaneContactWrenchMatrixCalculator(centerOfMassFrame, rhoSize, nContactPoints,
               nSupportVectorsPerContactPoint, wRho, wRhoSmoother, wRhoPenalizer, contactablePlaneBodies, registry);
         calculator.setPlaneContactStateCommandPool(pool);
         calculator.computeMatrices();
         DenseMatrix64F q = calculator.getQRho();

         DenseMatrix64F rho = new DenseMatrix64F(rhoSize, 1);
         RandomMatrices.setRandom(rho, random);

         Map<RigidBody, Wrench> rigidBodyWrenchMap = calculator.computeWrenches(rho);

         assertTotalWrenchIsSumOfIndividualWrenches(centerOfMassFrame, rigidBodyWrenchMap.values(), q, rho);

         assertWrenchesOK(contactStates, rigidBodyWrenchMap, coefficientOfFriction);
      }

   }

   private void assertTotalWrenchIsSumOfIndividualWrenches(CenterOfMassReferenceFrame centerOfMassFrame, Collection<Wrench> rigidBodyWrenchMap,
         DenseMatrix64F q, DenseMatrix64F rho)
   {
      DenseMatrix64F totalWrenchFromQ = new DenseMatrix64F(Wrench.SIZE, 1);
      CommonOps.mult(q, rho, totalWrenchFromQ);

      SpatialForceVector totalWrench = new Wrench(centerOfMassFrame, centerOfMassFrame);
      for (Wrench wrench : rigidBodyWrenchMap)
      {
         wrench.changeFrame(centerOfMassFrame);
         totalWrench.add(wrench);
      }

      DenseMatrix64F totalWrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
      totalWrench.getMatrix(totalWrenchMatrix);

      EjmlUnitTests.assertEquals(totalWrenchFromQ, totalWrenchMatrix, 1e-12);
   }

   /**
    * Asserts for each contactable body that:
    * 1- Normal contact force remains positive (the contactable body is not pulling on the ground),
    * 2- Contact force remains inside friction cone,
    * 3- Center of pressure remains inside the support polygon.
    * @param contactStates
    * @param rigidBodyWrenchMap
    * @param coefficientOfFriction
    */
   private void assertWrenchesOK(Map<RigidBody, PlaneContactState> contactStates, Map<RigidBody, Wrench> rigidBodyWrenchMap, double coefficientOfFriction)
   {
      CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();
      for (RigidBody rigidBody : rigidBodyWrenchMap.keySet())
      {
         Wrench wrench = rigidBodyWrenchMap.get(rigidBody);
         PlaneContactState contactState = contactStates.get(rigidBody);
         ReferenceFrame planeFrame = contactState.getPlaneFrame();

         wrench.changeFrame(planeFrame);

         double fZ = wrench.getLinearPartZ();
         assertTrue(fZ > 0.0);

         double fT = Math.hypot(wrench.getLinearPartX(), wrench.getLinearPartY());
         assertTrue(fT / fZ < coefficientOfFriction);

         FramePoint2d cop = new FramePoint2d(planeFrame);
         centerOfPressureResolver.resolveCenterOfPressureAndNormalTorque(cop, wrench, planeFrame);

         FrameConvexPolygon2d supportPolygon = new FrameConvexPolygon2d(contactState.getContactFramePoints2dInContactCopy());
         assertTrue(supportPolygon.isPointInside(cop));
      }
   }
}
