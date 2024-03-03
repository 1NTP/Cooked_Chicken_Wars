package me.uwuaden.kotlinplugin.skillSystem

import me.uwuaden.kotlinplugin.Main.Companion.econ
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.ItemManipulator.getName
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemManager
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerCapacityPoint
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerEItem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerEItemList
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerMaxUse
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.skillItem
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


private fun addLoreLine(item: ItemStack, loreLine: String) {
    val itemMeta: ItemMeta = item.itemMeta ?: return
    val currentLore: MutableList<String> = itemMeta.lore ?: mutableListOf()

    currentLore.add(ChatColor.RESET.toString() + loreLine)

    itemMeta.lore = currentLore
    item.itemMeta = itemMeta
}
private fun mainHandItemName(p: Player): String {
    return (p.inventory.itemInMainHand.itemMeta?.displayName ?: "")
}

object SkillManager {
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {

            plugin.server.onlinePlayers.filter { mainHandItemName(it) == CustomItemData.getDevineSword().getName() }.forEach { player ->
                player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 15, 0, false, false))
            }
            plugin.server.onlinePlayers.filter { mainHandItemName(it) == CustomItemData.getSwordOfHealing().getName() }.forEach { player ->
                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 15, 0, false, false))
            }
            plugin.server.onlinePlayers.filter { mainHandItemName(it) == CustomItemData.getLiberation().getName() }.forEach { player ->
                val playerCount = player.location.getNearbyPlayers(10.0).filter { it != player }.filter { CustomItemManager.isHittable(player, it) }.size + 1
                player.sendActionBar(Component.text("§7주변 플레이어 수: $playerCount"))
            }
        }, 0, 10)
    }
    private fun ItemStack.addEliteItemLore(cap: Int, maxUse: Int, type: String): ItemStack {
        val cloneItem = this.clone()
        val meta = cloneItem.itemMeta
        val itemLore = meta.lore?: mutableListOf()
        val addList = mutableListOf("§8Charge Capacity: $cap", "§8Max Use: $maxUse")
        when (type) {
            "nature" -> addList.add("§8[🍀] Nature")
            "divinity" -> addList.add("§8[🛡] Divinity")
            "chaos" -> addList.add("§8[🧨] Chaos")
            "tech" -> addList.add("§8[⚙] Tech")
            else -> addList.add("NULL")
        }
        itemLore.addAll(0, addList)
        meta.lore = itemLore
        cloneItem.itemMeta = meta
        return cloneItem
    }
    fun ItemStack.removeEliteItemLore(): ItemStack {
        val cloneItem = this.clone()
        val meta = cloneItem.itemMeta
        val itemLore = meta.lore?: mutableListOf()

        itemLore.removeIf { it.contains("Charge Capacity:") }
        itemLore.removeIf { it.contains("Max Use:") }
        itemLore.removeIf { it.contains("[🍀]") }
        itemLore.removeIf { it.contains("[🛡]") }
        itemLore.removeIf { it.contains("[🧨]") }
        itemLore.removeIf { it.contains("[⚙]") }

        meta.lore = itemLore
        cloneItem.itemMeta = meta

        return cloneItem
    }
    fun initData() {
        skillItem[0] = ItemManager.createNamedItem(Material.LIGHT_BLUE_DYE, 1, "§b§l반중력 큐브 V2", listOf("§8Charge Capacity: 500", "§8Max Use: 1", "§8[⚙] Tech", "§7재사용 가능한 반중력 큐브입니다! 사용시 보는 방향으로 자신과 상대를 밀어냅니다.", "§2쿨타임: 20초", " ", "§7Gadget"))
        skillItem[1] = CustomItemData.getGoldenCarrot().addEliteItemLore(200, 5, "nature")
        skillItem[2] = CustomItemData.getDivinityShield().addEliteItemLore(250, 2, "divinity")
        skillItem[3] = ItemStack(Material.GOLDEN_APPLE).addEliteItemLore(100, 10, "nature")
        skillItem[4] = ItemManager.createNamedItem(Material.RED_DYE, 1, "§c§lILLUSIONIZE", listOf("§8Charge Capacity: 500", "§8Max Use: 1", "§8[🧨] Chaos", "§7바라본 위치에 넓은 범위 안에 있는 플레이어에게 대미지를 주고, 그 플레이어와 위치를 바꿉니다.", "§7쿨타임: 30초", " ", "§7Gadget"))
        skillItem[5] = CustomItemData.getDevineSword().addEliteItemLore(600, 1, "divinity")
        skillItem[6] = CustomItemData.getFlareGun().addEliteItemLore(700, 1, "tech")
        skillItem[7] = CustomItemData.getTeleportLeggings()
        skillItem[8] = CustomItemData.getStinger()
        skillItem[9] = CustomItemData.getBookOfMastery().addEliteItemLore(1000, 1, "divinity")
        skillItem[10] = CustomItemData.getBookOfSalvation().addEliteItemLore(50, 1, "divinity")
        skillItem[11] = CustomItemData.getSwordOfHealing().addEliteItemLore(400, 1, "divinity")
        skillItem[12] = CustomItemData.getShotGun().addEliteItemLore(300, 1, "tech")
        skillItem[13] = CustomItemData.getQuickRocketLauncher().addEliteItemLore(500, 1, "tech")
        skillItem[14] = CustomItemData.getGravitization().addEliteItemLore(400, 1, "chaos")
        skillItem[15] = CustomItemData.getOverFlow().addEliteItemLore(500, 1, "chaos")
        skillItem[16] = CustomItemData.getBowOfEternity().addEliteItemLore(600, 1, "divinity")
        skillItem[17] = CustomItemData.getLiberation().addEliteItemLore(700, 1, "chaos")
        skillItem[18] = CustomItemData.getAltar().addEliteItemLore(100, 2, "nature")
    }
    fun changeChargeValue(item: ItemStack, new: Int) {
        val lores = item.itemMeta.lore ?: return
        lores.replaceAll { if (it.contains("Charge:")) "§3Charge: ${new}" else it }
        val m = item.itemMeta
        m.lore = lores
        item.itemMeta = m
    }
    fun getChargeValue(item: ItemStack): Int {
        val lores = item.itemMeta.lore ?: return 0
        lores.forEach {
            if (it.contains("Charge:")) {
                return ("§r$it").split(":")[1].trim().toInt()
            }
        }
        return 0
    }
    fun changeSaveValue(item: ItemStack, new: Int) {
        val lores = item.itemMeta.lore ?: return
        lores.replaceAll { if (it.contains("Saved:")) "§3Saved: ${new}" else it }
        val m = item.itemMeta
        m.lore = lores
        item.itemMeta = m
    }
    fun getSaveValue(item: ItemStack): Int {
        val lores = item.itemMeta.lore ?: return 0
        lores.forEach {
            if (it.contains("Saved:")) {
                return ("§r$it").split(":")[1].trim().toInt()
            }
        }
        return 0
    }
    fun inv(holder: InventoryHolder, page: Int, player: Player?): Inventory {
        val createdSkillList = skillItem.keys
        val invSlotSize = 54
        val inv = Bukkit.createInventory(holder, invSlotSize, "Elite Item")



        for (i in 0 until invSlotSize) {
            val item = ItemManager.createNamedItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", null)
            inv.setItem(i, item)
        }
        for (i in 0 until 9) {
            val item = ItemManager.createNamedItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, " ", null)
            inv.setItem(i, item)
        }
        for (i in invSlotSize-9 until invSlotSize) { //playerCoin
            val item = ItemManager.createNamedItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, " ", null)
            inv.setItem(i, item)
        }


        val startSlot = invSlotSize*(page)
        val endSlot = invSlotSize*(page+1)


        if (startSlot < 0) return inv
        if (endSlot < 0) return inv

        for (id in startSlot..endSlot) {
            if (createdSkillList.contains(id)) {
                val item = skillItem[id]
                if (item != null) {
                    val itemClone = item.clone()
                    val meta = itemClone.itemMeta

                    if (playerEItem[player?.uniqueId] == id) {
                        meta.addEnchant(Enchantment.DURABILITY, 1, true)
                        itemClone.itemMeta = meta
                        addLoreLine(itemClone, " ")
                        addLoreLine(itemClone, "§a§l선택됨.")
                    }

                    addLoreLine(itemClone, " ")

                    if (playerEItemList[player?.uniqueId]?.eliteItems?.contains(id) != true) {
                        addLoreLine(itemClone, "§eLocked")
                        addLoreLine(itemClone, " ")
                        addLoreLine(itemClone, "§e구매: 5000코인")

                    }
                    addLoreLine(itemClone, " ")
                    if (playerEItemList[player?.uniqueId]?.eliteItems?.contains(id) != true) {
                        addLoreLine(itemClone, "§e§lShift+Click §ato Buy")
                    } else {
                        addLoreLine(itemClone, "§e§lClick §ato Equip")
                    }
                    addLoreLine(itemClone, " ")
                    addLoreLine(itemClone, "§8Elite Item")
                    addLoreLine(itemClone, "§8ID: $id")




                    inv.setItem(id+9, itemClone)
                }
            }
        }
        val itemM = ItemManager.createNamedItem(Material.LIME_DYE, 1, "§aMoney: ${econ.getBalance(player)}", null)
        val itemH = ItemManager.createNamedItem(Material.REDSTONE_TORCH, 1, "§a도움말", listOf("§7아이템을 파밍하거나 플레이어 킬을 하면, Charge Capacity라는 포인트를 획득합니다. (이하 CC)", "§7지정된 CC를 전부 채우면, 선택한 아이템을 얻을 수 있습니다.", "§7플레이어 킬이 3킬 이상인 경우, 아이템을 파밍했을 때 CC를 얻을 수 없습니다."))
        inv.setItem(8, itemM)
        inv.setItem(invSlotSize-1, itemH)

        if (createdSkillList.size - invSlotSize*(page+1) >= 0) {
            inv.setItem(invSlotSize-4, ItemManager.createNamedItem(Material.ARROW, 1, "§aNext Page", null))
        }
        inv.setItem(invSlotSize-5, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, "§aPage: $page", null))
        if (createdSkillList.size - invSlotSize*(page-1) >= 0 && page > 0) {
            inv.setItem(invSlotSize-6, ItemManager.createNamedItem(Material.ARROW, 1, "§aPrevious Page", null))
        }

        return inv
    }

    fun getItemCharge(id: Int): Pair<Int, Int>? {
        val item = skillItem[id] ?: return null
        val lores = item.itemMeta.lore?: mutableListOf()
        if (lores.isEmpty()) {
            println(lores)
            return Pair(0, 0)
        }
        val capacity = lores.filter { it.contains("§8Charge Capacity:") }[0].split(": ")[1].trim().toInt()
        val maxUse = lores.filter { it.contains("§8Max Use:") }[0].split(": ")[1].trim().toInt()

        return Pair(capacity, maxUse) //Cap, Max Use
    }
    fun createPercentageBar(percentage: Double, length: Int): String {
        require(percentage in 0.0..100.0) { "백분율은 0에서 100 사이여야 합니다." }

        val barLength = length.coerceIn(0, 100) // 막대 길이를 0에서 100 사이로 제한
        val redSquareCount = (percentage * barLength / 100.0).toInt()
        val blackSquareCount = barLength - redSquareCount

        val aquaSquare = "§b■"
        val blackSquare = "§7■"

        var percentageBar = ""

        for (i in 0 until redSquareCount) {
            percentageBar += aquaSquare
        }

        for (i in 0 until blackSquareCount) {
            percentageBar += blackSquare
        }

        return percentageBar
    }

    fun addCapacityPoint(player: Player, point: Int) {
        val itemID = playerEItem[player.uniqueId] ?: return
        val chargeData = getItemCharge(itemID) ?: return

        if ((playerMaxUse[player.uniqueId] ?: 0) >= chargeData.second) return
        if ((playerCapacityPoint[player.uniqueId] ?: 0) >= chargeData.first) return

        playerCapacityPoint[player.uniqueId] = (playerCapacityPoint[player.uniqueId] ?: 0) + point

        player.sendActionBar(Component.text("§3CP: ${createPercentageBar(((playerCapacityPoint[player.uniqueId]!!.toDouble()/chargeData.first.toDouble())*100).coerceIn(0.0, 100.0), 10)} §f(${(playerCapacityPoint[player.uniqueId]?:0).coerceIn(0, chargeData.first)}/${chargeData.first}) +${point} §b(${playerMaxUse[player.uniqueId] ?: 0}/${chargeData.second})"))


        if ((playerCapacityPoint[player.uniqueId] ?: 0) >= chargeData.first) {
            playerCapacityPoint[player.uniqueId] = 0

            playerMaxUse[player.uniqueId] = (playerMaxUse[player.uniqueId] ?: 0) + 1

            val failedItems = player.inventory.addItem((skillItem[itemID] ?: return).removeEliteItemLore()).values //Todo:
            failedItems.forEach {
                player.world.dropItem(player.eyeLocation, it)
            }
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F)
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F)

            player.sendMessage("§a엘리트 아이템을 획득했습니다!")
        }
    }
}