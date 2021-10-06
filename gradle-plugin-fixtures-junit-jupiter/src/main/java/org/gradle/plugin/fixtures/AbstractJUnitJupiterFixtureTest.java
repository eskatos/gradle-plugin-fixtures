package org.gradle.plugin.fixtures;

import java.io.File;

public abstract class AbstractJUnitJupiterFixtureTest {

    protected abstract File underTestBuildDirectory();

    protected abstract String underTestTaskPath();
}
