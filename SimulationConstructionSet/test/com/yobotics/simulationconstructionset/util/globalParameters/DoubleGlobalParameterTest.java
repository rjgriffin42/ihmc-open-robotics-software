package com.yobotics.simulationconstructionset.util.globalParameters;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DoubleGlobalParameterTest    // extends TestCase
{
   private final double DEFAULT_VALUE = 11.99;
   private final double eps = 1e-10;

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

      DoubleGlobalParameter doubleGlobalParameter = new DoubleGlobalParameter("testParameter", "test description", DEFAULT_VALUE,
                                                       systemOutGlobalParameterChangedListener);
      assertEquals(DEFAULT_VALUE, doubleGlobalParameter.getValue(), eps);
   }

   @Test
   public void testSetValue()
   {
      SystemOutGlobalParameterChangedListener systemOutGlobalParameterChangedListener = new SystemOutGlobalParameterChangedListener();


      DoubleGlobalParameter doubleGlobalParameter = new DoubleGlobalParameter("testParameter", "test description", DEFAULT_VALUE,
                                                       systemOutGlobalParameterChangedListener);

      double newValue = -0.045;
      doubleGlobalParameter.set(newValue);
      assertEquals(newValue, doubleGlobalParameter.getValue(), eps);

      newValue = 1100.345;
      doubleGlobalParameter.set(newValue, "setting");
      assertEquals(newValue, doubleGlobalParameter.getValue(), eps);

      newValue = 1100.345;
      doubleGlobalParameter.setOnlyIfChange(newValue, "setting");
      assertEquals(newValue, doubleGlobalParameter.getValue(), eps);

      newValue = -906.345;
      doubleGlobalParameter.setOnlyIfChange(newValue, "setting");
      assertEquals(newValue, doubleGlobalParameter.getValue(), eps);
   }

   @Test(expected = RuntimeException.class)
   public void testThatCantHaveParentsUnlessOverwriteUpdateMethodOne()
   {
      DoubleGlobalParameter parent = new DoubleGlobalParameter("parent", "parent", DEFAULT_VALUE, null);
      new DoubleGlobalParameter("invalidChild", "test description", new GlobalParameter[] {parent}, null); // invalid

      parent.set(1.0);
   }


   @Test(expected = RuntimeException.class)
   public void testCantSetChild()
   {
      DoubleGlobalParameter parent = new DoubleGlobalParameter("parent", "", 0.7, null);
      DoubleGlobalParameter child = new DoubleGlobalParameter("child", "", new GlobalParameter[] {parent}, null);

      child.set(0.99, "Shouldn't be able to change this!");
   }
}
