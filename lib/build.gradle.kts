plugins {
    // Apply the shared KMP build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-multiplatform.gradle.kts`.
    id("buildsrc.convention.kotlin-multiplatform")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    id("maven-publish")
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "dev.mooner"
version = "1.0.0"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
                implementation(libs.bundles.kotlinxEcosystem)
            }
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = project.group.toString()
            artifactId = "dotenv-kmp"
            version = project.version.toString()
        }
    }
}
