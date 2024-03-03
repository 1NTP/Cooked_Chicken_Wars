package me.uwuaden.kotlinplugin.gui

import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MenuEvent: Listener {
    @EventHandler
    fun invClick(e: InventoryClickEvent) {
        val inv = e.view.topInventory
        val player = e.view.player as Player
        val slot = e.slot
        if (e.inventory != inv) return
        if (e.view.title != "§e§lProelium Menu") return

        e.isCancelled = true

        if (slot == 20) {
            player.performCommand("proelium 가이드북")
        } else if (slot == 21) {
            player.performCommand("proelium 디스코드")
            player.closeInventory()
        } else if (slot == 22) {
            player.performCommand("proelium 랭크")
        } else if (slot == 23) {
            player.performCommand("eliteitem")
        } else if (slot == 24) {
            player.performCommand("quickslot")
        } else {
            return
        }
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
    }
}