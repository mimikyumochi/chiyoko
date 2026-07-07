pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
}
stonecutter {
    create(rootProject) {
        versions("26.1", "26.2")
        vcsVersion = "26.2"
    }
}