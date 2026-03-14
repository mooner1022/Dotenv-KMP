plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.mavenPublish)
}

group = "dev.mooner"
version = "1.0.0"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.bundles.kotlinxEcosystem)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "dotenv-kmp",
        version = version.toString(),
    )

    pom {
        name = "Dotenv KMP"
        description = "Kotlin Multiplatform library for loading and parsing .env files."
        url = "https://github.com/mooner1022/Dotenv-KMP"
        inceptionYear = "2025"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "mooner1022"
                name = "mooner1022"
                url = "https://github.com/mooner1022"
            }
        }

        scm {
            connection = "scm:git:git://github.com/mooner1022/Dotenv-KMP.git"
            developerConnection = "scm:git:ssh://github.com/mooner1022/Dotenv-KMP.git"
            url = "https://github.com/mooner1022/Dotenv-KMP"
        }
    }
}
