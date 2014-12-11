package us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization;

//~--- non-JDK imports --------------------------------------------------------

import org.ejml.data.DenseMatrix64F;

import us.ihmc.convexOptimization.QpOASESCWrapper;
import us.ihmc.utilities.exeptions.NoConvergenceException;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;

//~--- JDK imports ------------------------------------------------------------


public class OASESConstrainedQPSolver extends ConstrainedQPSolver {

    /**
     *
     * min 0.5*x'Hx + g'x
     *
     * st lbA <= Ax <= ubA
     *     lb <= x <= ub
     *
     * matrices are row-major
     *
     * @param nWSR - number of working set re-calculation
     * @param cputime - maximum cputime, null
     * @param x - initial and return variable to be optimized
     * @return returnCode from C-API
     */
    YoVariableRegistry     registry                = new YoVariableRegistry(getClass().getSimpleName());
    DoubleYoVariable       maxCPUTime              = new DoubleYoVariable("maxCPUTime", registry);
    DoubleYoVariable       currentCPUTime          = new DoubleYoVariable("currentCPUTime", registry);
    IntegerYoVariable      maxWorkingSetChange     = new IntegerYoVariable("maxWorkingSetchange", registry);
    IntegerYoVariable      currentWorkingSetChange = new IntegerYoVariable("currentWorkingSetchange", registry);

    
    QpOASESCWrapper qpWrapper;


    public OASESConstrainedQPSolver(YoVariableRegistry parentRegistry) {
    	this(1,1, parentRegistry);
    }
    public OASESConstrainedQPSolver(int nvar, int ncon, YoVariableRegistry parentRegistry) {
        maxCPUTime.set(Double.POSITIVE_INFINITY);
        maxWorkingSetChange.set(10000);
        qpWrapper = new QpOASESCWrapper();

        if (parentRegistry != null) {
            parentRegistry.addChild(this.registry);
        }
    }


    @Override
    public boolean supportBoxConstraints() {
        return true;
    }

    @Override
    public void solve(DenseMatrix64F Q, DenseMatrix64F f, DenseMatrix64F Aeq, DenseMatrix64F beq, DenseMatrix64F Ain,
                      DenseMatrix64F bin, DenseMatrix64F x, boolean initialize)
            throws NoConvergenceException {
        solve(Q, f, Aeq, beq, Ain, bin, null, null, x, initialize);
    }

    @Override
    public void solve(DenseMatrix64F Q, DenseMatrix64F f, DenseMatrix64F Aeq, DenseMatrix64F beq, DenseMatrix64F Ain,
                      DenseMatrix64F bin, DenseMatrix64F lb, DenseMatrix64F ub, DenseMatrix64F x, boolean initialize)
            throws NoConvergenceException {

    	qpWrapper.setMaxCpuTime(maxCPUTime.getDoubleValue());
    	qpWrapper.setMaxWorkingSetChanges(maxWorkingSetChange.getIntegerValue());
    	qpWrapper.solve(Q, f, Aeq, beq, Ain, bin, lb, ub, x, initialize);
    	currentCPUTime.set(qpWrapper.getLastCpuTime());
    	currentWorkingSetChange.set(qpWrapper.getLastWorkingSetChanges());
    }
}
