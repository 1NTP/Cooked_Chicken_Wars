package me.uwuaden.kotlinplugin.gameSystem

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object LobbyManager {
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.forEach { world ->
                if (world.name == "world") {
                    world.players.forEach { player ->
                        player.inventory.setItem(8, ItemManager.createNamedItem(Material.NETHER_STAR, 1, "§aMenu", listOf("§7Click to Open Menu")))
                    }
                }
                if (world.name.contains("Queue-")) {
                    world.players.forEach { player ->
                        if (player.location.y < 80.0) {
                            if (player.inventory.helmet == null) player.inventory.helmet =
                                ItemStack(Material.IRON_HELMET)
                            if (player.inventory.chestplate == null) player.inventory.chestplate =
                                ItemStack(Material.IRON_CHESTPLATE)
                            if (player.inventory.leggings == null) player.inventory.leggings =
                                ItemStack(Material.IRON_LEGGINGS)
                            if (player.inventory.boots == null) player.inventory.boots =
                                ItemStack(Material.IRON_BOOTS)
                            if (player.inventory.getItem(0) == null) player.inventory.setItem(0, ItemStack(Material.IRON_SWORD))
                        } else {
                            player.inventory.clear()
                        }
                    }
                }
            }
        }, 0, 10)
    }
}