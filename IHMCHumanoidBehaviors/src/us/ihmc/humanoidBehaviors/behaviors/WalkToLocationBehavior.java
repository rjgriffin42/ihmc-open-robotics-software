package us.ihmc.humanoidBehaviors.behaviors;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.humanoidBehaviors.behaviors.primitives.FootstepListBehavior;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.pathGeneration.footstepGenerator.TurnStraightTurnFootstepGenerator;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.footsepGenerator.SimplePathParameters;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.humanoidRobotics.model.FullRobotModel;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.robotics.geometry.FrameOrientation2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FramePose2d;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.referenceFrames.Pose2dReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameOrientation;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFramePose;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;

public class WalkToLocationBehavior extends BehaviorInterface
{
   public enum WalkingOrientation
   {
      ALIGNED_WITH_PATH, START_ORIENTATION, TARGET_ORIENTATION, START_TARGET_ORIENTATION_MEAN, CUSTOM
   }

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final boolean DEBUG = false;
   private final FullRobotModel fullRobotModel;
   private final HumanoidReferenceFrames referenceFrames;

   private double swingTime;
   private double transferTime;

   private final FramePose robotPose = new FramePose();
   private final Point3d robotLocation = new Point3d();
   private final Quat4d robotOrientation = new Quat4d();

   private final YoFramePose robotYoPose = new YoFramePose("robotYoPose", worldFrame, registry);

   private final BooleanYoVariable hasTargetBeenProvided = new BooleanYoVariable("hasTargetBeenProvided", registry);
   private final BooleanYoVariable haveFootstepsBeenGenerated = new BooleanYoVariable("haveFootstepsBeenGenerated", registry);

   private final YoFramePoint targetLocation = new YoFramePoint(getName() + "TargetLocation", worldFrame, registry);
   private final YoFrameOrientation targetOrientation = new YoFrameOrientation(getName() + "TargetOrientation", worldFrame, registry);
   private final YoFrameVector walkPathVector = new YoFrameVector(getName(), worldFrame, registry);
   private final DoubleYoVariable walkDistance = new DoubleYoVariable(getName() + "WalkDistance", registry);

   private SimplePathParameters pathType;// = new SimplePathParameters(0.4, 0.30, 0.0, Math.toRadians(10.0), Math.toRadians(5.0), 0.4);

   private ArrayList<Footstep> footsteps = new ArrayList<Footstep>();
   private FootstepListBehavior footstepListBehavior;

   private final SideDependentList<RigidBody> feet = new SideDependentList<RigidBody>();
   private final SideDependentList<ReferenceFrame> soleFrames = new SideDependentList<ReferenceFrame>();

   private double minDistanceThresholdForWalking, minYawThresholdForWalking;

   public WalkToLocationBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, FullRobotModel fullRobotModel,
         HumanoidReferenceFrames referenceFrames, WalkingControllerParameters walkingControllerParameters)
   {
      super(outgoingCommunicationBridge);

      this.fullRobotModel = fullRobotModel;
      this.referenceFrames = referenceFrames;

      this.swingTime = walkingControllerParameters.getDefaultSwingTime();
      this.transferTime = walkingControllerParameters.getDefaultTransferTime();

      this.pathType = new SimplePathParameters(walkingControllerParameters.getMaxStepLength(), walkingControllerParameters.getInPlaceWidth(), 0.0,
            Math.toRadians(20.0), Math.toRadians(10.0), 0.4); // 10 5 0.4
      footstepListBehavior = new FootstepListBehavior(outgoingCommunicationBridge, walkingControllerParameters);

      for (RobotSide robotSide : RobotSide.values)
      {
         feet.put(robotSide, fullRobotModel.getFoot(robotSide));
         soleFrames.put(robotSide, fullRobotModel.getSoleFrame(robotSide));
      }
   }

   public void setTarget(FramePose2d targetPose2dInWorld)
   {
      setTarget(targetPose2dInWorld, WalkingOrientation.CUSTOM);
   }

   public void setTarget(FramePose2d targetPose2dInWorld, WalkingOrientation walkingOrientation)
   {
      targetPose2dInWorld.checkReferenceFrameMatch(worldFrame);
      this.targetLocation.set(targetPose2dInWorld.getX(), targetPose2dInWorld.getY(), 0.0);
      this.targetOrientation.setYawPitchRoll(targetPose2dInWorld.getYaw(), 0.0, 0.0);

      ReferenceFrame initialRobotFrame = referenceFrames.getPelvisZUpFrame();
      ReferenceFrame targetRobotFrame = new Pose2dReferenceFrame("targetFrame", targetPose2dInWorld);

      double initialRobotOrientationRelativeToWalkingPath = computeFrameOrientationRelativeToWalkingPath(initialRobotFrame);
      double targetRobotOrientationRelativeToWalkingPath = computeFrameOrientationRelativeToWalkingPath(targetRobotFrame);

      switch (walkingOrientation)
      {
      case ALIGNED_WITH_PATH:
         setWalkingOrientationRelativeToPathDirection(0.0);
         break;

      case START_ORIENTATION:
         setWalkingOrientationRelativeToPathDirection(initialRobotOrientationRelativeToWalkingPath);
         break;

      case TARGET_ORIENTATION:
         setWalkingOrientationRelativeToPathDirection(targetRobotOrientationRelativeToWalkingPath);
         break;

      case START_TARGET_ORIENTATION_MEAN:
         setWalkingOrientationRelativeToPathDirection(0.5 * (initialRobotOrientationRelativeToWalkingPath + targetRobotOrientationRelativeToWalkingPath));
         break;

      default:
         break;
      }

      hasTargetBeenProvided.set(true);
      generateFootsteps();
   }

   private double computeFrameOrientationRelativeToWalkingPath(ReferenceFrame referenceFrame)
   {
      this.walkPathVector.sub(this.targetLocation, robotYoPose.getPosition());
      fullRobotModel.updateFrames();

      FrameVector2d frameHeadingVector = new FrameVector2d(referenceFrame, 1.0, 0.0);
      frameHeadingVector.changeFrame(worldFrame);
      double ret = -frameHeadingVector.angle(walkPathVector.getFrameVector2dCopy());

      if (DEBUG)
      {
         PrintTools.debug(this, "FrameHeadingVector : " + frameHeadingVector);
         PrintTools.debug(this, "WalkPathVector : " + walkPathVector);
         PrintTools.debug(this, "OrientationToWalkPath : " + ret);
      }

      return ret;
   }

   public void setSwingTime(double swingTime)
   {
      this.swingTime = swingTime;
   }

   public void setTransferTime(double transferTime)
   {
      this.transferTime = transferTime;
   }

   public void setWalkingOrientationRelativeToPathDirection(double orientationRelativeToPathDirection)
   {
      pathType.setAngle(orientationRelativeToPathDirection);
      if (hasTargetBeenProvided.getBooleanValue())
         generateFootsteps();
   }

   @Override
   public void initialize()
   {
      hasTargetBeenProvided.set(false);
      haveFootstepsBeenGenerated.set(false);
      footstepListBehavior.initialize();

      robotPose.setToZero(fullRobotModel.getRootJoint().getFrameAfterJoint());
      robotPose.changeFrame(worldFrame);

      robotYoPose.set(robotPose);

      robotPose.getPosition(robotLocation);
      robotPose.getOrientation(robotOrientation);

      this.targetLocation.set(robotLocation);
      this.targetOrientation.set(robotOrientation);
   }

   public int getNumberOfFootSteps()
   {
      return footsteps.size();
   }

   public ArrayList<Footstep> getFootSteps()
   {
      return footsteps;
   }

   private void generateFootsteps()
   {
      FramePoint midFeetPosition = getCurrentMidFeetPosition();

      footsteps.clear();
      FramePose2d endPose = new FramePose2d(worldFrame);
      endPose.setPosition(new FramePoint2d(worldFrame, targetLocation.getX(), targetLocation.getY()));
      endPose.setOrientation(new FrameOrientation2d(worldFrame, targetOrientation.getYaw().getDoubleValue()));

      boolean computeFootstepsWithFlippedInitialTurnDirection = pathType.getAngle() != 0.0;

      TurnStraightTurnFootstepGenerator footstepGenerator = new TurnStraightTurnFootstepGenerator(feet, soleFrames, endPose, pathType);
      footstepGenerator.initialize();

      walkDistance.set(footstepGenerator.getDistance());

      if (footstepGenerator.getDistance() > minDistanceThresholdForWalking
            || Math.abs(footstepGenerator.getSignedInitialTurnDirection()) > minYawThresholdForWalking)
      {
         List<Footstep> footstepsNominalOrientation = footstepGenerator.generateDesiredFootstepList();

         if (computeFootstepsWithFlippedInitialTurnDirection)
         {
            pathType.setAngle(-pathType.getAngle());
            TurnStraightTurnFootstepGenerator footstepGeneratorFlippedInitialTurnDirection = new TurnStraightTurnFootstepGenerator(feet, soleFrames, endPose,
                  pathType); //FIXME: should be able to re-use other footStepGenerator, but doesn't work so far..
            footstepGeneratorFlippedInitialTurnDirection.initialize();
            List<Footstep> footstepsFlippedOrientation = footstepGeneratorFlippedInitialTurnDirection.generateDesiredFootstepList();
            pathType.setAngle(-pathType.getAngle());

            if (footstepsFlippedOrientation.size() < footstepsNominalOrientation.size())
            {
               footsteps.addAll(footstepsFlippedOrientation);
            }
            else
            {
               footsteps.addAll(footstepsNominalOrientation);
            }
         }
         else
         {
            footsteps.addAll(footstepsNominalOrientation);
         }

         for (Footstep footstep : footsteps)
         {
            footstep.setZ(midFeetPosition.getZ());
         }
      }

      footstepListBehavior.set(footsteps, swingTime, transferTime);
      haveFootstepsBeenGenerated.set(true);

      if (DEBUG)
         PrintTools.debug(this, "Walk Distance: " + walkDistance.getDoubleValue());
   }

   @Override
   public void doControl()
   {
      if (!hasTargetBeenProvided.getBooleanValue())
         return;
      if (!haveFootstepsBeenGenerated.getBooleanValue())
         generateFootsteps();
      footstepListBehavior.doControl();
   }

   private FramePoint getCurrentMidFeetPosition()
   {
      FramePoint ret = new FramePoint();
      ret.setToZero(referenceFrames.getMidFeetZUpFrame());
      ret.changeFrame(worldFrame);

      return ret;
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
      if (footstepListBehavior != null)
         footstepListBehavior.consumeObjectFromNetworkProcessor(object);
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
      if (footstepListBehavior != null)
         footstepListBehavior.consumeObjectFromController(object);
   }

   @Override
   public void stop()
   {
      footstepListBehavior.stop();
      isStopped.set(true);
   }

   @Override
   public void enableActions()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void pause()
   {
      footstepListBehavior.pause();
      isPaused.set(true);
   }

   @Override
   public void resume()
   {
      footstepListBehavior.resume();
      isPaused.set(false);

   }

   @Override
   public boolean isDone()
   {
      if (!haveFootstepsBeenGenerated.getBooleanValue() || !hasTargetBeenProvided.getBooleanValue())
         return false;
      if (haveFootstepsBeenGenerated.getBooleanValue() && footsteps.size() == 0)
         return true;
      return footstepListBehavior.isDone();
   }

   @Override
   public void doPostBehaviorCleanup()
   {
      isPaused.set(false);
      isStopped.set(false);
      hasTargetBeenProvided.set(false);
      haveFootstepsBeenGenerated.set(false);
      footstepListBehavior.doPostBehaviorCleanup();
   }

   public boolean hasInputBeenSet()
   {
      if (haveFootstepsBeenGenerated.getBooleanValue())
         return true;
      else
         return false;
   }

   public void setFootstepLength(double footstepLength)
   {
      pathType.setStepLength(footstepLength);
   }

   public void setDistanceThreshold(double minDistanceThresholdForWalking)
   {
      this.minDistanceThresholdForWalking = minDistanceThresholdForWalking;
   }

   public void setYawAngleThreshold(double minYawThresholdForWalking)
   {
      this.minYawThresholdForWalking = minYawThresholdForWalking;
   }

}
