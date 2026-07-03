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
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("runContractTests") {
    description = "Run auth contract tests using the fallback test runner"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.defdo.mobile.auth.AuthContractTestRunnerKt")
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("runDevIssuerSmokeHarness") {
    description = "Run the dev issuer smoke harness (requires DEFDO_DEV_ISSUER env var)"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.defdo.mobile.auth.DevIssuerSmokeHarnessKt")
    workingDir = rootProject.projectDir
    environment = System.getenv()
}
