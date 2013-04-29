package us.ihmc.sensorProcessing.stateEstimation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.ejml.alg.dense.mult.MatrixVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.AbstractControlFlowElement;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.kalman.YoKalmanFilter;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.MeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.processModelElements.ProcessModelElement;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class ComposableStateEstimator extends AbstractControlFlowElement
{
   private static final boolean DEGUG = false;
   
   protected final YoVariableRegistry registry;
   protected ComposableStateEstimatorKalmanFilter kalmanFilter;
   private final List<Runnable> postStateChangeRunnables = new ArrayList<Runnable>();
   private final ProcessModelAssembler processModelAssembler;
   private final SummaryStatistics statistics = new SummaryStatistics();

   // model elements
   private final List<MeasurementModelElement> measurementModelElements = new ArrayList<MeasurementModelElement>();

   public ComposableStateEstimator(String name, double controlDT, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.processModelAssembler = new ProcessModelAssembler(controlDT);
      parentRegistry.addChild(registry);
   }

   public void startComputation()
   {
      long t0 = System.currentTimeMillis();
      kalmanFilter.configure();
      kalmanFilter.predict(null);
      kalmanFilter.update(null);
      long tf = System.currentTimeMillis();
      statistics.addValue((double) (tf - t0));
//      System.out.println(statistics.getMean());
   }

   public void addProcessModelElement(ControlFlowOutputPort<?> statePort, ProcessModelElement processModelElement)
   {
      processModelAssembler.addProcessModelElement(processModelElement, statePort);

      registerOutputPort(statePort);
   }

   public void addMeasurementModelElement(MeasurementModelElement measurementModelElement)
   {
      measurementModelElements.add(measurementModelElement);
   }

   public void addPostStateChangeRunnable(Runnable runnable)
   {
      this.postStateChangeRunnables.add(runnable);
   }

   public void initialize()
   {
      ProcessModel processModel = processModelAssembler.getProcessModel();
      
      printIfDebug("\nComposableStateEstimator. processModel:\n" + processModel + "\n");
      
      MeasurementModel measurementModel = new MeasurementModel(measurementModelElements, processModel.getStateStartIndices(), processModel.getStateMatrixSize());
      kalmanFilter = new ComposableStateEstimatorKalmanFilter(processModel, measurementModel);

      runPostStateChangeRunnables();
      kalmanFilter.configure();
      initializeCovariance();
   }

   private void initializeCovariance()
   {
      DenseMatrix64F x = kalmanFilter.getState();
      kalmanFilter.computeSteadyStateGainAndCovariance(50); // TODO: magic number
      DenseMatrix64F P = kalmanFilter.getCovariance();
      CommonOps.scale(10.0, P); // TODO: magic number
      kalmanFilter.setState(x, P);
   }

   public void waitUntilComputationIsDone()
   {
      // empty
   }

   protected class ComposableStateEstimatorKalmanFilter extends YoKalmanFilter
   {
      private final ProcessModel processModel;
      private final MeasurementModel measurementModel;
      private final DenseMatrix64F correction;

      public ComposableStateEstimatorKalmanFilter(ProcessModel processModel, MeasurementModel measurementModel)
      {
         super(ComposableStateEstimatorKalmanFilter.class.getSimpleName(), ComposableStateEstimator.this.registry);
         this.processModel = processModel;
         this.measurementModel = measurementModel;
         correction = new DenseMatrix64F(processModel.getStateMatrixSize(), 1);
      }

      protected void configure()
      {
         processModel.updateMatrices();
         measurementModel.updateMatrices();

         super.configure(processModel.getStateMatrix(), processModel.getInputMatrix(), measurementModel.getOutputMatrix());
         setProcessNoiseCovariance(processModel.getProcessCovarianceMatrix());
         setMeasurementNoiseCovariance(measurementModel.getMeasurementCovarianceMatrix());
      }

      @Override
      protected void updateAPrioriState(DenseMatrix64F x, DenseMatrix64F u)
      {
         processModel.propagateState();
         runPostStateChangeRunnables();
      }

      @Override
      protected void updateAPosterioriState(DenseMatrix64F x, DenseMatrix64F y, DenseMatrix64F K)
      {
         DenseMatrix64F residual = measurementModel.computeResidual();
         MatrixVectorMult.mult(K, residual, correction);
         processModel.correctState(correction);
         runPostStateChangeRunnables();
      }
   }
   
   private void runPostStateChangeRunnables()
   {
      for (Runnable runnable : postStateChangeRunnables)
      {
         runnable.run();
      }
   }
   
   private void printIfDebug(String message)
   {
      if (DEGUG) System.out.println(message);
   }

}
