package us.ihmc.robotics.math.frames;

import org.apache.commons.lang3.StringUtils;

import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameTuple;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class YoSpatialVector
{
   protected final String namePrefix;
   protected final String nameSuffix;

   /** This is where the data is stored. All operations must act on these numbers. */
   protected final YoFrameVector linearPart;
   protected final YoFrameVector angularPart;
   /** Redundant but allows to make sure the frame isn't changed. */
   protected final ReferenceFrame expressedInFrame;

   public YoSpatialVector(String namePrefix, String nameSuffix, ReferenceFrame expressedInFrame, YoVariableRegistry registry)
   {
      this.namePrefix = namePrefix;
      this.nameSuffix = nameSuffix;

      linearPart = new YoFrameVector(namePrefix + "Linear", nameSuffix, expressedInFrame, registry);
      angularPart = new YoFrameVector(namePrefix + "Angular", nameSuffix, expressedInFrame, registry);
      this.expressedInFrame = expressedInFrame;
   }

   public YoSpatialVector(String namePrefix, ReferenceFrame expressedInFrame, YoVariableRegistry registry)
   {
      this(namePrefix, "", expressedInFrame, registry);
   }

   public YoSpatialVector(YoFrameVector yoLinearPart, YoFrameVector yoAngularPart)
   {
      yoLinearPart.checkReferenceFrameMatch(yoAngularPart);

      this.namePrefix = StringUtils.getCommonPrefix(yoLinearPart.getNamePrefix(), yoAngularPart.getNamePrefix());
      this.nameSuffix = YoFrameVariableNameTools.getCommonSuffix(yoLinearPart.getNameSuffix(), yoAngularPart.getNameSuffix());

      this.linearPart = yoLinearPart;
      this.angularPart = yoAngularPart;
      this.expressedInFrame = yoLinearPart.getReferenceFrame();
   }

   public void set(YoSpatialVector yoSpacialVector)
   {
      linearPart.set(yoSpacialVector.linearPart);
      angularPart.set(yoSpacialVector.angularPart);
   }

   public void setLinearPart(Vector3DReadOnly vector3d)
   {
      linearPart.set(vector3d);
   }

   public void setLinearPart(FrameVector frameVector)
   {
      linearPart.set(frameVector);
   }

   public void setLinearPart(YoFrameVector yoFrameVector)
   {
      linearPart.set(yoFrameVector);
   }

   public void setAngularPart(Vector3DReadOnly vector3d)
   {
      angularPart.set(vector3d);
   }

   public void setAngularPart(FrameVector frameVector)
   {
      angularPart.set(frameVector);
   }

   public void setAngularPart(YoFrameVector yoFrameVector)
   {
      angularPart.set(yoFrameVector);
   }

   public void set(Vector3DReadOnly linearPart, Vector3DReadOnly angularPart)
   {
      setLinearPart(linearPart);
      setAngularPart(angularPart);
   }

   public void set(FrameVector linearPart, FrameVector angularPart)
   {
      setLinearPart(linearPart);
      setAngularPart(angularPart);
   }

   public void set(YoFrameVector linearPart, YoFrameVector angularPart)
   {
      setLinearPart(linearPart);
      setAngularPart(angularPart);
   }

   public void scale(double scaleFactor)
   {
      scaleLinearPart(scaleFactor);
      scaleAngularPart(scaleFactor);
   }

   public void scaleLinearPart(double scaleFactor)
   {
      linearPart.scale(scaleFactor);
   }

   public void scaleAngularPart(double scaleFactor)
   {
      angularPart.scale(scaleFactor);
   }

   public void setToZero()
   {
      linearPart.setToZero();
      angularPart.setToZero();
   }

   public YoFrameVector getYoLinearPart()
   {
      return linearPart;
   }

   public YoFrameVector getYoAngularPart()
   {
      return angularPart;
   }

   public FrameVector getLinearPart()
   {
      return linearPart.getFrameTuple();
   }

   public FrameVector getAngularPart()
   {
      return angularPart.getFrameTuple();
   }

   public void setAndMatchFrameLinearPart(FrameTuple<?, ?> frameVector)
   {
      linearPart.setAndMatchFrame(frameVector);
   }

   public void setAndMatchFrameAngularPart(FrameTuple<?, ?> frameVector)
   {
      angularPart.setAndMatchFrame(frameVector);
   }

   public void setAndMatchFrame(FrameTuple<?, ?> linearPart, FrameTuple<?, ?> angularPart)
   {
      setAndMatchFrameLinearPart(linearPart);
      setAndMatchFrameAngularPart(angularPart);
   }

   public double getLinearPartX()
   {
      return linearPart.getX();
   }

   public double getLinearPartY()
   {
      return linearPart.getY();
   }

   public double getLinearPartZ()
   {
      return linearPart.getZ();
   }

   public double getAngularPartX()
   {
      return angularPart.getX();
   }

   public double getAngularPartY()
   {
      return angularPart.getY();
   }

   public double getAngularPartZ()
   {
      return angularPart.getZ();
   }

   public void getLinearPart(Vector3DBasics linearPartToPack)
   {
      this.linearPart.get(linearPartToPack);
   }

   public void getLinearPart(FrameVector linearPartToPack)
   {
      this.linearPart.getFrameTuple(linearPartToPack);
   }

   public void getLinearPartIncludingFrame(FrameVector linearPartToPack)
   {
      this.linearPart.getFrameTupleIncludingFrame(linearPartToPack);
   }

   public void getLinearPart(YoFrameVector linearPartToPack)
   {
      linearPartToPack.set(this.linearPart);
   }

   public void getAngularPart(Vector3DBasics angularPartToPack)
   {
      this.angularPart.get(angularPartToPack);
   }

   public void getAngularPart(FrameVector angularPartToPack)
   {
      this.angularPart.getFrameTuple(angularPartToPack);
   }

   public void getAngularPartIncludingFrame(FrameVector angularPartToPack)
   {
      this.angularPart.getFrameTupleIncludingFrame(angularPartToPack);
   }

   public void getAngularPart(YoFrameVector angularPartToPack)
   {
      angularPartToPack.set(this.angularPart);
   }

   public void get(Vector3DBasics linearPartToPack, Vector3DBasics angularPartToPack)
   {
      getLinearPart(linearPartToPack);
      getAngularPart(angularPartToPack);
   }

   public void get(FrameVector linearPartToPack, FrameVector angularPartToPack)
   {
      getLinearPart(linearPartToPack);
      getAngularPart(angularPartToPack);
   }

   public void getIncludingFrame(FrameVector linearPartToPack, FrameVector angularPartToPack)
   {
      getLinearPartIncludingFrame(linearPartToPack);
      getAngularPartIncludingFrame(angularPartToPack);
   }

   public void get(YoFrameVector linearPartToPack, YoFrameVector angularPartToPack)
   {
      getLinearPart(linearPartToPack);
      getAngularPart(angularPartToPack);
   }

   public ReferenceFrame getExpressedInFrame()
   {
      return expressedInFrame;
   }

   public String getNamePrefix()
   {
      return namePrefix;
   }

   public String getNameSuffix()
   {
      return nameSuffix;
   }
}
