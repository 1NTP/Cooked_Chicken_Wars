package me.uwuaden.kotlinplugin

import me.uwuaden.kotlinplugin.Main.Companion.playerStat
import me.uwuaden.kotlinplugin.Main.Companion.plugin
import me.uwuaden.kotlinplugin.Main.Companion.scheduler
import me.uwuaden.kotlinplugin.quickSlot.PlayerQuickSlotData
import me.uwuaden.kotlinplugin.quickSlot.QuickSlotEvent.Companion.playerQuickSlot
import me.uwuaden.kotlinplugin.rankSystem.PlayerStats
import me.uwuaden.kotlinplugin.rankSystem.RankSystem
import me.uwuaden.kotlinplugin.skillSystem.PlayerSkillHolder
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerEItem
import me.uwuaden.kotlinplugin.skillSystem.SkillEvent.Companion.playerEItemList
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*

private fun removeLastChars(str: String, n: Int): String {
    return str.substring(0, str.length - n)
}

object FileManager {
    fun copyDir(src: Path, destination: Path) {
        scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                Files.walk(src).forEach {
                    Files.copy(
                        it, destination.resolve(src.relativize(it)),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } catch (e: NumberFormatException) {
                println(e)
            }
        })
    }
    fun uploadAPIData() {
        scheduler.runTaskAsynchronously(plugin, Runnable {
            val file = File(plugin.dataFolder, "api.yml")
            if (!file.exists()) {
                file.createNewFile()
                val config = YamlConfiguration()
                config.set("pw", "")
                config.set("ip", "localhost")
                try {
                    config.save(file)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val config = YamlConfiguration()

            try {
                config.load(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val pw = config.get("pw").toString()
            val ip = config.get("ip").toString()

            playerStat.filter { plugin.server.getOfflinePlayer(it.key).name != null }.forEach { (uuid, data) ->
                val jsonData = URLEncoder.encode(
                    "{\"username\": \"${plugin.server.getOfflinePlayer(uuid).name}\", \"rank_enabled\": ${data.rank}, \"game_played\": ${data.gamePlayed}, \"unranked\": ${data.unRanked}, \"rank_string\": \"${
                        ChatColor.stripColor(
                            RankSystem.rateToString(uuid)
                        )
                    }\", \"user_score\": ${RankSystem.rateToScore(data.playerRank)},\"mmr_type\": 0}", "UTF-8"
                )//mmr_type추가
                var urlStr = "http://$ip"
                urlStr += "/post?"
                urlStr += "pw=${pw}"
                urlStr += "&uuid=${uuid}"
                urlStr += "&data=${jsonData}"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.content
                conn.disconnect()
            }
        })
    }
    fun saveVar() {
        var file = File(plugin.dataFolder, "PlayerEItem.yml")
        var config = YamlConfiguration()

        // HashMap을 YAML에 저장
        playerEItem.forEach { (uuid, value) ->
            config.set(uuid.toString(), value)
        }

        // 파일에 저장
        try {
            config.save(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        file = File(plugin.dataFolder, "PlayerEItemList.yml")
        config = YamlConfiguration()

        playerEItemList.forEach { (uuid, data) ->
            config.set(uuid.toString(), data.eliteItems.toList())
        }

        try {
            config.save(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        file = File(plugin.dataFolder, "PlayerQuickslotData.yml")
        config = YamlConfiguration()

        playerQuickSlot.forEach { (uuid, data) ->
            data.slotData.forEach { (idx, item) ->
                config.set(uuid.toString() + "_$idx", item)
            }
        }
        try {
            config.save(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        file = File(plugin.dataFolder, "PlayerRank.yml")
        config = YamlConfiguration()

        playerStat.forEach { (uuid, data) ->
            config.set(uuid.toString() + "_playerMMR", data.playerMMR)
            config.set(uuid.toString() + "_PlayerRank", data.playerRank)
            config.set(uuid.toString() + "_Rank", data.rank)
            config.set(uuid.toString() + "_GamePlayed", data.gamePlayed)
            config.set(uuid.toString() + "_IsUnranked", data.unRanked)

        }
        try {
            config.save(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun loadVar() {
        var file = File(plugin.dataFolder, "PlayerEItem.yml")
        var config = YamlConfiguration()

        // 파일에서 데이터 로드
        try {
            config.load(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val data1 = HashMap<UUID, Int>()

        // YAML에서 데이터를 HashMap으로 변환
        config.getKeys(false).forEach { uuidStr ->
            val uuid = UUID.fromString(uuidStr)
            val value = config.getInt(uuidStr)
            data1[uuid] = value
        }

        playerEItem = data1

        file = File(plugin.dataFolder, "PlayerEItemList.yml")
        config = YamlConfiguration()

        try {
            config.load(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val data2 = HashMap<UUID, PlayerSkillHolder>()
        config.getKeys(false).forEach { uuidStr ->
            val uuid = UUID.fromString(uuidStr)
            val value = config.getIntegerList(uuidStr)
            val holder = PlayerSkillHolder()
            holder.eliteItems = value.toMutableSet()
            data2[uuid] = holder
        }

        playerEItemList = data2


        val data3 = HashMap<UUID, PlayerQuickSlotData>()
        file = File(plugin.dataFolder, "PlayerQuickslotData.yml")
        config = YamlConfiguration()

        try {
            config.load(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        config.getKeys(false).forEach { uuidStr ->
            val list = uuidStr.split("_")
            val uuid = UUID.fromString(list[0])
            val item = config.getItemStack(uuidStr)
            val itemIdx = list[1].toInt()

            if (data3[uuid] == null) {
                data3[uuid] = PlayerQuickSlotData()
            }

            if (item != null) data3[uuid]?.slotData?.set(itemIdx, item)
        }
        playerQuickSlot = data3

        val data4 = HashMap<UUID, PlayerStats>()
        file = File(plugin.dataFolder, "PlayerRank.yml")
        config = YamlConfiguration()

        try {
            config.load(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val loopList = mutableSetOf<UUID>()
        config.getKeys(false).forEach {
            loopList.add(UUID.fromString(it.split("_")[0]))
        }
        loopList.forEach {
            data4[it] = PlayerStats(config.get(it.toString()+"_playerMMR") as Int, config.get(it.toString()+"_PlayerRank") as Int, config.get(it.toString()+"_Rank") as Boolean, config.get(it.toString()+"_GamePlayed") as Int, config.get(it.toString()+"_IsUnranked") as Boolean)
        }
        playerStat = data4
    }
//    fun saveVar() {
//        var f = File(plugin.dataFolder, "PlayerMMR.yml")
//        var t = ""
//        Main.playerStat.forEach {
//            t += "${it.key}: ${it.value.playerMMR}, ${it.value.playerRank}, ${it.value.rank}, ${it.value.gamePlayed}, ${it.value.unRanked}\n"
//        }
//        f.writeText(t)
//
////        f = File(plugin.dataFolder, "PlayerQuickSlot.yml")
////        t = ""
////        playerQuickSlot.forEach { (uuid, data) ->
////            t += "${uuid}/-:-/ "
////            data.slotData.forEach { (idx, item) ->
////                val displayName = item.itemMeta.displayName //Todo: 모르겠음
////
////                t += "${idx}/-&-/${displayName}/-&-/${item.type}/-,-/ "
////            }
////            t = removeLastChars(t, 6)
////            t += "\n"
////        }
////        f.writeText(t)
//    }

//    fun loadVar() {
//        var f = File(plugin.dataFolder.path)
//        if (!f.exists()) {
//            f.mkdirs()
//        }
//        f = File(plugin.dataFolder, "PlayerMMR.yml")
//        if (f.exists()) {
//            f.readText(Charsets.UTF_8).split("\n").forEach {
//                if (it.trim() != "") {
//                    try {
//                        val key = (UUID.fromString(it.split(": ")[0].trim()))
//                        if (key != null) {
//
//                            val classData = RankSystem.initData(key)
//                            val text = it.split(": ")[1]
//                            val t = StringTokenizer(text, ",")
//                            if (t.hasMoreTokens()) {
//                                val mmr = t.nextToken().trim().toInt()
//                                classData.playerMMR = mmr
//                            }
//                            if (t.hasMoreTokens()) {
//                                val rank = t.nextToken().trim().toInt()
//                                classData.playerRank = rank
//                            }
//                            if (t.hasMoreTokens()) {
//                                val bool = t.nextToken().trim().toBoolean()
//                                classData.rank = bool
//
//                            }
//                            if (t.hasMoreTokens()) {
//                                val gamePlayed = t.nextToken().trim().toInt()
//                                classData.gamePlayed = gamePlayed
//                            }
//                            if (t.hasMoreTokens()) {
//                                val unranked = t.nextToken().trim().toBoolean()
//                                classData.unRanked = unranked
//                            }
//                        }
//                    } catch (e: Exception) {
//                        println(e)
//                    }
//                }
//            }
//        } else {
//            f.createNewFile()
//        }
//        f = File(plugin.dataFolder, "PlayerQuickSlot.yml")
//        if (f.exists()) {
//            f.readText(Charsets.UTF_8).split("\n").forEach {
//                if (it.trim() != "") {
//                    try {
//                        val key = (UUID.fromString(it.split("/-:-/ ")[0].trim()))
//                        if (key != null) {
//                            val text = it.split("/-:-/ ")[1]
//
//                            playerQuickSlot[key] = PlayerQuickSlotData()
//                            text.split("/-,-/").forEach { str ->
//                                println(str)
//                                val data = str.split("/-&-/")
//
//                                val item = ItemStack(Material.valueOf(data[2].trim()))
//                                val meta = item.itemMeta
//                                if (data[1] != "") {
//                                    meta.displayName(Component.text(data[1]))
//                                }
//                                item.itemMeta = meta
//                                playerQuickSlot[key]!!.slotData[data[0].trim().toInt()] = item
//                            }
//                        }
//                    } catch (e: Exception) {
//                        println(e)
//                    }
//                }
//            }
//        } else {
//            f.createNewFile()
//        }
//    }
}