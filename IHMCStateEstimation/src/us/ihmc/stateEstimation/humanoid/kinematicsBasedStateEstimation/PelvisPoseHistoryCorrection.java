package us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packets.StampedPosePacket;
import us.ihmc.communication.packets.sensing.PelvisPoseErrorPacket;
import us.ihmc.communication.subscribers.PelvisPoseCorrectionCommunicatorInterface;
import us.ihmc.communication.subscribers.TimeStampedPelvisPoseBuffer;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.kinematics.TimeStampedTransform3D;
import us.ihmc.utilities.kinematics.TransformInterpolationCalculator;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFramePose;

/**
 * Here ICP stands for Iterative Closest Point.
 *
 */

public class PelvisPoseHistoryCorrection
{
   private static final boolean USE_ROTATION_CORRECTION = false;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final TimeStampedPelvisPoseBuffer stateEstimatorPelvisPoseBuffer;
   private PelvisPoseCorrectionCommunicatorInterface pelvisPoseCorrectionCommunicator;
   private final SixDoFJoint rootJoint;
   private final ReferenceFrame pelvisReferenceFrame;
   private final YoVariableRegistry registry;
   private static final double DEFAULT_BREAK_FREQUENCY = 0.015;

   private final RigidBodyTransform pelvisPose = new RigidBodyTransform();
   private final RigidBodyTransform errorBetweenCurrentPositionAndCorrected = new RigidBodyTransform();
   private final RigidBodyTransform totalError = new RigidBodyTransform();
   private final RigidBodyTransform translationErrorInPastTransform = new RigidBodyTransform();
   private final RigidBodyTransform rotationErrorInPastTransform = new RigidBodyTransform();
   private final RigidBodyTransform interpolatedTranslationError = new RigidBodyTransform();
   private final RigidBodyTransform interpolatedRotationError = new RigidBodyTransform();

   private final TimeStampedTransform3D seTimeStampedPose = new TimeStampedTransform3D();

   /** expressed in worldFrame */
   private final YoReferencePose translationCorrection;

   /** expressed in nonCorrectedPelvisFrame */
   private final YoReferencePose orientationCorrection;

   /** expressed in translationCorrection */
   private final YoReferencePose nonCorrectedPelvis;

   /** expressed in worldFrame */
   private final YoReferencePose correctedPelvis;

   private final YoReferencePose pelvisStateAtLocalizationTimeTranslationFrame;
   private final YoReferencePose pelvisStateAtLocalizationTimeRotationFrame;
   private final YoReferencePose newLocalizationTranslationFrame;
   private final YoReferencePose newLocalizationRotationFrame;
   private final YoReferencePose totalRotationErrorFrame;
   private final YoReferencePose totalTranslationErrorFrame;
   private final YoReferencePose interpolatedRotationCorrectionFrame;
   private final YoReferencePose interpolatedTranslationCorrectionFrame;
   private final YoReferencePose interpolationRotationStartFrame;
   private final YoReferencePose interpolationTranslationStartFrame;
   private final Vector3d distanceToTravelVector = new Vector3d();
   private final AxisAngle4d angleToTravelAxis4d = new AxisAngle4d();

   private final LongYoVariable seNonProcessedPelvisTimeStamp;

   private final AlphaFilteredYoVariable interpolationTranslationAlphaFilter;
   private final AlphaFilteredYoVariable interpolationRotationAlphaFilter;
   private final DoubleYoVariable confidenceFactor; // target for alpha filter
   private final DoubleYoVariable interpolationTranslationAlphaFilterBreakFrequency;
   private final DoubleYoVariable interpolationRotationAlphaFilterBreakFrequency;
   private final DoubleYoVariable distanceToTravel;
   private final DoubleYoVariable distanceTraveled;
   private final DoubleYoVariable angleToTravel;
   private final DoubleYoVariable angleTraveled;
   private final DoubleYoVariable previousTranslationClippedAlphaValue;
   private final DoubleYoVariable previousRotationClippedAlphaValue;
   private final DoubleYoVariable translationClippedAlphaValue;
   private final DoubleYoVariable rotationClippedAlphaValue;
   private final DoubleYoVariable maxTranslationVelocityClip;
   private final DoubleYoVariable maxRotationVelocityClip;
   private final DoubleYoVariable maxTranslationAlpha;
   private final DoubleYoVariable maxRotationAlpha;

   private final DoubleYoVariable interpolationTranslationAlphaFilterAlphaValue;
   private final DoubleYoVariable interpolationRotationAlphaFilterAlphaValue;

   private final BooleanYoVariable manuallyTriggerLocalizationUpdate;
   private final DoubleYoVariable manualTranslationOffsetX, manualTranslationOffsetY, manualTranslationOffsetZ;
   private final DoubleYoVariable manualRotationOffsetInRadX, manualRotationOffsetInRadY, manualRotationOffsetInRadZ;

   private final double estimatorDT;
   private boolean sendCorrectionUpdate = false;
   private final Quat4d totalRotationError = new Quat4d();
   private final Vector3d totalTranslationError = new Vector3d();

   private final Vector3d localizationTranslationInPast = new Vector3d();
   private final Vector3d seTranslationInPast = new Vector3d();
   private final Quat4d seRotationInPast = new Quat4d();
   private final Quat4d localizationRotationInPast = new Quat4d();

   public PelvisPoseHistoryCorrection(FullInverseDynamicsStructure inverseDynamicsStructure, final double dt, YoVariableRegistry parentRegistry,
         int pelvisBufferSize)
   {
      this(inverseDynamicsStructure.getRootJoint(), dt, parentRegistry, pelvisBufferSize, null);
   }

   public PelvisPoseHistoryCorrection(FullInverseDynamicsStructure inverseDynamicsStructure,
         PelvisPoseCorrectionCommunicatorInterface externalPelvisPoseSubscriber, final double dt, YoVariableRegistry parentRegistry, int pelvisBufferSize)
   {
      this(inverseDynamicsStructure.getRootJoint(), dt, parentRegistry, pelvisBufferSize, externalPelvisPoseSubscriber);
   }

   public PelvisPoseHistoryCorrection(SixDoFJoint sixDofJoint, final double estimatorDT, YoVariableRegistry parentRegistry, int pelvisBufferSize,
         PelvisPoseCorrectionCommunicatorInterface externalPelvisPoseSubscriber)
   {
      this.estimatorDT = estimatorDT;

      this.rootJoint = sixDofJoint;
      this.pelvisReferenceFrame = rootJoint.getFrameAfterJoint();
      this.pelvisPoseCorrectionCommunicator = externalPelvisPoseSubscriber;
      this.registry = new YoVariableRegistry(getClass().getSimpleName());
      parentRegistry.addChild(registry);

      stateEstimatorPelvisPoseBuffer = new TimeStampedPelvisPoseBuffer(pelvisBufferSize);

      pelvisStateAtLocalizationTimeTranslationFrame = new YoReferencePose("pelvisStateAtLocalizationTimeTranslationFrame", worldFrame, registry);
      pelvisStateAtLocalizationTimeRotationFrame = new YoReferencePose("pelvisStateAtLocalizationTimeRotationFrame", worldFrame, registry);

      newLocalizationTranslationFrame = new YoReferencePose("newLocalizationTranslationFrame", worldFrame, registry);
      newLocalizationRotationFrame = new YoReferencePose("newLocalizationRotationFrame", worldFrame, registry);

      interpolationTranslationStartFrame = new YoReferencePose("interpolationTranslationStartFrame", worldFrame, registry);
      interpolationRotationStartFrame = new YoReferencePose("interpolationRotationStartFrame", worldFrame, registry);

      totalTranslationErrorFrame = new YoReferencePose("totalTranslationErrorFrame", worldFrame, registry);
      totalRotationErrorFrame = new YoReferencePose("totalRotationErrorFrame", worldFrame, registry);

      interpolatedTranslationCorrectionFrame = new YoReferencePose("interpolatedTranslationCorrectionFrame", worldFrame, registry);
      interpolatedRotationCorrectionFrame = new YoReferencePose("interpolatedRotationCorrectionFrame", worldFrame, registry);

      translationCorrection = new YoReferencePose("translationCorrection", worldFrame, registry);
      nonCorrectedPelvis = new YoReferencePose("nonCorrectedPelvis", translationCorrection, registry);
      orientationCorrection = new YoReferencePose("orientationCorrection", nonCorrectedPelvis, registry);
      correctedPelvis = new YoReferencePose("correctedPelvis", worldFrame, registry);

      interpolationTranslationAlphaFilterAlphaValue = new DoubleYoVariable("interpolationTranslationAlphaFilterAlphaValue", registry);
      interpolationTranslationAlphaFilterBreakFrequency = new DoubleYoVariable("interpolationTranslationAlphaFilterBreakFrequency", registry);
      interpolationTranslationAlphaFilter = new AlphaFilteredYoVariable("PelvisTranslationErrorCorrectionAlphaFilter", registry,
            interpolationTranslationAlphaFilterAlphaValue);

      interpolationRotationAlphaFilterAlphaValue = new DoubleYoVariable("interpolationRotationAlphaFilterAlphaValue", registry);
      interpolationRotationAlphaFilterBreakFrequency = new DoubleYoVariable("interpolationRotationAlphaFilterBreakFrequency", registry);
      interpolationRotationAlphaFilter = new AlphaFilteredYoVariable("PelvisRotationErrorCorrectionAlphaFilter", registry,
            interpolationRotationAlphaFilterAlphaValue);

      interpolationTranslationAlphaFilterBreakFrequency.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            double alpha = AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(interpolationTranslationAlphaFilterBreakFrequency.getDoubleValue(),
                  estimatorDT);
            interpolationTranslationAlphaFilter.setAlpha(alpha);
         }
      });
      interpolationTranslationAlphaFilterBreakFrequency.set(DEFAULT_BREAK_FREQUENCY);

      interpolationRotationAlphaFilterBreakFrequency.addVariableChangedListener(new VariableChangedListener()
      {

         @Override
         public void variableChanged(YoVariable<?> v)
         {
            double alpha = AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(interpolationRotationAlphaFilterBreakFrequency.getDoubleValue(),
                  estimatorDT);
            interpolationRotationAlphaFilter.setAlpha(alpha);
         }
      });
      interpolationRotationAlphaFilterBreakFrequency.set(DEFAULT_BREAK_FREQUENCY);

      confidenceFactor = new DoubleYoVariable("PelvisErrorCorrectionConfidenceFactor", registry);

      seNonProcessedPelvisTimeStamp = new LongYoVariable("seNonProcessedPelvis_timestamp", registry);

      translationClippedAlphaValue = new DoubleYoVariable("translationClippedAlphaValue", registry);
      rotationClippedAlphaValue = new DoubleYoVariable("rotationClippedAlphaValue", registry);
      distanceTraveled = new DoubleYoVariable("distanceTraveled", registry);
      angleTraveled = new DoubleYoVariable("angleTraveled", registry);
      maxTranslationVelocityClip = new DoubleYoVariable("maxTranslationVelocityClip", registry);
      maxTranslationVelocityClip.set(0.01);
      maxRotationVelocityClip = new DoubleYoVariable("maxRotationVelocityClip", registry);
      maxRotationVelocityClip.set(0.005); // TODO Determine a good default Value
      previousTranslationClippedAlphaValue = new DoubleYoVariable("previousTranslationClippedAlphaValue", registry);
      previousRotationClippedAlphaValue = new DoubleYoVariable("previousRotationClippedAlphaValue", registry);
      maxTranslationAlpha = new DoubleYoVariable("maxTranslationAlpha", registry);
      maxRotationAlpha = new DoubleYoVariable("maxRotationAlpha", registry);
      distanceToTravel = new DoubleYoVariable("distanceToTravel", registry);
      angleToTravel = new DoubleYoVariable("angleToTravel", registry);

      //      distanceError = new DoubleYoVariable("distanceError", registry);

      manuallyTriggerLocalizationUpdate = new BooleanYoVariable("manuallyTriggerLocalizationUpdate", registry);

      manualTranslationOffsetX = new DoubleYoVariable("manualTranslationOffset_X", registry);
      manualTranslationOffsetY = new DoubleYoVariable("manualTranslationOffset_Y", registry);
      manualTranslationOffsetZ = new DoubleYoVariable("manualTranslationOffset_Z", registry);
      manualRotationOffsetInRadX = new DoubleYoVariable("manualRotationOffsetInRad_X", registry);
      manualRotationOffsetInRadY = new DoubleYoVariable("manualRotationOffsetInRad_Y", registry);
      manualRotationOffsetInRadZ = new DoubleYoVariable("manualRotationOffsetInRad_Z", registry);
      //defaultValues for testing with Atlas.
//      manualTranslationOffsetX.set(-0.11404);
//      manualTranslationOffsetY.set(0.00022);
//      manualTranslationOffsetZ.set(0.78931);
//      manualRotationOffsetInRadX.set(-0.00002);
//      manualRotationOffsetInRadY.set(0.00024);
//      manualRotationOffsetInRadZ.set(-0.00052);
   }

   /**
    * Converges the state estimator pelvis pose towards an external position provided by an external Pelvis Pose Subscriber
    * @param l 
    */
   public void doControl(long timestamp)
   {
      if (pelvisPoseCorrectionCommunicator != null)
      {
         pelvisReferenceFrame.update();
         checkForManualTrigger();
         checkForNewPacket();

         interpolationTranslationAlphaFilter.update(confidenceFactor.getDoubleValue());
         interpolationRotationAlphaFilter.update(confidenceFactor.getDoubleValue());

         pelvisReferenceFrame.getTransformToParent(pelvisPose);
         addPelvisePoseToPelvisBuffer(pelvisPose, timestamp);

         nonCorrectedPelvis.setAndUpdate(pelvisPose);
         correctPelvisPose(pelvisPose);
         correctedPelvis.setAndUpdate(pelvisPose);

         rootJoint.setPositionAndRotation(pelvisPose);
         pelvisReferenceFrame.update();
         checkForNeedToSendCorrectionUpdate();
      }
   }

   private void checkForNeedToSendCorrectionUpdate()
   {
      if (sendCorrectionUpdate)
      {
         sendCorrectionUpdatePacket();
         sendCorrectionUpdate = false;
      }
   }

   /**
    * triggers manual localization offset
    */
   private void checkForManualTrigger()
   {
      if (manuallyTriggerLocalizationUpdate.getBooleanValue())
      {
         manuallyTriggerLocalizationUpdate();
         sendCorrectionUpdate = true;
      }
   }

   /**
    * poll for new packet, input for unit tests and real robot
    */
   private void checkForNewPacket()
   {
      if (pelvisPoseCorrectionCommunicator.hasNewPose())
      {
         processNewPacket();
         sendCorrectionUpdate = true;
      }
   }

   /**
    * Updates max velocity clipping, interpolates from where we were 
    * at the last correction tick to the goal and updates the pelvis
    * @param pelvisPose - non corrected pelvis position
    */
   private void correctPelvisPose(RigidBodyTransform pelvisPose)
   {
      updateTranslationalMaxVelocityClip();
      updateRotationalMaxVelocityClip();

      interpolatedRotationCorrectionFrame.interpolate(interpolationRotationStartFrame, totalRotationErrorFrame, rotationClippedAlphaValue.getDoubleValue());

      interpolatedTranslationCorrectionFrame.interpolate(interpolationTranslationStartFrame, totalTranslationErrorFrame,
            translationClippedAlphaValue.getDoubleValue());

      if (USE_ROTATION_CORRECTION)
         interpolatedRotationCorrectionFrame.getTransformToParent(interpolatedRotationError);
      else
         interpolatedRotationError.setIdentity();

      orientationCorrection.setAndUpdate(interpolatedRotationError);

      interpolatedTranslationCorrectionFrame.getTransformToParent(interpolatedTranslationError);
      translationCorrection.setAndUpdate(interpolatedTranslationError);

      orientationCorrection.getTransformToDesiredFrame(pelvisPose, worldFrame);
   }

   /**
    * clips max translational velocity 
    */
   private void updateTranslationalMaxVelocityClip()
   {
      interpolatedTranslationCorrectionFrame.getTransformToDesiredFrame(errorBetweenCurrentPositionAndCorrected, worldFrame);
      errorBetweenCurrentPositionAndCorrected.getTranslation(distanceToTravelVector);
      distanceToTravel.set(distanceToTravelVector.length());
      maxTranslationAlpha.set((estimatorDT * maxTranslationVelocityClip.getDoubleValue() / distanceToTravel.getDoubleValue())
            + previousTranslationClippedAlphaValue.getDoubleValue());
      translationClippedAlphaValue.set(MathTools.clipToMinMax(interpolationTranslationAlphaFilter.getDoubleValue(), 0.0, maxTranslationAlpha.getDoubleValue()));
      previousTranslationClippedAlphaValue.set(translationClippedAlphaValue.getDoubleValue());
   }

   /**
    * clips max rotational velocity 
    */
   private void updateRotationalMaxVelocityClip()
   {
      interpolatedRotationCorrectionFrame.getTransformToDesiredFrame(errorBetweenCurrentPositionAndCorrected, worldFrame);
      errorBetweenCurrentPositionAndCorrected.getRotation(angleToTravelAxis4d);
      angleToTravel.set(angleToTravelAxis4d.getAngle());
      maxRotationAlpha.set((estimatorDT * maxRotationVelocityClip.getDoubleValue() / angleToTravel.getDoubleValue())
            + previousRotationClippedAlphaValue.getDoubleValue());
      rotationClippedAlphaValue.set(MathTools.clipToMinMax(interpolationRotationAlphaFilter.getDoubleValue(), 0.0, maxRotationAlpha.getDoubleValue()));
      previousRotationClippedAlphaValue.set(rotationClippedAlphaValue.getDoubleValue());
   }

   /**
    * adds noncorrected pelvis poses to buffer for pelvis pose lookups in past
    * @param pelvisPose non-corrected pelvis pose
    * @param timeStamp robot timestamp of pelvis pose
    */
   private void addPelvisePoseToPelvisBuffer(RigidBodyTransform pelvisPose, long timeStamp)
   {
      seNonProcessedPelvisTimeStamp.set(timeStamp);
      stateEstimatorPelvisPoseBuffer.put(pelvisPose, timeStamp);
   }

   /**
    * pulls the corrected pose from the buffer, check that the nonprocessed buffer has
    * corresponding pelvis poses and calculates the total error
    */
   private void processNewPacket()
   {
      StampedPosePacket newPacket = pelvisPoseCorrectionCommunicator.getNewExternalPose();
      TimeStampedTransform3D timeStampedExternalPose = newPacket.getTransform();

      if (stateEstimatorPelvisPoseBuffer.isInRange(timeStampedExternalPose.getTimeStamp()))
      {
         double confidence = newPacket.getConfidenceFactor();
         confidence = MathTools.clipToMinMax(confidence, 0.0, 1.0);
         confidenceFactor.set(confidence);
         addNewExternalPose(timeStampedExternalPose);
      }
   }

   /**
    * sets initials for correction and calculates error in past
    */
   private void addNewExternalPose(TimeStampedTransform3D newPelvisPoseWithTime)
   {
      previousTranslationClippedAlphaValue.set(0.0);
      interpolationTranslationAlphaFilter.set(0.0);
      distanceTraveled.set(0.0);

      previousRotationClippedAlphaValue.set(0.0);
      interpolationRotationAlphaFilter.set(0.0);
      angleTraveled.set(0.0);

      calculateAndStoreErrorInPast(newPelvisPoseWithTime);
      interpolationRotationStartFrame.setAndUpdate(interpolatedRotationCorrectionFrame.getTransformToParent());
      interpolationTranslationStartFrame.setAndUpdate(interpolatedTranslationCorrectionFrame.getTransformToParent());
   }

   /**
    * Calculates the difference between the external at t with the state estimated pelvis pose at t and stores it
    * @param localizationPose - the corrected pelvis pose
    */
   public void calculateAndStoreErrorInPast(TimeStampedTransform3D timestampedlocalizationPose)
   {
      long timeStamp = timestampedlocalizationPose.getTimeStamp();
      RigidBodyTransform localizationPose = timestampedlocalizationPose.getTransform3D();

      localizationPose.getTranslation(localizationTranslationInPast);
      newLocalizationTranslationFrame.setAndUpdate(localizationTranslationInPast);

      localizationPose.getRotation(localizationRotationInPast);
      newLocalizationRotationFrame.setAndUpdate(localizationRotationInPast);

      stateEstimatorPelvisPoseBuffer.findPose(timeStamp, seTimeStampedPose);
      RigidBodyTransform sePose = seTimeStampedPose.getTransform3D();

      sePose.getTranslation(seTranslationInPast);
      pelvisStateAtLocalizationTimeTranslationFrame.setAndUpdate(seTranslationInPast);

      sePose.getRotation(seRotationInPast);
      pelvisStateAtLocalizationTimeRotationFrame.setAndUpdate(seRotationInPast);

      newLocalizationTranslationFrame.getTransformToDesiredFrame(translationErrorInPastTransform, pelvisStateAtLocalizationTimeTranslationFrame);
      newLocalizationRotationFrame.getTransformToDesiredFrame(rotationErrorInPastTransform, pelvisStateAtLocalizationTimeRotationFrame);

      totalTranslationErrorFrame.setAndUpdate(translationErrorInPastTransform);
      totalRotationErrorFrame.setAndUpdate(rotationErrorInPastTransform);
   }

   public void manuallyTriggerLocalizationUpdate()
   {
      confidenceFactor.set(1.0);

      RigidBodyTransform pelvisPose = new RigidBodyTransform();

      Quat4d rotation = new Quat4d();
      pelvisPose.getRotation(rotation);
      RotationFunctions.setQuaternionBasedOnYawPitchRoll(rotation, manualRotationOffsetInRadZ.getDoubleValue(), manualRotationOffsetInRadY.getDoubleValue(),
            manualRotationOffsetInRadX.getDoubleValue());
      pelvisPose.setRotation(rotation);

      Vector3d translation = new Vector3d();
      pelvisPose.get(translation);
      translation.setX(manualTranslationOffsetX.getDoubleValue());
      translation.setY(manualTranslationOffsetY.getDoubleValue());
      translation.setZ(manualTranslationOffsetZ.getDoubleValue());
      pelvisPose.setTranslation(translation);

      TimeStampedTransform3D manualTimeStampedTransform3D = new TimeStampedTransform3D(pelvisPose, stateEstimatorPelvisPoseBuffer.getNewestTimestamp());
      addNewExternalPose(manualTimeStampedTransform3D);
      manuallyTriggerLocalizationUpdate.set(false);
   }

   //TODO Check how to integrate the rotationCorrection here
   private void sendCorrectionUpdatePacket()
   {
      totalRotationErrorFrame.get(totalRotationError);
      totalTranslationErrorFrame.get(totalTranslationError);
      totalError.set(totalRotationError, totalTranslationError);

      double maxCorrectionVelocity = maxTranslationVelocityClip.getDoubleValue();
      PelvisPoseErrorPacket pelvisPoseErrorPacket = new PelvisPoseErrorPacket(totalError, errorBetweenCurrentPositionAndCorrected, maxCorrectionVelocity);
      pelvisPoseCorrectionCommunicator.sendPelvisPoseErrorPacket(pelvisPoseErrorPacket);
   }

   public void setExternelPelvisCorrectorSubscriber(PelvisPoseCorrectionCommunicatorInterface externalPelvisPoseSubscriber)
   {
      this.pelvisPoseCorrectionCommunicator = externalPelvisPoseSubscriber;
   }

   // TODO Extract that guy.
   private class YoReferencePose extends ReferenceFrame
   {
      private static final long serialVersionUID = -7908261385108357220L;

      private final YoFramePose yoFramePose;

      //Below are used for interpolation only
      private final TransformInterpolationCalculator transformInterpolationCalculator = new TransformInterpolationCalculator();
      private final RigidBodyTransform interpolationStartingPosition = new RigidBodyTransform();
      private final RigidBodyTransform interpolationGoalPosition = new RigidBodyTransform();
      private final RigidBodyTransform output = new RigidBodyTransform();

      //Below are used for updating YoFramePose only
      private final Quat4d rotation = new Quat4d();
      private final Vector3d translation = new Vector3d();
      private final double[] yawPitchRoll = new double[3];

      public YoReferencePose(String frameName, ReferenceFrame parentFrame, YoVariableRegistry registry)
      {
         super(frameName, parentFrame);
         yoFramePose = new YoFramePose(frameName + "_", this, registry);
      }

      @Override
      protected void updateTransformToParent(RigidBodyTransform transformToParent)
      {
         yoFramePose.getOrientation().getQuaternion(rotation);
         transformToParent.setRotation(rotation);
         YoFramePoint yoFramePoint = yoFramePose.getPosition();
         transformToParent.setTranslation(yoFramePoint.getX(), yoFramePoint.getY(), yoFramePoint.getZ());
      }

      public void setAndUpdate(RigidBodyTransform transform)
      {
         transform.get(rotation, translation);
         setAndUpdate(rotation, translation);
      }

      public void setAndUpdate(Vector3d newTranslation)
      {
         set(newTranslation);
         update();
      }

      public void setAndUpdate(Quat4d newRotation)
      {
         set(newRotation);
         update();
      }

      public void setAndUpdate(Quat4d newRotation, Vector3d newTranslation)
      {
         set(newRotation);
         set(newTranslation);
         update();
      }

      private void set(Quat4d newRotation)
      {
         RotationFunctions.setYawPitchRollBasedOnQuaternion(yawPitchRoll, newRotation);
         yoFramePose.setYawPitchRoll(yawPitchRoll);
      }

      private void set(Vector3d newTranslation)
      {
         yoFramePose.setXYZ(newTranslation.getX(), newTranslation.getY(), newTranslation.getZ());
      }

      public void interpolate(YoReferencePose start, YoReferencePose goal, double alpha)
      {
         start.getTransformToDesiredFrame(interpolationStartingPosition, parentFrame);
         goal.getTransformToDesiredFrame(interpolationGoalPosition, parentFrame);

         transformInterpolationCalculator.computeInterpolation(interpolationStartingPosition, interpolationGoalPosition, output, alpha);
         setAndUpdate(output);
      }

      public void get(Quat4d rotation)
      {
         yoFramePose.getOrientation().getQuaternion(rotation);
      }

      public void get(Vector3d translation)
      {
         yoFramePose.getPosition().get(translation);
      }
   }
}
