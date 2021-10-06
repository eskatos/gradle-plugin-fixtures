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
