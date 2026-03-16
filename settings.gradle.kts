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
        version("1.21.9-fabric", "1.21.9")
        version("1.21.9-neoforge", "1.21.9")
        version("1.21.10-fabric", "1.21.10")
        version("1.21.10-neoforge", "1.21.10")
        version("1.21.11-fabric", "1.21.11")
        version("1.21.11-neoforge", "1.21.11")
        version("26.1-fabric", "26.1-pre-2")
        vcsVersion = "1.21.11-fabric"
    }
}

rootProject.name = "Bedrock Skins"
