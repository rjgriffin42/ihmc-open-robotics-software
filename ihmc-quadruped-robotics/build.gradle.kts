plugins {
   id("us.ihmc.ihmc-build") version "0.21.0"
   id("us.ihmc.ihmc-ci") version "5.3"
   id("us.ihmc.ihmc-cd") version "1.14"
   id("us.ihmc.scs") version "0.4"
   id("us.ihmc.log-tools-plugin") version "0.5.0"
}

ihmc {
   loadProductProperties("../product.properties")
   
   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("org.apache.commons:commons-lang3:3.8.1")
   api("us.ihmc.thirdparty.jinput:jinput:200128")
   api("org.ejml:ejml-core:0.39")
   api("net.sf.trove4j:trove4j:3.0.3")
   api("org.ejml:ejml-ddense:0.39")
   api("com.google.guava:guava:18.0")

   api("us.ihmc:euclid-frame:0.15.0")
   api("us.ihmc:euclid-shape:0.15.0")
   api("us.ihmc:ihmc-yovariables:0.8.0")
   api("us.ihmc:ihmc-commons:0.30.3")
   api("us.ihmc:ihmc-graphics-description:0.18.0")
   api("us.ihmc:ihmc-robot-description:0.19.0")
   api("us.ihmc:ihmc-robot-data-logger:0.19.0")
   api("us.ihmc:ihmc-common-walking-control-modules:source")
   api("us.ihmc:ihmc-convex-optimization:0.17.0")
   api("us.ihmc:ihmc-humanoid-robotics:source")
   api("us.ihmc:ihmc-quadruped-basics:source")
   api("us.ihmc:ihmc-quadruped-communication:source")
   api("us.ihmc:ihmc-quadruped-planning:source")
   api("us.ihmc:ihmc-java-toolkit:source")
   api("us.ihmc:ihmc-robotics-toolkit:source")
   api("us.ihmc:ihmc-communication:source")
   api("us.ihmc:ihmc-robot-models:source")
   api("us.ihmc:ihmc-sensor-processing:source")
   api("us.ihmc:ihmc-state-estimation:source")
   api("us.ihmc:ihmc-simulation-toolkit:source")
   api("us.ihmc:ihmc-system-identification:source")
}

testDependencies {
   api("com.google.caliper:caliper:1.0-beta-2")

   api("us.ihmc:simulation-construction-set:0.19.0")
   api("us.ihmc:simulation-construction-set-test:0.19.0")
   api("us.ihmc:ihmc-robotics-toolkit-test:source")
   api("us.ihmc:simulation-construction-set-tools-test:source")
   api("us.ihmc:ihmc-common-walking-control-modules-test:source")
   api("us.ihmc:ihmc-communication-test:source")
}
