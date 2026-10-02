pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9-beta.2"
}

stonecutter {
    create(rootProject) {
        version("1.21.11-fabric", "1.21.11")
        version("1.21.11-neoforge", "1.21.11")
        version("26.1-fabric", "26.1")
        version("26.1-neoforge", "26.1")
        version("26.2-fabric", "26.2")
        version("26.2-neoforge", "26.2")
        version("26.3-fabric", "26.3")
        version("26.3-neoforge", "26.3")
        vcsVersion = "26.1-fabric"
    }
}

rootProject.name = "Bedrock Skins"
