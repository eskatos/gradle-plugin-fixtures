package org.gradle.plugins.plugintesting.plugin

import org.gradle.plugin.fixtures.AbstractWellBehavedPluginTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.io.TempDir

import java.util.function.Consumer
import java.util.stream.Stream

class GradlePluginTestingWellBehavedPluginTest extends AbstractWellBehavedPluginTest {

    @TempDir
    File tmp

    File file(String path) {
        return new File(tmp, path).tap {
            parentFile.mkdirs()
        }
    }

    @Override
    protected File underTestBuildDirectory() {
        file("settings.gradle") << "rootProject.name = 'testception'"
        file("build.gradle") << """
            plugins {
                id 'groovy-gradle-plugin'
                id 'org.gradle.plugins.gradle-plugin-testing'
            }
        """.stripIndent()
        return tmp
    }

    @Override
    protected String underTestTaskPath() {
        return ":test"
    }

    @Override
    protected List<String> expectedExtraConfiguredTaskPaths() {
        return [':jar', ':compileGroovyPlugins']
    }

    @Override
    protected Consumer<File> initialUnderTestBuildDirectoryAssertion() {
        return {}
    }

    @Override
    protected Stream<TaskInputMutationArgument> taskInputMutationArguments() {
        return Stream.of()
    }

    @Disabled
    @Override
    void taskCachingAndRelocatability() {
    }

    @Disabled
    @Override
    void taskInputMutationUpToDateNess(TaskInputMutationArgument args) {
    }
}
