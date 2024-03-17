package me.uwuaden.kotlinplugin

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class GuideBookEvent: Listener {
    @EventHandler
    fun onGuideBookInvClick(e: InventoryClickEvent) {
        val inventory = Bukkit.createInventory(null, 54, "§e§lItem Guide Book")
        val clickedInv = e.clickedInventory ?: return
        val slot = e.slot
        val player = e.view.player
        if (e.view.title != "§e§lItem Guide Book") return
        e.isCancelled = true

        if (e.isRightClick or e.isLeftClick && e.slot == 0) {
            GuideBookGUI.openFileDropInvNormal(player as Player)
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)

        } else if (e.isRightClick or e.isLeftClick && e.slot == 1) {
            GuideBookGUI.openFileDropInvUtill(player as Player)
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)

        } else if (e.isRightClick or e.isLeftClick && e.slot == 2) {
            GuideBookGUI.openFileDropInvFlare(player as Player)
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
        }
    }
}










