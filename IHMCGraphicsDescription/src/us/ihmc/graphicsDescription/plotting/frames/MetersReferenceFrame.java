package us.ihmc.graphicsDescription.plotting.frames;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

@SuppressWarnings("serial")
public abstract class MetersReferenceFrame extends PlotterReferenceFrame
{   
   public MetersReferenceFrame(String frameName, ReferenceFrame parentFrame, PlotterSpaceConverter spaceConverter)
   {
      super(frameName, parentFrame, PlotterFrameSpace.METERS, spaceConverter);
   }

   public MetersReferenceFrame(String frameName, boolean isWorldFrame, boolean isZupFrame, PlotterSpaceConverter spaceConverter)
   {
      super(frameName, isWorldFrame, isZupFrame, PlotterFrameSpace.METERS, spaceConverter);
   }

   public MetersReferenceFrame(String frameName, ReferenceFrame parentFrame, boolean isWorldFrame, boolean isZupFrame, PlotterSpaceConverter spaceConverter)
   {
      super(frameName, parentFrame, isWorldFrame, isZupFrame, PlotterFrameSpace.METERS, spaceConverter);
   }

   public MetersReferenceFrame(String frameName, ReferenceFrame parentFrame, RigidBodyTransform transformToParent, boolean isWorldFrame,
                               boolean isZupFrame, PlotterSpaceConverter spaceConverter)
   {
      super(frameName, parentFrame, transformToParent, isWorldFrame, isZupFrame, PlotterFrameSpace.METERS, spaceConverter);
   }
   
   public Vector2D getConversionToMeters()
   {
      return getSpaceConverter().getConversionToSpace(PlotterFrameSpace.METERS);
   }
}
