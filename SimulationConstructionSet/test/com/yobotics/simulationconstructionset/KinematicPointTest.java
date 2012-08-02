package com.yobotics.simulationconstructionset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Before;
import org.junit.Test;


public class KinematicPointTest 
{
	Vector3d offset;
	Robot robot;
	KinematicPoint kinematicPoint;

	@Before
	public void setUp()
	{
		offset = new Vector3d(1.0, 2.0, 3.0);
		robot = new Robot("testRobot");
		kinematicPoint = new KinematicPoint("testPoint", offset, robot);
		
	}
	 
	@Test
	public void testGetAndSetParentJoint() 
	{
		PinJoint joint = new PinJoint("joint", new Vector3d(0.0, 0.0, 0.0), robot, Joint.X);
		robot.addRootJoint(joint);
		kinematicPoint.setParentJoint(joint);
		assertTrue(joint == kinematicPoint.getParentJoint());
	}
	
	@Test
	public void testToString()
	{
		assertEquals("name: testPoint x: 0.0, y: 0.0, z: 0.0", kinematicPoint.toString());
	}
	
	@Test
	public void testSetOffsetJointWithBothVectorAndXYAndZValuesAsParameters()
	{
		assertTrue(1.0 == kinematicPoint.getOffset().getX());
		assertTrue(2.0 == kinematicPoint.getOffset().getY());
		assertTrue(3.0 == kinematicPoint.getOffset().getZ());

		kinematicPoint.setOffsetJoint(3.0, 4.0, 7.0);
		assertTrue(3.0 == kinematicPoint.getOffset().getX());
		assertTrue(4.0 == kinematicPoint.getOffset().getY());
		assertTrue(7.0 == kinematicPoint.getOffset().getZ());
		
		Vector3d vectorTest = new Vector3d(9.0, 1.0, 5.0);
		kinematicPoint.setOffsetJoint(vectorTest);
		assertTrue(9.0 == kinematicPoint.getOffset().getX());
		assertTrue(1.0 == kinematicPoint.getOffset().getY());
		assertTrue(5.0 == kinematicPoint.getOffset().getZ());
	}
	
//	@Test
//	public void testSetOffsetWorld()
//	{
//		kinematicPoint.setOffsetWorld(4.0, 1.5, 3.5);
//		assertTrue(4.0 == kinematicPoint.getOffset().getX());
//		assertTrue(1.5 == kinematicPoint.getOffset().getY());
//		assertTrue(3.5 == kinematicPoint.getOffset().getZ());
//	}
	
	@Test
	public void testGetName()
	{
		assertTrue(kinematicPoint.getName() == "testPoint");
	}
	
	@Test
	public void testGetPosition()
	{
		Point3d positionToPack = new Point3d();
		kinematicPoint.getPosition(positionToPack);
		assertTrue(0 == positionToPack.x);
		assertTrue(0 == positionToPack.x);
		assertTrue(0 == positionToPack.x);

	}
	
	
	@Test
	public void testGetPositionPoint()
	{
		Point3d positionReceivedFromGetMethod = kinematicPoint.getPositionPoint();
		assertTrue(0 == positionReceivedFromGetMethod.x);
		assertTrue(0 == positionReceivedFromGetMethod.y);
		assertTrue(0 == positionReceivedFromGetMethod.z);

	}
	
	@Test
	public void testGetVelocityVector()
	{
		Vector3d vectorReceivedFromGetMethod = kinematicPoint.getVelocityVector();
		assertTrue(0 == vectorReceivedFromGetMethod.x);
		assertTrue(0 == vectorReceivedFromGetMethod.y);
		assertTrue(0 == vectorReceivedFromGetMethod.z);
	}
	
	@Test
	public void testGetVelocity()
	{
		Vector3d velocityToPack = kinematicPoint.getVelocityVector();
		kinematicPoint.getVelocity(velocityToPack);
		assertTrue(0 == velocityToPack.x);
		assertTrue(0 == velocityToPack.x);
		assertTrue(0 == velocityToPack.x);
	}
	
	@Test
	public void testGetYoPosition()
	{
		assertEquals("(0.0, 0.0, 0.0)", kinematicPoint.getYoPosition().toString());
		
	}
	
	@Test
	public void testGetYoVelocity()
	{
		assertEquals("(0.0, 0.0, 0.0)", kinematicPoint.getYoVelocity().toString());
	}
	
	
	

}
