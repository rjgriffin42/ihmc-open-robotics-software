package us.ihmc.commonWalkingControlModules.calibration.virtualChain;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.screwTheory.SixDoFJointReferenceFrame;

import com.yobotics.simulationconstructionset.FloatingJoint;

public class VirtualLinkFromFloatingJoint extends VirtualLinkFromJoint
{
   public VirtualLinkFromFloatingJoint(FloatingJoint joint)
   {
      super(joint);
   }

   private final Quat4d rotation = new Quat4d();
   private final Vector3d translation = new Vector3d();
   
   public void updateReferenceFrameFromJointAngle()
   {
      SixDoFJointReferenceFrame referenceFrame = (SixDoFJointReferenceFrame) virtualLinkFrameVector.getReferenceFrame();

      if (this.referenceFrame != referenceFrame)
      {
         throw new RuntimeException("(this.referenceFrame != referenceFrame)");
      }
      FloatingJoint floatingJoint = (FloatingJoint) joint;

      floatingJoint.getTransformFromWorld(rotation, translation);
      referenceFrame.setRotation(rotation);
      referenceFrame.setTranslation(translation);
      referenceFrame.update();
   }
}

