package us.ihmc.commonWalkingControlModules.modelPredictiveController;

import org.ejml.EjmlUnitTests;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.commands.CoMPositionCommand;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.ZeroConeRotationCalculator;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.yoVariables.registry.YoRegistry;

import static org.junit.jupiter.api.Assertions.*;

public class CoMPositionCommandTest
{
   @Test
   public void testCommandOptimize()
   {
      FramePoint3D objectivePosition = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.35, 0.7, 0.8);

      double gravityZ = -9.81;
      double omega = 3.0;
      double mu = 0.8;
      double dt = 1e-3;
      double mass = 1.5;

      FrameVector3D gravityVector = new FrameVector3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, gravityZ);

      ContactStateMagnitudeToForceMatrixHelper rhoHelper = new ContactStateMagnitudeToForceMatrixHelper(4, 4, new ZeroConeRotationCalculator());
      CoefficientJacobianMatrixHelper helper = new CoefficientJacobianMatrixHelper(4, 4);
      ContactPlaneHelper contactPlaneHelper = new ContactPlaneHelper(4, 4, new ZeroConeRotationCalculator());

      MPCIndexHandler indexHandler = new MPCIndexHandler(4);
      CoMMPCQPSolver solver = new CoMMPCQPSolver(indexHandler, dt, mass, gravityZ, new YoRegistry("test"));

      FramePose3D contactPose = new FramePose3D();

      ConvexPolygon2DReadOnly contactPolygon = MPCTestHelper.createDefaultContact();

      rhoHelper.computeMatrices(contactPolygon, contactPose, 1e-8, 1e-10, mu);
      contactPlaneHelper.computeBasisVectors(contactPolygon, contactPose, mu);

      indexHandler.initialize(i -> contactPolygon.getNumberOfVertices(), 1);

      double timeOfConstraint = 0.7;

      CoMPositionCommand command = new CoMPositionCommand();
      command.setObjective(objectivePosition);
      command.setTimeOfObjective(timeOfConstraint);
      command.setSegmentNumber(0);
      command.setOmega(omega);
      command.setWeight(1.0);
      command.addContactPlaneHelper(contactPlaneHelper);

      double regularization = 1e-5;
      solver.initialize();
      solver.submitMPCValueObjective(command);
      solver.setComCoefficientRegularizationWeight(regularization);
      solver.setRhoCoefficientRegularizationWeight(regularization);

      solver.solve();

      FramePoint3D solvedPositionAtConstraint = new FramePoint3D();
      FramePoint3D solvedObjectivePositionTuple = new FramePoint3D();
      DMatrixRMaj rhoValueVector = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);
      DMatrixRMaj solvedObjectivePosition = new DMatrixRMaj(3, 1);

      DMatrixRMaj solution = solver.getSolution();
      DMatrixRMaj rhoSolution = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);
      solvedPositionAtConstraint.addX(timeOfConstraint * solution.get(0, 0));
      solvedPositionAtConstraint.addX(solution.get(1, 0));
      solvedPositionAtConstraint.addY(timeOfConstraint * solution.get(2, 0));
      solvedPositionAtConstraint.addY(solution.get(3, 0));
      solvedPositionAtConstraint.addZ(timeOfConstraint * solution.get(4, 0));
      solvedPositionAtConstraint.addZ(solution.get(5, 0));

      MatrixTools.setMatrixBlock(rhoSolution, 0, 0, solution, 6, 0, rhoHelper.getRhoSize() * 4, 1, 1.0);

      helper.computeMatrices(timeOfConstraint, omega);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution, rhoValueVector);

      CommonOps_DDRM.mult(solver.qpInputTypeA.taskJacobian, solution, solvedObjectivePosition);
      solvedObjectivePositionTuple.set(solvedObjectivePosition);
      solvedObjectivePositionTuple.scaleAdd(0.5 * timeOfConstraint * timeOfConstraint, gravityVector, solvedObjectivePositionTuple);

      DMatrixRMaj taskObjectiveExpected = new DMatrixRMaj(3, 1);
      DMatrixRMaj achievedObjective = new DMatrixRMaj(3, 1);
      objectivePosition.get(taskObjectiveExpected);
      taskObjectiveExpected.add(2, 0, -0.5 * timeOfConstraint * timeOfConstraint * -Math.abs(gravityZ));

      DMatrixRMaj taskJacobianExpected = new DMatrixRMaj(3, 6 + rhoHelper.getRhoSize() * 4);
      taskJacobianExpected.set(0, 0, timeOfConstraint);
      taskJacobianExpected.set(0, 1, 1.0);
      taskJacobianExpected.set(1, 2, timeOfConstraint);
      taskJacobianExpected.set(1, 3, 1.0);
      taskJacobianExpected.set(2, 4, timeOfConstraint);
      taskJacobianExpected.set(2, 5, 1.0);



      for (int rhoIdx  = 0; rhoIdx < rhoHelper.getRhoSize(); rhoIdx++)
      {
         int startIdx = 6 + 4 * rhoIdx;
         double rhoValue = Math.exp(omega * timeOfConstraint) * solution.get(startIdx, 0);
         rhoValue += Math.exp(-omega * timeOfConstraint) * solution.get(startIdx + 1, 0);
         rhoValue += timeOfConstraint * timeOfConstraint * timeOfConstraint * solution.get(startIdx + 2, 0);
         rhoValue += timeOfConstraint * timeOfConstraint * solution.get(startIdx + 3, 0);

         assertEquals(rhoValue, rhoValueVector.get(rhoIdx), 1e-5);
         solvedPositionAtConstraint.scaleAdd(rhoValue, rhoHelper.getBasisVector(rhoIdx), solvedPositionAtConstraint);

         taskJacobianExpected.set(0, startIdx, rhoHelper.getBasisVector(rhoIdx).getX() * Math.exp(omega * timeOfConstraint));
         taskJacobianExpected.set(0, startIdx + 1, rhoHelper.getBasisVector(rhoIdx).getX() * Math.exp(-omega * timeOfConstraint));
         taskJacobianExpected.set(0, startIdx + 2, rhoHelper.getBasisVector(rhoIdx).getX() * timeOfConstraint * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(0, startIdx + 3, rhoHelper.getBasisVector(rhoIdx).getX() * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(1, startIdx, rhoHelper.getBasisVector(rhoIdx).getY() * Math.exp(omega * timeOfConstraint));
         taskJacobianExpected.set(1, startIdx + 1, rhoHelper.getBasisVector(rhoIdx).getY() * Math.exp(-omega * timeOfConstraint));
         taskJacobianExpected.set(1, startIdx + 2, rhoHelper.getBasisVector(rhoIdx).getY() * timeOfConstraint * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(1, startIdx + 3, rhoHelper.getBasisVector(rhoIdx).getY() * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(2, startIdx, rhoHelper.getBasisVector(rhoIdx).getZ() * Math.exp(omega * timeOfConstraint));
         taskJacobianExpected.set(2, startIdx + 1, rhoHelper.getBasisVector(rhoIdx).getZ() * Math.exp(-omega * timeOfConstraint));
         taskJacobianExpected.set(2, startIdx + 2, rhoHelper.getBasisVector(rhoIdx).getZ() * timeOfConstraint * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(2, startIdx + 3, rhoHelper.getBasisVector(rhoIdx).getZ() * timeOfConstraint * timeOfConstraint);
      }
      solvedPositionAtConstraint.scaleAdd(0.5 * timeOfConstraint * timeOfConstraint, gravityVector, solvedPositionAtConstraint);

      EjmlUnitTests.assertEquals(taskJacobianExpected, solver.qpInputTypeA.taskJacobian, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.qpInputTypeA.taskObjective, 1e-5);

      CommonOps_DDRM.mult(taskJacobianExpected, solution, achievedObjective);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, achievedObjective, 1e-4);

      DMatrixRMaj solverInput_H_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), taskJacobianExpected.getNumCols());
      DMatrixRMaj solverInput_f_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), 1);

      CommonOps_DDRM.multInner(taskJacobianExpected, solverInput_H_Expected);
      CommonOps_DDRM.multTransA(-1.0, taskJacobianExpected, taskObjectiveExpected, solverInput_f_Expected);

      MatrixTools.addDiagonal(solverInput_H_Expected, regularization);

      EjmlUnitTests.assertEquals(solverInput_H_Expected, solver.solverInput_H, 1e-10);
      EjmlUnitTests.assertEquals(solverInput_f_Expected, solver.solverInput_f, 1e-10);


      EuclidCoreTestTools.assertTuple3DEquals(objectivePosition, solvedObjectivePositionTuple, 1e-4);
      EuclidCoreTestTools.assertTuple3DEquals(objectivePosition, solvedPositionAtConstraint, 1e-4);
   }

   @Test
   public void testDoubleSubmission()
   {
      FramePoint3D objectivePosition = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.35, 0.7, 0.8);

      double gravityZ = -9.81;
      double omega = 3.0;
      double mu = 0.8;
      double dt = 1e-3;
      double mass = 1.5;

      FrameVector3D gravityVector = new FrameVector3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, gravityZ);

      ContactStateMagnitudeToForceMatrixHelper rhoHelper = new ContactStateMagnitudeToForceMatrixHelper(4, 4, new ZeroConeRotationCalculator());
      CoefficientJacobianMatrixHelper helper = new CoefficientJacobianMatrixHelper(4, 4);
      ContactPlaneHelper contactPlaneHelper = new ContactPlaneHelper(4, 4, new ZeroConeRotationCalculator());

      MPCIndexHandler indexHandler = new MPCIndexHandler(4);
      CoMMPCQPSolver solver = new CoMMPCQPSolver(indexHandler, dt, mass, gravityZ, new YoRegistry("test"));

      FramePose3D contactPose = new FramePose3D();

      ConvexPolygon2DReadOnly contactPolygon = MPCTestHelper.createDefaultContact();

      contactPlaneHelper.computeBasisVectors(contactPolygon, contactPose, mu);
      rhoHelper.computeMatrices(contactPolygon, contactPose, 1e-8, 1e-10, mu);

      indexHandler.initialize(i -> contactPolygon.getNumberOfVertices(), 1);

      double timeOfConstraint = 0.7;

      CoMPositionCommand command = new CoMPositionCommand();
      command.setObjective(objectivePosition);
      command.setTimeOfObjective(timeOfConstraint);
      command.setSegmentNumber(0);
      command.setOmega(omega);
      command.setWeight(1.0);
      command.addContactPlaneHelper(contactPlaneHelper);

      double regularization = 1e-5;
      solver.initialize();
      solver.submitMPCValueObjective(command);
      solver.submitMPCValueObjective(command);
      solver.setComCoefficientRegularizationWeight(regularization);
      solver.setRhoCoefficientRegularizationWeight(regularization);

      solver.solve();

      FramePoint3D solvedPositionAtConstraint = new FramePoint3D();
      FramePoint3D solvedObjectivePositionTuple = new FramePoint3D();
      DMatrixRMaj rhoValueVector = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);
      DMatrixRMaj solvedObjectivePosition = new DMatrixRMaj(3, 1);

      DMatrixRMaj solution = solver.getSolution();
      DMatrixRMaj rhoSolution = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);
      solvedPositionAtConstraint.addX(timeOfConstraint * solution.get(0, 0));
      solvedPositionAtConstraint.addX(solution.get(1, 0));
      solvedPositionAtConstraint.addY(timeOfConstraint * solution.get(2, 0));
      solvedPositionAtConstraint.addY(solution.get(3, 0));
      solvedPositionAtConstraint.addZ(timeOfConstraint * solution.get(4, 0));
      solvedPositionAtConstraint.addZ(solution.get(5, 0));

      MatrixTools.setMatrixBlock(rhoSolution, 0, 0, solution, 6, 0, rhoHelper.getRhoSize() * 4, 1, 1.0);

      helper.computeMatrices(timeOfConstraint, omega);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution, rhoValueVector);

      CommonOps_DDRM.mult(solver.qpInputTypeA.taskJacobian, solution, solvedObjectivePosition);
      solvedObjectivePositionTuple.set(solvedObjectivePosition);
      solvedObjectivePositionTuple.scaleAdd(0.5 * timeOfConstraint * timeOfConstraint, gravityVector, solvedObjectivePositionTuple);

      DMatrixRMaj taskObjectiveExpected = new DMatrixRMaj(3, 1);
      DMatrixRMaj achievedObjective = new DMatrixRMaj(3, 1);
      objectivePosition.get(taskObjectiveExpected);
      taskObjectiveExpected.add(2, 0, -0.5 * timeOfConstraint * timeOfConstraint * -Math.abs(gravityZ));

      DMatrixRMaj taskJacobianExpected = MPCTestHelper.getCoMPositionJacobian(timeOfConstraint, omega, rhoHelper);

      for (int rhoIdx  = 0; rhoIdx < rhoHelper.getRhoSize(); rhoIdx++)
      {
         int startIdx = 6 + 4 * rhoIdx;
         double rhoValue = Math.exp(omega * timeOfConstraint) * solution.get(startIdx, 0);
         rhoValue += Math.exp(-omega * timeOfConstraint) * solution.get(startIdx + 1, 0);
         rhoValue += timeOfConstraint * timeOfConstraint * timeOfConstraint * solution.get(startIdx + 2, 0);
         rhoValue += timeOfConstraint * timeOfConstraint * solution.get(startIdx + 3, 0);

         assertEquals(rhoValue, rhoValueVector.get(rhoIdx), 1e-5);
         solvedPositionAtConstraint.scaleAdd(rhoValue, rhoHelper.getBasisVector(rhoIdx), solvedPositionAtConstraint);
      }
      solvedPositionAtConstraint.scaleAdd(0.5 * timeOfConstraint * timeOfConstraint, gravityVector, solvedPositionAtConstraint);

      EjmlUnitTests.assertEquals(taskJacobianExpected, solver.qpInputTypeA.taskJacobian, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.qpInputTypeA.taskObjective, 1e-5);

      CommonOps_DDRM.mult(taskJacobianExpected, solution, achievedObjective);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, achievedObjective, 1e-4);

      DMatrixRMaj solverInput_H_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), taskJacobianExpected.getNumCols());
      DMatrixRMaj solverInput_f_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), 1);

      CommonOps_DDRM.multInner(taskJacobianExpected, solverInput_H_Expected);
      CommonOps_DDRM.scale(2.0, solverInput_H_Expected);
      CommonOps_DDRM.multTransA(-2.0, taskJacobianExpected, taskObjectiveExpected, solverInput_f_Expected);

      MatrixTools.addDiagonal(solverInput_H_Expected, regularization);

      EjmlUnitTests.assertEquals(solverInput_H_Expected, solver.solverInput_H, 1e-10);
      EjmlUnitTests.assertEquals(solverInput_f_Expected, solver.solverInput_f, 1e-10);


      EuclidCoreTestTools.assertTuple3DEquals(objectivePosition, solvedObjectivePositionTuple, 1e-4);
      EuclidCoreTestTools.assertTuple3DEquals(objectivePosition, solvedPositionAtConstraint, 1e-4);
   }


   @Test
   public void testCommandOptimizeSegment2()
   {
      FramePoint3D objectivePosition = new FramePoint3D(ReferenceFrame.getWorldFrame(), -0.35, 0.7, 0.8);

      double gravityZ = -9.81;
      double omega = 3.0;
      double mu = 0.8;
      double mass = 1.5;
      double dt = 1e-3;

      FrameVector3D gravityVector = new FrameVector3D(ReferenceFrame.getWorldFrame(), 0.0, 0.0, gravityZ);

      ContactStateMagnitudeToForceMatrixHelper rhoHelper = new ContactStateMagnitudeToForceMatrixHelper(4, 4, new ZeroConeRotationCalculator());
      CoefficientJacobianMatrixHelper helper = new CoefficientJacobianMatrixHelper(4, 4);
      ContactPlaneHelper contactPlaneHelper = new ContactPlaneHelper(4, 4, new ZeroConeRotationCalculator());

      MPCIndexHandler indexHandler = new MPCIndexHandler(4);
      CoMMPCQPSolver solver = new CoMMPCQPSolver(indexHandler, dt, mass, gravityZ, new YoRegistry("test"));

      FramePose3D contactPose = new FramePose3D();

      ConvexPolygon2DReadOnly contactPolygon = MPCTestHelper.createDefaultContact();

      rhoHelper.computeMatrices(contactPolygon, contactPose, 1e-8, 1e-10, mu);
      contactPlaneHelper.computeBasisVectors(contactPolygon, contactPose, mu);

      indexHandler.initialize(i -> contactPolygon.getNumberOfVertices(), 2);

      double timeOfConstraint = 0.7;

      CoMPositionCommand command = new CoMPositionCommand();
      command.setObjective(objectivePosition);
      command.setTimeOfObjective(timeOfConstraint);
      command.setSegmentNumber(1);
      command.setOmega(omega);
      command.setWeight(1.0);
      command.addContactPlaneHelper(contactPlaneHelper);

      double regularization = 1e-5;
      solver.initialize();
      solver.submitMPCValueObjective(command);
      solver.setComCoefficientRegularizationWeight(regularization);
      solver.setRhoCoefficientRegularizationWeight(regularization);

      solver.solve();

      FramePoint3D solvedPositionAtConstraint = new FramePoint3D();
      FramePoint3D solvedObjectivePositionTuple = new FramePoint3D();
      DMatrixRMaj rhoValueVector = new DMatrixRMaj(rhoHelper.getRhoSize(), 1);
      DMatrixRMaj solvedObjectivePosition = new DMatrixRMaj(3, 1);

      DMatrixRMaj solution = solver.getSolution();
      DMatrixRMaj rhoSolution = new DMatrixRMaj(rhoHelper.getRhoSize() * 4, 1);
      int start = 6 + 4 * rhoHelper.getRhoSize();

      solvedPositionAtConstraint.addX(timeOfConstraint * solution.get(start, 0));
      solvedPositionAtConstraint.addX(solution.get(start + 1, 0));
      solvedPositionAtConstraint.addY(timeOfConstraint * solution.get(start + 2, 0));
      solvedPositionAtConstraint.addY(solution.get(start + 3, 0));
      solvedPositionAtConstraint.addZ(timeOfConstraint * solution.get(start + 4, 0));
      solvedPositionAtConstraint.addZ(solution.get(start + 5, 0));

      MatrixTools.setMatrixBlock(rhoSolution, 0, 0, solution, 12 + 4 * rhoHelper.getRhoSize(), 0, rhoHelper.getRhoSize() * 4, 1, 1.0);

      helper.computeMatrices(timeOfConstraint, omega);
      CommonOps_DDRM.mult(helper.getPositionJacobianMatrix(), rhoSolution, rhoValueVector);

      CommonOps_DDRM.mult(solver.qpInputTypeA.taskJacobian, solution, solvedObjectivePosition);
      solvedObjectivePositionTuple.set(solvedObjectivePosition);
      solvedObjectivePositionTuple.scaleAdd(0.5 * timeOfConstraint * timeOfConstraint, gravityVector, solvedObjectivePositionTuple);

      DMatrixRMaj taskObjectiveExpected = new DMatrixRMaj(3, 1);
      DMatrixRMaj achievedObjective = new DMatrixRMaj(3, 1);
      objectivePosition.get(taskObjectiveExpected);
      taskObjectiveExpected.add(2, 0, -0.5 * timeOfConstraint * timeOfConstraint * -Math.abs(gravityZ));

      DMatrixRMaj taskJacobianExpected = new DMatrixRMaj(3, 2 * (6 + rhoHelper.getRhoSize() * 4));
      taskJacobianExpected.set(0, start, timeOfConstraint);
      taskJacobianExpected.set(0, start + 1, 1.0);
      taskJacobianExpected.set(1, start + 2, timeOfConstraint);
      taskJacobianExpected.set(1, start + 3, 1.0);
      taskJacobianExpected.set(2, start + 4, timeOfConstraint);
      taskJacobianExpected.set(2, start + 5, 1.0);



      for (int rhoIdx  = 0; rhoIdx < rhoHelper.getRhoSize(); rhoIdx++)
      {
         int startIdx = 12 + 4 * helper.getRhoSize() + 4 * rhoIdx;
         double rhoValue = Math.exp(omega * timeOfConstraint) * solution.get(startIdx, 0);
         rhoValue += Math.exp(-omega * timeOfConstraint) * solution.get(startIdx + 1, 0);
         rhoValue += timeOfConstraint * timeOfConstraint * timeOfConstraint * solution.get(startIdx + 2, 0);
         rhoValue += timeOfConstraint * timeOfConstraint * solution.get(startIdx + 3, 0);

         assertEquals(rhoValue, rhoValueVector.get(rhoIdx), 1e-5);
         solvedPositionAtConstraint.scaleAdd(rhoValue, rhoHelper.getBasisVector(rhoIdx), solvedPositionAtConstraint);

         taskJacobianExpected.set(0, startIdx, rhoHelper.getBasisVector(rhoIdx).getX() * Math.exp(omega * timeOfConstraint));
         taskJacobianExpected.set(0, startIdx + 1, rhoHelper.getBasisVector(rhoIdx).getX() * Math.exp(-omega * timeOfConstraint));
         taskJacobianExpected.set(0, startIdx + 2, rhoHelper.getBasisVector(rhoIdx).getX() * timeOfConstraint * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(0, startIdx + 3, rhoHelper.getBasisVector(rhoIdx).getX() * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(1, startIdx, rhoHelper.getBasisVector(rhoIdx).getY() * Math.exp(omega * timeOfConstraint));
         taskJacobianExpected.set(1, startIdx + 1, rhoHelper.getBasisVector(rhoIdx).getY() * Math.exp(-omega * timeOfConstraint));
         taskJacobianExpected.set(1, startIdx + 2, rhoHelper.getBasisVector(rhoIdx).getY() * timeOfConstraint * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(1, startIdx + 3, rhoHelper.getBasisVector(rhoIdx).getY() * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(2, startIdx, rhoHelper.getBasisVector(rhoIdx).getZ() * Math.exp(omega * timeOfConstraint));
         taskJacobianExpected.set(2, startIdx + 1, rhoHelper.getBasisVector(rhoIdx).getZ() * Math.exp(-omega * timeOfConstraint));
         taskJacobianExpected.set(2, startIdx + 2, rhoHelper.getBasisVector(rhoIdx).getZ() * timeOfConstraint * timeOfConstraint * timeOfConstraint);
         taskJacobianExpected.set(2, startIdx + 3, rhoHelper.getBasisVector(rhoIdx).getZ() * timeOfConstraint * timeOfConstraint);
      }
      solvedPositionAtConstraint.scaleAdd(0.5 * timeOfConstraint * timeOfConstraint, gravityVector, solvedPositionAtConstraint);

      EjmlUnitTests.assertEquals(taskJacobianExpected, solver.qpInputTypeA.taskJacobian, 1e-5);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, solver.qpInputTypeA.taskObjective, 1e-5);

      CommonOps_DDRM.mult(taskJacobianExpected, solution, achievedObjective);
      EjmlUnitTests.assertEquals(taskObjectiveExpected, achievedObjective, 1e-4);

      DMatrixRMaj solverInput_H_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), taskJacobianExpected.getNumCols());
      DMatrixRMaj solverInput_f_Expected = new DMatrixRMaj(taskJacobianExpected.getNumCols(), 1);

      CommonOps_DDRM.multInner(taskJacobianExpected, solverInput_H_Expected);
      CommonOps_DDRM.multTransA(-1.0, taskJacobianExpected, taskObjectiveExpected, solverInput_f_Expected);

      MatrixTools.addDiagonal(solverInput_H_Expected, regularization);

      EjmlUnitTests.assertEquals(solverInput_H_Expected, solver.solverInput_H, 1e-10);
      EjmlUnitTests.assertEquals(solverInput_f_Expected, solver.solverInput_f, 1e-10);

      EuclidCoreTestTools.assertTuple3DEquals(objectivePosition, solvedObjectivePositionTuple, 1e-4);
      EuclidCoreTestTools.assertTuple3DEquals(objectivePosition, solvedPositionAtConstraint, 1e-4);
   }


}
