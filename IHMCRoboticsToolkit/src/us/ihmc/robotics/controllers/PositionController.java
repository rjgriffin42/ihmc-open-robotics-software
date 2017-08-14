package us.ihmc.robotics.controllers;

import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

/**
 * @author twan
 *         Date: 6/15/13
 */
public interface PositionController
{
   public abstract void compute(FrameVector3D output, FramePoint3D desiredPosition, FrameVector3D desiredVelocity, FrameVector3D currentVelocity,
                FrameVector3D feedForward);

   public abstract ReferenceFrame getBodyFrame();
}
