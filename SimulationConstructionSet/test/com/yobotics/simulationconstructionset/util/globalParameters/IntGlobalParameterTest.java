package com.yobotics.simulationconstructionset.util.globalParameters;


import com.yobotics.simulationconstructionset.YoVariableType;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IntGlobalParameterTest
{

    private final int DEFAULT_VALUE = 11;
    
    @Before
    public void setUp() throws Exception
    {
       GlobalParameter.clearGlobalRegistry();
    }

    @After
    public void tearDown() throws Exception
    {
       GlobalParameter.clearGlobalRegistry();
    }
   
    @Test
    public void testGetValue()
    {	
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();
	
	IntGlobalParameter intGlobalParameter = new IntGlobalParameter("testParameter", "test description", DEFAULT_VALUE, systemOutGlobalParameterChangedListener);
	assertEquals(DEFAULT_VALUE, intGlobalParameter.getValue());
    }
    
    @Test
    public void testSetValue()
    {
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();

	
	IntGlobalParameter intGlobalParameter = new IntGlobalParameter("testParameter", "test description", DEFAULT_VALUE, systemOutGlobalParameterChangedListener);

	int newValue = -1;
	intGlobalParameter.set(newValue);
	assertEquals(newValue, intGlobalParameter.getValue());
	
	newValue = 1100;
	intGlobalParameter.set(newValue, "setting");
	assertEquals(newValue, intGlobalParameter.getValue());
	
	newValue = 1100;
	intGlobalParameter.setOnlyIfChange(newValue, "setting");
	assertEquals(newValue, intGlobalParameter.getValue());
	
	newValue = -906;
	intGlobalParameter.setOnlyIfChange(newValue, "setting");
	assertEquals(newValue, intGlobalParameter.getValue());
    }
    
    @Test
    public void testGetYoVariableType()
    {
	SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();

	IntGlobalParameter intGlobalParameter = new IntGlobalParameter("testParameter", "test description", DEFAULT_VALUE, systemOutGlobalParameterChangedListener);

	assertEquals(YoVariableType.INT, intGlobalParameter.getYoVariableType());
    }
    
    @Test (expected = RuntimeException.class)
    public void testThatCantHaveParentsUnlessOverwriteUpdateMethodOne()
    {
	IntGlobalParameter parent = new IntGlobalParameter("parent", "parent", DEFAULT_VALUE, null);
	IntGlobalParameter invalidChild = new IntGlobalParameter("invalidChild", "test description", new GlobalParameter[]{parent}, null);

	parent.set(1); 
    }
    

    @Test(expected = RuntimeException.class)
    public void testCantSetChild()
    {
       IntGlobalParameter parent = new IntGlobalParameter("parent", "", 0, null);
       IntGlobalParameter child = new IntGlobalParameter("child", "", new GlobalParameter[]{parent}, null);

       child.set(2, "Shouldn't be able to change this!");
    }   
}

