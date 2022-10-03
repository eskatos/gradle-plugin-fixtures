package org.gradle.plugins.plugintesting.helper;

import org.gradle.testkit.runner.GradleRunner;

public class GradlePluginTestingHelper {

    public static String getGradleVersion() {
        return System.getProperty(GradlePluginTestingConstants.GRADLE_VERSION_UNDER_TEST);
    }

    public static boolean getSupportsConfigurationCache() {
        return Boolean.getBoolean(GradlePluginTestingConstants.GRADLE_VERSION_SUPPORTS_CONFIGURATION_CACHE);
    }

    public static boolean getSupportsIsolatedProjects() {
        return Boolean.getBoolean(GradlePluginTestingConstants.GRADLE_VERSION_SUPPORTS_PROJECT_ISOLATION);
    }

    public static GradleRunner configureRunner(GradleRunner runner) {
        // TODO need something like runner.beforeExecuter or alike to be able to add arguments on invocations
        return runner.withGradleVersion(getGradleVersion());
    }

    private GradlePluginTestingHelper() {
        throw new IllegalStateException("unreachable");
    }
}
