package me.uwuaden.kotlinplugin.gameSystem

import com.destroystokyo.paper.Title
import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.Main.Companion.defaultMMR
import me.uwuaden.kotlinplugin.Main.Companion.econ
import me.uwuaden.kotlinplugin.Main.Companion.groundY
import me.uwuaden.kotlinplugin.Main.Companion.lastDamager
import me.uwuaden.kotlinplugin.Main.Companion.lobbyLoc
import me.uwuaden.kotlinplugin.Main.Companion.playerLocPing
import me.uwuaden.kotlinplugin.Main.Companion.playerStat
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.Main.Companion.scoreboardManager
import me.uwuaden.kotlinplugin.Main.Companion.worldDatas
import me.uwuaden.kotlinplugin.Recipes
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.assets.ItemManipulator.addEnchant
import me.uwuaden.kotlinplugin.assets.ItemManipulator.setCount
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemManager
import me.uwuaden.kotlinplugin.itemManager.itemData.WorldItemManager
import me.uwuaden.kotlinplugin.npc.AiData
import me.uwuaden.kotlinplugin.npc.NPCBehavior
import me.uwuaden.kotlinplugin.npc.NPCBehavior.idle
import me.uwuaden.kotlinplugin.npc.NPCEvent.Companion.aiWorld
import me.uwuaden.kotlinplugin.rankSystem.RankSystem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerCapacityPoint
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerMaxUse
import me.uwuaden.kotlinplugin.skillSystem.SkillManager
import me.uwuaden.kotlinplugin.teamSystem.TeamClass
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import me.uwuaden.kotlinplugin.zombie.ZombieManager
import net.citizensnpcs.api.CitizensAPI
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.Validate
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Team
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

private fun getRandomIronArmor(piece: Int): List<ItemStack> {
    val items = mutableListOf(
        ItemStack(Material.IRON_HELMET),
        ItemStack(Material.IRON_CHESTPLATE),
        ItemStack(Material.IRON_LEGGINGS),
        ItemStack(Material.IRON_BOOTS)
    )
    val listToUse = mutableListOf<ItemStack>()

    for (i in 0 until piece.coerceAtMost(4).coerceAtLeast(0)) {
        val itemToUse = items.random()
        listToUse.add(itemToUse)
        items.remove(itemToUse)
    }
    return listToUse
}

private fun getRandomDiamondArmor(piece: Int): List<ItemStack> {
    val items = mutableListOf(
        ItemStack(Material.DIAMOND_HELMET),
        ItemStack(Material.DIAMOND_CHESTPLATE),
        ItemStack(Material.DIAMOND_LEGGINGS),
        ItemStack(Material.DIAMOND_BOOTS)
    )
    val listToUse = mutableListOf<ItemStack>()

    for (i in 0 until piece.coerceAtMost(4).coerceAtLeast(0)) {
        val itemToUse = items.random()
        listToUse.add(itemToUse)
        items.remove(itemToUse)
    }

    if (!listToUse.contains(ItemStack(Material.DIAMOND_HELMET))) listToUse.add(ItemStack(Material.IRON_HELMET))
    if (!listToUse.contains(ItemStack(Material.DIAMOND_CHESTPLATE))) listToUse.add(ItemStack(Material.IRON_CHESTPLATE))
    if (!listToUse.contains(ItemStack(Material.DIAMOND_LEGGINGS))) listToUse.add(ItemStack(Material.IRON_LEGGINGS))
    if (!listToUse.contains(ItemStack(Material.DIAMOND_BOOTS))) listToUse.add(ItemStack(Material.IRON_BOOTS))

    return listToUse
}

private fun secondsToHMS(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    val hoursPart = if (hours > 0) "${hours}시간 " else ""
    val minutesPart = if (minutes > 0) "${minutes}분 " else ""
    val secondsPart = if (seconds > 0 || (hours == 0L && minutes == 0L)) "${remainingSeconds}초" else ""

    return "$hoursPart$minutesPart$secondsPart".trim()
}
private fun spawnItemDisplay(loc: Location, type: Material, scale: Double = 1.0) {
    val world = loc.world
    val itemDisplay1 = world.spawnEntity(loc.clone(), EntityType.ITEM_DISPLAY) as ItemDisplay
    itemDisplay1.itemStack = ItemStack(type)
    val transform = itemDisplay1.transformation
    transform.scale.set(scale)
    itemDisplay1.transformation = transform
    itemDisplay1.scoreboardTags.add("core_display")
}
private fun createCoreDisplay(loc: Location) {
    loc.yaw = 0.0f
    loc.pitch = 0.0f
    loc.add(0.0, 0.5, 0.0)
    spawnItemDisplay(loc.clone().add(0.0, 1.0, 0.0), Material.BEACON, 1.5)

    spawnItemDisplay(loc.clone().add(1.0, 0.0, 1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
    spawnItemDisplay(loc.clone().add(1.0, 0.0, -1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
    spawnItemDisplay(loc.clone().add(-1.0, 0.0, 1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
    spawnItemDisplay(loc.clone().add(-1.0, 0.0, -1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)

    spawnItemDisplay(loc.clone().add(1.0, 2.0, 1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
    spawnItemDisplay(loc.clone().add(1.0, 2.0, -1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
    spawnItemDisplay(loc.clone().add(-1.0, 2.0, 1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
    spawnItemDisplay(loc.clone().add(-1.0, 2.0, -1.0), Material.LIGHT_GRAY_CONCRETE, 1.2)
}
private fun shufflePlayers(players: ArrayList<Player>): ArrayList<Player> {
    val random = Random()
    players.shuffle()
    val playerHash = HashMap<Player, Int>()
    players.forEach {
        val data = RankSystem.initData(it.uniqueId)
        playerHash[it] =  (data.playerMMR/10)*10 + random.nextInt(-100, 100)
    }
    players.sortBy { playerHash[it] }
    return players
}
private fun probabilityTrue(n: Double): Boolean {
    require(n in 0.0..100.0) { "확률은 0에서 100 사이의 값이어야 합니다." }

    val randomValue = kotlin.random.Random.nextDouble(0.0, 100.0)
    return randomValue < n
}
private fun earthGrenade(loc: Location) {
    val originLoc = loc.clone()
    val particleLoc = loc.clone().add(0.0, 0.25, 0.0)
    val blocks = mutableSetOf<Block>()

    val r = 8
    loc.y += 1.0
    while (loc.y < 320) {

        EffectManager.getBlocksInCircle(loc, r).forEach {
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
                EffectManager.drawParticleCircle(particleLoc, 8.0, Color.fromRGB(100, 65, 23))
                EffectManager.playSurroundSound(originLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f)
            }, 0)
            Thread.sleep(1500)
        }
    })
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
        //RankSystem.updateMMR(p, (dataClass.playerKill[p.uniqueId] ?: 0), dataClass.totalPlayer, 1, dataClass.avgMMR)
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
    if ((dataClass.worldMode == "Solo" || dataClass.worldMode == "Quick") && dataClass.isRanked) {
        RankSystem.updateMMR(p, dataClass.playerKill[p.uniqueId] ?: 0, dataClass.totalPlayer, 1, dataClass.avgMMR)
        RankSystem.updateRank(p, dataClass.playerKill[p.uniqueId] ?: 0, dataClass.totalPlayer, 1, dataClass.avgMMR)
    }
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

private fun initDroppedItemLoc(loc: Location, rad: Double) { //Dep
    scheduler.runTaskAsynchronously(plugin, Runnable {
        try {
            val data = WorldManager.initData(loc.world)
            data.droppedItems.filter { it.loc.world == loc.world }.sortedBy { distance(loc, it.loc) }.forEach { droppedItem ->
                if (droppedItem.loc.distance(loc) <= rad) {
                    if (!droppedItem.isLocated) {
                        droppedItem.isLocated = true
                        droppedItem.loc = Location(droppedItem.loc.world, droppedItem.loc.x, getHighestBlockBelow(droppedItem.loc).y+0.5, droppedItem.loc.z)
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            ItemManager.createDisplay(droppedItem)
                        }, 0)
                    }
                }
            }
        } catch (e: Exception) {}
    })
}


private fun placeItems(loc: Location, rad: Double) {

}




private fun getMobSpawnLocation(loc: Location, rad: Int): Location? {
    val x = loc.blockX
    val y = loc.blockY
    val z = loc.blockZ
    val world =loc.world
    val random = Random()
    for (i in 0 until 50) {
        val spawnLoc = Location(world, random.nextInt(x - rad, x + rad) +0.5, random.nextInt(y, y + rad) +0.1, random.nextInt(z - rad, z + rad) + 0.5)

        if (world.isChunkLoaded(spawnLoc.x.roundToInt() shr 4, spawnLoc.z.roundToInt() shr 4) && !WorldManager.isOutsideBorder(spawnLoc)) {
            for (n in 0 until 10) {
                if (spawnLoc.block.type == Material.AIR && spawnLoc.clone().add(0.0, 1.0, 0.0).block.type == Material.AIR && spawnLoc.clone().add(0.0, -1.0, 0.0).block.isSolid) {
                    if (loc.distance(spawnLoc) > 20 && spawnLoc.y <= 8 && spawnLoc.y >= -1) {
                        return spawnLoc
                    }
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
private fun initPlayer(player: Player, sendTeam: Boolean = false, enhancedItem: Boolean = false) {
    player.gameMode = GameMode.SURVIVAL
    player.inventory.clear()
    player.activePotionEffects.clear()
    player.health = 20.0
    player.absorptionAmount = 0.0
    player.foodLevel = 20
    player.saturation = 0.0F
    playerCapacityPoint[player.uniqueId] = 0
    playerMaxUse[player.uniqueId] = 0
    lastDamager.remove(player)
    //player.inventory.setItem(8, MapManager.createMapView(player))
    if (enhancedItem) {
        player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.IRON_HELMET))
        player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.IRON_CHESTPLATE))
        player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.IRON_LEGGINGS))
        player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.IRON_BOOTS))
        player.inventory.addItem(ItemStack(Material.IRON_SWORD))
        player.inventory.addItem(ItemStack(Material.COOKED_BEEF, 64))
    } else {
        player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
        player.inventory.addItem(ItemStack(Material.WOODEN_PICKAXE))
        player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
    }
    if (!player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
        player.sendMessage("${ChatColor.GOLD}건물 안에 있는 아이템을 파밍하여 살아남으세요!")
        player.sendMessage("${ChatColor.GRAY}팁: ${EffectManager.randomTip()}")
        if (sendTeam) {
            val team = TeamManager.getTeam(player.world, player)
            if (team != null) {
                val names = mutableListOf<String>()
                team.players.forEach { player ->
                    names.add(player.name)
                }
                player.sendMessage("${ChatColor.GOLD}팀원: ${names.joinToString(", ")}")
            }
        }
    }
    player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*30, 4, false, false))

}
private fun initItem(loc: Location) {
    val world = loc.world
    val chunkX = loc.x.roundToInt() shr 4
    val chunkZ = loc.z.roundToInt() shr 4
    val RANGE = 3
    scheduler.runTaskAsynchronously(plugin, Runnable {
        for (x in (chunkX - RANGE)..(chunkX + RANGE)) {
            for (z in (chunkZ - RANGE)..(chunkZ + RANGE)) {
                val dataClass = WorldManager.initData(world)
                val itemCount = dataClass.worldDroppedItemData.ItemCount[Pair(x, z)] ?: 0
                if (itemCount != 0) {
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        WorldItemManager.createItems(world, x, z) //데이터 생성
                        //WorldItemManager.createResource(world, x, z) //데이터 생성
                        //GameManager.initDroppedItemLoc(world, x, z) //위치 설정
                    }, 0)
                }
            }
        }
    })
}

object GameManager {
    fun lobbyTeleportWorlds(world: World) {
        val data = WorldManager.initData(world)

        data.droppedItems.removeIf { it.loc.world == world }

        Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
            Main.scheduler.scheduleSyncDelayedTask(Main.plugin, {
                world.players.forEach { p ->
                    p.inventory.clear()
                    p.gameMode = GameMode.SURVIVAL
                    p.activePotionEffects.clear()
                    p.teleport(Main.lobbyLoc)
                }
            }, 20*10)
            scheduler.scheduleSyncDelayedTask(plugin, {
                WorldManager.deleteWorld(world)
                worldDatas.remove(world)
            }, 20*15)
        }, 20*10)
    }
    fun initDroppedItemLoc(world: World, x: Int, z: Int) {

        scheduler.runTaskAsynchronously(plugin, Runnable {
//        try {
            val data = WorldManager.initData(world)
            var loopTime = 0
            scheduler.scheduleSyncDelayedTask(plugin, {
                val droppedItems = data.droppedItems.filter { it.loc.x.roundToInt() shr 4 == x }.filter { it.loc.z.roundToInt() shr 4 == z }

                scheduler.runTaskAsynchronously(plugin, Runnable {
                    droppedItems.forEach { droppedItem ->
                        loopTime++
                        if (loopTime % 50 == 0) Thread.sleep(50)
                        if (!droppedItem.isLocated) {
                            droppedItem.isLocated = true
                            scheduler.scheduleSyncDelayedTask(plugin, {
                                droppedItem.loc = Location(droppedItem.loc.world, floor(droppedItem.loc.x) + 0.5, getHighestBlockBelow(droppedItem.loc).y + 0.5, floor(droppedItem.loc.z) + 0.5)
                                ItemManager.createDisplay(droppedItem)
                            }, 0)
                        }
                    }
                })
            }, 0)
//        } catch (e: Exception) {
//            plugin.server.logger.log(Level.WARNING, "Dropped Item Locating Error")
//            println(e.localizedMessage)
//        }
        })
    }
    fun chunkSch() {
        scheduler.scheduleSyncDelayedTask(plugin, {
            scheduler.runTaskAsynchronously(plugin, Runnable {
                Thread.sleep(10000)
                while (plugin.isEnabled) {
                    plugin.server.worlds.forEach { world ->
                        if (world.name.contains("Field-")) {
                            val locs = mutableListOf<Location>()
                            world.players.filter { it.gameMode == GameMode.SURVIVAL }.forEach {
                                locs.add(it.location)
                            }
                            CitizensAPI.getNPCRegistry().toList().forEach {
                                if (it.entity != null && it.entity.world == world) locs.add(it.entity.location)
                            }
                            locs.forEach { loc ->
                                initItem(loc)
                                Thread.sleep(1000/2)
                            }
                        }
                    }
                    Thread.sleep(1000/10)
                }
            })
        }, 20*10)
    }

//    fun zombieSch() {
//        scheduler.scheduleSyncRepeatingTask(plugin, {
//            plugin.server.worlds.forEach { world->
//                world.livingEntities.forEach {
//                    if (it.scoreboardTags.contains("Spawned-Zombie")) {
//                        val entity = it as Zombie
//                        if (null == entity.target) {
//                            val players = entity.location.getNearbyPlayers(320.0).filter { p -> p.gameMode != GameMode.SPECTATOR }.sortedBy { p -> entity.location.distance(p.location) }
//                            if (players.isNotEmpty()) entity.target = players[0]
//                        }
//                        if(WorldManager.isOutsideBorder(it.location)) {
//
//                            entity.damage(2.0)
//                        }
//                    }
//
//                    if (it.name == "${ChatColor.RED}${ChatColor.BOLD}TITAN ZOMBIE") {
//
//                        var sound = false
//                        for (x in -2..2) {
//                            for (y in 0..320) {
//                                for (z in -2..2) {
//                                    val loc = it.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
//                                    if (loc.world.isChunkLoaded(loc.x.roundToInt() shr 4, loc.z.roundToInt() shr 4) && loc.y.roundToInt() > groundY) {
//                                        if (loc.block.type != Material.AIR) {
//                                            //sound = true
//                                            loc.world.spawnParticle(Particle.BLOCK_CRACK, loc, 5, 0.5, 0.5, 0.5, 0.0, loc.block.blockData)
//                                            loc.block.type = Material.AIR
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                        if (sound) {
//                            EffectManager.playSurroundSound(it.location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.0F)
//                        }
//                    } else if (it.name.contains("${ChatColor.DARK_GRAY}${ChatColor.BOLD}Silence")) {
//
//                        val entity = it as LivingEntity
//                        if (entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
//                            val pls = it.location.getNearbyPlayers(24.0).filter { p -> p.gameMode != GameMode.SPECTATOR }
//                            if (pls.isNotEmpty()) {
//                                entity.removePotionEffect(PotionEffectType.INVISIBILITY)
//                                it.teleport(pls.random().location)
//                                it.world.playSound(it.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 2.0f)
//                                it.world.spawnParticle(Particle.REVERSE_PORTAL, it.location.clone().add(0.0, 1.0, 0.0), 20, 1.0, 1.0, 1.0, 0.0)
//                                it.world.spawnParticle(Particle.SMOKE_NORMAL, it.location.clone().add(0.0, 1.0, 0.0), 5, 1.0, 1.0, 1.0, 0.0)
//                            }
//                        }
//
//                    }
//                }
//            }
//        }, 0, 5)
//    }


    fun joinPlayers(fromWorld: World, toWorld: World, mode: String) {
        val random = Random()
        val dataClass = WorldManager.initData(toWorld)
        var randomSize = 200.0
        var borderRadius = 500.0
        var itemCount = 8000
        var resourceCount = 800

        if (dataClass.worldFolderName == "test") {
            randomSize = 200.0
            borderRadius = 600.0
            itemCount = 9000
            resourceCount = 800
        }

        if (dataClass.worldFolderName == "Sinchon") {
            randomSize = 50.0
            borderRadius = 250.0
            itemCount = 2000
            resourceCount = 200
        }

        val borderCenter = Location(
            toWorld,
            random.nextDouble(-1 * randomSize, randomSize),
            0.0,
            random.nextDouble(-1 * randomSize, randomSize)
        )
        fromWorld.players.forEach { p ->
            dataClass.addPlayerRecipe(p.uniqueId, Recipes(ItemStack(Material.STONE)))
        }

        toWorld.difficulty = Difficulty.HARD
        toWorld.time = 0
        toWorld.setGameRule(GameRule.DO_TILE_DROPS, false)
        toWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

        if (mode == "Quick") {
            randomSize/=2.0
            borderRadius/=2.0
            itemCount/=4
        }


        WorldItemManager.createItemData(
            toWorld,
            borderCenter.clone().add(-borderRadius, 0.0, -borderRadius),
            borderCenter.clone().add(borderRadius, 0.0, borderRadius),
            itemCount
        )
        WorldItemManager.createResourceData(toWorld,
            borderCenter.clone().add(-borderRadius, 0.0, -borderRadius),
            borderCenter.clone().add(borderRadius, 0.0, borderRadius),
            resourceCount
            )

//        val thread = thread(false) {
//            plugin.logger.log(Level.WARNING, "Item Creating Started")
//            scheduler.runTaskAsynchronously(plugin, Runnable {
//                var r = 0
//                dataClass.worldDroppedItemData.ItemCount.filter { it.value != 0 }.keys.forEach {
//                    r++
//                    scheduler.scheduleSyncDelayedTask(plugin, {
//                        val chunk = toWorld.getChunkAt(it.first, it.second)
//                        WorldItemManager.createItems(chunk) //데이터 생성
//                        GameManager.initDroppedItemLoc(chunk) //위치 설정
//                    }, 0)
//                    if (r % 400 == 0) {
//                        val tps =
//                            plugin.server.tps.first().coerceAtLeast(0.5)
//                                .coerceAtMost(19.5) //0.0~20.0 -> 0.5 ~ 19.5(서버 성능)
//                        Thread.sleep(((20.0 - tps) * 10.0).toLong()) //서버 성능에 따른 딜레이
//                    }
//                }
//                plugin.logger.log(Level.WARNING, "Item Creating Done")
//            })
//        }
//        thread.start()

        
        //수정시도



        fromWorld.players.forEach { player ->
            initPlayer(player)
        }

        dataClass.totalPlayer = fromWorld.players.size

        var sum = 0

        fromWorld.players.forEach { p ->
            val classData = playerStat[p.uniqueId]
            sum += classData?.playerMMR?: defaultMMR
        }

        dataClass.avgMMR = sum/fromWorld.players.size

        BorderManager.initBoarder(borderCenter, borderRadius*2)

        dataClass.worldMode = mode
        dataClass.worldTimer = System.currentTimeMillis()

        var players = ArrayList<Player>()
        fromWorld.players.forEach {
            players.add(it)
        }

        players = shufflePlayers(players)

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

        if (mode == "TwoTeam" || mode == "Heist") {
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
            val team1 = TeamClass(toWorld, first.toMutableSet())
            team1.id = 0
            dataClass.teams.add(team1)
            val team2 = TeamClass(toWorld, second.toMutableSet())
            team2.id = 1
            dataClass.teams.add(team2)

            var i = 0
            val spawnLocs = spawnLocList(borderCenter.clone().add(-1*borderRadius, 0.0, -1*borderRadius), borderCenter.clone().add(borderRadius, 0.0, borderRadius), 2, 500)
            spawnLocs.forEach {
                if (i == 0) {
                    var t = 0
                    first.forEach { p->
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            p.teleport(it)
                            initPlayer(p, true)
                        }, t*5L)
                        t++
                    }
                } else {
                    var t = 0
                    second.forEach { p->
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            p.teleport(it)
                            initPlayer(p, true)
                        }, t*5L)
                        t++
                    }
                }
                i++
            }
            if (mode == "Heist") {
                val allPlayers = first + second
                allPlayers.forEach {
                    initPlayer(it, enhancedItem = true)
                }
                scheduler.scheduleSyncDelayedTask(plugin, {
                    WorldManager.broadcastWorld(toWorld, "§a코어가 생성되고 있습니다.")
                    var n = 0
                    spawnLocs.forEach { loc ->
                        val earthG = loc.clone()
                        earthG.y = groundY
                        val lowestY = earthG.world.getHighestBlockYAt(earthG).toDouble()
                        if (earthG.y > lowestY) {
                            earthG.y = lowestY
                        }
                        earthGrenade(earthG.clone())
                        if (n == 0) {
                            dataClass.dataLoc1 = earthG.clone()
                        } else {
                            dataClass.dataLoc2 = earthG.clone()
                        }
                        n++
                    }
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        WorldManager.broadcastWorld(toWorld, "§a코어가 생성되었습니다.")
                        WorldManager.broadcastWorld(toWorld, "§a아군의 코어를 지키고, 적팀의 코어를 파괴하세요!")
                        WorldManager.broadcastWorld(toWorld, "§a코어가 남아있으면, 죽어도 부활할 수 있습니다.")
                        dataClass.dataInt1 = 1
                        dataClass.dataInt2 = 1
                        var n2 = 0
                        spawnLocs.forEach { loc ->
                            val earthG = loc.clone()
                            earthG.y = groundY
                            val lowestY = earthG.world.getHighestBlockYAt(earthG).toDouble()
                            if (earthG.y > lowestY) {
                                earthG.y = lowestY
                            }
                            val core = earthG.world.spawnEntity(earthG.clone().add(0.0, 1.0, 0.0), EntityType.IRON_GOLEM) as IronGolem
                            core.setAI(false)
                            core.isSilent = true
                            core.maxHealth = 1000.0
                            core.health = 1000.0
                            core.isInvisible = true
                            core.scoreboardTags.add("core")
                            core.scoreboardTags.add("teamid:${n2}")
                            if (n2 == 0) core.scoreboardTags.add("team:${team1.uuid}")
                            else core.scoreboardTags.add("team:${team2.uuid}")
                            createCoreDisplay(core.location)
                            n2++
                        }
                    }, 20*5)
                }, 20*10)
            }
        } else if (mode.contains("Teams:")) {
            try {
                val playerPerTeam = mode.split(":")[1].trim().toInt()
                val teams: HashMap<Int, TeamClass> = HashMap()
                val playerStack = Stack<Player>()

                val queueData = QueueOperator.initData(fromWorld)

                val teamArray = ArrayList<TeamClass>()

                queueData.teamList.forEach { teamClass ->
                    if (teamClass.players.size <= playerPerTeam) {

                        //플레이어 목록<Player>
                        val partyPlayers = mutableSetOf<Player>()

                        //플레이어 추가
                        teamClass.players.forEach {
                            val p = plugin.server.getPlayer(it)
                            if (p != null) partyPlayers.add(p)
                        }

                        //플레이어 팀 목록 제거
                        partyPlayers.forEach {
                            players.remove(it)
                        }
                        teamArray.add(TeamClass(toWorld, partyPlayers))
                    }
                }

                players = shufflePlayers(players)

                players.forEach {
                    playerStack.add(it)
                }

                val teamCount = ceil(players.size.toDouble()/playerPerTeam.toDouble()).roundToInt()


                //플레이어 배치
                for (t in 0 until playerPerTeam) {
                    for (i in 0 until teamCount) {
                        if (teams[i] == null) teams[i] = TeamClass(toWorld, mutableSetOf())
                        if (playerStack.size > 0) teams[i]!!.players.add(playerStack.pop())
                    }
                }



                teams.values.forEach {
                    teamArray.add(it)
                }
                teamArray.forEach {
                    dataClass.teams.add(it)
                }
                val spawnLocs = spawnLocList(borderCenter.clone().add(-1*borderRadius, 0.0, -1*borderRadius), borderCenter.clone().add(borderRadius, 0.0, borderRadius), teamArray.size+2, 30)
                scheduler.runTaskAsynchronously(plugin, Runnable {
                    var i = 0
                    teamArray.forEach {
                        scheduler.scheduleSyncDelayedTask(plugin, {
                            it.players.forEach { pl ->
                                pl.teleport(spawnLocs[i])
                                initPlayer(pl, true)
                            }
                        }, 0)
                        Thread.sleep(100)
                        i++
                    }
                })
            } catch (e: Exception) {
                println(e)
            }
        } else if (mode == "SoloSurvival") {
            toWorld.time = 13000
            toWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)

            val spawnLoc = spawnLocList(borderCenter.clone().add(-1*borderRadius, 0.0, -1*borderRadius), borderCenter.clone().add(borderRadius, 0.0, borderRadius), fromWorld.players.size, 30)
            val teams = mutableSetOf<Player>()
            var i = 0
            players.forEach { player ->
                teams.add(player)
                player.teleport(spawnLoc[i])
                i++
            }
            dataClass.teams.add(TeamClass(toWorld, teams))
        } else {
            val queueData = QueueOperator.initData(fromWorld)

            dataClass.isRanked = queueData.isRanked

            if (queueData.aiCount == 0 && !queueData.isRanked) {
                val aiCount = (70 - fromWorld.players.size)/4
                if (aiCount > 0) {
                    queueData.aiCount = aiCount
                }
            }

            dataClass.totalPlayer = fromWorld.players.size + queueData.aiCount

            var i = 0
            spawnLocList(borderCenter.clone().add(-0.8*borderRadius, 0.0, -0.8*borderRadius), borderCenter.clone().add(0.8*borderRadius, 0.0, 0.8*borderRadius), fromWorld.players.size + queueData.aiCount, 30).forEach {
                if (players.size > i) {
                    players[i].teleport(it)
                    initPlayer(players[i])
                } else {
                    dataClass.aiData.add(AiData(it, UUID.randomUUID()))
                }
                i++
            }

            if (mode == "Quick") {
                players.forEach { player ->
                    player.inventory.remove(Material.WOODEN_PICKAXE)
                    player.inventory.remove(Material.WOODEN_AXE)
                    player.inventory.remove(Material.WOODEN_SWORD)

                    player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.DIAMOND_HELMET))
                    player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.DIAMOND_CHESTPLATE))
                    player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.DIAMOND_LEGGINGS))
                    player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.DIAMOND_BOOTS))
                    player.inventory.setItem(EquipmentSlot.OFF_HAND, CustomItemData.getEnchantedShield())
                    player.inventory.addItem(ItemStack(Material.DIAMOND_SWORD))
                    player.inventory.addItem(ItemStack(Material.IRON_AXE).addEnchant(Enchantment.DIG_SPEED, 3))
                    player.inventory.addItem(ItemStack(Material.IRON_PICKAXE).addEnchant(Enchantment.DIG_SPEED, 3))
                    player.inventory.addItem(ItemStack(Material.BOW).addEnchant(Enchantment.ARROW_DAMAGE, 1))
                    player.inventory.addItem(CustomItemData.getVallista())
                    player.inventory.addItem(CustomItemData.getGravityG().setCount(5))
                    player.inventory.addItem(CustomItemData.getEarthGr().setCount(5))
                    player.inventory.addItem(CustomItemData.getFlashBang().setCount(5))
                    player.inventory.addItem(CustomItemData.getMolt().setCount(5))
                    player.inventory.addItem(CustomItemData.getSmokeG().setCount(5))
                    player.inventory.addItem(CustomItemData.getGoldenCarrot().setCount(3))
                    player.inventory.addItem(ItemStack(Material.GOLDEN_APPLE, 6))
                    player.inventory.addItem(ItemStack(Material.COOKED_BEEF, 64))
                    player.inventory.addItem(ItemStack(Material.ARROW, 128))
                }
            }
        }




        val worldDeleteSec: Long = 10

        val borderSize = borderRadius*2

        var changedBorderSize = borderSize
        val min = 1.0 //min분 마다 자기장 축소

        var changeAmount = 12
        if (mode == "SoloSurvival") {
            changeAmount = 6
        }

        scheduler.scheduleSyncDelayedTask(plugin, {
            fromWorld.players.forEach {
                it.teleport(lobbyLoc)
            }
            WorldManager.deleteWorld(fromWorld)
        }, 20*10)

        if (mode == "Heist") return

        scheduler.scheduleSyncDelayedTask(plugin, {
            scheduler.runTaskAsynchronously(plugin, Runnable {
                for (n in 0 until changeAmount) {
                    val origin = changedBorderSize
                    changedBorderSize *= 0.8
                    val gab = origin - changedBorderSize
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        WorldManager.broadcastWorld(toWorld, msg(gab, changedBorderSize))
                        BorderManager.changeInSec(toWorld, changedBorderSize, gab.roundToInt() / 2)
                    }, 0)
                    Thread.sleep(((min*60 + gab.roundToInt() / 2 )*1000).toLong())
                }
            })
        }, 20*60*5)
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
                if (isInRange && !WorldManager.isOutsideBorder(loc)) {
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

    fun playerSidebarSch() {
        val space = 8
        var title = "§f[  §6CCW  §f]"
        for (i in 0 until space) {
            title = "$title "
            title = " $title"
        }
        val ip = "ccw.mcv.kr"
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.onlinePlayers.forEach { player ->

                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yy. MM. d. HH:mm")
                val formatted = current.format(formatter)
                val scoreboard = scoreboardManager.newScoreboard
                val objective = scoreboard.registerNewObjective("ccw", "dummy", title)

                val newTeam = scoreboard.registerNewTeam("nth")
                newTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)

                for (p in plugin.server.onlinePlayers) newTeam.addPlayer(p)

                objective.displaySlot = DisplaySlot.SIDEBAR

                val numList = Stack<Int>()
                for (i in 0 until 16) numList.push(i)


                if (player.world.name.contains("Field-")) {
                    var showCP = true
                    val data = WorldManager.initData(player.world)
                    val time = (System.currentTimeMillis() - data.worldTimer)/1000
                    val playerCount = player.world.players.filter { it.gameMode == GameMode.SURVIVAL }.filter { !data.deadPlayer.contains(it) }.filter { !CitizensAPI.getNPCRegistry().isNPC(it) }.size
                    val itemID = SkillEvent.playerEItem[player.uniqueId] ?: 0

                    val chargeData = SkillManager.getItemCharge(itemID) ?: Pair(1, 1)

                    if ((playerMaxUse[player.uniqueId] ?: 0) >= chargeData.second) showCP = false
                    if ((playerCapacityPoint[player.uniqueId] ?: 0) >= chargeData.first) showCP = false

                    val cp = (((playerCapacityPoint[player.uniqueId] ?: 0).toDouble()/chargeData.first.toDouble())*100).coerceIn(0.0, 100.0)


                    objective.getScore("§7${formatted}").score = numList.pop()
                    objective.getScore("     ").score = numList.pop()
                    objective.getScore("§a생존자 수: ${playerCount + data.aiData.size} §7(${data.totalPlayer})").score = numList.pop()
                    objective.getScore("    ").score = numList.pop()
                    objective.getScore("§a시간: ${secondsToHMS(time)}").score = numList.pop()
                    objective.getScore("   ").score = numList.pop()
                    if (data.worldMode == "SoloSurvival") {
                        objective.getScore("§aWave: ${data.dataInt1}").score = numList.pop()
                        objective.getScore("        ").score = numList.pop()
                        objective.getScore("§a남은 좀비 수: ${player.world.livingEntities.filter { it.scoreboardTags.contains("Spawned-Zombie") }.size}").score = numList.pop()
                        objective.getScore("         ").score = numList.pop()
                        if (data.dataLong2 - System.currentTimeMillis() > 0) {
                            val world = player.world
                            var voteNeeded = world.players.filter { it.gameMode != GameMode.CREATIVE }.size/2
                            if (voteNeeded <= 0) voteNeeded = 1

                            objective.getScore("§a웨이브 스킵 투표 §7(${((data.dataLong2 - System.currentTimeMillis())/1000).toInt()}초, (${data.dataList1.filterIsInstance<UUID>().size}/${voteNeeded}))").score = numList.pop()
                            objective.getScore("          ").score = numList.pop()
                        }
                    } else {
                        objective.getScore("§aKill: ${data.playerKill[player.uniqueId] ?: 0}").score = numList.pop()
                        objective.getScore("  ").score = numList.pop()
                    }
                    if (showCP) {
                        var emoji = ""
                        if ((data.playerKill[player.uniqueId] ?: 0) < 3) emoji = "§c⚔"
                        objective.getScore("§3CP: §f${cp.roundToInt()}% §7(${playerMaxUse[player.uniqueId]}/${chargeData.second}) $emoji").score = numList.pop()
                        objective.getScore(" ").score = numList.pop()
                    }
                    objective.getScore("§fIP: $ip").score = numList.pop()

                    player.scoreboard = scoreboard
                } else if (player.world.name.contains("Queue-")) {
                    val data = QueueOperator.initData(player.world)
                    val sec = data.queueStartIn
                    var timeSec = ((sec - System.currentTimeMillis()) / 1000).toInt()
                    if (timeSec < 0) timeSec = 0
                    objective.getScore("§7${formatted}").score = numList.pop()
                    objective.getScore("    ").score = numList.pop()
                    objective.getScore("§a플레이어 수: ${player.world.players.size}").score = numList.pop()
                    objective.getScore("   ").score = numList.pop()
                    if (sec == -1L) objective.getScore("§c§l플레이어 대기중").score = numList.pop()
                    else objective.getScore("§a시작시간: $timeSec").score = numList.pop()
                    objective.getScore("  ").score = numList.pop()

                    var rankStr = "§7(Unranked)"

                    if (listOf("Solo", "Quick").contains(data.queueMode) && data.isRanked) {
                        rankStr = "§7(Ranked)"
                    }
                    objective.getScore("§a모드: ${data.queueMode} $rankStr").score = numList.pop()
                    objective.getScore(" ").score = numList.pop()
                    objective.getScore("§fIP: $ip").score = numList.pop()

                    player.scoreboard = scoreboard
                } else {
                    player.scoreboard = scoreboard
                }
            }
        }, 0, 20)
    }

    fun gameSch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.filter { it.name.contains("Field-") }.forEach { world ->
                val dataClass = WorldManager.initData(world)
                if (dataClass.worldMode == "Heist") {
                    world.players.forEach {
                        if (TeamManager.getTeam(world, it)?.id == 0) {
                            it.spawnParticle(Particle.REDSTONE, dataClass.dataLoc1, 400, 0.0, 100.0, 0.0, DustOptions(Color.GREEN, 1.0f))
                            it.spawnParticle(Particle.REDSTONE, dataClass.dataLoc2, 400, 0.0, 100.0, 0.0, DustOptions(Color.RED, 1.0f))
                        } else if (TeamManager.getTeam(world, it)?.id == 1) {
                            it.spawnParticle(Particle.REDSTONE, dataClass.dataLoc2, 400, 0.0, 100.0, 0.0, DustOptions(Color.GREEN, 1.0f))
                            it.spawnParticle(Particle.REDSTONE, dataClass.dataLoc1, 400, 0.0, 100.0, 0.0, DustOptions(Color.RED, 1.0f))
                        }
                    }
                }


                aiWorld.keys.forEach {
                    val npc = CitizensAPI.getNPCRegistry().getByUniqueId(it)
                    if (npc != null && npc.entity == null) {
                        dataClass.aiData.removeIf { data -> data.uuid == it }
                        WorldManager.broadcastWorld(world, "§c☠ AI-Bot §l➔ §cAI-Bot")
                        npc.destroy()
                    }
                }

                val ai = dataClass.aiData.filter { WorldManager.isOutsideBorder(it.loc) && !it.spawned }.randomOrNull()
                CitizensAPI.getNPCRegistry().toList().forEach {
                    if (it.name == "AI-Bot") {
                        it.idle()
                    }
                }
                if (ai != null) {
                    dataClass.aiData.remove(ai)
                    WorldManager.broadcastWorld(world, "§c☠ AI-Bot §l➔ §cAI-Bot")
                }
                val r = 40.0
                dataClass.aiData.filter { !it.spawned }.forEach { aiData ->
                    if (aiData.loc.getNearbyPlayers(r, 300.0, r).any { listOf(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(it.gameMode) }) {
                        val npc = NPCBehavior.createAI(aiData.loc)
                        aiData.spawned = true
                        aiData.uuid = npc.uniqueId
                        scheduler.runTaskAsynchronously(plugin, Runnable {
                            while (npc.entity == null) {
                                Thread.sleep(100)
                            }
                            scheduler.scheduleSyncDelayedTask(plugin, {
                                val time = ((System.currentTimeMillis() - dataClass.worldTimer) / 1000).toInt()
                                npc.idle()
                                val itemList = mutableListOf<ItemStack>()
                                when (time) {
                                    in 0..60 -> {
                                        itemList.add(ItemStack(Material.WOODEN_SWORD))
                                        itemList.add(ItemStack(Material.WOODEN_AXE))
                                        itemList.add(ItemStack(Material.WOODEN_PICKAXE))
                                        itemList.add(ItemStack(Material.COOKED_BEEF, 8))
                                    }

                                    in 60..120 -> {
                                        itemList.add(ItemStack(Material.IRON_SWORD))
                                        itemList.add(ItemStack(Material.BOW))
                                        itemList.add(ItemStack(Material.ARROW, 32))
                                        itemList.add(ItemStack(Material.COOKED_BEEF, 16))
                                        itemList.add(ItemStack(Material.STONE_AXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList.add(ItemStack(Material.STONE_PICKAXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList += getRandomIronArmor(2)
                                    }

                                    in 120..300 -> {
                                        itemList.add(ItemStack(Material.IRON_SWORD))
                                        itemList.add(ItemStack(Material.BOW).addEnchant(Enchantment.ARROW_DAMAGE, 1))
                                        itemList.add(ItemStack(Material.ARROW, 64))
                                        itemList.add(ItemStack(Material.COOKED_BEEF, 32))
                                        itemList.add(ItemStack(Material.STONE_AXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList.add(ItemStack(Material.STONE_PICKAXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList += getRandomIronArmor(4)
                                        (npc.entity as HumanEntity).equipment.setItem(EquipmentSlot.OFF_HAND, CustomItemData.getShield())
                                    }

                                    in 300..600 -> {
                                        itemList.add(ItemStack(Material.DIAMOND_SWORD))
                                        itemList.add(ItemStack(Material.BOW).addEnchant(Enchantment.ARROW_DAMAGE, 2))
                                        itemList.add(ItemStack(Material.ARROW, 64))
                                        itemList.add(ItemStack(Material.COOKED_BEEF, 64))
                                        itemList.add(ItemStack(Material.GOLDEN_APPLE, 8))
                                        itemList.add(CustomItemData.getVallista())
                                        itemList.add(ItemStack(Material.IRON_PICKAXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList.add(ItemStack(Material.IRON_AXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList += getRandomDiamondArmor(2)
                                        (npc.entity as HumanEntity).equipment.setItem(EquipmentSlot.OFF_HAND, CustomItemData.getEnchantedShield())
                                    }

                                    else -> {
                                        itemList.add(ItemStack(Material.DIAMOND_SWORD))
                                        itemList.add(ItemStack(Material.BOW).addEnchant(Enchantment.ARROW_DAMAGE, 3))
                                        itemList.add(ItemStack(Material.ARROW, 64))
                                        itemList.add(ItemStack(Material.ARROW, 64))
                                        itemList.add(ItemStack(Material.COOKED_BEEF, 64))
                                        itemList.add(ItemStack(Material.GOLDEN_APPLE, 16))
                                        itemList.add(CustomItemData.getVallista())
                                        itemList.add(ItemStack(Material.IRON_PICKAXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList.add(ItemStack(Material.IRON_AXE).addEnchant(Enchantment.DIG_SPEED, 3))
                                        itemList += getRandomDiamondArmor(4)
                                        (npc.entity as HumanEntity).equipment.setItem(EquipmentSlot.OFF_HAND, CustomItemData.getEnchantedShield())
                                    }
                                }
                                itemList.forEach {
                                    (npc.entity as HumanEntity).inventory.addItem(it)
                                }
                            }, 0)
                        })
                    }
                }
            }
        }, 0, 20)
        scheduler.scheduleSyncRepeatingTask(plugin, Runnable {
            plugin.server.worlds.filter { it.name.contains("Field-") }.forEach { world ->
                val dataClass = WorldManager.initData(world)
                if (dataClass.worldMode == "Heist") {
                    if (dataClass.dataInt1 == 1) {
                        val players = dataClass.dataLoc1.getNearbyPlayers(10.0).filter { TeamManager.getTeam(world, it)?.id == 1 }.sortedBy { it.location.distance(dataClass.dataLoc1) }.filter { it.gameMode == GameMode.SURVIVAL }
                        if (players.isNotEmpty()) {
                            for (target in players.take(4)) {
                                target.damage(1.0)
                                CustomItemManager.drawLine(dataClass.dataLoc1.clone().add(0.0, 2.0, 0.0), target.location.clone().add(0.0, 1.0, 0.0), 0.2, 0, 255, 255)
                            }
                            EffectManager.playSurroundSound(dataClass.dataLoc1, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f)
                            EffectManager.playSurroundSound(dataClass.dataLoc1, Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 2.0f)
                            EffectManager.playSurroundSound(dataClass.dataLoc1, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
                        }
                    }
                    if (dataClass.dataInt2 == 1) {
                        val players = dataClass.dataLoc2.getNearbyPlayers(10.0).filter { TeamManager.getTeam(world, it)?.id == 0 }.sortedBy { it.location.distance(dataClass.dataLoc2) }.filter { it.gameMode == GameMode.SURVIVAL }
                        if (players.isNotEmpty()) {
                            for (target in players.take(4)) {
                                target.damage(1.0)
                                CustomItemManager.drawLine(dataClass.dataLoc2.clone().add(0.0, 2.0, 0.0), target.location.clone().add(0.0, 1.0, 0.0), 0.2, 0, 255, 255)
                            }
                            EffectManager.playSurroundSound(dataClass.dataLoc2, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f)
                            EffectManager.playSurroundSound(dataClass.dataLoc2, Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 2.0f)
                            EffectManager.playSurroundSound(dataClass.dataLoc2, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f)
                        }
                    }
                }
            }
        }, 0, 20)
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
                if (null != p && loc.world == p.world) {
                    drawLine(p.location, loc, 0.2, 158, 253, 56, p)
                }
            }
            Main.plugin.server.worlds.filter { it.name.contains("Field-") }.forEach { world ->
                val dataClass = WorldManager.initData(world)

                world.players.forEach { player ->

                    if (!player.world.name.contains("Field-")) {
                        lastDamager.remove(player)
                    }
                }

                if (!dataClass.gameEndedWorld && (System.currentTimeMillis() - dataClass.worldTimer) > 1000 * 10) {
                    if (dataClass.worldMode == "Solo" || dataClass.worldMode == "Quick") {
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        if (players.size + dataClass.aiData.size == 1 || (players.isEmpty() && dataClass.aiData.size != 0)) {
                            dataClass.gameEndedWorld = true

                            if (players.isNotEmpty()) winPlayer(players.first())
                            lobbyTeleportWorlds(world)

                        }
                    } else if (dataClass.worldMode == "TwoTeam") {
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
                            dataClass.gameEndedWorld = true
                            winPlayers(ArrayList(team!!.players))
                            lobbyTeleportWorlds(world)
                        }
                    } else if (dataClass.worldMode == "Heist") {
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
                        if ((teams.size == 1 && dataClass.dataInt1 == 0 && teams.first().id == 1) || (teams.size == 1 && dataClass.dataInt2 == 0 && teams.first().id == 0)) {
                            dataClass.gameEndedWorld = true
                            winPlayers(ArrayList(team!!.players))
                            lobbyTeleportWorlds(world)
                        }
                    } else if (dataClass.worldMode == "SoloSurvival") {
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        val specs = world.players.filter { it.gameMode == GameMode.SPECTATOR }
                        if (specs.isNotEmpty() && players.isEmpty()) {
                            dataClass.gameEndedWorld = true
                            specs.forEach {
                                it.sendMessage("${ChatColor.RED}        GAME OVER")
                                it.sendMessage("${ChatColor.RED} ")
                                it.sendMessage("${ChatColor.RED}      Wave: ${dataClass.dataInt1}")
                                it.sendMessage("${ChatColor.RED}  ")
                            }
                            lobbyTeleportWorlds(world)
                        }
                    } else if (dataClass.worldMode?.contains("Teams:") == true) {
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
                            dataClass.gameEndedWorld = true
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
                if (dataClass.worldMode == "SoloSurvival") {
                    val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                    val monsters = world.entities.filter { it.scoreboardTags.contains("Spawned-Zombie") }
                    val time = (System.currentTimeMillis() -dataClass.worldTimer)/1000
                    if (dataClass.dataInt1 == 0) dataClass.dataInt1 = 1
                    if (dataClass.dataLong1 == 0L) dataClass.dataLong1 = System.currentTimeMillis()
                    val wave = dataClass.dataInt1
                    val waveDelay = dataClass.dataLong1
                    if (!dataClass.gameEndedWorld) {
                        if (System.currentTimeMillis() > waveDelay) {
                            if (monsters.isEmpty()) {
                                dataClass.dataLong1 = System.currentTimeMillis() + 10 * 1000
                                startWave(world, wave)
                                dataClass.dataInt1 = wave + 1
                            } else if (monsters.size <= 10) {
                                monsters.forEach { monster ->
                                    (monster as LivingEntity).addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 20*3, 0, false,false))

                                }
                            }
                        }
                    }
                }
            }
        }, 0, 20)
    }
    fun startWave(world: World, wave: Int, giveBuff: Boolean = true) {
        val mobList = ZombieManager.convertMobList(world.playerCount, ZombieManager.getMobList(wave))
        if (mobList.isEmpty()) {
            val dataClass = WorldManager.initData(world)
            dataClass.gameEndedWorld = true
            world.players.forEach { player ->
                player.sendTitle(Title("§6GG"))
                player.sendMessage("§6Victory!!")
                player.sendMessage("§6좀비모드 클리어를 축하합니다!")
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f)
                econ.depositPlayer(player, 5000.0)
                player.sendMessage("§a+5000코인")
            }
            lobbyTeleportWorlds(world)
            return
        } else {
            world.players.forEach { player ->
                player.sendTitle(Title("§cWave: $wave"))
                player.playSound(player, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.0f)
                econ.depositPlayer(player, 100.0)
                player.sendMessage("§a+100코인")
            }
            if (giveBuff) {
                val random = kotlin.random.Random
                val r = random.nextInt(0, 2)
                val dataClass = WorldManager.initData(world)
                if (r == 0) {
                    dataClass.dataInt2 += 1
                    world.players.forEach { player ->
                        player.sendMessage("§c§l대미지 버프 획득! §7(${dataClass.dataInt2})")
                    }
                } else if (r == 1) {
                    dataClass.dataInt3 += 1
                    world.players.forEach { player ->
                        player.sendMessage("§a§l회복 버프 획득! §7(${dataClass.dataInt3})")
                    }
                }
            }

            scheduler.runTaskAsynchronously(plugin, Runnable {
                mobList.forEach { mobStr ->
                    scheduler.scheduleSyncDelayedTask(plugin, {
                        val players = world.players.filter { it.gameMode == GameMode.SURVIVAL }
                        if (players.isNotEmpty()) {
                            val playerLoc = players.random().location
                            for (i in 0 until 100) {
                                val spawnLoc = getMobSpawnLocation(playerLoc, 50)
                                if (spawnLoc != null) {
                                    ZombieManager.spawnZombie(mobStr, spawnLoc)
                                    break
                                }
                            }
                        }
                    }, 0)
                    Thread.sleep(500)
                }
            })
        }
    }
}