import org.apache.commons.lang3.SystemUtils

buildscript {
   dependencies {
      classpath("org.apache.commons:commons-lang3:3.9")
   }
}

plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.4"
   id("us.ihmc.ihmc-cd") version "1.17"
   id("us.ihmc.log-tools-plugin") version "0.6.1"
}

ihmc {
   loadProductProperties("../product.properties")
   
   configureDependencyResolution()
   repository("https://oss.sonatype.org/content/repositories/snapshots")
   configurePublications()
}

mainDependencies {
   api(ihmc.sourceSetProject("javacv"))
   // For experimenting with local OpenCV:
   // api(files("/usr/local/share/OpenCV/java/opencv-310.jar"))

   api("org.apache.commons:commons-lang3:3.8.1")
   api("us.ihmc:ihmc-native-library-loader:1.3.1")
   api("org.georegression:georegression:0.22")
   api("org.ejml:ejml-core:0.39")
   api("org.ejml:ejml-ddense:0.39")
   api("net.java.dev.jna:jna:4.1.0")
   api("org.boofcv:boofcv-geo:0.36")
   api("org.boofcv:boofcv-ip:0.36")
   api("org.boofcv:boofcv-swing:0.36")
   api("org.boofcv:boofcv-io:0.36")
   api("org.boofcv:boofcv-recognition:0.36")
   api("org.boofcv:boofcv-calibration:0.36")
   api("us.ihmc.ihmcPerception:valvenet:0.0.4")
   api("us.ihmc.ihmcPerception:cuda:7.5")
   api("org.ddogleg:ddogleg:0.18")

   api("us.ihmc:euclid:0.16.1")
   api("us.ihmc:ihmc-yovariables:0.9.8")
   api("us.ihmc:simulation-construction-set:0.21.5")
   api("us.ihmc:ihmc-jmonkey-engine-toolkit:0.19.5")
   api("us.ihmc:ihmc-graphics-description:0.19.3")
   api("us.ihmc:ihmc-humanoid-robotics:source")
   api("us.ihmc:ihmc-communication:source")
   api("us.ihmc:ihmc-ros-tools:source")
   api("us.ihmc:ihmc-whole-body-controller:source")
   api("us.ihmc:ihmc-sensor-processing:source")
   api("us.ihmc:ihmc-robot-models:source")
   api("us.ihmc:ihmc-java-toolkit:source")
   api("us.ihmc:ihmc-robotics-toolkit:source")
   api("us.ihmc:ihmc-javafx-toolkit:0.18.0")
   api("us.ihmc:joctomap:1.11.0")
   api("us.ihmc:ihmc-path-planning:source")
   api("us.ihmc:ihmc-footstep-planning:source")
   api("us.ihmc:robot-environment-awareness:source")
}

openpnpDependencies {
   api("org.openpnp:opencv:4.3.0-2")
}

bytedecoDependencies {
   api("org.bytedeco:opencv-platform:4.5.1-1.5.5-SNAPSHOT")
}

javacvDependencies {
   api("org.bytedeco:javacv-platform:1.5.5-SNAPSHOT") {
      exclude("org.bytedeco:librealsense2")
   }
}

testDependencies {
   api("us.ihmc:ihmc-commons-testing:0.30.4")
   api("us.ihmc:simulation-construction-set:0.21.5")
   api("us.ihmc:ihmc-robotics-toolkit:source")
   api("us.ihmc:simulation-construction-set-tools:source")
   api("us.ihmc:simulation-construction-set-tools-test:source")
}
