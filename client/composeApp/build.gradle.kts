import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
}

kotlin {
    // Сейчас клиент собирается только для ПК. Android и iOS добавятся рядом
    // как новые цели, весь код из commonMain переиспользуется без изменений.
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
                // Нужен, чтобы корутины работали на потоке интерфейса Swing/Skia.
                implementation(libs.kotlinx.coroutines.swing)
                // Argon2id для ключа из кода доступа и Ed25519 для ключа входа: в JDK нет ни того, ни другого.
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
