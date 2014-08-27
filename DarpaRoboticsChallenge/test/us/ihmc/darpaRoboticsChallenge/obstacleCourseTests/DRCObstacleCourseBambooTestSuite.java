package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.test.JUnitTestSuiteConstructor;

@RunWith(Suite.class)
@Suite.SuiteClasses(
{
//   us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseFlatTest.class,
//   us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCoursePlatformTest.class,
//   us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseRampsTest.class
//   us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseRocksTest.class,
//   us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseSteppingStonesTest.class,
//   us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseTrialsTerrainTest.class
})

public class DRCObstacleCourseBambooTestSuite
{
   public static void main(String[] args)
   {
      String packageName = "us.ihmc.darpaRoboticsChallenge.obstacleCourseTests";
      System.out.println(JUnitTestSuiteConstructor.createTestSuite("DRCObstacleCourseBambooTestSuite", packageName));
   }
}
