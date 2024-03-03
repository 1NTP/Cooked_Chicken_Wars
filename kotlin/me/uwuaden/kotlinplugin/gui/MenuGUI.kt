package me.uwuaden.kotlinplugin.gui

import me.uwuaden.kotlinplugin.Main.Companion.econ
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addLores
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setName
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MenuGUI {
    fun openGUI(player: Player) {
        val inv = Bukkit.createInventory(null, 45, "§e§lProelium Menu")
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ISO_DATE
        val formatted = current.format(formatter)

        for (i in 0 until 45) inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        inv.setItem(20, ItemManager.createNamedItem(Material.WRITABLE_BOOK, 1, "§aItem Guide", listOf("§7GuideBook for unique items")))
        inv.setItem(21, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzM5ZWU3MTU0OTc5YjNmODc3MzVhMWM4YWMwODc4MTRiNzkyOGQwNTc2YTI2OTViYTAxZWQ2MTYzMTk0MjA0NSJ9fX0=")
            .setName("§3Discord").addLores(listOf("§aClick to Join Our Server!!")))
        inv.setItem(22, EffectManager.getPlayerSkull(player.uniqueId).setName("§a${player.name}").addLores(listOf("§aProelium Profile", "§7Click to Check Rank Tier", "§8${formatted}")))
        inv.setItem(23, ItemManager.createNamedItem(Material.DIAMOND_SWORD, 1, "§aElite Item", listOf("§7Click to Buy/Equip EliteItem")))
        inv.setItem(24, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "§aQuick Slot Manager", listOf("§7Click to Open QuickSlot Menu")))

        inv.setItem(8, ItemManager.createNamedItem(Material.LIME_DYE, 1, "§aCoin: ${econ.getBalance(player)}", listOf("§7Current Coin")))
        player.openInventory(inv)
    }


}