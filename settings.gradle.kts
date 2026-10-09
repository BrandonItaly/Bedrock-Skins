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
        for (minecraft in listOf("1.21.11", "26.1", "26.2", "26.3")) {
            for (loader in listOf("fabric", "neoforge")) {
                version("$minecraft-$loader", minecraft)
            }
        }
        version("26.4-snapshot-2-fabric", "26.4-snapshot-2")
        vcsVersion = "26.1-fabric"
    }
}

rootProject.name = "Bedrock Skins"
