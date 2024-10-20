import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    id("org.jetbrains.compose") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.vanniktech.maven.publish") version "0.30.0"
}


group = "com.kdroid.knotify"
version = "0.2.0"

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.kmplog)
            implementation("net.java.dev.jna:jna:5.15.0")
            implementation("net.java.dev.jna:jna-platform:5.15.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation(compose.desktop.currentOs)


        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junitApi)
            runtimeOnly(libs.junitEngine)
            implementation(libs.mockk)
        }

    }

}

tasks.withType<Test> {
    useJUnitPlatform()
}


mavenPublishing {
    coordinates(
        groupId = "io.github.kdroidfilter",
        artifactId = "knotify",
        version = version.toString()
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("KNotify")
        description.set("KNotify is a Kotlin library for sending native notifications across different operating systems. It provides a simple, unified interface for sending notifications on Linux, Windows, and macOS.")
        inceptionYear.set("2024")
        url.set("https://github.com/kdroidFilter/KNotify")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        // Specify developers information
        developers {
            developer {
                id.set("kdroidfilter")
                name.set("Elyahou Hadass")
                email.set("elyahou.hadass@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/kdroidFilter/KNotify")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)


    // Enable GPG signing for all publications
    signAllPublications()
}


task("testClasses") {}

compose.desktop {
    application {
        mainClass = "com.kdroid.composenotification.demo.ComposeAppKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "knotify"
            packageVersion = version.toString()
        }
    }
}