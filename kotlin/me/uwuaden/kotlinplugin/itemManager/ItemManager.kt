package me.uwuaden.kotlinplugin.itemManager

import me.uwuaden.kotlinplugin.Main.Companion.currentInv
import me.uwuaden.kotlinplugin.Main.Companion.droppedItems
import me.uwuaden.kotlinplugin.Main.Companion.inventoryData
import me.uwuaden.kotlinplugin.Main.Companion.isOpening
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.skillSystem.SkillManager
import net.kyori.adventure.text.Component

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import java.util.*
import java.util.logging.Level
import kotlin.random.Random


private fun hasInventory(p: Player, item: Material): Boolean {
    val invList = mutableSetOf<Material>()
    p.inventory.forEach {
        if (it != null) {
            invList.add(it.type)
        }
    }
    invList.add(p.inventory.itemInOffHand.type)

    return invList.contains(item)
}

private fun hasAnyBoots(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("boots") ?: false
}
private fun hasAnyLeggings(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("leggings") ?: false
}
private fun hasAnyChestplate(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("chestplate") ?: false
}
private fun hasAnyHelmet(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("helmet") ?: false
}
private fun hasAnySword(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("sword") ?: false
}
private fun hasAnyPickaxe(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("pickaxe") ?: false
}
private fun hasAnyAxe(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("axe") ?: false
}


private fun hasAnyBow(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("bow") ?: false
}
private fun hasAnyShield(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("shield") ?: false
}
private fun hasAnyFlareGun(player: Player): Boolean {
    val worldData = WorldManager.initData(player.world)
    return worldData.playerItemList[player.uniqueId]?.contains("flare_gun") ?: false
}

private fun probabilityTrue(n: Double): Boolean {
    require(n in 0.0..100.0) { "확률은 0에서 100 사이의 값이어야 합니다." }

    val randomValue = Random.nextDouble(0.0, 100.0)
    return randomValue < n
}



private fun addDroppedItemSlot(droppedItem: DroppedItem, item: ItemStack) {
    val emptySlots = ArrayList<Int>()
    for (i in 0 until 9*droppedItem.size) {
        emptySlots.add(i)
    }

    droppedItem.items.forEach { (k, v) ->
        emptySlots.remove(k)
    }

    droppedItem.itemGenerated = true
    droppedItem.items[emptySlots.random()] = item
}

object ItemManager {
    fun rangedWeaponList(): List<Material> {
        return listOf(Material.BOW, Material.CROSSBOW, Material.NETHERITE_SHOVEL, Material.DIAMOND_SHOVEL, Material.WOODEN_SHOVEL, Material.IRON_HOE)
    }

    fun isRangedWeapon(item: Material): Boolean {
        return rangedWeaponList().contains(item)
    }
     fun createNamedItem(material: Material, count: Int, name: String, lore: List<String>?): ItemStack {
        val returnItem = ItemStack(material, count)
        val itemMeta = returnItem.itemMeta
        itemMeta.setDisplayName(name)
        itemMeta.lore = lore
        returnItem.itemMeta = itemMeta

        return returnItem
    }
    fun addItemData(item: ItemStack, count: Int?, name: String?, lore: List<String>?): ItemStack {
        val returnItem = item.clone()
        if (count != null) returnItem.amount = count
        val itemMeta = returnItem.itemMeta
        if (name != null) itemMeta.setDisplayName(name)
        itemMeta.lore = lore
        returnItem.itemMeta = itemMeta
        return returnItem

    }
    fun createDroppedItem(location: Location, itemGen: Boolean, size: Int): DroppedItem {
        val dropped = DroppedItem(UUID.randomUUID(), location, isLocated = true, itemGenerated = itemGen, size = size)
        droppedItems.add(dropped)
        createDisplay(dropped)
        return dropped
    }

    fun openDroppedItem(player: Player, uuid: UUID) {
        if(isOpening.contains(uuid)) {
            player.sendMessage("§c이미 상호작용 중 입니다.")
            return
        }
        isOpening.add(uuid)
        val droppedItem = getDroppedItem(uuid)?: return

        val worldData = WorldManager.initData(player.world)
        if (worldData.playerItemList[player.uniqueId] == null) worldData.playerItemList[player.uniqueId] = mutableSetOf()
        //생성자
        val random = java.util.Random()
        if (!droppedItem.itemGenerated) {
            try {
                if ((worldData.playerKill[player.uniqueId] ?: 0) <= 0) SkillManager.addCapacityPoint(player, random.nextInt(10, 20))
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, e.message)
            }
            if (hasAnyFlareGun(player)) {
                if (probabilityTrue(0.5)) addDroppedItemSlot(droppedItem, createNamedItem(Material.REDSTONE_TORCH, 1, "§cFlare Gun", listOf("§7하늘에 발사시", "§7보급품이 떨어집니다!", " ", "§7보급품에 깔리지 않게 조심하세요!")))
            } else {
                if (probabilityTrue(2.0)) {
                    addDroppedItemSlot(droppedItem, createNamedItem(Material.REDSTONE_TORCH, 1, "§cFlare Gun", listOf("§7하늘에 발사시", "§7보급품이 떨어집니다!", " ", "§7보급품에 깔리지 않게 조심하세요!")))
                    worldData.playerItemList[player.uniqueId]!!.add("flare_gun")
                }
            }
            if (probabilityTrue(2.0)) {
                addDroppedItemSlot(droppedItem, CustomItemData.getEnergyDrink())
            }

            if (probabilityTrue(30.0)) {
                when (random.nextInt(0, 51)) {
                    in 0..0 -> addDroppedItemSlot(droppedItem, createNamedItem(Material.BLACK_DYE, 1, "§b컨버터", listOf("§7다이아몬드 장비를 대미지를 감소시키는 아이템으로 변환시킵니다.", "§7우클릭하여 컨버터 메뉴를 열 수 있습니다.")))
                    in 1..8 -> addDroppedItemSlot(droppedItem, createNamedItem(Material.DARK_OAK_BUTTON, 1, "§e영역 수류탄", listOf("§71회용*", "§7접착식 수류탄입니다.", "§7폭발시 건물을 붕괴시키는 지진을 일으키고 약한 대미지를 줍니다.", "§7폭발 위치보다 높은 곳의 건물만 파괴할 수 있습니다.", " ", "§2대미지: 1.0 x 3")))
                    in 9..16 -> addDroppedItemSlot(droppedItem, CustomItemData.getAGShotGun())
                    in 17..24 -> addDroppedItemSlot(droppedItem, CustomItemData.getGravityG())
                    in 25..28 -> addDroppedItemSlot(droppedItem, createNamedItem(Material.LIGHT_BLUE_DYE, 1, "§b반중력 큐브", listOf("§71회용*", "§7우클릭시 보는 방향의 자신과 주변 플레이어를 밀어냅니다.")))
                    in 29..36 -> addDroppedItemSlot(droppedItem, CustomItemData.getSmokeG())
                    in 37..38 -> addDroppedItemSlot(droppedItem, CustomItemData.getVallista())
                    in 39..40 -> addDroppedItemSlot(droppedItem, CustomItemData.getRevelation())
                    in 41..42 -> addDroppedItemSlot(droppedItem, CustomItemData.getRocketLauncher())
                    in 43..50 -> addDroppedItemSlot(droppedItem, CustomItemData.getMolt())
                }
            }
            var rn = random.nextInt(0, 19)


            val calibrateList = ArrayList<Int>()
            if (!hasAnySword(player)) calibrateList.add(0)

            if (!hasAnyHelmet(player)) calibrateList.add(1)
            if (!hasAnyChestplate(player)) calibrateList.add(2)
            if (!hasAnyLeggings(player)) calibrateList.add(3)
            if (!hasAnyBoots(player)) calibrateList.add(4)

            if (!hasAnyBow(player)) calibrateList.add(5)
            if (!hasAnyShield(player)) calibrateList.add(6)

            if (!hasAnyAxe(player)) calibrateList.add(7)
            if (!hasAnyPickaxe(player)) calibrateList.add(8)


            if (probabilityTrue(70.0) && calibrateList.isNotEmpty()) {
                val calibrate = calibrateList.random()
                rn = when (calibrate) {
                    0 -> 1
                    1 -> 3
                    2 -> 5
                    3 -> 7
                    4 -> 9
                    5 -> 11
                    6 -> 13
                    7 -> 15
                    8 -> 17
                    else -> rn
                }
            }

            //보정

            if (random.nextInt(1, 5) == 1) {
                when (rn) {
                    in 0..0 -> addDroppedItemSlot(droppedItem, createNamedItem(Material.GOLDEN_CARROT, 1, "§6Golden Carrot", listOf("§7작아서 휴대하기 편합니다!", " ", "§7우클릭하면 즉시 hp를 회복합니다.")))
                    in 1..2 -> addDroppedItemSlot(droppedItem, ItemStack(Material.DIAMOND_SWORD))
                    in 3..4 -> addDroppedItemSlot(droppedItem, ItemStack(Material.DIAMOND_HELMET))
                    in 5..6 -> addDroppedItemSlot(droppedItem, ItemStack(Material.DIAMOND_CHESTPLATE))
                    in 7..8 -> addDroppedItemSlot(droppedItem, ItemStack(Material.DIAMOND_LEGGINGS))
                    in 9..10 -> addDroppedItemSlot(droppedItem, ItemStack(Material.DIAMOND_BOOTS))
                    in 11..12 -> addDroppedItemSlot(droppedItem, enchantItem(ItemStack(Material.BOW), Enchantment.ARROW_DAMAGE, 1))
                    in 13..14 -> addDroppedItemSlot(droppedItem, addItemData(enchantItem(ItemStack(Material.SHIELD), Enchantment.DURABILITY, 3), null, "§b§lEnchanted Shield", listOf("§7왼손에 들고 있을 시, 받는 근접 대미지를 20% 감소 시킵니다.")))
                    in 15..16 -> addDroppedItemSlot(droppedItem, enchantItem(ItemStack(Material.IRON_AXE), Enchantment.DIG_SPEED, 3))
                    in 17..18 -> addDroppedItemSlot(droppedItem, enchantItem(ItemStack(Material.IRON_PICKAXE), Enchantment.DIG_SPEED, 3))
                }
            } else {
                when (rn) {
                    in 0..0 -> addDroppedItemSlot(droppedItem, ItemStack(Material.GOLDEN_APPLE))
                    in 1..2 -> addDroppedItemSlot(droppedItem, ItemStack(Material.IRON_SWORD))
                    in 3..4 -> addDroppedItemSlot(droppedItem, ItemStack(Material.IRON_HELMET))
                    in 5..6 -> addDroppedItemSlot(droppedItem, ItemStack(Material.IRON_CHESTPLATE))
                    in 7..8 -> addDroppedItemSlot(droppedItem, ItemStack(Material.IRON_LEGGINGS))
                    in 9..10 -> addDroppedItemSlot(droppedItem, ItemStack(Material.IRON_BOOTS))
                    in 11..12 -> addDroppedItemSlot(droppedItem, ItemStack(Material.BOW))
                    in 13..14 -> addDroppedItemSlot(droppedItem, addItemData(ItemStack(Material.SHIELD), null, "§eShield", listOf("§7왼손에 들고 있을 시, 받는 근접 대미지를 10% 감소 시킵니다.")))
                    in 15..16 -> addDroppedItemSlot(droppedItem, enchantItem(ItemStack(Material.STONE_AXE), Enchantment.DIG_SPEED, 3))
                    in 17..18 -> addDroppedItemSlot(droppedItem, enchantItem(ItemStack(Material.STONE_PICKAXE), Enchantment.DIG_SPEED, 3))
                }
            }
            when (rn) {
                in 1..2 -> worldData.playerItemList[player.uniqueId]!!.add("sword")
                in 3..4 -> worldData.playerItemList[player.uniqueId]!!.add("helmet")
                in 5..6 -> worldData.playerItemList[player.uniqueId]!!.add("chestplate")
                in 7..8 -> worldData.playerItemList[player.uniqueId]!!.add("leggings")
                in 9..10 -> worldData.playerItemList[player.uniqueId]!!.add("boots")
                in 11..12 -> worldData.playerItemList[player.uniqueId]!!.add("bow")
                in 13..14 -> worldData.playerItemList[player.uniqueId]!!.add("shield")
                in 15..16 -> worldData.playerItemList[player.uniqueId]!!.add("axe")
                in 17..18 -> worldData.playerItemList[player.uniqueId]!!.add("pickaxe")
            }

            for (i in 0 until random.nextInt(0, 5)) {
                addDroppedItemSlot(droppedItem, ItemStack(Material.ARROW, random.nextInt(1, 3)))
            }
            for (i in 0 until random.nextInt(1, 5)) {
                addDroppedItemSlot(droppedItem, ItemStack(Material.COOKED_BEEF, random.nextInt(1, 2)))
            }

        }

        val inv = Bukkit.createInventory(null, 9*droppedItem.size, Component.text("§e⚠ §8Ground"))

        currentInv[player.uniqueId] = uuid

        droppedItem.items.forEach { (k, v) -> inv.setItem(k, v) }

        player.openInventory(inv)
    }
    fun getDroppedItem(uuid: UUID): DroppedItem? {
        val itemClass = droppedItems.filter { it.uuid == uuid }
        if (itemClass.isEmpty()) return null
        return itemClass[0]
    }

    fun autoDelete(item: DroppedItem) {
        if (!item.itemGenerated) return
        if (item.items.values.isEmpty()) {
            droppedItems.remove(item)
        }
    }
    fun enchantItem(item: ItemStack, enchantment: Enchantment, lv: Int): ItemStack {
        item.addEnchantment(enchantment, lv)
        return item
    }
    fun createEnchantedBook(enchantment: Enchantment, level: Int): ItemStack {
        val enchantedBook = ItemStack(Material.ENCHANTED_BOOK)
        val meta = enchantedBook.itemMeta as EnchantmentStorageMeta

        meta.addStoredEnchant(enchantment, level, true)
        enchantedBook.itemMeta = meta


        return enchantedBook
    }
    fun updateInventorySch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.forEach { player ->
                val title = ChatColor.stripColor(player.openInventory.title)
                if (title != null) {
                    if (title.contains("⚠")) {
                        if (!inventoryData[player.uniqueId].contentEquals((player.openInventory.topInventory.contents))) {
                            inventoryData[player.uniqueId] = player.openInventory.topInventory.contents as Array<ItemStack?>

                            updateInventory(player.openInventory)

                        }
                    }
                }
            }
        }, 0, 2)
    }

    fun createDisplay(item: DroppedItem) {
        val loc = item.loc
        val uuid = item.uuid


        loc.getNearbyEntities(3.0, 3.0, 3.0).forEach { entity ->
            if (entity.scoreboardTags.contains("$uuid")) {
                entity.remove()
            }
        }





        val random = Random(uuid.mostSignificantBits)

        val m = 1.0

        if (item.itemGenerated) {
            val types = mutableSetOf<Material>()
            item.items.forEach { (k, v) ->
                types.add(v.type)
            }
            types.forEach { k ->
                val rf = (random.nextFloat() * 360.0 - 180.0).toFloat()
                val itemDisplay = loc.world.spawnEntity(loc.clone().add((random.nextDouble() -0.5)*m, random.nextDouble() * 0.01, (random.nextDouble() -0.5)*m), EntityType.ITEM_DISPLAY) as ItemDisplay
                itemDisplay.itemStack = ItemStack(k)
                itemDisplay.setRotation(rf, 90.0F)

                itemDisplay.addScoreboardTag("$uuid")
                itemDisplay.addScoreboardTag("tmp-display")
                val display = itemDisplay.transformation
                display.scale.set(0.5, 0.5, 0.5)
                itemDisplay.transformation = display
            }
        } else {
            for (i in 0..random.nextInt(2, 5)) {
                val rf = (random.nextFloat() * 360.0 - 180.0).toFloat()
                val itemDisplay = loc.world.spawnEntity(loc.clone().add((random.nextDouble() -0.5)*m, random.nextDouble() * 0.01, (random.nextDouble() -0.5)*m), EntityType.ITEM_DISPLAY) as ItemDisplay

                itemDisplay.itemStack = ItemStack(Material.STICK)
                itemDisplay.setRotation(rf, 90.0F)

                itemDisplay.addScoreboardTag("$uuid")
                itemDisplay.addScoreboardTag("tmp-display")
                val display = itemDisplay.transformation
                display.scale.set(0.5, 0.5, 0.5)
                itemDisplay.transformation = display

            }
        }
    }
     fun updateInventory(view: InventoryView) {
        if (!(ChatColor.stripColor(view.title)?: return).contains("⚠")) return
        val uuid = currentInv[view.player.uniqueId] ?: return
        val droppedItem = ItemManager.getDroppedItem(uuid) ?: return
        droppedItem.items.clear()
        for (i in 0..26) {
            val item = view.topInventory.getItem(i)
            if (item != null) {
                droppedItem.items[i] = item
            }
        }
        createDisplay(droppedItem)
    }
}