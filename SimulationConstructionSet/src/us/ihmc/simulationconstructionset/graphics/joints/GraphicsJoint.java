package us.ihmc.simulationconstructionset.graphics.joints;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.kinematics.CommonJoint;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;

public class GraphicsJoint extends Graphics3DNode
{
   private final CommonJoint joint;
   private final RigidBodyTransform transformToParent = new RigidBodyTransform();

   public GraphicsJoint(String name, CommonJoint joint, Graphics3DObject graphics3DObject, Graphics3DNodeType nodeType)
   {
      super(name, nodeType);
      this.joint = joint;

      setGraphicsObject(graphics3DObject);
     
   }

   public final void updateFromJoint()
   {
      transformToParent.setIdentity();
      transformToParent.multiply(joint.getOffsetTransform3D());
      transformToParent.multiply(joint.getJointTransform3D());
      setTransform(transformToParent);
   }
}
