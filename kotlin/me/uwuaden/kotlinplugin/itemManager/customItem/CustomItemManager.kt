package me.uwuaden.kotlinplugin.itemManager.customItem

import me.uwuaden.kotlinplugin.Main.Companion.lockedPlayer
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator.getName
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemEvent.Companion.GrenadeCD
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.Validate
import org.bukkit.*
import org.bukkit.Particle.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random


private fun drawLine( /* Would be your orange wool */
                      point1: Location,  /* Your white wool */
                      point2: Location,  /*Space between each particle*/
                      space: Double,
                      r: Int,
                      g: Int,
                      b: Int
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

        /*Spawn the particle at the point*/point1.world.spawnParticle(Particle.REDSTONE, p1.x, p1.y, p1.z, 1, DustOptions(Color.fromRGB(r, g, b), 1.0F))

        /* We add the space covered */covered += space
        p1.add(vector)
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
private fun addItemToChest(chest: Block, item: ItemStack) {
    val chestD = chest.state as Chest
    val emptySlots = ArrayList<Int>()
    val inv = chestD.inventory
    for (i in 0 until 27) if (chestD.inventory.getItem(i) == null) emptySlots.add(i)
    inv.setItem(emptySlots.random(), item)
}
private fun createTippedArrow(effectType: PotionEffectType, effectDuration: Int, effectAmplifier: Int, color: Color): ItemStack {
    val tippedArrow = ItemStack(Material.TIPPED_ARROW)
    val meta = tippedArrow.itemMeta as? PotionMeta

    if (meta != null) {
        val potionEffect = PotionEffect(effectType, effectDuration, effectAmplifier, false, false)
        meta.color = color
        meta.addCustomEffect(potionEffect, true)
        tippedArrow.itemMeta = meta
    }

    return tippedArrow
}
private fun findFirstBlockBetweenTwoLocations(loc1: Location, loc2: Location): Location? {
    val dx = loc2.x - loc1.x
    val dy = loc2.y - loc1.y
    val dz = loc2.z - loc1.z
    val world = loc1.world

    val stepX = if (dx < 0) -1 else 1
    val stepY = if (dy < 0) -1 else 1
    val stepZ = if (dz < 0) -1 else 1

    val maxLength = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))).roundToInt()

    var x = loc1.x
    var y = loc1.y
    var z = loc1.z

    for (i in 0..maxLength) {
        val location = Location(world, x, y, z)
        if (location.block.type != Material.AIR) {
            return location
        }

        val xFraction = (x - loc1.x) / dx
        val yFraction = (y - loc1.y) / dy
        val zFraction = (z - loc1.z) / dz

        if (xFraction < yFraction) {
            if (xFraction < zFraction) {
                x += stepX
            } else {
                z += stepZ
            }
        } else {
            if (yFraction < zFraction) {
                y += stepY
            } else {
                z += stepZ
            }
        }
    }

    return null
}

private fun isPlayerBetweenLocations(player: Player, loc1: Location, loc2: Location): Boolean {
    if (player.world != loc1.world) return false

    val playerLocation = player.eyeLocation
    val minX = loc1.x.coerceAtMost(loc2.x)
    val minY = loc1.y.coerceAtMost(loc2.y)
    val minZ = loc1.z.coerceAtMost(loc2.z)
    val maxX = loc1.x.coerceAtLeast(loc2.x)
    val maxY = loc1.y.coerceAtLeast(loc2.y)
    val maxZ = loc1.z.coerceAtLeast(loc2.z)
    return (playerLocation.x >= minX) && (playerLocation.x <= maxX) && (playerLocation.y >= minY) && (playerLocation.y <= maxY) && (playerLocation.z >= minZ) && (playerLocation.z <= maxZ)
}

object CustomItemManager {
    fun drawLine( /* Would be your orange wool */
                          point1: Location,  /* Your white wool */
                          point2: Location,  /*Space between each particle*/
                          space: Double,
                          r: Int,
                          g: Int,
                          b: Int
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

            /*Spawn the particle at the point*/point1.world.spawnParticle(Particle.REDSTONE, p1.x, p1.y, p1.z, 1, DustOptions(Color.fromRGB(r, g, b), 1.0F))

            /* We add the space covered */covered += space
            p1.add(vector)
        }
    }
    fun drawLine( /* Would be your orange wool */
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
    fun isHittable(player: Player, target: LivingEntity): Boolean {
        return !TeamManager.isSameTeam(player.world, player, target) && !(target is Player && target.gameMode == GameMode.SPECTATOR)
    }
    fun disablePlayerShield(player: Player) {
        player.world.playSound(player.location, Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f)
        val item1 = player.inventory.itemInMainHand.clone()
        if (item1.type == Material.SHIELD) {
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            scheduler.scheduleSyncDelayedTask(plugin, {
                player.inventory.setItemInMainHand(item1)
            }, 1)
        }
        val item2 = player.inventory.itemInOffHand.clone()
        if (item2.type == Material.SHIELD) {
            player.inventory.setItemInOffHand(ItemStack(Material.AIR))
            scheduler.scheduleSyncDelayedTask(plugin, {
                player.inventory.setItemInOffHand(item2)
            }, 1)
        }
    }
    fun itemSch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.forEach { player ->
                var blind = false
                CustomItemEvent.smokeRadius.values.forEach {
                    if (isPlayerBetweenLocations(player, it.first, it.second)) {
                        if (!blind) {
                            blind = true
                        }
                    }
                    if (blind) {
                        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false))
                    }
                }
            }
        }, 0, 5)
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.forEach { player ->
                val displayName = player.inventory.itemInMainHand.itemMeta?.displayName
                if (displayName != null) {
                    if (listOf("§e영역 수류탄", "§e반중력 수류탄", "§e중력 수류탄", "§e연막탄", "§e화염병").contains(displayName)) {
                        var cd = (((GrenadeCD[player.uniqueId] ?: System.currentTimeMillis()) - System.currentTimeMillis()).toDouble())/1000.0
                        if (cd < 0) cd = 0.0

                        if (player.isSneaking) player.sendActionBar(Component.text("§a던지기 모드: 가까이 던지기  §c쿨타임: ${(((cd)*10).roundToInt())/10.0}초"))
                        else player.sendActionBar(Component.text("§a던지기 모드: 멀리 던지기  §c쿨타임: ${(((cd)*10).roundToInt())/10.0}초"))
                    }
                }
                if (player.inventory.itemInMainHand.itemMeta?.displayName == CustomItemData.getExosist().getName()) {
                    if (player.name == "uwuaden") player.world.spawnParticle(CHERRY_LEAVES, player.location, 1, 1.0, 1.0, 1.0, 0.0)
                    if (!player.hasCooldown(Material.PINK_DYE)) player.world.spawnParticle(REDSTONE, player.location, 1, 1.0, 1.0, 1.0, 0.0, DustOptions(Color.fromRGB(242, 189, 205), 1.0f))
                }
                if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.AQUA}${ChatColor.BOLD}Prototype V3") {
                    player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 15, 2, false, false))
                }
                if (player.inventory.itemInMainHand.itemMeta?.displayName == "${ChatColor.DARK_PURPLE}${ChatColor.BOLD}Liberation" && player.health == player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value) {
                    if (player.location.getNearbyPlayers(30.0).filter { isHittable(player, it) }.size >= 3) player.world.spawnParticle(REVERSE_PORTAL, player.location.clone().add(0.0, 0.5, 0.0), 200, 30.0, 0.1, 30.0, 0.0)
                }
            }
        }, 0, 2)
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.forEach { w->
                w.entities.filterIsInstance<ArmorStand>().forEach {
                    if (it.scoreboardTags.contains("Entity-Supplies") && it.isOnGround) {
                        it.location.getNearbyLivingEntities(0.5).filterNot { e-> it == e }.forEach { e->
                            e.damage(15.0)
                            EffectManager.playSurroundSound(e.location, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 2.0f)
                        }
                        val chest = it.location.block

                        chest.type = Material.CHEST

                        val random = Random




                        scheduler.scheduleSyncDelayedTask(plugin, {
                            val chestMeta = chest.state as Chest
                            chestMeta.customName = "${ChatColor.YELLOW}Supplies"
                            chestMeta.update()


                            for (i in 0 until random.nextInt(2, 4)) {
                                addItemToChest(chest, CustomItemData.getGoldenCarrot())
                            }
                            for (i in 0 until random.nextInt(4, 5)) {
                                addItemToChest(chest, ItemStack(Material.GOLDEN_APPLE))
                            }
                            for (i in 0 until random.nextInt(2, 4)) {
                                addItemToChest(chest, ItemStack(Material.COOKED_BEEF, random.nextInt(1, 3)))
                            }

                            addItemToChest(chest, CustomItemData.getCompass())

                            val numList = mutableListOf<Int>()
                            for (i in 0..8) {
                                numList.add(i)
                            }




                            for (i in 0 until 2) {
                                val randomNumber = numList.random()
                                numList.remove(randomNumber)
                                when (randomNumber) {
                                    0 -> {
                                        addItemToChest(chest, ItemStack(Material.ANVIL))
                                        addItemToChest(chest, ItemManager.createEnchantedBook(Enchantment.DAMAGE_ALL, 1))
                                    }
                                    1 -> {
                                        addItemToChest(chest, ItemStack(Material.ANVIL))
                                        addItemToChest(chest, ItemManager.createEnchantedBook(Enchantment.ARROW_DAMAGE, 2))
                                    }
                                    2 -> {
                                        addItemToChest(chest, ItemStack(Material.ANVIL))
                                        addItemToChest(chest, ItemManager.createEnchantedBook(Enchantment.PROTECTION_ENVIRONMENTAL, 2))
                                    }
                                    3 -> {
                                        addItemToChest(chest, CustomItemData.getHolyShield())
                                    }
                                    4 -> {
                                        addItemToChest(chest, ItemStack(Material.ANVIL))
                                        addItemToChest(chest, ItemManager.createEnchantedBook(Enchantment.ARROW_KNOCKBACK, 1))
                                    }
                                    5 -> { addItemToChest(chest, ItemManager.createNamedItem(Material.NETHERITE_SHOVEL, 1, "${ChatColor.AQUA}${ChatColor.BOLD}Prototype V3", listOf("${ChatColor.GRAY}매우 강력한 스나이퍼 라이플입니다.", "${ChatColor.GRAY}거리가 멀수록 대미지가 증가합니다!"))) }
                                    6 -> { addItemToChest(chest, CustomItemData.getExosist()) }
                                    7 -> { addItemToChest(chest, CustomItemData.getExplosiveBow()) }
                                    8 -> { addItemToChest(chest, CustomItemData.getPurify()) }
                                }
                            }
                        }, 1)

                        it.location.getNearbyPlayers(80.0).filter { pl -> pl.gameMode == GameMode.SURVIVAL || pl.gameMode == GameMode.ADVENTURE }.forEach { pl ->
                            val loc = pl.eyeLocation.clone()
                            pl.sendMessage("${ChatColor.GREEN}주변에 보급품이 떨어졌습니다! (파티클로 방향이 표시됩니다.)")
                            pl.playSound(pl, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 0.5f)
                            for (i in 0 until 10) {
                                scheduler.scheduleSyncDelayedTask(plugin, {
                                    drawLine(it.location, loc, 0.2, 128, 128, 128, pl)
                                }, i*10L)
                            }
                        }
                        it.remove()
                    }
                }
            }
        }, 0, 5)

    }
    fun lockPlayer(player: Player) {
        lockedPlayer.add(player)
    }
    fun unlockPlayer(player: Player) {
        lockedPlayer.remove(player)
    }

    fun drawOrbital(player: Player, loc: Location, launchSpeed: Int, numPoints: Int, Color: Color, draw: Boolean, g: Double): Int {
        val yOffset = 0.0 // 포물선 곡선을 위한 높이 오프셋

        val pitchRadians = Math.toRadians(-loc.pitch.toDouble())
        val yawRadians = Math.toRadians(loc.yaw.toDouble() + 90.0)

        val timeStep = 0.1 // 시간 단계
        var t = 0.0 // 시간 변수
        var pastLoc: Location? = null
        for (i in 0 until numPoints) {
            val x = loc.x + (launchSpeed * t) * cos(pitchRadians) * cos(yawRadians)
            val y = loc.y + (launchSpeed * t) * sin(pitchRadians) - 0.5 * g * t * t + yOffset
            val z = loc.z + (launchSpeed * t) * cos(pitchRadians) * sin(yawRadians)




            val particleLocation = Location(loc.world, x, y, z)
            if (pastLoc != null) {
                val l = findFirstBlockBetweenTwoLocations(pastLoc, particleLocation)
                if (l != null) {
                    if (draw) player.spawnParticle(Particle.REDSTONE, l, 10, 0.2, 0.2, 0.2, 0.0, DustOptions(org.bukkit.Color.RED, 1.0F))
                    return i
                }
            }
            if (loc.y >= 320) {
                return i
            }
            pastLoc = particleLocation

            // 파티클로 궤도를 보여주기
            if (i >= 1 && draw) player.spawnParticle(Particle.REDSTONE, particleLocation, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color, 1.0F))

            t += timeStep
        }
        return numPoints
    }
    fun throwOrbital(player: Player, loc: Location, launchSpeed: Int, tick: Int, Color: Color, g: Double) { //tick
        val yOffset = 0.0 // 포물선 곡선을 위한 높이 오프셋

        val pitchRadians = Math.toRadians(-loc.pitch.toDouble())
        val yawRadians = Math.toRadians(loc.yaw.toDouble() + 90.0)

        val timeStep = 0.1 // 시간 단계
        var t = 0.0 // 시간 변수
        var i = 0
        var lastLoc= loc

        val taskId = scheduler.runTaskTimer(plugin, Runnable {
            // 실행할 작업 (메시지 출력 등)

            val x = loc.x + (launchSpeed * t) * cos(pitchRadians) * cos(yawRadians)
            val y = loc.y + (launchSpeed * t) * sin(pitchRadians) - 0.5 * g * t * t + yOffset
            val z = loc.z + (launchSpeed * t) * cos(pitchRadians) * sin(yawRadians)

            val particleLocation = Location(loc.world, x, y, z)


            // 파티클로 궤도를 보여주기
            if (i >= 1) player.world.spawnParticle(Particle.REDSTONE, particleLocation, 1, 0.0, 0.0, 0.0, 0.0, DustOptions(Color, 1.0F))

            t += timeStep
            i++
            lastLoc = particleLocation
        }, 0L, 1).taskId

        scheduler.runTaskLater(plugin, Runnable {
            Bukkit.getScheduler().cancelTask(taskId)
        }, tick.toLong())

    }
    fun getOrbitalLoc(player: Player, loc: Location, launchSpeed: Int, tick: Int, g: Double): Location {
        val yOffset = 0.0 // 포물선 곡선을 위한 높이 오프셋

        val pitchRadians = Math.toRadians(-loc.pitch.toDouble())
        val yawRadians = Math.toRadians(loc.yaw.toDouble() + 90.0)

        var loc2 = loc

        val timeStep = 0.1 // 시간 단계
        var t = 0.0 // 시간 변수
        for (i in 0 until tick) {
            val x = loc.x + (launchSpeed * t) * cos(pitchRadians) * cos(yawRadians)
            val y = loc.y + (launchSpeed * t) * sin(pitchRadians) - 0.5 * g * t * t + yOffset
            val z = loc.z + (launchSpeed * t) * cos(pitchRadians) * sin(yawRadians)

            val particleLocation = Location(loc.world, x, y, z)
            loc2 = particleLocation
            t += timeStep
        }
        return loc2
    }
}