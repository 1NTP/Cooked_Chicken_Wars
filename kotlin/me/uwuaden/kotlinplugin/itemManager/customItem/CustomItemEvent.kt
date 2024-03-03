package me.uwuaden.kotlinplugin.itemManager.customItem

import me.uwuaden.kotlinplugin.Main.Companion.boundingBoxExpand
import me.uwuaden.kotlinplugin.Main.Companion.lastDamager
import me.uwuaden.kotlinplugin.Main.Companion.lastWeapon
import me.uwuaden.kotlinplugin.Main.Companion.lockedPlayer
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator
import me.uwuaden.kotlinplugin.assets.ItemManipulator.getName
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setCount
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setName
import me.uwuaden.kotlinplugin.cooldown.CooldownManager.setCooldown
import me.uwuaden.kotlinplugin.gameSystem.LastWeaponData
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemEvent.Companion.smokeRadius
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import net.citizensnpcs.api.CitizensAPI
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
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.*
import kotlin.random.Random

private fun throwProjectile(player: Player, delayTick: Int, forceStopTick: Int, color: Color, longThrow: Boolean, callback: (Location, Player) -> Unit) {
    val throwLoc = player.eyeLocation
    val spawnLoc = throwLoc.clone()
    spawnLoc.add(spawnLoc.direction.clone().multiply(1.0))
    val dir: Vector
    var delay = delayTick
    var forceStop = forceStopTick
    var onGround = false
    if (longThrow) {
        dir = throwLoc.direction.clone().multiply(2.0)
    } else {
        dir = throwLoc.direction.clone().multiply(0.5)
        dir.add(Vector(0.0, 0.5, 0.0))
        delay/=2
        forceStop/=2
    }
    val entity = player.world.spawnEntity(spawnLoc.clone().add(0.0, 300.0, 0.0), EntityType.ARMOR_STAND, false) as ArmorStand
    entity.isSilent = true
    entity.isInvisible = true
    entity.isSmall = true
    entity.isInvulnerable = true
    entity.teleport(spawnLoc)
    entity.velocity = dir

    entity.setDisabledSlots(EquipmentSlot.HAND)
    entity.setDisabledSlots(EquipmentSlot.OFF_HAND)
    entity.setDisabledSlots(EquipmentSlot.HEAD)
    entity.setDisabledSlots(EquipmentSlot.CHEST)
    entity.setDisabledSlots(EquipmentSlot.LEGS)
    entity.setDisabledSlots(EquipmentSlot.FEET)

    var explode = false

    scheduler.runTaskAsynchronously(plugin, Runnable {
        shooting@for (i in 0 until forceStop) {
            scheduler.scheduleSyncDelayedTask(plugin, {

                if (entity.isOnGround) {
                    if (!onGround) {
                        EffectManager.playSurroundSound(entity.location, Sound.BLOCK_LANTERN_BREAK, 1.0f, 2.0f)
                    }
                    onGround = true
                } else {
                    onGround = false
                }

                if (entity.isOnGround && !explode) {
                    explode = true
                    forceStop = i + delay //터지는 시간
                }

                entity.world.spawnParticle(Particle.REDSTONE, entity.location, 3, 0.0, 0.0, 0.0, 0.0, DustOptions(color, 1.5f))


            }, 0)
            if (i > forceStop) {
                break@shooting
            }
            Thread.sleep(1000/20)
        }

        scheduler.scheduleSyncDelayedTask(plugin, {
            callback(entity.location, player)
            entity.remove()
        }, 0)
    })
}
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
    if (player.inventory.itemInMainHand.itemMeta?.displayName == "§5§lLiberation") {
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
    if (player.inventory.itemInMainHand.itemMeta?.displayName == "§5§lLiberation") {
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
        world.spawnParticle(Particle.REDSTONE, particleLocation, 3, 0.0, 0.0, 0.0, 0.0, DustOptions(Color, 1.0f), true)
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
    val particleLoc = loc.clone().add(0.0, 0.25, 0.0)
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


    scheduler.runTaskAsynchronously(plugin, Runnable {
        for (i in 0 until 3) {
            scheduler.scheduleSyncDelayedTask(plugin, {
                blocks.forEach {
                    if (probabilityTrue((10 + (i+1)*30).toDouble())) {
                        EffectManager.breakBlock(it.location)
                    }
                }

                originLoc.getNearbyLivingEntities(r.toDouble()).forEach { e->
                    EffectManager.setLastDamager(p, e, CustomItemData.getEarthGr())
                    e.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 2, 2, false, false))
                    e.damage(0.5)
                }
                drawParticleCircle(particleLoc, 8.0, Color.fromRGB(100, 65, 23))
                EffectManager.playSurroundSound(originLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f)
            }, 0)
            Thread.sleep(1500)
        }
    })
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
                        drawParticleCircle(loc.clone().add(0.0, 0.25, 0.0), 6.0, Color.fromRGB(104, 104, 135))
                        for (t in 0..20) {
                            drawParticleCircle(loc.clone().add(0.0, 0.25, 0.0), 2.5 - t*0.1, Color.fromRGB(104 + t*4, 104 + t, 135 + t*3))
                        }
                        drawParticleCircle(loc.clone().add(0.0, 0.25, 0.0), 0.5, Color.fromRGB(255, 255, 112))
                        loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 0.25, 0.0), 40, 3.0, 3.0, 3.0, 0.0, DustOptions(Color.fromRGB(163, 111, 208), 1.5f))
                        loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 0.25, 0.0), 20, 1.0, 1.0, 1.0, 0.0, DustOptions(Color.fromRGB(255, 255, 112), 2.0f))
                        loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 0.25, 0.0), 100, 0.5, 0.5, 0.5, 0.0, DustOptions(Color.fromRGB(255, 255, 112), 1.5f))
                        loc.getNearbyLivingEntities(6.0).forEach {
                            val locClone = loc.clone()
                            locClone.y = it.y
                            val direction = it.location.toVector().subtract(locClone.toVector()).normalize()
                            val vel = it.velocity.clone()
                            vel.add(direction.multiply(-0.2))
                            it.velocity = vel
                        }
                    }
                    loc.getNearbyLivingEntities(2.0).forEach {
                        val locClone = loc.clone()
                        locClone.y = it.y
                        val direction = it.location.toVector().subtract(locClone.toVector()).normalize()
                        val vel = it.velocity.clone()
                        vel.add(direction.multiply(-0.2))
                        it.velocity = vel
                    }
                }, 0)
                Thread.sleep(1000/4)
            }
        })
        scheduler.scheduleSyncDelayedTask(plugin, {
            EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f)
            loc.world.spawnParticle(Particle.EXPLOSION_HUGE, loc.clone().add(0.0, 0.25, 0.0), 1, 0.0, 0.0, 0.0, 0.0)
            loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 0.25, 0.0), 80, 6.0, 6.0, 6.0, 0.0, DustOptions(Color.fromRGB(163, 111, 208), 1.5f))
            loc.world.spawnParticle(Particle.REDSTONE, loc.clone().add(0.0, 0.25, 0.0), 40, 2.0, 2.0, 2.0, 0.0, DustOptions(Color.fromRGB(255, 255, 112), 2.0f))

            loc.getNearbyLivingEntities(6.0).forEach {
                EffectManager.setLastDamager(p, it, CustomItemData.getGravityG())
                it.damage(2.0)
            }
        }, 20*5)
    }, 0)
}
private fun smokeGrenade(loc: Location, p: Player) {
    val range = 3.5f
    val size = 6.5f
    loc.yaw = 0.0f
    loc.pitch = 0.0f
    loc.add(0.0, 1.0, 0.0)

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
                    EffectManager.playSurroundSound(entity.location, Sound.ENTITY_BAT_TAKEOFF, 2.0f, 1.2f)
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
}
private fun molotovCocktail(loc: Location, p: Player) {
    val originLoc = loc.clone()
    val r = 4

    val particleLoc = originLoc.clone().add(0.0, 0.25, 0.0)
    EffectManager.playSurroundSound(originLoc, Sound.BLOCK_GLASS_BREAK, 2.0f, 1.5f)
    EffectManager.playSurroundSound(originLoc, Sound.ITEM_FLINTANDSTEEL_USE, 2.0f, 2.0f)
    EffectManager.playSurroundSound(originLoc, Sound.ENTITY_GHAST_SHOOT, 1.5f, 1.5f)
    scheduler.runTaskAsynchronously(plugin, Runnable {
        for (i in 0 until 10*6) {
            scheduler.scheduleSyncDelayedTask(plugin, {
                originLoc.getNearbyLivingEntities(r.toDouble() + 3.0).forEach { e->
                    val distLoc = originLoc.clone()
                    val deltaY = abs(originLoc.y - e.location.y)
                    distLoc.y = e.location.y
                    if (deltaY <= 1.1 && distLoc.distance(e.location) <= r.toDouble()) {
                        if (TeamManager.isSameTeam(loc.world, p, e)) {
                            e.fireTicks = 20 * 2
                        } else {
                            e.fireTicks = 20 * 4
                            e.setCooldown("HEAL_BAN", 20*4)
                        }
                        if (i%5 == 0) {
                            EffectManager.setLastDamager(p, e, CustomItemData.getMolt())
                            EffectManager.playSurroundSound(originLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.5f)
                        }
                    }
                }
                for (n in 0 until 60 - i) {
                    val tryLoc = particleLoc.clone().add(Random.nextDouble(-r.toDouble(), r.toDouble()), 0.0, Random.nextDouble(-r.toDouble(), r.toDouble()))
                    if (tryLoc.distance(particleLoc) <= r.toDouble()) {
                        tryLoc.world.spawnParticle(REDSTONE, tryLoc, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color.ORANGE, 2.0f), true)
                    }
                }
                particleLoc.world.spawnParticle(REDSTONE, particleLoc, 2, r.toDouble()/2.0, 1.0, r.toDouble()/2.0, 0.0, DustOptions(Color.ORANGE, 0.5f), true)
                particleLoc.world.spawnParticle(SMOKE_LARGE, particleLoc, 2, r.toDouble()/2.0, 1.0, r.toDouble()/2.0, 0.0)
                if (i%5 == 0) drawParticleCircle(particleLoc, r.toDouble(), Color.fromRGB(255, 127, 0))
            }, 0)
            Thread.sleep(100)
        }
    })
}
private fun makeLookAt(player: Player, lookat: Location): Pair<Float, Float> {
    //Clone the loc to prevent applied changes to the input loc
    val loc = player.eyeLocation.clone()

    // Values of change in distance (make it relative)
    val dx = lookat.x - loc.x
    val dy = lookat.y - loc.y
    val dz = lookat.z - loc.z

    // Set yaw
    if (dx != 0.0) {
        // Set yaw start value based on dx
        if (dx < 0) {
            loc.yaw = (1.5 * Math.PI).toFloat()
        } else {
            loc.yaw = (0.5 * Math.PI).toFloat()
        }
        loc.yaw = loc.yaw - atan(dz / dx).toFloat()
    } else if (dz < 0) {
        loc.yaw = Math.PI.toFloat()
    }

    // Get the distance from dx/dz
    val dxz = sqrt(dx.pow(2.0) + dz.pow(2.0))

    // Set pitch
    loc.pitch = (-atan(dy / dxz)).toFloat()

    // Set values, convert to degrees (invert the yaw since Bukkit uses a different yaw dimension format)
    loc.yaw = -loc.yaw * 180f / Math.PI.toFloat()
    loc.pitch = loc.pitch * 180f / Math.PI.toFloat()
    return Pair(loc.yaw, loc.pitch)
}

//private fun convertTo
private fun isPlayerBetweenLocations(loc: Location, loc1: Location, loc2: Location): Boolean {
    if (loc.world != loc1.world) return false

    val playerLocation = loc
    val minX = loc1.x.coerceAtMost(loc2.x)
    val minY = loc1.y.coerceAtMost(loc2.y)
    val minZ = loc1.z.coerceAtMost(loc2.z)
    val maxX = loc1.x.coerceAtLeast(loc2.x)
    val maxY = loc1.y.coerceAtLeast(loc2.y)
    val maxZ = loc1.z.coerceAtLeast(loc2.z)
    return (playerLocation.x >= minX) && (playerLocation.x <= maxX) && (playerLocation.y >= minY) && (playerLocation.y <= maxY) && (playerLocation.z >= minZ) && (playerLocation.z <= maxZ)
}

private fun isLocInSmoke(loc: Location): Boolean {
    var isInSmoke = false
    smokeRadius.values.forEach {
        if (isPlayerBetweenLocations(loc, it.first, it.second)) {
            if (!isInSmoke) {
                isInSmoke = true
            }
        }
    }
    return isInSmoke
}

private fun flashBang(loc: Location, p: Player) {
    var isInSmoke = false
    smokeRadius.values.forEach {
        if (isPlayerBetweenLocations(loc, it.first, it.second)) {
            if (!isInSmoke) {
                isInSmoke = true
            }
        }
    }

    EffectManager.playSurroundSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 1.2f)
    loc.world.spawnParticle(FLASH, loc, 5, 0.0, 0.0, 0.0)
    scheduler.scheduleSyncDelayedTask(plugin, {
        val players = loc.getNearbyPlayers(50.0)
        if (isInSmoke) {
            players.clear()
        } else {
            players.removeIf { isLocInSmoke(it.location) }
        }

        players.forEach { player ->
            if (player.gameMode != GameMode.SPECTATOR) {
                val flashYawPitch = makeLookAt(player, loc)
                var dYaw = player.yaw - flashYawPitch.first
                var dPitch = player.pitch - flashYawPitch.second
                while (dYaw > 180.0f) {
                    dYaw -= 360.0f
                }
                dYaw = abs(dYaw)
                dPitch = abs(dPitch)

                if (player.hasLineOfSight(loc)) {
                    if (dYaw <= 60.0f && dPitch <= 45.0f) {
                        if (TeamManager.isSameTeam(p.world, p, player)) {
                            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0, false, true))
                            scheduler.runTaskAsynchronously(plugin, Runnable {
                                for (i in 0 until 2) {
                                    scheduler.scheduleSyncDelayedTask(plugin, {
                                        player.playSound(player.location, Sound.ITEM_TRIDENT_RETURN, 1.0f, 2.0f - 1.0f*i)
                                    }, 0)
                                    Thread.sleep(50)
                                }
                            })
                        } else {
                            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 6, 0, false, true))
                            player.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, 20 * 6, 0, false, true))
                            scheduler.runTaskAsynchronously(plugin, Runnable {
                                for (i in 0 until 2) {
                                    scheduler.scheduleSyncDelayedTask(plugin, {
                                        player.playSound(player.location, Sound.ITEM_TRIDENT_RETURN, 1.0f, 2.0f - 1.0f*i)
                                    }, 0)
                                    Thread.sleep(50)
                                }
                            })
                        }
                    } else {
                        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false))
                        player.playSound(player.location, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.8f)
                    }
                }
            }
        }
    }, 0)
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e영역 수류탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("§c 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L


            throwProjectile(player, 20*2, 20*5, Color.fromRGB(41, 24, 17), !player.isSneaking, ::earthGrenade)
        } else if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e중력 수류탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("§c 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L


            throwProjectile(player, 20*2, 20*5, Color.fromRGB(37, 13, 39), !player.isSneaking, ::gravityGrenade)
        } else if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e연막탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("§c 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L


            throwProjectile(player, 20*3, 20*8, Color.GRAY, !player.isSneaking, ::smokeGrenade)
        } else if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e화염병") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("§c 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L

            throwProjectile(player, 0, 20*5, Color.fromRGB(255, 120, 14), !player.isSneaking, ::molotovCocktail)
        } else if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e섬광탄") {
            e.isCancelled = true

            if ((GrenadeCD[player.uniqueId] ?: 0) >= System.currentTimeMillis()) {
                player.sendMessage(Component.text("§c 쿨타임 중 입니다. (${((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()) / 1000}초)"))
                return
            }

            player.inventory.itemInMainHand.amount -= 1
            GrenadeCD[player.uniqueId] = System.currentTimeMillis() + cd * 1000L

            throwProjectile(player, 20 * 20, 20 * 2, Color.fromRGB(28, 42, 56), !player.isSneaking, ::flashBang)
        }
    }

    @EventHandler
    fun onVallistaShoot(e: ProjectileLaunchEvent) {
        val projectile = e.entity
        if (projectile is Arrow) {
            val shooter = projectile.shooter as? Player ?: return
            if (shooter.inventory.itemInMainHand.itemMeta?.displayName != "§e§lVallista") {
                return
            }
            if (shooter.inventory.itemInOffHand.type == Material.BOW) return
            if (shooter.inventory.itemInOffHand.type == Material.CROSSBOW) return

            if (!CitizensAPI.getNPCRegistry().isNPC(shooter)) {
                if (shooter.getCooldown(Material.CROSSBOW) > 0) return
                shooter.setCooldown(Material.CROSSBOW, 20 * 1)
            }

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
                    if (it is LivingEntity && it.boundingBox.clone().expand(boundingBoxExpand).contains(
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
                EffectManager.setLastDamager(shooter, entity, shooter.inventory.itemInMainHand)
                entity.damage(4.0)
                val direction = entity.location.toVector().subtract(shooter.location.clone().toVector()).normalize()
                entity.velocity = direction.multiply(0.5).setY(0.4)
                if (entity is Player) {
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§b반중력 큐브") {
            e.isCancelled = true

            if (player.getCooldown(Material.LIGHT_BLUE_DYE) > 0) return
            player.setCooldown(Material.LIGHT_BLUE_DYE, 20 * 2)

            player.inventory.itemInMainHand.amount -= 1
            val loc = player.location
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
    fun revelationDamage(e: EntityDamageByEntityEvent) {
        val attacker = e.damager
        val victim = e.entity
        val data = WorldManager.initData(attacker.world)
        if ((data.playerKill[attacker.uniqueId] ?: 0) < 3) {
            if (attacker is Player && victim is LivingEntity && isHittable(attacker, victim)) {
                if (attacker.inventory.itemInMainHand.itemMeta?.displayName == "§b§lRevelation") {
                    if (attacker.getCooldown(Material.GOLDEN_SWORD) > 0) {
                        attacker.sendMessage(
                            Component.text(
                                "§c 쿨타임 중 입니다. (${
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
    fun onUseConverter(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§b컨버터") {
            val inv = Bukkit.createInventory(null, 27, "§1Converter")
            for (i in 0 until 27) {
                inv.setItem(i, ItemManager.createNamedItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", null))
            }
            for (i in 11..15) {
                inv.setItem(i, ItemStack(Material.AIR))
            }
            inv.setItem(22, ItemManager.createNamedItem(Material.IRON_PICKAXE, 1, "§AConvert", null))
            inv.setItem(
                26,
                ItemManager.createNamedItem(
                    Material.REDSTONE_TORCH,
                    1,
                    "§c도움말",
                    listOf(
                        "§7다이아몬드 장비(갑옷, 무기) 5개를 이용해서",
                        "§7근접 대미지를 감소시키는 아이템을 제작할 수 있습니다.",
                        "§7(최대 5개까지 중첩 가능)"
                    )
                )
            )
            e.player.openInventory(inv)
        }
    }

    @EventHandler
    fun onConverterInvClick(e: InventoryClickEvent) {
        if (e.view.title == "§1Converter" && e.clickedInventory == e.view.topInventory) {
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
                            "§9§l보호의 조각",
                            listOf("§7물리 피해 대미지를 조각 하나당 5% 감소시킵니다.", "§7(최대 5개까지 적용)")
                        )
                    )
                    EffectManager.playSurroundSound(p.location, Sound.BLOCK_SMITHING_TABLE_USE, 1.0F, 1.0F)
                }
            }
        }
    }

    @EventHandler
    fun onConverterInvClick(e: InventoryCloseEvent) {
        if (e.view.title == "§1Converter") {
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
            var shard = countItemsWithName(victim, "§9§l보호의 조각")
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§6Golden Carrot") {
            e.isCancelled = true
            if (player.getCooldown(Material.GOLDEN_CARROT) > 0) return
            player.setCooldown(Material.GOLDEN_CARROT, 20 * 5)
            player.inventory.itemInMainHand.amount -= 1


            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f)
            player.addPotionEffect(PotionEffect(PotionEffectType.HEAL, 1, 0, false, false))
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0, false, false))
            player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 20 * 60 * 2, 0, false, false))
        }
    }

    @EventHandler
    fun onUseFlare(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§cFlare Gun") {
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
                                    "§cFlare Gun",
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§b§lPrototype V3") {
            if (player.getCooldown(Material.NETHERITE_SHOVEL) > 0) return
            player.setCooldown(Material.NETHERITE_SHOVEL, 20 * 7)

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
                    if (it.boundingBox.clone().expand(boundingBoxExpand).contains(
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
                            EffectManager.setLastDamager(player, it, player.inventory.itemInMainHand)
                            it.damage(3.0)
                            if (dmgType == 0) dmgType = 1
                            player.sendMessage(Component.text("§bDMG: 3.0"))
                        } else {
                            EffectManager.setLastDamager(player, it, player.inventory.itemInMainHand.setName("§b§lPrototype V3*"))
                            var am = 2.5 * log(i.toDouble() / 10.0 + 3.0, 3.0)
                            if (am > 10.0) am = 10.0
                            it.damage(am)
                            player.sendMessage(Component.text("§bDMG: ${(am * 10.0).roundToInt() / 10.0}"))
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§cPlayer Tracker") {
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
                player.sendMessage("§c플레이어를 찾을 수 없습니다.")
            }
        }
    }

    @EventHandler
    fun onUsePrismShooter(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§ePrism Shooter") {
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
                                        "§ePrism Shooter",
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§a§l가젯 선택") {
            e.isCancelled = true
            val inv = Bukkit.createInventory(null, 27, "§7§l가젯 선택")
            for (i in 0 until 27) {
                inv.setItem(i, ItemManager.createNamedItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", null))
            }

            inv.setItem(
                11,
                ItemManager.createNamedItem(
                    Material.RED_DYE,
                    3,
                    "§c대미지 개조",
                    listOf("§7사용시 10초간 힘 2가 부여됩니다.")
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
                    "§e특급 영양식 키트",
                    listOf("§7사용시 즉시 회복 3 효과와 10초간 받는 대미지를 20% 감소시킵니다.")
                )
            )

            player.openInventory(inv)
        }
    }

    @EventHandler
    fun onClickSelectBook(e: InventoryClickEvent) {
        if (e.clickedInventory == e.view.topInventory && e.view.title == "§7§l가젯 선택") {
            e.isCancelled = true
            val player = e.view.player as Player

            if (player.inventory.itemInMainHand.itemMeta?.displayName == "§a§l가젯 선택") {
                val item = when (e.slot) {
                    11 -> {
                        ItemManager.createNamedItem(
                            Material.RED_DYE,
                            3,
                            "§c대미지 개조",
                            listOf("§7사용시 10초간 힘 2가 부여됩니다.", "§7Gadget")
                        )
                    }

                    13 -> CustomItemData.getDivinityShield().setCount(3)

                    15 -> {
                        ItemManager.createNamedItem(
                            Material.GREEN_DYE,
                            3,
                            "§e특급 영양식 키트",
                            listOf(
                                "§7사용시 즉시 회복 3 효과와 10초간 받는 대미지를 20% 감소시킵니다.",
                                "§7Gadget"
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§c대미지 개조") {
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
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 10, 0, false, true))
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e특급 영양식 키트") {
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
        if (item.itemMeta?.displayName == CustomItemData.getPurify().getName()) {
            var chargeShot = false
            if (shooter.world.name.contains("Field-")) {
                val dataClass = WorldManager.initData(shooter.world)
                chargeShot = ((dataClass.playerKill[shooter.uniqueId] ?: 0) == 0)

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
                            DustOptions(Color.WHITE, 2.0F)
                        )
                        if (loc.block.isSolid) {
                            break@sh
                        }
                        for (it in loc.getNearbyLivingEntities(10.0, 10.0, 10.0).filter { isHittable(shooter, it) }.filterNot { it == shooter }) {
                            if (it.boundingBox.expand(boundingBoxExpand).contains(loc.x, loc.y, loc.z)) {
                                break@sh
                            }
                        }
                    }

                    val particleLoc = loc.clone().add(0.0, 0.2, 0.0)

                    loc.world.spawnParticle(EXPLOSION_HUGE, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)


                    if (e.force == 1.0F) {
                        drawParticleCircle(particleLoc, 8.0, Color.GRAY)
                        scheduler.runTaskAsynchronously(plugin, Runnable {
                            Thread.sleep(1000/4)
                            for (i in 0..5) {
                                scheduler.scheduleSyncDelayedTask(plugin, {
                                    drawParticleCircle(particleLoc, 0.5 + i*0.5, Color.WHITE)
                                }, 0)
                                Thread.sleep(1000/20)
                            }
                        })
                    }
                    for (it in loc.getNearbyLivingEntities(8.0).filter { isHittable(shooter, it) }.filter { it != shooter }) {
                        EffectManager.setLastDamager(shooter, it, item)

                        if (it.location.distance(loc) <= 4.0) {

                            var dmg = e.force * 4.0

                            if (e.force == 1.0F) {
                                dmg *= 2
                                it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0, false, false))
                                it.addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, 20 * 2, 0, false, false))
                                it.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 2, 4, false, false))
                                it.world.spawnParticle(REDSTONE, it.location, 600, 0.0, 100.0, 0.0, DustOptions(Color.WHITE, 1.0f))

                                val direction = it.location.toVector().subtract(loc.toVector()).normalize()
                                it.velocity = direction.multiply(1.5)
                            }
                            it.damage(dmg)
                        } else {
                            if (e.force == 1.0F) {
                                it.damage(4.0*e.force)
                                val direction = it.location.toVector().subtract(loc.toVector()).normalize()
                                it.velocity = direction.multiply(0.5)
                            }
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
                if (shooter.inventory.itemInMainHand.itemMeta?.displayName == "§eExplosive Bow") {
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
        if (player.inventory.itemInMainHand.itemMeta?.displayName == "§e반중력 샷건") {
            if (player.getCooldown(Material.GLOW_INK_SAC) > 0) return
            player.setCooldown(Material.GLOW_INK_SAC, 20 * 2)

            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * 5, 2, false, false))

            val random = java.util.Random()

            val loc = player.location
            val entities = ArrayList<LivingEntity>()

            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
            EffectManager.playSurroundSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f)
            EffectManager.playSurroundSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 2.0f)

            player.inventory.itemInMainHand.amount -= 1

            val exLoc = loc.clone()
            exLoc.add(loc.direction.multiply(2.0))
            exLoc.getNearbyLivingEntities(2.5).forEach {
                val direction = it.location.toVector().subtract(exLoc.toVector()).normalize()
                val vel = it.velocity.clone()
                vel.add(direction.multiply(1.2))
                it.velocity = vel
            }

            for (n in 0 until 12) {
                val loc2 = player.eyeLocation.clone()
                val ent = mutableSetOf<LivingEntity>()
                loc2.yaw = loc.yaw + random.nextFloat(-20.0F, 20.0F)
                loc2.pitch = loc.pitch + random.nextFloat(-10.0F, 10.0F)
                sh@ for (i in 0 until 5 * 80) {
                    loc2.add(loc2.direction.multiply(0.2))
                    if (i > 5) loc2.world.spawnParticle(
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
                        if (it.boundingBox.clone().expand(boundingBoxExpand).contains(loc2.x, loc2.y, loc2.z) && isHittable(
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
                val vel = entity.velocity.clone()
                vel.add(direction.multiply(1.2))
                entity.velocity = vel.setY(0.4)

                if (entity is Player) {
                    lastDamager[entity] = player
                    lastWeapon[entity] = LastWeaponData(
                        ItemManager.createNamedItem(
                            Material.GLOW_INK_SAC,
                            1,
                            "§e반중력 샷건",
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
                if (shooter.inventory.itemInMainHand.itemMeta?.displayName == "§b§lStinger") {
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
            e.damage *= 0.5
        }
    }
    @EventHandler
    fun onUseRocketLauncher(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == ItemManipulator.itemName(CustomItemData.getRocketLauncher())) {
            e.isCancelled = true

            if (player.getCooldown(Material.WOODEN_SHOVEL) > 0) return
            player.setCooldown(Material.WOODEN_SHOVEL, 20 * 30)

            val loc = player.eyeLocation
            var explode = false
            val exLoc = player.location.clone()
            exLoc.add(loc.direction.multiply(2.0))
            EffectManager.playSurroundSound(exLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.5f)
            EffectManager.playSurroundSound(exLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.5F)

            val kb = 1.2

            val direction = player.location.toVector().subtract(exLoc.toVector()).normalize()
            player.velocity = direction.multiply(kb)


            scheduler.runTaskAsynchronously(plugin, Runnable {
                sh@for (i in 0 until 10*320) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        if (i > 10) loc.world.spawnParticle(REDSTONE, loc, 1, 0.1, 0.1, 0.1, 0.0, DustOptions(Color.GRAY, 1.5F))
                        if (loc.block.isSolid) {
                            explode = true
                        }
                        for (it in loc.getNearbyLivingEntities(10.0, 10.0, 10.0).filterNot { it == player }.filter { isHittable(player, it) }) {
                            if (it.boundingBox.clone().expand(boundingBoxExpand).contains(loc.x, loc.y, loc.z)) {
                                explode = true
                            }
                        }
                        loc.add(loc.direction.multiply(0.1))
                    }, 0)
                    if (explode) {
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            EffectManager.playSurroundSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F)
                            loc.world.spawnParticle(EXPLOSION_HUGE, loc, 1, 0.0, 0.0, 0.0, 0.0)
                            loc.getNearbyLivingEntities(5.0).filter { isHittable(player, it) }.forEach {
                                EffectManager.setLastDamager(player, it, CustomItemData.getRocketLauncher())
                                it.damage(6.0)

                                val directionVel = it.location.toVector().subtract(loc.toVector()).normalize().setY(0.5)
                                it.velocity = directionVel.multiply(kb)

                            }
                            val blocks = mutableSetOf<Block>()

                            val r = 5
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
                        }, 0)
                        break@sh
                    }
                    Thread.sleep(2)
                }
            })
        }
    }
    @EventHandler
    fun onUseExosist(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        if (projectile is Arrow) {
            val shooter = projectile.shooter as? Player ?: return
            if (shooter.inventory.itemInMainHand.itemMeta?.displayName != CustomItemData.getExosist().getName()) {
                return
            }
            if (shooter.inventory.itemInOffHand.type == Material.BOW) return

            val entities = LinkedHashSet<LivingEntity>()

            val isGlowing = !shooter.hasCooldown(Material.PINK_DYE)
            if (isGlowing) shooter.setCooldown(Material.PINK_DYE, 20*15)

            val loc = shooter.eyeLocation.clone()
            val random = java.util.Random()
            if (!isGlowing) {
                loc.yaw += random.nextFloat(-4.0F, 4.0F)
                loc.pitch += random.nextFloat(-2.0F, 2.0F)
            }

            val dir = loc.direction
            EffectManager.playSurroundSound(shooter.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0F, 0.5F)
            EffectManager.playSurroundSound(shooter.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0F, 1.0F)
            EffectManager.playSurroundSound(shooter.location, Sound.ITEM_CROSSBOW_SHOOT, 2.0F, 2.0F)

            if (isGlowing) EffectManager.playSurroundSound(shooter.location, Sound.ITEM_TRIDENT_RETURN, 1.0F, 2.0F)
            var stopTiming = -1
            var isWallBang = false
            shooting@ for (i in 0 until 100 * 10) {
                val pos = loc.clone().add(dir.clone().multiply(i / 10.0))
                if (!pos.isChunkLoaded) break@shooting

                if (pos.block.isSolid && stopTiming == -1) {
                    isWallBang = true
                    stopTiming = i + 10*10
                }
                if (i >= stopTiming && stopTiming != -1) {
                    break@shooting
                }
                if (i%2 == 0) {
                    if (isGlowing) pos.world.spawnParticle(Particle.REDSTONE, pos, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color.fromRGB(242, 189, 205), 1.0f))
                    else pos.world.spawnParticle(Particle.REDSTONE, pos, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color.fromRGB(200, 200, 200), 1.0f))
                }

                pos.getNearbyLivingEntities(10.0, 10.0, 10.0).forEach {
                    if (it is LivingEntity && it.boundingBox.clone().expand(boundingBoxExpand).contains(
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
                if (isGlowing) {
                    entity.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 20*5, 0, false, false))
                }
                val direction = entity.location.toVector().subtract(shooter.location.clone().toVector()).normalize()
                direction.add(entity.velocity)
                entity.velocity = direction.multiply(0.2).setY(0.1)
                if (entity is Player) {
                    lastDamager[entity] = shooter
                    lastWeapon[entity] = LastWeaponData(CustomItemData.getExosist(), System.currentTimeMillis() + 1000 * 10)
                    if (entity.isBlocking) {
                        CustomItemManager.disablePlayerShield(entity)
                        entity.setCooldown(Material.SHIELD, 20 * 8)
                    }
                }
                if (isWallBang) {
                    entity.damage(1.0)
                } else {
                    entity.damage(2.0)
                }
            }
            if (entities.isNotEmpty()) shooter.playSound(shooter, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F, 2.0F)

            projectile.remove()
        }
    }
    @EventHandler
    fun onHolyShieldActive(e: EntityDamageEvent) {
        if (e.entity !is Player) return

        val player = e.entity as Player
        val itemNameList = mutableSetOf<String>()

        player.inventory.contents.forEach {
            if (it != null) itemNameList.add(it.itemMeta.displayName)
        }
        if (itemNameList.contains(CustomItemData.getHolyShield().getName())) {
            if (!player.hasCooldown(Material.NETHER_STAR) && e.finalDamage >= 5) {
                player.setCooldown(Material.NETHER_STAR, 60*20)
                e.isCancelled = true
                EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 2.0F)
                EffectManager.playSurroundSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F)
                EffectManager.playSurroundSound(player.location, Sound.ITEM_TRIDENT_RETURN, 1.0F, 0.5F)
                EffectManager.playSurroundSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0F, 0.5F)
                player.location.world.spawnParticle(END_ROD, player.location, 100, 0.0, 0.0, 0.0, 1.0)


                player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, 4, false, false))
                player.location.getNearbyLivingEntities(3.0).filter { it != player }.forEach {
                    if (CustomItemManager.isHittable(player, it)) {
                        val direction = it.location.toVector().subtract(player.location.toVector()).normalize()
                        it.velocity = direction.multiply(1.0)
                        it.damage(4.0)
                    }
                }
            }
        }

    }

    @EventHandler
    fun onUseSolar(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.OFF_HAND) return
        if (!e.action.isRightClick) return
        val player = e.player
        if (player.inventory.itemInMainHand.itemMeta?.displayName == CustomItemData.getSolarCannon().getName()) {
            if (player.getCooldown(Material.GOLDEN_SHOVEL) > 0) return
            player.setCooldown(Material.GOLDEN_SHOVEL, 20 * 8)

            val maxDamageDist = 50*10

            val hitEntities = mutableListOf<Pair<LivingEntity, Int>>()

            val weapon = player.inventory.itemInMainHand.clone()
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20*2, 2, false, false))

            scheduler.runTaskAsynchronously(plugin, Runnable {
                for (n in 0 until 4) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        EffectManager.playSurroundSound(player.location, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f + n*0.5f)
                    }, 0)
                    Thread.sleep(200)
                }
                Thread.sleep(200)
                for (n in 0 until 3) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        if (player.inventory.itemInMainHand == weapon) {
                            val loc = player.eyeLocation.clone()
                            EffectManager.playSurroundSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.65f)
                            EffectManager.playSurroundSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)

                            var charge = 0.0
                            var colorCharge = 0.2
                            var color = Color.BLACK
                            sh@ for (i in 0 until 10 * 160) {
                                loc.add(loc.direction.multiply(0.1))
                                charge = (i.toDouble()/maxDamageDist.toDouble()).coerceAtLeast(0.0).coerceAtMost(1.0)
                                colorCharge = (charge + 0.2).coerceAtLeast(0.0).coerceAtMost(1.0)
                                color = Color.fromRGB(255, 255-((92*colorCharge).toInt()), 0)
                                if (i > 10) loc.world.spawnParticle(REDSTONE, loc, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(color, (3.0*charge).toFloat() + 0.5f))

                                if (loc.block.isSolid) break@sh

                                val entities = loc.getNearbyLivingEntities(5.0, 5.0, 5.0).filterNot { it == player }.filter { it.boundingBox.clone().expand(boundingBoxExpand).contains(loc.x, loc.y, loc.z) && CustomItemManager.isHittable(player, it) }
                                if (entities.isNotEmpty()) {
                                    EffectManager.setLastDamager(player, entities.first(), weapon)
                                    entities.first().damage(2.0*charge)

                                    val direction =  entities.first().location.toVector().subtract(player.location.clone().toVector()).normalize()
                                    if (entities.first().isOnGround) entities.first().velocity = direction.multiply(0.4*(1.0 - charge)).setY(0.4)
                                    else entities.first().velocity = direction.multiply(0.4*(1.0 - charge)).setY(0.0)
                                    if (charge >= 0.4) {
                                        entities.first().fireTicks += (20*charge).roundToInt()
                                        hitEntities.add(Pair(entities.first(), i))
                                        player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f)
                                    }
                                    break@sh
                                }
                            }
                        }
                    }, 0)
                    Thread.sleep(400)
                }
                Thread.sleep(1000)
                scheduler.scheduleSyncDelayedTask(plugin, {
                    var damageEntity = HashMap<LivingEntity, Double>()

                    hitEntities.filter { it.first.world == player.world }.forEach {
                        if (it.first is Player && (it.first as Player).gameMode == GameMode.SPECTATOR) {
                            return@forEach
                        }
                        val exLoc = it.first.location.clone()
                        val charge = (it.second/maxDamageDist.toDouble() +0.2).coerceAtLeast(0.0).coerceAtMost(1.0)
                        exLoc.getNearbyLivingEntities(3.0*charge).filter { entity -> CustomItemManager.isHittable(player, entity) }.filterNot { entity -> entity is ArmorStand }.forEach { entity ->
                            damageEntity[entity] = (damageEntity[entity]?: 0.0) + 2.5*charge
                        }
                        exLoc.world.spawnParticle(EXPLOSION_HUGE, exLoc, 1, 0.0, 0.0, 0.0)
                        EffectManager.playSurroundSound(exLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)

                        val random = Random
                        val particleEntities = mutableListOf<ArmorStand>()
                        for (i in 0 until (charge * 8).toInt()) {
                            val spawnLoc = exLoc.clone().add(random.nextDouble(-1.0, 1.0), random.nextDouble(0.0, 1.0), random.nextDouble(-1.0, 1.0))

                            val entity = spawnLoc.world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND) as ArmorStand
                            entity.isSmall = true
                            entity.isInvisible = true
                            entity.isInvulnerable = true
                            entity.isSilent = true
                            entity.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20*10, 0, false, true))
                            particleEntities.add(entity)
                        }
                        particleEntities.forEach { entity ->
                            val vectorOriginLoc = exLoc.clone().add(exLoc.clone().direction.multiply(-0.5))
                            val direction = entity.location.toVector().subtract(vectorOriginLoc.toVector()).normalize().multiply(1.0)
                            entity.velocity = direction
                        }
                        scheduler.runTaskAsynchronously(plugin, Runnable {
                            for (i in 0 until 10 * 3) {
                                scheduler.scheduleSyncDelayedTask(plugin, {
                                    val colorCharge = (it.second/maxDamageDist.toDouble() +0.2).coerceAtLeast(0.0).coerceAtMost(1.0)
                                    particleEntities.forEach { armorStand ->
                                        exLoc.world.spawnParticle(REDSTONE, armorStand.location, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color.fromRGB(255, 255-((92*colorCharge).toInt()), 0), (30-i).toFloat()/10.0f))
                                    }
                                }, 0)
                                Thread.sleep(100)
                            }
                            scheduler.scheduleSyncDelayedTask(plugin, {
                                particleEntities.forEach { armorStand ->
                                    armorStand.remove()
                                }
                            }, 0)
                        })
                    }
                    damageEntity.forEach { pair ->
                        EffectManager.setLastDamager(player, pair.key, weapon)
                        pair.key.damage(pair.value)
                        pair.key.fireTicks = 20
                    }
                }, 0)
            })
        }
    }
    @EventHandler
    fun onAttackSwordofEternal(e: EntityDamageByEntityEvent) {
        val damager = e.damager
        if (damager is LivingEntity) {
            val equipment = damager.equipment ?: return
            if (equipment.itemInMainHand.itemMeta?.displayName == CustomItemData.getSwordOfEternal().getName()) {
                e.damage += 1.0
                val loc = e.entity.location.clone().add(0.0, 1.0, 0.0)
                loc.world.spawnParticle(REDSTONE, loc, 10, 1.0, 1.0, 1.0, DustOptions(Color.WHITE, 1.0f))
            }
        }
    }
}