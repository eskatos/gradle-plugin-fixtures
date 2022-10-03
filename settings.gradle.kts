rootProject.name = "gradle-plugin-fixtures"

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("gradle-plugin-fixtures-settings")
}

include("gradle-plugin-fixtures-junit-jupiter")
include("gradle-plugin-fixtures-junit-jupiter-sample")

include("gradle-plugin-testing-plugin")
include("gradle-plugin-testing-helper")