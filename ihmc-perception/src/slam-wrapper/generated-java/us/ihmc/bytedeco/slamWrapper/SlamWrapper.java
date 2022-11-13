// Targeted by JavaCPP version 1.5.7: DO NOT EDIT THIS FILE

package us.ihmc.bytedeco.slamWrapper;

import java.nio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

public class SlamWrapper extends us.ihmc.bytedeco.slamWrapper.presets.SlamWrapperInfoMapper {
    static { Loader.load(); }

// Parsed from include/FactorGraphExternal.h

// #pragma once

// #include "FactorGraphHandler.h"

public static class FactorGraphExternal extends Pointer {
    static { Loader.load(); }
    /** Default native constructor. */
    public FactorGraphExternal() { super((Pointer)null); allocate(); }
    /** Native array allocator. Access with {@link Pointer#position(long)}. */
    public FactorGraphExternal(long size) { super((Pointer)null); allocateArray(size); }
    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
    public FactorGraphExternal(Pointer p) { super(p); }
    private native void allocate();
    private native void allocateArray(long size);
    @Override public FactorGraphExternal position(long position) {
        return (FactorGraphExternal)super.position(position);
    }
    @Override public FactorGraphExternal getPointer(long i) {
        return new FactorGraphExternal((Pointer)this).offsetAddress(i);
    }

        // Expects packed Pose3
        public native void addPriorPoseFactor(int index, FloatPointer pose);
        public native void addPriorPoseFactor(int index, FloatBuffer pose);
        public native void addPriorPoseFactor(int index, float[] pose);

        // Expects packed Pose3
        public native void addOdometryFactor(FloatPointer odometry, int poseId);
        public native void addOdometryFactor(FloatBuffer odometry, int poseId);
        public native void addOdometryFactor(float[] odometry, int poseId);

        // Expects 4x4 homogenous transform matrix to insert Pose3 factor
        public native void addOdometryFactorExtended(FloatPointer odometry, int poseId);
        public native void addOdometryFactorExtended(FloatBuffer odometry, int poseId);
        public native void addOdometryFactorExtended(float[] odometry, int poseId);

        // Expects packed Vector4
        public native void addOrientedPlaneFactor(FloatPointer lmMean, int lmId, int poseIndex);
        public native void addOrientedPlaneFactor(FloatBuffer lmMean, int lmId, int poseIndex);
        public native void addOrientedPlaneFactor(float[] lmMean, int lmId, int poseIndex);

        public native void optimize();

        public native void optimizeISAM2(@Cast("uint8_t") byte numberOfUpdates);

        public native void clearISAM2();

        // Expects packed Pose3
        public native void setPoseInitialValue(int index, FloatPointer value);
        public native void setPoseInitialValue(int index, FloatBuffer value);
        public native void setPoseInitialValue(int index, float[] value);

        // Expects 4x4 homogenous transform matrix as initial value for Pose3
        public native void setPoseInitialValueExtended(int index, FloatPointer value);
        public native void setPoseInitialValueExtended(int index, FloatBuffer value);
        public native void setPoseInitialValueExtended(int index, float[] value);

        // Expects packed OrientedPlane3
        public native void setOrientedPlaneInitialValue(int landmarkId, FloatPointer value);
        public native void setOrientedPlaneInitialValue(int landmarkId, FloatBuffer value);
        public native void setOrientedPlaneInitialValue(int landmarkId, float[] value);

        // Expects packed Vector6
        public native void createOdometryNoiseModel(FloatPointer odomVariance);
        public native void createOdometryNoiseModel(FloatBuffer odomVariance);
        public native void createOdometryNoiseModel(float[] odomVariance);

        // Expects packed Vector3
        public native void createOrientedPlaneNoiseModel(FloatPointer lmVariances);
        public native void createOrientedPlaneNoiseModel(FloatBuffer lmVariances);
        public native void createOrientedPlaneNoiseModel(float[] lmVariances);

        public native void addGenericProjectionFactor(FloatPointer point, int lmId, int poseIndex);
        public native void addGenericProjectionFactor(FloatBuffer point, int lmId, int poseIndex);
        public native void addGenericProjectionFactor(float[] point, int lmId, int poseIndex);

        public native void setPointLandmarkInitialValue(int landmarkId, FloatPointer value);
        public native void setPointLandmarkInitialValue(int landmarkId, FloatBuffer value);
        public native void setPointLandmarkInitialValue(int landmarkId, float[] value);

        public native void printResults();

        public native void helloWorldTest();

        public native void visualSLAMTest();
}

}
