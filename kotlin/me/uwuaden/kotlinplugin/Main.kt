package me.uwuaden.kotlinplugin

import io.github.monun.kommand.StringType
import io.github.monun.kommand.getValue
import io.github.monun.kommand.kommand
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.assets.CustomItemData
import me.uwuaden.kotlinplugin.assets.EffectManager
import me.uwuaden.kotlinplugin.cooldown.CooldownManager
import me.uwuaden.kotlinplugin.gameSystem.*
import me.uwuaden.kotlinplugin.gameSystem.GameEvent
import me.uwuaden.kotlinplugin.gui.GuideBookEvent
import me.uwuaden.kotlinplugin.gui.GuideBookGUI
import me.uwuaden.kotlinplugin.gui.MenuEvent
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import me.uwuaden.kotlinplugin.itemManager.OpenItemEvent
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemEvent
import me.uwuaden.kotlinplugin.itemManager.customItem.CustomItemManager
import me.uwuaden.kotlinplugin.itemManager.maps.MapEvent
import me.uwuaden.kotlinplugin.itemManager.maps.MapManager
import me.uwuaden.kotlinplugin.npc.NPCBehavior
import me.uwuaden.kotlinplugin.npc.NPCEvent
import me.uwuaden.kotlinplugin.quickSlot.QuickSlotEvent
import me.uwuaden.kotlinplugin.quickSlot.QuickSlotManager
import me.uwuaden.kotlinplugin.rankSystem.PlayerStats
import me.uwuaden.kotlinplugin.rankSystem.RankEvent
import me.uwuaden.kotlinplugin.rankSystem.RankSystem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent
import me.uwuaden.kotlinplugin.skillSystem.SkillInventoryHolder
import me.uwuaden.kotlinplugin.skillSystem.SkillManager
import me.uwuaden.kotlinplugin.skillSystem.SkillManager.removeEliteItemLore
import me.uwuaden.kotlinplugin.teamSystem.TeamEvent
import me.uwuaden.kotlinplugin.teamSystem.TeamManager
import me.uwuaden.kotlinplugin.zombie.ZombieEvent
import me.uwuaden.kotlinplugin.zombie.ZombieManager
import net.citizensnpcs.api.CitizensAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.luckperms.api.LuckPerms
import net.milkbowl.vault.economy.Economy
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scoreboard.ScoreboardManager
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.util.*
import java.util.logging.Level
import kotlin.math.roundToInt


private fun sendResetMessage(player: Player) {
    player.sendMessage("§a시즌을 초기화하시겠습니까?")
    player.sendMessage("§a§l/닭갈비관리자 시즌초기화 confirm§a으로 명령어를 실행해주세요.")
}
private fun initPluginFolder() {
    val pluginFolder = File(plugin.dataFolder, "maps")
    if (pluginFolder.exists()) return
    pluginFolder.mkdirs()

}
private fun queueEnabled(world: World): Boolean {
    val data = QueueOperator.initData(world)
    return data.queueEnabled
}
private fun ItemStack.unbreakable(): ItemStack {
    val item = this.clone()
    val meta = item.itemMeta
    meta.isUnbreakable = true
    item.itemMeta = meta
    return item
}
class Main: JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
        lateinit var luckpermAPI: LuckPerms
        val worldLoaded = ArrayList<String>()
        val currentInv = HashMap<UUID, UUID>()
        val isOpening = ArrayList<UUID>()
        val inventoryData = HashMap<UUID, Array<ItemStack?>>()
        val lastDamager = HashMap<Player, Player>()
        val lastWeapon = HashMap<Player, LastWeaponData>()

        val lockedPlayer = mutableSetOf<Player>()

        var playerStat = HashMap<UUID, PlayerStats>()

        var worldDatas = HashMap<World, WorldDataManager>()
        var queueDatas = HashMap<World, QueueData>()
        var queueJoin = true
        val chunkItemDisplayGen = mutableSetOf<Chunk>()
        val chunkItemLocInit = mutableSetOf<Chunk>()

        lateinit var scheduler: BukkitScheduler
        lateinit var scoreboardManager: ScoreboardManager

        lateinit var lobbyLoc: Location
        lateinit var econ: Economy

        var groundY = 0.0
        var underItemRange = 3
        var debugStart = false

        var playerLocPing = HashMap<UUID, Location>()

        const val defaultMMR = 1000
        var map = "test"
        const val boundingBoxExpand = 0.5

    }
    override fun onEnable() {
        logger.info("Plugin Enabled")
        plugin = this

        CitizensAPI.getNPCRegistry().toList().forEach {
            if (it.name == "AI-Bot") {
                println("removed")
                it.destroy()
            }
        }

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


        scheduler = Bukkit.getScheduler()
        scoreboardManager = Bukkit.getScoreboardManager()

        initPluginFolder()
        scheduler.cancelTasks(plugin)
        NPCBehavior.sch()
        QueueOperator.sch()
        GameManager.chunkSch() //아이템 생성 등등 여러가지
        GameManager.gameSch()
        GameManager.playerSidebarSch()
        ItemManager.updateInventorySch()
        CustomItemManager.itemSch()
        LobbyManager.sch()
        TeamManager.sch()
        RankSystem.saveSch()
        SkillManager.initData()
        SkillManager.sch()
        ZombieManager.zombieSkillSch()
        CooldownManager.sch()


        Bukkit.getPluginManager().registerEvents(Events(), this)
        Bukkit.getPluginManager().registerEvents(MenuEvent(), this)
        Bukkit.getPluginManager().registerEvents(OpenItemEvent(), this)
        Bukkit.getPluginManager().registerEvents(GameEvent(), this)
        Bukkit.getPluginManager().registerEvents(CustomItemEvent(), this)
        Bukkit.getPluginManager().registerEvents(MapEvent(), this)
        Bukkit.getPluginManager().registerEvents(TeamEvent(), this)
        Bukkit.getPluginManager().registerEvents(SkillEvent(), this)
        Bukkit.getPluginManager().registerEvents(QuickSlotEvent(), this)
        Bukkit.getPluginManager().registerEvents(ZombieEvent(), this)
        Bukkit.getPluginManager().registerEvents(GuideBookEvent(), this)
        Bukkit.getPluginManager().registerEvents(RankEvent(), this)
        Bukkit.getPluginManager().registerEvents(NPCEvent(), this)

        if (!setupEconomy()) {
            server.logger.log(Level.WARNING, "Vault Load Error")
            server.pluginManager.disablePlugin(plugin)
            return
        }
        val provider = Bukkit.getServicesManager().getRegistration(
            LuckPerms::class.java
        )
        if (provider != null) {
            luckpermAPI = provider.provider
        }

        plugin.server.worlds.forEach {
            if (it.name.contains("Queue-") || it.name.contains("Field-") || it.name.contains("death_match")) {
                WorldManager.deleteWorld(it)
            }
        }

        plugin.server.worldContainer.listFiles { file-> (file.name.contains("Queue-") || file.name.contains("Field-")) || file.name.contains("death_match") }.forEach {
            it.deleteRecursively()
        }

        lobbyLoc = Location(plugin.server.getWorld("world"), 491.5, -1.0, -22.5, 90.0f, 0.0f)
        groundY = 0.0
        //debugStart = true

        plugin.server.onlinePlayers.forEach {
            it.teleport(lobbyLoc)
        }

        FileManager.loadVar()

        scheduler.scheduleSyncDelayedTask(plugin, {
            val copyDir = File(File(plugin.dataFolder, "maps"), "Sinchon")
            val pasteDir = File(plugin.server.worldContainer, "death_match")
            scheduler.runTaskAsynchronously(plugin, Runnable {
                FileManager.copyDir(copyDir.toPath(), pasteDir.toPath())
                scheduler.scheduleSyncDelayedTask(plugin, {
                    WorldManager.loadWorld("death_match")
                }, 0)
                scheduler.scheduleSyncDelayedTask(plugin, {
                    plugin.server.getWorld("death_match")?.difficulty = Difficulty.HARD
                }, 20*10)
            })
        }, 20*30)

        kommand {
            register("join_dm") {
                requires { isPlayer }
                executes {
                    if (player.world.name == "world") {
                        player.teleport(Location(plugin.server.getWorld("death_match") ?: return@executes, 0.0, 2.0, 0.0))
                        player.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*10, 4))
                        player.inventory.clear()
                        player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.IRON_HELMET).unbreakable())
                        player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.IRON_CHESTPLATE).unbreakable())
                        player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.IRON_LEGGINGS).unbreakable())
                        player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.IRON_BOOTS).unbreakable())
                        player.inventory.setItem(EquipmentSlot.OFF_HAND, ItemStack(Material.SHIELD).unbreakable())
                        player.inventory.addItem(ItemStack(Material.IRON_SWORD).unbreakable())
                        player.inventory.addItem(ItemStack(Material.BOW).unbreakable())
                        player.inventory.addItem(ItemStack(Material.ARROW, 64))
                        player.inventory.addItem(ItemStack(Material.COOKED_BEEF, 64))
                        player.inventory.addItem(ItemStack(Material.IRON_AXE))
                        player.inventory.addItem(ItemStack(Material.IRON_PICKAXE))
                        player.inventory.addItem(CustomItemData.getVallista().unbreakable())
                        player.sendMessage("§a데스매치에 입장했습니다.")
                    }
                }
            }
            register("setmap") {
                requires { isOp }
                then("test") {
                    executes {
                        map = "test"
                        player.sendMessage("§aMap -> $map")
                    }
                }
                then("shinchon") {
                    executes {
                        map = "Sinchon"
                        player.sendMessage("§aMap -> $map")
                    }
                }
            }
            register("eliteitem") {
                requires { isPlayer }
                executes {
                    if (!player.world.name.contains("Field-")) {
                        player.openInventory(SkillManager.inv(SkillInventoryHolder(), 0, player))
                    }
                }
            }
            register("팀") {
                requires { isPlayer && player.world.name.contains("Queue-") }
                then("초대") {
                    then("target" to player()) {
                        executes {
                            val target: Player by it
                            val data = QueueOperator.initData(player.world)
                            if (player.world != target.world) return@executes
                            if (data.isInTeam(target.uniqueId)) {
                                player.sendMessage("§c이미 팀에 소속되어 있습니다.")
                                return@executes
                            }
                            var teamData = data.getTeamClass(player.uniqueId)
                            if (teamData == null) {
                                data.teamList.add(TeamClass(players = mutableSetOf(player.uniqueId), leader = player.uniqueId))
                                teamData = data.getTeamClass(player.uniqueId)
                            }

                            if (teamData!!.leader != player.uniqueId) {
                                player.sendMessage("§a당신은 파티의 리더가 아닙니다.")
                                return@executes
                            }

                            if (!teamData.invitedPlayers.add(target.uniqueId)) {
                                player.sendMessage("§c이미 초대된 플레이어입니다.")
                                return@executes
                            }

                            if (data.isInTeam(target.uniqueId)) {
                                player.sendMessage("§c이미 팀에 소속된 플레이어입니다.")
                                return@executes
                            }

                            player.sendMessage("§a${target.name}님을 초대했습니다.")
                            target.sendMessage("§a${player.name}님이 당신을 팀에 초대했습니다.")
                            target.sendMessage("§a/팀 수락 ${player.name}")
                            target.sendMessage("§a명령어로 수락해주세요")
                        }
                    }
                }
                then("수락") {
                    then("target" to player()) {
                        executes {
                            val target: Player by it
                            val data = QueueOperator.initData(player.world)
                            if (player.world != target.world) return@executes

                            if (data.isInTeam(player.uniqueId)) {
                                player.sendMessage("§c이미 팀에 소속되어 있습니다.")
                                return@executes
                            }

                            val teamData = data.getTeamClass(target.uniqueId)

                            if (teamData != null) {
                                if (!teamData.invitedPlayers.contains(player.uniqueId)) {
                                    player.sendMessage("§c요청이 없습니다!")
                                    return@executes
                                }

                                player.sendMessage("§a${target.name}님의 팀에 참가했습니다.")
                                teamData.players.forEach {
                                    val p = plugin.server.getPlayer(it)
                                    p?.sendMessage("§a${player.name}님이 팀에 참가했습니다.")
                                }
                                teamData.players.add(player.uniqueId)
                            } else {
                                player.sendMessage("§c요청이 없습니다!")
                            }
                        }
                    }
                }
                then("나가기") {
                    executes {
                        val data = QueueOperator.initData(player.world)
                        if (!data.isInTeam(player.uniqueId)) {
                            player.sendMessage("§c팀에 소속되어 있지 않습니다.")
                            return@executes
                        }

                        val teamData = data.getTeamClass(player.uniqueId)
                        if (teamData != null) {
                            teamData.players.forEach {
                                val p = plugin.server.getPlayer(it)
                                p?.sendMessage("§a${player.name}님이 팀을 나갔습니다.")
                            }
                            teamData.players.remove(player.uniqueId)
                        }
                    }
                }
                then("목록") {
                    executes {
                        val data = QueueOperator.initData(player.world)
                        if (!data.isInTeam(player.uniqueId)) {
                            player.sendMessage("§c팀에 소속되어 있지 않습니다.")
                            return@executes
                        }

                        val teamData = data.getTeamClass(player.uniqueId)
                        if (teamData != null) {
                            player.sendMessage("§a팀 목록:")
                            val leaderName = plugin.server.getPlayer(teamData.leader)?.name ?: "Load Error"
                            val players = ArrayList<String>()
                            teamData.players.forEach {
                                if (it != teamData.leader) {
                                    val playerName = plugin.server.getPlayer(it)?.name
                                    if (playerName != null) players.add(playerName)
                                }
                            }
                            player.sendMessage("§a ")
                            player.sendMessage("§aLeader: $leaderName")
                            player.sendMessage("§aPlayers: ${players.joinToString(", ")}")
                            player.sendMessage("§a  ")
                        }
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
                executes {

                }
                then ("setRankedGame") {
                    then ("enabled") {
                        executes {
                            if (player.world.name.contains("Queue-")) {
                                val data = QueueOperator.initData(player.world)
                                data.isRanked = true
                            }
                        }
                    }
                    then("disabled") {
                        executes {
                            if (player.world.name.contains("Queue-")) {
                                val data = QueueOperator.initData(player.world)
                                data.isRanked = false
                            }
                        }
                    }
                }
                then("엘리트아이템") {
                    then("n" to int(0)) {
                        executes {
                            val n: Int by it
                            player.inventory.addItem(SkillEvent.skillItem[n]?.removeEliteItemLore() ?: return@executes)
                        }
                    }
                }
                then("setAI") {
                    then("n" to int(0)) {
                        executes {
                            val n: Int by it
                            if (world.name.contains("Queue-")) {
                                val data = QueueOperator.initData(world)
                                data.aiCount = n
                            }
                        }
                    }
                }
                then("큐비활성화") {
                    executes {
                        player.sendMessage("§a큐가 비활성화 되었습니다")
                        queueJoin = false
                    }
                }
                then("큐활성화") {
                    executes {
                        player.sendMessage("§a큐가 활성화 되었습니다")
                        queueJoin = true
                    }
                }
                then("저장") {
                    executes {
                        FileManager.saveVar()
                        player.sendMessage("§a저장되었습니다.")
                    }
                }
                then("리로드") {
                    executes {
                        FileManager.loadVar()
                        player.sendMessage("§a리로드되었습니다.")

                    }
                }
                then("랭크") {
                    val autoCompleteBlockPosition = string().apply {
                        suggests { ctx ->
                            val list = mutableListOf<String>()
                            playerStat.forEach { (k, v) ->
                                val p = plugin.server.getOfflinePlayer(k)
                                if (p.name != null) list.add(p.name!!)
                            }
                            suggest(list)
                        }
                    }
                    then("PlayerName" to autoCompleteBlockPosition) {
                        executes {
                            val PlayerName: String by it
                            val p = plugin.server.getOfflinePlayer(PlayerName)
                            RankSystem.openGui(player, p.uniqueId)

                        }
                    }
                }

                then("시즌초기화") {
                    then("arg" to string(StringType.GREEDY_PHRASE)) {
                        executes {
                            val arg: String by it
                            if (arg == "confirm") {
                                plugin.server.onlinePlayers.forEach { player ->
                                    val classData = RankSystem.initData(player.uniqueId)
                                    player.sendMessage("§6=================================================")
                                    player.sendMessage(" ")
                                    player.sendMessage("§a${LocalDate.now().year}년 ${LocalDate.now().monthValue}월 ${LocalDate.now().dayOfMonth}일, 시즌이 종료되었습니다!")
                                    player.sendMessage(" ")
                                    player.sendMessage("§f당신의 최종 티어 : ${RankSystem.rateToString(player.uniqueId)} §a(${RankSystem.rateToScore(classData.playerRank)})")
                                    player.sendMessage(" ")
                                    player.sendMessage("§6=================================================")
                                }
                                plugin.server.offlinePlayers.forEach { offlinePlayer ->
                                    val classData = RankSystem.initData(offlinePlayer.uniqueId)

                                    when (classData.playerRank/400) { //점수를 400단위 (한 티어) 단위로 잘라서 계산 (0: 아이언, 1: 브론즈, 2: 실버..)
                                        //아이언
                                        0 -> {
                                            econ.depositPlayer(offlinePlayer, 500.0)
                                        }
                                        //브론즈
                                        1 -> {
                                            econ.depositPlayer(offlinePlayer, 500.0)
                                        }
                                        //실버
                                        2 -> {
                                            econ.depositPlayer(offlinePlayer, 1000.0)
                                        }
                                        //골드
                                        3 -> {
                                            econ.depositPlayer(offlinePlayer, 1500.0)
                                        }
                                        //플레티넘
                                        4 -> {
                                            econ.depositPlayer(offlinePlayer, 2000.0)
                                        }
                                        //다이아
                                        5 -> {
                                            econ.depositPlayer(offlinePlayer, 2500.0)
                                        }
                                        //마스터
                                        6 -> {
                                            econ.depositPlayer(offlinePlayer, 3000.0)
                                        }
                                        //그마
                                        7 -> {
                                            econ.depositPlayer(offlinePlayer, 3500.0)
                                        }
                                        //이터널
                                        in 8..Int.MAX_VALUE -> {
                                            econ.depositPlayer(offlinePlayer, 4000.0)
                                        }
                                    }
                                    classData.playerRank = 0
                                    classData.gamePlayed = 0
                                    val removal = 250*3
                                    when (classData.playerMMR) {
                                        in 0..1200 -> classData.playerMMR -= (removal*0.5).roundToInt()
                                        in 1201..2000 -> classData.playerMMR -= (removal*0.55).roundToInt()
                                        in 2001..Int.MAX_VALUE -> classData.playerMMR -= (removal*0.6).roundToInt()
                                    }

                                    if (classData.playerMMR < 0) classData.playerMMR = 0 //배치 관련 mmr 패치
                                    classData.unRanked = true
                                }
                            } else {
                                sendResetMessage(player)
                            }
                        }
                    }
                }
                then("강제시작") {
                    executes {
                        if (player.world.name.contains("Queue-")) {
                            val data = QueueOperator.initData(player.world)
                            data.queueStartIn = System.currentTimeMillis() + 10 * 1000

                            player.sendMessage("§a강제시작 됨 (10초)")
                        }
                    }
                }
                then("test" ) {
                    then("n" to string(StringType.GREEDY_PHRASE)) {
                        executes {
                            val n: String by it
                            EffectManager.drawImageXZ(player.location, n, 50, 50, 10.0)
                        }
                    }
                }
                then("test2") {
                    executes {
                        ZombieManager.spawnZombie("boss3", player.location)
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
                then("resetcooldown") {
                    executes {
                        player.setCooldown(player.inventory.itemInMainHand.type, 0)
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
                                player.sendMessage("§c해당 대상의 인벤토리를 확인할 수 없습니다.")
                            }
                        }
                    }
                }
            }
            register("quickslot") {
                requires { isPlayer }
                executes {
                    player.openInventory(QuickSlotManager.inv(player))
                }
            }
            register("lobby") {
                requires { isPlayer }
                executes {
                    if (!player.world.name.contains("Field-") || player.gameMode != GameMode.SURVIVAL) {
                        player.inventory.clear()
                        player.teleport(lobbyLoc)
                    }
                }
            }
            register("proelium", "닭갈비", "p", "pro") {
                then("가이드북",) {
                    executes {
                        GuideBookGUI.openFileDropInvNormal(player)
                    }
                }
                then("디스코드") {
                    executes {
                        val text = Component.text("§e디스코드: §nhttps://discord.gg/YSHuMRMyY6").clickEvent(ClickEvent.openUrl(URL("https://discord.gg/YSHuMRMyY6")))
                        player.sendMessage(text)
                    }
                }
                then("돈") {
                    executes {
                        player.sendMessage("§a${player.name}님의 돈: ${econ.getBalance(player)}")
                    }
                }
                then("랭크") {
                    executes {
                        RankSystem.openGui(player)
                    }
                    then("활성화") {
                        executes {
                            val classData = RankSystem.initData(player.uniqueId)
                            classData.rank = true
                            player.sendMessage("§a랭크가 활성화되었습니다.")
                        }
                    }
                    then("비활성화") {
                        executes {
                            val classData = RankSystem.initData(player.uniqueId)
                            classData.rank = false
                            player.sendMessage("§a랭크가 비활성화되었습니다.")
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
                            it.sendMessage("§a${player.name}님이 아이템을 지급했습니다.")
                        }
                    }

                    player.sendMessage("§a모든 플레이어에게 해당 아이템이 지급되었습니다.")

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
                            val data = QueueOperator.initData(player.world)
                            data.queueMode = mode
                            player.sendMessage("§a${mode}로 설정됨.")
                        } else {
                            player.sendMessage("§c큐에서만 설정가능합니다.")
                        }
                    }
                }
            }
            register("skipWave") {
                requires { isPlayer }
                executes {
                    val world = player.world
                    if (world.name.contains("Field-")) {
                        val data = WorldManager.initData(player.world)
                        if (data.worldMode == "SoloSurvival") {
                            val sec = 20

                            if (data.dataList1.contains(player.uniqueId)) {
                                player.sendMessage("§c이미 투표에 참여했습니다.")
                                return@executes
                            }

                            if (data.dataInt1 >= 15) {
                                player.sendMessage("§c15웨이브부터는 웨이브스킵이 불가능합니다.")
                                return@executes
                            }



                            if (data.dataLong2 - System.currentTimeMillis() > 0) {
                                data.dataList1.add(player.uniqueId)
                            } else {
                                var voteNeeded = world.players.filter { it.gameMode != GameMode.CREATIVE }.size/2
                                if (voteNeeded <= 0) voteNeeded = 1
                                data.dataLong2 = System.currentTimeMillis() + 1000 * sec
                                data.dataInt4 = 1
                                data.dataList1.clear()
                                data.dataList1.add(player.uniqueId)
                                world.players.forEach { player ->
                                    player.sendMessage("§a웨이브 스킵 투표 §7(${sec}초, $voteNeeded 찬성표 필요)")
                                    val comp = Component.text("  §a[찬성] §7(클릭하여 찬성)").clickEvent(ClickEvent.runCommand("/skipwave"))
                                    player.sendMessage(comp)
                                }
                                scheduler.scheduleSyncDelayedTask(plugin, {
                                    if (data.dataList1.filterIsInstance<UUID>().size >= voteNeeded) {
                                        data.dataInt1 += 5
                                        GameManager.startWave(world, data.dataInt1)
                                        WorldManager.broadcastWorld(world, "§a§l5웨이브 스킵됨.")
                                    } else {
                                        WorldManager.broadcastWorld(world, "§c§l웨이브 스킵 실패!")
                                    }
                                    data.dataList1.clear()
                                }, 20*sec.toLong())
                            }
                        }
                    }
                }
            }
            register("itp") {
                requires { isOp }
                executes {
                    val data = WorldManager.initData(player.world)
                    player.teleport(data.droppedItems.random().loc)
                }
            }
            register("items") {
                requires { isOp }
                executes {
                    val data = WorldManager.initData(player.world)
                    player.sendMessage(data.droppedItems.filter { it.loc.world == player.world }.size.toString())
                }
            }
            register("test") {
                requires { isOp }
                executes {
                    FileManager.uploadAPIData()
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
                    if (queueJoin) {
                        val worlds = plugin.server.worlds.filter { it.name.contains("Queue-") }.filter { queueEnabled(it) }.sortedByDescending { it.playerCount }
                        if (worlds.isNotEmpty()) {
                            player.teleport(Location(worlds.first(), 14.5, 106.5, -40.5))
                        }
                    } else {
                        player.sendMessage("§c큐가 비활성화 되었습니다.")
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


        FileManager.saveVar()
        plugin.server.worlds.forEach { w->
            w.entities.forEach { e->
                if (e.scoreboardTags.contains("tmp-display")) {
                    e.remove()
                }
            }
        }

    }
    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp = server.servicesManager.getRegistration(
            Economy::class.java
        ) ?: return false
        econ = rsp.provider
        return true
    }
}
