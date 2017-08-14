package us.ihmc.robotics.math.frames;

import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePoint2D;
import us.ihmc.robotics.geometry.interfaces.PointInterface;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

//Note: You should only make these once at the initialization of a controller. You shouldn't make any on the fly
//since they contain YoVariables.
public class YoFramePoint extends YoFrameTuple<YoFramePoint, FramePoint3D> implements PointInterface
{
   public YoFramePoint(String namePrefix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      this(namePrefix, "", frame, registry);
   }
   
   public YoFramePoint(String namePrefix, String nameSuffix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      super(namePrefix, nameSuffix, frame, registry);
   }

   public YoFramePoint(YoDouble xVariable, YoDouble yVariable, YoDouble zVariable, ReferenceFrame frame)
   {
      super(xVariable, yVariable, zVariable, frame);
   }

   protected FramePoint3D createEmptyFrameTuple()
   {
      return new FramePoint3D();
   }

   public double distance(FramePoint3D framePoint)
   {
      return getFrameTuple().distance(framePoint);
   }

   public double distance(YoFramePoint yoFramePoint)
   {
      return distance(yoFramePoint.getFrameTuple());
   }

   public double getXYPlaneDistance(FramePoint3D framePoint)
   {
      return getFrameTuple().getXYPlaneDistance(framePoint);
   }

   public double getXYPlaneDistance(FramePoint2D framePoint2d)
   {
      return getFrameTuple().getXYPlaneDistance(framePoint2d);
   }

   public double getXYPlaneDistance(YoFramePoint yoFramePoint)
   {
      return getXYPlaneDistance(yoFramePoint.getFrameTuple());
   }

   public double getXYPlaneDistance(YoFramePoint2d yoFramePoint2d)
   {
      return getXYPlaneDistance(yoFramePoint2d.getFrameTuple2d());
   }

   @Override
   public void getPoint(Point3D pointToPack)
   {
      this.get(pointToPack);
   }

   private final Point3D tempPoint = new Point3D();
   
   @Override
   public void setPoint(PointInterface pointInterface)
   {
      pointInterface.getPoint(tempPoint);
      this.set(tempPoint);
   }

   @Override
   public void setPoint(Point3D point)
   {
      this.set(point);
   }

   /**
    * Sets this point to the location of the origin of passed in referenceFrame.
    */
   @Override
   public void setFromReferenceFrame(ReferenceFrame referenceFrame)
   {
      super.setFromReferenceFrame(referenceFrame);
   }
}
