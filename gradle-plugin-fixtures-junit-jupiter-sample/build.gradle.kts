plugins {
    id("groovy-gradle-plugin")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    testImplementation(project(":gradle-plugin-fixtures-junit-jupiter"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
}

repositories {
    mavenCentral()
}

gradlePlugin {
    (plugins) {
        create("fancy") {
            id = "fancy"
            implementationClass = "FancyPlugin"
        }
    }
}
