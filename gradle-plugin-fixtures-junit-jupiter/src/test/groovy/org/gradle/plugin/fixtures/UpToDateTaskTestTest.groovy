package org.gradle.plugin.fixtures

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.internal.GFileUtils
import spock.lang.Specification
import spock.lang.TempDir

class UpToDateTaskTestTest extends Specification {

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
                    events "failed"
                    exceptionFormat "full"
                }
            }
            
            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
                testImplementation(files("${new File("build/libs/gradle-plugin-fixtures-junit-jupiter.jar").absolutePath}"))
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
        import org.gradle.plugin.fixtures.AbstractUpToDateTaskTest
        import java.io.File
        import java.util.function.Consumer;
        import org.junit.jupiter.api.io.TempDir
        import static org.junit.jupiter.api.Assertions.assertEquals;

        class FancyUpToDateTest extends AbstractUpToDateTaskTest {
        
            @TempDir
            File tmp
        
            protected File underTestBuildDirectory() {
                file("settings.gradle") << "rootProject.name = 'testception'"
                file("build.gradle") << "plugins { id 'fancy' }"
                return tmp
            }
        
            protected String underTestTaskPath() {
                return ":fancy"
            }
            
            protected Consumer<File> initialUnderTestBuildDirectoryAssertion() {
                return { dir -> 
                    assertEquals("PREFIX TEXT SUFFIX", new File(dir, "build/the-file.txt").text)  
                }
            }
            
            protected Consumer<File> underTestBuildDirectoryMutation() {
                return { dir -> new File(dir, "gradle.properties").text = "theText=SomeText" }
            }
            
            protected Consumer<File> mutatedUnderTestBuildDirectoryAssertion() {
                return { dir -> 
                    assertEquals("PREFIX SomeText SUFFIX", new File(dir, "build/the-file.txt").text)  
                }
            }
            
             File file(String path) {
                return new File(tmp, path).tap {
                    parentFile.mkdirs()
                }
            }
        }
        """.stripIndent()
        file("src/test/groovy/FancyCacheableTaskTest.groovy") << """
        import org.gradle.plugin.fixtures.AbstractCacheableTaskTest
        import java.io.File
        import org.junit.jupiter.api.io.TempDir
        import static org.junit.jupiter.api.Assertions.assertEquals;

        class FancyCacheableTaskTest extends AbstractCacheableTaskTest {
        
            @TempDir
            File tmp
        
            protected File underTestBuildDirectory() {
                file("settings.gradle") << "rootProject.name = 'testception'"
                file("build.gradle") << "plugins { id 'fancy' }"
                return tmp
            }
        
            protected String underTestTaskPath() {
                return ":fancy"
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

        cleanup:
        def report = file("build/reports/tests/test/index.html")
        if (report.isFile()) {
            def reportDir = report.parentFile
            GFileUtils.copyDirectory(reportDir, new File("/home/paul/boom"))
        }
    }

    File file(String path) {
        return new File(tmp, path).tap {
            parentFile.mkdirs()
        }
    }
}
