package us.ihmc.tools.calibration;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.tools.calibration.CalibrationProperties;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;


public class CalibrationPropertiesTest
{
   private String fileName = "testCalFile";
   private File currentPropertiesFile;
   private CalibrationProperties calibrationProperties;
   
   @Before
   public void setUp()
   {
      // create a calibration file with the specifies path.

      currentPropertiesFile = new File(fileName);
      calibrationProperties = new CalibrationProperties(".", fileName);
   }
   
   @After
   public void tearDown()
   {
      currentPropertiesFile.delete();
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSimple()
   {
      String key = "abcd";
      int value = 23;
      calibrationProperties.setProperty(key, value);
      assertEquals(value, calibrationProperties.getIntegerProperty(key));
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout = 30000)
   public void testSaveAndLoad()
   {
      String key = "abcd";
      int value = 23;
      calibrationProperties.setProperty(key, value);
      calibrationProperties.save();
      
      CalibrationProperties calibrationProperties2 = new CalibrationProperties(".", fileName);
      assertEquals(value, calibrationProperties2.getIntegerProperty(key));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testArithmetic1()
   {
      String key = "abcd";
      int value1 = 23;
      int value2 = 56;
      String value = value1 + "+" + value2;
      calibrationProperties.setProperty(key, value);
      assertEquals(value1 + value2, calibrationProperties.getIntegerProperty(key));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testArithmetic2()
   {
      String key = "abcd";
      String value = "-23 + 0-4 +8 +-5";
      calibrationProperties.setProperty(key, value);
      assertEquals(-24, calibrationProperties.getIntegerProperty(key));
   }
}
