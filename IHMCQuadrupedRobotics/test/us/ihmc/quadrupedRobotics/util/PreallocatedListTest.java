package us.ihmc.quadrupedRobotics.util;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.Test;
import us.ihmc.tools.testing.TestPlanAnnotations;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class PreallocatedListTest
{
   @TestPlanAnnotations.DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testCapacity()
   {
      PreallocatedList<MutableDouble> doubleList = new PreallocatedList<>(MutableDouble.class, 10);
      for (int i = 0; i < 10; i++)
      {
         assertTrue(doubleList.add());
      }
      assertFalse(doubleList.add());
      assertEquals(doubleList.size(), 10);
      assertEquals(doubleList.capacity(), 10);
   }

   @TestPlanAnnotations.DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testAddRemove()
   {
      double epsilon = 0.001;

      PreallocatedList<MutableDouble> doubleList = new PreallocatedList<>(MutableDouble.class, 10);
      for (int i = 0; i < 10; i++)
      {
         doubleList.add();
         doubleList.get(i).setValue(i);
      }

      // front of list
      doubleList.remove(0);
      assertEquals(doubleList.size(), 9);
      assertEquals(doubleList.get(0).getValue(), 1.0, epsilon);
      assertEquals(doubleList.get(1).getValue(), 2.0, epsilon);
      doubleList.add(0);
      doubleList.get(0).setValue(0);
      assertEquals(doubleList.size(), 10);
      assertEquals(doubleList.get(0).getValue(), 0.0, epsilon);
      assertEquals(doubleList.get(1).getValue(), 1.0, epsilon);

      // first half
      doubleList.remove(3);
      assertEquals(doubleList.size(), 9);
      assertEquals(doubleList.get(2).getValue(), 2.0, epsilon);
      assertEquals(doubleList.get(3).getValue(), 4.0, epsilon);
      assertEquals(doubleList.get(4).getValue(), 5.0, epsilon);
      doubleList.add(3);
      doubleList.get(3).setValue(3);
      assertEquals(doubleList.size(), 10);
      assertEquals(doubleList.get(2).getValue(), 2.0, epsilon);
      assertEquals(doubleList.get(3).getValue(), 3.0, epsilon);
      assertEquals(doubleList.get(4).getValue(), 4.0, epsilon);

      // second half
      doubleList.remove(7);
      assertEquals(doubleList.size(), 9);
      assertEquals(doubleList.get(6).getValue(), 6.0, epsilon);
      assertEquals(doubleList.get(7).getValue(), 8.0, epsilon);
      assertEquals(doubleList.get(8).getValue(), 9.0, epsilon);
      doubleList.add(7);
      doubleList.get(7).setValue(7);
      assertEquals(doubleList.size(), 10);
      assertEquals(doubleList.get(6).getValue(), 6.0, epsilon);
      assertEquals(doubleList.get(7).getValue(), 7.0, epsilon);
      assertEquals(doubleList.get(8).getValue(), 8.0, epsilon);

      // end of list
      doubleList.remove(9);
      assertEquals(doubleList.size(), 9);
      assertEquals(doubleList.get(7).getValue(), 7.0, epsilon);
      assertEquals(doubleList.get(8).getValue(), 8.0, epsilon);
      doubleList.add(9);
      doubleList.get(9).setValue(9);
      assertEquals(doubleList.size(), 10);
      assertEquals(doubleList.get(8).getValue(), 8.0, epsilon);
      assertEquals(doubleList.get(9).getValue(), 9.0, epsilon);
   }
}
