package me.uwuaden.kotlinplugin.skillSystem

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.itemManager.ItemManager
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

object SkillManager {
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.forEach { player ->
                if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}${ChatColor.BOLD}Divine Sword") {
                    player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 5, 0, false, false))
                    player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20, 0, false, false))
                }
            }
        }, 0, 2)
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.forEach { player ->
                val item = player.inventory.itemInMainHand
                if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}${ChatColor.BOLD}Prototype E-XI") {
                    val before = getChargeValue(item)
                    if (before - 2 > 0) {
                        changeChargeValue(item, before - 2)
                    } else {
                        changeChargeValue(item, 0)
                    }
                }
            }
        }, 0, 10)
    }
    private fun ItemStack.addEliteItemLore(cap: Int, maxUse: Int, type: String): ItemStack {
        val cloneItem = clone()
        val meta = cloneItem.itemMeta
        val itemLore = meta.lore?: return cloneItem
        val addList = mutableListOf("${ChatColor.DARK_GRAY}Charge Capacity: $cap", "${ChatColor.DARK_GRAY}Max Use: $maxUse")
        when (type) {
            "nature" -> addList.add("${ChatColor.DARK_GRAY}[🍀] Nature")
            "divinity" -> addList.add("${ChatColor.DARK_GRAY}[🛡] Divinity")
            "chaos" -> addList.add("${ChatColor.DARK_GRAY}[🧨] Chaos")
            "tech" -> addList.add("${ChatColor.DARK_GRAY}[⚙] Tech")
            else -> addList.add("NULL")
        }
        itemLore.addAll(0, addList)
        cloneItem.itemMeta = meta
        return cloneItem
    }
    fun initData() {
        skillItem[0] = ItemManager.createNamedItem(Material.LIGHT_BLUE_DYE, 1, "${ChatColor.AQUA}${ChatColor.BOLD}반중력 큐브 V2", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 400", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[⚙] Tech", "${ChatColor.GRAY}재사용 가능한 반중력 큐브입니다! 사용시 보는 방향으로 자신과 상대를 밀어냅니다.", " ", "${ChatColor.GRAY}Gadget"))
        skillItem[1] = ItemManager.createNamedItem(Material.GOLDEN_CARROT, 1, "${ChatColor.GOLD}Golden Carrot", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 200", "${ChatColor.DARK_GRAY}Max Use: 3", "${ChatColor.DARK_GRAY}[🍀] Nature", "${ChatColor.GRAY}황금당근을 3배로 즐겨보세요!", " ", "${ChatColor.GRAY}Gadget"))
        skillItem[2] = CustomItemData.getDivinityShield().addEliteItemLore(250, 2, "divinity")
        skillItem[3] = ItemManager.createNamedItem(Material.GOLDEN_APPLE, 1, "${ChatColor.GOLD}황금사과", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 100", "${ChatColor.DARK_GRAY}Max Use: 5", "${ChatColor.DARK_GRAY}[🍀] Nature", "${ChatColor.GRAY}평범한 황금사과입니다.", " ", "${ChatColor.GRAY}Gadget"))
        skillItem[4] = ItemManager.createNamedItem(Material.RED_DYE, 1, "${ChatColor.RED}${ChatColor.BOLD}ILLUSIONIZE", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 500", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[🧨] Chaos", "${ChatColor.GRAY}바라본 위치에 넓은 범위 안에 있는 플레이어에게 대미지를 주고, 그 플레이어와 위치를 바꿉니다.", "${ChatColor.GRAY}쿨타임: 30초", " ", "${ChatColor.GRAY}Gadget"))
        skillItem[5] = ItemManager.createNamedItem(Material.IRON_SWORD, 1, "${ChatColor.AQUA}${ChatColor.BOLD}Divine Sword", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 500", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[🛡] Divinity", "${ChatColor.GRAY}들고 있는 동안 신속1을 얻는 대신 나약함2를 받습니다.", " ", "${ChatColor.GRAY}Gadget"))
        skillItem[6] = ItemManager.createNamedItem(Material.REDSTONE_TORCH, 1, "${ChatColor.RED}Flare Gun", listOf("${ChatColor.DARK_GRAY}Charge Capacity: 500", "${ChatColor.DARK_GRAY}Max Use: 1", "${ChatColor.DARK_GRAY}[⚙] Tech", "${ChatColor.GRAY}하늘에 발사시", "${ChatColor.GRAY}보급품이 떨어집니다!", " ", "${ChatColor.GRAY}보급품에 깔리지 않게 조심하세요!"))
        skillItem[7] = CustomItemData.getTeleportLeggings()
        skillItem[8] = CustomItemData.getStinger()
    }
    fun changeChargeValue(item: ItemStack, new: Int) {
        val lores = item.itemMeta.lore ?: return
        lores.replaceAll { if (it.contains("Charge:")) "${ChatColor.DARK_AQUA}Charge: ${new}" else it }
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
        lores.replaceAll { if (it.contains("Saved:")) "${ChatColor.DARK_AQUA}Saved: ${new}" else it }
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
        val inv = Bukkit.createInventory(holder, invSlotSize, "skills")



        for (i in 0 until invSlotSize) {
            val item = ItemManager.createNamedItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", null)
            inv.setItem(i, item)
        }
        for (i in 0 until 9) {
            val item = ItemManager.createNamedItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, " ", null)
            inv.setItem(i, item)
        }
        for (i in invSlotSize-9 until invSlotSize) {
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
                        addLoreLine(itemClone, "${ChatColor.GREEN}${ChatColor.BOLD}선택됨.")
                    }

                    addLoreLine(itemClone, " ")

                    if (playerEItemList[player?.uniqueId]?.eliteItems?.contains(id) != true) {
                        addLoreLine(itemClone, "${ChatColor.YELLOW}Locked")
                        addLoreLine(itemClone, " ")
                        addLoreLine(itemClone, "${ChatColor.YELLOW}구매: 5000코인")

                    }
                    addLoreLine(itemClone, " ")
                    if (playerEItemList[player?.uniqueId]?.eliteItems?.contains(id) != true) {
                        addLoreLine(itemClone, "${ChatColor.YELLOW}${ChatColor.BOLD}Shift+Click ${ChatColor.GREEN}to Buy")
                    } else {
                        addLoreLine(itemClone, "${ChatColor.YELLOW}${ChatColor.BOLD}Click ${ChatColor.GREEN}to Equip")
                    }
                    addLoreLine(itemClone, " ")
                    addLoreLine(itemClone, "${ChatColor.DARK_GRAY}Skill")
                    addLoreLine(itemClone, "${ChatColor.DARK_GRAY}ID: $id")




                    inv.setItem(id+9, itemClone)
                }
            }
        }


        if (createdSkillList.size - invSlotSize*(page+1) >= 0) {
            inv.setItem(invSlotSize-4, ItemManager.createNamedItem(Material.ARROW, 1, "${ChatColor.GREEN}Next Page", null))
        }
        inv.setItem(invSlotSize-5, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, "${ChatColor.GREEN}Page: $page", null))
        if (createdSkillList.size - invSlotSize*(page-1) >= 0 && page > 0) {
            inv.setItem(invSlotSize-6, ItemManager.createNamedItem(Material.ARROW, 1, "${ChatColor.GREEN}Previous Page", null))
        }

        return inv
    }

    fun getItemCharge(id: Int): Pair<Int, Int>? {
        val item = skillItem[id] ?: return null
        val lores = item.itemMeta.lore?: mutableListOf()

        val capacity = lores.filter { it.contains("Charge Capacity:") }[0].split(": ")[1].trim().toInt()
        val maxUse = lores.filter { it.contains("Max Use:") }[0].split(": ")[1].trim().toInt()

        return Pair(capacity, maxUse) //Cap, Max Use
    }
    fun createPercentageBar(percentage: Double, length: Int): String {
        require(percentage in 0.0..100.0) { "백분율은 0에서 100 사이여야 합니다." }

        val barLength = length.coerceIn(0, 100) // 막대 길이를 0에서 100 사이로 제한
        val redSquareCount = (percentage * barLength / 100.0).toInt()
        val blackSquareCount = barLength - redSquareCount

        val aquaSquare = "${ChatColor.AQUA}■"
        val blackSquare = "${ChatColor.GRAY}■"

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

        player.sendActionBar(Component.text("${ChatColor.DARK_AQUA}CP: ${createPercentageBar(((playerCapacityPoint[player.uniqueId]!!.toDouble()/chargeData.first.toDouble())*100).coerceIn(0.0, 100.0), 10)} ${ChatColor.WHITE}(${(playerCapacityPoint[player.uniqueId]?:0).coerceIn(0, chargeData.first)}/${chargeData.first}) +${point} ${ChatColor.AQUA}(${playerMaxUse[player.uniqueId] ?: 0}/${chargeData.second})"))


        if ((playerCapacityPoint[player.uniqueId] ?: 0) >= chargeData.first) {
            playerCapacityPoint[player.uniqueId] = 0

            playerMaxUse[player.uniqueId] = (playerMaxUse[player.uniqueId] ?: 0) + 1

            val failedItems = player.inventory.addItem(skillItem[itemID] ?: return).values
            failedItems.forEach {
                player.world.dropItem(player.eyeLocation, it)
            }
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F)
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F)

            player.sendMessage("${ChatColor.GREEN}엘리트 아이템을 획득했습니다!")
        }
    }
}