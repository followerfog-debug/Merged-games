pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MergedGames"
include(":app")

// third_party/minosoft is NOT included here yet: it is a desktop (LWJGL/GLFW/JavaFX) project
// with its own Gradle build. See PORTING.md for the plan to turn it into an Android library module.
