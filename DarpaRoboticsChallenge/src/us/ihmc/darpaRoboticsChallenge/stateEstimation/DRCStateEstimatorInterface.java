package us.ihmc.darpaRoboticsChallenge.stateEstimation;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.sensorProcessing.stateEstimation.StateEstimator;

import com.yobotics.simulationconstructionset.robotController.RobotController;

public interface DRCStateEstimatorInterface extends RobotController
{
   public abstract StateEstimator getStateEstimator();

   public abstract void initializeEstimatorToActual(Point3d initialCoMPosition, Quat4d initialEstimationLinkOrientation);
}
