package me.uwuaden.kotlinplugin

import io.github.monun.kommand.StringType
import io.github.monun.kommand.getValue
import io.github.monun.kommand.kommand
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.gameSystem.*
import me.uwuaden.kotlinplugin.gameSystem.GameEvent
import me.uwuaden.kotlinplugin.itemManager.DroppedItem
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.OpenItemEvent
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemEvent
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemManager
import me.uwuaden.kotlinplugin.itemManager.maps.MapEvent
import me.uwuaden.kotlinplugin.itemManager.maps.MapManager
import me.uwuaden.kotlinplugin.quickSlot.QuickSlotEvent
import me.uwuaden.kotlinplugin.quickSlot.QuickSlotManager
import me.uwuaden.kotlinplugin.rankSystem.PlayerStats
import me.uwuaden.kotlinplugin.rankSystem.RankSystem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerCoin
import me.uwuaden.kotlinplugin.skillSystem.SkillInventoryHolder
import me.uwuaden.kotlinplugin.skillSystem.SkillManager
import me.uwuaden.kotlinplugin.teamSystem.TeamEvent
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import java.io.File
import java.util.*
import kotlin.math.log

private fun initPluginFolder() {
    val pluginFolder = File(plugin.dataFolder, "maps")
    if (pluginFolder.exists()) return
    pluginFolder.mkdirs()

}

private fun playSurroundSound(loc: Location, sound: Sound, volume: Float, pitch: Float) {
    loc.getNearbyPlayers(150.0).forEach { player ->
        val dist = loc.distance(player.location)
        if (dist <= 5) {
            player.playSound(loc, sound, volume, pitch)
        } else {
            val calculatedVol = volume * (log(dist+30.0, 1.2) - 20.0).toFloat()
            player.playSound(loc, sound, calculatedVol, pitch)
        }
    }
}

class Main: JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
        lateinit var scheduler: BukkitScheduler
        val worldLoaded = ArrayList<String>()
        val queueStartIn = HashMap<String, Long>()
        val queueClosed = ArrayList<String>()
        var droppedItems = ArrayList<DroppedItem>()
        val currentInv = HashMap<UUID, UUID>()
        val isOpening = ArrayList<UUID>()
        val inventoryData = HashMap<UUID, Array<ItemStack?>>()
        val queueMode = HashMap<World, String>()
        val lastDamager = HashMap<Player, Player>()
        val lastWeapon = HashMap<Player, LastWeaponData>()

        val lockedPlayer = mutableSetOf<Player>()

        var playerStat = HashMap<UUID, PlayerStats>()

        var worldDatas = HashMap<World, WorldDataManager>()

        lateinit var lobbyLoc: Location

        var groundY = 0.0
        var underItemRange = 3
        var debugStart = false

        var playerLocPing = HashMap<UUID, Location>()

        const val defaultMMR = 1000

    }
    override fun onEnable() {
        logger.info("Plugin Enabled")
        plugin = this



        plugin.server.worlds.forEach {
            val data = File(it.worldFolder, "data")
            if (data.isDirectory) {
                data.listFiles().forEach { file ->
                    if (file.name.contains("map_")) {
                        file.deleteRecursively()
                    }
                }
            }
        } //맵 데이터 삭제

        plugin.server.onlinePlayers.forEach {
            if (playerCoin[it.uniqueId] == null) playerCoin[it.uniqueId] = 10000
        }


        initPluginFolder()
        scheduler = Bukkit.getScheduler()
        scheduler.cancelTasks(plugin)
        QueueOperator.sch()
        GameManager.gameSch()
        GameManager.zombieSch()
        ItemManager.updateInventorySch()
        CustomItemManager.itemSch()
        LobbyManager.sch()
        TeamManager.sch()
        RankSystem.saveSch()
        SkillManager.initData()
        SkillManager.sch()


        Bukkit.getPluginManager().registerEvents(Events(), this)
        Bukkit.getPluginManager().registerEvents(OpenItemEvent(), this)
        Bukkit.getPluginManager().registerEvents(GameEvent(), this)
        Bukkit.getPluginManager().registerEvents(CustomItemEvent(), this)
        Bukkit.getPluginManager().registerEvents(MapEvent(), this)
        Bukkit.getPluginManager().registerEvents(TeamEvent(), this)
        Bukkit.getPluginManager().registerEvents(SkillEvent(), this)
        Bukkit.getPluginManager().registerEvents(QuickSlotEvent(), this)
        Bukkit.getPluginManager().registerEvents(SpecEvent(), this)


        plugin.server.worlds.forEach {
            if (it.name.contains("Queue-") || it.name.contains("Field-")) {
                WorldManager.deleteWorld(it)
            }
        }

        plugin.server.worldContainer.listFiles { file-> (file.name.contains("Queue-") || file.name.contains("Field-"))}.forEach {
            it.deleteRecursively()
        }

        lobbyLoc = Location(plugin.server.getWorld("world"), 0.5, 0.0, 0.5)
        groundY = 0.0
        debugStart = true

        plugin.server.onlinePlayers.forEach {
            it.teleport(lobbyLoc)
        }
        FileManager.loadVar()

        kommand {
            register("skills") {
                requires { isPlayer }
                executes {
                    if (!player.world.name.contains("Field-")) {
                        player.openInventory(SkillManager.inv(SkillInventoryHolder(), 0, player))
                    }
                }
            }
            register("teamcmd") {
                requires { isPlayer }
                then("assemble") {
                    executes {
                        val team = TeamManager.getTeam(player.world, player) ?: return@executes
                        team.players.forEach {
                            val c = Component.text("§a${player.name}님이 ${player.location.x.toInt()} ${player.location.y.toInt()} ${player.location.z.toInt()}로 집결을 요청합니다!")
                                .clickEvent(ClickEvent.runCommand("/ping location ${player.location.x.toInt()} ${player.location.y.toInt()} ${player.location.z.toInt()}"))
                                .hoverEvent(HoverEvent.showText(Component.text("클릭시 실행됩니다.")))
                            it.sendMessage(c)
                            it.sendMessage("§7(클릭하면 자동으로 위치 핑이 찍힙니다.)")
                        }
                    }
                }
            }
            register("ping") {
                requires { isPlayer }
                then("location") {
                    then("x" to int()) {
                        then("y" to int()) {
                            then("z" to int()) {
                                executes {
                                    val x: Int by it
                                    val y: Int by it
                                    val z: Int by it
                                    playerLocPing[player.uniqueId] = Location(player.world, x.toDouble(), y.toDouble(), z.toDouble())
                                    player.sendMessage("§a위치 핑이 지정되었습니다.")
                                    val c = Component.text("§a/ping reset 으로 초기화할 수 있습니다.")
                                        .clickEvent(
                                        ClickEvent.runCommand("/ping reset"))
                                        .hoverEvent(HoverEvent.showText(Component.text("클릭시 실행됩니다.")))
                                    player.sendMessage(c)
                                    player.sendMessage("§7(클릭하면 초기화됩니다.)")
                                }
                            }
                        }
                    }
                }
                then("reset") {
                    executes {
                        playerLocPing.remove(player.uniqueId)
                        player.sendMessage("§a핑이 초기화되었습니다.")
                    }
                }
            }
            register("chunkt") {
                requires { isOp || isConsole }
                then("i" to int()) {
                    executes {
                        val i: Int by it
                        WorldManager.loadWorldChunk(player.world, Location(player.world, 0.0, 0.0, 0.0), Location(player.world, i.toDouble(), 0.0, i.toDouble()), false)

                    }
                }
            }
            register("닭갈비관리자") {
                requires { isOp || isConsole }
                then("돈설정") {
                    then("amount" to int(0)) {
                        executes {
                            val amount: Int by it
                            playerCoin[player.uniqueId] = amount
                            player.sendMessage("${ChatColor.GREEN}${amount}로 설정됨.")
                        }
                    }
                }
                then("저장") {
                    executes {
                        FileManager.saveVar()
                        player.sendMessage("${ChatColor.GREEN}저장되었습니다.")
                    }
                }
                then("리로드") {
                    executes {
                        FileManager.loadVar()
                        player.sendMessage("${ChatColor.GREEN}리로드되었습니다.")

                    }
                }
                then ("랭크") {
                    val autoCompleteBlockPosition = string().apply {
                        suggests { ctx ->
                            val list = mutableListOf<String>()
                            playerStat.forEach { (k, v) ->
                                val p = plugin.server.getOfflinePlayer(k).player
                                if (p != null) list.add(p.name)
                            }
                            suggest(list)
                        }
                    }

                    then("PlayerName" to autoCompleteBlockPosition) {
                        executes {
                            val PlayerName: String by it
                            val p = plugin.server.getPlayer(PlayerName)
                            if (p != null) {
                                val classData = RankSystem.initData(p.uniqueId)

                                val rankStr = RankSystem.rateToString(player)
                                player.sendMessage("${ChatColor.GREEN}${PlayerName}님의 랭크: ${rankStr} ${ChatColor.GREEN}(${classData.playerRank%100}/100)")
                            }
                        }
                    }
                }

                then("강제시작") {
                    executes {
                        if (player.world.name.contains("Queue-")) {
                            queueStartIn[player.world.name] = System.currentTimeMillis() + 10*1000
                            player.sendMessage("${ChatColor.GREEN}강제시작 됨 (10초)")
                        }
                    }
                }
                then("test") {
                    executes {
                        playSurroundSound(Location(player.world, 0.0, 0.0, 0.0), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f)
                    }
                }
                then("test2") {
                    executes {
                        player.inventory.addItem(CustomItemData.getGravityG())
                    }
                }
                then("test3") {
                    executes {
                        player.location.getNearbyEntities(10.0, 10.0, 10.0).forEach {
                            if (it.scoreboardTags.contains("ccw_smoke")) {
                                if (it.boundingBox.contains(player.x, player.y, player.z)) {
                                    player.sendMessage("asdf")
                                }
                            }
                        }
                    }
                }
            }
            register("specdisplay") {
                requires { isOp }
                then("location") {
                    executes {

                    }
                }
                then("health") {
                    executes {
                        if (player.gameMode == GameMode.SPECTATOR) {

                        }
                    }
                }
                then("inv") {
                    then("target" to player()) {
                        executes {
                            val target: Player by it
                            if (player.world == target.world && player.gameMode == GameMode.SPECTATOR && target.gameMode == GameMode.SURVIVAL) {
                                val inv = Bukkit.createInventory(null, 54, "${target.name}'s Inventory (viewer)")
                                target.inventory.contents.forEach { item ->
                                    if (item != null) {
                                        inv.addItem(item)
                                    }
                                }
                            } else {
                                player.sendMessage("${ChatColor.RED}해당 대상의 인벤토리를 확인할 수 없습니다.")
                            }
                        }
                    }
                }
            }
            register("닭갈비") {
                then("돈") {
                    executes {
                        player.sendMessage("${ChatColor.GREEN}${player.name}님의 돈: ${playerCoin[player.uniqueId]?: 0}")
                    }
                }
                then("랭크") {
                    executes {
                        val classData = RankSystem.initData(player.uniqueId)
                        val rankStr = RankSystem.rateToString(player)
                        player.sendMessage("${rankStr} ${ChatColor.GREEN}(${classData.playerRank%100}/100)")
                    }
                    then("활성화") {
                        executes {
                            val classData = RankSystem.initData(player.uniqueId)
                            classData.rank = true
                            player.sendMessage("${ChatColor.GREEN}랭크가 활성화되었습니다.")
                        }
                    }
                    then("비활성화") {
                        executes {
                            val classData = RankSystem.initData(player.uniqueId)
                            classData.rank = false
                            player.sendMessage("${ChatColor.GREEN}랭크가 비활성화되었습니다.")
                        }
                    }
                }
                then("퀵슬롯") {
                    then("slot" to int()) {
                        executes {
                            val slot: Int by it
                            QuickSlotManager.setQuickSlot(player, slot)
                        }
                    }
                }
            }
            register("giveall") {
                requires { isOp }
                executes {
                    player.world.players.forEach {
                        if (it != player) {
                            it.inventory.addItem(player.inventory.itemInMainHand.clone())
                            it.sendMessage("${ChatColor.GREEN}${player.name}님이 아이템을 지급했습니다.")
                        }
                    }

                    player.sendMessage("${ChatColor.GREEN}모든 플레이어에게 해당 아이템이 지급되었습니다.")

                }
            }
            register("queue") {
                requires { isOp || isConsole }
                executes {
                    val uuid = WorldManager.createQueueWorld()
                    val fieldName = WorldManager.createFieldWorld("test", uuid)
                    val worldQueue = plugin.server.getWorld("Queue-$uuid") ?: return@executes
                    val world = plugin.server.getWorld(fieldName) ?: return@executes
                    WorldManager.loadWorldChunk(world, Location(world, -1000.0, 0.0, -1000.0), Location(world, 1000.0, 0.0, 1000.0), true)
                    player.teleport(Location(worldQueue, 0.0, 0.0, 0.0))
                }
            }
            register("queuemode") {
                requires { isOp || isConsole }
                then("mode" to string(StringType.GREEDY_PHRASE)) {
                    executes {
                        val mode: String by it
                        if (player.world.name.contains("Queue-")) {
                            queueMode[player.world] = mode
                            player.sendMessage("${ChatColor.GREEN}${mode}로 설정됨.")
                        } else {
                            player.sendMessage("${ChatColor.RED}큐에서만 설정가능합니다.")
                        }
                    }
                }
            }

            register("itp") {
                requires { isOp }
                executes {
                    player.teleport(droppedItems.random().loc)
                }
            }
            register("items") {
                requires { isOp }
                executes {
                    player.sendMessage(droppedItems.filter { it.loc.world == player.world }.size.toString())
                }
            }
            register("test") {
                requires { isOp }
                executes {

                }
            }
            register("queuelist") {
                requires { isOp }
                executes {
                    player.sendMessage(plugin.server.worlds.filter { it.name.contains("Queue-") }.toString())
                }
            }
            register("joinqueue") {
                requires { isPlayer }
                executes {
                    val worlds = plugin.server.worlds.filter { it.name.contains("Queue-") }.sortedByDescending { it.playerCount }
                    if (worlds.isNotEmpty()) {
                        player.teleport(Location(worlds[0], 14.5, 106.5, -40.5))
                    }
                }
            }
            register("createmap") {
                requires { isOp }
                executes {
                    player.inventory.addItem(MapManager.createMapView(player))
                }
            }
        }
    }
    override fun onDisable() {
        logger.info("Plugin Disabled")
        scheduler.cancelTasks(plugin)
        //니얼굴

        FileManager.saveVar()
        plugin.server.worlds.forEach { w->
            w.entities.forEach { e->
                if (e.scoreboardTags.contains("tmp-display")) {
                    e.remove()
                }
            }
        }
    }
}
