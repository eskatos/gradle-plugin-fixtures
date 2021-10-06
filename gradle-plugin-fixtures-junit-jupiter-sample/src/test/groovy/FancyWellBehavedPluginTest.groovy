import org.gradle.plugin.fixtures.AbstractWellBehavedPluginTest
import java.util.function.Consumer
import java.util.stream.Stream
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertEquals

class FancyWellBehavedPluginTest extends AbstractWellBehavedPluginTest {

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
