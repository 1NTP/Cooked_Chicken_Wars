package me.uwuaden.kotlinplugin.gameSystem

import me.uwuaden.kotlinplugin.Main.Companion.groundY
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator.getName
import me.uwuaden.kotlinplugin.gui.CraftingMenu
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.inventory.ItemStack

class GameEvent: Listener {
    companion object {
        val maxEnchantLevels = HashMap<Enchantment, Int>()
    }
    @EventHandler
    fun onEnable(e: PluginEnableEvent) {
        if (e.plugin == plugin) {
            maxEnchantLevels.clear()
            maxEnchantLevels[Enchantment.QUICK_CHARGE] = 2
            maxEnchantLevels[Enchantment.ARROW_DAMAGE] = 3
        }
    }
    @EventHandler
    fun onBreak(e: BlockBreakEvent) {
        val player = e.player
        e.isDropItems = false
        e.block.drops.clear()
        if (listOf(Material.IRON_ORE, Material.DIAMOND_ORE, Material.AMETHYST_BLOCK).contains(e.block.type)) {
            val data = WorldManager.initData(e.block.world)
            val resource = data.droppedResource.filter { it.type == e.block.type }.filter { e.block.location.distance(it.loc) <= 1.0 }.sortedBy { e.block.location.distance(it.loc) }.firstOrNull()

            if (resource != null) {
                resource.health -= 1
                if (resource.health <= 0) {
                    e.isCancelled = true
                    val r = 5.0
                    e.block.location.getNearbyEntities(r, r, r).filterIsInstance<BlockDisplay>().filter { it.scoreboardTags.contains("RESOURCE:${e.block.type.name}") }.forEach {
                        it.location.world.spawnParticle(Particle.SMOKE_NORMAL, it.location, 5, 0.1, 0.1, 0.1, 0.0)
                        it.remove()
                    }
                    var str = ""
                    when (e.block.type) {
                        Material.IRON_ORE -> {
                            str = "§fIron Essence +5"
                            CraftingMenu.addEssence(player, "IRON", 5)
                        }
                        Material.DIAMOND_ORE -> {
                            str = "§bDiamond Essence +5"
                            CraftingMenu.addEssence(player, "DIAMOND", 5)
                        }
                        Material.AMETHYST_BLOCK -> {
                            str = "§5Amethyst Essence +5"
                            CraftingMenu.addEssence(player, "AMETHYST", 5)
                        }
                        else -> {}
                    }

                    e.player.sendActionBar(Component.text(str))
                    e.player.playSound(e.player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)

                    e.block.location.world.spawnParticle(Particle.SMOKE_NORMAL, e.block.location.toCenterLocation(), 20, 0.5, 0.5, 0.5, 0.0)
                    e.block.type = Material.AIR
                } else {
                    e.isCancelled = true
                    e.block.location.world.spawnParticle(Particle.LAVA, e.block.location.toCenterLocation(), 20, 0.5, 0.5, 0.5)
                    EffectManager.playSurroundSound(e.block.location, Sound.BLOCK_LANTERN_BREAK, 1.0f, 1.0f)
                    e.player.sendActionBar(Component.text("§aResource HP: ${resource.health}/${resource.maxHealth}"))
                }
                return
            }
        }
        if (e.block.type.toString().lowercase().contains("anvil")) {
            e.block.world.dropItem(e.block.location, ItemStack(e.block.type))
            e.block.drops.clear()
            return
        }
        if (e.block.type == Material.CHEST) {
            val chest = e.block.state as Chest
            if (chest.customName == "§eSupplies") {
                chest.blockInventory.forEach {
                    if (it != null) {
                        chest.location.world.dropItem(chest.location, it)
                    }
                }
                e.block.drops.clear()
                return
            }
        }
        if (e.block.y <= groundY.toInt()) {
            e.isCancelled = true
            e.block.drops.clear()
            e.player.sendMessage("§c바닥의 블럭을 부술 수 없습니다.")
            e.block.world.spawnParticle(Particle.SMOKE_NORMAL, e.block.location.clone().add(0.5, 0.5, 0.5), 10, 0.0, 0.0, 0.0, 0.0)
        }
    }


    @EventHandler
    fun onFallDamage(e: EntityDamageEvent) {
        if (e.entity.world.name.contains("Queue-") && e.cause == EntityDamageEvent.DamageCause.FALL) {
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onAttackDamage(e: EntityDamageEvent) {
        if (e.entity.world.name.contains("Queue-") && e.cause != EntityDamageEvent.DamageCause.VOID && e.entity.location.y >= 80.0) {
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onQueueDeath(e: PlayerDeathEvent) {
        if (e.player.world.name.contains("Queue-")) {
            e.isCancelled = true
            e.player.health = 20.0
            e.player.teleport(Location(e.player.world, 14.5, 106.5, -40.5))
        }
    }
    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        val player = e.player
        if ((player.world.name.contains("Queue-") || player.world.name == "world") && (player.gameMode != GameMode.CREATIVE || player.gameMode != GameMode.SPECTATOR)) {
            if (player.isOp && player.gameMode == GameMode.CREATIVE) return
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onBreakLobby(e: BlockBreakEvent) {
        val player = e.player
        if ((player.world.name.contains("Queue-") || player.world.name == "world") && (player.gameMode != GameMode.CREATIVE || player.gameMode != GameMode.SPECTATOR)) {
            if (player.isOp && player.gameMode == GameMode.CREATIVE) return
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onPlaceLobby(e: BlockBreakEvent) {
        val player = e.player
        if ((player.world.name.contains("Queue-") || player.world.name == "world") && (player.gameMode != GameMode.CREATIVE || player.gameMode != GameMode.SPECTATOR)) {
            if (player.isOp && player.gameMode == GameMode.CREATIVE) return
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onLobbyDrop(e: PlayerDropItemEvent) {
        val player = e.player
        if ((player.world.name.contains("Queue-") || player.world.name == "world") && (player.gameMode != GameMode.CREATIVE || player.gameMode != GameMode.SPECTATOR)) {
            if (player.isOp && player.gameMode == GameMode.CREATIVE) return
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onLobbySwap(e: PlayerSwapHandItemsEvent) {
        val player = e.player
        if ((player.world.name.contains("Queue-") || player.world.name == "world") && (player.gameMode != GameMode.CREATIVE || player.gameMode != GameMode.SPECTATOR)) {
            if (player.isOp && player.gameMode == GameMode.CREATIVE) return
            e.isCancelled = true
        }
    }
    @EventHandler
    fun onInvClick(e: InventoryClickEvent) {
        val player = e.view.player
        if ((player.world.name.contains("Queue-") || player.world.name == "world") && (player.gameMode != GameMode.CREATIVE || player.gameMode != GameMode.SPECTATOR)) {
            if (player.isOp && player.gameMode == GameMode.CREATIVE) return
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onAnvilClick(e: PrepareAnvilEvent) {
        e.inventory.repairCost = 0
    }

    @EventHandler
    fun onEnchant(e: InventoryClickEvent) {
        if (e.inventory.type == InventoryType.ANVIL) {
            val worldData = WorldManager.initData(e.view.player.world)
            if (worldData.worldMode == "SoloSurvival") return
            if (e.inventory.getItem(1)?.getName() == CustomItemData.getBookOfMastery().getName()) return
            val player = e.view.player as Player
            if (e.slot == 2) {
                val itemLast = e.inventory.getItem(2) ?: return
                if (player.getCooldown(Material.DAMAGED_ANVIL) <= 0) {
                    maxEnchantLevels.forEach { (enchant, level) ->
                        if (itemLast.getEnchantmentLevel(enchant) > level) {
                            e.isCancelled = true
                            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f)
                            player.sendMessage("§c해당 아이템이 인챈트 레벨 제한을 넘었습니다!")
                            player.sendMessage("§c무시하고 인챈트 할려면 다시 클릭해주세요.")
                            player.setCooldown(Material.DAMAGED_ANVIL, 20*5)
                            return
                        }
                    }
                } else {
                    maxEnchantLevels.forEach { (enchant, level) ->
                        if (itemLast.getEnchantmentLevel(enchant) > level) {
                            itemLast.removeEnchantment(enchant)
                            itemLast.addEnchantment(enchant, level)
                        }
                    }
                    player.setCooldown(Material.DAMAGED_ANVIL, 0)
                    player.sendMessage("§a제한을 무시했습니다!")
                }
            }
        }
    }
}