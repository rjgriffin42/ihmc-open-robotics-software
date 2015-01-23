package us.ihmc.graphics3DAdapter.jme;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.jme.util.JMEDataTypeUtils;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.math.geometry.Transform3d;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;

public class JMEGraphics3DNode extends Node implements JMEUpdatable
{
   private final Graphics3DNode graphics3dNode;
   
   private final static boolean DEBUG= false;
   
   private final Quat4d rotation = new Quat4d();
   private final Vector3d translation = new Vector3d();
   private final Vector3d scale = new Vector3d();
   
   private final Quaternion jmeRotation = new Quaternion();
   private final Vector3f jmeTranslation = new Vector3f();
   private final Vector3f jmeScale = new Vector3f();

   private JMEGraphicsObject graphics;
   private Node graphicsObjectNode;
   private final JMEAssetLocator assetManager;
   private final JMERenderer application;
   
   public JMEGraphics3DNode(Graphics3DNode graphics3dNode, JMEAssetLocator assetManager, JMERenderer application)
   {  
      super(graphics3dNode.getName());
      this.graphics3dNode = graphics3dNode;
      this.application = application;
      
      this.assetManager = assetManager;
      createAndAttachGraphicsObject();
   }

   private void createAndAttachGraphicsObject()
   {      
      Graphics3DObject graphicsObject = graphics3dNode.getGraphicsObjectAndResetHasGraphicsObjectChanged();
      if(graphicsObject != null)
      {
         if (graphicsObjectNode != null)
         {
            this.detachChild(graphicsObjectNode);
         }
         
         graphics = new JMEGraphicsObject(application, assetManager, graphicsObject);
         graphicsObjectNode = graphics.getNode();
         attachChild(graphicsObjectNode);         
      }
      else
      {
         graphics = null;
         graphicsObjectNode = null;
      }
   }
   
   public void update()
   {
      if (graphics3dNode.getHasGraphicsObjectChanged())
      {
         createAndAttachGraphicsObject();
      }
      
      Transform3d transform = graphics3dNode.getTransform();
      transform.get(rotation, translation);
      transform.getScale(scale);
      
      JMEDataTypeUtils.packVecMathTuple3dInJMEVector3f(translation, jmeTranslation);
      JMEDataTypeUtils.packVectMathQuat4dInJMEQuaternion(rotation, jmeRotation);
      JMEDataTypeUtils.packVecMathTuple3dInJMEVector3f(scale, jmeScale);
      
      setLocalRotation(jmeRotation);
      setLocalTranslation(jmeTranslation);
      setLocalScale(jmeScale);
      
   }
   
   public Graphics3DNode getGraphics3DNode()
   {
      return graphics3dNode;
   }

   public static void setupNodeByType(Node jmeNode, Graphics3DNodeType nodeType)
   {
      if(DEBUG)
         System.out.println("JMEGraphics3DNode: setting up Node by type");
      switch (nodeType)
      {
         case TRANSFORM :
            jmeNode.setShadowMode(ShadowMode.Off);
         case ROOTJOINT :
         case JOINT :
            jmeNode.setShadowMode(ShadowMode.CastAndReceive);
            jmeNode.setUserData(JMERayCastOpacity.USER_DATA_FIELD, JMERayCastOpacity.OPAQUE.toString());
   
            break;
   
         case VISUALIZATION :
            jmeNode.setShadowMode(ShadowMode.Off);
            jmeNode.setUserData(JMERayCastOpacity.USER_DATA_FIELD, JMERayCastOpacity.TRANSPARENT.toString());
   
            break;
   
         case GROUND :
            jmeNode.setShadowMode(ShadowMode.CastAndReceive);
            if(DEBUG)
               System.out.println("JMEGraphics3DNode: setupNodeByType: This is a GroundNode: " +jmeNode.getName());
            jmeNode.setUserData(JMERayCastOpacity.USER_DATA_FIELD, JMERayCastOpacity.OPAQUE.toString());
   
            break;
      }
      if(DEBUG)
         System.out.println("JMEGraphics3DNode: node is set with RayCastOpacity: "+jmeNode.getUserData(JMERayCastOpacity.USER_DATA_FIELD));
   }

   public void setType(Graphics3DNodeType nodeType)
   {
      setupNodeByType(this,nodeType);      
   }
}
