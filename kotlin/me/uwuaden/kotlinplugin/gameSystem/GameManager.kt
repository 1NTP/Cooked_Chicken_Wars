package me.uwuaden.kotlinplugin.gameSystem

import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.Main.Companion.defaultMMR
import me.uwuaden.kotlinplugin.Main.Companion.droppedItems
import me.uwuaden.kotlinplugin.Main.Companion.groundY
import me.uwuaden.kotlinplugin.Main.Companion.lastDamager
import me.uwuaden.kotlinplugin.Main.Companion.playerLocPing
import me.uwuaden.kotlinplugin.Main.Companion.playerStat
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.Main.Companion.worldDatas
import me.uwuaden.kotlinplugin.effectManager.EffectManager
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.itemData.WorldItemManager
import me.uwuaden.kotlinplugin.itemManager.maps.MapManager
import me.uwuaden.kotlinplugin.rankSystem.RankSystem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerCapacityPoint
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerMaxUse
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.score
import me.uwuaden.kotlinplugin.teamSystem.TeamClass
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.Validate
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Team
import java.util.*
import kotlin.math.*

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

        /*Spawn the particle at the point*/p.spawnParticle(Particle.REDSTONE, p1.x, p1.y, p1.z, 1,
            Particle.DustOptions(Color.fromRGB(r, g, b), 1.0F)
        )

        /* We add the space covered */covered += space
        p1.add(vector)
    }
}
private fun broadcastWorld(world: World, msg: String) {
    world.players.forEach {
        it.sendMessage(Component.text(msg))
    }
}
private fun winPlayers(players: ArrayList<Player>) {
    broadcastWorld(players[0].world, "${ChatColor.RED}   ")
    broadcastWorld(players[0].world, "${ChatColor.RED}Game Ended")
    broadcastWorld(players[0].world, "${ChatColor.RED}    ")
    broadcastWorld(players[0].world, "${ChatColor.RED}    ")
    val playerNames =  ArrayList<String>()
    players.forEach {
        playerNames.add(it.name)
    }
    broadcastWorld(players[0].world, "${ChatColor.GOLD}Winner: ${playerNames.joinToString(", ")}")

    val dataClass = WorldManager.initData(players[0].world)
    players.forEach { p ->
        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            p.sendTitle("${ChatColor.GOLD}${ChatColor.BOLD}THE", " ", 1, 20 * 1, 1)
        }, 0)
        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            p.sendTitle("${ChatColor.WHITE}${ChatColor.BOLD}THE LAST", " ", 1, 20 * 1, 1)
        }, 20)
        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            p.sendTitle("${ChatColor.GOLD}${ChatColor.BOLD}THE LAST STANDER", " ", 1, 20 * 2, 1)
        }, 40)
        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            p.sendTitle("${ChatColor.WHITE}${ChatColor.BOLD}MATCH", " ", 1, 20 * 3, 1)
        }, 60)
        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            p.sendTitle("${ChatColor.GOLD}${ChatColor.BOLD}MATCH WINNER", " ", 1, 20 * 5, 1)
        }, 80)

        p.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}==============================")
        p.sendMessage(" ")
        p.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}WINNER: ${p.name}")
        p.sendMessage(
            "${ChatColor.GOLD}${ChatColor.BOLD}KILL: ${(dataClass.playerKill[p.uniqueId] ?: 0)}"
        )
        p.sendMessage(" ")
        p.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}==============================")
    }
}
private fun winPlayer(p: Player) {
    val dataClass = WorldManager.initData(p.world)
    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        p.sendTitle("${ChatColor.GOLD}${ChatColor.BOLD}THE", " ", 1, 20 * 1, 1)
    }, 0)
    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        p.sendTitle("${ChatColor.WHITE}${ChatColor.BOLD}THE LAST", " ", 1, 20 * 1, 1)
    }, 20)
    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        p.sendTitle("${ChatColor.GOLD}${ChatColor.BOLD}THE LAST STANDER", " ", 1, 20*2, 1)
    }, 40)
    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        p.sendTitle("${ChatColor.WHITE}${ChatColor.BOLD}MATCH", " ", 1, 20*3, 1)
    }, 60)
    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        p.sendTitle("${ChatColor.GOLD}${ChatColor.BOLD}MATCH WINNER", " ", 1, 20 * 5, 1)
    }, 80)

    broadcastWorld(p.world, "${ChatColor.GOLD}${ChatColor.BOLD}==============================")
    broadcastWorld(p.world, " ")
    broadcastWorld(p.world, "${ChatColor.GOLD}${ChatColor.BOLD}WINNER: ${p.name}")
    broadcastWorld(p.world, "${ChatColor.GOLD}${ChatColor.BOLD}KILL: ${(dataClass.playerKill[p.uniqueId] ?: 0)}")
    broadcastWorld(p.world, " ")
    broadcastWorld(p.world, "${ChatColor.GOLD}${ChatColor.BOLD}==============================")

    RankSystem.updateMMR(p, dataClass.playerKill[p.uniqueId]?: 0, dataClass.totalPlayer, 1, dataClass.avgMMR)
    RankSystem.updateRank(p, dataClass.playerKill[p.uniqueId]?: 0, dataClass.totalPlayer, 1, dataClass.avgMMR)
    dataClass.deadPlayer.add(p)
}

private fun msg(gab: Double, borderSize: Double): String {
    return "${ChatColor.GREEN}보더가 ${ChatColor.YELLOW}${gab.roundToInt()}${ChatColor.GREEN}초에 걸쳐서 ${ChatColor.YELLOW}${ChatColor.BOLD}${borderSize.roundToInt()}${ChatColor.GREEN}블럭으로 감소합니다."
}
private fun distance(loc1: Location, loc2: Location): Double {
    val dx = loc2.x - loc1.x
    val dy = loc2.y - loc1.y
    val dz = loc2.z - loc1.z

    return sqrt(dx * dx + dy * dy + dz * dz)
}
private fun getHighestBlockBelow(location: Location): Location {
    var y = location.y.toInt()
    while (y > -64) {
        val loc = location.clone()
        loc.y = y.toDouble()
        if (loc.block.type != Material.AIR) {
            println(loc.block.type)
            println(loc.y)
            loc.y = y +0.5
            return loc
        }
        y -= 1
    }
    return location
}

private fun initDroppedItemLoc(loc: Location, rad: Double) {
    scheduler.runTaskAsynchronously(plugin, Runnable {
        try {
            Main.droppedItems.filter { it.loc.world == loc.world }.sortedBy { distance(loc, it.loc) }.forEach { droppedItem ->
                if (droppedItem.loc.distance(loc) <= rad) {
                    if (!droppedItem.isLocated) {
                        droppedItem.isLocated = true
                        droppedItem.loc = Location(droppedItem.loc.world, droppedItem.loc.x, getHighestBlockBelow(droppedItem.loc).y+0.5, droppedItem.loc.z)
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            ItemManager.createDisplay(droppedItem)
                        }, 1)
                    }
                }
            }
        } catch (e: Exception) {}
    })
}

private fun placeItems(loc: Location, rad: Double) {

}

private fun lobbyTeleportWorlds(world: World) {
    droppedItems.removeIf { it.loc.world == world }

    Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            world.players.forEach { p ->
                p.inventory.clear()
                p.gameMode = GameMode.SURVIVAL
                p.activePotionEffects.clear()
                p.teleport(Main.lobbyLoc)
            }
        }, 20*10)
        WorldManager.deleteWorld(world)
        scheduler.scheduleSyncDelayedTask(plugin, {
           worldDatas.remove(world)

        }, 20*30)
    }, 20*10)
}


private fun getMobSpawnLocation(loc: Location, rad: Int): Location? {
    val x = loc.blockX
    val y = loc.blockY
    val z = loc.blockZ
    val world =loc.world
    val random = Random()
    for (i in 0 until 30) {
        val spawnLoc = Location(world, random.nextInt(x - rad, x + rad) +0.5, random.nextInt(y, y + rad) +0.1, random.nextInt(z - rad, z + rad) + 0.5)

        if (world.isChunkLoaded(spawnLoc.x.roundToInt() shr 4, spawnLoc.z.roundToInt() shr 4) && !WorldManager.isOutsideBorder(spawnLoc)) {
            for (n in 0 until 10) {
                if (spawnLoc.block.type == Material.AIR && spawnLoc.clone().add(0.0, 1.0, 0.0).block.type == Material.AIR && spawnLoc.clone().add(0.0, -1.0, 0.0).block.isSolid) {
                    return spawnLoc
                }
                spawnLoc.y--
            }
        }
    }
    return null
}
private fun spawnZombie(time: Int, loc: Location) {
    val min = time/60
    val random = Random()
    when (min) {
        in 0..4 -> {
            val r = random.nextInt(0, 2)
            if (r == 0) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.setItemInMainHand(ItemStack(Material.IRON_AXE))
                e.customName = "${ChatColor.GREEN}Axe Zombie lv1"
            } else if (r == 1) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.boots = ItemStack(Material.IRON_BOOTS)
                e.equipment.leggings = ItemStack(Material.IRON_LEGGINGS)
                e.equipment.chestplate = ItemStack(Material.IRON_CHESTPLATE)
                e.equipment.helmet = ItemStack(Material.IRON_HELMET)
                e.customName = "${ChatColor.GRAY}Tank Zombie lv1"
            }
        } in 5..9 -> {
            val r = random.nextInt(0, 3)
            if (r == 0) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.setItemInMainHand(ItemStack(Material.DIAMOND_AXE))
                e.equipment.leggings = ItemStack(Material.DIAMOND_LEGGINGS)
                e.equipment.chestplate = ItemStack(Material.DIAMOND_CHESTPLATE)
                e.customName = "${ChatColor.AQUA}${ChatColor.BOLD}Axe Zombie lv2"
            } else if (r == 1) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.boots = ItemStack(Material.DIAMOND_BOOTS)
                e.equipment.leggings = ItemStack(Material.DIAMOND_LEGGINGS)
                e.equipment.chestplate = ItemStack(Material.DIAMOND_CHESTPLATE)
                e.equipment.helmet = ItemStack(Material.DIAMOND_HELMET)
                e.customName = "${ChatColor.AQUA}${ChatColor.BOLD}Tank Zombie lv2"
            } else if (r == 2) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.boots = ItemStack(Material.DIAMOND_BOOTS)

                val item = ItemStack(Material.WOODEN_SWORD)
                item.addEnchantment(Enchantment.KNOCKBACK, 2)
                e.equipment.setItemInMainHand(item)

                e.customName = "${ChatColor.DARK_GRAY}${ChatColor.BOLD}Silence Zombie lv1"
                e.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20*1000000, 0, false, false))
            }
        } in 10..Int.MAX_VALUE -> {
            val r = random.nextInt(0, 6)
            if (r in 0..1) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.setItemInMainHand(ItemStack(Material.NETHERITE_AXE))
                e.equipment.leggings = ItemStack(Material.NETHERITE_LEGGINGS)
                e.equipment.chestplate = ItemStack(Material.NETHERITE_CHESTPLATE)
                e.customName = "${ChatColor.GRAY}${ChatColor.BOLD}Axe Zombie lv3"
            } else if (r in 2..3) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.boots = ItemStack(Material.NETHERITE_BOOTS)
                e.equipment.leggings = ItemStack(Material.NETHERITE_LEGGINGS)
                e.equipment.chestplate = ItemStack(Material.NETHERITE_CHESTPLATE)
                e.equipment.helmet = ItemStack(Material.NETHERITE_HELMET)
                e.customName = "${ChatColor.GRAY}${ChatColor.BOLD}Tank Zombie lv3"
            } else if (r == 4) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.chestplate = ItemStack(Material.DIAMOND_CHESTPLATE)
                e.customName = "${ChatColor.RED}${ChatColor.BOLD}TITAN ZOMBIE"
                e.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 6400.0
                e.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = 20.0
                e.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = 120.0
                e.health = 120.0
            } else if (r == 5) {
                val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
                e.scoreboardTags.add("Spawned-Zombie")
                e.equipment.boots = ItemStack(Material.NETHERITE_BOOTS)

                val item = ItemStack(Material.STONE_SWORD)
                item.addEnchantment(Enchantment.KNOCKBACK, 2)
                e.equipment.setItemInMainHand(item)

                e.customName = "${ChatColor.DARK_GRAY}${ChatColor.BOLD}Silence Zombie lv2"
                e.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20*1000000, 0, false, false))
            }
        } else -> {
            val e = loc.world.spawnEntity(loc, EntityType.ZOMBIE, false) as Zombie
            e.scoreboardTags.add("Spawned-Zombie")
            e.customName = "${ChatColor.GREEN}Normal Zombie"
        }
    }

}
private fun initPlayer(player: Player) {
    player.gameMode = GameMode.SURVIVAL
    player.inventory.clear()
    player.activePotionEffects.clear()
    player.health = 20.0
    player.absorptionAmount = 0.0
    player.foodLevel = 20
    player.saturation = 0.0F
    playerCapacityPoint[player.uniqueId] = 0
    playerMaxUse[player.uniqueId] = 0
    player.inventory.setItem(8, MapManager.createMapView(player))
    player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
    player.inventory.addItem(ItemStack(Material.WOODEN_PICKAXE))
    player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
    if (!player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
        player.sendMessage("${ChatColor.GOLD}건물 안에 있는 아이템을 파밍하여 살아남으세요!")
        player.sendMessage("${ChatColor.GRAY}팁: ${EffectManager.randomTip()}")
    }
    player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*30, 4, false, false))
}

object GameManager {
    fun zombieSch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.forEach { world->
                world.livingEntities.forEach {
                    if (it.scoreboardTags.contains("Spawned-Zombie")) {
                        val entity = it as Zombie
                        if (entity.target == null) {
                            val players = entity.location.getNearbyPlayers(320.0).filter { p -> p.gameMode != GameMode.SPECTATOR }.sortedBy { p -> entity.location.distance(p.location) }
                            if (players.isNotEmpty()) entity.target = players[0]
                        }
                        if(WorldManager.isOutsideBorder(it.location)) {

                            entity.damage(2.0)
                        }
                    }

                    if (it.name == "${ChatColor.RED}${ChatColor.BOLD}TITAN ZOMBIE") {

                        var sound = false
                        for (x in -2..2) {
                            for (y in 0..320) {
                                for (z in -2..2) {
                                    val loc = it.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                                    if (loc.world.isChunkLoaded(loc.x.roundToInt() shr 4, loc.z.roundToInt() shr 4) && loc.y.roundToInt() > groundY) {
                                        if (loc.block.type != Material.AIR) {
                                            //sound = true
                                            loc.world.spawnParticle(Particle.BLOCK_CRACK, loc, 5, 0.5, 0.5, 0.5, 0.0, loc.block.blockData)
                                            loc.block.type = Material.AIR
                                        }
                                    }
                                }
                            }
                        }
                        if (sound) {
                            EffectManager.playSurroundSound(it.location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.0F)
                        }
                    } else if (it.name.contains("${ChatColor.DARK_GRAY}${ChatColor.BOLD}Silence")) {

                        val entity = it as LivingEntity
                        if (entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            val pls = it.location.getNearbyPlayers(24.0).filter { p -> p.gameMode != GameMode.SPECTATOR }
                            if (pls.isNotEmpty()) {
                                entity.removePotionEffect(PotionEffectType.INVISIBILITY)
                                it.teleport(pls.random().location)
                                it.world.playSound(it.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 2.0f)
                                it.world.spawnParticle(Particle.REVERSE_PORTAL, it.location.clone().add(0.0, 1.0, 0.0), 20, 1.0, 1.0, 1.0, 0.0)
                                it.world.spawnParticle(Particle.SMOKE_NORMAL, it.location.clone().add(0.0, 1.0, 0.0), 5, 1.0, 1.0, 1.0, 0.0)
                            }
                        }

                    }
                }
            }
        }, 0, 5)
    }


    fun joinPlayers(fromWorld: World, toWorld: World, mode: String) {
        val random = Random()
        val randomSize = 200.0
        val borderRadius = 500.0
        val borderCenter = Location(
            toWorld,
            random.nextDouble(-1 * randomSize, randomSize),
            0.0,
            random.nextDouble(-1 * randomSize, randomSize)
        )

        toWorld.difficulty = Difficulty.HARD
        toWorld.time = 0
        toWorld.setGameRule(GameRule.DO_TILE_DROPS, false)
        toWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

        WorldItemManager.createItemData(
            toWorld,
            borderCenter.clone().add(-1 * borderRadius, 0.0, -1 * borderRadius),
            borderCenter.clone().add(borderRadius, 0.0, borderRadius),
            4000
        )

        fromWorld.players.forEach { player ->
            initPlayer(player)

        }
        val dataClass = WorldManager.initData(toWorld)


        dataClass.totalPlayer = fromWorld.players.size

        var sum = 0

        fromWorld.players.forEach { p ->
            val classData = playerStat[p.uniqueId]
            sum += classData?.playerMMR?: defaultMMR
        }

        dataClass.avgMMR = sum/fromWorld.players.size

        BorderManager.initBoarder(borderCenter, borderRadius*2)

        dataClass.worldMode[toWorld] = mode
        dataClass.worldTimer[toWorld] = System.currentTimeMillis()

        val players = ArrayList<Player>()
        fromWorld.players.forEach {
            players.add(it)
        }

        players.shuffle()
        val playerHash = HashMap<Player, Int>()
        players.forEach {
            val data = RankSystem.initData(it.uniqueId)
            playerHash[it] =  (data.playerMMR/10)*10 + random.nextInt(-100, 100)
        }

        players.sortBy { playerHash[it] }

        scheduler.scheduleSyncDelayedTask(plugin, {
            players.forEach {
                val data = RankSystem.initData(it.uniqueId)
                if (dataClass.avgMMR > data.playerMMR + 50) {
                    it.sendMessage("${ChatColor.GREEN}게임 MMR이 높아 가젯이 지급되었습니다!")
                    it.inventory.addItem(
                        ItemManager.createNamedItem(
                            Material.BOOK,
                            1,
                            "${ChatColor.GREEN}${ChatColor.BOLD}가젯 선택",
                            listOf("${ChatColor.GRAY}Gadget")
                        )
                    )
                }
            }
        }, 20*10)

        if (mode == "TwoTeam") {
            val first = ArrayList<Player>()
            val second = ArrayList<Player>()

            val r = random.nextInt(0, 2)

            for (i in 0 until players.size) {
                if (i%2 == r) {
                    first.add(players[i])
                } else {
                    second.add(players[i])
                }
            }

            dataClass.teams.add(TeamClass(toWorld, first.toMutableSet()))
            dataClass.teams.add(TeamClass(toWorld, second.toMutableSet()))

            var i = 0
            spawnLocList(borderCenter.clone().add(-1*borderRadius, 0.0, -1*borderRadius), borderCenter.clone().add(borderRadius, 0.0, borderRadius), 2, 30).forEach {
                if (i == 0) {
                    var t = 0
                    first.forEach { p->
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            p.teleport(it)
                            initPlayer(p)
                        }, t*5L)
                        t++
                    }
                } else {
                    var t = 0
                    second.forEach { p->
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            p.teleport(it)
                            initPlayer(p)
                        }, t*5L)
                        t++
                    }
                }
                i++
            }

        } else if (mode.contains("Teams:")) {
            try {
                val playerPerTeam = mode.split(":")[1].trim().toInt()
                val teamCount = ceil(players.size.toDouble()/playerPerTeam.toDouble()).roundToInt()
                val teams: HashMap<Int, TeamClass> = HashMap()
                val playerStack = Stack<Player>()
                players.forEach {
                    playerStack.add(it)
                }
                for (t in 0 until playerPerTeam) {
                    for (i in 0 until teamCount) {
                        if (teams[i] == null) teams[i] = TeamClass(toWorld, mutableSetOf())
                        if (playerStack.size > 0) teams[i]!!.players.add(playerStack.pop())
                    }
                }
                teams.forEach {
                    dataClass.teams.add(it.value)
                }

                val spawnLocs = spawnLocList(borderCenter.clone().add(-1*borderRadius, 0.0, -1*borderRadius), borderCenter.clone().add(borderRadius, 0.0, borderRadius), teamCount, 30)
                scheduler.runTaskAsynchronously(plugin, Runnable {
                    teams.forEach {
                        it.value.players.forEach { pl ->
                            scheduler.scheduleSyncDelayedTask(plugin, {
                                pl.teleport(spawnLocs[it.key])
                                initPlayer(pl)
                            }, 0)
                            Thread.sleep(100)
                        }
                    }
                })
            } catch (e: Exception) {
                println(e)
            }
        } else {
            var i = 0
            spawnLocList(borderCenter.clone().add(-1*borderRadius, 0.0, -1*borderRadius), borderCenter.clone().add(borderRadius, 0.0, borderRadius), fromWorld.players.size, 30).forEach {
                players[i].teleport(it)
                initPlayer(players[i])
                i++
            }
        }

        if (mode == "SoloSurvival") {
            toWorld.time = 10000
            toWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        }


        val worldDeleteSec: Long = 10

        var borderSize = borderRadius*2

        val min = 1.0 //min분 마다 자기장 축소


        var delay: Long = 0
        scheduler.scheduleSyncDelayedTask(plugin, {
            for (n in 1..12) {
                val changedBorderSize = borderSize*((0.8).pow(n))
                val gab = borderSize*((0.8).pow(n-1)) - borderSize*((0.8).pow(n))

                if (n != 1) {
                    delay += ((20 * gab.roundToInt())/2 + 20*min*60).toLong()
                }
                scheduler.scheduleSyncDelayedTask(plugin, {
                    WorldManager.broadcastWorld(toWorld, msg(gab, changedBorderSize))
                    BorderManager.changeInSec(toWorld, changedBorderSize, gab.roundToInt() / 2)

                }, delay)
            }
        }, 20*60*5)

        scheduler.scheduleSyncDelayedTask(plugin, {
            WorldManager.deleteWorld(fromWorld)
        }, 20*worldDeleteSec)
    }

    fun spawnLocList(loc1: Location, loc2: Location, count: Int, dist: Int): ArrayList<Location> {
        val xRange: ClosedFloatingPointRange<Double>
        val zRange: ClosedFloatingPointRange<Double>
        if (loc1.x > loc2.x) xRange = loc2.x..loc1.x
        else xRange = loc1.x..loc2.x
        if (loc1.z > loc2.z) zRange = loc2.z..loc1.z
        else zRange = loc1.z..loc2.z


        val world = loc1.world
        val random = Random()
        val locs = ArrayList<Location>()
        for (i in 0 until count) {
            var loop = true
            var minDis = dist*10+9
            while (loop) {

                val loc = Location(
                    world,
                    random.nextInt(xRange.start.toInt(), xRange.endInclusive.toInt()) + 0.5,
                    50.0,
                    random.nextInt(zRange.start.toInt(), zRange.endInclusive.toInt()) + 0.5
                )
                var isInRange = true

                locs.forEach { l ->
                    if (l.distance(loc) < minDis/10) {
                        isInRange = false
                        return@forEach
                    }
                }

                if (isInRange) {
                    locs.add(loc)
                    loop = false
                }
                if (!isInRange) {
                    minDis -= 1
                }

            }
        }

        locs.forEach {
            it.y = it.world.getHighestBlockAt(it).y + 1.2
        }
        return locs
    }

    fun gameSch() {
        Main.scheduler.scheduleSyncRepeatingTask(Main.plugin, {
            plugin.server.onlinePlayers.forEach { p ->
                if (p.gameMode == GameMode.SPECTATOR) {
                    if (p.spectatorTarget is Player) {
                        val target = p.spectatorTarget as Player
                        val dataClass = WorldManager.initData(p.world)
                        var hpStr = "${floor(target.health + target.absorptionAmount)}"
                        if (floor(target.health + target.absorptionAmount) > 20.0) {
                            hpStr = "§6$hpStr"
                        }
                        p.sendActionBar("§ePlayer: ${target.name}    §cHP: $hpStr    §9Kill: ${dataClass.playerKill[target.uniqueId] ?: 0}")
                    }
                }
            }

            playerLocPing.forEach { (uuid, loc) ->
                val p = plugin.server.getPlayer(uuid)
                if (p != null && loc.world == p.world) {
                    drawLine(p.location, loc, 0.2, 158, 253, 56, p)
                }
            }
            Main.plugin.server.worlds.forEach { world ->
                val dataClass = WorldManager.initData(world)

                world.players.forEach { player ->

                    if (!player.world.name.contains("Field-")) {
                        lastDamager.remove(player)
                    }
                    var t = score.getTeam("nameTagHide")
                    if (t == null) {
                        t = score.registerNewTeam("nameTagHide")
                        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
                    }
                    if(!t.hasPlayer(player)) {
                        t.addPlayer(player)
                    }

                    initDroppedItemLoc(player.location, 80.0)
                    WorldItemManager.createItems(player.location, 80.0)
                }

                if (!dataClass.GameEndedWorld.contains(world) && (System.currentTimeMillis() - (dataClass.worldTimer[world] ?: System.currentTimeMillis())) > 1000 * 5) {
                    if (dataClass.worldMode[world] == "Solo") {
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        if (players.size == 1) {
                            dataClass.GameEndedWorld.add(world)
                            winPlayer(players[0])
                            lobbyTeleportWorlds(world)

                        }
                    } else if (dataClass.worldMode[world] == "TwoTeam") {
                        var team: TeamClass? = null
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        val teams = mutableSetOf<TeamClass>()
                        players.forEach {
                            val t = TeamManager.getTeam(world, it)
                            if (t != null) {
                                teams.add(t)
                            }
                        }
                        teams.forEach {
                            team = it
                        }
                        if (teams.size == 1) {
                            dataClass.GameEndedWorld.add(world)
                            winPlayers(ArrayList(team!!.players))
                            lobbyTeleportWorlds(world)
                        }

                    } else if (dataClass.worldMode[world] == "SoloSurvival") {
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        val specs = world.players.filter { it.gameMode == GameMode.SPECTATOR }
                        if (specs.isNotEmpty() && players.isEmpty()) {
                            dataClass.GameEndedWorld.add(world)
                            specs.forEach {
                                it.sendMessage("${ChatColor.RED}        GAME OVER")
                                it.sendMessage("${ChatColor.RED} ")
                                it.sendMessage("${ChatColor.RED}    ${(System.currentTimeMillis() -(dataClass.worldTimer[world] ?: System.currentTimeMillis()))/1000}초 생존함.")
                                it.sendMessage("${ChatColor.RED}  ")
                            }
                            lobbyTeleportWorlds(world)
                        }
                    } else if (dataClass.worldMode[world]?.contains("Teams:") == true) {
                        var team: TeamClass? = null
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        val teams = mutableSetOf<TeamClass>()
                        players.forEach {
                            val t = TeamManager.getTeam(world, it)
                            if (t != null) {
                                teams.add(t)
                            }
                        }
                        teams.forEach {
                            team = it
                        }
                        if (teams.size == 1) {
                            dataClass.GameEndedWorld.add(world)
                            winPlayers(ArrayList(team!!.players))
                            lobbyTeleportWorlds(world)
                        }
                    }

                }
            }
        }, 0, 5)
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.forEach { world ->
                val dataClass = WorldManager.initData(world)
                if (dataClass.worldMode[world] == "SoloSurvival") {
                    val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                    val monsters = world.entities.filter { it.scoreboardTags.contains("Spawned-Zombie") }
                    val time = (System.currentTimeMillis() -(dataClass.worldTimer[world] ?: System.currentTimeMillis()))/1000
                    players.shuffled().forEach {
                        if (monsters.size < 30) {
                            val spawnLoc = getMobSpawnLocation(it.location, 100)
                            if (spawnLoc != null) {
                                spawnZombie(time.toInt(), spawnLoc)
                            }
                        }
                    }
                }
            }
        }, 0, 20)
    }
}