// Targeted by JavaCPP version 1.5.8: DO NOT EDIT THIS FILE

package us.ihmc.promp;

import java.nio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

import static us.ihmc.promp.global.promp.*;


    /**
     * \brief  Class that represents a multidimensional trajectory.
     * A trajectory is described by the values at each timestep and the speed parameter.
     * speed indicates how the trajectory has been modulated, 
     * for example speed=2 means that the original trajectory had twice the timesteps.
     */
    @Namespace("promp") @NoOffset @Properties(inherit = us.ihmc.promp.presets.ProMPInfoMapper.class)
public class Trajectory extends Pointer {
        static { Loader.load(); }
        /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
        public Trajectory(Pointer p) { super(p); }
        /** Native array allocator. Access with {@link Pointer#position(long)}. */
        public Trajectory(long size) { super((Pointer)null); allocateArray(size); }
        private native void allocateArray(long size);
        @Override public Trajectory position(long position) {
            return (Trajectory)super.position(position);
        }
        @Override public Trajectory getPointer(long i) {
            return new Trajectory((Pointer)this).offsetAddress(i);
        }
    
        
        /**
         *  \brief default constructor. Build empty trajectory.
         */
        public Trajectory() { super((Pointer)null); allocate(); }
        private native void allocate();

        /**
         *  \brief constructor that build a trajectory starting from data and speed
         *  \param data Eigen::Matrix containing the raw data, each column is a different dof
         *  \param speed speed of the original trajectory (time-scale factor: e.g., 2.0 to go from 200 time-steps to 100 time-steps)
         */
        public Trajectory(@Const @ByRef EigenMatrixXd data, double speed/*=1.0*/) { super((Pointer)null); allocate(data, speed); }
        private native void allocate(@Const @ByRef EigenMatrixXd data, double speed/*=1.0*/);
        public Trajectory(@Const @ByRef EigenMatrixXd data) { super((Pointer)null); allocate(data); }
        private native void allocate(@Const @ByRef EigenMatrixXd data);

        /**
         *  \brief return number of dimensions of the trajectory
         */
        public native @Cast("size_t") long dims();

        /**
         *  \brief return number of timesteps in the trajectory
         */
        public native @Cast("size_t") long timesteps();

        /**
         *  \brief return the trajectory' speed
         */
        public native double speed();

        /**
         *  \brief return the raw data as Eigen::Matrix
         */
        public native @Const @ByRef EigenMatrixXd matrix();

        /**
         *  \brief return monodimensional trajectory from the selected dimension
         *  \param dim  dimension used to create the returned trajectory
         */
        public native @ByVal Trajectory sub_trajectory(@Cast("size_t") long dim);

        /**
         *  \brief return  trajectory using data from the selected dimensions
         *  \param dim  list of dimensions used to create the returned trajectory
         */
        public native @ByVal Trajectory sub_trajectory(@Const @ByRef SizeTVector dims);

        /**
         *  \brief modulate the trajectory to the desired number of timesteps
         *  Adjust speed according to speed = this->speed() * this->timesteps() / timesteps
         *  \param timesteps  desired number of steps in the trajectory
         */
        public native void modulate_in_place(@Cast("size_t") long timesteps, @Cast("bool") boolean fast/*=true*/);
        public native void modulate_in_place(@Cast("size_t") long timesteps);

        /**
         *  \brief create a new modulated trajectory with the desired number of timesteps
         *  Adjust its speed according to speed = this->speed() * this->timesteps() / timesteps
         *  \param timesteps  desired number of steps in the trajectory
         */
        public native @ByVal Trajectory modulate(@Cast("size_t") long steps, @Cast("bool") boolean fast/*=true*/);
        public native @ByVal Trajectory modulate(@Cast("size_t") long steps);

        /**
         *  \brief compute the Euclidean distance between this and a second trajectory
         *  \param other   trajectory used to compute the distance with
         *  \param modulate  if false the distance is computed using data until the smaller trajectory lenght,
         *  if true the other trajectory is modulated to this trajectory length before computing the distance
         */
        public native double distance(@Const @ByRef Trajectory other, @Cast("bool") boolean modulate/*=false*/);
        public native double distance(@Const @ByRef Trajectory other);

        /**
         *  \brief infer the speed of a trajectory starting from the raw data
         *  \param obs_traj  data from which speed is inferred, comparing it to this trajectory
         *  \param lb  lower bound for inferred speed 
         *  \param ub  upper bound for inferred speed
         *  \param steps  number of speeds to be tested (linspace(lb, ub, steps))
         */
        public native double infer_speed(@Const @ByRef EigenMatrixXd obs_traj, double lb, double ub, @Cast("size_t") long steps);

       /**
        *  \brief compute the closest trajectory to current observed trajectory among a set of trajectories
        *  \param obs_traj   observed trajectory
        *  \param demo_trajectories  vector of trajectories to compute the difference with
        *  \return  index of the closest trajectory in vector to observed trajectory
        */
        public static native int infer_closest_trajectory(@Const @ByRef EigenMatrixXd obs_traj, @Const @ByRef TrajectoryVector demo_trajectories);
    }
