package us.ihmc.atlas.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.code.unitTesting.runner.JUnitTestSuiteRunner;

/** WARNING: AUTO-GENERATED FILE. DO NOT MAKE MANUAL CHANGES TO THIS FILE. **/
@RunWith(Suite.class)
@Suite.SuiteClasses
({
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCoursePlatformTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseRampsTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseRocksTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseStandingYawedTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseSteppingStonesTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseTrialsTerrainTest.class
})

public class AtlasEFastTestSuite
{
   public static void main(String[] args)
   {
      new JUnitTestSuiteRunner(AtlasEFastTestSuite.class);
   }
}

