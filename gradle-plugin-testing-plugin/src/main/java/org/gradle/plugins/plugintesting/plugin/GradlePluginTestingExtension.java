package org.gradle.plugins.plugintesting.plugin;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

public interface GradlePluginTestingExtension {

    SetProperty<String> getTestedGradleVersions();

    Property<String> getMinimumGradleVersionForConfigurationCache();

    Property<String> getMinimumGradleVersionForIsolatedProjects();
}
