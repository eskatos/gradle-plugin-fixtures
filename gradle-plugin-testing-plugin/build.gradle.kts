plugins {
    id("gradle-plugin-fixtures-java-library")
    groovy
    `java-gradle-plugin`
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    api(project(":gradle-plugin-testing-helper"))
    testImplementation(project(":gradle-plugin-fixtures-junit-jupiter"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
}

gradlePlugin {
    (plugins) {
        create("gradle-plugin-testing") {
            id = "org.gradle.plugins.gradle-plugin-testing"
            implementationClass = "org.gradle.plugins.plugintesting.plugin.GradlePluginTestingPlugin"
        }
    }
}
