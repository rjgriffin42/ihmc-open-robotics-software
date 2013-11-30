package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepUtils;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.PauseCommand;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHandPoseProvider;
import us.ihmc.commonWalkingControlModules.packets.HandPosePacket;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class ScriptBasedFootstepProvider implements FootstepProvider
{
   private int footstepCounter = 0;
   private int completedFootstepCount = 0;
   
   private final SideDependentList<ContactablePlaneBody> bipedFeet;
   private final ScriptFileLoader scriptFileLoader;
   private boolean loadedScriptFile = false;
   private final ConcurrentLinkedQueue<ScriptObject> scriptObjects = new ConcurrentLinkedQueue<ScriptObject>();

   private final DesiredHandPoseProvider desiredHandPoseProvider; 
   private final ConcurrentLinkedQueue<Footstep> footstepQueue = new ConcurrentLinkedQueue<Footstep>();
   
   private final DoubleYoVariable time;
   private final DoubleYoVariable scriptEventStartTime, scriptEventDuration;
   
   public ScriptBasedFootstepProvider(ScriptFileLoader scriptFileLoader, DoubleYoVariable time, SideDependentList<ContactablePlaneBody> bipedFeet, 
         FullRobotModel fullRobotModel, WalkingControllerParameters walkingControllerParameters, YoVariableRegistry registry)
   {
      this.time = time;
      this.bipedFeet = bipedFeet;
      
      this.scriptEventStartTime = new DoubleYoVariable("scriptEventStartTime", registry);
      this.scriptEventDuration = new DoubleYoVariable("scriptEventDuration", registry);
      
      this.scriptFileLoader = scriptFileLoader;
      desiredHandPoseProvider = new DesiredHandPoseProvider(fullRobotModel, walkingControllerParameters, registry);
   }
   
   private void loadScriptFileIfNecessary()
   {      
      if (loadedScriptFile) return;
      
//      //TODO: Get to work for more than just left foot frame.
      ReferenceFrame leftFootFrame = bipedFeet.get(RobotSide.LEFT).getBodyFrame();
      Transform3D transformFromLeftFootToWorldFrame = leftFootFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());      
      
      //TODO: Why does z translation need to be zero here?
      Vector3d translation = new Vector3d();
      transformFromLeftFootToWorldFrame.get(translation);
      translation.setZ(0.0);
      transformFromLeftFootToWorldFrame.setTranslation(translation);
      
      ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromLeftFootToWorldFrame); 
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
      else if (scriptObject instanceof HandPosePacket)
      {
         HandPosePacket handPosePacket = (HandPosePacket) scriptObject;
         desiredHandPoseProvider.consumeObject(handPosePacket);
         
         setupTimesForNewScriptEvent(handPosePacket.getTrajectoryTime());
      }

      else if (scriptObject instanceof PauseCommand)
      {
         PauseCommand pauseCommand = (PauseCommand) scriptObject;
         setupTimesForNewScriptEvent(0.5);
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
         ReferenceFrame soleReferenceFrame = FootstepUtils.createSoleFrame(footstepPoseFrame, contactableBody);

         List<FramePoint> expectedContactPoints = FootstepUtils.getContactPointsInFrame(contactableBody, soleReferenceFrame);

         String id = "scriptedFootstep_" + footstepCounter;
         Footstep footstep = new Footstep(id, contactableBody, footstepPoseFrame, soleReferenceFrame, expectedContactPoints, true);

         footsteps.add(footstep);

         footstepCounter++;
      }

      footstepQueue.addAll(footsteps);
   }
   
   @Override
   public Footstep poll()
   {
      grabNewScriptEventIfNecessary();
      return footstepQueue.poll();
   }

   @Override
   public Footstep peek()
   {
      grabNewScriptEventIfNecessary();
      return footstepQueue.peek();
   }

   @Override
   public Footstep peekPeek()
   {
      grabNewScriptEventIfNecessary();

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
      grabNewScriptEventIfNecessary();
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
      grabNewScriptEventIfNecessary();
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

}
