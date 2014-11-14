package us.ihmc.simulationconstructionset;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.utilities.Axis;
import us.ihmc.utilities.math.RotationalInertiaCalculator;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBodyInertia;

public class LinkSolidCylinder extends Link
{
	
	/**
	 * A solid cylindrical link with automatically generated mass and moment of inertia properties
	 */
	private static final long serialVersionUID = 6789282991137530985L;
	protected final ReferenceFrame world = ReferenceFrame.getWorldFrame();
	protected final ReferenceFrame cylinderReferenceFrame;


	public LinkSolidCylinder(String name, Vector3d cylinderZAxisInWorld, double mass, double length, double radius, AppearanceDefinition color)
	{
		this(name, cylinderZAxisInWorld, mass, length, radius, attachParentJointToDistalEndOfCylinder(cylinderZAxisInWorld, length), color);
	}
	
	
	private static Vector3d attachParentJointToDistalEndOfCylinder(Vector3d cylinderZAxisInWorld, double length)
	{
		Vector3d parentJointOffsetFromCoM = new Vector3d(cylinderZAxisInWorld);
		parentJointOffsetFromCoM.normalize();
		parentJointOffsetFromCoM.scale(-length / 2.0);
		
		return parentJointOffsetFromCoM;
	}
	
	public LinkSolidCylinder(String name, Vector3d cylinderZAxisInWorld, double mass, double length, double radius, Vector3d parentJointOffsetFromCoM, AppearanceDefinition color) 
	{
		super(name);
		
		FrameVector cylinderZAxisExpressedInWorld = new FrameVector(world, cylinderZAxisInWorld);
		this.cylinderReferenceFrame = ReferenceFrame.constructReferenceFrameFromPointAndZAxis(name, new FramePoint(world), cylinderZAxisExpressedInWorld);

      physics.comOffset.set(parentJointOffsetFromCoM);


			Matrix3d moiInLegFrame = RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidCylinder(mass, radius, length, Axis.Z);
			
			Boolean computeMoiInWorldInternally = false;
			
			if (computeMoiInWorldInternally)
			{
				FrameVector linkXAxis = new FrameVector(cylinderReferenceFrame, 1.0, 0.0, 0.0);
				FrameVector linkYAxis = new FrameVector(cylinderReferenceFrame, 0.0, 1.0, 0.0);
				FrameVector linkZAxis = new FrameVector(cylinderReferenceFrame, 0.0, 0.0, 1.0);

				linkXAxis.changeFrame(world);
				linkYAxis.changeFrame(world);
				linkZAxis.changeFrame(world);

				double[] eigenvectors = new double[]
				                                   { linkXAxis.getX(),  linkYAxis.getX(), linkZAxis.getX(), 
						linkXAxis.getY(),   linkYAxis.getY(), linkZAxis.getY(), 
						linkXAxis.getZ(),   linkYAxis.getZ(), linkZAxis.getZ() };

				Matrix3d Q = new Matrix3d(eigenvectors);

				Matrix3d moiInWorldFrame = new Matrix3d();

				moiInLegFrame.mulTransposeRight(moiInLegFrame, Q);
				moiInWorldFrame.mul(Q, moiInLegFrame);

				this.setMass(mass);
            setMomentOfInertia(moiInWorldFrame);
            setComOffset(physics.comOffset);
			}
			else
			{
				RigidBodyInertia inertia = new RigidBodyInertia(cylinderReferenceFrame, moiInLegFrame, mass);
				inertia.changeFrame(ReferenceFrame.getWorldFrame());

				this.setMass(mass);
            setMomentOfInertia(inertia.getMassMomentOfInertiaPartCopy());
            setComOffset(physics.comOffset);
			}

			this.addCoordinateSystemToCOM(length/10.0);
			this.addEllipsoidFromMassProperties2(color);
		}

	}
