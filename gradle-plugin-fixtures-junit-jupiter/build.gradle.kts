
plugins {
    groovy
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {

    compileOnly(gradleTestKit())
    implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")

    testImplementation(gradleTestKit())
    testImplementation("org.codehaus.groovy:groovy:3.0.7")
    testImplementation("org.spockframework:spock-core:2.0-groovy-3.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.jar)
}
