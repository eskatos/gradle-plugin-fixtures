package org.gradle.plugin.fixtures;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractUpToDateTaskTest {

    protected abstract File underTestBuildDirectory();

    protected abstract String underTestTaskPath();

    protected abstract Consumer<File> initialUnderTestBuildDirectoryAssertion();

    protected abstract Consumer<File> underTestBuildDirectoryMutation();

    protected abstract Consumer<File> mutatedUnderTestBuildDirectoryAssertion();

    @Test
    public void taskUpToDateness() {

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

        underTestBuildDirectoryMutation().accept(projectDir);
        result = runner.build();
        assertEquals(TaskOutcome.SUCCESS, result.task(taskPath).getOutcome());
        mutatedUnderTestBuildDirectoryAssertion().accept(projectDir);
    }
}
