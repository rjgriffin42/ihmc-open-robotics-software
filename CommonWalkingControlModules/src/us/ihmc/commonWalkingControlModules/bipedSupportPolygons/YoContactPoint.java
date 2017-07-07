package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePoint;

public class YoContactPoint implements ContactPointInterface
{
   private final YoVariableRegistry registry;
   private final YoFramePoint yoPosition;
   private final YoBoolean isInContact;
   private final String namePrefix;
   private final PlaneContactState parentContactState;

   public YoContactPoint(String namePrefix, int index, FramePoint2d contactPointPosition2d, PlaneContactState parentContactState,
         YoVariableRegistry parentRegistry)
   {
      this(namePrefix, index, contactPointPosition2d.getReferenceFrame(), parentContactState, parentRegistry);
      setPosition(contactPointPosition2d);
   }

   public YoContactPoint(String namePrefix, int index, FramePoint contactPointPosition, PlaneContactState parentContactState, YoVariableRegistry parentRegistry)
   {
      this(namePrefix, index, contactPointPosition.getReferenceFrame(), parentContactState, parentRegistry);
      setPosition(contactPointPosition);
   }

   public YoContactPoint(String namePrefix, int index, ReferenceFrame pointFrame, PlaneContactState parentContactState, YoVariableRegistry parentRegistry)
   {
      this.parentContactState = parentContactState;
      this.namePrefix = namePrefix;

      //TODO: Check if it is better to create an actual child registry
      registry = parentRegistry;

      yoPosition = new YoFramePoint(namePrefix + "Contact" + index, pointFrame, registry);
      isInContact = new YoBoolean(namePrefix + "InContact" + index, registry);
   }

   @Override
   public boolean isInContact()
   {
      return isInContact.getBooleanValue();
   }

   @Override
   public void setInContact(boolean inContact)
   {
      isInContact.set(inContact);
   }

   @Override
   public void getPosition2d(FramePoint2d framePoint2dToPack)
   {
      yoPosition.getFrameTuple2dIncludingFrame(framePoint2dToPack);
   }

   @Override
   public void getPosition(FramePoint framePointToPack)
   {
      yoPosition.getFrameTupleIncludingFrame(framePointToPack);
   }

   @Override
   public void getPosition2d(Point2D position2d)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setPosition(FramePoint position)
   {
      this.yoPosition.set(position);
   }

   @Override
   public void setPosition2d(FramePoint2d position2d)
   {
      yoPosition.set(position2d);
   }

   public void setPosition2d(Point2D contactPointLocation)
   {
      yoPosition.set(contactPointLocation);
   }

   public void setPosition(FramePoint2d contactPointLocation)
   {
      yoPosition.set(contactPointLocation);
   }

   @Override
   public PlaneContactState getParentContactState()
   {
      return parentContactState;
   }

   public boolean epsilonEquals(FramePoint2d contactPointPosition2d, double threshold)
   {
      yoPosition.checkReferenceFrameMatch(contactPointPosition2d);
      if (!MathTools.epsilonEquals(yoPosition.getX(), contactPointPosition2d.getX(), threshold))
         return false;
      if (!MathTools.epsilonEquals(yoPosition.getY(), contactPointPosition2d.getY(), threshold))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return namePrefix + ", in contact: " + isInContact() + ", position: " + yoPosition.toString();
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return yoPosition.getReferenceFrame();
   }
}
