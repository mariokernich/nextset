// NextSet for Android: the phone app, the Wear OS app and what both share.
// `core` holds the timer logic, storage, sounds, haptics, the phone/watch sync
// and the drawing of the ring and the logo; its unit tests run on the JVM
// without an emulator. `app` is the phone, `wear` the watch.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nextset-android"

include(":core", ":app", ":wear")
