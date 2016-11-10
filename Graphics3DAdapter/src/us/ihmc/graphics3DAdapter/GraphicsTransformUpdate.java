package us.ihmc.graphics3DAdapter;

import java.util.concurrent.Callable;

import us.ihmc.graphics3DDescription.structure.Graphics3DNode;
import us.ihmc.robotics.geometry.RigidBodyTransform;

public class GraphicsTransformUpdate implements Callable<Object>
{
   private RigidBodyTransform transform;
   private Graphics3DNode node;

   public GraphicsTransformUpdate(Graphics3DNode node, RigidBodyTransform transform)
   {
      this.node = node;
      this.transform = transform;
   }

   public Object call() throws Exception
   {
      node.setTransform(transform);

      return null;
   }
}
