package us.ihmc.humanoidBehaviors.behaviors.midLevel;

import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.behaviors.primitives.PelvisHeightTrajectoryBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.HandDesiredConfigurationBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.HandPoseBehavior;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.taskExecutor.PelvisHeightTrajectoryTask;
import us.ihmc.humanoidBehaviors.taskExecutor.HandDesiredConfigurationTask;
import us.ihmc.humanoidBehaviors.taskExecutor.GraspCylinderTask;
import us.ihmc.humanoidBehaviors.taskExecutor.OrientPalmToGraspCylinderTask;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.GraspCylinderPacket;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HandConfiguration;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.taskExecutor.PipeLine;

public class GraspCylinderBehavior extends BehaviorInterface
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final FullHumanoidRobotModel fullRobotModel;

   private final PipeLine<BehaviorInterface> pipeLine = new PipeLine<>();
   private final PelvisHeightTrajectoryBehavior comHeightBehavior;
   private final HandPoseBehavior handPoseBehavior;
   private final HandDesiredConfigurationBehavior handDesiredConfigurationBehavior;

   private final BooleanYoVariable haveInputsBeenSet;
   private final DoubleYoVariable yoTime;

   private final ConcurrentListeningQueue<GraspCylinderPacket> graspCylinderPacketListener;

   private RobotSide robotSide = null;

   public GraspCylinderBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, FullHumanoidRobotModel fullRobotModel,
         DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridge);

      this.yoTime = yoTime;
      this.fullRobotModel = fullRobotModel;

      comHeightBehavior = new PelvisHeightTrajectoryBehavior(outgoingCommunicationBridge, yoTime);
      handPoseBehavior = new HandPoseBehavior(outgoingCommunicationBridge, yoTime);
      handDesiredConfigurationBehavior = new HandDesiredConfigurationBehavior(outgoingCommunicationBridge, yoTime);

      haveInputsBeenSet = new BooleanYoVariable("haveInputsBeenSet", registry);

      graspCylinderPacketListener = new ConcurrentListeningQueue<GraspCylinderPacket>();
      super.attachNetworkProcessorListeningQueue(graspCylinderPacketListener, GraspCylinderPacket.class);

   }

   public void setInput(GraspCylinderPacket graspCylinderPacket)
   {
      FramePoint graspPoint = new FramePoint(worldFrame, graspCylinderPacket.graspPointInWorld);
      FrameVector graspCylinderLongAxis = new FrameVector(worldFrame, graspCylinderPacket.getCylinderLongAxisInWorld());
      
      setGraspPose(graspCylinderPacket.getRobotSide(), graspPoint, graspCylinderLongAxis, false);
   }

   public void setGraspPose(RobotSide robotSide, FramePoint graspPoint, FrameVector graspedCylinderLongAxis, boolean stopHandIfCollision)
   {
      this.robotSide = robotSide;

      graspPoint.changeFrame(worldFrame);

      PelvisHeightTrajectoryTask comHeightTask = new PelvisHeightTrajectoryTask(graspPoint.getZ() - 0.9, yoTime, comHeightBehavior, 1.0);
      HandDesiredConfigurationTask openHandTask = new HandDesiredConfigurationTask(robotSide, HandConfiguration.OPEN, handDesiredConfigurationBehavior, yoTime);

      OrientPalmToGraspCylinderTask orientPalmForGraspingTask = new OrientPalmToGraspCylinderTask(robotSide, graspPoint, graspedCylinderLongAxis,
            fullRobotModel, yoTime, handPoseBehavior, 3.0);

      GraspCylinderTask movePalmToContactCylinder = new GraspCylinderTask(robotSide, graspPoint, graspedCylinderLongAxis, fullRobotModel, yoTime,
            handPoseBehavior, 1.0);

      HandDesiredConfigurationTask closeHandTask = new HandDesiredConfigurationTask(robotSide, HandConfiguration.CLOSE, handDesiredConfigurationBehavior, yoTime);

      pipeLine.clearAll();
      pipeLine.submitSingleTaskStage(comHeightTask);
      pipeLine.submitSingleTaskStage(orientPalmForGraspingTask);
      pipeLine.submitSingleTaskStage(openHandTask);
      pipeLine.submitSingleTaskStage(movePalmToContactCylinder);
      pipeLine.submitSingleTaskStage(closeHandTask);

      haveInputsBeenSet.set(true);
   }

   @Override
   public void doControl()
   {
      if (graspCylinderPacketListener.isNewPacketAvailable() && !haveInputsBeenSet.getBooleanValue())
      {
         setInput(graspCylinderPacketListener.getNewestPacket());
      }
      
      pipeLine.doControl();
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

   public RobotSide getSideToUse()
   {
      return robotSide;
   }

   @Override
   public void stop()
   {
      handPoseBehavior.stop();
      handDesiredConfigurationBehavior.stop();
      comHeightBehavior.stop();
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
      handDesiredConfigurationBehavior.pause();
      comHeightBehavior.pause();
   }

   @Override
   public void resume()
   {
      handPoseBehavior.resume();
      handDesiredConfigurationBehavior.resume();
      comHeightBehavior.resume();
   }

   @Override
   public boolean isDone()
   {
      return pipeLine.isDone();
   }

   @Override
   public void doPostBehaviorCleanup()
   {
      haveInputsBeenSet.set(false);
   }

   @Override
   public void initialize()
   {
      haveInputsBeenSet.set(false);

   }

   @Override
   public boolean hasInputBeenSet()
   {
      return haveInputsBeenSet.getBooleanValue();
   }
}
