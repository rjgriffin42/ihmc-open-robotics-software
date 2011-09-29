package us.ihmc.kalman;

import org.junit.runner.*;
import org.junit.runners.*;

import us.ihmc.utilities.test.JUnitTestSuiteConstructor;

@RunWith(Suite.class)
@Suite.SuiteClasses(
{
us.ihmc.kalman.YoKalmanFilterTest.class
}
)

public class KalmanProjectBambooTestSuite
{
public static void main(String[] args)
{
   String packageName = "us.ihmc.kalman";
   System.out.println(JUnitTestSuiteConstructor.createTestSuite("KalmanProjectBambooTestSuite", packageName));
}
}

