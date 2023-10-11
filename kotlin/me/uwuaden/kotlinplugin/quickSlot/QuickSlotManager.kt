package me.uwuaden.kotlinplugin.quickSlot

import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.quickSlot.QuickSlotEvent.Companion.playerQuickSlot
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

private fun addLoreLine(item: ItemStack, loreLine: String) {
    val itemMeta: ItemMeta = item.itemMeta ?: return
    val currentLore: MutableList<String> = itemMeta.lore ?: mutableListOf()

    currentLore.add(ChatColor.RESET.toString() + loreLine)

    itemMeta.lore = currentLore
    item.itemMeta = itemMeta
}

object QuickSlotManager {
    fun inv(player: Player): Inventory {
        val inv = Bukkit.createInventory(QuickSlotInvHolder(), 45, "${ChatColor.DARK_GRAY}Quick Slot Manager")
        for (i in 0 until 9) inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        for (i in 18 until 27) inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        for (i in 36 until 45) inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        inv.setItem(44, ItemManager.createNamedItem(Material.REDSTONE_TORCH, 1, "${ChatColor.RED}도움말", listOf("${ChatColor.GRAY}/닭갈비 퀵슬롯 (숫자) 명령어로 퀵슬롯을 지정할 수 있습니다!", "${ChatColor.GRAY}쉬프트 + F(양손 들기 키)로 이 메뉴를 열 수 있습니다.")))

        val data = initData(player.uniqueId)

        data.slotData.forEach { (slot, item) ->
            val itemUsing = item.clone()
            itemUsing.lore?.clear()
            addLoreLine(itemUsing, "${ChatColor.GREEN}Click to Quick Equip")
            if (slot <= 8) {
                val invSlot = slot + 9

                inv.setItem(invSlot, itemUsing)
            } else {
                val invSlot = slot + 18
                inv.setItem(invSlot, itemUsing)
            }
        }

        return inv
    }

    fun setQuickSlot(player: Player, slot: Int) {
        val data = initData(player.uniqueId)
        if (slot !in 0..17) {
            player.sendMessage("${ChatColor.RED}슬롯은 0부터 17사이의 숫자여야 합니다.")
            return
        }
        if (player.inventory.itemInMainHand.type == Material.AIR) {
            player.sendMessage("${ChatColor.RED}퀵슬롯 ${slot}번이 삭제되었습니다.")
            data.slotData.remove(slot)
            return
        }
        val item = player.inventory.itemInMainHand.clone()
        if (item.itemMeta.displayName().toString().contains("/-&-/")) {
            player.sendMessage("${ChatColor.RED}이름에 /-&-/기호가 들어가면 안됩니다.")
            return
        }
        if (item.itemMeta.displayName().toString().contains("/-:-/")) {
            player.sendMessage("${ChatColor.RED}이름에 /-:-/기호가 들어가면 안됩니다.")
            return
        }
        if (item.itemMeta.displayName().toString().contains("/-,-/")) {
            player.sendMessage("${ChatColor.RED}이름에 /-,-/기호가 들어가면 안됩니다.")
            return
        }

        item.itemMeta.lore?.clear()
        item.amount = 1
        data.slotData[slot] = item

        player.sendMessage("${ChatColor.GREEN}해당 아이템이 퀵슬롯 ${slot}번에 추가되었습니다.")
    }
    fun initData(playerUUID: UUID): PlayerQuickSlotData {
        if (playerQuickSlot[playerUUID] == null) {
            playerQuickSlot[playerUUID] = PlayerQuickSlotData(HashMap())
        }
        return playerQuickSlot[playerUUID]!!
    }

}