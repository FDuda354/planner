import com.github.gradle.node.npm.task.NpmTask

plugins {
    base
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    version.set("24.15.0")
    download.set(true)
}

tasks.register<NpmTask>("npmCi") {
    group = "build"
    description = "Installs frontend dependencies using npm ci (deterministic)."

    dependsOn(tasks.named("npmSetup"))

    args.set(listOf("ci"))

    inputs.file("package.json")
    inputs.file("package-lock.json")
    outputs.dir("node_modules")
}

tasks.register<NpmTask>("buildFrontend") {
    group = "build"
    description = "Builds Angular frontend into dist/."

    dependsOn(tasks.named("npmCi"))

    args.set(listOf("run", "build"))
    environment.set(mapOf("PRIMEUI_LICENSE" to (System.getenv("PRIMEUI_LICENSE") ?: "")))
    inputs.property("primeUiLicense", providers.environmentVariable("PRIMEUI_LICENSE").orElse(""))

    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("package-lock.json")
    inputs.file("angular.json")
    inputs.file("tsconfig.json")
    inputs.file("tsconfig.app.json")
    outputs.dir("dist")
}

tasks.register<Delete>("cleanFrontend") {
    group = "build"
    description = "Cleans frontend artifacts (dist, node_modules)."

    delete("dist", "node_modules")
}

tasks.named("clean") {
    dependsOn(tasks.named("cleanFrontend"))
}

tasks.named("build") {
    dependsOn(tasks.named("buildFrontend"))
}
