package us.ihmc.robotics.referenceFrames;

import us.ihmc.euclid.referenceFrame.FrameTuple3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;

public class TranslationReferenceFrame extends ReferenceFrame
{
   private static final long serialVersionUID = -6741627181585210414L;
   public final FrameVector3D originVector;

   public TranslationReferenceFrame(String frameName, ReferenceFrame parentFrame)
   {
      super(frameName, parentFrame, false, parentFrame.isZupFrame());

      originVector = new FrameVector3D(parentFrame);
   }

   @Override
   protected void updateTransformToParent(RigidBodyTransform transformToParent)
   {      
      transformToParent.setIdentity();
      transformToParent.setTranslation(originVector.getVector());
   }

   public void updateTranslation(FrameTuple3D<?, ?> frameVector)
   {      
      originVector.set(frameVector);
      this.update();
   }
   
   public void updateTranslation(Tuple3DBasics translation)
   {      
      originVector.set(translation);
      this.update();
   }
}  
