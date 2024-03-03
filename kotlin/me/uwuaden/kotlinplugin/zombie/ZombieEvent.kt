package me.uwuaden.kotlinplugin.zombie

import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.MathCal
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

private fun getBlocksInCircle(center: Location, radius: Int): List<Block> {
    val blocksInCircle = mutableListOf<Block>()

    val centerX = center.blockX
    val centerY = center.blockY
    val centerZ = center.blockZ

    for (x in centerX - radius..centerX + radius) {
        for (z in centerZ - radius..centerZ + radius) {
            val distanceSquared = (centerX - x) * (centerX - x) + (centerZ - z) * (centerZ - z)
            if (distanceSquared <= radius * radius) {
                val y = centerY // 원 형태이므로 y 좌표는 고정값으로 사용
                blocksInCircle.add(Location(center.world, x.toDouble(), y.toDouble(), z.toDouble()).block)
            }
        }
    }

    return blocksInCircle
}
private fun dropMessage(loc: Location, message: String) {
    EffectManager.playSurroundSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f)
    val textDisplay = loc.world.spawnEntity(loc, EntityType.TEXT_DISPLAY) as TextDisplay
    textDisplay.text(Component.text(message))
    textDisplay.isSeeThrough = true
    textDisplay.billboard = Display.Billboard.CENTER
    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        textDisplay.remove()
    }, 20*5)
}
class ZombieEvent: Listener {
    @EventHandler
    fun onZombieAttack(e: EntityDamageByEntityEvent) {
        val attacker = e.damager
        val name = attacker.customName
        if (attacker.scoreboardTags.contains("Spawned-Zombie")) {
            e.damage = when (name) {
                "§8Heavy Zombie" -> 9.0
                "§2Boss Zombie 1" -> 12.0
                "§0Shadow" -> 6.0
                "§c§lLava" -> 10.0
                "§0Void" -> 12.0
                "§2Boss Zombie 2" -> 18.0
                "§cExplosion" -> 10.0
                "§bElectro Zombie" -> 14.0
                "§5Summoner" -> 15.0
                "§fSkeleton" -> 10.0
                else -> 4.5
            }
        }
        if (e.entity.scoreboardTags.contains("Spawned-Zombie")) {
            if (e.entity.name == "§2Boss Zombie 2") {
                if (attacker is Arrow) {
                    e.isCancelled = true
                }
            }
        }
    }
    @EventHandler
    fun onZombieReceiveDamage(e: EntityDamageEvent) {
        val entity = e.entity
        val name = entity.name
        if (name == "§8Heavy Zombie") {
            EffectManager.playSurroundSound(entity.location, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.0f)
        } else if (name == "§c§lLava" && !(entity as LivingEntity).hasAI()) {
            EffectManager.playSurroundSound(entity.location, Sound.BLOCK_BASALT_BREAK, 1.0F, 1.0f)
        } else if (name == "§eFlash") {
            EffectManager.playSurroundSound(entity.location, Sound.ENTITY_BLAZE_HURT, 0.5f, 2.0f)
        }
    }

    @EventHandler
    fun onZombieDeath(e: EntityDeathEvent) {
        if (e.entity.scoreboardTags.contains("Spawned-Zombie")) {
            val loc = e.entity.location
            if (MathCal.probabilityTrue(3.0)) {
                dropMessage(loc, "§f§lITEM DROP!")
                loc.world.dropItem(loc, ItemStack(Material.ARROW, 32))
            }
            if (MathCal.probabilityTrue(0.5)) {
                dropMessage(loc, "§c§lITEM DROP!")
                loc.world.dropItem(loc, CustomItemData.getFlareGun())
            }
            if (MathCal.probabilityTrue(2.0)) {
                dropMessage(loc, "§b§lITEM DROP!")
                val a = Random.nextInt(0, 5)
                when (a) {
                    0 -> loc.world.dropItem(loc, ItemStack(Material.DIAMOND_HELMET))
                    1 -> loc.world.dropItem(loc, ItemStack(Material.DIAMOND_CHESTPLATE))
                    2 -> loc.world.dropItem(loc, ItemStack(Material.DIAMOND_LEGGINGS))
                    3 -> loc.world.dropItem(loc, ItemStack(Material.DIAMOND_BOOTS))
                    4 -> loc.world.dropItem(loc, ItemStack(Material.DIAMOND_SWORD))
                }
            }
            if (MathCal.probabilityTrue(0.5)) {
                dropMessage(loc, "§7§lITEM DROP!")
                loc.world.dropItem(loc, ItemStack(Material.ANVIL))
            }
            if (MathCal.probabilityTrue(2.0)) {
                dropMessage(loc, "§d§lITEM DROP!")
                val a = Random.nextInt(0, 5)
                when (a) {
                    0 -> loc.world.dropItem(loc, ItemManager.createEnchantedBook(Enchantment.PROTECTION_ENVIRONMENTAL, 2))
                    1 -> loc.world.dropItem(loc, ItemManager.createEnchantedBook(Enchantment.DAMAGE_ALL, 2))
                    2 -> loc.world.dropItem(loc, ItemManager.createEnchantedBook(Enchantment.ARROW_DAMAGE, 2))
                    3 -> loc.world.dropItem(loc, ItemManager.createEnchantedBook(Enchantment.SWEEPING_EDGE, 2))
                    4 -> loc.world.dropItem(loc, ItemManager.createEnchantedBook(Enchantment.DURABILITY, 2))
                }
            }
            if (MathCal.probabilityTrue(5.0)) {
                dropMessage(loc, "§7§lITEM DROP!")
                loc.world.dropItem(loc, ItemStack(Material.COOKED_BEEF, 16))
            }
            if (MathCal.probabilityTrue(2.0)) {
                dropMessage(loc, "§6§lITEM DROP!")
                loc.world.dropItem(loc, ItemStack(Material.GOLDEN_APPLE, 5))
            }
            if (MathCal.probabilityTrue(2.0)) {
                val players = e.entity.world.players.filter { it.gameMode == GameMode.SPECTATOR }
                if (players.isNotEmpty()) {
                    val player = players.random()
                    e.entity.world.sendMessage(Component.text("§a${player.name}님이 부활했습니다!"))
                    player.teleport(e.entity.world.players.filter { it.gameMode == GameMode.SURVIVAL }.random())
                    player.gameMode = GameMode.SURVIVAL
                    player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*20, 4, false, false))
                }
            }


            if (e.entity.customName == "§cExplosion") {
                EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F)
                loc.world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 0.0, 0.0, 0.0, 0.0)
                loc.getNearbyLivingEntities(3.0).filterIsInstance<Player>().forEach {
                    it.damage(4.0)
                }
                val blocks = mutableSetOf<Block>()

                val r = 3
                loc.y += 1.0
                while (loc.y < 320) {

                    getBlocksInCircle(loc, r).forEach {
                        blocks.add(it)
                    }
                    loc.y += 1.0
                }
                blocks.filter { it.type != Material.AIR }

                blocks.forEach {
                    EffectManager.breakBlock(it.location)
                }
            }
        }
    }
}