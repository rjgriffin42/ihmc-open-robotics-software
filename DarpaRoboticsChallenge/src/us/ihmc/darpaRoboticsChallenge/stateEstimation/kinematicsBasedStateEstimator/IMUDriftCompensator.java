package us.ihmc.darpaRoboticsChallenge.stateEstimation.kinematicsBasedStateEstimator;

import javax.vecmath.AxisAngle4d;

import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoFrameVector;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameQuaternion;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

/**
 * IMUDriftCompensator when activated estimates the IMU drift on the yaw angle and correct the root joint orientation and angular velocity around the vertical axis.
 * @author Sylvain
 *
 */
public class IMUDriftCompensator
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final BooleanYoVariable isIMUDriftCompensationActivated = new BooleanYoVariable("isIMUDriftCompensationActivated", registry);
   private final BooleanYoVariable isIMUDriftYawRateEstimationActivated = new BooleanYoVariable("isIMUDriftYawRateEstimationActivated", registry);
   private final BooleanYoVariable isIMUDriftYawRateEstimated = new BooleanYoVariable("isIMUDriftYawRateEstimated", registry);
   
   private final DoubleYoVariable alphaFilterIMUDrift = new DoubleYoVariable("alphaFilterIMUDrift", registry);
   private final DoubleYoVariable imuDriftYawRate = new DoubleYoVariable("estimatedIMUDriftYawRate", registry);
   private final AlphaFilteredYoVariable imuDriftYawRateFiltered = new AlphaFilteredYoVariable("estimatedIMUDriftYawRateFiltered", registry, alphaFilterIMUDrift, imuDriftYawRate);
   private final DoubleYoVariable imuDriftYawAngle = new DoubleYoVariable("estimatedIMUDriftYawAngle", registry);
   
   private final DoubleYoVariable rootJointYawAngleCorrected = new DoubleYoVariable("rootJointYawAngleWithDriftCompensation", registry);
   private final DoubleYoVariable rootJointYawRateCorrected = new DoubleYoVariable("rootJointYawRateWithDriftCompensation", registry);
   
   private final SideDependentList<YoFrameQuaternion> footOrientationsInWorld = new SideDependentList<YoFrameQuaternion>();
   private final SideDependentList<YoFrameVector> footAxisAnglesInWorld = new SideDependentList<YoFrameVector>();
   private final DoubleYoVariable alphaFilterFootAngularVelocity = new DoubleYoVariable("alphaFilterFootAngularVelocity", registry);
   private final SideDependentList<YoFrameVector> footAngularVelocitiesInWorld = new SideDependentList<YoFrameVector>();
   private final SideDependentList<AlphaFilteredYoVariable> footAngularVelocitiesInWorldFilteredX = new SideDependentList<AlphaFilteredYoVariable>();
   private final SideDependentList<AlphaFilteredYoVariable> footAngularVelocitiesInWorldFilteredY = new SideDependentList<AlphaFilteredYoVariable>();
   private final SideDependentList<AlphaFilteredYoVariable> footAngularVelocitiesInWorldFilteredZ = new SideDependentList<AlphaFilteredYoVariable>();
   private final YoFrameVector footAngularVelocityDifference = new YoFrameVector("footAngularVelocityDifference", worldFrame, registry);
   private final YoFrameVector footAngularVelocityAverage = new YoFrameVector("footAngularVelocityAverage", worldFrame, registry);
   private final DoubleYoVariable alphaFilterFootAngularVelocityAverage = new DoubleYoVariable("alphaFilterFootAngularVelocityAverage", registry);
   private final AlphaFilteredYoFrameVector footAngularVelocityAverageFiltered = AlphaFilteredYoFrameVector.createAlphaFilteredYoFrameVector("footAngularVelocityAverageFiltered", "", registry, alphaFilterFootAngularVelocityAverage, footAngularVelocityAverage);
   private final YoFrameVector footAngularVelocityDifferenceThresholdToEstimateIMUDrift = new YoFrameVector("footAngularVelocityDifferenceThresholdToEstimateIMUDrift", worldFrame, registry);

   private final SideDependentList<ReferenceFrame> footFrames;

   private final double estimatorDT;

   private final TwistCalculator twistCalculator;
   
   private final SixDoFJoint rootJoint;
   private final ReferenceFrame rootJointFrame;

   private final SideDependentList<WrenchBasedFootSwitch> footSwitches;
   private final DoubleYoVariable totalLoadPercentageOnFeet = new DoubleYoVariable("totalLoadPercentageOnFeet", registry);
   private final DoubleYoVariable loadPercentageOnFeetThresholdForIMUDrift = new DoubleYoVariable("loadPercentageOnFeetThresholdForIMUDrift", registry);
   
   public IMUDriftCompensator(SideDependentList<ReferenceFrame> footFrames, FullInverseDynamicsStructure inverseDynamicsStructure,
         SideDependentList<WrenchBasedFootSwitch> footSwitches, double estimatorDT, YoVariableRegistry parentRegistry)
   {
      this.rootJoint = inverseDynamicsStructure.getRootJoint();
      this.footFrames = footFrames;
      this.rootJointFrame = rootJoint.getFrameAfterJoint();
      this.estimatorDT = estimatorDT;
      this.twistCalculator = inverseDynamicsStructure.getTwistCalculator();
      this.footSwitches = footSwitches;
      loadPercentageOnFeetThresholdForIMUDrift.set(0.5);
      
      for (RobotSide robotSide : RobotSide.values)
      {
         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
         
         YoFrameQuaternion footOrientationInWorld = new YoFrameQuaternion(sidePrefix + "FootOrientationInWorld", worldFrame, registry);
         footOrientationsInWorld.put(robotSide, footOrientationInWorld);
         
         YoFrameVector footAxisAngleInWorld = new YoFrameVector(sidePrefix + "FootAxisAngleInWorld", worldFrame, registry);
         footAxisAnglesInWorld.put(robotSide, footAxisAngleInWorld);

         YoFrameVector footAngularVelocityInWorld = new YoFrameVector(sidePrefix + "FootAngularVelocitiesInWorld", worldFrame, registry);
         footAngularVelocitiesInWorld.put(robotSide, footAngularVelocityInWorld);
         
         AlphaFilteredYoVariable footAngularVelocityInWorldX = new AlphaFilteredYoVariable(sidePrefix + "FootAngularVelocityInWorldFilteredX", registry, alphaFilterFootAngularVelocity);
         footAngularVelocitiesInWorldFilteredX.put(robotSide, footAngularVelocityInWorldX);

         AlphaFilteredYoVariable footAngularVelocityInWorldY = new AlphaFilteredYoVariable(sidePrefix + "FootAngularVelocityInWorldFilteredY", registry, alphaFilterFootAngularVelocity);
         footAngularVelocitiesInWorldFilteredY.put(robotSide, footAngularVelocityInWorldY);

         AlphaFilteredYoVariable footAngularVelocityInWorldZ = new AlphaFilteredYoVariable(sidePrefix + "FootAngularVelocityInWorldFilteredZ", registry, alphaFilterFootAngularVelocity);
         footAngularVelocitiesInWorldFilteredZ.put(robotSide, footAngularVelocityInWorldZ);
      }
      
      isIMUDriftYawRateEstimated.set(false);
      imuDriftYawRate.set(0.0);
      imuDriftYawRateFiltered.reset();
      imuDriftYawRateFiltered.update();
      imuDriftYawAngle.set(0.0);
      rootJointYawAngleCorrected.set(0.0);
      
      parentRegistry.addChild(registry);
   }
   
   public void activateEstimation(boolean activate)
   {
      isIMUDriftYawRateEstimationActivated.set(activate);
   }
   
   public void activateCompensation(boolean activate)
   {
      isIMUDriftCompensationActivated.set(activate);
   }
   
   public void setAlphaIMUDrift(double alphaFilter)
   {
      alphaFilterIMUDrift.set(alphaFilter);
   }
   
   public void setAlphaFootAngularVelocity(double alphaFilter)
   {
      alphaFilterFootAngularVelocity.set(alphaFilter);
      alphaFilterFootAngularVelocityAverage.set(alphaFilter);
   }
   
   public void setFootAngularVelocityThreshold(double threshold)
   {
      footAngularVelocityDifferenceThresholdToEstimateIMUDrift.set(threshold, threshold, threshold);
   }

   public void initialize()
   {
      imuDriftYawRate.set(0.0);
      imuDriftYawRateFiltered.reset();
      resetFootAngularVelocitiesFiltered();
      updateFootOrientations();
      resetFootAngularVelocitiesFiltered();
      updateFootOrientations();
      
      if (isIMUDriftYawRateEstimationActivated.getBooleanValue())
      {
         isIMUDriftYawRateEstimated.set(true);
         estimateIMUDriftYaw();
      }
      else
      {
         isIMUDriftYawRateEstimated.set(false);
      }
      
      if (isIMUDriftCompensationActivated.getBooleanValue())
         compensateIMUDriftYaw();
   }

   public void updateAndCompensateDrift()
   {
      if (!isIMUDriftYawRateEstimationActivated.getBooleanValue())
      {
         resetFootAngularVelocitiesFiltered();
         updateFootOrientations();
         resetFootAngularVelocitiesFiltered();
         isIMUDriftYawRateEstimated.set(false);
      }
      
      updateFootOrientations();

      if (isIMUDriftCompensationActivated.getBooleanValue())
         compensateIMUDriftYaw();
   }

   public void esimtateDriftIfPossible(boolean isInDoubleSupport)
   {
      if (!isInDoubleSupport)
      {
         isIMUDriftYawRateEstimated.set(false);
         return;
      }

      double totalLoadPercentage = 0.0;
      for (RobotSide robotSide : RobotSide.values)
         totalLoadPercentage += footSwitches.get(robotSide).computeFootLoadPercentage();
      totalLoadPercentageOnFeet.set(totalLoadPercentage);
      boolean areFeetLoadedEnough = totalLoadPercentageOnFeet.getDoubleValue() > loadPercentageOnFeetThresholdForIMUDrift.getDoubleValue();

      boolean isAngularVelocityXLowEnough = Math.abs(footAngularVelocityDifference.getX()) < footAngularVelocityDifferenceThresholdToEstimateIMUDrift.getX();
      boolean isAngularVelocityYLowEnough = Math.abs(footAngularVelocityDifference.getY()) < footAngularVelocityDifferenceThresholdToEstimateIMUDrift.getY();
      boolean isAngularVelocityZLowEnough = Math.abs(footAngularVelocityDifference.getZ()) < footAngularVelocityDifferenceThresholdToEstimateIMUDrift.getZ();

      if (isIMUDriftYawRateEstimationActivated.getBooleanValue() && areFeetLoadedEnough && isAngularVelocityXLowEnough && isAngularVelocityYLowEnough && isAngularVelocityZLowEnough)
      {
         isIMUDriftYawRateEstimated.set(true);
         estimateIMUDriftYaw();
      }
      else
      {
         isIMUDriftYawRateEstimated.set(false);
      }
   }

   private void estimateIMUDriftYaw()
   {
      imuDriftYawRate.set(footAngularVelocityAverageFiltered.getZ());
      imuDriftYawRateFiltered.update();

      imuDriftYawAngle.add(imuDriftYawRateFiltered.getDoubleValue() * estimatorDT);
      imuDriftYawAngle.set(AngleTools.trimAngleMinusPiToPi(imuDriftYawAngle.getDoubleValue()));

      rootJoint.packRotation(rootJointYawPitchRoll);
      rootJointYawPitchRoll[0] -= imuDriftYawAngle.getDoubleValue();
      rootJointYawPitchRoll[0] = AngleTools.trimAngleMinusPiToPi(rootJointYawPitchRoll[0]);
      rootJointYawAngleCorrected.set(rootJointYawPitchRoll[0]);

      rootJoint.packJointTwist(rootJointTwist);
      rootJointTwist.packAngularPart(rootJointAngularVelocity);
      rootJointAngularVelocity.changeFrame(worldFrame);
      rootJointYawRateCorrected.set(rootJointAngularVelocity.getZ() - imuDriftYawRateFiltered.getDoubleValue());
   }
   
   private final double[] rootJointYawPitchRoll = new double[]{0.0, 0.0, 0.0};
   private final Twist rootJointTwist = new Twist();
   private final FrameVector rootJointAngularVelocity = new FrameVector();
   
   private void compensateIMUDriftYaw()
   {
      rootJoint.packRotation(rootJointYawPitchRoll);
      rootJointYawPitchRoll[0] -= imuDriftYawAngle.getDoubleValue();
      rootJointYawPitchRoll[0] = AngleTools.trimAngleMinusPiToPi(rootJointYawPitchRoll[0]);
      rootJointYawAngleCorrected.set(rootJointYawPitchRoll[0]);
      rootJoint.setRotation(rootJointYawPitchRoll[0], rootJointYawPitchRoll[1], rootJointYawPitchRoll[2]);
      rootJoint.getFrameAfterJoint().update();
      
      rootJoint.packJointTwist(rootJointTwist);
      rootJointTwist.packAngularPart(rootJointAngularVelocity);
      rootJointAngularVelocity.changeFrame(worldFrame);
      rootJointYawRateCorrected.set(rootJointAngularVelocity.getZ() - imuDriftYawRateFiltered.getDoubleValue());
      rootJointAngularVelocity.setZ(rootJointYawRateCorrected.getDoubleValue());
      rootJointAngularVelocity.changeFrame(rootJointFrame);
      rootJointTwist.setAngularPart(rootJointAngularVelocity.getVector());
      rootJoint.setJointTwist(rootJointTwist);
      twistCalculator.compute();
   }
   
   private final SideDependentList<FrameOrientation> footOrientations = new SideDependentList<FrameOrientation>(new FrameOrientation(), new FrameOrientation());
   private final SideDependentList<FrameOrientation> footOrientationsPrevValue = new SideDependentList<FrameOrientation>(new FrameOrientation(), new FrameOrientation());
   private final SideDependentList<FrameVector> footAxisAnglesPrevValue = new SideDependentList<FrameVector>(new FrameVector(), new FrameVector());
   private final AxisAngle4d footAxisAngle = new AxisAngle4d();
   
   private void updateFootOrientations()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         FrameOrientation footOrientation = footOrientations.get(robotSide);
         footOrientationsPrevValue.get(robotSide).set(footOrientation);
         
         footOrientation.setToZero(footFrames.get(robotSide));
         footOrientation.changeFrame(worldFrame);
         
         YoFrameQuaternion footOrientationInWorld = footOrientationsInWorld.get(robotSide);
         footOrientationInWorld.set(footOrientation);
         
         YoFrameVector footAxisAngleInWorld = footAxisAnglesInWorld.get(robotSide);
         footAxisAngleInWorld.getFrameTuple(footAxisAnglesPrevValue.get(robotSide));
         footOrientationInWorld.get(footAxisAngle);
         footAxisAngleInWorld.set(footAxisAngle.getX(), footAxisAngle.getY(), footAxisAngle.getZ());
         footAxisAngleInWorld.scale(footAxisAngle.getAngle());
         

         YoFrameVector footAngularVelocityInWorld = footAngularVelocitiesInWorld.get(robotSide);
         footAngularVelocityInWorld.setX(AngleTools.computeAngleDifferenceMinusPiToPi(footAxisAngleInWorld.getX(), footAxisAnglesPrevValue.get(robotSide).getX()));
         footAngularVelocityInWorld.setY(AngleTools.computeAngleDifferenceMinusPiToPi(footAxisAngleInWorld.getY(), footAxisAnglesPrevValue.get(robotSide).getY()));
         footAngularVelocityInWorld.setZ(AngleTools.computeAngleDifferenceMinusPiToPi(footAxisAngleInWorld.getZ(), footAxisAnglesPrevValue.get(robotSide).getZ()));
         footAngularVelocityInWorld.scale(1.0 / estimatorDT);

         footAngularVelocitiesInWorldFilteredX.get(robotSide).update(footAngularVelocityInWorld.getX());
         footAngularVelocitiesInWorldFilteredY.get(robotSide).update(footAngularVelocityInWorld.getY());
         footAngularVelocitiesInWorldFilteredZ.get(robotSide).update(footAngularVelocityInWorld.getZ());
      }

      footAngularVelocityDifference.setX(Math.abs(footAngularVelocitiesInWorldFilteredX.get(RobotSide.LEFT).getDoubleValue() - footAngularVelocitiesInWorldFilteredX.get(RobotSide.RIGHT).getDoubleValue()));
      footAngularVelocityDifference.setY(Math.abs(footAngularVelocitiesInWorldFilteredY.get(RobotSide.LEFT).getDoubleValue() - footAngularVelocitiesInWorldFilteredY.get(RobotSide.RIGHT).getDoubleValue()));
      footAngularVelocityDifference.setZ(Math.abs(footAngularVelocitiesInWorldFilteredZ.get(RobotSide.LEFT).getDoubleValue() - footAngularVelocitiesInWorldFilteredZ.get(RobotSide.RIGHT).getDoubleValue()));
      
      footAngularVelocityAverage.setX(footAngularVelocitiesInWorldFilteredX.get(RobotSide.LEFT).getDoubleValue() + footAngularVelocitiesInWorldFilteredX.get(RobotSide.RIGHT).getDoubleValue());
      footAngularVelocityAverage.setY(footAngularVelocitiesInWorldFilteredY.get(RobotSide.LEFT).getDoubleValue() + footAngularVelocitiesInWorldFilteredY.get(RobotSide.RIGHT).getDoubleValue());
      footAngularVelocityAverage.setZ(footAngularVelocitiesInWorldFilteredZ.get(RobotSide.LEFT).getDoubleValue() + footAngularVelocitiesInWorldFilteredZ.get(RobotSide.RIGHT).getDoubleValue());
      footAngularVelocityAverage.scale(0.5);
      footAngularVelocityAverageFiltered.update();
   }
   
   private void resetFootAngularVelocitiesFiltered()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         footAngularVelocitiesInWorldFilteredX.get(robotSide).reset();
         footAngularVelocitiesInWorldFilteredY.get(robotSide).reset();
         footAngularVelocitiesInWorldFilteredZ.get(robotSide).reset();
      }
   }
}
