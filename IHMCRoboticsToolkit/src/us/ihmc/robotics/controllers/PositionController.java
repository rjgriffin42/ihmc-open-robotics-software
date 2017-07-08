package us.ihmc.robotics.controllers;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector3D;

/**
 * @author twan
 *         Date: 6/15/13
 */
public interface PositionController
{
   public abstract void compute(FrameVector3D output, FramePoint desiredPosition, FrameVector3D desiredVelocity, FrameVector3D currentVelocity,
                FrameVector3D feedForward);

   public abstract ReferenceFrame getBodyFrame();
}
