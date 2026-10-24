import java.math.BigDecimal

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    filter {
        excludeTestsMatching("*IntegrationTest")
        isFailOnNoMatchingTests = false
    }
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("*IntegrationTest")
        isFailOnNoMatchingTests = false
    }
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(tasks.named("integrationTest"))
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

jacoco {
    toolVersion = "0.8.13"
}

val coverageExcludes = listOf(
    "pl/dudios/planner/PlannerApplication.class",
    "**/model/**",
    "**/dto/**",
    "**/config/**",
    "**/exception/*.class"
)

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("compileJava"))
    mustRunAfter(tasks.named("test"), tasks.named("integrationTest"))
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(coverageExcludes) }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    finalizedBy("copyJacocoForQodana")
}

tasks.register<Copy>("copyJacocoForQodana") {
    val xmlReport = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
    onlyIf { xmlReport.get().asFile.exists() }
    from(xmlReport)
    into(rootProject.layout.projectDirectory.dir(".qodana/code-coverage"))
    rename { "jacoco.xml" }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(coverageExcludes) }
        })
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.60")
            }
        }
    }
}

tasks.register<Copy>("copyFrontend") {
    val frontendProject = project(projects.frontend.path)
    dependsOn(frontendProject.tasks.named("build"))

    val frontendDist = frontendProject.file("dist/frontend/browser")

    from(frontendDist)
    into(layout.buildDirectory.dir("resources/main/static"))

    doFirst {
        if (!frontendDist.exists() || !frontendDist.isDirectory) {
            throw GradleException("Missing frontend dist directory: $frontendDist. Did ':frontend:build' produce it?")
        }
    }
}

tasks.named("processResources") {
    dependsOn(tasks.named("copyFrontend"))
}
