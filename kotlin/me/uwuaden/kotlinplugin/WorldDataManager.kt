package me.uwuaden.kotlinplugin

import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.itemManager.DroppedItem
import me.uwuaden.kotlinplugin.itemManager.DroppedResource
import me.uwuaden.kotlinplugin.itemManager.itemData.WorldItemData
import me.uwuaden.kotlinplugin.itemManager.itemData.WorldResourceData
import me.uwuaden.kotlinplugin.npc.AiData
import me.uwuaden.kotlinplugin.teamSystem.TeamClass
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

class WorldDataManager(var teams: ArrayList<TeamClass> = ArrayList(), var worldTimer: Long = 0L, val playerKill: HashMap<UUID, Int> = HashMap(), var worldMode: String = "Solo", var playerEssence: HashMap<Pair<UUID, String>, Int> = HashMap(), var gameEndedWorld: Boolean = false, var worldDroppedItemData: WorldItemData = WorldItemData(), var worldResourceData: WorldResourceData = WorldResourceData(),
                       var deadPlayer: MutableSet<Player> = mutableSetOf(), var totalPlayer: Int = 50, var avgMMR: Int = 0,
                       var playerRecipe: HashMap<UUID, MutableList<Recipes>> = hashMapOf(),
                       var dataInt1: Int = 0,
                       var dataInt2: Int = 0,
                       var dataInt3: Int = 0,
                       var dataInt4: Int = 0,
                       var dataList1: MutableList<Any> = mutableListOf(),
                       var dataLong1: Long = 0, var dataLong2: Long = 0,
                       var playerItemList: HashMap<UUID, MutableSet<String>> = HashMap(), var worldFolderName: String = "test", var droppedItems: ArrayList<DroppedItem> = ArrayList(), var droppedResource: ArrayList<DroppedResource> = ArrayList(), var dataLoc1: Location = Location(plugin.server.getWorld("world")!!, 0.0, 0.0, 0.0), var dataLoc2: Location = Location(plugin.server.getWorld("world")!!, 0.0, 0.0, 0.0),
                       var aiData: MutableList<AiData> = mutableListOf(), var isRanked: Boolean = false
) {
    init {
        this.worldTimer = System.currentTimeMillis()
    }

    fun initPlayerRecipe(uuid: UUID) {
        if (!playerRecipe.containsKey(uuid)) {
            playerRecipe[uuid] = mutableListOf()
        }
    }
    fun addPlayerRecipe(uuid: UUID, recipes: Recipes) {
        initPlayerRecipe(uuid)
        playerRecipe[uuid]!!.add(recipes)
    }
    fun getPlayerTypedRecipes(uuid: UUID): HashMap<Recipes, Int> {
        initPlayerRecipe(uuid)
        val hash = HashMap<Recipes, Int>()
        playerRecipe[uuid]!!.forEach { recipe ->
            hash[recipe] = (hash[recipe]?: 0) + 1
        }
        return hash
    }
    fun removePlayerRecipe(uuid: UUID, recipes: Recipes): Boolean {
        initPlayerRecipe(uuid)
        return playerRecipe[uuid]!!.remove(recipes)
    }
}