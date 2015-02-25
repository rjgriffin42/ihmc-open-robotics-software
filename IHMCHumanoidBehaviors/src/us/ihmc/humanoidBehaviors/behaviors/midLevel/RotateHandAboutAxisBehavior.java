package us.ihmc.humanoidBehaviors.behaviors.midLevel;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.manipulation.HandPosePacket.Frame;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.behaviors.primitives.HandPoseBehavior;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.taskExecutor.HandPoseTask;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.io.printing.SysoutTool;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.TransformTools;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.taskExecutor.TaskExecutor;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

/**
 * Rotate a 1-DOF body such as a valve or door handle/hinge by a specified angle.
 * Inputs: Grasp Pose and PinJoint Pose
 * @author cschmidt
 *
 */
public class RotateHandAboutAxisBehavior extends BehaviorInterface
{
   private double radiansToRotateBetweenHandPoses = Math.toRadians(18.0);

   private static final boolean DEBUG = false;

   private final ReferenceFrame world = ReferenceFrame.getWorldFrame();

   private final FullRobotModel fullRobotModel;

   private final TaskExecutor taskExecutor;
   private final HandPoseBehavior handPoseBehavior;
//   private final WholeBodyInverseKinematicBehavior wholeBodyInverseKinematicBehavior;

   private final DoubleYoVariable yoTime;
   private final BooleanYoVariable hasInputBeenSet;

   public RotateHandAboutAxisBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, SDFFullRobotModel fullRobotModel,
         ReferenceFrames referenceFrames, WholeBodyControllerParameters wholeBodyControllerParameters, DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridge);
      this.fullRobotModel = fullRobotModel;
      this.taskExecutor = new TaskExecutor();
      handPoseBehavior = new HandPoseBehavior(outgoingCommunicationBridge, yoTime);
//      this.wholeBodyInverseKinematicBehavior = new WholeBodyInverseKinematicBehavior(outgoingCommunicationBridge, wholeBodyControllerParameters, fullRobotModel, yoTime);
      this.yoTime = yoTime;
      this.hasInputBeenSet = new BooleanYoVariable("hasInputBeenSet", registry);
   }

   @Override
   public void doControl()
   {
      if (hasInputBeenSet())
      {
         taskExecutor.doControl();
      }
   }

   public void setInput(RobotSide robotSide, Axis axisOrientationInGraspedObjectFrame, RigidBodyTransform graspedObjectTransformToWorld,
         double totalRotationInRadians, double trajectoryTime)
   {
      FramePose currentHandPoseInWorld = new FramePose();
      currentHandPoseInWorld.setToZero(fullRobotModel.getHandControlFrame(robotSide));
      currentHandPoseInWorld.changeFrame(world);

      setInput(robotSide, currentHandPoseInWorld, axisOrientationInGraspedObjectFrame, graspedObjectTransformToWorld, totalRotationInRadians, trajectoryTime);
   }

   private RigidBodyTransform afterToBeforeRotationTransform = new RigidBodyTransform();
   private PoseReferenceFrame graspedObjectFrame;

   public void setInput(RobotSide robotSide, FramePose currentHandPoseInWorld, Axis axisOrientationInGraspedObjectFrame,
         RigidBodyTransform graspedObjectTransformToWorld, double totalRotationInRadians, double trajectoryTime)
   {
      graspedObjectFrame = new PoseReferenceFrame("graspedObjectFrameBeforeRotation", world);
      graspedObjectFrame.setPoseAndUpdate(graspedObjectTransformToWorld);

      if (DEBUG)
      {
         SysoutTool.println("Current Hand Pose in World: " + currentHandPoseInWorld);
         SysoutTool.println("Grasped Object Transform To World: " + graspedObjectTransformToWorld);
         SysoutTool.println("Grasped Object Reference Frame: " + graspedObjectFrame);
      }

      radiansToRotateBetweenHandPoses = Math.abs(radiansToRotateBetweenHandPoses) * Math.signum(totalRotationInRadians);
      int numberOfDiscreteHandPosesToUse = (int) Math.round(totalRotationInRadians / radiansToRotateBetweenHandPoses);
      TransformTools.rotate(afterToBeforeRotationTransform, radiansToRotateBetweenHandPoses, axisOrientationInGraspedObjectFrame);

      ArrayList<FramePose> handPosesTangentToSemiCircularPath = copyHandPoseAndRotateAboutAxisRecursively(currentHandPoseInWorld, numberOfDiscreteHandPosesToUse);

      for (FramePose desiredHandPose : handPosesTangentToSemiCircularPath)
      {
         Frame packetFrame;
         if (desiredHandPose.getReferenceFrame().isWorldFrame())
         {
            packetFrame = Frame.WORLD;
         }
         else
         {
            throw new RuntimeException("Hand Pose must be defined in World reference frame.");
         }
         taskExecutor.submit(new HandPoseTask(robotSide, trajectoryTime / numberOfDiscreteHandPosesToUse, desiredHandPose, packetFrame, handPoseBehavior, yoTime));
//         taskExecutor.submit(new WholeBodyInverseKinematicTask(robotSide, yoTime, wholeBodyInverseKinematicBehavior, desiredHandPose, trajectoryTime / numberOfDiscreteHandPosesToUse, 0, ControlledDoF.DOF_3P2R, true));
      }

      hasInputBeenSet.set(true);
   }


   private ArrayList<FramePose> copyHandPoseAndRotateAboutAxisRecursively(FramePose handPoseInWorld, int numberOfHandPoses)
   {
      ArrayList<FramePose> desiredHandPoses = new ArrayList<FramePose>(numberOfHandPoses);

      desiredHandPoses.add(copyHandPoseAndRotateAboutAxis(handPoseInWorld));

      for (int i = desiredHandPoses.size(); i < numberOfHandPoses; i++)
      {
         FramePose previousDesiredHandPose = desiredHandPoses.get(i - 1);
         FramePose nextDesiredHandPose = copyHandPoseAndRotateAboutAxis(previousDesiredHandPose);
         desiredHandPoses.add(i, nextDesiredHandPose);
      }
      return desiredHandPoses;
   }

   private Vector3d graspedObjectToHandVector = new Vector3d();

   private FramePose copyHandPoseAndRotateAboutAxis(FramePose handPoseInWorld)
   {
      FramePose ret = new FramePose(handPoseInWorld);
      ret.changeFrame(graspedObjectFrame);
      RigidBodyTransform handToGraspedObjectTransformBeforeRotation = new RigidBodyTransform();
      ret.getPose(handToGraspedObjectTransformBeforeRotation);

      RigidBodyTransform handToGraspedObjectTransformAfterRotation = new RigidBodyTransform(handToGraspedObjectTransformBeforeRotation);
      handToGraspedObjectTransformAfterRotation.multiply(afterToBeforeRotationTransform);

      ret.setPoseIncludingFrame(graspedObjectFrame, handToGraspedObjectTransformAfterRotation);

      handToGraspedObjectTransformBeforeRotation.get(graspedObjectToHandVector);
      afterToBeforeRotationTransform.transform(graspedObjectToHandVector);
      ret.setPosition(graspedObjectToHandVector);

      ret.changeFrame(world);

      return ret;
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
      handPoseBehavior.consumeObjectFromNetworkProcessor(object);
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
      handPoseBehavior.consumeObjectFromController(object);
   }

   @Override
   public void stop()
   {
      handPoseBehavior.stop();
   }

   @Override
   public void enableActions()
   {
      handPoseBehavior.enableActions();
   }

   @Override
   public void pause()
   {
      handPoseBehavior.pause();
      isPaused.set(true);
   }

   @Override
   public void resume()
   {
      handPoseBehavior.resume();
      isPaused.set(false);
   }

   @Override
   public boolean isDone()
   {
      return taskExecutor.isDone();
   }

   @Override
   public void finalize()
   {
      hasInputBeenSet.set(false);
      handPoseBehavior.finalize();
   }

   @Override
   public void initialize()
   {
      handPoseBehavior.initialize();
      hasInputBeenSet.set(false);
   }

   @Override
   public boolean hasInputBeenSet()
   {
      return hasInputBeenSet.getBooleanValue();
   }
}
