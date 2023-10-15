package me.uwuaden.kotlinplugin.itemManager.customItem

import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.Main.Companion.lastDamager
import me.uwuaden.kotlinplugin.Main.Companion.lastWeapon
import me.uwuaden.kotlinplugin.Main.Companion.lockedPlayer
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setCount
import me.uwuaden.kotlinplugin.gameSystem.LastWeaponData
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemEvent.Companion.smokeRadius
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.Validate
import org.bukkit.*
import org.bukkit.Particle.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.*
import kotlin.random.Random


private fun isHittable(player: Player, target: LivingEntity): Boolean {
    return !TeamManager.isSameTeam(player.world, player, target) && !(target is Player && target.gameMode == GameMode.SPECTATOR)
}

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
private fun fireworkWithColor(loc: Location, color: Color) {
    val f = loc.world.spawnEntity(loc, EntityType.FIREWORK) as Firework
    val meta = f.fireworkMeta
    val effectBuilder = FireworkEffect.builder()
        .withColor(color)
        .withFade(Color.WHITE)
        .with(FireworkEffect.Type.BALL_LARGE)
        .trail(false)
        .flicker(true)
    meta.addEffect(effectBuilder.build())
    meta.power = 0
    f.fireworkMeta = meta
    f.scoreboardTags.add("display_firework")
    f.detonate()
}
private fun countItemsWithName(player: Player, itemName: String): Int {
    var itemCount = 0

    player.inventory.contents?.forEach { item ->
        if (item != null && item.type != Material.AIR && item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
            val displayName = item.itemMeta!!.displayName
            if (displayName == itemName) {
                itemCount += item.amount
            }
        }
    }

    return itemCount
}
private fun liberationSkillTP(player: Player) {
    if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.DARK_PURPLE}${ChatColor.BOLD}Liberation") {
        if (player.location.getNearbyPlayers(30.0).filter { isHittable(player, it) }.size < 3) return //쓸거
        if (player.health != player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value) return

        if (player.getCooldown(Material.INFESTED_CRACKED_STONE_BRICKS) > 0) return
        player.setCooldown(Material.INFESTED_CRACKED_STONE_BRICKS, 2 * 20)

        val playerLoc = player.location.clone()
        for (i in 0..101) {
            val playerTargetLoc = playerLoc.clone().add(playerLoc.clone().direction.multiply((i.toDouble()/10.0))).add(0.0, 0.5, 0.0)
            val block1 = playerTargetLoc.block
            val block2 = playerTargetLoc.clone().add(0.0, 1.0, 0.0).block
            if (!(block1.type == Material.AIR && block2.type == Material.AIR) || i == 101) {
                val finalTpLoc  = playerLoc.clone().add(playerLoc.clone().direction.multiply(((i-1).toDouble()/10.0))).add(0.0, 0.5, 0.0)
                player.world.spawnParticle(PORTAL, player.location, 30, 0.1, 1.0, 0.1, 0.0)
                val finalBlock = finalTpLoc.block
                val tpLoc = finalBlock.location.add(0.5, 0.5, 0.5)
                tpLoc.yaw = playerLoc.yaw
                tpLoc.pitch = playerLoc.pitch
                player.teleport(tpLoc)
                EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 2.0F)
                EffectManager.playSurroundSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 2.0F)
                playerLoc.world.spawnParticle(PORTAL, player.location, 30, 0.1, 1.0, 0.1, 0.0)
                return
            }
        }
        player.setCooldown(Material.INFESTED_CRACKED_STONE_BRICKS, 0)
    }
}
private fun liberationSkill(player: Player) {
    if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.DARK_PURPLE}${ChatColor.BOLD}Liberation") {
        if (player.location.getNearbyPlayers(30.0).filter { isHittable(player, it) }.size < 3) return //쓸거

        if (player.getCooldown(Material.INFESTED_STONE) > 0) return
        player.setCooldown(Material.INFESTED_STONE, 2*20)
        var dmg = 2.0
        if (player.health == player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value) dmg*=2

        //case 1: 오른쪽 위, 왼쪽 아래, case2: 왼쪽 위, 오른쪽 아래

        val playerLoc = player.location
        val playerTargetLoc = playerLoc.clone().add(playerLoc.clone().direction.multiply(5.0))
        val random = Random
        val loc1 = playerTargetLoc.clone().add(random.nextInt(-3, 4).toDouble(), random.nextInt(0, 4).toDouble(), random.nextInt(-3, 4).toDouble())
        val loc2 = playerTargetLoc.clone().add(random.nextInt(-3, 4).toDouble(), random.nextInt(0, 2).toDouble(), random.nextInt(-3, 4).toDouble())

        scheduler.scheduleSyncDelayedTask(plugin, {
            EffectManager.playSurroundSound(playerTargetLoc, Sound.ENTITY_ALLAY_DEATH, 1.0f, 2.0f)
            EffectManager.playSurroundSound(playerTargetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2.0f)
            EffectManager.playSurroundSound(playerTargetLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, 0.5f, 2.0f)
            playerTargetLoc.getNearbyLivingEntities(5.0).filter { isHittable(player, it) }.filterNot { it == player }.forEach {
                it.damage(dmg)
            }
            scheduler.runTaskAsynchronously(plugin, Runnable {
                for (i in 0 until 30) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        CustomItemManager.drawLine(loc1, loc2, 0.2, 140, 0, 153)
                    }, 0)
                    Thread.sleep(100)
                }
            })
        }, 20)
    }
}
private fun probabilityTrue(n: Double): Boolean {
    require(n in 0.0..100.0) { "확률은 0에서 100 사이의 값이어야 합니다." }

    val randomValue = Random.nextDouble(0.0, 100.0)
    return randomValue < n
}

private fun drawParticleCircle(center: Location, radius: Double, Color: Color) {
    val world = center.world
    val centerX = center.x
    val centerY = center.y
    val centerZ = center.z

    // 원의 점 개수를 설정 (점 개수가 늘어날수록 원이 부드러워집니다)
    val points = 50

    for (i in 0 until points) {
        val angle = 2.0 * Math.PI * i / points
        val x = centerX + radius * cos(angle)
        val z = centerZ + radius * sin(angle)
        val particleLocation = Location(world, x, centerY, z)
        world.spawnParticle(Particle.REDSTONE, particleLocation, 3, 0.0, 0.0, 0.0, 0.0, DustOptions(Color, 1.0f))
    }
}
private fun drawLine( /* Would be your orange wool */
                      point1: Location,  /* Your white wool */
                      point2: Location,  /*Space between each particle*/
                      space: Double,
                      r: Int,
                      g: Int,
                      b: Int,
                      p: Player
) {
    val world = point1.world

    /*Throw an error if the points are in different worlds*/Validate.isTrue(
        point2.world.equals(world),
        "Lines cannot be in different worlds!"
    )

    /*Distance between the two particles*/
    val distance = point1.distance(point2)

    /* The points as vectors */
    val p1 = point1.toVector()
    val p2 = point2.toVector()

    /* Subtract gives you a vector between the points, we multiply by the space*/
    val vector = p2.clone().subtract(p1).normalize().multiply(space)

    /*The distance covered*/
    var covered = 0.0

    /* We run this code while we haven't covered the distance, we increase the point by the space every time*/while (covered < distance) {

        /*Spawn the particle at the point*/p.spawnParticle(Particle.REDSTONE, p1.x, p1.y, p1.z, 1, DustOptions(Color.fromRGB(r, g, b), 1.0F))

        /* We add the space covered */covered += space
        p1.add(vector)
    }
}
private fun earthGrenade(loc: Location, p: Player) {
    val originLoc = loc.clone()
    val particleLoc = loc.clone().add(0.0, 1.0, 0.0)
    val blocks = mutableSetOf<Block>()

    val r = 8
    loc.y += 1.0
    while (loc.y < 320) {

        getBlocksInCircle(loc, r).forEach {
            blocks.add(it)
        }
        loc.y += 1.0
    }
    blocks.filter { it.type != Material.AIR }

    scheduler.scheduleSyncDelayedTask(plugin, {
        blocks.forEach {
            if (probabilityTrue(30.0)) {
                loc.world.spawnParticle(Particle.BLOCK_CRACK, it.location, 5, 0.5, 0.5, 0.5, 0.0, it.blockData)
                it.type = Material.AIR
            }
        }

        originLoc.getNearbyLivingEntities(r.toDouble()).forEach { e->
            e.damage(2.0)
            if (e is Player) {
                lastDamager[e] = p
                lastWeapon[e] = LastWeaponData(ItemManager.createNamedItem(Material.STICK, 1, "영역 수류탄", null), System.currentTimeMillis()+1000*10)
            }
        }
        drawParticleCircle(particleLoc, 8.0, Color.fromRGB(100, 65, 23))
        EffectManager.playSurroundSound(originLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f)
        scheduler.scheduleSyncDelayedTask(plugin, {
            blocks.forEach {
                if (probabilityTrue(50.0)) {
                    loc.world.spawnParticle(Particle.BLOCK_CRACK, it.location, 5, 0.5, 0.5, 0.5, 0.0, it.blockData)
                    it.type = Material.AIR
                }
            }
            originLoc.getNearbyLivingEntities(r.toDouble()).forEach { e->
                e.damage(2.0)
                if (e is Player) {
                    lastDamager[e] = p
                    lastWeapon[e] = LastWeaponData(ItemManager.createNamedItem(Material.STICK, 1, "영역 수류탄", null), System.currentTimeMillis()+1000*10)
                }
            }
            drawParticleCircle(particleLoc, 8.0, Color.fromRGB(100, 65, 23))
            EffectManager.playSurroundSound(originLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f)
            scheduler.scheduleSyncDelayedTask(plugin, {
                blocks.filter {it.y != originLoc.blockY}.forEach {
                    if (probabilityTrue(100.0)) {
                        loc.world.spawnParticle(Particle.BLOCK_CRACK, it.location, 5, 0.5, 0.5, 0.5, 0.0, it.blockData)
                        it.type = Material.AIR
                    }
                }
                originLoc.getNearbyLivingEntities(r.toDouble()).forEach { e->
                    e.damage(2.0)
                    if (e is Player) {
                        lastDamager[e] = p
                        lastWeapon[e] = LastWeaponData(ItemManager.createNamedItem(Material.STICK, 1, "영역 수류탄", null), System.currentTimeMillis()+1000*10)
                    }
                }
                drawParticleCircle(originLoc, 8.0, Color.fromRGB(100, 65, 23))
                EffectManager.playSurroundSound(particleLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.7f)
            }, 30)
        }, 30)
    }, 30)
}
private fun antiGravityGrenade(loc: Location, p: Player) {
    scheduler.scheduleSyncDelayedTask(plugin, {
        EffectManager.playSurroundSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f)
        drawParticleCircle(loc.clone().add(0.0, 1.0, 0.0), 6.5, Color.AQUA)
        scheduler.scheduleSyncDelayedTask(plugin, {
            EffectManager.playSurroundSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.8f)
            drawParticleCircle(loc.clone().add(0.0, 1.0, 0.0), 7.0, Color.AQUA)
            scheduler.scheduleSyncDelayedTask(plugin, {
                EffectManager.playSurroundSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 2.0f)
                drawParticleCircle(loc.clone().add(0.0, 1.0, 0.0), 7.5, Color.AQUA)
                scheduler.scheduleSyncDelayedTask(plugin, {
                    EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f)
                    drawParticleCircle(loc.clone().add(0.0, 1.0, 0.0), 8.0, Color.AQUA)
                    loc.getNearbyLivingEntities(8.0).forEach {
                        val direction = it.location.toVector().subtract(loc.toVector()).normalize()
                        it.velocity = direction.multiply(2.0)
                        if (it is Player) {
                            lastDamager[it] = p
                            lastWeapon[it] = LastWeaponData(ItemManager.createNamedItem(Material.STICK, 1, "반중력 수류탄", null), System.currentTimeMillis()+1000*10)
                        }
                        it.damage(2.0)
                    }
                }, 5)
            }, 5)
        }, 5)
    }, 5)
}
private fun gravityGrenade(loc: Location, p: Player) {
    scheduler.scheduleSyncDelayedTask(plugin, {
        EffectManager.playSurroundSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f)
        EffectManager.playSurroundSound(loc, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.5f)
        scheduler.runTaskAsynchronously(plugin, Runnable {
            for (i in 0 until 20) {
                scheduler.scheduleSyncDelayedTask(plugin, {
                    if (i%2 == 0) {
                        drawParticleCircle(loc.clone().add(0.0, 1.0, 0.0), 8.0, Color.PURPLE)
                        loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 1.0, 0.0), 50, 0.21, 0.21, 0.21, 0.0, DustOptions(Color.PURPLE, 2.0f))
                        loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 1.0, 0.0), 50, 0.2, 0.2, 0.2, 0.0, DustOptions(Color.BLACK, 2.0f))
                        loc.getNearbyLivingEntities(8.0).forEach {
                            val locClone = loc.clone()
                            locClone.y = it.y
                            val direction = it.location.toVector().subtract(locClone.toVector()).normalize()
                            it.velocity = direction.multiply(-0.2)
                        }
                    }
                    loc.getNearbyLivingEntities(2.0).forEach {
                        val locClone = loc.clone()
                        locClone.y = it.y
                        val direction = it.location.toVector().subtract(locClone.toVector()).normalize()
                        it.velocity = direction.multiply(-0.2)
                    }
                }, 0)
                Thread.sleep(1000/4)
            }
        })
        scheduler.scheduleSyncDelayedTask(plugin, {
            EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f)
            loc.getNearbyLivingEntities(8.0).forEach {
                it.damage(2.0)
                if (it is Player) {
                    lastDamager[it] = p
                    lastWeapon[it] = LastWeaponData(ItemManager.createNamedItem(Material.STICK, 1, "중력 수류탄", null), System.currentTimeMillis()+1000*10)
                }
            }
        }, 20*5)
    }, 20)
}
private fun smokeGrenade(loc: Location, p: Player) {
    val range = 3.5f
    val size = 6.5f
    scheduler.scheduleSyncDelayedTask(plugin, {
        val entity = loc.world.spawnEntity(loc, EntityType.ITEM_DISPLAY) as ItemDisplay
        val loc2 = entity.location.clone().set(loc.x, ceil(loc.y), loc.z)
        val spawnLoc = entity.location.clone().set(loc.x, ceil(loc.y) + size/2, loc.z)
        entity.teleport(spawnLoc)
        entity.itemStack = EffectManager.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzZiMDNiNDAxNThiYjZlOThiMjA3NjZiMWQ0NmQ5MTgyYzE1YTFhNGNmNDM5ZTEzYTc2NzBiODdkMTI3NTdiIn19fQ==")
        val transform = entity.transformation
        transform.scale.set(0.0, 0.0, 0.0)
        entity.transformation = transform
        val loopTime = 10
        var smoke = true

        smokeRadius[entity] = Pair(loc2.clone().add(range.toDouble(), range.toDouble(), range.toDouble()), loc2.clone().add(-1*range.toDouble(), -1*range.toDouble(), -1*range.toDouble()))

        scheduler.runTaskAsynchronously(plugin, Runnable {
            for (i in 0 until loopTime) {
                scheduler.scheduleSyncDelayedTask(plugin, {
                    EffectManager.playSurroundSound(entity.location, Sound.ENTITY_BAT_TAKEOFF, 2.0f, 0.5f)
                    loc2.world.spawnParticle(CLOUD, loc2, 100, 0.0, 0.5, 0.0, 0.3)
                    val scale = ((size*2)/(loopTime.toFloat()))*(i+1)
                    val transform2 = entity.transformation
                    transform2.scale.set(scale, scale, scale)
                    //transform2.translation.set(scale/-2.0, scale/-2.0, scale/-2.0)
                    entity.transformation = transform2
                }, 0)
                Thread.sleep(1000/10)
            }
        })
        scheduler.runTaskAsynchronously(plugin, Runnable {
            while (smoke) {
                loc2.world.spawnParticle(CLOUD, loc2, 10, range.toDouble(), 0.0, range.toDouble(), 0.1)
                Thread.sleep(1000 / 10)
            }
        })

        scheduler.scheduleSyncDelayedTask(plugin, {
            scheduler.runTaskAsynchronously(plugin, Runnable {
                for (i in 0 until loopTime) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        EffectManager.playSurroundSound(entity.location, Sound.ENTITY_BAT_TAKEOFF, 2.0f, 0.5f)
                        loc2.world.spawnParticle(CLOUD, loc2, 100, 0.0, 0.5, 0.0, 0.3)
                        val scale = ((size*2)/(loopTime.toFloat()))*(10 - (i+1))
                        val transform2 = entity.transformation
                        transform2.scale.set(scale, scale, scale)
                        //transform2.translation.set(scale/-2.0, scale/-2.0, scale/-2.0)
                        entity.transformation = transform2
                    }, 0)
                    Thread.sleep(1000/10)
                }
                scheduler.scheduleSyncDelayedTask(plugin, {
                    smokeRadius.remove(entity)
                    entity.remove()
                    smoke = false
                }, 0)
            })
        }, 20*40)
    }, 20*1)
}

private fun isDiamond(material: Material): Boolean {
    val list = listOf(
        Material.DIAMOND_HELMET,
        Material.DIAMOND_CHESTPLATE,
        Material.DIAMOND_LEGGINGS,
        Material.DIAMOND_BOOTS,
        Material.DIAMOND_SWORD
    )
    return list.any { list.contains(material) }
}

class CustomItemEvent: Listener {
    companion object {
        val GrenadeCD = HashMap<UUID, Long>()
        val smokeRadius = HashMap<ItemDisplay, Pair<Location, Location>>()
    }


    @EventHandler
    fun onThrowGrenade(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        val cd = 3
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}영역 수류탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("${ChatColor.RED} 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L


            EffectManager.playSurroundSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0F, 1.0F)

            val tick = CustomItemManager.drawOrbital(player, player.eyeLocation, 15, 100, Color.WHITE, false, 9.81)
            CustomItemManager.throwOrbital(player, player.eyeLocation, 15, tick, Color.GREEN, 9.81)
            val loc = CustomItemManager.getOrbitalLoc(player, player.eyeLocation, 15, tick, 9.81)

            scheduler.scheduleSyncDelayedTask(Main.plugin, {
                earthGrenade(loc, player)
            }, tick.toLong())
        }
//        else if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}반중력 수류탄") {
//            e.isCancelled = true
//
//            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
//                player.sendMessage(Component.text("${ChatColor.RED} 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
//                return
//            }
//
//            player.inventory.itemInMainHand.amount -= 1
//            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L
//
//            EffectManager.playSurroundSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0F, 1.0F)
//
//            val tick = CustomItemManager.drawOrbital(player, player.eyeLocation, 15, 100, Color.WHITE, false, 9.81)
//            CustomItemManager.throwOrbital(player, player.eyeLocation, 15, tick, Color.GREEN, 9.81)
//            val loc = CustomItemManager.getOrbitalLoc(player, player.eyeLocation, 15, tick, 9.81)
//            scheduler.scheduleSyncDelayedTask(Main.plugin, {
//                antiGravityGrenade(loc, player)
//            }, tick.toLong())
//        }
        else if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}중력 수류탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("${ChatColor.RED} 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L

            EffectManager.playSurroundSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0F, 1.0F)

            val tick = CustomItemManager.drawOrbital(player, player.eyeLocation, 15, 100, Color.WHITE, false, 9.81)
            CustomItemManager.throwOrbital(player, player.eyeLocation, 15, tick, Color.GREEN, 9.81)
            val loc = CustomItemManager.getOrbitalLoc(player, player.eyeLocation, 15, tick, 9.81)
            scheduler.scheduleSyncDelayedTask(Main.plugin, {
                gravityGrenade(loc, player)
            }, tick.toLong())
        } else if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}연막탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("${ChatColor.RED} 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L

            EffectManager.playSurroundSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0F, 1.0F)

            val tick = CustomItemManager.drawOrbital(player, player.eyeLocation, 15, 100, Color.WHITE, false, 9.81)
            CustomItemManager.throwOrbital(player, player.eyeLocation, 15, tick, Color.GREEN, 9.81)
            val loc = CustomItemManager.getOrbitalLoc(player, player.eyeLocation, 15, tick, 9.81)
            scheduler.scheduleSyncDelayedTask(Main.plugin, {
                smokeGrenade(loc, player)
            }, tick.toLong())
        }
    }

    @EventHandler
    fun revelationDamage(e: EntityDamageByEntityEvent) {
        val attacker = e.damager
        val victim = e.entity
        val data = WorldManager.initData(attacker.world)
        if ((data.playerKill[attacker.uniqueId] ?: 0) < 3) {
            if (attacker is Player && victim is LivingEntity && isHittable(attacker, victim)) {
                if (attacker.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}${ChatColor.BOLD}Revelation") {
                    if (attacker.getCooldown(Material.GOLDEN_SWORD) > 0) {
                        attacker.sendMessage(
                            Component.text(
                                "${ChatColor.RED} 쿨타임 중 입니다. (${
                                    attacker.getCooldown(
                                        Material.GOLDEN_SWORD
                                    ).toDouble() / 20.0
                                }초)"
                            )
                        )
                        return
                    }
                    attacker.setCooldown(Material.GOLDEN_SWORD, 20 * 5)

                    val dmg = (50 - 20 * (data.playerKill[attacker.uniqueId] ?: 0)).toDouble() / 10.0
                    victim.damage(dmg)
                    victim.world.strikeLightningEffect(victim.location)
                    EffectManager.playSurroundSound(
                        victim.location,
                        Sound.ITEM_TRIDENT_THUNDER,
                        1.0F,
                        1.0F + ((10 - 2 * (data.playerKill[attacker.uniqueId] ?: 0)).toFloat() / 10.0F)
                    )
                }
            }
        }
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        if (projectile is Arrow) {
            val shooter = projectile.shooter as? Player ?: return
            if (shooter.inventory.itemInMainHand.itemMeta?.displayName != "${ChatColor.YELLOW}${ChatColor.BOLD}Vallista") {
                return
            }
            if (shooter.getCooldown(Material.CROSSBOW) > 0) return
            shooter.setCooldown(Material.CROSSBOW, 20 * 1)

            val entities = LinkedHashSet<LivingEntity>()

            val loc = projectile.location
            val dir = projectile.velocity
            EffectManager.playSurroundSound(shooter.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0F, 0.5F)
            EffectManager.playSurroundSound(shooter.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0F, 1.0F)
            EffectManager.playSurroundSound(shooter.location, Sound.ITEM_CROSSBOW_SHOOT, 2.0F, 2.0F)
            EffectManager.playSurroundSound(shooter.location, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 2.0F)
            shooting@ for (i in 0 until 100 * 100) {
                val pos = loc.clone().add(dir.clone().multiply(i / 100.0))
                if (!pos.isChunkLoaded) break@shooting

                if (pos.block.isSolid) break@shooting


                if (i % 10 == 0) pos.world.spawnParticle(Particle.END_ROD, pos, 1, 0.0, 0.0, 0.0, 0.0)
                pos.getNearbyLivingEntities(10.0, 10.0, 10.0).forEach {
                    if (it is LivingEntity && it.boundingBox.clone().expand(1.1).contains(
                            pos.x,
                            pos.y,
                            pos.z
                        ) && !(it is Player && it.gameMode == GameMode.SPECTATOR) && !TeamManager.isSameTeam(
                            shooter.world,
                            shooter,
                            it
                        )
                    ) {
                        entities.add(it)
                    }
                }

            }

            entities.remove(shooter)

            // 파티클에 맞은 엔티티에게 대미지 적용
            for (entity in entities) {
                entity.damage(4.0)
                val direction = entity.location.toVector().subtract(shooter.location.clone().toVector()).normalize()
                entity.velocity = direction.multiply(0.5).setY(0.4)
                if (entity is Player) {
                    lastDamager[entity] = shooter
                    lastWeapon[entity] = LastWeaponData(
                        ItemManager.createNamedItem(
                            Material.CROSSBOW,
                            1,
                            "${ChatColor.YELLOW}${ChatColor.BOLD}Vallista",
                            null
                        ), System.currentTimeMillis() + 1000 * 10
                    )
                    if (entity.isBlocking) {
                        CustomItemManager.disablePlayerShield(entity)
                        entity.setCooldown(Material.SHIELD, 20 * 8)
                    }
                }
            }
            if (entities.isNotEmpty()) shooter.playSound(shooter, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 2.0F)

            projectile.remove()
        }
    }

    @EventHandler
    fun onUseCube(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}반중력 큐브") {
            e.isCancelled = true

            if (player.getCooldown(Material.LIGHT_BLUE_DYE) > 0) return
            player.setCooldown(Material.LIGHT_BLUE_DYE, 20 * 2)

            player.inventory.itemInMainHand.amount -= 1
            val loc = player.eyeLocation
            val exLoc = loc.clone()
            exLoc.add(loc.direction.multiply(2.0))
            EffectManager.playSurroundSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 2.0f)
            exLoc.getNearbyLivingEntities(2.5).forEach {
                val direction = it.location.toVector().subtract(exLoc.toVector()).normalize()
                it.velocity = direction.multiply(1.2)
            }
        }
    }

    @EventHandler
    fun onUseConverter(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}컨버터") {
            val inv = Bukkit.createInventory(null, 27, "${ChatColor.DARK_BLUE}Converter")
            for (i in 0 until 27) {
                inv.setItem(i, ItemManager.createNamedItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", null))
            }
            for (i in 11..15) {
                inv.setItem(i, ItemStack(Material.AIR))
            }
            inv.setItem(22, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "${ChatColor.GREEN}Convert", null))
            inv.setItem(
                26,
                ItemManager.createNamedItem(
                    Material.REDSTONE_TORCH,
                    1,
                    "${ChatColor.RED}도움말",
                    listOf(
                        "${ChatColor.GRAY}다이아몬드 장비(갑옷, 무기) 5개를 이용해서",
                        "${ChatColor.GRAY}근접 대미지를 감소시키는 아이템을 제작할 수 있습니다.",
                        "${ChatColor.GRAY}(최대 5개까지 중첩 가능)"
                    )
                )
            )
            e.player.openInventory(inv)
        }
    }

    @EventHandler
    fun onConverterInvClick(e: InventoryClickEvent) {
        if (e.view.title == "${ChatColor.DARK_BLUE}Converter" && e.clickedInventory == e.view.topInventory) {
            if (e.slot in 11..15) return
            e.isCancelled = true
            if (e.slot == 22) {
                var bool = true
                search@ for (i in 11..15) {
                    if (!isDiamond(e.view.topInventory.getItem(i)?.type ?: Material.AIR)) {
                        bool = false
                        break@search
                    }
                }

                if (bool) {
                    for (i in 11..15) {
                        e.view.topInventory.setItem(i, ItemStack(Material.AIR))
                    }
                    val p = e.view.player
                    p.world.dropItem(
                        p.eyeLocation,
                        ItemManager.createNamedItem(
                            Material.ECHO_SHARD,
                            1,
                            "${ChatColor.BLUE}${ChatColor.BOLD}보호의 조각",
                            listOf("${ChatColor.GRAY}물리 피해 대미지를 조각 하나당 5% 감소시킵니다.", "${ChatColor.GRAY}(최대 5개까지 적용)")
                        )
                    )
                    EffectManager.playSurroundSound(p.location, Sound.BLOCK_SMITHING_TABLE_USE, 1.0F, 1.0F)
                }
            }
        }
    }

    @EventHandler
    fun onConverterInvClick(e: InventoryCloseEvent) {
        if (e.view.title == "${ChatColor.DARK_BLUE}Converter") {
            val p = e.player
            for (i in 11..15) {
                p.world.dropItem(p.eyeLocation, e.inventory.getItem(i) ?: ItemStack(Material.AIR))
            }
        }
    }

    @EventHandler
    fun reduceDamage(e: EntityDamageByEntityEvent) {
        val victim = e.entity

        if (victim is Player) {
            var shard = countItemsWithName(victim, "${ChatColor.BLUE}${ChatColor.BOLD}보호의 조각")
            if (shard > 5) {
                shard = 5
            }
            if (e.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK && shard > 0) {
                e.damage * (1.0 - (shard.toDouble() * 4.0 / 100.0))
                e.entity.world.playSound( //그대로
                    e.entity.location,
                    Sound.ENTITY_WARDEN_ATTACK_IMPACT,
                    1.0F,
                    2.0F - shard * 0.15F
                )
            }

            if (e.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK && victim.inventory.itemInOffHand.type == Material.SHIELD) {
                if (victim.inventory.itemInOffHand.getEnchantmentLevel(Enchantment.DURABILITY) == 3) {
                    e.damage *= 0.8
                } else {
                    e.damage *= 0.9
                }
            }
        }
    }

    @EventHandler
    fun onUseCarrot(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.GOLD}Golden Carrot") {
            e.isCancelled = true
            if (player.getCooldown(Material.GOLDEN_CARROT) > 0) return
            player.setCooldown(Material.GOLDEN_CARROT, 20 * 1)
            player.inventory.itemInMainHand.amount -= 1


            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f)
            player.addPotionEffect(PotionEffect(PotionEffectType.HEAL, 1, 0, false, false))
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0, false, false))
            player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 20 * 60 * 5, 0, false, false))
        }
    }

    @EventHandler
    fun onUseFlare(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.RED}Flare Gun") {
            e.isCancelled = true
            player.inventory.itemInMainHand.amount -= 1

            EffectManager.playSurroundSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f)
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 2.0f)

            val loc = player.eyeLocation

            var shooting = true

            for (i in 0 until 175) {
                scheduler.scheduleSyncDelayedTask(plugin, {
                    val entities = ArrayList<LivingEntity>()
                    sh@ for (n in 0 until 20) {
                        if (shooting) {
                            loc.add(loc.direction.multiply(0.1))
                            if (i > 1) loc.world.spawnParticle(
                                REDSTONE,
                                loc,
                                1,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                DustOptions(Color.RED, 1.0F)
                            )
                            if (loc.block.isSolid) {
                                shooting = false
                                break@sh
                            }
                            loc.getNearbyLivingEntities(10.0, 10.0, 10.0).filterNot { it == player }.forEach {
                                if (it.boundingBox.contains(loc.x, loc.y, loc.z) && isHittable(player, it)) {
                                    entities.add(it)

                                }
                            }
                            if (entities.isNotEmpty()) {
                                shooting = false
                                break@sh
                            }
                        }
                    }
                    if (entities.isNotEmpty()) EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)

                    entities.forEach {
                        if (it is Player) {
                            lastDamager[it] = e.player
                            lastWeapon[it] = LastWeaponData(
                                ItemManager.createNamedItem(
                                    Material.CROSSBOW,
                                    1,
                                    "${ChatColor.RED}Flare Gun",
                                    null
                                ), System.currentTimeMillis() + 1000 * 10
                            )
                        }
                        it.damage(3.0)
                        it.fireTicks = 20 * 5
                    }
                }, i.toLong())
            }
            scheduler.scheduleSyncDelayedTask(plugin, {
                if (loc.y >= 200) {
                    val random = Random
                    val dropLoc = e.player.location.clone()
                        .add(random.nextInt(-5, 5).toDouble(), 0.0, random.nextInt(-5, 5).toDouble())
                    dropLoc.y = 320.0
                    val entity = loc.world.spawnEntity(dropLoc, EntityType.ARMOR_STAND) as ArmorStand
                    entity.setItem(EquipmentSlot.HEAD, ItemStack(Material.CHEST))
                    entity.isInvulnerable = true
                    entity.isInvisible = true
                    entity.addScoreboardTag("Entity-Supplies")
                }
            }, 175)
        }
    }

    @EventHandler
    fun onUsePrototype(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}${ChatColor.BOLD}Prototype V3") {
            if (player.getCooldown(Material.NETHERITE_SHOVEL) > 0) return
            player.setCooldown(Material.NETHERITE_SHOVEL, 20 * 4)

            val loc = player.eyeLocation
            val entities = ArrayList<LivingEntity>()
            var dmgType = 0

            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
            EffectManager.playSurroundSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 2.0f)

            sh@ for (i in 0 until 10 * 320) {
                loc.add(loc.direction.multiply(0.1))
                if (i > 10) loc.world.spawnParticle(REDSTONE, loc, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color.AQUA, 1.0F))
                if (loc.block.isSolid) {
                    break@sh
                }
                for (it in loc.getNearbyLivingEntities(10.0, 10.0, 10.0).filterNot { it == player }) {
                    if (it.boundingBox.clone().expand(1.1).contains(
                            loc.x,
                            loc.y,
                            loc.z
                        ) && !entities.contains(it) && !TeamManager.isSameTeam(
                            player.world,
                            player,
                            it
                        ) && !(it is Player && it.gameMode == GameMode.SPECTATOR)
                    ) {
                        entities.add(it)

                        if (i in 0..20 * 10) {
                            it.damage(3.0)
                            if (it is Player) {
                                lastDamager[it] = e.player
                                lastWeapon[it] = LastWeaponData(
                                    ItemManager.createNamedItem(
                                        Material.NETHERITE_SHOVEL,
                                        1,
                                        "${ChatColor.AQUA}${ChatColor.BOLD}Prototype V3",
                                        null
                                    ), System.currentTimeMillis() + 1000 * 10
                                )
                            }
                            if (dmgType == 0) dmgType = 1
                            player.sendMessage(Component.text("${ChatColor.AQUA}DMG: 3.0"))
                        } else {
                            var am = 2.5 * log(i.toDouble() / 10.0 + 3.0, 3.0)
                            if (am > 10.0) am = 10.0
                            it.damage(am)
                            if (it is Player) {
                                lastDamager[it] = e.player
                                lastWeapon[it] = LastWeaponData(
                                    ItemManager.createNamedItem(
                                        Material.NETHERITE_SHOVEL,
                                        1,
                                        "${ChatColor.AQUA}${ChatColor.BOLD}Prototype V3*",
                                        null
                                    ), System.currentTimeMillis() + 1000 * 10
                                )
                            }
                            player.sendMessage(Component.text("${ChatColor.AQUA}DMG: ${(am * 10.0).roundToInt() / 10.0}"))
                            dmgType = 2
                        }
                    }
                }
            }
            if (dmgType == 1) player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f)
            else if (dmgType == 2) {
                player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f)
                player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f)
                player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f)
            }
        }
    }

    @EventHandler
    fun lockPlayer1(e: PlayerMoveEvent) {
        if (lockedPlayer.contains(e.player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun lockPlayer2(e: PlayerSwapHandItemsEvent) {
        if (lockedPlayer.contains(e.player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun lockPlayer3(e: PlayerDropItemEvent) {
        if (lockedPlayer.contains(e.player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun lockPlayer4(e: PlayerItemHeldEvent) {
        if (lockedPlayer.contains(e.player)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onUseCompass(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.RED}Player Tracker") {
            if (player.getCooldown(Material.COMPASS) > 0) return
            player.setCooldown(Material.COMPASS, 20 * 10)


            //팀 모드때 수정

            val targets = player.location.getNearbyPlayers(160.0).filter { it != player }
                .filter { !TeamManager.isSameTeam(it.world, it, player) }
                .filter { it is Player && it.gameMode != GameMode.SPECTATOR }
                .sortedBy { player.location.distance(it.location) }
            if (targets.isNotEmpty()) {
                player.playSound(player, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.5f)
                player.inventory.itemInMainHand.amount -= 1
                for (i in 0 until 40) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        drawLine(player.location, targets[0].location, 0.2, 196, 52, 45, player)
                    }, i * 10L)
                }
            } else {
                player.sendMessage("${ChatColor.RED}플레이어를 찾을 수 없습니다.")
            }
        }
    }

    @EventHandler
    fun onUsePrismShooter(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}Prism Shooter") {
            e.isCancelled = true
            if (player.getCooldown(Material.IRON_SHOVEL) > 0) return
            player.setCooldown(Material.IRON_SHOVEL, 20 * 10)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 4, 3, false, false))
            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 4, 4, false, false))

            var r = 0
            val colors = listOf(
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.LIME,
                Color.GREEN,
                Color.AQUA,
                Color.BLUE,
                Color.PURPLE
            )
            for (n in 0 until 10) {
                val color = colors.random()
                scheduler.scheduleSyncDelayedTask(plugin, {
                    val loc = player.eyeLocation.clone()
                    EffectManager.playSurroundSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2.0f, 2.0f)
                    EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2.0f)
                    var hit = false

                    sh@ for (i in 0 until 10 * 320) {
                        loc.add(loc.direction.multiply(0.1))
                        if (i > 10) loc.world.spawnParticle(
                            REDSTONE,
                            loc,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            DustOptions(color, 1.0F)
                        )
                        if (loc.block.isSolid) {
                            hit = true
                            break@sh
                        }
                        for (it in loc.getNearbyLivingEntities(10.0, 10.0, 10.0).filterNot { it == player }) {
                            if (it.boundingBox.contains(
                                    loc.x,
                                    loc.y,
                                    loc.z
                                ) && !(it is Player && it.gameMode == GameMode.SPECTATOR)
                            ) {
                                hit = true
                                break@sh
                            }
                        }
                    }

                    if (hit) {
                        if (r % 5 == 0) {
                            fireworkWithColor(loc, Color.RED)
                        } else if (r % 5 == 1) {
                            fireworkWithColor(loc, Color.GREEN)
                        } else if (r % 5 == 2) {
                            fireworkWithColor(loc, Color.BLUE)
                        } else if (r % 5 == 3) {
                            fireworkWithColor(loc, Color.GRAY)
                        } else if (r % 5 == 4) {
                            fireworkWithColor(loc, Color.WHITE)
                        }
                        loc.getNearbyLivingEntities(3.0).filter { !TeamManager.isSameTeam(player.world, player, it) }
                            .filter { !(it is Player && it.gameMode == GameMode.SPECTATOR) }.forEach {
                            if (it is Player) {
                                lastDamager[it] = player
                                lastWeapon[it] = LastWeaponData(
                                    ItemManager.createNamedItem(
                                        Material.IRON_SHOVEL,
                                        1,
                                        "${ChatColor.YELLOW}Prism Shooter",
                                        null
                                    ), System.currentTimeMillis() + 1000 * 10
                                )
                            }

                            //대미지
                            val dmg = 0.2
                            if (it.health > dmg) {
                                it.health -= dmg
                            } else {
                                if (!it.isDead) it.health = 0.0
                            }


                            if (r % 5 == 0) {
                                it.fireTicks = 20 * 5
                            } else if (r % 5 == 1) {
                                it.addPotionEffect(PotionEffect(PotionEffectType.POISON, 20 * 3, 0, false, false))
                            } else if (r % 5 == 2) {
                                it.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 3, 0, false, false))
                            } else if (r % 5 == 3) {
                                it.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 3, 0, false, false))
                            } else if (r % 5 == 4) {
                                it.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 3, 0, false, false))
                            }
                        }
                        r++
                    }
                }, n * 2L)
            }

        }
    }

    @EventHandler
    fun onUseSelectBook(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.GREEN}${ChatColor.BOLD}가젯 선택") {
            e.isCancelled = true
            val inv = Bukkit.createInventory(null, 27, "${ChatColor.GRAY}${ChatColor.BOLD}가젯 선택")
            for (i in 0 until 27) {
                inv.setItem(i, ItemManager.createNamedItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", null))
            }

            inv.setItem(
                11,
                ItemManager.createNamedItem(
                    Material.RED_DYE,
                    3,
                    "${ChatColor.RED}대미지 개조",
                    listOf("${ChatColor.GRAY}사용시 10초간 힘 2가 부여됩니다.")
                )
            )
            inv.setItem(
                13,
                CustomItemData.getDivinityShield().setCount(3)
            )
            inv.setItem(
                15,
                ItemManager.createNamedItem(
                    Material.GREEN_DYE,
                    3,
                    "${ChatColor.YELLOW}특급 영양식 키트",
                    listOf("${ChatColor.GRAY}사용시 즉시 회복 3 효과와 10초간 받는 대미지를 20% 감소시킵니다.")
                )
            )

            player.openInventory(inv)
        }
    }

    @EventHandler
    fun onClickSelectBook(e: InventoryClickEvent) {
        if (e.clickedInventory == e.view.topInventory && e.view.title == "${ChatColor.GRAY}${ChatColor.BOLD}가젯 선택") {
            e.isCancelled = true
            val player = e.view.player as Player

            if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.GREEN}${ChatColor.BOLD}가젯 선택") {
                val item = when (e.slot) {
                    11 -> {
                        ItemManager.createNamedItem(
                            Material.RED_DYE,
                            3,
                            "${ChatColor.RED}대미지 개조",
                            listOf("${ChatColor.GRAY}사용시 10초간 힘 2가 부여됩니다.", "${ChatColor.GRAY}Gadget")
                        )
                    }

                    13 -> CustomItemData.getDivinityShield().setCount(3)

                    15 -> {
                        ItemManager.createNamedItem(
                            Material.GREEN_DYE,
                            3,
                            "${ChatColor.YELLOW}특급 영양식 키트",
                            listOf(
                                "${ChatColor.GRAY}사용시 즉시 회복 3 효과와 10초간 받는 대미지를 20% 감소시킵니다.",
                                "${ChatColor.GRAY}Gadget"
                            )
                        )
                    }

                    else -> null
                }

                if (item != null) {
                    player.inventory.itemInMainHand.amount -= 1
                    player.inventory.addItem(item)
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)
                    player.inventory.close()
                }
            }
        }
    }

    @EventHandler
    fun onUseGadget1(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.RED}대미지 개조") {
            e.isCancelled = true
            if (player.getCooldown(Material.RED_DYE) > 0) return
            player.setCooldown(Material.RED_DYE, 20 * 15)
            player.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 10, 1, false, true))
            EffectManager.playSurroundSound(player.location, Sound.BLOCK_SMITHING_TABLE_USE, 1.0F, 2.0F)
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8F, 1.2F)

            player.inventory.itemInMainHand.amount -= 1
        }
    }

    @EventHandler
    fun onUseGadget2(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == ItemManipulator.itemName(CustomItemData.getDivinityShield())) {
            e.isCancelled = true
            if (player.getCooldown(Material.YELLOW_DYE) > 0) return
            player.setCooldown(Material.YELLOW_DYE, 20 * 15)
            player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 4, false, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 10, 0, false, true))
            EffectManager.playSurroundSound(player.location, Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.4F)
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8F, 1.2F)

            player.inventory.itemInMainHand.amount -= 1
        }
    }

    @EventHandler
    fun onUseGadget3(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}특급 영양식 키트") {
            e.isCancelled = true
            if (player.getCooldown(Material.GREEN_DYE) > 0) return
            player.setCooldown(Material.GREEN_DYE, 20 * 10)
            player.addPotionEffect(PotionEffect(PotionEffectType.HEAL, 1, 2, false, false))
            player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 0, false, true))
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EAT, 1.0F, 1.0F)
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8F, 1.2F)

            player.inventory.itemInMainHand.amount -= 1
        }
    }

    @EventHandler
    fun onUsePurify(e: EntityShootBowEvent) {

        val shooter = e.entity
        if (shooter !is Player) return
        if (lockedPlayer.contains(shooter)) {
            e.isCancelled = true
            return
        }

        val item = shooter.inventory.itemInMainHand
        if (item.itemMeta?.displayName == "${ChatColor.YELLOW}${ChatColor.BOLD}Purify") {
            var chargeShot = false
            if (shooter.world.name.contains("Field-")) {
                val dataClass = WorldManager.initData(shooter.world)
                chargeShot = ((dataClass.playerKill[shooter.uniqueId] ?: 0) == 0)
                println(dataClass.playerKill[shooter.uniqueId] ?: 0)
            } else {
                chargeShot = true
            }

            if (chargeShot && !shooter.isOnGround) {
                shooter.setCooldown(Material.BOW, 20 * 8)
                e.projectile.remove()

                val delay = e.force * 0.8
                val loc = shooter.eyeLocation.clone()

                val readyParticleLoc = loc.clone()
                for (i in 0 until (delay * 10).roundToInt()) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        drawParticleCircle(shooter.location, 1.5, Color.WHITE)
                        EffectManager.playSurroundSound(
                            readyParticleLoc,
                            Sound.ENTITY_WITHER_SHOOT,
                            0.5f,
                            (0.4f + 0.2f * i)
                        )


                    }, i * 2L)
                }


                CustomItemManager.lockPlayer(shooter)



                scheduler.scheduleSyncDelayedTask(plugin, {
                    shooter.setCooldown(Material.BOW, 20 * 3)
                    CustomItemManager.unlockPlayer(shooter)
                    val pLoc = shooter.location.clone()

                    pLoc.add(pLoc.direction.multiply(0.5))

                    val dir = shooter.location.toVector().subtract(pLoc.toVector()).normalize()
                    shooter.velocity = dir.multiply(1.5 * e.force)

                    shooter.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 2, 0, false, false))

                    EffectManager.playSurroundSound(readyParticleLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
                    EffectManager.playSurroundSound(readyParticleLoc, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.8f)
                    EffectManager.playSurroundSound(readyParticleLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f)

                    sh@ for (i in 0 until 10 * 320) {
                        loc.add(loc.direction.multiply(0.1))
                        if (i > 10) loc.world.spawnParticle(
                            REDSTONE,
                            loc,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            DustOptions(Color.WHITE, 1.0F)
                        )
                        if (loc.block.isSolid) {
                            break@sh
                        }
                        for (it in loc.getNearbyLivingEntities(10.0, 10.0, 10.0).filter { isHittable(shooter, it) }.filterNot { it == shooter }) {
                            if (it.boundingBox.expand(1.2).contains(loc.x, loc.y, loc.z)) {
                                break@sh
                            }
                        }
                    }

                    loc.world.spawnParticle(EXPLOSION_HUGE, loc, 10, 1.0, 1.0, 1.0, 0.0)
                    drawParticleCircle(loc.clone().add(0.0, 1.0, 0.0), 20.0, Color.GRAY)
                    val random = java.util.Random()


                    if (e.force == 1.0F) {
                        for (m in 0 until 8) {
                            val loc2 = loc.clone()
                            loc2.yaw = random.nextFloat(0.0F, 360.0F)
                            loc2.pitch = random.nextFloat(-90.0F, 90.0F)
                            sh@ for (i in 0 until 10 * 64) {
                                loc2.add(loc2.direction.multiply(0.1))
                                if (i > 5) loc2.world.spawnParticle(END_ROD, loc2, 1, 0.0, 0.0, 0.0, 0.0)
                                if (loc2.block.isSolid && i > 10 * 5) {
                                    break@sh
                                }
                            }
                        }
                    }
                    for (it in loc.getNearbyLivingEntities(20.0).filter { isHittable(shooter, it) }.filter { it != shooter }) {
                        if (it is Player) {
                            lastDamager[it] = shooter
                            lastWeapon[it] = LastWeaponData(
                                ItemManager.createNamedItem(
                                    Material.BOW,
                                    1,
                                    "${ChatColor.YELLOW}${ChatColor.BOLD}Purify",
                                    listOf(
                                        "${ChatColor.GRAY}킬이 0이고 공중에 있을 때 능력이 발동됩니다.",
                                        "${ChatColor.GRAY}체력의 일부를 소비해서 폭발하는 히트스캔 화살을 발사합니다.",
                                        "${ChatColor.GRAY}풀차징일때 넉백과 기절을 부여합니다.",
                                        "${ChatColor.GRAY}화살 발사에는 딜레이가 있으며, 자신도 맞을 수 있습니다."
                                    )
                                ), System.currentTimeMillis() + 1000 * 10
                            )
                        }

                        if (it.location.distance(loc) <= 4.0) {

                            var dmg = e.force * 4.0

                            if (e.force == 1.0F) {
                                dmg *= 2
                                it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, false, false))
                                it.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, 20 * 2, 0, false, false))
                                it.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 2, 4, false, false))

                                val direction = it.location.toVector().subtract(loc.toVector()).normalize()
                                it.velocity = direction.multiply(1.5)
                            }
                            it.damage(dmg)
                        } else {
                            it.damage(0.5)
                            val direction = it.location.toVector().subtract(loc.toVector()).normalize()
                            it.velocity = direction.multiply(0.2)
                        }
                    }
                }, (delay * 20.0).roundToLong())

            }
        }
    }

    @EventHandler
    fun onExpBowShoot(e: EntityShootBowEvent) {
        val projectile = e.projectile
        if (projectile is Arrow) {
            val shooter = projectile.shooter ?: return
            if (shooter is Player) {
                if (shooter.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}Explosive Bow") {
                    projectile.addScoreboardTag("ExposiveBow_${e.force}")
                }
            }
        }
    }

    @EventHandler
    fun onExpHit(e: ProjectileHitEvent) {
        val tags = e.entity.scoreboardTags.filter { it.contains("ExposiveBow_") }
        if (tags.isEmpty()) return
        val tag = tags[0]
        val shooter = e.entity.shooter ?: return
        if (shooter !is Player) return
        val force = tag.split("_")[1].toDouble()

        val loc = e.entity.location

        if (force >= 0.8) {

            loc.world.spawnParticle(EXPLOSION_HUGE, loc, 1, 0.0, 0.0, 0.0)
            EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F)
            val entities = loc.getNearbyLivingEntities(3.0).filter { isHittable(shooter, it) && it != shooter }
            if (entities.isNotEmpty()) {
                shooter.playSound(shooter, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.0F)
            }

            entities.forEach {
                it.damage(force * 2.0)
            }
        }

        e.entity.remove()
    }

    @EventHandler
    fun onUseAGShotGun(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.YELLOW}반중력 샷건") {
            if (player.getCooldown(Material.GLOW_INK_SAC) > 0) return
            player.setCooldown(Material.GLOW_INK_SAC, 20 * 2)

            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 5, 2, false, false))

            val random = java.util.Random()

            val loc = player.eyeLocation
            val entities = ArrayList<LivingEntity>()

            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
            EffectManager.playSurroundSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f)
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 2.0f)

            player.inventory.itemInMainHand.amount -= 1

            val exLoc = loc.clone()
            exLoc.add(loc.direction.multiply(2.0))
            exLoc.getNearbyLivingEntities(2.5).forEach {
                val direction = it.location.toVector().subtract(exLoc.toVector()).normalize()
                it.velocity = direction.multiply(1.2)
            }

            for (n in 0 until 12) {
                val loc2 = loc.clone()
                val ent = mutableSetOf<LivingEntity>()
                loc2.yaw = loc.yaw + random.nextFloat(-20.0F, 20.0F)
                loc2.pitch = loc.pitch + random.nextFloat(-10.0F, 10.0F)
                sh@ for (i in 0 until 10 * 320) {
                    loc2.add(loc2.direction.multiply(0.1))
                    if (i > 10) loc2.world.spawnParticle(
                        REDSTONE,
                        loc2,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        DustOptions(Color.AQUA, 0.3F)
                    )
                    if (loc2.block.isSolid) {
                        break@sh
                    }
                    for (it in loc2.getNearbyLivingEntities(10.0, 10.0, 10.0).filterNot { it == player }) {
                        if (it.boundingBox.clone().expand(1.1).contains(loc2.x, loc2.y, loc2.z) && isHittable(
                                player,
                                it
                            )
                        ) {
                            ent.add(it)
                        }
                    }
                }
                ent.forEach {
                    entities.add(it)
                }
            }
            if (entities.isNotEmpty()) player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f)

            val dmgEntities = mutableSetOf<LivingEntity>()

            entities.forEach {
                dmgEntities.add(it)
            }
            dmgEntities.forEach { entity ->
                entity.damage(entities.filter { it == entity }.size * 0.25)
                val direction = entity.location.toVector().subtract(exLoc.clone().toVector()).normalize()

                entity.velocity = direction.multiply(2.0).setY(0.4)
                if (entity is Player) {
                    lastDamager[entity] = player
                    lastWeapon[entity] = LastWeaponData(
                        ItemManager.createNamedItem(
                            Material.GLOW_INK_SAC,
                            1,
                            "${ChatColor.YELLOW}반중력 샷건",
                            null
                        ), System.currentTimeMillis() + 1000 * 10
                    )
                }
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 2, 0, false, false))
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 2, 4, false, false))
                entity.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 2, 2, false, false))
            }
        }
    }

    @EventHandler
    fun onUseLiberation(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (e.player.gameMode == GameMode.SPECTATOR) return
        val player = e.player
        if (e.action.isLeftClick) {
            liberationSkill(player)
        } else if (e.action.isRightClick) {
            liberationSkillTP(player)
        }
    }

    @EventHandler
    fun onAttackLiberation(e: EntityDamageByEntityEvent) {
        val player = e.damager
        if (player is Player) liberationSkill(player)
    }

    @EventHandler
    fun onStinger(e: EntityShootBowEvent) {
        val projectile = e.projectile
        if (projectile is Arrow) {
            val shooter = projectile.shooter ?: return
            if (shooter is Player) {
                if (shooter.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}${ChatColor.BOLD}Stinger") {
                    projectile.velocity = projectile.velocity.multiply(2.0)
                    projectile.addScoreboardTag("Stinger")
                    shooter.world.spawnParticle(SMOKE_LARGE, shooter.location, 3, 0.25, 0.25, 0.25, 0.0)
                    EffectManager.playSurroundSound(shooter.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
                    EffectManager.playSurroundSound(shooter.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.9f)
                }
            }
        }
    }

    @EventHandler
    fun onStingerHit(e: EntityDamageByEntityEvent) {
        val tags = e.damager.scoreboardTags.filter { it.contains("Stinger") }
        if (tags.isEmpty()) return
        val tag = tags[0]
        if (tag == "Stinger") {
            e.damage = e.damage*0.5
        }

    }
}