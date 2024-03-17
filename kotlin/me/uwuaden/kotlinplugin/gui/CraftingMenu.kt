package me.uwuaden.kotlinplugin.gui

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setName
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CraftingMenu {
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.filter { it.openInventory.title == "§eEssence Extract Menu" } .forEach { player ->
                val inv = player.openInventory

                val item = inv.getItem(11)
                if (item != null) {
                    var isReady = false
                    if (CraftingMenu.isIronType(item)) {
                        inv.setItem(
                            15,
                            ItemManager.createNamedItem(
                                Material.IRON_INGOT,
                                CraftingMenu.itemToEssence(item),
                                "§fIron Essence",
                                null
                            )
                        )
                        isReady = true
                    } else if (CraftingMenu.isDiamondType(item)) {
                        inv.setItem(
                            15,
                            ItemManager.createNamedItem(
                                Material.DIAMOND,
                                CraftingMenu.itemToEssence(item),
                                "§bDiamond Essence",
                                null
                            )
                        )
                        isReady = true
                    } else {
                        inv.setItem(15, ItemManager.createNamedItem(Material.BARRIER, 1, "§c결과물 없음!", listOf("§7왼쪽에 재료를 넣어주세요!", "§7철 장비를 Iron Essence로, 다이아몬드 장비는 Diamond Essence로", "§7변환시킬 수 있습니다!")))
                    }

                    if (isReady) {
                        for (i in 12..14) inv.setItem(i, ItemManager.createNamedItem(Material.LIME_STAINED_GLASS_PANE, 1, " ", null))
                    }

                }
            }
        }, 0, 1)
    }

    fun getEssenceList(p: Player): List<String> {
        val list = mutableListOf<String>()
        val data = WorldManager.initData(p.world)
        val uuid = p.uniqueId
        list.add(" §8- §fIron: ${data.playerEssence[Pair(uuid, "IRON")] ?: 0}")
        list.add(" §8- §bDiamond: ${data.playerEssence[Pair(uuid, "DIAMOND")] ?: 0}")
        list.add(" §8- §5Amethyst: ${data.playerEssence[Pair(uuid, "AMETHYST")] ?: 0}")
        return list
    }

    fun isIronType(item: ItemStack): Boolean {
        return listOf(Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.IRON_SWORD).contains(item.type)
    }
    fun isDiamondType(item: ItemStack): Boolean {
        return listOf(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.DIAMOND_SWORD).contains(item.type)
    }

    fun itemToEssence(item: ItemStack): Int {
        val hash = hashMapOf<Material, Int>()
        hash[Material.IRON_HELMET] = 1
        hash[Material.IRON_CHESTPLATE] = 2
        hash[Material.IRON_LEGGINGS] = 2
        hash[Material.IRON_BOOTS] = 1
        hash[Material.IRON_SWORD] = 1

        hash[Material.DIAMOND_HELMET] = 1
        hash[Material.DIAMOND_CHESTPLATE] = 2
        hash[Material.DIAMOND_LEGGINGS] = 2
        hash[Material.DIAMOND_BOOTS] = 1
        hash[Material.DIAMOND_SWORD] = 1
        return hash.getOrDefault(item.type, 0)
    }

    fun addEssence(player: Player, string: String, amount: Int) {
        val data = WorldManager.initData(player.world)
        data.playerEssence[Pair(player.uniqueId, string)] = (data.playerEssence[Pair(player.uniqueId, string)] ?: 0) + amount
    }
    fun openMain(p: Player) {
        val world = p.world
        val uuid = p.uniqueId
        if (!world.name.contains("Field-")) return

        val inv = Bukkit.createInventory(null, 27, "§eCrafting Menu")
        for (i in 0 until 27) {
            inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        }
        inv.setItem(10, ItemManager.createNamedItem(Material.CRAFTING_TABLE, 1, "§aEssence Crafting", null))
        inv.setItem(13, ItemManager.createNamedItem(Material.CHEST, 1, "§aEssence", getEssenceList(p)))
        inv.setItem(16, ItemManager.createNamedItem(Material.ANVIL, 1, "§aAnvil", null))
        p.openInventory(inv)
    }
    fun openEssenceCrafting(p: Player) {
        val world = p.world
        val uuid = p.uniqueId
        if (!world.name.contains("Field-")) return

        val data = WorldManager.initData(world)

        val inv = Bukkit.createInventory(null, 27, "§eEssence Crafting Menu")
        for (i in 0 until 27) {
            inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        }
        inv.setItem(12, ItemManager.createNamedItem(Material.CRAFTING_TABLE, 1, "§aCrafting", null))
        inv.setItem(14, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "§aExtract", null))
        inv.setItem(26, ItemManager.createNamedItem(Material.CHEST, 1, "§aEssence", getEssenceList(p)))
        p.openInventory(inv)
    }
    fun openExtract(p: Player) {
        val world = p.world
        val uuid = p.uniqueId
        if (!world.name.contains("Field-")) return

        val data = WorldManager.initData(world)

        val inv = Bukkit.createInventory(null, 36, "§eEssence Extract Menu")
        for (i in 0 until 36) {
            inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        }
        inv.setItem(11, ItemStack(Material.AIR))
        inv.setItem(12, ItemManager.createNamedItem(Material.RED_STAINED_GLASS_PANE, 1, " ", null))
        inv.setItem(13, ItemManager.createNamedItem(Material.RED_STAINED_GLASS_PANE, 1, " ", null))
        inv.setItem(14, ItemManager.createNamedItem(Material.RED_STAINED_GLASS_PANE, 1, " ", null))
        inv.setItem(15, ItemManager.createNamedItem(Material.BARRIER, 1, "§c결과물 없음!", listOf("§7왼쪽에 재료를 넣어주세요!", "§7철 장비를 Iron Essence로, 다이아몬드 장비는 Diamond Essence로", "§7변환시킬 수 있습니다!")))
        inv.setItem(35, ItemManager.createNamedItem(Material.CHEST, 1, "§aEssence", getEssenceList(p)))
        p.openInventory(inv)
    }
    fun openCraftingMenu(p: Player, page: Int = 1, search: String = "", showCraftable: Boolean = false, sortByChar: Boolean = true) {
        //sort = char, craftable
        //TODO
        val world = p.world
        val uuid = p.uniqueId
        if (!world.name.contains("Field-")) return

        val data = WorldManager.initData(world)

        val inv = Bukkit.createInventory(null, 54, "§eEssence Recipes")
        for (i in 0 until 9) inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        for (i in 45 until 54) inv.setItem(i, ItemManager.createNamedItem(Material.WHITE_STAINED_GLASS_PANE, 1, " ", null))
        if (sortByChar) {
            inv.setItem(50, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTY3ZDgxM2FlN2ZmZTViZTk1MWE0ZjQxZjJhYTYxOWE1ZTM4OTRlODVlYTVkNDk4NmY4NDk0OWM2M2Q3NjcyZSJ9fX0=")
                .setName("§aSorted by Alphabet"))
        } else {
            inv.setItem(50, EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGMzNjA0NTIwOGY5YjVkZGNmOGM0NDMzZTQyNGIxY2ExN2I5NGY2Yjk2MjAyZmIxZTUyNzBlZThkNTM4ODFiMSJ9fX0=")
                .setName("§aSorted by Craftable"))
        }
        var searchStr = "§aClick to Search"
//        if (search != "") {
//            searchStr = "§aSearch: $search"
//        }
//        inv.setItem(45, ItemManager.createNamedItem(Material.OAK_SIGN, 1, searchStr, null))
        if (showCraftable) {
            inv.setItem(51, ItemManager.createNamedItem(Material.CRAFTING_TABLE, 1, "§aShowing Craftable", null))
        } else {
            inv.setItem(51, ItemManager.createNamedItem(Material.RED_STAINED_GLASS, 1, "§aShowing All", null))
        }

        inv.setItem(53, ItemManager.createNamedItem(Material.CHEST, 1, "§aEssence", getEssenceList(p)))

        val hash = data.getPlayerTypedRecipes(uuid)
        val list = hash.keys.toMutableList()
        for (i in 0 until 36) {
            inv.setItem(i+9, list.getOrNull(i)?.item ?: ItemStack(Material.AIR))
        }



        p.openInventory(inv)
    }
}