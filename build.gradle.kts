plugins {
    base
    id("org.sonarqube") version "7.3.1.8318"
}

allprojects {
    group = "pl.dudios"
    version = "1.0.0-SNAPSHOT"
}

sonar {
    properties {
        property("sonar.projectKey", "planner")
        property("sonar.projectName", "Planner")
        property("sonar.sources", "frontend/src")
        property("sonar.exclusions", "**/node_modules/**,**/dist/**,**/*.spec.ts,**/build/**,frontend/src/assets/**")
        property("sonar.coverage.exclusions", "frontend/src/**")
    }
}

tasks.register("unitTests") {
    group = "verification"
    description = "Run backend unit tests (auto-skips frontend build)."
    dependsOn(":backend:test")
}

tasks.register("integrationTests") {
    group = "verification"
    description = "Run backend integration tests with Testcontainers (auto-skips frontend build)."
    dependsOn(":backend:integrationTest")
}

tasks.register("allTests") {
    group = "verification"
    description = "Run unit + integration tests (auto-skips frontend build). Units run first."
    dependsOn("unitTests", "integrationTests")
}

gradle.projectsEvaluated {
    val testAliases = setOf("unitTests", "integrationTests", "allTests")
    val launchedFromAlias = gradle.startParameter.taskNames.any(testAliases::contains)
    if (launchedFromAlias) {
        gradle.startParameter.excludedTaskNames.addAll(
            listOf(":frontend:build", ":backend:copyFrontend")
        )
    }
}
