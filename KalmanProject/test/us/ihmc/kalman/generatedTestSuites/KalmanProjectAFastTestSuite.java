package us.ihmc.kalman.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.code.unitTesting.runner.JUnitTestSuiteRunner;

/** WARNING: AUTO-GENERATED FILE. DO NOT MAKE MANUAL CHANGES TO THIS FILE. **/
@RunWith(Suite.class)
@Suite.SuiteClasses
({
   us.ihmc.kalman.imu.QuaternionToolsTest.class,
   us.ihmc.kalman.imu.testCases.KalmanFilterComparisonTest.class,
   us.ihmc.kalman.YoKalmanFilterTest.class
})

public class KalmanProjectAFastTestSuite
{
   public static void main(String[] args)
   {
      new JUnitTestSuiteRunner(KalmanProjectAFastTestSuite.class);
   }
}

