plugins {
    id("us.ihmc.ihmc-build")
    id("us.ihmc.ihmc-ci") version "7.4"
   id("us.ihmc.ihmc-cd") version "1.17"
    id("us.ihmc.log-tools-plugin") version "0.6.1"
}

ihmc {
    loadProductProperties("../product.properties")

    configureDependencyResolution()
    configurePublications()
}

mainDependencies {
    api("org.ejml:ejml-core:0.39")
    api("org.ejml:ejml-ddense:0.39")

    api("net.java.jinput:jinput:2.0.6-ihmc")
    api("us.ihmc:euclid-frame:0.16.2")
    api("us.ihmc:euclid-shape:0.16.2")
    api("us.ihmc:ihmc-yovariables:0.9.8")
    api("us.ihmc:ihmc-robot-description:0.21.2")
    api("us.ihmc:ihmc-robotics-toolkit:source")
    api("us.ihmc:ihmc-humanoid-robotics:source")
    api("us.ihmc:ihmc-quadruped-basics:source")
    api("us.ihmc:robot-environment-awareness:source")
    api("us.ihmc:simulation-construction-set-tools-test:source")
    api("us.ihmc:ihmc-robot-data-logger:0.20.7")
    api("us.ihmc:ihmc-path-planning:source")
}

testDependencies {

}
