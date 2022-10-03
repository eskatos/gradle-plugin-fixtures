package org.gradle.plugins.plugintesting.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.plugintesting.helper.GradlePluginTestingConstants;
import org.gradle.util.GradleVersion;

import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class GradlePluginTestingPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.getPlugins().apply("java-gradle-plugin");

        GradlePluginTestingExtension extension = (GradlePluginTestingExtension) project.getExtensions().create("gradlePluginTesting", GradlePluginTestingExtension.class);
        extension.getTestedGradleVersions().finalizeValueOnRead();
        extension.getMinimumGradleVersionForConfigurationCache().finalizeValueOnRead();
        extension.getMinimumGradleVersionForIsolatedProjects().finalizeValueOnRead();

        String currentGradleVersion = GradleVersion.current().getVersion();
        extension.getTestedGradleVersions().convention(Arrays.asList(currentGradleVersion));
        extension.getMinimumGradleVersionForConfigurationCache().convention("7.0");
        extension.getMinimumGradleVersionForIsolatedProjects().convention("8.0");

        project.afterEvaluate(p -> {

            extension.getTestedGradleVersions().finalizeValue();
            extension.getMinimumGradleVersionForConfigurationCache().finalizeValue();
            extension.getMinimumGradleVersionForIsolatedProjects().finalizeValue();

            SortedSet<GradleVersion> testedVersions = new TreeSet<>();
            for (String version : (Set<String>) extension.getTestedGradleVersions().get()) {
                testedVersions.add(GradleVersion.version(version));
            }

            GradleVersion latest = testedVersions.last();
            testedVersions.remove(latest);

            TaskProvider<Task> check = project.getTasks().named("check");

            TaskProvider<Test> testLatest = project.getTasks().named("test", Test.class);
            configureGradlePluginTestTask(extension, testLatest, latest);

            for (GradleVersion version : testedVersions) {
                TaskProvider<Test> testVersion = project.getTasks().register(testTaskNameFor(version), Test.class);
                configureTestTask(testLatest, testVersion, check);
                configureGradlePluginTestTask(extension, testVersion, version);
            }
        });
    }

    private String testTaskNameFor(GradleVersion version) {
        return "testGradle" + version.getVersion().replaceAll("[\\.-]", "_");
    }

    private void configureTestTask(TaskProvider<Test> test, TaskProvider<Test> testVersion, TaskProvider<Task> check) {
        testVersion.configure(task -> {
            task.dependsOn(check);
            test.get().copyTo(task); // TODO TCA?
            task.getModularity().getInferModulePath().set(test.flatMap(t -> t.getModularity().getInferModulePath()));
            task.getJavaLauncher().set(test.flatMap(Test::getJavaLauncher));
            task.setTestClassesDirs(test.get().getTestClassesDirs()); // TODO TCA?
            task.setIncludes(test.get().getIncludes()); // TODO TCA?
            task.setExcludes(test.get().getExcludes()); // TODO TCA?
            task.setClasspath(test.get().getClasspath()); // TODO TCA?
            // TODO Test framework
            // TODO Test framework options
            task.setScanForTestClasses(test.get().isScanForTestClasses()); // TODO TCA?
            task.setForkEvery(test.get().getForkEvery()); // TODO TCA?
            task.setMaxParallelForks(test.get().getMaxParallelForks()); // TODO TCA?
        });
    }

    private void configureGradlePluginTestTask(GradlePluginTestingExtension extension, TaskProvider<Test> testTask, GradleVersion version) {
        testTask.configure(task -> {

            task.systemProperty(GradlePluginTestingConstants.GRADLE_VERSION_UNDER_TEST, version.getVersion());

            GradleVersion minConfigCache = GradleVersion.version(extension.getMinimumGradleVersionForConfigurationCache().get());
            if (version.compareTo(minConfigCache) >= 0) {
                task.systemProperty(GradlePluginTestingConstants.GRADLE_VERSION_SUPPORTS_CONFIGURATION_CACHE, "true");
            }

            GradleVersion minIsolatedProjects = GradleVersion.version(extension.getMinimumGradleVersionForIsolatedProjects().get());
            if (version.compareTo(minIsolatedProjects) >= 0) {
                task.systemProperty(GradlePluginTestingConstants.GRADLE_VERSION_SUPPORTS_PROJECT_ISOLATION, "true");
            }
        });
    }
}
