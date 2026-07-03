plugins {
    kotlin("jvm")
}

group = "dev.defdo.mobile"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    // Theme module tests run through the fallback contract runner (runContractTests),
    // mirroring the auth module. The standard test task has no JUnit-discoverable
    // tests, so don't fail when none are found.
    systemProperty("failOnNoDiscoveredTests", "false")
    testLogging {
        events("passed", "skipped", "failed")
    }
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("runContractTests") {
    description = "Run theme contract tests using the fallback test runner"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.defdo.mobile.theme.ThemeContractTestRunnerKt")
    workingDir = rootProject.projectDir
}
