package me.uwuaden.kotlinplugin

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.itemManager.DroppedItem
import me.uwuaden.kotlinplugin.itemManager.itemData.WorldItemData
import me.uwuaden.kotlinplugin.npc.AiData
import me.uwuaden.kotlinplugin.teamSystem.TeamClass
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

class WorldDataManager(var teams: ArrayList<TeamClass> = ArrayList(), var worldTimer: Long = 0L, val playerKill: HashMap<UUID, Int> = HashMap(), var worldMode: String = "Solo", var gameEndedWorld: Boolean = false, var worldDroppedItemData: WorldItemData = WorldItemData(), var deadPlayer: MutableSet<Player> = mutableSetOf(), var totalPlayer: Int = 50, var avgMMR: Int = 0,
                       var dataInt1: Int = 0,
                       var dataInt2: Int = 0,
                       var dataInt3: Int = 0,
                       var dataInt4: Int = 0,
                       var dataList1: MutableList<Any> = mutableListOf(),
                       var dataLong1: Long = 0, var dataLong2: Long = 0,
                       var playerItemList: HashMap<UUID, MutableSet<String>> = HashMap(), var worldFolderName: String = "test", var droppedItems: ArrayList<DroppedItem> = ArrayList(), var dataLoc1: Location = Location(plugin.server.getWorld("world")!!, 0.0, 0.0, 0.0), var dataLoc2: Location = Location(plugin.server.getWorld("world")!!, 0.0, 0.0, 0.0),
                       var aiData: MutableList<AiData> = mutableListOf(), var isRanked: Boolean = false
) {
    init {
        this.worldTimer = System.currentTimeMillis()
    }
}