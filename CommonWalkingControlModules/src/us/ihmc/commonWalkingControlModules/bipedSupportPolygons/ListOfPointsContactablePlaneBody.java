package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;

import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

public class ListOfPointsContactablePlaneBody implements ContactablePlaneBody
{
   private final RigidBody rigidBody;
   private final ReferenceFrame soleFrame;
   private final List<Point2d> contactPoints = new ArrayList<Point2d>();
   private final int totalNumberOfContactPoints;

   public ListOfPointsContactablePlaneBody(RigidBody rigidBody, ReferenceFrame soleFrame, List<Point2d> contactPointsInSoleFrame)
   {
      this.rigidBody = rigidBody;
      this.soleFrame = soleFrame;
      this.contactPoints.addAll(contactPointsInSoleFrame);
      
      totalNumberOfContactPoints = contactPoints.size();
   }

   public RigidBody getRigidBody()
   {
      return rigidBody;
   }
   
   public String getName()
   {
      return rigidBody.getName();
   }

   public List<FramePoint> getContactPoints()
   {
      List<FramePoint> ret = new ArrayList<FramePoint>(contactPoints.size());
      for (int i = 0; i < contactPoints.size(); i++)
      {
         Tuple2d point = contactPoints.get(i);
         ret.add(new FramePoint(soleFrame, point.getX(), point.getY(), 0.0));
      }
   
      return ret;
   }

   public ReferenceFrame getBodyFrame()
   {
      return rigidBody.getParentJoint().getFrameAfterJoint();
   }

   public FrameConvexPolygon2d getContactPolygonCopy()
   {
      return new FrameConvexPolygon2d(soleFrame, contactPoints);
   }

   public ReferenceFrame getPlaneFrame()
   {
      return soleFrame;
   }

   public List<FramePoint2d> getContactPoints2d()
   {
      List<FramePoint2d> ret = new ArrayList<FramePoint2d>(contactPoints.size());
      for (int i = 0; i < contactPoints.size(); i++)
      {
         Tuple2d point = contactPoints.get(i);
         ret.add(new FramePoint2d(soleFrame, point));
      }
   
      return ret;
   }

   public boolean inContact()
   {
      throw new RuntimeException();
   }

   public int getTotalNumberOfContactPoints()
   {
      return totalNumberOfContactPoints;
   }

}