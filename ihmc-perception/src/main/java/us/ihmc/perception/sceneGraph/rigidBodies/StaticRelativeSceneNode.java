package us.ihmc.perception.sceneGraph.rigidBodies;

import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.perception.sceneGraph.DetectableSceneNode;
import us.ihmc.perception.sceneGraph.PredefinedRigidBodySceneNode;
import us.ihmc.perception.sceneGraph.SceneNode;

import javax.annotation.Nullable;

/**
 * This node stays in the same spot relative to where a parent scene node
 * at the time it is seen up close.
 *
 * Once it has been seen up close, the pose of this node is set as known
 * and does not move until {@link #setTrackDetectedPose} is called.
 *
 * The whole point of this is so we don't have to put markers on everything,
 * especially things that don't move.
 */
public class StaticRelativeSceneNode extends PredefinedRigidBodySceneNode
{
   private final DetectableSceneNode parentNode;
   /**
    * We don't want to lock in the static pose until we are close enough
    * for it to matter and also to get higher accuracy.
    */
   private double distanceToDisableTracking;
   private double currentDistance = Double.NaN;
   private transient final FramePose3D originalPose = new FramePose3D();

   public StaticRelativeSceneNode(String name,
                                  DetectableSceneNode parentNode,
                                  RigidBodyTransform transformToParentNode,
                                  String visualModelFilePath,
                                  RigidBodyTransform visualModelToNodeFrameTransform,
                                  double distanceToDisableTracking)
   {
      super(name, visualModelFilePath, visualModelToNodeFrameTransform);
      this.parentNode = parentNode;

      changeParentFrame(parentNode.getNodeFrame());
      getNodeToParentFrameTransform().set(transformToParentNode);
      getNodeFrame().update();

      this.distanceToDisableTracking = distanceToDisableTracking;
   }

   @Override
   public void setTrackDetectedPose(boolean trackDetectedPose)
   {
      super.setTrackDetectedPose(trackDetectedPose);

      if (parentNode != null)
      {
         if (trackDetectedPose && parentNode.getNodeFrame() != getNodeFrame().getParent())
         {
            changeParentFrameWithoutMoving(parentNode.getNodeFrame());
         }
         else if (!trackDetectedPose && getNodeFrame().getParent() != ReferenceFrame.getWorldFrame())
         {
            changeParentFrameWithoutMoving(ReferenceFrame.getWorldFrame());
         }
      }
   }

   @Override
   public void clearOffset()
   {
      if (parentNode.getNodeFrame() != getNodeFrame().getParent())
      {
         originalPose.setToZero(parentNode.getNodeFrame());
         originalPose.set(getOriginalTransformToParent());
         originalPose.changeFrame(getNodeFrame().getParent());
         originalPose.get(getNodeToParentFrameTransform());
      }
      else
      {
         getNodeToParentFrameTransform().set(getOriginalTransformToParent());
      }
      getNodeFrame().update();
   }

   @Override
   public boolean getCurrentlyDetected()
   {
      return !getTrackDetectedPose() || parentNode.getCurrentlyDetected();
   }

   public void setDistanceToDisableTracking(double distanceToDisableTracking)
   {
      this.distanceToDisableTracking = distanceToDisableTracking;
   }

   public double getDistanceToDisableTracking()
   {
      return distanceToDisableTracking;
   }

   public void setCurrentDistance(double currentDistance)
   {
      this.currentDistance = currentDistance;
   }

   public double getCurrentDistance()
   {
      return currentDistance;
   }

   @Nullable
   public SceneNode getParentNode()
   {
      return parentNode;
   }
}
