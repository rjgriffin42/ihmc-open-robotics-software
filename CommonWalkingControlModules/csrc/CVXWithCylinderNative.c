/*
 * CVXWithCylinderNative.c
 *
 *  Created on: May 7, 2013
 *      Author: graythomas
 *      based on work by twan.
 */

#include <stdio.h>
#include "CVXWithCylinderNative.h"
#include "CVXWithCylinder/solver.h"

#define nSupportVectors us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nSupportVectors
#define nPointsPerPlane us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nPointsPerPlane
#define nPlanes us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nPlanes
#define nCylinders us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nCylinders
#define nCylinderVectors us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nCylinderVectors
#define nCylinderBoundedVariables us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nCylinderBoundedVariables
#define wrenchLength us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_wrenchLength
#define nDoF us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nDoF
#define nNull us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_nNull

#define rhoSize (nSupportVectors * nPointsPerPlane * nPlanes+nCylinderVectors*nCylinders)
#define phiSize (nCylinders * nCylinderBoundedVariables)
#define vdSize nDoF

#define ASize (wrenchLength * nDoF)
#define bSize wrenchLength
#define CSize wrenchLength
#define JsSize (nDoF * nDoF)
#define psSize nDoF
#define WsSize nDoF
#define LambdaSize nDoF
#define QrhoSize (wrenchLength * rhoSize)
#define QphiSize (wrenchLength * phiSize)
#define cSize wrenchLength
#define rhoMinSize rhoSize
#define phiMinSize phiSize
#define phiMaxSize phiSize
#define NSize (nDoF * nNull)
#define zSize nNull



Vars vars;
Params params;
Workspace work;
Settings settings;


jobject AByteBuffer;
jobject bByteBuffer;
jobject CByteBuffer;
jobject JsByteBuffer;
jobject psByteBuffer;
jobject WsByteBuffer;
jobject LambdaByteBuffer;
jobject QrhoByteBuffer;
jobject QphiByteBuffer;
jobject cByteBuffer;
jobject NByteBuffer;
jobject zByteBuffer;
jobject rhoMinByteBuffer;
jobject phiMinByteBuffer;
jobject phiMaxByteBuffer;
jobject rhoByteBuffer;
jobject phiByteBuffer;
jobject vdByteBuffer;

JNIEXPORT void JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_initialize
  (JNIEnv * env, jclass jClass)
{
	set_defaults();
	setup_indexing();
	settings.verbose = 0;

	AByteBuffer = (*env)->NewDirectByteBuffer(env, params.A, sizeof(double) * ASize);
    bByteBuffer = (*env)->NewDirectByteBuffer(env, params.b, sizeof(double) * bSize);
    CByteBuffer = (*env)->NewDirectByteBuffer(env, params.C, sizeof(double) * CSize);
    JsByteBuffer = (*env)->NewDirectByteBuffer(env, params.Js, sizeof(double) * JsSize);
    psByteBuffer = (*env)->NewDirectByteBuffer(env, params.ps, sizeof(double) * psSize);
    WsByteBuffer = (*env)->NewDirectByteBuffer(env, params.Ws, sizeof(double) * WsSize);
    LambdaByteBuffer = (*env)->NewDirectByteBuffer(env, params.Lambda, sizeof(double) * LambdaSize);
    QrhoByteBuffer = (*env)->NewDirectByteBuffer(env, params.Qrho, sizeof(double) * QrhoSize);
    QphiByteBuffer = (*env)->NewDirectByteBuffer(env, params.Qphi, sizeof(double) * QphiSize);
    cByteBuffer = (*env)->NewDirectByteBuffer(env, params.c, sizeof(double) * cSize);
    NByteBuffer = (*env)->NewDirectByteBuffer(env, params.N, sizeof(double) * NSize);
    zByteBuffer = (*env)->NewDirectByteBuffer(env, params.z, sizeof(double) * zSize);
    rhoMinByteBuffer = (*env)->NewDirectByteBuffer(env, params.rhoMin, sizeof(double) * rhoMinSize);
    phiMinByteBuffer = (*env)->NewDirectByteBuffer(env, params.phiMin, sizeof(double) * phiMinSize);
    phiMaxByteBuffer = (*env)->NewDirectByteBuffer(env, params.phiMax, sizeof(double) * phiMaxSize);
    rhoByteBuffer = (*env)->NewDirectByteBuffer(env, vars.rho, sizeof(double) * rhoSize);
    phiByteBuffer = (*env)->NewDirectByteBuffer(env, vars.phi, sizeof(double) * phiSize);
    vdByteBuffer = (*env)->NewDirectByteBuffer(env, vars.vd, sizeof(double) * vdSize);
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getABuffer
  (JNIEnv * env, jclass jClass)
{
    return AByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getbBuffer
  (JNIEnv * env, jclass jClass)
{
    return bByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getCBuffer
  (JNIEnv * env, jclass jClass)
{
    return CByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getJsBuffer
  (JNIEnv * env, jclass jClass)
{
    return JsByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getpsBuffer
  (JNIEnv * env, jclass jClass)
{
    return psByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getWsBuffer
  (JNIEnv * env, jclass jClass)
{
    return WsByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getLambdaBuffer
  (JNIEnv * env, jclass jClass)
{
    return LambdaByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getQrhoBuffer
  (JNIEnv * env, jclass jClass)
{
    return QrhoByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getQphiBuffer
	(JNIEnv * env, jclass jClass)
{
	return QphiByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getcBuffer
  (JNIEnv * env, jclass jClass)
{
    return cByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getNBuffer
  (JNIEnv * env, jclass jClass)
{
    return NByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getzBuffer
  (JNIEnv * env, jclass jClass)
{
    return zByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getrhoMinBuffer
  (JNIEnv * env, jclass jClass)
{
    return rhoMinByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getphiMinBuffer
  (JNIEnv * env, jclass jClass)
{
	return phiMinByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getphiMaxBuffer
  (JNIEnv * env, jclass jClass)
{
	return phiMaxByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getrhoBuffer
  (JNIEnv * env, jclass jClass)
{
    return rhoByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getphiBuffer
  (JNIEnv * env, jclass jClass)
{
	return phiByteBuffer;
}

JNIEXPORT jobject JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getvdBuffer
  (JNIEnv * env, jclass jClass)
{
    return vdByteBuffer;
}

JNIEXPORT jint JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_solveNative
  (JNIEnv * env, jclass jClass, jdouble wRho)
{
	int numberOfIterations;

	params.wRho[0] = wRho;
	numberOfIterations = solve();

	if(work.converged == 1)
	{
		return numberOfIterations;
	}
	else
	{
		return -1;
	}
}

JNIEXPORT jdouble JNICALL Java_us_ihmc_commonWalkingControlModules_controlModules_nativeOptimization_CVXWithCylinderNative_getOptValNative
  (JNIEnv * env, jclass jClass)
{
	return work.optval;
}




