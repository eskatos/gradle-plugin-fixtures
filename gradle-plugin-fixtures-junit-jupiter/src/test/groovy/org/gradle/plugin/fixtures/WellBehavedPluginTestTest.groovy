package org.gradle.plugin.fixtures

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.internal.TextUtil
import spock.lang.Specification
import spock.lang.TempDir

class WellBehavedPluginTestTest extends Specification {

    @TempDir
    File tmp

    def "test"() {

        given: "a build for a plugin that uses the fixture"
        file("settings.gradle") << "rootProject.name = 'test-project'"
        file("build.gradle") << """
            plugins {
                id 'groovy-gradle-plugin'
            }
            
            test {
                useJUnitPlatform()
                testLogging {
                    showStandardStreams = true
                }
            }
            
            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
                testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
                testImplementation(files("${TextUtil.escapeString(new File("build/libs/gradle-plugin-fixtures-junit-jupiter.jar").absolutePath)}"))
            }
            
            repositories {
                mavenCentral()
            }
            
            gradlePlugin {
                plugins {
                    fancy {
                        id = "fancy"
                        implementationClass = "FancyPlugin"
                    }
                }
            }
        """
        file("src/main/groovy/FancyTask.groovy") << """
            import org.gradle.api.*
            import org.gradle.api.file.*
            import org.gradle.api.provider.*
            import org.gradle.api.tasks.*

            @CacheableTask
            abstract class FancyTask extends DefaultTask {
            
                @Input
                abstract Property<String> getThePrefix()

                @Input
                abstract Property<String> getTheText()

                @Input
                abstract Property<String> getTheSuffix()

                @OutputFile
                abstract RegularFileProperty getTheFile()                
            
                @TaskAction
                void action() {
                    theFile.get().asFile.text = "${'$'}{thePrefix.get()} ${'$'}{theText.get()} ${'$'}{theSuffix.get()}"
                }
            }
        """.stripIndent()
        file("src/main/groovy/FancyPlugin.groovy") << """
            import org.gradle.api.*
            abstract class FancyPlugin implements Plugin<Project> {
                void apply(Project project) {
                    project.tasks.register("fancy", FancyTask) {
                        thePrefix.convention(project.providers.gradleProperty("thePrefix").orElse("PREFIX"))
                        theText.convention(project.providers.gradleProperty("theText").orElse("TEXT"))
                        theSuffix.convention(project.providers.gradleProperty("theSuffix").orElse("SUFFIX"))
                        theFile.convention(project.layout.buildDirectory.file("the-file.txt"))
                    }
                }
            }
        """.stripIndent()
        file("src/test/groovy/FancyUpToDateTest.groovy") << """
        import org.gradle.plugin.fixtures.AbstractWellBehavedPluginTest
        import java.io.File
        import java.util.function.Consumer;
        import java.util.stream.Stream
        import org.junit.jupiter.api.extension.ExtensionContext
        import org.junit.jupiter.params.provider.Arguments
        import org.junit.jupiter.api.io.TempDir
        import static org.junit.jupiter.api.Assertions.assertEquals;

        class FancyUpToDateTest extends AbstractWellBehavedPluginTest {
        
            @TempDir
            File tmp
        
            @Override
            protected File underTestBuildDirectory() {
                file("settings.gradle") << "rootProject.name = 'testception'"
                file("build.gradle") << "plugins { id 'fancy' }"
                return tmp
            }
        
            @Override
            protected String underTestTaskPath() {
                return ":fancy"
            }
            
            @Override
            protected Consumer<File> initialUnderTestBuildDirectoryAssertion() {
                return { dir -> 
                    assertEquals("PREFIX TEXT SUFFIX", new File(dir, "build/the-file.txt").text)  
                }
            }
            
            @Override
            protected Stream<TaskInputMutationArgument> taskInputMutationArguments() {
                return Stream.of(
                    new TaskInputMutationArgument(
                        "thePrefix", 
                        { dir -> new File(dir, "gradle.properties").text = "thePrefix=SomePrefix" },
                        { dir -> assertEquals("SomePrefix TEXT SUFFIX", new File(dir, "build/the-file.txt").text) }
                    ),
                    new TaskInputMutationArgument(
                        "theText",
                        { dir -> new File(dir, "gradle.properties").text = "theText=SomeText" },
                        { dir -> assertEquals("PREFIX SomeText SUFFIX", new File(dir, "build/the-file.txt").text) }
                    ),
                    new TaskInputMutationArgument(
                        "theSuffix",
                        { dir -> new File(dir, "gradle.properties").text = "theSuffix=SomeSuffix" },
                        { dir -> assertEquals("PREFIX TEXT SomeSuffix", new File(dir, "build/the-file.txt").text) }
                    )
                )
            }

            File file(String path) {
                return new File(tmp, path).tap {
                    parentFile.mkdirs()
                }
            }
        }
        """.stripIndent()

        when: "running the test task"
        def result = GradleRunner.create()
                .forwardOutput()
                .withProjectDir(tmp)
                .withArguments("test")
                .build()

        then: "success"
        result.task(":test").outcome == TaskOutcome.SUCCESS
    }

    File file(String path) {
        return new File(tmp, path).tap {
            parentFile.mkdirs()
        }
    }
}
