package us.ihmc.perception.sceneGraph.multiBodies.door;

import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.perception.sceneGraph.PredefinedRigidBodySceneNode;
import us.ihmc.perception.sceneGraph.SceneGraph;
import us.ihmc.perception.sceneGraph.rigidBodies.StaticRelativeSceneNode;
import us.ihmc.perception.sceneGraph.arUco.ArUcoMarkerNode;
import us.ihmc.robotics.referenceFrames.ReferenceFrameMissingTools;

/**
 * We want to measure this stuff in the right order.
 *
 * It's all based on the one ArUco marker, so it makes setting everything up kinda hard.
 */
public class DoorSceneNodeDefinitions
{

   // PUSH DOOR

   public static final RigidBodyTransform PUSH_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM  = new RigidBodyTransform();
   static
   {
      PUSH_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM.getTranslation().setZ(DoorModelParameters.PUSH_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Z);
      PUSH_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM.getTranslation().setY(-DoorModelParameters.PUSH_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Y);
   }
   public static final RigidBodyTransform PUSH_DOOR_MARKER_TO_PANEL_TRANSFORM = new RigidBodyTransform();
   static
   {
      RigidBodyTransform leverToPanelTransform = new RigidBodyTransform();
      leverToPanelTransform.getTranslation().setX(DoorModelParameters.DOOR_PANEL_THICKNESS / 2.0);
      leverToPanelTransform.getTranslation().setY(DoorModelParameters.DOOR_PANEL_WIDTH - DoorModelParameters.DOOR_LEVER_HANDLE_INSET);
      leverToPanelTransform.getTranslation().setZ(DoorModelParameters.DOOR_LEVER_HANDLE_FROM_BOTTOM_OF_PANEL);

      ReferenceFrame panelFrame = ReferenceFrameMissingTools.constructARootFrame();
      ReferenceFrame leverFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(panelFrame, leverToPanelTransform);
      ReferenceFrame markerFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(leverFrame,
                                                                                                            PUSH_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM);
      FramePose3D markerPose = new FramePose3D(markerFrame);
      markerPose.changeFrame(panelFrame);
      markerPose.get(PUSH_DOOR_MARKER_TO_PANEL_TRANSFORM);
   }
   public static final RigidBodyTransform PUSH_DOOR_FRAME_TO_PANEL_TRANSFORM = new RigidBodyTransform();
   static
   {
      RigidBodyTransform panelToFrameTransform = new RigidBodyTransform();
      panelToFrameTransform.getTranslation().setY(DoorModelParameters.DOOR_PANEL_HINGE_OFFSET);
      panelToFrameTransform.getTranslation().setZ(DoorModelParameters.DOOR_PANEL_GROUND_GAP_HEIGHT);

      ReferenceFrame frameFrame = ReferenceFrameMissingTools.constructARootFrame();
      ReferenceFrame panelFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(frameFrame, panelToFrameTransform);

      FramePose3D framePose = new FramePose3D(frameFrame);
      framePose.changeFrame(panelFrame);
      framePose.get(PUSH_DOOR_FRAME_TO_PANEL_TRANSFORM);
   }

   // PULL DOOR

   public static final RigidBodyTransform PULL_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM  = new RigidBodyTransform();
   static
   {
      PULL_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM.getTranslation().setZ(DoorModelParameters.PULL_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Z);
      PULL_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM.getTranslation().setY(-DoorModelParameters.PULL_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Y);
   }
   public static final RigidBodyTransform PULL_DOOR_MARKER_TO_PANEL_TRANSFORM = new RigidBodyTransform();
   static
   {
      RigidBodyTransform leverToPanelTransform = new RigidBodyTransform();
      leverToPanelTransform.getRotation().setToYawOrientation(Math.PI);
      leverToPanelTransform.getTranslation().setX(-DoorModelParameters.DOOR_PANEL_THICKNESS / 2.0);
      leverToPanelTransform.getTranslation().setY(DoorModelParameters.DOOR_PANEL_WIDTH - DoorModelParameters.DOOR_LEVER_HANDLE_INSET);
      leverToPanelTransform.getTranslation().setZ(DoorModelParameters.DOOR_LEVER_HANDLE_FROM_BOTTOM_OF_PANEL);

      ReferenceFrame panelFrame = ReferenceFrameMissingTools.constructARootFrame();
      ReferenceFrame leverFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(panelFrame, leverToPanelTransform);
      ReferenceFrame markerFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(leverFrame,
                                                                                                            PULL_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM);
      FramePose3D markerPose = new FramePose3D(markerFrame);
      markerPose.changeFrame(panelFrame);
      markerPose.get(PULL_DOOR_MARKER_TO_PANEL_TRANSFORM);
   }
   public static final RigidBodyTransform PULL_DOOR_FRAME_TO_PANEL_TRANSFORM = new RigidBodyTransform();
   static
   {
      RigidBodyTransform panelToFrameTransform = new RigidBodyTransform();
      panelToFrameTransform.getTranslation().setY(DoorModelParameters.DOOR_PANEL_HINGE_OFFSET);
      panelToFrameTransform.getTranslation().setZ(DoorModelParameters.DOOR_PANEL_GROUND_GAP_HEIGHT);

      ReferenceFrame frameFrame = ReferenceFrameMissingTools.constructARootFrame();
      ReferenceFrame panelFrame = ReferenceFrameMissingTools.constructFrameWithUnchangingTransformToParent(frameFrame, panelToFrameTransform);

      FramePose3D framePose = new FramePose3D(frameFrame);
      framePose.changeFrame(panelFrame);
      framePose.get(PULL_DOOR_FRAME_TO_PANEL_TRANSFORM);
   }

   public static final double DOOR_FRAME_MAXIMUM_DISTANCE_TO_LOCK_IN = 2.0;

   // TODO: These transforms need to be verified.
   public static final String DOOR_PANEL_VISUAL_MODEL_FILE_PATH = "environmentObjects/door/doorPanel/DoorPanel.g3dj";
   public static final RigidBodyTransform PULL_DOOR_PANEL_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   public static final RigidBodyTransform PUSH_DOOR_PANEL_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   public static final String DOOR_FRAME_VISUAL_MODEL_FILE_PATH = "environmentObjects/door/doorFrame/DoorFrame.g3dj";
   public static final RigidBodyTransform PULL_DOOR_FRAME_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   public static final RigidBodyTransform PUSH_DOOR_FRAME_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   public static final String DOOR_LEVER_HANDLE_VISUAL_MODEL_FILE_PATH = "environmentObjects/door/doorLeverHandle/DoorLeverHandle.g3dj";
   public static final RigidBodyTransform PULL_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   static
   {
      PULL_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM.appendYawRotation(Math.PI);
   }
   public static final RigidBodyTransform PUSH_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM = new RigidBodyTransform();
   static
   {
      PUSH_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM.appendRollRotation(Math.PI);
      PUSH_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM.appendYawRotation(Math.PI);
   }

   public static PredefinedRigidBodySceneNode createPullDoorPanel()
   {
      PredefinedRigidBodySceneNode node = new PredefinedRigidBodySceneNode(SceneGraph.NEXT_ID.getAndIncrement(),
                                                                           "PullDoorPanel",
                                                                           DOOR_PANEL_VISUAL_MODEL_FILE_PATH,
                                                                           PULL_DOOR_PANEL_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM);
      node.getNodeToParentFrameTransform().setAndInvert(PULL_DOOR_MARKER_TO_PANEL_TRANSFORM);
      node.setOriginalTransformToParent(node.getNodeToParentFrameTransform());
      return node;
   }

   public static PredefinedRigidBodySceneNode createPushDoorPanel()
   {
      PredefinedRigidBodySceneNode node = new PredefinedRigidBodySceneNode(SceneGraph.NEXT_ID.getAndIncrement(),
                                                                           "PushDoorPanel",
                                                                           DOOR_PANEL_VISUAL_MODEL_FILE_PATH,
                                                                           PUSH_DOOR_PANEL_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM);
      node.getNodeToParentFrameTransform().setAndInvert(PUSH_DOOR_MARKER_TO_PANEL_TRANSFORM);
      node.setOriginalTransformToParent(node.getNodeToParentFrameTransform());
      return node;
   }

   public static StaticRelativeSceneNode createPullDoorFrame(ArUcoMarkerNode pullDoorPanel)
   {
      StaticRelativeSceneNode node = new StaticRelativeSceneNode(SceneGraph.NEXT_ID.getAndIncrement(),
                                                                 "PullDoorFrame",
                                                                 DOOR_FRAME_VISUAL_MODEL_FILE_PATH,
                                                                 PULL_DOOR_FRAME_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM,
                                                                 DOOR_FRAME_MAXIMUM_DISTANCE_TO_LOCK_IN);
      node.getNodeToParentFrameTransform().set(PULL_DOOR_FRAME_TO_PANEL_TRANSFORM);
      node.setOriginalTransformToParent(node.getNodeToParentFrameTransform());
      return node;
   }

   public static StaticRelativeSceneNode createPushDoorFrame(ArUcoMarkerNode pushDoorPanel)
   {
      return new StaticRelativeSceneNode(SceneGraph.NEXT_ID.getAndIncrement(),
                                         "PushDoorFrame",
                                         pushDoorPanel,
                                         PUSH_DOOR_FRAME_TO_PANEL_TRANSFORM,
                                         DOOR_FRAME_VISUAL_MODEL_FILE_PATH,
                                         PUSH_DOOR_FRAME_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM,
                                         DOOR_FRAME_MAXIMUM_DISTANCE_TO_LOCK_IN);
   }

   public static ArUcoMarkerNode createPushDoorLeverHandle()
   {
      return new ArUcoMarkerNode(SceneGraph.NEXT_ID.getAndIncrement(),
                                 "PushDoorLeverHandle",
                                 DoorModelParameters.PUSH_DOOR_MARKER_ID,
                                 DoorModelParameters.DOOR_ARUCO_MARKER_WIDTH,
                                 PUSH_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM,
                                 DOOR_LEVER_HANDLE_VISUAL_MODEL_FILE_PATH,
                                 PUSH_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM);
   }

   public static ArUcoMarkerNode createPullDoorLeverHandle()
   {
      return new ArUcoMarkerNode(SceneGraph.NEXT_ID.getAndIncrement(),
                                 "PullDoorLeverHandle",
                                 DoorModelParameters.PULL_DOOR_MARKER_ID,
                                 DoorModelParameters.DOOR_ARUCO_MARKER_WIDTH,
                                 PULL_DOOR_MARKER_TO_LEVER_HANDLE_TRANSFORM,
                                 DOOR_LEVER_HANDLE_VISUAL_MODEL_FILE_PATH,
                                 PULL_DOOR_LEVER_HANDLE_VISUAL_MODEL_TO_NODE_FRAME_TRANSFORM);
   }

   public static void addDefaultObjects(SceneGraph sceneGraph)
   {
      ArUcoMarkerNode pushDoorPanel = DoorSceneNodeDefinitions.createPushDoorPanel();
      ArUcoMarkerNode pullDoorPanel = DoorSceneNodeDefinitions.createPullDoorPanel();
      ArUcoMarkerNode pushDoorLeverHandle = DoorSceneNodeDefinitions.createPushDoorLeverHandle();
      ArUcoMarkerNode pullDoorLeverHandle = DoorSceneNodeDefinitions.createPullDoorLeverHandle();
      sceneGraph.registerArUcoDetectableSceneNode(pushDoorPanel);
      sceneGraph.registerArUcoDetectableSceneNode(pullDoorPanel);
      sceneGraph.registerArUcoDetectableSceneNode(pushDoorLeverHandle);
      sceneGraph.registerArUcoDetectableSceneNode(pullDoorLeverHandle);

      // The frames stay in place after being seen
      StaticRelativeSceneNode pushDoorFrame = DoorSceneNodeDefinitions.createPushDoorFrame(pushDoorPanel);
      StaticRelativeSceneNode pullDoorFrame = DoorSceneNodeDefinitions.createPullDoorFrame(pullDoorPanel);
      sceneGraph.registerStaticArUcoRelativeDetectableSceneNode(pushDoorFrame);
      sceneGraph.registerStaticArUcoRelativeDetectableSceneNode(pullDoorFrame);
   }
}
