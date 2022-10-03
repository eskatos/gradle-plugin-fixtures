package org.gradle.plugin.fixtures;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.internal.GFileUtils;
import org.gradle.util.internal.TextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractWellBehavedPluginTest {

    @TempDir
    File abstractWellBehavedPluginTemporaryDir;

    protected abstract File underTestBuildDirectory();

    protected abstract String underTestTaskPath();

    protected abstract Consumer<File> initialUnderTestBuildDirectoryAssertion();

    protected abstract Stream<TaskInputMutationArgument> taskInputMutationArguments();

    @Test
    public void doesntForceAnyTaskConfigurationAtConfigurationTime() {

        File projectDir = underTestBuildDirectory();

        File init = new File(abstractWellBehavedPluginTemporaryDir, "only-help.init.gradle");
        GFileUtils.writeFile(onlyHelpConfiguredInitScript(), init);

        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments("help", "-I", init.getAbsolutePath());

        runner.build();
    }

    private String onlyHelpConfiguredInitScript() {
        // This also allows 'clean' to be configured because it still is eagerly configured in Gradle core
        return "allprojects { p ->\n" +
                "  p.tasks.withType(Task).configureEach { t ->\n" +
                "    if (t.name !in ['help', 'clean']) {\n" +
                "      throw new Exception(\"Task '$t' was configured but it should not be!\")" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    @Test
    public void doesntForceExtraTasksConfigurationWhenOwnTaskIsScheduled() {

        File projectDir = underTestBuildDirectory();
        String taskPath = underTestTaskPath();

        File init = new File(abstractWellBehavedPluginTemporaryDir, "only-task.init.gradle");
        GFileUtils.writeFile(noExtraConfiguredTasksInitScript(), init);

        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(taskPath, "-I", init.getAbsolutePath());

        runner.build();
    }

    private String noExtraConfiguredTasksInitScript() {
        List<String> expectedExtra = expectedExtraConfiguredTaskPaths();
        return "List<String> configuredTaskPaths = []\n" +
                "gradle.allprojects { p ->\n" +
                "    p.tasks.withType(Task).configureEach { task ->\n" +
                "        configuredTaskPaths += task.path\n" +
                "    }\n" +
                "}\n" +
                "gradle.taskGraph.whenReady { graph ->\n" +
                "    List<String> scheduledTaskPaths = graph.allTasks.collect { it.path }\n" +
                "    List<String> expectedExtraConfiguredTaskPaths = [" + expectedExtra.stream().collect(Collectors.joining("', '", "'", "'")) + "]\n" +
                "    List<String> extraConfiguredTaskPaths = (configuredTaskPaths - scheduledTaskPaths - expectedExtraConfiguredTaskPaths).findAll { !it.empty }\n" +
                "    if (!extraConfiguredTaskPaths.empty) {\n" +
                "        throw new GradleException(\"Unscheduled tasks were configured: $extraConfiguredTaskPaths\")\n" +
                "    }\n" +
                "    List<String> unexpectedExtraConfiguredTaskPaths = (expectedExtraConfiguredTaskPaths - configuredTaskPaths).findAll { !it.empty }\n" +
                "    if (!unexpectedExtraConfiguredTaskPaths.empty) {\n" +
                "        throw new GradleException(\"Expected extra configured tasks were not configured: $unexpectedExtraConfiguredTaskPaths\")\n" +
                "    }\n" +
                "}\n";
    }

    protected List<String> expectedExtraConfiguredTaskPaths() {
        return Collections.emptyList();
    }

    @Test
    public void taskCachingAndRelocatability() {

        File projectDir = underTestBuildDirectory();
        String taskPath = underTestTaskPath();

        File copy1 = new File(abstractWellBehavedPluginTemporaryDir, "copy_1");
        File copy2 = new File(abstractWellBehavedPluginTemporaryDir, "copy_2");
        GFileUtils.copyDirectory(projectDir, copy1);
        GFileUtils.copyDirectory(projectDir, copy2);

        File init = new File(abstractWellBehavedPluginTemporaryDir, "isolated_build_cache.init.gradle");
        File localCache = new File(abstractWellBehavedPluginTemporaryDir, "isolated_build_cache");
        GFileUtils.writeFile(isolatedLocalBuildCacheInitScript(localCache), init);

        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(copy1)
                .withArguments(taskPath, "--build-cache", "-I", init.getAbsolutePath());
        BuildResult result = runner.build();
        assertEquals(TaskOutcome.SUCCESS, result.task(taskPath).getOutcome());

        runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(copy2)
                .withArguments(taskPath, "--build-cache", "-I", init.getAbsolutePath());
        result = runner.build();
        assertEquals(TaskOutcome.FROM_CACHE, result.task(taskPath).getOutcome());
    }

    private String isolatedLocalBuildCacheInitScript(File dir) {
        return "settingsEvaluated { settings ->\n" +
                "  settings.buildCache.local.directory = '" + TextUtil.escapeString(dir.getAbsolutePath()) + "'" +
                "}\n";
    }

    @ParameterizedTest(name = "{index}: {0}")
    @ArgumentsSource(TaskInputMutationUpToDateNessArgumentsProvider.class)
    public void taskInputMutationUpToDateNess(TaskInputMutationArgument args) {

        File projectDir = underTestBuildDirectory();
        String taskPath = underTestTaskPath();

        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(taskPath);

        BuildResult result = runner.build();
        assertEquals(TaskOutcome.SUCCESS, result.task(taskPath).getOutcome());
        initialUnderTestBuildDirectoryAssertion().accept(projectDir);

        result = runner.build();
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(taskPath).getOutcome());

        args.underTestBuildDirectoryMutation.accept(projectDir);

        result = runner.build();
        assertEquals(TaskOutcome.SUCCESS, result.task(taskPath).getOutcome());
        args.mutatedUnderTestBuildDirectoryAssertion.accept(projectDir);
    }

    static class TaskInputMutationUpToDateNessArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            Class<?> testClass = context.getTestClass().get();
            AbstractWellBehavedPluginTest instance = (AbstractWellBehavedPluginTest) testClass.getDeclaredConstructor().newInstance();
            return instance.taskInputMutationArguments().map(Arguments::of);
        }
    }

    public static class TaskInputMutationArgument {

        final String inputName;
        final Consumer<File> underTestBuildDirectoryMutation;
        final Consumer<File> mutatedUnderTestBuildDirectoryAssertion;

        public TaskInputMutationArgument(
                String inputName,
                Consumer<File> underTestBuildDirectoryMutation,
                Consumer<File> mutatedUnderTestBuildDirectoryAssertion
        ) {
            this.inputName = inputName;
            this.underTestBuildDirectoryMutation = underTestBuildDirectoryMutation;
            this.mutatedUnderTestBuildDirectoryAssertion = mutatedUnderTestBuildDirectoryAssertion;
        }

        @Override
        public String toString() {
            return "for input '" + inputName + "'";
        }
    }
}
