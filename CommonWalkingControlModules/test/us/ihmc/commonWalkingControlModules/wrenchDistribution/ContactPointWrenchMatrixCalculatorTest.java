package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.EjmlUnitTests;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolver;
import us.ihmc.utilities.math.geometry.CenterOfMassReferenceFrame;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTestTools;
import us.ihmc.utilities.screwTheory.SpatialForceVector;
import us.ihmc.utilities.screwTheory.Wrench;

import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertTrue;

/**
 * @author twan
 *         Date: 5/1/13
 */
public class ContactPointWrenchMatrixCalculatorTest
{
   private static final Vector3d X = new Vector3d(1.0, 0.0, 0.0);
   private static final Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
   private static final Vector3d Z = new Vector3d(0.0, 0.0, 1.0);

   @Test
   public void testComputeContactPointWrenchMatrix() throws Exception
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
         bodies.add(randomFloatingChain.getRevoluteJoints().get(2).getSuccessor());
         bodies.add(randomFloatingChain.getRevoluteJoints().get(4).getSuccessor());


         List<PlaneContactState> contactStates = new ArrayList<PlaneContactState>();
         YoVariableRegistry registry = new YoVariableRegistry("test");

         double coefficientOfFriction = random.nextDouble();

         int nContactPoints = 4;
         int contactNumber = 0;

         for (RigidBody body : bodies)
         {
            ReferenceFrame frameAfterJoint = body.getParentJoint().getFrameAfterJoint();
            ReferenceFrame planeFrame = body.getBodyFixedFrame();
            YoPlaneContactState contactState = new YoPlaneContactState("contactState" + contactNumber++, frameAfterJoint, planeFrame, registry);
            contactStates.add(contactState);

            List<FramePoint2d> contactPoints = new ArrayList<FramePoint2d>();
            for (int i = 0; i < nContactPoints; i++)
            {
               FramePoint2d contactPoint = new FramePoint2d(planeFrame, random.nextDouble(), random.nextDouble());
               contactPoints.add(contactPoint);
            }

            contactState.set(contactPoints, coefficientOfFriction);
         }

         int nSupportVectorsPerContactPoint = 4;
         int nColumns = nContactPoints * contactStates.size() * nSupportVectorsPerContactPoint + 5;
         ContactPointWrenchMatrixCalculator calculator = new ContactPointWrenchMatrixCalculator(centerOfMassFrame, nSupportVectorsPerContactPoint, nColumns);

         calculator.computeMatrix(contactStates);
         DenseMatrix64F q = calculator.getMatrix();

         DenseMatrix64F rho = new DenseMatrix64F(nColumns, 1);
         RandomMatrices.setRandom(rho, random);

         assertTotalWrenchIsSumOfIndividualWrenches(centerOfMassFrame, contactStates, calculator, q, rho);

         assertWrenchesOK(contactStates, calculator, rho, coefficientOfFriction);
      }

   }

   private void assertTotalWrenchIsSumOfIndividualWrenches(CenterOfMassReferenceFrame centerOfMassFrame, List<PlaneContactState> contactStates, ContactPointWrenchMatrixCalculator calculator, DenseMatrix64F q, DenseMatrix64F rho)
   {
      DenseMatrix64F totalWrenchFromQ = new DenseMatrix64F(Wrench.SIZE, 1);
      CommonOps.mult(q, rho, totalWrenchFromQ);

      calculator.computeWrenches(contactStates, rho);
      SpatialForceVector totalWrench = new Wrench(centerOfMassFrame, centerOfMassFrame);
      for (PlaneContactState contactState : contactStates)
      {
         Wrench wrench = calculator.getWrench(contactState);
         totalWrench.add(wrench);
      }

      DenseMatrix64F totalWrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
      totalWrench.packMatrix(totalWrenchMatrix);

      EjmlUnitTests.assertEquals(totalWrenchFromQ, totalWrenchMatrix, 1e-12);
   }

   private void assertWrenchesOK(List<PlaneContactState> contactStates, ContactPointWrenchMatrixCalculator calculator, DenseMatrix64F rho,
                                 double coefficientOfFriction)
   {
      CenterOfPressureResolver centerOfPressureResolver = new CenterOfPressureResolver();
      calculator.computeWrenches(contactStates, rho);
      for (PlaneContactState contactState : contactStates)
      {
         Wrench wrench = calculator.getWrench(contactState);
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
