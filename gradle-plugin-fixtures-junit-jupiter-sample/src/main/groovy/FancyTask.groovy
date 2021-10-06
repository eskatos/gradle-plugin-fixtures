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
        theFile.get().asFile.text = "${thePrefix.get()} ${theText.get()} ${theSuffix.get()}"
    }
}
