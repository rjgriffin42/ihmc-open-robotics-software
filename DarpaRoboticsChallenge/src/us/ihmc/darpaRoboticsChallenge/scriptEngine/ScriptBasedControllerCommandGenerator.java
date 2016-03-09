package us.ihmc.darpaRoboticsChallenge.scriptEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ControllerCommand;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.FootstepDataListControllerCommand;
import us.ihmc.humanoidBehaviors.behaviors.scripts.engine.ScriptFileLoader;
import us.ihmc.humanoidBehaviors.behaviors.scripts.engine.ScriptObject;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class ScriptBasedControllerCommandGenerator
{
   private final ConcurrentLinkedQueue<ScriptObject> scriptObjects = new ConcurrentLinkedQueue<ScriptObject>();
   private final ConcurrentLinkedQueue<ControllerCommand<?, ?>> controllerCommands;
   
   public ScriptBasedControllerCommandGenerator(ConcurrentLinkedQueue<ControllerCommand<?, ?>> controllerCommands)
   {
      this.controllerCommands = controllerCommands;
   }

   public void loadScriptFile(String scriptFilename, ReferenceFrame referenceFrame)
   {
      ScriptFileLoader scriptFileLoader;
      try
      {
         scriptFileLoader = new ScriptFileLoader(scriptFilename);
         
         RigidBodyTransform transformFromReferenceFrameToWorldFrame = referenceFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
         ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromReferenceFrameToWorldFrame);
         scriptObjects.addAll(scriptObjectsList);
         convertFromScriptObjectsToControllerCommands();
      }
      catch (IOException e)
      {
         System.err.println("Could not load script file " + scriptFilename);
      }            
   }
   
   public void loadScriptFile(InputStream scriptInputStream, ReferenceFrame referenceFrame)
   {
      ScriptFileLoader scriptFileLoader;
      try
      {
         scriptFileLoader = new ScriptFileLoader(scriptInputStream);
         
         RigidBodyTransform transformFromReferenceFrameToWorldFrame = referenceFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
         ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromReferenceFrameToWorldFrame);
         scriptObjects.addAll(scriptObjectsList);
         convertFromScriptObjectsToControllerCommands();
      }
      catch (IOException e)
      {
         System.err.println("Could not load script file " + scriptInputStream);
      }           
      
   }

   private void convertFromScriptObjectsToControllerCommands()
   {
      while(!scriptObjects.isEmpty())
      {
      ScriptObject nextObject = scriptObjects.poll();
      Object scriptObject = nextObject.getScriptObject();

      if (scriptObject instanceof FootstepDataListMessage)
      {
         FootstepDataListMessage footstepDataListMessage = (FootstepDataListMessage) scriptObject;
         FootstepDataListControllerCommand footstepDataListControllerCommand = new FootstepDataListControllerCommand();
         footstepDataListControllerCommand.set(footstepDataListMessage);
         controllerCommands.add(footstepDataListControllerCommand);
      }
//      else if (scriptObject instanceof FootTrajectoryMessage)
//      {
//         FootTrajectoryMessage message = (FootTrajectoryMessage) scriptObject;
//         footTrajectoryMessageSubscriber.receivedPacket(message);
//         setupTimesForNewScriptEvent(0.5);
//      }
//      else if (scriptObject instanceof HandTrajectoryMessage)
//      {
//         HandTrajectoryMessage handTrajectoryMessage = (HandTrajectoryMessage) scriptObject;
//         handTrajectoryMessageSubscriber.receivedPacket(handTrajectoryMessage);
//
//         setupTimesForNewScriptEvent(handTrajectoryMessage.getLastTrajectoryPoint().time);
//      }
//      else if (scriptObject instanceof ArmTrajectoryMessage)
//      {
//         ArmTrajectoryMessage armTrajectoryMessage = (ArmTrajectoryMessage) scriptObject;
//         armTrajectoryMessageSubscriber.receivedPacket(armTrajectoryMessage);
//         
//         setupTimesForNewScriptEvent(armTrajectoryMessage.getTrajectoryTime());
//      }
//      else if (scriptObject instanceof PelvisTrajectoryMessage)
//      {
//         PelvisTrajectoryMessage pelvisPosePacket = (PelvisTrajectoryMessage) scriptObject;
//         pelvisTrajectoryMessageSubscriber.receivedPacket(pelvisPosePacket);
//
//         setupTimesForNewScriptEvent(pelvisPosePacket.getTrajectoryTime());
//      }
//      else if (scriptObject instanceof PauseWalkingMessage)
//      {
//         PauseWalkingMessage pauseWalkingMessage = (PauseWalkingMessage) scriptObject;
//         
////         setupTimesForNewScriptEvent(0.5);
//      }

//      else if (scriptObject instanceof PelvisHeightTrajectoryMessage)
//      {
//         PelvisHeightTrajectoryMessage comHeightPacket = (PelvisHeightTrajectoryMessage) scriptObject;
//         pelvisHeightTrajectoryMessageSubscriber.receivedPacket(comHeightPacket);
//         setupTimesForNewScriptEvent(2.0); // Arbitrary two second duration to allow for changing the CoM height. Might be possible to lower this a little bit. 
//      }
      
      else
      {
         System.err.println("ScriptBasedControllerCommandGenerator: Didn't process script object " + nextObject);
      }
   }

   }




  
}
