package us.ihmc.humanoidBehaviors.behaviors.rrtPlanner;

import java.util.ArrayList;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

import us.ihmc.commons.PrintTools;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.communication.packets.PlanarRegionsListMessage;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage;
import us.ihmc.communication.packets.TextToSpeechPacket;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.MeshDataGenerator;
import us.ihmc.graphicsDescription.MeshDataHolder;
import us.ihmc.graphicsDescription.ModifiableMeshDataHolder;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.WholeBodyTrajectoryBehavior;
import us.ihmc.humanoidBehaviors.behaviors.rrtPlanner.CleaningMotionStateMachineBehavior.CleaningMotionState;
import us.ihmc.humanoidBehaviors.behaviors.rrtPlanner.SolarPanelCleaningInfo.CleaningPathType;
import us.ihmc.humanoidBehaviors.behaviors.rrtPlanner.SolarPanelCleaningInfo.DegreesOfRedundancy;
import us.ihmc.humanoidBehaviors.behaviors.simpleBehaviors.BehaviorAction;
import us.ihmc.humanoidBehaviors.behaviors.solarPanel.SolarPanelWholeBodyTrajectoryMessageFacotry;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridge;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidBehaviors.stateMachine.StateMachineBehavior;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.jMonkeyEngineToolkit.jme.JMEMeshDataInterpreter;
import us.ihmc.manipulation.planning.solarpanelmotion.SolarPanel;
import us.ihmc.manipulation.planning.solarpanelmotion.SolarPanelCleaningPose;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.geometry.transformables.Pose;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateTransitionCondition;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;

public class CleaningMotionStateMachineBehavior extends StateMachineBehavior<CleaningMotionState>
{   
   private GetSolarPanelBehavior getSolarPanelBehavior;
   private ControlPointOptimizationStateMachineBehavior controlPointOptimizationBehavior;
   
   private WholeBodyTrajectoryBehavior wholebodyTrajectoryBehavior;
   
   private TestDoneBehavior doneBehavior;
   
   private SolarPanelWholeBodyTrajectoryMessageFacotry motionFactory;
   
   private DoubleYoVariable yoTime;
   private FullHumanoidRobotModel fullRobotModel;
   private WholeBodyControllerParameters wholeBodyControllerParameters;
      
   
   
   private final ConcurrentListeningQueue<PlanarRegionsListMessage> planarRegionsListQueue = new ConcurrentListeningQueue<>(10);
   
   public enum CleaningMotionState
   {
      GET_SOLARPANEL, CONTROLPOINT_OPTIMIZATION, GOTO_READYPOSE, CLEANING_MOTION, DONE
   }
   
   public CleaningMotionStateMachineBehavior(CommunicationBridge communicationBridge, DoubleYoVariable yoTime,  
                                         WholeBodyControllerParameters wholeBodyControllerParameters, FullHumanoidRobotModel fullRobotModel, HumanoidReferenceFrames referenceFrames)
   {
      super("CleaningMotionStateMachineBehavior", CleaningMotionState.class, yoTime, communicationBridge);
      
      PrintTools.info("CleaningMotionStateMachineBehavior ");

      getSolarPanelBehavior = new GetSolarPanelBehavior(communicationBridge);
      wholebodyTrajectoryBehavior = new WholeBodyTrajectoryBehavior(communicationBridge, yoTime);
      doneBehavior = new TestDoneBehavior(communicationBridge);      
      
      motionFactory = new SolarPanelWholeBodyTrajectoryMessageFacotry(fullRobotModel);
      
      this.yoTime = yoTime;
      this.fullRobotModel = fullRobotModel;
      this.wholeBodyControllerParameters = wholeBodyControllerParameters;
      
      controlPointOptimizationBehavior
      = new ControlPointOptimizationStateMachineBehavior(communicationBridge, yoTime, wholeBodyControllerParameters, fullRobotModel, referenceFrames);
      
      
      attachNetworkListeningQueue(planarRegionsListQueue, PlanarRegionsListMessage.class);
      
      setUpStateMachine();
   }
   
   public void setUpStateMachine()
   {    
      // condition check for the case that no solution is exist in the CONTROLPOINT_OPTIMIZATION state.
      // In that case, the state transition should be to the DONE state.
      
      // In CONTROLPOINT_OPTIMIZATION, manually selected cleaning motion is put for test.
      
      
      BehaviorAction<CleaningMotionState> getSolarPanelAction = new BehaviorAction<CleaningMotionState>(CleaningMotionState.GET_SOLARPANEL, getSolarPanelBehavior)
      {
         @Override
         protected void setBehaviorInput()
         {
            TextToSpeechPacket p1 = new TextToSpeechPacket("getSolarPanelAction");
            sendPacket(p1);
         }
      };
      
      BehaviorAction<CleaningMotionState> controlPointOptimizationAction = new BehaviorAction<CleaningMotionState>(CleaningMotionState.CONTROLPOINT_OPTIMIZATION, controlPointOptimizationBehavior)
      {
         @Override
         protected void setBehaviorInput()
         {
            TextToSpeechPacket p1 = new TextToSpeechPacket("controlPointOptimizationAction");
            sendPacket(p1);
         }
      };
      
      StateTransitionCondition yesSolutionCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {            
            boolean b = controlPointOptimizationAction.isDone() && controlPointOptimizationBehavior.isSolved() == true;         
            return b;
         }
      };
      
      StateTransitionCondition noSolutionCondition = new StateTransitionCondition()
      {
         @Override
         public boolean checkCondition()
         {            
            boolean b = controlPointOptimizationAction.isDone() && controlPointOptimizationBehavior.isSolved() != true;
            return b;
         }
      };
      
      BehaviorAction<CleaningMotionState> gotoReadyPoseAction = new BehaviorAction<CleaningMotionState>(CleaningMotionState.GOTO_READYPOSE, wholebodyTrajectoryBehavior)
      {
         @Override
         protected void setBehaviorInput()
         {
            TextToSpeechPacket p1 = new TextToSpeechPacket("gotoReadyPoseAction");
            sendPacket(p1);
            
            PrintTools.info("gotoReadyPoseAction");
            WholeBodyTrajectoryMessage wholebodyMessage = new WholeBodyTrajectoryMessage();
            
            SolarPanelCleaningPose pose = SolarPanelCleaningInfo.getReadyPose();
            motionFactory.setMessage(pose, Math.PI*0.0, 0.0, 3.0);
            wholebodyMessage = motionFactory.getWholeBodyTrajectoryMessage();
            wholebodyTrajectoryBehavior.setInput(wholebodyMessage);
         }
      };
      
      BehaviorAction<CleaningMotionState> cleaningAction = new BehaviorAction<CleaningMotionState>(CleaningMotionState.CLEANING_MOTION, wholebodyTrajectoryBehavior)
      {
         @Override
         protected void setBehaviorInput()
         {
            TextToSpeechPacket p1 = new TextToSpeechPacket("cleaningAction");
            sendPacket(p1);
            
            PrintTools.info("cleaningAction");
            WholeBodyTrajectoryMessage wholebodyMessage = new WholeBodyTrajectoryMessage();
            motionFactory.setCleaningPath(SolarPanelCleaningInfo.getCleaningPath());         
            motionFactory.setMessage(controlPointOptimizationBehavior.getOptimalControlPointNodePath());            
            wholebodyMessage = motionFactory.getWholeBodyTrajectoryMessage();
            wholebodyTrajectoryBehavior.setInput(wholebodyMessage);
            
            
         }
      };
            
      BehaviorAction<CleaningMotionState> doneAction = new BehaviorAction<CleaningMotionState>(CleaningMotionState.DONE, doneBehavior)
      {
         @Override
         protected void setBehaviorInput()
         {
            TextToSpeechPacket p1 = new TextToSpeechPacket("doneAction");
            sendPacket(p1);
         }
      };
            
      statemachine.addStateWithDoneTransition(getSolarPanelAction, CleaningMotionState.CONTROLPOINT_OPTIMIZATION);
            
      statemachine.addStateWithDoneTransition(gotoReadyPoseAction, CleaningMotionState.CLEANING_MOTION);
      statemachine.addStateWithDoneTransition(cleaningAction, CleaningMotionState.DONE);
      
      statemachine.addState(controlPointOptimizationAction);            
      controlPointOptimizationAction.addStateTransition(CleaningMotionState.GOTO_READYPOSE, yesSolutionCondition);
      controlPointOptimizationAction.addStateTransition(CleaningMotionState.DONE, noSolutionCondition);
      
      statemachine.addState(doneAction);
      
      statemachine.setStartState(CleaningMotionState.GET_SOLARPANEL);
      PrintTools.info("setUpStateMachine done ");
   }

   @Override
   public void onBehaviorExited()
   {
      // TODO Auto-generated method stub
   }
      
   private void setUpSolarPanel()
   {
   }
   
   private boolean isPlanarRegionWithinVolume(PlanarRegion planarRegion)
   {
      boolean isAllNullPolygon = true;
      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      planarRegion.getTransformToWorld(transformToWorld);

      PrintTools.info("getNumberOfConvexPolygons "+planarRegion.getNumberOfConvexPolygons());
      
      for (int polygonIndex = 0; polygonIndex < planarRegion.getNumberOfConvexPolygons(); polygonIndex++)
      {
         ConvexPolygon2D convexPolygon = planarRegion.getConvexPolygon(polygonIndex);
         MeshDataHolder polygon = MeshDataGenerator.Polygon(transformToWorld, convexPolygon);
         
         PrintTools.info("polygonIndex "+polygonIndex);
         if(polygon != null)
         {
            PrintTools.info("Vectices "+polygon.getVertices().length);
            for(int i=0;i<polygon.getVertices().length;i++)
            {
               PrintTools.info(""+i+" "+polygon.getVertices()[i].getX()+" "+polygon.getVertices()[i].getY()+" "+polygon.getVertices()[i].getZ());
               if(!isOutsideOftheVolume(polygon.getVertices()[i]))
               {
                  return false;
               }                  
            }
            isAllNullPolygon = false;
         }         
      }
      if(isAllNullPolygon == true)
      {
         PrintTools.info("All polygons are null ");
         return false;
      }
         
      
      return true;
   }
   
   private boolean isOutsideOftheVolume(Point3D32 pointOfVertex)
   {
      if(pointOfVertex.getX() > 1.5 || pointOfVertex.getX() < 0.5 || pointOfVertex.getY() > 1.0 || pointOfVertex.getY() < -1.0 || pointOfVertex.getZ() > 1.5 || pointOfVertex.getZ() < 0.5)
      {
         PrintTools.info("This polygon is on outside of the volume ");
         return false;
      } 
      
      return true;
   }
      
   private void requestPlanarRegions()
   {
      RequestPlanarRegionsListMessage requestPlanarRegionsListMessage = new RequestPlanarRegionsListMessage(
            RequestPlanarRegionsListMessage.RequestType.SINGLE_UPDATE);
      requestPlanarRegionsListMessage.setDestination(PacketDestination.REA_MODULE);
      sendPacket(requestPlanarRegionsListMessage);
      sendPacket(requestPlanarRegionsListMessage);
   }
   
   private void sortOutSolarPanel(PlanarRegionsList planarRegionsList)
   {
      PrintTools.info("getNumberOfPlanarRegions");
      PrintTools.info(""+planarRegionsList.getNumberOfPlanarRegions());
      
      ArrayList<PlanarRegion> planarRegionsWithinVolume = new ArrayList<PlanarRegion>();
      
      for(int i=0;i<planarRegionsList.getNumberOfPlanarRegions();i++)
      {
         PrintTools.info("");
         PrintTools.info("Planar Region "+i);         
         
         PlanarRegion planarRegion = planarRegionsList.getPlanarRegion(i);
         //Geometry geometryOfPlanarRegion = createRegionGeometry(planarRegion, null);
         if(isPlanarRegionWithinVolume(planarRegion))
            planarRegionsWithinVolume.add(planarRegion);
      }
      
      PrintTools.info("");
      PrintTools.info("The number Of planar regions with in volume is "+planarRegionsWithinVolume.size());
      for(int i=0;i<planarRegionsWithinVolume.size();i++)
      {
         
      }
   }
   
   private class GetSolarPanelBehavior extends AbstractBehavior
   {
      private final BooleanYoVariable receivedPlanarRegionsList = new BooleanYoVariable("ReceivedPlanarRegionsList", registry);
      private PlanarRegionsList planarRegionsList;

      public GetSolarPanelBehavior(CommunicationBridgeInterface communicationBridge)
      {
         super(communicationBridge);
      }

      @Override
      public void doControl()
      {            
         if (planarRegionsListQueue.isNewPacketAvailable())
         {
            PlanarRegionsListMessage planarRegionsListMessage = planarRegionsListQueue.getLatestPacket();
            planarRegionsList = PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListMessage);
            receivedPlanarRegionsList.set(true);
            
            
            
            
            
            
            sortOutSolarPanel(planarRegionsList);
            
            PrintTools.info("!!!!!!!!!!!!! "+ planarRegionsList.getNumberOfPlanarRegions());
         }
         else
         {
            requestPlanarRegions();
         }
      }

      @Override
      public void onBehaviorEntered()
      {   
         receivedPlanarRegionsList.set(false);
         requestPlanarRegions();

         PrintTools.info("Entered GetSolarPanelBehavior ");
      }

      @Override
      public void onBehaviorAborted()
      {
      }

      @Override
      public void onBehaviorPaused()
      {
      }

      @Override
      public void onBehaviorResumed()
      {
      }

      @Override
      public void onBehaviorExited()
      {           
         // ********************************** get SolarPanel Info ********************************** //  
         Pose poseSolarPanel = new Pose();
         Quaternion quaternionSolarPanel = new Quaternion();
         poseSolarPanel.setPosition(0.75, -0.1, 0.9);
         quaternionSolarPanel.appendYawRotation(Math.PI*0.05);
         quaternionSolarPanel.appendRollRotation(0.0);
         quaternionSolarPanel.appendPitchRotation(-Math.PI*0.25);
         poseSolarPanel.setOrientation(quaternionSolarPanel);
         
         SolarPanel solarPanel = new SolarPanel(poseSolarPanel, 0.6, 0.6);
         
         // ********************************** get SolarPanel Info ********************************** //
         // *********************************** get Cleaning Path *********************************** //
         
         
         
         SolarPanelCleaningInfo.setSolarPanel(solarPanel);
         SolarPanelCleaningInfo.setCleaningPath(CleaningPathType.HORIZONAL);
         SolarPanelCleaningInfo.setDegreesOfRedundancy(DegreesOfRedundancy.THREE);
         
         PrintTools.info("cur Height is "+ fullRobotModel.getPelvis().getBodyFixedFrame().getTransformToWorldFrame().getM23() +" "+ yoTime);
         TimeDomain3DNode.defaultPelvisHeight = fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint().getTransformToWorldFrame().getM23();
         // *********************************** get Cleaning Path *********************************** //
         controlPointOptimizationBehavior.setRootNode(SolarPanelCleaningInfo.getNode());
         

         PrintTools.info("Exit GetSolarPanelBehavior ");
      }

      @Override
      public boolean isDone()
      {
         return receivedPlanarRegionsList.getBooleanValue();
      }
   }
   
   
   private class TestDoneBehavior extends AbstractBehavior
   {
      private boolean isDone = false;
      
      public TestDoneBehavior(CommunicationBridgeInterface communicationBridge)
      {
         super(communicationBridge);
      }

      @Override
      public void doControl()
      {            
         isDone = true;
      }

      @Override
      public void onBehaviorEntered()
      {   
         PrintTools.info("TestDoneBehavior");
         
      }

      @Override
      public void onBehaviorAborted()
      {
      }

      @Override
      public void onBehaviorPaused()
      {
      }

      @Override
      public void onBehaviorResumed()
      {
      }

      @Override
      public void onBehaviorExited()
      {         
      }

      @Override
      public boolean isDone()
      {
         return isDone;
      }
   }
}
