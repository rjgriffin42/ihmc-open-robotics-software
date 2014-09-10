package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.j3d.Transform3D;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredFootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredPelvisPoseProvider;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.walking.ComHeightPacket;
import us.ihmc.communication.packets.walking.FootPosePacket;
import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.communication.packets.walking.PauseCommand;
import us.ihmc.communication.packets.walking.PelvisPosePacket;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;


public class ScriptBasedFootstepProvider implements FootstepProvider, Updatable
{
   private int footstepCounter = 0;
   private int completedFootstepCount = 0;
   
   private final SideDependentList<ContactablePlaneBody> bipedFeet;
   private final ScriptFileLoader scriptFileLoader;
   private boolean loadedScriptFile = false;
   private final ConcurrentLinkedQueue<ScriptObject> scriptObjects = new ConcurrentLinkedQueue<ScriptObject>();

   private final DesiredHandPoseProvider desiredHandPoseProvider; 
   private final DesiredPelvisPoseProvider desiredPelvisPoseProvider; 
   private final DesiredHandstepProvider handstepProvider;
   private final DesiredComHeightProvider desiredComHeightProvider;
   private final DesiredFootPoseProvider desiredFootPoseProvider;
   private final ConcurrentLinkedQueue<Footstep> footstepQueue = new ConcurrentLinkedQueue<Footstep>();
   
   private final DoubleYoVariable time;
   private final DoubleYoVariable scriptEventStartTime, scriptEventDuration;

   private final FullRobotModel fullRobotModel;
   
   public ScriptBasedFootstepProvider(ScriptFileLoader scriptFileLoader, DoubleYoVariable time, SideDependentList<ContactablePlaneBody> bipedFeet,
         FullRobotModel fullRobotModel, WalkingControllerParameters walkingControllerParameters, YoVariableRegistry registry)
   {
      this.time = time;
      this.bipedFeet = bipedFeet;
      this.fullRobotModel = fullRobotModel;
      
      this.scriptEventStartTime = new DoubleYoVariable("scriptEventStartTime", registry);
      this.scriptEventDuration = new DoubleYoVariable("scriptEventDuration", registry);
      
      this.scriptFileLoader = scriptFileLoader;
      desiredHandPoseProvider = new DesiredHandPoseProvider(fullRobotModel, walkingControllerParameters.getDesiredHandPosesWithRespectToChestFrame());
      desiredPelvisPoseProvider = new DesiredPelvisPoseProvider();
      handstepProvider = new DesiredHandstepProvider(fullRobotModel);
      desiredComHeightProvider = new DesiredComHeightProvider();

      desiredFootPoseProvider = new DesiredFootPoseProvider();
   }
   
   private void loadScriptFileIfNecessary()
   {      
      FramePoint soleInRootJointFrame = new FramePoint(bipedFeet.get(RobotSide.LEFT).getSoleFrame());
      soleInRootJointFrame.changeFrame(fullRobotModel.getRootJoint().getFrameAfterJoint());
      if (loadedScriptFile) return;
      
//      //TODO: Get to work for more than just left foot frame.
      ReferenceFrame leftFootFrame = bipedFeet.get(RobotSide.LEFT).getSoleFrame();
      Transform3D transformFromLeftFootPlaneFrameToWorldFrame = leftFootFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());      
      
      ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromLeftFootPlaneFrameToWorldFrame); 
      scriptObjects.addAll(scriptObjectsList);

      loadedScriptFile = true;
   }
   
   public void grabNewScriptEventIfNecessary()
   { 
      loadScriptFileIfNecessary();

      if (scriptObjects.isEmpty()) return;
      if (!footstepQueue.isEmpty()) return;
      if (completedFootstepCount != footstepCounter) return;
      if (time.getDoubleValue() < scriptEventStartTime.getDoubleValue() + scriptEventDuration.getDoubleValue()) return;
      
      ScriptObject nextObject = scriptObjects.poll();
      Object scriptObject = nextObject.getScriptObject();

      if (scriptObject instanceof FootstepDataList)
      { 
         FootstepDataList footstepDataList = (FootstepDataList) scriptObject;
         this.addFootstepDataList(footstepDataList);
         setupTimesForNewScriptEvent(0.5); // Arbitrary half second duration. With footsteps, it waits till they are done before looking for a new command.
      }
      else if (scriptObject instanceof FootPosePacket)
      { 
         FootPosePacket footPosePacket = (FootPosePacket) scriptObject;
         desiredFootPoseProvider.consumeObject(footPosePacket);
         setupTimesForNewScriptEvent(0.5);
      }
      else if (scriptObject instanceof HandPosePacket)
      {
         HandPosePacket handPosePacket = (HandPosePacket) scriptObject;
         desiredHandPoseProvider.consumeObject(handPosePacket);
         
         setupTimesForNewScriptEvent(handPosePacket.getTrajectoryTime());
      }
      else if (scriptObject instanceof PelvisPosePacket)
      {
         PelvisPosePacket pelvisPosePacket = (PelvisPosePacket) scriptObject;
         desiredPelvisPoseProvider.consumeObject(pelvisPosePacket);
         
         setupTimesForNewScriptEvent(pelvisPosePacket.getTrajectoryTime());
      }
      else if (scriptObject instanceof PauseCommand)
      {
         PauseCommand pauseCommand = (PauseCommand) scriptObject;
         setupTimesForNewScriptEvent(0.5);
      }
      
      else if (scriptObject instanceof ComHeightPacket )
      {
         ComHeightPacket comHeightPacket = (ComHeightPacket) scriptObject;
         desiredComHeightProvider.getComHeightPacketConsumer().consumeObject(comHeightPacket);
         setupTimesForNewScriptEvent(2.0); // Arbitrary two second duration to allow for changing the CoM height. Might be possible to lower this a little bit. 
      }
   }
   
   private void setupTimesForNewScriptEvent(double scriptEventDuration)
   {
      scriptEventStartTime.set(time.getDoubleValue());
      this.scriptEventDuration.set(scriptEventDuration);
   }

   private void addFootstepDataList(FootstepDataList footstepDataList)
   {
      ArrayList<FootstepData> footstepList = footstepDataList.getDataList();

      ArrayList<Footstep> footsteps = new ArrayList<Footstep>();
      for (FootstepData footstepData : footstepList)
      {
         RobotSide robotSide = footstepData.getRobotSide();
         ContactablePlaneBody contactableBody = bipedFeet.get(robotSide);

         FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());
         PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);
         String id = "scriptedFootstep_" + footstepCounter;
         Footstep footstep = new Footstep(id, contactableBody, footstepPoseFrame, true);

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
   public void notifyComplete()
   {
      completedFootstepCount++;      
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
   
   public DesiredHandPoseProvider getDesiredHandPoseProvider()
   {
      return desiredHandPoseProvider;
   }
   
   public DesiredPelvisPoseProvider getDesiredPelvisPoseProvider()
   {
      return desiredPelvisPoseProvider;
   }

   public DesiredComHeightProvider getDesiredComHeightProvider()
   {
      return desiredComHeightProvider;
   }

   public DesiredFootPoseProvider getDesiredFootPoseProvider()
   {
      return desiredFootPoseProvider;
   }
   
   public DesiredHandstepProvider getDesiredHandstepProvider()
   {
      return handstepProvider;
   }

   @Override
   public void update(double time)
   {
      grabNewScriptEventIfNecessary();
   }
}
