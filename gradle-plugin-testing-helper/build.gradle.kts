plugins {
    id("gradle-plugin-fixtures-java-library")
}

dependencies {
    compileOnly(gradleTestKit())
}
