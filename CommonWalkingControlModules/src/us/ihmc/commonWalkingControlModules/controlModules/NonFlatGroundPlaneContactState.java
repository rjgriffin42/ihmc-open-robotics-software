package us.ihmc.commonWalkingControlModules.controlModules;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class NonFlatGroundPlaneContactState implements PlaneContactState
{
   private final ArrayList<FramePoint> contactPoints;
   private final ArrayList<FramePoint2d> contactPoints2d;
   private final ReferenceFrame planeFrame;
   private final double coefficientOfFriction;
   private final FrameVector contactNormalFrameVector;

   public NonFlatGroundPlaneContactState(double footLength, double footWidth, Point3d midfootLocation, Vector3d normalToContactPlane,
           double coefficientOfFriction)
   {
      contactPoints = new ArrayList<FramePoint>();
      contactPoints2d = new ArrayList<FramePoint2d>();
      planeFrame = ReferenceFrame.constructReferenceFrameFromPointAndZAxis("planeFrame", new FramePoint(ReferenceFrame.getWorldFrame(), midfootLocation),
              new FrameVector(ReferenceFrame.getWorldFrame(), normalToContactPlane));

      Point3d frontLeft = new Point3d(midfootLocation);
      frontLeft.setX(frontLeft.getX() + footLength / 2.0);
      frontLeft.setY(frontLeft.getY() + footWidth / 2.0);

      Point3d frontRight = new Point3d(midfootLocation);
      frontRight.setX(frontRight.getX() + footLength / 2.0);
      frontRight.setY(frontRight.getY() - footWidth / 2.0);

      Point3d backLeft = new Point3d(midfootLocation);
      backLeft.setX(backLeft.getX() - footLength / 2.0);
      backLeft.setY(backLeft.getY() + footWidth / 2.0);

      Point3d backRight = new Point3d(midfootLocation);
      backRight.setX(backRight.getX() - footLength / 2.0);
      backRight.setY(backRight.getY() - footWidth / 2.0);

      contactPoints.add(new FramePoint(planeFrame, frontLeft));
      contactPoints.add(new FramePoint(planeFrame, frontRight));
      contactPoints.add(new FramePoint(planeFrame, backRight));
      contactPoints.add(new FramePoint(planeFrame, backLeft));

      contactPoints2d.add(new FramePoint2d(planeFrame, projectToXY(frontLeft)));
      contactPoints2d.add(new FramePoint2d(planeFrame, projectToXY(frontRight)));
      contactPoints2d.add(new FramePoint2d(planeFrame, projectToXY(backRight)));
      contactPoints2d.add(new FramePoint2d(planeFrame, projectToXY(backLeft)));

      this.coefficientOfFriction = coefficientOfFriction;
      
      this.contactNormalFrameVector = new FrameVector(planeFrame, 0.0, 0.0, 1.0);
   }

   private Point2d projectToXY(Point3d point)
   {
      return new Point2d(point.getX(), point.getY());
   }

   public List<FramePoint> getContactPoints()
   {
      return contactPoints;
   }

   public ReferenceFrame getBodyFrame()
   {
      return planeFrame;
   }

   public boolean inContact()
   {
      return true;
   }

   public ReferenceFrame getPlaneFrame()
   {
      return planeFrame;
   }

   public List<FramePoint2d> getContactPoints2d()
   {
      return contactPoints2d;
   }

   public double getCoefficientOfFriction()
   {
      return coefficientOfFriction;
   }

   public int getNumberOfContactPoints()
   {
      return contactPoints.size();
   }

   public FrameVector getContactNormalFrameVector()
   {
      return contactNormalFrameVector;
   }
}
