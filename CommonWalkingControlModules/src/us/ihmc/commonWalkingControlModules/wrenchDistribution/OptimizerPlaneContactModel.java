package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactPoint;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

public class OptimizerPlaneContactModel implements OptimizerContactModel
{
	private double rhoMin;
	private static final int VECTORS = 3;
	private static final int MAXPOINTS = 6;
	private int numberOfPointsInContact=MAXPOINTS;
	private static final int MAX_RHO_SIZE = MAXPOINTS * VECTORS;
	private static final double ANGLE_INCREMENT = 2 * Math.PI / ((double) VECTORS);
	private double mu = 0.3;
	private double wRho;
	private final DenseMatrix64F[] rhoQ = new DenseMatrix64F[MAX_RHO_SIZE];
	private SpatialForceVector tempForceVector= new SpatialForceVector();
	private final Matrix3d tempTransformLinearPart = new Matrix3d();
	private final Vector3d tempLinearPart = new Vector3d();
	private final Vector3d tempArm = new Vector3d();
	private final FramePoint tempFramePoint = new FramePoint();
	private final FrameVector tempContactNormalVector = new FrameVector();

	public OptimizerPlaneContactModel()
	{
		for (int i = 0; i < MAXPOINTS; i++)
		{
			for (int j = 0; j < VECTORS; j++)
			{
				int rhoPosition = i * VECTORS + j;
				rhoQ[rhoPosition]= new DenseMatrix64F(6,1);
			}
		}
	}

	public void setup(PlaneContactState plane, double wRho, double rhoMin)
	{
		if (!plane.inContact())
			return;
		
		this.mu = plane.getCoefficientOfFriction();
		numberOfPointsInContact = plane.getNumberOfContactPointsInContact();
		plane.getContactNormalFrameVector(tempContactNormalVector);
		tempContactNormalVector.changeFrame(plane.getPlaneFrame());
		tempContactNormalVector.normalize();
		
		if (numberOfPointsInContact > MAXPOINTS)
		{
			throw new RuntimeException("Unhandled number of contact points: " + numberOfPointsInContact);
		}

		int i = -1;
		
		for (ContactPoint contactPoint : plane.getContactPoints())
		{
			if (!contactPoint.isInContact())
			   continue;
			
			i++;
			
			tempFramePoint.setAndChangeFrame(contactPoint.getPosition());
			tempContactNormalVector.checkReferenceFrameMatch(tempFramePoint.getReferenceFrame());
			
			for (int j = 0; j < VECTORS; j++)
			{
				int rhoPosition = i * VECTORS + j;

				double angle = j * ANGLE_INCREMENT;
				double cosAngleMu = Math.cos(angle) * mu;
				double sinAngleMu = Math.sin(angle) * mu;
				tempContactNormalVector.getVector(tempLinearPart);
				tempLinearPart.normalize();
				tempArm.set(1.0, 1.0, 1.0);
				tempArm.sub(tempLinearPart);
				// Matrix to get from the contactNormalVector the discrete cone of friction
				tempTransformLinearPart.setM00(1.0);
				tempTransformLinearPart.setM01(sinAngleMu);
				tempTransformLinearPart.setM02(cosAngleMu);
				tempTransformLinearPart.setM10(cosAngleMu);
				tempTransformLinearPart.setM11(1.0);
				tempTransformLinearPart.setM12(sinAngleMu);
				tempTransformLinearPart.setM20(sinAngleMu);
				tempTransformLinearPart.setM21(cosAngleMu);
				tempTransformLinearPart.setM22(1.0);
				tempTransformLinearPart.transform(tempLinearPart);
				tempLinearPart.normalize();

				tempArm.set(tempFramePoint.getX() * tempArm.x, tempFramePoint.getY() * tempArm.y, tempFramePoint.getZ() * tempArm.z);
				tempForceVector.setUsingArm(plane.getPlaneFrame(), tempLinearPart, tempArm);
				tempForceVector.changeFrame(plane.getFrameAfterParentJoint());
				
				tempForceVector.packMatrix(rhoQ[rhoPosition]);
			}
		}
      this.wRho = wRho;
      this.rhoMin = rhoMin;
      
//      setupOld(plane, wRho, rhoMin);
	}

   public void setupOld(PlaneContactState plane, double wRho, double rhoMin)
   {
      this.mu = plane.getCoefficientOfFriction();
      numberOfPointsInContact = plane.getNumberOfContactPointsInContact();
      plane.getContactNormalFrameVector(tempContactNormalVector);
      ReferenceFrame contactFrame = tempContactNormalVector.getReferenceFrame();

      if (numberOfPointsInContact > MAXPOINTS)
      {
         throw new RuntimeException("Unhandled number of contact points: " + numberOfPointsInContact);
      }

      int i = -1;

      for (ContactPoint contactPoint : plane.getContactPoints())
      {
         if (!contactPoint.isInContact())
            continue;

         i++;

         tempFramePoint.setAndChangeFrame(contactPoint.getPosition());
         tempFramePoint.changeFrame(contactFrame);

         for (int j = 0; j < VECTORS; j++)
         {
            int rhoPosition = i * VECTORS + j;

            double angle = j*ANGLE_INCREMENT;
            tempLinearPart.set(Math.cos(angle)*mu, Math.sin(angle)*mu, 1);
            tempLinearPart.normalize();

            tempArm.set(tempFramePoint.getX(), tempFramePoint.getY(), 0.0);
            tempForceVector.setUsingArm(tempFramePoint.getReferenceFrame(), tempLinearPart, tempArm);            
            tempForceVector.changeFrame(plane.getFrameAfterParentJoint());

            tempForceVector.packMatrix(rhoQ[rhoPosition]);
         }
      }
      this.wRho = wRho;
      this.rhoMin = rhoMin;
   }
   
	@Deprecated
	public void setup(double coefficientOfFriction, List<FramePoint> contactPoints, FrameVector normalContactVector, ReferenceFrame endEffectorFrame, double wRho, double rhoMin)
	{
		this.mu = coefficientOfFriction;
		numberOfPointsInContact = contactPoints.size();
		ReferenceFrame contactFrame = normalContactVector.getReferenceFrame();

		if (numberOfPointsInContact > MAXPOINTS)
		{
			throw new RuntimeException("Unhandled number of contact points: " + numberOfPointsInContact);
		}

		for (int i = 0; i < numberOfPointsInContact; i++)
		{
		   tempFramePoint.setAndChangeFrame(contactPoints.get(i));
		   tempFramePoint.changeFrame(contactFrame);
			
			for (int j = 0; j < VECTORS; j++)
			{
				int rhoPosition = i * VECTORS + j;

				double angle = j * ANGLE_INCREMENT;
				double cosAngleMu = Math.cos(angle) * mu;
				double sinAngleMu = Math.sin(angle) * mu;
				normalContactVector.getVector(tempLinearPart);
				tempLinearPart.normalize();
				tempArm.set(1.0, 1.0, 1.0);
				tempArm.sub(tempLinearPart);
				// Matrix to get from the contactNormalVector the discrete cone of friction
				tempTransformLinearPart.setM00(1.0);
				tempTransformLinearPart.setM01(sinAngleMu);
				tempTransformLinearPart.setM02(cosAngleMu);
				tempTransformLinearPart.setM10(cosAngleMu);
				tempTransformLinearPart.setM11(1.0);
				tempTransformLinearPart.setM12(sinAngleMu);
				tempTransformLinearPart.setM20(sinAngleMu);
				tempTransformLinearPart.setM21(cosAngleMu);
				tempTransformLinearPart.setM22(1.0);
				tempTransformLinearPart.transform(tempLinearPart);
				tempLinearPart.normalize();

				tempArm.set(tempFramePoint.getX() * tempArm.x, tempFramePoint.getY() * tempArm.y, tempFramePoint.getZ() * tempArm.z);
				tempForceVector.setUsingArm(contactFrame, tempLinearPart, tempArm);
				tempForceVector.changeFrame(endEffectorFrame);

				tempForceVector.packMatrix(rhoQ[rhoPosition]);
			}
		}
		this.wRho = wRho;
		this.rhoMin = rhoMin;
	}

	public int getRhoSize()
	{
		return VECTORS*numberOfPointsInContact;
	}

	public int getPhiSize()
	{
		return 0;
	}

	public double getRhoMin(int i)
	{
		return rhoMin;
	}

	public double getPhiMin(int i)
	{
		return 0;
	}

	public double getPhiMax(int i)
	{
		return 0;
	}

	public void packQRhoBodyFrame(int i, SpatialForceVector spatialForceVector, ReferenceFrame referenceFrame)
	{
		spatialForceVector.set(referenceFrame, rhoQ[i]);
	}

	public void packQPhiBodyFrame(int i, SpatialForceVector spatialForceVector, ReferenceFrame referenceFrame)
	{
	}

	public double getWPhi()
	{
		return Double.NaN;
	}

	public double getWRho()
	{
		return wRho;
	}

}
