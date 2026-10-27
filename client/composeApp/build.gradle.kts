import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
}

kotlin {
    // For now the client is built for desktop only. Android and iOS will be added alongside
    // as new targets, and all the code in commonMain is reused unchanged.
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Needed so that coroutines work on the Swing/Skia UI thread.
                implementation(libs.kotlinx.coroutines.swing)
                // Argon2id for the key from the access code and Ed25519 for the sign-in key: the JDK has neither.
                implementation(libs.bouncycastle)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "app.nine.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "app.nine.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Nine"
            packageVersion = "1.0.0"
        }
    }
}
