package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.SdfLoader.models.FullRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ArmTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.FootTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisHeightTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisTrajectoryMessageSubscriber;
import us.ihmc.humanoidBehaviors.behaviors.scripts.engine.ScriptFileLoader;
import us.ihmc.humanoidBehaviors.behaviors.scripts.engine.ScriptObject;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ComHeightPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PauseWalkingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisHeightTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;

public class ScriptBasedFootstepProvider implements FootstepProvider, Updatable
{
   private int footstepCounter = 0;
   private int completedFootstepCount = 0;

   private final SideDependentList<? extends ContactablePlaneBody> bipedFeet;
   private final ScriptFileLoader scriptFileLoader;
   private boolean loadedScriptFile = false;
   private final ConcurrentLinkedQueue<ScriptObject> scriptObjects = new ConcurrentLinkedQueue<ScriptObject>();

   private final PelvisTrajectoryMessageSubscriber pelvisTrajectoryMessageSubscriber;
   private final PelvisHeightTrajectoryMessageSubscriber pelvisHeightTrajectoryMessageSubscriber;
   private final ConcurrentLinkedQueue<Footstep> footstepQueue = new ConcurrentLinkedQueue<Footstep>();

   private final HandTrajectoryMessageSubscriber handTrajectoryMessageSubscriber;
   private final ArmTrajectoryMessageSubscriber armTrajectoryMessageSubscriber;
   private final FootTrajectoryMessageSubscriber footTrajectoryMessageSubscriber;

   private final DoubleYoVariable time;
   private final DoubleYoVariable scriptEventStartTime, scriptEventDuration;

   private final FullRobotModel fullRobotModel;

   public ScriptBasedFootstepProvider(CommonHumanoidReferenceFrames referenceFrames, ScriptFileLoader scriptFileLoader, DoubleYoVariable time, SideDependentList<? extends ContactablePlaneBody> bipedFeet,
         FullHumanoidRobotModel fullRobotModel, WalkingControllerParameters walkingControllerParameters, YoVariableRegistry registry)
   {
      this.time = time;
      this.bipedFeet = bipedFeet;
      this.fullRobotModel = fullRobotModel;

      this.scriptEventStartTime = new DoubleYoVariable("scriptEventStartTime", registry);
      this.scriptEventDuration = new DoubleYoVariable("scriptEventDuration", registry);

      this.scriptFileLoader = scriptFileLoader;
      pelvisTrajectoryMessageSubscriber = new PelvisTrajectoryMessageSubscriber(null);
      pelvisHeightTrajectoryMessageSubscriber = new PelvisHeightTrajectoryMessageSubscriber(null);

      handTrajectoryMessageSubscriber = new HandTrajectoryMessageSubscriber(null);
      armTrajectoryMessageSubscriber = new ArmTrajectoryMessageSubscriber(null);
      footTrajectoryMessageSubscriber = new FootTrajectoryMessageSubscriber(null);
   }

   private void loadScriptFileIfNecessary()
   {
      FramePoint soleInRootJointFrame = new FramePoint(bipedFeet.get(RobotSide.LEFT).getSoleFrame());
      soleInRootJointFrame.changeFrame(fullRobotModel.getRootJoint().getFrameAfterJoint());
      if (loadedScriptFile)
         return;

      // TODO: Get to work for more than just left foot frame.
      ReferenceFrame leftFootFrame = bipedFeet.get(RobotSide.LEFT).getSoleFrame();
      RigidBodyTransform transformFromLeftFootPlaneFrameToWorldFrame = leftFootFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());

      ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromLeftFootPlaneFrameToWorldFrame);
      scriptObjects.addAll(scriptObjectsList);
      setupTimesForNewScriptEvent(0.1);

      loadedScriptFile = true;
   }

   public void grabNewScriptEventIfNecessary()
   {
      loadScriptFileIfNecessary();

      if (scriptObjects.isEmpty())
         return;
      if (!footstepQueue.isEmpty())
         return;
      if (completedFootstepCount != footstepCounter)
         return;
      if (time.getDoubleValue() < scriptEventStartTime.getDoubleValue() + scriptEventDuration.getDoubleValue())
         return;

      ScriptObject nextObject = scriptObjects.poll();
      Object scriptObject = nextObject.getScriptObject();

      if (scriptObject instanceof FootstepDataListMessage)
      {
         FootstepDataListMessage footstepDataList = (FootstepDataListMessage) scriptObject;
         this.addFootstepDataList(footstepDataList);
         setupTimesForNewScriptEvent(0.5); // Arbitrary half second duration. With footsteps, it waits till they are done before looking for a new command.
      }
      else if (scriptObject instanceof FootTrajectoryMessage)
      {
         FootTrajectoryMessage message = (FootTrajectoryMessage) scriptObject;
         footTrajectoryMessageSubscriber.receivedPacket(message);
         setupTimesForNewScriptEvent(0.5);
      }
      else if (scriptObject instanceof HandTrajectoryMessage)
      {
         HandTrajectoryMessage handTrajectoryMessage = (HandTrajectoryMessage) scriptObject;
         handTrajectoryMessageSubscriber.receivedPacket(handTrajectoryMessage);

         setupTimesForNewScriptEvent(handTrajectoryMessage.getLastWaypoint().time);
      }
      else if (scriptObject instanceof ArmTrajectoryMessage)
      {
         ArmTrajectoryMessage armTrajectoryMessage = (ArmTrajectoryMessage) scriptObject;
         armTrajectoryMessageSubscriber.receivedPacket(armTrajectoryMessage);
         
         setupTimesForNewScriptEvent(armTrajectoryMessage.getTrajectoryTime());
      }
      else if (scriptObject instanceof PelvisTrajectoryMessage)
      {
         PelvisTrajectoryMessage pelvisPosePacket = (PelvisTrajectoryMessage) scriptObject;
         pelvisTrajectoryMessageSubscriber.receivedPacket(pelvisPosePacket);

         setupTimesForNewScriptEvent(pelvisPosePacket.getTrajectoryTime());
      }
      else if (scriptObject instanceof PauseWalkingMessage)
      {
         PauseWalkingMessage pauseCommand = (PauseWalkingMessage) scriptObject;
         setupTimesForNewScriptEvent(0.5);
      }

      else if (scriptObject instanceof PelvisHeightTrajectoryMessage)
      {
         PelvisHeightTrajectoryMessage comHeightPacket = (PelvisHeightTrajectoryMessage) scriptObject;
         pelvisHeightTrajectoryMessageSubscriber.receivedPacket(comHeightPacket);
         setupTimesForNewScriptEvent(2.0); // Arbitrary two second duration to allow for changing the CoM height. Might be possible to lower this a little bit. 
      }
   }

   private void setupTimesForNewScriptEvent(double scriptEventDuration)
   {
      scriptEventStartTime.set(time.getDoubleValue());
      this.scriptEventDuration.set(scriptEventDuration);
   }

   private void addFootstepDataList(FootstepDataListMessage footstepDataList)
   {
      ArrayList<FootstepDataMessage> footstepList = footstepDataList.getDataList();

      ArrayList<Footstep> footsteps = new ArrayList<Footstep>();
      for (FootstepDataMessage footstepData : footstepList)
      {
         RobotSide robotSide = footstepData.getRobotSide();
         ContactablePlaneBody contactableBody = bipedFeet.get(robotSide);

         FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
         PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
         String id = "scriptedFootstep_" + footstepCounter;
         Footstep footstep = new Footstep(id, contactableBody.getRigidBody(), robotSide, contactableBody.getSoleFrame(), footstepPoseFrame, true);

         footsteps.add(footstep);

         footstepCounter++;
      }

      footstepQueue.addAll(footsteps);
   }

   @Override
   public Footstep poll()
   {
      return footstepQueue.poll();
   }

   @Override
   public Footstep peek()
   {
      return footstepQueue.peek();
   }

   @Override
   public Footstep peekPeek()
   {
      Iterator<Footstep> iterator = footstepQueue.iterator();

      if (iterator.hasNext())
      {
         iterator.next();
      }
      else
      {
         return null;
      }
      if (iterator.hasNext())
      {
         return iterator.next();
      }
      else
      {
         return null;
      }
   }

   @Override
   public boolean isEmpty()
   {
      return footstepQueue.isEmpty();
   }

   @Override
   public void notifyComplete(FramePose actualFootPoseInWorld)
   {
      completedFootstepCount++;
   }

   @Override
   public void notifyWalkingComplete()
   {
   }

   @Override
   public int getNumberOfFootstepsToProvide()
   {
      return footstepQueue.size();
   }

   @Override
   public boolean isBlindWalking()
   {
      return false;
   }

   public PelvisTrajectoryMessageSubscriber getPelvisTrajectorySubscriber()
   {
      return pelvisTrajectoryMessageSubscriber;
   }

   public PelvisHeightTrajectoryMessageSubscriber getPelvisHeightTrajectoryMessageSubscriber()
   {
      return pelvisHeightTrajectoryMessageSubscriber;
   }

   public FootTrajectoryMessageSubscriber getFootTrajectoryMessageSubscriber()
   {
      return footTrajectoryMessageSubscriber;
   }

   @Override
   public void update(double time)
   {
      grabNewScriptEventIfNecessary();
   }

   @Override
   public boolean isPaused()
   {
      return false;
   }

   @Override
   public void cancelPlan()
   {
      footstepQueue.clear();
   }
}
