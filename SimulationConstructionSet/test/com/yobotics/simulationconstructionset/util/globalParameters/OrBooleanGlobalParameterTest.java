package com.yobotics.simulationconstructionset.util.globalParameters;

import static org.junit.Assert.*;

import org.junit.*;

public class OrBooleanGlobalParameterTest
{

    @Before
    public void setUp()
    {
       GlobalParameter.clearGlobalRegistry();
    }
    
    @After
    public void tearDown()
    {
       GlobalParameter.clearGlobalRegistry();
    }
    
    @Test
    public void testSetThrowsException()
    {
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();

	boolean valueA = true;
	boolean valueB = false;
	
	BooleanGlobalParameter booleanGlobalParameterA = new BooleanGlobalParameter("testParameterA", "test description", valueA, systemOutGlobalParameterChangedListener);
	BooleanGlobalParameter booleanGlobalParameterB = new BooleanGlobalParameter("testParameterB", "test description", valueB, systemOutGlobalParameterChangedListener);

	AndBooleanGlobalParameter multiplicativeDoubleGlobalParameter = new AndBooleanGlobalParameter("testMulti",
		"multiplicative parameter", new BooleanGlobalParameter[]{booleanGlobalParameterA, booleanGlobalParameterB}, systemOutGlobalParameterChangedListener);
	
	try
	{
	    multiplicativeDoubleGlobalParameter.set(false);
	    fail();
	}
	catch (Exception e)
	{
	}
    }

    @Test
    public void testMultiplicativeDoubleGlobalParameter()
    {
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();

	boolean valueA = true;
	boolean valueB = false;
	
	BooleanGlobalParameter booleanGlobalParameterA = new BooleanGlobalParameter("testParameterA", "test description", valueA, systemOutGlobalParameterChangedListener);
	BooleanGlobalParameter booleanGlobalParameterB = new BooleanGlobalParameter("testParameterB", "test description", valueB, systemOutGlobalParameterChangedListener);

	AndBooleanGlobalParameter multiplicativeDoubleGlobalParameter = new AndBooleanGlobalParameter("testMulti",
		"multiplicative parameter", new BooleanGlobalParameter[]{booleanGlobalParameterA, booleanGlobalParameterB}, systemOutGlobalParameterChangedListener);
	
	
	assertEquals(valueA || valueB, multiplicativeDoubleGlobalParameter.getValue());
    }
    
    
    @Test
    public void testMultiplicativeDoubleGlobalParameterUpdate()
    {
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();

	
	boolean valueA = true;
	boolean valueB = false;
	
	BooleanGlobalParameter booleanGlobalParameterA = new BooleanGlobalParameter("testParameterA", "test description", valueA, systemOutGlobalParameterChangedListener);
	BooleanGlobalParameter booleanGlobalParameterB = new BooleanGlobalParameter("testParameterB", "test description", valueB, systemOutGlobalParameterChangedListener);

	AndBooleanGlobalParameter multiplicativeDoubleGlobalParameter = new AndBooleanGlobalParameter("testMulti",
		"multiplicative parameter", new BooleanGlobalParameter[]{booleanGlobalParameterA, booleanGlobalParameterB}, systemOutGlobalParameterChangedListener);
	
	
	valueA = false;
	booleanGlobalParameterA.set(valueA);	
	assertEquals(valueA || valueB, multiplicativeDoubleGlobalParameter.getValue());

	valueB = true;
	booleanGlobalParameterB.set(valueB);
	assertEquals(valueA || valueB, multiplicativeDoubleGlobalParameter.getValue());

	valueA = true;
	valueB = true;
	booleanGlobalParameterA.set(valueA);
	booleanGlobalParameterB.set(valueB);
	assertEquals(valueA || valueB, multiplicativeDoubleGlobalParameter.getValue());
    }
    
    @Test
    public void testFamilyTree()
    {
//	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = null; //new SystemOutGlobalParameterChangedListener();


	boolean valueA = false;
	boolean valueB = true;

	BooleanGlobalParameter grandParentA = new BooleanGlobalParameter("grandParentA", "test descriptionA", valueA, systemOutGlobalParameterChangedListener);
	BooleanGlobalParameter grandParentB = new BooleanGlobalParameter("grandParentB", "test descriptionB", valueB, systemOutGlobalParameterChangedListener);

	AndBooleanGlobalParameter parentA = new AndBooleanGlobalParameter("parentA",
		"multiplicative parameter", new BooleanGlobalParameter[]{grandParentA}, systemOutGlobalParameterChangedListener);
	
	AndBooleanGlobalParameter parentB = new AndBooleanGlobalParameter("parentB",
		"multiplicative parameter", new BooleanGlobalParameter[]{grandParentB}, systemOutGlobalParameterChangedListener);
	
	AndBooleanGlobalParameter childA = new AndBooleanGlobalParameter("childA",
		"multiplicative parameter", new BooleanGlobalParameter[]{grandParentA, parentA}, systemOutGlobalParameterChangedListener);
	
	AndBooleanGlobalParameter childB = new AndBooleanGlobalParameter("childB",
		"multiplicative parameter", new BooleanGlobalParameter[]{grandParentA, grandParentB, parentA, parentB}, systemOutGlobalParameterChangedListener);
	
	
	boolean expectedParentA = valueA;
	assertEquals(expectedParentA, parentA.getValue());
	
	boolean expectedParentB = valueB;
	assertEquals(expectedParentB, parentB.getValue());
	
	boolean expectedChildA = valueA || valueA;
	assertEquals(expectedChildA, childA.getValue());

	boolean expectedChildB = valueA || valueB || valueA || valueB;
	assertEquals(expectedChildB, childB.getValue());

	valueA = !valueA;
	grandParentA.set(valueA);
	expectedParentA = valueA;
	assertEquals(expectedParentA, parentA.getValue());
	
	expectedParentB = valueB;
	assertEquals(expectedParentB, parentB.getValue());
	
	expectedChildA = valueA || valueA;
	assertEquals(expectedChildA, childA.getValue());

	expectedChildB = valueA || valueB || valueA || valueB;
	assertEquals(expectedChildB, childB.getValue());
	
	valueA = !valueA;
	valueB = !valueB;
	grandParentA.set(valueA);
	grandParentB.set(valueB);
	
	expectedParentA = valueA;
	assertEquals(expectedParentA, parentA.getValue());
	
	expectedParentB = valueB;
	assertEquals(expectedParentB, parentB.getValue());
	
	expectedChildA = valueA || valueA;
	assertEquals(expectedChildA, childA.getValue());

	expectedChildB = valueA || valueB || valueA || valueB;
	assertEquals(expectedChildB, childB.getValue());
    }



}

