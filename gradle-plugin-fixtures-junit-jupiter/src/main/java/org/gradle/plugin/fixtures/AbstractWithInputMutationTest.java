package org.gradle.plugin.fixtures;


import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractWithInputMutationTest extends AbstractJUnitJupiterFixtureTest {

    protected abstract Consumer<File> initialUnderTestBuildDirectoryAssertion();

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
            AbstractWithInputMutationTest instance = (AbstractWithInputMutationTest) testClass.getDeclaredConstructor().newInstance();
            return instance.taskInputMutationArguments().map(arguments -> Arguments.of(Named.of(arguments.inputName, arguments)));
        }
    }

    protected abstract Stream<TaskInputMutationArgument> taskInputMutationArguments();

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
            return inputName;
        }
    }
}
