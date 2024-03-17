package me.uwuaden.kotlinplugin.itemManager.itemData

import me.uwuaden.kotlinplugin.Main.Companion.groundY
import me.uwuaden.kotlinplugin.Main.Companion.underItemRange
import me.uwuaden.kotlinplugin.gameSystem.WorldManager
import me.uwuaden.kotlinplugin.itemManager.ItemManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.util.*
import kotlin.math.abs


object WorldItemManager {
    fun createResourceData(world: World, loc1: Location, loc2: Location, count: Int) {
        val xList = listOf(loc1.x, loc2.x)
        val xRange = xList.min()..xList.max()
        val xChunkRange = (xList.min().toInt() shr 4)..(xList.max().toInt() shr 4)
        val zList = listOf(loc1.z, loc2.z)
        val zRange = zList.min()..zList.max()
        val zChunkRange = (zList.min().toInt() shr 4)..(zList.max().toInt() shr 4)

        val chunkNumber = abs(((xRange.start.toInt() shr 4)-(xRange.endInclusive.toInt() shr 4))*(zRange.start.toInt() shr 4)-(zRange.endInclusive.toInt() shr 4))

        val chunkItemCount = HashMap<Pair<Int, Int>, Int>()

        val rest = count%chunkNumber
        val perChunk = count/chunkNumber

        for (x in xChunkRange) {
            for (z in zChunkRange) {
                chunkItemCount[Pair(x, z)] = perChunk
            }
        }
        for (i in 0 until rest) {
            val r = chunkItemCount.keys.random()
            chunkItemCount[r] = (chunkItemCount[r] ?: 0) + 1
        }

        val worldData = WorldManager.initData(world)
        worldData.worldResourceData.ItemCount = chunkItemCount
    }
    fun createItemData(world: World, loc1: Location, loc2: Location, count: Int) { //TODO: 범위코드 위 함수처럼 수정
        val xRange: ClosedFloatingPointRange<Double>
        val zRange: ClosedFloatingPointRange<Double>
        if (loc1.x > loc2.x) xRange = loc2.x..loc1.x
        else xRange = loc1.x..loc2.x
        if (loc1.z > loc2.z) zRange = loc2.z..loc1.z
        else zRange = loc1.z..loc2.z
        val chunkNumber = abs(((xRange.start.toInt() shr 4)-(xRange.endInclusive.toInt() shr 4))*(zRange.start.toInt() shr 4)-(zRange.endInclusive.toInt() shr 4))

        val hashMap = HashMap<Pair<Int, Int>, Int>()
        val rest = count%chunkNumber


        for (x in (xRange.start.toInt() shr 4)..(xRange.endInclusive.toInt() shr 4)) {
            for (z in (zRange.start.toInt() shr 4)..(zRange.endInclusive.toInt() shr 4)) {//
                hashMap[Pair(x, z)] = count/chunkNumber

            }
        }
        for (i in 0 until rest) {
            val r = hashMap.keys.random()
            hashMap[r] = (hashMap[r]?: 0) +1


        }
        val worldData = WorldManager.initData(world)
        worldData.worldDroppedItemData.ItemCount = hashMap
    }

    fun createItems(loc: Location, rad: Double) {
        val worldData = WorldManager.initData(loc.world)
        val dataClass = worldData.worldDroppedItemData
        val random = Random()
        for (x in ((loc.x-rad).toInt() shr 4)..((loc.x+rad).toInt() shr 4)) {
            for (z in ((loc.z - rad).toInt() shr 4)..((loc.z + rad).toInt() shr 4)) {
                val chunk = Pair(x, z)
                if (dataClass.ItemCount.keys.contains(chunk) && loc.world.isChunkLoaded(x, z)) {

                    // 여기부터
                    for (i in 0 until (dataClass.ItemCount[chunk]?: 0)) {

                        dataClass.ItemCount[chunk] = (dataClass.ItemCount[chunk] ?: 0) -1
                        if ((dataClass.ItemCount[chunk]?: 0) < 1) {
                            dataClass.ItemCount.remove(chunk)
                        }
                        var create = false

                        //아이템 생성
                        if (!WorldManager.isOutsideBorder(Location(loc.world, (chunk.first shl 4).toDouble(), 0.0, (chunk.second shl 4).toDouble())) && !dataClass.FailedList.contains(chunk)) {
                            loop@for (t in 0 until 100) {
                                val selLoc = Location(loc.world, (chunk.first shl 4) + random.nextInt(0, 16).toDouble(), random.nextInt(groundY.toInt()-underItemRange, groundY.toInt()+30).toDouble(), (chunk.second shl 4) + random.nextInt(0, 16).toDouble())
                                selLoc.y += 0.5
                                selLoc.x += 0.5
                                selLoc.z += 0.5

                                var condition = 0
                                if (selLoc.block.type == Material.AIR) {
                                    while (selLoc.y > -64) {
                                        selLoc.y -= 1
                                        selLoc.block.type
                                        if (condition == 0 && selLoc.block.isSolid) {
                                            condition = 1
                                        } else if (condition == 1 && selLoc.block.type == Material.AIR) {
                                            condition = 2
                                        } else if (condition == 2 && selLoc.block.isSolid) {
                                            selLoc.y += 0.5
                                            ItemManager.createDroppedItem(selLoc, false, 3)
                                            create = true

                                            break@loop
                                        }
                                    }
                                }
                            }
                        }

                        //생성 실패시:
                        if (!create) {
                            val key = dataClass.ItemCount.filterKeys { it != chunk }.keys.random()
                            dataClass.ItemCount[key] = (dataClass.ItemCount[key] ?: 0) + 1
                            dataClass.FailedList.add(chunk)
                        }
                    }
                }
            }
        }
    }
    fun createResource(world: World, x: Int, y: Int) {
        val worldData = WorldManager.initData(world)
        val dataClass = worldData.worldResourceData
        val random = Random()
        val pairChunk = Pair(x, y)
        if (dataClass.ItemCount.keys.contains(pairChunk)) {
            // 여기부터
            for (i in 0 until (dataClass.ItemCount[pairChunk] ?: 0)) {

                dataClass.ItemCount[pairChunk] = (dataClass.ItemCount[pairChunk] ?: 0) - 1
                if ((dataClass.ItemCount[pairChunk] ?: 0) <= 0) {
                    dataClass.ItemCount.remove(pairChunk)
                }
                var create = false

                //아이템 생성
                if (!WorldManager.isOutsideBorder(Location(world, (pairChunk.first shl 4).toDouble(), 0.0, (pairChunk.second shl 4).toDouble()))) {
                    spawnTry@ for (t in 0 until 50) {
                        val selLoc = Location(
                            world,
                            (pairChunk.first shl 4) + random.nextInt(0, 16).toDouble(),
                            random.nextInt(groundY.toInt(), groundY.toInt() + 30).toDouble(),
                            (pairChunk.second shl 4) + random.nextInt(0, 16).toDouble()
                        )
                        selLoc.y += 0.5
                        selLoc.x += 0.5
                        selLoc.z += 0.5

                        if (selLoc.block.type == Material.AIR) {
                            while (selLoc.y > -64) {
                                selLoc.y -= 1
                                if (selLoc.block.isSolid) {
                                    selLoc.y += 1.0
                                    ItemManager.createResourceItem(selLoc)
                                    break@spawnTry
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    fun createItems(world: World, x: Int, y: Int) { //청크 좌표 사용
        val worldData = WorldManager.initData(world)
        val dataClass = worldData.worldDroppedItemData
        val random = Random()
        val pairChunk = Pair(x, y)
        if (dataClass.ItemCount.keys.contains(pairChunk)) {
            // 여기부터
            for (i in 0 until (dataClass.ItemCount[pairChunk] ?: 0)) {

                dataClass.ItemCount[pairChunk] = (dataClass.ItemCount[pairChunk] ?: 0) - 1
                if ((dataClass.ItemCount[pairChunk] ?: 0) <= 0) {
                    dataClass.ItemCount.remove(pairChunk)
                }
                var create = false

                //아이템 생성
                if (!WorldManager.isOutsideBorder(Location(world, (pairChunk.first shl 4).toDouble(), 0.0, (pairChunk.second shl 4).toDouble()))) {
                    spawnTry@ for (t in 0 until 50) {
                        val selLoc = Location(
                            world,
                            (pairChunk.first shl 4) + random.nextInt(0, 16).toDouble(),
                            random.nextInt(groundY.toInt() - underItemRange, groundY.toInt() + 30).toDouble(),
                            (pairChunk.second shl 4) + random.nextInt(0, 16).toDouble()
                        )
                        selLoc.y += 0.5
                        selLoc.x += 0.5
                        selLoc.z += 0.5

                        var condition = 0
                        if (selLoc.block.type == Material.AIR) {
                            while (selLoc.y > -64) {
                                selLoc.y -= 1
                                selLoc.block.type
                                if (condition == 0 && selLoc.block.isSolid) {
                                    condition = 1
                                } else if (condition == 1 && selLoc.block.type == Material.AIR) {
                                    condition = 2
                                } else if (condition == 2 && selLoc.block.isSolid) {
                                    selLoc.y += 0.5
                                    ItemManager.createDroppedItem(selLoc, false, 3)
                                    create = true

                                    break@spawnTry
                                }
                            }
                        }
                    }
                }

//                생성 실패시:
//                if (!create) {
//                    val key = dataClass.ItemCount.filterKeys { it != pairChunk }.keys.random()
//                    dataClass.ItemCount[key] = (dataClass.ItemCount[key] ?: 0) + 1
//                    //dataClass.FailedList.add(chunk)
//                }
            }
        }
    }
}