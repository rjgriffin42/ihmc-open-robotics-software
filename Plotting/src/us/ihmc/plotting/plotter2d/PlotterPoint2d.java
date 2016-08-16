package us.ihmc.plotting.plotter2d;

import javax.vecmath.Tuple2d;

import us.ihmc.plotting.plotter2d.frames.MetersReferenceFrame;
import us.ihmc.plotting.plotter2d.frames.PixelsReferenceFrame;
import us.ihmc.plotting.plotter2d.frames.PlotterReferenceFrame;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

@SuppressWarnings("serial")
public class PlotterPoint2d extends FramePoint2d
{
   public PlotterPoint2d(PlotterPoint2d frameTuple2d)
   {
      super(frameTuple2d);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, double x, double y, String name)
   {
      super(referenceFrame, x, y, name);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, double x, double y)
   {
      super(referenceFrame, x, y);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, double[] position, String name)
   {
      super(referenceFrame, position, name);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, double[] position)
   {
      super(referenceFrame, position);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, String name)
   {
      super(referenceFrame, name);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, Tuple2d position, String name)
   {
      super(referenceFrame, position, name);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame, Tuple2d position)
   {
      super(referenceFrame, position);
   }

   public PlotterPoint2d(PlotterReferenceFrame referenceFrame)
   {
      super(referenceFrame);
   }
   
   public void changeFrame(MetersReferenceFrame metersReferenceFrame)
   {
      if (getReferenceFrame() instanceof PixelsReferenceFrame)
      {
         changeFrame(ReferenceFrame.getWorldFrame());
         scale(metersReferenceFrame.getConversionToMeters());
      }
      
      super.changeFrame(metersReferenceFrame);
   }

   public void changeFrame(PixelsReferenceFrame pixelsReferenceFrame)
   {
      if (getReferenceFrame() instanceof MetersReferenceFrame)
      {
         changeFrame(ReferenceFrame.getWorldFrame());
         scale(pixelsReferenceFrame.getConversionToPixels());
      }
      
      super.changeFrame(pixelsReferenceFrame);
   }
}
