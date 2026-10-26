rootProject.name = "nine-client"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Compose Multiplatform опирается на библиотеки androidx, которые лежат только в Google Maven.
        // Фильтр не даёт этому репозиторию подсунуть что-нибудь, кроме них.
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
    }
}

include(":composeApp")
