package me.uwuaden.kotlinplugin.gui

import me.uwuaden.kotlinplugin.itemManager.ItemManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

class CraftingMenuEvent: Listener {
    @EventHandler
    fun onMenuCloseEvent(e: InventoryCloseEvent) {
        val inv = e.view.topInventory
        val player = e.view.player as Player
        if (e.inventory != inv) return
        if (e.view.title == "§eEssence Extract Menu") {
            val items = player.inventory.addItem(inv.getItem(11) ?: return).values
            items.forEach {
                player.world.dropItem(player.eyeLocation, it)
            }
        }
    }

    @EventHandler
    fun onCraftingMenuClickEvent(e: InventoryClickEvent) {
        val inv = e.view.topInventory
        val player = e.view.player as Player
        val slot = e.slot
        if (e.inventory != inv) return

        when (e.view.title) {
            "§eCrafting Menu" -> {
                e.isCancelled = true
                if (slot == 10) {
                    CraftingMenu.openEssenceCrafting(player)
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                }
                if (slot == 16) {
                    player.performCommand("proelium anvil")
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                }
            }
            "§eEssence Crafting Menu" -> {
                e.isCancelled = true
                if (slot == 12) {
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                    CraftingMenu.openCraftingMenu(player)
                }
                if (slot == 14) {
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                    CraftingMenu.openExtract(player)
                }
            }
            "§eEssence Extract Menu" -> {
                if (e.clickedInventory == inv) {
                    if (slot != 11) e.isCancelled = true

                    if (slot == 15 && inv.getItem(slot) != null) {
                        val count = inv.getItem(slot)!!.amount
                        var str = ""
                        var genItem = false
                        val itemInUse = inv.getItem(11) ?: return
                        when (inv.getItem(slot)!!.type) {
                            Material.IRON_INGOT -> {
                                CraftingMenu.addEssence(player, "IRON", count)
                                str = "§fIron Essence +$count"
                                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                                if (CraftingMenu.isIronType(itemInUse)) {
                                    genItem = true
                                }
                            }

                            Material.DIAMOND -> {
                                CraftingMenu.addEssence(player, "DIAMOND", count)
                                str = "§bDiamond Essence +$count"
                                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                                if (CraftingMenu.isDiamondType(itemInUse)) {
                                    genItem = true
                                }
                            }

                            else -> {}
                        }
                        if (genItem) {
                            player.playSound(player, Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 2.0f)
                            player.sendMessage(Component.text(str))
                            inv.setItem(11, ItemStack(Material.AIR))
                            inv.setItem(15, ItemManager.createNamedItem(Material.BARRIER, 1, "§c결과물 없음!", listOf("§7왼쪽에 재료를 넣어주세요!", "§7철 장비를 Iron Essence로, 다이아몬드 장비는 Diamond Essence로", "§7변환시킬 수 있습니다!")))
                            inv.setItem(35, ItemManager.createNamedItem(Material.CHEST, 1, "§aEssence", CraftingMenu.getEssenceList(player)))
                        }
                    }
                }
            }
            "§eEssence Recipes" -> {
                e.isCancelled = true
                val item = inv.getItem(slot)
                if (item != null) {
                    val showing = inv.getItem(51)!!.type == Material.CRAFTING_TABLE
                    val sort = inv.getItem(50)!!.itemMeta.displayName == "§aSorted by Alphabet"
//                    var search = ""
                    //val name = ChatColor.stripColor(inv.getItem(45)!!.itemMeta.displayName)!!
//                    if (name != "§aClick to Search") {
//                        search = name.split(": ").lastOrNull() ?: ""
//                    }
                    if (slot == 50) {
                        CraftingMenu.openCraftingMenu(player, sortByChar = !sort, showCraftable = showing)
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                    }
                    if (slot == 51) {
                        CraftingMenu.openCraftingMenu(player, sortByChar = sort, showCraftable = !showing)
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                    }
                }
            }
        }
    }
}