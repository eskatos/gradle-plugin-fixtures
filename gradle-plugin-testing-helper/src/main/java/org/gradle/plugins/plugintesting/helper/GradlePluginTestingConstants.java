package org.gradle.plugins.plugintesting.helper;

public final class GradlePluginTestingConstants {

    public static final String GRADLE_VERSION_UNDER_TEST = "gradlePluginTesting_gradleVersion";
    public static final String GRADLE_VERSION_SUPPORTS_CONFIGURATION_CACHE = "gradlePluginTesting_supportsConfigurationCache";
    public static final String GRADLE_VERSION_SUPPORTS_PROJECT_ISOLATION = "gradlePluginTesting_supportsIsolatedProjects";

    private GradlePluginTestingConstants() {
        throw new IllegalStateException("unreachable");
    }
}
