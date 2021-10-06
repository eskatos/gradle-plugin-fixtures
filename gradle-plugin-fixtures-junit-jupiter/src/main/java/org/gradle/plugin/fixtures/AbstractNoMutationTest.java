package org.gradle.plugin.fixtures;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.internal.GFileUtils;
import org.gradle.util.internal.TextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractNoMutationTest extends AbstractJUnitJupiterFixtureTest {

    @TempDir
    File tmp;

    @Test
    public void doesntForceAnyTaskCreationAtConfigurationTime() {

        File projectDir = underTestBuildDirectory();

        File init = new File(tmp, "only-help.init.gradle");
        GFileUtils.writeFile(onlyHelpConfiguredInitScript(), init);

        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments("help", "-I", init.getAbsolutePath());

        runner.build();
    }

    private String onlyHelpConfiguredInitScript() {
        return "allprojects { p ->\n" +
                "  p.tasks.withType(Task).configureEach { t ->\n" +
                "    if (t.name != 'help') {\n" +
                "      throw new Exception(\"Task '$t' was configured but it should not be!\")" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    @Test
    public void doesntForceAllTasksConfigurationWhenOwnTaskConfigured() {
        // run fancy
        // make sure help task not configured

        File projectDir = underTestBuildDirectory();
        String taskPath = underTestTaskPath();

        File init = new File(tmp, "only-task.init.gradle");
        GFileUtils.writeFile(onlyTaskConfiguredInitScript(taskPath), init);

        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(taskPath, "-I", init.getAbsolutePath());

        runner.build();
    }

    private String onlyTaskConfiguredInitScript(String taskPath) {
        return "allprojects { p ->\n" +
                "  p.tasks.withType(Task).configureEach { t ->\n" +
                "    if (t.path != '" + taskPath + "') {\n" +
                "      throw new Exception(\"Task '$t' was configured but it should not be!\")" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    @Test
    public void taskCachingAndRelocatability() {

        File projectDir = underTestBuildDirectory();
        String taskPath = underTestTaskPath();

        File copy1 = new File(tmp, "copy_1");
        File copy2 = new File(tmp, "copy_2");
        GFileUtils.copyDirectory(projectDir, copy1);
        GFileUtils.copyDirectory(projectDir, copy2);

        File init = new File(tmp, "isolated_build_cache.init.gradle");
        File localCache = new File(tmp, "isolated_build_cache");
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
}
