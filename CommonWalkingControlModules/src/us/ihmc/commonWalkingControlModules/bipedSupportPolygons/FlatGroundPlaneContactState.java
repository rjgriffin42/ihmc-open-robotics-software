package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

public class FlatGroundPlaneContactState implements PlaneContactState
{
   private final RigidBody rigidBody;
   private final ArrayList<FramePoint> contactFramePoints;
   private final ArrayList<FramePoint2d> contactFramePoints2d;
   private final List<ContactPoint> contactPoints;
   private final double coefficientOfFriction;
   private final FrameVector contactNormalFrameVector;
   private final int totalNumberOfContactPoints;
   private boolean inContact;

   public static FlatGroundPlaneContactState createRandomFlatGroundContactState(Random random, boolean leftSide, double coefficientOfFriction, RigidBody rigidBody)
   {
      double footLength = RandomTools.generateRandomDouble(random, 0.1, 0.3);
      double footWidth = RandomTools.generateRandomDouble(random, 0.1, 0.2);

      Point3d midfootLocation = RandomTools.generateRandomPoint(random, -1.0, 0.3, 0.0, 1.0, 1.0, 0.0);
      midfootLocation.setZ(0.0);

      if (!leftSide)
         midfootLocation.setY(-midfootLocation.getY());

      FlatGroundPlaneContactState flatGroundPlaneContactState = new FlatGroundPlaneContactState(footLength, footWidth, midfootLocation, coefficientOfFriction, rigidBody);
      
      return flatGroundPlaneContactState;
   }

   public FlatGroundPlaneContactState(double[][] contactPointLocations, double coefficientOfFriction, RigidBody rigidBody)
   {
      this.rigidBody = rigidBody;
      
      contactFramePoints = new ArrayList<FramePoint>();
      contactFramePoints2d = new ArrayList<FramePoint2d>();
      
      for (double[] contactPointLocation : contactPointLocations)
      {
         contactFramePoints.add(new FramePoint(ReferenceFrame.getWorldFrame(), contactPointLocation[0], contactPointLocation[1], 0.0));
         contactFramePoints2d.add(new FramePoint2d(ReferenceFrame.getWorldFrame(), contactPointLocation[0], contactPointLocation[1]));
      }
      this.coefficientOfFriction = coefficientOfFriction;
      this.contactNormalFrameVector = new FrameVector(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 1.0);
      
      contactPoints = new ArrayList<ContactPoint>();
      for (int i = 0; i < contactFramePoints2d.size(); i++)
      {
         ContactPoint contactPoint = new ContactPoint(contactFramePoints2d.get(i), this);
         contactPoint.setInContact(true);
         contactPoints.add(contactPoint);
      }
      
      inContact = true;
      
      totalNumberOfContactPoints = contactPoints.size();
   }

   public FlatGroundPlaneContactState(double footLength, double footWidth, Point3d midfootLocation, double coefficientOfFriction, RigidBody rigidBody)
   {
      this.rigidBody = rigidBody;
      
      contactFramePoints = new ArrayList<FramePoint>();
      contactFramePoints2d = new ArrayList<FramePoint2d>();

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

      contactFramePoints.add(new FramePoint(ReferenceFrame.getWorldFrame(), frontLeft));
      contactFramePoints.add(new FramePoint(ReferenceFrame.getWorldFrame(), frontRight));
      contactFramePoints.add(new FramePoint(ReferenceFrame.getWorldFrame(), backRight));
      contactFramePoints.add(new FramePoint(ReferenceFrame.getWorldFrame(), backLeft));

      contactFramePoints2d.add(new FramePoint2d(ReferenceFrame.getWorldFrame(), projectToXY(frontLeft)));
      contactFramePoints2d.add(new FramePoint2d(ReferenceFrame.getWorldFrame(), projectToXY(frontRight)));
      contactFramePoints2d.add(new FramePoint2d(ReferenceFrame.getWorldFrame(), projectToXY(backRight)));
      contactFramePoints2d.add(new FramePoint2d(ReferenceFrame.getWorldFrame(), projectToXY(backLeft)));
      
      this.coefficientOfFriction = coefficientOfFriction;
      
      this.contactNormalFrameVector = new FrameVector(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 1.0);
      
      contactPoints = new ArrayList<ContactPoint>();
      for (int i = 0; i < contactFramePoints2d.size(); i++)
      {
         ContactPoint contactPoint = new ContactPoint(contactFramePoints2d.get(i), this);
         contactPoint.setInContact(true);
         contactPoints.add(contactPoint);
      }
      
      inContact = true;
      
      totalNumberOfContactPoints = contactPoints.size();
   }

   private Point2d projectToXY(Point3d point)
   {
      return new Point2d(point.getX(), point.getY());
   }

   public boolean inContact()
   {
      return inContact;
   }

   public void setFullyConstrained()
   {
      for (int i = 0; i < totalNumberOfContactPoints; i++)
      {
         contactPoints.get(i).setInContact(true);
      }
      
      inContact = true;
   }

   public List<FramePoint> getContactFramePointsInContactCopy()
   {
      return contactFramePoints;
   }

   public List<FramePoint2d> getContactFramePoints2dInContactCopy()
   {
      return contactFramePoints2d;
   }

   public ReferenceFrame getFrameAfterParentJoint()
   {
      return ReferenceFrame.getWorldFrame();
   }

   public ReferenceFrame getPlaneFrame()
   {
      return ReferenceFrame.getWorldFrame();
   }


   public String toString()
   {
      return contactFramePoints2d.toString();
   }

   public double getCoefficientOfFriction()
   {
      return coefficientOfFriction;
   }

   public int getNumberOfContactPointsInContact()
   {
      return contactFramePoints2d.size();
   }

   public FrameVector getContactNormalFrameVectorCopy()
   {
      return new FrameVector(contactNormalFrameVector);
   }
   
   public void getContactNormalFrameVector(FrameVector frameVectorToPack)
   {
	   frameVectorToPack.setAndChangeFrame(contactNormalFrameVector);
   }

   public void setRhoContactRegularization(double wRho)
   {
   }

   public double getRhoContactRegularization()
   {
      return DEFAULT_WRHO;
   }

   public void resetContactRegularization()
   {
   }

   public List<ContactPoint> getContactPoints()
   {
      return contactPoints;
   }

   public int getTotalNumberOfContactPoints()
   {
      return totalNumberOfContactPoints;
   }

   public RigidBody getRigidBody()
   {
      return rigidBody;
   }
}
