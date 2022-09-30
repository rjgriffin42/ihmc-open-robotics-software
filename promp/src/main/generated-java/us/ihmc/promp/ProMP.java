// Targeted by JavaCPP version 1.5.7: DO NOT EDIT THIS FILE

package us.ihmc.promp;

import java.nio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

import static us.ihmc.promp.global.promp.*;


    /**
     * \brief Class that implements a multi-dimensional Probabilistic Motion Primitive. References:
     * - Paraschos A, Daniel C, Peters J, Neumann G. Probabilistic movement primitives. Advances in neural information processing systems. 2013. <a href="https://www.ias.informatik.tu-darmstadt.de/uploads/Publications/Paraschos_NIPS_2013.pdf">[pdf]</a>
	 * - Paraschos A, Daniel C, Peters J, Neumann G. Using probabilistic movement primitives in robotics. Autonomous Robots. 2018 Mar;42(3):529-51. <a href="https://www.ias.informatik.tu-darmstadt.de/uploads/Team/AlexandrosParaschos/promps_auro.pdf">[pdf]</a>.
     */
    @Namespace("promp") @NoOffset @Properties(inherit = us.ihmc.promp.presets.PrompInfoMapper.class)
public class ProMP extends Pointer {
        static { Loader.load(); }
        /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
        public ProMP(Pointer p) { super(p); }
    
        /** \brief constructor: The constructor will parameterize a phase vector, and compute the basis function matrix
         * {@code  \Psi } for all phase steps. Then, for each demonstration within the std::vector \p data, it will estimate a.
         * vector of basis functions' weights {@code  w_i }.
         * Lastly, it fits a gaussian over all the weight vectors to obtain {@code  \mu_w }, and {@code  \Sigma_w }
         *
         *	@param data 	 vector of trajectories. All the trajectories MUST have the same length and same dimension.
         *	@param num_bf 	 number of basis functions
         *	@param std_bf	 standard deviation; this is set automatically to  1.0 / (n_rbf*n_rbf) if std_bf <= 0
         */
        public ProMP(@Const @ByRef TrajectoryVector data, int num_bf, double std_bf/*=-1*/) { super((Pointer)null); allocate(data, num_bf, std_bf); }
        private native void allocate(@Const @ByRef TrajectoryVector data, int num_bf, double std_bf/*=-1*/);
        public ProMP(@Const @ByRef TrajectoryVector data, int num_bf) { super((Pointer)null); allocate(data, num_bf); }
        private native void allocate(@Const @ByRef TrajectoryVector data, int num_bf);

        /** \brief constructor: The constructor will parameterize a phase vector, and compute the basis function matrix
         * {@code  \Psi } for all phase steps. Then, for each demonstration within the std::vector \p data, it will estimate a.
         * vector of basis functions' weights {@code  w_i }.
         * Lastly, it fits a gaussian over all the weight vectors to obtain {@code  \mu_w }, and {@code  \Sigma_w }
         *
         *	@param data 	 vector of trajectories. All the trajectories MUST have the same length and same dimension.
         *	@param num_bf 	 number of basis functions
         *	@param std_bf	 standard deviation; this is set automatically to  1.0 / (n_rbf*n_rbf) if std_bf <= 0
         */
        public ProMP(@Const @ByRef TrajectoryGroup data, int num_bf, double std_bf/*=-1*/) { super((Pointer)null); allocate(data, num_bf, std_bf); }
        private native void allocate(@Const @ByRef TrajectoryGroup data, int num_bf, double std_bf/*=-1*/);
        public ProMP(@Const @ByRef TrajectoryGroup data, int num_bf) { super((Pointer)null); allocate(data, num_bf); }
        private native void allocate(@Const @ByRef TrajectoryGroup data, int num_bf);

        /**
         * \brief      This is an alternate constructor that uses prelearned weights, covariance and number of samples in the trajectory
         *
         * @param w [in]    	The mean of weights' distribution
         * @param cov_w [in]    The co-variance of weights' distribution
         * @param std_bf [in]   The standard deviation of each basis function
         * @param n_sample [in] The number of samples required for the trajectory
         */
        public ProMP(@Const @ByRef EigenVectorXd w, @Const @ByRef EigenMatrixXd cov_w, double std_bf, int n_sample, @Cast("size_t") long dims, double time_mod/*=1.0*/) { super((Pointer)null); allocate(w, cov_w, std_bf, n_sample, dims, time_mod); }
        private native void allocate(@Const @ByRef EigenVectorXd w, @Const @ByRef EigenMatrixXd cov_w, double std_bf, int n_sample, @Cast("size_t") long dims, double time_mod/*=1.0*/);
        public ProMP(@Const @ByRef EigenVectorXd w, @Const @ByRef EigenMatrixXd cov_w, double std_bf, int n_sample, @Cast("size_t") long dims) { super((Pointer)null); allocate(w, cov_w, std_bf, n_sample, dims); }
        private native void allocate(@Const @ByRef EigenVectorXd w, @Const @ByRef EigenMatrixXd cov_w, double std_bf, int n_sample, @Cast("size_t") long dims);

        /**
         * \brief      Gets the basis function matrix for the current trained ProMP.
         *
         * @return     The basis function {@code  \Psi }.
         */
        public native @Const @ByRef EigenMatrixXd get_basis_function();

        /** \brief	generates basis functions
         *	@param phase
         *	@return matrix of basis functions
         */
        public native @ByVal EigenMatrixXd generate_basis_function(@Const @ByRef EigenVectorXd phase);

        /** \brief	maps time vector into a phase vector give a desired number of timesteps
         */
        public native @ByVal EigenVectorXd compute_phase(@Cast("size_t") long timesteps);

        /**
         * \brief      Gets the phase vector.
         *
         * @return     The phase vector.
         */
        public native @Const @ByRef EigenVectorXd get_phase();

        /**
         * \brief      Gets the vector of the mean of all weights {@code  \mu_w }.
         *
         * @return     The weights.
         */
        public native @Const @ByRef EigenVectorXd get_weights();

        /**
         * \brief      Gets the co-variance matrix {@code  \Sigma_w }.
         *
         * @return     The co-variance matrix.
         */
        public native @Const @ByRef EigenMatrixXd get_covariance();

        /**
         * \brief      Gets the number of samples for the trajectory {@code  \s_ }.
         *
         * @return     The number of samples for the trajectory
         */
        public native int get_n_samples();

        /**
         * \brief      Gets the trajectory length.
         *
         * @return     The trajectory length
         */
        public native int get_traj_length();

        /**
         * \brief      Gets the std deviation {@code  \std_bf } of the ProMP
         * @return     The std deviation.
         */
        public native double get_std_bf();

        /**
         * \brief	return the number of dimensions represented and generated by the promp
         * @return	Number of dimensions of the generated function
         */
        public native @Cast("size_t") long get_dims();

        /**
         * \brief      Gets the average time modulation in the demonstrations used to train the ProMP
         * @return     m_alpha_ The avergae demo time modulation
         */
        public native double get_mean_demo_time_mod();

        /**
         * \brief		Set the ridge factor value which condition the pphi inverse. Helps against singularities
         * @param ridge_factor [in]
         */
        public native void set_ridge_factor(double ridge_factor);

        /**
         * \brief      Generates MEAN trajectory based on current weights distribution and rbf
         *
         * @return     Mean trajectory
         */
        public native @ByVal EigenMatrixXd generate_trajectory();

        /**
         * \brief      Generates MEAN trajectory based on current weights distribution and
         * a required number of steps
         *
         * @param req_num_steps [in]  The requested number of steps for trajectory
         *
         * @return     Mean trajectory
         */
        public native @ByVal EigenMatrixXd generate_trajectory(@Cast("size_t") long req_num_steps);

        /**
         * \brief      Generates MEAN trajectory based on current weights distributions
         * and a required phase speed {@code  \dot{z}_t }.
         *
         * @param req_phase_speed [in]  The request phase speed. <b> To play trajectory at orignal speed set
         * {@code  \dot{z}_t = 1.0 } </b>.
         *
         * @return     { description_of_the_return_value }
         */
        public native @ByVal EigenMatrixXd generate_trajectory_with_speed(double req_phase_speed);

        public native @ByVal EigenMatrixXd generate_trajectory_at(@Const @ByRef EigenVectorXd phase);

        /** \brief	set desired goal/end point for trajectory
         *	@param 	goal 	desired value at end
         *	@param	std 	desired standard deviation. <b> typically around {@code  10^{-6}} for accuracy </b>
         *	@return
         */
        public native void condition_goal(@Const @ByRef EigenVectorXd goal, @Const @ByRef EigenMatrixXd std);

        /** \brief	set desired start/initial point for trajectory
         *	@param 	start 	desired value at start
         *	@param	std   	desired standard deviation.<b> typically around {@code  10^{-6}} for accuracy </b>
         *	@return
         */
        public native void condition_start(@Const @ByRef EigenVectorXd start, @Const @ByRef EigenMatrixXd std);

        /**
         * \brief      Generates standard deviation vector with the standard deviation for every time step, with a certain number of time steps
         *
         * @param req_num_steps [in]  The requested number steps. if <= 0 (default) use the internal phase parametrization
         *
         * @return     Standard Deviation Vector {@code  DIAG( \Sigma ) }
         */
        public native @ByVal EigenMatrixXd gen_traj_std_dev(@Cast("size_t") long req_num_steps/*=0*/);
        public native @ByVal EigenMatrixXd gen_traj_std_dev();

        /**
         * \brief      Generates step covariance matrices for for each step of the trajectory, with a certain number of time steps
         *
         * @param req_num_steps [in]  The requested number steps. if <= 0 (default) use the internal phase parametrization
         *
         * @return     std::vector containg and entry for each step covariance matrix
         */
        public native @StdVector EigenMatrixXd generate_trajectory_covariance(@Cast("size_t") long req_num_steps/*=0*/);
        public native @StdVector EigenMatrixXd generate_trajectory_covariance();

        /**
         * \brief      Conditions all via points registered in 'viaPoints_'.
         * It updates  \a _mean_w and _cov_w  and clear _via_points vector
         */
        public native void condition_via_points(@StdVector IntVectorMatrixTuple via_points);

        /**
         * \brief      Conditions all via points registered in 'viaPoints_'.
         * It updates  \a _mean_w and _cov_w  and clear _via_points vector
         */
        /** \brief	set via point for trajectory
         *	@param 	t 			time at which via point is to be added (between 0 and LAST TIME STEP)
         *	@param	via_point 	desired value at via point
         *	@param	std   		desired standard deviation.<b> typically around {@code  10^{-6}} for accuracy </b>
         *	@return
         *	\todo Use phase {@code  z_t} instead of time step t.
         */
        public native void condition_via_point(int t, @Const @ByRef EigenVectorXd via_point, @Const @ByRef EigenMatrixXd std);

        public native @ByVal EigenVectorXd get_upper_weights(double K);

        public native @ByVal EigenVectorXd get_lower_weights(double K);

        /**
         * \brief      Overloads << operator in order to print the weights of a given promp
         *
         * @param      out   The out
         * @param mp [in]    THIS promp
         *
         */
        

        /**
         * \brief generate the speed from a number of steps
         * @param steps number of steps required in the trajectory
         * @return double the phase speed to obtain the desired number of steps in the trajectory
         */
        public native double phase_speed_from_steps(int steps);
    }
