plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "org.showen"
version = "2026.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1.4.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // This is crucial for @Service annotation scanning and Java-related APIs.
        bundledPlugin("com.intellij.java")
    }
    implementation("org.jacoco:org.jacoco.core:0.8.12")
    // Include JaCoCo agent runtime jar for distribution
    implementation("org.jacoco:org.jacoco.agent:0.8.12:runtime")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            <ul>
              <li>Initial paid release (2026.2)</li>
              <li>Real-time JaCoCo coverage highlighting with auto JVM agent injection</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    
    // Copy JaCoCo agent jar to resources for runtime extraction
    val copyAgentJar by registering(Copy::class) {
        from(configurations.runtimeClasspath.get().filter { it.name.contains("org.jacoco.agent") && it.name.contains("runtime") })
        into("src/main/resources/lib")
        rename { "org.jacoco.agent-0.8.12-runtime.jar" }
    }
    
    processResources {
        dependsOn(copyAgentJar)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
sourceSets {
    main {
        java.srcDirs("src/main/java")
        kotlin.srcDirs("src/main/kotlin")
    }
}