package me.uwuaden.kotlinplugin.gameSystem

import me.uwuaden.kotlinplugin.Main
import me.uwuaden.kotlinplugin.Main.Companion.debugStart
import me.uwuaden.kotlinplugin.Main.Companion.map
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.Main.Companion.worldLoaded
import me.uwuaden.kotlinplugin.QueueData
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World

private fun setStartCount(world: World, startSec: Int) {
    val data = QueueOperator.initData(world)
    data.queueStartIn = System.currentTimeMillis() + startSec * 1000
    WorldManager.broadcastWorld(world, "§a게임이 ${startSec}초 뒤에 시작합니다")
}
private fun playerHolder(world: World) {
    val gameWorld = plugin.server.getWorld("Field-"+ world.name.replace("Queue-", ""))?: return
    val data = QueueOperator.initData(world)
    if (!data.queueEnabled) return

    if (worldLoaded.contains(gameWorld.name)) {
        worldLoaded.remove(gameWorld.name)
        WorldManager.broadcastWorld(world, "§e월드 로드 완료!")
    }
    if (world.players.isEmpty()) {
        data.queueStartIn = -1L
    }
    if (debugStart) {
        if (world.players.size >= 1 && data.queueStartIn == -1L) {
            setStartCount(world, 60)
        }
    }

    if (!data.isRanked) {
        if (world.players.size >= 4 && data.queueStartIn == -1L) {
            setStartCount(world, 600)
        } else {
            if (!data.forceStarted) data.queueStartIn = -1L
        }
    } else {
        if (world.players.size <= 5 && !data.forceStarted) {
            data.queueStartIn = -1L
        }
    }

    if (world.players.size >= 15 && data.queueStartIn == -1L) {
        setStartCount(world, 600)
    }

    if (world.players.size >= 40 && (data.queueStartIn - System.currentTimeMillis()) / 1000 >= 180) {
        setStartCount(world, 300)
    }
    if (world.players.size >= 60 && (data.queueStartIn - System.currentTimeMillis()) / 1000 >= 120) {
        setStartCount(world, 180)
    }
    if (world.players.size >= 80 && (data.queueStartIn - System.currentTimeMillis()) / 1000 >= 60) {
        setStartCount(world, 60)
    }
    val sec = data.queueStartIn
    val timeSec = ((sec - System.currentTimeMillis()) / 1000).toInt()
    if (sec > 0L) {
        if (timeSec in 1..10) {
            WorldManager.broadcastWorld(world, "§a게임이 ${timeSec}초 뒤에 시작합니다!")
            world.players.forEach {
                it.playSound(it, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f)
            }
        }
        if (timeSec < 0) {
            data.queueStartIn = 0L
            data.queueEnabled = false
            GameManager.joinPlayers(world, gameWorld, data.queueMode)
        }
    }
}

object QueueOperator {
    fun initData(world: World): QueueData {
        if (Main.queueDatas[world] == null) Main.queueDatas[world] = QueueData()
        return Main.queueDatas[world]!!
    }
    fun sch() {
        scheduler.scheduleSyncRepeatingTask(plugin, {
            plugin.server.worlds.forEach { world ->
                if (world.name.startsWith("Queue-")) {
                    playerHolder(world)
                }
            }
        }, 0, 20)
        scheduler.scheduleSyncRepeatingTask(plugin, {
            if (queueList().size < 2 && WorldManager.getInGameWorldCount() == 0) { //큐 개수 수정
                val modeList = mutableListOf("Solo", "Solo")
                if (plugin.server.onlinePlayers.size >= 10) {
                    modeList.add("TwoTeam")
                }
                createQueue(modeList.random(), map, false) //Sinchon
            }
        }, 0, 20*60)
    }

    fun createQueue(mode: String, worldFolderName: String, ranked: Boolean = true) {
        val uuid = WorldManager.createQueueWorld()
        var worldStr = ""
        scheduler.runTaskAsynchronously(plugin, Runnable {
            scheduler.scheduleSyncDelayedTask(plugin, {
                worldStr = WorldManager.createFieldWorld(worldFolderName, uuid)
            }, 0)
            while (plugin.server.getWorld(worldStr) == null && plugin.server.getWorld("Queue-$uuid") == null) {
                Thread.sleep(100)
            }
            scheduler.scheduleSyncDelayedTask(plugin, {
                if (worldStr != "") {
                    try {
                        val field = plugin.server.getWorld(worldStr)!!
                        val queue = plugin.server.getWorld("Queue-$uuid")!!
                        val dataClass = WorldManager.initData(field)
                        val queueClass = QueueOperator.initData(queue)
                        queueClass.queueMode = mode
                        queueClass.isRanked = ranked
                        dataClass.worldFolderName = worldFolderName
                        WorldManager.loadWorldChunk(
                            field,
                            Location(field, -1000.0, 0.0, -1000.0),
                            Location(field, 1000.0, 0.0, 1000.0),
                            true
                        )
                    } catch (e: Exception) {
                        println("world load error")
                    }
                }
            }, 100)
        })
    }

    fun queueList(): List<World> {
        return plugin.server.worlds.filter { it.name.contains("Queue-") }
    }
}