package com.brandonitaly.bedrockskins.client

import net.minecraft.client.MinecraftClient

object SkinManager {
    // Map of Player UUID (String) to Skin Key (packName:skinName) for runtime only
    private val playerSkins = mutableMapOf<String, String>()
    // Load the saved local player's skin from the shared state file and store it in the runtime map
    fun load() {
        playerSkins.clear()
        try {
            val state = StateManager.readState()
            val selected = state.selected
            val client = MinecraftClient.getInstance()
            val player = client.player
            if (!selected.isNullOrEmpty() && player != null) {
                playerSkins[player.uuid.toString()] = selected
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Return the selected skin key for the local player, if any.
    fun getLocalSelectedKey(): String? {
        val client = MinecraftClient.getInstance()
        val localUuid = client.player?.uuid?.toString() ?: return null
        return playerSkins[localUuid]
    }

    fun setSkin(uuid: String, packName: String, skinName: String) {
        val key = "$packName:$skinName"
        playerSkins[uuid] = key
        val client = MinecraftClient.getInstance()
        val localUuid = client.player?.uuid?.toString()
        if (localUuid != null && localUuid == uuid) {
            // Persist combined state (favorites + selected)
            try {
                val favorites = FavoritesManager.getFavoriteKeys()
                StateManager.saveState(favorites, key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Set a preview-only skin mapping without persisting state.
    fun setPreviewSkin(uuid: String, packName: String, skinName: String) {
        val key = "$packName:$skinName"
        playerSkins[uuid] = key
    }

    // Remove a preview-only skin mapping without persisting state.
    fun resetPreviewSkin(uuid: String) {
        playerSkins.remove(uuid)
    }

    @JvmStatic
    fun getSkin(uuid: String): String? {
        return playerSkins[uuid]
    }

    fun resetSkin(uuid: String) {
        if (playerSkins.remove(uuid) != null) {
            val client = MinecraftClient.getInstance()
            val localUuid = client.player?.uuid?.toString()
            if (localUuid != null && localUuid == uuid) {
                try {
                    val favorites = FavoritesManager.getFavoriteKeys()
                    StateManager.saveState(favorites, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
