package us.ihmc.robotics.math.frames;


import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FramePoint2D;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

//Note: You should only make these once at the initialization of a controller. You shouldn't make any on the fly
//since they contain YoVariables.
public class YoFramePoint2d extends YoFrameTuple2d<YoFramePoint2d, FramePoint2D>
{
   public YoFramePoint2d(String namePrefix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      this(namePrefix, "", frame, registry);
   }

   public YoFramePoint2d(String namePrefix, String nameSuffix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      super(namePrefix, nameSuffix, frame, registry);
   }

   public YoFramePoint2d(YoDouble xVariable, YoDouble yVariable, ReferenceFrame frame)
   {
      super(xVariable, yVariable, frame);
   }

   protected FramePoint2D createEmptyFrameTuple2d()
   {
      return new FramePoint2D();
   }
   
   public double distance(FramePoint2D framePoint2d)
   {
      return getFrameTuple2d().distance(framePoint2d);
   }
}
